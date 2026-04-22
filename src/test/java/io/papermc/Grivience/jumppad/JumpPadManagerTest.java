package io.papermc.Grivience.jumppad;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

public final class JumpPadManagerTest {

    @Test
    void launchCenterPreservesLaunchOrientationForAreaPads() {
        World world = mock(World.class);
        JumpPadManager.JumpPad pad = new JumpPadManager.JumpPad();
        pad.setLaunch(new Location(world, 2.0D, 70.0D, 4.0D, 135.0F, 12.0F));
        pad.setLaunchCorner(new Location(world, 6.0D, 74.0D, 8.0D, 0.0F, 0.0F));

        Location center = pad.getLaunchCenter();

        assertSame(world, center.getWorld());
        assertEquals(4.0D, center.getX(), 1e-9);
        assertEquals(72.0D, center.getY(), 1e-9);
        assertEquals(6.0D, center.getZ(), 1e-9);
        assertEquals(135.0F, center.getYaw(), 1e-9F);
        assertEquals(12.0F, center.getPitch(), 1e-9F);
    }

    @Test
    void targetCenterPreservesTargetWorldAndOrientationForAreaPads() {
        World world = mock(World.class);
        JumpPadManager.JumpPad pad = new JumpPadManager.JumpPad();
        pad.setTarget(new Location(world, 10.0D, 80.0D, 20.0D, 90.0F, 7.0F));
        pad.setTargetCorner(new Location(world, 18.0D, 84.0D, 28.0D, 0.0F, 0.0F));

        Location center = pad.getTargetCenter();

        assertSame(world, center.getWorld());
        assertEquals(14.0D, center.getX(), 1e-9);
        assertEquals(82.0D, center.getY(), 1e-9);
        assertEquals(24.0D, center.getZ(), 1e-9);
        assertEquals(90.0F, center.getYaw(), 1e-9F);
        assertEquals(7.0F, center.getPitch(), 1e-9F);
    }

    @Test
    void isWithinLaunchAreaIdentifiesSingleBlockPads() {
        World world = mock(World.class);
        JumpPadManager.JumpPad pad = new JumpPadManager.JumpPad();
        pad.setLaunch(new Location(world, 10.0D, 64.0D, 10.0D));

        org.junit.jupiter.api.Assertions.assertTrue(pad.isWithinLaunchArea(new Location(world, 10.1D, 64.0D, 10.9D)));
        org.junit.jupiter.api.Assertions.assertFalse(pad.isWithinLaunchArea(new Location(world, 11.0D, 64.0D, 10.0D)));
    }

    @Test
    void isWithinLaunchAreaIdentifiesCuboidPads() {
        World world = mock(World.class);
        JumpPadManager.JumpPad pad = new JumpPadManager.JumpPad();
        pad.setLaunch(new Location(world, 10.0D, 64.0D, 10.0D));
        pad.setLaunchCorner(new Location(world, 12.0D, 66.0D, 12.0D));

        org.junit.jupiter.api.Assertions.assertTrue(pad.isWithinLaunchArea(new Location(world, 11.0D, 65.0D, 11.0D)));
        org.junit.jupiter.api.Assertions.assertTrue(pad.isWithinLaunchArea(new Location(world, 10.0D, 64.0D, 10.0D)));
        org.junit.jupiter.api.Assertions.assertTrue(pad.isWithinLaunchArea(new Location(world, 12.0D, 66.0D, 12.0D)));
        org.junit.jupiter.api.Assertions.assertFalse(pad.isWithinLaunchArea(new Location(world, 13.0D, 65.0D, 11.0D)));
    }
}
