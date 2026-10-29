package com.slaythecraft.client;

/**
 * "悬停 0.6 秒才显示说明"的小工具（v0.4.6 需求）。
 *
 * <p>需求原文："将鼠标悬停在状态，充能球上持续 0.6 秒，可以查看状态/充能球功能描述。"</p>
 *
 * <p>用法：每帧把"当前鼠标下的对象 key"喂给 {@link #update(String)}，
 * 同一个 key 连续停留够 {@link #HOVER_MS} 之后 {@link #ready()} 才会变成 true。</p>
 */
public final class HoverTracker {

    /** 需要停留多久（毫秒）。 */
    public static final long HOVER_MS = 600L;

    private static String current = "";
    private static long since;

    private HoverTracker() {
    }

    /**
     * 更新当前悬停对象。
     *
     * @param key 悬停对象标识（{@code null} / 空串 = 没有悬停任何东西）
     * @return 是否已经满足"持续 0.6 秒"
     */
    public static boolean update(String key) {
        String next = key == null ? "" : key;
        if (!next.equals(current)) {
            current = next;
            since = System.currentTimeMillis();
            return false;
        }
        return !current.isEmpty() && System.currentTimeMillis() - since >= HOVER_MS;
    }

    /** 当前是否已经满足悬停时长（不改变状态）。 */
    public static boolean ready() {
        return !current.isEmpty() && System.currentTimeMillis() - since >= HOVER_MS;
    }

    /** 当前悬停的对象（空串 = 没有）。 */
    public static String current() {
        return current;
    }

    /** 清空（离开界面时调用，避免换界面后"继承"悬停计时）。 */
    public static void clear() {
        current = "";
        since = 0L;
    }
}
