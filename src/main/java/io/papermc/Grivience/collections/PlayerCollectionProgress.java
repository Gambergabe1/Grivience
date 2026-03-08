package io.papermc.Grivience.collections;

import org.bukkit.configuration.ConfigurationSection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks a player's progress in a specific collection.
 * Persists collected amount and unlocked tiers.
 */
public class PlayerCollectionProgress {
    private final UUID profileId;
    private final String collectionId;
    private long collectedAmount;
    private final Set<Integer> unlockedTiers;
    private final Map<UUID, Long> contributionsByMember;
    private boolean hasReceivedRewards;
    private long lastCollectionTime;
    private int totalSkyblockXpEarned;

    public PlayerCollectionProgress(UUID profileId, String collectionId) {
        this.profileId = profileId;
        this.collectionId = collectionId;
        this.collectedAmount = 0;
        this.unlockedTiers = ConcurrentHashMap.newKeySet();
        this.contributionsByMember = new ConcurrentHashMap<>();
        this.hasReceivedRewards = false;
        this.lastCollectionTime = 0;
        this.totalSkyblockXpEarned = 0;
    }

    public UUID getProfileId() {
        return profileId;
    }

    public String getCollectionId() {
        return collectionId;
    }

    public long getCollectedAmount() {
        return collectedAmount;
    }

    public Set<Integer> getUnlockedTiers() {
        return new HashSet<>(unlockedTiers);
    }

    public Map<UUID, Long> getContributionsByMember() {
        return new HashMap<>(contributionsByMember);
    }

    public boolean hasReceivedRewards() {
        return hasReceivedRewards;
    }

    public long getLastCollectionTime() {
        return lastCollectionTime;
    }

    public int getTotalSkyblockXpEarned() {
        return totalSkyblockXpEarned;
    }

    public void mergeFrom(PlayerCollectionProgress other) {
        if (other == null) {
            return;
        }
        this.collectedAmount = Math.max(this.collectedAmount, other.collectedAmount);
        this.unlockedTiers.addAll(other.unlockedTiers);
        other.contributionsByMember.forEach((memberId, amount) -> {
            if (memberId == null) {
                return;
            }
            long safe = Math.max(0L, amount == null ? 0L : amount);
            if (safe <= 0L) {
                return;
            }
            // Merge conservatively to avoid double-counting on migration paths.
            contributionsByMember.merge(memberId, safe, Math::max);
        });
        this.hasReceivedRewards = this.hasReceivedRewards || other.hasReceivedRewards;
        this.lastCollectionTime = Math.max(this.lastCollectionTime, other.lastCollectionTime);
        this.totalSkyblockXpEarned = Math.max(this.totalSkyblockXpEarned, other.totalSkyblockXpEarned);
    }

    /**
     * Add to collected amount.
     */
    public void addCollection(long amount) {
        this.collectedAmount += amount;
        this.lastCollectionTime = System.currentTimeMillis();
    }

    public void addContribution(UUID contributorId, long amount) {
        if (contributorId == null) {
            return;
        }
        if (amount <= 0L) {
            return;
        }
        contributionsByMember.merge(contributorId, amount, Long::sum);
    }

    /**
     * Set collected amount directly.
     */
    public void setCollectedAmount(long amount) {
        this.collectedAmount = amount;
        this.lastCollectionTime = System.currentTimeMillis();
    }

    /**
     * Unlock a tier.
     */
    public void unlockTier(int tierLevel) {
        this.unlockedTiers.add(tierLevel);
    }

    /**
     * Check if tier is unlocked.
     */
    public boolean isTierUnlocked(int tierLevel) {
        return unlockedTiers.contains(tierLevel);
    }

    /**
     * Mark rewards as received.
     */
    public void setRewardsReceived(boolean received) {
        this.hasReceivedRewards = received;
    }

    /**
     * Add Skyblock XP earned.
     */
    public void addSkyblockXp(int xp) {
        this.totalSkyblockXpEarned += xp;
    }

    /**
     * Get progress percentage to next tier.
     */
    public double getProgressToNext(CollectionDefinition collection) {
        if (collection.isMaxed(collectedAmount)) {
            return 100.0;
        }
        
        CollectionTier nextTier = collection.getNextTier(collectedAmount);
        if (nextTier == null) return 100.0;
        
        long prevAmount = 0;
        for (CollectionTier tier : collection.getTiers()) {
            if (tier.getTierLevel() < nextTier.getTierLevel()) {
                prevAmount = tier.getAmountRequired();
            }
        }
        
        long range = nextTier.getAmountRequired() - prevAmount;
        long progress = collectedAmount - prevAmount;
        
        return Math.min(100.0, (progress * 100.0) / range);
    }

    /**
     * Save to YAML configuration.
     */
    public void save(ConfigurationSection section) {
        section.set("collected-amount", collectedAmount);
        section.set("unlocked-tiers", new ArrayList<>(unlockedTiers));
        section.set("rewards-received", hasReceivedRewards);
        section.set("last-collection-time", lastCollectionTime);
        section.set("total-skyblock-xp", totalSkyblockXpEarned);

        if (!contributionsByMember.isEmpty()) {
            section.set("contributions", null);
            ConfigurationSection contrib = section.createSection("contributions");
            for (Map.Entry<UUID, Long> entry : contributionsByMember.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                long amount = Math.max(0L, entry.getValue());
                if (amount <= 0L) {
                    continue;
                }
                contrib.set(entry.getKey().toString(), amount);
            }
        }
    }

    /**
     * Load from YAML configuration.
     */
    public static PlayerCollectionProgress load(UUID playerId, String collectionId, ConfigurationSection section) {
        if (section == null) {
            return new PlayerCollectionProgress(playerId, collectionId);
        }

        PlayerCollectionProgress progress = new PlayerCollectionProgress(playerId, collectionId);
        progress.collectedAmount = section.getLong("collected-amount", 0);
        
        if (section.contains("unlocked-tiers")) {
            for (Object tier : section.getList("unlocked-tiers")) {
                if (tier instanceof Integer) {
                    progress.unlockedTiers.add((Integer) tier);
                }
            }
        }
        
        progress.hasReceivedRewards = section.getBoolean("rewards-received", false);
        progress.lastCollectionTime = section.getLong("last-collection-time", 0);
        progress.totalSkyblockXpEarned = section.getInt("total-skyblock-xp", 0);

        ConfigurationSection contributionsSection = section.getConfigurationSection("contributions");
        if (contributionsSection != null) {
            for (String key : contributionsSection.getKeys(false)) {
                if (key == null || key.isBlank()) {
                    continue;
                }
                UUID contributorId;
                try {
                    contributorId = UUID.fromString(key);
                } catch (Exception ignored) {
                    continue;
                }
                long contributed = contributionsSection.getLong(key, 0L);
                if (contributed > 0L) {
                    progress.contributionsByMember.put(contributorId, contributed);
                }
            }
        }
        
        return progress;
    }

    /**
     * Create a copy of this progress.
     */
    public PlayerCollectionProgress clone() {
        PlayerCollectionProgress copy = new PlayerCollectionProgress(profileId, collectionId);
        copy.collectedAmount = this.collectedAmount;
        copy.unlockedTiers.addAll(this.unlockedTiers);
        copy.contributionsByMember.putAll(this.contributionsByMember);
        copy.hasReceivedRewards = this.hasReceivedRewards;
        copy.lastCollectionTime = this.lastCollectionTime;
        copy.totalSkyblockXpEarned = this.totalSkyblockXpEarned;
        return copy;
    }
}

