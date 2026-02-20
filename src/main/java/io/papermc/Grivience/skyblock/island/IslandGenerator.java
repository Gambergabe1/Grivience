package io.papermc.Grivience.skyblock.island;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.Bukkit;

public final class IslandGenerator {

    public static void generateIsland(Island island) {
        if (island.getCenter() == null) return;

        Location center = island.getCenter();
        int size = island.getSize();
        int halfSize = size / 2;

        if (generateFromSchematic(center)) {
            return;
        }

        generateBasePlatform(center, halfSize);
        generateSpawnArea(center);
        generateChests(center);
        generateTree(center);
        generateWaterSource(center);
    }

    private static boolean generateFromSchematic(Location center) {
        if (Bukkit.getPluginManager().getPlugin("WorldEdit") == null) {
            return false;
        }
        try (var stream = IslandGenerator.class.getClassLoader()
                .getResourceAsStream("schematics/skyblock_island.schematic")) {
            if (stream == null) {
                return false;
            }
            return SchematicPaster.pasteSchematic(center.getWorld(), center.clone().subtract(0.5, 0, 0.5), stream);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void generateBasePlatform(Location center, int halfSize) {
        WorldHelper.fillBlocks(
                center.clone().subtract(halfSize, 0, halfSize),
                center.clone().add(halfSize, 0, halfSize),
                Material.GRASS_BLOCK
        );

        for (int x = -halfSize; x <= halfSize; x++) {
            for (int z = -halfSize; z <= halfSize; z++) {
                if (Math.abs(x) == halfSize || Math.abs(z) == halfSize) {
                    Block block = center.clone().add(x, 0, z).getBlock();
                    if (block.getType() == Material.GRASS_BLOCK) {
                        block.setType(Material.DIRT);
                    }
                }
            }
        }

        WorldHelper.fillBlocks(
                center.clone().subtract(halfSize + 1, -1, halfSize + 1),
                center.clone().add(halfSize + 1, -1, halfSize + 1),
                Material.STONE
        );
    }

    private static void generateSpawnArea(Location center) {
        Location spawnPlatform = center.clone();

        WorldHelper.fillBlocks(
                spawnPlatform.clone().subtract(2, 0, 2),
                spawnPlatform.clone().add(2, 0, 2),
                Material.SMOOTH_STONE
        );

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                    Block block = spawnPlatform.clone().add(x, 0, z).getBlock();
                    block.setType(Material.STONE_BRICK_SLAB);
                }
            }
        }

        for (int y = 1; y <= 4; y++) {
            Block corner1 = spawnPlatform.clone().add(-2, y, -2).getBlock();
            Block corner2 = spawnPlatform.clone().add(2, y, -2).getBlock();
            Block corner3 = spawnPlatform.clone().add(-2, y, 2).getBlock();
            Block corner4 = spawnPlatform.clone().add(2, y, 2).getBlock();

            if (y == 4) {
                corner1.setType(Material.SEA_LANTERN);
                corner2.setType(Material.SEA_LANTERN);
                corner3.setType(Material.SEA_LANTERN);
                corner4.setType(Material.SEA_LANTERN);
            } else {
                corner1.setType(Material.STONE_BRICKS);
                corner2.setType(Material.STONE_BRICKS);
                corner3.setType(Material.STONE_BRICKS);
                corner4.setType(Material.STONE_BRICKS);
            }
        }

        Location signLoc = spawnPlatform.clone().add(0, 1, -3);
        Block signBlock = signLoc.getBlock();
        signBlock.setType(Material.OAK_SIGN);
    }

    private static void generateChests(Location center) {
        Location chestArea = center.clone().add(5, 0, 0);

        Block chestBlock1 = chestArea.clone().subtract(1, 0, 0).getBlock();
        chestBlock1.setType(Material.CHEST);

        Block chestBlock2 = chestArea.clone().add(1, 0, 0).getBlock();
        chestBlock2.setType(Material.CHEST);

        WorldHelper.fillBlocks(
                chestArea.clone().subtract(2, 0, 1),
                chestArea.clone().add(2, 0, 1),
                Material.SMOOTH_STONE
        );

        Block torch1 = chestArea.clone().subtract(2, 1, 0).getBlock();
        torch1.setType(Material.TORCH);

        Block torch2 = chestArea.clone().add(2, 1, 0).getBlock();
        torch2.setType(Material.TORCH);

        for (int y = 1; y <= 2; y++) {
            Block pillar1 = chestArea.clone().subtract(2, y, 1).getBlock();
            Block pillar2 = chestArea.clone().add(2, y, 1).getBlock();
            pillar1.setType(Material.STONE_BRICKS);
            pillar2.setType(Material.STONE_BRICKS);
        }

        Block lamp1 = chestArea.clone().subtract(2, 3, 1).getBlock();
        Block lamp2 = chestArea.clone().add(2, 3, 1).getBlock();
        lamp1.setType(Material.SEA_LANTERN);
        lamp2.setType(Material.SEA_LANTERN);
    }

    private static void generateTree(Location center) {
        Location treeLocation = center.clone().add(-6, 0, 4);

        for (int y = 0; y < 5; y++) {
            treeLocation.clone().add(0, y, 0).getBlock().setType(Material.OAK_LOG);
        }

        Location leavesCenter = treeLocation.clone().add(0, 5, 0);
        for (int x = -2; x <= 2; x++) {
            for (int y = -1; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    if (Math.abs(x) + Math.abs(y) + Math.abs(z) <= 3) {
                        Block leafBlock = leavesCenter.clone().add(x, y, z).getBlock();
                        if (leafBlock.getType() == Material.AIR) {
                            leafBlock.setType(Material.OAK_LEAVES);
                        }
                    }
                }
            }
        }

        Block dirtBlock = treeLocation.clone().subtract(0, 1, 0).getBlock();
        dirtBlock.setType(Material.GRASS_BLOCK);
    }

    private static void generateWaterSource(Location center) {
        Location farmArea = center.clone().add(-6, 0, -4);

        farmArea.getBlock().setType(Material.WATER_CAULDRON);

        Block dirt1 = farmArea.clone().add(1, 0, 0).getBlock();
        Block dirt2 = farmArea.clone().add(-1, 0, 0).getBlock();
        Block dirt3 = farmArea.clone().add(0, 0, 1).getBlock();
        Block dirt4 = farmArea.clone().add(0, 0, -1).getBlock();

        dirt1.setType(Material.GRASS_BLOCK);
        dirt2.setType(Material.GRASS_BLOCK);
        dirt3.setType(Material.GRASS_BLOCK);
        dirt4.setType(Material.GRASS_BLOCK);
    }

    public static void regenerateIsland(Island island) {
        if (island.getCenter() == null) return;

        Location center = island.getCenter();
        int halfSize = island.getSize() / 2;

        Location min = center.clone().subtract(halfSize + 2, 0, halfSize + 2);
        Location max = center.clone().add(halfSize + 2, 256, halfSize + 2);

        WorldHelper.clearArea(min, max);

        generateIsland(island);
    }
}
