package io.papermc.Grivience.accessory;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.item.ItemRarity;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import io.papermc.Grivience.storage.StorageManager;
import io.papermc.Grivience.storage.StorageProfile;
import io.papermc.Grivience.storage.StorageType;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Computes accessory bag bonuses and validates accessory items.
 * Includes Hypixel-style Magical Power system and enrichment support.
 */
public final class AccessoryManager {
    private static final int MAX_ECHO_STACKS = 6;
    private static final double ECHO_DEFENSE_PER_STACK = 2.0D;
    private static final double ECHO_INTELLIGENCE_PER_STACK = 3.0D;

    private static final double RESONANCE_HEALTH = 40.0D;
    private static final double RESONANCE_DEFENSE = 10.0D;
    private static final double RESONANCE_STRENGTH = 12.0D;
    private static final double RESONANCE_INTELLIGENCE = 25.0D;
    private static final double RESONANCE_FARMING_FORTUNE = 30.0D;

    private final GriviencePlugin plugin;
    private final CustomItemService customItemService;
    private final NamespacedKey enrichmentKey;

    public AccessoryManager(GriviencePlugin plugin, CustomItemService customItemService) {
        this.plugin = plugin;
        this.customItemService = customItemService;
        this.enrichmentKey = new NamespacedKey(plugin, "accessory_enrichment");
    }

    public AccessoryBonuses bonuses(Player player) {
        if (player == null) {
            return AccessoryBonuses.NONE;
        }

        StorageManager storageManager = plugin.getStorageManager();
        if (storageManager == null) {
            return AccessoryBonuses.NONE;
        }

        StorageProfile bag = storageManager.getStorage(player, StorageType.ACCESSORY_BAG);
        if (bag == null || bag.getContents().isEmpty()) {
            return AccessoryBonuses.NONE;
        }

        // Track best accessory per family (only highest tier counts)
        Map<String, AccessoryData> bestByFamily = new HashMap<>();
        int totalAccessoriesInBag = 0;
        int duplicateCount = 0;

        for (ItemStack item : bag.getContents().values()) {
            AccessoryType type = accessoryType(item);
            if (type == null) {
                continue;
            }

            totalAccessoriesInBag++;
            AccessoryEnrichment enrichment = getEnrichment(item);
            ItemRarity effectiveRarity = getEffectiveRarity(type.rarity(), enrichment);

            AccessoryData existing = bestByFamily.get(type.family());
            if (existing == null) {
                bestByFamily.put(type.family(), new AccessoryData(type, enrichment, effectiveRarity));
            } else {
                // Keep highest tier; if tied, keep highest enrichment
                if (type.tier() > existing.type().tier() ||
                        (type.tier() == existing.type().tier() && enrichment.level() > existing.enrichment().level())) {
                    bestByFamily.put(type.family(), new AccessoryData(type, enrichment, effectiveRarity));
                }
                duplicateCount++;
            }
        }

        if (bestByFamily.isEmpty()) {
            return AccessoryBonuses.NONE;
        }

        // Calculate base stats from accessories
        double health = 0.0D;
        double defense = 0.0D;
        double strength = 0.0D;
        double critChance = 0.0D;
        double critDamage = 0.0D;
        double intelligence = 0.0D;
        double farmingFortune = 0.0D;
        int totalMagicalPower = 0;
        EnumSet<AccessoryType.Category> categories = EnumSet.noneOf(AccessoryType.Category.class);

        for (AccessoryData data : bestByFamily.values()) {
            AccessoryType type = data.type();
            AccessoryEnrichment enrichment = data.enrichment();
            double statMultiplier = enrichment.statMultiplier();

            // Apply enrichment multiplier to base stats
            health += type.health() * statMultiplier;
            defense += type.defense() * statMultiplier;
            strength += type.strength() * statMultiplier;
            critChance += type.critChance() * statMultiplier;
            critDamage += type.critDamage() * statMultiplier;
            intelligence += type.intelligence() * statMultiplier;
            farmingFortune += type.farmingFortune() * statMultiplier;

            // Calculate magical power
            totalMagicalPower += AccessoryPower.calculatePower(data.effectiveRarity(), enrichment);
            categories.add(type.category());
        }

        // Apply Magical Power bonuses (flat stats)
        AccessoryPower.MagicalPowerBonuses mpBonuses = AccessoryPower.calculatePowerBonuses(totalMagicalPower);
        health += mpBonuses.health();
        defense += mpBonuses.defense();
        strength += mpBonuses.strength();
        intelligence += mpBonuses.intelligence();

        // Apply Magical Power stat multiplier to all accessory-derived stats
        double mpMultiplier = AccessoryPower.statMultiplierFromPower(totalMagicalPower);
        health *= mpMultiplier;
        defense *= mpMultiplier;
        strength *= mpMultiplier;
        critChance *= mpMultiplier;
        critDamage *= mpMultiplier;
        intelligence *= mpMultiplier;
        farmingFortune *= mpMultiplier;

        // Apply selected Power stats
        SkyBlockProfile profile = plugin.getProfileManager() != null ? plugin.getProfileManager().getSelectedProfile(player) : null;
        if (profile != null) {
            AccessoryPowerType power = profile.getSelectedAccessoryPower();
            Map<String, Double> powerStats = AccessoryPower.calculatePowerTypeStats(power, totalMagicalPower);
            
            health += powerStats.getOrDefault("Health", 0.0);
            defense += powerStats.getOrDefault("Defense", 0.0);
            strength += powerStats.getOrDefault("Strength", 0.0);
            critChance += powerStats.getOrDefault("Crit Chance", 0.0);
            critDamage += powerStats.getOrDefault("Crit Damage", 0.0);
            intelligence += powerStats.getOrDefault("Intelligence", 0.0);
            farmingFortune += powerStats.getOrDefault("Farming Fortune", 0.0);
        }

        // Echo bonus (from duplicates)
        int echoStacks = Math.max(0, Math.min(MAX_ECHO_STACKS, duplicateCount));
        if (echoStacks > 0) {
            defense += echoStacks * ECHO_DEFENSE_PER_STACK;
            intelligence += echoStacks * ECHO_INTELLIGENCE_PER_STACK;
        }

        // Resonance bonus (requires all 3 categories)
        boolean resonanceActive = categories.contains(AccessoryType.Category.COMBAT)
                && categories.contains(AccessoryType.Category.WISDOM)
                && categories.contains(AccessoryType.Category.HARVEST);
        if (resonanceActive) {
            health += RESONANCE_HEALTH;
            defense += RESONANCE_DEFENSE;
            strength += RESONANCE_STRENGTH;
            intelligence += RESONANCE_INTELLIGENCE;
            farmingFortune += RESONANCE_FARMING_FORTUNE;
        }

        return new AccessoryBonuses(
                health,
                defense,
                strength,
                critChance,
                critDamage,
                intelligence,
                farmingFortune,
                totalAccessoriesInBag,
                bestByFamily.size(),
                totalMagicalPower,
                echoStacks,
                resonanceActive
        );
    }

