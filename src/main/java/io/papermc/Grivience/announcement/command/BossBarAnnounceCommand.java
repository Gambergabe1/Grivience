package io.papermc.Grivience.announcement.command;

import io.papermc.Grivience.announcement.BossBarAnnouncementManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class BossBarAnnounceCommand implements CommandExecutor, TabCompleter {
    private static final List<String> COLORS = Arrays.asList("BLUE", "GREEN", "PINK", "PURPLE", "RED", "WHITE", "YELLOW");
    private static final List<String> STYLES = Arrays.asList("SOLID", "SEGMENTED_6", "SEGMENTED_10", "SEGMENTED_12", "SEGMENTED_20");

    private final BossBarAnnouncementManager manager;

    public BossBarAnnounceCommand(BossBarAnnouncementManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("grivience.announce")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to send announcements.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);

        switch (subCommand) {
            case "send", "announce" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /bossbar announce <message> [color] [style]");
                    return true;
                }
                
                // Parse message (everything after the subcommand)
                StringBuilder messageBuilder = new StringBuilder();
                BarColor color = manager.getDefaultColor();
                BarStyle style = manager.getDefaultStyle();
                
                int argIndex = 1;
                
                // Check for color argument
                if (argIndex < args.length) {
                    String potentialColor = args[argIndex].toUpperCase(Locale.ROOT);
                    if (COLORS.contains(potentialColor)) {
                        try {
                            color = BarColor.valueOf(potentialColor);
                            argIndex++;
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
                
                // Check for style argument
                if (argIndex < args.length) {
                    String potentialStyle = args[argIndex].toUpperCase(Locale.ROOT);
                    if (STYLES.contains(potentialStyle)) {
                        try {
                            style = BarStyle.valueOf(potentialStyle);
                            argIndex++;
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
                
                // Rest is the message
                for (int i = argIndex; i < args.length; i++) {
                    if (i > argIndex) messageBuilder.append(" ");
                    messageBuilder.append(args[i]);
                }
                
                String message = messageBuilder.toString();
                if (message.isBlank()) {
                    sender.sendMessage(ChatColor.RED + "Usage: /bossbar announce <message> [color] [style]");
                    return true;
                }
                
                manager.announce(message, color, style);
                sender.sendMessage(ChatColor.GREEN + "Announcement sent to " + ChatColor.AQUA + Bukkit.getOnlinePlayers().size() + ChatColor.GREEN + " players.");
            }
            
            case "to" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /bossbar to <player> <message> [color] [style]");
                    return true;
                }
                
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                    return true;
                }
                
                StringBuilder messageBuilder = new StringBuilder();
                BarColor color = manager.getDefaultColor();
                BarStyle style = manager.getDefaultStyle();
                
                int argIndex = 2;
                
                // Check for color argument
                if (argIndex < args.length) {
                    String potentialColor = args[argIndex].toUpperCase(Locale.ROOT);
                    if (COLORS.contains(potentialColor)) {
                        try {
                            color = BarColor.valueOf(potentialColor);
                            argIndex++;
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
                
                // Check for style argument
                if (argIndex < args.length) {
                    String potentialStyle = args[argIndex].toUpperCase(Locale.ROOT);
                    if (STYLES.contains(potentialStyle)) {
                        try {
                            style = BarStyle.valueOf(potentialStyle);
                            argIndex++;
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
                
                // Rest is the message
                for (int i = argIndex; i < args.length; i++) {
                    if (i > argIndex) messageBuilder.append(" ");
                    messageBuilder.append(args[i]);
                }
                
                String message = messageBuilder.toString();
                if (message.isBlank()) {
                    sender.sendMessage(ChatColor.RED + "Usage: /bossbar to <player> <message> [color] [style]");
                    return true;
                }
                
                manager.announceTo(target, message, color, style);
                sender.sendMessage(ChatColor.GREEN + "Announcement sent to " + ChatColor.AQUA + target.getName() + ChatColor.GREEN + ".");
            }
            
            case "clear", "remove" -> {
                manager.removeAllBossBars();
                sender.sendMessage(ChatColor.GREEN + "All boss bar announcements cleared.");
            }
            
            case "reload" -> {
                manager.reload();
                sender.sendMessage(ChatColor.GREEN + "BossBar announcement settings reloaded.");
            }
            
            case "help" -> {
                sendUsage(sender);
            }
            
            default -> {
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /bossbar help for usage.");
            }
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== BossBar Announcement Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/bossbar announce <message> [color] [style]" + ChatColor.GRAY + " - Send announcement to all players");
        sender.sendMessage(ChatColor.YELLOW + "/bossbar to <player> <message> [color] [style]" + ChatColor.GRAY + " - Send announcement to a player");
        sender.sendMessage(ChatColor.YELLOW + "/bossbar clear" + ChatColor.GRAY + " - Remove all active boss bars");
        sender.sendMessage(ChatColor.YELLOW + "/bossbar reload" + ChatColor.GRAY + " - Reload configuration");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "Colors: " + ChatColor.WHITE + String.join(", ", COLORS));
        sender.sendMessage(ChatColor.GRAY + "Styles: " + ChatColor.WHITE + String.join(", ", STYLES));
        sender.sendMessage(ChatColor.GRAY + "Example: " + ChatColor.YELLOW + "/bossbar announce &6Server Restart in 5 minutes! RED SOLID");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterPrefix(List.of("announce", "to", "clear", "reload", "help"), args[0]);
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("to")) {
            List<String> playerNames = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                playerNames.add(online.getName());
            }
            return filterPrefix(playerNames, args[1]);
        }
        
        if (args.length == 3 && args[0].equalsIgnoreCase("to")) {
            return filterPrefix(COLORS, args[2]);
        }
        
        if (args.length == 4 && args[0].equalsIgnoreCase("to")) {
            return filterPrefix(STYLES, args[3]);
        }
        
        if (args.length == 2 && (args[0].equalsIgnoreCase("announce") || args[0].equalsIgnoreCase("send"))) {
            return filterPrefix(COLORS, args[1]);
        }
        
        if (args.length == 3 && (args[0].equalsIgnoreCase("announce") || args[0].equalsIgnoreCase("send"))) {
            return filterPrefix(STYLES, args[2]);
        }
        
        return List.of();
    }

    private List<String> filterPrefix(List<String> input, String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        return input.stream()
                .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(lowerPrefix))
                .collect(Collectors.toList());
    }
}
