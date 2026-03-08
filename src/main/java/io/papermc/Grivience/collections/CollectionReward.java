package io.papermc.Grivience.collections;

import org.bukkit.configuration.ConfigurationSection;
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
        this.skillType = builder.skillType;
        this.item = builder.item;
        this.commands = builder.commands;
        this.statBonus = builder.statBonus;
        this.statValue = builder.statValue;
        this.description = builder.description;
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
            case SKYBLOCK_XP -> "§7+§b" + skyblockXp + " §3Skyblock XP";
            case SKILL_XP -> "§7+§a" + (int) amount + " §a" + skillType + " XP";
            case ITEM -> "§eUnlock: §7" + (description != null ? description : "Item");
            case COMMAND -> "§d" + (description != null ? description : "Custom Reward");
            case STAT_BONUS -> "§7" + statBonus + ": §a-" + (int) statValue + "% EXP Cost";
            case TRADE_UNLOCK -> "§6Unlock Trade: §7" + (description != null ? description : "NPC Trade");
            case RECIPE_UNLOCK -> "§eUnlock Recipe: §7" + (description != null ? description : "Crafting Recipe");
        };
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

