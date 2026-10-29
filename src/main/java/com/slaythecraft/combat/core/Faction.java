package com.slaythecraft.combat.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 敌人**阵营**（v0.4.1）。
 *
 * <p>需求："不同的敌人也有帮派，或者组织，管他们是什么呢。" 每个阵营有自己的成员、描述与特性；
 * v0.4.6.2 又按需求做了一轮归属与档位调整（新增【终末住民】，远古守卫者升为二层 BOSS 等）；</p>
 * <p>
 * 特性会真的作用在战斗里（免疫灼烧 / 免疫伤害药水 / 免疫第一次负面效果），
 * 所以阵营表放在 {@code core} 里：图鉴与战斗规则共用同一份数据。</p>
 */
public enum Faction {

    DEEP_SEA("深海生物",
            "来自深海的守卫者一族，眼睛多、脾气差，被打到会发激光。远古守卫者是它们的首领。",
            List.of("首领级成员【远古守卫者】是第 2 层 BOSS"),
            // v0.4.6.2：溺尸移到【亡灵生物】
            List.of("elder_guardian", "guardian")),
    UNDEAD("亡灵生物",
            "死者不安的产物。没有体温，也没有对药剂反应的身体。",
            List.of("免疫伤害药水（中毒 / 火焰药水对它们无效）"),
            // v0.4.6.2：溺尸加入；末影人/末影螨移到【终末住民】
            List.of("zombie", "husk", "drowned", "zombie_villager", "skeleton", "stray", "phantom")),
    NORMAL("常规敌怪",
            "最常见的野外生物，没有什么组织，只是恰好挡在路上。",
            List.of(),
            // v0.4.6.2：女巫移到【灾厄部队】，末影人/末影螨移到【终末住民】
            List.of("creeper", "spider", "cave_spider", "slime", "silverfish")),
    ILLAGER("灾厄部队",
            "袭击村庄的军队：掠夺者、卫道士、唤魔者与他们的攻城兽。",
            List.of(),
            // v0.4.6.2：女巫加入（常规敌怪）
            List.of("pillager", "vindicator", "evoker", "vex", "ravager", "witch")),
    NETHER("地狱生物",
            "在下界的高温里活下来的生物，火焰对它们来说是家常便饭。",
            List.of("免疫灼烧效果"),
            List.of("zombified_piglin", "wither_skeleton", "hoglin", "zoglin", "magma_cube", "blaze",
                    "ghast")),
    BASTION("堡垒护卫",
            "守着堡垒的猪灵与猪灵蛮兵，规矩很多。猪灵蛮兵是这一族的精锐。",
            List.of(),
            List.of("piglin", "piglin_brute")),
    CONSTRUCT("人工造物",
            "被造出来干活的铁家伙：没有痛觉，也不太在意你打了它几下。",
            List.of("免疫第一次被施加的负面效果"),
            // v0.4.6.2：铁傀儡明确归到人工造物（精英）
            List.of("iron_golem")),
    /**
     * 终末住民（v0.4.6.2 新阵营）。
     *
     * <p>需求原文："这些敌人的老家是永恒的虚空，我们应该把他们都送回他们来的地方。"
     * 末影螨 / 末影人是常规敌怪，末影龙是 BOSS。</p>
     */
    END_DWELLER("终末住民",
            "这些敌人的老家是永恒的虚空，我们应该把他们都送回他们来的地方。",
            List.of("末影人 / 末影螨是常规敌怪，末影龙是第 3 层 BOSS"),
            // v0.4.6.7 需求："将潜影贝加入【终末住民】阵营，属于常规敌怪。"
            List.of("endermite", "enderman", "shulker", "ender_dragon")),
    /**
     * 尖塔护卫（v0.4.6 建立，v0.4.6.5 起作为"全部 BOSS 的图鉴归属"）：本 mod 三层尖塔的 BOSS 名册。
     *
     * <p>需求把旋风人 / 唤魔者 / 嗅探兽 / 僵尸疣猪兽 / 熊猫 / 远古守卫者 / 凋零 / 坚守者 / 末影龙
     * 定为各层 BOSS；v0.4.6.3 起**怪物图鉴里的 BOSS 全部显示在本阵营下**（见
     * {@code MobCodex#factionOf}），所以这里只列"只属于尖塔"的那几只，唤魔者 / 僵尸疣猪兽 /
     * 远古守卫者 / 末影龙仍保留在各自的原生阵营里 —— 战斗特性（例如地狱生物免疫灼烧）
     * 走的是 {@link #ofMob(String)}，不会被图鉴的展示归属影响。</p>
     */
    SPIRE_GUARDIAN("尖塔护卫",
            "守在各层尖塔尽头的强大生物。它们不会自己找上门，需要你主动去挑战。",
            List.of("三层尖塔的全部 BOSS 都登记在这里（战斗生命值见怪物图鉴）"),
            List.of("breeze", "sniffer", "panda", "warden", "wither"));

