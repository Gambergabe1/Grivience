package io.papermc.Grivience.wardrobe;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Command handler for wardrobe commands.
 */
public class WardrobeCommand implements CommandExecutor, TabCompleter {
    private final WardrobeGui gui;

    public WardrobeCommand(WardrobeGui gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            // Open wardrobe GUI
            gui.open(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("help")) {
            sendHelp(player);
            return true;
        }

        player.sendMessage(ChatColor.RED + "Unknown subcommand. Use /wardrobe help for help.");
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Wardrobe Commands");
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "/wardrobe" + ChatColor.GRAY + " - Open wardrobe GUI");
        player.sendMessage(ChatColor.YELLOW + "/wardrobe help" + ChatColor.GRAY + " - Show this help");
        player.sendMessage("");
        player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "Controls:");
        player.sendMessage(ChatColor.GRAY + "  • Left-Click: Equip armor");
        player.sendMessage(ChatColor.GRAY + "  • Right-Click: Save armor");
        player.sendMessage(ChatColor.GRAY + "  • Shift+Right: Rename slot");
        player.sendMessage("");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage("");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String input = args[0].toLowerCase();

            if ("help".startsWith(input)) {
                completions.add("help");
            }

            return completions;
        }

        return new ArrayList<>();
    }
}
