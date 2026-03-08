package io.papermc.Grivience.fasttravel;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Listener for auto-unlocking fast travel destinations when players visit them.
 */
public final class FastTravelListener implements Listener {
    private final FastTravelManager manager;
    private final GriviencePlugin plugin;
    private final Map<UUID, Set<String>> unlockedThisSession = new HashMap<>();
    private final Map<UUID, Long> lastUnlockCheck = new HashMap<>();
    private static final long UNLOCK_CHECK_INTERVAL = 5000; // 5 seconds

    public FastTravelListener(GriviencePlugin plugin, FastTravelManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        checkAllDestinations(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> checkDestinations(event.getPlayer()), 10L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        
        if (to == null) return;
        
        // Only check if blocks moved significantly
        Location from = event.getFrom();
        if (to.getBlockX() == from.getBlockX() && 
            to.getBlockY() == from.getBlockY() && 
            to.getBlockZ() == from.getBlockZ()) {
            return;
        }

        long now = System.currentTimeMillis();
        UUID playerId = player.getUniqueId();
        
        Long lastCheck = lastUnlockCheck.get(playerId);
        if (lastCheck != null && now - lastCheck < UNLOCK_CHECK_INTERVAL) {
            return;
        }
        
        lastUnlockCheck.put(playerId, now);
        checkDestinations(player);
    }

    private void checkDestinations(Player player) {
        for (FastTravelManager.FastTravelPoint point : manager.getAllPoints()) {
            if (!manager.isUnlocked(player, point.key())) {
                // Check if player is near the destination (within 10 blocks)
                Location playerLoc = player.getLocation();
                Location destLoc = point.location();
                
                if (playerLoc.getWorld() != null && 
                    playerLoc.getWorld().equals(destLoc.getWorld()) &&
                    playerLoc.distance(destLoc) <= 15.0) {
                    unlockDestination(player, point);
                }
            }
        }
    }

    private void checkAllDestinations(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (FastTravelManager.FastTravelPoint point : manager.getAllPoints()) {
                if (!manager.isUnlocked(player, point.key())) {
                    Location playerLoc = player.getLocation();
                    Location destLoc = point.location();
                    
                    if (playerLoc.getWorld() != null && 
                        playerLoc.getWorld().equals(destLoc.getWorld()) &&
                        playerLoc.distance(destLoc) <= 15.0) {
                        unlockDestination(player, point);
                    }
                }
            }
        }, 40L);
    }

    private void unlockDestination(Player player, FastTravelManager.FastTravelPoint point) {
        UUID playerId = player.getUniqueId();
        Set<String> sessionUnlocks = unlockedThisSession.computeIfAbsent(playerId, k -> new HashSet<>());
        
        if (!sessionUnlocks.contains(point.key())) {
            manager.unlock(player, point.key());
            sessionUnlocks.add(point.key());
            
            player.sendMessage("");
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "🔓 Fast Travel Unlocked!");
            player.sendMessage(ChatColor.GRAY + "You can now fast travel to:");
            player.sendMessage(ChatColor.AQUA + point.name());
            player.sendMessage(ChatColor.GRAY + "Open your fast travel menu with " + ChatColor.YELLOW + "/fasttravel");
            player.sendMessage("");
            
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.2F);
        }
    }
}
