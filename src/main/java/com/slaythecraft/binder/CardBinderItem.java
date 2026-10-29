package com.slaythecraft.binder;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 【卡牌收纳包】（设计文档 §6.1）。
 *
 * <p>它**不存储任何数据**：玩家的卡牌与金币记录在 {@code StcAttachments.CARD_DATA} 上，
 * 所以做多少个包都是同一份档案，包丢了档案也还在——只是没有包就不能开战 / 不能打开界面。</p>
 */
public class CardBinderItem extends Item {

    public CardBinderItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            BinderOpener.open(serverPlayer);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.stc.card_binder.tooltip.open").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.stc.card_binder.tooltip.records").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.stc.card_binder.tooltip.carry").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.stc.card_binder.tooltip.gate").withStyle(ChatFormatting.DARK_GRAY));
    }
}
