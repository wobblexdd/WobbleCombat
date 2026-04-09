package com.wobble.wobblecombat.hook;

import com.wobble.wobblecombat.WobbleCombatPlugin;
import com.wobble.wobblecombat.combat.CombatManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class WobbleCombatPlaceholderExpansion extends PlaceholderExpansion {

    private final WobbleCombatPlugin plugin;

    public WobbleCombatPlaceholderExpansion(WobbleCombatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "wobblecombat";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (!(offlinePlayer instanceof Player player)) {
            return "";
        }

        CombatManager combatManager = plugin.getCombatManager();
        return switch (params.toLowerCase()) {
            case "player" -> player.getName();
            case "world" -> player.getWorld().getName();
            case "time" -> String.valueOf(combatManager.getRemainingSeconds(player));
            case "attacker" -> combatManager.getLastAttackerName(player);
            case "status" -> combatManager.isTagged(player) ? "tagged" : "safe";
            default -> null;
        };
    }
}
