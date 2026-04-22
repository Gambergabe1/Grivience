package io.papermc.Grivience.collections;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skyblock.profile.ProfileManager;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Command handler for collections system.
 *
 * Commands:
 * - /collection - Open collections menu
 * - /collection <player> - View another player's collections
 * - /collection search <query> - Search for a collection
 * - /collection top <collection> - View leaderboard
 * - /collections - Alias for /collection
 *
 * Admin Commands:
 * - /collection admin reload - Reload collections config
 * - /collection admin set <player> <collection> <amount> - Set collection progress
 * - /collection admin reset <player> <collection> - Reset collection progress
 * - /collection admin list - List all collections
 */
public class CollectionCommand implements CommandExecutor, TabCompleter {
    private static final int SEARCH_RESULT_LIMIT = 10;

    private final GriviencePlugin plugin;
    private final CollectionsManager collectionsManager;
    private final CollectionGUI collectionGui;

    public CollectionCommand(GriviencePlugin plugin, CollectionsManager collectionsManager, CollectionGUI collectionGui) {
        this.plugin = plugin;
        this.collectionsManager = collectionsManager;
        this.collectionGui = collectionGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                return true;
            }
            if (!ensureCollectionsEnabled(sender)) {
                return true;
            }

            collectionGui.openMainGui(player);
            return true;
        }

        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("search") || args[0].equalsIgnoreCase("top")) {
                sender.sendMessage(ChatColor.RED + "Usage: /" + label + " " + args[0] + " <query>");
                return true;
            }

            if (args[0].equalsIgnoreCase("admin")) {
                if (!sender.hasPermission("grivience.collections.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to use admin commands.");
                    return true;
                }
                sendAdminHelp(sender, label);
                return true;
            }

            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                return true;
            }
            if (!ensureCollectionsEnabled(sender)) {
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found or not online.");
                return true;
            }

            collectionGui.openMainGui(player, target);
            if (!player.getUniqueId().equals(target.getUniqueId())) {
                player.sendMessage(ChatColor.YELLOW + "Viewing " + ChatColor.GOLD + target.getName() + ChatColor.YELLOW + "'s collections.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("search")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                return true;
            }
            if (!ensureCollectionsEnabled(sender)) {
                return true;
            }

            String query = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            CollectionDefinition bestMatch = collectionsManager.findBestCollectionMatch(query);
            if (bestMatch != null) {
                collectionGui.openCollectionDetailsGui(player, bestMatch);
                return true;
            }

            List<CollectionDefinition> results = collectionsManager.searchCollections(query);
            if (results.isEmpty()) {
                player.sendMessage(ChatColor.RED + "No collections found matching '" + query + "'.");
                return true;
            }

            player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Found " + results.size() + " collection(s):");
            int shown = 0;
            for (CollectionDefinition result : results) {
                if (shown >= SEARCH_RESULT_LIMIT) {
                    break;
                }
                player.sendMessage(formatSearchResult(result));
                shown++;
            }
            if (results.size() > shown) {
                player.sendMessage(ChatColor.GRAY + "...and " + (results.size() - shown) + " more.");
            }
            player.sendMessage(ChatColor.YELLOW + "Use a more specific query to open a collection directly.");
            return true;
        }

        if (args[0].equalsIgnoreCase("top")) {
            if (!ensureCollectionsEnabled(sender)) {
                return true;
            }

            String query = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            CollectionDefinition collection = collectionsManager.findBestCollectionMatch(query);
            if (collection == null) {
                sender.sendMessage(ChatColor.RED + "Collection not found.");
                return true;
            }

            sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + collection.getName() + ChatColor.GOLD + " Collection Leaderboard");
            List<java.util.Map.Entry<UUID, Long>> leaderboard = collectionsManager.getLeaderboard(collection.getId(), 10);
            if (leaderboard.isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + "No data yet.");
                return true;
            }

            int rank = 1;
            for (java.util.Map.Entry<UUID, Long> entry : leaderboard) {
                UUID profileId = entry.getKey();
                long amount = entry.getValue();
                String name = resolveLeaderboardName(profileId);
                sender.sendMessage(ChatColor.YELLOW + "#" + rank + " " + ChatColor.WHITE + name + ChatColor.GRAY + " - " + ChatColor.GREEN + formatNumber(amount));
                rank++;
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("admin")) {
            if (!sender.hasPermission("grivience.collections.admin")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use admin commands.");
                return true;
            }

            handleAdminCommand(sender, label, args);
            return true;
        }

        sendHelp(sender, label);
        return true;
    }

    private void handleAdminCommand(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sendAdminHelp(sender, label);
            return;
        }

        String subCommand = args[1].toLowerCase(Locale.ROOT);
        switch (subCommand) {
            case "reload" -> {
                collectionsManager.reload();
                sender.sendMessage(ChatColor.GREEN + "Collections reloaded.");
            }
            case "set" -> {
                if (args.length < 5) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " admin set <player> <collection> <amount>");
                    return;
                }

                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found or not online.");
                    return;
                }

                CollectionDefinition collection = collectionsManager.findBestCollectionMatch(args[3]);
                if (collection == null) {
                    sender.sendMessage(ChatColor.RED + "Collection not found.");
                    return;
                }

                long amount;
                try {
                    amount = Long.parseLong(args[4]);
                } catch (NumberFormatException exception) {
                    sender.sendMessage(ChatColor.RED + "Invalid amount.");
                    return;
                }

                collectionsManager.setCollection(target, collection.getId(), amount);
                sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s " + CollectionTextUtil.plainText(collection.getName()) + " progress to " + amount + ".");
            }
            case "reset" -> {
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " admin reset <player> <collection>");
                    return;
                }

                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found or not online.");
                    return;
                }

                CollectionDefinition collection = collectionsManager.findBestCollectionMatch(args[3]);
                if (collection == null) {
                    sender.sendMessage(ChatColor.RED + "Collection not found.");
                    return;
                }

                collectionsManager.setCollection(target, collection.getId(), 0);
                sender.sendMessage(ChatColor.GREEN + "Reset " + target.getName() + "'s " + CollectionTextUtil.plainText(collection.getName()) + " progress.");
            }
            case "list" -> {
                sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "All Collections (" + collectionsManager.getCollections().size() + "):");
                for (CollectionCategory category : CollectionCategory.values()) {
                    List<CollectionDefinition> categoryCollections = collectionsManager.getCollectionsByCategory(category);
                    if (categoryCollections.isEmpty()) {
                        continue;
                    }

                    sender.sendMessage(ChatColor.YELLOW + CollectionTextUtil.plainText(category.getDisplayName()) + ":");
                    for (CollectionDefinition collection : categoryCollections) {
                        ChatColor status = collection.isEnabled() ? ChatColor.GREEN : ChatColor.RED;
                        sender.sendMessage("  " + status + CollectionTextUtil.plainText(collection.getName()) + ChatColor.GRAY + " (" + collection.getId() + ")");
                    }
                }
            }
            default -> {
                sender.sendMessage(ChatColor.RED + "Unknown admin command: " + subCommand);
                sendAdminHelp(sender, label);
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase(Locale.ROOT);
            addIfMatches(completions, "search", partial);
            addIfMatches(completions, "top", partial);
            if (sender.hasPermission("grivience.collections.admin")) {
                addIfMatches(completions, "admin", partial);
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                addIfMatches(completions, player.getName(), partial);
            }
            return completions;
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("admin") && sender.hasPermission("grivience.collections.admin")) {
                String partial = args[1].toLowerCase(Locale.ROOT);
                addIfMatches(completions, "reload", partial);
                addIfMatches(completions, "set", partial);
                addIfMatches(completions, "reset", partial);
                addIfMatches(completions, "list", partial);
            } else if (args[0].equalsIgnoreCase("top")) {
                addCollectionCompletions(completions, args[1]);
            }
            return completions;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("admin")) {
            if (args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("reset")) {
                String partial = args[2].toLowerCase(Locale.ROOT);
                for (Player player : Bukkit.getOnlinePlayers()) {
                    addIfMatches(completions, player.getName(), partial);
                }
            }
            return completions;
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("admin")) {
            if (args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("reset")) {
                addCollectionCompletions(completions, args[3]);
            }
        }

        return completions;
    }

    private boolean ensureCollectionsEnabled(CommandSender sender) {
        if (collectionsManager.isEnabled()) {
            return true;
        }
        sender.sendMessage(ChatColor.RED + "The collections system is currently disabled.");
        return false;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Collections Commands:");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + ChatColor.GRAY + " - Open collections menu");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " search <query>" + ChatColor.GRAY + " - Search for a collection");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " top <collection>" + ChatColor.GRAY + " - View leaderboard");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " <player>" + ChatColor.GRAY + " - View a player's collections");
        if (sender.hasPermission("grivience.collections.admin")) {
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " admin" + ChatColor.GRAY + " - Admin commands");
        }
    }

    private void sendAdminHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Collection Admin Commands:");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " admin reload" + ChatColor.GRAY + " - Reload collections config");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " admin set <player> <collection> <amount>" + ChatColor.GRAY + " - Set progress");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " admin reset <player> <collection>" + ChatColor.GRAY + " - Reset progress");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " admin list" + ChatColor.GRAY + " - List all collections");
    }

    private String formatSearchResult(CollectionDefinition collection) {
        StringBuilder line = new StringBuilder();
        line.append(ChatColor.GOLD).append(collection.getName());
        line.append(ChatColor.GRAY).append(" - ").append(collection.getCategory().getDisplayName());
        if (!collection.getSubcategory().isBlank()) {
            line.append(ChatColor.DARK_GRAY).append(" / ").append(ChatColor.YELLOW).append(collection.getSubcategory());
        }
        line.append(ChatColor.DARK_GRAY).append(" (").append(collection.getId()).append(")");
        return line.toString();
    }

    private void addCollectionCompletions(List<String> completions, String partial) {
        String normalized = partial == null ? "" : partial.toLowerCase(Locale.ROOT);
        for (CollectionDefinition collection : collectionsManager.getEnabledCollections()) {
            addIfMatches(completions, collection.getId(), normalized);
        }
    }

    private void addIfMatches(List<String> completions, String value, String partial) {
        if (value == null) {
            return;
        }
        String normalized = partial == null ? "" : partial.toLowerCase(Locale.ROOT);
        if (value.toLowerCase(Locale.ROOT).startsWith(normalized)) {
            completions.add(value);
        }
    }

    private String resolveLeaderboardName(UUID profileId) {
        if (profileId == null) {
            return "Unknown";
        }

        ProfileManager profileManager = plugin == null ? null : plugin.getProfileManager();
        if (profileManager != null) {
            SkyBlockProfile profile = profileManager.getProfile(profileId);
            if (profile != null) {
                String ownerName = Bukkit.getOfflinePlayer(profile.getOwnerId()).getName();
                if (ownerName == null || ownerName.isBlank()) {
                    ownerName = profile.getOwnerId().toString().substring(0, 8);
                }
                return ownerName + " (" + profile.getProfileName() + ")";
            }
        }

        String name = Bukkit.getOfflinePlayer(profileId).getName();
        if (name != null && !name.isBlank()) {
            return name;
        }
        return profileId.toString().substring(0, 8);
    }

    private String formatNumber(long amount) {
        if (amount >= 1_000_000_000L) {
            return String.format("%.1fB", amount / 1_000_000_000.0);
        }
        if (amount >= 1_000_000L) {
            return String.format("%.1fM", amount / 1_000_000.0);
        }
        if (amount >= 1_000L) {
            return String.format("%.1fK", amount / 1_000.0);
        }
        return Long.toString(amount);
    }
}
