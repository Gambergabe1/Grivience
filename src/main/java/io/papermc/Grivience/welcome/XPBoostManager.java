package io.papermc.Grivience.welcome;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages XP boosts for players (mining, farming, etc.).
 */
public class XPBoostManager {
    private static final double MIN_BOOST_PERCENT = 0.0;
    private static final double MAX_BOOST_PERCENT = 500.0; // Cap at 500% (6x multiplier) to prevent runaway XP

    private final GriviencePlugin plugin;
    private final Map<UUID, BoostData> miningBoosts = new ConcurrentHashMap<>();
    private final Map<UUID, BoostData> farmingBoosts = new ConcurrentHashMap<>();
    private final File boostFile;
    private FileConfiguration boostConfig;

    private double miningBoostPercent = 15.0;
    private double farmingBoostPercent = 15.0;
    private int defaultDurationMinutes = 45;

    public XPBoostManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.boostFile = new File(plugin.getDataFolder(), "xp-boosts.yml");
        loadConfig();
        loadBoosts();
        loadBoostsFromFile();
        startExpirationTask();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();

        miningBoostPercent = clampBoostPercent(config.getDouble("welcome-event.mining-boost-percent", 15.0));
        farmingBoostPercent = clampBoostPercent(config.getDouble("welcome-event.farming-boost-percent", 15.0));
        defaultDurationMinutes = Math.max(1, config.getInt("welcome-event.boost-duration-minutes", 45));
    }

    private double clampBoostPercent(double value) {
        if (value < MIN_BOOST_PERCENT) {
            plugin.getLogger().warning("Mining/farming boost percent cannot be less than " + MIN_BOOST_PERCENT + "%. Clamping to minimum.");
            return MIN_BOOST_PERCENT;
        }
        if (value > MAX_BOOST_PERCENT) {
            plugin.getLogger().warning("Mining/farming boost percent cannot exceed " + MAX_BOOST_PERCENT + "%. Clamping to maximum to prevent runaway XP.");
            return MAX_BOOST_PERCENT;
        }
        return value;
    }

    public void loadBoosts() {
        if (!boostFile.exists()) {
            File parent = boostFile.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            try {
                boostFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create XP boosts file: " + e.getMessage());
            }
        }

        boostConfig = YamlConfiguration.loadConfiguration(boostFile);
    }

    public void saveBoosts() {
        if (boostConfig == null) {
            return;
        }
        try {
            boostConfig.save(boostFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save XP boosts: " + e.getMessage());
        }
    }

    /**
     * Apply a mining boost to a player.
     */
    public void applyMiningBoost(Player player, int durationMinutes) {
        UUID uuid = player.getUniqueId();
        long expiryTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L);

        miningBoosts.put(uuid, new BoostData(uuid, expiryTime, durationMinutes));

        if (boostConfig != null) {
            String path = "mining." + uuid;
            boostConfig.set(path + ".expiry", expiryTime);
            boostConfig.set(path + ".duration", durationMinutes);
            boostConfig.set(path + ".start", System.currentTimeMillis());
            saveBoosts();
        }

        player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Mining Boost Applied!");
        player.sendMessage(ChatColor.GRAY + "+" + ChatColor.YELLOW + formatPercent(miningBoostPercent)
                + "% " + ChatColor.GRAY + "Mining XP for " + ChatColor.YELLOW + durationMinutes + " minutes");
    }

    /**
     * Apply a farming boost to a player.
     */
    public void applyFarmingBoost(Player player, int durationMinutes) {
        UUID uuid = player.getUniqueId();
        long expiryTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L);

        farmingBoosts.put(uuid, new BoostData(uuid, expiryTime, durationMinutes));

        if (boostConfig != null) {
            String path = "farming." + uuid;
            boostConfig.set(path + ".expiry", expiryTime);
            boostConfig.set(path + ".duration", durationMinutes);
            boostConfig.set(path + ".start", System.currentTimeMillis());
            saveBoosts();
        }

        player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Farming Boost Applied!");
        player.sendMessage(ChatColor.GRAY + "+" + ChatColor.YELLOW + formatPercent(farmingBoostPercent)
                + "% " + ChatColor.GRAY + "Farming XP for " + ChatColor.YELLOW + durationMinutes + " minutes");
    }

    /**
     * Check if a player has an active mining boost.
     */
    public boolean hasMiningBoost(Player player) {
        BoostData boost = miningBoosts.get(player.getUniqueId());
        return boost != null && !boost.isExpired();
    }

    /**
     * Check if a player has an active farming boost.
     */
    public boolean hasFarmingBoost(Player player) {
        BoostData boost = farmingBoosts.get(player.getUniqueId());
        return boost != null && !boost.isExpired();
    }

    /**
     * Get the mining boost multiplier for a player.
     */
    public double getMiningBoostMultiplier(Player player) {
        if (hasMiningBoost(player)) {
            return 1.0 + (miningBoostPercent / 100.0);
        }
        return 1.0;
    }

    /**
     * Get the farming boost multiplier for a player.
     */
    public double getFarmingBoostMultiplier(Player player) {
        if (hasFarmingBoost(player)) {
            return 1.0 + (farmingBoostPercent / 100.0);
        }
        return 1.0;
    }

    /**
     * Get remaining time for mining boost in minutes.
     */
    public int getMiningBoostRemaining(Player player) {
        BoostData boost = miningBoosts.get(player.getUniqueId());
        if (boost == null || boost.isExpired()) {
            return 0;
        }
        long remaining = boost.getExpiryTime() - System.currentTimeMillis();
        return (int) (remaining / 60000L);
    }

    /**
     * Get remaining time for farming boost in minutes.
     */
    public int getFarmingBoostRemaining(Player player) {
        BoostData boost = farmingBoosts.get(player.getUniqueId());
        if (boost == null || boost.isExpired()) {
            return 0;
        }
        long remaining = boost.getExpiryTime() - System.currentTimeMillis();
        return (int) (remaining / 60000L);
    }

    /**
     * Remove a player's mining boost.
     */
    public void removeMiningBoost(Player player) {
        UUID uuid = player.getUniqueId();
        miningBoosts.remove(uuid);
        if (boostConfig != null) {
            boostConfig.set("mining." + uuid, null);
            saveBoosts();
        }
    }

    /**
     * Remove a player's farming boost.
     */
    public void removeFarmingBoost(Player player) {
        UUID uuid = player.getUniqueId();
        farmingBoosts.remove(uuid);
        if (boostConfig != null) {
            boostConfig.set("farming." + uuid, null);
            saveBoosts();
        }
    }

    /**
     * Load boosts from file on startup.
     */
    private void loadBoostsFromFile() {
        if (boostConfig == null) {
            return;
        }

        long now = System.currentTimeMillis();
        loadBoostSection("mining", miningBoosts, now);
        loadBoostSection("farming", farmingBoosts, now);
        saveBoosts();
    }

    private void loadBoostSection(String path, Map<UUID, BoostData> target, long now) {
        ConfigurationSection section = boostConfig.getConfigurationSection(path);
        if (section == null) {
            return;
        }

        for (String uuidStr : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                long expiry = section.getLong(uuidStr + ".expiry", 0L);
                int duration = section.getInt(uuidStr + ".duration", defaultDurationMinutes);

                if (expiry > now) {
                    target.put(uuid, new BoostData(uuid, expiry, duration));
                } else {
                    boostConfig.set(path + "." + uuidStr, null);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    /**
     * Start a task to check for expired boosts.
     */
    private void startExpirationTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            miningBoosts.entrySet().removeIf(entry -> {
                if (entry.getValue().isExpired()) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player != null && player.isOnline()) {
                        player.sendMessage(ChatColor.YELLOW + "Your Mining Boost has expired!");
                    }
                    if (boostConfig != null) {
                        boostConfig.set("mining." + entry.getKey(), null);
                        saveBoosts();
                    }
                    return true;
                }
                return false;
            });

            farmingBoosts.entrySet().removeIf(entry -> {
                if (entry.getValue().isExpired()) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player != null && player.isOnline()) {
                        player.sendMessage(ChatColor.YELLOW + "Your Farming Boost has expired!");
                    }
                    if (boostConfig != null) {
                        boostConfig.set("farming." + entry.getKey(), null);
                        saveBoosts();
                    }
                    return true;
                }
                return false;
            });
        }, 1200L, 1200L);
    }

    /**
     * Apply XP boost with multiplier.
     */
    public double applyBoost(double baseXP, Player player, BoostType type) {
        if (type == null) return baseXP;
        return switch (type) {
            case MINING -> baseXP * getMiningBoostMultiplier(player);
            case FARMING -> baseXP * getFarmingBoostMultiplier(player);
            default -> baseXP;
        };
    }

    public enum BoostType {
        MINING,
        FARMING,
        COMBAT,
        FORAGING,
        FISHING,
        ENCHANTING
    }

    public double getMiningBoostPercent() {
        return miningBoostPercent;
    }

    public double getFarmingBoostPercent() {
        return farmingBoostPercent;
    }

    public int getDefaultDurationMinutes() {
        return defaultDurationMinutes;
    }

    private String formatPercent(double value) {
        if (Math.rint(value) == value) {
            return String.format(Locale.ROOT, "%.0f", value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    /**
     * Boost data class.
     */
    public static class BoostData {
        private final UUID playerId;
        private final long expiryTime;
        private final int durationMinutes;
        private final long startTime;

        public BoostData(UUID playerId, long expiryTime, int durationMinutes) {
            this.playerId = playerId;
            this.expiryTime = expiryTime;
            this.durationMinutes = durationMinutes;
            this.startTime = System.currentTimeMillis();
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public long getExpiryTime() {
            return expiryTime;
        }

        public int getDurationMinutes() {
            return durationMinutes;
        }

        public long getStartTime() {
            return startTime;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() >= expiryTime;
        }

        public long getRemainingTime() {
            return Math.max(0, expiryTime - System.currentTimeMillis());
        }
    }
}
