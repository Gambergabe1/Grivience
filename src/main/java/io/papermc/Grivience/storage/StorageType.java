package io.papermc.Grivience.storage;

/**
 * Storage System Type - Defines the type of storage available.
 * Each storage type has unique characteristics and upgrade paths.
 */
public enum StorageType {
    /**
     * Personal Storage - Basic backpack storage for players.
     * Starts with 27 slots, upgradable to 54 slots.
     */
    PERSONAL("Personal Storage", 27, 54, "storage.personal"),

    /**
     * Vault Storage - Secure bank-like storage accessible from any vault point.
     * Starts with 27 slots, upgradable to 162 slots (6 rows).
     * Can be accessed from special vault terminals.
     */
    VAULT("Vault Storage", 27, 162, "storage.vault"),

    /**
     * Ender Storage - Portable storage linked to player's ender chest.
     * Starts with 27 slots, upgradable to 54 slots.
     * Accessible from any ender chest.
     */
    ENDER("Ender Storage", 27, 54, "storage.ender"),

    /**
     * Backpack Storage - Portable storage that can be carried.
     * Multiple backpacks can be owned.
     * Starts with 9 slots per backpack, upgradable to 45 slots.
     */
    BACKPACK("Backpack Storage", 9, 45, "storage.backpack"),

    /**
     * Warehouse Storage - Large-scale storage for bulk items.
     * Starts with 54 slots, upgradable to 540 slots (20 pages).
     * Designed for mass storage and organization.
     */
    WAREHOUSE("Warehouse Storage", 54, 540, "storage.warehouse"),

    /**
     * Accessory Bag - Specialized storage for accessories.
     */
    ACCESSORY_BAG("Accessory Bag", 9, 72, "storage.accessory"),

    /**
     * Potion Bag - Specialized storage for potions.
     */
    POTION_BAG("Potion Bag", 18, 54, "storage.potion");

    private final String displayName;
    private final int baseSlots;
    private final int maxSlots;
    private final String permissionNode;

    StorageType(String displayName, int baseSlots, int maxSlots, String permissionNode) {
        this.displayName = displayName;
        this.baseSlots = baseSlots;
        this.maxSlots = maxSlots;
        this.permissionNode = permissionNode;
    }

    /**
     * Get the display name of this storage type.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the base number of slots (starting size).
     */
    public int getBaseSlots() {
        return baseSlots;
    }

    /**
     * Get the maximum number of slots (fully upgraded).
     */
    public int getMaxSlots() {
        return maxSlots;
    }

    /**
     * Get the permission node required to use this storage type.
     */
    public String getPermissionNode() {
        return permissionNode;
    }

    /**
     * Check if a player has permission to use this storage type.
     * Note: Actual permission check should be done externally.
     */
    public boolean hasPermission(String playerPermission) {
        return playerPermission.contains(permissionNode) || playerPermission.equals("*");
    }

    /**
     * Get the number of rows for GUI display based on slot count.
     */
    public static int getRowsForSlots(int slots) {
        return (slots + 8) / 9; // Round up to nearest row
    }

    /**
     * Get storage type by ID.
     */
    public static StorageType fromId(String id) {
        for (StorageType type : values()) {
            if (type.name().equalsIgnoreCase(id)) {
                return type;
            }
        }
        return PERSONAL;
    }
}
