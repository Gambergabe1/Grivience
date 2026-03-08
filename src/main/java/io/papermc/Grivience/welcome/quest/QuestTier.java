package io.papermc.Grivience.welcome.quest;

import org.bukkit.ChatColor;

/**
 * Represents the tier/difficulty of a welcome quest.
 */
public enum QuestTier {
    BEGINNER(ChatColor.WHITE, "Beginner", 1),
    INTERMEDIATE(ChatColor.GREEN, "Intermediate", 2),
    ADVANCED(ChatColor.AQUA, "Advanced", 3),
    EXPERT(ChatColor.GOLD, "Expert", 4),
    MASTER(ChatColor.LIGHT_PURPLE, "Master", 5);

    private final ChatColor color;
    private final String displayName;
    private final int tier;

    QuestTier(ChatColor color, String displayName, int tier) {
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
