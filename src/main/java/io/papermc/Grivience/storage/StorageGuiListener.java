package io.papermc.Grivience.storage;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * GUI listener for storage system menus.
 * Handles clicks in storage main and upgrade menus.
 */
public class StorageGuiListener implements Listener {
    private static final int UPGRADE_ACTION_SLOT = 13;
    private static final int UPGRADE_BACK_SLOT = 18;
    private static final int UPGRADE_CLOSE_SLOT = 26;

    private final GriviencePlugin plugin;
    private final StorageManager storageManager;
    private final StorageGui storageGui;
    private final NamespacedKey actionKey;
    private final NamespacedKey valueKey;

    public StorageGuiListener(GriviencePlugin plugin, StorageManager storageManager, StorageGui storageGui) {
        this.plugin = plugin;
        this.storageManager = storageManager;
        this.storageGui = storageGui;
        this.actionKey = new NamespacedKey(plugin, "storage-action");
        this.valueKey = new NamespacedKey(plugin, "storage-value");
    }

    /**
     * Handle inventory clicks in storage menus.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof StorageGui.StorageMenuHolder holder)) {
            return;
        }

        event.setCancelled(true);

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= top.getSize()) {
            // Ignore player-inventory slots while keeping menu non-interactive.
            return;
        }

        if (holder.getMenuType() == StorageGui.StorageMenuHolder.MenuType.MAIN) {
            handleMainMenuClick(player, event, event.getCurrentItem());
            return;
        }

        if (holder.getMenuType() == StorageGui.StorageMenuHolder.MenuType.UPGRADE) {
            StorageType storageType = holder.getStorageType();
            if (storageType != null) {
                handleUpgradeMenuClick(player, storageType, rawSlot, event.getCurrentItem());
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() instanceof StorageGui.StorageMenuHolder) {
            event.setCancelled(true);
        }
    }

    /**
     * Handle clicks in the main storage menu.
     */
    private void handleMainMenuClick(Player player, InventoryClickEvent event, ItemStack clickedItem) {
        if (clickedItem == null) {
            return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        String action = meta == null ? null : meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        String value = meta == null ? null : meta.getPersistentDataContainer().get(valueKey, PersistentDataType.STRING);

        if ("close".equals(action)) {
            player.closeInventory();
            return;
        }

        if ("back".equals(action)) {
            player.closeInventory();
            player.performCommand("skyblock menu");
            return;
        }

        if ("status".equals(action)) {
            player.performCommand("storage status");
            return;
        }

        StorageType type = null;
        if (value != null && !value.isBlank()) {
            try {
                type = StorageType.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (type == null) {
            return;
        }

        if (!player.hasPermission(type.getPermissionNode())) {
            storageGui.playErrorSound(player);
            player.sendMessage(ChatColor.RED + "You don't have permission to access this storage.");
            return;
        }

        if (event.getClick().isShiftClick()) {
            storageGui.openUpgradeMenu(player, type);
        } else {
            storageGui.openStorage(player, type);
        }
    }

    /**
     * Handle clicks in the upgrade menu.
     */
    private void handleUpgradeMenuClick(Player player, StorageType type, int slot, ItemStack clickedItem) {
        if (clickedItem == null) {
            return;
        }

        StorageProfile profile = storageManager.getStorage(player, type);
        if (profile == null) {
            return;
        }

        if (slot == UPGRADE_BACK_SLOT || slot == UPGRADE_CLOSE_SLOT) {
            player.closeInventory();
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> plugin.getStorageGui().openMainMenu(player), 1L);
            return;
        }

        if (slot != UPGRADE_ACTION_SLOT) {
            return;
        }

        if (!storageManager.canUpgrade(player, profile)) {
            storageGui.playErrorSound(player);
            player.sendMessage(ChatColor.RED + "Cannot upgrade storage yet.");
            return;
        }

        if (storageManager.upgradeStorage(player, profile)) {
            storageGui.playUpgradeSound(player);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> storageGui.openUpgradeMenu(player, type), 2L);
        } else {
            storageGui.playErrorSound(player);
        }
    }
}
