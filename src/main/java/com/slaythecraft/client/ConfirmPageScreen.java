package com.slaythecraft.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * 通用「确认页」（v0.2.7）：**需要二次确认的操作一律新开这一页**（删卡、重开本局…）。
 *
 * <p>以前这些确认框是画在母页面上的浮层，容易和别的文字叠在一起；现在统一成独立页面：
 * 标题 + 折行说明 + 「确认」/「取消」两个按钮，取消或确认后都回到来源页。</p>
 */
public class ConfirmPageScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {

    private static final int BUTTON_H = 18;
    private static final int BUTTON_W = 96;

    private final Component heading;
    private final List<String> body;
    private final String confirmLabel;
    private final Runnable onConfirm;
    private final Supplier<Screen> back;
    private final boolean danger;

    public ConfirmPageScreen(M menu, Inventory inventory, Component title, Component heading, List<String> body,
                             String confirmLabel, boolean danger, Runnable onConfirm, Supplier<Screen> back) {
        super(menu, inventory, title);
        this.imageWidth = BinderStyle.WIDTH;
        this.imageHeight = 150;
        this.heading = heading;
        this.body = List.copyOf(body);
        this.confirmLabel = confirmLabel;
        this.danger = danger;
        this.onConfirm = onConfirm;
        this.back = back;
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
        int x = leftPos + BinderStyle.GRID_X;
        int width = imageWidth - BinderStyle.GRID_X * 2;
        TextStyle.draw(graphics, font, heading, x + (width - font.width(heading)) / 2, topPos + 14,
                danger ? BinderStyle.DANGER : BinderStyle.ACCENT);
        graphics.fill(x, topPos + 28, x + width, topPos + 29, BinderStyle.BOOK_LINE);

        List<String> lines = new ArrayList<>();
        for (String paragraph : body) {
            lines.addAll(BinderStyle.wrap(font, paragraph, width));
        }
        int y = topPos + 40;
        for (String line : lines) {
            if (y > topPos + 92) {
                break;
            }
            TextStyle.draw(graphics, font, line, x + (width - font.width(line)) / 2, y, BinderStyle.INK);
            y += 11;
        }

        int gap = 8;
        int startX = leftPos + (imageWidth - BUTTON_W * 2 - gap) / 2;
        int buttonY = topPos + imageHeight - 30;
        BinderStyle.button(graphics, font, startX, buttonY, BUTTON_W, BUTTON_H, confirmLabel, true);
        BinderStyle.button(graphics, font, startX + BUTTON_W + gap, buttonY, BUTTON_W, BUTTON_H,
                Component.translatable("stc.binder.cancel").getString(), true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (minecraft == null) {
            return super.mouseClicked(mouseX, mouseY, mouseButton);
        }
        int relX = (int) mouseX - leftPos;
        int relY = (int) mouseY - topPos;
        int gap = 8;
        int startX = (imageWidth - BUTTON_W * 2 - gap) / 2;
        int buttonY = imageHeight - 30;
        if (relY >= buttonY && relY < buttonY + BUTTON_H) {
            if (relX >= startX && relX < startX + BUTTON_W) {
                if (onConfirm != null) {
                    onConfirm.run();
                }
                minecraft.setScreen(back.get());
                return true;
            }
            if (relX >= startX + BUTTON_W + gap && relX < startX + BUTTON_W * 2 + gap) {
                minecraft.setScreen(back.get());
                return true;
            }
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
