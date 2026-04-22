package io.papermc.Grivience.mines;

import java.util.Locale;

public final class DrillStatProfile {
    public static final String BASIC_ENGINE = "BASIC_ENGINE";
    public static final String SMALL_TANK = "SMALL_TANK";
    public static final int BASE_FUEL_PER_BLOCK = 10;
    public static final long BASE_ABILITY_COOLDOWN_MILLIS = 30_000L;
    public static final int BASE_ABILITY_DURATION_TICKS = 100;
    public static final int BASE_ABILITY_AMPLIFIER = 2;

    private DrillStatProfile() {
    }

    public record EngineTuning(
            String id,
            int miningSpeedBonus,
            int fuelReductionPerBlock,
            long abilityCooldownReductionMillis,
            int abilityDurationBonusTicks,
            int abilityAmplifierBonus
    ) {
    }

    public record TankTuning(String id, int maxFuel) {
    }

    public record Profile(
            String drillId,
            String engineId,
            String tankId,
            int miningSpeed,
            int breakingPower,
            int maxFuel,
            int fuelCostPerBlock,
            long abilityCooldownMillis,
            int abilityDurationTicks,
            int abilityAmplifier,
            int crystalNodeHitReduction
    ) {
    }

    public static boolean isDrillId(String drillId) {
        return drillId != null && drillId.toUpperCase(Locale.ROOT).endsWith("_DRILL");
    }

    public static Profile resolve(String drillId, String engineId, String tankId) {
        String normalizedDrillId = normalizeDrillId(drillId);
        EngineTuning engine = engineTuning(engineId);
        TankTuning tank = tankTuning(tankId);
        int miningSpeed = baseMiningSpeedFor(normalizedDrillId) + engine.miningSpeedBonus();
        return new Profile(
                normalizedDrillId,
                engine.id(),
                tank.id(),
                miningSpeed,
                breakingPowerFor(normalizedDrillId),
                tank.maxFuel(),
                Math.max(1, BASE_FUEL_PER_BLOCK - engine.fuelReductionPerBlock()),
                Math.max(12_000L, BASE_ABILITY_COOLDOWN_MILLIS - engine.abilityCooldownReductionMillis()),
                BASE_ABILITY_DURATION_TICKS + engine.abilityDurationBonusTicks(),
                BASE_ABILITY_AMPLIFIER + engine.abilityAmplifierBonus(),
                crystalNodeHitReduction(miningSpeed)
        );
    }

    public static EngineTuning engineTuning(String engineId) {
        String normalizedEngineId = normalizeEngineId(engineId);
        return switch (normalizedEngineId) {
            case "MITHRIL_ENGINE" -> new EngineTuning(normalizedEngineId, 50, 1, 2_000L, 20, 0);
            case "TITANIUM_ENGINE" -> new EngineTuning(normalizedEngineId, 100, 2, 4_000L, 40, 0);
            case "GEMSTONE_ENGINE" -> new EngineTuning(normalizedEngineId, 150, 3, 6_000L, 60, 1);
            case "DIVAN_ENGINE" -> new EngineTuning(normalizedEngineId, 200, 4, 8_000L, 80, 1);
            default -> new EngineTuning(BASIC_ENGINE, 0, 0, 0L, 0, 0);
        };
    }

    public static TankTuning tankTuning(String tankId) {
        String normalizedTankId = normalizeTankId(tankId);
        return switch (normalizedTankId) {
            case "MEDIUM_FUEL_TANK" -> new TankTuning(normalizedTankId, 50_000);
            case "LARGE_FUEL_TANK" -> new TankTuning(normalizedTankId, 100_000);
            default -> new TankTuning(SMALL_TANK, 20_000);
        };
    }

    public static int baseMiningSpeedFor(String drillId) {
        return switch (normalizeDrillId(drillId)) {
            case "TITANIUM_DRILL" -> 800;
            case "GEMSTONE_DRILL" -> 1_000;
            default -> 400;
        };
    }

    public static int breakingPowerFor(String drillId) {
        return switch (normalizeDrillId(drillId)) {
            case "TITANIUM_DRILL" -> 7;
            case "GEMSTONE_DRILL" -> 8;
            default -> 5;
        };
    }

    public static int crystalNodeHitReduction(int miningSpeed) {
        return Math.max(0, Math.min(2, Math.max(0, miningSpeed) / 500));
    }

    private static String normalizeDrillId(String drillId) {
        if (!isDrillId(drillId)) {
            return "IRONCREST_DRILL";
        }
        return drillId.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeEngineId(String engineId) {
        return engineId == null || engineId.isBlank() ? BASIC_ENGINE : engineId.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeTankId(String tankId) {
        return tankId == null || tankId.isBlank() ? SMALL_TANK : tankId.trim().toUpperCase(Locale.ROOT);
    }
}
