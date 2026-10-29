package com.slaythecraft.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.slaythecraft.binder.CardBinderMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 状态图鉴**详情页**（v0.4.6.7，独立新页面）。
 *
 * <p>需求原文："详情页面显示状态效果描述，图标，名称，和机制。"</p>
 *
 * <p>所以这一页从上到下是：图标（32×32）+ 名称、描述（一句话）、机制（怎么算 / 怎么结算）。
 * 两段正文都按面板宽度**折行**（{@link BinderStyle#wrap}），一个字都不截断。</p>
 */
public class StatusDetailScreen extends AbstractContainerScreen<CardBinderMenu> {

    /** 详情页放大的图标边长（列表里是 16×16）。 */
    private static final int ICON = 32;
    private static final int LINE_H = 11;
    private static final int SECTION_GAP = 8;

    private final StatusIcons.Entry entry;
    private final Supplier<Screen> back;
    private final List<String> descriptionLines = new ArrayList<>();
    private final List<String> mechanicLines = new ArrayList<>();

    public StatusDetailScreen(CardBinderMenu menu, Inventory inventory, Component title,
                              StatusIcons.Entry entry, Supplier<Screen> back) {
        super(menu, inventory, title);
        this.entry = entry;
        this.back = back;
        this.imageWidth = BinderStyle.WIDTH;
        this.imageHeight = BinderStyle.HEIGHT;
    }

    @Override
    public void init() {
        // v0.4.3：面板尺寸按屏幕自适应（默认更大，小窗口自动收缩）
        this.imageWidth = BinderStyle.widthFor(this);
        this.imageHeight = BinderStyle.heightFor(this);
        super.init();
        // 正文按正文宽度折行（面板变窄时行数会变多，所以放在 init 里算）
        int width = imageWidth - BinderStyle.GRID_X * 2;
        descriptionLines.clear();
        descriptionLines.addAll(BinderStyle.wrap(font, entry.flavor(), width));
        mechanicLines.clear();
        mechanicLines.addAll(BinderStyle.wrap(font, entry.mechanics(), width));
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

        int x = leftPos + BinderStyle.GRID_X;
        int y = topPos + 38;
        // 图标 + 名称（名称跟着图标垂直居中）
        drawIcon(graphics, entry.texture(), x, y);
        TextStyle.draw(graphics, font, entry.label(), x + ICON + 10, y + (ICON - 8) / 2, BinderStyle.ACCENT);

        int bodyY = y + ICON + 10;
        bodyY = section(graphics, x, bodyY, "stc.codex.detail.desc", descriptionLines);
        bodyY += SECTION_GAP;
        section(graphics, x, bodyY, "stc.codex.detail.mechanics", mechanicLines);

        BinderStyle.button(graphics, font, x, topPos + BinderStyle.footerY(imageHeight), 52, 14,
                Component.translatable("stc.binder.back").getString(), true);
    }

    /** 画一段"小标题 + 折行正文"，返回下一段的 y。 */
    private int section(GuiGraphics graphics, int x, int y, String headingKey, List<String> lines) {
        TextStyle.draw(graphics, font, Component.translatable(headingKey), x, y, BinderStyle.ACCENT);
        int lineY = y + LINE_H + 1;
        for (String line : lines) {
            TextStyle.draw(graphics, font, line, x, lineY, BinderStyle.INK);
            lineY += LINE_H;
        }
        return lineY;
    }

    /** 状态图标：交给 {@link IconFit} 按真实像素尺寸等比缩放、居中放进 32×32 的方框。 */
    private static void drawIcon(GuiGraphics graphics, ResourceLocation texture, int x, int y) {
        IconFit.draw(graphics, texture, x, y, ICON, ICON);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (minecraft == null) {
            return super.mouseClicked(mouseX, mouseY, mouseButton);
        }
        int relX = (int) mouseX - leftPos;
        int relY = (int) mouseY - topPos;
        if (relX >= BinderStyle.GRID_X && relX < BinderStyle.GRID_X + 52
                && relY >= BinderStyle.footerY(imageHeight) && relY < BinderStyle.footerY(imageHeight) + 14) {
            minecraft.setScreen(back.get());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    /** Esc = 返回列表页（不要走 super，否则会把整个收纳包界面关掉）。 */
    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(back.get());
        }
    }
}
