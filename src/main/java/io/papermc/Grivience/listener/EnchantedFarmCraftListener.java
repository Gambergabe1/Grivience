package io.papermc.Grivience.listener;

import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.item.EnchantedFarmItemType;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Objects;

/**
 * Lets players craft Skyblock-style enchanted farming items by compressing 160 of a base ingredient,
 * or 160 of the tier-1 enchanted item into its tier-2 form.
 */
public final class EnchantedFarmCraftListener implements Listener {
    private final CustomItemService customItemService;

    public EnchantedFarmCraftListener(CustomItemService customItemService) {
        this.customItemService = customItemService;
    }

    @EventHandler
    public void onPrepare(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory();
        // Skyblock crafting is done via a crafting table (3x3). Do not allow 2x2 player crafting grid.
        if (inv.getType() != InventoryType.WORKBENCH) {
            return;
        }

        ItemStack[] matrix = inv.getMatrix();
        if (matrix == null || matrix.length == 0) {
            return;
        }

        // Filter non-empty
        ItemStack first = Arrays.stream(matrix)
                .filter(Objects::nonNull)
                .filter(item -> !item.getType().isAir())
                .findFirst()
                .orElse(null);
        if (first == null) {
            return;
        }
        Material firstMat = first.getType();
        String firstId = customItemService.itemId(first);

        int total = Arrays.stream(matrix)
                .filter(Objects::nonNull)
                .mapToInt(ItemStack::getAmount)
                .sum();
        if (total < 160) {
            return;
        }

        // Tier 1: base material match
        EnchantedFarmItemType tier1 = matchTier1(firstMat);
        if (tier1 != null && allSameMaterial(matrix, firstMat)) {
            inv.setResult(customItemService.createEnchantedFarmItem(tier1));
            return;
        }

        // Tier 2: base enchanted id match
        EnchantedFarmItemType tier2 = matchTier2(firstId);
        if (tier2 != null && allSameId(matrix, firstId)) {
            inv.setResult(customItemService.createEnchantedFarmItem(tier2));
        }
    }

    private EnchantedFarmItemType matchTier1(Material material) {
        if (material == null) return null;
        for (EnchantedFarmItemType type : EnchantedFarmItemType.values()) {
            if (type.baseMaterial() != null && type.baseMaterial() == material) {
                return type;
            }
        }
        return null;
    }

    private EnchantedFarmItemType matchTier2(String itemId) {
        if (itemId == null) return null;
        for (EnchantedFarmItemType type : EnchantedFarmItemType.values()) {
            if (type.baseItemId() != null && type.baseItemId().equalsIgnoreCase(itemId)) {
                return type;
            }
        }
        return null;
    }

    private boolean allSameMaterial(ItemStack[] matrix, Material material) {
        return Arrays.stream(matrix)
                .filter(Objects::nonNull)
                .allMatch(item -> item.getType() == material);
    }

    private boolean allSameId(ItemStack[] matrix, String id) {
        return Arrays.stream(matrix)
                .filter(Objects::nonNull)
                .allMatch(item -> {
                    String other = customItemService.itemId(item);
                    return other != null && other.equalsIgnoreCase(id);
                });
    }
}

