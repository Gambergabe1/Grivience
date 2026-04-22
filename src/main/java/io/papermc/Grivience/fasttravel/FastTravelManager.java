package io.papermc.Grivience.fasttravel;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages fast travel destinations and player unlocks.
 * Handles loading/saving travel points and tracking which destinations each player has unlocked.
 * Includes built-in integration with /hub, /dungeonhub, /minehub, and /farmhub warps.
 */
public final class FastTravelManager {
    private static final String CHECK = "\u2713";
    private static final Set<String> HIDDEN_DESTINATION_KEYS = Set.of(
            "shopping",
            "shopping_district",
            "forest",
            "forest_hub"
    );
    private static final Set<String> HIDDEN_DESTINATION_NAMES = Set.of(
            "shopping district",
            "forest hub"
    );

    private final GriviencePlugin plugin;
    private final File configFile;
    private FileConfiguration config;
    private final Map<String, FastTravelPoint> pointsByName = new LinkedHashMap<>();
    private final Map<UUID, Set<String>> unlockedPoints = new HashMap<>();
    private final Map<String, String> playerHeadOwners = new HashMap<>();

    public FastTravelManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "fasttravel.yml");
    }

    public void load() {
        if (!configFile.exists()) {
            plugin.saveResource("fasttravel.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        loadPoints();
        loadUnlocks();
        syncHubWarpsFromConfig();
    }

    private void loadPoints() {
        pointsByName.clear();
        playerHeadOwners.clear();

        for (String key : config.getKeys(false)) {
            if (key.equals("unlocks")) continue;
            if (isHiddenDestinationKey(key)) {
                continue;
            }

            String name = config.getString(key + ".name", key);
            if (isHiddenDestinationName(name)) {
                continue;
            }
            String world = config.getString(key + ".world", "world");
            double x = config.getDouble(key + ".x", 0);
            double y = config.getDouble(key + ".y", 64);
            double z = config.getDouble(key + ".z", 0);
            float yaw = (float) config.getDouble(key + ".yaw", 0);
            float pitch = (float) config.getDouble(key + ".pitch", 0);
            String description = config.getString(key + ".description", "");
            String headOwner = config.getString(key + ".head-owner", "");
            int requiredLevel = config.getInt(key + ".required-level", 0);
            String permission = config.getString(key + ".permission", "");

            World bukkitWorld = Bukkit.getWorld(world);
            if (bukkitWorld == null) {
                plugin.getLogger().warning("Fast travel point '" + key + "' world '" + world + "' not found. Skipping.");
                continue;
            }

            Location location = new Location(bukkitWorld, x, y, z, yaw, pitch);
            FastTravelPoint point = new FastTravelPoint(key, name, location, description, headOwner, requiredLevel, permission);
            pointsByName.put(key, point);

            if (!headOwner.isEmpty()) {
                playerHeadOwners.put(key, headOwner);
            }
        }

        plugin.getLogger().info("Loaded " + pointsByName.size() + " fast travel points from config.");
    }

    private boolean isHiddenDestinationKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        return HIDDEN_DESTINATION_KEYS.contains(key.trim().toLowerCase(Locale.ROOT));
    }

    private boolean isHiddenDestinationName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        return HIDDEN_DESTINATION_NAMES.contains(name.trim().toLowerCase(Locale.ROOT));
    }

    public void syncHubWarpsFromConfig() {
        // Load built-in hub warps from main config
        // These are automatically added as fast travel destinations
        
        // Load /hub location
        loadHubWarp("hub", "Hub", "skyblock.hub-spawn", "skyblock.hub-world", 
            "Main hub and spawn area", "MHF_Arrow");
        
        // Load /dungeonhub location
        loadHubWarp("dungeonhub", "Dungeon Hub", "dungeons.hub", "dungeons.hub.world",
            "Gateway to dungeon instances", "MHF_Steve");
        
        // Load /minehub location
        loadHubWarp("minehub", "Mining Hub", "skyblock.minehub-spawn", "skyblock.minehub-world",
            "Access to mining zones and caves", "MHF_Miner");
        
        // Load /farmhub location only when it is distinct from the main hub.
        if (usesSeparateFarmHubWarp()) {
            loadHubWarp("farmhub", "Farm Hub", "skyblock.farmhub-spawn", "skyblock.farmhub-world",
                "Farming areas and crop contests", "MHF_Farmer");
        } else {
            pointsByName.remove("farmhub");
            playerHeadOwners.remove("farmhub");
        }

        // Load /endmine location (End Mines v0.2 expansion)
        int endMinesRequiredLevel = plugin.getConfig().getInt("end-mines.required-level", 25);
        loadHubWarp("endmine", "End Mines", "end-mines.spawn", "end-mines.world-name",
                "Late-game End mining and combat challenge", "MHF_Enderman", endMinesRequiredLevel, "");

        // Auto-discover additional hub warps from config paths like:
        // - skyblock.<id>-world
        // - skyblock.<id>-spawn.{x,y,z,yaw,pitch}
        discoverSkyblockHubWarps();
        
        plugin.getLogger().info("Loaded hub warp fast travel points.");
    }

    private void discoverSkyblockHubWarps() {
        ConfigurationSection skyblockSection = plugin.getConfig().getConfigurationSection("skyblock");
        if (skyblockSection == null) {
            return;
        }

        Set<String> reserved = Set.of("hub", "dungeonhub", "minehub", "farmhub");
        for (String rawKey : skyblockSection.getKeys(false)) {
            if (rawKey == null || !rawKey.endsWith("-world")) {
                continue;
            }

            String base = rawKey.substring(0, rawKey.length() - "-world".length());
            if (base.isBlank()) {
                continue;
            }

            String normalized = base.toLowerCase(Locale.ROOT);
            if (!normalized.equals("hub") && !normalized.endsWith("hub")) {
                continue;
            }
            if (reserved.contains(normalized)) {
                continue;
            }

            String displayName = autoHubDisplayName(normalized);
            loadHubWarp(
                    normalized,
                    displayName,
                    "skyblock." + normalized + "-spawn",
                    "skyblock." + normalized + "-world",
                    "Travel to the " + displayName,
                    "MHF_Arrow"
            );
        }
    }

    private String autoHubDisplayName(String key) {
        if (key == null || key.isBlank()) {
            return "Hub";
        }

        String normalized = key.toLowerCase(Locale.ROOT);
        if (normalized.equals("hub")) {
            return "Hub";
        }

        if (normalized.endsWith("hub") && normalized.length() > 3) {
            String prefix = normalized.substring(0, normalized.length() - 3);
            if (prefix.isBlank()) {
                return "Hub";
            }
            String prettyPrefix = switch (prefix) {
                case "mine" -> "Mining";
                default -> capitalize(prefix);
            };
            return prettyPrefix + " Hub";
        }

        return capitalize(normalized);
    }

    private String capitalize(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String trimmed = input.trim();
        if (trimmed.length() == 1) {
            return trimmed.toUpperCase(Locale.ROOT);
        }
        return trimmed.substring(0, 1).toUpperCase(Locale.ROOT) + trimmed.substring(1);
    }

    private void loadHubWarp(String key, String name, String spawnPath, String worldPath, 
                             String description, String headOwner) {
        loadHubWarp(key, name, spawnPath, worldPath, description, headOwner, 0, "");
    }

    private boolean usesSeparateFarmHubWarp() {
        String hubWorld = plugin.getConfig().getString("skyblock.hub-world", "world");
        String farmhubWorld = plugin.getConfig().getString("skyblock.farmhub-world", "world");
        if (hubWorld == null || farmhubWorld == null) {
            return false;
        }
        return !hubWorld.equalsIgnoreCase(farmhubWorld);
    }

    private void loadHubWarp(String key, String name, String spawnPath, String worldPath,
                             String description, String headOwner, int requiredLevel, String permission) {
        // If the server owner explicitly defined this destination in fasttravel.yml,
        // treat that as the source of truth and do not overwrite it.
        if (config != null && config.isConfigurationSection(key)) {
            return;
        }

        String worldName = plugin.getConfig().getString(worldPath, "world");
        World world = Bukkit.getWorld(worldName);
        
        if (world == null) {
            plugin.getLogger().warning("Hub warp '" + key + "' world '" + worldName + "' not found. Skipping.");
            return;
        }
        
        Location location;
        if (plugin.getConfig().contains(spawnPath)) {
            double x = plugin.getConfig().getDouble(spawnPath + ".x", 0);
            double y = plugin.getConfig().getDouble(spawnPath + ".y", 64);
            double z = plugin.getConfig().getDouble(spawnPath + ".z", 0);
            float yaw = (float) plugin.getConfig().getDouble(spawnPath + ".yaw", 0);
            float pitch = (float) plugin.getConfig().getDouble(spawnPath + ".pitch", 0);
            location = new Location(world, x, y, z, yaw, pitch);
        } else {
            // Fallback to world spawn
            location = world.getSpawnLocation().add(0.5, 0, 0.5);
        }
        
        FastTravelPoint point = new FastTravelPoint(key, name, location, description, headOwner, requiredLevel, permission == null ? "" : permission);
        pointsByName.put(key, point);
        if (headOwner != null && !headOwner.isBlank()) {
            playerHeadOwners.put(key, headOwner);
        }
    }

    private void loadUnlocks() {
        unlockedPoints.clear();
        if (config == null) {
            return;
        }

        ConfigurationSection unlockSection = config.getConfigurationSection("unlocks");
        if (unlockSection == null) {
            config.createSection("unlocks");
            return;
        }

        for (String uuidStr : unlockSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                Set<String> points = new HashSet<>(config.getStringList("unlocks." + uuidStr));
                unlockedPoints.put(uuid, points);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in fast travel unlocks: " + uuidStr);
            }
        }
    }

    public void save() {
        for (Map.Entry<UUID, Set<String>> entry : unlockedPoints.entrySet()) {
            config.set("unlocks." + entry.getKey().toString(), new ArrayList<>(entry.getValue()));
        }
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save fast travel data: " + e.getMessage());
        }
    }

    public Collection<FastTravelPoint> getAllPoints() {
        return Collections.unmodifiableCollection(pointsByName.values());
    }

    public FastTravelPoint getPointByName(String name) {
        return pointsByName.get(name);
    }

    public boolean isUnlocked(Player player, String pointKey) {
        Set<String> playerUnlocks = unlockedPoints.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        return playerUnlocks.contains(pointKey);
    }

    public void unlock(Player player, String pointKey) {
        Set<String> playerUnlocks = unlockedPoints.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        if (playerUnlocks.add(pointKey)) {
            FastTravelPoint point = pointsByName.get(pointKey);
            String display = point == null ? pointKey : point.name();
            player.sendMessage(ChatColor.GREEN + CHECK + " Unlocked fast travel: " + ChatColor.AQUA + display);
            save();
        }
    }

    public void unlockAll(Player player) {
        Set<String> playerUnlocks = unlockedPoints.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        playerUnlocks.addAll(pointsByName.keySet());
        player.sendMessage(ChatColor.GREEN + CHECK + " Unlocked all fast travel points!");
        save();
    }

    public Set<String> getUnlockedPoints(Player player) {
        return Collections.unmodifiableSet(unlockedPoints.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()));
    }

    public boolean lock(Player player, String pointKey) {
        if (player == null || pointKey == null || pointKey.isBlank()) {
            return false;
        }
        Set<String> playerUnlocks = unlockedPoints.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        boolean removed = playerUnlocks.remove(pointKey);
        if (removed) {
            save();
        }
        return removed;
    }

    public String getHeadOwner(String pointKey) {
        return playerHeadOwners.getOrDefault(pointKey, "");
    }

    public boolean teleport(Player player, String pointKey) {
        if (player == null) {
            return false;
        }

        FastTravelPoint point = pointsByName.get(pointKey);
        if (point == null) {
            player.sendMessage(ChatColor.RED + "Unknown destination: " + pointKey);
            return false;
        }

        if (!isUnlocked(player, pointKey)) {
            player.sendMessage(ChatColor.RED + "Locked! You haven't unlocked this destination yet.");
            player.sendMessage(ChatColor.GRAY + "Unlock destinations by visiting them first.");
            return false;
        }

        if (point.permission() != null && !point.permission().isBlank() && !player.hasPermission(point.permission())) {
            player.sendMessage(ChatColor.RED + "You don't have permission to travel there.");
            return false;
        }

        if ("endmine".equalsIgnoreCase(pointKey) && plugin.getConfig().getBoolean("end-mines.access.operator-only", false) && !player.isOp()) {
            player.sendMessage(ChatColor.RED + "End Mines access is currently operator-only.");
            return false;
        }

        if (point.requiredLevel() > 0) {
            int level = plugin.getSkyblockStatsManager() == null ? 0 : plugin.getSkyblockStatsManager().getLevel(player);
            if (level < point.requiredLevel()) {
                player.sendMessage(ChatColor.RED + "You need Skyblock Level " + point.requiredLevel() + " to travel there.");
                return false;
            }
        }

        player.teleport(point.location());
        player.sendMessage(ChatColor.GREEN + CHECK + " Teleported to: " + ChatColor.AQUA + point.name());
        
        // Auto-start quest if entering minehub
        if (pointKey.equalsIgnoreCase("minehub") && plugin.getQuestManager() != null) {
            plugin.getQuestManager().startQuest(player, "ironcrest_part1_arrival", 
                io.papermc.Grivience.quest.QuestTriggerSource.COMMAND, true);
        }
        
        return true;
    }

    public void addPoint(FastTravelPoint point) {
        pointsByName.put(point.key(), point);
        if (!point.headOwner().isEmpty()) {
            playerHeadOwners.put(point.key(), point.headOwner());
        }
        config.set(point.key() + ".name", point.name());
        config.set(point.key() + ".world", point.location().getWorld().getName());
        config.set(point.key() + ".x", point.location().getX());
        config.set(point.key() + ".y", point.location().getY());
        config.set(point.key() + ".z", point.location().getZ());
        config.set(point.key() + ".yaw", point.location().getYaw());
        config.set(point.key() + ".pitch", point.location().getPitch());
        config.set(point.key() + ".description", point.description());
        config.set(point.key() + ".head-owner", point.headOwner());
        config.set(point.key() + ".required-level", point.requiredLevel());
        config.set(point.key() + ".permission", point.permission());
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save fast travel point: " + e.getMessage());
        }
    }

    public void removePoint(String pointKey) {
        pointsByName.remove(pointKey);
        playerHeadOwners.remove(pointKey);
        config.set(pointKey, null);
        for (Set<String> unlocks : unlockedPoints.values()) {
            unlocks.remove(pointKey);
        }
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to remove fast travel point: " + e.getMessage());
        }
    }

    public FastTravelPoint createPointFromLocation(String key, String name, Location location, String description, String headOwner, int requiredLevel) {
        return new FastTravelPoint(key, name, location, description, headOwner, requiredLevel, "");
    }

    public record FastTravelPoint(
            String key,
            String name,
            Location location,
            String description,
            String headOwner,
            int requiredLevel,
            String permission
    ) {}
}

