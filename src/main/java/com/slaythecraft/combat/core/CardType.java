package com.slaythecraft.combat.core;

/** Card classification, mirroring Slay the Spire's card types. */
public enum CardType {
    ATTACK("攻击"),
    SKILL("技能"),
    POWER("能力"),
    STATUS("状态"),
    CURSE("诅咒");

    private final String label;

    CardType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
