package io.papermc.Grivience.auction.gui;

import io.papermc.Grivience.auction.AuctionCategory;
import io.papermc.Grivience.auction.AuctionItem;
import io.papermc.Grivience.auction.AuctionManager;
import io.papermc.Grivience.auction.SortMode;
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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AuctionBrowserGui implements InventoryHolder {

    public enum TypeFilter {
        ALL("All Auctions", Material.GOLD_BLOCK),
        BIN("BIN Only", Material.GOLD_INGOT),
        AUCTION("Auctions Only", Material.ANVIL);

        private final String displayName;
        private final Material icon;

        TypeFilter(String displayName, Material icon) {
            this.displayName = displayName;
            this.icon = icon;
        }
    }

    private final Inventory inventory;
    private List<AuctionItem> allAuctions;
    private List<AuctionItem> filteredAuctions;
    private final Player player;
    
    private int page = 0;
    private AuctionCategory categoryFilter = null;
    private TypeFilter typeFilter = TypeFilter.ALL;
    private SortMode sortMode = SortMode.ENDING_SOON;

    public AuctionBrowserGui(Player player, AuctionManager auctionManager) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, "Auction Browser");
        this.allAuctions = auctionManager.getActiveAuctions();
        applyFilters();
    }

    public void applyFilters() {
        filteredAuctions = allAuctions.stream()
                .filter(a -> categoryFilter == null || AuctionCategory.fromItemStack(a.getItem()) == categoryFilter)
                .filter(a -> typeFilter == TypeFilter.ALL || (typeFilter == TypeFilter.BIN && a.isBin()) || (typeFilter == TypeFilter.AUCTION && !a.isBin()))
                .collect(Collectors.toList());

        // Apply sorting
        switch (sortMode) {
            case ENDING_SOON -> filteredAuctions.sort(Comparator.comparingLong(AuctionItem::getEndTime));
            case MOST_RECENT -> filteredAuctions.sort((a, b) -> Long.compare(b.getEndTime(), a.getEndTime()));
            case LOWEST_PRICE -> filteredAuctions.sort(Comparator.comparingLong(AuctionItem::getStartingBid));
            case HIGHEST_BID -> filteredAuctions.sort((a, b) -> {
                long bidA = a.getHighestBid() != null ? a.getHighestBid().amount() : a.getStartingBid();
                long bidB = b.getHighestBid() != null ? b.getHighestBid().amount() : b.getStartingBid();
                return Long.compare(bidB, bidA);
            });
        }
        
        update();
    }

    public void update() {
        inventory.clear();

        // Fill background
        ItemStack filler = SkyblockGui.filler(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 9; i++) inventory.setItem(i, filler);
        for (int i = 45; i < 54; i++) inventory.setItem(i, filler);
        for (int i = 0; i < 54; i+=9) {
            inventory.setItem(i, filler);
            inventory.setItem(i + 8, filler);
        }

        // Top row filters
        inventory.setItem(1, SkyblockGui.button(Material.HOPPER, ChatColor.GREEN + "Sort Mode", 
                List.of(ChatColor.GRAY + "Currently: " + ChatColor.YELLOW + sortMode.getDisplayName(), "", ChatColor.YELLOW + "Click to cycle!")));
        
        inventory.setItem(3, SkyblockGui.button(typeFilter.icon, ChatColor.GREEN + "Auction Type", 
                List.of(ChatColor.GRAY + "Currently: " + ChatColor.YELLOW + typeFilter.displayName, "", ChatColor.YELLOW + "Click to cycle!")));

        inventory.setItem(5, SkyblockGui.button(Material.OAK_SIGN, ChatColor.GREEN + "Search", 
                List.of(ChatColor.GRAY + "Find specific items.", "", ChatColor.YELLOW + "Click to search!")));

        inventory.setItem(7, SkyblockGui.button(categoryFilter == null ? Material.ITEM_FRAME : categoryFilter.getIcon(), 
                ChatColor.GREEN + "Category", 
                List.of(ChatColor.GRAY + "Currently: " + ChatColor.YELLOW + (categoryFilter == null ? "All" : categoryFilter.getDisplayName()), "", ChatColor.YELLOW + "Click to cycle!")));

        // Display auctions
        int startIndex = page * 28;
        int endIndex = Math.min(startIndex + 28, filteredAuctions.size());

        int slot = 10;
        for (int i = startIndex; i < endIndex; i++) {
            AuctionItem auction = filteredAuctions.get(i);
            ItemStack displayItem = auction.getItem().clone();
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.getLore();
                if (lore == null) lore = new ArrayList<>();
                lore.add("");
                lore.add(ChatColor.GRAY + "Seller: " + ChatColor.YELLOW + Bukkit.getOfflinePlayer(auction.getSeller()).getName());
                if (auction.isBin()) {
                    lore.add(ChatColor.GRAY + "Buy it now: " + ChatColor.GOLD + auction.getStartingBid() + " coins");
                } else {
                    long currentBid = auction.getHighestBid() != null ? auction.getHighestBid().amount() : 0;
                    lore.add(ChatColor.GRAY + "Highest bid: " + ChatColor.GOLD + currentBid + " coins");
                    if (currentBid == 0) {
                        lore.add(ChatColor.GRAY + "Starting bid: " + ChatColor.GOLD + auction.getStartingBid() + " coins");
                    }
                }
                
                long timeLeft = auction.getEndTime() - System.currentTimeMillis();
                String timeStr = formatTime(timeLeft);
                lore.add(ChatColor.GRAY + "Ends in: " + ChatColor.YELLOW + timeStr);
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

        // Navigation
        if (page > 0) {
            inventory.setItem(48, SkyblockGui.button(Material.ARROW, ChatColor.GREEN + "Previous Page", List.of(ChatColor.GRAY + "Page " + page)));
        }
        
        inventory.setItem(49, SkyblockGui.backButton("Auction House"));

        if (endIndex < filteredAuctions.size()) {
            inventory.setItem(50, SkyblockGui.button(Material.ARROW, ChatColor.GREEN + "Next Page", List.of(ChatColor.GRAY + "Page " + (page + 2))));
        }
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

    public List<AuctionItem> getFilteredAuctions() {
        return filteredAuctions;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
        update();
    }

    public void setCategoryFilter(AuctionCategory categoryFilter) {
        this.categoryFilter = categoryFilter;
        this.page = 0;
        applyFilters();
    }

    public void cycleCategory() {
        if (categoryFilter == null) {
            categoryFilter = AuctionCategory.values()[0];
        } else {
            int next = categoryFilter.ordinal() + 1;
            if (next >= AuctionCategory.values().length) {
                categoryFilter = null;
            } else {
                categoryFilter = AuctionCategory.values()[next];
            }
        }
        this.page = 0;
        applyFilters();
    }

    public void cycleType() {
        int next = typeFilter.ordinal() + 1;
        if (next >= TypeFilter.values().length) {
            typeFilter = TypeFilter.values()[0];
        } else {
            typeFilter = TypeFilter.values()[next];
        }
        this.page = 0;
        applyFilters();
    }

    public void cycleSort() {
        int next = sortMode.ordinal() + 1;
        if (next >= SortMode.values().length) {
            sortMode = SortMode.values()[0];
        } else {
            sortMode = SortMode.values()[next];
        }
        this.page = 0;
        applyFilters();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
