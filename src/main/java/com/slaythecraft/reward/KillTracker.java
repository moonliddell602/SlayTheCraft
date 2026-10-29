package com.slaythecraft.reward;

import com.slaythecraft.SlayTheCraft;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * 原版击杀路径（v0.0.2 V10 起；**v0.4.6.2 起关闭**）。
 *
 * <p>需求原文（v0.4.6.2）："非尖塔玩法不再掉落卡牌。"</p>
 *
 * <p>以前这一路会把"玩家用剑/弓打死怪"也计入击杀阈值，于是**不打尖塔战斗也能刷出卡牌奖励**。
 * 现在卡牌奖励只认尖塔战斗内的击杀：由 {@code CombatManager.killMob} 直接调用
 * {@link RewardService#recordCombatKill} 记账（引擎会把"最后一击是谁打的"传下来）。</p>
 *
 * <p>这个类因此只保留一个"什么都不做"的监听：留着它可以把"原版击杀不再发奖"这条规则
 * 写在一处、并且和日志一起便于排查（如果哪天整合包想恢复，把 {@link #REWARDS_FROM_VANILLA_KILLS}
 * 改成 true 即回到旧行为）。</p>
 */
@EventBusSubscriber(modid = SlayTheCraft.MODID)
public final class KillTracker {

    /**
     * 原版击杀是否也能触发卡牌奖励。
     *
     * <p>v0.4.6.2 需求："非尖塔玩法不再掉落卡牌。" —— 默认 false；
     * 打开它会让"用剑打怪"也能累计击杀进度并发奖励（旧行为）。</p>
     */
    public static final boolean REWARDS_FROM_VANILLA_KILLS = false;

    private KillTracker() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (REWARDS_FROM_VANILLA_KILLS || event.getEntity().level().isClientSide()) {
            return;
        }
        // 只记一行调试日志：非尖塔玩法不再掉落卡牌（v0.4.6.2）
        if (event.getEntity().getKillCredit() instanceof net.minecraft.server.level.ServerPlayer player) {
            SlayTheCraft.LOGGER.debug("[STC] {} 击杀 {}：非尖塔玩法不再掉落卡牌（奖励只在尖塔战斗内产生）",
                    player.getScoreboardName(),
                    net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()));
        }
    }
}
