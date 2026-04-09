package com.wobble.wobblecombat.listener;

import com.wobble.wobblecombat.WobbleCombatPlugin;
import com.wobble.wobblecombat.api.event.WobbleCombatLogEvent;
import com.wobble.wobblecombat.combat.CombatManager;
import com.wobble.wobblecombat.hook.AuraCombatBridgeManager;
import com.wobble.wobblecombat.util.Text;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;

public final class ConnectionListener implements Listener {

    private final WobbleCombatPlugin plugin;
    private final CombatManager combatManager;
    private final AuraCombatBridgeManager auraCombatBridgeManager;

    public ConnectionListener(WobbleCombatPlugin plugin, CombatManager combatManager, AuraCombatBridgeManager auraCombatBridgeManager) {
        this.plugin = plugin;
        this.combatManager = combatManager;
        this.auraCombatBridgeManager = auraCombatBridgeManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        handleLeave(event.getPlayer(), true, "");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onKick(PlayerKickEvent event) {
        String reason = event.reason() == null ? "" : PlainTextComponentSerializer.plainText().serialize(event.reason());
        handleLeave(event.getPlayer(), combatManager.shouldPunishOnKick(reason), reason);
    }

    private void handleLeave(Player player, boolean punish, String reason) {
        if (!combatManager.isTagged(player)) {
            combatManager.clear(player);
            return;
        }
        if (!punish || !combatManager.shouldPunishCombatLog()) {
            combatManager.clear(player);
            return;
        }

        Map<String, String> replacements = Map.of(
                "player", player.getName(),
                "reason", reason == null ? "" : reason,
                "attacker", combatManager.getLastAttackerName(player),
                "time", String.valueOf(combatManager.getRemainingSeconds(player)),
                "status", "tagged"
        );

        String broadcast = plugin.getMessages().getString("chat.quit-broadcast", "");
        if (combatManager.shouldBroadcastCombatLog() && !broadcast.isBlank()) {
            Bukkit.broadcast(Text.component(combatManager.apply(player, broadcast, replacements)));
        }

        combatManager.notifyCombatLog(player, reason);
        combatManager.addHistory(player, null, "COMBAT_LOG", reason == null || reason.isBlank() ? "quit" : reason);
        boolean delegated = auraCombatBridgeManager.handleCombatLogout(player, combatManager.getLastAttacker(player), reason);
        Bukkit.getPluginManager().callEvent(new WobbleCombatLogEvent(
                player,
                combatManager.getLastAttackerName(player),
                reason,
                delegated ? CombatManager.CombatLogPunishmentMode.NONE : combatManager.getCombatLogPunishmentMode()
        ));
        if (!delegated) {
            combatManager.executeCombatLogPunishment(player);
        }
        combatManager.clear(player);
    }
}
