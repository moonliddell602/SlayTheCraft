package com.slaythecraft.item;

import com.slaythecraft.chat.StcChat;
import com.slaythecraft.combat.mc.CombatManager;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The battle standard is the physical entry point of the combat system: right-click a hostile mob
 * to start a card battle with the mobs around you.
 *
 * <p><b>v0.4.2 修复</b>：以前"右键空气 = 结束回合"。问题是**按住右键打怪开战**时，原版会每 4 tick
 * 重复一次物品使用动作，于是开战的同一下就把第 1 回合结束掉了（表现为"第一回合被跳过"，
 * 而且按角色的球被动/回合结束效果会立刻结算）。现在右键空气**不再结束回合**，
 * 结束回合请点界面上的「结束回合」按钮、按 <b>R</b> 或用 {@code /stc combat end}。</p>
 */
public class BattleStandardItem extends Item {

    public BattleStandardItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            if (CombatManager.inCombat(serverPlayer)) {
                // 绝对不要在这里结束回合（见类注释）：只给一句提示
                hint(serverPlayer, "战斗中：点界面上的「结束回合」或按 R 结束回合（右键军旗不再结束回合）。");
            } else {
                hint(serverPlayer, "对着敌对生物右键使用「战斗军旗」即可开始卡牌战斗。");
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer && target instanceof Mob) {
            if (CombatManager.inCombat(serverPlayer)) {
                hint(serverPlayer, "战斗进行中：点界面上的「结束回合」或按 R 结束回合；卡牌请点手牌打出。");
            } else {
                String failure = CombatManager.start(serverPlayer, -1);
                if (failure != null) {
                    hint(serverPlayer, failure);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private static void hint(ServerPlayer player, String message) {
        StcChat.tell(player, Component.literal("[STC] " + message).withStyle(ChatFormatting.YELLOW));
    }
}
