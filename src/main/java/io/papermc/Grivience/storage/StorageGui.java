package io.papermc.Grivience.storage;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.gui.SkyblockGui;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI Manager for the storage system.
 * Provides Skyblock-style storage menus and interfaces.
 */
public class StorageGui {
    private static final int[] STORAGE_TYPE_SLOTS = {10, 12, 14, 16, 28};
    private static final int BACK_SLOT = 48;
    private static final int CLOSE_SLOT = 49;
    private static final int STATUS_SLOT = 50;

    private final GriviencePlugin plugin;
    private final StorageManager storageManager;
    private StorageListener storageListener;
    private final NamespacedKey actionKey;
    private final NamespacedKey valueKey;

    public StorageGui(GriviencePlugin plugin, StorageManager storageManager) {
        this.plugin = plugin;
        this.storageManager = storageManager;
        this.actionKey = new NamespacedKey(plugin, "storage-action");
        this.valueKey = new NamespacedKey(plugin, "storage-value");
    }

    /**
     * Set the storage listener for tracking storage inventory interactions.
     */
    public void setStorageListener(StorageListener listener) {
        this.storageListener = listener;
    }

    /**
     * Open the main storage menu for a player.
     */
    public void openMainMenu(Player player) {
        Inventory gui = Bukkit.createInventory(new StorageMenuHolder(StorageMenuHolder.MenuType.MAIN, null), 54, SkyblockGui.title("Storage"));

        SkyblockGui.fillAll(gui, SkyblockGui.filler(Material.BLACK_STAINED_GLASS_PANE));
        ItemStack border = SkyblockGui.filler(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < gui.getSize(); slot++) {
            boolean top = slot < 9;
            boolean bottom = slot >= gui.getSize() - 9;
            boolean left = slot % 9 == 0;
            boolean right = slot % 9 == 8;
            if (top || bottom || left || right) {
                gui.setItem(slot, border.clone());
            }
        }

        gui.setItem(4, SkyblockGui.button(
                Material.CHEST,
                "§aStorage",
                List.of(
                        "§7Access your storages from anywhere.",
                        "",
                        "§eClick a storage to open.",
                        "§6Shift-Click for upgrades."
                )
        ));

        // Fill with storage type icons
        int slotIndex = 0;
        for (StorageType type : StorageType.values()) {
            if (!player.hasPermission(type.getPermissionNode())) {
                continue;
            }

            StorageProfile profile = storageManager.getStorage(player, type);
            if (profile == null) continue;

            if (slotIndex >= STORAGE_TYPE_SLOTS.length) {
                break;
            }

            ItemStack icon = createStorageIcon(type, profile);
            tag(icon, "open_storage", type.name());
            gui.setItem(STORAGE_TYPE_SLOTS[slotIndex], icon);
            slotIndex++;
        }

        // Add status item
        ItemStack statusItem = createStatusItem(player);
        tag(statusItem, "status", "");
        gui.setItem(STATUS_SLOT, statusItem);

        ItemStack backButton = SkyblockGui.backButton("Skyblock Menu");
        tag(backButton, "back", "");
        gui.setItem(BACK_SLOT, backButton);

        ItemStack closeButton = SkyblockGui.closeButton();
        tag(closeButton, "close", "");
        gui.setItem(CLOSE_SLOT, closeButton);

        player.openInventory(gui);
    }

    /**
     * Open a specific storage for a player.
     */
    public void openStorage(Player player, StorageType type) {
        StorageProfile profile = storageManager.getStorage(player, type);
        if (profile == null) {
            player.sendMessage("§cFailed to open storage.");
            return;
        }

        // Create inventory with profile's current slots
        Inventory storageInventory = profile.createInventory();
        
        // Add back button in the last slot (bottom-right corner)
        int lastSlot = storageInventory.getSize() - 1;
        ItemStack backButton = createBackButton();
        storageInventory.setItem(lastSlot, backButton);
        
        player.openInventory(storageInventory);

        // Track that this player has this storage type open
        if (storageListener != null) {
            storageListener.trackStorageOpen(player, type);
        }
    }

