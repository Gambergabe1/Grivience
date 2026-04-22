package io.papermc.Grivience.skills;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public final class SkyblockSkillTest {

    @Test
    void parse_supportsEnumDisplayAndAliasForms() {
        assertEquals(SkyblockSkill.COMBAT, SkyblockSkill.parse("combat"));
        assertEquals(SkyblockSkill.FORAGING, SkyblockSkill.parse("Foraging"));
        assertEquals(SkyblockSkill.DUNGEONEERING, SkyblockSkill.parse("dungeoneering"));
        assertEquals(SkyblockSkill.DUNGEONEERING, SkyblockSkill.parse("catacombs"));
    }

    @Test
    void parse_returnsNullForUnknownSkill() {
        assertNull(SkyblockSkill.parse("unknown_skill"));
        assertNull(SkyblockSkill.parse(""));
        assertNull(SkyblockSkill.parse(null));
    }

    @Test
    void perkValueScalingIsAccurate() {
        // Combat: 0.5 per level
        assertEquals(0.5D, SkyblockSkill.COMBAT.getPerkValue(1), 1e-9);
        assertEquals(25.0D, SkyblockSkill.COMBAT.getPerkValue(50), 1e-9);

        // Mining: 2.0 per level
        assertEquals(2.0D, SkyblockSkill.MINING.getPerkValue(1), 1e-9);
        assertEquals(120.0D, SkyblockSkill.MINING.getPerkValue(60), 1e-9);

        // Fishing: Starts at 0.2, ends at 12.0 (Level 60)
        assertEquals(0.2D, SkyblockSkill.FISHING.getPerkValue(1), 1e-9);
        assertEquals(12.0D, SkyblockSkill.FISHING.getPerkValue(60), 1e-9);
    }
}
