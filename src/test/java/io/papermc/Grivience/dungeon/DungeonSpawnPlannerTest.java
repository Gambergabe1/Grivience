package io.papermc.Grivience.dungeon;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class DungeonSpawnPlannerTest {
    @Test
    void candidateColumns_coverEntireUsableSquareWithoutDuplicates() {
        List<DungeonSpawnPlanner.SpawnColumn> columns = DungeonSpawnPlanner.candidateColumns(0, 0, 3, null, null);

        assertEquals(49, columns.size());
        assertEquals(49, new HashSet<>(columns).size());
        assertTrue(columns.stream().allMatch(column -> Math.abs(column.x()) <= 3 && Math.abs(column.z()) <= 3));
    }

    @Test
    void candidateColumns_prioritizePreferredColumnNeighborhoodBeforeCenterFallback() {
        List<DungeonSpawnPlanner.SpawnColumn> columns = DungeonSpawnPlanner.candidateColumns(0, 0, 6, 4, 0);

        assertEquals(new DungeonSpawnPlanner.SpawnColumn(4, 0), columns.getFirst());
        int localNeighborIndex = columns.indexOf(new DungeonSpawnPlanner.SpawnColumn(4, 1));
        int centerIndex = columns.indexOf(new DungeonSpawnPlanner.SpawnColumn(0, 0));

        assertTrue(localNeighborIndex > 0);
        assertTrue(centerIndex > localNeighborIndex);
    }
}
