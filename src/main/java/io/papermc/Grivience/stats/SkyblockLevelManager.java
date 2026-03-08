package io.papermc.Grivience.stats;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skyblock.profile.ProfileManager;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

public final class SkyblockLevelManager {
    private static final String FILE_NAME = "levels.yml";

    private final JavaPlugin plugin;
    private io.papermc.Grivience.item.CustomArmorManager armorManager;
    private io.papermc.Grivience.skills.SkyblockSkillManager skillManager;
    private final Map<UUID, Long> xpByPlayer = new HashMap<>();
    private final Map<UUID, Map<String, Long>> countersByPlayer = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> milestoneByPlayer = new HashMap<>();
    private final Map<UUID, Set<String>> objectivesByPlayer = new HashMap<>();
    // Backward-compatible aliases used by newer helper methods.
    private final Map<UUID, Map<String, Integer>> milestoneStagesByPlayer = milestoneByPlayer;
    private final Map<UUID, Set<String>> claimedObjectivesByPlayer = objectivesByPlayer;

    private final List<GuideTrack> tracks = new ArrayList<>();
    private final Map<String, GuideTrack> tracksById = new LinkedHashMap<>();
    private final Map<String, List<GuideTrack>> tracksByCounter = new HashMap<>();
    private final NavigableMap<Integer, List<String>> featureUnlocks = new TreeMap<>();

    private File file;
    private FileConfiguration fileConfig;

    private long xpPerLevel;
    private int maxLevel;
    private boolean notifyXpGain;
    private boolean skillLevelingEnabled;

    // Skyblock-accurate skill leveling XP requirements
    private final long[] skillXpRequirements = new long[61]; // Index 0 unused, 1-60 for levels
    
    // Skyblock XP rewards for skill level ups
    private long skillLevelXpReward1to10;
    private long skillLevelXpReward11to25;
    private long skillLevelXpReward26to50;
    private long skillLevelXpReward51to60;
    
    // Catacombs XP rewards
    private long catacombsLevelXpReward1to39;
    private long catacombsLevelXpReward40to50;
    private long classLevelXpReward;
    private final Map<String, Long> dungeonFirstClearXpByFloorKey = new HashMap<>();
    private long dungeonFirstSRankXp;
    private long dungeonFirstARankXp;
    private long dungeonScore200Xp;
    private long dungeonScore250Xp;
    private long dungeonScore300Xp;

    private long combatKillXp;
    private long miningOreXp;
    private long foragingLogXp;
    private long farmingHarvestXp;
    private long fishingCatchXp;
    private long questCompleteXp;
    private long dungeonCompleteXp;
    private long islandCreateXp;
    private long islandUpgradeXp;

    private long combatActionsPerLevel;
    private long miningActionsPerLevel;
    private long foragingActionsPerLevel;
    private long farmingActionsPerLevel;
    private long fishingActionsPerLevel;
    private long catacombsRunsPerLevel;
    private long classRunsPerLevel;
    private int pseudoSkillCap;
    private int pseudoCatacombsCap;
    private int pseudoClassCap;

    private int bestiaryTierSize;
    private long bestiaryTierXp;
    private int bestiaryMilestoneEvery;
    private int bestiaryMilestoneEveryTiers;
    private long bestiaryMilestoneXp;

    private int healthPerLevel;
    private int strengthPerFiveLevels;
    private int farmingFortunePerLevel;

    // Pet Score and Magical Power
    private int petScorePerCommon = 1;
    private int petScorePerUncommon = 2;
    private int petScorePerRare = 3;
    private int petScorePerEpic = 4;
    private int petScorePerLegendary = 5;
    private int petScorePerMythic = 6;
    private long xpPerPetScore = 3L;
    private long xpPerMagicalPower = 1L;

    public SkyblockLevelManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void setArmorManager(io.papermc.Grivience.item.CustomArmorManager armorManager) {
        this.armorManager = armorManager;
    }

    public void setSkillManager(io.papermc.Grivience.skills.SkyblockSkillManager skillManager) {
        this.skillManager = skillManager;
    }

    public io.papermc.Grivience.item.CustomArmorManager getArmorManager() {
        return armorManager;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public boolean hasMinerFullSet(Player player) {
        if (armorManager == null || player == null) return false;
        int pieces = 0;
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            if (piece == null) continue;
            String setId = armorManager.getArmorSetId(piece);
            if ("miner".equalsIgnoreCase(setId)) pieces++;
        }
        return pieces >= 4;
    }

