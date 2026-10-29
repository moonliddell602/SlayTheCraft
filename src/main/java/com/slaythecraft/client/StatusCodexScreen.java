package com.slaythecraft.client;

import java.util.List;

import com.slaythecraft.binder.CardBinderMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 「状态图鉴」页（v0.4.6.7 新图鉴）。
 *
 * <p>需求原文："新图鉴：状态图鉴 —— 按钮显示状态图标和名称"，
 * "详情页面显示状态效果描述，图标，名称，和机制。"</p>
 *
 * <p>所以这一页每条状态都是【图标 + 名字】的按钮（数据来自 {@link StatusIcons#all()}，
 * 一条都不排除 —— 需求里"不包括能力"说的是**能力牌**不是状态，所以这里也不加任何卡牌），
 * 点进去是**新页面** {@link StatusDetailScreen}（沿用 v0.2.6 的约定）。
 * 条目超过一页时用底部的「上一页 / 下一页」按钮 + {@code stc.question.pick.page} 计数翻页
 * （和阵营详情页 / 选牌页同一套写法）。</p>
 */
public class StatusCodexScreen extends AbstractContainerScreen<CardBinderMenu> {

    /** 列表两列（格子够宽，状态名和图标都看得清）。 */
    public static final int COLS = 2;
    /** 每页可见行数。 */
    public static final int VISIBLE_ROWS = 6;
    private static final int CELL_H = 26;
    private static final int CELL_GAP_X = 4;
    private static final int CELL_GAP_Y = 2;
    private static final int GRID_Y = 58;
    /** 列表格子里的状态图标边长（把 8×8 的状态精灵放大到 16×16）。 */
    private static final int ICON = 16;

    private int page;

    public StatusCodexScreen(CardBinderMenu menu, Inventory inventory, Component title) {
        this(menu, inventory, title, 0);
    }

    /** 带页码的构造器：详情页返回时可以回到原来那一页。 */
    public StatusCodexScreen(CardBinderMenu menu, Inventory inventory, Component title, int page) {
        super(menu, inventory, title);
        this.imageWidth = BinderStyle.WIDTH;
        this.imageHeight = BinderStyle.HEIGHT;
        this.page = Math.max(0, page);
    }

    /** 每行几个（和遗物图鉴一样抽成公开静态方法，自检可以直接算）。 */
    public static int columns() {
        return COLS;
    }

    /** 一页能放几条。 */
    public static int perPage() {
        return COLS * VISIBLE_ROWS;
    }

    /** {@code count} 条状态要几页（空列表也算 1 页）。 */
    public static int pagesFor(int count) {
        return Math.max(1, (Math.max(0, count) + perPage() - 1) / perPage());
    }

    @Override
    public void init() {
        // v0.4.3：面板尺寸按屏幕自适应（默认更大，小窗口自动收缩）
        this.imageWidth = BinderStyle.widthFor(this);
        this.imageHeight = BinderStyle.heightFor(this);
        super.init();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        BinderStyle.book(graphics, leftPos, topPos, imageWidth, imageHeight);
    }

    /** 格子宽度跟着面板走（窗口变窄时两列一起收缩，不会顶出书页）。 */
    private int cellWidth() {
        return Math.max(60, (imageWidth - BinderStyle.GRID_X * 2 - CELL_GAP_X * (COLS - 1)) / COLS);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        BinderStyle.header(graphics, font, leftPos, topPos);
        Component heading = Component.translatable("stc.codex.status.heading");
        TextStyle.draw(graphics, font, heading, leftPos + BinderStyle.GRID_X, topPos + 40, BinderStyle.INK);

        List<StatusIcons.Entry> entries = StatusIcons.all();
        int pages = pagesFor(entries.size());
        clampPage(entries.size());
        int cellW = cellWidth();
        int first = page * perPage();
        for (int cell = 0; cell < perPage(); cell++) {
            int index = first + cell;
            if (index >= entries.size()) {
                break;
            }
            StatusIcons.Entry entry = entries.get(index);
            int x = leftPos + BinderStyle.GRID_X + (cell % COLS) * (cellW + CELL_GAP_X);
            int y = topPos + GRID_Y + (cell / COLS) * (CELL_H + CELL_GAP_Y);
            boolean hovered = isInside(mouseX, mouseY, x, y, cellW, CELL_H);
            graphics.fill(x, y, x + cellW, y + CELL_H, hovered ? 0xFFFFF3D6 : 0xFFF2E6C4);
            BinderStyle.border(graphics, x, y, x + cellW, y + CELL_H,
                    hovered ? BinderStyle.CELL_SELECTED : BinderStyle.BOOK_EDGE);
            // 需求："按钮显示状态图标和名称。"
            drawIcon(graphics, entry, x + 4, y + (CELL_H - ICON) / 2);
            TextStyle.draw(graphics, font, entry.label(), x + 4 + ICON + 5, y + (CELL_H - 8) / 2, BinderStyle.INK);
        }
        if (entries.isEmpty()) {
            Component empty = Component.translatable("stc.codex.empty");
            TextStyle.draw(graphics, font, empty, leftPos + (imageWidth - font.width(empty)) / 2, topPos + 110,
                    BinderStyle.INK_DIM);
        }
        if (pages > 1) {
            Component pageText = Component.translatable("stc.question.pick.page", page + 1, pages);
            TextStyle.draw(graphics, font, pageText,
                    leftPos + imageWidth - BinderStyle.GRID_X - font.width(pageText), topPos + 40,
                    BinderStyle.INK_DIM);
        }

        int footer = topPos + BinderStyle.footerY(imageHeight);
        BinderStyle.button(graphics, font, leftPos + BinderStyle.GRID_X, footer, 52, 14,
                Component.translatable("stc.binder.back").getString(), true);
        // 翻页按钮的宽度按面板自适应：面板被屏幕压窄时一起收缩，不会盖住「返回」
        int pageW = pageButtonWidth();
        int nextX = nextButtonX();
        int prevX = BinderStyle.prevPageButtonX(imageWidth);
        Component count = Component.translatable("stc.codex.status.count", entries.size());
        // 总数放得下才画（放不下就让位给翻页按钮；页数在标题行右侧一直看得到）
        if (pages <= 1 || BinderStyle.footerCountFits(imageWidth, font.width(count))) {
            TextStyle.draw(graphics, font, count, BinderStyle.footerCountX(leftPos), footer + 3,
                    BinderStyle.INK_DIM);
        }
        if (pages > 1) {
            BinderStyle.button(graphics, font, prevX, footer, pageW, 14,
                    Component.translatable("stc.codex.page.prev").getString(), page > 0);
            BinderStyle.button(graphics, font, nextX, footer, pageW, 14,
                    Component.translatable("stc.codex.page.next").getString(), page < pages - 1);
        }
    }

    /**
     * 画一条状态的图标（需求："按钮显示状态图标和名称"）。
     *
     * <p>交给 {@link IconFit} 按贴图**真实像素尺寸**等比缩放、居中塞进 16×16 的方框：
     * 原版状态效果贴图是 18×18、物品贴图是 16×16，作者自己画更大 / 不是正方形的图也不会被压扁，
     * 和 {@link StatusIcons#draw}（战斗内 8×8 的同款画法）共用同一套规则。</p>
     */
    private static void drawIcon(GuiGraphics graphics, StatusIcons.Entry entry, int x, int y) {
        IconFit.draw(graphics, entry.texture(), x, y, ICON, ICON);
    }

    private void clampPage(int count) {
        page = Math.max(0, Math.min(pagesFor(count) - 1, page));
    }

    /** 翻页按钮宽度（面板窄时收缩，保证不盖住左下角的「返回」）。 */
    private int pageButtonWidth() {
        return BinderStyle.pageButtonWidth(imageWidth);
    }

    /** 右对齐的「下一页」按钮 x（相对面板左边）。 */
    private int nextButtonX() {
        return BinderStyle.nextPageButtonX(imageWidth);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (minecraft == null || minecraft.player == null) {
            return super.mouseClicked(mouseX, mouseY, mouseButton);
        }
        int relX = (int) mouseX - leftPos;
        int relY = (int) mouseY - topPos;
        int footer = BinderStyle.footerY(imageHeight);
        if (relX >= BinderStyle.GRID_X && relX < BinderStyle.GRID_X + 52
                && relY >= footer && relY < footer + 14) {
            minecraft.setScreen(new CodexScreen(menu, minecraft.player.getInventory(), title));
            return true;
        }
        List<StatusIcons.Entry> entries = StatusIcons.all();
        int pages = pagesFor(entries.size());
        if (pages > 1) {
            int pageW = pageButtonWidth();
            int nextX = nextButtonX();
            int prevX = BinderStyle.prevPageButtonX(imageWidth);
            if (isInside(relX, relY, prevX, footer, pageW, 14)) {
                page = Math.max(0, page - 1);
                return true;
            }
            if (isInside(relX, relY, nextX, footer, pageW, 14)) {
                page = Math.min(pages - 1, page + 1);
                return true;
            }
        }
        int cellW = cellWidth();
        int first = page * perPage();
        for (int cell = 0; cell < perPage(); cell++) {
            int index = first + cell;
            if (index >= entries.size()) {
                break;
            }
            int x = BinderStyle.GRID_X + (cell % COLS) * (cellW + CELL_GAP_X);
            int y = GRID_Y + (cell / COLS) * (CELL_H + CELL_GAP_Y);
            if (isInside(relX, relY, x, y, cellW, CELL_H)) {
                minecraft.setScreen(new StatusDetailScreen(menu, minecraft.player.getInventory(), title,
                        entries.get(index), this::samePage));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int pages = pagesFor(StatusIcons.all().size());
        if (pages > 1) {
            page = Math.max(0, Math.min(pages - 1, page + (scrollY > 0 ? -1 : 1)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /** 重建"本页"（详情页返回时用，页码原样带上）。 */
    private StatusCodexScreen samePage() {
        return new StatusCodexScreen(menu, minecraft.player.getInventory(), title, page);
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
