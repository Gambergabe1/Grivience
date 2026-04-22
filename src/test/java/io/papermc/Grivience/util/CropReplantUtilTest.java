package io.papermc.Grivience.util;

import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class CropReplantUtilTest {
    @Test
    void matureAgeableCropsAreDetectedAndResetToSeedlings() {
        Ageable wheat = mock(Ageable.class);
        Ageable replanted = mock(Ageable.class);
        when(wheat.getAge()).thenReturn(7);
        when(wheat.getMaximumAge()).thenReturn(7);
        when(wheat.clone()).thenReturn((BlockData) replanted);

        assertTrue(CropReplantUtil.isMature(Material.WHEAT, wheat));

        BlockData result = CropReplantUtil.seedlingData(Material.WHEAT, wheat);
        assertSame(replanted, result);
        verify(replanted).setAge(0);
    }

    @Test
    void immatureCropsDoNotQualifyForReplenish() {
        Ageable carrots = mock(Ageable.class);
        when(carrots.getAge()).thenReturn(0);
        when(carrots.getMaximumAge()).thenReturn(7);

        assertFalse(CropReplantUtil.isMature(Material.CARROTS, carrots));
    }

    @Test
    void replantCostUsesExpectedSeedOrCropItems() {
        assertEquals(Material.WHEAT_SEEDS, CropReplantUtil.replantCost(Material.WHEAT));
        assertEquals(Material.CARROT, CropReplantUtil.replantCost(Material.CARROTS));
        assertEquals(Material.BEETROOT_SEEDS, CropReplantUtil.replantCost(Material.BEETROOTS));
        assertEquals(Material.NETHER_WART, CropReplantUtil.replantCost(Material.NETHER_WART));
    }

    @Test
    void oneSeedIsRemovedFromHarvestDrops() {
        org.bukkit.inventory.ItemStack wheat = mock(org.bukkit.inventory.ItemStack.class);
        when(wheat.getType()).thenReturn(Material.WHEAT);
        when(wheat.getAmount()).thenReturn(3);

        org.bukkit.inventory.ItemStack seeds = mock(org.bukkit.inventory.ItemStack.class);
        when(seeds.getType()).thenReturn(Material.WHEAT_SEEDS);
        when(seeds.getAmount()).thenReturn(2);

        List<org.bukkit.inventory.ItemStack> drops = new ArrayList<>();
        drops.add(wheat);
        drops.add(seeds);

        assertTrue(CropReplantUtil.removeOneFromStacks(drops, Material.WHEAT_SEEDS));
        verify(seeds).setAmount(1);
    }
}
