package io.papermc.Grivience.dungeon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class DungeonBuilderGeometryTest {
    @Test
    void spawnRadius_expandsBeyondLegacyInnerSquare() {
        // With ROOM_WING_DEPTH = 5: (23/2) - 3 + (5-1) = 11 - 3 + 4 = 12
        assertEquals(12, DungeonBuilder.spawnRadius(23));
        assertTrue(DungeonBuilder.spawnRadius(23) > ((23 / 2) - 3));
    }

    @Test
    void encounterPillarOffset_pullsColumnsInwardToOpenTheRoom() {
        assertEquals(8, DungeonBuilder.encounterPillarOffset(23));
    }
}
