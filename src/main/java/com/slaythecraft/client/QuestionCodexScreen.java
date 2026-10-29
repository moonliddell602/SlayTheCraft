package com.slaythecraft.client;

import java.util.ArrayList;
import java.util.List;

import com.slaythecraft.binder.CardBinderMenu;
import com.slaythecraft.question.QuestionEvent;
import com.slaythecraft.question.QuestionLibrary;
import com.slaythecraft.question.QuestionOption;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;

/**
 * 「问号事件图鉴」页（v0.5.1.2 新图鉴）。
 *
 * <p>需求原文："图鉴：新增问号事件图鉴（查看问号事件与事件奖励）。"</p>
 *
 * <p>列表数据直接来自 {@link QuestionLibrary#all()}（登记了几个就画几个，不写死数量）；
 * 每个格子用望远镜图标 + 事件名 + 触发方式。点进去是**新页面**，正文依次写
 * 触发方式、事件描述、以及**每个选项的奖励**（{@link QuestionOption#description()}，
 * 也就是玩家在事件里真正能拿到什么）。</p>
 *
 * <p>沿用 v0.2.6 的约定："默认任何有子页面的内容，其母页面的查看详情等引导都默认新建页面 UI
 * 显示子页面。"</p>
 */
public class QuestionCodexScreen extends AbstractContainerScreen<CardBinderMenu> {

    /** 列表两列（图标 + 事件名都放得下）。 */
    public static final int COLS = 2;
    /** 每页可见行数。 */
    public static final int VISIBLE_ROWS = 6;
    private static final int CELL_H = 26;
    private static final int CELL_GAP_X = 4;
    private static final int CELL_GAP_Y = 2;
    private static final int GRID_Y = 58;
    /** 列表格子里的图标边长（16×16，与物品栏里的观感一致）。 */
    private static final int ICON = 16;

    private int page;

    public QuestionCodexScreen(CardBinderMenu menu, Inventory inventory, Component title) {
        this(menu, inventory, title, 0);
    }

    /** 带页码的构造器：详情页返回时可以回到原来那一页。 */
    public QuestionCodexScreen(CardBinderMenu menu, Inventory inventory, Component title, int page) {
        super(menu, inventory, title);
        this.imageWidth = BinderStyle.WIDTH;
        this.imageHeight = BinderStyle.HEIGHT;
        this.page = Math.max(0, page);
    }

    /** 一页能放几个事件。 */
    public static int perPage() {
        return COLS * VISIBLE_ROWS;
    }

    /** {@code count} 个事件要几页（空列表也算 1 页）。 */
    public static int pagesFor(int count) {
        return Math.max(1, (Math.max(0, count) + perPage() - 1) / perPage());
    }

    /**
     * 图鉴列表里的全部事件（顺序 = 登记顺序）。
     *
     * <p>抽成静态方法，自检可以不碰客户端界面就核对"列表长度 == 事件总数"。</p>
     */
    public static List<QuestionEvent> events() {
        return QuestionLibrary.all();
    }

