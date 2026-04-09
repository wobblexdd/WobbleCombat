package com.wobble.wobblecombat.listener;

import com.wobble.wobblecombat.combat.CombatManager;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public final class StateListener implements Listener {

    private final CombatManager combatManager;

    public StateListener(CombatManager combatManager) {
        this.combatManager = combatManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (combatManager.isDisabledWorld(player.getWorld())) {
            combatManager.clear(player, "disabled-world");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (combatManager.isDisabledWorld(event.getTo() == null ? null : event.getTo().getWorld())) {
            combatManager.clear(player, "disabled-world");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRespawn(PlayerRespawnEvent event) {
        combatManager.clear(event.getPlayer(), "respawn");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        GameMode newGameMode = event.getNewGameMode();
        if (newGameMode == GameMode.CREATIVE || newGameMode == GameMode.SPECTATOR) {
            combatManager.clear(event.getPlayer(), "respawn");
        }
    }
}
