package io.papermc.Grivience.stats.command;

import io.papermc.Grivience.stats.BitsManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class BitsAdminCommand implements CommandExecutor, TabCompleter {
    private final BitsManager bitsManager;

    public BitsAdminCommand(BitsManager bitsManager) {
        this.bitsManager = bitsManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("grivience.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to manage bits.");
            return true;
        }

        if (args.length < 3) {
            sendUsage(sender);
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        String targetName = args[1];

        UUID targetId = getPlayerUUID(targetName);
        if (targetId == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + targetName);
            return true;
        }

        String targetDisplayName = targetName;
        Player targetPlayer = Bukkit.getPlayer(targetId);
        if (targetPlayer != null) {
            targetDisplayName = targetPlayer.getName();
        }

        switch (action) {
            case "set" -> {
                long amount = parseAmount(sender, args[2]);
                if (amount < 0) {
                    return true;
                }
                bitsManager.setBits(targetId, amount);
                sender.sendMessage(ChatColor.GREEN + "Set " + ChatColor.AQUA + targetDisplayName + ChatColor.GREEN + "'s bits to " + 
                    ChatColor.YELLOW + String.format("%,d", amount) + ChatColor.GREEN + ".");
                if (targetPlayer != null) {
                    targetPlayer.sendMessage(ChatColor.YELLOW + "An admin has set your bits to " + 
                        ChatColor.AQUA + String.format("%,d", amount) + ChatColor.YELLOW + ".");
                }
            }
            case "give", "add" -> {
                long amount = parseAmount(sender, args[2]);
                if (amount < 0) {
                    return true;
                }
                bitsManager.addBits(targetId, amount);
                long newBalance = bitsManager.getBits(targetId);
                sender.sendMessage(ChatColor.GREEN + "Gave " + ChatColor.AQUA + targetDisplayName + ChatColor.GREEN + " " + 
                    ChatColor.YELLOW + String.format("%,d", amount) + ChatColor.GREEN + " bits.");
                sender.sendMessage(ChatColor.GRAY + "New balance: " + ChatColor.AQUA + String.format("%,d", newBalance));
                if (targetPlayer != null) {
                    targetPlayer.sendMessage(ChatColor.GREEN + "You received " + 
                        ChatColor.AQUA + String.format("%,d", amount) + ChatColor.GREEN + " bits!");
                    targetPlayer.sendMessage(ChatColor.GRAY + "New balance: " + ChatColor.AQUA + String.format("%,d", newBalance));
                }
            }
            case "take", "remove", "subtract" -> {
                long amount = parseAmount(sender, args[2]);
                if (amount < 0) {
                    return true;
                }
                long currentBalance = bitsManager.getBits(targetId);
                bitsManager.takeBits(targetId, amount);
                long newBalance = bitsManager.getBits(targetId);
                long actuallyTaken = currentBalance - newBalance;
                
                sender.sendMessage(ChatColor.GREEN + "Took " + ChatColor.AQUA + targetDisplayName + ChatColor.GREEN + " " + 
                    ChatColor.YELLOW + String.format("%,d", actuallyTaken) + ChatColor.GREEN + " bits.");
                sender.sendMessage(ChatColor.GRAY + "New balance: " + ChatColor.AQUA + String.format("%,d", newBalance));
                if (targetPlayer != null) {
                    targetPlayer.sendMessage(ChatColor.RED + "An admin took " + 
                        ChatColor.AQUA + String.format("%,d", actuallyTaken) + ChatColor.RED + " bits from you.");
                    targetPlayer.sendMessage(ChatColor.GRAY + "New balance: " + ChatColor.AQUA + String.format("%,d", newBalance));
                }
            }
            case "check", "get", "balance" -> {
                long balance = bitsManager.getBits(targetId);
                sender.sendMessage(ChatColor.AQUA + targetDisplayName + ChatColor.GRAY + " has " + 
                    ChatColor.YELLOW + String.format("%,d", balance) + ChatColor.GRAY + " bits.");
            }
            default -> sendUsage(sender);
        }

        return true;
    }

    private long parseAmount(CommandSender sender, String input) {
        try {
            long amount = Long.parseLong(input);
            if (amount < 0) {
                sender.sendMessage(ChatColor.RED + "Amount cannot be negative.");
                return -1;
            }
            return amount;
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid amount: " + input);
            return -1;
        }
    }

    private UUID getPlayerUUID(String nameOrUuid) {
        // Try to find online player first
        Player onlinePlayer = Bukkit.getPlayerExact(nameOrUuid);
        if (onlinePlayer != null) {
            return onlinePlayer.getUniqueId();
        }

        // Try to parse as UUID
        try {
            return UUID.fromString(nameOrUuid);
        } catch (IllegalArgumentException e) {
            // Not a UUID, continue to offline player lookup
        }

        // Try offline player lookup
        var offlinePlayer = Bukkit.getOfflinePlayer(nameOrUuid);
        if (offlinePlayer != null && offlinePlayer.getUniqueId() != null) {
            return offlinePlayer.getUniqueId();
        }

        return null;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Bits Admin Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/bitsadmin set <player> <amount>" + ChatColor.GRAY + " - Set player's bits");
        sender.sendMessage(ChatColor.YELLOW + "/bitsadmin give <player> <amount>" + ChatColor.GRAY + " - Give bits to player");
        sender.sendMessage(ChatColor.YELLOW + "/bitsadmin take <player> <amount>" + ChatColor.GRAY + " - Take bits from player");
        sender.sendMessage(ChatColor.YELLOW + "/bitsadmin check <player>" + ChatColor.GRAY + " - Check player's bits");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterPrefix(List.of("set", "give", "take", "check"), args[0]);
        }
        if (args.length == 2) {
            List<String> playerNames = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                playerNames.add(online.getName());
            }
            return filterPrefix(playerNames, args[1]);
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("give") || 
            args[0].equalsIgnoreCase("take") || args[0].equalsIgnoreCase("remove"))) {
            return filterPrefix(List.of("100", "500", "1000", "5000", "10000"), args[2]);
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
