package io.papermc.Grivience.util;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class EndMinesSpawnSafetyUtilTest {

    @Test
    void rejectsCreativeOrAdminPlayersAsSpawnActivity() {
        World world = mock(World.class);
        Location mineSpawn = new Location(world, 0.0D, 80.0D, 0.0D);

        Player creative = mock(Player.class);
        when(creative.isDead()).thenReturn(false);
        when(creative.getGameMode()).thenReturn(GameMode.CREATIVE);
        when(creative.hasPermission("grivience.admin")).thenReturn(false);
        when(creative.hasPermission("grivience.endmines.build")).thenReturn(false);
        when(creative.getLocation()).thenReturn(new Location(world, 60.0D, 80.0D, 0.0D));
        assertFalse(EndMinesSpawnSafetyUtil.isEligibleActivityPlayer(creative, mineSpawn, 1600.0D));

        Player admin = mock(Player.class);
        when(admin.isDead()).thenReturn(false);
        when(admin.getGameMode()).thenReturn(GameMode.SURVIVAL);
        when(admin.hasPermission("grivience.admin")).thenReturn(true);
        when(admin.hasPermission("grivience.endmines.build")).thenReturn(false);
        when(admin.getLocation()).thenReturn(new Location(world, 60.0D, 80.0D, 0.0D));
        assertFalse(EndMinesSpawnSafetyUtil.isEligibleActivityPlayer(admin, mineSpawn, 1600.0D));
    }

    @Test
    void rejectsPlayersStandingInsideEndMinesSpawnSafeZone() {
        World world = mock(World.class);
        Location mineSpawn = new Location(world, 0.0D, 80.0D, 0.0D);

        Player player = mock(Player.class);
        when(player.isDead()).thenReturn(false);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        when(player.hasPermission("grivience.admin")).thenReturn(false);
        when(player.hasPermission("grivience.endmines.build")).thenReturn(false);
        when(player.getLocation()).thenReturn(new Location(world, 10.0D, 80.0D, 0.0D));

        assertFalse(EndMinesSpawnSafetyUtil.isEligibleActivityPlayer(player, mineSpawn, 400.0D));
    }

    @Test
    void allowsSurvivalPlayersOutsideTheSpawnSafeZone() {
        World world = mock(World.class);
        Location mineSpawn = new Location(world, 0.0D, 80.0D, 0.0D);

        Player player = mock(Player.class);
        when(player.isDead()).thenReturn(false);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        when(player.hasPermission("grivience.admin")).thenReturn(false);
        when(player.hasPermission("grivience.endmines.build")).thenReturn(false);
        when(player.getLocation()).thenReturn(new Location(world, 50.0D, 80.0D, 0.0D));

        assertTrue(EndMinesSpawnSafetyUtil.isEligibleActivityPlayer(player, mineSpawn, 1600.0D));
    }
}
