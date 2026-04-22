package io.papermc.Grivience.storage;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
    private final Set<UUID> pendingProfileSyncs;

    public StorageListener(GriviencePlugin plugin, StorageManager storageManager) {
        this.plugin = plugin;
        this.storageManager = storageManager;
        this.openInventories = new HashMap<>();
        this.pendingProfileSyncs = ConcurrentHashMap.newKeySet();
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
        openInventories.remove(playerId);
        pendingProfileSyncs.remove(playerId);
    }

    /**
     * Handle player death - optionally preserve storage contents.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent event) {
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
        Inventory topInventory = player.getOpenInventory().getTopInventory();
        if (!isTrackedStorageInventory(topInventory, player, access)) {
            openInventories.remove(player.getUniqueId());
            return;
        }

        StorageProfile profile = storageManager.getStorage(player, access.profileId, access.storageType);
        if (profile == null || profile.isLocked()) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "This storage is locked!");
            return;
        }

        Inventory clickedInventory = event.getClickedInventory();

        if (clickedInventory == null) {
            return;
        }

        // Handle shift-click transfers from player inventory into storage.
        if (!clickedInventory.equals(topInventory)) {
            boolean movingIntoTop = event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY || event.getClick().isShiftClick();
            if (movingIntoTop && !isAllowedInStorage(access.storageType, event.getCurrentItem())) {
                event.setCancelled(true);
                sendRestrictionMessage(player, access.storageType);
            }
            return;
        }

        int slot = event.getSlot();

        // Handle back button click (last slot)
        if (slot == topInventory.getSize() - 1) {
            event.setCancelled(true);
            player.closeInventory();
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> plugin.getStorageGui().openMainMenu(player), 1L);
            return;
        }

        // Prevent clicking outside storage bounds (excluding back button slot)
        if (slot >= profile.getCurrentSlots() && slot != topInventory.getSize() - 1) {
            event.setCancelled(true);
            return;
        }

        // Restrict input items for specialized bags.
        if (slot < profile.getCurrentSlots()) {
            ItemStack incoming = incomingItem(player, event);
            if (!isAllowedInStorage(access.storageType, incoming)) {
                event.setCancelled(true);
                sendRestrictionMessage(player, access.storageType);
                return;
            }
        }

        // Update profile contents after the click.
        scheduleProfileSync(player, access, profile, topInventory);
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
        Inventory topInventory = player.getOpenInventory().getTopInventory();
        if (!isTrackedStorageInventory(topInventory, player, access)) {
            openInventories.remove(player.getUniqueId());
            return;
        }

        StorageProfile profile = storageManager.getStorage(player, access.profileId, access.storageType);
        if (profile == null || profile.isLocked()) {
            event.setCancelled(true);
            return;
        }
        boolean touchesTopInventory = false;

        // Check if any slots are outside storage bounds
        for (int slot : event.getRawSlots()) {
            if (slot >= topInventory.getSize()) {
                continue;
            }
            touchesTopInventory = true;
            if (slot >= profile.getCurrentSlots()) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Cannot place items beyond storage capacity!");
                return;
            }
        }

        if (touchesTopInventory && !isAllowedInStorage(access.storageType, event.getOldCursor())) {
            event.setCancelled(true);
            sendRestrictionMessage(player, access.storageType);
            return;
        }

        // Update profile contents after the drag.
        scheduleProfileSync(player, access, profile, topInventory);
    }

    /**
     * Handle inventory close - save contents.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        UUID playerId = player.getUniqueId();

        StorageAccess access = openInventories.remove(playerId);
        pendingProfileSyncs.remove(playerId);
        if (access == null) {
            return;
        }
        if (!isTrackedStorageInventory(event.getInventory(), player, access)) {
            return;
        }

        StorageProfile profile = storageManager.getStorage(player, access.profileId, access.storageType);
        if (profile != null) {
            // Save contents
            profile.loadFromInventory(event.getInventory());
            ejectDisallowedItems(player, profile, access.storageType);
            profile.updateLastAccessed();
            storageManager.markDirty(playerId);

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

    private void scheduleProfileSync(Player player, StorageAccess access, StorageProfile profile, Inventory trackedInventory) {
        if (player == null || access == null || profile == null || trackedInventory == null) {
            return;
        }

        UUID playerId = player.getUniqueId();
        if (!pendingProfileSyncs.add(playerId)) {
            return;
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            try {
                if (!player.isOnline()) {
                    return;
                }

                StorageAccess currentAccess = openInventories.get(playerId);
                if (currentAccess == null) {
                    return;
                }
                if (currentAccess.storageType != access.storageType || !Objects.equals(currentAccess.profileId, access.profileId)) {
                    return;
                }

                Inventory currentTop = player.getOpenInventory().getTopInventory();
                if (currentTop == null || currentTop != trackedInventory) {
                    return;
                }
                if (!isTrackedStorageInventory(currentTop, player, currentAccess)) {
                    openInventories.remove(playerId);
                    return;
                }

                profile.loadFromInventory(currentTop);
                storageManager.markDirty(playerId);
            } finally {
                pendingProfileSyncs.remove(playerId);
            }
        }, 1L);
    }

    private ItemStack incomingItem(Player player, InventoryClickEvent event) {
        InventoryAction action = event.getAction();
        if (action == null) {
            return null;
        }

        return switch (action) {
            case PLACE_ALL, PLACE_ONE, PLACE_SOME, SWAP_WITH_CURSOR -> event.getCursor();
            case HOTBAR_SWAP, HOTBAR_MOVE_AND_READD -> {
                int button = event.getHotbarButton();
                if (button < 0 || button >= player.getInventory().getSize()) {
                    yield null;
                }
                yield player.getInventory().getItem(button);
            }
            default -> null;
        };
    }

    private boolean isAllowedInStorage(StorageType type, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return true;
        }
        return switch (type) {
            case ACCESSORY_BAG -> plugin.getAccessoryManager() != null
                    && item.getAmount() == 1
                    && plugin.getAccessoryManager().isAccessory(item);
            case POTION_BAG -> isPotion(item.getType());
            default -> true;
        };
    }

    private boolean isPotion(Material material) {
        return material == Material.POTION
                || material == Material.SPLASH_POTION
                || material == Material.LINGERING_POTION;
    }

    private void ejectDisallowedItems(Player player, StorageProfile profile, StorageType type) {
        if (profile == null || profile.getContents().isEmpty()) {
            return;
        }

        Map<Integer, ItemStack> removed = new HashMap<>();
        profile.getContents().entrySet().removeIf(entry -> {
            if (isAllowedInStorage(type, entry.getValue())) {
                return false;
            }
            removed.put(entry.getKey(), entry.getValue());
            return true;
        });

        if (removed.isEmpty()) {
            return;
        }

        for (ItemStack item : removed.values()) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item.clone());
            for (ItemStack leftover : leftovers.values()) {
                if (leftover != null && !leftover.getType().isAir()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
            }
        }

        sendRestrictionMessage(player, type);
    }

    private void sendRestrictionMessage(Player player, StorageType type) {
        if (type == StorageType.ACCESSORY_BAG) {
            player.sendMessage(ChatColor.RED + "Accessory Bag only accepts accessory items with stack size 1.");
            return;
        }
        if (type == StorageType.POTION_BAG) {
            player.sendMessage(ChatColor.RED + "Only potion items can be stored in the Potion Bag.");
        }
    }

    private boolean isTrackedStorageInventory(Inventory inventory, Player player, StorageAccess access) {
        if (inventory == null || player == null || access == null) {
            return false;
        }

        InventoryHolder holder = inventory.getHolder();
        if (!(holder instanceof StorageProfile.StorageInventoryHolder storageHolder)) {
            return false;
        }
        if (!player.getUniqueId().equals(storageHolder.ownerId())) {
            return false;
        }
        if (storageHolder.storageType() != access.storageType) {
            return false;
        }
        return Objects.equals(access.profileId, storageHolder.profileId());
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

