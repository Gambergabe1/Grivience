package io.papermc.Grivience.bazaar;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks price history for Bazaar products.
 * Mirrors Skyblock's price history tracking.
 */
public final class BazaarPriceHistory {
    private final File file;
    private final Map<String, ProductPriceHistory> histories = new ConcurrentHashMap<>();
    private final int maxEntriesPerProduct;
    private final long historyDuration;

    public BazaarPriceHistory(File dataFolder) {
        this(dataFolder, 500, 7L * 24 * 60 * 60 * 1000); // 500 entries, 7 days
    }
    
    public BazaarPriceHistory(File dataFolder, int maxEntriesPerProduct, long historyDuration) {
        this.file = new File(dataFolder, "bazaar_history.yml");
        this.maxEntriesPerProduct = maxEntriesPerProduct;
        this.historyDuration = historyDuration;
        load();
    }
    
    /**
     * Load price history from file.
     */
    public synchronized void load() {
        histories.clear();
        
        if (!file.exists()) {
            return;
        }
        
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection historySection = yaml.getConfigurationSection("history");
        
        if (historySection != null) {
            for (String productId : historySection.getKeys(false)) {
                ConfigurationSection productSection = historySection.getConfigurationSection(productId);
                ProductPriceHistory history = ProductPriceHistory.fromSection(productSection, maxEntriesPerProduct);
                if (history != null) {
                    histories.put(productId, history);
                }
            }
        }
    }
    
    /**
     * Save price history to file.
     */
    public synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection historySection = yaml.createSection("history");
        
        for (Map.Entry<String, ProductPriceHistory> entry : histories.entrySet()) {
            ConfigurationSection productSection = historySection.createSection(entry.getKey());
            entry.getValue().save(productSection);
        }
        
