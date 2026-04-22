package io.papermc.Grivience.wizard;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WizardTowerCommand implements CommandExecutor, TabCompleter {
    private final WizardTowerManager manager;

    public WizardTowerCommand(WizardTowerManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            Player player = requirePlayer(sender);
            if (player == null) {
                return true;
            }
            manager.sendOverview(player, true);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "gui", "open", "shop" -> {
                Player player = requirePlayer(sender);
                if (player == null) {
                    return true;
                }
                manager.openGui(player);
            }
            case "buy", "use", "activate" -> {
                Player player = requirePlayer(sender);
                if (player == null) {
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " buy <tier>");
                    return true;
                }
                manager.purchase(player, args[1]);
            }
            case "status" -> {
                Player player = requirePlayer(sender);
                if (player == null) {
                    return true;
                }
                manager.sendStatus(player);
            }
            case "list", "tiers" -> {
                Player player = requirePlayer(sender);
                if (player == null) {
                    return true;
                }
                manager.sendOverview(player, true);
            }
            case "setznpc", "znpc" -> {
                if (!requireAdmin(sender)) {
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " setznpc <znpcsNpcId|none>");
                    return true;
                }
                String value = args[1];
                if (value.equalsIgnoreCase("none") || value.equalsIgnoreCase("clear")) {
                    value = "";
                }
                manager.setTrackedNpcId(value);
                if (manager.getTrackedNpcId().isBlank()) {
                    sender.sendMessage(ChatColor.YELLOW + "Wizard Tower ZNPC binding cleared.");
                } else {
                    sender.sendMessage(ChatColor.GREEN + "Wizard Tower ZNPC set to: " + ChatColor.AQUA + manager.getTrackedNpcId());
                }
            }
            case "reload" -> {
                if (!requireAdmin(sender)) {
                    return true;
                }
                manager.reload();
                sender.sendMessage(ChatColor.GREEN + "Wizard Tower configuration reloaded.");
            }
            case "help" -> sendHelp(sender, label);
            default -> sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /" + label + " help");
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

    private boolean requireAdmin(CommandSender sender) {
        if (sender.hasPermission("grivience.wizardtower.admin")) {
            return true;
        }
        sender.sendMessage(ChatColor.RED + "You do not have permission.");
        return false;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "=== Wizard Tower Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + ChatColor.GRAY + " - Open the Wizard Tower GUI");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " buy <tier>" + ChatColor.GRAY + " - Permanently unlock one blessing");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " status" + ChatColor.GRAY + " - View your stacked bonuses");

        if (!sender.hasPermission("grivience.wizardtower.admin")) {
            return;
        }
        sender.sendMessage(ChatColor.DARK_GRAY + "--- Admin ---");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " setznpc <znpcsNpcId|none>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> root = new ArrayList<>(List.of("gui", "open", "shop", "buy", "status", "list", "help"));
            if (sender.hasPermission("grivience.wizardtower.admin")) {
                root.add("setznpc");
                root.add("reload");
            }
            return filterPrefix(root, args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("buy") || sub.equals("use") || sub.equals("activate")) {
                return filterPrefix(manager.tierIds(), args[1]);
            }
            if (sub.equals("setznpc") || sub.equals("znpc")) {
                List<String> values = new ArrayList<>();
                if (!manager.getTrackedNpcId().isBlank()) {
                    values.add(manager.getTrackedNpcId());
                }
                values.add("none");
                return filterPrefix(values, args[1]);
            }
        }

        return List.of();
    }

    private List<String> filterPrefix(List<String> values, String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        List<String> output = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                output.add(value);
            }
        }
        return output;
    }
}
