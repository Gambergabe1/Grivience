package io.papermc.Grivience.item;

import java.util.Locale;

public enum CustomWeaponType {
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
    FLYING_RAIJIN;

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
