package io.papermc.Grivience.dungeon;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DungeonBuilder {
    private static final int ROOM_INTERIOR_HEIGHT = 5;
    private static final int ROOM_WING_DEPTH = 2;
    private static final int ROOM_WING_HALF_WIDTH = 2;
    private static final int ROOM_DETAIL_OFFSET = 4;
    private static final int CEILING_BEAM_SPACING = 4;
    private static final int CORRIDOR_HALF_WIDTH = 3;
    private static final int CORRIDOR_INTERIOR_HEIGHT = 4;
    private static final int CORRIDOR_LIGHT_SPACING = 5;
    private static final int GATE_HALF_WIDTH = 2;

    private DungeonBuilder() {
    }

    public static ArenaLayout buildArena(World world, Location anchor, FloorConfig floor, List<RoomType> encounterPlan) {
        int totalRooms = encounterPlan.size() + 2; // lobby + encounter rooms + boss
        int roomSize = floor.roomSize();
        int gap = roomSize + 8;
        int y = anchor.getBlockY();
        int baseX = anchor.getBlockX();
        int baseZ = anchor.getBlockZ();

        List<GridPoint> roomPath = buildRoomPath(totalRooms, baseX, baseZ, gap);
        List<Location> centers = new ArrayList<>(totalRooms);
        for (int roomIndex = 0; roomIndex < totalRooms; roomIndex++) {
            GridPoint point = roomPath.get(roomIndex);
            int centerX = point.x();
            int centerZ = point.z();
            Material floorMaterial;
            Material wallMaterial;

            if (roomIndex == 0) {
                floorMaterial = Material.CHERRY_PLANKS;
                wallMaterial = Material.DARK_OAK_PLANKS;
            } else if (roomIndex == totalRooms - 1) {
                floorMaterial = Material.POLISHED_BLACKSTONE;
                wallMaterial = Material.POLISHED_BLACKSTONE_BRICKS;
            } else {
                RoomType roomType = encounterPlan.get(roomIndex - 1);
                floorMaterial = floorMaterialFor(roomType, floor);
                wallMaterial = wallMaterialFor(roomType, floor);
            }

            carveRoom(world, centerX, y, centerZ, roomSize, floorMaterial, wallMaterial);
            if (roomIndex == 0) {
                CorridorDirection exitDirection = roomPath.size() > 1
                        ? directionBetween(roomPath.get(0), roomPath.get(1))
                        : CorridorDirection.SOUTH;
                decorateLobby(world, centerX, y, centerZ, roomSize, exitDirection);
            } else if (roomIndex == totalRooms - 1) {
                CorridorDirection approachDirection = directionBetween(roomPath.get(roomIndex - 1), roomPath.get(roomIndex));
                decorateBossRoom(world, centerX, y, centerZ, roomSize, approachDirection);
            } else {
                decorateEncounterRoom(world, centerX, y, centerZ, roomSize, encounterPlan.get(roomIndex - 1));
            }
            centers.add(new Location(world, centerX + 0.5D, y + 1.0D, centerZ + 0.5D));
        }

        List<ArenaLayout.Gate> gates = new ArrayList<>();
        int radius = roomSize / 2;
        for (int gateIndex = 0; gateIndex < totalRooms - 1; gateIndex++) {
            GridPoint from = roomPath.get(gateIndex);
            GridPoint to = roomPath.get(gateIndex + 1);
            CorridorDirection direction = directionBetween(from, to);

            carveCorridor(world, y, from, to, radius, direction);
            List<Location> barrier = placeLockedGate(world, y, from, to, radius, direction);
            gates.add(ArenaLayout.Gate.of(gateIndex, barrier));
        }

        return new ArenaLayout(centers, gates);
    }

    private static List<GridPoint> buildRoomPath(int totalRooms, int baseX, int baseZ, int gap) {
        for (int attempt = 0; attempt < 80; attempt++) {
            List<GridPoint> path = new ArrayList<>(totalRooms);
            Set<String> used = new HashSet<>();
            GridPoint start = new GridPoint(baseX, baseZ);
            path.add(start);
            used.add(pointKey(start));

            CorridorDirection previousDirection = null;
            boolean failed = false;
            for (int index = 1; index < totalRooms; index++) {
                List<CorridorDirection> candidates = new ArrayList<>(List.of(CorridorDirection.values()));
                Collections.shuffle(candidates);

                GridPoint from = path.getLast();
                GridPoint selected = null;
                for (CorridorDirection candidate : candidates) {
                    if (previousDirection != null && candidate == previousDirection.opposite()) {
                        continue;
                    }
                    GridPoint next = new GridPoint(
                            from.x() + (candidate.stepX() * gap),
                            from.z() + (candidate.stepZ() * gap)
                    );
                    if (used.contains(pointKey(next))) {
                        continue;
                    }
                    selected = next;
                    previousDirection = candidate;
                    break;
                }

                if (selected == null) {
                    failed = true;
                    break;
                }
                path.add(selected);
                used.add(pointKey(selected));
            }

            if (!failed && path.size() == totalRooms) {
                return path;
            }
        }

        // Fallback to original straight layout if random path generation fails.
        List<GridPoint> fallback = new ArrayList<>(totalRooms);
        for (int roomIndex = 0; roomIndex < totalRooms; roomIndex++) {
            fallback.add(new GridPoint(baseX, baseZ + (roomIndex * gap)));
        }
        return fallback;
    }

    private static void carveRoom(
            World world,
            int centerX,
            int baseY,
            int centerZ,
            int roomSize,
            Material floorMaterial,
            Material wallMaterial
    ) {
        int radius = roomSize / 2;
        int minX = centerX - radius;
        int maxX = centerX + radius;
        int minZ = centerZ - radius;
        int maxZ = centerZ + radius;
        int wallTop = baseY + ROOM_INTERIOR_HEIGHT + 1;
        Material roofMaterial = slabFor(wallMaterial);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean edge = x == minX || x == maxX || z == minZ || z == maxZ;

                Block floor = world.getBlockAt(x, baseY, z);
                floor.setType(floorMaterial, false);

                for (int y = baseY + 1; y <= wallTop - 1; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (edge) {
                        block.setType(wallMaterial, false);
                    } else {
                        block.setType(Material.AIR, false);
                    }
                }

                Block roof = world.getBlockAt(x, wallTop, z);
                roof.setType(roofMaterial, false);
            }
        }

        carveRoomWing(world, centerX, baseY, centerZ, radius, wallTop, floorMaterial, wallMaterial, roofMaterial, CorridorDirection.NORTH);
        carveRoomWing(world, centerX, baseY, centerZ, radius, wallTop, floorMaterial, wallMaterial, roofMaterial, CorridorDirection.SOUTH);
        carveRoomWing(world, centerX, baseY, centerZ, radius, wallTop, floorMaterial, wallMaterial, roofMaterial, CorridorDirection.EAST);
        carveRoomWing(world, centerX, baseY, centerZ, radius, wallTop, floorMaterial, wallMaterial, roofMaterial, CorridorDirection.WEST);
        applyRoomDetailing(world, centerX, baseY, centerZ, radius, wallTop, floorMaterial, wallMaterial);
        placeRoomLights(world, baseY + ROOM_INTERIOR_HEIGHT, centerX, centerZ, radius);
    }

    private static void carveCorridor(
            World world,
            int baseY,
            GridPoint from,
            GridPoint to,
            int radius,
            CorridorDirection direction
    ) {
        switch (direction) {
            case NORTH -> carveZCorridor(world, baseY, from.x(), from.z() - radius, to.z() + radius);
            case SOUTH -> carveZCorridor(world, baseY, from.x(), from.z() + radius, to.z() - radius);
            case EAST -> carveXCorridor(world, baseY, from.z(), from.x() + radius, to.x() - radius);
            case WEST -> carveXCorridor(world, baseY, from.z(), from.x() - radius, to.x() + radius);
        }
    }

    private static void carveZCorridor(World world, int baseY, int centerX, int startZ, int endZ) {
        int minZ = Math.min(startZ, endZ);
        int maxZ = Math.max(startZ, endZ);
        Material roofMaterial = Material.DARK_OAK_SLAB;
        int roofY = baseY + CORRIDOR_INTERIOR_HEIGHT + 1;
        for (int z = minZ; z <= maxZ; z++) {
            for (int x = centerX - CORRIDOR_HALF_WIDTH; x <= centerX + CORRIDOR_HALF_WIDTH; x++) {
                world.getBlockAt(x, baseY, z).setType(Material.BAMBOO_PLANKS, false);
            }
            for (int y = baseY + 1; y <= baseY + CORRIDOR_INTERIOR_HEIGHT; y++) {
                world.getBlockAt(centerX - CORRIDOR_HALF_WIDTH, y, z).setType(Material.DARK_OAK_PLANKS, false);
                world.getBlockAt(centerX + CORRIDOR_HALF_WIDTH, y, z).setType(Material.DARK_OAK_PLANKS, false);
                for (int x = centerX - CORRIDOR_HALF_WIDTH + 1; x <= centerX + CORRIDOR_HALF_WIDTH - 1; x++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
                }
            }
            for (int x = centerX - CORRIDOR_HALF_WIDTH; x <= centerX + CORRIDOR_HALF_WIDTH; x++) {
                world.getBlockAt(x, roofY, z).setType(roofMaterial, false);
            }
            if ((z - minZ) % CORRIDOR_LIGHT_SPACING == 0) {
                placeLight(world, centerX, roofY, z);
            }
        }
    }

    private static void carveXCorridor(World world, int baseY, int centerZ, int startX, int endX) {
        int minX = Math.min(startX, endX);
        int maxX = Math.max(startX, endX);
        Material roofMaterial = Material.DARK_OAK_SLAB;
        int roofY = baseY + CORRIDOR_INTERIOR_HEIGHT + 1;
        for (int x = minX; x <= maxX; x++) {
            for (int z = centerZ - CORRIDOR_HALF_WIDTH; z <= centerZ + CORRIDOR_HALF_WIDTH; z++) {
                world.getBlockAt(x, baseY, z).setType(Material.BAMBOO_PLANKS, false);
            }
            for (int y = baseY + 1; y <= baseY + CORRIDOR_INTERIOR_HEIGHT; y++) {
                world.getBlockAt(x, y, centerZ - CORRIDOR_HALF_WIDTH).setType(Material.DARK_OAK_PLANKS, false);
                world.getBlockAt(x, y, centerZ + CORRIDOR_HALF_WIDTH).setType(Material.DARK_OAK_PLANKS, false);
                for (int z = centerZ - CORRIDOR_HALF_WIDTH + 1; z <= centerZ + CORRIDOR_HALF_WIDTH - 1; z++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
                }
            }
            for (int z = centerZ - CORRIDOR_HALF_WIDTH; z <= centerZ + CORRIDOR_HALF_WIDTH; z++) {
                world.getBlockAt(x, roofY, z).setType(roofMaterial, false);
            }
            if ((x - minX) % CORRIDOR_LIGHT_SPACING == 0) {
                placeLight(world, x, roofY, centerZ);
            }
        }
    }

    private static List<Location> placeLockedGate(
            World world,
            int baseY,
            GridPoint from,
            GridPoint to,
            int radius,
            CorridorDirection direction
    ) {
        return switch (direction) {
            case NORTH -> placeZGate(world, baseY, from.x(), (from.z() - radius + to.z() + radius) / 2);
            case SOUTH -> placeZGate(world, baseY, from.x(), (from.z() + radius + to.z() - radius) / 2);
            case EAST -> placeXGate(world, baseY, (from.x() + radius + to.x() - radius) / 2, from.z());
            case WEST -> placeXGate(world, baseY, (from.x() - radius + to.x() + radius) / 2, from.z());
        };
    }

    private static List<Location> placeZGate(World world, int baseY, int centerX, int gateZ) {
        List<Location> barrier = new ArrayList<>(20);
        for (int x = centerX - GATE_HALF_WIDTH; x <= centerX + GATE_HALF_WIDTH; x++) {
            for (int y = baseY + 1; y <= baseY + CORRIDOR_INTERIOR_HEIGHT; y++) {
                Block block = world.getBlockAt(x, y, gateZ);
                block.setType(Material.COAL_BLOCK, false);
                barrier.add(block.getLocation());
            }
        }
        world.getBlockAt(centerX, baseY + CORRIDOR_INTERIOR_HEIGHT + 1, gateZ).setType(Material.LANTERN, false);
        return barrier;
    }

    private static List<Location> placeXGate(World world, int baseY, int gateX, int centerZ) {
        List<Location> barrier = new ArrayList<>(20);
        for (int z = centerZ - GATE_HALF_WIDTH; z <= centerZ + GATE_HALF_WIDTH; z++) {
            for (int y = baseY + 1; y <= baseY + CORRIDOR_INTERIOR_HEIGHT; y++) {
                Block block = world.getBlockAt(gateX, y, z);
                block.setType(Material.COAL_BLOCK, false);
                barrier.add(block.getLocation());
            }
        }
        world.getBlockAt(gateX, baseY + CORRIDOR_INTERIOR_HEIGHT + 1, centerZ).setType(Material.LANTERN, false);
        return barrier;
    }

    private static void applyRoomDetailing(
            World world,
            int centerX,
            int baseY,
            int centerZ,
            int radius,
            int wallTop,
            Material floorMaterial,
            Material wallMaterial
    ) {
        carveFloorInlays(world, centerX, baseY, centerZ, radius, floorMaterial, wallMaterial);
        carveCeilingBeams(world, centerX, centerZ, radius, wallTop - 1, wallMaterial);
        carveWallNiches(world, centerX, baseY, centerZ, radius, wallMaterial);
        addHangingLanternCluster(world, centerX, baseY, centerZ, Math.max(3, radius - 3));
    }

    private static void carveFloorInlays(
            World world,
            int centerX,
            int baseY,
            int centerZ,
            int radius,
            Material floorMaterial,
            Material wallMaterial
    ) {
        Material primaryAccent = detailAccentFor(wallMaterial);
        Material secondaryAccent = detailSecondaryAccentFor(floorMaterial);
        int inset = Math.max(3, radius - 2);
        int innerRing = Math.max(2, inset - 3);

        for (int offset = -inset; offset <= inset; offset++) {
            world.getBlockAt(centerX + offset, baseY, centerZ - inset).setType(primaryAccent, false);
            world.getBlockAt(centerX + offset, baseY, centerZ + inset).setType(primaryAccent, false);
            world.getBlockAt(centerX - inset, baseY, centerZ + offset).setType(primaryAccent, false);
            world.getBlockAt(centerX + inset, baseY, centerZ + offset).setType(primaryAccent, false);
        }

        for (int offset = -innerRing; offset <= innerRing; offset++) {
            world.getBlockAt(centerX + offset, baseY, centerZ - innerRing).setType(secondaryAccent, false);
            world.getBlockAt(centerX + offset, baseY, centerZ + innerRing).setType(secondaryAccent, false);
            world.getBlockAt(centerX - innerRing, baseY, centerZ + offset).setType(secondaryAccent, false);
            world.getBlockAt(centerX + innerRing, baseY, centerZ + offset).setType(secondaryAccent, false);
        }

        for (int offset = -inset + 1; offset <= inset - 1; offset += 2) {
            world.getBlockAt(centerX + offset, baseY, centerZ + offset).setType(secondaryAccent, false);
            world.getBlockAt(centerX + offset, baseY, centerZ - offset).setType(secondaryAccent, false);
        }
    }

    private static void carveCeilingBeams(
            World world,
            int centerX,
            int centerZ,
            int radius,
            int beamY,
            Material wallMaterial
    ) {
        Material beamMaterial = detailAccentFor(wallMaterial);
        int minX = centerX - radius + 2;
        int maxX = centerX + radius - 2;
        int minZ = centerZ - radius + 2;
        int maxZ = centerZ + radius - 2;

        for (int x = minX; x <= maxX; x += CEILING_BEAM_SPACING) {
            for (int z = minZ; z <= maxZ; z++) {
                world.getBlockAt(x, beamY, z).setType(beamMaterial, false);
            }
        }
        for (int z = minZ; z <= maxZ; z += CEILING_BEAM_SPACING) {
            for (int x = minX; x <= maxX; x++) {
                world.getBlockAt(x, beamY, z).setType(beamMaterial, false);
            }
        }
    }

    private static void carveWallNiches(
            World world,
            int centerX,
            int baseY,
            int centerZ,
            int radius,
            Material wallMaterial
    ) {
        int[] offsets = {-ROOM_DETAIL_OFFSET, 0, ROOM_DETAIL_OFFSET};
        for (int offset : offsets) {
            if (Math.abs(offset) >= radius - 1) {
                continue;
            }
            carveNiche(world, centerX + offset, baseY, centerZ - radius, wallMaterial);
            carveNiche(world, centerX + offset, baseY, centerZ + radius, wallMaterial);
            carveNiche(world, centerX - radius, baseY, centerZ + offset, wallMaterial);
            carveNiche(world, centerX + radius, baseY, centerZ + offset, wallMaterial);
        }
    }

    private static void carveNiche(World world, int x, int baseY, int z, Material wallMaterial) {
        Material detail = detailAccentFor(wallMaterial);
        world.getBlockAt(x, baseY + 1, z).setType(detail, false);
        world.getBlockAt(x, baseY + 3, z).setType(detail, false);
        world.getBlockAt(x, baseY + 4, z).setType(Material.IRON_BARS, false);
        world.getBlockAt(x, baseY + 2, z).setType(Material.LANTERN, false);
    }

    private static void addHangingLanternCluster(World world, int centerX, int baseY, int centerZ, int offset) {
        hangLantern(world, centerX, baseY, centerZ);
        hangLantern(world, centerX - offset, baseY, centerZ);
        hangLantern(world, centerX + offset, baseY, centerZ);
        hangLantern(world, centerX, baseY, centerZ - offset);
        hangLantern(world, centerX, baseY, centerZ + offset);
    }

    private static void hangLantern(World world, int x, int baseY, int z) {
        world.getBlockAt(x, baseY + ROOM_INTERIOR_HEIGHT, z).setType(Material.IRON_BARS, false);
        world.getBlockAt(x, baseY + ROOM_INTERIOR_HEIGHT - 1, z).setType(Material.LANTERN, false);
    }

    private static void placeLight(World world, int x, int y, int z) {
        world.getBlockAt(x, y, z).setType(Material.SEA_LANTERN, false);
    }

    private static Material floorMaterialFor(RoomType type, FloorConfig floor) {
        return switch (type) {
            case COMBAT -> floor.floorMaterial();
            case PUZZLE_SEQUENCE -> Material.CHERRY_PLANKS;
            case PUZZLE_SYNC -> Material.BAMBOO_MOSAIC;
            case TREASURE -> Material.RED_TERRACOTTA;
        };
    }

    private static Material wallMaterialFor(RoomType type, FloorConfig floor) {
        return switch (type) {
            case COMBAT -> floor.wallMaterial();
            case PUZZLE_SEQUENCE -> Material.CHERRY_WOOD;
            case PUZZLE_SYNC -> Material.BAMBOO_BLOCK;
            case TREASURE -> Material.DARK_OAK_PLANKS;
        };
    }

    private static void decorateEncounterRoom(World world, int centerX, int baseY, int centerZ, int roomSize, RoomType type) {
        int radius = roomSize / 2;
        int pillarOffset = Math.max(2, radius - 2);
        placePillar(world, centerX - pillarOffset, baseY + 1, centerZ - pillarOffset, 4, Material.STRIPPED_CHERRY_LOG);
        placePillar(world, centerX - pillarOffset, baseY + 1, centerZ + pillarOffset, 4, Material.STRIPPED_CHERRY_LOG);
        placePillar(world, centerX + pillarOffset, baseY + 1, centerZ - pillarOffset, 4, Material.STRIPPED_CHERRY_LOG);
        placePillar(world, centerX + pillarOffset, baseY + 1, centerZ + pillarOffset, 4, Material.STRIPPED_CHERRY_LOG);
        addEncounterLanternRing(world, centerX, baseY, centerZ, radius);

        switch (type) {
            case COMBAT -> decorateCombatRoom(world, centerX, baseY, centerZ, radius);
            case PUZZLE_SEQUENCE -> decorateSequenceRoom(world, centerX, baseY, centerZ, radius);
            case PUZZLE_SYNC -> decorateSyncRoom(world, centerX, baseY, centerZ, radius);
            case TREASURE -> decorateTreasureVault(world, centerX, baseY, centerZ, radius);
        }
    }

    private static void addEncounterLanternRing(World world, int centerX, int baseY, int centerZ, int radius) {
        int offset = Math.max(3, radius - 4);
        hangLantern(world, centerX - offset, baseY, centerZ - offset);
        hangLantern(world, centerX - offset, baseY, centerZ + offset);
        hangLantern(world, centerX + offset, baseY, centerZ - offset);
        hangLantern(world, centerX + offset, baseY, centerZ + offset);
    }

    private static void decorateCombatRoom(World world, int centerX, int baseY, int centerZ, int radius) {
        int trackZNorth = centerZ - radius + 1;
        int trackZSouth = centerZ + radius - 1;
        int[] xOffsets = {-ROOM_DETAIL_OFFSET, 0, ROOM_DETAIL_OFFSET};
        for (int offset : xOffsets) {
            if (Math.abs(offset) >= radius - 1) {
                continue;
            }
            world.getBlockAt(centerX + offset, baseY + 1, trackZNorth).setType(Material.TARGET, false);
            world.getBlockAt(centerX + offset, baseY + 1, trackZSouth).setType(Material.TARGET, false);
        }
        world.getBlockAt(centerX - radius + 2, baseY + 1, centerZ).setType(Material.SMITHING_TABLE, false);
        world.getBlockAt(centerX + radius - 2, baseY + 1, centerZ).setType(Material.ANVIL, false);
    }

    private static void decorateSequenceRoom(World world, int centerX, int baseY, int centerZ, int radius) {
        int z = centerZ + Math.min(4, radius - 3);
        Material[] colors = {Material.RED_CONCRETE, Material.LIME_CONCRETE, Material.BLUE_CONCRETE, Material.YELLOW_CONCRETE};
        int[] xOffsets = {-3, -1, 1, 3};
        for (int index = 0; index < xOffsets.length; index++) {
            world.getBlockAt(centerX + xOffsets[index], baseY, z).setType(colors[index], false);
        }

        int shelfX = centerX - radius + 1;
        int shelfZ = centerZ - radius + 1;
        world.getBlockAt(shelfX, baseY + 1, shelfZ).setType(Material.BOOKSHELF, false);
        world.getBlockAt(centerX + radius - 1, baseY + 1, shelfZ).setType(Material.BOOKSHELF, false);
    }

    private static void decorateSyncRoom(World world, int centerX, int baseY, int centerZ, int radius) {
        world.getBlockAt(centerX, baseY + 1, centerZ).setType(Material.BELL, false);
        int altarOffset = Math.max(5, radius - 3);
        placePillar(world, centerX - altarOffset, baseY + 1, centerZ - altarOffset, 3, Material.BAMBOO_BLOCK);
        placePillar(world, centerX - altarOffset, baseY + 1, centerZ + altarOffset, 3, Material.BAMBOO_BLOCK);
        placePillar(world, centerX + altarOffset, baseY + 1, centerZ - altarOffset, 3, Material.BAMBOO_BLOCK);
        placePillar(world, centerX + altarOffset, baseY + 1, centerZ + altarOffset, 3, Material.BAMBOO_BLOCK);
    }

    private static void decorateTreasureVault(World world, int centerX, int baseY, int centerZ, int radius) {
        world.getBlockAt(centerX, baseY + 1, centerZ - 1).setType(Material.GOLD_BLOCK, false);
        world.getBlockAt(centerX, baseY + 1, centerZ + 1).setType(Material.GOLD_BLOCK, false);

        int vaultX = centerX - radius + 1;
        int vaultZ = centerZ + radius - 1;
        world.getBlockAt(vaultX, baseY + 1, centerZ).setType(Material.BARREL, false);
        world.getBlockAt(centerX + radius - 1, baseY + 1, centerZ).setType(Material.BARREL, false);
        world.getBlockAt(centerX, baseY + 1, vaultZ).setType(Material.CHISELED_STONE_BRICKS, false);
    }

    private static void decorateLobby(
            World world,
            int centerX,
            int baseY,
            int centerZ,
            int roomSize,
            CorridorDirection exitDirection
    ) {
        int radius = roomSize / 2;
        int accentY = baseY + ROOM_INTERIOR_HEIGHT + 1;
        switch (exitDirection) {
            case SOUTH -> {
                int gateZ = centerZ + radius - 1;
                placePillar(world, centerX - 2, baseY + 1, gateZ, 4, Material.RED_CONCRETE);
                placePillar(world, centerX + 2, baseY + 1, gateZ, 4, Material.RED_CONCRETE);
                for (int x = centerX - 3; x <= centerX + 3; x++) {
                    world.getBlockAt(x, accentY, gateZ).setType(Material.POLISHED_BLACKSTONE, false);
                }
            }
            case NORTH -> {
                int gateZ = centerZ - radius + 1;
                placePillar(world, centerX - 2, baseY + 1, gateZ, 4, Material.RED_CONCRETE);
                placePillar(world, centerX + 2, baseY + 1, gateZ, 4, Material.RED_CONCRETE);
                for (int x = centerX - 3; x <= centerX + 3; x++) {
                    world.getBlockAt(x, accentY, gateZ).setType(Material.POLISHED_BLACKSTONE, false);
                }
            }
            case EAST -> {
                int gateX = centerX + radius - 1;
                placePillar(world, gateX, baseY + 1, centerZ - 2, 4, Material.RED_CONCRETE);
                placePillar(world, gateX, baseY + 1, centerZ + 2, 4, Material.RED_CONCRETE);
                for (int z = centerZ - 3; z <= centerZ + 3; z++) {
                    world.getBlockAt(gateX, accentY, z).setType(Material.POLISHED_BLACKSTONE, false);
                }
            }
            case WEST -> {
                int gateX = centerX - radius + 1;
                placePillar(world, gateX, baseY + 1, centerZ - 2, 4, Material.RED_CONCRETE);
                placePillar(world, gateX, baseY + 1, centerZ + 2, 4, Material.RED_CONCRETE);
                for (int z = centerZ - 3; z <= centerZ + 3; z++) {
                    world.getBlockAt(gateX, accentY, z).setType(Material.POLISHED_BLACKSTONE, false);
                }
            }
        }
        decorateLobbySanctum(world, centerX, baseY, centerZ, radius);
        world.getBlockAt(centerX, baseY + 1, centerZ).setType(Material.BELL, false);
    }

    private static void decorateBossRoom(
            World world,
            int centerX,
            int baseY,
            int centerZ,
            int roomSize,
            CorridorDirection approachDirection
    ) {
        int radius = roomSize / 2;
        int accentY = baseY + ROOM_INTERIOR_HEIGHT + 1;
        CorridorDirection markerSide = approachDirection.opposite();

        switch (markerSide) {
            case NORTH -> {
                int markerZ = centerZ - radius + 1;
                placePillar(world, centerX - 3, baseY + 1, markerZ, 4, Material.RED_CONCRETE);
                placePillar(world, centerX + 3, baseY + 1, markerZ, 4, Material.RED_CONCRETE);
                for (int x = centerX - 4; x <= centerX + 4; x++) {
                    world.getBlockAt(x, accentY, markerZ).setType(Material.POLISHED_BLACKSTONE_BRICKS, false);
                }
            }
            case SOUTH -> {
                int markerZ = centerZ + radius - 1;
                placePillar(world, centerX - 3, baseY + 1, markerZ, 4, Material.RED_CONCRETE);
                placePillar(world, centerX + 3, baseY + 1, markerZ, 4, Material.RED_CONCRETE);
                for (int x = centerX - 4; x <= centerX + 4; x++) {
                    world.getBlockAt(x, accentY, markerZ).setType(Material.POLISHED_BLACKSTONE_BRICKS, false);
                }
            }
            case EAST -> {
                int markerX = centerX + radius - 1;
                placePillar(world, markerX, baseY + 1, centerZ - 3, 4, Material.RED_CONCRETE);
                placePillar(world, markerX, baseY + 1, centerZ + 3, 4, Material.RED_CONCRETE);
                for (int z = centerZ - 4; z <= centerZ + 4; z++) {
                    world.getBlockAt(markerX, accentY, z).setType(Material.POLISHED_BLACKSTONE_BRICKS, false);
                }
            }
            case WEST -> {
                int markerX = centerX - radius + 1;
                placePillar(world, markerX, baseY + 1, centerZ - 3, 4, Material.RED_CONCRETE);
                placePillar(world, markerX, baseY + 1, centerZ + 3, 4, Material.RED_CONCRETE);
                for (int z = centerZ - 4; z <= centerZ + 4; z++) {
                    world.getBlockAt(markerX, accentY, z).setType(Material.POLISHED_BLACKSTONE_BRICKS, false);
                }
            }
        }
        decorateBossSanctum(world, centerX, baseY, centerZ, radius);
        world.getBlockAt(centerX, baseY + 1, centerZ).setType(Material.BLACKSTONE, false);
    }

    private static void decorateLobbySanctum(World world, int centerX, int baseY, int centerZ, int radius) {
        int ring = Math.max(3, radius - 4);
        for (int offset = -ring; offset <= ring; offset++) {
            world.getBlockAt(centerX + offset, baseY, centerZ).setType(Material.CHERRY_WOOD, false);
            world.getBlockAt(centerX, baseY, centerZ + offset).setType(Material.CHERRY_WOOD, false);
        }
        addHangingLanternCluster(world, centerX, baseY, centerZ, Math.max(2, ring - 1));
    }

    private static void decorateBossSanctum(World world, int centerX, int baseY, int centerZ, int radius) {
        int ring = Math.max(3, radius - 4);
        for (int offset = -ring; offset <= ring; offset++) {
            world.getBlockAt(centerX + offset, baseY, centerZ - ring).setType(Material.BLACKSTONE, false);
            world.getBlockAt(centerX + offset, baseY, centerZ + ring).setType(Material.BLACKSTONE, false);
            world.getBlockAt(centerX - ring, baseY, centerZ + offset).setType(Material.BLACKSTONE, false);
            world.getBlockAt(centerX + ring, baseY, centerZ + offset).setType(Material.BLACKSTONE, false);
        }

        int brazierOffset = Math.max(4, radius - 3);
        placePillar(world, centerX - brazierOffset, baseY + 1, centerZ - brazierOffset, 3, Material.POLISHED_BLACKSTONE_BRICKS);
        placePillar(world, centerX - brazierOffset, baseY + 1, centerZ + brazierOffset, 3, Material.POLISHED_BLACKSTONE_BRICKS);
        placePillar(world, centerX + brazierOffset, baseY + 1, centerZ - brazierOffset, 3, Material.POLISHED_BLACKSTONE_BRICKS);
        placePillar(world, centerX + brazierOffset, baseY + 1, centerZ + brazierOffset, 3, Material.POLISHED_BLACKSTONE_BRICKS);

        hangSoulLantern(world, centerX - brazierOffset, baseY, centerZ - brazierOffset);
        hangSoulLantern(world, centerX - brazierOffset, baseY, centerZ + brazierOffset);
        hangSoulLantern(world, centerX + brazierOffset, baseY, centerZ - brazierOffset);
        hangSoulLantern(world, centerX + brazierOffset, baseY, centerZ + brazierOffset);
    }

    private static void hangSoulLantern(World world, int x, int baseY, int z) {
        world.getBlockAt(x, baseY + ROOM_INTERIOR_HEIGHT, z).setType(Material.IRON_BARS, false);
        world.getBlockAt(x, baseY + ROOM_INTERIOR_HEIGHT - 1, z).setType(Material.SOUL_LANTERN, false);
    }

    private static void carveRoomWing(
            World world,
            int centerX,
            int baseY,
            int centerZ,
            int radius,
            int wallTop,
            Material floorMaterial,
            Material wallMaterial,
            Material roofMaterial,
            CorridorDirection direction
    ) {
        int lateralStepX = direction == CorridorDirection.NORTH || direction == CorridorDirection.SOUTH ? 1 : 0;
        int lateralStepZ = direction == CorridorDirection.NORTH || direction == CorridorDirection.SOUTH ? 0 : 1;

        for (int depth = 1; depth <= ROOM_WING_DEPTH; depth++) {
            for (int lateral = -ROOM_WING_HALF_WIDTH; lateral <= ROOM_WING_HALF_WIDTH; lateral++) {
                int x = centerX + (direction.stepX() * (radius + depth)) + (lateralStepX * lateral);
                int z = centerZ + (direction.stepZ() * (radius + depth)) + (lateralStepZ * lateral);

                world.getBlockAt(x, baseY, z).setType(floorMaterial, false);

                boolean edge = Math.abs(lateral) == ROOM_WING_HALF_WIDTH || depth == ROOM_WING_DEPTH;
                for (int y = baseY + 1; y <= wallTop - 1; y++) {
                    world.getBlockAt(x, y, z).setType(edge ? wallMaterial : Material.AIR, false);
                }
                world.getBlockAt(x, wallTop, z).setType(roofMaterial, false);
            }
        }
    }

    private static void placeRoomLights(World world, int lightY, int centerX, int centerZ, int radius) {
        int inset = Math.max(2, radius - 2);
        placeLight(world, centerX - inset, lightY, centerZ - inset);
        placeLight(world, centerX - inset, lightY, centerZ + inset);
        placeLight(world, centerX + inset, lightY, centerZ - inset);
        placeLight(world, centerX + inset, lightY, centerZ + inset);
        placeLight(world, centerX, lightY, centerZ);
        placeLight(world, centerX - inset, lightY, centerZ);
        placeLight(world, centerX + inset, lightY, centerZ);
        placeLight(world, centerX, lightY, centerZ - inset);
        placeLight(world, centerX, lightY, centerZ + inset);
    }

    private static void placePillar(World world, int x, int startY, int z, int height, Material material) {
        for (int y = startY; y < startY + height; y++) {
            world.getBlockAt(x, y, z).setType(material, false);
        }
    }

    private static Material detailAccentFor(Material wallMaterial) {
        return switch (wallMaterial) {
            case DARK_OAK_PLANKS -> Material.CHERRY_WOOD;
            case POLISHED_BLACKSTONE_BRICKS -> Material.CHISELED_POLISHED_BLACKSTONE;
            case CHERRY_WOOD -> Material.STRIPPED_CHERRY_WOOD;
            case BAMBOO_BLOCK -> Material.BAMBOO_MOSAIC;
            case DEEPSLATE_BRICKS -> Material.CRACKED_DEEPSLATE_BRICKS;
            default -> Material.POLISHED_BLACKSTONE;
        };
    }

    private static Material detailSecondaryAccentFor(Material floorMaterial) {
        return switch (floorMaterial) {
            case BAMBOO_MOSAIC -> Material.BAMBOO_PLANKS;
            case CHERRY_PLANKS -> Material.STRIPPED_CHERRY_WOOD;
            case POLISHED_BLACKSTONE -> Material.GILDED_BLACKSTONE;
            case POLISHED_DEEPSLATE -> Material.CRACKED_DEEPSLATE_BRICKS;
            case RED_TERRACOTTA -> Material.CUT_RED_SANDSTONE;
            default -> Material.SMOOTH_STONE;
        };
    }

    private static Material slabFor(Material wallMaterial) {
        return switch (wallMaterial) {
            case DARK_OAK_PLANKS -> Material.DARK_OAK_SLAB;
            case POLISHED_BLACKSTONE_BRICKS -> Material.POLISHED_BLACKSTONE_BRICK_SLAB;
            case CHERRY_WOOD -> Material.CHERRY_SLAB;
            case BAMBOO_BLOCK -> Material.BAMBOO_SLAB;
            case BLACKSTONE -> Material.BLACKSTONE_SLAB;
            case DEEPSLATE_BRICKS -> Material.DEEPSLATE_BRICK_SLAB;
            default -> Material.DARK_OAK_SLAB;
        };
    }

    private static CorridorDirection directionBetween(GridPoint from, GridPoint to) {
        int dx = Integer.compare(to.x(), from.x());
        int dz = Integer.compare(to.z(), from.z());
        if (dx > 0) {
            return CorridorDirection.EAST;
        }
        if (dx < 0) {
            return CorridorDirection.WEST;
        }
        if (dz < 0) {
            return CorridorDirection.NORTH;
        }
        return CorridorDirection.SOUTH;
    }

    private static String pointKey(GridPoint point) {
        return point.x() + ":" + point.z();
    }

    private record GridPoint(int x, int z) {
    }

    private enum CorridorDirection {
        NORTH(0, -1),
        SOUTH(0, 1),
        EAST(1, 0),
        WEST(-1, 0);

        private final int stepX;
        private final int stepZ;

        CorridorDirection(int stepX, int stepZ) {
            this.stepX = stepX;
            this.stepZ = stepZ;
        }

        int stepX() {
            return stepX;
        }

        int stepZ() {
            return stepZ;
        }

        CorridorDirection opposite() {
            return switch (this) {
                case NORTH -> SOUTH;
                case SOUTH -> NORTH;
                case EAST -> WEST;
                case WEST -> EAST;
            };
        }
    }
}
