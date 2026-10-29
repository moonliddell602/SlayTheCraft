package com.slaythecraft.combat.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 一名参战玩家的私有战斗状态（设计文档 §10.2）：能量、手牌、抽牌堆、弃牌堆、本回合出牌数。
 *
 * <p>多玩家战斗里每个玩家各有一份；敌人是共享的（由 {@link CombatContext} 持有）。</p>
 */
public final class PlayerState {
    private final Combatant combatant;
    private final int energyPerTurn;
    /** 这名玩家携带的遗物（v0.0.2 V7）。 */
    private final List<Relic> relics;
    final List<Card> hand = new ArrayList<>();
    final Pile drawPile = new Pile();
    final Pile discardPile = new Pile();
    final Pile exhaustPile = new Pile();
    int energy;
    int cardsPlayedThisTurn;
    /** 最近一次打出的 X 费卡的 X（v0.4.7.3：那一下花掉的能量数）。 */
    int lastX;
    /**
     * 连续打出同**定义**手牌的计数（v0.5.0.10 隐藏成就【回响形态了 回响形态】）。
     *
     * <p>需求："连续打出 4 张同名手牌。" 这里按**卡牌 id** 判定：同一张定义（例如 4 张【打击】）
     * 连打 4 次就算；中间打出别的牌就归 1 重来。</p>
     */
    String lastPlayedCardId;
    int sameCardStreak;
    /** 还要跳过几个自己的回合（v0.4.7.5 紧急避险的"下一回合晕眩"）。 */
    int skipTurns;

    /**
     * 这一轮里是否**已经确认结束回合**（v0.5.1.6 回合模型改造）。
     *
     * <p>所有玩家的回合现在是同一个"玩家回合"：谁都可以在自己还没确认时出牌，
     * 全员都确认了才进入敌怪回合。倒下的玩家视为已确认。</p>
     */
    boolean turnDone;

    /** 这一轮是否已经确认结束回合（只读）。 */
    public boolean turnDone() {
        return turnDone;
    }

    /**
     * 正在等待玩家选牌的效果（v0.5.1.7 A3：卡面写着"选择…"的牌要真的让玩家选）。
     *
     * <p>卡牌效果没法阻塞在界面上，所以这里登记"要选几张、从哪个牌堆选"，
     * 由 Minecraft 层弹选牌页、再把下标回传（见 {@code CombatContext.resolveCardPick}）。</p>
     */
    CombatContext.PendingCardPick pendingPick;
    /** 本场战斗后要从牌组里彻底移除的卡 id（v0.4.7.8 不屈）。 */
    final java.util.Set<String> removeFromDeck = new java.util.LinkedHashSet<>();

    /**
     * 本场战斗结束时要从牌组**永久删掉**的卡 id（只读快照，v0.5.1.6）。
     *
     * <p>引擎只负责记账；真正写回玩家档案由 Minecraft 层做（见
     * {@code CombatManager.discardRemovedCards}）—— 那边走的是和卡牌回收站同一个删牌方法。</p>
     */
    public java.util.Set<String> removeFromDeck() {
        return java.util.Set.copyOf(removeFromDeck);
    }

    /**
     * 【村庄英雄】（v0.5.1.4）：本场战斗里每打 3 张攻击牌 → 随机敌人 15 层凋零；
     * 每打 3 张技能牌 → 自己与队友各 1 层附甲；并且**附甲不再衰减**。
     */
    boolean villageHero;
    /** 【村庄英雄】的二计数：攻击牌 / 技能牌各打了几张（每满 3 触发一次）。 */
    int heroAttacks;
    int heroSkills;

    /** 【村庄英雄】是否已生效（本场战斗）。 */
    public boolean villageHero() {
        return villageHero;
    }

    /** 攻击牌 / 技能牌的累计张数（自检与界面用）。 */
    public int heroAttacks() {
        return heroAttacks;
    }

    public int heroSkills() {
        return heroSkills;
    }

    /** 还要跳过几个自己的回合。 */
    public int skipTurns() {
        return skipTurns;
    }

