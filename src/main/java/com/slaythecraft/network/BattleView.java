package com.slaythecraft.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/**
 * 战斗快照（v0.0.2 V1）：服务端把"某个玩家视角下的战斗状态"打包给客户端渲染出牌界面。
 *
 * <p>刻意做成**纯数据 + 手写 NBT 编解码**：V2–V12 还会不断往快照里加字段（意图、遗物、药水…），
 * 手写 NBT 的改动成本远低于维护一串 {@code StreamCodec.composite}。代价是编译器帮不上忙，
 * 因此配套 {@code BattleViewCheck} 做编解码往返自检。</p>
 */
public record BattleView(
        boolean active,
        boolean yourTurn,
        String activePlayer,
        int turn,
        String phase,
        int energy,
        int energyPerTurn,
        List<HandCard> hand,
        int drawPile,
        int discardPile,
        int exhaustPile,
        int hp,
        int maxHp,
        int block,
        /** v0.0.3：自己的状态**逐条**列出（客户端画在药水栏左边，换行显示）。 */
        List<String> selfStatuses,
        /** v0.0.3 W4：自己的充能球（渲染成环绕玩家的方块；空栏位用标靶）。 */
        List<String> selfOrbs,
        /** v0.0.3 W4：充能球栏位数量。 */
        int orbSlots,
        List<EnemyView> enemies,
        /** V8：药水槽位内容（槽位顺序；客户端用 id 去药水表取名字与正负性）。 */
        List<ResourceLocation> potions,
        /** v0.4.2：抽牌堆 / 弃牌堆 / 消耗堆的**内容**（"查看牌堆"用；只带 id 与强化标记）。 */
        List<CardRef> drawCards,
        List<CardRef> discardCards,
        List<CardRef> exhaustCards,
        /** v0.4.5：这一战是不是 BOSS 战（客户端据此播放该层 BOSS 音乐）。 */
        boolean bossBattle,
        /** v0.4.5：当前层号（BOSS 音乐按层选曲；非战斗时是玩家档案里的层数）。 */
        int floor,
        /** v0.4.6：自己的药水栏位数（含遗物【药水腰带】的 +2）。 */
        int potionSlots,
        /** v0.4.6：参战名单（正面药水可以对自己 / 队友使用，所以界面要知道有谁）。 */
        List<PartyMember> party,
        /**
         * v0.4.6.7：**手牌是不是已经到上限**。
         *
         * <p>需求原文："当手牌达到上限时，在药水栏上方提示『我的手牌太多了...』"。
         * 上限是服务端的战斗规则（{@code CombatRules#maxHandSize()}），客户端自己猜不准，
         * 所以由快照直接带一个布尔值（见 {@code CombatManager.battleView}）。</p>
         */
        boolean handFull,
        /**
         * v0.5.1.6：**当前是不是"玩家回合"**（一轮内所有玩家同时出牌的那个阶段）。
         *
         * <p>回合模型改造后，"轮到谁"这个问法没有了：玩家回合里所有人都能出牌，
         * 只是各自确认之后就不能再出。客户端用它决定"确认之后还要不要把界面留在屏幕上等队友"
         * —— 敌怪回合时它是 false，那时仍然整屏收起（与旧行为一致）。</p>
         */
        boolean playerRound) {

    /**
     * 一名参战玩家（v0.4.6；v0.5.1.6 加 {@code ready}）：正面药水选择目标时显示。
     *
     * @param self  这一条是不是"我"（客户端据此显示「自己」）
     * @param ready v0.5.1.6：这名玩家**这一轮是否已经确认结束回合**（界面用它显示"等待队友"）
     */
    public record PartyMember(String name, int hp, int maxHp, int block, boolean alive, boolean self,
                              boolean ready) {
    }

    /** 牌堆里的一张牌（客户端用 id 去卡池取名称/费用/描述）。 */
    public record CardRef(ResourceLocation id, boolean upgraded) {
    }

    /**
     * 手牌里的一张卡（只带渲染需要的字段；名称/费用由客户端查卡池）。
     *
     * <p>{@code description} 是 v0.0.3 W1 加的：服务端已经按**当前状态**把卡面数值算好了
     * （虚弱 2 的打击在这里就是"造成 4 点伤害"），客户端直接画，不需要自己算。</p>
     */
    public record HandCard(ResourceLocation id, int cost, boolean needsTarget,
                           /**
                            * v0.5.4.1（A4-20）：**强化的档数**（0 = 基础版）。
                            *
                            * <p>以前这里是 {@code boolean upgraded}：客户端只能看出"有没有强化过"，
                            * 于是手牌里的【灼热打击】+3 会显示成 +1 的名字。改成档数之后，
                            * 客户端可以用 {@code CardLibrary.resolve(id, upgrades)} 取到正确的那一档。</p>
                            */
                           int upgrades, String keywords,
                           String enchantments, String description,
                           /**
                            * v0.5.3.9（A4-18）：这张牌**现在能不能打出**（服务端判定）。
                            *
                            * <p>用于【交锋】这种"条件不满足就打不出去"的牌（手里必须全是攻击牌）。
                            * 客户端据此把它画成不可用（灰掉），不必等点了才收到失败提示。</p>
                            */
                           boolean playable) {
    }

    /**
     * 敌人行显示需要的信息，附带"这个意图是不是冲着我来的"。
     *
     * <p>{@code statuses} 是 v0.0.3 W1 加的：显示在意图框下面（例如 {@code 虚弱 5 · 易伤 2}）。</p>
     *
     * <p>v0.5.0.7：{@code intent} 从**单行字符串**改成 {@code intents} **多行列表**。
     * 原因是需求"存在意图行动超出敌人UI范围导致显示不全的问题。解决方案：有多个行动则换行，
     * 而不是显示『行动1+行动2』导致显示不全。"，而且每一行都带**实际数值**
     * （把力量/虚弱/易伤算进去，和结算共用 {@code previewDamage}）。</p>
     */
    public record EnemyView(String name, int hp, int maxHp, int block, List<String> intents, boolean targetingYou,
                            boolean alive, String statuses, boolean stunned) {
    }

    public BattleView {
        hand = List.copyOf(hand);
        enemies = List.copyOf(enemies);
        potions = List.copyOf(potions);
        selfStatuses = List.copyOf(selfStatuses);
        selfOrbs = List.copyOf(selfOrbs);
        drawCards = List.copyOf(drawCards);
        discardCards = List.copyOf(discardCards);
        exhaustCards = List.copyOf(exhaustCards);
        party = List.copyOf(party);
    }

    /** 非战斗状态（战斗结束/未参战时下发给客户端，用来关掉界面）。 */
    public static BattleView inactive() {
        return new BattleView(false, false, "", 0, "", 0, 0, List.of(), 0, 0, 0, 0, 0, 0, List.of(), List.of(), 0,
                List.of(), List.of(), List.of(), List.of(), List.of(), false, 0, 0, List.of(), false, false);
    }

    // ------------------------------------------------------------------ v0.5.1.6 便利查询

    /**
     * "我这一轮已经确认结束回合了吗"（自己不在名单里时返回 false）。
     *
     * <p>界面用它区分"轮到我"和"我已经交了、在等队友"。</p>
     */
    public boolean selfReady() {
        for (PartyMember member : party) {
            if (member.self()) {
                return member.ready();
            }
        }
        return false;
    }

    /** 还没确认结束回合的**队友**名字（不含自己、不含倒下的人）；都交了就返回空表。 */
    public List<String> waitingFor() {
        List<String> waiting = new ArrayList<>();
        for (PartyMember member : party) {
            if (!member.self() && member.alive() && !member.ready()) {
                waiting.add(member.name());
            }
        }
        return List.copyOf(waiting);
    }

    /**
     * v0.5.1.9：服务端下发的 {@code phase} 里"敌怪回合"的那个值。
     *
     * <p>它必须是引擎 {@code CombatPhase.ENEMY_TURN} 的枚举名 —— 自检里有一条断言专门盯着
     * 这个字面量与枚举对齐，改名会当场挂掉，不会悄悄失效。</p>
     */
    public static final String PHASE_ENEMY_TURN = "ENEMY_TURN";

    /**
     * v0.5.1.9：现在是不是**敌怪回合**。
     *
     * <p>需求原文："出牌回合结束后的敌怪环节，出现了玩家可能会暂时离开战斗UI，
     * 可以按 wasd 移动和旋转视角的问题。" —— 出牌界面改成**整场战斗都留在屏幕上**之后，
     * 客户端需要区分"玩家回合（可以出牌）"和"敌怪回合（只许看、不许动）"，
     * 分别画不同的提示、并让手牌不可交互。</p>
     *
     * <p>判据用 {@code phase} 而不是 {@code !playerRound}：后者的含义是"不是玩家回合"，
     * 会把 {@code VICTORY} / {@code DEFEAT} 也算进来（战斗结束的一瞬间快照仍是 active）。</p>
     */
    public boolean enemyTurn() {
        return active && PHASE_ENEMY_TURN.equals(phase);
    }

    /**
     * v0.5.1.9：整场战斗里界面都该留在屏幕上（除了被 ESC 暂时收起）。
     *
     * <p>以前只有玩家回合显示、敌怪回合收起，于是敌怪行动时玩家掉回世界里，
     * 能按 wasd 走动、能转视角 —— 见 {@link #enemyTurn()} 里的需求原文。</p>
     */
    public boolean keepsHudOpen() {
        return active;
    }

    // ------------------------------------------------------------------ 编解码

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("active", active);
        tag.putBoolean("your_turn", yourTurn);
        // v0.5.1.6：玩家回合标记（一轮内所有人同时出牌）
        tag.putBoolean("player_round", playerRound);
        tag.putString("active_player", activePlayer);
        tag.putInt("turn", turn);
        tag.putString("phase", phase);
        tag.putInt("energy", energy);
        tag.putInt("energy_per_turn", energyPerTurn);
        tag.putInt("draw_pile", drawPile);
        tag.putInt("discard_pile", discardPile);
        tag.putInt("exhaust_pile", exhaustPile);
        tag.putInt("hp", hp);
        tag.putInt("max_hp", maxHp);
        tag.putInt("block", block);
        // v0.4.5：BOSS 战标记与层号（客户端 BOSS 音乐）
        tag.putBoolean("boss_battle", bossBattle);
        tag.putInt("floor", floor);
        // v0.4.6：药水栏位与参战名单
        tag.putInt("potion_slots", potionSlots);
        ListTag partyTag = new ListTag();
        for (PartyMember member : party) {
            CompoundTag entry = new CompoundTag();
            entry.putString("name", member.name());
            entry.putInt("hp", member.hp());
            entry.putInt("max_hp", member.maxHp());
            entry.putInt("block", member.block());
            entry.putBoolean("alive", member.alive());
            entry.putBoolean("self", member.self());
            // v0.5.1.6：这一轮是否已经确认结束回合
            entry.putBoolean("ready", member.ready());
            partyTag.add(entry);
        }
        tag.put("party", partyTag);
        // v0.4.6.7：手牌满标记（客户端在药水栏上方提示"我的手牌太多了..."）
        tag.putBoolean("hand_full", handFull);

        // v0.0.3：自己的状态逐条（换行显示）
        ListTag selfTag = new ListTag();
        for (String status : selfStatuses) {
            selfTag.add(net.minecraft.nbt.StringTag.valueOf(status));
        }
        tag.put("self_statuses", selfTag);

        // v0.0.3 W4：充能球（栏位数 + 每颗球的 id）
        tag.putInt("orb_slots", orbSlots);
        ListTag orbTag = new ListTag();
        for (String orb : selfOrbs) {
            orbTag.add(net.minecraft.nbt.StringTag.valueOf(orb));
        }
        tag.put("self_orbs", orbTag);

        ListTag handTag = new ListTag();
        for (HandCard card : hand) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", card.id().toString());
            entry.putInt("cost", card.cost());
            entry.putBoolean("needs_target", card.needsTarget());
            entry.putInt("upgrades", card.upgrades());
            entry.putBoolean("playable", card.playable());
            entry.putString("keywords", card.keywords());
            entry.putString("enchantments", card.enchantments());
            entry.putString("description", card.description());
            handTag.add(entry);
        }
        tag.put("hand", handTag);

        ListTag enemyTag = new ListTag();
        for (EnemyView enemy : enemies) {
            CompoundTag entry = new CompoundTag();
            entry.putString("name", enemy.name());
            entry.putInt("hp", enemy.hp());
            entry.putInt("max_hp", enemy.maxHp());
            entry.putInt("block", enemy.block());
            // v0.5.0.7：意图改成多行（每个行动一行），不会再被 " + " 挤到显示不全
            ListTag intentTag = new ListTag();
            for (String line : enemy.intents()) {
                intentTag.add(net.minecraft.nbt.StringTag.valueOf(line));
            }
            entry.put("intents", intentTag);
            entry.putBoolean("targeting_you", enemy.targetingYou());
            entry.putBoolean("alive", enemy.alive());
            entry.putString("statuses", enemy.statuses());
            entry.putBoolean("stunned", enemy.stunned());
            enemyTag.add(entry);
        }
        tag.put("enemies", enemyTag);

        ListTag potionTag = new ListTag();
        for (ResourceLocation potion : potions) {
            potionTag.add(net.minecraft.nbt.StringTag.valueOf(potion.toString()));
        }
        tag.put("potions", potionTag);

        // v0.4.2：三个牌堆的内容（"查看牌堆"页面用）
        tag.put("draw_cards", cardRefs(drawCards));
        tag.put("discard_cards", cardRefs(discardCards));
        tag.put("exhaust_cards", cardRefs(exhaustCards));
        return tag;
    }

    private static ListTag cardRefs(List<CardRef> cards) {
        ListTag list = new ListTag();
        for (CardRef card : cards) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", card.id().toString());
            entry.putBoolean("upgraded", card.upgraded());
            list.add(entry);
        }
        return list;
    }

    public static BattleView fromTag(CompoundTag tag) {
        List<String> selfStatuses = new ArrayList<>();
        ListTag selfTag = tag.getList("self_statuses", Tag.TAG_STRING);
        for (int i = 0; i < selfTag.size(); i++) {
            selfStatuses.add(selfTag.getString(i));
        }
        List<String> selfOrbs = new ArrayList<>();
        ListTag orbTag = tag.getList("self_orbs", Tag.TAG_STRING);
        for (int i = 0; i < orbTag.size(); i++) {
            selfOrbs.add(orbTag.getString(i));
        }
        int orbSlots = tag.getInt("orb_slots");

        List<HandCard> hand = new ArrayList<>();
        ListTag handTag = tag.getList("hand", Tag.TAG_COMPOUND);
        for (int i = 0; i < handTag.size(); i++) {
            CompoundTag entry = handTag.getCompound(i);
            hand.add(new HandCard(ResourceLocation.parse(entry.getString("id")), entry.getInt("cost"),
                    entry.getBoolean("needs_target"),
                    // v0.5.4.1：新快照写 "upgrades"（档数）；老快照只有 "upgraded"（布尔）→ 当成 0/1
                    entry.contains("upgrades") ? entry.getInt("upgrades")
                            : (entry.getBoolean("upgraded") ? 1 : 0),
                    entry.getString("keywords"),
                    entry.getString("enchantments"), entry.getString("description"),
                    // 老快照没有这个字段时按"能打出"处理（宽容解码）
                    !entry.contains("playable") || entry.getBoolean("playable")));
        }

        List<EnemyView> enemies = new ArrayList<>();
        ListTag enemyTag = tag.getList("enemies", Tag.TAG_COMPOUND);
        for (int i = 0; i < enemyTag.size(); i++) {
            CompoundTag entry = enemyTag.getCompound(i);
            // v0.5.0.7：意图是多行列表，每个行动一行
            ListTag intentTag = entry.getList("intents", Tag.TAG_STRING);
            List<String> intents = new ArrayList<>(intentTag.size());
            for (int line = 0; line < intentTag.size(); line++) {
                intents.add(intentTag.getString(line));
            }
            enemies.add(new EnemyView(entry.getString("name"), entry.getInt("hp"), entry.getInt("max_hp"),
                    entry.getInt("block"), intents, entry.getBoolean("targeting_you"),
                    entry.getBoolean("alive"), entry.getString("statuses"),
                    entry.getBoolean("stunned")));
        }

        List<ResourceLocation> potions = new ArrayList<>();
        ListTag potionTag = tag.getList("potions", Tag.TAG_STRING);
        for (int i = 0; i < potionTag.size(); i++) {
            potions.add(ResourceLocation.parse(potionTag.getString(i)));
        }

        return new BattleView(
                tag.getBoolean("active"),
                tag.getBoolean("your_turn"),
                tag.getString("active_player"),
                tag.getInt("turn"),
                tag.getString("phase"),
                tag.getInt("energy"),
                tag.getInt("energy_per_turn"),
                hand,
                tag.getInt("draw_pile"),
                tag.getInt("discard_pile"),
                tag.getInt("exhaust_pile"),
                tag.getInt("hp"),
                tag.getInt("max_hp"),
                tag.getInt("block"),
                selfStatuses,
                selfOrbs,
                orbSlots,
                enemies,
                potions,
                readCardRefs(tag.getList("draw_cards", Tag.TAG_COMPOUND)),
                readCardRefs(tag.getList("discard_cards", Tag.TAG_COMPOUND)),
                readCardRefs(tag.getList("exhaust_cards", Tag.TAG_COMPOUND)),
                tag.getBoolean("boss_battle"),
                tag.getInt("floor"),
                tag.getInt("potion_slots"),
                readParty(tag.getList("party", Tag.TAG_COMPOUND)),
                // v0.4.6.7：手牌满标记
                tag.getBoolean("hand_full"),
                // v0.5.1.6：玩家回合标记
                tag.getBoolean("player_round"));
    }

    private static List<PartyMember> readParty(ListTag tag) {
        List<PartyMember> party = new ArrayList<>();
        for (int i = 0; i < tag.size(); i++) {
            CompoundTag entry = tag.getCompound(i);
            party.add(new PartyMember(entry.getString("name"), entry.getInt("hp"), entry.getInt("max_hp"),
                    entry.getInt("block"), entry.getBoolean("alive"), entry.getBoolean("self"),
                    entry.getBoolean("ready")));
        }
        return List.copyOf(party);
    }

    private static List<CardRef> readCardRefs(ListTag tag) {
        List<CardRef> cards = new ArrayList<>();
        for (int i = 0; i < tag.size(); i++) {
            CompoundTag entry = tag.getCompound(i);
            cards.add(new CardRef(ResourceLocation.parse(entry.getString("id")), entry.getBoolean("upgraded")));
        }
        return cards;
    }
}
