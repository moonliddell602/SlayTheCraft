package com.slaythecraft.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.slaythecraft.api.StcApi;

import net.minecraft.resources.ResourceLocation;

/**
 * 收纳包皮肤接口的自检（v0.4.6.5）。
 *
 * <p>需求："为编辑卡牌收纳包背景，按钮样式，人物头像在 assets 留下接口，允许我自定义修改和开发。"</p>
 *
 * <p>这里盯三件事：</p>
 * <ol>
 *   <li><b>约定路径</b>：三个槽位各自应该查哪个文件（作者照着放文件就能生效）；</li>
 *   <li><b>API 优先</b>：{@link StcApi} 登记过的贴图要盖过约定路径，清空后回到约定路径；</li>
 *   <li><b>九宫格几何</b>（{@link NineSlice}）：铺满目标矩形、角落不缩放、矩形过小时收缩边框而不是画出负尺寸；</li>
 *   <li><b>接线没断</b>：{@code BinderStyle} 真的把背景 / 按钮 / 头像交给了 {@code BinderSkin}
 *       （否则接口"写好了但没接上"，界面上永远看不到效果）。</li>
 * </ol>
 *
 * <pre>
 * java -cp "build\classes\java\main;build\classes\java\test;&lt;NeoForm classpath&gt;" com.slaythecraft.client.BinderSkinCheck
 * </pre>
 */
public final class BinderSkinCheck {

    private BinderSkinCheck() {
    }

