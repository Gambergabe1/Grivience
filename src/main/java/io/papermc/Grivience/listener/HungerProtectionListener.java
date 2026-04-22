package io.papermc.Grivience.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class HungerProtectionListener implements Listener {
    private static final int FULL_FOOD_LEVEL = 20;
    private static final float FULL_SATURATION = 20.0F;

    private final JavaPlugin plugin;

    public HungerProtectionListener(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTask(plugin, this::normalizeOnlinePlayers);
        Bukkit.getScheduler().runTaskTimer(plugin, this::normalizeOnlinePlayers, 20L, 200L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        event.setCancelled(true);
        normalizePlayer(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStarvationDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getCause() != EntityDamageEvent.DamageCause.STARVATION) {
            return;
        }
        event.setCancelled(true);
        normalizePlayer(player);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        normalizePlayer(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        normalizePlayer(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> normalizePlayer(event.getPlayer()), 1L);
    }

    private void normalizeOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            normalizePlayer(player);
        }
    }

    private void normalizePlayer(Player player) {
        if (player == null) {
            return;
        }
        player.setFoodLevel(FULL_FOOD_LEVEL);
        player.setSaturation(FULL_SATURATION);
        player.setExhaustion(0.0F);
    }
}
