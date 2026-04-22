package io.papermc.Grivience.accessory;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.stats.SkyblockLevelManager;
import io.papermc.Grivience.storage.StorageManager;
import io.papermc.Grivience.storage.StorageProfile;
import io.papermc.Grivience.storage.StorageType;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Manages accessory bag slot scaling based on Skyblock Level.
 * Similar to Hypixel Skyblock's system where higher SB level = more bag slots.
 */
public final class AccessoryBagSlotManager {
    // Base slots at level 1
    private static final int BASE_SLOTS = 9;

    // Slots unlocked per level milestone
    private static final int[] LEVEL_MILESTONES = {
            0,   // Level 0
            9,   // Level 1-4: 9 slots (base)
            18,  // Level 5-9: +9 slots
            27,  // Level 10-14: +9 slots
            36,  // Level 15-19: +9 slots
            45,  // Level 20-29: +9 slots
            54,  // Level 30-39: +9 slots
            63,  // Level 40-49: +9 slots
            72,  // Level 50-59: +9 slots
            81,  // Level 60-79: +9 slots
            90,  // Level 80-99: +9 slots
            99,  // Level 100-149: +9 slots
            108, // Level 150-199: +9 slots
            117, // Level 200-249: +9 slots
            126, // Level 250-299: +9 slots
            135, // Level 300+: +9 slots
    };

    private final GriviencePlugin plugin;
    private final SkyblockLevelManager levelManager;
    private final StorageManager storageManager;

    public AccessoryBagSlotManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.levelManager = plugin.getSkyblockLevelManager();
        this.storageManager = plugin.getStorageManager();
    }

    /**
     * Calculate maximum accessory bag slots for a player based on their Skyblock Level.
     */
    public int calculateMaxSlots(Player player) {
        if (player == null || levelManager == null) {
            return BASE_SLOTS;
        }

        int sbLevel = levelManager.getLevel(player);
        return calculateMaxSlotsForLevel(sbLevel);
    }

    /**
     * Calculate maximum accessory bag slots for a specific Skyblock Level.
     */
    public int calculateMaxSlotsForLevel(int sbLevel) {
        if (sbLevel < 1) {
            return BASE_SLOTS;
        }

        // Determine which milestone tier the player is in
        int tier;
        if (sbLevel < 5) tier = 1;
        else if (sbLevel < 10) tier = 2;
        else if (sbLevel < 15) tier = 3;
        else if (sbLevel < 20) tier = 4;
        else if (sbLevel < 30) tier = 5;
        else if (sbLevel < 40) tier = 6;
        else if (sbLevel < 50) tier = 7;
        else if (sbLevel < 60) tier = 8;
        else if (sbLevel < 80) tier = 9;
        else if (sbLevel < 100) tier = 10;
        else if (sbLevel < 150) tier = 11;
        else if (sbLevel < 200) tier = 12;
        else if (sbLevel < 250) tier = 13;
        else if (sbLevel < 300) tier = 14;
        else tier = 15;

        return tier < LEVEL_MILESTONES.length ? LEVEL_MILESTONES[tier] : LEVEL_MILESTONES[LEVEL_MILESTONES.length - 1];
    }

    /**
     * Check and update a player's accessory bag slots based on their current level.
     * Called when a player levels up or logs in.
     */
    public void updatePlayerSlots(Player player) {
        if (player == null || storageManager == null) {
            return;
        }

        int maxSlots = calculateMaxSlots(player);
        StorageProfile bag = storageManager.getStorage(player, StorageType.ACCESSORY_BAG);
        if (bag == null) {
            return;
        }

        int currentCapacity = bag.getCurrentSlots();
        if (maxSlots > currentCapacity) {
            bag.setCurrentSlots(maxSlots);
            int newSlots = maxSlots - currentCapacity;
            player.sendMessage(ChatColor.GREEN + "✦ Your Accessory Bag has been expanded to " + ChatColor.AQUA + maxSlots + ChatColor.GREEN + " slots! (+" + newSlots + " from Skyblock Level)");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            storageManager.markDirty(player.getUniqueId());
        }
    }

    /**
     * Get the next slot unlock level for a player.
     * Returns -1 if player has max slots.
     */
    public int getNextUnlockLevel(Player player) {
        if (player == null || levelManager == null) {
            return -1;
        }

        int currentLevel = levelManager.getLevel(player);
        int currentSlots = calculateMaxSlots(player);

        // Find next milestone
        int[] checkLevels = {5, 10, 15, 20, 30, 40, 50, 60, 80, 100, 150, 200, 250, 300};
        for (int checkLevel : checkLevels) {
            if (currentLevel < checkLevel) {
                int slotsAtLevel = calculateMaxSlotsForLevel(checkLevel);
                if (slotsAtLevel > currentSlots) {
                    return checkLevel;
                }
            }
        }

        return -1; // Max slots reached
    }

    /**
     * Get formatted info about current and next slot unlock.
     */
    public String getSlotInfo(Player player) {
        int currentSlots = calculateMaxSlots(player);
        int nextLevel = getNextUnlockLevel(player);

        if (nextLevel == -1) {
            return ChatColor.GREEN + "Slots: " + ChatColor.AQUA + currentSlots + ChatColor.DARK_GRAY + " (MAX)";
        }

        int nextSlots = calculateMaxSlotsForLevel(nextLevel);
        int slotsToGain = nextSlots - currentSlots;
        return ChatColor.GREEN + "Slots: " + ChatColor.AQUA + currentSlots + ChatColor.DARK_GRAY + " (+" + slotsToGain + " at SB Level " + nextLevel + ")";
    }
}
