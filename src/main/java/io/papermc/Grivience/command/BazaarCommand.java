package io.papermc.Grivience.command;

import io.papermc.Grivience.bazaar.BazaarShopManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BazaarCommand implements CommandExecutor, TabCompleter {
    private final BazaarShopManager bazaarShopManager;

    public BazaarCommand(BazaarShopManager bazaarShopManager) {
        this.bazaarShopManager = bazaarShopManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (!bazaarShopManager.isEnabled()) {
            player.sendMessage(ChatColor.RED + "Bazaar is disabled in config.");
            return true;
        }

        if (args.length == 0) {
            bazaarShopManager.openMainMenu(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "menu", "main" -> bazaarShopManager.openMainMenu(player);
            case "custom", "plugin" -> bazaarShopManager.openCustomMenu(player, 0);
            case "vanilla", "base" -> bazaarShopManager.openVanillaMenu(player, 0);
            case "help" -> sendHelp(player, label);
            default -> {
                player.sendMessage(ChatColor.RED + "Unknown subcommand. Use /" + label + " help.");
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

    private void sendHelp(Player player, String label) {
        player.sendMessage(ChatColor.GOLD + "Bazaar Commands");
        player.sendMessage(ChatColor.YELLOW + "/" + label + ChatColor.GRAY + " - Open Bazaar main menu");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " custom" + ChatColor.GRAY + " - Open plugin items");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " vanilla" + ChatColor.GRAY + " - Open base server items");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> root = new ArrayList<>(List.of("menu", "custom", "vanilla", "help"));
            return filterPrefix(root, args[0]);
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
