package io.papermc.Grivience.wardrobe;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class WardrobeGui implements Listener {
    private static final String TITLE = ChatColor.DARK_AQUA + "Wardrobe";
    private final WardrobeManager manager;
    private final GriviencePlugin plugin;

    public WardrobeGui(GriviencePlugin plugin, WardrobeManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(new Holder(), 54, TITLE);
        List<ItemStack[]> slots = manager.getSlots(player);
        for (int i = 0; i < 18; i++) {
            ItemStack display = buildSlotItem(player, slots, i);
            inv.setItem(slotIndexToInventorySlot(i), display);
        }
        inv.setItem(49, actionItem(Material.BARRIER, ChatColor.RED + "Close", List.of(ChatColor.GRAY + "Click to close")));
        player.openInventory(inv);
    }

    private ItemStack buildSlotItem(Player player, List<ItemStack[]> slots, int slot) {
        ItemStack[] armor = slot < slots.size() ? slots.get(slot) : null;
        boolean hasSet = armor != null;
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.YELLOW + "Left-click: Equip");
        lore.add(ChatColor.YELLOW + "Right-click: Save current armor");
        if (hasSet) {
            lore.add("");
            lore.add(ChatColor.GRAY + "Contains armor set.");
        } else {
            lore.add("");
            lore.add(ChatColor.GRAY + "Empty slot.");
        }
        ItemStack item = new ItemStack(hasSet ? Material.ARMOR_STAND : Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Wardrobe Slot " + (slot + 1));
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    private int slotIndexToInventorySlot(int slot) {
        // lay out 6 columns x 3 rows starting row 2
        int row = slot / 6;
        int col = slot % 6;
        return 10 + col + row * 9;
    }

    private ItemStack actionItem(Material type, String name, List<String> lore) {
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        int rawSlot = event.getRawSlot();
        if (rawSlot == 49) {
            player.closeInventory();
            return;
        }
        int slotIndex = inventorySlotToSlotIndex(rawSlot);
        if (slotIndex < 0 || slotIndex >= 18) {
            return;
        }

        if (event.isLeftClick()) {
            equipSlot(player, slotIndex);
        } else if (event.isRightClick()) {
            saveSlot(player, slotIndex);
        }
        Bukkit.getScheduler().runTask(plugin, () -> open(player));
    }

    private int inventorySlotToSlotIndex(int raw) {
        int row = raw / 9;
        int col = raw % 9;
        if (row < 1 || row > 3) return -1;
        if (col < 1 || col > 6) return -1;
        return (row - 1) * 6 + (col - 1);
    }

    private void equipSlot(Player player, int slot) {
        ItemStack[] armor = manager.slot(player, slot);
        if (armor == null) {
            player.sendMessage(ChatColor.RED + "This wardrobe slot is empty.");
            return;
        }
        player.getInventory().setArmorContents(armor);
        player.sendMessage(ChatColor.GREEN + "Equipped wardrobe slot " + (slot + 1) + ".");
    }

    private void saveSlot(Player player, int slot) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        manager.saveSlot(player, slot, armor);
        player.sendMessage(ChatColor.YELLOW + "Saved current armor to slot " + (slot + 1) + ".");
    }

    private static final class Holder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
