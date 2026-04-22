package io.papermc.Grivience.farming;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class FarmingContestRulesTest {
    @Test
    void selectContestCropsIsDeterministicAndUnique() {
        List<FarmingContestCrop> pool = FarmingContestCrop.parsePool(List.of(
                "wheat",
                "carrot",
                "potato",
                "sugar_cane",
                "nether_wart",
                "cactus"
        ));

        List<FarmingContestCrop> first = FarmingContestRules.selectContestCrops(pool, 12345L, 42L, 3);
        List<FarmingContestCrop> second = FarmingContestRules.selectContestCrops(pool, 12345L, 42L, 3);
        List<FarmingContestCrop> different = FarmingContestRules.selectContestCrops(pool, 12345L, 43L, 3);

        assertEquals(3, first.size());
        assertEquals(3, first.stream().distinct().count());
        assertIterableEquals(first, second);
        assertNotEquals(first, different);
    }

    @Test
    void resolveBracketUsesHypixelStylePercentThresholds() {
        assertEquals(FarmingContestRules.Bracket.DIAMOND,
                FarmingContestRules.resolveBracket(100, 2, 0.60D, 0.30D, 0.10D, 0.05D, 0.02D));
        assertEquals(FarmingContestRules.Bracket.PLATINUM,
                FarmingContestRules.resolveBracket(100, 5, 0.60D, 0.30D, 0.10D, 0.05D, 0.02D));
        assertEquals(FarmingContestRules.Bracket.GOLD,
                FarmingContestRules.resolveBracket(100, 10, 0.60D, 0.30D, 0.10D, 0.05D, 0.02D));
        assertEquals(FarmingContestRules.Bracket.SILVER,
                FarmingContestRules.resolveBracket(100, 30, 0.60D, 0.30D, 0.10D, 0.05D, 0.02D));
        assertEquals(FarmingContestRules.Bracket.BRONZE,
                FarmingContestRules.resolveBracket(100, 60, 0.60D, 0.30D, 0.10D, 0.05D, 0.02D));
        assertEquals(FarmingContestRules.Bracket.NONE,
                FarmingContestRules.resolveBracket(100, 61, 0.60D, 0.30D, 0.10D, 0.05D, 0.02D));
    }

    @Test
    void cropParsingSupportsAliases() {
        assertEquals(FarmingContestCrop.SUGAR_CANE, FarmingContestCrop.fromInput("sugarcane"));
        assertEquals(FarmingContestCrop.NETHER_WART, FarmingContestCrop.fromInput("nether wart"));
        assertEquals(FarmingContestCrop.COCOA_BEANS, FarmingContestCrop.fromInput("cocoa"));
        assertEquals(FarmingContestCrop.MUSHROOM, FarmingContestCrop.fromInput("brown_mushroom"));
    }
}
