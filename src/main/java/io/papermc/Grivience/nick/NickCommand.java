package io.papermc.Grivience.nick;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public final class NickCommand implements CommandExecutor, TabCompleter {
    private final NickManager nickManager;
    private final NickGuiManager guiManager;

    public NickCommand(NickManager nickManager, NickGuiManager guiManager) {
        this.nickManager = nickManager;
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command is for players only.");
            return true;
        }

        if (!player.hasPermission("grivience.nick")) {
            player.sendMessage(ChatColor.RED + "You must have " + ChatColor.GOLD + "MVP++" + ChatColor.RED + " to use this feature!");
            return true;
        }

        if (label.equalsIgnoreCase("unnick")) {
            nickManager.removeNick(player);
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reset")) {
            nickManager.removeNick(player);
            return true;
        }

        guiManager.openMain(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (alias.equalsIgnoreCase("unnick")) return Collections.emptyList();
        if (args.length == 1) return List.of("reset");
        return Collections.emptyList();
    }
}
