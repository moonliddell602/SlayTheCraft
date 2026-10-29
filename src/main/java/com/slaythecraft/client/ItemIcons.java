package com.slaythecraft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 小图标：把**物品**画成界面上的小图标（v0.5.1.2 美术需求）。
 *
 * <p>需求原文："小图标：收纳包主页 4 个（查看牌组=minecraft:book、查看遗物=minecraft:chest、
 * 事件回合统计=minecraft:compass、查看图鉴=minecraft:writable_book）" 以及事件触发方式页 /
 * 篝火 / 战斗界面的若干处。</p>
 *
 * <p>为什么不直接按贴图路径 {@code textures/item/xxx.png} 去画：{@code chest} / {@code campfire} /
 * {@code anvil} 这些是**方块物品**，贴图在 {@code textures/block/} 下、模型还带旋转，
 * 手写路径一定会画错。这里统一走 {@code GuiGraphics#renderItem} —— 游戏用哪套模型就用哪套，
 * 和原版物品栏里的样子完全一致（{@link PotionIcons} 画炖菜用的也是这条路）。</p>
 *
 * <p>原版物品图标是 16×16，要求别的尺寸时用姿态栈缩放（与 {@code PotionIcons} 的写法一致；
 * 内部先平移到目标点再缩放，所以缩放是"以左上角为原点"的，不会把图标挪出格子）。</p>
 */
public final class ItemIcons {

    /** 原版物品图标的原生边长。 */
    public static final int NATIVE = 16;

    private ItemIcons() {
    }

    /**
     * 在 {@code (x, y)} 画一个边长 {@code size} 的物品图标（左上角对齐）。
     *
     * @param size 目标边长；{@code <= 0} 或空物品时不画
     */
    public static void draw(GuiGraphics graphics, ItemStack stack, int x, int y, int size) {
        if (graphics == null || stack == null || stack.isEmpty() || size <= 0) {
            return;
        }
        if (size == NATIVE) {
            graphics.renderItem(stack, x, y);
            return;
        }
        float scale = size / (float) NATIVE;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.renderItem(stack, 0, 0);
        graphics.pose().popPose();
    }

    /** 用法同上，直接给物品。 */
    public static void draw(GuiGraphics graphics, Item item, int x, int y, int size) {
        if (item == null) {
            return;
        }
        draw(graphics, new ItemStack(item), x, y, size);
    }
}
