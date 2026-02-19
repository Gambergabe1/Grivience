package io.papermc.Grivience.skyblock.island;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

public final class IslandGenerator {

    public static void generateIsland(Island island) {
        if (island.getCenter() == null) return;

        Location center = island.getCenter();
        int size = island.getSize();
        int halfSize = size / 2;

        generateBasePlatform(center, halfSize);
        generateSpawnArea(center);
        generateChests(center);
        generateTree(center);
    }

    private static void generateBasePlatform(Location center, int halfSize) {
        WorldHelper.fillBlocks(
                center.clone().subtract(halfSize, 1, halfSize),
                center.clone().add(halfSize, 1, halfSize),
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
                center.clone().subtract(halfSize + 1, 0, halfSize + 1),
                center.clone().add(halfSize + 1, 0, halfSize + 1),
                Material.STONE
        );
    }

    private static void generateSpawnArea(Location center) {
        Location spawnPlatform = center.clone().add(0, 2, 0);

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

        for (int y = 1; y <= 3; y++) {
            Block corner1 = spawnPlatform.clone().add(-2, y, -2).getBlock();
            Block corner2 = spawnPlatform.clone().add(2, y, -2).getBlock();
            Block corner3 = spawnPlatform.clone().add(-2, y, 2).getBlock();
            Block corner4 = spawnPlatform.clone().add(2, y, 2).getBlock();

            if (y == 3) {
                corner1.setType(Material.JACK_O_LANTERN);
                corner2.setType(Material.JACK_O_LANTERN);
                corner3.setType(Material.JACK_O_LANTERN);
                corner4.setType(Material.JACK_O_LANTERN);
            } else {
                corner1.setType(Material.COBBLESTONE_WALL);
                corner2.setType(Material.COBBLESTONE_WALL);
                corner3.setType(Material.COBBLESTONE_WALL);
                corner4.setType(Material.COBBLESTONE_WALL);
            }
        }

        Location signLoc = spawnPlatform.clone().add(0, 1, -2);
        Block signBlock = signLoc.getBlock();
        signBlock.setType(Material.OAK_SIGN);
    }

    private static void generateChests(Location center) {
        Location chestArea = center.clone().add(3, 2, 0);

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
    }

    private static void generateTree(Location center) {
        Location treeLocation = center.clone().add(-5, 2, 3);

        for (int y = 0; y < 4; y++) {
            treeLocation.clone().add(0, y, 0).getBlock().setType(Material.OAK_LOG);
        }

        Location leavesCenter = treeLocation.clone().add(0, 4, 0);
        for (int x = -2; x <= 2; x++) {
            for (int y = -1; y <= 1; y++) {
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
