package io.papermc.Grivience.mines.end;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Owns the End Mines world lifecycle (creation/config/spawn).
 * Layout generation, drops, mobs, etc. are layered on top of this in other components.
 */
public final class EndMinesManager {
    private final GriviencePlugin plugin;
    private World endMinesWorld;
    private BukkitRunnable generationTask;

    public EndMinesManager(GriviencePlugin plugin) {
        this.plugin = plugin;
    }

    public String getZoneName(Location loc) {
        if (loc == null || loc.getWorld() == null || endMinesWorld == null || !loc.getWorld().equals(endMinesWorld)) {
            return null;
        }

        Location anchor = getSpawnLocation(endMinesWorld);
        if (anchor == null) return "Unknown";

        double dx = loc.getX() - anchor.getX();
        double dz = loc.getZ() - anchor.getZ();
        double distSq = dx * dx + dz * dz;

        // Hub Area
        if (distSq < 15 * 15) return "End Hub";

        // Zone Rooms (Approximate based on LayoutGenerator offsets of 34)
        if (dz < -25 && Math.abs(dx) < 15) return "Chorus Grove";
        if (dz > 25 && Math.abs(dx) < 15) return "Obsidian Quarry";
        if (dx > 25 && Math.abs(dz) < 15) return "Crystal Cavern";
        if (dx < -25 && Math.abs(dz) < 15) return "Rift Gallery";

        return "Deep Mines";
    }

    public java.util.List<String> getAllZoneNames() {
        return java.util.List.of("End Hub", "Chorus Grove", "Obsidian Quarry", "Crystal Cavern", "Rift Gallery", "Deep Mines");
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("end-mines.enabled", true);
    }

    public String getWorldName() {
        return plugin.getConfig().getString("end-mines.world-name", "skyblock_end_mines");
    }

    public World getWorld() {
        if (endMinesWorld == null) {
            initializeWorld();
        }
        return endMinesWorld;
    }

    public void initializeWorld() {
        if (!isEnabled()) {
            return;
        }

        String worldName = getWorldName();
        endMinesWorld = Bukkit.getWorld(worldName);
        if (endMinesWorld != null) {
            configureWorld(endMinesWorld);
            ensureSpawnPlatform(endMinesWorld, getSpawnLocation(endMinesWorld));
            ensureSpawnZone(endMinesWorld);
            maybeAutoGenerate(endMinesWorld);
            plugin.getLogger().info("End Mines world loaded: " + worldName);
            return;
        }

        plugin.getLogger().info("Creating End Mines void world: " + worldName);
        WorldCreator creator = WorldCreator.name(worldName)
                .environment(World.Environment.THE_END)
                .generator(new EndMinesWorldGenerator())
                .generateStructures(false);

        endMinesWorld = creator.createWorld();
        if (endMinesWorld == null) {
            plugin.getLogger().severe("Failed to create End Mines world: " + worldName);
            return;
        }

        configureWorld(endMinesWorld);

        Location spawn = getSpawnLocation(endMinesWorld);
        endMinesWorld.setSpawnLocation(spawn);
        ensureSpawnPlatform(endMinesWorld, spawn);
        ensureSpawnZone(endMinesWorld);
        maybeAutoGenerate(endMinesWorld);

        plugin.getLogger().info("End Mines world configured: " + worldName + " spawn=(" +
                String.format("%.1f", spawn.getX()) + ", " +
                String.format("%.1f", spawn.getY()) + ", " +
                String.format("%.1f", spawn.getZ()) + ")");
    }

    public boolean isGenerated() {
        return plugin.getConfig().getBoolean("end-mines.generated", false);
    }

    public boolean isGenerating() {
        return generationTask != null;
    }

    private void maybeAutoGenerate(World world) {
        if (world == null) {
            return;
        }
        if (!plugin.getConfig().getBoolean("end-mines.auto-generate", true)) {
            return;
        }
        if (isGenerated() || isGenerating()) {
            return;
        }
        // Defer one tick so the server finishes enabling before heavy edits begin.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> generateLayout(false), 1L);
    }

