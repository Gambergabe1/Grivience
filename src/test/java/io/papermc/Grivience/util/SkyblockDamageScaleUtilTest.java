package io.papermc.Grivience.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkyblockDamageScaleUtilTest {

    @Test
    void convertsSkyblockDamageUsingConfiguredHealthScale() {
        assertEquals(3.5D, SkyblockDamageScaleUtil.toMinecraftDamage(17.5D, 5.0D));
    }

    @Test
    void fallsBackToDefaultScaleWhenConfiguredScaleIsInvalid() {
        assertEquals(4.0D, SkyblockDamageScaleUtil.toMinecraftDamage(20.0D, 0.0D));
        assertEquals(4.0D, SkyblockDamageScaleUtil.toMinecraftDamage(20.0D, Double.NaN));
    }

    @Test
    void clampsNonPositiveDamageToZero() {
        assertEquals(0.0D, SkyblockDamageScaleUtil.toMinecraftDamage(0.0D, 5.0D));
        assertEquals(0.0D, SkyblockDamageScaleUtil.toMinecraftDamage(-5.0D, 5.0D));
    }
}
