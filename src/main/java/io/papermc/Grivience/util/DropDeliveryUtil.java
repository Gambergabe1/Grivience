package io.papermc.Grivience.util;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.Map;

/**
 * Inventory-first drop delivery helper.
 *
 * Items are inserted into the player's inventory first; overflow is dropped naturally
 * at the provided fallback location (or the player's current location if null).
 */
public final class DropDeliveryUtil {
    private DropDeliveryUtil() {
    }

    public static void giveToInventoryOrDrop(Player player, ItemStack stack, Location fallbackLocation) {
        giveToInventoryOrDrop(player, stack, fallbackLocation, false);
    }

    public static void giveToInventoryOrDrop(Player player, ItemStack stack, Location fallbackLocation, boolean trackCollection) {
        if (player == null || stack == null || stack.getType().isAir() || stack.getAmount() <= 0) {
            return;
        }

        Location dropAt = resolvedLocation(player, fallbackLocation);
        World world = dropAt.getWorld();
        if (world == null) {
            return;
        }

        for (ItemStack give : StackSizeSanitizer.splitToLegalStacks(stack)) {
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(give.clone());
            if (leftovers == null || leftovers.isEmpty()) {
                continue;
            }

            for (ItemStack leftover : leftovers.values()) {
                if (leftover == null || leftover.getType().isAir() || leftover.getAmount() <= 0) {
                    continue;
                }
                for (ItemStack legalDrop : StackSizeSanitizer.splitToLegalStacks(leftover)) {
                    world.dropItemNaturally(dropAt, legalDrop);
                }
            }
        }

        if (trackCollection) {
            awardCollection(player, stack);
        }
        queueCompactor(player);
    }

    public static void giveToInventoryOrDrop(Player player, Collection<ItemStack> stacks, Location fallbackLocation) {
        giveToInventoryOrDrop(player, stacks, fallbackLocation, false);
    }

    public static void giveToInventoryOrDrop(Player player, Collection<ItemStack> stacks, Location fallbackLocation, boolean trackCollection) {
        if (stacks == null || stacks.isEmpty()) {
            return;
        }
        for (ItemStack stack : stacks) {
            giveToInventoryOrDrop(player, stack, fallbackLocation, trackCollection);
        }
    }

    private static Location resolvedLocation(Player player, Location fallbackLocation) {
        if (fallbackLocation != null && fallbackLocation.getWorld() != null) {
            return fallbackLocation;
        }
        return player.getLocation();
    }

    private static void queueCompactor(Player player) {
        GriviencePlugin grivience = grivience();
        if (player == null || grivience == null || grivience.getPersonalCompactorManager() == null) {
            return;
        }
        grivience.getPersonalCompactorManager().queueCompaction(player);
    }

    private static void awardCollection(Player player, ItemStack stack) {
        GriviencePlugin grivience = grivience();
        if (player == null || stack == null || grivience == null || grivience.getCollectionsManager() == null) {
            return;
        }
        grivience.getCollectionsManager().addCollectionFromDrop(player, stack);
    }

    private static GriviencePlugin grivience() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Grivience");
        if (!(plugin instanceof GriviencePlugin grivience)) {
            return null;
        }
        return grivience;
    }
}
