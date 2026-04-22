package io.papermc.Grivience.storage;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Command handler for storage system.
 * Provides player and admin commands for storage management.
 */
public class StorageCommand implements CommandExecutor, TabCompleter {
    private final StorageManager storageManager;
    private final StorageGui storageGui;

    public StorageCommand(StorageManager storageManager, StorageGui storageGui) {
        this.storageManager = storageManager;
        this.storageGui = storageGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        if (args.length == 0) {
            openStorageMenu(player);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);

        switch (subCommand) {
            case "open" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /storage open <type>");
                    player.sendMessage("§7Types: §e" + availableTypes(player));
                    return true;
                }
                openStorage(player, args[1]);
            }
            case "upgrade" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /storage upgrade <type>");
                    return true;
                }
                upgradeStorage(player, args[1]);
            }
            case "status" -> showStatus(player);
            case "rename" -> {
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /storage rename <type> <name>");
                    return true;
                }
                renameStorage(player, args[1], String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
            }
            case "lock" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /storage lock <type>");
                    return true;
                }
                lockStorage(player, args[1], true);
            }
            case "unlock" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /storage unlock <type>");
                    return true;
                }
                lockStorage(player, args[1], false);
            }
            case "clear" -> {
                if (!player.hasPermission("grivience.storage.admin")) {
                    player.sendMessage("§cYou don't have permission to clear storage!");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /storage clear <type>");
                    return true;
                }
                clearStorage(player, args[1]);
            }
            case "top" -> showLeaderboard(player);
            case "rank" -> showRank(player);
            case "help" -> showHelp(player);
            default -> {
                player.sendMessage("§cUnknown command: " + subCommand);
                showHelp(player);
            }
        }

        return true;
    }

    /**
     * Open the main storage menu.
     */
    private void openStorageMenu(Player player) {
        storageGui.openMainMenu(player);
    }

    /**
     * Open a specific storage type.
     */
    private void openStorage(Player player, String typeId) {
        StorageType type = parseStorageType(typeId);
        if (type == null) {
            player.sendMessage("§cUnknown storage type: " + typeId);
            player.sendMessage("§7Available types: §e" + availableTypes(player));
            return;
        }

        if (!player.hasPermission(type.getPermissionNode())) {
            player.sendMessage("§cYou don't have permission to access " + type.getDisplayName() + "!");
            return;
        }

        StorageProfile profile = storageManager.getStorage(player, type);
        if (profile == null) {
            player.sendMessage("§cFailed to open storage. Please try again.");
            return;
        }

        if (profile.isLocked()) {
            player.sendMessage("§cThis storage is locked!");
            return;
        }

        storageGui.openStorage(player, type);
    }

    /**
     * Upgrade a storage type.
     */
    private void upgradeStorage(Player player, String typeId) {
        StorageType type = parseStorageType(typeId);
        if (type == null) {
            player.sendMessage("§cUnknown storage type: " + typeId);
            return;
        }

        StorageProfile profile = storageManager.getStorage(player, type);
        if (profile == null) {
            player.sendMessage("§cFailed to access storage profile.");
            return;
        }

        if (!storageManager.canUpgrade(player, profile)) {
            StorageUpgrade nextUpgrade = storageManager.getNextUpgrade(profile);
            if (nextUpgrade == null) {
                player.sendMessage("§e" + type.getDisplayName() + " §cis already maxed out!");
            } else if (profile.getCurrentSlots() >= type.getMaxSlots()) {
                player.sendMessage("§e" + type.getDisplayName() + " §cis already at maximum capacity!");
            } else {
                player.sendMessage("§cYou cannot upgrade this storage yet.");
            }
            return;
        }

        if (!storageManager.upgradeStorage(player, profile)) {
            player.sendMessage("§cFailed to upgrade storage.");
        }
    }

    /**
     * Show storage status.
     */
    private void showStatus(Player player) {
        player.sendMessage("");
        player.sendMessage("§6§l=== STORAGE STATUS ===");
        player.sendMessage("");

        for (StorageType type : StorageType.values()) {
            if (!player.hasPermission(type.getPermissionNode())) {
                continue;
            }

            StorageProfile profile = storageManager.getStorage(player, type);
            if (profile != null) {
                player.sendMessage("§e" + type.getDisplayName() + ":");
                player.sendMessage("  " + profile.getStatus());
                if (profile.getCustomName() != null) {
                    player.sendMessage("  §7Name: §f" + profile.getCustomName());
                }
                if (profile.isLocked()) {
                    player.sendMessage("  §cLocked");
                }
                player.sendMessage("");
            }
        }

        int totalItems = storageManager.getTotalItemsStored(player);
        int totalCapacity = storageManager.getTotalStorageCapacity(player);
        double percentage = storageManager.getStorageUsagePercentage(player);

        player.sendMessage("§7Total Storage: §e" + totalItems + "§7/§e" + totalCapacity + " §7(§e" + String.format("%.1f", percentage) + "%§7)");
        player.sendMessage("");
    }

    /**
     * Rename a storage type.
     */
    private void renameStorage(Player player, String typeId, String name) {
        StorageType type = parseStorageType(typeId);
        if (type == null) {
            player.sendMessage("§cUnknown storage type: " + typeId);
            return;
        }

        if (!player.hasPermission(type.getPermissionNode())) {
            player.sendMessage("§cYou don't have permission to access " + type.getDisplayName() + "!");
            return;
        }

        storageManager.setStorageName(player, type, name);
    }

    /**
     * Lock or unlock a storage type.
     */
    private void lockStorage(Player player, String typeId, boolean lock) {
        StorageType type = parseStorageType(typeId);
        if (type == null) {
            player.sendMessage("§cUnknown storage type: " + typeId);
            return;
        }

        if (!player.hasPermission(type.getPermissionNode())) {
            player.sendMessage("§cYou don't have permission to access " + type.getDisplayName() + "!");
            return;
        }

        UUID playerId = player.getUniqueId();
        UUID profileId = playerId;
        var profile = storageManager.getSelectedSkyBlockProfile(player);
        if (profile != null) {
            profileId = profile.getProfileId();
        }
        if (lock) {
            storageManager.lockStorage(playerId, profileId, type);
            player.sendMessage("§a" + type.getDisplayName() + " §ehas been §clocked§e.");
        } else {
            storageManager.unlockStorage(playerId, profileId, type);
            player.sendMessage("§a" + type.getDisplayName() + " §ehas been §aunlocked§e.");
        }
    }

    /**
     * Clear a storage type (admin only).
     */
    private void clearStorage(Player player, String typeId) {
        StorageType type = parseStorageType(typeId);
        if (type == null) {
            player.sendMessage("§cUnknown storage type: " + typeId);
            return;
        }

        storageManager.clearStorage(player, type);
        player.sendMessage("§a" + type.getDisplayName() + " §ehas been cleared.");
    }

    /**
     * Show storage leaderboard.
     */
    private void showLeaderboard(Player player) {
        player.sendMessage("");
        player.sendMessage("§6§l=== STORAGE LEADERBOARD ===");
        player.sendMessage("");

        List<Map.Entry<UUID, Integer>> leaderboard = storageManager.getLeaderboard(10);
        int rank = 1;

        for (Map.Entry<UUID, Integer> entry : leaderboard) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.getKey());
            String playerName = offlinePlayer.getName();
            int items = entry.getValue();

            if (player.getUniqueId().equals(entry.getKey())) {
                player.sendMessage("§e§l#" + rank + " §f" + playerName + " §7- §e" + items + " items");
            } else {
                player.sendMessage("§6#" + rank + " §f" + playerName + " §7- §e" + items + " items");
            }

            rank++;
        }

        player.sendMessage("");
    }

    /**
     * Show player's rank.
     */
    private void showRank(Player player) {
        int rank = storageManager.getPlayerRank(player.getUniqueId());
        int totalItems = storageManager.getTotalItemsStored(player);

        if (rank == -1) {
            player.sendMessage("§cYou are not ranked yet.");
        } else {
            player.sendMessage("§6§l=== YOUR STORAGE RANK ===");
            player.sendMessage("");
            player.sendMessage("§eRank: §6#" + rank);
            player.sendMessage("§eTotal Items: §6" + totalItems);
            player.sendMessage("");
        }
    }

    /**
     * Show help message.
     */
    private void showHelp(Player player) {
        player.sendMessage("");
        player.sendMessage("§6§l=== STORAGE COMMANDS ===");
        player.sendMessage("");
        player.sendMessage("§e/storage §7- Open storage menu");
        player.sendMessage("§e/storage open <type> §7- Open specific storage");
        player.sendMessage("§e/storage upgrade <type> §7- Upgrade storage capacity");
        player.sendMessage("§e/storage status §7- View storage status");
        player.sendMessage("§e/storage rename <type> <name> §7- Rename storage");
        player.sendMessage("§e/storage lock <type> §7- Lock storage");
        player.sendMessage("§e/storage unlock <type> §7- Unlock storage");
        player.sendMessage("§e/storage top §7- View leaderboard");
        player.sendMessage("§e/storage rank §7- View your rank");
        player.sendMessage("§e/storage help §7- Show this help");
        player.sendMessage("");
    }

    private StorageType parseStorageType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "ACCESSORY", "ACCESSORYBAG", "ACCESSORY_BAG", "ACCESSORIES" -> StorageType.ACCESSORY_BAG;
            case "POTION", "POTIONBAG", "POTION_BAG" -> StorageType.POTION_BAG;
            default -> {
                try {
                    yield StorageType.valueOf(normalized);
                } catch (IllegalArgumentException ignored) {
                    yield null;
                }
            }
        };
    }

    private String availableTypes(Player player) {
        List<String> values = new ArrayList<>();
        for (StorageType type : StorageType.values()) {
            if (player.hasPermission(type.getPermissionNode())) {
                values.add(type.name().toLowerCase(Locale.ROOT));
            }
        }
        return String.join(", ", values);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String partial = args[0].toLowerCase(Locale.ROOT);

            List<String> commands = Arrays.asList("open", "upgrade", "status", "rename", "lock", "unlock", "clear", "top", "rank", "help");
            for (String cmd : commands) {
                if (cmd.startsWith(partial)) {
                    completions.add(cmd);
                }
            }

            return completions;
        }

        if (args.length == 2) {
            if (Arrays.asList("open", "upgrade", "lock", "unlock", "clear", "rename").contains(args[0].toLowerCase(Locale.ROOT))) {
                List<String> completions = new ArrayList<>();
                String partial = args[1].toLowerCase(Locale.ROOT);

                for (StorageType type : StorageType.values()) {
                    if (player.hasPermission(type.getPermissionNode()) && type.name().toLowerCase(Locale.ROOT).startsWith(partial)) {
                        completions.add(type.name().toLowerCase(Locale.ROOT));
                    }
                }

                return completions;
            }
        }

        return new ArrayList<>();
    }
}
