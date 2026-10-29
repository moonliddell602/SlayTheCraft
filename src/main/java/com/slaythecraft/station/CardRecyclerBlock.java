package com.slaythecraft.station;

import com.slaythecraft.binder.CardBinderMenu;
import com.slaythecraft.data.StcAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 【卡牌回收站】方块（设计文档 §6.3）：花钱删卡的另一条途径。
 *
 * <p>打开的是**和收纳包同一个界面类**（{@link CardBinderMenu.Mode#RECYCLER}），
 * 所以价格、二次确认、服务端校验与收纳包内删卡完全一致，不会有第二套行为。</p>
 */
public class CardRecyclerBlock extends Block {

    public CardRecyclerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                              BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            StcAttachments.syncTo(serverPlayer);
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, ignored) -> CardBinderMenu.recyclerMenu(containerId, inventory),
                    Component.translatable("stc.recycler.title")));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
