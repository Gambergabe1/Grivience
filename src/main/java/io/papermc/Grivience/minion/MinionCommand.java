package io.papermc.Grivience.minion;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skyblock.island.Island;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MinionCommand implements CommandExecutor, TabCompleter {
    private final GriviencePlugin plugin;
    private final MinionManager minionManager;
    private final MinionGuiManager minionGuiManager;

    public MinionCommand(GriviencePlugin plugin, MinionManager minionManager, MinionGuiManager minionGuiManager) {
        this.plugin = plugin;
        this.minionManager = minionManager;
        this.minionGuiManager = minionGuiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <list|collectall|pickupall|give|givefuel|giveupgrade|givefragment|constellation>");
                return true;
            }
            minionGuiManager.openOverview(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "list" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
                    return true;
                }
                handleList(player);
            }
            case "collectall" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
                    return true;
                }
                minionManager.collectAll(player);
            }
            case "pickupall" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
                    return true;
                }
                minionManager.pickupAll(player);
            }
            case "give" -> handleGive(sender, label, args);
            case "givefuel" -> handleGiveUtility(sender, label, args, true);
            case "giveupgrade" -> handleGiveUtility(sender, label, args, false);
            case "givefragment" -> handleGiveFragment(sender, label, args);
            case "constellation" -> handleConstellation(sender, label, args);
            case "help" -> sendHelp(sender, label);
            default -> {
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /" + label + " help.");
            }
        }
        return true;
    }

    private void handleList(Player player) {
        if (minionManager == null || !minionManager.isEnabled()) {
            player.sendMessage(ChatColor.RED + "Minions are currently disabled.");
            return;
        }
        Island island = plugin.getIslandManager() != null ? plugin.getIslandManager().getIsland(player) : null;
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You don't have an island.");
            return;
        }
        List<MinionInstance> minions = minionManager.getMinionsForIsland(island);
        int limit = minionManager.getIslandMinionLimit(island);
        player.sendMessage(ChatColor.GOLD + "=== Island Minions ===");
        player.sendMessage(ChatColor.GRAY + "Slots: " + ChatColor.AQUA + minions.size()
                + ChatColor.DARK_GRAY + "/" + ChatColor.AQUA + (limit < 0 ? "Unlimited" : limit));
        if (minions.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "No minions placed.");
            return;
        }
        int shown = 0;
        for (MinionInstance minion : minions) {
            player.sendMessage(ChatColor.YELLOW + "- " + ChatColor.GREEN + minion.type().displayName()
                    + ChatColor.GRAY + " T" + minion.tier()
                    + ChatColor.DARK_GRAY + " | "
                    + ChatColor.YELLOW + minion.storedAmount() + "/" + minionManager.getStorageCap(minion.type(), minion.tier()));
            shown++;
            if (shown >= 15) {
                player.sendMessage(ChatColor.DARK_GRAY + "... and " + (minions.size() - shown) + " more.");
                break;
            }
        }
    }

    private void handleGive(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("grivience.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " give <type> [tier] [player]");
            return;
        }
        MinionType type = MinionType.parse(args[1]);
        if (type == null) {
            sender.sendMessage(ChatColor.RED + "Unknown minion type: " + args[1]);
            return;
        }

        int tier = 1;
        if (args.length >= 3) {
            try {
                tier = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Tier must be a number.");
                return;
            }
        }
        tier = Math.max(1, Math.min(tier, minionManager.getMaxTier(type)));

        Player target;
        if (args.length >= 4) {
            target = Bukkit.getPlayerExact(args[3]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[3]);
                return;
            }
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Specify a player from console.");
                return;
            }
            target = player;
        }

        ItemStack item = minionManager.createMinionItem(type, tier);
        if (item == null) {
            sender.sendMessage(ChatColor.RED + "Failed to create minion item.");
            return;
        }

        var leftovers = target.getInventory().addItem(item);
        if (!leftovers.isEmpty()) {
            for (ItemStack stack : leftovers.values()) {
                if (stack != null) {
                    target.getWorld().dropItemNaturally(target.getLocation(), stack);
                }
            }
        }

        sender.sendMessage(ChatColor.GREEN + "Gave " + ChatColor.AQUA + target.getName()
                + ChatColor.GREEN + " a " + ChatColor.YELLOW + type.displayName() + " Minion T" + tier + ChatColor.GREEN + ".");
        if (sender != target) {
            target.sendMessage(ChatColor.GOLD + "You received a " + ChatColor.YELLOW + type.displayName() + " Minion T" + tier + ChatColor.GOLD + ".");
        }
    }

    private void handleGiveUtility(CommandSender sender, String label, String[] args, boolean fuel) {
        if (!sender.hasPermission("grivience.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission.");
            return;
        }
        String sub = fuel ? "givefuel" : "giveupgrade";
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " " + sub + " <id> [amount] [player]");
            return;
        }

        String id = args[1].trim().toLowerCase(Locale.ROOT);
        ItemStack item = fuel ? minionManager.createFuelItem(id, 1) : minionManager.createUpgradeItem(id, 1);
        if (item == null) {
            sender.sendMessage(ChatColor.RED + "Unknown " + (fuel ? "fuel" : "upgrade") + " id: " + id);
            return;
        }

        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Math.max(1, Math.min(64, Integer.parseInt(args[2])));
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Amount must be a number.");
                return;
            }
        }

        Player target;
        if (args.length >= 4) {
            target = Bukkit.getPlayerExact(args[3]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[3]);
                return;
            }
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Specify a player from console.");
                return;
            }
            target = player;
        }

        int remaining = amount;
        while (remaining > 0) {
            int stackAmount = Math.min(remaining, item.getMaxStackSize());
            ItemStack give = item.clone();
            give.setAmount(stackAmount);
            var leftover = target.getInventory().addItem(give);
            if (!leftover.isEmpty()) {
                for (ItemStack stack : leftover.values()) {
                    if (stack != null && !stack.getType().isAir()) {
                        target.getWorld().dropItemNaturally(target.getLocation(), stack);
                    }
                }
            }
            remaining -= stackAmount;
        }

        sender.sendMessage(ChatColor.GREEN + "Gave " + ChatColor.AQUA + target.getName() + ChatColor.GREEN + " " + amount
                + "x " + ChatColor.YELLOW + (fuel ? minionManager.fuelDisplayName(id) : minionManager.upgradeDisplayName(id)) + ChatColor.GREEN + ".");
        if (sender != target) {
            target.sendMessage(ChatColor.GOLD + "You received " + ChatColor.YELLOW + amount + "x "
                    + (fuel ? minionManager.fuelDisplayName(id) : minionManager.upgradeDisplayName(id)) + ChatColor.GOLD + ".");
        }
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "=== Minion Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + ChatColor.GRAY + " - Open minion management GUI");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " list" + ChatColor.GRAY + " - List placed island minions");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " collectall" + ChatColor.GRAY + " - Collect all minion resources");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " pickupall" + ChatColor.GRAY + " - Pick up all island minions");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " give <type> [tier] [player]" + ChatColor.GRAY + " - Admin minion item");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " givefuel <id> [amount] [player]" + ChatColor.GRAY + " - Admin minion fuel");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " giveupgrade <id> [amount] [player]" + ChatColor.GRAY + " - Admin minion upgrade");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " givefragment [amount] [player]" + ChatColor.GRAY + " - Admin Constellation Fragment");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " constellation <status|set|clear> ..." + ChatColor.GRAY + " - Admin constellation control");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterPrefix(List.of("list", "collectall", "pickupall", "give", "givefuel", "giveupgrade", "givefragment", "constellation", "help"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> types = new ArrayList<>();
            for (MinionType type : MinionType.values()) {
                types.add(type.id());
            }
            return filterPrefix(types, args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("givefuel")) {
            return filterPrefix(minionManager.fuelIds(), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("giveupgrade")) {
            return filterPrefix(minionManager.upgradeIds(), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("givefragment")) {
            return filterPrefix(List.of("1", "8", "16", "32", "64", "128"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("constellation")) {
            return filterPrefix(List.of("status", "set", "clear"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            MinionType type = MinionType.parse(args[1]);
            int max = type == null ? 11 : minionManager.getMaxTier(type);
            List<String> tiers = new ArrayList<>();
            for (int i = 1; i <= max; i++) {
                tiers.add(String.valueOf(i));
            }
            return filterPrefix(tiers, args[2]);
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("givefuel") || args[0].equalsIgnoreCase("giveupgrade"))) {
            return filterPrefix(List.of("1", "8", "16", "32", "64"), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("givefragment")) {
            List<String> players = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                players.add(online.getName());
            }
            return filterPrefix(players, args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("constellation")) {
            if (args[1].equalsIgnoreCase("set")) {
                return filterPrefix(List.of("0", "1", "2", "3"), args[2]);
            }
            if (args[1].equalsIgnoreCase("status") || args[1].equalsIgnoreCase("clear")) {
                List<String> players = new ArrayList<>();
                for (Player online : Bukkit.getOnlinePlayers()) {
                    players.add(online.getName());
                }
                return filterPrefix(players, args[2]);
            }
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            List<String> players = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                players.add(online.getName());
            }
            return filterPrefix(players, args[3]);
        }
        if (args.length == 4 && (args[0].equalsIgnoreCase("givefuel") || args[0].equalsIgnoreCase("giveupgrade"))) {
            List<String> players = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                players.add(online.getName());
            }
            return filterPrefix(players, args[3]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("constellation") && args[1].equalsIgnoreCase("set")) {
            List<String> players = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                players.add(online.getName());
            }
            return filterPrefix(players, args[3]);
        }
        return List.of();
    }

    private void handleGiveFragment(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("grivience.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission.");
            return;
        }

        int amount = 1;
        if (args.length >= 2) {
            try {
                amount = Math.max(1, Math.min(2304, Integer.parseInt(args[1])));
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Amount must be a number.");
                return;
            }
        }

        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[2]);
                return;
            }
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Usage from console: /" + label + " givefragment <amount> <player>");
                return;
            }
            target = player;
        }

        ItemStack base = minionManager.createIngredientItem("constellation_fragment", 1);
        if (base == null) {
            sender.sendMessage(ChatColor.RED + "Constellation Fragment item could not be created.");
            return;
        }

        int remaining = amount;
        while (remaining > 0) {
            int stackAmount = Math.min(remaining, base.getMaxStackSize());
            ItemStack give = base.clone();
            give.setAmount(stackAmount);
            var leftovers = target.getInventory().addItem(give);
            if (!leftovers.isEmpty()) {
                for (ItemStack stack : leftovers.values()) {
                    if (stack != null && !stack.getType().isAir()) {
                        target.getWorld().dropItemNaturally(target.getLocation(), stack);
                    }
                }
            }
            remaining -= stackAmount;
        }

        sender.sendMessage(ChatColor.GREEN + "Gave " + ChatColor.AQUA + target.getName()
                + ChatColor.GREEN + " " + ChatColor.YELLOW + amount + "x Constellation Fragment" + ChatColor.GREEN + ".");
        if (sender != target) {
            target.sendMessage(ChatColor.GOLD + "You received " + ChatColor.YELLOW + amount + "x Constellation Fragment" + ChatColor.GOLD + ".");
        }
    }

    private void handleConstellation(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("grivience.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " constellation <status|set|clear> ...");
            return;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "status" -> {
                Player target = resolveTarget(sender, label, args, 2, "constellation status");
                if (target == null) {
                    return;
                }
                Island island = plugin.getIslandManager() != null ? plugin.getIslandManager().getIsland(target) : null;
                if (island == null) {
                    sender.sendMessage(ChatColor.RED + target.getName() + " has no island.");
                    return;
                }
                MinionManager.ConstellationInfo info = minionManager.getConstellationInfo(island);
                sender.sendMessage(ChatColor.GOLD + "=== Constellation Synergy: " + target.getName() + " ===");
                sender.sendMessage(ChatColor.GRAY + "Tier: " + ChatColor.AQUA + info.tier()
                        + ChatColor.DARK_GRAY + " (" + ChatColor.AQUA + minionManager.constellationTierName(info.tier()) + ChatColor.DARK_GRAY + ")");
                sender.sendMessage(ChatColor.GRAY + "Unique Minion Types: " + ChatColor.YELLOW + info.uniqueMinionTypes());
                sender.sendMessage(ChatColor.GRAY + "Speed Bonus: " + ChatColor.GREEN + "+" + formatPercent(info.speedMultiplier() - 1.0D));
                sender.sendMessage(ChatColor.GRAY + "Fragment Chance: " + ChatColor.LIGHT_PURPLE + formatPercent(info.fragmentChance()));
                if (info.overrideTier() != null) {
                    sender.sendMessage(ChatColor.GRAY + "Override: " + ChatColor.GOLD + info.overrideTier());
                } else {
                    sender.sendMessage(ChatColor.GRAY + "Override: " + ChatColor.DARK_GRAY + "None (auto)");
                }
            }
            case "set" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " constellation set <tier:0-3> [player]");
                    return;
                }
                int tier;
                try {
                    tier = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Tier must be a number (0-3).");
                    return;
                }
                if (tier < 0 || tier > 3) {
                    sender.sendMessage(ChatColor.RED + "Tier must be between 0 and 3.");
                    return;
                }
                Player target = resolveTarget(sender, label, args, 3, "constellation set <tier:0-3>");
                if (target == null) {
                    return;
                }
                Island island = plugin.getIslandManager() != null ? plugin.getIslandManager().getIsland(target) : null;
                if (island == null) {
                    sender.sendMessage(ChatColor.RED + target.getName() + " has no island.");
                    return;
                }
                minionManager.setConstellationTierOverride(island.getId(), tier);
                sender.sendMessage(ChatColor.GREEN + "Set Constellation tier for " + ChatColor.AQUA + target.getName()
                        + ChatColor.GREEN + " to " + ChatColor.YELLOW + tier + ChatColor.DARK_GRAY
                        + " (" + ChatColor.YELLOW + minionManager.constellationTierName(tier) + ChatColor.DARK_GRAY + ")");
            }
            case "clear" -> {
                Player target = resolveTarget(sender, label, args, 2, "constellation clear");
                if (target == null) {
                    return;
                }
                Island island = plugin.getIslandManager() != null ? plugin.getIslandManager().getIsland(target) : null;
                if (island == null) {
                    sender.sendMessage(ChatColor.RED + target.getName() + " has no island.");
                    return;
                }
                boolean removed = minionManager.clearConstellationTierOverride(island.getId());
                if (removed) {
                    sender.sendMessage(ChatColor.GREEN + "Cleared Constellation override for " + ChatColor.AQUA + target.getName() + ChatColor.GREEN + ".");
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "No Constellation override was set for " + target.getName() + ".");
                }
            }
            default -> sender.sendMessage(ChatColor.RED + "Usage: /" + label + " constellation <status|set|clear> ...");
        }
    }

    private Player resolveTarget(CommandSender sender, String label, String[] args, int playerArgIndex, String usageTail) {
        if (args.length > playerArgIndex) {
            Player target = Bukkit.getPlayerExact(args[playerArgIndex]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[playerArgIndex]);
                return null;
            }
            return target;
        }
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage(ChatColor.RED + "Usage from console: /" + label + " " + usageTail + " <player>");
        return null;
    }

    private static String formatPercent(double ratio) {
        double percent = ratio * 100.0D;
        if (Math.abs(percent - Math.rint(percent)) < 0.0001D) {
            return String.format(Locale.US, "%.0f%%", percent);
        }
        return String.format(Locale.US, "%.1f%%", percent);
    }

    private List<String> filterPrefix(List<String> input, String prefix) {
        String lower = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> filtered = new ArrayList<>();
        for (String entry : input) {
            if (entry.toLowerCase(Locale.ROOT).startsWith(lower)) {
                filtered.add(entry);
            }
        }
        return filtered;
    }
}
