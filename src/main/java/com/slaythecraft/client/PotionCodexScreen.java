package com.slaythecraft.client;

import java.util.List;

import com.slaythecraft.binder.CardBinderMenu;
import com.slaythecraft.combat.core.Potion;
import com.slaythecraft.combat.core.PotionLibrary;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 「药水图鉴」页（v0.4.6.7 新图鉴）。
 *
 * <p>需求原文："新图鉴：药水图鉴 —— 按钮显示药水图标和名称"，
 * "详情页面显示效果描述，图标，名称，和机制。"</p>
 *
 * <p>数据是 {@link PotionLibrary#all()}（全部药水，一瓶不落）。列表按钮上的图标直接用
 * {@link PotionIcons#draw} —— 和 {@code CardShopScreen} 画商店药水货架**同一套**画法，
 * 所以喷溅 / 滞留 / 常规贴图，以及"用物品贴图"的两瓶炖菜（{@code PotionForm.ITEM}）
 * 都会自动画对。点进去是**新页面** {@link PotionDetailScreen}（沿用 v0.2.6 的约定）。</p>
 */
public class PotionCodexScreen extends AbstractContainerScreen<CardBinderMenu> {

    /** 列表两列（瓶子 16×16 + 名字都放得下）。 */
    public static final int COLS = 2;
    /** 每页可见行数。 */
    public static final int VISIBLE_ROWS = 6;
    private static final int CELL_H = 26;
    private static final int CELL_GAP_X = 4;
    private static final int CELL_GAP_Y = 2;
    private static final int GRID_Y = 58;
    /** 列表格子里的药水图标边长（16×16）。 */
    private static final int ICON = 16;

    private int page;

    public PotionCodexScreen(CardBinderMenu menu, Inventory inventory, Component title) {
        this(menu, inventory, title, 0);
    }

    /** 带页码的构造器：详情页返回时可以回到原来那一页。 */
    public PotionCodexScreen(CardBinderMenu menu, Inventory inventory, Component title, int page) {
        super(menu, inventory, title);
        this.imageWidth = BinderStyle.WIDTH;
        this.imageHeight = BinderStyle.HEIGHT;
        this.page = Math.max(0, page);
    }

    /** 每行几个（公开静态方法，自检可以直接算）。 */
    public static int columns() {
        return COLS;
    }

    /** 一页能放几瓶。 */
    public static int perPage() {
        return COLS * VISIBLE_ROWS;
    }

    /** {@code count} 瓶药水要几页（空列表也算 1 页）。 */
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
        Component heading = Component.translatable("stc.codex.potion.heading");
        TextStyle.draw(graphics, font, heading, leftPos + BinderStyle.GRID_X, topPos + 40, BinderStyle.INK);

        List<Potion> potions = PotionLibrary.all();
        int pages = pagesFor(potions.size());
        clampPage(potions.size());
        int cellW = cellWidth();
        int first = page * perPage();
        for (int cell = 0; cell < perPage(); cell++) {
            int index = first + cell;
            if (index >= potions.size()) {
                break;
            }
            Potion potion = potions.get(index);
            int x = leftPos + BinderStyle.GRID_X + (cell % COLS) * (cellW + CELL_GAP_X);
            int y = topPos + GRID_Y + (cell / COLS) * (CELL_H + CELL_GAP_Y);
            boolean hovered = isInside(mouseX, mouseY, x, y, cellW, CELL_H);
            graphics.fill(x, y, x + cellW, y + CELL_H, hovered ? 0xFFFFF3D6 : 0xFFF2E6C4);
            BinderStyle.border(graphics, x, y, x + cellW, y + CELL_H,
                    hovered ? BinderStyle.CELL_SELECTED : BinderStyle.BOOK_EDGE);
            // 需求："按钮显示药水图标和名称。"（图标画法与商店药水货架完全一致）
            PotionIcons.draw(graphics, potion, x + 4, y + (CELL_H - ICON) / 2, 1.0F);
            TextStyle.draw(graphics, font, potion.name(), x + 4 + ICON + 5, y + (CELL_H - 8) / 2, BinderStyle.INK);
        }
        if (potions.isEmpty()) {
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
        Component count = Component.translatable("stc.codex.potion.count", potions.size());
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
        List<Potion> potions = PotionLibrary.all();
        int pages = pagesFor(potions.size());
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
            if (index >= potions.size()) {
                break;
            }
            int x = BinderStyle.GRID_X + (cell % COLS) * (cellW + CELL_GAP_X);
            int y = GRID_Y + (cell / COLS) * (CELL_H + CELL_GAP_Y);
            if (isInside(relX, relY, x, y, cellW, CELL_H)) {
                minecraft.setScreen(new PotionDetailScreen(menu, minecraft.player.getInventory(), title,
                        potions.get(index), this::samePage));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int pages = pagesFor(PotionLibrary.all().size());
        if (pages > 1) {
            page = Math.max(0, Math.min(pages - 1, page + (scrollY > 0 ? -1 : 1)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /** 重建"本页"（详情页返回时用，页码原样带上）。 */
    private PotionCodexScreen samePage() {
        return new PotionCodexScreen(menu, minecraft.player.getInventory(), title, page);
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
