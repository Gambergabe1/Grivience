package io.papermc.Grivience.skyblock.island;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;

public final class WorldHelper {

    public static void fillBlocks(Location min, Location max, Material material) {
        if (min == null || max == null || min.getWorld() == null || max.getWorld() == null) {
            return;
        }

        if (!min.getWorld().equals(max.getWorld())) {
            return;
        }

        int minX = (int) Math.min(min.getX(), max.getX());
        int minY = (int) Math.min(min.getY(), max.getY());
        int minZ = (int) Math.min(min.getZ(), max.getZ());

        int maxX = (int) Math.max(min.getX(), max.getX());
        int maxY = (int) Math.max(min.getY(), max.getY());
        int maxZ = (int) Math.max(min.getZ(), max.getZ());

        World world = min.getWorld();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    block.setType(material);
                }
            }
        }
    }

    public static void clearArea(Location min, Location max) {
        if (min == null || max == null || min.getWorld() == null || max.getWorld() == null) {
            return;
        }

        if (!min.getWorld().equals(max.getWorld())) {
            return;
        }

        World world = min.getWorld();

        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (!block.getType().isAir() && block.getType() != Material.BEDROCK) {
                        block.setType(Material.AIR);
                    }
                }
            }
        }

        for (Entity entity : world.getEntities()) {
            Location entityLoc = entity.getLocation();
            if (entityLoc.getX() >= min.getX() && entityLoc.getX() <= max.getX() &&
                    entityLoc.getY() >= min.getY() && entityLoc.getY() <= max.getY() &&
                    entityLoc.getZ() >= min.getZ() && entityLoc.getZ() <= max.getZ()) {
                if (entity instanceof Item) {
                    entity.remove();
                }
            }
        }
    }

    public static void setBlock(Location location, Material material) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        location.getBlock().setType(material);
    }

    public static Material getBlockType(Location location) {
        if (location == null || location.getWorld() == null) {
            return Material.AIR;
        }
        return location.getBlock().getType();
    }

    public static boolean isSafeLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }

        Block ground = location.getBlock().getRelative(0, -1, 0);
        Block feet = location.getBlock();
        Block head = location.getBlock().getRelative(0, 1, 0);

        if (!ground.getType().isSolid()) {
            return false;
        }

        if (!feet.getType().isAir() && !feet.getType().toString().contains("WATER") && !feet.getType().toString().contains("LAVA")) {
            return false;
        }

        if (!head.getType().isAir() && !head.getType().toString().contains("WATER") && !head.getType().toString().contains("LAVA")) {
            return false;
        }

        return true;
    }

    public static Location findSafeLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }

        if (isSafeLocation(location)) {
            return location;
        }

        for (int y = 0; y < 10; y++) {
            Location up = location.clone().add(0, y, 0);
            if (isSafeLocation(up)) {
                return up;
            }

            Location down = location.clone().subtract(0, y, 0);
            if (isSafeLocation(down)) {
                return down;
            }
        }

        return location.clone().add(0, 1, 0);
    }
}
