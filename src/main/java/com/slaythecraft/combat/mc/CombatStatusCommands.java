package com.slaythecraft.combat.mc;

import com.slaythecraft.chat.StcChat;
import java.util.List;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.slaythecraft.SlayTheCraft;
import com.slaythecraft.combat.core.CombatContext;
import com.slaythecraft.combat.core.Combatant;
import com.slaythecraft.combat.core.Enemy;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * v0.0.2 V5 的调试指令：{@code /stc status …}。
 *
 * <p>新效果（迅捷/缓慢/灼烧/凋零/中毒/神龟）目前还没有正式卡牌去施加它们（那是 V11 事件卡的事），
 * 所以这里提供一个人为施加的入口，方便在游戏里直接验收这些效果的行为。
 * 需要管理员权限。</p>
 */
@EventBusSubscriber(modid = SlayTheCraft.MODID)
public final class CombatStatusCommands {

    private static final List<String> STATUS_NAMES = List.of(
            "strength", "weak", "vulnerable", "dexterity", "slow", "burn", "decay", "poison", "plated");

    private static final SuggestionProvider<CommandSourceStack> STATUS_SUGGESTIONS = (context, builder) -> {
        for (String name : STATUS_NAMES) {
            builder.suggest(name);
        }
        return builder.buildFuture();
    };

    /** 状态牌（Tab 补全；v0.5.2.1 加了『伤口』）。 */
    private static final List<String> STATUS_CARDS = List.of(
            "void", "slimed", "burn", "dazed", "venom", "call", "wound");

    private static final SuggestionProvider<CommandSourceStack> STATUS_CARD_SUGGESTIONS = (context, builder) -> {
        for (String id : STATUS_CARDS) {
            builder.suggest(id);
        }
        return builder.buildFuture();
    };

    /** 把状态牌塞进自己的弃牌堆（战斗中才有效）。 */
    private static int statusCard(CommandContext<CommandSourceStack> context, int count)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String cardId = StringArgumentType.getString(context, "card");
        String message = CombatManager.giveStatusCard(player, cardId, count);
        StcChat.feedback(context.getSource(), () -> Component.literal("[STC] " + message));
        return 1;
    }

    private CombatStatusCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("stc")
                .then(Commands.literal("status")
                        .requires(source -> source.hasPermission(2))
                        .executes(CombatStatusCommands::show)
                        .then(Commands.literal("add")
                                .then(Commands.argument("status", StringArgumentType.word())
                                        .suggests(STATUS_SUGGESTIONS)
                                        .then(Commands.argument("stacks", IntegerArgumentType.integer(1, 999))
                                                .executes(context -> add(context, -1))
                                                .then(Commands.argument("enemy", IntegerArgumentType.integer(1, 8))
                                                        .executes(context -> add(context,
                                                                IntegerArgumentType.getInteger(context, "enemy") - 1))))))
                        .then(Commands.literal("clear").executes(CombatStatusCommands::clear))
                        // v0.0.3 W2：把状态牌塞进自己的弃牌堆（验收状态牌用）
                        .then(Commands.literal("card")
                                .then(Commands.argument("card", StringArgumentType.word())
                                        .suggests(STATUS_CARD_SUGGESTIONS)
                                        .executes(context -> statusCard(context, 1))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 9))
                                                .executes(context -> statusCard(context,
                                                        IntegerArgumentType.getInteger(context, "count"))))))));
    }

    private static int show(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        CombatContext combat = CombatManager.combatOf(player);
        if (combat == null) {
            StcChat.failure(context.getSource(), Component.literal("[STC] 你当前不在战斗中。"));
            return 0;
        }
        Combatant self = combat.combatantNamed(player.getName().getString());
        for (String line : combat.statusLines()) {
            StcChat.feedback(context.getSource(), () -> Component.literal("[STC] " + line));
        }
        if (self != null) {
            StcChat.feedback(context.getSource(), () -> Component.literal("[STC] 你的状态："
                    + (self.hasStatuses() ? self.statusSuffix().trim() : "无")));
        }
        return 1;
    }

    private static int add(CommandContext<CommandSourceStack> context, int enemyIndex) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        CombatContext combat = CombatManager.combatOf(player);
        if (combat == null) {
            StcChat.failure(context.getSource(), Component.literal("[STC] 你当前不在战斗中。"));
            return 0;
        }
        String status = StringArgumentType.getString(context, "status").toLowerCase();
        int stacks = IntegerArgumentType.getInteger(context, "stacks");

        Combatant target;
        if (enemyIndex >= 0) {
            List<Enemy> enemies = combat.enemies();
            if (enemyIndex >= enemies.size()) {
                StcChat.failure(context.getSource(), Component.literal("[STC] 敌人序号超出范围（本场 " + enemies.size() + " 个）。"));
                return 0;
            }
            target = enemies.get(enemyIndex);
        } else {
            target = combat.combatantNamed(player.getName().getString());
        }
        if (target == null) {
            StcChat.failure(context.getSource(), Component.literal("[STC] 找不到目标。"));
            return 0;
        }
        if (!apply(target, status, stacks)) {
            StcChat.failure(context.getSource(), Component.literal("[STC] 未知状态：" + status
                    + "（可用：" + String.join("/", STATUS_NAMES) + "）"));
            return 0;
        }
        String line = "[STC] 已为 " + target.name() + " 施加 " + status + " " + stacks
                + " →" + target.statusSuffix();
        StcChat.feedback(context.getSource(), () -> Component.literal(line));
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        CombatContext combat = CombatManager.combatOf(player);
        if (combat == null) {
            StcChat.failure(context.getSource(), Component.literal("[STC] 你当前不在战斗中。"));
            return 0;
        }
        Combatant self = combat.combatantNamed(player.getName().getString());
        if (self != null) {
            self.clearStatuses();
        }
        for (Enemy enemy : combat.enemies()) {
            enemy.clearStatuses();
        }
        StcChat.feedback(context.getSource(), () -> Component.literal("[STC] 已清空场上所有状态。"));
        return 1;
    }

    /** 状态名 → 施加（同时接受英文键与中文名）。 */
    private static boolean apply(Combatant target, String status, int stacks) {
        switch (status) {
            case "strength", "力量" -> target.addStrength(stacks);
            case "weak", "虚弱" -> target.addWeak(stacks);
            // v0.5.1.4：旧的 -25% 机制现在叫【挖掘疲劳】
            case "mining", "mining_fatigue", "挖掘疲劳" -> target.addMiningFatigue(stacks);
            case "vulnerable", "易伤" -> target.addVulnerable(stacks);
            case "dexterity", "迅捷" -> target.addDexterity(stacks);
            case "slow", "缓慢" -> target.addSlow(stacks);
            case "burn", "灼烧" -> target.addBurn(stacks);
            case "decay", "凋零" -> target.addDecay(stacks);
            case "poison", "中毒" -> target.addPoison(stacks);
            case "plated", "神龟" -> target.addPlatedArmor(stacks);
            default -> {
                return false;
            }
        }
        return true;
    }
}
