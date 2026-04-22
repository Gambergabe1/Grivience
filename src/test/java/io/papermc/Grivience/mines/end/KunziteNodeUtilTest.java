package io.papermc.Grivience.mines.end;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class KunziteNodeUtilTest {

    @Test
    void matchesConfiguredKunziteNodeInConfiguredWorld() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("end-hub.kunzite.enabled", true);
        config.set("end-hub.kunzite.world-name", "skyblock_end_mines");
        config.set("end-hub.kunzite.blocks", java.util.List.of("PINK_CONCRETE"));

        World world = mock(World.class);
        when(world.getName()).thenReturn("skyblock_end_mines");

        Block block = mock(Block.class);
        when(block.getWorld()).thenReturn(world);
        when(block.getType()).thenReturn(Material.PINK_CONCRETE);

        assertTrue(KunziteNodeUtil.isConfiguredKunziteNode(config, block));
    }

    @Test
    void rejectsKunziteBlockOutsideConfiguredWorld() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("end-hub.kunzite.enabled", true);
        config.set("end-hub.kunzite.world-name", "world_the_end");

        World world = mock(World.class);
        when(world.getName()).thenReturn("skyblock_end_mines");

        Block block = mock(Block.class);
        when(block.getWorld()).thenReturn(world);
        when(block.getType()).thenReturn(Material.PINK_STAINED_GLASS);

        assertFalse(KunziteNodeUtil.isConfiguredKunziteNode(config, block));
    }
}
