package com.slaythecraft.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.slaythecraft.binder.CardBinderMenu;
import com.slaythecraft.binder.CardDeleteService;
import com.slaythecraft.combat.core.Relic;
import com.slaythecraft.combat.core.RelicLibrary;
import com.slaythecraft.data.PlayerCardData;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 「拥有遗物」子页面（v0.2.7：点遗物进入**独立的详情页**，描述全文折行显示）。
 *
 * <p>反馈："存在会因为遗物描述文字超出显示范围而导致显示不全的问题。" 列表里只显示名字，
 * 完整描述放到新开的详情页并按宽度折行，多长都能看全。</p>
 */
public class BinderRelicsScreen extends AbstractContainerScreen<CardBinderMenu> {

    private static final int SLOTS = 12;
    private static final int ACCENT = 0xFF6B4E9A;

    public BinderRelicsScreen(CardBinderMenu menu, Inventory inventory, Component title) {
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
        PlayerCardData data = BinderStyle.data();
        if (data == null) {
            return;
        }
        List<Relic> relics = RelicLibrary.relicsOf(data.relics());
        if (relics.isEmpty()) {
            Component empty = Component.translatable("stc.binder.relics.empty");
            TextStyle.draw(graphics, font, empty, leftPos + (imageWidth - font.width(empty)) / 2, topPos + 110,
                    BinderStyle.INK_DIM);
        }
        for (int i = 0; i < relics.size() && i < SLOTS; i++) {
            Relic relic = relics.get(i);
            int x = leftPos + BinderStyle.GRID_X + (i % 2) * 100;
            int y = topPos + 50 + (i / 2) * 22;
            boolean hovered = mouseX >= x && mouseX < x + 96 && mouseY >= y && mouseY < y + 20;
            graphics.fill(x, y, x + 96, y + 20, hovered ? 0xFFFFF3D6 : 0xFFF2E6C4);
            BinderStyle.border(graphics, x, y, x + 96, y + 20, hovered ? BinderStyle.BOOK_EDGE : ACCENT);
            TextStyle.draw(graphics, font, font.plainSubstrByWidth(relic.name(), 90), x + 3, y + 6, BinderStyle.INK);
        }
        if (relics.size() > SLOTS) {
            Component more = Component.translatable("stc.binder.relics.more", relics.size() - SLOTS);
            TextStyle.draw(graphics, font, more, leftPos + BinderStyle.GRID_X, topPos + 50 + (SLOTS / 2) * 22 + 4,
                    BinderStyle.INK_DIM);
        }
        TextStyle.draw(graphics, font, Component.translatable("stc.binder.relics.tip"), leftPos + BinderStyle.GRID_X,
                topPos + imageHeight - 44, BinderStyle.ACCENT);

        int y = topPos + BinderStyle.footerY(imageHeight);
        BinderStyle.button(graphics, font, leftPos + BinderStyle.GRID_X, y, 52, 14,
                Component.translatable("stc.binder.back").getString(), true);
        Component count = Component.translatable("stc.binder.relics.count", relics.size());
        TextStyle.draw(graphics, font, count, leftPos + BinderStyle.GRID_X + 58, y + 3, BinderStyle.INK_DIM);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        PlayerCardData data = BinderStyle.data();
        if (data == null || minecraft == null || minecraft.player == null) {
            return super.mouseClicked(mouseX, mouseY, mouseButton);
        }
        int relX = (int) mouseX - leftPos;
        int relY = (int) mouseY - topPos;
        if (relX >= BinderStyle.GRID_X && relX < BinderStyle.GRID_X + 52
                && relY >= BinderStyle.footerY(imageHeight) && relY < BinderStyle.footerY(imageHeight) + 14) {
            minecraft.setScreen(new CardBinderScreen(menu, minecraft.player.getInventory(), title));
            return true;
        }
        List<Relic> relics = RelicLibrary.relicsOf(data.relics());
        for (int i = 0; i < relics.size() && i < SLOTS; i++) {
            int x = BinderStyle.GRID_X + (i % 2) * 100;
            int y = 50 + (i / 2) * 22;
            if (relX >= x && relX < x + 96 && relY >= y && relY < y + 20) {
                openDetail(relics.get(i));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    /** 遗物详情：**新开一页**，描述按宽度折行（多长都看得全）。 */
    private void openDetail(Relic relic) {
        List<String> body = new ArrayList<>();
        body.add(relic.description());
        body.add("ID：" + relic.id());
        Supplier<Screen> back = () -> this;
        minecraft.setScreen(new InfoPageScreen<>(menu, minecraft.player.getInventory(), title,
                Component.literal(relic.name()), body, null, null, back));
    }
}
