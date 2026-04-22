package io.papermc.Grivience.bazaar;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores Bazaar account upgrades (Hypixel-style).
 *
 * Hypixel reference: "Bazaar Flipper" increases max concurrent orders and reduces Bazaar tax.
 * This data is stored per player account (UUID), not per SkyBlock profile.
 */
public final class BazaarAccountUpgrades {
    public static final int MAX_BAZAAR_FLIPPER_TIER = 5;

    private final File file;
    private final Map<UUID, Integer> bazaarFlipperTierByPlayer = new ConcurrentHashMap<>();

    public BazaarAccountUpgrades(File dataFolder) {
        this.file = new File(dataFolder, "bazaar_upgrades.yml");
        load();
    }

    public synchronized void load() {
        bazaarFlipperTierByPlayer.clear();

        if (!file.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("players");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            UUID playerId;
            try {
                playerId = UUID.fromString(key);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            int tier = clampBazaarFlipperTier(section.getInt(key + ".bazaar-flipper-tier", 0));
            if (tier > 0) {
                bazaarFlipperTierByPlayer.put(playerId, tier);
            }
        }
    }

    public synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection section = yaml.createSection("players");

        for (Map.Entry<UUID, Integer> entry : bazaarFlipperTierByPlayer.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            section.set(entry.getKey().toString() + ".bazaar-flipper-tier", clampBazaarFlipperTier(entry.getValue()));
        }

        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            yaml.save(file);
        } catch (IOException ignored) {
        }
    }

    public int getBazaarFlipperTier(UUID playerId) {
        if (playerId == null) {
            return 0;
        }
        return clampBazaarFlipperTier(bazaarFlipperTierByPlayer.getOrDefault(playerId, 0));
    }

    public synchronized void setBazaarFlipperTier(UUID playerId, int tier) {
        if (playerId == null) {
            return;
        }
        int clamped = clampBazaarFlipperTier(tier);
        if (clamped <= 0) {
            bazaarFlipperTierByPlayer.remove(playerId);
        } else {
            bazaarFlipperTierByPlayer.put(playerId, clamped);
        }
        save();
    }

    public int maxOrders(UUID playerId) {
        return maxOrdersForTier(getBazaarFlipperTier(playerId));
    }

    /**
     * Hypixel Bazaar tax is applied on selling. (Base 1.25%, reduced by Bazaar Flipper.)
     */
    public double sellTaxRate(UUID playerId) {
        return sellTaxRateForTier(getBazaarFlipperTier(playerId));
    }

    public static int maxOrdersForTier(int tier) {
        // Tier 0: 14, Tier I: 21, Tier II: 28, Tier III: 35, Tier IV: 42, Tier V: 56
        int clamped = clampBazaarFlipperTier(tier);
        if (clamped >= 5) return 56;
        return 14 + (7 * clamped);
    }

    public static double sellTaxRateForTier(int tier) {
        // Tier 0: 1.25%, Tier I: 1.125%, Tier II: 1%, Tier III: 0.875%, Tier IV: 0.75%, Tier V: 0.5%
        int clamped = clampBazaarFlipperTier(tier);
        if (clamped >= 5) return 0.005D;
        return 0.0125D - (0.00125D * clamped);
    }

    private static int clampBazaarFlipperTier(int tier) {
        if (tier <= 0) {
            return 0;
        }
        return Math.min(MAX_BAZAAR_FLIPPER_TIER, tier);
    }
}

