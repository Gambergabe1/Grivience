package io.papermc.Grivience.event;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.mines.MiningEventManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Central manager for global server events.
 */
public class GlobalEventManager {
    private final GriviencePlugin plugin;
    private final MiningEventManager miningEventManager;
    
    private double globalXpMultiplier = 1.0;
    private long xpBoostEndTime = 0;
    private BukkitTask xpBoostTask = null;

    public GlobalEventManager(GriviencePlugin plugin, MiningEventManager miningEventManager) {
        this.plugin = plugin;
        this.miningEventManager = miningEventManager;
    }

    /**
     * Start a global XP boost.
     * @param multiplier The multiplier (e.g. 1.5 for +50%)
     * @param durationMinutes Duration in minutes
     */
    public void startGlobalXpBoost(double multiplier, int durationMinutes) {
        stopGlobalXpBoost();
        
        this.globalXpMultiplier = multiplier;
        this.xpBoostEndTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L);
        
        String percent = String.format("%.0f", (multiplier - 1.0) * 100.0);
        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "GLOBAL EVENT: " + ChatColor.YELLOW + "Global XP Boost!");
        Bukkit.broadcastMessage(ChatColor.WHITE + "A global " + ChatColor.GREEN + "+" + percent + "% XP boost" + ChatColor.WHITE + " has started for " + ChatColor.YELLOW + durationMinutes + " minutes!");
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }

        xpBoostTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "EVENT ENDED: " + ChatColor.YELLOW + "Global XP Boost has finished.");
            stopGlobalXpBoost();
        }, durationMinutes * 60 * 20L);
    }

    public void stopGlobalXpBoost() {
        if (xpBoostTask != null) {
            xpBoostTask.cancel();
            xpBoostTask = null;
        }
        globalXpMultiplier = 1.0;
        xpBoostEndTime = 0;
    }

    public double getGlobalXpMultiplier() {
        return System.currentTimeMillis() < xpBoostEndTime ? globalXpMultiplier : 1.0;
    }

    public long getXpBoostRemainingMillis() {
        return Math.max(0, xpBoostEndTime - System.currentTimeMillis());
    }

    /**
     * Get all currently active global events.
     */
    public List<String> getActiveEvents() {
        List<String> active = new ArrayList<>();
        
        if (getGlobalXpMultiplier() > 1.0) {
            String percent = String.format("%.0f", (globalXpMultiplier - 1.0) * 100.0);
            active.add(ChatColor.LIGHT_PURPLE + "Global XP Boost (+" + percent + "%)");
        }
        
        if (miningEventManager.getActiveEvent() != null) {
            active.add(ChatColor.GOLD + "Mining Event: " + miningEventManager.getActiveEvent().displayName());
        }
        
        return active;
    }
}
