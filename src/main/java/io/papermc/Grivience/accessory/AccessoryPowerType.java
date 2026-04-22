package io.papermc.Grivience.accessory;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

import java.util.Map;

/**
 * Represents a Hypixel-style Accessory Power (selected via Power Stones).
 * Stats scale based on the player's total Magical Power (MP).
 */
public enum AccessoryPowerType {
    NONE("None", "No power selected.", Material.FLOWER_POT, Map.of()),
    
    COMMANDO("Commando", "Balanced offensive stats.", Material.IRON_SWORD, Map.of(
            "Health", 0.4,
            "Strength", 0.6,
            "Crit Damage", 0.6
    )),
    
    BLOODY("Bloody", "High Strength and Speed.", Material.REDSTONE, Map.of(
            "Strength", 0.8,
            "Speed", 0.4,
            "Attack Speed", 0.2
    )),
    
    SILKY("Silky", "Focuses entirely on Crit Damage.", Material.STRING, Map.of(
            "Crit Damage", 1.2,
            "Crit Chance", 0.1
    )),
    
    ADVENTURER("Adventurer", "Defensive and health focused.", Material.IRON_CHESTPLATE, Map.of(
            "Health", 1.0,
            "Defense", 0.5
    )),
    
    SHADED("Shaded", "High Crit Damage and Strength.", Material.COAL, Map.of(
            "Strength", 0.5,
            "Crit Damage", 1.0
    )),
    
    STRONG("Strong", "Pure Strength.", Material.STONE_SWORD, Map.of(
            "Strength", 1.2
    )),
    
    PLEASANT("Pleasant", "Pure Defense.", Material.IRON_INGOT, Map.of(
            "Defense", 1.2
    ));

    private final String displayName;
    private final String description;
    private final Material icon;
    private final Map<String, Double> statScales; // Stat name -> value per 10 MP

    AccessoryPowerType(String displayName, String description, Material icon, Map<String, Double> statScales) {
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
        this.statScales = statScales;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public Material getIcon() { return icon; }
    public Map<String, Double> getStatScales() { return statScales; }

    /**
     * Calculate stats for this power based on total Magical Power.
     */
    public Map<String, Double> calculateStats(int totalMP) {
        Map<String, Double> stats = new java.util.HashMap<>();
        double factor = totalMP / 10.0;
        
        statScales.forEach((stat, scale) -> {
            stats.put(stat, scale * factor);
        });
        
        return stats;
    }
}