    /** 最近一次打出的 X 费卡的 X（只读）。 */
    public int lastX() {
        return lastX;
    }

    /** 连续打出同定义手牌的当前计数（v0.5.0.10 隐藏成就）。 */
    public int sameCardStreak() {
        return sameCardStreak;
    }

    /** 最近一次打出的卡牌 id（没有则为 {@code null}）。 */
    public String lastPlayedCardId() {
        return lastPlayedCardId;
    }

    /**
     * 记下这次打出的牌，返回**更新后的连打计数**（v0.5.0.10）。
     *
     * <p>同一个卡牌 id 连续打出就累加；换了别的牌就从 1 重新开始。</p>
     */
    public int recordPlayedCard(String cardId) {
        if (cardId != null && cardId.equals(lastPlayedCardId)) {
            sameCardStreak++;
        } else {
            sameCardStreak = 1;
        }
        lastPlayedCardId = cardId;
        return sameCardStreak;
    }
    /**
     * v0.4.2：【触媒】额外触发【中毒】结算的次数（0 = 只触发一次；v0.4.6.1 起"涂毒"更名为"中毒"）。
     */
    /** 中毒结算的额外触发次数（【触媒】，v0.4.2 的"涂毒触发"在 v0.4.6 更名）。 */
    int poisonExtraTriggers;
    /**
     * v0.4.2：【余像】—— 每打出一张牌就获得 1 点格挡（能力牌，本场战斗有效）。
     */
    boolean afterImage;
    /** 是否已经开过自己的第一个回合（"固有"与"战斗开始"类遗物只在第一回合生效）。 */
    boolean firstTurnDone;

    public PlayerState(Combatant combatant, List<Card> deck, int energyPerTurn, List<Relic> relics) {
        this.combatant = combatant;
        this.energyPerTurn = Math.max(1, energyPerTurn);
        this.relics = relics == null ? List.of() : List.copyOf(relics);
        this.drawPile.addAll(deck);
    }

    public PlayerState(Combatant combatant, List<Card> deck, int energyPerTurn) {
        this(combatant, deck, energyPerTurn, List.of());
    }

    /** 该玩家携带的遗物。 */
    public List<Relic> relics() {
        return relics;
    }

    /** 该玩家每回合的能量（角色不同则不同，所以放在玩家状态上而不是全局规则里）。 */
    public int energyPerTurn() {
        return energyPerTurn;
    }

    public Combatant combatant() {
        return combatant;
    }

    public boolean alive() {
        return combatant.alive();
    }

    /** 手牌只读快照。 */
    public List<Card> hand() {
        return List.copyOf(hand);
    }

    public Pile drawPile() {
        return drawPile;
    }

    public Pile discardPile() {
        return discardPile;
    }

    public Pile exhaustPile() {
        return exhaustPile;
    }

    public int energy() {
        return energy;
    }

    public int cardsPlayedThisTurn() {
        return cardsPlayedThisTurn;
    }

    /** 【触媒】给的额外【中毒】触发次数（v0.4.2）。 */
    public int poisonExtraTriggers() {
        return poisonExtraTriggers;
    }

    public void addPoisonTriggers(int amount) {
        poisonExtraTriggers = Math.max(0, poisonExtraTriggers + amount);
    }

    /** 【余像】是否已生效（v0.4.2）。 */
    public boolean afterImage() {
        return afterImage;
    }

    public void setAfterImage(boolean value) {
        this.afterImage = value;
    }

    // ------------------------------------------------------------------ v0.5.3.1 两张"每打一张牌"的能力

    /**
     * 【众生皆苦】：本场战斗里**每打出一张牌**（包括这张能力牌自己）给所有敌人 1 层凋零。
     */
    boolean allSuffering;

    /**
     * 【劫掠形态】：本场战斗里每打出一张攻击牌，就往手牌里加一张「弩箭射击」；
     * {@link #pillageFormUpgraded} 为真时加的是强化版【弩箭射击+】（升级后的卡）。
     */
    boolean pillageForm;
    boolean pillageFormUpgraded;

    /** 【众生皆苦】是否已生效。 */
    public boolean allSuffering() {
        return allSuffering;
    }

