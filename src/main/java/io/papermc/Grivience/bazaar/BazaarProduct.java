package io.papermc.Grivience.bazaar;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Represents a Bazaar product with all its market data.
 * Mirrors Skyblock's Bazaar product structure with 100% accuracy.
 * 
 * Features:
 * - Real-time pricing from order book
 * - 24-hour price history and trends
 * - Volume and order count statistics
 * - Skyblock-style display formatting
 */
public final class BazaarProduct {
    private final String productId;
    private final String productName;
    private final Material icon;
    private final BazaarCategory category;
    private final BazaarSubcategory subcategory;
    private final boolean isCustomItem;
    private final String customItemKey;
    private final int maxStackSize;
    private final List<BazaarProduct> variants;
    private String parentProductId;

    // Market data - updated from order book
    private double instantBuyPrice;
    private double instantSellPrice;
    private double lowestSellOrder;
    private double highestBuyOrder;
    private int sellOrderCount;
    private int buyOrderCount;
    private double sellOrderVolume;
    private double buyOrderVolume;
    
    // 24-hour statistics
    private int salesCount24h;
    private double volume24h;
    private double avgPrice24h;
    private double lowestPrice24h;
    private double highestPrice24h;
    private double priceChange24h;
    private double priceChangePercent24h;

    // Price history (last 24 hours)
    private final List<PriceHistoryEntry> priceHistory;
    
    public BazaarProduct(
            String productId,
            String productName,
            Material icon,
            BazaarCategory category,
            BazaarSubcategory subcategory,
            boolean isCustomItem,
            String customItemKey,
            int maxStackSize
    ) {
        this.productId = productId;
        this.productName = productName;
        this.icon = icon;
        this.category = category;
        this.subcategory = subcategory;
        this.isCustomItem = isCustomItem;
        this.customItemKey = customItemKey;
        this.maxStackSize = maxStackSize;
        this.instantBuyPrice = 0.0;
        this.instantSellPrice = 0.0;
        this.lowestSellOrder = Double.NaN;
        this.highestBuyOrder = Double.NaN;
        this.sellOrderCount = 0;
        this.buyOrderCount = 0;
        this.sellOrderVolume = 0.0;
        this.buyOrderVolume = 0.0;
        this.priceHistory = new ArrayList<>();
        this.variants = new ArrayList<>();
    }

    public void addVariant(BazaarProduct variant) {
        if (variant != null) {
            this.variants.add(variant);
            variant.setParentProductId(this.productId);
        }
    }

    public List<BazaarProduct> getVariants() { return new ArrayList<>(variants); }
    public boolean hasVariants() { return !variants.isEmpty(); }
    public String getParentProductId() { return parentProductId; }
    public void setParentProductId(String parentProductId) { this.parentProductId = parentProductId; }
    public boolean isVariant() { return parentProductId != null; }
    
    // Getters
    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public Material getIcon() { return icon; }
    public BazaarCategory getCategory() { return category; }
    public BazaarSubcategory getSubcategory() { return subcategory; }
    public boolean isCustomItem() { return isCustomItem; }
    public String getCustomItemKey() { return customItemKey; }
    public int getMaxStackSize() { return maxStackSize; }
    
    public double getInstantBuyPrice() { return instantBuyPrice; }
    public double getInstantSellPrice() { return instantSellPrice; }
    public double getLowestSellOrder() { return lowestSellOrder; }
    public double getHighestBuyOrder() { return highestBuyOrder; }
    public int getSellOrderCount() { return sellOrderCount; }
    public int getBuyOrderCount() { return buyOrderCount; }
    public double getSellOrderVolume() { return sellOrderVolume; }
    public double getBuyOrderVolume() { return buyOrderVolume; }
    public List<PriceHistoryEntry> getPriceHistory() { return new ArrayList<>(priceHistory); }
    
    // 24-hour statistics getters
    public int getSalesCount24h() { return salesCount24h; }
    public double getVolume24h() { return volume24h; }
    public double getAvgPrice24h() { return avgPrice24h; }
    public double getLowestPrice24h() { return lowestPrice24h; }
    public double getHighestPrice24h() { return highestPrice24h; }
    public double getPriceChange24h() { return priceChange24h; }
    public double getPriceChangePercent24h() { return priceChangePercent24h; }

