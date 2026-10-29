package com.slaythecraft.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.slaythecraft.combat.core.Relic;
import com.slaythecraft.combat.core.RelicLibrary;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 战斗界面**左侧的遗物栏**（v0.5.1.3 美术需求）。
 *
 * <p>需求原文（用户在截图里圈的空白区 + 红字）："显示已经拥有的遗物（显示其小图标）
 * 鼠标在遗物上悬停以查看描述。"</p>
 *
 * <h2>位置</h2>
 *
 * <p>贴在状态框（{@code 8,26,132,44}）下面、左下角能量框与「查看我的牌组」按钮**之上**：
 * 那一列本来就是空的（用户的截图里圈的就是这块）。底边由
 * {@link BattleScreen#relicBarBottomLimit(int)} 算出来，所以永远不会压住按钮。</p>
 *
 * <h2>放不下怎么办</h2>
 *
 * <p>一列放不下就**多排一列**（最多 {@link #MAX_COLS} 列，仍在左侧空白区内）；
 * 实在装不下时最后一格画 {@code +N}，并且鼠标悬停那一格会列出**全部**遗物名 —— 不静默丢信息。</p>
 *
 * <p>几何全部是纯静态函数，自检（{@code CodexPagesCheck}）直接算，不需要真的进游戏。</p>
 */
public final class BattleRelicBar {

    /** 左边缘（与状态框对齐）。 */
    public static final int X = 8;
    /** 顶边：状态框下沿 70 + 6。 */
    public static final int TOP = 76;
    /** 每格边长（含 2 像素内边距，图标 16×16）。 */
    public static final int SLOT = 20;
    /** 图标边长。 */
    public static final int ICON = 16;
    /** 最多排几列（再右就会挤到手牌区）。 */
    public static final int MAX_COLS = 3;
    /** 悬停说明框的宽度。 */
    private static final int TIP_W = 168;
    /** 说明框里每行文字的行高。 */
    private static final int TIP_LINE_H = 10;

    // 战斗 HUD 的配色（与 BattleScreen 里那几个常量一致）
    private static final int PANEL_BG = 0xC0101016;
    private static final int PANEL_EDGE = 0xFF5A5A64;
    private static final int SLOT_BG = 0x80000000;
    private static final int TEXT = 0xFFE8E8E8;
    private static final int TEXT_DIM = 0xFF9A9AA4;
    private static final int GOLD = 0xFFFFD24A;
    private static final int SELECTED = 0xFFB8860B;

    private BattleRelicBar() {
    }

    /** 遗物栏底边（= 能量框上一行按钮的顶边再留 4 像素）。 */
    public static int bottom(int guiHeight) {
        return BattleScreen.relicBarBottomLimit(guiHeight);
    }

    /** 一列能放几格（界面太矮时可能为 0）。 */
    public static int rowsFor(int guiHeight) {
        return Math.max(0, (bottom(guiHeight) - TOP) / SLOT);
    }

    /** 显示 {@code count} 件遗物需要几列（0 = 一件都没有 / 一行都放不下）。 */
    public static int columnsFor(int count, int guiHeight) {
        int rows = rowsFor(guiHeight);
        if (rows <= 0 || count <= 0) {
            return 0;
        }
        return Math.min(MAX_COLS, (count + rows - 1) / rows);
    }

    /** 最多画得下几件（超出的用 {@code +N} 表示）。 */
    public static int capacity(int guiHeight) {
        return MAX_COLS * rowsFor(guiHeight);
    }

    /**
     * 第 {@code index} 件遗物的格子矩形（{@code {x, y, w, h}}，绝对坐标）。
     *
     * <p>按**列优先**排：遗物少的时候就是一列往下排，多了才往右边长第二、第三列。</p>
     */
    public static int[] slotRect(int index, int guiHeight) {
        int rows = Math.max(1, rowsFor(guiHeight));
        int col = index / rows;
        int row = index % rows;
        return new int[] {X + col * SLOT, TOP + row * SLOT, SLOT, SLOT};
    }

    /** 整个栏位的背景矩形；一件遗物都没有（或一行都放不下）时返回 0 尺寸。 */
    public static int[] panelRect(int count, int guiHeight) {
        int shown = Math.min(Math.max(0, count), capacity(guiHeight));
        int rows = rowsFor(guiHeight);
        if (shown <= 0 || rows <= 0) {
            return new int[] {X, TOP, 0, 0};
        }
        int cols = Math.max(1, columnsFor(shown, guiHeight));
        int usedRows = Math.min(rows, shown);
        return new int[] {X - 2, TOP - 2, cols * SLOT + 4, usedRows * SLOT + 4};
    }

    /** 鼠标命中的格子下标（{@code -1} = 没命中）。 */
    public static int hitIndex(double mouseX, double mouseY, int count, int guiHeight) {
        int shown = Math.min(Math.max(0, count), capacity(guiHeight));
        for (int i = 0; i < shown; i++) {
            int[] r = slotRect(i, guiHeight);
            if (mouseX >= r[0] && mouseX < r[0] + r[2] && mouseY >= r[1] && mouseY < r[1] + r[3]) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 把"已拥有的遗物 id"排成**位置稳定**的顺序。
     *
     * <p>档案里存的是 {@code Set}，直接迭代的话顺序没有保证 —— 那样遗物栏的图标会在每次同步后
     * 换位置，鼠标悬停就会指到别的东西。这里统一按**图鉴登记顺序**排（初始遗物在前，
     * 与玩家获得顺序大体一致），其它模组塞进来的未知 id 排在后面并按字典序，保证稳定。</p>
     */
    public static List<String> orderedIds(Collection<String> ids) {
        List<String> out = new ArrayList<>();
        if (ids == null || ids.isEmpty()) {
            return out;
        }
        for (Relic relic : RelicLibrary.all()) {
            if (ids.contains(relic.id())) {
                out.add(relic.id());
            }
        }
        List<String> unknown = new ArrayList<>();
        for (String id : ids) {
            if (RelicLibrary.byId(id) == null) {
                unknown.add(id);
            }
        }
        Collections.sort(unknown);
        out.addAll(unknown);
        return out;
    }

    /**
     * 画整个遗物栏（背景 + 图标 + 悬停说明）。
     *
     * @param relicIds 已拥有的遗物 id（{@link #orderedIds} 会先排一遍）
     */
    public static void draw(GuiGraphics graphics, Font font, Collection<String> relicIds, int guiHeight,
                            int screenWidth, double mouseX, double mouseY) {
        List<String> ids = orderedIds(relicIds);
        if (ids.isEmpty()) {
            return;
        }
        int rows = rowsFor(guiHeight);
        int shown = Math.min(ids.size(), capacity(guiHeight));
        if (rows <= 0 || shown <= 0) {
            return;
        }
        int[] panel = panelRect(ids.size(), guiHeight);
        graphics.fill(panel[0], panel[1], panel[0] + panel[2], panel[1] + panel[3], PANEL_BG);
        border(graphics, panel[0], panel[1], panel[0] + panel[2], panel[1] + panel[3], PANEL_EDGE);

        int hovered = hitIndex(mouseX, mouseY, ids.size(), guiHeight);
        int overflowSlot = -1;
        for (int i = 0; i < shown; i++) {
            boolean last = i == shown - 1;
            boolean overflow = last && ids.size() > shown;
            if (overflow) {
                overflowSlot = i;
            }
            int[] r = slotRect(i, guiHeight);
            boolean hot = hovered == i;
            graphics.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], SLOT_BG);
            border(graphics, r[0], r[1], r[0] + r[2], r[1] + r[3], hot ? SELECTED : PANEL_EDGE);
            int iconX = r[0] + (r[2] - ICON) / 2;
            int iconY = r[1] + (r[3] - ICON) / 2;
            if (overflow) {
                TextStyle.draw(graphics, font, "+" + (ids.size() - shown), r[0] + 3, r[1] + 6, GOLD);
            } else {
                drawIcon(graphics, font, ids.get(i), iconX, iconY);
            }
        }
        if (hovered >= 0) {
            drawTooltip(graphics, font, ids, hovered, overflowSlot == hovered, guiHeight, screenWidth);
        }
    }

    /** 单个遗物图标（有贴图用贴图，没有就画程序化符文）。 */
    private static void drawIcon(GuiGraphics graphics, Font font, String relicId, int x, int y) {
        Relic relic = RelicLibrary.byId(relicId);
        if (relic == null) {
            // 其它模组塞进来的未知 id：只画一个问号符文，别静默不画
            graphics.fill(x, y, x + ICON, y + ICON, 0xFF3A3A48);
            TextStyle.draw(graphics, font, "?", x + 4, y + 4, TEXT_DIM);
            return;
        }
        RelicIcons.draw(graphics, font, relic, relic.name(), x, y, ICON);
    }

    /** 悬停说明：遗物名 + 描述（折行，不截断）；溢出格列出全部名字。 */
    private static void drawTooltip(GuiGraphics graphics, Font font, List<String> ids, int index,
                                    boolean overflow, int guiHeight, int screenWidth) {
        Relic relic = RelicLibrary.byId(ids.get(index));
        String title = overflow
                ? net.minecraft.network.chat.Component.translatable("stc.battle.relics.overflow").getString()
                : (relic == null ? ids.get(index) : relic.name());
        List<String> lines = new ArrayList<>();
        if (overflow) {
            for (String id : ids) {
                Relic other = RelicLibrary.byId(id);
                lines.add(other == null ? id : other.name());
            }
        } else if (relic != null) {
            lines.addAll(BinderStyle.wrap(font, relic.description(), TIP_W - 10));
        }

        int[] panel = panelRect(ids.size(), guiHeight);
        int tipX = panel[0] + panel[2] + 6;
        if (tipX + TIP_W > screenWidth - 4) {
            tipX = Math.max(4, screenWidth - TIP_W - 4);
        }
        int tipH = 14 + Math.max(1, lines.size()) * TIP_LINE_H + 4;
        int tipY = Math.max(4, Math.min(guiHeight - tipH - 4, slotRect(index, guiHeight)[1]));
        graphics.fill(tipX - 1, tipY - 1, tipX + TIP_W + 1, tipY + tipH + 1, 0xFF000000);
        graphics.fill(tipX, tipY, tipX + TIP_W, tipY + tipH, 0xF0101016);
        border(graphics, tipX, tipY, tipX + TIP_W, tipY + tipH, PANEL_EDGE);
        TextStyle.draw(graphics, font, title, tipX + 5, tipY + 4, GOLD);
        int lineY = tipY + 16;
        for (String line : lines) {
            TextStyle.draw(graphics, font, line, tipX + 5, lineY, TEXT);
            lineY += TIP_LINE_H;
        }
    }

    private static void border(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        graphics.fill(x1, y1, x2, y1 + 1, color);
        graphics.fill(x1, y2 - 1, x2, y2, color);
        graphics.fill(x1, y1, x1 + 1, y2, color);
        graphics.fill(x2 - 1, y1, x2, y2, color);
    }
}
