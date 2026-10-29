package com.slaythecraft.combat.core;

/** Tunables for one battle, filled from the mod config by the Minecraft layer. */
public record CombatRules(int energyPerTurn, int handSize, int maxHandSize, int maxEnemies,
                          boolean dotDecaysEachTurn) {

    public static CombatRules defaults() {
        return new CombatRules(3, 5, 10, 4, false);
    }

    /** 大多数调用点只关心前四个参数。 */
    public CombatRules(int energyPerTurn, int handSize, int maxHandSize, int maxEnemies) {
        this(energyPerTurn, handSize, maxHandSize, maxEnemies, false);
    }

    public CombatRules {
        energyPerTurn = Math.max(1, energyPerTurn);
        handSize = Math.max(1, handSize);
        maxHandSize = Math.max(handSize, maxHandSize);
        maxEnemies = Math.max(1, maxEnemies);
    }
}
