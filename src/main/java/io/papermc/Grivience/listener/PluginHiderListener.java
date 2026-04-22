package io.papermc.Grivience.listener;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.server.TabCompleteEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class PluginHiderListener implements Listener {
    private static final String BASE_PATH = "security.plugin-hider.";
    private static final List<String> DEFAULT_BLOCKED_COMMANDS = List.of(
            "pl",
            "plugins",
            "bukkit:pl",
            "bukkit:plugins",
            "ver",
            "version",
            "bukkit:ver",
            "bukkit:version",
            "paper:plugins",
            "paper:version",
            "spigot:plugins",
            "spigot:version",
            "icanhasbukkit"
    );

    private final GriviencePlugin plugin;

    public PluginHiderListener(GriviencePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (!isEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        if (hasBypass(player)) {
            return;
        }

        String commandRoot = extractCommandRoot(event.getMessage());
        if (!isBlockedCommand(commandRoot)) {
            return;
        }

        event.setCancelled(true);
        player.sendMessage(colorize(blockMessage()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandSend(PlayerCommandSendEvent event) {
        if (!isEnabled() || hasBypass(event.getPlayer())) {
            return;
        }
        event.getCommands().removeIf(this::isBlockedCommand);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTabComplete(TabCompleteEvent event) {
        if (!isEnabled()) {
            return;
        }
        CommandSender sender = event.getSender();
        if (!(sender instanceof Player player) || hasBypass(player)) {
            return;
        }

        String root = extractCommandRoot(event.getBuffer());
        if (isBlockedCommand(root)) {
            event.setCompletions(List.of());
            event.setCancelled(true);
            return;
        }

        List<String> original = event.getCompletions();
        if (original.isEmpty()) {
            return;
        }

        List<String> filtered = new ArrayList<>(original.size());
        for (String completion : original) {
            String normalized = normalizeCommandToken(completion);
            if (isBlockedCommand(normalized)) {
                continue;
            }
            filtered.add(completion);
        }

        if (filtered.size() != original.size()) {
            event.setCompletions(filtered);
        }
    }

    private boolean isEnabled() {
        return plugin.getConfig().getBoolean(BASE_PATH + "enabled", true);
    }

    private boolean hasBypass(CommandSender sender) {
        String permission = plugin.getConfig().getString(
                BASE_PATH + "bypass-permission",
                "grivience.pluginhider.bypass"
        );
        return permission != null && !permission.isBlank() && sender.hasPermission(permission);
    }

    private String blockMessage() {
        return plugin.getConfig().getString(
                BASE_PATH + "block-message",
                "&cUnknown command. Type \"/help\" for help."
        );
    }

    private Set<String> blockedCommands() {
        List<String> configured = plugin.getConfig().getStringList(BASE_PATH + "blocked-commands");
        if (configured == null || configured.isEmpty()) {
            configured = DEFAULT_BLOCKED_COMMANDS;
        }

        Set<String> blocked = new HashSet<>();
        for (String raw : configured) {
            String normalized = normalizeCommandToken(raw);
            if (!normalized.isEmpty()) {
                blocked.add(normalized);
            }
        }
        return blocked;
    }

    private boolean isBlockedCommand(String commandRootRaw) {
        String commandRoot = normalizeCommandToken(commandRootRaw);
        if (commandRoot.isEmpty()) {
            return false;
        }

        Set<String> blocked = blockedCommands();
        if (blocked.contains(commandRoot)) {
            return true;
        }

        int namespaceSeparator = commandRoot.indexOf(':');
        if (namespaceSeparator >= 0 && namespaceSeparator + 1 < commandRoot.length()) {
            String plain = commandRoot.substring(namespaceSeparator + 1);
            if (blocked.contains(plain)) {
                return true;
            }
        }
        return false;
    }

    private String extractCommandRoot(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            return "";
        }
        String normalized = rawInput.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        int firstSpace = normalized.indexOf(' ');
        if (firstSpace >= 0) {
            normalized = normalized.substring(0, firstSpace);
        }
        return normalizeCommandToken(normalized);
    }

    private String normalizeCommandToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return "";
        }

        String normalized = rawToken.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        int firstSpace = normalized.indexOf(' ');
        if (firstSpace >= 0) {
            normalized = normalized.substring(0, firstSpace);
        }

        return normalized.toLowerCase(Locale.ROOT);
    }

    private String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }
}
