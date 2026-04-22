package io.papermc.Grivience.mines.end;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EndMinesConvergenceManagerTest {

    @Test
    void normalizeRequiredItemIdsFallsBackToDefaultsAndDeduplicates() {
        List<String> normalized = EndMinesConvergenceManager.normalizeRequiredItemIds(
                List.of("kunzite", " KUNZITE ", "rift_essence", "", "void_crystal")
        );

        assertEquals(List.of("KUNZITE", "RIFT_ESSENCE", "VOID_CRYSTAL"), normalized);
        assertEquals(
                List.of("KUNZITE", "RIFT_ESSENCE", "VOID_CRYSTAL", "OBSIDIAN_CORE", "CHORUS_WEAVE"),
                EndMinesConvergenceManager.normalizeRequiredItemIds(List.of())
        );
    }

    @Test
    void parseRewardEntriesSkipsInvalidRowsAndKeepsValidOnes() {
        List<EndMinesConvergenceManager.RewardEntry> entries = EndMinesConvergenceManager.parseRewardEntries(
                List.of(
                        "KUNZITE:2:6:10",
                        "bad-row",
                        "VOID_CRYSTAL:0:2:3",
                        "VOLTA:1:2:5"
                )
        );

        assertEquals(2, entries.size());
        assertEquals("KUNZITE", entries.get(0).itemId());
        assertEquals(2, entries.get(0).minAmount());
        assertEquals(6, entries.get(0).maxAmount());
        assertEquals(10, entries.get(0).weight());
        assertEquals("VOLTA", entries.get(1).itemId());
        assertTrue(entries.stream().noneMatch(entry -> entry.itemId().equals("VOID_CRYSTAL")));
    }
}
