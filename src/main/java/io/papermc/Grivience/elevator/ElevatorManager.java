package io.papermc.Grivience.elevator;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ElevatorManager {
    private final GriviencePlugin plugin;
    private final File configFile;
    private FileConfiguration config;
    private final Map<String, Elevator> elevators = new HashMap<>();

    public ElevatorManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "elevators.yml");
        load();
    }

    public void load() {
        elevators.clear();
        if (!configFile.exists()) {
            try {
                configFile.getParentFile().mkdirs();
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create elevators.yml", e);
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        ConfigurationSection elevatorsSection = config.getConfigurationSection("elevators");
        if (elevatorsSection == null) return;

        for (String id : elevatorsSection.getKeys(false)) {
            String path = "elevators." + id;
            String name = config.getString(path + ".name", id);
            Elevator elevator = new Elevator(id, ChatColor.translateAlternateColorCodes('&', name));

            ConfigurationSection floorsSection = config.getConfigurationSection(path + ".floors");
            if (floorsSection != null) {
                for (String floorKey : floorsSection.getKeys(false)) {
                    String floorPath = path + ".floors." + floorKey;
                    String floorName = config.getString(floorPath + ".name", floorKey);
                    Material iconMat = Material.valueOf(config.getString(floorPath + ".icon", "STONE"));
                    Location loc = config.getLocation(floorPath + ".location");
                    String reqLayer = config.getString(floorPath + ".required-layer", "");
                    
                    if (loc != null) {
                        elevator.addFloor(new ElevatorFloor(ChatColor.translateAlternateColorCodes('&', floorName), new ItemStack(iconMat), loc, reqLayer));
                    }
                }
            }
            elevators.put(id.toLowerCase(), elevator);
        }
    }

    public void save() {
        config.set("elevators", null);
        for (Elevator elevator : elevators.values()) {
            String path = "elevators." + elevator.getId();
            config.set(path + ".name", elevator.getDisplayName());
            
            int i = 0;
            for (ElevatorFloor floor : elevator.getFloors()) {
                String floorPath = path + ".floors.f" + i;
                config.set(floorPath + ".name", floor.name());
                config.set(floorPath + ".icon", floor.icon().getType().name());
                config.set(floorPath + ".location", floor.location());
                config.set(floorPath + ".required-layer", floor.requiredLayer());
                i++;
            }
        }
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save elevators.yml", e);
        }
    }

    public Elevator createElevator(String id, String name) {
        Elevator elevator = new Elevator(id, ChatColor.translateAlternateColorCodes('&', name));
        elevators.put(id.toLowerCase(), elevator);
        save();
        return elevator;
    }

    public Elevator getElevator(String id) {
        return elevators.get(id.toLowerCase());
    }

    public Map<String, Elevator> getElevators() {
        return elevators;
    }

    public void removeElevator(String id) {
        elevators.remove(id.toLowerCase());
        save();
    }
}
