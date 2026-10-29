package com.slaythecraft.client;

import java.util.HashMap;
import java.util.Map;

import com.slaythecraft.api.StcApi;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * 卡面**插画位**的绘制（v0.4.5）。
 *
 * <p>位置约定参照原版《杀戮尖塔》：插画在卡面**上半部分**，下半部分是名称/数值/描述。
 * 本类只负责"把 1 个矩形画出来"：有贴图就贴上，没有就画一个占位框（虚线感的边框 + 一个"图"字），
 * 所以装上贴图之前界面照样能用。</p>
 *
 * <p>贴图来源见 {@link StcApi#textureForCard(ResourceLocation)}：
 * 优先用 {@code StcApi.registerCardTexture} 登记的路径，否则用约定路径
 * {@code <命名空间>:textures/card/<卡牌路径>.png}。</p>
 */
public final class CardArt {

    /** 占位框底色 / 边框。 */
    private static final int SLOT_BG = 0xFF12121A;
    private static final int SLOT_EDGE = 0xFF3A3A46;

    /** 贴图是否存在的结果缓存（避免每帧都查资源管理器）。 */
    private static final Map<ResourceLocation, Boolean> PRESENT = new HashMap<>();
    private static long cacheStamp;
    private static final long CACHE_MS = 5000L;

    private CardArt() {
    }

    /**
     * 画一个插画位。
     *
     * @param cardId 卡牌 id（{@code stc:strike} 这种）
     */
    public static void draw(GuiGraphics graphics, ResourceLocation cardId, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, SLOT_BG);
        ResourceLocation texture = artId(cardId) == null ? null : StcApi.textureForCard(artId(cardId));
        boolean drawn = false;
        if (texture != null && present(texture)) {
            // 声明 256×256：这样无论图片实际多大，UV 0..1 都正好覆盖整张图
            graphics.blit(texture, x, y, 0.0F, 0.0F, w, h, 256, 256);
            drawn = true;
        }
        border(graphics, x, y, x + w, y + h, drawn ? 0xFF6A6A78 : SLOT_EDGE);
        if (!drawn && w >= 12 && h >= 10) {
            // 占位标记：一个小小的"图"字，提示这里是插画位
            String mark = "图";
            var font = Minecraft.getInstance().font;
            TextStyle.draw(graphics, font, mark, x + (w - font.width(mark)) / 2,
                    y + (h - 8) / 2, SLOT_EDGE);
        }
    }

    /** 这张卡有没有可用的插画贴图。 */
    public static boolean has(ResourceLocation cardId) {
        return cardId != null && present(StcApi.textureForCard(artId(cardId)));
    }

    /**
     * 插画用哪个 id（v0.5.1.4）。
     *
     * <p>【魔术技巧！】的每一档费用都是**独立定义**（{@code magic_trick_d1..d10}），但插画应该和
     * 基础版共用一张 —— 否则作者丢一张 {@code textures/card/magic_trick.png} 之后，
     * 减过费的复制品还是显示占位框。</p>
     */
    private static ResourceLocation artId(ResourceLocation cardId) {
        if (cardId == null) {
            return null;
        }
        String path = cardId.getPath();
        if (path.startsWith("magic_trick_d")) {
            return ResourceLocation.fromNamespaceAndPath(cardId.getNamespace(), "magic_trick");
        }
        return cardId;
    }

    /** 资源存在性检查（带 5 秒缓存；装资源包/换图后最多 5 秒生效）。 */
    private static boolean present(ResourceLocation texture) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - cacheStamp > CACHE_MS) {
            PRESENT.clear();
            cacheStamp = now;
        }
        return PRESENT.computeIfAbsent(texture,
                key -> minecraft.getResourceManager().getResource(key).isPresent());
    }

    private static void border(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        graphics.fill(x1, y1, x2, y1 + 1, color);
        graphics.fill(x1, y2 - 1, x2, y2, color);
        graphics.fill(x1, y1, x1 + 1, y2, color);
        graphics.fill(x2 - 1, y1, x2, y2, color);
    }
}
