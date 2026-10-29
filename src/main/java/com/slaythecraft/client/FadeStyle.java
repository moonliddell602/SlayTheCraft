package com.slaythecraft.client;

import com.mojang.blaze3d.systems.RenderSystem;

/**
 * 淡入淡出绘制的公共助手（v0.3.0）。
 *
 * <p>MC 的 GUI 文字/方块绘制靠"全局着色器颜色"来叠加透明度：这里把它包成一个 try/finally，
 * 保证画完之后一定恢复成不透明，不会把后面的界面一起画淡。</p>
 */
public final class FadeStyle {

    private FadeStyle() {
    }

    /** 用给定的全局透明度画一段（0 = 完全透明，1 = 不透明）；画完立即恢复。 */
    public static void withAlpha(float alpha, Runnable body) {
        float clamped = Math.max(0.0F, Math.min(1.0F, alpha));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, clamped);
        try {
            body.run();
        } finally {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        }
    }
}
