package io.papermc.Grivience.util;

import org.bukkit.inventory.ItemStack;

import java.util.function.Predicate;

/**
 * Canonical recipe shape for enchanted-item compression recipes.
 */
public final class EnchantedItemRecipePattern {
    public static final int SLOT_COST = 32;
    public static final int[] REQUIRED_SLOTS = {0, 1, 2, 3, 4};
    public static final int TOTAL_COST = SLOT_COST * REQUIRED_SLOTS.length;

    private EnchantedItemRecipePattern() {
    }

    public static int[] slotCosts() {
        int[] costs = new int[9];
        for (int slot : REQUIRED_SLOTS) {
            costs[slot] = SLOT_COST;
        }
        return costs;
    }

    public static boolean matches(ItemStack[] matrix, Predicate<ItemStack> ingredientMatcher) {
        if (matrix == null || matrix.length < 9 || ingredientMatcher == null) {
            return false;
        }
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = matrix[slot];
            boolean required = isRequiredSlot(slot);
            if (!required) {
                if (stack != null && !stack.getType().isAir()) {
                    return false;
                }
                continue;
            }
            if (stack == null || stack.getType().isAir() || stack.getAmount() < SLOT_COST) {
                return false;
            }
            if (!ingredientMatcher.test(stack)) {
                return false;
            }
        }
        return true;
    }

    public static int maxCrafts(ItemStack[] matrix, Predicate<ItemStack> ingredientMatcher) {
        if (!matches(matrix, ingredientMatcher)) {
            return 0;
        }
        int maxCrafts = Integer.MAX_VALUE;
        for (int slot : REQUIRED_SLOTS) {
            ItemStack stack = matrix[slot];
            if (stack == null || stack.getType().isAir()) {
                return 0;
            }
            // Cap to legal max stack size to prevent over-calculation
            int legalAmount = Math.min(stack.getAmount(), stack.getMaxStackSize());
            maxCrafts = Math.min(maxCrafts, legalAmount / SLOT_COST);
        }
        return maxCrafts == Integer.MAX_VALUE ? 0 : Math.max(0, maxCrafts);
    }

    public static boolean consume(ItemStack[] matrix, int crafts, Predicate<ItemStack> ingredientMatcher) {
        if (matrix == null || crafts <= 0 || maxCrafts(matrix, ingredientMatcher) < crafts) {
            return false;
        }
        int amountPerSlot = SLOT_COST * crafts;
        for (int slot : REQUIRED_SLOTS) {
            ItemStack stack = matrix[slot];
            if (stack == null || stack.getType().isAir()) {
                return false;
            }
            int remaining = stack.getAmount() - amountPerSlot;
            if (remaining <= 0) {
                matrix[slot] = null;
                continue;
            }
            stack.setAmount(remaining);
            matrix[slot] = stack;
        }
        return true;
    }

    private static boolean isRequiredSlot(int slot) {
        for (int requiredSlot : REQUIRED_SLOTS) {
            if (requiredSlot == slot) {
                return true;
            }
        }
        return false;
    }
}
