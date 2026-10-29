package com.slaythecraft.combat.mc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.slaythecraft.combat.core.Enemy;
import com.slaythecraft.combat.core.Intent;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

/**
 * Mob behaviour table: which intent rotation (and which display name) a mob type fights with.
 * This is the only place that knows about Minecraft's entity types, so adding new enemy patterns
 * never touches the battle engine.
 *
 * <p><b>意图状态机</b>（v0.4.5）：一个 {@link Profile} 不再只是"一串意图"，而是一串**回合**，
 * 每个回合里可以有**多个动作**，按顺序结算完再轮到下一回合。需求里给的原型是僵尸：</p>
 *
 * <pre>
 * 第一回合：伤害 5
 * 第二回合：伤害 2×3，对玩家施加挖掘疲劳 1
 * 第三回合：获得力量 1，获得 5 格挡
 * </pre>
 *
 * <p>老写法（{@link #register(EntityType, String, Intent...)}，每个 {@code Intent} 各占一回合）
 * 仍然可用，行为与之前完全一致。</p>
 */
public final class MobIntents {

    /**
     * A mob's battle identity: what the player sees and the state machine it telegraphs.
     *
     * <p>v0.4.6.7 追加三个"特殊机制"字段（都是需求里点名的 BOSS 行为）：</p>
     * <ul>
     *   <li>{@code hpLock} —— 锁血（闪电苦力怕）：血量即将低于 1 时进入"晕眩"，下一回合结算蓄能后死亡；</li>
     *   <li>{@code loopFrom} —— 状态机从第几个回合开始循环（"第一回合只做一次"）；</li>
     *   <li>{@code sleepTurns} + {@code awakeTurns} —— 前几回合睡眠（嗅探兽），苏醒后换成另一套状态机。</li>
     * </ul>
     */
    public record Profile(String displayName, List<List<Intent>> turns, boolean hpLock, int loopFrom,
                          int sleepTurns, List<List<Intent>> awakeTurns) {

        public Profile {
            turns = List.copyOf(turns);
            awakeTurns = List.copyOf(awakeTurns);
        }

        /** 普通档案（老写法）：从头循环、不睡、不锁血。 */
        public Profile(String displayName, List<List<Intent>> turns) {
            this(displayName, turns, false, 0, 0, List.of());
        }

        /** 兼容旧接口：把状态机拍平成"每回合的第一个动作"。 */
        public List<Intent> rotation() {
            List<Intent> first = new ArrayList<>(turns.size());
            for (List<Intent> turn : turns) {
                first.add(turn.isEmpty() ? Intent.unknown() : turn.get(0));
            }
            return List.copyOf(first);
        }

        /** 这个状态机最长的一回合有几个动作（自检/UI 用）。 */
        public int maxActionsPerTurn() {
            int max = 0;
            for (List<Intent> turn : turns) {
                max = Math.max(max, turn.size());
            }
            return max;
        }
    }

    private static final Map<EntityType<?>, Profile> PROFILES = new HashMap<>();

    /**
     * **虚拟怪物**（v0.4.6.7）：原版没有对应的 EntityType，但需要有图鉴条目与意图状态机。
     *
     * <p>v0.4.6.8 起**暂时没有虚拟怪物**（闪电苦力怕已移除）；机制保留给"原版没有独立实体类型
     * 的 BOSS"这类需求，避免下次又要临时改一圈。</p>
     */
    private static final Map<String, Profile> VIRTUALS = new HashMap<>();
    /** 一回合里依次执行的动作（状态机的"一帧"）。 */
    private static final Profile DEFAULT_HOSTILE =
            new Profile("敌人", turns(turn(Intent.attack(4)), turn(Intent.attack(5))));
    private static final Profile DEFAULT_PASSIVE =
            new Profile("生物", turns(turn(Intent.unknown()), turn(Intent.attack(2))));