        try {
            yaml.save(file);
        } catch (IOException ex) {
            System.err.println("Failed to save bazaar price history: " + ex.getMessage());
        }
    }
    
    /**
     * Record a transaction in price history.
     */
    public void recordTransaction(String productId, double price, int amount, TransactionType type) {
        ProductPriceHistory history = histories.computeIfAbsent(productId, k -> new ProductPriceHistory(maxEntriesPerProduct));
        history.addEntry(price, amount, type);
        
        // Cleanup old entries periodically
        if (history.getEntries().size() % 50 == 0) {
            cleanupOldEntries();
        }
    }
    
    /**
     * Get price history for a product.
     */
    public ProductPriceHistory getHistory(String productId) {
        return histories.getOrDefault(productId, new ProductPriceHistory(maxEntriesPerProduct));
    }
    
    /**
     * Get the average price over a time period.
     */
    public double getAveragePrice(String productId, long durationMs) {
        ProductPriceHistory history = getHistory(productId);
        return history.getAveragePrice(durationMs);
    }
    
    /**
     * Get the median price over a time period.
     */
    public double getMedianPrice(String productId, long durationMs) {
        ProductPriceHistory history = getHistory(productId);
        return history.getMedianPrice(durationMs);
    }
    
    /**
     * Get price trend (positive = increasing, negative = decreasing).
     */
    public double getPriceTrend(String productId, long durationMs) {
        ProductPriceHistory history = getHistory(productId);
        return history.getPriceTrend(durationMs);
    }
    
    /**
     * Get total volume over a time period.
     */
    public int getTotalVolume(String productId, long durationMs) {
        ProductPriceHistory history = getHistory(productId);
        return history.getTotalVolume(durationMs);
    }
    
    /**
     * Cleanup old entries from all histories.
     */
    public void cleanupOldEntries() {
        long cutoff = System.currentTimeMillis() - historyDuration;
        
        for (ProductPriceHistory history : histories.values()) {
            history.removeOlderThan(cutoff);
        }
        
        // Remove empty histories
        histories.entrySet().removeIf(e -> e.getValue().getEntries().isEmpty());
    }
    
    /**
     * Clear history for a specific product.
     */
    public void clearHistory(String productId) {
        histories.remove(productId);
        save();
    }
    
    /**
     * Transaction type for history tracking.
     */
    public enum TransactionType {
        INSTANT_BUY("§aInstant Buy"),
        INSTANT_SELL("§6Instant Sell"),
        BUY_ORDER("§eBuy Order"),
        SELL_ORDER("§dSell Order");
        
        private final String displayName;
        
        TransactionType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
    
    /**
     * Price history for a single product.
     */
    public static class ProductPriceHistory {
        private final List<PriceEntry> entries = new ArrayList<>();
        private final int maxEntries;
        
        public ProductPriceHistory(int maxEntries) {
            this.maxEntries = maxEntries;
        }
        
        public List<PriceEntry> getEntries() { return new ArrayList<>(entries); }
        
        public void addEntry(double price, int amount, TransactionType type) {
            entries.add(new PriceEntry(price, amount, type, System.currentTimeMillis()));
            
            // Trim to max entries
            while (entries.size() > maxEntries) {
                entries.remove(0);
            }
        }
        
        public void removeOlderThan(long timestamp) {
            entries.removeIf(e -> e.timestamp() < timestamp);
        }
        
        public double getAveragePrice(long durationMs) {
            long cutoff = System.currentTimeMillis() - durationMs;
            return entries.stream()
                .filter(e -> e.timestamp() >= cutoff)
                .mapToDouble(PriceEntry::price)
                .average()
                .orElse(Double.NaN);
        }
        
        public double getMedianPrice(long durationMs) {
            long cutoff = System.currentTimeMillis() - durationMs;
            List<Double> prices = entries.stream()
                .filter(e -> e.timestamp() >= cutoff)
                .map(PriceEntry::price)
                .sorted()
                .toList();
            
            if (prices.isEmpty()) return Double.NaN;
            
            int mid = prices.size() / 2;
            return prices.size() % 2 == 0 
                ? (prices.get(mid - 1) + prices.get(mid)) / 2.0 
                : prices.get(mid);
        }
        
        public double getPriceTrend(long durationMs) {
            long cutoff = System.currentTimeMillis() - durationMs;
            List<PriceEntry> filtered = entries.stream()
                .filter(e -> e.timestamp() >= cutoff)
                .toList();
            
            if (filtered.size() < 2) return 0.0;
            
            // Simple linear regression slope
            double firstHalf = filtered.subList(0, filtered.size() / 2).stream()
                .mapToDouble(PriceEntry::price).average().orElse(0.0);
            double secondHalf = filtered.subList(filtered.size() / 2, filtered.size()).stream()
                .mapToDouble(PriceEntry::price).average().orElse(0.0);
            
            return secondHalf - firstHalf;
        }
        
        public int getTotalVolume(long durationMs) {
            long cutoff = System.currentTimeMillis() - durationMs;
            return entries.stream()
                .filter(e -> e.timestamp() >= cutoff)
                .mapToInt(PriceEntry::amount)
                .sum();
        }
        
        public double getLowestPrice(long durationMs) {
            long cutoff = System.currentTimeMillis() - durationMs;
            return entries.stream()
                .filter(e -> e.timestamp() >= cutoff)
                .mapToDouble(PriceEntry::price)
                .min()
                .orElse(Double.NaN);
        }
        
        public double getHighestPrice(long durationMs) {
            long cutoff = System.currentTimeMillis() - durationMs;
            return entries.stream()
                .filter(e -> e.timestamp() >= cutoff)
                .mapToDouble(PriceEntry::price)
                .max()
                .orElse(Double.NaN);
        }
        
        public void save(ConfigurationSection section) {
            ConfigurationSection entriesSection = section.createSection("entries");
            int index = 0;
            for (PriceEntry entry : entries) {
                ConfigurationSection entrySection = entriesSection.createSection(String.valueOf(index));
                entrySection.set("price", entry.price);
                entrySection.set("amount", entry.amount);
                entrySection.set("type", entry.type().name());
                entrySection.set("timestamp", entry.timestamp);
                index++;
            }
        }
        
        public static ProductPriceHistory fromSection(ConfigurationSection section, int maxEntries) {
            if (section == null) return new ProductPriceHistory(maxEntries);
            
            ProductPriceHistory history = new ProductPriceHistory(maxEntries);
            ConfigurationSection entriesSection = section.getConfigurationSection("entries");
            
            if (entriesSection != null) {
                for (String key : entriesSection.getKeys(false)) {
                    ConfigurationSection entrySection = entriesSection.getConfigurationSection(key);
                    if (entrySection != null) {
                        double price = entrySection.getDouble("price", 0.0);
                        int amount = entrySection.getInt("amount", 0);
                        TransactionType type;
                        try {
                            type = TransactionType.valueOf(entrySection.getString("type", "INSTANT_BUY"));
                        } catch (IllegalArgumentException ex) {
                            type = TransactionType.INSTANT_BUY;
                        }
                        long timestamp = entrySection.getLong("timestamp", System.currentTimeMillis());
                        history.entries.add(new PriceEntry(price, amount, type, timestamp));
                    }
                }
            }
            
            return history;
        }
    }
    
    /**
     * A single price entry.
     */
    public record PriceEntry(double price, int amount, TransactionType type, long timestamp) {}
}

