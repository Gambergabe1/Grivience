package io.papermc.Grivience.announcement;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Manages BossBar announcements for server-wide messages.
 */
public final class BossBarAnnouncementManager {
    private final JavaPlugin plugin;
    private final Map<UUID, BossBar> bossBarsByPlayer = new HashMap<>();
    private BukkitTask cleanupTask;
    
    private BarColor defaultColor;
    private BarStyle defaultStyle;
    private int displayTicks;
    private boolean progressAnimation;

    public BossBarAnnouncementManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.defaultColor = BarColor.PURPLE;
        this.defaultStyle = BarStyle.SOLID;
        this.displayTicks = 100; // 5 seconds default
        this.progressAnimation = true;
    }

    public void reload() {
        var config = plugin.getConfig();
        String path = "bossbar-announcements.";
        String colorName = config.getString(path + "default-color", "PURPLE");
        String styleName = config.getString(path + "default-style", "SOLID");
        displayTicks = Math.max(20, config.getInt(path + "display-ticks", 100));
        progressAnimation = config.getBoolean(path + "progress-animation", true);
        
        try {
            defaultColor = BarColor.valueOf(colorName.toUpperCase());
        } catch (IllegalArgumentException e) {
            defaultColor = BarColor.PURPLE;
        }
        
        try {
            defaultStyle = BarStyle.valueOf(styleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            defaultStyle = BarStyle.SOLID;
        }
    }

    public void start() {
        reload();
        startCleanupTask();
    }

    public void shutdown() {
        stopCleanupTask();
        removeAllBossBars();
    }

    /**
     * Send a BossBar announcement to all online players.
     */
    public void announce(String message) {
        announce(message, defaultColor, defaultStyle);
    }

    /**
     * Send a BossBar announcement to all online players with custom color.
     */
    public void announce(String message, BarColor color) {
        announce(message, color, defaultStyle);
    }

    /**
     * Send a BossBar announcement to all online players with custom color and style.
     */
    public void announce(String message, BarColor color, BarStyle style) {
        if (message == null || message.isBlank()) {
            return;
        }

        String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);
        
        // Remove existing boss bars from all players
        removeAllBossBars();

        // Create and show boss bar to all players
        BossBar bossBar = Bukkit.createBossBar(coloredMessage, color, style, BarFlag.CREATE_FOG, BarFlag.DARKEN_SKY);
        bossBar.setProgress(1.0);
        
        if (progressAnimation) {
            bossBar.addFlag(BarFlag.PLAY_BOSS_MUSIC);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(player);
            bossBarsByPlayer.put(player.getUniqueId(), bossBar);
        }

        // Schedule removal
        if (displayTicks > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                bossBar.removeAll();
                bossBarsByPlayer.clear();
            }, displayTicks);
        }
    }

    /**
     * Send a BossBar announcement to a specific player.
     */
    public void announceTo(Player player, String message) {
        announceTo(player, message, defaultColor, defaultStyle);
    }

    /**
     * Send a BossBar announcement to a specific player with custom color and style.
     */
    public void announceTo(Player player, String message, BarColor color, BarStyle style) {
        if (player == null || message == null || message.isBlank()) {
            return;
        }

        String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);
        
        // Remove existing boss bar from this player
        removeBossBar(player);

        // Create and show boss bar
        BossBar bossBar = Bukkit.createBossBar(coloredMessage, color, style, BarFlag.CREATE_FOG, BarFlag.DARKEN_SKY);
        bossBar.setProgress(1.0);
        bossBar.addPlayer(player);
        bossBarsByPlayer.put(player.getUniqueId(), bossBar);

        // Schedule removal
        if (displayTicks > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> removeBossBar(player), displayTicks);
        }
    }

    /**
     * Remove a boss bar from a specific player.
     */
    public void removeBossBar(Player player) {
        if (player == null) {
            return;
        }
        BossBar bossBar = bossBarsByPlayer.remove(player.getUniqueId());
        if (bossBar != null) {
            bossBar.removePlayer(player);
        }
    }

    /**
     * Remove all boss bars from all players.
     */
    public void removeAllBossBars() {
        for (Map.Entry<UUID, BossBar> entry : bossBarsByPlayer.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                entry.getValue().removePlayer(player);
            }
        }
        bossBarsByPlayer.clear();
    }

    /**
     * Check if a player currently has a boss bar displayed.
     */
    public boolean hasBossBar(Player player) {
        return player != null && bossBarsByPlayer.containsKey(player.getUniqueId());
    }

    private void startCleanupTask() {
        stopCleanupTask();
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupStaleBossBars, 100L, 100L);
    }

    private void stopCleanupTask() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
    }

    /**
     * Clean up boss bars for players who have disconnected.
     */
    private void cleanupStaleBossBars() {
        Set<UUID> toRemove = new HashSet<>();
        for (UUID playerId : bossBarsByPlayer.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                toRemove.add(playerId);
            }
        }
        for (UUID playerId : toRemove) {
            bossBarsByPlayer.remove(playerId);
        }
    }

    /**
     * Get the default boss bar color.
     */
    public BarColor getDefaultColor() {
        return defaultColor;
    }

    /**
     * Set the default boss bar color.
     */
    public void setDefaultColor(BarColor color) {
        this.defaultColor = color;
    }

    /**
     * Get the default boss bar style.
     */
    public BarStyle getDefaultStyle() {
        return defaultStyle;
    }

    /**
     * Set the default boss bar style.
     */
    public void setDefaultStyle(BarStyle style) {
        this.defaultStyle = style;
    }

    /**
     * Get the display duration in ticks.
     */
    public int getDisplayTicks() {
        return displayTicks;
    }

    /**
     * Set the display duration in ticks.
     */
    public void setDisplayTicks(int ticks) {
        this.displayTicks = Math.max(20, ticks);
    }

    /**
     * Check if progress animation is enabled.
     */
    public boolean isProgressAnimation() {
        return progressAnimation;
    }

    /**
     * Set whether progress animation is enabled.
     */
    public void setProgressAnimation(boolean enabled) {
        this.progressAnimation = enabled;
    }
}
