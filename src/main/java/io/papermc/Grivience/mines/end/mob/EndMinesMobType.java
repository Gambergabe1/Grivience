package io.papermc.Grivience.mines.end.mob;

import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;

import java.util.Locale;

public enum EndMinesMobType {
    RIFTWALKER("riftwalker", EntityType.ENDERMAN, ChatColor.DARK_PURPLE + "Riftwalker", 220.0D, 14.0D, 0.30D, 55, 10),
    CRYSTAL_SENTRY("crystal_sentry", EntityType.SHULKER, ChatColor.LIGHT_PURPLE + "Crystal Sentry", 160.0D, 10.0D, 0.0D, 30, 9),
    OBSIDIAN_GOLEM("obsidian_golem", EntityType.RAVAGER, ChatColor.DARK_GRAY + "Obsidian Golem", 320.0D, 18.0D, 0.24D, 15, 12),
    
    ZOMBIE_MINER("zombie_miner", EntityType.ZOMBIE, ChatColor.DARK_GREEN + "Zombie Miner", 100.0D, 8.0D, 0.23D, 40, 7),
    SKELETON_WATCHER("skeleton_watcher", EntityType.SKELETON, ChatColor.GRAY + "Skeleton Watcher", 120.0D, 10.0D, 0.25D, 35, 8),
    CAVE_SPIDER_SWARM("cave_spider_swarm", EntityType.CAVE_SPIDER, ChatColor.DARK_AQUA + "Cave Spider Swarm", 60.0D, 6.0D, 0.35D, 30, 6),
    ENDERMAN_EXCAVATOR("enderman_excavator", EntityType.ENDERMAN, ChatColor.LIGHT_PURPLE + "Enderman Excavator", 250.0D, 15.0D, 0.30D, 10, 11),
    IRON_GOLEM_GUARDIAN("iron_golem_guardian", EntityType.IRON_GOLEM, ChatColor.GOLD + "Iron Golem Guardian", 1000.0D, 25.0D, 0.20D, 0, 15);

    private final String id;
    private final EntityType entityType;
    private final String displayName;
    private final double maxHealth;
    private final double attackDamage;
    private final double moveSpeed;
    private final int weight;
    private final int baseLevel;

    EndMinesMobType(
            String id,
            EntityType entityType,
            String displayName,
            double maxHealth,
            double attackDamage,
            double moveSpeed,
            int weight,
            int baseLevel
    ) {
        this.id = id;
        this.entityType = entityType;
        this.displayName = displayName;
        this.maxHealth = maxHealth;
        this.attackDamage = attackDamage;
        this.moveSpeed = moveSpeed;
        this.weight = weight;
        this.baseLevel = baseLevel;
    }

    public String id() {
        return id;
    }

    public EntityType entityType() {
        return entityType;
    }

    public String displayName() {
        return displayName;
    }

    public double maxHealth() {
        return maxHealth;
    }

    public double attackDamage() {
        return attackDamage;
    }

    public double moveSpeed() {
        return moveSpeed;
    }

    public int weight() {
        return weight;
    }

    public int baseLevel() {
        return baseLevel;
    }

    public static EndMinesMobType parse(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String normalized = input.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        for (EndMinesMobType type : values()) {
            if (type.id.equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
