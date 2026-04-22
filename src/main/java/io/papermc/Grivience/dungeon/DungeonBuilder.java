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
    private static final int ROOM_INTERIOR_HEIGHT = 14;
    private static final int ROOM_WING_DEPTH = 5;
    private static final int ROOM_WING_HALF_WIDTH = 4;
    private static final int ROOM_DETAIL_OFFSET = 6;
    private static final int ROOM_PERSPECTIVE_BAY_DEPTH = 3;
    private static final int ROOM_PERSPECTIVE_BAY_HALF_WIDTH = 2;
    private static final int CEILING_BEAM_SPACING = 4;
    private static final int CORRIDOR_HALF_WIDTH = 5;
    private static final int CORRIDOR_INTERIOR_HEIGHT = 8;
    private static final int CORRIDOR_LIGHT_SPACING = 4;
    private static final int CORRIDOR_ARCH_SPACING = 3;
    private static final int GATE_HALF_WIDTH = 4;
    private static final Material DUNGEON_SOLID_ACCENT = Material.BLACK_CONCRETE;
    private static final Material SECRET_CHEST = Material.CHEST;
    private static final Material BLESSING_ALTAR = Material.ENCHANTING_TABLE;

    private DungeonBuilder() {
    }

    static Material solidAccentMaterial() {
        return DUNGEON_SOLID_ACCENT;
    }

    static Material gateBarrierMaterial(FloorConfig floor) {
        return themeForFloor(floor).gateBarrier();
    }

    static int spawnRadius(int roomSize) {
        return Math.max(4, (roomSize / 2) - 3 + (ROOM_WING_DEPTH - 1));
    }

    static int perimeterFeatureOffset(int roomSize) {
        return Math.max(3, (roomSize / 2) - 4);
    }

    static int encounterPillarOffset(int roomSize) {
        return Math.max(3, (roomSize / 2) - 3);
    }

    public static ArenaLayout buildArena(World world, Location anchor, FloorConfig floor, List<RoomType> encounterPlan) {
        int totalRooms = encounterPlan.size() + 2; // lobby + encounter rooms + boss
        int roomSize = floor.roomSize();
        int gap = roomSize + 8;
        int y = anchor.getBlockY();
        int baseX = anchor.getBlockX();
        int baseZ = anchor.getBlockZ();
        DungeonTheme theme = themeForFloor(floor);

        List<GridPoint> roomPath = buildRoomPath(totalRooms, baseX, baseZ, gap);
        List<Location> centers = new ArrayList<>(totalRooms);
        List<ArenaLayout.Cuboid> cleanupVolumes = new ArrayList<>();
        for (int roomIndex = 0; roomIndex < totalRooms; roomIndex++) {
            GridPoint point = roomPath.get(roomIndex);
            int centerX = point.x();
            int centerZ = point.z();
            Material floorMaterial;
            Material wallMaterial;

            if (roomIndex == 0) {
                floorMaterial = Material.STONE_BRICKS;
                wallMaterial = Material.MOSSY_STONE_BRICKS;
            } else if (roomIndex == totalRooms - 1) {
                floorMaterial = Material.DEEPSLATE_TILES;
                wallMaterial = Material.DEEPSLATE_BRICKS;
            } else {
                RoomType roomType = encounterPlan.get(roomIndex - 1);
                floorMaterial = floorMaterialFor(roomType, floor);
                wallMaterial = wallMaterialFor(roomType, floor);
            }

            carveRoom(world, centerX, y, centerZ, roomSize, floorMaterial, wallMaterial);
            cleanupVolumes.add(roomCleanupVolume(centerX, y, centerZ, roomSize));
            if (roomIndex == 0) {
                CorridorDirection exitDirection = roomPath.size() > 1
                        ? directionBetween(roomPath.get(0), roomPath.get(1))
                        : CorridorDirection.SOUTH;
                decorateLobby(world, centerX, y, centerZ, roomSize, exitDirection, floor);
            } else if (roomIndex == totalRooms - 1) {
                CorridorDirection approachDirection = directionBetween(roomPath.get(roomIndex - 1), roomPath.get(roomIndex));
                decorateBossRoom(world, centerX, y, centerZ, roomSize, approachDirection, floor);
            } else {
                decorateEncounterRoom(world, centerX, y, centerZ, roomSize, encounterPlan.get(roomIndex - 1), floor);
            }
            centers.add(new Location(world, centerX + 0.5D, y + 1.0D, centerZ + 0.5D));
        }

        List<ArenaLayout.Gate> gates = new ArrayList<>();
        int radius = roomSize / 2;
        for (int gateIndex = 0; gateIndex < totalRooms - 1; gateIndex++) {
            GridPoint from = roomPath.get(gateIndex);
            GridPoint to = roomPath.get(gateIndex + 1);
            CorridorDirection direction = directionBetween(from, to);

            carveCorridor(world, y, from, to, radius, direction, theme);
            cleanupVolumes.add(corridorCleanupVolume(y, from, to, radius, direction));
            List<Location> barrier = placeLockedGate(world, y, from, to, radius, direction, theme);
            gates.add(ArenaLayout.Gate.of(gateIndex, barrier, theme.gateBarrier()));
        }

        return new ArenaLayout(centers, gates, cleanupVolumes);
    }

    private static ArenaLayout.Cuboid roomCleanupVolume(int centerX, int baseY, int centerZ, int roomSize) {
        int radius = roomSize / 2;
        int extra = ROOM_WING_DEPTH;
        int minX = centerX - radius - extra;
        int maxX = centerX + radius + extra;
        int minZ = centerZ - radius - extra;
        int maxZ = centerZ + radius + extra;
        int minY = baseY;
        int maxY = baseY + ROOM_INTERIOR_HEIGHT + 2;
        return new ArenaLayout.Cuboid(minX, maxX, minY, maxY, minZ, maxZ);
    }

    private static ArenaLayout.Cuboid corridorCleanupVolume(
            int baseY,
            GridPoint from,
            GridPoint to,
            int roomRadius,
            CorridorDirection direction
    ) {
        int minY = baseY;
        int maxY = baseY + CORRIDOR_INTERIOR_HEIGHT + 2;
        if (direction == CorridorDirection.NORTH || direction == CorridorDirection.SOUTH) {
            int centerX = from.x();
            int startZ = Math.min(from.z(), to.z()) - roomRadius;
            int endZ = Math.max(from.z(), to.z()) + roomRadius;
            return new ArenaLayout.Cuboid(
                    centerX - CORRIDOR_HALF_WIDTH,
                    centerX + CORRIDOR_HALF_WIDTH,
                    minY,
                    maxY,
                    startZ,
                    endZ
            );
        }

        int centerZ = from.z();
        int startX = Math.min(from.x(), to.x()) - roomRadius;
        int endX = Math.max(from.x(), to.x()) + roomRadius;
        return new ArenaLayout.Cuboid(
                startX,
                endX,
                minY,
                maxY,
                centerZ - CORRIDOR_HALF_WIDTH,
                centerZ + CORRIDOR_HALF_WIDTH
        );
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
        carvePerspectiveBays(world, centerX, baseY, centerZ, radius, wallTop, floorMaterial, wallMaterial, roofMaterial);
        addRoomPerimeterFraming(world, centerX, baseY, centerZ, radius, wallTop, wallMaterial);
        applyRoomDetailing(world, centerX, baseY, centerZ, radius, wallTop, floorMaterial, wallMaterial);
        carveVaultedCeiling(world, centerX, centerZ, radius, wallTop, wallMaterial, roofMaterial);
        placeRoomLights(world, baseY + ROOM_INTERIOR_HEIGHT, centerX, centerZ, radius);
    }

    private static void carveCorridor(
            World world,
            int baseY,
            GridPoint from,
            GridPoint to,
            int radius,
            CorridorDirection direction,
            DungeonTheme theme
    ) {
        switch (direction) {
            case NORTH -> carveZCorridor(world, baseY, from.x(), from.z() - radius, to.z() + radius, theme);
            case SOUTH -> carveZCorridor(world, baseY, from.x(), from.z() + radius, to.z() - radius, theme);
            case EAST -> carveXCorridor(world, baseY, from.z(), from.x() + radius, to.x() - radius, theme);
            case WEST -> carveXCorridor(world, baseY, from.z(), from.x() - radius, to.x() + radius, theme);
        }
    }

    private static void carveZCorridor(World world, int baseY, int centerX, int startZ, int endZ, DungeonTheme theme) {
        int minZ = Math.min(startZ, endZ);
        int maxZ = Math.max(startZ, endZ);
        Material roofMaterial = theme.corridorRoof();
        Material floorMaterial = theme.corridorFloor();
        Material wallMaterial = theme.corridorWall();
        Material trimMaterial = theme.corridorTrim();
        Material lightMaterial = theme.lightMaterial();
        int roofY = baseY + CORRIDOR_INTERIOR_HEIGHT + 1;
        for (int z = minZ; z <= maxZ; z++) {
            boolean arch = (z - minZ) % CORRIDOR_ARCH_SPACING == 0;
            boolean striped = ((z - minZ) & 1) == 0;
            for (int x = centerX - CORRIDOR_HALF_WIDTH; x <= centerX + CORRIDOR_HALF_WIDTH; x++) {
                int abs = Math.abs(x - centerX);
                Material floorType = (striped && abs == CORRIDOR_HALF_WIDTH - 1) ? trimMaterial : floorMaterial;
                world.getBlockAt(x, baseY, z).setType(floorType, false);
            }
            for (int y = baseY + 1; y <= baseY + CORRIDOR_INTERIOR_HEIGHT; y++) {
                for (int x = centerX - CORRIDOR_HALF_WIDTH; x <= centerX + CORRIDOR_HALF_WIDTH; x++) {
                    int abs = Math.abs(x - centerX);
                    if (abs == CORRIDOR_HALF_WIDTH) {
                        world.getBlockAt(x, y, z).setType(wallMaterial, false);
                    } else if (abs == CORRIDOR_HALF_WIDTH - 1
                            && (y == baseY + 1 || y == baseY + CORRIDOR_INTERIOR_HEIGHT - 1 || arch)) {
                        world.getBlockAt(x, y, z).setType(trimMaterial, false);
                    } else {
                        world.getBlockAt(x, y, z).setType(Material.AIR, false);
                    }
                }
            }
            for (int x = centerX - CORRIDOR_HALF_WIDTH; x <= centerX + CORRIDOR_HALF_WIDTH; x++) {
                world.getBlockAt(x, roofY, z).setType(roofMaterial, false);
            }
            if (arch) {
                for (int x = centerX - CORRIDOR_HALF_WIDTH + 1; x <= centerX + CORRIDOR_HALF_WIDTH - 1; x++) {
                    world.getBlockAt(x, baseY + CORRIDOR_INTERIOR_HEIGHT, z).setType(trimMaterial, false);
                }
                world.getBlockAt(centerX - CORRIDOR_HALF_WIDTH, baseY + CORRIDOR_INTERIOR_HEIGHT, z).setType(trimMaterial, false);
                world.getBlockAt(centerX + CORRIDOR_HALF_WIDTH, baseY + CORRIDOR_INTERIOR_HEIGHT, z).setType(trimMaterial, false);
            }
            if ((z - minZ) % CORRIDOR_LIGHT_SPACING == 0) {
                placeLight(world, centerX, roofY, z, lightMaterial);
            }
        }
    }

    private static void carveXCorridor(World world, int baseY, int centerZ, int startX, int endX, DungeonTheme theme) {
        int minX = Math.min(startX, endX);
        int maxX = Math.max(startX, endX);
        Material roofMaterial = theme.corridorRoof();
        Material floorMaterial = theme.corridorFloor();
        Material wallMaterial = theme.corridorWall();
        Material trimMaterial = theme.corridorTrim();
        Material lightMaterial = theme.lightMaterial();
        int roofY = baseY + CORRIDOR_INTERIOR_HEIGHT + 1;
        for (int x = minX; x <= maxX; x++) {
            boolean arch = (x - minX) % CORRIDOR_ARCH_SPACING == 0;
            boolean striped = ((x - minX) & 1) == 0;
            for (int z = centerZ - CORRIDOR_HALF_WIDTH; z <= centerZ + CORRIDOR_HALF_WIDTH; z++) {
                int abs = Math.abs(z - centerZ);
                Material floorType = (striped && abs == CORRIDOR_HALF_WIDTH - 1) ? trimMaterial : floorMaterial;
                world.getBlockAt(x, baseY, z).setType(floorType, false);
            }
            for (int y = baseY + 1; y <= baseY + CORRIDOR_INTERIOR_HEIGHT; y++) {
                for (int z = centerZ - CORRIDOR_HALF_WIDTH; z <= centerZ + CORRIDOR_HALF_WIDTH; z++) {
                    int abs = Math.abs(z - centerZ);
                    if (abs == CORRIDOR_HALF_WIDTH) {
                        world.getBlockAt(x, y, z).setType(wallMaterial, false);
                    } else if (abs == CORRIDOR_HALF_WIDTH - 1
                            && (y == baseY + 1 || y == baseY + CORRIDOR_INTERIOR_HEIGHT - 1 || arch)) {
                        world.getBlockAt(x, y, z).setType(trimMaterial, false);
                    } else {
                        world.getBlockAt(x, y, z).setType(Material.AIR, false);
                    }
                }
            }
            for (int z = centerZ - CORRIDOR_HALF_WIDTH; z <= centerZ + CORRIDOR_HALF_WIDTH; z++) {
                world.getBlockAt(x, roofY, z).setType(roofMaterial, false);
            }
            if (arch) {
                for (int z = centerZ - CORRIDOR_HALF_WIDTH + 1; z <= centerZ + CORRIDOR_HALF_WIDTH - 1; z++) {
                    world.getBlockAt(x, baseY + CORRIDOR_INTERIOR_HEIGHT, z).setType(trimMaterial, false);
                }
                world.getBlockAt(x, baseY + CORRIDOR_INTERIOR_HEIGHT, centerZ - CORRIDOR_HALF_WIDTH).setType(trimMaterial, false);
                world.getBlockAt(x, baseY + CORRIDOR_INTERIOR_HEIGHT, centerZ + CORRIDOR_HALF_WIDTH).setType(trimMaterial, false);
            }
            if ((x - minX) % CORRIDOR_LIGHT_SPACING == 0) {
                placeLight(world, x, roofY, centerZ, lightMaterial);
            }
        }
    }

    private static List<Location> placeLockedGate(
            World world,
            int baseY,
            GridPoint from,
            GridPoint to,
            int radius,
            CorridorDirection direction,
            DungeonTheme theme
    ) {
        return switch (direction) {
            case NORTH -> placeZGate(world, baseY, from.x(), (from.z() - radius + to.z() + radius) / 2, theme);
            case SOUTH -> placeZGate(world, baseY, from.x(), (from.z() + radius + to.z() - radius) / 2, theme);
            case EAST -> placeXGate(world, baseY, (from.x() + radius + to.x() - radius) / 2, from.z(), theme);
            case WEST -> placeXGate(world, baseY, (from.x() - radius + to.x() + radius) / 2, from.z(), theme);
        };
    }

    private static List<Location> placeZGate(World world, int baseY, int centerX, int gateZ, DungeonTheme theme) {
        List<Location> barrier = new ArrayList<>(28);
        int topY = baseY + CORRIDOR_INTERIOR_HEIGHT;
        for (int x = centerX - GATE_HALF_WIDTH - 1; x <= centerX + GATE_HALF_WIDTH + 1; x++) {
            for (int y = baseY + 1; y <= topY + 1; y++) {
                boolean frame = x == centerX - GATE_HALF_WIDTH - 1
                        || x == centerX + GATE_HALF_WIDTH + 1
                        || y == baseY + 1
                        || y == topY + 1;
                if (frame) {
                    world.getBlockAt(x, y, gateZ).setType(theme.gateFrame(), false);
                }
            }
        }

        for (int x = centerX - GATE_HALF_WIDTH; x <= centerX + GATE_HALF_WIDTH; x++) {
            for (int y = baseY + 1; y <= topY; y++) {
                Block block = world.getBlockAt(x, y, gateZ);
                block.setType(theme.gateBarrier(), false);
                barrier.add(block.getLocation());
            }
        }
        world.getBlockAt(centerX, baseY + 2 + (CORRIDOR_INTERIOR_HEIGHT / 2), gateZ).setType(theme.gateRune(), false);
        world.getBlockAt(centerX, topY + 2, gateZ).setType(theme.gateLantern(), false);
        world.getBlockAt(centerX, topY + 1, gateZ).setType(solidAccentMaterial(), false);
        return barrier;
    }

    private static List<Location> placeXGate(World world, int baseY, int gateX, int centerZ, DungeonTheme theme) {
        List<Location> barrier = new ArrayList<>(28);
        int topY = baseY + CORRIDOR_INTERIOR_HEIGHT;
        for (int z = centerZ - GATE_HALF_WIDTH - 1; z <= centerZ + GATE_HALF_WIDTH + 1; z++) {
            for (int y = baseY + 1; y <= topY + 1; y++) {
                boolean frame = z == centerZ - GATE_HALF_WIDTH - 1
                        || z == centerZ + GATE_HALF_WIDTH + 1
                        || y == baseY + 1
                        || y == topY + 1;
                if (frame) {
                    world.getBlockAt(gateX, y, z).setType(theme.gateFrame(), false);
                }
            }
        }

        for (int z = centerZ - GATE_HALF_WIDTH; z <= centerZ + GATE_HALF_WIDTH; z++) {
            for (int y = baseY + 1; y <= topY; y++) {
                Block block = world.getBlockAt(gateX, y, z);
                block.setType(theme.gateBarrier(), false);
                barrier.add(block.getLocation());
            }
        }
        world.getBlockAt(gateX, baseY + 2 + (CORRIDOR_INTERIOR_HEIGHT / 2), centerZ).setType(theme.gateRune(), false);
        world.getBlockAt(gateX, topY + 2, centerZ).setType(theme.gateLantern(), false);
        world.getBlockAt(gateX, topY + 1, centerZ).setType(solidAccentMaterial(), false);
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
        carveCeilingMedallion(world, centerX, centerZ, wallTop - 1, radius, wallMaterial);
    }

    private static void addRoomPerimeterFraming(
            World world,
            int centerX,
            int baseY,
            int centerZ,
            int radius,
            int wallTop,
            Material wallMaterial
    ) {
        int minX = centerX - radius;
        int maxX = centerX + radius;
        int minZ = centerZ - radius;
        int maxZ = centerZ + radius;
        Material trim = detailAccentFor(wallMaterial);
        int galleryY = baseY + 4;
        int crownY = wallTop - 2;
        java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();

        for (int x = minX; x <= maxX; x++) {
            world.getBlockAt(x, baseY + 1, minZ).setType(trim, false);
            world.getBlockAt(x, baseY + 1, maxZ).setType(trim, false);
            
            // Add moss/cracked variety
            if (random.nextDouble() < 0.2) {
                world.getBlockAt(x, baseY + 2, minZ).setType(Material.MOSSY_STONE_BRICK_WALL, false);
                world.getBlockAt(x, baseY + 2, maxZ).setType(Material.MOSSY_STONE_BRICK_WALL, false);
            }

            world.getBlockAt(x, wallTop - 1, minZ).setType(trim, false);
            world.getBlockAt(x, wallTop - 1, maxZ).setType(trim, false);
        }
        for (int z = minZ; z <= maxZ; z++) {
            world.getBlockAt(minX, baseY + 1, z).setType(trim, false);
            world.getBlockAt(maxX, baseY + 1, z).setType(trim, false);
            world.getBlockAt(minX, wallTop - 1, z).setType(trim, false);
            world.getBlockAt(maxX, wallTop - 1, z).setType(trim, false);
        }

        // Add hanging chains and decorations
        for (int i = 0; i < 4; i++) {
            int cx = centerX + random.nextInt(-radius + 1, radius);
            int cz = centerZ + random.nextInt(-radius + 1, radius);
            int chainLen = random.nextInt(2, 5);
            for (int y = 0; y < chainLen; y++) {
                world.getBlockAt(cx, wallTop - 1 - y, cz).setType(Material.CHAIN, false);
            }
            if (random.nextBoolean()) {
                world.getBlockAt(cx, wallTop - 1 - chainLen, cz).setType(Material.LANTERN, false);
            }
        }

        for (int x = minX + 2; x <= maxX - 2; x += 3) {
            world.getBlockAt(x, galleryY, minZ).setType(trim, false);
            world.getBlockAt(x, galleryY, maxZ).setType(trim, false);
            world.getBlockAt(x, crownY, minZ).setType(trim, false);
            world.getBlockAt(x, crownY, maxZ).setType(trim, false);
        }
        for (int z = minZ + 2; z <= maxZ - 2; z += 3) {
            world.getBlockAt(minX, galleryY, z).setType(trim, false);
            world.getBlockAt(maxX, galleryY, z).setType(trim, false);
            world.getBlockAt(minX, crownY, z).setType(trim, false);
            world.getBlockAt(maxX, crownY, z).setType(trim, false);
        }

        placePillar(world, minX, baseY + 1, minZ, ROOM_INTERIOR_HEIGHT - 1, trim);
        placePillar(world, minX, baseY + 1, maxZ, ROOM_INTERIOR_HEIGHT - 1, trim);
        placePillar(world, maxX, baseY + 1, minZ, ROOM_INTERIOR_HEIGHT - 1, trim);
        placePillar(world, maxX, baseY + 1, maxZ, ROOM_INTERIOR_HEIGHT - 1, trim);

        for (int offset = -radius + 2; offset <= radius - 2; offset += 4) {
            world.getBlockAt(centerX + offset, baseY + 2, minZ).setType(trim, false);
            world.getBlockAt(centerX + offset, baseY + 2, maxZ).setType(trim, false);
            world.getBlockAt(minX, baseY + 2, centerZ + offset).setType(trim, false);
            world.getBlockAt(maxX, baseY + 2, centerZ + offset).setType(trim, false);
        }
    }

    private static void carvePerspectiveBays(
            World world,
            int centerX,
            int baseY,
            int centerZ,
            int radius,
            int wallTop,
            Material floorMaterial,
            Material wallMaterial,
            Material roofMaterial
    ) {
        int bayOffset = Math.max(4, radius - 6);
        int[] offsets = {-bayOffset, bayOffset};
        for (int offset : offsets) {
            carvePerspectiveBay(world, centerX + offset, baseY, centerZ - radius, wallTop, floorMaterial, wallMaterial, roofMaterial, CorridorDirection.NORTH);
            carvePerspectiveBay(world, centerX + offset, baseY, centerZ + radius, wallTop, floorMaterial, wallMaterial, roofMaterial, CorridorDirection.SOUTH);
            carvePerspectiveBay(world, centerX - radius, baseY, centerZ + offset, wallTop, floorMaterial, wallMaterial, roofMaterial, CorridorDirection.WEST);
            carvePerspectiveBay(world, centerX + radius, baseY, centerZ + offset, wallTop, floorMaterial, wallMaterial, roofMaterial, CorridorDirection.EAST);
        }
    }

    private static void carvePerspectiveBay(
            World world,
            int anchorX,
            int baseY,
            int anchorZ,
            int wallTop,
            Material floorMaterial,
            Material wallMaterial,
            Material roofMaterial,
            CorridorDirection direction
    ) {
        Material trim = detailAccentFor(wallMaterial);
        Material floorAccent = detailSecondaryAccentFor(floorMaterial);
        Material lantern = nicheLanternFor(wallMaterial);
        int lateralStepX = direction == CorridorDirection.NORTH || direction == CorridorDirection.SOUTH ? 1 : 0;
        int lateralStepZ = direction == CorridorDirection.NORTH || direction == CorridorDirection.SOUTH ? 0 : 1;

        for (int depth = 0; depth <= ROOM_PERSPECTIVE_BAY_DEPTH; depth++) {
            for (int lateral = -ROOM_PERSPECTIVE_BAY_HALF_WIDTH; lateral <= ROOM_PERSPECTIVE_BAY_HALF_WIDTH; lateral++) {
                int x = anchorX + (direction.stepX() * depth) + (lateralStepX * lateral);
                int z = anchorZ + (direction.stepZ() * depth) + (lateralStepZ * lateral);
                boolean backWall = depth == ROOM_PERSPECTIVE_BAY_DEPTH;
                boolean sideWall = depth > 0 && Math.abs(lateral) == ROOM_PERSPECTIVE_BAY_HALF_WIDTH;

                world.getBlockAt(x, baseY, z).setType(depth == ROOM_PERSPECTIVE_BAY_DEPTH ? floorAccent : floorMaterial, false);
                for (int y = baseY + 1; y <= wallTop - 1; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (backWall) {
                        if (lateral == 0 && y == baseY + 2) {
                            block.setType(lantern, false);
                        } else if (lateral == 0 && y == baseY + 4) {
                            block.setType(solidAccentMaterial(), false);
                        } else if (y == baseY + 1 || y == wallTop - 2 || Math.abs(lateral) == ROOM_PERSPECTIVE_BAY_HALF_WIDTH) {
                            block.setType(trim, false);
                        } else {
                            block.setType(wallMaterial, false);
                        }
                    } else if (sideWall) {
                        block.setType(y == baseY + 3 ? trim : wallMaterial, false);
                    } else {
                        block.setType(Material.AIR, false);
                    }
                }
                world.getBlockAt(x, wallTop, z).setType(roofMaterial, false);
            }
        }
    }

    private static void carveVaultedCeiling(
            World world,
            int centerX,
            int centerZ,
            int radius,
            int wallTop,
            Material wallMaterial,
            Material roofMaterial
    ) {
        int inset = Math.max(3, radius - 4);
        Material trim = detailAccentFor(wallMaterial);
        for (int x = centerX - inset; x <= centerX + inset; x++) {
            for (int z = centerZ - inset; z <= centerZ + inset; z++) {
                boolean raisedEdge = Math.abs(x - centerX) == inset || Math.abs(z - centerZ) == inset;
                world.getBlockAt(x, wallTop, z).setType(raisedEdge ? trim : Material.AIR, false);
                world.getBlockAt(x, wallTop + 1, z).setType(roofMaterial, false);
            }
        }

        placeLight(world, centerX, wallTop + 1, centerZ);
        placeLight(world, centerX - inset + 1, wallTop + 1, centerZ);
        placeLight(world, centerX + inset - 1, wallTop + 1, centerZ);
        placeLight(world, centerX, wallTop + 1, centerZ - inset + 1);
        placeLight(world, centerX, wallTop + 1, centerZ + inset - 1);
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
        Material lantern = nicheLanternFor(wallMaterial);
        world.getBlockAt(x, baseY + 1, z).setType(detail, false);
        world.getBlockAt(x, baseY + 3, z).setType(detail, false);
        world.getBlockAt(x, baseY + 4, z).setType(solidAccentMaterial(), false);
        world.getBlockAt(x, baseY + 2, z).setType(lantern, false);
    }

    private static Material nicheLanternFor(Material wallMaterial) {
        return switch (wallMaterial) {
            case BLACKSTONE,
                    POLISHED_BLACKSTONE,
                    POLISHED_BLACKSTONE_BRICKS,
                    CHISELED_POLISHED_BLACKSTONE,
                    CRACKED_POLISHED_BLACKSTONE_BRICKS,
                    DEEPSLATE_BRICKS,
                    DEEPSLATE_TILES,
                    CHISELED_DEEPSLATE,
                    REINFORCED_DEEPSLATE,
                    OBSIDIAN,
                    CRYING_OBSIDIAN,
                    NETHER_BRICKS -> Material.SOUL_LANTERN;
            default -> Material.LANTERN;
        };
    }

    private static void addHangingLanternCluster(World world, int centerX, int baseY, int centerZ, int offset) {
        hangLantern(world, centerX, baseY, centerZ);
        hangLantern(world, centerX - offset, baseY, centerZ);
        hangLantern(world, centerX + offset, baseY, centerZ);
        hangLantern(world, centerX, baseY, centerZ - offset);
        hangLantern(world, centerX, baseY, centerZ + offset);
    }

    private static void carveCeilingMedallion(
            World world,
            int centerX,
            int centerZ,
            int y,
            int radius,
            Material wallMaterial
    ) {
        Material trim = detailAccentFor(wallMaterial);
        int arm = Math.max(3, radius - 5);
        for (int offset = -arm; offset <= arm; offset++) {
            world.getBlockAt(centerX + offset, y, centerZ).setType(trim, false);
            world.getBlockAt(centerX, y, centerZ + offset).setType(trim, false);
            if (Math.abs(offset) <= arm - 1 && (Math.abs(offset) % 2 == 0)) {
                world.getBlockAt(centerX + offset, y, centerZ + offset).setType(trim, false);
                world.getBlockAt(centerX + offset, y, centerZ - offset).setType(trim, false);
            }
        }
    }

    private static void hangLantern(World world, int x, int baseY, int z) {
        hangLantern(world, x, baseY, z, Material.LANTERN);
    }

    private static void hangLantern(World world, int x, int baseY, int z, Material lanternMaterial) {
        world.getBlockAt(x, baseY + ROOM_INTERIOR_HEIGHT, z).setType(solidAccentMaterial(), false);
        world.getBlockAt(x, baseY + ROOM_INTERIOR_HEIGHT - 1, z).setType(lanternMaterial, false);
    }

    private static void placeLight(World world, int x, int y, int z) {
        placeLight(world, x, y, z, Material.GLOWSTONE);
    }

    private static void placeLight(World world, int x, int y, int z, Material lightMaterial) {
        world.getBlockAt(x, y, z).setType(lightMaterial, false);
    }

    private static Material floorMaterialFor(RoomType type, FloorConfig floor) {
        int tier = floorTier(floor);
        return switch (tier) {
            case 1 -> switch (type) {
                case COMBAT -> Material.STONE_BRICKS;
                case PUZZLE_SEQUENCE -> Material.SMOOTH_STONE;
                case PUZZLE_SYNC -> Material.MOSSY_STONE_BRICKS;
                case PUZZLE_CHIME -> Material.SMOOTH_STONE;
                case PUZZLE_SEAL -> Material.POLISHED_ANDESITE;
                case TREASURE -> Material.CRACKED_STONE_BRICKS;
            };
            case 2 -> switch (type) {
                case COMBAT -> Material.BLACKSTONE;
                case PUZZLE_SEQUENCE -> Material.POLISHED_BLACKSTONE_BRICKS;
                case PUZZLE_SYNC -> Material.POLISHED_BLACKSTONE_BRICKS;
                case PUZZLE_CHIME -> Material.CHISELED_POLISHED_BLACKSTONE;
                case PUZZLE_SEAL -> Material.POLISHED_BLACKSTONE;
                case TREASURE -> Material.GILDED_BLACKSTONE;
            };
            case 3 -> switch (type) {
                case COMBAT -> Material.POLISHED_DEEPSLATE;
                case PUZZLE_SEQUENCE -> Material.COBBLED_DEEPSLATE;
                case PUZZLE_SYNC -> Material.DEEPSLATE_TILES;
                case PUZZLE_CHIME -> Material.CHISELED_DEEPSLATE;
                case PUZZLE_SEAL -> Material.DEEPSLATE_BRICKS;
                case TREASURE -> Material.REINFORCED_DEEPSLATE;
            };
            default -> switch (type) {
                case COMBAT -> Material.POLISHED_BLACKSTONE;
                case PUZZLE_SEQUENCE -> Material.NETHER_BRICKS;
                case PUZZLE_SYNC -> Material.REINFORCED_DEEPSLATE;
                case PUZZLE_CHIME -> Material.BLACKSTONE;
                case PUZZLE_SEAL -> Material.OBSIDIAN;
                case TREASURE -> Material.GILDED_BLACKSTONE;
            };
        };
    }

    private static Material wallMaterialFor(RoomType type, FloorConfig floor) {
        int tier = floorTier(floor);
        return switch (tier) {
            case 1 -> switch (type) {
                case COMBAT -> Material.MOSSY_STONE_BRICKS;
                case PUZZLE_SEQUENCE -> Material.STONE_BRICKS;
                case PUZZLE_SYNC -> Material.MOSSY_STONE_BRICKS;
                case PUZZLE_CHIME -> Material.CRACKED_STONE_BRICKS;
                case PUZZLE_SEAL -> Material.CHISELED_STONE_BRICKS;
                case TREASURE -> Material.STONE_BRICKS;
            };
            case 2 -> switch (type) {
                case COMBAT -> Material.POLISHED_BLACKSTONE_BRICKS;
                case PUZZLE_SEQUENCE -> Material.CRACKED_POLISHED_BLACKSTONE_BRICKS;
                case PUZZLE_SYNC -> Material.CHISELED_POLISHED_BLACKSTONE;
                case PUZZLE_CHIME -> Material.BLACKSTONE;
                case PUZZLE_SEAL -> Material.CRYING_OBSIDIAN;
                case TREASURE -> Material.POLISHED_BLACKSTONE_BRICKS;
            };
            case 3 -> switch (type) {
                case COMBAT -> Material.DEEPSLATE_BRICKS;
                case PUZZLE_SEQUENCE -> Material.CRACKED_DEEPSLATE_BRICKS;
                case PUZZLE_SYNC -> Material.CHISELED_DEEPSLATE;
                case PUZZLE_CHIME -> Material.DEEPSLATE_TILES;
                case PUZZLE_SEAL -> Material.REINFORCED_DEEPSLATE;
                case TREASURE -> Material.DEEPSLATE_BRICKS;
            };
            default -> switch (type) {
                case COMBAT -> Material.POLISHED_BLACKSTONE_BRICKS;
                case PUZZLE_SEQUENCE -> Material.NETHER_BRICKS;
                case PUZZLE_SYNC -> Material.REINFORCED_DEEPSLATE;
                case PUZZLE_CHIME -> Material.OBSIDIAN;
                case PUZZLE_SEAL -> Material.CRYING_OBSIDIAN;
                case TREASURE -> Material.BLACKSTONE;
            };
        };
    }

    private static Material supportMaterialFor(RoomType type, FloorConfig floor) {
        return switch (floorTier(floor)) {
            case 1 -> switch (type) {
                case COMBAT, PUZZLE_SYNC, PUZZLE_SEAL -> Material.STONE_BRICKS;
                case PUZZLE_SEQUENCE -> Material.CHISELED_STONE_BRICKS;
                case PUZZLE_CHIME -> Material.MOSSY_STONE_BRICKS;
                case TREASURE -> Material.CRACKED_STONE_BRICKS;
            };
            case 2 -> switch (type) {
                case COMBAT, PUZZLE_SYNC, PUZZLE_SEAL -> Material.POLISHED_BLACKSTONE_BRICKS;
                case PUZZLE_SEQUENCE -> Material.CRACKED_POLISHED_BLACKSTONE_BRICKS;
                case PUZZLE_CHIME -> Material.CHISELED_POLISHED_BLACKSTONE;
                case TREASURE -> Material.GILDED_BLACKSTONE;
            };
            case 3 -> switch (type) {
                case COMBAT, PUZZLE_CHIME, TREASURE -> Material.DEEPSLATE_BRICKS;
                case PUZZLE_SEQUENCE -> Material.CRACKED_DEEPSLATE_BRICKS;
                case PUZZLE_SYNC, PUZZLE_SEAL -> Material.CHISELED_DEEPSLATE;
            };
            default -> switch (type) {
                case COMBAT, PUZZLE_SYNC, TREASURE -> Material.OBSIDIAN;
                case PUZZLE_SEQUENCE -> Material.NETHER_BRICKS;
                case PUZZLE_CHIME -> Material.CRYING_OBSIDIAN;
                case PUZZLE_SEAL -> Material.REINFORCED_DEEPSLATE;
            };
        };
    }

    private static Material lanternMaterialForFloor(FloorConfig floor) {
        return floorTier(floor) <= 2 ? Material.LANTERN : Material.SOUL_LANTERN;
    }

    private static void decorateEncounterRoom(
            World world,
            int centerX,
            int baseY,
            int centerZ,
            int roomSize,
            RoomType type,
            FloorConfig floor
    ) {
        int radius = roomSize / 2;
        Material support = supportMaterialFor(type, floor);
        Material lanternMaterial = lanternMaterialForFloor(floor);
        int pillarOffset = encounterPillarOffset(roomSize);
        int pillarHeight = ROOM_INTERIOR_HEIGHT - 2;
        placePillar(world, centerX - pillarOffset, baseY + 1, centerZ - pillarOffset, pillarHeight, support);
        placePillar(world, centerX - pillarOffset, baseY + 1, centerZ + pillarOffset, pillarHeight, support);
        placePillar(world, centerX + pillarOffset, baseY + 1, centerZ - pillarOffset, pillarHeight, support);
        placePillar(world, centerX + pillarOffset, baseY + 1, centerZ + pillarOffset, pillarHeight, support);
        addEncounterCanopyFrame(world, centerX, baseY, centerZ, pillarOffset, support, lanternMaterial);
        addEncounterLanternRing(world, centerX, baseY, centerZ, radius, lanternMaterial);

        // Place 1-3 Secrets (Chests)
        java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();
        int secretCount = random.nextInt(1, 4);
        for (int i = 0; i < secretCount; i++) {
            int sx = centerX + random.nextInt(-radius + 2, radius - 1);
            int sz = centerZ + random.nextInt(-radius + 2, radius - 1);
            world.getBlockAt(sx, baseY + 1, sz).setType(SECRET_CHEST, false);
        }

        // 30% chance for a Blessing Altar
        if (random.nextDouble() < 0.3) {
            int ax = centerX + random.nextInt(-radius + 4, radius - 3);
            int az = centerZ + random.nextInt(-radius + 4, radius - 3);
            world.getBlockAt(ax, baseY + 1, az).setType(BLESSING_ALTAR, false);
            world.getBlockAt(ax, baseY, az).setType(Material.GOLD_BLOCK, false);
        }

        switch (type) {
            case COMBAT -> decorateCombatRoom(world, centerX, baseY, centerZ, radius, floor);
            case PUZZLE_SEQUENCE -> decorateSequenceRoom(world, centerX, baseY, centerZ, radius, floor);
            case PUZZLE_SYNC -> decorateSyncRoom(world, centerX, baseY, centerZ, radius, floor);
            case PUZZLE_CHIME -> decorateChimeRoom(world, centerX, baseY, centerZ, radius, floor);
            case PUZZLE_SEAL -> decorateSealRoom(world, centerX, baseY, centerZ, radius, floor);
            case TREASURE -> decorateTreasureVault(world, centerX, baseY, centerZ, radius, floor);
        }
    }

    private static void addEncounterLanternRing(
            World world,
            int centerX,
            int baseY,
            int centerZ,
            int radius,
            Material lanternMaterial
    ) {
        int offset = Math.max(3, radius - 4);
        hangLantern(world, centerX - offset, baseY, centerZ - offset, lanternMaterial);
        hangLantern(world, centerX - offset, baseY, centerZ + offset, lanternMaterial);
        hangLantern(world, centerX + offset, baseY, centerZ - offset, lanternMaterial);
        hangLantern(world, centerX + offset, baseY, centerZ + offset, lanternMaterial);
    }

    private static void addEncounterCanopyFrame(
            World world,
            int centerX,
            int baseY,
            int centerZ,
            int pillarOffset,
            Material supportMaterial,
            Material lanternMaterial
    ) {
        int beamY = baseY + ROOM_INTERIOR_HEIGHT - 2;
        for (int x = centerX - pillarOffset; x <= centerX + pillarOffset; x++) {
            world.getBlockAt(x, beamY, centerZ - pillarOffset).setType(supportMaterial, false);
            world.getBlockAt(x, beamY, centerZ + pillarOffset).setType(supportMaterial, false);
        }
        for (int z = centerZ - pillarOffset; z <= centerZ + pillarOffset; z++) {
            world.getBlockAt(centerX - pillarOffset, beamY, z).setType(supportMaterial, false);
            world.getBlockAt(centerX + pillarOffset, beamY, z).setType(supportMaterial, false);
        }
        for (int x = centerX - pillarOffset + 1; x <= centerX + pillarOffset - 1; x++) {
            world.getBlockAt(x, beamY, centerZ).setType(supportMaterial, false);
        }
        for (int z = centerZ - pillarOffset + 1; z <= centerZ + pillarOffset - 1; z++) {
            world.getBlockAt(centerX, beamY, z).setType(supportMaterial, false);
        }

        world.getBlockAt(centerX, beamY - 1, centerZ - pillarOffset).setType(lanternMaterial, false);
        world.getBlockAt(centerX, beamY - 1, centerZ + pillarOffset).setType(lanternMaterial, false);
        world.getBlockAt(centerX - pillarOffset, beamY - 1, centerZ).setType(lanternMaterial, false);
        world.getBlockAt(centerX + pillarOffset, beamY - 1, centerZ).setType(lanternMaterial, false);
    }

    private static void decorateCombatRoom(World world, int centerX, int baseY, int centerZ, int radius, FloorConfig floor) {
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

        int tier = floorTier(floor);
        Material combatRelic = switch (tier) {
            case 1 -> Material.CHISELED_STONE_BRICKS;
            case 2 -> Material.CHISELED_POLISHED_BLACKSTONE;
            case 3 -> Material.CHISELED_DEEPSLATE;
            default -> Material.REINFORCED_DEEPSLATE;
        };
        world.getBlockAt(centerX, baseY + 1, centerZ).setType(combatRelic, false);
        world.getBlockAt(centerX, baseY + 2, centerZ).setType(solidAccentMaterial(), false);
    }

    private static void decorateSequenceRoom(World world, int centerX, int baseY, int centerZ, int radius, FloorConfig floor) {
        int z = centerZ - 2;
        Material[] colors = {Material.RED_CONCRETE, Material.LIME_CONCRETE, Material.BLUE_CONCRETE, Material.YELLOW_CONCRETE};
        int[] xOffsets = {-3, -1, 1, 3};
        for (int index = 0; index < xOffsets.length; index++) {
            world.getBlockAt(centerX + xOffsets[index], baseY, z).setType(colors[index], false);
        }

        int legendZ = z - 2;
        for (int index = 0; index < xOffsets.length; index++) {
            world.getBlockAt(centerX + xOffsets[index], baseY + 1, legendZ).setType(colors[index], false);
        }

        int shelfX = centerX - radius + 1;
        int shelfZ = centerZ - radius + 1;
        Material shelf = switch (floorTier(floor)) {
            case 1 -> Material.BOOKSHELF;
            case 2 -> Material.CHISELED_BOOKSHELF;
            case 3 -> Material.CHISELED_BOOKSHELF;
            default -> Material.OBSIDIAN;
        };
        world.getBlockAt(shelfX, baseY + 1, shelfZ).setType(shelf, false);
        world.getBlockAt(centerX + radius - 1, baseY + 1, shelfZ).setType(shelf, false);
    }

    private static void decorateSyncRoom(World world, int centerX, int baseY, int centerZ, int radius, FloorConfig floor) {
        world.getBlockAt(centerX, baseY + 1, centerZ).setType(Material.BELL, false);
        int altarOffset = Math.max(5, radius - 3);
        Material pillarMaterial = switch (floorTier(floor)) {
            case 1 -> Material.STONE_BRICKS;
            case 2 -> Material.POLISHED_BLACKSTONE_BRICKS;
            case 3 -> Material.DEEPSLATE_BRICKS;
            default -> Material.OBSIDIAN;
        };
        placePillar(world, centerX - altarOffset, baseY + 1, centerZ - altarOffset, 3, pillarMaterial);
        placePillar(world, centerX - altarOffset, baseY + 1, centerZ + altarOffset, 3, pillarMaterial);
        placePillar(world, centerX + altarOffset, baseY + 1, centerZ - altarOffset, 3, pillarMaterial);
        placePillar(world, centerX + altarOffset, baseY + 1, centerZ + altarOffset, 3, pillarMaterial);
    }

    private static void decorateChimeRoom(World world, int centerX, int baseY, int centerZ, int radius, FloorConfig floor) {
        int chimeOffset = Math.max(3, radius - 4);
        int[][] offsets = {
                {0, -chimeOffset},
                {-chimeOffset, 0},
                {chimeOffset, 0},
                {0, chimeOffset}
        };
        Material pedestal = switch (floorTier(floor)) {
            case 1 -> Material.POLISHED_ANDESITE;
            case 2 -> Material.POLISHED_BLACKSTONE;
            case 3 -> Material.POLISHED_DEEPSLATE;
            default -> Material.BLACKSTONE;
        };
        Material chimeLantern = lanternMaterialForFloor(floor);

        for (int[] offset : offsets) {
            int x = centerX + offset[0];
            int z = centerZ + offset[1];
            world.getBlockAt(x, baseY, z).setType(pedestal, false);
            world.getBlockAt(x, baseY + 1, z).setType(Material.BELL, false);
            world.getBlockAt(x, baseY + 2, z).setType(solidAccentMaterial(), false);
            world.getBlockAt(x, baseY + 3, z).setType(chimeLantern, false);
        }

        world.getBlockAt(centerX, baseY + 1, centerZ).setType(Material.CHISELED_STONE_BRICKS, false);
    }

    private static void decorateSealRoom(World world, int centerX, int baseY, int centerZ, int radius, FloorConfig floor) {
        int sealOffset = Math.max(3, radius - 4);
        int[][] offsets = {
                {-sealOffset, -sealOffset},
                {sealOffset, -sealOffset},
                {-sealOffset, sealOffset},
                {sealOffset, sealOffset}
        };
        Material sealPedestal = switch (floorTier(floor)) {
            case 1 -> Material.POLISHED_BLACKSTONE;
            case 2 -> Material.CRYING_OBSIDIAN;
            case 3 -> Material.CHISELED_DEEPSLATE;
            default -> Material.REINFORCED_DEEPSLATE;
        };

        for (int[] offset : offsets) {
            int x = centerX + offset[0];
            int z = centerZ + offset[1];
            world.getBlockAt(x, baseY, z).setType(sealPedestal, false);
            world.getBlockAt(x, baseY + 1, z).setType(Material.LEVER, false);
            world.getBlockAt(x, baseY + 2, z).setType(solidAccentMaterial(), false);
            world.getBlockAt(x, baseY + 3, z).setType(lanternMaterialForFloor(floor), false);
        }
    }

    private static void decorateTreasureVault(World world, int centerX, int baseY, int centerZ, int radius, FloorConfig floor) {
        int tier = floorTier(floor);
        Material treasureBlock = switch (tier) {
            case 1 -> Material.GOLD_BLOCK;
            case 2 -> Material.GILDED_BLACKSTONE;
            case 3 -> Material.EMERALD_BLOCK;
            default -> Material.NETHERITE_BLOCK;
        };
        Material sideStorage = switch (tier) {
            case 1 -> Material.BARREL;
            case 2 -> Material.CHISELED_POLISHED_BLACKSTONE;
            case 3 -> Material.CHISELED_DEEPSLATE;
            default -> Material.BLACKSTONE;
        };
        world.getBlockAt(centerX, baseY + 1, centerZ - 1).setType(treasureBlock, false);
        world.getBlockAt(centerX, baseY + 1, centerZ + 1).setType(treasureBlock, false);

        int vaultX = centerX - radius + 1;
        int vaultZ = centerZ + radius - 1;
        world.getBlockAt(vaultX, baseY + 1, centerZ).setType(sideStorage, false);
        world.getBlockAt(centerX + radius - 1, baseY + 1, centerZ).setType(sideStorage, false);
        world.getBlockAt(centerX, baseY + 1, vaultZ).setType(detailAccentFor(wallMaterialFor(RoomType.TREASURE, floor)), false);
    }

    private static void decorateLobby(
            World world,
            int centerX,
            int baseY,
            int centerZ,
            int roomSize,
            CorridorDirection exitDirection,
            FloorConfig floor
    ) {
        int radius = roomSize / 2;
        int tier = floorTier(floor);
        Material archPillar = switch (tier) {
            case 1 -> Material.STONE_BRICKS;
            case 2 -> Material.POLISHED_BLACKSTONE_BRICKS;
            case 3 -> Material.DEEPSLATE_BRICKS;
            default -> Material.POLISHED_BLACKSTONE;
        };
        Material archBeam = switch (tier) {
            case 1 -> Material.CHISELED_STONE_BRICKS;
            case 2 -> Material.CHISELED_POLISHED_BLACKSTONE;
            case 3 -> Material.CHISELED_DEEPSLATE;
            default -> Material.REINFORCED_DEEPSLATE;
        };
        addGrandDoorArch(
                world,
                centerX,
                baseY,
                centerZ,
                radius,
                exitDirection,
                archPillar,
                archBeam,
                lanternMaterialForFloor(floor)
        );
        decorateLobbySanctum(world, centerX, baseY, centerZ, radius, floor);

        // Mort (Dungeon Master) Placeholder
        Location mortLoc = new Location(world, centerX + 0.5, baseY + 1, centerZ - 2.5);
        world.getBlockAt(mortLoc).setType(Material.LECTERN, false);
        
        // Ready Up Station
        world.getBlockAt(centerX, baseY + 1, centerZ - 4).setType(Material.STONE_BUTTON, false);
        world.getBlockAt(centerX, baseY + 1, centerZ - 5).setType(Material.OAK_SIGN, false);

        // Class Selection Altar
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (Math.abs(x) == 1 && Math.abs(z) == 1) {
                    world.getBlockAt(centerX + x, baseY + 1, centerZ + 2 + z).setType(Material.CHISELED_STONE_BRICKS, false);
                    world.getBlockAt(centerX + x, baseY + 2, centerZ + 2 + z).setType(Material.SOUL_LANTERN, false);
                }
            }
        }
        world.getBlockAt(centerX, baseY + 1, centerZ + 2).setType(Material.ENCHANTING_TABLE, false);
    }

    private static void decorateBossRoom(
            World world,
            int centerX,
            int baseY,
            int centerZ,
            int roomSize,
            CorridorDirection approachDirection,
            FloorConfig floor
    ) {
        int radius = roomSize / 2;
        CorridorDirection markerSide = approachDirection.opposite();
        int tier = floorTier(floor);
        Material archPillar = switch (tier) {
            case 1 -> Material.POLISHED_BLACKSTONE_BRICKS;
            case 2 -> Material.CRYING_OBSIDIAN;
            case 3 -> Material.DEEPSLATE_TILES;
            default -> Material.OBSIDIAN;
        };
        Material archBeam = switch (tier) {
            case 1 -> Material.CHISELED_POLISHED_BLACKSTONE;
            case 2 -> Material.GILDED_BLACKSTONE;
            case 3 -> Material.CHISELED_DEEPSLATE;
            default -> Material.REINFORCED_DEEPSLATE;
        };
        addGrandDoorArch(
                world,
                centerX,
                baseY,
                centerZ,
                radius,
                markerSide,
                archPillar,
                archBeam,
                Material.SOUL_LANTERN
        );
        decorateBossSanctum(world, centerX, baseY, centerZ, radius, floor);
        world.getBlockAt(centerX, baseY + 1, centerZ).setType(supportMaterialFor(RoomType.COMBAT, floor), false);
    }

    private static void addGrandDoorArch(
            World world,
            int centerX,
            int baseY,
            int centerZ,
            int radius,
            CorridorDirection side,
            Material pillarMaterial,
            Material beamMaterial,
            Material lanternMaterial
    ) {
        int crownY = baseY + ROOM_INTERIOR_HEIGHT;
        int lanternY = baseY + ROOM_INTERIOR_HEIGHT - 1;
        int pillarHeight = ROOM_INTERIOR_HEIGHT - 2;
        switch (side) {
            case NORTH -> {
                int z = centerZ - radius + 1;
                placePillar(world, centerX - 3, baseY + 1, z, pillarHeight, pillarMaterial);
                placePillar(world, centerX + 3, baseY + 1, z, pillarHeight, pillarMaterial);
                for (int x = centerX - 4; x <= centerX + 4; x++) {
                    world.getBlockAt(x, crownY, z).setType(beamMaterial, false);
                }
                world.getBlockAt(centerX - 3, lanternY, z).setType(lanternMaterial, false);
                world.getBlockAt(centerX + 3, lanternY, z).setType(lanternMaterial, false);
            }
            case SOUTH -> {
                int z = centerZ + radius - 1;
                placePillar(world, centerX - 3, baseY + 1, z, pillarHeight, pillarMaterial);
                placePillar(world, centerX + 3, baseY + 1, z, pillarHeight, pillarMaterial);
                for (int x = centerX - 4; x <= centerX + 4; x++) {
                    world.getBlockAt(x, crownY, z).setType(beamMaterial, false);
                }
                world.getBlockAt(centerX - 3, lanternY, z).setType(lanternMaterial, false);
                world.getBlockAt(centerX + 3, lanternY, z).setType(lanternMaterial, false);
            }
            case EAST -> {
                int x = centerX + radius - 1;
                placePillar(world, x, baseY + 1, centerZ - 3, pillarHeight, pillarMaterial);
                placePillar(world, x, baseY + 1, centerZ + 3, pillarHeight, pillarMaterial);
                for (int z = centerZ - 4; z <= centerZ + 4; z++) {
                    world.getBlockAt(x, crownY, z).setType(beamMaterial, false);
                }
                world.getBlockAt(x, lanternY, centerZ - 3).setType(lanternMaterial, false);
                world.getBlockAt(x, lanternY, centerZ + 3).setType(lanternMaterial, false);
            }
            case WEST -> {
                int x = centerX - radius + 1;
                placePillar(world, x, baseY + 1, centerZ - 3, pillarHeight, pillarMaterial);
                placePillar(world, x, baseY + 1, centerZ + 3, pillarHeight, pillarMaterial);
                for (int z = centerZ - 4; z <= centerZ + 4; z++) {
                    world.getBlockAt(x, crownY, z).setType(beamMaterial, false);
                }
                world.getBlockAt(x, lanternY, centerZ - 3).setType(lanternMaterial, false);
                world.getBlockAt(x, lanternY, centerZ + 3).setType(lanternMaterial, false);
            }
        }
    }

    private static void decorateLobbySanctum(
            World world,
            int centerX,
            int baseY,
            int centerZ,
            int radius,
            FloorConfig floor
    ) {
        int tier = floorTier(floor);
        Material crossMaterial = switch (tier) {
            case 1 -> Material.MOSSY_STONE_BRICKS;
            case 2 -> Material.POLISHED_BLACKSTONE_BRICKS;
            case 3 -> Material.DEEPSLATE_TILES;
            default -> Material.OBSIDIAN;
        };
        Material coreMaterial = switch (tier) {
            case 1 -> Material.CHISELED_STONE_BRICKS;
            case 2 -> Material.GILDED_BLACKSTONE;
            case 3 -> Material.CHISELED_DEEPSLATE;
            default -> Material.CRYING_OBSIDIAN;
        };
        int ring = Math.max(3, radius - 4);
        for (int offset = -ring; offset <= ring; offset++) {
            world.getBlockAt(centerX + offset, baseY, centerZ).setType(crossMaterial, false);
            world.getBlockAt(centerX, baseY, centerZ + offset).setType(crossMaterial, false);
        }
        for (int x = centerX - 1; x <= centerX + 1; x++) {
            for (int z = centerZ - 1; z <= centerZ + 1; z++) {
                boolean corner = Math.abs(x - centerX) == 1 && Math.abs(z - centerZ) == 1;
                world.getBlockAt(x, baseY, z).setType(corner ? crossMaterial : coreMaterial, false);
            }
        }
        addHangingLanternCluster(world, centerX, baseY, centerZ, Math.max(2, ring - 1));
    }

    private static void decorateBossSanctum(
            World world,
            int centerX,
            int baseY,
            int centerZ,
            int radius,
            FloorConfig floor
    ) {
        int tier = floorTier(floor);
        Material ringMaterial = switch (tier) {
            case 1 -> Material.MOSSY_STONE_BRICKS;
            case 2 -> Material.POLISHED_BLACKSTONE_BRICKS;
            case 3 -> Material.DEEPSLATE_TILES;
            default -> Material.OBSIDIAN;
        };
        Material centerEdge = switch (tier) {
            case 1 -> Material.CHISELED_STONE_BRICKS;
            case 2 -> Material.GILDED_BLACKSTONE;
            case 3 -> Material.CHISELED_DEEPSLATE;
            default -> Material.REINFORCED_DEEPSLATE;
        };
        Material centerCore = switch (tier) {
            case 1 -> Material.CRYING_OBSIDIAN;
            case 2 -> Material.CRYING_OBSIDIAN;
            case 3 -> Material.GILDED_BLACKSTONE;
            default -> Material.CRYING_OBSIDIAN;
        };
        Material brazierPillar = switch (tier) {
            case 1 -> Material.STONE_BRICKS;
            case 2 -> Material.CRYING_OBSIDIAN;
            case 3 -> Material.DEEPSLATE_BRICKS;
            default -> Material.OBSIDIAN;
        };
        Material lanternMaterial = tier == 1 ? Material.LANTERN : Material.SOUL_LANTERN;
        int ring = Math.max(3, radius - 4);
        for (int offset = -ring; offset <= ring; offset++) {
            world.getBlockAt(centerX + offset, baseY, centerZ - ring).setType(ringMaterial, false);
            world.getBlockAt(centerX + offset, baseY, centerZ + ring).setType(ringMaterial, false);
            world.getBlockAt(centerX - ring, baseY, centerZ + offset).setType(ringMaterial, false);
            world.getBlockAt(centerX + ring, baseY, centerZ + offset).setType(ringMaterial, false);
        }

        for (int x = centerX - 2; x <= centerX + 2; x++) {
            for (int z = centerZ - 2; z <= centerZ + 2; z++) {
                boolean edge = x == centerX - 2 || x == centerX + 2 || z == centerZ - 2 || z == centerZ + 2;
                world.getBlockAt(x, baseY, z).setType(edge ? centerEdge : centerCore, false);
            }
        }
        world.getBlockAt(centerX, baseY, centerZ).setType(centerCore, false);

        int brazierOffset = Math.max(4, radius - 3);
        placePillar(world, centerX - brazierOffset, baseY + 1, centerZ - brazierOffset, 3, brazierPillar);
        placePillar(world, centerX - brazierOffset, baseY + 1, centerZ + brazierOffset, 3, brazierPillar);
        placePillar(world, centerX + brazierOffset, baseY + 1, centerZ - brazierOffset, 3, brazierPillar);
        placePillar(world, centerX + brazierOffset, baseY + 1, centerZ + brazierOffset, 3, brazierPillar);

        hangLantern(world, centerX - brazierOffset, baseY, centerZ - brazierOffset, lanternMaterial);
        hangLantern(world, centerX - brazierOffset, baseY, centerZ + brazierOffset, lanternMaterial);
        hangLantern(world, centerX + brazierOffset, baseY, centerZ - brazierOffset, lanternMaterial);
        hangLantern(world, centerX + brazierOffset, baseY, centerZ + brazierOffset, lanternMaterial);
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
        Material trim = detailAccentFor(wallMaterial);
        Material floorAccent = detailSecondaryAccentFor(floorMaterial);
        Material lantern = nicheLanternFor(wallMaterial);

        for (int depth = 1; depth <= ROOM_WING_DEPTH; depth++) {
            for (int lateral = -ROOM_WING_HALF_WIDTH; lateral <= ROOM_WING_HALF_WIDTH; lateral++) {
                int x = centerX + (direction.stepX() * (radius + depth)) + (lateralStepX * lateral);
                int z = centerZ + (direction.stepZ() * (radius + depth)) + (lateralStepZ * lateral);

                boolean outerWall = depth == ROOM_WING_DEPTH;
                boolean sideWall = depth > 1 && Math.abs(lateral) == ROOM_WING_HALF_WIDTH;
                world.getBlockAt(x, baseY, z).setType(outerWall ? floorAccent : floorMaterial, false);

                for (int y = baseY + 1; y <= wallTop - 1; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (outerWall) {
                        if (lateral == 0 && y == baseY + 2) {
                            block.setType(lantern, false);
                        } else if (y == baseY + 1 || y == wallTop - 2 || (Math.abs(lateral) == ROOM_WING_HALF_WIDTH - 1 && y == baseY + 4)) {
                            block.setType(trim, false);
                        } else {
                            block.setType(wallMaterial, false);
                        }
                    } else if (sideWall) {
                        block.setType(y == baseY + 3 ? trim : wallMaterial, false);
                    } else {
                        block.setType(Material.AIR, false);
                    }
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
            case STONE_BRICKS -> Material.CHISELED_STONE_BRICKS;
            case MOSSY_STONE_BRICKS -> Material.STONE_BRICKS;
            case CRACKED_STONE_BRICKS -> Material.STONE_BRICKS;
            case CHISELED_STONE_BRICKS -> Material.STONE_BRICKS;
            case POLISHED_ANDESITE -> Material.CHISELED_STONE_BRICKS;
            case DARK_OAK_PLANKS -> Material.CHERRY_WOOD;
            case POLISHED_BLACKSTONE -> Material.CHISELED_POLISHED_BLACKSTONE;
            case POLISHED_BLACKSTONE_BRICKS -> Material.CHISELED_POLISHED_BLACKSTONE;
            case CRACKED_POLISHED_BLACKSTONE_BRICKS -> Material.POLISHED_BLACKSTONE_BRICKS;
            case CHERRY_WOOD -> Material.STRIPPED_CHERRY_WOOD;
            case CRIMSON_HYPHAE -> Material.CRIMSON_PLANKS;
            case BAMBOO_BLOCK -> Material.BAMBOO_MOSAIC;
            case CHISELED_POLISHED_BLACKSTONE -> Material.GILDED_BLACKSTONE;
            case BLACKSTONE -> Material.POLISHED_BLACKSTONE_BRICKS;
            case DEEPSLATE_BRICKS -> Material.CRACKED_DEEPSLATE_BRICKS;
            case CHISELED_DEEPSLATE -> Material.DEEPSLATE_TILES;
            case COBBLED_DEEPSLATE -> Material.POLISHED_DEEPSLATE;
            case DEEPSLATE_TILES -> Material.CHISELED_DEEPSLATE;
            case OBSIDIAN -> Material.CRYING_OBSIDIAN;
            case REINFORCED_DEEPSLATE -> Material.DEEPSLATE_TILES;
            case NETHER_BRICKS -> Material.RED_NETHER_BRICKS;
            case CRYING_OBSIDIAN -> Material.GILDED_BLACKSTONE;
            default -> Material.STONE_BRICKS;
        };
    }

    private static Material detailSecondaryAccentFor(Material floorMaterial) {
        return switch (floorMaterial) {
            case STONE_BRICKS -> Material.MOSSY_STONE_BRICKS;
            case MOSSY_STONE_BRICKS -> Material.CRACKED_STONE_BRICKS;
            case CRACKED_STONE_BRICKS -> Material.CHISELED_STONE_BRICKS;
            case CHISELED_STONE_BRICKS -> Material.STONE_BRICKS;
            case POLISHED_ANDESITE -> Material.COBBLESTONE;
            case SMOOTH_STONE -> Material.CHISELED_STONE_BRICKS;
            case BAMBOO_MOSAIC -> Material.BAMBOO_PLANKS;
            case CHERRY_PLANKS -> Material.STRIPPED_CHERRY_WOOD;
            case CRIMSON_PLANKS -> Material.NETHER_BRICKS;
            case POLISHED_BLACKSTONE -> Material.GILDED_BLACKSTONE;
            case POLISHED_BLACKSTONE_BRICKS -> Material.CHISELED_POLISHED_BLACKSTONE;
            case BLACKSTONE -> Material.POLISHED_BLACKSTONE_BRICKS;
            case POLISHED_DEEPSLATE -> Material.DEEPSLATE_BRICKS;
            case DEEPSLATE_BRICKS -> Material.CRACKED_DEEPSLATE_BRICKS;
            case TUFF_BRICKS -> Material.CHISELED_TUFF;
            case DEEPSLATE_TILES -> Material.CHISELED_DEEPSLATE;
            case NETHER_BRICKS -> Material.RED_NETHER_BRICKS;
            case REINFORCED_DEEPSLATE -> Material.CRYING_OBSIDIAN;
            case RED_TERRACOTTA -> Material.CUT_RED_SANDSTONE;
            default -> Material.STONE_BRICKS;
        };
    }

    private static Material slabFor(Material wallMaterial) {
        return switch (wallMaterial) {
            case STONE_BRICKS, MOSSY_STONE_BRICKS, CRACKED_STONE_BRICKS, CHISELED_STONE_BRICKS -> Material.STONE_BRICK_SLAB;
            case POLISHED_ANDESITE -> Material.ANDESITE_SLAB;
            case DARK_OAK_PLANKS -> Material.DARK_OAK_SLAB;
            case POLISHED_BLACKSTONE_BRICKS, CHISELED_POLISHED_BLACKSTONE, CRACKED_POLISHED_BLACKSTONE_BRICKS -> Material.POLISHED_BLACKSTONE_BRICK_SLAB;
            case CHERRY_WOOD -> Material.CHERRY_SLAB;
            case CRIMSON_HYPHAE -> Material.CRIMSON_SLAB;
            case BAMBOO_BLOCK -> Material.BAMBOO_SLAB;
            case BLACKSTONE -> Material.BLACKSTONE_SLAB;
            case POLISHED_BLACKSTONE -> Material.POLISHED_BLACKSTONE_SLAB;
            case OBSIDIAN -> Material.POLISHED_BLACKSTONE_SLAB;
            case NETHER_BRICKS -> Material.NETHER_BRICK_SLAB;
            case DEEPSLATE_BRICKS -> Material.DEEPSLATE_BRICK_SLAB;
            case DEEPSLATE_TILES, CHISELED_DEEPSLATE, COBBLED_DEEPSLATE, REINFORCED_DEEPSLATE -> Material.DEEPSLATE_TILE_SLAB;
            case TUFF_BRICKS -> Material.TUFF_BRICK_SLAB;
            default -> Material.STONE_BRICK_SLAB;
        };
    }

    private static int floorTier(FloorConfig floor) {
        String id = floor.id();
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            }
        }
        if (digits.isEmpty()) {
            return 1;
        }
        try {
            int parsed = Integer.parseInt(digits.toString());
            return Math.max(1, Math.min(4, parsed));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private static DungeonTheme themeForFloor(FloorConfig floor) {
        String floorId = floor.id();
        if ("F1".equalsIgnoreCase(floorId)) {
            return new DungeonTheme(
                    Material.STONE_BRICKS,
                    Material.MOSSY_STONE_BRICKS,
                    Material.CHISELED_STONE_BRICKS,
                    Material.STONE_BRICK_SLAB,
                    Material.GLOWSTONE,
                    Material.STONE_BRICKS,
                    solidAccentMaterial(),
                    Material.CHISELED_STONE_BRICKS,
                    Material.LANTERN
            );
        }
        if ("F2".equalsIgnoreCase(floorId)) {
            return new DungeonTheme(
                    Material.POLISHED_BLACKSTONE_BRICKS,
                    Material.POLISHED_BLACKSTONE,
                    Material.CHISELED_POLISHED_BLACKSTONE,
                    Material.POLISHED_BLACKSTONE_BRICK_SLAB,
                    Material.GLOWSTONE,
                    Material.POLISHED_BLACKSTONE_BRICKS,
                    solidAccentMaterial(),
                    Material.GILDED_BLACKSTONE,
                    Material.SOUL_LANTERN
            );
        }
        if ("F3".equalsIgnoreCase(floorId)) {
            return new DungeonTheme(
                    Material.POLISHED_DEEPSLATE,
                    Material.DEEPSLATE_BRICKS,
                    Material.CRACKED_DEEPSLATE_BRICKS,
                    Material.DEEPSLATE_BRICK_SLAB,
                    Material.SEA_LANTERN,
                    Material.DEEPSLATE_TILES,
                    solidAccentMaterial(),
                    Material.CHISELED_DEEPSLATE,
                    Material.SOUL_LANTERN
            );
        }
        if ("F4".equalsIgnoreCase(floorId)) {
            return new DungeonTheme(
                    Material.BLACKSTONE,
                    Material.POLISHED_BLACKSTONE_BRICKS,
                    Material.CHISELED_POLISHED_BLACKSTONE,
                    Material.POLISHED_BLACKSTONE_BRICK_SLAB,
                    Material.SEA_LANTERN,
                    Material.OBSIDIAN,
                    solidAccentMaterial(),
                    Material.CRYING_OBSIDIAN,
                    Material.SOUL_LANTERN
            );
        }
        return new DungeonTheme(
                Material.POLISHED_DEEPSLATE,
                floor.wallMaterial(),
                detailAccentFor(floor.wallMaterial()),
                slabFor(floor.wallMaterial()),
                Material.GLOWSTONE,
                floor.wallMaterial(),
                solidAccentMaterial(),
                Material.CRYING_OBSIDIAN,
                Material.SOUL_LANTERN
        );
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

    private record DungeonTheme(
            Material corridorFloor,
            Material corridorWall,
            Material corridorTrim,
            Material corridorRoof,
            Material lightMaterial,
            Material gateFrame,
            Material gateBarrier,
            Material gateRune,
            Material gateLantern
    ) {
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
