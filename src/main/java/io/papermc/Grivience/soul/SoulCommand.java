package io.papermc.Grivience.soul;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SoulCommand implements CommandExecutor, TabCompleter {
    private final SoulManager soulManager;

    public SoulCommand(SoulManager soulManager) {
        this.soulManager = soulManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("grivience.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /soul <add|remove|list>");
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("add")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /soul add <id>");
                return true;
            }
            String id = args[1];
            Block target = player.getTargetBlockExact(5);
            if (target == null || target.getType().isAir()) {
                player.sendMessage(ChatColor.RED + "You must be looking at a block.");
                return true;
            }
            soulManager.addSoul(id, target.getLocation());
            player.sendMessage(ChatColor.GREEN + "Added soul '" + id + "' at your target block.");
        } else if (sub.equals("remove")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /soul remove <id>");
                return true;
            }
            String id = args[1];
            soulManager.removeSoul(id);
            player.sendMessage(ChatColor.GREEN + "Removed soul '" + id + "'.");
        } else if (sub.equals("list")) {
            player.sendMessage(ChatColor.GREEN + "Total souls: " + soulManager.getTotalSouls());
        } else {
            player.sendMessage(ChatColor.RED + "Usage: /soul <add|remove|list>");
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("grivience.admin")) return List.of();

        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            if ("add".startsWith(input)) completions.add("add");
            if ("remove".startsWith(input)) completions.add("remove");
            if ("list".startsWith(input)) completions.add("list");
        }
        return completions;
    }
}
