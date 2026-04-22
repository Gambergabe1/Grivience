package io.papermc.Grivience.collections;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a reward for reaching a collection milestone.
 * Skyblock accurate reward system.
 *
 * Reward Types:
 * - SKYBLOCK_XP: Grants Skyblock XP (typically +4 per tier)
 * - SKILL_XP: Grants skill-specific XP (Farming, Mining, Combat, etc.)
 * - ITEM: Grants an item or recipe unlock
 * - COMMAND: Executes commands (for custom rewards)
 * - STAT_BONUS: Permanent stat bonuses (EXP discounts, etc.)
 * - TRADE_UNLOCK: Unlocks NPC trades
 * - RECIPE_UNLOCK: Unlocks crafting recipes
 */
public class CollectionReward {
    private final RewardType type;
    private final double amount;
    private final String skillType;
    private final ItemStack item;
    private final List<String> commands;
    private final String statBonus;
    private final double statValue;
    private final String description;
    private final int skyblockXp;

    private CollectionReward(Builder builder) {
        this.type = builder.type;
        this.amount = builder.amount;
        this.skillType = CollectionTextUtil.sanitizeDisplayText(builder.skillType);
        this.item = builder.item;
        this.commands = builder.commands;
        this.statBonus = CollectionTextUtil.sanitizeDisplayText(builder.statBonus);
        this.statValue = builder.statValue;
        this.description = CollectionTextUtil.sanitizeDisplayText(builder.description);
        this.skyblockXp = builder.skyblockXp;
    }

    public RewardType getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public String getSkillType() {
        return skillType;
    }

    public ItemStack getItem() {
        return item;
    }

    public List<String> getCommands() {
        return commands;
    }

    public String getStatBonus() {
        return statBonus;
    }

    public double getStatValue() {
        return statValue;
    }

    public String getDescription() {
        return description;
    }

    public int getSkyblockXp() {
        return skyblockXp;
    }

    /**
     * Get formatted lore line for this reward.
     */
    public String getFormattedLore() {
        return switch (type) {
            case SKYBLOCK_XP -> ChatColor.GRAY + "+" + ChatColor.AQUA + skyblockXp + " " + ChatColor.DARK_AQUA + "Skyblock XP";
            case SKILL_XP -> ChatColor.GRAY + "+" + ChatColor.GREEN + (int) amount + " " + ChatColor.GREEN + skillType + " XP";
            case ITEM -> ChatColor.YELLOW + "Unlock: " + ChatColor.GRAY + fallbackDescription("Item");
            case COMMAND -> ChatColor.LIGHT_PURPLE + fallbackDescription("Custom Reward");
            case STAT_BONUS -> ChatColor.GRAY + statBonus + ": " + ChatColor.GREEN + "-" + (int) statValue + "% EXP Cost";
            case TRADE_UNLOCK -> ChatColor.GOLD + "Unlock Trade: " + ChatColor.GRAY + fallbackDescription("NPC Trade");
            case RECIPE_UNLOCK -> ChatColor.YELLOW + "Unlock Recipe: " + ChatColor.GRAY + fallbackDescription("Crafting Recipe");
        };
    }

    private String fallbackDescription(String fallback) {
        return (description == null || description.isBlank()) ? fallback : description;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private RewardType type = RewardType.ITEM;
        private double amount = 0;
        private String skillType = "";
        private ItemStack item = null;
        private List<String> commands = new ArrayList<>();
        private String statBonus = "";
        private double statValue = 0;
        private String description = "";
        private int skyblockXp = 4;

        public Builder type(RewardType type) {
            this.type = type;
            return this;
        }

        public Builder amount(double amount) {
            this.amount = amount;
            return this;
        }

        public Builder skillType(String skillType) {
            this.skillType = skillType;
            return this;
        }

        public Builder item(ItemStack item) {
            this.item = item;
            return this;
        }

        public Builder commands(List<String> commands) {
            this.commands = commands;
            return this;
        }

        public Builder statBonus(String statBonus) {
            this.statBonus = statBonus;
            return this;
        }

        public Builder statValue(double statValue) {
            this.statValue = statValue;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder skyblockXp(int skyblockXp) {
            this.skyblockXp = skyblockXp;
            return this;
        }

        public CollectionReward build() {
            return new CollectionReward(this);
        }
    }

    public enum RewardType {
        SKYBLOCK_XP,
        SKILL_XP,
        ITEM,
        COMMAND,
        STAT_BONUS,
        TRADE_UNLOCK,
        RECIPE_UNLOCK
    }
}
