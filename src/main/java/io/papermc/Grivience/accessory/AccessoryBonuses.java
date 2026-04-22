package io.papermc.Grivience.accessory;

/**
 * Snapshot of effective accessory-derived stat bonuses.
 * Includes Magical Power system similar to Hypixel Skyblock.
 */
public record AccessoryBonuses(
        double health,
        double defense,
        double strength,
        double critChance,
        double critDamage,
        double intelligence,
        double farmingFortune,
        int activeAccessories,
        int uniqueAccessories,
        int magicalPower,
        int echoStacks,
        boolean resonanceActive
) {
    public static final AccessoryBonuses NONE = new AccessoryBonuses(
            0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0, 0, 0, 0, false
    );
}
