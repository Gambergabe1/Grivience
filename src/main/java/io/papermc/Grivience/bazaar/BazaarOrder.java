package io.papermc.Grivience.bazaar;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

/**
 * Represents a player-created Bazaar order.
 * Mirrors Skyblock's order system with buy and sell orders.
 * 
 * Features:
 * - Orders expire after 7 days (Skyblock standard)
 * - Partial fills with shopping bag delivery
 * - Price-time priority matching
 */
public final class BazaarOrder {
    public enum OrderType {
        BUY("§6Buy Order", "§ePlace Buy Order", "§7You will buy at this price or lower."),
        SELL("§aSell Order", "§dPlace Sell Order", "§7You will sell at this price or higher.");
        
        private final String displayName;
        private final String actionName;
        private final String description;
        
        OrderType(String displayName, String actionName, String description) {
            this.displayName = displayName;
            this.actionName = actionName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getActionName() { return actionName; }
        public String getDescription() { return description; }
    }
    
    public enum OrderStatus {
        ACTIVE,
        PARTIALLY_FILLED,
        FILLED,
        CANCELLED,
        EXPIRED
    }

    private final String orderId;
    private final UUID owner;
    private final UUID profileId;
    private final String ownerName;
    private final String productId;
    private final OrderType orderType;
    private final double unitPrice;
    private final int totalAmount;
    private int remainingAmount;
    private int filledAmount;
    private final long createdAt;
    private long expiresAt;
    private OrderStatus status;
    private final ItemStack itemSample;
    
    // Statistics
    private double totalSpent = 0.0;  // For buy orders: total coins spent
    private double totalEarned = 0.0; // For sell orders: total coins earned

    public BazaarOrder(
            String orderId,
            UUID owner,
            UUID profileId,
            String ownerName,
            String productId,
            OrderType orderType,
            double unitPrice,
            int totalAmount,
            long createdAt
    ) {
        this.orderId = orderId;
        this.owner = owner;
        this.profileId = profileId;
        this.ownerName = ownerName;
        this.productId = productId;
        this.orderType = orderType;
        this.unitPrice = unitPrice;
        this.totalAmount = totalAmount;
        this.remainingAmount = totalAmount;
        this.filledAmount = 0;
        this.createdAt = createdAt;
        this.expiresAt = createdAt + (7L * 24L * 60L * 60L * 1000L); // 7 days (Skyblock standard)
        this.status = OrderStatus.ACTIVE;
        this.itemSample = null;
    }

    public BazaarOrder(
            String orderId,
            UUID owner,
            UUID profileId,
            String ownerName,
            String productId,
            OrderType orderType,
            double unitPrice,
            int totalAmount,
            long createdAt,
            long expiresAt
    ) {
        this.orderId = orderId;
        this.owner = owner;
        this.profileId = profileId;
        this.ownerName = ownerName;
        this.productId = productId;
        this.orderType = orderType;
        this.unitPrice = unitPrice;
        this.totalAmount = totalAmount;
        this.remainingAmount = totalAmount;
        this.filledAmount = 0;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.status = OrderStatus.ACTIVE;
        this.itemSample = null;
    }
    
    public BazaarOrder(
            String orderId,
            UUID owner,
            UUID profileId,
            String ownerName,
            String productId,
            OrderType orderType,
            double unitPrice,
            int totalAmount,
            int remainingAmount,
            int filledAmount,
            long createdAt,
            long expiresAt,
            OrderStatus status,
            ItemStack itemSample,
            double totalSpent,
            double totalEarned
    ) {
        this.orderId = orderId;
        this.owner = owner;
        this.profileId = profileId;
        this.ownerName = ownerName;
        this.productId = productId;
        this.orderType = orderType;
        this.unitPrice = unitPrice;
        this.totalAmount = totalAmount;
        this.remainingAmount = remainingAmount;
        this.filledAmount = filledAmount;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.status = status;
        this.itemSample = itemSample;
        this.totalSpent = totalSpent;
        this.totalEarned = totalEarned;
    }

    // Getters
    public String getOrderId() { return orderId; }
    public UUID getOwner() { return owner; }
    public UUID getProfileId() { return profileId; }
    public String getOwnerName() { return ownerName; }
    public String getProductId() { return productId; }
    public OrderType getOrderType() { return orderType; }
    public double getUnitPrice() { return unitPrice; }
    public int getTotalAmount() { return totalAmount; }
    public int getRemainingAmount() { return remainingAmount; }
    public int getFilledAmount() { return filledAmount; }
    public long getCreatedAt() { return createdAt; }
    public long getExpiresAt() { return expiresAt; }
    public OrderStatus getStatus() { return status; }
    public ItemStack getItemSample() { return itemSample; }
    public double getTotalSpent() { return totalSpent; }
    public double getTotalEarned() { return totalEarned; }
    
    /**
     * Fill part of this order.
     * @param amount Amount to fill
     * @param price Actual price per unit (may be better than order price)
     * @return Amount actually filled
     */
    public int fill(int amount, double price) {
        int toFill = Math.min(amount, remainingAmount);
        if (toFill <= 0) return 0;
        
        remainingAmount -= toFill;
        filledAmount += toFill;
        
        if (orderType == OrderType.BUY) {
            totalSpent += toFill * price;
        } else {
            totalEarned += toFill * price;
        }
        
        // Update status
        if (remainingAmount <= 0) {
            status = OrderStatus.FILLED;
        } else if (filledAmount > 0) {
            status = OrderStatus.PARTIALLY_FILLED;
        }
        
        return toFill;
    }
    
