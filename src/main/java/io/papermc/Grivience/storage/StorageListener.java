package io.papermc.Grivience.storage;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listener for storage system events.
 * Handles inventory interactions, persistence, and death handling.
 * 
 * Integration:
 * - Works with Skyblock Profile System for profile-specific storage
 * - Each storage access is tied to the player's currently selected profile
 */
public class StorageListener implements Listener {
    private final GriviencePlugin plugin;
    private final StorageManager storageManager;
    // Track open storage inventories: player UUID -> (storage type, profile UUID)
    private final Map<UUID, StorageAccess> openInventories;

    public StorageListener(GriviencePlugin plugin, StorageManager storageManager) {
        this.plugin = plugin;
        this.storageManager = storageManager;
        this.openInventories = new HashMap<>();
    }

    /**
     * Handle player join - load storage data.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Ensure storage profiles are loaded for this player
        for (StorageType type : StorageType.values()) {
            if (player.hasPermission(type.getPermissionNode())) {
                storageManager.getStorage(player, type);
            }
        }
    }

    /**
     * Handle player quit - save storage data.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Save storage data
        storageManager.savePlayerData(playerId);

        // Close any open storage inventories
        if (openInventories.containsKey(playerId)) {
            openInventories.remove(playerId);
        }
    }

    /**
     * Handle player death - optionally preserve storage contents.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Storage contents are preserved (not dropped on death)
        // This is handled by the storage system persistence
    }

    /**
     * Handle inventory clicks in storage inventories.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        StorageAccess access = openInventories.get(player.getUniqueId());
        if (access == null) {
            return;
        }

        StorageProfile profile = storageManager.getStorage(player, access.profileId, access.storageType);
        if (profile == null || profile.isLocked()) {
            event.setCancelled(true);
            player.sendMessage("§cThis storage is locked!");
            return;
        }

        Inventory clickedInventory = event.getClickedInventory();
        Inventory topInventory = player.getOpenInventory().getTopInventory();

        // Only handle clicks in the top inventory (storage)
        if (clickedInventory == null || !clickedInventory.equals(topInventory)) {
            return;
        }

        int slot = event.getSlot();

        // Handle back button click (last slot)
        if (slot == topInventory.getSize() - 1) {
            event.setCancelled(true);
            player.closeInventory();
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getStorageGui().openMainMenu(player);
            }, 1L);
            return;
        }

        // Prevent clicking outside storage bounds (excluding back button slot)
        if (slot >= profile.getCurrentSlots() && slot != topInventory.getSize() - 1) {
            event.setCancelled(true);
            return;
        }

        // Handle cursor movement and item placement
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        // Update profile contents after the click
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            profile.loadFromInventory(player.getOpenInventory().getTopInventory());
        }, 1L);
    }

    /**
     * Handle inventory drag in storage inventories.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        StorageAccess access = openInventories.get(player.getUniqueId());
        if (access == null) {
            return;
        }

        StorageProfile profile = storageManager.getStorage(player, access.profileId, access.storageType);
        if (profile == null || profile.isLocked()) {
            event.setCancelled(true);
            return;
        }

        Inventory topInventory = player.getOpenInventory().getTopInventory();

        // Check if any slots are outside storage bounds
        for (int slot : event.getRawSlots()) {
            if (slot < topInventory.getSize() && slot >= profile.getCurrentSlots()) {
                event.setCancelled(true);
                player.sendMessage("§cCannot place items beyond storage capacity!");
                return;
            }
        }

        // Update profile contents after the drag
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            profile.loadFromInventory(player.getOpenInventory().getTopInventory());
        }, 1L);
    }

    /**
     * Handle inventory close - save contents.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        UUID playerId = player.getUniqueId();

        StorageAccess access = openInventories.remove(playerId);
        if (access == null) {
            return;
        }

        StorageProfile profile = storageManager.getStorage(player, access.profileId, access.storageType);
        if (profile != null) {
            // Save contents
            profile.loadFromInventory(event.getInventory());
            profile.updateLastAccessed();

            // Save to disk
            storageManager.savePlayerData(playerId);
        }
    }

    /**
     * Track when a player opens a storage inventory with their current profile.
     */
    public void trackStorageOpen(Player player, StorageType type) {
        SkyBlockProfile profile = storageManager.getSelectedSkyBlockProfile(player);
        UUID profileId = profile != null ? profile.getProfileId() : player.getUniqueId();
        openInventories.put(player.getUniqueId(), new StorageAccess(type, profileId));
    }

    /**
     * Stop tracking a player's storage inventory on close.
     */
    public void stopTracking(Player player) {
        openInventories.remove(player.getUniqueId());
    }

    /**
     * Simple data class to track storage access.
     */
    private static class StorageAccess {
        final StorageType storageType;
        final UUID profileId;

        StorageAccess(StorageType storageType, UUID profileId) {
            this.storageType = storageType;
            this.profileId = profileId;
        }
    }
}

