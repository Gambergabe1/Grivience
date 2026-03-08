package io.papermc.Grivience.pet;

/**
 * Additive Skyblock-style stats provided by a pet, in Skyblock units.
 * 100% accurate to Hypixel Skyblock's stat list for pets.
 */
public record PetStatBonuses(
        double health,
        double defense,
        double strength,
        double critChance,
        double critDamage,
        double intelligence,
        double speed,
        double attackSpeed,
        double ferocity,
        double magicFind,
        double petLuck,
        double seaCreatureChance,
        double trueDefense,
        double abilityDamage
) {
    public static final PetStatBonuses ZERO = new PetStatBonuses(0,0,0,0,0,0,0,0,0,0,0,0,0,0);

    // Legacy support for older constructor (if any exists in code)
    public PetStatBonuses(double health, double defense, double strength, double critChance, double critDamage, double intelligence) {
        this(health, defense, strength, critChance, critDamage, intelligence, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
