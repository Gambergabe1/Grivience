package io.papermc.Grivience.mines.end;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HeartOfTheEndMinesManagerThresholdTest {

    @Test
    void legacyDefaultThresholdsMigrateToWiderCurve() {
        List<Long> widened = HeartOfTheEndMinesManager.normalizeThresholds(
                List.of(0L, 75L, 200L, 450L, 800L, 1300L, 2000L, 3000L, 4300L, 6000L)
        );

        assertEquals(List.of(0L, 75L, 225L, 525L, 975L, 1700L, 2800L, 4400L, 6650L, 9750L), widened);
    }

    @Test
    void customThresholdsAreNormalizedToIncreasingGapSizes() {
        List<Long> widened = HeartOfTheEndMinesManager.normalizeThresholds(
                List.of(0L, 100L, 200L, 300L, 400L)
        );

        assertEquals(List.of(0L, 100L, 201L, 303L, 406L), widened);

        long previousGap = 0L;
        for (int index = 1; index < widened.size(); index++) {
            long gap = widened.get(index) - widened.get(index - 1);
            assertTrue(gap > previousGap);
            previousGap = gap;
        }
    }

    @Test
    void defaultTierTokenRewardsMatchHotmStyleDistribution() {
        List<Integer> rewards = HeartOfTheEndMinesManager.normalizeTierTokenRewards(List.of(), 10, 1);

        assertEquals(List.of(0, 1, 2, 2, 2, 2, 2, 3, 2, 2, 2), rewards);
    }

    @Test
    void legacyFlatTokenRewardOnlyPersistsWhenExplicitlyCustomized() {
        List<Integer> rewards = HeartOfTheEndMinesManager.normalizeTierTokenRewards(List.of(), 5, 3);

        assertEquals(List.of(0, 3, 3, 3, 3, 3), rewards);
    }
}
