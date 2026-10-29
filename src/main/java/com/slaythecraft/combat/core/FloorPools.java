package com.slaythecraft.combat.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * **层数专属怪物池**（v0.4.6.7）。
 *
 * <p>需求原文：</p>
 * <pre>
 * 新定义：层数专属怪物
 * 每个层数都有他们包含的怪物池，玩家不能对非本层的怪物触发战斗。
 * 所有层数的BOSS都只能触发一次战斗，且进入下一层后，不能再和上一层的boss触发战斗。
 *
 * 这些怪物只有在到达第三层才能发起战斗。        → 【终末住民】
 * 这些怪物可以在二层/三层发起战斗。             → 【堡垒守卫】【地狱生物】
 * 这些怪物只有在到达第二层才能发起战斗，        → 【深海生物】
 *   且前置层和后续层（一层和三层）不会再触发和它们的战斗。
 * 这些怪物可以在一层/二层发起战斗。             → 【灾厄部队】【人工造物】
 * 这些怪物在第一层就能发起战斗，                → 【亡灵生物】【常规生物】
 *   且后续层不会再触发和它们的战斗。
 * </pre>
 *
 * <p>BOSS 不按阵营判定：每只 BOSS 自己就规定了层数（见 {@link FloorBoss}），所以
 * {@link #allowed(String, int)} 先看 BOSS 表、再看阵营表。**没有登记过阵营的实体**
 * （别的模组加进来的怪）按【常规敌怪】处理，也就是只在第 1 层能打 —— 否则"非本层的怪物"
 * 这条规则就没法兜底。</p>
 *
 * <p>纯逻辑、零 Minecraft 依赖，所以这些数字都在引擎自检（{@code CombatSelfTest}）里钉死。</p>
 */
public final class FloorPools {

    /** 阵营 → 这一族能发起战斗的层数（顺序即展示顺序）。 */
    private static final Map<Faction, Set<Integer>> FLOORS = new LinkedHashMap<>();

    static {
        // "只有在到达第三层才能发起战斗"
        put(Faction.END_DWELLER, 3);
        // "可以在二层/三层发起战斗"
        put(Faction.BASTION, 2, 3);
        put(Faction.NETHER, 2, 3);
        // "只有在到达第二层才能发起战斗，且一层和三层不会再触发"
        put(Faction.DEEP_SEA, 2);
        // "可以在一层/二层发起战斗"
        put(Faction.ILLAGER, 1, 2);
        put(Faction.CONSTRUCT, 1, 2);
        // "在第一层就能发起战斗，且后续层不会再触发"
        put(Faction.UNDEAD, 1);
        put(Faction.NORMAL, 1);
        // 尖塔护卫是 BOSS 名册：逐只按 FloorBoss 的层数判定，所以这里不给层数
        put(Faction.SPIRE_GUARDIAN);
    }

    private FloorPools() {
    }

    private static void put(Faction faction, int... floors) {
        Set<Integer> set = new LinkedHashSet<>();
        for (int floor : floors) {
            set.add(floor);
        }
        FLOORS.put(faction, set);
    }

    /** 某个阵营能打的层数（只读；尖塔护卫返回空集 = 逐只按 BOSS 表判定）。 */
    public static Set<Integer> floorsOf(Faction faction) {
        Set<Integer> set = FLOORS.get(faction);
        // 用 LinkedHashSet 复制：Set.copyOf 的迭代顺序不保证，会让"第 2、3 层"这种文案随机
        return set == null ? Set.of() : new java.util.LinkedHashSet<>(set);
    }

    /**
     * 这个阵营**在第 floor 层**能不能发起战斗。
     *
     * <p>尖塔护卫（BOSS 名册）返回 {@code false}：BOSS 的层数由 {@link FloorBoss} 逐只决定，
     * 调用方应该用 {@link #allowed(String, int)}。</p>
     */
    public static boolean allowedOnFloor(Faction faction, int floor) {
        Set<Integer> set = FLOORS.get(faction);
        return set != null && set.contains(floor);
    }

    /**
     * 这个实体**在第 floor 层**能不能发起战斗。
     *
     * <p>顺序：BOSS 表 → 阵营表 → 没登记阵营按【常规敌怪】兜底。</p>
     */
    public static boolean allowed(String entityId, int floor) {
        if (entityId == null || floor < 1) {
            return false;
        }
        FloorBoss boss = FloorBoss.byEntityId(entityId);
        if (boss != null) {
            return boss.floor() == floor;
        }
        Faction faction = Faction.ofMob(entityId);
        if (faction == null) {
            // 没登记阵营的怪（别的模组加的）按常规敌怪处理：只在第 1 层
            return allowedOnFloor(Faction.NORMAL, floor);
        }
        if (faction == Faction.SPIRE_GUARDIAN) {
            // 尖塔护卫里没有登记成 BOSS 的成员（理论上不该有）：不给它开战
            return false;
        }
        return allowedOnFloor(faction, floor);
    }

    /** 这个实体是不是"本层专属"之外的东西（用来给玩家一句解释）。 */
    public static String blockedReason(String entityId, int floor) {
        FloorBoss boss = FloorBoss.byEntityId(entityId);
        if (boss != null) {
            return "【" + boss.name() + "】是第 " + boss.floor() + " 层的 BOSS，这里是第 " + floor + " 层，打不起来。";
        }
        Faction faction = Faction.ofMob(entityId);
        String who = faction == null ? "常规敌怪" : faction.label();
        Set<Integer> floors = faction == null ? floorsOf(Faction.NORMAL) : floorsOf(faction);
        return "【" + who + "】只在" + describeFloors(floors) + "能发起战斗，这里是第 " + floor + " 层。";
    }

    /** 层数集合的展示文本：{@code 第 2、3 层}。 */
    public static String describeFloors(Set<Integer> floors) {
        if (floors == null || floors.isEmpty()) {
            return "尖塔里";
        }
        List<String> parts = new ArrayList<>();
        List<Integer> sorted = new ArrayList<>(floors);
        sorted.sort(Integer::compareTo);
        for (int floor : sorted) {
            parts.add(String.valueOf(floor));
        }
        return "第 " + String.join("、", parts) + " 层";
    }

    /** 某个阵营的层数文本（图鉴/提示用）。 */
    public static String describe(Faction faction) {
        if (faction == Faction.SPIRE_GUARDIAN) {
            return "各层尽头的 BOSS（逐只按它自己的层数挑战）";
        }
        return describeFloors(floorsOf(faction));
    }

    /** 已登记的阵营数量（自检用）。 */
    public static int size() {
        return FLOORS.size();
    }
}
