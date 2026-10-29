package com.slaythecraft.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.slaythecraft.binder.CardBinderMenu;
import com.slaythecraft.combat.core.Potion;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 药水图鉴**详情页**（v0.4.6.7，独立新页面）。
 *
 * <p>需求原文："详情页面显示效果描述，图标，名称，和机制。"</p>
 *
 * <p>所以这一页从上到下是：图标（32×32，{@link PotionIcons#draw} 放大两倍）+ 名称、
 * 效果描述（{@link Potion#description()}）、机制（同一段说明 + 性质 / 图标形态 / 使用对象）。
 * 正文按面板宽度**折行**，一个字都不截断。</p>
 */
public class PotionDetailScreen extends AbstractContainerScreen<CardBinderMenu> {

    /** 详情页放大的图标边长（16×16 的图标 ×2）。 */
    private static final int ICON = 32;
    private static final float ICON_SCALE = 2.0F;
    private static final int LINE_H = 11;
    private static final int SECTION_GAP = 8;

    private final Potion potion;
    private final Supplier<Screen> back;
    private final List<String> descriptionLines = new ArrayList<>();
    private final List<String> mechanicLines = new ArrayList<>();

    public PotionDetailScreen(CardBinderMenu menu, Inventory inventory, Component title, Potion potion,
                              Supplier<Screen> back) {
        super(menu, inventory, title);
        this.potion = potion;
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
        int width = imageWidth - BinderStyle.GRID_X * 2;
        descriptionLines.clear();
        descriptionLines.addAll(BinderStyle.wrap(font, potion.description(), width));
        // v0.4.6.8 需求："移除：药水机制介绍（与药水描述重复）" + "图鉴详情页不再显示小图标素材来源"
        // 所以详情页只留【名称 + 图标 + 效果描述】，不再画机制段（也就没有"图标形态"这种素材来源）
        mechanicLines.clear();
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
        PotionIcons.draw(graphics, potion, x, y, ICON_SCALE);
        TextStyle.draw(graphics, font, potion.name(), x + ICON + 10, y + (ICON - 8) / 2, BinderStyle.ACCENT);

        int bodyY = y + ICON + 10;
        section(graphics, x, bodyY, "stc.codex.potion.effect", descriptionLines);

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
