package com.slaythecraft.client;

import java.util.ArrayList;
import java.util.List;

/**
 * **九宫格（9-slice）几何**（v0.4.6.5）。
 *
 * <p>收纳包界面的按钮是可自定义贴图的（见 {@link BinderSkin}），而按钮宽度各不相同
 * （52、96、118、140…），所以贴图必须按九宫格绘制：四个角保持原尺寸，四条边与中心拉伸。</p>
 *
 * <p>刻意做成**纯计算**（不碰 {@code GuiGraphics}），这样九宫格的正确性可以在无头自检里逐条钉住
 * （覆盖整个矩形、互不重叠、角落不缩放、矩形太小时按比例收缩边框而不是画出负尺寸）。</p>
 *
 * <pre>
 *   ┌───┬─────────┬───┐
 *   │ 1 │    2    │ 3 │   b = border
 *   ├───┼─────────┼───┤
 *   │ 4 │    5    │ 6 │   1/3/7/9 = 固定角
 *   ├───┼─────────┼───┤   2/8 = 上下边（横向拉伸）
 *   │ 7 │    8    │ 9 │   4/6 = 左右边（纵向拉伸）
 *   └───┴─────────┴───┘   5     = 中心（双向拉伸）
 * </pre>
 */
public final class NineSlice {

    /**
     * 一块绘制区域：从贴图的 {@code (srcX, srcY, srcW, srcH)} 画到屏幕的
     * {@code (dstX, dstY, dstW, dstH)}。
     */
    public record Region(int srcX, int srcY, int srcW, int srcH, int dstX, int dstY, int dstW, int dstH) {

        /** 屏幕上的面积（自检用）。 */
        public int area() {
            return dstW * dstH;
        }
    }

    private NineSlice() {
    }

    /**
     * 把一张 {@code texW × texH} 的贴图按九宫格铺到 {@code (dstX, dstY, dstW, dstH)}。
     *
     * <p>边框宽度会被**自动收缩**，保证：</p>
     * <ul>
     *   <li>两边之和不超过贴图尺寸的一半（{@code border <= texW/2}、{@code border <= texH/2}）；</li>
     *   <li>两边之和不超过目标矩形的一半（否则会画出负宽度的中间格）。</li>
     * </ul>
     *
     * <p>返回的每一块都是**非零尺寸**的；目标矩形小于 2×2 时退化成"整图拉伸到整个矩形"。</p>
     */
    public static List<Region> regions(int dstX, int dstY, int dstW, int dstH, int border, int texW, int texH) {
        if (dstW <= 0 || dstH <= 0 || texW <= 0 || texH <= 0) {
            return List.of();
        }
        // 退化情况：矩形太小，九宫格没有意义 → 整图拉伸
        if (dstW < 2 || dstH < 2) {
            return List.of(new Region(0, 0, texW, texH, dstX, dstY, dstW, dstH));
        }
        int b = Math.max(1, border);
        b = Math.min(b, Math.min(texW / 2, texH / 2));
        b = Math.min(b, Math.min(dstW / 2, dstH / 2));
        b = Math.max(0, b);
        if (b == 0) {
            return List.of(new Region(0, 0, texW, texH, dstX, dstY, dstW, dstH));
        }

        // 三列 / 三行的起点与长度（贴图侧与屏幕侧各一份）
        int[] srcCols = {0, b, texW - b};
        int[] srcColW = {b, texW - 2 * b, b};
        int[] dstCols = {dstX, dstX + b, dstX + dstW - b};
        int[] dstColW = {b, dstW - 2 * b, b};
        int[] srcRows = {0, b, texH - b};
        int[] srcRowH = {b, texH - 2 * b, b};
        int[] dstRows = {dstY, dstY + b, dstY + dstH - b};
        int[] dstRowH = {b, dstH - 2 * b, b};

        List<Region> regions = new ArrayList<>(9);
        for (int row = 0; row < 3; row++) {
            if (srcRowH[row] <= 0 || dstRowH[row] <= 0) {
                continue;
            }
            for (int col = 0; col < 3; col++) {
                if (srcColW[col] <= 0 || dstColW[col] <= 0) {
                    continue;
                }
                regions.add(new Region(srcCols[col], srcRows[row], srcColW[col], srcRowH[row],
                        dstCols[col], dstRows[row], dstColW[col], dstRowH[row]));
            }
        }
        return List.copyOf(regions);
    }
}
