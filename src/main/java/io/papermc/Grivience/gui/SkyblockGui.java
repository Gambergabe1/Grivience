package io.papermc.Grivience.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Small helpers for making inventories look like Skyblock menus.
 * Keeps styling consistent across the plugin without changing click behavior.
 */
public final class SkyblockGui {
    private SkyblockGui() {
    }

    public static String title(String name) {
        if (name == null || name.isBlank()) {
            return ChatColor.DARK_GRAY.toString();
        }
        return ChatColor.DARK_GRAY + name;
    }

    public static ItemStack filler(Material material) {
        ItemStack item = new ItemStack(material == null ? Material.BLACK_STAINED_GLASS_PANE : material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            meta.setLore(List.of());
            meta.addItemFlags(
                    ItemFlag.HIDE_ATTRIBUTES,
                    ItemFlag.HIDE_ENCHANTS,
                    ItemFlag.HIDE_UNBREAKABLE
            );
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void fillAll(Inventory inventory, ItemStack item) {
        if (inventory == null || item == null) {
            return;
        }
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, item.clone());
        }
    }

    public static void fillEmpty(Inventory inventory, ItemStack item) {
        if (inventory == null || item == null) {
            return;
        }
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, item.clone());
            }
        }
    }

    public static ItemStack closeButton() {
        return taggedItem(
                Material.BARRIER,
                ChatColor.RED + "Close",
                List.of(ChatColor.GRAY + "Close this menu."),
                "action", "close"
        );
    }

    public static ItemStack backButton(String destination) {
        String to = destination == null || destination.isBlank() ? "previous menu" : destination;
        return taggedItem(
                Material.ARROW,
                ChatColor.GREEN + "Go Back",
                List.of(ChatColor.GRAY + "To " + to + "."),
                "action", "back"
        );
    }

    public static ItemStack border() {
        return filler(Material.GRAY_STAINED_GLASS_PANE);
    }

    public static ItemStack taggedItem(Material material, String name, List<String> lore, String tagKey, String tagValue) {
        ItemStack item = button(material, name, lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey("grivience", "gui_tag_" + tagKey),
                org.bukkit.persistence.PersistentDataType.STRING,
                tagValue
            );
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack button(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material == null ? Material.BARRIER : material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name == null ? " " : name);
            meta.setLore(lore == null ? List.of() : List.copyOf(lore));
            meta.addItemFlags(
                    ItemFlag.HIDE_ATTRIBUTES,
                    ItemFlag.HIDE_ENCHANTS,
                    ItemFlag.HIDE_UNBREAKABLE
            );
            item.setItemMeta(meta);
        }
        return item;
    }
}

