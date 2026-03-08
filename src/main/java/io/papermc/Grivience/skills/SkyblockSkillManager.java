package io.papermc.Grivience.skills;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.stats.SkyblockLevelManager;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * Manages Skyblock skill leveling and rewards.
 */
public final class SkyblockSkillManager {
    private static final int MAX_SKILL_LEVEL = 60;
    private static final String SKILL_XP_PREFIX = "skill_xp.";

    private final SkyblockLevelManager levelManager;

    public SkyblockSkillManager(GriviencePlugin plugin, SkyblockLevelManager levelManager) {
        this.levelManager = levelManager;
    }

    public int getLevel(Player player, SkyblockSkill skill) {
        if (player == null || skill == null || levelManager == null) {
            return 0;
        }
        int maxLevel = getMaxLevel(skill);

        int pseudoLevel = switch (skill) {
            case DUNGEONEERING -> levelManager.getPseudoSkillLevel(player, "catacombs");
            case HUNTING, TAMING -> 0;
            default -> levelManager.getPseudoSkillLevel(player, skill.name().toLowerCase(Locale.ROOT));
        };

        if (pseudoLevel > 0) {
            return Math.min(maxLevel, pseudoLevel);
        }

        return Math.min(maxLevel, levelFromXp(skill, getXp(player, skill)));
    }

    public double getXp(Player player, SkyblockSkill skill) {
        if (player == null || skill == null || levelManager == null) {
            return 0.0D;
        }
        return levelManager.getCounter(player, skillXpCounter(skill));
    }

    public int getMaxLevel() {
        return MAX_SKILL_LEVEL;
    }

    public int getMaxLevel(SkyblockSkill skill) {
        if (skill == SkyblockSkill.DUNGEONEERING) {
            return 50;
        }
        return MAX_SKILL_LEVEL;
    }

    public long getXpPerLevel(SkyblockSkill skill) {
        return xpPerLevel(skill);
    }

    public double getXpForLevel(SkyblockSkill skill, int level) {
        if (skill == null) {
            return 0.0D;
        }
        int clamped = Math.max(0, Math.min(getMaxLevel(skill), level));
        return (double) clamped * (double) xpPerLevel(skill);
    }

    public double getXpForLevel(int level) {
        return getXpForLevel(SkyblockSkill.COMBAT, level);
    }

    public void addXp(Player player, SkyblockSkill skill, long amount) {
        if (player == null || skill == null || amount <= 0L || levelManager == null) {
            return;
        }
        String counterKey = skillXpCounter(skill);
        long currentXp = Math.max(0L, levelManager.getCounter(player, counterKey));
        long minimumForCurrentLevel = (long) getXpForLevel(skill, getLevel(player, skill));
        if (minimumForCurrentLevel > currentXp) {
            levelManager.addToCounter(player, counterKey, minimumForCurrentLevel - currentXp);
        }
        levelManager.addToCounter(player, counterKey, amount);
    }

    /**
     * Gets the stat bonus provided by all skills for a player.
     */
    public double getStatBonus(Player player, String statType) {
        if (player == null || statType == null || statType.isBlank()) {
            return 0.0D;
        }

        String normalizedStatType = statType.toLowerCase(Locale.ROOT);
        double total = 0.0D;
        for (SkyblockSkill skill : SkyblockSkill.values()) {
            int level = getLevel(player, skill);
            if (level <= 0) {
                continue;
            }

            if (skill.getStatName().toLowerCase(Locale.ROOT).contains(normalizedStatType)) {
                total += getStatValueForLevel(skill, level);
            }
        }
        return total;
    }

    private double getStatValueForLevel(SkyblockSkill skill, int level) {
        // Hypixel-like stat scaling
        return switch (skill) {
            case COMBAT -> level * 0.5; // +0.5% Crit Chance per level
            case MINING -> level * 2.0; // +2 Defense per level
            case FARMING, FISHING, DUNGEONEERING, CARPENTRY -> {
                // Health scaling: 2 per level for 1-14, 3 for 15-19, etc.
                if (level < 15) yield level * 2.0;
                if (level < 20) yield 14 * 2.0 + (level - 14) * 3.0;
                if (level < 25) yield 14 * 2.0 + 5 * 3.0 + (level - 19) * 4.0;
                yield 14 * 2.0 + 5 * 3.0 + 5 * 4.0 + (level - 24) * 5.0;
            }
            case FORAGING -> level * 1.0; // +1 Strength per level
            case ENCHANTING, ALCHEMY -> level * 2.0; // +2 Intelligence per level
            case TAMING -> level * 1.0; // +1 Pet Luck per level
            default -> 0;
        };
    }

    public double getPerkValue(Player player, SkyblockSkill skill) {
        if (skill == null) {
            return 0.0D;
        }
        return skill.getPerkValue(getLevel(player, skill));
    }

    public int getTrackedSkillCount() {
        return SkyblockSkill.values().length;
    }

    public int getTotalSkillLevels(Player player) {
        if (player == null) {
            return 0;
        }
        int total = 0;
        for (SkyblockSkill skill : SkyblockSkill.values()) {
            total += getLevel(player, skill);
        }
        return total;
    }

    public double getSkillAverage(Player player) {
        int tracked = getTrackedSkillCount();
        if (player == null || tracked <= 0) {
            return 0.0D;
        }
        return getTotalSkillLevels(player) / (double) tracked;
    }

    public SkyblockSkill getHighestSkill(Player player) {
        if (player == null) {
            return null;
        }

        SkyblockSkill best = null;
        int bestLevel = Integer.MIN_VALUE;
        for (SkyblockSkill skill : SkyblockSkill.values()) {
            int level = getLevel(player, skill);
            if (best == null || level > bestLevel) {
                best = skill;
                bestLevel = level;
            }
        }
        return best;
    }

    private long xpPerLevel(SkyblockSkill skill) {
        if (skill == null) {
            return 1L;
        }

        if (levelManager == null) {
            return defaultXpPerLevel(skill);
        }

        long configured = switch (skill) {
            case ALCHEMY -> safe(levelManager.getSkillActionsPerLevel(skill.name())) * 5L;
            case HUNTING -> safe(levelManager.getSkillActionsPerLevel("combat"));
            case TAMING -> 100L;
            default -> safe(levelManager.getSkillActionsPerLevel(skill.name()));
        };
        return Math.max(1L, configured);
    }

    private long defaultXpPerLevel(SkyblockSkill skill) {
        return switch (skill) {
            case COMBAT, HUNTING -> 40L;
            case MINING, FORAGING -> 140L;
            case FARMING -> 120L;
            case FISHING -> 20L;
            case DUNGEONEERING -> 3L;
            case ENCHANTING -> 50L;
            case ALCHEMY, CARPENTRY, TAMING -> 100L;
        };
    }

    private int levelFromXp(SkyblockSkill skill, double xp) {
        long safeXp = (long) Math.max(0.0D, xp);
        long perLevel = Math.max(1L, xpPerLevel(skill));
        long computed = safeXp / perLevel;
        int maxLevel = skill == null ? MAX_SKILL_LEVEL : getMaxLevel(skill);
        return (int) Math.min(maxLevel, Math.max(0L, computed));
    }

    private long safe(long value) {
        return Math.max(1L, value);
    }

    private String skillXpCounter(SkyblockSkill skill) {
        return SKILL_XP_PREFIX + skill.name().toLowerCase(Locale.ROOT);
    }
}
