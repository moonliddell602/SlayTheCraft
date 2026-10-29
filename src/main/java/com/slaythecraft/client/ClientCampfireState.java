package com.slaythecraft.client;

import com.slaythecraft.network.CampfireStatePayload;

/**
 * 客户端保存"篝火准备状态"（v0.0.3 W5）。
 *
 * <p>服务端在打开界面 / 有人进出 / 有人选择时会推一份 {@link CampfireStatePayload}，
 * 界面用它显示等待名单与"能不能开始"。</p>
 */
public final class ClientCampfireState {

    /** @param waiting 还没进入准备界面的队友名字 @param ready 是否所有人都已就位 @param allies 可包扎的队友 */
    public record Snapshot(java.util.List<String> waiting, boolean ready, java.util.List<String> allies) {
    }

    private static Snapshot snapshot;

    private ClientCampfireState() {
    }

    public static void accept(CampfireStatePayload payload) {
        snapshot = new Snapshot(payload.waiting(), payload.ready(), payload.allies());
    }

    public static Snapshot state() {
        return snapshot;
    }

    public static void clear() {
        snapshot = null;
    }
}
