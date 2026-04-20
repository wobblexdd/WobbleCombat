package com.wobble.wobblecombat;

import com.wobble.wobblecombat.command.WobbleCombatCommand;
import com.wobble.wobblecombat.api.service.SimpleWobbleCombatService;
import com.wobble.wobblecombat.api.service.WobbleCombatService;
import com.wobble.wobblecombat.combat.CombatManager;
import com.wobble.wobblecombat.combat.CrystalOwnershipTracker;
import com.wobble.wobblecombat.config.MessagesConfig;
import com.wobble.wobblecombat.hook.AuraCombatBridgeManager;
import com.wobble.wobblecombat.hook.WobbleCombatPlaceholderExpansion;
import com.wobble.wobblecombat.listener.CombatListener;
import com.wobble.wobblecombat.listener.ConnectionListener;
import com.wobble.wobblecombat.listener.DeathListener;
import com.wobble.wobblecombat.listener.RestrictionListener;
import com.wobble.wobblecombat.listener.StateListener;
import com.wobble.wobblecombat.storage.CombatHistoryStorage;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class WobbleCombatPlugin extends JavaPlugin {

    private CombatManager combatManager;
    private MessagesConfig messages;
    private CombatHistoryStorage historyStorage;
    private WobbleCombatPlaceholderExpansion placeholderExpansion;
    private BukkitTask historyAutosaveTask;
    private WobbleCombatService combatService;
    private AuraCombatBridgeManager auraCombatBridgeManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.messages = new MessagesConfig(this);
        this.messages.load();

        this.historyStorage = new CombatHistoryStorage(this);
        this.historyStorage.reload();

        this.combatManager = new CombatManager(this);
        this.auraCombatBridgeManager = new AuraCombatBridgeManager(this);
        this.combatService = new SimpleWobbleCombatService(combatManager);
        this.combatManager.loadHistory(historyStorage.load());
        this.combatManager.start();
        startHistoryAutosave();

        CrystalOwnershipTracker crystalOwnershipTracker = new CrystalOwnershipTracker(this);
        getServer().getPluginManager().registerEvents(crystalOwnershipTracker, this);
        getServer().getPluginManager().registerEvents(new CombatListener(this, combatManager, crystalOwnershipTracker), this);
        getServer().getPluginManager().registerEvents(new ConnectionListener(this, combatManager, auraCombatBridgeManager), this);
        getServer().getPluginManager().registerEvents(new DeathListener(this, combatManager), this);
        getServer().getPluginManager().registerEvents(new StateListener(combatManager), this);
        getServer().getPluginManager().registerEvents(new RestrictionListener(this, combatManager), this);

        PluginCommand command = Objects.requireNonNull(getCommand("wobblecombat"), "wobblecombat command missing in plugin.yml");
        WobbleCombatCommand executor = new WobbleCombatCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);

        registerPlaceholderExpansion();
        logBypassPermissionSnapshot();
        getLogger().info("WCombatLog enabled.");
    }

    @Override
    public void onDisable() {
        if (historyAutosaveTask != null) {
            historyAutosaveTask.cancel();
            historyAutosaveTask = null;
        }
        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
            placeholderExpansion = null;
        }
        if (combatManager != null) {
            saveHistoryNow();
            combatManager.shutdown();
        }
        if (historyStorage != null) {
            historyStorage.close();
        }
    }

    public void reloadAll() {
        saveHistoryNow();
        reloadConfig();
        messages.load();
        historyStorage.reload();
        combatManager.reload();
        combatManager.loadHistory(historyStorage.load());
        startHistoryAutosave();
        registerPlaceholderExpansion();
        logBypassPermissionSnapshot();
    }

    private void logBypassPermissionSnapshot() {
        List<String> bypassPlayers = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("wobblecombat.bypass")) {
                bypassPlayers.add(player.getName());
            }
        }
        if (!bypassPlayers.isEmpty()) {
            getLogger().warning("Players currently inheriting wobblecombat.bypass: " + String.join(", ", bypassPlayers));
        }
    }

    private void startHistoryAutosave() {
        if (historyAutosaveTask != null) {
            historyAutosaveTask.cancel();
            historyAutosaveTask = null;
        }
        if (!historyStorage.isEnabled()) {
            return;
        }
        long period = Math.max(20L, historyStorage.getAutosaveSeconds() * 20L);
        historyAutosaveTask = Bukkit.getScheduler().runTaskTimer(this, this::saveHistoryNow, period, period);
    }

    public void saveHistoryNow() {
        if (historyStorage != null && historyStorage.isEnabled() && historyStorage.isDirty() && combatManager != null) {
            historyStorage.save(combatManager.snapshotHistory());
        }
    }

    private void registerPlaceholderExpansion() {
        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
            placeholderExpansion = null;
        }

        boolean enabledInConfig = getConfig().getBoolean("settings.register-placeholderapi-expansion", true);
        if (!enabledInConfig) {
            return;
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }

        placeholderExpansion = new WobbleCombatPlaceholderExpansion(this);
        placeholderExpansion.register();
        getLogger().info("Registered PlaceholderAPI expansion.");
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }

    public MessagesConfig getMessages() {
        return messages;
    }

    public CombatHistoryStorage getHistoryStorage() {
        return historyStorage;
    }

    public WobbleCombatService getCombatService() {
        return combatService;
    }

    public AuraCombatBridgeManager getAuraCombatBridgeManager() {
        return auraCombatBridgeManager;
    }
}
