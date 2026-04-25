package io.papermc.Grivience.jumppad;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Jump pad registry with persistent storage in jumppads.yml.
 * Refined to prevent data wiping during partial updates and improve area handling.
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

    private JumpPad getOrCreate(String id) {
        return pads.computeIfAbsent(normalizeId(id), k -> new JumpPad());
    }

    public synchronized void setLaunch(String id, Location launch) {
        if (id == null || launch == null) return;
        getOrCreate(id).setLaunch(launch.clone());
        save();
    }

    public synchronized void setPos1(String id, Location pos1) {
        if (id == null || pos1 == null) return;
        getOrCreate(id).setLaunch(pos1.clone());
        save();
    }

    public synchronized void setPos2(String id, Location pos2) {
        if (id == null || pos2 == null) return;
        getOrCreate(id).setLaunchCorner(pos2.clone());
        save();
    }

    public synchronized void setTarget(String id, Location target) {
        if (id == null || target == null) return;
        getOrCreate(id).setTarget(target.clone());
        save();
    }

    public synchronized void setTargetCorner(String id, Location targetCorner) {
        if (id == null || targetCorner == null) return;
        getOrCreate(id).setTargetCorner(targetCorner.clone());
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
            if (pad == null) continue;

            ConfigurationSection section = root.createSection(id);
            writeLocation(section, "launch", pad.getLaunch(), pad.getLaunchWorldName());
            writeLocation(section, "launch-corner", pad.getLaunchCorner(), pad.getLaunchCornerWorldName());
            writeLocation(section, "target", pad.getTarget(), pad.getTargetWorldName());
            writeLocation(section, "target-corner", pad.getTargetCorner(), pad.getTargetCornerWorldName());

            // Save requirements
            if (pad.getMinSkyblockLevel() > 0) section.set("min-skyblock-level", pad.getMinSkyblockLevel());
            if (pad.getMinCombatLevel() > 0) section.set("min-combat-level", pad.getMinCombatLevel());
            if (pad.getMinMiningLevel() > 0) section.set("min-mining-level", pad.getMinMiningLevel());
            if (pad.getMinFarmingLevel() > 0) section.set("min-farming-level", pad.getMinFarmingLevel());
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
        if (!dataFile.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection root = yaml.getConfigurationSection(ROOT_KEY);
        if (root == null) return;

        for (String rawId : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(rawId);
            if (section == null) continue;

            JumpPad pad = new JumpPad();
            pad.setLaunch(readLocation(section.getConfigurationSection("launch"), pad::setLaunchWorldName));
            pad.setLaunchCorner(readLocation(section.getConfigurationSection("launch-corner"), pad::setLaunchCornerWorldName));
            pad.setTarget(readLocation(section.getConfigurationSection("target"), pad::setTargetWorldName));
            pad.setTargetCorner(readLocation(section.getConfigurationSection("target-corner"), pad::setTargetCornerWorldName));

            // Load requirements
            pad.setMinSkyblockLevel(section.getInt("min-skyblock-level", 0));
            pad.setMinCombatLevel(section.getInt("min-combat-level", 0));
            pad.setMinMiningLevel(section.getInt("min-mining-level", 0));
            pad.setMinFarmingLevel(section.getInt("min-farming-level", 0));

            pads.put(normalizeId(rawId), pad);
        }
    }

    private void writeLocation(ConfigurationSection section, String key, Location location, String worldName) {
        if (section == null || key == null) return;
        
        String effectiveWorldName = (location != null && location.getWorld() != null) 
            ? location.getWorld().getName() 
            : worldName;
            
        if (effectiveWorldName == null || effectiveWorldName.isBlank()) return;

        ConfigurationSection locSection = section.createSection(key);
        locSection.set("world", effectiveWorldName);
        
        if (location != null) {
            locSection.set("x", location.getX());
            locSection.set("y", location.getY());
            locSection.set("z", location.getZ());
            locSection.set("yaw", location.getYaw());
            locSection.set("pitch", location.getPitch());
        }
    }

    private Location readLocation(ConfigurationSection section, java.util.function.Consumer<String> worldNameSetter) {
        if (section == null) return null;

        String worldName = section.getString("world");
        if (worldName == null || worldName.isBlank()) return null;
        
        if (worldNameSetter != null) {
            worldNameSetter.accept(worldName);
        }

        World world = Bukkit.getWorld(worldName);
        
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

    public synchronized JumpPad getPadAtLocation(Location location) {
        for (JumpPad pad : pads.values()) {
            if (pad.isWithinLaunchArea(location)) return pad;
        }
        return null;
    }

    /**
     * Represents a single Jump Pad configuration.
     */
    public static final class JumpPad {
        private Location launch;
        private String launchWorldName;
        
        private Location launchCorner;
        private String launchCornerWorldName;
        
        private Location target;
        private String targetWorldName;
        
        private Location targetCorner;
        private String targetCornerWorldName;

        // Requirements
        private int minSkyblockLevel = 0;
        private int minCombatLevel = 0;
        private int minMiningLevel = 0;
        private int minFarmingLevel = 0;

        public JumpPad() {}

        public boolean isValid() {
            return (launch != null || launchWorldName != null) && (target != null || targetWorldName != null);
        }

        public Location getLaunch() { return launch; }
        public void setLaunch(Location launch) { 
            this.launch = launch; 
            if (launch != null && launch.getWorld() != null) {
                this.launchWorldName = launch.getWorld().getName();
            }
        }
        
        public String getLaunchWorldName() { return launchWorldName; }
        public void setLaunchWorldName(String name) { this.launchWorldName = name; }

        public Location getLaunchCorner() { return launchCorner; }
        public void setLaunchCorner(Location launchCorner) { 
            this.launchCorner = launchCorner; 
            if (launchCorner != null && launchCorner.getWorld() != null) {
                this.launchCornerWorldName = launchCorner.getWorld().getName();
            }
        }
        
        public String getLaunchCornerWorldName() { return launchCornerWorldName; }
        public void setLaunchCornerWorldName(String name) { this.launchCornerWorldName = name; }

        public Location getTarget() { return target; }
        public void setTarget(Location target) { 
            this.target = target; 
            if (target != null && target.getWorld() != null) {
                this.targetWorldName = target.getWorld().getName();
            }
        }
        
        public String getTargetWorldName() { return targetWorldName; }
        public void setTargetWorldName(String name) { this.targetWorldName = name; }

        public Location getTargetCorner() { return targetCorner; }
        public void setTargetCorner(Location targetCorner) { 
            this.targetCorner = targetCorner; 
            if (targetCorner != null && targetCorner.getWorld() != null) {
                this.targetCornerWorldName = targetCorner.getWorld().getName();
            }
        }
        
        public String getTargetCornerWorldName() { return targetCornerWorldName; }
        public void setTargetCornerWorldName(String name) { this.targetCornerWorldName = name; }

        public int getMinSkyblockLevel() { return minSkyblockLevel; }
        public void setMinSkyblockLevel(int minSkyblockLevel) { this.minSkyblockLevel = minSkyblockLevel; }

        public int getMinCombatLevel() { return minCombatLevel; }
        public void setMinCombatLevel(int minCombatLevel) { this.minCombatLevel = minCombatLevel; }

        public int getMinMiningLevel() { return minMiningLevel; }
        public void setMinMiningLevel(int minMiningLevel) { this.minMiningLevel = minMiningLevel; }

        public int getMinFarmingLevel() { return minFarmingLevel; }
        public void setMinFarmingLevel(int minFarmingLevel) { this.minFarmingLevel = minFarmingLevel; }

        public boolean isWithinLaunchArea(Location location) {
            if (location == null) return false;
            
            // Ensure we are in the same world
            if (launchWorldName == null || !launchWorldName.equalsIgnoreCase(location.getWorld().getName())) {
                return false;
            }
            
            // Resolve world if needed
            if (launch != null && launch.getWorld() == null) {
                launch.setWorld(location.getWorld());
            }
            if (launchCorner != null && launchCorner.getWorld() == null) {
                launchCorner.setWorld(location.getWorld());
            }

            if (launchCorner == null) {
                return launch.getBlockX() == location.getBlockX()
                        && launch.getBlockY() == location.getBlockY()
                        && launch.getBlockZ() == location.getBlockZ();
            }

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

        public Location getLaunchCenter() {
            if (launch == null) return null;
            if (launch.getWorld() == null && launchWorldName != null) {
                World w = Bukkit.getWorld(launchWorldName);
                if (w != null) launch.setWorld(w);
            }
            if (launch.getWorld() == null) return null;
            
            if (launchCorner == null) return launch.clone();
            
            if (launchCorner.getWorld() == null && launchCornerWorldName != null) {
                World w = Bukkit.getWorld(launchCornerWorldName);
                if (w != null) launchCorner.setWorld(w);
            }

            return new Location(
                    launch.getWorld(),
                    (launch.getX() + launchCorner.getX()) / 2.0,
                    (launch.getY() + launchCorner.getY()) / 2.0,
                    (launch.getZ() + launchCorner.getZ()) / 2.0,
                    launch.getYaw(),
                    launch.getPitch()
            );
        }

        public Location getTargetCenter() {
            if (target == null) return null;
            if (target.getWorld() == null && targetWorldName != null) {
                World w = Bukkit.getWorld(targetWorldName);
                if (w != null) target.setWorld(w);
            }
            if (target.getWorld() == null) return null;

            if (targetCorner == null) return target.clone();
            
            if (targetCorner.getWorld() == null && targetCornerWorldName != null) {
                World w = Bukkit.getWorld(targetCornerWorldName);
                if (w != null) targetCorner.setWorld(w);
            }

            return new Location(
                    target.getWorld(),
                    (target.getX() + targetCorner.getX()) / 2.0,
                    (target.getY() + targetCorner.getY()) / 2.0,
                    (target.getZ() + targetCorner.getZ()) / 2.0,
                    target.getYaw(),
                    target.getPitch()
            );
        }
    }
}
