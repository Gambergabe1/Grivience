package io.papermc.Grivience.combat;

public final class CombatDamageModel {
    private static final double DEFAULT_DAMAGE_SCALE = 0.2D;

    private CombatDamageModel() {
    }

    public static AttackResult computeWeaponAttack(
            double weaponDamage,
            double strength,
            double critDamagePercent,
            boolean critical,
            double strengthScaling,
            double enchantMultiplier,
            double perkMultiplier,
            double damageScale,
            double dungeonMultiplier,
            double outgoingMultiplier,
            double incomingMultiplier
    ) {
        double displayDamage = (Math.max(0.0D, weaponDamage) + 5.0D)
                * strengthMultiplier(strength, strengthScaling)
                * sanitizeMultiplier(enchantMultiplier)
                * sanitizeMultiplier(perkMultiplier)
                * criticalMultiplier(critDamagePercent, critical)
                * sanitizeMultiplier(dungeonMultiplier)
                * sanitizeMultiplier(outgoingMultiplier)
                * sanitizeMultiplier(incomingMultiplier);
        return new AttackResult(
                Math.max(0.0D, displayDamage * normalizeDamageScale(damageScale)),
                Math.max(0.0D, displayDamage),
                critical
        );
    }

    public static AttackResult computeAbilityAttack(
            double baseDisplayDamage,
            double strength,
            double intelligence,
            double critDamagePercent,
            boolean critical,
            double strengthScaling,
            double damageScale,
            double outgoingMultiplier,
            double storedMultiplier,
            double dungeonMultiplier,
            double incomingMultiplier
    ) {
        double displayDamage = Math.max(0.0D, baseDisplayDamage)
                * strengthMultiplier(strength, strengthScaling)
                * intelligenceMultiplier(intelligence)
                * criticalMultiplier(critDamagePercent, critical)
                * sanitizeMultiplier(outgoingMultiplier)
                * sanitizeMultiplier(storedMultiplier)
                * sanitizeMultiplier(dungeonMultiplier)
                * sanitizeMultiplier(incomingMultiplier);
        return new AttackResult(
                Math.max(0.0D, displayDamage * normalizeDamageScale(damageScale)),
                Math.max(0.0D, displayDamage),
                critical
        );
    }

    public static double clampCritChance(double critChancePercent) {
        if (!Double.isFinite(critChancePercent)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(100.0D, critChancePercent));
    }

    public static boolean isCriticalHit(double critChancePercent, double roll) {
        if (!Double.isFinite(roll)) {
            return false;
        }
        return roll < (clampCritChance(critChancePercent) / 100.0D);
    }

    public static double displayDamageFromAppliedMcDamage(double minecraftDamage, double damageScale) {
        if (!Double.isFinite(minecraftDamage) || minecraftDamage <= 0.0D) {
            return 0.0D;
        }
        return minecraftDamage / normalizeDamageScale(damageScale);
    }

    private static double strengthMultiplier(double strength, double strengthScaling) {
        double safeStrength = Double.isFinite(strength) ? Math.max(0.0D, strength) : 0.0D;
        double safeScaling = Double.isFinite(strengthScaling) ? Math.max(0.0D, strengthScaling) : 0.0D;
        return Math.max(0.2D, 1.0D + (safeStrength * safeScaling));
    }

    private static double intelligenceMultiplier(double intelligence) {
        double safeIntelligence = Double.isFinite(intelligence) ? Math.max(0.0D, intelligence) : 0.0D;
        return Math.max(0.25D, 1.0D + (safeIntelligence / 100.0D));
    }

    private static double criticalMultiplier(double critDamagePercent, boolean critical) {
        if (!critical) {
            return 1.0D;
        }
        double safeCritDamage = Double.isFinite(critDamagePercent) ? Math.max(0.0D, critDamagePercent) : 0.0D;
        return Math.max(1.0D, 1.0D + (safeCritDamage / 100.0D));
    }

    private static double sanitizeMultiplier(double value) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            return 1.0D;
        }
        return value;
    }

    private static double normalizeDamageScale(double damageScale) {
        if (!Double.isFinite(damageScale) || damageScale <= 0.0D) {
            return DEFAULT_DAMAGE_SCALE;
        }
        return damageScale;
    }

    public record AttackResult(
            double minecraftDamage,
            double displayDamage,
            boolean critical
    ) {
        public AttackResult scale(double multiplier) {
            double safeMultiplier = sanitizeMultiplier(multiplier);
            return new AttackResult(minecraftDamage * safeMultiplier, displayDamage * safeMultiplier, critical);
        }
    }
}
