package io.papermc.Grivience.farming;

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

public final class FarmingContestCommand implements CommandExecutor, TabCompleter {
    private static final String ADMIN_PERMISSION = "grivience.admin.farmingcontest";

    private final FarmingContestManager manager;

    public FarmingContestCommand(FarmingContestManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "open" : args[0].toLowerCase(Locale.ROOT);
        Player player = sender instanceof Player online ? online : null;

        switch (sub) {
            case "open", "menu" -> {
                if (player == null) {
                    manager.sendStatus(sender, null);
                    return true;
                }
                manager.openMenu(player);
                return true;
            }
            case "status" -> {
                manager.sendStatus(sender, player);
                return true;
            }
            case "schedule" -> {
                manager.sendSchedule(sender);
                return true;
            }
            case "check" -> {
                if (args.length == 1) {
                    if (player == null) {
                        sender.sendMessage(ChatColor.RED + "Console must specify a target profile.");
                        return true;
                    }
                    FarmingContestManager.ContestProfileRef self = manager.resolveProfileTarget(player.getName());
                    if (self == null) {
                        sender.sendMessage(ChatColor.RED + "Your profile could not be resolved.");
                        return true;
                    }
                    manager.sendProfileSummary(sender, self.displayName(), self.profileId());
                    return true;
                }
                if (!sender.hasPermission(ADMIN_PERMISSION)) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to inspect other contest profiles.");
                    return true;
                }
                FarmingContestManager.ContestProfileRef target = manager.resolveProfileTarget(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Target profile not found.");
                    return true;
                }
                manager.sendProfileSummary(sender, target.displayName(), target.profileId());
                return true;
            }
            case "start" -> {
                if (!sender.hasPermission(ADMIN_PERMISSION)) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to start Farming Contests.");
                    return true;
                }
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " start <crop1> <crop2> <crop3> [durationMinutes]");
                    return true;
                }
                List<FarmingContestCrop> crops = new ArrayList<>();
                for (int i = 1; i <= 3; i++) {
                    FarmingContestCrop crop = FarmingContestCrop.fromInput(args[i]);
                    if (crop == null) {
                        sender.sendMessage(ChatColor.RED + "Unknown crop: " + args[i]);
                        return true;
                    }
                    crops.add(crop);
                }
                int duration = 20;
                if (args.length >= 5) {
                    try {
                        duration = Integer.parseInt(args[4]);
                    } catch (NumberFormatException exception) {
                        sender.sendMessage(ChatColor.RED + "Invalid duration: " + args[4]);
                        return true;
                    }
                }
                if (!manager.startForcedContest(crops, duration, sender)) {
                    sender.sendMessage(ChatColor.RED + "Could not start Farming Contest. Stop the current contest first and provide 3 unique crops.");
                    return true;
                }
                sender.sendMessage(ChatColor.GREEN + "Started Farming Contest for " + ChatColor.GOLD + duration + ChatColor.GREEN + " minutes.");
                return true;
            }
            case "stop" -> {
                if (!sender.hasPermission(ADMIN_PERMISSION)) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to stop Farming Contests.");
                    return true;
                }
                if (!manager.stopActiveContest(sender, true)) {
                    sender.sendMessage(ChatColor.RED + "There is no active Farming Contest.");
                    return true;
                }
                sender.sendMessage(ChatColor.GREEN + "Stopped the active Farming Contest and paid out rewards.");
                return true;
            }
            case "cancel" -> {
                if (!sender.hasPermission(ADMIN_PERMISSION)) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to cancel Farming Contests.");
                    return true;
                }
                if (!manager.stopActiveContest(sender, false)) {
                    sender.sendMessage(ChatColor.RED + "There is no active Farming Contest.");
                    return true;
                }
                sender.sendMessage(ChatColor.YELLOW + "Cancelled the active Farming Contest without rewards.");
                return true;
            }
            case "reload" -> {
                if (!sender.hasPermission(ADMIN_PERMISSION)) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to reload Farming Contests.");
                    return true;
                }
                manager.reload();
                sender.sendMessage(ChatColor.GREEN + "Reloaded Farming Contest config and state.");
                return true;
            }
            case "set", "add" -> {
                if (!sender.hasPermission(ADMIN_PERMISSION)) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to edit contest currencies.");
                    return true;
                }
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " " + sub + " <player|profileId> <tickets|bronze|silver|gold> <amount>");
                    return true;
                }
                FarmingContestManager.ContestProfileRef target = manager.resolveProfileTarget(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Target profile not found.");
                    return true;
                }
                FarmingContestManager.ContestCurrency currency = FarmingContestManager.ContestCurrency.fromInput(args[2]);
                if (currency == null) {
                    sender.sendMessage(ChatColor.RED + "Unknown currency: " + args[2]);
                    return true;
                }
                long amount;
                try {
                    amount = Long.parseLong(args[3]);
                } catch (NumberFormatException exception) {
                    sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[3]);
                    return true;
                }
                boolean changed = sub.equals("set")
                        ? manager.setCurrency(target.profileId(), currency, amount)
                        : manager.addCurrency(target.profileId(), currency, amount);
                if (!changed) {
                    sender.sendMessage(ChatColor.RED + "Could not update contest currency.");
                    return true;
                }
                manager.sendProfileSummary(sender, target.displayName(), target.profileId());
                return true;
            }
            case "setscore", "addscore" -> {
                if (!sender.hasPermission(ADMIN_PERMISSION)) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to edit contest scores.");
                    return true;
                }
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " " + sub + " <player|profileId> <crop> <amount>");
                    return true;
                }
                FarmingContestManager.ContestProfileRef target = manager.resolveProfileTarget(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Target profile not found.");
                    return true;
                }
                FarmingContestCrop crop = FarmingContestCrop.fromInput(args[2]);
                if (crop == null) {
                    sender.sendMessage(ChatColor.RED + "Unknown crop: " + args[2]);
                    return true;
                }
                long amount;
                try {
                    amount = Long.parseLong(args[3]);
                } catch (NumberFormatException exception) {
                    sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[3]);
                    return true;
                }
                boolean success = manager.setActiveScore(target.profileId(), crop, amount, sub.equals("addscore"));
                if (!success) {
                    sender.sendMessage(ChatColor.RED + "Could not update the active contest score. Make sure the crop is featured and a contest is active.");
                    return true;
                }
                sender.sendMessage(ChatColor.GREEN + "Updated " + ChatColor.AQUA + target.displayName()
                        + ChatColor.GREEN + " for " + ChatColor.GOLD + crop.displayName() + ChatColor.GREEN + ".");
                return true;
            }
            case "help" -> {
                sendHelp(sender, label);
                return true;
            }
            default -> {
                sendHelp(sender, label);
                return true;
            }
        }
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "=== Farming Contest Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + ChatColor.GRAY + " - Open the Farming Contest menu.");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " status" + ChatColor.GRAY + " - Show the active or next contest.");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " schedule" + ChatColor.GRAY + " - Show the next contests.");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " check [player|profileId]" + ChatColor.GRAY + " - View contest tickets and medals.");
        if (sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " start <crop1> <crop2> <crop3> [minutes]" + ChatColor.GRAY + " - Start an admin contest.");
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " stop" + ChatColor.GRAY + " - End the active contest and pay rewards.");
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " cancel" + ChatColor.GRAY + " - End the active contest without rewards.");
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " set|add <target> <tickets|bronze|silver|gold> <amount>" + ChatColor.GRAY + " - Edit wallet balances.");
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " setscore|addscore <target> <crop> <amount>" + ChatColor.GRAY + " - Edit active contest scores.");
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload" + ChatColor.GRAY + " - Reload contest config and saved data.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            suggestions.add("open");
            suggestions.add("status");
            suggestions.add("schedule");
            suggestions.add("check");
            suggestions.add("help");
            if (sender.hasPermission(ADMIN_PERMISSION)) {
                suggestions.add("start");
                suggestions.add("stop");
                suggestions.add("cancel");
                suggestions.add("set");
                suggestions.add("add");
                suggestions.add("setscore");
                suggestions.add("addscore");
                suggestions.add("reload");
            }
            return filterPrefix(suggestions, args[0]);
        }
        if (args.length >= 2 && args.length <= 4 && args[0].equalsIgnoreCase("start")) {
            return filterPrefix(FarmingContestCrop.ids(), args[args.length - 1]);
        }
        if (args.length == 5 && args[0].equalsIgnoreCase("start")) {
            return filterPrefix(List.of("10", "20", "30", "60"), args[4]);
        }
        if (args.length == 2 && List.of("check", "set", "add", "setscore", "addscore").contains(args[0].toLowerCase(Locale.ROOT))) {
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                names.add(online.getName());
            }
            return filterPrefix(names, args[1]);
        }
        if (args.length == 3 && List.of("set", "add").contains(args[0].toLowerCase(Locale.ROOT))) {
            return filterPrefix(List.of("tickets", "bronze", "silver", "gold"), args[2]);
        }
        if (args.length == 4 && List.of("set", "add").contains(args[0].toLowerCase(Locale.ROOT))) {
            return filterPrefix(List.of("1", "5", "10", "25", "50"), args[3]);
        }
        if (args.length == 3 && List.of("setscore", "addscore").contains(args[0].toLowerCase(Locale.ROOT))) {
            return filterPrefix(FarmingContestCrop.ids(), args[2]);
        }
        if (args.length == 4 && List.of("setscore", "addscore").contains(args[0].toLowerCase(Locale.ROOT))) {
            return filterPrefix(List.of("100", "500", "1000", "5000", "10000"), args[3]);
        }
        return List.of();
    }

    private List<String> filterPrefix(List<String> values, String prefix) {
        String loweredPrefix = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> filtered = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(loweredPrefix)) {
                filtered.add(value);
            }
        }
        return filtered;
    }
}