    /** 【劫掠形态】是否已生效。 */
    public boolean pillageForm() {
        return pillageForm;
    }

    /** 【劫掠形态】加的是不是强化版【弩箭射击+】。 */
    public boolean pillageFormUpgraded() {
        return pillageFormUpgraded;
    }

    // ------------------------------------------------------------------ v0.5.3.3（A4-12）回合钩子

    /**
     * A4-12 的四张**回合钩子能力牌**：引擎在{@code CombatContext} 的
     * {@code onTurnStartForPowers} / {@code onTurnEndForPowers} 两个派发点读这些字段。
     *
     * <ul>
     *   <li>{@link #metallicize}【金属化】：回合结束时获得这么多格挡（0 = 没这张牌）；</li>
     *   <li>{@link #combust}【自燃】：回合结束时失去 1 点生命，并对所有敌人造成这么多伤害；</li>
     *   <li>{@link #infiniteBlades}【无限刀刃】：回合开始时加一张『小刀』进手牌；</li>
     *   <li>{@link #noxiousFumes}【毒雾】：回合开始时对所有敌人施加这么多层中毒。</li>
     * </ul>
     */
    int metallicize;
    int combust;
    boolean infiniteBlades;
    int noxiousFumes;

    /** 【金属化】每回合结束给多少格挡。 */
    public int metallicize() {
        return metallicize;
    }

    /** 【自燃】每回合结束对全体敌人的伤害（同时自己失去 1 点生命）。 */
    public int combust() {
        return combust;
    }

    /** 【无限刀刃】是否已生效。 */
    public boolean infiniteBlades() {
        return infiniteBlades;
    }

    /** 【毒雾】每回合开始给全体敌人的中毒层数。 */
    public int noxiousFumes() {
        return noxiousFumes;
    }

    // ------------------------------------------------------------------ v0.5.3.3【布置】的免费牌

    /**
     * 【布置】的账：**这些 id 的牌被打出之前费用为 0**（每登记一次只能用掉一次）。
     *
     * <p>为什么是"按 id 计数"而不是"按某一张牌"：本引擎的 {@code Card} 是**不可变共享定义**，
     * 没有"卡牌实例"这个概念（见 A4 计划 §3 的 E5）。所以这里用
     * 「id → 还能免费几次」近似：手里有两张同名牌时，先打出的那一张免费。
     * 实战中【布置】把牌放到**抽牌堆顶**，下一张抽到的就是它，所以顺序上不会有歧义。</p>
     */
    final java.util.Map<String, Integer> freeToPlay = new java.util.LinkedHashMap<>();

    /** 这张牌现在是不是"打出前费用为 0"。 */
    public boolean isFreeToPlay(String cardId) {
        return cardId != null && freeToPlay.getOrDefault(cardId, 0) > 0;
    }

    /** 登记一次"这张牌打出前费用为 0"。 */
    public void markFreeToPlay(String cardId) {
        if (cardId != null) {
            freeToPlay.merge(cardId, 1, Integer::sum);
        }
    }

    /** 用掉一次免费额度（打出时调用）；没额度返回 false。 */
    public boolean consumeFreeToPlay(String cardId) {
        if (!isFreeToPlay(cardId)) {
            return false;
        }
        int left = freeToPlay.get(cardId) - 1;
        if (left <= 0) {
            freeToPlay.remove(cardId);
        } else {
            freeToPlay.put(cardId, left);
        }
        return true;
    }

    /** 还欠着几次"免费"（自检与界面用）。 */
    public int freeToPlayCount() {
        int total = 0;
        for (int count : freeToPlay.values()) {
            total += count;
        }
        return total;
    }

    /**
     * 【地狱之刃】（A4-16）：**本回合**给某张牌"费用为 0"，回合结束就作废。
     *
     * <p>与 {@link #freeToPlay}（【布置】的"打出之前一直 0 费"）刻意分开：
     * 【布置】的那张牌躺在**抽牌堆顶**、要到下回合才抽到，所以不能跟着回合结束清掉。</p>
     */
    final java.util.Map<String, Integer> freeThisTurn = new java.util.LinkedHashMap<>();

