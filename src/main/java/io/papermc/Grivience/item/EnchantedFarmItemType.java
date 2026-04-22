package io.papermc.Grivience.item;

import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.Locale;

/**
 * Ascent Skyblock-style enchanted farming items.
 */
public enum EnchantedFarmItemType {
    ENCHANTED_WHEAT("enchanted_wheat", ChatColor.GOLD + "Enchanted Wheat", Material.WHEAT, null),
    ENCHANTED_SEEDS("enchanted_seeds", ChatColor.GREEN + "Enchanted Seeds", Material.WHEAT_SEEDS, null),
    ENCHANTED_CARROT("enchanted_carrot", ChatColor.GOLD + "Enchanted Carrot", Material.CARROT, null),
    ENCHANTED_POTATO("enchanted_potato", ChatColor.GOLD + "Enchanted Potato", Material.POTATO, null),
    ENCHANTED_SUGAR("enchanted_sugar", ChatColor.AQUA + "Enchanted Sugar", Material.SUGAR_CANE, null),
    ENCHANTED_MELON("enchanted_melon", ChatColor.GREEN + "Enchanted Melon", Material.MELON_SLICE, null),
    ENCHANTED_PUMPKIN("enchanted_pumpkin", ChatColor.GOLD + "Enchanted Pumpkin", Material.PUMPKIN, null),
    ENCHANTED_CACTUS("enchanted_cactus", ChatColor.DARK_GREEN + "Enchanted Cactus", Material.CACTUS, null),
    ENCHANTED_COCOA("enchanted_cocoa", ChatColor.GOLD + "Enchanted Cocoa Bean", Material.COCOA_BEANS, null),
    ENCHANTED_NETHER_WART("enchanted_nether_wart", ChatColor.DARK_RED + "Enchanted Nether Wart", Material.NETHER_WART, null),
    ENCHANTED_RED_MUSHROOM("enchanted_red_mushroom", ChatColor.RED + "Enchanted Red Mushroom", Material.RED_MUSHROOM, null),
    ENCHANTED_BROWN_MUSHROOM("enchanted_brown_mushroom", ChatColor.GOLD + "Enchanted Brown Mushroom", Material.BROWN_MUSHROOM, null),

    ENCHANTED_HAY_BALE("enchanted_hay_bale", ChatColor.GOLD + "Enchanted Hay Bale", null, "enchanted_wheat"),
    ENCHANTED_BAKED_POTATO("enchanted_baked_potato", ChatColor.GOLD + "Enchanted Baked Potato", null, "enchanted_potato"),
    ENCHANTED_SUGAR_CANE("enchanted_sugar_cane", ChatColor.AQUA + "Enchanted Sugar Cane", null, "enchanted_sugar"),
    ENCHANTED_GLISTERING_MELON("enchanted_glistering_melon", ChatColor.GOLD + "Enchanted Glistering Melon", null, "enchanted_melon"),
    ENCHANTED_CACTUS_GREEN("enchanted_cactus_green", ChatColor.DARK_GREEN + "Enchanted Cactus Green", null, "enchanted_cactus");

    private final String id;
    private final String displayName;
    private final Material baseMaterial;
    private final String baseItemId;

    EnchantedFarmItemType(String id, String displayName, Material baseMaterial, String baseItemId) {
        this.id = id;
        this.displayName = displayName;
        this.baseMaterial = baseMaterial;
        this.baseItemId = baseItemId;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public Material baseMaterial() {
        return baseMaterial;
    }

    public String baseItemId() {
        return baseItemId;
    }

    public boolean isTierTwo() {
        return baseMaterial == null && baseItemId != null;
    }

    public static EnchantedFarmItemType parse(String input) {
        if (input == null || input.isBlank()) return null;
        String normalized = input.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        normalized = switch (normalized) {
            case "enchanted_hay_block" -> "enchanted_hay_bale";
            case "enchanted_nether_stalk" -> "enchanted_nether_wart";
            default -> normalized;
        };
        String enumName = normalized.toUpperCase(Locale.ROOT);
        for (EnchantedFarmItemType type : values()) {
            if (type.name().equals(enumName) || type.id.equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        return null;
    }
}
