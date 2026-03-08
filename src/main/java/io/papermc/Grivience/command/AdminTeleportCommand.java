package io.papermc.Grivience.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class AdminTeleportCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }
        if (!sender.hasPermission("grivience.admin")) {
            sender.sendMessage(ChatColor.RED + "You need grivience.admin to use this.");
            return true;
        }

        if (args.length == 1) {
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }
            player.teleport(target.getLocation());
            sender.sendMessage(ChatColor.GREEN + "Teleported to " + target.getName() + ".");
            return true;
        }

        if (args.length >= 3) {
            try {
                double x = Double.parseDouble(args[0]);
                double y = Double.parseDouble(args[1]);
                double z = Double.parseDouble(args[2]);
                World world = args.length >= 4 ? Bukkit.getWorld(args[3]) : player.getWorld();
                if (world == null) {
                    sender.sendMessage(ChatColor.RED + "Unknown world: " + args[3]);
                    return true;
                }
                player.teleport(new Location(world, x, y, z));
                sender.sendMessage(ChatColor.GREEN + "Teleported to coordinates.");
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <player>|<x> <y> <z> [world]");
            }
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <player>|<x> <y> <z> [world]");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(prefix))
                    .collect(Collectors.toList());
        }
        if (args.length == 4) {
            String prefix = args[3].toLowerCase();
            List<String> worlds = new ArrayList<>();
            for (World world : Bukkit.getWorlds()) {
                if (world.getName().toLowerCase().startsWith(prefix)) {
                    worlds.add(world.getName());
                }
            }
            return worlds;
        }
        return List.of();
    }
}
