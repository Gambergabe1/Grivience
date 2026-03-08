package io.papermc.Grivience.fasttravel;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Command handler for fast travel functionality.
 */
public final class FastTravelCommand implements CommandExecutor, TabCompleter {
    private final FastTravelManager manager;
    private final FastTravelGui gui;

    public FastTravelCommand(FastTravelManager manager, FastTravelGui gui) {
        this.manager = manager;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }

        if (args.length == 0) {
            gui.open(player);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);

        switch (subCommand) {
            case "unlock" -> {
                if (!player.hasPermission("grivience.fasttravel.admin")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to unlock destinations!");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /fasttravel unlock <player> [destination]");
                    return true;
                }
                Player target = player.getServer().getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(ChatColor.RED + "Player not found!");
                    return true;
                }
                if (args.length >= 3) {
                    String destKey = args[2];
                    if (manager.getPointByName(destKey) == null) {
                        player.sendMessage(ChatColor.RED + "Unknown destination: " + destKey);
                        return true;
                    }
                    manager.unlock(target, destKey);
                    player.sendMessage(ChatColor.GREEN + "Unlocked " + destKey + " for " + target.getName());
                } else {
                    manager.unlockAll(target);
                    player.sendMessage(ChatColor.GREEN + "Unlocked all destinations for " + target.getName());
                }
            }
            case "lock" -> {
                if (!player.hasPermission("grivience.fasttravel.admin")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to lock destinations!");
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /fasttravel lock <player> <destination>");
                    return true;
                }
                Player target = player.getServer().getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(ChatColor.RED + "Player not found!");
                    return true;
                }
                String destKey = args[2];
                if (manager.getPointByName(destKey) == null) {
                    player.sendMessage(ChatColor.RED + "Unknown destination: " + destKey);
                    return true;
                }
                if (manager.lock(target, destKey)) {
                    player.sendMessage(ChatColor.GREEN + "Locked " + destKey + " for " + target.getName());
                } else {
                    player.sendMessage(ChatColor.YELLOW + target.getName() + " hasn't unlocked " + destKey);
                }
            }
            case "add" -> {
                if (!player.hasPermission("grivience.fasttravel.admin")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to add destinations!");
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /fasttravel add <key> <name> [head-owner]");
                    return true;
                }
                String key = args[1];
                String name = args[2];
                String headOwner = args.length >= 4 ? args[3] : "";

                if (manager.getPointByName(key) != null) {
                    player.sendMessage(ChatColor.RED + "Destination with key '" + key + "' already exists!");
                    return true;
                }

                var point = manager.createPointFromLocation(
                        key,
                        name,
                        player.getLocation(),
                        "A fast travel destination",
                        headOwner,
                        0
                );
                manager.addPoint(point);
                player.sendMessage(ChatColor.GREEN + "Added fast travel destination: " + name);
            }
            case "remove" -> {
                if (!player.hasPermission("grivience.fasttravel.admin")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to remove destinations!");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /fasttravel remove <key>");
                    return true;
                }
                String key = args[1];
                if (manager.getPointByName(key) == null) {
                    player.sendMessage(ChatColor.RED + "Unknown destination: " + key);
                    return true;
                }
                manager.removePoint(key);
                player.sendMessage(ChatColor.GREEN + "Removed fast travel destination: " + key);
            }
            case "list" -> {
                player.sendMessage("");
                player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Fast Travel Destinations");
                player.sendMessage("");
                var unlocks = manager.getUnlockedPoints(player);
                for (var point : manager.getAllPoints()) {
                    boolean unlocked = unlocks.contains(point.key());
                    String status = unlocked ? ChatColor.GREEN + "UNLOCKED" : ChatColor.RED + "LOCKED";
                    player.sendMessage(status + ChatColor.GRAY + " " + point.name() + ChatColor.DARK_GRAY + " (" + point.key() + ")");
                }
                player.sendMessage("");
            }
            case "reload" -> {
                if (!player.hasPermission("grivience.fasttravel.admin")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to reload!");
                    return true;
                }
                manager.load();
                player.sendMessage(ChatColor.GREEN + "Fast travel configuration reloaded!");
            }
            default -> {
                String destKey = args[0];
                if (args.length == 1 && manager.getPointByName(destKey) != null) {
                    if (!manager.isUnlocked(player, destKey)) {
                        player.sendMessage(ChatColor.RED + "Locked! You haven't unlocked this destination yet.");
                        player.sendMessage(ChatColor.GRAY + "Unlock destinations by visiting them first or use " + ChatColor.YELLOW + "/fasttravel" + ChatColor.GRAY + ".");
                        return true;
                    }

                    manager.teleport(player, destKey);
                    return true;
                }

                player.sendMessage("");
                player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Fast Travel Commands");
                player.sendMessage("");
                player.sendMessage(ChatColor.AQUA + "/fasttravel" + ChatColor.GRAY + " - Open fast travel menu");
                player.sendMessage(ChatColor.AQUA + "/fasttravel <destination>" + ChatColor.GRAY + " - Teleport to a destination");
                player.sendMessage(ChatColor.AQUA + "/fasttravel list" + ChatColor.GRAY + " - List all destinations");
                if (player.hasPermission("grivience.fasttravel.admin")) {
                    player.sendMessage(ChatColor.AQUA + "/fasttravel unlock <player> [dest]" + ChatColor.GRAY + " - Unlock destination");
                    player.sendMessage(ChatColor.AQUA + "/fasttravel lock <player> <dest>" + ChatColor.GRAY + " - Lock destination");
                    player.sendMessage(ChatColor.AQUA + "/fasttravel add <key> <name> [head]" + ChatColor.GRAY + " - Add destination");
                    player.sendMessage(ChatColor.AQUA + "/fasttravel remove <key>" + ChatColor.GRAY + " - Remove destination");
                    player.sendMessage(ChatColor.AQUA + "/fasttravel reload" + ChatColor.GRAY + " - Reload config");
                }
                player.sendMessage("");
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("list");
            if (sender.hasPermission("grivience.fasttravel.admin")) {
                completions.add("unlock");
                completions.add("lock");
                completions.add("add");
                completions.add("remove");
                completions.add("reload");
            }

            if (sender instanceof Player player) {
                for (var point : manager.getAllPoints()) {
                    if (manager.isUnlocked(player, point.key())) {
                        completions.add(point.key());
                    }
                }
            }
        } else if (args.length == 2 && sender.hasPermission("grivience.fasttravel.admin")) {
            String subCommand = args[0].toLowerCase(Locale.ROOT);
            if (subCommand.equals("unlock") || subCommand.equals("lock")) {
                for (Player player : sender.getServer().getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            } else if (subCommand.equals("remove")) {
                for (var point : manager.getAllPoints()) {
                    completions.add(point.key());
                }
            }
        } else if (args.length == 3 && sender.hasPermission("grivience.fasttravel.admin")) {
            String subCommand = args[0].toLowerCase(Locale.ROOT);
            if (subCommand.equals("unlock") || subCommand.equals("lock")) {
                for (var point : manager.getAllPoints()) {
                    completions.add(point.key());
                }
            }
        }

        return completions;
    }
}
