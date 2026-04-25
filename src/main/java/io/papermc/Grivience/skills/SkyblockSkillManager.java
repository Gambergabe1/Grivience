package io.papermc.Grivience.skills;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import io.papermc.Grivience.stats.SkyblockLevelManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Reworked Skyblock Skill Manager.
 * 
 * Release-ready features:
 * - Unified XP curve via SkillXPTable (Skyblock-accurate)
 * - Sole source of truth: SkyBlockProfile (Persistent across restarts)
 * - Automated Stat Rewards per level
 * - Integrated Skyblock XP rewards for leveling up
 * - Support for multiple profiles and offline lookups
 */
public final class SkyblockSkillManager {
    private final GriviencePlugin plugin;
    private final SkyblockLevelManager levelManager;

    private final Map<UUID, org.bukkit.boss.BossBar> activeSkillBars = new HashMap<>();
    private final Map<UUID, org.bukkit.scheduler.BukkitTask> skillBarTasks = new HashMap<>();

    public SkyblockSkillManager(GriviencePlugin plugin, SkyblockLevelManager levelManager) {
        this.plugin = plugin;
        this.levelManager = levelManager;
    }

    // ==================== CORE API ====================

    /**
     * Get the current level of a skill for a player.
     */
    public int getLevel(Player player, SkyblockSkill skill) {
        SkyBlockProfile profile = getProfile(player);
        return profile == null ? 0 : profile.getSkillLevel(skill.name());
    }

    /**
     * Get the current level of a skill for a specific profile.
     */
    public int getLevel(UUID profileId, SkyblockSkill skill) {
        SkyBlockProfile profile = getProfile(profileId);
        return profile == null ? 0 : profile.getSkillLevel(skill.name());
    }

    /**
     * Get the total XP for a skill.
     */
    public double getXp(Player player, SkyblockSkill skill) {
        SkyBlockProfile profile = getProfile(player);
        return profile == null ? 0.0D : (double) profile.getSkillXp(skill.name());
    }

    /**
     * Get the total XP for a skill on a specific profile.
     */
    public double getXp(UUID profileId, SkyblockSkill skill) {
        SkyBlockProfile profile = getProfile(profileId);
        return profile == null ? 0.0D : (double) profile.getSkillXp(skill.name());
    }

    /**
     * Add skill XP and handle level-ups.
     */
    public void addXp(Player player, SkyblockSkill skill, long amount) {
        if (player == null || skill == null || amount <= 0L) return;

        SkyBlockProfile profile = getProfile(player);
        if (profile == null) return;

        // Apply multipliers (e.g. from pets)
        double multiplier = 1.0;
        if (plugin.getPetManager() != null) {
            multiplier *= plugin.getPetManager().getSkillXpMultiplier(player, skill);
        }

        // Apply XP boosts from welcome event
        if (plugin.getWelcomeManager() != null && plugin.getWelcomeManager().getXPBoostManager() != null) {
            io.papermc.Grivience.welcome.XPBoostManager.BoostType boostType = switch (skill) {
                case MINING -> io.papermc.Grivience.welcome.XPBoostManager.BoostType.MINING;
                case FARMING -> io.papermc.Grivience.welcome.XPBoostManager.BoostType.FARMING;
                case COMBAT -> io.papermc.Grivience.welcome.XPBoostManager.BoostType.COMBAT;
                case FORAGING -> io.papermc.Grivience.welcome.XPBoostManager.BoostType.FORAGING;
                case FISHING -> io.papermc.Grivience.welcome.XPBoostManager.BoostType.FISHING;
                default -> null;
            };
            if (boostType != null) {
                multiplier = plugin.getWelcomeManager().getXPBoostManager().applyBoost(multiplier, player, boostType);
            }
        }

        long finalAmount = Math.round(amount * multiplier);

        String skillKey = skill.name();
        long currentTotalXp = profile.getSkillXp(skillKey);
        long newTotalXp = currentTotalXp + finalAmount;
        
        int oldLevel = SkillXPTable.getLevelFromXp(currentTotalXp);
        int newLevel = SkillXPTable.getLevelFromXp(newTotalXp);

        // Update profile
        profile.setSkillXp(skillKey, newTotalXp);
        profile.setSkillLevel(skillKey, newLevel);

        // UI Feedback
        showXpGain(player, skill, finalAmount, newTotalXp, newLevel);

        // Level Up Logic
        if (newLevel > oldLevel) {
            handleLevelUp(player, skill, oldLevel, newLevel);
        }

        // Persistent save queue
        if (plugin.getProfileManager() != null) {
            plugin.getProfileManager().syncSkillSnapshotsAndQueueSave(player);
        }
    }

    // ==================== STATS & BONUSES ====================

