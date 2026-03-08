package io.papermc.Grivience.listener;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

/**
 * Prevents monsters from targeting other mobs to avoid mob-on-mob combat.
 */
public final class MobTargetLimiterListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Monster)) {
            return;
        }
        LivingEntity target = event.getTarget();
        if (target == null) {
            return;
        }
        if (target instanceof Player) {
            return;
        }
        event.setCancelled(true);
    }
}
