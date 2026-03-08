package io.papermc.Grivience.item;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Staff protection listener.
 * 
 * Prevents:
 * - Staffs from being placed as blocks
 * - Staffs from being used in crafting grids
 * 
 * Allows:
 * - Right-click abilities
 * - Left-click attacks
 * - Inventory management
 */
public final class StaffProtectionListener implements Listener {
    private final GriviencePlugin plugin;
    private final StaffManager staffManager;

    public StaffProtectionListener(GriviencePlugin plugin, StaffManager staffManager) {
        this.plugin = plugin;
        this.staffManager = staffManager;
    }

    /**
     * Prevent staffs from being placed as blocks - HIGHEST priority.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!staffManager.isEnabled()) return;
        
        ItemStack item = event.getItemInHand();
        if (item == null || item.getType().isAir()) return;
        
        // Check if the item is a staff
        if (staffManager.isStaff(item)) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            player.sendMessage(ChatColor.RED + "Staffs cannot be placed as blocks!");
            player.sendMessage(ChatColor.GRAY + "Right-click to use abilities instead.");
            return;
        }
    }

    /**
     * Prevent staff block placement on right-click.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!staffManager.isEnabled()) return;
        
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null || item.getType().isAir()) return;
        
        // Check if item is a staff
        if (!staffManager.isStaff(item)) return;
        
        Action action = event.getAction();
        
        // Allow right-click air/block for abilities
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            // Check if player is trying to place a block (shift + right-click)
            if (player.isSneaking()) {
                return;
            }
            
            // Check if clicking on a block that would place something
            if (action == Action.RIGHT_CLICK_BLOCK) {
                Material clickedType = event.getClickedBlock().getType();
                
                // Allow interacting with containers, doors, etc.
                if (isInteractable(clickedType)) {
                    return;
                }
                
                // Prevent any attempt to place the staff itself
                event.setCancelled(true);
                
                // Trigger staff ability instead
                triggerStaffAbility(player, item);
            }
        }
    }

    /**
     * Trigger staff ability on right-click.
     */
    private void triggerStaffAbility(Player player, ItemStack staff) {
        // Check cooldown
        if (staffManager.isOnCooldown(player)) {
            staffManager.sendCooldownMessage(player);
            return;
        }
        
        // Set cooldown
        staffManager.setCooldown(player, (long) staffManager.getGlobalCooldownMs());
        
        // Send ability message
        player.sendMessage(ChatColor.AQUA + "Staff ability triggered! (Cooldown: " + 
            (staffManager.getGlobalCooldownMs() / 1000.0) + "s)");
    }

    /**
     * Check if a block type is interactable.
     */
    private boolean isInteractable(Material material) {
        if (material == null) return false;
        
        return switch (material) {
            case CHEST, TRAPPED_CHEST, BARREL, SHULKER_BOX,
                 FURNACE, BLAST_FURNACE, SMOKER,
                 DISPENSER, DROPPER, HOPPER,
                 BREWING_STAND, ENCHANTING_TABLE, ANVIL,
                 CRAFTING_TABLE, CARTOGRAPHY_TABLE, FLETCHING_TABLE,
                 GRINDSTONE, SMITHING_TABLE, STONECUTTER, LOOM,
                 BEACON, ENDER_CHEST -> true;
            default -> material.toString().contains("DOOR") ||
                      material.toString().contains("TRAPDOOR") ||
                      material.toString().contains("GATE") ||
                      material.toString().contains("BUTTON") ||
                      material.toString().contains("PLATE");
        };
    }

    /**
     * Prevent staffs from being used in crafting.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onCraftItemPrepare(org.bukkit.event.inventory.PrepareItemCraftEvent event) {
        if (!staffManager.isEnabled()) return;
        
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (ingredient != null && staffManager.isStaff(ingredient)) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }
}
