package io.papermc.Grivience.skyblock.island;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class IslandManager {
    private final GriviencePlugin plugin;
    private final Map<UUID, Island> islandsByOwner = new ConcurrentHashMap<>();
    private final Map<UUID, Island> islandsById = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerToIsland = new ConcurrentHashMap<>();
    private World islandWorld;
    private int islandSpacing;
    private int startingSize;
    private List<Integer> upgradeSizes;
    private Map<Integer, Double> upgradeCosts;
    private int islandSpawnY;

    public IslandManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        loadIslands();
    }

    public void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection skyblockSection = config.getConfigurationSection("skyblock");

        if (skyblockSection == null) {
            plugin.getLogger().warning("SkyBlock config section not found. Using defaults.");
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
    }

    public void initializeWorld() {
        String worldName = plugin.getConfig().getString("skyblock.world-name", "skyblock_world");
        islandWorld = Bukkit.getWorld(worldName);

        if (islandWorld == null) {
            plugin.getLogger().info("Creating SkyBlock world: " + worldName);
            WorldCreator creator = new WorldCreator(worldName);
            creator.environment(World.Environment.NORMAL);
            creator.generateStructures(false);
            creator.generator(new SkyblockWorldGenerator());
            islandWorld = Bukkit.createWorld(creator);

            if (islandWorld != null) {
                islandWorld.setTime(6000);
                islandWorld.setWeatherDuration(0);
                islandWorld.setStorm(false);
                islandWorld.setThundering(false);
                plugin.getLogger().info("SkyBlock world created successfully.");
            }
        }
    }

    public World getIslandWorld() {
        return islandWorld;
    }

    public Island createIsland(Player player) {
        if (hasIsland(player.getUniqueId())) {
            return getIsland(player.getUniqueId());
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

        Island island = new Island(player.getUniqueId(), player.getName(), spawnLocation);
        island.setSize(startingSize);

        islandsByOwner.put(player.getUniqueId(), island);
        islandsById.put(island.getId(), island);
        playerToIsland.put(player.getUniqueId(), island.getId());

        IslandGenerator.generateIsland(island);

        // Set default island spawn to the platform center and persist.
        Location defaultSpawn = spawnLocation.clone().add(0.5, 1, 0.5);
        island.setSpawnPoint(defaultSpawn);
        saveIsland(island);

        Location safeSpawn = getSafeSpawnLocation(island);
        if (safeSpawn != null) {
            player.teleport(safeSpawn);
            player.setBedSpawnLocation(safeSpawn, true);
        } else {
            Location fallback = defaultSpawn;
            player.teleport(fallback);
            player.setBedSpawnLocation(fallback, true);
        }

        player.sendMessage(ChatColor.GREEN + "Your island has been created!");
        player.sendMessage(ChatColor.GRAY + "Size: " + startingSize + "x" + startingSize);
        player.sendMessage(ChatColor.GRAY + "Use /island go to return anytime.");

        return island;
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
        return islandsByOwner.containsKey(playerUuid) || playerToIsland.containsKey(playerUuid);
    }

    public Island getIsland(UUID playerUuid) {
        Island island = islandsByOwner.get(playerUuid);
        if (island != null) return island;

        UUID islandId = playerToIsland.get(playerUuid);
        if (islandId != null) {
            return islandsById.get(islandId);
        }

        for (Island candidate : islandsById.values()) {
            if (candidate != null && candidate.isMember(playerUuid)) {
                return candidate;
            }
        }

        return null;
    }

    public Island getIslandById(UUID islandId) {
        return islandsById.get(islandId);
    }

    public Location getSafeSpawnLocation(Island island) {
        if (island == null || island.getCenter() == null) return null;

        Location spawnPoint = island.getSpawnPoint();
        if (spawnPoint != null) {
            Location safe = WorldHelper.findSafeLocation(spawnPoint);
            return safe != null ? safe : spawnPoint;
        }

        Location center = island.getCenter();
        if (center == null) {
            return null;
        }
        Location centerSpot = center.clone().add(0.5, 1, 0.5);
        Location safeCenter = WorldHelper.findSafeLocation(centerSpot);
        return safeCenter != null ? safeCenter : centerSpot;
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
        if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            net.milkbowl.vault.economy.Economy economy = getEconomy();
            return economy != null && economy.has(player, amount);
        }
        return true;
    }

    private void withdrawMoney(Player player, double amount) {
        if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            net.milkbowl.vault.economy.Economy economy = getEconomy();
            if (economy != null) {
                economy.withdrawPlayer(player, amount);
            }
        }
    }

    private net.milkbowl.vault.economy.Economy getEconomy() {
        if (Bukkit.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class) != null) {
            return Bukkit.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class).getProvider();
        }
        return null;
    }

    public void deleteIsland(UUID playerUuid) {
        Island island = getIsland(playerUuid);
        if (island == null) return;

        islandsByOwner.remove(playerUuid);
        islandsById.remove(island.getId());
        playerToIsland.remove(playerUuid);

        deleteIslandData(island);
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
                    islandsByOwner.put(island.getOwner(), island);
                    playerToIsland.put(island.getOwner(), island.getId());
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load island from: " + file.getName());
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("Loaded " + islandsById.size() + " islands.");
    }

    public void saveIsland(Island island) {
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

    public Collection<Island> getAllIslands() {
        return islandsById.values();
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