    public boolean isAccessory(ItemStack item) {
        return accessoryType(item) != null;
    }

    public AccessoryType accessoryType(ItemStack item) {
        if (item == null || item.getType().isAir() || customItemService == null) {
            return null;
        }
        return AccessoryType.parse(customItemService.itemId(item));
    }

    public AccessoryEnrichment getEnrichment(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return AccessoryEnrichment.NONE;
        }
        String stored = item.getItemMeta().getPersistentDataContainer().get(enrichmentKey, PersistentDataType.STRING);
        return AccessoryEnrichment.parse(stored);
    }

    public void setEnrichment(ItemStack item, AccessoryEnrichment enrichment) {
        if (item == null || !item.hasItemMeta() || enrichment == null) {
            return;
        }
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(enrichmentKey, PersistentDataType.STRING, enrichment.name());
        item.setItemMeta(meta);
    }

    private ItemRarity getEffectiveRarity(ItemRarity baseRarity, AccessoryEnrichment enrichment) {
        if (baseRarity == null || enrichment == null || enrichment.rarityBoost() == 0) {
            return baseRarity;
        }

        int ordinal = baseRarity.ordinal() + enrichment.rarityBoost();
        ItemRarity[] values = ItemRarity.values();
        if (ordinal >= values.length) {
            return values[values.length - 1]; // Cap at highest rarity
        }
        return values[ordinal];
    }

    private record AccessoryData(AccessoryType type, AccessoryEnrichment enrichment, ItemRarity effectiveRarity) {
    }
}
