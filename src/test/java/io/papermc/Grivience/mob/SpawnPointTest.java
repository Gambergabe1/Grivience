package io.papermc.Grivience.mob;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

final class SpawnPointTest {

    @Test
    void newSpawnPointsDefaultToTenMobs() {
        SpawnPoint point = new SpawnPoint(new Location(mock(World.class), 0.0D, 70.0D, 0.0D), "zombie");

        assertEquals(SpawnPoint.FIXED_MAX_NEARBY_ENTITIES, point.getMaxNearbyEntities());
    }

    @Test
    void loadedSpawnPointsNormalizeLegacyCapsToTenMobs() {
        Map<String, Object> data = new HashMap<>();
        data.put("id", UUID.randomUUID().toString());
        data.put("monsterId", "zombie");
        data.put("spawnRadius", 4);
        data.put("spawnDelay", 100);
        data.put("maxNearbyEntities", 6);
        data.put("active", true);
        data.put("world", "world");
        data.put("x", 0.0D);
        data.put("y", 70.0D);
        data.put("z", 0.0D);

        SpawnPoint point = new SpawnPoint(data);

        assertEquals(SpawnPoint.FIXED_MAX_NEARBY_ENTITIES, point.getMaxNearbyEntities());
    }

    @Test
    void loadedSpawnPointsKeepWorldNameEvenWhenWorldIsUnavailable() {
        Map<String, Object> data = new HashMap<>();
        data.put("id", UUID.randomUUID().toString());
        data.put("monsterId", "sea_guardian");
        data.put("world", "missing_world");
        data.put("x", 12.5D);
        data.put("y", 64.0D);
        data.put("z", -4.5D);

        SpawnPoint point = new SpawnPoint(data);

        assertEquals("missing_world", point.getWorldName());
        assertNull(point.getLocation().getWorld());
        assertFalse(point.isLocationValid());
        assertEquals(12.5D, point.getLocation().getX());
    }

    @Test
    void refillOnlyTriggersAtTwoOrFewerActiveMobs() {
        SpawnPoint point = new SpawnPoint(new Location(mock(World.class), 0.0D, 70.0D, 0.0D), "zombie");

        assertTrue(point.shouldRefill(0));
        assertTrue(point.shouldRefill(2));
        assertFalse(point.shouldRefill(3));
        assertFalse(point.shouldRefill(SpawnPoint.FIXED_MAX_NEARBY_ENTITIES));
    }
}
