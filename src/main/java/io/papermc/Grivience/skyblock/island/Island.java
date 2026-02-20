package io.papermc.Grivience.skyblock.island;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Island implements ConfigurationSerializable {
    private final UUID id;
    private final UUID owner;
    private Location center;
    private Location spawnPoint;
    private int level;
    private int size;
    private String name;
    private String description;
    private long createdAt;
    private long lastVisited;
    private Map<String, Long> visits;
    private int totalVisits;
    private Set<UUID> members;

    public Island(UUID owner, String ownerName, Location center) {
        this.id = UUID.randomUUID();
        this.owner = owner;
        this.center = center;
        this.spawnPoint = null;
        this.level = 1;
        this.size = 32;
        this.name = ownerName + "'s Island";
        this.description = "A humble island home.";
        this.createdAt = System.currentTimeMillis();
        this.lastVisited = System.currentTimeMillis();
        this.visits = new HashMap<>();
        this.totalVisits = 0;
        this.members = new HashSet<>();
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
        this.members = new HashSet<>();

        this.visits = new HashMap<>();
        Object visitsObj = data.get("visits");
        if (visitsObj instanceof ConfigurationSection) {
            ConfigurationSection visitsSection = (ConfigurationSection) visitsObj;
            for (String key : visitsSection.getKeys(false)) {
                visits.put(key, visitsSection.getLong(key));
            }
        }
        Object membersObj = data.get("members");
        if (membersObj instanceof List<?> list) {
            for (Object o : list) {
                try {
                    members.add(UUID.fromString(o.toString()));
                } catch (Exception ignored) {
                }
            }
        }

        String worldName = (String) data.get("world");
        double x = (double) data.get("x");
        double y = (double) data.get("y");
        double z = (double) data.get("z");
        World world = org.bukkit.Bukkit.getWorld(worldName);
        if (world != null) {
            this.center = new Location(world, x, y, z);
        }

        if (data.containsKey("spawnX") && data.containsKey("spawnY") && data.containsKey("spawnZ")) {
            double spawnX = (double) data.get("spawnX");
            double spawnY = (double) data.get("spawnY");
            double spawnZ = (double) data.get("spawnZ");
            float spawnYaw = (float) (double) data.getOrDefault("spawnYaw", 0.0f);
            float spawnPitch = (float) (double) data.getOrDefault("spawnPitch", 0.0f);
            if (world != null) {
                this.spawnPoint = new Location(world, spawnX, spawnY, spawnZ, spawnYaw, spawnPitch);
            }
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

        if (spawnPoint != null) {
            data.put("spawnX", spawnPoint.getX());
            data.put("spawnY", spawnPoint.getY());
            data.put("spawnZ", spawnPoint.getZ());
            data.put("spawnYaw", spawnPoint.getYaw());
            data.put("spawnPitch", spawnPoint.getPitch());
        }
        if (members != null && !members.isEmpty()) {
            List<String> memberList = new java.util.ArrayList<>();
            for (UUID uuid : members) {
                memberList.add(uuid.toString());
            }
            data.put("members", memberList);
        }

        return data;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwner() {
        return owner;
    }

    public Location getCenter() {
        return center;
    }

    public Location getSpawnPoint() {
        if (spawnPoint != null) {
            return spawnPoint.clone().add(0.5, 1, 0.5);
        }
        if (center != null) {
            return center.clone().add(0.5, 1, 0.5);
        }
        return null;
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

    public void setCenter(Location center) {
        this.center = center;
    }

    public void setSpawnPoint(Location spawnPoint) {
        this.spawnPoint = spawnPoint.clone();
        this.spawnPoint.setYaw(0);
        this.spawnPoint.setPitch(0);
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public Set<UUID> getMembers() {
        return Set.copyOf(members);
    }

    public void addMember(UUID uuid) {
        if (uuid != null) {
            members.add(uuid);
        }
    }

    public void removeMember(UUID uuid) {
        if (uuid != null) {
            members.remove(uuid);
        }
    }

    public boolean isMember(UUID uuid) {
        return uuid != null && members.contains(uuid);
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
        return (size - 32) / 16 + 1;
    }

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