    /**
     * Gets the stat bonus provided by all skills for a player.
     */
    public double getStatBonus(Player player, String statType) {
        if (player == null || statType == null) return 0.0D;
        SkyBlockProfile profile = getProfile(player);
        if (profile == null) return 0.0D;

        String normalizedType = statType.toLowerCase(Locale.ROOT);
        double total = 0.0D;

        for (SkyblockSkill skill : SkyblockSkill.values()) {
            int level = profile.getSkillLevel(skill.name());
            if (level <= 0) continue;

            // Primary Stat
            if (skill.getStatName().toLowerCase(Locale.ROOT).contains(normalizedType)) {
                total += skill.getPerkValue(level);
            }
            
            // Secondary Stats
            total += getSecondaryStatBonus(skill, level, normalizedType);
        }
        return total;
    }

    private double getSecondaryStatBonus(SkyblockSkill skill, int level, String statType) {
        return switch (skill) {
            case COMBAT -> statType.contains("strength") ? level * 0.5 : 0;
            case MINING -> statType.contains("mining fortune") ? level * 2.0 : 0;
            case FORAGING -> statType.contains("foraging fortune") ? level * 2.0 : 0;
            case FARMING -> statType.contains("farming fortune") ? level * 2.0 : 0;
            case FISHING -> statType.contains("sea creature chance") ? level * 0.1 : 0;
            case TAMING -> statType.contains("pet luck") ? level * 1.0 : 0;
            case ALCHEMY -> statType.contains("speed") ? (level / 10.0) : 0;
            default -> 0;
        };
    }

    public double getSkillAverage(Player player) {
        SkyBlockProfile profile = getProfile(player);
        if (profile == null) return 0.0D;
        
        int totalLevels = 0;
        for (SkyblockSkill skill : SkyblockSkill.values()) {
            totalLevels += profile.getSkillLevel(skill.name());
        }
        return (double) totalLevels / SkyblockSkill.values().length;
    }

    public double getSkillAverage(UUID profileId) {
        SkyBlockProfile profile = getProfile(profileId);
        if (profile == null) return 0.0D;
        int total = 0;
        for (SkyblockSkill s : SkyblockSkill.values()) total += profile.getSkillLevel(s.name());
        return (double) total / SkyblockSkill.values().length;
    }

    // ==================== INTERNAL HELPERS ====================

    private void handleLevelUp(Player player, SkyblockSkill skill, int oldLevel, int newLevel) {
        for (int level = oldLevel + 1; level <= newLevel; level++) {
            String skillName = skill.getDisplayName();
            
            player.sendMessage(ChatColor.DARK_GRAY + "--------------------------------------------------");
            player.sendMessage(ChatColor.GOLD + "  SKILL LEVEL UP " + ChatColor.AQUA + skillName + " " + level);
            player.sendMessage("");
            player.sendMessage(ChatColor.GREEN + "  REWARDS");
            player.sendMessage(ChatColor.GRAY + "  \u25cf " + ChatColor.AQUA + "+" + skill.getStatName());
            
            if (skill.getPerkName() != null && !skill.getPerkName().equalsIgnoreCase("None")) {
                player.sendMessage(ChatColor.GRAY + "  \u25cf " + ChatColor.YELLOW + skill.getPerkName() + " Perk increased!");
            }

            // Skyblock Level XP Reward
            if (levelManager != null) {
                long rewardXp = calculateSkyblockXpReward(level);
                if (rewardXp > 0) {
                    levelManager.awardCategoryXp(player, "skill", rewardXp, skillName + " Level " + level, false);
                    player.sendMessage(ChatColor.GRAY + "  \u25cf " + ChatColor.DARK_AQUA + "+" + rewardXp + " Skyblock XP");
                }
            }

            player.sendMessage(ChatColor.DARK_GRAY + "--------------------------------------------------");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        }
    }

    private long calculateSkyblockXpReward(int level) {
        // Skyblock XP rewards for leveling up skills (Skyblock-accurate)
        if (level <= 10) return 5L;
        if (level <= 25) return 10L;
        if (level <= 50) return 20L;
        return 30L;
    }

    private void showXpGain(Player player, SkyblockSkill skill, long amount, long totalXp, int level) {
        double progress = SkillXPTable.getProgress(totalXp);
        long currentLevelBase = SkillXPTable.getCumulativeXp(level);
        long nextLevelBase = SkillXPTable.getCumulativeXp(level + 1);
        long reqForNext = nextLevelBase - currentLevelBase;
        long gainedInLevel = totalXp - currentLevelBase;

        String progressBar = level >= SkillXPTable.getMaxLevel() ? "MAX LEVEL" : 
                String.format(Locale.US, "%,d/%,d", gainedInLevel, reqForNext);

        String actionMsg = String.format("§3+%d %s §8(%s)", amount, skill.getDisplayName(), progressBar);
        player.sendActionBar(net.kyori.adventure.text.Component.text(actionMsg));

        updateBossBar(player, skill, level, progress);
    }

