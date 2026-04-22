package io.papermc.Grivience.accessory;

import io.papermc.Grivience.item.ItemRarity;
import java.util.Map;

/**
 * Calculates Magical Power (MP) contribution from accessories.
 * Based on Hypixel Skyblock's Magical Power system.
 */
public final class AccessoryPower {
    private AccessoryPower() {
    }

    /**
     * Calculate Magical Power for an accessory.
     */
    public static int calculatePower(ItemRarity rarity, AccessoryEnrichment enrichment) {
        if (rarity == null) {
            return 0;
        }

        // Base power by rarity
        int basePower = switch (rarity) {
            case COMMON -> 3;
            case UNCOMMON -> 5;
            case RARE -> 8;
            case EPIC -> 12;
            case LEGENDARY -> 16;
            case MYTHIC -> 22;
        };

        // Enrichment bonus
        if (enrichment == AccessoryEnrichment.ENRICHED) {
            basePower += 2;
        } else if (enrichment == AccessoryEnrichment.RECOMBOBULATED) {
            basePower += 4;
        }

        return basePower;
    }

    /**
     * Calculate total stat multiplier from Magical Power.
     * Every 10 MP = +0.5% to all accessory-derived stats.
     */
    public static double statMultiplierFromPower(int totalPower) {
        if (totalPower <= 0) {
            return 1.0;
        }
        // Consistent 0.5% per 10 MP (0.05% per 1 MP)
        return 1.0 + (totalPower * 0.0005);
    }

    /**
     * Calculate stats provided by the selected power type.
     */
    public static Map<String, Double> calculatePowerTypeStats(AccessoryPowerType type, int totalPower) {
        if (type == null || type == AccessoryPowerType.NONE || totalPower <= 0) {
            return Map.of();
        }
        return type.calculateStats(totalPower);
    }

    /**
     * Calculate bonus stats from Magical Power tiers.
     * Tiers grant flat stat bonuses similar to Hypixel's Rift Prism.
     */
    public static MagicalPowerBonuses calculatePowerBonuses(int totalPower) {
        int tier = totalPower / 50; // Every 50 MP = 1 tier

        double health = tier * 5.0;           // +5 HP per tier
        double defense = tier * 2.0;          // +2 Defense per tier
        double strength = tier * 1.0;         // +1 Strength per tier
        double intelligence = tier * 3.0;     // +3 Intelligence per tier

        return new MagicalPowerBonuses(health, defense, strength, intelligence);
    }

    public record MagicalPowerBonuses(
            double health,
            double defense,
            double strength,
            double intelligence
    ) {
        public static final MagicalPowerBonuses NONE = new MagicalPowerBonuses(0, 0, 0, 0);
    }
}