    public void generateLayout(boolean force) {
        if (!isEnabled()) {
            return;
        }
        if (isGenerating()) {
            plugin.getLogger().warning("End Mines generation already in progress.");
            return;
        }
        if (!force && isGenerated()) {
            return;
        }

        World world = getWorld();
        if (world == null) {
            plugin.getLogger().severe("Cannot generate End Mines layout: world is not loaded.");
            return;
        }

        Location anchor = getSpawnLocation(world);
        plugin.getLogger().info("Generating End Mines layout at (" +
                anchor.getBlockX() + ", " + anchor.getBlockY() + ", " + anchor.getBlockZ() + ") in " + world.getName());

        generationTask = new EndMinesLayoutGenerator(plugin).generate(world, anchor, () -> {
            generationTask = null;
            plugin.getConfig().set("end-mines.generated", true);
            plugin.saveConfig();
            plugin.getLogger().info("End Mines layout generation complete.");
        });
    }

    private void configureWorld(World world) {
        if (world == null) {
            return;
        }

        world.setAutoSave(true);
        world.setKeepSpawnInMemory(false);

        // Keep the dimension stable; the End has no time cycle but these rules still help.
        world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(org.bukkit.GameRule.MOB_GRIEFING, false);
        world.setGameRule(org.bukkit.GameRule.DO_INSOMNIA, false);
        world.setGameRule(org.bukkit.GameRule.DO_TRADER_SPAWNING, false);

        // Disable natural spawning; custom monsters handle combat.
        world.setSpawnFlags(false, false);
    }

    public Location getSpawnLocation(World world) {
        String base = "end-mines.spawn.";
        double x = plugin.getConfig().getDouble(base + "x", 0.5D);
        double y = plugin.getConfig().getDouble(base + "y", 80.0D);
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

        for (int x = baseX - 2; x <= baseX + 2; x++) {
            for (int z = baseZ - 2; z <= baseZ + 2; z++) {
                world.getBlockAt(x, baseY, z).setType(Material.OBSIDIAN, false);
            }
        }
        // Ensure air at spawn position.
        world.getBlockAt(baseX, baseY + 1, baseZ).setType(Material.AIR, false);
        world.getBlockAt(baseX, baseY + 2, baseZ).setType(Material.AIR, false);
    }

    private void ensureSpawnZone(World world) {
        if (world == null || plugin.getZoneManager() == null) {
            return;
        }

        String zoneId = "endmines_spawn";
        io.papermc.Grivience.zone.Zone zone = plugin.getZoneManager().getZone(zoneId);
        
        if (zone == null) {
            zone = plugin.getZoneManager().createZone(zoneId, "Endmines Spawn Area", "§bEndmines Spawn");
        }

        if (zone != null) {
            // User requested: X-cord:56 Y-cord:62 Z-cord: -38 To X-cord: 56 Y-cord: 87 Z-cord: 41
            // Using 56.0 to 57.0 for X to cover the block at 56.
            Location pos1 = new Location(world, 56.0, 62.0, -38.0);
            Location pos2 = new Location(world, 57.0, 87.0, 41.0);
            
            zone.setWorld(world.getName());
            zone.setPos1(pos1);
            zone.setPos2(pos2);
            zone.setPriority(100);
            zone.setColor(org.bukkit.ChatColor.AQUA);
            
            // Apply requested protections
            zone.setCanPvP(false);
            zone.setCanSpawnMobs(false);
            zone.setCanBreakBlocks(false);
            
            plugin.getZoneManager().saveZones();
        }
    }

    public boolean teleportToEndMines(org.bukkit.entity.Player player) {
        if (player == null) {
            return false;
        }
        if (!isEnabled()) {
            player.sendMessage(org.bukkit.ChatColor.RED + "End Mines is disabled.");
            return false;
        }

        World world = getWorld();
        if (world == null) {
            player.sendMessage(org.bukkit.ChatColor.RED + "End Mines world '" + getWorldName() + "' not found. Contact an administrator.");
            return false;
        }

        Location spawn = getSpawnLocation(world);
        player.teleport(spawn);
        player.playSound(spawn, org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
        return true;
    }

    public void shutdown() {
        if (generationTask != null) {
            generationTask.cancel();
            generationTask = null;
        }
    }
}

