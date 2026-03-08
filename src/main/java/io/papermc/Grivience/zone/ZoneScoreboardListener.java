package io.papermc.Grivience.zone;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listener that updates the scoreboard when players enter/exit zones.
 * Integrates with SkyblockScoreboardManager to display current zone.
 */
public final class ZoneScoreboardListener implements Listener {
    private final GriviencePlugin plugin;
    private final ZoneManager zoneManager;
    
    // Track player's current zone to detect changes
    private final Map<UUID, String> playerCurrentZone = new HashMap<>();
    
    // Cooldown to prevent rapid updates
    private final Map<UUID, Long> updateCooldown = new HashMap<>();
    private final long updateCooldownMs = 500; // 500ms between zone updates
    
    private BukkitTask checkTask;
    private boolean enabled;
    private int checkIntervalTicks;

    public ZoneScoreboardListener(GriviencePlugin plugin, ZoneManager zoneManager) {
        this.plugin = plugin;
        this.zoneManager = zoneManager;
    }

    public void start() {
        reload();
    }

    public void reload() {
        stop();
        
        enabled = zoneManager.isEnabled();
        checkIntervalTicks = plugin.getConfig().getInt("zones.scoreboard-update-interval", 20);
        
        if (enabled) {
            startPeriodicCheck();
        }
    }

    public void stop() {
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }
    }

    private void startPeriodicCheck() {
        checkTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkAllPlayers, 20L, checkIntervalTicks);
    }

    private void checkAllPlayers() {
        if (!enabled) return;
        
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            checkPlayerZone(player);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled || !zoneManager.isShowOnJoin()) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Schedule check after player fully joins
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                checkPlayerZone(player);
            }
        }, 20L);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (!enabled) return;
        
        Player player = event.getPlayer();
        
        // Clear cached zone for player
        playerCurrentZone.remove(player.getUniqueId());
        
        // Check new world zones after short delay
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                checkPlayerZone(player);
            }
        }, 10L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!enabled || !zoneManager.isUpdateOnChange()) return;
        
        // Only check if player moved to a different block
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Check cooldown
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastUpdate = updateCooldown.get(playerId);
        
        if (lastUpdate != null && now - lastUpdate < updateCooldownMs) {
            return;
        }
        
        checkPlayerZone(player);
        updateCooldown.put(playerId, now);
    }

    /**
     * Check if player's zone has changed and update scoreboard.
     */
    private void checkPlayerZone(Player player) {
        if (!player.isOnline()) return;
        
        UUID playerId = player.getUniqueId();
        Zone currentZone = zoneManager.getZoneAt(player.getLocation());
        String currentZoneId = currentZone != null ? currentZone.getId() : null;
        String previousZoneId = playerCurrentZone.get(playerId);
        
        // Check if zone changed
        if (currentZoneId == null && previousZoneId == null) {
            return; // Still no zone
        }
        
        if (currentZoneId != null && currentZoneId.equals(previousZoneId)) {
            return; // Same zone
        }
        
        // Zone changed - update scoreboard
        playerCurrentZone.put(playerId, currentZoneId);
        
        String zoneName = zoneManager.getZoneName(player);
        
        // Notify player of zone change
        if (zoneManager.isShowOnJoin() || previousZoneId != null) {
            String previousName = previousZoneId != null 
                ? zoneManager.getZone(previousZoneId).getColoredDisplayName()
                : zoneManager.getDefaultZoneName();
            
            if (currentZone != null) {
                player.sendMessage(ChatColor.GRAY + "Entering: " + zoneName);
            } else {
                player.sendMessage(ChatColor.GRAY + "Leaving: " + previousName);
            }
        }
        
        // Trigger scoreboard refresh if using SkyblockScoreboardManager
        refreshScoreboard(player);
    }

    /**
     * Refresh the player's scoreboard to show current zone.
     * This integrates with SkyblockScoreboardManager.
     */
    private void refreshScoreboard(Player player) {
        // The scoreboard manager will automatically pick up the zone name
        // when it next renders. We can force an immediate update by
        // scheduling a task to re-render.
        
        // Note: This assumes SkyblockScoreboardManager is managing the scoreboard
        // The zone name will be fetched via ZoneManager.getZoneName() when rendering
    }

    /**
     * Get the current zone for a player.
     */
    public Zone getCurrentZone(Player player) {
        String zoneId = playerCurrentZone.get(player.getUniqueId());
        if (zoneId == null) return null;
        return zoneManager.getZone(zoneId);
    }

    /**
     * Get the cached zone ID for a player.
     */
    public String getCachedZoneId(Player player) {
        return playerCurrentZone.get(player.getUniqueId());
    }

    /**
     * Clear cached zone for a player.
     */
    public void clearCachedZone(Player player) {
        playerCurrentZone.remove(player.getUniqueId());
    }

    public boolean isEnabled() {
        return enabled;
    }
}
