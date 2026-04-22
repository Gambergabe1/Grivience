package io.papermc.Grivience.collections;

import org.bukkit.Material;

import java.util.Locale;

/**
 * Normalizes item ids into the collection ids tracked by the Skyblock collection system.
 */
public final class CollectionItemIdUtil {
    private CollectionItemIdUtil() {
    }

    public static String normalizeTrackedItemId(String raw) {
        if (raw == null) {
            return "";
        }

        String normalized = raw.trim()
                .toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');

        return switch (normalized) {
            case "carrot_item" -> "carrot";
            case "potato_item" -> "potato";
            case "melon_slice" -> "melon";
            case "nether_stalk" -> "nether_wart";
            case "enchanted_hay_block" -> "enchanted_hay_bale";
            case "enchanted_nether_stalk" -> "enchanted_nether_wart";
            default -> normalized;
        };
    }

    public static String trackedItemIdForMaterial(Material material) {
        if (material == null) {
            return "";
        }

        return switch (material) {
            case RAW_IRON, IRON_ORE, DEEPSLATE_IRON_ORE -> "iron_ingot";
            case RAW_GOLD, GOLD_ORE, DEEPSLATE_GOLD_ORE, NETHER_GOLD_ORE -> "gold_ingot";
            case COAL_ORE, DEEPSLATE_COAL_ORE -> "coal";
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE -> "diamond";
            case EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> "emerald";
            case LAPIS_ORE, DEEPSLATE_LAPIS_ORE -> "lapis_lazuli";
            case REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE -> "redstone";
            case NETHER_QUARTZ_ORE -> "quartz";
            default -> normalizeTrackedItemId(material.name());
        };
    }
}
