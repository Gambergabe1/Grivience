package io.papermc.Grivience.event;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.mines.MiningEventManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Central manager for global server events.
 */
public class GlobalEventManager {
    private final GriviencePlugin plugin;
    private final MiningEventManager miningEventManager;
    
    private final Map<BoosterType, ActiveBooster> activeBoosters = new EnumMap<>(BoosterType.class);

    public enum BoosterType {
        EXPERIENCE("Experience", ChatColor.LIGHT_PURPLE, Sound.UI_TOAST_CHALLENGE_COMPLETE),
        MINION_SPEED("Minion Speed", ChatColor.GOLD, Sound.ENTITY_VILLAGER_WORK_TOOLSMITH),
        DAMAGE("Damage", ChatColor.RED, Sound.ENTITY_PLAYER_ATTACK_CRIT),
        MINEHUB_HEART("Minehub Heart Power", ChatColor.AQUA, Sound.BLOCK_BEACON_ACTIVATE),
        ENDMINES_HEART("Endmines Heart Power", ChatColor.DARK_PURPLE, Sound.BLOCK_END_PORTAL_SPAWN);

        private final String displayName;
        private final ChatColor color;
        private final Sound sound;

        BoosterType(String displayName, ChatColor color, Sound sound) {
            this.displayName = displayName;
            this.color = color;
            this.sound = sound;
        }

        public String getDisplayName() { return displayName; }
        public ChatColor getColor() { return color; }
        public Sound getSound() { return sound; }
    }

    private static class ActiveBooster {
        private final double multiplier;
        private final long endTime;
        private final BukkitTask task;

        public ActiveBooster(double multiplier, long endTime, BukkitTask task) {
            this.multiplier = multiplier;
            this.endTime = endTime;
            this.task = task;
        }

        public double getMultiplier() { return multiplier; }
        public long getEndTime() { return endTime; }
        public BukkitTask getTask() { return task; }
    }

    public GlobalEventManager(GriviencePlugin plugin, MiningEventManager miningEventManager) {
        this.plugin = plugin;
        this.miningEventManager = miningEventManager;
    }

    /**
     * Start a global booster.
     * @param type The type of booster
     * @param multiplier The multiplier (e.g. 1.5 for +50%)
     * @param durationMinutes Duration in minutes
     */
    public void startBooster(BoosterType type, double multiplier, int durationMinutes) {
        stopBooster(type);
        
        long endTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L);
        
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "BOOSTER ENDED: " + type.getColor() + "Global " + type.getDisplayName() + " Boost has finished.");
            activeBoosters.remove(type);
        }, durationMinutes * 60 * 20L);

        activeBoosters.put(type, new ActiveBooster(multiplier, endTime, task));
        
        String percent = String.format("%.0f", (multiplier - 1.0) * 100.0);
        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "GLOBAL BOOSTER: " + type.getColor() + "Global " + type.getDisplayName() + " Boost!");
        Bukkit.broadcastMessage(ChatColor.WHITE + "A global " + ChatColor.GREEN + "+" + percent + "% " + type.getDisplayName() + " boost" + ChatColor.WHITE + " has started for " + ChatColor.YELLOW + durationMinutes + " minutes!");
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), type.getSound(), 1.0f, 1.0f);
        }
    }

    public void stopBooster(BoosterType type) {
        ActiveBooster active = activeBoosters.remove(type);
        if (active != null && active.getTask() != null) {
            active.getTask().cancel();
        }
    }

    public double getMultiplier(BoosterType type) {
        ActiveBooster active = activeBoosters.get(type);
        if (active == null) return 1.0;
        if (System.currentTimeMillis() > active.getEndTime()) {
            activeBoosters.remove(type);
            return 1.0;
        }
        return active.getMultiplier();
    }

    public long getRemainingMillis(BoosterType type) {
        ActiveBooster active = activeBoosters.get(type);
        if (active == null) return 0;
        return Math.max(0, active.getEndTime() - System.currentTimeMillis());
    }

    // Legacy method support for XP
    public double getGlobalXpMultiplier() {
        return getMultiplier(BoosterType.EXPERIENCE);
    }

    public void startGlobalXpBoost(double multiplier, int duration) {
        startBooster(BoosterType.EXPERIENCE, multiplier, duration);
    }

    public void stopGlobalXpBoost() {
        stopBooster(BoosterType.EXPERIENCE);
    }

    public long getXpBoostRemainingMillis() {
        return getRemainingMillis(BoosterType.EXPERIENCE);
    }

    /**
     * Get all currently active global events and boosters.
     */
    public List<String> getActiveEvents() {
        List<String> active = new ArrayList<>();
        
        for (BoosterType type : BoosterType.values()) {
            double mult = getMultiplier(type);
            if (mult > 1.0) {
                String percent = String.format("%.0f", (mult - 1.0) * 100.0);
                active.add(type.getColor() + "Global " + type.getDisplayName() + " Boost (+" + percent + "%)");
            }
        }
        
        if (miningEventManager.getActiveEvent() != null) {
            active.add(ChatColor.GOLD + "Mining Event: " + miningEventManager.getActiveEvent().displayName());
        }
        
        return active;
    }
}
