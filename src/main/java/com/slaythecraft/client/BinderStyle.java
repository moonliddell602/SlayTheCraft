package com.slaythecraft.client;

import com.slaythecraft.Config;
import com.slaythecraft.data.PlayerCardData;
import com.slaythecraft.data.StcAttachments;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 收纳包系列界面的公共画法与几何（v0.2.6）。
 *
 * <p>反馈："以后，默认任何有子页面的内容，其母页面的查看详情等引导都默认**新建页面 UI** 显示子页面，
 * 以防出现文本堆叠等问题。" 于是收纳包被拆成：主页 {@link CardBinderScreen} +
 * 三个子页面（{@link BinderCardsScreen} / {@link BinderRelicsScreen} / {@link BinderProgressScreen}），
 * 它们共用这里的书页背景、抬头与按钮画法。</p>
 */
final class BinderStyle {

    /**
     * 统一面板尺寸（所有页面一致，切换时不会跳来跳去）。
     *
     * <p>v0.4.3 按反馈**放大**：原来的 232×238 太小，图鉴模块会顶出背景；
     * 现在默认 268×300，并且用 {@link #widthFor}/{@link #heightFor} 按屏幕自适应收缩，
     * 小窗口下也不会超出屏幕。</p>
     */
    static final int WIDTH = 268;
    static final int HEIGHT = 300;
    static final int GRID_X = 16;
    /** 页脚按钮的 y（跟着面板高度走，不再写死）。 */
    static int footerY(int imageHeight) {
        return imageHeight - 26;
    }

    /** 实际宽度：屏幕太窄时收缩。 */
    static int widthFor(net.minecraft.client.gui.screens.Screen screen) {
        return Math.max(180, Math.min(WIDTH, screen.width - 12));
    }

    /** 实际高度：屏幕太矮时收缩。 */
    static int heightFor(net.minecraft.client.gui.screens.Screen screen) {
        return Math.max(160, Math.min(HEIGHT, screen.height - 12));
    }

    // ---------------------------------------------------------------- 页脚翻页几何
    //
    // v0.5.0.4 需求："药水图鉴位置，上一页/下一页的位置错误（被放置在了图鉴外）。"
    //
    // 这几个方法把"页脚翻页按钮到底画在哪"变成**可断言的纯函数**（以前是散在
    // PotionCodexScreen / StatusCodexScreen 里的私有方法，自检没法验证），
    // 并且**统一按左右对称的内缩排版**：左边「返回」在 GRID_X，右边「下一页」的右边缘
    // 也正好在 imageWidth - GRID_X，两边留白一样宽，任何面板宽度下都不会越过书页边界。

    /** 页脚「返回」按钮的宽度。 */
    static final int FOOTER_BACK_W = 52;
    /** 页脚翻页按钮的高度。 */
    static final int FOOTER_BTN_H = 14;
    /** 页脚翻页按钮的最大宽度。 */
    static final int PAGE_BTN_MAX_W = 52;
    /** 相邻两个按钮之间的间隙。 */
    static final int PAGE_BTN_GAP = 4;

    /**
     * 翻页按钮宽度（面板窄时收缩，保证「返回」和两个翻页按钮互不重叠）。
     *
     * <p>可用宽度 = 面板宽 − 左右内缩 − 「返回」宽度 − 「返回」与「上一页」之间的一个间隙，
     * 再对半分给两个按钮。下限取 26 而不是 30：面板最窄（180）时若强行给 30，
     * 两个按钮加起来就放不下了，会和「返回」压在一起（自检会抓到）。</p>
     */
    static int pageButtonWidth(int imageWidth) {
        int usable = imageWidth - GRID_X * 2 - FOOTER_BACK_W - PAGE_BTN_GAP;
        return Math.min(PAGE_BTN_MAX_W, Math.max(26, usable / 2));
    }

    /** 右对齐的「下一页」按钮 x（相对面板左边）：右边缘落在 imageWidth - GRID_X。 */
    static int nextPageButtonX(int imageWidth) {
        return imageWidth - GRID_X - pageButtonWidth(imageWidth);
    }

    /** 「上一页」按钮 x。最窄时也不会被挤到面板左边之外。 */
    static int prevPageButtonX(int imageWidth) {
        int prev = nextPageButtonX(imageWidth) - PAGE_BTN_GAP - pageButtonWidth(imageWidth);
        return Math.max(GRID_X, prev);
    }

