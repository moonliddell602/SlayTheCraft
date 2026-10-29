package com.slaythecraft.gate;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 引导方块（设计文档 §6.2）：一切尖塔内容的入口都从"右键这个方块"开始，
 * 从而**不劫持原版玩法**——不装模组的人正常玩，装了的人主动来点它。
 */
public class SpireGateBlock extends Block {

    public SpireGateBlock(Properties properties) {
        super(properties);
    }

    /** 空手右键（1.21.1 用 useWithoutItem；持物右键走 useItemOn，不拦截）。 */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                              BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            SpireGateOpener.open(serverPlayer);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
