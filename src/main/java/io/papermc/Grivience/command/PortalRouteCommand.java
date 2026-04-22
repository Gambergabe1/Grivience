package io.papermc.Grivience.command;

import io.papermc.Grivience.skyblock.portal.PortalRoutingManager;
import io.papermc.Grivience.skyblock.portal.PortalRoutingManager.PortalKind;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PortalRouteCommand implements CommandExecutor, TabCompleter {
    private final PortalRoutingManager portalRoutingManager;

    public PortalRouteCommand(PortalRoutingManager portalRoutingManager) {
        this.portalRoutingManager = portalRoutingManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendInfo(sender);
            sendHelp(sender, label);
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        return switch (subcommand) {
            case "set" -> handleSet(sender, label, args);
            case "disable" -> handleDisable(sender, args);
            case "info", "status" -> handleInfo(sender, args);
            case "help" -> {
                sendHelp(sender, label);
                yield true;
            }
            default -> {
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /" + label + " help.");
                yield true;
            }
        };
    }

    private boolean handleSet(CommandSender sender, String label, String[] args) {
        ResolvedPortalContext context = resolvePortalContext(sender, args, 1, "set");
        if (context == null) {
            return true;
        }

        if (args.length <= context.targetIndex()) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " set [nether|end] <hub|minehub|farmhub|dungeonhub|island|world> [worldName]");
            return true;
        }

        PortalKind portalKind = context.portalKind();
        String target = args[context.targetIndex()].toLowerCase(Locale.ROOT);
        if ("world".equals(target)) {
            int worldNameIndex = context.targetIndex() + 1;
            if (args.length <= worldNameIndex) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " set [nether|end] world <worldName>");
                return true;
            }

            String worldName = args[worldNameIndex];
            if (!portalRoutingManager.availableWorlds().contains(worldName)) {
                sender.sendMessage(ChatColor.RED + "World '" + worldName + "' is not loaded.");
                return true;
            }

            portalRoutingManager.saveWorldTarget(portalKind, worldName);
            sender.sendMessage(ChatColor.GREEN + portalKind.displayName() + " portal route set to world '" + worldName + "'.");
            return true;
        }

        if (!portalRoutingManager.builtinTargets().contains(target)) {
            sender.sendMessage(ChatColor.RED + "Target must be hub, minehub, farmhub, dungeonhub, island, or world.");
            return true;
        }

        portalRoutingManager.saveBuiltinTarget(portalKind, target);
        sender.sendMessage(ChatColor.GREEN + portalKind.displayName() + " portal route set to " + target + ".");
        return true;
    }

    private boolean handleDisable(CommandSender sender, String[] args) {
        ResolvedPortalContext context = resolvePortalContext(sender, args, 1, "disable");
        if (context == null) {
            return true;
        }

        portalRoutingManager.setEnabled(context.portalKind(), false);
        sender.sendMessage(ChatColor.GREEN + context.portalKind().displayName() + " portal routing disabled.");
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            PortalKind portalKind = PortalKind.fromArgument(args[1]);
            if (portalKind == null) {
                sender.sendMessage(ChatColor.RED + "Portal type must be 'nether' or 'end'.");
                return true;
            }
            sendRouteInfo(sender, portalKind);
            return true;
        }

        if (sender instanceof Player player) {
            PortalKind lookedAtPortal = portalRoutingManager.detectLookedAtPortal(player);
            if (lookedAtPortal != null) {
                sendRouteInfo(sender, lookedAtPortal);
                return true;
            }
        }

        sendInfo(sender);
        return true;
    }

    private void sendInfo(CommandSender sender) {
        sendRouteInfo(sender, PortalKind.NETHER);
        sendRouteInfo(sender, PortalKind.END);
    }

    private void sendRouteInfo(CommandSender sender, PortalKind portalKind) {
        sender.sendMessage(ChatColor.GOLD + portalKind.displayName() + " Portal" + ChatColor.GRAY + ": " + portalRoutingManager.describeRoute(portalKind));
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "Portal Routing Commands");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " info" + ChatColor.GRAY + " - Show both routes, or the route for the portal you are looking at.");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " set <hub|minehub|farmhub|dungeonhub|island>" + ChatColor.GRAY + " - Look at a portal and route that portal type.");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " set world <worldName>" + ChatColor.GRAY + " - Look at a portal and route it to a world's spawn.");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " set <nether|end> ..." + ChatColor.GRAY + " - Manual fallback if you do not want look detection.");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " disable [nether|end]" + ChatColor.GRAY + " - Disable the portal you are looking at, or specify the type.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("set", "disable", "info", "help"), args[0]);
        }
        if (args.length == 2) {
            String subcommand = args[0].toLowerCase(Locale.ROOT);
            if ("set".equals(subcommand)) {
                List<String> options = new ArrayList<>(portalRoutingManager.builtinTargets());
                options.add("world");
                options.add("nether");
                options.add("end");
                return filter(options, args[1]);
            }
            if ("disable".equals(subcommand) || "info".equals(subcommand) || "status".equals(subcommand)) {
                return filter(List.of("nether", "end"), args[1]);
            }
        }
        if (args.length == 3 && "set".equalsIgnoreCase(args[0]) && PortalKind.fromArgument(args[1]) != null) {
            List<String> targets = new ArrayList<>(portalRoutingManager.builtinTargets());
            targets.add("world");
            return filter(targets, args[2]);
        }
        if (args.length == 3 && "set".equalsIgnoreCase(args[0]) && "world".equalsIgnoreCase(args[1])) {
            return filter(portalRoutingManager.availableWorlds(), args[2]);
        }
        if (args.length == 4 && "set".equalsIgnoreCase(args[0]) && "world".equalsIgnoreCase(args[2])) {
            return filter(portalRoutingManager.availableWorlds(), args[3]);
        }
        return List.of();
    }

    private ResolvedPortalContext resolvePortalContext(CommandSender sender, String[] args, int argumentIndex, String action) {
        if (args.length > argumentIndex) {
            PortalKind manualPortalKind = PortalKind.fromArgument(args[argumentIndex]);
            if (manualPortalKind != null) {
                return new ResolvedPortalContext(manualPortalKind, argumentIndex + 1);
            }
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Console must specify a portal type: /portalroute " + action + " <nether|end> ...");
            return null;
        }

        PortalKind lookedAtPortal = portalRoutingManager.detectLookedAtPortal(player);
        if (lookedAtPortal == null) {
            sender.sendMessage(ChatColor.RED + "Look directly at an End Portal or Nether Portal block/frame, or specify nether/end in the command.");
            return null;
        }

        return new ResolvedPortalContext(lookedAtPortal, argumentIndex);
    }

    private List<String> filter(List<String> input, String prefix) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        List<String> output = new ArrayList<>();
        for (String candidate : input) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(normalized)) {
                output.add(candidate);
            }
        }
        return output;
    }

    private record ResolvedPortalContext(PortalKind portalKind, int targetIndex) {
    }
}
