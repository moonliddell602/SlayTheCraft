package com.slaythecraft.combat.core;

import java.util.List;

/** Sink for battle narration; the Minecraft layer forwards it to the player's chat. */
@FunctionalInterface
public interface CombatLog {
    void log(String message);

    static CombatLog noop() {
        return message -> {
        };
    }

    static CombatLog toList(List<String> sink) {
        return sink::add;
    }
}
