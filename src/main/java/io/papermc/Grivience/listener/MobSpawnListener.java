package io.papermc.Grivience.listener;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.util.MobHealthDisplay;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;

/**
 * Handles mob spawning restrictions and modifications.
 */
public final class MobSpawnListener implements Listener {
    private final GriviencePlugin plugin;
    private final NamespacedKey monsterLevelKey;
    private final NamespacedKey baseNameKey;

    public MobSpawnListener(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.monsterLevelKey = new NamespacedKey(plugin, "monster_level");
        this.baseNameKey = new NamespacedKey(plugin, "health_base_name");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBatSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType() != EntityType.BAT) {
            return;
        }

        boolean disableBats = plugin.getConfig().getBoolean("mob-settings.disable-natural-bat-spawns", true);
        if (!disableBats) {
            return;
        }

        // Only block natural spawns (natural, cave, etc.)
        // Keep spawns from plugins, spawn eggs, or commands if needed.
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        if (reason == CreatureSpawnEvent.SpawnReason.NATURAL || 
            reason == CreatureSpawnEvent.SpawnReason.DEFAULT ||
            reason == CreatureSpawnEvent.SpawnReason.MOUNT) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobSpawn(CreatureSpawnEvent event) {
        LivingEntity living = event.getEntity();
        if (living instanceof org.bukkit.entity.Player) return;
        if (living instanceof org.bukkit.entity.ArmorStand) return;
        
        // Use a slight delay to allow other plugins (like custom monster managers) 
        // to set their own metadata/levels first.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!living.isValid()) return;

            // Skip NPCs
            if (living.hasMetadata("NPC") || living.getPersistentDataContainer().has(new NamespacedKey("znpcs", "npc"), PersistentDataType.INTEGER)) {
                return;
            }

            // If it doesn't have a level yet, assign a default vanilla level
            if (!living.getPersistentDataContainer().has(monsterLevelKey, PersistentDataType.INTEGER)) {
                int level = 1;
                // Basic vanilla level scaling based on difficulty or world
                if (living.getWorld().getDifficulty() == org.bukkit.Difficulty.NORMAL) level = 5;
                else if (living.getWorld().getDifficulty() == org.bukkit.Difficulty.HARD) level = 10;
                
                living.getPersistentDataContainer().set(monsterLevelKey, PersistentDataType.INTEGER, level);
            }

            // Ensure it's tracked by the health display system
            if (!living.getPersistentDataContainer().has(baseNameKey, PersistentDataType.STRING)) {
                String name = living.getCustomName() != null 
                        ? living.getCustomName() 
                        : living.getName().replace('_', ' ');
                MobHealthDisplay.setBaseName(living, baseNameKey, name);
            } else {
                // Already has a base name (likely a custom mob), just force an update to show the nameplate
                MobHealthDisplay.update(living, baseNameKey);
            }
        });
    }
}
