package com.slaythecraft.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.slaythecraft.Config;
import com.slaythecraft.character.CharacterProfile;
import com.slaythecraft.character.CharacterRegistry;
import com.slaythecraft.combat.core.Card;
import com.slaythecraft.combat.core.CardLibrary;
import com.slaythecraft.combat.core.Relic;
import com.slaythecraft.gate.SpireGateMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 角色详情页（v0.0.3 W3）：**点角色卡就新开这一页**（按 v0.2.6 定下的约定）。
 *
 * <p>内容：程序化头像（叛逃掠夺者是"左眼闭合 + 伤痕"的掠夺者）、角色描述、初始生命/能量、
 * 初始牌组（按种类合并显示数量）、初始遗物（名字 + 描述）。底部两个按钮：
 * 「选择这个角色」与「返回」。</p>
 */
public class CharacterDetailScreen extends AbstractContainerScreen<SpireGateMenu> {

    private static final int BODY_Y = 62;
    private static final int LINE_H = 11;
    private static final int BUTTON_H = 16;

    private final int index;
    private final Supplier<Screen> back;

    public CharacterDetailScreen(SpireGateMenu menu, Inventory inventory, Component title, int index,
                                 Supplier<Screen> back) {
        super(menu, inventory, title);
        this.imageWidth = BinderStyle.WIDTH;
        this.imageHeight = BinderStyle.HEIGHT;
        this.index = index;
        this.back = back;
    }

    private CharacterProfile profile() {
        return CharacterRegistry.byIndex(index);
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
        CharacterProfile profile = profile();
        int x = leftPos + BinderStyle.GRID_X;
        int width = imageWidth - BinderStyle.GRID_X * 2;
        if (profile == null) {
            TextStyle.draw(graphics, font, "?", x, topPos + BODY_Y, BinderStyle.INK);
            button(graphics, x, topPos + imageHeight - 30, 52, 14,
                    Component.translatable("stc.binder.back").getString());
            return;
        }

        // 头像（v0.4.6.10：优先用作者提供的贴图，没有才画程序化头像）
        int avatarX = leftPos + (imageWidth - 40) / 2;
        if (!BinderSkin.drawAvatar(graphics, profile.id(), avatarX, topPos + 10, 40)) {
            drawAvatar(graphics, avatarX, topPos + 10, profile.shortName());
        }

        Component name = Component.translatable(profile.nameKey());
        TextStyle.draw(graphics, font, name, leftPos + (imageWidth - font.width(name)) / 2, topPos + 48,
                BinderStyle.ACCENT);
        graphics.fill(x, topPos + 58, x + width, topPos + 59, BinderStyle.BOOK_LINE);

        List<String> lines = new ArrayList<>();
        lines.addAll(BinderStyle.wrap(font, Component.translatable(profile.descriptionKey()).getString(), width));
        lines.add("");
        // v0.5.0.7 需求："移除基础数值文案显示，移动到详情页面里。"
        // 原来角色卡上那行"独立生命 / 能量 / 起始张数"搬到这里（卡片上不再画）
        lines.add(Component.translatable("stc.character.hp", Config.maxPlayerHp()).getString());
        lines.add(Component.translatable("stc.character.stats", profile.startingDeck().size(),
                profile.energyPerTurn()).getString());
        lines.add(Component.translatable("stc.character.deck").getString());
        for (String entry : deckSummary(profile)) {
            lines.add("  " + entry);
        }
        Relic relic = profile.startingRelicDefinition();
        lines.add(Component.translatable("stc.character.relic").getString());
        if (relic == null) {
            lines.add("  " + Component.translatable("stc.character.relic.none").getString());
        } else {
            lines.add("  【" + relic.name() + "】");
            lines.addAll(BinderStyle.wrap(font, relic.description(), width - 8));
        }

        int y = topPos + BODY_Y;
        int lineEnd = topPos + imageHeight - 46;
        for (String line : lines) {
            if (y + LINE_H > lineEnd) {
                TextStyle.draw(graphics, font, "…", x, y, BinderStyle.INK_DIM);
                break;
            }
            TextStyle.draw(graphics, font, line, x, y, BinderStyle.INK);
            y += LINE_H;
        }

        int buttonY = topPos + imageHeight - 32;
        BinderStyle.button(graphics, font, x, buttonY, width, BUTTON_H,
                Component.translatable("stc.character.choose").getString(), true);
        BinderStyle.button(graphics, font, x, buttonY + BUTTON_H + 4, 52, 14,
                Component.translatable("stc.binder.back").getString(), true);
    }

