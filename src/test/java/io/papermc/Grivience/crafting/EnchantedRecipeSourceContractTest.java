package io.papermc.Grivience.crafting;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantedRecipeSourceContractTest {

    @Test
    void canonicalRecipePatternUsesTheScreenshotShape() throws IOException {
        String source = read("src/main/java/io/papermc/Grivience/util/EnchantedItemRecipePattern.java");

        assertTrue(source.contains("SLOT_COST = 32"), "Canonical enchanted recipe should keep 32 items per filled slot");
        assertTrue(source.contains("REQUIRED_SLOTS = {0, 1, 2, 3, 4}"),
                "Canonical enchanted recipe should use the top-row plus second-row-left layout");
        assertTrue(source.contains("TOTAL_COST = SLOT_COST * REQUIRED_SLOTS.length"),
                "Canonical enchanted recipe should remain a five-stack compression recipe");
    }

    @Test
    void farmCraftingUsesTheCanonicalRecipePattern() throws IOException {
        String source = read("src/main/java/io/papermc/Grivience/minion/MinionCraftingListener.java");
        String pluginSource = read("src/main/java/io/papermc/Grivience/GriviencePlugin.java");

        assertTrue(source.contains("onResultClick"),
                "Farm enchanted crafting should be handled by a manual Result slot click listener");
        assertTrue(source.contains("minionManager.matchCraftingRecipe"),
                "Farm enchanted crafting should validate against the canonical recipe catalog");
        assertFalse(pluginSource.contains("new EnchantedFarmCraftListener"),
                "Farm crafting should not be handled by a second competing workbench listener");
    }

    @Test
    void minionRecipeTableDefinesOnlyCanonicalEnchantedRecipes() throws IOException {
        String source = read("src/main/java/io/papermc/Grivience/minion/MinionManager.java");
        String catalogSource = read("src/main/java/io/papermc/Grivience/util/EnchantedItemRecipeCatalog.java");

        assertTrue(source.contains("for (Map.Entry<String, String> entry : EnchantedItemRecipeCatalog.canonicalInputs().entrySet())"),
                "Minion recipe table should build enchanted recipes from the shared canonical catalog");
        assertTrue(catalogSource.contains("for (EnchantedFarmItemType type : EnchantedFarmItemType.values())"),
                "Canonical enchanted recipe catalog should normalize all enchanted farm items");
        assertTrue(catalogSource.contains("addCanonical(map, \"enchanted_golden_carrot\", \"golden_carrot\")"),
                "Enchanted golden carrot should use the same single-ingredient canonical recipe");
        assertTrue(catalogSource.contains("addCanonical(map, \"enchanted_mycelium\", \"mycelium\")"),
                "Other enchanted ingredient outputs should stay on the canonical recipe catalog");
        assertFalse(source.contains("fullRecipe("), "No enchanted ingredient should keep a full-grid alternate recipe");
        assertFalse(source.contains("crossCenterRecipe("), "No enchanted ingredient should keep a mixed-input alternate recipe");
        assertFalse(source.contains("crossRecipe("), "No enchanted ingredient should keep the old cross-shaped recipe helper");
    }

    @Test
    void aliasAndCompactorContractsStayAlignedWithTheCanonicalRecipe() throws IOException {
        String farmTypeSource = read("src/main/java/io/papermc/Grivience/item/EnchantedFarmItemType.java");
        String compactorSource = read("src/main/java/io/papermc/Grivience/compactor/PersonalCompactorManager.java");

        assertTrue(farmTypeSource.contains("case \"enchanted_hay_block\" -> \"enchanted_hay_bale\""),
                "Farm item parsing should understand the minion ingredient alias for enchanted hay");
        assertTrue(farmTypeSource.contains("case \"enchanted_nether_stalk\" -> \"enchanted_nether_wart\""),
                "Farm item parsing should understand the minion ingredient alias for enchanted nether wart");
        assertTrue(compactorSource.contains("putRule(rules, \"golden_carrot\", 160, \"enchanted_golden_carrot\", 1);"),
                "Personal compactor fallback should follow the canonical enchanted golden carrot recipe");
        assertFalse(compactorSource.contains("putRule(rules, \"hay_block\", 144, \"enchanted_hay_block\", 1);"),
                "Personal compactor fallback should not retain the old enchanted hay alternate recipe");
    }

    @Test
    void finalGuardAndRecipeRegistryStayLockedDown() throws IOException {
        String guardSource = read("src/main/java/io/papermc/Grivience/listener/EnchantedItemRecipeGuardListener.java");
        String pluginSource = read("src/main/java/io/papermc/Grivience/GriviencePlugin.java");
        String recipeRegistrySource = read("src/main/java/io/papermc/Grivience/crafting/RecipeRegistry.java");

        assertTrue(guardSource.contains("EnchantedItemRecipeCatalog.inputFor(outputId)"),
                "Final enchanted-item craft guard should validate results against the canonical recipe catalog");
        assertTrue(guardSource.contains("inventory.setResult(null);"),
                "Final enchanted-item craft guard should clear invalid crafted results");
        assertTrue(pluginSource.contains("new EnchantedItemRecipeGuardListener(minionManager)"),
                "Plugin startup should register the final enchanted-item craft guard");
        assertFalse(recipeRegistrySource.contains("new NamespacedKey(plugin, \"enchanted_diamond_block\")"),
                "RecipeRegistry should not keep the old enchanted diamond block placeholder recipe");
    }

    @Test
    void runtimeCoverageValidationStayInTheRecipeTable() throws IOException {
        String source = read("src/main/java/io/papermc/Grivience/minion/MinionManager.java");

        assertTrue(source.contains("validateEnchantedRecipeCoverage();"),
                "MinionManager should fail fast if enchanted recipe coverage drifts");
        assertTrue(source.contains("private static void validateEnchantedRecipeCoverage()"),
                "Enchanted recipe coverage validation should live next to the recipe table");
        assertTrue(source.contains("if (!ingredientOutputs.equals(catalogOutputs))"),
                "Enchanted ingredient outputs should be checked against the canonical catalog");
        assertTrue(source.contains("if (ingredientOutputs.contains(outputId))"),
                "Enchanted outputs should not be defined by both ingredient and utility recipes");
        assertTrue(source.contains("Duplicate enchanted ingredient recipe detected for"),
                "Duplicate enchanted ingredient outputs should fail fast");
        assertTrue(source.contains("Duplicate enchanted utility recipe detected for"),
                "Duplicate enchanted utility outputs should fail fast");
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
