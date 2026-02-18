package io.papermc.Grivience.item;

import org.bukkit.ChatColor;

import java.util.Locale;

public enum ReforgeType {
    JAGGED("Jagged", ChatColor.RED, 16.0D, 10.0D, 6.0D, 12.0D),
    TITANIC("Titanic", ChatColor.GOLD, 8.0D, 38.0D, 0.0D, 16.0D),
    ARCANE("Arcane", ChatColor.LIGHT_PURPLE, 0.0D, 12.0D, 10.0D, 34.0D),
    TEMPEST("Tempest", ChatColor.AQUA, 12.0D, 22.0D, 8.0D, 20.0D);

    private final String displayName;
    private final ChatColor color;
    private final double damageBonus;
    private final double strengthBonus;
    private final double critChanceBonus;
    private final double critDamageBonus;

    ReforgeType(
            String displayName,
            ChatColor color,
            double damageBonus,
            double strengthBonus,
            double critChanceBonus,
            double critDamageBonus
    ) {
        this.displayName = displayName;
        this.color = color;
        this.damageBonus = damageBonus;
        this.strengthBonus = strengthBonus;
        this.critChanceBonus = critChanceBonus;
        this.critDamageBonus = critDamageBonus;
    }

    public String displayName() {
        return displayName;
    }

    public ChatColor color() {
        return color;
    }

    public double damageBonus() {
        return damageBonus;
    }

    public double strengthBonus() {
        return strengthBonus;
    }

    public double critChanceBonus() {
        return critChanceBonus;
    }

    public double critDamageBonus() {
        return critDamageBonus;
    }

    public static ReforgeType parse(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String normalized = input.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        for (ReforgeType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
