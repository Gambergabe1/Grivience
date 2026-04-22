package io.papermc.Grivience.storage;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.gui.SkyblockGui;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * GUI manager for the storage system.
 * Provides the main storage browser, per-storage inventory access, and upgrade menus.
 */
public class StorageGui {
    private static final int[] STORAGE_TYPE_SLOTS = {19, 21, 23, 25, 29, 31, 33};
    private static final int HEADER_SLOT = 4;
    private static final int PROFILE_SLOT = 13;
    private static final int BACK_SLOT = 48;
    private static final int CLOSE_SLOT = 49;
    private static final int STATUS_SLOT = 50;
    private static final int UPGRADE_ACTION_SLOT = 13;
    private static final int UPGRADE_BACK_SLOT = 18;
    private static final int UPGRADE_CLOSE_SLOT = 26;

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
        decorateFrame(gui);

        gui.setItem(HEADER_SLOT, createMainHeader());
        gui.setItem(PROFILE_SLOT, createProfileCard(player));

        StorageType[] types = StorageType.values();
        for (int i = 0; i < types.length && i < STORAGE_TYPE_SLOTS.length; i++) {
            StorageType type = types[i];
            StorageProfile profile = storageManager.getStorage(player, type);
            boolean hasPermission = player.hasPermission(type.getPermissionNode());
            ItemStack icon = createStorageIcon(type, profile, hasPermission);
            tag(icon, "open_storage", type.name());
            gui.setItem(STORAGE_TYPE_SLOTS[i], icon);
        }

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
        if (type == StorageType.ACCESSORY_BAG) {
            plugin.getAccessoryBagGui().open(player);
            return;
        }
        
        StorageProfile profile = storageManager.getStorage(player, type);
        if (profile == null) {
            player.sendMessage(ChatColor.RED + "Failed to open storage.");
            return;
        }

        Inventory storageInventory = profile.createInventory();
        int lastSlot = storageInventory.getSize() - 1;
        storageInventory.setItem(lastSlot, createBackButton());

