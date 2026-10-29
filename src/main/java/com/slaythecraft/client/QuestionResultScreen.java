package com.slaythecraft.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 问号事件的**结果页**（v0.5.0，独立新页面）。
 *
 * <p>需求："选择选项之后弹出的文案"—— 选完之后弹这一页，正文按宽度折行显示（长文案不会缺字），
 * 点「确定」或按 ESC 关闭。</p>
 */
public class QuestionResultScreen extends Screen {

    private static final int PANEL_W = 240;
    private static final int LINE_H = 11;
    private static final int PANEL_BG = 0xF01C1C22;
    private static final int PANEL_EDGE = 0xFF5A5A64;
    private static final int TEXT = 0xFFE8E8E8;
    private static final int HIGHLIGHT = 0xFFFFD24A;
    private static final int BUTTON_H = 16;

    public QuestionResultScreen() {
        super(Component.translatable("stc.question.result.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    /** 折行后的正文（绘制与点击判定共用，避免"按钮画在这里、判定在那里"）。 */
    private List<String> lines() {
        List<String> lines = new ArrayList<>();
        String text = ClientQuestionState.resultText();
        if (text == null) {
            return lines;
        }
        for (String paragraph : text.split("\n")) {
            lines.addAll(BinderStyle.wrap(font, paragraph, PANEL_W - 24));
        }
        return lines;
    }

    /** v0.4.5 修复：面板高度只在这里算一次，绘制与点击都用它。 */
    private int panelHeight() {
        return 46 + Math.max(1, lines().size()) * LINE_H + BUTTON_H + 16;
    }

    private int panelLeft() {
        return (width - PANEL_W) / 2;
    }

    private int panelTop() {
        return (height - panelHeight()) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        String title = ClientQuestionState.resultTitle();
        String text = ClientQuestionState.resultText();
        if (text == null) {
            return;
        }
        List<String> lines = lines();
        int panelH = panelHeight();
        int left = panelLeft();
        int top = panelTop();
        graphics.fill(left, top, left + PANEL_W, top + panelH, PANEL_BG);
        border(graphics, left, top, left + PANEL_W, top + panelH, PANEL_EDGE);

        Component heading = Component.translatable("stc.question.result.heading");
        TextStyle.draw(graphics, font, heading, left + (PANEL_W - font.width(heading)) / 2, top + 8, HIGHLIGHT);
        if (title != null) {
            String trimmed = font.plainSubstrByWidth(title, PANEL_W - 16);
            TextStyle.draw(graphics, font, trimmed, left + (PANEL_W - font.width(trimmed)) / 2, top + 22, TEXT);
        }
        graphics.fill(left + 12, top + 36, left + PANEL_W - 12, top + 37, PANEL_EDGE);

        int y = top + 42;
        for (String line : lines) {
            TextStyle.draw(graphics, font, line, left + 12, y, TEXT);
            y += LINE_H;
        }

        Component ok = Component.translatable("stc.question.result.ok");
        int buttonW = 60;
        int buttonX = left + (PANEL_W - buttonW) / 2;
        int buttonY = top + panelH - BUTTON_H - 10;
        boolean hovered = isInside(mouseX, mouseY, buttonX, buttonY, buttonW, BUTTON_H);
        graphics.fill(buttonX, buttonY, buttonX + buttonW, buttonY + BUTTON_H,
                hovered ? 0xFF3A3420 : 0xFF26262E);
        border(graphics, buttonX, buttonY, buttonX + buttonW, buttonY + BUTTON_H,
                hovered ? HIGHLIGHT : PANEL_EDGE);
        TextStyle.draw(graphics, font, ok, buttonX + (buttonW - font.width(ok)) / 2, buttonY + 4,
                hovered ? HIGHLIGHT : TEXT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // v0.4.5：整个面板都可点 = 确定（以前只有按钮那一小块，点偏了就"没反应"）
        int panelH = panelHeight();
        int left = panelLeft();
        int top = panelTop();
        if (isInside(mouseX, mouseY, left, top, PANEL_W, panelH)) {
            onClose();
            return true;
        }
        int buttonW = 60;
        int buttonX = left + (PANEL_W - buttonW) / 2;
        int buttonY = top + panelH - BUTTON_H - 10;
        if (isInside(mouseX, mouseY, buttonX, buttonY, buttonW, BUTTON_H)) {
            onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** 结果页按 ESC 也算"确定"（选择已经生效了）。 */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        ClientQuestionState.clearResult();
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
