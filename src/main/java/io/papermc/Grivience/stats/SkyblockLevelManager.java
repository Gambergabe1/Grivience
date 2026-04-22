package io.papermc.Grivience.stats;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skyblock.profile.ProfileManager;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import io.papermc.Grivience.skills.SkillXPTable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class SkyblockLevelManager {
    private static final String FILE_NAME = "levels.yml";

    private final GriviencePlugin plugin;
    private io.papermc.Grivience.item.CustomArmorManager armorManager;
    private io.papermc.Grivience.skills.SkyblockSkillManager skillManager;
    private final Map<UUID, Long> xpByPlayer = new HashMap<>();
    private final Map<UUID, Map<String, Long>> countersByPlayer = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> milestoneByPlayer = new HashMap<>();
    private final Map<UUID, Set<String>> objectivesByPlayer = new HashMap<>();

    private final List<GuideTrack> tracks = new ArrayList<>();
    private final Map<String, GuideTrack> tracksById = new LinkedHashMap<>();
    private final Map<String, List<GuideTrack>> tracksByCounter = new HashMap<>();
    private final NavigableMap<Integer, List<String>> featureUnlocks = new TreeMap<>();

    private File file;
    private FileConfiguration fileConfig;
    private BukkitTask autoSaveTask;
    private boolean dirty;
    private int autoSaveIntervalSeconds;

    private long xpPerLevel;
    private int maxLevel;
    private boolean notifyXpGain;

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
    private long combatSkillBaseXp;
    private int combatSkillLevelsPerStep;
    private long combatSkillXpPerStep;
    private long combatSkillEmpoweredBonusXp;
    private long bossCombatSkillBaseXp;
    private int bossCombatSkillLevelsPerStep;
    private long bossCombatSkillXpPerStep;
    private int bossCombatSkillMinimumLevel;
    private final Set<String> customBossMonsterIds = new LinkedHashSet<>();
    private long miningOreXp;
    private long foragingLogXp;
    private long farmingHarvestXp;
    private long fishingCatchXp;
    private long questCompleteXp;
    private long dungeonCompleteXp;
    private long islandCreateXp;
    private long islandUpgradeXp;

    private int bestiaryTierSize;
    private long bestiaryTierXp;
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

    public SkyblockLevelManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), FILE_NAME);
        load();
        startAutoSave();
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

    public io.papermc.Grivience.skills.SkyblockSkillManager getSkillManager() {
        return skillManager;
    }

    public GriviencePlugin getPlugin() {
        return plugin;
    }

    public void load() {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException ignored) {}
        }
        fileConfig = YamlConfiguration.loadConfiguration(file);
        loadSettingsFromConfig();

        xpByPlayer.clear();
        countersByPlayer.clear();
        milestoneByPlayer.clear();
        objectivesByPlayer.clear();

        ConfigurationSection playersSection = fileConfig.getConfigurationSection("players");
        if (playersSection != null) {
            for (String uuidStr : playersSection.getKeys(false)) {
                UUID uuid = parseUuid(uuidStr);
                if (uuid == null) continue;

                ConfigurationSection section = playersSection.getConfigurationSection(uuidStr);
                if (section == null) continue;

                xpByPlayer.put(uuid, section.getLong("xp", 0L));

                ConfigurationSection countersSec = section.getConfigurationSection("counters");
                if (countersSec != null) {
                    Map<String, Long> counters = new HashMap<>();
                    for (String key : countersSec.getKeys(false)) {
                        counters.put(normalize(key), countersSec.getLong(key));
                    }
                    countersByPlayer.put(uuid, counters);
                }

                ConfigurationSection milestonesSec = section.getConfigurationSection("milestones");
                if (milestonesSec != null) {
                    Map<String, Integer> milestones = new HashMap<>();
                    for (String key : milestonesSec.getKeys(false)) {
                        milestones.put(normalize(key), milestonesSec.getInt(key));
                    }
                    milestoneByPlayer.put(uuid, milestones);
                }

                List<String> objectives = section.getStringList("objectives");
                if (!objectives.isEmpty()) {
                    Set<String> set = new LinkedHashSet<>();
                    for (String obj : objectives) {
                        set.add(normalize(obj));
                    }
                    objectivesByPlayer.put(uuid, set);
                }
            }
        }
    }

    public void save() {
        if (fileConfig == null) return;
        
        fileConfig.set("players", null);
        Set<UUID> allIds = new HashSet<>(xpByPlayer.keySet());
        allIds.addAll(countersByPlayer.keySet());
        allIds.addAll(milestoneByPlayer.keySet());
        allIds.addAll(objectivesByPlayer.keySet());

        for (UUID uuid : allIds) {
            String path = "players." + uuid.toString() + ".";
            fileConfig.set(path + "xp", xpByPlayer.getOrDefault(uuid, 0L));

            Map<String, Long> counters = countersByPlayer.get(uuid);
            if (counters != null && !counters.isEmpty()) {
                for (Map.Entry<String, Long> entry : counters.entrySet()) {
                    fileConfig.set(path + "counters." + entry.getKey(), entry.getValue());
                }
            }

            Map<String, Integer> milestones = milestoneByPlayer.get(uuid);
            if (milestones != null && !milestones.isEmpty()) {
                for (Map.Entry<String, Integer> entry : milestones.entrySet()) {
                    fileConfig.set(path + "milestones." + entry.getKey(), entry.getValue());
                }
            }

            Set<String> objectives = objectivesByPlayer.get(uuid);
            if (objectives != null && !objectives.isEmpty()) {
                fileConfig.set(path + "objectives", new ArrayList<>(objectives));
            }
        }

        try {
            fileConfig.save(file);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save levels.yml: " + e.getMessage());
        }
    }

    public void saveIfDirty() {
        if (dirty) save();
    }

    public void shutdown() {
        stopAutoSave();
        save();
    }

    public int getLevel(UUID playerId) {
        return (int) (getXp(playerId) / (double) Math.max(1L, xpPerLevel));
    }

    public int getLevel(Player player) {
        UUID profileId = resolveProfileId(player);
        return profileId == null ? 0 : getLevel(profileId);
    }

    public long getXp(UUID playerId) {
        return xpByPlayer.getOrDefault(playerId, 0L);
    }

    public long getXp(Player player) {
        UUID profileId = resolveProfileId(player);
        return profileId == null ? 0L : getXp(profileId);
    }

    public long getXpPerLevel() { return xpPerLevel; }
    public int getMaxLevel() { return maxLevel; }

    public boolean isAtMaxLevel(UUID playerId) {
        return maxLevel > 0 && getLevel(playerId) >= maxLevel;
    }

    public boolean isAtMaxLevel(Player player) {
        UUID profileId = resolveProfileId(player);
        return profileId != null && isAtMaxLevel(profileId);
    }

    public double getProgress(UUID playerId) {
        return isAtMaxLevel(playerId) ? 1.0D : (getXp(playerId) % Math.max(1L, xpPerLevel)) / (double) Math.max(1L, xpPerLevel);
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
        if (isAtMaxLevel(playerId)) return 0L;
        long per = Math.max(1L, xpPerLevel);
        return per - xpIntoCurrentLevel(playerId);
    }

    public long xpToNextLevel(Player player) {
        UUID profileId = resolveProfileId(player);
        return profileId == null ? 0L : xpToNextLevel(profileId);
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
        if (level < 360) return ChatColor.YELLOW;
        if (level < 400) return ChatColor.RED;
        if (level < 440) return ChatColor.DARK_RED;
        if (level < 480) return ChatColor.DARK_BLUE;
        if (level < 520) return ChatColor.DARK_GREEN;
        return ChatColor.YELLOW;
    }

    public int getHealthBonus(Player player) {
        return getLevel(player) * healthPerLevel;
    }

    public int getStrengthBonus(Player player) {
        return (getLevel(player) / 5) * strengthPerFiveLevels;
    }

    public int getFarmingFortuneBonus(Player player) {
        return getLevel(player) * farmingFortunePerLevel;
    }

    public FeatureUnlock nextFeatureUnlock(Player player) {
        int level = getLevel(player);
        Map.Entry<Integer, List<String>> entry = featureUnlocks.higherEntry(level);
        return entry == null ? null : new FeatureUnlock(entry.getKey(), entry.getValue());
    }

    public List<String> featureUnlocksAtLevel(int level) {
        return featureUnlocks.getOrDefault(level, Collections.emptyList());
    }

    public void updatePetScore(Player player, int score) {
        if (player == null) return;
        UUID pid = resolveProfileId(player);
        if (pid == null) return;
        
        long old = getCounter(pid, "pet_score");
        if (score == old) return;
        setCounter(pid, "pet_score", (long) score);
        
        long xp = score * xpPerPetScore;
        updateUnlimitedXpSource(player, "misc.pet_score", "misc", xp, "Pet Score");
    }

    public void updateMagicalPower(Player player, int power) {
        if (player == null) return;
        UUID pid = resolveProfileId(player);
        if (pid == null) return;
        
        long old = getCounter(pid, "magical_power");
        if (power == old) return;
        setCounter(pid, "magical_power", (long) power);
        
        long xp = power * xpPerMagicalPower;
        updateUnlimitedXpSource(player, "core.magical_power", "core", xp, "Magical Power");
    }

    private void updateUnlimitedXpSource(Player player, String objId, String catId, long totalXp, String reason) {
        UUID pid = resolveProfileId(player);
        if (pid == null) return;
        
        String internalKey = "unlimited_xp." + normalize(objId);
        long already = getCounter(pid, internalKey);
        long delta = totalXp - already;
        if (delta == 0) return;
        
        setCounter(pid, internalKey, totalXp);
        awardCategoryXp(player, catId, delta, reason, false);
    }

    public void setXp(UUID pid, long amount) {
        xpByPlayer.put(pid, clampXp(amount));
        markDirty();
    }

    public void setXp(Player player, long amount) {
        UUID pid = resolveProfileId(player);
        if (pid != null) setXp(pid, amount);
    }

    public void addXp(Player player, long amount, String reason, boolean silent) {
        if (player == null || amount <= 0L) return;
        UUID profileId = resolveProfileId(player);
        if (profileId == null) return;

        int oldLevel = getLevel(profileId);
        long newXp = getXp(profileId) + amount;
        xpByPlayer.put(profileId, newXp);
        int newLevel = getLevel(profileId);

        if (!silent && notifyXpGain) {
            player.sendMessage(ChatColor.DARK_AQUA + "+" + amount + " Skyblock XP (" + reason + ")");
        }

        if (newLevel > oldLevel) {
            handleGlobalLevelUp(player, oldLevel, newLevel);
        }
        markDirty();
    }

    public void addXp(Player player, long amount) {
        addXp(player, amount, "Bonus", true);
    }

    public void addXp(UUID profileId, long amount) {
        setXp(profileId, getXp(profileId) + amount);
    }

    private void handleGlobalLevelUp(Player player, int oldLevel, int newLevel) {
        // Update accessory slots
        if (plugin.getAccessoryBagSlotManager() != null) {
            plugin.getAccessoryBagSlotManager().updatePlayerSlots(player);
        }

        for (int lv = oldLevel + 1; lv <= newLevel; lv++) {
            player.sendMessage(ChatColor.DARK_GRAY + "--------------------------------------------------");
            player.sendMessage(ChatColor.GOLD + "  SKYBLOCK LEVEL UP " + ChatColor.AQUA + lv);
            player.sendMessage("");
            player.sendMessage(ChatColor.GREEN + "  REWARDS");
            player.sendMessage(ChatColor.GRAY + "  \u25cf " + ChatColor.RED + "+" + healthPerLevel + " Health");
            if (lv % 5 == 0) {
                player.sendMessage(ChatColor.GRAY + "  \u25cf " + ChatColor.RED + "+" + strengthPerFiveLevels + " Strength");
            }
            
            List<String> unlocks = featureUnlocks.get(lv);
            if (unlocks != null) {
                for (String unlock : unlocks) {
                    player.sendMessage(ChatColor.GRAY + "  \u25cf " + ChatColor.YELLOW + "Unlocked: " + ChatColor.WHITE + unlock);
                }
            }
            player.sendMessage(ChatColor.DARK_GRAY + "--------------------------------------------------");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
        
        if (plugin instanceof GriviencePlugin g && g.getSkyblockStatsManager() != null) {
            g.getSkyblockStatsManager().reload(player);
        }
    }

    public void recordCombatKill(Player player, LivingEntity entity) {
        if (player == null || entity == null) return;
        incrementCounter(player, "combat_kills", 1L);

        long combatSkillXpAward = resolveCombatSkillXp(player, entity);
        if (skillManager != null) {
            skillManager.addXp(player, io.papermc.Grivience.skills.SkyblockSkill.COMBAT, combatSkillXpAward);
            skillManager.addXp(player, io.papermc.Grivience.skills.SkyblockSkill.HUNTING, combatSkillXpAward);
        }

        if (combatKillXp > 0L) addXp(player, combatKillXp, "Combat", true);

        String mob = normalize(entity.getType().name());
        long mobKills = incrementCounter(player, "bestiary_kills." + mob, 1L);
        awardObjectiveXp(player, "slaying.discovery." + mob, "slaying", 2L, "Bestiary Discovery", false);
        evaluateBestiary(player, mobKills);
        if (isBoss(entity)) {
            awardObjectiveXp(player, "slaying.boss." + bossObjectiveMobId(entity, mob), "slaying", 20L, "Boss Defeated", true);
        }
    }

    public void recordCombatKill(Player player, EntityType type) {
        if (player == null || type == null) return;
        incrementCounter(player, "combat_kills", 1L);

        if (skillManager != null) {
            long fallbackXp = Math.max(1L, combatSkillBaseXp);
            skillManager.addXp(player, io.papermc.Grivience.skills.SkyblockSkill.COMBAT, fallbackXp);
            skillManager.addXp(player, io.papermc.Grivience.skills.SkyblockSkill.HUNTING, fallbackXp);
        }

        if (combatKillXp > 0L) addXp(player, combatKillXp, "Combat", true);

        String mob = normalize(type.name());
        long mobKills = incrementCounter(player, "bestiary_kills." + mob, 1L);
        awardObjectiveXp(player, "slaying.discovery." + mob, "slaying", 2L, "Bestiary Discovery", false);
        evaluateBestiary(player, mobKills);
        if (isBossEntity(type)) {
            awardObjectiveXp(player, "slaying.boss." + mob, "slaying", 20L, "Boss Defeated", true);
        }
    }

    public long resolveCombatSkillXp(Player player, LivingEntity entity) {
        if (entity == null) return Math.max(1L, combatSkillBaseXp);
        Integer explicitLevel = resolveTaggedMonsterLevel(entity);
        int effectiveLevel = explicitLevel == null ? 1 : explicitLevel;
        boolean isCustomBoss = isBoss(entity); // Checks PDCT for boss_mob
        boolean isBukkitBoss = isBossEntity(entity.getType()); // Checks EntityType
        boolean isBoss = isCustomBoss || isBukkitBoss; // Consider it a boss if either is true
        if (isBoss) { // Apply boss logic if it's a boss by either method
            effectiveLevel = Math.max(effectiveLevel, bossCombatSkillMinimumLevel);
        }
        long reward = scaledCombatSkillXp(effectiveLevel, combatSkillBaseXp, combatSkillLevelsPerStep, combatSkillXpPerStep);
        if (isEmpowered(entity)) reward += combatSkillEmpoweredBonusXp;
        if (isBoss) reward = Math.max(reward, scaledCombatSkillXp(effectiveLevel, bossCombatSkillBaseXp, bossCombatSkillLevelsPerStep, bossCombatSkillXpPerStep));

        if (player != null) {
            int pLevel = getLevel(player);
            if (effectiveLevel > pLevel) {
                reward = (long) (reward * (1.0 + Math.min(9.0, (effectiveLevel - pLevel) * 0.10)));
            } else if (pLevel > effectiveLevel + 20) {
                reward = (long) (reward * Math.max(0.5, 1.0 - ((pLevel - (effectiveLevel + 20)) * 0.02)));
            }
        }
        return Math.max(1L, reward);
    }

    public long resolveCombatSkillXp(LivingEntity entity) { return resolveCombatSkillXp(null, entity); }

    public void recordMiningOre(Player player, Material material) {
        if (player == null || material == null) return;
        incrementCounter(player, "mining_ores", 1L);
        if (skillManager != null) {
            long xp = switch (material) {
                case COAL_ORE, DEEPSLATE_COAL_ORE, IRON_ORE, DEEPSLATE_IRON_ORE, COPPER_ORE, DEEPSLATE_COPPER_ORE -> 5;
                case GOLD_ORE, DEEPSLATE_GOLD_ORE -> 6;
                case LAPIS_ORE, DEEPSLATE_LAPIS_ORE, LAPIS_BLOCK, REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE, REDSTONE_BLOCK -> 7;
                case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE, DIAMOND_BLOCK -> 15;
                case EMERALD_ORE, DEEPSLATE_EMERALD_ORE, EMERALD_BLOCK, OBSIDIAN, CRYING_OBSIDIAN, BLUE_STAINED_GLASS, BLUE_STAINED_GLASS_PANE, BLUE_CONCRETE_POWDER -> 20;
                case AMETHYST_CLUSTER, BUDDING_AMETHYST, AMETHYST_BLOCK -> 25;
                default -> 1;
            };
            skillManager.addXp(player, io.papermc.Grivience.skills.SkyblockSkill.MINING, xp);
        }
        if (miningOreXp > 0L) addXp(player, miningOreXp, "Mining", true);
    }

    public void recordForagingLog(Player player, Material material) {
        if (player == null || material == null) return;
        incrementCounter(player, "foraging_logs", 1L);
        if (skillManager != null) skillManager.addXp(player, io.papermc.Grivience.skills.SkyblockSkill.FORAGING, 1);
        if (foragingLogXp > 0L) addXp(player, foragingLogXp, "Foraging", true);
    }

    public void recordFarmingHarvest(Player player, Material material) {
        if (player == null || material == null) return;
        incrementCounter(player, "farming_harvests", 1L);
        if (skillManager != null) skillManager.addXp(player, io.papermc.Grivience.skills.SkyblockSkill.FARMING, 1);
        if (farmingHarvestXp > 0L) addXp(player, farmingHarvestXp, "Farming", true);
    }

    public void recordFishingCatch(Player player) {
        if (player == null) return;
        incrementCounter(player, "fishing_catches", 1L);
        if (skillManager != null) skillManager.addXp(player, io.papermc.Grivience.skills.SkyblockSkill.FISHING, 1);
        if (fishingCatchXp > 0L) addXp(player, fishingCatchXp, "Fishing", true);
    }

    public void recordEnchanting(Player player, long levelsSpent) {
        if (player == null || levelsSpent <= 0) return;
        incrementCounter(player, "enchanting_actions", levelsSpent);
        if (skillManager != null) skillManager.addXp(player, io.papermc.Grivience.skills.SkyblockSkill.ENCHANTING, levelsSpent);
    }

    public void recordAlchemy(Player player, int potionCount) {
        if (player == null || potionCount <= 0) return;
        incrementCounter(player, "alchemy_actions", (long) potionCount);
        if (skillManager != null) skillManager.addXp(player, io.papermc.Grivience.skills.SkyblockSkill.ALCHEMY, potionCount * 5);
    }

    public void recordCarpentry(Player player, int amount) {
        if (player == null || amount <= 0) return;
        incrementCounter(player, "carpentry_actions", (long) amount);
        if (skillManager != null) skillManager.addXp(player, io.papermc.Grivience.skills.SkyblockSkill.CARPENTRY, (long) amount);
    }

    public void recordCarpentryCraft(Player player, ItemStack result) {
        if (player == null || result == null) return;
        
        long xp = result.getAmount(); // Base XP is 1 per item
        
        String itemId = plugin.getCustomItemService() != null ? plugin.getCustomItemService().itemId(result) : null;
        if (itemId != null) {
            itemId = itemId.toLowerCase(Locale.ROOT);
            
            // Check for Enchanted Farming items
            io.papermc.Grivience.item.EnchantedFarmItemType farmType = io.papermc.Grivience.item.EnchantedFarmItemType.parse(itemId);
            if (farmType != null) {
                if (farmType.isTierTwo()) {
                    xp = (long) result.getAmount() * 200; // Tier 2 enchanted (e.g. Sugar Cane)
                } else {
                    xp = (long) result.getAmount() * 20; // Tier 1 enchanted (e.g. Sugar)
                }
            }
            
            // Check for Mining items
            io.papermc.Grivience.item.MiningItemType miningType = io.papermc.Grivience.item.MiningItemType.parse(itemId);
            if (miningType != null) {
                if (itemId.contains("enchanted")) {
                    xp = (long) result.getAmount() * 25;
                } else if (itemId.contains("drill") || itemId.contains("engine")) {
                    xp = (long) result.getAmount() * 500; // Large crafting projects
                }
            }
            
            // Check for Weapons/Armor
            if (plugin.getCustomItemService().isWeapon(result) || plugin.getCustomItemService().isArmor(result)) {
                xp = (long) result.getAmount() * 100;
            }
        } else {
            // Vanilla items: check if it's a "valuable" block
            Material type = result.getType();
            if (type.name().endsWith("_BLOCK") && !type.name().contains("DIRT") && !type.name().contains("COBBLESTONE")) {
                xp = (long) result.getAmount() * 5;
            }
        }

        incrementCounter(player, "carpentry_actions", (long) result.getAmount());
        if (skillManager != null) {
            skillManager.addXp(player, io.papermc.Grivience.skills.SkyblockSkill.CARPENTRY, xp);
        }
    }

    public void recordQuestCompletion(Player player, String questId) {
        if (player == null) return;
        incrementCounter(player, "quest_completions", 1L);
        if (questCompleteXp > 0L) addXp(player, questCompleteXp, "Quest", true);
    }

    public void recordDungeonCompletion(Player player, String floorId, String grade, int score) {
        if (player == null) return;
        incrementCounter(player, "dungeon_completions", 1L);
        if (skillManager != null) skillManager.addXp(player, io.papermc.Grivience.skills.SkyblockSkill.DUNGEONEERING, 1);
        if (dungeonCompleteXp > 0L) addXp(player, dungeonCompleteXp, "Dungeon", true);
    }

    public void recordIslandCreated(Player player) {
        if (player == null) return;
        incrementCounter(player, "islands_created", 1L);
        if (islandCreateXp > 0L) addXp(player, islandCreateXp, "Island", true);
    }

    public void recordIslandUpgrade(Player player, int newLevel) {
        if (player == null) return;
        incrementCounter(player, "island_upgrades", 1L);
        if (islandUpgradeXp > 0L) addXp(player, islandUpgradeXp, "Island Upgrade", true);
    }

    public long awardObjectiveXp(Player player, String objectiveId, String category, long amount, String reason, boolean silent) {
        if (player == null || objectiveId == null) return 0L;
        UUID profileId = resolveProfileId(player);
        if (profileId == null) return 0L;

        Set<String> claimed = objectivesByPlayer.computeIfAbsent(profileId, k -> new HashSet<>());
        String key = normalize(objectiveId);
        if (claimed.add(key)) {
            addXp(player, amount, reason, silent);
            markDirty();
            return amount;
        }
        return 0L;
    }

    public void awardCategoryXp(Player player, String categoryId, long amount, String reason, boolean silent) {
        addXp(player, amount, reason, silent);
    }

    public long getCounter(UUID playerId, String counterKey) {
        Map<String, Long> counters = countersByPlayer.get(playerId);
        return counters == null ? 0L : counters.getOrDefault(normalize(counterKey), 0L);
    }

    public long getCounter(Player player, String counterKey) {
        UUID profileId = resolveProfileId(player);
        return profileId == null ? 0L : getCounter(profileId, counterKey);
    }

    public Map<String, Long> getCounters(UUID playerId) {
        return countersByPlayer.getOrDefault(playerId, Collections.emptyMap());
    }

    public long incrementCounter(Player player, String counterKey, long amount) {
        UUID profileId = resolveProfileId(player);
        if (profileId == null) return 0L;
        Map<String, Long> map = countersByPlayer.computeIfAbsent(profileId, k -> new HashMap<>());
        String key = normalize(counterKey);
        long updated = map.getOrDefault(key, 0L) + amount;
        map.put(key, updated);
        evaluateMilestones(player, key, updated);
        markDirty();
        return updated;
    }

    public void addToCounter(Player player, String counterKey, long amount) {
        incrementCounter(player, counterKey, amount);
    }

    public void setCounter(Player player, String counterKey, long value) {
        UUID profileId = resolveProfileId(player);
        if (profileId != null) {
            setCounter(profileId, counterKey, value);
        }
    }

    public long addMiningActions(Player player, long amount) { return incrementCounter(player, "mining_ores", amount); }

    private long incrementCounterAndGet(Player player, String key, long amount) { return incrementCounter(player, key, amount); }

    public void setCounter(UUID playerId, String counterKey, long value) {
        countersByPlayer.computeIfAbsent(playerId, k -> new HashMap<>()).put(normalize(counterKey), value);
        markDirty();
    }

    public long getSkillXp(UUID profileId, String skillId) { return getCounter(profileId, "skill_xp_" + normalize(skillId)); }

    public int getMilestoneStage(UUID playerId, String trackId) {
        return milestoneByPlayer.getOrDefault(playerId, Collections.emptyMap()).getOrDefault(normalize(trackId), 0);
    }

    public void setMilestoneStage(UUID playerId, String trackId, int stage) {
        milestoneByPlayer.computeIfAbsent(playerId, k -> new HashMap<>()).put(normalize(trackId), stage);
        markDirty();
    }

    private void evaluateMilestones(Player player, String counterKey, long counterValue) {
        List<GuideTrack> matching = tracksByCounter.get(normalize(counterKey));
        if (matching == null) return;
        UUID profileId = resolveProfileId(player);
        if (profileId == null) return;

        for (GuideTrack track : matching) {
            int stage = getMilestoneStage(profileId, track.id());
            while (stage < track.milestones().size() && counterValue >= track.milestones().get(stage)) {
                long reward = track.rewards().get(stage);
                stage++;
                setMilestoneStage(profileId, track.id(), stage);
                if (reward > 0L) addXp(player, reward, color(track.displayName()) + " Milestone", false);
            }
        }
    }

    public int totalMilestones() {
        if (tracks == null) return 0;
        int total = 0;
        for (GuideTrack track : tracks) {
            if (track != null && track.milestones() != null) {
                total += track.milestones().size();
            }
        }
        return total;
    }

    public int claimedMilestones(Player player) {
        if (player == null || tracks == null) return 0;
        UUID pid = resolveProfileId(player);
        if (pid == null) return 0;
        int total = 0;
        for (GuideTrack track : tracks) {
            if (track != null) {
                total += getMilestoneStage(pid, track.id());
            }
        }
        return total;
    }

    private void evaluateBestiary(Player player, long totalKills) {
        int tier = (int) (totalKills / bestiaryTierSize);
        UUID pid = resolveProfileId(player);
        if (pid == null) return;
        int current = getMilestoneStage(pid, "bestiary");
        if (tier > current) {
            setMilestoneStage(pid, "bestiary", tier);
            awardCategoryXp(player, "slaying", bestiaryTierXp, "Bestiary Tier " + tier, false);
            if (tier % bestiaryMilestoneEveryTiers == 0) awardCategoryXp(player, "slaying", bestiaryMilestoneXp, "Bestiary Milestone", true);
        }
    }

    private void evaluateBestiaryTiers(Player player, long mobKills) {
        evaluateBestiary(player, mobKills);
    }

    private void loadSettingsFromConfig() {
        FileConfiguration config = plugin.getConfig();
        xpPerLevel = Math.max(1L, config.getLong("skyblock-leveling.xp-per-level", 100L));
        maxLevel = Math.max(0, config.getInt("skyblock-leveling.max-level", 521));
        notifyXpGain = config.getBoolean("skyblock-leveling.notify-xp-gain", true);
        autoSaveIntervalSeconds = Math.max(60, config.getInt("skyblock-leveling.auto-save-interval-seconds", 300));
        
        healthPerLevel = config.getInt("skyblock-leveling.level-rewards.health-per-level", 5);
        strengthPerFiveLevels = config.getInt("skyblock-leveling.level-rewards.strength-per-5-levels", 1);
        farmingFortunePerLevel = config.getInt("skyblock-leveling.level-rewards.farming-fortune-per-level", 2);

        combatKillXp = config.getLong("skyblock-leveling.action-xp.combat-kill", 0L);
        combatSkillBaseXp = config.getLong("skyblock-leveling.combat-skill-xp.base-per-kill", 1L);
        combatSkillLevelsPerStep = Math.max(1, config.getInt("skyblock-leveling.combat-skill-xp.extra-every-levels", 3));
        combatSkillXpPerStep = config.getLong("skyblock-leveling.combat-skill-xp.extra-per-step", 1L);
        combatSkillEmpoweredBonusXp = config.getLong("skyblock-leveling.combat-skill-xp.empowered-bonus", 2L);

        bossCombatSkillBaseXp = config.getLong("skyblock-leveling.combat-skill-xp.boss-base", 24L);
        bossCombatSkillLevelsPerStep = Math.max(1, config.getInt("skyblock-leveling.combat-skill-xp.boss-extra-every-levels", 2));
        bossCombatSkillXpPerStep = config.getLong("skyblock-leveling.combat-skill-xp.boss-extra-per-step", 2L);
        bossCombatSkillMinimumLevel = config.getInt("skyblock-leveling.combat-skill-xp.boss-default-level", 15);
        miningOreXp = config.getLong("skyblock-leveling.action-xp.mining-ore", 0L);
        foragingLogXp = config.getLong("skyblock-leveling.action-xp.foraging-log", 0L);
        farmingHarvestXp = config.getLong("skyblock-leveling.action-xp.farming-harvest", 0L);
        fishingCatchXp = config.getLong("skyblock-leveling.action-xp.fishing-catch", 0L);
        dungeonCompleteXp = config.getLong("skyblock-leveling.action-xp.dungeon-complete", 0L);
        islandCreateXp = config.getLong("skyblock-leveling.action-xp.island-create", 0L);
        islandUpgradeXp = config.getLong("skyblock-leveling.action-xp.island-upgrade", 0L);
        
        bestiaryTierSize = config.getInt("skyblock-leveling.bestiary.tier-size", 10);
        bestiaryTierXp = config.getLong("skyblock-leveling.bestiary.family-tier-xp", 1L);
        bestiaryMilestoneEveryTiers = config.getInt("skyblock-leveling.bestiary.milestone-every-tiers", 10);
        bestiaryMilestoneXp = config.getLong("skyblock-leveling.bestiary.milestone-xp", 10L);

        loadTracks(config.getConfigurationSection("skyblock-leveling.tracks"));
        loadFeatureUnlocks(config.getConfigurationSection("skyblock-leveling.level-rewards.feature-unlocks"));
    }

    private void loadTracks(ConfigurationSection section) {
        tracks.clear(); tracksById.clear(); tracksByCounter.clear();
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            GuideTrack t = parseTrack(key, section.getConfigurationSection(key));
            if (t != null) registerTrack(t);
        }
    }

    private GuideTrack parseTrack(String id, ConfigurationSection sec) {
        if (sec == null) return null;
        String name = sec.getString("display-name", id);
        String ckey = normalize(sec.getString("counter-key", id));
        Material icon = parseMaterial(sec.getString("icon", "BOOK"), Material.BOOK);
        List<Long> milestones = parseLongList(sec.getList("milestones"));
        List<Long> rewards = parseLongList(sec.getList("rewards"));
        return new GuideTrack(id, name, icon, ckey, milestones, rewards, sec.getStringList("lore"));
    }

    private void registerTrack(GuideTrack t) {
        tracks.add(t); tracksById.put(t.id(), t);
        tracksByCounter.computeIfAbsent(t.counterKey(), k -> new ArrayList<>()).add(t);
    }

    private void loadFeatureUnlocks(ConfigurationSection section) {
        featureUnlocks.clear();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    int lv = Integer.parseInt(key.trim());
                    featureUnlocks.computeIfAbsent(lv, k -> new ArrayList<>()).addAll(section.getStringList(key));
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    public long getXpRequiredForSkillLevel(int level) { return SkillXPTable.getXpRequired(level); }

    private void markDirty() { dirty = true; }

    private void startAutoSave() {
        if (Bukkit.getServer() == null) return;
        autoSaveTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> { if (dirty) save(); }, 20L * autoSaveIntervalSeconds, 20L * autoSaveIntervalSeconds);
    }

    private void stopAutoSave() { if (autoSaveTask != null) autoSaveTask.cancel(); }

    public UUID resolveProfileId(Player player) {
        if (player == null) return null;
        if (plugin instanceof GriviencePlugin g && g.getProfileManager() != null) {
            SkyBlockProfile p = g.getProfileManager().getSelectedProfile(player);
            return p != null ? p.getCanonicalProfileId() : player.getUniqueId();
        }
        return player.getUniqueId();
    }

    public UUID resolveOfflineProfileId(OfflinePlayer player) {
        if (player == null) return null;
        if (player.isOnline()) return resolveProfileId(player.getPlayer());
        
        if (plugin instanceof GriviencePlugin g && g.getProfileManager() != null) {
            return g.getProfileManager().getSelectedProfileId(player.getUniqueId());
        }
        return player.getUniqueId();
    }

    public String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('.', '_');
    }

    public String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }

    private List<Long> parseLongList(List<?> in) {
        if (in == null) return Collections.emptyList();
        List<Long> out = new ArrayList<>();
        for (Object o : in) if (o instanceof Number n) out.add(n.longValue());
        return out;
    }

    private Material parseMaterial(String s, Material def) {
        try { return Material.valueOf(s.toUpperCase(Locale.ROOT)); } catch (Exception e) { return def; }
    }

    public boolean isMiningMaterial(Material m) { return m.name().endsWith("_ORE") || m == Material.ANCIENT_DEBRIS || m == Material.OBSIDIAN || m == Material.CRYING_OBSIDIAN || m == Material.GLOWSTONE || m.name().contains("AMETHYST"); }
    public boolean isForagingMaterial(Material m) { return m.name().endsWith("_LOG") || m.name().endsWith("_WOOD"); }
    public boolean isFarmingMaterial(Material m) { return m == Material.WHEAT || m == Material.CARROTS || m == Material.POTATOES || m == Material.NETHER_WART || m == Material.SUGAR_CANE || m == Material.MELON || m == Material.PUMPKIN || m == Material.CACTUS; }

    public boolean hasMinerFullSet(Player player) {
        return armorManager != null && armorManager.hasEquippedPieces(player, "miner", 4);
    }

    public boolean hasDeepcoreFullSet(Player player) {
        return armorManager != null && armorManager.hasEquippedPieces(player, "deepcore", 4);
    }

    public List<GuideTrack> tracks() { return Collections.unmodifiableList(tracks); }
    public GuideTrack track(String id) { return tracksById.get(normalize(id)); }
    public GuideProgress progressFor(Player player, GuideTrack track) {
        UUID pid = resolveProfileId(player);
        if (pid == null || track == null) return null;
        long val = getCounter(pid, track.counterKey());
        int stage = getMilestoneStage(pid, track.id());
        int total = track.milestones().size();
        if (stage >= total) return new GuideProgress(track, val, total, total, 0, 0, 0, 1.0, true);
        long next = track.milestones().get(stage);
        long prev = stage == 0 ? 0 : track.milestones().get(stage-1);
        double prog = (double)(val - prev) / (next - prev);
        return new GuideProgress(track, val, stage, total, prev, next, track.rewards().get(stage), prog, false);
    }

    private boolean isBoss(LivingEntity e) {
        if (e == null) return false;
        PersistentDataContainer container = e.getPersistentDataContainer();
        Byte bossMarker = container.get(namespacedKey("boss_mob"), PersistentDataType.BYTE);
        return bossMarker != null && bossMarker == 1;
    }

    private boolean isBossEntity(EntityType t) {
        return t == EntityType.ENDER_DRAGON || t == EntityType.WITHER || t == EntityType.WARDEN;
    }

    private String bossObjectiveMobId(LivingEntity e, String fallback) {
        if (e == null) return fallback;
        PersistentDataContainer container = e.getPersistentDataContainer();
        String customId = container.get(namespacedKey("custom_monster"), PersistentDataType.STRING);
        return customId != null ? normalize(customId) : fallback;
    }

    private Integer resolveTaggedMonsterLevel(LivingEntity e) {
        if (e == null) return null;
        PersistentDataContainer container = e.getPersistentDataContainer();
        if (container.has(namespacedKey("monster_level"), PersistentDataType.INTEGER)) {
            return container.get(namespacedKey("monster_level"), PersistentDataType.INTEGER);
        }
        return null;
    }

    private boolean isEmpowered(LivingEntity e) {
        if (e == null) return false;
        PersistentDataContainer container = e.getPersistentDataContainer();
        Byte val = container.get(namespacedKey("end_mines_empowered"), PersistentDataType.BYTE);
        return val != null && val == 1;
    }

    private long scaledCombatSkillXp(int lv, long b, int s, long p) { 
        if (s <= 0) return b; // Avoid division by zero
        return b + (lv / s) * p; 
    }
    private NamespacedKey namespacedKey(String k) { return new NamespacedKey(plugin, k); }
    private String categoryCounterKey(String c) { return "category_xp." + normalize(c); }
    private long xpCap() { return maxLevel <= 0 ? Long.MAX_VALUE : (long) maxLevel * xpPerLevel; }
    private long clampXp(long r) { long c = xpCap(); return c == Long.MAX_VALUE ? Math.max(0, r) : Math.min(Math.max(0, r), c); }
    private UUID parseUuid(String s) { try { return UUID.fromString(s); } catch (Exception e) { return null; } }

    public record FeatureUnlock(int level, List<String> unlocks) {}
    public record GuideTrack(String id, String displayName, Material icon, String counterKey, List<Long> milestones, List<Long> rewards, List<String> lore) {}
    public record GuideProgress(GuideTrack track, long counterValue, int claimedMilestones, int totalMilestones, long previousThreshold, long nextThreshold, long nextRewardXp, double progressToNext, boolean completed) {}
}
