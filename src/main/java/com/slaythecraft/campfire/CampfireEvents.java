package com.slaythecraft.campfire;

import com.slaythecraft.chat.StcChat;
import com.slaythecraft.SlayTheCraft;
import com.slaythecraft.binder.BinderResolver;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 篝火交互（v0.0.2 V10；v0.4.1 按反馈换成"打火石 + 收纳包"的触发方式）。
 *
 * <p>需求原文："更换篝火的触发方式为 副手手持卡牌收纳包，主手手持打火石。"</p>
 *
 * <p>所以触发条件就是**双手**：主手【打火石】+ 副手【卡牌收纳包】→ 右键点燃的营火 → 打开篝火准备界面
 * （愈合 / 锻造 / 多人包扎）。条件不满足时不取消事件（打火石照常点火），
 * 但如果玩家手里已经拿着其中一样，就给一条提示告诉他缺什么 —— 免得"看起来毫无反应"。</p>
 */
@EventBusSubscriber(modid = SlayTheCraft.MODID)
public final class CampfireEvents {

    private CampfireEvents() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!CampfireBlock.isLitCampfire(state)) {
            return;
        }
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        boolean flint = main.is(Items.FLINT_AND_STEEL);
        boolean binder = off.is(SlayTheCraft.CARD_BINDER.get());
        if (!flint || !binder) {
            // 只有"看起来像要开篝火"的时候才提示，避免路过营火就刷屏
            if (flint || binder) {
                StringBuilder missing = new StringBuilder();
                if (!binder) {
                    missing.append("副手【卡牌收纳包】");
                }
                if (!flint) {
                    if (missing.length() > 0) {
                        missing.append(" + ");
                    }
                    missing.append("主手【打火石】");
                }
                String hint = "[STC] 篝火需要：副手【卡牌收纳包】+ 主手【打火石】（还缺 " + missing + "）";
                player.displayClientMessage(Component.literal(hint), true);
                SlayTheCraft.LOGGER.info("[STC] {} 在篝火旁缺条件：{}",
                        player.getScoreboardName(), hint);
            }
            return;
        }
        String reason = CampfireService.blockReason(player);
        if (reason != null) {
            // 提示同时进聊天栏与行动栏：聊天栏留痕，行动栏立刻可见
            StcChat.tell(player, Component.literal("[STC] " + reason));
            player.displayClientMessage(Component.literal("[STC] " + reason), true);
            SlayTheCraft.LOGGER.info("[STC] {} 的篝火被拒绝：{}", player.getScoreboardName(), reason);
            return;
        }
        BlockPos pos = event.getPos();
        String failure = CampfireService.open(player, pos);
        if (failure != null) {
            StcChat.tell(player, Component.literal("[STC] " + failure));
            return;
        }
        // 确实打开了界面：挡掉原版右键（不然打火石会去点别的方块）
        event.setCanceled(true);
    }

    /** 兼容旧提示文案（"怎么携带收纳包"）。 */
    public static String equipHint() {
        return BinderResolver.equipHint();
    }
}
