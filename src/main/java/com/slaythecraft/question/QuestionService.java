package com.slaythecraft.question;

import com.slaythecraft.chat.StcChat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.slaythecraft.Config;
import com.slaythecraft.binder.BinderResolver;
import com.slaythecraft.combat.core.EventCaps;
import com.slaythecraft.combat.core.EventKind;
import com.slaythecraft.combat.mc.CombatManager;
import com.slaythecraft.data.PlayerCardData;
import com.slaythecraft.data.StcAttachments;
import com.slaythecraft.network.QuestionOfferPayload;
import com.slaythecraft.network.QuestionResultPayload;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 问号事件的服务端逻辑（v0.0.2 V11）。
 *
 * <p>流程：**Shift + 右键**事件生物 → 服务端校验（收纳包 / 本层次数上限）→ 下发
 * {@link QuestionOfferPayload} 给客户端弹选项界面 → 玩家点选项 → 客户端回
 * {@code QuestionChoicePayload} → 服务端**重新按 id 查表**（不信任客户端文案）→ 应用效果 →
 * 记一次问号事件 → 同步档案。</p>
 *
 * <p>次数在"真正选了选项"时才消耗，所以打开界面后按 ESC 不会白白浪费一次机会。</p>
 */
public final class QuestionService {

    /** 界面一次最多显示几个选项。 */
    public static final int MAX_OPTIONS = 3;

    private QuestionService() {
    }

    /**
     * 尝试打开事件界面。
     *
     * @return 给玩家看的拒绝原因；{@code null} 表示已下发界面（或这个生物没有事件）
     */
    public static String open(ServerPlayer player, Entity entity) {
        QuestionEvent event = eventOf(entity);
        if (event == null) {
            return null;
        }
        String reason = blockReason(player);
        if (reason != null) {
            return reason;
        }
        // v0.4.5：同一个问号事件每层只能访问一次
        if (StcAttachments.dataOf(player).hasVisitedQuestion(event.id())) {
            return visitedReason(event, player);
        }
        PacketDistributor.sendToPlayer(player,
                new QuestionOfferPayload(event.id(), event.name(), event.description(), views(event)));
        return null;
    }

    /** 本层已经访问过这个事件时的拒绝文案（带上"换一层再来"的说明）。 */
    private static String visitedReason(QuestionEvent event, ServerPlayer player) {
        return "本层的【" + event.name() + "】你已经访问过了（相同问号事件每层只能访问一次），"
                + "击败本层 BOSS 进入下一层后可以再来。";
    }

    /**
     * 直接用"事件本体"打开界面（v0.4.6：蘑菇岛 / 烟熏蜜蜂这两个事件的触发条件是手持特定物品）。
     *
     * @return 给玩家看的拒绝原因；{@code null} 表示已下发界面
     */
    public static String openDirect(ServerPlayer player, QuestionEvent event) {
        if (event == null) {
            return null;
        }
        String reason = blockReason(player);
        if (reason != null) {
            return reason;
        }
        if (StcAttachments.dataOf(player).hasVisitedQuestion(event.id())) {
            return visitedReason(event, player);
        }
        PacketDistributor.sendToPlayer(player,
                new QuestionOfferPayload(event.id(), event.name(), event.description(), views(event)));
        return null;
    }

