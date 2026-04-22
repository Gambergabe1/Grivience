package io.papermc.Grivience.dragon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DragonAscensionCombatModelTest {

    @Test
    void computeScoreCombinesDamageObjectivesAndPresence() {
        DragonAscensionCombatModel.ContributionSnapshot contribution = new DragonAscensionCombatModel.ContributionSnapshot(
                2_500.0D,
                2,
                4,
                3,
                400L
        );
        DragonAscensionCombatModel.FightTotals totals = new DragonAscensionCombatModel.FightTotals(5_000.0D);
        DragonAscensionCombatModel.ScoreWeights weights = new DragonAscensionCombatModel.ScoreWeights(
                50.0D,
                8.0D,
                2.0D,
                5.0D,
                0.5D,
                10.0D
        );

        double score = DragonAscensionCombatModel.computeScore(contribution, totals, weights);

        assertEquals(74.0D, score, 0.001D);
    }

    @Test
    void rewardTierUsesDescendingThresholds() {
        DragonAscensionCombatModel.Thresholds thresholds = new DragonAscensionCombatModel.Thresholds(85.0D, 65.0D, 40.0D, 20.0D);

        assertEquals(DragonAscensionCombatModel.RewardTier.S, DragonAscensionCombatModel.rewardTier(90.0D, thresholds));
        assertEquals(DragonAscensionCombatModel.RewardTier.A, DragonAscensionCombatModel.rewardTier(70.0D, thresholds));
        assertEquals(DragonAscensionCombatModel.RewardTier.B, DragonAscensionCombatModel.rewardTier(45.0D, thresholds));
        assertEquals(DragonAscensionCombatModel.RewardTier.C, DragonAscensionCombatModel.rewardTier(25.0D, thresholds));
        assertEquals(DragonAscensionCombatModel.RewardTier.NONE, DragonAscensionCombatModel.rewardTier(10.0D, thresholds));
    }

    @Test
    void meaningfulParticipationRequiresPresenceAndAction() {
        DragonAscensionCombatModel.ContributionSnapshot activeFighter = new DragonAscensionCombatModel.ContributionSnapshot(
                150.0D,
                0,
                0,
                0,
                40L
        );
        DragonAscensionCombatModel.ContributionSnapshot passiveViewer = new DragonAscensionCombatModel.ContributionSnapshot(
                0.0D,
                0,
                0,
                0,
                200L
        );
        DragonAscensionCombatModel.ContributionSnapshot absentSummoner = new DragonAscensionCombatModel.ContributionSnapshot(
                0.0D,
                0,
                0,
                2,
                0L
        );

        assertTrue(DragonAscensionCombatModel.hasMeaningfulParticipation(activeFighter));
        assertFalse(DragonAscensionCombatModel.hasMeaningfulParticipation(passiveViewer));
        assertFalse(DragonAscensionCombatModel.hasMeaningfulParticipation(absentSummoner));
    }

    @Test
    void rewardEligibilityKeepsLootGateSeparateFromCodexTracking() {
        DragonAscensionCombatModel.ContributionSnapshot contributor = new DragonAscensionCombatModel.ContributionSnapshot(
                300.0D,
                1,
                1,
                1,
                400L
        );

        assertTrue(DragonAscensionCombatModel.qualifiesForRewards(contributor, 24.0D, 10, 20.0D));
        assertFalse(DragonAscensionCombatModel.qualifiesForRewards(contributor, 8.0D, 10, 20.0D));
        assertFalse(DragonAscensionCombatModel.qualifiesForRewards(contributor, 24.0D, 30, 20.0D));
    }

    @Test
    void reputationPerksClampAtConfiguredCaps() {
        assertEquals(1.0D, DragonAscensionCombatModel.damageBonusMultiplier(0), 0.0001D);
        assertEquals(1.20D, DragonAscensionCombatModel.damageBonusMultiplier(50), 0.0001D);
        assertEquals(0.85D, DragonAscensionCombatModel.resistanceMultiplier(50), 0.0001D);
        assertEquals(1.10D, DragonAscensionCombatModel.lootChanceMultiplier(50), 0.0001D);
    }
}
