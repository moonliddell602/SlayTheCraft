package com.slaythecraft.reward;

import com.slaythecraft.chat.StcChat;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.slaythecraft.SlayTheCraft;
import com.slaythecraft.data.PlayerCardData;
import com.slaythecraft.data.RewardOffer;
import com.slaythecraft.data.StcAttachments;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * S4 的调试 / 验收指令：{@code /stc reward …}。
 *
 * <p>没有它就得真打 10 只僵尸才能验收一次三选一，所以这几条是刻意的测试入口（需要管理员权限）。</p>
 */
@EventBusSubscriber(modid = SlayTheCraft.MODID)
public final class RewardCommands {

    private RewardCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("stc")
                .then(Commands.literal("reward")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("status").executes(RewardCommands::status))
                        .then(Commands.literal("open").executes(RewardCommands::open))
                        .then(Commands.literal("grant")
                                .executes(context -> grant(context, false))
                                .then(Commands.argument("golden", BoolArgumentType.bool())
                                        .executes(context -> grant(context,
                                                BoolArgumentType.getBool(context, "golden")))))
                        .then(Commands.literal("clear").executes(RewardCommands::clear))));
    }

    private static int status(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerCardData data = StcAttachments.dataOf(player);
        StcChat.feedback(context.getSource(), () -> Component.literal("[STC] 待领奖励 " + data.pendingRewards()
                + " 次　金币 " + data.gold() + "　卡牌 " + data.cardCount() + " 张"));
        RewardOffer offer = data.firstOffer();
        if (offer == null) {
            StcChat.feedback(context.getSource(), () -> Component.literal("[STC] 队首：无"));
            return 1;
        }
        StringBuilder sb = new StringBuilder("[STC] 队首候选（" + (offer.golden() ? "金色" : "普通") + "）：");
        if (offer.choices().isEmpty()) {
            sb.append("尚未抽取");
        } else {
            for (int i = 0; i < offer.choices().size(); i++) {
                ResourceLocation id = offer.choices().get(i);
                if (i > 0) {
                    sb.append("　");
                }
                sb.append(i + 1).append('.').append(id.getPath());
            }
        }
        StcChat.feedback(context.getSource(), () -> Component.literal(sb.toString()));
        return 1;
    }

    private static int open(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        RewardService.openMenu(context.getSource().getPlayerOrException());
        return 1;
    }

    private static int grant(CommandContext<CommandSourceStack> context, boolean golden) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String message = RewardService.grantForDebug(player, golden);
        StcChat.feedback(context.getSource(), () -> Component.literal("[STC] " + message));
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerCardData data = StcAttachments.dataOf(player);
        int removed = data.pendingRewards();
        while (data.consumePendingReward()) {
            // 全部清掉
        }
        StcAttachments.syncTo(player);
        int finalRemoved = removed;
        StcChat.feedback(context.getSource(), () -> Component.literal("[STC] 已清空 " + finalRemoved + " 次待领奖励。"));
        return removed;
    }
}