    /**
     * 玩家选了一个选项：校验 + 应用 + 计数。
     *
     * <p>选项要求"先选一张牌"时，{@code cardIndex} 必须是合法下标，否则只回一句提示、**不消耗次数**
     * （客户端会先弹选牌页，所以正常情况下不会走到这里）。</p>
     *
     * @param cardIndex 选中的牌下标（不需要选牌时传 -1）
     * @return 给聊天栏看的结果文案（同时会下发 {@link QuestionResultPayload} 给结果页）
     */
    public static String choose(ServerPlayer player, String eventId, int optionIndex, int cardIndex) {
        QuestionEvent event = QuestionLibrary.byId(eventId);
        if (event == null) {
            return "这个事件已经不存在了（整合包改动过？）。";
        }
        QuestionOption option = event.option(optionIndex);
        if (option == null) {
            return "没有这个选项。";
        }
        String reason = blockReason(player);
        if (reason != null) {
            return reason;
        }
        PlayerCardData data = StcAttachments.dataOf(player);
        // v0.4.5：同一个问号事件每层只能访问一次（服务端兜底，客户端文案不可信）
        if (data.hasVisitedQuestion(event.id())) {
            return visitedReason(event, player);
        }
        String outcome;
        if (option.needsCard()) {
            if (cardIndex < 0) {
                return "请先选择一张牌。";
            }
            if (!QuestionCards.matches(option.pick(), data, cardIndex)) {
                return "这张牌不符合这个选项的要求。";
            }
            outcome = option.choose().apply(data, cardIndex);
        } else {
            outcome = option.effect().apply(data, new Random(player.getRandom().nextLong()));
        }
        data.recordEvent(EventKind.QUESTION, Config.eventCaps());
        // v0.4.5：记下"本层已访问过这个事件"（进阶时自动清空）
        data.markQuestionVisited(event.id());
        StcAttachments.syncTo(player);
        // v0.5.0：结果同时进"结果页"（需求："选择选项之后弹出的文案"）
        PacketDistributor.sendToPlayer(player,
                new QuestionResultPayload(event.name(), option.label(), outcome));
        return "【" + event.name() + "】" + option.label() + "：" + outcome;
    }

    /** 不能用问号事件的原因；{@code null} 表示可以。 */
    public static String blockReason(ServerPlayer player) {
        if (CombatManager.inCombat(player)) {
            return "战斗中不能处理问号事件。";
        }
        if (Config.requireBinderToStart() && !BinderResolver.hasBinder(player)) {
            return "需要携带【卡牌收纳包】才能触发问号事件（当前位置："
                    + BinderResolver.binderLocation(player) + "）。";
        }
        PlayerCardData data = StcAttachments.dataOf(player);
        EventCaps caps = Config.eventCaps();
        if (!data.canTrigger(EventKind.QUESTION, caps)) {
            return "本层的问号事件已经用完（" + data.progress().countOf(EventKind.QUESTION) + "/"
                    + caps.capOf(EventKind.QUESTION) + "）。";
        }
        return null;
    }

    /** 这个生物对应哪个事件；没有事件返回 {@code null}。 */
    public static QuestionEvent eventOf(Entity entity) {
        if (entity instanceof Villager villager) {
            ResourceLocation profession = BuiltInRegistries.VILLAGER_PROFESSION
                    .getKey(villager.getVillagerData().getProfession());
            return profession == null ? null : QuestionLibrary.byProfession(profession.toString());
        }
        // v0.4.1：山羊（山羊角会再生吗？）与流浪商人（奇怪的商人）
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (type != null) {
            QuestionEvent byType = QuestionLibrary.byEntityType(type.toString());
            if (byType != null) {
                return byType;
            }
        }
        // 受惊的小猪仔：小体型的猪
        if (entity instanceof Pig pig && pig.isBaby()) {
            return QuestionLibrary.STARTLED_PIGLET;
        }
        return null;
    }

    /** 下发给客户端的选项视图（只带展示用的文本）。 */
    private static List<QuestionOfferPayload.OptionView> views(QuestionEvent event) {
        List<QuestionOfferPayload.OptionView> views = new ArrayList<>();
        for (QuestionOption option : event.options()) {
            if (views.size() >= MAX_OPTIONS) {
                break;
            }
            views.add(new QuestionOfferPayload.OptionView(option.label(), option.description(),
                    option.needsCard(), option.pickHint()));
        }
        return List.copyOf(views);
    }

    /** 结果直接发到聊天栏（与其它交互一致）。 */
    public static void send(ServerPlayer player, String message) {
        StcChat.tell(player, Component.literal("[STC] " + message));
    }
}
