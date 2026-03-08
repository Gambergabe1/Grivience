package io.papermc.Grivience.mines.end.mob;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.UUID;

/**
 * Persistent spawn point for End Mines mobs. Managed via /endmine mobs ... commands.
 */
public final class EndMinesMobSpawnPoint {
    private final UUID id;
    private String mobTypeId;
    private String worldName;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private int spawnRadius;
    private int spawnDelayTicks;
    private int maxNearbyEntities;
    private boolean active;

    public EndMinesMobSpawnPoint(UUID id) {
        this.id = id;
        this.mobTypeId = EndMinesMobType.RIFTWALKER.id();
        this.worldName = "world";
        this.spawnRadius = 6;
        this.spawnDelayTicks = 100;
        this.maxNearbyEntities = 6;
        this.active = true;
    }

    public UUID id() {
        return id;
    }

    public String mobTypeId() {
        return mobTypeId;
    }

    public String worldName() {
        return worldName;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public void setMobTypeId(String mobTypeId) {
        this.mobTypeId = mobTypeId;
    }

    public int spawnRadius() {
        return spawnRadius;
    }

    public void setSpawnRadius(int spawnRadius) {
        this.spawnRadius = Math.max(1, spawnRadius);
    }

    public int spawnDelayTicks() {
        return spawnDelayTicks;
    }

    public void setSpawnDelayTicks(int spawnDelayTicks) {
        this.spawnDelayTicks = Math.max(1, spawnDelayTicks);
    }

    public int maxNearbyEntities() {
        return maxNearbyEntities;
    }

    public void setMaxNearbyEntities(int maxNearbyEntities) {
        this.maxNearbyEntities = Math.max(0, maxNearbyEntities);
    }

    public boolean active() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        this.worldName = location.getWorld().getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
    }

    public Location toLocation() {
        if (worldName == null || worldName.isBlank()) {
            return null;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z, yaw, pitch);
    }

    public void save(ConfigurationSection section) {
        if (section == null) {
            return;
        }
        section.set("mob-type", mobTypeId);
        section.set("world", worldName);
        section.set("x", x);
        section.set("y", y);
        section.set("z", z);
        section.set("yaw", yaw);
        section.set("pitch", pitch);
        section.set("spawn-radius", spawnRadius);
        section.set("spawn-delay-ticks", spawnDelayTicks);
        section.set("max-nearby-entities", maxNearbyEntities);
        section.set("active", active);
    }

    public static EndMinesMobSpawnPoint load(UUID id, ConfigurationSection section) {
        if (id == null || section == null) {
            return null;
        }
        EndMinesMobSpawnPoint point = new EndMinesMobSpawnPoint(id);
        point.mobTypeId = section.getString("mob-type", point.mobTypeId);
        point.worldName = section.getString("world", point.worldName);
        point.x = section.getDouble("x", point.x);
        point.y = section.getDouble("y", point.y);
        point.z = section.getDouble("z", point.z);
        point.yaw = (float) section.getDouble("yaw", point.yaw);
        point.pitch = (float) section.getDouble("pitch", point.pitch);
        point.spawnRadius = Math.max(1, section.getInt("spawn-radius", point.spawnRadius));
        point.spawnDelayTicks = Math.max(1, section.getInt("spawn-delay-ticks", point.spawnDelayTicks));
        point.maxNearbyEntities = Math.max(0, section.getInt("max-nearby-entities", point.maxNearbyEntities));
        point.active = section.getBoolean("active", point.active);
        return point;
    }
}
