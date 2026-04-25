package io.papermc.Grivience.auction.gui;

import io.papermc.Grivience.auction.AuctionCategory;
import io.papermc.Grivience.gui.SkyblockGui;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class AuctionMainMenuGui implements InventoryHolder {

    private final Inventory inventory;

    public AuctionMainMenuGui(Player player) {
        this.inventory = Bukkit.createInventory(this, 54, "Auction House");
        update();
    }

    public void update() {
        inventory.clear();
        
        // Fill border
        ItemStack filler = SkyblockGui.filler(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        // Categories
        inventory.setItem(11, SkyblockGui.button(Material.GOLDEN_SWORD, ChatColor.GOLD + "Weapons", 
                List.of(ChatColor.GRAY + "Find the best weapons", ChatColor.GRAY + "to defeat your enemies.", "", ChatColor.YELLOW + "Click to browse!")));
        
        inventory.setItem(12, SkyblockGui.button(Material.DIAMOND_CHESTPLATE, ChatColor.GOLD + "Armor", 
                List.of(ChatColor.GRAY + "Find the best armor", ChatColor.GRAY + "to protect yourself.", "", ChatColor.YELLOW + "Click to browse!")));
        
        inventory.setItem(13, SkyblockGui.button(Material.GOLD_NUGGET, ChatColor.GOLD + "Accessories", 
                List.of(ChatColor.GRAY + "Find talismans and rings", ChatColor.GRAY + "to boost your stats.", "", ChatColor.YELLOW + "Click to browse!")));
        
        inventory.setItem(14, SkyblockGui.button(Material.POTION, ChatColor.GOLD + "Consumables", 
                List.of(ChatColor.GRAY + "Find potions and food", ChatColor.GRAY + "to keep you going.", "", ChatColor.YELLOW + "Click to browse!")));
        
        inventory.setItem(15, SkyblockGui.button(Material.GRASS_BLOCK, ChatColor.GOLD + "Blocks", 
                List.of(ChatColor.GRAY + "Find all kinds of blocks", ChatColor.GRAY + "for your island.", "", ChatColor.YELLOW + "Click to browse!")));
        
        inventory.setItem(16, SkyblockGui.button(Material.STICK, ChatColor.GOLD + "Tools & Misc", 
                List.of(ChatColor.GRAY + "Find tools and miscellaneous", ChatColor.GRAY + "items here.", "", ChatColor.YELLOW + "Click to browse!")));

        // Bottom row
        inventory.setItem(48, SkyblockGui.button(Material.OAK_SIGN, ChatColor.GREEN + "Search", 
                List.of(ChatColor.GRAY + "Search for specific items", ChatColor.GRAY + "in the auction house.", "", ChatColor.YELLOW + "Click to search!")));
        
        inventory.setItem(49, SkyblockGui.closeButton());
        
        inventory.setItem(50, SkyblockGui.button(Material.BOOK, ChatColor.GREEN + "Manage Auctions", 
                List.of(ChatColor.GRAY + "View your auctions and bids.", "", ChatColor.YELLOW + "Click to manage!")));
        
        // Browser button in the middle
        inventory.setItem(31, SkyblockGui.button(Material.GOLD_BLOCK, ChatColor.GOLD + "Browse Auctions", 
                List.of(ChatColor.GRAY + "View all active auctions.", "", ChatColor.YELLOW + "Click to browse!")));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
