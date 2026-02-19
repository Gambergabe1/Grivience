package io.papermc.Grivience.skyblock.command;

import io.papermc.Grivience.GriviencePlugin;
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
import java.util.Locale;

public final class SkyblockAdminCommand implements CommandExecutor, TabCompleter {
    private final GriviencePlugin plugin;

    public SkyblockAdminCommand(GriviencePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("grivience.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }

        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        switch (subCommand) {
            case "sethub" -> handleSetHub(player);
            case "reload" -> handleReload(sender);
            case "help" -> sendHelp(sender);
            default -> {
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /skyblockadmin help");
            }
        }
        return true;
    }

    private void handleSetHub(Player player) {
        Location location = player.getLocation();
        World world = location.getWorld();

        if (world == null) {
            player.sendMessage(ChatColor.RED + "Failed to get current world.");
            return;
        }

        plugin.getConfig().set("skyblock.hub-world", world.getName());
        plugin.getConfig().set("skyblock.hub-spawn.x", location.getX());
        plugin.getConfig().set("skyblock.hub-spawn.y", location.getY());
        plugin.getConfig().set("skyblock.hub-spawn.z", location.getZ());
        plugin.getConfig().set("skyblock.hub-spawn.yaw", location.getYaw());
        plugin.getConfig().set("skyblock.hub-spawn.pitch", location.getPitch());

        try {
            plugin.saveConfig();
            player.sendMessage(ChatColor.GREEN + "Hub location set successfully!");
            player.sendMessage(ChatColor.GRAY + "World: " + ChatColor.AQUA + world.getName());
            player.sendMessage(ChatColor.GRAY + "Location: " + ChatColor.AQUA + 
                    String.format("%.2f, %.2f, %.2f", location.getX(), location.getY(), location.getZ()));
            player.sendMessage(ChatColor.YELLOW + "Players will now teleport here when using /hub or /spawn");
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Failed to save config. Check console for errors.");
            plugin.getLogger().severe("Failed to save hub location: " + e.getMessage());
        }
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "Grivience config reloaded.");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== SkyBlock Admin Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/skyblockadmin sethub" + ChatColor.GRAY + " - Set hub spawn location");
        sender.sendMessage(ChatColor.YELLOW + "/skyblockadmin reload" + ChatColor.GRAY + " - Reload plugin config");
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> commands = new ArrayList<>(List.of("sethub", "reload", "help"));
            return filterPrefix(commands, args[0]);
        }
        return List.of();
    }

    private List<String> filterPrefix(List<String> input, String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        List<String> filtered = new ArrayList<>();
        for (String candidate : input) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                filtered.add(candidate);
            }
        }
        return filtered;
    }
}
