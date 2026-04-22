package io.papermc.Grivience.skyblock.economy.command;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.bank.BankManager;
import io.papermc.Grivience.skyblock.profile.ProfileManager;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
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

/**
 * Admin commands for adjusting Skyblock profile-scoped coin balances.
 * <p>
 * Purse balances are stored per selected profile. Bank balances resolve to the shared coop profile when applicable.
 */
public final class CoinsAdminCommand implements CommandExecutor, TabCompleter {
    private enum BalanceType {
        PURSE("Purse"),
        BANK("Bank");

        private final String displayName;

        BalanceType(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    private final GriviencePlugin plugin;
    private final BankManager bankManager;

    public CoinsAdminCommand(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.bankManager = new BankManager(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("grivience.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        String root = args[0].toLowerCase(Locale.ROOT);
        switch (root) {
            case "purse", "coins" -> handleBalanceCommand(sender, label, args, BalanceType.PURSE);
            case "bank" -> handleBalanceCommand(sender, label, args, BalanceType.BANK);
            case "balance", "bal" -> handleShowBalance(sender, label, args);
            case "help" -> sendUsage(sender, label);
            default -> sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /" + label + " help.");
        }

        return true;
    }

    private void handleBalanceCommand(CommandSender sender, String label, String[] args, BalanceType balanceType) {
        if (args.length < 3) {
            sendUsage(sender, label);
            return;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        String targetName = args[2];

        UUID targetId = resolvePlayerUUID(targetName);
        if (targetId == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + targetName);
            return;
        }

        ProfileManager profileManager = plugin.getProfileManager();
        if (profileManager == null) {
            sender.sendMessage(ChatColor.RED + "Profile system is not available.");
            return;
        }

        SkyBlockProfile profile = profileManager.getSelectedProfile(targetId);
        if (profile == null) {
            sender.sendMessage(ChatColor.RED + "That player does not have a Skyblock profile.");
            return;
        }

        Player targetPlayer = Bukkit.getPlayer(targetId);
        String targetDisplayName = targetPlayer != null ? targetPlayer.getName() : targetName;

        if (action.equals("check") || action.equals("get") || action.equals("bal") || action.equals("balance")) {
            sendSingleBalance(sender, targetDisplayName, resolveBalanceProfile(profileManager, profile, balanceType), balanceType);
            return;
        }

        if (args.length < 4) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " " + balanceType.name().toLowerCase(Locale.ROOT)
                    + " <set|give|take> <player> <amount>");
            return;
        }

        Long amount = parseCoins(sender, args[3]);
        if (amount == null) {
            return;
        }

        long safeAmount = amount;
        SkyBlockProfile balanceProfile = resolveBalanceProfile(profileManager, profile, balanceType);
        double current = balanceType == BalanceType.PURSE
                ? Math.max(0.0D, balanceProfile.getPurse())
                : Math.max(0.0D, balanceProfile.getBankBalance());
        long currentRounded = (long) Math.floor(current + 1e-9D);

        double next;
        long delta;
        String senderMessage;
        String targetMessage;
        switch (action) {
            case "set" -> {
                next = safeAmount;
                delta = safeAmount;
                senderMessage = ChatColor.GREEN + "Set " + ChatColor.AQUA + targetDisplayName + ChatColor.GREEN + "'s "
                        + balanceType.displayName() + " to " + ChatColor.YELLOW + formatCoins(safeAmount)
                        + ChatColor.GREEN + " coins.";
                targetMessage = ChatColor.YELLOW + "An admin set your " + ChatColor.AQUA + balanceType.displayName()
                        + ChatColor.YELLOW + " to " + ChatColor.GOLD + formatCoins(safeAmount) + ChatColor.YELLOW + " coins.";
            }
            case "give", "add" -> {
                next = current + safeAmount;
                delta = safeAmount;
                senderMessage = ChatColor.GREEN + "Gave " + ChatColor.YELLOW + formatCoins(delta) + ChatColor.GREEN
                        + " coins to " + ChatColor.AQUA + targetDisplayName + ChatColor.GREEN + "'s " + balanceType.displayName() + ".";
                targetMessage = ChatColor.GREEN + "You received " + ChatColor.GOLD + formatCoins(delta) + ChatColor.GREEN
                        + " coins in your " + ChatColor.AQUA + balanceType.displayName() + ChatColor.GREEN + ".";
            }
            case "take", "remove", "subtract" -> {
                next = Math.max(0.0D, current - safeAmount);
                delta = currentRounded - (long) Math.floor(next + 1e-9D);
                senderMessage = ChatColor.GREEN + "Took " + ChatColor.YELLOW + formatCoins(delta) + ChatColor.GREEN
                        + " coins from " + ChatColor.AQUA + targetDisplayName + ChatColor.GREEN + "'s " + balanceType.displayName() + ".";
                targetMessage = ChatColor.RED + "An admin took " + ChatColor.GOLD + formatCoins(delta) + ChatColor.RED
                        + " coins from your " + ChatColor.AQUA + balanceType.displayName() + ChatColor.RED + ".";
            }
            default -> {
                sendUsage(sender, label);
                return;
            }
        }

        if (!Double.isFinite(next) || next < 0.0D) {
            sender.sendMessage(ChatColor.RED + "Failed to apply that balance change.");
            return;
        }

        if (balanceType == BalanceType.PURSE) {
            balanceProfile.setPurse(next);
        } else {
            balanceProfile.setBankBalance(next);
        }
        profileManager.saveProfile(balanceProfile);

        sender.sendMessage(senderMessage);

        sendSingleBalance(sender, targetDisplayName, balanceProfile, balanceType);

        if (targetPlayer != null) {
            targetPlayer.sendMessage(targetMessage);

            // If they're currently viewing the bank GUI, update the numbers immediately.
            bankManager.refreshOpenInventory(targetPlayer);
        }
    }

    private void handleShowBalance(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " balance <player>");
            return;
        }

