package io.papermc.Grivience.auction.gui;

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

public class AuctionCreateGui implements InventoryHolder {

    private final Inventory inventory;
    private final Player player;
    private boolean isBin = true;
    private long price = 500;
    private long durationHours = 24;
    private ItemStack itemToAuction = null;

    public AuctionCreateGui(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, "Create Auction");
        update();
    }

    public void update() {
        inventory.clear();

        // Fill background
        ItemStack filler = SkyblockGui.filler(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, filler);
        }
        
        if (itemToAuction != null) {
            inventory.setItem(13, itemToAuction);
        } else {
            inventory.setItem(13, null); // Clear if null
        }

        // Price button
        inventory.setItem(29, SkyblockGui.button(Material.GOLD_INGOT, ChatColor.GOLD + (isBin ? "Buy It Now Price" : "Starting Bid"), 
                List.of(ChatColor.GRAY + "Price: " + ChatColor.GOLD + price + " coins", "", 
                        ChatColor.YELLOW + "Left Click: +500", ChatColor.YELLOW + "Right Click: -500",
                        ChatColor.AQUA + "Shift + Left Click: +10000", ChatColor.AQUA + "Shift + Right Click: -10000")));

        // Duration button
        inventory.setItem(31, SkyblockGui.button(Material.CLOCK, ChatColor.GOLD + "Duration", 
                List.of(ChatColor.GRAY + "Current: " + ChatColor.YELLOW + durationHours + " hours", "", 
                        ChatColor.YELLOW + "Left Click: +1h", ChatColor.YELLOW + "Right Click: -1h",
                        ChatColor.AQUA + "Shift + Left Click: +24h", ChatColor.AQUA + "Shift + Right Click: -24h")));

        // Type button
        inventory.setItem(33, SkyblockGui.button(isBin ? Material.GOLD_BLOCK : Material.ANVIL, ChatColor.GOLD + "Auction Type", 
                List.of(ChatColor.GRAY + "Current: " + ChatColor.YELLOW + (isBin ? "BIN (Buy It Now)" : "Auction"), "", ChatColor.YELLOW + "Click to toggle!")));

        // Create button
        inventory.setItem(48, SkyblockGui.button(Material.EMERALD, ChatColor.GREEN + "Create Auction", 
                List.of(ChatColor.GRAY + "Submit your auction to the house.", "", ChatColor.YELLOW + "Click to create!")));

        inventory.setItem(49, SkyblockGui.backButton("Auction House"));
    }

    public void changePrice(long delta) {
        this.price = Math.max(10, this.price + delta);
        update();
    }

    public void changeDuration(long delta) {
        this.durationHours = Math.max(1, Math.min(336, this.durationHours + delta)); // Max 14 days
        update();
    }

    public void toggleType() {
        this.isBin = !this.isBin;
        update();
    }

    public void setItemToAuction(ItemStack item) {
        this.itemToAuction = item;
        update();
    }

    public ItemStack getItemToAuction() {
        return itemToAuction;
    }

    public boolean isBin() {
        return isBin;
    }

    public long getPrice() {
        return price;
    }

    public long getDurationHours() {
        return durationHours;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
