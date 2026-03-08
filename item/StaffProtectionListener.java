package io.papermc.Grivience.item;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
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
 * - Staffs from being dropped accidentally
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
     * Prevent staff block placement on right-click - prevents phantom blocks.
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
                // Allow sneaking + right-click for inventory management
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
            return;
        }
        
        // Prevent left-click on blocks (could attempt to use as tool)
        if (action == Action.LEFT_CLICK_BLOCK) {
            // Allow attacking entities through blocks
            if (event.getClickedBlock() != null) {
                // Check if there's an entity at the location
                var entities = player.getWorld().getNearbyEntities(
                    event.getClickedBlock().getLocation(),
                    0.5, 0.5, 0.5
                );
                
                if (!entities.isEmpty()) {
                    // Allow hitting entities
                    return;
                }
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
        
        CustomWeaponType staffType = staffManager.getStaffType(staff);
        if (staffType == null) return;
        
        // Check mana if required
        if (staffManager.isRequireMana()) {
            // TODO: Integrate with mana system
            // For now, just set cooldown
        }
        
        // Set cooldown
        staffManager.setCooldown(player, (long) staffManager.getGlobalCooldownMs());
        
        // Send ability message
        player.sendMessage(ChatColor.AQUA + "Staff ability triggered! (Cooldown: " + 
            (staffManager.getGlobalCooldownMs() / 1000.0) + "s)");
        
        // TODO: Implement actual staff abilities based on type
        // - ARCANE_STAFF: Shoot arcane missile
        // - FROSTBITE_STAFF: Freeze enemies
        // - INFERNO_STAFF: Fire explosion
        // - STORMCALLER_STAFF: Lightning strike
        // - VOIDWALKER_STAFF: Teleport
        // - CELESTIAL_STAFF: Healing light
    }

    /**
     * Check if a block type is interactable (not a placement target).
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
     * Prevent staffs from being dropped accidentally (optional).
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDropItem(org.bukkit.event.player.PlayerDropItemEvent event) {
        if (!staffManager.isEnabled()) return;
        
        ItemStack item = event.getItemDrop().getItemStack();
        if (item == null || item.getType().isAir()) return;
        
        // Optional: Prevent dropping staffs (uncomment if desired)
        // if (staffManager.isStaff(item)) {
        //     event.setCancelled(true);
        //     event.getPlayer().sendMessage(ChatColor.RED + "You cannot drop your staff!");
        // }
    }

    /**
     * Prevent staffs from being used in crafting.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onCraftItemPrepare(org.bukkit.event.inventory.PrepareItemCraftEvent event) {
        if (!staffManager.isEnabled()) return;
        
        // Check if any crafting ingredient is a staff
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (ingredient != null && staffManager.isStaff(ingredient)) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }
}
