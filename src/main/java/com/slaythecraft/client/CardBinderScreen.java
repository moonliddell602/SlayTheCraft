package com.slaythecraft.client;

import com.slaythecraft.binder.CardBinderMenu;
import com.slaythecraft.data.PlayerCardData;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * 卡牌收纳包**主页**（v0.2.6：子页面全部独立成新页面）。
 *
 * <p>反馈："以后，默认任何有子页面的内容，其母页面的查看详情等引导都默认新建页面 UI 显示子页面，
 * 以防出现文本堆叠等问题。" 所以主页只做一件事：摆三个功能模块（拥有卡牌 / 拥有遗物 / 事件回合统计），
 * 点哪一个就**换一个界面**，不再在同一个界面里切视图。</p>
 */
public class CardBinderScreen extends AbstractContainerScreen<CardBinderMenu> {

    private static final int MODULE_X = 16;
    private static final int MODULE_W = 200;
    // v0.4.3：面板变大后模块也放大一点（更好点）
    private static final int MODULE_H = 30;
    private static final int MODULE_GAP = 10;
    private static final int MODULE_Y = 62;

    public CardBinderScreen(CardBinderMenu menu, Inventory inventory, Component title) {
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

        Component heading = Component.translatable("stc.binder.home.heading");
        TextStyle.draw(graphics, font, heading, leftPos + MODULE_X, topPos + 40, BinderStyle.INK);

        int y = topPos + MODULE_Y;
        // v0.5.1.2 美术需求："收纳包主页 4 个（查看牌组=minecraft:book、查看遗物=minecraft:chest、
        // 事件回合统计=minecraft:compass、查看图鉴=minecraft:writable_book）"。
        module(graphics, y, Items.BOOK,
                Component.translatable("stc.binder.module.cards", data.cardCount()).getString(),
                Component.translatable(menu.mode() == CardBinderMenu.Mode.RECYCLER
                        ? "stc.binder.module.cards.hint.recycler" : "stc.binder.module.cards.hint").getString());
        module(graphics, y + MODULE_H + MODULE_GAP, Items.CHEST,
                Component.translatable("stc.binder.module.relics", data.relicCount()).getString(),
                Component.translatable("stc.binder.module.relics.hint").getString());
        module(graphics, y + (MODULE_H + MODULE_GAP) * 2, Items.COMPASS,
                Component.translatable("stc.binder.module.stats", data.floor()).getString(),
                Component.translatable("stc.binder.module.stats.hint").getString());
        // v0.4.0：第四个模块 —— 查看图鉴（预览模组里全部卡牌与遗物）
        module(graphics, y + (MODULE_H + MODULE_GAP) * 3, Items.WRITABLE_BOOK,
                Component.translatable("stc.binder.module.codex").getString(),
                Component.translatable("stc.binder.module.codex.hint").getString());

        int noteY = y + (MODULE_H + MODULE_GAP) * 4;
        Component note = data.pendingRewards() > 0
                ? Component.translatable("stc.binder.reward.pending", data.pendingRewards())
                : Component.translatable("stc.binder.reward.none");
        TextStyle.draw(graphics, font, note, leftPos + MODULE_X, noteY + 4,
                data.pendingRewards() > 0 ? BinderStyle.ACCENT : BinderStyle.INK_DIM);
        Component noteHint = Component.translatable("stc.binder.reward.hint");
        TextStyle.draw(graphics, font, noteHint, leftPos + MODULE_X, noteY + 16, BinderStyle.INK_DIM);
        Component hint = Component.translatable("stc.binder.home.hint");
        TextStyle.draw(graphics, font, hint, leftPos + MODULE_X, topPos + BinderStyle.footerY(imageHeight), BinderStyle.INK_DIM);
    }

    /** 一个功能模块：左侧小图标 + 标题 + 一行说明（v0.5.1.2 加图标）。 */
    private void module(GuiGraphics graphics, int y, Item icon, String label, String hint) {
        int x = leftPos + MODULE_X;
        graphics.fill(x, y, x + MODULE_W, y + MODULE_H, 0xFFF2E6C4);
        BinderStyle.border(graphics, x, y, x + MODULE_W, y + MODULE_H, BinderStyle.BOOK_EDGE);
        // 图标竖着居中在 30 高的模块里（16 高 → 上边距 7）
        ItemIcons.draw(graphics, icon, x + 8, y + (MODULE_H - ItemIcons.NATIVE) / 2, ItemIcons.NATIVE);
        int textX = x + 8 + ItemIcons.NATIVE + 6;
        // 文字可用宽度跟着图标一起右移，长标题不会被挤到边框外
        int textW = Math.max(16, x + MODULE_W - 8 - textX);
        TextStyle.draw(graphics, font, font.plainSubstrByWidth(label, textW), textX, y + 7, BinderStyle.INK);
        TextStyle.draw(graphics, font, font.plainSubstrByWidth(hint, textW), textX, y + 18, BinderStyle.INK_DIM);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        PlayerCardData data = BinderStyle.data();
        if (data == null || minecraft == null) {
            return super.mouseClicked(mouseX, mouseY, mouseButton);
        }
        int relX = (int) mouseX - leftPos;
        int relY = (int) mouseY - topPos;
        for (int i = 0; i < 4; i++) {
            int y = MODULE_Y + i * (MODULE_H + MODULE_GAP);
            if (relX < MODULE_X || relX >= MODULE_X + MODULE_W || relY < y || relY >= y + MODULE_H) {
                continue;
            }
            // v0.2.6：子页面一律**新建页面**，不在主页里切视图（避免文字堆叠）
            // v0.2.7：卡牌页在收纳包里是"只看"，删除只在卡牌回收站 / 卡牌商人处提供
            boolean recycler = menu.mode() == CardBinderMenu.Mode.RECYCLER;
            switch (i) {
                case 0 -> minecraft.setScreen(new CardPickScreen<>(menu, minecraft.player.getInventory(), title,
                        recycler, recycler ? Component.translatable("stc.binder.delete.hint").getString() : null,
                        index -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, index),
                        () -> this));
                case 1 -> minecraft.setScreen(new BinderRelicsScreen(menu, minecraft.player.getInventory(), title));
                case 2 -> minecraft.setScreen(new BinderProgressScreen(menu, minecraft.player.getInventory(), title));
                default -> minecraft.setScreen(new CodexScreen(menu, minecraft.player.getInventory(), title));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }
}