    public boolean hasDeepcoreFullSet(Player player) {
        if (armorManager == null || player == null) return false;
        int pieces = 0;
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            if (piece == null) continue;
            String setId = armorManager.getArmorSetId(piece);
            if ("deepcore".equalsIgnoreCase(setId)) pieces++;
        }
        return pieces >= 4;
    }

    public void load() {
        loadSettingsFromConfig();

        file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            try {
                file.createNewFile();
            } catch (IOException ignored) {
            }
        }

        fileConfig = YamlConfiguration.loadConfiguration(file);
        xpByPlayer.clear();
        countersByPlayer.clear();
        milestoneByPlayer.clear();
        objectivesByPlayer.clear();

        ConfigurationSection players = fileConfig.getConfigurationSection("players");
        if (players != null) {
            for (String key : players.getKeys(false)) {
                UUID uuid = parseUuid(key);
                if (uuid == null) {
                    continue;
                }
                String base = "players." + key + ".";
                xpByPlayer.put(uuid, clampXp(Math.max(0L, fileConfig.getLong(base + "xp", 0L))));

                ConfigurationSection counters = fileConfig.getConfigurationSection(base + "counters");
                if (counters != null) {
                    Map<String, Long> map = new HashMap<>();
                    for (String counter : counters.getKeys(false)) {
                        map.put(normalize(counter), Math.max(0L, counters.getLong(counter, 0L)));
                    }
                    if (!map.isEmpty()) {
                        countersByPlayer.put(uuid, map);
                    }
                }

                ConfigurationSection milestones = fileConfig.getConfigurationSection(base + "milestones");
                if (milestones != null) {
                    Map<String, Integer> map = new HashMap<>();
                    for (String trackId : milestones.getKeys(false)) {
                        map.put(normalize(trackId), Math.max(0, milestones.getInt(trackId, 0)));
                    }
                    if (!map.isEmpty()) {
                        milestoneByPlayer.put(uuid, map);
                    }
                }

                List<String> objectives = fileConfig.getStringList(base + "objectives");
                if (!objectives.isEmpty()) {
                    Set<String> set = new LinkedHashSet<>();
                    for (String objective : objectives) {
                        String normalized = normalize(objective);
                        if (!normalized.isBlank()) {
                            set.add(normalized);
                        }
                    }
                    if (!set.isEmpty()) {
                        objectivesByPlayer.put(uuid, set);
                    }
                }
            }
        }

        ConfigurationSection legacy = fileConfig.getConfigurationSection("levels");
        if (legacy != null) {
            for (String key : legacy.getKeys(false)) {
                UUID uuid = parseUuid(key);
                if (uuid == null || xpByPlayer.containsKey(uuid)) {
                    continue;
                }
                xpByPlayer.put(uuid, clampXp(Math.max(0L, legacy.getLong(key, 0L))));
            }
            fileConfig.set("levels", null);
            save();
        }
    }

    public void save() {
        if (fileConfig == null) {
            return;
        }

        fileConfig.set("players", null);
        Set<UUID> all = new LinkedHashSet<>();
        all.addAll(xpByPlayer.keySet());
        all.addAll(countersByPlayer.keySet());
        all.addAll(milestoneByPlayer.keySet());
        all.addAll(objectivesByPlayer.keySet());

        for (UUID playerId : all) {
            String base = "players." + playerId + ".";
            fileConfig.set(base + "xp", getXp(playerId));

            Map<String, Long> counters = countersByPlayer.get(playerId);
            if (counters != null) {
                for (Map.Entry<String, Long> entry : counters.entrySet()) {
                    if (entry.getValue() > 0L) {
                        fileConfig.set(base + "counters." + entry.getKey(), entry.getValue());
                    }
                }
            }

            Map<String, Integer> milestones = milestoneByPlayer.get(playerId);
            if (milestones != null) {
                for (Map.Entry<String, Integer> entry : milestones.entrySet()) {
                    if (entry.getValue() > 0) {
                        fileConfig.set(base + "milestones." + entry.getKey(), entry.getValue());
                    }
                }
            }

            Set<String> objectives = objectivesByPlayer.get(playerId);
            if (objectives != null && !objectives.isEmpty()) {
                List<String> list = new ArrayList<>(objectives);
                list.sort(String::compareTo);
                fileConfig.set(base + "objectives", list);
            }
        }

        try {
            fileConfig.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save levels.yml: " + exception.getMessage());
        }
    }

    public long getXp(UUID playerId) {
        return xpByPlayer.getOrDefault(playerId, 0L);
    }

    public long getXp(Player player) {
        UUID profileId = resolveProfileId(player);
        return profileId == null ? 0L : getXp(profileId);
    }

    public int getLevel(UUID playerId) {
        return (int) Math.floor(getXp(playerId) / (double) Math.max(1L, xpPerLevel));
    }

    public int getLevel(Player player) {
        UUID profileId = resolveProfileId(player);
        return profileId == null ? 0 : getLevel(profileId);
    }

    public ChatColor getLevelColor(int level) {
        if (level < 40) return ChatColor.GRAY;
        if (level < 80) return ChatColor.WHITE;
        if (level < 120) return ChatColor.GREEN;
        if (level < 160) return ChatColor.BLUE;
        if (level < 200) return ChatColor.DARK_PURPLE;
        if (level < 240) return ChatColor.GOLD;
        if (level < 280) return ChatColor.LIGHT_PURPLE;
        if (level < 320) return ChatColor.AQUA;
        if (level < 360) return ChatColor.YELLOW; // Orange fallback
        if (level < 400) return ChatColor.RED;
        if (level < 440) return ChatColor.DARK_RED;
        if (level < 480) return ChatColor.DARK_BLUE;
        if (level < 520) return ChatColor.DARK_GREEN;
        return ChatColor.YELLOW; // Golden Flirtase
    }

    public long getXpPerLevel() {
        return xpPerLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public boolean isAtMaxLevel(UUID playerId) {
        return maxLevel > 0 && getLevel(playerId) >= maxLevel;
    }

    public boolean isAtMaxLevel(Player player) {
        UUID profileId = resolveProfileId(player);
        return profileId != null && isAtMaxLevel(profileId);
    }

    public double getProgress(UUID playerId) {
        return isAtMaxLevel(playerId) ? 1.0D : xpIntoCurrentLevel(playerId) / (double) Math.max(1L, xpPerLevel);
    }

    public double getProgress(Player player) {
        UUID profileId = resolveProfileId(player);
        return profileId == null ? 0.0D : getProgress(profileId);
    }

    public long xpIntoCurrentLevel(UUID playerId) {
        return getXp(playerId) % Math.max(1L, xpPerLevel);
    }

    public long xpIntoCurrentLevel(Player player) {
        UUID profileId = resolveProfileId(player);
        return profileId == null ? 0L : xpIntoCurrentLevel(profileId);
    }

    public long xpToNextLevel(UUID playerId) {
        if (isAtMaxLevel(playerId)) {
            return 0L;
        }
        long per = Math.max(1L, xpPerLevel);
        long into = xpIntoCurrentLevel(playerId);
        return into == 0L ? per : per - into;
    }

    public long xpToNextLevel(Player player) {
        UUID profileId = resolveProfileId(player);
        return profileId == null ? 0L : xpToNextLevel(profileId);
    }

    public int getHealthBonus(UUID playerId) {
        return Math.max(0, getLevel(playerId)) * Math.max(0, healthPerLevel);
    }

    public int getHealthBonus(Player player) {
        UUID profileId = resolveProfileId(player);
        return profileId == null ? 0 : getHealthBonus(profileId);
    }

    public int getStrengthBonus(UUID playerId) {
        int level = Math.max(0, getLevel(playerId));
        return (level / 5) * Math.max(0, strengthPerFiveLevels);
    }

    public int getStrengthBonus(Player player) {
        UUID profileId = resolveProfileId(player);
        return profileId == null ? 0 : getStrengthBonus(profileId);
    }

    public int getFarmingFortuneBonus(UUID playerId) {
        return Math.max(0, getLevel(playerId)) * Math.max(0, farmingFortunePerLevel);
    }

    public int getFarmingFortuneBonus(Player player) {
        UUID profileId = resolveProfileId(player);
        return profileId == null ? 0 : getFarmingFortuneBonus(profileId);
    }

    public FeatureUnlock nextFeatureUnlock(UUID playerId) {
        int level = getLevel(playerId);
        Map.Entry<Integer, List<String>> next = featureUnlocks.higherEntry(level);
        return next == null ? null : new FeatureUnlock(next.getKey(), List.copyOf(next.getValue()));
    }

    public FeatureUnlock nextFeatureUnlock(Player player) {
        UUID profileId = resolveProfileId(player);
        return profileId == null ? null : nextFeatureUnlock(profileId);
    }

    public List<String> featureUnlocksAtLevel(int level) {
        List<String> list = featureUnlocks.get(level);
        return list == null ? List.of() : List.copyOf(list);
    }

    public void updatePetScore(Player player, int score) {
        if (player == null) return;
        UUID profileId = resolveProfileId(player);
        if (profileId == null) return;
        
        long oldScore = getCounter(profileId, "pet_score");
        if (score == oldScore) return;
        
        setCounter(profileId, "pet_score", score);
        long xpReward = score * xpPerPetScore;
        // Pet Score XP is "unlimited" and doesn't use the standard category room logic, 
        // but we track it in misc category for the GUI breakdown.
        updateUnlimitedXpSource(player, "misc.pet_score", "misc", xpReward, "Pet Score");
    }

    public void updateMagicalPower(Player player, int power) {
        if (player == null) return;
        UUID profileId = resolveProfileId(player);
        if (profileId == null) return;
        
        long oldPower = getCounter(profileId, "magical_power");
        if (power == oldPower) return;
        
        setCounter(profileId, "magical_power", power);
        long xpReward = power * xpPerMagicalPower;
        updateUnlimitedXpSource(player, "core.magical_power", "core", xpReward, "Magical Power");
    }

    private void updateUnlimitedXpSource(Player player, String objectiveId, String categoryId, long totalXp, String reason) {
        UUID profileId = resolveProfileId(player);
        if (profileId == null) return;
        
        String counterKey = categoryCounterKey(categoryId);
        long currentCategoryXp = getCounter(profileId, counterKey);
        
        // We need to know how much XP this specific objective ALREADY gave
        String internalKey = "unlimited_xp." + normalizeKey(objectiveId);
        long alreadyAwarded = getCounter(profileId, internalKey);
        
        long delta = totalXp - alreadyAwarded;
        if (delta == 0) return;
        
        setCounter(profileId, internalKey, totalXp);
        setCounter(profileId, counterKey, currentCategoryXp + delta);
        
        addXp(player, delta, reason, false);
    }

    public void addXp(Player player, long amount) {
        if (player != null) {
            addXp(player, amount, "Skyblock", false);
        }
    }

    public void addXp(UUID playerId, long amount) {
        if (playerId == null || amount <= 0L) {
            return;
        }
        long old = getXp(playerId);
        long updated = clampXp(old + amount);
        if (updated != old) {
            xpByPlayer.put(playerId, updated);
        }
    }

    /**
     * Set a player's total Skyblock XP directly (admin use).
     */
    public void setXp(UUID playerId, long amount) {
        if (playerId == null) {
            return;
        }
        long clampedAmount = clampXp(Math.max(0L, amount));
        xpByPlayer.put(playerId, clampedAmount);
    }

    /**
     * Set a player's total Skyblock XP directly (admin use).
     */
    public void setXp(Player player, long amount) {
        if (player == null) {
            return;
        }
        UUID profileId = resolveProfileId(player);
        if (profileId != null) {
            setXp(profileId, amount);
        }
    }

    public void addXp(Player player, long amount, String reason, boolean showGainToast) {
        if (player == null || amount <= 0L) {
            return;
        }

        UUID playerId = resolveProfileId(player);
        if (playerId == null) {
            return;
        }
        long oldXp = getXp(playerId);
        long room = Math.max(0L, xpCap() - oldXp);
        long applied = Math.min(amount, room);
        if (applied <= 0L) {
            return;
        }

        int oldLevel = getLevel(playerId);
        long updated = clampXp(oldXp + applied);
        xpByPlayer.put(playerId, updated);

        if (showGainToast && notifyXpGain) {
            String cleanReason = reason == null || reason.isBlank() ? "Skyblock" : reason;
            player.sendActionBar(ChatColor.AQUA + "+" + fmt(applied) + " Skyblock XP " + ChatColor.DARK_GRAY + "(" + cleanReason + ")");
        }

        int newLevel = getLevel(playerId);
        if (newLevel > oldLevel) {
            for (int level = oldLevel + 1; level <= newLevel; level++) {
                player.sendMessage(ChatColor.DARK_GRAY + "--------------------------------------------------");
                player.sendMessage(ChatColor.GOLD + "  [Skyblock] Level Up!");
                player.sendMessage("");
                player.sendMessage(ChatColor.YELLOW + "  " + getLevelColor(level) + "Skyblock Level " + level);
                player.sendMessage("");
                
                int healthGain = Math.max(0, healthPerLevel);
                int strengthGain = level % 5 == 0 ? Math.max(0, strengthPerFiveLevels) : 0;
                if (healthGain > 0) {
                    player.sendMessage(ChatColor.GRAY + "  \u25cf " + ChatColor.RED + "+" + healthGain + " Health");
                }
                if (strengthGain > 0) {
                    player.sendMessage(ChatColor.GRAY + "  \u25cf " + ChatColor.RED + "+" + strengthGain + " Strength");
                }

                List<String> unlocks = featureUnlocksAtLevel(level);
                if (!unlocks.isEmpty()) {
                    player.sendMessage("");
                    for (String unlock : unlocks) {
                        player.sendMessage(ChatColor.GRAY + "  \u25cf " + ChatColor.AQUA + "Unlocked: " + unlock);
                    }
                }
                player.sendMessage(ChatColor.DARK_GRAY + "--------------------------------------------------");
            }
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
        }
    }

    public void recordCombatKill(Player player, EntityType type) {
        if (player == null || type == null) {
            return;
        }
        long kills = incrementCounter(player, "combat_kills", 1L);
        evaluatePseudoSkill(player, "combat", kills, combatActionsPerLevel, pseudoSkillCap);
        
        if (skillManager != null) {
            skillManager.addXp(player, io.papermc.Grivience.skills.SkyblockSkill.COMBAT, 1);
            skillManager.addXp(player, io.papermc.Grivience.skills.SkyblockSkill.HUNTING, 1);
        }

        if (combatKillXp > 0L) {
            awardCategoryXp(player, "skill", combatKillXp, "Combat", true);
        }

        String mob = normalize(type.name());
        long mobKills = incrementCounter(player, "bestiary_kills." + mob, 1L);
        awardObjectiveXp(player, "slaying.discovery." + mob, "slaying", 2L, "Bestiary Discovery", false);
        evaluateBestiary(player, mobKills);
        if (isBoss(type)) {
            awardObjectiveXp(player, "slaying.boss." + mob, "slaying", 20L, "Boss Defeated", true);
        }
    }

    public void recordMiningOre(Player player, Material material) {
        if (player == null || material == null) {
            return;
        }
        long mined = incrementCounter(player, "mining_ores", 1L);
        evaluatePseudoSkill(player, "mining", mined, miningActionsPerLevel, pseudoSkillCap);
        
        if (skillManager != null) {
            skillManager.addXp(player, io.papermc.Grivience.skills.SkyblockSkill.MINING, 1);
        }

        if (miningOreXp > 0L) {
            awardCategoryXp(player, "skill", miningOreXp, "Mining", true);
        }
        awardObjectiveXp(player, "core.collection." + normalize(material.name()), "core", 2L, "Collection Discovery", false);
        if (material == Material.ANCIENT_DEBRIS) {
            awardObjectiveXp(player, "core.collection.ancient_debris", "core", 10L, "Ancient Debris Discovery", true);
        }
    }

    /**
     * Returns a Skyblock-style pseudo skill level for the active profile.
     * <p>
     * These levels are derived from action counters (e.g. mining_ores) and are stored
     * as {@code pseudo_level.<skillId>}.
     */
    public int getPseudoSkillLevel(Player player, String skillId) {
        if (player == null || skillId == null || skillId.isBlank()) {
            return 0;
        }
        UUID profileId = resolveProfileId(player);
        if (profileId == null) {
            return 0;
        }
        String counterKey = "pseudo_level." + normalizeKey(skillId);
        return (int) Math.max(0L, getCounter(profileId, counterKey));
    }

    public long getSkillActionsPerLevel(String skillId) {
        String normalized = normalizeKey(skillId);
        if (normalized.isBlank()) {
            return 100L;
        }
        return switch (normalized) {
            case "combat", "hunting" -> Math.max(1L, combatActionsPerLevel);
            case "mining" -> Math.max(1L, miningActionsPerLevel);
            case "foraging" -> Math.max(1L, foragingActionsPerLevel);
            case "farming" -> Math.max(1L, farmingActionsPerLevel);
            case "fishing" -> Math.max(1L, fishingActionsPerLevel);
            case "dungeoneering" -> Math.max(1L, catacombsRunsPerLevel);
            case "enchanting" -> 50L;
            case "alchemy" -> 20L;
            case "carpentry", "taming" -> 100L;
            default -> 100L;
        };
    }

    /**
     * Adds bonus "mined ore" actions for the active profile without triggering collection discovery.
     * Intended for mining streak rewards, scrolls, and deep-layer multipliers.
     */
    public long addMiningActions(Player player, long bonusOres) {
        if (player == null || bonusOres <= 0L) {
            return 0L;
        }
        long mined = incrementCounter(player, "mining_ores", bonusOres);
        evaluatePseudoSkill(player, "mining", mined, miningActionsPerLevel, pseudoSkillCap);
        return mined;
    }

    public void recordForagingLog(Player player, Material material) {
        if (player == null || material == null) {
            return;
        }
        long logs = incrementCounter(player, "foraging_logs", 1L);
        evaluatePseudoSkill(player, "foraging", logs, foragingActionsPerLevel, pseudoSkillCap);
        
        if (skillManager != null) {
            skillManager.addXp(player, io.papermc.Grivience.skills.SkyblockSkill.FORAGING, 1);
        }

        if (foragingLogXp > 0L) {
            awardCategoryXp(player, "skill", foragingLogXp, "Foraging", true);
        }
        awardObjectiveXp(player, "core.collection." + normalize(material.name()), "core", 2L, "Collection Discovery", false);
    }

    public void recordFarmingHarvest(Player player, Material material) {
        if (player == null || material == null) {
            return;
        }
        long harvests = incrementCounter(player, "farming_harvests", 1L);
        evaluatePseudoSkill(player, "farming", harvests, farmingActionsPerLevel, pseudoSkillCap);
        
        if (skillManager != null) {
            skillManager.addXp(player, io.papermc.Grivience.skills.SkyblockSkill.FARMING, 1);
        }

        if (farmingHarvestXp > 0L) {
            awardCategoryXp(player, "skill", farmingHarvestXp, "Farming", true);
        }
        awardObjectiveXp(player, "core.collection." + normalize(material.name()), "core", 2L, "Collection Discovery", false);
    }

    public void recordFishingCatch(Player player) {
        if (player == null) {
            return;
        }
        long catches = incrementCounter(player, "fishing_catches", 1L);
        evaluatePseudoSkill(player, "fishing", catches, fishingActionsPerLevel, pseudoSkillCap);
        
        if (skillManager != null) {
            skillManager.addXp(player, io.papermc.Grivience.skills.SkyblockSkill.FISHING, 1);
        }

        if (fishingCatchXp > 0L) {
            awardCategoryXp(player, "skill", fishingCatchXp, "Fishing", true);
        }
        awardObjectiveXp(player, "misc.first_fish", "misc", 5L, "First Catch", false);
    }

    public void recordEnchanting(Player player, int levelsSpent) {
        if (player == null || levelsSpent <= 0) {
            return;
        }
        long actions = incrementCounter(player, "enchanting_actions", (long) levelsSpent);
        evaluatePseudoSkill(player, "enchanting", actions, 50, pseudoSkillCap);
        
        if (skillManager != null) {
            skillManager.addXp(player, io.papermc.Grivience.skills.SkyblockSkill.ENCHANTING, levelsSpent);
        }
        
        awardCategoryXp(player, "skill", (long) levelsSpent / 2, "Enchanting", true);
    }

    public void recordAlchemy(Player player, int potionCount) {
        if (player == null || potionCount <= 0) {
            return;
        }
        long actions = incrementCounter(player, "alchemy_actions", (long) potionCount);
        evaluatePseudoSkill(player, "alchemy", actions, 20, pseudoSkillCap);
        
        if (skillManager != null) {
            skillManager.addXp(player, io.papermc.Grivience.skills.SkyblockSkill.ALCHEMY, potionCount * 5);
        }
        
        awardCategoryXp(player, "skill", (long) potionCount * 2, "Alchemy", true);
    }

    public void recordCarpentry(Player player, int amount) {
        if (player == null || amount <= 0) {
            return;
        }
        long actions = incrementCounter(player, "carpentry_actions", (long) amount);
        evaluatePseudoSkill(player, "carpentry", actions, 100, pseudoSkillCap);
        
        if (skillManager != null) {
            skillManager.addXp(player, io.papermc.Grivience.skills.SkyblockSkill.CARPENTRY, amount);
        }
        
        awardCategoryXp(player, "skill", (long) amount, "Carpentry", true);
    }

    public void recordQuestCompletion(Player player, String questId) {
        if (player == null) {
            return;
        }
        incrementCounter(player, "quest_completions", 1L);
        if (questCompleteXp > 0L) {
            awardCategoryXp(player, "story", questCompleteXp, "Quest", true);
        }
        if (questId != null && !questId.isBlank()) {
            awardObjectiveXp(player, "story.quest." + normalize(questId), "story", 20L, "Quest Completion", true);
        }
    }

    public void recordDungeonCompletion(Player player, String floorId, String grade, int score) {
        if (player == null) {
            return;
        }
        long clears = incrementCounter(player, "dungeon_completions", 1L);
        evaluatePseudoDungeon(player, clears);
        
        if (skillManager != null) {
            skillManager.addXp(player, io.papermc.Grivience.skills.SkyblockSkill.DUNGEONEERING, 1);
        }

        if (dungeonCompleteXp > 0L) {
            awardCategoryXp(player, "dungeon", dungeonCompleteXp, "Dungeon", true);
        }

        if (floorId != null && !floorId.isBlank()) {
            long xp = firstClearReward(floorId);
            if (xp > 0L) {
                awardObjectiveXp(player, "dungeon.floor." + normalize(floorId), "dungeon", xp, "First Floor Clear", true);
            }
        }

        String rank = grade == null ? "" : grade.trim().toUpperCase(Locale.ROOT);
        if ("S".equals(rank)) {
            awardObjectiveXp(player, "dungeon.rank.s", "dungeon", dungeonFirstSRankXp, "First S Rank", true);
        } else if ("A".equals(rank)) {
            awardObjectiveXp(player, "dungeon.rank.a", "dungeon", dungeonFirstARankXp, "First A Rank", false);
        }

        if (score >= 300) {
            awardObjectiveXp(player, "dungeon.score.300", "dungeon", dungeonScore300Xp, "300 Score Run", false);
        }
        if (score >= 250) {
            awardObjectiveXp(player, "dungeon.score.250", "dungeon", dungeonScore250Xp, "250 Score Run", false);
        }
        if (score >= 200) {
            awardObjectiveXp(player, "dungeon.score.200", "dungeon", dungeonScore200Xp, "200 Score Run", false);
        }
    }

    public void recordIslandCreated(Player player) {
        if (player == null) {
            return;
        }
        incrementCounter(player, "islands_created", 1L);
        
        if (skillManager != null) {
            skillManager.addXp(player, io.papermc.Grivience.skills.SkyblockSkill.CARPENTRY, 1);
        }

        if (islandCreateXp > 0L) {
            awardCategoryXp(player, "core", islandCreateXp, "Island", true);
        }
        awardObjectiveXp(player, "core.island.create", "core", 25L, "Island Created", true);
    }

    public void recordIslandUpgrade(Player player, int newLevel) {
        if (player == null) {
            return;
        }
        incrementCounter(player, "island_upgrades", 1L);
        
        if (skillManager != null) {
            skillManager.addXp(player, io.papermc.Grivience.skills.SkyblockSkill.CARPENTRY, 1);
        }

        if (islandUpgradeXp > 0L) {
            awardCategoryXp(player, "core", islandUpgradeXp, "Island Upgrade", true);
        }
        int bonus = Math.max(10, Math.min(80, 10 + (Math.max(1, newLevel) * 5)));
        awardObjectiveXp(player, "core.island.upgrade." + Math.max(1, newLevel), "core", bonus, "Island Tier " + Math.max(1, newLevel), true);
    }

    public boolean isMiningMaterial(Material material) {
        if (material == null) {
            return false;
        }
        String name = material.name();
        if (name.endsWith("_ORE") || material == Material.ANCIENT_DEBRIS) {
            return true;
        }
        
        return switch (material) {
            case GLOWSTONE, OBSIDIAN, CRYING_OBSIDIAN, GILDED_BLACKSTONE,
                 AMETHYST_CLUSTER, BUDDING_AMETHYST, AMETHYST_BLOCK,
                 RAW_IRON_BLOCK, RAW_GOLD_BLOCK, RAW_COPPER_BLOCK,
                 SCULK, SCULK_CATALYST, SCULK_SHRIEKER, SCULK_VEIN -> true;
            default -> false;
        };
    }

    public boolean isForagingMaterial(Material material) {
        if (material == null) {
            return false;
        }
        String name = material.name();
        return name.endsWith("_LOG") || name.endsWith("_WOOD") || name.endsWith("_STEM") || name.endsWith("_HYPHAE");
    }

    public boolean isFarmingMaterial(Material material) {
        if (material == null) {
            return false;
        }
        return switch (material) {
            case WHEAT, CARROTS, POTATOES, BEETROOTS, NETHER_WART, COCOA, SUGAR_CANE,
                    MELON, MELON_STEM, PUMPKIN, PUMPKIN_STEM, CACTUS, BAMBOO,
                    SWEET_BERRY_BUSH, GLOW_BERRIES, CHORUS_PLANT, CHORUS_FLOWER -> true;
            default -> false;
        };
    }

    public List<GuideTrack> tracks() {
        return Collections.unmodifiableList(tracks);
    }

    public GuideTrack track(String trackId) {
        return tracksById.get(normalize(trackId));
    }

    public GuideProgress progressFor(UUID playerId, String trackId) {
        GuideTrack track = track(trackId);
        return track == null ? null : progressFor(playerId, track);
    }

    public GuideProgress progressFor(Player player, String trackId) {
        UUID profileId = resolveProfileId(player);
        return profileId == null ? null : progressFor(profileId, trackId);
    }

    public GuideProgress progressFor(UUID playerId, GuideTrack track) {
        long counter = getCounter(playerId, track.counterKey());
        int claimed = Math.max(0, getMilestoneStage(playerId, track.id()));
        int total = track.milestones().size();

        if (claimed >= total) {
            long threshold = total == 0 ? 0L : track.milestones().get(total - 1);
            return new GuideProgress(track, counter, total, total, threshold, threshold, 0L, 1.0D, true);
        }

        long previous = claimed <= 0 ? 0L : track.milestones().get(claimed - 1);
        long next = track.milestones().get(claimed);
        long reward = track.rewards().get(claimed);
        double progress = next <= previous ? 1.0D : Math.max(0.0D, Math.min(1.0D, (counter - previous) / (double) (next - previous)));

        return new GuideProgress(track, counter, claimed, total, previous, next, reward, progress, false);
    }

    public GuideProgress progressFor(Player player, GuideTrack track) {
        UUID profileId = resolveProfileId(player);
        return profileId == null || track == null ? null : progressFor(profileId, track);
    }

    public int totalMilestones() {
        int total = 0;
        for (GuideTrack track : tracks) {
            total += track.milestones().size();
        }
        return total;
    }

    public int claimedMilestones(UUID playerId) {
        int claimed = 0;
        for (GuideTrack track : tracks) {
            claimed += Math.min(track.milestones().size(), Math.max(0, getMilestoneStage(playerId, track.id())));
        }
        return claimed;
    }

    public int claimedMilestones(Player player) {
        UUID profileId = resolveProfileId(player);
        return profileId == null ? 0 : claimedMilestones(profileId);
    }

    public long getCounter(UUID playerId, String counterKey) {
        Map<String, Long> counters = countersByPlayer.get(playerId);
        return counters == null ? 0L : counters.getOrDefault(normalize(counterKey), 0L);
    }

    public long getCounter(Player player, String counterKey) {
        if (player == null || counterKey == null || counterKey.isBlank()) {
            return 0L;
        }
        UUID profileId = resolveProfileId(player);
        return profileId == null ? 0L : getCounter(profileId, counterKey);
    }

    public long addToCounter(Player player, String counterKey, long amount) {
        return incrementCounter(player, counterKey, amount);
    }

    private long incrementCounter(Player player, String counterKey, long amount) {
        if (player == null || amount <= 0L) {
            return 0L;
        }
        UUID playerId = resolveProfileId(player);
        if (playerId == null) {
            return 0L;
        }
        long updated = Math.max(0L, getCounter(playerId, counterKey) + amount);
        setCounter(playerId, counterKey, updated);
        evaluateMilestones(player, counterKey, updated);
        return updated;
    }

    // Compatibility wrappers used by other helper blocks in this file.
    private long incrementCounterAndGet(Player player, String counterKey, long amount) {
        return incrementCounter(player, counterKey, amount);
    }

    private void evaluatePseudoSkill(Player player, String skillId, long actions, long actionsPerLevel, int cap) {
        evaluatePseudoSkillLevels(player, skillId, actions, actionsPerLevel, cap);
    }

    private void evaluateBestiary(Player player, long mobKills) {
        evaluateBestiaryTiers(player, mobKills);
    }

    private boolean isBoss(EntityType type) {
        return isBossEntity(type);
    }

    private void evaluatePseudoDungeon(Player player, long clears) {
        evaluatePseudoDungeonLevels(player, clears);
    }

    private void setCounter(UUID playerId, String counterKey, long value) {
        countersByPlayer.computeIfAbsent(playerId, ignored -> new HashMap<>())
                .put(normalize(counterKey), Math.max(0L, value));
    }

    private int getMilestoneStage(UUID playerId, String trackId) {
        Map<String, Integer> map = milestoneByPlayer.get(playerId);
        return map == null ? 0 : map.getOrDefault(normalize(trackId), 0);
    }

    private void setMilestoneStage(UUID playerId, String trackId, int stage) {
        milestoneByPlayer.computeIfAbsent(playerId, ignored -> new HashMap<>())
                .put(normalize(trackId), Math.max(0, stage));
    }

    private void evaluateMilestones(Player player, String counterKey, long counterValue) {
        List<GuideTrack> matching = tracksByCounter.get(normalize(counterKey));
        if (matching == null || matching.isEmpty()) {
            return;
        }

        UUID playerId = resolveProfileId(player);
        if (playerId == null) {
            return;
        }
        for (GuideTrack track : matching) {
            int stage = getMilestoneStage(playerId, track.id());
            while (stage < track.milestones().size() && counterValue >= track.milestones().get(stage)) {
                long reward = track.rewards().get(stage);
                stage++;
                setMilestoneStage(playerId, track.id(), stage);
                if (reward > 0L) {
                    addXp(player, reward, ChatColor.stripColor(color(track.displayName())) + " Milestone", false);
                }
                player.sendMessage(ChatColor.GOLD + "[Skyblock Guide] " + ChatColor.YELLOW + color(track.displayName())
                        + ChatColor.GRAY + " milestone " + ChatColor.AQUA + stage + ChatColor.GRAY + " completed"
                        + (reward > 0L ? ChatColor.GRAY + " (+" + fmt(reward) + " XP)" : ""));
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7F, 1.25F);
            }
        }
    }

    private void loadSettingsFromConfig() {
        FileConfiguration config = plugin.getConfig();
        xpPerLevel = Math.max(1L, config.getLong("skyblock-leveling.xp-per-level", 100L));
        maxLevel = Math.max(0, config.getInt("skyblock-leveling.max-level", 521));
        notifyXpGain = config.getBoolean("skyblock-leveling.notify-xp-gain", true);
        skillLevelingEnabled = config.getBoolean("skyblock-leveling.skill-leveling.enabled", true);

        // Load Skyblock-accurate skill leveling XP requirements
        loadSkillXpRequirements(config);
        
        // Load Skyblock XP rewards for skill level ups
        ConfigurationSection skillRewardsSection = config.getConfigurationSection("skyblock-leveling.skill-level-xp-rewards");
        skillLevelXpReward1to10 = skillRewardsSection == null ? 5L : Math.max(0L, skillRewardsSection.getLong("level-1-to-10", 5L));
        skillLevelXpReward11to25 = skillRewardsSection == null ? 10L : Math.max(0L, skillRewardsSection.getLong("level-11-to-25", 10L));
        skillLevelXpReward26to50 = skillRewardsSection == null ? 20L : Math.max(0L, skillRewardsSection.getLong("level-26-to-50", 20L));
        skillLevelXpReward51to60 = skillRewardsSection == null ? 30L : Math.max(0L, skillRewardsSection.getLong("level-51-to-60", 30L));
        
        // Load Catacombs XP rewards
        ConfigurationSection dungeonSection = config.getConfigurationSection("skyblock-leveling.dungeons");
        ConfigurationSection catacombsRewards = dungeonSection == null ? null : dungeonSection.getConfigurationSection("catacombs-level-xp-rewards");
        catacombsLevelXpReward1to39 = catacombsRewards == null ? 20L : Math.max(0L, catacombsRewards.getLong("level-1-to-39", 20L));
        catacombsLevelXpReward40to50 = catacombsRewards == null ? 40L : Math.max(0L, catacombsRewards.getLong("level-40-to-50", 40L));
        classLevelXpReward = dungeonSection == null ? 4L : Math.max(0L, dungeonSection.getLong("class-level-xp-reward", 4L));
        catacombsRunsPerLevel = Math.max(1L, dungeonSection == null ? 3L : dungeonSection.getLong("catacombs-runs-per-level", 3L));
        classRunsPerLevel = Math.max(1L, dungeonSection == null ? 4L : dungeonSection.getLong("class-runs-per-level", 4L));
        loadDungeonFirstClearRewards(dungeonSection == null ? null : dungeonSection.getConfigurationSection("floor-completion-xp"));
        ConfigurationSection dungeonObjectiveXp = dungeonSection == null ? null : dungeonSection.getConfigurationSection("objective-xp");
        dungeonFirstSRankXp = dungeonObjectiveXp == null ? 20L : Math.max(0L, dungeonObjectiveXp.getLong("first-s-rank", 20L));
        dungeonFirstARankXp = dungeonObjectiveXp == null ? 10L : Math.max(0L, dungeonObjectiveXp.getLong("first-a-rank", 10L));
        dungeonScore200Xp = dungeonObjectiveXp == null ? 5L : Math.max(0L, dungeonObjectiveXp.getLong("score-200", 5L));
        dungeonScore250Xp = dungeonObjectiveXp == null ? 10L : Math.max(0L, dungeonObjectiveXp.getLong("score-250", 10L));
        dungeonScore300Xp = dungeonObjectiveXp == null ? 15L : Math.max(0L, dungeonObjectiveXp.getLong("score-300", 15L));

        combatKillXp = Math.max(0L, config.getLong("skyblock-leveling.action-xp.combat-kill", 0L));
        miningOreXp = Math.max(0L, config.getLong("skyblock-leveling.action-xp.mining-ore", 0L));
        foragingLogXp = Math.max(0L, config.getLong("skyblock-leveling.action-xp.foraging-log", 0L));
        farmingHarvestXp = Math.max(0L, config.getLong("skyblock-leveling.action-xp.farming-harvest", 0L));
        fishingCatchXp = Math.max(0L, config.getLong("skyblock-leveling.action-xp.fishing-catch", 0L));
        questCompleteXp = Math.max(0L, config.getLong("skyblock-leveling.action-xp.quest-complete", 0L));
        dungeonCompleteXp = Math.max(0L, config.getLong("skyblock-leveling.action-xp.dungeon-complete", 0L));
        islandCreateXp = Math.max(0L, config.getLong("skyblock-leveling.action-xp.island-create", 0L));
        islandUpgradeXp = Math.max(0L, config.getLong("skyblock-leveling.action-xp.island-upgrade", 0L));

        combatActionsPerLevel = Math.max(1L, config.getLong("skyblock-leveling.skill-actions-per-level.combat", 40L));
        miningActionsPerLevel = Math.max(1L, config.getLong("skyblock-leveling.skill-actions-per-level.mining", 140L));
        foragingActionsPerLevel = Math.max(1L, config.getLong("skyblock-leveling.skill-actions-per-level.foraging", 140L));
        farmingActionsPerLevel = Math.max(1L, config.getLong("skyblock-leveling.skill-actions-per-level.farming", 120L));
        fishingActionsPerLevel = Math.max(1L, config.getLong("skyblock-leveling.skill-actions-per-level.fishing", 20L));
        pseudoSkillCap = Math.max(1, config.getInt("skyblock-leveling.skill-leveling.max-level",
                config.getInt("skyblock-leveling.skill-level-cap", 60)));
        pseudoCatacombsCap = Math.max(1, config.getInt("skyblock-leveling.dungeons.catacombs-level-cap", 50));
        pseudoClassCap = Math.max(1, config.getInt("skyblock-leveling.dungeons.class-level-cap", 50));

        bestiaryTierSize = Math.max(1, config.getInt("skyblock-leveling.bestiary.tier-size", 10));
        bestiaryTierXp = Math.max(0L, config.getLong("skyblock-leveling.bestiary.family-tier-xp", 1L));
        bestiaryMilestoneEveryTiers = Math.max(1, config.getInt("skyblock-leveling.bestiary.milestone-every-tiers",
                config.getInt("skyblock-leveling.bestiary.milestone-every-kills", 10)));
        bestiaryMilestoneXp = Math.max(0L, config.getLong("skyblock-leveling.bestiary.milestone-xp", 10L));

        healthPerLevel = Math.max(0, config.getInt("skyblock-leveling.level-rewards.health-per-level", 5));
        strengthPerFiveLevels = Math.max(0, config.getInt("skyblock-leveling.level-rewards.strength-per-5-levels", 1));
        farmingFortunePerLevel = Math.max(0, config.getInt("skyblock-leveling.level-rewards.farming-fortune-per-level", 2));

        loadTracks(config.getConfigurationSection("skyblock-leveling.tracks"));
        loadFeatureUnlocks(config.getConfigurationSection("skyblock-leveling.level-rewards.feature-unlocks"));
    }

    private void loadSkillXpRequirements(FileConfiguration config) {
        // Load Skyblock-accurate XP requirements for skill levels 1-60
        ConfigurationSection section = config.getConfigurationSection("skyblock-leveling.skill-leveling.xp-requirements");
        if (section != null) {
            for (int level = 1; level <= 60; level++) {
                skillXpRequirements[level] = Math.max(0L, section.getLong(String.valueOf(level), 0L));
            }
        } else {
            // Default Skyblock values if config section is missing
            long[] defaults = {
                0, 50, 125, 200, 300, 500, 750, 1000, 1500, 2000, 3500,
                5000, 7500, 10000, 15000, 20000, 30000, 50000, 75000, 100000, 200000,
                300000, 400000, 500000, 600000, 700000, 800000, 900000, 1000000, 1100000, 1200000,
                1300000, 1400000, 1500000, 1600000, 1700000, 1800000, 1900000, 2000000, 2100000, 2200000,
                2300000, 2400000, 2500000, 2600000, 2750000, 2900000, 3100000, 3400000, 3700000, 4000000,
                4300000, 4600000, 4900000, 5200000, 5500000, 5800000, 6100000, 6400000, 6700000, 7000000
            };
            System.arraycopy(defaults, 0, skillXpRequirements, 0, defaults.length);
        }
    }

    private void loadTracks(ConfigurationSection section) {
        tracks.clear();
        tracksById.clear();
        tracksByCounter.clear();

        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection trackSection = section.getConfigurationSection(key);
                if (trackSection == null) {
                    continue;
                }
                GuideTrack track = parseTrack(key, trackSection);
                if (track != null) {
                    registerTrack(track);
                }
            }
        }

        if (!tracks.isEmpty()) {
            return;
        }

        registerTrack(defaultCategoryTrack("core", "&6Core", Material.NETHER_STAR, 17089L, List.of(1000L, 5000L, 10000L, 15000L, 17089L), List.of("&7Profile, account, and", "&7global progression tasks.")));
        registerTrack(defaultCategoryTrack("event", "&dEvent", Material.FIREWORK_STAR, 3391L, List.of(250L, 1000L, 2000L, 3000L, 3391L), List.of("&7Seasonal and time-limited", "&7event progression.")));
        registerTrack(defaultCategoryTrack("dungeon", "&5Dungeon", Material.WITHER_SKELETON_SKULL, 3905L, List.of(250L, 1000L, 2000L, 3200L, 3905L), List.of("&7Floor clears, class levels,", "&7and catacombs progress.")));
        registerTrack(defaultCategoryTrack("essence", "&bEssence", Material.AMETHYST_SHARD, 1085L, List.of(100L, 300L, 600L, 900L, 1085L), List.of("&7Essence-related", "&7milestones and upgrades.")));
        registerTrack(defaultCategoryTrack("slaying", "&cSlaying", Material.IRON_SWORD, 7410L, List.of(500L, 2000L, 4000L, 6000L, 7410L), List.of("&7Bestiary and boss", "&7combat progression.")));
        registerTrack(defaultCategoryTrack("skill", "&aSkill Related", Material.ENCHANTED_BOOK, 10818L, List.of(500L, 2500L, 5000L, 9000L, 10818L), List.of("&7Skill level-up and", "&7resource gathering progression.")));
        registerTrack(defaultCategoryTrack("misc", "&eMisc", Material.COMPASS, 5782L, List.of(250L, 1000L, 2500L, 4500L, 5782L), List.of("&7Side systems and", "&7exploration-based progression.")));
        registerTrack(defaultCategoryTrack("story", "&9Story", Material.WRITABLE_BOOK, 1590L, List.of(100L, 400L, 900L, 1300L, 1590L), List.of("&7Questline and", "&7story completion progression.")));
    }

    private GuideTrack defaultCategoryTrack(String id, String displayName, Material icon, long maxXp, List<Long> milestones, List<String> lore) {
        List<Long> fixedMilestones = new ArrayList<>(milestones);
        if (fixedMilestones.isEmpty() || fixedMilestones.get(fixedMilestones.size() - 1) != maxXp) {
            fixedMilestones = List.of(maxXp);
        }
        List<Long> rewards = new ArrayList<>();
        for (int i = 0; i < fixedMilestones.size(); i++) {
            rewards.add(0L);
        }
        return new GuideTrack(id, displayName, icon, categoryCounterKey(id), fixedMilestones, rewards, lore);
    }

    private GuideTrack parseTrack(String id, ConfigurationSection section) {
        String normalizedId = normalizeKey(id);
        if (normalizedId.isBlank()) {
            return null;
        }

        String displayName = section.getString("display-name", id);
        String counterKey = normalizeKey(section.getString("counter-key", normalizedId));
        if (counterKey.isBlank()) {
            return null;
        }

        Material icon = parseMaterial(section.getString("icon", "BOOK"), Material.BOOK);
        List<Long> milestones = parseLongList(section.getList("milestones"));
        if (milestones.isEmpty()) {
            return null;
        }

        List<Long> rewards = parseLongList(section.getList("rewards"));
        List<Long> fixedMilestones = new ArrayList<>();
        List<Long> fixedRewards = new ArrayList<>();
        int count = Math.min(milestones.size(), rewards.isEmpty() ? milestones.size() : rewards.size());
        for (int i = 0; i < count; i++) {
            long threshold = Math.max(1L, milestones.get(i));
            if (!fixedMilestones.isEmpty() && threshold <= fixedMilestones.get(fixedMilestones.size() - 1)) {
                threshold = fixedMilestones.get(fixedMilestones.size() - 1) + 1L;
            }
            fixedMilestones.add(threshold);
            fixedRewards.add(rewards.isEmpty() ? 0L : Math.max(0L, rewards.get(i)));
        }

        return new GuideTrack(normalizedId, displayName, icon, counterKey, fixedMilestones, fixedRewards, section.getStringList("lore"));
    }

    private void registerTrack(GuideTrack track) {
        tracks.add(track);
        tracksById.put(track.id(), track);
        tracksByCounter.computeIfAbsent(track.counterKey(), ignored -> new ArrayList<>()).add(track);
    }

    private void loadFeatureUnlocks(ConfigurationSection section) {
        featureUnlocks.clear();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                int level;
                try {
                    level = Integer.parseInt(key.trim());
                } catch (NumberFormatException ignored) {
                    continue;
                }
                for (String unlock : parseStringList(section.get(key))) {
                    featureUnlocks.computeIfAbsent(level, ignored -> new ArrayList<>()).add(unlock);
                }
            }
        }
        if (!featureUnlocks.isEmpty()) {
            return;
        }

        addDefaultUnlock(3, "Community Shop");
        addDefaultUnlock(5, "Auction House");
        addDefaultUnlock(7, "Bazaar");
        addDefaultUnlock(9, "Museum");
        addDefaultUnlock(10, "Depth Strider Enchantment");
        addDefaultUnlock(15, "Garden");
        addDefaultUnlock(20, "Hex");
        addDefaultUnlock(25, "Skill Average 15");
        addDefaultUnlock(50, "Skill Average 20");
        addDefaultUnlock(70, "Garden Level 5");
        addDefaultUnlock(100, "Skill Average 25");
        addDefaultUnlock(120, "Garden Level 10");
        addDefaultUnlock(150, "Skill Average 30");
        addDefaultUnlock(200, "Skill Average 35");
        addDefaultUnlock(240, "Garden Level 15");
        addDefaultUnlock(250, "Skill Average 40");
        addDefaultUnlock(280, "Garden Level 20");
        addDefaultUnlock(300, "Skill Average 45");
        addDefaultUnlock(350, "Skill Average 50");
        addDefaultUnlock(400, "Skill Average 55");
        addDefaultUnlock(450, "Skill Average 60");
        addDefaultUnlock(500, "Skill Average 65");
    }

    private void addDefaultUnlock(int level, String unlock) {
        featureUnlocks.computeIfAbsent(level, ignored -> new ArrayList<>()).add(unlock);
    }

    private void evaluateBestiaryTiers(Player player, long mobKills) {
        long previousTier = Math.max(0L, mobKills - 1L) / bestiaryTierSize;
        long newTier = mobKills / bestiaryTierSize;
        for (long tier = previousTier + 1; tier <= newTier; tier++) {
            if (bestiaryTierXp > 0L) {
                awardCategoryXp(player, "slaying", bestiaryTierXp, "Bestiary Tier", false);
            }
            long totalTiers = incrementCounterAndGet(player, "bestiary_tiers", 1L);
            if (bestiaryMilestoneXp > 0L && totalTiers % bestiaryMilestoneEveryTiers == 0L) {
                awardCategoryXp(player, "slaying", bestiaryMilestoneXp, "Bestiary Milestone", true);
            }
        }
    }

    private void evaluatePseudoSkillLevels(Player player, String skillId, long actions, long actionsPerLevel, int cap) {
        if (!skillLevelingEnabled) {
            return;
        }
        UUID playerId = resolveProfileId(player);
        if (playerId == null) {
            return;
        }
        int targetLevel = (int) Math.min(cap, actions / actionsPerLevel);
        String counterKey = "pseudo_level." + normalizeKey(skillId);
        int currentLevel = (int) getCounter(playerId, counterKey);

        while (currentLevel < targetLevel) {
            currentLevel++;
            setCounter(playerId, counterKey, currentLevel);
            
            String skillName = capitalize(skillId);
            
            // Level Up Message
            player.sendMessage(ChatColor.DARK_GRAY + "--------------------------------------------------");
            player.sendMessage(ChatColor.GOLD + "  SKILL LEVEL UP " + ChatColor.AQUA + skillName + " " + currentLevel);
            player.sendMessage("");
            player.sendMessage(ChatColor.GREEN + "  REWARDS");
            
            // Skill Stat Bonus display logic
            io.papermc.Grivience.skills.SkyblockSkill skill = io.papermc.Grivience.skills.SkyblockSkill.parse(skillId);
            if (skill != null) {
                player.sendMessage(ChatColor.GRAY + "  \u25cf " + ChatColor.AQUA + "+" + skill.getStatName());
                if (skill.getPerkName() != null && !skill.getPerkName().equalsIgnoreCase("None")) {
                    player.sendMessage(ChatColor.GRAY + "  \u25cf " + ChatColor.YELLOW + skill.getPerkName() + " Perk increased!");
                }
            }

            long rewardXp = skillLevelRewardXp(currentLevel);
            if (rewardXp > 0L) {
                awardCategoryXp(player, "skill", rewardXp, skillName + " Level " + currentLevel, false);
                player.sendMessage(ChatColor.GRAY + "  \u25cf " + ChatColor.DARK_AQUA + "+" + rewardXp + " Skyblock XP");
            }
            player.sendMessage(ChatColor.DARK_GRAY + "--------------------------------------------------");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        }
    }

    private void evaluatePseudoDungeonLevels(Player player, long clears) {
        evaluatePseudoDungeonTrack(player, clears, "pseudo_level.catacombs", catacombsRunsPerLevel, pseudoCatacombsCap, true);
        evaluatePseudoDungeonTrack(player, clears, "pseudo_level.class", classRunsPerLevel, pseudoClassCap, false);
    }

    private void evaluatePseudoDungeonTrack(Player player, long clears, String counterKey, long runsPerLevel, int cap, boolean catacombs) {
        UUID playerId = resolveProfileId(player);
        if (playerId == null) {
            return;
        }
        long safeRunsPerLevel = Math.max(1L, runsPerLevel);
        int targetLevel = (int) Math.min(cap, clears / safeRunsPerLevel);
        int currentLevel = (int) getCounter(playerId, counterKey);
        while (currentLevel < targetLevel) {
            currentLevel++;
            setCounter(playerId, counterKey, currentLevel);
            long xp;
            if (catacombs) {
                // Skyblock-accurate: Level 1-39: +20 XP, Level 40-50: +40 XP
                xp = catacombsLevelXpReward(currentLevel);
            } else {
                // Class levels: +4 XP per level (configurable)
                xp = classLevelXpReward;
            }
            awardCategoryXp(player, "dungeon", xp, (catacombs ? "Catacombs" : "Class") + " Level " + currentLevel, false);
        }
    }

    private long catacombsLevelXpReward(int level) {
        // Skyblock-accurate Catacombs XP rewards
        // Level 1-39: +20 XP per level, Level 40-50: +40 XP per level
        if (level >= 1 && level <= 39) {
            return catacombsLevelXpReward1to39;
        }
        if (level >= 40 && level <= 50) {
            return catacombsLevelXpReward40to50;
        }
        return 0L;
    }

    private long skillLevelRewardXp(int level) {
        // Skyblock-accurate Skyblock XP rewards for skill level ups
        // Level 1-10: +5 XP, Level 11-25: +10 XP, Level 26-50: +20 XP, Level 51-60: +30 XP
        if (level >= 1 && level <= 10) {
            return skillLevelXpReward1to10;
        }
        if (level >= 11 && level <= 25) {
            return skillLevelXpReward11to25;
        }
        if (level >= 26 && level <= 50) {
            return skillLevelXpReward26to50;
        }
        if (level >= 51 && level <= 60) {
            return skillLevelXpReward51to60;
        }
        return 0L;
    }

    /**
     * Get the XP required to reach a specific skill level (Skyblock-accurate).
     * @param level The target skill level (1-60)
     * @return The XP required to reach that level from the previous level
     */
    public long getXpRequiredForLevel(int level) {
        if (level < 1 || level > 60) {
            return 0L;
        }
        return skillXpRequirements[level];
    }

    /**
     * Get the cumulative XP required to reach a specific skill level from level 0 (Skyblock-accurate).
     * @param level The target skill level (1-60)
     * @return The total cumulative XP required
     */
    public long getCumulativeXpForLevel(int level) {
        if (level < 1 || level > 60) {
            return 0L;
        }
        long total = 0L;
        for (int i = 1; i <= level; i++) {
            total += skillXpRequirements[i];
        }
        return total;
    }

    private long firstClearReward(String floorId) {
        String key = normalizeDungeonFloorKey(floorId);
        if (key.isBlank()) {
            return 0L;
        }
        Long configured = dungeonFirstClearXpByFloorKey.get(key);
        if (configured != null) {
            return Math.max(0L, configured);
        }

        // Backward-compatible fallback (older configs didn't define floor-completion-xp).
        if (key.equals("entrance")) {
            return 20L;
        }
        if (key.startsWith("master-") && isInteger(key.substring("master-".length()))) {
            return 50L;
        }
        if (key.startsWith("floor-") && isInteger(key.substring("floor-".length()))) {
            int floor = Integer.parseInt(key.substring("floor-".length()));
            if (floor <= 4) {
                return 20L;
            }
            if (floor <= 7) {
                return 30L;
            }
            return 40L;
        }
        return 20L;
    }

    private void loadDungeonFirstClearRewards(ConfigurationSection section) {
        dungeonFirstClearXpByFloorKey.clear();
        if (section == null) {
            return;
        }
        for (String rawKey : section.getKeys(false)) {
            if (rawKey == null || rawKey.isBlank()) {
                continue;
            }
            long xp = Math.max(0L, section.getLong(rawKey, 0L));
            String normalized = normalizeDungeonFloorKey(rawKey);
            if (!normalized.isBlank()) {
                dungeonFirstClearXpByFloorKey.put(normalized, xp);
            }
        }
    }

    private String normalizeDungeonFloorKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String id = raw.trim().toLowerCase(Locale.ROOT);
        id = id.replace(' ', '_').replace('_', '-');

        if (id.equals("entrance") || id.equals("e") || id.equals("f0") || id.equals("floor-0") || id.equals("floor0")) {
            return "entrance";
        }

        if (id.startsWith("master-") && isInteger(id.substring("master-".length()))) {
            return "master-" + Integer.parseInt(id.substring("master-".length()));
        }
        if (id.startsWith("m") && id.length() > 1 && isInteger(id.substring(1))) {
            return "master-" + Integer.parseInt(id.substring(1));
        }

        if (id.startsWith("floor-") && isInteger(id.substring("floor-".length()))) {
            return "floor-" + Integer.parseInt(id.substring("floor-".length()));
        }
        if (id.startsWith("floor") && id.length() > "floor".length() && isInteger(id.substring("floor".length()))) {
            return "floor-" + Integer.parseInt(id.substring("floor".length()));
        }
        if (id.startsWith("f") && id.length() > 1 && isInteger(id.substring(1))) {
            return "floor-" + Integer.parseInt(id.substring(1));
        }

        return id;
    }

    private boolean isBossEntity(EntityType type) {
        return switch (type) {
            case ENDER_DRAGON, WITHER, WARDEN, RAVAGER, ELDER_GUARDIAN -> true;
            default -> false;
        };
    }

    public long awardObjectiveXp(Player player, String objectiveId, String categoryId, long xp, String reason, boolean showGainToast) {
        if (player == null || xp <= 0L) {
            return 0L;
        }
        UUID playerId = resolveProfileId(player);
        if (playerId == null) {
            return 0L;
        }
        if (!claimObjective(playerId, objectiveId)) {
            return 0L;
        }
        return awardCategoryXp(player, categoryId, xp, reason, showGainToast);
    }

    public long awardCategoryXp(Player player, String categoryId, long amount, String reason, boolean showGainToast) {
        if (player == null || amount <= 0L) {
            return 0L;
        }

        long finalAmount = amount;
        if (plugin instanceof GriviencePlugin grivience && grivience.getGlobalEventManager() != null) {
            double multiplier = grivience.getGlobalEventManager().getGlobalXpMultiplier();
            if (multiplier > 1.0) {
                finalAmount = (long) (amount * multiplier);
            }
        }

        UUID playerId = resolveProfileId(player);
        if (playerId == null) {
            return 0L;
        }
        long globalRoom = Math.max(0L, xpCap() - getXp(playerId));
        if (globalRoom <= 0L) {
            return 0L;
        }

        String category = normalizeKey(categoryId);
        if (category.isBlank()) {
            category = "misc";
        }

        String counterKey = categoryCounterKey(category);
        long categoryRoom = Math.max(0L, categoryCap(counterKey) - getCounter(playerId, counterKey));
        long applied = Math.min(finalAmount, Math.min(globalRoom, categoryRoom));
        if (applied <= 0L) {
            return 0L;
        }

        long categoryXp = getCounter(playerId, counterKey) + applied;
        setCounter(playerId, counterKey, categoryXp);
        evaluateMilestones(player, counterKey, categoryXp);
        addXp(player, applied, reason, showGainToast);
        return applied;
    }

    private long categoryCap(String counterKey) {
        List<GuideTrack> matching = tracksByCounter.get(normalizeKey(counterKey));
        if (matching == null || matching.isEmpty()) {
            return Long.MAX_VALUE;
        }
        long cap = 0L;
        for (GuideTrack track : matching) {
            if (!track.milestones().isEmpty()) {
                cap = Math.max(cap, track.milestones().get(track.milestones().size() - 1));
            }
        }
        return cap <= 0L ? Long.MAX_VALUE : cap;
    }

    private boolean claimObjective(UUID playerId, String objectiveId) {
        String normalized = normalizeKey(objectiveId);
        if (normalized.isBlank()) {
            return false;
        }
        Set<String> claimed = claimedObjectivesByPlayer.computeIfAbsent(playerId, ignored -> new LinkedHashSet<>());
        return claimed.add(normalized);
    }

    private long xpCap() {
        if (maxLevel <= 0) {
            return Long.MAX_VALUE;
        }
        return (long) maxLevel * xpPerLevel;
    }

    private String categoryCounterKey(String categoryId) {
        return "category_xp." + normalizeKey(categoryId);
    }

    private long clampXp(long rawXp) {
        long safe = Math.max(0L, rawXp);
        long cap = xpCap();
        return cap == Long.MAX_VALUE ? safe : Math.min(safe, cap);
    }

    private UUID resolveProfileId(Player player) {
        if (player == null) {
            return null;
        }

        UUID ownerId = player.getUniqueId();
        if (ownerId == null) {
            return null;
        }

        if (plugin instanceof GriviencePlugin grivience) {
            // Co-op members should share SkyBlock Level progress with the island profile they belong to.
            // (Owner resolves via selected profile below.)
            var islandManager = grivience.getIslandManager();
            if (islandManager != null) {
                UUID coopProfileId = islandManager.getCoopProfileId(ownerId);
                if (coopProfileId != null) {
                    return coopProfileId;
                }
            }

            ProfileManager profileManager = grivience.getProfileManager();
            if (profileManager != null) {
                SkyBlockProfile profile = profileManager.getSelectedProfile(player);
                if (profile != null && profile.getProfileId() != null) {
                    UUID profileId = profile.getProfileId();
                    migrateLegacyProgress(ownerId, profileId);
                    return profileId;
                }
            }
        }

        return ownerId;
    }

    private void migrateLegacyProgress(UUID ownerId, UUID profileId) {
        if (ownerId == null || profileId == null || ownerId.equals(profileId)) {
            return;
        }

        boolean legacyPresent = xpByPlayer.containsKey(ownerId)
                || countersByPlayer.containsKey(ownerId)
                || milestoneByPlayer.containsKey(ownerId)
                || objectivesByPlayer.containsKey(ownerId);
        if (!legacyPresent) {
            return;
        }

        boolean changed = false;

        Long legacyXp = xpByPlayer.remove(ownerId);
        if (legacyXp != null && legacyXp > 0L) {
            long currentXp = xpByPlayer.getOrDefault(profileId, 0L);
            if (legacyXp > currentXp) {
                xpByPlayer.put(profileId, legacyXp);
                changed = true;
            }
            changed = true;
        }

        Map<String, Long> legacyCounters = countersByPlayer.remove(ownerId);
        if (legacyCounters != null && !legacyCounters.isEmpty()) {
            Map<String, Long> target = countersByPlayer.computeIfAbsent(profileId, ignored -> new HashMap<>());
            for (Map.Entry<String, Long> entry : legacyCounters.entrySet()) {
                long value = Math.max(0L, entry.getValue());
                if (value <= 0L) {
                    continue;
                }
                String key = normalizeKey(entry.getKey());
                long existing = target.getOrDefault(key, 0L);
                if (value > existing) {
                    target.put(key, value);
                    changed = true;
                }
            }
        }

        Map<String, Integer> legacyMilestones = milestoneByPlayer.remove(ownerId);
        if (legacyMilestones != null && !legacyMilestones.isEmpty()) {
            Map<String, Integer> target = milestoneByPlayer.computeIfAbsent(profileId, ignored -> new HashMap<>());
            for (Map.Entry<String, Integer> entry : legacyMilestones.entrySet()) {
                int value = Math.max(0, entry.getValue());
                if (value <= 0) {
                    continue;
                }
                String key = normalizeKey(entry.getKey());
                int existing = target.getOrDefault(key, 0);
                if (value > existing) {
                    target.put(key, value);
                    changed = true;
                }
            }
        }

        Set<String> legacyObjectives = objectivesByPlayer.remove(ownerId);
        if (legacyObjectives != null && !legacyObjectives.isEmpty()) {
            Set<String> target = objectivesByPlayer.computeIfAbsent(profileId, ignored -> new LinkedHashSet<>());
            for (String objective : legacyObjectives) {
                String key = normalizeKey(objective);
                if (!key.isBlank() && target.add(key)) {
                    changed = true;
                }
            }
        }

        if (changed) {
            save();
        }
    }

    private UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String normalizeKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private String normalize(String raw) {
        return normalizeKey(raw);
    }

    private String capitalize(String raw) {
        String normalized = normalizeKey(raw).replace('_', ' ');
        if (normalized.isBlank()) {
            return "";
        }
        return normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
    }

    private String fmt(long value) {
        return String.format(Locale.US, "%,d", Math.max(0L, value));
    }

    private boolean isInteger(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        try {
            Integer.parseInt(raw.trim());
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private List<Long> parseLongList(List<?> input) {
        List<Long> output = new ArrayList<>();
        if (input == null) {
            return output;
        }
        for (Object value : input) {
            if (value instanceof Number number) {
                output.add(number.longValue());
            } else if (value instanceof String text) {
                try {
                    output.add(Long.parseLong(text));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return output;
    }

    private List<String> parseStringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof String text) {
            return text.isBlank() ? List.of() : List.of(text.trim());
        }
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object entry : list) {
                if (entry instanceof String text && !text.isBlank()) {
                    result.add(text.trim());
                }
            }
            return result;
        }
        return List.of();
    }

    private Material parseMaterial(String raw, Material fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    public String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }

    public record FeatureUnlock(int level, List<String> unlocks) {
    }

    public record GuideTrack(
            String id,
            String displayName,
            Material icon,
            String counterKey,
            List<Long> milestones,
            List<Long> rewards,
            List<String> lore
    ) {
    }

    public record GuideProgress(
            GuideTrack track,
            long counterValue,
            int claimedMilestones,
            int totalMilestones,
            long previousThreshold,
            long nextThreshold,
            long nextRewardXp,
            double progressToNext,
            boolean completed
    ) {
    }
}

