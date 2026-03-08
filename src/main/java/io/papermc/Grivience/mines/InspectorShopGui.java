package io.papermc.Grivience.mines;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.item.MiningItemType;
import io.papermc.Grivience.skyblock.economy.ProfileEconomyService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class InspectorShopGui implements Listener {
    private final GriviencePlugin plugin;
    private final CustomItemService itemService;
    private final ProfileEconomyService economyService;
    private static final String TITLE = ChatColor.DARK_PURPLE + "Inspector's Shop";

    public InspectorShopGui(GriviencePlugin plugin, CustomItemService itemService, ProfileEconomyService economyService) {
        this.plugin = plugin;
        this.itemService = itemService;
        this.economyService = economyService;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        inv.setItem(11, createShopItem(itemService.createMiningItem(MiningItemType.MINING_XP_SCROLL), 5000, "Grants +500 Mining XP."));
        inv.setItem(13, createShopItem(itemService.createMiningItem(MiningItemType.ORE_FRAGMENT_BUNDLE), 12000, "Contains 5-15 random ore fragments."));
        inv.setItem(15, createShopItem(itemService.createMiningItem(MiningItemType.TEMP_MINING_SPEED_BOOST), 25000, "Haste II for 5-10 minutes. Non-stackable."));
        inv.setItem(22, createNavItem(
                Material.EMERALD,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Open Commodity Buyback",
                "Sell Bazaar commodities to the NPC.",
                "AH-style gear is intentionally blocked."
        ));

        player.openInventory(inv);
    }

    private ItemStack createShopItem(ItemStack item, double price, String description) {
        ItemStack shopItem = item.clone();
        ItemMeta meta = shopItem.getItemMeta();
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        lore.add(ChatColor.GRAY + description);
        lore.add("");
        lore.add(ChatColor.GOLD + "Price: " + ChatColor.YELLOW + price + " coins");
        lore.add(ChatColor.YELLOW + "Click to purchase!");
        meta.setLore(lore);
        shopItem.setItemMeta(meta);
        return shopItem;
    }

    private ItemStack createNavItem(Material icon, String name, String... lines) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>();
        for (String line : lines) {
            lore.add(ChatColor.GRAY + line);
        }
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to open.");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITLE)) return;
        event.setCancelled(true);

        // Only handle clicks in the top inventory
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getInventory())) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta()) return;
        int slot = event.getRawSlot();

        if (slot == 22) {
            if (plugin.getNpcSellShopGui() != null) {
                plugin.getNpcSellShopGui().open(player);
            }
            return;
        }

        double price = 0;
        MiningItemType type = null;

        if (slot == 11 && clicked.getType() == Material.PAPER) {
            price = 5000;
            type = MiningItemType.MINING_XP_SCROLL;
        } else if (slot == 13 && clicked.getType() == Material.CHEST) {
            price = 12000;
            type = MiningItemType.ORE_FRAGMENT_BUNDLE;
        } else if (slot == 15 && clicked.getType() == Material.GOLDEN_PICKAXE) {
            price = 25000;
            type = MiningItemType.TEMP_MINING_SPEED_BOOST;
        }

        if (type != null) {
            if (economyService.has(player, price)) {
                economyService.withdraw(player, price);
                player.getInventory().addItem(itemService.createMiningItem(type));
                player.sendMessage(ChatColor.GREEN + "Purchased " + clicked.getItemMeta().getDisplayName() + "!");
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            } else {
                player.sendMessage(ChatColor.RED + "You don't have enough coins!");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            }
        }
    }
}