        player.openInventory(storageInventory);

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
            player.sendMessage(ChatColor.RED + "Failed to open upgrade menu.");
            return;
        }

        String title = SkyblockGui.title("Upgrade " + type.getDisplayName());
        Inventory gui = Bukkit.createInventory(new StorageMenuHolder(StorageMenuHolder.MenuType.UPGRADE, type), 27, title);
        decorateFrame(gui);

        StorageUpgrade nextUpgrade = storageManager.getNextUpgrade(profile);
        gui.setItem(4, createUpgradeHeader(type, profile));
        gui.setItem(11, createCurrentTierItem(profile, type));
        gui.setItem(15, nextUpgrade == null ? createMaxedItem() : createNextUpgradeItem(nextUpgrade, profile));

        if (storageManager.canUpgrade(player, profile) && nextUpgrade != null) {
            gui.setItem(UPGRADE_ACTION_SLOT, createUpgradeButton(nextUpgrade));
        } else {
            gui.setItem(UPGRADE_ACTION_SLOT, createLockedButton(player, profile, nextUpgrade));
        }

        gui.setItem(UPGRADE_BACK_SLOT, SkyblockGui.backButton("Storage"));
        gui.setItem(UPGRADE_CLOSE_SLOT, SkyblockGui.closeButton());

        player.openInventory(gui);
    }

    private void decorateFrame(Inventory gui) {
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
    }

    private ItemStack createMainHeader() {
        return SkyblockGui.button(
                Material.ENDER_CHEST,
                ChatColor.GREEN + "Storage",
                List.of(
                        ChatColor.GRAY + "Browse and open all storage types.",
                        "",
                        ChatColor.YELLOW + "Click a storage to open it.",
                        ChatColor.GOLD + "Shift-Click to open upgrades."
                )
        );
    }

    private ItemStack createProfileCard(Player player) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof SkullMeta skullMeta)) {
            return item;
        }

        io.papermc.Grivience.skyblock.profile.ProfileManager profileManager = plugin.getProfileManager();
        io.papermc.Grivience.skyblock.profile.SkyBlockProfile selected = profileManager == null ? null : profileManager.getSelectedProfile(player);
        String profileName = selected == null ? "Current Profile" : selected.getProfileName();

        skullMeta.setOwningPlayer(player);
        skullMeta.setDisplayName(ChatColor.AQUA + "Profile Context");
        skullMeta.setLore(List.of(
                ChatColor.GRAY + "Profile: " + ChatColor.YELLOW + profileName,
                ChatColor.GRAY + "Unlocked Storages: " + ChatColor.GREEN + countUnlockedStorages(player) + ChatColor.DARK_GRAY + "/" + ChatColor.GREEN + StorageType.values().length
        ));
        skullMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(skullMeta);
        return item;
    }

    /**
     * Create a storage type icon item.
     */
    private ItemStack createStorageIcon(StorageType type, StorageProfile profile, boolean hasPermission) {
        boolean available = hasPermission && profile != null && !profile.isLocked();
        Material iconMaterial = hasPermission ? getIconForStorageType(type) : Material.GRAY_DYE;
        ItemStack item = new ItemStack(iconMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        ChatColor nameColor = available ? ChatColor.GREEN : (hasPermission ? ChatColor.RED : ChatColor.DARK_GRAY);
        meta.setDisplayName(nameColor + type.getDisplayName());

        int usedSlots = profile == null ? 0 : profile.getTotalItems();
        int currentSlots = profile == null ? type.getBaseSlots() : profile.getCurrentSlots();
        int maxSlots = type.getMaxSlots();
        int tier = profile == null ? 0 : profile.getUpgradeTier();

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Status: " + (available ? ChatColor.GREEN + "Ready" : ChatColor.RED + "Locked"));
        lore.add(ChatColor.GRAY + "Capacity: " + ChatColor.YELLOW + currentSlots + ChatColor.DARK_GRAY + "/" + ChatColor.YELLOW + maxSlots);
        lore.add(ChatColor.GRAY + "Stored Stacks: " + ChatColor.AQUA + usedSlots);
        lore.add(ChatColor.GRAY + "Tier: " + ChatColor.GOLD + tier);
        lore.add(ChatColor.GRAY + "Usage: " + usageBar(usedSlots, currentSlots));

        if (profile != null && profile.getCustomName() != null && !profile.getCustomName().isBlank()) {
            lore.add(ChatColor.GRAY + "Alias: " + ChatColor.WHITE + profile.getCustomName());
        }

        lore.add("");
        if (!hasPermission) {
            lore.add(ChatColor.RED + "Requires permission:");
            lore.add(ChatColor.DARK_GRAY + type.getPermissionNode());
        } else if (profile != null && profile.isLocked()) {
            lore.add(ChatColor.RED + "This storage is currently locked.");
        } else {
            lore.add(ChatColor.YELLOW + "Click to Open");
            lore.add(ChatColor.GOLD + "Shift-Click for Upgrades");
        }

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        if (available) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        }
        meta.getPersistentDataContainer().set(valueKey, PersistentDataType.STRING, type.name());
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create a status summary item.
     */
    private ItemStack createStatusItem(Player player) {
        ItemStack item = new ItemStack(Material.COMPARATOR);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        int totalItems = storageManager.getTotalItemsStored(player);
        int totalCapacity = storageManager.getTotalStorageCapacity(player);
        double usage = storageManager.getStorageUsagePercentage(player);
        int rank = storageManager.getPlayerRank(player.getUniqueId());

        String profileName = "Current Profile";
        io.papermc.Grivience.skyblock.profile.ProfileManager profileManager = plugin.getProfileManager();
        if (profileManager != null) {
            io.papermc.Grivience.skyblock.profile.SkyBlockProfile profile = profileManager.getSelectedProfile(player);
            if (profile != null) {
                profileName = profile.getProfileName();
            }
        }

        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Storage Overview");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Profile: " + ChatColor.YELLOW + profileName);
        lore.add(ChatColor.GRAY + "Items Stored: " + ChatColor.AQUA + totalItems);
        lore.add(ChatColor.GRAY + "Total Capacity: " + ChatColor.YELLOW + totalCapacity);
        lore.add(ChatColor.GRAY + "Usage: " + usageBar(totalItems, Math.max(1, totalCapacity)));
        lore.add(ChatColor.GRAY + "Leaderboard Rank: " + (rank > 0 ? ChatColor.GOLD + "#" + rank : ChatColor.DARK_GRAY + "Unranked"));
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to run " + ChatColor.WHITE + "/storage status");
        lore.add(ChatColor.DARK_GRAY + "Usage exact: " + String.format(Locale.US, "%.1f%%", usage));

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createUpgradeHeader(StorageType type, StorageProfile profile) {
        ItemStack item = new ItemStack(getIconForStorageType(type));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(ChatColor.GREEN + type.getDisplayName() + ChatColor.GRAY + " Upgrade");
        meta.setLore(List.of(
                ChatColor.GRAY + "Current Slots: " + ChatColor.YELLOW + profile.getCurrentSlots(),
                ChatColor.GRAY + "Max Slots: " + ChatColor.YELLOW + type.getMaxSlots(),
                ChatColor.GRAY + "Current Tier: " + ChatColor.GOLD + profile.getUpgradeTier()
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create current tier display item.
     */
    private ItemStack createCurrentTierItem(StorageProfile profile, StorageType type) {
        ItemStack item = new ItemStack(Material.CHEST_MINECART);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(ChatColor.GRAY + "Current Tier");
        meta.setLore(List.of(
                ChatColor.GRAY + "Storage Type: " + ChatColor.YELLOW + type.getDisplayName(),
                ChatColor.GRAY + "Tier: " + ChatColor.GOLD + profile.getUpgradeTier(),
                ChatColor.GRAY + "Slots: " + ChatColor.YELLOW + profile.getCurrentSlots() + ChatColor.DARK_GRAY + "/" + ChatColor.YELLOW + type.getMaxSlots(),
                ChatColor.GRAY + "Stored Stacks: " + ChatColor.AQUA + profile.getTotalItems()
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create next upgrade display item.
     */
    private ItemStack createNextUpgradeItem(StorageUpgrade upgrade, StorageProfile profile) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Next Upgrade");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Tier Name: " + ChatColor.YELLOW + ChatColor.stripColor(upgrade.getDisplayName()));
        lore.add(ChatColor.GRAY + "New Slots: " + ChatColor.YELLOW + upgrade.getSlots());
        lore.add(ChatColor.GRAY + "Increase: " + ChatColor.GREEN + "+" + Math.max(0, upgrade.getSlots() - profile.getCurrentSlots()));
        lore.add("");
        if (upgrade.hasCost()) {
            lore.add(ChatColor.GRAY + "Cost: " + ChatColor.GOLD + formatNumber(upgrade.getCost()) + " coins");
        } else {
            lore.add(ChatColor.GRAY + "Cost: " + ChatColor.GREEN + "Free");
        }
        lore.add("");
        lore.add(ChatColor.YELLOW + "Use the center button to upgrade.");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create maxed out display item.
     */
    private ItemStack createMaxedItem() {
        ItemStack item = new ItemStack(Material.BEACON);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Max Tier Reached");
        meta.setLore(List.of(
                ChatColor.GRAY + "This storage is fully upgraded.",
                ChatColor.YELLOW + "No additional tiers available."
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create upgrade button.
     */
    private ItemStack createUpgradeButton(StorageUpgrade upgrade) {
        ItemStack item = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Upgrade Storage");
        List<String> lore = new ArrayList<>();
        if (upgrade != null && upgrade.hasCost()) {
            lore.add(ChatColor.GRAY + "Cost: " + ChatColor.GOLD + formatNumber(upgrade.getCost()) + " coins");
        } else {
            lore.add(ChatColor.GRAY + "Cost: " + ChatColor.GREEN + "Free");
        }
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to confirm upgrade.");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create locked button.
     */
    private ItemStack createLockedButton(Player player, StorageProfile profile, StorageUpgrade nextUpgrade) {
        ItemStack item = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Cannot Upgrade");
        List<String> lore = new ArrayList<>();
        if (nextUpgrade == null || profile.getCurrentSlots() >= profile.getStorageType().getMaxSlots()) {
            lore.add(ChatColor.GRAY + "This storage is already maxed.");
        } else if (!player.hasPermission(profile.getStorageType().getPermissionNode() + ".upgrade")) {
            lore.add(ChatColor.GRAY + "Missing permission:");
            lore.add(ChatColor.DARK_GRAY + profile.getStorageType().getPermissionNode() + ".upgrade");
        } else {
            lore.add(ChatColor.GRAY + "Requirements not met.");
            lore.add(ChatColor.DARK_GRAY + "Check coins or profile state.");
        }
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create back button to return to main menu.
     */
    private ItemStack createBackButton() {
        return SkyblockGui.button(
                Material.ARROW,
                ChatColor.GREEN + "Go Back",
                List.of(ChatColor.GRAY + "Return to the Storage menu.")
        );
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

    private int countUnlockedStorages(Player player) {
        int count = 0;
        for (StorageType type : StorageType.values()) {
            if (player.hasPermission(type.getPermissionNode())) {
                count++;
            }
        }
        return count;
    }

    private String usageBar(int used, int capacity) {
        int safeCapacity = Math.max(1, capacity);
        double percent = Math.max(0.0D, Math.min(100.0D, (used * 100.0D) / safeCapacity));
        int segments = 10;
        int filled = (int) Math.round((percent / 100.0D) * segments);
        if (filled < 0) {
            filled = 0;
        }
        if (filled > segments) {
            filled = segments;
        }

        String full = "|".repeat(filled);
        String empty = "|".repeat(Math.max(0, segments - filled));
        return ChatColor.GREEN + full + ChatColor.DARK_GRAY + empty + ChatColor.GRAY + " " + ChatColor.YELLOW + String.format(Locale.US, "%.1f%%", percent);
    }

    private String formatNumber(double value) {
        return String.format(Locale.US, "%,.0f", value);
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
            case ACCESSORY_BAG -> Material.TOTEM_OF_UNDYING;
            case POTION_BAG -> Material.BREWING_STAND;
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
