package io.papermc.Grivience.storage;

import io.papermc.Grivience.util.StackSizeSanitizer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a player's storage profile for a specific storage type.
 * Contains slot count, inventory contents, and upgrade tier.
 * Storage is now linked to Skyblock profiles for profile-specific storage.
 */
public class StorageProfile {
    private final UUID ownerId;
    private final UUID profileId;
    private final StorageType storageType;
    private int currentSlots;
    private int upgradeTier;
    private final Map<Integer, ItemStack> contents;
    private String customName;
    private boolean locked;
    private long lastAccessed;

    public StorageProfile(UUID ownerId, UUID profileId, StorageType storageType) {
        this.ownerId = ownerId;
        this.profileId = profileId;
        this.storageType = storageType;
        this.currentSlots = storageType.getBaseSlots();
        this.upgradeTier = 0;
        this.contents = new ConcurrentHashMap<>();
        this.customName = null;
        this.locked = false;
        this.lastAccessed = System.currentTimeMillis();
    }

    /**
     * Create a storage profile without a profile ID (for legacy/compatibility).
     * @deprecated Use {@link #StorageProfile(UUID, UUID, StorageType)} instead.
     */
    @Deprecated
    public StorageProfile(UUID ownerId, StorageType storageType) {
        this(ownerId, null, storageType);
    }

    /**
     * Get the owner's UUID.
     */
    public UUID getOwnerId() {
        return ownerId;
    }

    /**
     * Get the associated Skyblock profile ID.
     */
    public UUID getProfileId() {
        return profileId;
    }

    /**
     * Get the storage type.
     */
    public StorageType getStorageType() {
        return storageType;
    }

    /**
     * Get the current number of slots.
     */
    public int getCurrentSlots() {
        return currentSlots;
    }

    /**
     * Set the current number of slots (used for upgrades).
     */
    public void setCurrentSlots(int currentSlots) {
        this.currentSlots = Math.min(currentSlots, storageType.getMaxSlots());
    }

    /**
     * Get the current upgrade tier.
     */
    public int getUpgradeTier() {
        return upgradeTier;
    }

    /**
     * Set the upgrade tier.
     */
    public void setUpgradeTier(int upgradeTier) {
        this.upgradeTier = upgradeTier;
    }

    /**
     * Get the contents map.
     */
    public Map<Integer, ItemStack> getContents() {
        return contents;
    }

    /**
     * Get an item at a specific slot.
     */
    public ItemStack getItem(int slot) {
        return contents.get(slot);
    }

    /**
     * Set an item at a specific slot.
     */
    public void setItem(int slot, ItemStack item) {
        if (slot >= 0 && slot < currentSlots) {
            if (item == null || item.getType().isAir()) {
                contents.remove(slot);
            } else {
                contents.put(slot, item.clone());
            }
        }
    }

    /**
     * Get the custom name of this storage.
     */
    public String getCustomName() {
        return customName;
    }

    /**
     * Set the custom name of this storage.
     */
    public void setCustomName(String customName) {
        this.customName = customName;
    }

    /**
     * Check if this storage is locked.
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     * Set the locked state.
     */
    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    /**
     * Get the last accessed timestamp.
     */
    public long getLastAccessed() {
        return lastAccessed;
    }

    /**
     * Update the last accessed timestamp.
     */
    public void updateLastAccessed() {
        this.lastAccessed = System.currentTimeMillis();
    }

    /**
     * Create a Bukkit Inventory for this storage profile.
     */
    public Inventory createInventory() {
        int rows = StorageType.getRowsForSlots(currentSlots);
        String title = customName != null ? customName : storageType.getDisplayName();
        StorageInventoryHolder holder = new StorageInventoryHolder(ownerId, profileId, storageType);
        Inventory inventory = Bukkit.createInventory(holder, rows * 9, title);
        holder.inventory = inventory;

        // Load contents
        for (Map.Entry<Integer, ItemStack> entry : contents.entrySet()) {
            if (entry.getKey() < inventory.getSize()) {
                inventory.setItem(entry.getKey(), entry.getValue());
            }
        }

        return inventory;
    }

