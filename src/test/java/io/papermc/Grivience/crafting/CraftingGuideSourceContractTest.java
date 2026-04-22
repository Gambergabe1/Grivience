package io.papermc.Grivience.crafting;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CraftingGuideSourceContractTest {

    @Test
    void craftingGuiExposesAMaterialGuideInsteadOfAPlaceholder() throws IOException {
        String source = read("src/main/java/io/papermc/Grivience/crafting/CraftingGuiManager.java");

        assertTrue(source.contains("Crafting Materials"),
                "Crafting GUI should expose a crafting-material browser");
        assertTrue(source.contains("openMaterialIndex("),
                "Crafting GUI should have a material index menu");
        assertTrue(source.contains("openMaterialDetail("),
                "Crafting GUI should have a per-material recipe menu");
        assertTrue(source.contains("GUIDE_RECIPE_DETAIL"),
                "Crafting GUI should support a recipe-detail view from the material guide");
        assertFalse(source.contains("Recipe Guide is under construction!"),
                "Crafting GUI should no longer ship a placeholder recipe-guide button");
    }

    @Test
    void craftCommandSearchesMaterialsAsWellAsRecipes() throws IOException {
        String source = read("src/main/java/io/papermc/Grivience/crafting/CraftingManager.java");

        assertTrue(source.contains("findMaterials(query)"),
                "/craft search should look up crafting materials");
        assertTrue(source.contains("openMaterialGuide(player"),
                "/craft should open a material guide when a material search is exact");
        assertTrue(source.contains("No recipes or crafting materials found matching"),
                "/craft should report material search misses explicitly");
    }

    @Test
    void materialGuideCatalogPullsFromAllCraftSources() throws IOException {
        String source = read("src/main/java/io/papermc/Grivience/crafting/CraftingGuideCatalog.java");

        assertTrue(source.contains("addSkyblockRecipes(recipes);"),
                "Material guide should include RecipeRegistry recipes");
        assertTrue(source.contains("addCustomItemRecipes(recipes"),
                "Material guide should include custom item recipes");
        assertTrue(source.contains("addConfiguredArmorRecipes(recipes"),
                "Material guide should include config-driven armor recipes");
        assertTrue(source.contains("addPetRecipes(recipes"),
                "Material guide should include pet recipes");
        assertTrue(source.contains("addMinionRecipes(recipes"),
                "Material guide should include minion recipes");
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
