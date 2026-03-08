package io.papermc.Grivience.skyblock.hub;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skyblock.island.SkyblockWorldGenerator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;

/**
 * Creates and configures the Minehub as a dedicated void world.
 * Teleport locations are controlled by {@code skyblock.minehub-spawn} in config.
 */
public final class MinehubWorldManager {
    private final GriviencePlugin plugin;
    private World minehubWorld;

    public MinehubWorldManager(GriviencePlugin plugin) {
        this.plugin = plugin;
    }

    public String getWorldName() {
        return plugin.getConfig().getString("skyblock.minehub-world", "minehub_world");
    }

    public World getWorld() {
        if (minehubWorld == null) {
            initializeWorld();
        }
        return minehubWorld;
    }

    public void initializeWorld() {
        String worldName = getWorldName();
        minehubWorld = Bukkit.getWorld(worldName);

        if (minehubWorld == null) {
            plugin.getLogger().info("Creating Minehub void world: " + worldName);
            WorldCreator creator = WorldCreator.name(worldName)
                    .environment(World.Environment.NORMAL)
                    .generator(new SkyblockWorldGenerator())
                    .generateStructures(false);
            try {
                creator.type(org.bukkit.WorldType.FLAT);
            } catch (Exception ignored) {
            }
            minehubWorld = creator.createWorld();
        }

        if (minehubWorld == null) {
            plugin.getLogger().severe("Failed to create/load Minehub world: " + worldName);
            return;
        }

        configureWorld(minehubWorld);

        Location spawn = getSpawnLocation(minehubWorld);
        minehubWorld.setSpawnLocation(spawn);
        ensureSpawnPlatform(minehubWorld, spawn);

        plugin.getLogger().info("Minehub world ready: " + worldName + " spawn=("
                + spawn.getBlockX() + ", " + spawn.getBlockY() + ", " + spawn.getBlockZ() + ")");
    }

    private void configureWorld(World world) {
        world.setAutoSave(true);
        world.setKeepSpawnInMemory(false);

        world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(org.bukkit.GameRule.MOB_GRIEFING, false);

        world.setStorm(false);
        world.setThundering(false);
        world.setTime(6000L);

        // Minehub is a safe zone; do not allow natural mob spawning.
        world.setSpawnFlags(false, false);
    }

    private Location getSpawnLocation(World world) {
        String base = "skyblock.minehub-spawn.";
        double x = plugin.getConfig().getDouble(base + "x", 0.5D);
        double y = plugin.getConfig().getDouble(base + "y", 100.0D);
        double z = plugin.getConfig().getDouble(base + "z", 0.5D);
        float yaw = (float) plugin.getConfig().getDouble(base + "yaw", 0.0D);
        float pitch = (float) plugin.getConfig().getDouble(base + "pitch", 0.0D);
        return new Location(world, x, y, z, yaw, pitch);
    }

    private void ensureSpawnPlatform(World world, Location spawn) {
        if (world == null || spawn == null) {
            return;
        }

        int baseX = spawn.getBlockX();
        int baseY = spawn.getBlockY() - 1;
        int baseZ = spawn.getBlockZ();

        // Only create the initial spawn block if the spawn has no floor yet (avoid overwriting built hubs).
        if (!world.getBlockAt(baseX, baseY, baseZ).getType().isAir()) {
            return;
        }

        world.getBlockAt(baseX, baseY, baseZ).setType(Material.STONE, false);
    }
}
