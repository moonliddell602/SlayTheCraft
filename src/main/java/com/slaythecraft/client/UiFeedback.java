package com.slaythecraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * 界面反馈：**按钮音效**与**渐显动画**（v0.5.0.20）。
 *
 * <p>需求原文：</p>
 * <ul>
 *   <li>"战斗结算，卡牌领取页面添加动画：渐显。"</li>
 *   <li>"为每次点击按钮都添加音效：block.note_block.hat，音高（第一八度）F♯/G♭ - Fi/Se。"</li>
 * </ul>
 *
 * <h2>关于音高 0.5</h2>
 *
 * <p>原版音符盒的音高是"半音阶的倍数"：{@code F♯4 = 0.5}、{@code F♯5 = 1.0}
 * （即一个八度翻一倍）。需求说的"第一八度 F♯/G♭"对应这个音高体系里的 <b>F♯4 = 0.5</b>，
 * 所以这里用 {@code 0.5F}。</p>
 */
public final class UiFeedback {

    /** F♯4 的音高（"第一八度 F♯/G♭"）。 */
    public static final float PITCH_F_SHARP_4 = 0.5F;

    /** 按钮音效的默认音量（含 v0.5.2.5 的全局 ×1.25，与战斗音效同一套音量）。 */
    private static final float BUTTON_VOLUME = 0.7F * ClientBattleFx.SFX_VOLUME_SCALE;

    /** 渐显动画的时长（毫秒）。 */
    private static final long FADE_IN_MS = 220L;

    private UiFeedback() {
    }

    /** 点按钮的音效：{@code block.note_block.hat}，音高 F♯4。 */
    public static void click() {
        click(PITCH_F_SHARP_4);
    }

    /** 指定音高的按钮音效（音效不对时安静跳过，绝不因音频问题崩界面）。 */
    public static void click(float pitch) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getSoundManager() == null) {
            return;
        }
        SoundEvent sound = SoundEvents.NOTE_BLOCK_HAT.value();
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, BUTTON_VOLUME));
    }

    /**
     * 渐显用的透明度（0 = 刚打开、1 = 完全显示）。
     *
     * <p>纯函数，便于自检：{@code openedAt} 起算，{@link #FADE_IN_MS} 毫秒内线性到 1。</p>
     *
     * @param nowMillis 当前时间
     * @param openedAt  界面打开的时刻（0 或负数表示"不做渐显"）
     */
    public static float fadeAlpha(long nowMillis, long openedAt) {
        if (openedAt <= 0L) {
            return 1.0F;
        }
        long elapsed = nowMillis - openedAt;
        if (elapsed <= 0L) {
            return 0.0F;
        }
        if (elapsed >= FADE_IN_MS) {
            return 1.0F;
        }
        return (float) elapsed / FADE_IN_MS;
    }

    /** 渐显时长（自检用）。 */
    public static long fadeInMs() {
        return FADE_IN_MS;
    }
}
