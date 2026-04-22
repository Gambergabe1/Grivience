package io.papermc.Grivience.item;

import java.util.Locale;

public enum CustomWeaponType {
    WARDENS_CLEAVER,
    NEWBIE_KATANA,
    ONI_CLEAVER,
    TENGU_GALEBLADE,
    TENGU_STORMBOW,
    TENGU_SHORTBOW,
    KAPPA_TIDEBREAKER,
    ONRYO_SPIRITBLADE,
    ONRYO_SHORTBOW,
    JOROGUMO_STINGER,
    JOROGUMO_SHORTBOW,
    KITSUNE_FANG,
    KITSUNE_DAWNBOW,
    KITSUNE_SHORTBOW,
    GASHADOKURO_NODACHI,
    FLYING_RAIJIN,
    HAYABUSA_KATANA,
    RAIJIN_SHORTBOW,
    RIFTBLADE,
    VOID_ASPECT_BLADE,
    RIFTBREAKER,
    SOVEREIGN_ASPECT,
    VOIDFANG_DAGGER,
    WARP_BOW,
    VOIDSHOT_BOW,
    RIFTSTORM_BOW,
    ORBITAL_LONGBOW,
    DRAGON_HUNTER_SHORTBOW,

    // Mage Weapons - Staffs
    ARCANE_STAFF,
    FROSTBITE_STAFF,
    INFERNO_STAFF,
    STORMCALLER_STAFF,
    VOIDWALKER_STAFF,
    CELESTIAL_STAFF,

    // Mage Weapons - Wands
    FLAME_WAND,
    ICE_WAND,
    LIGHTNING_WAND,
    POISON_WAND,
    HEALING_WAND,

    // Mage Weapons - Scepters
    SCEPTER_OF_HEALING,
    SCEPTER_OF_DECAY,
    SCEPTER_OF_MENDING;

    public static CustomWeaponType parse(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String normalized = input.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        for (CustomWeaponType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
