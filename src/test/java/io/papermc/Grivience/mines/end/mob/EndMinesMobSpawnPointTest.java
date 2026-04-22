package io.papermc.Grivience.mines.end.mob;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class EndMinesMobSpawnPointTest {

    @Test
    void setMaxNearbyEntitiesClampsToSafetyLimit() {
        EndMinesMobSpawnPoint point = new EndMinesMobSpawnPoint(UUID.randomUUID());

        point.setMaxNearbyEntities(500);
        assertEquals(EndMinesMobSpawnPoint.MAX_NEARBY_ENTITIES_LIMIT, point.maxNearbyEntities());

        point.setMaxNearbyEntities(-5);
        assertEquals(0, point.maxNearbyEntities());
    }

    @Test
    void loadClampsLegacySpawnPointCaps() {
        YamlConfiguration section = new YamlConfiguration();
        section.set("max-nearby-entities", 250);

        EndMinesMobSpawnPoint point = EndMinesMobSpawnPoint.load(UUID.randomUUID(), section);

        assertEquals(EndMinesMobSpawnPoint.MAX_NEARBY_ENTITIES_LIMIT, point.maxNearbyEntities());
    }
}
