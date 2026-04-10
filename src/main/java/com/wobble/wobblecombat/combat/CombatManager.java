package com.wobble.wobblecombat.combat;

import com.wobble.wobblecombat.WobbleCombatPlugin;
import com.wobble.wobblecombat.api.event.WobbleCombatTagEvent;
import com.wobble.wobblecombat.api.event.WobbleCombatUntagEvent;
import com.wobble.wobblecombat.util.Placeholders;
import com.wobble.wobblecombat.util.Text;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class CombatManager {

    public enum CommandBlockMode {
        BLACKLIST,
        WHITELIST
    }

    public enum CombatLogPunishmentMode {
        NONE,
        KILL,
        STRIP,
        COMMANDS,
        HYBRID
    }

    private final WobbleCombatPlugin plugin;
    private final Map<UUID, CombatTag> tags = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> bossBars = new ConcurrentHashMap<>();
    private final Map<UUID, java.util.Deque<CombatHistoryEntry>> history = new ConcurrentHashMap<>();
    private BukkitTask task;

    private int durationSeconds;
    private Set<String> disabledWorlds;
    private List<String> blockedCommands;
    private List<String> combatLogCommands;
    private List<String> ignoredKickReasons;
    private boolean broadcastCombatLog;
    private boolean ignoreNonSurvival;
    private boolean clearTagOnDeath;
    private boolean punishOnKick;
    private boolean usePlaceholderApi;
    private boolean closeOpenInventoryOnTag;
    private boolean dropInventoryOnCombatLog;
    private boolean clearInventoryOnCombatLog;
    private boolean dropExpOnCombatLog;
    private boolean clearExpOnCombatLog;
    private boolean blockEnderPearl;
    private boolean blockChorusFruit;
    private boolean blockPortal;
    private boolean blockElytra;
    private boolean combatLogNotifyEnabled;
    private String combatLogNotifyPermission;
    private String combatLogNotifyFormat;
    private double minimumFinalDamageToTag;
    private boolean ignoreSelfDamageTagging;
    private int historyLimit;
    private CommandBlockMode commandBlockMode;
    private CombatLogPunishmentMode combatLogPunishmentMode;

    private boolean bossBarEnabled;
    private String bossBarText;
    private BossBar.Color bossBarColor;
    private BossBar.Overlay bossBarOverlay;

    public CombatManager(WobbleCombatPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void start() {
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (UUID uuid : new ArrayList<>(bossBars.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                hideBossBar(player);
            }
        }
        bossBars.clear();
        tags.clear();
    }

    public void reload() {
        FileConfiguration config = plugin.getConfig();
        this.durationSeconds = Math.max(1, config.getInt("combat-tag-duration", 15));
        this.disabledWorlds = config.getStringList("disabled-worlds").stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        this.blockedCommands = new ArrayList<>();
        for (String command : config.getStringList("blocked-commands")) {
            String normalized = normalizeConfiguredCommand(command);
            if (normalized != null) {
                this.blockedCommands.add(normalized);
            }
        }

        this.combatLogCommands = config.getStringList("settings.combat-log-commands").stream()
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toList());
        this.ignoredKickReasons = config.getStringList("settings.ignored-kick-reasons").stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toList());
        this.broadcastCombatLog = config.getBoolean("settings.broadcast-combat-log", true);
        this.ignoreNonSurvival = config.getBoolean("settings.ignore-non-survival", true);
        this.clearTagOnDeath = config.getBoolean("settings.clear-tag-on-death", true);
        this.punishOnKick = config.getBoolean("settings.punish-on-kick", true);
        this.usePlaceholderApi = config.getBoolean("settings.use-placeholderapi-if-available", true);
        this.closeOpenInventoryOnTag = config.getBoolean("settings.close-open-inventory-on-tag", false);
        this.dropInventoryOnCombatLog = config.getBoolean("settings.combat-log-drop-inventory", false);
        this.clearInventoryOnCombatLog = config.getBoolean("settings.combat-log-clear-inventory", false);
        this.dropExpOnCombatLog = config.getBoolean("settings.combat-log-drop-exp", false);
        this.clearExpOnCombatLog = config.getBoolean("settings.combat-log-clear-exp", false);
        this.blockEnderPearl = config.getBoolean("settings.restrictions.block-ender-pearl", false);
        this.blockChorusFruit = config.getBoolean("settings.restrictions.block-chorus-fruit", false);
        this.blockPortal = config.getBoolean("settings.restrictions.block-portals", false);
        this.blockElytra = config.getBoolean("settings.restrictions.block-elytra", false);
        this.combatLogNotifyEnabled = config.getBoolean("settings.staff-notify.combat-log.enabled", false);
        this.combatLogNotifyPermission = config.getString("settings.staff-notify.combat-log.permission", "wobblecombat.notify");
        this.combatLogNotifyFormat = config.getString("settings.staff-notify.combat-log.format", "&8[&cWobbleCombat&8] &f{player} &7logged during combat. &8(&f{attacker}&8)");
        this.minimumFinalDamageToTag = Math.max(0.0D, config.getDouble("settings.damage.minimum-final-damage-to-tag", 0.1D));
        this.ignoreSelfDamageTagging = config.getBoolean("settings.damage.ignore-self-damage-tagging", true);
        this.historyLimit = Math.max(1, config.getInt("settings.history-limit", 15));
        this.commandBlockMode = parseMode(config.getString("settings.command-block-mode", "BLACKLIST"));
        this.combatLogPunishmentMode = parseCombatLogMode(config);

        this.bossBarEnabled = plugin.getMessages().getBoolean("bossbar.enabled", false);
        this.bossBarText = plugin.getMessages().getString("bossbar.text", "&cCombat: &f{time}s");
        this.bossBarColor = parseBossBarColor(plugin.getMessages().getString("bossbar.color", "RED"));
        this.bossBarOverlay = parseBossBarOverlay(plugin.getMessages().getString("bossbar.overlay", "PROGRESS"));

        if (dropInventoryOnCombatLog && combatLogPunishmentMode == CombatLogPunishmentMode.KILL && !clearInventoryOnCombatLog) {
            plugin.getLogger().warning("combat-log-drop-inventory is enabled while combat-log-clear-inventory is disabled. This can duplicate drops when KILL punishment is used.");
        }
        if (dropExpOnCombatLog && combatLogPunishmentMode == CombatLogPunishmentMode.KILL && !clearExpOnCombatLog) {
            plugin.getLogger().warning("combat-log-drop-exp is enabled while combat-log-clear-exp is disabled. This can duplicate experience when KILL punishment is used.");
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isTagged(player)) {
                hideBossBar(player);
            }
        }
    }

    private String normalizeConfiguredCommand(String command) {
        if (command == null || command.isBlank()) {
            return null;
        }
        String normalized = command.trim().toLowerCase(Locale.ROOT);
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized;
    }

    private String normalizeMessageForCompare(String rawMessage, boolean stripNamespace) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return "";
        }
        String normalized = rawMessage.trim().toLowerCase(Locale.ROOT);
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }

        int split = normalized.indexOf(' ');
        String root = split == -1 ? normalized : normalized.substring(0, split);
        String suffix = split == -1 ? "" : normalized.substring(split);
        if (stripNamespace) {
            int namespaceIndex = root.indexOf(':');
            if (namespaceIndex != -1) {
                root = "/" + root.substring(namespaceIndex + 1);
            }
        }
        return root + suffix;
    }

    private CommandBlockMode parseMode(String raw) {
        if (raw == null) {
            return CommandBlockMode.BLACKLIST;
        }
        try {
            return CommandBlockMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return CommandBlockMode.BLACKLIST;
        }
    }

    private CombatLogPunishmentMode parseCombatLogMode(FileConfiguration config) {
        String raw = config.getString("settings.combat-log-punishment-mode");
        if (raw == null || raw.isBlank()) {
            boolean legacy = config.getBoolean("settings.punish-combat-log", true);
            return legacy ? CombatLogPunishmentMode.KILL : CombatLogPunishmentMode.NONE;
        }
        try {
            return CombatLogPunishmentMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return CombatLogPunishmentMode.KILL;
        }
    }

    private BossBar.Color parseBossBarColor(String raw) {
        try {
            return BossBar.Color.valueOf(raw == null ? "RED" : raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return BossBar.Color.RED;
        }
    }

    private BossBar.Overlay parseBossBarOverlay(String raw) {
        try {
            return BossBar.Overlay.valueOf(raw == null ? "PROGRESS" : raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return BossBar.Overlay.PROGRESS;
        }
    }

    public void tag(Player first, Player second) {
        if (!canBeTagged(first) || !canBeTagged(second)) {
            return;
        }

        long expiresAt = System.currentTimeMillis() + (durationSeconds * 1000L);
        tagSingle(first, expiresAt, second.getUniqueId(), second, "pvp");
        tagSingle(second, expiresAt, first.getUniqueId(), first, "pvp");
        addHistory(first, second, "TAG", "combat-start");
        addHistory(second, first, "TAG", "combat-start");
    }

    private void tagSingle(Player player, long expiresAt, UUID attacker, Player attackerPlayer, String cause) {
        boolean wasTagged = isTagged(player.getUniqueId());
        tags.put(player.getUniqueId(), new CombatTag(expiresAt, attacker));
        Bukkit.getPluginManager().callEvent(new WobbleCombatTagEvent(player, attackerPlayer, durationSeconds, cause));
        if (!wasTagged) {
            if (closeOpenInventoryOnTag && player.getOpenInventory() != null
                    && player.getOpenInventory().getTopInventory().getType() != InventoryType.CRAFTING) {
                player.closeInventory();
            }
            sendTaggedEffects(player);
        }
    }

    public boolean canBeTagged(Player player) {
        if (player == null || !player.isOnline() || player.isDead()) {
            return false;
        }
        if (player.hasPermission("wobblecombat.bypass")) {
            return false;
        }
        if (isDisabledWorld(player.getWorld())) {
            return false;
        }
        if (ignoreNonSurvival) {
            GameMode mode = player.getGameMode();
            if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR) {
                return false;
            }
        }
        return true;
    }

    public boolean isDisabledWorld(World world) {
        return world != null && disabledWorlds.contains(world.getName().toLowerCase(Locale.ROOT));
    }

    public boolean isTagged(Player player) {
        return player != null && isTagged(player.getUniqueId());
    }

    public boolean isTagged(UUID uuid) {
        CombatTag tag = tags.get(uuid);
        return tag != null && tag.expiresAtMillis() > System.currentTimeMillis();
    }

    public int getRemainingSeconds(Player player) {
        if (player == null) {
            return 0;
        }
        CombatTag tag = tags.get(player.getUniqueId());
        if (tag == null) {
            return 0;
        }
        long remainingMillis = tag.expiresAtMillis() - System.currentTimeMillis();
        if (remainingMillis <= 0L) {
            return 0;
        }
        return (int) Math.ceil(remainingMillis / 1000.0D);
    }

    public UUID getLastAttacker(Player player) {
        CombatTag tag = player == null ? null : tags.get(player.getUniqueId());
        return tag == null ? null : tag.lastAttacker();
    }

    public String getLastAttackerName(Player player) {
        UUID attackerId = getLastAttacker(player);
        if (attackerId == null) {
            return "None";
        }
        Player attacker = Bukkit.getPlayer(attackerId);
        return attacker == null ? attackerId.toString() : attacker.getName();
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public CombatLogPunishmentMode getCombatLogPunishmentMode() {
        return combatLogPunishmentMode;
    }

    public int getTrackedPlayerCount() {
        return tags.size();
    }

    public int getHistoryCount(Player player) {
        return getHistory(player).size();
    }

    public boolean shouldTagForDamage(double finalDamage, double baseDamage) {
        return Math.max(finalDamage, baseDamage) >= minimumFinalDamageToTag;
    }

    public boolean shouldIgnoreSelfDamageTagging() {
        return ignoreSelfDamageTagging;
    }

    public void clear(Player player) {
        clear(player, "manual-clear");
    }

    public void clear(Player player, String reason) {
        if (player != null) {
            clear(player.getUniqueId(), reason);
            hideBossBar(player);
        }
    }

    public void clear(UUID uuid) {
        clear(uuid, "manual-clear");
    }

    public void clear(UUID uuid, String reason) {
        if (uuid == null) {
            return;
        }
        CombatTag removed = tags.remove(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            hideBossBar(player);
            if (removed != null) {
                Bukkit.getPluginManager().callEvent(new WobbleCombatUntagEvent(player, reason));
            }
        }
    }

    public boolean isCommandBlocked(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return false;
        }

        String normalized = normalizeMessageForCompare(rawMessage, false);
        String deNamespaced = normalizeMessageForCompare(rawMessage, true);
        boolean matched = false;

        for (String entry : blockedCommands) {
            if (matchesCommand(entry, normalized) || matchesCommand(entry, deNamespaced)) {
                matched = true;
                break;
            }
        }

        if (commandBlockMode == CommandBlockMode.WHITELIST) {
            return !matched;
        }
        return matched;
    }

    private boolean matchesCommand(String entry, String message) {
        return message.equals(entry) || message.startsWith(entry + " ");
    }



    public java.util.List<CombatHistoryEntry> getHistory(Player player) {
        if (player == null) {
            return java.util.Collections.emptyList();
        }
        java.util.Deque<CombatHistoryEntry> deque = history.get(player.getUniqueId());
        if (deque == null || deque.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return new java.util.ArrayList<>(deque);
    }

    public void addHistory(Player player, Player attacker, String type, String details) {
        if (player == null) {
            return;
        }
        java.util.Deque<CombatHistoryEntry> deque = history.computeIfAbsent(player.getUniqueId(), k -> new java.util.ArrayDeque<>());
        deque.addFirst(new CombatHistoryEntry(System.currentTimeMillis(), player.getName(), attacker == null ? "None" : attacker.getName(), type, details));
        while (deque.size() > historyLimit) {
            deque.removeLast();
        }
        plugin.getHistoryStorage().markDirty();
    }

    public void loadHistory(Map<UUID, java.util.Deque<CombatHistoryEntry>> loadedHistory) {
        history.clear();
        if (loadedHistory != null) {
            history.putAll(loadedHistory);
        }
    }

    public Map<UUID, java.util.Deque<CombatHistoryEntry>> snapshotHistory() {
        Map<UUID, java.util.Deque<CombatHistoryEntry>> snapshot = new java.util.HashMap<>();
        for (Map.Entry<UUID, java.util.Deque<CombatHistoryEntry>> entry : history.entrySet()) {
            snapshot.put(entry.getKey(), new java.util.ArrayDeque<>(entry.getValue()));
        }
        return snapshot;
    }

    public boolean isEnderPearlBlocked() {
        return blockEnderPearl;
    }

    public boolean isChorusFruitBlocked() {
        return blockChorusFruit;
    }

    public boolean isPortalBlocked() {
        return blockPortal;
    }

    public boolean isElytraBlocked() {
        return blockElytra;
    }

    public boolean shouldBroadcastCombatLog() {
        return broadcastCombatLog;
    }

    public boolean shouldClearTagOnDeath() {
        return clearTagOnDeath;
    }

    public boolean shouldPunishOnKick(String reason) {
        if (!punishOnKick) {
            return false;
        }
        String normalized = reason == null ? "" : reason.toLowerCase(Locale.ROOT);
        for (String fragment : ignoredKickReasons) {
            if (normalized.contains(fragment)) {
                return false;
            }
        }
        return true;
    }

    public boolean shouldPunishCombatLog() {
        return combatLogPunishmentMode != CombatLogPunishmentMode.NONE;
    }

    public void executeCombatLogPunishment(Player player) {
        if (player == null) {
            return;
        }

        if (dropInventoryOnCombatLog) {
            dropInventory(player);
        }
        if (dropExpOnCombatLog) {
            dropExperience(player);
        }
        if (clearInventoryOnCombatLog) {
            clearInventory(player);
        }
        if (clearExpOnCombatLog) {
            clearExperience(player);
        }

        if (!combatLogCommands.isEmpty()) {
            ConsoleCommandSender console = Bukkit.getConsoleSender();
            for (String command : combatLogCommands) {
                String parsed = apply(player, command, Map.of(
                        "player", player.getName(),
                        "time", String.valueOf(getRemainingSeconds(player)),
                        "attacker", getLastAttackerName(player)
                ));
                String sanitized = parsed.startsWith("/") ? parsed.substring(1) : parsed;
                Bukkit.dispatchCommand(console, sanitized);
            }
        }

        if (combatLogPunishmentMode == CombatLogPunishmentMode.KILL || combatLogPunishmentMode == CombatLogPunishmentMode.HYBRID) {
            if (!player.isDead()) {
                player.setHealth(0.0D);
            }
        }

        if (combatLogPunishmentMode == CombatLogPunishmentMode.STRIP || combatLogPunishmentMode == CombatLogPunishmentMode.HYBRID) {
            if (!dropInventoryOnCombatLog) {
                dropInventory(player);
            }
            if (!clearInventoryOnCombatLog) {
                clearInventory(player);
            }
            if (!dropExpOnCombatLog) {
                dropExperience(player);
            }
            if (!clearExpOnCombatLog) {
                clearExperience(player);
            }
        }

        playConfiguredSound(player, "sounds.combat-log");
    }

    private void dropInventory(Player player) {
        dropItemArray(player, player.getInventory().getStorageContents());
        dropItemArray(player, player.getInventory().getArmorContents());
        dropItemArray(player, player.getInventory().getExtraContents());
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand != null && !offHand.getType().isAir()) {
            player.getWorld().dropItemNaturally(player.getLocation(), offHand.clone());
        }
    }

    private void dropItemArray(Player player, ItemStack[] contents) {
        if (contents == null) {
            return;
        }
        for (ItemStack item : contents) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            player.getWorld().dropItemNaturally(player.getLocation(), item.clone());
        }
    }

    private void clearInventory(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[player.getInventory().getArmorContents().length]);
        player.getInventory().setExtraContents(new ItemStack[player.getInventory().getExtraContents().length]);
        player.getInventory().setItemInOffHand(null);
        player.updateInventory();
    }

    private void dropExperience(Player player) {
        int totalExp = Math.max(0, player.getTotalExperience());
        if (totalExp <= 0) {
            return;
        }
        player.getWorld().spawn(player.getLocation(), ExperienceOrb.class, orb -> orb.setExperience(totalExp));
    }

    private void clearExperience(Player player) {
        player.setExp(0.0F);
        player.setLevel(0);
        player.setTotalExperience(0);
    }

    public void notifyCombatLog(Player player, String reason) {
        if (player == null || !combatLogNotifyEnabled || combatLogNotifyPermission == null || combatLogNotifyPermission.isBlank()) {
            return;
        }
        String template = combatLogNotifyFormat == null || combatLogNotifyFormat.isBlank()
                ? "&8[&cWobbleCombat&8] &f{player} &7logged during combat. &8(&f{attacker}&8)"
                : combatLogNotifyFormat;
        String rendered = Text.color(apply(player, template, Map.of(
                "player", player.getName(),
                "reason", reason == null ? "" : reason,
                "attacker", getLastAttackerName(player),
                "time", String.valueOf(getRemainingSeconds(player)),
                "status", "tagged"
        )));
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission(combatLogNotifyPermission)) {
                online.sendMessage(rendered);
            }
        }
        plugin.getLogger().info(Text.stripColor(rendered));
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, CombatTag> entry : new ArrayList<>(tags.entrySet())) {
            UUID uuid = entry.getKey();
            CombatTag tag = entry.getValue();

            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline() || player.isDead()) {
                if (tag.expiresAtMillis() <= now) {
                    tags.remove(uuid);
                }
                continue;
            }

            if (tag.expiresAtMillis() <= now) {
                tags.remove(uuid);
                hideBossBar(player);
                addHistory(player, null, "UNTAG", "timer-expired");
                Bukkit.getPluginManager().callEvent(new WobbleCombatUntagEvent(player, "timer-expired"));
                sendUntaggedEffects(player);
                continue;
            }

            int remaining = (int) Math.ceil((tag.expiresAtMillis() - now) / 1000.0D);
            sendCombatActionBar(player, remaining);
            updateBossBar(player, remaining);
        }
    }

    private void sendCombatActionBar(Player player, int remaining) {
        String actionBar = plugin.getMessages().getString("action-bar.combat", "");
        if (!actionBar.isBlank()) {
            player.sendActionBar(Text.component(apply(player, actionBar, Map.of(
                    "time", String.valueOf(remaining),
                    "attacker", getLastAttackerName(player),
                    "status", "tagged"
            ))));
        }
    }

    private void sendTaggedEffects(Player player) {
        String message = plugin.getMessages().getString("chat.tagged", "");
        if (!message.isBlank()) {
            player.sendMessage(Text.color(apply(player, message, Map.of(
                    "time", String.valueOf(durationSeconds),
                    "attacker", getLastAttackerName(player),
                    "status", "tagged"
            ))));
        }
        sendConfiguredTitle(player, "titles.tagged", Map.of(
                "time", String.valueOf(durationSeconds),
                "attacker", getLastAttackerName(player),
                "status", "tagged"
        ));
        playConfiguredSound(player, "sounds.tagged");
        updateBossBar(player, durationSeconds);
    }

    private void sendUntaggedEffects(Player player) {
        String endBar = plugin.getMessages().getString("action-bar.end", "");
        if (!endBar.isBlank()) {
            player.sendActionBar(Text.component(apply(player, endBar, Map.of(
                    "status", "safe"
            ))));
        }
        String message = plugin.getMessages().getString("chat.untagged", "");
        if (!message.isBlank()) {
            player.sendMessage(Text.color(apply(player, message, Map.of(
                    "status", "safe"
            ))));
        }
        sendConfiguredTitle(player, "titles.untagged", Map.of(
                "status", "safe"
        ));
        playConfiguredSound(player, "sounds.untagged");
    }

    private void sendConfiguredTitle(Player player, String path, Map<String, String> replacements) {
        if (!plugin.getMessages().getBoolean(path + ".enabled", false)) {
            return;
        }

        String title = apply(player, plugin.getMessages().getString(path + ".title", ""), replacements);
        String subtitle = apply(player, plugin.getMessages().getString(path + ".subtitle", ""), replacements);
        int fadeIn = plugin.getMessages().getInt(path + ".fade-in", 5);
        int stay = plugin.getMessages().getInt(path + ".stay", 30);
        int fadeOut = plugin.getMessages().getInt(path + ".fade-out", 10);
        player.sendTitlePart(net.kyori.adventure.title.TitlePart.TITLE, Text.component(title));
        player.sendTitlePart(net.kyori.adventure.title.TitlePart.SUBTITLE, Text.component(subtitle));
        player.sendTitlePart(net.kyori.adventure.title.TitlePart.TIMES,
                net.kyori.adventure.title.Title.Times.times(
                        java.time.Duration.ofMillis(fadeIn * 50L),
                        java.time.Duration.ofMillis(stay * 50L),
                        java.time.Duration.ofMillis(fadeOut * 50L)
                ));
    }

    private void playConfiguredSound(Player player, String path) {
        if (!plugin.getMessages().getBoolean(path + ".enabled", false)) {
            return;
        }
        String rawSound = plugin.getMessages().getString(path + ".name", "ENTITY_EXPERIENCE_ORB_PICKUP");
        float volume = plugin.getMessages().getFloat(path + ".volume", 1.0F);
        float pitch = plugin.getMessages().getFloat(path + ".pitch", 1.0F);

        try {
            org.bukkit.Sound sound = org.bukkit.Sound.valueOf(rawSound.toUpperCase(Locale.ROOT));
            player.playSound(player.getLocation(), sound, SoundCategory.MASTER, volume, pitch);
        } catch (IllegalArgumentException ignored) {
            plugin.getLogger().warning("Invalid sound in messages.yml at " + path + ".name: " + rawSound);
        }
    }

    private void updateBossBar(Player player, int remaining) {
        if (!bossBarEnabled) {
            hideBossBar(player);
            return;
        }

        BossBar bar = bossBars.computeIfAbsent(player.getUniqueId(), uuid -> {
            BossBar created = BossBar.bossBar(Text.component(""), 1.0F, bossBarColor, bossBarOverlay);
            player.showBossBar(created);
            return created;
        });

        float progress = Math.max(0.0F, Math.min(1.0F, remaining / (float) durationSeconds));
        bar.name(Text.component(apply(player, bossBarText, Map.of(
                "time", String.valueOf(remaining),
                "attacker", getLastAttackerName(player),
                "status", "tagged"
        ))));
        bar.progress(progress);
        bar.color(bossBarColor);
        bar.overlay(bossBarOverlay);
    }

    private void hideBossBar(Player player) {
        BossBar bar = bossBars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    public String apply(Player player, String text, Map<String, String> replacements) {
        return Placeholders.apply(player, text, replacements, usePlaceholderApi);
    }
}
