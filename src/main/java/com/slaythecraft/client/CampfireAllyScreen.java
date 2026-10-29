package com.slaythecraft.client;

import java.util.List;

import com.slaythecraft.campfire.CampfireMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 「包扎」选队友页（v0.0.3 W5，独立新页面）。
 *
 * <p>名单来自服务端（客户端不自己算队伍），点一个名字就给那个人回复 25% 生命。</p>
 */
public class CampfireAllyScreen extends AbstractContainerScreen<CampfireMenu> {

    private static final int PANEL_BG = 0xF01C1C22;
    private static final int PANEL_EDGE = 0xFF5A5A64;
    private static final int TEXT = 0xFFE8E8E8;
    private static final int TEXT_DIM = 0xFF9A9AA4;
    private static final int GOLD = 0xFFFFD24A;

    private static final int ROW_H = 18;
    private static final int ROW_Y = 46;
    private static final int ROW_X = 24;
    private static final int ROW_W = 184;

    private final List<String> allies;

    public CampfireAllyScreen(CampfireMenu menu, Inventory inventory, Component title, List<String> allies) {
        super(menu, inventory, title);
        this.imageWidth = 232;
        this.imageHeight = 190;
        this.allies = List.copyOf(allies);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x2 = leftPos + imageWidth;
        int y2 = topPos + imageHeight;
        graphics.fill(leftPos - 2, topPos - 2, x2 + 2, y2 + 2, PANEL_EDGE);
        graphics.fill(leftPos, topPos, x2, y2, PANEL_BG);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        Component title = Component.translatable("stc.campfire.bandage.title");
        TextStyle.draw(graphics, font, title, leftPos + (imageWidth - font.width(title)) / 2, topPos + 12, GOLD);
        if (allies.isEmpty()) {
            Component none = Component.translatable("stc.campfire.bandage.none");
            TextStyle.draw(graphics, font, none, leftPos + 12, topPos + ROW_Y, TEXT_DIM);
        }
        for (int i = 0; i < allies.size(); i++) {
            int y = topPos + ROW_Y + i * ROW_H;
            boolean hovered = mouseX >= leftPos + ROW_X && mouseX < leftPos + ROW_X + ROW_W
                    && mouseY >= y && mouseY < y + ROW_H - 2;
            graphics.fill(leftPos + ROW_X, y, leftPos + ROW_X + ROW_W, y + ROW_H - 2,
                    hovered ? 0xFF3A3A48 : 0xFF26262E);
            border(graphics, leftPos + ROW_X, y, leftPos + ROW_X + ROW_W, y + ROW_H - 2,
                    hovered ? GOLD : PANEL_EDGE);
            TextStyle.draw(graphics, font, allies.get(i), leftPos + ROW_X + 6, y + 4, hovered ? GOLD : TEXT);
        }
        BinderStyle.button(graphics, font, leftPos + 12, topPos + imageHeight - 28, 52, 14,
                Component.translatable("stc.binder.back").getString(), true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (minecraft == null || minecraft.player == null) {
            return super.mouseClicked(mouseX, mouseY, mouseButton);
        }
        int relX = (int) mouseX - leftPos;
        int relY = (int) mouseY - topPos;
        if (relX >= 12 && relX < 64 && relY >= imageHeight - 28 && relY < imageHeight - 14) {
            minecraft.setScreen(new CampfireScreen(menu, minecraft.player.getInventory(), title));
            return true;
        }
        for (int i = 0; i < allies.size(); i++) {
            int y = ROW_Y + i * ROW_H;
            if (relX >= ROW_X && relX < ROW_X + ROW_W && relY >= y && relY < y + ROW_H - 2) {
                if (minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, CampfireMenu.ALLY_BASE + i);
                }
                minecraft.setScreen(null);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void border(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        graphics.fill(x1, y1, x2, y1 + 1, color);
        graphics.fill(x1, y2 - 1, x2, y2, color);
        graphics.fill(x1, y1, x1 + 1, y2, color);
        graphics.fill(x2 - 1, y1, x2, y2, color);
    }
}
