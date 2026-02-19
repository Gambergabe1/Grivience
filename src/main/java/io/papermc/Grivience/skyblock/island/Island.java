package io.papermc.Grivience.skyblock.island;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Island implements ConfigurationSerializable {
    private final UUID id;
    private final UUID owner;
    private Location center;
    private int level;
    private int size;
    private String name;
    private String description;
    private long createdAt;
    private long lastVisited;
    private Map<String, Long> visits;
    private int totalVisits;

    public Island(UUID owner, String ownerName, Location center) {
        this.id = UUID.randomUUID();
        this.owner = owner;
        this.center = center;
        this.level = 1;
        this.size = 32; // Starting size (32x32)
        this.name = ownerName + "'s Island";
        this.description = "A humble island home.";
        this.createdAt = System.currentTimeMillis();
        this.lastVisited = System.currentTimeMillis();
        this.visits = new HashMap<>();
        this.totalVisits = 0;
    }

    public Island(Map<String, Object> data) {
        this.id = UUID.fromString((String) data.get("id"));
        this.owner = UUID.fromString((String) data.get("owner"));
        this.level = (int) data.getOrDefault("level", 1);
        this.size = (int) data.getOrDefault("size", 32);
        this.name = (String) data.getOrDefault("name", "Unknown Island");
        this.description = (String) data.getOrDefault("description", "");
        this.createdAt = (long) data.getOrDefault("createdAt", 0L);
        this.lastVisited = (long) data.getOrDefault("lastVisited", 0L);
        this.totalVisits = (int) data.getOrDefault("totalVisits", 0);

        // Deserialize visits
        this.visits = new HashMap<>();
        Object visitsObj = data.get("visits");
        if (visitsObj instanceof ConfigurationSection) {
            ConfigurationSection visitsSection = (ConfigurationSection) visitsObj;
            for (String key : visitsSection.getKeys(false)) {
                visits.put(key, visitsSection.getLong(key));
            }
        }

        // Deserialize location
        String worldName = (String) data.get("world");
        double x = (double) data.get("x");
        double y = (double) data.get("y");
        double z = (double) data.get("z");
        World world = org.bukkit.Bukkit.getWorld(worldName);
        if (world != null) {
            this.center = new Location(world, x, y, z);
        }
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("id", id.toString());
        data.put("owner", owner.toString());
        data.put("level", level);
        data.put("size", size);
        data.put("name", name);
        data.put("description", description);
        data.put("createdAt", createdAt);
        data.put("lastVisited", lastVisited);
        data.put("totalVisits", totalVisits);

        // Serialize visits
        if (!visits.isEmpty()) {
            Map<String, Object> visitsMap = new HashMap<>();
            for (Map.Entry<String, Long> entry : visits.entrySet()) {
                visitsMap.put(entry.getKey(), entry.getValue());
            }
            data.put("visits", visitsMap);
        }

        if (center != null) {
            data.put("world", center.getWorld().getName());
            data.put("x", center.getX());
            data.put("y", center.getY());
            data.put("z", center.getZ());
        }

        return data;
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public UUID getOwner() {
        return owner;
    }

    public Location getCenter() {
        return center;
    }

    public int getLevel() {
        return level;
    }

    public int getSize() {
        return size;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getLastVisited() {
        return lastVisited;
    }

    // Setters
    public void setCenter(Location center) {
        this.center = center;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLastVisited(long lastVisited) {
        this.lastVisited = lastVisited;
    }

    // Utility methods
    public Location getMinCorner() {
        if (center == null) return null;
        int halfSize = size / 2;
        return center.clone().subtract(halfSize, 0, halfSize);
    }

    public Location getMaxCorner() {
        if (center == null) return null;
        int halfSize = size / 2;
        return center.clone().add(halfSize, 256, halfSize);
    }

    public boolean isWithinIsland(Location location) {
        if (center == null || location == null) return false;
        if (!center.getWorld().equals(location.getWorld())) return false;

        int halfSize = size / 2;
        double dx = Math.abs(location.getX() - center.getX());
        double dz = Math.abs(location.getZ() - center.getZ());

        return dx <= halfSize && dz <= halfSize;
    }

    public int getUpgradeLevel() {
        // Calculate upgrade level based on size
        // 32 = level 1, 48 = level 2, 64 = level 3, etc.
        return (size - 32) / 16 + 1;
    }

    // Visit tracking methods
    public void addVisit(String playerName) {
        visits.put(playerName.toLowerCase(), System.currentTimeMillis());
        totalVisits++;
    }

    public long getVisitCount(String playerName) {
        return visits.getOrDefault(playerName.toLowerCase(), 0L);
    }

    public int getTotalVisits() {
        return totalVisits;
    }

    public Map<String, Long> getVisits() {
        return new HashMap<>(visits);
    }

    public List<String> getRecentVisitors(int limit) {
        return visits.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }
}
