package io.papermc.Grivience.stats;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.item.CustomArmorManager;
import io.papermc.Grivience.item.CustomArmorType;
import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.item.CustomToolType;
import io.papermc.Grivience.item.CustomWeaponProfiles;
import io.papermc.Grivience.item.CustomWeaponType;
import io.papermc.Grivience.pet.PetManager;
import io.papermc.Grivience.pet.PetStatBonuses;
import io.papermc.Grivience.skills.SkyblockSkillManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Computes Skyblock-like Skyblock combat stats (health/defense/strength/crit/intelligence)
 * from level progression and equipped items.
 *
 * This is the single place that should define "what your stats are". Other systems
 * (damage calculation, defense reduction, GUI display) should read from here.
 */
public final class SkyblockCombatStatsService {
    private final GriviencePlugin plugin;
    private final SkyblockLevelManager levelManager;
    private final SkyblockStatsManager baseStatsManager;
    private final CustomItemService customItemService;
    private final CustomArmorManager customArmorManager;
    private SkyblockSkillManager skillManager;

    private double baseHealth;
    private double maxCritChancePercent;

    public SkyblockCombatStatsService(
            GriviencePlugin plugin,
            SkyblockLevelManager levelManager,
            SkyblockStatsManager baseStatsManager,
            CustomItemService customItemService,
            CustomArmorManager customArmorManager
    ) {
        this.plugin = plugin;
        this.levelManager = levelManager;
        this.baseStatsManager = baseStatsManager;
        this.customItemService = customItemService;
        this.customArmorManager = customArmorManager;
        reload();
    }

    public void setSkillManager(SkyblockSkillManager skillManager) {
        this.skillManager = skillManager;
    }

    public void reload() {
        baseHealth = clampFinite(plugin.getConfig().getDouble("skyblock-combat.base-health", 100.0D), 1.0D, 100_000.0D);
        maxCritChancePercent = clampFinite(plugin.getConfig().getDouble("skyblock-combat.max-crit-chance", 100.0D), 0.0D, 100.0D);
    }