    public static void main(String[] args) {
        List<String> lines = new ArrayList<>();
        int[] counters = {0, 0};

        StcApi.clearBinderSkin();

        // ---------------------------------------------------------- 1. 约定路径
        check(lines, counters, "背景约定路径 = stc:textures/gui/binder/background.png",
                BinderSkin.backgroundPath().equals(id("textures/gui/binder/background.png")), "约定路径",
                BinderSkin.backgroundPath());
        check(lines, counters, "按钮正常态 = …/binder/button.png",
                BinderSkin.buttonPath(false, false).equals(id("textures/gui/binder/button.png")), "约定路径",
                BinderSkin.buttonPath(false, false));
        check(lines, counters, "按钮悬停态 = …/binder/button_hover.png",
                BinderSkin.buttonPath(true, false).equals(id("textures/gui/binder/button_hover.png")), "约定路径",
                BinderSkin.buttonPath(true, false));
        check(lines, counters, "按钮禁用态 = …/binder/button_disabled.png",
                BinderSkin.buttonPath(false, true).equals(id("textures/gui/binder/button_disabled.png")), "约定路径",
                BinderSkin.buttonPath(false, true));
        check(lines, counters, "头像约定路径 = …/binder/avatar_<角色>.png",
                BinderSkin.avatarPath(id("ironclad")).equals(id("textures/gui/binder/avatar_ironclad.png")),
                "avatar_ironclad.png", BinderSkin.avatarPath(id("ironclad")));
        check(lines, counters, "头像约定路径：四个角色各不相同",
                new HashSet<>(List.of(BinderSkin.avatarPath(id("ironclad")),
                        BinderSkin.avatarPath(id("silent")), BinderSkin.avatarPath(id("defect")),
                        BinderSkin.avatarPath(id("pillager")))).size() == 4, 4, 4);
        check(lines, counters, "没选角色时没有头像约定路径", BinderSkin.avatarPath(null) == null, null,
                BinderSkin.avatarPath(null));
        check(lines, counters, "通用头像约定路径 = …/binder/avatar.png",
                BinderSkin.DEFAULT_AVATAR.equals(id("textures/gui/binder/avatar.png")), "avatar.png",
                BinderSkin.DEFAULT_AVATAR);

        // ---------------------------------------------------------- 2. API 优先
        check(lines, counters, "没登记时背景就是约定路径",
                BinderSkin.background().equals(BinderSkin.backgroundPath()), "约定路径", BinderSkin.background());

        ResourceLocation custom = StcApi.id("mypack:gui/my_book");
        StcApi.registerBinderBackground(custom);
        check(lines, counters, "登记后背景用登记的贴图", BinderSkin.background().equals(custom), custom,
                BinderSkin.background());
        StcApi.registerBinderBackground(null);
        check(lines, counters, "登记 null 恢复约定路径",
                BinderSkin.background().equals(BinderSkin.backgroundPath()), "约定路径", BinderSkin.background());

        StcApi.registerBinderButtons(StcApi.id("mypack:gui/btn"), StcApi.id("mypack:gui/btn_hover"),
                StcApi.id("mypack:gui/btn_off"));
        check(lines, counters, "按钮三态都能被 API 覆盖",
                StcApi.binderButtonNormal().orElseThrow().equals(StcApi.id("mypack:gui/btn"))
                        && StcApi.binderButtonHovered().orElseThrow().equals(StcApi.id("mypack:gui/btn_hover"))
                        && StcApi.binderButtonDisabled().orElseThrow().equals(StcApi.id("mypack:gui/btn_off")),
                true, false);
        StcApi.registerBinderButtons(null, null, null);

        StcApi.registerBinderAvatar(id("ironclad"), StcApi.id("mypack:gui/me"));
        check(lines, counters, "角色头像能被 API 覆盖（优先于约定路径）",
                BinderSkin.avatar(id("ironclad")).equals(StcApi.id("mypack:gui/me")), "mypack:gui/me",
                BinderSkin.avatar(id("ironclad")));
        check(lines, counters, "只登记一个角色时，其他角色不受影响",
                BinderSkin.avatar(id("silent")) == null, null, BinderSkin.avatar(id("silent")));
        StcApi.registerBinderDefaultAvatar(StcApi.id("mypack:gui/default_face"));
        check(lines, counters, "通用头像能兜住所有角色",
                BinderSkin.avatar(id("silent")).equals(StcApi.id("mypack:gui/default_face")),
                "mypack:gui/default_face", BinderSkin.avatar(id("silent")));
        check(lines, counters, "登记计数正确", StcApi.binderAvatarCount() == 1, 1, StcApi.binderAvatarCount());
        StcApi.clearBinderSkin();
        check(lines, counters, "清空之后又回到约定路径",
                BinderSkin.background().equals(BinderSkin.backgroundPath())
                        && StcApi.binderAvatarCount() == 0,
                "约定路径 / 0", BinderSkin.background() + " / " + StcApi.binderAvatarCount());

        // ---------------------------------------------------------- 3. 九宫格几何
        nineSlice(lines, counters);

        // ---------------------------------------------------------- 4. 接线没断
        String style = read("client/BinderStyle.java", lines, counters);
        if (style != null) {
            check(lines, counters, "BinderStyle 的背景交给 BinderSkin",
                    style.contains("BinderSkin.drawBackground("), true, false);
            check(lines, counters, "BinderStyle 的按钮交给 BinderSkin",
                    style.contains("BinderSkin.drawButton("), true, false);
            check(lines, counters, "BinderStyle 的头像交给 BinderSkin",
                    style.contains("BinderSkin.drawAvatar("), true, false);
            check(lines, counters, "BinderStyle 自己判断按钮悬停（调用点不用改）",
                    style.contains("BinderSkin.hovered("), true, false);
            check(lines, counters, "缺贴图时有程序化兜底画法",
                    style.contains("proceduralButton(") && style.contains("BOOK_PAGE"), true, false);
            check(lines, counters, "背景接口带面板尺寸（窗口小时不再和面板错位）",
                    style.contains("book(GuiGraphics graphics, int leftPos, int topPos, int width, int height)"),
                    true, false);
        }
        String skin = read("client/BinderSkin.java", lines, counters);
        if (skin != null) {
            check(lines, counters, "BinderSkin 用真实贴图尺寸做九宫格",
                    skin.contains("ClientAssets.sizeOf("), true, false);
            check(lines, counters, "BinderSkin 缺图时返回 false 让调用方兜底",
                    skin.contains("return false;"), true, false);
        }
        String assets = read("client/ClientAssets.java", lines, counters);
        if (assets != null) {
            check(lines, counters, "ClientAssets 带 5 秒缓存（换资源包不用重启）",
                    assets.contains("CACHE_MS"), true, false);
        }

        // ---------------------------------------------------------- v0.4.6.10：人物小图标真的被用上
        String skinSource = read("client/BinderSkin.java", lines, counters);
        String gateSource = read("client/SpireGateScreen.java", lines, counters);
        String detailSource = read("client/CharacterDetailScreen.java", lines, counters);
        if (skinSource != null) {
            check(lines, counters, "头像不再写死 256×256 的 UV（88×88 的图不再被裁掉）",
                    !skinSource.contains("size, size, 256, 256"), true, "仍写死 256");
            check(lines, counters, "头像按真实贴图尺寸等比缩放（走 IconFit）",
                    skinSource.contains("IconFit.draw(graphics, texture, x, y, size, size)"), true, false);
            check(lines, counters, "读不出尺寸的\"假 png\"（例如 webp 改名）退回程序化小脸",
                    skinSource.contains("ClientAssets.sizeOf(texture) == null"), true, false);
        }
        if (gateSource != null) {
            check(lines, counters, "人物选取界面用上了作者的头像（BinderSkin.drawAvatar）",
                    gateSource.contains("BinderSkin.drawAvatar(graphics, profile.id()"), true, false);
        }
        if (detailSource != null) {
            check(lines, counters, "角色详情页优先用作者头像，没有才画程序化头像",
                    detailSource.contains("if (!BinderSkin.drawAvatar(graphics, profile.id()"), true, false);
        }
        lines.forEach(System.out::println);
        String summary = (counters[1] == 0 ? "全部通过" : "存在失败") + "：" + counters[0] + " 通过 / " + counters[1]
                + " 失败";
        System.out.println(summary);
        if (counters[1] > 0) {
            System.exit(1);
        }
    }

