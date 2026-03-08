package io.papermc.Grivience.stats;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player bits storage and operations.
 */
public final class BitsManager {
    private final File folder;
    private final Map<UUID, Long> bitsCache = new ConcurrentHashMap<>();

    public BitsManager(File dataFolder) {
        this.folder = new File(dataFolder, "bits");
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    /**
     * Get the amount of bits a player has.
     */
    public long getBits(UUID playerId) {
        if (playerId == null) {
            return 0;
        }
        return bitsCache.computeIfAbsent(playerId, this::loadBits);
    }

    /**
     * Get the amount of bits a player has (by Player object).
     */
    public long getBits(Player player) {
        return getBits(player.getUniqueId());
    }

    /**
     * Set the amount of bits a player has.
     */
    public void setBits(UUID playerId, long amount) {
        if (playerId == null) {
            return;
        }
        long clampedAmount = Math.max(0, amount);
        bitsCache.put(playerId, clampedAmount);
        saveBits(playerId, clampedAmount);
    }

    /**
     * Set the amount of bits a player has (by Player object).
     */
    public void setBits(Player player, long amount) {
        setBits(player.getUniqueId(), amount);
    }

    /**
     * Add bits to a player's balance.
     */
    public void addBits(UUID playerId, long amount) {
        if (playerId == null || amount <= 0) {
            return;
        }
        long current = getBits(playerId);
        setBits(playerId, current + amount);
    }

    /**
     * Add bits to a player's balance (by Player object).
     */
    public void addBits(Player player, long amount) {
        addBits(player.getUniqueId(), amount);
    }

    /**
     * Remove bits from a player's balance.
     */
    public void removeBits(UUID playerId, long amount) {
        if (playerId == null || amount <= 0) {
            return;
        }
        long current = getBits(playerId);
        long newAmount = Math.max(0, current - amount);
        setBits(playerId, newAmount);
    }

    /**
     * Remove bits from a player's balance (by Player object).
     */
    public void removeBits(Player player, long amount) {
        removeBits(player.getUniqueId(), amount);
    }

    /**
     * Take bits from a player (alias for removeBits).
     */
    public void takeBits(UUID playerId, long amount) {
        removeBits(playerId, amount);
    }

    /**
     * Take bits from a player (alias for removeBits).
     */
    public void takeBits(Player player, long amount) {
        removeBits(player.getUniqueId(), amount);
    }

    /**
     * Check if a player has at least a certain amount of bits.
     */
    public boolean hasBits(UUID playerId, long amount) {
        return getBits(playerId) >= amount;
    }

    /**
     * Check if a player has at least a certain amount of bits.
     */
    public boolean hasBits(Player player, long amount) {
        return hasBits(player.getUniqueId(), amount);
    }

    /**
     * Load bits from the player's data file.
     */
    private long loadBits(UUID playerId) {
        File file = new File(folder, playerId.toString() + ".yml");
        if (!file.exists()) {
            return 0L;
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        return cfg.getLong("bits", 0L);
    }

    /**
     * Save bits to the player's data file.
     */
    private void saveBits(UUID playerId, long amount) {
        File file = new File(folder, playerId.toString() + ".yml");
        FileConfiguration cfg = new YamlConfiguration();
        cfg.set("bits", amount);
        try {
            cfg.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().severe("[Grivience] Failed to save bits for " + playerId + ": " + e.getMessage());
        }
    }

    /**
     * Clear cached bits for a player (useful on quit).
     */
    public void clearCache(UUID playerId) {
        bitsCache.remove(playerId);
    }

    /**
     * Clear cached bits for a player (useful on quit).
     */
    public void clearCache(Player player) {
        clearCache(player.getUniqueId());
    }
}
