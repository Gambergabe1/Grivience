package io.papermc.Grivience.item;

import org.bukkit.Material;

import java.util.Locale;

public enum ReforgeStoneType {
    GENTLE_STONE(ReforgeType.GENTLE, Material.FLINT, 4),
    ODD_STONE(ReforgeType.ODD, Material.FERMENTED_SPIDER_EYE, 6),
    FAST_STONE(ReforgeType.FAST, Material.FEATHER, 5),
    FAIR_STONE(ReforgeType.FAIR, Material.AMETHYST_SHARD, 6),
    EPIC_STONE(ReforgeType.EPIC, Material.PRISMARINE_CRYSTALS, 7),
    SHARP_STONE(ReforgeType.SHARP, Material.IRON_NUGGET, 7),
    HEROIC_STONE(ReforgeType.HEROIC, Material.BLAZE_ROD, 8),
    SPICY_STONE(ReforgeType.SPICY, Material.MAGMA_CREAM, 8),
    LEGENDARY_STONE(ReforgeType.LEGENDARY, Material.NETHER_STAR, 10);

    private final ReforgeType reforgeType;
    private final Material material;
    private final int levelCost;

    ReforgeStoneType(ReforgeType reforgeType, Material material, int levelCost) {
        this.reforgeType = reforgeType;
        this.material = material;
        this.levelCost = levelCost;
    }

    public ReforgeType reforgeType() {
        return reforgeType;
    }

    public Material material() {
        return material;
    }

    public int levelCost() {
        return levelCost;
    }

    public static ReforgeStoneType parse(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String normalized = input.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        for (ReforgeStoneType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
