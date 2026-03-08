package io.papermc.Grivience.skills;

import io.papermc.Grivience.pet.PetSkillType;
import org.bukkit.Material;

import java.util.Locale;

/**
 * Represents a Skyblock skill and its primary stat bonus.
 */
public enum SkyblockSkill {
    COMBAT("Combat", PetSkillType.COMBAT, Material.DIAMOND_SWORD, "\u2623 Crit Chance", "Warrior", 4.0, 210.0),
    MINING("Mining", PetSkillType.MINING, Material.DIAMOND_PICKAXE, "\u2747 Defense", "Spelunker", 4.0, 240.0),
    FARMING("Farming", PetSkillType.FARMING, Material.GOLDEN_HOE, "\u2764 Health", "Farmhand", 4.0, 240.0),
    FORAGING("Foraging", PetSkillType.FORAGING, Material.GOLDEN_AXE, "\u2741 Strength", "Logger", 4.0, 200.0),
    FISHING("Fishing", PetSkillType.FISHING, Material.FISHING_ROD, "\u2764 Health", "Treasure Hunter", 0.5, 5.0),
    ENCHANTING("Enchanting", PetSkillType.ENCHANTING, Material.ENCHANTED_BOOK, "\u270e Intelligence", "Conjurer", 5.0, 300.0),
    ALCHEMY("Alchemy", PetSkillType.ALCHEMY, Material.BREWING_STAND, "\u270e Intelligence", "Brewer", 1.0, 50.0),
    TAMING("Taming", PetSkillType.TAMING, Material.BONE, "\u2663 Pet Luck", "Zoologist", 1.0, 60.0),
    HUNTING("Hunting", PetSkillType.HUNTING, Material.BOW, "\u2618 Hunter Fortune", "Charming", 0.04, 1.0),
    DUNGEONEERING("Dungeoneering", PetSkillType.DUNGEONEERING, Material.WITHER_SKELETON_SKULL, "\u2764 Health", "Dungeon Master", 10.0, 475.0),
    CARPENTRY("Carpentry", PetSkillType.CARPENTRY, Material.CRAFTING_TABLE, "\u2764 Health", "None", 0.0, 0.0);

    private final String displayName;
    private final PetSkillType petSkillType;
    private final Material icon;
    private final String statName;
    private final String perkName;
    private final double perkBase;
    private final double perkMax;

    SkyblockSkill(String displayName, PetSkillType petSkillType, Material icon, String statName, String perkName, double perkBase, double perkMax) {
        this.displayName = displayName;
        this.petSkillType = petSkillType;
        this.icon = icon;
        this.statName = statName;
        this.perkName = perkName;
        this.perkBase = perkBase;
        this.perkMax = perkMax;
    }

    public String getDisplayName() {
        return displayName;
    }

    public PetSkillType getPetSkillType() {
        return petSkillType;
    }

    public Material getIcon() {
        return icon;
    }

    public String getStatName() {
        return statName;
    }

    public String getPerkName() {
        return perkName;
    }

    public double getPerkValue(int level) {
        if (level <= 0) return 0;
        if (this == FISHING) return Math.min(perkMax, level * perkBase / 10.0); // Special case for fishing treasure chance
        if (this == HUNTING) return Math.min(perkMax, level * perkBase);
        if (this == DUNGEONEERING) {
            // Hypixel Dungeoneering scaling is complex, but let's approximate
            return 10.0 + (level * 9.5); // 10% to 485% roughly
        }
        
        // Linear scaling for others
        double step = (perkMax - perkBase) / 59.0;
        return perkBase + (level - 1) * step;
    }

    public static SkyblockSkill parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String normalized = raw.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        if ("CATACOMBS".equals(normalized)) {
            normalized = "DUNGEONEERING";
        }

        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException e) {
            String compact = raw.trim()
                    .toLowerCase(Locale.ROOT)
                    .replace("-", "")
                    .replace("_", "")
                    .replace(" ", "");
            for (SkyblockSkill skill : values()) {
                String display = skill.displayName.toLowerCase(Locale.ROOT).replace(" ", "");
                if (display.equals(compact)) {
                    return skill;
                }
            }
            return null;
        }
    }
}
