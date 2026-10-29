package com.slaythecraft.combat.core;

/**
 * 药水的正负性质（v0.0.2 V8），决定它能对谁使用：
 *
 * <ul>
 *   <li>{@link #BENEFICIAL} 正面：只能对自己 / 队友使用；</li>
 *   <li>{@link #HARMFUL} 负面：只能对敌人使用。</li>
 * </ul>
 */
public enum PotionKind {
    BENEFICIAL("正面"),
    HARMFUL("负面");

    private final String label;

    PotionKind(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** 能否用在"自己人"身上。 */
    public boolean canTargetAlly() {
        return this == BENEFICIAL;
    }

    /** 能否用在敌人身上。 */
    public boolean canTargetEnemy() {
        return this == HARMFUL;
    }
}