    private void updateBossBar(Player player, SkyblockSkill skill, int level, double progress) {
        if (Bukkit.getServer() == null) return;
        UUID uuid = player.getUniqueId();
        if (skillBarTasks.containsKey(uuid)) skillBarTasks.get(uuid).cancel();

        org.bukkit.boss.BossBar bar = activeSkillBars.computeIfAbsent(uuid, k -> {
            org.bukkit.boss.BossBar newBar = Bukkit.createBossBar("", getBarColor(skill), org.bukkit.boss.BarStyle.SOLID);
            newBar.addPlayer(player);
            return newBar;
        });

        bar.setTitle(String.format("§6%s §eLevel %d §7- §e%d%%", skill.getDisplayName(), level, (int)(progress * 100)));
        bar.setProgress(Math.min(1.0, progress));
        bar.setColor(getBarColor(skill));
        bar.setVisible(true);

        skillBarTasks.put(uuid, Bukkit.getScheduler().runTaskLater(plugin, () -> {
            bar.setVisible(false);
            activeSkillBars.remove(uuid);
            skillBarTasks.remove(uuid);
        }, 60L));
    }

    private org.bukkit.boss.BarColor getBarColor(SkyblockSkill skill) {
        return switch (skill) {
            case MINING -> org.bukkit.boss.BarColor.WHITE;
            case COMBAT -> org.bukkit.boss.BarColor.RED;
            case FARMING -> org.bukkit.boss.BarColor.GREEN;
            case FORAGING -> org.bukkit.boss.BarColor.YELLOW;
            case FISHING -> org.bukkit.boss.BarColor.BLUE;
            case ALCHEMY, ENCHANTING -> org.bukkit.boss.BarColor.PURPLE;
            default -> org.bukkit.boss.BarColor.PINK;
        };
    }

    private SkyBlockProfile getProfile(Player player) {
        return plugin.getProfileManager() != null ? plugin.getProfileManager().getSelectedProfile(player) : null;
    }

    private SkyBlockProfile getProfile(UUID profileId) {
        if (plugin.getProfileManager() == null || profileId == null) return null;
        
        // Try looking up by Profile ID directly first (most common for skill sync)
        SkyBlockProfile profile = plugin.getProfileManager().findProfileById(profileId);
        if (profile != null) return profile;
        
        // Fallback: If not a profile ID, maybe it's an owner ID (offline lookup)
        return plugin.getProfileManager().getSelectedProfile(profileId);
    }

    // ==================== LEGACY / COMPATIBILITY ====================

    public int getMaxLevel() { return SkillXPTable.getMaxLevel(); }
    public int getMaxLevel(SkyblockSkill skill) { return skill == SkyblockSkill.DUNGEONEERING ? 50 : 60; }
    public double getXpForLevel(SkyblockSkill skill, int level) { return (double) SkillXPTable.getCumulativeXp(level); }
    public int getTrackedSkillCount() { return SkyblockSkill.values().length; }
    
    public int getTotalSkillLevels(Player player) {
        SkyBlockProfile profile = getProfile(player);
        if (profile == null) return 0;
        int total = 0;
        for (SkyblockSkill s : SkyblockSkill.values()) total += profile.getSkillLevel(s.name());
        return total;
    }

    public String getHighestSkill(Player player) {
        SkyBlockProfile profile = getProfile(player);
        if (profile == null) return null;
        SkyblockSkill best = null;
        int max = -1;
        for (SkyblockSkill s : SkyblockSkill.values()) {
            int lvl = profile.getSkillLevel(s.name());
            if (lvl > max) { max = lvl; best = s; }
        }
        return best == null ? null : best.name();
    }

    public double getPerkValue(Player player, SkyblockSkill skill) {
        if (skill == null) return 0.0D;
        return skill.getPerkValue(getLevel(player, skill));
    }

    /**
     * Generates a MiniMessage formatted hover string showing all skill levels.
     */
    public String getSkillHover(UUID profileId) {
        SkyBlockProfile profile = getProfile(profileId);
        if (profile == null) return "<red>No Profile Data</red>";

        StringBuilder sb = new StringBuilder("<gold><bold>Skill Levels</bold></gold>");
        for (SkyblockSkill skill : SkyblockSkill.values()) {
            int level = profile.getSkillLevel(skill.name());
            long xp = profile.getSkillXp(skill.name());
            
            sb.append("\n<gray>• <white>").append(skill.getDisplayName()).append(": </white>")
              .append("<aqua>").append(level).append(" </aqua>")
              .append("<dark_gray>(").append(String.format(Locale.US, "%,d", xp)).append(" XP)</dark_gray>");
        }
        
        double avg = getSkillAverage(profileId);
        sb.append("\n\n<yellow>Skill Average: </yellow><gold>").append(String.format(Locale.US, "%.1f", avg)).append("</gold>");
        
        return sb.toString();
    }
}
