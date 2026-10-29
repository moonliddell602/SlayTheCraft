package com.slaythecraft.merchant;

import com.slaythecraft.chat.StcChat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * 卡牌商人生成蛋（调试 / 摆摊位用）。
 *
 * <p>原版生成蛋那套颜色 + 渲染层配置这里用不上，所以做一个最小实现：右键方块生成一只商人。</p>
 */
public class CardMerchantSpawnEggItem extends Item {

    public CardMerchantSpawnEggItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos spawnPos = context.getClickedPos().relative(context.getClickedFace());
        CardMerchant merchant = StcEntities.CARD_MERCHANT.get().spawn(serverLevel, spawnPos, MobSpawnType.SPAWN_EGG);
        if (merchant == null) {
            return InteractionResult.FAIL;
        }
        merchant.setPersistenceRequired();
        // v0.4.6.3：系统提示统一走 StcChat（默认不写进聊天框）
        if (context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            StcChat.tell(serverPlayer, net.minecraft.network.chat.Component.literal(
                    "[STC] 已召唤卡牌商人：右键它即可购买卡牌。"));
        }
        return InteractionResult.SUCCESS;
    }
}
