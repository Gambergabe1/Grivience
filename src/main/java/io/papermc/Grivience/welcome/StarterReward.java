package io.papermc.Grivience.welcome;

import org.bukkit.ChatColor;

/**
 * Represents a starter reward package for new players.
 */
public class StarterReward {
    private final String id;
    private final String displayName;
    private final RewardType type;
    private final Object value;
    private final int duration; // in minutes (for boosts)
    private final boolean tradeable;
    private final boolean sellable;
    private final boolean auctionable;

    public StarterReward(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.type = builder.type;
        this.value = builder.value;
        this.duration = builder.duration;
        this.tradeable = builder.tradeable;
        this.sellable = builder.sellable;
        this.auctionable = builder.auctionable;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public RewardType getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public int getDuration() {
        return duration;
    }

    public boolean isTradeable() {
        return tradeable;
    }

    public boolean isSellable() {
        return sellable;
    }

    public boolean isAuctionable() {
        return auctionable;
    }

    public enum RewardType {
        MONEY,
        MINING_BOOST,
        FARMING_BOOST,
        ARMOR_HELMET,
        ARMOR_CHESTPLATE,
        ARMOR_LEGGINGS,
        ARMOR_BOOTS,
        TOOL_PICKAXE,
        TOOL_AXE,
        TOOL_HOE,
        FOOD,
        MISC
    }

    public static Builder builder(String id, String displayName) {
        return new Builder(id, displayName);
    }

    public static class Builder {
        private final String id;
        private final String displayName;
        private RewardType type;
        private Object value;
        private int duration = 0;
        private boolean tradeable = false;
        private boolean sellable = false;
        private boolean auctionable = false;

        public Builder(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public Builder type(RewardType type) {
            this.type = type;
            return this;
        }

        public Builder value(Object value) {
            this.value = value;
            return this;
        }

        public Builder duration(int durationMinutes) {
            this.duration = durationMinutes;
            return this;
        }

        public Builder tradeable(boolean tradeable) {
            this.tradeable = tradeable;
            return this;
        }

        public Builder sellable(boolean sellable) {
            this.sellable = sellable;
            return this;
        }

        public Builder auctionable(boolean auctionable) {
            this.auctionable = auctionable;
            return this;
        }

        public StarterReward build() {
            return new StarterReward(this);
        }
    }
}
