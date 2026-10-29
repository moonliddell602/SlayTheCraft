package com.slaythecraft.binder;

import com.slaythecraft.chat.StcChat;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.slaythecraft.SlayTheCraft;
import com.slaythecraft.combat.core.Card;
import com.slaythecraft.combat.core.CardEnchantment;
import com.slaythecraft.combat.core.CardLibrary;
import com.slaythecraft.data.CardIds;
import com.slaythecraft.data.CardInstance;
import com.slaythecraft.data.PlayerCardData;
import com.slaythecraft.data.StcAttachments;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * S1 的调试 / 验收指令：{@code /stc binder …}。
 *
 * <p>需要管理员权限（等级 2）。这些指令是 S1 验收用的（金币、加卡、查档案），
 * 正式玩法里金币与卡牌只能通过击杀奖励与商店获得。指令树与 {@code /stc combat …}
 * 共用 {@code stc} 根节点，Brigadier 会自动合并同名字面量节点。</p>
 */
@EventBusSubscriber(modid = SlayTheCraft.MODID)
public final class BinderCommands {

    private static final SuggestionProvider<CommandSourceStack> CARD_IDS = (context, builder) -> {
        for (Card card : CardLibrary.all()) {
            builder.suggest(SlayTheCraft.MODID + ":" + card.id());
        }
        return builder.buildFuture();
    };

    /** V6：附魔的补全（英文键）。 */
    private static final SuggestionProvider<CommandSourceStack> ENCHANTMENT_IDS = (context, builder) -> {
        for (CardEnchantment enchantment : CardEnchantment.values()) {
            builder.suggest(enchantment.name().toLowerCase());
        }
        return builder.buildFuture();
    };

