package io.papermc.Grivience.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class FarmingSetBonusUtilTest {

    @Test
    void harvesterEmbraceAppliesDeterministicYieldBonusForWholePercentage() {
        assertEquals(15, FarmingSetBonusUtil.computeHarvesterEmbraceExtraDrops(2, 100));
        assertEquals(15, FarmingSetBonusUtil.computeHarvesterEmbraceExtraDrops(4, 100));
    }

    @Test
    void harvesterEmbraceRequiresTwoPieces() {
        assertEquals(0, FarmingSetBonusUtil.computeHarvesterEmbraceExtraDrops(1, 100));
    }

    @Test
    void gildedHarvesterFullSetAddsHealthOnlyAtFourPieces() {
        assertEquals(0.0D, FarmingSetBonusUtil.gildedHarvesterFullSetHealthBonus(3));
        assertEquals(50.0D, FarmingSetBonusUtil.gildedHarvesterFullSetHealthBonus(4));
    }

    @Test
    void collectionFarmingSetsExposeTheirConfiguredFullSetBonuses() {
        assertEquals(25.0D, FarmingSetBonusUtil.rootboundGarbFullSetFarmingFortuneBonus(4));
        assertEquals(60.0D, FarmingSetBonusUtil.taterguardFullSetHealthBonus(4));
        assertEquals(20.0D, FarmingSetBonusUtil.melonMonarchFullSetHealthBonus(4));
        assertEquals(35.0D, FarmingSetBonusUtil.melonMonarchFullSetFarmingFortuneBonus(4));
        assertEquals(75.0D, FarmingSetBonusUtil.wartwovenRegaliaFullSetIntelligenceBonus(4));
    }
}
