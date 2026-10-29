package com.slaythecraft.client;

import java.util.List;

import com.slaythecraft.combat.core.Card;
import com.slaythecraft.combat.core.CardLibrary;
import com.slaythecraft.combat.core.Potion;
import com.slaythecraft.combat.core.PotionLibrary;
import com.slaythecraft.combat.core.Relic;
import com.slaythecraft.combat.core.RelicLibrary;
import com.slaythecraft.data.PlayerCardData;
import com.slaythecraft.data.StcAttachments;
import com.slaythecraft.merchant.CardShopMenu;
import com.slaythecraft.merchant.CardShopOffer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/**
 * 卡牌大师商店界面（设计文档 §6.4；v0.4.6 按需求扩充）。
 *
 * <p>需求原文："用本模组的独立ui代替原版交易面板。售卖：5张卡牌 三种遗物 3种药水 一次删牌机会。
 * 花费 40 金币可以刷新除删牌机会外的其他物资，最多三次。"</p>
 *
 * <p>货架数据由服务端推送（{@link ClientShopOffers}），客户端只发"买第几件 / 刷新 / 删哪张卡"；
 * 金币、遗物重复、药水槽位都由服务端再次校验。</p>
 */
public class CardShopScreen extends AbstractContainerScreen<CardShopMenu> {

    // 卡牌货架
    private static final int CARD_W = 62;
    private static final int CARD_H = 84;
    private static final int CARD_GAP = 4;
    private static final int CARD_X = 12;
    private static final int CARD_Y = 32;
    // 遗物 / 药水货架（同一套格子尺寸）
    private static final int ITEM_W = 104;
    private static final int ITEM_H = 26;
    private static final int ITEM_GAP = 4;
    private static final int ITEM_X = 12;
    private static final int RELIC_Y = 134;
    private static final int POTION_Y = 176;
    // 底部按钮
    private static final int BUTTON_Y = 206;
    private static final int BUTTON_W = 140;
    private static final int BUTTON_H = 14;

    private static final int PANEL_BG = 0xF01C1C22;
    private static final int PANEL_EDGE = 0xFF5A5A64;
    private static final int CELL_BG = 0xFF26262E;
    private static final int TEXT = 0xFFE8E8E8;
    private static final int TEXT_DIM = 0xFF9A9AA4;
    private static final int GOLD = 0xFFFFD24A;
    private static final int DANGER = 0xFFD05A5A;
    private static final int PURPLE = 0xFFB48AD8;

    public CardShopScreen(CardShopMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 348;
        this.imageHeight = 224;
    }

