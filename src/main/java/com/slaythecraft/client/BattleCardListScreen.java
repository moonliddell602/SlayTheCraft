package com.slaythecraft.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.slaythecraft.combat.core.Card;
import com.slaythecraft.combat.core.CardLibrary;
import com.slaythecraft.network.BattleView;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 战斗内「查看牌组 / 查看牌堆」页（v0.4.2）。
 *
 * <p>需求："新功能：战斗内查看牌组 —— 点击能量上方的【查看我的牌组】"、"新功能：查看牌堆 ——
 * 将抽牌堆 / 弃牌堆 / 消耗堆拆开…点击可以看到有哪些牌"。</p>
 *
 * <p>网格 + 数量统计；点一张牌进入独立详情页（同样是 {@link BattleInfoScreen}）。</p>
 */
public class BattleCardListScreen extends Screen implements BattleInfoScreen {

    /** 要看哪一堆。 */
    public enum Kind {
        /** 整副战斗牌组（抽牌堆 + 手牌 + 弃牌堆 + 消耗堆）。 */
        DECK("stc.battle.deck.title"),
        DRAW("stc.battle.pile.draw.title"),
        DISCARD("stc.battle.pile.discard.title"),
        EXHAUST("stc.battle.pile.exhaust.title");

        private final String titleKey;

        Kind(String titleKey) {
            this.titleKey = titleKey;
        }

        public String titleKey() {
            return titleKey;
        }
    }

    // v0.4.3：改成 5 行 × 4 列，格子加宽（卡名显示得全）
    private static final int COLS = 4;
    private static final int VISIBLE_ROWS = 5;
    private static final int CELL_W = 56;
    private static final int CELL_H = 30;
    /** v0.4.5：格子左侧的卡面插画位（缩略图）尺寸。 */
    private static final int ART_W = 20;
    private static final int ART_H = 20;
    private static final int GAP = 3;
    private static final int PANEL_BG = 0xF01C1C22;
    private static final int PANEL_EDGE = 0xFF5A5A64;
    private static final int TEXT = 0xFFE8E8E8;
    private static final int TEXT_DIM = 0xFF9A9AA4;
    private static final int GOLD = 0xFFFFD24A;

    private final Kind kind;

    public BattleCardListScreen(Kind kind) {
        super(Component.translatable(kind.titleKey()));
        this.kind = kind;
    }

