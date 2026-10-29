package com.slaythecraft.client;

import com.slaythecraft.Config;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 界面文字的统一入口（v0.5.0）。
 *
 * <p>反馈原文："当前字体可能导致显示有重影，最好能直接使用不会重影的字体。" 重影的来源是
 * 原版 {@code GuiGraphics.drawString(...)} 默认会画**一层 1 像素的深色投影**（drop shadow）——
 * 中文小字号 + 我们的深色半透明面板上，那层投影看起来就是"重影"。</p>
 *
 * <p>所以模组自己的界面文字统一走这里：**不画投影**（配置项 {@code uiTextShadow} 可以改回来）。</p>
 */
public final class TextStyle {

    private TextStyle() {
    }

    public static void draw(GuiGraphics graphics, Font font, String text, int x, int y, int color) {
        if (text == null || text.isEmpty()) {
            return;
        }
        graphics.drawString(font, text, x, y, color, Config.uiTextShadow());
    }

    public static void draw(GuiGraphics graphics, Font font, Component text, int x, int y, int color) {
        if (text == null) {
            return;
        }
        graphics.drawString(font, text, x, y, color, Config.uiTextShadow());
    }

    /**
     * **等比缩放**地画一段文字（v0.5.0.15）。
     *
     * <p>需求（卡面版式图）："卡牌描述（应当适当缩放文本）"——卡面上的效果文案格子高度固定，
     * 而文案长短差别很大（"造成 6 点伤害。" vs "对所有敌人造成 8 点伤害并抽 2 张牌。"），
     * 硬画就会溢出格子。所以这里按需要缩到 {@code scale} 再画。</p>
     *
     * <p>缩放以**文字左上角**为基准，所以 {@code x/y} 仍然是"这块文字的左上角"，
     * 调用方不用为了居中再去反算坐标。</p>
     *
     * @param scale 缩放系数（1.0 = 原大；调用方负责夹到合理区间）
     */
    public static void drawScaled(GuiGraphics graphics, Font font, String text, int x, int y, float scale,
                                  int color) {
        if (text == null || text.isEmpty()) {
            return;
        }
        boolean scaled = Math.abs(scale - 1.0F) > 1.0E-3F;
        if (scaled) {
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0.0F);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.pose().translate(-x, -y, 0.0F);
        }
        graphics.drawString(font, text, x, y, color, Config.uiTextShadow());
        if (scaled) {
            graphics.pose().popPose();
        }
    }

    /**
     * 让一段文字**塞进固定大小的格子**所需的缩放系数（纯函数，便于自检）。
     *
     * <p>算法：先按格子宽度折行，再同时看"行数够不够"和"最宽那一行会不会横向溢出"，
     * 取两者里更小的那个需求；最后夹到 {@code [minScale, 1.0]}。</p>
     *
     * @param width      最宽一行的像素宽
     * @param lineCount  折行后的行数
     * @param boxW       格子宽
     * @param boxH       格子高
     * @param lineHeight 一行的高度
     * @return 缩放系数（永远 ≤ 1.0，即只缩小、不放大）
     */
    public static float fitScale(int width, int lineCount, int boxW, int boxH, int lineHeight) {
        if (width <= 0 || lineCount <= 0 || boxW <= 0 || boxH <= 0 || lineHeight <= 0) {
            return 1.0F;
        }
        float byWidth = width > boxW ? (float) boxW / width : 1.0F;
        float byHeight = lineCount * lineHeight > boxH ? (float) boxH / (lineCount * lineHeight) : 1.0F;
        return clampScale(Math.min(byWidth, byHeight));
    }

    /** 卡面文案允许的最小缩放（再小就看不清了，宁可让它按 {@code maxLines} 截断）。 */
    public static final float MIN_TEXT_SCALE = 0.6F;

    /** 把缩放系数夹到合法区间。 */
    public static float clampScale(float scale) {
        return Math.max(MIN_TEXT_SCALE, Math.min(1.0F, scale));
    }
}