    /**
     * Cancel this order.
     */
    public void cancel() {
        status = OrderStatus.CANCELLED;
    }

    /**
     * Expire this order.
     */
    public void expire() {
        status = OrderStatus.EXPIRED;
    }
    
    /**
     * Check if this order is expired.
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
    
    /**
     * Check if this order can still be filled.
     */
    public boolean isActive() {
        return status == OrderStatus.ACTIVE || status == OrderStatus.PARTIALLY_FILLED;
    }
    
    /**
     * Get the total value of remaining items.
     */
    public double getRemainingValue() {
        return remainingAmount * unitPrice;
    }
    
    /**
     * Get the total value of filled items.
     */
    public double getFilledValue() {
        return orderType == OrderType.BUY ? totalSpent : totalEarned;
    }
    
    /**
     * Create a display ItemStack for this order.
     */
    public ItemStack createDisplayItem(String productName, Material icon) {
        Material displayIcon = icon != null ? icon : Material.PAPER;
        ItemStack display = new ItemStack(displayIcon);
        display.setAmount(1);
        
        var meta = display.getItemMeta();
        if (meta == null) return display;
        
        meta.setDisplayName(orderType.getDisplayName() + " §8- §7" + productName);
        
        var lore = new java.util.ArrayList<String>();
        lore.add("");
        lore.add("§7Quantity: §e" + totalAmount + "x");
        lore.add("§7Unit Price: §6" + String.format("%.1f coins", unitPrice));
        lore.add("");
        
        if (filledAmount > 0) {
            double percent = (filledAmount * 100.0) / totalAmount;
            lore.add("§a§lFilled: " + String.format("%.1f%%", percent));
            lore.add("§7Status: §a" + filledAmount + " §8/ §a" + totalAmount + " §7filled");
            lore.add("");
        } else {
            lore.add("§7Status: §ePending...");
            lore.add("");
        }
        
        lore.add("§7Expires: §e" + getExpiryString());
        lore.add("");
        lore.add("§cClick to cancel!");
        
        meta.setLore(lore);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES, org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        display.setItemMeta(meta);
        
        return display;
    }
    
    private String getStatusColor() {
        return switch (status) {
            case ACTIVE -> "§a";
            case PARTIALLY_FILLED -> "§e";
            case FILLED -> "§a";
            case CANCELLED -> "§c";
            case EXPIRED -> "§8";
        };
    }
    
    private String getExpiryString() {
        long remaining = expiresAt - System.currentTimeMillis();
        if (remaining <= 0) return "Expired";
        
        long hours = remaining / (1000 * 60 * 60);
        long minutes = (remaining % (1000 * 60 * 60)) / (1000 * 60);
        
        if (hours > 24) {
            return (hours / 24) + " days";
        } else if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m";
        }
    }
    
    public void save(ConfigurationSection section) {
        section.set("owner", owner.toString());
        section.set("profile-id", profileId.toString());
        section.set("owner-name", ownerName);
        section.set("product", productId);
        section.set("type", orderType.name());
        section.set("unit-price", unitPrice);
        section.set("total-amount", totalAmount);
        section.set("remaining-amount", remainingAmount);
        section.set("filled-amount", filledAmount);
        section.set("created-at", createdAt);
        section.set("expires-at", expiresAt);
        section.set("status", status.name());
        section.set("total-spent", totalSpent);
        section.set("total-earned", totalEarned);
        if (itemSample != null) {
            section.set("sample-item", itemSample);
        }
    }

    public static BazaarOrder fromSection(String id, ConfigurationSection section) {
        if (section == null) return null;
        
        String ownerString = section.getString("owner");
        UUID owner;
        try {
            owner = UUID.fromString(ownerString);
        } catch (Exception ex) {
            return null;
        }

        UUID profileId = owner; // Legacy fallback: old orders were implicitly per-player.
        String profileIdString = section.getString("profile-id");
        if (profileIdString != null && !profileIdString.isBlank()) {
            try {
                profileId = UUID.fromString(profileIdString);
            } catch (Exception ex) {
                return null;
            }
        }
        
        String ownerName = section.getString("owner-name", "Unknown");
        String productId = section.getString("product", "");
        
        OrderType type;
        try {
            type = OrderType.valueOf(section.getString("type", "BUY"));
        } catch (IllegalArgumentException ex) {
            type = OrderType.BUY;
        }
        
        double unitPrice = section.getDouble("unit-price", 0.0);
        int totalAmount = section.getInt("total-amount", 0);
        int remainingAmount = section.getInt("remaining-amount", totalAmount);
        int filledAmount = section.getInt("filled-amount", 0);
        long createdAt = section.getLong("created-at", System.currentTimeMillis());
        long expiresAt = section.getLong("expires-at", createdAt + (36L * 60 * 60 * 1000));
        
        OrderStatus status;
        try {
            status = OrderStatus.valueOf(section.getString("status", "ACTIVE"));
        } catch (IllegalArgumentException ex) {
            status = OrderStatus.ACTIVE;
        }
        
        double totalSpent = section.getDouble("total-spent", 0.0);
        double totalEarned = section.getDouble("total-earned", 0.0);
        
        ItemStack sample = null;
        if (section.isItemStack("sample-item")) {
            sample = section.getItemStack("sample-item");
        }
        
        return new BazaarOrder(
            id, owner, profileId, ownerName, productId, type, unitPrice,
            totalAmount, remainingAmount, filledAmount,
            createdAt, expiresAt, status, sample, totalSpent, totalEarned
        );
    }
}
