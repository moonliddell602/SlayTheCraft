package com.slaythecraft.client;

import java.util.List;

import com.slaythecraft.SlayTheCraft;
import com.slaythecraft.api.StcApi;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * **卡牌收纳包界面的可自定义皮肤**（v0.4.6.5）。
 *
 * <p>需求："为编辑卡牌收纳包背景，按钮样式，人物头像在 assets 留下接口，允许我自定义修改和开发。"</p>
 *
 * <p>三个槽位，全部**约定路径 + 可选覆盖**，缺图时自动退回现在的程序化画法，
 * 所以"只放一张背景"或"只放三个按钮"都能用：</p>
 *
 * <table border="1">
 *   <caption>贴图约定</caption>
 *   <tr><th>槽位</th><th>约定路径（{@code assets/<命名空间>/…}）</th><th>画法</th></tr>
 *   <tr><td>书页背景</td><td>{@code stc:textures/gui/binder/background.png}</td>
 *       <td>整张贴图拉伸到面板（建议按默认 268×300 画）</td></tr>
 *   <tr><td>按钮</td><td>{@code …/binder/button.png}、{@code button_hover.png}、{@code button_disabled.png}</td>
 *       <td>九宫格（{@link NineSlice}，固定边 3 像素，贴图建议 ≥12×12）</td></tr>
 *   <tr><td>人物头像</td><td>{@code …/binder/avatar_<角色>.png}（如 {@code avatar_ironclad.png}），
 *       退而求其次 {@code …/binder/avatar.png}</td>
 *       <td>等比压进抬头那个 22×22 的方框</td></tr>
 * </table>
 *
 * <p>除了"丢文件"，也可以用 {@link StcApi} 在代码里指定（整合包 / 附加模组用）；
 * API 指定的优先于约定路径。全部缺省时保持 v0.4.6.5 之前的样子（程序化羊皮纸 + 程序化小脸）。</p>
 */
public final class BinderSkin {

    /** 皮肤目录（相对 {@code stc} 命名空间）。 */
    public static final String DIR = "textures/gui/binder/";

    /** 约定路径：背景。 */
    public static final ResourceLocation DEFAULT_BACKGROUND = stc(DIR + "background.png");
    /** 约定路径：按钮（正常 / 悬停 / 禁用）。 */
    public static final ResourceLocation DEFAULT_BUTTON = stc(DIR + "button.png");
    public static final ResourceLocation DEFAULT_BUTTON_HOVER = stc(DIR + "button_hover.png");
    public static final ResourceLocation DEFAULT_BUTTON_DISABLED = stc(DIR + "button_disabled.png");
    /** 约定路径：通用头像。 */
    public static final ResourceLocation DEFAULT_AVATAR = stc(DIR + "avatar.png");

    /** 按钮九宫格的固定边框（贴图角落保持 3 像素不缩放）。 */
    public static final int BUTTON_BORDER = 3;

    private BinderSkin() {
    }

    private static ResourceLocation stc(String path) {
        return ResourceLocation.fromNamespaceAndPath(SlayTheCraft.MODID, path);
    }

    // ------------------------------------------------------------------ 路径解析（纯逻辑，可自检）

    /** 约定路径：书页背景。 */
    public static ResourceLocation backgroundPath() {
        return DEFAULT_BACKGROUND;
    }

    /** 约定路径：按钮（按状态）。 */
    public static ResourceLocation buttonPath(boolean hovered, boolean disabled) {
        if (disabled) {
            return DEFAULT_BUTTON_DISABLED;
        }
        return hovered ? DEFAULT_BUTTON_HOVER : DEFAULT_BUTTON;
    }

    /**
     * 约定路径：角色头像（{@code avatar_<角色>.png}）；没选角色时返回 {@code null}。
     *
     * <p>纯路径计算，不查资源是否真的存在 —— 查存在性是 {@link #avatar(ResourceLocation)} 的事。</p>
     */
    public static ResourceLocation avatarPath(ResourceLocation character) {
        if (character == null) {
            return null;
        }
        return character.withPath(path -> DIR + "avatar_" + path + ".png");
    }

    /** 背景贴图：API 指定优先，其次约定路径。 */
    public static ResourceLocation background() {
        ResourceLocation override = StcApi.binderBackground().orElse(null);
        return override != null ? override : backgroundPath();
    }

    /** 按钮贴图（按状态解析：API → 约定路径，禁用 / 悬停缺图时退回正常态）。 */
    public static ResourceLocation button(boolean hovered, boolean disabled) {
        ResourceLocation normal = StcApi.binderButtonNormal().orElse(DEFAULT_BUTTON);
        ResourceLocation hover = StcApi.binderButtonHovered().orElse(DEFAULT_BUTTON_HOVER);
        ResourceLocation off = StcApi.binderButtonDisabled().orElse(DEFAULT_BUTTON_DISABLED);
        if (disabled) {
            return ClientAssets.present(off) ? off : normal;
        }
        if (hovered) {
            return ClientAssets.present(hover) ? hover : normal;
        }
        return normal;
    }

