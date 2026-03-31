package com.wobble.wobblecombat.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class WobbleCombatTagEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Player attacker;
    private final int durationSeconds;
    private final String cause;

    public WobbleCombatTagEvent(Player player, Player attacker, int durationSeconds, String cause) {
        this.player = player;
        this.attacker = attacker;
        this.durationSeconds = durationSeconds;
        this.cause = cause;
    }

    public Player getPlayer() { return player; }
    public Player getAttacker() { return attacker; }
    public int getDurationSeconds() { return durationSeconds; }
    public String getCause() { return cause; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
