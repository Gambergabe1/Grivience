package io.papermc.Grivience.listener;

import io.papermc.Grivience.minion.MinionManager;
import io.papermc.Grivience.util.EnchantedItemRecipeCatalog;
import io.papermc.Grivience.util.EnchantedItemRecipePattern;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

/**
 * Final guard rail that rejects any enchanted-item craft result that does not match the canonical recipe map.
 */
public final class EnchantedItemRecipeGuardListener implements Listener {
    private final MinionManager minionManager;

    public EnchantedItemRecipeGuardListener(MinionManager minionManager) {
        this.minionManager = minionManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepare(PrepareItemCraftEvent event) {
        if (!(event.getInventory() instanceof CraftingInventory inventory)) {
            return;
        }
        if (inventory.getType() != InventoryType.WORKBENCH) {
            return;
        }
        if (!isCanonicalCraft(inventory.getResult(), inventory.getMatrix())) {
            inventory.setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getInventory() instanceof CraftingInventory inventory)) {
            return;
        }
        if (inventory.getType() != InventoryType.WORKBENCH) {
            return;
        }
        if (isCanonicalCraft(event.getCurrentItem(), inventory.getMatrix())) {
            return;
        }
        event.setCancelled(true);
        inventory.setResult(null);
        if (event.getWhoClicked() instanceof Player player) {
            player.updateInventory();
        }
    }

    private boolean isCanonicalCraft(ItemStack result, ItemStack[] matrix) {
        String outputId = canonicalOutputId(result);
        if (outputId == null) {
            return true;
        }
        String expectedInput = EnchantedItemRecipeCatalog.inputFor(outputId);
        if (expectedInput == null || minionManager == null) {
            return false;
        }
        return EnchantedItemRecipePattern.matches(
                matrix,
                stack -> minionManager.matchesIngredient(stack, expectedInput, EnchantedItemRecipePattern.SLOT_COST)
        );
    }

    private String canonicalOutputId(ItemStack result) {
        if (result == null || result.getType().isAir() || minionManager == null) {
            return null;
        }
        String ingredientId = minionManager.readIngredientId(result);
        if (ingredientId == null || !ingredientId.startsWith("enchanted_")) {
            return null;
        }
        return EnchantedItemRecipeCatalog.normalizeId(ingredientId);
    }
}
