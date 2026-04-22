package io.papermc.Grivience.item;

import java.util.Locale;

public enum RaijinCraftingItemType {
    STORM_SIGIL,
    THUNDER_ESSENCE,
    RAIJIN_CORE,
    SUMMONING_EYE,
    ASCENSION_SHARD,
    DRAGON_HEART,
    DRAGONS_SPINE,
    DRAGON_SCALE,
    BLOSSOM_FIBER,
    ARCANE_CLOTH,
    SOULTHREAD,
    STAR_PEARL,
    GUARDIAN_FRAGMENT,
    GOD_POTION;

    public static RaijinCraftingItemType parse(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String normalized = input.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        for (RaijinCraftingItemType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
