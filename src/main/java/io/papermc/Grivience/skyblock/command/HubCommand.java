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

public final class HubCommand implements CommandExecutor, TabCompleter {
    private final GriviencePlugin plugin;

    public HubCommand(GriviencePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }

        teleportToHub(player);
        return true;
    }

    public void teleportToHub(Player player) {
        World hubWorld = getHubWorld();
        Location hubLocation;

        if (hubWorld != null) {
            hubLocation = getSpawnLocation(hubWorld);
        } else {
            player.sendMessage(ChatColor.RED + "Hub world '" + getHubWorldName() + "' not found. Contact an administrator.");
            return;
        }

        if (hubLocation == null) {
            player.sendMessage(ChatColor.RED + "Failed to locate hub spawn. Contact an administrator.");
            return;
        }

        player.teleport(hubLocation);
        player.playSound(hubLocation, org.bukkit.Sound.ENTITY_PLAYER_TELEPORT, 1.0F, 1.0F);
        player.sendMessage(ChatColor.GREEN + "Teleported to the Hub!");
    }

    private String getHubWorldName() {
        return plugin.getConfig().getString("skyblock.hub-world", "world");
    }

    private World getHubWorld() {
        String hubWorldName = getHubWorldName();
        return Bukkit.getWorld(hubWorldName);
    }

    private Location getSpawnLocation(World world) {
        String path = "skyblock.hub-spawn";
        if (plugin.getConfig().contains(path)) {
            double x = plugin.getConfig().getDouble(path + ".x");
            double y = plugin.getConfig().getDouble(path + ".y");
            double z = plugin.getConfig().getDouble(path + ".z");
            float yaw = (float) plugin.getConfig().getDouble(path + ".yaw", 0);
            float pitch = (float) plugin.getConfig().getDouble(path + ".pitch", 0);
            return new Location(world, x + 0.5, y, z + 0.5, yaw, pitch);
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
        return List.of();
    }
}
