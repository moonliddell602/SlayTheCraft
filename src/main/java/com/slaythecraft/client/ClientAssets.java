package com.slaythecraft.client;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端**可选贴图**的存在性查询（v0.4.6.5）。
 *
 * <p>模组的界面默认全是程序化绘制（画矩形 + 原版字体），但允许作者丢一张贴图进来就替换掉
 * （收纳包背景 / 按钮 / 头像，见 {@link BinderSkin}；卡牌插画见 {@link CardArt}）。
 * 判断"这张图在不在"要查资源管理器，所以这里统一做一层缓存。</p>
 *
 * <p>缓存 5 秒过期：装资源包、换贴图之后最多 5 秒生效，不用重启。</p>
 */
public final class ClientAssets {

    private static final long CACHE_MS = 5000L;
    private static final Map<ResourceLocation, Boolean> PRESENT = new HashMap<>();
    /** 贴图像素尺寸缓存（{@code null} 值表示"读不到"）。 */
    private static final Map<ResourceLocation, int[]> SIZE = new HashMap<>();
    private static long stamp;

    private ClientAssets() {
    }

    /** 这张贴图在客户端资源里存在吗（带 5 秒缓存）。 */
    public static boolean present(ResourceLocation texture) {
        if (texture == null) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - stamp > CACHE_MS) {
            PRESENT.clear();
            SIZE.clear();
            stamp = now;
        }
        return PRESENT.computeIfAbsent(texture,
                key -> minecraft.getResourceManager().getResource(key).isPresent());
    }

    /** 清掉缓存（例如资源重载之后想立刻生效）。 */
    public static void invalidate() {
        PRESENT.clear();
        SIZE.clear();
        stamp = 0L;
    }

    /**
     * 贴图的**像素尺寸**（{@code {宽, 高}}）；读不到返回 {@code null}。
     *
     * <p>九宫格绘制需要知道原图多大（否则 UV 会算错），而界面绘制每帧都会调用，
     * 所以这里只在第一次（以及缓存过期后）真正解码一次 PNG。</p>
     */
    public static int[] sizeOf(ResourceLocation texture) {
        if (texture == null || !present(texture)) {
            return null;
        }
        return SIZE.computeIfAbsent(texture, key -> {
            Minecraft minecraft = Minecraft.getInstance();
            // 注意：Resource 本身不是 AutoCloseable（只有它 open() 出来的流是）
            net.minecraft.server.packs.resources.Resource resource =
                    minecraft.getResourceManager().getResource(key).orElse(null);
            if (resource == null) {
                return null;
            }
            try (var stream = resource.open()) {
                com.mojang.blaze3d.platform.NativeImage image =
                        com.mojang.blaze3d.platform.NativeImage.read(stream);
                int[] size = {image.getWidth(), image.getHeight()};
                image.close();
                return size;
            } catch (java.io.IOException | RuntimeException e) {
                return null;
            }
        });
    }
}
