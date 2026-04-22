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

public final class FarmHubCommand implements CommandExecutor, TabCompleter {
    private static final String FARM_HUB_ZONE_ID = "farm_hub";

    private final GriviencePlugin plugin;
    private final Map<UUID, Location> selectionPos1 = new HashMap<>();
    private final Map<UUID, Location> selectionPos2 = new HashMap<>();

    public FarmHubCommand(GriviencePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            switch (sub) {
                case "set" -> {
                    return handleSetSpawn(sender);
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

        teleportToFarmHub(player);
        return true;
    }

    private boolean handleSetSpawn(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
            return true;
        }

        if (!sender.hasPermission("grivience.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to set the " + farmHubDisplayName().toLowerCase(Locale.ROOT) + ".");
            return true;
        }

        Location loc = player.getLocation();
        plugin.getConfig().set("skyblock.farmhub-world", loc.getWorld().getName());
        plugin.getConfig().set("skyblock.farmhub-spawn.x", loc.getX());
        plugin.getConfig().set("skyblock.farmhub-spawn.y", loc.getY());
        plugin.getConfig().set("skyblock.farmhub-spawn.z", loc.getZ());
        plugin.getConfig().set("skyblock.farmhub-spawn.yaw", loc.getYaw());
        plugin.getConfig().set("skyblock.farmhub-spawn.pitch", loc.getPitch());
        plugin.saveConfig();
        if (plugin.getFastTravelManager() != null) {
            plugin.getFastTravelManager().syncHubWarpsFromConfig();
        }

        sender.sendMessage(ChatColor.GREEN + farmHubDisplayName() + " location set to " + loc.getWorld().getName() + " (" +
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
            sender.sendMessage(ChatColor.RED + "You do not have permission to set the " + farmHubDisplayName().toLowerCase(Locale.ROOT) + " area.");
            return true;
        }

        Location pos = player.getLocation().getBlock().getLocation();
        selectionPos1.put(player.getUniqueId(), pos);
        sender.sendMessage(ChatColor.GREEN + farmHubDisplayName() + " area position 1 set at " + formatBlockPos(pos) + ".");
        sender.sendMessage(ChatColor.GRAY + "Now run " + ChatColor.YELLOW + "/farmhub setpos2" + ChatColor.GRAY + " then " + ChatColor.YELLOW + "/farmhub setarea");
        return true;
    }

    private boolean handleSetPos2(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
            return true;
        }
        if (!sender.hasPermission("grivience.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to set the " + farmHubDisplayName().toLowerCase(Locale.ROOT) + " area.");
            return true;
        }

        Location pos = player.getLocation().getBlock().getLocation();
        selectionPos2.put(player.getUniqueId(), pos);
        sender.sendMessage(ChatColor.GREEN + farmHubDisplayName() + " area position 2 set at " + formatBlockPos(pos) + ".");
        sender.sendMessage(ChatColor.GRAY + "Run " + ChatColor.YELLOW + "/farmhub setarea" + ChatColor.GRAY + " to apply.");
        return true;
    }

    private boolean handleSetArea(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
            return true;
        }
        if (!sender.hasPermission("grivience.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to set the " + farmHubDisplayName().toLowerCase(Locale.ROOT) + " area.");
            return true;
        }

        Location pos1 = selectionPos1.get(player.getUniqueId());
        Location pos2 = selectionPos2.get(player.getUniqueId());
        if (pos1 == null || pos2 == null) {
            sender.sendMessage(ChatColor.RED + "Set both positions first: /farmhub setpos1 and /farmhub setpos2");
            return true;
        }
        if (pos1.getWorld() == null || pos2.getWorld() == null || !pos1.getWorld().equals(pos2.getWorld())) {
            sender.sendMessage(ChatColor.RED + "Both positions must be in the same world.");
            return true;
        }

