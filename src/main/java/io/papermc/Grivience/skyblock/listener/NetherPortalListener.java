package io.papermc.Grivience.skyblock.listener;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skyblock.command.HubCommand;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class NetherPortalListener implements Listener {
    private final GriviencePlugin plugin;
    private final HubCommand hubCommand;
    private final Set<UUID> onCooldown = new HashSet<>();

    public NetherPortalListener(GriviencePlugin plugin, HubCommand hubCommand) {
        this.plugin = plugin;
        this.hubCommand = hubCommand;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        World world = player.getWorld();

        if (!plugin.getConfig().getBoolean("skyblock.nether-portal-to-hub.enabled", true)) {
            return;
        }

        if (world.getEnvironment() == World.Environment.NETHER) {
            event.setCancelled(true);
            hubCommand.teleportToHub(player);
            player.sendMessage(ChatColor.GRAY + "Nether portal teleports to Hub in SkyBlock worlds.");
            return;
        }

        if (world.getEnvironment() == World.Environment.NORMAL) {
            if (isSkyblockWorld(world)) {
                event.setCancelled(true);
                hubCommand.teleportToHub(player);
                player.sendMessage(ChatColor.GREEN + "Teleported to the Hub via Nether Portal!");
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (onCooldown.contains(playerId)) {
            return;
        }

        World world = player.getWorld();
        if (!isSkyblockWorld(world)) {
            return;
        }

        Block block = player.getLocation().getBlock();
        if (isNetherPortalBlock(block)) {
            onCooldown.add(playerId);
            hubCommand.teleportToHub(player);

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                onCooldown.remove(playerId);
            }, 20L);
        }
    }

    private boolean isNetherPortalBlock(Block block) {
        Material type = block.getType();
        return type == Material.NETHER_PORTAL || type == Material.OBSIDIAN;
    }

    private boolean isSkyblockWorld(World world) {
        String skyblockWorldName = plugin.getConfig().getString("skyblock.world-name", "skyblock_world");
        return world.getName().equalsIgnoreCase(skyblockWorldName);
    }
}
