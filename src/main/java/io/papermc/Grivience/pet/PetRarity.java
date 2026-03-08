package io.papermc.Grivience.pet;

import org.bukkit.ChatColor;

/**
 * Basic rarity definitions for pets. Provides simple colour tagging and XP scaling.
 */
public enum PetRarity {
    COMMON(ChatColor.WHITE),
    UNCOMMON(ChatColor.GREEN),
    RARE(ChatColor.BLUE),
    EPIC(ChatColor.DARK_PURPLE),
    LEGENDARY(ChatColor.GOLD),
    MYTHIC(ChatColor.LIGHT_PURPLE),
    SPECIAL(ChatColor.RED),
    VERY_SPECIAL(ChatColor.RED),
    DIVINE(ChatColor.AQUA);

    private final ChatColor color;

    PetRarity(ChatColor color) {
        this.color = color;
    }

    public ChatColor color() {
        return color;
    }
}
