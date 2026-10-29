package com.slaythecraft.client;

import com.slaythecraft.binder.CardBinderMenu;
import com.slaythecraft.character.CharacterProfile;
import com.slaythecraft.character.CharacterRegistry;
import com.slaythecraft.combat.core.CardLibrary;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 「卡牌图鉴 → 选择角色」页（v0.4.0）。
 *
 * <p>需求："卡牌图鉴需要按照角色分开放出（选择卡牌图鉴→选择要查看的角色→…）"。
 * 每个角色一张卡片，点进去看**那个角色能用的牌**。</p>
 */
public class CardCodexCharactersScreen extends AbstractContainerScreen<CardBinderMenu> {

    private static final int ROW_X = 16;
    private static final int ROW_W = 236;
    private static final int ROW_GAP = 6;
    private static final int ROW_Y = 58;

    /** v0.4.3：行高跟着面板高度（面板更大时卡片更高）。 */
    private int rowHeight() {
        int available = imageHeight - ROW_Y - 44;
        int rows = Math.max(1, CharacterRegistry.all().size());
        return Math.max(20, Math.min(34, available / rows - ROW_GAP));
    }

    public CardCodexCharactersScreen(CardBinderMenu menu, Inventory inventory, Component title) {
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
        Component heading = Component.translatable("stc.codex.cards.choose");
        TextStyle.draw(graphics, font, heading, leftPos + ROW_X, topPos + 40, BinderStyle.INK);

        int index = 0;
        for (CharacterProfile profile : CharacterRegistry.all()) {
            int x = leftPos + ROW_X;
            int rowH = rowHeight();
            int y = topPos + ROW_Y + index * (rowH + ROW_GAP);
            boolean hovered = mouseX >= x && mouseX < x + ROW_W && mouseY >= y && mouseY < y + rowH;
            graphics.fill(x, y, x + ROW_W, y + rowH, hovered ? 0xFFFFF3D6 : 0xFFF2E6C4);
            BinderStyle.border(graphics, x, y, x + ROW_W, y + rowH,
                    hovered ? BinderStyle.CELL_SELECTED : BinderStyle.BOOK_EDGE);
            Component name = Component.translatable(profile.nameKey());
            TextStyle.draw(graphics, font, name, x + 8, y + 4, BinderStyle.INK);
            Component count = Component.translatable("stc.codex.cards.count",
                    CardLibrary.codexFor(profile.id().toString()).size());
            TextStyle.draw(graphics, font, count, x + 8, y + 16, BinderStyle.INK_DIM);
            Component open = Component.translatable("stc.codex.open");
            TextStyle.draw(graphics, font, open, x + ROW_W - 8 - font.width(open), y + 8, BinderStyle.ACCENT);
            index++;
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
        int index = 0;
        int rowH = rowHeight();
        for (CharacterProfile profile : CharacterRegistry.all()) {
            int y = ROW_Y + index * (rowH + ROW_GAP);
            if (relX >= ROW_X && relX < ROW_X + ROW_W && relY >= y && relY < y + rowH) {
                minecraft.setScreen(new CardCodexScreen(menu, minecraft.player.getInventory(), title, index));
                return true;
            }
            index++;
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }
}
