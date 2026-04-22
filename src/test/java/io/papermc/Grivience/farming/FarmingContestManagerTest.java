package io.papermc.Grivience.farming;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FarmingContestManagerTest {
    @TempDir
    Path tempDir;

    @Test
    void stoppingScheduledContestKeepsItSuppressedAcrossReload() throws Exception {
        YamlConfiguration config = baseConfig();
        FarmingContestManager firstManager = new FarmingContestManager(mockPlugin(config), false);

        invokeTick(firstManager);
        String scheduledId = activeContestId(firstManager);
        assertNotNull(scheduledId);
        assertTrue(scheduledId.startsWith("scheduled:"));

        assertTrue(firstManager.stopActiveContest(null, false));
        assertEquals(scheduledId, suppressedScheduledContestId(firstManager));

        invokeTick(firstManager);
        assertNull(activeContestId(firstManager));

        firstManager.shutdown();

        FarmingContestManager reloadedManager = new FarmingContestManager(mockPlugin(config), false);
        assertEquals(scheduledId, suppressedScheduledContestId(reloadedManager));

        invokeTick(reloadedManager);
        assertNull(activeContestId(reloadedManager));
    }

    @Test
    void stoppingForcedContestAlsoSuppressesCurrentScheduledWindow() throws Exception {
        FarmingContestManager manager = new FarmingContestManager(mockPlugin(baseConfig()), false);

        assertTrue(manager.startForcedContest(
                List.of(FarmingContestCrop.WHEAT, FarmingContestCrop.CARROT, FarmingContestCrop.POTATO),
                20,
                null
        ));
        assertNotNull(activeContestId(manager));
        assertTrue(activeContestId(manager).startsWith("forced:"));

        assertTrue(manager.stopActiveContest(null, false));
        String suppressedId = suppressedScheduledContestId(manager);
        assertNotNull(suppressedId);
        assertTrue(suppressedId.startsWith("scheduled:"));

        invokeTick(manager);
        assertNull(activeContestId(manager));
    }

    private GriviencePlugin mockPlugin(YamlConfiguration config) {
        GriviencePlugin plugin = mock(GriviencePlugin.class);
        when(plugin.getName()).thenReturn("Grivience");
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        return plugin;
    }

    private static YamlConfiguration baseConfig() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("farming-contests.enabled", true);
        config.set("farming-contests.minimum-farming-level", 0);
        config.set("farming-contests.minimum-collection-increase", 1L);
        config.set("farming-contests.schedule.interval-minutes", 20);
        config.set("farming-contests.schedule.duration-minutes", 20);
        config.set("farming-contests.schedule.offset-minutes", 0);
        config.set("farming-contests.schedule.seed", 91_451L);
        config.set("farming-contests.crop-pool", List.of(
                "wheat",
                "carrot",
                "potato",
                "sugar_cane",
                "nether_wart",
                "cactus"
        ));
        config.set("farming-contests.announcements.broadcast-start", false);
        config.set("farming-contests.announcements.broadcast-end", false);
        return config;
    }

    private static void invokeTick(FarmingContestManager manager) throws Exception {
        Method method = FarmingContestManager.class.getDeclaredMethod("tickContestState");
        method.setAccessible(true);
        method.invoke(manager);
    }

    private static String activeContestId(FarmingContestManager manager) throws Exception {
        Field activeContestField = FarmingContestManager.class.getDeclaredField("activeContest");
        activeContestField.setAccessible(true);
        Object activeContest = activeContestField.get(manager);
        if (activeContest == null) {
            return null;
        }
        Field idField = activeContest.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        return (String) idField.get(activeContest);
    }

    private static String suppressedScheduledContestId(FarmingContestManager manager) throws Exception {
        Field suppressedField = FarmingContestManager.class.getDeclaredField("suppressedScheduledContestId");
        suppressedField.setAccessible(true);
        return (String) suppressedField.get(manager);
    }
}
