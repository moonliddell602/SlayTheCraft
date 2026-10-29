package com.slaythecraft.combat.core;

import java.util.Set;
import java.util.StringJoiner;

/**
 * 卡牌附魔（v0.0.2 V6）。附魔是**卡牌实例**上的数据（同一张卡的不同副本可以附不同的魔），
 * 因此定义在 {@code CardInstance} 上，战斗时随牌组进入引擎。
 *
 * <ul>
 *   <li>{@link #SWEEPING_EDGE} 横扫之刃：这张牌对其他敌人造成其本身伤害的 35%；</li>
 *   <li>{@link #FIRE_ASPECT} 火焰附加：为被这张牌攻击到的敌人附加 1 层灼烧；</li>
 *   <li>{@link #SMITE} 亡灵杀手：攻击目标为亡灵时伤害 ×1.2。</li>
 * </ul>
 */
public enum CardEnchantment {
    SWEEPING_EDGE("横扫之刃", 0.35D),
    FIRE_ASPECT("火焰附加", 1.0D),
    SMITE("亡灵杀手", 1.2D);

    private final String label;
    private final double value;

    CardEnchantment(String label, double value) {
        this.label = label;
        this.value = value;
    }

    public String label() {
        return label;
    }

    /** 附魔的数值：横扫是比例、亡灵是倍率、火焰附加是层数。 */
    public double value() {
        return value;
    }

    /** 按名字（不区分大小写，也接受中文名）查找，找不到返回 null。 */
    public static CardEnchantment byName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String trimmed = name.trim();
        for (CardEnchantment enchantment : values()) {
            if (enchantment.name().equalsIgnoreCase(trimmed) || enchantment.label.equals(trimmed)) {
                return enchantment;
            }
        }
        return null;
    }

    /** 展示文本（例如 {@code 横扫之刃·亡灵杀手}）。 */
    public static String join(Set<CardEnchantment> enchantments) {
        if (enchantments == null || enchantments.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner("·");
        for (CardEnchantment enchantment : values()) {
            if (enchantments.contains(enchantment)) {
                joiner.add(enchantment.label());
            }
        }
        return joiner.toString();
    }
}
