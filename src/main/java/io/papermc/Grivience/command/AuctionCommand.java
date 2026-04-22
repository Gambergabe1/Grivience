package io.papermc.Grivience.command;

import io.papermc.Grivience.auction.gui.AuctionGuiManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AuctionCommand implements CommandExecutor {

    private final AuctionGuiManager guiManager;

    public AuctionCommand(AuctionGuiManager guiManager) {
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        guiManager.openBrowser(player);
        return true;
    }
}
