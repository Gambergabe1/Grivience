package io.papermc.Grivience.accessory;

import java.util.Locale;

/**
 * Accessory enrichment levels (similar to Hypixel's recombobulator).
 * Enriched accessories gain bonus stats and rarity upgrades.
 */
public enum AccessoryEnrichment {
    NONE("None", 0, 1.0, 0),
    ENRICHED("Enriched", 1, 1.10, 1),      // +10% stats, +1 rarity
    RECOMBOBULATED("Recombobulated", 2, 1.25, 1); // +25% stats, +1 rarity (mythic possible)

    private final String displayName;
    private final int level;
    private final double statMultiplier;
    private final int rarityBoost;

    AccessoryEnrichment(String displayName, int level, double statMultiplier, int rarityBoost) {
        this.displayName = displayName;
        this.level = level;
        this.statMultiplier = statMultiplier;
        this.rarityBoost = rarityBoost;
    }

    public String displayName() {
        return displayName;
    }

    public int level() {
        return level;
    }

    public double statMultiplier() {
        return statMultiplier;
    }

    public int rarityBoost() {
        return rarityBoost;
    }

    public static AccessoryEnrichment parse(String input) {
        if (input == null || input.isBlank()) {
            return NONE;
        }
        String normalized = input.trim().toUpperCase(Locale.ROOT);
        for (AccessoryEnrichment type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return NONE;
    }
}
