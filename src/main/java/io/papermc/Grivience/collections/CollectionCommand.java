package io.papermc.Grivience.collections;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skyblock.profile.ProfileManager;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
            // /collection
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cThis command can only be used by players!");
                return true;
            }
            
            if (!collectionsManager.isEnabled()) {
                sender.sendMessage("§cThe collections system is currently disabled!");
                return true;
            }
            
            collectionGui.openMainGui(player);
            return true;
        }

        if (args.length == 1) {
            // /collection <player> - View another player's collections
            if (args[0].equalsIgnoreCase("search") || args[0].equalsIgnoreCase("top")) {
                sender.sendMessage("§cUsage: /" + label + " " + args[0] + " <query/collection>");
                return true;
            }
            
            if (args[0].equalsIgnoreCase("admin")) {
                if (!sender.hasPermission("grivience.collections.admin")) {
                    sender.sendMessage("§cYou don't have permission to use admin commands!");
                    return true;
                }
                sender.sendMessage("§6§lCollection Admin Commands:");
                sender.sendMessage("§e/" + label + " admin reload §7- Reload collections config");
                sender.sendMessage("§e/" + label + " admin set <player> <collection> <amount> §7- Set progress");
                sender.sendMessage("§e/" + label + " admin reset <player> <collection> §7- Reset progress");
                sender.sendMessage("§e/" + label + " admin list §7- List all collections");
                return true;
            }
            
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cThis command can only be used by players!");
                return true;
            }
            
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found!");
                return true;
            }
            
            // View own collections for now (multi-player viewing could be expanded)
            collectionGui.openMainGui(player);
            return true;
        }

        if (args.length >= 2) {
            if (args[0].equalsIgnoreCase("search")) {
                // /collection search <query>
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cThis command can only be used by players!");
                    return true;
                }
                
                String query = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).toLowerCase();
                List<CollectionDefinition> results = new ArrayList<>();
                
                for (CollectionDefinition collection : collectionsManager.getEnabledCollections()) {
                    if (collection.getName().toLowerCase().contains(query) ||
                        collection.getId().toLowerCase().contains(query)) {
                        results.add(collection);
                    }
                }
                
                if (results.isEmpty()) {
                    player.sendMessage("§cNo collections found matching '" + query + "'");
                } else {
                    player.sendMessage("§a§lFound " + results.size() + " collection(s):");
                    for (CollectionDefinition collection : results) {
                        player.sendMessage("  §6" + collection.getName() + " §7- " + collection.getCategory().getDisplayName());
                    }
                }
                return true;
            }
            
            if (args[0].equalsIgnoreCase("top")) {
                // /collection top <collection>
                String collectionId = String.join("_", Arrays.copyOfRange(args, 1, args.length)).toLowerCase();
                CollectionDefinition collection = collectionsManager.getCollection(collectionId);
                
                if (collection == null) {
                    // Try to find by name
                    for (CollectionDefinition def : collectionsManager.getEnabledCollections()) {
                        if (def.getName().toLowerCase().contains(collectionId)) {
                            collection = def;
                            break;
                        }
                    }
                }
                
                if (collection == null) {
                    sender.sendMessage("§cCollection not found!");
                    return true;
                }
                
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cThis command can only be used by players!");
                    return true;
                }

                sender.sendMessage("§6§l" + collection.getName() + " Collection Leaderboard");
                List<java.util.Map.Entry<UUID, Long>> leaderboard = collectionsManager.getLeaderboard(collection.getId(), 10);
                if (leaderboard.isEmpty()) {
                    sender.sendMessage("§7No data yet.");
                    return true;
                }

                int rank = 1;
                for (java.util.Map.Entry<UUID, Long> entry : leaderboard) {
                    UUID profileId = entry.getKey();
                    long amount = entry.getValue();
                    String name = resolveLeaderboardName(profileId);
                    sender.sendMessage("§e#" + rank + " §f" + name + " §7- §a" + formatNumber(amount));
                    rank++;
                }
                return true;
            }
            
            if (args[0].equalsIgnoreCase("admin")) {
                if (!sender.hasPermission("grivience.collections.admin")) {
                    sender.sendMessage("§cYou don't have permission to use admin commands!");
                    return true;
                }
                
                handleAdminCommand(sender, args);
                return true;
            }
        }

        sender.sendMessage("§6§lCollections Commands:");
        sender.sendMessage("§e/" + label + " §7- Open collections menu");
        sender.sendMessage("§e/" + label + " search <query> §7- Search for a collection");
        sender.sendMessage("§e/" + label + " top <collection> §7- View leaderboard");
        sender.sendMessage("§e/" + label + " <player> §7- View player's collections");
        
        if (sender.hasPermission("grivience.collections.admin")) {
            sender.sendMessage("§e/" + label + " admin §7- Admin commands");
        }
        
        return true;
    }

    private void handleAdminCommand(CommandSender sender, String[] args) {
        if (args.length < 2) return;
        
        String subCommand = args[1].toLowerCase();
        
        switch (subCommand) {
            case "reload" -> {
                collectionsManager.reload();
                sender.sendMessage("§aCollections reloaded!");
            }
            
            case "set" -> {
                if (args.length < 5) {
                    sender.sendMessage("§cUsage: /collection admin set <player> <collection> <amount>");
                    return;
                }
                
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage("§cPlayer not found!");
                    return;
                }
                
                String collectionId = args[3].toLowerCase();
                CollectionDefinition collection = collectionsManager.getCollection(collectionId);
                
                if (collection == null) {
                    sender.sendMessage("§cCollection not found!");
                    return;
                }
                
                long amount;
                try {
                    amount = Long.parseLong(args[4]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid amount!");
                    return;
                }
                
                collectionsManager.setCollection(target, collection.getId(), amount);
                sender.sendMessage("§aSet " + target.getName() + "'s " + collection.getName() + " to " + amount);
            }
            
            case "reset" -> {
                if (args.length < 4) {
                    sender.sendMessage("§cUsage: /collection admin reset <player> <collection>");
                    return;
                }
                
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage("§cPlayer not found!");
                    return;
                }
                
                String collectionId = args[3].toLowerCase();
                CollectionDefinition collection = collectionsManager.getCollection(collectionId);
                
                if (collection == null) {
                    sender.sendMessage("§cCollection not found!");
                    return;
                }
                
                collectionsManager.setCollection(target, collection.getId(), 0);
                sender.sendMessage("§aReset " + target.getName() + "'s " + collection.getName() + " progress");
            }
            
            case "list" -> {
                sender.sendMessage("§6§lAll Collections (" + collectionsManager.getCollections().size() + "):");
                
                for (CollectionCategory category : CollectionCategory.values()) {
                    List<CollectionDefinition> categoryCollections = collectionsManager.getCollectionsByCategory(category);
                    if (!categoryCollections.isEmpty()) {
                        sender.sendMessage("§e" + category.getDisplayName() + ":");
                        for (CollectionDefinition collection : categoryCollections) {
                            String status = collection.isEnabled() ? "§a" : "§c";
                            sender.sendMessage("  " + status + collection.getName() + " §7(" + collection.getId() + ")");
                        }
                    }
                }
            }
            
            default -> {
                sender.sendMessage("§cUnknown admin command: " + subCommand);
                sender.sendMessage("§eUsage: /collection admin <reload|set|reset|list>");
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            
            if ("search".startsWith(partial)) completions.add("search");
            if ("top".startsWith(partial)) completions.add("top");
            if ("admin".startsWith(partial) && sender.hasPermission("grivience.collections.admin")) {
                completions.add("admin");
            }
            
            // Player names
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partial)) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("admin") && sender.hasPermission("grivience.collections.admin")) {
                String partial = args[1].toLowerCase();
                
                if ("reload".startsWith(partial)) completions.add("reload");
                if ("set".startsWith(partial)) completions.add("set");
                if ("reset".startsWith(partial)) completions.add("reset");
                if ("list".startsWith(partial)) completions.add("list");
            }
            
            if (args[0].equalsIgnoreCase("top")) {
                String partial = args[1].toLowerCase();
                for (CollectionDefinition collection : collectionsManager.getEnabledCollections()) {
                    if (collection.getId().toLowerCase().startsWith(partial) ||
                        collection.getName().toLowerCase().startsWith(partial)) {
                        completions.add(collection.getId().replace("_", " "));
                    }
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("admin")) {
            if (args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("reset")) {
                String partial = args[2].toLowerCase();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(partial)) {
                        completions.add(player.getName());
                    }
                }
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("set")) {
            String partial = args[3].toLowerCase();
            for (CollectionDefinition collection : collectionsManager.getEnabledCollections()) {
                if (collection.getId().toLowerCase().startsWith(partial)) {
                    completions.add(collection.getId());
                }
            }
        }
        
        return completions;
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