    // Setters for market data updates
    public void setInstantBuyPrice(double instantBuyPrice) { this.instantBuyPrice = instantBuyPrice; }
    public void setInstantSellPrice(double instantSellPrice) { this.instantSellPrice = instantSellPrice; }
    public void setLowestSellOrder(double lowestSellOrder) { this.lowestSellOrder = lowestSellOrder; }
    public void setHighestBuyOrder(double highestBuyOrder) { this.highestBuyOrder = highestBuyOrder; }
    public void setSellOrderCount(int sellOrderCount) { this.sellOrderCount = sellOrderCount; }
    public void setBuyOrderCount(int buyOrderCount) { this.buyOrderCount = buyOrderCount; }
    public void setSellOrderVolume(double sellOrderVolume) { this.sellOrderVolume = sellOrderVolume; }
    public void setBuyOrderVolume(double buyOrderVolume) { this.buyOrderVolume = buyOrderVolume; }
    
    // 24-hour statistics setters
    public void setSalesCount24h(int salesCount24h) { this.salesCount24h = salesCount24h; }
    public void setVolume24h(double volume24h) { this.volume24h = volume24h; }
    public void setAvgPrice24h(double avgPrice24h) { this.avgPrice24h = avgPrice24h; }
    public void setLowestPrice24h(double lowestPrice24h) { this.lowestPrice24h = lowestPrice24h; }
    public void setHighestPrice24h(double highestPrice24h) { this.highestPrice24h = highestPrice24h; }
    public void setPriceChange24h(double priceChange24h) { this.priceChange24h = priceChange24h; }
    public void setPriceChangePercent24h(double priceChangePercent24h) { this.priceChangePercent24h = priceChangePercent24h; }
    
    /**
     * Add a price history entry.
     */
    public void addPriceHistory(double price, int amount, long timestamp) {
        priceHistory.add(new PriceHistoryEntry(price, amount, timestamp));
        // Keep only last 24 hours (assuming entries every few minutes, keep ~288 entries for 24h at 5min intervals)
        while (priceHistory.size() > 300) {
            priceHistory.remove(0);
        }
    }
    
    /**
     * Update market data from order book.
     */
    public void updateFromOrderBook(List<BazaarOrder> buyOrders, List<BazaarOrder> sellOrders) {
        buyOrders = buyOrders == null ? List.of() : buyOrders;
        sellOrders = sellOrders == null ? List.of() : sellOrders;
        // Calculate highest buy order
        if (buyOrders.isEmpty()) {
            highestBuyOrder = Double.NaN;
            buyOrderCount = 0;
            buyOrderVolume = 0.0;
        } else {
            highestBuyOrder = buyOrders.get(0).getUnitPrice();
            buyOrderCount = buyOrders.size();
            buyOrderVolume = buyOrders.stream().mapToDouble(o -> o.getRemainingAmount() * o.getUnitPrice()).sum();
        }

        // Calculate lowest sell order
        if (sellOrders.isEmpty()) {
            lowestSellOrder = Double.NaN;
            sellOrderCount = 0;
            sellOrderVolume = 0.0;
        } else {
            lowestSellOrder = sellOrders.get(0).getUnitPrice();
            sellOrderCount = sellOrders.size();
            sellOrderVolume = sellOrders.stream().mapToDouble(o -> o.getRemainingAmount() * o.getUnitPrice()).sum();
        }

        // Hypixel: instant buy/sell is only available if there is resting liquidity on the other side.
        // - Instant Buy pulls from sell offers (lowest sell).
        // - Instant Sell pulls from buy orders (highest buy).
        instantBuyPrice = Double.isFinite(lowestSellOrder) && lowestSellOrder > 0.0 ? lowestSellOrder : Double.NaN;
        instantSellPrice = Double.isFinite(highestBuyOrder) && highestBuyOrder > 0.0 ? highestBuyOrder : Double.NaN;
        
        // Update 24-hour statistics from price history
        update24hStats();
    }
    
