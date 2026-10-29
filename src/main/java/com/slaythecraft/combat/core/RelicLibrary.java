package com.slaythecraft.combat.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 遗物表（v0.0.2 V7）。
 *
 * <p>目前是**代码注册表**（与卡池一致）；内容变多或需要整合包自定义时，可以整体搬到数据包注册表，
 * 字段与卡牌一样是一一对应的。</p>
 *
 * <p>首批 5 件覆盖了主要的触发点形态：战斗开始给状态/格挡/抽牌、回合开始给状态、
 * 战斗胜利回复生命 —— 足以验证触发点机制，也够玩。</p>
 */
public final class RelicLibrary {

    private static final Map<String, Relic> BY_ID = new LinkedHashMap<>();

    /** 金刚杵：战斗开始 +1 力量。 */
    public static final Relic VAJRA = register(new Relic(
            "vajra", "金刚杵", "战斗开始时获得 1 点力量。",
            Set.of(RelicTrigger.BATTLE_START),
            (ctx, owner) -> {
                owner.addStrength(1);
                ctx.announce("  力量 +1（当前 +" + owner.strength() + "）");
            }));

    /** 锚：战斗开始 +10 格挡。 */
    public static final Relic ANCHOR = register(new Relic(
            "anchor", "锚", "战斗开始时获得 10 点格挡。",
            Set.of(RelicTrigger.BATTLE_START),
            (ctx, owner) -> {
                ctx.gainBlockFor(owner, 10);
                ctx.announce("  获得 10 点格挡（当前 " + owner.block() + "）");
            }));

    /** 准备背包：战斗开始额外抽 2 张牌。 */
    public static final Relic BAG_OF_PREPARATION = register(new Relic(
            "bag_of_preparation", "准备背包", "战斗开始时额外抽 2 张牌。",
            Set.of(RelicTrigger.BATTLE_START),
            // v0.2.8：改成"给持有者抽"（多人时以前会抽到当前行动玩家手里）
            (ctx, owner) -> ctx.drawCardsFor(owner, 2)));

    /** 光滑的石头：每回合开始 +1 迅捷。 */
    public static final Relic ODDLY_SMOOTH_STONE = register(new Relic(
            "oddly_smooth_stone", "光滑的石头", "每回合开始时获得 1 点迅捷。",
            Set.of(RelicTrigger.TURN_START),
            (ctx, owner) -> owner.addDexterity(1)));

    /** 燃烧之血：战斗胜利后回复 6 点生命。 */
    public static final Relic BURNING_BLOOD = register(new Relic(
            "burning_blood", "燃烧之血", "战斗胜利后回复 6 点生命。",
            Set.of(RelicTrigger.ON_VICTORY),
            (ctx, owner) -> {
                owner.heal(6);
                ctx.announce("  回复 6 点生命（当前 HP " + owner.hp() + "/" + owner.maxHp() + "）");
            }));

    /**
     * 受惊的小猪仔（v0.0.2 V11；v0.2.8 换成需求指定的效果）。
     *
     * <p>效果：**每场战斗第一次失去生命值时，抽 3 张牌**；
     * 描述分为两行（先写效果，再写风味文案），详情页会按宽度折行显示。</p>
     */
    public static final Relic STARTLED_PIGLET = register(new Relic(
            "startled_piglet", "受惊的小猪仔",
            "每场战斗第一次失去生命值时，抽 3 张牌。\n至少你救下了一条生命，不是吗？",
            Set.of(RelicTrigger.FIRST_HP_LOST),
            (ctx, owner) -> {
                ctx.drawCardsFor(owner, 3);
                ctx.announce("  小猪仔钻到你怀里：你顺手摸出 3 张牌");
            }));

    /**
     * 蛇之戒指（v0.0.3 W3）：静默猎手的初始遗物 —— 战斗开始时额外抽 2 张牌。
     *
     * <p>原版《杀戮尖塔》的静默猎手初始遗物就是它（Ring of the Snake：开局多抽 2 张）。</p>
     */
    public static final Relic RING_OF_THE_SNAKE = register(new Relic(
            "ring_of_the_snake", "蛇之戒指", "战斗开始时额外抽 2 张牌。",
            Set.of(RelicTrigger.BATTLE_START),
            (ctx, owner) -> ctx.drawCardsFor(owner, 2)));