    static {
        // v0.4.5：需求里给的意图状态机样例 —— 伤害 5 / 伤害 2×3 + 挖掘疲劳 1 / 力量 1 + 格挡 5，然后循环
        // v0.4.6.7 需求给的僵尸状态机：伤害5 / 伤害2×3+挖掘疲劳1 / 力量1，循环
        registerScript(EntityType.ZOMBIE, "僵尸",
                turn(Intent.attack(5)),
                turn(Intent.attack(3, 2), Intent.miningFatigue(1)),
                turn(Intent.strength(1)));
        // v0.4.6.7：施加易伤2 / 伤害3×2 / 伤害5，循环
        registerScript(EntityType.HUSK, "尸壳",
                turn(Intent.vulnerable(2)),
                turn(Intent.attack(2, 3)),
                turn(Intent.attack(5)));
        registerScript(EntityType.DROWNED, "溺尸",
                turn(Intent.attack(5)),
                turn(Intent.defend(4), Intent.attack(5)));
        registerScript(EntityType.ZOMBIE_VILLAGER, "僵尸村民",
                turn(Intent.attack(4)),
                turn(Intent.miningFatigue(2), Intent.attack(4)));
        // v0.4.6.7：等待 / 伤害12 / 获得5格挡，循环
        registerScript(EntityType.SKELETON, "骷髅",
                turn(Intent.unknown()),
                turn(Intent.attack(12)),
                turn(Intent.defend(5)));
        // v0.4.6.7：等待 / 伤害12 / 挖掘疲劳2，循环
        registerScript(EntityType.STRAY, "流浪者",
                turn(Intent.unknown()),
                turn(Intent.attack(12)),
                turn(Intent.miningFatigue(2)));
        // v0.4.6.1：凋零（累加，超过当前生命即死）需要一个正常的怪物来源 —— 凋灵骷髅正好合适
        registerScript(EntityType.WITHER_SKELETON, "凋灵骷髅",
                turn(Intent.attack(7)),
                turn(Intent.attack(7), Intent.strength(2)),
                turn(Intent.decay(3), Intent.attack(5)));
        // v0.4.6.7：伤害9 / 伤害8 / 力量1，循环
        // （带电的苦力怕是**第 1 层 BOSS 闪电苦力怕**，走下面的虚拟档案，不在这里）
        registerScript(EntityType.CREEPER, "苦力怕",
                turn(Intent.attack(9)),
                turn(Intent.attack(8)),
                turn(Intent.strength(1)));
        registerScript(EntityType.SPIDER, "蜘蛛",
                turn(Intent.attack(5)),
                turn(Intent.miningFatigue(2), Intent.attack(3)));
        registerScript(EntityType.CAVE_SPIDER, "洞穴蜘蛛",
                turn(Intent.attack(3), Intent.statusCard("venom", 1)));
        registerScript(EntityType.SLIME, "史莱姆",
                turn(Intent.attack(4)),
                turn(Intent.defend(6), Intent.statusCard("slimed", 1)));
        registerScript(EntityType.MAGMA_CUBE, "岩浆怪",
                turn(Intent.attack(5)),
                turn(Intent.burn(2), Intent.statusCard("burn", 1)));
        registerScript(EntityType.BLAZE, "烈焰人",
                turn(Intent.attack(2, 4)),
                turn(Intent.burn(3)));
        registerScript(EntityType.GHAST, "恶魂",
                turn(Intent.attack(7)),
                turn(Intent.attack(7)));
        // v0.4.6.7：挖掘疲劳1 / 缓慢1+伤害6 / 易伤1+伤害8 / 恢复8生命，循环
        registerScript(EntityType.WITCH, "女巫",
                turn(Intent.miningFatigue(1)),
                turn(Intent.slow(1), Intent.attack(6)),
                turn(Intent.vulnerable(1), Intent.attack(8)),
                turn(Intent.heal(8)));
        // v0.4.6.7：伤害5 / 伤害12 / 格挡5 / （挖掘疲劳2 或 场上只剩1只掠夺者时再召唤1只），循环
        registerScript(EntityType.PILLAGER, "掠夺者",
                turn(Intent.attack(5)),
                turn(Intent.attack(12)),
                turn(Intent.defend(5)),
                turn(Intent.summonIfFew("minecraft:pillager", 1, 1), Intent.miningFatigue(2)));
        // v0.4.6.7：易伤3+缓慢1 / 伤害3×3 / 伤害6 / 力量1，循环
        registerScript(EntityType.VINDICATOR, "卫道士",
                turn(Intent.vulnerable(3), Intent.slow(1)),
                turn(Intent.attack(3, 3)),
                turn(Intent.attack(6)),
                turn(Intent.strength(1)));
        // v0.4.6.7：召唤恼鬼×2 / 伤害12 / 易伤2+伤害3×5 / 伤害14 / （挖掘疲劳2 或 场上恼鬼归0时再召唤2只）
        // 第一回合只做一次，之后第 2~5 回合循环
        register(EntityType.EVOKER, new Profile("唤魔者",
                turns(turn(Intent.summon("minecraft:vex", 2)),
                        turn(Intent.attack(12)),
                        turn(Intent.vulnerable(2), Intent.attack(5, 3)),
                        turn(Intent.attack(14)),
                        turn(Intent.summonIfFew("minecraft:vex", 2, 0), Intent.miningFatigue(2))),
                false, 1, 0, List.of()));
        registerScript(EntityType.RAVAGER, "劫掠兽",
                turn(Intent.attack(9)),
                turn(Intent.defend(5), Intent.attack(9)));
        registerScript(EntityType.ZOMBIFIED_PIGLIN, "僵尸猪灵",
                turn(Intent.attack(5)),
                turn(Intent.attack(5)));
        registerScript(EntityType.PIGLIN, "猪灵",
                turn(Intent.attack(5)),
                turn(Intent.defend(4)));
        registerScript(EntityType.HOGLIN, "疣猪兽",
                turn(Intent.attack(7)),
                turn(Intent.attack(7)));
        registerScript(EntityType.ENDERMAN, "末影人",
                turn(Intent.attack(9)),
                turn(Intent.vulnerable(2)),
                turn(Intent.attack(9)));
        registerScript(EntityType.SILVERFISH, "蠹虫",
                turn(Intent.attack(2, 3)));
        // v0.4.6.7 需求："将潜影贝加入【终末住民】阵营，属于常规敌怪。" —— 缩在壳里打漂浮弹
        registerScript(EntityType.SHULKER, "潜影贝",
                turn(Intent.attack(6)),
                turn(Intent.defend(8)),
                turn(Intent.attack(3, 3)));
        registerScript(EntityType.ENDERMITE, "末影螨",
                turn(Intent.attack(3)),
                turn(Intent.miningFatigue(2)));
        registerScript(EntityType.GUARDIAN, "守卫者",
                turn(Intent.attack(6)),
                turn(Intent.attack(6)));
        registerScript(EntityType.PHANTOM, "幻翼",
                turn(Intent.attack(5)),
                turn(Intent.attack(5)));

        // ------------------------------------------------------------------ v0.4.6.3
        // 反馈原文："BOSS 图鉴没有正常在各个所属阵营显示，有时也会显示'阵营没有记录怪物'。"
        // 根因：这些生物的**意图状态机从来没登记过**，而怪物图鉴的数据源就是 MobIntents.entries()，
        // 所以 BOSS（以及个别精英）在图鉴里根本不存在，所属阵营自然"没有记录怪物"。
        // 现在把三层尖塔的全部 BOSS（见 FloorBoss.ALL）与漏掉的精英补齐。
        // 第 1 层 BOSS
        // v0.4.6.8 需求："移除闪电苦力怕的所有内容。闪电苦力怕的状态机机制现在属于旋风人。"
        // 特殊状态【蓄能】= 死亡时一次性打给玩家的伤害会计；血量即将低于 1 时进入【锁血】
        //（界面显示"晕眩"而不是数字），下一回合结算蓄能后死亡。
        // 意图循环 = 第一回合只做一次，之后第 2~5 回合循环。
        register(EntityType.BREEZE, new Profile("旋风人",
                turns(turn(Intent.charge(15)),
                        turn(Intent.attack(12), Intent.charge(3)),
                        turn(Intent.attack(15), Intent.charge(3)),
                        turn(Intent.strength(1), Intent.charge(3)),
                        turn(Intent.heal(15), Intent.charge(10))),
                true, 1, 0, List.of()));
        // v0.4.6.7 需求："嗅探兽的前三回合处于『睡眠』状态，在每回合的出牌环节开始前获得 30 格挡，
        // 不会执行任何行动。在这期间，如果它的格挡被击破并导致其受到伤害，其会结束睡眠状态。"
        // 苏醒后：施加挖掘疲劳2+造成13伤害 / 造成6×2伤害 / 造成10伤害 / 施加缓慢2，循环。
        // 注：需求原文第三回合的动作缺失（"第三回合：造成"），这里按 10 点伤害补全。
        register(EntityType.SNIFFER, new Profile("嗅探兽",
                turns(turn(Intent.unknown())), false, 0, 3,
                turns(turn(Intent.miningFatigue(2), Intent.attack(13)),
                        turn(Intent.attack(2, 6)),
                        turn(Intent.attack(10)),
                        turn(Intent.slow(2)))));
        // 第 2 层 BOSS
        registerScript(EntityType.ZOGLIN, "僵尸疣猪兽",
                turn(Intent.attack(10)),
                turn(Intent.strength(2), Intent.attack(6)),
                turn(Intent.attack(12), Intent.decay(3)));
        registerScript(EntityType.PANDA, "熊猫",
                turn(Intent.defend(12), Intent.attack(5)),
                turn(Intent.attack(8)),
                turn(Intent.statusCard("dazed", 1), Intent.attack(9)));
        registerScript(EntityType.ELDER_GUARDIAN, "远古守卫者",
                turn(Intent.attack(9), Intent.vulnerable(2)),
                turn(Intent.attack(3, 4)),
                turn(Intent.strength(2), Intent.defend(10)));
        // 第 3 层 BOSS
        registerScript(EntityType.ENDER_DRAGON, "末影龙",
                turn(Intent.attack(12)),
                turn(Intent.vulnerable(3), Intent.attack(10)),
                turn(Intent.attack(3, 6), Intent.strength(2)));
        registerScript(EntityType.WITHER, "凋零",
                turn(Intent.attack(8), Intent.decay(3)),
                turn(Intent.attack(12)),
                turn(Intent.decay(5), Intent.strength(2)));
        registerScript(EntityType.WARDEN, "坚守者",
                turn(Intent.attack(14)),
                turn(Intent.decay(4), Intent.attack(8)),
                turn(Intent.attack(20), Intent.defend(10)));
        // 之前同样漏登记的精英 / 常规成员（否则它们所属的阵营也会显示"没有记录怪物"）
        registerScript(EntityType.IRON_GOLEM, "铁傀儡",
                turn(Intent.attack(9)),
                turn(Intent.defend(6), Intent.attack(9)));
        registerScript(EntityType.PIGLIN_BRUTE, "猪灵蛮兵",
                turn(Intent.attack(8)),
                turn(Intent.defend(5), Intent.attack(8)),
                turn(Intent.strength(2), Intent.attack(8)));
        // v0.4.6.7：伤害6 / 伤害3×2 / 伤害4，循环（召唤敌怪：不会在常规战斗里遇到）
        registerScript(EntityType.VEX, "恼鬼",
                turn(Intent.attack(6)),
                turn(Intent.attack(2, 3)),
                turn(Intent.attack(4)));
    }


