package io.papermc.Grivience.pet;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Applies and clears pet effects on player lifecycle events.
 */
public final class PetExpListener implements Listener {
    private final GriviencePlugin plugin;
    private final PetManager petManager;

    public PetExpListener(GriviencePlugin plugin, PetManager petManager) {
        this.plugin = plugin;
        this.petManager = petManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Apply equipped pet bonuses after a short delay to ensure the player is fully initialized.
        var player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                petManager.cleanupLegacyPotionEffects(player);
                petManager.applyCurrent(player);
            }
        }, 10L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        petManager.handleQuit(event.getPlayer());
    }
}
