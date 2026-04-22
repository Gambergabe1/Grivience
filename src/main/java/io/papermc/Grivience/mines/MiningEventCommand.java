package io.papermc.Grivience.mines;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class MiningEventCommand implements CommandExecutor, TabCompleter {
    private final MiningEventManager eventManager;

    public MiningEventCommand(MiningEventManager eventManager) {
        this.eventManager = eventManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("grivience.admin.mineevent")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "start" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /mineevent start <event_type> [layer]");
                    return true;
                }
                String typeStr = args[1].toUpperCase();
                MiningEventManager.MiningEvent event;
                try {
                    event = MiningEventManager.MiningEvent.valueOf(typeStr);
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid event type. Available: KINGS_INSPECTION, DEEP_CORE_BREACH, GRAND_EXTRACTION");
                    return true;
                }
                String layer = args.length > 2 ? args[2] : "Deep Core";
                eventManager.startEvent(event, layer);
                sender.sendMessage(ChatColor.GREEN + "Started event " + event.displayName() + " in " + layer);
            }
            case "stop" -> {
                eventManager.stopActiveEvent();
                sender.sendMessage(ChatColor.GREEN + "Stopped active mining event.");
            }
            case "status" -> {
                MiningEventManager.MiningEvent active = eventManager.getActiveEvent();
                if (active == null) {
                    sender.sendMessage(ChatColor.YELLOW + "No mining event is currently active.");
                } else {
                    sender.sendMessage(ChatColor.GOLD + "Active Event: " + ChatColor.YELLOW + active.displayName());
                    sender.sendMessage(ChatColor.GOLD + "Target Layer: " + ChatColor.YELLOW + eventManager.getTargetLayer());
                }
            }
            case "shop" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can open the shop.");
                    return true;
                }
                MiningEventManager.getPlugin().getInspectorShopGui().open(player);
                sender.sendMessage(ChatColor.GREEN + "Opened Inspector Shop.");
            }
            case "mechanic" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can open the menu.");
                    return true;
                }
                MiningEventManager.getPlugin().getDrillMechanicGui().open(player);
                sender.sendMessage(ChatColor.GREEN + "Opened Drill Forge.");
            }
            case "npcshop" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can open the menu.");
                    return true;
                }
                if (MiningEventManager.getPlugin().getNpcSellShopGui() == null) {
                    sender.sendMessage(ChatColor.RED + "NPC commodity shop is unavailable.");
                    return true;
                }
                MiningEventManager.getPlugin().getNpcSellShopGui().open(player);
                sender.sendMessage(ChatColor.GREEN + "Opened NPC Commodity Buyback.");
            }
            case "setnpc" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can set the NPC location.");
                    return true;
                }
                eventManager.updateNpcLocation(player.getLocation());
                sender.sendMessage(ChatColor.GREEN + "King's Inspection NPC location has been set to your current position!");
            }
            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- Mining Event Admin Commands ---");
        sender.sendMessage(ChatColor.YELLOW + "/mineevent start <type> [layer] " + ChatColor.GRAY + "- Force start an event");
        sender.sendMessage(ChatColor.YELLOW + "/mineevent stop " + ChatColor.GRAY + "- Stop the active event");
        sender.sendMessage(ChatColor.YELLOW + "/mineevent status " + ChatColor.GRAY + "- Check current event status");
        sender.sendMessage(ChatColor.YELLOW + "/mineevent shop " + ChatColor.GRAY + "- Open Inspector Shop");
        sender.sendMessage(ChatColor.YELLOW + "/mineevent mechanic " + ChatColor.GRAY + "- Open Drill Forge (admin shortcut)");
        sender.sendMessage(ChatColor.YELLOW + "/mineevent npcshop " + ChatColor.GRAY + "- Open NPC Commodity Buyback");
        sender.sendMessage(ChatColor.YELLOW + "/mineevent setnpc " + ChatColor.GRAY + "- Set King NPC location to your current position");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Stream.of("start", "stop", "status", "shop", "mechanic", "npcshop", "setnpc")
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            return Stream.of(MiningEventManager.MiningEvent.values())
                    .map(Enum::name)
                    .filter(s -> s.startsWith(args[1].toUpperCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