    /**
     * 人物头像贴图：角色专属（API → {@code avatar_<角色>.png}）→ 通用（API → {@code avatar.png}）。
     *
     * @param character 角色 id（{@code stc:ironclad} 之类）；{@code null} = 还没选角色
     * @return 找到的贴图；都没有则返回 {@code null}（调用方退回程序化头像）
     */
    public static ResourceLocation avatar(ResourceLocation character) {
        if (character != null) {
            ResourceLocation override = StcApi.binderAvatar(character).orElse(null);
            if (override != null) {
                return override;
            }
            ResourceLocation convention = avatarPath(character);
            if (ClientAssets.present(convention)) {
                return convention;
            }
        }
        ResourceLocation fallback = StcApi.binderDefaultAvatar().orElse(null);
        if (fallback != null) {
            return fallback;
        }
        return ClientAssets.present(DEFAULT_AVATAR) ? DEFAULT_AVATAR : null;
    }

    // ------------------------------------------------------------------ 绘制

    /**
     * 画书页背景。
     *
     * @return true = 用了自定义贴图；false = 调用方照旧画程序化羊皮纸
     */
    public static boolean drawBackground(GuiGraphics graphics, int x, int y, int width, int height) {
        ResourceLocation texture = background();
        if (!ClientAssets.present(texture)) {
            return false;
        }
        // 整图拉伸：作者按默认面板尺寸（268×300）画即可；窗口很小时会被轻微拉伸
        graphics.blit(texture, x, y, 0.0F, 0.0F, width, height, 256, 256);
        return true;
    }

    /**
     * 画一个按钮。
     *
     * @param fallback 缺图时调用的程序化画法（保持"只换一部分"也能用）
     */
    public static void drawButton(GuiGraphics graphics, Font font, int x, int y, int width, int height,
                                  String label, boolean enabled, boolean hovered, Runnable fallback) {
        ResourceLocation texture = button(hovered, !enabled);
        if (!ClientAssets.present(texture)) {
            fallback.run();
            return;
        }
        // 九宫格：按贴图**实际像素尺寸**切（读不到时按 12×12、边 3 保守处理）
        int[] size = ClientAssets.sizeOf(texture);
        int texW = size == null ? 12 : Math.max(1, size[0]);
        int texH = size == null ? 12 : Math.max(1, size[1]);
        for (NineSlice.Region region : NineSlice.regions(x, y, width, height, BUTTON_BORDER, texW, texH)) {
            graphics.blit(texture, region.dstX(), region.dstY(), region.dstW(), region.dstH(),
                    (float) region.srcX(), (float) region.srcY(), region.srcW(), region.srcH(),
                    texW, texH);
        }
        if (label != null && !label.isEmpty()) {
            TextStyle.draw(graphics, font, label, x + (width - font.width(label)) / 2, y + (height - 8) / 2,
                    enabled ? BinderStyle.INK : BinderStyle.INK_DIM);
        }
    }

    /**
     * 画人物头像。
     *
     * @return true = 用了自定义贴图；false = 调用方画程序化小脸
     */
    public static boolean drawAvatar(GuiGraphics graphics, ResourceLocation character, int x, int y, int size) {
        ResourceLocation texture = avatar(character);
        if (texture == null || !ClientAssets.present(texture)) {
            return false;
        }
        // v0.4.6.10 修：原来写死 texW=texH=256，而作者的头像是 88×88 —— 于是只被裁出
        // 22/256 的一小块（看起来"贴图没被使用"）。现在交给 IconFit：按**真实尺寸**等比缩放 +
        // 居中，任何尺寸都能填满这个方框；读不出尺寸（例如把 webp 改名成 png）就退回程序化小脸。
        if (ClientAssets.sizeOf(texture) == null) {
            return false;
        }
        IconFit.Fit fit = IconFit.draw(graphics, texture, x, y, size, size);
        return !fit.empty();
    }

    /** 当前鼠标是否压在某个矩形里（按钮悬停判定用；界面里画按钮时正好能拿到鼠标）。 */
    static boolean hovered(int x, int y, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null || minecraft.screen == null) {
            return false;
        }
        int mouseX = (int) minecraft.mouseHandler.xpos() * minecraft.getWindow().getGuiScaledWidth()
                / minecraft.getWindow().getWidth();
        int mouseY = (int) minecraft.mouseHandler.ypos() * minecraft.getWindow().getGuiScaledHeight()
                / minecraft.getWindow().getHeight();
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    /** 三个槽位是否都已备齐（给自检 / 调试用）。 */
    public static List<String> describe() {
        return List.of(
                "background=" + background() + (ClientAssets.present(background()) ? "（存在）" : "（缺省→程序化）"),
                "button=" + button(false, false) + (ClientAssets.present(button(false, false)) ? "（存在）" : "（缺省→程序化）"),
                "avatar=" + String.valueOf(avatar(null)));
    }
}
