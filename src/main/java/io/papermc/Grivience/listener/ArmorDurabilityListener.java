package io.papermc.Grivience.listener;

import io.papermc.Grivience.util.ArmorDurabilityUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class ArmorDurabilityListener implements Listener {
    private final JavaPlugin plugin;

    public ArmorDurabilityListener(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, this::normalizeOnlineArmor, 20L, 100L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        ItemStack item = event.getItem();
        if (!ArmorDurabilityUtil.shouldPreventDurabilityLoss(item)) {
            return;
        }

        event.setCancelled(true);
        ArmorDurabilityUtil.ensureArmorUnbreakable(item);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        normalizeItems(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> normalizeItems(event.getPlayer()), 1L);
    }

    private void normalizeOnlineArmor() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            normalizeItems(player);
        }
    }

    private void normalizeItems(Player player) {
        if (player == null) {
            return;
        }
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                ArmorDurabilityUtil.ensureArmorUnbreakable(item);
            }
        }
    }
}
