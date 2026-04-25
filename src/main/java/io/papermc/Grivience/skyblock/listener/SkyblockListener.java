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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean firstJoin = !player.hasPlayedBefore();
        long guideDelayTicks = resolveFirstJoinGuideDelayTicks();
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
            // Auto-generate if they have a profile but no island.
            if (profileManager != null) {
                var selectedProfile = profileManager.getSelectedProfile(player);
                if (selectedProfile != null) {
                    player.sendMessage(ChatColor.YELLOW + "Generating your Skyblock island...");
                    islandManager.createIsland(player);
                    scheduleFirstJoinGuide(player, guideDelayTicks);
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
            boolean forceIslandSpawn = plugin.getConfig().getBoolean("skyblock.force-spawn-on-island-on-join", false);

            if (firstJoin || forceIslandSpawn) {
                // Force to island if first join OR config enabled.
                Location spawn = islandManager.getSafeSpawnLocation(island);
                if (spawn != null) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            player.teleport(spawn);
                            player.setBedSpawnLocation(spawn, true);
                        }
                    }, 2L);
                }
            }
            // Returning players (firstJoin=false and forceIslandSpawn=false) 
            // will naturally spawn at their logout location by vanilla logic.
        }

        if (firstJoin) {
            scheduleFirstJoinGuide(player, guideDelayTicks);
        }
    }

    private Location getHubSpawn() {
        String hubWorldName = plugin.getConfig().getString("skyblock.hub-world", "world");
        World world = Bukkit.getWorld(hubWorldName);
        if (world == null) return null;
        
        String path = "skyblock.hub-spawn";
        if (plugin.getConfig().contains(path)) {
            double x = plugin.getConfig().getDouble(path + ".x");
            double y = plugin.getConfig().getDouble(path + ".y");
            double z = plugin.getConfig().getDouble(path + ".z");
            float yaw = (float) plugin.getConfig().getDouble(path + ".yaw", 0);
            float pitch = (float) plugin.getConfig().getDouble(path + ".pitch", 0);
            return new Location(world, x, y, z, yaw, pitch);
        }
        return world.getSpawnLocation().add(0.5, 0, 0.5);
    }

    private Location getMinehubSpawn() {
        World world = Bukkit.getWorld("Minehub");
        if (world == null) return null;
        String path = "skyblock.minehub-spawn";
        if (plugin.getConfig().contains(path)) {
            double x = plugin.getConfig().getDouble(path + ".x");
            double y = plugin.getConfig().getDouble(path + ".y");
            double z = plugin.getConfig().getDouble(path + ".z");
            return new Location(world, x, y, z);
        }
        return world.getSpawnLocation().add(0.5, 0, 0.5);
    }

    private Location getEndMinesSpawn() {
        World world = Bukkit.getWorld("skyblock_end_mines");
        if (world == null) return null;
        // End mines uses a safe platform at 0, 70, 0 usually or world spawn
        return world.getSpawnLocation().add(0.5, 0, 0.5);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Island island = islandManager.getIsland(player);

        if (island != null && island.getCenter() != null) {
            Location spawnLocation = islandManager.getSafeSpawnLocation(island);
            String islandWorldName = islandManager.getIslandWorldName();
            if (spawnLocation != null && spawnLocation.getWorld().getName().equalsIgnoreCase(islandWorldName)) {
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

        String islandWorldName = islandManager.getIslandWorldName();
        if (!to.getWorld().getName().equalsIgnoreCase(islandWorldName)) {
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

    private void scheduleFirstJoinGuide(Player player, long delayTicks) {
        if (player == null) {
            return;
        }
        if (!plugin.getConfig().getBoolean("skyblock-guide.first-join-auto-open", true)) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }

            var profileManager = plugin.getProfileManager();
            if (profileManager != null && profileManager.getSelectedProfile(player) == null) {
                return;
            }

            if (plugin.getSkyblockLevelGui() == null) {
                return;
            }

            if (plugin.getConfig().getBoolean("skyblock-guide.first-join-chat-tip", true)) {
                player.sendMessage("");
                player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "SkyBlock Guide");
                player.sendMessage(ChatColor.GRAY + "Welcome to your new island! Here are some tips to get started:");
                player.sendMessage(ChatColor.YELLOW + " \u27a4 " + ChatColor.WHITE + "Use " + ChatColor.GREEN + "/npcsell" + ChatColor.WHITE + " to sell items for coins.");
                player.sendMessage(ChatColor.YELLOW + " \u27a4 " + ChatColor.WHITE + "Use " + ChatColor.GREEN + "/spawn" + ChatColor.WHITE + " to visit the Hub world.");
                player.sendMessage(ChatColor.YELLOW + " \u27a4 " + ChatColor.WHITE + "Use " + ChatColor.GREEN + "/farmhub" + ChatColor.WHITE + " for early-game leveling.");
                player.sendMessage("");
                player.sendMessage(ChatColor.GRAY + "Tasks and milestones are listed in the guide menu.");
            }

            plugin.getSkyblockLevelGui().openMenu(player, io.papermc.Grivience.gui.SkyblockLevelGui.LevelTab.GUIDE, null);
        }, Math.max(1L, delayTicks));
    }

    private long resolveFirstJoinGuideDelayTicks() {
        return Math.max(1L, plugin.getConfig().getLong("skyblock-guide.first-join-open-delay-ticks", 80L));
    }
}
