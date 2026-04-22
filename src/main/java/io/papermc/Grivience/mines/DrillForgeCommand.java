package io.papermc.Grivience.mines;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DrillForgeCommand implements CommandExecutor, TabCompleter {
    private static final List<String> ROOT_SUBCOMMANDS = List.of("open", "projects", "overdrive", "status", "parts", "recipes", "help");

    private final DrillMechanicGui drillForgeGui;

    public DrillForgeCommand(DrillMechanicGui drillForgeGui) {
        this.drillForgeGui = drillForgeGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
            return true;
        }

        String sub = args.length == 0 ? "open" : args[0].trim().toLowerCase(Locale.ROOT);
        switch (sub) {
            case "open", "gui", "forge", "mechanic" -> {
                drillForgeGui.open(player);
                player.sendMessage(ChatColor.GREEN + "Opened Drill Forge.");
                return true;
            }
            case "projects" -> {
                drillForgeGui.openProjects(player);
                player.sendMessage(ChatColor.GREEN + "Opened Drill Forge projects.");
                return true;
            }
            case "overdrive" -> {
                drillForgeGui.openOverdrive(player);
                player.sendMessage(ChatColor.GREEN + "Opened Drill Forge overdrive chamber.");
                return true;
            }
            case "parts" -> {
                drillForgeGui.openParts(player);
                player.sendMessage(ChatColor.GREEN + "Opened Drill Forge parts locker.");
                return true;
            }
            case "status" -> {
                drillForgeGui.sendStatus(player);
                return true;
            }
            case "recipes" -> {
                sendPartsHelp(player, label);
                return true;
            }
            case "help" -> {
                sendHelp(player, label);
                return true;
            }
            default -> {
                player.sendMessage(ChatColor.RED + "Unknown subcommand. Use /" + label + " help.");
                return true;
            }
        }
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "=== Drill Forge ===");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + ChatColor.GRAY + " - Open the Drill Forge");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " projects" + ChatColor.GRAY + " - Open the forge queue");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " overdrive" + ChatColor.GRAY + " - Open the overdrive chamber");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " parts" + ChatColor.GRAY + " - Open the owned drill parts locker");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " status" + ChatColor.GRAY + " - View forge heat and queue status");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " recipes" + ChatColor.GRAY + " - View craftable drill part recipes");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " help" + ChatColor.GRAY + " - Show Drill Forge help");
        sender.sendMessage(ChatColor.GRAY + "You can also sneak-right-click an anvil while holding a drill.");
    }

    private void sendPartsHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "=== Drill Forge Parts ===");
        sender.sendMessage(ChatColor.GRAY + "Open your owned parts in /" + label + " parts.");
        sender.sendMessage(ChatColor.GRAY + "Craft these at a crafting table or forge advanced versions in /" + label + " projects:");
        sender.sendMessage(ChatColor.YELLOW + "- Mithril Engine");
        sender.sendMessage(ChatColor.YELLOW + "- Titanium Engine");
        sender.sendMessage(ChatColor.YELLOW + "- Gemstone Engine");
        sender.sendMessage(ChatColor.YELLOW + "- Divan Engine");
        sender.sendMessage(ChatColor.YELLOW + "- Medium Fuel Tank");
        sender.sendMessage(ChatColor.YELLOW + "- Large Fuel Tank");
        sender.sendMessage(ChatColor.YELLOW + "- Volta");
        sender.sendMessage(ChatColor.YELLOW + "- Oil Barrel");
        sender.sendMessage(ChatColor.GRAY + "Forge Heat from completed projects powers " + ChatColor.AQUA + "/"
                + label + " overdrive" + ChatColor.GRAY + ".");
        sender.sendMessage(ChatColor.GRAY + "Recipe guide shortcut: " + ChatColor.YELLOW
                + "/craft mithril_engine" + ChatColor.GRAY + " or " + ChatColor.YELLOW + "/craft oil_barrel");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }

        String prefix = args[0] == null ? "" : args[0].trim().toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String candidate : ROOT_SUBCOMMANDS) {
            if (candidate.startsWith(prefix)) {
                out.add(candidate);
            }
        }
        return out;
    }
}
