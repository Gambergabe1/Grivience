package io.papermc.Grivience.mines;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public final class MiningEventManager {
    private static GriviencePlugin staticPlugin;
    private final GriviencePlugin plugin;
    private MiningEvent activeEvent = null;
    private long eventEndTime = 0;
    private BukkitTask eventTask = null;

    // Event specific progress
    private int oresMinedDuringEvent = 0;
    private int currentOresGoal = 25000;
    private long eventStartTime = 0;
    private int stabilityMeter = 0;
    private static final int STABILITY_GOAL = 500;
    private static final int INITIAL_EXTENSION_GOAL = 25000;
    private static final int MAX_TOTAL_GOAL = 150000;
    private static final long MAX_DURATION_MILLIS = 60 * 60 * 1000L; // 1 hour
    private boolean firstGoalReached = false;

    private String targetLayer = "Unknown";
    private final Map<UUID, Integer> extractionLeaderboard = new HashMap<>();
    
    private MiningZnpcsHook kingsInspectionHook;
    private boolean kingsInspectionNpcSpawned = false;

    public enum MiningEvent {
        KINGS_INSPECTION("King's Inspection", 15),
        DEEP_CORE_BREACH("Deep Core Breach", 10),
        GRAND_EXTRACTION("Grand Extraction", 10);

        private final String displayName;
        private final int defaultDurationMinutes;

        MiningEvent(String displayName, int defaultDurationMinutes) {
            this.displayName = displayName;
            this.defaultDurationMinutes = defaultDurationMinutes;
        }

        public String displayName() {
            return displayName;
        }

        public int defaultDurationMinutes() {
            return defaultDurationMinutes;
        }
    }

    public MiningEventManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        staticPlugin = plugin;
        initializeNpcHook();
    }

    private void initializeNpcHook() {
        var config = plugin.getConfig();
        String path = "mining-events.kings-inspection.npc.";
        String name = config.getString(path + "name", "&6&lKing Ironcrest");
        String skin = config.getString(path + "skin", "King");
        
        String worldName = config.getString(path + "location.world", "minehub_world");
        double x = config.getDouble(path + "location.x", 10.5);
        double y = config.getDouble(path + "location.y", 100.0);
        double z = config.getDouble(path + "location.z", 10.5);
        float yaw = (float) config.getDouble(path + "location.yaw", 180.0);
        float pitch = (float) config.getDouble(path + "location.pitch", 0.0);
        
        var world = Bukkit.getWorld(worldName);
        if (world != null) {
            Location loc = new Location(world, x, y, z, yaw, pitch);
            this.kingsInspectionHook = new MiningZnpcsHook(plugin, name, loc, skin);
        } else {
            plugin.getLogger().warning("MiningEventManager: Could not find world " + worldName + " for King NPC.");
        }
    }

    public static GriviencePlugin getPlugin() {
        return staticPlugin;
    }

    public void startEvent(MiningEvent event, String layer) {
        stopActiveEvent();

        this.activeEvent = event;
        long durationMillis = event.defaultDurationMinutes() * 60 * 1000L;
        this.eventStartTime = System.currentTimeMillis();
        this.eventEndTime = this.eventStartTime + durationMillis;
        this.oresMinedDuringEvent = 0;
        this.currentOresGoal = INITIAL_EXTENSION_GOAL;
        this.stabilityMeter = 0;
        this.firstGoalReached = false;
        this.targetLayer = layer != null ? layer : "Deep Core";
        this.extractionLeaderboard.clear();

        broadcastStart(event);

        eventTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (System.currentTimeMillis() >= eventEndTime) {
                handleEventEnd();
                stopActiveEvent();
            } else {
                handleEventTick();
            }
        }, 20L, 20L);
    }

    public void stopActiveEvent() {
        if (eventTask != null) {
            eventTask.cancel();
            eventTask = null;
        }
        activeEvent = null;
    }

    private void broadcastStart(MiningEvent event) {
        switch (event) {
            case KINGS_INSPECTION -> {
                Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "EVENT: " + ChatColor.YELLOW + "King's Inspection");
                Bukkit.broadcastMessage(ChatColor.WHITE + "“The King of Ironcrest is inspecting the mines. Work efficiently.”");
                Bukkit.broadcastMessage(ChatColor.GRAY + "Effects: " + ChatColor.GREEN + "+25% Mining XP, +10% Rare Drop Chance");
                spawnKingNPC();
            }
            case DEEP_CORE_BREACH -> {
                Bukkit.broadcastMessage(ChatColor.RED + "" + ChatColor.BOLD + "WARNING: " + ChatColor.DARK_RED + "Deep Core Breach in " + targetLayer);
                Bukkit.broadcastMessage(ChatColor.WHITE + "“Structural instability detected in the lower mines.”");
                Bukkit.broadcastMessage(ChatColor.GRAY + "Effects: " + ChatColor.RED + "3x Reinforced Ores, Stronger Mobs");
            }
            case GRAND_EXTRACTION -> {
                Bukkit.broadcastMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "EVENT: " + ChatColor.BLUE + "Grand Extraction in " + targetLayer);
                Bukkit.broadcastMessage(ChatColor.WHITE + "“Ironcrest has authorized a Grand Extraction.”");
                Bukkit.broadcastMessage(ChatColor.GRAY + "Goal: Mine as many ores as possible for extra rewards!");
            }
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 1.0f, 1.0f);
        }
    }

    private void handleEventTick() {
        if (activeEvent == MiningEvent.DEEP_CORE_BREACH) {
            // Placeholder for falling gravel zones or other periodic effects
        }
    }

    private void handleEventEnd() {
        if (activeEvent == null) return;

        Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "EVENT ENDED: " + ChatColor.YELLOW + activeEvent.displayName());

        if (activeEvent == MiningEvent.KINGS_INSPECTION) {
            despawnKingNPC();
        } else if (activeEvent == MiningEvent.DEEP_CORE_BREACH) {
            if (stabilityMeter >= STABILITY_GOAL) {
                Bukkit.broadcastMessage(ChatColor.GREEN + "Stability Meter FILLED! Global Mining XP boost for 20 minutes!");
                // Implementation for global boost would go here
            } else {
                Bukkit.broadcastMessage(ChatColor.RED + "Stability Meter FAILED! Reduced ore spawn rate in " + targetLayer + " for 15 minutes.");
            }
        } else if (activeEvent == MiningEvent.GRAND_EXTRACTION) {
            displayExtractionLeaderboard();
        }
    }

    private void displayExtractionLeaderboard() {
        List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>(extractionLeaderboard.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        Bukkit.broadcastMessage(ChatColor.AQUA + "--- Grand Extraction Top Miners ---");
        for (int i = 0; i < Math.min(3, sorted.size()); i++) {
            Map.Entry<UUID, Integer> entry = sorted.get(i);
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            Bukkit.broadcastMessage(ChatColor.YELLOW + String.valueOf(i + 1) + ". " + name + ": " + entry.getValue() + " ores");
        }
    }

    public void registerMine(Player player) {
        if (activeEvent == null) return;

        oresMinedDuringEvent++;
        if (activeEvent == MiningEvent.KINGS_INSPECTION && oresMinedDuringEvent >= currentOresGoal) {
            // Check if we can still extend (max duration 1 hour, max goal 150k)
            boolean canExtend = (eventEndTime - eventStartTime < MAX_DURATION_MILLIS) && (currentOresGoal < MAX_TOTAL_GOAL);

            if (canExtend) {
                // Extend by 10 minutes, but cap at 1 hour from start
                long extensionMillis = 10 * 60 * 1000L;
                long newEndTime = eventEndTime + extensionMillis;
                if (newEndTime - eventStartTime > MAX_DURATION_MILLIS) {
                    newEndTime = eventStartTime + MAX_DURATION_MILLIS;
                }
                
                if (newEndTime > eventEndTime) {
                    eventEndTime = newEndTime;
                    currentOresGoal = Math.min(MAX_TOTAL_GOAL, currentOresGoal + 25000);
                    
                    Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "GOAL REACHED! " + ChatColor.YELLOW + "King's Inspection goal increased and event extended!");
                    if (!firstGoalReached) {
                        firstGoalReached = true;
                        Bukkit.broadcastMessage(ChatColor.AQUA + "Bonus: +5% Mining XP for the remainder of the event!");
                    }
                    
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    }
                }
            }
        }

        if (activeEvent == MiningEvent.GRAND_EXTRACTION) {
            extractionLeaderboard.merge(player.getUniqueId(), 1, Integer::sum);
        }
    }

    public void addStability(int amount) {
        if (activeEvent == MiningEvent.DEEP_CORE_BREACH) {
            stabilityMeter += amount;
            if (stabilityMeter % 50 == 0) {
                Bukkit.broadcastMessage(ChatColor.RED + "Stability Meter: " + stabilityMeter + "/" + STABILITY_GOAL);
            }
        }
    }

    public MiningEvent getActiveEvent() {
        return activeEvent;
    }

    public String getTargetLayer() {
        return targetLayer;
    }

    public double getXpMultiplier() {
        if (activeEvent == MiningEvent.KINGS_INSPECTION) return firstGoalReached ? 1.30 : 1.25;
        return 1.0;
    }

    public int getOresMinedDuringEvent() {
        return oresMinedDuringEvent;
    }

    public int getExtensionGoal() {
        return currentOresGoal;
    }

    public long getEventRemainingMillis() {
        if (activeEvent == null) return 0;
        return Math.max(0, eventEndTime - System.currentTimeMillis());
    }

    private void spawnKingNPC() {
        if (kingsInspectionHook != null) {
            kingsInspectionHook.spawnNPC();
            kingsInspectionNpcSpawned = true;
        } else {
            plugin.getLogger().info("Spawning King NPC at Surface of Mines (Hook not initialized).");
        }
    }

    private void despawnKingNPC() {
        if (kingsInspectionHook != null && kingsInspectionNpcSpawned) {
            kingsInspectionHook.despawnNPC();
            kingsInspectionNpcSpawned = false;
        } else {
            plugin.getLogger().info("Despawning King NPC.");
        }
    }

    public void updateNpcLocation(Location loc) {
        var config = plugin.getConfig();
        String path = "mining-events.kings-inspection.npc.location.";
        config.set(path + "world", loc.getWorld().getName());
        config.set(path + "x", loc.getX());
        config.set(path + "y", loc.getY());
        config.set(path + "z", loc.getZ());
        config.set(path + "yaw", (double) loc.getYaw());
        config.set(path + "pitch", (double) loc.getPitch());
        plugin.saveConfig();
        
        initializeNpcHook();
    }

    public void updateNpcName(String name) {
        var config = plugin.getConfig();
        String path = "mining-events.kings-inspection.npc.name";
        config.set(path, name);
        plugin.saveConfig();
    }

    public void cleanup() {
        if (kingsInspectionNpcSpawned) {
            despawnKingNPC();
        }
    }

    public double getRareDropMultiplier() {
        if (activeEvent == MiningEvent.KINGS_INSPECTION) return 1.10;
        return 1.0;
    }

    public MiningZnpcsHook getKingsInspectionHook() {
        return kingsInspectionHook;
    }

    public boolean isBreachActive(String layer) {
        return activeEvent == MiningEvent.DEEP_CORE_BREACH && (layer == null || targetLayer.equalsIgnoreCase(layer));
    }

    public boolean isExtractionActive(String layer) {
        return activeEvent == MiningEvent.GRAND_EXTRACTION && (layer == null || targetLayer.equalsIgnoreCase(layer));
    }
}
