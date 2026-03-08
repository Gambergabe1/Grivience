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
    void perkValue_isClampedForSpecialCases() {
        assertEquals(1.0D, SkyblockSkill.HUNTING.getPerkValue(30), 1e-9);
        assertEquals(5.0D, SkyblockSkill.FISHING.getPerkValue(200), 1e-9);
    }
}
