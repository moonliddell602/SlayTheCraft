package com.slaythecraft.client;

import com.slaythecraft.network.DevDeckPayload;

import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 客户端的**开发者模式牌组编辑**入口（v0.5.1.2）。
 *
 * <p>需求（开发者模式）："可以在图鉴里直接向卡组添加卡牌。可以直接在查看卡组页面删牌。"</p>
 *
 * <p>这里只负责把请求发上去：{@link ClientDevState} 决定按钮显不显示，服务端
 * {@code DevDeckService} 决定这次请求算不算数（会重新查一次开发者模式开关）。
 * 也就是说**改档案的权限始终在服务端**，客户端被改坏也只是多发几个会被拒绝的包。</p>
 */
public final class ClientDevActions {

    private ClientDevActions() {
    }

    /** 请求把一张卡加入牌组。 */
    public static void addCard(String cardId) {
        send(DevDeckPayload.ACTION_ADD, cardId);
    }

    /** 请求从牌组里移除一张该 id 的卡。 */
    public static void removeCard(String cardId) {
        send(DevDeckPayload.ACTION_REMOVE, cardId);
    }

    private static void send(String action, String cardId) {
        if (cardId == null || cardId.isBlank()) {
            return;
        }
        PacketDistributor.sendToServer(new DevDeckPayload(action, cardId));
    }
}
