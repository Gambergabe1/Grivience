package io.papermc.Grivience.command;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DungeonHubCommand implements CommandExecutor, TabCompleter {
    private final GriviencePlugin plugin;

    public DungeonHubCommand(GriviencePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("set")) {
            if (!sender.hasPermission("grivience.admin")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to set the dungeon hub.");
                return true;
            }
            return handleSet(sender, args);
        }

        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        teleportToDungeonHub(player);
        return true;
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /dungeonhub set <world> <x> <y> <z> [yaw] [pitch]");
            return true;
        }

        String worldName = args[1];
        double x, y, z;
        float yaw = 0f, pitch = 0f;
        try {
            x = Double.parseDouble(args[2]);
            y = Double.parseDouble(args[3]);
            z = Double.parseDouble(args[4]);
            if (args.length >= 6) {
                yaw = Float.parseFloat(args[5]);
            }
            if (args.length >= 7) {
                pitch = Float.parseFloat(args[6]);
            }
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Coordinates must be numbers.");
            return true;
        }

        plugin.getConfig().set("dungeons.hub.world", worldName);
        plugin.getConfig().set("dungeons.hub.x", x);
        plugin.getConfig().set("dungeons.hub.y", y);
        plugin.getConfig().set("dungeons.hub.z", z);
        plugin.getConfig().set("dungeons.hub.yaw", yaw);
        plugin.getConfig().set("dungeons.hub.pitch", pitch);
        plugin.saveConfig();

        sender.sendMessage(ChatColor.GREEN + "Dungeon Hub set to " + worldName + " (" + x + ", " + y + ", " + z + ") yaw " + yaw + " pitch " + pitch + ".");
        return true;
    }

    private void teleportToDungeonHub(Player player) {
        String worldName = plugin.getConfig().getString("dungeons.hub.world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage(ChatColor.RED + "Dungeon hub world '" + worldName + "' not found.");
            return;
        }

        double x = plugin.getConfig().getDouble("dungeons.hub.x", 0.5D);
        double y = plugin.getConfig().getDouble("dungeons.hub.y", 120.0D);
        double z = plugin.getConfig().getDouble("dungeons.hub.z", 0.5D);
        float yaw = (float) plugin.getConfig().getDouble("dungeons.hub.yaw", 0.0D);
        float pitch = (float) plugin.getConfig().getDouble("dungeons.hub.pitch", 0.0D);

        Location location = new Location(world, x + 0.5, y, z + 0.5, yaw, pitch);
        player.teleport(location);
        player.playSound(location, org.bukkit.Sound.ENTITY_PLAYER_TELEPORT, 1.0F, 1.0F);
        player.sendMessage(ChatColor.GREEN + "Teleported to the Dungeon Hub.");
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("set"), args[0]);
        }
        return List.of();
    }

    private List<String> filter(List<String> input, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String candidate : input) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(p)) {
                out.add(candidate);
            }
        }
        return out;
    }
}
