package io.papermc.Grivience.command;

import io.papermc.Grivience.gui.SkyblockMenuManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SkyblockMenuCommand implements CommandExecutor, TabCompleter {
    private final SkyblockMenuManager skyblockMenuManager;

    public SkyblockMenuCommand(SkyblockMenuManager skyblockMenuManager) {
        this.skyblockMenuManager = skyblockMenuManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }

        if (args.length == 0) {
            skyblockMenuManager.openMainMenu(player);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        switch (subCommand) {
            case "menu", "main" -> {
                skyblockMenuManager.openMainMenu(player);
                player.sendMessage(ChatColor.GREEN + "Opened SkyBlock Menu.");
            }
            case "island", "is" -> {
                skyblockMenuManager.openIslandMenu(player);
                player.sendMessage(ChatColor.GREEN + "Opened Island Management.");
            }
            case "upgrades" -> {
                skyblockMenuManager.openUpgradesMenu(player);
                player.sendMessage(ChatColor.GREEN + "Opened Island Upgrades.");
            }
            case "minions" -> {
                skyblockMenuManager.openMinionsMenu(player);
                player.sendMessage(ChatColor.GREEN + "Opened Minion Management.");
            }
            case "settings" -> {
                skyblockMenuManager.openSettingsMenu(player);
                player.sendMessage(ChatColor.GREEN + "Opened Island Settings.");
            }
            case "permissions", "perms" -> {
                skyblockMenuManager.openPermissionsMenu(player);
                player.sendMessage(ChatColor.GREEN + "Opened Island Permissions.");
            }
            case "help" -> {
                sendHelp(player);
            }
            default -> {
                player.sendMessage(ChatColor.RED + "Unknown subcommand. Use /skyblock help.");
            }
        }
        return true;
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
        return null;
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== SkyBlock Menu Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/skyblock" + ChatColor.GRAY + " - Open main menu");
        player.sendMessage(ChatColor.YELLOW + "/skyblock island" + ChatColor.GRAY + " - Island management");
        player.sendMessage(ChatColor.YELLOW + "/skyblock upgrades" + ChatColor.GRAY + " - Browse upgrades");
        player.sendMessage(ChatColor.YELLOW + "/skyblock minions" + ChatColor.GRAY + " - Minion management");
        player.sendMessage(ChatColor.YELLOW + "/skyblock settings" + ChatColor.GRAY + " - Island settings");
        player.sendMessage(ChatColor.YELLOW + "/skyblock permissions" + ChatColor.GRAY + " - Manage permissions");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> commands = new ArrayList<>(List.of("menu", "island", "upgrades", "minions", "settings", "permissions", "help"));
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
