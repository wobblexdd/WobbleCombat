package com.wobble.wobblecombat.combat;

import com.wobble.wobblecombat.WobbleCombatPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CrystalOwnershipTracker implements Listener {

    private final WobbleCombatPlugin plugin;
    private final Map<UUID, UUID> owners = new ConcurrentHashMap<>();

    public CrystalOwnershipTracker(WobbleCombatPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCrystalPlace(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        if (event.getMaterial() != Material.END_CRYSTAL) {
            return;
        }

        Player player = event.getPlayer();
        Location base = event.getClickedBlock().getLocation().add(0.5, 1.0, 0.5);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Entity nearest = null;
            double bestDistance = Double.MAX_VALUE;
            for (Entity entity : base.getWorld().getNearbyEntities(base, 2.0, 2.0, 2.0)) {
                if (!(entity instanceof EnderCrystal)) {
                    continue;
                }
                double distance = entity.getLocation().distanceSquared(base);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    nearest = entity;
                }
            }
            if (nearest instanceof EnderCrystal crystal) {
                owners.put(crystal.getUniqueId(), player.getUniqueId());
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCrystalDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof EnderCrystal crystal) {
            owners.remove(crystal.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        owners.remove(event.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        for (Entity entity : event.getEntities()) {
            owners.remove(entity.getUniqueId());
        }
    }

    public UUID getOwner(EnderCrystal crystal) {
        return owners.get(crystal.getUniqueId());
    }
}
