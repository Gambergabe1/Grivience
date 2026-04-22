package io.papermc.Grivience.dungeon;

import org.junit.jupiter.api.Test;
import java.util.List;

public class SpawnTest {
    @Test
    public void testCandidateColumns() {
        List<DungeonSpawnPlanner.SpawnColumn> cols = DungeonSpawnPlanner.candidateColumns(0, 0, 3, null, null);
        System.out.println("Candidate count: " + cols.size());
        for (DungeonSpawnPlanner.SpawnColumn col : cols) {
            System.out.println("Col: " + col.x() + ", " + col.z());
        }
        assert cols.size() > 0;
    }
}
