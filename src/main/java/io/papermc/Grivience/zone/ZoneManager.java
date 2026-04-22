package io.papermc.Grivience.zone;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Manages zone definitions, persistence, and zone lookups.
 * Handles loading/saving zones from YAML and provides API for zone queries.
 */
public final class ZoneManager {
    private static final String HUB_FARM_ZONE_ID = "hub_farm";
    private static final Comparator<Zone> PRIORITY_DESCENDING = Comparator.comparingInt(Zone::getPriority).reversed();

    private final GriviencePlugin plugin;
    private final File zonesFile;
    private final Map<String, Zone> zonesById = new HashMap<>();

    // Cache for quick zone lookups by world.
    private final Map<String, List<Zone>> zonesByWorld = new HashMap<>();
    private final Map<UUID, PlayerZoneSnapshot> playerZoneCache = new HashMap<>();

    private org.bukkit.scheduler.BukkitTask dynamicTask;

    private boolean enabled;
    private String defaultZoneName;
    private boolean showZoneOnJoin;
    private boolean updateZoneOnChange;

    /**
     * Get zones that a player has selected (for editing).
     */
    private final Map<UUID, String> playerSelection = new HashMap<>();

    public ZoneManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.zonesFile = new File(plugin.getDataFolder(), "zones.yml");
        loadConfig();
        loadZones();
        startDynamicTask();
    }

    private void startDynamicTask() {
        dynamicTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            boolean changed = false;
            List<String> toRemove = new ArrayList<>();

            for (Zone zone : zonesById.values()) {
                if (zone.isExpired()) {
                    toRemove.add(zone.getId());
                    changed = true;
                    continue;
                }

                if (zone.getAttachedEntityId() != null) {
                    org.bukkit.entity.Entity entity = plugin.getServer().getEntity(zone.getAttachedEntityId());
                    if (entity == null || !entity.isValid()) {
                        // Attached entity is gone, zone should probably expire or stop moving
                        if (zone.getExpiryTime() == -1) {
                            zone.setExpiryTime(System.currentTimeMillis() + 5000); // Expire in 5s
                        }
                    } else {
                        zone.updateLocation(entity.getLocation());
                        changed = true;
                    }
                }
            }

            for (String id : toRemove) {
                zonesById.remove(id);
            }

            if (changed) {
                rebuildWorldCache();
            }
        }, 1L, 1L);
    }

    public void stop() {
        if (dynamicTask != null) {
            dynamicTask.cancel();
        }
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
        playerZoneCache.clear();

        if (!zonesFile.exists()) {
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
                zonesById.put(zone.getId(), zone);
            }
        }

        importConfigAreas();
        rebuildWorldCache();
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
        plugin.getLogger().info("No zones configured. Use /zone create <id> to create zones.");
    }

    /**
     * Add a zone to the registry.
     */
    public void addZone(Zone zone) {
        if (zone == null) {
            return;
        }

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
        return zonesByWorld.getOrDefault(worldName, Collections.emptyList());
    }

    /**
     * Get the zone at a location (highest priority first).
     */
    public Zone getZoneAt(Location location) {
        if (!enabled || location == null || location.getWorld() == null) {
            return null;
        }
        return resolveZoneAt(location);
    }

    /**
     * Get all zones at a location (sorted by priority descending).
     */
    public List<Zone> getZonesAt(Location location) {
        if (!enabled || location == null || location.getWorld() == null) {
            return Collections.emptyList();
        }
        List<Zone> result = new ArrayList<>();
        List<Zone> worldZones = getZonesInWorld(location.getWorld().getName());
        for (Zone zone : worldZones) {
            if (zone.isEnabled() && zone.contains(location)) {
                result.add(zone);
            }
        }
        return result;
    }

    public Zone getZoneAt(Player player) {
        if (!enabled || player == null) {
            return null;
        }

        Location location = player.getLocation();
        if (location == null || location.getWorld() == null) {
            return null;
        }

        UUID playerId = player.getUniqueId();
        PlayerZoneSnapshot snapshot = playerZoneCache.get(playerId);
        if (snapshot != null && snapshot.matches(location)) {
            return snapshot.zone();
        }

        Zone zone = resolveZoneAt(location);
        playerZoneCache.put(playerId, PlayerZoneSnapshot.from(location, zone));
        return zone;
    }

    /**
     * Get the zone display name for a player's current location.
     */
    public String getZoneName(Player player) {
        if (!enabled || player == null) {
            return defaultZoneName;
        }

        Zone zone = getZoneAt(player);
        if (zone != null) {
            return getFormattedZoneName(zone);
        }

        return defaultZoneName;
    }

    public String getZoneDisplayName(String zoneId) {
        Zone zone = getZone(zoneId);
        if (zone == null) {
            return defaultZoneName;
        }
        return getFormattedZoneName(zone);
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
        if (zone == null) {
            return;
        }

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
        playerZoneCache.clear();

        for (Zone zone : zonesById.values()) {
            String world = zone.getWorld();
            if (world != null) {
                zonesByWorld.computeIfAbsent(world, ignored -> new ArrayList<>()).add(zone);
            }
        }

        for (List<Zone> worldZones : zonesByWorld.values()) {
            worldZones.sort(PRIORITY_DESCENDING);
        }
    }

    /**
     * Reload zones from config.
     */
    public void reload() {
        loadConfig();
        loadZones();
        importConfigAreas();
    }

    private void importConfigAreas() {
        // Main Hub Zone - Base for all sub-zones
        String hubWorld = plugin.getConfig().getString("skyblock.hub-world", "Hub 2");
        importHardcodedArea("hub_main", "The Hub", hubWorld, 
            -1000, 0, -1000, 1000, 255, 1000, ChatColor.GOLD, 1);

        // Oakly's Wood Depo - Higher priority to override hub_main
        importConfigArea("oaklys_depo", "Oakly's Wood Depo", "skyblock.oaklys-wood-depo", ChatColor.GREEN, 100, "hub_main");
        
        // Wheat Fields (Legacy: Hub Farm)
        importConfigArea("hub_wheat_area", "Wheat Fields", "skyblock.hub-crop-area", ChatColor.YELLOW, 100, "hub_main");
        
        // Farm Hub
        importConfigArea("farmhub_legacy", "Farm Hub", "skyblock.farmhub-crop-area", ChatColor.YELLOW, 90, "hub_main");

        // The Undead Cremetory (Hardcoded Legacy fallback)
        importHardcodedArea("undead_cremetory", "The Undead Cremetory", hubWorld, 
            101, 90, 257, 448, 53, 135, ChatColor.DARK_RED, 100, "hub_main");

        // Newbie Mines
        importHardcodedArea("newbie_mines", "Newbie Mines", hubWorld, 
            -86, 41, 193, 76, 78, 305, ChatColor.AQUA, 100, "hub_main");
    }

    private void importHardcodedArea(String id, String name, String worldName, double x1, double y1, double z1, double x2, double y2, double z2, ChatColor color, int priority) {
        importHardcodedArea(id, name, worldName, x1, y1, z1, x2, y2, z2, color, priority, null);
    }

    private void importHardcodedArea(String id, String name, String worldName, double x1, double y1, double z1, double x2, double y2, double z2, ChatColor color, int priority, String parentId) {
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) return;

        Zone zone = new Zone(id, name, worldName, 
            new Location(world, x1, y1, z1), 
            new Location(world, x2, y2, z2), 
            name, color, priority, true, "Hardcoded system zone.");
        zone.setParentId(parentId);
        
        addZone(zone);
    }

    private void importConfigArea(String id, String name, String path, ChatColor color, int priority) {
        importConfigArea(id, name, path, color, priority, null);
    }

    private void importConfigArea(String id, String name, String path, ChatColor color, int priority, String parentId) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(path);
        if (section == null || !section.getBoolean("enabled", false)) {
            return;
        }

        String worldName = section.getString("world");
        World world = worldName != null ? plugin.getServer().getWorld(worldName) : null;
        if (world == null) return;

        double x1 = section.getDouble("pos1.x");
        double y1 = section.getDouble("pos1.y");
        double z1 = section.getDouble("pos1.z");
        double x2 = section.getDouble("pos2.x");
        double y2 = section.getDouble("pos2.y");
        double z2 = section.getDouble("pos2.z");

        Zone zone = new Zone(id, name, worldName, 
            new Location(world, x1, y1, z1), 
            new Location(world, x2, y2, z2), 
            name, color, priority, true, "Imported from config.");
        zone.setParentId(parentId);
        
        addZone(zone);
    }

    private Zone resolveZoneAt(Location location) {
        List<Zone> worldZones = getZonesInWorld(location.getWorld().getName());
        for (Zone zone : worldZones) {
            if (zone.isEnabled() && zone.contains(location)) {
                return zone;
            }
        }
        return null;
    }

    private String getFormattedZoneName(Zone zone) {
        if (zone == null) {
            return defaultZoneName;
        }
        if (HUB_FARM_ZONE_ID.equalsIgnoreCase(zone.getId())) {
            ChatColor color = zone.getColor() == null ? ChatColor.WHITE : zone.getColor();
            return color + "Hub";
        }
        return zone.getColoredDisplayName();
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
        if (zone == null) {
            return;
        }

        if (displayName != null) {
            zone.setDisplayName(displayName);
        }
        if (color != null) {
            zone.setColor(color);
        }
        zone.setPriority(priority);
        rebuildWorldCache();
        saveZones();
    }

    /**
     * Set zone enabled state.
     */
    public void setZoneEnabled(String zoneId, boolean enabled) {
        Zone zone = getZone(zoneId);
        if (zone == null) {
            return;
        }

        zone.setEnabled(enabled);
        rebuildWorldCache();
        saveZones();
    }

    /**
     * Set zone description.
     */
    public void setZoneDescription(String zoneId, String description) {
        Zone zone = getZone(zoneId);
        if (zone == null) {
            return;
        }

        zone.setDescription(description);
        saveZones();
    }

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

    public void clearPlayerCache(Player player) {
        if (player == null) {
            return;
        }
        playerZoneCache.remove(player.getUniqueId());
    }

    public void clearPlayerCaches() {
        playerZoneCache.clear();
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

    private record PlayerZoneSnapshot(String worldName, int blockX, int blockY, int blockZ, Zone zone) {
        private static PlayerZoneSnapshot from(Location location, Zone zone) {
            return new PlayerZoneSnapshot(
                    location.getWorld().getName(),
                    location.getBlockX(),
                    location.getBlockY(),
                    location.getBlockZ(),
                    zone
            );
        }

        private boolean matches(Location location) {
            return worldName.equals(location.getWorld().getName())
                    && blockX == location.getBlockX()
                    && blockY == location.getBlockY()
                    && blockZ == location.getBlockZ();
        }
    }
}
