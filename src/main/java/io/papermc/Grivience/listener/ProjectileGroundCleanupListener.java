package io.papermc.Grivience.listener;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

/**
 * Cleans up arrows and tridents on ground impact so they don't stick out of blocks.
 */
public final class ProjectileGroundCleanupListener implements Listener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getHitBlock() == null) {
            return;
        }
        Projectile projectile = event.getEntity();
        if (projectile instanceof Arrow || projectile instanceof Trident) {
            projectile.remove();
        }
    }
}