    /** 图鉴里标成【精英】的成员（只是展示用；精英判定走 {@code mob_tier/elite} 标签）。 */
    private static final List<String> ELITE_MEMBERS =
            List.of("ravager", "iron_golem", "piglin_brute", "wither_skeleton", "guardian", "hoglin",
                    "phantom");

    private static final Map<String, Faction> BY_MOB = new LinkedHashMap<>();

    static {
        for (Faction faction : values()) {
            for (String mob : faction.members) {
                BY_MOB.put(mob, faction);
            }
        }
    }

    private final String label;
    private final String description;
    private final List<String> traits;
    private final List<String> members;

    Faction(String label, String description, List<String> traits, List<String> members) {
        this.label = label;
        this.description = description;
        this.traits = List.copyOf(traits);
        this.members = List.copyOf(members);
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    /** 特性（展示文本；为空表示没有特殊规则）。 */
    public List<String> traits() {
        return traits;
    }

    /** 成员生物的**实体 id 路径**（例如 {@code zombie}）。 */
    public List<String> members() {
        return members;
    }

    /** 这个成员是不是图鉴里标注的精英。 */
    public static boolean isEliteMember(String mobPath) {
        return mobPath != null && ELITE_MEMBERS.contains(mobPath);
    }

    /** 亡灵生物：免疫伤害药水。 */
    public boolean immuneToDamagePotions() {
        return this == UNDEAD;
    }

    /** 地狱生物：免疫灼烧效果。 */
    public boolean immuneToBurn() {
        return this == NETHER;
    }

    /** 人工造物：免疫第一次被施加的负面效果。 */
    public boolean immuneToFirstDebuff() {
        return this == CONSTRUCT;
    }

    /** 默认中立的阵营（放弃战斗不会造成损失）：人工造物。 */
    public boolean neutral() {
        return this == CONSTRUCT;
    }

    /** 按实体 id（可以是 {@code minecraft:zombie} 或 {@code zombie}）找阵营；找不到返回 {@code null}。 */
    public static Faction ofMob(String entityId) {
        if (entityId == null) {
            return null;
        }
        int colon = entityId.indexOf(':');
        String path = colon >= 0 ? entityId.substring(colon + 1) : entityId;
        return BY_MOB.get(path);
    }

    /** 全部阵营（图鉴按这个顺序展示）。 */
    public static List<Faction> all() {
        return List.of(values());
    }

    /** 某个阵营里所有成员的实体 id 路径（按表内顺序）。 */
    public static List<String> membersOf(Faction faction) {
        return faction == null ? List.of() : faction.members();
    }

    /** 成员文本：{@code 僵尸 / 骷髅 / 尸壳}（精英成员后面标【精英】）。 */
    public static String describeMembers(Faction faction) {
        List<String> names = new ArrayList<>();
        for (String member : membersOf(faction)) {
            names.add(member + (isEliteMember(member) ? "【精英】" : ""));
        }
        return String.join(" / ", names);
    }
}
