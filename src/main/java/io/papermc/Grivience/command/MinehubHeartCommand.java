package io.papermc.Grivience.command;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.mines.MinehubCommissionManager;
import io.papermc.Grivience.mines.MinehubHeartManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class MinehubHeartCommand implements CommandExecutor, TabCompleter {
    private final GriviencePlugin plugin;
    private final MinehubHeartManager heartManager;
    private final MinehubCommissionManager commissionManager;

    public MinehubHeartCommand(GriviencePlugin plugin, MinehubHeartManager heartManager, MinehubCommissionManager commissionManager) {
        this.plugin = plugin;
        this.heartManager = heartManager;
        this.commissionManager = commissionManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            // Open main Heart GUI
            heartManager.openGui(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "commissions", "commission", "comm" -> {
                if (commissionManager != null) {
                    commissionManager.openCommissionsGui(player);
                } else {
                    player.sendMessage(ChatColor.RED + "Commissions are not available.");
                }
                return true;
            }

            case "frenzy", "activate" -> {
                if (heartManager.canActivateMiningFrenzy(player)) {
                    heartManager.activateMiningFrenzy(player);
                } else {
                    player.sendMessage(ChatColor.RED + "You cannot activate Mining Frenzy right now!");
                    player.sendMessage(ChatColor.GRAY + "Requirement: Unlock " + ChatColor.GOLD + "Minehub Heart Core" + ChatColor.GRAY + " perk (Level 7, 5 tokens)");
                }
                return true;
            }

            case "help", "?" -> {
                player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "=== Heart of the Minehub ===");
                player.sendMessage(ChatColor.YELLOW + "/minehub" + ChatColor.GRAY + " - Open Heart GUI");
                player.sendMessage(ChatColor.YELLOW + "/minehub commissions" + ChatColor.GRAY + " - View daily commissions");
                player.sendMessage(ChatColor.YELLOW + "/minehub frenzy" + ChatColor.GRAY + " - Activate Mining Frenzy (if unlocked)");
                player.sendMessage(ChatColor.YELLOW + "/minehub help" + ChatColor.GRAY + " - Show this help");
                return true;
            }

            default -> {
                player.sendMessage(ChatColor.RED + "Unknown subcommand. Use /minehub help");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("commissions");
            completions.add("frenzy");
            completions.add("help");
        }

        return completions;
    }
}
