package io.papermc.Grivience.stats;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.accessory.AccessoryBonuses;
import io.papermc.Grivience.accessory.AccessoryManager;
import io.papermc.Grivience.item.CustomArmorManager;
import io.papermc.Grivience.item.CustomArmorType;
import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.item.CustomToolType;
import io.papermc.Grivience.item.CustomWeaponProfiles;
import io.papermc.Grivience.item.CustomWeaponType;
import io.papermc.Grivience.pet.PetManager;
import io.papermc.Grivience.pet.PetStatBonuses;
import io.papermc.Grivience.skills.SkyblockSkillManager;
import io.papermc.Grivience.util.EndZoneUtil;
import io.papermc.Grivience.util.FarmingSetBonusUtil;
import io.papermc.Grivience.wizard.WizardTowerManager;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;

/**
 * Computes Skyblock-like Skyblock combat stats (health/defense/strength/crit/intelligence)
 * from level progression and equipped items.
 *
 * This is the single place that should define "what your stats are". Other systems
 * (damage calculation, defense reduction, GUI display) should read from here.
 */
public final class SkyblockCombatStatsService {
    public static final NamespacedKey TEMP_DEFENSE_SHRED_PERCENT_KEY = new NamespacedKey("grivience", "temp_defense_shred_percent");
    public static final NamespacedKey TEMP_DEFENSE_SHRED_UNTIL_KEY = new NamespacedKey("grivience", "temp_defense_shred_until");

    private final GriviencePlugin plugin;
    private final SkyblockLevelManager levelManager;
    private final SkyblockStatsManager baseStatsManager;
    private final CustomItemService customItemService;
    private final CustomArmorManager customArmorManager;
    private final AccessoryManager accessoryManager;
    private SkyblockSkillManager skillManager;
    private io.papermc.Grivience.soul.SoulManager soulManager;

    private double baseHealth;
    private double maxCritChancePercent;
    private double regularFortuneToFarmingFortunePerLevel;

    public SkyblockCombatStatsService(
            GriviencePlugin plugin,
            SkyblockLevelManager levelManager,
            SkyblockStatsManager baseStatsManager,
            CustomItemService customItemService,
            CustomArmorManager customArmorManager,
            AccessoryManager accessoryManager
    ) {
        this.plugin = plugin;
        this.levelManager = levelManager;
        this.baseStatsManager = baseStatsManager;
        this.customItemService = customItemService;
        this.customArmorManager = customArmorManager;
        this.accessoryManager = accessoryManager;
        reload();
    }

    public void setSkillManager(SkyblockSkillManager skillManager) {
        this.skillManager = skillManager;
    }

    public void setSoulManager(io.papermc.Grivience.soul.SoulManager soulManager) {
        this.soulManager = soulManager;
    }

