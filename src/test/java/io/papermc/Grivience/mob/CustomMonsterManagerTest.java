package io.papermc.Grivience.mob;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomMonsterManagerTest {

    @Test
    void shadowStalkerGetsFallbackSummoningEyeDrop() {
        CustomMonster monster = new CustomMonster("shadow_stalker");

        CustomMonsterManager.ensureDefaultDrops(monster);

        assertEquals(1, monster.getDrops().size());
        MonsterDrop drop = monster.getDrops().get(0);
        assertEquals("SUMMONING_EYE", drop.getCustomItemId());
        assertEquals(0.05D, drop.getChance(), 1.0E-12D);
        assertEquals(1, drop.getMinAmount());
        assertEquals(1, drop.getMaxAmount());
    }

    @Test
    void existingSummoningEyeDropIsNormalizedWithoutDuplication() {
        CustomMonster monster = new CustomMonster("shadow_stalker");
        monster.getDrops().add(new MonsterDrop(true, "SUMMONING_EYE", 0.25D, 1, 1));

        CustomMonsterManager.ensureDefaultDrops(monster);

        assertEquals(1, monster.getDrops().size());
        assertEquals(0.05D, monster.getDrops().get(0).getChance(), 1.0E-12D);
    }
}
