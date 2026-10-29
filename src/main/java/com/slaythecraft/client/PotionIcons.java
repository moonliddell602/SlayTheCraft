package com.slaythecraft.client;

import com.slaythecraft.combat.core.Potion;
import com.slaythecraft.combat.core.PotionForm;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

/**
 * 药水**图标**的绘制（v0.4.6 美术需求）。
 *
 * <p>需求原文：</p>
 * <ul>
 *   <li>"对敌人使用的药水使用喷溅的对应效果药水贴图"；</li>
 *   <li>"对自己/队友使用的药水使用常规的对应效果药水贴图"；</li>
 *   <li>"特殊药水使用滞留的对应效果药水贴图"。</li>
 * </ul>
 *
 * <p>做法：直接构造一个原版药水物品（{@code POTION} / {@code SPLASH_POTION} / {@code LINGERING_POTION}），
 * 用 {@link PotionContents} 给它上成本模组定的颜色 —— 于是拿到的就是**原版贴图 + 对应效果的颜色**，
 * 不需要任何自制美术。两瓶炖菜（{@link PotionForm#ITEM}）直接用它们自己的物品贴图。</p>
 */
public final class PotionIcons {

    private PotionIcons() {
    }

    /** 构造"这瓶药水长什么样"的物品栈（给 HUD / 界面 / 提示框共用）。 */
    public static ItemStack icon(Potion potion) {
        if (potion == null) {
            return ItemStack.EMPTY;
        }
        if (potion.usesItemIcon()) {
            ResourceLocation id = ResourceLocation.tryParse(potion.itemId());
            Item item = id == null ? null : BuiltInRegistries.ITEM.get(id);
            return new ItemStack(item == null ? Items.POTION : item);
        }
        ItemStack stack = new ItemStack(switch (potion.form()) {
            case SPLASH -> Items.SPLASH_POTION;
            case LINGERING -> Items.LINGERING_POTION;
            default -> Items.POTION;
        });
        stack.set(DataComponents.POTION_CONTENTS,
                new PotionContents(java.util.Optional.empty(), java.util.Optional.of(potion.color()),
                        java.util.List.of()));
        return stack;
    }

    /**
     * 在 {@code (x, y)} 画一个 16×16 的药水图标，并按 {@code scale} 缩放（0.5 = 8×8）。
     *
     * <p>缩放用 pose 实现，所以调用方不需要自己算 UV。</p>
     */
    public static void draw(GuiGraphics graphics, Potion potion, int x, int y, float scale) {
        ItemStack stack = icon(potion);
        if (stack.isEmpty() || scale <= 0.0F) {
            return;
        }
        if (Math.abs(scale - 1.0F) < 0.001F) {
            graphics.renderItem(stack, x, y);
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.renderItem(stack, 0, 0);
        graphics.pose().popPose();
    }

    /** 图标按缩放后的边长（给布局用）。 */
    public static int size(float scale) {
        return Math.max(1, Math.round(16.0F * scale));
    }

    /** 客户端 Font 宽度（提示里显示药水名用）。 */
    public static int textWidth(String text) {
        return Minecraft.getInstance().font.width(text);
    }
}
