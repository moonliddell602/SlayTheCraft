package com.slaythecraft.progress;

import com.slaythecraft.chat.StcChat;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.slaythecraft.Config;
import com.slaythecraft.SlayTheCraft;
import com.slaythecraft.combat.core.CombatSelfTest;
import com.slaythecraft.combat.core.EventCaps;
import com.slaythecraft.combat.core.EventKind;
import com.slaythecraft.combat.core.FloorBoss;
import com.slaythecraft.combat.core.LifePool;
import com.slaythecraft.combat.core.ProgressState;
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
 * v0.0.2 V9 的调试指令：{@code /stc progress …} —— 独立生命值与三层进度。
 *
 * <p>正式玩法里生命值由战斗与火堆改变、层数由击败 BOSS 推进，这里是为了不必真去打三层就能验收
 * （也方便排查"我的血量怎么变成 0 了"）。需要管理员权限。</p>
 */
@EventBusSubscriber(modid = SlayTheCraft.MODID)
public final class ProgressCommands {

    private ProgressCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("stc")
                .then(Commands.literal("progress")
                        .requires(source -> source.hasPermission(2))
                        .executes(ProgressCommands::show)
                        .then(Commands.literal("show").executes(ProgressCommands::show))
                        .then(Commands.literal("heal")
                                .executes(context -> heal(context, -1))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1000))
                                        .executes(context -> heal(context,
                                                IntegerArgumentType.getInteger(context, "amount")))))
                        .then(Commands.literal("damage")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1000))
                                        .executes(context -> damage(context,
                                                IntegerArgumentType.getInteger(context, "amount")))))
                        .then(Commands.literal("boss").executes(ProgressCommands::defeatBoss))
                        .then(Commands.literal("campfire").executes(ProgressCommands::campfire))
                        .then(Commands.literal("restart").executes(ProgressCommands::restart))
                        .then(Commands.literal("endrun").executes(ProgressCommands::endRun))
                        .then(Commands.literal("floor")
                                .then(Commands.argument("floor", IntegerArgumentType.integer(1, FloorBoss.FLOORS))
                                        .executes(context -> setFloor(context,
                                                IntegerArgumentType.getInteger(context, "floor")))))
                        .then(Commands.literal("reset").executes(ProgressCommands::reset))
                        .then(Commands.literal("selftest").executes(ProgressCommands::selftest))));
    }

    private static int show(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerCardData data = StcAttachments.dataOf(player);
        EventCaps caps = Config.eventCaps();
        ProgressState progress = data.progress();
        StcChat.feedback(context.getSource(), () -> Component.literal(
                "[STC] 独立生命值 " + data.life().describe() + "（上限 " + Config.maxPlayerHp()
                        + "，配置值）"));
        StcChat.feedback(context.getSource(), () -> Component.literal("[STC] " + progress.describe(caps)));
        for (EventKind kind : ProgressState.CAPPED_KINDS) {
            int count = progress.countOf(kind);
            int cap = caps.capOf(kind);
            String line = "[STC] " + kind.label() + "：" + count + "/" + cap
                    + (progress.canTrigger(kind, caps) ? "（还可触发）" : "（已达上限）");
            StcChat.feedback(context.getSource(), () -> Component.literal(line));
        }
        FloorBoss boss = progress.boss();
        if (boss != null) {
            StcChat.feedback(context.getSource(), () -> Component.literal("[STC] 本层 BOSS【" + boss.name()
                    + "】" + boss.entityId() + "：用尖塔入口的「挑战 BOSS」按钮发起（需在 "
                    + Config.bossChallengeRadius() + " 格内找到它）"));
        }
        return 1;
    }

    private static int heal(CommandContext<CommandSourceStack> context, int amount) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerCardData data = StcAttachments.dataOf(player);
        LifePool before = data.life();
        data.setLife(amount < 0 ? before.restore() : before.heal(amount));
        StcAttachments.syncTo(player);
        String text = "[STC] 生命值 " + before.describe() + " → " + data.life().describe();
        StcChat.feedback(context.getSource(), () -> Component.literal(text));
        return data.hp();
    }

    private static int damage(CommandContext<CommandSourceStack> context, int amount) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerCardData data = StcAttachments.dataOf(player);
        LifePool before = data.life();
        data.setLife(before.damage(amount));
        StcAttachments.syncTo(player);
        String text = "[STC] 生命值 " + before.describe() + " → " + data.life().describe();
        StcChat.feedback(context.getSource(), () -> Component.literal(text));
        return data.hp();
    }

    private static int defeatBoss(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerCardData data = StcAttachments.dataOf(player);
        int defeatedFloor = data.floor();
        ProgressState after = data.recordBossDefeat();
        StcAttachments.syncTo(player);
        String text = after.completed()
                ? "[STC] 已记录：击败第 " + defeatedFloor + " 层 BOSS —— 三层通关。"
                : "[STC] 已记录：击败第 " + defeatedFloor + " 层 BOSS → 第 " + after.floor() + " 层，目标【"
                        + (after.boss() == null ? "?" : after.boss().name()) + "】。";
        StcChat.feedback(context.getSource(), () -> Component.literal(text));
        return after.floor();
    }

    /** 不开世界里的篝火也能验收休息规则（满血 / 连续使用 / 次数上限都会被拒绝）。 */
    private static int campfire(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String result = com.slaythecraft.campfire.CampfireService.rest(player);
        StcChat.feedback(context.getSource(), () -> Component.literal("[STC] " + result));
        // v0.5.0：篝火现在是"打开准备界面"，这条指令只做"愈合"；只要没被拒绝就算成功
        return result.startsWith("愈合") ? 1 : 0;
    }

    /** 重开本局（与尖塔入口的「重开本局」按钮同一个动作：清空本局 + 清空角色，等玩家重新选角色）。 */
    private static int restart(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String message = com.slaythecraft.character.RunService.clearForReselect(player);
        StcChat.feedback(context.getSource(), () -> Component.literal("[STC] " + message));
        return 1;
    }

    /** 把本局标记成"已结束（战败）"，用来验收"本局结束前不能换角色"这条规则。 */
    private static int endRun(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerCardData data = StcAttachments.dataOf(player);
        data.markRunDefeated();
        StcAttachments.syncTo(player);
        StcChat.feedback(context.getSource(), () -> Component.literal(
                "[STC] 本局已标记为结束（战败）：现在可以在尖塔入口换角色了（换角色 = 开新的一局）。"));
        return 1;
    }

    private static int setFloor(CommandContext<CommandSourceStack> context, int floor) throws CommandSyntaxException {        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerCardData data = StcAttachments.dataOf(player);
        ProgressState current = data.progress();
        data.setProgress(ProgressState.of(floor, java.util.Map.of(), current.bossesDefeated(),
                current.completed()));
        StcAttachments.syncTo(player);
        String text = "[STC] 层数设为 " + floor + "，本层事件计数已清零。";
        StcChat.feedback(context.getSource(), () -> Component.literal(text));
        return floor;
    }

    private static int reset(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerCardData data = StcAttachments.dataOf(player);
        data.setProgress(data.progress().reset());
        StcAttachments.syncTo(player);
        StcChat.feedback(context.getSource(), () -> Component.literal("[STC] 进度已重置：回到第 1 层（生命值不动）。"));
        return 1;
    }

    /** 游戏内跑一遍引擎自检（V9 把生命值与进度规则也加进去了）。 */
    private static int selftest(CommandContext<CommandSourceStack> context) {
        CombatSelfTest.Result result = CombatSelfTest.run();
        StcChat.feedback(context.getSource(), () -> Component.literal("[STC] " + result.summary()));
        result.lines().stream().filter(line -> line.startsWith("FAIL")).limit(5)
                .forEach(line -> StcChat.feedback(context.getSource(),
                        () -> Component.literal("[STC] " + line)));
        return result.ok() ? 1 : 0;
    }
}
