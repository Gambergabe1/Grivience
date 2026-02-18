package io.papermc.Grivience.command;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.dungeon.DungeonManager;
import io.papermc.Grivience.dungeon.FloorConfig;
import io.papermc.Grivience.gui.DungeonGuiManager;
import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.party.Party;
import io.papermc.Grivience.party.PartyManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class DungeonCommand implements CommandExecutor, TabCompleter {
    private final GriviencePlugin plugin;
    private final PartyManager partyManager;
    private final DungeonManager dungeonManager;
    private final DungeonGuiManager guiManager;
    private final CustomItemService customItemService;

    public DungeonCommand(
            GriviencePlugin plugin,
            PartyManager partyManager,
            DungeonManager dungeonManager,
            DungeonGuiManager guiManager,
            CustomItemService customItemService
    ) {
        this.plugin = plugin;
        this.partyManager = partyManager;
        this.dungeonManager = dungeonManager;
        this.guiManager = guiManager;
        this.customItemService = customItemService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String commandName = command.getName().toLowerCase(Locale.ROOT);
        if (commandName.equals("party")) {
            Player player = requirePlayer(sender);
            if (player == null) {
                return true;
            }
            if (args.length == 0) {
                guiManager.openPartyFinder(player);
                return true;
            }
            handlePartyCommand(player, args, 0, "/party");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help" -> {
                sendHelp(sender);
                return true;
            }
            case "floors" -> {
                sendFloors(sender);
                return true;
            }
            case "menu", "gui" -> {
                Player player = requirePlayer(sender);
                if (player == null) {
                    return true;
                }
                guiManager.openMainMenu(player);
                return true;
            }
            case "reload" -> {
                if (!sender.hasPermission("grivience.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission.");
                    return true;
                }
                plugin.reloadSystems();
                sender.sendMessage(ChatColor.GREEN + "Grivience dungeon config reloaded.");
                return true;
            }
            case "give" -> {
                handleGiveCommand(sender, args);
                return true;
            }
            case "start" -> {
                Player player = requirePlayer(sender);
                if (player == null) {
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /dungeon start <floor>");
                    return true;
                }
                if (dungeonManager.isInDungeon(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "You are already in a dungeon run.");
                    return true;
                }
                String error = dungeonManager.startDungeon(player, args[1]);
                if (error != null) {
                    player.sendMessage(ChatColor.RED + error);
                } else {
                    player.sendMessage(ChatColor.GREEN + "Dungeon run started.");
                }
                return true;
            }
            case "abandon" -> {
                Player player = requirePlayer(sender);
                if (player == null) {
                    return true;
                }
                String error = dungeonManager.abandonDungeon(player.getUniqueId(), player.getName());
                if (error != null) {
                    player.sendMessage(ChatColor.RED + error);
                }
                return true;
            }
            case "party" -> {
                Player player = requirePlayer(sender);
                if (player == null) {
                    return true;
                }
                handlePartyCommand(player, args, 1, "/dungeon party");
                return true;
            }
            default -> {
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /dungeon help.");
                return true;
            }
        }
    }

    private void handlePartyCommand(Player player, String[] args, int actionIndex, String usageRoot) {
        if (args.length <= actionIndex) {
            player.sendMessage(ChatColor.RED + "Usage: " + usageRoot + " <create|invite|accept|leave|kick|list>");
            return;
        }

        String action = args[actionIndex].toLowerCase(Locale.ROOT);
        switch (action) {
            case "finder", "gui" -> guiManager.openPartyFinder(player);
            case "create" -> {
                if (ensurePartyUnlocked(player)) {
                    return;
                }
                String error = partyManager.createParty(player);
                if (error != null) {
                    player.sendMessage(ChatColor.RED + error);
                } else {
                    player.sendMessage(ChatColor.GREEN + "Party created. Invite players with /party invite <name>.");
                }
            }
            case "invite" -> {
                if (ensurePartyUnlocked(player)) {
                    return;
                }
                if (args.length <= actionIndex + 1) {
                    player.sendMessage(ChatColor.RED + "Usage: " + usageRoot + " invite <player>");
                    return;
                }
                Player target = Bukkit.getPlayerExact(args[actionIndex + 1]);
                if (target == null) {
                    player.sendMessage(ChatColor.RED + "That player is not online.");
                    return;
                }
                String error = partyManager.invite(player, target);
                if (error != null) {
                    player.sendMessage(ChatColor.RED + error);
                } else {
                    player.sendMessage(ChatColor.GREEN + "Invited " + target.getName() + ".");
                    target.sendMessage(
                            Component.text(player.getName(), NamedTextColor.GOLD)
                                    .append(Component.text(" invited you to a dungeon party.", NamedTextColor.YELLOW))
                    );
                    target.sendMessage(
                            Component.text("[CLICK TO JOIN]", NamedTextColor.GREEN)
                                    .clickEvent(ClickEvent.runCommand("/party accept " + player.getName()))
                                    .hoverEvent(HoverEvent.showText(Component.text(
                                            "Join " + player.getName() + "'s party",
                                            NamedTextColor.GRAY
                                    )))
                                    .append(Component.text(" or use /p accept", NamedTextColor.YELLOW))
                    );
                }
            }
            case "accept" -> {
                if (ensurePartyUnlocked(player)) {
                    return;
                }
                UUID leaderId;
                if (args.length <= actionIndex + 1) {
                    List<UUID> invites = new ArrayList<>(partyManager.getInviteSenders(player.getUniqueId()));
                    if (invites.isEmpty()) {
                        player.sendMessage(ChatColor.RED + "You have no pending invites.");
                        return;
                    }
                    if (invites.size() > 1) {
                        List<String> leaderNames = invites.stream()
                                .map(partyManager::nameOf)
                                .toList();
                        player.sendMessage(ChatColor.RED + "You have multiple invites. Use " + usageRoot + " accept <leader>.");
                        player.sendMessage(ChatColor.YELLOW + "Invites: " + String.join(", ", leaderNames));
                        return;
                    }
                    leaderId = invites.getFirst();
                } else {
                    leaderId = resolveLeaderInvite(player, args[actionIndex + 1]);
                    if (leaderId == null) {
                        player.sendMessage(ChatColor.RED + "No invite found from " + args[actionIndex + 1] + ".");
                        return;
                    }
                }

                String error = partyManager.acceptInvite(player, leaderId);
                if (error != null) {
                    player.sendMessage(ChatColor.RED + error);
                } else {
                    player.sendMessage(ChatColor.GREEN + "You joined the party.");
                    Player leader = Bukkit.getPlayer(leaderId);
                    if (leader != null) {
                        leader.sendMessage(ChatColor.GREEN + player.getName() + " joined your party.");
                    }
                }
            }
            case "leave" -> {
                if (ensurePartyUnlocked(player)) {
                    return;
                }
                String error = partyManager.leave(player);
                if (error != null) {
                    player.sendMessage(ChatColor.RED + error);
                } else {
                    player.sendMessage(ChatColor.YELLOW + "You left the party.");
                }
            }
            case "kick" -> {
                if (ensurePartyUnlocked(player)) {
                    return;
                }
                if (args.length <= actionIndex + 1) {
                    player.sendMessage(ChatColor.RED + "Usage: " + usageRoot + " kick <player>");
                    return;
                }
                UUID targetId = resolvePartyMember(player, args[actionIndex + 1]);
                if (targetId == null) {
                    player.sendMessage(ChatColor.RED + "That player is not in your party.");
                    return;
                }
                String error = partyManager.kick(player, targetId);
                if (error != null) {
                    player.sendMessage(ChatColor.RED + error);
                } else {
                    player.sendMessage(ChatColor.YELLOW + "Member removed from party.");
                    Player target = Bukkit.getPlayer(targetId);
                    if (target != null) {
                        target.sendMessage(ChatColor.RED + "You were removed from the party.");
                    }
                }
            }
            case "list" -> {
                Party party = partyManager.getParty(player.getUniqueId());
                if (party == null) {
                    player.sendMessage(ChatColor.RED + "You are not in a party.");
                    return;
                }
                for (String line : partyManager.formatParty(party)) {
                    player.sendMessage(ChatColor.GOLD + line);
                }
            }
            default -> player.sendMessage(ChatColor.RED + "Unknown party action.");
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Dungeon Commands");
        sender.sendMessage(ChatColor.YELLOW + "/dungeon floors");
        sender.sendMessage(ChatColor.YELLOW + "/dungeon start <floor>");
        sender.sendMessage(ChatColor.YELLOW + "/dungeon abandon");
        sender.sendMessage(ChatColor.YELLOW + "/dungeon menu");
        sender.sendMessage(ChatColor.YELLOW + "/dungeon party create|invite|accept [leader]|leave|kick|list");
        sender.sendMessage(ChatColor.YELLOW + "/party create|invite|accept [leader]|leave|kick|list|finder");
        if (sender.hasPermission("grivience.admin")) {
            sender.sendMessage(ChatColor.YELLOW + "/dungeon reload");
            sender.sendMessage(ChatColor.YELLOW + "/dungeon give <player> <item_id> [amount]");
        }
    }

    private void sendFloors(CommandSender sender) {
        Collection<FloorConfig> floors = dungeonManager.floors();
        if (floors.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No floors configured.");
            return;
        }
        sender.sendMessage(ChatColor.GOLD + "Configured Floors:");
        for (FloorConfig floor : floors) {
            sender.sendMessage(ChatColor.YELLOW + "- " + floor.id()
                    + ChatColor.GRAY + " (" + floor.displayName() + ", "
                    + floor.minPartySize() + "-" + floor.maxPartySize() + " players, "
                    + floor.combatRooms() + " combat, "
                    + floor.puzzleRooms() + " puzzle, "
                    + floor.treasureRooms() + " treasure)");
        }
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
        return null;
    }

    private boolean ensurePartyUnlocked(Player player) {
        if (!dungeonManager.isInDungeon(player.getUniqueId())) {
            return false;
        }
        player.sendMessage(ChatColor.RED + "Party edits are locked while in a dungeon run.");
        return true;
    }

    private UUID resolveLeaderInvite(Player player, String leaderInput) {
        for (UUID inviter : partyManager.getInviteSenders(player.getUniqueId())) {
            String name = partyManager.nameOf(inviter);
            if (name.equalsIgnoreCase(leaderInput)) {
                return inviter;
            }
        }
        return null;
    }

    private UUID resolvePartyMember(Player player, String nameInput) {
        Party party = partyManager.getParty(player.getUniqueId());
        if (party == null) {
            return null;
        }
        for (UUID memberId : party.members()) {
            String memberName = partyManager.nameOf(memberId);
            if (memberName.equalsIgnoreCase(nameInput)) {
                return memberId;
            }
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String commandName = command.getName().toLowerCase(Locale.ROOT);
        if (commandName.equals("party")) {
            return tabCompleteParty(sender, args, 0);
        }

        if (args.length == 1) {
            List<String> root = new ArrayList<>(List.of("help", "floors", "party", "start", "abandon", "menu", "gui"));
            if (sender.hasPermission("grivience.admin")) {
                root.add("reload");
                root.add("give");
            }
            return filterPrefix(root, args[0]);
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("party")) {
            return tabCompleteParty(sender, args, 1);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            return filterPrefix(dungeonManager.floorIds(), args[1]);
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("give") && sender.hasPermission("grivience.admin")) {
            if (args.length == 2) {
                List<String> players = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .toList();
                return filterPrefix(players, args[1]);
            }
            if (args.length == 3) {
                return filterPrefix(customItemService.allItemKeys(), args[2].toLowerCase(Locale.ROOT));
            }
            if (args.length == 4) {
                return filterPrefix(List.of("1", "2", "4", "8", "16", "32", "64"), args[3]);
            }
        }

        return List.of();
    }

    private List<String> tabCompleteParty(CommandSender sender, String[] args, int actionIndex) {
        if (args.length == actionIndex + 1) {
            return filterPrefix(List.of("create", "invite", "accept", "leave", "kick", "list", "finder", "gui"), args[actionIndex]);
        }
        if (!(sender instanceof Player player)) {
            return List.of();
        }

        if (args.length == actionIndex + 2) {
            String action = args[actionIndex].toLowerCase(Locale.ROOT);
            String prefix = args[actionIndex + 1];
            if (action.equals("invite")) {
                List<String> names = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .toList();
                return filterPrefix(names, prefix);
            }
            if (action.equals("accept")) {
                List<String> leaders = partyManager.getInviteSenders(player.getUniqueId()).stream()
                        .map(partyManager::nameOf)
                        .toList();
                return filterPrefix(leaders, prefix);
            }
            if (action.equals("kick")) {
                Party party = partyManager.getParty(player.getUniqueId());
                if (party == null) {
                    return List.of();
                }
                List<String> names = new ArrayList<>();
                for (UUID memberId : party.members()) {
                    names.add(partyManager.nameOf(memberId));
                }
                return filterPrefix(names, prefix);
            }
        }

        return List.of();
    }

    private List<String> filterPrefix(Collection<String> input, String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        List<String> filtered = new ArrayList<>();
        for (String candidate : input) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                filtered.add(candidate);
            }
        }
        return filtered;
    }

    private void handleGiveCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("grivience.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission.");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /dungeon give <player> <item_id> [amount]");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player must be online.");
            return;
        }

        ItemStack template = customItemService.createItemByKey(args[2]);
        if (template == null) {
            sender.sendMessage(ChatColor.RED + "Unknown custom item id: " + args[2]);
            sender.sendMessage(ChatColor.GRAY + "Try one of: " + String.join(", ", customItemService.allItemKeys()));
            return;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException ignored) {
                sender.sendMessage(ChatColor.RED + "Amount must be a number.");
                return;
            }
            if (amount <= 0) {
                sender.sendMessage(ChatColor.RED + "Amount must be at least 1.");
                return;
            }
            amount = Math.min(amount, 2304); // 36 full stacks max to avoid abuse
        }

        int maxStack = Math.max(1, template.getMaxStackSize());
        int remaining = amount;
        while (remaining > 0) {
            int stackAmount = Math.min(remaining, maxStack);
            ItemStack stack = template.clone();
            stack.setAmount(stackAmount);
            var leftovers = target.getInventory().addItem(stack);
            leftovers.values().forEach(item -> target.getWorld().dropItemNaturally(target.getLocation(), item));
            remaining -= stackAmount;
        }

        String displayName = template.hasItemMeta() && template.getItemMeta().hasDisplayName()
                ? template.getItemMeta().getDisplayName()
                : template.getType().name();
        sender.sendMessage(ChatColor.GREEN + "Gave " + amount + "x " + ChatColor.YELLOW + displayName + ChatColor.GREEN + " to " + target.getName() + ".");
        target.sendMessage(ChatColor.GOLD + "[Items] " + ChatColor.YELLOW + "You received " + amount + "x " + displayName + ChatColor.YELLOW + ".");
    }
}
