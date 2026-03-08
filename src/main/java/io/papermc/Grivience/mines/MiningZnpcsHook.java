package io.papermc.Grivience.mines;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Random;

/**
 * Handles Normal ZNPCs integration for Mining Events using reflection.
 * Specifically for spawning and despawning the King NPC.
 */
public final class MiningZnpcsHook {
    private static final String NPC_NAME = "&6&lKing Ironcrest";
    private static final int NPC_ID = 9999; // Fixed ID for the King NPC in ZNPCs

    private final GriviencePlugin plugin;
    private final String displayName;
    private final Location spawnLocation;
    private final String skinName;
    
    private Object activeNpc;
    private boolean available;

    public MiningZnpcsHook(GriviencePlugin plugin, String displayName, Location spawnLocation, String skinName) {
        this.plugin = plugin;
        this.displayName = ChatColor.translateAlternateColorCodes('&', displayName);
        this.spawnLocation = spawnLocation;
        this.skinName = skinName;
        checkAvailability();
    }

    private void checkAvailability() {
        Plugin znpcs = Bukkit.getPluginManager().getPlugin("ZNPCs");
        this.available = znpcs != null && znpcs.isEnabled();
        if (available) {
            plugin.getLogger().info("MiningZnpcsHook: Detected Normal ZNPCs.");
        }
    }

    public void spawnNPC() {
        if (!available || activeNpc != null) return;
        if (spawnLocation == null || spawnLocation.getWorld() == null) {
            plugin.getLogger().warning("MiningZnpcsHook: Cannot spawn NPC - invalid location.");
            return;
        }

        try {
            // ZNPCs uses NPCManager.createNPC(NPCType, Location, String, int)
            Class<?> npcManagerClass = Class.forName("me.pikamug.npcs.NPCManager");
            Method getManager = npcManagerClass.getMethod("getManager");
            Object npcManager = getManager.invoke(null);

            // Create NPC type (PLAYER type)
            Class<?> npcTypeClass = Class.forName("me.pikamug.npcs.NPCType");
            java.lang.reflect.Field playerField = npcTypeClass.getField("PLAYER");
            Object playerType = playerField.get(null);

            // Create NPC
            Method createNPC = npcManagerClass.getMethod("createNPC", 
                playerType.getClass(), Location.class, String.class, int.class);
            
            activeNpc = createNPC.invoke(npcManager, playerType, spawnLocation, displayName, NPC_ID);

            if (activeNpc != null) {
                Class<?> npcClass = activeNpc.getClass();
                
                // Set NPC as clickable
                Method setClickable = npcClass.getMethod("setClickable", boolean.class);
                setClickable.invoke(activeNpc, true);

                // Spawn NPC
                Method spawn = npcClass.getMethod("spawn");
                spawn.invoke(activeNpc);

                plugin.getLogger().info("King Ironcrest NPC created using Normal ZNPCs!");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("MiningZnpcsHook: Failed to spawn King NPC with ZNPCs: " + e.getMessage());
        }
    }

    public void despawnNPC() {
        if (!available || activeNpc == null) return;

        try {
            Class<?> npcClass = activeNpc.getClass();
            Method removeMethod = npcClass.getMethod("remove");
            removeMethod.invoke(activeNpc);
            activeNpc = null;
            plugin.getLogger().info("King Ironcrest NPC removed.");
        } catch (Exception e) {
            plugin.getLogger().warning("MiningZnpcsHook: Failed to despawn King NPC: " + e.getMessage());
        }
    }

    public Object getActiveNpc() {
        return activeNpc;
    }

    public void handleInteraction(Player player) {
        MiningEventManager manager = plugin.getMiningEventManager();
        if (manager.getActiveEvent() != MiningEventManager.MiningEvent.KINGS_INSPECTION) {
            player.sendMessage(ChatColor.YELLOW + "King Ironcrest: " + ChatColor.WHITE + "“The mines are quiet today.”");
            return;
        }

        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "KING IRONCREST");
        player.sendMessage(ChatColor.WHITE + "“The inspection is ongoing. Ensure maximum efficiency.”");
        player.sendMessage(ChatColor.GRAY + "Status: " + ChatColor.YELLOW + manager.getOresMinedDuringEvent() + "/" + manager.getExtensionGoal() + " ores mined.");
        
        long remaining = manager.getEventRemainingMillis() / 1000;
        player.sendMessage(ChatColor.GRAY + "Time Remaining: " + ChatColor.GREEN + (remaining / 60) + "m " + (remaining % 60) + "s");
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "Opening Inspector Shop...");
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getInspectorShopGui().open(player);
            }
        }, 10L);
    }
}
