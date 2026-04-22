package io.papermc.Grivience.auction.gui;

import io.papermc.Grivience.auction.AuctionItem;
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

        inventory.setItem(13, auction.getItem());

        ItemStack actionItem = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta actionMeta = actionItem.getItemMeta();
        
        List<String> lore = new ArrayList<>();
        if (auction.isBin()) {
            actionMeta.setDisplayName(ChatColor.GOLD + "Buy It Now");
            lore.add(ChatColor.GRAY + "Cost: " + ChatColor.GOLD + auction.getStartingBid() + " coins");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to buy!");
        } else {
            actionMeta.setDisplayName(ChatColor.GOLD + "Place Bid");
            long currentBid = auction.getHighestBid() != null ? auction.getHighestBid().amount() : 0;
            long nextBid = currentBid == 0 ? auction.getStartingBid() : (long) (currentBid * 1.1);
            if (currentBid == nextBid) nextBid += 10;
            lore.add(ChatColor.GRAY + "Next Bid: " + ChatColor.GOLD + nextBid + " coins");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to bid!");
        }
        
        actionMeta.setLore(lore);
        actionItem.setItemMeta(actionMeta);
        inventory.setItem(31, actionItem);

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(ChatColor.GREEN + "Back to Browser");
        back.setItemMeta(backMeta);
        inventory.setItem(49, back);
    }

    public AuctionItem getAuction() {
        return auction;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
