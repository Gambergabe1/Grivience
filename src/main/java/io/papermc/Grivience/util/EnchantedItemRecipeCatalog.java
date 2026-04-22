package io.papermc.Grivience.util;

import io.papermc.Grivience.item.EnchantedFarmItemType;
import org.bukkit.Material;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Shared catalog of canonical enchanted-item compression recipes.
 */
public final class EnchantedItemRecipeCatalog {
    private static final Map<String, String> CANONICAL_INPUTS = buildCanonicalInputs();

    private EnchantedItemRecipeCatalog() {
    }

    public static Map<String, String> canonicalInputs() {
        return CANONICAL_INPUTS;
    }

    public static String inputFor(String outputId) {
        return CANONICAL_INPUTS.get(normalizeId(outputId));
    }

    public static String normalizeId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "carrot" -> "carrot_item";
            case "potato" -> "potato_item";
            case "enchanted_hay_bale" -> "enchanted_hay_block";
            case "enchanted_nether_wart" -> "enchanted_nether_stalk";
            default -> normalized;
        };
    }

    private static Map<String, String> buildCanonicalInputs() {
        Map<String, String> map = new LinkedHashMap<>();

        for (EnchantedFarmItemType type : EnchantedFarmItemType.values()) {
            String outputId = normalizeId(type.id());
            String inputId = type.baseMaterial() != null
                    ? ingredientIdFromFarmMaterial(type.baseMaterial())
                    : normalizeId(type.baseItemId());
            addCanonical(map, outputId, inputId);
        }

        addCanonical(map, "enchanted_cobblestone", "cobblestone");
        addCanonical(map, "enchanted_coal", "coal");
        addCanonical(map, "enchanted_coal_block", "enchanted_coal");
        addCanonical(map, "enchanted_iron", "iron_ingot");
        addCanonical(map, "enchanted_iron_block", "enchanted_iron");
        addCanonical(map, "enchanted_gold", "gold_ingot");
        addCanonical(map, "enchanted_gold_block", "enchanted_gold");
        addCanonical(map, "enchanted_redstone", "redstone");
        addCanonical(map, "enchanted_redstone_block", "enchanted_redstone");
        addCanonical(map, "enchanted_lapis_lazuli", "lapis_lazuli");
        addCanonical(map, "enchanted_lapis_lazuli_block", "enchanted_lapis_lazuli");
        addCanonical(map, "enchanted_diamond", "diamond");
        addCanonical(map, "enchanted_diamond_block", "enchanted_diamond");
        addCanonical(map, "enchanted_emerald", "emerald");
        addCanonical(map, "enchanted_emerald_block", "enchanted_emerald");
        addCanonical(map, "enchanted_mycelium", "mycelium");
        addCanonical(map, "enchanted_golden_carrot", "golden_carrot");
        addCanonical(map, "enchanted_sapphire", "sapphire");

        return Map.copyOf(map);
    }

    private static void addCanonical(Map<String, String> map, String outputId, String inputId) {
        String normalizedOutput = normalizeId(outputId);
        String normalizedInput = normalizeId(inputId);
        if (normalizedOutput == null || normalizedInput == null) {
            return;
        }
        map.put(normalizedOutput, normalizedInput);
    }

    private static String ingredientIdFromFarmMaterial(Material material) {
        if (material == null) {
            return null;
        }
        return switch (material) {
            case CARROT -> "carrot_item";
            case POTATO -> "potato_item";
            case NETHER_WART -> "nether_stalk";
            default -> material.getKey().getKey();
        };
    }
}
