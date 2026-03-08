package io.papermc.Grivience.listener;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.stats.SkyblockLevelManager;
import io.papermc.Grivience.nick.NickManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * 100% Skyblock Accurate Chat Formatting: [LEVEL] [RANK] Name: Message
 */
public final class SkyblockChatListener implements Listener {
    private final GriviencePlugin plugin;
    private final SkyblockLevelManager levelManager;
    private final NickManager nickManager;

    public SkyblockChatListener(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.levelManager = plugin.getSkyblockLevelManager();
        this.nickManager = plugin.getNickManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (levelManager == null) return;

        int level = levelManager.getLevel(player);
        ChatColor levelColor = levelManager.getLevelColor(level);
        
        String levelPrefix = ChatColor.DARK_GRAY + "[" + levelColor + level + ChatColor.DARK_GRAY + "] ";
        String rankAndName = "";

        if (nickManager != null && nickManager.isNicked(player)) {
            NickManager.NickData data = nickManager.getNickData(player);
            rankAndName = data.rank().getPrefix() + data.nickname();
        } else {
            // For non-nicked, use display name which usually includes rank if another plugin (like LP/Vault) sets it.
            // If displayName is just the name, that's fine too.
            rankAndName = player.getDisplayName();
        }

        // Hypixel style: [LEVEL] [RANK] Name: Message
        // We use %2$s to include the original message format safely.
        event.setFormat(levelPrefix + rankAndName + ChatColor.WHITE + ": " + "%2$s");
    }
}
