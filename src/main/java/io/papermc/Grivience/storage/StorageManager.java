package io.papermc.Grivience.storage;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skyblock.economy.ProfileEconomyService;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main Storage System Manager.
 * Handles storage profiles, upgrades, persistence, and access control.
 *
 * Features:
 * - Multiple storage types (Personal, Vault, Ender, Backpack, Warehouse)
 * - Upgrade system with configurable costs
 * - Persistent storage per player profile
 * - Access control and permissions
 * - Storage locking mechanism
 * - Auto-save functionality
 * 
 * Integration:
 * - Works with Skyblock Profile System for profile-specific storage
 * - Each Skyblock profile has its own separate storage
 */
public class StorageManager {
    private final GriviencePlugin plugin;
    private final ProfileEconomyService profileEconomy;
    // Storage indexed by: owner UUID -> profile UUID -> storage type -> storage profile
    private final Map<UUID, Map<UUID, Map<StorageType, StorageProfile>>> playerProfileStorages;
    private final Map<StorageType, List<StorageUpgrade>> upgradeTrees;
    private final File storageDataFile;
    private final File upgradesConfigFile;
    private boolean enabled;
    private boolean autoSaveEnabled;
    private long autoSaveIntervalTicks;
    private int autoSaveTaskId;

    public StorageManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.profileEconomy = new ProfileEconomyService(plugin);
        this.playerProfileStorages = new ConcurrentHashMap<>();
        this.upgradeTrees = new EnumMap<>(StorageType.class);
        this.storageDataFile = new File(plugin.getDataFolder(), "storage_data.yml");
        this.upgradesConfigFile = new File(plugin.getDataFolder(), "storage_upgrades.yml");
        this.enabled = false;
        this.autoSaveEnabled = true;
        this.autoSaveIntervalTicks = 600L; // 30 seconds
    }

    /**
     * Load and enable the storage system.
     */
    public void load() {
        loadUpgradeConfig();
        loadPlayerData();
        enabled = true;

        if (autoSaveEnabled) {
            startAutoSave();
        }

        plugin.getLogger().info("Storage System loaded with " + upgradeTrees.size() + " storage types.");
    }

    /**
     * Disable the storage system.
     */
    public void disable() {
        if (autoSaveTaskId != -1) {
            Bukkit.getScheduler().cancelTask(autoSaveTaskId);
        }
        saveAllPlayerData();
        enabled = false;
    }

    /**
     * Reload storage configuration.
     */
    public void reload() {
        saveAllPlayerData();
        upgradeTrees.clear();
        loadUpgradeConfig();
        plugin.getLogger().info("Storage System reloaded.");
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get a player's storage profile for a specific type and profile.
     */
    public StorageProfile getStorage(UUID playerId, UUID profileId, StorageType type) {
        return playerProfileStorages
            .computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(profileId, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(type, k -> {
                StorageProfile profile = new StorageProfile(playerId, profileId, type);
                // Apply default upgrade tier 0 (base slots)
                profile.setCurrentSlots(type.getBaseSlots());
                return profile;
            });
    }

    /**
     * Get a player's storage profile for a specific type and Skyblock profile.
     */
    public StorageProfile getStorage(Player player, UUID profileId, StorageType type) {
        return getStorage(player.getUniqueId(), profileId, type);
    }

    /**
     * Get a player's storage profile for a specific type using their selected profile.
     */
    public StorageProfile getStorage(Player player, StorageType type) {
        SkyBlockProfile profile = getSelectedSkyBlockProfile(player);
        if (profile == null) {
            // Fallback to player UUID if no profile is selected
            return getStorage(player.getUniqueId(), player.getUniqueId(), type);
        }
        return getStorage(player.getUniqueId(), profile.getProfileId(), type);
    }

    /**
     * Get a player's storage profile for a specific type (legacy, uses player UUID as profile).
     * @deprecated Use {@link #getStorage(Player, UUID, StorageType)} for explicit profile access.
     */
    @Deprecated
    public StorageProfile getStorage(UUID playerId, StorageType type) {
        return getStorage(playerId, playerId, type);
    }

    /**
     * Get all storage profiles for a player's specific profile.
     */
    public Map<StorageType, StorageProfile> getAllStorages(UUID playerId, UUID profileId) {
        return new EnumMap<>(playerProfileStorages
            .computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(profileId, k -> new EnumMap<>(StorageType.class)));
    }

    /**
     * Get all storage profiles for a player's selected profile.
     */
    public Map<StorageType, StorageProfile> getAllStorages(Player player) {
        SkyBlockProfile profile = getSelectedSkyBlockProfile(player);
        if (profile == null) {
            return getAllStorages(player.getUniqueId(), player.getUniqueId());
        }
        return getAllStorages(player.getUniqueId(), profile.getProfileId());
    }

    /**
     * Get all storage profiles for a player (legacy, uses player UUID as profile).
     * @deprecated Use {@link #getAllStorages(Player)} for profile-aware access.
     */
    @Deprecated
    public Map<StorageType, StorageProfile> getAllStorages(UUID playerId) {
        return getAllStorages(playerId, playerId);
    }

    /**
     * Get the player's selected Skyblock profile.
     * Package-private for StorageListener access.
     */
    SkyBlockProfile getSelectedSkyBlockProfile(Player player) {
        io.papermc.Grivience.skyblock.profile.ProfileManager profileManager = plugin.getProfileManager();
        if (profileManager != null) {
            return profileManager.getSelectedProfile(player);
        }
        return null;
    }

    /**
     * Get upgrade tree for a storage type.
     */
    public List<StorageUpgrade> getUpgrades(StorageType type) {
        return upgradeTrees.getOrDefault(type, new ArrayList<>());
    }

    /**
     * Get the next available upgrade for a storage type.
     */
    public StorageUpgrade getNextUpgrade(StorageProfile profile) {
        List<StorageUpgrade> upgrades = getUpgrades(profile.getStorageType());
        int currentTier = profile.getUpgradeTier();

        for (StorageUpgrade upgrade : upgrades) {
            if (upgrade.getTier() > currentTier) {
                return upgrade;
            }
        }

        return null;
    }

    /**
     * Check if a player can upgrade their storage.
     */
    public boolean canUpgrade(Player player, StorageProfile profile) {
        StorageUpgrade nextUpgrade = getNextUpgrade(profile);
        if (nextUpgrade == null) {
            return false;
        }

        // Check if player has reached max slots
        if (profile.getCurrentSlots() >= profile.getStorageType().getMaxSlots()) {
            return false;
        }

        // Check permission
        if (!player.hasPermission(profile.getStorageType().getPermissionNode() + ".upgrade")) {
            return false;
        }

        return true;
    }

    /**
     * Upgrade a player's storage.
     * Returns true if successful.
     */
    public boolean upgradeStorage(Player player, StorageProfile profile) {
        if (!canUpgrade(player, profile)) {
            return false;
        }

        StorageUpgrade nextUpgrade = getNextUpgrade(profile);
        if (nextUpgrade == null) {
            return false;
        }

        // Check cost (if any)
        if (nextUpgrade.hasCost()) {
            if (profileEconomy.requireSelectedProfile(player) == null) {
                return false;
            }
            if (!profileEconomy.has(player, nextUpgrade.getCost())) {
                player.sendMessage("§cInsufficient funds! Need §e" + String.format(Locale.ROOT, "%,.0f", nextUpgrade.getCost()) + " coins");
                return false;
            }
            if (!profileEconomy.withdraw(player, nextUpgrade.getCost())) {
                player.sendMessage("§cFailed to charge upgrade cost.");
                return false;
            }
        }

        // Apply upgrade
        profile.setCurrentSlots(nextUpgrade.getSlots());
        profile.setUpgradeTier(nextUpgrade.getTier());

        // Execute upgrade commands
        for (String command : nextUpgrade.getCommands()) {
            String processed = command.replace("{player}", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processed);
        }

        // Notify player
        player.sendMessage("§a§lSTORAGE UPGRADED!");
        player.sendMessage("§7Upgraded to §e" + nextUpgrade.getDisplayName());
        player.sendMessage("§7New capacity: §e" + nextUpgrade.getSlots() + " slots");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        // Save immediately
        savePlayerData(player.getUniqueId());

        return true;
    }

    /**
     * Lock a player's storage.
     */
    public void lockStorage(UUID playerId, UUID profileId, StorageType type) {
        StorageProfile profile = getStorage(playerId, profileId, type);
        if (profile != null) {
            profile.setLocked(true);
        }
    }

    /**
     * Lock a player's storage (legacy, uses player UUID as profile).
     * @deprecated Use {@link #lockStorage(UUID, UUID, StorageType)} for explicit profile access.
     */
    @Deprecated
    public void lockStorage(UUID playerId, StorageType type) {
        lockStorage(playerId, playerId, type);
    }

    /**
     * Unlock a player's storage.
     */
    public void unlockStorage(UUID playerId, UUID profileId, StorageType type) {
        StorageProfile profile = getStorage(playerId, profileId, type);
        if (profile != null) {
            profile.setLocked(false);
        }
    }

    /**
     * Unlock a player's storage (legacy, uses player UUID as profile).
     * @deprecated Use {@link #unlockStorage(UUID, UUID, StorageType)} for explicit profile access.
     */
    @Deprecated
    public void unlockStorage(UUID playerId, StorageType type) {
        unlockStorage(playerId, playerId, type);
    }

    /**
     * Check if a player's storage is locked.
     */
    public boolean isStorageLocked(UUID playerId, UUID profileId, StorageType type) {
        StorageProfile profile = getStorage(playerId, profileId, type);
        return profile != null && profile.isLocked();
    }

    /**
     * Check if a player's storage is locked (legacy, uses player UUID as profile).
     * @deprecated Use {@link #isStorageLocked(UUID, UUID, StorageType)} for explicit profile access.
     */
    @Deprecated
    public boolean isStorageLocked(UUID playerId, StorageType type) {
        return isStorageLocked(playerId, playerId, type);
    }

    /**
     * Set a custom name for a storage.
     */
    public void setStorageName(Player player, StorageType type, String name) {
        StorageProfile profile = getStorage(player, type);
        if (profile != null) {
            profile.setCustomName(name);
            player.sendMessage("§aStorage renamed to §e" + name);
            savePlayerData(player.getUniqueId());
        }
    }

    /**
     * Get total items stored across all storage types for a player's selected profile.
     */
    public int getTotalItemsStored(Player player) {
        SkyBlockProfile profile = getSelectedSkyBlockProfile(player);
        if (profile == null) {
            return getTotalItemsStored(player.getUniqueId(), player.getUniqueId());
        }
        return getTotalItemsStored(player.getUniqueId(), profile.getProfileId());
    }

    /**
     * Get total items stored across all storage types for a player's specific profile.
     */
    public int getTotalItemsStored(UUID playerId, UUID profileId) {
        int total = 0;
        Map<UUID, Map<StorageType, StorageProfile>> playerData = playerProfileStorages.get(playerId);
        if (playerData != null) {
            Map<StorageType, StorageProfile> storages = playerData.get(profileId);
            if (storages != null) {
                for (StorageProfile storageProfile : storages.values()) {
                    total += storageProfile.getTotalItems();
                }
            }
        }
        return total;
    }

    /**
     * Get total items stored across all storage types for a player (legacy, sums all profiles).
     * @deprecated Use {@link #getTotalItemsStored(Player)} for profile-specific totals.
     */
    @Deprecated
    public int getTotalItemsStored(UUID playerId) {
        int total = 0;
        Map<UUID, Map<StorageType, StorageProfile>> playerData = playerProfileStorages.get(playerId);
        if (playerData != null) {
            for (Map<StorageType, StorageProfile> storages : playerData.values()) {
                for (StorageProfile profile : storages.values()) {
                    total += profile.getTotalItems();
                }
            }
        }
        return total;
    }

    /**
     * Get total storage capacity for a player's selected profile.
     */
    public int getTotalStorageCapacity(Player player) {
        SkyBlockProfile profile = getSelectedSkyBlockProfile(player);
        if (profile == null) {
            return getTotalStorageCapacity(player.getUniqueId(), player.getUniqueId());
        }
        return getTotalStorageCapacity(player.getUniqueId(), profile.getProfileId());
    }

    /**
     * Get total storage capacity for a player's specific profile.
     */
    public int getTotalStorageCapacity(UUID playerId, UUID profileId) {
        int total = 0;
        Map<UUID, Map<StorageType, StorageProfile>> playerData = playerProfileStorages.get(playerId);
        if (playerData != null) {
            Map<StorageType, StorageProfile> storages = playerData.get(profileId);
            if (storages != null) {
                for (StorageProfile storageProfile : storages.values()) {
                    total += storageProfile.getCurrentSlots();
                }
            }
        }
        return total;
    }

    /**
     * Get total storage capacity for a player (legacy, sums all profiles).
     * @deprecated Use {@link #getTotalStorageCapacity(Player)} for profile-specific capacity.
     */
    @Deprecated
    public int getTotalStorageCapacity(UUID playerId) {
        int total = 0;
        Map<UUID, Map<StorageType, StorageProfile>> playerData = playerProfileStorages.get(playerId);
        if (playerData != null) {
            for (Map<StorageType, StorageProfile> storages : playerData.values()) {
                for (StorageProfile profile : storages.values()) {
                    total += profile.getCurrentSlots();
                }
            }
        }
        return total;
    }

    /**
     * Get storage usage percentage for a player's selected profile.
     */
    public double getStorageUsagePercentage(Player player) {
        SkyBlockProfile profile = getSelectedSkyBlockProfile(player);
        if (profile == null) {
            return getStorageUsagePercentage(player.getUniqueId(), player.getUniqueId());
        }
        return getStorageUsagePercentage(player.getUniqueId(), profile.getProfileId());
    }

    /**
     * Get storage usage percentage for a player's specific profile.
     */
    public double getStorageUsagePercentage(UUID playerId, UUID profileId) {
        int totalItems = getTotalItemsStored(playerId, profileId);
        int totalCapacity = getTotalStorageCapacity(playerId, profileId);
        if (totalCapacity == 0) return 0;
        return (totalItems * 100.0) / totalCapacity;
    }

    /**
     * Get storage usage percentage for a player (legacy).
     * @deprecated Use {@link #getStorageUsagePercentage(Player)} for profile-specific percentage.
     */
    @Deprecated
    public double getStorageUsagePercentage(UUID playerId) {
        int totalItems = getTotalItemsStored(playerId);
        int totalCapacity = getTotalStorageCapacity(playerId);
        if (totalCapacity == 0) return 0;
        return (totalItems * 100.0) / totalCapacity;
    }

    /**
     * Get storage leaderboard (top players by items stored across all profiles).
     */
    public List<Map.Entry<UUID, Integer>> getLeaderboard(int limit) {
        Map<UUID, Integer> amounts = new HashMap<>();

        for (Map.Entry<UUID, Map<UUID, Map<StorageType, StorageProfile>>> playerEntry : playerProfileStorages.entrySet()) {
            int total = 0;
            for (Map<StorageType, StorageProfile> profileMap : playerEntry.getValue().values()) {
                for (StorageProfile profile : profileMap.values()) {
                    total += profile.getTotalItems();
                }
            }
            amounts.put(playerEntry.getKey(), total);
        }

        return amounts.entrySet().stream()
            .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
            .limit(limit)
            .toList();
    }

    /**
     * Get a player's rank in storage usage.
     */
    public int getPlayerRank(UUID playerId) {
        List<Map.Entry<UUID, Integer>> leaderboard = getLeaderboard(Integer.MAX_VALUE);
        for (int i = 0; i < leaderboard.size(); i++) {
            if (leaderboard.get(i).getKey().equals(playerId)) {
                return i + 1;
            }
        }
        return -1;
    }

    // ==================== PERSISTENCE ====================

    /**
     * Load upgrade configuration from file.
     */
    private void loadUpgradeConfig() {
        if (!upgradesConfigFile.exists()) {
            plugin.saveResource("storage_upgrades.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(upgradesConfigFile);

        for (StorageType type : StorageType.values()) {
            List<StorageUpgrade> upgrades = new ArrayList<>();
            ConfigurationSection typeSection = config.getConfigurationSection("upgrades." + type.name().toLowerCase());

            if (typeSection != null) {
                ConfigurationSection tiersSection = typeSection.getConfigurationSection("tiers");
                if (tiersSection != null) {
                    for (String key : tiersSection.getKeys(false)) {
                        StorageUpgrade upgrade = StorageUpgrade.fromConfig(
                            key,
                            tiersSection.getConfigurationSection(key)
                        );
                        if (upgrade != null) {
                            upgrades.add(upgrade);
                        }
                    }
                }
            }

            // Sort by tier
            upgrades.sort(Comparator.comparingInt(StorageUpgrade::getTier));
            upgradeTrees.put(type, upgrades);
        }
    }

    /**
     * Load player data from file.
     * Supports both legacy format (player -> storage) and new format (player -> profile -> storage).
     */
    private void loadPlayerData() {
        if (!storageDataFile.exists()) {
            storageDataFile.getParentFile().mkdirs();
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(storageDataFile);

        for (String uuidStr : config.getKeys(false)) {
            UUID playerId;
            try {
                playerId = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                continue;
            }

            ConfigurationSection playerSection = config.getConfigurationSection(uuidStr);
            if (playerSection == null) continue;

            Map<UUID, Map<StorageType, StorageProfile>> playerProfileMap = new ConcurrentHashMap<>();

            for (String typeId : playerSection.getKeys(false)) {
                try {
                    StorageType type = StorageType.valueOf(typeId.toUpperCase());
                    ConfigurationSection typeSection = playerSection.getConfigurationSection(typeId);
                    
                    // Check if this is a storage type section (legacy format)
                    if (typeSection != null && typeSection.contains("type")) {
                        // Legacy format: player -> storage type -> profile
                        // Load with player UUID as profile ID for compatibility
                        StorageProfile profile = StorageProfile.load(playerId, typeSection);
                        playerProfileMap.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                            .put(type, profile);
                    } else if (typeSection != null) {
                        // New format: player -> profile UUID -> storage type -> profile
                        // This section is actually a profile UUID, iterate through storage types
                        UUID profileId;
                        try {
                            profileId = UUID.fromString(typeId);
                        } catch (IllegalArgumentException e2) {
                            continue; // Not a valid UUID, skip
                        }
                        
                        Map<StorageType, StorageProfile> storageMap = new ConcurrentHashMap<>();
                        for (String storageTypeId : typeSection.getKeys(false)) {
                            try {
                                StorageType storageType = StorageType.valueOf(storageTypeId.toUpperCase());
                                ConfigurationSection storageSection = typeSection.getConfigurationSection(storageTypeId);
                                StorageProfile profile = StorageProfile.load(playerId, storageSection);
                                storageMap.put(storageType, profile);
                            } catch (IllegalArgumentException e3) {
                                // Unknown storage type, skip
                            }
                        }
                        playerProfileMap.put(profileId, storageMap);
                    }
                } catch (IllegalArgumentException e) {
                    // Unknown storage type, might be a profile UUID - handled above
                }
            }

            playerProfileStorages.put(playerId, playerProfileMap);
        }

        plugin.getLogger().info("Loaded storage data for " + playerProfileStorages.size() + " players.");
    }

    /**
     * Save a single player's data.
     */
    public synchronized void savePlayerData(UUID playerId) {
        Map<UUID, Map<StorageType, StorageProfile>> playerProfileMap = playerProfileStorages.get(playerId);
        if (playerProfileMap == null) return;

        YamlConfiguration config;
        if (storageDataFile.exists()) {
            config = YamlConfiguration.loadConfiguration(storageDataFile);
        } else {
            config = new YamlConfiguration();
            storageDataFile.getParentFile().mkdirs();
        }

        ConfigurationSection playerSection = config.createSection(playerId.toString());
        for (Map.Entry<UUID, Map<StorageType, StorageProfile>> profileEntry : playerProfileMap.entrySet()) {
            UUID profileId = profileEntry.getKey();
            ConfigurationSection profileSection = playerSection.createSection(profileId.toString());
            
            for (Map.Entry<StorageType, StorageProfile> entry : profileEntry.getValue().entrySet()) {
                ConfigurationSection typeSection = profileSection.createSection(entry.getKey().name());
                entry.getValue().save(typeSection);
            }
        }

        try {
            config.save(storageDataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player storage data: " + e.getMessage());
        }
    }

    /**
     * Save all player data.
     */
    public synchronized void saveAllPlayerData() {
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, Map<UUID, Map<StorageType, StorageProfile>>> playerEntry : playerProfileStorages.entrySet()) {
            ConfigurationSection playerSection = config.createSection(playerEntry.getKey().toString());
            
            for (Map.Entry<UUID, Map<StorageType, StorageProfile>> profileEntry : playerEntry.getValue().entrySet()) {
                UUID profileId = profileEntry.getKey();
                ConfigurationSection profileSection = playerSection.createSection(profileId.toString());
                
                for (Map.Entry<StorageType, StorageProfile> storageEntry : profileEntry.getValue().entrySet()) {
                    ConfigurationSection typeSection = profileSection.createSection(storageEntry.getKey().name());
                    storageEntry.getValue().save(typeSection);
                }
            }
        }

        storageDataFile.getParentFile().mkdirs();
        try {
            config.save(storageDataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save all player storage data: " + e.getMessage());
        }
    }

    /**
     * Start auto-save task.
     */
    private void startAutoSave() {
        autoSaveTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(
            plugin,
            this::saveAllPlayerData,
            autoSaveIntervalTicks,
            autoSaveIntervalTicks
        ).getTaskId();

        plugin.getLogger().info("Storage auto-save enabled (every " + (autoSaveIntervalTicks / 20) + " seconds)");
    }

    /**
     * Clear a player's storage data (for admin use).
     */
    public void clearPlayerData(UUID playerId) {
        playerProfileStorages.remove(playerId);
        saveAllPlayerData();
    }

    /**
     * Clear a player's specific storage type for their selected profile.
     */
    public void clearStorage(Player player, StorageType type) {
        SkyBlockProfile profile = getSelectedSkyBlockProfile(player);
        if (profile == null) {
            clearStorage(player.getUniqueId(), player.getUniqueId(), type);
            return;
        }
        clearStorage(player.getUniqueId(), profile.getProfileId(), type);
    }

    /**
     * Clear a player's specific storage type for a specific profile.
     */
    public void clearStorage(UUID playerId, UUID profileId, StorageType type) {
        Map<UUID, Map<StorageType, StorageProfile>> playerData = playerProfileStorages.get(playerId);
        if (playerData != null) {
            Map<StorageType, StorageProfile> storages = playerData.get(profileId);
            if (storages != null) {
                StorageProfile storageProfile = storages.get(type);
                if (storageProfile != null) {
                    storageProfile.clear();
                    savePlayerData(playerId);
                }
            }
        }
    }

    /**
     * Clear a player's specific storage type (legacy, uses player UUID as profile).
     * @deprecated Use {@link #clearStorage(Player, StorageType)} or {@link #clearStorage(UUID, UUID, StorageType)}.
     */
    @Deprecated
    public void clearStorage(UUID playerId, StorageType type) {
        clearStorage(playerId, playerId, type);
    }

    /**
     * Get the storage data file.
     */
    public File getStorageDataFile() {
        return storageDataFile;
    }

    /**
     * Get the upgrades config file.
     */
    public File getUpgradesConfigFile() {
        return upgradesConfigFile;
    }

}

