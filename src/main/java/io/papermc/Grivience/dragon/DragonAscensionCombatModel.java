package io.papermc.Grivience.dragon;

public final class DragonAscensionCombatModel {
    private DragonAscensionCombatModel() {
    }

    public enum RewardTier {
        S,
        A,
        B,
        C,
        NONE
    }

    public record ContributionSnapshot(
            double damageDealt,
            int crystalsDestroyed,
            int mechanicScore,
            int eyesPlaced,
            long activeTicks
    ) {
    }

    public record FightTotals(double topDamageDealt) {
    }

    public record ScoreWeights(
            double maxDamageSharePoints,
            double crystalPoints,
            double mechanicPointWeight,
            double eyePoints,
            double presencePointPerSecond,
            double maxPresencePoints
    ) {
    }

    public record Thresholds(
            double sTier,
            double aTier,
            double bTier,
            double cTier
    ) {
    }

    public static double computeScore(ContributionSnapshot contribution, FightTotals totals, ScoreWeights weights) {
        if (contribution == null || totals == null || weights == null) {
            return 0.0D;
        }

        double topDamage = Math.max(0.0D, totals.topDamageDealt());
        double damageShare = topDamage <= 0.0D
                ? 0.0D
                : clamp(contribution.damageDealt() / topDamage, 0.0D, 1.0D) * Math.max(0.0D, weights.maxDamageSharePoints());

        double crystalScore = Math.max(0, contribution.crystalsDestroyed()) * Math.max(0.0D, weights.crystalPoints());
        double mechanicScore = Math.max(0, contribution.mechanicScore()) * Math.max(0.0D, weights.mechanicPointWeight());
        double eyeScore = Math.max(0, contribution.eyesPlaced()) * Math.max(0.0D, weights.eyePoints());

        double activeSeconds = Math.max(0L, contribution.activeTicks()) / 20.0D;
        double presenceScore = Math.min(
                Math.max(0.0D, weights.maxPresencePoints()),
                activeSeconds * Math.max(0.0D, weights.presencePointPerSecond())
        );

        return damageShare + crystalScore + mechanicScore + eyeScore + presenceScore;
    }

    public static RewardTier rewardTier(double score, Thresholds thresholds) {
        if (thresholds == null || score <= 0.0D) {
            return RewardTier.NONE;
        }
        if (score >= thresholds.sTier()) {
            return RewardTier.S;
        }
        if (score >= thresholds.aTier()) {
            return RewardTier.A;
        }
        if (score >= thresholds.bTier()) {
            return RewardTier.B;
        }
        if (score >= thresholds.cTier()) {
            return RewardTier.C;
        }
        return RewardTier.NONE;
    }

    public static boolean hasMeaningfulParticipation(ContributionSnapshot contribution) {
        if (contribution == null || contribution.activeTicks() <= 0L) {
            return false;
        }
        return contribution.damageDealt() > 0.0D
                || contribution.crystalsDestroyed() > 0
                || contribution.mechanicScore() > 0
                || contribution.eyesPlaced() > 0;
    }

    public static boolean qualifiesForRewards(
            ContributionSnapshot contribution,
            double score,
            int minimumActiveSeconds,
            double minimumScore
    ) {
        if (contribution == null || !hasMeaningfulParticipation(contribution)) {
            return false;
        }
        return contribution.activeTicks() >= Math.max(0, minimumActiveSeconds) * 20L
                && score >= Math.max(0.0D, minimumScore);
    }

    public static double damageBonusMultiplier(int reputationLevel) {
        int safeLevel = Math.max(0, reputationLevel);
        return 1.0D + Math.min(0.20D, safeLevel * 0.02D);
    }

    public static double resistanceMultiplier(int reputationLevel) {
        int safeLevel = Math.max(0, reputationLevel);
        return 1.0D - Math.min(0.15D, safeLevel * 0.01D);
    }

    public static double lootChanceMultiplier(int reputationLevel) {
        int safeLevel = Math.max(0, reputationLevel);
        return 1.0D + Math.min(0.10D, safeLevel * 0.01D);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
