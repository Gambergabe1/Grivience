package io.papermc.Grivience.util;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class StackSizeSanitizer {
    private StackSizeSanitizer() {
    }

    public static boolean isOverstacked(ItemStack stack) {
        return stack != null
                && !stack.getType().isAir()
                && stack.getAmount() > maxLegalAmount(stack);
    }

    public static int maxLegalAmount(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return 0;
        }
        return Math.max(1, stack.getMaxStackSize());
    }

    public static SanitizedStack sanitize(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return new SanitizedStack(null, List.of());
        }

        int amount = stack.getAmount();
        if (amount <= 0) {
            return new SanitizedStack(null, List.of());
        }
        int max = maxLegalAmount(stack);
        if (amount <= max) {
            return new SanitizedStack(stack.clone(), List.of());
        }

        ItemStack primary = stack.clone();
        primary.setAmount(max);
        return new SanitizedStack(primary, splitToLegalStacks(stack, amount - max));
    }

    public static List<ItemStack> splitToLegalStacks(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return List.of();
        }
        return splitToLegalStacks(stack, stack.getAmount());
    }

    public static List<ItemStack> splitToLegalStacks(ItemStack template, int amount) {
        List<ItemStack> stacks = new ArrayList<>();
        if (template == null || template.getType().isAir() || amount <= 0) {
            return stacks;
        }

        int max = maxLegalAmount(template);
        int remaining = amount;
        while (remaining > 0) {
            int chunk = Math.min(remaining, max);
            ItemStack piece = template.clone();
            piece.setAmount(chunk);
            stacks.add(piece);
            remaining -= chunk;
        }
        return stacks;
    }

    public record SanitizedStack(ItemStack primary, List<ItemStack> overflow) {
    }
}
