package io.papermc.Grivience.pet;

import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import java.util.List;
import java.util.Map;

/**
 * 1:1 Hypixel Skyblock Pet Definition
 */
public record PetDefinition(
        String id,
        String displayName,
        Material icon,
        String headTexture, // Base64 or URL
        List<String> lore, // Description lore (optional)
        List<PotionEffect> effects, // Legacy support
        Map<org.bukkit.attribute.Attribute, Double> attributeBonuses, // Legacy support
        double cropMultiplier, // Legacy support
        PetStatBonuses stats, // Base stats at Level 100
        PetSkillType skillType,
        PetRarity rarity,
        int maxLevel,
        List<PetAbility> abilities
) {
    public PetDefinition(String id, String displayName, Material icon, String headTexture, List<String> lore, List<PotionEffect> effects, Map<org.bukkit.attribute.Attribute, Double> attributeBonuses, double cropMultiplier, PetStatBonuses stats, PetSkillType skillType, PetRarity rarity, int maxLevel) {
        this(id, displayName, icon, headTexture, lore, effects, attributeBonuses, cropMultiplier, stats, skillType, rarity, maxLevel, List.of());
    }

    public record PetAbility(
            String name,
            List<String> description, // Lore with placeholders like {value}
            double startValue, // Value at level 1
            double endValue,   // Value at level 100
            PetRarity minRarity // Ability only unlocks at this rarity or higher
    ) {
        public double getValue(int level) {
            if (level <= 1) return startValue;
            if (level >= 100) return endValue;
            return startValue + ((endValue - startValue) * (level - 1) / 99.0);
        }
    }
}
