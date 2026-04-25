package io.papermc.Grivience.npcshop;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.bazaar.BazaarShopManager;
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
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NpcShopGui implements Listener {
    private final GriviencePlugin plugin;
    private final ProfileEconomyService economyService;
    private final BazaarShopManager bazaarManager;

    private static final int[] BORDER_SLOTS = {
        0, 1, 2, 3, 4, 5, 6, 7, 8,
        9, 17, 18, 26, 27, 35, 36, 44,
        45, 46, 47, 48, 50, 51, 52, 53
    };

    private static final int[] SHOP_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };

    public NpcShopGui(GriviencePlugin plugin, ProfileEconomyService economyService, BazaarShopManager bazaarManager) {
        this.plugin = plugin;
        this.economyService = economyService;
        this.bazaarManager = bazaarManager;
    }

    public void open(Player player, NpcShop shop) {
        if (shop == null) return;
        Inventory inv = Bukkit.createInventory(new ShopHolder(shop), 54, shop.getDisplayName());

        // Fill border
        ItemStack border = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int slot : BORDER_SLOTS) {
            inv.setItem(slot, border);
        }

        // Fill shop items
        // We use a mapping or just sequential slots if shop items aren't fixed to slots
        // But current NpcShop seems to use specific slots.
        // Let's adapt: if the shop uses slots 10-16 etc, we respect them.
        // If it uses 0, 1, 2 we might want to remap them to the center grid.
        
        for (Map.Entry<Integer, NpcShopItem> entry : shop.getItems().entrySet()) {
            int originalSlot = entry.getKey();
            // Simple remapping: if it's a small shop (indices 0-27), map to center grid
            int targetSlot = originalSlot;
            if (originalSlot >= 0 && originalSlot < SHOP_SLOTS.length) {
                targetSlot = SHOP_SLOTS[originalSlot];
            }
            
            if (targetSlot >= 0 && targetSlot < 54) {
                inv.setItem(targetSlot, buildShopDisplay(entry.getValue()));
            }
        }

        // Footer
        if (shop.getId().contains("_")) {
            inv.setItem(48, createGuiItem(Material.ARROW, ChatColor.GREEN + "Go Back", 
                ChatColor.GRAY + "To " + ChatColor.DARK_GRAY + "Previous Menu"));
        } else {
            inv.setItem(48, border);
        }

        inv.setItem(49, createGuiItem(Material.BARRIER, ChatColor.RED + "Close"));
        inv.setItem(50, createSellItemHover());

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f);
    }

    private ItemStack buildShopDisplay(NpcShopItem shopItem) {
        ItemStack base = shopItem.item().clone();
        ItemMeta meta = base.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            if (!lore.isEmpty()) lore.add("");
            
            if (shopItem.isCategory()) {
                lore.add(ChatColor.YELLOW + "Click to view category!");
            } else {
                lore.add(ChatColor.GRAY + "Cost");
                lore.add(ChatColor.GOLD + String.format("%,.1f", shopItem.price()) + " Coins");
                lore.add("");
                lore.add(ChatColor.YELLOW + "Left-Click to buy 1!");
                lore.add(ChatColor.YELLOW + "Right-Click to buy 64!");
            }
            meta.setLore(lore);
            base.setItemMeta(meta);
        }
        return base;
    }

    private ItemStack createGuiItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                List<String> l = new ArrayList<>();
                for (String s : lore) l.add(s);
                meta.setLore(l);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSellItemHover() {
        return createGuiItem(Material.HOPPER, ChatColor.GREEN + "Sell Item",
            ChatColor.GRAY + "Click items in your inventory to",
            ChatColor.GRAY + "sell them to this shop!",
            "",
            ChatColor.YELLOW + "Right-Click to sell all of a type!",
            ChatColor.DARK_GRAY + "(Not implemented yet)");
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();
        
        if (top != null && top.getHolder() instanceof ShopHolder holder) {
            event.setCancelled(true);
            if (event.getClickedInventory() == null) return;
            
            if (event.getClickedInventory().equals(top)) {
                int slot = event.getRawSlot();
                if (slot == 49) {
                    player.closeInventory();
                    player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.5f, 1.0f);
                } else if (slot == 48 && top.getItem(48) != null && top.getItem(48).getType() == Material.ARROW) {
                    String id = holder.getShop().getId();
                    if (id.contains("_")) {
                        String parentId = id.substring(0, id.lastIndexOf('_'));
                        NpcShop parent = plugin.getNpcShopManager().getShop(parentId);
                        if (parent != null) {
                            open(player, parent);
                        }
                    }
                } else {
                    // Find which shop item was clicked by checking remapped slot
                    NpcShopItem clickedItem = null;
                    for (Map.Entry<Integer, NpcShopItem> entry : holder.getShop().getItems().entrySet()) {
                        int originalSlot = entry.getKey();
                        int targetSlot = originalSlot;
                        if (originalSlot >= 0 && originalSlot < SHOP_SLOTS.length) {
                            targetSlot = SHOP_SLOTS[originalSlot];
                        }
                        
                        if (targetSlot == slot) {
                            clickedItem = entry.getValue();
                            break;
                        }
                    }
                    
                    if (clickedItem != null) {
                        if (clickedItem.isCategory()) {
                            open(player, plugin.getNpcShopManager().getShop(clickedItem.targetShopId()));
                        } else {
                            int amount = event.isRightClick() ? 64 : 1;
                            buyItem(player, clickedItem, amount);
                        }
                    }
                }
            } else if (event.getClickedInventory().equals(player.getInventory())) {
                sellItem(player, event.getCurrentItem(), event.getSlot());
            }
        }
    }
    
    private void buyItem(Player player, NpcShopItem shopItem, int multiplier) {
        double totalCost = shopItem.price() * multiplier;
        if (economyService.has(player, totalCost)) {
            ItemStack template = shopItem.item().clone();
            int unitAmount = template.getAmount();
            int totalAmount = unitAmount * multiplier;
            
            // Check if player has space (rough check or just use DropDeliveryUtil)
            economyService.withdraw(player, totalCost);
            
            ItemStack toGive = template.clone();
            toGive.setAmount(totalAmount);
            
            // Using a helper to give items safely
            io.papermc.Grivience.util.DropDeliveryUtil.giveToInventoryOrDrop(player, toGive, player.getLocation());
            
            String itemName = template.getType().name();
            if (template.hasItemMeta() && template.getItemMeta().hasDisplayName()) {
                itemName = template.getItemMeta().getDisplayName();
            }
            
            String amountStr = (totalAmount > 1) ? totalAmount + "x " : "";
            player.sendMessage(ChatColor.GREEN + "You bought " + amountStr + itemName + ChatColor.GREEN + " for " + ChatColor.GOLD + String.format("%,.1f", totalCost) + " Coins!");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 2.0f);
        } else {
            player.sendMessage(ChatColor.RED + "You don't have enough coins!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
    
    private void sellItem(Player player, ItemStack item, int slot) {
        if (item == null || item.getType() == Material.AIR) return;
        
        if (bazaarManager == null) {
            player.sendMessage(ChatColor.RED + "Selling is currently disabled.");
            return;
        }
        
        java.util.UUID profileId = bazaarManager.requireProfileId(player);
        if (profileId == null) return;
        
        BazaarShopManager.NpcSellQuote quote = bazaarManager.quoteNpcSell(player, item, item.getAmount());
        if (!quote.sellable()) {
            player.sendMessage(ChatColor.RED + "You cannot sell this item to an NPC!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        
        int amount = item.getAmount();
        player.getInventory().setItem(slot, new ItemStack(Material.AIR));
        
        // Payout to profile
        economyService.deposit(player, quote.netCoins());
        
        player.sendMessage(ChatColor.GREEN + "You sold " + amount + "x " + quote.product().getProductName() + 
            " for " + ChatColor.GOLD + String.format("%,.1f", quote.netCoins()) + " Coins!");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }
    
    public static class ShopHolder implements InventoryHolder {
        private final NpcShop shop;
        public ShopHolder(NpcShop shop) { this.shop = shop; }
        public NpcShop getShop() { return shop; }
        @Override public Inventory getInventory() { return null; }
    }
}
