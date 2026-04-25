package io.papermc.Grivience.item;

import io.papermc.Grivience.crafting.RecipeCategory;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomArmorManagerTest {

    @Test
    void resolveRarityPrefersConfiguredValue() {
        assertEquals(
                ItemRarity.MYTHIC,
                CustomArmorManager.resolveRarity("mythic", ChatColor.GREEN + "Ignored Color")
        );
    }

    @Test
    void resolveRarityFallsBackToDisplayNameColor() {
        assertEquals(
                ItemRarity.LEGENDARY,
                CustomArmorManager.resolveRarity(null, ChatColor.GOLD + "Shogun Kabuto")
        );
    }

    @Test
    void buildDisplayLoreUsesHypixelSectionsAndStoredStats() {
        CustomArmorManager.ArmorPieceConfig config = new CustomArmorManager.ArmorPieceConfig(
                Material.GOLDEN_HELMET,
                ChatColor.LIGHT_PURPLE + "Starseer Crown",
                List.of(
                        "&7Radiates stellar energy.",
                        "&bIntelligence: +90",
                        "&6Full Set Bonus: Placeholder"
                ),
                3,
                1.6D,
                true,
                90,
                16,
                5,
                10,
                0,
                2,
                25,
                null
        );
        CustomArmorManager.CustomArmorSet set = new CustomArmorManager.CustomArmorSet(
                "starseer",
                ChatColor.LIGHT_PURPLE + "Starseer Regalia",
                ChatColor.GRAY + "Celestial armor.",
                ItemRarity.MYTHIC,
                Map.of(CustomArmorManager.ArmorPieceType.HELMET, config),
                List.of("&74 Pieces: &b+260 Mana", "&7and &f+20% &7ability damage"),
                4,
                RecipeCategory.COMBAT,
                null,
                0
        );

        List<String> plainLore = CustomArmorManager.buildDisplayLore(
                        set,
                        CustomArmorManager.ArmorPieceType.HELMET,
                        config,
                        List.of(ChatColor.BLUE + "Protection V")
                ).stream()
                .map(ChatColor::stripColor)
                .toList();

        assertTrue(plainLore.contains("Health: +16"));
        assertTrue(plainLore.contains("Defense: +3"));
        assertTrue(plainLore.contains("Armor Toughness: +1.6"));
        assertTrue(plainLore.contains("Intelligence: +90"));
        assertTrue(plainLore.contains("Crit Chance: +5%"));
        assertTrue(plainLore.contains("Crit Damage: +10%"));
        assertTrue(plainLore.contains("Breaking Power: +2"));
        assertTrue(plainLore.contains("Mining Speed: +25"));
        assertTrue(plainLore.contains("Protection V"));
        assertTrue(plainLore.contains("Full Set Bonus: Starseer Regalia"));
        assertTrue(plainLore.contains("4 Pieces: +260 Mana"));
        assertTrue(plainLore.contains("and +20% ability damage"));
        assertTrue(plainLore.contains("Radiates stellar energy."));
        assertEquals("MYTHIC HELMET", plainLore.get(plainLore.size() - 1));
        assertEquals(1L, plainLore.stream().filter("Intelligence: +90"::equals).count());
        assertFalse(plainLore.contains("Full Set Bonus: Placeholder"));
    }

    @Test
    void buildDisplayLoreUsesSetBonusHeaderWhenBonusesAreNotFullSetOnly() {
        CustomArmorManager.ArmorPieceConfig config = new CustomArmorManager.ArmorPieceConfig(
                Material.NETHERITE_HELMET,
                ChatColor.GOLD + "Crimson Helm",
                List.of("&7Forged in volcanic flame."),
                3,
                0.0D,
                false,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0,
                0,
                null
        );
        CustomArmorManager.CustomArmorSet set = new CustomArmorManager.CustomArmorSet(
                "crimson",
                ChatColor.GOLD + "Crimson Warrior",
                ChatColor.GRAY + "A fierce volcanic set.",
                ItemRarity.LEGENDARY,
                Map.of(CustomArmorManager.ArmorPieceType.HELMET, config),
                List.of("&72 Pieces: &fSpeed I", "&74 Pieces: &fStrength I"),
                2,
                RecipeCategory.COMBAT,
                null,
                0
        );

        List<String> plainLore = CustomArmorManager.buildDisplayLore(
                        set,
                        CustomArmorManager.ArmorPieceType.HELMET,
                        config,
                        List.of()
                ).stream()
                .map(ChatColor::stripColor)
                .toList();

        assertTrue(plainLore.contains("Set Bonus: Crimson Warrior"));
        assertFalse(plainLore.contains("Full Set Bonus: Crimson Warrior"));
    }
}
