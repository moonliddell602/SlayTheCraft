package com.slaythecraft.combat.core;

import java.util.Set;
import java.util.StringJoiner;

/**
 * 卡牌关键字（v0.0.2 V4）。行为定义：
 *
 * <ul>
 *   <li>{@link #RETAIN}：回合结束时**留在手牌**，不进入弃牌堆；</li>
 *   <li>{@link #EXHAUST}：打出后从本场战斗移除，进入**消耗牌堆**；</li>
 *   <li>{@link #ETHEREAL}：回合结束时若仍在手牌，则被**消耗**（进消耗牌堆）；</li>
 *   <li>{@link #INNATE}：第一回合必定入手且**最先抽到**，且不占固定抽牌数（仍受手牌上限限制）。</li>
 * </ul>
 */
public enum CardKeyword {
    RETAIN("保留"),
    EXHAUST("消耗"),
    ETHEREAL("虚无"),
    INNATE("固有");

    private final String label;

    CardKeyword(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** 把一组关键字拼成展示用文本（例如 {@code 保留·消耗}）。 */
    public static String join(Set<CardKeyword> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner("·");
        for (CardKeyword keyword : values()) {
            if (keywords.contains(keyword)) {
                joiner.add(keyword.label());
            }
        }
        return joiner.toString();
    }
}
