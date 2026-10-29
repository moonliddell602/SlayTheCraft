package com.slaythecraft.combat.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * **本层可挑战名单**（v0.5.0.9）。
 *
 * <p>需求原文："显示：挑战第X层boss的功能按钮（把boss拉过来进行战斗）
 * 功能修改为：详情查看当前层数可以触发战斗的敌怪和boss（以阵营筛选，包括【尖塔护卫】【亡灵生物】等。）"</p>
 *
 * <p>把 {@link Faction}（阵营成员）与 {@link FloorPools}（每族能打的层数）+ {@link FloorBoss}
 * （逐只 BOSS 的层数）三份既有数据聚合成"第 N 层能打谁、各归哪个阵营"的只读快照，
 * 供界面直接渲染。</p>
 *
 * <p>纯逻辑、零 Minecraft 依赖，所以能进裸 JVM 自检。</p>
 */
public final class FloorRoster {

    /**
     * 名单里的一条。
     *
     * @param faction  归属阵营
     * @param entityId 实体 id（{@code minecraft:zombie} 这种带命名空间的写法）
     * @param name     显示名（BOSS 用 BOSS 表里的名字，其余用实体 id 的路径）
     * @param boss     是不是 BOSS 档
     * @param bossHp   BOSS 的尖塔战斗生命值（非 BOSS 为 0）
     */
    public record Entry(Faction faction, String entityId, String name, boolean boss, int bossHp) {
    }

    private FloorRoster() {
    }

    /**
     * 第 {@code floor} 层可以发起战斗的全部目标，**按阵营分组**（阵营顺序 = {@link Faction#all()}）。
     *
     * <p>规则与 {@link FloorPools#allowed(String, int)} 完全一致，保证"名单里有的就能打、
     * 名单里没有的就打不了"：</p>
     * <ul>
     *   <li>BOSS（{@link FloorBoss}）只出现在**它自己那一层**，并且归到【尖塔护卫】；</li>
     *   <li>阵营成员按 {@link FloorPools#allowedOnFloor(Faction, int)} 判定；</li>
     *   <li>既是 BOSS 又是某阵营成员的（唤魔者 / 远古守卫者 / 末影龙等）**只出现在尖塔护卫下**
     *       —— 免得同一个目标在名单里出现两次；</li>
     *   <li>【尖塔护卫】阵营自身的成员表（breeze / sniffer / panda / warden / wither）里，
     *       凡是在 BOSS 表里的按 BOSS 层数出现，不在 BOSS 表里的按 {@link FloorPools} 的规则
     *       （该族没有登记层数，所以只有 BOSS 表里那些会出现）。</li>
     * </ul>
     *
     * @return 阵营 → 该阵营本层能打的目标（空列表的阵营不会出现在结果里）
     */
    public static Map<Faction, List<Entry>> byFaction(int floor) {
        Map<Faction, List<Entry>> grouped = new LinkedHashMap<>();
        if (floor < 1) {
            return grouped;
        }

        // 1) BOSS：只出现自己那一层，统一归到【尖塔护卫】
        for (FloorBoss boss : FloorBoss.bossesOf(floor)) {
            add(grouped, Faction.SPIRE_GUARDIAN,
                    new Entry(Faction.SPIRE_GUARDIAN, boss.entityId(), boss.name(), true, boss.battleHp()));
        }

        // 2) 各阵营的普通成员（跳过已经作为 BOSS 出现过的那一只，避免重复）
        for (Faction faction : Faction.all()) {
            if (faction == Faction.SPIRE_GUARDIAN) {
                // 尖塔护卫的成员全部由上面的 BOSS 表覆盖
                continue;
            }
            if (!FloorPools.allowedOnFloor(faction, floor)) {
                continue;
            }
            for (String path : Faction.membersOf(faction)) {
                String entityId = namespaced(path);
                if (FloorBoss.byEntityId(entityId) != null) {
                    // 这一只已经作为 BOSS 出现在尖塔护卫下了
                    continue;
                }
                add(grouped, faction, new Entry(faction, entityId, path, false, 0));
            }
        }
        return grouped;
    }

    /** 本层能挑战的目标总数（自检/标题用）。 */
    public static int count(int floor) {
        int total = 0;
        for (List<Entry> list : byFaction(floor).values()) {
            total += list.size();
        }
        return total;
    }

    /** 本层有可挑战目标的阵营数量。 */
    public static int factionCount(int floor) {
        return byFaction(floor).size();
    }

    /** 实体 id 补上 {@code minecraft:} 命名空间（阵营成员表里存的是路径）。 */
    public static String namespaced(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        return path.indexOf(':') >= 0 ? path : "minecraft:" + path;
    }

    private static void add(Map<Faction, List<Entry>> grouped, Faction faction, Entry entry) {
        grouped.computeIfAbsent(faction, key -> new ArrayList<>()).add(entry);
    }
}
