package com.slaythecraft.question;

import com.slaythecraft.SlayTheCraft;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 问号事件的触发（v0.0.2 V11）：**Shift + 右键**事件生物。
 *
 * <p>特意要求 Shift：普通右键仍然走原版（村民交易、给猪喂胡萝卜），
 * 只有明确按住 Shift 时才被我们接管，接管的这一次会被取消掉以免同时打开交易界面。</p>
 */
@EventBusSubscriber(modid = SlayTheCraft.MODID)
public final class QuestionEvents {

    private QuestionEvents() {
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isShiftKeyDown()) {
            return;
        }
        String reason = QuestionService.open(player, event.getTarget());
        if (reason == null) {
            // 要么已经下发界面（成功），要么这个生物根本没有事件；后者不该吃掉右键
            if (QuestionService.eventOf(event.getTarget()) != null) {
                event.setCanceled(true);
            }
            return;
        }
        player.displayClientMessage(Component.literal("[STC] " + reason), true);
    }
}
