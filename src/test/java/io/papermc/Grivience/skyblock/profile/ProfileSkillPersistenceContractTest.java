package io.papermc.Grivience.skyblock.profile;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileSkillPersistenceContractTest {

    @Test
    void profileManagerSyncsStoredSkillSnapshotsFromLiveProgress() throws IOException {
        String source = read("src/main/java/io/papermc/Grivience/skyblock/profile/ProfileManager.java");

        assertTrue(source.contains("private void syncProfileSnapshots(SkyBlockProfile profile)"),
                "ProfileManager should centralize all profile snapshot synchronization");
        assertTrue(source.contains("private void syncProfileSkillLevels(SkyBlockProfile profile)"),
                "ProfileManager should centralize profile skill synchronization");
        assertTrue(source.contains("UUID skillProfileId = profile.getCanonicalProfileId();"),
                "Profile skill snapshots should resolve the canonical shared profile when needed");
        assertTrue(source.contains("skillManager.getLevel(skillProfileId, skill)"),
                "Profile skill snapshots should come from the canonical live skill manager state");
        assertTrue(source.contains("profile.setSkillLevel(skill.name(),"),
                "Profile saves should mirror live skill levels into the persisted snapshot");
        assertTrue(countOccurrences(source, "syncProfileSnapshots(profile);") >= 7,
                "ProfileManager should sync live profile snapshots before returning or saving profiles");
    }

    @Test
    void profileManagerQueuesActiveSkillSnapshotSaves() throws IOException {
        String source = read("src/main/java/io/papermc/Grivience/skyblock/profile/ProfileManager.java");

        assertTrue(source.contains("public void syncSkillSnapshotsAndQueueSave(Player player)"),
                "ProfileManager should expose an active player skill snapshot sync entry point");
        assertTrue(source.contains("private void queueProfileSave(UUID profileId)"),
                "ProfileManager should debounce profile saves for active skill updates");
        assertTrue(source.contains("queueProfileSave(profile.getProfileId());"),
                "Skill snapshot sync should queue a persisted profile save");
    }

    @Test
    void reloadPathSavesLevelDataBeforeReloadingIt() throws IOException {
        String source = read("src/main/java/io/papermc/Grivience/GriviencePlugin.java");
        int reloadIndex = source.indexOf("public void reloadSystems()");
        int saveIndex = source.indexOf("skyblockLevelManager.save();", reloadIndex);
        int loadIndex = source.indexOf("skyblockLevelManager.load();", reloadIndex);

        assertTrue(reloadIndex >= 0, "Plugin should keep a reloadSystems entry point");
        assertTrue(saveIndex > reloadIndex, "Reload should save Skyblock level data before clearing memory");
        assertTrue(loadIndex > saveIndex, "Reload should only load levels after the save completes");
    }
    private static int countOccurrences(String source, String token) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
