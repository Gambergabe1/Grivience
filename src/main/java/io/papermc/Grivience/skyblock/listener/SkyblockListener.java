package io.papermc.Grivience.skyblock.listener;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skyblock.island.Island;
import io.papermc.Grivience.skyblock.island.IslandManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.Bukkit;

import java.util.UUID;

public final class SkyblockListener implements Listener {
    private final GriviencePlugin plugin;
    private final IslandManager islandManager;

    public SkyblockListener(GriviencePlugin plugin, IslandManager islandManager) {
        this.plugin = plugin;
        this.islandManager = islandManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        World islandWorld = islandManager.getIslandWorld();

        if (islandWorld == null) {
            islandManager.initializeWorld();
            islandWorld = islandManager.getIslandWorld();
        }

        // Ensure per-profile inventory isolation.
        var profileManager = plugin.getProfileManager();
        if (profileManager != null) {
            var selectedProfile = profileManager.getSelectedProfile(player);
            if (selectedProfile == null && profileManager.getPlayerProfiles(player).isEmpty()) {
                profileManager.createProfile(player, "Default");
                selectedProfile = profileManager.getSelectedProfile(player);
            }
            if (selectedProfile != null) {
                islandManager.loadProfileInventory(player, selectedProfile.getProfileId(), selectedProfile.getProfileName());
            }
        }

        if (!islandManager.hasIsland(player)) {
            // If they have a profile but no island, create it automatically.
            if (profileManager != null) {
                var selectedProfile = profileManager.getSelectedProfile(player);
                if (selectedProfile != null) {
                    player.sendMessage(ChatColor.YELLOW + "Generating your Skyblock island...");
                    islandManager.createIsland(player);
                    return;
                }
            }

            player.sendMessage("");
            player.sendMessage(ChatColor.GOLD + "=== Welcome ===");
            player.sendMessage(ChatColor.GRAY + "You don't have an island yet.");
            player.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.GREEN + "/island create" + ChatColor.YELLOW +
                    " to create your island!");
            player.sendMessage("");
        } else {
            Island island = islandManager.getIsland(player);
            Location spawn = islandManager.getSafeSpawnLocation(island);
            if (spawn != null && islandWorld != null) {
                // Teleport shortly after join to ensure chunks are loaded.
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        player.teleport(spawn);
                        player.setBedSpawnLocation(spawn, true);
                    }
                }, 2L);
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Island island = islandManager.getIsland(player);

        if (island != null && island.getCenter() != null) {
            Location spawnLocation = islandManager.getSafeSpawnLocation(island);
            if (spawnLocation != null && islandManager.getIslandWorld() != null &&
                    spawnLocation.getWorld().equals(islandManager.getIslandWorld())) {
                event.setRespawnLocation(spawnLocation);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.COMMAND) {
            return;
        }

        Player player = event.getPlayer();
        Location to = event.getTo();

        if (to == null || to.getWorld() == null) {
            return;
        }

        World islandWorld = islandManager.getIslandWorld();
        if (islandWorld == null || !to.getWorld().equals(islandWorld)) {
            return;
        }

        Island island = islandManager.getIsland(player);
        if (island == null) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You need to create an island first. Use /island create");
            return;
        }

        if (!island.isWithinIsland(to)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You can only teleport to your own island area.");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        islandManager.saveProfileInventory(player);
    }
}
