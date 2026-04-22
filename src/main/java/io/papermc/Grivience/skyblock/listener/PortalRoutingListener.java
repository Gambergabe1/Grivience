package io.papermc.Grivience.skyblock.listener;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skyblock.portal.PortalRoutingManager;
import io.papermc.Grivience.skyblock.portal.PortalRoutingManager.PortalKind;
import org.bukkit.ChatColor;
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

public final class PortalRoutingListener implements Listener {
    private final GriviencePlugin plugin;
    private final PortalRoutingManager portalRoutingManager;
    private final Set<UUID> onCooldown = new HashSet<>();

    public PortalRoutingListener(GriviencePlugin plugin, PortalRoutingManager portalRoutingManager) {
        this.plugin = plugin;
        this.portalRoutingManager = portalRoutingManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        PortalKind portalKind = resolvePortalKind(event.getCause());
        if (portalKind == null || !shouldHandle(player.getWorld())) {
            return;
        }

        if (!portalRoutingManager.isEnabled(portalKind)) {
            return;
        }

        event.setCancelled(true);
        routePlayer(player, portalKind);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
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
        PortalKind portalKind = detectPortalKind(player);
        if (portalKind == null || !shouldHandle(world) || !portalRoutingManager.isEnabled(portalKind)) {
            return;
        }

        routePlayer(player, portalKind);
    }

    private void routePlayer(Player player, PortalKind portalKind) {
        UUID playerId = player.getUniqueId();
        onCooldown.add(playerId);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                if (!portalRoutingManager.teleport(player, portalKind)) {
                    player.sendMessage(ChatColor.RED + portalKind.displayName() + " portal route failed.");
                }
            } finally {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> onCooldown.remove(playerId), 20L);
            }
        });
    }

    private PortalKind detectPortalKind(Player player) {
        Block feet = player.getLocation().getBlock();
        Block head = player.getLocation().clone().add(0.0D, 1.0D, 0.0D).getBlock();
        if (isPortalBlock(feet, PortalKind.END)) {
            return PortalKind.END;
        }
        if (isPortalBlock(feet, PortalKind.NETHER) || isPortalBlock(head, PortalKind.NETHER)) {
            return PortalKind.NETHER;
        }
        return null;
    }

    private boolean isPortalBlock(Block block, PortalKind portalKind) {
        Material type = block.getType();
        return switch (portalKind) {
            case NETHER -> type == Material.NETHER_PORTAL;
            case END -> type == Material.END_PORTAL;
        };
    }

    private PortalKind resolvePortalKind(PlayerTeleportEvent.TeleportCause cause) {
        if (cause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            return PortalKind.NETHER;
        }
        if (cause == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            return PortalKind.END;
        }
        return null;
    }

    private boolean shouldHandle(World world) {
        return world != null;
    }
}

