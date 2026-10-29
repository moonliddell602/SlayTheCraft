package com.slaythecraft.relic;

import com.slaythecraft.chat.StcChat;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.slaythecraft.SlayTheCraft;
import com.slaythecraft.combat.core.Relic;
import com.slaythecraft.combat.core.RelicLibrary;
import com.slaythecraft.data.PlayerCardData;
import com.slaythecraft.data.StcAttachments;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * v0.0.2 V7 的调试指令：{@code /stc relic …}。
 *
 * <p>遗物的正式来源是"击败精英必掉"，但要凑一场精英战不方便，所以这里可以直接发放，
 * 用来验收"战斗内增益"与"收纳包遗物页"。需要管理员权限。</p>
 */
@EventBusSubscriber(modid = SlayTheCraft.MODID)
public final class RelicCommands {

    private static final SuggestionProvider<CommandSourceStack> RELIC_IDS = (context, builder) -> {
        for (Relic relic : RelicLibrary.all()) {
            builder.suggest(relic.id());
        }
        return builder.buildFuture();
    };

    private RelicCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("stc")
                .then(Commands.literal("relic")
                        .requires(source -> source.hasPermission(2))
                        .executes(RelicCommands::list)
                        .then(Commands.literal("list").executes(RelicCommands::list))
                        .then(Commands.literal("grant")
                                .executes(context -> grantRandom(context))
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(RELIC_IDS)
                                        .executes(context -> grant(context,
                                                StringArgumentType.getString(context, "id")))))
                        .then(Commands.literal("clear").executes(RelicCommands::clear))));
    }

    private static int list(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerCardData data = StcAttachments.dataOf(player);
        StcChat.feedback(context.getSource(), () -> Component.literal("[STC] 已拥有 " + data.relicCount() + " 件遗物："));
        for (Relic relic : RelicLibrary.all()) {
            boolean owned = data.hasRelic(relic.id());
            String line = "[STC] " + (owned ? "✔ " : "✘ ") + relic.id() + "　" + relic.describe();
            StcChat.feedback(context.getSource(), () -> Component.literal(line));
        }
        return 1;
    }

    private static int grantRandom(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerCardData data = StcAttachments.dataOf(player);
        Relic relic = RelicLibrary.randomUnowned(new java.util.Random(player.getRandom().nextLong()), data.relics());
        if (relic == null) {
            StcChat.failure(context.getSource(), Component.literal("[STC] 所有遗物都已拥有。"));
            return 0;
        }
        data.addRelic(relic.id());
        // v0.5.3.0：指令这条路也要走"获得时"效果（破旧打火石 / 制图台）
        RelicObtainService.apply(player, relic.id());
        StcAttachments.syncTo(player);
        StcChat.feedback(context.getSource(), () -> Component.literal(
                "[STC] 获得遗物【" + relic.name() + "】—— " + relic.description()));
        return 1;
    }

    private static int grant(CommandContext<CommandSourceStack> context, String id) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Relic relic = RelicLibrary.byId(id);
        if (relic == null) {
            StcChat.failure(context.getSource(), Component.literal("[STC] 没有这件遗物：" + id));
            return 0;
        }
        // v0.4.0：初始遗物是"固定角色特有的"，连调试指令也不发放（只能靠对应角色开局）
        if (RelicLibrary.isStarting(relic.id())) {
            StcChat.failure(context.getSource(), Component.literal("[STC] 【" + relic.name()
                    + "】是某个角色的初始遗物，只能在那个角色开局时获得。"));
            return 0;
        }
        PlayerCardData data = StcAttachments.dataOf(player);
        boolean added = data.addRelic(relic.id());
        if (added) {
            RelicObtainService.apply(player, relic.id());
        }
        StcAttachments.syncTo(player);
        String message = added
                ? "获得遗物【" + relic.name() + "】—— " + relic.description()
                : "你已经拥有【" + relic.name() + "】了。";
        StcChat.feedback(context.getSource(), () -> Component.literal("[STC] " + message));
        return added ? 1 : 0;
    }

    private static int clear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerCardData data = StcAttachments.dataOf(player);
        int removed = data.relicCount();
        data.clearRelics();
        StcAttachments.syncTo(player);
        StcChat.feedback(context.getSource(), () -> Component.literal("[STC] 已清空 " + removed + " 件遗物。"));
        return removed;
    }
}
