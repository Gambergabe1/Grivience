package io.papermc.Grivience.dungeon;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class DungeonBuilderMaterialTest {
    @Test
    void solidAccentMaterial_usesBlackConcrete() {
        assertEquals(Material.BLACK_CONCRETE, DungeonBuilder.solidAccentMaterial());
    }

    @Test
    void gateBarrierMaterial_usesSolidAccentAcrossDungeonFloors() {
        assertEquals(Material.BLACK_CONCRETE, DungeonBuilder.gateBarrierMaterial(floor("F1")));
        assertEquals(Material.BLACK_CONCRETE, DungeonBuilder.gateBarrierMaterial(floor("F2")));
        assertEquals(Material.BLACK_CONCRETE, DungeonBuilder.gateBarrierMaterial(floor("F3")));
        assertEquals(Material.BLACK_CONCRETE, DungeonBuilder.gateBarrierMaterial(floor("F4")));
        assertEquals(Material.BLACK_CONCRETE, DungeonBuilder.gateBarrierMaterial(floor("sanctum")));
    }

    private static FloorConfig floor(String id) {
        return new FloorConfig(
                id,
                id,
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
                Map.of()
        );
    }
}
