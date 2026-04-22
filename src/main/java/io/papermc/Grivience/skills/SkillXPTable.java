package io.papermc.Grivience.skills;

import java.util.Arrays;

/**
 * Skyblock-accurate skill XP requirements.
 * Data based on standard Skyblock skill leveling (approx 55.1M total for level 50).
 */
public final class SkillXPTable {
    private static final long[] LEVEL_XP;
    private static final long[] CUMULATIVE_XP;
    private static final int MAX_LEVEL = 60;

    static {
        LEVEL_XP = new long[MAX_LEVEL + 1];
        CUMULATIVE_XP = new long[MAX_LEVEL + 1];

        // Level 1-50 standard Skyblock curve
        long[] standard50 = {
            0, 50, 125, 200, 300, 500, 750, 1000, 1500, 2000, 3500,
            5000, 7500, 10000, 15000, 20000, 30000, 50000, 75000, 100000, 200000,
            300000, 400000, 500000, 600000, 700000, 800000, 900000, 1000000, 1100000, 1200000,
            1300000, 1400000, 1500000, 1600000, 1700000, 1800000, 1900000, 2000000, 2100000, 2200000,
            2300000, 2400000, 2500000, 2600000, 2750000, 2900000, 3100000, 3400000, 3700000, 4000000
        };
        
        System.arraycopy(standard50, 0, LEVEL_XP, 0, Math.min(standard50.length, LEVEL_XP.length));

        // Level 51-60 (post-50 curve is steeper)
        for (int i = 51; i <= MAX_LEVEL; i++) {
            LEVEL_XP[i] = LEVEL_XP[i-1] + 500000L;
        }

        // Calculate cumulative
        long total = 0;
        for (int i = 0; i <= MAX_LEVEL; i++) {
            total += LEVEL_XP[i];
            CUMULATIVE_XP[i] = total;
        }
    }

    private SkillXPTable() {}

    /**
     * Get the amount of XP required to go FROM the previous level TO the specified level.
     */
    public static long getXpRequired(int level) {
        if (level < 0 || level > MAX_LEVEL) return Long.MAX_VALUE;
        return LEVEL_XP[level];
    }

    /**
     * Get the total cumulative XP required to reach the specified level.
     */
    public static long getCumulativeXp(int level) {
        if (level < 0) return 0;
        if (level > MAX_LEVEL) return CUMULATIVE_XP[MAX_LEVEL];
        return CUMULATIVE_XP[level];
    }

    /**
     * Calculate the level for a given total XP amount.
     */
    public static int getLevelFromXp(double totalXp) {
        if (totalXp <= 0) return 0;
        for (int i = MAX_LEVEL; i >= 0; i--) {
            if (totalXp >= CUMULATIVE_XP[i]) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Get progress (0.0 to 1.0) towards the NEXT level.
     */
    public static double getProgress(double totalXp) {
        int level = getLevelFromXp(totalXp);
        if (level >= MAX_LEVEL) return 1.0;
        
        long currentBase = getCumulativeXp(level);
        long nextBase = getCumulativeXp(level + 1);
        long required = nextBase - currentBase;
        
        if (required <= 0) return 1.0;
        return (totalXp - currentBase) / (double) required;
    }

    public static int getMaxLevel() {
        return MAX_LEVEL;
    }
}
