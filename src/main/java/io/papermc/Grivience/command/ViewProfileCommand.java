package io.papermc.Grivience.command;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skyblock.profile.gui.ProfileViewGui;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Command to view another player's Skyblock profile GUI.
 */
public final class ViewProfileCommand implements CommandExecutor, TabCompleter {
    private final GriviencePlugin plugin;
    private final ProfileViewGui profileViewGui;

    public ViewProfileCommand(GriviencePlugin plugin, ProfileViewGui profileViewGui) {
        this.plugin = plugin;
        this.profileViewGui = profileViewGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /viewprofile <player>");
            return true;
        }

        String targetName = args[0];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        
        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            player.sendMessage(ChatColor.RED + "Player not found!");
            return true;
        }

        profileViewGui.open(player, target);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> players = new ArrayList<>();
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    players.add(p.getName());
                }
            }
            return players;
        }
        return List.of();
    }
}
