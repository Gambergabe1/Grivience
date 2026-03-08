package io.papermc.Grivience.welcome.quest;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a welcome quest in the quest line.
 */
public class WelcomeQuest {
    private final String id;
    private final String name;
    private final String description;
    private final QuestTier tier;
    private final QuestType type;
    private final int requiredAmount;
    private final List<String> rewards;
    private final int moneyReward;
    private final int xpReward;
    private final ItemStack icon;
    private final boolean repeatable;
    private final String previousQuestId; // Prerequisite quest

    public WelcomeQuest(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.description = builder.description;
        this.tier = builder.tier;
        this.type = builder.type;
        this.requiredAmount = builder.requiredAmount;
        this.rewards = builder.rewards;
        this.moneyReward = builder.moneyReward;
        this.xpReward = builder.xpReward;
        this.icon = builder.icon;
        this.repeatable = builder.repeatable;
        this.previousQuestId = builder.previousQuestId;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public QuestTier getTier() {
        return tier;
    }

    public QuestType getType() {
        return type;
    }

    public int getRequiredAmount() {
        return requiredAmount;
    }

    public List<String> getRewards() {
        return rewards;
    }

    public int getMoneyReward() {
        return moneyReward;
    }

    public int getXpReward() {
        return xpReward;
    }

    public ItemStack getIcon() {
        return icon != null ? icon.clone() : new ItemStack(Material.BOOK);
    }

    public boolean isRepeatable() {
        return repeatable;
    }

    public String getPreviousQuestId() {
        return previousQuestId;
    }

    /**
     * Get the formatted display name.
     */
    public String getDisplayName() {
        return tier.getColor().toString() + ChatColor.BOLD + name;
    }

    /**
     * Get the formatted lore for the quest.
     */
    public List<String> getFormattedLore(int progress) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + description);
        lore.add("");
        lore.add(ChatColor.GOLD + "" + ChatColor.BOLD + "Objective:");
        lore.add(ChatColor.GRAY + "Progress: " + ChatColor.GREEN + progress + "/" + requiredAmount);
        lore.add("");

        if (moneyReward > 0) {
            lore.add(ChatColor.GOLD + "Money Reward: " + ChatColor.GREEN + "$" + moneyReward);
        }
        if (xpReward > 0) {
            lore.add(ChatColor.AQUA + "XP Reward: " + ChatColor.GREEN + xpReward + " XP");
        }
        if (!rewards.isEmpty()) {
            lore.add(ChatColor.GREEN + "Item Rewards:");
            for (String reward : rewards) {
                lore.add(ChatColor.GRAY + "  • " + reward);
            }
        }
        lore.add("");

        if (previousQuestId != null) {
            lore.add(ChatColor.RED + "Requires: Complete previous quest first");
        } else {
            lore.add(ChatColor.GREEN + "Available");
        }

        return lore;
    }

    /**
     * Check if the quest is complete.
     */
    public boolean isComplete(int progress) {
        return progress >= requiredAmount;
    }

    public static Builder builder(String id, String name) {
        return new Builder(id, name);
    }

    public static class Builder {
        private final String id;
        private final String name;
        private String description = "Complete this quest";
        private QuestTier tier = QuestTier.BEGINNER;
        private QuestType type = QuestType.MINE_BLOCKS;
        private int requiredAmount = 1;
        private List<String> rewards = new ArrayList<>();
        private int moneyReward = 0;
        private int xpReward = 0;
        private ItemStack icon = new ItemStack(Material.BOOK);
        private boolean repeatable = false;
        private String previousQuestId;

        public Builder(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder tier(QuestTier tier) {
            this.tier = tier;
            return this;
        }

        public Builder type(QuestType type) {
            this.type = type;
            return this;
        }

        public Builder requiredAmount(int amount) {
            this.requiredAmount = amount;
            return this;
        }

        public Builder rewards(List<String> rewards) {
            this.rewards = new ArrayList<>(rewards);
            return this;
        }

        public Builder moneyReward(int amount) {
            this.moneyReward = amount;
            return this;
        }

        public Builder xpReward(int amount) {
            this.xpReward = amount;
            return this;
        }

        public Builder icon(ItemStack icon) {
            this.icon = icon != null ? icon.clone() : new ItemStack(Material.BOOK);
            return this;
        }

        public Builder icon(Material material) {
            this.icon = new ItemStack(material);
            return this;
        }

        public Builder repeatable(boolean repeatable) {
            this.repeatable = repeatable;
            return this;
        }

        public Builder previousQuestId(String previousQuestId) {
            this.previousQuestId = previousQuestId;
            return this;
        }

        public WelcomeQuest build() {
            return new WelcomeQuest(this);
        }
    }
}
