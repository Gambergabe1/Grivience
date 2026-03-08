package io.papermc.Grivience.stats;

/**
 * Snapshot of Skyblock-style stats in Skyblock units.
 *
 * Notes:
 * - Health is in Skyblock HP (base 100 on Skyblock).
 * - Defense is in Skyblock Defense (damage reduction uses 100/(100+defense)).
 * - Crit chance/damage are percents (0-100+).
 */
public record SkyblockPlayerStats(
        double health,
        double defense,
        double strength,
        double critChancePercent,
        double critDamagePercent,
        double intelligence,
        double farmingFortune
) {
}


