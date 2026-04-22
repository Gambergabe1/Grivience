package io.papermc.Grivience.mayor;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class MayorManager {
    private final GriviencePlugin plugin;
    private final File configFile;
    private FileConfiguration config;
    
    private final Map<String, Mayor> mayors = new HashMap<>();
    private String activeMayorName;
    private final Map<UUID, String> playerVotes = new HashMap<>();
    private Location spawnLocation;
    
    private long termEndTime;
    private long electionEndTime;
    private boolean electionActive;
    
    private MayorZnpcsHook znpcsHook;
    private org.bukkit.scheduler.BukkitTask timerTask;

    public MayorManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "mayor.yml");
    }

    public void setZnpcsHook(MayorZnpcsHook hook) {
        this.znpcsHook = hook;
        refreshNpc();
    }

    public void load() {
        if (!configFile.exists()) {
            try {
                configFile.getParentFile().mkdirs();
                configFile.createNewFile();
                // Load defaults
                initializeDefaults();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create mayor.yml", e);
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        activeMayorName = config.getString("active-mayor");
        termEndTime = config.getLong("term-end-time", 0);
        electionEndTime = config.getLong("election-end-time", 0);
        electionActive = config.getBoolean("election-active", false);
        
        if (config.contains("spawn-location")) {
            spawnLocation = config.getLocation("spawn-location");
        }

        mayors.clear();
        ConfigurationSection mayorSection = config.getConfigurationSection("mayors");
        if (mayorSection != null) {
            for (String key : mayorSection.getKeys(false)) {
                String name = mayorSection.getString(key + ".name");
                String buff = mayorSection.getString(key + ".buff");
                String skin = mayorSection.getString(key + ".skin", name);
                Mayor mayor = new Mayor(name, buff, skin);
                for (String action : mayorSection.getStringList(key + ".actions")) {
                    mayor.addAction(action);
                }
                mayors.put(name.toUpperCase(), mayor);
            }
        } else {
            initializeDefaults();
        }

        playerVotes.clear();
        if (config.contains("votes")) {
            ConfigurationSection votesSection = config.getConfigurationSection("votes");
            if (votesSection != null) {
                for (String uuidStr : votesSection.getKeys(false)) {
                    playerVotes.put(UUID.fromString(uuidStr), config.getString("votes." + uuidStr).toUpperCase());
                }
            }
        }
        
        startTimer();
        refreshNpc();
    }

    private void initializeDefaults() {
        for (MayorCandidate candidate : MayorCandidate.values()) {
            Mayor mayor = new Mayor(candidate.getDisplayName(), candidate.getBuffDescription(), candidate.getSkinName());
            mayors.put(mayor.getName().toUpperCase(), mayor);
        }
        if (activeMayorName == null && !electionActive) {
            startElection();
        }
        save();
    }

    public void save() {
        config.set("active-mayor", activeMayorName);
        config.set("spawn-location", spawnLocation);
        config.set("term-end-time", termEndTime);
        config.set("election-end-time", electionEndTime);
        config.set("election-active", electionActive);
        
        config.set("mayors", null);
        for (Mayor mayor : mayors.values()) {
            String path = "mayors." + mayor.getName().toUpperCase();
            config.set(path + ".name", mayor.getName());
            config.set(path + ".buff", mayor.getBuffDescription());
            config.set(path + ".skin", mayor.getSkinName());
            config.set(path + ".actions", mayor.getActions());
        }

        config.set("votes", null);
        for (Map.Entry<UUID, String> entry : playerVotes.entrySet()) {
            config.set("votes." + entry.getKey().toString(), entry.getValue());
        }
        
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save mayor.yml", e);
        }
    }

    private void startTimer() {
        if (timerTask != null) timerTask.cancel();
        timerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L * 60, 20L * 60); // Every minute
    }

    private void tick() {
        long now = System.currentTimeMillis();
        
        if (electionActive) {
            if (now >= electionEndTime) {
                electMayor();
            }
        } else if (activeMayorName != null) {
            if (now >= termEndTime) {
                endTerm();
            }
        } else {
            // No mayor and no election? Start one.
            startElection();
        }
    }

    public void startElection() {
        electionActive = true;
        // Election lasts 2 days (real time)
        electionEndTime = System.currentTimeMillis() + (2L * 24 * 60 * 60 * 1000);
        activeMayorName = null;
        playerVotes.clear();
        save();
        refreshNpc();
        plugin.getServer().broadcastMessage(org.bukkit.ChatColor.GOLD + "[Mayor] A new mayor election has started! Vote at the Mayor NPC.");
    }

    public void electMayor() {
        Map<String, Integer> counts = getVoteCounts();
        String winner = null;
        int maxVotes = -1;

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                winner = entry.getKey();
            }
        }

        if (winner == null && !mayors.isEmpty()) {
            winner = mayors.keySet().iterator().next();
        }

        activeMayorName = winner;
        electionActive = false;
        // Term lasts 5 days (real time)
        termEndTime = System.currentTimeMillis() + (5L * 24 * 60 * 60 * 1000);
        playerVotes.clear();
        save();
        refreshNpc();
        
        Mayor mayor = getActiveMayor();
        if (mayor != null) {
            plugin.getServer().broadcastMessage(org.bukkit.ChatColor.GOLD + "[Mayor] " + org.bukkit.ChatColor.YELLOW + mayor.getName() + org.bukkit.ChatColor.GOLD + " has been elected as the new mayor!");
        }
    }

    public void endTerm() {
        activeMayorName = null;
        termEndTime = 0;
        save();
        refreshNpc();
        plugin.getServer().broadcastMessage(org.bukkit.ChatColor.GOLD + "[Mayor] The current mayor's term has ended. A new election will begin shortly.");
        startElection();
    }

    public void refreshNpc() {
        if (znpcsHook == null || spawnLocation == null) return;
        
        if (activeMayorName != null) {
            summonActiveMayor();
        } else {
            // NPC only shows when they are elected
            znpcsHook.deleteMayor("mayor_npc");
        }
    }

    public void summonActiveMayor() {
        if (znpcsHook == null || spawnLocation == null || activeMayorName == null) return;
        Mayor mayor = getActiveMayor();
        if (mayor == null) return;

        znpcsHook.summonMayor("mayor_npc", spawnLocation, mayor.getName(), mayor.getSkinName());
    }

    public void setSpawnLocation(Location location) {
        this.spawnLocation = location;
        save();
        refreshNpc();
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public void castVote(UUID playerId, String mayorName) {
        if (!mayors.containsKey(mayorName.toUpperCase())) return;
        playerVotes.put(playerId, mayorName.toUpperCase());
        save();
    }

    public String getVote(UUID playerId) {
        return playerVotes.get(playerId);
    }

    public Map<String, Integer> getVoteCounts() {
        Map<String, Integer> counts = new HashMap<>();
        for (String mayorName : mayors.keySet()) {
            counts.put(mayorName, 0);
        }
        for (String vote : playerVotes.values()) {
            counts.put(vote, counts.getOrDefault(vote, 0) + 1);
        }
        return counts;
    }

    public Mayor getActiveMayor() {
        if (activeMayorName == null) return null;
        return mayors.get(activeMayorName.toUpperCase());
    }

    public void addMayor(Mayor mayor) {
        mayors.put(mayor.getName().toUpperCase(), mayor);
        save();
    }
    
    public Map<String, Mayor> getAllMayors() {
        return new HashMap<>(mayors);
    }
    
    public void performActions(Player player) {
        Mayor mayor = getActiveMayor();
        if (mayor == null) return;
        
        for (String action : mayor.getActions()) {
            String cmd = action.replace("{player}", player.getName());
            if (cmd.startsWith("/")) cmd = cmd.substring(1);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }

    public boolean isElectionActive() {
        return electionActive;
    }

    public long getElectionEndTime() {
        return electionEndTime;
    }

    public long getTermEndTime() {
        return termEndTime;
    }

    public void stop() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }
}
