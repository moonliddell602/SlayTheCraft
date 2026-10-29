package com.slaythecraft.combat.core;

/**
 * The rules text of a card. {@code target} is the chosen enemy for {@link CardTarget#ENEMY}, the
 * player for {@link CardTarget#SELF}, and {@code null} for {@link CardTarget#ALL_ENEMIES} /
 * {@link CardTarget#NONE} (use {@link CombatContext#aliveEnemies()} there).
 */
@FunctionalInterface
public interface CardEffect {
    void apply(CombatContext context, Combatant self, Combatant target);
}