    /** 九宫格几何：铺满、不重叠、角落不缩放、极端尺寸不炸。 */
    private static void nineSlice(List<String> lines, int[] counters) {
        int x = 10;
        int y = 20;
        int w = 100;
        int h = 14;
        int border = 3;
        int texW = 12;
        int texH = 12;
        List<NineSlice.Region> regions = NineSlice.regions(x, y, w, h, border, texW, texH);
        check(lines, counters, "正常矩形切出 9 块", regions.size() == 9, 9, regions.size());

        int area = 0;
        Set<String> cells = new HashSet<>();
        boolean overlap = false;
        boolean outOfBounds = false;
        for (NineSlice.Region region : regions) {
            area += region.area();
            outOfBounds |= region.dstX() < x || region.dstY() < y || region.dstX() + region.dstW() > x + w
                    || region.dstY() + region.dstH() > y + h;
            for (int px = region.dstX(); px < region.dstX() + region.dstW(); px++) {
                for (int py = region.dstY(); py < region.dstY() + region.dstH(); py++) {
                    if (!cells.add(px + "," + py)) {
                        overlap = true;
                    }
                }
            }
        }
        check(lines, counters, "九块面积之和 = 目标矩形面积（铺满且不重叠）", area == w * h, w * h, area);
        check(lines, counters, "没有任何字节重叠", !overlap, false, true);
        check(lines, counters, "没有画到矩形外面", !outOfBounds, false, true);

        // 角落保持原尺寸
        NineSlice.Region topLeft = regions.get(0);
        check(lines, counters, "左上角不缩放（src 与 dst 都是 border 大小）",
                topLeft.srcW() == border && topLeft.srcH() == border && topLeft.dstW() == border
                        && topLeft.dstH() == border,
                border, topLeft.srcW() + "x" + topLeft.srcH() + " -> " + topLeft.dstW() + "x" + topLeft.dstH());
        boolean anyScaledCorner = false;
        for (NineSlice.Region region : regions) {
            boolean isCorner = (region.srcX() == 0 || region.srcX() == texW - border)
                    && (region.srcY() == 0 || region.srcY() == texH - border);
            if (isCorner && (region.srcW() != region.dstW() || region.srcH() != region.dstH())) {
                anyScaledCorner = true;
            }
        }
        check(lines, counters, "四个角都没有被缩放", !anyScaledCorner, false, true);

        // 中间列/行被拉伸
        boolean stretchedMiddle = false;
        for (NineSlice.Region region : regions) {
            if (region.srcW() == texW - 2 * border && region.dstW() == w - 2 * border) {
                stretchedMiddle = true;
            }
        }
        check(lines, counters, "中间一列被拉伸到矩形中段", stretchedMiddle, true, false);

        // 极端尺寸：矩形比两倍边框还小 → 边框自动收缩，仍然铺满
        List<NineSlice.Region> tiny = NineSlice.regions(0, 0, 4, 4, 3, 12, 12);
        int tinyArea = 0;
        for (NineSlice.Region region : tiny) {
            tinyArea += region.area();
        }
        check(lines, counters, "小矩形（4×4）时边框收缩到 2，仍然铺满", tinyArea == 16, 16, tinyArea);
        boolean nonNegative = true;
        for (NineSlice.Region region : tiny) {
            nonNegative &= region.srcW() > 0 && region.srcH() > 0 && region.dstW() > 0 && region.dstH() > 0
                    && region.srcW() <= 12 && region.srcH() <= 12;
        }
        check(lines, counters, "小矩形下没有零/负尺寸的块，也没有越出贴图", nonNegative, true, false);

        // 退化：1×1 用整图拉伸
        List<NineSlice.Region> degenerate = NineSlice.regions(0, 0, 1, 1, 3, 12, 12);
        check(lines, counters, "1×1 的矩形退化成整图拉伸", degenerate.size() == 1
                && degenerate.get(0).dstW() == 1 && degenerate.get(0).dstH() == 1, 1, degenerate.size());

        // 非法输入
        check(lines, counters, "宽度为 0 时返回空列表", NineSlice.regions(0, 0, 0, 10, 3, 12, 12).isEmpty(), true,
                false);
        check(lines, counters, "贴图尺寸为 0 时返回空列表", NineSlice.regions(0, 0, 10, 10, 3, 0, 12).isEmpty(), true,
                false);

        // 更大的贴图（32×32、边框 3）也要铺满
        List<NineSlice.Region> big = NineSlice.regions(5, 5, 118, 14, 3, 32, 32);
        int bigArea = 0;
        for (NineSlice.Region region : big) {
            bigArea += region.area();
        }
        check(lines, counters, "32×32 贴图同样铺满（118×14 的按钮）", bigArea == 118 * 14, 118 * 14, bigArea);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("stc", path);
    }

    private static String read(String relative, List<String> lines, int[] counters) {
        Path path = Path.of("src", "main", "java", "com", "slaythecraft").resolve(relative);
        if (!Files.isRegularFile(path)) {
            check(lines, counters, "源码存在：" + path, false, "存在", "缺失");
            return null;
        }
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            check(lines, counters, "源码可读：" + path, false, "可读", e.getClass().getSimpleName());
            return null;
        }
    }

    private static void check(List<String> lines, int[] counters, String label, boolean condition,
                             Object expected, Object actual) {
        if (condition) {
            counters[0]++;
            lines.add("PASS  " + label);
        } else {
            counters[1]++;
            lines.add("FAIL  " + label + "（期望 " + expected + "，实际 " + actual + "）");
        }
    }
}