    /**
     * 破损核心（v0.0.3 W4）：故障机器人的初始遗物 —— 战斗开始时生成 1 颗闪电球。
     */
    public static final Relic CRACKED_CORE = register(new Relic(
            "cracked_core", "破损核心", "战斗开始时生成 1 颗闪电球。",
            Set.of(RelicTrigger.BATTLE_START),
            (ctx, owner) -> ctx.channelOrb(owner, Orb.LIGHTNING)));

    /**
     * 臂弩（v0.0.3 W3）：叛逃掠夺者的初始遗物 —— 战斗开始时把一张【箭在弦上】加入手牌。
     *
     * <p>【箭在弦上】是 0 费无色状态牌（获得活力 4），任何地方都买不到 —— 只有这件遗物会发。</p>
     */
    public static final Relic CROSSBOW = register(new Relic(
            "crossbow", "臂弩", "战斗开始时，把一张【箭在弦上】加入你的手牌。\n（箭在弦上：0 费，获得活力 4）",
            Set.of(RelicTrigger.BATTLE_START),
            (ctx, owner) -> ctx.addCardsToHand(owner, CardLibrary.ARROW_NOCKED, 1)));

    /**
     * 袭击号角（v0.4.1）：对【灾厄部队】的敌人额外造成 10% 伤害。
     *
     * <p>来源：问号事件"山羊角会再生吗？"（砍下山羊的一只角）。</p>
     */
    public static final Relic RAID_HORN = register(new Relic(
            "raid_horn", "袭击号角", "对【灾厄部队】的敌人额外造成 10% 伤害。",
            Set.of(RelicTrigger.BATTLE_START),
            (ctx, owner) -> {
                owner.setRaidHorn(true);
                ctx.announce("  袭击号角：对【灾厄部队】+10% 伤害");
            }));

    /**
     * 失活的珊瑚（v0.4.1）：每次受到的伤害 -1。
     *
     * <p>来源：问号事件"奇怪的商人"（花 200 金币买下来）。</p>
     */
    public static final Relic DEAD_CORAL = register(new Relic(
            "dead_coral", "失活的珊瑚", "每次受到伤害时，其数值减少 1。",
            Set.of(RelicTrigger.BATTLE_START),
            (ctx, owner) -> {
                owner.setDeadCoral(true);
                ctx.announce("  失活的珊瑚：受到的伤害 -1");
            }));

    /**
     * 海洋之心（v0.4.1）：雨天 / 雷暴天受到的伤害 -10%。
     *
     * <p>来源：问号事件"奇怪的商人"（花 150 金币买下来）。</p>
     */
    public static final Relic HEART_OF_THE_SEA = register(new Relic(
            "heart_of_the_sea", "海洋之心", "如果当前天气是雨天 / 雷暴天，你受到的伤害降低 10%。",
            Set.of(RelicTrigger.BATTLE_START),
            (ctx, owner) -> {
                owner.setHeartOfTheSea(true);
                ctx.announce(owner.stormy()
                        ? "  海洋之心：正在下雨，受到的伤害 -10%"
                        : "  海洋之心：现在没下雨，先收着");
            }));

    // ------------------------------------------------------------------ v0.4.6：四件新遗物

    /**
     * 药水腰带（v0.4.6）：额外 2 个药水栏位（1×3 → 1×5）。
     *
     * <p>它不改战斗数值，只改"能带几瓶药水"，所以效果走 {@link #extraPotionSlots}，
     * 由界面与发药水的地方一起读。</p>
     */
    public static final Relic POTION_BELT = register(new Relic(
            "potion_belt", "药水腰带", "获得额外的 2 个药水栏位（1×3 扩展为 1×5）。",
            Set.of(RelicTrigger.BATTLE_START),
            (ctx, owner) -> ctx.announce("  药水腰带：药水栏位 +2")));

