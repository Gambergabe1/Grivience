package io.papermc.Grivience.compactor;

import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.Locale;

/**
 * Personal Compactor tiers, modeled after Skyblock tier naming.
 */
public enum PersonalCompactorType {
    PERSONAL_COMPACTOR_3000("PERSONAL_COMPACTOR_3000", "Personal Compactor 3000", Material.DROPPER, 1, ChatColor.GREEN + "" + ChatColor.BOLD + "UNCOMMON"),
    PERSONAL_COMPACTOR_4000("PERSONAL_COMPACTOR_4000", "Personal Compactor 4000", Material.DROPPER, 3, ChatColor.GREEN + "" + ChatColor.BOLD + "UNCOMMON"),
    PERSONAL_COMPACTOR_5000("PERSONAL_COMPACTOR_5000", "Personal Compactor 5000", Material.DROPPER, 6, ChatColor.BLUE + "" + ChatColor.BOLD + "RARE"),
    PERSONAL_COMPACTOR_6000("PERSONAL_COMPACTOR_6000", "Personal Compactor 6000", Material.DROPPER, 9, ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC"),
    PERSONAL_COMPACTOR_7000("PERSONAL_COMPACTOR_7000", "Personal Compactor 7000", Material.DROPPER, 12, ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY");

    private final String id;
    private final String displayName;
    private final Material material;
    private final int slots;
    private final String rarity;

    PersonalCompactorType(String id, String displayName, Material material, int slots, String rarity) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.slots = slots;
        this.rarity = rarity;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public Material material() {
        return material;
    }

    public int slots() {
        return slots;
    }

    public String rarity() {
        return rarity;
    }

    public static PersonalCompactorType parse(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String normalized = input.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        for (PersonalCompactorType type : values()) {
            if (type.name().equals(normalized) || type.id.equalsIgnoreCase(input)) {
                return type;
            }
        }
        return null;
    }
}
