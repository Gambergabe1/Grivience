package io.papermc.Grivience.skyblock.command;

import io.papermc.Grivience.skyblock.island.Island;
import io.papermc.Grivience.skyblock.island.IslandManager;
import io.papermc.Grivience.party.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class IslandCommand implements CommandExecutor, TabCompleter {
    private final IslandManager islandManager;
    private final PartyManager partyManager;

    public IslandCommand(IslandManager islandManager, PartyManager partyManager) {
        this.islandManager = islandManager;
        this.partyManager = partyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        switch (subCommand) {
            case "go", "home", "visit" -> handleGo(player, args);
            case "create" -> handleCreate(player);
            case "expand", "upgrade" -> handleExpand(player, args);
            case "info" -> handleInfo(player);
            case "sethome" -> handleSetHome(player);
            case "kick" -> handleKick(player, args);
            case "leave" -> handleLeave(player);
            case "setname" -> handleSetName(player, args);
            case "setdesc" -> handleSetDesc(player, args);
            case "warp" -> handleWarp(player);
            case "help" -> sendHelp(player);
            default -> {
                player.sendMessage(ChatColor.RED + "Unknown command. Use /island help");
            }
        }
        return true;
    }

    private void handleGo(Player player, String[] args) {
        if (args.length >= 2) {
            String targetName = args[1];
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                player.sendMessage(ChatColor.RED + "Player not found: " + targetName);
                return;
            }

            Island targetIsland = islandManager.getIsland(target.getUniqueId());
            if (targetIsland == null) {
                player.sendMessage(ChatColor.RED + targetName + " does not have an island.");
                return;
            }

            if (targetIsland.getCenter() == null) {
                player.sendMessage(ChatColor.RED + "That island is not available.");
                return;
            }

            // Log the visit
            targetIsland.addVisit(player.getName());
            islandManager.saveIsland(targetIsland);

            player.teleport(targetIsland.getCenter().clone().add(0.5, 2, 0.5));
            player.sendMessage(ChatColor.GREEN + "Teleported to " + targetName + "'s island.");
            
            // Show visit count
            int totalVisits = targetIsland.getTotalVisits();
            long playerVisits = targetIsland.getVisitCount(player.getName());
            player.sendMessage(ChatColor.GRAY + "Total visits to this island: " + ChatColor.YELLOW + totalVisits);
            if (playerVisits > 1) {
                player.sendMessage(ChatColor.GRAY + "Your visits: " + ChatColor.AQUA + playerVisits);
            }
            return;
        }

        Island island = islandManager.getIsland(player.getUniqueId());
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You don't have an island. Use /island create to create one.");
            return;
        }

        if (island.getCenter() == null) {
            player.sendMessage(ChatColor.RED + "Your island location is not available.");
            return;
        }

        player.teleport(island.getCenter().clone().add(0.5, 2, 0.5));
        player.sendMessage(ChatColor.GREEN + "Teleported to your island.");
    }

    private void handleCreate(Player player) {
        if (islandManager.hasIsland(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You already have an island. Use /island go to visit it.");
            return;
        }

        Island island = islandManager.createIsland(player);
        if (island != null) {
            player.sendMessage(ChatColor.GREEN + "Island created successfully!");
            player.sendMessage(ChatColor.GRAY + "Use /island expand to upgrade your island size.");
        }
    }

    private void handleExpand(Player player, String[] args) {
        Island island = islandManager.getIsland(player.getUniqueId());
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You don't have an island. Use /island create to create one.");
            return;
        }

        if (args.length >= 2) {
            try {
                int targetLevel = Integer.parseInt(args[1]);
                islandManager.expandIsland(player, targetLevel);
                return;
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid level. Use a number.");
                return;
            }
        }

        int nextLevel = islandManager.getNextUpgradeLevel(island);
        int nextSize = islandManager.getNextUpgradeSize(island);
        double cost = islandManager.getUpgradeCost(nextLevel);

        if (nextLevel > islandManager.getUpgradeSizes().size()) {
            player.sendMessage(ChatColor.RED + "Your island is at maximum level.");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "=== Island Expansion ===");
        player.sendMessage(ChatColor.GRAY + "Current Level: " + ChatColor.AQUA + island.getUpgradeLevel());
        player.sendMessage(ChatColor.GRAY + "Current Size: " + ChatColor.AQUA + island.getSize() + "x" + island.getSize());
        player.sendMessage(ChatColor.GRAY + "Next Level: " + ChatColor.GREEN + nextLevel);
        player.sendMessage(ChatColor.GRAY + "Next Size: " + ChatColor.GREEN + nextSize + "x" + nextSize);
        player.sendMessage(ChatColor.GRAY + "Cost: " + ChatColor.RED + "$" + String.format("%.2f", cost));
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "Use /island expand " + nextLevel + " to upgrade.");
    }

    private void handleInfo(Player player) {
        Island island = islandManager.getIsland(player.getUniqueId());
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You don't have an island.");
            return;
        }

        OfflinePlayer owner = Bukkit.getOfflinePlayer(island.getOwner());
        int totalVisits = island.getTotalVisits();
        List<String> recentVisitors = island.getRecentVisitors(5);

        player.sendMessage(ChatColor.GOLD + "=== Island Info ===");
        player.sendMessage(ChatColor.GRAY + "Name: " + ChatColor.WHITE + island.getName());
        player.sendMessage(ChatColor.GRAY + "Owner: " + ChatColor.AQUA + owner.getName());
        player.sendMessage(ChatColor.GRAY + "Level: " + ChatColor.AQUA + island.getUpgradeLevel());
        player.sendMessage(ChatColor.GRAY + "Size: " + ChatColor.AQUA + island.getSize() + "x" + island.getSize());
        player.sendMessage(ChatColor.GRAY + "Created: " + ChatColor.YELLOW + formatDate(island.getCreatedAt()));
        player.sendMessage(ChatColor.GRAY + "Last Visited: " + ChatColor.YELLOW + formatDate(island.getLastVisited()));
        player.sendMessage(ChatColor.GRAY + "Total Visits: " + ChatColor.GREEN + "" + totalVisits);
        if (!recentVisitors.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "Recent Visitors: " + ChatColor.AQUA + String.join(", ", recentVisitors));
        }
        player.sendMessage(ChatColor.GRAY + "Description: " + ChatColor.ITALIC + island.getDescription());
    }

    private void handleSetHome(Player player) {
        Island island = islandManager.getIsland(player.getUniqueId());
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You don't have an island.");
            return;
        }

        island.setCenter(player.getLocation());
        islandManager.saveIsland(island);

        player.sendMessage(ChatColor.GREEN + "Island spawn point set to your current location.");
    }

    private void handleKick(Player player, String[] args) {
        player.sendMessage(ChatColor.RED + "Party system coming soon.");
    }

    private void handleLeave(Player player) {
        player.sendMessage(ChatColor.RED + "Island deletion coming soon. Use /island create to create a new one.");
    }

    private void handleWarp(Player player) {
        if (partyManager == null) {
            player.sendMessage(ChatColor.RED + "Party system is not available.");
            return;
        }

        String error = partyManager.warpPartyToIsland(player);
        if (error != null) {
            player.sendMessage(ChatColor.RED + error);
        }
    }

    private void handleSetName(Player player, String[] args) {
        Island island = islandManager.getIsland(player.getUniqueId());
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You don't have an island.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /island setname <name>");
            return;
        }

        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            nameBuilder.append(args[i]).append(" ");
        }
        String newName = nameBuilder.toString().trim();

        if (newName.length() > 32) {
            player.sendMessage(ChatColor.RED + "Name must be 32 characters or less.");
            return;
        }

        island.setName(newName);
        islandManager.saveIsland(island);

        player.sendMessage(ChatColor.GREEN + "Island name set to: " + ChatColor.AQUA + newName);
    }

    private void handleSetDesc(Player player, String[] args) {
        Island island = islandManager.getIsland(player.getUniqueId());
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You don't have an island.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /island setdesc <description>");
            return;
        }

        StringBuilder descBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            descBuilder.append(args[i]).append(" ");
        }
        String newDesc = descBuilder.toString().trim();

        if (newDesc.length() > 100) {
            player.sendMessage(ChatColor.RED + "Description must be 100 characters or less.");
            return;
        }

        island.setDescription(newDesc);
        islandManager.saveIsland(island);

        player.sendMessage(ChatColor.GREEN + "Island description updated.");
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Island Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/island create" + ChatColor.GRAY + " - Create your island");
        player.sendMessage(ChatColor.YELLOW + "/island go" + ChatColor.GRAY + " - Teleport to your island");
        player.sendMessage(ChatColor.YELLOW + "/island go <player>" + ChatColor.GRAY + " - Visit a player's island");
        player.sendMessage(ChatColor.YELLOW + "/island expand" + ChatColor.GRAY + " - View expansion options");
        player.sendMessage(ChatColor.YELLOW + "/island expand <level>" + ChatColor.GRAY + " - Expand island size");
        player.sendMessage(ChatColor.YELLOW + "/island info" + ChatColor.GRAY + " - View island information");
        player.sendMessage(ChatColor.YELLOW + "/island sethome" + ChatColor.GRAY + " - Set island spawn point");
        player.sendMessage(ChatColor.YELLOW + "/island setname <name>" + ChatColor.GRAY + " - Rename your island");
        player.sendMessage(ChatColor.YELLOW + "/island setdesc <desc>" + ChatColor.GRAY + " - Set island description");
        player.sendMessage(ChatColor.YELLOW + "/island warp" + ChatColor.GRAY + " - Warp party to your island (Leader)");
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
        return null;
    }

    private String formatDate(long timestamp) {
        long days = (System.currentTimeMillis() - timestamp) / (1000 * 60 * 60 * 24);
        if (days <= 0) {
            return "Today";
        } else if (days == 1) {
            return "Yesterday";
        } else if (days < 7) {
            return days + " days ago";
        } else if (days < 30) {
            return (days / 7) + " weeks ago";
        } else if (days < 365) {
            return (days / 30) + " months ago";
        } else {
            return (days / 365) + " years ago";
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> commands = new ArrayList<>(List.of(
                    "go", "create", "expand", "info", "sethome",
                    "setname", "setdesc", "leave", "warp", "help"
            ));
            return filterPrefix(commands, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("go")) {
            List<String> players = new ArrayList<>();
            UUID senderId = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (senderId == null || !online.getUniqueId().equals(senderId)) {
                    players.add(online.getName());
                }
            }
            return filterPrefix(players, args[1]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("expand")) {
            if (sender instanceof Player player) {
                Island island = islandManager.getIsland(player.getUniqueId());
                if (island != null) {
                    int nextLevel = islandManager.getNextUpgradeLevel(island);
                    int maxLevel = islandManager.getUpgradeSizes().size();
                    List<String> levels = new ArrayList<>();
                    for (int i = nextLevel; i <= maxLevel; i++) {
                        levels.add(String.valueOf(i));
                    }
                    return filterPrefix(levels, args[1]);
                }
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
