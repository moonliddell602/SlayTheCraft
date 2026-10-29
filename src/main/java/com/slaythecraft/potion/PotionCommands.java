package com.slaythecraft.potion;

import com.slaythecraft.chat.StcChat;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.slaythecraft.Config;
import com.slaythecraft.SlayTheCraft;
import com.slaythecraft.combat.core.Potion;
import com.slaythecraft.combat.core.PotionLibrary;
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
 * v0.0.2 V8 的调试指令：{@code /stc potion …}。
 *
 * <p>药水的正式来源还没铺开（V11 的牧师事件等），但验收"槽位 / 对自己 / 对敌人"需要手上先有药水，
 * 所以这里可以直接发放。需要管理员权限。</p>
 */
@EventBusSubscriber(modid = SlayTheCraft.MODID)
public final class PotionCommands {

    private static final SuggestionProvider<CommandSourceStack> POTION_IDS = (context, builder) -> {
        for (Potion potion : PotionLibrary.all()) {
            builder.suggest(potion.id());
        }
        return builder.buildFuture();
    };

    private PotionCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("stc")
                .then(Commands.literal("potion")
                        .requires(source -> source.hasPermission(2))
                        .executes(PotionCommands::list)
                        .then(Commands.literal("list").executes(PotionCommands::list))
                        .then(Commands.literal("give")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(POTION_IDS)
                                        .executes(context -> give(context,
                                                StringArgumentType.getString(context, "id")))))
                        .then(Commands.literal("clear").executes(PotionCommands::clear))));
    }

    private static int list(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerCardData data = StcAttachments.dataOf(player);
        int slots = Config.potionSlots();
        StcChat.feedback(context.getSource(), () -> Component.literal(
                "[STC] 药水槽位 " + data.potionCount() + "/" + slots + "："));
        for (int i = 0; i < slots; i++) {
            String id = data.potionAt(i);
            Potion potion = id == null ? null : PotionLibrary.byId(id);
            String line = potion == null
                    ? "[STC] 槽位 " + (i + 1) + "：空"
                    : "[STC] 槽位 " + (i + 1) + "：" + potion.name() + "（" + potion.id() + "）—— "
                            + potion.description();
            StcChat.feedback(context.getSource(), () -> Component.literal(line));
        }
        return 1;
    }

    private static int give(CommandContext<CommandSourceStack> context, String id) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Potion potion = PotionLibrary.byId(id);
        if (potion == null) {
            StcChat.failure(context.getSource(), Component.literal("[STC] 没有这瓶药水：" + id));
            return 0;
        }
        if (!PotionService.give(player, potion)) {
            StcChat.failure(context.getSource(), Component.literal(
                    "[STC] 药水槽位已满（" + Config.potionSlots() + " 格），先用掉一瓶或调大 potionSlots。"));
            return 0;
        }
        StcChat.feedback(context.getSource(), () -> Component.literal(
                "[STC] 获得药水【" + potion.name() + "】—— " + potion.description()));
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerCardData data = StcAttachments.dataOf(player);
        int removed = data.potionCount();
        data.clearPotions();
        StcAttachments.syncTo(player);
        StcChat.feedback(context.getSource(), () -> Component.literal("[STC] 已清空 " + removed + " 瓶药水。"));
        return removed;
    }
}