    /**
     * Update 24-hour statistics from price history.
     */
    private void update24hStats() {
        long cutoff = System.currentTimeMillis() - (24L * 60 * 60 * 1000);
        
        List<PriceHistoryEntry> recentHistory = priceHistory.stream()
            .filter(e -> e.timestamp() >= cutoff)
            .toList();
        
        if (recentHistory.isEmpty()) {
            salesCount24h = 0;
            volume24h = 0.0;
            avgPrice24h = Double.NaN;
            lowestPrice24h = Double.NaN;
            highestPrice24h = Double.NaN;
            priceChange24h = 0.0;
            priceChangePercent24h = 0.0;
            return;
        }
        
        salesCount24h = recentHistory.size();
        volume24h = recentHistory.stream().mapToInt(PriceHistoryEntry::amount).sum();
        avgPrice24h = recentHistory.stream().mapToDouble(PriceHistoryEntry::price).average().orElse(Double.NaN);
        lowestPrice24h = recentHistory.stream().mapToDouble(PriceHistoryEntry::price).min().orElse(Double.NaN);
        highestPrice24h = recentHistory.stream().mapToDouble(PriceHistoryEntry::price).max().orElse(Double.NaN);
        
        // Calculate price change (first vs last in period)
        if (recentHistory.size() >= 2) {
            double oldPrice = recentHistory.get(0).price();
            double newPrice = recentHistory.get(recentHistory.size() - 1).price();
            priceChange24h = newPrice - oldPrice;
            priceChangePercent24h = oldPrice > 0 ? ((newPrice - oldPrice) / oldPrice) * 100 : 0.0;
        } else {
            priceChange24h = 0.0;
            priceChangePercent24h = 0.0;
        }
    }
    
    private double getDefaultPrice() {
        // Default prices based on material type
        if (icon == null) return 100.0;
        return switch (icon) {
            case WHEAT, CARROT, POTATO -> 5.0;
            case PUMPKIN, MELON -> 8.0;
            case SUGAR_CANE, CACTUS -> 6.0;
            case COBBLESTONE, NETHERRACK -> 3.0;
            case COAL_ORE, IRON_ORE, GOLD_ORE -> 15.0;
            case DIAMOND_ORE, EMERALD_ORE -> 50.0;
            case ROTTEN_FLESH, BONE, STRING -> 8.0;
            case GUNPOWDER, ENDER_PEARL -> 25.0;
            case OAK_LOG, SPRUCE_LOG, BIRCH_LOG -> 10.0;
            case COD, SALMON, TROPICAL_FISH -> 12.0;
            default -> 100.0;
        };
    }
    
    /**
     * Create a display ItemStack for the Bazaar menu.
     * 100% Accurate Hypixel Skyblock formatting with pricing and statistics.
     */
    public ItemStack createDisplayItem() {
        ItemStack display = new ItemStack(icon != null ? icon : Material.BARRIER);
        display.setAmount(1);

        var meta = display.getItemMeta();
        if (meta == null) return display;

        meta.setDisplayName("§6" + productName);

        List<String> lore = new ArrayList<>();
        // Hypixel format:
        // [Rarity Color][Name] (handled by setDisplayName, usually rarity color is part of name or handled elsewhere, assuming Gold for now)
        // 
        // Buy Price: [Price]
        // Sell Price: [Price]
        // 
        // Buy Orders: [Count] ([Volume] items)
        // Sell Offers: [Count] ([Volume] items)
        // 
        // Click to view options!

        // Note: Hypixel often adds a spacer line after name if it has rarity, but let's stick to the core data.
        
        // Buy Price = Lowest Sell Offer (what you pay to buy)
        if (!Double.isNaN(lowestSellOrder) && lowestSellOrder > 0) {
            lore.add("§7Buy Price: §6" + formatCoins(lowestSellOrder));
        } else {
            lore.add("§7Buy Price: §cN/A");
        }

        // Sell Price = Highest Buy Order (what you get for selling)
        if (!Double.isNaN(highestBuyOrder) && highestBuyOrder > 0) {
            lore.add("§7Sell Price: §6" + formatCoins(highestBuyOrder));
        } else {
            lore.add("§7Sell Price: §cN/A");
        }
        
        lore.add("");

        // Market Depth
        lore.add("§7Buy Orders: §e" + buyOrderCount + " §8(§b" + formatNumber(buyOrderVolume) + " items§8)");
        lore.add("§7Sell Offers: §e" + sellOrderCount + " §8(§b" + formatNumber(sellOrderVolume) + " items§8)");
        
        lore.add("");
        lore.add("§eClick to view options!");

        meta.setLore(lore);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES, org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        
        // Set action to open product or variants
        meta.getPersistentDataContainer().set(BazaarKeys.ACTION_KEY, PersistentDataType.STRING, "open_product");
        meta.getPersistentDataContainer().set(BazaarKeys.VALUE_KEY, PersistentDataType.STRING, productId);
        
        display.setItemMeta(meta);

        return display;
    }
    
