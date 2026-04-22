package io.papermc.Grivience.skyblock.profile;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skills.SkyblockSkill;
import io.papermc.Grivience.skills.SkyblockSkillManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.scheduler.BukkitTask;

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
    private static final long ACTIVE_SKILL_SAVE_DELAY_TICKS = 100L;

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
    private BukkitTask autoSaveTask;
    private final Map<UUID, BukkitTask> queuedProfileSaves = new ConcurrentHashMap<>();
    
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
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }

        long periodTicks = Math.max(20L, autoSaveIntervalSeconds * 20L);
        autoSaveTask = Bukkit.getScheduler().runTaskTimer(plugin, this::saveAllProfiles, periodTicks, periodTicks);
        
        plugin.getLogger().info("Profile auto-save started (every " + autoSaveIntervalSeconds + " seconds)");
    }
    
    public void shutdown() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
        for (BukkitTask task : queuedProfileSaves.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        queuedProfileSaves.clear();
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
        if (playerProfileIds.size() >= getMaxProfiles(player)) {
            sendMaxProfilesMessage(player);
            return null;
        }
        
        // Check for duplicate profile name
        for (UUID profileId : playerProfileIds) {
            SkyBlockProfile existing = profilesById.get(profileId);
            if (existing != null && existing.getProfileName().equalsIgnoreCase(profileName)) {
                sendDuplicateProfileNameMessage(player, profileName);
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

    public SkyBlockProfile createCoopProfile(Player player, SkyBlockProfile sharedProfile) {
        if (player == null || sharedProfile == null) {
            return null;
        }

        UUID playerId = player.getUniqueId();
        UUID sharedProfileId = sharedProfile.getProfileId();
        if (sharedProfileId == null) {
            return null;
        }

        SkyBlockProfile existing = getCoopProfile(playerId, sharedProfileId);
        if (existing != null) {
            return existing;
        }

        Set<UUID> playerProfileIds = playerProfiles.getOrDefault(playerId, new HashSet<>());
        if (playerProfileIds.size() >= maxProfilesPerPlayer) {
            sendMaxProfilesMessage(player);
            return null;
        }

        String profileName = nextAvailableProfileName(playerId, sharedProfile.getProfileName() + " Coop");
        SkyBlockProfile profile = new SkyBlockProfile(playerId, profileName);
        profile.setSharedProfileId(sharedProfileId);
        profile.setProfileIcon(sharedProfile.getProfileIcon());

        profilesById.put(profile.getProfileId(), profile);
        playerProfiles.computeIfAbsent(playerId, ignored -> ConcurrentHashMap.newKeySet()).add(profile.getProfileId());
        saveProfile(profile);

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

        // Flush profile-scoped GUI state (storage/accessory bags, etc.) before selection changes.
        if (currentProfileId != null
                && player.getOpenInventory() != null
                && player.getOpenInventory().getTopInventory() != null
                && player.getOpenInventory().getTopInventory().getType() != InventoryType.CRAFTING) {
            player.closeInventory();
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
        
        // Restore skill data to LevelManager so persistence is maintained across restarts
        pushProfileDataToLevelManager(player, profile);
        
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

        // Ensure profile-dependent combat/accessory/mana stats are immediately re-evaluated.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (plugin.getSkyblockCombatEngine() != null) {
                plugin.getSkyblockCombatEngine().refreshNow(player);
            }
            if (plugin.getSkyblockManaManager() != null) {
                plugin.getSkyblockManaManager().getMana(player);
            }
        }, 1L);
        
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

        if (targetProfile.isCoopMemberProfile()) {
            player.sendMessage(ChatColor.RED + "You cannot delete a coop profile directly.");
            player.sendMessage(ChatColor.GRAY + "Use /island leave to leave that coop properly.");
            return false;
        }

        io.papermc.Grivience.skyblock.island.IslandManager islandManager = plugin.getIslandManager();
        if (islandManager != null && islandManager.hasCoopMembers(targetProfile.getProfileId())) {
            player.sendMessage(ChatColor.RED + "That profile still has coop members.");
            player.sendMessage(ChatColor.GRAY + "Remove the coop members first before deleting the shared profile.");
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

    public boolean deleteProfile(UUID ownerId, UUID profileId) {
        if (ownerId == null || profileId == null) {
            return false;
        }

        SkyBlockProfile targetProfile = profilesById.get(profileId);
        if (targetProfile == null || !ownerId.equals(targetProfile.getOwnerId())) {
            return false;
        }

        Set<UUID> playerProfileIds = playerProfiles.get(ownerId);
        if (playerProfileIds != null) {
            playerProfileIds.remove(profileId);
            if (playerProfileIds.isEmpty()) {
                playerProfiles.remove(ownerId);
            }
        }

        profilesById.remove(profileId);
        playerSelectedProfile.remove(ownerId, profileId);
        deleteProfileFile(targetProfile);
        return true;
    }
    
    // ==================== PROFILE LISTING ====================
    
    /**
     * List all profiles for a player.
     */
    public List<SkyBlockProfile> getPlayerProfiles(Player player) {
        return player == null ? new ArrayList<>() : getPlayerProfiles(player.getUniqueId());
    }

    public List<SkyBlockProfile> getPlayerProfiles(UUID playerId) {
        Set<UUID> playerProfileIds = playerProfiles.get(playerId);

        if (playerProfileIds == null || playerProfileIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<SkyBlockProfile> profiles = new ArrayList<>();
        for (UUID profileId : playerProfileIds) {
            SkyBlockProfile profile = profilesById.get(profileId);
            if (profile != null) {
                syncProfileSnapshots(profile);
                profiles.add(profile);
            }
        }

        return profiles;
    }
    
    /**
     * Get the currently selected profile for a player.
     */
    public UUID getProfileId(UUID playerUuid) {
        return playerSelectedProfile.get(playerUuid);
    }

    public SkyBlockProfile getSelectedProfile(Player player) {
        UUID playerId = player.getUniqueId();
        UUID selectedProfileId = playerSelectedProfile.get(playerId);
        if (selectedProfileId != null && !profilesById.containsKey(selectedProfileId)) {
            playerSelectedProfile.remove(playerId, selectedProfileId);
            selectedProfileId = null;
        }
        
        if (selectedProfileId == null) {
            // Auto-select first profile if none selected
            Set<UUID> playerProfileIds = playerProfiles.get(playerId);
            if (playerProfileIds != null && !playerProfileIds.isEmpty()) {
                UUID firstProfileId = playerProfileIds.iterator().next();
                if (!selectProfile(player, firstProfileId, false)) {
                    return null;
                }
                SkyBlockProfile profile = profilesById.get(firstProfileId);
                syncProfileSnapshots(profile);
                return profile;
            }
            return null;
        }

        SkyBlockProfile profile = profilesById.get(selectedProfileId);
        syncProfileSnapshots(profile);
        return profile;
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
            SkyBlockProfile profile = profilesById.get(selectedProfileId);
            if (profile != null) {
                syncProfileSnapshots(profile);
                return profile;
            }
            playerSelectedProfile.remove(ownerId, selectedProfileId);
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
        syncProfileSnapshots(profile);
        return profile;
    }
    
    /**
     * Get a profile by ID.
     */
    public SkyBlockProfile getProfile(UUID profileId) {
        SkyBlockProfile profile = profilesById.get(profileId);
        syncProfileSnapshots(profile);
        return profile;
    }

    public SkyBlockProfile getCoopProfile(UUID ownerId, UUID sharedProfileId) {
        if (ownerId == null || sharedProfileId == null) {
            return null;
        }

        for (SkyBlockProfile profile : getPlayerProfiles(ownerId)) {
            if (profile != null
                    && profile.isCoopMemberProfile()
                    && sharedProfileId.equals(profile.getSharedProfileId())) {
                return profile;
            }
        }

        return null;
    }

    public List<SkyBlockProfile> getCoopProfiles(UUID ownerId, UUID sharedProfileId) {
        List<SkyBlockProfile> linkedProfiles = new ArrayList<>();
        if (ownerId == null || sharedProfileId == null) {
            return linkedProfiles;
        }

        for (SkyBlockProfile profile : getPlayerProfiles(ownerId)) {
            if (profile == null
                    || !profile.isCoopMemberProfile()
                    || !sharedProfileId.equals(profile.getSharedProfileId())) {
                continue;
            }
            linkedProfiles.add(profile);
        }

        return linkedProfiles;
    }

    public SkyBlockProfile resolveSharedProfile(SkyBlockProfile profile) {
        if (profile == null) {
            return null;
        }

        UUID canonicalProfileId = profile.getCanonicalProfileId();
        if (canonicalProfileId == null || canonicalProfileId.equals(profile.getProfileId())) {
            return profile;
        }

        SkyBlockProfile sharedProfile = getProfile(canonicalProfileId);
        return sharedProfile != null ? sharedProfile : profile;
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
                syncProfileSnapshots(profile);
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

        for (SkyBlockProfile profile : profilesById.values()) {
            syncProfileSnapshots(profile);
        }
        
        plugin.getLogger().info("Loaded " + loaded + " Skyblock profiles.");
    }
    
    public synchronized void saveProfile(SkyBlockProfile profile) {
        if (profile == null) return;

        BukkitTask queuedTask = queuedProfileSaves.remove(profile.getProfileId());
        if (queuedTask != null) {
            queuedTask.cancel();
        }

        syncProfileSnapshots(profile);
        flushLiveProfileData();
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

    private void syncProfileSnapshots(SkyBlockProfile profile) {
        syncProfileSkillLevels(profile);
        syncProfileCollectionLevels(profile);
        syncProfileCompletedQuests(profile);
    }

    /**
     * CRITICAL: Pushes data FROM the profile INTO the LevelManager/SkillManager.
     * This ensures that when a player selects a profile, their persistent skill XP
     * and pseudo-levels are restored to the active counter system.
     */
    private void pushProfileDataToLevelManager(Player player, SkyBlockProfile profile) {
        if (player == null || profile == null) return;

        io.papermc.Grivience.stats.SkyblockLevelManager levelManager = plugin.getSkyblockLevelManager();
        if (levelManager == null) return;

        UUID profileId = profile.getCanonicalProfileId();

        // Restore Skill XP and Levels
        for (String skillName : profile.getSkillLevels().keySet()) {
            int level = profile.getSkillLevel(skillName);
            long xp = profile.getSkillXp(skillName);

            String skillKey = skillName.toLowerCase(Locale.ROOT);
            if (skillKey.equals("dungeoneering")) skillKey = "catacombs";

            // Push into LevelManager counters (using the new underscore format via normalize)
            levelManager.addToCounter(player, "skill_xp_" + skillKey, xp);
            levelManager.addToCounter(player, "pseudo_level_" + skillKey, (long) level);
        }
    }

    private void syncProfileSkillLevels(SkyBlockProfile profile) {
        if (profile == null) {
            return;
        }

        SkyblockSkillManager skillManager = plugin.getSkyblockSkillManager();
        io.papermc.Grivience.stats.SkyblockLevelManager levelManager = plugin.getSkyblockLevelManager();
        UUID skillProfileId = profile.getCanonicalProfileId();
        if (skillManager == null || levelManager == null || skillProfileId == null) {
            return;
        }

        // 1. Sync standard Enum-based skills
        for (SkyblockSkill skill : SkyblockSkill.values()) {
            profile.setSkillLevel(skill.name(), skillManager.getLevel(skillProfileId, skill));
            profile.setSkillXp(skill.name(), (long) skillManager.getXp(skillProfileId, skill));
        }

        // 2. Scan for any "pseudo-levels" or "extra skills" not in the main Enum
        Map<String, Long> counters = levelManager.getCounters(skillProfileId);
        if (counters != null && !counters.isEmpty()) {
            for (Map.Entry<String, Long> entry : counters.entrySet()) {
                String key = entry.getKey();
                if (key == null) continue;

                // Support both legacy dot-format and new underscore-format keys
                if (key.startsWith("pseudo_level.") || key.startsWith("pseudo_level_")) {
                    String skillName = key.substring(13).toUpperCase(Locale.ROOT);
                    if (skillName.equals("CATACOMBS")) skillName = "DUNGEONEERING";

                    int level = entry.getValue().intValue();
                    // Only update if higher than current to avoid Enum-based level conflicts
                    if (level > profile.getSkillLevel(skillName)) {
                        profile.setSkillLevel(skillName, level);
                    }
                } else if (key.startsWith("skill_xp.") || key.startsWith("skill_xp_")) {
                    String skillName = key.substring(9).toUpperCase(Locale.ROOT);
                    long xp = entry.getValue();
                    if (xp > profile.getSkillXp(skillName)) {
                        profile.setSkillXp(skillName, xp);
                    }
                }
            }
        }
    }
    private void syncProfileCollectionLevels(SkyBlockProfile profile) {
        if (profile == null) {
            return;
        }

        io.papermc.Grivience.collections.CollectionsManager collectionsManager = plugin.getCollectionsManager();
        UUID collectionProfileId = profile.getCanonicalProfileId();
        if (collectionsManager == null || collectionProfileId == null) {
            return;
        }

        profile.replaceCollectionLevels(collectionsManager.getCollectionTierSnapshot(collectionProfileId));
    }

    private void syncProfileCompletedQuests(SkyBlockProfile profile) {
        if (profile == null) {
            return;
        }

        io.papermc.Grivience.quest.QuestManager questManager = plugin.getQuestManager();
        UUID questProfileId = profile.getCanonicalProfileId();
        if (questManager == null || questProfileId == null) {
            return;
        }

        Set<String> mergedCompleted = new LinkedHashSet<>(profile.getCompletedQuests());
        Set<String> managedQuestIds = new HashSet<>(questManager.questIds());
        if (!managedQuestIds.isEmpty()) {
            mergedCompleted.removeIf(questId -> questId != null && managedQuestIds.contains(questId.toLowerCase(Locale.ROOT)));
        }
        mergedCompleted.addAll(questManager.completedQuestIds(questProfileId));
        profile.replaceCompletedQuests(mergedCompleted);
    }

    private void flushLiveProfileData() {
        io.papermc.Grivience.stats.SkyblockLevelManager skyblockLevelManager = plugin.getSkyblockLevelManager();
        if (skyblockLevelManager != null) {
            skyblockLevelManager.saveIfDirty();
        }
    }

    public void syncSkillSnapshotsAndQueueSave(Player player) {
        if (player == null) {
            return;
        }
        SkyBlockProfile selected = getSelectedProfile(player);
        if (selected == null) {
            return;
        }
        syncSkillSnapshotsAndQueueSave(selected.getCanonicalProfileId());
    }

    public void syncSkillSnapshotsAndQueueSave(UUID canonicalProfileId) {
        if (canonicalProfileId == null) {
            return;
        }
        for (SkyBlockProfile profile : profilesById.values()) {
            if (profile == null || !canonicalProfileId.equals(profile.getCanonicalProfileId())) {
                continue;
            }
            syncProfileSnapshots(profile);
            queueProfileSave(profile.getProfileId());
        }
    }

    private void queueProfileSave(UUID profileId) {
        if (profileId == null || !plugin.isEnabled()) {
            return;
        }

        BukkitTask existing = queuedProfileSaves.remove(profileId);
        if (existing != null) {
            existing.cancel();
        }

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            queuedProfileSaves.remove(profileId);
            SkyBlockProfile profile = profilesById.get(profileId);
            if (profile != null) {
                saveProfile(profile);
            }
        }, ACTIVE_SKILL_SAVE_DELAY_TICKS);
        queuedProfileSaves.put(profileId, task);
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

    public int getMaxProfiles(Player player) {
        if (player == null) return getMaxProfilesPerPlayer();
        
        int max = getMaxProfilesPerPlayer();
        
        if (player.hasPermission("grivience.profiles.limit.mvpplusplus")) {
            max = Math.max(max, 7);
        } else if (player.hasPermission("grivience.profiles.limit.mvpplus")) {
            max = Math.max(max, 5);
        } else if (player.hasPermission("grivience.profiles.limit.mvp")) {
            max = Math.max(max, 4);
        } else if (player.hasPermission("grivience.profiles.limit.vipplusplus")) {
            max = Math.max(max, 3);
        } else if (player.hasPermission("grivience.profiles.limit.vipplus")) {
            max = Math.max(max, 2);
        } else if (player.hasPermission("grivience.profiles.limit.vip")) {
            max = Math.max(max, 2);
        }
        
        return max;
    }
    
    public boolean isAllowProfileDeletion() {
        return allowProfileDeletion;
    }
    
    public int getAutoSaveIntervalSeconds() {
        return autoSaveIntervalSeconds;
    }

    public UUID getSelectedProfileId(UUID ownerId) {
        if (ownerId == null) {
            return null;
        }

        UUID selectedProfileId = playerSelectedProfile.get(ownerId);
        if (selectedProfileId != null && !profilesById.containsKey(selectedProfileId)) {
            playerSelectedProfile.remove(ownerId, selectedProfileId);
            return null;
        }
        return selectedProfileId;
    }

    public SkyBlockProfile findProfileById(UUID profileId) {
        if (profileId == null) {
            return null;
        }
        return profilesById.get(profileId);
    }

    public UUID getFirstProfileId(UUID ownerId) {
        if (ownerId == null) {
            return null;
        }

        Set<UUID> profileIds = playerProfiles.get(ownerId);
        if (profileIds == null || profileIds.isEmpty()) {
            return null;
        }
        return profileIds.iterator().next();
    }

    private void sendMaxProfilesMessage(Player player) {
        if (player == null) {
            return;
        }
        player.sendMessage(ChatColor.RED + "You have reached the maximum number of profiles (" + maxProfilesPerPlayer + ").");
        player.sendMessage(ChatColor.GRAY + "Delete a profile with /profile delete <name> to create a new one.");
    }

    private void sendDuplicateProfileNameMessage(Player player, String profileName) {
        if (player == null) {
            return;
        }
        player.sendMessage(ChatColor.RED + "You already have a profile named '" + profileName + "'.");
    }

    private String nextAvailableProfileName(UUID playerId, String baseName) {
        String candidate = (baseName == null || baseName.isBlank()) ? "Coop" : baseName.trim();
        if (!hasProfileName(playerId, candidate)) {
            return candidate;
        }

        String indexedBase = candidate;
        int counter = 2;
        while (hasProfileName(playerId, indexedBase + " " + counter)) {
            counter++;
        }
        return indexedBase + " " + counter;
    }

    private boolean hasProfileName(UUID playerId, String profileName) {
        if (playerId == null || profileName == null || profileName.isBlank()) {
            return false;
        }

        for (SkyBlockProfile profile : getPlayerProfiles(playerId)) {
            if (profile != null && profile.getProfileName().equalsIgnoreCase(profileName)) {
                return true;
            }
        }
        return false;
    }
}

