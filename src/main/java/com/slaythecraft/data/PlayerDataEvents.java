package com.slaythecraft.data;

import com.slaythecraft.Config;
import com.slaythecraft.SlayTheCraft;
import com.slaythecraft.achievement.AchievementService;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 玩家档案的一次性初始化：首次进入时发放配置的初始金币，
 * 以及【尖塔在等着你】这条"进游戏即有"的成就（v0.5.0.11）。
 *
 * <p>用 {@code initialized} 标记而不是"金币为 0 就发"，否则玩家把钱花光后会反复领到初始金币。</p>
 */
@EventBusSubscriber(modid = SlayTheCraft.MODID)
public final class PlayerDataEvents {

    private PlayerDataEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        // 【尖塔在等着你】在登录时**显式授予**，不依赖 JSON 里的 minecraft:tick。
        // tick 触发器本身是有效的（见 AdvancementCheck 的触发器白名单），但"玩家进世界后会不会
        // 恰好收到一次 tick 事件"依赖加载时序；显式授予是确定的，而且保证 toast 一定弹出来。
        // 放在 initialized 早退**之前**：老存档（.10 就已初始化过）也能补上这条成就。
        AchievementService.grantEnteredGame(player);

        PlayerCardData data = StcAttachments.dataOf(player);
        // v0.5.1.2 开发者模式：开关只存在内存里，**重登不会自动开**，但同一次进程内重登要补一次金币
        // （玩家可能把钱花掉了），并且必须把开关状态同步给客户端 —— 否则图鉴里的「加入牌组」按钮
        // 会因为没有收到同步包而消失。
        com.slaythecraft.dev.DevMode.syncTo(player);
        if (com.slaythecraft.dev.DevMode.isEnabled(player)) {
            data.setGold(com.slaythecraft.dev.DevMode.DEV_GOLD);
            StcAttachments.syncTo(player);
        }
        if (data.initialized()) {
            return;
        }
        data.markInitialized();
        int startingGold = Config.startingGold();
        if (startingGold > 0) {
            data.addGold(startingGold);
            SlayTheCraft.LOGGER.debug("[STC] 首次初始化 {} 的卡牌档案，发放初始金币 {}",
                    player.getScoreboardName(), startingGold);
        }
        StcAttachments.syncTo(player);
    }
}
