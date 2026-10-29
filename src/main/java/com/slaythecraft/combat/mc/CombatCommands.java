package com.slaythecraft.combat.mc;

import com.slaythecraft.chat.StcChat;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.slaythecraft.SlayTheCraft;
import com.slaythecraft.combat.core.CombatSelfTest;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /stc combat …} — the temporary control surface of the combat prototype. Until a card GUI
 * exists, these commands are how a battle is played and inspected.
 */
// NeoForge 21.1 起 EventBusSubscriber.bus() 已标记待删除：GAME 就是默认总线，不再显式指定。
@EventBusSubscriber(modid = SlayTheCraft.MODID)
public final class CombatCommands {

    private CombatCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("stc")
                .then(Commands.literal("combat")
                        .then(Commands.literal("start")
                                .executes(ctx -> start(ctx.getSource(), -1))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(2, 48))
                                        .executes(ctx -> start(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius")))))
                        .then(Commands.literal("play")
                                .then(Commands.argument("hand", IntegerArgumentType.integer(1, 20))
                                        .executes(ctx -> play(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "hand"), 0))
                                        .then(Commands.argument("enemy", IntegerArgumentType.integer(1, 8))
                                                .executes(ctx -> play(ctx.getSource(),
                                                        IntegerArgumentType.getInteger(ctx, "hand"),
                                                        IntegerArgumentType.getInteger(ctx, "enemy"))))))
                        .then(Commands.literal("end").executes(ctx -> end(ctx.getSource())))
                        .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
                        .then(Commands.literal("quit").executes(ctx -> quit(ctx.getSource())))
                        .then(Commands.literal("selftest").executes(ctx -> selftest(ctx.getSource())))
                        .then(Commands.literal("help").executes(ctx -> help(ctx.getSource())))
                        .executes(ctx -> help(ctx.getSource()))));
    }

    private static int start(CommandSourceStack source, int radius) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String failure = CombatManager.start(player, radius);
        if (failure != null) {
            StcChat.failure(source, Component.literal("[STC] " + failure));
            return 0;
        }
        StcChat.feedback(source, () -> Component.literal("[STC] 战斗开始！用 /stc combat status 查看手牌与敌人意图。"));
        return 1;
    }

    private static int play(CommandSourceStack source, int hand, int enemy) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String failure = CombatManager.playCard(player, hand, enemy);
        if (failure != null) {
            StcChat.failure(source, Component.literal("[STC] " + failure));
            return 0;
        }
        return 1;
    }

    private static int end(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String failure = CombatManager.endTurn(player);
        if (failure != null) {
            StcChat.failure(source, Component.literal("[STC] " + failure));
            return 0;
        }
        return 1;
    }

    private static int status(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var lines = CombatManager.status(player);
        if (lines.isEmpty()) {
            StcChat.failure(source, Component.literal("[STC] 你当前不在战斗中。"));
            return 0;
        }
        for (String line : lines) {
            StcChat.feedback(source, () -> Component.literal("[STC] " + line));
        }
        return 1;
    }

    private static int quit(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String failure = CombatManager.quit(player, "主动放弃");
        if (failure != null) {
            StcChat.failure(source, Component.literal("[STC] " + failure));
            return 0;
        }
        return 1;
    }

    private static int selftest(CommandSourceStack source) {
        CombatSelfTest.Result result = CombatSelfTest.run();
        SlayTheCraft.LOGGER.info("[STC] self-test {}", result.summary());
        for (String line : result.lines()) {
            SlayTheCraft.LOGGER.info("[STC] {}", line);
        }
        StcChat.feedback(source, () -> Component.literal("[STC] 战斗规则自检：" + result.summary()));
        for (String line : result.lines()) {
            if (line.startsWith("FAIL")) {
                StcChat.failure(source, Component.literal("[STC] " + line));
            }
        }
        return result.ok() ? 1 : 0;
    }

    private static int help(CommandSourceStack source) {
        StcChat.feedback(source, () -> Component.literal("[STC] 卡牌战斗指令："));
        StcChat.feedback(source, () -> Component.literal("[STC]  /stc combat start [半径]  —— 与附近敌对生物开战"));
        StcChat.feedback(source, () -> Component.literal("[STC]  /stc combat status        —— 查看回合、手牌、敌人意图"));
        StcChat.feedback(source, () -> Component.literal("[STC]  /stc combat play <手牌> [敌人] —— 打出第 N 张手牌"));
        StcChat.feedback(source, () -> Component.literal("[STC]  /stc combat end           —— 结束回合，敌人行动"));
        StcChat.feedback(source, () -> Component.literal("[STC]  /stc combat quit          —— 放弃当前战斗"));
        StcChat.feedback(source, () -> Component.literal("[STC]  /stc combat selftest      —— 运行战斗规则自检"));
        return 1;
    }
}
