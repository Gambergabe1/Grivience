package io.papermc.Grivience.mob;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SpawnPoint implements ConfigurationSerializable {
    public static final int FIXED_MAX_NEARBY_ENTITIES = 10;
    public static final int REFILL_TRIGGER_NEARBY_ENTITIES = 2;

    private final UUID id;
    private Location location;
    private String worldName;
    private String monsterId;
    private int spawnRadius;
    private int spawnDelay;
    private int maxNearbyEntities;
    private boolean active;

    public SpawnPoint(Location location, String monsterId) {
        this.id = UUID.randomUUID();
        this.location = location == null ? null : location.clone();
        this.worldName = resolveWorldName(location);
        this.monsterId = monsterId;
        this.spawnRadius = 4;
        this.spawnDelay = 100;
        this.maxNearbyEntities = FIXED_MAX_NEARBY_ENTITIES;
        this.active = true;
    }

    public SpawnPoint(Map<String, Object> data) {
        this.id = parseUuid(data.get("id"));
        this.monsterId = stringValue(data.get("monsterId"), "zombie");
        this.spawnRadius = intValue(data.get("spawnRadius"), 4);
        this.spawnDelay = intValue(data.get("spawnDelay"), 100);
        this.maxNearbyEntities = FIXED_MAX_NEARBY_ENTITIES;
        this.active = booleanValue(data.get("active"), true);
        this.worldName = stringValue(data.get("world"), null);

        double x = doubleValue(data.get("x"), 0.0D);
        double y = doubleValue(data.get("y"), 64.0D);
        double z = doubleValue(data.get("z"), 0.0D);
        float yaw = (float) doubleValue(data.get("yaw"), 0.0D);
        float pitch = (float) doubleValue(data.get("pitch"), 0.0D);
        World world = worldName == null || Bukkit.getServer() == null ? null : Bukkit.getWorld(worldName);
        this.location = new Location(world, x, y, z, yaw, pitch);
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
            String storedWorldName = resolveWorldName(location);
            if (storedWorldName != null) {
                data.put("world", storedWorldName);
            }
            data.put("x", location.getX());
            data.put("y", location.getY());
            data.put("z", location.getZ());
            data.put("yaw", location.getYaw());
            data.put("pitch", location.getPitch());
        } else if (worldName != null) {
            data.put("world", worldName);
        }

        return data;
    }

    public UUID getId() {
        return id;
    }

    public Location getLocation() {
        if (location != null && location.getWorld() == null && worldName != null && Bukkit.getServer() != null) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                location.setWorld(world);
            }
        }
        return location;
    }

    public void setLocation(Location location) {
        this.location = location == null ? null : location.clone();
        this.worldName = resolveWorldName(location);
    }

    public String getWorldName() {
        return worldName;
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
        this.maxNearbyEntities = FIXED_MAX_NEARBY_ENTITIES;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isLocationValid() {
        Location resolved = getLocation();
        return resolved != null && resolved.getWorld() != null;
    }

    public boolean shouldRefill(int activeEntities) {
        int normalizedActiveEntities = Math.max(0, activeEntities);
        return normalizedActiveEntities <= REFILL_TRIGGER_NEARBY_ENTITIES
                && normalizedActiveEntities < maxNearbyEntities;
    }

    private static UUID parseUuid(Object value) {
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            try {
                return UUID.fromString(stringValue);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return UUID.randomUUID();
    }

    private static String stringValue(Object value, String fallback) {
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return stringValue;
        }
        return fallback;
    }

    private static int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Integer.parseInt(stringValue);
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private static double doubleValue(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Double.parseDouble(stringValue);
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private static boolean booleanValue(Object value, boolean fallback) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof String stringValue) {
            return Boolean.parseBoolean(stringValue);
        }
        return fallback;
    }

    private String resolveWorldName(Location location) {
        if (location == null) {
            return worldName;
        }
        World world = location.getWorld();
        return world == null ? worldName : world.getName();
    }
}