    /**
     * 页脚"共 N 瓶 / 共 N 条"的总数文本**左边缘**（跟着「返回」右边留 6 像素）。
     *
     * <p>以前这 6 像素是写死的 {@code + 58}（52 + 6），现在统一从 {@link #FOOTER_BACK_W} 推出来。</p>
     */
    static int footerCountX(int leftPos) {
        return leftPos + GRID_X + FOOTER_BACK_W + 6;
    }

    /**
     * 页脚"共 N 瓶 / 共 N 条"的总数文本能不能放得下（放不下就让位给翻页按钮）。
     *
     * @param countTextWidth 文本像素宽
     */
    static boolean footerCountFits(int imageWidth, int countTextWidth) {
        int countStart = GRID_X + FOOTER_BACK_W + 6;
        return countStart + countTextWidth + PAGE_BTN_GAP <= prevPageButtonX(imageWidth);
    }

    // ---------------------------------------------------------------- 名单页脚几何（v0.5.0.12）
    //
    // 反馈："页码被返回按钮覆盖的问题。"
    //
    // 名单页原来是**写死坐标**的：页码画在 CONTENT_X=12、返回按钮也画在 CONTENT_X+66=78，
    // 而"第 1 / 3 页（滚轮翻页）"在中文下宽约 100 像素 —— 于是页码的右半截正好被返回按钮压住。
    // 现在改成：**按钮从右往左排**（「返回」贴最右），页码占据剩下的左侧空间，
    // 并且只要页码放不下就换成短文案（见 {@link #rosterPageText}），不再有重叠的可能。

    /** 名单页脚按钮宽度（与 {@link #FOOTER_BACK_W} 同宽，视觉一致）。 */
    static final int ROSTER_BTN_W = 56;

    /** 名单页脚「返回」按钮 x（右对齐到 imageWidth − GRID_X）。 */
    static int rosterBackButtonX(int imageWidth) {
        return imageWidth - GRID_X - ROSTER_BTN_W;
    }

    /** 名单页脚「翻页」按钮 x（在「返回」左边留一个间隙）。 */
    static int rosterPageButtonX(int imageWidth) {
        return rosterBackButtonX(imageWidth) - PAGE_BTN_GAP - ROSTER_BTN_W;
    }

    /** 名单页脚页码文本的可用宽度（从 GRID_X 到「翻页」按钮左边再留一个间隙）。 */
    static int rosterPageTextWidth(int imageWidth) {
        return Math.max(0, rosterPageButtonX(imageWidth) - PAGE_BTN_GAP - GRID_X);
    }

    /** 页码文本的 y（相对面板顶）。 */
    static final int ROSTER_FOOTER_TEXT_Y = 3;

    /**
     * 名单页脚的页码文案：长版放不下就用短版。
     *
     * <p>这是"页码被覆盖"的结构性修复：不再假设文案多宽，而是**先量再选**。</p>
     *
     * @param textWidth 量文本像素宽的函数
     */
    static String rosterPageText(Font font, int imageWidth, int page, int pages, String longText, String shortText) {
        int room = rosterPageTextWidth(imageWidth);
        return font.width(longText) <= room ? longText : shortText;
    }
    static final int BOOK_PAGE = 0xFFE8D8AE;
    static final int BOOK_EDGE = 0xFF7A5A2E;
    static final int BOOK_LINE = 0xFFC4AE7E;
    static final int INK = 0xFF3A2A18;
    static final int INK_DIM = 0xFF7A6A50;
    static final int ACCENT = 0xFF9A5A12;
    static final int CELL_BG = 0xFF2A2A34;
    static final int CELL_SELECTED = 0xFFB8860B;
    static final int TEXT = 0xFFE8E8E8;
    static final int TEXT_DIM = 0xFF9A9AA4;
    static final int GOLD = 0xFFFFD24A;
    static final int DANGER = 0xFFD05A5A;

    private BinderStyle() {
    }

    /** 当前客户端档案（同步过来的那份）；拿不到返回 null。 */
    static PlayerCardData data() {
        Player player = Minecraft.getInstance().player;
        return player == null ? null : StcAttachments.dataOf(player);
    }

