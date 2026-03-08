package io.papermc.Grivience.zone;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages zone definitions, persistence, and zone lookups.
 * Handles loading/saving zones from YAML and provides API for zone queries.
 */
public final class ZoneManager {
    private final GriviencePlugin plugin;
    private final File zonesFile;
    private final Map<String, Zone> zonesById = new HashMap<>();
    
    // Cache for quick zone lookups by world
    private final Map<String, List<Zone>> zonesByWorld = new HashMap<>();
    
    private boolean enabled;
    private String defaultZoneName;
    private boolean showZoneOnJoin;
    private boolean updateZoneOnChange;
    
    public ZoneManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.zonesFile = new File(plugin.getDataFolder(), "zones.yml");
        loadConfig();
        loadZones();
    }
    
    /**
     * Load plugin configuration for zones.
     */
    private void loadConfig() {
        enabled = plugin.getConfig().getBoolean("zones.enabled", true);
        defaultZoneName = plugin.getConfig().getString("zones.default-zone-name", "Overworld");
        showZoneOnJoin = plugin.getConfig().getBoolean("zones.show-on-join", true);
        updateZoneOnChange = plugin.getConfig().getBoolean("zones.update-on-change", true);
    }
    
    /**
     * Load zones from YAML file.
     */
    public void loadZones() {
        zonesById.clear();
        zonesByWorld.clear();
        
        if (!zonesFile.exists()) {
            // Create default zones if file doesn't exist
            createDefaultZones();
            return;
        }
        
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(zonesFile);
        ConfigurationSection zonesSection = yaml.getConfigurationSection("zones");
        
        if (zonesSection == null) {
            createDefaultZones();
            return;
        }
        
        for (String zoneId : zonesSection.getKeys(false)) {
            ConfigurationSection zoneSection = zonesSection.getConfigurationSection(zoneId);
            Zone zone = Zone.fromSection(zoneId, zoneSection);
            if (zone != null) {
                addZone(zone);
            }
        }
        
        plugin.getLogger().info("Loaded " + zonesById.size() + " zones.");
    }
    
    /**
     * Save zones to YAML file.
     */
    public void saveZones() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection zonesSection = yaml.createSection("zones");
        
        for (Zone zone : zonesById.values()) {
            ConfigurationSection zoneSection = zonesSection.createSection(zone.getId());
            zone.save(zoneSection);
        }
        
        try {
            yaml.save(zonesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save zones: " + e.getMessage());
        }
    }
    
    /**
     * Create default zones for common areas.
     */
    private void createDefaultZones() {
        // Default zones will be created by admin using commands
        plugin.getLogger().info("No zones configured. Use /zone create <id> to create zones.");
    }
    
    /**
     * Add a zone to the registry.
     */
    public void addZone(Zone zone) {
        if (zone == null) return;
        
        zonesById.put(zone.getId(), zone);
        rebuildWorldCache();
    }
    
    /**
     * Remove a zone by ID.
     */
    public Zone removeZone(String id) {
        Zone removed = zonesById.remove(id);
        if (removed != null) {
            rebuildWorldCache();
            saveZones();
        }
        return removed;
    }
    
    /**
     * Get a zone by ID.
     */
    public Zone getZone(String id) {
        return zonesById.get(id);
    }
    
    /**
     * Get all zones.
     */
    public Collection<Zone> getAllZones() {
        return Collections.unmodifiableCollection(zonesById.values());
    }
    
    /**
     * Get all zones in a world.
     */
    public List<Zone> getZonesInWorld(String worldName) {
        return zonesByWorld.getOrDefault(worldName, new ArrayList<>());
    }
    
    /**
     * Get the zone at a location (highest priority first).
     */
    public Zone getZoneAt(Location location) {
        if (!enabled || location == null) {
            return null;
        }
        
        List<Zone> worldZones = getZonesInWorld(location.getWorld().getName());
        if (worldZones.isEmpty()) {
            return null;
        }
        
        // Sort by priority (highest first)
        return worldZones.stream()
                .filter(Zone::isEnabled)
                .filter(zone -> zone.contains(location))
                .max(Comparator.comparingInt(Zone::getPriority))
                .orElse(null);
    }
    
    /**
     * Get the zone display name for a player's current location.
     */
    public String getZoneName(Player player) {
        if (!enabled || player == null) {
            return defaultZoneName;
        }
        
        Zone zone = getZoneAt(player.getLocation());
        if (zone != null) {
            return zone.getColoredDisplayName();
        }
        
        return defaultZoneName;
    }
    
    /**
     * Check if a zone ID exists.
     */
    public boolean hasZone(String id) {
        return zonesById.containsKey(id);
    }
    
    /**
     * Update zone bounds.
     */
    public void updateZoneBounds(String zoneId, Location pos1, Location pos2) {
        Zone zone = getZone(zoneId);
        if (zone == null) return;
        
        zone.setPos1(pos1);
        zone.setPos2(pos2);
        zone.setWorld(pos1.getWorld().getName());
        rebuildWorldCache();
        saveZones();
    }
    
    /**
     * Rebuild the world cache for fast lookups.
     */
    private void rebuildWorldCache() {
        zonesByWorld.clear();
        
        for (Zone zone : zonesById.values()) {
            String world = zone.getWorld();
            if (world != null) {
                zonesByWorld.computeIfAbsent(world, k -> new ArrayList<>()).add(zone);
            }
        }
    }
    
    /**
     * Reload zones from config.
     */
    public void reload() {
        loadConfig();
        loadZones();
    }
    
    /**
     * Get all zone IDs.
     */
    public Set<String> getZoneIds() {
        return zonesById.keySet();
    }
    
    /**
     * Create a new zone with default values.
     */
    public Zone createZone(String id, String name, String displayName) {
        if (hasZone(id)) {
            return null;
        }
        
        Zone zone = new Zone(id);
        zone.setName(name != null ? name : id);
        zone.setDisplayName(displayName != null ? displayName : name);
        addZone(zone);
        saveZones();
        return zone;
    }
    
    /**
     * Set zone display properties.
     */
    public void setZoneDisplay(String zoneId, String displayName, ChatColor color, int priority) {
        Zone zone = getZone(zoneId);
        if (zone == null) return;
        
        if (displayName != null) {
            zone.setDisplayName(displayName);
        }
        if (color != null) {
            zone.setColor(color);
        }
        zone.setPriority(priority);
        saveZones();
    }
    
    /**
     * Set zone enabled state.
     */
    public void setZoneEnabled(String zoneId, boolean enabled) {
        Zone zone = getZone(zoneId);
        if (zone == null) return;
        
        zone.setEnabled(enabled);
        saveZones();
    }
    
    /**
     * Set zone description.
     */
    public void setZoneDescription(String zoneId, String description) {
        Zone zone = getZone(zoneId);
        if (zone == null) return;
        
        zone.setDescription(description);
        saveZones();
    }
    
    /**
     * Get zones that a player has selected (for editing).
     */
    private final Map<UUID, String> playerSelection = new HashMap<>();
    
    public void setPlayerSelection(Player player, String zoneId) {
        if (zoneId == null) {
            playerSelection.remove(player.getUniqueId());
        } else {
            playerSelection.put(player.getUniqueId(), zoneId);
        }
    }
    
    public String getPlayerSelection(Player player) {
        return playerSelection.get(player.getUniqueId());
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public String getDefaultZoneName() {
        return defaultZoneName;
    }
    
    public boolean isShowOnJoin() {
        return showZoneOnJoin;
    }
    
    public boolean isUpdateOnChange() {
        return updateZoneOnChange;
    }
}
