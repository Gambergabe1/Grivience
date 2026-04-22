package io.papermc.Grivience.util;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Objects;

public final class EndMinesSpawnSafetyUtil {
    private EndMinesSpawnSafetyUtil() {
    }

    public static boolean isEligibleActivityPlayer(Player player, Location mineSpawn, double activationSafeRadiusSq) {
        if (player == null || player.isDead()) {
            return false;
        }

        GameMode gameMode = player.getGameMode();
        if (gameMode == GameMode.CREATIVE || gameMode == GameMode.SPECTATOR) {
            return false;
        }
        if (player.hasPermission("grivience.admin") || player.hasPermission("grivience.endmines.build")) {
            return false;
        }
        return isOutsideActivationSafeZone(player.getLocation(), mineSpawn, activationSafeRadiusSq);
    }

    public static boolean isOutsideActivationSafeZone(Location playerLocation, Location mineSpawn, double activationSafeRadiusSq) {
        if (playerLocation == null) {
            return false;
        }
        if (activationSafeRadiusSq <= 0.0D || mineSpawn == null) {
            return true;
        }
        if (playerLocation.getWorld() == null || mineSpawn.getWorld() == null) {
            return true;
        }
        if (!Objects.equals(playerLocation.getWorld(), mineSpawn.getWorld())) {
            return true;
        }
        return playerLocation.distanceSquared(mineSpawn) > activationSafeRadiusSq;
    }
}
