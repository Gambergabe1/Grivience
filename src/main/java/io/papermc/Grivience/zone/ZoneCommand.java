package io.papermc.Grivience.zone;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Admin command for managing zones.
 * Allows creating, editing, and removing zone areas.
 */
public final class ZoneCommand implements CommandExecutor, TabCompleter {
    private final GriviencePlugin plugin;
    private final ZoneManager zoneManager;

    public ZoneCommand(GriviencePlugin plugin, ZoneManager zoneManager) {
        this.plugin = plugin;
        this.zoneManager = zoneManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("grivience.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help" -> {
                sendHelp(sender, label);
                return true;
            }
            case "create" -> handleCreate(sender, args);
            case "delete", "remove" -> handleDelete(sender, args);
            case "setpos1" -> handleSetPos1(sender, args);
            case "setpos2" -> handleSetPos2(sender, args);
            case "setname" -> handleSetName(sender, args);
            case "setdisplayname" -> handleSetDisplayName(sender, args);
            case "setcolor" -> handleSetColor(sender, args);
            case "setpriority" -> handleSetPriority(sender, args);
            case "setenabled" -> handleSetEnabled(sender, args);
            case "setpvp" -> handleSetPvP(sender, args);
            case "setmobspawn" -> handleSetMobSpawn(sender, args);
            case "setbreak" -> handleSetBreak(sender, args);
            case "setradius" -> handleSetRadius(sender, args);
            case "attach" -> handleAttach(sender, args);
            case "setexpiry" -> handleSetExpiry(sender, args);
            case "setdesc" -> handleSetDesc(sender, args);
            case "info" -> handleInfo(sender, args);
            case "list" -> handleList(sender);
            case "reload" -> handleReload(sender);
            case "select" -> handleSelect(sender, args);
            case "wand" -> handleWand(sender);
            default -> {
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /" + label + " help.");
                return true;
            }
        }

        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "=== Zone Editor Help ===");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " create <id> [name] [displayName]" + ChatColor.GRAY + " - Create a new zone");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " delete <id>" + ChatColor.GRAY + " - Delete a zone");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " select <id>" + ChatColor.GRAY + " - Select a zone for editing");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " setpos1" + ChatColor.GRAY + " - Set position 1 (current location)");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " setpos2" + ChatColor.GRAY + " - Set position 2 (current location)");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " setname <name>" + ChatColor.GRAY + " - Set zone internal name");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " setdisplayname <name>" + ChatColor.GRAY + " - Set display name");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " setcolor <color>" + ChatColor.GRAY + " - Set display color");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " setpriority <number>" + ChatColor.GRAY + " - Set priority (higher = override)");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " setenabled <true|false>" + ChatColor.GRAY + " - Enable/disable zone");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " setpvp <id> <true|false>" + ChatColor.GRAY + " - Allow/deny PvP");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " setmobspawn <id> <true|false>" + ChatColor.GRAY + " - Allow/deny mob spawning");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " setbreak <id> <true|false>" + ChatColor.GRAY + " - Allow/deny block breaking");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " setradius <id> <radius>" + ChatColor.GRAY + " - Set spherical radius (-1 to disable)");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " attach <id> [player_name]" + ChatColor.GRAY + " - Attach zone to an entity/player");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " setexpiry <id> <seconds>" + ChatColor.GRAY + " - Set temporary lifetime");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " setdesc <description>" + ChatColor.GRAY + " - Set description");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " info [id]" + ChatColor.GRAY + " - View zone info");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " list" + ChatColor.GRAY + " - List all zones");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload" + ChatColor.GRAY + " - Reload zones from config");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " wand" + ChatColor.GRAY + " - Get zone selection wand");
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /zone create <id> [name] [displayName]");
            return;
        }

        String id = args[1];
        if (zoneManager.hasZone(id)) {
            sender.sendMessage(ChatColor.RED + "A zone with ID '" + id + "' already exists.");
            return;
        }

        String name = args.length > 2 ? args[2] : id;
        String displayName = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : name;

        Zone zone = zoneManager.createZone(id, name, displayName);
        if (zone == null) {
            sender.sendMessage(ChatColor.RED + "Failed to create zone.");
            return;
        }

        sender.sendMessage(ChatColor.GREEN + "Zone created successfully!");
        sender.sendMessage(ChatColor.GRAY + "ID: " + ChatColor.YELLOW + id);
        sender.sendMessage(ChatColor.GRAY + "Name: " + ChatColor.YELLOW + name);
        sender.sendMessage(ChatColor.GRAY + "Display: " + ChatColor.YELLOW + displayName);
        sender.sendMessage(ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/zone select " + id + ChatColor.GRAY + " to start editing bounds.");
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /zone delete <id>");
            return;
        }

        String id = args[1];
        Zone removed = zoneManager.removeZone(id);
        if (removed == null) {
            sender.sendMessage(ChatColor.RED + "Zone '" + id + "' not found.");
            return;
        }

        sender.sendMessage(ChatColor.GREEN + "Zone '" + id + "' deleted successfully.");
    }

    private void handleSelect(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /zone select <id>");
            return;
        }

        String id = args[1];
        Zone zone = zoneManager.getZone(id);
        if (zone == null) {
            sender.sendMessage(ChatColor.RED + "Zone '" + id + "' not found.");
            return;
        }

        zoneManager.setPlayerSelection(player, id);
        player.sendMessage(ChatColor.GREEN + "Selected zone: " + zone.getColoredDisplayName());
        player.sendMessage(ChatColor.GRAY + "Use /zone setpos1 and /zone setpos2 to set bounds.");
    }

    private void handleSetPos1(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
            return;
        }

        String selectedId = zoneManager.getPlayerSelection(player);
        if (selectedId == null) {
            sender.sendMessage(ChatColor.RED + "No zone selected. Use /zone select <id> first.");
            return;
        }

        Zone zone = zoneManager.getZone(selectedId);
        if (zone == null) {
            sender.sendMessage(ChatColor.RED + "Selected zone no longer exists.");
            zoneManager.setPlayerSelection(player, null);
            return;
        }

        Location pos1 = player.getLocation().clone();
        zoneManager.updateZoneBounds(selectedId, pos1, zone.getPos2());

        player.sendMessage(ChatColor.GREEN + "Position 1 set for zone " + zone.getColoredDisplayName());
        player.sendMessage(ChatColor.GRAY + "Location: " + ChatColor.YELLOW + formatLocation(pos1));
    }

    private void handleSetPos2(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
            return;
        }

        String selectedId = zoneManager.getPlayerSelection(player);
        if (selectedId == null) {
            sender.sendMessage(ChatColor.RED + "No zone selected. Use /zone select <id> first.");
            return;
        }

        Zone zone = zoneManager.getZone(selectedId);
        if (zone == null) {
            sender.sendMessage(ChatColor.RED + "Selected zone no longer exists.");
            zoneManager.setPlayerSelection(player, null);
            return;
        }

        Location pos2 = player.getLocation().clone();
        zoneManager.updateZoneBounds(selectedId, zone.getPos1(), pos2);

        player.sendMessage(ChatColor.GREEN + "Position 2 set for zone " + zone.getColoredDisplayName());
        player.sendMessage(ChatColor.GRAY + "Location: " + ChatColor.YELLOW + formatLocation(pos2));
        player.sendMessage(ChatColor.GRAY + "Zone size: " + ChatColor.YELLOW + String.format("%.0f", zone.getSize()) + " blocks³");
    }

    private void handleSetName(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /zone setname <id> <name>");
            return;
        }

        String id = args[1];
        Zone zone = zoneManager.getZone(id);
        if (zone == null) {
            sender.sendMessage(ChatColor.RED + "Zone '" + id + "' not found.");
            return;
        }

        String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        zone.setName(name);
        zoneManager.saveZones();

        sender.sendMessage(ChatColor.GREEN + "Zone name updated to: " + ChatColor.YELLOW + name);
    }

    private void handleSetDisplayName(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /zone setdisplayname <id> <displayName>");
            return;
        }

        String id = args[1];
        Zone zone = zoneManager.getZone(id);
        if (zone == null) {
            sender.sendMessage(ChatColor.RED + "Zone '" + id + "' not found.");
            return;
        }

        String displayName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        zone.setDisplayName(displayName);
        zoneManager.saveZones();

        sender.sendMessage(ChatColor.GREEN + "Display name updated to: " + zone.getColoredDisplayName());
    }

    private void handleSetColor(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /zone setcolor <id> <color>");
            sender.sendMessage(ChatColor.GRAY + "Colors: BLACK, DARK_BLUE, DARK_GREEN, DARK_AQUA, DARK_RED,");
            sender.sendMessage(ChatColor.GRAY + "       DARK_PURPLE, GOLD, GRAY, DARK_GRAY, BLUE, GREEN,");
            sender.sendMessage(ChatColor.GRAY + "       AQUA, RED, LIGHT_PURPLE, YELLOW, WHITE");
            return;
        }

        String id = args[1];
        Zone zone = zoneManager.getZone(id);
        if (zone == null) {
            sender.sendMessage(ChatColor.RED + "Zone '" + id + "' not found.");
            return;
        }

        ChatColor color;
        try {
            color = ChatColor.valueOf(args[2].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid color. Use one of: " + String.join(", ", getValidColors()));
            return;
        }

        zone.setColor(color);
        zoneManager.saveZones();

        sender.sendMessage(ChatColor.GREEN + "Color updated to: " + color + color.name());
    }

    private void handleSetPriority(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /zone setpriority <id> <number>");
            return;
        }

        String id = args[1];
        Zone zone = zoneManager.getZone(id);
        if (zone == null) {
            sender.sendMessage(ChatColor.RED + "Zone '" + id + "' not found.");
            return;
        }

        int priority;
        try {
            priority = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Priority must be a number.");
            return;
        }

        zone.setPriority(priority);
        zoneManager.saveZones();

        sender.sendMessage(ChatColor.GREEN + "Priority set to: " + ChatColor.YELLOW + priority);
        sender.sendMessage(ChatColor.GRAY + "Higher priority zones override lower ones on the scoreboard.");
    }

    private void handleSetEnabled(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /zone setenabled <id> <true|false>");
            return;
        }

        String id = args[1];
        Zone zone = zoneManager.getZone(id);
        if (zone == null) {
            sender.sendMessage(ChatColor.RED + "Zone '" + id + "' not found.");
            return;
        }

        boolean enabled = args[2].equalsIgnoreCase("true");
        zoneManager.setZoneEnabled(id, enabled);

        sender.sendMessage(ChatColor.GREEN + "Zone " + (enabled ? "enabled" : "disabled") + ".");
    }

    private void handleSetDesc(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /zone setdesc <id> <description>");
            return;
        }

        String id = args[1];
        Zone zone = zoneManager.getZone(id);
        if (zone == null) {
            sender.sendMessage(ChatColor.RED + "Zone '" + id + "' not found.");
            return;
        }

        String description = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        zoneManager.setZoneDescription(id, description);

        sender.sendMessage(ChatColor.GREEN + "Description updated.");
    }

    private void handleSetPvP(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /zone setpvp <id> <true|false>");
            return;
        }
        String id = args[1];
        Zone zone = zoneManager.getZone(id);
        if (zone == null) {
            sender.sendMessage(ChatColor.RED + "Zone '" + id + "' not found.");
            return;
        }
        boolean allow = args[2].equalsIgnoreCase("true");
        zone.setCanPvP(allow);
        zoneManager.saveZones();
        sender.sendMessage(ChatColor.GREEN + "PvP " + (allow ? "allowed" : "denied") + " in zone " + id + ".");
    }

    private void handleSetMobSpawn(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /zone setmobspawn <id> <true|false>");
            return;
        }
        String id = args[1];
        Zone zone = zoneManager.getZone(id);
        if (zone == null) {
            sender.sendMessage(ChatColor.RED + "Zone '" + id + "' not found.");
            return;
        }
        boolean allow = args[2].equalsIgnoreCase("true");
        zone.setCanSpawnMobs(allow);
        zoneManager.saveZones();
        sender.sendMessage(ChatColor.GREEN + "Mob spawning " + (allow ? "allowed" : "denied") + " in zone " + id + ".");
    }

    private void handleSetBreak(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /zone setbreak <id> <true|false>");
            return;
        }
        String id = args[1];
        Zone zone = zoneManager.getZone(id);
        if (zone == null) {
            sender.sendMessage(ChatColor.RED + "Zone '" + id + "' not found.");
            return;
        }
        boolean allow = args[2].equalsIgnoreCase("true");
        zone.setCanBreakBlocks(allow);
        zoneManager.saveZones();
        sender.sendMessage(ChatColor.GREEN + "Block breaking " + (allow ? "allowed" : "denied") + " in zone " + id + ".");
    }

    private void handleSetRadius(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /zone setradius <id> <radius>");
            sender.sendMessage(ChatColor.GRAY + "Set radius to -1 to use cuboid bounds.");
            return;
        }
        String id = args[1];
        Zone zone = zoneManager.getZone(id);
        if (zone == null) {
            sender.sendMessage(ChatColor.RED + "Zone '" + id + "' not found.");
            return;
        }
        double radius;
        try {
            radius = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Radius must be a number.");
            return;
        }
        zone.setRadius(radius);
        zoneManager.saveZones();
        sender.sendMessage(ChatColor.GREEN + "Radius for zone " + id + " set to " + radius + ".");
    }

    private void handleAttach(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /zone attach <id> [player_name|none]");
            return;
        }
        String id = args[1];
        Zone zone = zoneManager.getZone(id);
        if (zone == null) {
            sender.sendMessage(ChatColor.RED + "Zone '" + id + "' not found.");
            return;
        }
        
        if (args.length < 3 || args[2].equalsIgnoreCase("none")) {
            zone.setAttachedEntityId(null);
            zoneManager.saveZones();
            sender.sendMessage(ChatColor.GREEN + "Zone " + id + " is no longer attached to any entity.");
            return;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[2] + "' not found.");
            return;
        }

        zone.setAttachedEntityId(target.getUniqueId());
        zone.updateLocation(target.getLocation());
        zoneManager.saveZones();
        sender.sendMessage(ChatColor.GREEN + "Zone " + id + " is now attached to " + target.getName() + ".");
    }

    private void handleSetExpiry(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /zone setexpiry <id> <seconds>");
            sender.sendMessage(ChatColor.GRAY + "Set seconds to -1 for permanent.");
            return;
        }
        String id = args[1];
        Zone zone = zoneManager.getZone(id);
        if (zone == null) {
            sender.sendMessage(ChatColor.RED + "Zone '" + id + "' not found.");
            return;
        }
        long seconds;
        try {
            seconds = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Seconds must be a number.");
            return;
        }
        
        if (seconds < 0) {
            zone.setExpiryTime(-1);
        } else {
            zone.setExpiryTime(System.currentTimeMillis() + (seconds * 1000));
        }
        zoneManager.saveZones();
        sender.sendMessage(ChatColor.GREEN + "Expiry for zone " + id + " updated.");
    }

    private void handleInfo(CommandSender sender, String[] args) {
        Player player = sender instanceof Player p ? p : null;
        
        String zoneId;
        if (args.length >= 2) {
            zoneId = args[1];
        } else if (player != null) {
            Zone currentZone = zoneManager.getZoneAt(player.getLocation());
            if (currentZone == null) {
                sender.sendMessage(ChatColor.RED + "You are not in any zone. Specify a zone ID.");
                return;
            }
            zoneId = currentZone.getId();
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /zone info [id]");
            return;
        }

        Zone zone = zoneManager.getZone(zoneId);
        if (zone == null) {
            sender.sendMessage(ChatColor.RED + "Zone '" + zoneId + "' not found.");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "=== Zone Info ===");
        sender.sendMessage(ChatColor.GRAY + "ID: " + ChatColor.YELLOW + zone.getId());
        sender.sendMessage(ChatColor.GRAY + "Name: " + ChatColor.YELLOW + zone.getName());
        sender.sendMessage(ChatColor.GRAY + "Display: " + zone.getColoredDisplayName());
        sender.sendMessage(ChatColor.GRAY + "World: " + ChatColor.YELLOW + zone.getWorld());
        sender.sendMessage(ChatColor.GRAY + "Priority: " + ChatColor.YELLOW + zone.getPriority());
        sender.sendMessage(ChatColor.GRAY + "Enabled: " + ChatColor.YELLOW + zone.isEnabled());
        sender.sendMessage(ChatColor.GRAY + "PvP: " + (zone.canPvP() ? ChatColor.GREEN + "Allowed" : ChatColor.RED + "Denied"));
        sender.sendMessage(ChatColor.GRAY + "Mob Spawning: " + (zone.canSpawnMobs() ? ChatColor.GREEN + "Allowed" : ChatColor.RED + "Denied"));
        sender.sendMessage(ChatColor.GRAY + "Block Breaking: " + (zone.canBreakBlocks() ? ChatColor.GREEN + "Allowed" : ChatColor.RED + "Denied"));
        
        if (zone.getRadius() > 0) {
            sender.sendMessage(ChatColor.GRAY + "Type: " + ChatColor.AQUA + "Spherical (" + zone.getRadius() + "m)");
        } else {
            sender.sendMessage(ChatColor.GRAY + "Type: " + ChatColor.AQUA + "Cuboid");
        }

        if (zone.getAttachedEntityId() != null) {
            sender.sendMessage(ChatColor.GRAY + "Attached: " + ChatColor.YELLOW + zone.getAttachedEntityId());
        }

        if (zone.getExpiryTime() > 0) {
            long remaining = (zone.getExpiryTime() - System.currentTimeMillis()) / 1000;
            sender.sendMessage(ChatColor.GRAY + "Expires: " + ChatColor.RED + remaining + "s");
        }

        if (zone.getDescription() != null && !zone.getDescription().isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "Description: " + ChatColor.YELLOW + zone.getDescription());
        }
        
        if (zone.getPos1() != null) {
            if (zone.getRadius() > 0) {
                sender.sendMessage(ChatColor.GRAY + "Center: " + ChatColor.YELLOW + formatLocation(zone.getPos1()));
            } else if (zone.getPos2() != null) {
                sender.sendMessage(ChatColor.GRAY + "Position 1: " + ChatColor.YELLOW + formatLocation(zone.getPos1()));
                sender.sendMessage(ChatColor.GRAY + "Position 2: " + ChatColor.YELLOW + formatLocation(zone.getPos2()));
                sender.sendMessage(ChatColor.GRAY + "Size: " + ChatColor.YELLOW + String.format("%.0f", zone.getSize()) + " blocks³");
            } else {
                sender.sendMessage(ChatColor.GRAY + "Position 1: " + ChatColor.YELLOW + formatLocation(zone.getPos1()));
                sender.sendMessage(ChatColor.GRAY + "Bounds: " + ChatColor.RED + "Incomplete");
            }
        } else {
            sender.sendMessage(ChatColor.GRAY + "Bounds: " + ChatColor.RED + "Not set");
        }
    }

    private void handleList(CommandSender sender) {
        Collection<Zone> zones = zoneManager.getAllZones();
        if (zones.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No zones configured.");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "=== Zones (" + zones.size() + ") ===");
        
        zones.stream()
            .sorted((a, b) -> b.getPriority() - a.getPriority())
            .forEach(zone -> {
                String status = zone.isEnabled() ? ChatColor.GREEN + "[+]" : ChatColor.RED + "[-]";
                String bounds = (zone.getPos1() != null && zone.getPos2() != null)
                    ? ChatColor.GREEN + "SET"
                    : ChatColor.RED + "MISSING";
                sender.sendMessage(
                    status + ChatColor.GRAY + " " + 
                    zone.getColoredDisplayName() + 
                    ChatColor.GRAY + " (ID: " + zone.getId() + 
                    ", Priority: " + zone.getPriority() + 
                    ", Bounds: " + bounds + ")"
                );
            });
    }

    private void handleReload(CommandSender sender) {
        zoneManager.reload();
        sender.sendMessage(ChatColor.GREEN + "Zones reloaded. Loaded " + zoneManager.getAllZones().size() + " zones.");
    }

    private void handleWand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
            return;
        }

        // Give player a wooden axe for selection
        var item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.WOODEN_AXE);
        var meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Zone Selection Wand");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Left-click to set position 1");
        lore.add(ChatColor.GRAY + "Right-click to set position 2");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Select a zone first with /zone select <id>");
        meta.setLore(lore);
        item.setItemMeta(meta);

        player.getInventory().addItem(item);
        player.sendMessage(ChatColor.GREEN + "Zone wand added to inventory.");
        player.sendMessage(ChatColor.GRAY + "Make sure to select a zone first with /zone select <id>");
    }

    private String formatLocation(Location loc) {
        return String.format("%.1f, %.1f, %.1f in %s", loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName());
    }

    private List<String> getValidColors() {
        return Arrays.stream(ChatColor.values())
            .filter(c -> !c.isFormat() && c != ChatColor.MAGIC && c != ChatColor.RESET)
            .map(Enum::name)
            .collect(Collectors.toList());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("grivience.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> commands = Arrays.asList(
                "help", "create", "delete", "select", "setpos1", "setpos2",
                "setname", "setdisplayname", "setcolor", "setpriority",
                "setenabled", "setpvp", "setmobspawn", "setbreak", 
                "setradius", "attach", "setexpiry", "setdesc", "info", "list", "reload", "wand"
            );
            return filterPrefix(commands, args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (List.of("delete", "select", "setname", "setdisplayname", "setcolor", 
                       "setpriority", "setenabled", "setpvp", "setmobspawn", "setbreak",
                       "setradius", "attach", "setexpiry", "setdesc", "info").contains(sub)) {
                return filterPrefix(zoneManager.getZoneIds(), args[1]);
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("setcolor")) {
                return filterPrefix(getValidColors(), args[2].toUpperCase());
            }
            if (List.of("setenabled", "setpvp", "setmobspawn", "setbreak").contains(sub)) {
                return filterPrefix(Arrays.asList("true", "false"), args[2]);
            }
            if (sub.equals("attach")) {
                List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                players.add("none");
                return filterPrefix(players, args[2]);
            }
        }

        return Collections.emptyList();
    }

    private List<String> filterPrefix(

            Collection<String> input, String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        return input.stream()
            .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(lowerPrefix))
            .collect(Collectors.toList());
    }
}
