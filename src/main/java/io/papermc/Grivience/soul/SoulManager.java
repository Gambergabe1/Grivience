package io.papermc.Grivience.soul;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SoulManager {
    private final GriviencePlugin plugin;
    private final Map<String, Location> soulLocations = new HashMap<>();
    private File file;
    private FileConfiguration config;

    public SoulManager(GriviencePlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        soulLocations.clear();
        file = new File(plugin.getDataFolder(), "souls.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ignored) {
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
        if (config.contains("souls")) {
            for (String key : config.getConfigurationSection("souls").getKeys(false)) {
                Location loc = config.getLocation("souls." + key);
                if (loc != null) {
                    soulLocations.put(key, loc);
                }
            }
        }
    }

    public void save() {
        if (config == null || file == null) return;
        config.set("souls", null);
        for (Map.Entry<String, Location> entry : soulLocations.entrySet()) {
            config.set("souls." + entry.getKey(), entry.getValue());
        }
        try {
            config.save(file);
        } catch (IOException ignored) {
        }
    }

    public void addSoul(String id, Location loc) {
        soulLocations.put(id, loc);
        save();
    }

    public void removeSoul(String id) {
        soulLocations.remove(id);
        save();
    }

    public String getSoulAt(Location loc) {
        for (Map.Entry<String, Location> entry : soulLocations.entrySet()) {
            Location entryLoc = entry.getValue();
            if (entryLoc.getWorld() != null && entryLoc.getWorld().equals(loc.getWorld()) &&
                entryLoc.getBlockX() == loc.getBlockX() &&
                entryLoc.getBlockY() == loc.getBlockY() &&
                entryLoc.getBlockZ() == loc.getBlockZ()) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Map<String, Location> getSouls() {
        return soulLocations;
    }

    public int getTotalSouls() {
        return soulLocations.size();
    }

    public int getDiscoveredCount(Player player) {
        if (plugin.getProfileManager() == null) return 0;
        SkyBlockProfile profile = plugin.getProfileManager().getSelectedProfile(player);
        if (profile == null) return 0;
        return profile.getDiscoveredSouls().size();
    }

    public double getHealthBonus(Player player) {
        return getDiscoveredCount(player) * 1.0;
    }

    public double getDefenseBonus(Player player) {
        return getDiscoveredCount(player) * 0.5;
    }

    public double getStrengthBonus(Player player) {
        return getDiscoveredCount(player) * 0.2;
    }
}