    private MobIntents() {
    }

    /** 虚拟怪物档案（按 id；没有返回 {@code null}）。 */
    public static Profile virtualProfile(String entityId) {
        return VIRTUALS.get(entityId);
    }

    /** 全部虚拟怪物 id（图鉴用）。 */
    public static List<String> virtualIds() {
        return List.copyOf(VIRTUALS.keySet());
    }

    /** 一个回合里依次执行的动作列表。 */
    @SafeVarargs
    private static List<Intent> turn(Intent... actions) {
        return List.of(actions);
    }

    @SafeVarargs
    private static List<List<Intent>> turns(List<Intent>... turns) {
        return List.of(turns);
    }

    /**
     * 兼容登记：每个 {@link Intent} 自成一回合（结果与老版本逐字节一致）。
     */
    private static void register(EntityType<?> type, String displayName, Intent... rotation) {
        List<List<Intent>> turns = new ArrayList<>(rotation.length);
        for (Intent intent : rotation) {
            turns.add(List.of(intent));
        }
        PROFILES.put(type, new Profile(displayName, turns));
    }

    /** 登记一份**完整档案**（带锁血 / 循环起点 / 睡眠这些特殊机制）。 */
    private static void register(EntityType<?> type, Profile profile) {
        PROFILES.put(type, profile);
    }

