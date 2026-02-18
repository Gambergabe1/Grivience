package io.papermc.Grivience.item;

import java.util.Locale;

public enum ItemStyle {
    JAPANESE,
    SKYBLOCK,
    MINIMAL;

    public static ItemStyle parse(String input) {
        if (input == null || input.isBlank()) {
            return JAPANESE;
        }
        String normalized = input.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        for (ItemStyle style : values()) {
            if (style.name().equals(normalized)) {
                return style;
            }
        }
        return JAPANESE;
    }
}
