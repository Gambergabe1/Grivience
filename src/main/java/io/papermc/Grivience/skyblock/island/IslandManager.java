package io.papermc.Grivience.skyblock.island;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skyblock.economy.ProfileEconomyService;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class IslandManager {
    private final GriviencePlugin plugin;
    private final ProfileEconomyService profileEconomy;
    private final Map<UUID, List<UUID>> ownerProfiles = new ConcurrentHashMap<>();
    private final Map<UUID, Island> islandsById = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerToIsland = new ConcurrentHashMap<>();

    // Fast lookup for co-op member -> co-op profile id (island profile id).
    // This is used by systems like Collections to share progress across co-op members.
    private final Map<UUID, UUID> coopProfileByMember = new ConcurrentHashMap<>();

    // Fast lookup for profile id -> island id (the island representing that profile).
    private final Map<UUID, UUID> islandByProfileId = new ConcurrentHashMap<>();

    private World islandWorld;
    private int islandSpacing;
    private int startingSize;
    private List<Integer> upgradeSizes;
    private Map<Integer, Double> upgradeCosts;
    private int islandSpawnY;

    public IslandManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.profileEconomy = new ProfileEconomyService(plugin);
        loadConfig();
        loadIslands();
    }

    public void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection skyblockSection = config.getConfigurationSection("skyblock");

        if (skyblockSection == null) {
            plugin.getLogger().warning("Skyblock config section not found. Using defaults.");
            islandSpacing = 100;
            startingSize = 32;
            upgradeSizes = Arrays.asList(32, 48, 64, 80, 96, 112, 128);
            upgradeCosts = Map.of(
                    2, 5000.0,
                    3, 15000.0,
                    4, 30000.0,
                    5, 50000.0,
                    6, 75000.0,
                    7, 100000.0
            );
            islandSpawnY = 80;
            return;
        }

        // Store old values before reloading (to prevent island resets)
        int oldSpacing = islandSpacing;
        
        islandSpacing = skyblockSection.getInt("island-spacing", 100);
        startingSize = skyblockSection.getInt("starting-size", 32);
        islandSpawnY = skyblockSection.getInt("island-spawn-y", 80);
        upgradeSizes = skyblockSection.getIntegerList("upgrade-sizes");
        if (upgradeSizes.isEmpty()) {
            upgradeSizes = Arrays.asList(32, 48, 64, 80, 96, 112, 128);
        }

        upgradeCosts = new HashMap<>();
        ConfigurationSection costsSection = skyblockSection.getConfigurationSection("upgrade-costs");
        if (costsSection != null) {
            for (String key : costsSection.getKeys(false)) {
                try {
                    int level = Integer.parseInt(key);
                    upgradeCosts.put(level, costsSection.getDouble(key));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (upgradeCosts.isEmpty()) {
            upgradeCosts = Map.of(
                    2, 5000.0,
                    3, 15000.0,
                    4, 30000.0,
                    5, 50000.0,
                    6, 75000.0,
                    7, 100000.0
            );
        }
        
        // Log that islands are preserved
        plugin.getLogger().info("Island configuration reloaded. " + islandsById.size() + " islands preserved.");
    }

    public void initializeWorld() {
        String worldName = plugin.getConfig().getString("skyblock.world-name", "skyblock_world");
        islandWorld = Bukkit.getWorld(worldName);

        // Check if world exists and is properly configured as void world
        if (islandWorld != null) {
            // Verify world generator is correct
            if (islandWorld.getGenerator() == null || !(islandWorld.getGenerator() instanceof SkyblockWorldGenerator)) {
                plugin.getLogger().warning("Skyblock world '" + worldName + "' exists but does not have the void generator!");
                plugin.getLogger().warning("The plugin will continue, but new chunks might generate natural terrain.");
            }
            plugin.getLogger().info("Skyblock world loaded: " + worldName);
            configureSkyBlockWorld();
            return;
        }

        // World doesn't exist - create new void world
        plugin.getLogger().info("Creating new Skyblock void world: " + worldName);
        WorldCreator creator = new WorldCreator(worldName);
        creator.environment(World.Environment.NORMAL);
        creator.generateStructures(false);
        creator.generator(new SkyblockWorldGenerator());
        try {
            creator.type(org.bukkit.WorldType.FLAT);
        } catch (Exception ignored) {
            // Some versions might not support this or it might be redundant with generator
        }
        
        islandWorld = Bukkit.createWorld(creator);

        if (islandWorld != null) {
            configureSkyBlockWorld();
            plugin.getLogger().info("Skyblock void world created successfully!");
            plugin.getLogger().info("World properties:");
            plugin.getLogger().info("  - Name: " + worldName);
            plugin.getLogger().info("  - Environment: NORMAL");
            plugin.getLogger().info("  - Type: FLAT (Void)");
            plugin.getLogger().info("  - Structures: DISABLED");
            plugin.getLogger().info("  - Terrain Generation: VOID");
            plugin.getLogger().info("  - Mob Spawning: ENABLED (for player islands)");
        } else {
            plugin.getLogger().severe("Failed to create Skyblock world: " + worldName);
        }
    }

    /**
     * Configure Skyblock world settings for optimal gameplay.
     */
    private void configureSkyBlockWorld() {
        if (islandWorld == null) return;
        
        // Set time to noon and freeze it if desired (optional, but keep it noon for now)
        islandWorld.setTime(6000);
        
        // Disable weather cycles
        islandWorld.setWeatherDuration(0);
        islandWorld.setStorm(false);
        islandWorld.setThundering(false);
        
        // Disable natural mob spawning (islands handle their own)
        islandWorld.setSpawnFlags(false, false);

        // Standard Skyblock gamerules
        islandWorld.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, true);
        islandWorld.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, false);
        islandWorld.setGameRule(org.bukkit.GameRule.MOB_GRIEFING, false);
        islandWorld.setGameRule(org.bukkit.GameRule.DO_FIRE_TICK, false);
        islandWorld.setGameRule(org.bukkit.GameRule.DO_INSOMNIA, false);
        islandWorld.setGameRule(org.bukkit.GameRule.DO_PATROL_SPAWNING, false);
        islandWorld.setGameRule(org.bukkit.GameRule.DO_TRADER_SPAWNING, false);
        islandWorld.setGameRule(org.bukkit.GameRule.ANNOUNCE_ADVANCEMENTS, false);
        
        // Set spawn location to 0,100,0 (safe area above void)
        Location spawnLoc = new Location(islandWorld, 0.5, 100, 0.5);
        islandWorld.setSpawnLocation(spawnLoc);
        
        plugin.getLogger().info("Skyblock world configured: time=6000, weather=clear, spawn=(0,100,0), gamerules updated");
    }

    public World getIslandWorld() {
        if (islandWorld == null) {
            initializeWorld();
        }
        return islandWorld;
    }

    public Island createIsland(Player player) {
        SkyBlockProfile profile = profileEconomy.requireSelectedProfile(player);
        if (profile == null) {
            return null;
        }
        return createIslandForProfile(player, profile);
    }

    public Island createIsland(Player player, String profileName) {
        if (player == null) {
            return null;
        }
        String requestedName = profileName == null ? "" : profileName.trim();
        if (requestedName.isEmpty()) {
            return createIsland(player);
        }

        // Legacy entrypoint: treat the name as a Skyblock profile name and ensure it is selected.
        if (plugin.getProfileManager() != null) {
            SkyBlockProfile profile = plugin.getProfileManager().getProfile(player, requestedName);
            if (profile == null) {
                profile = plugin.getProfileManager().createProfile(player, requestedName);
                if (profile == null) {
                    return null;
                }
            }
            plugin.getProfileManager().selectProfile(player, profile.getProfileId());
            return createIslandForProfile(player, profile);
        }

        SkyBlockProfile profile = profileEconomy.requireSelectedProfile(player);
        if (profile == null) {
            return null;
        }
        return createIslandForProfile(player, profile);
    }

    private Island createIslandForProfile(Player player, SkyBlockProfile profile) {
        if (player == null || profile == null) {
            return null;
        }
        UUID owner = player.getUniqueId();
        Island existing = findIslandForOwnerProfile(owner, profile.getProfileId(), profile.getProfileName());
        if (existing != null) {
            playerToIsland.put(owner, existing.getId());
            Location spawn = getSafeSpawnLocation(existing);
            if (spawn != null) {
                player.teleport(spawn);
                player.setBedSpawnLocation(spawn, true);
            }
            player.sendMessage(ChatColor.YELLOW + "You already have an island for this profile.");
            return existing;
        }

        if (islandWorld == null) {
            initializeWorld();
        }

        if (islandWorld == null) {
            player.sendMessage(ChatColor.RED + "Failed to create island world.");
            return null;
        }

        Location spawnLocation = findAvailableIslandLocation();
        if (spawnLocation == null) {
            player.sendMessage(ChatColor.RED + "No available island locations. Try again later.");
            return null;
        }

        Island island = new Island(owner, player.getName(), spawnLocation);
        island.setProfileId(profile.getProfileId());
        island.setProfileName(profile.getProfileName());
        island.setSize(startingSize);
        island.setGuestLimit(computeGuestLimit(player));
        island.setVisitPolicy(loadDefaultVisitPolicy());

        ownerProfiles.computeIfAbsent(owner, k -> new ArrayList<>()).add(island.getId());
        islandsById.put(island.getId(), island);
        playerToIsland.put(owner, island.getId());

        // Generate island schematic/platform FIRST
        IslandGenerator.generateIsland(island);

        // Wait a tick for schematic to fully paste (if using WorldEdit)
        Bukkit.getScheduler().runTask(plugin, () -> {
            // Set default island spawn to the platform center (searching a safe spot around center) and persist.
            Location defaultSpawn = findCenterSafeSpawn(island);
            island.setSpawnPoint(defaultSpawn);
            saveIsland(island);

            // Teleport player AFTER schematic is generated
            player.teleport(defaultSpawn);
            player.setBedSpawnLocation(defaultSpawn, true);
            loadProfileInventory(player, profile.getProfileId(), profile.getProfileName());

            player.sendMessage(ChatColor.GREEN + "Your island has been created!");
            player.sendMessage(ChatColor.GRAY + "Size: " + startingSize + "x" + startingSize);
            player.sendMessage(ChatColor.GRAY + "Use /island go to return anytime.");
            if (plugin.getSkyblockLevelManager() != null) {
                plugin.getSkyblockLevelManager().recordIslandCreated(player);
            }
        });

        return island;
    }

    public Location getSafeGuestSpawnLocation(Island island) {
        if (island == null || island.getCenter() == null) return null;

        Location guestSpawn = island.getGuestSpawnPoint();
        if (guestSpawn != null) {
            Location safe = WorldHelper.findSafeLocation(guestSpawn);
            if (safe != null) {
                return safe;
            }
        }

        return getSafeSpawnLocation(island);
    }

    public void handleSkyBlockProfileSwitch(Player player, UUID previousProfileId, SkyBlockProfile newProfile) {
        if (player == null || newProfile == null) {
            return;
        }

        if (previousProfileId != null && !previousProfileId.equals(newProfile.getProfileId())) {
            saveProfileInventory(player, previousProfileId);
        }

        UUID owner = player.getUniqueId();
        Island island = findIslandForOwnerProfile(owner, newProfile.getProfileId(), null);
        if (island == null) {
            island = findIslandForOwnerProfile(owner, null, newProfile.getProfileName());
            if (island != null && island.getProfileId() == null) {
                island.setProfileId(newProfile.getProfileId());
                saveIsland(island);
            }
        }

        loadProfileInventory(player, newProfile.getProfileId(), newProfile.getProfileName());

        if (island != null) {
            playerToIsland.put(owner, island.getId());
            Location spawn = getSafeSpawnLocation(island);
            if (spawn != null) {
                player.teleport(spawn);
                player.setBedSpawnLocation(spawn, true);
            }
            return;
        }

        // If no island exists for this profile, create one automatically
        plugin.getLogger().info("No island found for profile '" + newProfile.getProfileName() + "' (" + owner + "). Creating one automatically...");
        createIsland(player);
    }

    public int computeGuestLimit(Player owner) {
        int limit = Math.max(1, plugin.getConfig().getInt("skyblock.visiting.guest-limit.default", 1));
        if (owner == null) {
            return limit;
        }

        if (owner.hasPermission("grivience.visit.guestlimit.unlimited")) {
            return -1;
        }

        // Prefer list format to avoid Bukkit's '.' path handling in YAML keys.
        List<String> permissionLimits = plugin.getConfig().getStringList("skyblock.visiting.guest-limit.permission-limits");
        if (permissionLimits != null && !permissionLimits.isEmpty()) {
            for (String entry : permissionLimits) {
                if (entry == null) {
                    continue;
                }
                String trimmed = entry.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                String[] parts = trimmed.split("=", 2);
                if (parts.length != 2) {
                    continue;
                }
                String perm = parts[0].trim();
                if (perm.isEmpty()) {
                    continue;
                }
                int val;
                try {
                    val = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException ignored) {
                    continue;
                }
                if (val != 0 && owner.hasPermission(perm)) {
                    limit = Math.max(limit, val);
                }
            }
            return limit;
        }

        // Default Hypixel-like mapping if no config is present.
        if (owner.hasPermission("grivience.visit.guestlimit.youtuber")) {
            limit = Math.max(limit, 15);
        }
        if (owner.hasPermission("grivience.visit.guestlimit.mvpplus")) {
            limit = Math.max(limit, 7);
        }
        if (owner.hasPermission("grivience.visit.guestlimit.mvp")) {
            limit = Math.max(limit, 5);
        }
        if (owner.hasPermission("grivience.visit.guestlimit.vip")) {
            limit = Math.max(limit, 3);
        }
        return limit;
    }

    private Island.VisitPolicy loadDefaultVisitPolicy() {
        String raw = plugin.getConfig().getString("skyblock.visiting.default-policy", "OFF");
        if (raw == null || raw.isBlank()) {
            return Island.VisitPolicy.OFF;
        }
        try {
            return Island.VisitPolicy.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return Island.VisitPolicy.OFF;
        }
    }

    private Location findAvailableIslandLocation() {
        if (islandWorld == null) return null;

        int maxRadius = 10000;
        Random random = new Random();

        for (int attempt = 0; attempt < 100; attempt++) {
            int angle = random.nextInt(360);
            int distance = islandSpacing + (int) (Math.random() * maxRadius);

            int x = (int) (Math.cos(Math.toRadians(angle)) * distance);
            int z = (int) (Math.sin(Math.toRadians(angle)) * distance);

            x = (x / islandSpacing) * islandSpacing;
            z = (z / islandSpacing) * islandSpacing;

            Location center = new Location(islandWorld, x + 0.5, islandSpawnY, z + 0.5);

            if (!isLocationOccupied(center)) {
                return center;
            }
        }

        int searchRadius = maxRadius + islandSpacing;
        for (int x = -searchRadius; x <= searchRadius; x += islandSpacing) {
            for (int z = -searchRadius; z <= searchRadius; z += islandSpacing) {
                Location center = new Location(islandWorld, x + 0.5, islandSpawnY, z + 0.5);
                if (!isLocationOccupied(center)) {
                    return center;
                }
            }
        }

        return null;
    }

    private boolean isLocationOccupied(Location center) {
        for (Island island : islandsById.values()) {
            Location existing = island.getCenter();
            if (existing == null || !existing.getWorld().equals(center.getWorld())) {
                continue;
            }

            double distance = center.distance(existing);
            return distance < islandSpacing;
        }
        return false;
    }

    public boolean hasIsland(UUID playerUuid) {
        List<UUID> profiles = ownerProfiles.get(playerUuid);
        return (profiles != null && !profiles.isEmpty()) || playerToIsland.containsKey(playerUuid);
    }

    public boolean hasIsland(Player player) {
        return getIsland(player) != null;
    }

    public Island getIsland(Player player) {
        if (player == null) {
            return null;
        }

        SkyBlockProfile profile = profileEconomy.getSelectedProfile(player);
        if (profile == null) {
            return getIsland(player.getUniqueId());
        }

        UUID owner = player.getUniqueId();
        Island island = findIslandForOwnerProfile(owner, profile.getProfileId(), null);
        if (island == null) {
            // Legacy islands keyed by profile name: migrate on access.
            island = findIslandForOwnerProfile(owner, null, profile.getProfileName());
            if (island != null && island.getProfileId() == null) {
                island.setProfileId(profile.getProfileId());
                saveIsland(island);
            }
        }
        if (island != null) {
            playerToIsland.put(owner, island.getId());
            return island;
        }

        // Fallback: member islands
        for (Island candidate : islandsById.values()) {
            if (candidate != null && candidate.isMember(owner)) {
                return candidate;
            }
        }
        return null;
    }

    public Island getIsland(UUID playerUuid) {
        UUID islandId = playerToIsland.get(playerUuid);
        if (islandId != null) {
            Island active = islandsById.get(islandId);
            if (active != null) {
                return active;
            }
        }
        List<UUID> profiles = ownerProfiles.get(playerUuid);
        if (profiles != null && !profiles.isEmpty()) {
            UUID first = profiles.getFirst();
            Island island = islandsById.get(first);
            if (island != null) {
                playerToIsland.put(playerUuid, first);
                return island;
            }
        }
        // fallback: member islands
        for (Island candidate : islandsById.values()) {
            if (candidate != null && candidate.isMember(playerUuid)) {
                return candidate;
            }
        }
        return null;
    }

    public Location getSafeSpawnLocation(Island island) {
        if (island == null || island.getCenter() == null) return null;

        Location spawnPoint = island.getSpawnPoint();
        if (spawnPoint != null) {
            Location safe = WorldHelper.findSafeLocation(spawnPoint);
            if (safe != null) {
                return safe;
            }
        }

        // Spawn not set or unsafe: choose a safe center spot and persist it.
        Location safeCenter = findCenterSafeSpawn(island);
        island.setSpawnPoint(safeCenter);
        saveIsland(island);
        return safeCenter;
    }

    private Location findCenterSafeSpawn(Island island) {
        Location center = island.getCenter();
        if (center == null) return null;
        
        // Start slightly above center to find topmost block
        Location base = center.clone().add(0.5, 1, 0.5);
        
        // First try WorldHelper's safe location finder
        Location safe = WorldHelper.findSafeLocation(base);
        if (safe != null) {
            return safe;
        }
        
        // Fallback: scan small radius around center to find highest block
        Location bestSpawn = null;
        double highestY = -1;
        
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                Location candidate = center.clone().add(dx + 0.5, 256, dz + 0.5);
                // Find highest solid block
                while (candidate.getY() > center.getY() - 5) {
                    Block block = candidate.getBlock();
                    if (block.getType().isSolid() && block.getType() != Material.BEDROCK) {
                        // Found solid block, spawn above it
                        Location spawnLoc = candidate.clone().add(0, 1, 0);
                        if (bestSpawn == null || spawnLoc.getY() > highestY) {
                            bestSpawn = spawnLoc;
                            highestY = spawnLoc.getY();
                        }
                        break;
                    }
                    candidate.add(0, -1, 0);
                }
            }
        }
        
        if (bestSpawn != null) {
            return bestSpawn;
        }
        
        // Last resort: return center + 1 block
        return center.clone().add(0.5, 2, 0.5);
    }

    public boolean expandIsland(Player player, int newLevel) {
        Island island = getIsland(player.getUniqueId());
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You don't have an island.");
            return false;
        }

        if (newLevel <= island.getUpgradeLevel()) {
            player.sendMessage(ChatColor.RED + "Your island is already at this level or higher.");
            return false;
        }

        if (newLevel > upgradeSizes.size()) {
            player.sendMessage(ChatColor.RED + "Maximum island level reached.");
            return false;
        }

        double cost = upgradeCosts.getOrDefault(newLevel, Double.MAX_VALUE);
        if (cost == Double.MAX_VALUE) {
            player.sendMessage(ChatColor.RED + "Invalid upgrade level.");
            return false;
        }

        if (!hasEnoughMoney(player, cost)) {
            player.sendMessage(ChatColor.RED + "Insufficient funds. Need: $" + String.format("%.2f", cost));
            return false;
        }

        int newSize = upgradeSizes.get(newLevel - 1);
        island.setSize(newSize);
        island.setLevel(newLevel);

        withdrawMoney(player, cost);

        saveIsland(island);

        player.sendMessage(ChatColor.GREEN + "Island expanded to level " + newLevel + "!");
        player.sendMessage(ChatColor.GRAY + "New size: " + newSize + "x" + newSize);
        player.sendMessage(ChatColor.GRAY + "Cost: $" + String.format("%.2f", cost));
        if (plugin.getSkyblockLevelManager() != null) {
            plugin.getSkyblockLevelManager().recordIslandUpgrade(player, newLevel);
        }

        return true;
    }

    public double getUpgradeCost(int level) {
        return upgradeCosts.getOrDefault(level, 0.0);
    }

    public int getNextUpgradeLevel(Island island) {
        return island.getUpgradeLevel() + 1;
    }

    public int getNextUpgradeSize(Island island) {
        int nextLevel = getNextUpgradeLevel(island);
        if (nextLevel > upgradeSizes.size()) {
            return island.getSize();
        }
        return upgradeSizes.get(nextLevel - 1);
    }

    private boolean hasEnoughMoney(Player player, double amount) {
        if (profileEconomy.requireSelectedProfile(player) == null) {
            return false;
        }
        return profileEconomy.has(player, amount);
    }

    private void withdrawMoney(Player player, double amount) {
        if (profileEconomy.requireSelectedProfile(player) == null) {
            return;
        }
        profileEconomy.withdraw(player, amount);
    }

    public void deleteIsland(UUID playerUuid) {
        List<UUID> profiles = ownerProfiles.get(playerUuid);
        if (profiles == null || profiles.isEmpty()) return;

        UUID activeId = playerToIsland.get(playerUuid);
        UUID toDelete = activeId != null ? activeId : profiles.getFirst();

        Island island = islandsById.remove(toDelete);
        profiles.remove(toDelete);
        if (profiles.isEmpty()) {
            ownerProfiles.remove(playerUuid);
            playerToIsland.remove(playerUuid);
        } else {
            playerToIsland.put(playerUuid, profiles.getFirst());
        }

        if (island != null) {
            deleteIslandData(island);
            deleteProfileInventory(playerUuid, island.getProfileId(), island.getProfileName());
        }
    }

    public void deleteIslandForProfile(UUID owner, UUID profileId, String legacyProfileName) {
        if (owner == null) {
            return;
        }

        Island island = findIslandForOwnerProfile(owner, profileId, legacyProfileName);
        if (island == null) {
            deleteProfileInventory(owner, profileId, legacyProfileName);
            return;
        }

        UUID islandId = island.getId();
        islandsById.remove(islandId);

        List<UUID> ids = ownerProfiles.get(owner);
        if (ids != null) {
            ids.remove(islandId);
            if (ids.isEmpty()) {
                ownerProfiles.remove(owner);
            }
        }

        UUID activeId = playerToIsland.get(owner);
        if (activeId != null && activeId.equals(islandId)) {
            if (ids != null && !ids.isEmpty()) {
                playerToIsland.put(owner, ids.getFirst());
            } else {
                playerToIsland.remove(owner);
            }
        }

        deleteIslandData(island);
        deleteProfileInventory(owner, profileId, legacyProfileName);
    }

    private void deleteIslandData(Island island) {
        File islandsFolder = new File(plugin.getDataFolder(), "skyblock/islands");
        File islandFile = new File(islandsFolder, island.getId().toString() + ".yml");
        if (islandFile.exists()) {
            islandFile.delete();
        }
    }

    private void loadIslands() {
        File islandsFolder = new File(plugin.getDataFolder(), "skyblock/islands");
        if (!islandsFolder.exists()) {
            return;
        }

        File[] islandFiles = islandsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (islandFiles == null) return;

        for (File file : islandFiles) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                ConfigurationSection islandSection = config.getConfigurationSection("island");
                if (islandSection != null) {
                    Island island = new Island(islandSection.getValues(false));
                    islandsById.put(island.getId(), island);
                    ownerProfiles.computeIfAbsent(island.getOwner(), k -> new ArrayList<>()).add(island.getId());
                    playerToIsland.putIfAbsent(island.getOwner(), island.getId());
                    indexIslandForCoop(island);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load island from: " + file.getName());
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("Loaded " + islandsById.size() + " islands.");
    }

    private void indexIslandForCoop(Island island) {
        if (island == null) {
            return;
        }

        UUID profileId = island.getProfileId();
        if (profileId != null) {
            islandByProfileId.putIfAbsent(profileId, island.getId());

            for (UUID memberId : island.getMembers()) {
                if (memberId == null) {
                    continue;
                }
                // Owner is not considered a "member" for this index.
                if (island.getOwner() != null && island.getOwner().equals(memberId)) {
                    continue;
                }
                coopProfileByMember.putIfAbsent(memberId, profileId);
            }
        }
    }

    public void setIslandBiome(Island island, org.bukkit.block.Biome biome) {
        WorldHelper.setAreaBiome(island.getMinCorner(), island.getMaxCorner(), biome);
    }

    public void saveIsland(Island island) {
        // Keep co-op lookup indices in sync when islands are migrated/updated at runtime.
        indexIslandForCoop(island);

        File islandsFolder = new File(plugin.getDataFolder(), "skyblock/islands");
        if (!islandsFolder.exists()) {
            islandsFolder.mkdirs();
        }

        File islandFile = new File(islandsFolder, island.getId().toString() + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        config.set("island", island.serialize());

        try {
            config.save(islandFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save island: " + island.getId());
            e.printStackTrace();
        }
    }

    /**
     * @return The co-op profile id (island profile id) this player belongs to as a member, or null if none.
     */
    public UUID getCoopProfileId(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        return coopProfileByMember.get(playerId);
    }

    /**
     * @return The island associated with a profile id, if any.
     */
    public Island getIslandByProfileId(UUID profileId) {
        if (profileId == null) {
            return null;
        }
        UUID islandId = islandByProfileId.get(profileId);
        if (islandId != null) {
            return islandsById.get(islandId);
        }
        // Fallback: slow scan (should rarely happen, but keeps behavior correct even if index is missing).
        for (Island island : islandsById.values()) {
            if (island != null && profileId.equals(island.getProfileId())) {
                islandByProfileId.putIfAbsent(profileId, island.getId());
                return island;
            }
        }
        return null;
    }

    /**
     * Returns owner + members for the island tied to this profile id.
     */
    public Set<UUID> getCoopMemberIdsForProfileId(UUID profileId) {
        Island island = getIslandByProfileId(profileId);
        if (island == null) {
            return Set.of();
        }
        Set<UUID> ids = new HashSet<>();
        if (island.getOwner() != null) {
            ids.add(island.getOwner());
        }
        ids.addAll(island.getMembers());
        return ids;
    }

    /**
     * Adds a co-op member to an island and updates fast lookup indices.
     */
    public boolean addCoopMember(Island island, UUID memberId) {
        if (island == null || memberId == null) {
            return false;
        }
        if (island.getOwner() != null && island.getOwner().equals(memberId)) {
            return false;
        }

        UUID profileId = island.getProfileId();
        if (profileId == null) {
            plugin.getLogger().warning("Attempted to add co-op member to island without profileId: " + island.getId());
            return false;
        }

        // Only support one co-op membership per player for now to avoid ambiguous "active profile" selection.
        UUID existing = coopProfileByMember.get(memberId);
        if (existing != null && !existing.equals(profileId)) {
            return false;
        }

        island.addMember(memberId);
        coopProfileByMember.put(memberId, profileId);
        islandByProfileId.putIfAbsent(profileId, island.getId());
        saveIsland(island);
        return true;
    }

    /**
     * Removes a co-op member from an island and updates fast lookup indices.
     */
    public boolean removeCoopMember(Island island, UUID memberId) {
        if (island == null || memberId == null) {
            return false;
        }
        island.removeMember(memberId);

        UUID profileId = island.getProfileId();
        if (profileId != null) {
            coopProfileByMember.remove(memberId, profileId);
        } else {
            coopProfileByMember.remove(memberId);
        }

        saveIsland(island);
        return true;
    }
    
    /**
     * Save all islands to disk.
     * Called during plugin shutdown to ensure no data is lost.
     */
    public void saveAllIslands() {
        if (islandsById.isEmpty()) {
            return;
        }
        
        plugin.getLogger().info("Saving " + islandsById.size() + " islands...");
        int saved = 0;
        for (Island island : islandsById.values()) {
            if (island != null) {
                saveIsland(island);
                saved++;
            }
        }
        plugin.getLogger().info("Saved " + saved + " islands successfully.");
    }
    
    /**
     * Shutdown method to cleanup and save all data.
     * Called during plugin disable.
     */
    public void shutdown() {
        saveAllIslands();
        saveAllOnlineInventories();
        plugin.getLogger().info("IslandManager shutdown complete.");
    }

    /**
     * Save inventories for all currently online players.
     * Crucial for ensuring no data loss during server shutdown or plugin reloads.
     */
    public void saveAllOnlineInventories() {
        Collection<? extends Player> online = Bukkit.getOnlinePlayers();
        if (online.isEmpty()) {
            return;
        }

        plugin.getLogger().info("Saving inventories for " + online.size() + " online players...");
        int saved = 0;
        for (Player player : online) {
            try {
                saveProfileInventory(player);
                saved++;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save inventory for player " + player.getName() + ": " + e.getMessage());
            }
        }
        plugin.getLogger().info("Successfully saved " + saved + " player inventories.");
    }

    public Collection<Island> getAllIslands() {
        return islandsById.values();
    }

    public Island getIslandById(UUID islandId) {
        if (islandId == null) {
            return null;
        }
        return islandsById.get(islandId);
    }

    public List<Island> getIslandsForOwner(UUID owner) {
        List<UUID> ids = ownerProfiles.getOrDefault(owner, List.of());
        List<Island> list = new ArrayList<>();
        for (UUID id : ids) {
            Island island = islandsById.get(id);
            if (island != null) {
                list.add(island);
            }
        }
        return list;
    }

    private Island findIslandForOwnerProfile(UUID owner, UUID profileId, String profileName) {
        if (owner == null) {
            return null;
        }

        List<Island> list = getIslandsForOwner(owner);
        if (profileId != null) {
            for (Island island : list) {
                if (island != null && profileId.equals(island.getProfileId())) {
                    return island;
                }
            }
        }

        if (profileName != null && !profileName.isBlank()) {
            for (Island island : list) {
                if (island != null && island.getProfileName() != null && island.getProfileName().equalsIgnoreCase(profileName)) {
                    return island;
                }
            }
        }

        return null;
    }

    public Island switchActiveIsland(UUID owner, String profileName) {
        List<Island> list = getIslandsForOwner(owner);
        for (Island island : list) {
            if (island.getProfileName().equalsIgnoreCase(profileName)) {
                playerToIsland.put(owner, island.getId());
                return island;
            }
        }
        return null;
    }

    public Island createProfile(UUID owner, String ownerName, String profileName, Player creator) {
        Player player = creator;
        if (player == null || !player.getUniqueId().equals(owner)) {
            player = Bukkit.getPlayer(owner);
        }
        if (player == null) return null;
        return createIsland(player, profileName);
    }

    public void saveProfileInventory(Player player) {
        if (player == null) {
            return;
        }
        SkyBlockProfile profile = profileEconomy.getSelectedProfile(player);
        if (profile == null) {
            return;
        }
        saveProfileInventory(player, profile.getProfileId());
    }

    public void saveProfileInventory(Player player, UUID profileId) {
        if (player == null || profileId == null) {
            return;
        }
        File file = inventoryFile(player.getUniqueId(), profileId);
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("inventory", player.getInventory().getContents());
        cfg.set("armor", player.getInventory().getArmorContents());
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save profile inventory: " + e.getMessage());
        }
    }

    public void loadProfileInventory(Player player, UUID profileId, String legacyProfileName) {
        if (player == null || profileId == null) {
            return;
        }
        UUID owner = player.getUniqueId();
        File file = inventoryFile(owner, profileId);
        if (!file.exists() && legacyProfileName != null && !legacyProfileName.isBlank()) {
            File legacy = legacyInventoryFile(owner, legacyProfileName);
            if (legacy.exists()) {
                loadInventoryFromFile(player, legacy);
                saveProfileInventory(player, profileId); // migrate into profileId storage
                return;
            }
        }
        if (!file.exists()) {
            player.getInventory().clear();
            player.getInventory().setArmorContents(new ItemStack[4]);
            return;
        }
        loadInventoryFromFile(player, file);
    }

    public void deleteProfileInventory(UUID owner, UUID profileId, String legacyProfileName) {
        if (owner == null) {
            return;
        }
        if (profileId != null) {
            File file = inventoryFile(owner, profileId);
            if (file.exists()) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }
        if (legacyProfileName != null && !legacyProfileName.isBlank()) {
            File legacy = legacyInventoryFile(owner, legacyProfileName);
            if (legacy.exists()) {
                //noinspection ResultOfMethodCallIgnored
                legacy.delete();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadInventoryFromFile(Player player, File file) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        List<ItemStack> inv = (List<ItemStack>) cfg.getList("inventory", new ArrayList<ItemStack>());
        List<ItemStack> armor = (List<ItemStack>) cfg.getList("armor", new ArrayList<ItemStack>());
        player.getInventory().setContents(inv.toArray(new ItemStack[0]));
        player.getInventory().setArmorContents(armor.toArray(new ItemStack[0]));
    }

    private File inventoryFile(UUID owner, UUID profileId) {
        File dir = new File(plugin.getDataFolder(), "skyblock/profile-inventories/" + owner);
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return new File(dir, profileId.toString() + ".yml");
    }

    private File legacyInventoryFile(UUID owner, String profileName) {
        return new File(plugin.getDataFolder(), "skyblock/profiles/" + owner + "/" + profileName + ".yml");
    }

    public Island getIslandAt(Location location) {
        if (location == null || islandWorld == null || !location.getWorld().equals(islandWorld)) {
            return null;
        }
        for (Island island : islandsById.values()) {
            if (island != null && island.isWithinIsland(location)) {
                return island;
            }
        }
        return null;
    }

    public int getTotalIslands() {
        return islandsById.size();
    }

    public int getIslandSpacing() {
        return islandSpacing;
    }

    public int getStartingSize() {
        return startingSize;
    }

    public List<Integer> getUpgradeSizes() {
        return upgradeSizes;
    }
}

