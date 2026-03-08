package io.papermc.Grivience.command;

import io.papermc.Grivience.listener.BlacksmithGuiListener;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class BlacksmithCommand implements CommandExecutor {
    private final BlacksmithGuiListener listener;

    public BlacksmithCommand(BlacksmithGuiListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use the blacksmith.");
            return true;
        }
        listener.open(player);
        return true;
    }
}
