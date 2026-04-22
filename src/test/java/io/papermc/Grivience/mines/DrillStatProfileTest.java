package io.papermc.Grivience.mines;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DrillStatProfileTest {

    @Test
    void engineTuningChangesLiveDrillBehavior() {
        DrillStatProfile.Profile basic = DrillStatProfile.resolve("IRONCREST_DRILL", "BASIC_ENGINE", "SMALL_TANK");
        DrillStatProfile.Profile titanium = DrillStatProfile.resolve("IRONCREST_DRILL", "TITANIUM_ENGINE", "SMALL_TANK");

        assertEquals(400, basic.miningSpeed());
        assertEquals(10, basic.fuelCostPerBlock());
        assertEquals(30_000L, basic.abilityCooldownMillis());
        assertEquals(100, basic.abilityDurationTicks());
        assertEquals(2, basic.abilityAmplifier());
        assertEquals(0, basic.crystalNodeHitReduction());

        assertEquals(500, titanium.miningSpeed());
        assertEquals(8, titanium.fuelCostPerBlock());
        assertEquals(26_000L, titanium.abilityCooldownMillis());
        assertEquals(140, titanium.abilityDurationTicks());
        assertEquals(2, titanium.abilityAmplifier());
        assertEquals(1, titanium.crystalNodeHitReduction());
    }

    @Test
    void topEndProfilesStackDrillTierEngineAndTank() {
        DrillStatProfile.Profile profile = DrillStatProfile.resolve("GEMSTONE_DRILL", "DIVAN_ENGINE", "LARGE_FUEL_TANK");

        assertEquals(1_200, profile.miningSpeed());
        assertEquals(8, profile.breakingPower());
        assertEquals(100_000, profile.maxFuel());
        assertEquals(6, profile.fuelCostPerBlock());
        assertEquals(22_000L, profile.abilityCooldownMillis());
        assertEquals(180, profile.abilityDurationTicks());
        assertEquals(3, profile.abilityAmplifier());
        assertEquals(2, profile.crystalNodeHitReduction());
    }

    @Test
    void unknownPartsFallBackToStockValues() {
        DrillStatProfile.Profile profile = DrillStatProfile.resolve("IRONCREST_DRILL", "unknown_engine", "unknown_tank");

        assertEquals("IRONCREST_DRILL", profile.drillId());
        assertEquals(DrillStatProfile.BASIC_ENGINE, profile.engineId());
        assertEquals(DrillStatProfile.SMALL_TANK, profile.tankId());
        assertEquals(400, profile.miningSpeed());
        assertEquals(20_000, profile.maxFuel());
        assertEquals(10, profile.fuelCostPerBlock());
    }
}
