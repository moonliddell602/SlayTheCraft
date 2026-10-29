package com.slaythecraft.client;

import com.slaythecraft.network.CardPickOfferPayload;

/**
 * 客户端这边记住的**待选牌请求**（v0.5.1.7 A3）。
 *
 * <p>{@code StcClientSetup} 每 tick 检查一次：有请求且当前没在显示选牌页就弹出来。</p>
 */
public final class ClientCardPickState {

    private static CardPickOfferPayload offer;

    private ClientCardPickState() {
    }

    /**
     * 收到服务端的选牌请求。
     *
     * <p>服务端在"已经处理完了"时会发一条 {@link CardPickOfferPayload#active()} 为假的空请求，
     * 客户端据此把界面收掉 —— 否则超时兜底/别处结算之后选牌页会一直挂在那里。</p>
     */
    public static void accept(CardPickOfferPayload payload) {
        offer = payload == null || !payload.active() ? null : payload;
    }

    /** 当前待选牌请求；没有则返回 {@code null}。 */
    public static CardPickOfferPayload offer() {
        return offer;
    }

    /** 选完 / 放弃之后清掉（否则会一直弹）。 */
    public static void clear() {
        offer = null;
    }
}
