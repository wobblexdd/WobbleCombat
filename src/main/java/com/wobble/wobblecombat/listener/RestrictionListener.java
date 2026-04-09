package com.wobble.wobblecombat.listener;

import com.wobble.wobblecombat.WobbleCombatPlugin;
import com.wobble.wobblecombat.combat.CombatManager;
import com.wobble.wobblecombat.util.Text;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Map;

public final class RestrictionListener implements Listener {

    private final WobbleCombatPlugin plugin;
    private final CombatManager combatManager;

    public RestrictionListener(WobbleCombatPlugin plugin, CombatManager combatManager) {
        this.plugin = plugin;
        this.combatManager = combatManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (!combatManager.isTagged(player) || player.hasPermission("wobblecombat.bypass")) {
            return;
        }

        PlayerTeleportEvent.TeleportCause cause = event.getCause();
        if (cause == PlayerTeleportEvent.TeleportCause.ENDER_PEARL && combatManager.isEnderPearlBlocked()) {
            cancel(player, event, "chat.blocked-ender-pearl");
            return;
        }
        if (cause == PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT && combatManager.isChorusFruitBlocked()) {
            cancel(player, event, "chat.blocked-chorus-fruit");
            return;
        }
        if ((cause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL || cause == PlayerTeleportEvent.TeleportCause.END_PORTAL)
                && combatManager.isPortalBlocked()) {
            cancel(player, event, "chat.blocked-portal");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGlide(EntityToggleGlideEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }
        if (!event.isGliding()) {
            return;
        }
        if (!combatManager.isTagged(player) || player.hasPermission("wobblecombat.bypass")) {
            return;
        }
        if (!combatManager.isElytraBlocked()) {
            return;
        }

        event.setCancelled(true);
        send(player, "chat.blocked-elytra");
    }

    private void cancel(Player player, PlayerTeleportEvent event, String path) {
        event.setCancelled(true);
        send(player, path);
    }

    private void send(Player player, String path) {
        String message = plugin.getMessages().getString(path, "&cThat action is blocked while in combat. &7({time}s)");
        player.sendMessage(Text.color(combatManager.apply(player, message, Map.of(
                "time", String.valueOf(combatManager.getRemainingSeconds(player)),
                "attacker", combatManager.getLastAttackerName(player),
                "status", "tagged"
        ))));
    }
}
