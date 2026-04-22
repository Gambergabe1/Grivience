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
            case "booster" -> {
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "Usage: /globalevent booster <type> <multiplier> <duration_minutes>");
                    sender.sendMessage(ChatColor.GRAY + "Types: EXPERIENCE, MINION_SPEED, DAMAGE, MINEHUB_HEART, ENDMINES_HEART");
                    return true;
                }
                String typeStr = args[1].toUpperCase();
                GlobalEventManager.BoosterType type;
                try {
                    type = GlobalEventManager.BoosterType.valueOf(typeStr);
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid booster type: " + typeStr);
                    return true;
                }

                double multiplier;
                int duration;
                try {
                    multiplier = Double.parseDouble(args[2]);
                    duration = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid multiplier or duration. Please enter numbers.");
                    return true;
                }

                eventManager.startBooster(type, multiplier, duration);
                sender.sendMessage(ChatColor.GREEN + "Started Global " + type.getDisplayName() + " Boost: " + multiplier + "x for " + duration + " minutes.");
            }
            case "stopbooster" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /globalevent stopbooster <type>");
                    return true;
                }
                String typeStr = args[1].toUpperCase();
                try {
                    GlobalEventManager.BoosterType type = GlobalEventManager.BoosterType.valueOf(typeStr);
                    eventManager.stopBooster(type);
                    sender.sendMessage(ChatColor.GREEN + "Stopped Global " + type.getDisplayName() + " Boost.");
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid booster type: " + typeStr);
                }
            }
            case "status" -> {
                List<String> active = eventManager.getActiveEvents();
                if (active.isEmpty()) {
                    sender.sendMessage(ChatColor.YELLOW + "No global events are currently active.");
                } else {
                    sender.sendMessage(ChatColor.GOLD + "--- Active Global Events & Boosters ---");
                    for (String e : active) {
                        sender.sendMessage(ChatColor.WHITE + "- " + e);
                    }
                    
                    for (GlobalEventManager.BoosterType type : GlobalEventManager.BoosterType.values()) {
                        long remaining = eventManager.getRemainingMillis(type);
                        if (remaining > 0) {
                            sender.sendMessage(type.getColor() + type.getDisplayName() + ChatColor.GOLD + " Time Remaining: " + ChatColor.YELLOW + (remaining / 60000L) + " minutes");
                        }
                    }
                }
            }
            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- Global Event Admin Commands ---");
        sender.sendMessage(ChatColor.YELLOW + "/globalevent booster <type> <mult> <min> " + ChatColor.GRAY + "- Start a global booster");
        sender.sendMessage(ChatColor.YELLOW + "/globalevent stopbooster <type> " + ChatColor.GRAY + "- Stop a global booster");
        sender.sendMessage(ChatColor.YELLOW + "/globalevent startboost <multiplier> <duration> " + ChatColor.GRAY + "- Start a global XP boost");
        sender.sendMessage(ChatColor.YELLOW + "/globalevent stopboost " + ChatColor.GRAY + "- Stop active global XP boost");
        sender.sendMessage(ChatColor.YELLOW + "/globalevent status " + ChatColor.GRAY + "- View current active events");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Stream.of("booster", "stopbooster", "startboost", "stopboost", "status")
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("booster") || args[0].equalsIgnoreCase("stopbooster"))) {
            return Stream.of(GlobalEventManager.BoosterType.values())
                    .map(Enum::name)
                    .filter(s -> s.startsWith(args[1].toUpperCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