    private PlayerCardData data() {
        Player player = Minecraft.getInstance().player;
        return player == null ? null : StcAttachments.dataOf(player);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos - 1, topPos - 1, leftPos + imageWidth + 1, topPos + imageHeight + 1, PANEL_EDGE);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL_BG);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        TextStyle.draw(graphics, font, title, leftPos + 10, topPos + 8, TEXT);

        PlayerCardData data = data();
        int id = menu.containerId;
        if (data != null) {
            Component gold = Component.translatable("stc.binder.gold", data.gold());
            TextStyle.draw(graphics, font, gold, leftPos + imageWidth - 10 - font.width(gold), topPos + 8, GOLD);
        }

        drawCards(graphics, mouseX, mouseY, id, data);
        drawItems(graphics, mouseX, mouseY, id, data);
        drawFooter(graphics, id, data);
    }

    // ------------------------------------------------------------------ 卡牌货架

    private void drawCards(GuiGraphics graphics, int mouseX, int mouseY, int id, PlayerCardData data) {
        List<CardShopOffer> offers = ClientShopOffers.offersFor(id);
        TextStyle.draw(graphics, font, Component.translatable("stc.shop.section.cards").getString(),
                leftPos + CARD_X, topPos + 22, TEXT_DIM);
        if (offers.isEmpty()) {
            TextStyle.draw(graphics, font, Component.translatable("stc.shop.empty").getString(),
                    leftPos + CARD_X, topPos + CARD_Y + 30, TEXT_DIM);
        }
        for (int i = 0; i < offers.size(); i++) {
            CardShopOffer offer = offers.get(i);
            int x = leftPos + CARD_X + i * (CARD_W + CARD_GAP);
            int y = topPos + CARD_Y;
            Card definition = CardLibrary.byId(offer.cardId().getPath());
            int accent = definition == null ? PANEL_EDGE : definition.rarity().color();
            boolean affordable = data != null && data.gold() >= offer.price();
            boolean hovered = isInside(mouseX, mouseY, x, y, CARD_W, CARD_H);

            graphics.fill(x, y, x + CARD_W, y + CARD_H, CELL_BG);
            border(graphics, x, y, x + CARD_W, y + CARD_H, hovered ? GOLD : (affordable ? accent : PANEL_EDGE));
            // v0.4.6：卡面插画位（与战斗内手牌同一套贴图规则）
            CardArt.draw(graphics, offer.cardId(), x + 2, y + 2, CARD_W - 4, 26);

            String name = definition == null ? offer.cardId().getPath() : definition.name();
            name = font.plainSubstrByWidth(name, CARD_W - 6);
            TextStyle.draw(graphics, font, name, x + (CARD_W - font.width(name)) / 2, y + 31, TEXT);
            if (definition != null) {
                TextStyle.draw(graphics, font, definition.rarity().label(),
                        x + (CARD_W - font.width(definition.rarity().label())) / 2, y + 43, accent);
                String cost = Component.translatable("stc.reward.energy", definition.cost()).getString();
                TextStyle.draw(graphics, font, cost, x + (CARD_W - font.width(cost)) / 2, y + 54, TEXT_DIM);
            }
            Component price = Component.translatable("stc.shop.price", offer.price());
            TextStyle.draw(graphics, font, price, x + (CARD_W - font.width(price)) / 2, y + 64,
                    affordable ? GOLD : DANGER);
        }
    }

    // ------------------------------------------------------------------ 遗物 / 药水货架

    private void drawItems(GuiGraphics graphics, int mouseX, int mouseY, int id, PlayerCardData data) {
        List<String> relics = ClientShopOffers.relicsFor(id);
        List<String> potions = ClientShopOffers.potionsFor(id);
        int relicPrice = ClientShopOffers.relicPriceFor(id);
        int potionPrice = ClientShopOffers.potionPriceFor(id);

        TextStyle.draw(graphics, font, Component.translatable("stc.shop.section.relics").getString(),
                leftPos + ITEM_X, topPos + RELIC_Y - 10, TEXT_DIM);
        for (int i = 0; i < relics.size(); i++) {
            Relic relic = RelicLibrary.byId(relics.get(i));
            drawItemCell(graphics, mouseX, mouseY, leftPos + ITEM_X + i * (ITEM_W + ITEM_GAP),
                    topPos + RELIC_Y, relic == null ? relics.get(i) : relic.name(),
                    relic == null ? "?" : relic.description(), relicPrice, data, PURPLE);
        }

        TextStyle.draw(graphics, font, Component.translatable("stc.shop.section.potions").getString(),
                leftPos + ITEM_X, topPos + POTION_Y - 10, TEXT_DIM);
        for (int i = 0; i < potions.size(); i++) {
            Potion potion = PotionLibrary.byId(potions.get(i));
            int x = leftPos + ITEM_X + i * (ITEM_W + ITEM_GAP);
            int y = topPos + POTION_Y;
            drawItemCell(graphics, mouseX, mouseY, x, y, potion == null ? potions.get(i) : potion.name(),
                    potion == null ? "?" : potion.description(), potionPrice, data, 0xFF5AD05A);
            // v0.4.6：药水用原版贴图（按目标类型选常规 / 喷溅 / 滞留）
            if (potion != null) {
                PotionIcons.draw(graphics, potion, x + 2, y + 7, 0.75F);
            }
        }
    }

    private void drawItemCell(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, String name,
                              String description, int price, PlayerCardData data, int accent) {
        boolean affordable = data != null && data.gold() >= price;
        boolean hovered = isInside(mouseX, mouseY, x, y, ITEM_W, ITEM_H);
        graphics.fill(x, y, x + ITEM_W, y + ITEM_H, CELL_BG);
        border(graphics, x, y, x + ITEM_W, y + ITEM_H, hovered ? GOLD : accent);
        String shown = font.plainSubstrByWidth(name, ITEM_W - 50);
        TextStyle.draw(graphics, font, shown, x + 16, y + 4, hovered ? GOLD : TEXT);
        Component priceText = Component.literal(price + " 金币");
        TextStyle.draw(graphics, font, priceText, x + ITEM_W - 4 - font.width(priceText), y + 4,
                affordable ? GOLD : DANGER);
        String hint = font.plainSubstrByWidth(description, ITEM_W - 22);
        TextStyle.draw(graphics, font, hint, x + 16, y + 15, TEXT_DIM);
    }

    // ------------------------------------------------------------------ 底部按钮

    private void drawFooter(GuiGraphics graphics, int id, PlayerCardData data) {
        int refreshCost = ClientShopOffers.refreshCostFor(id);
        int refreshLeft = ClientShopOffers.refreshLeftFor(id);
        int deleteCost = ClientShopOffers.deleteCostFor(id);
        boolean canRefresh = data != null && data.gold() >= refreshCost && refreshLeft > 0;
        Component refresh = Component.translatable("stc.shop.refresh", refreshCost, refreshLeft);
        button(graphics, leftPos + ITEM_X, topPos + BUTTON_Y, BUTTON_W, BUTTON_H, refresh.getString(),
                canRefresh);

        // 删牌：价格随"已删张数"上涨（v0.4.6）
        boolean canDelete = data != null && data.cardCount() > 0 && data.gold() >= deleteCost;
        Component delete = Component.translatable("stc.shop.delete_service", deleteCost);
        button(graphics, leftPos + ITEM_X + BUTTON_W + 8, topPos + BUTTON_Y, BUTTON_W, BUTTON_H,
                delete.getString(), canDelete);

        // v0.4.6.3：提示原来右对齐画在按钮那一行，长文案会盖到删牌按钮上；
        // 现在移到**背景边框正下方**并居中（贴着面板外沿，不会再和任何按钮重叠）
        Component hint = Component.translatable("stc.shop.hint2");
        int hintY = Math.min(topPos + imageHeight + 4, height - 12);
        TextStyle.draw(graphics, font, hint, leftPos + (imageWidth - font.width(hint)) / 2, hintY, TEXT_DIM);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        int relX = (int) mouseX - leftPos;
        int relY = (int) mouseY - topPos;
        int id = menu.containerId;

        List<CardShopOffer> offers = ClientShopOffers.offersFor(id);
        for (int i = 0; i < offers.size(); i++) {
            int x = CARD_X + i * (CARD_W + CARD_GAP);
            if (isInside(relX, relY, x, CARD_Y, CARD_W, CARD_H)) {
                click(i);
                return true;
            }
        }
        List<String> relics = ClientShopOffers.relicsFor(id);
        for (int i = 0; i < relics.size(); i++) {
            int x = ITEM_X + i * (ITEM_W + ITEM_GAP);
            if (isInside(relX, relY, x, RELIC_Y, ITEM_W, ITEM_H)) {
                click(CardShopMenu.RELIC_BASE + i);
                return true;
            }
        }
        List<String> potions = ClientShopOffers.potionsFor(id);
        for (int i = 0; i < potions.size(); i++) {
            int x = ITEM_X + i * (ITEM_W + ITEM_GAP);
            if (isInside(relX, relY, x, POTION_Y, ITEM_W, ITEM_H)) {
                click(CardShopMenu.POTION_BASE + i);
                return true;
            }
        }
        if (isInside(relX, relY, ITEM_X, BUTTON_Y, BUTTON_W, BUTTON_H)) {
            click(CardShopMenu.BUTTON_REFRESH);
            return true;
        }
        // 删牌服务 → 新开"选卡页面"（页面里再进详情页/确认页，全部独立页面）
        if (isInside(relX, relY, ITEM_X + BUTTON_W + 8, BUTTON_Y, BUTTON_W, BUTTON_H)) {
            var data = com.slaythecraft.data.StcAttachments.dataOf(minecraft.player);
            if (data.cardCount() > 0) {
                minecraft.setScreen(new CardPickScreen<>(menu, minecraft.player.getInventory(), title, true,
                        Component.translatable("stc.shop.delete_hint").getString(),
                        index -> click(CardShopMenu.DELETE_CARD_BASE + index),
                        () -> this));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void removed() {
        super.removed();
        ClientShopOffers.clear();
    }

    private void click(int buttonId) {
        if (minecraft == null || minecraft.gameMode == null) {
            return;
        }
        minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private void button(GuiGraphics graphics, int x, int y, int width, int height, String label, boolean enabled) {
        graphics.fill(x, y, x + width, y + height, enabled ? 0xFF3A3A48 : 0xFF26262E);
        border(graphics, x, y, x + width, y + height, enabled ? PANEL_EDGE : 0xFF33333C);
        TextStyle.draw(graphics, font, label, x + (width - font.width(label)) / 2, y + (height - 8) / 2,
                enabled ? TEXT : TEXT_DIM);
    }

    private void border(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        graphics.fill(x1, y1, x2, y1 + 1, color);
        graphics.fill(x1, y2 - 1, x2, y2, color);
        graphics.fill(x1, y1, x1 + 1, y2, color);
        graphics.fill(x2 - 1, y1, x2, y2, color);
    }
}
