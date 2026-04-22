package io.papermc.Grivience.command;

import io.papermc.Grivience.GriviencePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class DiscordCommand implements CommandExecutor {
    private final GriviencePlugin plugin;

    public DiscordCommand(GriviencePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String link = plugin.getConfig().getString("social.discord-link", "https://discord.gg/yourlink");

        Component message = Component.text()
                .append(Component.newline())
                .append(Component.text("  \u2740 ", NamedTextColor.LIGHT_PURPLE))
                .append(Component.text("Grivience Community", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" \u2740  ", NamedTextColor.LIGHT_PURPLE))
                .append(Component.newline())
                .append(Component.text("  \u2502", NamedTextColor.DARK_GRAY))
                .append(Component.newline())
                .append(Component.text("  \u2514\u2574 ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Join our Discord for updates & support!", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("    ", NamedTextColor.DARK_GRAY))
                .append(Component.text(link, NamedTextColor.AQUA, TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.openUrl(link))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to join our Discord server!", NamedTextColor.YELLOW))))
                .append(Component.newline())
                .build();

        sender.sendMessage(message);
        return true;
    }
}
