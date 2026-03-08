package io.papermc.Grivience.nick;

import io.papermc.Grivience.GriviencePlugin;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.PrefixNode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages nicknames and rank overrides - 100% Skyblock accurate.
 */
public final class NickManager {
    private final GriviencePlugin plugin;
    private final Map<UUID, NickData> nicks = new HashMap<>();
    
    public NickManager(GriviencePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Apply a nickname and rank to a player.
     */
    public void applyNick(Player player, String nickname, NickRank rank) {
        NickData data = new NickData(nickname, rank);
        nicks.put(player.getUniqueId(), data);
        
        // Update display name
        String fullName = rank.getPrefix() + nickname;
        player.setDisplayName(fullName);
        player.setPlayerListName(fullName);
        
        // Update LuckPerms prefix (for other plugins reading from LP)
        updateLuckPermsPrefix(player, rank.getPrefix());
        
        // Mark as nicked
        player.setMetadata("is_nicked", new FixedMetadataValue(plugin, true));
        
        player.sendMessage(ChatColor.GREEN + "You are now nicked as " + fullName + ChatColor.GREEN + "!");
        player.sendMessage(ChatColor.GRAY + "Note: Real names are hidden from tab and chat.");
    }

    /**
     * Remove nickname from a player.
     */
    public void removeNick(Player player) {
        if (!nicks.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You are not currently nicked.");
            return;
        }
        
        nicks.remove(player.getUniqueId());
        player.removeMetadata("is_nicked", plugin);
        
        // Remove LuckPerms prefix override
        updateLuckPermsPrefix(player, null);
        
        // Restore original names
        player.setDisplayName(player.getName());
        player.setPlayerListName(player.getName());
        
        player.sendMessage(ChatColor.GREEN + "Your nickname has been removed.");
    }

    private void updateLuckPermsPrefix(Player player, String prefix) {
        try {
            LuckPerms lp = LuckPermsProvider.get();
            User user = lp.getUserManager().getUser(player.getUniqueId());
            if (user == null) return;

            // Remove existing nick prefixes (priority 1000)
            user.data().clear(node -> node instanceof PrefixNode pn && pn.getPriority() == 1000);

            if (prefix != null) {
                // Add new nick prefix
                user.data().add(PrefixNode.builder(prefix, 1000).build());
            }

            lp.getUserManager().saveUser(user);
        } catch (Exception ignored) {
            // LuckPerms might not be installed or API not ready
        }
    }

    public boolean isNicked(Player player) {
        return nicks.containsKey(player.getUniqueId());
    }

    public NickData getNickData(Player player) {
        return nicks.get(player.getUniqueId());
    }

    public enum NickRank {
        DEFAULT("§7", "Default", "§7"),
        VIP("§a[VIP] ", "VIP", "§a"),
        VIP_PLUS("§a[VIP§6+§a] ", "VIP+", "§a"),
        MVP("§b[MVP] ", "MVP", "§b"),
        MVP_PLUS("§b[MVP§c+§b] ", "MVP+", "§b"),
        MVP_PLUS_PLUS("§6[MVP§c++§6] ", "MVP++", "§6");

        private final String prefix;
        private final String name;
        private final String color;

        NickRank(String prefix, String name, String color) {
            this.prefix = prefix;
            this.name = name;
            this.color = color;
        }

        public String getPrefix() { return prefix; }
        public String getName() { return name; }
        public String getColor() { return color; }
    }

    public record NickData(String nickname, NickRank rank) {}
}
