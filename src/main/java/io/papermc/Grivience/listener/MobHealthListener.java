package io.papermc.Grivience.listener;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.util.MobHealthDisplay;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Keeps monster nameplates updated with live health values.
 */
public final class MobHealthListener implements Listener {
    private final GriviencePlugin plugin;
    private final NamespacedKey baseNameKey;

    public MobHealthListener(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.baseNameKey = new NamespacedKey(plugin, "health_base_name");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        if (!ensureTracked(living)) {
            return;
        }
        
        // Immediate visual update for responsiveness
        double finalDamage = event.getFinalDamage();
        double predictedHealth = Math.max(0.0D, living.getHealth() - finalDamage);
        MobHealthDisplay.update(living, baseNameKey, predictedHealth);

        // Consistency check next tick
        new BukkitRunnable() {
            @Override
            public void run() {
                if (living.isValid()) {
                    MobHealthDisplay.update(living, baseNameKey);
                }
            }
        }.runTask(plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobHeal(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        if (!ensureTracked(living)) {
            return;
        }
        
        double amount = event.getAmount();
        double predictedHealth = Math.min(living.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue(), living.getHealth() + amount);
        MobHealthDisplay.update(living, baseNameKey, predictedHealth);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (living.isValid()) {
                    MobHealthDisplay.update(living, baseNameKey);
                }
            }
        }.runTask(plugin);
    }

    private boolean hasHealthName(LivingEntity living) {
        return living.getPersistentDataContainer().has(baseNameKey);
    }

    private boolean ensureTracked(LivingEntity living) {
        if (living == null) return false;
        if (living instanceof org.bukkit.entity.Player) return false;
        if (living instanceof org.bukkit.entity.ArmorStand) return false;
        
        // Skip NPCs (common metadata for ZNPCs, Citizens, etc.)
        if (living.hasMetadata("NPC") || living.getPersistentDataContainer().has(new NamespacedKey("znpcs", "npc"), org.bukkit.persistence.PersistentDataType.INTEGER)) {
            return false;
        }

        if (living.getPersistentDataContainer().has(baseNameKey)) {
            return true;
        }

        String fallback = living.getCustomName() != null
                ? living.getCustomName()
                : living.getName().replace('_', ' ');
        MobHealthDisplay.setBaseName(living, baseNameKey, fallback);
        return true;
    }
}
