package io.papermc.Grivience.item;

import org.bukkit.Material;

import java.util.Locale;

public enum ReforgeStoneType {
    JAGGED_STONE(ReforgeType.JAGGED, Material.FLINT, 6),
    TITAN_STONE(ReforgeType.TITANIC, Material.QUARTZ, 7),
    ARCANE_STONE(ReforgeType.ARCANE, Material.PRISMARINE_CRYSTALS, 8),
    TEMPEST_STONE(ReforgeType.TEMPEST, Material.NETHER_STAR, 10);

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
