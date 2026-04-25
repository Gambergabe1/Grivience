package io.papermc.Grivience.item;

import java.util.Locale;

public enum CustomToolType {
    // Pickaxes
    IRONCREST_PICKAXE,
    VOID_STEEL_PICKAXE,
    TITAN_BREAKER,
    
    // Farming Tools
    GILDED_HOE,
    NEWTONIAN_HOE,
    GAIA_SCYTHE,
    MATHEMATICAL_HOE_BLUEPRINT,
    EUCLIDS_WHEAT_HOE,
    GAUSS_CARROT_HOE,
    PYTHAGOREAN_POTATO_HOE,
    TURING_SUGAR_CANE_HOE,
    NEWTON_NETHER_WARTS_HOE,
    MELON_DICER,
    PUMPKIN_DICER;

    public static CustomToolType parse(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String normalized = input.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        for (CustomToolType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
