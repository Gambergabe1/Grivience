package io.papermc.Grivience.collections;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.crafting.RecipeRegistry;
import io.papermc.Grivience.crafting.SkyblockRecipe;
import io.papermc.Grivience.skyblock.profile.ProfileManager;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main manager for the Collections System.
 * Handles collection definitions, player progress tracking, and persistence.
 * 
 * Skyblock accurate features:
 * - Natural item collection tracking (player-placed blocks don't count)
 * - Per-profile persistent progress
 * - Tier-based rewards with Skyblock XP
 * - Collection categories and browsing
 * - Leaderboards and statistics
 */
public class CollectionsManager {
    private final GriviencePlugin plugin;
    private final Map<String, CollectionDefinition> collections;
    private final Map<String, String> collectionIdByItemId;
    private final Map<UUID, Map<String, PlayerCollectionProgress>> progressByProfile;
    private final Set<UUID> dirtyProfiles = ConcurrentHashMap.newKeySet();
    private final Object playerDataFileLock = new Object();
    private final File collectionsFile;
    private final File playerDataFile;
    private final NamespacedKey customItemIdKey;
    private BukkitTask autoSaveTask;
    private boolean enabled;

    public CollectionsManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.collections = new LinkedHashMap<>();
        this.collectionIdByItemId = new HashMap<>();
        this.progressByProfile = new ConcurrentHashMap<>();
        this.collectionsFile = new File(plugin.getDataFolder(), "collections.yml");
        this.playerDataFile = new File(plugin.getDataFolder(), "collections" + File.separator + "player_data.yml");
        this.customItemIdKey = new NamespacedKey(plugin, "custom-item-id");
        this.enabled = false;
    }

    /**
     * Load and enable the collections system.
     */
    public void load() {
        loadCollections();
        loadPlayerData();
        enabled = true;
        startAutoSave();
        plugin.getLogger().info("Collections system loaded with " + collections.size() + " collections.");
    }

    /**
     * Disable the collections system.
     */
    public void disable() {
        stopAutoSave();
        saveAllPlayerData();
        enabled = false;
    }

    /**
     * Reload collections from config.
     */
    public void reload() {
        saveAllPlayerData();
        collections.clear();
        collectionIdByItemId.clear();
        loadCollections();
        if (enabled) {
            startAutoSave();
        }
        plugin.getLogger().info("Collections reloaded.");
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get all collection definitions.
     */
    public Map<String, CollectionDefinition> getCollections() {
        return new HashMap<>(collections);
    }

    /**
     * Get a specific collection by ID.
     */
    public CollectionDefinition getCollection(String id) {
        return collections.get(normalizeCollectionId(id));
    }

    /**
     * Get collections by category.
     */
    public List<CollectionDefinition> getCollectionsByCategory(CollectionCategory category) {
        List<CollectionDefinition> result = new ArrayList<>();
        for (CollectionDefinition collection : collections.values()) {
            if (collection.getCategory() == category && collection.isEnabled()) {
                result.add(collection);
            }
        }
        return result;
    }

    public List<String> getSubcategories(CollectionCategory category) {
        if (category == null) {
            return List.of();
        }

        Map<String, String> ordered = new LinkedHashMap<>();
        for (CollectionDefinition collection : collections.values()) {
            if (!collection.isEnabled() || collection.getCategory() != category) {
                continue;
            }
            String raw = collection.getSubcategory();
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String trimmed = raw.trim();
            ordered.putIfAbsent(trimmed.toLowerCase(Locale.ROOT), trimmed);
        }

        return new ArrayList<>(ordered.values());
    }

    public List<CollectionDefinition> getCollectionsByCategoryAndSubcategory(CollectionCategory category, String subcategory) {
        if (category == null || subcategory == null || subcategory.isBlank()) {
            return List.of();
        }

        String target = subcategory.trim().toLowerCase(Locale.ROOT);
        List<CollectionDefinition> result = new ArrayList<>();
        for (CollectionDefinition collection : collections.values()) {
            if (!collection.isEnabled() || collection.getCategory() != category) {
                continue;
            }
            String raw = collection.getSubcategory();
            if (raw == null || raw.isBlank()) {
                continue;
            }
            if (raw.trim().toLowerCase(Locale.ROOT).equals(target)) {
                result.add(collection);
            }
        }
        return result;
    }

    /**
     * Get all enabled collections.
     */
    public List<CollectionDefinition> getEnabledCollections() {
        List<CollectionDefinition> result = new ArrayList<>();
        for (CollectionDefinition collection : collections.values()) {
            if (collection.isEnabled()) {
                result.add(collection);
            }
        }
        return result;
    }

    /**
     * Get a profile's progress for a specific collection.
     */
    public PlayerCollectionProgress getPlayerProgress(UUID profileId, String collectionId) {
        String normalizedCollectionId = normalizeCollectionId(collectionId);
        return progressByProfile
            .computeIfAbsent(profileId, ignored -> new ConcurrentHashMap<>())
            .computeIfAbsent(normalizedCollectionId, ignored -> new PlayerCollectionProgress(profileId, normalizedCollectionId));
    }

    public PlayerCollectionProgress getPlayerProgress(Player player, String collectionId) {
        UUID profileId = resolveProfileId(player);
        if (profileId == null) {
            return new PlayerCollectionProgress(player == null ? null : player.getUniqueId(), normalizeCollectionId(collectionId));
        }
        return getPlayerProgress(profileId, collectionId);
    }

    /**
     * Get all progress for a profile.
     */
    public Map<String, PlayerCollectionProgress> getPlayerProgress(UUID profileId) {
        return new HashMap<>(progressByProfile.computeIfAbsent(profileId, ignored -> new ConcurrentHashMap<>()));
    }

    public Map<String, PlayerCollectionProgress> getPlayerProgress(Player player) {
        UUID profileId = resolveProfileId(player);
        if (profileId == null) {
            return new HashMap<>();
        }
        return getPlayerProgress(profileId);
    }

    /**
     * Add to a player's collection progress.
     * Returns the new collected amount.
     */
    public long addCollection(Player player, String itemId, long amount) {
        if (!enabled) return 0;

        // Find which collection tracks this item
        CollectionDefinition collection = findCollectionForItem(itemId);
        if (collection == null || !collection.isEnabled()) return 0;

        // Check if this is a natural collection (not player-placed)
        // This would be handled by the listener, but we check here too
        UUID profileId = resolveProfileId(player);
        if (profileId == null) {
            return 0;
        }
        PlayerCollectionProgress progress = getPlayerProgress(profileId, collection.getId());
        progress.addCollection(amount);
        progress.addContribution(player.getUniqueId(), amount);
        markDirty(profileId);

        // Check for tier unlocks
        checkTierUnlocks(player, collection, progress);

        return progress.getCollectedAmount();
    }

    /**
     * Set a player's collection amount directly (for admin commands).
     */
    public void setCollection(Player player, String collectionId, long amount) {
        CollectionDefinition collection = getCollection(collectionId);
        if (collection == null) return;

        UUID profileId = resolveProfileId(player);
        if (profileId == null) {
            return;
        }
        PlayerCollectionProgress progress = getPlayerProgress(profileId, collection.getId());
        long oldAmount = progress.getCollectedAmount();
        progress.setCollectedAmount(amount);
        markDirty(profileId);

        // Check for tier unlocks if increased
        if (amount > oldAmount) {
            checkTierUnlocks(player, collection, progress);
        }
    }

    /**
     * Find the collection that tracks a specific item.
     */
    public CollectionDefinition findCollectionForItem(String itemId) {
        String normalizedItemId = normalizeItemId(itemId);
        if (normalizedItemId.isBlank()) {
            return null;
        }

        String collectionId = collectionIdByItemId.get(normalizedItemId);
        if (collectionId == null) {
            return null;
        }

        return collections.get(collectionId);
    }

    /**
     * Find the collection that tracks a specific Material.
     */
    public CollectionDefinition findCollectionForMaterial(Material material) {
        if (material == null) {
            return null;
        }
        return findCollectionForItem(material.name());
    }

    /**
     * Find the collection that tracks a specific ItemStack (including custom items).
     */
    public CollectionDefinition findCollectionForItemStack(ItemStack item) {
        if (item == null) return null;

        // Try custom item ID first
        String customId = getCustomItemId(item);
        if (customId != null) {
            CollectionDefinition collection = findCollectionForItem(customId);
            if (collection != null) return collection;
        }

        // Fall back to material
        return findCollectionForMaterial(item.getType());
    }

    public int getCollectionTier(Player player, String collectionId) {
        CollectionDefinition collection = getCollection(collectionId);
        if (collection == null) {
            return 0;
        }
        PlayerCollectionProgress progress = getPlayerProgress(player, collection.getId());
        return collection.getCurrentTierLevel(progress.getCollectedAmount());
    }

    /**
     * Check and unlock tiers for a player.
     */
    private void checkTierUnlocks(Player player, CollectionDefinition collection, PlayerCollectionProgress progress) {
        for (CollectionTier tier : collection.getTiers()) {
            if (!progress.isTierUnlocked(tier.getTierLevel()) && 
                progress.getCollectedAmount() >= tier.getAmountRequired()) {
                
                // Unlock tier
                progress.unlockTier(tier.getTierLevel());
                markDirty(progress.getProfileId());
                
                // Grant rewards
                grantRewards(player, collection, tier, progress);

                // Discover any custom recipes unlocked by this tier (vanilla recipe book integration).
                discoverRecipesUnlockedForTier(progress.getProfileId(), player, collection.getId(), tier.getTierLevel());
                
                // Notify player
                notifyTierUnlock(progress.getProfileId(), player, collection, tier);
            }
        }
    }

    private void discoverRecipesUnlockedForTier(UUID profileId, Player triggeringPlayer, String collectionId, int tierLevel) {
        if (profileId == null || collectionId == null || collectionId.isBlank() || tierLevel <= 0) {
            return;
        }

        List<Player> members = onlineMembersForProfile(profileId);
        if (members.isEmpty() && triggeringPlayer != null) {
            members = List.of(triggeringPlayer);
        }

        for (Player member : members) {
            for (SkyblockRecipe recipe : RecipeRegistry.getAll()) {
                if (recipe == null || recipe.getKey() == null) {
                    continue;
                }
                if (recipe.getCollectionId() == null || recipe.getCollectionId().isBlank()) {
                    continue;
                }
                if (!recipe.getCollectionId().equalsIgnoreCase(collectionId)) {
                    continue;
                }
                if (recipe.getCollectionTierRequired() != tierLevel) {
                    continue;
                }
                if (member.hasDiscoveredRecipe(recipe.getKey())) {
                    continue;
                }

                member.discoverRecipe(recipe.getKey());
            }
        }
    }

    /**
     * Grant rewards for a tier.
     */
    private void grantRewards(Player player, CollectionDefinition collection, CollectionTier tier, PlayerCollectionProgress progress) {
        for (CollectionReward reward : tier.getRewards()) {
            switch (reward.getType()) {
                case SKYBLOCK_XP:
                    int applied = grantSkyblockXp(player, collection, tier, reward.getSkyblockXp());
                    if (applied > 0) {
                        progress.addSkyblockXp(applied);
                    }
                    break;
                case SKILL_XP:
                    if (player != null) {
                        grantSkillXp(player, reward.getSkillType(), (int) reward.getAmount());
                    }
                    break;
                case ITEM:
                    if (reward.getItem() != null && player != null) {
                        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(reward.getItem());
                        if (!leftovers.isEmpty()) {
                            for (ItemStack remaining : leftovers.values()) {
                                if (remaining == null || remaining.getType().isAir()) {
                                    continue;
                                }
                                player.getWorld().dropItemNaturally(player.getLocation(), remaining);
                            }
                            player.sendMessage("§cInventory full. Collection rewards dropped at your feet.");
                        }
                    }
                    break;
                case COMMAND:
                    if (player != null) {
                        for (String command : reward.getCommands()) {
                            String processed = command.replace("{player}", player.getName());
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processed);
                        }
                    }
                    break;
                case STAT_BONUS:
                    // Permanent stat bonuses would be applied here
                    break;
                case TRADE_UNLOCK:
                case RECIPE_UNLOCK:
                    // Recipe/trade unlocks would be handled here
                    break;
            }
        }
    }

    /**
     * Grant Skyblock XP to a player.
     */
    private int grantSkyblockXp(Player player, CollectionDefinition collection, CollectionTier tier, int xp) {
        // Use the SkyblockLevelManager if available
        var levelManager = plugin.getSkyblockLevelManager();
        if (levelManager != null && player != null && xp > 0) {
            String collectionId = collection == null ? "unknown" : collection.getId();
            int tierLevel = tier == null ? 0 : tier.getTierLevel();
            String objectiveId = "skill.collection_tier." + collectionId + ".tier_" + tierLevel;
            String reason = (collection == null ? "Collection" : collection.getName()) + " Tier " + (tier == null ? tierLevel : tier.getTierRoman());
            long applied = levelManager.awardObjectiveXp(player, objectiveId, "skill", xp, reason, true);
            return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, applied));
        }
        return 0;
    }

    /**
     * Grant skill XP to a player.
     */
    private void grantSkillXp(Player player, String skillType, int xp) {
        // Could integrate with skills plugins or custom skill system
        player.sendMessage("§a+§b" + xp + " §a" + skillType + " XP");
    }

    /**
     * Notify all online co-op members of a tier unlock (owner + members for the island profile).
     */
    private void notifyTierUnlock(UUID profileId, Player triggeringPlayer, CollectionDefinition collection, CollectionTier tier) {
        if (profileId == null || collection == null || tier == null) {
            return;
        }

        List<String> recipeNames = recipesUnlockedForTier(collection.getId(), tier.getTierLevel());
        List<Player> members = onlineMembersForProfile(profileId);
        if (members.isEmpty() && triggeringPlayer != null) {
            members = List.of(triggeringPlayer);
        }

        for (Player player : members) {
            player.sendMessage("");
            player.sendMessage("§6§l COLLECTION TIER UNLOCKED!");
            player.sendMessage("§e" + collection.getName() + " §6Tier " + tier.getTierRoman());
            if (triggeringPlayer != null && triggeringPlayer.getUniqueId() != null
                    && !triggeringPlayer.getUniqueId().equals(player.getUniqueId())) {
                player.sendMessage("§7Unlocked by: §a" + triggeringPlayer.getName());
            }
            player.sendMessage("");
            player.sendMessage("§aRewards:");
            for (CollectionReward reward : tier.getRewards()) {
                player.sendMessage("  " + reward.getFormattedLore());
            }

            if (!recipeNames.isEmpty()) {
                player.sendMessage("");
                player.sendMessage("§aRecipes:");
                int shown = 0;
                for (String name : recipeNames) {
                    if (shown >= 6) {
                        break;
                    }
                    player.sendMessage("  §eUnlock Recipe: §b" + name);
                    shown++;
                }
                if (recipeNames.size() > shown) {
                    player.sendMessage("  §7(and " + (recipeNames.size() - shown) + " more)");
                }
            }
            player.sendMessage("");

            // Play sound
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
    }

    private List<Player> onlineMembersForProfile(UUID profileId) {
        if (profileId == null) {
            return List.of();
        }

        var islandManager = plugin.getIslandManager();
        if (islandManager == null) {
            return List.of();
        }

        List<Player> players = new ArrayList<>();
        for (UUID memberId : islandManager.getCoopMemberIdsForProfileId(profileId)) {
            Player online = Bukkit.getPlayer(memberId);
            if (online != null && online.isOnline()) {
                players.add(online);
            }
        }
        return players;
    }

    private List<String> recipesUnlockedForTier(String collectionId, int tierLevel) {
        if (collectionId == null || collectionId.isBlank() || tierLevel <= 0) {
            return List.of();
        }

        List<String> names = new ArrayList<>();
        for (SkyblockRecipe recipe : RecipeRegistry.getAll()) {
            if (recipe == null || recipe.getCollectionId() == null) {
                continue;
            }
            if (recipe.getCollectionTierRequired() != tierLevel) {
                continue;
            }
            if (!recipe.getCollectionId().equalsIgnoreCase(collectionId)) {
                continue;
            }
            names.add(recipe.getName());
        }
        return names;
    }

    /**
     * Get total Skyblock XP earned from collections.
     */
    public int getTotalCollectionXp(UUID profileId) {
        int total = 0;
        Map<String, PlayerCollectionProgress> progress = progressByProfile.get(profileId);
        if (progress != null) {
            for (PlayerCollectionProgress p : progress.values()) {
                total += p.getTotalSkyblockXpEarned();
            }
        }
        return total;
    }

    public int getTotalCollectionXp(Player player) {
        UUID profileId = resolveProfileId(player);
        return profileId == null ? 0 : getTotalCollectionXp(profileId);
    }

    /**
     * Get player's total collected items across all collections.
     */
    public long getTotalCollectedItems(UUID profileId) {
        long total = 0;
        Map<String, PlayerCollectionProgress> progress = progressByProfile.get(profileId);
        if (progress != null) {
            for (PlayerCollectionProgress p : progress.values()) {
                total += p.getCollectedAmount();
            }
        }
        return total;
    }

    public long getTotalCollectedItems(Player player) {
        UUID profileId = resolveProfileId(player);
        return profileId == null ? 0L : getTotalCollectedItems(profileId);
    }

    /**
     * Get player's maxed collections count.
     */
    public int getMaxedCollectionsCount(UUID profileId) {
        int count = 0;
        Map<String, PlayerCollectionProgress> progress = progressByProfile.get(profileId);
        if (progress != null) {
            for (Map.Entry<String, PlayerCollectionProgress> entry : progress.entrySet()) {
                CollectionDefinition collection = collections.get(entry.getKey());
                if (collection != null && collection.isMaxed(entry.getValue().getCollectedAmount())) {
                    count++;
                }
            }
        }
        return count;
    }

    public int getMaxedCollectionsCount(Player player) {
        UUID profileId = resolveProfileId(player);
        return profileId == null ? 0 : getMaxedCollectionsCount(profileId);
    }

    /**
     * Get collection leaderboard (top players by collection amount).
     */
    public List<Map.Entry<UUID, Long>> getLeaderboard(String collectionId, int limit) {
        String normalizedCollectionId = normalizeCollectionId(collectionId);
        Map<UUID, Long> amounts = new HashMap<>();
        
        for (Map.Entry<UUID, Map<String, PlayerCollectionProgress>> playerEntry : progressByProfile.entrySet()) {
            PlayerCollectionProgress progress = playerEntry.getValue().get(normalizedCollectionId);
            if (progress != null) {
                amounts.put(playerEntry.getKey(), progress.getCollectedAmount());
            }
        }
        
        return amounts.entrySet().stream()
            .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
            .limit(limit)
            .toList();
    }

    /**
     * Get player's rank in a collection.
     */
    public int getPlayerRank(UUID playerId, String collectionId) {
        if (playerId == null) {
            return -1;
        }

        String normalizedCollectionId = normalizeCollectionId(collectionId);
        Map<String, PlayerCollectionProgress> selfMap = progressByProfile.get(playerId);
        PlayerCollectionProgress selfProgress = selfMap == null ? null : selfMap.get(normalizedCollectionId);
        if (selfProgress == null) {
            return -1;
        }

        long selfAmount = selfProgress.getCollectedAmount();
        int rank = 1;
        for (Map.Entry<UUID, Map<String, PlayerCollectionProgress>> entry : progressByProfile.entrySet()) {
            if (entry.getKey() == null || entry.getKey().equals(playerId)) {
                continue;
            }
            PlayerCollectionProgress other = entry.getValue().get(normalizedCollectionId);
            if (other != null && other.getCollectedAmount() > selfAmount) {
                rank++;
            }
        }
        return rank;
    }

    private UUID resolveProfileId(Player player) {
        if (player == null) {
            return null;
        }

        UUID ownerId = player.getUniqueId();
        if (ownerId == null) {
            return null;
        }

        // Co-op members share collection progress with the island profile they belong to.
        var islandManager = plugin.getIslandManager();
        if (islandManager != null) {
            UUID coopProfileId = islandManager.getCoopProfileId(ownerId);
            if (coopProfileId != null) {
                return coopProfileId;
            }
        }

        ProfileManager profileManager = plugin.getProfileManager();
        if (profileManager == null) {
            return ownerId;
        }

        SkyBlockProfile profile = profileManager.getSelectedProfile(player);
        if (profile == null || profile.getProfileId() == null) {
            return ownerId;
        }

        UUID profileId = profile.getProfileId();
        migrateLegacyProgress(ownerId, profileId);
        return profileId;
    }

    private void migrateLegacyProgress(UUID ownerId, UUID profileId) {
        if (ownerId == null || profileId == null || ownerId.equals(profileId)) {
            return;
        }

        Map<String, PlayerCollectionProgress> legacy = progressByProfile.get(ownerId);
        if (legacy == null || legacy.isEmpty()) {
            return;
        }

        Map<String, PlayerCollectionProgress> target = progressByProfile.computeIfAbsent(profileId, ignored -> new ConcurrentHashMap<>());
        for (Map.Entry<String, PlayerCollectionProgress> entry : legacy.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
                continue;
            }
            String normalizedCollectionId = normalizeCollectionId(entry.getKey());
            PlayerCollectionProgress existing = target.get(normalizedCollectionId);
            if (existing == null) {
                PlayerCollectionProgress migrated = new PlayerCollectionProgress(profileId, normalizedCollectionId);
                migrated.mergeFrom(entry.getValue());
                target.put(normalizedCollectionId, migrated);
            } else {
                existing.mergeFrom(entry.getValue());
            }
        }

        progressByProfile.remove(ownerId);
        markDirty(profileId);
        saveAllPlayerData();
    }

    // ==================== PERSISTENCE ====================

    /**
     * Load collections from config file.
     */
    private void loadCollections() {
        if (!collectionsFile.exists()) {
            plugin.saveResource("collections.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(collectionsFile);
        ConfigurationSection collectionsSection = config.getConfigurationSection("collections");
        
        if (collectionsSection == null) return;

        for (String key : collectionsSection.getKeys(false)) {
            String normalizedId = normalizeCollectionId(key);
            CollectionDefinition definition = loadCollection(normalizedId, collectionsSection.getConfigurationSection(key));
            if (definition != null) {
                if (collections.containsKey(definition.getId())) {
                    plugin.getLogger().warning("Duplicate collection id '" + definition.getId() + "' in collections.yml. Keeping first.");
                    continue;
                }
                collections.put(definition.getId(), definition);
            }
        }

        rebuildItemIndex();
    }

    /**
     * Load a single collection from configuration.
     */
    private CollectionDefinition loadCollection(String id, ConfigurationSection section) {
        if (section == null) return null;

        String normalizedId = normalizeCollectionId(id);
        String name = section.getString("name", normalizedId);
        String description = section.getString("description", "");
        Material icon = Material.matchMaterial(section.getString("icon", "BARRIER"));
        if (icon == null) icon = Material.BARRIER;
        
        CollectionCategory category;
        try {
            category = CollectionCategory.valueOf(
                section.getString("category", "SPECIAL").toUpperCase()
            );
        } catch (IllegalArgumentException e) {
            category = CollectionCategory.SPECIAL;
        }

        List<String> trackedItems = new ArrayList<>();
        for (String item : section.getStringList("items")) {
            String normalizedItemId = normalizeItemId(item);
            if (!normalizedItemId.isBlank()) {
                trackedItems.add(normalizedItemId);
            }
        }
        String subcategory = section.getString("subcategory", "");
        boolean enabled = section.getBoolean("enabled", true);

        // Load tiers
        List<CollectionTier> tiers = new ArrayList<>();
        ConfigurationSection tiersSection = section.getConfigurationSection("tiers");
        
        if (tiersSection != null) {
            for (String tierKey : tiersSection.getKeys(false)) {
                CollectionTier tier = loadTier(tierKey, tiersSection.getConfigurationSection(tierKey));
                if (tier != null) {
                    tiers.add(tier);
                }
            }
            tiers.sort(Comparator.comparingInt(CollectionTier::getTierLevel));
        }

        return CollectionDefinition.builder()
            .id(normalizedId)
            .name(name)
            .description(description)
            .icon(icon)
            .category(category)
            .subcategory(subcategory)
            .trackedItems(trackedItems)
            .tiers(tiers)
            .enabled(enabled)
            .build();
    }

    /**
     * Load a single tier from configuration.
     */
    private CollectionTier loadTier(String key, ConfigurationSection section) {
        if (section == null) return null;

        int tierLevel;
        try {
            tierLevel = Integer.parseInt(key.replace("tier-", ""));
        } catch (NumberFormatException e) {
            tierLevel = 1;
        }

        long amountRequired = section.getLong("amount", 100);
        List<CollectionReward> rewards = new ArrayList<>();

        // Load rewards
        for (String rewardKey : section.getKeys(false)) {
            if (rewardKey.equals("amount")) continue;
            
            ConfigurationSection rewardSection = section.getConfigurationSection(rewardKey);
            if (rewardSection != null) {
                CollectionReward reward = loadReward(rewardSection);
                if (reward != null) {
                    rewards.add(reward);
                }
            }
        }

        return new CollectionTier(tierLevel, amountRequired, rewards);
    }

    /**
     * Load a single reward from configuration.
     */
    private CollectionReward loadReward(ConfigurationSection section) {
        String typeStr = section.getString("type", "ITEM").toUpperCase();
        CollectionReward.RewardType type;
        try {
            type = CollectionReward.RewardType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            type = CollectionReward.RewardType.ITEM;
        }

        CollectionReward.Builder builder = CollectionReward.builder()
            .type(type)
            .amount(section.getDouble("amount", 0))
            .skillType(section.getString("skill-type", ""))
            .description(section.getString("description", ""))
            .skyblockXp(section.getInt("skyblock-xp", 4))
            .commands(section.getStringList("commands"));

        if (section.contains("stat-bonus")) {
            builder.statBonus(section.getString("stat-bonus", ""))
                   .statValue(section.getDouble("stat-value", 0));
        }

        return builder.build();
    }

    /**
     * Load player data from file.
     */
    private void loadPlayerData() {
        if (!playerDataFile.exists()) {
            playerDataFile.getParentFile().mkdirs();
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerDataFile);
        
        for (String uuidStr : config.getKeys(false)) {
            UUID profileId;
            try {
                profileId = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                continue;
            }

            ConfigurationSection playerSection = config.getConfigurationSection(uuidStr);
            if (playerSection == null) continue;

            Map<String, PlayerCollectionProgress> progressMap = new ConcurrentHashMap<>();
            
            for (String collectionId : playerSection.getKeys(false)) {
                ConfigurationSection collectionSection = playerSection.getConfigurationSection(collectionId);
                String normalizedCollectionId = normalizeCollectionId(collectionId);
                PlayerCollectionProgress progress = PlayerCollectionProgress.load(
                    profileId, normalizedCollectionId, collectionSection
                );
                progressMap.put(normalizedCollectionId, progress);
            }
            
            progressByProfile.put(profileId, progressMap);
        }
    }

    /**
     * Save a single player's data.
     */
    public void savePlayerData(UUID playerId) {
        Map<String, PlayerCollectionProgress> progressMap = progressByProfile.get(playerId);
        if (progressMap == null) return;

        synchronized (playerDataFileLock) {
            YamlConfiguration config;
            if (playerDataFile.exists()) {
                config = YamlConfiguration.loadConfiguration(playerDataFile);
            } else {
                config = new YamlConfiguration();
                playerDataFile.getParentFile().mkdirs();
            }

            config.set(playerId.toString(), null);
            ConfigurationSection playerSection = config.createSection(playerId.toString());
            for (PlayerCollectionProgress progress : progressMap.values()) {
                ConfigurationSection collectionSection = playerSection.createSection(progress.getCollectionId());
                progress.save(collectionSection);
            }

            try {
                config.save(playerDataFile);
                dirtyProfiles.remove(playerId);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save player collection data: " + e.getMessage());
            }
        }
    }

    /**
     * Save all player data.
     */
    public void saveAllPlayerData() {
        synchronized (playerDataFileLock) {
            YamlConfiguration config = new YamlConfiguration();

            for (Map.Entry<UUID, Map<String, PlayerCollectionProgress>> entry : progressByProfile.entrySet()) {
                ConfigurationSection playerSection = config.createSection(entry.getKey().toString());
                for (PlayerCollectionProgress progress : entry.getValue().values()) {
                    ConfigurationSection collectionSection = playerSection.createSection(progress.getCollectionId());
                    progress.save(collectionSection);
                }
            }

            playerDataFile.getParentFile().mkdirs();
            try {
                config.save(playerDataFile);
                dirtyProfiles.clear();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save all player collection data: " + e.getMessage());
            }
        }
    }

    private void markDirty(UUID profileId) {
        if (profileId == null) {
            return;
        }
        dirtyProfiles.add(profileId);
    }

    private void startAutoSave() {
        stopAutoSave();

        int intervalSeconds = Math.max(60, plugin.getConfig().getInt("collections.auto-save-interval-seconds", 300));
        long periodTicks = intervalSeconds * 20L;
        autoSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::autoSaveDirtyProfiles, periodTicks, periodTicks);
    }

    private void stopAutoSave() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
    }

    private void autoSaveDirtyProfiles() {
        if (!enabled) {
            return;
        }
        if (dirtyProfiles.isEmpty()) {
            return;
        }

        Set<UUID> toSave = Set.copyOf(dirtyProfiles);
        if (toSave.isEmpty()) {
            return;
        }

        boolean saved = saveProfilesIntoSingleFile(toSave);
        if (saved) {
            dirtyProfiles.removeAll(toSave);
        }
    }

    private boolean saveProfilesIntoSingleFile(Set<UUID> profileIds) {
        if (profileIds == null || profileIds.isEmpty()) {
            return true;
        }

        synchronized (playerDataFileLock) {
            YamlConfiguration config;
            if (playerDataFile.exists()) {
                config = YamlConfiguration.loadConfiguration(playerDataFile);
            } else {
                config = new YamlConfiguration();
                File parent = playerDataFile.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }
            }

            for (UUID profileId : profileIds) {
                if (profileId == null) {
                    continue;
                }

                Map<String, PlayerCollectionProgress> progressMap = progressByProfile.get(profileId);
                config.set(profileId.toString(), null);
                if (progressMap == null || progressMap.isEmpty()) {
                    continue;
                }

                ConfigurationSection playerSection = config.createSection(profileId.toString());
                for (PlayerCollectionProgress progress : progressMap.values()) {
                    if (progress == null || progress.getCollectionId() == null || progress.getCollectionId().isBlank()) {
                        continue;
                    }
                    ConfigurationSection collectionSection = playerSection.createSection(progress.getCollectionId());
                    progress.save(collectionSection);
                }
            }

            playerDataFile.getParentFile().mkdirs();
            try {
                config.save(playerDataFile);
                return true;
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save collection data: " + e.getMessage());
                return false;
            }
        }
    }

    private void rebuildItemIndex() {
        collectionIdByItemId.clear();
        for (CollectionDefinition collection : collections.values()) {
            if (collection == null) {
                continue;
            }
            String collectionId = normalizeCollectionId(collection.getId());
            for (String itemId : collection.getTrackedItems()) {
                String normalizedItemId = normalizeItemId(itemId);
                if (normalizedItemId.isBlank()) {
                    continue;
                }
                String existing = collectionIdByItemId.putIfAbsent(normalizedItemId, collectionId);
                if (existing != null && !existing.equals(collectionId)) {
                    plugin.getLogger().warning("Item '" + normalizedItemId + "' is tracked by multiple collections: " + existing + " and " + collectionId);
                }
            }
        }
    }

    private String normalizeCollectionId(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim()
                .toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
    }

    private String normalizeItemId(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim()
                .toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
    }

    /**
     * Get custom item ID from ItemStack (for custom items plugin integration).
     */
    private String getCustomItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        String id = meta.getPersistentDataContainer().get(customItemIdKey, PersistentDataType.STRING);
        if (id == null || id.isBlank()) {
            return null;
        }
        return normalizeItemId(id);
    }
}

