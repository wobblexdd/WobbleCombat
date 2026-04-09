package com.wobble.wobblecombat.api.event;

import com.wobble.wobblecombat.combat.CombatManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class WobbleCombatLogEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String attackerName;
    private final String reason;
    private final CombatManager.CombatLogPunishmentMode punishmentMode;

    public WobbleCombatLogEvent(Player player, String attackerName, String reason, CombatManager.CombatLogPunishmentMode punishmentMode) {
        this.player = player;
        this.attackerName = attackerName;
        this.reason = reason;
        this.punishmentMode = punishmentMode;
    }

    public Player getPlayer() { return player; }
    public String getAttackerName() { return attackerName; }
    public String getReason() { return reason; }
    public CombatManager.CombatLogPunishmentMode getPunishmentMode() { return punishmentMode; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
