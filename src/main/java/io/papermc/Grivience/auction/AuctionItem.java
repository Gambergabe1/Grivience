package io.papermc.Grivience.auction;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AuctionItem {
    private final UUID id;
    private final UUID seller;
    private final ItemStack item;
    private final boolean bin; // true if Buy It Now
    private final long startingBid;
    private final long endTime;
    private final List<AuctionBid> bids;
    private AuctionStatus status;
    private boolean sellerClaimed;

    public enum AuctionStatus {
        ACTIVE, SOLD, EXPIRED
    }

    public AuctionItem(UUID id, UUID seller, ItemStack item, boolean bin, long startingBid, long endTime, AuctionStatus status, boolean sellerClaimed, List<AuctionBid> bids) {
        this.id = id;
        this.seller = seller;
        this.item = item;
        this.bin = bin;
        this.startingBid = startingBid;
        this.endTime = endTime;
        this.status = status;
        this.sellerClaimed = sellerClaimed;
        this.bids = new ArrayList<>(bids);
    }

    public AuctionItem(UUID seller, ItemStack item, boolean bin, long startingBid, long durationMs) {
        this(UUID.randomUUID(), seller, item, bin, startingBid, System.currentTimeMillis() + durationMs, AuctionStatus.ACTIVE, false, new ArrayList<>());
    }

    public UUID getId() { return id; }
    public UUID getSeller() { return seller; }
    public ItemStack getItem() { return item.clone(); }
    public boolean isBin() { return bin; }
    public long getStartingBid() { return startingBid; }
    public long getEndTime() { return endTime; }
    public AuctionStatus getStatus() { return status; }
    public boolean isSellerClaimed() { return sellerClaimed; }
    public List<AuctionBid> getBids() { return new ArrayList<>(bids); }

    public void setStatus(AuctionStatus status) { this.status = status; }
    public void setSellerClaimed(boolean claimed) { this.sellerClaimed = claimed; }

    public void addBid(AuctionBid bid) {
        this.bids.add(bid);
    }

    public AuctionBid getHighestBid() {
        if (bids.isEmpty()) return null;
        AuctionBid highest = bids.get(0);
        for (AuctionBid bid : bids) {
            if (bid.amount() > highest.amount()) {
                highest = bid;
            }
        }
        return highest;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= endTime && status == AuctionStatus.ACTIVE;
    }

    public void updateStatus() {
        if (isExpired()) {
            if (bin || bids.isEmpty()) {
                status = AuctionStatus.EXPIRED;
            } else {
                status = AuctionStatus.SOLD;
            }
        }
    }
}
