package io.papermc.Grivience.util;

import java.util.concurrent.ThreadLocalRandom;

public final class FarmingSetBonusUtil {
    public static final String HARVESTER_EMBRACE_SET_ID = "harvester_embrace";
    public static final String GILDED_HARVESTER_SET_ID = "gilded_harvester";
    public static final String ROOTBOUND_GARB_SET_ID = "rootbound_garb";
    public static final String TATERGUARD_SET_ID = "taterguard";
    public static final String MELON_MONARCH_SET_ID = "melon_monarch";
    public static final String WARTWOVEN_REGALIA_SET_ID = "wartwoven_regalia";
    public static final int HARVESTER_EMBRACE_YIELD_PIECES = 2;
    public static final int GILDED_HARVESTER_FULL_SET_PIECES = 4;

    private static final double HARVESTER_EMBRACE_YIELD_BONUS = 0.15D;
    private static final double GILDED_HARVESTER_FULL_SET_HEALTH_BONUS = 50.0D;
    private static final double ROOTBOUND_GARB_FULL_SET_FARMING_FORTUNE = 25.0D;
    private static final double TATERGUARD_FULL_SET_HEALTH_BONUS = 60.0D;
    private static final double MELON_MONARCH_FULL_SET_HEALTH_BONUS = 20.0D;
    private static final double MELON_MONARCH_FULL_SET_FARMING_FORTUNE = 35.0D;
    private static final double WARTWOVEN_REGALIA_FULL_SET_INTELLIGENCE = 75.0D;

    private FarmingSetBonusUtil() {
    }

    public static int computeHarvesterEmbraceExtraDrops(int equippedPieces, int baseDrops) {
        if (equippedPieces < HARVESTER_EMBRACE_YIELD_PIECES) {
            return 0;
        }
        return computePercentBonusDrops(baseDrops, HARVESTER_EMBRACE_YIELD_BONUS);
    }

    public static double gildedHarvesterFullSetHealthBonus(int equippedPieces) {
        return equippedPieces >= GILDED_HARVESTER_FULL_SET_PIECES ? GILDED_HARVESTER_FULL_SET_HEALTH_BONUS : 0.0D;
    }

    public static double rootboundGarbFullSetFarmingFortuneBonus(int equippedPieces) {
        return equippedPieces >= GILDED_HARVESTER_FULL_SET_PIECES ? ROOTBOUND_GARB_FULL_SET_FARMING_FORTUNE : 0.0D;
    }

    public static double taterguardFullSetHealthBonus(int equippedPieces) {
        return equippedPieces >= GILDED_HARVESTER_FULL_SET_PIECES ? TATERGUARD_FULL_SET_HEALTH_BONUS : 0.0D;
    }

    public static double melonMonarchFullSetHealthBonus(int equippedPieces) {
        return equippedPieces >= GILDED_HARVESTER_FULL_SET_PIECES ? MELON_MONARCH_FULL_SET_HEALTH_BONUS : 0.0D;
    }

    public static double melonMonarchFullSetFarmingFortuneBonus(int equippedPieces) {
        return equippedPieces >= GILDED_HARVESTER_FULL_SET_PIECES ? MELON_MONARCH_FULL_SET_FARMING_FORTUNE : 0.0D;
    }

    public static double wartwovenRegaliaFullSetIntelligenceBonus(int equippedPieces) {
        return equippedPieces >= GILDED_HARVESTER_FULL_SET_PIECES ? WARTWOVEN_REGALIA_FULL_SET_INTELLIGENCE : 0.0D;
    }

    public static int computePercentBonusDrops(int baseDrops, double percentBonus) {
        if (baseDrops <= 0 || !Double.isFinite(percentBonus) || percentBonus <= 0.0D) {
            return 0;
        }

        double expected = baseDrops * percentBonus;
        int guaranteed = (int) Math.floor(expected);
        double fractional = expected - guaranteed;
        if (fractional > 0.0D && ThreadLocalRandom.current().nextDouble() < fractional) {
            guaranteed += 1;
        }
        return Math.max(0, guaranteed);
    }
}
