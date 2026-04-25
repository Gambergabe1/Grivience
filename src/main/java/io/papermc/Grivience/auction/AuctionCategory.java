package io.papermc.Grivience.auction;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public enum AuctionCategory {
    WEAPONS("Weapons", Material.GOLDEN_SWORD),
    ARMOR("Armor", Material.DIAMOND_CHESTPLATE),
    ACCESSORIES("Accessories", Material.GOLD_NUGGET),
    CONSUMABLES("Consumables", Material.POTION),
    BLOCKS("Blocks", Material.GRASS_BLOCK),
    TOOLS_MISC("Tools & Misc", Material.STICK);

    private final String displayName;
    private final Material icon;

    AuctionCategory(String displayName, Material icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public static AuctionCategory fromItemStack(ItemStack item) {
        if (item == null) return TOOLS_MISC;
        Material mat = item.getType();
        String name = mat.name();

        if (name.contains("SWORD") || name.contains("BOW") || name.contains("AXE") && !name.contains("PICKAXE")) {
            return WEAPONS;
        }
        if (name.contains("HELMET") || name.contains("CHESTPLATE") || name.contains("LEGGINGS") || name.contains("BOOTS")) {
            return ARMOR;
        }
        if (name.contains("POTION") || name.contains("FOOD") || name.contains("APPLE") || name.contains("CARROT") || name.contains("POTATO") || name.contains("STEAK")) {
            return CONSUMABLES;
        }
        if (mat.isBlock()) {
            return BLOCKS;
        }
        // Accessories usually have special metadata or are specific materials in Skyblock.
        // For now, let's just use some common Skyblock accessory materials.
        if (name.contains("TALISMAN") || name.contains("RING") || name.contains("ARTIFACT") || name.contains("SKULL")) {
            return ACCESSORIES;
        }

        return TOOLS_MISC;
    }
}
