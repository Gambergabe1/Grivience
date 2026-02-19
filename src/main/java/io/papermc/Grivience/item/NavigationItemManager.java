package io.papermc.Grivience.item;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.gui.SkyblockMenuManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class NavigationItemManager implements Listener {
    private static final int NAVIGATION_SLOT = 8; // Slot 9 (0-indexed)
    private static final String NAVIGATION_ITEM_NAME = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "SkyBlock Navigator";
    private static final List<String> NAVIGATION_ITEM_LORE = List.of(
            ChatColor.GRAY + "Right-click to open the",
            ChatColor.GRAY + "SkyBlock menu and manage",
            ChatColor.GRAY + "your island.",
            "",
            ChatColor.GREEN + "Right-Click to Open"
    );

    private final GriviencePlugin plugin;
    private final SkyblockMenuManager skyblockMenuManager;
    private final NamespacedKey navigationKey;

    public NavigationItemManager(GriviencePlugin plugin, SkyblockMenuManager skyblockMenuManager) {
        this.plugin = plugin;
        this.skyblockMenuManager = skyblockMenuManager;
        this.navigationKey = new NamespacedKey(plugin, "navigation_item");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        giveNavigationItem(player);
    }

    public void giveNavigationItem(Player player) {
        if (!plugin.getConfig().getBoolean("navigation-item.enabled", true)) {
            return;
        }

        ItemStack navigationItem = createNavigationItem();
        player.getInventory().setItem(NAVIGATION_SLOT, navigationItem);
    }

    public ItemStack createNavigationItem() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(NAVIGATION_ITEM_NAME);
        meta.setLore(NAVIGATION_ITEM_LORE);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.getPersistentDataContainer().set(navigationKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!plugin.getConfig().getBoolean("navigation-item.enabled", true)) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta.getPersistentDataContainer().has(navigationKey, PersistentDataType.BYTE)) {
            event.setCancelled(true);
            openNavigationMenu(player);
        }
    }

    private void openNavigationMenu(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
        skyblockMenuManager.openMainMenu(player);
    }

    public boolean isNavigationItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(navigationKey, PersistentDataType.BYTE);
    }
}