    /** 小血瓶（v0.4.6）：每场战斗开始时恢复 3 点生命。 */
    public static final Relic SMALL_VIAL = register(new Relic(
            "small_vial", "小血瓶", "每场战斗开始时恢复 3 点生命。",
            Set.of(RelicTrigger.BATTLE_START),
            (ctx, owner) -> {
                owner.heal(3);
                ctx.announce("  回复 3 点生命（当前 HP " + owner.hp() + "/" + owner.maxHp() + "）");
            }));

    /**
     * 嚒咕菲力（v0.4.6）：获得 14 点最大生命值，每回合少抽 1 张牌。
     *
     * <p>最大生命值是**档案层**的事（{@code LifePool}），所以发遗物时就会 +14；
     * 引擎这里负责"每回合少抽 1 张"。</p>
     */
    public static final Relic MOGU_FILLET = register(new Relic(
            "mogu_fillet", "嚒咕菲力", "获得 14 点最大生命值，每回合少抽 1 张牌。",
            Set.of(RelicTrigger.BATTLE_START),
            (ctx, owner) -> {
                owner.setDrawPenalty(1);
                ctx.announce("  嚒咕菲力：每回合少抽 1 张牌");
            }));

    /** 蜂蜜佐料（v0.4.6）：每场战斗结束时，获得 1 最大生命值。 */
    public static final Relic HONEY_SEASONING = register(new Relic(
            "honey_seasoning", "蜂蜜佐料", "每场战斗结束时，获得 1 最大生命值。",
            Set.of(RelicTrigger.ON_VICTORY),
            (ctx, owner) -> ctx.announce("  蜂蜜佐料：战斗结束 +1 最大生命值（战斗外结算）")));

    // ================================================================== v0.5.3.0 八件新遗物

    /**
     * 脏玻璃（v0.5.3.0）：每场战斗开始时获得 3 层【荆棘】。
     *
     * <p>图标用"遮光玻璃"。来源：商店 / 藏宝箱。</p>
     */
    public static final Relic DIRTY_GLASS = register(new Relic(
            "dirty_glass", "脏玻璃", "每场战斗开始时，获得 3 层【荆棘】。",
            Set.of(RelicTrigger.BATTLE_START),
            (ctx, owner) -> {
                owner.addThorns(3);
                ctx.announce("  脏玻璃：获得荆棘 3（当前 " + owner.thorns() + "）");
            }));

    /**
     * 破旧打火石（v0.5.3.0）：**获得时**为两张攻击牌附魔【火焰附加】。
     *
     * <p>它没有战斗内触发点（`triggers` 为空）—— 效果在获得的瞬间由
     * {@code PlayerCardData.grantRelic} 结算。</p>
     */
    public static final Relic WORN_FLINT = register(new Relic(
            "worn_flint", "破旧打火石", "获得时：为你的两张攻击牌附魔【火焰附加】。",
            Set.of(),
            (ctx, owner) -> ctx.announce("  破旧打火石：效果在获得时结算（战斗内没有触发点）")));

    /**
     * 行商羊驼披肩（v0.5.3.0）：商店价格降低四分之一。
     *
     * <p>没有战斗内触发点：效果由商店的价格计算读取（{@code CardShopOffers}）。来源：**只能商店**。</p>
     */
    public static final Relic TRADER_LLAMA_SHAWL = register(new Relic(
            "trader_llama_shawl", "行商羊驼披肩", "商店的价格降低四分之一。",
            Set.of(),
            (ctx, owner) -> ctx.announce("  行商羊驼披肩：商店价格 -25%（在商店里生效）")));

    /**
     * 制图台（v0.5.3.0）：**获得时**立刻获得 5 次卡牌奖励。
     *
     * <p>来源：**只能商店** —— 所以效果在商店购买成功处结算（那里有 ServerPlayer，能直接发奖励）。</p>
     */
    public static final Relic CARTOGRAPHY_TABLE = register(new Relic(
            "cartography_table", "制图台", "获得时：立刻获得 5 次卡牌奖励。",
            Set.of(),
            (ctx, owner) -> ctx.announce("  制图台：效果在获得时结算（战斗内没有触发点）")));

