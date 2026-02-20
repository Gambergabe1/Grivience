package io.papermc.Grivience.command;

import io.papermc.Grivience.listener.ReforgeAnvilGuiListener;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ReforgeCommand implements CommandExecutor {
    private final ReforgeAnvilGuiListener reforgeAnvilGuiListener;

    public ReforgeCommand(ReforgeAnvilGuiListener reforgeAnvilGuiListener) {
        this.reforgeAnvilGuiListener = reforgeAnvilGuiListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
            return true;
        }

        reforgeAnvilGuiListener.openReforgeUi(player);
        return true;
    }
}
