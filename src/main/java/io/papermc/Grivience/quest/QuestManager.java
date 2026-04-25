package io.papermc.Grivience.quest;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skyblock.economy.ProfileEconomyService;
import io.papermc.Grivience.util.CommandDispatchUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages quest definitions, player progress, and objective tracking.
 */
public final class QuestManager {
    private static final String QUESTS_FILE_NAME = "quests.yml";
    private static final String PROGRESS_FILE_NAME = "quest-progress.yml";

    private final GriviencePlugin plugin;
    private final File questsFile;
    private final File progressFile;
    private final Map<String, ConversationQuest> quests = new LinkedHashMap<>();
    private final Map<String, Map<String, QuestProgress>> progressByPlayer = new ConcurrentHashMap<>();

    public QuestManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.questsFile = new File(plugin.getDataFolder(), QUESTS_FILE_NAME);
        this.progressFile = new File(plugin.getDataFolder(), PROGRESS_FILE_NAME);
        reload();
    }

    public synchronized void reload() {
        loadQuestDefinitions();
        loadProgress();
    }

    public synchronized Map<String, ConversationQuest> quests() {
        return Collections.unmodifiableMap(quests);
    }

    public synchronized List<String> questIds() {
        return new ArrayList<>(quests.keySet());
    }

    public synchronized List<ConversationQuest> questsSorted() {
        List<ConversationQuest> sorted = new ArrayList<>(quests.values());
        sorted.sort(Comparator.comparing(ConversationQuest::id));
        return sorted;
    }

    public synchronized ConversationQuest quest(String questId) {
        return quests.get(ConversationQuest.normalizeId(questId));
    }

    public synchronized boolean createQuest(String questId, String displayName) {
        String normalizedId = ConversationQuest.normalizeId(questId);
        if (normalizedId.isBlank() || quests.containsKey(normalizedId)) {
            return false;
        }
        String prettyName = displayName == null || displayName.isBlank() ? normalizedId : displayName.trim();
        ConversationQuest quest = new ConversationQuest(
                normalizedId,
                prettyName,
                "Talk to " + normalizedId + ".",
                "Hub",
                "",
                normalizedId,
                List.of(),
                List.of(),
                new ArrayList<>(),
                false,
                true
        );
        quests.put(normalizedId, quest);
        saveQuestDefinitions();
        return true;
    }

    public synchronized boolean deleteQuest(String questId) {
        String normalizedId = ConversationQuest.normalizeId(questId);
        ConversationQuest removed = quests.remove(normalizedId);
        if (removed == null) {
            return false;
        }
        for (Map<String, QuestProgress> questProgressMap : progressByPlayer.values()) {
            questProgressMap.remove(normalizedId);
        }
        saveQuestDefinitions();
        saveProgress();
        return true;
    }

    public synchronized boolean setDisplayName(String questId, String displayName) {
        ConversationQuest quest = quest(questId);
        if (quest == null) {
            return false;
        }
        quest.setDisplayName(displayName);
        saveQuestDefinitions();
        return true;
    }

    public synchronized boolean setDescription(String questId, String description) {
        ConversationQuest quest = quest(questId);
        if (quest == null) {
            return false;
        }
        quest.setDescription(description);
        saveQuestDefinitions();
        return true;
    }

    public synchronized boolean setWorld(String questId, String world) {
        ConversationQuest quest = quest(questId);
        if (quest == null) {
            return false;
        }
        quest.setWorld(world);
        saveQuestDefinitions();
        return true;
    }

    public synchronized boolean setStarterNpc(String questId, String starterNpcId) {
        ConversationQuest quest = quest(questId);
        if (quest == null) {
            return false;
        }
        quest.setStarterNpcId(Objects.requireNonNullElse(starterNpcId, ""));
        saveQuestDefinitions();
        return true;
    }

    public synchronized boolean setTargetNpc(String questId, String targetNpcId) {
        ConversationQuest quest = quest(questId);
        if (quest == null) {
            return false;
        }
        quest.setTargetNpcId(Objects.requireNonNullElse(targetNpcId, questId));
        saveQuestDefinitions();
        return true;
    }

    public synchronized boolean setRepeatable(String questId, boolean repeatable) {
        ConversationQuest quest = quest(questId);
        if (quest == null) {
            return false;
        }
        quest.setRepeatable(repeatable);
        saveQuestDefinitions();
        return true;
    }

    public synchronized boolean setEnabled(String questId, boolean enabled) {
        ConversationQuest quest = quest(questId);
        if (quest == null) {
            return false;
        }
        quest.setEnabled(enabled);
        saveQuestDefinitions();
        return true;
    }

    public synchronized boolean addRewardCommand(String questId, String command) {
        ConversationQuest quest = quest(questId);
        if (quest == null || command == null || command.isBlank()) {
            return false;
        }
        quest.rewardCommands().add(command.trim());
        saveQuestDefinitions();
        return true;
    }

    public synchronized boolean removeRewardCommand(String questId, int index) {
        ConversationQuest quest = quest(questId);
        if (quest == null || index < 0 || index >= quest.rewardCommands().size()) {
            return false;
        }
        quest.rewardCommands().remove(index);
        saveQuestDefinitions();
        return true;
    }

    public synchronized boolean clearRewardCommands(String questId) {
        ConversationQuest quest = quest(questId);
        if (quest == null) {
            return false;
        }
        quest.rewardCommands().clear();
        saveQuestDefinitions();
        return true;
    }

    public synchronized StartResult startQuest(Player player, String questId, QuestTriggerSource source, boolean notify) {
        ConversationQuest quest = quest(questId);
        if (quest == null) return StartResult.QUEST_NOT_FOUND;
        if (!quest.enabled()) return StartResult.QUEST_DISABLED;

        UUID playerId = player.getUniqueId();
        String progressId = resolveProgressId(playerId).toString();
        Map<String, QuestProgress> playerProgress = progressByPlayer.computeIfAbsent(progressId, k -> new ConcurrentHashMap<>());
        
        QuestProgress progress = playerProgress.get(quest.id());
        if (progress != null && progress.active()) return StartResult.ALREADY_ACTIVE;
        if (progress != null && progress.hasCompletedAtLeastOnce() && !quest.repeatable()) return StartResult.ALREADY_COMPLETED;

        // Check prerequisites
        for (String preId : quest.prerequisites()) {
            QuestProgress preProg = playerProgress.get(preId);
            if (preProg == null || !preProg.hasCompletedAtLeastOnce()) {
                if (notify && source == QuestTriggerSource.COMMAND) {
                    player.sendMessage(ChatColor.RED + "You haven't unlocked this quest yet!");
                }
                return StartResult.PREREQUISITE_NOT_MET;
            }
        }

        if (progress == null) {
            progress = new QuestProgress();
            playerProgress.put(quest.id(), progress);
        }
        
        progress.setActive(true);
        saveProgress();

        if (notify) {
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "[Quest Started] " + color(quest.displayName()));
            player.sendMessage(ChatColor.YELLOW + quest.description());
        }

        return StartResult.STARTED;
    }

    public synchronized CancelResult cancelQuest(Player player, String questId, boolean notify) {
        ConversationQuest quest = quest(questId);
        if (quest == null) return CancelResult.QUEST_NOT_FOUND;

        String progressId = resolveProgressId(player.getUniqueId()).toString();
        Map<String, QuestProgress> playerProgress = progressByPlayer.get(progressId);
        if (playerProgress == null || !playerProgress.containsKey(quest.id())) return CancelResult.NOT_ACTIVE;

        QuestProgress progress = playerProgress.get(quest.id());
        if (!progress.active()) return CancelResult.NOT_ACTIVE;

        progress.setActive(false);
        saveProgress();

        if (notify) {
            player.sendMessage(ChatColor.RED + "Quest canceled: " + color(quest.displayName()));
        }

        return CancelResult.CANCELED;
    }

    public synchronized CompleteResult completeQuest(Player player, String questId, QuestTriggerSource source, boolean notify) {
        ConversationQuest quest = quest(questId);
        if (quest == null) return CompleteResult.QUEST_NOT_FOUND;

        String progressId = resolveProgressId(player).toString();
        Map<String, QuestProgress> playerProgress = progressByPlayer.get(progressId);
        if (playerProgress == null || !playerProgress.containsKey(quest.id())) return CompleteResult.NOT_ACTIVE;

        QuestProgress progress = playerProgress.get(quest.id());
        if (!progress.active()) return CompleteResult.NOT_ACTIVE;

        // Check if all objectives are complete
        if (!isObjectivesComplete(quest, progress)) {
            return CompleteResult.OBJECTIVES_NOT_MET;
        }

        progress.setActive(false);
        progress.incrementCompletions();
        saveProgress();

        runRewardCommands(player, quest);

        if (notify) {
            player.sendMessage("");
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "QUEST COMPLETE!");
            player.sendMessage(ChatColor.YELLOW + ChatColor.stripColor(color(quest.displayName())));
            player.sendMessage("");
        }

        return CompleteResult.COMPLETED;
    }

    public synchronized void advanceObjective(Player player, QuestObjective.ObjectiveType type, String target, int amount) {
        String progressId = resolveProgressId(player).toString();
        Map<String, QuestProgress> playerProgress = progressByPlayer.get(progressId);
        if (playerProgress == null) return;

        boolean changed = false;
        for (Map.Entry<String, QuestProgress> entry : playerProgress.entrySet()) {
            QuestProgress prog = entry.getValue();
            if (!prog.active()) continue;
            ConversationQuest q = quest(entry.getKey());
            if (q == null) continue;

            for (int i = 0; i < q.objectives().size(); i++) {
                QuestObjective obj = q.objectives().get(i);
                if (obj.type() == type && (obj.target().isEmpty() || obj.target().equalsIgnoreCase(target))) {
                    int current = prog.getObjectiveProgress(i);
                    if (current < obj.amount()) {
                        prog.setObjectiveProgress(i, Math.min(obj.amount(), current + amount));
                        changed = true;
                        
                        if (prog.getObjectiveProgress(i) >= obj.amount()) {
                            player.sendMessage(ChatColor.GREEN + "[Quest] Objective complete: " + obj.description());
                            if (isObjectivesComplete(q, prog)) {
                                player.sendMessage(ChatColor.GOLD + "[Quest] All objectives complete! Return to " + ChatColor.AQUA + q.targetNpcId() + ChatColor.GOLD + " to finish.");
                            }
                        }
                    }
                }
            }
        }

        if (changed) {
            saveProgress();
        }
    }

    private boolean isObjectivesComplete(ConversationQuest quest, QuestProgress progress) {
        for (int i = 0; i < quest.objectives().size(); i++) {
            if (progress.getObjectiveProgress(i) < quest.objectives().get(i).amount()) {
                return false;
            }
        }
        return true;
    }

    public synchronized int handleNpcConversation(Player player, String npcId, QuestTriggerSource source, boolean notify) {
        int changes = 0;
        
        // 1. Try to complete active quests
        List<ConversationQuest> active = activeQuests(player);
        for (ConversationQuest q : active) {
            if (q.matchesTargetNpc(npcId)) {
                CompleteResult res = completeQuest(player, q.id(), source, notify);
                if (res == CompleteResult.COMPLETED) {
                    changes++;
                } else if (res == CompleteResult.OBJECTIVES_NOT_MET && notify) {
                    player.sendMessage(ChatColor.YELLOW + "[Quest] " + ChatColor.WHITE + q.targetNpcId() + ": " + ChatColor.GRAY + "You haven't finished your tasks yet!");
                }
            }
        }

        // 2. Try to start available quests
        for (ConversationQuest q : quests.values()) {
            if (q.enabled() && q.matchesStarterNpc(npcId)) {
                StartResult res = startQuest(player, q.id(), source, notify);
                if (res == StartResult.STARTED) {
                    changes++;
                }
            }
        }

        return changes;
    }

    public synchronized boolean isQuestActive(Player player, String questId) {
        String progressId = resolveProgressId(player).toString();
        Map<String, QuestProgress> playerProgress = progressByPlayer.get(progressId);
        if (playerProgress == null) return false;
        QuestProgress prog = playerProgress.get(questId);
        return prog != null && prog.active();
    }

    public synchronized boolean hasCompletedQuest(Player player, String questId) {
        String progressId = resolveProgressId(player).toString();
        Map<String, QuestProgress> playerProgress = progressByPlayer.get(progressId);
        if (playerProgress == null) return false;
        QuestProgress prog = playerProgress.get(questId);
        return prog != null && prog.hasCompletedAtLeastOnce();
    }

    public synchronized QuestProgress getProgress(Player player, String questId) {
        String progressId = resolveProgressId(player).toString();
        Map<String, QuestProgress> playerProgress = progressByPlayer.get(progressId);
        return playerProgress != null ? playerProgress.get(questId) : null;
    }

    public synchronized List<ConversationQuest> activeQuests(Player player) {
        String progressId = resolveProgressId(player).toString();
        Map<String, QuestProgress> playerProgress = progressByPlayer.get(progressId);
        if (playerProgress == null) return List.of();

        List<ConversationQuest> active = new ArrayList<>();
        for (Map.Entry<String, QuestProgress> entry : playerProgress.entrySet()) {
            if (!entry.getValue().active()) continue;
            ConversationQuest quest = quest(entry.getKey());
            if (quest != null) {
                active.add(quest);
            }
        }
        active.sort(Comparator.comparing(ConversationQuest::id));
        return active;
    }

    public synchronized Set<String> completedQuestIds(UUID playerId) {
        Map<String, QuestProgress> playerProgress = progressByPlayer.get(resolveProgressId(playerId));
        if (playerProgress == null || playerProgress.isEmpty()) {
            return Set.of();
        }

        Set<String> completed = new LinkedHashSet<>();
        for (Map.Entry<String, QuestProgress> entry : playerProgress.entrySet()) {
            if (entry.getValue() != null && entry.getValue().hasCompletedAtLeastOnce()) {
                completed.add(entry.getKey());
            }
        }
        return completed;
    }

    private void runRewardCommands(Player player, ConversationQuest quest) {
        if (quest.rewardCommands().isEmpty()) {
            return;
        }

        for (String command : quest.rewardCommands()) {
            if (command == null || command.isBlank()) {
                continue;
            }
            String parsed = command
                    .replace("{player}", player.getName())
                    .replace("{uuid}", player.getUniqueId().toString())
                    .replace("{quest_id}", quest.id())
                    .replace("{quest_name}", ChatColor.stripColor(color(quest.displayName())));
            if (parsed.startsWith("/")) {
                parsed = parsed.substring(1);
            }
            
            ProfileEconomyService profileEconomy = new ProfileEconomyService(plugin);
            if (profileEconomy.executeEcoLikeCommand(player, parsed)) {
                continue;
            }
            boolean success = CommandDispatchUtil.dispatchConsole(plugin, parsed);
            if (!success) {
                plugin.getLogger().warning("Failed reward command for quest '" + quest.id() + "': " + parsed);
            }
        }
    }

    private void loadQuestDefinitions() {
        quests.clear();

        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data directory for quests.");
            return;
        }

        if (!questsFile.exists()) {
            if (plugin.getResource(QUESTS_FILE_NAME) != null) {
                plugin.saveResource(QUESTS_FILE_NAME, false);
            } else {
                createDefaultQuests();
                saveQuestDefinitions();
                return;
            }
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(questsFile);
        ConfigurationSection section = yaml.getConfigurationSection("quests");
        if (section == null || section.getKeys(false).isEmpty()) {
            createDefaultQuests();
            saveQuestDefinitions();
            return;
        }

        List<String> questIds = new ArrayList<>(section.getKeys(false));
        questIds.sort(String::compareToIgnoreCase);

        for (String key : questIds) {
            ConfigurationSection questSection = section.getConfigurationSection(key);
            if (questSection == null) {
                continue;
            }

            String questId = ConversationQuest.normalizeId(key);
            if (questId.isBlank()) {
                continue;
            }

            List<QuestObjective> objectives = new ArrayList<>();
            ConfigurationSection objSection = questSection.getConfigurationSection("objectives");
            if (objSection != null) {
                for (String objKey : objSection.getKeys(false)) {
                    ConfigurationSection o = objSection.getConfigurationSection(objKey);
                    if (o == null) continue;
                    try {
                        QuestObjective.ObjectiveType type = QuestObjective.ObjectiveType.valueOf(o.getString("type", "TALK_TO_NPC").toUpperCase());
                        objectives.add(new QuestObjective(
                                type,
                                o.getString("target", ""),
                                o.getInt("amount", 1),
                                o.getString("description", "Objective")
                        ));
                    } catch (IllegalArgumentException ignored) {}
                }
            }

            ConversationQuest quest = new ConversationQuest(
                    questId,
                    questSection.getString("display-name", key),
                    questSection.getString("description", "Talk to an NPC."),
                    questSection.getString("world", "Hub"),
                    questSection.getString("starter-npc", ""),
                    questSection.getString("target-npc", ""),
                    questSection.getStringList("rewards"),
                    questSection.getStringList("prerequisites"),
                    objectives,
                    questSection.getBoolean("repeatable", false),
                    questSection.getBoolean("enabled", true)
            );

            if (!quest.hasTargetNpc() && quest.hasStarterNpc()) {
                quest.setTargetNpcId(quest.starterNpcId());
            }
            if (!quest.hasTargetNpc()) {
                quest.setTargetNpcId(quest.id());
            }

            quests.put(quest.id(), quest);
        }

        if (quests.isEmpty()) {
            createDefaultQuests();
            saveQuestDefinitions();
        }
    }

    private void createDefaultQuests() {
        quests.clear();
        ConversationQuest quest = new ConversationQuest(
                "village_greeting",
                "&aVillage Greeting",
                "Talk to the village elder.",
                "Hub",
                "elder_start",
                "elder_finish",
                List.of("eco give {player} 1000"),
                List.of(),
                new ArrayList<>(),
                false,
                true
        );
        quests.put(quest.id(), quest);
    }

    private void saveQuestDefinitions() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (ConversationQuest quest : quests.values()) {
            String base = "quests." + quest.id() + ".";
            yaml.set(base + "display-name", quest.displayName());
            yaml.set(base + "description", quest.description());
            yaml.set(base + "world", quest.world());
            yaml.set(base + "starter-npc", quest.starterNpcId());
            yaml.set(base + "target-npc", quest.targetNpcId());
            yaml.set(base + "rewards", quest.rewardCommands());
            yaml.set(base + "prerequisites", quest.prerequisites());
            yaml.set(base + "repeatable", quest.repeatable());
            yaml.set(base + "enabled", quest.enabled());

            if (!quest.objectives().isEmpty()) {
                for (int i = 0; i < quest.objectives().size(); i++) {
                    QuestObjective obj = quest.objectives().get(i);
                    String objBase = base + "objectives.obj" + i + ".";
                    yaml.set(objBase + "type", obj.type().name());
                    yaml.set(objBase + "target", obj.target());
                    yaml.set(objBase + "amount", obj.amount());
                    yaml.set(objBase + "description", obj.description());
                }
            }
        }

        try {
            yaml.save(questsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save quests to " + questsFile.getName() + ": " + e.getMessage());
        }
    }

    private void loadProgress() {
        progressByPlayer.clear();
        if (!progressFile.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(progressFile);
        ConfigurationSection playersSection = yaml.getConfigurationSection("profiles");
        if (playersSection == null) {
            playersSection = yaml.getConfigurationSection("players");
        }
        if (playersSection == null) return;

        for (String playerKey : playersSection.getKeys(false)) {
            ConfigurationSection playerSection = playersSection.getConfigurationSection(playerKey + ".quests");
            if (playerSection == null) {
                playerSection = playersSection.getConfigurationSection(playerKey);
            }
            if (playerSection == null) continue;

            Map<String, QuestProgress> quests = new ConcurrentHashMap<>();
            for (String qId : playerSection.getKeys(false)) {
                ConfigurationSection qSection = playerSection.getConfigurationSection(qId);
                if (qSection == null) continue;

                QuestProgress progress = new QuestProgress();
                progress.setActive(qSection.getBoolean("active", false));
                progress.setCompletions(qSection.getBoolean("completed", false) ? 1 : 0);
                
                ConfigurationSection objProg = qSection.getConfigurationSection("objectives");
                if (objProg != null) {
                    for (String oKey : objProg.getKeys(false)) {
                        try {
                            int idx = Integer.parseInt(oKey.replace("obj", ""));
                            progress.setObjectiveProgress(idx, objProg.getInt(oKey, 0));
                        } catch (NumberFormatException ignored) {}
                    }
                }
                quests.put(qId, progress);
            }
            progressByPlayer.put(playerKey, quests);
        }
    }

    private void saveProgress() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, Map<String, QuestProgress>> playerEntry : progressByPlayer.entrySet()) {
            String playerId = playerEntry.getKey();
            for (Map.Entry<String, QuestProgress> questEntry : playerEntry.getValue().entrySet()) {
                QuestProgress prog = questEntry.getValue();
                String base = "profiles." + playerId + ".quests." + questEntry.getKey() + ".";
                yaml.set(base + "active", prog.active());
                yaml.set(base + "completed", prog.hasCompletedAtLeastOnce());
                
                for (Map.Entry<Integer, Integer> objEntry : prog.getAllObjectiveProgress().entrySet()) {
                    yaml.set(base + "objectives.obj" + objEntry.getKey(), objEntry.getValue());
                }
            }
        }

        try {
            yaml.save(progressFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save quest progress to " + progressFile.getName() + ": " + e.getMessage());
        }
    }

    public String progressLabel(Player p, ConversationQuest q) {
        if (hasCompletedQuest(p, q.id())) return ChatColor.GREEN + "Completed";
        if (isQuestActive(p, q.id())) return ChatColor.YELLOW + "Active";
        return ChatColor.RED + "Not Started";
    }

    public String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public List<String> starterNpcIds() {
        Set<String> ids = new HashSet<>();
        for (ConversationQuest q : quests.values()) {
            if (q.hasStarterNpc()) ids.add(q.starterNpcId());
            if (q.hasTargetNpc()) ids.add(q.targetNpcId());
        }
        return new ArrayList<>(ids);
    }

    private void migrateLegacyProgress(UUID ownerId, UUID progressId) {
        if (ownerId.equals(progressId)) return;
        Map<String, QuestProgress> legacy = progressByPlayer.remove(ownerId.toString());
        if (legacy != null && !legacy.isEmpty()) {
            progressByPlayer.computeIfAbsent(progressId.toString(), k -> new ConcurrentHashMap<>()).putAll(legacy);
            saveProgress();
        }
    }

    private UUID resolveProgressId(UUID playerId) {
        if (plugin.getSkyblockLevelManager() == null || plugin.getProfileManager() == null) {
            return playerId;
        }
        var profile = plugin.getProfileManager().getSelectedProfile(playerId);
        UUID progressId = profile != null ? profile.getCanonicalProfileId() : playerId;
        migrateLegacyProgress(playerId, progressId);
        return progressId;
    }

    private UUID resolveProgressId(Player player) {
        if (plugin.getProfileManager() == null) {
            return player.getUniqueId();
        }
        var profile = plugin.getProfileManager().getSelectedProfile(player);
        UUID progressId = profile != null ? profile.getCanonicalProfileId() : player.getUniqueId();
        UUID ownerId = player.getUniqueId();
        migrateLegacyProgress(ownerId, progressId);
        return progressId;
    }

    public enum StartResult { QUEST_NOT_FOUND, QUEST_DISABLED, ALREADY_ACTIVE, ALREADY_COMPLETED, PREREQUISITE_NOT_MET, STARTED }
    public enum CancelResult { QUEST_NOT_FOUND, NOT_ACTIVE, CANCELED }
    public enum CompleteResult { QUEST_NOT_FOUND, NOT_ACTIVE, OBJECTIVES_NOT_MET, COMPLETED }

    public synchronized String znpcsHint(ConversationQuest quest) {
        String command = "/quest talk " + quest.targetNpcId();
        return ChatColor.GRAY + "ZNPCS action: " + ChatColor.AQUA + command;
    }
}
