package io.papermc.Grivience.welcome;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Command handler for welcome event commands.
 */
public class WelcomeCommand implements CommandExecutor, TabCompleter {
    private final GriviencePlugin plugin;
    private final WelcomeEventManager welcomeManager;

    public WelcomeCommand(GriviencePlugin plugin, WelcomeEventManager welcomeManager) {
        this.plugin = plugin;
        this.welcomeManager = welcomeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            // Open welcome GUI
            welcomeManager.openWelcomeGUI(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("claim")) {
            // Claim rewards
            welcomeManager.claimRewards(player);
            return true;
        }

        if (subCommand.equals("status")) {
            welcomeManager.sendStatusMessage(player);
            return true;
        }

        if (subCommand.equals("help")) {
            sendHelp(player);
            return true;
        }

        player.sendMessage(ChatColor.RED + "Unknown subcommand. Use /welcome help for help.");
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Welcome Event Commands");
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "/welcome" + ChatColor.GRAY + " - Open welcome GUI");
        player.sendMessage(ChatColor.YELLOW + "/welcome claim" + ChatColor.GRAY + " - Claim starter rewards");
        player.sendMessage(ChatColor.YELLOW + "/welcome status" + ChatColor.GRAY + " - View claim/boost status");
        player.sendMessage(ChatColor.YELLOW + "/welcome help" + ChatColor.GRAY + " - Show this help");
        player.sendMessage("");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String input = args[0].toLowerCase();

            if ("claim".startsWith(input)) {
                completions.add("claim");
            }
            if ("status".startsWith(input)) {
                completions.add("status");
            }
            if ("help".startsWith(input)) {
                completions.add("help");
            }

            return completions;
        }

        return new ArrayList<>();
    }
}
