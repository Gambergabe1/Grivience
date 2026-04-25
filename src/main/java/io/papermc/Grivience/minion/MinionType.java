package io.papermc.Grivience.minion;

import org.bukkit.Material;

import java.util.List;
import java.util.Locale;

public enum MinionType {
    COBBLESTONE(
            "cobblestone",
            "Cobblestone",
            Material.COBBLESTONE,
            Material.COBBLESTONE,
            Material.WOODEN_PICKAXE,
            List.of(14D, 14D, 12D, 12D, 10D, 10D, 9D, 9D, 8D, 8D, 7D, 6D),
            List.of(64, 192, 192, 384, 384, 576, 576, 768, 768, 960, 960, 960),
            List.of(
                    recipe(1, "cobblestone", 10),
                    recipe(2, "cobblestone", 20),
                    recipe(3, "cobblestone", 40),
                    recipe(4, "cobblestone", 64),
                    recipe(5, "enchanted_cobblestone", 1),
                    recipe(6, "enchanted_cobblestone", 2),
                    recipe(7, "enchanted_cobblestone", 4),
                    recipe(8, "enchanted_cobblestone", 8),
                    recipe(9, "enchanted_cobblestone", 16),
                    recipe(10, "enchanted_cobblestone", 32),
                    recipe(11, "enchanted_cobblestone", 64)
            )
    ),
    COAL(
            "coal",
            "Coal",
            Material.COAL_ORE,
            Material.COAL,
            Material.WOODEN_PICKAXE,
            List.of(15D, 15D, 13D, 13D, 12D, 12D, 10D, 10D, 9D, 9D, 7D, 6D),
            List.of(64, 192, 192, 384, 384, 576, 576, 768, 768, 960, 960, 960),
            List.of(
                    recipe(1, "coal", 10),
                    recipe(2, "coal", 20),
                    recipe(3, "coal", 40),
                    recipe(4, "coal", 64),
                    recipe(5, "enchanted_coal", 1),
                    recipe(6, "enchanted_coal", 3),
                    recipe(7, "enchanted_coal", 8),
                    recipe(8, "enchanted_coal", 16),
                    recipe(9, "enchanted_coal", 32),
                    recipe(10, "enchanted_coal", 64),
                    recipe(11, "enchanted_coal_block", 1)
            )
    ),
    IRON(
            "iron",
            "Iron",
            Material.IRON_ORE,
            Material.IRON_INGOT,
            Material.WOODEN_PICKAXE,
            List.of(17D, 17D, 15D, 15D, 14D, 14D, 12D, 12D, 10D, 10D, 8D, 7D),
            List.of(64, 192, 192, 384, 384, 576, 576, 768, 768, 960, 960, 960),
            List.of(
                    recipe(1, "iron_ingot", 10),
                    recipe(2, "iron_ingot", 20),
                    recipe(3, "iron_ingot", 40),
                    recipe(4, "iron_ingot", 64),
                    recipe(5, "enchanted_iron", 1),
                    recipe(6, "enchanted_iron", 3),
                    recipe(7, "enchanted_iron", 8),
                    recipe(8, "enchanted_iron", 16),
                    recipe(9, "enchanted_iron", 32),
                    recipe(10, "enchanted_iron", 64),
                    recipe(11, "enchanted_iron_block", 1)
            )
    ),
    GOLD(
            "gold",
            "Gold",
            Material.GOLD_ORE,
            Material.GOLD_INGOT,
            Material.WOODEN_PICKAXE,
            List.of(22D, 22D, 20D, 20D, 18D, 18D, 16D, 16D, 14D, 14D, 11D, 9D),
            List.of(64, 192, 192, 384, 384, 576, 576, 768, 768, 960, 960, 960),
            List.of(
                    recipe(1, "gold_ingot", 10),
                    recipe(2, "gold_ingot", 20),
                    recipe(3, "gold_ingot", 40),
                    recipe(4, "gold_ingot", 64),
                    recipe(5, "enchanted_gold", 1),
                    recipe(6, "enchanted_gold", 3),
                    recipe(7, "enchanted_gold", 8),
                    recipe(8, "enchanted_gold", 16),
                    recipe(9, "enchanted_gold", 32),
                    recipe(10, "enchanted_gold", 64),
                    recipe(11, "enchanted_gold_block", 1)
            )
    ),
    REDSTONE(
            "redstone",
            "Redstone",
            Material.REDSTONE_ORE,
            Material.REDSTONE,
            Material.WOODEN_PICKAXE,
            List.of(29D, 29D, 27D, 27D, 25D, 25D, 23D, 23D, 21D, 21D, 18D, 16D),
            List.of(64, 192, 192, 384, 384, 576, 576, 768, 768, 960, 960, 960),
            List.of(
                    recipe(1, "redstone", 16),
                    recipe(2, "redstone", 32),
                    recipe(3, "redstone", 64),
                    recipe(4, "enchanted_redstone", 1),
                    recipe(5, "enchanted_redstone", 3),
                    recipe(6, "enchanted_redstone", 8),
                    recipe(7, "enchanted_redstone", 16),
                    recipe(8, "enchanted_redstone", 32),
                    recipe(9, "enchanted_redstone", 64),
                    recipe(10, "enchanted_redstone_block", 1),
                    recipe(11, "enchanted_redstone_block", 2)
            )
    ),
    LAPIS(
            "lapis",
            "Lapis",
            Material.LAPIS_ORE,
            Material.LAPIS_LAZULI,
            Material.WOODEN_PICKAXE,
            List.of(29D, 29D, 27D, 27D, 25D, 25D, 23D, 23D, 21D, 21D, 18D, 16D),
            List.of(64, 192, 192, 384, 384, 576, 576, 768, 768, 960, 960, 960),
            List.of(
                    recipe(1, "lapis_lazuli", 32),
                    recipe(2, "lapis_lazuli", 64),
                    recipe(3, "enchanted_lapis_lazuli", 1),
                    recipe(4, "enchanted_lapis_lazuli", 3),
                    recipe(5, "enchanted_lapis_lazuli", 8),
                    recipe(6, "enchanted_lapis_lazuli", 16),
                    recipe(7, "enchanted_lapis_lazuli", 32),
                    recipe(8, "enchanted_lapis_lazuli", 64),
                    recipe(9, "enchanted_lapis_lazuli_block", 1),
                    recipe(10, "enchanted_lapis_lazuli_block", 2),
                    recipe(11, "enchanted_lapis_lazuli_block", 4)
            )
    ),
    DIAMOND(
            "diamond",
            "Diamond",
            Material.DIAMOND_ORE,
            Material.DIAMOND,
            Material.WOODEN_PICKAXE,
            List.of(29D, 29D, 27D, 27D, 25D, 25D, 22D, 22D, 19D, 19D, 15D, 12D),
            List.of(64, 192, 192, 384, 384, 576, 576, 768, 768, 960, 960, 960),
            List.of(
                    recipe(1, "diamond", 10),
                    recipe(2, "diamond", 20),
                    recipe(3, "diamond", 40),
                    recipe(4, "diamond", 64),
                    recipe(5, "enchanted_diamond", 1),
                    recipe(6, "enchanted_diamond", 3),
                    recipe(7, "enchanted_diamond", 8),
                    recipe(8, "enchanted_diamond", 16),
                    recipe(9, "enchanted_diamond", 32),
                    recipe(10, "enchanted_diamond", 64),
                    recipe(11, "enchanted_diamond_block", 1)
            )
    ),
    EMERALD(
            "emerald",
            "Emerald",
            Material.EMERALD_ORE,
            Material.EMERALD,
            Material.WOODEN_PICKAXE,
            List.of(28D, 28D, 26D, 26D, 24D, 24D, 21D, 21D, 18D, 18D, 14D, 12D),
            List.of(64, 192, 192, 384, 384, 576, 576, 768, 768, 960, 960, 960),
            List.of(
                    recipe(1, "emerald", 10),
                    recipe(2, "emerald", 20),
                    recipe(3, "emerald", 40),
                    recipe(4, "emerald", 64),
                    recipe(5, "enchanted_emerald", 1),
                    recipe(6, "enchanted_emerald", 3),
                    recipe(7, "enchanted_emerald", 8),
                    recipe(8, "enchanted_emerald", 16),
                    recipe(9, "enchanted_emerald", 32),
                    recipe(10, "enchanted_emerald", 64),
                    recipe(11, "enchanted_emerald_block", 1)
            )
    ),
    WHEAT(
            "wheat",
            "Wheat",
            Material.WHEAT,
            Material.WHEAT,
            Material.WOODEN_HOE,
            List.of(15D, 15D, 13D, 13D, 11D, 11D, 10D, 10D, 9D, 9D, 8D, 7D),
            List.of(128, 256, 256, 384, 384, 576, 576, 768, 768, 960, 960, 960),
            List.of(
                    recipe(1, "wheat", 10),
                    recipe(2, "wheat", 20),
                    recipe(3, "wheat", 40),
                    recipe(4, "wheat", 64),
                    recipe(5, "hay_block", 12),
                    recipe(6, "hay_block", 24),
                    recipe(7, "hay_block", 48),
                    recipe(8, "hay_block", 64),
                    recipe(9, "enchanted_hay_block", 1),
                    recipe(10, "enchanted_hay_block", 2),
                    recipe(11, "enchanted_hay_block", 4)
            )
    ),
    CARROT(
            "carrot",
            "Carrot",
            Material.CARROT,
            Material.CARROT,
            Material.WOODEN_HOE,
            List.of(20D, 20D, 18D, 18D, 16D, 16D, 14D, 14D, 12D, 12D, 10D, 8D),
            List.of(64, 192, 192, 384, 384, 576, 576, 768, 768, 960, 960, 960),
            List.of(
                    recipe(1, "carrot_item", 16),
                    recipe(2, "carrot_item", 32),
                    recipe(3, "carrot_item", 64),
                    recipe(4, "enchanted_carrot", 1),
                    recipe(5, "enchanted_carrot", 3),
                    recipe(6, "enchanted_carrot", 8),
                    recipe(7, "enchanted_carrot", 16),
                    recipe(8, "enchanted_carrot", 32),
                    recipe(9, "enchanted_carrot", 64),
                    recipe(10, "enchanted_golden_carrot", 1),
                    recipe(11, "enchanted_golden_carrot", 2)
            )
    ),
    POTATO(
            "potato",
            "Potato",
            Material.POTATO,
            Material.POTATO,
            Material.WOODEN_HOE,
            List.of(20D, 20D, 18D, 18D, 16D, 16D, 14D, 14D, 12D, 12D, 10D, 8D),
            List.of(64, 192, 192, 384, 384, 576, 576, 768, 768, 960, 960, 960),
            List.of(
                    recipe(1, "potato_item", 16),
                    recipe(2, "potato_item", 32),
                    recipe(3, "potato_item", 64),
                    recipe(4, "enchanted_potato", 1),
                    recipe(5, "enchanted_potato", 3),
                    recipe(6, "enchanted_potato", 8),
                    recipe(7, "enchanted_potato", 16),
                    recipe(8, "enchanted_potato", 32),
                    recipe(9, "enchanted_potato", 64),
                    recipe(10, "enchanted_baked_potato", 1),
                    recipe(11, "enchanted_baked_potato", 2)
            )
    ),
    SUGAR_CANE(
            "sugar_cane",
            "Sugar Cane",
            Material.SUGAR_CANE,
            Material.SUGAR_CANE,
            Material.WOODEN_HOE,
            List.of(22D, 22D, 20D, 20D, 18D, 18D, 16D, 16D, 14.5D, 14.5D, 12D, 9D),
            List.of(64, 192, 192, 384, 384, 576, 576, 768, 768, 960, 960, 960),
            List.of(
                    recipe(1, "sugar_cane", 16),
                    recipe(2, "sugar_cane", 32),
                    recipe(3, "sugar_cane", 64),
                    recipe(4, "enchanted_sugar", 1),
                    recipe(5, "enchanted_sugar", 3),
                    recipe(6, "enchanted_sugar", 8),
                    recipe(7, "enchanted_sugar", 16),
                    recipe(8, "enchanted_sugar", 32),
                    recipe(9, "enchanted_sugar", 64),
                    recipe(10, "enchanted_sugar_cane", 1),
                    recipe(11, "enchanted_sugar_cane", 2)
            )
    ),
    NETHER_WART(
            "nether_wart",
            "Nether Wart",
            Material.NETHER_WART,
            Material.NETHER_WART,
            Material.WOODEN_HOE,
            List.of(50D, 50D, 47D, 47D, 44D, 44D, 41D, 41D, 38D, 38D, 32D, 27D),
            List.of(64, 192, 192, 384, 384, 576, 576, 768, 768, 960, 960, 960),
            List.of(
                    recipe(1, "nether_stalk", 10),
                    recipe(2, "nether_stalk", 20),
                    recipe(3, "nether_stalk", 40),
                    recipe(4, "nether_stalk", 64),
                    recipe(5, "enchanted_nether_stalk", 1),
                    recipe(6, "enchanted_nether_stalk", 2),
                    recipe(7, "enchanted_nether_stalk", 4),
                    recipe(8, "enchanted_nether_stalk", 8),
                    recipe(9, "enchanted_nether_stalk", 16),
                    recipe(10, "enchanted_nether_stalk", 32),
                    recipe(11, "enchanted_nether_stalk", 64)
            )
    );

