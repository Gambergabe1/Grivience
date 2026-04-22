package io.papermc.Grivience.slayer;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SlayerCommand implements CommandExecutor, TabCompleter {
    private final SlayerManager slayerManager;

    public SlayerCommand(SlayerManager slayerManager) {
        this.slayerManager = slayerManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /slayer start <type>");
            return true;
        }

        if (args[0].equalsIgnoreCase("start") && args.length == 2) {
            try {
                SlayerType type = SlayerType.valueOf(args[1].toUpperCase());
                slayerManager.startQuest(player, type);
            } catch (IllegalArgumentException e) {
                player.sendMessage(ChatColor.RED + "Invalid slayer type. Available types: ZOMBIE, SPIDER, WOLF");
            }
            return true;
        }

        player.sendMessage(ChatColor.RED + "Usage: /slayer start <type>");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if ("start".startsWith(args[0].toLowerCase())) {
                completions.add("start");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            for (SlayerType type : SlayerType.values()) {
                if (type.name().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(type.name());
                }
            }
        }
        return completions;
    }
}