    /**
     * Open the upgrade menu for a storage type.
     */
    public void openUpgradeMenu(Player player, StorageType type) {
        StorageProfile profile = storageManager.getStorage(player, type);
        if (profile == null) {
            player.sendMessage("§cFailed to open upgrade menu.");
            return;
        }

        String title = SkyblockGui.title("Upgrade " + type.getDisplayName());
        Inventory gui = Bukkit.createInventory(new StorageMenuHolder(StorageMenuHolder.MenuType.UPGRADE, type), 27, title);

        SkyblockGui.fillAll(gui, SkyblockGui.filler(Material.BLACK_STAINED_GLASS_PANE));
        ItemStack border = SkyblockGui.filler(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < gui.getSize(); slot++) {
            boolean top = slot < 9;
            boolean bottom = slot >= gui.getSize() - 9;
            boolean left = slot % 9 == 0;
            boolean right = slot % 9 == 8;
            if (top || bottom || left || right) {
                gui.setItem(slot, border.clone());
            }
        }

        // Current tier display
        ItemStack currentTierItem = createCurrentTierItem(profile, type);
        gui.setItem(11, currentTierItem);

        // Next upgrade display
        StorageUpgrade nextUpgrade = storageManager.getNextUpgrade(profile);
        if (nextUpgrade != null) {
            ItemStack nextUpgradeItem = createNextUpgradeItem(nextUpgrade, profile);
            gui.setItem(15, nextUpgradeItem);
        } else {
            ItemStack maxedItem = createMaxedItem();
            gui.setItem(15, maxedItem);
        }

        // Upgrade button
        if (storageManager.canUpgrade(player, profile)) {
            ItemStack upgradeButton = createUpgradeButton(nextUpgrade);
            gui.setItem(13, upgradeButton);
        } else {
            ItemStack lockedButton = createLockedButton();
            gui.setItem(13, lockedButton);
        }

        // Close button
        ItemStack closeButton = createCloseButton();
        gui.setItem(26, closeButton);

        player.openInventory(gui);
    }