        String targetName = args[1];
        UUID targetId = resolvePlayerUUID(targetName);
        if (targetId == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + targetName);
            return;
        }

        ProfileManager profileManager = plugin.getProfileManager();
        if (profileManager == null) {
            sender.sendMessage(ChatColor.RED + "Profile system is not available.");
            return;
        }

        SkyBlockProfile profile = profileManager.getSelectedProfile(targetId);
        if (profile == null) {
            sender.sendMessage(ChatColor.RED + "That player does not have a Skyblock profile.");
            return;
        }
        SkyBlockProfile bankProfile = resolveBalanceProfile(profileManager, profile, BalanceType.BANK);

        Player targetPlayer = Bukkit.getPlayer(targetId);
        String targetDisplayName = targetPlayer != null ? targetPlayer.getName() : targetName;

        sender.sendMessage(ChatColor.GOLD + "=== Coin Balances (" + targetDisplayName + ") ===");
        sender.sendMessage(ChatColor.GRAY + "Purse: " + ChatColor.GOLD + formatCoins(profile.getPurse()) + " coins");
        sender.sendMessage(ChatColor.GRAY + "Bank: " + ChatColor.GOLD + formatCoins(bankProfile.getBankBalance()) + " coins");
    }

    private void sendSingleBalance(CommandSender sender, String targetName, SkyBlockProfile profile, BalanceType type) {
        double value = type == BalanceType.PURSE ? profile.getPurse() : profile.getBankBalance();
        sender.sendMessage(ChatColor.GRAY + type.displayName() + ": " + ChatColor.GOLD + formatCoins(value) + " coins"
                + ChatColor.GRAY + " (" + ChatColor.AQUA + targetName + ChatColor.GRAY + ")");
    }

    private SkyBlockProfile resolveBalanceProfile(ProfileManager profileManager, SkyBlockProfile selectedProfile, BalanceType type) {
        if (selectedProfile == null || type == BalanceType.PURSE || profileManager == null) {
            return selectedProfile;
        }

        SkyBlockProfile sharedProfile = profileManager.resolveSharedProfile(selectedProfile);
        return sharedProfile != null ? sharedProfile : selectedProfile;
    }

    private Long parseCoins(CommandSender sender, String raw) {
        if (raw == null) {
            sender.sendMessage(ChatColor.RED + "Amount is required.");
            return null;
        }
        String normalized = raw.trim().replace(",", "").replace("_", "");
        if (normalized.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Invalid amount: " + raw);
            return null;
        }
        try {
            long amount = Long.parseLong(normalized);
            if (amount < 0L) {
                sender.sendMessage(ChatColor.RED + "Amount cannot be negative.");
                return null;
            }
            return amount;
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid amount: " + raw);
            return null;
        }
    }

    private static String formatCoins(double coins) {
        if (!Double.isFinite(coins) || coins <= 0.0D) {
            return "0";
        }
        long rounded = (long) Math.floor(coins + 1e-9D);
        if (rounded <= 0L) {
            return "0";
        }
        return String.format(Locale.US, "%,d", rounded);
    }

    private static UUID resolvePlayerUUID(String nameOrUuid) {
        if (nameOrUuid == null || nameOrUuid.isBlank()) {
            return null;
        }

        Player online = Bukkit.getPlayerExact(nameOrUuid);
        if (online != null) {
            return online.getUniqueId();
        }

        try {
            return UUID.fromString(nameOrUuid);
        } catch (IllegalArgumentException ignored) {
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(nameOrUuid);
        return offline != null ? offline.getUniqueId() : null;
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "=== Coins Admin Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " balance <player>" + ChatColor.GRAY + " - View purse/bank");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " purse <set|give|take|check> <player> [amount]" + ChatColor.GRAY + " - Manage purse");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " bank <set|give|take|check> <player> [amount]" + ChatColor.GRAY + " - Manage bank");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("grivience.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            return filterPrefix(List.of("balance", "purse", "bank", "help"), args[0]);
        }

        if (args.length == 2) {
            String root = args[0].toLowerCase(Locale.ROOT);
            if (root.equals("purse") || root.equals("coins") || root.equals("bank")) {
                return filterPrefix(List.of("set", "give", "take", "check"), args[1]);
            }
            if (root.equals("balance") || root.equals("bal")) {
                return filterPrefix(onlinePlayerNames(), args[1]);
            }
            return List.of();
        }

        if (args.length == 3) {
            String root = args[0].toLowerCase(Locale.ROOT);
            if (root.equals("purse") || root.equals("coins") || root.equals("bank")) {
                return filterPrefix(onlinePlayerNames(), args[2]);
            }
            return List.of();
        }

        if (args.length == 4) {
            String root = args[0].toLowerCase(Locale.ROOT);
            String action = args[1].toLowerCase(Locale.ROOT);
            if ((root.equals("purse") || root.equals("coins") || root.equals("bank"))
                    && (action.equals("set") || action.equals("give") || action.equals("add")
                    || action.equals("take") || action.equals("remove") || action.equals("subtract"))) {
                return filterPrefix(List.of("100", "1000", "10000", "100000", "1000000"), args[3]);
            }
        }

        return List.of();
    }

    private List<String> onlinePlayerNames() {
        List<String> names = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            names.add(online.getName());
        }
        return names;
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
