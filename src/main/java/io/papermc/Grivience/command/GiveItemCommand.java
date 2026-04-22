package io.papermc.Grivience.command;

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

/**
 * Admin command to give custom items to players.
 * Usage: /gi <item_id> [amount] [player]
 */
public final class GiveItemCommand implements CommandExecutor, TabCompleter {
    private static final int MAX_GIVE_AMOUNT = 2304;
    private final AdminItemResolver itemResolver;

    public GiveItemCommand(AdminItemResolver itemResolver) {
        this.itemResolver = itemResolver;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("grivience.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <item_id> [amount] [player]");
            return true;
        }

        String itemId = args[0].toLowerCase(Locale.ROOT);
        int amount = 1;
        Player target;

        if (args.length >= 2) {
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[1]);
                return true;
            }
            if (amount <= 0) {
                sender.sendMessage(ChatColor.RED + "Amount must be at least 1.");
                return true;
            }
            amount = Math.min(amount, MAX_GIVE_AMOUNT);
        }

        if (args.length >= 3) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[2]);
                return true;
            }
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Please specify a player when running from console.");
                return true;
            }
            target = player;
        }

        ItemStack item = itemResolver.resolve(itemId, target);
        if (item == null) {
            sender.sendMessage(ChatColor.RED + "Unknown custom item: " + itemId);
            List<String> suggestions = itemResolver.matchingKeys(itemId, 12);
            if (!suggestions.isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + "Matches: " + String.join(", ", suggestions));
            } else {
                sender.sendMessage(ChatColor.GRAY + "Use tab-complete to browse item ids.");
            }
            return true;
        }

        item.setAmount(1); // Set to 1 first to handle stacking correctly
        int maxStack = item.getMaxStackSize();
        int remaining = amount;

        while (remaining > 0) {
            int toGive = Math.min(remaining, maxStack);
            ItemStack stack = item.clone();
            stack.setAmount(toGive);
            
            var leftovers = target.getInventory().addItem(stack);
            for (ItemStack leftover : leftovers.values()) {
                target.getWorld().dropItemNaturally(target.getLocation(), leftover);
            }
            remaining -= toGive;
        }

        String displayName = item.hasItemMeta() && item.getItemMeta().hasDisplayName() 
                ? item.getItemMeta().getDisplayName() 
                : item.getType().name();

        sender.sendMessage(ChatColor.GREEN + "Gave " + amount + "x " + ChatColor.YELLOW + displayName + ChatColor.GREEN + " to " + target.getName() + ".");
        if (sender != target) {
            target.sendMessage(ChatColor.GOLD + "[Items] " + ChatColor.YELLOW + "You received " + amount + "x " + displayName + ChatColor.YELLOW + ".");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("grivience.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return new ArrayList<>(itemResolver.matchingKeys(prefix, 100));
        }

        if (args.length == 2) {
            return List.of("1", "16", "64");
        }

        if (args.length == 3) {
            String prefix = args[2].toLowerCase(Locale.ROOT);
            List<String> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    players.add(player.getName());
                }
            }
            return players;
        }

        return List.of();
    }
}