    /** 这张牌**本回合**是不是 0 费。 */
    public boolean isFreeThisTurn(String cardId) {
        return cardId != null && freeThisTurn.getOrDefault(cardId, 0) > 0;
    }

    /** 登记一次"这张牌本回合 0 费"。 */
    public void markFreeThisTurn(String cardId) {
        if (cardId != null) {
            freeThisTurn.merge(cardId, 1, Integer::sum);
        }
    }

    /** 用掉一次"本回合 0 费"额度。 */
    public boolean consumeFreeThisTurn(String cardId) {
        if (!isFreeThisTurn(cardId)) {
            return false;
        }
        int left = freeThisTurn.get(cardId) - 1;
        if (left <= 0) {
            freeThisTurn.remove(cardId);
        } else {
            freeThisTurn.put(cardId, left);
        }
        return true;
    }

    /** 回合结束：清掉"本回合 0 费"的账。 */
    public void clearFreeThisTurn() {
        freeThisTurn.clear();
    }

    // ------------------------------------------------------------------ v0.5.3.3（A4-13）出牌 / 格挡钩子

    /**
     * A4-13 的四张"钩子能力牌" + 两张"本回合下一次双发"：
     *
     * <ul>
     *   <li>{@link #thousandCuts}【千刀万剐】：**每打出一张牌**（含它自己）→ 全体敌人受这么多伤害；</li>
     *   <li>{@link #envenom}【涂毒】：**攻击牌造成伤害**时给那个敌人这么多层中毒；</li>
     *   <li>{@link #juggernaut}【主宰】：**每次获得格挡** → 随机敌人受这么多伤害；</li>
     *   <li>{@link #corruption}【腐化】：技能牌费用为 0，且打完**直接消耗**；</li>
     *   <li>{@link #doubleTap}【双重施法】/ {@link #burstSkills}【爆发】：
     *       本回合接下来的 N 张攻击 / 技能牌打两遍（回合结束时清零）。</li>
     * </ul>
     */
    int thousandCuts;
    int envenom;
    int juggernaut;
    boolean corruption;
    int doubleTap;
    int burstSkills;

    /** 【千刀万剐】每打出一张牌对全体敌人的伤害。 */
    public int thousandCuts() {
        return thousandCuts;
    }

    /** 【涂毒】每次攻击命中给的中毒层数。 */
    public int envenom() {
        return envenom;
    }

    /** 【主宰】每次获得格挡时的伤害。 */
    public int juggernaut() {
        return juggernaut;
    }

    /** 【腐化】是否已生效（技能免费 + 打完消耗）。 */
    public boolean corruption() {
        return corruption;
    }

    /** 【双重施法】本回合还能双发几张攻击牌。 */
    public int doubleTap() {
        return doubleTap;
    }

    /** 【爆发】本回合还能双发几张技能牌。 */
    public int burstSkills() {
        return burstSkills;
    }

    /**
     * 这一张牌**这一次**该按什么类型双发（A4-13）。
     *
     * @return {@code true} = 这次要打两遍（并顺手扣掉一次额度）
     */
    public boolean consumeDoublePlay(CardType type) {
        if (type == CardType.ATTACK && doubleTap > 0) {
            doubleTap--;
            return true;
        }
        if (type == CardType.SKILL && burstSkills > 0) {
            burstSkills--;
            return true;
        }
        return false;
    }

    /** 回合结束：清掉"本回合"的双发额度。 */
    public void clearDoublePlays() {
        doubleTap = 0;
        burstSkills = 0;
    }

    /** 回合结束：清掉"本回合"的【狂怒】/【窒息】/【子弹时间】/【幻影杀手】。 */
    public void clearTurnPowers() {
        rage = 0;
        clearFreeThisTurn();
        // v0.5.4.0（A4-19）
        chokeAmount = 0;
        chokeEnemy = null;
        bulletTime = false;
        phantomKiller = false;
    }

    // ------------------------------------------------------------------ v0.5.3.5（A4-14）抽牌 / 消耗钩子