    private final String id;
    private final String displayName;
    private final Material iconMaterial;
    private final Material outputMaterial;
    private final Material baseCraftTool;
    private final List<Double> secondsPerActionByTier;
    private final List<Integer> storageByTier;
    private final List<TierRecipe> tierRecipes;

    MinionType(
            String id,
            String displayName,
            Material iconMaterial,
            Material outputMaterial,
            Material baseCraftTool,
            List<Double> secondsPerActionByTier,
            List<Integer> storageByTier,
            List<TierRecipe> tierRecipes
    ) {
        this.id = id;
        this.displayName = displayName;
        this.iconMaterial = iconMaterial;
        this.outputMaterial = outputMaterial;
        this.baseCraftTool = baseCraftTool;
        this.secondsPerActionByTier = secondsPerActionByTier;
        this.storageByTier = storageByTier;
        this.tierRecipes = tierRecipes;
    }

    private static TierRecipe recipe(int tier, String ingredientId, int amountPerOuterSlot) {
        return new TierRecipe(tier, ingredientId, amountPerOuterSlot);
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public Material iconMaterial() {
        return iconMaterial;
    }

    public Material outputMaterial() {
        return outputMaterial;
    }

    public Material baseCraftTool() {
        return baseCraftTool;
    }

    public int maxTier() {
        return secondsPerActionByTier.size();
    }

    public int maxCraftableTier() {
        return tierRecipes.size();
    }

    public double secondsPerAction(int tier) {
        int clamped = Math.max(1, Math.min(tier, maxTier()));
        return secondsPerActionByTier.get(clamped - 1);
    }

    public int storageForTier(int tier) {
        int clamped = Math.max(1, Math.min(tier, storageByTier.size()));
        return storageByTier.get(clamped - 1);
    }

    public TierRecipe recipeForTier(int tier) {
        if (tier < 1 || tier > tierRecipes.size()) {
            return null;
        }
        return tierRecipes.get(tier - 1);
    }

    public static MinionType parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (MinionType type : values()) {
            if (type.id.equals(normalized)) {
                return type;
            }
            if (type.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return type;
            }
        }
        return null;
    }

    public record TierRecipe(int tier, String ingredientId, int amountPerOuterSlot) {
    }

    public Material placeMaterial() {
        return switch (this) {
            case COBBLESTONE -> Material.COBBLESTONE;
            case COAL -> Material.COAL_ORE;
            case IRON -> Material.IRON_ORE;
            case GOLD -> Material.GOLD_ORE;
            case REDSTONE -> Material.REDSTONE_ORE;
            case LAPIS -> Material.LAPIS_ORE;
            case DIAMOND -> Material.DIAMOND_ORE;
            case EMERALD -> Material.EMERALD_ORE;
            case WHEAT -> Material.WHEAT;
            case CARROT -> Material.CARROTS;
            case POTATO -> Material.POTATOES;
            case SUGAR_CANE -> Material.SUGAR_CANE;
            case NETHER_WART -> Material.NETHER_WART;
        };
    }
}
