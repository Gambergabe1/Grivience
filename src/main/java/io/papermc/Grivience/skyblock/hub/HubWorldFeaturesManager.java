package io.papermc.Grivience.skyblock.hub;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

/**
 * Applies hub-world settings that should survive reloads and world loads.
 */
public final class HubWorldFeaturesManager implements Listener {
    private final GriviencePlugin plugin;

    public HubWorldFeaturesManager(GriviencePlugin plugin) {
        this.plugin = plugin;
    }

    public void applyNow() {
        String worldName = plugin.getConfig().getString("skyblock.hub-world", "world");
        if (worldName == null || worldName.isBlank()) {
            return;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return;
        }

        boolean allowMobs = plugin.getConfig().getBoolean("skyblock.hub-features.enable-mob-spawning", true);
        world.setSpawnFlags(allowMobs, allowMobs);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, allowMobs);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        if (event == null || event.getWorld() == null) {
            return;
        }

        String worldName = plugin.getConfig().getString("skyblock.hub-world", "world");
        if (worldName == null || worldName.isBlank()) {
            return;
        }
        if (!event.getWorld().getName().equalsIgnoreCase(worldName)) {
            return;
        }

        applyNow();
    }
}
