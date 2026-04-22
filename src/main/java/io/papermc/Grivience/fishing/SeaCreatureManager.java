package io.papermc.Grivience.fishing;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class SeaCreatureManager implements Listener {

    private final GriviencePlugin plugin;
    private final NamespacedKey seaCreatureKey;

    public SeaCreatureManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.seaCreatureKey = new NamespacedKey(plugin, "sea_creature_type");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void spawnSeaCreature(Location location, Player target) {
        SeaCreatureType[] types = SeaCreatureType.values();
        SeaCreatureType type = types[ThreadLocalRandom.current().nextInt(types.length)];

        LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, type.getEntityType());
        
        entity.setCustomName(type.getDisplayName() + " §c" + (int)type.getMaxHealth() + "❤");
        entity.setCustomNameVisible(true);
        if (entity.getAttribute(Attribute.MAX_HEALTH) != null) {
            entity.getAttribute(Attribute.MAX_HEALTH).setBaseValue(type.getMaxHealth());
            entity.setHealth(type.getMaxHealth());
        }

        entity.getPersistentDataContainer().set(seaCreatureKey, PersistentDataType.STRING, type.name());

        if (entity instanceof Mob mob) {
            mob.setTarget(target);
        }
        
        // Push slightly upwards and towards the player so it jumps out of water
        Vector direction = target.getLocation().toVector().subtract(location.toVector()).normalize();
        direction.multiply(0.5).setY(0.6);
        entity.setVelocity(direction);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.getPersistentDataContainer().has(seaCreatureKey, PersistentDataType.STRING)) {
            String typeName = entity.getPersistentDataContainer().get(seaCreatureKey, PersistentDataType.STRING);
            try {
                SeaCreatureType type = SeaCreatureType.valueOf(typeName);
                event.getDrops().clear();
                event.getDrops().addAll(type.getDrops());
            } catch (IllegalArgumentException e) {
                // Ignore
            }
        }
    }
}
