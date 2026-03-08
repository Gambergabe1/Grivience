package io.papermc.Grivience.bazaar;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the Bazaar order book with instant buy/sell matching.
 * Implements Skyblock's order matching system with 100% accuracy.
 * 
 * Features:
 * - Price-time priority matching (best price first, then oldest first)
 * - Partial order fills with shopping bag delivery
 * - Hypixel-style order expiry (configurable)
 */
public final class BazaarOrderBook {
    private final GriviencePlugin plugin;
    private final File file;

    // Orders indexed by product ID
    private final Map<String, List<BazaarOrder>> buyOrders = new ConcurrentHashMap<>();
    private final Map<String, List<BazaarOrder>> sellOrders = new ConcurrentHashMap<>();

    // Orders indexed by order ID for quick lookup
    private final Map<String, BazaarOrder> allOrders = new ConcurrentHashMap<>();

    // Profile's active orders
    private final Map<UUID, Set<String>> profileOrders = new ConcurrentHashMap<>();

    private int lastOrderId = 0;
    
    // Configuration
    private int maxOrdersPerPlayer;
    private long orderExpiryMs;
    private int maxOrderAmount;

    public BazaarOrderBook(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "bazaar_orders.yml");
        this.maxOrdersPerPlayer = 14;
        this.orderExpiryMs = 7L * 24L * 60L * 60L * 1000L; // 7 days
        this.maxOrderAmount = 71680; // Hypixel instant buy cap

