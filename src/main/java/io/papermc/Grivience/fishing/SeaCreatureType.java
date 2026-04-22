package io.papermc.Grivience.fishing;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public enum SeaCreatureType {
    SQUID(EntityType.SQUID, ChatColor.AQUA + "Squid", 100.0, List.of(new ItemStack(Material.LILY_PAD, 2), new ItemStack(Material.INK_SAC, 4))),
    SEA_WALKER(EntityType.DROWNED, ChatColor.BLUE + "Sea Walker", 300.0, List.of(new ItemStack(Material.ROTTEN_FLESH, 3), new ItemStack(Material.PRISMARINE_SHARD, 1))),
    SEA_GUARDIAN(EntityType.GUARDIAN, ChatColor.DARK_PURPLE + "Sea Guardian", 1000.0, List.of(new ItemStack(Material.PRISMARINE_CRYSTALS, 2), new ItemStack(Material.PRISMARINE_SHARD, 3))),
    EMPEROR_OF_THE_DEEP(EntityType.ELDER_GUARDIAN, ChatColor.GOLD + "Emperor of the Deep", 5000.0, List.of(new ItemStack(Material.SPONGE, 1), new ItemStack(Material.LILY_PAD, 5), new ItemStack(Material.PRISMARINE_CRYSTALS, 5)));

    private final EntityType entityType;
    private final String displayName;
    private final double maxHealth;
    private final List<ItemStack> drops;

    SeaCreatureType(EntityType entityType, String displayName, double maxHealth, List<ItemStack> drops) {
        this.entityType = entityType;
        this.displayName = displayName;
        this.maxHealth = maxHealth;
        this.drops = drops;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public List<ItemStack> getDrops() {
        return drops;
    }
}
