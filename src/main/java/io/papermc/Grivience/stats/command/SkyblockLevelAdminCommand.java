package io.papermc.Grivience.stats.command;

import io.papermc.Grivience.stats.SkyblockLevelManager;
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

public final class SkyblockLevelAdminCommand implements CommandExecutor, TabCompleter {
    private final SkyblockLevelManager levelManager;

    public SkyblockLevelAdminCommand(SkyblockLevelManager levelManager) {
        this.levelManager = levelManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("grivience.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to manage Skyblock levels.");
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
                int level = parseLevel(sender, args[2]);
                if (level < 0) {
                    return true;
                }
                long xpForLevel = calculateXpForLevel(level);
                if (targetPlayer != null) {
                    levelManager.setXp(targetPlayer, xpForLevel);
                } else {
                    levelManager.setXp(targetId, xpForLevel);
                }
                sender.sendMessage(ChatColor.GREEN + "Set " + ChatColor.AQUA + targetDisplayName + ChatColor.GREEN + "'s Skyblock level to " + 
                    ChatColor.YELLOW + level + ChatColor.GREEN + " (" + ChatColor.AQUA + String.format("%,d", xpForLevel) + ChatColor.GREEN + " XP).");
                if (targetPlayer != null) {
                    targetPlayer.sendMessage(ChatColor.YELLOW + "An admin has set your Skyblock level to " + 
                        ChatColor.AQUA + level + ChatColor.YELLOW + ".");
                    targetPlayer.playSound(targetPlayer.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
                }
            }
            case "setxp" -> {
                long xp = parseXp(sender, args[2]);
                if (xp < 0) {
                    return true;
                }
                if (targetPlayer != null) {
                    levelManager.setXp(targetPlayer, xp);
                } else {
                    levelManager.setXp(targetId, xp);
                }
                int newLevel = targetPlayer != null ? levelManager.getLevel(targetPlayer) : levelManager.getLevel(targetId);
                sender.sendMessage(ChatColor.GREEN + "Set " + ChatColor.AQUA + targetDisplayName + ChatColor.GREEN + "'s Skyblock XP to " + 
                    ChatColor.YELLOW + String.format("%,d", xp) + ChatColor.GREEN + " (Level " + ChatColor.AQUA + newLevel + ChatColor.GREEN + ").");
                if (targetPlayer != null) {
                    targetPlayer.sendMessage(ChatColor.YELLOW + "An admin has set your Skyblock XP to " + 
                        ChatColor.AQUA + String.format("%,d", xp) + ChatColor.YELLOW + " (Level " + ChatColor.AQUA + newLevel + ChatColor.YELLOW + ").");
                    targetPlayer.playSound(targetPlayer.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
                }
            }
            case "give", "add" -> {
                long xp = parseXp(sender, args[2]);
                if (xp < 0) {
                    return true;
                }
                long oldXp = targetPlayer != null ? levelManager.getXp(targetPlayer) : levelManager.getXp(targetId);
                if (targetPlayer != null) {
                    levelManager.addXp(targetPlayer, xp);
                } else {
                    levelManager.addXp(targetId, xp);
                }
                long newXp = targetPlayer != null ? levelManager.getXp(targetPlayer) : levelManager.getXp(targetId);
                int newLevel = targetPlayer != null ? levelManager.getLevel(targetPlayer) : levelManager.getLevel(targetId);
                sender.sendMessage(ChatColor.GREEN + "Gave " + ChatColor.AQUA + targetDisplayName + ChatColor.GREEN + " " + 
                    ChatColor.YELLOW + String.format("%,d", xp) + ChatColor.GREEN + " Skyblock XP.");
                sender.sendMessage(ChatColor.GRAY + "Old: " + ChatColor.AQUA + String.format("%,d", oldXp) + 
                    ChatColor.GRAY + " → New: " + ChatColor.AQUA + String.format("%,d", newXp) + 
                    ChatColor.GRAY + " (Level " + ChatColor.AQUA + newLevel + ChatColor.GRAY + ")");
                if (targetPlayer != null) {
                    targetPlayer.sendMessage(ChatColor.GREEN + "You received " + 
                        ChatColor.AQUA + String.format("%,d", xp) + ChatColor.GREEN + " Skyblock XP!");
                    targetPlayer.playSound(targetPlayer.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
                }
            }
            case "take", "remove", "subtract" -> {
                long xp = parseXp(sender, args[2]);
                if (xp < 0) {
                    return true;
                }
                long oldXp = targetPlayer != null ? levelManager.getXp(targetPlayer) : levelManager.getXp(targetId);
                long currentXp = oldXp;
                long toRemove = Math.min(xp, currentXp);
                long updated = currentXp - toRemove;
                if (targetPlayer != null) {
                    levelManager.setXp(targetPlayer, updated);
                } else {
                    levelManager.setXp(targetId, updated);
                }
                long newXp = targetPlayer != null ? levelManager.getXp(targetPlayer) : levelManager.getXp(targetId);
                int newLevel = targetPlayer != null ? levelManager.getLevel(targetPlayer) : levelManager.getLevel(targetId);
                sender.sendMessage(ChatColor.GREEN + "Took " + ChatColor.AQUA + targetDisplayName + ChatColor.GREEN + " " + 
                    ChatColor.YELLOW + String.format("%,d", toRemove) + ChatColor.GREEN + " Skyblock XP.");
                sender.sendMessage(ChatColor.GRAY + "Old: " + ChatColor.AQUA + String.format("%,d", oldXp) + 
                    ChatColor.GRAY + " → New: " + ChatColor.AQUA + String.format("%,d", newXp) + 
                    ChatColor.GRAY + " (Level " + ChatColor.AQUA + newLevel + ChatColor.GRAY + ")");
                if (targetPlayer != null) {
                    targetPlayer.sendMessage(ChatColor.RED + "An admin took " + 
                        ChatColor.AQUA + String.format("%,d", toRemove) + ChatColor.RED + " Skyblock XP from you.");
                }
            }
            case "check", "get", "level", "xp" -> {
                long xp = targetPlayer != null ? levelManager.getXp(targetPlayer) : levelManager.getXp(targetId);
                int level = targetPlayer != null ? levelManager.getLevel(targetPlayer) : levelManager.getLevel(targetId);
                long xpInto = targetPlayer != null ? levelManager.xpIntoCurrentLevel(targetPlayer) : levelManager.xpIntoCurrentLevel(targetId);
                long xpToNext = targetPlayer != null ? levelManager.xpToNextLevel(targetPlayer) : levelManager.xpToNextLevel(targetId);
                long xpPerLevel = levelManager.getXpPerLevel();
                
                sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.AQUA + targetDisplayName + ChatColor.GOLD + "'s Skyblock Stats ===");
                sender.sendMessage(ChatColor.GRAY + "Level: " + ChatColor.AQUA + level);
                sender.sendMessage(ChatColor.GRAY + "Total XP: " + ChatColor.AQUA + String.format("%,d", xp));
                sender.sendMessage(ChatColor.GRAY + "Progress: " + ChatColor.AQUA + String.format("%,d", xpInto) + 
                    ChatColor.GRAY + "/" + ChatColor.AQUA + String.format("%,d", xpPerLevel) + 
                    ChatColor.GRAY + " (" + ChatColor.YELLOW + Math.round((xpInto * 100.0) / Math.max(1, xpPerLevel)) + "%" + ChatColor.GRAY + ")");
                sender.sendMessage(ChatColor.GRAY + "XP to Next Level: " + ChatColor.AQUA + String.format("%,d", xpToNext));
            }
            case "reset" -> {
                if (targetPlayer != null) {
                    levelManager.setXp(targetPlayer, 0L);
                } else {
                    levelManager.setXp(targetId, 0L);
                }
                sender.sendMessage(ChatColor.GREEN + "Reset " + ChatColor.AQUA + targetDisplayName + ChatColor.GREEN + "'s Skyblock level to 0.");
                if (targetPlayer != null) {
                    targetPlayer.sendMessage(ChatColor.RED + "An admin has reset your Skyblock level to 0.");
                }
            }
            default -> sendUsage(sender);
        }

        return true;
    }

