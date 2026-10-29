package com.slaythecraft.combat.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 三层尖塔的 BOSS 表（v0.0.2 V9；v0.4.5 每层多 BOSS + 战斗生命覆盖）。
 *
 * <p>BOSS **不由本 mod 生成**：需求写明"需自行寻找"，所以这里只记录"每一层有哪些 BOSS"。
 * 挑战时在玩家附近找这些实体（见 {@code CombatManager.challengeBoss}）。</p>
 *
 * <p>v0.4.5 按需求调整：</p>
 * <ul>
 *   <li>第 1 层：旋风人 / 唤魔者 / 嗅探兽（**移出**坚守者），战斗生命 224；</li>
 *   <li>第 2 层：僵尸疣猪兽 / 熊猫 / 远古守卫者（后两者 v0.4.6.2 起，远古守卫者 320），战斗生命 415；</li>
 *   <li>第 3 层：末影龙 / 凋零 / 坚守者（后两者移入），战斗生命 675。</li>
 * </ul>
 *
 * <p>{@code battleHp > 0} 表示"尖塔玩法内的战斗生命值"直接取这个数（**不改原版实体的血量上限**）；
 * {@code 0} 表示沿用原来的缩放规则。</p>
 *
 * @param floor    层号（1 起）
 * @param entityId 原版实体 id（用于附近搜索）
 * @param name     展示名
 * @param battleHp 尖塔战斗内的生命值覆盖（0 = 用默认缩放）
 */
public record FloorBoss(int floor, String entityId, String name, int battleHp) {

    /** 层数：三层尖塔。 */
    public static final int FLOORS = 3;

    // v0.4.6.8 需求："移除闪电苦力怕的所有内容。闪电苦力怕的状态机机制现在属于旋风人。"
    // 于是第 1 层回到 3 只 BOSS（旋风人 / 唤魔者 / 嗅探兽），蓄能+锁血机制搬到【旋风人】身上。

    public static final List<FloorBoss> ALL = List.of(
            // 第 1 层
            new FloorBoss(1, "minecraft:breeze", "旋风人", 224),
            new FloorBoss(1, "minecraft:evoker", "唤魔者", 224),
            new FloorBoss(1, "minecraft:sniffer", "嗅探兽", 224),

            // 第 2 层
            new FloorBoss(2, "minecraft:zoglin", "僵尸疣猪兽", 415),
            new FloorBoss(2, "minecraft:panda", "熊猫", 415),
            // v0.4.6.2 需求：远古守卫者从"精英"升级为第 2 层 BOSS，尖塔战斗生命 320
            new FloorBoss(2, "minecraft:elder_guardian", "远古守卫者", 320),
            // 第 3 层
            new FloorBoss(3, "minecraft:ender_dragon", "末影龙", 0),
            new FloorBoss(3, "minecraft:wither", "凋零", 675),
            new FloorBoss(3, "minecraft:warden", "坚守者", 675));

    /** 某一层的**代表 BOSS**（界面显示 / 兼容旧调用）；层号越界返回 {@code null}。 */
    public static FloorBoss ofFloor(int floor) {
        for (FloorBoss boss : ALL) {
            if (boss.floor == floor) {
                return boss;
            }
        }
        return null;
    }

    /** 某一层的**全部** BOSS（挑战时可以挑任意一只）。 */
    public static List<FloorBoss> bossesOf(int floor) {
        List<FloorBoss> list = new ArrayList<>();
        for (FloorBoss boss : ALL) {
            if (boss.floor == floor) {
                list.add(boss);
            }
        }
        return List.copyOf(list);
    }

    /** 按实体 id 找 BOSS 定义（不在表里返回 {@code null}）。 */
    public static FloorBoss byEntityId(String entityId) {
        if (entityId == null) {
            return null;
        }
        for (FloorBoss boss : ALL) {
            if (boss.entityId.equals(entityId)) {
                return boss;
            }
        }
        return null;
    }

    /** 这个实体是不是某一层的 BOSS。 */
    public static boolean isBoss(String entityId) {
        return byEntityId(entityId) != null;
    }

    /** 本层所有 BOSS 的名字（展示用）：{@code 旋风人 / 唤魔者 / 嗅探兽}。 */
    public static String describeFloor(int floor) {
        List<String> names = new ArrayList<>();
        for (FloorBoss boss : bossesOf(floor)) {
            names.add(boss.name());
        }
        return String.join(" / ", names);
    }

    /** 最后一层（通关判定用）。 */
    public static FloorBoss last() {
        return ALL.get(ALL.size() - 1);
    }
}
