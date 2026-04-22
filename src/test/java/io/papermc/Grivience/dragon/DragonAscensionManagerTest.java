package io.papermc.Grivience.dragon;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DragonAscensionManagerTest {

    @Test
    void dragonHealthScalingIncreasesStrictlyPerTier() {
        double tierOneHealth = DragonAscensionManager.dragonMaxHealthForTier(1_000.0D, 250.0D, 1);
        double tierTwoHealth = DragonAscensionManager.dragonMaxHealthForTier(1_000.0D, 250.0D, 2);
        double tierThreeHealth = DragonAscensionManager.dragonMaxHealthForTier(1_000.0D, 250.0D, 3);
        double tierFourHealth = DragonAscensionManager.dragonMaxHealthForTier(1_000.0D, 250.0D, 4);
        double tierFiveHealth = DragonAscensionManager.dragonMaxHealthForTier(1_000.0D, 250.0D, 5);

        assertTrue(tierTwoHealth > tierOneHealth);
        assertTrue(tierThreeHealth > tierTwoHealth);
        assertTrue(tierFourHealth > tierThreeHealth);
        assertTrue(tierFiveHealth > tierFourHealth);
        
        // Tier 4: (1000 + 250*3) * 1.5 = 1750 * 1.5 = 2625
        assertEquals(2_625.0D, tierFourHealth, 0.0001D);
        // Tier 5: (1000 + 250*4) * 2.5 = 2000 * 2.5 = 5000
        assertEquals(5_000.0D, tierFiveHealth, 0.0001D);
    }

    @Test
    void dragonHealthScalingClampsTierBelowOneToTierOneValue() {
        assertEquals(
                DragonAscensionManager.dragonMaxHealthForTier(1_200.0D, 200.0D, 1),
                DragonAscensionManager.dragonMaxHealthForTier(1_200.0D, 200.0D, 0),
                0.0001D
        );
    }

    @Test
    void dragonShieldScalingMatchesTierProgression() {
        assertEquals(220.0D, DragonAscensionManager.dragonShieldForTier(220.0D, 55.0D, 1), 0.0001D);
        assertEquals(440.0D, DragonAscensionManager.dragonShieldForTier(220.0D, 55.0D, 5), 0.0001D);
    }

    @Test
    void dragonsSpineOnlyDropsForTierFourAndAboveAtOneIn650() {
        assertFalse(DragonAscensionManager.shouldDropDragonsSpine(3, 0.0D));
        assertTrue(DragonAscensionManager.shouldDropDragonsSpine(4, 0.0D));
        assertTrue(DragonAscensionManager.shouldDropDragonsSpine(5, (1.0D / 650.0D) - 1.0E-12D));
        assertFalse(DragonAscensionManager.shouldDropDragonsSpine(4, 1.0D / 650.0D));
    }

    @Test
    void dragonsSpineEligibilityRequiresMeaningfulParticipationAndTierFourPlus() {
        assertFalse(DragonAscensionManager.isDragonsSpineEligible(3, true));
        assertFalse(DragonAscensionManager.isDragonsSpineEligible(5, false));
        assertTrue(DragonAscensionManager.isDragonsSpineEligible(4, true));
        assertTrue(DragonAscensionManager.isDragonsSpineEligible(5, true));
    }

    @Test
    void dragonTypeLockClampPreventsEightEyeTierCollapse() {
        assertEquals(4, DragonAscensionManager.clampDragonTypeLockEyes(8, 8));
        assertEquals(4, DragonAscensionManager.clampDragonTypeLockEyes(8, 6));
        assertEquals(2, DragonAscensionManager.clampDragonTypeLockEyes(4, 4));
    }

    @Test
    void defaultPedestalPowerSpreadCanResolveTierFourAndTierFive() {
        List<Double> defaultPedestalPowers = List.of(1.0D, 2.8D, 2.5D, 2.3D, 3.0D, 3.5D, 2.7D, 3.8D);

        assertEquals(4, DragonAscensionManager.resolveTier(3.0D, defaultPedestalPowers, 4));
        assertEquals(5, DragonAscensionManager.resolveTier(3.275D, defaultPedestalPowers, 4));
    }

    @Test
    void fullEightEyeSummonDoesNotCollapseTierFourAndFiveBackToTierThree() {
        List<Double> defaultPedestalPowers = List.of(1.0D, 2.8D, 2.5D, 2.3D, 3.0D, 3.5D, 2.7D, 3.8D);

        assertEquals(4, DragonAscensionManager.resolveTier(2.7D, defaultPedestalPowers, 8));
        assertEquals(5, DragonAscensionManager.resolveTier(3.275D, defaultPedestalPowers, 8));
    }

    @Test
    void upperTierSpawnRollsAreRareAndFallBackCleanly() {
        assertEquals(4, DragonAscensionManager.resolveEncounterTierForSpawn(4, 0.0D, 0.75D, 0.02D, 0.0025D));
        assertEquals(3, DragonAscensionManager.resolveEncounterTierForSpawn(4, 0.02D, 0.75D, 0.02D, 0.0025D));

        double tierFourConditionalThreshold = DragonAscensionManager.tierFourConditionalRollChance(
                DragonAscensionManager.tierFourEncounterChance(5, 0.02D),
                DragonAscensionManager.tierFiveEncounterChance(5, 0.02D, 0.0025D)
        );
        assertEquals(5, DragonAscensionManager.resolveEncounterTierForSpawn(5, 0.75D, 0.0D, 0.02D, 0.0025D));
        assertEquals(4, DragonAscensionManager.resolveEncounterTierForSpawn(5, tierFourConditionalThreshold - 1.0E-12D, 0.75D, 0.02D, 0.0025D));
        assertEquals(3, DragonAscensionManager.resolveEncounterTierForSpawn(5, tierFourConditionalThreshold, 0.75D, 0.02D, 0.0025D));
    }

    @Test
    void upperTierEncounterChanceHelpersExposeExactTierOdds() {
        assertEquals(0.02D, DragonAscensionManager.tierFourEncounterChance(4, 0.02D), 0.0000001D);
        assertEquals(0.02D, DragonAscensionManager.tierFourEncounterChance(5, 0.02D), 0.0000001D);
        assertEquals(0.0D, DragonAscensionManager.tierFiveEncounterChance(4, 0.02D, 0.0025D), 0.0000001D);
        assertEquals(0.0025D, DragonAscensionManager.tierFiveEncounterChance(5, 0.02D, 0.0025D), 0.0000001D);
        assertEquals(0.25D, DragonAscensionManager.tierFiveEncounterChance(5, 0.75D, 0.50D), 0.0000001D);
        assertEquals(1.0D, DragonAscensionManager.tierFourConditionalRollChance(0.75D, 0.25D), 0.0000001D);
        assertEquals(0.0D, DragonAscensionManager.tierFourConditionalRollChance(0.75D, 1.0D), 0.0000001D);
    }

    @Test
    void rareTierChanceSanitizerClampsInvalidValues() {
        assertEquals(0.0D, DragonAscensionManager.sanitizeRareTierChance(Double.NaN), 0.0001D);
        assertEquals(0.0D, DragonAscensionManager.sanitizeRareTierChance(-1.0D), 0.0001D);
        assertEquals(1.0D, DragonAscensionManager.sanitizeRareTierChance(4.0D), 0.0001D);
        assertEquals(0.25D, DragonAscensionManager.sanitizeRareTierChance(0.25D), 0.0001D);
    }

    @Test
    void upperTierCadenceTightensWithoutChangingLowerTierBaselines() {
        assertEquals(80L, DragonAscensionManager.aerialVolleyBasePeriod(3));
        assertEquals(65L, DragonAscensionManager.aerialVolleyBasePeriod(4));
        assertEquals(50L, DragonAscensionManager.aerialVolleyBasePeriod(5));

        assertEquals(70L, DragonAscensionManager.targetedStrikeBasePeriod(3, false));
        assertEquals(55L, DragonAscensionManager.targetedStrikeBasePeriod(4, false));
        assertEquals(45L, DragonAscensionManager.targetedStrikeBasePeriod(5, false));

        assertEquals(55L, DragonAscensionManager.targetedStrikeBasePeriod(3, true));
        assertEquals(45L, DragonAscensionManager.targetedStrikeBasePeriod(4, true));
        assertEquals(35L, DragonAscensionManager.targetedStrikeBasePeriod(5, true));

        assertEquals(60L, DragonAscensionManager.hazardBasePeriod(3));
        assertEquals(50L, DragonAscensionManager.hazardBasePeriod(4));
        assertEquals(40L, DragonAscensionManager.hazardBasePeriod(5));
    }

    @Test
    void periodCrossingsTrackNonDivisibleTierCadencesOnTenTickLoop() {
        assertEquals(0, DragonAscensionManager.periodCrossings(0L, 10L, DragonAscensionManager.aerialVolleyBasePeriod(4)));
        assertEquals(1, DragonAscensionManager.periodCrossings(60L, 70L, DragonAscensionManager.aerialVolleyBasePeriod(4)));
        assertEquals(1, DragonAscensionManager.periodCrossings(50L, 60L, DragonAscensionManager.targetedStrikeBasePeriod(4, false)));
        assertEquals(1, DragonAscensionManager.periodCrossings(30L, 40L, DragonAscensionManager.targetedStrikeBasePeriod(5, true)));
        assertEquals(1, DragonAscensionManager.periodCrossings(40L, 50L, 15L));
    }

    @Test
    void periodCrossingsAccumulateMissedPulsesWhenTickStepIsWiderThanCadence() {
        assertEquals(2, DragonAscensionManager.periodCrossings(20L, 50L, 15L));
        assertEquals(3, DragonAscensionManager.periodCrossings(0L, 35L, 10L));
    }

    @Test
    void upperTierMechanicCountsEscalateForEpicPhases() {
        assertEquals(3, DragonAscensionManager.aerialVolleyCount(3));
        assertEquals(4, DragonAscensionManager.aerialVolleyCount(4));
        assertEquals(5, DragonAscensionManager.aerialVolleyCount(5));

        assertEquals(1, DragonAscensionManager.hazardClusterCount(3));
        assertEquals(2, DragonAscensionManager.hazardClusterCount(4));
        assertEquals(3, DragonAscensionManager.hazardClusterCount(5));

        assertEquals(0, DragonAscensionManager.meteorEchoCount(3, false));
        assertEquals(2, DragonAscensionManager.meteorEchoCount(4, false));
        assertEquals(3, DragonAscensionManager.meteorEchoCount(4, true));
        assertEquals(3, DragonAscensionManager.meteorEchoCount(5, false));
        assertEquals(4, DragonAscensionManager.meteorEchoCount(5, true));
    }

    @Test
    void cataclysmOnlyUnlocksForTierFive() {
        assertFalse(DragonAscensionManager.cataclysmEnabled(4));
        assertTrue(DragonAscensionManager.cataclysmEnabled(5));
        assertEquals(Long.MAX_VALUE, DragonAscensionManager.cataclysmBasePeriod(4));
        assertEquals(90L, DragonAscensionManager.cataclysmBasePeriod(5));
    }

    @Test
    void dragonHeartDropsOnlyFromTierFiveAtOneInTenThousand() {
        double dragonHeartChance = 1.0D / 10000.0D;
        assertFalse(DragonAscensionManager.shouldDropDragonHeart(4, 0.0D));
        assertTrue(DragonAscensionManager.shouldDropDragonHeart(5, dragonHeartChance - 1.0E-12D));
        assertFalse(DragonAscensionManager.shouldDropDragonHeart(5, dragonHeartChance));
    }
}
