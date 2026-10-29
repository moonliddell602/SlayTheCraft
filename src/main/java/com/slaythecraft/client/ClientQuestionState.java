package com.slaythecraft.client;

import java.util.List;

import com.slaythecraft.network.QuestionOfferPayload;
import com.slaythecraft.network.QuestionResultPayload;

/**
 * 客户端当前待处理的问号事件（v0.0.2 V11；v0.5.0 增加"选牌中"与"结果页"）。
 *
 * <p>服务端下发的东西先存这里，再由 {@code StcClientSetup} 的客户端 tick 打开对应界面
 * （与战斗界面的"自动弹出"同一套做法，避免在包处理体里直接操作界面）。</p>
 */
public final class ClientQuestionState {

    /** "正在为某个选项选一张牌"。 */
    public record PendingPick(String eventId, int optionIndex, String eventName, String optionLabel,
                              boolean upgrade) {
    }

    private static QuestionOfferPayload offer;
    /** 上一次收到的事件（选牌页按 ESC 反悔时要回到它）。 */
    private static QuestionOfferPayload lastOffer;
    /** 待显示的选牌任务（v0.5.0）。 */
    private static PendingPick pendingPick;
    /** 待显示的结果页文本（v0.5.0）。 */
    private static String resultText;
    private static String resultTitle;

    private ClientQuestionState() {
    }

    public static void accept(QuestionOfferPayload payload) {
        offer = payload;
        lastOffer = payload;
        pendingPick = null;
    }

    public static void acceptResult(QuestionResultPayload payload) {
        resultTitle = payload.eventName() + " · " + payload.optionLabel();
        resultText = payload.text();
    }

    /** 当前待显示的事件；没有则返回 {@code null}。 */
    public static QuestionOfferPayload offer() {
        return offer;
    }

    public static List<QuestionOfferPayload.OptionView> options() {
        return offer == null ? List.of() : offer.options();
    }

    /** 开始为某个选项选牌（点了一个 needsCard 的选项）；选项页先留着，ESC 反悔时能回去。 */
    public static void beginPick(String eventId, int optionIndex, String eventName, String optionLabel,
                                 boolean upgrade) {
        pendingPick = new PendingPick(eventId, optionIndex, eventName, optionLabel, upgrade);
    }

    public static PendingPick pendingPick() {
        return pendingPick;
    }

    public static void clearPick() {
        pendingPick = null;
    }

    /** 选牌页按 ESC：回到上一次的选项页（不消耗次数）。 */
    public static void requestReopen() {
        offer = lastOffer;
    }

    public static String resultText() {
        return resultText;
    }

    public static String resultTitle() {
        return resultTitle;
    }

    public static void clearResult() {
        resultText = null;
        resultTitle = null;
    }

    /** 界面关闭 / 选完之后清掉，避免反复弹出。 */
    public static void clear() {
        offer = null;
        lastOffer = null;
        pendingPick = null;
    }

    // ------------------------------------------------------------------ v0.4.5：不能直接离开

    /** ESC 被拒绝时的提示截止时间（到点就消失）。 */
    private static long refusalHintUntil;

    /** 玩家试图直接离开时给一句提示（需求：问号事件不再能直接离开）。 */
    public static void hintMustChoose() {
        refusalHintUntil = System.currentTimeMillis() + 2500L;
    }

    /** 现在是否要显示"必须先选一个选项"的提示。 */
    public static boolean refusing() {
        return System.currentTimeMillis() <= refusalHintUntil;
    }
}
