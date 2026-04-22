package io.papermc.Grivience.mob;

import org.bukkit.entity.EntityType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomMonsterConfigTest {

    @Test
    void landOfGhoulsMobRosterExistsInConfig() {
        YamlConfiguration config = loadConfig();
        List<String> ghoulMobIds = List.of(
                "grave_shambler",
                "crypt_husk",
                "plague_surgeon",
                "drowned_revenant",
                "ghoul_overseer"
        );

        for (String mobId : ghoulMobIds) {
            String base = "custom-monsters.monsters." + mobId;
            assertTrue(config.contains(base), "Missing Land Of Ghouls mob config: " + mobId);
            assertTrue(config.contains(base + ".entity-type"), "Missing entity type for " + mobId);
            assertTrue(config.contains(base + ".level"), "Missing level for " + mobId);
            assertTrue(config.contains(base + ".drops"), "Missing drops for " + mobId);
        }
    }

    @Test
    void configuredCustomMonsterEntityTypesAreLivingAndSpawnable() {
        YamlConfiguration config = loadConfig();
        var monstersSection = config.getConfigurationSection("custom-monsters.monsters");
        assertNotNull(monstersSection, "custom monster section should exist");

        for (String mobId : monstersSection.getKeys(false)) {
            String rawType = config.getString("custom-monsters.monsters." + mobId + ".entity-type");
            assertNotNull(rawType, "Missing entity type for " + mobId);

            EntityType entityType = EntityType.valueOf(rawType.trim().toUpperCase());
            assertTrue(entityType.isAlive(), "Custom monster " + mobId + " must use a living entity type");
            assertTrue(entityType.isSpawnable(), "Custom monster " + mobId + " must use a spawnable entity type");
        }
    }

    @Test
    void shadowStalkerDropsSummoningEyeAtFivePercent() {
        YamlConfiguration config = loadConfig();
        String base = "custom-monsters.monsters.shadow_stalker.drops.summoning_eye";

        assertEquals("SUMMONING_EYE", config.getString(base + ".custom-item"));
        assertEquals(0.05D, config.getDouble(base + ".chance"), 1.0E-12D);
        assertEquals(1, config.getInt(base + ".min-amount"));
        assertEquals(1, config.getInt(base + ".max-amount"));
    }

    private YamlConfiguration loadConfig() {
        var stream = CustomMonsterConfigTest.class.getClassLoader().getResourceAsStream("config.yml");
        assertNotNull(stream, "config.yml should be available in test resources");
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
