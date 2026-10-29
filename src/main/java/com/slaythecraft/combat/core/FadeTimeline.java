package com.slaythecraft.combat.core;

/**
 * 淡入/淡出时间线（v0.3.0）：手牌淡入与屏幕中央横幅共用这一套数学。
 *
 * <p>需求原文："让每张牌以淡入的方式进入手牌"、"在屏幕中心渐显展示'战斗开始'，并且渐隐，
 * 整个动画持续 1.5 秒"、"在每个回合开始时，在屏幕中心展示'第 X 回合'"。</p>
 *
 * <p>v0.4.6.3 起"战斗开始 / 第 X 回合"由服务端改发**原版 /title 大标题**
 * （{@code BattleTitles} 的 6/18/6 与 6/12/6 tick），下面两个常量就是那两组时长的换算依据，
 * 保留下来既作为需求留档，也继续被引擎自检盯住。</p>
 */
public final class FadeTimeline {

    /** "战斗开始"大标题的时长（需求：整个动画 1.5 秒 = 30 tick）。 */
    public static final long BATTLE_START_MS = 1500L;
    /** "第 X 回合"大标题的时长（1.2 秒 = 24 tick）。 */
    public static final long TURN_BANNER_MS = 1200L;
    /** 横幅淡入/淡出各占的时长。 */
    public static final long BANNER_FADE_MS = 300L;
    /** 单张手牌淡入的时长。 */
    public static final long CARD_FADE_MS = 260L;

    private FadeTimeline() {
    }

    /** 是否已经播完。 */
    public static boolean finished(long nowMillis, long startedAt, long duration) {
        return duration <= 0L || nowMillis - startedAt >= duration;
    }

    /** 只淡入：0 → 1（结束后恒为 1）。 */
    public static float fadeIn(long nowMillis, long startedAt, long duration) {
        if (duration <= 0L) {
            return 1.0F;
        }
        double t = (double) (nowMillis - startedAt) / duration;
        return (float) ease(Math.max(0.0D, Math.min(1.0D, t)));
    }

    /**
     * 淡入 → 保持 → 淡出：返回 0..1 的不透明度（结束后为 0）。
     *
     * @param fadeMillis 淡入与淡出各自占用的时长
     */
    public static float fadeInOut(long nowMillis, long startedAt, long duration, long fadeMillis) {
        long elapsed = nowMillis - startedAt;
        if (elapsed <= 0L || duration <= 0L || elapsed >= duration) {
            return 0.0F;
        }
        long fade = Math.max(1L, Math.min(fadeMillis, duration / 2L));
        if (elapsed < fade) {
            return (float) ease((double) elapsed / fade);
        }
        if (elapsed > duration - fade) {
            return (float) ease((double) (duration - elapsed) / fade);
        }
        return 1.0F;
    }

    /** 平滑曲线（smoothstep）。 */
    public static double ease(double t) {
        double clamped = Math.max(0.0D, Math.min(1.0D, t));
        return clamped * clamped * (3.0D - 2.0D * clamped);
    }
}