    /**
     * A4-14 的三张"钩子能力牌"（都是战士的）：
     *
     * <ul>
     *   <li>{@link #evolve}【进化】：**每抽到一张状态牌** → 再抽这么多张；</li>
     *   <li>{@link #fireBreathing}【火焰吐息】：**每抽到一张状态牌或诅咒牌** → 全体敌人受这么多伤害；</li>
     *   <li>{@link #feelNoPain}【无痛】：**每消耗一张牌** → 获得这么多格挡。</li>
     * </ul>
     */
    int evolve;
    int fireBreathing;
    int feelNoPain;

    /** 【进化】每抽到一张状态牌再抽几张。 */
    public int evolve() {
        return evolve;
    }

    /** 【火焰吐息】每抽到状态 / 诅咒牌时对全体敌人的伤害。 */
    public int fireBreathing() {
        return fireBreathing;
    }

    /** 【无痛】每消耗一张牌给多少格挡。 */
    public int feelNoPain() {
        return feelNoPain;
    }

    // ------------------------------------------------------------------ v0.5.3.6（A4-15）消耗 / 出牌钩子

    /**
     * A4-15 的两张能力牌旗标 + 【终结技】要用的计数：
     *
     * <ul>
     *   <li>{@link #darkEmbrace}【黑暗拥抱】：**每消耗一张牌** → 抽这么多张；</li>
     *   <li>{@link #rage}【狂怒】：**本回合**每打出一张攻击牌 → 获得这么多格挡（回合结束清零）；</li>
     *   <li>{@link #attacksPlayedThisTurn}【终结技】：本回合**已经打出的攻击牌张数**（每个回合重置）。</li>
     * </ul>
     */
    int darkEmbrace;
    int rage;
    int attacksPlayedThisTurn;

    /** 【残忍】（A4-16）：**回合开始时**失去 1 点生命并抽 1 张牌（>0 = 已生效）。 */
    int brutality;

    /** 【狂暴】（A4-17，稀有版）：每个回合开始额外获得多少能量（>0 = 已生效）。 */
    int berserk;

    // ---------------------------------------------------------------- v0.5.4.0（A4-19）猎手的三张"本回合"牌

    /** 【窒息】：本回合每打出一张牌，这个敌人失去这么多**生命**（无视格挡）。 */
    Enemy chokeEnemy;
    int chokeAmount;

    /**
     * 【幻影杀手】：**下个回合**你的攻击伤害翻倍（kamigame 原文是「次のターン」= 下个回合）。
     *
     * <p>所以分两半：{@code phantomKillerPending} 是"已经打了牌、等下回合生效"，
     * {@code phantomKiller} 是"这个回合正在生效"。回合开始时把 pending 挪过来，回合结束时清掉生效位。</p>
     */
    boolean phantomKillerPending;
    boolean phantomKiller;

    /** 【子弹时间】：本回合手牌全部 0 费，且不能再抽牌。 */
    boolean bulletTime;

    /** 【幽魂形态】：是否已生效（**回合结束时**失去 1 点敏捷，与原版一致）。 */
    boolean wraithForm;

    // ------------------------------------------------------------------ v0.5.4.1（A4-20）收官批

    /**
     * A4-20 三张牌的记账：
     *
     * <ul>
     *   <li>{@link #pendingPotions}【炼金】：本场战斗"该发还没发"的药水瓶数 —— 药水存在档案层
     *       （{@code PlayerCardData.potions}），零 MC 依赖的引擎碰不到它，所以引擎只记账，
     *       由 MC 层在**这张牌打完之后**立刻兑现（{@code CombatManager.grantPendingPotions}）；</li>
     *   <li>{@link #toolsOfTheTrade}【行业工具】：回合开始"抽 1 张、弃 1 张"的层数（打出两张就叠两层）；</li>
     *   <li>{@link #wellLaidPlans}【计划周全】：回合结束**额外保留**的手牌张数（升级版每张给 2）。</li>
     * </ul>
     */
    int pendingPotions;
    int toolsOfTheTrade;
    int wellLaidPlans;

    /** 【窒息】的目标（没有则为 {@code null}）。 */
    public Enemy chokeEnemy() {
        return chokeEnemy;
    }

