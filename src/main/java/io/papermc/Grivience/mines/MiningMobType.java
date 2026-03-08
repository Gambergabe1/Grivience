package io.papermc.Grivience.mines;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.Locale;

/**
 * Enum representing custom mining mob types with their configurations.
 * All mobs use vanilla models with adjusted stats.
 */
public enum MiningMobType {
    /**
     * Zombie with iron helmet and iron pickaxe.
     * Higher health and damage in deeper layers.
     */
    ZOMBIE_MINER(
            "zombie_miner",
            EntityType.ZOMBIE,
            ChatColor.GREEN + "Zombie Miner",
            40.0D,  // Base health
            6.0D,   // Base damage
            0.23D,  // Movement speed
            50,     // Weight for spawning
            Material.IRON_HELMET,
            Material.IRON_PICKAXE,
            ChatColor.GRAY + "A undead miner drawn to the depths.",
            ChatColor.BLUE + "" + ChatColor.BOLD + "COMMON MINING MOB"
    ),

    /**
     * Skeleton with chain armor guarding rail corridors.
     */
    SKELETON_WATCHER(
            "skeleton_watcher",
            EntityType.SKELETON,
            ChatColor.WHITE + "Skeleton Watcher",
            35.0D,
            7.0D,
            0.20D,
            40,
            Material.CHAINMAIL_HELMET,
            Material.CHAINMAIL_CHESTPLATE,
            ChatColor.GRAY + "Guards the abandoned rail corridors.",
            ChatColor.BLUE + "" + ChatColor.BOLD + "COMMON MINING MOB"
    ),

    /**
     * Spawned only in abandoned shafts. Increased movement speed.
     */
    CAVE_SPIDER_SWARM(
            "cave_spider_swarm",
            EntityType.CAVE_SPIDER,
            ChatColor.DARK_GREEN + "Cave Spider Swarm",
            12.0D,
            4.0D,
            0.35D,  // Increased movement speed
            60,
            null,
            null,
            ChatColor.GRAY + "A venomous spider from abandoned shafts.",
            ChatColor.GREEN + "" + ChatColor.BOLD + "UNCOMMON MINING MOB"
    ),

    /**
     * Rare spawn in diamond layer. Drops bonus mining XP.
     */
    ENDERMAN_EXCAVATOR(
            "enderman_excavator",
            EntityType.ENDERMAN,
            ChatColor.DARK_PURPLE + "Enderman Excavator",
            80.0D,
            12.0D,
            0.30D,
            15,
            Material.DIAMOND_PICKAXE,
            null,
            ChatColor.GRAY + "A rare excavator from the diamond depths.",
            ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "RARE MINING MOB"
    ),

    /**
     * Mini-Boss Event mob. Spawns during special mining events with scaled health.
     */
    IRON_GOLEM_GUARDIAN(
            "iron_golem_guardian",
            EntityType.IRON_GOLEM,
            ChatColor.GOLD + "Iron Golem Guardian",
            300.0D,
            20.0D,
            0.28D,
            5,
            Material.IRON_CHESTPLATE,
            Material.IRON_SWORD,
            ChatColor.GRAY + "A massive guardian awakened by mining events.",
            ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "MINI-BOSS"
    );

    private final String id;
    private final EntityType entityType;
    private final String displayName;
    private final double baseMaxHealth;
    private final double baseAttackDamage;
    private final double moveSpeed;
    private final int weight;
    private final Material helmetMaterial;
    private final Material chestplateMaterial;
    private final String description;
    private final String rarity;

    MiningMobType(
            String id,
            EntityType entityType,
            String displayName,
            double baseMaxHealth,
            double baseAttackDamage,
            double moveSpeed,
            int weight,
            Material helmetMaterial,
            Material chestplateMaterial,
            String description,
            String rarity
    ) {
        this.id = id;
        this.entityType = entityType;
        this.displayName = displayName;
        this.baseMaxHealth = baseMaxHealth;
        this.baseAttackDamage = baseAttackDamage;
        this.moveSpeed = moveSpeed;
        this.weight = weight;
        this.helmetMaterial = helmetMaterial;
        this.chestplateMaterial = chestplateMaterial;
        this.description = description;
        this.rarity = rarity;
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

    public double baseMaxHealth() {
        return baseMaxHealth;
    }

    public double baseAttackDamage() {
        return baseAttackDamage;
    }

    public double moveSpeed() {
        return moveSpeed;
    }

    public int weight() {
        return weight;
    }

    public Material helmetMaterial() {
        return helmetMaterial;
    }

    public Material chestplateMaterial() {
        return chestplateMaterial;
    }

    public String description() {
        return description;
    }

    public String rarity() {
        return rarity;
    }

    /**
     * Calculates scaled health based on mine layer (deeper = harder).
     * @param layer The mine layer (0 = surface, higher = deeper)
     * @param maxLayer The maximum layer depth
     * @return Scaled health value
     */
    public double getScaledHealth(int layer, int maxLayer) {
        if (layer <= 0 || maxLayer <= 0) {
            return baseMaxHealth;
        }
        double layerMultiplier = 1.0D + (0.15D * ((double) layer / (double) maxLayer));
        return baseMaxHealth * layerMultiplier;
    }

    /**
     * Calculates scaled damage based on mine layer (deeper = harder).
     * @param layer The mine layer (0 = surface, higher = deeper)
     * @param maxLayer The maximum layer depth
     * @return Scaled damage value
     */
    public double getScaledDamage(int layer, int maxLayer) {
        if (layer <= 0 || maxLayer <= 0) {
            return baseAttackDamage;
        }
        double layerMultiplier = 1.0D + (0.10D * ((double) layer / (double) maxLayer));
        return baseAttackDamage * layerMultiplier;
    }

    /**
     * Parses a string input into a MiningMobType.
     * @param input The string to parse
     * @return The matching MiningMobType or null if not found
     */
    public static MiningMobType parse(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String normalized = input.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        for (MiningMobType type : values()) {
            if (type.id.equals(normalized)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Checks if this mob is a mini-boss.
     * @return true if this is a mini-boss type
     */
    public boolean isMiniBoss() {
        return this == IRON_GOLEM_GUARDIAN;
    }

    /**
     * Checks if this mob is a rare spawn.
     * @return true if this is a rare or mini-boss type
     */
    public boolean isRare() {
        return this == ENDERMAN_EXCAVATOR || this == IRON_GOLEM_GUARDIAN;
    }
}