    public SkyblockPlayerStats compute(Player player) {
        if (player == null) {
            return new SkyblockPlayerStats(baseHealth, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        double health = baseHealth;
        if (levelManager != null) {
            health += Math.max(0.0D, levelManager.getHealthBonus(player));
        }

        if (skillManager != null) {
            health += skillManager.getStatBonus(player, "Health");
        }

        double defense = baseStatsManager == null ? 0.0D : Math.max(0.0D, baseStatsManager.getDefense(player));
        double strength = baseStatsManager == null ? 0.0D : Math.max(0.0D, baseStatsManager.getStrength(player));
        double critChance = baseStatsManager == null ? 0.0D : Math.max(0.0D, baseStatsManager.getCritChance(player));
        double critDamage = baseStatsManager == null ? 0.0D : Math.max(0.0D, baseStatsManager.getCritDamage(player));
        double intelligence = baseStatsManager == null ? 0.0D : Math.max(0.0D, baseStatsManager.getIntelligence(player));
        double farmingFortune = baseStatsManager == null ? 0.0D : Math.max(0.0D, baseStatsManager.getFarmingFortune(player));

        EquipmentBonuses equipment = equipmentBonuses(player);
        health += equipment.health;
        defense += equipment.defense;
        strength += equipment.strength;
        critChance += equipment.critChancePercent;
        critDamage += equipment.critDamagePercent;
        intelligence += equipment.intelligence;
        farmingFortune += equipment.farmingFortune;

        WeaponBonuses weapon = heldWeaponBonuses(player);
        strength += weapon.strength;
        critChance += weapon.critChancePercent;
        critDamage += weapon.critDamagePercent;
        intelligence += weapon.intelligence;

        PetManager petManager = plugin.getPetManager();
        if (petManager != null) {
            PetStatBonuses pet = petManager.equippedStatBonuses(player);
            if (pet != null) {
                health += pet.health();
                defense += pet.defense();
                strength += pet.strength();
                critChance += pet.critChance();
                critDamage += pet.critDamage();
                intelligence += pet.intelligence();
            }
        }

        farmingFortune += heldToolFarmingFortune(player);

        health = Math.max(1.0D, health);
        defense = Math.max(0.0D, defense);
        strength = Math.max(0.0D, strength);
        critChance = clampFinite(critChance, 0.0D, maxCritChancePercent);
        critDamage = Math.max(0.0D, critDamage);
        intelligence = Math.max(0.0D, intelligence);
        farmingFortune = Math.max(0.0D, farmingFortune);

        return new SkyblockPlayerStats(health, defense, strength, critChance, critDamage, intelligence, farmingFortune);
    }

    private double heldToolFarmingFortune(Player player) {
        if (player == null || customItemService == null) {
            return 0.0D;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) {
            return 0.0D;
        }

        String id = customItemService.itemId(held);
        CustomToolType tool = CustomToolType.parse(id);
        if (tool == null) {
            return 0.0D;
        }

        return switch (tool) {
            case GILDED_HOE -> 40.0D;
            case NEWTONIAN_HOE -> 80.0D;
            case GAIA_SCYTHE -> 150.0D;
            default -> 0.0D;
        };
    }

    private WeaponBonuses heldWeaponBonuses(Player player) {
        if (player == null || customItemService == null) {
            return new WeaponBonuses(0.0D, 0.0D, 0.0D, 0.0D);
        }

        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (!customItemService.isCustomDungeonWeapon(weapon)) {
            return new WeaponBonuses(0.0D, 0.0D, 0.0D, 0.0D);
        }

        CustomWeaponType weaponType = CustomWeaponType.parse(customItemService.itemId(weapon));
        if (weaponType == null) {
            return new WeaponBonuses(0.0D, 0.0D, 0.0D, 0.0D);
        }

        CustomWeaponProfiles.StatProfile base = CustomWeaponProfiles.stats(weaponType);
        CustomItemService.ReforgeStats reforgeStats = customItemService.reforgeStats(customItemService.reforgeOf(weapon), weapon);

        double strength = base.strength() + reforgeStats.strengthBonus();
        double critChance = base.critChancePercent() + reforgeStats.critChanceBonus();
        double critDamage = base.critDamagePercent() + reforgeStats.critDamageBonus();
        double intelligence = base.intelligence() + reforgeStats.intelligenceBonus();

        return new WeaponBonuses(strength, critChance, critDamage, intelligence);
    }

    private EquipmentBonuses equipmentBonuses(Player player) {
        double health = 0.0D;
        double defense = 0.0D;
        double strength = 0.0D;
        double critChance = 0.0D;
        double critDamage = 0.0D;
        double intelligence = 0.0D;
        double farmingFortune = 0.0D;

        for (ItemStack piece : player.getInventory().getArmorContents()) {
            if (piece == null || piece.getType().isAir()) {
                continue;
            }

            // Dungeon armor (CustomArmorType) identified via custom item IDs.
            if (customItemService != null) {
                String itemId = customItemService.itemId(piece);
                CustomArmorType armorType = CustomArmorType.parse(itemId);
                if (armorType != null) {
                    defense += armorType.defense();
                    health += armorType.health();
                    intelligence += armorType.mana();
                    continue;
                }
            }

            // Config-driven custom armor sets (CustomArmorManager).
            if (customArmorManager != null && customArmorManager.isCustomArmor(piece)) {
                String setId = customArmorManager.getArmorSetId(piece);
                CustomArmorManager.ArmorPieceType type = customArmorManager.getArmorPieceType(piece);
                CustomArmorManager.CustomArmorSet set = setId == null ? null : customArmorManager.getArmorSet(setId);
                CustomArmorManager.ArmorPieceConfig cfg = (set == null || type == null) ? null : set.getPiece(type);
                if (cfg != null) {
                    // In Skyblock terms "armor" in config represents Defense.
                    defense += Math.max(0.0D, cfg.getArmorValue());
                    health += Math.max(0.0D, cfg.getHealthBonus());
                    intelligence += Math.max(0.0D, cfg.getManaBonus());
                    critChance += Math.max(0.0D, cfg.getCritChanceBonus());
                    critDamage += Math.max(0.0D, cfg.getCritDamageBonus());
                    farmingFortune += Math.max(0.0D, cfg.getFarmingFortuneBonus());
                }
            }
        }

        return new EquipmentBonuses(health, defense, strength, critChance, critDamage, intelligence, farmingFortune);
    }

    private double clampFinite(double value, double min, double max) {
        if (!Double.isFinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private record EquipmentBonuses(
            double health,
            double defense,
            double strength,
            double critChancePercent,
            double critDamagePercent,
            double intelligence,
            double farmingFortune
    ) {
    }

    private record WeaponBonuses(
            double strength,
            double critChancePercent,
            double critDamagePercent,
            double intelligence
    ) {
    }
}
