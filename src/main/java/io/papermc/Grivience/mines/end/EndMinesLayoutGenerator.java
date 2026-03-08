package io.papermc.Grivience.mines.end;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

/**
 * Batched generator for the End Mines layout.
 * Runs over multiple ticks to avoid freezing the main thread.
 */
public final class EndMinesLayoutGenerator {
    private final GriviencePlugin plugin;

    public EndMinesLayoutGenerator(GriviencePlugin plugin) {
        this.plugin = plugin;
    }

    public BukkitRunnable generate(World world, Location anchor, Runnable onComplete) {
        int blocksPerTick = Math.max(500, plugin.getConfig().getInt("end-mines.generation.blocks-per-tick", 2500));
        int radius = Math.max(24, plugin.getConfig().getInt("end-mines.generation.radius", 48));
        int minY = plugin.getConfig().getInt("end-mines.generation.min-y", 60);
        int maxY = plugin.getConfig().getInt("end-mines.generation.max-y", 78);
        long seed = plugin.getConfig().getLong("end-mines.generation.seed", 0L);

        int centerX = anchor.getBlockX();
        int centerZ = anchor.getBlockZ();

        int minX = centerX - radius;
        int maxX = centerX + radius;
        int minZ = centerZ - radius;
        int maxZ = centerZ + radius;

        long effectiveSeed = seed != 0L ? seed : world.getSeed() ^ ((long) centerX << 32) ^ (long) centerZ;
        Random random = new Random(effectiveSeed);

        BukkitRunnable task = new BukkitRunnable() {
            private int stage = 0;

            // Stage 0 cursor (fill)
            private int x = minX;
            private int y = minY;
            private int z = minZ;

            // Stage 1 cursor (carve)
            private int carveIndex = 0;

            @Override
            public void run() {
                if (world == null) {
                    cancel();
                    return;
                }

                if (stage == 0) {
                    int placed = 0;
                    while (placed < blocksPerTick) {
                        // Fill bounding volume with bedrock shell + end stone interior.
                        Material material = materialForFill(x, y, z, minX, maxX, minY, maxY, minZ, maxZ);
                        setFast(world, x, y, z, material);
                        placed++;

                        // Advance 3D cursor
                        x++;
                        if (x > maxX) {
                            x = minX;
                            z++;
                            if (z > maxZ) {
                                z = minZ;
                                y++;
                                if (y > maxY) {
                                    stage = 1;
                                    carveIndex = 0;
                                    return;
                                }
                            }
                        }
                    }
                    return;
                }

                if (stage == 1) {
                    // One carve operation per tick to keep TPS stable.
                    if (!carveNext(world, anchor, radius, minY, maxY, random)) {
                        stage = 2;
                    }
                    return;
                }

                if (stage == 2) {
                    buildSurfaceHub(world, anchor);
                    stage = 3;
                    return;
                }

                if (stage == 3) {
                    cancel();
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            }

            private boolean carveNext(World world, Location anchor, int radius, int minY, int maxY, Random random) {
                // Carve operations are applied sequentially to keep generation deterministic.
                int centerX = anchor.getBlockX();
                int centerY = anchor.getBlockY();
                int centerZ = anchor.getBlockZ();

                int mineTop = Math.min(maxY, centerY - 2);
                int mineFloor = Math.max(minY, mineTop - 14);

                // Main chamber
                if (carveIndex == 0) {
                    // Keep the carve below the spawn platform (y = anchorY - 1).
                    carveCylinder(world, centerX, mineTop - 3, centerZ, 11, 7);
                    carveIndex++;
                    return true;
                }

                // Entry shaft
                if (carveIndex == 1) {
                    carveCuboid(world,
                            centerX - 2, centerX + 2,
                            mineFloor + 1, mineTop,
                            centerZ - 2, centerZ + 2,
                            Material.AIR);
                    carveIndex++;
                    return true;
                }

                // Four tunnels + zone rooms
                if (carveIndex == 2) {
                    carveTunnel(world, centerX, mineFloor + 2, centerZ, 0, -1, 34);
                    carveZoneRoom(world, centerX, mineFloor + 2, centerZ - 34, "chorus");
                    carveIndex++;
                    return true;
                }
                if (carveIndex == 3) {
                    carveTunnel(world, centerX, mineFloor + 2, centerZ, 0, 1, 34);
                    carveZoneRoom(world, centerX, mineFloor + 2, centerZ + 34, "obsidian");
                    carveIndex++;
                    return true;
                }
                if (carveIndex == 4) {
                    carveTunnel(world, centerX, mineFloor + 2, centerZ, 1, 0, 34);
                    carveZoneRoom(world, centerX + 34, mineFloor + 2, centerZ, "crystal");
                    carveIndex++;
                    return true;
                }
                if (carveIndex == 5) {
                    carveTunnel(world, centerX, mineFloor + 2, centerZ, -1, 0, 34);
                    carveZoneRoom(world, centerX - 34, mineFloor + 2, centerZ, "rift");
                    carveIndex++;
                    return true;
                }

                // Populate veins (simple deterministic patches)
                if (carveIndex == 6) {
                    placeVeins(world, centerX, mineFloor + 2, centerZ, random);
                    carveIndex++;
                    return true;
                }

                return false;
            }

            private void placeVeins(World world, int centerX, int baseY, int centerZ, Random random) {
                // Obsidian vein (south room)
                placePatch(world, centerX, baseY, centerZ + 34, 8, Material.OBSIDIAN, random);
                placePatch(world, centerX, baseY, centerZ + 34, 6, Material.CRYING_OBSIDIAN, random);

                // Crystal vein (east room)
                placePatch(world, centerX + 34, baseY, centerZ, 7, Material.AMETHYST_BLOCK, random);
                placePatch(world, centerX + 34, baseY, centerZ, 5, Material.BUDDING_AMETHYST, random);

                // Rift fragments (west room)
                placePatch(world, centerX - 34, baseY, centerZ, 7, Material.PURPUR_BLOCK, random);
                placePatch(world, centerX - 34, baseY, centerZ, 5, Material.END_STONE_BRICKS, random);

                // Chorus grove (north room)
                for (int i = 0; i < 10; i++) {
                    int dx = random.nextInt(11) - 5;
                    int dz = random.nextInt(11) - 5;
                    int x = centerX + dx;
                    int z = centerZ - 34 + dz;
                    Block ground = world.getBlockAt(x, baseY - 1, z);
                    if (ground.getType() == Material.END_STONE || ground.getType() == Material.END_STONE_BRICKS) {
                        Block plant = ground.getRelative(0, 1, 0);
                        plant.setType(Material.CHORUS_PLANT, false);
                    }
                }
            }

            private void placePatch(World world, int originX, int originY, int originZ, int radius, Material material, Random random) {
                for (int i = 0; i < 120; i++) {
                    int x = originX + random.nextInt(radius * 2 + 1) - radius;
                    int y = originY + random.nextInt(7) - 3;
                    int z = originZ + random.nextInt(radius * 2 + 1) - radius;
                    if (y < minY + 1 || y > maxY - 1) {
                        continue;
                    }
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == Material.END_STONE) {
                        block.setType(material, false);
                    }
                }
            }
        };

        task.runTaskTimer(plugin, 1L, 1L);
        return task;
    }

