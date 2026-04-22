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

        int slot = 10;
        for (AuctionItem auction : myAuctions) {
            if (slot > 43) break; // Limit to one page for simplicity

            ItemStack displayItem = auction.getItem().clone();
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.getLore();
                if (lore == null) lore = new ArrayList<>();
                lore.add("");
                lore.add(ChatColor.GRAY + "Status: " + ChatColor.YELLOW + auction.getStatus());
                
                if (auction.isBin()) {
                    lore.add(ChatColor.GRAY + "Buy it now: " + ChatColor.GOLD + auction.getStartingBid() + " coins");
                } else {
                    long currentBid = auction.getHighestBid() != null ? auction.getHighestBid().amount() : 0;
                    lore.add(ChatColor.GRAY + "Highest bid: " + ChatColor.GOLD + currentBid + " coins");
                }
                
                if (auction.getStatus() == AuctionItem.AuctionStatus.SOLD || auction.getStatus() == AuctionItem.AuctionStatus.EXPIRED) {
                    if (auction.getSeller().equals(player.getUniqueId()) && !auction.isSellerClaimed()) {
                        lore.add(ChatColor.GREEN + "Click to claim!");
                    } else if (auction.getHighestBid() != null && auction.getHighestBid().bidder().equals(player.getUniqueId()) && auction.getStatus() == AuctionItem.AuctionStatus.EXPIRED) {
                        // In real skyblock, you claim items/coins from bids here too, but to keep it simple we assume item is delivered on buy/bid win.
                        lore.add(ChatColor.YELLOW + "Auction ended.");
                    }
                }

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

        ItemStack create = new ItemStack(Material.GOLDEN_HORSE_ARMOR); // Horse armor as create
        ItemMeta createMeta = create.getItemMeta();
        createMeta.setDisplayName(ChatColor.GREEN + "Create Auction");
        create.setItemMeta(createMeta);
        inventory.setItem(48, create);

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(ChatColor.GREEN + "Back to Browser");
        back.setItemMeta(backMeta);
        inventory.setItem(49, back);
    }

    public List<AuctionItem> getMyAuctions() {
        return myAuctions;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
