package io.papermc.Grivience.mob;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public final class CustomMonsterDeathListener implements Listener {
    private final GriviencePlugin plugin;
    private final CustomMonsterManager monsterManager;

    public CustomMonsterDeathListener(GriviencePlugin plugin, CustomMonsterManager monsterManager) {
        this.plugin = plugin;
        this.monsterManager = monsterManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        if (killer == null) {
            return;
        }

        // Check if this is a custom monster
        String monsterId = entity.getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, "custom_monster"),
                org.bukkit.persistence.PersistentDataType.STRING
        );

        if (monsterId != null) {
            // Prevent default drops for custom monsters
            event.getDrops().clear();
            event.setDroppedExp(0);

            // Handle custom drops
            monsterManager.handleMonsterDeath(entity, killer);
        }
    }
}
