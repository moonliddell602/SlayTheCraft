package com.slaythecraft.client;

import java.util.ArrayList;
import java.util.List;

import com.slaythecraft.question.QuestionCards;
import com.slaythecraft.question.QuestionOption;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 问号事件的**选牌页**（v0.5.0，独立新页面）。
 *
 * <p>盔甲商"给他不需要的装备"、武器匠"传递武器"、傻子村民"给他吃的"都要先选一张牌；
 * 这里只列出**符合条件**的牌（筛选规则与服务端同一份代码 {@link QuestionCards}）。</p>
 */
public class QuestionCardPickScreen extends Screen {

    private static final int COLS = 5;
    private static final int CELL_W = 42;
    private static final int CELL_H = 26;
    private static final int GAP = 4;
    private static final int PANEL_BG = 0xF01C1C22;
    private static final int PANEL_EDGE = 0xFF5A5A64;
    private static final int TEXT = 0xFFE8E8E8;
    private static final int TEXT_DIM = 0xFF9A9AA4;
    private static final int HIGHLIGHT = 0xFFFFD24A;

    private final ClientQuestionState.PendingPick pick;
    private final List<Integer> indices;
    private int page;

    public QuestionCardPickScreen(ClientQuestionState.PendingPick pick) {
        super(Component.translatable("stc.question.pick.title"));
        this.pick = pick;
        QuestionOption.CardPick filter = pick.upgrade() ? QuestionOption.CardPick.UPGRADE_ATTACK
                : QuestionOption.CardPick.REMOVE_ANY;
        this.indices = QuestionCards.selectable(filter, BinderStyle.data());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int panelW = COLS * (CELL_W + GAP) + 16;
        int rows = (int) Math.ceil(Math.min(QuestionCards.PAGE_SIZE, indices.size()) / (double) COLS);
        int panelH = 60 + Math.max(1, rows) * (CELL_H + GAP) + 24;
        int left = (width - panelW) / 2;
        int top = (height - panelH) / 2;
        graphics.fill(left, top, left + panelW, top + panelH, PANEL_BG);
        border(graphics, left, top, left + panelW, top + panelH, PANEL_EDGE);

        Component title = Component.translatable("stc.question.pick.heading", pick.eventName(),
                pick.optionLabel());
        TextStyle.draw(graphics, font, title, left + (panelW - font.width(title)) / 2, top + 8, HIGHLIGHT);
        String hint = pick.upgrade() ? Component.translatable("stc.question.pick.upgrade").getString()
                : Component.translatable("stc.question.pick.remove").getString();
        TextStyle.draw(graphics, font, hint, left + (panelW - font.width(hint)) / 2, top + 22, TEXT_DIM);
        Component hint2 = Component.translatable("stc.question.pick.hint");
        TextStyle.draw(graphics, font, hint2, left + (panelW - font.width(hint2)) / 2, top + 34, TEXT_DIM);

        if (indices.isEmpty()) {
            Component empty = Component.translatable("stc.question.pick.empty");
            TextStyle.draw(graphics, font, empty, left + (panelW - font.width(empty)) / 2, top + 70, TEXT_DIM);
            return;
        }

        int start = page * QuestionCards.PAGE_SIZE;
        for (int cell = 0; cell < QuestionCards.PAGE_SIZE; cell++) {
            int index = start + cell;
            if (index >= indices.size()) {
                break;
            }
            int cardIndex = indices.get(index);
            var instance = BinderStyle.data().cards().get(cardIndex);
            var definition = com.slaythecraft.combat.core.CardLibrary
                    .resolve(instance.id().getPath(), instance.upgrades());
            int x = left + 8 + (cell % COLS) * (CELL_W + GAP);
            int y = top + 52 + (cell / COLS) * (CELL_H + GAP);
            boolean hovered = isInside(mouseX, mouseY, x, y, CELL_W, CELL_H);
            graphics.fill(x, y, x + CELL_W, y + CELL_H, hovered ? 0xFF3A3420 : 0xFF26262E);
            border(graphics, x, y, x + CELL_W, y + CELL_H, hovered ? HIGHLIGHT : PANEL_EDGE);
            String name = definition == null ? instance.id().getPath() : definition.name();
            if (font.width(name) > CELL_W - 6) {
                name = font.plainSubstrByWidth(name, CELL_W - 6);
            }
            TextStyle.draw(graphics, font, name, x + 3, y + 4, hovered ? HIGHLIGHT : TEXT);
            String cost = definition == null ? "?" : String.valueOf(definition.cost());
            TextStyle.draw(graphics, font, cost, x + 3, y + 15, TEXT_DIM);
        }

        if (QuestionCards.pages(indices.size()) > 1) {
            Component pages = Component.translatable("stc.question.pick.page", page + 1,
                    QuestionCards.pages(indices.size()));
            TextStyle.draw(graphics, font, pages, left + 8, top + panelH - 16, TEXT_DIM);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int panelW = COLS * (CELL_W + GAP) + 16;
        int rows = (int) Math.ceil(Math.min(QuestionCards.PAGE_SIZE, indices.size()) / (double) COLS);
        int panelH = 60 + Math.max(1, rows) * (CELL_H + GAP) + 24;
        int left = (width - panelW) / 2;
        int top = (height - panelH) / 2;
        int start = page * QuestionCards.PAGE_SIZE;
        for (int cell = 0; cell < QuestionCards.PAGE_SIZE; cell++) {
            int index = start + cell;
            if (index >= indices.size()) {
                break;
            }
            int x = left + 8 + (cell % COLS) * (CELL_W + GAP);
            int y = top + 52 + (cell / COLS) * (CELL_H + GAP);
            if (isInside(mouseX, mouseY, x, y, CELL_W, CELL_H)) {
                var payload = new com.slaythecraft.network.QuestionChoicePayload(pick.eventId(),
                        pick.optionIndex(), indices.get(index));
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(payload);
                ClientQuestionState.clear();
                onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int pages = QuestionCards.pages(indices.size());
        if (pages > 1) {
            page = Math.max(0, Math.min(pages - 1, page + (scrollY > 0 ? -1 : 1)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        // ESC = 反悔：只清掉选牌任务，选项页会自动重新弹出（不消耗次数）
        ClientQuestionState.clearPick();
        super.onClose();
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private void border(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        graphics.fill(x1, y1, x2, y2, color);
        graphics.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, PANEL_BG);
        graphics.fill(x1, y1, x2, y1 + 1, color);
        graphics.fill(x1, y2 - 1, x2, y2, color);
        graphics.fill(x1, y1, x1 + 1, y2, color);
        graphics.fill(x2 - 1, y1, x2, y2, color);
    }
}
