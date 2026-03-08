package io.papermc.Grivience.command;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class GrivienceReloadCommand implements CommandExecutor {
    private final GriviencePlugin plugin;

    public GrivienceReloadCommand(GriviencePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("grivience.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }
        long start = System.currentTimeMillis();
        plugin.reloadSystems();
        sender.sendMessage(ChatColor.GREEN + "Grivience reloaded in " + (System.currentTimeMillis() - start) + "ms.");
        return true;
    }
}
