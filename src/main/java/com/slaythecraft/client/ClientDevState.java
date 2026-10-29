package com.slaythecraft.client;

import com.slaythecraft.network.DevStatePayload;

/**
 * 客户端这边记住的**开发者模式开关**（v0.5.1.2）。
 *
 * <p>只用于决定"要不要显示 加入牌组 / 删除 按钮"：服务端每次收到牌组编辑请求都会重新校验，
 * 所以这里就算记错了也改不动牌组。</p>
 */
public final class ClientDevState {

    private static volatile boolean enabled;

    private ClientDevState() {
    }

    /** 收到服务端同步的开关状态。 */
    public static void accept(DevStatePayload payload) {
        if (payload != null) {
            enabled = payload.enabled();
        }
    }

    /** 开发者模式是否已开启（默认关）。 */
    public static boolean enabled() {
        return enabled;
    }

    /** 退出游戏 / 切换服务器时清掉（避免在主菜单里还记着上一个服务器的状态）。 */
    public static void clear() {
        enabled = false;
    }
}
