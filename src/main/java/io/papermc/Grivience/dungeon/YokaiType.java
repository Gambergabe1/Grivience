package io.papermc.Grivience.dungeon;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public enum YokaiType {
    ONI_BRUTE("Oni Brute", EntityType.VINDICATOR, NamedTextColor.DARK_RED, 1.55D, 1.45D, 0),
    TENGU_SKIRMISHER("Tengu Skirmisher", EntityType.STRAY, NamedTextColor.AQUA, 1.2D, 1.25D, 1),
    KAPPA_RAIDER("Kappa Raider", EntityType.DROWNED, NamedTextColor.GREEN, 1.3D, 1.15D, 0),
    ONRYO_WRAITH("Onryo Wraith", EntityType.HUSK, NamedTextColor.GRAY, 1.15D, 1.3D, 1),
    JOROGUMO_WEAVER("Jorogumo Weaver", EntityType.SPIDER, NamedTextColor.DARK_GREEN, 1.05D, 1.2D, 2),
    KITSUNE_TRICKSTER("Kitsune Trickster", EntityType.PILLAGER, NamedTextColor.GOLD, 1.1D, 1.15D, 1),
    GASHADOKURO_SENTINEL("Gashadokuro Sentinel", EntityType.WITHER_SKELETON, NamedTextColor.WHITE, 1.9D, 1.55D, 0);

    private final String displayName;
    private final EntityType entityType;
    private final NamedTextColor nameColor;
    private final double healthScale;
    private final double damageScale;
    private final int speedAmplifier;

    YokaiType(
            String displayName,
            EntityType entityType,
            NamedTextColor nameColor,
            double healthScale,
            double damageScale,
            int speedAmplifier
    ) {
        this.displayName = displayName;
        this.entityType = entityType;
        this.nameColor = nameColor;
        this.healthScale = healthScale;
        this.damageScale = damageScale;
        this.speedAmplifier = speedAmplifier;
    }

    public String displayName() {
        return displayName;
    }

    public EntityType entityType() {
        return entityType;
    }

    public NamedTextColor nameColor() {
        return nameColor;
    }

    public double healthScale() {
        return healthScale;
    }

    public double damageScale() {
        return damageScale;
    }

    public int speedAmplifier() {
        return speedAmplifier;
    }

    public void applyLoadout(LivingEntity living, int damageTier) {
        EntityEquipment equipment = living.getEquipment();
        if (equipment == null) {
            return;
        }

        equipment.clear();
        equipment.setItemInMainHandDropChance(0.0F);
        equipment.setHelmetDropChance(0.0F);
        equipment.setChestplateDropChance(0.0F);
        equipment.setLeggingsDropChance(0.0F);
        equipment.setBootsDropChance(0.0F);

        switch (this) {
            case ONI_BRUTE -> {
                ItemStack club = new ItemStack(Material.IRON_AXE);
                club.addUnsafeEnchantment(Enchantment.SHARPNESS, Math.max(1, 1 + (damageTier / 2)));
                equipment.setItemInMainHand(club);
                equipment.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
            }
            case TENGU_SKIRMISHER -> {
                ItemStack bow = new ItemStack(Material.BOW);
                bow.addUnsafeEnchantment(Enchantment.POWER, Math.max(1, 1 + (damageTier / 2)));
                bow.addUnsafeEnchantment(Enchantment.PUNCH, 1);
                equipment.setItemInMainHand(bow);
                equipment.setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
            }
            case KAPPA_RAIDER -> {
                ItemStack spear = new ItemStack(Material.TRIDENT);
                spear.addUnsafeEnchantment(Enchantment.IMPALING, Math.max(1, 1 + (damageTier / 3)));
                equipment.setItemInMainHand(spear);
            }
            case ONRYO_WRAITH -> {
                ItemStack blade = new ItemStack(Material.IRON_SWORD);
                blade.addUnsafeEnchantment(Enchantment.SHARPNESS, Math.max(1, 1 + (damageTier / 2)));
                equipment.setItemInMainHand(blade);
            }
            case JOROGUMO_WEAVER -> {
                // Spiders do not use equipment.
            }
            case KITSUNE_TRICKSTER -> {
                ItemStack crossbow = new ItemStack(Material.CROSSBOW);
                crossbow.addUnsafeEnchantment(Enchantment.QUICK_CHARGE, Math.max(1, 1 + (damageTier / 3)));
                equipment.setItemInMainHand(crossbow);
                equipment.setHelmet(new ItemStack(Material.GOLDEN_HELMET));
            }
            case GASHADOKURO_SENTINEL -> {
                ItemStack ancientBlade = new ItemStack(Material.NETHERITE_SWORD);
                ancientBlade.addUnsafeEnchantment(Enchantment.SHARPNESS, Math.max(2, 2 + (damageTier / 2)));
                equipment.setItemInMainHand(ancientBlade);
                equipment.setHelmet(new ItemStack(Material.NETHERITE_HELMET));
                equipment.setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
            }
        }
    }

    public void applySpecialEffects(LivingEntity living, int damageTier) {
        if (speedAmplifier > 0) {
            living.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED,
                    Integer.MAX_VALUE,
                    Math.min(3, speedAmplifier + (damageTier / 4)),
                    false,
                    false
            ));
        }

        switch (this) {
            case ONI_BRUTE -> living.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0, false, false));
            case TENGU_SKIRMISHER -> living.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 1, false, false));
            case KAPPA_RAIDER -> living.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, Integer.MAX_VALUE, 0, false, false));
            case ONRYO_WRAITH -> living.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 200 + (damageTier * 40), 0, false, false));
            case JOROGUMO_WEAVER -> living.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 2, false, false));
            case KITSUNE_TRICKSTER -> {
                living.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
                living.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 0, false, false));
            }
            case GASHADOKURO_SENTINEL -> {
                living.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1, false, false));
                living.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1, false, false));
            }
        }

        if (living instanceof Mob mob) {
            mob.setCanPickupItems(false);
        }
    }

    public static List<YokaiType> defaultPool() {
        return List.of(
                ONI_BRUTE,
                TENGU_SKIRMISHER,
                KAPPA_RAIDER,
                ONRYO_WRAITH,
                JOROGUMO_WEAVER,
                KITSUNE_TRICKSTER
        );
    }

    public static List<YokaiType> strongerPool() {
        List<YokaiType> pool = new ArrayList<>(defaultPool());
        pool.add(GASHADOKURO_SENTINEL);
        return pool;
    }

    public static YokaiType parse(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String normalized = input.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        for (YokaiType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
