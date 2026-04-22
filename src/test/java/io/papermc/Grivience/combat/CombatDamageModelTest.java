package io.papermc.Grivience.combat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CombatDamageModelTest {

    @Test
    void computeWeaponAttack_appliesStatsCritAndMultipliers() {
        CombatDamageModel.AttackResult attack = CombatDamageModel.computeWeaponAttack(
                100.0D,
                150.0D,
                200.0D,
                true,
                0.01D,
                1.10D,
                1.05D,
                0.20D,
                1.08D,
                1.40D,
                1.15D
        );

        double expectedDisplay = (100.0D + 5.0D)
                * (1.0D + (150.0D * 0.01D))
                * 1.10D
                * 1.05D
                * (1.0D + (200.0D / 100.0D))
                * 1.08D
                * 1.40D
                * 1.15D;

        Assertions.assertEquals(expectedDisplay, attack.displayDamage(), 1.0E-9D);
        Assertions.assertEquals(expectedDisplay * 0.20D, attack.minecraftDamage(), 1.0E-9D);
        Assertions.assertTrue(attack.critical());
    }

    @Test
    void computeAbilityAttack_appliesScalingCritAndAbilityBonuses() {
        CombatDamageModel.AttackResult attack = CombatDamageModel.computeAbilityAttack(
                120.0D,
                200.0D,
                300.0D,
                150.0D,
                true,
                0.01D,
                0.20D,
                1.40D,
                1.30D,
                1.24D,
                1.15D
        );

        double expectedDisplay = 120.0D
                * (1.0D + (200.0D * 0.01D))
                * (1.0D + (300.0D / 100.0D))
                * (1.0D + (150.0D / 100.0D))
                * 1.40D
                * 1.30D
                * 1.24D
                * 1.15D;

        Assertions.assertEquals(expectedDisplay, attack.displayDamage(), 1.0E-9D);
        Assertions.assertEquals(expectedDisplay * 0.20D, attack.minecraftDamage(), 1.0E-9D);
        Assertions.assertTrue(attack.critical());
    }

    @Test
    void displayDamageFromAppliedMcDamage_roundTripsBackToDisplayedDamage() {
        CombatDamageModel.AttackResult attack = CombatDamageModel.computeAbilityAttack(
                175.0D,
                80.0D,
                120.0D,
                50.0D,
                false,
                0.01D,
                0.20D,
                1.10D,
                1.00D,
                1.08D,
                1.00D
        );

        double displayed = CombatDamageModel.displayDamageFromAppliedMcDamage(attack.minecraftDamage(), 0.20D);
        Assertions.assertEquals(attack.displayDamage(), displayed, 1.0E-9D);
    }

    @Test
    void critChance_isClampedBeforeRoll() {
        Assertions.assertEquals(100.0D, CombatDamageModel.clampCritChance(180.0D));
        Assertions.assertEquals(0.0D, CombatDamageModel.clampCritChance(-15.0D));
        Assertions.assertEquals(0.0D, CombatDamageModel.clampCritChance(Double.NaN));

        Assertions.assertTrue(CombatDamageModel.isCriticalHit(180.0D, 0.9999D));
        Assertions.assertFalse(CombatDamageModel.isCriticalHit(-15.0D, 0.0D));
        Assertions.assertFalse(CombatDamageModel.isCriticalHit(50.0D, Double.NaN));
    }
}
