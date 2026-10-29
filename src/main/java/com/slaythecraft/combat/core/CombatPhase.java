package com.slaythecraft.combat.core;

/** Which side of the turn structure a battle is currently in. */
public enum CombatPhase {
    PLAYER_TURN,
    ENEMY_TURN,
    VICTORY,
    DEFEAT;

    public boolean isOver() {
        return this == VICTORY || this == DEFEAT;
    }
}