    private static Material materialForFill(int x, int y, int z, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        boolean boundary = x == minX || x == maxX || z == minZ || z == maxZ || y == minY || y == maxY;
        if (boundary) {
            return Material.BEDROCK;
        }
        return Material.END_STONE;
    }

    private static void setFast(World world, int x, int y, int z, Material material) {
        world.getBlockAt(x, y, z).setType(material, false);
    }

    private static void carveCuboid(World world, int minX, int maxX, int minY, int maxY, int minZ, int maxZ, Material material) {
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    world.getBlockAt(x, y, z).setType(material, false);
                }
            }
        }
    }

    private static void carveCylinder(World world, int centerX, int centerY, int centerZ, int radius, int height) {
        int minY = centerY - (height / 2);
        int maxY = centerY + (height / 2);
        int r2 = radius * radius;
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            int dx = x - centerX;
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                int dz = z - centerZ;
                if (dx * dx + dz * dz > r2) {
                    continue;
                }
                for (int y = minY; y <= maxY; y++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
                }
            }
        }
    }

    private static void carveTunnel(World world, int startX, int y, int startZ, int stepX, int stepZ, int length) {
        int x = startX;
        int z = startZ;
        for (int i = 0; i < length; i++) {
            carveCuboid(world, x - 2, x + 2, y - 1, y + 3, z - 2, z + 2, Material.AIR);
            // Walkway
            carveCuboid(world, x - 2, x + 2, y - 2, y - 2, z - 2, z + 2, Material.END_STONE_BRICKS);
            x += stepX;
            z += stepZ;
        }
    }

    private static void carveZoneRoom(World world, int centerX, int y, int centerZ, String zone) {
        carveCuboid(world, centerX - 8, centerX + 8, y - 2, y + 6, centerZ - 8, centerZ + 8, Material.AIR);
        carveCuboid(world, centerX - 8, centerX + 8, y - 3, y - 3, centerZ - 8, centerZ + 8, Material.END_STONE_BRICKS);

        // Simple theming
        Material pillar = switch (zone) {
            case "obsidian" -> Material.OBSIDIAN;
            case "crystal" -> Material.AMETHYST_BLOCK;
            case "rift" -> Material.PURPUR_PILLAR;
            default -> Material.END_STONE_BRICKS;
        };
        for (int dy = 0; dy < 5; dy++) {
            world.getBlockAt(centerX - 8, y - 2 + dy, centerZ - 8).setType(pillar, false);
            world.getBlockAt(centerX + 8, y - 2 + dy, centerZ - 8).setType(pillar, false);
            world.getBlockAt(centerX - 8, y - 2 + dy, centerZ + 8).setType(pillar, false);
            world.getBlockAt(centerX + 8, y - 2 + dy, centerZ + 8).setType(pillar, false);
        }

        // Lighting
        world.getBlockAt(centerX, y + 2, centerZ).setType(Material.END_ROD, false);
        world.getBlockAt(centerX + 3, y + 2, centerZ + 3).setType(Material.END_ROD, false);
        world.getBlockAt(centerX - 3, y + 2, centerZ - 3).setType(Material.END_ROD, false);
    }

    private static void buildSurfaceHub(World world, Location anchor) {
        int x0 = anchor.getBlockX();
        int y0 = anchor.getBlockY() - 1;
        int z0 = anchor.getBlockZ();

        // Large hub platform around spawn.
        int radius = 10;
        int r2 = radius * radius;
        for (int x = x0 - radius; x <= x0 + radius; x++) {
            int dx = x - x0;
            for (int z = z0 - radius; z <= z0 + radius; z++) {
                int dz = z - z0;
                if (dx * dx + dz * dz > r2) {
                    continue;
                }
                Material floor = (dx * dx + dz * dz >= (radius - 1) * (radius - 1)) ? Material.OBSIDIAN : Material.PURPUR_BLOCK;
                world.getBlockAt(x, y0, z).setType(floor, false);
                world.getBlockAt(x, y0 + 1, z).setType(Material.AIR, false);
                world.getBlockAt(x, y0 + 2, z).setType(Material.AIR, false);
            }
        }

        // Simple archway
        for (int y = 0; y <= 4; y++) {
            world.getBlockAt(x0 - 4, y0 + 1 + y, z0).setType(Material.OBSIDIAN, false);
            world.getBlockAt(x0 + 4, y0 + 1 + y, z0).setType(Material.OBSIDIAN, false);
        }
        for (int x = x0 - 4; x <= x0 + 4; x++) {
            world.getBlockAt(x, y0 + 6, z0).setType(Material.OBSIDIAN, false);
        }

        world.getBlockAt(x0, y0 + 1, z0 - 2).setType(Material.END_ROD, false);
        world.getBlockAt(x0, y0 + 1, z0 + 2).setType(Material.END_ROD, false);
    }
}
