package com.slaythecraft.combat.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * The turn-based battle engine. Holds no Minecraft references: the same instance drives a real
 * fight in game and the offline checks in {@link CombatSelfTest}.
 *
 * <p>Turn structure (Slay the Spire style, now with 1..4 players — 设计文档 §10.2):</p>
 * <ol>
 *   <li>每名玩家依次行动：能量回满、清空自己的格挡、抽 {@code handSize} 张牌；</li>
 *   <li>该玩家出牌，结束回合时弃掉剩余手牌、自己的状态衰减；</li>
 *   <li>全员行动完 → 敌人回合：清空自身格挡、执行意图（单体意图打它预告的目标）；</li>
 *   <li>敌人状态衰减并预告下一意图（同时抽好目标）。</li>
 * </ol>
 *
 * <p>敌人是共享的，玩家各自拥有能量/手牌/抽牌堆/弃牌堆/格挡/状态。单人战斗就是玩家数 = 1 的特例，
 * 因此既有的单玩家 API（{@link #player()}、{@link #hand()}、{@link #endTurn()} 等）保持可用。</p>
 */
public final class CombatContext {
    private static final double WEAK_MULTIPLIER = 0.75D;
    /** 易伤（v0.0.3 改）：受到的伤害 +25%（旧版是 ×1.5）。 */
    private static final double VULNERABLE_MULTIPLIER = 1.25D;

    /** 一名参战玩家的入场配置（{@code energyPerTurn} 允许每名角色不同；{@code relics} 为携带的遗物）。 */
    public record Participant(Combatant combatant, List<Card> deck, int energyPerTurn, List<Relic> relics) {
        public Participant(Combatant combatant, List<Card> deck) {
            this(combatant, deck, 3, List.of());
        }

        public Participant(Combatant combatant, List<Card> deck, int energyPerTurn) {
            this(combatant, deck, energyPerTurn, List.of());
        }

        public Participant {
            deck = List.copyOf(deck);
            relics = relics == null ? List.of() : List.copyOf(relics);
            if (deck.isEmpty()) {
                throw new IllegalArgumentException("a battle needs a non-empty deck");
            }
        }
    }

    private final List<PlayerState> players = new ArrayList<>();
    private final List<Enemy> enemies;
    private final CombatRules rules;
    private final Random random;
    private final CombatLog log;

    private int activeIndex;
    private int turn;
    private CombatPhase phase = CombatPhase.PLAYER_TURN;
    /**
     * **正在结算的那名玩家**（v0.5.1.6 回合模型改造）。
     *
     * <p>以前"当前玩家"就是"轮到谁"（{@code activeIndex}）；现在所有玩家在同一个玩家回合里
     * 各打各的牌，所以"当前玩家"只在这张牌的效果结算期间有意义 ——
     * 卡牌效果里的 {@code drawCards(...)}、获得格挡、遗物触发都该落到出牌者身上。
     * 不在结算期间（界面查询、自检）时为 {@code null}，由 {@link #activeState()} 回退到
     * "下一个还没确认结束回合的存活玩家"。</p>
     */
    private PlayerState acting;
    /** V6：当前正在结算的这张牌带哪些附魔（只在 playCard 的效果执行期间非空）。 */
    private Set<CardEnchantment> activeEnchantments = Set.of();
    /** v0.0.3：活力给当前这张牌的伤害加成（只作用于第一次伤害）。 */
    private int cardBonusDamage;
    /**
     * v0.5.0.4【模组改造】：当前这张牌落进弃牌堆时要换成哪一张定义（费用档位）。
     *
     * <p>为什么需要它：{@code Card} 是共享的不可变定义，引擎没有"卡牌实例"这个概念，
     * 所以"这张牌的费用 +1"只能用"换成另一档定义"来表达。而 {@code playCard} 是
     * **效果结算完之后**才把打出的牌放进弃牌堆的，所以效果里只能先把"要换成哪一张"
     * 记在这里，落堆那一刻再替换。</p>
     */
    private Card boostedCard;
    /** v0.2.8：本场战斗里已经"失去过生命值"的参战者（遗物 FIRST_HP_LOST 只广播一次）。 */
    private final Set<Combatant> hpLossSeen = new HashSet<>();
    /** 防止"横扫之刃"的波及伤害再次触发横扫。 */
    private boolean resolvingSweep;
    /** 防止【荆棘】反弹互相触发（v0.5.2.5）。 */
    private boolean resolvingThorns;
    /**
     * 正在结算的这张牌**是不是攻击牌**（v0.5.3.3 A4-13【涂毒】用）。
     *
     * <p>和 {@link #activeEnchantments} 同一段作用域：只在"这张牌的效果正在结算"时为真。
     * 用它而不是看伤害来源，是因为 {@code dealDamage} 也被荆棘 / 充能球 / 能力牌调用，
     * 那些都**不该**算"攻击牌造成伤害"。</p>
     */
    private boolean resolvingAttackCard;
    /**
     * v0.4.0：敌人回合是否**一个一个来**（游戏内 true，好让动画从左到右依次播放；
     * 自检与指令走的还是"一次跑完"的同步模式）。
     */
    private boolean pacedEnemyTurn;
    /**
     * 诅咒【贪婪】造成的金币损失（v0.4.6）。
     *
     * <p>引擎不认识 Minecraft 的金币（它在玩家档案里），所以这里只**记账**：
     * 战斗结束时由 Minecraft 层读走并真正扣钱。</p>
     */
    private final java.util.Map<Combatant, Integer> goldDebt = new java.util.HashMap<>();
    /**
     * 【幸运药水】（v0.4.6）：本场战斗里喝过它的人 —— 下一次卡牌奖励的候选全部是强化版。
     *
     * <p>引擎只记账，战斗结束时由 Minecraft 层写进玩家档案（{@code PlayerCardData.luckyReward}）。</p>
     */
    private final Set<Combatant> luckyReward = new HashSet<>();
    /** 本次敌人回合的行动顺序（左到右；由 Minecraft 层按实际站位指定）。 */
    private final List<Enemy> enemyOrder = new ArrayList<>();
    /** 还没行动的敌人。 */
    private final List<Enemy> enemyQueue = new ArrayList<>();

    /**
     * **召唤处理器**（v0.4.6.7）：引擎不认识 Minecraft 的实体，所以"召唤恼鬼 ×2"这种动作
     * 交给 Minecraft 层实现 —— 它负责真的把实体生成出来，并返回一个可以参战的 {@link Enemy}。
     */
    public interface Summoner {
        /**
         * @param entityId 要召唤的实体 id（{@code minecraft:vex}）
         * @param summoner 发起召唤的那只敌人
         * @return 造好的敌人（失败返回 {@code null}）
         */
        Enemy summon(String entityId, Enemy summoner);
    }

    private Summoner summoner;
    /** 敌人回合是否正在进行（分步模式下由 {@link #stepEnemyTurn()} 推进）。 */
    private boolean enemyTurnRunning;

    /** 单玩家构造（既有 API 与 39 项自检都走这里）。 */
    public CombatContext(Combatant player, List<Enemy> enemies, List<Card> deck, CombatRules rules,
                         long seed, CombatLog log) {
        this(List.of(new Participant(player, deck, rules.energyPerTurn())), enemies, rules, seed, log);
    }

    /** 多玩家构造（1..4 人共享同一场遭遇战）。 */
    public CombatContext(List<Participant> participants, List<Enemy> enemies, CombatRules rules,
                         long seed, CombatLog log) {
        if (enemies.isEmpty()) {
            throw new IllegalArgumentException("a battle needs at least one enemy");
        }
        if (participants.isEmpty()) {
            throw new IllegalArgumentException("a battle needs at least one player");
        }
        for (Participant participant : participants) {
            PlayerState state = new PlayerState(participant.combatant(), participant.deck(),
                    participant.energyPerTurn(), participant.relics());
            players.add(state);
            // v0.4.1：被动加成类遗物（袭击号角 / 失活的珊瑚 / 海洋之心）在开战时点亮
            java.util.List<String> ids = new ArrayList<>(participant.relics().size());
            for (Relic relic : participant.relics()) {
                ids.add(relic.id());
            }
            RelicLibrary.applyPassiveBonuses(participant.combatant(), ids);
        }
        this.enemies = new ArrayList<>(enemies);
        this.rules = rules;
        this.random = new Random(seed);
        this.log = log == null ? CombatLog.noop() : log;
    }

    /** Starts the battle: shuffle, telegraph intents, then begin the first player's turn. */
    public void start() {
        for (PlayerState state : players) {
            state.drawPile.shuffle(random);
        }
        assignIntents();
        log.log("=== 战斗开始：" + describeParty() + " VS " + describeEnemies(enemies) + " ===");
        turn = 1;
        activeIndex = firstLivingPlayer(0);
        if (activeIndex < 0) {
            defeat();
            return;
        }
        beginPlayerRound();
    }

    // ------------------------------------------------------------------ 查询

    /** 当前行动中的玩家（单人战斗里就是唯一的玩家）。 */
    public Combatant player() {
        return activePlayer();
    }

    public Combatant activePlayer() {
        PlayerState state = activeState();
        return state == null ? null : state.combatant();
    }

    public int activeIndex() {
        return activeIndex;
    }

    /** 全部参战玩家（含已倒下的）。 */
    public List<Combatant> combatants() {
        List<Combatant> list = new ArrayList<>(players.size());
        for (PlayerState state : players) {
            list.add(state.combatant());
        }
        return List.copyOf(list);
    }

    public List<Combatant> livingPlayers() {
        List<Combatant> list = new ArrayList<>();
        for (PlayerState state : players) {
            if (state.alive()) {
                list.add(state.combatant());
            }
        }
        return List.copyOf(list);
    }

    public int playerCount() {
        return players.size();
    }

    /** 某名玩家是否还是本场战斗的参战者。 */
    public boolean isParticipant(Combatant combatant) {
        for (PlayerState state : players) {
            if (state.combatant() == combatant) {
                return true;
            }
        }
        return false;
    }

    /**
     * 这名参战者**现在还能不能出牌**（v0.5.1.6 重新定义）。
     *
     * <p>旧含义是"轮到他了"；回合模型改造后一轮里所有人同时能出牌，所以改成
     * "玩家回合 + 人还活着 + 这一轮他还没确认结束回合"。界面用它决定要不要画手牌与按钮。</p>
     */
    public boolean isActive(Combatant combatant) {
        if (combatant == null || phase != CombatPhase.PLAYER_TURN) {
            return false;
        }
        PlayerState state = stateOf(combatant);
        return state != null && state.alive() && !state.turnDone;
    }

    /** 某名参战者的私有状态（UI 同步用）；不是参战者则返回 null。 */
    public PlayerState stateOf(Combatant combatant) {
        for (PlayerState state : players) {
            if (state.combatant() == combatant) {
                return state;
            }
        }
        return null;
    }

    /** 按名字找参战者（服务端/客户端都用玩家名定位）。 */
    public Combatant combatantNamed(String name) {
        for (PlayerState state : players) {
            if (state.combatant().name().equals(name)) {
                return state.combatant();
            }
        }
        return null;
    }

    /** Every enemy in the encounter, alive or not. */
    public List<Enemy> enemies() {
        return List.copyOf(enemies);
    }

    public List<Enemy> aliveEnemies() {
        List<Enemy> alive = new ArrayList<>();
        for (Enemy enemy : enemies) {
            if (enemy.alive()) {
                alive.add(enemy);
            }
        }
        return alive;
    }

    /** 行动中玩家的手牌。 */
    public List<Card> hand() {
        return activeState().hand();
    }

    public Pile drawPile() {
        return activeState().drawPile();
    }

    public Pile discardPile() {
        return activeState().discardPile();
    }

    public Pile exhaustPile() {
        return activeState().exhaustPile();
    }

    public CombatRules rules() {
        return rules;
    }

    public int energy() {
        return activeState().energy();
    }

    public int turn() {
        return turn;
    }

    public int cardsPlayedThisTurn() {
        return activeState().cardsPlayedThisTurn();
    }

    public CombatPhase phase() {
        return phase;
    }

    public boolean isOver() {
        return phase.isOver();
    }

    // ------------------------------------------------------------------ 敌人回合的分步播放（v0.4.0）

    /**
     * 诅咒【贪婪】：记下"这名玩家欠了多少金币"（v0.4.6）。
     *
     * <p>真正扣钱在战斗结束时由 Minecraft 层做（金币住在玩家档案里），扣到 0 为止不会变负数。</p>
     */
    public void loseGold(Combatant owner, int amount) {
        if (owner == null || amount <= 0) {
            return;
        }
        goldDebt.merge(owner, amount, Integer::sum);
    }

    /** 这名参战者欠下的金币（战斗结束时结算）；没有欠款返回 0。 */
    public int goldDebt(Combatant owner) {
        return goldDebt.getOrDefault(owner, 0);
    }

    /** 清空金币欠款（结算完之后调用，避免重复扣）。 */
    public void clearGoldDebt() {
        goldDebt.clear();
    }

    /** 【幸运药水】：让这名参战者的下一次卡牌奖励候选全部变成强化版（v0.4.6）。 */
    public void markLuckyReward(Combatant owner) {
        if (owner == null) {
            return;
        }
        luckyReward.add(owner);
        log.log("  " + owner.name() + " 获得【幸运】：下一次卡牌奖励的所有候选都会是强化版");
    }

    /** 这名参战者本场战斗里是否喝过【幸运药水】。 */
    public boolean luckyReward(Combatant owner) {
        return luckyReward.contains(owner);
    }

    /** 引擎自带的随机数（谜之炖菜这类"随机效果"用；同一颗种子可复现）。 */
    public int nextRandomInt(int bound) {
        return bound <= 0 ? 0 : random.nextInt(bound);
    }

    /**
     * 打开/关闭"敌人一个一个行动"的分步模式。
     *
     * <p>游戏内由 Minecraft 层打开：{@code endTurn()} 会停在敌人回合的第一个敌人之前，
     * 之后每调一次 {@link #stepEnemyTurn()} 才让一只敌人行动 —— 动画就能依次播放。</p>
     */
    public void setPacedEnemyTurn(boolean paced) {
        this.pacedEnemyTurn = paced;
    }

    public boolean pacedEnemyTurn() {
        return pacedEnemyTurn;
    }

    /**
     * 指定敌人行动的**先后顺序**（需求："每个敌人执行行动时，从左到右依次播放动画"）。
     *
     * <p>引擎不认识坐标，"左到右"由 Minecraft 层按玩家朝向与实际站位算好后传进来；
     * 没指定的敌人在其后按表内顺序补上。</p>
     */
    public void setEnemyOrder(List<Enemy> order) {
        enemyOrder.clear();
        if (order != null) {
            enemyOrder.addAll(order);
        }
    }

    public List<Enemy> enemyOrder() {
        return List.copyOf(enemyOrder);
    }

    /** 敌人回合是否还没走完（分步模式下 Minecraft 层据此继续推进）。 */
    public boolean awaitingEnemyActions() {
        return enemyTurnRunning;
    }

    // ------------------------------------------------------------------ 出牌 / 回合

    /**
     * Plays the card at {@code handIndex} for the **active** player.
     *
     * @param enemyIndex preferred enemy index for single-target cards; other values fall back to
     *                   the first living enemy
     */
    /** 当前这张牌将要落进弃牌堆的"费用档位"定义；没有替换时为 null（v0.5.0.4 模组改造）。 */
    public Card boostedCard() {
        return boostedCard;
    }

    /** 登记"当前这张牌落堆时要换成哪一张"（v0.5.0.4 模组改造）。 */
    public void markBoostedCard(Card card) {
        this.boostedCard = card;
    }

    /**
     * 打出一张牌（**不带玩家**：给自检与指令用，出牌者 = "下一个还没确认结束回合的存活玩家"）。
     *
     * <p>单人战斗里就是唯一的玩家；多人战斗里等于"按座位顺序轮到谁" —— 自检里的多人脚本就是这么写的。
     * 游戏内的真实出牌走 {@link #playCard(Combatant, int, int)}（出牌者由包是谁发的决定）。</p>
     */
    public PlayResult playCard(int handIndex, int enemyIndex) {
        PlayerState state = activeState();
        if (state == null) {
            return PlayResult.fail("没有可以行动的玩家");
        }
        return playCard(state.combatant(), handIndex, enemyIndex);
    }

    /**
     * **某一名玩家**打出一张牌（v0.5.1.6 回合模型改造）。
     *
     * <p>需求："多个玩家可以同时出牌，按照出牌时间顺序结算打出的牌。" 服务端是单线程的，
     * 所以"出牌时间顺序"天然等于"请求到达顺序" —— 谁先发到这个方法，谁先结算。</p>
     *
     * @param who 出牌者（必须是本场战斗的参战者，而且这一轮还没确认结束回合）
     */
    public PlayResult playCard(Combatant who, int handIndex, int enemyIndex) {
        if (phase != CombatPhase.PLAYER_TURN) {
            return PlayResult.fail("现在不是玩家回合（当前：" + phase + "）");
        }
        PlayerState state = stateOf(who);
        if (state == null) {
            return PlayResult.fail("不是本场战斗的参战者");
        }
        if (!state.alive()) {
            return PlayResult.fail("你已经倒下了");
        }
        if (state.turnDone) {
            return PlayResult.fail("你已经结束本回合了");
        }
        if (state.pendingPick != null) {
            // v0.5.1.7：还没选完牌就不能再出牌（否则候选下标会错位）
            return PlayResult.fail("先完成「" + state.pendingPick.cardName() + "」的选择");
        }
        // 结算期间"当前玩家"就是出牌者：卡牌效果里的 drawCards(...) / 格挡 / 遗物都该落到他身上
        PlayerState previous = acting;
        acting = state;
        try {
            return playCardFor(state, handIndex, enemyIndex);
        } finally {
            acting = previous;
        }
    }

    /** 出牌主体：{@code state} 是出牌者（调用方已经校验过阶段与"还没确认"）。 */
    private PlayResult playCardFor(PlayerState state, int handIndex, int enemyIndex) {
        Combatant actor = state.combatant();
        if (handIndex < 0 || handIndex >= state.hand.size()) {
            return PlayResult.fail("手牌编号无效：可用范围 1-" + state.hand.size());
        }
        Card card = state.hand.get(handIndex);
        // v0.0.3 W2：状态牌里有一部分"不能被打出"（虚空 / 灼烧 / 晕眩）
        if (StatusCards.unplayable(card)) {
            return PlayResult.fail("「" + card.name() + "」是状态牌，不能被打出");
        }
        // v0.5.3.9（A4-18）E16：动态条件（【交锋】要求手里全是攻击牌）
        if (!playableNow(state, card)) {
            return PlayResult.fail("「" + card.name() + "」现在的条件不满足（手里必须全是攻击牌）");
        }
        // v0.4.7.3：X 费卡（cost < 0）不检查能量，直接花光当前能量
        // v0.5.3.3【布置】：被布置过的牌在打出之前费用为 0（effectiveCost 是唯一出口）
        int cost = effectiveCost(state, card);
        if (!card.xCost() && cost > state.energy) {
            return PlayResult.fail("能量不足：「" + card.name() + "」需要 " + cost + " 点，剩余 " + state.energy);
        }

        Combatant target = null;
        if (card.target() == CardTarget.ENEMY) {
            int index = resolveEnemyIndex(enemyIndex);
            if (index < 0) {
                return PlayResult.fail("没有可以被指定的敌人");
            }
            target = enemies.get(index);
        } else if (card.target() == CardTarget.SELF) {
            target = actor;
        }

        if (card.xCost()) {
            state.lastX = state.energy;
            state.energy = 0;
        } else {
            state.energy -= cost;
            // v0.5.3.3【布置】：这一次免费额度用掉了（下次同一张牌恢复原价）
            if (cost == 0 && card.cost() > 0) {
                state.consumeFreeToPlay(card.id());
                state.consumeFreeThisTurn(card.id());
            }
        }
        state.cardsPlayedThisTurn++;
        // v0.5.0.10 隐藏成就【回响形态了 回响形态】：需求"连续打出 4 张同名手牌。"
        // 按卡牌 id 判定连打；打满 4 张广播一次（引擎不管给谁记，MC 层决定）。
        if (state.recordPlayedCard(card.id()) >= 4) {
            emitFx(CombatFx.FOUR_SAME_CARDS, actor, actor);
        }
        state.hand.remove(handIndex);
        log.log(prefix(actor) + "打出「" + card.name() + "」（能量 " + state.energy + "/" + state.energyPerTurn() + "）");
        if (!card.enchantments().isEmpty()) {
            log.log("  附魔生效：" + card.enchantmentLabel());
        }
        // v0.5.3.3（A4-13）【双重施法】/【爆发】：这一张牌要不要打两遍
        int plays = state.consumeDoublePlay(card.type()) ? 2 : 1;
        if (plays > 1) {
            log.log("  【双重施法 / 爆发】：「" + card.name() + "」这一张会打两遍");
        }
        // v0.0.3：活力只作用于"下一张攻击牌"，加在该牌的第一次伤害上，然后清零
        Card toDiscard = card;
        for (int play = 0; play < plays; play++) {
            if (play > 0) {
                // 第一遍把目标打死了：第二遍随机换一个活着的敌人（与"牌还在，只是目标没了"一致）
                if (card.target() == CardTarget.ENEMY && (target == null || !target.alive())) {
                    target = randomLivingEnemy();
                    if (target == null) {
                        log.log("  没有可以重新指定的敌人，第二遍略过");
                        break;
                    }
                    log.log("  原目标已倒下 → 第二遍改为 " + target.name());
                }
                log.log("  「" + card.name() + "」第二遍结算");
            }
            if (card.type() == CardType.ATTACK) {
                // v0.5.3.6（A4-15）【终结技】：双发算两张
                state.attacksPlayedThisTurn++;
            }
            cardBonusDamage = 0;
            if (card.type() == CardType.ATTACK && actor.vigor() > 0) {
                cardBonusDamage = actor.vigor();
                actor.clearVigor();
                log.log("  活力生效：这张牌额外造成 " + cardBonusDamage + " 点伤害");
            }
            // V6：附魔只在这张牌结算期间有效（敌人回合不受影响）
            Set<CardEnchantment> previousEnchantments = activeEnchantments;
            activeEnchantments = card.enchantments();
            // v0.5.3.3【涂毒】：标记"现在结算的是一张攻击牌"
            boolean previousAttackFlag = resolvingAttackCard;
            resolvingAttackCard = card.type() == CardType.ATTACK;
            int blockBeforePlay = actor.block();
            // v0.5.0.4 模组改造：效果里可以要求"本牌落堆时换成另一档费用"
            Card previousBoosted = boostedCard;
            boostedCard = null;
            try {
                card.effect().apply(this, actor, target);
            } finally {
                activeEnchantments = previousEnchantments;
                resolvingAttackCard = previousAttackFlag;
            }
            // 效果里登记过费用档位就用那一张落堆（效果自己负责清掉，避免影响下一张牌）
            if (play == 0) {
                toDiscard = boostedCard != null ? boostedCard : card;
            }
            boostedCard = previousBoosted;
            // v0.5.1.4：两张新牌的"每打出一张牌就记一次账"的效果（村庄英雄 / 魔术技巧！）
            // v0.5.3.3：A4-12/13 的【众生皆苦】【劫掠形态】【千刀万剐】也挂在这个点上
            onCardPlayedForNewPowers(state, card);
            // v0.3.0：表现层事件（攻击 → 前冲 + 暴击音效；给自己上格挡 → 蹲起 + 穿甲音效）
            if (card.type() == CardType.ATTACK) {
                emitFx(CombatFx.PLAYER_ATTACK, actor, target);
            }
            if (actor.block() > blockBeforePlay) {
                emitFx(CombatFx.PLAYER_BLOCK, actor, target);
            }
            if (plays > 1) {
                checkBattleEnd();
                if (phase.isOver()) {
                    break;
                }
                if (play + 1 < plays && state.pendingPick != null) {
                    // ⚠️ 选牌只有一个槽位：第二遍会覆盖第一遍的候选快照，所以**略过第二遍**
                    // （宁可少打一遍，也不能让第一次的选择凭空消失）
                    log.log("  「" + card.name() + "」还在等你选牌，第二遍略过（避免选牌请求互相覆盖）");
                    break;
                }
            }
        }
        // The played card only reaches the discard pile after its effect resolved, so that
        // "draw a card" effects can never redraw it in the same instant.
        // V4：带「消耗」的牌改为进入消耗牌堆，本场战斗不会再出现。
        if (card.type() == CardType.POWER) {
            // v0.4.2：需求"新卡牌类型：能力" —— 打出后获得增益，然后**本场战斗从牌组里移除**
            log.log("  「" + card.name() + "」是能力牌：效果常驻，本场战斗不再出现");
        } else if (card.has(CardKeyword.EXHAUST)) {
            exhaustCard(state, card);
            log.log("  「" + card.name() + "」被消耗（本场战斗不再出现）");
        } else if (state.corruption && card.type() == CardType.SKILL) {
            // v0.5.3.3【腐化】：打出的技能牌直接消耗
            exhaustCard(state, toDiscard);
            log.log("  【腐化】：「" + card.name() + "」被打出后直接消耗（本场战斗不再出现）");
        } else {
            state.discardPile.add(toDiscard);
        }
        // v0.4.2：【余像】—— 每打出一张牌获得 1 点格挡（含能力牌自己）
        if (state.afterImage) {
            int gained = previewBlock(actor, 1);
            gainBlockFor(actor, gained);
            log.log("  「余像」：打出「" + card.name() + "」后获得 " + gained + " 点格挡（当前 "
                    + actor.block() + "）");
        }
        // V7：打出卡牌后的遗物触发点
        fireRelics(RelicTrigger.CARD_PLAYED, state);
        checkBattleEnd();
        return PlayResult.ok("打出「" + card.name() + "」");
    }

    /**
     * 需要玩家**挑牌**的效果种类（v0.5.1.7 A3）。
     *
     * <p>这四张牌的卡面本来就写着"选择…"，但实现一直是"自动挑费用最高的" —— 现在改成真的让玩家选。</p>
     */
    public enum CardPickKind {
        /** 废料回收：从**弃牌堆**选 1 张放到抽牌堆顶。 */
        SCRAP_RECYCLE,
        /** 应急储备：从**手牌**选 1 张变成闪光弹。 */
        CONTINGENCY,
        /** 弃置边陵：从**手牌**选最多 N 张消耗。 */
        ABANDON,
        /** 回收：从**手牌**选 1 张消耗，获得等同其费用的能量。 */
        RECYCLE,
        /**
         * 弃牌（v0.5.2.2 A4-9）：从**手牌**选 N 张**弃掉**（进弃牌堆，不是消耗）。
         *
         * <p>准备 / 集中精神 / 杂技 / 飞刀投掷用；与 {@link #ABANDON}（消耗）、
         * {@link #RECYCLE}（消耗换能量）的区别只在"落点"。头槌复用 {@link #SCRAP_RECYCLE}
         * （效果完全一样，只是方向相反）。</p>
         */
        DISCARD,
        /** 军备（v0.5.2.3 A4-10）：从**手牌**选 1 张**升级**（本场战斗）。 */
        UPGRADE_ONE,
        /** 双持（v0.5.2.3 A4-10）：从**手牌**选 1 张攻击/能力牌，**复制** {@code amount} 张进手牌。 */
        DUPLICATE,
        /**
         * 燃烧契约（v0.5.2.3 A4-10）：从**手牌**选 1 张**消耗**，然后抽 {@code amount} 张。
         *
         * <p>和 ABANDON 的差别只在"消耗完还要抽牌"——抽牌必须等玩家选完才做，
         * 所以只能由 {@link #applyCardPick} 一起完成。</p>
         */
        BURNING_PACT,
        /** 战吼（v0.5.2.4 A4-11）：从**手牌**选 1 张，放到**抽牌堆顶**。 */
        PUT_ON_TOP,
        /**
         * 布置（v0.5.3.3 A4-12）：从**手牌**选 1 张放到**抽牌堆顶**，
         * 并且**那张牌被打出之前费用为 0**（比 PUT_ON_TOP 多记一笔"免费额度"）。
         */
        SETUP,
        /** 挖掘（v0.5.3.9 A4-18）：从**消耗堆**选 1 张拿回手牌。 */
        EXHUME,
        /**
         * 噩梦（v0.5.3.9 A4-18）：从**手牌**选 1 张，**下个回合**开始把 {@code amount} 张复制加入手牌。
         */
        NIGHTMARE
    }

    /** 候选牌从哪个牌堆来。 */
    public enum CardPickSource {
        HAND,
        DISCARD,
        /** 消耗堆（v0.5.3.9 A4-18【挖掘】）。 */
        EXHAUST
    }

    /**
     * 一次"等待玩家选牌"的请求（v0.5.1.7 A3；v0.5.2.3 加 {@code sourceIndices} / {@code amount}）。
     *
     * @param cardName      发起这次选择的牌名（写日志用）
     * @param maxPicks      最多选几张（弃置边陵是"最多 N 张"，其余是恰好 1 张）
     * @param payload       效果需要的**替换目标**（应急储备的闪光弹定义），其余为 {@code null}
     * @param candidates    **挂起那一刻的候选快照** —— 必须快照：打出的那张牌在效果结算完之后
     *                      才进弃牌堆，如果等到玩家选的时候再取，废料回收的候选会凭空多出一张、
     *                      下标全部错位
     * @param sourceIndices 第 i 个候选在**来源牌堆**里的真实下标（手牌 / 弃牌堆）。
     *                      <p>不筛选时就是 {@code 0..n-1}；一旦按条件筛过（军备只给"还能升级的"、
     *                      双持只给攻击/能力牌），候选下标就和手牌下标**对不上**了，
     *                      所以必须把真实下标一起存下来 —— 否则会改错牌。</p>
     * @param amount        "要做几次"：燃烧契约抽几张、双持复制几张；其余 kind 填 0
     */
    public record PendingCardPick(String cardName, CardPickKind kind, CardPickSource source, int maxPicks,
                                  Card payload, List<Card> candidates, List<Integer> sourceIndices,
                                  int amount) {
    }

    /**
     * 新卡牌（v0.5.1.4）里"每打出一张牌就记一次账"的两件事，都挂在出牌流程的同一个点上。
     *
     * <ul>
     *   <li>【村庄英雄】：每打 3 张**攻击牌** → 对随机敌人施加 15 层凋零；
     *       每打 3 张**技能牌** → 自己与队友各 1 层附甲（附甲不衰减由旗标负责）；</li>
     *   <li>【魔术技巧！】：**在手牌中时**每打出一张技能牌，手牌里的每一张都 -1 费
     *       （每张单独计算 —— 见 {@link #advanceMagicTricks}）。</li>
     * </ul>
     *
     * <p>v0.5.3.1 又在这个点上挂了叛逃掠夺者的两张能力牌：</p>
     * <ul>
     *   <li>【众生皆苦】：每打出一张牌 → 所有敌人 1 层凋零（这张能力牌自己也算一张，
     *       与【村庄英雄】一致 —— 点亮它的那一下就已经把敌人挂上凋零了）；</li>
     *   <li>【劫掠形态】：每打出一张**攻击牌** → 加一张「弩箭射击」进手牌。
     *       ⚠️ 注意「弩箭射击」自己也是攻击牌，所以它会**自我续杯**：手牌上限到了就不再进牌，
     *       这是需求原文的写法，不是 bug（见交接稿 §4.38）。</li>
     * </ul>
     */
    private void onCardPlayedForNewPowers(PlayerState state, Card card) {
        if (card == null) {
            return;
        }
        if (state.allSuffering) {
            int hit = 0;
            for (Enemy enemy : aliveEnemies()) {
                applyDecay(enemy, 1);
                hit++;
            }
            if (hit > 0) {
                log.log("  【众生皆苦】：" + card.name() + " → " + hit + " 个敌人各获得凋零 1");
            }
        }
        if (state.pillageForm && card.type() == CardType.ATTACK) {
            Card bolt = state.pillageFormUpgraded
                    ? CardLibrary.resolve("bolt_shot", 1) : CardLibrary.resolve("bolt_shot", 0);
            if (bolt != null) {
                int added = addCardsToHand(state.combatant(), bolt, 1);
                log.log("  【劫掠形态】：" + card.name() + " → 加入 " + added + " 张「" + bolt.name() + "」");
            }
        }
        // v0.5.3.3（A4-13）【千刀万剐】：每打出一张牌（含它自己）→ 全体敌人各受 N 点伤害
        if (state.thousandCuts > 0) {
            List<Enemy> alive = aliveEnemies();
            if (!alive.isEmpty()) {
                log.log("  【千刀万剐】：打出「" + card.name() + "」→ " + alive.size() + " 个敌人各受 "
                        + state.thousandCuts + " 点伤害");
                for (Enemy enemy : alive) {
                    dealDamage(state.combatant(), enemy, state.thousandCuts, "「千刀万剐」");
                }
            }
        }
        // v0.5.4.0（A4-19）【窒息】：本回合每打出一张牌 → 目标失去 N 点生命（含它自己打出的那些牌）
        if (state.chokeAmount > 0 && state.chokeEnemy != null && state.chokeEnemy.alive()
                && !"choke".equals(card.id())) {
            log.log("  【窒息】：打出「" + card.name() + "」→ " + state.chokeEnemy.name() + " 失去 "
                    + state.chokeAmount + " 点生命");
            loseHp(state.chokeEnemy, state.chokeAmount, "「窒息」");
            if (phase.isOver()) {
                return;
            }
        }
        // v0.5.3.6（A4-15）【狂怒】：本回合每打出一张攻击牌 → 获得 N 点格挡
        if (state.rage > 0 && card.type() == CardType.ATTACK) {
            int gained = previewBlock(state.combatant(), state.rage);
            gainBlockFor(state.combatant(), gained);
            log.log("  【狂怒】：打出攻击牌「" + card.name() + "」→ 获得 " + gained + " 点格挡（当前 "
                    + state.combatant().block() + "）");
        }
        if (state.villageHero) {
            if (card.type() == CardType.ATTACK) {
                state.heroAttacks++;
                if (state.heroAttacks % 3 == 0) {
                    List<Enemy> alive = aliveEnemies();
                    if (!alive.isEmpty()) {
                        Enemy victim = alive.get(nextRandomInt(alive.size()));
                        victim.addDecay(15);
                        log.log("  【村庄英雄】：第 " + state.heroAttacks + " 张攻击牌 → "
                                + victim.name() + " 获得凋零 15" + victim.statusSuffix());
                    }
                }
            } else if (card.type() == CardType.SKILL) {
                state.heroSkills++;
                if (state.heroSkills % 3 == 0) {
                    StringBuilder who = new StringBuilder();
                    for (PlayerState member : players) {
                        if (member.alive()) {
                            member.combatant().addPlatedArmor(1);
                            if (who.length() > 0) {
                                who.append('、');
                            }
                            who.append(member.combatant().name());
                        }
                    }
                    log.log("  【村庄英雄】：第 " + state.heroSkills + " 张技能牌 → " + who
                            + " 各获得 1 层附甲");
                }
            }
        }
        if (card.type() == CardType.SKILL) {
            advanceMagicTricks(state);
        }
    }

    /**
     * 【魔术技巧！】的费用递减（v0.5.1.4）。
     *
     * <p>需求："当这张牌在你的手牌中时，你每次打出技能牌都会使这张牌的费用-1。"
     * 并且（用户补充的消歧义）"手牌里的每张魔术技巧单独计算当前已经减少的费用"。</p>
     *
     * <p>实现方式：这张牌**每一档费用都是一个独立定义**（{@code magic_trick} → {@code magic_trick_d1}
     * → … → {@code magic_trick_d10}，同样带强化版），打技能牌时把手里那一张**换成便宜一档的定义**。
     * 这样"每张单独算"是**结构上成立**的 —— 不需要在别处维护一份与手牌平行的折扣表，
     * 也就不会因为漏改某个抽牌/弃牌点而错位。费用显示、能量扣除、卡面预览全都自动跟上。</p>
     */
    private void advanceMagicTricks(PlayerState state) {
        for (int i = 0; i < state.hand.size(); i++) {
            Card inHand = state.hand.get(i);
            if (!CardLibrary.isMagicTrick(inHand.id())) {
                continue;
            }
            int next = CardLibrary.magicTrickDiscount(inHand.id()) + 1;
            if (next > CardLibrary.MAGIC_TRICK_MAX_DISCOUNT) {
                continue;
            }
            boolean upgraded = inHand.name().endsWith("+");
            Card cheaper = CardLibrary.resolve(CardLibrary.magicTrickId(next), upgraded ? 1 : 0);
            if (cheaper != null) {
                transformInHand(state.combatant(), i, cheaper);
                log.log("  【魔术技巧！】费用 -1（当前 " + cheaper.cost() + " 费）");
            }
        }
    }

    /**
     * 确认"下一位还没确认结束回合的存活玩家"结束回合（v0.5.1.6 起"结束回合"是**逐人确认**）。
     *
     * <p>回合模型改造之后，"结束回合"不再等于"把回合交给下一个人" —— 所有玩家在同一个玩家回合里
     * 各打各的牌，每个人各自确认；全员确认后才进入敌怪回合。单人战斗里两者完全一致，
     * 所以这个无参版本仍然是自检与指令的通用入口。</p>
     */
    public void endTurn() {
        PlayerState state = nextUnconfirmedPlayer(0);
        if (state == null) {
            log.log("现在不是玩家回合，无法结束回合");
            return;
        }
        confirmEndTurn(state.combatant());
    }

    /**
     * **某一名玩家**确认结束自己的回合（Minecraft 层按"这个包是谁发的"调用）。
     *
     * @return 这次确认是否生效（已经确认过 / 已倒下 / 不在玩家回合 → 不生效）
     */
    public boolean confirmEndTurn(Combatant who) {
        if (phase != CombatPhase.PLAYER_TURN) {
            log.log("现在不是玩家回合，无法结束回合");
            return false;
        }
        PlayerState state = stateOf(who);
        if (state == null) {
            return false;
        }
        if (!state.alive() || state.turnDone) {
            return false;
        }
        if (state.pendingPick != null) {
            // v0.5.1.7：选牌没完成就不让结束回合（否则候选下标会错位）
            log.log("「" + state.pendingPick.cardName() + "」还在等你选牌，先选完再结束回合");
            return false;
        }
        finishPlayerTurn(state);
        afterPlayerConfirmed();
        return true;
    }

    /**
     * 一名玩家"结束出牌环节"的完整结算（v0.5.1.6 从原来的 {@code endTurn()} 里抽出来）。
     *
     * <p>顺序保持原样：神龟给格挡 → 中毒 → 状态牌的回合结束效果 → 弃手牌 → 状态衰减 →
     * 凋零处决 → 回合结束类遗物 → 充能球被动。</p>
     */
    private void finishPlayerTurn(PlayerState state) {
        state.turnDone = true;
        // v0.5.3.3（A4-13）【双重施法】/【爆发】的额度是"本回合内"，回合结束就作废
        state.clearDoublePlays();
        // v0.5.3.6（A4-15）【狂怒】是"本回合"的
        state.clearTurnPowers();
        // v0.5.4.0（A4-19）【幽魂形态】：**回合结束时**失去 1 点敏捷（原版措辞是 ターン終了時）
        if (state.wraithForm && state.combatant().alive()) {
            state.combatant().addDexterity(-1);
            log.log("  【幽魂形态】：失去 1 点敏捷（当前 " + state.combatant().dexterity() + "）");
        }
        // v0.5.3.3（A4-12）**回合钩子（回合结束）**：金属化给格挡、自燃自伤并烧全体；
        // 顺便把【火焰屏障】那种"只这一回合有效"的荆棘收回来。
        // 放在最前面：卡面写的就是"回合结束时"，而且给的格挡必须能挡住接下来的敌人回合。
        onTurnEndForPowers(state);
        checkBattleEnd();
        if (phase.isOver()) {
            return;
        }
        // V5：神龟在"结束出牌"时给格挡 —— 先给，才能挡住接下来的敌人回合
        grantPlatedArmorBlock(state.combatant());
        // v0.4.2 的"涂毒"在 v0.4.6 更名为【中毒】，仍在"出牌环节结束时"结算（【触媒】会额外触发）
        applyPoisonDamage(state);
        // v0.0.3 W2：状态牌的"回合结束"效果（灼烧/毒液造成伤害、呼唤失去生命值），在弃手牌之前结算
        applyStatusCardEndOfTurn(state);
        discardHand(state);
        state.combatant().decayStatuses();
        // V5：中毒处决判定（层数大于当前生命 → 立刻死亡）
        applyDecayExecutions();
        checkBattleEnd();
        if (phase.isOver()) {
            return;
        }
        // V7：回合结束类遗物（在进入敌人回合之前，所以给的格挡能生效）
        fireRelics(RelicTrigger.TURN_END, state);
        // v0.0.3 W4：充能球的被动在回合结束时依次触发（之后才轮到敌人行动）
        activateOrbPassives(state.combatant());
        checkBattleEnd();
    }

    /**
     * 有人确认之后：看是不是**全员都确认了**；是就进敌怪回合，否则只更新"下一位"指针。
     *
     * <p>这就是需求里那句"直到所有玩家都确认【结束回合】，才会进入敌怪的攻击回合"。</p>
     */
    private void afterPlayerConfirmed() {
        if (phase.isOver() || allPlayersDown()) {
            defeat();
            return;
        }
        if (allPlayersConfirmed()) {
            advanceRound();
            return;
        }
        activeIndex = indexOfNextUnconfirmed();
    }

    /** 存活玩家是否**全部**确认结束回合（倒下的玩家视为已确认，所以不挡推进）。 */
    public boolean allPlayersConfirmed() {
        for (PlayerState state : players) {
            if (state.alive() && !state.turnDone) {
                return false;
            }
        }
        return true;
    }

    /** 下一个还没确认结束回合的存活玩家（从 {@code from} 开始找，找不到返回 {@code null}）。 */
    private PlayerState nextUnconfirmedPlayer(int from) {
        for (int i = Math.max(0, from); i < players.size(); i++) {
            PlayerState state = players.get(i);
            if (state.alive() && !state.turnDone) {
                return state;
            }
        }
        return null;
    }

    private int indexOfNextUnconfirmed() {
        for (int i = 0; i < players.size(); i++) {
            PlayerState state = players.get(i);
            if (state.alive() && !state.turnDone) {
                return i;
            }
        }
        return activeIndex;
    }

    /** 全员确认之后的推进：进入敌怪回合。 */
    private void advanceRound() {
        if (allPlayersDown()) {
            defeat();
            return;
        }
        // v0.4.0：敌人回合可能是"一个一个来"的（游戏内），也可能一次跑完（自检/指令）
        runEnemyTurn();
    }

    /**
     * 开启一整轮**玩家回合**（v0.5.1.6 回合模型改造）。
     *
     * <p>需求原文："不再让玩家依次出牌。所有玩家的回合归纳为『玩家回合』，多个玩家可以同时出牌，
     * 按照出牌时间顺序结算打出的牌。直到所有玩家都确认【结束回合】，才会进入敌怪的攻击回合。"</p>
     *
     * <p>所以这里**一次把全部存活玩家的回合都开起来**（各自重置能量/格挡、各自抽手牌、
     * 各自触发回合开始类遗物），然后等他们各自确认；出牌本身由
     * {@link #playCard(Combatant, int, int)} 按到达顺序结算，谁先发到服务端谁先结算。</p>
     */
    private void beginPlayerRound() {
        // v0.4.6.7 嗅探兽"在每回合的出牌环节开始前获得 30 格挡" —— "出牌环节"现在整轮只有一次，
        // 所以这里只在轮首调用一次（旧模型是每个玩家的回合各调一次）。
        sleepingEnemiesGainBlock();
        phase = CombatPhase.PLAYER_TURN;
        List<PlayerState> skipped = new ArrayList<>();
        for (PlayerState state : players) {
            if (!state.alive()) {
                // 倒下的玩家视为已确认，不挡推进
                state.turnDone = true;
                continue;
            }
            // v0.4.7.5 紧急避险：上一回合挂上的"晕眩"在这一轮生效 —— 整轮跳过
            if (state.skipTurns > 0) {
                state.skipTurns--;
                state.turnDone = true;
                log.log("  " + state.combatant().name() + " 处于晕眩：跳过本回合（剩余跳过 "
                        + state.skipTurns + " 回合）");
                skipped.add(state);
                continue;
            }
            beginPlayerTurn(state);
            checkBattleEnd();
            if (phase.isOver()) {
                return;
            }
        }
        // 被跳过的人也要走一遍"回合结束"的结算（状态衰减 / 弃手牌 / 回合结束类遗物 / 充能球被动）
        // —— 与旧模型里"跳过分支直接 endTurn()"的行为一致。
        for (PlayerState state : skipped) {
            finishPlayerTurn(state);
            checkBattleEnd();
            if (phase.isOver()) {
                return;
            }
        }
        // 全员都没法行动（全被跳过）→ 直接进敌怪回合，不要卡住
        if (allPlayersConfirmed()) {
            advanceRound();
        } else {
            activeIndex = indexOfNextUnconfirmed();
        }
    }

    /**
     * 一名玩家的**回合开始**（从旧的 {@code beginActivePlayerTurn} 拆出来，逻辑原样保留）。
     *
     * <p>被持续伤害打倒时只把自己标成"已确认"，不推进整轮 —— 其他人还要照常出牌。</p>
     */
    private void beginPlayerTurn(PlayerState state) {
        state.turnDone = false;
        Combatant actor = state.combatant();
        // v0.5.3.0【荧光墨囊】：剩下的费用不重置，可以和下一回合累加（上限 = 每回合费用的 2 倍）
        if (actor.energyCarryover()) {
            int cap = state.energyPerTurn() * 2;
            int carried = Math.min(cap, state.energy + state.energyPerTurn());
            if (state.energy > 0) {
                log.log("  【荧光墨囊】上回合剩余 " + state.energy + " 点费用留到这一回合（上限 "
                        + cap + "）");
            }
            state.energy = carried;
        } else {
            state.energy = state.energyPerTurn();
        }
        state.cardsPlayedThisTurn = 0;
        // v0.5.3.6（A4-15）【终结技】要数"本回合打出的攻击牌"，每个回合重置
        state.attacksPlayedThisTurn = 0;
        // v0.5.3.8（A4-17）【剔骨】要数"本回合弃掉的张数"
        state.cardsDiscardedThisTurn = 0;
        // v0.3.0：等离子球的被动在**回合开始时**给能量（原版行为；不占"回合结束被动"的位置）
        int plasma = actor.countOrbs(Orb.PLASMA);
        if (plasma > 0) {
            state.energy += plasma * Orb.PLASMA.passiveEnergy();
        }
        // v0.5.3.5（A4-14）【机动】【飞膝】：上回合攒下的"下回合额外能量"
        int pendingEnergy = actor.takeNextTurnEnergy();
        if (pendingEnergy > 0) {
            state.energy += pendingEnergy;
            log.log("  上回合预留的能量生效：+" + pendingEnergy + "（当前 " + state.energy + "）");
        }
        // v0.5.0.3 壁垒：格挡不会在回合开始时消失
        // v0.5.3.6（A4-15）【模糊】：只保"下一次"回合开始的格挡（可叠加）
        if (actor.barricade()) {
            // 永久保留
        } else if (actor.blurTurns() > 0) {
            actor.consumeBlurTurn();
            log.log("  【模糊】：这一回合不清空格挡（保留 " + actor.block() + "，剩余 "
                    + actor.blurTurns() + " 次）");
        } else {
            actor.clearBlock();
        }
        // v0.5.1.4：附甲在常规状态下**每个玩家回合开始时 -1 层**（用户拍板的规则）；
        // 【村庄英雄】开着时锁住不降。放在这里是因为它属于"回合开始的状态处理"。
        if (actor.decayPlatedArmor() > 0) {
            log.log("  附甲衰减：-1 层（当前 " + actor.platedArmor() + "）");
        }
        // v0.0.3 W3：上一回合留下的"下回合格挡"（寻找掩体）在清空格挡之后补上
        int pendingBlock = actor.takeNextTurnBlock();
        if (pendingBlock > 0) {
            int gained = previewBlock(actor, pendingBlock);
            gainBlockFor(actor, gained);
            log.log("  上回合预留的格挡生效：获得 " + gained + " 点格挡（当前 " + actor.block() + "）");
        }
        // v0.4.9.5 创造力：回合开始随机给 1 张能力牌
        if (actor.creativeAI()) {
            Card[] pool = {CardLibrary.BUFFER, CardLibrary.DEFRAGMENT, CardLibrary.ELECTRODYNAMICS};
            Card gift = pool[nextRandomInt(pool.length)];
            addCardsToHand(actor, gift, 1);
            log.log("  【创造力】获得「" + gift.name() + "」");
        }
        // v0.4.9.5 循环：回合开始触发充能球被动 N 次
        for (int i = 0; i < actor.loopCount(); i++) {
            activateOrbPassives(actor);
        }
        // v0.5.3.3（A4-12）**回合钩子（回合开始）**：无限刀刃给一张『小刀』、毒雾给全体中毒。
        // 放在抽手牌**之前**：小刀会先入手，再和这一回合抽到的牌一起摆在手里。
        onTurnStartForPowers(state);
        checkBattleEnd();
        if (phase.isOver()) {
            return;
        }
        actor.resetEvokeUsedThisTurn();   // v0.4.9.4 增幅器：每回合重新给一次
        log.log("── 第 " + turn + " 回合 · " + actor.name() + " 的回合 ── 能量 " + state.energy + "/"
                + state.energyPerTurn() + "，HP " + actor.hp() + "/" + actor.maxHp() + actor.statusSuffix());
        // V7：回合开始类遗物（能量与格挡已重置，抽牌之前）
        fireRelics(RelicTrigger.TURN_START, state);
        // V7：战斗开始类遗物（只在第一个回合；放在清空格挡之后，这样"锚"的格挡不会被抹掉）
        if (!state.firstTurnDone) {
            state.firstTurnDone = true;
            fireRelics(RelicTrigger.BATTLE_START, state);
            // V4：固有牌在自己的第一回合最先入手，且不占固定抽牌数
            drawInnateCards(state);
        }
        // v0.4.6【力量药水】：临时力量在下个回合开始时收回
        int tempStrength = actor.takeTemporaryStrength();
        if (tempStrength > 0) {
            log.log("  【力量药水】效果结束：失去 " + tempStrength + " 点力量（当前 +" + actor.strength() + "）");
        }
        // v0.4.6【不祥之瓶】：每回合开始时给自己叠 5 层【凋零】（累计大于当前生命就死）
        if (actor.ominous()) {
            actor.addDecay(5);
            log.log("  【不祥之瓶】：获得凋零 5" + actor.statusSuffix());
        }
        // v0.4.6【再生】：回合开始时恢复等同于层数的生命（在持续伤害之前结算）
        if (actor.regeneration() > 0 && actor.alive()) {
            int before = actor.hp();
            actor.heal(actor.regeneration());
            int healed = actor.hp() - before;
            log.log("  【再生】生效：" + actor.name() + " 回复 " + healed + " 点生命（当前 "
                    + actor.hp() + "/" + actor.maxHp() + "）");
        }
        // V5：回合开始的持续伤害（灼烧/凋零）
        applyDamageOverTime(actor);
        checkBattleEnd();
        if (phase.isOver()) {
            return;
        }
        if (!actor.alive()) {
            // 持续伤害把这名玩家击倒：他这一轮就不用出牌了（但整轮照常继续）
            log.log("  " + actor.name() + " 倒下了，跳过其回合");
            state.turnDone = true;
            return;
        }
        // v0.4.6：【嚒咕菲力】这类遗物会让抽牌数变少
        // v0.5.3.9（A4-18）【噩梦】：上回合选中的牌，复制 N 张在这个回合开始时入手
        List<Card> gifts = state.takeNextTurnCards();
        for (Card gift : gifts) {
            addCardsToHand(actor, gift, 1);
        }
        if (!gifts.isEmpty()) {
            log.log("  【噩梦】：本回合开始加入 " + gifts.size() + " 张「" + gifts.get(0).name() + "」的复制");
        }
        // v0.5.3.5（A4-14）【捕食者】：上回合攒下的"下回合额外抽牌"也一起抽
        drawCards(state, Math.max(0, rules.handSize() - actor.drawPenalty() + actor.takeNextTurnDraw()));
        // v0.5.4.1（A4-20）【行业工具】：抽 1 张、弃 1 张。
        // 放在**正常抽牌之后**：原版这个能力也是手牌抽齐了才触发，不然"弃 1 张"常常没牌可弃。
        applyToolsOfTheTrade(state);
        log.log("  手牌：" + state.describeHand());
        for (Enemy enemy : aliveEnemies()) {
            log.log("  " + describeEnemy(enemy));
        }
    }

    /**
     * 敌人回合（v0.4.0 起支持**一个一个来**）。
     *
     * <p>同步模式（默认，自检与指令用）：一次跑完全部敌人，然后直接进入下一回合。<br>
     * 分步模式（游戏内，{@link #setPacedEnemyTurn(boolean)}）：先摆好队列就返回，
     * 由 Minecraft 层每隔若干 tick 调一次 {@link #stepEnemyTurn()}，这样每个敌人的
     * 动作动画才能**从左到右依次播放**（需求原文）。</p>
     */
    private void runEnemyTurn() {
        phase = CombatPhase.ENEMY_TURN;
        log.log("── 敌人回合 ──");
        // v0.5.3.0【龙蛋】：每个敌人回合重新计算受伤额度
        // v0.5.3.1【商队护送】：同一时刻清掉"本回合被打穿格挡"的旗标
        for (Combatant combatant : combatants()) {
            combatant.resetDamageTakenThisTurn();
            combatant.resetEscortBreached();
        }
        buildEnemyQueue();
        if (pacedEnemyTurn) {
            enemyTurnRunning = !enemyQueue.isEmpty();
            if (!enemyTurnRunning) {
                finishEnemyTurn();
            }
            return;
        }
        while (enemyTurnRunning) {
            stepEnemyTurn();
        }
    }

    /** 按"左到右"排好本次敌人回合的行动顺序（没被指定过就按敌人表的顺序）。 */
    private void buildEnemyQueue() {
        enemyQueue.clear();
        for (Enemy enemy : enemyOrder) {
            if (enemies.contains(enemy) && enemy.alive()) {
                enemyQueue.add(enemy);
            }
        }
        for (Enemy enemy : enemies) {
            if (enemy.alive() && !enemyQueue.contains(enemy)) {
                enemyQueue.add(enemy);
            }
        }
        enemyTurnRunning = !enemyQueue.isEmpty();
    }

    /**
     * 执行队列里**下一只**敌人的完整行动。
     *
     * @return 是否还有敌人待行动（false = 敌人回合结束，回合已经推进到下一轮玩家）
     */
    public boolean stepEnemyTurn() {
        if (!enemyTurnRunning) {
            return false;
        }
        Enemy enemy = null;
        while (!enemyQueue.isEmpty() && enemy == null) {
            Enemy next = enemyQueue.remove(0);
            if (next.alive()) {
                enemy = next;
            }
        }
        if (enemy != null) {
            enemy.clearBlock();
            // V5：敌人自己回合开始的持续伤害
            applyDamageOverTime(enemy);
            checkBattleEnd();
            if (phase.isOver()) {
                enemyTurnRunning = false;
                return false;
            }
            if (enemy.alive()) {
                log.log("  " + enemy.name() + " 行动（第 " + (enemyQueue.size() + 1) + " / "
                        + enemies.size() + " 只）");
                executeEnemyTurnActions(enemy);
            }
        }
        if (allPlayersDown()) {
            enemyTurnRunning = false;
            defeat();
            return false;
        }
        if (phase.isOver()) {
            enemyTurnRunning = false;
            return false;
        }
        if (enemyQueue.isEmpty()) {
            enemyTurnRunning = false;
            finishEnemyTurn();
            return false;
        }
        return true;
    }

    /** 敌人回合的收尾：状态衰减、中毒处决、重抽意图，然后进入下一轮的第一名玩家。 */
    private void finishEnemyTurn() {
        // v0.5.3.1【商队护送】：敌人的行动已经全部走完，这里就是"本回合结束"的判定点
        resolveEscorts();
        for (Enemy enemy : enemies) {
            if (enemy.alive()) {
                enemy.decayStatuses();
            }
        }
        // V5：敌人回合结束后的中毒处决判定
        applyDecayExecutions();
        checkBattleEnd();
        if (phase.isOver()) {
            return;
        }
        assignIntents();
        // 一轮结束：回合数 +1，从第一名存活玩家重新开始
        turn++;
        activeIndex = firstLivingPlayer(0);
        if (activeIndex < 0) {
            defeat();
            return;
        }
        beginPlayerRound();
    }

    /**
     * 【灼烧】自己回合开始时受到等同层数的伤害，**无视格挡**。
     *
     * <p>v0.4.6 需求："灼烧：可以累加。每累计一点，在下回合开始时，造成对应层数的伤害。"
     * 于是旧的"凋零 DOT"被并进灼烧（两者本来就是同一套规则），这里只剩一个 DOT。</p>
     *
     * <p>是否每回合递减 1 由 {@link CombatRules#dotDecaysEachTurn()} 决定（默认按需求字面意思：不递减）。</p>
     */
    private void applyDamageOverTime(Combatant combatant) {
        int burn = combatant.burn();
        if (burn <= 0) {
            return;
        }
        loseHp(combatant, burn, "持续伤害（灼烧 " + burn + "）");
        if (rules.dotDecaysEachTurn()) {
            combatant.addBurn(-1);
        }
    }

    /**
     * **失去生命值**（v0.0.3 新增，与"造成伤害"区分）：无视格挡，直接扣当前生命。
     *
     * <p>需求："对玩家造成伤害可以被格挡抵消；失去生命值不能被抵消，会直接扣除当前生命值。"
     * 灼烧/凋零/中毒处决、状态牌【呼唤】等都走这里；敌人攻击与卡牌伤害仍走 {@link #dealDamage}。</p>
     *
     * @return 实际失去的生命值
     */
    public int loseHp(Combatant target, int amount, String label) {
        // v0.4.7.8 不屈：玩家 HP 即将 <= 0 时，先看手里有没有【不屈】
        // v0.5.0.3 破灭：因自己的牌失去生命时获得力量
        if (isPlayer(target) && amount > 0 && target.rupture() > 0) {
            target.addStrength(target.rupture());
            log.log("  【破灭】获得 " + target.rupture() + " 点力量（当前 +" + target.strength() + "）");
        }
        if (tryLastStand(target, amount)) {
            return 0;
        }
        int before = target.hp();
        target.loseHp(amount);
        int lost = before - target.hp();
        if (lost > 0) {
            // v0.5.3.8（A4-17）E5【血债血偿】/【高超打击】：失去生命（含持续伤害 / 自伤）也算一次
            if (isPlayer(target)) {
                PlayerState victimState = stateOf(target);
                if (victimState != null) {
                    victimState.recordHpLoss();
                }
            }
            // v0.2.8：失去生命也算"掉血"，第一次同样触发遗物
            notifyHpLost(target);
            log.log("  " + target.name() + " 失去 " + lost + " 点生命值（" + label + "，无视格挡），HP "
                    + target.hp() + "/" + target.maxHp());
            // v0.3.0：失去生命值也是被打了 —— 玩家照样做受击动画（无视格挡，所以没有"被挡住"分支）
            if (isPlayer(target)) {
                emitFx(CombatFx.PLAYER_HURT, null, target);
            }
        } else if (amount > 0) {
            log.log("  " + target.name() + " 已经空血，" + label + " 没有造成额外损失");
        }
        if (!target.alive() && before > 0) {
            log.log("  " + target.name() + " 被" + label + "击倒！");
        }
        checkBattleEnd();
        return lost;
    }

    /**
     * 【中毒】结算（v0.4.6 更名，机制同 v0.4.2 的"涂毒"）：出牌环节结束时，
     * 每有一层中毒就受到 1 点伤害（触发次数由【触媒】决定），每个新回合 -1 层。
     *
     * <p>需求原文："中毒：每拥有 x 点中毒，出牌环节结束时，受到 X 点伤害。每一个新的回合会减少 1 层数。"</p>
     */
    private void applyPoisonDamage(PlayerState state) {
        int triggers = 1 + state.poisonExtraTriggers();
        boolean any = false;
        for (Enemy enemy : enemies) {
            if (!enemy.alive() || enemy.poison() <= 0) {
                continue;
            }
            if (!any) {
                log.log("  ── 中毒结算（触发 " + triggers + " 次）──");
                any = true;
            }
            for (int i = 0; i < triggers && enemy.alive(); i++) {
                takeFixedDamage(enemy, enemy.poison(), "「中毒」");
            }
        }
    }

    /** 神龟：结束出牌时获得等同层数的格挡。 */
    private void grantPlatedArmorBlock(Combatant actor) {        if (actor.platedArmor() <= 0) {
            return;
        }
        int gained = previewBlock(actor, actor.platedArmor());
        gainBlockFor(actor, gained);
        log.log("  " + prefix(actor) + "神龟生效，获得 " + gained + " 点格挡（当前 " + actor.block() + "）");
    }

    /**
     * 【凋零】：层数**大于**当前生命值 → 回合结束后立刻死亡（玩家与敌人一视同仁）。
     *
     * <p>v0.4.6 需求："凋零：可以累加。当对应角色/敌人累计的中毒大于当前生命值，回合结束后会立刻杀死该敌人。"
     * （需求沿用了旧称呼"中毒" —— 也就是 v0.4.6 之前 {@code poison} 的那套规则。）</p>
     */
    private void applyDecayExecutions() {
        for (PlayerState state : players) {
            executeIfDecayLethal(state.combatant());
        }
        for (Enemy enemy : enemies) {
            executeIfDecayLethal(enemy);
        }
    }

    private void executeIfDecayLethal(Combatant combatant) {
        if (!combatant.alive() || combatant.decay() <= 0) {
            return;
        }
        if (combatant.decay() > combatant.hp()) {
            log.log("  " + combatant.name() + " 凋零发作（凋零 " + combatant.decay() + " > HP " + combatant.hp()
                    + "），立刻死亡");
            combatant.setHp(0);
        }
    }

    /**
     * 执行某只敌人**本回合的全部动作**（v0.4.5 意图状态机）。
     *
     * <p>一次敌人行动可以包含多个动作，按状态机里写的顺序依次结算，例如僵尸第二回合
     * "伤害 2×3，然后对玩家施加虚弱 1"。玩家被打倒 / 战斗结束会立刻中断剩余动作。</p>
     */
    private void executeEnemyTurnActions(Enemy enemy) {
        // v0.4.6.7 嗅探兽：睡眠中"不会执行任何行动"（每回合出牌环节开始前已经在堆格挡）
        if (enemy.sleeping()) {
            enemy.spendSleepTurn();
            log.log("  " + enemy.name() + " 还在睡觉（剩余 " + enemy.sleepTurns() + " 回合），这一回合不行动");
            return;
        }
        List<Intent> actions = enemy.actions();
        if (actions.isEmpty()) {
            actions = enemy.rollActions();
        }
        int total = actions.size();
        // v0.4.6.7："A 或 B"的写法 = 条件成立就用 A 并跳过紧接着的 B（见 Intent.summonIfFew）
        boolean skipNext = false;
        for (int i = 0; i < total; i++) {
            if (!enemy.alive() || phase.isOver()) {
                return;
            }
            if (skipNext) {
                skipNext = false;
                continue;
            }
            if (total > 1) {
                log.log("  " + enemy.name() + " 动作 " + (i + 1) + "/" + total + "：" + actions.get(i).label());
            }
            if (executeIntent(enemy, actions.get(i)) && i + 1 < total) {
                skipNext = true;
            }
        }
    }

    /**
     * 执行一个动作。
     *
     * @return true = 这个动作"成立并取代了同一回合的下一个动作"（条件召唤）；其余情况 false
     */
    private boolean executeIntent(Enemy enemy, Intent intent) {
        Combatant target = enemy.target();
        if (needsPlayerTarget(intent)) {
            if (target == null || !target.alive() || !isParticipant(target)) {
                target = randomLivingPlayer();
                enemy.setTarget(target);
            }
            if (target == null) {
                log.log("  " + enemy.name() + " 找不到目标，动作取消");
                return false;
            }
        }
        // 负面效果要交给 lambda 执行，而 target 在上面被重新赋过值 —— 这里固定一份 final 引用
        final Combatant victim = target;
        switch (intent.type()) {
            case ATTACK -> {
                // v0.5.0.5：多段攻击要按段数结算伤害，并且**一段一次广播**，
                // 客户端才有机会排队播 N 次动画（需求："应为攻击多少次就播放多少次动画"）。
                // 注意每段都把 pendingHits 复位成 1：段数由这里的循环表达，不能再让
                // emitDamageFx 也乘一遍（否则 2×3 会变成 4 次动画，自检抓过这个 bug）。
                int hits = Math.max(1, intent.times());
                for (int hit = 0; hit < hits && target.alive(); hit++) {
                    resetPendingHits();
                    dealDamage(enemy, target, intent.value(), enemy.name() + " 的" + intent.label());
                }
                resetPendingHits();
            }
            case DEFEND -> {
                // V5：格挡要经过迅捷/缓慢
                int gained = previewBlock(enemy, intent.value());
                gainBlockFor(enemy, gained);
                log.log("  " + enemy.name() + " 获得 " + gained + " 点格挡（当前 " + enemy.block() + "）");
                // v0.4.1：敌人获得格挡也要有和玩家一样的动作 + 音效
                emitFx(CombatFx.ENEMY_BLOCK, enemy, null);
            }
            case BUFF_STRENGTH -> {
                enemy.addStrength(intent.value());
                log.log("  " + enemy.name() + " 力量 +" + intent.value() + "（当前 +" + enemy.strength() + "）");
                // v0.5.0.5：敌人给自己上增益也是有"仪式动作"的魔法，同样播施法表现
                emitFx(CombatFx.ENEMY_CAST, enemy, enemy);
            }
            case DEBUFF_MINING_FATIGUE -> {
                // v0.5.1.4：这条意图原来是"给予虚弱"，现在给的是【挖掘疲劳】（旧的 -25% 机制）
                if (applyDebuff(victim, "挖掘疲劳 " + intent.value(),
                        () -> victim.addMiningFatigue(intent.value()))) {
                    log.log("  " + victim.name() + " 陷入挖掘疲劳 " + intent.value() + "（接下来的 "
                            + intent.value() + " 回合攻击伤害 -25%）");
                    emitFx(CombatFx.ENEMY_CAST, enemy, victim);
                }
            }
            case DEBUFF_VULNERABLE -> {
                if (applyDebuff(victim, "易伤 " + intent.value(),
                        () -> victim.addVulnerable(intent.value()))) {
                    log.log("  " + victim.name() + " 陷入易伤 " + intent.value()
                            + " 回合（受到伤害 +25%）");
                    emitFx(CombatFx.ENEMY_CAST, enemy, victim);
                }
            }
            case DEBUFF_BURN -> {
                if (applyDebuff(victim, "灼烧 " + intent.value(), () -> victim.addBurn(intent.value()))) {
                    if (victim.burn() > 0) {
                        log.log("  " + victim.name() + " 被附加灼烧 " + intent.value()
                                + "（下回合开始受到等量伤害）" + target.statusSuffix());
                    } else {
                        log.log("  " + victim.name() + " 免疫灼烧（" + victim.factionLabel() + "）");
                    }
                    emitFx(CombatFx.ENEMY_CAST, enemy, victim);
                }
            }
            case DEBUFF_DECAY -> {
                if (applyDebuff(victim, "凋零 " + intent.value(), () -> victim.addDecay(intent.value()))) {
                    log.log("  " + victim.name() + " 被附加凋零 " + intent.value()
                            + "（累计大于当前生命时回合结束即死）" + target.statusSuffix());
                    emitFx(CombatFx.ENEMY_CAST, enemy, victim);
                }
            }
            case DEBUFF_POISON -> {
                if (applyDebuff(victim, "中毒 " + intent.value(), () -> victim.addPoison(intent.value()))) {
                    log.log("  " + victim.name() + " 被附加中毒 " + intent.value()
                            + "（出牌环节结束时按层数受伤，每回合 -1 层）" + target.statusSuffix());
                    emitFx(CombatFx.ENEMY_CAST, enemy, victim);
                }
            }
            case DEBUFF_SLOW -> {
                if (applyDebuff(victim, "缓慢 " + intent.value(), () -> victim.addSlow(intent.value()))) {
                    log.log("  " + victim.name() + " 被附加缓慢 " + intent.value()
                            + "（每层获得格挡 -1）" + target.statusSuffix());
                    emitFx(CombatFx.ENEMY_CAST, enemy, victim);
                }
            }

            // ------------------------------------------------ v0.4.6.7 新增动作
            case HEAL -> {
                int before = enemy.hp();
                enemy.heal(intent.value());
                log.log("  " + enemy.name() + " 恢复 " + (enemy.hp() - before) + " 点生命（HP "
                        + enemy.hp() + "/" + enemy.maxHp() + "）");
                // v0.5.0.5：治疗也是"施法"，同样给一个表现
                emitFx(CombatFx.ENEMY_CAST, enemy, enemy);
            }
            case BUFF_CHARGE -> {
                enemy.addCharge(intent.value());
                log.log("  " + enemy.name() + " 蓄能 +" + intent.value() + "（当前 " + enemy.charge()
                        + " 层，死亡前会一次性打出来）");
            }
            case SUMMON -> summonFor(enemy, intent.payload(), intent.value(), 0);
            case SUMMON_IF_FEW -> {
                if (summonFor(enemy, intent.payload(), intent.value(), intent.times())) {
                    // 条件成立 → 这一支生效，跳过同一回合的下一个动作（那是"或"的另一支）
                    return true;
                }
            }
            case LOCK -> {
                // v0.5.0.8 需求（用户例子）：击晕后的**第一次**敌人阶段不结算，让玩家还能走完
                // 下一个出牌阶段；再一次敌人阶段才引爆。
                if (enemy.consumeChargeDelay()) {
                    log.log("  " + enemy.name() + " 仍在【晕眩】中蓄能（" + enemy.charge()
                            + " 层），下一次敌人回合自爆");
                    break;
                }
                int charge = enemy.charge();
                log.log("  " + enemy.name() + " 从【晕眩】中醒来：自爆 " + charge + " 层蓄能！");
                // v0.5.0.8 需求："在结算时播放爆炸动画"
                emitFx(CombatFx.ENEMY_EXPLODE, enemy, victim);
                if (charge > 0 && victim != null && victim.alive()) {
                    dealDamage(enemy, victim, charge, enemy.name() + " 的自爆");
                }
                enemy.clearCharge();
                enemy.setHpLocked(false);
                enemy.setHp(0);
                log.log("  " + enemy.name() + " 自爆完毕，倒下了。");
            }

            case DEBUFF_STATUS_CARD -> {
                // v0.0.3 W2：把状态牌塞进目标玩家的弃牌堆（需求：怪物可以把状态牌放进
                // 手牌 / 弃牌堆 / 抽牌堆；这里用弃牌堆，抽到时才会进入手牌）
                PlayerState targetState = stateOf(target);
                Card statusCard = CardLibrary.byId(intent.payload());
                if (targetState == null || statusCard == null) {
                    log.log("  " + enemy.name() + " 想塞状态牌，但目标或卡牌无效");
                    break;
                }
                // v0.4.1：塞状态牌也是负面效果 —— 人工造物会免疫第一次
                if (target.consumeDebuffShield()) {
                    log.log("  " + target.name() + " 的【" + target.factionLabel()
                            + "】特性免疫了这次状态牌");
                    break;
                }
                for (int i = 0; i < intent.value(); i++) {
                    targetState.discardPile.add(statusCard);
                }
                log.log("  " + enemy.name() + " 把 " + intent.value() + " 张「" + statusCard.name()
                        + "」塞进了 " + target.name() + " 的弃牌堆");
            }
            case UNKNOWN -> log.log("  " + enemy.name() + " 什么也没做");
        }
        return false;
    }

    /** 睡眠中的敌人获得 {@code SLEEP_BLOCK} 点格挡（v0.4.6.7 嗅探兽）。 */
    public static final int SLEEP_BLOCK = 30;

    /** 每个玩家回合的**出牌环节开始前**：还在睡觉的敌人先堆格挡。 */
    private void sleepingEnemiesGainBlock() {
        for (Enemy enemy : enemies) {
            if (!enemy.alive() || !enemy.sleeping()) {
                continue;
            }
            int gained = previewBlock(enemy, SLEEP_BLOCK);
            gainBlockFor(enemy, gained);
            log.log("  " + enemy.name() + " 在睡眠中蜷缩，获得 " + gained + " 点格挡（当前 " + enemy.block()
                    + "，剩余睡眠 " + enemy.sleepTurns() + " 回合）");
        }
    }

    /**
     * 召唤 {@code count} 只随从（v0.4.6.7）。
     *
     * @param times 触发条件的编码："阈值 + 1"（0 = 无条件召唤；1 = 场上同类归零时才召唤）
     * @return true = 真的召唤出了至少一只（调用方据此决定要不要跳过"或"的另一支）
     */
    private boolean summonFor(Enemy caller, String entityId, int count, int times) {
        if (entityId == null || entityId.isEmpty()) {
            log.log("  " + caller.name() + " 想召唤，但没写要召唤什么");
            return false;
        }
        int alive = countLiving(entityId);
        int atMost = times - 1;
        if (times > 0 && alive > atMost) {
            log.log("  " + caller.name() + " 想召唤，但场上还有 " + alive + " 只同类（条件是只剩 " + atMost
                    + " 只），改用别的动作");
            return false;
        }
        if (summoner == null) {
            log.log("  " + caller.name() + " 想召唤 " + entityId + "，但这一战没有注册召唤处理器");
            return false;
        }
        int summoned = 0;
        for (int i = 0; i < Math.max(1, count); i++) {
            Enemy created = summoner.summon(entityId, caller);
            if (created == null) {
                break;
            }
            addSummonedEnemy(created);
            summoned++;
        }
        if (summoned == 0) {
            log.log("  " + caller.name() + " 的召唤失败了");
            return false;
        }
        log.log("  " + caller.name() + " 召唤了 " + summoned + " 只随从加入战斗！");
        return true;
    }

    /** 场上还活着几只这种实体。 */
    public int countLiving(String entityId) {
        int count = 0;
        for (Enemy enemy : enemies) {
            if (enemy.alive() && entityId.equals(enemy.mobId())) {
                count++;
            }
        }
        return count;
    }

    /** 把召唤出来的敌人加进战场（同时排进"依次行动"的队尾）。 */
    public void addSummonedEnemy(Enemy enemy) {
        if (enemy == null || enemies.contains(enemy)) {
            return;
        }
        enemies.add(enemy);
        enemyOrder.add(enemy);
        enemy.rollActions();
    }

    /** 注册召唤处理器（Minecraft 层在开战时调用）。 */
    public void setSummoner(Summoner handler) {
        this.summoner = handler;
    }

    /**
     * 状态牌在**回合结束**时的结算（v0.0.3 W2）：灼烧 / 毒液对自己造成伤害（可被格挡），
     * 呼唤失去生命值（无视格挡）。手里有几张就结算几次，结算完照常被弃掉（或按关键消耗）。
     */
    /**
     * 把若干张牌塞进某名玩家的**弃牌堆**（v0.0.3 W2：状态牌的调试/事件入口）。
     *
     * @return 给玩家看的提示
     */
    public String insertCards(String playerName, Card card, int count) {
        Combatant combatant = combatantNamed(playerName);
        PlayerState state = combatant == null ? null : stateOf(combatant);
        if (state == null) {
            return "找不到玩家 " + playerName;
        }
        for (int i = 0; i < count; i++) {
            state.discardPile.add(card);
        }
        log.log("  " + playerName + " 的弃牌堆被塞入 " + count + " 张「" + card.name() + "」");
        return "已把 " + count + " 张「" + card.name() + "」放进 " + playerName + " 的弃牌堆（当前弃牌堆 "
                + state.discardPile.size() + " 张）";
    }

    /**
     * 把**手牌**第 {@code index} 张弃掉（v0.5.2.1 A4-6【全力攻击】）。
     *
     * <p>与 {@link #exhaustFromHand(Combatant, java.util.Collection)} 是一对：那个进消耗堆、这个进弃牌堆。
     * 后续【准备 / 杂技 / 飞刀投掷 / 集中精神 / 计算赌博】都要用它（见 docs/a4-card-plan.md 的 E1）。</p>
     *
     * <p>注意 {@link PlayerState#hand()} 返回的是**只读副本**（{@code List.copyOf}），
     * 所以只能在这里通过包内字段改 —— 卡牌效果里 {@code state.hand().remove(...)} 会抛
     * {@code UnsupportedOperationException}。</p>
     *
     * @return 被弃掉的那张牌；没弃成（下标越界 / 不在战斗里）返回 {@code null}
     */
    public Card discardFromHand(Combatant owner, int index) {
        PlayerState state = stateOf(owner);
        if (state == null || index < 0 || index >= state.hand.size()) {
            return null;
        }
        Card card = state.hand.remove(index);
        state.discardPile.add(card);
        state.recordDiscard();
        log.log("  " + owner.name() + " 弃掉了手里的「" + card.name() + "」");
        // v0.5.3.7（A4-16）**弃牌钩子**：【反射】【战术家】挂在这里。
        // 注意"回合结束时弃手牌"走的是 discardHand，**不经过这里** —— 正好符合它们
        // 卡面上那句"在你的回合内被弃置时"。
        CardLibrary.SelfDiscardEffect selfDiscard = CardLibrary.selfDiscard(card.id());
        if (selfDiscard != null && state.alive()) {
            selfDiscard.apply(this, state, card);
        }
        return card;
    }

    private void applyStatusCardEndOfTurn(PlayerState state) {
        // 手牌数量在结算过程中会变（【戏谑】会往弃牌堆加牌、不影响手牌），所以先取一份快照
        List<Card> snapshot = new ArrayList<>(state.hand);
        for (Card card : snapshot) {
            int damage = StatusCards.endOfTurnDamage(card);
            if (damage > 0) {
                takeFixedDamage(state.combatant(), damage, "「" + card.name() + "」");
            }
            int loss = StatusCards.endOfTurnHpLoss(card);
            if (loss > 0) {
                loseHp(state.combatant(), loss, "「" + card.name() + "」");
            }
            // ---- v0.4.6 诅咒（出牌回合结束时还在手里就发作） ----
            int perCard = StatusCards.endOfTurnHpLossPerHandCard(card);
            if (perCard > 0) {
                int handSize = state.hand.size();
                loseHp(state.combatant(), perCard * handSize, "「" + card.name() + "」（手牌 " + handSize + " 张）");
            }
            int goldLoss = StatusCards.endOfTurnGoldLoss(card);
            if (goldLoss > 0) {
                loseGold(state.combatant(), goldLoss);
                log.log("  「" + card.name() + "」发作：" + state.combatant().name() + " 失去 " + goldLoss
                        + " 金币（战斗结束时结算）");
            }
            if (StatusCards.copiesItselfOnEndOfTurn(card)) {
                state.discardPile.add(card);
                log.log("  「" + card.name() + "」发作：复制一张加入弃牌堆");
            }
            int vulnerable = StatusCards.endOfTurnVulnerable(card);
            if (vulnerable > 0) {
                state.combatant().addVulnerable(vulnerable);
                log.log("  「" + card.name() + "」发作：" + state.combatant().name() + " 获得易伤 " + vulnerable
                        + state.combatant().statusSuffix());
            }
            checkBattleEnd();
            if (phase.isOver()) {
                return;
            }
        }
    }

    /**
     * 固定数值的伤害（v0.0.3 W2）：**可以被格挡**，但不受力量/虚弱/易伤影响。
     *
     * <p>给状态牌的自伤用（"对你造成 4 点伤害"写的是固定值，不该被玩家的力量放大）。
     * 与"失去生命值"（{@link #loseHp}）的区别正是本次需求强调的那一条。</p>
     *
     * @return 实际掉的血
     */
    public int takeFixedDamage(Combatant target, int amount, String label) {
        // v0.4.6【缓降】：固定伤害同样被完全免疫
        if (target.absorbWithSlowFall()) {
            log.log("  " + target.name() + " 的【缓降】生效：免疫了这次伤害（" + label + "）"
                    + target.statusSuffix());
            if (isPlayer(target)) {
                emitFx(CombatFx.PLAYER_HURT_BLOCKED, null, target);
            }
            return 0;
        }
        int absorbed = target.absorbWithBlock(amount);
        int hpLoss = amount - absorbed;
        if (hpLoss > 0) {
            // v0.4.9.5 静电放电：玩家受到攻击伤害时生成闪电球
            if (isPlayer(target) && hpLoss > 0 && target.staticDischarge() > 0) {
                for (int i = 0; i < target.staticDischarge(); i++) {
                    channelOrb(target, Orb.LIGHTNING);
                }
                log.log("  【静电放电】生成 " + target.staticDischarge() + " 颗闪电球");
            }
            if (tryLastStand(target, hpLoss)) {
                hpLoss = 0;
            }
            target.loseHp(hpLoss);
            notifyHpLost(target);
        }
        log.log("  " + target.name() + " 受到 " + amount + " 点伤害（" + label
                + (absorbed > 0 ? "，格挡吸收 " + absorbed : "") + "），HP " + target.hp() + "/"
                + target.maxHp());
        if (!target.alive() && amount > 0) {
            log.log("  " + target.name() + " 被" + label + "击倒！");
        }
        // v0.3.0：状态牌自伤也走同一套表现（掉血 = 受击；全被挡 = 盾牌音效）
        if (isPlayer(target)) {
            emitFx(hpLoss > 0 ? CombatFx.PLAYER_HURT : CombatFx.PLAYER_HURT_BLOCKED, null, target);
        }
        checkBattleEnd();
        return hpLoss;
    }

    private void discardHand(PlayerState state) {
        if (state.hand.isEmpty()) {
            return;
        }
        // V4：保留留手、虚无被消耗、其余进弃牌堆
        // v0.5.4.1（A4-20）【计划周全】：额外保留 N 张（见下面的 keepByPlans）
        List<Card> remaining = new ArrayList<>(state.hand);
        state.hand.clear();
        // 【计划周全】的额度：从"最不差"的一头取 N 张（费用最高的最值得留）。
        // ⚠️ 原版是玩家自己选，这里没有选牌弹窗，所以按贪心自动留 —— 与【行业工具】的自动弃牌同一类近似。
        java.util.Set<Integer> keepByPlans = new java.util.HashSet<>();
        if (state.wellLaidPlans > 0) {
            List<Integer> order = new ArrayList<>();
            for (int i = 0; i < remaining.size(); i++) {
                order.add(i);
            }
            // 费用最高的优先留（并列时留靠后的那张，与其它贪心兜底一致）
            order.sort((a, b) -> {
                int costA = remaining.get(a).cost();
                int costB = remaining.get(b).cost();
                return costA != costB ? Integer.compare(costB, costA) : Integer.compare(b, a);
            });
            for (int i = 0; i < order.size() && keepByPlans.size() < state.wellLaidPlans; i++) {
                keepByPlans.add(order.get(i));
            }
        }
        int discarded = 0;
        int retained = 0;
        int exhausted = 0;
        int planned = 0;
        for (int i = 0; i < remaining.size(); i++) {
            Card card = remaining.get(i);
            if (card.has(CardKeyword.RETAIN)) {
                state.hand.add(card);
                retained++;
            } else if (card.has(CardKeyword.ETHEREAL)) {
                exhaustCard(state, card);
                exhausted++;
            } else if (keepByPlans.contains(i)) {
                state.hand.add(card);
                planned++;
            } else {
                state.discardPile.add(card);
                discarded++;
            }
        }
        log.log("  " + prefix(state.combatant()) + "手牌结算：弃掉 " + discarded + " 张"
                + (retained > 0 ? "，保留 " + retained + " 张" : "")
                + (planned > 0 ? "，【计划周全】保留 " + planned + " 张" : "")
                + (exhausted > 0 ? "，虚无消耗 " + exhausted + " 张" : ""));
    }

    /**
     * 固有（INNATE）：第一回合先把它们从抽牌堆捞进手牌，**不占固定抽牌数**，但仍受手牌上限限制
     * （超出上限的直接进弃牌堆）。捞取顺序保持抽牌堆里的先后。
     */
    private void drawInnateCards(PlayerState state) {
        List<Card> innate = state.drawPile.extractMatching(card -> card.has(CardKeyword.INNATE));
        if (innate.isEmpty()) {
            return;
        }
        int drawn = 0;
        for (Card card : innate) {
            if (state.hand.size() >= rules.maxHandSize()) {
                state.discardPile.add(card);
                continue;
            }
            state.hand.add(card);
            drawn++;
        }
        log.log("  固有：" + drawn + " 张牌直接入手（不占固定抽牌数）");
    }

    // ------------------------------------------------------------------ card effects API

    /** Draws up to {@code amount} cards for the active player, reshuffling when the draw pile runs dry. */
    public void drawCards(int amount) {
        drawCards(activeState(), amount);
    }

    /**
     * 给**指定玩家**抽牌（v0.2.8）。
     *
     * <p>以前只有 {@link #drawCards(int)}（永远给"当前行动玩家"抽），多人时遗物/事件给非当前玩家抽牌
     * 就会抽错人 —— 小猪仔这类"自己掉血抽牌"的触发点尤其明显。</p>
     */
    public void drawCardsFor(Combatant owner, int amount) {
        PlayerState state = stateOf(owner);
        if (state != null) {
            drawCards(state, amount);
        }
    }

    private void drawCards(PlayerState state, int amount) {
        // v0.5.4.0（A4-19）【子弹时间】：本回合不能抽牌
        if (state != null && state.bulletTime && amount > 0) {
            log.log("  【子弹时间】：本回合不能抽牌（" + amount + " 张没有抽成）");
            return;
        }
        for (int i = 0; i < amount; i++) {
            if (state.drawPile.isEmpty()) {
                if (state.discardPile.isEmpty()) {
                    log.log("  抽牌堆与弃牌堆均已耗尽，停止抽牌");
                    return;
                }
                List<Card> recycled = state.discardPile.drainAll();
                state.drawPile.addAll(recycled);
                state.drawPile.shuffle(random);
                log.log("  弃牌堆重洗入抽牌堆（" + state.drawPile.size() + " 张）");
            }
            Card card = state.drawPile.draw();
            if (card == null) {
                return;
            }
            if (state.hand.size() >= rules.maxHandSize()) {
                state.discardPile.add(card);
                log.log("  手牌已满（" + rules.maxHandSize() + "），「" + card.name() + "」直接进入弃牌堆");
                continue;
            }
            state.hand.add(card);
            // v0.0.3 W2：抽到状态牌的即时效果（虚空：本回合费用 −1）
            int energyLoss = StatusCards.onDrawEnergyLoss(card);
            if (energyLoss > 0) {
                int actual = Math.min(energyLoss, state.energy);
                state.energy -= actual;
                log.log("  「" + card.name() + "」入手：本回合费用 -" + actual + "（剩余 " + state.energy + "）");
            }
            // v0.5.3.5（A4-14）**抽牌钩子**：【进化】【火焰吐息】挂在"牌真的进了手牌"这一刻
            onCardDrawnForPowers(state, card);
        }
    }

    /**
     * **抽牌钩子**（v0.5.3.5 A4-14）：一张牌**真的进了手牌**之后触发。
     *
     * <p>手牌已满而直接进弃牌堆的那些牌**不算抽到**（与原版一致），所以这个调用点在
     * {@code state.hand.add(card)} 之后、且在"手牌已满"分支之外。</p>
     *
     * <p>目前挂在这里的：</p>
     * <ul>
     *   <li>【进化】：抽到状态牌 → 再抽 N 张（N 张也是抽牌，会**继续触发**本钩子 ——
     *       牌堆有限，所以一定收敛）；</li>
     *   <li>【火焰吐息】：抽到状态 / 诅咒牌 → 对所有敌人造成 N 点伤害。</li>
     * </ul>
     */
    private void onCardDrawnForPowers(PlayerState state, Card card) {
        if (card == null || !state.alive()) {
            return;
        }
        // v0.5.4.0（A4-19）【无尽苦痛】：每抽到一张，就再往手里补一张（补牌不算"抽到"，不会死循环）
        if ("endless_agony".equals(card.id())) {
            int added = addCardsToHand(state.combatant(), card, 1);
            log.log("  【无尽苦痛】：抽到「" + card.name() + "」→ 再入手 " + added + " 张");
        }
        boolean status = card.type() == CardType.STATUS;
        boolean curse = card.type() == CardType.CURSE;
        if (state.evolve > 0 && status) {
            log.log("  【进化】：抽到「" + card.name() + "」→ 再抽 " + state.evolve + " 张");
            drawCards(state, state.evolve);
        }
        if (state.fireBreathing > 0 && (status || curse)) {
            List<Enemy> alive = aliveEnemies();
            if (!alive.isEmpty()) {
                log.log("  【火焰吐息】：抽到「" + card.name() + "」→ " + alive.size() + " 个敌人各受 "
                        + state.fireBreathing + " 点伤害");
                for (Enemy enemy : alive) {
                    dealDamage(state.combatant(), enemy, state.fireBreathing, "「火焰吐息」");
                }
            }
        }
    }

    /**
     * **消耗钩子**（v0.5.3.5 A4-14）：一张牌**进了消耗堆**之后触发。
     *
     * <p>目前挂在这里的：【黑暗拥抱】（再抽 N 张，v0.5.3.6）与【无痛】（获得 N 点格挡）。</p>
     */
    private void onCardExhaustedForPowers(PlayerState state, Card card) {
        if (state == null || card == null || !state.alive()) {
            return;
        }
        if (state.darkEmbrace > 0) {
            log.log("  【黑暗拥抱】：消耗「" + card.name() + "」→ 抽 " + state.darkEmbrace + " 张");
            drawCards(state, state.darkEmbrace);
        }
        if (state.feelNoPain > 0) {
            int gained = previewBlock(state.combatant(), state.feelNoPain);
            gainBlockFor(state.combatant(), gained);
            log.log("  【无痛】：消耗「" + card.name() + "」→ 获得 " + gained + " 点格挡（当前 "
                    + state.combatant().block() + "）");
        }
    }

    /**
     * **把一张牌放进消耗堆**的唯一入口（v0.5.3.5 A4-14）。
     *
     * <p>和 {@link #gainBlockFor} 同一个思路：消耗有三条路（带「消耗」关键字的牌、
     * 从手牌里被效果消耗、【腐化】打出的技能牌），加上虚无牌的回合末消耗与【浩劫】，
     * 全部收口到这里，才好挂【无痛】【黑暗拥抱】这类钩子。</p>
     *
     * <p>v0.5.3.6（A4-15）又多一层：**这张牌自己**被消耗时的效果（【哨兵】回能量）——
     * 由 {@code CardLibrary.selfExhaust(cardId)} 这张 id 表查出来执行。</p>
     */
    private void exhaustCard(PlayerState state, Card card) {
        if (state == null || card == null) {
            return;
        }
        state.exhaustPile.add(card);
        CardLibrary.SelfExhaustEffect selfExhaust = CardLibrary.selfExhaust(card.id());
        if (selfExhaust != null && state.alive()) {
            selfExhaust.apply(this, state, card);
        }
        onCardExhaustedForPowers(state, card);
    }

    /**
     * 预估一次伤害的实际数值（力量 / 虚弱 / 易伤都算进去，但不扣血）。
     *
     * <p>给"敌人头顶意图"用：显示的应是**实际**将造成的伤害，而不是意图里的原始数值。
     * {@link #dealDamage} 也走这里，保证预览与结算永远一致。</p>
     */
    public static int previewDamage(Combatant source, Combatant target, int baseDamage) {
        int damage = previewDamage(source, target.vulnerable() > 0, baseDamage);
        // v0.4.1：三件新遗物的伤害修正（预览与结算共用，头顶意图数字也一致）
        if (source.raidHornActive() && target.faction() == Faction.ILLAGER) {
            // 【袭击号角】：对灾厄部队额外 +10%
            damage = (int) Math.floor(damage * 1.10D);
        }
        if (target.deadCoralActive()) {
            // 【失活的珊瑚】：每次受到的伤害 -1
            damage -= 1;
        }
        if (target.heartOfTheSeaActive() && target.stormy()) {
            // 【海洋之心】：雨天/雷暴时受到的伤害 -10%
            damage = (int) Math.floor(damage * 0.90D);
        }
        return Math.max(0, damage);
    }

    /**
     * 伤害预览（v0.5.1.4 重做）：`(基础 + 力量 − 虚弱) → 挖掘疲劳 ×0.75 → 易伤 ×1.25`，每步向下取整。
     *
     * <p>需求（v0.5.1.4）："虚弱：可以累加。每累计一点，攻击伤害-1。" 所以虚弱从"×0.75"变成了
     * **每层 -1 的加减项**，而且会与力量互相抵消（见 {@link Combatant#addWeak(int)}）；
     * 原来那套"-25% 持续 X 回合"搬到了【挖掘疲劳】身上。</p>
     *
     * @param targetVulnerable 目标是否处于易伤（卡面预览时可以用"中立目标"= false）
     */
    public static int previewDamage(Combatant source, boolean targetVulnerable, int baseDamage) {
        int damage = baseDamage + source.strength() - source.weak();
        if (source.miningFatigue() > 0) {
            damage = (int) Math.floor(damage * WEAK_MULTIPLIER);
        }
        if (targetVulnerable) {
            damage = (int) Math.floor(damage * VULNERABLE_MULTIPLIER);
        }
        return Math.max(0, damage);
    }

    /** 卡面预览用：只把**自己**的状态（力量/虚弱）算进去，假设目标没有易伤。 */
    public static int previewDamageNeutral(Combatant source, int baseDamage) {
        return previewDamage(source, false, baseDamage);
    }

    /**
     * 预估一次"获得格挡"的实际数值（V5）：迅捷每层 +1、缓慢每层 −1，最低 0。
     *
     * <p>战斗里所有"获得格挡"都应该走这里，否则迅捷/缓慢会失效。</p>
     */
    public static int previewBlock(Combatant owner, int baseBlock) {
        return Math.max(0, baseBlock + owner.dexterity() - owner.slow());
    }

    /**
     * Applies a single damage instance: strength and weak modify the attacker's output, vulnerable
     * modifies what the target takes, and the target's block absorbs damage first.
     *
     * @return the HP actually lost
     */
    public int dealDamage(Combatant source, Combatant target, int baseDamage, String label) {
        return dealDamage(source, target, baseDamage, label, true);
    }

    /**
     * 【荆棘】反弹这类**固定数值**的伤害用这个入口：不做力量 / 虚弱 / 易伤换算，也不触发附魔。
     *
     * @param scaled 是否按力量 / 虚弱 / 易伤 / 附魔换算（{@code false} = 原样打出这个数字）
     */
    public int dealDamage(Combatant source, Combatant target, int baseDamage, String label, boolean scaled) {
        // 测试反馈修正：把"谁打的"告诉目标（MobCombatant 用它记录击杀归属，原版无法提供）
        target.recordDamageFrom(source);
        // v0.4.6【缓降】：完全免疫这一次伤害（层数 -1）
        if (target.absorbWithSlowFall()) {
            log.log("  " + target.name() + " 的【缓降】生效：免疫了这次伤害（" + label + "）"
                    + target.statusSuffix());
            emitFx(isPlayer(target) ? CombatFx.PLAYER_HURT_BLOCKED : CombatFx.ENEMY_BLOCK, null, target);
            return 0;
        }
        // v0.0.3：活力只加在**这一张牌**的第一次伤害上，然后清零
        int bonus = 0;
        if (cardBonusDamage > 0) {
            bonus = cardBonusDamage;
            cardBonusDamage = 0;
        }
        int damage = scaled ? previewDamage(source, target, baseDamage + bonus)
                : Math.max(0, baseDamage + bonus);
        // v0.5.4.0（A4-19）【幻影杀手】：生效的那个回合里，攻击牌的伤害翻倍
        if (scaled && resolvingAttackCard) {
            PlayerState attacker = stateOf(source);
            if (attacker != null && attacker.phantomKiller) {
                damage *= 2;
            }
        }
        // v0.5.4.0（A4-19）E15【无形】（【幽魂形态】）：受到的伤害一律变成 1
        if (target.intangibleTurns() > 0 && damage > 1) {
            log.log("  【无形】" + target.name() + " 只受到 1 点伤害（原本 " + damage + "）");
            damage = 1;
        }
        if (bonus > 0) {
            label = label + "（活力 +" + bonus + "）";
        }
        // v0.5.3.0【龙蛋】：每个敌人回合的受伤额度（额度用完就完全免伤）
        if (target.damageCap() > 0) {
            int left = Math.max(0, target.damageCap() - target.damageTakenThisTurn());
            if (damage > left) {
                log.log("  【龙蛋】把这次 " + damage + " 点伤害夹到 " + left + " 点（本回合额度 "
                        + target.damageCap() + "）");
                damage = left;
            }
            target.addDamageTakenThisTurn(damage);
        }
        // V6：亡灵杀手 —— 目标为亡灵时伤害 ×1.2（在目标易伤之后生效）
        if (scaled && activeEnchantments.contains(CardEnchantment.SMITE) && target.undead()) {
            damage = (int) Math.floor(damage * CardEnchantment.SMITE.value());
        }

        int absorbed = target.absorbWithBlock(damage);
        int hpLoss = damage - absorbed;
        boolean defeated = false;
        if (hpLoss > 0) {
            // v0.5.3.1【商队护送】：真的掉了血 = 这一轮格挡被打穿（被格挡全吃掉不算）
            if (isPlayer(target)) {
                target.markEscortBreached();
            }
            // v0.4.6.7【旋风人】锁血：血量"即将低于 1" → 进入【晕眩】，这一击杀不掉它，
            // 当前回合的意图被改成"晕眩"（下一次敌人阶段结算蓄能后自爆死亡）。
            //
            // v0.5.1.0 修：以前这个分支带着 `&& !lockable.hpLocked()`，所以**只有第一次**攻击
            // 会被锁血拦下，第二次攻击就绕过它把怪打死 —— 用户报的"晕眩后没有锁血、受击即死"
            // 正是这个。现在**已锁血时也把伤害夹在"最多打到 1 点"**，
            // 与 Combatant.setHp 里的"锁血下限 1"是同一条不变量的两道保险。
            if (target instanceof Enemy lockable && lockable.hpLock()) {
                boolean entering = !lockable.hpLocked() && target.hp() - hpLoss <= 0;
                boolean alreadyLocked = lockable.hpLocked();
                if (entering || alreadyLocked) {
                    int overkill = hpLoss - (target.hp() - 1);
                    if (overkill > 0) {
                        hpLoss -= overkill;
                    }
                    if (entering) {
                        lockable.setHpLocked(true);
                        lockable.setActions(java.util.List.of(Intent.lock(lockable.charge())));
                        // v0.5.0.8 需求（用户例子）："玩家在第 8 回合击晕了旋风人，
                        // 则旋风人应该在第九回合出牌回合结束后结算蓄能状态。"
                        // —— 所以**本回合的敌人阶段先不爆**（等 1 次），
                        // 让玩家还能走完第 9 回合的出牌阶段，结算发生在第 9 回合结束后的敌人阶段。
                        lockable.setChargeDelay(1);
                        log.log("  " + target.name() + " 的血量即将低于 1 —— 进入【晕眩】锁血状态，无法被击杀"
                                + "（蓄能 " + lockable.charge() + " 层，下一次敌人回合自爆）");
                        emitFx(CombatFx.ENEMY_BLOCK, null, target);
                    } else {
                        log.log("  " + target.name() + " 处于【晕眩】锁血中：这一击无法击杀它（剩 "
                                + Math.max(1, target.hp() - hpLoss) + " 点，蓄能 "
                                + lockable.charge() + " 层）");
                    }
                }
            }
            // v0.4.9.5 静电放电：玩家受到攻击伤害时生成闪电球
            if (isPlayer(target) && hpLoss > 0 && target.staticDischarge() > 0) {
                for (int i = 0; i < target.staticDischarge(); i++) {
                    channelOrb(target, Orb.LIGHTNING);
                }
                log.log("  【静电放电】生成 " + target.staticDischarge() + " 颗闪电球");
            }
            if (tryLastStand(target, hpLoss)) {
                hpLoss = 0;
            }
            target.loseHp(hpLoss);
            // v0.5.3.8（A4-17）E5【血债血偿】/【高超打击】：记一次"玩家失去生命"
            if (isPlayer(target)) {
                PlayerState victimState = stateOf(target);
                if (victimState != null) {
                    victimState.recordHpLoss();
                }
            }
            // v0.4.6.7 嗅探兽：睡梦中被打破格挡并真的掉了血 → 立刻苏醒
            if (target instanceof Enemy sleeper && sleeper.sleeping() && hpLoss > 0) {
                sleeper.wake();
                log.log("  " + target.name() + " 被打醒了！");
            }
            // v0.2.8：本场战斗第一次失去生命 → 广播给遗物（小猪仔）
            notifyHpLost(target);
            defeated = !target.alive();
        }

        StringBuilder line = new StringBuilder("  ").append(label).append(" → ").append(target.name())
                .append(" 造成 ").append(damage).append(" 点伤害");
        if (absorbed > 0) {
            line.append("（格挡吸收 ").append(absorbed).append("）");
        }
        line.append("，HP ").append(target.hp()).append("/").append(target.maxHp());
        log.log(line.toString());
        if (defeated) {
            log.log("  " + target.name() + " 被击败！");
        }

        // V6：火焰附加 —— 被这张牌打到的敌人附加灼烧
        // v0.5.2.5 改：层数 = 造成伤害的四分之一（原来是固定的 1 层）
        int fireBurn = scaled ? fireAspectBurn(damage) : 0;
        if (scaled && activeEnchantments.contains(CardEnchantment.FIRE_ASPECT) && fireBurn > 0 && target.alive()) {
            target.addBurn(fireBurn);
            log.log("  「火焰附加」为 " + target.name() + " 附加灼烧 " + fireBurn + target.statusSuffix());
        }
        // v0.5.3.3 A4-13【涂毒】：攻击牌**真的造成伤害**（被格挡吃光的这一下不算）时给中毒
        if (resolvingAttackCard && hpLoss > 0 && target.alive() && target != source) {
            PlayerState poisoner = stateOf(source);
            if (poisoner != null && poisoner.envenom > 0) {
                target.addPoison(poisoner.envenom);
                log.log("  【涂毒】：" + target.name() + " 获得中毒 " + poisoner.envenom
                        + "（当前 " + target.poison() + "）");
            }
        }
        // V6：横扫之刃 —— 对其他敌人造成本次伤害的 35%
        if (!resolvingSweep && activeEnchantments.contains(CardEnchantment.SWEEPING_EDGE) && damage > 0) {
            applySweepingEdge(source, target, damage);
        }

        // v0.3.0：表现层事件（敌人前冲 / 玩家受击后退 / 完全格挡时的盾牌音效）
        emitDamageFx(source, target, absorbed, hpLoss);
        // v0.5.2.5【荆棘】：被攻击时反弹（不管这一下有没有被格挡挡住）
        if (!resolvingThorns && target.alive() && target.thorns() > 0) {
            applyThorns(target, source);
        }
        checkBattleEnd();
        return hpLoss;
    }

    /**
     * 【火焰附加】要附加的灼烧层数（v0.5.2.5 改）。
     *
     * <p>需求原文："火焰附加：为攻击到的敌人附加效果：灼烧，数值等于造成伤害的四分之一。"
     * 原来是固定 1 层，现在按伤害折算，**向下取整**（不足 4 点伤害就是 0 层，也就是不附加）。</p>
     *
     * <p>判定用的是**这次攻击的伤害数值**（力量 / 虚弱 / 易伤算完之后、格挡吸收之前），
     * 与卡面上写的"造成 N 点伤害"一致。</p>
     */
    public static int fireAspectBurn(int damage) {
        return Math.max(0, damage / 4);
    }

    /**
     * 【荆棘】反弹：被攻击时对攻击者造成等同于层数的伤害（v0.5.2.5）。
     *
     * <p>用 {@code scaled=false} 的入口：**不吃力量 / 虚弱 / 易伤**，但照常被对方的格挡吸收，
     * 也照常走击杀归属（荆棘反杀算玩家的击杀）。用 {@link #resolvingThorns} 防止互相反弹死循环。</p>
     */
    private void applyThorns(Combatant target, Combatant source) {
        int reflect = target.thorns();
        if (reflect <= 0 || source == null || !source.alive() || source == target) {
            return;
        }
        resolvingThorns = true;
        try {
            log.log("  【荆棘】" + target.name() + " 反弹 " + reflect + " 点伤害给 " + source.name());
            dealDamage(target, source, reflect, "「荆棘」", false);
        } finally {
            resolvingThorns = false;
        }
    }

    /**
     * 横扫之刃：对其他存活敌人造成"本次对主目标造成的伤害"的 35%。
     *
     * <p>波及伤害**不再吃力量/虚弱/易伤**（避免同一次附魔被反复加成），但仍然正常被格挡吸收；
     * 若这张牌同时有火焰附加，被波及的敌人也会被附加灼烧。</p>
     */
    private void applySweepingEdge(Combatant source, Combatant mainTarget, int dealtDamage) {
        int swept = (int) Math.floor(dealtDamage * CardEnchantment.SWEEPING_EDGE.value());
        if (swept <= 0) {
            return;
        }
        resolvingSweep = true;
        try {
            for (Enemy enemy : enemies) {
                if (enemy == mainTarget || !enemy.alive()) {
                    continue;
                }
                int absorbed = enemy.absorbWithBlock(swept);
                int loss = swept - absorbed;
                if (loss > 0) {
                    enemy.loseHp(loss);
                }
                log.log("  「横扫之刃」波及 " + enemy.name() + " 造成 " + swept + " 点伤害"
                        + (absorbed > 0 ? "（格挡吸收 " + absorbed + "）" : "")
                        + "，HP " + enemy.hp() + "/" + enemy.maxHp());
                if (activeEnchantments.contains(CardEnchantment.FIRE_ASPECT) && enemy.alive()) {
                    int sweptBurn = fireAspectBurn(swept);
                    if (sweptBurn > 0) {
                        enemy.addBurn(sweptBurn);
                        log.log("  「火焰附加」为 " + enemy.name() + " 附加灼烧 " + sweptBurn);
                    }
                }
            }
        } finally {
            resolvingSweep = false;
        }
    }

    /** Lets a card add its own line to the battle narration. */
    public void announce(String message) {
        log.log(message);
    }

    /** Grants block to the active player, used by skill cards. */
    public void grantPlayerBlock(int amount, String source) {
        Combatant actor = activePlayer();
        int gained = previewBlock(actor, amount);
        gainBlockFor(actor, gained);
        log.log("  " + prefix(actor) + "获得 " + gained + " 点格挡（当前 " + actor.block() + "）"
                + (source == null ? "" : " —— " + source));
    }

    // ------------------------------------------------------------------ v0.5.3.3（A4-12）回合钩子

    /**
     * **回合钩子：回合开始**（v0.5.3.3 / A4-12）。
     *
     * <p>原版里这类效果是"能力牌自己在回合开始广播时触发"，本引擎的做法与【余像】【村庄英雄】
     * 一致：能力牌在打出的那一刻把一个数字 / 旗标写进 {@link PlayerState}，
     * 由这里在**每个回合开始的固定位置**读一次。所以不必给卡牌建一套回调注册表，
     * 也保证"谁的能力谁触发"（多人各算各的）。</p>
     *
     * <p>目前挂在这里的：</p>
     * <ul>
     *   <li>【无限刀刃】：加 1 张『小刀』进手牌；</li>
     *   <li>【毒雾】：对所有存活敌人施加 N 层中毒。</li>
     * </ul>
     */
    private void onTurnStartForPowers(PlayerState state) {
        Combatant actor = state.combatant();
        if (!actor.alive()) {
            return;
        }
        // 【火焰屏障】的收尾：它必须活过**整个敌人回合**（反弹就发生在那里），
        // 所以收回的时机是"自己下一个回合开始"，不是"自己回合结束"。
        if (actor.temporaryThorns() > 0) {
            int removed = actor.temporaryThorns();
            actor.clearTemporaryThorns();
            log.log("  【火焰屏障】结束：收回 " + removed + " 层临时荆棘（剩余荆棘 "
                    + actor.thorns() + "）");
        }
        if (state.infiniteBlades) {
            int added = addCardsToHand(actor, CardLibrary.SHIV, 1);
            log.log("  【无限刀刃】：加入 " + added + " 张『小刀』");
        }
        // v0.5.4.0（A4-19）【幻影杀手】：上回合打的牌，这个回合生效
        if (state.phantomKillerPending) {
            state.phantomKillerPending = false;
            state.phantomKiller = true;
            log.log("  【幻影杀手】：本回合你的攻击伤害翻倍");
        }
        // v0.5.4.0（A4-19）E15【无形】：自己回合开始时递减（2 层 = 保两个敌人回合）
        if (actor.intangibleTurns() > 0) {
            log.log("  【无形】：本回合结束后还剩 " + actor.decayIntangible() + " 层");
        }
        // v0.5.3.8（A4-17）【狂暴】（稀有版）：回合开始额外给能量
        if (state.berserk > 0) {
            state.energy += state.berserk;
            log.log("  【狂暴】：获得 " + state.berserk + " 点能量（当前 " + state.energy + "）");
        }
        // v0.5.3.7（A4-16）【残忍】：回合开始失去 1 点生命并抽 1 张
        if (state.brutality > 0 && actor.alive()) {
            loseHp(actor, 1, "「残忍」");
            log.log("  【残忍】：失去 1 点生命（当前 " + actor.hp() + "/" + actor.maxHp() + "）→ 抽 1 张");
            if (actor.alive()) {
                drawCards(state, 1);
            }
        }
        if (state.noxiousFumes > 0) {
            int hit = 0;
            for (Enemy enemy : aliveEnemies()) {
                applyPoison(enemy, state.noxiousFumes);
                hit++;
            }
            if (hit > 0) {
                log.log("  【毒雾】：" + hit + " 个敌人各获得中毒 " + state.noxiousFumes);
            }
        }
    }

    /**
     * 【行业工具】（v0.5.4.1 A4-20）：回合开始时按层数各来一次"抽 1 张、弃 1 张"。
     *
     * <p>规则（kamigame「商売道具」）："ターン開始時、カードを1枚引き、1枚捨てる"。
     * 层数就是打出的张数（两张【行业工具】= 抽 1 弃 1 两次）。</p>
     */
    private void applyToolsOfTheTrade(PlayerState state) {
        if (state == null || state.toolsOfTheTrade <= 0 || !state.combatant().alive()) {
            return;
        }
        for (int i = 0; i < state.toolsOfTheTrade; i++) {
            drawCards(state, 1);
            Card discarded = discardWorstCard(state);
            log.log("  【行业工具】：抽 1 张、弃 1 张"
                    + (discarded == null ? "（手牌是空的，没有牌可弃）" : "（弃掉「" + discarded.name() + "」）"));
        }
        if (state.hand.isEmpty()) {
            log.log("  【行业工具】：手牌已空");
        }
    }

    /**
     * 弃掉手里**最没用**的一张（【行业工具】的自动弃牌；返回弃掉的那张，手里没牌返回 {@code null}）。
     *
     * <p>⚠️ 原版是**玩家自己选**弃哪张，这里没有为它加选牌弹窗，所以按
     * {@link #discardOrder} 的贪心顺序自动弃：状态 / 诅咒最优先，其次攻击 → 技能 → 能力，
     * 同类型里费用低的先扔。与【集中精神】那类"弃 N 张"的兜底共用同一份判断。</p>
     */
    private Card discardWorstCard(PlayerState state) {
        if (state.hand.isEmpty()) {
            return null;
        }
        List<Integer> order = discardOrder(List.copyOf(state.hand));
        if (order.isEmpty()) {
            return null;
        }
        return discardFromHand(state.combatant(), order.get(0));
    }

    /**
     * **回合钩子：回合结束**（v0.5.3.3 / A4-12）。
     *
     * <p>目前挂在这里的：</p>
     * <ul>
     *   <li>【金属化】：获得 N 点格挡；</li>
     *   <li>【自燃】：自己失去 1 点生命，并对所有存活敌人造成 N 点伤害。</li>
     * </ul>
     *
     * <p>【火焰屏障】的临时荆棘**不在这里**收回 —— 见 {@link #onTurnStartForPowers}。</p>
     */
    private void onTurnEndForPowers(PlayerState state) {
        Combatant actor = state.combatant();
        if (state.metallicize > 0 && actor.alive()) {
            int gained = previewBlock(actor, state.metallicize);
            gainBlockFor(actor, gained);
            log.log("  【金属化】：获得 " + gained + " 点格挡（当前 " + actor.block() + "）");
        }
        if (state.combust > 0 && actor.alive()) {
            loseHp(actor, 1, "「自燃」");
            log.log("  【自燃】：" + actor.name() + " 失去 1 点生命（当前 " + actor.hp() + "/"
                    + actor.maxHp() + "），对全体敌人造成 " + state.combust + " 点伤害");
            for (Enemy enemy : aliveEnemies()) {
                dealDamage(actor, enemy, state.combust, "「自燃」");
            }
        }
    }

    /**
     * 这张牌**这一刻的实际费用**（v0.5.3.3【布置】）。
     *
     * <p>被【布置】放到抽牌堆顶的那张牌，在被打出之前费用是 0 —— 这里就是那笔账的唯一出口：
     * 出牌校验、扣能量、以及下发客户端的卡面费用（{@code CombatManager.battleView}）都用它，
     * 所以**界面显示与实际扣费永远一致**。</p>
     */
    public int effectiveCost(PlayerState state, Card card) {
        if (card == null) {
            return 0;
        }
        if (state == null) {
            return Math.max(0, card.cost());
        }
        // v0.5.3.3（A4-13）【腐化】：技能牌不消耗能量
        if (state.corruption && card.type() == CardType.SKILL) {
            return 0;
        }
        // v0.5.4.0（A4-19）【子弹时间】：本回合手牌费用全部为 0
        if (state.bulletTime) {
            return 0;
        }
        // v0.5.3.8（A4-17）E5 的费用改写（按 id）：
        // 【血债血偿】每失去一次生命 -1 费；【高超打击】每失去一次生命 +1 费；
        // 【剔骨】本回合每弃一张牌 -1 费
        int cost = Math.max(0, card.cost());
        if ("blood_for_blood".equals(card.id())) {
            cost = Math.max(0, cost - state.hpLossEvents());
        } else if ("masterful_stab".equals(card.id())) {
            cost = cost + state.hpLossEvents();
        } else if ("eviscerate".equals(card.id())) {
            cost = Math.max(0, cost - state.cardsDiscardedThisTurn());
        }
        // v0.5.3.7（A4-16）【地狱之刃】：本回合 0 费（回合结束就作废）
        if (state.isFreeThisTurn(card.id())) {
            return 0;
        }
        if (state.isFreeToPlay(card.id())) {
            return 0;
        }
        return cost;
    }

    // ------------------------------------------------------------------ v0.5.3.3（A4-13）格挡钩子

    /**
     * **格挡的统一入口**（v0.5.3.3 A4-13【主宰】）。
     *
     * <p>【主宰】是"每次获得格挡 → 随机敌人受 N 点伤害"，所以每一个"发格挡"的地方都得经过这里：
     * 卡牌（{@link #grantPlayerBlock}）、【神龟】的回合末格挡、下回合预留格挡、充能球、
     * 金属化、余像、巩固、第二次呼吸、遗物、护送……</p>
     *
     * <p>敌人也走这个入口（它们的 {@code stateOf} 是 {@code null}，不会触发能力牌），
     * 这样"格挡只有一条路"这个不变量在代码里看得见。</p>
     */
    public void gainBlockFor(Combatant owner, int amount) {
        if (owner == null || amount <= 0) {
            return;
        }
        owner.gainBlock(amount);
        triggerJuggernaut(owner);
    }

    /** 【主宰】的触发体（没有这张能力牌 / 没有敌人时什么也不做）。 */
    private void triggerJuggernaut(Combatant owner) {
        PlayerState state = stateOf(owner);
        if (state == null || state.juggernaut <= 0 || !owner.alive()) {
            return;
        }
        Enemy victim = randomLivingEnemy();
        if (victim == null) {
            return;
        }
        log.log("  【主宰】：获得格挡 → 对 " + victim.name() + " 造成 " + state.juggernaut + " 点伤害");
        dealDamage(owner, victim, state.juggernaut, "「主宰」");
    }

    // ------------------------------------------------------------------ v0.5.3.1【商队护送】

    /**
     * 【商队护送】的"发格挡 + 挂账"：给**全场存活玩家**（自己 + 队友）各加 {@code amount} 点格挡，
     * 然后在施放者身上登记一次护送，等这一轮结束（敌人回合走完）时结算。
     *
     * <p>名单是**当时**的存活玩家，之后有人倒下也不会被漏掉（见 {@code PlayerState.escorted}）。</p>
     */
    public void grantEscortBlock(Combatant caster, int amount, String source) {
        PlayerState state = stateOf(caster);
        List<Combatant> members = livingPlayers();
        StringBuilder who = new StringBuilder();
        for (Combatant member : members) {
            member.resetEscortBreached();
            int gained = previewBlock(member, amount);
            gainBlockFor(member, gained);
            if (who.length() > 0) {
                who.append('、');
            }
            who.append(member.name()).append(' ').append(gained);
        }
        if (state != null) {
            state.armEscort(members);
        }
        log.log("  " + (source == null ? "" : source + "：") + "护送的 " + members.size()
                + " 人各获得格挡（" + who + "）；若这一轮没人被打穿格挡，战斗结束后额外 +20 金币");
    }

    /**
     * 结算所有挂着的【商队护送】：这一轮里名单上**没有人被打穿格挡**（也就是没人真的掉血）
     * 就记一笔 20 金币。
     *
     * <p>调用点有两处：敌人回合收尾（{@link #finishEnemyTurn}）与战斗胜利（{@link #victory}）——
     * 后者是为了"牌刚打完，敌人还没来得及出手就把它们全清了"这种情况也能结账。</p>
     */
    private void resolveEscorts() {
        for (PlayerState state : players) {
            if (!state.escortPending()) {
                continue;
            }
            boolean intact = true;
            StringBuilder broken = new StringBuilder();
            for (Combatant member : state.escorted()) {
                if (member.escortBreached()) {
                    intact = false;
                    if (broken.length() > 0) {
                        broken.append('、');
                    }
                    broken.append(member.name());
                }
            }
            state.finishEscort(intact);
            if (intact) {
                state.addEscortGold(20);
                log.log("  【商队护送】成功：本轮无人被打穿格挡 → 战斗结束后额外 +20 金币（累计 "
                        + state.escortGold() + "）");
            } else {
                log.log("  【商队护送】失败：" + broken + " 被打穿了格挡，没有额外金币");
            }
        }
    }

    /** 这名玩家靠【商队护送】攒下的额外金币（0 = 没有）。 */
    public int escortGold(Combatant combatant) {
        PlayerState state = stateOf(combatant);
        return state == null ? 0 : state.escortGold();
    }

    /** 这名玩家【商队护送】成功的次数（自检与界面用）。 */
    public int escortSuccesses(Combatant combatant) {
        PlayerState state = stateOf(combatant);
        return state == null ? 0 : state.escortSuccesses();
    }

    /** 这名玩家**本回合已经打出的攻击牌张数**（v0.5.3.6 A4-15【终结技】用）。 */
    public int attacksPlayedThisTurn(Combatant combatant) {
        PlayerState state = stateOf(combatant);
        return state == null ? 0 : state.attacksPlayedThisTurn;
    }

    /**
     * 从**某个角色**的奖励池里随机抽一张**攻击牌**（v0.5.3.7 A4-16【地狱之刃】用）。
     *
     * <p>"随机攻击牌"在原版是"从该角色的攻击牌里随机一张"。本模组没有"当前角色"这个概念
     * （{@code PlayerState} 不带角色），但【地狱之刃】本身是**铁甲战士专属**卡，
     * 所以直接取铁甲战士的池子 —— 与"这张牌只可能在战士手里"完全等价。</p>
     *
     * @return 抽到的那张牌；池子为空（理论上不会）返回 {@code null}
     */
    public Card randomAttackCard(String characterId) {
        return randomCardOfType(characterId, CardType.ATTACK);
    }

    /** 随机抽一张**技能牌**（v0.5.3.9 A4-18【转移】用）。 */
    public Card randomSkillCard(String characterId) {
        return randomCardOfType(characterId, CardType.SKILL);
    }

    /** 从某角色的奖励池里随机抽一张指定类型的可打出牌。 */
    private Card randomCardOfType(String characterId, CardType type) {
        List<Card> pool = new ArrayList<>();
        for (Card card : CardLibrary.rewardPool(characterId)) {
            if (card.type() == type && !StatusCards.unplayable(card)) {
                pool.add(card);
            }
        }
        if (pool.isEmpty()) {
            return null;
        }
        return pool.get(nextRandomInt(pool.size()));
    }

    /**
     * 这张牌**这一刻能不能打出**（v0.5.3.9 A4-18，E16）。
     *
     * <p>与 {@code StatusCards.unplayable}（按 id 的静态"不可打出"）配合使用：
     * 那个管"状态 / 诅咒 / 使用不可"，这个管**动态条件** —— 目前只有【交锋】
     * （"手牌全是攻击牌时才能打出"）。</p>
     *
     * <p>服务端用它做出牌门禁，同时把结果塞进战斗快照的 {@code HandCard.playable}，
     * 客户端据此把牌灰掉。**同一个判定两个地方用**，不会出现"界面能点、点了报错"。</p>
     */
    public boolean playableNow(PlayerState state, Card card) {
        if (state == null || card == null) {
            return true;
        }
        if ("clash".equals(card.id())) {
            for (Card inHand : state.hand) {
                if (inHand.type() != CardType.ATTACK) {
                    return false;
                }
            }
        }
        // v0.5.4.0（A4-19）【华丽收场】：只有抽牌堆**没有牌**时才能打出
        if ("grand_finale".equals(card.id())) {
            return state.drawPile().isEmpty();
        }
        return true;
    }

    // ------------------------------------------------------------------ 状态输出

    /** Multi-line status report used by the in-game {@code /stc combat status} command. */
    public List<String> statusLines() {
        List<String> lines = new ArrayList<>();
        lines.add("第 " + turn + " 回合 · " + phase + " · 参战 " + players.size() + " 人");
        for (int i = 0; i < players.size(); i++) {
            PlayerState state = players.get(i);
            Combatant combatant = state.combatant();
            String marker = i == activeIndex ? "▶ " : "  ";
            if (!combatant.alive()) {
                lines.add(marker + combatant.name() + "：已倒下");
                continue;
            }
            lines.add(marker + combatant.name() + "：HP " + combatant.hp() + "/" + combatant.maxHp()
                    + " · 格挡 " + combatant.block() + combatant.statusSuffix()
                    + (i == activeIndex ? " · 能量 " + state.energy + "/" + state.energyPerTurn()
                    + " · 本回合已出牌 " + state.cardsPlayedThisTurn : ""));
            if (i == activeIndex) {
                lines.add("   手牌（" + state.hand.size() + "）：" + state.describeHand());
                lines.add("   抽牌堆 " + state.drawPile.size() + " · 弃牌堆 " + state.discardPile.size());
            }
        }
        for (int i = 0; i < enemies.size(); i++) {
            lines.add("敌人 " + (i + 1) + "：" + describeEnemy(enemies.get(i)));
        }
        return lines;
    }

    /** Short status used for the action bar. */
    public String actionBar() {
        StringBuilder sb = new StringBuilder();
        sb.append("回合 ").append(turn);
        for (int i = 0; i < players.size(); i++) {
            PlayerState state = players.get(i);
            Combatant combatant = state.combatant();
            sb.append(" | ").append(i == activeIndex ? "▶" : "").append(combatant.name());
            sb.append(" HP ").append(combatant.hp()).append('/').append(combatant.maxHp());
            sb.append(" 格挡 ").append(combatant.block());
            if (i == activeIndex) {
                sb.append(" 能量 ").append(state.energy).append('/').append(state.energyPerTurn());
                sb.append(" 抽牌 ").append(state.drawPile.size()).append(" 弃牌 ").append(state.discardPile.size());
            }
            if (combatant.hasStatuses()) {
                sb.append(combatant.statusSuffix());
            }
            if (!combatant.alive()) {
                sb.append("(倒下)");
            }
        }
        sb.append(" | 敌人 ");
        for (Enemy enemy : enemies) {
            sb.append(enemy.alive() ? enemy.name() + " " + enemy.hp() + "(" + enemy.intentLabelWithTarget() + ")"
                    : enemy.name() + " 已倒下");
            sb.append("  ");
        }
        return sb.toString().trim();
    }

    public String describeHand() {
        return activeState().describeHand();
    }

    private static String prefix(Combatant combatant) {
        return combatant.name() + " ";
    }

    private static String describeEnemy(Enemy enemy) {
        if (!enemy.alive()) {
            return enemy.name() + " 已倒下";
        }
        return enemy.name() + " HP " + enemy.hp() + "/" + enemy.maxHp() + " · 格挡 " + enemy.block()
                + " · 意图 " + enemy.intentLabelWithTarget() + enemy.statusSuffix();
    }

    private static String describeEnemies(List<Enemy> enemies) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < enemies.size(); i++) {
            if (i > 0) {
                sb.append('、');
            }
            sb.append(enemies.get(i).name());
        }
        return sb.toString();
    }

    private String describeParty() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < players.size(); i++) {
            if (i > 0) {
                sb.append('、');
            }
            sb.append(players.get(i).combatant().name());
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ 内部工具

    /**
     * "当前玩家"（v0.5.1.6 起含义变了，见 {@link #acting}）。
     *
     * <ol>
     *   <li>正在结算某张牌 → 出牌者；</li>
     *   <li>否则 → **下一个还没确认结束回合的存活玩家**（界面与自检问"轮到谁"时就是这个答案）；</li>
     *   <li>全员都确认了（正在跑敌怪回合）→ 退回第 0 个玩家，保证永不为 null。</li>
     * </ol>
     */
    private PlayerState activeState() {
        if (acting != null) {
            return acting;
        }
        PlayerState next = nextUnconfirmedPlayer(0);
        if (next != null) {
            return next;
        }
        if (players.isEmpty()) {
            return null;
        }
        return players.get(Math.max(0, Math.min(activeIndex, players.size() - 1)));
    }

    private int firstLivingPlayer(int from) {
        for (int i = 0; i < players.size(); i++) {
            int index = Math.floorMod(from + i, players.size());
            if (players.get(index).alive()) {
                return index;
            }
        }
        return -1;
    }

    private boolean allPlayersDown() {
        for (PlayerState state : players) {
            if (state.alive()) {
                return false;
            }
        }
        return true;
    }

    private static boolean needsPlayerTarget(Intent intent) {
        return switch (intent.type()) {
            case ATTACK, DEBUFF_MINING_FATIGUE, DEBUFF_VULNERABLE, DEBUFF_BURN, DEBUFF_DECAY, DEBUFF_POISON,
                    DEBUFF_SLOW, LOCK,
                DEBUFF_STATUS_CARD -> true;
            default -> false;
        };
    }

    private Combatant randomLivingPlayer() {
        List<Combatant> alive = new ArrayList<>();
        for (PlayerState state : players) {
            if (state.alive()) {
                alive.add(state.combatant());
            }
        }
        if (alive.isEmpty()) {
            return null;
        }
        return alive.get(random.nextInt(alive.size()));
    }

    /**
     * 为所有存活敌人抽好**下一回合的整套动作**，并给需要目标的动作指定一名存活玩家。
     *
     * <p>v0.4.5 意图状态机：一次 {@link Enemy#rollActions()} 推进一整个"回合程序"，
     * 只要这套程序里有任何动作需要目标，就先抽好一名玩家（头上意图会显示"→ 玩家名"）。</p>
     */
    private void assignIntents() {
        for (Enemy enemy : enemies) {
            if (!enemy.alive()) {
                continue;
            }
            List<Intent> actions = enemy.rollActions();
            boolean needsTarget = false;
            for (Intent action : actions) {
                if (needsPlayerTarget(action)) {
                    needsTarget = true;
                    break;
                }
            }
            enemy.setTarget(needsTarget ? randomLivingPlayer() : null);
        }
    }

    private int resolveEnemyIndex(int requested) {
        if (requested >= 0 && requested < enemies.size() && enemies.get(requested).alive()) {
            return requested;
        }
        for (int i = 0; i < enemies.size(); i++) {
            if (enemies.get(i).alive()) {
                return i;
            }
        }
        return -1;
    }

    /** Re-evaluates victory/defeat; used when an enemy leaves the fight outside of card play. */
    public void recheck() {
        checkBattleEnd();
    }

    // ------------------------------------------------------------------ 充能球（v0.0.3 W4；v0.3.0 补全四种）

    /**
     * 生成一颗充能球（故障机器人机制）。
     *
     * <p>栏位满了就先**激发最前面那颗**（原版行为），再把新球放到最后。</p>
     */
    public void channelOrb(Combatant owner, Orb orb) {
        if (orb == Orb.FROST) {
            owner.addFrostCount(1);
        }
        if (owner == null || orb == null) {
            return;
        }
        // v0.5.0.10 成就【集中冰才对】：需求"在拥有集中状态时，生成冰球。"
        // 引擎只广播事实，给谁记成就由 MC 层决定。
        if (orb == Orb.FROST && owner.focus() > 0) {
            emitFx(CombatFx.FROST_CHANNELLED, owner, owner);
        }
        if (owner.orbSlots() <= 0) {
            // v0.4.0：只有故障机器人有充能球栏位
            log.log("  " + owner.name() + " 没有充能球栏位，无法生成" + orb.label());
            return;
        }
        if (owner.orbsFull()) {
            log.log("  充能球栏位已满（" + owner.orbSlots() + "），先激发最前面那颗");
            evokeOrb(owner);
        }
        owner.addOrb(orb);
        OrbInstance added = owner.orbs().get(owner.orbs().size() - 1);
        // v0.5.0.4【电流相生】：记下"本场已经生成过多少颗闪电球"，那是它的 X。
        // 记在真正生成成功之后，所以没有球位时不会被计数。
        if (orb == Orb.LIGHTNING) {
            owner.addLightningCount(1);
        }
        log.log("  " + owner.name() + " 生成 1 颗" + added.describe() + "（栏位 "
                + owner.orbs().size() + "/" + owner.orbSlots() + "）");
    }

    /** 随机生成一颗球（【混沌】用；种子固定所以自检里可复现）。 */
    public Orb randomOrb() {
        Orb[] all = Orb.values();
        return all[random.nextInt(all.length)];
    }

    /**
     * 激发最前面的充能球（一次性大效果）。
     *
     * <ul>
     *   <li>闪电球：随机敌人 8 点伤害；</li>
     *   <li>冰球：5 点格挡；</li>
     *   <li>暗球：对**当前生命最低的敌人**造成等同于充能的伤害（原版行为）；</li>
     *   <li>等离子球：+2 能量。</li>
     * </ul>
     *
     * @return 是否真的激发了一颗
     */
        /**
     * 激发最右侧的充能球。
     *
     * <p>v0.4.9.4 增幅器：若持有增幅器且本回合还没用过，则这次激发会**连续激发两次**
     *（注意 {@link #doEvoke} 一开始就会把球摘掉，所以"两次"是各摘一颗球，而不是同一颗结算两遍）。</p>
     */
    public boolean evokeOrb(Combatant owner) {
        if (owner != null && owner.evokeBonusOnce() && !owner.evokeUsedThisTurn()) {
            owner.markEvokeUsedThisTurn();
            boolean first = doEvoke(owner);
            boolean second = doEvoke(owner);
            return first || second;
        }
        return doEvoke(owner);
    }

    /** 真正结算一次激发（原来叫 evokeOrb）。 */
    private boolean doEvoke(Combatant owner) {
        if (owner == null) {
            return false;
        }
        OrbInstance orb = owner.removeFirstOrb();
        if (orb == null) {
            log.log("  " + owner.name() + " 没有充能球可以激发");
            return false;
        }
        switch (orb.type()) {
            case LIGHTNING -> {
                Enemy target = randomLivingEnemy();
                if (target != null) {
                    dealDamage(owner, target, (orb.effectiveEvoke() + owner.focus()), "「激发·闪电球」");
                    // v0.4.1：球打人要有音效（需求："充能球结算造成伤害时没有音效"）
                    emitFx(CombatFx.ORB_DAMAGE, owner, target);
                }
            }
            case FROST -> {
                int gained = previewBlock(owner, (orb.effectiveEvoke() + owner.focus()));
                gainBlockFor(owner, gained);
                log.log("  「激发·冰球」：获得 " + gained + " 点格挡（当前 " + owner.block() + "）");
                emitFx(CombatFx.ORB_BLOCK, owner, owner);
            }
            case DARK -> {
                Enemy target = lowestHpEnemy();
                if (target == null) {
                    log.log("  「激发·暗球」：没有敌人可以打");
                } else {
                    log.log("  「激发·暗球」：充能 " + orb.charge() + " 全部砸向 "
                            + target.name() + "（当前生命最低）");
                    dealDamage(owner, target, (orb.charge() + owner.focus()), "「激发·暗球」");
                    emitFx(CombatFx.ORB_DAMAGE, owner, target);
                }
            }
            case PLASMA -> {
                int gained = gainEnergy(owner, orb.type().evokeEnergy());
                log.log("  「激发·等离子球」：获得 " + gained + " 点能量（当前 " + energyOf(owner) + "）");
            }
        }
        return true;
    }

    /** 回合结束时依次触发每颗球的被动（暗球在这里成长，等离子球的被动在回合开始）。 */
    private void activateOrbPassives(Combatant owner) {
        if (owner.orbs().isEmpty()) {
            return;
        }
        for (OrbInstance orb : owner.orbs()) {
            switch (orb.type()) {
                case LIGHTNING -> {
                    Enemy target = randomLivingEnemy();
                    if (target != null) {
                        if (owner.lightningHitsAll()) {
                            for (Enemy all : aliveEnemies()) {
                                dealDamage(owner, all, (orb.type().passiveValue() + owner.focus()), "「闪电球」被动（电动力学）");
                            }
                        } else {
                            dealDamage(owner, target, (orb.type().passiveValue() + owner.focus()), "「闪电球」被动");
                        }
                        // v0.4.1：球造成伤害要有音效
                        emitFx(CombatFx.ORB_DAMAGE, owner, target);
                    }
                }
                case FROST -> {
                    int gained = previewBlock(owner, (orb.type().passiveValue() + owner.focus()));
                    gainBlockFor(owner, gained);
                    log.log("  「冰球」被动：获得 " + gained + " 点格挡（当前 " + owner.block() + "）");
                    emitFx(CombatFx.ORB_BLOCK, owner, owner);
                }
                case DARK -> log.log("  「暗球」被动：充能 +" + orb.type().growth() + "（激发时会砸向生命最低的敌人）");
                case PLASMA -> log.log("  「等离子球」被动：下回合开始 +" + orb.type().passiveEnergy() + " 能量");
            }
        }
        owner.growOrbs();
    }

    /** 给某个参战者加能量（只能加给玩家；返回实际加上的点数）。 */
    public int gainEnergy(Combatant owner, int amount) {
        PlayerState state = stateOf(owner);
        if (state == null || amount <= 0) {
            return 0;
        }
        state.energy += amount;
        return amount;
    }

    private int energyOf(Combatant owner) {
        PlayerState state = stateOf(owner);
        return state == null ? 0 : state.energy;
    }

    /** 随机一个存活敌人（球与【弹跳药瓶】用；和原版一样是随机目标）。 */
    /**
     * 把一张牌放到某名玩家的**抽牌堆顶**（v0.4.7.4 多重射击：打完把它自己放回去，
     * 于是下一次抽牌立刻又抽到它）。
     */
    public void putOnTopOfDrawPile(Combatant owner, Card card) {
        PlayerState state = stateOf(owner);
        if (state == null || card == null) {
            return;
        }
        state.drawPile().addFirst(card);
        log.log("  「" + card.name() + "」回到了 " + owner.name() + " 的抽牌堆顶部");
    }

    /**
     * 选牌动作（v0.4.7.6）：把**弃牌堆**第 {@code index} 张放到抽牌堆顶（废料回收）。
     *
     * @return true = 真的移动了
     */
    public boolean moveDiscardToDrawPileTop(Combatant owner, int index) {
        PlayerState state = stateOf(owner);
        if (state == null) {
            return false;
        }
        Card card = state.discardPile.removeAt(index);
        if (card == null) {
            return false;
        }
        state.drawPile().addFirst(card);
        log.log("  把弃牌堆的「" + card.name() + "」放到了 " + owner.name() + " 的抽牌堆顶部");
        return true;
    }

    /**
     * 选牌动作（v0.4.7.6）：把**手牌**第 {@code index} 张**变化**成另一张牌（应急储备 → 闪光弹）。
     *
     * @return true = 真的变化了
     */
    public boolean transformInHand(Combatant owner, int index, Card replacement) {
        PlayerState state = stateOf(owner);
        if (state == null || replacement == null || index < 0 || index >= state.hand.size()) {
            return false;
        }
        Card old = state.hand.set(index, replacement);
        log.log("  " + owner.name() + " 手里的「" + old.name() + "」变成了「" + replacement.name() + "」");
        return true;
    }

    /**
     * 选牌动作（v0.4.7.6）：把**手牌**里指定的若干张**消耗**掉（弃置边陵）。
     *
     * @param indices 要消耗的手牌下标（可乱序；越界自动忽略）
     * @return 实际消耗的张数
     */
    public int exhaustFromHand(Combatant owner, java.util.Collection<Integer> indices) {
        PlayerState state = stateOf(owner);
        if (state == null || indices == null || indices.isEmpty()) {
            return 0;
        }
        java.util.List<Integer> sorted = new java.util.ArrayList<>(indices);
        sorted.sort(java.util.Comparator.reverseOrder());
        int done = 0;
        for (int index : sorted) {
            if (index < 0 || index >= state.hand.size()) {
                continue;
            }
            Card card = state.hand.remove(index);
            exhaustCard(state, card);
            log.log("  " + owner.name() + " 消耗了手里的「" + card.name() + "」");
            done++;
        }
        return done;
    }

    /**
     * 【不屈】的濒死救回（v0.4.7.8）：回 30% 最大生命、+5 力量、+3 能量，并把这张牌移出手牌、
     * 记进"战斗结束后从牌组删掉"的集合。
     *
     * @return true = 已经救回来了（调用方不要再扣血）
     */
    private boolean tryLastStand(Combatant target, int incoming) {
        if (!isPlayer(target) || incoming <= 0 || target.hp() - incoming > 0) {
            return false;
        }
        PlayerState state = stateOf(target);
        if (state == null) {
            return false;
        }
        for (int i = 0; i < state.hand.size(); i++) {
            if (!StatusCards.isLastStand(state.hand.get(i))) {
                continue;
            }
            Card used = state.hand.remove(i);
            int healed = Math.max(1, target.maxHp() * 3 / 10);
            target.heal(healed);
            target.addStrength(5);
            gainEnergy(target, 3);
            state.removeFromDeck.add(used.id());
            log.log("  【不屈】发动：恢复 " + healed + " 点生命（HP " + target.hp() + "/" + target.maxHp()
                    + "），力量 +5，能量 +3，这张牌将从牌组彻底移除");
            emitFx(CombatFx.PLAYER_BLOCK, null, target);
            return true;
        }
        return false;
    }
    // ------------------------------------------------------------------ 选牌（v0.5.1.7 A3）

    /**
     * 卡牌效果要求玩家选牌：登记请求，真正的结算留到 {@link #resolveCardPick}。
     *
     * <p>引擎没法阻塞在界面上，所以这里只"挂起"；Minecraft 层看到 {@link #pendingPick}
     * 非空就弹选牌页，玩家选完回传下标。</p>
     */
    public void beginCardPick(Combatant owner, String cardName, CardPickKind kind, CardPickSource source,
                              int maxPicks, Card payload) {
        beginCardPick(owner, cardName, kind, source, maxPicks, payload, null, 0);
    }

    /**
     * 带**筛选**与**次数**的版本（v0.5.2.3 A4-10）。
     *
     * @param filter 只把满足条件的牌作为候选（军备：还能升级的；双持：攻击 / 能力牌）；
     *               {@code null} = 全都给
     * @param amount 燃烧契约抽几张 / 双持复制几张；其余 kind 传 0
     */
    public void beginCardPick(Combatant owner, String cardName, CardPickKind kind, CardPickSource source,
                              int maxPicks, Card payload, java.util.function.Predicate<Card> filter,
                              int amount) {
        PlayerState state = stateOf(owner);
        if (state == null) {
            return;
        }
        List<Card> pool = switch (source) {
            case DISCARD -> state.discardPile.view();
            case EXHAUST -> state.exhaustPile.view();
            case HAND -> List.copyOf(state.hand);
        };
        List<Card> candidates = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < pool.size(); i++) {
            if (filter == null || filter.test(pool.get(i))) {
                candidates.add(pool.get(i));
                indices.add(i);
            }
        }
        state.pendingPick = new PendingCardPick(cardName, kind, source, Math.max(1, maxPicks), payload,
                List.copyOf(candidates), List.copyOf(indices), amount);
        log.log("  「" + cardName + "」：等待选择 " + Math.max(1, maxPicks) + " 张牌（"
                + switch (source) {
                    case DISCARD -> "弃牌堆";
                    case EXHAUST -> "消耗堆";
                    case HAND -> "手牌";
                } + "，候选 " + candidates.size() + " 张）");
    }

    /** 这名玩家当前待处理的选牌请求；没有则返回 {@code null}。 */
    public PendingCardPick pendingPick(Combatant owner) {
        PlayerState state = stateOf(owner);
        return state == null ? null : state.pendingPick;
    }

    /** 待选牌的候选列表（顺序 = 界面显示顺序 = 回传下标的含义）。 */
    public List<Card> pickCandidates(Combatant owner) {
        PlayerState state = stateOf(owner);
        if (state == null || state.pendingPick == null) {
            return List.of();
        }
        return state.pendingPick.candidates();
    }

    /**
     * 玩家选好了（把下标回传进来）。
     *
     * @param indices 选中的下标；传 {@code null} 表示"放弃/超时" —— 这时退回**贪心自动选**
     *                （与这次改造前的行为一致），保证卡牌效果不会凭空消失
     * @return 给玩家看的一句话
     */
    public String resolveCardPick(Combatant owner, List<Integer> indices) {
        PlayerState state = stateOf(owner);
        if (state == null || state.pendingPick == null) {
            return "现在没有需要选择的牌。";
        }
        PendingCardPick pick = state.pendingPick;
        List<Card> candidates = pickCandidates(owner);
        List<Integer> chosen = new ArrayList<>();
        if (indices != null) {
            for (int index : indices) {
                if (index >= 0 && index < candidates.size() && !chosen.contains(index)
                        && chosen.size() < pick.maxPicks()) {
                    chosen.add(index);
                }
            }
        } else {
            chosen.addAll(greedyPick(pick, candidates));
        }
        state.pendingPick = null;
        String detail = applyCardPick(state, pick, chosen, candidates);
        log.log("  「" + pick.cardName() + "」结算：" + detail);
        return detail;
    }

    /** 放弃 / 超时：按"贪心"替他选（旧的自动选行为）。 */
    public String cancelCardPick(Combatant owner) {
        return resolveCardPick(owner, null);
    }

    /** 贪心兜底：与改造前的"自动挑费用最高"一致。 */
    private List<Integer> greedyPick(PendingCardPick pick, List<Card> candidates) {
        List<Integer> chosen = new ArrayList<>();
        if (candidates.isEmpty()) {
            return chosen;
        }
        switch (pick.kind()) {
            case CONTINGENCY -> {
                // 旧行为：第一张**不带消耗**的牌（否则会把刚打出的消耗牌再变一次，没有意义）
                for (int i = 0; i < candidates.size(); i++) {
                    if (!candidates.get(i).has(CardKeyword.EXHAUST)) {
                        chosen.add(i);
                        break;
                    }
                }
            }
            case ABANDON, BURNING_PACT -> {
                List<Integer> order = new ArrayList<>();
                for (int i = 0; i < candidates.size(); i++) {
                    order.add(i);
                }
                order.sort((a, b) -> Integer.compare(candidates.get(b).cost(), candidates.get(a).cost()));
                for (int i = 0; i < order.size() && i < pick.maxPicks(); i++) {
                    chosen.add(order.get(i));
                }
            }
            case DISCARD -> {
                // 弃牌是"丢掉最没用的"：状态 / 诅咒优先，其次费用最低的
                List<Integer> order = discardOrder(candidates);
                for (int i = 0; i < order.size() && i < pick.maxPicks(); i++) {
                    chosen.add(order.get(i));
                }
            }
            default -> {
                // 废料回收 / 回收 / 军备 / 双持 / 战吼 / **布置**：费用最高的一张
                // （并列时取靠后的那张，与旧实现一致）
                // 布置取费用最高的最合算：免费额度花在贵牌上。
                int best = -1;
                int bestCost = -1;
                for (int i = 0; i < candidates.size(); i++) {
                    if (candidates.get(i).cost() >= bestCost) {
                        bestCost = candidates.get(i).cost();
                        best = i;
                    }
                }
                if (best >= 0) {
                    chosen.add(best);
                }
            }
        }
        return chosen;
    }

    /** 按种类落地选牌效果。 */
    private String applyCardPick(PlayerState state, PendingCardPick pick, List<Integer> chosen,
                                 List<Card> candidates) {
        Combatant owner = state.combatant();
        if (chosen.isEmpty()) {
            return "什么都没有选（「" + pick.cardName() + "」没有产生任何效果）";
        }
        // 候选下标 → 来源牌堆里的**真实下标**（筛选过的 kind 两者不一样）
        List<Integer> real = new ArrayList<>();
        for (int index : chosen) {
            if (index >= 0 && index < pick.sourceIndices().size()) {
                real.add(pick.sourceIndices().get(index));
            }
        }
        if (real.isEmpty()) {
            return "选中的牌已经不在原处了（「" + pick.cardName() + "」没有产生效果）";
        }
        switch (pick.kind()) {
            case SCRAP_RECYCLE -> {
                int index = real.get(0);
                Card card = candidates.get(chosen.get(0));
                moveDiscardToDrawPileTop(owner, index);
                return "把「" + card.name() + "」放到了抽牌堆顶";
            }
            case CONTINGENCY -> {
                int index = real.get(0);
                Card card = candidates.get(chosen.get(0));
                Card replacement = pick.payload();
                if (replacement != null && transformInHand(owner, index, replacement)) {
                    return "把「" + card.name() + "」变成了「" + replacement.name() + "」";
                }
                return "「" + card.name() + "」没有变化";
            }
            case ABANDON -> {
                int removed = exhaustFromHand(owner, real);
                return "消耗了 " + removed + " 张手牌";
            }
            case RECYCLE -> {
                int index = chosen.get(0);
                Card card = candidates.get(index);
                int gained = Math.max(0, card.cost());
                exhaustFromHand(owner, List.of(index));
                if (gained > 0) {
                    gainEnergy(owner, gained);
                }
                return "消耗「" + card.name() + "」，获得 " + gained + " 点能量";
            }
            case DISCARD -> {
                // 规则是"弃 N 张"：玩家少选了几张就按贪心补足，否则等于白拿收益
                List<Integer> picks = new ArrayList<>(chosen);
                for (int index : discardOrder(candidates)) {
                    if (picks.size() >= pick.maxPicks()) {
                        break;
                    }
                    if (!picks.contains(index)) {
                        picks.add(index);
                    }
                }
                // 换成真实手牌下标，并从大到小删：前面删掉会让后面的下标整体前移
                List<Integer> toDiscard = new ArrayList<>();
                for (int index : picks) {
                    if (index >= 0 && index < pick.sourceIndices().size()) {
                        toDiscard.add(pick.sourceIndices().get(index));
                    }
                }
                toDiscard.sort(java.util.Comparator.reverseOrder());
                int done = 0;
                for (int index : toDiscard) {
                    if (discardFromHand(owner, index) != null) {
                        done++;
                    }
                }
                return "弃掉了 " + done + " 张手牌";
            }
            case UPGRADE_ONE -> {
                int index = real.get(0);
                Card card = candidates.get(chosen.get(0));
                // v0.5.4.1（A4-20）：从**这张牌当前的档位**再上一档 ——
                // 【灼热打击】在手里可能已经是 +3，写死 resolve(id, 1) 会把它降回 +1。
                // 附魔也一起带过去（附魔是实例数据，升级不该把它洗掉）。
                Card upgraded = CardLibrary.nextUpgradeOf(card);
                if (upgraded != null && upgraded != card && !upgraded.equals(card)
                        && transformInHand(owner, index, upgraded.withEnchantments(card.enchantments()))) {
                    return "把「" + card.name() + "」升级成了「" + upgraded.name() + "」";
                }
                return "「" + card.name() + "」已经是强化版，没有变化";
            }
            case DUPLICATE -> {
                int index = real.get(0);
                Card card = candidates.get(chosen.get(0));
                int copies = Math.max(1, pick.amount());
                int added = addCardsToHand(owner, card, copies);
                return "复制了 " + added + " 张「" + card.name() + "」进手牌";
            }
            case BURNING_PACT -> {
                int removed = exhaustFromHand(owner, real);
                int draw = Math.max(1, pick.amount());
                drawCardsFor(owner, draw);
                return "消耗了 " + removed + " 张手牌，抽了 " + draw + " 张";
            }
            case PUT_ON_TOP -> {
                int index = real.get(0);
                Card card = candidates.get(chosen.get(0));
                if (moveHandToDrawPileTop(owner, index) != null) {
                    return "把「" + card.name() + "」放到了抽牌堆顶";
                }
                return "「" + card.name() + "」没有放回去";
            }
            case SETUP -> {
                // v0.5.3.3【布置】：放到抽牌堆顶 + 记一笔"打出前费用为 0"
                int index = real.get(0);
                Card card = candidates.get(chosen.get(0));
                if (moveHandToDrawPileTop(owner, index) != null) {
                    state.markFreeToPlay(card.id());
                    return "把「" + card.name() + "」放到了抽牌堆顶，它被打出之前费用为 0";
                }
                return "「" + card.name() + "」没有放回去";
            }
            case EXHUME -> {
                // v0.5.3.9（A4-18）【挖掘】：从**消耗堆**里把选中的那张拿回手牌
                int index = real.get(0);
                Card card = candidates.get(chosen.get(0));
                if (state.exhaustPile.view().size() <= index) {
                    return "「" + card.name() + "」已经不在消耗堆里了";
                }
                if (state.hand.size() >= rules.maxHandSize()) {
                    return "手牌已满（" + rules.maxHandSize() + "），「" + card.name() + "」留在消耗堆里";
                }
                state.exhaustPile.removeAt(index);
                state.hand.add(card);
                return "把「" + card.name() + "」从消耗堆拿回了手牌";
            }
            case NIGHTMARE -> {
                // v0.5.3.9（A4-18）【噩梦】：选中的那张牌，**下个回合**开始给 amount 张复制
                Card card = candidates.get(chosen.get(0));
                int copies = Math.max(1, pick.amount());
                for (int i = 0; i < copies; i++) {
                    state.nextTurnCards.add(card);
                }
                return "下个回合开始时加入 " + copies + " 张「" + card.name() + "」的复制";
            }
            default -> {
                return "未知的选牌效果";
            }
        }
    }

    /** 「弃牌」类选牌的贪心顺序：状态 / 诅咒优先，其次费用低的（A4-9 准备 / 集中精神）。 */
    private static List<Integer> discardOrder(List<Card> candidates) {
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            order.add(i);
        }
        order.sort((a, b) -> {
            int wa = discardWeight(candidates.get(a));
            int wb = discardWeight(candidates.get(b));
            return wa != wb ? Integer.compare(wa, wb)
                    : Integer.compare(candidates.get(a).cost(), candidates.get(b).cost());
        });
        return order;
    }

    /** 弃牌贪心兜底的权重：越小越先被扔掉（状态 / 诅咒最优先）。 */
    private static int discardWeight(Card card) {
        if (card == null) {
            return 0;
        }
        return switch (card.type()) {
            case STATUS, CURSE -> 0;
            case ATTACK -> 1;
            case SKILL -> 2;
            case POWER -> 3;
        };
    }

    /** 让某名玩家**跳过自己的下一个回合**（v0.4.7.5 紧急避险）。 */
    public void skipNextTurn(Combatant owner) {
        PlayerState state = stateOf(owner);
        if (state == null) {
            return;
        }
        state.skipTurns++;
        log.log("  " + owner.name() + " 的下一回合将被跳过（晕眩）");
    }

    /**
     * **回合推进断链**的判定（v0.5.1.3 看门狗）。
     *
     * <p>正常状态下这两个条件不可能同时成立：敌人队列跑完时 {@link #finishEnemyTurn()} 会紧接着
     * 开启下一轮的第一个玩家回合（phase 变 PLAYER_TURN）。如果敌人队列空了、phase 却还停在
     * ENEMY_TURN，说明"接上下一个玩家回合"这一步失败了 —— 此时玩家出不了牌、结束不了回合、
     * 也无法放弃战斗，整局锁死。v0.5.1.2 的紧急避险就是这么卡住的（见 {@link #beginPlayerRound()}）。</p>
     */
    public boolean stalledBetweenTurns() {
        return phase == CombatPhase.ENEMY_TURN && !enemyTurnRunning;
    }

    /**
     * 把断掉的回合推进**强制接上**（v0.5.1.3）。
     *
     * <p>给 Minecraft 层的看门狗用：宁可让这一轮的出牌环节重新开始，也不能让整局卡死。
     * 这里**不**递增回合数 —— 断链发生时那一轮通常已经计过数了，再 +1 会凭空多算一轮。</p>
     *
     * @return 真的接上了（{@code false} = 本来就没断链，什么都没做）
     */
    public boolean resumeStalledTurn() {
        if (!stalledBetweenTurns()) {
            return false;
        }
        log.log("⚠ 回合推进中断（敌人队列已空，阶段却停在敌人回合）：强制接上下一轮");
        if (allPlayersDown()) {
            defeat();
            return true;
        }
        int next = firstLivingPlayer(0);
        if (next < 0) {
            defeat();
            return true;
        }
        activeIndex = next;
        beginPlayerRound();
        return true;
    }

    /**
     * 仅供自检：把阶段强推到某个值，用来复现"回合推进断链"（v0.5.1.3 看门狗的自检用例）。
     *
     * <p>包级可见，不进公开 API。</p>
     */
    void forcePhaseForTest(CombatPhase forced) {
        this.phase = forced;
    }

    /** 抽牌堆顶那张牌（没有牌时返回 {@code null}；自检/界面用）。 */
    public Card topOfDrawPile(Combatant owner) {
        PlayerState state = stateOf(owner);
        if (state == null || state.drawPile().isEmpty()) {
            return null;
        }
        return state.drawPile().view().get(0);
    }

    /**
     * 这名玩家**当前还在这场战斗里的全部牌**（手牌 + 抽牌堆 + 弃牌堆 + 消耗堆）—— v0.5.2.2 A4-9。
     *
     * <p>【完美打击】要"数牌组里带『打击』的牌"，需要一个读牌组的入口。</p>
     *
     * <p><b>与"开战时的牌组"的差别</b>：{@code Participant.deck} 在构造战斗时就被铺进抽牌堆、
     * 没有留副本，所以这里按四个牌堆**现场合成**。被消耗的牌仍然算在内（它还是"你的牌"），
     * 与本 mod 里"消耗堆也属于你自己的牌"的直觉一致。</p>
     */
    public List<Card> deckOf(Combatant owner) {
        PlayerState state = stateOf(owner);
        if (state == null) {
            return List.of();
        }
        List<Card> all = new ArrayList<>(state.hand);
        all.addAll(state.drawPile.view());
        all.addAll(state.discardPile.view());
        all.addAll(state.exhaustPile.view());
        return all;
    }

    /**
     * 把**手牌**第 {@code index} 张放到**抽牌堆顶**（v0.5.2.4 A4-11【战吼】）。
     *
     * @return 被移动的那张牌；没移动（下标越界 / 不在战斗里）返回 {@code null}
     */
    public Card moveHandToDrawPileTop(Combatant owner, int index) {
        PlayerState state = stateOf(owner);
        if (state == null || index < 0 || index >= state.hand.size()) {
            return null;
        }
        Card card = state.hand.remove(index);
        state.drawPile().addFirst(card);
        log.log("  " + owner.name() + " 把手里的「" + card.name() + "」放到了抽牌堆顶");
        return card;
    }

    /**
     * 打出**抽牌堆顶**的那张牌，然后把它**消耗**掉（v0.5.2.4 A4-11【浩劫】）。
     *
     * <p>不花能量（原版浩劫就是"免费打出"）；需要指定敌人的牌会**随机挑一个活着的敌人**
     * （原版同样是随机目标）。能力牌在这里也"用完即消耗"——浩劫的说明就是"打出并消耗"。</p>
     *
     * <p>这里把 {@code playCardFor} 的那套记账照抄了一份（附魔作用域、活力、出手/格挡特效、
     * "每打出一张牌"的两个能力牌钩子），因为浩劫的牌不是从手牌打的，走不了那条路径。</p>
     *
     * @return 被打出的那张牌；抽牌堆为空 / 没有活着的敌人时返回 {@code null}
     */
    public Card playTopOfDrawPile(Combatant owner) {
        PlayerState state = stateOf(owner);
        if (state == null || state.drawPile().isEmpty()) {
            return null;
        }
        Card card = state.drawPile().removeAt(0);
        if (card == null) {
            return null;
        }
        Combatant target = null;
        if (card.target() == CardTarget.ENEMY) {
            target = randomLivingEnemy();
            if (target == null) {
                // 没有活着的敌人：放回抽牌堆顶，当作什么都没发生
                state.drawPile().addFirst(card);
                return null;
            }
        } else if (card.target() == CardTarget.SELF) {
            target = owner;
        }
        state.cardsPlayedThisTurn++;
        log.log("  「" + card.name() + "」从抽牌堆顶被打出（浩劫），之后会被消耗");
        cardBonusDamage = 0;
        if (card.type() == CardType.ATTACK && owner.vigor() > 0) {
            cardBonusDamage = owner.vigor();
            owner.clearVigor();
            log.log("  活力生效：这张牌额外造成 " + cardBonusDamage + " 点伤害");
        }
        Set<CardEnchantment> previousEnchantments = activeEnchantments;
        activeEnchantments = card.enchantments();
        // v0.5.3.3（A4-13）【涂毒】：浩劫打出的攻击牌也算"攻击牌造成伤害"
        boolean previousAttackFlag = resolvingAttackCard;
        resolvingAttackCard = card.type() == CardType.ATTACK;
        int blockBefore = owner.block();
        try {
            card.effect().apply(this, owner, target);
        } finally {
            activeEnchantments = previousEnchantments;
            resolvingAttackCard = previousAttackFlag;
            // 浩劫打出的牌不参与"费用档位"（模组改造那套），避免污染外层那张牌
            boostedCard = null;
        }
        onCardPlayedForNewPowers(state, card);
        if (card.type() == CardType.ATTACK) {
            emitFx(CombatFx.PLAYER_ATTACK, owner, target);
        }
        if (owner.block() > blockBefore) {
            emitFx(CombatFx.PLAYER_BLOCK, owner, target);
        }
        exhaustCard(state, card);
        if (state.afterImage) {
            int gained = previewBlock(owner, 1);
            gainBlockFor(owner, gained);
            log.log("  「余像」：打出「" + card.name() + "」后获得 " + gained + " 点格挡（当前 "
                    + owner.block() + "）");
        }
        return card;
    }

    /** 最近一次打出的 X 费卡的 X（v0.4.7.3）。 */
    public int lastX(Combatant owner) {
        PlayerState state = stateOf(owner);
        return state == null ? 0 : state.lastX;
    }

    /**
     * **击晕**（v0.4.7.0 闪光弹）：把敌人**当前回合**的意图改成「晕眩」——它这一回合什么也不做。
     *
     * <p>与锁血结算（{@code IntentType.LOCK}）不同：击晕只是让它空过一回合，不会扣血也不会死亡。</p>
     */
    public void stunEnemy(Enemy enemy) {
        if (enemy == null || !enemy.alive()) {
            return;
        }
        enemy.setActions(java.util.List.of(new Intent(IntentType.UNKNOWN, 0, 1, "晕眩", "")));
        log.log("  " + enemy.name() + " 被击晕：本回合的意图被改成「晕眩」");
    }

    /**
     * 往某名玩家的**抽牌堆**里加牌（v0.4.7.0：装填弩箭 / 箭袋补充）。
     *
     * <p>与 {@link #insertCards(String, Card, int)} 的区别：那个进的是**弃牌堆**。</p>
     */
    public void addToDrawPile(Combatant owner, Card card, int count) {
        PlayerState state = stateOf(owner);
        if (state == null || card == null) {
            return;
        }
        int amount = Math.max(1, count);
        for (int i = 0; i < amount; i++) {
            state.drawPile().add(card);
        }
        log.log("  " + owner.name() + " 的抽牌堆加入了 " + amount + " 张「" + card.name() + "」");
    }

    /**
     * 把若干张牌塞进某名玩家的**弃牌堆**（v0.5.2.1 A4-7【献祭】）。
     *
     * <p>与 {@link #addToDrawPile} 是一对：那个进抽牌堆、这个进弃牌堆。</p>
     */
    public void addToDiscardPile(Combatant owner, Card card, int count) {
        PlayerState state = stateOf(owner);
        if (state == null || card == null) {
            return;
        }
        int amount = Math.max(1, count);
        for (int i = 0; i < amount; i++) {
            state.discardPile().add(card);
        }
        log.log("  " + owner.name() + " 的弃牌堆加入了 " + amount + " 张「" + card.name() + "」");
    }

    /**
     * 对**随机**敌人造成 {@code times} 次伤害（v0.4.7.0 需求："对随机敌人造成伤害 X 次时，
     * 每 1 次选择的目标都是随机的。"）。
     *
     * <p>所以随机是**每一次**都重新抽，而不是"先抽一个目标再打 N 下"。</p>
     *
     * @return 实际造成的总伤害
     */
    public int dealRandomDamage(Combatant source, int times, int damage, String label) {
        int total = 0;
        for (int i = 0; i < Math.max(1, times); i++) {
            Enemy target = randomLivingEnemy();
            if (target == null) {
                break;
            }
            total += dealDamage(source, target, damage, label + "（第 " + (i + 1) + " 次）");
        }
        return total;
    }

    public Enemy randomLivingEnemy() {
        List<Enemy> alive = aliveEnemies();
        if (alive.isEmpty()) {
            return null;
        }
        return alive.get(random.nextInt(alive.size()));
    }

    /** 当前生命最低的存活敌人（暗球激发的目标，和原版一致；并列时取最前面的那只）。 */
    private Enemy lowestHpEnemy() {
        Enemy best = null;
        for (Enemy enemy : enemies) {
            if (!enemy.alive()) {
                continue;
            }
            if (best == null || enemy.hp() < best.hp()) {
                best = enemy;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ 表现层事件（v0.3.0）

    /** 表现层接收器（Minecraft 层挂上；离线自检也能挂一个来断言事件）。 */
    private java.util.function.Consumer<CombatFxEvent> fxSink;
    /** v0.5.0.5：接下来这次广播算几段动作（多段攻击用；默认 1）。 */
    private int pendingHits = 1;

    /** 挂上（或清空）表现层接收器。 */
    public void setFxSink(java.util.function.Consumer<CombatFxEvent> sink) {
        this.fxSink = sink;
    }

    /** v0.4.2：【余像】—— 让这名玩家在本场战斗里"每打出一张牌获得 1 点格挡"。 */
    public void enableAfterImage(Combatant owner) {
        PlayerState state = stateOf(owner);
        if (state == null || state.afterImage()) {
            return;
        }
        state.setAfterImage(true);
        log.log("  「余像」生效：本场战斗每打出一张牌获得 1 点格挡");
    }

    /** v0.4.2：【触媒】—— 让这名玩家的**中毒**在结算时额外触发若干次。 */
    public void addPoisonTriggers(Combatant owner, int amount) {
        PlayerState state = stateOf(owner);
        if (state == null || amount <= 0) {
            return;
        }
        state.addPoisonTriggers(amount);
        log.log("  「触媒」生效：中毒结算时将额外触发 " + amount + " 次（共 "
                + (1 + state.poisonExtraTriggers()) + " 次）");
    }

    /**
     * 取走并清空某名玩家"待发药水"的瓶数（v0.5.4.1 A4-20【炼金】）。
     *
     * <p>引擎只记账：药水是档案层（{@code PlayerCardData.potions}）的数据，
     * 零 MC 依赖的 {@code combat/core} 不能直接发（见 {@code CombatManager.grantPendingPotions}）。</p>
     */
    public int takePendingPotions(Combatant owner) {
        PlayerState state = stateOf(owner);
        return state == null ? 0 : state.takePendingPotions();
    }

    /** v0.4.6：给敌人施加**中毒**（机制同 v0.4.2 的"涂毒"，带战报）。 */
    public void applyPoison(Combatant target, int amount) {
        if (target == null || amount <= 0) {
            return;
        }
        target.addPoison(amount);
        log.log("  " + target.name() + " 获得中毒 " + amount + "（当前 " + target.poison()
                + "，出牌环节结束时按层数受伤）");
    }

    /** v0.4.6：给目标施加**凋零**（累加，累计大于当前生命时回合结束即死）。 */
    public void applyDecay(Combatant target, int amount) {
        if (target == null || amount <= 0) {
            return;
        }
        target.addDecay(amount);
        log.log("  " + target.name() + " 获得凋零 " + amount + "（当前 " + target.decay()
                + "，累计大于当前生命值时回合结束即死）");
    }

    /** 广播一个表现层事件；没挂接收器时什么也不做。 */
    public void emitFx(CombatFx kind, Combatant actor, Combatant target) {
        if (fxSink != null && kind != null) {
            fxSink.accept(new CombatFxEvent(kind, actor, target, pendingHits));
        }
    }

    /**
     * 设置"接下来这次广播是几段"（v0.5.0.5 多段攻击）。
     *
     * <p>为什么用这个钩子而不是给 {@link #emitFx} 加参数：{@code emitFx(kind, actor, target)}
     * 是三参的既有接口，自检里很多地方直接调它；再加一个重载会到处歧义。
     * 多段攻击只在 {@code executeIntent} 的 ATTACK 分支里发生，那里设一次、用完即复位。</p>
     */
    public void setPendingHits(int hits) {
        this.pendingHits = Math.max(1, hits);
    }

    /** 把"接下来的广播算一段"复位（每次设过之后都要复位，避免泄漏到别的事件）。 */
    public void resetPendingHits() {
        this.pendingHits = 1;
    }

    /**
     * 施加一个负面效果（v0.4.1）：【人工造物】（铁傀儡）会**免疫第一次**，其余照常生效。
     *
     * @param label 战报里显示的名字，例如 {@code 虚弱 3}
     * @return 是否真的生效（false = 被特性免疫了）
     */
    public boolean applyDebuff(Combatant target, String label, Runnable apply) {
        if (target.consumeDebuffShield()) {
            log.log("  " + target.name() + " 的【" + target.factionLabel() + "】特性免疫了这次 "
                    + label);
            return false;
        }
        apply.run();
        return true;
    }

    /** 这个参战者是不是玩家（用来区分"玩家受击"和"敌人受击"）。 */
    private boolean isPlayer(Combatant combatant) {
        return combatant != null && stateOf(combatant) != null;
    }

    /**
     * 敌人的攻击/受击也要给表现层；玩家与敌人共用同一套动画规则。
     *
     * <p>v0.5.0.5：多段攻击要**一段一段**广播，客户端才有机会排队播放 N 次动画
     * （需求："敌人的多段攻击动画会被按照单次攻击结算。应为攻击多少次就播放多少次动画。"）。
     * 段数由 {@link #setPendingHits} 设入。</p>
     */
    private void emitDamageFx(Combatant source, Combatant target, int absorbed, int hpLoss) {
        int hits = Math.max(1, pendingHits);
        for (int i = 0; i < hits; i++) {
            if (source instanceof Enemy) {
                emitFx(CombatFx.ENEMY_ATTACK, source, target);
            }
            if (isPlayer(target)) {
                if (hpLoss > 0) {
                    emitFx(CombatFx.PLAYER_HURT, source, target);
                } else if (absorbed > 0) {
                    emitFx(CombatFx.PLAYER_HURT_BLOCKED, source, target);
                }
            } else if (target instanceof Enemy && hpLoss > 0) {
                emitFx(CombatFx.ENEMY_HURT, source, target);
            }
        }
    }

    /**
     * 把若干张牌直接放进某名玩家的手牌（v0.0.3 W3：臂弩把【箭在弦上】塞进手牌）。
     *
     * @return 实际放进去的张数（受手牌上限限制）
     */
    public int addCardsToHand(Combatant owner, Card card, int count) {
        PlayerState state = stateOf(owner);
        if (state == null || card == null) {
            return 0;
        }
        int added = 0;
        for (int i = 0; i < count; i++) {
            if (state.hand.size() >= rules.maxHandSize()) {
                log.log("  手牌已满（" + rules.maxHandSize() + "），「" + card.name() + "」没有加入手牌");
                break;
            }
            state.hand.add(card);
            added++;
        }
        if (added > 0) {
            log.log("  " + owner.name() + " 的手牌加入 " + added + " 张「" + card.name() + "」");
        }
        return added;
    }

    // ------------------------------------------------------------------ 遗物（V7）

    /**
     * "本场战斗第一次失去生命值"的广播（v0.2.8）。
     *
     * <p>每个参战者每场战斗只广播一次；只有真的掉了血（hp 减少）才调用，
     * 被格挡完全吸收的伤害不算失去生命。</p>
     */
    private void notifyHpLost(Combatant combatant) {
        if (combatant == null || !hpLossSeen.add(combatant)) {
            return;
        }
        PlayerState state = stateOf(combatant);
        if (state != null) {
            fireRelics(RelicTrigger.FIRST_HP_LOST, state);
        }
    }

    /** 把触发点广播给**当前行动玩家**身上的遗物。 */
    private void fireRelics(RelicTrigger trigger) {
        fireRelics(trigger, activeState());
    }

    /** 把触发点广播给指定玩家身上的遗物（按登记顺序依次触发）。 */
    private void fireRelics(RelicTrigger trigger, PlayerState state) {
        if (state.relics().isEmpty()) {
            return;
        }
        for (Relic relic : state.relics()) {
            if (!relic.firesOn(trigger)) {
                continue;
            }
            log.log("  【" + relic.name() + "】" + relic.description());
            relic.effect().apply(this, state.combatant());
        }
    }

    private void checkBattleEnd() {
        if (phase.isOver()) {
            return;
        }
        // v0.5.4.1（A4-20）【尸体爆炸】：先让"刚死的"敌人爆炸，再判断胜负。
        // 放在这里是因为**所有**击杀路径最后都会走 checkBattleEnd ——
        // 伤害、中毒、凋零、荆棘、状态牌自伤，一个都不漏。
        detonateCorpseExplosions();
        if (phase.isOver()) {
            return;
        }
        if (allPlayersDown()) {
            defeat();
        } else if (aliveEnemies().isEmpty()) {
            victory();
        }
    }

    /** 正在结算【尸体爆炸】时为 true：防止"A 爆炸炸死 B、B 又爆炸"引起的递归重入。 */
    private boolean detonatingCorpseExplosions;

    /**
     * 【尸体爆炸】（v0.5.4.1 A4-20）：把**已经死掉**、身上还带着爆炸标记的敌人结算掉。
     *
     * <p>规则（kamigame「死体爆破」）："対象の敵が死亡した時、その最大HPに等しいダメージを
     * 敵全体に与える" —— 伤害等于**它自己的最大生命值**，打**全体敌人**。</p>
     *
     * <p>两个细节：</p>
     * <ul>
     *   <li>伤害走 {@link #takeFixedDamage}（"等同于最大 HP 的伤害"是固定值，不该被力量 / 虚弱放大，
     *       但照样**会被格挡吸收**）；</li>
     *   <li>用 {@code while} 反复扫：一次爆炸可能炸死另一个带标记的敌人，那个也要接着炸
     *       （原版同样会连锁）。</li>
     * </ul>
     */
    private void detonateCorpseExplosions() {
        if (detonatingCorpseExplosions) {
            return;
        }
        detonatingCorpseExplosions = true;
        try {
            boolean again = true;
            while (again) {
                again = false;
                for (Enemy enemy : enemies) {
                    if (!enemy.corpseExplosion() || enemy.alive()) {
                        continue;
                    }
                    enemy.clearCorpseExplosion();
                    int damage = Math.max(0, enemy.maxHp());
                    log.log("  【尸体爆炸】：" + enemy.name() + " 死亡，对全体敌人造成 " + damage
                            + " 点伤害（等同于其最大生命值）");
                    for (Enemy other : aliveEnemies()) {
                        takeFixedDamage(other, damage, "「尸体爆炸」");
                    }
                    again = true;
                    break;
                }
            }
        } finally {
            detonatingCorpseExplosions = false;
        }
    }

    private void victory() {
        phase = CombatPhase.VICTORY;
        log.log("=== 战斗胜利！第 " + turn + " 回合结束，剩余：" + survivors() + " ===");
        // v0.5.3.1【商队护送】：如果在敌人出手之前就把敌人清完了，也要把这一笔结掉
        resolveEscorts();
        // V7：胜利类遗物（例如"燃烧之血"回血）
        for (PlayerState state : players) {
            if (state.alive()) {
                fireRelics(RelicTrigger.ON_VICTORY, state);
            }
        }
    }

    private void defeat() {
        phase = CombatPhase.DEFEAT;
        log.log("=== 全员倒下，战斗失败（第 " + turn + " 回合）===");
    }

    private String survivors() {
        StringBuilder sb = new StringBuilder();
        for (PlayerState state : players) {
            if (!state.alive()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('、');
            }
            sb.append(state.combatant().name()).append(' ').append(state.combatant().hp()).append('/')
                    .append(state.combatant().maxHp());
        }
        return sb.length() == 0 ? "无人" : sb.toString();
    }

    /** Result of an attempt to play a card. */
    public record PlayResult(boolean success, String message) {
        public static PlayResult ok(String message) {
            return new PlayResult(true, message);
        }

        public static PlayResult fail(String message) {
            return new PlayResult(false, message);
        }
    }
}
