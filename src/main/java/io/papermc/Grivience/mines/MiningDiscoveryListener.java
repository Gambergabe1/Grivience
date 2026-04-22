package io.papermc.Grivience.mines;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skyblock.profile.ProfileManager;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public final class MiningDiscoveryListener implements Listener {
    private final GriviencePlugin plugin;

    public MiningDiscoveryListener(GriviencePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        ProfileManager pm = plugin.getProfileManager();
        if (pm == null) return;

        SkyBlockProfile profile = pm.getSelectedProfile(player);
        if (profile == null) return;

        String layerName = null;
        Location loc = event.getTo();
        String worldName = loc.getWorld().getName();
        
        String minehubWorld = plugin.getConfig().getString("skyblock.minehub-world", "Minehub");
        if (worldName.equalsIgnoreCase(minehubWorld)) {
            if (plugin.getMinehubOreListener() != null) {
                layerName = plugin.getMinehubOreListener().getLayerName(loc.getBlockY());
            }
        } else if (plugin.getEndMinesManager() != null && worldName.equalsIgnoreCase(plugin.getEndMinesManager().getWorldName())) {
            layerName = plugin.getEndMinesManager().getZoneName(loc);
        }

        if (layerName != null && !profile.hasDiscoveredLayer(layerName)) {
            profile.addDiscoveredLayer(layerName);
            player.sendMessage(ChatColor.GOLD + " \u272a " + ChatColor.YELLOW + "New Area Discovered: " + ChatColor.AQUA + ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', layerName)));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
        }
    }
}
