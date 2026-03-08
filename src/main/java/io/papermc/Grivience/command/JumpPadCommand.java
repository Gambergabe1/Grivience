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
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " <create|pos1|pos2|target|targetpos|remove|list> [id]");
            return true;
        }
        String sub = args[0].toLowerCase();

        if (sub.equals("list") && args.length == 1) {
            var pads = manager.allPads();
            if (pads.isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + "No jump pads configured.");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Jump pads:");
                pads.forEach((key, pad) -> {
                    String areaInfo;
                    if (pad.launch() == null) {
                        areaInfo = ChatColor.RED + " (Launch unset)";
                    } else if (pad.launchCorner() != null) {
                        areaInfo = ChatColor.GREEN + " (Area: " + pad.launch().getBlockX() + "," + pad.launch().getBlockZ()
                                + " to " + pad.launchCorner().getBlockX() + "," + pad.launchCorner().getBlockZ() + ")";
                    } else {
                        areaInfo = ChatColor.GRAY + " (Single block)";
                    }
                    sender.sendMessage(ChatColor.AQUA + " - " + key + areaInfo
                            + ChatColor.GRAY + " -> target=" + pretty(pad.target()));
                });
            }
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " <create|pos1|pos2|target|targetpos|remove|list> [id]");
            return true;
        }
        String id = args[1].toLowerCase();

        switch (sub) {
            case "create", "launch" -> {
                Location loc = player.getLocation();
                manager.setLaunch(id, loc);
                sender.sendMessage(ChatColor.GREEN + "Set launch point for jump pad '" + id + "' at your location.");
                sender.sendMessage(ChatColor.GRAY + "Tip: Use " + ChatColor.YELLOW + "/" + label + " pos1 " + id + ChatColor.GRAY + " and " + ChatColor.YELLOW + "/" + label + " pos2 " + id + ChatColor.GRAY + " to set an area.");
            }
            case "pos1" -> {
                Location loc = player.getLocation();
                manager.setPos1(id, loc);
                sender.sendMessage(ChatColor.GREEN + "Set position 1 for jump pad '" + id + "' at your location.");
                sender.sendMessage(ChatColor.GRAY + "Now use " + ChatColor.YELLOW + "/" + label + " pos2 " + id + ChatColor.GRAY + " to complete the area.");
            }
            case "pos2" -> {
                Location loc = player.getLocation();
                manager.setPos2(id, loc);
                sender.sendMessage(ChatColor.GREEN + "Set position 2 for jump pad '" + id + "' at your location.");
                sender.sendMessage(ChatColor.GREEN + "Launch area created! Players will be launched from anywhere within this area.");
            }
            case "target" -> {
                Location loc = player.getLocation();
                manager.setTarget(id, loc);
                sender.sendMessage(ChatColor.GREEN + "Set target point for jump pad '" + id + "' at your location.");
            }
            case "targetpos", "targetarea" -> {
                Location loc = player.getLocation();
                manager.setTargetCorner(id, loc);
                sender.sendMessage(ChatColor.GREEN + "Set target area corner for jump pad '" + id + "' at your location.");
            }
            case "remove" -> {
                manager.remove(id);
                sender.sendMessage(ChatColor.YELLOW + "Removed jump pad '" + id + "'.");
            }
            case "list" -> {
                var pads = manager.allPads();
                if (pads.isEmpty()) {
                    sender.sendMessage(ChatColor.GRAY + "No jump pads configured.");
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "Jump pads:");
                    pads.forEach((key, pad) -> {
                        String areaInfo;
                        if (pad.launch() == null) {
                            areaInfo = ChatColor.RED + " (Launch unset)";
                        } else if (pad.launchCorner() != null) {
                            areaInfo = ChatColor.GREEN + " (Area: " + pad.launch().getBlockX() + "," + pad.launch().getBlockZ()
                                    + " to " + pad.launchCorner().getBlockX() + "," + pad.launchCorner().getBlockZ() + ")";
                        } else {
                            areaInfo = ChatColor.GRAY + " (Single block)";
                        }
                        sender.sendMessage(ChatColor.AQUA + " - " + key + areaInfo
                                + ChatColor.GRAY + " -> target=" + pretty(pad.target()));
                    });
                }
            }
            default -> sender.sendMessage(ChatColor.RED + "Unknown subcommand. Try create|pos1|pos2|target|remove|list.");
        }
        return true;
    }

    private String pretty(Location loc) {
        if (loc == null || loc.getWorld() == null) return "unset";
        return loc.getWorld().getName() + " " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return List.of("create", "pos1", "pos2", "target", "targetpos", "remove", "list").stream()
                    .filter(opt -> opt.startsWith(prefix))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            String prefix = args[1].toLowerCase();
            List<String> ids = new ArrayList<>(manager.allPads().keySet());
            ids.add("default");
            return ids.stream().filter(id -> id.startsWith(prefix)).collect(Collectors.toList());
        }
        return List.of();
    }
}
