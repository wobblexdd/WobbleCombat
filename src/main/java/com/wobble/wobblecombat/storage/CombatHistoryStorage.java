package com.wobble.wobblecombat.storage;

import com.wobble.wobblecombat.WobbleCombatPlugin;
import com.wobble.wobblecombat.combat.CombatHistoryEntry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class CombatHistoryStorage {

    public enum StorageMode {
        YAML,
        SQLITE
    }

    private final WobbleCombatPlugin plugin;
    private File yamlFile;
    private File sqliteFile;
    private boolean enabled;
    private int autosaveSeconds;
    private boolean dirty;
    private StorageMode mode;

    public CombatHistoryStorage(WobbleCombatPlugin plugin) {
        this.plugin = plugin;
        this.mode = StorageMode.YAML;
    }

    public void reload() {
        this.enabled = plugin.getConfig().getBoolean("settings.history-storage.enabled", true);
        this.autosaveSeconds = Math.max(5, plugin.getConfig().getInt("settings.history-storage.autosave-seconds", 30));

        String rawMode = plugin.getConfig().getString("settings.history-storage.type", "YAML");
        try {
            this.mode = StorageMode.valueOf(rawMode == null ? "YAML" : rawMode.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            this.mode = StorageMode.YAML;
        }

        String yamlFileName = plugin.getConfig().getString("settings.history-storage.file", "histories.yml");
        this.yamlFile = new File(plugin.getDataFolder(), yamlFileName == null || yamlFileName.isBlank() ? "histories.yml" : yamlFileName);

        String sqliteFileName = plugin.getConfig().getString("settings.history-storage.sqlite-file", "histories.db");
        this.sqliteFile = new File(plugin.getDataFolder(), sqliteFileName == null || sqliteFileName.isBlank() ? "histories.db" : sqliteFileName);

        this.dirty = false;

        if (enabled && mode == StorageMode.SQLITE) {
            try {
                initializeSqlite();
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to initialize SQLite history storage: " + exception.getMessage());
                this.mode = StorageMode.YAML;
            }
        }
    }

    public Map<UUID, Deque<CombatHistoryEntry>> load() {
        if (!enabled) {
            return new HashMap<>();
        }
        if (mode == StorageMode.SQLITE) {
            try {
                return loadFromSqlite();
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to load SQLite combat history: " + exception.getMessage());
                return new HashMap<>();
            }
        }
        return loadFromYaml();
    }

    public void save(Map<UUID, Deque<CombatHistoryEntry>> historyMap) {
        if (!enabled) {
            return;
        }

        try {
            if (mode == StorageMode.SQLITE) {
                saveToSqlite(historyMap);
            } else {
                saveToYaml(historyMap);
            }
            dirty = false;
        } catch (Exception exception) {
            plugin.getLogger().severe("Failed to save combat history: " + exception.getMessage());
        }
    }

    public void markDirty() {
        this.dirty = true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getAutosaveSeconds() {
        return autosaveSeconds;
    }

    public boolean isDirty() {
        return dirty;
    }

    public StorageMode getMode() {
        return mode;
    }

    public void close() {
        this.dirty = false;
    }

    private Map<UUID, Deque<CombatHistoryEntry>> loadFromYaml() {
        Map<UUID, Deque<CombatHistoryEntry>> result = new HashMap<>();
        if (yamlFile == null || !yamlFile.exists()) {
            return result;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(yamlFile);
        ConfigurationSection histories = yaml.getConfigurationSection("histories");
        if (histories == null) {
            return result;
        }

        for (String uuidKey : histories.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidKey);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            List<Map<?, ?>> rawEntries = yaml.getMapList("histories." + uuidKey);
            Deque<CombatHistoryEntry> deque = new ArrayDeque<>();
            for (Map<?, ?> raw : rawEntries) {
                long timestamp = raw.get("timestamp") instanceof Number number ? number.longValue() : System.currentTimeMillis();
                Object playerValue = raw.containsKey("player") ? raw.get("player") : "Unknown";
                Object attackerValue = raw.containsKey("attacker") ? raw.get("attacker") : "None";
                Object typeValue = raw.containsKey("type") ? raw.get("type") : "UNKNOWN";
                Object detailsValue = raw.containsKey("details") ? raw.get("details") : "";
                deque.addLast(new CombatHistoryEntry(
                        timestamp,
                        String.valueOf(playerValue),
                        String.valueOf(attackerValue),
                        String.valueOf(typeValue),
                        String.valueOf(detailsValue)
                ));
            }
            result.put(uuid, deque);
        }
        return result;
    }

    private void saveToYaml(Map<UUID, Deque<CombatHistoryEntry>> historyMap) throws IOException {
        if (yamlFile == null) {
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Deque<CombatHistoryEntry>> entry : historyMap.entrySet()) {
            List<Map<String, Object>> serialized = new ArrayList<>();
            for (CombatHistoryEntry historyEntry : entry.getValue()) {
                Map<String, Object> map = new HashMap<>();
                map.put("timestamp", historyEntry.timestampMillis());
                map.put("player", historyEntry.player());
                map.put("attacker", historyEntry.attacker());
                map.put("type", historyEntry.type());
                map.put("details", historyEntry.details());
                serialized.add(map);
            }
            yaml.set("histories." + entry.getKey(), serialized);
        }
        File parent = yamlFile.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        yaml.save(yamlFile);
    }

    private void initializeSqlite() throws SQLException {
        if (sqliteFile == null) {
            return;
        }
        File parent = sqliteFile.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS wobblecombat_history (" +
                    "player_uuid TEXT NOT NULL," +
                    "timestamp_millis INTEGER NOT NULL," +
                    "player_name TEXT NOT NULL," +
                    "attacker_name TEXT NOT NULL," +
                    "entry_type TEXT NOT NULL," +
                    "details TEXT NOT NULL" +
                    ")");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_wobblecombat_history_player ON wobblecombat_history(player_uuid)");
        }
    }

    private Map<UUID, Deque<CombatHistoryEntry>> loadFromSqlite() throws SQLException {
        Map<UUID, Deque<CombatHistoryEntry>> result = new HashMap<>();
        if (sqliteFile == null || !sqliteFile.exists()) {
            return result;
        }
        String sql = "SELECT player_uuid, timestamp_millis, player_name, attacker_name, entry_type, details " +
                "FROM wobblecombat_history ORDER BY player_uuid ASC, timestamp_millis DESC";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(resultSet.getString("player_uuid"));
                } catch (IllegalArgumentException ignored) {
                    continue;
                }
                Deque<CombatHistoryEntry> deque = result.computeIfAbsent(uuid, key -> new ArrayDeque<>());
                deque.addLast(new CombatHistoryEntry(
                        resultSet.getLong("timestamp_millis"),
                        resultSet.getString("player_name"),
                        resultSet.getString("attacker_name"),
                        resultSet.getString("entry_type"),
                        resultSet.getString("details")
                ));
            }
        }
        return result;
    }

    private void saveToSqlite(Map<UUID, Deque<CombatHistoryEntry>> historyMap) throws SQLException {
        initializeSqlite();
        String deleteSql = "DELETE FROM wobblecombat_history";
        String insertSql = "INSERT INTO wobblecombat_history (player_uuid, timestamp_millis, player_name, attacker_name, entry_type, details) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement delete = connection.prepareStatement(deleteSql);
                 PreparedStatement insert = connection.prepareStatement(insertSql)) {
                delete.executeUpdate();
                for (Map.Entry<UUID, Deque<CombatHistoryEntry>> entry : historyMap.entrySet()) {
                    String playerUuid = entry.getKey().toString();
                    for (CombatHistoryEntry historyEntry : entry.getValue()) {
                        insert.setString(1, playerUuid);
                        insert.setLong(2, historyEntry.timestampMillis());
                        insert.setString(3, historyEntry.player());
                        insert.setString(4, historyEntry.attacker());
                        insert.setString(5, historyEntry.type());
                        insert.setString(6, historyEntry.details());
                        insert.addBatch();
                    }
                }
                insert.executeBatch();
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private Connection openConnection() throws SQLException {
        if (sqliteFile == null) {
            throw new SQLException("SQLite file is not configured.");
        }
        return DriverManager.getConnection("jdbc:sqlite:" + sqliteFile.getAbsolutePath());
    }
}
