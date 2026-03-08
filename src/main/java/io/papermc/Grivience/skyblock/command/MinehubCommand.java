package io.papermc.Grivience.skyblock.command;

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

public final class MinehubCommand implements CommandExecutor, TabCompleter {
    private final GriviencePlugin plugin;

    public MinehubCommand(GriviencePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("set")) {
            return handleSet(sender, args);
        }

        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }

        teleportToMinehub(player);
        return true;
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
            return true;
        }

        if (!sender.hasPermission("grivience.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to set the minehub.");
            return true;
        }

        Location loc = player.getLocation();
        plugin.getConfig().set("skyblock.minehub-world", loc.getWorld().getName());
        plugin.getConfig().set("skyblock.minehub-spawn.x", loc.getX());
        plugin.getConfig().set("skyblock.minehub-spawn.y", loc.getY());
        plugin.getConfig().set("skyblock.minehub-spawn.z", loc.getZ());
        plugin.getConfig().set("skyblock.minehub-spawn.yaw", loc.getYaw());
        plugin.getConfig().set("skyblock.minehub-spawn.pitch", loc.getPitch());
        plugin.saveConfig();
        if (plugin.getFastTravelManager() != null) {
            plugin.getFastTravelManager().syncHubWarpsFromConfig();
        }

        sender.sendMessage(ChatColor.GREEN + "Minehub location set to " + loc.getWorld().getName() + " (" + 
            String.format("%.1f", loc.getX()) + ", " + String.format("%.1f", loc.getY()) + ", " + 
            String.format("%.1f", loc.getZ()) + ") yaw " + String.format("%.1f", loc.getYaw()) + 
            " pitch " + String.format("%.1f", loc.getPitch()) + ".");
        return true;
    }

    public void teleportToMinehub(Player player) {
        World minehubWorld = getMinehubWorld();
        Location minehubLocation;

        if (minehubWorld != null) {
            minehubLocation = getSpawnLocation(minehubWorld);
        } else {
            player.sendMessage(ChatColor.RED + "Minehub world '" + getMinehubWorldName() + "' not found. Contact an administrator.");
            return;
        }

        if (minehubLocation == null) {
            player.sendMessage(ChatColor.RED + "Failed to locate minehub spawn. Contact an administrator.");
            return;
        }

        player.teleport(minehubLocation);
        player.playSound(minehubLocation, org.bukkit.Sound.ENTITY_PLAYER_TELEPORT, 1.0F, 1.0F);
        player.sendMessage(ChatColor.GREEN + "Teleported to the Minehub!");
    }

    private String getMinehubWorldName() {
        return plugin.getConfig().getString("skyblock.minehub-world", "minehub_world");
    }

    private World getMinehubWorld() {
        String minehubWorldName = getMinehubWorldName();
        return Bukkit.getWorld(minehubWorldName);
    }

    private Location getSpawnLocation(World world) {
        String path = "skyblock.minehub-spawn";
        if (plugin.getConfig().contains(path)) {
            double x = plugin.getConfig().getDouble(path + ".x");
            double y = plugin.getConfig().getDouble(path + ".y");
            double z = plugin.getConfig().getDouble(path + ".z");
            float yaw = (float) plugin.getConfig().getDouble(path + ".yaw", 0);
            float pitch = (float) plugin.getConfig().getDouble(path + ".pitch", 0);
            return new Location(world, x, y, z, yaw, pitch);
        }
        return world.getSpawnLocation().add(0.5, 0, 0.5);
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
