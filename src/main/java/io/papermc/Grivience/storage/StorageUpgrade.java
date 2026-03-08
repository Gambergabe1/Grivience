package io.papermc.Grivience.storage;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a storage upgrade tier.
 * Each tier increases the storage capacity and has an associated cost.
 */
public class StorageUpgrade {
    private final int tier;
    private final int slots;
    private final double cost;
    private final List<String> commands;
    private final String displayName;

    public StorageUpgrade(int tier, int slots, double cost, List<String> commands, String displayName) {
        this.tier = tier;
        this.slots = slots;
        this.cost = cost;
        this.commands = commands != null ? new ArrayList<>(commands) : new ArrayList<>();
        this.displayName = displayName;
    }

    /**
     * Get the tier level (1-based).
     */
    public int getTier() {
        return tier;
    }

    /**
     * Get the number of slots at this tier.
     */
    public int getSlots() {
        return slots;
    }

    /**
     * Get the cost to upgrade to this tier.
     */
    public double getCost() {
        return cost;
    }

    /**
     * Get the commands to execute when upgrading to this tier.
     */
    public List<String> getCommands() {
        return new ArrayList<>(commands);
    }

    /**
     * Get the display name for this upgrade tier.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Check if this upgrade has a cost.
     */
    public boolean hasCost() {
        return cost > 0;
    }

    /**
     * Create a StorageUpgrade from configuration.
     */
    public static StorageUpgrade fromConfig(String key, ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        int tier;
        try {
            tier = Integer.parseInt(key.replace("tier-", ""));
        } catch (NumberFormatException e) {
            tier = 1;
        }

        int slots = section.getInt("slots", 27);
        double cost = section.getDouble("cost", 0);
        List<String> commands = section.getStringList("commands");
        String displayName = section.getString("name", "&6Tier " + tier);

        return new StorageUpgrade(tier, slots, cost, commands, displayName);
    }

    /**
     * Create a builder for StorageUpgrade.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int tier = 1;
        private int slots = 27;
        private double cost = 0;
        private List<String> commands = new ArrayList<>();
        private String displayName = "&6Storage Upgrade";

        public Builder tier(int tier) {
            this.tier = tier;
            return this;
        }

        public Builder slots(int slots) {
            this.slots = slots;
            return this;
        }

        public Builder cost(double cost) {
            this.cost = cost;
            return this;
        }

        public Builder commands(List<String> commands) {
            this.commands = commands != null ? new ArrayList<>(commands) : new ArrayList<>();
            return this;
        }

        public Builder command(String command) {
            this.commands.add(command);
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public StorageUpgrade build() {
            return new StorageUpgrade(tier, slots, cost, commands, displayName);
        }
    }
}
