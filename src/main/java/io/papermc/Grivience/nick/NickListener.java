package io.papermc.Grivience.nick;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.bazaar.BazaarKeys;
import io.papermc.Grivience.nick.NickManager;
import io.papermc.Grivience.nick.NickGuiManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public final class NickListener implements Listener {
    private final NickManager nickManager;

    public NickListener(NickManager nickManager) {
        this.nickManager = nickManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        // Chat formatting is now handled by SkyblockChatListener for all players, 
        // including [LEVEL] and [RANK] prefixes.
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // In a real production system, we'd load nicks from a database here.
        // For now, we ensure they aren't nicked on join to avoid state issues.
        Player player = event.getPlayer();
        player.setDisplayName(player.getName());
        player.setPlayerListName(player.getName());
    }
}
