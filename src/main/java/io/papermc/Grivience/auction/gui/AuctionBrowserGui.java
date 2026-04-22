package io.papermc.Grivience.auction.gui;

import io.papermc.Grivience.auction.AuctionItem;
import io.papermc.Grivience.auction.AuctionManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class AuctionBrowserGui implements InventoryHolder {

    private final Inventory inventory;
    private final List<AuctionItem> auctions;
    private final Player player;
    private int page = 0;

    public AuctionBrowserGui(Player player, AuctionManager auctionManager) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, "Auction Browser");
        this.auctions = auctionManager.getActiveAuctions();
        update();
    }

    public void update() {
        inventory.clear();

        int startIndex = page * 28;
        int endIndex = Math.min(startIndex + 28, auctions.size());

        int slot = 10;
        for (int i = startIndex; i < endIndex; i++) {
            AuctionItem auction = auctions.get(i);
            ItemStack displayItem = auction.getItem().clone();
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.getLore();
                if (lore == null) lore = new ArrayList<>();
                lore.add("");
                lore.add(ChatColor.GRAY + "Seller: " + Bukkit.getOfflinePlayer(auction.getSeller()).getName());
                if (auction.isBin()) {
                    lore.add(ChatColor.GRAY + "Buy it now: " + ChatColor.GOLD + auction.getStartingBid() + " coins");
                } else {
                    long currentBid = auction.getHighestBid() != null ? auction.getHighestBid().amount() : 0;
                    lore.add(ChatColor.GRAY + "Highest bid: " + ChatColor.GOLD + currentBid + " coins");
                    lore.add(ChatColor.GRAY + "Starting bid: " + ChatColor.GOLD + auction.getStartingBid() + " coins");
                }
                
                long timeLeft = auction.getEndTime() - System.currentTimeMillis();
                lore.add(ChatColor.GRAY + "Ends in: " + ChatColor.YELLOW + (timeLeft > 0 ? (timeLeft / 1000) + "s" : "Ended"));
                lore.add("");
                lore.add(ChatColor.YELLOW + "Click to inspect!");
                meta.setLore(lore);
                displayItem.setItemMeta(meta);
            }
            inventory.setItem(slot, displayItem);

            if ((slot + 1) % 9 == 8) {
                slot += 3;
            } else {
                slot++;
            }
        }

        // Navigation and other buttons
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(ChatColor.GREEN + "Previous Page");
        back.setItemMeta(backMeta);
        inventory.setItem(48, back);

        ItemStack next = new ItemStack(Material.ARROW);
        ItemMeta nextMeta = next.getItemMeta();
        nextMeta.setDisplayName(ChatColor.GREEN + "Next Page");
        next.setItemMeta(nextMeta);
        inventory.setItem(50, next);

        ItemStack manage = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta manageMeta = manage.getItemMeta();
        manageMeta.setDisplayName(ChatColor.GREEN + "Manage Auctions");
        manage.setItemMeta(manageMeta);
        inventory.setItem(49, manage);
    }

    public List<AuctionItem> getAuctions() {
        return auctions;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
        update();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
