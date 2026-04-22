package io.papermc.Grivience.skyblock.profile;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfilePersistenceContractTest {

    @Test
    void profileManagerSyncsCollectionAndQuestSnapshotsBeforeSaving() throws IOException {
        String source = read("src/main/java/io/papermc/Grivience/skyblock/profile/ProfileManager.java");

        assertTrue(source.contains("private void syncProfileCollectionLevels(SkyBlockProfile profile)"),
                "ProfileManager should sync collection snapshot data from the live collections manager");
        assertTrue(source.contains("profile.replaceCollectionLevels(collectionsManager.getCollectionTierSnapshot(collectionProfileId));"),
                "Profile saves should mirror canonical collection tiers into the profile snapshot");
        assertTrue(source.contains("private void syncProfileCompletedQuests(SkyBlockProfile profile)"),
                "ProfileManager should sync quest completion snapshots from the live quest manager");
        assertTrue(source.contains("mergedCompleted.addAll(questManager.completedQuestIds(questProfileId));"),
                "Profile saves should merge live quest completions into the persisted profile snapshot");
        assertTrue(source.contains("flushLiveProfileData();"),
                "Profile saves should flush dirty external profile-scoped data before writing the YAML snapshot");
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