    /**
     * 书页背景：**优先用可自定义贴图**（{@link BinderSkin}），没有才画程序化羊皮纸。
     *
     * <p>v0.4.6.5：面板尺寸传给这里（原来写死 268×300，窗口小时背景会和面板对不齐）。</p>
     */
    static void book(GuiGraphics graphics, int leftPos, int topPos, int width, int height) {
        int x2 = leftPos + width;
        int y2 = topPos + height;
        graphics.fill(leftPos - 2, topPos - 2, x2 + 2, y2 + 2, BOOK_EDGE);
        if (BinderSkin.drawBackground(graphics, leftPos, topPos, width, height)) {
            // 自定义背景：整块交给作者，不再叠程序化的书脊与页边
            return;
        }
        graphics.fill(leftPos, topPos, x2, y2, BOOK_PAGE);
        graphics.fill(leftPos + width / 2 - 1, topPos + 2, leftPos + width / 2 + 1, y2 - 2, BOOK_LINE);
        graphics.fill(leftPos + 6, topPos + 6, x2 - 6, topPos + 7, BOOK_LINE);
        graphics.fill(leftPos + 6, y2 - 7, x2 - 6, y2 - 6, BOOK_LINE);
    }

    /** 兼容旧调用：用默认面板尺寸。 */
    static void book(GuiGraphics graphics, int leftPos, int topPos) {
        book(graphics, leftPos, topPos, WIDTH, HEIGHT);
    }

    /** 抬头：书与笔图标 + 程序化头像 + 名字 + 独立生命值 + 金币 + 层数。 */
    static void header(GuiGraphics graphics, Font font, int leftPos, int topPos) {
        int x = leftPos + 10;
        int y = topPos + 8;
        graphics.renderItem(new ItemStack(Items.WRITABLE_BOOK), x, y);
        int avatarX = x + 22;
        PlayerCardData data = data();
        drawAvatar(graphics, font, avatarX, y, data);

        String name = Minecraft.getInstance().player == null ? "?" : Minecraft.getInstance().player.getName().getString();
        TextStyle.draw(graphics, font, name, avatarX + 26, y + 1, INK);
        Component hp = data == null
                ? Component.literal("HP -/-")
                : Component.translatable("stc.binder.progress.hp", data.life().describe(), Config.maxPlayerHp());
        TextStyle.draw(graphics, font, hp, avatarX + 26, y + 11, ACCENT);
        if (data != null) {
            Component gold = Component.translatable("stc.binder.gold", data.gold());
            TextStyle.draw(graphics, font, gold, leftPos + WIDTH - 10 - font.width(gold), y + 1, ACCENT);
            Component floor = Component.translatable("stc.binder.header.floor", data.floor());
            TextStyle.draw(graphics, font, floor, leftPos + WIDTH - 10 - font.width(floor), y + 11, INK_DIM);
        }
    }

    /**
     * 人物头像：**优先用可自定义贴图**（{@link BinderSkin}），没有才画程序化小脸。
     *
     * <p>贴图查找顺序：角色专属 → 通用 → 程序化（22×22 小脸，底色按角色区分）。</p>
     */
    static void drawAvatar(GuiGraphics graphics, Font font, int x, int y, PlayerCardData data) {
        ResourceLocation character = data == null ? null : data.character();
        if (BinderSkin.drawAvatar(graphics, character, x, y, AVATAR_SIZE)) {
            return;
        }
        int skin = avatarColor(data);
        graphics.fill(x, y, x + AVATAR_SIZE, y + AVATAR_SIZE, skin);
        border(graphics, x, y, x + AVATAR_SIZE, y + AVATAR_SIZE, BOOK_EDGE);
        graphics.fill(x + 5, y + 7, x + 8, y + 10, INK);
        graphics.fill(x + 13, y + 7, x + 16, y + 10, INK);
        graphics.fill(x + 6, y + 15, x + 15, y + 16, INK);
    }

    static int avatarColor(PlayerCardData data) {
        if (data == null || data.character() == null) {
            return 0xFFC9A227;
        }
        return switch (data.character().getPath()) {
            case "ironclad" -> 0xFFB5533C;
            case "silent" -> 0xFF4E8C6A;
            case "defect" -> 0xFF4A7FB5;
            case "pillager" -> 0xFF6E7A6A;
            default -> 0xFFC9A227;
        };
    }

    /** 头像方框边长（程序化头像与自定义头像共用同一块位置）。 */
    static final int AVATAR_SIZE = 22;

