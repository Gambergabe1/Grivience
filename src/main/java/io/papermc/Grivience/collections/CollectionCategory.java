package io.papermc.Grivience.collections;

import org.bukkit.Material;

/**
 * Collection categories matching Skyblock.
 * Each category groups related collections together.
 */
public enum CollectionCategory {
    FARMING("§eFarming", Material.WHEAT, "Farming items and animal products"),
    MINING("§6Mining", Material.COAL_ORE, "Ores, gems, and mining resources"),
    COMBAT("§cCombat", Material.DIAMOND_SWORD, "Mob drops and combat materials"),
    FORAGING("§2Foraging", Material.OAK_LOG, "Wood and foraging resources"),
    FISHING("§bFishing", Material.FISHING_ROD, "Fish and fishing treasures"),
    BOSS("§4Boss", Material.NETHER_STAR, "Dungeon boss and Kuudra drops"),
    SPECIAL("§dSpecial", Material.ENCHANTED_BOOK, "Special and event items"),
    SKILL("§aSkill", Material.EXPERIENCE_BOTTLE, "Skill-specific items");

    private final String displayName;
    private final Material icon;
    private final String description;

    CollectionCategory(String displayName, Material icon, String description) {
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public String getDescription() {
        return description;
    }
}

