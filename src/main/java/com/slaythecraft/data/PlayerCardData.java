package com.slaythecraft.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.slaythecraft.combat.core.EventCaps;
import com.slaythecraft.combat.core.EventKind;
import com.slaythecraft.combat.core.LifePool;
import com.slaythecraft.combat.core.ProgressState;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * 玩家卡牌档案（设计文档 §5）。**以玩家为单位**，而不是以【卡牌收纳包】物品为单位：
 * 收纳包只是打开这份档案的钥匙，做多少个包、包丢没丢，都不影响这里的数据。
 *
 * <p>可变对象：奖励、删卡、金币增减都直接改这份实例，再由 Attachment 负责持久化。</p>
 */
public final class PlayerCardData {

    /**
     * 独立生命值的编解码（v0.0.2 V9）。
     *
     * <p>单独做成一个字段：DFU 的 {@code RecordCodecBuilder.group} 最多 16 个字段，
     * 把"生命值 + 进度"各自打包成一个子对象，既躲开上限也让格式更清楚。</p>
     */
    private static final Codec<LifePool> LIFE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("hp", LifePool.DEFAULT_MAX).forGetter(LifePool::hp),
            Codec.INT.optionalFieldOf("max", LifePool.DEFAULT_MAX).forGetter(LifePool::maxHp)
    ).apply(instance, LifePool::of));

    /** 三层进度的编解码（v0.0.2 V9；V10 起含 {@code last_event}，v0.0.2 修正起含 {@code defeated}）。 */
    private static final Codec<ProgressState> PROGRESS_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("floor", 1).forGetter(ProgressState::floor),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("counts", Map.of())
                    .forGetter(PlayerCardData::countKeys),
            Codec.INT.listOf().optionalFieldOf("bosses", List.of())
                    .forGetter(progress -> List.copyOf(progress.bossesDefeated())),
            Codec.BOOL.optionalFieldOf("completed", false).forGetter(ProgressState::completed),
            Codec.STRING.optionalFieldOf("last_event")
                    .forGetter(progress -> Optional.ofNullable(eventName(progress.lastEvent()))),
            Codec.BOOL.optionalFieldOf("defeated", false).forGetter(ProgressState::defeated),
            // v0.4.5：本层已访问过的问号事件 id（相同问号事件每层只能访问一次；进阶自动清空）
            Codec.STRING.listOf().optionalFieldOf("visited", List.of())
                    .forGetter(progress -> List.copyOf(progress.visitedEvents())),
            // v0.4.6.7：本局已经触发过战斗的 BOSS 实体 id（同一只 BOSS 只打一次）
            Codec.STRING.listOf().optionalFieldOf("fought_bosses", List.of())
                    .forGetter(progress -> List.copyOf(progress.bossesFought()))
    ).apply(instance, PlayerCardData::progressFrom));

    public static final Codec<PlayerCardData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CardInstance.CODEC.listOf().fieldOf("cards").forGetter(PlayerCardData::cards),
            Codec.INT.fieldOf("gold").forGetter(PlayerCardData::gold),
            ResourceLocation.CODEC.optionalFieldOf("character").forGetter(data -> Optional.ofNullable(data.character())),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).fieldOf("kill_progress")
                    .forGetter(PlayerCardData::killProgress),
            RewardOffer.CODEC.listOf().optionalFieldOf("pending_offers", List.of())
                    .forGetter(PlayerCardData::pendingOffers),
            Stats.CODEC.fieldOf("stats").forGetter(PlayerCardData::stats),
            Codec.BOOL.optionalFieldOf("initialized", false).forGetter(PlayerCardData::initialized),
            ResourceLocation.CODEC.listOf().optionalFieldOf("started_characters", List.of())
                    .forGetter(data -> List.copyOf(data.startedCharacters())),
            Codec.STRING.listOf().optionalFieldOf("relics", List.of())
                    .forGetter(data -> List.copyOf(data.relics)),
            Codec.STRING.listOf().optionalFieldOf("potions", List.of())
                    .forGetter(data -> List.copyOf(data.potions)),
            // v0.0.2 V9：独立生命值（上限默认 75，与原版血量解耦）
            LIFE_CODEC.optionalFieldOf("life", LifePool.standard()).forGetter(PlayerCardData::life),
            // v0.0.2 V9：三层进度（层数 / 本层事件计数 / 已击败的 BOSS / 是否通关 / 最近一次事件）
            PROGRESS_CODEC.optionalFieldOf("progress", ProgressState.fresh())
                    .forGetter(PlayerCardData::progress),
            // v0.4.6【幸运药水】：下一次卡牌奖励的候选全部是强化版
            Codec.BOOL.optionalFieldOf("lucky_reward", false).forGetter(PlayerCardData::luckyReward),
            // v0.4.6【药水腰带】等遗物带来的额外药水栏位（冗余存一份，方便读档后立刻生效）
            Codec.INT.optionalFieldOf("bonus_potion_slots", 0).forGetter(PlayerCardData::bonusPotionSlots),
            // ---- v0.5.0.20：战斗结算页面的"待领取"状态 ----
            // ⚠️ 注意：RecordCodecBuilder.group 最多支持 **16** 个参数，这里已经到上限，
            // 所以只加两个字段："待领金币"与"待领遗物"。
            // 领取状态**由字段自身表达**：金币领了就归 0、遗物领了就清空 id —— 不需要额外的布尔。
            // 本场战斗待领取的金币（结算页第一项；领了才进金币余额）
            Codec.INT.optionalFieldOf("settle_gold", 0).forGetter(PlayerCardData::pendingGold),
            // 本场掉落的遗物 id（没掉落 / 已领就是空；结算页第二项）
            Codec.STRING.optionalFieldOf("settle_relic").forGetter(PlayerCardData::pendingRelicOptional)
    ).apply(instance, PlayerCardData::new));

    /**
     * 同步到客户端用的流编解码器（Attachment 的 {@code sync(...)} 需要它）。
     * 直接复用 {@link #CODEC} 做 NBT 往返，避免"落盘一套、同步另一套"导致两份序列化逻辑分叉。
     */
    public static final StreamCodec<ByteBuf, PlayerCardData> STREAM_CODEC =
            ByteBufCodecs.COMPOUND_TAG.map(PlayerCardData::fromTag, PlayerCardData::toTag);

    private final List<CardInstance> cards;
    private final Map<ResourceLocation, Integer> killProgress;
    /** 已经"开过局"的角色：切回它们时不再重复发放起始牌组（防止来回切换刷牌）。 */
    private final Set<ResourceLocation> startedCharacters;
    /** 待领取的卡牌奖励（候选已抽好），数量即 HUD 上的角标。 */
    private final List<RewardOffer> pendingOffers;
    /** 已拥有的遗物 id（v0.0.2 V7）。 */
    private final Set<String> relics;
    /** 药水槽位（v0.0.2 V8）：列表顺序就是槽位顺序，最多由配置决定。 */
    private final List<String> potions;
    /** 独立生命值（v0.0.2 V9）：不再跟着原版血条走。 */
    private LifePool life;
    /** 尖塔进度（v0.0.2 V9）：层数 + 本层事件计数 + 已击败 BOSS。 */
    private ProgressState progress;
    private int gold;
    private ResourceLocation character;
    private Stats stats;
    private boolean initialized;
    /** v0.4.6【幸运药水】：下一次卡牌奖励的候选全部是强化版。 */
    private boolean luckyReward;
    /** v0.4.6：遗物带来的额外药水栏位（【药水腰带】+2）。 */
    private int bonusPotionSlots;

    // ---- v0.5.0.20：战斗结算页面的"待领取"状态 ----
    // 领取状态由字段自身表达（金币领了归 0 / 遗物领了清空 id），不另存布尔 ——
    // 因为 RecordCodecBuilder.group 最多 16 个参数，字段数已经到顶。
    /** 本场战斗待领取的金币（结算页第一项）。 */
    private int pendingGold;
    /** 本场掉落的遗物 id（null = 没掉落或已领）。 */
    private String pendingRelic;

    /** 全新玩家：空牌组、0 金币、未选角色、满血第 1 层。 */
    public PlayerCardData() {
        this(List.of(), 0, Optional.empty(), Map.of(), List.of(), Stats.EMPTY, false, List.of(), List.of(),
                List.of(), LifePool.standard(), ProgressState.fresh());
    }

    private PlayerCardData(List<CardInstance> cards, int gold, Optional<ResourceLocation> character,
                           Map<ResourceLocation, Integer> killProgress, List<RewardOffer> pendingOffers, Stats stats,
                           boolean initialized, List<ResourceLocation> startedCharacters, List<String> relics,
                           List<String> potions, LifePool life, ProgressState progress) {
        this(cards, gold, character, killProgress, pendingOffers, stats, initialized, startedCharacters, relics,
                potions, life, progress, false, 0, 0, Optional.empty());
    }

    private PlayerCardData(List<CardInstance> cards, int gold, Optional<ResourceLocation> character,
                           Map<ResourceLocation, Integer> killProgress, List<RewardOffer> pendingOffers, Stats stats,
                           boolean initialized, List<ResourceLocation> startedCharacters, List<String> relics,
                           List<String> potions, LifePool life, ProgressState progress, boolean luckyReward,
                           int bonusPotionSlots, int pendingGold, Optional<String> pendingRelic) {
        this.cards = new ArrayList<>(cards);
        this.gold = Math.max(0, gold);
        this.character = character.orElse(null);
        this.killProgress = new LinkedHashMap<>(killProgress);
        this.pendingOffers = new ArrayList<>(pendingOffers);
        this.stats = stats;
        this.initialized = initialized;
        this.startedCharacters = new LinkedHashSet<>(startedCharacters);
        this.relics = new LinkedHashSet<>(relics);
        this.potions = new ArrayList<>(potions);
        this.life = life == null ? LifePool.standard() : life;
        this.progress = progress == null ? ProgressState.fresh() : progress;
        this.luckyReward = luckyReward;
        this.pendingGold = Math.max(0, pendingGold);
        this.pendingRelic = pendingRelic == null ? null : pendingRelic.orElse(null);
        // 【药水腰带】这类遗物：读档时按遗物重新推导一次，避免老档案缺字段
        this.bonusPotionSlots = Math.max(bonusPotionSlots,
                com.slaythecraft.combat.core.RelicLibrary.extraPotionSlots(this.relics));
    }

    private static String eventName(EventKind kind) {
        return kind == null ? null : kind.name();
    }

    private static EventKind eventKindOf(String name) {
        if (name == null) {
            return null;
        }
        try {
            return EventKind.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            // 存档里有个不认识的类型（改过枚举）：当作"本层还没发生事件"
            return null;
        }
    }

    /** 由 {@link #PROGRESS_CODEC} 组装进度（计数键名不认识就跳过，不让整份档案读不出来）。 */
    private static ProgressState progressFrom(int floor, Map<String, Integer> counts, List<Integer> bosses,
                                             boolean completed, Optional<String> lastEvent, boolean defeated,
                                             List<String> visited, List<String> foughtBosses) {
        return ProgressState.of(floor, eventCountsOf(counts), new LinkedHashSet<>(bosses), completed,
                eventKindOf(lastEvent.orElse(null)), defeated, new LinkedHashSet<>(visited),
                new LinkedHashSet<>(foughtBosses));
    }

    /** 进度计数写成 {@code 枚举名 → 次数}（未知键在读回时忽略，删档也不会报错）。 */
    private static Map<String, Integer> countKeys(ProgressState progress) {
        Map<String, Integer> keys = new LinkedHashMap<>();
        for (EventKind kind : EventKind.values()) {
            int count = progress.countOf(kind);
            if (count > 0) {
                keys.put(kind.name(), count);
            }
        }
        return keys;
    }

    private static Map<EventKind, Integer> eventCountsOf(Map<String, Integer> keys) {
        Map<EventKind, Integer> counts = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : keys.entrySet()) {
            try {
                counts.put(EventKind.valueOf(entry.getKey()), entry.getValue());
            } catch (IllegalArgumentException ignored) {
                // 存档里有个不认识的类型（改过枚举）：跳过这一条，不让整个档案读不出来
            }
        }
        return counts;
    }

    public Set<ResourceLocation> startedCharacters() {
        return Set.copyOf(startedCharacters);
    }

    public boolean hasStartedCharacter(ResourceLocation characterId) {
        return startedCharacters.contains(characterId);
    }

    public void markCharacterStarted(ResourceLocation characterId) {
        startedCharacters.add(characterId);
    }

    // ------------------------------------------------------------------ 遗物（V7）

    /** 已拥有的遗物 id（只读）。 */
    public Set<String> relics() {
        return Set.copyOf(relics);
    }

    public int relicCount() {
        return relics.size();
    }

    public boolean hasRelic(String relicId) {
        return relics.contains(relicId);
    }

    /** 获得一件遗物；已拥有则返回 false。 */
    public boolean addRelic(String relicId) {
        return relics.add(relicId);
    }

    /**
     * **正式获得**一件遗物（v0.4.6）：除了写进档案，还会应用"获得时立刻生效"的效果。
     *
     * <ul>
     *   <li>【嚒咕菲力】：最大生命值 +14（当前生命同 +14，不会因为加上限而"看起来掉血"）</li>
     *   <li>【药水腰带】：药水栏位 +2（记录在档案里，界面与发药水都读它）</li>
     * </ul>
     *
     * <p>读档时走的是构造函数（按遗物重新推导），**不要**在加载路径调用本方法，否则会重复叠加。</p>
     *
     * @return 获得效果的说明（没有额外效果时返回空串）
     */
    public String grantRelic(String relicId) {
        if (!addRelic(relicId)) {
            return "";
        }
        StringBuilder extra = new StringBuilder();
        int maxHp = com.slaythecraft.combat.core.RelicLibrary.bonusMaxHp(relicId);
        if (maxHp > 0) {
            this.life = this.life.withMax(this.life.maxHp() + maxHp);
            this.life = this.life.heal(maxHp);
            extra.append("最大生命值 +").append(maxHp).append("（当前 ").append(this.life.describe()).append("）");
        }
        int slots = com.slaythecraft.combat.core.RelicLibrary.extraPotionSlots(java.util.List.of(relicId));
        if (slots > 0) {
            bonusPotionSlots += slots;
            if (!extra.isEmpty()) {
                extra.append("，");
            }
            extra.append("药水栏位 +").append(slots);
        }
        return extra.toString();
    }

    // ------------------------------------------------------------------ v0.4.6：药水栏位与幸运

    /** 遗物带来的额外药水栏位（【药水腰带】= 2）。 */
    public int bonusPotionSlots() {
        return bonusPotionSlots;
    }

    /** 实际可用的药水栏位数（配置基础值 + 遗物加成）。 */
    public int potionSlots(int baseSlots) {
        return Math.max(1, baseSlots) + Math.max(0, bonusPotionSlots);
    }

    /** 下一次卡牌奖励的候选是否全部强化（【幸运药水】）。 */
    public boolean luckyReward() {
        return luckyReward;
    }

    public void setLuckyReward(boolean value) {
        this.luckyReward = value;
    }

    /** 用掉一次幸运（生成奖励候选时调用）。 */
    public boolean consumeLuckyReward() {
        if (!luckyReward) {
            return false;
        }
        luckyReward = false;
        return true;
    }

    /** 清空遗物（调试指令用）。 */
    public void clearRelics() {
        relics.clear();
    }

    // ------------------------------------------------------------------ 药水（V8）

    /** 药水槽位内容（槽位顺序）。 */
    public List<String> potions() {
        return List.copyOf(potions);
    }

    public int potionCount() {
        return potions.size();
    }

    /** 指定槽位的药水 id；空槽或越界返回 null。 */
    public String potionAt(int slot) {
        return slot >= 0 && slot < potions.size() ? potions.get(slot) : null;
    }

    /** 放入一瓶药水；槽位已满返回 false（上限由调用方按配置传入）。 */
    public boolean addPotion(String potionId, int maxSlots) {
        if (potions.size() >= Math.max(1, maxSlots)) {
            return false;
        }
        potions.add(potionId);
        return true;
    }

    /** 取走指定槽位的药水（使用后消耗）。 */
    public String takePotionAt(int slot) {
        return slot >= 0 && slot < potions.size() ? potions.remove(slot) : null;
    }

    public void clearPotions() {
        potions.clear();
    }

    // ------------------------------------------------------------------ 独立生命值（V9）

    /** 独立生命池（不可变值对象，改完要 {@link #setLife} 写回）。 */
    public LifePool life() {
        return life;
    }

    public void setLife(LifePool pool) {
        this.life = pool == null ? LifePool.standard() : pool;
    }

    public int hp() {
        return life.hp();
    }

    public int maxHp() {
        return life.maxHp();
    }

    /** 战斗中掉血（引擎在战斗结束时把最终血量写回档案，这里给事件/调试用）。 */
    public void damageHp(int amount) {
        life = life.damage(amount);
    }

    public void healHp(int amount) {
        life = life.heal(amount);
    }

    // ------------------------------------------------------------------ 尖塔进度（V9）

    public ProgressState progress() {
        return progress;
    }

    public void setProgress(ProgressState state) {
        this.progress = state == null ? ProgressState.fresh() : state;
    }

    public int floor() {
        return progress.floor();
    }

    // ------------------------------------------------------------------ 问号事件的本层记忆（v0.4.5）

    /** 本层的这个问号事件是否已经访问过（需求：相同问号事件每层只能访问一次）。 */
    public boolean hasVisitedQuestion(String eventId) {
        return progress.hasVisitedEvent(eventId);
    }

    /** 记下"本层已经访问过这个问号事件"（进阶时自动清空）。 */
    public void markQuestionVisited(String eventId) {
        progress = progress.withVisitedEvent(eventId);
    }

    /** 这一类事件在本层还能不能触发（上限由配置给出）。 */
    public boolean canTrigger(EventKind kind, EventCaps caps) {
        return progress.canTrigger(kind, caps);
    }

    /** 记一次事件；返回是否真的记上了（达上限时为 false）。 */
    public boolean recordEvent(EventKind kind, EventCaps caps) {
        if (!progress.canTrigger(kind, caps)) {
            return false;
        }
        progress = progress.record(kind, caps);
        return true;
    }

    /** 击败当前层 BOSS：进阶 / 通关。返回新进度便于调用方播报。 */
    public ProgressState recordBossDefeat() {
        progress = progress.recordBossDefeat();
        return progress;
    }

    /** 记下"本局战败"（测试反馈：通关或战败之前不允许换角色）。 */
    public void markRunDefeated() {
        progress = progress.withDefeated();
    }

    /** 领奖后卡池/遗物可能变多，这里重新推导一次药水栏位（防止落盘字段与实际不一致）。 */
    public void refreshPotionSlotsFromRelics() {
        this.bonusPotionSlots = Math.max(this.bonusPotionSlots,
                com.slaythecraft.combat.core.RelicLibrary.extraPotionSlots(this.relics));
    }

    /** 本局是否已结束（通关或战败）。 */
    public boolean isRunOver() {
        return progress.isRunOver();
    }

    // ------------------------------------------------------------------ v0.5.0.20 战斗结算页面

    /** 本场战斗待领取的金币。 */
    public int pendingGold() {
        return pendingGold;
    }

    /** 本场掉落的遗物 id（{@code null} = 没掉落）。 */
    public String pendingRelic() {
        return pendingRelic;
    }

    /** 供 codec 用的 Optional 形式（{@code null} → 空）。 */
    public Optional<String> pendingRelicOptional() {
        return Optional.ofNullable(pendingRelic);
    }

    /** 金币是不是已经领过（金币待领额归 0 即视为已领，不另存布尔）。 */
    public boolean settleGoldClaimed() {
        return pendingGold <= 0;
    }

    /** 遗物是不是已经领过（待领 id 清空即视为已领）。 */
    public boolean settleRelicClaimed() {
        return pendingRelic == null;
    }

    /** 战斗结算页面上还有没有没领完的东西。 */
    public boolean settlementOpen() {
        return pendingGold > 0 || pendingRelic != null || pendingRewards() > 0;
    }

    /**
     * 开一次战斗结算：把这一场赚到的金币先**暂存**起来（不是立刻进余额），
     * 并记下可能掉落的遗物。金币要等玩家在结算页点「领取」才真正进账。
     *
     * @param gold  本场金币
     * @param relic 本场掉落的遗物 id（可空）
     */
    public void beginSettlement(int gold, String relic) {
        this.pendingGold = Math.max(0, gold);
        this.pendingRelic = relic;
    }

    /** 领取结算页的金币。 */
    public boolean claimSettlementGold() {
        if (pendingGold <= 0) {
            return false;
        }
        this.gold += pendingGold;
        this.pendingGold = 0;
        return true;
    }

    /** 领取结算页的遗物。 */
    public boolean claimSettlementRelic() {
        if (pendingRelic == null) {
            return false;
        }
        this.relics.add(pendingRelic);
        this.pendingRelic = null;
        refreshPotionSlotsFromRelics();
        return true;
    }

    /** 清空结算状态（跳过奖励 / 领完之后调用）。 */
    public void clearSettlement() {
        this.pendingGold = 0;
        this.pendingRelic = null;
    }

    /** 清空全部卡牌（换角色清档用；不返还金币）。 */
    public void clearCards() {
        cards.clear();
    }

    /** 是否已经做过一次性初始化（初始金币只发一次，见 {@code PlayerDataEvents}）。 */
    public boolean initialized() {
        return initialized;
    }

    public void markInitialized() {
        initialized = true;
    }

    // ------------------------------------------------------------------ cards

    /** 只读快照，顺序与获得顺序一致。 */
    public List<CardInstance> cards() {
        return List.copyOf(cards);
    }

    public int cardCount() {
        return cards.size();
    }

    public void addCard(CardInstance card) {
        cards.add(card);
    }

    /**
     * 按 id 删除一张卡（只删一张，允许重复卡）。
     *
     * @return 是否真的删掉了
     */
    public boolean removeFirstCard(ResourceLocation id) {
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).id().equals(id)) {
                cards.remove(i);
                stats = stats.withCardsDeleted(stats.cardsDeleted() + 1);
                return true;
            }
        }
        return false;
    }

    /** 按索引删除（UI 删除卡牌走这条；服务端会再次校验索引）。 */
    public Optional<CardInstance> removeCardAt(int index) {
        if (index < 0 || index >= cards.size()) {
            return Optional.empty();
        }
        CardInstance removed = cards.remove(index);
        stats = stats.withCardsDeleted(stats.cardsDeleted() + 1);
        return Optional.of(removed);
    }

    /**
     * 强化指定下标的卡（v0.0.3 W5：篝火"锻造"选项）。
     *
     * <p>v0.5.4.1（A4-20）：能不能再强化改问 {@code CardLibrary.maxUpgrades} ——
     * 【灼热打击】是"可以强化任意次"，以前写死的 {@code upgrades > 0} 会让它只能升一次。</p>
     *
     * @return 是否真的强化了（越界 / 没有强化版 / 已到上限都返回 false）
     */
    public boolean upgradeCardAt(int index) {
        if (index < 0 || index >= cards.size()) {
            return false;
        }
        CardInstance instance = cards.get(index);
        if (instance.upgrades() >= maxUpgradesOf(instance)) {
            return false;
        }
        cards.set(index, instance.withUpgrade());
        return true;
    }

    /** 这张卡还能不能被强化（界面用）。 */
    public boolean canUpgradeAt(int index) {
        if (index < 0 || index >= cards.size()) {
            return false;
        }
        CardInstance instance = cards.get(index);
        return instance.upgrades() < maxUpgradesOf(instance);
    }

    /** 这张卡在档案里最多能强化几次（【灼热打击】没有上限）。 */
    private static int maxUpgradesOf(CardInstance instance) {
        return com.slaythecraft.combat.core.CardLibrary.maxUpgrades(instance.id().getPath());
    }

    public int indexOfCard(ResourceLocation id) {
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 给第一张匹配的卡附魔（v0.0.2 V6；图书管理员事件的底层操作）。
     *
     * @return 是否找到并附魔成功
     */
    public boolean enchantFirstCard(ResourceLocation id, com.slaythecraft.combat.core.CardEnchantment enchantment) {
        int index = indexOfCard(id);
        if (index < 0) {
            return false;
        }
        CardInstance instance = cards.get(index);
        cards.set(index, instance.withEnchantment(enchantment));
        return true;
    }

    /**
     * 给**前 N 张符合条件**的卡附魔（v0.5.3.0【破旧打火石】用）。
     *
     * @param enchantment 要附的魔
     * @param amount      最多附几张
     * @param filter      只考虑满足条件的卡（例如"攻击牌"）
     * @return 实际附魔的张数
     */
    public int enchantFirstMatching(com.slaythecraft.combat.core.CardEnchantment enchantment, int amount,
                                    java.util.function.Predicate<CardInstance> filter) {
        int done = 0;
        for (int i = 0; i < cards.size() && done < amount; i++) {
            CardInstance instance = cards.get(i);
            if (!filter.test(instance) || instance.enchantments().contains(enchantment)) {
                continue;
            }
            cards.set(i, instance.withEnchantment(enchantment));
            done++;
        }
        return done;
    }

    /**
     * 强化第一张匹配的卡（v0.0.2 V11）。
     *
     * <p>只强化**还没到强化上限**、并且确实有强化版的那一张。
     * v0.5.4.1（A4-20）起上限按卡问 {@code CardLibrary.maxUpgrades}：
     * 普通卡还是 1 次，【灼热打击】可以一直升。</p>
     *
     * @return 是否真的强化了
     */
    public boolean upgradeFirstCard(ResourceLocation id) {
        for (int i = 0; i < cards.size(); i++) {
            CardInstance instance = cards.get(i);
            if (!instance.id().equals(id) || instance.upgrades() >= maxUpgradesOf(instance)) {
                continue;
            }
            cards.set(i, instance.withUpgrade());
            return true;
        }
        return false;
    }

    /** 强化**所有**匹配的卡（各 +1），返回被强化的张数（武器匠事件用）。 */
    public int upgradeAllCards(ResourceLocation id) {
        int upgraded = 0;
        for (int i = 0; i < cards.size(); i++) {
            CardInstance instance = cards.get(i);
            if (!instance.id().equals(id) || instance.upgrades() >= maxUpgradesOf(instance)) {
                continue;
            }
            cards.set(i, instance.withUpgrade());
            upgraded++;
        }
        return upgraded;
    }

    // ------------------------------------------------------------------ gold

    public int gold() {
        return gold;
    }

    public void addGold(int amount) {
        if (amount > 0) {
            gold += amount;
        }
    }

    public void setGold(int amount) {
        gold = Math.max(0, amount);
    }

    /** 扣钱；余额不足时**不扣**并返回 false（所有消费都必须先过这里）。 */
    public boolean spendGold(int amount) {
        if (amount < 0 || gold < amount) {
            return false;
        }
        gold -= amount;
        return true;
    }

    // ------------------------------------------------------------------ character

    public ResourceLocation character() {
        return character;
    }

    public void setCharacter(ResourceLocation character) {
        this.character = character;
    }

    // ------------------------------------------------------------------ kill progress & rewards

    public Map<ResourceLocation, Integer> killProgress() {
        return Map.copyOf(killProgress);
    }

    public int killCount(ResourceLocation entityType) {
        return killProgress.getOrDefault(entityType, 0);
    }

    /** 记一次击杀，返回累计数（是否达标由奖励服务判断，见 S4）。 */
    public int recordKill(ResourceLocation entityType) {
        int current = killProgress.merge(entityType, 1, Integer::sum);
        stats = stats.withKills(stats.kills() + 1);
        return current;
    }

    public void clearKillProgress(ResourceLocation entityType) {
        killProgress.remove(entityType);
    }

    /** 清空全部击杀计数（重开一局用）。 */
    public void clearKillCounters() {
        killProgress.clear();
    }

    /** 待领奖励数量（= HUD 角标）。 */
    public int pendingRewards() {
        return pendingOffers.size();
    }

    public List<RewardOffer> pendingOffers() {
        return List.copyOf(pendingOffers);
    }

    /** 队首奖励（还没抽候选时返回未抽的占位）。 */
    public RewardOffer firstOffer() {
        return pendingOffers.isEmpty() ? null : pendingOffers.get(0);
    }

    /** 加若干个"还没抽候选"的奖励（S1 计数语义 / 调试用）。 */
    public void addPendingRewards(int amount) {
        for (int i = 0; i < amount; i++) {
            pendingOffers.add(RewardOffer.unrolled(false));
        }
    }

    /** 加一个已经抽好候选的奖励。 */
    public void addOffer(RewardOffer offer) {
        pendingOffers.add(offer);
    }

    /** 把第 index 个奖励的候选写回去（击杀达标时第一次抽取 / 补抽）。 */
    public boolean setOffer(int index, RewardOffer offer) {
        if (index < 0 || index >= pendingOffers.size()) {
            return false;
        }
        pendingOffers.set(index, offer);
        return true;
    }

    /** 领取（移除）第 index 个奖励；索引越界返回 false。 */
    public boolean claimOffer(int index) {
        if (index < 0 || index >= pendingOffers.size()) {
            return false;
        }
        pendingOffers.remove(index);
        return true;
    }

    /** 领取一次奖励（队首）；没有待领奖励时返回 false。 */
    public boolean consumePendingReward() {
        return claimOffer(0);
    }

    /**
     * 清空全部未领奖励（v0.0.2 V9：**下一场战斗开始时**清空）。
     *
     * @return 被清掉的奖励个数
     */
    public int clearPendingOffers() {
        int removed = pendingOffers.size();
        pendingOffers.clear();
        return removed;
    }

    // ------------------------------------------------------------------ stats

    public Stats stats() {
        return stats;
    }

    public void recordBattleFinished(boolean won) {
        stats = won ? stats.withBattlesWon(stats.battlesWon() + 1) : stats.withBattlesLost(stats.battlesLost() + 1);
    }

    // ------------------------------------------------------------------ serialization helpers

    /** 供 Attachment 同步用的 NBT 往返（复用上面的 Codec，避免两套序列化逻辑分叉）。 */
    public CompoundTag toTag() {
        return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow();
    }

    public static PlayerCardData fromTag(CompoundTag tag) {
        PlayerCardData data = CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
        // S1~S3 存的是 pending_rewards 计数，没有候选列表：按数量补成"未抽候选"的奖励
        int legacyCount = tag.getInt("pending_rewards");
        while (data.pendingRewards() < legacyCount) {
            data.addPendingRewards(1);
        }
        // V9 开发版把生命值与进度平铺在顶层（life_hp / floor / event_counts …）：
        // 这里读一次老键，避免那一版玩家的存档被当成"满血第 1 层"
        if (!tag.contains("life") && (tag.contains("life_hp") || tag.contains("life_max"))) {
            data.setLife(LifePool.of(tag.contains("life_hp") ? tag.getInt("life_hp") : LifePool.DEFAULT_MAX,
                    tag.contains("life_max") ? tag.getInt("life_max") : LifePool.DEFAULT_MAX));
        }
        if (!tag.contains("progress") && tag.contains("floor")) {
            Map<String, Integer> legacyCounts = new LinkedHashMap<>();
            CompoundTag counts = tag.getCompound("event_counts");
            for (String key : counts.getAllKeys()) {
                legacyCounts.put(key, counts.getInt(key));
            }
            List<Integer> legacyBosses = new ArrayList<>();
            for (int boss : tag.getIntArray("bosses_defeated")) {
                legacyBosses.add(boss);
            }
            data.setProgress(ProgressState.of(tag.getInt("floor"), eventCountsOf(legacyCounts),
                    new LinkedHashSet<>(legacyBosses), tag.getBoolean("completed"),
                    eventKindOf(tag.contains("last_event") ? tag.getString("last_event") : null)));
        }
        return data;
    }

    /** 只读统计。 */
    public record Stats(int kills, int battlesWon, int battlesLost, int cardsDeleted) {
        public static final Stats EMPTY = new Stats(0, 0, 0, 0);

        public static final Codec<Stats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("kills", 0).forGetter(Stats::kills),
                Codec.INT.optionalFieldOf("battles_won", 0).forGetter(Stats::battlesWon),
                Codec.INT.optionalFieldOf("battles_lost", 0).forGetter(Stats::battlesLost),
                Codec.INT.optionalFieldOf("cards_deleted", 0).forGetter(Stats::cardsDeleted)
        ).apply(instance, Stats::new));

        Stats withKills(int value) {
            return new Stats(value, battlesWon, battlesLost, cardsDeleted);
        }

        Stats withBattlesWon(int value) {
            return new Stats(kills, value, battlesLost, cardsDeleted);
        }

        Stats withBattlesLost(int value) {
            return new Stats(kills, battlesWon, value, cardsDeleted);
        }

        Stats withCardsDeleted(int value) {
            return new Stats(kills, battlesWon, battlesLost, value);
        }
    }
}