    /**
     * Load contents from a Bukkit Inventory.
     */
    public void loadFromInventory(Inventory inventory) {
        contents.clear();
        for (int i = 0; i < Math.min(inventory.getSize(), currentSlots); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && !item.getType().isAir()) {
                contents.put(i, item.clone());
            }
        }
        updateLastAccessed();
    }

    /**
     * Get the total number of items stored.
     */
    public int getTotalItems() {
        return contents.size();
    }

    /**
     * Get the number of empty slots.
     */
    public int getEmptySlots() {
        return currentSlots - contents.size();
    }

    /**
     * Check if the storage is full.
     */
    public boolean isFull() {
        return contents.size() >= currentSlots;
    }

    /**
     * Add an item to the storage (first available slot).
     * Returns the amount that couldn't be stored.
     */
    public int addItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return 0;
        }

        int amountToStore = item.getAmount();

        // First, try to stack with existing items
        for (Map.Entry<Integer, ItemStack> entry : contents.entrySet()) {
            ItemStack existing = entry.getValue();
            if (existing.isSimilar(item)) {
                int maxStack = existing.getMaxStackSize();
                int space = maxStack - existing.getAmount();
                int toAdd = Math.min(space, amountToStore);
                existing.setAmount(existing.getAmount() + toAdd);
                amountToStore -= toAdd;
                if (amountToStore <= 0) {
                    return 0;
                }
            }
        }

        // Then, find empty slots
        while (amountToStore > 0) {
            int emptySlot = -1;
            for (int i = 0; i < currentSlots; i++) {
                if (!contents.containsKey(i)) {
                    emptySlot = i;
                    break;
                }
            }

            if (emptySlot == -1) {
                // No more space
                return amountToStore;
            }

            int toStore = Math.min(amountToStore, item.getMaxStackSize());
            ItemStack toAdd = item.clone();
            toAdd.setAmount(toStore);
            contents.put(emptySlot, toAdd);
            amountToStore -= toStore;
        }

        return 0;
    }

    /**
     * Remove items from the storage.
     * Returns the actual amount removed.
     */
    public int removeItem(ItemStack item, int amount) {
        if (item == null || item.getType().isAir()) {
            return 0;
        }

        int toRemove = amount;

        for (Map.Entry<Integer, ItemStack> entry : contents.entrySet()) {
            ItemStack existing = entry.getValue();
            if (existing.isSimilar(item)) {
                int existingAmount = existing.getAmount();
                int removeNow = Math.min(existingAmount, toRemove);
                existing.setAmount(existingAmount - removeNow);
                toRemove -= removeNow;

                if (existing.getAmount() <= 0) {
                    contents.remove(entry.getKey());
                }

                if (toRemove <= 0) {
                    return amount;
                }
            }
        }

        return amount - toRemove;
    }

    /**
     * Clear all contents.
     */
    public void clear() {
        contents.clear();
    }

    /**
     * Save this profile to configuration.
     */
    public void save(ConfigurationSection section) {
        section.set("type", storageType.name());
        section.set("profile-id", profileId != null ? profileId.toString() : null);
        section.set("slots", currentSlots);
        section.set("tier", upgradeTier);
        section.set("name", customName);
        section.set("locked", locked);
        section.set("lastAccessed", lastAccessed);

        // Save contents
        ConfigurationSection contentsSection = section.createSection("contents");
        for (Map.Entry<Integer, ItemStack> entry : contents.entrySet()) {
            ConfigurationSection itemSection = contentsSection.createSection(String.valueOf(entry.getKey()));
            itemSection.set("type", entry.getValue().getType().name());
            itemSection.set("amount", entry.getValue().getAmount());
            itemSection.set("damage", entry.getValue().getDurability());
            if (entry.getValue().hasItemMeta()) {
                itemSection.set("name", entry.getValue().getItemMeta().getDisplayName());
                itemSection.set("lore", entry.getValue().getItemMeta().getLore());
            }
        }
    }

    /**
     * Load a profile from configuration.
     */
    public static StorageProfile load(UUID ownerId, ConfigurationSection section) {
        String typeId = section.getString("type", "PERSONAL");
        StorageType type = StorageType.fromId(typeId);
        
        // Load profile ID (may be null for legacy data)
        UUID profileId = null;
        String profileIdString = section.getString("profile-id");
        if (profileIdString != null) {
            try {
                profileId = UUID.fromString(profileIdString);
            } catch (IllegalArgumentException e) {
                // Invalid UUID, keep as null
            }
        }

        StorageProfile profile = new StorageProfile(ownerId, profileId, type);
        profile.setCurrentSlots(section.getInt("slots", type.getBaseSlots()));
        profile.setUpgradeTier(section.getInt("tier", 0));
        profile.setCustomName(section.getString("name"));
        profile.setLocked(section.getBoolean("locked", false));
        profile.lastAccessed = section.getLong("lastAccessed", System.currentTimeMillis());

        // Load contents
        ConfigurationSection contentsSection = section.getConfigurationSection("contents");
        if (contentsSection != null) {
            java.util.List<ItemStack> overflow = new java.util.ArrayList<>();
            for (String key : contentsSection.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    ConfigurationSection itemSection = contentsSection.getConfigurationSection(key);
                    if (itemSection != null) {
                        String materialName = itemSection.getString("type", "STONE");
                        org.bukkit.Material material = org.bukkit.Material.matchMaterial(materialName);
                        if (material != null) {
                            ItemStack item = new ItemStack(material);
                            int rawAmount = Math.max(1, itemSection.getInt("amount", 1));
                            item.setDurability((short) itemSection.getInt("damage", 0));
                            java.util.List<ItemStack> legalStacks = StackSizeSanitizer.splitToLegalStacks(item, rawAmount);
                            if (legalStacks.isEmpty()) {
                                continue;
                            }
                            profile.contents.put(slot, legalStacks.getFirst());
                            if (legalStacks.size() > 1) {
                                overflow.addAll(legalStacks.subList(1, legalStacks.size()));
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid slot
                }
            }
            for (ItemStack stack : overflow) {
                int leftover = profile.addItem(stack);
                if (leftover > 0) {
                    Bukkit.getLogger().warning("Discarded " + leftover + " overflow items while sanitizing storage profile "
                            + profile.storageType + " for owner " + ownerId + ".");
                }
            }
        }

        return profile;
    }

    /**
     * Get a formatted status string for this storage.
     */
    public String getStatus() {
        return String.format("§7Slots: §e%d§7/§e%d §7| Tier: §6%d §7| Items: §a%d",
                contents.size(), currentSlots, upgradeTier, contents.size());
    }

    /**
     * Marker holder so island protection can reliably detect Storage inventories.
     * Bukkit allows a null holder, but then visitor protections cannot whitelist it.
     */
    static final class StorageInventoryHolder implements InventoryHolder {
        private final UUID ownerId;
        private final UUID profileId;
        private final StorageType storageType;
        private Inventory inventory;

        StorageInventoryHolder(UUID ownerId, UUID profileId, StorageType storageType) {
            this.ownerId = ownerId;
            this.profileId = profileId;
            this.storageType = storageType;
        }

        UUID ownerId() {
            return ownerId;
        }

        UUID profileId() {
            return profileId;
        }

        StorageType storageType() {
            return storageType;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}