    private BinderCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("stc")
                .then(Commands.literal("binder")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("status").executes(BinderCommands::status))
                        .then(Commands.literal("give").executes(BinderCommands::give))
                        .then(Commands.literal("gold")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0, 1_000_000))
                                        .executes(context -> setGold(context,
                                                IntegerArgumentType.getInteger(context, "amount")))))
                        .then(Commands.literal("add-card")
                                .then(Commands.argument("card", ResourceLocationArgument.id())
                                        .suggests(CARD_IDS)
                                        .executes(context -> addCard(context, 1))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 99))
                                                .executes(context -> addCard(context,
                                                        IntegerArgumentType.getInteger(context, "count"))))))
                        .then(Commands.literal("remove-card")
                                .then(Commands.argument("card", ResourceLocationArgument.id())
                                        .suggests(CARD_IDS)
                                        .executes(BinderCommands::removeCard)))
                        .then(Commands.literal("enchant")
                                .then(Commands.argument("card", ResourceLocationArgument.id())
                                        .suggests(CARD_IDS)
                                        .then(Commands.argument("enchantment", StringArgumentType.word())
                                                .suggests(ENCHANTMENT_IDS)
                                                .executes(BinderCommands::enchant))))
                        .then(Commands.literal("clear").executes(BinderCommands::clear))));
    }

    private static int enchant(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ResourceLocation id = ResourceLocationArgument.getId(context, "card");
        String name = StringArgumentType.getString(context, "enchantment");
        CardEnchantment enchantment = CardEnchantment.byName(name);
        if (enchantment == null) {
            StcChat.failure(context.getSource(), Component.literal("[STC] 未知附魔：" + name
                    + "（可用 横扫之刃/火焰附加/亡灵杀手，或 sweeping_edge/fire_aspect/smite）"));
            return 0;
        }
        PlayerCardData data = StcAttachments.dataOf(player);
        if (!data.enchantFirstCard(id, enchantment)) {
            StcChat.failure(context.getSource(), Component.literal("[STC] 档案里没有这张卡：" + id));
            return 0;
        }
        StcAttachments.syncTo(player);
        StcChat.feedback(context.getSource(), () -> Component.literal(
                "[STC] 已为一张 " + id + " 附魔：" + enchantment.label() + "（下次战斗生效）"));
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        for (String line : BinderStatus.linesFor(player)) {
            StcChat.feedback(context.getSource(), () -> Component.literal("[STC] " + line));
        }
        return 1;
    }

    private static int give(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack stack = new ItemStack(SlayTheCraft.CARD_BINDER.get());
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        StcChat.feedback(context.getSource(), () -> Component.literal("[STC] 已获得一个卡牌收纳包。"));
        return 1;
    }

    private static int setGold(CommandContext<CommandSourceStack> context, int amount) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerCardData data = StcAttachments.dataOf(player);
        data.setGold(amount);
        StcAttachments.syncTo(player);
        StcChat.feedback(context.getSource(), () -> Component.literal("[STC] 金币已设为 " + data.gold() + "。"));
        return 1;
    }

    private static int addCard(CommandContext<CommandSourceStack> context, int count) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ResourceLocation id = ResourceLocationArgument.getId(context, "card");
        if (!CardIds.isKnown(id)) {
            StcChat.failure(context.getSource(), Component.literal("[STC] 未知卡牌：" + id
                    + "（S2 起改为从数据包注册表 stc:card 校验）"));
            return 0;
        }
        PlayerCardData data = StcAttachments.dataOf(player);
        for (int i = 0; i < count; i++) {
            data.addCard(CardInstance.of(id));
        }
        StcAttachments.syncTo(player);
        StcChat.feedback(context.getSource(), () -> Component.literal("[STC] 已加入 " + count + " 张 " + id
                + "，当前共 " + data.cardCount() + " 张。"));
        return count;
    }

    private static int removeCard(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ResourceLocation id = ResourceLocationArgument.getId(context, "card");
        PlayerCardData data = StcAttachments.dataOf(player);
        if (!data.removeFirstCard(id)) {
            StcChat.failure(context.getSource(), Component.literal("[STC] 档案里没有这张卡：" + id));
            return 0;
        }
        StcAttachments.syncTo(player);
        StcChat.feedback(context.getSource(), () -> Component.literal("[STC] 已删除一张 " + id
                + "，剩余 " + data.cardCount() + " 张。"));
        return 1;
    }

    /** V11：把第一张匹配的卡强化 +1（没有 id 时强化第一张可强化的卡）。 */
    private static int upgrade(CommandContext<CommandSourceStack> context, String id)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerCardData data = StcAttachments.dataOf(player);
        boolean ok = data.upgradeFirstCard(ResourceLocation.parse(id));
        if (!ok) {
            StcChat.failure(context.getSource(), Component.literal("[STC] 没有可强化的 " + id
                    + "（要么档案里没有这张卡，要么它没有强化版）。"));
            return 0;
        }
        StcAttachments.syncTo(player);
        StcChat.feedback(context.getSource(), () -> Component.literal("[STC] 已强化 " + id + "（数值见卡面「+」）。"));
        return 1;
    }

    private static int upgradeFirst(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerCardData data = StcAttachments.dataOf(player);
        for (CardInstance instance : data.cards()) {
            if (data.upgradeFirstCard(instance.id())) {
                StcAttachments.syncTo(player);
                StcChat.feedback(context.getSource(), () -> Component.literal(
                        "[STC] 已强化 " + instance.id() + "。"));
                return 1;
            }
        }
        StcChat.failure(context.getSource(), Component.literal("[STC] 档案里没有可强化的卡。"));
        return 0;
    }

    private static int clear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerCardData data = StcAttachments.dataOf(player);
        int removed = data.cardCount();
        for (int i = data.cardCount() - 1; i >= 0; i--) {
            data.removeCardAt(i);
        }
        StcAttachments.syncTo(player);
        StcChat.feedback(context.getSource(), () -> Component.literal("[STC] 已清空 " + removed + " 张卡牌。"));
        return removed;
    }
}
