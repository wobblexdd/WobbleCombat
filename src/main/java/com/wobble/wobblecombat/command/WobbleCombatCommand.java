package com.wobble.wobblecombat.command;

import com.wobble.wobblecombat.WobbleCombatPlugin;
import com.wobble.wobblecombat.combat.CombatManager;
import com.wobble.wobblecombat.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class WobbleCombatCommand implements CommandExecutor, TabCompleter {

    private final WobbleCombatPlugin plugin;

    public WobbleCombatCommand(WobbleCombatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("wobblecombat.reload") && !sender.hasPermission("donutcombat.reload")) {
                sender.sendMessage(Text.color(plugin.getMessages().getString("chat.no-permission", "&cYou do not have permission.")));
                return true;
            }
            plugin.reloadAll();
            sender.sendMessage(Text.color(plugin.getMessages().getString("chat.reload", "&aWobbleCombat reloaded.")));
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("status")) {
            if (!sender.hasPermission("wobblecombat.status")) {
                sender.sendMessage(Text.color(plugin.getMessages().getString("chat.no-permission", "&cYou do not have permission.")));
                return true;
            }
            return handleStatus(sender, args);
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("history")) {
            if (!sender.hasPermission("wobblecombat.status")) {
                sender.sendMessage(Text.color(plugin.getMessages().getString("chat.no-permission", "&cYou do not have permission.")));
                return true;
            }
            return handleHistory(sender, args);
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("debug")) {
            if (!sender.hasPermission("wobblecombat.debug")) {
                sender.sendMessage(Text.color(plugin.getMessages().getString("chat.no-permission", "&cYou do not have permission.")));
                return true;
            }
            return handleDebug(sender, args);
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("clear")) {
            if (!sender.hasPermission("wobblecombat.admin")) {
                sender.sendMessage(Text.color(plugin.getMessages().getString("chat.no-permission", "&cYou do not have permission.")));
                return true;
            }
            return handleClear(sender, args);
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("savehistory")) {
            if (!sender.hasPermission("wobblecombat.admin")) {
                sender.sendMessage(Text.color(plugin.getMessages().getString("chat.no-permission", "&cYou do not have permission.")));
                return true;
            }
            plugin.saveHistoryNow();
            sender.sendMessage(Text.color(plugin.getMessages().getString("chat.save-history", "&aCombat history saved.")));
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("forcetag")) {
            if (!sender.hasPermission("wobblecombat.admin")) {
                sender.sendMessage(Text.color(plugin.getMessages().getString("chat.no-permission", "&cYou do not have permission.")));
                return true;
            }
            return handleForceTag(sender, args);
        }

        sender.sendMessage(Text.color("&7Usage: &f/" + label + " reload &7| &f/" + label + " status [player] &7| &f/" + label + " history [player] &7| &f/" + label + " debug [player] &7| &f/" + label + " clear <player> &7| &f/" + label + " savehistory &7| &f/" + label + " forcetag <attacker> <victim>"));
        return true;
    }


    private boolean handleHistory(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(Text.color(plugin.getMessages().getString("chat.player-not-found", "&cPlayer not found.")));
                return true;
            }
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Text.color("&cConsole must specify a player."));
                return true;
            }
            target = player;
        }
        java.util.List<com.wobble.wobblecombat.combat.CombatHistoryEntry> entries = plugin.getCombatManager().getHistory(target);
        sender.sendMessage(Text.color("&6History for &f" + target.getName() + "&6:"));
        if (entries.isEmpty()) {
            sender.sendMessage(Text.color("&7No combat history."));
            return true;
        }
        int max = Math.min(entries.size(), 8);
        for (int i = 0; i < max; i++) {
            var e = entries.get(i);
            sender.sendMessage(Text.color("&8- &f" + e.type() + " &7attacker: &f" + e.attacker() + " &7details: &f" + e.details()));
        }
        return true;
    }



private boolean handleDebug(CommandSender sender, String[] args) {
    Player target;
    if (args.length >= 2) {
        target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Text.color(plugin.getMessages().getString("chat.player-not-found", "&cPlayer not found.")));
            return true;
        }
    } else {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.color("&cConsole must specify a player."));
            return true;
        }
        target = player;
    }

    CombatManager combatManager = plugin.getCombatManager();
    sender.sendMessage(Text.color(combatManager.apply(target,
            plugin.getMessages().getString("chat.debug-header", "&6Debug for &f{player}&6:"),
            Map.of("player", target.getName(), "attacker", combatManager.getLastAttackerName(target), "time", String.valueOf(combatManager.getRemainingSeconds(target)), "status", combatManager.isTagged(target) ? "tagged" : "safe"))));
    sendDebugLine(sender, target, "tagged", String.valueOf(combatManager.isTagged(target)));
    sendDebugLine(sender, target, "remaining", combatManager.getRemainingSeconds(target) + "s");
    sendDebugLine(sender, target, "attacker", combatManager.getLastAttackerName(target));
    sendDebugLine(sender, target, "world", target.getWorld().getName());
    sendDebugLine(sender, target, "bypass", String.valueOf(target.hasPermission("wobblecombat.bypass")));
    sendDebugLine(sender, target, "history_entries", String.valueOf(combatManager.getHistoryCount(target)));
    sendDebugLine(sender, target, "tracked_players", String.valueOf(combatManager.getTrackedPlayerCount()));
    sendDebugLine(sender, target, "punishment_mode", combatManager.getCombatLogPunishmentMode().name());
    sendDebugLine(sender, target, "history_storage", plugin.getHistoryStorage().getMode().name());
    return true;
}

