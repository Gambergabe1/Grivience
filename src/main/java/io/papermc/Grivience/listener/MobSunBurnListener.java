package io.papermc.Grivience.listener;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.persistence.PersistentDataContainer;

public class MobSunBurnListener implements Listener {
    private final NamespacedKey customMonsterKey;
    private final NamespacedKey slayerBossKey;
    private final NamespacedKey endMinesMobKey;
    private final NamespacedKey dungeonMobKey;
    private final NamespacedKey bossMobKey;
    private final NamespacedKey seaCreatureKey;
    private final NamespacedKey wardenMinionKey;

    public MobSunBurnListener(GriviencePlugin plugin) {
        this.customMonsterKey = new NamespacedKey(plugin, "custom_monster");
        this.slayerBossKey = new NamespacedKey(plugin, "slayer_boss");
        this.endMinesMobKey = new NamespacedKey(plugin, "end_mines_mob_type");
        this.dungeonMobKey = new NamespacedKey(plugin, "dungeon_mob");
        this.bossMobKey = new NamespacedKey(plugin, "boss_mob");
        this.seaCreatureKey = new NamespacedKey(plugin, "sea_creature_type");
        this.wardenMinionKey = new NamespacedKey(plugin, "crimson_warden_minion");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityCombust(EntityCombustEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity)) {
            return;
        }

        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        if (pdc.has(customMonsterKey) || pdc.has(slayerBossKey) || pdc.has(endMinesMobKey)
                || pdc.has(dungeonMobKey) || pdc.has(bossMobKey) || pdc.has(seaCreatureKey)
                || pdc.has(wardenMinionKey)) {
            // Check if it's sun damage
            // EntityCombustEvent itself doesn't directly tell us if it's from the sun,
            // but we can infer it if there's no duration increase (which often happens from block-based fire)
            // or we just cancel it for all custom mobs to be safe.
            // Hypixel mobs usually don't burn at all unless by fire.
            
            // Actually, there's no easy way in Bukkit's EntityCombustEvent to know the CAUSE.
            // However, most people just want them to not burn in daytime.
            // If they are on fire from a lava block, it might be different, but typically we just disable it.
            
            // Check if they are in the sun
            if (entity.getWorld().getTime() < 12000 && isInSun(entity)) {
                event.setCancelled(true);
            }
        }
    }

    private boolean isInSun(Entity entity) {
        var loc = entity.getLocation();
        return loc.getBlock().getLightFromSky() > 10;
    }
}