    /**
     * 书页风格按钮：**优先用可自定义贴图**（{@link BinderSkin}，九宫格），没有才画程序化方块。
     *
     * <p>悬停状态由这里自己按鼠标位置判断（界面画按钮时正好能拿到鼠标），
     * 所以所有调用点不用改就能享受"悬停贴图"。</p>
     */
    static void button(GuiGraphics graphics, Font font, int x, int y, int width, int height, String label,
                       boolean enabled) {
        boolean hovered = enabled && BinderSkin.hovered(x, y, width, height);
        BinderSkin.drawButton(graphics, font, x, y, width, height, label, enabled, hovered,
                () -> proceduralButton(graphics, font, x, y, width, height, label, enabled, hovered));
    }

    /** 程序化按钮（缺贴图时的兜底画法）。 */
    private static void proceduralButton(GuiGraphics graphics, Font font, int x, int y, int width, int height,
                                         String label, boolean enabled, boolean hovered) {
        int fill = !enabled ? 0xFFDCCFAE : (hovered ? 0xFFFFF3D6 : 0xFFF2E6C4);
        graphics.fill(x, y, x + width, y + height, fill);
        border(graphics, x, y, x + width, y + height, enabled ? BOOK_EDGE : BOOK_LINE);
        TextStyle.draw(graphics, font, label, x + (width - font.width(label)) / 2, y + (height - 8) / 2,
                enabled ? INK : INK_DIM);
    }

    static void border(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        graphics.fill(x1, y1, x2, y2 == y1 + 0 ? y1 : y1 + 1, color);
        graphics.fill(x1, y2 - 1, x2, y2, color);
        graphics.fill(x1, y1, x1 + 1, y2, color);
        graphics.fill(x2 - 1, y1, x2, y2, color);
    }

    /**
     * 按宽度把文字折行（v0.2.7）。
     *
     * <p>反馈："存在会因为卡牌描述文字超出显示范围而导致显示不全的问题" / "遗物描述…"。
     * 以前用 {@code plainSubstrByWidth} 直接截断，长描述就看不全；现在统一折行显示。</p>
     */
    static java.util.List<String> wrap(Font font, String text, int maxWidth) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return lines;
        }
        for (String paragraph : text.split("\\n")) {
            String remaining = paragraph;
            while (!remaining.isEmpty()) {
                String line = font.plainSubstrByWidth(remaining, maxWidth);
                if (line.isEmpty()) {
                    break;
                }
                lines.add(line);
                remaining = remaining.substring(line.length());
            }
        }
        return lines;
    }

    /**
     * 画一段**保证不越框**的文字：先按 {@code maxWidth} 折行，最多画 {@code maxLines} 行，
     * 放不下的部分用「…」收尾（v0.5.0.12）。
     *
     * <p>反馈："右侧的ID存在会超出UI范围的问题。" / "尖塔入口主页的文字存在会超出UI范围的问题。"
     * 以前这些位置用 {@code plainSubstrByWidth} 直接截断、或者干脆不截断，长文案就爬出面板；
     * 现在统一走这里，宽度方向由折行兜住、行数方向由 {@code maxLines} 兜住。</p>
     *
     * @return 实际画了多少行
     */
    static int text(GuiGraphics graphics, Font font, String text, int x, int y, int maxWidth, int maxLines,
                    int color) {
        if (maxLines <= 0 || maxWidth <= 0) {
            return 0;
        }
        java.util.List<String> lines = wrap(font, text, maxWidth);
        if (lines.isEmpty()) {
            TextStyle.draw(graphics, font, text == null ? "" : text, x, y, color);
            return 1;
        }
        boolean clipped = lines.size() > maxLines;
        int shown = Math.min(maxLines, lines.size());
        for (int i = 0; i < shown; i++) {
            String line = lines.get(i);
            if (clipped && i == shown - 1) {
                // 末行给省略号留位置（同样先量再截，不会反过来越框）
                int room = Math.max(0, maxWidth - font.width("…"));
                line = font.plainSubstrByWidth(line, room) + "…";
            }
            TextStyle.draw(graphics, font, line, x, y + i * LINE_H, color);
        }
        return shown;
    }

    /** {@link #text} 用的行高（与界面其它正文一致）。 */
    static final int LINE_H = 11;
}
