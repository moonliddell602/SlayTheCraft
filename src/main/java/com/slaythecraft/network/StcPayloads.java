package com.slaythecraft.network;

import com.slaythecraft.chat.StcChat;
import com.slaythecraft.binder.BinderOpener;
import com.slaythecraft.client.ClientBattleFx;
import com.slaythecraft.client.ClientBattleState;
import com.slaythecraft.client.ClientCampfireState;
import com.slaythecraft.client.ClientCardPickState;
import com.slaythecraft.client.ClientDevState;
import com.slaythecraft.client.ClientQuestionState;
import com.slaythecraft.client.ClientShopOffers;
import com.slaythecraft.combat.mc.CombatManager;
import com.slaythecraft.potion.PotionService;
import com.slaythecraft.question.QuestionService;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 网络包注册（在主类构造里挂到 mod 事件总线）。
 *
 * <p>协议版本号 {@code "1"}：改动包结构时必须递增，否则新旧客户端会静默不匹配。</p>
 */
public final class StcPayloads {

    /** 协议版本；任何包结构变更都要 +1。v2：新增战斗快照与出牌/结束回合。 */
    public static final String PROTOCOL_VERSION = "2";

    private StcPayloads() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(OpenBinderPayload.TYPE, OpenBinderPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        BinderOpener.open(player);
                    }
                }));
        // 商店货架：服务端 → 客户端。处理体里引用的是客户端类，但这段代码只会在客户端执行，
        // 因此专用服务器不会加载 ClientShopOffers。
        registrar.playToClient(ShopOffersPayload.TYPE, ShopOffersPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientShopOffers.accept(payload)));
        // V1：战斗快照（服务端 → 客户端）与出牌/结束回合（客户端 → 服务端）
        registrar.playToClient(BattleStatePayload.TYPE, BattleStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientBattleState.accept(payload)));
        // v0.3.0：战斗表现层事件（攻击/格挡/受击的动画与音效）
        registrar.playToClient(BattleFxPayload.TYPE, BattleFxPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientBattleFx.accept(payload)));
        // v0.4.4：客户端把"战斗信息栏的屏幕位置"发上来，服务端据此把怪物摆到栏位正下方
        registrar.playToServer(BattleAlignPayload.TYPE, BattleAlignPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        CombatManager.alignEnemies(player, payload.centers(), payload.fov(), payload.aspect());
                    }
                }));
        registrar.playToServer(PlayCardPayload.TYPE, PlayCardPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        // 载荷是 0 基，CombatManager 用 1 基
                        CombatManager.playCard(player, payload.handIndex() + 1, payload.enemyIndex() + 1);
                    }
                }));
        registrar.playToServer(EndTurnPayload.TYPE, EndTurnPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        CombatManager.endTurn(player);
                    }
                }));
        // v0.5.0.9：挑战"本层可挑战名单"里点的那一只
        registrar.playToServer(ChallengeTargetPayload.TYPE, ChallengeTargetPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        String failure = CombatManager.challengeTarget(player, payload.entityId(), -1);
                        if (failure != null) {
                            StcChat.tell(player, Component.literal("[STC] " + failure)
                                    .withStyle(net.minecraft.ChatFormatting.RED));
                        } else {
                            player.closeContainer();
                        }
                    }
                }));
        // V8：使用药水
        registrar.playToServer(UsePotionPayload.TYPE, UsePotionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        StcChat.tell(player, Component.literal("[STC] "
                                + PotionService.use(player, payload.slot(), payload.target())));
                    }
                }));
        // V11：问号事件的选项（服务端 → 客户端）与选择（客户端 → 服务端）
        registrar.playToClient(CampfireStatePayload.TYPE, CampfireStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientCampfireState.accept(payload)));
        registrar.playToServer(QuitBattlePayload.TYPE, QuitBattlePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        String failure = CombatManager.quit(player, "主动放弃");
                        if (failure != null) {
                            StcChat.tell(player, Component.literal("[STC] " + failure));
                        }
                    }
                }));        registrar.playToClient(QuestionOfferPayload.TYPE, QuestionOfferPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientQuestionState.accept(payload)));
        // v0.5.0：选择之后的结果页文案
        registrar.playToClient(QuestionResultPayload.TYPE, QuestionResultPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientQuestionState.acceptResult(payload)));
        registrar.playToServer(QuestionChoicePayload.TYPE, QuestionChoicePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        QuestionService.send(player,
                                QuestionService.choose(player, payload.eventId(), payload.optionIndex(),
                                        payload.cardIndex()));
                    }
                }));
        // v0.5.1.0 开发者模式：图鉴加卡 / 查看牌组删牌（服务端校验开发者模式是否开启）
        registrar.playToServer(DevDeckPayload.TYPE, DevDeckPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        StcChat.tell(player, Component.literal("[STC] "
                                + com.slaythecraft.dev.DevDeckService.handle(player, payload.action(),
                                        payload.cardId())));
                    }
                }));
        // v0.5.1.2：开发者模式开关（服务端 → 客户端）—— 客户端据此决定要不要显示那两个按钮
        registrar.playToClient(DevStatePayload.TYPE, DevStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientDevState.accept(payload)));
        // v0.5.1.7 A3：卡牌让你选牌（服务端下发候选 → 客户端回传下标）
        registrar.playToClient(CardPickOfferPayload.TYPE, CardPickOfferPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientCardPickState.accept(payload)));
        registrar.playToServer(CardPickChoicePayload.TYPE, CardPickChoicePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        CombatManager.resolveCardPick(player, payload.indices(), payload.auto());
                    }
                }));
    }
}