    /** 这一堆里的牌（按 id 合并计数，显示 {@code 打击 ×4}）。 */
    private List<Map.Entry<String, Integer>> groups() {
        BattleView view = ClientBattleState.view();
        List<BattleView.CardRef> cards = switch (kind) {
            case DECK -> {
                List<BattleView.CardRef> all = new ArrayList<>();
                all.addAll(view.drawCards());
                all.addAll(view.discardCards());
                all.addAll(view.exhaustCards());
                for (BattleView.HandCard card : view.hand()) {
                    all.add(new BattleView.CardRef(card.id(), card.upgrades() > 0));
                }
                yield all;
            }
            case DRAW -> view.drawCards();
            case DISCARD -> view.discardCards();
            case EXHAUST -> view.exhaustCards();
        };
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (BattleView.CardRef card : cards) {
            String key = card.id().getPath() + (card.upgraded() ? "+" : "");
            counts.merge(key, 1, Integer::sum);
        }
        return new ArrayList<>(counts.entrySet());
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
        return new BattleScreen();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        List<Map.Entry<String, Integer>> groups = groups();
        int rows = Math.max(1, (groups.size() + COLS - 1) / COLS);
        int panelW = COLS * (CELL_W + GAP) + 20;
        int panelH = 52 + rows * (CELL_H + GAP) + 26;
        int left = (width - panelW) / 2;
        int top = (height - panelH) / 2;
        graphics.fill(left, top, left + panelW, top + panelH, PANEL_BG);
        border(graphics, left, top, left + panelW, top + panelH, PANEL_EDGE);

        Component title = Component.translatable(kind.titleKey());
        TextStyle.draw(graphics, font, title, left + (panelW - font.width(title)) / 2, top + 8, GOLD);
        int total = 0;
        for (Map.Entry<String, Integer> entry : groups) {
            total += entry.getValue();
        }
        Component count = Component.translatable("stc.battle.deck.count", total, groups.size());
        TextStyle.draw(graphics, font, count, left + (panelW - font.width(count)) / 2, top + 22, TEXT_DIM);
        Component hint = Component.translatable("stc.battle.deck.hint");
        TextStyle.draw(graphics, font, hint, left + (panelW - font.width(hint)) / 2, top + 34, TEXT_DIM);

        if (groups.isEmpty()) {
            Component empty = Component.translatable("stc.battle.deck.empty");
            TextStyle.draw(graphics, font, empty, left + (panelW - font.width(empty)) / 2, top + 70, TEXT_DIM);
        }

        for (int i = 0; i < groups.size(); i++) {
            Map.Entry<String, Integer> entry = groups.get(i);
            String key = entry.getKey();
            boolean upgraded = key.endsWith("+");
            String id = upgraded ? key.substring(0, key.length() - 1) : key;
            Card definition = CardLibrary.resolve(id, upgraded ? 1 : 0);
            int x = left + 10 + (i % COLS) * (CELL_W + GAP);
            int y = top + 50 + (i / COLS) * (CELL_H + GAP);
            boolean hovered = isInside(mouseX, mouseY, x, y, CELL_W, CELL_H);
            graphics.fill(x, y, x + CELL_W, y + CELL_H, typeColor(definition));
            graphics.fill(x + 1, y + 1, x + CELL_W - 1, y + CELL_H - 1, 0xFF2A2A34);
            if (hovered) {
                border(graphics, x, y, x + CELL_W, y + CELL_H, GOLD);
            }
            String name = definition == null ? id : definition.name();
            // v0.4.5：格子左侧留出卡面插画位（没有贴图时是占位框）
            CardArt.draw(graphics, ResourceLocation.fromNamespaceAndPath(
                    com.slaythecraft.SlayTheCraft.MODID, id), x + 2, y + 2, ART_W, ART_H);
            name = font.plainSubstrByWidth(name, CELL_W - ART_W - 8);
            TextStyle.draw(graphics, font, name, x + ART_W + 4, y + 4,
                    upgraded ? GOLD : TEXT);
            String cost = definition == null ? "?" : String.valueOf(definition.cost());
            TextStyle.draw(graphics, font, cost, x + ART_W + 4, y + CELL_H - 12, TEXT_DIM);
            TextStyle.draw(graphics, font, "×" + entry.getValue(), x + CELL_W - 4 - font.width("×" + entry.getValue()),
                    y + CELL_H - 12, GOLD);
        }

        Component back = Component.translatable("stc.battle.deck.back");
        TextStyle.draw(graphics, font, back, left + (panelW - font.width(back)) / 2, top + panelH - 16, TEXT_DIM);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<Map.Entry<String, Integer>> groups = groups();
        int rows = Math.max(1, (groups.size() + COLS - 1) / COLS);
        int panelW = COLS * (CELL_W + GAP) + 20;
        int panelH = 52 + rows * (CELL_H + GAP) + 26;
        int left = (width - panelW) / 2;
        int top = (height - panelH) / 2;
        for (int i = 0; i < groups.size(); i++) {
            int x = left + 10 + (i % COLS) * (CELL_W + GAP);
            int y = top + 50 + (i / COLS) * (CELL_H + GAP);
            if (isInside(mouseX, mouseY, x, y, CELL_W, CELL_H)) {
                String key = groups.get(i).getKey();
                boolean upgraded = key.endsWith("+");
                String id = upgraded ? key.substring(0, key.length() - 1) : key;
                if (minecraft != null) {
                    minecraft.setScreen(new BattleCardDetailScreen(kind, id, upgraded));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(backScreen());
        }
        super.onClose();
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private void border(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        graphics.fill(x1, y1, x2, y1 + 1, color);
        graphics.fill(x1, y2 - 1, x2, y2, color);
        graphics.fill(x1, y1, x1 + 1, y2, color);
        graphics.fill(x2 - 1, y1, x2, y2, color);
    }

    static int typeColor(Card card) {
        if (card == null) {
            return 0xFF4A4A54;
        }
        return switch (card.type()) {
            case ATTACK -> 0xFFB5533C;
            case SKILL -> 0xFF4A7FB5;
            case POWER -> 0xFF7A4AB5;
            case STATUS, CURSE -> 0xFF6A6A6A;
        };
    }
}
