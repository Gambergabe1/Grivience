package io.papermc.Grivience.stats;

import io.papermc.Grivience.skills.SkyblockSkillManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Provides Skyblock-style base stats (strength, crits, intelligence, defense) derived
 * from Skyblock level and skills without any external dependencies.
 */
public final class SkyblockStatsManager {
    private final JavaPlugin plugin;
    private final SkyblockLevelManager levelManager;
    private SkyblockSkillManager skillManager;

    private double baseStrength;
    private double baseCritChancePercent;
    private double baseCritDamagePercent;
    private double baseIntelligence;
    private double baseDefense;

    private double perLevelStrength;
    private double perLevelCritChancePercent;
    private double perLevelCritDamagePercent;
    private double perLevelIntelligence;
    private double perLevelDefense;

    public SkyblockStatsManager(JavaPlugin plugin, SkyblockLevelManager levelManager) {
        this.plugin = plugin;
        this.levelManager = levelManager;
    }

    public void setSkillManager(SkyblockSkillManager skillManager) {
        this.skillManager = skillManager;
    }

    public void reload() {
        FileConfiguration cfg = plugin.getConfig();
        baseStrength = cfg.getDouble("skyblock-stats.base.strength", 0.0D);
        // Skyblock defaults: 30% crit chance, 50% crit damage, 100 intelligence (mana).
        baseCritChancePercent = cfg.getDouble("skyblock-stats.base.crit-chance", 30.0D);
        baseCritDamagePercent = cfg.getDouble("skyblock-stats.base.crit-damage", 50.0D);
        baseIntelligence = cfg.getDouble("skyblock-stats.base.intelligence", 100.0D);
        baseDefense = cfg.getDouble("skyblock-stats.base.defense", 0.0D);

        perLevelStrength = cfg.getDouble("skyblock-stats.per-level.strength", 0.0D);
        perLevelCritChancePercent = cfg.getDouble("skyblock-stats.per-level.crit-chance", 0.0D);
        perLevelCritDamagePercent = cfg.getDouble("skyblock-stats.per-level.crit-damage", 0.0D);
        perLevelIntelligence = cfg.getDouble("skyblock-stats.per-level.intelligence", 0.0D);
        perLevelDefense = cfg.getDouble("skyblock-stats.per-level.defense", 0.0D);
    }

    public double getStrength(Player player) {
        if (player == null) {
            return baseStrength;
        }
        int level = levelManager.getLevel(player);
        int levelRewardStrength = levelManager.getStrengthBonus(player);
        double skillBonus = skillManager == null ? 0.0 : skillManager.getStatBonus(player, "Strength");
        return baseStrength + perLevelStrength * level + levelRewardStrength + skillBonus;
    }

    /**
     * Returns crit chance as percent (not decimal). e.g. 25 means 25%.
     */
    public double getCritChance(Player player) {
        int level = levelManager.getLevel(player);
        double skillBonus = skillManager == null ? 0.0 : skillManager.getStatBonus(player, "Crit Chance");
        return baseCritChancePercent + perLevelCritChancePercent * level + skillBonus;
    }

    /**
     * Returns crit damage as percent (not decimal).
     */
    public double getCritDamage(Player player) {
        int level = levelManager.getLevel(player);
        double skillBonus = skillManager == null ? 0.0 : skillManager.getStatBonus(player, "Crit Damage");
        return baseCritDamagePercent + perLevelCritDamagePercent * level + skillBonus;
    }

    public double getIntelligence(Player player) {
        int level = levelManager.getLevel(player);
        double skillBonus = skillManager == null ? 0.0 : skillManager.getStatBonus(player, "Intelligence");
        return baseIntelligence + perLevelIntelligence * level + skillBonus;
    }

    public double getDefense(Player player) {
        int level = levelManager.getLevel(player);
        double skillBonus = skillManager == null ? 0.0 : skillManager.getStatBonus(player, "Defense");
        return baseDefense + perLevelDefense * level + skillBonus;
    }

    public double getFarmingFortune(Player player) {
        int levelReward = levelManager.getFarmingFortuneBonus(player);
        double skillBonus = skillManager == null ? 0.0 : skillManager.getStatBonus(player, "Farming Fortune");
        return levelReward + skillBonus;
    }

    public int getLevel(Player player) {
        return levelManager.getLevel(player);
    }

    public SkyblockLevelManager getLevelManager() {
        return levelManager;
    }
}

