package com.wobble.wobblecombat.listener;

import com.wobble.wobblecombat.WobbleCombatPlugin;
import com.wobble.wobblecombat.combat.CombatManager;
import com.wobble.wobblecombat.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Map;
import java.util.UUID;

public final class DeathListener implements Listener {

    private final WobbleCombatPlugin plugin;
    private final CombatManager combatManager;

    public DeathListener(WobbleCombatPlugin plugin, CombatManager combatManager) {
        this.plugin = plugin;
        this.combatManager = combatManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        UUID attackerId = combatManager.getLastAttacker(player);
        if (attackerId != null) {
            Player attacker = Bukkit.getPlayer(attackerId);
            String prefix = plugin.getMessages().getString("death.prefix", "");
            String template;
            if (attacker != null && attacker.isOnline()) {
                template = plugin.getMessages().getString("death.player", "{prefix}{victim} was eliminated by {attacker}");
            } else {
                template = plugin.getMessages().getString("death.combat", "{prefix}{victim} died in combat.");
            }

            String finalMessage = combatManager.apply(player, template, Map.of(
                    "prefix", prefix,
                    "victim", player.getName(),
                    "attacker", attacker == null ? "Unknown" : attacker.getName()
            ));
            event.deathMessage(Text.component(finalMessage));
        }

        combatManager.addHistory(player, attackerId == null ? null : Bukkit.getPlayer(attackerId), "DEATH", "death-event");

        if (combatManager.shouldClearTagOnDeath()) {
            combatManager.clear(player, "death");
        }
    }
}
