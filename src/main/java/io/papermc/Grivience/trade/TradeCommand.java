package io.papermc.Grivience.trade;

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

public final class TradeCommand implements CommandExecutor, TabCompleter {
    private final TradeManager tradeManager;

    public TradeCommand(TradeManager tradeManager) {
        this.tradeManager = tradeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "accept" -> {
                Player requester = args.length >= 2 ? Bukkit.getPlayerExact(args[1]) : null;
                tradeManager.acceptIncoming(player, requester);
                return true;
            }
            case "decline", "deny" -> {
                Player requester = args.length >= 2 ? Bukkit.getPlayerExact(args[1]) : null;
                tradeManager.declineIncoming(player, requester);
                return true;
            }
            case "cancel" -> {
                // Cancel active trade first, otherwise cancel the outgoing request.
                if (tradeManager.getSessionByPlayer(player.getUniqueId()) != null) {
                    tradeManager.cancelActiveTrade(player);
                } else {
                    tradeManager.cancelOutgoing(player);
                }
                return true;
            }
            case "help" -> {
                sendHelp(player, label);
                return true;
            }
            default -> {
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target == null) {
                    player.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }
                tradeManager.requestTrade(player, target);
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.add("accept");
            out.add("decline");
            out.add("cancel");
            out.add("help");
            for (Player p : Bukkit.getOnlinePlayers()) {
                out.add(p.getName());
            }
            return out;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("accept") || args[0].equalsIgnoreCase("decline") || args[0].equalsIgnoreCase("deny"))) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                out.add(p.getName());
            }
        }
        return out;
    }

    private void sendHelp(Player player, String label) {
        player.sendMessage(ChatColor.GOLD + "Trade Commands:");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " <player>" + ChatColor.GRAY + " - Send a trade request");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " accept [player]" + ChatColor.GRAY + " - Accept a trade request");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " decline [player]" + ChatColor.GRAY + " - Decline a trade request");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " cancel" + ChatColor.GRAY + " - Cancel outgoing request or active trade");
    }
}

