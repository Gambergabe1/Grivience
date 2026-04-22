package io.papermc.Grivience.dungeon;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class DungeonSpawnPlanner {
    private static final int PREFERRED_SEARCH_RADIUS = 3;

    private DungeonSpawnPlanner() {
    }

    static List<SpawnColumn> candidateColumns(int centerX, int centerZ, int radius, Integer preferredX, Integer preferredZ) {
        int boundedRadius = Math.max(0, radius);
        Set<SpawnColumn> ordered = new LinkedHashSet<>();

        if (preferredX != null && preferredZ != null) {
            addSquareSearch(ordered, centerX, centerZ, boundedRadius, preferredX, preferredZ, PREFERRED_SEARCH_RADIUS);
        }

        for (int ring = boundedRadius; ring >= 0; ring--) {
            addSquareRing(ordered, centerX, centerZ, ring);
        }

        return List.copyOf(ordered);
    }

    private static void addSquareSearch(
            Set<SpawnColumn> ordered,
            int centerX,
            int centerZ,
            int roomRadius,
            int originX,
            int originZ,
            int searchRadius
    ) {
        for (int ring = 0; ring <= searchRadius; ring++) {
            addSquareRing(ordered, centerX, centerZ, roomRadius, originX, originZ, ring);
        }
    }

    private static void addSquareRing(Set<SpawnColumn> ordered, int centerX, int centerZ, int ring) {
        addSquareRing(ordered, centerX, centerZ, ring, centerX, centerZ, ring);
    }

    private static void addSquareRing(
            Set<SpawnColumn> ordered,
            int centerX,
            int centerZ,
            int roomRadius,
            int originX,
            int originZ,
            int ring
    ) {
        if (ring == 0) {
            addIfInside(ordered, centerX, centerZ, roomRadius, originX, originZ);
            return;
        }

        int minX = originX - ring;
        int maxX = originX + ring;
        int minZ = originZ - ring;
        int maxZ = originZ + ring;

        for (int x = minX; x <= maxX; x++) {
            addIfInside(ordered, centerX, centerZ, roomRadius, x, minZ);
        }
        for (int z = minZ + 1; z <= maxZ; z++) {
            addIfInside(ordered, centerX, centerZ, roomRadius, maxX, z);
        }
        for (int x = maxX - 1; x >= minX; x--) {
            addIfInside(ordered, centerX, centerZ, roomRadius, x, maxZ);
        }
        for (int z = maxZ - 1; z > minZ; z--) {
            addIfInside(ordered, centerX, centerZ, roomRadius, minX, z);
        }
    }

    private static void addIfInside(Set<SpawnColumn> ordered, int centerX, int centerZ, int roomRadius, int x, int z) {
        if (Math.abs(x - centerX) > roomRadius || Math.abs(z - centerZ) > roomRadius) {
            return;
        }
        ordered.add(new SpawnColumn(x, z));
    }

    record SpawnColumn(int x, int z) {
    }
}
