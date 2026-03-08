package io.papermc.Grivience.bazaar;

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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * NPC sell shop for commodities only.
 * Payout is always capped at or below Bazaar-equivalent sell value.
 */
public final class NpcSellShopGui implements Listener {
    private static final String TITLE = ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Commodity Buyback";
    private static final int INVENTORY_SIZE = 54;

    private final BazaarShopManager shopManager;

    public NpcSellShopGui(BazaarShopManager shopManager) {
        this.shopManager = shopManager;
    }

    public void open(Player player) {
        if (player == null) {
            return;
        }
        if (!shopManager.isNpcShopEnabled()) {
            player.sendMessage(ChatColor.RED + "NPC commodity shop is disabled.");
            return;
        }

        Inventory inventory = Bukkit.createInventory(new Holder(), INVENTORY_SIZE, TITLE);
        render(player, inventory);
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (top == null || !(top.getHolder() instanceof Holder)) {
            return;
        }

        event.setCancelled(true);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(top)) {
            return;
        }

        int slot = event.getRawSlot();
        switch (slot) {
            case 29 -> {
                boolean sold = shopManager.sellMainHandToNpc(player);
                if (!sold) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
                }
                refreshNextTick(player);
            }
            case 33 -> {
                BazaarShopManager.NpcBulkSellResult result = shopManager.sellAllEligibleToNpc(player);
                if (!result.soldAnything()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
                }
                refreshNextTick(player);
            }
            case 49 -> {
                render(player, top);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
            }
            case 53 -> player.closeInventory();
            default -> {
            }
        }
    }

    private void refreshNextTick(Player player) {
        Bukkit.getScheduler().runTaskLater(shopManager.getPlugin(), () -> {
            if (!player.isOnline()) {
                return;
            }
            Inventory top = player.getOpenInventory().getTopInventory();
            if (top != null && top.getHolder() instanceof Holder) {
                render(player, top);
            }
        }, 1L);
    }

    private void render(Player player, Inventory inventory) {
        ItemStack filler = createSimpleItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inventory.setItem(i, filler);
        }

        inventory.setItem(4, headerItem(player));
        inventory.setItem(13, handPreviewItem(player));
        inventory.setItem(29, sellHandButton(player));
        inventory.setItem(33, sellAllButton(player));
        inventory.setItem(49, createSimpleItem(Material.CLOCK, ChatColor.YELLOW + "Refresh"));
        inventory.setItem(53, createSimpleItem(Material.BARRIER, ChatColor.RED + "Close"));
    }

    private ItemStack headerItem(Player player) {
        ItemStack item = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "NPC Commodity Buyer");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Sell Bazaar-eligible commodity items.");
        lore.add(ChatColor.DARK_GRAY + "Weapons, armor, and AH-style gear are blocked.");
        lore.add("");
        lore.add(ChatColor.GRAY + "Current Payout Rate: " + ChatColor.GREEN
                + formatPercent(shopManager.getNpcShopSellMultiplier()));
        lore.add(ChatColor.GRAY + "of capped Bazaar sell value.");
        lore.add("");
        lore.add(ChatColor.GRAY + "Profile: " + ChatColor.YELLOW + player.getName());
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack handPreviewItem(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        BazaarShopManager.NpcSellQuote quote = shopManager.quoteNpcSell(player, hand);
        if (!quote.sellable() || quote.product() == null) {
            ItemStack barrier = new ItemStack(Material.BARRIER);
            ItemMeta meta = barrier.getItemMeta();
            if (meta == null) {
                return barrier;
            }
            meta.setDisplayName(ChatColor.RED + "Main Hand Item Not Sellable");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + quote.rejectionReason());
            lore.add("");
            lore.add(ChatColor.YELLOW + "Hold a Bazaar commodity item");
            lore.add(ChatColor.YELLOW + "in your main hand.");
            meta.setLore(lore);
            barrier.setItemMeta(meta);
            return barrier;
        }

        ItemStack preview = new ItemStack(quote.product().getIcon());
        ItemMeta meta = preview.getItemMeta();
        if (meta == null) {
            return preview;
        }
        meta.setDisplayName(ChatColor.GOLD + "Main Hand Quote: " + quote.product().getProductName());
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Amount: " + ChatColor.YELLOW + quote.amount());
        lore.add(ChatColor.GRAY + "Bazaar Cap / unit: " + ChatColor.GOLD + shopManager.formatCoins(quote.bazaarCapUnitPrice()));
        lore.add(ChatColor.GRAY + "NPC Price / unit: " + ChatColor.GREEN + shopManager.formatCoins(quote.npcUnitPrice()));
        lore.add("");
        lore.add(ChatColor.GRAY + "Gross: " + ChatColor.GOLD + shopManager.formatCoins(quote.grossCoins()));
        lore.add(ChatColor.GRAY + "Final Payout: " + ChatColor.GREEN + shopManager.formatCoins(quote.netCoins()));
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "NPC payout is capped at Bazaar-equivalent value.");
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        preview.setItemMeta(meta);
        return preview;
    }

    private ItemStack sellHandButton(Player player) {
        BazaarShopManager.NpcSellQuote quote = shopManager.quoteNpcSell(
                player,
                player.getInventory().getItemInMainHand()
        );

        ItemStack item = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Sell Main Hand Stack");
        List<String> lore = new ArrayList<>();
        if (quote.sellable() && quote.product() != null) {
            lore.add(ChatColor.GRAY + "Item: " + ChatColor.YELLOW + quote.product().getProductName());
            lore.add(ChatColor.GRAY + "Amount: " + ChatColor.YELLOW + quote.amount());
            lore.add(ChatColor.GRAY + "Payout: " + ChatColor.GREEN + shopManager.formatCoins(quote.netCoins()));
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to sell now.");
        } else {
            lore.add(ChatColor.RED + "No sellable item in your main hand.");
            lore.add("");
            lore.add(ChatColor.GRAY + "Reason: " + ChatColor.RED + quote.rejectionReason());
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack sellAllButton(Player player) {
        BazaarShopManager.NpcBulkSellQuote preview = shopManager.previewNpcInventorySell(player);

        ItemStack item = new ItemStack(Material.CHEST_MINECART);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Sell All Eligible Inventory");
        List<String> lore = new ArrayList<>();
        if (preview.hasSellableItems()) {
            lore.add(ChatColor.GRAY + "Sellable Stacks: " + ChatColor.YELLOW + preview.sellableStacks());
            lore.add(ChatColor.GRAY + "Sellable Items: " + ChatColor.YELLOW + preview.sellableItems());
            lore.add(ChatColor.GRAY + "Skipped Stacks: " + ChatColor.RED + preview.skippedStacks());
            lore.add("");
            lore.add(ChatColor.GRAY + "Gross: " + ChatColor.GOLD + shopManager.formatCoins(preview.grossCoins()));
            lore.add(ChatColor.GRAY + "Final Payout: " + ChatColor.GREEN + shopManager.formatCoins(preview.netCoins()));
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to sell all eligible items.");
        } else {
            lore.add(ChatColor.RED + "No Bazaar-eligible commodities found.");
            lore.add(ChatColor.GRAY + "Skipped Stacks: " + ChatColor.RED + preview.skippedStacks());
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSimpleItem(Material type, String name) {
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private String formatPercent(double value) {
        if (!Double.isFinite(value)) {
            return "0%";
        }
        return String.format(Locale.US, "%.0f%%", Math.max(0.0, value) * 100.0);
    }

    private static final class Holder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