    /** 登记一个真正的意图状态机：每个参数是一回合里的动作列表。 */
    @SafeVarargs
    private static void registerScript(EntityType<?> type, String displayName, List<Intent>... turns) {
        PROFILES.put(type, new Profile(displayName, turns(turns)));
    }

    /**
     * 怪物图鉴用的条目（v0.4.6）：一个实体类型 + 它的战斗档案。
     *
     * @param type      实体类型
     * @param id        实体 id（{@code minecraft:zombie}）
     * @param profile   战斗档案（显示名 + 意图状态机）
     */
    public record Entry(EntityType<?> type, String id, Profile profile) {
    }

    /** 全部登记过意图状态机的怪物（图鉴按显示名排序展示）。 */
    public static List<Entry> entries() {
        List<Entry> list = new ArrayList<>();
        for (Map.Entry<EntityType<?>, Profile> entry : PROFILES.entrySet()) {
            ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                    .getKey(entry.getKey());
            list.add(new Entry(entry.getKey(), id == null ? "" : id.toString(), entry.getValue()));
        }
        return List.copyOf(list);
    }

    /** 某个实体类型是不是登记过意图状态机。 */
    public static boolean registered(EntityType<?> type) {
        return PROFILES.containsKey(type);
    }

    /**
     * 怪物图鉴的**描述**（v0.4.6）：一句"它是谁 + 怎么打"的说明。
     *
     * <p>没有单独写描述的怪物会按阵营/档位自动生成一句通用文案，所以新增怪物永远不会显示空白。</p>
     */
    public static String describe(EntityType<?> type) {
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type);
        String path = id == null ? "" : id.getPath();
        String custom = DESCRIPTIONS.get(path);
        if (custom != null) {
            return custom;
        }
        com.slaythecraft.combat.core.Faction faction = com.slaythecraft.combat.core.Faction.ofMob(path);
        String who = faction == null ? "野外生物" : faction.label();
        return "属于【" + who + "】的敌人，回合行动见下方的意图状态机。";
    }

    /** 虚拟怪物（闪电苦力怕）的图鉴描述：按 id 的路径查同一张描述表。 */
    public static String describeVirtual(String entityId) {
        if (entityId == null || entityId.isEmpty()) {
            return "";
        }
        int colon = entityId.indexOf(':');
        String custom = DESCRIPTIONS.get(colon >= 0 ? entityId.substring(colon + 1) : entityId);
        return custom == null ? "属于【尖塔护卫】的 BOSS，回合行动见下方的意图状态机。" : custom;
    }

    /** 类型 id 路径 → 图鉴描述（v0.4.6）。 */
    private static final Map<String, String> DESCRIPTIONS = new HashMap<>();

    private static void describe(String path, String description) {
        DESCRIPTIONS.put(path, description);
    }

    static {
        describe("zombie", "最常见的亡灵：先来一记直拳，再连打三下顺手削弱你，最后一边蓄力一边架起格挡。");
        describe("husk", "沙漠里的僵尸：伤害更高，还会在第二回合补上一次挖掘疲劳。");
        describe("drowned", "水下的亡灵：会先打一拳再架格挡，攻守交替。");
        describe("zombie_villager", "被感染的村民：攻击不高，但会连续削弱你的输出。");
        describe("skeleton", "远程亡灵：喜欢打四连射，也会塞给你【晕眩】并同时架起格挡。");
        describe("stray", "冰原骷髅：攻击附带挖掘疲劳，越拖越难受。");
        describe("wither_skeleton", "下界亡灵：伤害很高，第二回合还会给自己叠力量，第三回合开始往你身上堆【凋零】。");
        describe("creeper", "会爆炸的伏击者：9 点、8 点，然后开始蓄力。");

        describe("sniffer", "第 1 层的 BOSS 之一：前三回合在睡觉（每回合先堆 30 格挡），被打破格挡并掉血才会醒，醒来后每一击都很重。");
        describe("evoker", "第 1 层的 BOSS 之一：开场召唤两只恼鬼，之后易伤 + 五连击，恼鬼清空时会再召唤。");
        describe("shulker", "终末住民：缩在壳里，先用漂浮弹打你，再补一层壳，然后三连弹。");
        describe("spider", "轻快的节肢动物：一边咬你一边削弱你的攻击。");
        describe("cave_spider", "洞穴蜘蛛：伤害低但会往你的弃牌堆里塞【毒液】。");
        describe("slime", "史莱姆：会架格挡，还会把【粘液】塞进你的牌堆。");
        describe("magma_cube", "下界岩浆怪：附加灼烧，还会塞【灼烧】牌。");
        describe("blaze", "烈焰人：四连射之后给你附加灼烧，地狱生物免疫灼烧。");
        describe("ghast", "恶魂：只有一招，但每回合 7 点伤害很稳定。");
        describe("witch", "女巫：先下毒再削弱，随后塞给你【呼唤】。");
        describe("pillager", "灾厄部队的枪手：常规敌怪，攻守兼备。");
        describe("vindicator", "灾厄部队的近战：常规敌怪，8 点伤害起手。");
        describe("evoker", "唤魔者：先破防再重击，也是第 1 层的 BOSS 之一。");
        describe("ravager", "劫掠兽：精英，9 点伤害 + 格挡。");
        describe("zombified_piglin", "僵尸猪灵：稳定的 5 点攻击。");
        describe("piglin", "猪灵：打得一手好算盘，攻守交替。");
        describe("hoglin", "疣猪兽：每回合都是 7 点伤害。");
        describe("zoglin", "僵尸疣猪兽：第 2 层的 BOSS 之一。");
        describe("enderman", "末影人：重击之后让你易伤，再补一记重击。");
        describe("silverfish", "蠹虫：微弱但快速的三连咬。");
        describe("endermite", "末影螨：轻咬加挖掘疲劳。");
        describe("guardian", "守卫者：来自深海，每回合 6 点伤害。");
        describe("phantom", "幻翼：亡灵，夜里缠着你不放。");
        describe("breeze", "第 1 层的 BOSS 之一：风里裹着【蓄能】，血量掉到 1 以下时会进入【晕眩】锁血状态，下一回合把攒下的蓄能全部打给你后消散。");
        describe("sniffer", "嗅探兽：第 1 层的 BOSS 之一，鼻子很灵。");
        describe("panda", "熊猫：第 2 层的 BOSS 之一，别被外表骗了。");
        describe("warden", "坚守者：第 3 层的 BOSS 之一，尖塔里最沉重的存在。");
        describe("wither", "凋零：第 3 层的 BOSS 之一，会持续凋零你的生命。");
        describe("ender_dragon", "末影龙：第 3 层的 BOSS 之一，尖塔的终点。");
        describe("iron_golem", "铁傀儡：人工造物，默认中立，免疫第一次负面效果。");
        describe("elder_guardian", "远古守卫者：第 2 层的 BOSS 之一，深海的眼睛盯着你，近身就会被激光扫射。");
        describe("piglin_brute", "猪灵蛮兵：堡垒护卫精英，成队出现（2 只）。");
        describe("vex", "恼鬼：唤魔者的召唤物，飘来飘去。");
    }

    /** The profile for a mob, falling back to a generic hostile or passive pattern. */
    public static Profile profile(Mob mob) {

        Profile known = PROFILES.get(mob.getType());
        if (known != null) {
            return known;
        }
        return mob instanceof net.minecraft.world.entity.monster.Enemy ? DEFAULT_HOSTILE : DEFAULT_PASSIVE;
    }

    /** 某实体类型的状态机（自检/图鉴用）；没有登记过则返回 {@code null}。 */
    public static Profile profileOf(EntityType<?> type) {
        return PROFILES.get(type);
    }

    /** 已登记的实体类型数量（自检用）。 */
    public static int size() {
        return PROFILES.size();
    }

    /** 该实体类型是否登记过状态机（自检用）。 */
    public static boolean known(EntityType<?> type) {
        return PROFILES.containsKey(type);
    }

    /** 仅供自检：直接构造一个状态机档案。 */
    public static Profile script(String displayName, List<List<Intent>> turns) {
        return new Profile(displayName, turns);
    }
}
