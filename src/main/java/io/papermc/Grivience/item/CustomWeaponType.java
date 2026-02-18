package io.papermc.Grivience.item;

import java.util.Locale;

public enum CustomWeaponType {
    ONI_CLEAVER,
    TENGU_GALEBLADE,
    TENGU_STORMBOW,
    KAPPA_TIDEBREAKER,
    ONRYO_SPIRITBLADE,
    JOROGUMO_STINGER,
    KITSUNE_FANG,
    KITSUNE_DAWNBOW,
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
