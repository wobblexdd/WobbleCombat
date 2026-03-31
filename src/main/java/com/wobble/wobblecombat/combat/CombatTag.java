package com.wobble.wobblecombat.combat;

import java.util.UUID;

public record CombatTag(long expiresAtMillis, UUID lastAttacker) {
}
