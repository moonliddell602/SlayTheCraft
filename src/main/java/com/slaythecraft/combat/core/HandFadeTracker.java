package com.slaythecraft.combat.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 手牌"哪几张是刚抽到的"（v0.3.0）。
 *
 * <p>客户端每收到一份战斗快照就要决定"这张牌要不要播淡入"。判据是"上一帧没有、这一帧有"，
 * 而且要按 **id 的多重集**匹配：手里本来就有 3 张打击，新抽到第 4 张时，多出来的那一张才是新的。</p>
 *
 * <p>纯逻辑（只吃 id 字符串），所以可以直接进裸 JVM 自检。</p>
 */
public final class HandFadeTracker {

    private HandFadeTracker() {
    }

    /**
     * 返回 {@code current} 里"相对 {@code previous} 新出现"的下标（升序）。
     *
     * <p>匹配顺序与手牌顺序一致：同样的 id 先用掉旧的那一张，多出来的就是新抽到的。</p>
     */
    public static List<Integer> newIndices(List<String> previous, List<String> current) {
        if (current == null || current.isEmpty()) {
            return List.of();
        }
        Map<String, Integer> remaining = new HashMap<>();
        if (previous != null) {
            for (String id : previous) {
                remaining.merge(id, 1, Integer::sum);
            }
        }
        List<Integer> fresh = new ArrayList<>();
        for (int i = 0; i < current.size(); i++) {
            String id = current.get(i);
            Integer left = remaining.get(id);
            if (left != null && left > 0) {
                remaining.put(id, left - 1);
            } else {
                fresh.add(i);
            }
        }
        return List.copyOf(fresh);
    }

    /** 手牌换了回合（整手都算新的）时用它：任何一张都要淡入。 */
    public static List<Integer> allIndices(int handSize) {
        List<Integer> all = new ArrayList<>(Math.max(0, handSize));
        for (int i = 0; i < handSize; i++) {
            all.add(i);
        }
        return List.copyOf(all);
    }

    /** 简易的"id + 强化标记"键，供调用方自己拼列表。 */
    public static String key(String id, boolean upgraded) {
        return upgraded ? id + "+" : id;
    }
}
