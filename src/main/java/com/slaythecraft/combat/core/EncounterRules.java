package com.slaythecraft.combat.core;

/**
 * 遭遇战编成规则（v0.0.2 测试反馈修正）。
 *
 * <p>反馈原文：</p>
 * <ul>
 *   <li>"如果是精英战斗，应该最多只能有一个敌人（即精英敌人）……boss 战也是如此"</li>
 *   <li>"周围最好只加入 3 个敌人进入战斗"</li>
 * </ul>
 *
 * <p>于是规则变成：</p>
 * <ol>
 *   <li>**离你最近的那只怪物决定战斗类型**（精英 / BOSS / 普通）；</li>
 *   <li>一场战斗**只收同一档**的怪：精英战不会再夹带小怪，普通战也不会突然卷进一只精英；</li>
 *   <li>精英 / BOSS 战默认 **1 只**（{@code eliteEncounterSize}）；普通战默认 **3 只**（{@code maxEnemies}）；</li>
 *   <li>唯一例外是需求点名的"猪灵蛮兵小队"（2 只，{@code eliteSquad} 可关掉）。</li>
 * </ol>
 *
 * <p>纯逻辑、零 Minecraft 依赖，因此这些数字都在引擎自检里钉死（见 {@code CombatSelfTest}）。</p>
 */
public final class EncounterRules {

    private EncounterRules() {
    }

    /** 最近的那只怪物决定战斗类型。 */
    public static EventKind eventKind(MobTier nearestTier) {
        return switch (nearestTier) {
            case BOSS -> EventKind.BOSS;
            case ELITE -> EventKind.ELITE;
            default -> EventKind.NORMAL;
        };
    }

    /** 这一档的怪能不能加入这场战斗（精英战只收精英、BOSS 战只收 BOSS、普通战只收普通）。 */
    public static boolean joinsEncounter(EventKind kind, MobTier tier) {
        return switch (kind) {
            case BOSS -> tier == MobTier.BOSS;
            case ELITE -> tier == MobTier.ELITE;
            default -> tier == MobTier.NORMAL;
        };
    }

    /**
     * 本场战斗最多卷入几只。
     *
     * @param kind             战斗类型
     * @param maxEnemies       普通战上限（配置 {@code maxEnemies}，默认 3）
     * @param eliteEncounterSize 精英 / BOSS 战的数量（配置，默认 1）
     * @param leaderSquadSize  最近那只怪的"小队规模"（猪灵蛮兵 = 2，其余 = 1）
     * @param allowSquad       是否允许小队例外（配置 {@code eliteSquad}）
     */
    public static int encounterSize(EventKind kind, int maxEnemies, int eliteEncounterSize, int leaderSquadSize,
                                    boolean allowSquad) {
        int cap = Math.max(1, maxEnemies);
        int limit = switch (kind) {
            case BOSS, ELITE -> Math.max(1, eliteEncounterSize);
            default -> cap;
        };
        if (kind != EventKind.NORMAL && allowSquad && leaderSquadSize > 1) {
            // 小队例外：让"猪灵蛮兵小队(2 只)"仍然成立，但不会顺手夹带别的怪
            limit = Math.max(limit, leaderSquadSize);
        }
        return Math.max(1, Math.min(limit, cap));
    }
}
