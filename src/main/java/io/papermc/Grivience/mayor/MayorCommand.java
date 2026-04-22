package io.papermc.Grivience.mayor;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MayorCommand implements CommandExecutor, TabCompleter {
    private final MayorManager manager;

    public MayorCommand(MayorManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /mayor <vote|status|elect>");
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("vote")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can vote.");
                return true;
            }
            Player player = (Player) sender;
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /mayor vote <candidate>");
                return true;
            }
            String candidateStr = args[1].toUpperCase();
            manager.castVote(player.getUniqueId(), candidateStr);
            player.sendMessage(ChatColor.GREEN + "You voted for " + candidateStr + "!");
            return true;
        } else if (sub.equals("status")) {
            Mayor active = manager.getActiveMayor();
            sender.sendMessage(ChatColor.GOLD + "=== Mayor Status ===");
            sender.sendMessage(ChatColor.YELLOW + "Active Mayor: " + (active != null ? ChatColor.GREEN + active.getName() : ChatColor.RED + "None"));
            if (active != null) {
                sender.sendMessage(ChatColor.YELLOW + "Active Buff: " + ChatColor.AQUA + active.getBuffDescription());
                long remaining = manager.getTermEndTime() - System.currentTimeMillis();
                sender.sendMessage(ChatColor.YELLOW + "Term Ends In: " + ChatColor.WHITE + formatTime(remaining));
            } else if (manager.isElectionActive()) {
                long remaining = manager.getElectionEndTime() - System.currentTimeMillis();
                sender.sendMessage(ChatColor.YELLOW + "Election Ends In: " + ChatColor.WHITE + formatTime(remaining));
            }
            sender.sendMessage(ChatColor.GOLD + "Current Votes:");
            Map<String, Integer> counts = manager.getVoteCounts();
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                sender.sendMessage(ChatColor.YELLOW + "- " + entry.getKey() + ": " + ChatColor.WHITE + entry.getValue());
            }
            if (sender instanceof Player) {
                Player p = (Player) sender;
                String myVote = manager.getVote(p.getUniqueId());
                sender.sendMessage(ChatColor.YELLOW + "Your vote: " + (myVote != null ? ChatColor.GREEN + myVote : ChatColor.RED + "None"));
            }
            return true;
        } else if (sub.equals("elect")) {
            if (!sender.hasPermission("grivience.mayor.elect")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to force an election.");
                return true;
            }
            manager.electMayor();
            Mayor winner = manager.getActiveMayor();
            sender.sendMessage(ChatColor.GREEN + "Election completed! New mayor is " + (winner != null ? winner.getName() : "None") + "!");
            return true;
        } else if (sub.equals("setspawn")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Only players can set the spawn.");
                return true;
            }
            if (!player.hasPermission("grivience.mayor.setspawn")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to set the mayor spawn.");
                return true;
            }
            manager.setSpawnLocation(player.getLocation());
            player.sendMessage(ChatColor.GREEN + "Mayor spawn location set to your current position!");
            return true;
        } else if (sub.equals("add")) {
            if (!sender.hasPermission("grivience.admin")) return true;
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /mayor add <name> <skin> <buff>");
                return true;
            }
            String name = args[1];
            String skin = args[2];
            String buff = String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length));
            manager.addMayor(new Mayor(name, buff, skin));
            sender.sendMessage(ChatColor.GREEN + "Added mayor " + name + "!");
            return true;
        } else if (sub.equals("addaction")) {
            if (!sender.hasPermission("grivience.admin")) return true;
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /mayor addaction <name> <command>");
                return true;
            }
            String name = args[1];
            Mayor mayor = manager.getAllMayors().get(name.toUpperCase());
            if (mayor == null) {
                sender.sendMessage(ChatColor.RED + "Mayor not found.");
                return true;
            }
            String cmd = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
            mayor.addAction(cmd);
            manager.save();
            sender.sendMessage(ChatColor.GREEN + "Added action to " + name + ": " + cmd);
            return true;
        } else if (sub.equals("clearactions")) {
            if (!sender.hasPermission("grivience.admin")) return true;
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /mayor clearactions <name>");
                return true;
            }
            String name = args[1];
            Mayor mayor = manager.getAllMayors().get(name.toUpperCase());
            if (mayor == null) {
                sender.sendMessage(ChatColor.RED + "Mayor not found.");
                return true;
            }
            mayor.clearActions();
            manager.save();
            sender.sendMessage(ChatColor.GREEN + "Cleared actions for " + name + ".");
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Unknown sub-command. Usage: /mayor <vote|status|elect|setspawn|add|addaction|clearactions>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String low = args[0].toLowerCase();
            if ("vote".startsWith(low)) completions.add("vote");
            if ("status".startsWith(low)) completions.add("status");
            if ("elect".startsWith(low) && sender.hasPermission("grivience.mayor.elect")) completions.add("elect");
            if ("setspawn".startsWith(low) && sender.hasPermission("grivience.mayor.setspawn")) completions.add("setspawn");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("vote")) {
            for (MayorCandidate candidate : MayorCandidate.values()) {
                if (candidate.name().startsWith(args[1].toUpperCase())) {
                    completions.add(candidate.name());
                }
            }
        }
        return completions;
    }

    private String formatTime(long ms) {
        if (ms <= 0) return "Soon...";
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) return days + "d " + (hours % 24) + "h";
        if (hours > 0) return hours + "h " + (minutes % 60) + "m";
        if (minutes > 0) return minutes + "m " + (seconds % 60) + "s";
        return seconds + "s";
    }
}
