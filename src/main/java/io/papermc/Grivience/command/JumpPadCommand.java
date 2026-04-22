package io.papermc.Grivience.command;

import io.papermc.Grivience.jumppad.JumpPadManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class JumpPadCommand implements CommandExecutor, TabCompleter {
    private final JumpPadManager manager;

    public JumpPadCommand(JumpPadManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }
        if (!sender.hasPermission("grivience.admin")) {
            sender.sendMessage(ChatColor.RED + "You need grivience.admin to use this.");
            return true;
        }
        if (args.length < 1) {
            sendHelp(sender, label);
            return true;
        }
        String sub = args[0].toLowerCase();

        if (sub.equals("list") && args.length == 1) {
            sendList(sender);
            return true;
        }

        if (args.length < 2) {
            sendHelp(sender, label);
            return true;
        }
        String id = args[1].toLowerCase();

        switch (sub) {
            case "create", "launch" -> {
                Location loc = player.getLocation();
                manager.setLaunch(id, loc);
                sender.sendMessage(ChatColor.GREEN + "✔ Anchor set for jump pad '" + id + "' at your feet.");
                sender.sendMessage(ChatColor.GRAY + "Tip: Set a " + ChatColor.YELLOW + "target" + ChatColor.GRAY + " next!");
            }
            case "pos1" -> {
                Location loc = player.getLocation();
                manager.setPos1(id, loc);
                sender.sendMessage(ChatColor.GREEN + "✔ Corner 1 set for '" + id + "'.");
                sender.sendMessage(ChatColor.GRAY + "Now stand at the opposite corner and use " + ChatColor.YELLOW + "/jp pos2 " + id);
            }
            case "pos2" -> {
                Location loc = player.getLocation();
                manager.setPos2(id, loc);
                sender.sendMessage(ChatColor.GREEN + "✔ Corner 2 set for '" + id + "'.");
                sender.sendMessage(ChatColor.AQUA + "Launch area is now defined between the two corners.");
            }
            case "target" -> {
                Location loc = player.getLocation();
                manager.setTarget(id, loc);
                sender.sendMessage(ChatColor.GREEN + "✔ Exit point set for '" + id + "'.");
                sender.sendMessage(ChatColor.GRAY + "Facing: " + ChatColor.WHITE + String.format("%.1f, %.1f", loc.getYaw(), loc.getPitch()));
            }
            case "targetpos", "targetarea" -> {
                Location loc = player.getLocation();
                manager.setTargetCorner(id, loc);
                sender.sendMessage(ChatColor.GREEN + "✔ Target corner set for '" + id + "'.");
            }
            case "info" -> {
                JumpPadManager.JumpPad pad = manager.pad(id);
                if (pad == null) {
                    sender.sendMessage(ChatColor.RED + "Jump pad '" + id + "' not found.");
                    return true;
                }
                sendInfo(sender, id, pad);
            }
            case "remove" -> {
                manager.remove(id);
                sender.sendMessage(ChatColor.YELLOW + "✘ Removed jump pad '" + id + "'.");
            }
            case "setreq" -> {
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "Usage: /jp setreq <id> <skyblock|combat|mining|farming> <level>");
                    return true;
                }
                JumpPadManager.JumpPad pad = manager.pad(id);
                if (pad == null) {
                    sender.sendMessage(ChatColor.RED + "Jump pad '" + id + "' not found.");
                    return true;
                }
                String type = args[2].toLowerCase();
                int level;
                try {
                    level = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Level must be a number.");
                    return true;
                }

                switch (type) {
                    case "skyblock" -> pad.setMinSkyblockLevel(level);
                    case "combat" -> pad.setMinCombatLevel(level);
                    case "mining" -> pad.setMinMiningLevel(level);
                    case "farming" -> pad.setMinFarmingLevel(level);
                    default -> {
                        sender.sendMessage(ChatColor.RED + "Unknown requirement type: " + type);
                        return true;
                    }
                }
                manager.save();
                sender.sendMessage(ChatColor.GREEN + "✔ Requirement updated for '" + id + "': " + ChatColor.YELLOW + type + " Level " + level);
            }
            case "list" -> {
                sendList(sender);
            }
            default -> {
                sender.sendMessage(ChatColor.RED + "Unknown subcommand.");
                sendHelp(sender, label);
            }
        }
        return true;
    }

    private void sendInfo(CommandSender sender, String id, JumpPadManager.JumpPad pad) {
        sender.sendMessage(ChatColor.YELLOW + "--- Jump Pad: " + ChatColor.WHITE + id + ChatColor.YELLOW + " ---");
        sender.sendMessage(ChatColor.GRAY + "Status: " + (pad.isValid() ? ChatColor.GREEN + "READY" : ChatColor.RED + "INCOMPLETE (needs launch & target)"));
        sender.sendMessage(ChatColor.GRAY + "Launch: " + describeArea(pad.getLaunch(), pad.getLaunchCorner()));
        sender.sendMessage(ChatColor.GRAY + "Target: " + describeArea(pad.getTarget(), pad.getTargetCorner()));
        if (pad.getTarget() != null) {
            sender.sendMessage(ChatColor.GRAY + "World: " + ChatColor.WHITE + pad.getTarget().getWorld().getName());
        }
        
        // Requirements info
        if (pad.getMinSkyblockLevel() > 0 || pad.getMinCombatLevel() > 0 || pad.getMinMiningLevel() > 0 || pad.getMinFarmingLevel() > 0) {
            sender.sendMessage(ChatColor.YELLOW + "Requirements:");
            if (pad.getMinSkyblockLevel() > 0) sender.sendMessage(ChatColor.GRAY + " • Skyblock Level: " + ChatColor.AQUA + pad.getMinSkyblockLevel());
            if (pad.getMinCombatLevel() > 0) sender.sendMessage(ChatColor.GRAY + " • Combat Level: " + ChatColor.AQUA + pad.getMinCombatLevel());
            if (pad.getMinMiningLevel() > 0) sender.sendMessage(ChatColor.GRAY + " • Mining Level: " + ChatColor.AQUA + pad.getMinMiningLevel());
            if (pad.getMinFarmingLevel() > 0) sender.sendMessage(ChatColor.GRAY + " • Farming Level: " + ChatColor.AQUA + pad.getMinFarmingLevel());
        }
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.YELLOW + "Jump Pad Setup Guide:");
        sender.sendMessage(ChatColor.GRAY + " 1. " + ChatColor.WHITE + "/jp create <id>" + ChatColor.GRAY + " - Set the base launch block.");
        sender.sendMessage(ChatColor.GRAY + " 2. " + ChatColor.WHITE + "/jp target <id>" + ChatColor.GRAY + " - Set where players arrive.");
        sender.sendMessage(ChatColor.GRAY + " 3. " + ChatColor.WHITE + "/jp setreq <id> <type> <level>" + ChatColor.GRAY + " - Set level requirements.");
        sender.sendMessage(ChatColor.GRAY + " 4. " + ChatColor.WHITE + "/jp info <id>" + ChatColor.GRAY + "   - Check configuration status.");
        sender.sendMessage(ChatColor.GRAY + "Optional:");
        sender.sendMessage(ChatColor.GRAY + " - " + ChatColor.WHITE + "/jp pos1/pos2 <id>" + ChatColor.GRAY + " - Define a launch cuboid.");
        sender.sendMessage(ChatColor.GRAY + " - " + ChatColor.WHITE + "/jp list" + ChatColor.GRAY + "           - See all configured pads.");
    }

    private void sendList(CommandSender sender) {
        var pads = manager.allPads();
        if (pads.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No jump pads configured.");
            return;
        }

        sender.sendMessage(ChatColor.YELLOW + "Jump pads (" + pads.size() + "):");
        pads.forEach((key, pad) -> sender.sendMessage(
                (pad.isValid() ? ChatColor.GREEN : ChatColor.RED) + " • " + ChatColor.AQUA + key
                        + ChatColor.GRAY + " (" + (pad.getLaunchCorner() != null ? "Area" : "Block") + ")"
        ));
    }

    private String describeArea(Location primary, Location corner) {
        if (primary == null) return ChatColor.RED + "UNSET";
        if (corner == null) return String.format("%d, %d, %d", primary.getBlockX(), primary.getBlockY(), primary.getBlockZ());
        return String.format("%d,%d,%d to %d,%d,%d",
                primary.getBlockX(), primary.getBlockY(), primary.getBlockZ(),
                corner.getBlockX(), corner.getBlockY(), corner.getBlockZ());
    }

    private String pretty(Location loc) {
        if (loc == null || loc.getWorld() == null) return "unset";
        return loc.getWorld().getName() + " " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return List.of("create", "pos1", "pos2", "target", "targetpos", "setreq", "remove", "list", "info").stream()
                    .filter(opt -> opt.startsWith(prefix))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            String prefix = args[1].toLowerCase();
            List<String> ids = new ArrayList<>(manager.allPads().keySet());
            ids.add("default");
            return ids.stream().filter(id -> id.startsWith(prefix)).collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setreq")) {
            String prefix = args[2].toLowerCase();
            return List.of("skyblock", "combat", "mining", "farming").stream()
                    .filter(opt -> opt.startsWith(prefix))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