        load();
    }

    public synchronized void reloadFromConfig(ConfigurationSection section) {
        if (section == null) {
            return;
        }

        maxOrdersPerPlayer = Math.max(1, section.getInt("max-orders-per-player", maxOrdersPerPlayer));
        maxOrderAmount = Math.max(1, section.getInt("max-order-amount", maxOrderAmount));

        double expiryHours = section.getDouble("order-expiry-hours", orderExpiryMs / 3600000.0);
        orderExpiryMs = Math.max(0L, Math.round(expiryHours * 3600000.0));
    }

    public synchronized BazaarOrder createOrder(
            String orderId,
            UUID owner,
            UUID profileId,
            String ownerName,
            String productId,
            BazaarOrder.OrderType orderType,
            double unitPrice,
            int totalAmount,
            long createdAt
    ) {
        long expiresAt = createdAt + orderExpiryMs;
        return new BazaarOrder(
                orderId,
                owner,
                profileId,
                ownerName,
                productId,
                orderType,
                unitPrice,
                totalAmount,
                createdAt,
                expiresAt
        );
    }

    public synchronized void load() {
        buyOrders.clear();
        sellOrders.clear();
        allOrders.clear();
        profileOrders.clear();

        if (!file.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        lastOrderId = Math.max(yaml.getInt("last-order-id", 0), yaml.getInt("last-id", 0));

        ConfigurationSection ordersSection = yaml.getConfigurationSection("orders");
        if (ordersSection == null) {
            ordersSection = yaml.getConfigurationSection("orderbook.orders");
        }
        if (ordersSection == null) {
            return;
        }

        for (String id : ordersSection.getKeys(false)) {
            BazaarOrder order = BazaarOrder.fromSection(id, ordersSection.getConfigurationSection(id));
            if (order == null) {
                continue;
            }

            allOrders.put(order.getOrderId(), order);
            if (order.getOrderType() == BazaarOrder.OrderType.BUY) {
                buyOrders.computeIfAbsent(order.getProductId(), ignored -> new ArrayList<>()).add(order);
            } else {
                sellOrders.computeIfAbsent(order.getProductId(), ignored -> new ArrayList<>()).add(order);
            }
            profileOrders.computeIfAbsent(order.getProfileId(), ignored -> ConcurrentHashMap.newKeySet()).add(order.getOrderId());

            int numeric = parseNumericOrderId(order.getOrderId());
            if (numeric > lastOrderId) {
                lastOrderId = numeric;
            }
        }
    }

    public synchronized void save() {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("last-order-id", lastOrderId);

        ConfigurationSection ordersSection = yaml.createSection("orders");
        for (BazaarOrder order : allOrders.values()) {
            ConfigurationSection section = ordersSection.createSection(order.getOrderId());
            order.save(section);
        }

        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to save bazaar order book: " + ex.getMessage());
        }
    }

    private int parseNumericOrderId(String orderId) {
        if (orderId == null) {
            return -1;
        }
        String digits = orderId.startsWith("ORD-") ? orderId.substring("ORD-".length()) : orderId;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }
    
    /**
     * Check if an order can be placed (validates limits and constraints).
     * @return Validation result with success status and error message if any
     */
    public synchronized ValidationResult validateOrder(
            UUID ownerId,
            UUID profileId,
            BazaarOrder.OrderType orderType,
            double unitPrice,
            int amount,
            int maxOrdersAllowed
    ) {
        if (ownerId == null) {
            return new ValidationResult(false, "§cInvalid player.");
        }
        if (profileId == null) {
            return new ValidationResult(false, "§cNo Skyblock profile selected.");
        }
        if (!Double.isFinite(unitPrice) || unitPrice <= 0.0) {
            return new ValidationResult(false, "§cInvalid price.");
        }
        if (amount <= 0) {
            return new ValidationResult(false, "§cInvalid amount.");
        }

        // Check profile order limit
        int limit = maxOrdersAllowed > 0 ? maxOrdersAllowed : maxOrdersPerPlayer;
        int currentOrders = getProfileOrders(profileId, ownerId).size();
        if (currentOrders >= limit) {
            return new ValidationResult(false, "§cYou have reached the maximum number of orders (§e" + limit + "§c).");
        }
        
        // Check amount limits
        if (amount > maxOrderAmount) {
            return new ValidationResult(false, "§cOrder amount must be between 1 and " + maxOrderAmount + ".");
        }
        
        return new ValidationResult(true, null);
    }
    
    /**
     * Generate a unique order ID.
     */
    public synchronized String generateOrderId() {
        lastOrderId++;
        return "ORD-" + String.format("%08d", lastOrderId);
    }
    
    /**
     * Place a new order.
     */
    public synchronized void placeOrder(BazaarOrder order) {
        allOrders.put(order.getOrderId(), order);

        if (order.getOrderType() == BazaarOrder.OrderType.BUY) {
            buyOrders.computeIfAbsent(order.getProductId(), k -> new ArrayList<>()).add(order);
            sortBuyOrders(order.getProductId());
        } else {
            sellOrders.computeIfAbsent(order.getProductId(), k -> new ArrayList<>()).add(order);
            sortSellOrders(order.getProductId());
        }

        profileOrders.computeIfAbsent(order.getProfileId(), k -> ConcurrentHashMap.newKeySet()).add(order.getOrderId());

        save();
    }
    
    /**
     * Remove an order.
     */
    public synchronized void removeOrder(String orderId) {
        BazaarOrder order = allOrders.remove(orderId);
        if (order == null) return;
        
        Map<String, List<BazaarOrder>> orderMap = order.getOrderType() == BazaarOrder.OrderType.BUY ? buyOrders : sellOrders;
        orderMap.computeIfPresent(order.getProductId(), (k, list) -> {
            list.removeIf(o -> o.getOrderId().equals(orderId));
            return list.isEmpty() ? null : list;
        });
        
        profileOrders.computeIfPresent(order.getProfileId(), (k, set) -> {
            set.remove(orderId);
            return set.isEmpty() ? null : set;
        });
    }
    
    /**
     * Get all active buy orders for a product, sorted by price (highest first), then time.
     */
    public synchronized List<BazaarOrder> getBuyOrders(String productId) {
        List<BazaarOrder> orders = buyOrders.getOrDefault(productId, new ArrayList<>());
        return orders.stream()
            .filter(BazaarOrder::isActive)
            .sorted(Comparator
                .comparingDouble((BazaarOrder o) -> -o.getUnitPrice())  // Highest price first
                .thenComparingLong(BazaarOrder::getCreatedAt))           // Then oldest first
            .toList();
    }
    
    /**
     * Get all active sell orders for a product, sorted by price (lowest first), then time.
     */
    public synchronized List<BazaarOrder> getSellOrders(String productId) {
        List<BazaarOrder> orders = sellOrders.getOrDefault(productId, new ArrayList<>());
        return orders.stream()
            .filter(BazaarOrder::isActive)
            .sorted(Comparator
                .comparingDouble(BazaarOrder::getUnitPrice)            // Lowest price first
                .thenComparingLong(BazaarOrder::getCreatedAt))         // Then oldest first
            .toList();
    }
    
    /**
     * Get the highest buy order price for a product.
     */
    public synchronized double getHighestBuyPrice(String productId) {
        List<BazaarOrder> orders = getBuyOrders(productId);
        return orders.isEmpty() ? Double.NaN : orders.get(0).getUnitPrice();
    }
    
    /**
     * Get the lowest sell order price for a product.
     */
    public synchronized double getLowestSellPrice(String productId) {
        List<BazaarOrder> orders = getSellOrders(productId);
        return orders.isEmpty() ? Double.NaN : orders.get(0).getUnitPrice();
    }
    
    /**
     * Execute an instant buy - buy from lowest sell orders.
     * @return MatchResult with details of what was filled
     */
    public synchronized MatchResult instantBuy(String productId, int amount, double maxPricePerUnit) {
        List<BazaarOrder> orders = getSellOrders(productId);
        return executeMatch(productId, amount, maxPricePerUnit, orders, true);
    }
    
    /**
     * Execute an instant sell - sell to highest buy orders.
     * @return MatchResult with details of what was filled
     */
    public synchronized MatchResult instantSell(String productId, int amount, double minPricePerUnit) {
        List<BazaarOrder> orders = getBuyOrders(productId);
        return executeMatch(productId, amount, minPricePerUnit, orders, false);
    }
    
    /**
     * Execute order matching.
     * @param isBuying true for buying from sell orders, false for selling to buy orders
     */
    private MatchResult executeMatch(
            String productId,
            int amount,
            double priceLimit,
            List<BazaarOrder> orders,
            boolean isBuying
    ) {
        int remaining = amount;
        int filled = 0;
        double totalCost = 0.0;
        List<MatchEntry> matches = new ArrayList<>();
        
        for (BazaarOrder order : orders) {
            if (remaining <= 0 || !order.isActive()) continue;
            
            // Check price limit
            if (isBuying && order.getUnitPrice() > priceLimit) break;
            if (!isBuying && order.getUnitPrice() < priceLimit) break;
            
            int toFill = Math.min(remaining, order.getRemainingAmount());
            double actualPrice = order.getUnitPrice();
            
            int actuallyFilled = order.fill(toFill, actualPrice);
            if (actuallyFilled > 0) {
                filled += actuallyFilled;
                remaining -= actuallyFilled;
                totalCost += actuallyFilled * actualPrice;
                matches.add(new MatchEntry(order.getOrderId(), order.getProfileId(), actuallyFilled, actualPrice));
                
                // Remove filled orders
                if (!order.isActive()) {
                    removeOrder(order.getOrderId());
                }
            }
        }

        if (filled > 0) {
            save();
        }
        
        return new MatchResult(filled, remaining, totalCost, matches);
    }
    
    /**
     * Get all active orders for a player.
     */
    public synchronized List<BazaarOrder> getProfileOrders(UUID profileId) {
        Set<String> orderIds = profileOrders.getOrDefault(profileId, new HashSet<>());
        return orderIds.stream()
            .map(allOrders::get)
            .filter(Objects::nonNull)
            .filter(BazaarOrder::isActive)
            .toList();
    }

    /**
     * Get all active orders for a profile created by a specific owner.
     */
    public synchronized List<BazaarOrder> getProfileOrders(UUID profileId, UUID ownerId) {
        if (profileId == null || ownerId == null) {
            return List.of();
        }
        return getProfileOrders(profileId).stream()
                .filter(order -> ownerId.equals(order.getOwner()))
                .toList();
    }
    
    /**
     * Get all active orders for a profile of a specific type.
     */
    public synchronized List<BazaarOrder> getProfileOrders(UUID profileId, BazaarOrder.OrderType type) {
        return getProfileOrders(profileId).stream()
            .filter(o -> o.getOrderType() == type)
            .toList();
    }
    
    /**
     * Cancel all orders for a profile.
     */
    public synchronized void cancelProfileOrders(UUID profileId) {
        List<BazaarOrder> orders = getProfileOrders(profileId);
        for (BazaarOrder order : orders) {
            order.cancel();
            removeOrder(order.getOrderId());
        }

        if (!orders.isEmpty()) {
            save();
        }
    }

    /**
     * Migrate legacy orders (stored under owner UUID) to the active Skyblock profile UUID.
     * Legacy orders are identified by {@code order.profileId == order.owner}.
     */
    public synchronized boolean migrateLegacyOrders(UUID ownerId, UUID targetProfileId) {
        if (ownerId == null || targetProfileId == null || ownerId.equals(targetProfileId)) {
            return false;
        }

        Set<String> legacyOrderIds = profileOrders.get(ownerId);
        if (legacyOrderIds == null || legacyOrderIds.isEmpty()) {
            return false;
        }

        boolean migratedAny = false;
        Set<String> snapshot = new HashSet<>(legacyOrderIds);
        for (String orderId : snapshot) {
            BazaarOrder order = allOrders.get(orderId);
            if (order == null) {
                legacyOrderIds.remove(orderId);
                continue;
            }
            if (!ownerId.equals(order.getOwner())) {
                continue;
            }
            if (!ownerId.equals(order.getProfileId())) {
                continue;
            }

            BazaarOrder migrated = copyWithProfileId(order, targetProfileId);
            allOrders.put(orderId, migrated);
            replaceInProductList(order, migrated);

            legacyOrderIds.remove(orderId);
            profileOrders.computeIfAbsent(targetProfileId, ignored -> ConcurrentHashMap.newKeySet()).add(orderId);
            migratedAny = true;
        }

        if (legacyOrderIds.isEmpty()) {
            profileOrders.remove(ownerId);
        }

        if (migratedAny) {
            save();
        }
        return migratedAny;
    }

    private BazaarOrder copyWithProfileId(BazaarOrder order, UUID profileId) {
        return new BazaarOrder(
            order.getOrderId(),
            order.getOwner(),
            profileId,
            order.getOwnerName(),
            order.getProductId(),
            order.getOrderType(),
            order.getUnitPrice(),
            order.getTotalAmount(),
            order.getRemainingAmount(),
            order.getFilledAmount(),
            order.getCreatedAt(),
            order.getExpiresAt(),
            order.getStatus(),
            order.getItemSample(),
            order.getTotalSpent(),
            order.getTotalEarned()
        );
    }

    private void replaceInProductList(BazaarOrder previous, BazaarOrder next) {
        Map<String, List<BazaarOrder>> orderMap = previous.getOrderType() == BazaarOrder.OrderType.BUY ? buyOrders : sellOrders;
        List<BazaarOrder> list = orderMap.get(previous.getProductId());
        if (list == null || list.isEmpty()) {
            return;
        }

        for (ListIterator<BazaarOrder> it = list.listIterator(); it.hasNext();) {
            BazaarOrder current = it.next();
            if (current != null && current.getOrderId().equals(previous.getOrderId())) {
                it.set(next);
                break;
            }
        }
    }
    
    /**
     * Clean up expired orders.
     */
    public synchronized int cleanupExpiredOrders() {
        return removeExpiredActiveOrders().size();
    }

    public synchronized List<BazaarOrder> removeExpiredActiveOrders() {
        List<BazaarOrder> expired = allOrders.values().stream()
                .filter(BazaarOrder::isActive)
                .filter(BazaarOrder::isExpired)
                .toList();

        for (BazaarOrder order : expired) {
            order.expire();
            removeOrder(order.getOrderId());
        }

        if (!expired.isEmpty()) {
            save();
        }

        return expired;
    }
    
    /**
     * Get statistics for a product.
     */
    public synchronized ProductStatistics getProductStatistics(String productId) {
        List<BazaarOrder> buys = getBuyOrders(productId);
        List<BazaarOrder> sells = getSellOrders(productId);
        
        return new ProductStatistics(
            buys.size(),
            sells.size(),
            buys.stream().mapToDouble(BazaarOrder::getRemainingValue).sum(),
            sells.stream().mapToDouble(BazaarOrder::getRemainingValue).sum(),
            buys.stream().mapToInt(BazaarOrder::getRemainingAmount).sum(),
            sells.stream().mapToInt(BazaarOrder::getRemainingAmount).sum()
        );
    }
    
    private void sortBuyOrders(String productId) {
        List<BazaarOrder> orders = buyOrders.get(productId);
        if (orders != null) {
            orders.sort(Comparator
                .comparingDouble((BazaarOrder o) -> -o.getUnitPrice())
                .thenComparingLong(BazaarOrder::getCreatedAt));
        }
    }
    
    private void sortSellOrders(String productId) {
        List<BazaarOrder> orders = sellOrders.get(productId);
        if (orders != null) {
            orders.sort(Comparator
                .comparingDouble(BazaarOrder::getUnitPrice)
                .thenComparingLong(BazaarOrder::getCreatedAt));
        }
    }
    
    /**
     * Result of an order match.
     */
    public record MatchResult(
        int filled,
        int remaining,
        double totalCost,
        List<MatchEntry> matches
    ) {
        public boolean isFullyFilled() { return remaining == 0; }
        public boolean isPartiallyFilled() { return filled > 0 && remaining > 0; }
        public boolean isNotFilled() { return filled == 0; }
    }
    
    /**
     * Individual match entry.
     */
    public record MatchEntry(String orderId, UUID counterpartyProfileId, int amount, double price) {}
    
    /**
     * Product statistics.
     */
    public record ProductStatistics(
        int buyOrderCount,
        int sellOrderCount,
        double buyOrderValue,
        double sellOrderValue,
        int buyOrderVolume,
        int sellOrderVolume
    ) {}
    
    /**
     * Order validation result.
     */
    public record ValidationResult(boolean valid, String errorMessage) {}
}

