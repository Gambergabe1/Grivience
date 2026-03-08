package io.papermc.Grivience.bank;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skyblock.economy.ProfileBankService;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Locale;

public final class BankManager {
    private final GriviencePlugin plugin;
    private final ProfileBankService bankService;

    public BankManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.bankService = new ProfileBankService(plugin);
    }

    public void open(Player player, BankGui.ViewType viewType) {
        if (player == null) {
            return;
        }
        long purse = bankService.purseCoins(player);
        long bank = bankService.bankCoins(player);
        Inventory inv = BankGui.create(player, viewType, purse, bank);
        player.openInventory(inv);
        BankGui.playOpenSound(player);
    }

    public void openMain(Player player) {
        open(player, BankGui.ViewType.MAIN);
    }

    public void openDeposit(Player player) {
        open(player, BankGui.ViewType.DEPOSIT);
    }

    public void openWithdraw(Player player) {
        open(player, BankGui.ViewType.WITHDRAW);
    }

    public void refreshOpenInventory(Player player) {
        if (player == null) {
            return;
        }
        Inventory top = player.getOpenInventory().getTopInventory();
        if (!BankGui.isBankInventory(top)) {
            return;
        }
        BankGui.ViewType viewType = BankGui.viewType(top);
        long purse = bankService.purseCoins(player);
        long bank = bankService.bankCoins(player);
        BankGui.populate(top, viewType, player, purse, bank);
        player.updateInventory();
    }

    public void showBalances(Player player) {
        if (player == null) {
            return;
        }
        long purse = bankService.purseCoins(player);
        long bank = bankService.bankCoins(player);
        player.sendMessage(ChatColor.GOLD + "Bank");
        player.sendMessage(ChatColor.GRAY + "Purse: " + ChatColor.GOLD + formatCoins(purse) + " coins");
        player.sendMessage(ChatColor.GRAY + "Bank: " + ChatColor.GOLD + formatCoins(bank) + " coins");
    }

    public boolean deposit(Player player, long coins) {
        if (player == null) {
            return false;
        }
        long safe = Math.max(0L, coins);
        if (safe == 0L) {
            player.sendMessage(ChatColor.YELLOW + "Nothing to deposit.");
            return false;
        }
        if (bankService.requireSelectedProfile(player) == null) {
            return false;
        }
        long purse = bankService.purseCoins(player);
        if (safe > purse) {
            player.sendMessage(ChatColor.RED + "You do not have that many coins in your purse.");
            player.sendMessage(ChatColor.GRAY + "Purse: " + ChatColor.YELLOW + formatCoins(purse) + " coins");
            return false;
        }
        if (!bankService.depositToBank(player, safe)) {
            player.sendMessage(ChatColor.RED + "Deposit failed.");
            return false;
        }
        player.sendMessage(ChatColor.GREEN + "Deposited " + ChatColor.GOLD + formatCoins(safe) + ChatColor.GREEN + " coins into your bank.");
        return true;
    }

    public boolean depositAll(Player player) {
        if (player == null) {
            return false;
        }
        if (bankService.requireSelectedProfile(player) == null) {
            return false;
        }
        long purse = bankService.purseCoins(player);
        if (purse <= 0L) {
            player.sendMessage(ChatColor.YELLOW + "You have no coins in your purse.");
            return false;
        }
        if (!bankService.depositToBank(player, purse)) {
            player.sendMessage(ChatColor.RED + "Deposit failed.");
            return false;
        }
        player.sendMessage(ChatColor.GREEN + "Deposited " + ChatColor.GOLD + formatCoins(purse) + ChatColor.GREEN + " coins into your bank.");
        return true;
    }

    public boolean withdraw(Player player, long coins) {
        if (player == null) {
            return false;
        }
        long safe = Math.max(0L, coins);
        if (safe == 0L) {
            player.sendMessage(ChatColor.YELLOW + "Nothing to withdraw.");
            return false;
        }
        if (bankService.requireSelectedProfile(player) == null) {
            return false;
        }
        long bank = bankService.bankCoins(player);
        if (safe > bank) {
            player.sendMessage(ChatColor.RED + "You do not have that many coins in your bank.");
            player.sendMessage(ChatColor.GRAY + "Bank: " + ChatColor.YELLOW + formatCoins(bank) + " coins");
            return false;
        }
        if (!bankService.withdrawFromBank(player, safe)) {
            player.sendMessage(ChatColor.RED + "Withdrawal failed.");
            return false;
        }
        player.sendMessage(ChatColor.GREEN + "Withdrew " + ChatColor.GOLD + formatCoins(safe) + ChatColor.GREEN + " coins into your purse.");
        return true;
    }

    public boolean withdrawAll(Player player) {
        if (player == null) {
            return false;
        }
        if (bankService.requireSelectedProfile(player) == null) {
            return false;
        }
        long bank = bankService.bankCoins(player);
        if (bank <= 0L) {
            player.sendMessage(ChatColor.YELLOW + "You have no coins in your bank.");
            return false;
        }
        if (!bankService.withdrawFromBank(player, bank)) {
            player.sendMessage(ChatColor.RED + "Withdrawal failed.");
            return false;
        }
        player.sendMessage(ChatColor.GREEN + "Withdrew " + ChatColor.GOLD + formatCoins(bank) + ChatColor.GREEN + " coins into your purse.");
        return true;
    }

    private static String formatCoins(long coins) {
        return String.format(Locale.US, "%,d", Math.max(0L, coins));
    }
}

