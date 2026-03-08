package io.papermc.Grivience.minion;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class MinionInstance {
    private final UUID id;
    private final UUID islandId;
    private final UUID ownerId;
    private final UUID profileId;
    private final MinionType type;

    private int tier;
    private int storedAmount;
    private final Map<String, Integer> storedItems = new LinkedHashMap<>();
    private int actionProgress;
    private long lastActionAtMs;
    private long createdAtMs;
    private Location location;
    private UUID displayEntityId;
    private String fuelItemId;
    private long fuelExpiresAtMs;
    private String upgradeSlotOneId;
    private String upgradeSlotTwoId;
    private double hopperCoins;

    public MinionInstance(
            UUID id,
            UUID islandId,
            UUID ownerId,
            UUID profileId,
            MinionType type,
            int tier,
            int storedAmount,
            int actionProgress,
            long lastActionAtMs,
            long createdAtMs,
            Location location,
            UUID displayEntityId
    ) {
        this(
                id,
                islandId,
                ownerId,
                profileId,
                type,
                tier,
                storedAmount,
                null,
                actionProgress,
                lastActionAtMs,
                createdAtMs,
                location,
                displayEntityId,
                null,
                0L,
                null,
                null,
                0.0D
        );
    }

    public MinionInstance(
            UUID id,
            UUID islandId,
            UUID ownerId,
            UUID profileId,
            MinionType type,
            int tier,
            int storedAmount,
            Map<String, Integer> storedItems,
            int actionProgress,
            long lastActionAtMs,
            long createdAtMs,
            Location location,
            UUID displayEntityId,
            String fuelItemId,
            long fuelExpiresAtMs,
            String upgradeSlotOneId,
            String upgradeSlotTwoId,
            double hopperCoins
    ) {
        this.id = id;
        this.islandId = islandId;
        this.ownerId = ownerId;
        this.profileId = profileId;
        this.type = type;
        this.tier = tier;
        this.storedAmount = Math.max(0, storedAmount);
        if (storedItems != null) {
            for (Map.Entry<String, Integer> entry : storedItems.entrySet()) {
                if (entry == null || entry.getKey() == null || entry.getKey().isBlank()) {
                    continue;
                }
                int value = Math.max(0, entry.getValue() == null ? 0 : entry.getValue());
                if (value > 0) {
                    this.storedItems.put(entry.getKey(), value);
                }
            }
            if (!this.storedItems.isEmpty()) {
                this.storedAmount = this.storedItems.values().stream().mapToInt(Integer::intValue).sum();
            }
        }
        this.actionProgress = Math.max(0, actionProgress);
        this.lastActionAtMs = Math.max(0L, lastActionAtMs);
        this.createdAtMs = Math.max(0L, createdAtMs);
        this.location = location == null ? null : location.clone();
        this.displayEntityId = displayEntityId;
        this.fuelItemId = fuelItemId;
        this.fuelExpiresAtMs = fuelExpiresAtMs;
        this.upgradeSlotOneId = upgradeSlotOneId;
        this.upgradeSlotTwoId = upgradeSlotTwoId;
        this.hopperCoins = Math.max(0.0D, hopperCoins);
    }

    public UUID id() {
        return id;
    }

    public UUID islandId() {
        return islandId;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public UUID profileId() {
        return profileId;
    }

    public MinionType type() {
        return type;
    }

    public int tier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = tier;
    }

    public int storedAmount() {
        return storedAmount;
    }

    public void setStoredAmount(int storedAmount) {
        this.storedAmount = Math.max(0, storedAmount);
    }

    public Map<String, Integer> storedItems() {
        return new LinkedHashMap<>(storedItems);
    }

    public void setStoredItems(Map<String, Integer> items) {
        storedItems.clear();
        if (items != null) {
            for (Map.Entry<String, Integer> entry : items.entrySet()) {
                if (entry == null || entry.getKey() == null || entry.getKey().isBlank()) {
                    continue;
                }
                int amount = Math.max(0, entry.getValue() == null ? 0 : entry.getValue());
                if (amount > 0) {
                    storedItems.put(entry.getKey(), amount);
                }
            }
        }
        this.storedAmount = storedItems.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int storedAmount(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return 0;
        }
        return Math.max(0, storedItems.getOrDefault(itemId, 0));
    }

    public void addStoredItem(String itemId, int amount) {
        if (itemId == null || itemId.isBlank() || amount <= 0) {
            return;
        }
        int next = Math.max(0, storedItems.getOrDefault(itemId, 0)) + amount;
        storedItems.put(itemId, next);
        this.storedAmount += amount;
    }

    public int clearStoredItems() {
        int total = storedAmount;
        storedItems.clear();
        storedAmount = 0;
        return total;
    }

    public int actionProgress() {
        return actionProgress;
    }

    public void setActionProgress(int actionProgress) {
        this.actionProgress = Math.max(0, actionProgress);
    }

    public long lastActionAtMs() {
        return lastActionAtMs;
    }

    public void setLastActionAtMs(long lastActionAtMs) {
        this.lastActionAtMs = Math.max(0L, lastActionAtMs);
    }

    public long createdAtMs() {
        return createdAtMs;
    }

    public Location location() {
        return location == null ? null : location.clone();
    }

    public void setLocation(Location location) {
        this.location = location == null ? null : location.clone();
    }

    public UUID displayEntityId() {
        return displayEntityId;
    }

    public void setDisplayEntityId(UUID displayEntityId) {
        this.displayEntityId = displayEntityId;
    }

    public String fuelItemId() {
        return fuelItemId;
    }

    public void setFuelItemId(String fuelItemId) {
        this.fuelItemId = fuelItemId;
    }

    public long fuelExpiresAtMs() {
        return fuelExpiresAtMs;
    }

    public void setFuelExpiresAtMs(long fuelExpiresAtMs) {
        this.fuelExpiresAtMs = fuelExpiresAtMs;
    }

    public String upgradeSlotOneId() {
        return upgradeSlotOneId;
    }

    public void setUpgradeSlotOneId(String upgradeSlotOneId) {
        this.upgradeSlotOneId = upgradeSlotOneId;
    }

    public String upgradeSlotTwoId() {
        return upgradeSlotTwoId;
    }

    public void setUpgradeSlotTwoId(String upgradeSlotTwoId) {
        this.upgradeSlotTwoId = upgradeSlotTwoId;
    }

    public double hopperCoins() {
        return hopperCoins;
    }

    public void setHopperCoins(double hopperCoins) {
        this.hopperCoins = Math.max(0.0D, hopperCoins);
    }

    public void addHopperCoins(double amount) {
        if (!Double.isFinite(amount) || amount <= 0.0D) {
            return;
        }
        this.hopperCoins = Math.max(0.0D, this.hopperCoins + amount);
    }

    public void save(ConfigurationSection section) {
        if (section == null) {
            return;
        }
        section.set("id", id.toString());
        section.set("island-id", islandId.toString());
        section.set("owner-id", ownerId.toString());
        if (profileId != null) {
            section.set("profile-id", profileId.toString());
        }
        section.set("type", type.name());
        section.set("tier", tier);
        section.set("stored", storedAmount);
        ConfigurationSection storedSection = section.createSection("stored-items");
        for (Map.Entry<String, Integer> entry : storedItems.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            int amount = Math.max(0, entry.getValue() == null ? 0 : entry.getValue());
            if (amount > 0) {
                storedSection.set(entry.getKey(), amount);
            }
        }
        section.set("action-progress", actionProgress);
        section.set("last-action-at", lastActionAtMs);
        section.set("created-at", createdAtMs);
        if (displayEntityId != null) {
            section.set("display-entity-id", displayEntityId.toString());
        }
        if (fuelItemId != null && !fuelItemId.isBlank()) {
            section.set("fuel.item-id", fuelItemId);
            section.set("fuel.expires-at-ms", fuelExpiresAtMs);
        }
        if (upgradeSlotOneId != null && !upgradeSlotOneId.isBlank()) {
            section.set("upgrades.slot-1", upgradeSlotOneId);
        }
        if (upgradeSlotTwoId != null && !upgradeSlotTwoId.isBlank()) {
            section.set("upgrades.slot-2", upgradeSlotTwoId);
        }
        if (hopperCoins > 0.0D) {
            section.set("shipping.coins", hopperCoins);
        }
        if (location != null && location.getWorld() != null) {
            section.set("world", location.getWorld().getName());
            section.set("x", location.getX());
            section.set("y", location.getY());
            section.set("z", location.getZ());
            section.set("yaw", location.getYaw());
            section.set("pitch", location.getPitch());
        }
    }

    public static MinionInstance fromSection(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        try {
            UUID id = UUID.fromString(section.getString("id", ""));
            UUID islandId = UUID.fromString(section.getString("island-id", ""));
            UUID ownerId = UUID.fromString(section.getString("owner-id", ""));
            UUID profileId = null;
            String profileRaw = section.getString("profile-id", "");
            if (profileRaw != null && !profileRaw.isBlank()) {
                profileId = UUID.fromString(profileRaw);
            }
            MinionType type = MinionType.parse(section.getString("type", ""));
            if (type == null) {
                return null;
            }

            String worldName = section.getString("world");
            World world = worldName == null ? null : Bukkit.getWorld(worldName);
            Location location = null;
            if (world != null) {
                location = new Location(
                        world,
                        section.getDouble("x"),
                        section.getDouble("y"),
                        section.getDouble("z"),
                        (float) section.getDouble("yaw"),
                        (float) section.getDouble("pitch")
                );
            }

            UUID displayId = null;
            String displayRaw = section.getString("display-entity-id", "");
            if (displayRaw != null && !displayRaw.isBlank()) {
                displayId = UUID.fromString(displayRaw);
            }

            Map<String, Integer> storedItems = new LinkedHashMap<>();
            ConfigurationSection storedSection = section.getConfigurationSection("stored-items");
            if (storedSection != null) {
                for (String key : storedSection.getKeys(false)) {
                    if (key == null || key.isBlank()) {
                        continue;
                    }
                    int value = Math.max(0, storedSection.getInt(key, 0));
                    if (value > 0) {
                        storedItems.put(key, value);
                    }
                }
            }

            if (storedItems.isEmpty()) {
                int legacyStored = Math.max(0, section.getInt("stored", 0));
                if (legacyStored > 0) {
                    storedItems.put(type.outputMaterial().name(), legacyStored);
                }
            }

            String fuelId = section.getString("fuel.item-id");
            long fuelExpiresAt = section.getLong("fuel.expires-at-ms", 0L);
            if (fuelExpiresAt <= 0L) {
                // Legacy compatibility for old field name.
                long legacyRemaining = section.getLong("fuel.actions-remaining", 0L);
                if (legacyRemaining > 0L) {
                    fuelExpiresAt = System.currentTimeMillis() + legacyRemaining;
                }
            }
            String upgradeOne = section.getString("upgrades.slot-1");
            String upgradeTwo = section.getString("upgrades.slot-2");
            double hopperCoins = Math.max(0.0D, section.getDouble("shipping.coins", 0.0D));

            return new MinionInstance(
                    id,
                    islandId,
                    ownerId,
                    profileId,
                    type,
                    section.getInt("tier", 1),
                    section.getInt("stored", 0),
                    storedItems,
                    section.getInt("action-progress", 0),
                    section.getLong("last-action-at", System.currentTimeMillis()),
                    section.getLong("created-at", System.currentTimeMillis()),
                    location,
                    displayId,
                    fuelId,
                    fuelExpiresAt,
                    upgradeOne,
                    upgradeTwo,
                    hopperCoins
            );
        } catch (Exception ignored) {
            return null;
        }
    }
}