    /**
     * 不太寻常的潜影箱（v0.5.3.0）：每场战斗**第一回合开始时**获得 1 层【缓降】。
     *
     * <p>用户原话给的答案（原本只写了图标与来源）。缓降不随回合衰减，所以战斗开始时给就等于
     * "第一回合开始时有 1 层"，直到挨一次打才消耗掉。</p>
     */
    public static final Relic UNUSUAL_SHULKER_BOX = register(new Relic(
            "unusual_shulker_box", "不太寻常的潜影箱", "每场战斗第一回合开始时，获得 1 层【缓降】。",
            Set.of(RelicTrigger.BATTLE_START),
            (ctx, owner) -> {
                owner.addSlowFall(1);
                ctx.announce("  不太寻常的潜影箱：获得缓降 1（免疫下一次伤害）");
            }));

    /**
     * 龙蛋（v0.5.3.0）：你**每回合**受到的伤害不会大于 20。
     *
     * <p>实现：给参战者挂一个"每敌人回合 20 点"的额度（{@code Combatant.damageCap}），
     * 敌人回合开始时清零。来源：**只能商店**。</p>
     */
    public static final Relic DRAGON_EGG = register(new Relic(
            "dragon_egg", "龙蛋", "你每回合受到的伤害不会大于 20。",
            Set.of(RelicTrigger.BATTLE_START),
            (ctx, owner) -> {
                owner.setDamageCap(20);
                ctx.announce("  龙蛋：每个敌人回合最多受到 20 点伤害");
            }));

    /**
     * 灵魂篝火（v0.5.3.0）：你可以在篝火处选择**任意数量**的选项（原来是三选一）。
     *
     * <p>没有战斗内触发点：效果由 {@code CampfireService} 读遗物 id 判断。来源：商店 / 藏宝箱。</p>
     */
    public static final Relic SOUL_CAMPFIRE = register(new Relic(
            "soul_campfire", "灵魂篝火", "你可以在篝火处选择任意数量的选项。",
            Set.of(),
            (ctx, owner) -> ctx.announce("  灵魂篝火：篝火可以多选（在篝火界面生效）")));

    /**
     * 荧光墨囊（v0.5.3.0）：回合结束时剩余费用**不重置**，可与下一回合累加，上限为费用的 2 倍。
     *
     * <p>实现：给参战者挂 {@code energyCarryover} 旗标，回合开始处按上限夹。</p>
     */
    public static final Relic GLOW_INK_SAC = register(new Relic(
            "glow_ink_sac", "荧光墨囊", "回合结束时，你的剩余费用不会被重置，可以和下一回合的费用累加，"
                    + "累加的最大上限为总费用上限的二倍。",
            Set.of(RelicTrigger.BATTLE_START),
            (ctx, owner) -> {
                owner.setEnergyCarryover(true);
                ctx.announce("  荧光墨囊：剩余费用会留到下一回合（上限 2 倍）");
            }));

    /**
     * 把"被动加成类"遗物的效果同步到参战者身上（v0.4.1）。
     *
     * <p>战斗开始时由 {@code CombatContext} 统一调用，所以离线自检也能直接用遗物 id 验证，
     * 不需要 Minecraft。加新的被动遗物只要在这里加一行。</p>
     *
     * @param relicIds 这名参战者携带的遗物 id
     */
    public static void applyPassiveBonuses(Combatant owner, Collection<String> relicIds) {
        if (owner == null || relicIds == null) {
            return;
        }
        for (String id : relicIds) {
            if (RAID_HORN.id().equals(id)) {
                owner.setRaidHorn(true);
            } else if (DEAD_CORAL.id().equals(id)) {
                owner.setDeadCoral(true);
            } else if (HEART_OF_THE_SEA.id().equals(id)) {
                owner.setHeartOfTheSea(true);
            } else if (MOGU_FILLET.id().equals(id)) {
                owner.setDrawPenalty(1);
            }
        }
    }

