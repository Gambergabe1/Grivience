package io.papermc.Grivience.skyblock.command;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.zone.ZoneManager;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class HubCommand implements CommandExecutor, TabCompleter {
    private static final String HUB_FARM_ZONE_ID = "hub_farm";

    private final GriviencePlugin plugin;
    private final Map<UUID, Location> selectionPos1 = new HashMap<>();
    private final Map<UUID, Location> selectionPos2 = new HashMap<>();

    public HubCommand(GriviencePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            switch (sub) {
                case "set" -> {
                    return handleSet(sender, args);
                }
                case "setpos1", "pos1" -> {
                    return handleSetPos1(sender);
                }
                case "setpos2", "pos2" -> {
                    return handleSetPos2(sender);
                }
                case "setarea", "area" -> {
                    return handleSetArea(sender);
                }
                case "info" -> {
                    return handleAreaInfo(sender);
                }
                default -> {
                    sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /" + label + " set|setpos1|setpos2|setarea|info");
                    return true;
                }
            }
        }

        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }

        teleportToHub(player);
        return true;
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
            return true;
        }

        if (!sender.hasPermission("grivience.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to set the hub.");
            return true;
        }

        Location loc = player.getLocation();
        plugin.getConfig().set("skyblock.hub-world", loc.getWorld().getName());
        plugin.getConfig().set("skyblock.hub-spawn.x", loc.getX());
        plugin.getConfig().set("skyblock.hub-spawn.y", loc.getY());
        plugin.getConfig().set("skyblock.hub-spawn.z", loc.getZ());
        plugin.getConfig().set("skyblock.hub-spawn.yaw", loc.getYaw());
        plugin.getConfig().set("skyblock.hub-spawn.pitch", loc.getPitch());
        plugin.saveConfig();
        if (plugin.getFastTravelManager() != null) {
            plugin.getFastTravelManager().syncHubWarpsFromConfig();
        }

        sender.sendMessage(ChatColor.GREEN + "Hub location set to " + loc.getWorld().getName() + " (" + 
            String.format("%.1f", loc.getX()) + ", " + String.format("%.1f", loc.getY()) + ", " + 
            String.format("%.1f", loc.getZ()) + ") yaw " + String.format("%.1f", loc.getYaw()) + 
            " pitch " + String.format("%.1f", loc.getPitch()) + ".");
        return true;
    }

    private boolean handleSetPos1(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
            return true;
        }
        if (!sender.hasPermission("grivience.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to set the hub farming area.");
            return true;
        }

        Location pos = player.getLocation().getBlock().getLocation();
        selectionPos1.put(player.getUniqueId(), pos);
        sender.sendMessage(ChatColor.GREEN + "Hub farming area position 1 set at " + formatBlockPos(pos) + ".");
        sender.sendMessage(ChatColor.GRAY + "Now run " + ChatColor.YELLOW + "/hub setpos2" + ChatColor.GRAY + " then " + ChatColor.YELLOW + "/hub setarea");
        return true;
    }

    private boolean handleSetPos2(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
            return true;
        }
        if (!sender.hasPermission("grivience.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to set the hub farming area.");
            return true;
        }

        Location pos = player.getLocation().getBlock().getLocation();
        selectionPos2.put(player.getUniqueId(), pos);
        sender.sendMessage(ChatColor.GREEN + "Hub farming area position 2 set at " + formatBlockPos(pos) + ".");
        sender.sendMessage(ChatColor.GRAY + "Run " + ChatColor.YELLOW + "/hub setarea" + ChatColor.GRAY + " to apply.");
        return true;
    }

    private boolean handleSetArea(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
            return true;
        }
        if (!sender.hasPermission("grivience.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to set the hub farming area.");
            return true;
        }

        Location pos1 = selectionPos1.get(player.getUniqueId());
        Location pos2 = selectionPos2.get(player.getUniqueId());
        if (pos1 == null || pos2 == null) {
            sender.sendMessage(ChatColor.RED + "Set both positions first: /hub setpos1 and /hub setpos2");
            return true;
        }
        if (pos1.getWorld() == null || pos2.getWorld() == null || !pos1.getWorld().equals(pos2.getWorld())) {
            sender.sendMessage(ChatColor.RED + "Both positions must be in the same world.");
            return true;
        }

        String areaBase = "skyblock.hub-crop-area.";
        plugin.getConfig().set(areaBase + "enabled", true);
        plugin.getConfig().set(areaBase + "world", pos1.getWorld().getName());
        plugin.getConfig().set(areaBase + "pos1.x", pos1.getBlockX());
        plugin.getConfig().set(areaBase + "pos1.y", pos1.getBlockY());
        plugin.getConfig().set(areaBase + "pos1.z", pos1.getBlockZ());
        plugin.getConfig().set(areaBase + "pos2.x", pos2.getBlockX());
        plugin.getConfig().set(areaBase + "pos2.y", pos2.getBlockY());
        plugin.getConfig().set(areaBase + "pos2.z", pos2.getBlockZ());
        plugin.saveConfig();
        syncHubZone(pos1, pos2);

        sender.sendMessage(ChatColor.GREEN + "Hub farming area saved.");
        sender.sendMessage(ChatColor.GRAY + "World: " + ChatColor.YELLOW + pos1.getWorld().getName());
        sender.sendMessage(ChatColor.GRAY + "Pos1: " + ChatColor.YELLOW + formatBlockPos(pos1));
        sender.sendMessage(ChatColor.GRAY + "Pos2: " + ChatColor.YELLOW + formatBlockPos(pos2));
        return true;
    }

    private boolean handleAreaInfo(CommandSender sender) {
        String areaBase = "skyblock.hub-crop-area.";
        boolean enabled = plugin.getConfig().getBoolean(areaBase + "enabled", false);
        String world = plugin.getConfig().getString(areaBase + "world", "unset");
        int x1 = plugin.getConfig().getInt(areaBase + "pos1.x");
        int y1 = plugin.getConfig().getInt(areaBase + "pos1.y");
        int z1 = plugin.getConfig().getInt(areaBase + "pos1.z");
        int x2 = plugin.getConfig().getInt(areaBase + "pos2.x");
        int y2 = plugin.getConfig().getInt(areaBase + "pos2.y");
        int z2 = plugin.getConfig().getInt(areaBase + "pos2.z");
        long cropDelayTicks = plugin.getConfig().getLong("skyblock.hub-crop-regen.delay-ticks", 60L);
        long treeDelayTicks = plugin.getConfig().getLong("skyblock.hub-tree-regen.delay-ticks", 100L);
        boolean growthEnabled = plugin.getConfig().getBoolean("skyblock.hub-crop-growth.enabled", true);
        boolean farmlandHydrated = plugin.getConfig().getBoolean("skyblock.hub-farmland.force-hydrated", true);
        boolean mobsEnabled = plugin.getConfig().getBoolean("skyblock.hub-features.enable-mob-spawning", true);

        sender.sendMessage(ChatColor.GOLD + "Hub Farming Area");
        sender.sendMessage(ChatColor.GRAY + "Enabled: " + (enabled ? ChatColor.GREEN + "true" : ChatColor.RED + "false"));
        sender.sendMessage(ChatColor.GRAY + "World: " + ChatColor.YELLOW + world);
        sender.sendMessage(ChatColor.GRAY + "Pos1: " + ChatColor.YELLOW + x1 + ", " + y1 + ", " + z1);
        sender.sendMessage(ChatColor.GRAY + "Pos2: " + ChatColor.YELLOW + x2 + ", " + y2 + ", " + z2);
        sender.sendMessage(ChatColor.GRAY + "Crop Regen Delay: " + ChatColor.YELLOW + cropDelayTicks + " ticks");
        sender.sendMessage(ChatColor.GRAY + "Tree Regen Delay: " + ChatColor.YELLOW + treeDelayTicks + " ticks");
        sender.sendMessage(ChatColor.GRAY + "Growth Boost: " + (growthEnabled ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
        sender.sendMessage(ChatColor.GRAY + "Farmland Forced Hydrated: " + (farmlandHydrated ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
        sender.sendMessage(ChatColor.GRAY + "Mob Spawning: " + (mobsEnabled ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
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

    private void syncHubZone(Location pos1, Location pos2) {
        ZoneManager zoneManager = plugin.getZoneManager();
        if (zoneManager == null || pos1 == null || pos2 == null) {
            return;
        }

        if (!zoneManager.hasZone(HUB_FARM_ZONE_ID)) {
            zoneManager.createZone(HUB_FARM_ZONE_ID, "hub_farm", "Hub");
        }

        zoneManager.updateZoneBounds(HUB_FARM_ZONE_ID, pos1, pos2);
        zoneManager.setZoneDisplay(HUB_FARM_ZONE_ID, "Hub", ChatColor.YELLOW, 96);
        zoneManager.setZoneDescription(HUB_FARM_ZONE_ID, "Hub farming and fishing area.");
        zoneManager.setZoneEnabled(HUB_FARM_ZONE_ID, true);
    }

    private String formatBlockPos(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return "unset";
        }
        return loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + " (" + loc.getWorld().getName() + ")";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("set", "setpos1", "setpos2", "setarea", "info"), args[0]);
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
