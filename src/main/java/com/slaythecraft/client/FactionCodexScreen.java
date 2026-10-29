package com.slaythecraft.client;

import java.util.ArrayList;
import java.util.List;

import com.slaythecraft.binder.CardBinderMenu;
import com.slaythecraft.combat.core.Faction;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 「敌人阵营图鉴」（v0.4.1）。
 *
 * <p>需求："添加：敌人阵营及图鉴。将阵营的描述加入图鉴。图鉴介绍：不同的敌人也有帮派，或者组织，
 * 管他们是什么呢。"</p>
 *
 * <p>每个阵营一行：名字 + 成员 + 特性；点进去是独立详情页（描述 / 成员 / 特性全文）。</p>
 */
public class FactionCodexScreen extends AbstractContainerScreen<CardBinderMenu> {

    private static final int ROW_X = 12;
    private static final int ROW_W = 244;
    private static final int ROW_GAP = 4;
    private static final int ROW_Y = 60;

    /**
     * v0.4.3：每行的高度按可用空间算（面板被屏幕压扁时也不会顶出背景）。
     *
     * <p>反馈原文："阵营图鉴超出背景区域。"</p>
     */
    private int rowHeight() {
        int available = imageHeight - ROW_Y - 40;
        int rows = Math.max(1, Faction.all().size());
        return Math.max(16, Math.min(30, available / rows - ROW_GAP));
    }

    public FactionCodexScreen(CardBinderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = BinderStyle.WIDTH;
        this.imageHeight = BinderStyle.HEIGHT;
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

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        BinderStyle.header(graphics, font, leftPos, topPos);
        Component intro = Component.translatable("stc.codex.faction.intro");
        TextStyle.draw(graphics, font, font.plainSubstrByWidth(intro.getString(), imageWidth - 24),
                leftPos + ROW_X, topPos + 38, BinderStyle.INK_DIM);

        List<Faction> factions = Faction.all();
        for (int i = 0; i < factions.size(); i++) {
            Faction faction = factions.get(i);
            int x = leftPos + ROW_X;
            int rowH = rowHeight();
            int y = topPos + ROW_Y + i * (rowH + ROW_GAP);
            boolean hovered = mouseX >= x && mouseX < x + ROW_W && mouseY >= y && mouseY < y + rowH;
            graphics.fill(x, y, x + ROW_W, y + rowH, hovered ? 0xFFFFF3D6 : 0xFFF2E6C4);
            BinderStyle.border(graphics, x, y, x + ROW_W, y + rowH,
                    hovered ? BinderStyle.CELL_SELECTED : BinderStyle.BOOK_EDGE);
            // v0.4.6.3 需求："阵营选择页面，标题下方不再显示怪物名称，只显示阵营名称。"
            // 所以这一行只有阵营名（成员名单在阵营详情页里看），文字在行内垂直居中。
            int labelY = y + (rowH - 8) / 2;
            TextStyle.draw(graphics, font, faction.label(), x + 6, labelY, BinderStyle.INK);
            Component open = Component.translatable("stc.codex.open");
            TextStyle.draw(graphics, font, open, x + ROW_W - 6 - font.width(open), labelY, BinderStyle.ACCENT);
        }

        BinderStyle.button(graphics, font, leftPos + BinderStyle.GRID_X, topPos + BinderStyle.footerY(imageHeight), 52, 14,
                Component.translatable("stc.binder.back").getString(), true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (minecraft == null || minecraft.player == null) {
            return super.mouseClicked(mouseX, mouseY, mouseButton);
        }
        int relX = (int) mouseX - leftPos;
        int relY = (int) mouseY - topPos;
        if (relX >= BinderStyle.GRID_X && relX < BinderStyle.GRID_X + 52
                && relY >= BinderStyle.footerY(imageHeight) && relY < BinderStyle.footerY(imageHeight) + 14) {
            minecraft.setScreen(new CodexScreen(menu, minecraft.player.getInventory(), title));
            return true;
        }
        List<Faction> factions = Faction.all();
        int rowH = rowHeight();
        for (int i = 0; i < factions.size(); i++) {
            int y = ROW_Y + i * (rowH + ROW_GAP);
            if (relX >= ROW_X && relX < ROW_X + ROW_W && relY >= y && relY < y + rowH) {
                openDetail(factions.get(i));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    /**
     * 阵营详情（新页面，v0.4.6 按需求重做）：上面是描述 + 特性，下面是该阵营的**怪物图鉴**。
     */
    private void openDetail(Faction faction) {
        minecraft.setScreen(new FactionDetailScreen(menu, minecraft.player.getInventory(), title, faction));
    }
}