    /** 【窒息】本回合的每张牌伤害。 */
    public int chokeAmount() {
        return chokeAmount;
    }

    /** 【幻影杀手】是否已生效（本回合）。 */
    public boolean phantomKiller() {
        return phantomKiller;
    }

    /** 【幻影杀手】是否已经打出、正在等下个回合生效。 */
    public boolean phantomKillerPending() {
        return phantomKillerPending;
    }

    /** 【子弹时间】是否已生效（本回合）。 */
    public boolean bulletTime() {
        return bulletTime;
    }

    /** 【幽魂形态】是否已生效（每个回合开始 -1 敏捷）。 */
    public boolean wraithForm() {
        return wraithForm;
    }

    /** 【炼金】记一笔"待发药水"（引擎记账，MC 层兑现）。 */
    public void addPendingPotion(int count) {
        if (count > 0) {
            pendingPotions += count;
        }
    }

    /** 现在有几瓶"待发药水"（不取走；自检用）。 */
    public int pendingPotions() {
        return pendingPotions;
    }

    /** 取走并清空待发药水（MC 层打完【炼金】后立刻兑现）。 */
    public int takePendingPotions() {
        int taken = pendingPotions;
        pendingPotions = 0;
        return taken;
    }

    /** 【行业工具】回合开始"抽 1 张、弃 1 张"的层数。 */
    public int toolsOfTheTrade() {
        return toolsOfTheTrade;
    }

    /** 【计划周全】回合结束额外保留几张手牌。 */
    public int wellLaidPlans() {
        return wellLaidPlans;
    }

    /** 【狂暴】每回合开始的额外能量。 */
    public int berserk() {
        return berserk;
    }

    /** 【残忍】是否已生效。 */
    public int brutality() {
        return brutality;
    }

    /** 【黑暗拥抱】每消耗一张牌抽几张。 */
    public int darkEmbrace() {
        return darkEmbrace;
    }

    /** 【狂怒】本回合每打出一张攻击牌给多少格挡。 */
    public int rage() {
        return rage;
    }

    /** 本回合已经打出的攻击牌张数（【终结技】用）。 */
    public int attacksPlayedThisTurn() {
        return attacksPlayedThisTurn;
    }

    // ------------------------------------------------------------------ v0.5.3.8（A4-17）E5 单卡记账

    /**
     * **E5：单卡"记账"**（v0.5.3.8 A4-17）。
     *
     * <p>本引擎的 {@code Card} 是**不可变共享定义**、没有"这张牌自己记一个数"的地方，
     * 所以这里按 **id** 记一份数：</p>
     *
     * <ul>
     *   <li>{@link #cardCounters}【狂暴】的"本场战斗 +5 伤害"次数、【玻璃刀】的"打出一次 -2"次数；</li>
     *   <li>{@link #hpLossEvents}【血债血偿】/【高超打击】要的"本场战斗失去过几次生命"；</li>
     *   <li>{@link #cardsDiscardedThisTurn}【剔骨】要的"本回合弃过几张牌"。</li>
     * </ul>
     *
     * <p>⚠️ 局限（与 {@code freeToPlay} 同一类近似）：**同名多张牌共享这一份账**。
     * 手里两张【狂暴】时，它们会一起涨伤害 —— 这是"没有卡牌实例"的直接后果，
     * 已写进交接稿 §4.45 的已知偏差里。</p>
     */
    final java.util.Map<String, Integer> cardCounters = new java.util.LinkedHashMap<>();

    /** 本场战斗"失去生命的次数"（【血债血偿】每失去一次 -1 费；【高超打击】每失去一次 +1 费）。 */
    int hpLossEvents;

    /** 本回合"从手牌弃掉的张数"（【剔骨】每弃一张 -1 费）。 */
    int cardsDiscardedThisTurn;

