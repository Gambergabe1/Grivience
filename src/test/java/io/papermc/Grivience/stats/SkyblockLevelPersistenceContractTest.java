package io.papermc.Grivience.stats;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SkyblockLevelPersistenceContractTest {

    @Test
    void levelManagerTracksDirtyStateAndAutoSaves() throws IOException {
        String source = read("src/main/java/io/papermc/Grivience/stats/SkyblockLevelManager.java");

        assertTrue(source.contains("private BukkitTask autoSaveTask;"),
                "SkyblockLevelManager should keep an auto-save task for profile-scoped level data");
        assertTrue(source.contains("private boolean dirty;"),
                "SkyblockLevelManager should track whether live level data needs to be flushed");
        assertTrue(source.contains("public void saveIfDirty()"),
                "SkyblockLevelManager should expose a dirty-only save path");
        assertTrue(source.contains("public void shutdown()"),
                "SkyblockLevelManager should stop auto-save and flush pending data on shutdown");
        assertTrue(source.contains("runTaskTimer(plugin, () -> { if (dirty) save(); }"),
                "SkyblockLevelManager should periodically flush dirty level data to disk");
        assertTrue(source.contains("markDirty();"),
                "SkyblockLevelManager mutations should mark the in-memory state dirty");
    }

    @Test
    void pluginDeclaresAndUsesSkyblockLevelAutoSaveDefaults() throws IOException {
        String source = read("src/main/java/io/papermc/Grivience/GriviencePlugin.java");

        assertTrue(source.contains("config.addDefault(base + \"auto-save-interval-seconds\", 300);"),
                "Plugin defaults should include a Skyblock level auto-save interval");
        assertTrue(source.contains("skyblockLevelManager.shutdown();"),
                "Plugin shutdown should stop the auto-save task and flush dirty level data");
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
