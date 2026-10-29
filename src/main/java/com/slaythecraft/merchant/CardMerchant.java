package com.slaythecraft.merchant;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 【卡牌商人】（设计文档 §6.4）：外观就是村民（"特殊村民"），但右键打开的是我们的商店界面。
 *
 * <p>刻意继承原版村民而不是新做职业 + 工作站：这样不会让服务器上的普通村民被"劫持"成商人，
 * 也不依赖兴趣点认领逻辑；同时可以直接复用原版村民的模型与渲染器，不需要美术资源。</p>
 */
public class CardMerchant extends Villager {

    public CardMerchant(EntityType<? extends Villager> type, Level level) {
        super(type, level);
        // 商人不该因为远离村庄/区块卸载而消失
        this.setPersistenceRequired();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide()) {
            CardShopMenu.open(player);
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    /** 商人不参与村庄职业系统，避免出现原版交易界面。 */
    @Override
    protected void updateTrades() {
        // 意图为空：所有交互都走我们自己的商店
    }
}
