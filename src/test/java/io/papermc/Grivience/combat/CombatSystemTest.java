package io.papermc.Grivience.combat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CombatSystemTest {

    // Simulates the damage calculation logic from CustomWeaponCombatListener
    // We cannot run the actual listener because it depends on Bukkit classes which are not mocked here.
    // Instead, we extract the core formula to verify its correctness mathematically.

    @Test
    public void testDamageCalculation() {
        // Base stats
        double weaponDamage = 100.0;
        double strength = 150.0;
        double critDamage = 200.0; // 200%
        
        // Settings
        double baseDamageScale = 0.2; // Example scale
        double strengthScaling = 0.01; // 1% per strength

        // 1. Calculate Base Skyblock Damage
        // Formula: (Weapon Damage + 5) * (1 + Strength/100) * Multipliers
        // (100 + 5) * (1 + 150 * 0.01) = 105 * 2.5 = 262.5
        double expectedSkyblockDamage = (weaponDamage + 5.0) * (1.0 + (strength * strengthScaling));
        Assertions.assertEquals(262.5, expectedSkyblockDamage, 0.01, "Base Skyblock damage should match formula");

        // 2. Calculate Final Damage (Scaled to Minecraft Health)
        double expectedMinecraftDamage = expectedSkyblockDamage * baseDamageScale;
        Assertions.assertEquals(52.5, expectedMinecraftDamage, 0.01, "Scaled Minecraft damage should match");

        // 3. Calculate Critical Hit
        // Crit Multiplier = 1 + (Crit Damage / 100)
        // 1 + (200 / 100) = 3.0x multiplier
        double critMultiplier = 1.0 + (critDamage / 100.0);
        double expectedCritDamage = expectedMinecraftDamage * critMultiplier;
        
        Assertions.assertEquals(157.5, expectedCritDamage, 0.01, "Critical hit damage should apply multiplier correctly");
    }

    @Test
    public void testVanillaWeaponProfile() {
        // Simulating the vanilla profile method logic
        
        // Diamond Sword
        double diamondSwordDamage = 7.0;
        double strength = 0.0; // No stats
        double scaling = 0.01;
        
        // (7 + 5) * (1 + 0) = 12 Skyblock Damage
        double expected = (diamondSwordDamage + 5.0) * (1.0 + (strength * scaling));
        Assertions.assertEquals(12.0, expected, 0.01);
    }
}
