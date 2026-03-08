package io.papermc.Grivience.jumppad;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Jump pad registry with persistent storage in jumppads.yml.
 */
public final class JumpPadManager {
    private static final String ROOT_KEY = "pads";

    private final GriviencePlugin plugin;
    private final File dataFile;
    private final Map<String, JumpPad> pads = new HashMap<>();

    public JumpPadManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "jumppads.yml");
        load();
    }

    public synchronized void setLaunch(String id, Location launch) {
        if (id == null || launch == null) return;
        String key = normalizeId(id);
        JumpPad pad = pads.getOrDefault(key, new JumpPad(null, null, null, null));
        pads.put(key, new JumpPad(launch.clone(), null, pad.target(), pad.targetCorner()));
        save();
    }

    public synchronized void setTarget(String id, Location target) {
        if (id == null || target == null) return;
        String key = normalizeId(id);
        JumpPad pad = pads.getOrDefault(key, new JumpPad(null, null, null, null));
        pads.put(key, new JumpPad(pad.launch(), null, target.clone(), null));
        save();
    }

    public synchronized void setPos1(String id, Location pos1) {
        if (id == null || pos1 == null) return;
        String key = normalizeId(id);
        JumpPad pad = pads.getOrDefault(key, new JumpPad(null, null, null, null));
        // pos1 is the first launch-area corner.
        pads.put(key, new JumpPad(pos1.clone(), pad.launchCorner(), pad.target(), pad.targetCorner()));
        save();
    }

    public synchronized void setPos2(String id, Location pos2) {
        if (id == null || pos2 == null) return;
        String key = normalizeId(id);
        JumpPad pad = pads.getOrDefault(key, new JumpPad(null, null, null, null));
        pads.put(key, new JumpPad(pad.launch(), pos2.clone(), pad.target(), pad.targetCorner()));
        save();
    }

    public synchronized void setTargetCorner(String id, Location targetCorner) {
        if (id == null || targetCorner == null) return;
        String key = normalizeId(id);
        JumpPad pad = pads.getOrDefault(key, new JumpPad(null, null, null, null));
        pads.put(key, new JumpPad(pad.launch(), pad.launchCorner(), pad.target(), targetCorner.clone()));
        save();
    }

    public synchronized JumpPad pad(String id) {
        if (id == null) return null;
        return pads.get(normalizeId(id));
    }

    public synchronized Map<String, JumpPad> allPads() {
        return Collections.unmodifiableMap(pads);
    }

    public synchronized void remove(String id) {
        if (id == null) return;
        if (pads.remove(normalizeId(id)) != null) {
            save();
        }
    }

    public synchronized void reload() {
        load();
    }

    public synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection root = yaml.createSection(ROOT_KEY);

        for (Map.Entry<String, JumpPad> entry : pads.entrySet()) {
            String id = entry.getKey();
            JumpPad pad = entry.getValue();
            if (pad == null) {
                continue;
            }

            ConfigurationSection section = root.createSection(id);
            writeLocation(section, "launch", pad.launch());
            writeLocation(section, "launch-corner", pad.launchCorner());
            writeLocation(section, "target", pad.target());
            writeLocation(section, "target-corner", pad.targetCorner());
        }

        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Could not create plugin data folder for jump pad persistence.");
            }
            yaml.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save jump pads to " + dataFile.getName() + ": " + exception.getMessage());
        }
    }

    private synchronized void load() {
        pads.clear();
        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection root = yaml.getConfigurationSection(ROOT_KEY);
        if (root == null) {
            return;
        }

        for (String rawId : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(rawId);
            if (section == null) {
                continue;
            }

            Location launch = readLocation(section.getConfigurationSection("launch"));
            Location launchCorner = readLocation(section.getConfigurationSection("launch-corner"));
            Location target = readLocation(section.getConfigurationSection("target"));
            Location targetCorner = readLocation(section.getConfigurationSection("target-corner"));

            if (launch == null && launchCorner == null && target == null && targetCorner == null) {
                continue;
            }

            String id = normalizeId(rawId);
            pads.put(id, new JumpPad(launch, launchCorner, target, targetCorner));
        }
    }

    private void writeLocation(ConfigurationSection section, String key, Location location) {
        if (section == null || key == null || location == null || location.getWorld() == null) {
            return;
        }

        ConfigurationSection locSection = section.createSection(key);
        locSection.set("world", location.getWorld().getName());
        locSection.set("x", location.getX());
        locSection.set("y", location.getY());
        locSection.set("z", location.getZ());
        locSection.set("yaw", location.getYaw());
        locSection.set("pitch", location.getPitch());
    }

    private Location readLocation(ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        String worldName = section.getString("world");
        if (worldName == null || worldName.isBlank()) {
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }

        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw", 0.0D);
        float pitch = (float) section.getDouble("pitch", 0.0D);
        return new Location(world, x, y, z, yaw, pitch);
    }

    private String normalizeId(String id) {
        return id.toLowerCase(Locale.ROOT);
    }

    /**
     * Check if a location is within the launch area of any jump pad.
     * @param location The location to check
     * @return The JumpPad if the location is within its launch area, null otherwise
     */
    public synchronized JumpPad getPadAtLocation(Location location) {
        for (JumpPad pad : pads.values()) {
            if (pad.isWithinLaunchArea(location)) {
                return pad;
            }
        }
        return null;
    }

    public record JumpPad(
            Location launch,
            Location launchCorner,  // pos2 for launch area
            Location target,
            Location targetCorner   // pos2 for target area (for future use)
    ) {
        /**
         * Check if a location is within the launch area.
         * If launchCorner is null, checks for exact block match with launch.
         * If launchCorner is set, checks if location is within the cuboid defined by launch and launchCorner.
         */
        public boolean isWithinLaunchArea(Location location) {
            if (launch == null || location == null || launch.getWorld() == null) {
                return false;
            }

            if (!launch.getWorld().equals(location.getWorld())) {
                return false;
            }

            if (launchCorner == null) {
                // Single block launch pad
                return launch.getBlockX() == location.getBlockX()
                        && launch.getBlockY() == location.getBlockY()
                        && launch.getBlockZ() == location.getBlockZ();
            }

            // Area-based launch pad - check if within cuboid
            int minX = Math.min(launch.getBlockX(), launchCorner.getBlockX());
            int maxX = Math.max(launch.getBlockX(), launchCorner.getBlockX());
            int minY = Math.min(launch.getBlockY(), launchCorner.getBlockY());
            int maxY = Math.max(launch.getBlockY(), launchCorner.getBlockY());
            int minZ = Math.min(launch.getBlockZ(), launchCorner.getBlockZ());
            int maxZ = Math.max(launch.getBlockZ(), launchCorner.getBlockZ());

            return location.getBlockX() >= minX && location.getBlockX() <= maxX
                    && location.getBlockY() >= minY && location.getBlockY() <= maxY
                    && location.getBlockZ() >= minZ && location.getBlockZ() <= maxZ;
        }

        /**
         * Get the center of the launch area.
         */
        public Location getLaunchCenter() {
            if (launch == null) return null;

            if (launchCorner == null) {
                return launch.clone();
            }

            return new Location(
                    launch.getWorld(),
                    (launch.getX() + launchCorner.getX()) / 2.0,
                    (launch.getY() + launchCorner.getY()) / 2.0,
                    (launch.getZ() + launchCorner.getZ()) / 2.0
            );
        }

        /**
         * Get the center of the target area.
         */
        public Location getTargetCenter() {
            if (target == null) {
                return null;
            }

            if (targetCorner == null) {
                return target.clone();
            }

            return new Location(
                    target.getWorld(),
                    (target.getX() + targetCorner.getX()) / 2.0,
                    (target.getY() + targetCorner.getY()) / 2.0,
                    (target.getZ() + targetCorner.getZ()) / 2.0
            );
        }
    }
}
