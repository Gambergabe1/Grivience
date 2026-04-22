package io.papermc.Grivience.compactor;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class PersonalCompactorManagerTest {

    @Test
    void resolveSelectedSlotKeepsRequestedUnlockedSlot() {
        assertEquals(4, PersonalCompactorManager.resolveSelectedSlot(6, Map.of(), 4));
    }

    @Test
    void resolveSelectedSlotFallsBackToFirstEmptyUnlockedSlot() {
        assertEquals(2, PersonalCompactorManager.resolveSelectedSlot(5, Map.of(
                0, "enchanted_cobblestone",
                1, "enchanted_coal"
        ), null));
    }

    @Test
    void resolveSelectedSlotIgnoresLockedRequestedSlot() {
        assertEquals(1, PersonalCompactorManager.resolveSelectedSlot(3, Map.of(
                0, "enchanted_cobblestone"
        ), 9));
    }

    @Test
    void resolveSelectedSlotReturnsNullWhenAllUnlockedSlotsAreFilled() {
        assertNull(PersonalCompactorManager.resolveSelectedSlot(3, Map.of(
                0, "enchanted_cobblestone",
                1, "enchanted_coal",
                2, "enchanted_diamond"
        ), null));
    }
}
