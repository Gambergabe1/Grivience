package io.papermc.Grivience.bazaar;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BazaarOrderStore {
    private final GriviencePlugin plugin;
    private final File file;
    private final Map<String, BazaarOrder> orders = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> pendingDeliveries = new HashMap<>();
    private int lastId = 0;

    public BazaarOrderStore(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "bazaar_orders.yml");
        load();
    }

    public synchronized void load() {
        orders.clear();
        pendingDeliveries.clear();
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        lastId = yaml.getInt("last-id", 0);

        ConfigurationSection ordersSection = yaml.getConfigurationSection("orders");
        if (ordersSection != null) {
            for (String id : ordersSection.getKeys(false)) {
                BazaarOrder order = BazaarOrder.fromSection(id, ordersSection.getConfigurationSection(id));
                if (order != null) {
                    orders.put(id, order);
                }
            }
        }

        ConfigurationSection deliverySection = yaml.getConfigurationSection("deliveries");
        if (deliverySection != null) {
            for (String rawUuid : deliverySection.getKeys(false)) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(rawUuid);
                } catch (Exception ignored) {
                    continue;
                }
                ConfigurationSection items = deliverySection.getConfigurationSection(rawUuid);
                if (items == null) {
                    continue;
                }
                Map<String, Integer> byItem = new HashMap<>();
                for (String itemKey : items.getKeys(false)) {
                    int amount = items.getInt(itemKey, 0);
                    if (amount > 0) {
                        byItem.put(itemKey, amount);
                    }
                }
                if (!byItem.isEmpty()) {
                    pendingDeliveries.put(uuid, byItem);
                }
            }
        }
    }

    public synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("last-id", lastId);
        ConfigurationSection ordersSection = yaml.createSection("orders");
        for (BazaarOrder order : orders.values()) {
            ConfigurationSection section = ordersSection.createSection(order.getId());
            order.save(section);
        }
        ConfigurationSection deliverySection = yaml.createSection("deliveries");
        for (Map.Entry<UUID, Map<String, Integer>> entry : pendingDeliveries.entrySet()) {
            ConfigurationSection section = deliverySection.createSection(entry.getKey().toString());
            for (Map.Entry<String, Integer> item : entry.getValue().entrySet()) {
                section.set(item.getKey(), item.getValue());
            }
        }
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to save bazaar order store: " + ex.getMessage());
        }
    }

    public synchronized String nextId() {
        lastId++;
        return String.format("%05d", lastId);
    }

    public synchronized void addOrder(BazaarOrder order) {
        orders.put(order.getId(), order);
        save();
    }

    public synchronized void updateOrder(BazaarOrder order) {
        orders.put(order.getId(), order);
        save();
    }

    public synchronized void removeOrder(String id) {
        orders.remove(id);
        save();
    }

    public synchronized List<BazaarOrder> ordersFor(String itemKey, BazaarOrder.Type type) {
        List<BazaarOrder> matched = new ArrayList<>();
        for (BazaarOrder order : orders.values()) {
            if (order.getType() == type && order.getItemKey().equalsIgnoreCase(itemKey) && order.getRemainingAmount() > 0) {
                matched.add(order);
            }
        }
        Comparator<BazaarOrder> comparator = Comparator.comparingDouble(BazaarOrder::getUnitPrice)
                .thenComparingLong(BazaarOrder::getCreatedAt);
        if (type == BazaarOrder.Type.BUY) {
            comparator = comparator.reversed();
        }
        matched.sort(comparator);
        return Collections.unmodifiableList(matched);
    }

    public synchronized double bestPrice(String itemKey, BazaarOrder.Type type) {
        List<BazaarOrder> list = ordersFor(itemKey, type);
        if (list.isEmpty()) {
            return Double.NaN;
        }
        return list.get(0).getUnitPrice();
    }

    public synchronized void recordDelivery(UUID owner, String itemKey, int amount) {
        if (amount <= 0) {
            return;
        }
        Map<String, Integer> entries = pendingDeliveries.computeIfAbsent(owner, ignored -> new HashMap<>());
        entries.merge(itemKey, amount, Integer::sum);
        save();
    }

    public synchronized Map<String, Integer> consumeDeliveries(UUID owner) {
        Map<String, Integer> items = pendingDeliveries.remove(owner);
        if (items == null) {
            return Map.of();
        }
        save();
        return items;
    }

    public synchronized Map<String, BazaarOrder> snapshot() {
        return Map.copyOf(orders);
    }
}
