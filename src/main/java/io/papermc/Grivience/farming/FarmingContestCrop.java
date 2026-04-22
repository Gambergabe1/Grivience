package io.papermc.Grivience.farming;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public enum FarmingContestCrop {
    WHEAT("wheat", "Wheat", Material.HAY_BLOCK, EnumSet.of(Material.WHEAT), List.of("wheat", "wheat_crop")),
    CARROT("carrot", "Carrot", Material.CARROT, EnumSet.of(Material.CARROT), List.of("carrot", "carrots")),
    POTATO("potato", "Potato", Material.POTATO, EnumSet.of(Material.POTATO), List.of("potato", "potatoes")),
    NETHER_WART("nether_wart", "Nether Wart", Material.NETHER_WART, EnumSet.of(Material.NETHER_WART), List.of("nether_wart", "netherwart", "wart")),
    SUGAR_CANE("sugar_cane", "Sugar Cane", Material.SUGAR_CANE, EnumSet.of(Material.SUGAR_CANE), List.of("sugar_cane", "sugarcane", "cane")),
    CACTUS("cactus", "Cactus", Material.CACTUS, EnumSet.of(Material.CACTUS), List.of("cactus")),
    COCOA_BEANS("cocoa_beans", "Cocoa Beans", Material.COCOA_BEANS, EnumSet.of(Material.COCOA_BEANS), List.of("cocoa_beans", "cocoa", "cocoabeans")),
    MELON("melon", "Melon", Material.MELON_SLICE, EnumSet.of(Material.MELON_SLICE), List.of("melon", "melons", "melon_slice")),
    PUMPKIN("pumpkin", "Pumpkin", Material.PUMPKIN, EnumSet.of(Material.PUMPKIN), List.of("pumpkin", "pumpkins")),
    MUSHROOM("mushroom", "Mushroom", Material.RED_MUSHROOM, EnumSet.of(Material.RED_MUSHROOM, Material.BROWN_MUSHROOM), List.of("mushroom", "mushrooms", "red_mushroom", "brown_mushroom"));

    private static final Map<String, FarmingContestCrop> BY_ALIAS = new HashMap<>();
    private static final Map<Material, FarmingContestCrop> BY_HARVEST_MATERIAL = new HashMap<>();

    static {
        for (FarmingContestCrop crop : values()) {
            BY_ALIAS.put(crop.id, crop);
            BY_ALIAS.put(normalize(crop.displayName), crop);
            for (String alias : crop.aliases) {
                BY_ALIAS.put(normalize(alias), crop);
            }
            for (Material material : crop.harvestMaterials) {
                BY_HARVEST_MATERIAL.put(material, crop);
            }
        }
    }

    private final String id;
    private final String displayName;
    private final Material icon;
    private final Set<Material> harvestMaterials;
    private final List<String> aliases;

    FarmingContestCrop(String id, String displayName, Material icon, Set<Material> harvestMaterials, List<String> aliases) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.harvestMaterials = Set.copyOf(harvestMaterials);
        this.aliases = List.copyOf(aliases);
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public Material icon() {
        return icon;
    }

    public static FarmingContestCrop fromHarvestMaterial(Material material) {
        if (material == null) {
            return null;
        }
        return BY_HARVEST_MATERIAL.get(material);
    }

    public static FarmingContestCrop fromInput(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        return BY_ALIAS.get(normalize(input));
    }

    public static List<String> ids() {
        List<String> ids = new ArrayList<>();
        for (FarmingContestCrop crop : values()) {
            ids.add(crop.id);
        }
        return ids;
    }

    public static List<FarmingContestCrop> parsePool(List<String> inputs) {
        LinkedHashSet<FarmingContestCrop> parsed = new LinkedHashSet<>();
        if (inputs != null) {
            for (String input : inputs) {
                FarmingContestCrop crop = fromInput(input);
                if (crop != null) {
                    parsed.add(crop);
                }
            }
        }
        if (parsed.size() < 3) {
            parsed.clear();
            for (FarmingContestCrop crop : values()) {
                parsed.add(crop);
            }
        }
        return List.copyOf(parsed);
    }

    private static String normalize(String input) {
        return input == null ? "" : input.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }
}
