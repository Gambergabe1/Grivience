package io.papermc.Grivience.auction.gui;

import io.papermc.Grivience.auction.AuctionItem;
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

public class AuctionInspectGui implements InventoryHolder {

    private final Inventory inventory;
    private final AuctionItem auction;
    private final Player player;

    public AuctionInspectGui(Player player, AuctionItem auction) {
        this.player = player;
        this.auction = auction;
        this.inventory = Bukkit.createInventory(this, 54, "Inspect Auction");
        update();
    }

    public void update() {
        inventory.clear();

        // Fill background
        ItemStack filler = SkyblockGui.filler(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        inventory.setItem(13, auction.getItem());

        // Bid/Buy button
        List<String> lore = new ArrayList<>();
        if (auction.isBin()) {
            lore.add(ChatColor.GRAY + "Cost: " + ChatColor.GOLD + auction.getStartingBid() + " coins");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to buy!");
            inventory.setItem(31, SkyblockGui.button(Material.GOLD_BLOCK, ChatColor.GOLD + "Buy It Now", lore));
        } else {
            long currentBid = auction.getHighestBid() != null ? auction.getHighestBid().amount() : 0;
            long nextBid = currentBid == 0 ? auction.getStartingBid() : (long) (currentBid * 1.1);
            if (currentBid == nextBid) nextBid += 10;
            
            lore.add(ChatColor.GRAY + "Highest Bid: " + ChatColor.GOLD + currentBid + " coins");
            if (auction.getHighestBid() != null) {
                lore.add(ChatColor.GRAY + "Bidder: " + ChatColor.YELLOW + auction.getHighestBid().bidderName());
            }
            lore.add("");
            lore.add(ChatColor.GRAY + "New Bid: " + ChatColor.GOLD + nextBid + " coins");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to bid!");
            inventory.setItem(31, SkyblockGui.button(Material.GOLD_INGOT, ChatColor.GOLD + "Place Bid", lore));
        }

        // Seller info
        inventory.setItem(40, SkyblockGui.button(Material.PLAYER_HEAD, ChatColor.GREEN + "Seller Info", 
                List.of(ChatColor.GRAY + "Seller: " + ChatColor.YELLOW + Bukkit.getOfflinePlayer(auction.getSeller()).getName())));

        inventory.setItem(49, SkyblockGui.backButton("Browser"));
    }

    public AuctionItem getAuction() {
        return auction;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
