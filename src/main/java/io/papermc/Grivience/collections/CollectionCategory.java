package io.papermc.Grivience.collections;

import org.bukkit.ChatColor;
import org.bukkit.Material;

/**
 * Collection categories matching Skyblock.
 * Each category groups related collections together.
 */
public enum CollectionCategory {
    FARMING(ChatColor.YELLOW + "Farming", Material.WHEAT, "Farming items and animal products"),
    MINING(ChatColor.GOLD + "Mining", Material.COAL_ORE, "Ores, gems, and mining resources"),
    COMBAT(ChatColor.RED + "Combat", Material.DIAMOND_SWORD, "Mob drops and combat materials"),
    FORAGING(ChatColor.DARK_GREEN + "Foraging", Material.OAK_LOG, "Wood and foraging resources"),
    FISHING(ChatColor.AQUA + "Fishing", Material.FISHING_ROD, "Fish and fishing treasures"),
    BOSS(ChatColor.DARK_RED + "Boss", Material.NETHER_STAR, "Dungeon boss and Kuudra drops"),
    SPECIAL(ChatColor.LIGHT_PURPLE + "Special", Material.ENCHANTED_BOOK, "Special and event items"),
    SKILL(ChatColor.GREEN + "Skill", Material.EXPERIENCE_BOTTLE, "Skill-specific items");

    private final String displayName;
    private final Material icon;
    private final String description;

    CollectionCategory(String displayName, Material icon, String description) {
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
    }

    public String getDisplayName() {
        return CollectionTextUtil.sanitizeDisplayText(displayName);
    }

    public Material getIcon() {
        return icon;
    }

    public String getDescription() {
        return CollectionTextUtil.sanitizeDisplayText(description);
    }
}