private void sendDebugLine(CommandSender sender, Player target, String key, String value) {
    sender.sendMessage(Text.color(plugin.getCombatManager().apply(target,
            plugin.getMessages().getString("chat.debug-line", "&8- &7{key}: &f{value}"),
            Map.of("key", key, "value", value, "player", target.getName(), "attacker", plugin.getCombatManager().getLastAttackerName(target), "time", String.valueOf(plugin.getCombatManager().getRemainingSeconds(target)), "status", plugin.getCombatManager().isTagged(target) ? "tagged" : "safe"))));
}

    private boolean handleStatus(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(Text.color(plugin.getMessages().getString("chat.player-not-found", "&cPlayer not found.")));
                return true;
            }
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Text.color("&cConsole must specify a player."));
                return true;
            }
            target = player;
        }

        CombatManager combatManager = plugin.getCombatManager();
        boolean tagged = combatManager.isTagged(target);
        String status = tagged ? "tagged" : "safe";
        boolean selfView = sender instanceof Player player && target.getUniqueId().equals(player.getUniqueId());
        String template = selfView
                ? plugin.getMessages().getString("chat.status-self", "&7Combat: &f{status} &8| &7Time: &f{time}s &8| &7Attacker: &f{attacker}")
                : plugin.getMessages().getString("chat.status-other", "&7{player}: &f{status} &8| &7Time: &f{time}s &8| &7Attacker: &f{attacker}");

        String rendered = combatManager.apply(target, template, Map.of(
                "player", target.getName(),
                "status", status,
                "time", String.valueOf(combatManager.getRemainingSeconds(target)),
                "attacker", combatManager.getLastAttackerName(target)
        ));
        sender.sendMessage(Text.color(rendered));
        return true;
    }

    private boolean handleClear(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Text.color("&cUsage: /wobblecombat clear <player>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Text.color(plugin.getMessages().getString("chat.player-not-found", "&cPlayer not found.")));
            return true;
        }
        plugin.getCombatManager().clear(target);
        sender.sendMessage(Text.color(plugin.getCombatManager().apply(target,
                plugin.getMessages().getString("chat.clear-success", "&aCleared combat tag for &f{player}&a."),
                Map.of("player", target.getName(), "status", "safe", "time", "0", "attacker", plugin.getCombatManager().getLastAttackerName(target))
        )));
        return true;
    }

    private boolean handleForceTag(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Text.color("&cUsage: /wobblecombat forcetag <attacker> <victim>"));
            return true;
        }
        Player attacker = Bukkit.getPlayerExact(args[1]);
        Player victim = Bukkit.getPlayerExact(args[2]);
        if (attacker == null || victim == null) {
            sender.sendMessage(Text.color(plugin.getMessages().getString("chat.player-not-found", "&cPlayer not found.")));
            return true;
        }
        plugin.getCombatManager().tag(attacker, victim);
        sender.sendMessage(Text.color(plugin.getCombatManager().apply(victim,
                plugin.getMessages().getString("chat.forcetag-success", "&aForced combat tag between &f{attacker} &aand &f{player}&a."),
                Map.of("player", victim.getName(), "attacker", attacker.getName(), "status", "tagged", "time", String.valueOf(plugin.getCombatManager().getDurationSeconds()))
        )));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            if ((sender.hasPermission("wobblecombat.reload") || sender.hasPermission("donutcombat.reload")) && "reload".startsWith(prefix)) {
                completions.add("reload");
            }
            if (sender.hasPermission("wobblecombat.status") && "status".startsWith(prefix)) {
                completions.add("status");
            }
            if (sender.hasPermission("wobblecombat.status") && "history".startsWith(prefix)) {
                completions.add("history");
            }
            if (sender.hasPermission("wobblecombat.debug") && "debug".startsWith(prefix)) {
                completions.add("debug");
            }
            if (sender.hasPermission("wobblecombat.admin") && "clear".startsWith(prefix)) {
                completions.add("clear");
            }
            if (sender.hasPermission("wobblecombat.admin") && "savehistory".startsWith(prefix)) {
                completions.add("savehistory");
            }
            if (sender.hasPermission("wobblecombat.admin") && "forcetag".startsWith(prefix)) {
                completions.add("forcetag");
            }
            return completions;
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("status") || args[0].equalsIgnoreCase("history") || args[0].equalsIgnoreCase("debug") || args[0].equalsIgnoreCase("clear") || args[0].equalsIgnoreCase("forcetag"))) {
            return onlinePlayers(args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("forcetag")) {
            return onlinePlayers(args[2]);
        }

        return Collections.emptyList();
    }

    private List<String> onlinePlayers(String prefixText) {
        String prefix = prefixText.toLowerCase();
        List<String> players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().toLowerCase().startsWith(prefix)) {
                players.add(player.getName());
            }
        }
        return players;
    }
}
