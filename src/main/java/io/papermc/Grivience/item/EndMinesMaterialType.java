package io.papermc.Grivience.item;

import java.util.Locale;

/**
 * Custom materials used by the End Mines expansion.
 */
public enum EndMinesMaterialType {
    KUNZITE,
    ENDSTONE_SHARD,
    RIFT_ESSENCE,
    VOID_CRYSTAL,
    OBSIDIAN_CORE,
    CHORUS_WEAVE,
    ORE_FRAGMENT;

    public static EndMinesMaterialType parse(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String normalized = input.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        for (EndMinesMaterialType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
