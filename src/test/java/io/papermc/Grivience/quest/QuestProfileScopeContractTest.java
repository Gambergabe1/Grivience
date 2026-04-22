package io.papermc.Grivience.quest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestProfileScopeContractTest {

    @Test
    void questProgressUsesCanonicalProfileIds() throws IOException {
        String source = read("src/main/java/io/papermc/Grivience/quest/QuestManager.java");

        assertTrue(source.contains("private UUID resolveProgressId(Player player)"),
                "QuestManager should resolve active quest progress from the current profile");
        assertTrue(source.contains("private UUID resolveProgressId(UUID playerId)"),
                "QuestManager should support owner/profile UUID lookups without falling back to raw player state");
        assertTrue(source.contains("profile.getCanonicalProfileId()"),
                "Quest progress should collapse coop members onto the canonical shared profile");
        assertTrue(source.contains("public synchronized Set<String> completedQuestIds(UUID playerId)"),
                "QuestManager should expose completed quest snapshots for profile persistence");
    }

    @Test
    void questProgressPersistsUnderProfileKeys() throws IOException {
        String source = read("src/main/java/io/papermc/Grivience/quest/QuestManager.java");

        assertTrue(source.contains("ConfigurationSection playersSection = yaml.getConfigurationSection(\"profiles\");"),
                "QuestManager should read the profile-scoped progress section first");
        assertTrue(source.contains("playersSection = yaml.getConfigurationSection(\"players\");"),
                "QuestManager should still migrate legacy player-scoped progress");
        assertTrue(source.contains("String base = \"profiles.\" + playerId + \".quests.\" + questEntry.getKey() + \".\";"),
                "QuestManager should write quest progress back under profile UUID keys");
        assertTrue(source.contains("migrateLegacyProgress(ownerId, progressId);"),
                "QuestManager should migrate legacy owner UUID quest progress into profile UUID storage");
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