    /**
     * Create a storage type icon item.
     */
    private ItemStack createStorageIcon(StorageType type, StorageProfile profile) {
        Material iconMaterial = getIconForStorageType(type);
        ItemStack item = new ItemStack(iconMaterial);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§a" + type.getDisplayName());

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Status: " + (profile.isLocked() ? "§cLocked" : "§aUnlocked"));
        lore.add("§7Slots: §e" + profile.getCurrentSlots() + " §7/ §e" + type.getMaxSlots());
        lore.add("§7Items: §a" + profile.getTotalItems());
        lore.add("§7Tier: §6" + profile.getUpgradeTier());

        if (profile.getCustomName() != null) {
            lore.add("");
            lore.add("§7Custom Name: §f" + profile.getCustomName());
        }

        lore.add("");
        lore.add("§e§lClick to Open");
        lore.add("§6§lShift+Click for Upgrades");

        meta.setLore(lore);
        meta.getPersistentDataContainer().set(valueKey, PersistentDataType.STRING, type.name());
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create a status summary item.
     */
    private ItemStack createStatusItem(Player player) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§6§lStorage Summary");

        // Get player's selected profile name for display
        String profileName = "Current Profile";
        io.papermc.Grivience.skyblock.profile.ProfileManager profileManager = plugin.getProfileManager();
        if (profileManager != null) {
            io.papermc.Grivience.skyblock.profile.SkyBlockProfile profile = profileManager.getSelectedProfile(player);
            if (profile != null) {
                profileName = profile.getProfileName();
            }
        }

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Profile: §e" + profileName);
        lore.add("§7Total Items: §e" + storageManager.getTotalItemsStored(player));
        lore.add("§7Total Capacity: §e" + storageManager.getTotalStorageCapacity(player));
        lore.add("§7Usage: §e" + String.format("%.1f", storageManager.getStorageUsagePercentage(player)) + "%");
        lore.add("");
        lore.add("§7Rank: §e#" + storageManager.getPlayerRank(player.getUniqueId()));
        lore.add("");
        lore.add("§7View detailed status with");
        lore.add("§7§e/storage status");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create current tier display item.
     */
    private ItemStack createCurrentTierItem(StorageProfile profile, StorageType type) {
        ItemStack item = new ItemStack(Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§7Current Tier");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Storage Type: §e" + type.getDisplayName());
        lore.add("§7Current Tier: §6" + profile.getUpgradeTier());
        lore.add("§7Current Slots: §e" + profile.getCurrentSlots());
        lore.add("§7Max Slots: §e" + type.getMaxSlots());
        lore.add("");
        lore.add("§7Items Stored: §a" + profile.getTotalItems());

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create next upgrade display item.
     */
    private ItemStack createNextUpgradeItem(StorageUpgrade upgrade, StorageProfile profile) {
        ItemStack item = new ItemStack(Material.GREEN_DYE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§a§lNext Upgrade");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Upgrade to: §e" + upgrade.getDisplayName());
        lore.add("§7New Slots: §e" + upgrade.getSlots());
        lore.add("§7Slot Increase: §a+" + (upgrade.getSlots() - profile.getCurrentSlots()));
        lore.add("");

        if (upgrade.hasCost()) {
            lore.add("§7Upgrade Cost: §6$" + upgrade.getCost());
        } else {
            lore.add("§7Upgrade Cost: §aFree!");
        }

        lore.add("");
        lore.add("§e§lClick to Upgrade");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create maxed out display item.
     */
    private ItemStack createMaxedItem() {
        ItemStack item = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§6§lMAXED OUT");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7This storage has reached");
        lore.add("§7its maximum capacity!");
        lore.add("");
        lore.add("§eNo further upgrades available");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create upgrade button.
     */
    private ItemStack createUpgradeButton(StorageUpgrade upgrade) {
        ItemStack item = new ItemStack(Material.LIME_WOOL);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§a§lCLICK TO UPGRADE");

        List<String> lore = new ArrayList<>();
        lore.add("");
        if (upgrade != null && upgrade.hasCost()) {
            lore.add("§7Cost: §6$" + upgrade.getCost());
        } else {
            lore.add("§7Free Upgrade!");
        }
        lore.add("");
        lore.add("§e§lClick to confirm upgrade");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create locked button.
     */
    private ItemStack createLockedButton() {
        ItemStack item = new ItemStack(Material.RED_WOOL);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§c§lCannot Upgrade");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Requirements not met");
        lore.add("§7Check permissions or cost");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create close button.
     */
    private ItemStack createCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§cClose");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Click to close menu");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create back button to return to main menu.
     */
    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§aGo Back");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Click to return to");
        lore.add("§7the main storage menu");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void tag(ItemStack item, String action, String value) {
        if (item == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        if (action != null && !action.isBlank()) {
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        }
        meta.getPersistentDataContainer().set(valueKey, PersistentDataType.STRING, value == null ? "" : value);
        item.setItemMeta(meta);
    }

    /**
     * Get the icon material for a storage type.
     */
    private Material getIconForStorageType(StorageType type) {
        return switch (type) {
            case PERSONAL -> Material.CHEST;
            case VAULT -> Material.ENDER_CHEST;
            case ENDER -> Material.ENDER_EYE;
            case BACKPACK -> Material.SHULKER_BOX;
            case WAREHOUSE -> Material.BARREL;
            case ACCESSORY_BAG -> Material.REDSTONE;
            case POTION_BAG -> Material.POTION;
        };
    }

    /**
     * Play upgrade success sound.
     */
    public void playUpgradeSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }

    /**
     * Play error sound.
     */
    public void playErrorSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
    }

    /**
     * Inventory holder for storage menus.
     */
    static final class StorageMenuHolder implements InventoryHolder {
        enum MenuType {
            MAIN,
            UPGRADE
        }

        private final MenuType menuType;
        private final StorageType storageType;

        StorageMenuHolder(MenuType menuType, StorageType storageType) {
            this.menuType = menuType;
            this.storageType = storageType;
        }

        MenuType getMenuType() {
            return menuType;
        }

        StorageType getStorageType() {
            return storageType;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
