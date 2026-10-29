package com.slaythecraft.client;

import com.slaythecraft.SlayTheCraft;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * 战斗时**隐藏原版 HUD**（v0.5.0.6）。
 *
 * <p>需求原文："卡牌位置过高。应该向下（正下位置，可以隐藏物品栏，饱食度，经验值和生命值等UI
 * 来达到只显示卡牌的效果。）。届时，也要一并移动药水栏，状态显示位置和结束回合按钮。"</p>
 *
 * <p>原版 HUD（快捷栏 / 生命 / 饱食度 / 经验条）在屏幕底部与左下占了一条，正好压着战斗手牌区。
 * 这里在战斗期间取消 {@link RenderGuiEvent.Pre}，把整条原版 HUD 让给战斗界面；
 * 战斗结束立刻恢复，不影响正常游玩。</p>
 *
 * <p>只影响**原版 HUD**：本模组自己的各层（{@link PotionHudLayer} / {@link RewardHudLayer} /
 * {@link OrbRenderer} 等）走的是各自的绘制路径，不会被这里取消。</p>
 */
@EventBusSubscriber(modid = SlayTheCraft.MODID, value = Dist.CLIENT)
public final class BattleHudHider {

    private BattleHudHider() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        if (ClientBattleState.view().active()) {
            event.setCanceled(true);
        }
    }
}
