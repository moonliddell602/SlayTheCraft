package com.slaythecraft.combat.core;

/**
 * 战斗动作的**时间曲线**（v0.3.0）。
 *
 * <p>需求原文："打出攻击时玩家向前迅速移动一下再回到原位……使用防御的动画是快速蹲起一次……
 * 如果玩家受到伤害，则表现为受击、后退一步之后快速回到原位。敌怪执行攻击时也表现为和玩家
 * 一样的攻击方式。"</p>
 *
 * <p>三条曲线都是"0 → 1 → 0"：{@link #strike}（前冲 / 被击退）在 35% 处到达峰值，
 * {@link #crouch}（蹲起）在 45% 处到达峰值。数值只描述"位移/下蹲的比例"，
 * 具体秒数和距离由 Minecraft 层乘上 {@link #ATTACK_DISTANCE} 之类的常量。</p>
 */
public final class BattleAnim {

    /** 攻击前冲的时长。 */
    public static final long ATTACK_MS = 300L;
    /** 防御蹲起的时长。 */
    public static final long DEFEND_MS = 300L;
    /** 受击后退的时长。 */
    public static final long HURT_MS = 360L;
    /** 自爆的时长（v0.5.0.8：旋风人蓄能结算）。 */
    public static final long EXPLODE_MS = 500L;
    /**
     * 多段攻击时每段的动作要**快四分之一**（v0.5.0.5）。
     *
     * <p>需求原文："多段攻击时，每一段的攻击动画应该比单次攻击的动画速度快四分之一。"</p>
     *
     * <p>速度 ×1.25 ⇔ 时长 ÷1.25 = ×0.8。</p>
     */
    public static final double MULTI_HIT_SPEED = 1.25D;

    /** 多段攻击里**单段**的动画时长（= 普通攻击的 1/1.25）。 */
    public static long multiHitMs(long baseMs) {
        return Math.max(1L, Math.round(baseMs / MULTI_HIT_SPEED));
    }
    /** 前冲距离（方块）。 */
    public static final double ATTACK_DISTANCE = 0.55D;
    /** 受击后退距离（方块）。 */
    public static final double HURT_DISTANCE = 0.45D;
    /** 蹲下深度（方块）。 */
    public static final double CROUCH_DEPTH = 0.30D;
    /**
     * 敌人**依次行动**时每只之间的间隔（v0.5.0 按反馈从 0.6 秒延长到 **1.2 秒**）。
     *
     * <p>游戏内由 {@code CombatManager} 换算成 tick（20 tick = 1 秒）来推进分步敌人回合。</p>
     */
    public static final long ENEMY_STEP_MS = 1200L;

    private BattleAnim() {
    }

    /** 动画是否已经结束。 */
    public static boolean finished(long nowMillis, long startedAt, long duration) {
        return duration <= 0L || nowMillis - startedAt >= duration;
    }

    /** 进度（0..1，已夹紧）。 */
    public static double progress(long nowMillis, long startedAt, long duration) {
        if (duration <= 0L) {
            return 1.0D;
        }
        double t = (double) (nowMillis - startedAt) / duration;
        return Math.max(0.0D, Math.min(1.0D, t));
    }

    /** 攻击前冲：快速冲出去，再回到原位。 */
    public static float strike(long nowMillis, long startedAt) {
        return curve(nowMillis, startedAt, ATTACK_MS, 0.35D);
    }

    /** 攻击前冲（可指定时长：多段攻击的每一段用更短的时长 = 更快）。 */
    public static float strike(long nowMillis, long startedAt, long duration) {
        return curve(nowMillis, startedAt, duration, 0.35D);
    }

    /** 受击后退：快速退一步，再快速回到原位。 */
    public static float knockback(long nowMillis, long startedAt) {
        return curve(nowMillis, startedAt, HURT_MS, 0.30D);
    }

    /** 受击后退（可指定时长：多段攻击的每一段用更短的时长 = 更快）。 */
    public static float knockback(long nowMillis, long startedAt, long duration) {
        return curve(nowMillis, startedAt, duration, 0.30D);
    }

    /** 蹲起：快速蹲下再站起。 */
    public static float crouch(long nowMillis, long startedAt) {
        return curve(nowMillis, startedAt, DEFEND_MS, 0.45D);
    }

    /**
     * 通用的"0 → 1 → 0"曲线。
     *
     * @param peakAt 到达峰值的进度（0..1）
     */
    public static float curve(long nowMillis, long startedAt, long duration, double peakAt) {
        double t = progress(nowMillis, startedAt, duration);
        if (t <= 0.0D || t >= 1.0D) {
            return 0.0F;
        }
        double peak = Math.max(0.05D, Math.min(0.95D, peakAt));
        if (t < peak) {
            return (float) easeOut(t / peak);
        }
        return (float) (1.0D - easeIn((t - peak) / (1.0D - peak)));
    }

    /** 缓出：开头快、结尾慢。 */
    public static double easeOut(double t) {
        double clamped = Math.max(0.0D, Math.min(1.0D, t));
        return 1.0D - Math.pow(1.0D - clamped, 2.0D);
    }

    /** 缓入：开头慢、结尾快（用在"回原位"的那一段，看起来像快速收回）。 */
    public static double easeIn(double t) {
        double clamped = Math.max(0.0D, Math.min(1.0D, t));
        return clamped * clamped;
    }
}