    /** 初始牌组按种类合并：{@code 打击 ×4}。 */
    private List<String> deckSummary(CharacterProfile profile) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (var cardId : profile.startingDeck()) {
            Card card = CardLibrary.byId(cardId.getPath());
            String name = card == null ? cardId.getPath() : card.name();
            counts.merge(name, 1, Integer::sum);
        }
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            out.add(entry.getKey() + " ×" + entry.getValue());
        }
        return out;
    }

    /** 程序化头像：40×40 的脸；叛逃掠夺者额外画闭合的左眼与一道伤痕。 */
    private void drawAvatar(GuiGraphics graphics, int x, int y, String shortName) {
        int skin = switch (shortName) {
            case "ironclad" -> 0xFFB5533C;
            case "silent" -> 0xFF4E8C6A;
            case "defect" -> 0xFF4A7FB5;
            case "pillager" -> 0xFF6E7A6A;
            default -> 0xFFC9A227;
        };
        graphics.fill(x, y, x + 40, y + 40, skin);
        BinderStyle.border(graphics, x, y, x + 40, y + 40, BinderStyle.BOOK_EDGE);
        if ("pillager".equals(shortName)) {
            // 掠夺者：大鼻子 + 左眼闭合 + 右眼睁开 + 伤痕
            graphics.fill(x + 6, y + 12, x + 12, y + 14, BinderStyle.INK);   // 闭合的左眼（一条线）
            graphics.fill(x + 26, y + 10, x + 32, y + 16, BinderStyle.INK);  // 右眼
            graphics.fill(x + 16, y + 16, x + 24, y + 28, BinderStyle.INK);  // 鼻子
            graphics.fill(x + 8, y + 6, x + 10, y + 22, 0xFF8C4A4A);         // 伤痕
            graphics.fill(x + 12, y + 32, x + 28, y + 34, BinderStyle.INK);  // 嘴
        } else {
            graphics.fill(x + 9, y + 14, x + 14, y + 19, BinderStyle.INK);
            graphics.fill(x + 26, y + 14, x + 31, y + 19, BinderStyle.INK);
            graphics.fill(x + 12, y + 28, x + 28, y + 30, BinderStyle.INK);
        }
    }

    private void button(GuiGraphics graphics, int x, int y, int width, int height, String label) {
        BinderStyle.button(graphics, font, x, y, width, height, label, true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (minecraft == null || minecraft.player == null) {
            return super.mouseClicked(mouseX, mouseY, mouseButton);
        }
        int relX = (int) mouseX - leftPos;
        int relY = (int) mouseY - topPos;
        int buttonY = imageHeight - 32;
        if (relY >= buttonY && relY < buttonY + BUTTON_H
                && relX >= BinderStyle.GRID_X && relX < imageWidth - BinderStyle.GRID_X) {
            // 选择这个角色：把按钮 id（= 角色下标）发给服务端，然后回到尖塔入口
            if (minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, index);
            }
            minecraft.setScreen(back.get());
            return true;
        }
        if (relY >= buttonY + BUTTON_H + 4 && relY < buttonY + BUTTON_H + 18
                && relX >= BinderStyle.GRID_X && relX < BinderStyle.GRID_X + 52) {
            minecraft.setScreen(back.get());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && minecraft != null) {
            minecraft.setScreen(back.get());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
