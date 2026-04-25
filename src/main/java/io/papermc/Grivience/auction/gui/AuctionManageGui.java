package io.papermc.Grivience.auction.gui;

import io.papermc.Grivience.auction.AuctionItem;
import io.papermc.Grivience.auction.AuctionManager;
import io.papermc.Grivience.gui.SkyblockGui;
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

public class AuctionManageGui implements InventoryHolder {

    private final Inventory inventory;
    private final Player player;
    private final List<AuctionItem> myAuctions;

    public AuctionManageGui(Player player, AuctionManager auctionManager) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, "Manage Auctions");
        this.myAuctions = auctionManager.getPlayerAuctions(player.getUniqueId());
        
        List<AuctionItem> myBids = auctionManager.getPlayerBids(player.getUniqueId());
        for (AuctionItem bidItem : myBids) {
            if (!myAuctions.contains(bidItem)) {
                myAuctions.add(bidItem);
            }
        }
        
        update();
    }

    public void update() {
        inventory.clear();

        // Fill background
        ItemStack filler = SkyblockGui.filler(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        // Info item
        inventory.setItem(13, SkyblockGui.button(Material.BOOK, ChatColor.GREEN + "Your Auctions", 
                List.of(ChatColor.GRAY + "View and manage your current", ChatColor.GRAY + "auctions and bids.", "", ChatColor.GRAY + "Auctions: " + ChatColor.YELLOW + myAuctions.size())));

        // Display auctions in the middle
        int[] slots = {20, 21, 22, 23, 24, 29, 30, 31, 32, 33};
        int index = 0;
        for (AuctionItem auction : myAuctions) {
            if (index >= slots.length) break;

            ItemStack displayItem = auction.getItem().clone();
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.getLore();
                if (lore == null) lore = new ArrayList<>();
                lore.add("");
                
                String statusColor = switch (auction.getStatus()) {
                    case ACTIVE -> ChatColor.GREEN.toString();
                    case SOLD -> ChatColor.GOLD.toString();
                    case EXPIRED -> ChatColor.RED.toString();
                };
                
                lore.add(ChatColor.GRAY + "Status: " + statusColor + auction.getStatus().name());
                
                if (auction.isBin()) {
                    lore.add(ChatColor.GRAY + "Buy it now: " + ChatColor.GOLD + auction.getStartingBid() + " coins");
                } else {
                    long currentBid = auction.getHighestBid() != null ? auction.getHighestBid().amount() : 0;
                    lore.add(ChatColor.GRAY + "Highest bid: " + ChatColor.GOLD + currentBid + " coins");
                }
                
                if (auction.getStatus() == AuctionItem.AuctionStatus.SOLD || auction.getStatus() == AuctionItem.AuctionStatus.EXPIRED) {
                    if (auction.getSeller().equals(player.getUniqueId()) && !auction.isSellerClaimed()) {
                        lore.add("");
                        lore.add(ChatColor.YELLOW + "Click to claim!");
                    }
                } else {
                    long timeLeft = auction.getEndTime() - System.currentTimeMillis();
                    lore.add(ChatColor.GRAY + "Ends in: " + ChatColor.YELLOW + formatTime(timeLeft));
                }

                meta.setLore(lore);
                displayItem.setItemMeta(meta);
            }
            inventory.setItem(slots[index++], displayItem);
        }

        // Create button
        inventory.setItem(48, SkyblockGui.button(Material.GOLDEN_HORSE_ARMOR, ChatColor.GREEN + "Create Auction", 
                List.of(ChatColor.GRAY + "Put an item up for auction", ChatColor.GRAY + "to earn some coins.", "", ChatColor.YELLOW + "Click to create!")));

        inventory.setItem(49, SkyblockGui.backButton("Auction House"));
    }

    private String formatTime(long ms) {
        if (ms <= 0) return "Ended";
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        if (hours > 0) return hours + "h " + (minutes % 60) + "m";
        if (minutes > 0) return minutes + "m " + (seconds % 60) + "s";
        return seconds + "s";
    }

    public List<AuctionItem> getMyAuctions() {
        return myAuctions;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
