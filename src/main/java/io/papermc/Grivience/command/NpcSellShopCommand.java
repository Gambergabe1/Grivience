package io.papermc.Grivience.command;

import io.papermc.Grivience.bazaar.NpcSellShopGui;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class NpcSellShopCommand implements CommandExecutor {
    private final NpcSellShopGui npcSellShopGui;

    public NpcSellShopCommand(NpcSellShopGui npcSellShopGui) {
        this.npcSellShopGui = npcSellShopGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
            return true;
        }

        npcSellShopGui.open(player);
        return true;
    }
}
