package io.papermc.Grivience.bazaar;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Simple record of a player-created Bazaar order.
 */
public final class BazaarOrder {
    public enum Type {
        BUY,
        SELL
    }

    private final String id;
    private final UUID owner;
    private final String itemKey;
    private final double unitPrice;
    private int remainingAmount;
    private final Type type;
    private final long createdAt;
    private final ItemStack storedItem;

    public BazaarOrder(
            String id,
            UUID owner,
            String itemKey,
            double unitPrice,
            int remainingAmount,
            Type type,
            long createdAt,
            ItemStack storedItem
    ) {
        this.id = id;
        this.owner = owner;
        this.itemKey = itemKey;
        this.unitPrice = unitPrice;
        this.remainingAmount = Math.max(0, remainingAmount);
        this.type = type;
        this.createdAt = createdAt;
        this.storedItem = storedItem;
    }

    public String getId() {
        return id;
    }

    public UUID getOwner() {
        return owner;
    }

    public String getItemKey() {
        return itemKey;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public int getRemainingAmount() {
        return remainingAmount;
    }

    public void decrement(int amount) {
        remainingAmount = Math.max(0, remainingAmount - Math.max(0, amount));
    }

    public Type getType() {
        return type;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public ItemStack getStoredItem() {
        return storedItem;
    }

    public void save(ConfigurationSection section) {
        section.set("owner", owner.toString());
        section.set("item", itemKey);
        section.set("unit-price", unitPrice);
        section.set("remaining", remainingAmount);
        section.set("type", type.name());
        section.set("created-at", createdAt);
        if (storedItem != null) {
            section.set("sample-item", storedItem);
        }
    }

    public static BazaarOrder fromSection(String id, ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String ownerString = section.getString("owner");
        String itemKey = section.getString("item");
        String typeRaw = section.getString("type", "BUY");
        UUID owner;
        try {
            owner = UUID.fromString(ownerString);
        } catch (Exception ex) {
            return null;
        }
        BazaarOrder.Type type;
        try {
            type = BazaarOrder.Type.valueOf(typeRaw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            type = BazaarOrder.Type.BUY;
        }
        double unitPrice = section.getDouble("unit-price", 0.0D);
        int remaining = section.getInt("remaining", 0);
        long createdAt = section.getLong("created-at", System.currentTimeMillis());
        ItemStack sample = null;
        if (section.isItemStack("sample-item")) {
            sample = section.getItemStack("sample-item");
        }
        return new BazaarOrder(id, owner, itemKey, unitPrice, remaining, type, createdAt, sample);
    }
}