    /**
     * **下个回合开始时加入手牌的牌**（v0.5.3.9 A4-18【噩梦】）—— 选中的那张牌的 N 张复制。
     *
     * <p>与"下回合抽 N 张"（{@code Combatant.nextTurnDraw}）的区别：这里是**指定的牌**，
     * 不是抽牌堆里抽出来的。在 {@code beginPlayerTurn} 里、正常抽牌**之前**加入。</p>
     */
    final List<Card> nextTurnCards = new java.util.ArrayList<>();

    /** 登记"下个回合开始时加入手牌"的一张牌。 */
    public void addNextTurnCard(Card card) {
        if (card != null) {
            nextTurnCards.add(card);
        }
    }

    /** 取走并清空"下个回合要加入手牌的牌"。 */
    public List<Card> takeNextTurnCards() {
        List<Card> copy = List.copyOf(nextTurnCards);
        nextTurnCards.clear();
        return copy;
    }

    /** 某张牌记的数（没有就是 0）。 */
    public int cardCounter(String cardId) {
        return cardId == null ? 0 : cardCounters.getOrDefault(cardId, 0);
    }

    /** 给某张牌记账 +{@code delta}。 */
    public void addCardCounter(String cardId, int delta) {
        if (cardId != null && delta != 0) {
            cardCounters.merge(cardId, delta, Integer::sum);
        }
    }

    /** 本场战斗失去生命的次数。 */
    public int hpLossEvents() {
        return hpLossEvents;
    }

    /** 记一次"失去生命"（玩家）。 */
    public void recordHpLoss() {
        hpLossEvents++;
    }

    /** 本回合弃掉的张数。 */
    public int cardsDiscardedThisTurn() {
        return cardsDiscardedThisTurn;
    }

    /** 记一次"弃掉一张手牌"。 */
    public void recordDiscard() {
        cardsDiscardedThisTurn++;
    }

    // ------------------------------------------------------------------ v0.5.3.1【商队护送】

    /**
     * 【商队护送】的护送名单（打完那张牌的当回合，被"你与队友"覆盖的人）。
     *
     * <p>为什么要把名单**存下来**而不是结算时现取场上的存活玩家：被护送的人如果在这一轮里
     * 被打倒，他就不在"存活玩家"里了，可他的格挡恰恰是**被打穿了**的 —— 现取会把这次失败
     * 悄悄漏掉，反而白送金币。</p>
     */
    final List<Combatant> escorted = new java.util.ArrayList<>();

    /** 【商队护送】是否正在等这一轮结束的结算。 */
    boolean escortPending;

    /** 【商队护送】已经攒下的额外金币（战斗结束后由 Minecraft 层入账）。 */
    int escortGold;

    /** 【商队护送】成功结算过几次。 */
    int escortSuccesses;

    /** 本回合是否有一次【商队护送】等待结算。 */
    public boolean escortPending() {
        return escortPending;
    }

    /** 这次护送覆盖了哪些人（只读快照）。 */
    public List<Combatant> escorted() {
        return List.copyOf(escorted);
    }

    /** 登记一次护送（打完牌那一刻调用）。 */
    public void armEscort(List<Combatant> members) {
        escorted.clear();
        if (members != null) {
            escorted.addAll(members);
        }
        escortPending = true;
    }

    /** 结算完毕后收尾（`succeeded` 只用于计数）。 */
    public void finishEscort(boolean succeeded) {
        escortPending = false;
        escorted.clear();
        if (succeeded) {
            escortSuccesses++;
        }
    }

    /** 【商队护送】攒下的额外金币。 */
    public int escortGold() {
        return escortGold;
    }

    /** 【商队护送】再加一笔额外金币。 */
    public void addEscortGold(int amount) {
        if (amount > 0) {
            escortGold += amount;
        }
    }

    /** 【商队护送】成功次数。 */
    public int escortSuccesses() {
        return escortSuccesses;
    }

    /** 手牌编号 → 可读的一行（行动栏与状态输出共用）。 */
    public String describeHand() {
        if (hand.isEmpty()) {
            return "（空）";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hand.size(); i++) {
            if (i > 0) {
                sb.append(" | ");
            }
            sb.append(i + 1).append('.').append(hand.get(i).name()).append('(').append(hand.get(i).cost()).append(')');
        }
        return sb.toString();
    }
}
