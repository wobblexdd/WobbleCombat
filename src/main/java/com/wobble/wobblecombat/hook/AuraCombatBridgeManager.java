package com.wobble.wobblecombat.hook;

import com.wobble.wobblecombat.WobbleCombatPlugin;
import com.wobble.wobblecombat.api.integration.AuraCombatBridge;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class AuraCombatBridgeManager {

    private final WobbleCombatPlugin plugin;

    public AuraCombatBridgeManager(WobbleCombatPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean handleCombatLogout(Player player, UUID attackerUuid, String reason) {
        AuraCombatBridge bridge = Bukkit.getServicesManager().load(AuraCombatBridge.class);
        if (bridge == null) {
            return false;
        }

        try {
            return bridge.handleCombatLogout(player, attackerUuid, reason == null ? "" : reason);
        } catch (Exception exception) {
            plugin.getLogger().warning("Aura combat bridge failed during logout delegation: " + exception.getMessage());
            return false;
        }
    }
}
