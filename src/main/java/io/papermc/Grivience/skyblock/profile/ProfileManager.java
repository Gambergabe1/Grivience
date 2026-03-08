package io.papermc.Grivience.skyblock.profile;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Skyblock-accurate player profiles.
 * 
 * Features:
 * - Multiple profiles per player (up to 5 by default, Skyblock standard)
 * - Profile creation, deletion, switching
 * - Profile persistence to YAML
 * - Auto-save on interval
 * - Profile-specific data isolation
 */
public final class ProfileManager {
    private final GriviencePlugin plugin;
    
    // Profiles indexed by profile ID
    private final Map<UUID, SkyBlockProfile> profilesById = new ConcurrentHashMap<>();
    
    // Player's profiles indexed by owner UUID
    private final Map<UUID, Set<UUID>> playerProfiles = new ConcurrentHashMap<>();
    
    // Player's currently selected profile
    private final Map<UUID, UUID> playerSelectedProfile = new ConcurrentHashMap<>();
    
    // Configuration
    private int maxProfilesPerPlayer;
    private boolean allowProfileDeletion;
    private int autoSaveIntervalSeconds;
    private File profilesFolder;
    
    // Auto-save task
    private Timer autoSaveTimer;
    
    public ProfileManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.profilesFolder = new File(plugin.getDataFolder(), "skyblock/profiles");
        loadConfig();
        ensureDirectories();
        loadAllProfiles();
        startAutoSave();
    }
    
    public void loadConfig() {
        maxProfilesPerPlayer = plugin.getConfig().getInt("skyblock-profiles.max-profiles", 5);
        allowProfileDeletion = plugin.getConfig().getBoolean("skyblock-profiles.allow-deletion", true);
        autoSaveIntervalSeconds = plugin.getConfig().getInt("skyblock-profiles.auto-save-interval-seconds", 300);
    }
    
    private void ensureDirectories() {
        if (!profilesFolder.exists()) {
            profilesFolder.mkdirs();
            plugin.getLogger().info("Created profiles directory: " + profilesFolder.getPath());
        }
    }
    
    private void startAutoSave() {
        autoSaveTimer = new Timer("Grivience-Profile-AutoSave", true);
        autoSaveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                saveAllProfiles();
            }
        }, autoSaveIntervalSeconds * 1000L, autoSaveIntervalSeconds * 1000L);
        
        plugin.getLogger().info("Profile auto-save started (every " + autoSaveIntervalSeconds + " seconds)");
    }
    
    public void shutdown() {
        if (autoSaveTimer != null) {
            autoSaveTimer.cancel();
        }
        saveAllProfiles();
        plugin.getLogger().info("ProfileManager shutdown complete. All profiles saved.");
    }
    
    // ==================== PROFILE CREATION ====================
    
    /**
     * Create a new profile for a player.
     */
    public SkyBlockProfile createProfile(Player player, String profileName) {
        UUID playerId = player.getUniqueId();
        
        // Check max profiles limit
        Set<UUID> playerProfileIds = playerProfiles.getOrDefault(playerId, new HashSet<>());
        if (playerProfileIds.size() >= maxProfilesPerPlayer) {
            player.sendMessage(ChatColor.RED + "You have reached the maximum number of profiles (" + maxProfilesPerPlayer + ").");
            player.sendMessage(ChatColor.GRAY + "Delete a profile with /profile delete <name> to create a new one.");
            return null;
        }
        
        // Check for duplicate profile name
        for (UUID profileId : playerProfileIds) {
            SkyBlockProfile existing = profilesById.get(profileId);
            if (existing != null && existing.getProfileName().equalsIgnoreCase(profileName)) {
                player.sendMessage(ChatColor.RED + "You already have a profile named '" + profileName + "'.");
                return null;
            }
        }
        
        // Create the profile
        SkyBlockProfile profile = new SkyBlockProfile(playerId, profileName);
        
        // Add to indexes
        profilesById.put(profile.getProfileId(), profile);
        Set<UUID> actualProfileIds = playerProfiles.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet());
        boolean isFirstProfile = actualProfileIds.isEmpty();
        actualProfileIds.add(profile.getProfileId());
        
        // If this is the player's first profile, select it automatically
        if (isFirstProfile) {
            selectProfile(player, profile.getProfileId());
        }
        
        // Save immediately
        saveProfile(profile);
        
        player.sendMessage(ChatColor.GREEN + "Profile '" + profileName + "' created successfully!");
        player.sendMessage(ChatColor.GRAY + "Use /profile select <name> to switch profiles.");
        
        return profile;
    }
    
    // ==================== PROFILE SELECTION ====================
    
    /**
     * Select a profile for a player.
     */
    public boolean selectProfile(Player player, UUID profileId) {
        return selectProfile(player, profileId, true);
    }

    private boolean selectProfile(Player player, UUID profileId, boolean sendMessage) {
        UUID playerId = player.getUniqueId();

        // Verify the profile belongs to the player
        Set<UUID> playerProfileIds = playerProfiles.get(playerId);
        if (playerProfileIds == null || !playerProfileIds.contains(profileId)) {
            player.sendMessage(ChatColor.RED + "You don't have access to that profile.");
            return false;
        }

        UUID currentProfileId = playerSelectedProfile.get(playerId);
        if (Objects.equals(currentProfileId, profileId)) {
            if (sendMessage) {
                SkyBlockProfile current = profilesById.get(profileId);
                if (current != null) {
                    player.sendMessage(ChatColor.YELLOW + "You are already on profile: " + ChatColor.AQUA + current.getProfileName());
                }
            }
            return true;
        }

        // Profiles have separate inventories/islands/purses. Switching mid-trade can move items or coins across profiles.
        io.papermc.Grivience.trade.TradeManager tradeManager = plugin.getTradeManager();
        if (tradeManager != null && tradeManager.getSessionByPlayer(playerId) != null) {
            player.sendMessage(ChatColor.RED + "You cannot switch Skyblock profiles while trading.");
            player.sendMessage(ChatColor.GRAY + "Finish or cancel your trade first.");
            return false;
        }

        SkyBlockProfile profile = profilesById.get(profileId);
        if (profile == null) {
            player.sendMessage(ChatColor.RED + "Profile not found.");
            return false;
        }

        // Deselect current profile
        if (currentProfileId != null) {
            SkyBlockProfile currentProfile = profilesById.get(currentProfileId);
            if (currentProfile != null) {
                currentProfile.setSelected(false);
                saveProfile(currentProfile);
            }
        }

        // Select new profile
        profile.setSelected(true);
        playerSelectedProfile.put(playerId, profileId);
        saveProfile(profile);
        
        if (sendMessage) {
            player.sendMessage(ChatColor.GREEN + "Switched to profile: " + ChatColor.AQUA + profile.getProfileName());
        }

        io.papermc.Grivience.skyblock.island.IslandManager islandManager = plugin.getIslandManager();
        if (islandManager != null) {
            islandManager.handleSkyBlockProfileSwitch(player, currentProfileId, profile);
        }

        io.papermc.Grivience.pet.PetManager petManager = plugin.getPetManager();
        if (petManager != null) {
            // Apply pet bonuses after the profile swap completes (inventory/world changes may happen during switch).
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    petManager.applyCurrent(player);
                }
            }, 1L);
        }
        
        return true;
    }
    
    /**
     * Select a profile by name.
     */
    public boolean selectProfile(Player player, String profileName) {
        UUID playerId = player.getUniqueId();
        Set<UUID> playerProfileIds = playerProfiles.get(playerId);
        
        if (playerProfileIds == null) {
            player.sendMessage(ChatColor.RED + "You don't have any profiles.");
            return false;
        }
        
        for (UUID profileId : playerProfileIds) {
            SkyBlockProfile profile = profilesById.get(profileId);
            if (profile != null && profile.getProfileName().equalsIgnoreCase(profileName)) {
                return selectProfile(player, profileId, true);
            }
        }
        
        player.sendMessage(ChatColor.RED + "Profile '" + profileName + "' not found.");
        return false;
    }
    
    // ==================== PROFILE DELETION ====================
    
    /**
     * Delete a profile.
     */
    public boolean deleteProfile(Player player, String profileName) {
        if (!allowProfileDeletion) {
            player.sendMessage(ChatColor.RED + "Profile deletion is disabled on this server.");
            return false;
        }
        
        UUID playerId = player.getUniqueId();
        Set<UUID> playerProfileIds = playerProfiles.get(playerId);
        
        if (playerProfileIds == null || playerProfileIds.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You don't have any profiles.");
            return false;
        }
        
        // Find the profile
        SkyBlockProfile targetProfile = null;
        for (UUID profileId : playerProfileIds) {
            SkyBlockProfile profile = profilesById.get(profileId);
            if (profile != null && profile.getProfileName().equalsIgnoreCase(profileName)) {
                targetProfile = profile;
                break;
            }
        }
        
        if (targetProfile == null) {
            player.sendMessage(ChatColor.RED + "Profile '" + profileName + "' not found.");
            return false;
        }
        
        // Prevent deleting the last profile
        if (playerProfileIds.size() <= 1) {
            player.sendMessage(ChatColor.RED + "You cannot delete your last profile.");
            return false;
        }
        
        // If deleting selected profile, switch to another
        UUID selectedProfileId = playerSelectedProfile.get(playerId);
        if (selectedProfileId != null && selectedProfileId.equals(targetProfile.getProfileId())) {
            // Find another profile to select
            for (UUID profileId : playerProfileIds) {
                if (!profileId.equals(targetProfile.getProfileId())) {
                    selectProfile(player, profileId);
                    break;
                }
            }
        }
        
        // Remove from indexes
        profilesById.remove(targetProfile.getProfileId());
        playerProfileIds.remove(targetProfile.getProfileId());
        
        // Delete the file
        deleteProfileFile(targetProfile);
        
        player.sendMessage(ChatColor.GREEN + "Profile '" + profileName + "' deleted successfully.");
        
        return true;
    }
    
    // ==================== PROFILE LISTING ====================
    
    /**
     * List all profiles for a player.
     */
    public List<SkyBlockProfile> getPlayerProfiles(Player player) {
        UUID playerId = player.getUniqueId();
        Set<UUID> playerProfileIds = playerProfiles.get(playerId);
        
        if (playerProfileIds == null || playerProfileIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<SkyBlockProfile> profiles = new ArrayList<>();
        for (UUID profileId : playerProfileIds) {
            SkyBlockProfile profile = profilesById.get(profileId);
            if (profile != null) {
                profiles.add(profile);
            }
        }
        
        return profiles;
    }
    
    /**
     * Get the currently selected profile for a player.
     */
    public SkyBlockProfile getSelectedProfile(Player player) {
        UUID playerId = player.getUniqueId();
        UUID selectedProfileId = playerSelectedProfile.get(playerId);
        
        if (selectedProfileId == null) {
            // Auto-select first profile if none selected
            Set<UUID> playerProfileIds = playerProfiles.get(playerId);
            if (playerProfileIds != null && !playerProfileIds.isEmpty()) {
                UUID firstProfileId = playerProfileIds.iterator().next();
                selectProfile(player, firstProfileId, false);
                return profilesById.get(firstProfileId);
            }
            return null;
        }
        
        return profilesById.get(selectedProfileId);
    }

    /**
     * Offline-safe selected profile lookup.
     * <p>
     * Hypixel-style visiting needs to resolve the owner's currently selected profile even when they are offline.
     */
    public SkyBlockProfile getSelectedProfile(UUID ownerId) {
        if (ownerId == null) {
            return null;
        }

        UUID selectedProfileId = playerSelectedProfile.get(ownerId);
        if (selectedProfileId != null) {
            return profilesById.get(selectedProfileId);
        }

        // Auto-select the first profile if none is selected (no chat messages, but persist the selection).
        Set<UUID> profileIds = playerProfiles.get(ownerId);
        if (profileIds == null || profileIds.isEmpty()) {
            return null;
        }

        UUID firstProfileId = profileIds.iterator().next();
        SkyBlockProfile profile = profilesById.get(firstProfileId);
        if (profile == null) {
            return null;
        }

        // Deselect any other selected profile flags just in case.
        for (UUID profileId : profileIds) {
            SkyBlockProfile other = profilesById.get(profileId);
            if (other == null) {
                continue;
            }
            other.setSelected(profileId.equals(firstProfileId));
            saveProfile(other);
        }

        playerSelectedProfile.put(ownerId, firstProfileId);
        return profile;
    }
    
    /**
     * Get a profile by ID.
     */
    public SkyBlockProfile getProfile(UUID profileId) {
        return profilesById.get(profileId);
    }
    
    /**
     * Get a profile by name for a player.
     */
    public SkyBlockProfile getProfile(Player player, String profileName) {
        UUID playerId = player.getUniqueId();
        Set<UUID> playerProfileIds = playerProfiles.get(playerId);
        
        if (playerProfileIds == null) return null;
        
        for (UUID profileId : playerProfileIds) {
            SkyBlockProfile profile = profilesById.get(profileId);
            if (profile != null && profile.getProfileName().equalsIgnoreCase(profileName)) {
                return profile;
            }
        }
        
        return null;
    }
    
    // ==================== PERSISTENCE ====================
    
    private void loadAllProfiles() {
        if (!profilesFolder.exists()) {
            plugin.getLogger().info("No profiles directory found. Will create on first profile.");
            return;
        }

        // Profiles are stored under skyblock/profiles/<ownerUUID>/<profileId>.yml
        // Load both legacy top-level .yml files and per-owner nested files.
        List<File> profileFiles = new ArrayList<>();
        File[] topLevelEntries = profilesFolder.listFiles();
        if (topLevelEntries != null) {
            for (File entry : topLevelEntries) {
                if (entry == null) {
                    continue;
                }
                if (entry.isFile() && entry.getName().endsWith(".yml")) {
                    profileFiles.add(entry);
                    continue;
                }
                if (!entry.isDirectory()) {
                    continue;
                }
                File[] nested = entry.listFiles((dir, name) -> name.endsWith(".yml"));
                if (nested != null && nested.length > 0) {
                    profileFiles.addAll(Arrays.asList(nested));
                }
            }
        }

        if (profileFiles.isEmpty()) {
            plugin.getLogger().info("No profile files found.");
            return;
        }
        
        int loaded = 0;
        for (File file : profileFiles) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                ConfigurationSection profileSection = config.getConfigurationSection("profile");
                
                if (profileSection != null) {
                    SkyBlockProfile profile = SkyBlockProfile.fromSection(profileSection);
                    if (profile != null) {
                        profilesById.put(profile.getProfileId(), profile);
                        playerProfiles.computeIfAbsent(profile.getOwnerId(), k -> ConcurrentHashMap.newKeySet())
                            .add(profile.getProfileId());
                        
                        if (profile.isSelected()) {
                            playerSelectedProfile.put(profile.getOwnerId(), profile.getProfileId());
                        }
                        
                        loaded++;
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load profile from: " + file.getName());
                e.printStackTrace();
            }
        }
        
        plugin.getLogger().info("Loaded " + loaded + " Skyblock profiles.");
    }
    
    public synchronized void saveProfile(SkyBlockProfile profile) {
        if (profile == null) return;
        
        profile.setLastSaveTime(System.currentTimeMillis());
        
        File playerFolder = new File(profilesFolder, profile.getOwnerId().toString());
        if (!playerFolder.exists()) {
            playerFolder.mkdirs();
        }
        
        File profileFile = new File(playerFolder, profile.getProfileId().toString() + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        
        ConfigurationSection profileSection = config.createSection("profile");
        profile.save(profileSection);
        
        try {
            config.save(profileFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save profile: " + profile.getProfileName());
            e.printStackTrace();
        }
    }
    
    public synchronized void saveAllProfiles() {
        int saved = 0;
        for (SkyBlockProfile profile : profilesById.values()) {
            saveProfile(profile);
            saved++;
        }
        if (saved > 0) {
            plugin.getLogger().info("Auto-saved " + saved + " Skyblock profiles.");
        }
    }
    
    private void deleteProfileFile(SkyBlockProfile profile) {
        File playerFolder = new File(profilesFolder, profile.getOwnerId().toString());
        File profileFile = new File(playerFolder, profile.getProfileId().toString() + ".yml");
        
        if (profileFile.exists()) {
            profileFile.delete();
        }
        
        // Clean up empty player folder
        if (playerFolder.list() == null || playerFolder.list().length == 0) {
            playerFolder.delete();
        }
    }
    
    // ==================== CONFIGURATION ====================
    
    public int getMaxProfilesPerPlayer() {
        return maxProfilesPerPlayer;
    }
    
    public boolean isAllowProfileDeletion() {
        return allowProfileDeletion;
    }
    
    public int getAutoSaveIntervalSeconds() {
        return autoSaveIntervalSeconds;
    }
}