    /**
     * Calculate the XP required to be at a specific level.
     */
    private long calculateXpForLevel(int level) {
        if (level <= 0) {
            return 0L;
        }
        long xpPerLevel = levelManager.getXpPerLevel();
        return (long) level * xpPerLevel;
    }

    private int parseLevel(CommandSender sender, String input) {
        try {
            int level = Integer.parseInt(input);
            if (level < 0) {
                sender.sendMessage(ChatColor.RED + "Level cannot be negative.");
                return -1;
            }
            int maxLevel = levelManager.getMaxLevel();
            if (maxLevel > 0 && level > maxLevel) {
                sender.sendMessage(ChatColor.YELLOW + "Warning: Level " + level + " exceeds max level (" + maxLevel + "). Setting to max level.");
                return maxLevel;
            }
            return level;
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid level: " + input);
            return -1;
        }
    }

    private long parseXp(CommandSender sender, String input) {
        try {
            long xp = Long.parseLong(input);
            if (xp < 0) {
                sender.sendMessage(ChatColor.RED + "XP cannot be negative.");
                return -1;
            }
            return xp;
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid XP amount: " + input);
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
        sender.sendMessage(ChatColor.GOLD + "=== Skyblock Level Admin Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/sbadmin set <player> <level>" + ChatColor.GRAY + " - Set player's Skyblock level");
        sender.sendMessage(ChatColor.YELLOW + "/sbadmin setxp <player> <xp>" + ChatColor.GRAY + " - Set player's Skyblock XP directly");
        sender.sendMessage(ChatColor.YELLOW + "/sbadmin give <player> <xp>" + ChatColor.GRAY + " - Give Skyblock XP to player");
        sender.sendMessage(ChatColor.YELLOW + "/sbadmin take <player> <xp>" + ChatColor.GRAY + " - Take Skyblock XP from player");
        sender.sendMessage(ChatColor.YELLOW + "/sbadmin check <player>" + ChatColor.GRAY + " - Check player's Skyblock level and XP");
        sender.sendMessage(ChatColor.YELLOW + "/sbadmin reset <player>" + ChatColor.GRAY + " - Reset player's Skyblock level to 0");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterPrefix(List.of("set", "setxp", "give", "take", "check", "reset"), args[0]);
        }
        if (args.length == 2) {
            List<String> playerNames = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                playerNames.add(online.getName());
            }
            return filterPrefix(playerNames, args[1]);
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("setxp") || 
            args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("take"))) {
            if (args[0].equalsIgnoreCase("set")) {
                return filterPrefix(List.of("1", "10", "50", "100", "200", "300", "400", "500", "521"), args[2]);
            } else {
                return filterPrefix(List.of("100", "500", "1000", "5000", "10000", "50000", "100000"), args[2]);
            }
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

