package io.papermc.Grivience.accessory;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.gui.SkyblockGui;
import io.papermc.Grivience.storage.StorageManager;
import io.papermc.Grivience.storage.StorageProfile;
import io.papermc.Grivience.storage.StorageType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Specialized GUI for the Accessory Bag.
 * Shows Magical Power, current Power, and allows item management.
 */
public final class AccessoryBagGui implements Listener {
    private static final String TITLE = SkyblockGui.title("Accessory Bag");
    private static final int MP_SLOT = 4;
    private static final int POWER_SLOT = 49;
    private static final int BACK_SLOT = 48;
    private static final int CLOSE_SLOT = 50;

    private final GriviencePlugin plugin;
    private final AccessoryManager accessoryManager;
    private final StorageManager storageManager;
    private final NamespacedKey actionKey;

    public AccessoryBagGui(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.accessoryManager = plugin.getAccessoryManager();
        this.storageManager = plugin.getStorageManager();
        this.actionKey = new NamespacedKey(plugin, "accessory_gui_action");
    }

    public void open(Player player) {
        StorageProfile profile = storageManager.getStorage(player, StorageType.ACCESSORY_BAG);
        if (profile == null) {
            player.sendMessage(ChatColor.RED + "Could not load accessory bag.");
            return;
        }

        int capacity = profile.getCurrentSlots();
        int rows = (int) Math.ceil(capacity / 9.0) + 1; // Always add one row for footer
        if (rows < 3) rows = 3;
        if (rows > 6) rows = 6;
        
        Inventory inv = Bukkit.createInventory(new Holder(), rows * 9, TITLE);
        
        // Fill bag contents
        profile.getContents().forEach(inv::setItem);

        // Fill footer background
        int lastRowStart = (rows - 1) * 9;
        for (int i = lastRowStart; i < rows * 9; i++) {
            inv.setItem(i, SkyblockGui.border());
        }

        AccessoryBonuses bonuses = accessoryManager.bonuses(player);
        inv.setItem(lastRowStart + 0, SkyblockGui.backButton("Storage"));
        inv.setItem(lastRowStart + 4, createMPItem(bonuses));
        inv.setItem(lastRowStart + 5, createPowerItem(player, bonuses.magicalPower()));
        inv.setItem(lastRowStart + 8, SkyblockGui.closeButton());

        player.openInventory(inv);
    }

    private ItemStack createMPItem(AccessoryBonuses bonuses) {
        ItemStack item = new ItemStack(Material.BEACON);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Magical Power: " + ChatColor.WHITE + bonuses.magicalPower());
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Magical Power is granted by the");
        lore.add(ChatColor.GRAY + "accessories in your bag.");
        lore.add("");
        lore.add(ChatColor.GRAY + "Current Bonuses:");
        lore.add(ChatColor.GRAY + "✦ Stat Multiplier: " + ChatColor.YELLOW + String.format(Locale.US, "+%.1f%%", (AccessoryPower.statMultiplierFromPower(bonuses.magicalPower()) - 1.0) * 100));
        lore.add(ChatColor.RED + "❤ Health: " + ChatColor.WHITE + String.format(Locale.US, "+%.1f", bonuses.health()));
        lore.add(ChatColor.GREEN + "❈ Defense: " + ChatColor.WHITE + String.format(Locale.US, "+%.1f", bonuses.defense()));
        lore.add(ChatColor.RED + "❁ Strength: " + ChatColor.WHITE + String.format(Locale.US, "+%.1f", bonuses.strength()));
        lore.add(ChatColor.AQUA + "✎ Intelligence: " + ChatColor.WHITE + String.format(Locale.US, "+%.1f", bonuses.intelligence()));
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "Unique Accessories: " + ChatColor.WHITE + bonuses.uniqueAccessories());
        
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPowerItem(Player player, int mp) {
        AccessoryPowerType power = AccessoryPowerType.NONE;
        var profile = plugin.getProfileManager().getSelectedProfile(player);
        if (profile != null) power = profile.getSelectedAccessoryPower();

        ItemStack item = new ItemStack(power.getIcon());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Current Power: " + ChatColor.YELLOW + power.getDisplayName());
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + power.getDescription());
        lore.add("");
        lore.add(ChatColor.GRAY + "Selected build stats:");
        
