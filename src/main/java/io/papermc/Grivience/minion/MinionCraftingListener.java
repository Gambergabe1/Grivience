package io.papermc.Grivience.minion;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class MinionCraftingListener implements Listener {
    private final MinionManager minionManager;

    public MinionCraftingListener(MinionManager minionManager) {
        this.minionManager = minionManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepare(PrepareItemCraftEvent event) {
        CraftingInventory inventory = event.getInventory();
        if (inventory.getType() != InventoryType.WORKBENCH) {
            return;
        }
        
        MinionManager.CraftingMatch match = minionManager.matchCraftingRecipe(inventory.getMatrix());
        if (match != null) {
            inventory.setResult(match.result().clone());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onResultClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory() instanceof CraftingInventory inventory)) return;
        if (inventory.getType() != InventoryType.WORKBENCH) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;

        ItemStack result = inventory.getResult();
        if (result == null || result.getType().isAir()) return;

        MinionManager.CraftingMatch match = minionManager.matchCraftingRecipe(inventory.getMatrix());
        if (match == null) return;

        // Verify the result matches our expected custom item (to avoid stealing vanilla results)
        if (!isSimilarCustomItem(result, match.result())) return;

        ClickType click = event.getClick();
        if (!isSupportedClick(click)) {
            event.setCancelled(true);
            return;
        }

        // Handle the craft manually
        event.setCancelled(true);

        ItemStack[] matrix = inventory.getMatrix();
        int maxByInput = minionManager.maxCrafts(matrix, match);
        if (maxByInput <= 0) return;

        int crafts;
        if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
            crafts = Math.min(maxByInput, maxCraftsForInventory(player.getInventory(), result));
        } else {
            crafts = canPlaceOnCursor(event.getCursor(), result) ? 1 : 0;
        }

        if (crafts <= 0) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Consume ingredients
        if (!minionManager.consumeForCraft(matrix, match, crafts)) return;

        // Deliver results
        deliverItems(player, event, result, crafts);

        // Update matrix on next tick to avoid desync
        Bukkit.getScheduler().runTask(minionManager.getPlugin(), () -> {
            inventory.setMatrix(matrix);
            
            // Re-calculate the next available craft
            MinionManager.CraftingMatch next = minionManager.matchCraftingRecipe(inventory.getMatrix());
            inventory.setResult(next == null ? null : next.result().clone());
            
            player.updateInventory();
        });

        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.65f, 1.2f);
    }

    private boolean isSimilarCustomItem(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        if (a.getType() != b.getType()) return false;
        // Basic similarity check, could be more robust by checking custom NBT
        return a.hasItemMeta() == b.hasItemMeta();
    }

    private boolean isSupportedClick(ClickType click) {
        return switch (click) {
            case LEFT, RIGHT, SHIFT_LEFT, SHIFT_RIGHT -> true;
            default -> false;
        };
    }

    private int maxCraftsForInventory(PlayerInventory inventory, ItemStack result) {
        int maxStack = result.getMaxStackSize();
        int totalRoom = 0;
        for (ItemStack slot : inventory.getStorageContents()) {
            if (slot == null || slot.getType().isAir()) {
                totalRoom += maxStack;
            } else if (slot.isSimilar(result)) {
                totalRoom += Math.max(0, maxStack - slot.getAmount());
            }
        }
        return totalRoom / result.getAmount();
    }

    private boolean canPlaceOnCursor(ItemStack cursor, ItemStack result) {
        if (cursor == null || cursor.getType().isAir()) return true;
        return cursor.isSimilar(result) && (cursor.getAmount() + result.getAmount() <= cursor.getMaxStackSize());
    }

    private void deliverItems(Player player, InventoryClickEvent event, ItemStack result, int crafts) {
        int totalAmount = result.getAmount() * crafts;
        ItemStack delivery = result.clone();
        delivery.setAmount(totalAmount);

        if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
            player.getInventory().addItem(delivery).values().forEach(remaining -> {
                player.getWorld().dropItemNaturally(player.getLocation(), remaining);
            });
        } else {
            ItemStack cursor = event.getCursor();
            if (cursor == null || cursor.getType().isAir()) {
                event.setCursor(delivery);
            } else {
                cursor.setAmount(cursor.getAmount() + totalAmount);
                event.setCursor(cursor);
            }
        }
    }
}
