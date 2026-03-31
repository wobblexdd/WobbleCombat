package com.wobble.wobblecombat.combat;

public record CombatHistoryEntry(long timestampMillis, String player, String attacker, String type, String details) {
}
