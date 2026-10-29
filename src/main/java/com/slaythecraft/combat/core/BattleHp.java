package com.slaythecraft.combat.core;

import java.util.List;

/**
 * **尖塔战斗内的生命值覆盖表**（v0.4.6.2）。
 *
 * <p>需求里不断出现"其尖塔玩法内战斗生命值调整为 X，不调整其尖塔玩法局外的原版生命值"：
 * BOSS 的覆盖值写在 {@link FloorBoss} 里（因为它同时还决定"哪一层有哪些 BOSS"），
 * 而**非 BOSS** 的个别敌人（例如堡垒守卫的猪灵蛮兵 70）记在这张表里。</p>
 *
 * <p>两者的优先级：BOSS 表优先，其次这张表，都没有就按"原版生命 × 配置倍率"缩放。</p>
 */
public final class BattleHp {

    /**
     * 一件覆盖记录。
     *
     * @param entityId 原版实体 id
     * @param name     展示名
     * @param battleHp 尖塔战斗内的生命值（原版实体的血量上限完全不动）
     */
    public record Override(String entityId, String name, int battleHp) {
    }

    /** 非 BOSS 的尖塔战斗生命值覆盖。 */
    public static final List<Override> ALL = List.of(
            // 需求（v0.4.6.2）："将猪灵蛮兵移动到【堡垒守卫】，其尖塔玩法内战斗生命值调整为：70"
            new Override("minecraft:piglin_brute", "猪灵蛮兵", 70));

    private BattleHp() {
    }

    /** 按实体 id 找覆盖记录（没有返回 {@code null}）。 */
    public static Override byEntityId(String entityId) {
        if (entityId == null) {
            return null;
        }
        for (Override override : ALL) {
            if (override.entityId().equals(entityId)) {
                return override;
            }
        }
        return null;
    }

    /**
     * 这个实体在尖塔战斗里的生命值覆盖（0 = 没有覆盖，按默认缩放走）。
     *
     * <p>先看 BOSS 表，再看上面的非 BOSS 覆盖表。</p>
     */
    public static int overrideFor(String entityId) {
        FloorBoss boss = FloorBoss.byEntityId(entityId);
        if (boss != null && boss.battleHp() > 0) {
            return boss.battleHp();
        }
        Override override = byEntityId(entityId);
        return override == null ? 0 : override.battleHp();
    }

    /** 有没有为这个实体指定过覆盖值。 */
    public static boolean hasOverride(String entityId) {
        return overrideFor(entityId) > 0;
    }
}
