package com.slaythecraft.dev;

import com.mojang.brigadier.CommandDispatcher;

import com.slaythecraft.chat.StcChat;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import com.slaythecraft.SlayTheCraft;

/**
 * 开发者模式指令（v0.5.1.0）。
 *
 * <pre>
 * /stc dev on     开启开发者模式（并发放 9999999 金币）
 * /stc dev off    关闭
 * /stc dev status 查看当前状态与四项便利的开关情况
 * </pre>
 *
 * <p>需求原文："新增：开发者模式 —— 为开发者预留的指令。使用指令启用开发者模式后：
 * 每场战斗开始时获得 9999 层缓冲和 9999 力量。可以在图鉴里直接向卡组添加卡牌。
 * 可以直接在查看卡组页面删牌。获得 9999999 金币。"</p>
 */
@EventBusSubscriber(modid = SlayTheCraft.MODID)
public final class DevCommands {

    private DevCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("stc")
                .then(Commands.literal("dev")
                        .then(Commands.literal("on").executes(ctx -> toggle(ctx.getSource(), true)))
                        .then(Commands.literal("off").executes(ctx -> toggle(ctx.getSource(), false)))
                        .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))));
    }

    private static int toggle(CommandSourceStack source, boolean on) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            StcChat.failure(source, Component.literal("[STC] 该指令只能由玩家执行。"));
            return 0;
        }
        String message = on ? DevMode.enable(player) : DevMode.disable(player);
        StcChat.tell(player, Component.literal("[STC] " + message));
        return 1;
    }

    private static int status(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            StcChat.failure(source, Component.literal("[STC] 该指令只能由玩家执行。"));
            return 0;
        }
        boolean on = DevMode.isEnabled(player);
        StcChat.tell(player, Component.literal("[STC] 开发者模式：" + (on ? "已开启" : "未开启")));
        if (on) {
            StcChat.tell(player, Component.literal("[STC] 开局加成：" + DevMode.DEV_BUFFER + " 层缓冲 + "
                    + DevMode.DEV_STRENGTH + " 力量；金币 " + DevMode.DEV_GOLD));
            StcChat.tell(player, Component.literal("[STC] 图鉴加卡：" + DevMode.canEditDeckFromCodex(player)
                    + "　查看牌组删牌：" + DevMode.canDeleteFromDeckView(player)));
            StcChat.tell(player, Component.literal("[STC] " + DevMode.warningLine()));
        }
        return 1;
    }
}