    /**
     * 这件遗物带来的**额外药水栏位**（v0.4.6：【药水腰带】+2）。
     *
     * @param relicIds 玩家拥有的遗物 id
     */
    public static int extraPotionSlots(java.util.Collection<String> relicIds) {
        return relicIds != null && relicIds.contains(POTION_BELT.id()) ? 2 : 0;
    }

    /**
     * 这件遗物带来的**额外最大生命值**（v0.4.6：【嚒咕菲力】+14）。
     *
     * <p>只在"获得遗物的那一刻"加一次，所以不要再在别处重复叠加 —— 这里只给指令/自检查询。</p>
     */
    public static int bonusMaxHp(String relicId) {
        return MOGU_FILLET.id().equals(relicId) ? 14 : 0;
    }

    /** 【蜂蜜佐料】：每场战斗结束时 +1 最大生命值。 */
    public static boolean grantsMaxHpPerBattle(String relicId) {
        return HONEY_SEASONING.id().equals(relicId);
    }

    /** 这件遗物是不是"被动加成类"（不进触发点广播，只在开战时点亮）。 */
    public static boolean isPassiveBonus(String relicId) {
        return RAID_HORN.id().equals(relicId) || DEAD_CORAL.id().equals(relicId)
                || HEART_OF_THE_SEA.id().equals(relicId);
    }

    private RelicLibrary() {
    }

    private static Relic register(Relic relic) {
        BY_ID.put(relic.id(), relic);
        return relic;
    }

    public static Relic byId(String id) {
        return BY_ID.get(id);
    }

    public static List<Relic> all() {
        return List.copyOf(BY_ID.values());
    }

    /** 把存档里的 id 列表翻译成遗物（不认识的 id 跳过，删档不会因此报错）。 */
    public static List<Relic> relicsOf(Collection<String> ids) {
        List<Relic> relics = new ArrayList<>(ids.size());
        for (String id : ids) {
            Relic relic = BY_ID.get(id);
            if (relic != null) {
                relics.add(relic);
            }
        }
        return List.copyOf(relics);
    }

