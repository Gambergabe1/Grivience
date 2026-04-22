package io.papermc.Grivience.listener;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.util.DropDeliveryUtil;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts vanilla block drop entities into inventory-first rewards.
 */
public final class AutoPickupDropListener implements Listener {
    private final GriviencePlugin plugin;

    public AutoPickupDropListener(GriviencePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent event) {
        if (plugin == null || !plugin.getConfig().getBoolean("skyblock.auto-pickup.block-drops", true)) {
            return;
        }

        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        List<Item> droppedEntities = event.getItems();
        if (droppedEntities == null || droppedEntities.isEmpty()) {
            return;
        }

        Location fallback = event.getBlock().getLocation().add(0.5D, 0.5D, 0.5D);
        for (Item itemEntity : new ArrayList<>(droppedEntities)) {
            if (itemEntity == null) {
                continue;
            }
            ItemStack stack = itemEntity.getItemStack();
            if (stack == null || stack.getType().isAir() || stack.getAmount() <= 0) {
                itemEntity.remove();
                continue;
            }

            if (plugin.getFarmingContestManager() != null) {
                plugin.getFarmingContestManager().recordHarvest(player, stack);
            }
            DropDeliveryUtil.giveToInventoryOrDrop(player, stack, fallback);
            itemEntity.remove();
        }
    }
}
