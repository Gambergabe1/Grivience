package io.papermc.Grivience.item;

import java.util.Locale;

public enum MiningItemType {
    IRONCREST_DRILL,
    TITANIUM_DRILL,
    GEMSTONE_DRILL,
    PROSPECTOR_COMPASS,
    STABILITY_ANCHOR,
    MINING_XP_SCROLL,
    ORE_FRAGMENT_BUNDLE,
    TEMP_MINING_SPEED_BOOST,
    VOLTA,
    OIL_BARREL,
    MITHRIL_ENGINE,
    TITANIUM_ENGINE,
    GEMSTONE_ENGINE,
    DIVAN_ENGINE,
    MEDIUM_FUEL_TANK,
    LARGE_FUEL_TANK,
    SAPPHIRE,
    ENCHANTED_SAPPHIRE,
    TITANIUM,
    ENCHANTED_TITANIUM,
    TITANIUM_BLOCK,
    ENCHANTED_TITANIUM_BLOCK,
    ENCHANTED_REDSTONE,
    ENCHANTED_REDSTONE_BLOCK,
    ENCHANTED_COBBLESTONE;

    public static MiningItemType parse(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String normalized = input.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        for (MiningItemType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