    public void reload() {
        baseHealth = clampFinite(plugin.getConfig().getDouble("skyblock-combat.base-health", 100.0D), 1.0D, 100_000.0D);
        maxCritChancePercent = clampFinite(plugin.getConfig().getDouble("skyblock-combat.max-crit-chance", 100.0D), 0.0D, 100.0D);
        regularFortuneToFarmingFortunePerLevel = clampFinite(
                plugin.getConfig().getDouble("skyblock-combat.regular-fortune-farming-fortune-per-level", 15.0D),
                0.0D,
                500.0D
        );
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

        // Apply Dungeon Class bonuses and scaling (Only if Dungeonized)
        if (plugin.getDungeonManager() != null && plugin.getDungeonManager().isInDungeon(player.getUniqueId())) {

            io.papermc.Grivience.dungeon.DungeonClass cls = plugin.getDungeonClassManager().getSelectedClass(player);
            if (cls != null) {
                switch (cls) {
                    case TANK -> defense *= 1.5;
                    case MAGE -> intelligence *= 1.5;
                    case BERSERK -> strength *= 1.1;
                    case ARCHER -> {
                        strength *= 1.1;
                        critDamage *= 1.1;
                    }
                }
            }
            
            // Dungeon scaling only applies to health/def/etc if the player has at least SOME dungeonized gear?
            // Actually, HS scales the PLAYER'S base stats too, but most scaling comes from the gear being dungeonized.
            // Let's keep the global scaling for now but ensure gear-specific scaling checks dungeonization.
            health *= 2.0;
            defense *= 2.0;
            strength *= 2.0;
            intelligence *= 2.0;
        }

        if (accessoryManager != null) {
            AccessoryBonuses accessoryBonuses = accessoryManager.bonuses(player);
            health += accessoryBonuses.health();
            defense += accessoryBonuses.defense();
            strength += accessoryBonuses.strength();
            critChance += accessoryBonuses.critChance();
            critDamage += accessoryBonuses.critDamage();
            intelligence += accessoryBonuses.intelligence();
            farmingFortune += accessoryBonuses.farmingFortune();
        }

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
                farmingFortune += pet.farmingFortune();

                // Implement common pet abilities
                String petId = petManager.equippedPet(player);
                if (petId != null) {
                    if (petId.equalsIgnoreCase("bee")) {
                        // Hive Mind: Intelligence per nearby bee
                        int nearbyBees = countNearbyPets(player, "bee", 15.0);
                        if (nearbyBees > 0) {
                            int level = petManager.getLevel(player, "bee");
                            double intelPerBee = 1.0 + (nearbyBees > 1 ? (level - 1) * 0.09D : 0); // approx logic
                            intelligence += (intelPerBee * nearbyBees);
                        }
                    } else if (petId.equalsIgnoreCase("wolf")) {
                        // Pack Leader: Crit Damage per nearby wolf
                        int nearbyWolves = countNearbyPets(player, "wolf", 15.0);
                        if (nearbyWolves > 0) {
                            int level = petManager.getLevel(player, "wolf");
                            double cdPerWolf = 5.0 + (level - 1) * 0.15D;
                            critDamage += (cdPerWolf * nearbyWolves);
                        }
                    } else if (petId.equalsIgnoreCase("blue_whale")) {
                        // Bulk: Increases max health by 10-20%
                        int level = petManager.getLevel(player, "blue_whale");
                        double percent = 10.0 + (level - 1) * 0.10D;
                        health *= (1.0 + (percent / 100.0));
                    } else if (petId.equalsIgnoreCase("tiger")) {
                        // Merciless Swipe: +10-30 Ferocity
                        int level = petManager.getLevel(player, "tiger");
                        // ferocity is a stat but not directly in SkyblockPlayerStats yet?
                        // Let's assume we want to apply it or at least track it.
                        // I'll add ferocity to PetStatBonuses earlier.
                    } else if (petId.equalsIgnoreCase("lion")) {
                        // King of the Jungle: +5-50 Strength
                        int level = petManager.getLevel(player, "lion");
                        strength += (5.0 + (level - 1) * 0.45D);
                    }
                }
            }
        }

        WizardTowerManager wizardTowerManager = plugin.getWizardTowerManager();
        if (wizardTowerManager != null) {
            WizardTowerManager.StatBonus blessing = wizardTowerManager.activeStatBonus(player);
            health += blessing.health();
            strength += blessing.combat();
            intelligence += blessing.intelligence();
        }

        farmingFortune += heldToolFarmingFortune(player);
        farmingFortune += regularFortuneFarmingBonus(player);

        health = Math.max(1.0D, health);
        defense = Math.max(0.0D, applyTemporaryDefenseShred(player, defense));
        strength = Math.max(0.0D, strength);
        critChance = clampFinite(critChance, 0.0D, maxCritChancePercent);
        critDamage = Math.max(0.0D, critDamage);
        intelligence = Math.max(0.0D, intelligence);
        farmingFortune = Math.max(0.0D, farmingFortune);

        return new SkyblockPlayerStats(health, defense, strength, critChance, critDamage, intelligence, farmingFortune);
    }

    private int countNearbyPets(Player player, String petId, double radius) {
        if (player == null || plugin.getPetManager() == null) return 0;
        int count = 0;
        double rSq = radius * radius;
        for (Player other : player.getWorld().getPlayers()) {
            if (other.equals(player)) continue;
            if (other.getLocation().distanceSquared(player.getLocation()) <= rSq) {
                String equipped = plugin.getPetManager().equippedPet(other);
                if (equipped != null && equipped.equalsIgnoreCase(petId)) {
                    count++;
                }
            }
        }
        return count;
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

    private double regularFortuneFarmingBonus(Player player) {
        if (player == null || regularFortuneToFarmingFortunePerLevel <= 0.0D) {
            return 0.0D;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) {
            return 0.0D;
        }

        int fortuneLevel = held.getEnchantmentLevel(Enchantment.FORTUNE);
        if (fortuneLevel <= 0) {
            return 0.0D;
        }

        return fortuneLevel * regularFortuneToFarmingFortunePerLevel;
    }

    private WeaponBonuses heldWeaponBonuses(Player player) {
        if (player == null || customItemService == null) {
            return new WeaponBonuses(0.0D, 0.0D, 0.0D, 0.0D);
        }

        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (!customItemService.isReforgable(weapon)) {
            return new WeaponBonuses(0.0D, 0.0D, 0.0D, 0.0D);
        }

        CustomWeaponType weaponType = CustomWeaponType.parse(customItemService.itemId(weapon));
        CustomWeaponProfiles.StatProfile base = (weaponType != null) 
                ? CustomWeaponProfiles.stats(weaponType) 
                : CustomWeaponProfiles.StatProfile.ZERO;

        CustomItemService.ReforgeStats reforgeStats = customItemService.reforgeStats(customItemService.reforgeOf(weapon), weapon);

        double strength = base.strength() + reforgeStats.strengthBonus();
        double critChance = base.critChancePercent() + reforgeStats.critChanceBonus();
        double critDamage = base.critDamagePercent() + reforgeStats.critDamageBonus();
        double intelligence = base.intelligence() + reforgeStats.intelligenceBonus();

        // Apply Dungeon Stars (10% per star in dungeons, only if Dungeonized)
        if (plugin.getDungeonManager() != null && plugin.getDungeonManager().isInDungeon(player.getUniqueId())) {
            if (customItemService.isDungeonized(weapon)) {
                int stars = customItemService.getDungeonStars(weapon);
                if (stars > 0) {
                    double multiplier = 1.0 + (stars * 0.1);
                    strength *= multiplier;
                    critChance *= multiplier;
                    critDamage *= multiplier;
                    intelligence *= multiplier;
                }
            }
        }

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
        Map<String, Integer> customArmorPieceCounts = new HashMap<>();

        boolean inDungeon = plugin.getDungeonManager() != null && plugin.getDungeonManager().isInDungeon(player.getUniqueId());

        for (ItemStack piece : player.getInventory().getArmorContents()) {
            if (piece == null || piece.getType().isAir()) {
                continue;
            }

            // Dungeon armor (CustomArmorType) identified via custom item IDs.
            if (customItemService != null) {
                String itemId = customItemService.itemId(piece);
                CustomArmorType armorType = CustomArmorType.parse(itemId);
                if (armorType != null) {
                    double pDefense = armorType.defense();
                    double pHealth = armorType.health();
                    double pIntelligence = armorType.mana();
                    double pStrength = 0.0;

                    if (armorType == CustomArmorType.DRAGON_SLAYER_LEGGINGS && EndZoneUtil.isEndWorld(plugin, player.getWorld())) {
                        pStrength += 25.0D;
                    }

                    if (inDungeon) {
                        if (customItemService.isDungeonized(piece)) {
                            int stars = customItemService.getDungeonStars(piece);
                            if (stars > 0) {
                                double multiplier = 1.0 + (stars * 0.1);
                                pDefense *= multiplier;
                                pHealth *= multiplier;
                                pIntelligence *= multiplier;
                                pStrength *= multiplier;
                            }
                        }
                    }

                    defense += pDefense;
                    health += pHealth;
                    intelligence += pIntelligence;
                    strength += pStrength;
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
                    customArmorPieceCounts.merge(setId.toLowerCase(java.util.Locale.ROOT), 1, Integer::sum);
                }
            }
        }

        if (customArmorPieceCounts.getOrDefault("crimson_warden", 0) >= 4) {
            health += 50.0D;
        }
        health += FarmingSetBonusUtil.gildedHarvesterFullSetHealthBonus(
                customArmorPieceCounts.getOrDefault(FarmingSetBonusUtil.GILDED_HARVESTER_SET_ID, 0)
        );
        health += FarmingSetBonusUtil.taterguardFullSetHealthBonus(
                customArmorPieceCounts.getOrDefault(FarmingSetBonusUtil.TATERGUARD_SET_ID, 0)
        );
        health += FarmingSetBonusUtil.melonMonarchFullSetHealthBonus(
                customArmorPieceCounts.getOrDefault(FarmingSetBonusUtil.MELON_MONARCH_SET_ID, 0)
        );
        farmingFortune += FarmingSetBonusUtil.rootboundGarbFullSetFarmingFortuneBonus(
                customArmorPieceCounts.getOrDefault(FarmingSetBonusUtil.ROOTBOUND_GARB_SET_ID, 0)
        );
        farmingFortune += FarmingSetBonusUtil.melonMonarchFullSetFarmingFortuneBonus(
                customArmorPieceCounts.getOrDefault(FarmingSetBonusUtil.MELON_MONARCH_SET_ID, 0)
        );
        intelligence += FarmingSetBonusUtil.wartwovenRegaliaFullSetIntelligenceBonus(
                customArmorPieceCounts.getOrDefault(FarmingSetBonusUtil.WARTWOVEN_REGALIA_SET_ID, 0)
        );

        return new EquipmentBonuses(health, defense, strength, critChance, critDamage, intelligence, farmingFortune);
    }

    private double applyTemporaryDefenseShred(Player player, double defense) {
        if (player == null || defense <= 0.0D) {
            return Math.max(0.0D, defense);
        }

        PersistentDataContainer data = player.getPersistentDataContainer();
        long until = data.getOrDefault(TEMP_DEFENSE_SHRED_UNTIL_KEY, PersistentDataType.LONG, 0L);
        if (until <= 0L) {
            return Math.max(0.0D, defense);
        }
        if (System.currentTimeMillis() >= until) {
            data.remove(TEMP_DEFENSE_SHRED_UNTIL_KEY);
            data.remove(TEMP_DEFENSE_SHRED_PERCENT_KEY);
            return Math.max(0.0D, defense);
        }

        double percent = data.getOrDefault(TEMP_DEFENSE_SHRED_PERCENT_KEY, PersistentDataType.DOUBLE, 0.0D);
        percent = clampFinite(percent, 0.0D, 0.95D);
        return Math.max(0.0D, defense * (1.0D - percent));
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
