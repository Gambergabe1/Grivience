package io.papermc.Grivience.npcshop;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class NpcShopCommand implements CommandExecutor, TabCompleter {
    private final NpcShopManager manager;

    public NpcShopCommand(NpcShopManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /shop <shopName>");
            return true;
        }
        
        String shopId = args[0].toLowerCase();
        NpcShop shop = manager.getShop(shopId);
        
        if (shop == null) {
            player.sendMessage(ChatColor.RED + "Shop not found!");
            return true;
        }
        
        manager.getGui().open(player, shop);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String input = args[0].toLowerCase();
            for (String shopId : manager.getShops().keySet()) {
                if (shopId.startsWith(input)) {
                    completions.add(shopId);
                }
            }
            return completions;
        }
        return List.of();
    }
}
