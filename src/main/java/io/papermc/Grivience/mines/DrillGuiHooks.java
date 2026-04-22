package io.papermc.Grivience.mines;

import java.util.List;

public final class DrillGuiHooks {
    private DrillGuiHooks() {
    }

    public record Snapshot(
            DrillSnapshot drill,
            ForgeSnapshot forge,
            List<QueueEntry> queue
    ) {
    }

    public record DrillSnapshot(
            boolean present,
            String itemId,
            String displayName,
            String engineId,
            String tankId,
            int fuel,
            int maxFuel,
            int fuelPercent,
            int estimatedRangeBlocks,
            DrillStatProfile.Profile baseProfile,
            int activeFuelCostPerBlock,
            long activeAbilityCooldownMillis,
            int activeAbilityDurationTicks,
            int activeAbilityAmplifier
    ) {
    }

    public record ForgeSnapshot(
            int heat,
            int maxHeat,
            int heatTier,
            int speedBonusPercent,
            int activeProjects,
            int maxQueueSize,
            int readyProjects,
            int totalClaims,
            boolean overdriveActive,
            int activeOverdriveTier,
            int ignitionPreviewTier,
            long overdriveRemainingMillis,
            long ignitionPreviewDurationMillis,
            int nextHeatMilestone,
            String nextHeatMilestoneName,
            int heatNeededForIgnition,
            int overdriveHeatCost,
            double overdriveCoinCost
    ) {
    }

    public record QueueEntry(
            String projectId,
            String displayName,
            String outputItemId,
            boolean ready,
            long startedAt,
            long readyAt,
            long totalDurationMillis,
            long remainingMillis,
            int heatGain,
            int forecastHeat,
            int forecastSpeedBonusPercent,
            int overdrivePreviewTier
    ) {
    }
}
