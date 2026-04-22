package io.papermc.Grivience.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Safe command-dispatch helpers.
 */
public final class CommandDispatchUtil {
    private CommandDispatchUtil() {
    }

    public static boolean dispatchConsole(JavaPlugin plugin, String rawCommand) {
        String command = sanitize(rawCommand);
        if (command == null) {
            warn(plugin, "Rejected unsafe console command payload.");
            return false;
        }
        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    public static boolean dispatchPlayer(JavaPlugin plugin, Player player, String rawCommand) {
        if (player == null) {
            return false;
        }
        String command = sanitize(rawCommand);
        if (command == null) {
            warn(plugin, "Rejected unsafe player command payload.");
            return false;
        }
        return player.performCommand(command);
    }

    private static String sanitize(String rawCommand) {
        if (rawCommand == null) {
            return null;
        }
        String command = rawCommand.trim();
        while (command.startsWith("/")) {
            command = command.substring(1).trim();
        }
        if (command.isEmpty()) {
            return null;
        }
        for (int i = 0; i < command.length(); i++) {
            if (Character.isISOControl(command.charAt(i))) {
                return null;
            }
        }
        return command;
    }

    private static void warn(JavaPlugin plugin, String message) {
        if (plugin != null && message != null && !message.isBlank()) {
            plugin.getLogger().warning(message);
        }
    }
}