    /** 随机抽一件**尚未拥有**的遗物；全部拥有时返回 null（战斗掉落用）。 */
    public static Relic randomUnowned(Random random, Collection<String> owned) {
        List<Relic> candidates = new ArrayList<>();
        for (Relic relic : BY_ID.values()) {
            // v0.4.0：初始遗物是"固定角色特有的"，不能从任何随机途径（战斗掉落 / 事件 / 指令）拿到
            // v0.4.6：事件限定遗物同样不进随机掉落（它们只能从对应事件拿）
            // v0.5.3.1：还要排掉"只能商店"与"只走藏宝箱/商店"的那几件（见 eliteDroppable）
            if (!eliteDroppable(relic.id())) {
                continue;
            }
            if (!owned.contains(relic.id())) {
                candidates.add(relic);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    /**
     * **初始遗物**（v0.4.0）：只随对应角色开局发放，不进随机掉落。
     *
     * <p>需求原文："不同角色不能获得其他角色的初始遗物，这是固定角色特有的。"</p>
     */
    private static final Set<String> STARTING_RELIC_IDS =
            Set.of("burning_blood", "ring_of_the_snake", "cracked_core", "crossbow");

    /**
     * **只能从事件获得**的遗物（v0.4.6 需求点名）。
     *
     * <p>需求原文："受惊的小猪仔 只能通过屠夫事件获得；失活的珊瑚 / 海洋之心 只能通过流浪商人事件获得；
     * 袭击号角 只能通过山羊事件获得。" 于是它们既不进精英掉落，也不进商店货架。</p>
     */
    private static final Set<String> EVENT_ONLY_RELIC_IDS = Set.of(
            "startled_piglet",    // 屠夫事件
            "dead_coral",         // 流浪商人事件
            "heart_of_the_sea",   // 流浪商人事件
            "raid_horn",          // 山羊事件
            "mogu_fillet",        // 嚒菇牛事件
            "honey_seasoning");   // 蜂巢事件

    /** 这件遗物是不是"只能从事件获得"。 */
    public static boolean isEventOnly(String relicId) {
        return relicId != null && EVENT_ONLY_RELIC_IDS.contains(relicId);
    }

    /**
     * **只能在商店买到**的遗物（v0.5.3.0 三件 → v0.5.3.1【行商羊驼披肩】改走"藏宝箱 / 商店" →
     * v0.5.3.2【制图台】也改走"藏宝箱 / 商店"，所以现在只剩【龙蛋】一件）——
     * 它既不进藏宝箱、也不进战斗掉落。
     */
    private static final Set<String> SHOP_ONLY_RELIC_IDS = Set.of(
            "dragon_egg");          // 龙蛋

    /**
     * **只走"藏宝箱 / 商店"、不进战斗掉落**的遗物（v0.5.3.1 用户给的来源表；v0.5.3.2 加了制图台）。
     *
     * <ul>
     *   <li>【行商羊驼披肩】：v0.5.3.1 从"只能商店"改成"商店 / 藏宝箱"；</li>
     *   <li>【制图台】：v0.5.3.2 从"只能商店"改成"商店 / 藏宝箱"。</li>
     * </ul>
     *
     * <p>它们**没有**把"战斗掉落（精英 / 常规战斗 10% 那一档）"算进去，所以在
     * {@link #randomUnowned} 里被排掉，只在 {@link #chestPool()} 与 {@link #shopPool()} 里出现。</p>
     */
    private static final Set<String> CHEST_AND_SHOP_ONLY_RELIC_IDS = Set.of(
            "trader_llama_shawl",   // 行商羊驼披肩
            "cartography_table");   // 制图台

    /**
     * **每件遗物的商店售价**（v0.5.3.1 用户逐件给定；v0.5.3.2 补上【制图台】210 与【龙蛋】240）。
     *
     * <p>没列进来的只有两种，而且它们**根本不卖**：</p>
     * <ul>
     *   <li>初始遗物（角色专属，不上架）；</li>
     *   <li>事件限定遗物（用户写"价格无效"）。</li>
     * </ul>
     *
     * <p>也就是说 v0.5.3.2 起"商店里能出现的每一件遗物都有明确单价"，
     * 配置里的统一价 {@code shopPriceRelic} 只剩**兜底**作用（万一以后加了没定价的遗物）。</p>
     */
    private static final Map<String, Integer> PRICES = Map.ofEntries(
            Map.entry("vajra", 195),
            Map.entry("anchor", 210),
            Map.entry("bag_of_preparation", 225),
            Map.entry("oddly_smooth_stone", 195),
            Map.entry("potion_belt", 205),
            Map.entry("small_vial", 195),
            Map.entry("dirty_glass", 210),
            Map.entry("worn_flint", 210),
            Map.entry("trader_llama_shawl", 205),
            Map.entry("unusual_shulker_box", 225),
            Map.entry("soul_campfire", 225),
            Map.entry("glow_ink_sac", 225),
            Map.entry("cartography_table", 210),
            Map.entry("dragon_egg", 240));

    /** 这件遗物的商店售价；**没有给定价格**时返回 0（调用方退回配置统一价）。 */
    public static int priceOf(String relicId) {
        return relicId == null ? 0 : PRICES.getOrDefault(relicId, 0);
    }

    /** 用户有没有给这件遗物单独定价。 */
    public static boolean hasPrice(String relicId) {
        return relicId != null && PRICES.containsKey(relicId);
    }

    /** 商店在售的遗物件数（自检用）。 */
    public static int pricedCount() {
        return PRICES.size();
    }

    /** 这件遗物是不是"只能商店买"。 */
    public static boolean isShopOnly(String relicId) {
        return relicId != null && SHOP_ONLY_RELIC_IDS.contains(relicId);
    }

    /**
     * **效果不在战斗内**的遗物（v0.5.3.0）：它们的 {@code triggers} 是空的，因为效果发生在
     * 获得的那一刻或别的界面里，战斗回合钩子一辈子都碰不到。
     *
     * <ul>
     *   <li>{@code worn_flint} 破旧打火石 —— 获得时给两张攻击牌附魔；</li>
     *   <li>{@code trader_llama_shawl} 行商羊驼披肩 —— 商店价格打折；</li>
     *   <li>{@code cartography_table} 制图台 —— 获得时发 5 次卡牌奖励；</li>
     *   <li>{@code soul_campfire} 灵魂篝火 —— 篝火界面可以多选。</li>
     * </ul>
     *
     * <p>自检里"每件遗物都要有触发点"那条会放行这个集合（见 {@code CombatSelfTest}）。</p>
     */
    public static final Set<String> OUTSIDE_COMBAT_RELIC_IDS = Set.of(
            "worn_flint", "trader_llama_shawl", "cartography_table", "soul_campfire");

    /** 这件遗物能不能从随机途径（战斗掉落 / 藏宝箱 / 商店）拿到 —— 只排初始遗物与事件限定遗物。 */
    public static boolean droppableRelic(String relicId) {
        return relicId != null && !isStarting(relicId) && !isEventOnly(relicId);
    }

    /** 这件遗物是不是"只走藏宝箱 / 商店、不进战斗掉落"（v0.5.3.1；v0.5.3.2 起两件）。 */
    public static boolean isChestAndShopOnly(String relicId) {
        return relicId != null && CHEST_AND_SHOP_ONLY_RELIC_IDS.contains(relicId);
    }

    /** 这件遗物能不能从**战斗掉落**里出现（精英掉落 / 常规战斗 10% 那一档）。 */
    public static boolean eliteDroppable(String relicId) {
        return droppableRelic(relicId) && !isShopOnly(relicId) && !isChestAndShopOnly(relicId);
    }

    /** 这件遗物能不能从**藏宝箱**里开出来。 */
    public static boolean chestDroppable(String relicId) {
        return droppableRelic(relicId) && !isShopOnly(relicId);
    }

    /** 这件遗物能不能在**商店**里上架。 */
    public static boolean shopSellable(String relicId) {
        return droppableRelic(relicId);
    }

    /**
     * **战斗掉落**的池子（v0.5.3.1 起有了"来源通道"这个说法）。
     *
     * <p>排除：初始遗物、事件限定遗物、【龙蛋】（只能商店）、
     * 【行商羊驼披肩】【制图台】（藏宝箱 / 商店）。</p>
     */
    public static List<Relic> elitePool() {
        List<Relic> list = new ArrayList<>();
        for (Relic relic : BY_ID.values()) {
            if (eliteDroppable(relic.id())) {
                list.add(relic);
            }
        }
        return List.copyOf(list);
    }

    /**
     * **藏宝箱**能开出来的遗物（v0.5.3.0；v0.5.3.1 加【行商羊驼披肩】；v0.5.3.2 加【制图台】）：
     * 排除初始遗物、事件限定、以及"只能商店"的【龙蛋】。
     */
    public static List<Relic> chestPool() {
        List<Relic> list = new ArrayList<>();
        for (Relic relic : BY_ID.values()) {
            if (chestDroppable(relic.id())) {
                list.add(relic);
            }
        }
        return List.copyOf(list);
    }

    /**
     * **商店**能卖的遗物（v0.5.3.0）：可掉落的 + "只能商店"的。
     */
    public static List<Relic> shopPool() {
        List<Relic> list = new ArrayList<>();
        for (Relic relic : BY_ID.values()) {
            if (shopSellable(relic.id())) {
                list.add(relic);
            }
        }
        return List.copyOf(list);
    }

    /** 这件遗物是不是"角色初始遗物"。 */
    public static boolean isStarting(String relicId) {
        return relicId != null && STARTING_RELIC_IDS.contains(relicId);
    }

    /** 所有"随机途径的候选"（战斗掉落 ∪ 藏宝箱 ∪ 商店）—— 排除初始遗物与事件限定遗物。 */
    public static List<Relic> droppable() {
        List<Relic> list = new ArrayList<>();
        for (Relic relic : BY_ID.values()) {
            if (droppableRelic(relic.id())) {
                list.add(relic);
            }
        }
        return List.copyOf(list);
    }
}
