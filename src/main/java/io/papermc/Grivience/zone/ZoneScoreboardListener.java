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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Listener that updates the scoreboard when players enter/exit zones.
 * Integrates with SkyblockScoreboardManager to display current zone.
 */
public final class ZoneScoreboardListener implements Listener {
    private final GriviencePlugin plugin;
    private final ZoneManager zoneManager;

    // Track player's current zone to detect changes.
    private final Map<UUID, String> playerCurrentZone = new HashMap<>();

    // Cooldown to prevent rapid updates.
    private final Map<UUID, Long> updateCooldown = new HashMap<>();
    private final long updateCooldownMs = 500L;

    private BukkitTask checkTask;
    private boolean enabled;
    private int checkIntervalTicks;
    private int checkCursor;
    private long checkAccumulator;

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
        checkIntervalTicks = Math.max(1, plugin.getConfig().getInt("zones.scoreboard-update-interval", 20));
        playerCurrentZone.clear();
        updateCooldown.clear();
        zoneManager.clearPlayerCaches();

        if (enabled) {
            startPeriodicCheck();
        }
    }

    public void stop() {
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }
        checkCursor = 0;
        checkAccumulator = 0L;
    }

    private void startPeriodicCheck() {
        checkTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickPeriodicCheck, 20L, 1L);
    }

    private void tickPeriodicCheck() {
        if (!enabled) {
            return;
        }

        Player[] online = plugin.getServer().getOnlinePlayers().toArray(Player[]::new);
        if (online.length == 0) {
            checkCursor = 0;
            checkAccumulator = 0L;
            return;
        }

        long effectiveIntervalTicks = effectiveCheckIntervalTicks();
        checkAccumulator += online.length;
        int playersThisTick = (int) Math.min(online.length, checkAccumulator / effectiveIntervalTicks);
        if (playersThisTick <= 0) {
            return;
        }
        checkAccumulator %= effectiveIntervalTicks;

        for (int i = 0; i < playersThisTick; i++) {
            checkPlayerZone(online[(checkCursor + i) % online.length]);
        }
        checkCursor = (checkCursor + playersThisTick) % online.length;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled || !zoneManager.isShowOnJoin()) {
            return;
        }

        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                checkPlayerZone(player);
            }
        }, 20L);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (!enabled) {
            return;
        }

        Player player = event.getPlayer();
        clearCachedZone(player);
        zoneManager.clearPlayerCache(player);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                checkPlayerZone(player);
            }
        }, 10L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (!enabled) {
            return;
        }

        Player player = event.getPlayer();
        zoneManager.clearPlayerCache(player);
        checkPlayerZone(player);
        updateCooldown.put(player.getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!enabled || !zoneManager.isUpdateOnChange()) {
            return;
        }
        if (event.getTo() == null) {
            return;
        }

        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()
                && Objects.equals(event.getFrom().getWorld(), event.getTo().getWorld())) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastUpdate = updateCooldown.get(playerId);

        if (lastUpdate != null && now - lastUpdate < updateCooldownMs) {
            return;
        }

        checkPlayerZone(player);
        updateCooldown.put(playerId, now);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        playerCurrentZone.remove(player.getUniqueId());
        updateCooldown.remove(player.getUniqueId());
        zoneManager.clearPlayerCache(player);
    }

    /**
     * Check if player's zone has changed and update scoreboard.
     */
    private void checkPlayerZone(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        UUID playerId = player.getUniqueId();
        Zone currentZone = zoneManager.getZoneAt(player);
        String currentZoneId = currentZone == null ? null : currentZone.getId();
        String previousZoneId = playerCurrentZone.get(playerId);

        if (Objects.equals(currentZoneId, previousZoneId)) {
            return;
        }

        if (currentZoneId == null) {
            playerCurrentZone.remove(playerId);
        } else {
            playerCurrentZone.put(playerId, currentZoneId);
        }

        String zoneName = currentZone == null
                ? zoneManager.getDefaultZoneName()
                : zoneManager.getZoneDisplayName(currentZoneId);

        if (zoneManager.isShowOnJoin() || previousZoneId != null) {
            String previousName = previousZoneId != null
                    ? zoneManager.getZoneDisplayName(previousZoneId)
                    : zoneManager.getDefaultZoneName();

            if (currentZone != null) {
                player.sendMessage(ChatColor.GRAY + "Entering: " + zoneName);
            } else {
                player.sendMessage(ChatColor.GRAY + "Leaving: " + previousName);
            }
        }

        refreshScoreboard(player);
    }

    /**
     * Refresh the player's scoreboard to show current zone.
     * This integrates with SkyblockScoreboardManager.
     */
    private void refreshScoreboard(Player player) {
        if (plugin.getSkyblockScoreboardManager() != null) {
            plugin.getSkyblockScoreboardManager().updateForPlayer(player);
        }
    }

    /**
     * Get the current zone for a player.
     */
    public Zone getCurrentZone(Player player) {
        String zoneId = playerCurrentZone.get(player.getUniqueId());
        if (zoneId == null) {
            return null;
        }
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
        if (player == null) {
            return;
        }
        playerCurrentZone.remove(player.getUniqueId());
        updateCooldown.remove(player.getUniqueId());
    }

    public boolean isEnabled() {
        return enabled;
    }

    private long effectiveCheckIntervalTicks() {
        if (plugin.getServerPerformanceMonitor() == null) {
            return checkIntervalTicks;
        }
        return plugin.getServerPerformanceMonitor().scalePeriod(checkIntervalTicks, 2, 4);
    }
}