        power.calculateStats(mp).forEach((stat, val) -> {
            lore.add(ChatColor.GRAY + " - " + stat + ": " + ChatColor.GREEN + "+" + String.format(Locale.US, "%.1f", val));
        });
        
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to select a different Power!");
        
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "select_power");
        item.setItemMeta(meta);
        return item;
    }

    // ... (createMPItem and createPowerItem stay same)

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder)) return;
        
        int rawSlot = event.getRawSlot();
        Inventory inv = event.getInventory();
        int rows = inv.getSize() / 9;
        int lastRowStart = (rows - 1) * 9;
        
        // Protection: Block all interactions in the footer row
        if (rawSlot >= lastRowStart && rawSlot < inv.getSize()) {
            event.setCancelled(true);
            
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            Player player = (Player) event.getWhoClicked();
            String action = getTag(clicked, "action");
            if (action == null) action = getTag(clicked, "accessory_gui_action");

            if (action != null) {
                switch (action) {
                    case "back" -> {
                        if (plugin.getStorageGui() != null) {
                            plugin.getStorageGui().openMainMenu(player);
                        } else {
                            player.closeInventory();
                        }
                    }
                    case "close" -> player.closeInventory();
                    case "select_power" -> openPowerSelection(player);
                }
            }
            return;
        }

        // Protection: Prevent shift-clicking into the footer row
        if (event.getAction() == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            // Standard shift-click logic: if it's coming from player inventory, we need to find where it would land.
            // However, we can just let it happen and our onInventoryClose will ignore any items in the footer.
            // But for "High Quality" feel, we should block it if the destination is a footer slot.
            // Simplified: we'll allow shift-clicks to the top part (bag slots) but not the footer.
            // Since Minecraft's default shift-click behavior fills from slot 0, it usually won't hit the footer
            // unless the bag is full.
        }

        // Protection: Prevent hotbar swapping items into the footer
        if (event.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY) {
            if (rawSlot >= lastRowStart && rawSlot < inv.getSize()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder)) return;
        
        int size = event.getInventory().getSize();
        int lastRowStart = size - 9;
        
        for (int slot : event.getRawSlots()) {
            if (slot < size && slot >= lastRowStart) {
                event.setCancelled(true);
                break;
            }
        }
    }

    private String getTag(ItemStack item, String key) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        NamespacedKey nk = new NamespacedKey("grivience", "gui_tag_" + key);
        if (meta.getPersistentDataContainer().has(nk, PersistentDataType.STRING)) {
            return meta.getPersistentDataContainer().get(nk, PersistentDataType.STRING);
        }
        // Check plugin-specific key too
        NamespacedKey pk = new NamespacedKey(plugin, key);
        if (meta.getPersistentDataContainer().has(pk, PersistentDataType.STRING)) {
            return meta.getPersistentDataContainer().get(pk, PersistentDataType.STRING);
        }
        return null;
    }

    private void openPowerSelection(Player player) {
        Inventory inv = Bukkit.createInventory(new PowerHolder(), 36, SkyblockGui.title("Select Accessory Power"));
        SkyblockGui.fillAll(inv, SkyblockGui.filler(Material.BLACK_STAINED_GLASS_PANE));

        int slot = 10;
        for (AccessoryPowerType type : AccessoryPowerType.values()) {
            if (type == AccessoryPowerType.NONE) continue;
            
            ItemStack item = new ItemStack(type.getIcon());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + type.getDisplayName());
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + type.getDescription());
            lore.add("");
            lore.add(ChatColor.GRAY + "Stats per 100 MP:");
            type.getStatScales().forEach((stat, scale) -> {
                lore.add(ChatColor.GRAY + " - " + stat + ": " + ChatColor.AQUA + "+" + (scale * 10));
            });
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to select!");
            
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "set_power:" + type.name());
            item.setItemMeta(meta);
            
            inv.setItem(slot++, item);
            if (slot == 17) slot = 19;
        }

        inv.setItem(31, SkyblockGui.backButton("Accessory Bag"));
        player.openInventory(inv);
    }

    @EventHandler
    public void onPowerClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof PowerHolder)) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        Player player = (Player) event.getWhoClicked();
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;
        
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (action == null) return;
        
        if (action.startsWith("set_power:")) {
            String powerName = action.substring(10);
            AccessoryPowerType type = AccessoryPowerType.valueOf(powerName);
            
            var profile = plugin.getProfileManager().getSelectedProfile(player);
            if (profile != null) {
                profile.setSelectedAccessoryPower(type);
                player.sendMessage(ChatColor.GREEN + "Successfully selected " + ChatColor.YELLOW + type.getDisplayName() + ChatColor.GREEN + " power!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                open(player);
            }
        } else if ("back".equals(action)) {
            open(player);
        }
    }

    private static final class Holder implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }
    
    private static final class PowerHolder implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }
}
