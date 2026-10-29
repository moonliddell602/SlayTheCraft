package com.slaythecraft.client;

import java.util.ArrayList;
import java.util.List;

import com.slaythecraft.combat.core.Card;
import com.slaythecraft.combat.core.CardLibrary;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 战斗内的卡牌详情页（v0.4.2，独立新页面）。
 *
 * <p>从「查看我的牌组 / 查看牌堆」点进来：名称、费用·类型·品质、关键字、完整描述（按宽度折行）。</p>
 */
public class BattleCardDetailScreen extends Screen implements BattleInfoScreen {

    private static final int PANEL_W = 240;
    private static final int LINE_H = 11;
    /** v0.4.5：详情页的卡面插画位高度（参照原版位置：卡面插画占上半部分）。 */
    private static final int ART_H = 72;
    private static final int PANEL_BG = 0xF01C1C22;
    private static final int PANEL_EDGE = 0xFF5A5A64;
    private static final int TEXT = 0xFFE8E8E8;
    private static final int TEXT_DIM = 0xFF9A9AA4;
    private static final int GOLD = 0xFFFFD24A;

    private final BattleCardListScreen.Kind kind;
    private final String cardId;
    private final boolean upgraded;

    public BattleCardDetailScreen(BattleCardListScreen.Kind kind, String cardId, boolean upgraded) {
        super(Component.translatable("stc.battle.deck.detail.title"));
        this.kind = kind;
        this.cardId = cardId;
        this.upgraded = upgraded;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public Screen backScreen() {
        return new BattleCardListScreen(kind);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Card card = CardLibrary.resolve(cardId, upgraded ? 1 : 0);
        String name = card == null ? cardId : card.name();
        List<String> lines = new ArrayList<>();
        if (card == null) {
            lines.add(Component.translatable("stc.battle.deck.unknown").getString());
        } else {
            lines.add(Component.translatable("stc.battle.deck.type", card.cost(), card.type().label(),
                    card.rarity().label()).getString());
            if (!card.keywordLabel().isEmpty()) {
                lines.add(Component.translatable("stc.battle.deck.keywords", card.keywordLabel()).getString());
            }
            lines.add("");
            lines.addAll(BinderStyle.wrap(font, card.description(), PANEL_W - 24));
            if (card.type() == com.slaythecraft.combat.core.CardType.POWER) {
                lines.add("");
                lines.add(Component.translatable("stc.battle.deck.power.note").getString());
            }
        }
        // v0.4.5：先给卡面插画位留出空间，再排文字
        int panelH = 46 + ART_H + 8 + lines.size() * LINE_H + 30;
        int left = (width - PANEL_W) / 2;
        int top = (height - panelH) / 2;
        graphics.fill(left, top, left + PANEL_W, top + panelH, PANEL_BG);
        border(graphics, left, top, left + PANEL_W, top + panelH, PANEL_EDGE);
        TextStyle.draw(graphics, font, name, left + (PANEL_W - font.width(name)) / 2, top + 10, GOLD);
        // 插画位：宽 = 面板宽 − 24，高 = ART_H（贴图缺省时画占位框）
        int artX = left + 12;
        int artY = top + 26;
        CardArt.draw(graphics, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                com.slaythecraft.SlayTheCraft.MODID, cardId), artX, artY, PANEL_W - 24, ART_H);
        graphics.fill(left + 12, artY + ART_H + 6, left + PANEL_W - 12, artY + ART_H + 7, PANEL_EDGE);
        int y = artY + ART_H + 14;
        for (String line : lines) {
            TextStyle.draw(graphics, font, line, left + 12, y, line.isEmpty() ? TEXT_DIM : TEXT);
            y += LINE_H;
        }
        Component back = Component.translatable("stc.battle.deck.back");
        TextStyle.draw(graphics, font, back, left + (PANEL_W - font.width(back)) / 2, top + panelH - 16, TEXT_DIM);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(backScreen());
        }
        super.onClose();
    }

    private void border(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        graphics.fill(x1, y1, x2, y1 + 1, color);
        graphics.fill(x1, y2 - 1, x2, y2, color);
        graphics.fill(x1, y1, x1 + 1, y2, color);
        graphics.fill(x2 - 1, y1, x2, y2, color);
    }
}
