package io.papermc.Grivience.event;

import io.papermc.Grivience.mines.MiningEventManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Command for managing global server events.
 */
public class GlobalEventCommand implements CommandExecutor, TabCompleter {
    private final GlobalEventManager eventManager;

    public GlobalEventCommand(GlobalEventManager eventManager) {
        this.eventManager = eventManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("grivience.admin.globalevent")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "startboost" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /globalevent startboost <multiplier> <duration_minutes>");
                    return true;
                }
                double multiplier;
                int duration;
                try {
                    multiplier = Double.parseDouble(args[1]);
                    duration = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid multiplier or duration. Please enter numbers.");
                    return true;
                }
                eventManager.startGlobalXpBoost(multiplier, duration);
                sender.sendMessage(ChatColor.GREEN + "Started Global XP Boost: " + multiplier + "x for " + duration + " minutes.");
            }
            case "stopboost" -> {
                eventManager.stopGlobalXpBoost();
                sender.sendMessage(ChatColor.GREEN + "Stopped Global XP Boost.");
            }
            case "status" -> {
                List<String> active = eventManager.getActiveEvents();
                if (active.isEmpty()) {
                    sender.sendMessage(ChatColor.YELLOW + "No global events are currently active.");
                } else {
                    sender.sendMessage(ChatColor.GOLD + "--- Active Global Events ---");
                    for (String e : active) {
                        sender.sendMessage(ChatColor.WHITE + "- " + e);
                    }
                    long remaining = eventManager.getXpBoostRemainingMillis();
                    if (remaining > 0) {
                        sender.sendMessage(ChatColor.GOLD + "XP Boost Time Remaining: " + ChatColor.YELLOW + (remaining / 60000L) + " minutes");
                    }
                }
            }
            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- Global Event Admin Commands ---");
        sender.sendMessage(ChatColor.YELLOW + "/globalevent startboost <multiplier> <duration> " + ChatColor.GRAY + "- Start a global XP boost");
        sender.sendMessage(ChatColor.YELLOW + "/globalevent stopboost " + ChatColor.GRAY + "- Stop active global XP boost");
        sender.sendMessage(ChatColor.YELLOW + "/globalevent status " + ChatColor.GRAY + "- View current active events");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Stream.of("startboost", "stopboost", "status")
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
