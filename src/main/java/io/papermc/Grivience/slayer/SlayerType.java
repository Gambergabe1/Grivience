package io.papermc.Grivience.slayer;

import org.bukkit.entity.EntityType;

public enum SlayerType {
    ZOMBIE("Revenant Horror", EntityType.ZOMBIE, 100),
    SPIDER("Tarantula Broodfather", EntityType.SPIDER, 100),
    WOLF("Sven Packmaster", EntityType.WOLF, 100);

    private final String bossName;
    private final EntityType targetType;
    private final int requiredXp;

    SlayerType(String bossName, EntityType targetType, int requiredXp) {
        this.bossName = bossName;
        this.targetType = targetType;
        this.requiredXp = requiredXp;
    }

    public String getBossName() {
        return bossName;
    }

    public EntityType getTargetType() {
        return targetType;
    }

    public int getRequiredXp() {
        return requiredXp;
    }
}