    @Override
    public void init() {
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
        Component heading = Component.translatable("stc.codex.question.heading");
        TextStyle.draw(graphics, font, heading, leftPos + BinderStyle.GRID_X, topPos + 40, BinderStyle.INK);

        List<QuestionEvent> events = events();
        int pages = pagesFor(events.size());
        clampPage(events.size());
        int cellW = cellWidth();
        int first = page * perPage();
        for (int cell = 0; cell < perPage(); cell++) {
            int index = first + cell;
            if (index >= events.size()) {
                break;
            }
            QuestionEvent event = events.get(index);
            int x = leftPos + BinderStyle.GRID_X + (cell % COLS) * (cellW + CELL_GAP_X);
            int y = topPos + GRID_Y + (cell / COLS) * (CELL_H + CELL_GAP_Y);
            boolean hovered = isInside(mouseX, mouseY, x, y, cellW, CELL_H);
            graphics.fill(x, y, x + cellW, y + CELL_H, hovered ? 0xFFFFF3D6 : 0xFFF2E6C4);
            BinderStyle.border(graphics, x, y, x + cellW, y + CELL_H,
                    hovered ? BinderStyle.CELL_SELECTED : BinderStyle.BOOK_EDGE);
            // 每个问号事件都用望远镜图标（问号事件本身的象征物）
            ItemIcons.draw(graphics, Items.SPYGLASS, x + 4, y + (CELL_H - ICON) / 2, ICON);
            int textX = x + 4 + ICON + 5;
            int textW = Math.max(8, x + cellW - 4 - textX);
            TextStyle.draw(graphics, font, font.plainSubstrByWidth(event.name(), textW), textX, y + 5,
                    BinderStyle.INK);
            String trigger = QuestionLibrary.triggerOf(event.id());
            TextStyle.draw(graphics, font, font.plainSubstrByWidth(trigger, textW), textX, y + 15,
                    BinderStyle.INK_DIM);
        }
        if (events.isEmpty()) {
            Component empty = Component.translatable("stc.codex.question.empty");
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
        int pageW = BinderStyle.pageButtonWidth(imageWidth);
        Component count = Component.translatable("stc.codex.question.count", events.size());
        if (pages <= 1 || BinderStyle.footerCountFits(imageWidth, font.width(count))) {
            TextStyle.draw(graphics, font, count, BinderStyle.footerCountX(leftPos), footer + 3,
                    BinderStyle.INK_DIM);
        }
        if (pages > 1) {
            BinderStyle.button(graphics, font, BinderStyle.prevPageButtonX(imageWidth), footer, pageW, 14,
                    Component.translatable("stc.codex.page.prev").getString(), page > 0);
            BinderStyle.button(graphics, font, BinderStyle.nextPageButtonX(imageWidth), footer, pageW, 14,
                    Component.translatable("stc.codex.page.next").getString(), page < pages - 1);
        }
    }

    private void clampPage(int count) {
        page = Math.max(0, Math.min(pagesFor(count) - 1, page));
    }

    /** 详情页正文：触发方式 → 事件描述 → 每个选项的奖励。 */
    static List<InfoPageScreen.BodyLine> detailLines(QuestionEvent event) {
        List<InfoPageScreen.BodyLine> body = new ArrayList<>();
        String trigger = QuestionLibrary.triggerOf(event.id());
        if (!trigger.isEmpty()) {
            body.add(new InfoPageScreen.BodyLine(
                    Component.translatable("stc.codex.question.trigger", trigger).getString(),
                    BinderStyle.INK, Items.SPYGLASS));
        }
        for (String paragraph : event.description()) {
            if (!paragraph.isEmpty()) {
                body.add(new InfoPageScreen.BodyLine(paragraph));
            }
        }
        body.add(new InfoPageScreen.BodyLine(
                Component.translatable("stc.codex.question.rewards").getString(), BinderStyle.ACCENT));
        for (QuestionOption option : event.options()) {
            body.add(new InfoPageScreen.BodyLine(
                    Component.translatable("stc.codex.question.option", option.label(), option.description())
                            .getString(),
                    BinderStyle.INK));
            // 需要先选牌的选项：把"要选什么牌"也说清楚，否则玩家不知道点下去会发生什么
            if (option.needsCard()) {
                body.add(new InfoPageScreen.BodyLine("    " + option.pickHint(), BinderStyle.INK_DIM));
            }
        }
        return body;
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
        List<QuestionEvent> events = events();
        int pages = pagesFor(events.size());
        if (pages > 1) {
            int pageW = BinderStyle.pageButtonWidth(imageWidth);
            if (isInside(relX, relY, BinderStyle.prevPageButtonX(imageWidth), footer, pageW, 14)) {
                page = Math.max(0, page - 1);
                return true;
            }
            if (isInside(relX, relY, BinderStyle.nextPageButtonX(imageWidth), footer, pageW, 14)) {
                page = Math.min(pages - 1, page + 1);
                return true;
            }
        }
        int cellW = cellWidth();
        int first = page * perPage();
        for (int cell = 0; cell < perPage(); cell++) {
            int index = first + cell;
            if (index >= events.size()) {
                break;
            }
            int x = BinderStyle.GRID_X + (cell % COLS) * (cellW + CELL_GAP_X);
            int y = GRID_Y + (cell / COLS) * (CELL_H + CELL_GAP_Y);
            if (isInside(relX, relY, x, y, cellW, CELL_H)) {
                QuestionEvent event = events.get(index);
                minecraft.setScreen(InfoPageScreen.rich(menu, minecraft.player.getInventory(), title,
                        Component.literal(event.name()), detailLines(event), null, null, this::samePage));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int pages = pagesFor(events().size());
        if (pages > 1) {
            page = Math.max(0, Math.min(pages - 1, page + (scrollY > 0 ? -1 : 1)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /** 重建"本页"（详情页返回时用，页码原样带上）。 */
    private QuestionCodexScreen samePage() {
        return new QuestionCodexScreen(menu, minecraft.player.getInventory(), title, page);
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
