package com.wobble.wobblecombat.listener;

import com.wobble.wobblecombat.WobbleCombatPlugin;
import com.wobble.wobblecombat.combat.CombatManager;
import com.wobble.wobblecombat.combat.CrystalOwnershipTracker;
import com.wobble.wobblecombat.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Map;
import java.util.UUID;

public final class CombatListener implements Listener {

    private final WobbleCombatPlugin plugin;
    private final CombatManager combatManager;
    private final CrystalOwnershipTracker crystalOwnershipTracker;

    public CombatListener(WobbleCombatPlugin plugin, CombatManager combatManager, CrystalOwnershipTracker crystalOwnershipTracker) {
        this.plugin = plugin;
        this.combatManager = combatManager;
        this.crystalOwnershipTracker = crystalOwnershipTracker;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (!combatManager.shouldTagForDamage(event.getFinalDamage(), event.getDamage())) {
            return;
        }

        Player attacker = resolveDamagingPlayer(event.getDamager(), 0);
        if (attacker == null) {
            return;
        }
        if (attacker.getUniqueId().equals(victim.getUniqueId()) && combatManager.shouldIgnoreSelfDamageTagging()) {
            return;
        }

        combatManager.tag(attacker, victim);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!combatManager.isTagged(player)) {
            return;
        }
        if (player.hasPermission("wobblecombat.bypass")) {
            return;
        }
        if (!combatManager.isCommandBlocked(event.getMessage())) {
            return;
        }

        event.setCancelled(true);
        String message = plugin.getMessages().getString("chat.blocked-command", "&cYou cannot use that command while in combat. &7({time}s)");
        player.sendMessage(Text.color(combatManager.apply(player, message, Map.of(
                "time", String.valueOf(combatManager.getRemainingSeconds(player)),
                "attacker", combatManager.getLastAttackerName(player),
                "status", "tagged"
        ))));
    }

    private Player resolveDamagingPlayer(Entity damager, int depth) {
        if (damager == null || depth > 3) {
            return null;
        }
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player player) {
                return player;
            }
            if (source instanceof Entity entity) {
                return resolveDamagingPlayer(entity, depth + 1);
            }
        }
        if (damager instanceof TNTPrimed tnt) {
            return resolveDamagingPlayer(tnt.getSource(), depth + 1);
        }
        if (damager instanceof Tameable tameable && tameable.getOwner() instanceof Player owner) {
            return owner;
        }
        if (damager instanceof EnderCrystal crystal) {
            UUID ownerId = crystalOwnershipTracker.getOwner(crystal);
            if (ownerId != null) {
                return Bukkit.getPlayer(ownerId);
            }
        }
        return null;
    }
}
