package io.papermc.Grivience.bank;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BankCommand implements CommandExecutor, TabCompleter {
    private final BankManager bankManager;

    public BankCommand(BankManager bankManager) {
        this.bankManager = bankManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
            return true;
        }

        if (args.length == 0) {
            bankManager.openMain(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "balance", "bal" -> bankManager.showBalances(player);
            case "deposit", "dep" -> handleDeposit(player, args);
            case "withdraw", "with" -> handleWithdraw(player, args);
            case "help" -> sendHelp(player, label);
            default -> {
                player.sendMessage(ChatColor.RED + "Unknown subcommand. Use /" + label + " help.");
            }
        }

        return true;
    }

    private void handleDeposit(Player player, String[] args) {
        if (args.length < 2) {
            bankManager.openDeposit(player);
            return;
        }

        String amount = args[1];
        if (amount.equalsIgnoreCase("all")) {
            bankManager.depositAll(player);
            bankManager.refreshOpenInventory(player);
            return;
        }

        Long parsed = parseCoins(amount);
        if (parsed == null) {
            player.sendMessage(ChatColor.RED + "Invalid number. Example: /bank deposit 25000");
            return;
        }

        bankManager.deposit(player, parsed);
        bankManager.refreshOpenInventory(player);
    }

    private void handleWithdraw(Player player, String[] args) {
        if (args.length < 2) {
            bankManager.openWithdraw(player);
            return;
        }

        String amount = args[1];
        if (amount.equalsIgnoreCase("all")) {
            bankManager.withdrawAll(player);
            bankManager.refreshOpenInventory(player);
            return;
        }

        Long parsed = parseCoins(amount);
        if (parsed == null) {
            player.sendMessage(ChatColor.RED + "Invalid number. Example: /bank withdraw 25000");
            return;
        }

        bankManager.withdraw(player, parsed);
        bankManager.refreshOpenInventory(player);
    }

    private Long parseCoins(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().replace(",", "").replace("_", "");
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            long coins = Long.parseLong(normalized);
            if (coins < 0L) {
                return null;
            }
            return coins;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.add("balance");
            out.add("deposit");
            out.add("withdraw");
            out.add("help");
            return out;
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("deposit") || sub.equals("dep") || sub.equals("withdraw") || sub.equals("with")) {
                out.add("all");
                out.add("100");
                out.add("1000");
                out.add("10000");
                out.add("100000");
                out.add("1000000");
            }
        }
        return out;
    }

    private void sendHelp(Player player, String label) {
        player.sendMessage(ChatColor.GOLD + "Bank Commands:");
        player.sendMessage(ChatColor.YELLOW + "/" + label + ChatColor.GRAY + " - Open the bank menu");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " balance" + ChatColor.GRAY + " - View purse/bank");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " deposit <amount|all>" + ChatColor.GRAY + " - Deposit from purse to bank");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " withdraw <amount|all>" + ChatColor.GRAY + " - Withdraw from bank to purse");
    }
}

