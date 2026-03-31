package com.wobble.wobblecombat.config;

import com.wobble.wobblecombat.WobbleCombatPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

public final class MessagesConfig {

    private final WobbleCombatPlugin plugin;
    private final File file;
    private FileConfiguration configuration;

    public MessagesConfig(WobbleCombatPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "messages.yml");
    }

    public void load() {
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
            migrateLegacyMessages(plugin.getConfig());
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        boolean changed = validateAndFillDefaults(yaml);
        if (changed) {
            try {
                yaml.save(file);
            } catch (IOException exception) {
                plugin.getLogger().log(Level.WARNING, "Could not save validated messages.yml", exception);
            }
        }
        this.configuration = yaml;
    }

    private boolean validateAndFillDefaults(FileConfiguration yaml) {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("action-bar.combat", "&7Combat: &5{time}");
        defaults.put("action-bar.end", "");
        defaults.put("chat.blocked-command", "&cYou cannot use that command while in combat. &7({time}s)");
        defaults.put("chat.tagged", "&cYou are now in combat for &f{time}&c seconds.");
        defaults.put("chat.untagged", "&aYou are no longer in combat.");
        defaults.put("chat.quit-broadcast", "&c{player} logged out during combat and died.");
        defaults.put("chat.reload", "&aWobbleCombat reloaded.");
        defaults.put("chat.no-permission", "&cYou do not have permission.");
        defaults.put("chat.player-not-found", "&cPlayer not found.");
        defaults.put("chat.status-self", "&7Combat: &f{status} &8| &7Time: &f{time}s &8| &7Attacker: &f{attacker}");
        defaults.put("chat.status-other", "&7{player}: &f{status} &8| &7Time: &f{time}s &8| &7Attacker: &f{attacker}");
        defaults.put("chat.clear-success", "&aCleared combat tag for &f{player}&a.");
        defaults.put("chat.forcetag-success", "&aForced combat tag between &f{attacker} &aand &f{player}&a.");
        defaults.put("chat.blocked-ender-pearl", "&cYou cannot use ender pearls while in combat. &7({time}s)");
        defaults.put("chat.blocked-chorus-fruit", "&cYou cannot use chorus fruit while in combat. &7({time}s)");
        defaults.put("chat.blocked-portal", "&cYou cannot use portals while in combat. &7({time}s)");
        defaults.put("chat.blocked-elytra", "&cYou cannot start gliding while in combat. &7({time}s)");
        defaults.put("death.prefix", "&c☠ ");
        defaults.put("death.player", "{prefix}{victim} was eliminated by {attacker}");
        defaults.put("death.combat", "{prefix}{victim} died in combat.");
        defaults.put("bossbar.enabled", false);
        defaults.put("bossbar.text", "&cCombat: &f{time}s");
        defaults.put("bossbar.color", "RED");
        defaults.put("bossbar.overlay", "PROGRESS");
        defaults.put("titles.tagged.enabled", false);
        defaults.put("titles.tagged.title", "&cCOMBAT");
        defaults.put("titles.tagged.subtitle", "&7Do not logout for &f{time}&7s");
        defaults.put("titles.tagged.fade-in", 5);
        defaults.put("titles.tagged.stay", 30);
        defaults.put("titles.tagged.fade-out", 10);
        defaults.put("titles.untagged.enabled", false);
        defaults.put("titles.untagged.title", "&aSAFE");
        defaults.put("titles.untagged.subtitle", "&7You are no longer in combat");
        defaults.put("titles.untagged.fade-in", 5);
        defaults.put("titles.untagged.stay", 20);
        defaults.put("titles.untagged.fade-out", 10);
        defaults.put("sounds.tagged.enabled", false);
        defaults.put("sounds.tagged.name", "ENTITY_EXPERIENCE_ORB_PICKUP");
        defaults.put("sounds.tagged.volume", 1.0);
        defaults.put("sounds.tagged.pitch", 0.8);
        defaults.put("sounds.untagged.enabled", false);
        defaults.put("sounds.untagged.name", "ENTITY_EXPERIENCE_ORB_PICKUP");
        defaults.put("sounds.untagged.volume", 1.0);
        defaults.put("sounds.untagged.pitch", 1.2);
        defaults.put("sounds.combat-log.enabled", false);
        defaults.put("sounds.combat-log.name", "ENTITY_WITHER_DEATH");
        defaults.put("sounds.combat-log.volume", 1.0);
        defaults.put("sounds.combat-log.pitch", 1.0);

        boolean changed = false;
        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            if (!yaml.contains(entry.getKey())) {
                yaml.set(entry.getKey(), entry.getValue());
                plugin.getLogger().warning("messages.yml missing key: " + entry.getKey() + " - default inserted.");
                changed = true;
            }
        }
        return changed;
    }

    private void migrateLegacyMessages(FileConfiguration legacyConfig) {
        ConfigurationSection section = legacyConfig.getConfigurationSection("messages");
        if (section == null) {
            return;
        }

        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        copyIfPresent(section, yaml, "action-bar-combat", "action-bar.combat");
        copyIfPresent(section, yaml, "action-bar-end", "action-bar.end");
        copyIfPresent(section, yaml, "blocked-command", "chat.blocked-command");
        copyIfPresent(section, yaml, "tagged", "chat.tagged");
        copyIfPresent(section, yaml, "untagged", "chat.untagged");
        copyIfPresent(section, yaml, "quit-broadcast", "chat.quit-broadcast");
        copyIfPresent(section, yaml, "reload", "chat.reload");
        copyIfPresent(section, yaml, "no-permission", "chat.no-permission");
        copyIfPresent(section, yaml, "death-prefix", "death.prefix");
        copyIfPresent(section, yaml, "death-message-player", "death.player");
        copyIfPresent(section, yaml, "death-message-combat", "death.combat");

        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not migrate legacy messages to messages.yml", exception);
        }
    }

    private void copyIfPresent(ConfigurationSection source, FileConfiguration target, String oldPath, String newPath) {
        if (source.contains(oldPath)) {
            target.set(newPath, source.get(oldPath));
        }
    }

    public String getString(String path, String fallback) {
        return configuration == null ? fallback : configuration.getString(path, fallback);
    }

    public boolean getBoolean(String path, boolean fallback) {
        return configuration == null ? fallback : configuration.getBoolean(path, fallback);
    }

    public int getInt(String path, int fallback) {
        return configuration == null ? fallback : configuration.getInt(path, fallback);
    }

    public float getFloat(String path, float fallback) {
        return configuration == null ? fallback : (float) configuration.getDouble(path, fallback);
    }
}
