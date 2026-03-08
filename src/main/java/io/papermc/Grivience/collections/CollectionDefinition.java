package io.papermc.Grivience.collections;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a collection definition.
 * A collection tracks one or more item types and has multiple tiers with rewards.
 * 
 * Skyblock accurate structure:
 * - Each collection tracks specific items (e.g., Wheat, Diamond, Rotten Flesh)
 * - Multiple item types can be in one collection
 * - Tiers range from I to XI (some up to XVI)
 * - Each tier grants rewards and Skyblock XP
 */
public class CollectionDefinition {
    private final String id;
    private final String name;
    private final String description;
    private final Material icon;
    private final CollectionCategory category;
    private final String subcategory;
    private final List<String> trackedItems;
    private final List<CollectionTier> tiers;
    private final long totalAmountRequired;
    private final boolean enabled;

    public CollectionDefinition(
            String id,
            String name,
            String description,
            Material icon,
            CollectionCategory category,
            String subcategory,
            List<String> trackedItems,
            List<CollectionTier> tiers,
            boolean enabled
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.category = category;
        this.subcategory = subcategory == null ? "" : subcategory;
        this.trackedItems = new ArrayList<>(trackedItems);
        this.tiers = new ArrayList<>(tiers);
        this.enabled = enabled;

        // Calculate total amount required for max
        long total = 0;
        if (!tiers.isEmpty()) {
            total = tiers.get(tiers.size() - 1).getAmountRequired();
        }
        this.totalAmountRequired = total;
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

    public Material getIcon() {
        return icon;
    }

    public CollectionCategory getCategory() {
        return category;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public List<String> getTrackedItems() {
        return trackedItems;
    }

    public List<CollectionTier> getTiers() {
        return tiers;
    }

    public long getTotalAmountRequired() {
        return totalAmountRequired;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Check if an item ID is tracked by this collection.
     */
    public boolean tracksItem(String itemId) {
        return trackedItems.contains(itemId);
    }

    /**
     * Get the tier that corresponds to a collected amount.
     */
    public CollectionTier getTierForAmount(long collectedAmount) {
        CollectionTier highestUnlocked = null;
        for (CollectionTier tier : tiers) {
            if (collectedAmount >= tier.getAmountRequired()) {
                highestUnlocked = tier;
            } else {
                break;
            }
        }
        return highestUnlocked;
    }

    /**
     * Get the next tier that hasn't been unlocked yet.
     */
    public CollectionTier getNextTier(long collectedAmount) {
        for (CollectionTier tier : tiers) {
            if (collectedAmount < tier.getAmountRequired()) {
                return tier;
            }
        }
        return null;
    }

    /**
     * Check if collection is maxed at given amount.
     */
    public boolean isMaxed(long collectedAmount) {
        return collectedAmount >= totalAmountRequired;
    }

    /**
     * Get total Skyblock XP available from this collection.
     */
    public int getTotalSkyblockXp() {
        return tiers.stream()
            .mapToInt(CollectionTier::getTotalSkyblockXp)
            .sum();
    }

    /**
     * Get current tier level (0 if none unlocked).
     */
    public int getCurrentTierLevel(long collectedAmount) {
        int level = 0;
        for (CollectionTier tier : tiers) {
            if (collectedAmount >= tier.getAmountRequired()) {
                level = tier.getTierLevel();
            } else {
                break;
            }
        }
        return level;
    }

    /**
     * Create a display ItemStack for this collection.
     */
    public ItemStack createDisplayItem(long collectedAmount) {
        ItemStack display = new ItemStack(icon != null ? icon : Material.BARRIER);
        display.setAmount(1);

        var meta = display.getItemMeta();
        if (meta == null) return display;

        meta.setDisplayName("§6" + name);

        List<String> lore = new ArrayList<>();
        lore.add("§7" + category.getDisplayName());
        if (description != null && !description.isEmpty()) {
            lore.add("§8" + description);
        }
        lore.add("");

        // Collection progress
        int currentTierLevel = getCurrentTierLevel(collectedAmount);
        CollectionTier currentTier = getTierForAmount(collectedAmount);
        int maxTierLevel = tiers.isEmpty() ? 0 : tiers.get(tiers.size() - 1).getTierLevel();
        
        if (currentTierLevel > 0) {
            lore.add("§eTier " + CollectionTier.toRoman(currentTierLevel));
        }

        if (currentTierLevel < maxTierLevel) {
            CollectionTier nextTier = tiers.get(currentTierLevel); // tiers is 0-indexed, currentTierLevel is 1-indexed
            double percent = (double) collectedAmount / nextTier.getAmountRequired() * 100.0;
            if (percent > 100.0) percent = 100.0;

            lore.add("§7Progress to Tier " + CollectionTier.toRoman(nextTier.getTierLevel()) + ": §e" + String.format("%.1f%%", percent));
            lore.add(createProgressBar(percent / 100.0, 20));
            lore.add("§e" + formatNumber(collectedAmount) + "§7/§e" + formatNumber(nextTier.getAmountRequired()));
            lore.add("");
            lore.add("§aTier " + CollectionTier.toRoman(nextTier.getTierLevel()) + " Rewards:");
            for (CollectionReward reward : nextTier.getRewards()) {
                lore.add("  " + reward.getFormattedLore());
            }
        } else {
            lore.add("§a§lMAXED OUT!");
            lore.add("§e" + formatNumber(collectedAmount) + " §7items collected");
        }

        lore.add("");
        lore.add("§eClick to view details!");

        meta.setLore(lore);
        display.setItemMeta(meta);

        return display;
    }

    private String createProgressBar(double percent, int length) {
        int filled = (int) (percent * length);
        if (filled < 0) filled = 0;
        if (filled > length) filled = length;
        int empty = length - filled;

        StringBuilder sb = new StringBuilder("§2");
        for (int i = 0; i < filled; i++) {
            sb.append("-");
        }
        sb.append("§f");
        for (int i = 0; i < empty; i++) {
            sb.append("-");
        }
        return sb.toString();
    }

    private String formatNumber(long amount) {
        if (amount >= 1_000_000_000) {
            return String.format("%.1fB", amount / 1_000_000_000.0);
        } else if (amount >= 1_000_000) {
            return String.format("%.1fM", amount / 1_000_000.0);
        } else if (amount >= 1_000) {
            return String.format("%.1fK", amount / 1_000.0);
        } else {
            return String.valueOf(amount);
        }
    }

    /**
     * Builder for CollectionDefinition.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String name;
        private String description = "";
        private Material icon = Material.BARRIER;
        private CollectionCategory category = CollectionCategory.SPECIAL;
        private String subcategory = "";
        private List<String> trackedItems = new ArrayList<>();
        private List<CollectionTier> tiers = new ArrayList<>();
        private boolean enabled = true;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder icon(Material icon) {
            this.icon = icon;
            return this;
        }

        public Builder category(CollectionCategory category) {
            this.category = category;
            return this;
        }

        public Builder subcategory(String subcategory) {
            this.subcategory = subcategory;
            return this;
        }

        public Builder trackedItems(List<String> trackedItems) {
            this.trackedItems = trackedItems;
            return this;
        }

        public Builder tiers(List<CollectionTier> tiers) {
            this.tiers = tiers;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public CollectionDefinition build() {
            return new CollectionDefinition(
                id, name, description, icon, category, subcategory, trackedItems, tiers, enabled
            );
        }
    }
}

