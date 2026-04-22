package io.papermc.Grivience.dungeon;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks blessings and secrets discovered during a dungeon run.
 */
public final class DungeonStats {
    private final Map<UUID, Integer> secretsFound = new HashMap<>();
    private final Map<BlessingType, Integer> blessings = new HashMap<>();

    public void addSecret(UUID player) {
        secretsFound.put(player, secretsFound.getOrDefault(player, 0) + 1);
    }

    public int getSecrets(UUID player) {
        return secretsFound.getOrDefault(player, 0);
    }

    public int getTotalSecrets() {
        return secretsFound.values().stream().mapToInt(Integer::intValue).sum();
    }

    public void addBlessing(BlessingType type) {
        blessings.put(type, blessings.getOrDefault(type, 0) + 1);
    }

    public int getBlessingLevel(BlessingType type) {
        return blessings.getOrDefault(type, 0);
    }

    public enum BlessingType {
        POWER("Blessing of Power", "&cStrength"),
        LIFE("Blessing of Life", "&aHealth"),
        WISDOM("Blessing of Wisdom", "&bIntelligence"),
        STONE("Blessing of Stone", "&7Defense");

        private final String displayName;
        private final String statName;

        BlessingType(String displayName, String statName) {
            this.displayName = displayName;
            this.statName = statName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getStatName() {
            return statName;
        }
    }
}