        String areaBase = "skyblock.farmhub-crop-area.";
        plugin.getConfig().set(areaBase + "enabled", true);
        plugin.getConfig().set(areaBase + "world", pos1.getWorld().getName());
        plugin.getConfig().set(areaBase + "pos1.x", pos1.getBlockX());
        plugin.getConfig().set(areaBase + "pos1.y", pos1.getBlockY());
        plugin.getConfig().set(areaBase + "pos1.z", pos1.getBlockZ());
        plugin.getConfig().set(areaBase + "pos2.x", pos2.getBlockX());
        plugin.getConfig().set(areaBase + "pos2.y", pos2.getBlockY());
        plugin.getConfig().set(areaBase + "pos2.z", pos2.getBlockZ());

        if (!plugin.getConfig().contains("skyblock.farmhub-crop-regen.enabled")) {
            plugin.getConfig().set("skyblock.farmhub-crop-regen.enabled", true);
        }
        if (!plugin.getConfig().contains("skyblock.farmhub-crop-regen.delay-ticks")) {
            plugin.getConfig().set("skyblock.farmhub-crop-regen.delay-ticks", 100L);
        }

        plugin.saveConfig();
        syncFarmHubZone(pos1, pos2);

        sender.sendMessage(ChatColor.GREEN + farmHubDisplayName() + " crop regeneration area saved.");
        sender.sendMessage(ChatColor.GRAY + "World: " + ChatColor.YELLOW + pos1.getWorld().getName());
        sender.sendMessage(ChatColor.GRAY + "Pos1: " + ChatColor.YELLOW + formatBlockPos(pos1));
        sender.sendMessage(ChatColor.GRAY + "Pos2: " + ChatColor.YELLOW + formatBlockPos(pos2));
        return true;
    }

    private boolean handleAreaInfo(CommandSender sender) {
        String areaBase = "skyblock.farmhub-crop-area.";
        boolean enabled = plugin.getConfig().getBoolean(areaBase + "enabled", false);
        String world = plugin.getConfig().getString(areaBase + "world", "unset");
        int x1 = plugin.getConfig().getInt(areaBase + "pos1.x");
        int y1 = plugin.getConfig().getInt(areaBase + "pos1.y");
        int z1 = plugin.getConfig().getInt(areaBase + "pos1.z");
        int x2 = plugin.getConfig().getInt(areaBase + "pos2.x");
        int y2 = plugin.getConfig().getInt(areaBase + "pos2.y");
        int z2 = plugin.getConfig().getInt(areaBase + "pos2.z");
        long delayTicks = plugin.getConfig().getLong("skyblock.farmhub-crop-regen.delay-ticks", 100L);
        boolean growthEnabled = plugin.getConfig().getBoolean("skyblock.farmhub-crop-growth.enabled", true);
        double growthMultiplier = plugin.getConfig().getDouble("skyblock.farmhub-crop-growth.multiplier", 3.0D);
        boolean forceGrowth = plugin.getConfig().getBoolean("skyblock.farmhub-crop-growth.force-growth", true);
        long forceGrowthInterval = plugin.getConfig().getLong("skyblock.farmhub-crop-growth.force-interval-ticks", 40L);
        boolean farmlandHydrated = plugin.getConfig().getBoolean("skyblock.farmhub-farmland.force-hydrated", true);
        long farmlandHydrateInterval = plugin.getConfig().getLong("skyblock.farmhub-farmland.hydrate-interval-ticks", 100L);
        long maintenanceScanPerRun = plugin.getConfig().getLong("skyblock.farmhub-maintenance.scan-blocks-per-run", 25000L);

        sender.sendMessage(ChatColor.GOLD + farmHubDisplayName() + " Crop Area");
        sender.sendMessage(ChatColor.GRAY + "Enabled: " + (enabled ? ChatColor.GREEN + "true" : ChatColor.RED + "false"));
        sender.sendMessage(ChatColor.GRAY + "World: " + ChatColor.YELLOW + world);
        sender.sendMessage(ChatColor.GRAY + "Pos1: " + ChatColor.YELLOW + x1 + ", " + y1 + ", " + z1);
        sender.sendMessage(ChatColor.GRAY + "Pos2: " + ChatColor.YELLOW + x2 + ", " + y2 + ", " + z2);
        sender.sendMessage(ChatColor.GRAY + "Regen Delay: " + ChatColor.YELLOW + delayTicks + " ticks");
        sender.sendMessage(ChatColor.GRAY + "Growth Boost: " + (growthEnabled ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
        sender.sendMessage(ChatColor.GRAY + "Growth Multiplier: " + ChatColor.YELLOW + String.format(Locale.US, "%.2f", growthMultiplier) + "x");
        sender.sendMessage(ChatColor.GRAY + "Forced Growth (Bypass Region Flags): " + (forceGrowth ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
        sender.sendMessage(ChatColor.GRAY + "Forced Growth Interval: " + ChatColor.YELLOW + forceGrowthInterval + " ticks");
        sender.sendMessage(ChatColor.GRAY + "Maintenance Scan/Run: " + ChatColor.YELLOW + maintenanceScanPerRun + " blocks");
        sender.sendMessage(ChatColor.GRAY + "Farmland Forced Hydrated: " + (farmlandHydrated ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
        sender.sendMessage(ChatColor.GRAY + "Farmland Hydration Interval: " + ChatColor.YELLOW + farmlandHydrateInterval + " ticks");
        return true;
    }

    public void teleportToFarmHub(Player player) {
        World farmhubWorld = getFarmhubWorld();
        Location farmhubLocation;

        if (farmhubWorld != null) {
            farmhubLocation = getSpawnLocation(farmhubWorld);
        } else {
            player.sendMessage(ChatColor.RED + farmHubDisplayName() + " world '" + getFarmhubWorldName() + "' not found. Contact an administrator.");
            return;
        }

        if (farmhubLocation == null) {
            player.sendMessage(ChatColor.RED + "Failed to locate " + farmHubDisplayName().toLowerCase(Locale.ROOT) + " spawn. Contact an administrator.");
            return;
        }

        player.teleport(farmhubLocation);
        player.playSound(farmhubLocation, org.bukkit.Sound.ENTITY_PLAYER_TELEPORT, 1.0F, 1.0F);
        player.sendMessage(ChatColor.GREEN + "Teleported to the " + farmHubDisplayName() + "!");
    }

    private String getFarmhubWorldName() {
        return plugin.getConfig().getString("skyblock.farmhub-world", "world");
    }

    private World getFarmhubWorld() {
        String farmhubWorldName = getFarmhubWorldName();
        return Bukkit.getWorld(farmhubWorldName);
    }

    private Location getSpawnLocation(World world) {
        String path = "skyblock.farmhub-spawn";
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

    private void syncFarmHubZone(Location pos1, Location pos2) {
        ZoneManager zoneManager = plugin.getZoneManager();
        if (zoneManager == null || pos1 == null || pos2 == null) {
            return;
        }

        if (!zoneManager.hasZone(FARM_HUB_ZONE_ID)) {
            zoneManager.createZone(FARM_HUB_ZONE_ID, "farm_hub", farmHubDisplayName());
        }

        zoneManager.updateZoneBounds(FARM_HUB_ZONE_ID, pos1, pos2);
        zoneManager.setZoneDisplay(FARM_HUB_ZONE_ID, farmHubDisplayName(), ChatColor.GREEN, 95);
        zoneManager.setZoneDescription(FARM_HUB_ZONE_ID, farmHubDisplayName() + " crop regeneration area.");
        zoneManager.setZoneEnabled(FARM_HUB_ZONE_ID, true);
    }

    private String farmHubDisplayName() {
        return usesHubDisplayName() ? "Hub" : "Farm Hub";
    }

    private boolean usesHubDisplayName() {
        String farmhubWorld = getFarmhubWorldName();
        String hubWorld = plugin.getConfig().getString("skyblock.hub-world", "world");
        if (farmhubWorld == null || hubWorld == null) {
            return false;
        }
        return farmhubWorld.equalsIgnoreCase(hubWorld);
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
