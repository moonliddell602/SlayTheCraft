package com.slaythecraft.client;

import java.util.ArrayList;
import java.util.List;

import com.slaythecraft.network.QuestionChoicePayload;
import com.slaythecraft.network.QuestionOfferPayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 问号事件的选项界面（v0.0.2 V11；v0.5.0 增加事件描述与"先选牌"分支）。
 *
 * <p>程序化绘制、不暂停游戏：标题是事件名（村民职业），下面是**事件描述**（按宽度折行），
 * 再下面是若干可选分支卡片（标签 + 说明）。点击分支即回传选择；
 * 需要选牌的分支会先打开选牌页（独立新页面）；ESC = 什么都不选（不消耗次数）。</p>
 */
public class QuestionScreen extends Screen {

    private static final int CARD_W = 260;
    private static final int CARD_H = 26;
    private static final int CARD_GAP = 6;
    private static final int LINE_H = 11;
    private static final int PANEL_BG = 0xF01C1C22;
    private static final int PANEL_EDGE = 0xFF5A5A64;
    private static final int TEXT = 0xFFE8E8E8;
    private static final int TEXT_DIM = 0xFF9A9AA4;
    private static final int HIGHLIGHT = 0xFFFFD24A;

    public QuestionScreen() {
        super(Component.translatable("stc.question.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    private List<QuestionOfferPayload.OptionView> options() {
        return ClientQuestionState.options();
    }

    /** 事件描述按面板宽度折行后的正文。 */
    private List<String> descriptionLines() {
        QuestionOfferPayload offer = ClientQuestionState.offer();
        List<String> lines = new ArrayList<>();
        if (offer == null) {
            return lines;
        }
        for (String paragraph : offer.description()) {
            lines.addAll(BinderStyle.wrap(font, paragraph, CARD_W));
        }
        return lines;
    }

    /** 第一条选项卡片的 y（描述在上面）。 */
    private int optionsTop() {
        List<String> description = descriptionLines();
        int descriptionHeight = description.isEmpty() ? 0 : description.size() * LINE_H + 10;
        int total = descriptionHeight + options().size() * (CARD_H + CARD_GAP);
        return height / 2 - total / 2 + descriptionHeight;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        QuestionOfferPayload offer = ClientQuestionState.offer();
        if (offer == null) {
            return;
        }
        List<QuestionOfferPayload.OptionView> options = options();
        List<String> description = descriptionLines();
        int x = (width - CARD_W) / 2;
        int top = optionsTop();

        int panelTop = top - (description.isEmpty() ? 34 : description.size() * LINE_H + 44);
        int panelBottom = top + options.size() * (CARD_H + CARD_GAP) + 10;
        graphics.fill(x - 8, panelTop - 8, x + CARD_W + 8, panelBottom + 8, PANEL_BG);
        border(graphics, x - 8, panelTop - 8, x + CARD_W + 8, panelBottom + 8, PANEL_EDGE);

        Component title = Component.translatable("stc.question.heading", offer.name());
        TextStyle.draw(graphics, font, title, x + (CARD_W - font.width(title)) / 2, panelTop, HIGHLIGHT);
        Component hint = Component.translatable("stc.question.hint");
        TextStyle.draw(graphics, font, hint, x + (CARD_W - font.width(hint)) / 2, panelTop + 12, TEXT_DIM);

        int lineY = panelTop + 26;
        for (String line : description) {
            TextStyle.draw(graphics, font, line, x, lineY, TEXT);
            lineY += LINE_H;
        }

        for (int i = 0; i < options.size(); i++) {
            QuestionOfferPayload.OptionView option = options.get(i);
            int cardY = top + i * (CARD_H + CARD_GAP);
            boolean hovered = isInside(mouseX, mouseY, x, cardY, CARD_W, CARD_H);
            graphics.fill(x, cardY, x + CARD_W, cardY + CARD_H, hovered ? 0xFF3A3420 : 0xFF26262E);
            border(graphics, x, cardY, x + CARD_W, cardY + CARD_H, hovered ? HIGHLIGHT : PANEL_EDGE);
            TextStyle.draw(graphics, font, option.label(), x + 6, cardY + 4, hovered ? HIGHLIGHT : TEXT);
            TextStyle.draw(graphics, font, font.plainSubstrByWidth(option.description(), CARD_W - 60), x + 6,
                    cardY + 14, TEXT_DIM);
            if (option.needsCard()) {
                Component pick = Component.translatable("stc.question.needs_card");
                TextStyle.draw(graphics, font, pick, x + CARD_W - 6 - font.width(pick), cardY + 4, HIGHLIGHT);
            }
        }

        // v0.4.5：试图按 ESC 直接离开时的提示
        if (ClientQuestionState.refusing()) {
            Component refusal = Component.translatable("stc.question.must_choose");
            TextStyle.draw(graphics, font, refusal, x + (CARD_W - font.width(refusal)) / 2,
                    panelBottom + 12, 0xFFFF6A5A);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<QuestionOfferPayload.OptionView> options = options();
        QuestionOfferPayload offer = ClientQuestionState.offer();
        if (offer == null) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        int x = (width - CARD_W) / 2;
        int top = optionsTop();
        for (int i = 0; i < options.size(); i++) {
            QuestionOfferPayload.OptionView option = options.get(i);
            int cardY = top + i * (CARD_H + CARD_GAP);
            if (isInside(mouseX, mouseY, x, cardY, CARD_W, CARD_H)) {
                if (option.needsCard()) {
                    // v0.5.0：先打开"选一张牌"页面（独立页面），选完再回传
                    ClientQuestionState.beginPick(offer.eventId(), i, offer.name(), option.label(),
                            option.pickHint() != null && option.pickHint().contains("攻击"));
                    return true;
                }
                PacketDistributor.sendToServer(new QuestionChoicePayload(offer.eventId(), i, -1));
                ClientQuestionState.clear();
                onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * v0.4.5：问号事件**不再能直接离开** —— 需求原文"问号事件不再能直接离开"。
     *
     * <p>ESC 不再关掉选项页，必须先选一个分支（每个事件都至少有一个不需要选牌的分支，
     * 需要选牌的分支可以在选牌页按 ESC 退回选项页，所以不会把玩家卡死）。</p>
     */
    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            // 只提示，不关闭
            ClientQuestionState.hintMustChoose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        // 走到这里只可能是"已经选完分支"或玩家下线；不选分支的离开路径已被封死
        ClientQuestionState.clear();
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
}
