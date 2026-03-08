package io.papermc.Grivience.dungeon;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class FloorConfigTest {
    @Test
    void rewardsForGrade_prefersExactTier() {
        FloorConfig floor = floorWithRewards(Map.of(
                "S", List.of("reward_s"),
                "D", List.of("reward_d")
        ));

        assertEquals(List.of("reward_s"), floor.rewardsForGrade("S"));
        assertEquals(List.of("reward_s"), floor.rewardsForGrade("s"));
        assertEquals(List.of("reward_d"), floor.rewardsForGrade("D"));
    }

    @Test
    void rewardsForGrade_fallsBackToNextAvailableTier() {
        FloorConfig floor = floorWithRewards(Map.of(
                "D", List.of("reward_d")
        ));

        assertEquals(List.of("reward_d"), floor.rewardsForGrade("S"));
        assertEquals(List.of("reward_d"), floor.rewardsForGrade("A"));
        assertEquals(List.of("reward_d"), floor.rewardsForGrade("B"));
        assertEquals(List.of("reward_d"), floor.rewardsForGrade("C"));
        assertEquals(List.of("reward_d"), floor.rewardsForGrade("D"));
    }

    @Test
    void rewardsForGrade_skipsMissingIntermediateTiers() {
        FloorConfig floor = floorWithRewards(Map.of(
                "B", List.of("reward_b")
        ));

        assertEquals(List.of("reward_b"), floor.rewardsForGrade("S"));
        assertEquals(List.of("reward_b"), floor.rewardsForGrade("A"));
        assertEquals(List.of("reward_b"), floor.rewardsForGrade("B"));
        assertEquals(List.of(), floor.rewardsForGrade("C"));
        assertEquals(List.of(), floor.rewardsForGrade("D"));
    }

    private static FloorConfig floorWithRewards(Map<String, List<String>> rewardsByGrade) {
        Map<String, List<String>> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : rewardsByGrade.entrySet()) {
            normalized.put(entry.getKey().toUpperCase(Locale.ROOT), entry.getValue());
        }
        return new FloorConfig(
                "F1",
                "F1",
                1,
                5,
                0,
                0,
                0,
                List.of(),
                23,
                1,
                1.0D,
                0,
                true,
                List.of(),
                List.of(EntityType.ZOMBIE),
                EntityType.WITHER_SKELETON,
                "Boss",
                1.0D,
                Material.STONE,
                Material.STONE_BRICKS,
                normalized
        );
    }
}
