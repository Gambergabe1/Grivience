package io.papermc.Grivience.skyblock.command;

import io.papermc.Grivience.skyblock.island.IslandManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Admin command to bypass island build restrictions.
 * Allows admins to build on any island for maintenance and support.
 */
public class IslandBypassCommand implements CommandExecutor, TabCompleter {
    private final IslandManager islandManager;
    private final Set<UUID> bypassEnabled = new HashSet<>();

    public IslandBypassCommand(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("grivience.island.bypass")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            toggleBypass(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("toggle")) {
            toggleBypass(player);
            return true;
        }

        if (subCommand.equals("on")) {
            enableBypass(player);
            return true;
        }

        if (subCommand.equals("off")) {
            disableBypass(player);
            return true;
        }

        if (subCommand.equals("status")) {
            showStatus(player);
            return true;
        }

        if (subCommand.equals("list")) {
            listBypassPlayers(player);
            return true;
        }

        sendHelp(player);
        return true;
    }

    private void toggleBypass(Player player) {
        if (bypassEnabled.contains(player.getUniqueId())) {
            disableBypass(player);
        } else {
            enableBypass(player);
        }
    }

    private void enableBypass(Player player) {
        if (bypassEnabled.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "Build bypass is already enabled!");
            return;
        }

        bypassEnabled.add(player.getUniqueId());
        player.sendMessage("");
        player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Build Bypass Enabled");
        player.sendMessage(ChatColor.GRAY + "You can now build on any island.");
        player.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.GOLD + "/islandbypass off" + ChatColor.YELLOW + " to disable.");
        player.sendMessage("");
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0F, 1.0F);
    }

    private void disableBypass(Player player) {
        if (!bypassEnabled.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "Build bypass is already disabled!");
            return;
        }

        bypassEnabled.remove(player.getUniqueId());
        player.sendMessage("");
        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Build Bypass Disabled");
        player.sendMessage(ChatColor.GRAY + "You are now subject to normal island restrictions.");
        player.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.GOLD + "/islandbypass on" + ChatColor.YELLOW + " to enable.");
        player.sendMessage("");
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0F, 0.8F);
    }

    private void showStatus(Player player) {
        boolean enabled = bypassEnabled.contains(player.getUniqueId());
        player.sendMessage("");
        player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "Build Bypass Status");
        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "Status: " + (enabled ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"));
        player.sendMessage(ChatColor.GRAY + "Permission: " + ChatColor.GREEN + "grivience.island.bypass");
        player.sendMessage("");
    }

    private void listBypassPlayers(Player player) {
        if (!player.hasPermission("grivience.island.bypass.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to view this list.");
            return;
        }

        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Active Build Bypass");
        player.sendMessage("");

        if (bypassEnabled.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "No players currently have bypass enabled.");
        } else {
            player.sendMessage(ChatColor.GREEN + "Players with bypass enabled:");
            for (UUID uuid : bypassEnabled) {
                Player p = Bukkit.getPlayer(uuid);
                String name = p != null ? p.getName() : uuid.toString();
                player.sendMessage(ChatColor.GRAY + "  • " + ChatColor.AQUA + name);
            }
        }
        player.sendMessage("");
    }

    private void sendHelp(Player player) {
        player.sendMessage("");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Island Bypass Commands");
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "/islandbypass" + ChatColor.GRAY + " - Toggle bypass on/off");
        player.sendMessage(ChatColor.YELLOW + "/islandbypass on" + ChatColor.GRAY + " - Enable bypass");
        player.sendMessage(ChatColor.YELLOW + "/islandbypass off" + ChatColor.GRAY + " - Disable bypass");
        player.sendMessage(ChatColor.YELLOW + "/islandbypass status" + ChatColor.GRAY + " - Check your status");
        player.sendMessage(ChatColor.YELLOW + "/islandbypass list" + ChatColor.GRAY + " - List active bypass (admin)");
        player.sendMessage("");
        player.sendMessage(ChatColor.AQUA + "Permission: " + ChatColor.WHITE + "grivience.island.bypass");
        player.sendMessage(ChatColor.AQUA + "Admin Permission: " + ChatColor.WHITE + "grivience.island.bypass.admin");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage("");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return new ArrayList<>();
        }

        if (!player.hasPermission("grivience.island.bypass")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String input = args[0].toLowerCase();

            if ("toggle".startsWith(input)) completions.add("toggle");
            if ("on".startsWith(input)) completions.add("on");
            if ("off".startsWith(input)) completions.add("off");
            if ("status".startsWith(input)) completions.add("status");

            if (player.hasPermission("grivience.island.bypass.admin")) {
                if ("list".startsWith(input)) completions.add("list");
            }

            return completions;
        }

        return new ArrayList<>();
    }

    /**
     * Check if a player has build bypass enabled.
     */
    public boolean hasBypass(Player player) {
        return bypassEnabled.contains(player.getUniqueId());
    }

    /**
     * Check if a player can build on an island (owner, member, or bypass).
     */
    public boolean canBuild(Player player, io.papermc.Grivience.skyblock.island.Island island) {
        if (island == null) return true; // Not on an island
        if (hasBypass(player)) return true; // Admin bypass
        if (island.getOwner() != null && island.getOwner().equals(player.getUniqueId())) return true; // Owner
        if (island.isMember(player.getUniqueId())) return true; // Member
        return false; // Visitor
    }
}
