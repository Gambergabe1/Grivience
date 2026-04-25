package io.papermc.Grivience.elevator;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ElevatorCommand implements CommandExecutor, TabCompleter {
    private final io.papermc.Grivience.GriviencePlugin plugin;
    private final ElevatorManager manager;
    private final ElevatorGui gui;

    public ElevatorCommand(io.papermc.Grivience.GriviencePlugin plugin, ElevatorManager manager, ElevatorGui gui) {
        this.plugin = plugin;
        this.manager = manager;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (sender.hasPermission("grivience.admin")) {
                sender.sendMessage(ChatColor.RED + "Usage: /elevator <create|addfloor|remove|open>");
            } else {
                sender.sendMessage(ChatColor.RED + "Usage: /elevator open <id>");
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("open")) {
            if (!(sender instanceof Player player)) return true;
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /elevator open <id>");
                return true;
            }
            Elevator elevator = manager.getElevator(args[1]);
            if (elevator == null) {
                sender.sendMessage(ChatColor.RED + "Elevator not found.");
                return true;
            }
            gui.open(player, elevator);
            return true;
        }

        if (!sender.hasPermission("grivience.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        if (sub.equals("create")) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /elevator create <id> <name>");
                return true;
            }
            manager.createElevator(args[1], args[2]);
            sender.sendMessage(ChatColor.GREEN + "Elevator '" + args[1] + "' created.");
        } else if (sub.equals("addfloor")) {
            if (!(sender instanceof Player player)) return true;
            if (args.length < 4) {
                sender.sendMessage(ChatColor.RED + "Usage: /elevator addfloor <id> <floor_name> <material_icon> [required_layer]");
                return true;
            }
            Elevator elevator = manager.getElevator(args[1]);
            if (elevator == null) {
                sender.sendMessage(ChatColor.RED + "Elevator not found.");
                return true;
            }
            try {
                Material icon = Material.valueOf(args[3].toUpperCase());
                String reqLayer = args.length >= 5 ? String.join(" ", java.util.Arrays.copyOfRange(args, 4, args.length)) : "";
                elevator.addFloor(new ElevatorFloor(ChatColor.translateAlternateColorCodes('&', args[2]), new ItemStack(icon), player.getLocation(), reqLayer));
                manager.save();
                sender.sendMessage(ChatColor.GREEN + "Floor added to " + args[1] + (reqLayer.isEmpty() ? "." : " with requirement: " + reqLayer));
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "Invalid material icon.");
            }
        } else if (sub.equals("setrequirement")) {
            if (args.length < 4) {
                sender.sendMessage(ChatColor.RED + "Usage: /elevator setrequirement <id> <floor_index> <layer_name|none>");
                return true;
            }
            Elevator elevator = manager.getElevator(args[1]);
            if (elevator == null) {
                sender.sendMessage(ChatColor.RED + "Elevator not found.");
                return true;
            }
            try {
                int index = Integer.parseInt(args[2]);
                if (index < 0 || index >= elevator.getFloors().size()) {
                    sender.sendMessage(ChatColor.RED + "Invalid floor index.");
                    return true;
                }
                String req = args.length >= 4 ? String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length)) : "";
                if (req.equalsIgnoreCase("none")) req = "";
                
                ElevatorFloor old = elevator.getFloors().get(index);
                elevator.getFloors().set(index, new ElevatorFloor(old.name(), old.icon(), old.location(), req));
                manager.save();
                sender.sendMessage(ChatColor.GREEN + "Updated requirement for floor #" + index + ".");
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid index.");
            }
        } else if (sub.equals("remove")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /elevator remove <id>");
                return true;
            }
            manager.removeElevator(args[1]);
            sender.sendMessage(ChatColor.GREEN + "Elevator '" + args[1] + "' removed.");
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        boolean isAdmin = sender.hasPermission("grivience.admin");

        if (args.length == 1) {
            String low = args[0].toLowerCase();
            if (isAdmin && "create".startsWith(low)) completions.add("create");
            if (isAdmin && "addfloor".startsWith(low)) completions.add("addfloor");
            if (isAdmin && "setrequirement".startsWith(low)) completions.add("setrequirement");
            if (isAdmin && "remove".startsWith(low)) completions.add("remove");
            if ("open".startsWith(low)) completions.add("open");
        } else if (args.length == 2 && ((isAdmin && (args[0].equalsIgnoreCase("addfloor") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("setrequirement"))) || args[0].equalsIgnoreCase("open"))) {
            String low = args[1].toLowerCase();
            for (String id : manager.getElevators().keySet()) {
                if (id.startsWith(low)) completions.add(id);
            }
        } else if (isAdmin && args.length == 4 && args[0].equalsIgnoreCase("addfloor")) {
            String low = args[3].toLowerCase();
            for (Material mat : Material.values()) {
                if (mat.name().toLowerCase().startsWith(low)) completions.add(mat.name());
                if (completions.size() > 50) break;
            }
        } else if (isAdmin && ((args.length >= 5 && args[0].equalsIgnoreCase("addfloor")) || (args.length >= 4 && args[0].equalsIgnoreCase("setrequirement")))) {
            // Provide layer names for the requirement
            String input = String.join(" ", java.util.Arrays.copyOfRange(args, args[0].equalsIgnoreCase("addfloor") ? 4 : 3, args.length)).toLowerCase();
            
            List<String> layers = new ArrayList<>();
            layers.add("none");
            
            if (plugin.getMinehubOreListener() != null) {
                layers.addAll(plugin.getMinehubOreListener().getAllLayerNames());
            }
            if (plugin.getEndMinesManager() != null) {
                layers.addAll(plugin.getEndMinesManager().getAllZoneNames());
            }
            
            for (String layer : layers) {
                if (layer.toLowerCase().startsWith(input)) {
                    completions.add(layer);
                }
            }
        }
        return completions;
    }
}
