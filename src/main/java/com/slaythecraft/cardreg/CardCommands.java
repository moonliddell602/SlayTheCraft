package com.slaythecraft.cardreg;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.slaythecraft.SlayTheCraft;
import com.slaythecraft.chat.StcChat;
import com.slaythecraft.combat.core.Card;
import com.slaythecraft.combat.core.CardBehaviors;
import com.slaythecraft.combat.core.CardLibrary;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /stc card …} —— 数据包卡牌注册表的控制面（v0.5.5.0 A6-1）。
 *
 * <pre>
 * /stc card list        列出当前来自数据包的卡（以及内置卡总数）
 * /stc card info &lt;id&gt;   看某一张卡现在的定义（内置 / 数据包、基础与强化版）
 * /stc card behaviors   列出 JSON 里可以写的全部行为 id
 * /stc card validate    重新校验数据包定义并报告问题（不改变运行中的卡池）
 * /stc card reload      把注册表重新灌进卡池（改完数据包 /reload 之后可以手动再来一次）
 * </pre>
 */
@EventBusSubscriber(modid = SlayTheCraft.MODID)
public final class CardCommands {

    private CardCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("stc")
                .then(Commands.literal("card")
                        .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                        .then(Commands.literal("behaviors").executes(ctx -> behaviors(ctx.getSource())))
                        .then(Commands.literal("validate").executes(ctx -> validate(ctx.getSource())))
                        .then(Commands.literal("reload").executes(ctx -> reload(ctx.getSource())))
                        .then(Commands.literal("info")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> info(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "id")))))
                        .executes(ctx -> help(ctx.getSource()))));
    }

    private static int help(CommandSourceStack source) {
        StcChat.feedback(source, () -> Component.literal("""
                [STC] 数据包卡牌注册表 stc:card
                  文件放在 data/<数据包>/stc/card/<卡牌 id>.json
                  /stc card list       · 列出数据包卡
                  /stc card info <id>  · 看一张卡现在的定义
                  /stc card behaviors  · 列出可用的行为 id
                  /stc card validate   · 校验并报告问题
                  /stc card reload     · 重新灌进卡池"""));
        return 1;
    }

    private static int list(CommandSourceStack source) {
        int total = CardLibrary.all().size();
        var external = CardLibrary.externalIds();
        StcChat.feedback(source, () -> Component.literal("[STC] 卡池共 " + total + " 张，其中数据包提供 "
                + external.size() + " 张" + (external.isEmpty() ? "。" : "：" + String.join("、", external))));
        return 1;
    }

    private static int behaviors(CommandSourceStack source) {
        for (CardBehaviors.Definition definition : CardBehaviors.all()) {
            StcChat.feedback(source, () -> Component.literal("  " + definition.id() + "（数值参数 "
                    + definition.numbers() + " / 文本参数 " + definition.strings() + "）—— "
                    + definition.summary()));
        }
        StcChat.feedback(source, () -> Component.literal("[STC] 共 " + CardBehaviors.count() + " 条行为。"));
        return 1;
    }

    private static int validate(CommandSourceStack source) {
        var result = CardRegistryLoader.apply(source.getServer().registryAccess());
        if (result.clean()) {
            StcChat.feedback(source, () -> Component.literal("[STC] 数据包卡牌校验通过：" + result.applied()
                    + " 张已生效。"));
            return 1;
        }
        StcChat.failure(source, Component.literal("[STC] 有 " + result.errors().size() + " 条问题（这些卡被跳过，"
                + "其余 " + result.applied() + " 张照常生效）："));
        for (String error : result.errors()) {
            StcChat.failure(source, Component.literal("  - " + error));
        }
        return 0;
    }

    private static int reload(CommandSourceStack source) {
        // 先还原内置卡池：这样"数据包把某个文件删了"之后，重新加载也能回到内置定义
        CardLibrary.resetExternal();
        var result = CardRegistryLoader.apply(source.getServer().registryAccess());
        StcChat.feedback(source, () -> Component.literal("[STC] 已重载 stc:card：" + result.applied()
                + " 张数据包卡生效（跳过 " + result.errors().size() + " 张）。"));
        for (String error : result.errors()) {
            StcChat.failure(source, Component.literal("  - " + error));
        }
        return result.clean() ? 1 : 0;
    }

    private static int info(CommandSourceStack source, String raw) {
        String path = raw.contains(":") ? ResourceLocation.parse(raw).getPath() : raw;
        Card base = CardLibrary.byId(path);
        if (base == null) {
            StcChat.failure(source, Component.literal("[STC] 没有这张卡：" + raw));
            return 0;
        }
        String origin = CardLibrary.isExternal(path) ? "数据包" : "代码内置";
        StcChat.feedback(source, () -> Component.literal("[STC] " + path + "（" + origin + "）"));
        StcChat.feedback(source, () -> Component.literal("  " + describe(base)));
        Card upgraded = CardLibrary.upgraded(path);
        if (upgraded != null) {
            StcChat.feedback(source, () -> Component.literal("  " + describe(upgraded)));
        } else {
            StcChat.feedback(source, () -> Component.literal("  （没有强化版）"));
        }
        StcChat.feedback(source, () -> Component.literal("  关键词：" + base.keywordLabel()
                + " · 最多强化 " + CardLibrary.maxUpgrades(path) + " 次"
                + (CardLibrary.isXCost(path) ? " · X 费" : "")
                + (CardLibrary.isGenerated(path) ? " · 生成牌" : "")));
        return 1;
    }

    private static String describe(Card card) {
        return card.name() + " | " + card.cost() + " 费 · " + card.type().label() + " · "
                + card.rarity().label() + " | " + card.description();
    }
}
