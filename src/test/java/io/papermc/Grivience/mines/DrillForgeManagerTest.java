package io.papermc.Grivience.mines;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DrillForgeManagerTest {

    @Test
    void heatTierCapsAtFourAndScalesEveryTwentyHeat() {
        assertEquals(0, DrillForgeManager.heatTierFor(0));
        assertEquals(0, DrillForgeManager.heatTierFor(19));
        assertEquals(1, DrillForgeManager.heatTierFor(20));
        assertEquals(2, DrillForgeManager.heatTierFor(40));
        assertEquals(3, DrillForgeManager.heatTierFor(60));
        assertEquals(4, DrillForgeManager.heatTierFor(80));
        assertEquals(4, DrillForgeManager.heatTierFor(200));
    }

    @Test
    void speedBonusScalesContinuouslyWithStoredHeat() {
        assertEquals(0, DrillForgeManager.speedBonusPercentFor(0));
        assertEquals(4, DrillForgeManager.speedBonusPercentFor(10));
        assertEquals(8, DrillForgeManager.speedBonusPercentFor(19));
        assertEquals(32, DrillForgeManager.speedBonusPercentFor(80));
        assertEquals(40, DrillForgeManager.speedBonusPercentFor(100));
    }

    @Test
    void durationFallsAsHeatBuildsButNeverBelowOneMinute() {
        long base = 10L * 60L * 1000L;
        long cool = DrillForgeManager.adjustedDurationMillis(base, 0);
        long warm = DrillForgeManager.adjustedDurationMillis(base, 10);
        long hot = DrillForgeManager.adjustedDurationMillis(base, 80);

        assertEquals(base, cool);
        assertTrue(warm < cool);
        assertTrue(hot < cool);
        assertEquals(408_000L, hot);
        assertEquals(60_000L, DrillForgeManager.adjustedDurationMillis(10_000L, 80));
    }

    @Test
    void overdriveFuelCostHasAFloorAndScalesByIgnitionTier() {
        assertEquals(10, DrillForgeManager.adjustedFuelCostPerBlock(10, 0, false));
        assertEquals(7, DrillForgeManager.adjustedFuelCostPerBlock(10, 0, true));
        assertEquals(5, DrillForgeManager.adjustedFuelCostPerBlock(10, 2, true));
        assertEquals(4, DrillForgeManager.adjustedFuelCostPerBlock(5, 4, true));
    }

    @Test
    void overdriveDurationGetsLongerAtHigherIgnitionHeat() {
        assertEquals(12L * 60L * 1000L, DrillForgeManager.overdriveDurationMillisForHeat(20));
        assertEquals((12L * 60L * 1000L) + 90_000L, DrillForgeManager.overdriveDurationMillisForHeat(40));
        assertEquals((12L * 60L * 1000L) + (4L * 90_000L), DrillForgeManager.overdriveDurationMillisForHeat(100));
    }
}
