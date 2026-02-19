package io.papermc.Grivience.mob;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SpawnPoint implements ConfigurationSerializable {
    private final UUID id;
    private Location location;
    private String monsterId;
    private int spawnRadius;
    private int spawnDelay;
    private int maxNearbyEntities;
    private boolean active;

    public SpawnPoint(Location location, String monsterId) {
        this.id = UUID.randomUUID();
        this.location = location;
        this.monsterId = monsterId;
        this.spawnRadius = 4;
        this.spawnDelay = 100;
        this.maxNearbyEntities = 6;
        this.active = true;
    }

    public SpawnPoint(Map<String, Object> data) {
        this.id = UUID.fromString((String) data.get("id"));
        this.monsterId = (String) data.getOrDefault("monsterId", "zombie");
        this.spawnRadius = (int) data.getOrDefault("spawnRadius", 4);
        this.spawnDelay = (int) data.getOrDefault("spawnDelay", 100);
        this.maxNearbyEntities = (int) data.getOrDefault("maxNearbyEntities", 6);
        this.active = (boolean) data.getOrDefault("active", true);

        // Deserialize location
        String worldName = (String) data.get("world");
        double x = (double) data.get("x");
        double y = (double) data.get("y");
        double z = (double) data.get("z");
        float yaw = (float) data.getOrDefault("yaw", 0);
        float pitch = (float) data.getOrDefault("pitch", 0);
        org.bukkit.World world = org.bukkit.Bukkit.getWorld(worldName);
        if (world != null) {
            this.location = new Location(world, x, y, z, yaw, pitch);
        }
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("id", id.toString());
        data.put("monsterId", monsterId);
        data.put("spawnRadius", spawnRadius);
        data.put("spawnDelay", spawnDelay);
        data.put("maxNearbyEntities", maxNearbyEntities);
        data.put("active", active);

        if (location != null) {
            data.put("world", location.getWorld().getName());
            data.put("x", location.getX());
            data.put("y", location.getY());
            data.put("z", location.getZ());
            data.put("yaw", location.getYaw());
            data.put("pitch", location.getPitch());
        }

        return data;
    }

    public UUID getId() {
        return id;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getMonsterId() {
        return monsterId;
    }

    public void setMonsterId(String monsterId) {
        this.monsterId = monsterId;
    }

    public int getSpawnRadius() {
        return spawnRadius;
    }

    public void setSpawnRadius(int spawnRadius) {
        this.spawnRadius = spawnRadius;
    }

    public int getSpawnDelay() {
        return spawnDelay;
    }

    public void setSpawnDelay(int spawnDelay) {
        this.spawnDelay = spawnDelay;
    }

    public int getMaxNearbyEntities() {
        return maxNearbyEntities;
    }

    public void setMaxNearbyEntities(int maxNearbyEntities) {
        this.maxNearbyEntities = maxNearbyEntities;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isLocationValid() {
        return location != null && location.getWorld() != null;
    }

    public int countNearbyEntities() {
        if (!isLocationValid()) {
            return 0;
        }
        return location.getWorld().getNearbyEntities(location, spawnRadius, 4, spawnRadius).size();
    }
}
