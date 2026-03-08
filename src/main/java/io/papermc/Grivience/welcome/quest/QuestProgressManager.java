package io.papermc.Grivience.welcome.quest;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player quest progress and persistence.
 */
public class QuestProgressManager {
    private final GriviencePlugin plugin;
    private final Map<UUID, PlayerQuestData> playerProgress = new ConcurrentHashMap<>();
    private File progressFile;
    private FileConfiguration progressConfig;

    public QuestProgressManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.progressFile = new File(plugin.getDataFolder(), "welcome-quest-progress.yml");
        loadProgress();
    }

    /**
     * Load all player progress from file.
     */
    public void loadProgress() {
        if (!progressFile.exists()) {
            progressFile.getParentFile().mkdirs();
            try {
                progressFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create quest progress file: " + e.getMessage());
            }
        }

        progressConfig = YamlConfiguration.loadConfiguration(progressFile);

        // Load all player progress
        if (progressConfig.contains("players")) {
            for (String uuidStr : progressConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    Set<String> completedQuests = new HashSet<>(progressConfig.getStringList("players." + uuidStr + ".completed"));
                    Map<String, Object> progressMap = progressConfig.getConfigurationSection("players." + uuidStr + ".progress").getValues(false);
                    Map<String, Integer> questProgress = new ConcurrentHashMap<>();
                    
                    for (Map.Entry<String, Object> entry : progressMap.entrySet()) {
                        questProgress.put(entry.getKey(), (Integer) entry.getValue());
                    }

                    playerProgress.put(uuid, new PlayerQuestData(completedQuests, questProgress));
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load progress for " + uuidStr + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Save all player progress to file.
     */
    public void saveProgress() {
        if (progressConfig == null || progressFile == null) {
            return;
        }

        progressConfig.set("players", null); // Clear existing data

        for (Map.Entry<UUID, PlayerQuestData> entry : playerProgress.entrySet()) {
            String path = "players." + entry.getKey().toString();
            progressConfig.set(path + ".completed", new ArrayList<>(entry.getValue().completedQuests));
            progressConfig.set(path + ".progress", entry.getValue().questProgress);
        }

        try {
            progressConfig.save(progressFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save quest progress: " + e.getMessage());
        }
    }

    /**
     * Get a player's quest data.
     */
    public PlayerQuestData getQuestData(Player player) {
        return playerProgress.computeIfAbsent(player.getUniqueId(), k -> new PlayerQuestData());
    }

    /**
     * Get progress for a specific quest.
     */
    public int getQuestProgress(Player player, String questId) {
        PlayerQuestData data = getQuestData(player);
        return data.questProgress.getOrDefault(questId, 0);
    }

    /**
     * Update progress for a quest.
     */
    public void updateQuestProgress(Player player, String questId, int progress) {
        PlayerQuestData data = getQuestData(player);
        data.questProgress.put(questId, progress);
        
        // Auto-save periodically
        saveProgress();
    }

    /**
     * Increment progress for a quest.
     */
    public void incrementQuestProgress(Player player, String questId, int amount) {
        int current = getQuestProgress(player, questId);
        updateQuestProgress(player, questId, current + amount);
    }

    /**
     * Check if a quest is completed.
     */
    public boolean isQuestCompleted(Player player, String questId) {
        PlayerQuestData data = getQuestData(player);
        return data.completedQuests.contains(questId);
    }

    /**
     * Complete a quest.
     */
    public void completeQuest(Player player, String questId) {
        PlayerQuestData data = getQuestData(player);
        if (!data.completedQuests.contains(questId)) {
            data.completedQuests.add(questId);
            data.questProgress.remove(questId); // Clear progress
            
            // Save immediately
            saveProgress();
            
            // Notify player
            WelcomeQuest quest = WelcomeQuestRegistry.get(questId);
            if (quest != null) {
                player.sendMessage("");
                player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Quest Complete: " + quest.getName());
                if (quest.getMoneyReward() > 0) {
                    player.sendMessage(ChatColor.GOLD + "Reward: $" + quest.getMoneyReward());
                }
                if (quest.getXpReward() > 0) {
                    player.sendMessage(ChatColor.AQUA + "Reward: " + quest.getXpReward() + " XP");
                }
                player.sendMessage("");
            }
        }
    }

    /**
     * Get all completed quests for a player.
     */
    public Set<String> getCompletedQuests(Player player) {
        return new HashSet<>(getQuestData(player).completedQuests);
    }

    /**
     * Get total quests completed by a player.
     */
    public int getTotalCompleted(Player player) {
        return getQuestData(player).completedQuests.size();
    }

    /**
     * Reset a player's quest progress.
     */
    public void resetProgress(Player player) {
        playerProgress.remove(player.getUniqueId());
        saveProgress();
    }

    /**
     * Check if a player can start a quest (prerequisites met).
     */
    public boolean canStartQuest(Player player, WelcomeQuest quest) {
        if (quest.getPreviousQuestId() == null) {
            return true; // No prerequisite
        }
        return isQuestCompleted(player, quest.getPreviousQuestId());
    }

    /**
     * Get available quests for a player.
     */
    public Set<WelcomeQuest> getAvailableQuests(Player player) {
        Set<String> completed = getCompletedQuests(player);
        Set<WelcomeQuest> available = new HashSet<>();
        
        for (WelcomeQuest quest : WelcomeQuestRegistry.getAll()) {
            if (!completed.contains(quest.getId()) || quest.isRepeatable()) {
                if (canStartQuest(player, quest)) {
                    available.add(quest);
                }
            }
        }
        
        return available;
    }

    /**
     * Player quest data class.
     */
    public static class PlayerQuestData {
        public final Set<String> completedQuests;
        public final Map<String, Integer> questProgress;

        public PlayerQuestData() {
            this.completedQuests = new HashSet<>();
            this.questProgress = new ConcurrentHashMap<>();
        }

        public PlayerQuestData(Set<String> completedQuests, Map<String, Integer> questProgress) {
            this.completedQuests = completedQuests;
            this.questProgress = questProgress;
        }
    }
}
