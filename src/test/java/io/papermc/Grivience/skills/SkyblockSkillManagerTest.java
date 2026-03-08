package io.papermc.Grivience.skills;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public final class SkyblockSkillManagerTest {

    @Test
    void xpThresholds_useSkillSpecificFallbacksWhenNoLevelManagerIsAvailable() {
        SkyblockSkillManager manager = new SkyblockSkillManager(null, null);

        assertEquals(60, manager.getMaxLevel());
        assertEquals(50, manager.getMaxLevel(SkyblockSkill.DUNGEONEERING));
        assertEquals(40.0D, manager.getXpForLevel(SkyblockSkill.COMBAT, 1), 1e-9);
        assertEquals(400.0D, manager.getXpForLevel(SkyblockSkill.COMBAT, 10), 1e-9);
        assertEquals(100.0D, manager.getXpForLevel(SkyblockSkill.ALCHEMY, 1), 1e-9);
        assertEquals(3.0D, manager.getXpForLevel(SkyblockSkill.DUNGEONEERING, 1), 1e-9);
        assertEquals(150.0D, manager.getXpForLevel(SkyblockSkill.DUNGEONEERING, 999), 1e-9);
    }

    @Test
    void legacyXpForLevelMethod_defaultsToCombatThresholds() {
        SkyblockSkillManager manager = new SkyblockSkillManager(null, null);

        assertEquals(120.0D, manager.getXpForLevel(3), 1e-9);
    }

    @Test
    void summaryHelpers_handleNullPlayerSafely() {
        SkyblockSkillManager manager = new SkyblockSkillManager(null, null);

        assertEquals(SkyblockSkill.values().length, manager.getTrackedSkillCount());
        assertEquals(0, manager.getTotalSkillLevels(null));
        assertEquals(0.0D, manager.getSkillAverage(null), 1e-9);
        assertNull(manager.getHighestSkill(null));
    }
}
