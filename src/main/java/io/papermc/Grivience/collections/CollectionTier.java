package io.papermc.Grivience.collections;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single tier/milestone within a collection.
 * Each tier has a required amount and associated rewards.
 *
 * Skyblock accurate tier structure:
 * - Tier I-XI (some go up to XVI)
 * - Each tier typically grants +4 Skyblock XP
 * - Rewards include recipes, trades, items, skill XP, stat bonuses
 */
public class CollectionTier {
    private final int tierLevel;
    private final long amountRequired;
    private final List<CollectionReward> rewards;
    private final boolean isMaxed;

    public CollectionTier(int tierLevel, long amountRequired, List<CollectionReward> rewards) {
        this.tierLevel = tierLevel;
        this.amountRequired = amountRequired;
        this.rewards = new ArrayList<>(rewards);
        this.isMaxed = false;
    }

    public CollectionTier(int tierLevel, long amountRequired, List<CollectionReward> rewards, boolean isMaxed) {
        this.tierLevel = tierLevel;
        this.amountRequired = amountRequired;
        this.rewards = new ArrayList<>(rewards);
        this.isMaxed = isMaxed;
    }

    public int getTierLevel() {
        return tierLevel;
    }

    public long getAmountRequired() {
        return amountRequired;
    }

    public List<CollectionReward> getRewards() {
        return rewards;
    }

    public boolean isMaxed() {
        return isMaxed;
    }

    /**
     * Get Roman numeral representation of tier level.
     */
    public String getTierRoman() {
        return toRoman(tierLevel);
    }

    /**
     * Get formatted tier display name.
     */
    public String getDisplayName() {
        return ChatColor.GOLD + "Tier " + getTierRoman();
    }

    /**
     * Get total Skyblock XP from this tier's rewards.
     */
    public int getTotalSkyblockXp() {
        return rewards.stream()
                .filter(r -> r.getType() == CollectionReward.RewardType.SKYBLOCK_XP)
                .mapToInt(CollectionReward::getSkyblockXp)
                .sum();
    }

    /**
     * Check if a player has unlocked this tier.
     */
    public boolean isUnlocked(long collectedAmount) {
        return collectedAmount >= amountRequired;
    }

    /**
     * Get progress percentage toward this tier.
     */
    public double getProgress(long collectedAmount) {
        if (collectedAmount >= amountRequired) {
            return 100.0;
        }
        return (collectedAmount * 100.0) / amountRequired;
    }

    /**
     * Get formatted progress bar.
     */
    public String getProgressBar(long collectedAmount, int length) {
        return CollectionTextUtil.createProgressBar(getProgress(collectedAmount), length);
    }

    /**
     * Convert integer to Roman numeral.
     */
    public static String toRoman(int num) {
        if (num <= 0) {
            return String.valueOf(num);
        }

        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] roman = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            while (num >= values[i]) {
                num -= values[i];
                result.append(roman[i]);
            }
        }
        return result.toString();
    }
}
