package com.slaythecraft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * 小图标的**自适应缩放**（v0.4.6.7）。
 *
 * <p>需求原文："小图标如果出现大小错误的问题，自动将图像调整到合适大小。"</p>
 *
 * <p>以前画状态图标是写死的 {@code graphics.blit(贴图, x, y, 0, 0, 8, 8, 8, 8)}：只要作者换了一张
 * 不是 8×8 / 16×16 的贴图（例如自己画的 32×32 状态图标），画出来就会不对 —— 要么被压扁、
 * 要么比例走形。这里统一改成"先问 {@link ClientAssets#sizeOf} 要真实像素尺寸，再按**等比**
 * 缩放塞进目标方框、居中绘制"：</p>
 * <ul>
 *   <li>缩放系数 = {@code min(框宽 / 图宽, 框高 / 图高)}，所以**永远不会画出方框之外**；</li>
 *   <li>居中：多出来的边距两边平分；</li>
 *   <li>方框或贴图尺寸为 0 / 读不到时不会算出负数、也不会除零，最差只是不画。</li>
 * </ul>
 *
 * <p>纯数学部分单独抽成 {@link #fit(int, int, int, int, int, int)}，不需要 Minecraft 就能自检
 * （见 {@code com.slaythecraft.client.IconFitCheck}）。</p>
 */
public final class IconFit {

    /** 贴图尺寸读不到时的兜底边长（原版物品贴图基本都是 16×16）。 */
    public static final int FALLBACK_SIZE = 16;

    private IconFit() {
    }

    /**
     * 一条"缩放并居中之后"的绘制矩形（绝对坐标）。
     *
     * @param x 左边缘
     * @param y 上边缘
     * @param w 绘制宽度（可能为 0 = 不画）
     * @param h 绘制高度（可能为 0 = 不画）
     */
    public record Fit(int x, int y, int w, int h) {

        /** 这次绘制有没有实际面积。 */
        public boolean empty() {
            return w <= 0 || h <= 0;
        }
    }

    /**
     * 纯数学：把 {@code texW×texH} 的贴图等比缩放、居中放进 {@code (boxX, boxY, boxW, boxH)}。
     *
     * <p>约定：</p>
     * <ul>
     *   <li>方框宽或高 {@code <= 0} → 返回 0 尺寸（调用方不画，绝不产生负尺寸）；</li>
     *   <li>贴图宽或高 {@code <= 0}（尺寸未知）→ 当作"尺寸不明"，按整个方框拉伸（保持旧行为）；</li>
     *   <li>结果永远满足 {@code w <= boxW}、{@code h <= boxH}（"不放大到方框之外"）。</li>
     * </ul>
     */
    public static Fit fit(int boxX, int boxY, int boxW, int boxH, int texW, int texH) {
        if (boxW <= 0 || boxH <= 0) {
            return new Fit(boxX, boxY, 0, 0);
        }
        if (texW <= 0 || texH <= 0) {
            // 尺寸未知：没有比例可言，铺满方框（和旧版 blit 的表现一致）
            return new Fit(boxX, boxY, boxW, boxH);
        }
        int w;
        int h;
        // 用 long 比大小，避免超大贴图把 int 乘爆
        if ((long) boxW * texH <= (long) boxH * texW) {
            // 宽度受限：贴满宽度，高度按比例缩
            w = boxW;
            h = (int) Math.max(1L, Math.round((double) boxW * texH / texW));
        } else {
            // 高度受限：贴满高度，宽度按比例缩
            h = boxH;
            w = (int) Math.max(1L, Math.round((double) boxH * texW / texH));
        }
        // 四舍五入理论上不会越界，这里再夹一次，保证"绝不超出方框"
        w = Math.min(w, boxW);
        h = Math.min(h, boxH);
        int x = boxX + (boxW - w) / 2;
        int y = boxY + (boxH - h) / 2;
        return new Fit(x, y, w, h);
    }

    /**
     * 画一张自适应的小图标：贴图缺省按 {@value #FALLBACK_SIZE}×{@value #FALLBACK_SIZE} 估算尺寸。
     *
     * @return 实际画出去的矩形（供调用方排版 / 命中判定；没画时是 0 尺寸）
     */
    public static Fit draw(GuiGraphics graphics, ResourceLocation texture, int boxX, int boxY, int boxW, int boxH) {
        return draw(graphics, texture, boxX, boxY, boxW, boxH, FALLBACK_SIZE, FALLBACK_SIZE);
    }

    /**
     * 画一张自适应的小图标（可指定"尺寸读不到时"的兜底尺寸）。
     *
     * @param fallbackW 兜底宽（贴图缺失 / 读不出来 / 尺寸为 0 时用）
     * @param fallbackH 兜底高
     * @return 实际画出去的矩形（供调用方排版 / 命中判定；没画时是 0 尺寸）
     */
    public static Fit draw(GuiGraphics graphics, ResourceLocation texture, int boxX, int boxY, int boxW, int boxH,
                           int fallbackW, int fallbackH) {
        if (graphics == null || texture == null || boxW <= 0 || boxH <= 0) {
            return new Fit(boxX, boxY, 0, 0);
        }
        int texW = Math.max(1, fallbackW);
        int texH = Math.max(1, fallbackH);
        int[] size = ClientAssets.sizeOf(texture);
        if (size != null && size.length >= 2 && size[0] > 0 && size[1] > 0) {
            texW = size[0];
            texH = size[1];
        }
        Fit fit = fit(boxX, boxY, boxW, boxH, texW, texH);
        if (fit.empty()) {
            return fit;
        }
        // v0.4.6.8 修正：必须用 10 参 blit —— 8 参那版把"目标宽高"当成 UV 区域，
        // 于是"32×32 的目标 + 16×16 的贴图"会采样 0..2，贴图被平铺成 4 份（图鉴详情页的
        // "四个小图标"就是这个）。这里把 UV 区域固定成整图（texW×texH），目标尺寸另外给。
        graphics.blit(texture, fit.x(), fit.y(), fit.w(), fit.h(), 0.0F, 0.0F, texW, texH, texW, texH);
        return fit;
    }
}
