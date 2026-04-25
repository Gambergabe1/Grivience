package io.papermc.Grivience.listener;

import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeUnlockListenerTest {

    @Test
    void essentialVanillaRecipesIncludeReportedBasics() {
        assertTrue(RecipeUnlockListener.isEssentialVanillaRecipe(NamespacedKey.minecraft("crafting_table")));
        assertTrue(RecipeUnlockListener.isEssentialVanillaRecipe(NamespacedKey.minecraft("furnace")));
        assertTrue(RecipeUnlockListener.isEssentialVanillaRecipe(NamespacedKey.minecraft("oak_slab")));
        assertTrue(RecipeUnlockListener.isEssentialVanillaRecipe(NamespacedKey.minecraft("stone_slab")));
        assertTrue(RecipeUnlockListener.isEssentialVanillaRecipe(NamespacedKey.minecraft("oak_stairs")));
        assertTrue(RecipeUnlockListener.isEssentialVanillaRecipe(NamespacedKey.minecraft("pale_oak_stairs")));
    }

    @Test
    void nonEssentialOrCustomRecipesStayFiltered() {
        assertFalse(RecipeUnlockListener.isEssentialVanillaRecipe(NamespacedKey.minecraft("diamond_sword")));
        assertFalse(RecipeUnlockListener.isEssentialVanillaRecipe(NamespacedKey.minecraft("iron_pickaxe")));
        assertFalse(RecipeUnlockListener.isEssentialVanillaRecipe(new NamespacedKey("grivience", "furnace")));
    }

    @Test
    void recipeKeyReturnsOnlyKeysFromKeyedRecipes() {
        NamespacedKey expected = NamespacedKey.minecraft("crafting_table");
        assertEquals(expected, RecipeUnlockListener.recipeKey(new FakeKeyedRecipe(expected)));
        assertNull(RecipeUnlockListener.recipeKey(new FakeRecipe()));
    }

    private static class FakeRecipe implements Recipe {
        @Override
        public ItemStack getResult() {
            return new ItemStack(Material.STONE);
        }
    }

    private static final class FakeKeyedRecipe extends FakeRecipe implements Keyed {
        private final NamespacedKey key;

        private FakeKeyedRecipe(NamespacedKey key) {
            this.key = key;
        }

        @Override
        public NamespacedKey getKey() {
            return key;
        }
    }
}
