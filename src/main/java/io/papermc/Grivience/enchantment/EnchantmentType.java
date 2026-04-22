package io.papermc.Grivience.enchantment;

import org.bukkit.ChatColor;

/**
 * Represents the type/rarity of an enchantment in Skyblock style.
 */
public enum EnchantmentType {
    COMMON(ChatColor.WHITE, "Common", 1),
    UNCOMMON(ChatColor.GREEN, "Uncommon", 2),
    RARE(ChatColor.BLUE, "Rare", 3),
    EPIC(ChatColor.DARK_PURPLE, "Epic", 4),
    LEGENDARY(ChatColor.GOLD, "Legendary", 5),
    MYTHIC(ChatColor.LIGHT_PURPLE, "Mythic", 6),
    DRAGON_TRACKER(ChatColor.LIGHT_PURPLE, "Dragon Tracker", 6),
    SPECIAL(ChatColor.AQUA, "Special", 7),
    VERY_SPECIAL(ChatColor.DARK_AQUA, "Very Special", 8);

    private final ChatColor color;
    private final String displayName;
    private final int tier;

    EnchantmentType(ChatColor color, String displayName, int tier) {
        this.color = color;
        this.displayName = displayName;
        this.tier = tier;
    }

    public ChatColor getColor() {
        return color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getTier() {
        return tier;
    }
}

