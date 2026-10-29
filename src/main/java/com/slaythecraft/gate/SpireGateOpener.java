package com.slaythecraft.gate;

import com.slaythecraft.chat.StcChat;
import com.slaythecraft.Config;
import com.slaythecraft.combat.mc.CombatManager;
import com.slaythecraft.data.StcAttachments;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

/** 打开「尖塔入口」界面的入口。 */
public final class SpireGateOpener {

    private SpireGateOpener() {
    }

    /**
     * 打开入口界面。**这里不校验收纳包**：玩家可以先进来选角色，
     * 真正开战时才由 {@code CombatManager.start} 校验收纳包。
     */
    public static void open(ServerPlayer player) {
        StcAttachments.syncTo(player);
        // 队伍预览：开战时谁会被一起拉进来（服务端算好，界面不需要持有队伍状态）
        for (String line : CombatManager.partyPreviewLines(player, Config.combatRadius())) {
            StcChat.tell(player, Component.literal("[STC] " + line));
        }
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new SpireGateMenu(containerId, inventory),
                Component.translatable("stc.gate.title")));
    }
}
