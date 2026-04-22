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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class QuestManager {
    public enum StartResult {
        STARTED,
        QUEST_NOT_FOUND,
        QUEST_DISABLED,
        ALREADY_ACTIVE,
        ALREADY_COMPLETED
    }

    public enum CompleteResult {
        COMPLETED,
        QUEST_NOT_FOUND,
        QUEST_DISABLED,
        NOT_ACTIVE
    }

    public enum CancelResult {
        CANCELED,
        QUEST_NOT_FOUND,
        NOT_ACTIVE
    }

    private static final String QUESTS_FILE_NAME = "quests.yml";
    private static final String PROGRESS_FILE_NAME = "quest-progress.yml";

    private final GriviencePlugin plugin;
    private final ProfileEconomyService profileEconomy;
    private final File questsFile;
    private final File progressFile;

    private final Map<String, ConversationQuest> quests = new LinkedHashMap<>();
    // Quest progress is keyed by the active profile's canonical UUID so separate profiles do not share state.
    private final Map<UUID, Map<String, QuestProgress>> progressByPlayer = new HashMap<>();

    public QuestManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.profileEconomy = new ProfileEconomyService(plugin);
        this.questsFile = new File(plugin.getDataFolder(), QUESTS_FILE_NAME);
        this.progressFile = new File(plugin.getDataFolder(), PROGRESS_FILE_NAME);
        reload();
    }

    public synchronized void reload() {
        loadQuestDefinitions();
        loadProgress();
        plugin.getLogger().info("Quest system loaded with " + quests.size() + " conversation quest(s).");
    }

    public synchronized Collection<ConversationQuest> quests() {
        return Collections.unmodifiableCollection(new ArrayList<>(quests.values()));
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
                "",
                normalizedId,
                List.of(),
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
        quest.setTargetNpcId(targetNpcId);
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

    public synchronized boolean addRewardCommand(String questId, String rewardCommand) {
        ConversationQuest quest = quest(questId);
        if (quest == null || rewardCommand == null || rewardCommand.isBlank()) {
            return false;
        }
        quest.rewardCommands().add(rewardCommand.trim());
        saveQuestDefinitions();
        return true;
    }

    public synchronized boolean removeRewardCommand(String questId, int index) {
        ConversationQuest quest = quest(questId);
        if (quest == null) {
            return false;
        }
        if (index < 0 || index >= quest.rewardCommands().size()) {
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

    public synchronized StartResult startQuest(Player player, String questId, QuestTriggerSource source, boolean notifyPlayer) {
        ConversationQuest quest = quest(questId);
        if (quest == null) {
            return StartResult.QUEST_NOT_FOUND;
        }
        if (!quest.enabled()) {
            return StartResult.QUEST_DISABLED;
        }

        UUID progressId = resolveProgressId(player);
        
        // Check prerequisites
        for (String preId : quest.prerequisites()) {
            if (!hasCompletedQuest(progressId, preId)) {
                if (notifyPlayer) {
                    ConversationQuest pre = quest(preId);
                    String name = pre != null ? color(pre.displayName()) : preId;
                    player.sendMessage(ChatColor.RED + "You must complete " + name + ChatColor.RED + " first!");
                }
                return StartResult.QUEST_DISABLED;
            }
        }

        QuestProgress progress = progress(progressId, quest.id(), true);
        if (progress.active()) {
            return StartResult.ALREADY_ACTIVE;
        }
        if (!quest.repeatable() && progress.hasCompletedAtLeastOnce()) {
            return StartResult.ALREADY_COMPLETED;
        }

        progress.setActive(true);
        saveProgress();

        if (notifyPlayer) {
            player.sendMessage(ChatColor.GOLD + "[Quest] " + ChatColor.GREEN + "Started: " + color(quest.displayName()));
            if (quest.hasTargetNpc() && quest.objectives().isEmpty()) {
                player.sendMessage(ChatColor.GRAY + "Talk to NPC " + ChatColor.AQUA + quest.targetNpcId() + ChatColor.GRAY + " to complete it.");
            } else if (!quest.objectives().isEmpty()) {
                player.sendMessage(ChatColor.GRAY + "Check your quest log (/quest) for objectives.");
            }
        }

        if (source != QuestTriggerSource.ZNPCS_EVENT) {
            plugin.getLogger().fine("Quest " + quest.id() + " started for " + player.getName() + " via " + source);
        }
        return StartResult.STARTED;
    }

    public synchronized CompleteResult completeQuest(Player player, String questId, QuestTriggerSource source, boolean notifyPlayer) {
        ConversationQuest quest = quest(questId);
        if (quest == null) {
            return CompleteResult.QUEST_NOT_FOUND;
        }
        if (!quest.enabled()) {
            return CompleteResult.QUEST_DISABLED;
        }

        UUID progressId = resolveProgressId(player);
        QuestProgress progress = progress(progressId, quest.id(), false);
        if (progress == null || !progress.active()) {
            return CompleteResult.NOT_ACTIVE;
        }

        progress.setActive(false);
        progress.incrementCompletions();
        saveProgress();

        if (plugin.getSkyblockLevelManager() != null) {
            plugin.getSkyblockLevelManager().recordQuestCompletion(player, quest.id());
        }

        runRewardCommands(player, quest);

        if (notifyPlayer) {
            player.sendMessage(ChatColor.GOLD + "[Quest] " + ChatColor.GREEN + "Completed: " + color(quest.displayName()));
            if (!quest.rewardCommands().isEmpty()) {
                player.sendMessage(ChatColor.GRAY + "Rewards granted: " + ChatColor.YELLOW + quest.rewardCommands().size() + " command(s).");
            }
        }

        if (source != QuestTriggerSource.ZNPCS_EVENT) {
            plugin.getLogger().fine("Quest " + quest.id() + " completed for " + player.getName() + " via " + source);
        }
        return CompleteResult.COMPLETED;
    }

    public synchronized CancelResult cancelQuest(Player player, String questId, boolean notifyPlayer) {
        ConversationQuest quest = quest(questId);
        if (quest == null) {
            return CancelResult.QUEST_NOT_FOUND;
        }

        UUID progressId = resolveProgressId(player);
        QuestProgress progress = progress(progressId, quest.id(), false);
        if (progress == null || !progress.active()) {
            return CancelResult.NOT_ACTIVE;
        }

        progress.setActive(false);
        saveProgress();

        if (notifyPlayer) {
            player.sendMessage(ChatColor.GOLD + "[Quest] " + ChatColor.RED + "Canceled: " + color(quest.displayName()));
        }

        return CancelResult.CANCELED;
    }

    public synchronized void advanceObjective(Player player, QuestObjective.ObjectiveType type, String target, int amount) {
        UUID progressId = resolveProgressId(player);
        Map<String, QuestProgress> playerProgressMap = progressByPlayer.get(progressId);
        if (playerProgressMap == null) return;

        boolean changed = false;
        for (Map.Entry<String, QuestProgress> entry : playerProgressMap.entrySet()) {
            QuestProgress prog = entry.getValue();
            if (!prog.active()) continue;
            ConversationQuest quest = quests.get(entry.getKey());
            if (quest == null) continue;

            for (int i = 0; i < quest.objectives().size(); i++) {
                QuestObjective obj = quest.objectives().get(i);
                if (obj.type() == type && (obj.target().isEmpty() || obj.target().equalsIgnoreCase(target))) {
                    int current = prog.getObjectiveProgress(i);
                    if (current < obj.amount()) {
                        prog.incrementObjectiveProgress(i, amount);
                        changed = true;
                        
                        if (allObjectivesComplete(prog, quest)) {
                            completeQuest(player, quest.id(), QuestTriggerSource.COMMAND, true);
                        }
                    }
                }
            }
        }
        if (changed) saveProgress();
    }

    private boolean allObjectivesComplete(QuestProgress progress, ConversationQuest quest) {
        if (quest.objectives().isEmpty()) return true;
        for (int i = 0; i < quest.objectives().size(); i++) {
            if (!quest.objectives().get(i).isComplete(progress.getObjectiveProgress(i))) {
                return false;
            }
        }
        return true;
    }

    public synchronized int handleNpcConversation(Player player, String npcId, QuestTriggerSource source, boolean notifyPlayer) {
        String normalizedNpcId = ConversationQuest.normalizeNpcId(npcId);
        if (normalizedNpcId.isBlank()) {
            return 0;
        }

        int changedQuests = 0;
        UUID progressId = resolveProgressId(player);
        for (ConversationQuest quest : quests.values()) {
            if (!quest.enabled()) {
                continue;
            }

            boolean starterMatch = quest.matchesStarterNpc(normalizedNpcId);
            boolean targetMatch = quest.matchesTargetNpc(normalizedNpcId);
            if (!starterMatch && !targetMatch) {
                continue;
            }

            QuestProgress progress = progress(progressId, quest.id(), true);
            boolean targetCanStartQuest = !quest.hasStarterNpc() && targetMatch;

            if ((starterMatch || targetCanStartQuest) && !progress.active()) {
                if (!quest.repeatable() && progress.hasCompletedAtLeastOnce()) {
                    continue;
                }
                StartResult startResult = startQuest(player, quest.id(), source, notifyPlayer);
                if (startResult == StartResult.STARTED) {
                    changedQuests++;
                    if (targetMatch) {
                        CompleteResult completeResult = completeQuest(player, quest.id(), source, notifyPlayer);
                        if (completeResult == CompleteResult.COMPLETED) {
                            changedQuests++;
                        }
                    }
                    continue;
                }
            }

            if (targetMatch && progress.active()) {
                CompleteResult completeResult = completeQuest(player, quest.id(), source, notifyPlayer);
                if (completeResult == CompleteResult.COMPLETED) {
                    changedQuests++;
                }
            }
        }

        return changedQuests;
    }

    public synchronized boolean isQuestActive(UUID playerId, String questId) {
        QuestProgress progress = progress(resolveProgressId(playerId), ConversationQuest.normalizeId(questId), false);
        return progress != null && progress.active();
    }

    public synchronized boolean hasCompletedQuest(UUID playerId, String questId) {
        QuestProgress progress = progress(resolveProgressId(playerId), ConversationQuest.normalizeId(questId), false);
        return progress != null && progress.hasCompletedAtLeastOnce();
    }

    public synchronized int completionCount(UUID playerId, String questId) {
        QuestProgress progress = progress(resolveProgressId(playerId), ConversationQuest.normalizeId(questId), false);
        return progress == null ? 0 : progress.completions();
    }

    public synchronized List<ConversationQuest> activeQuests(UUID playerId) {
        Map<String, QuestProgress> playerProgress = progressByPlayer.get(resolveProgressId(playerId));
        if (playerProgress == null || playerProgress.isEmpty()) {
            return List.of();
        }

        List<ConversationQuest> active = new ArrayList<>();
        for (Map.Entry<String, QuestProgress> entry : playerProgress.entrySet()) {
            if (!entry.getValue().active()) {
                continue;
            }
            ConversationQuest quest = quests.get(entry.getKey());
            if (quest != null) {
                active.add(quest);
            }
        }
        active.sort(Comparator.comparing(ConversationQuest::id));
        return active;
    }

    private QuestProgress progress(UUID playerId, String questId, boolean createIfMissing) {
        if (playerId == null) {
            return null;
        }
        String normalizedQuestId = ConversationQuest.normalizeId(questId);
        if (normalizedQuestId.isBlank()) {
            return null;
        }
        Map<String, QuestProgress> playerMap = progressByPlayer.get(playerId);
        if (playerMap == null) {
            if (!createIfMissing) {
                return null;
            }
            playerMap = new HashMap<>();
            progressByPlayer.put(playerId, playerMap);
        }

        QuestProgress questProgress = playerMap.get(normalizedQuestId);
        if (questProgress == null && createIfMissing) {
            questProgress = new QuestProgress();
            playerMap.put(normalizedQuestId, questProgress);
        }
        return questProgress;
    }

    public synchronized boolean isQuestActive(Player player, String questId) {
        UUID progressId = resolveProgressId(player);
        QuestProgress progress = progress(progressId, ConversationQuest.normalizeId(questId), false);
        return progress != null && progress.active();
    }

    public synchronized boolean hasCompletedQuest(Player player, String questId) {
        UUID progressId = resolveProgressId(player);
        QuestProgress progress = progress(progressId, ConversationQuest.normalizeId(questId), false);
        return progress != null && progress.hasCompletedAtLeastOnce();
    }

    public synchronized int completionCount(Player player, String questId) {
        UUID progressId = resolveProgressId(player);
        QuestProgress progress = progress(progressId, ConversationQuest.normalizeId(questId), false);
        return progress == null ? 0 : progress.completions();
    }

    public synchronized List<ConversationQuest> activeQuests(Player player) {
        UUID progressId = resolveProgressId(player);
        Map<String, QuestProgress> playerProgress = progressByPlayer.get(progressId);
        if (playerProgress == null || playerProgress.isEmpty()) {
            return List.of();
        }

        List<ConversationQuest> active = new ArrayList<>();
        for (Map.Entry<String, QuestProgress> entry : playerProgress.entrySet()) {
            if (!entry.getValue().active()) {
                continue;
            }
            ConversationQuest quest = quests.get(entry.getKey());
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
                "elder_start",
                "elder_finish",
                List.of("eco give {player} 1000"),
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
            yaml.set(base + "starter-npc", quest.starterNpcId());
            yaml.set(base + "target-npc", quest.targetNpcId());
            yaml.set(base + "rewards", new ArrayList<>(quest.rewardCommands()));
            yaml.set(base + "prerequisites", new ArrayList<>(quest.prerequisites()));
            
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

            yaml.set(base + "repeatable", quest.repeatable());
            yaml.set(base + "enabled", quest.enabled());
        }
        try {
            yaml.save(questsFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save quests.yml: " + exception.getMessage());
        }
    }

    private void loadProgress() {
        progressByPlayer.clear();

        if (!progressFile.exists()) {
            saveProgress();
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(progressFile);
        ConfigurationSection playersSection = yaml.getConfigurationSection("profiles");
        if (playersSection == null) {
            playersSection = yaml.getConfigurationSection("players");
        }
        if (playersSection == null) {
            return;
        }

        for (String uuidKey : playersSection.getKeys(false)) {
            UUID playerId;
            try {
                playerId = UUID.fromString(uuidKey);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            ConfigurationSection questSection = playersSection.getConfigurationSection(uuidKey + ".quests");
            if (questSection == null) {
                continue;
            }

            Map<String, QuestProgress> questProgressMap = new HashMap<>();
            for (String questIdRaw : questSection.getKeys(false)) {
                String questId = ConversationQuest.normalizeId(questIdRaw);
                if (questId.isBlank()) {
                    continue;
                }
                ConfigurationSection stateSection = questSection.getConfigurationSection(questIdRaw);
                if (stateSection == null) {
                    continue;
                }
                QuestProgress progress = new QuestProgress();
                progress.setActive(stateSection.getBoolean("active", false));
                progress.setCompletions(stateSection.getInt("completions", 0));
                
                ConfigurationSection objProgSection = stateSection.getConfigurationSection("objectives");
                if (objProgSection != null) {
                    for (String idxKey : objProgSection.getKeys(false)) {
                        try {
                            int idx = Integer.parseInt(idxKey);
                            progress.setObjectiveProgress(idx, objProgSection.getInt(idxKey));
                        } catch (NumberFormatException ignored) {}
                    }
                }

                if (progress.active() || progress.completions() > 0) {
                    questProgressMap.put(questId, progress);
                }
            }

            if (!questProgressMap.isEmpty()) {
                progressByPlayer.put(playerId, questProgressMap);
            }
        }
    }

    private void saveProgress() {
        YamlConfiguration yaml = new YamlConfiguration();

        for (Map.Entry<UUID, Map<String, QuestProgress>> playerEntry : progressByPlayer.entrySet()) {
            UUID playerId = playerEntry.getKey();
            for (Map.Entry<String, QuestProgress> questEntry : playerEntry.getValue().entrySet()) {
                QuestProgress progress = questEntry.getValue();
                if (!progress.active() && progress.completions() <= 0) {
                    continue;
                }
                String base = "profiles." + playerId + ".quests." + questEntry.getKey() + ".";
                yaml.set(base + "active", progress.active());
                yaml.set(base + "completions", progress.completions());
                
                Map<Integer, Integer> objProg = progress.getAllObjectiveProgress();
                if (!objProg.isEmpty()) {
                    for (Map.Entry<Integer, Integer> objEntry : objProg.entrySet()) {
                        yaml.set(base + "objectives." + objEntry.getKey(), objEntry.getValue());
                    }
                }
            }
        }

        try {
            yaml.save(progressFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save quest-progress.yml: " + exception.getMessage());
        }
    }

    public String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }

    public String progressLabel(UUID playerId, ConversationQuest quest) {
        QuestProgress progress = progress(resolveProgressId(playerId), quest.id(), false);
        if (progress == null) {
            return ChatColor.GRAY + "Not started";
        }
        if (progress.active()) {
            return ChatColor.YELLOW + "In progress";
        }
        if (progress.hasCompletedAtLeastOnce()) {
            if (quest.repeatable()) {
                return ChatColor.GREEN + "Completed " + progress.completions() + "x";
            }
            return ChatColor.GREEN + "Completed";
        }
        return ChatColor.GRAY + "Not started";
    }

    public String progressLabel(Player player, ConversationQuest quest) {
        QuestProgress progress = progress(resolveProgressId(player), quest.id(), false);
        if (progress == null) {
            return ChatColor.GRAY + "Not started";
        }
        if (progress.active()) {
            return ChatColor.YELLOW + "In progress";
        }
        if (progress.hasCompletedAtLeastOnce()) {
            if (quest.repeatable()) {
                return ChatColor.GREEN + "Completed " + progress.completions() + "x";
            }
            return ChatColor.GREEN + "Completed";
        }
        return ChatColor.GRAY + "Not started";
    }

    private UUID resolveProgressId(Player player) {
        if (player == null) {
            return null;
        }

        UUID ownerId = player.getUniqueId();
        if (ownerId == null) {
            return null;
        }

        io.papermc.Grivience.skyblock.profile.ProfileManager profileManager = plugin.getProfileManager();
        if (profileManager == null) {
            return ownerId;
        }

        io.papermc.Grivience.skyblock.profile.SkyBlockProfile profile = profileManager.getSelectedProfile(player);
        if (profile == null) {
            return ownerId;
        }

        UUID progressId = canonicalProgressId(profile);
        migrateLegacyProgress(ownerId, progressId);
        return progressId == null ? ownerId : progressId;
    }

    private UUID resolveProgressId(UUID playerId) {
        if (playerId == null) {
            return null;
        }

        io.papermc.Grivience.skyblock.profile.ProfileManager profileManager = plugin.getProfileManager();
        if (profileManager == null) {
            return playerId;
        }

        io.papermc.Grivience.skyblock.profile.SkyBlockProfile profile = profileManager.findProfileById(playerId);
        if (profile != null) {
            UUID progressId = canonicalProgressId(profile);
            migrateLegacyProgress(playerId, progressId);
            return progressId == null ? playerId : progressId;
        }

        UUID selectedProfileId = profileManager.getSelectedProfileId(playerId);
        if (selectedProfileId == null) {
            selectedProfileId = profileManager.getFirstProfileId(playerId);
            if (selectedProfileId == null) {
                return playerId;
            }
        }

        io.papermc.Grivience.skyblock.profile.SkyBlockProfile selectedProfile = profileManager.findProfileById(selectedProfileId);
        UUID progressId = canonicalProgressId(selectedProfile);
        migrateLegacyProgress(playerId, progressId);
        return progressId == null ? playerId : progressId;
    }

    private UUID canonicalProgressId(io.papermc.Grivience.skyblock.profile.SkyBlockProfile profile) {
        if (profile == null) {
            return null;
        }
        UUID canonicalProfileId = profile.getCanonicalProfileId();
        return canonicalProfileId != null ? canonicalProfileId : profile.getProfileId();
    }

    private void migrateLegacyProgress(UUID legacyId, UUID progressId) {
        if (legacyId == null || progressId == null || legacyId.equals(progressId)) {
            return;
        }

        Map<String, QuestProgress> legacyProgress = progressByPlayer.get(legacyId);
        if (legacyProgress == null || legacyProgress.isEmpty()) {
            return;
        }

        Map<String, QuestProgress> targetProgress = progressByPlayer.computeIfAbsent(progressId, ignored -> new HashMap<>());
        boolean changed = false;
        for (Map.Entry<String, QuestProgress> entry : legacyProgress.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
                continue;
            }

            String questId = ConversationQuest.normalizeId(entry.getKey());
            QuestProgress existing = targetProgress.get(questId);
            if (existing == null) {
                QuestProgress migrated = new QuestProgress();
                migrated.setActive(entry.getValue().active());
                migrated.setCompletions(entry.getValue().completions());
                targetProgress.put(questId, migrated);
                changed = true;
                continue;
            }

            if (entry.getValue().active() && !existing.active()) {
                existing.setActive(true);
                changed = true;
            }
            if (entry.getValue().completions() > existing.completions()) {
                existing.setCompletions(entry.getValue().completions());
                changed = true;
            }
        }

        progressByPlayer.remove(legacyId);
        saveProgress();
    }

    public synchronized List<String> questIds() {
        List<String> ids = new ArrayList<>(quests.keySet());
        ids.sort(String::compareTo);
        return ids;
    }

    public synchronized List<String> starterNpcIds() {
        List<String> npcIds = new ArrayList<>();
        for (ConversationQuest quest : quests.values()) {
            if (quest.hasStarterNpc()) {
                npcIds.add(quest.starterNpcId());
            }
            if (quest.hasTargetNpc()) {
                npcIds.add(quest.targetNpcId());
            }
        }
        npcIds.sort(String::compareTo);
        return npcIds;
    }

    public synchronized String znpcsHint(ConversationQuest quest) {
        String command = "/quest talk " + quest.targetNpcId();
        return ChatColor.GRAY + "ZNPCS action: " + ChatColor.AQUA + command;
    }
}
