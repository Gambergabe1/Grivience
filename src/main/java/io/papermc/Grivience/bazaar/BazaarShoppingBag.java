package io.papermc.Grivience.bazaar;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages profile shopping bags for Bazaar purchases and sales.
 * Mirrors Skyblock's shopping bag system where items/coins are claimed after orders fill.
 */
public final class BazaarShoppingBag {
    private final File file;
    private final Map<UUID, PlayerShoppingBag> profileBags = new HashMap<>();

    public BazaarShoppingBag(File dataFolder) {
        this.file = new File(dataFolder, "bazaar_bags.yml");
        load();
    }
    
    /**
     * Load shopping bags from file.
     */
    public synchronized void load() {
        profileBags.clear();
        
        if (!file.exists()) {
            return;
        }
        
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection bagsSection = yaml.getConfigurationSection("bags");
        
        if (bagsSection != null) {
            for (String uuidString : bagsSection.getKeys(false)) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(uuidString);
                } catch (IllegalArgumentException ex) {
                    continue;
                }
                
                ConfigurationSection bagSection = bagsSection.getConfigurationSection(uuidString);
                if (bagSection == null) continue;
                
                PlayerShoppingBag bag = PlayerShoppingBag.fromSection(bagSection);
                if (bag != null) {
                    profileBags.put(uuid, bag);
                }
            }
        }
    }
    
    /**
     * Save shopping bags to file.
     */
    public synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection bagsSection = yaml.createSection("bags");
        
        for (Map.Entry<UUID, PlayerShoppingBag> entry : profileBags.entrySet()) {
            ConfigurationSection bagSection = bagsSection.createSection(entry.getKey().toString());
            entry.getValue().save(bagSection);
        }
        
        try {
            yaml.save(file);
        } catch (IOException ex) {
            System.err.println("Failed to save bazaar shopping bags: " + ex.getMessage());
        }
    }
    
    /**
     * Get a profile's shopping bag.
     */
    public synchronized PlayerShoppingBag getBag(UUID profileId) {
        return profileBags.computeIfAbsent(profileId, k -> new PlayerShoppingBag());
    }

    /**
     * Migrate a legacy bag (stored under owner UUID) into the active Skyblock profile UUID.
     * This prevents cross-profile claims while avoiding item/coin loss after upgrading.
     */
    public synchronized boolean migrateLegacyBag(UUID ownerId, UUID targetProfileId) {
        if (ownerId == null || targetProfileId == null || ownerId.equals(targetProfileId)) {
            return false;
        }

        PlayerShoppingBag legacy = profileBags.get(ownerId);
        if (legacy == null || legacy.isEmpty()) {
            return false;
        }

        PlayerShoppingBag target = profileBags.get(targetProfileId);
        if (target == null || target.isEmpty()) {
            profileBags.put(targetProfileId, legacy);
        } else {
            for (Map.Entry<String, Integer> entry : legacy.getItems().entrySet()) {
                target.addItems(entry.getKey(), entry.getValue());
            }
            target.addCoins(legacy.getCoins());
        }

        profileBags.remove(ownerId);
        save();
        return true;
    }
    
    /**
     * Add items to a profile's shopping bag (from filled buy orders).
     */
    public synchronized void addItems(UUID profileId, String productId, int amount) {
        if (profileId == null || productId == null || productId.isBlank() || amount <= 0) {
            return;
        }
        PlayerShoppingBag bag = getBag(profileId);
        bag.addItems(productId, amount);
        save();
    }
    
    /**
     * Add coins to a profile's shopping bag (from filled sell orders).
     */
    public synchronized void addCoins(UUID profileId, double amount) {
        if (profileId == null || !Double.isFinite(amount) || amount <= 0.0) {
            return;
        }
        PlayerShoppingBag bag = getBag(profileId);
        bag.addCoins(amount);
        save();
    }
    
    /**
     * Claim all items from a profile's shopping bag.
     * @return Map of product IDs to amounts claimed
     */
    public synchronized Map<String, Integer> claimItems(UUID profileId) {
        PlayerShoppingBag bag = profileBags.get(profileId);
        if (bag == null) return new HashMap<>();
        
        Map<String, Integer> items = new HashMap<>(bag.getItems());
        bag.clearItems();
        save();
        
        return items;
    }
    
    /**
     * Claim all coins from a profile's shopping bag.
     * @return Amount of coins claimed
     */
    public synchronized double claimCoins(UUID profileId) {
        PlayerShoppingBag bag = profileBags.get(profileId);
        if (bag == null) return 0.0;
        
        double coins = bag.getCoins();
        bag.clearCoins();
        save();
        
        return coins;
    }
    
    /**
     * Claim everything from a profile's shopping bag.
     */
    public synchronized BagContents claimAll(UUID profileId) {
        PlayerShoppingBag bag = profileBags.get(profileId);
        if (bag == null) return new BagContents(new HashMap<>(), 0.0);
        
        BagContents contents = new BagContents(new HashMap<>(bag.getItems()), bag.getCoins());
        bag.clear();
        save();
        
        return contents;
    }
    
    /**
     * Remove specific items from a profile's bag.
     */
    public synchronized void removeItems(UUID profileId, String productId, int amount) {
        if (profileId == null || productId == null || productId.isBlank() || amount <= 0) {
            return;
        }
        PlayerShoppingBag bag = profileBags.get(profileId);
        if (bag == null) return;
        
        bag.removeItems(productId, amount);
        save();
    }
    
    /**
     * Get total value of a profile's shopping bag.
     */
    public synchronized double getBagValue(UUID profileId, BazaarProductCache productCache) {
        PlayerShoppingBag bag = profileBags.get(profileId);
        if (bag == null) return 0.0;
        
        double total = bag.getCoins();
        for (Map.Entry<String, Integer> entry : bag.getItems().entrySet()) {
            BazaarProduct product = productCache.getProduct(entry.getKey());
            if (product != null) {
                double price = product.getInstantBuyPrice();
                if (Double.isFinite(price) && price > 0.0D) {
                    total += entry.getValue() * price;
                }
            }
        }
        
        return total;
    }
    
    /**
     * Contents of a shopping bag claim.
     */
    public record BagContents(Map<String, Integer> items, double coins) {}
    
    /**
     * A player's shopping bag.
     */
    public static class PlayerShoppingBag {
        private final Map<String, Integer> items = new HashMap<>();
        private double coins = 0.0;
        private long lastUpdated = System.currentTimeMillis();
        
        public Map<String, Integer> getItems() { return new HashMap<>(items); }
        public double getCoins() { return coins; }
        public long getLastUpdated() { return lastUpdated; }
        
        public void addItems(String productId, int amount) {
            items.merge(productId, amount, Integer::sum);
            lastUpdated = System.currentTimeMillis();
        }
        
        public void addCoins(double amount) {
            coins += amount;
            lastUpdated = System.currentTimeMillis();
        }
        
        public void removeItems(String productId, int amount) {
            Integer current = items.get(productId);
            if (current == null) return;
            
            int remaining = current - amount;
            if (remaining <= 0) {
                items.remove(productId);
            } else {
                items.put(productId, remaining);
            }
            lastUpdated = System.currentTimeMillis();
        }
        
        public void clearItems() {
            items.clear();
            lastUpdated = System.currentTimeMillis();
        }
        
        public void clearCoins() {
            coins = 0.0;
            lastUpdated = System.currentTimeMillis();
        }
        
        public void clear() {
            items.clear();
            coins = 0.0;
            lastUpdated = System.currentTimeMillis();
        }
        
        public boolean hasItems() { return !items.isEmpty(); }
        public boolean hasCoins() { return coins > 0.0; }
        public boolean isEmpty() { return items.isEmpty() && coins <= 0.0; }
        
        public void save(ConfigurationSection section) {
            section.set("coins", coins);
            section.set("last-updated", lastUpdated);
            
            ConfigurationSection itemsSection = section.createSection("items");
            for (Map.Entry<String, Integer> entry : items.entrySet()) {
                itemsSection.set(entry.getKey(), entry.getValue());
            }
        }
        
        public static PlayerShoppingBag fromSection(ConfigurationSection section) {
            if (section == null) return null;
            
            PlayerShoppingBag bag = new PlayerShoppingBag();
            bag.coins = section.getDouble("coins", 0.0);
            bag.lastUpdated = section.getLong("last-updated", System.currentTimeMillis());
            
            ConfigurationSection itemsSection = section.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String productId : itemsSection.getKeys(false)) {
                    int amount = itemsSection.getInt(productId, 0);
                    if (amount > 0) {
                        bag.items.put(productId, amount);
                    }
                }
            }
            
            return bag;
        }
    }
    
    /**
     * Simple product cache interface for value calculation.
     */
    public interface BazaarProductCache {
        BazaarProduct getProduct(String productId);
    }
}