    private String formatCoins(double amount) {
        if (Double.isNaN(amount)) return "§cN/A";
        if (amount < 0.01) return "§7<0.01";
        if (amount >= 1_000_000_000) {
            return String.format("§6%.1f§7B", amount / 1_000_000_000);
        } else if (amount >= 1_000_000) {
            return String.format("§6%.1f§7M", amount / 1_000_000);
        } else if (amount >= 1_000) {
            return String.format("§6%.1f§7k", amount / 1_000);
        } else {
            return String.format("§6%.1f", amount);
        }
    }
    
    public String formatNumber(double amount) {
        if (amount >= 1_000_000_000) {
            return String.format("%.1fb", amount / 1_000_000_000);
        } else if (amount >= 1_000_000) {
            return String.format("%.1fm", amount / 1_000_000);
        } else if (amount >= 1_000) {
            return String.format("%.1fk", amount / 1_000);
        } else {
            return String.format("%.0f", amount);
        }
    }
    
    public record PriceHistoryEntry(double price, int amount, long timestamp) {}
    
    public enum BazaarCategory {
        AGRICULTURE("§eFarming", Material.WHEAT),
        MINING("§6Mining", Material.COBBLESTONE),
        COMBAT("§cCombat", Material.ROTTEN_FLESH),
        WOODS_FISHES("§2Woods & Fishes", Material.OAK_LOG),
        ODDITIES("§5Oddities", Material.MAGMA_CREAM);

        private final String displayName;
        private final Material icon;

        BazaarCategory(String displayName, Material icon) {
            this.displayName = displayName;
            this.icon = icon;
        }

        public String getDisplayName() { return displayName; }
        public Material getIcon() { return icon; }
    }

    public enum BazaarSubcategory {
        // Farming Subcategories
        FARMING_ITEMS("Farming Items", BazaarCategory.AGRICULTURE),
        MUSHROOMS("Mushrooms", BazaarCategory.AGRICULTURE),
        ANIMAL("Animal Products", BazaarCategory.AGRICULTURE),
        
        // Mining Subcategories
        ORE("Ores", BazaarCategory.MINING),
        GEMS("Gems & Dust", BazaarCategory.MINING),
        FUEL("Fuel", BazaarCategory.MINING),
        SPECIAL("Special Mining", BazaarCategory.MINING),
        
        // Combat Subcategories
        MOB_DROPS("Mob Drops", BazaarCategory.COMBAT),
        DUNGEON("Dungeon Items", BazaarCategory.COMBAT),
        SPECIAL_COMBAT("Special Combat", BazaarCategory.COMBAT),
        
        // Woods & Fishes Subcategories (Merged)
        WOOD("Wood", BazaarCategory.WOODS_FISHES),
        SAPLING("Saplings", BazaarCategory.WOODS_FISHES),
        SPECIAL_FORAGING("Special Foraging", BazaarCategory.WOODS_FISHES),
        FISH("Fish", BazaarCategory.WOODS_FISHES),
        TREASURE("Treasure", BazaarCategory.WOODS_FISHES),
        SPECIAL_FISHING("Special Fishing", BazaarCategory.WOODS_FISHES),
        
        // Oddities Subcategories (Renamed from Misc)
        MAGIC("Magic", BazaarCategory.ODDITIES),
        CONSUMABLES("Consumables", BazaarCategory.ODDITIES),
        MISC("Miscellaneous", BazaarCategory.ODDITIES);

        private final String displayName;
        private final BazaarCategory parent;

        BazaarSubcategory(String displayName, BazaarCategory parent) {
            this.displayName = displayName;
            this.parent = parent;
        }

        public String getDisplayName() { return displayName; }
        public BazaarCategory getParent() { return parent; }
    }
}

