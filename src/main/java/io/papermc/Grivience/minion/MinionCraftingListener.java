package io.papermc.Grivience.minion;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

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
        if (match == null) {
            return;
        }
        inventory.setResult(match.result().clone());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        CraftingInventory inventory = event.getInventory();
        if (inventory.getType() != InventoryType.WORKBENCH) {
            return;
        }

        MinionManager.CraftingMatch match = minionManager.matchCraftingRecipe(inventory.getMatrix());
        if (match == null) {
            return;
        }

        event.setCancelled(true);

        ItemStack[] matrix = inventory.getMatrix();
        int maxCrafts = minionManager.maxCrafts(matrix, match);
        if (maxCrafts <= 0) {
            inventory.setResult(null);
            return;
        }

        int crafts = event.isShiftClick() ? maxCrafts : 1;
        if (!minionManager.consumeForCraft(matrix, match, crafts)) {
            inventory.setResult(null);
            return;
        }

        inventory.setMatrix(matrix);
        rewardCraft(player, match.result(), crafts);

        MinionManager.CraftingMatch next = minionManager.matchCraftingRecipe(inventory.getMatrix());
        inventory.setResult(next == null ? null : next.result().clone());

        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.65F, 1.2F);
        player.updateInventory();
    }

    private void rewardCraft(Player player, ItemStack baseResult, int crafts) {
        if (player == null || baseResult == null || crafts <= 0) {
            return;
        }

        int total = baseResult.getAmount() * crafts;
        int maxStack = Math.max(1, baseResult.getMaxStackSize());
        while (total > 0) {
            int amount = Math.min(total, maxStack);
            ItemStack reward = baseResult.clone();
            reward.setAmount(amount);
            var leftover = player.getInventory().addItem(reward);
            if (!leftover.isEmpty()) {
                for (ItemStack stack : leftover.values()) {
                    if (stack != null && !stack.getType().isAir()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), stack);
                    }
                }
            }
            total -= amount;
        }
    }
}
