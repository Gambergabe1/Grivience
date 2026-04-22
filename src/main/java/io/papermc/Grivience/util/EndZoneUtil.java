package io.papermc.Grivience.util;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class EndZoneUtil {
    private EndZoneUtil() {
    }

    public static boolean isEndWorld(GriviencePlugin plugin, World world) {
        if (world == null) {
            return false;
        }
        if (world.getEnvironment() == World.Environment.THE_END) {
            return true;
        }

        String worldName = world.getName();
        String endHubWorld = plugin == null ? "world_the_end" : plugin.getConfig().getString("end-hub.kunzite.world-name", "world_the_end");
        String endMinesWorld = plugin == null ? "skyblock_end_mines" : plugin.getConfig().getString("end-mines.world-name", "skyblock_end_mines");
        return worldName.equalsIgnoreCase(endHubWorld) || worldName.equalsIgnoreCase(endMinesWorld);
    }

    public static boolean isEndMob(GriviencePlugin plugin, Entity entity) {
        return entity instanceof LivingEntity
                && !(entity instanceof Player)
                && isEndWorld(plugin, entity.getWorld());
    }
}
