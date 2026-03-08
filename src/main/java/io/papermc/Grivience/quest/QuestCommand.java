package io.papermc.Grivience.quest;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class QuestCommand implements CommandExecutor, TabCompleter {
    private final QuestManager questManager;
    private final QuestGui questGui;

    public QuestCommand(QuestManager questManager, QuestGui questGui) {
        this.questManager = questManager;
        this.questGui = questGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                questGui.openPlayerMenu(player);
            } else {
                sendHelp(sender, label);
            }
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        switch (subCommand) {
            case "menu" -> {
                Player player = requirePlayer(sender);
                if (player == null) {
                    return true;
                }
                questGui.openPlayerMenu(player);
            }
            case "gui", "editor" -> {
                Player player = requirePlayer(sender);
                if (player == null) {
                    return true;
                }
                if (player.hasPermission("grivience.admin")) {
                    questGui.openAdminMenu(player);
                } else {
                    questGui.openPlayerMenu(player);
                }
            }
            case "list" -> handleList(sender);
            case "progress" -> handleProgress(sender);
            case "start" -> handleStart(sender, args);
            case "cancel" -> handleCancel(sender, args);
            case "talk", "npc", "npctalk" -> handleTalk(sender, args);
            case "reload" -> handleReload(sender);
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "setname" -> handleSetName(sender, args);
            case "setdescription", "setdesc" -> handleSetDescription(sender, args);
            case "setstarter", "setstartnpc" -> handleSetStarter(sender, args);
            case "settarget", "settargetnpc" -> handleSetTarget(sender, args);
            case "setrepeatable" -> handleSetRepeatable(sender, args);
            case "setenabled" -> handleSetEnabled(sender, args);
            case "addreward" -> handleAddReward(sender, args);
            case "removereward" -> handleRemoveReward(sender, args);
            case "clearrewards" -> handleClearRewards(sender, args);
            case "znpcshint" -> handleZnpcsHint(sender, args);
            case "help" -> sendHelp(sender, label);
            default -> sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /" + label + " help");
        }

        return true;
    }

    private void handleList(CommandSender sender) {
        if (questManager.quests().isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No quests configured.");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "=== Conversation Quests ===");
        if (sender instanceof Player player) {
            for (ConversationQuest quest : questManager.questsSorted()) {
                sender.sendMessage(ChatColor.AQUA + "- " + quest.id() + ChatColor.GRAY + " (" + questManager.progressLabel(player.getUniqueId(), quest) + ChatColor.GRAY + ")");
                sender.sendMessage(ChatColor.DARK_GRAY + "  " + ChatColor.stripColor(questManager.color(quest.displayName())) + ChatColor.GRAY + " -> target npc: " + ChatColor.AQUA + quest.targetNpcId());
            }
            return;
        }

        for (ConversationQuest quest : questManager.questsSorted()) {
            sender.sendMessage(ChatColor.AQUA + "- " + quest.id() + ChatColor.GRAY + " | starter="
                    + (quest.hasStarterNpc() ? quest.starterNpcId() : "none")
                    + " target=" + quest.targetNpcId()
                    + " enabled=" + quest.enabled()
                    + " repeatable=" + quest.repeatable());
        }
    }

    private void handleProgress(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }

        List<ConversationQuest> active = questManager.activeQuests(player.getUniqueId());
        if (active.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No active quests.");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "=== Active Quests ===");
        for (ConversationQuest quest : active) {
            player.sendMessage(ChatColor.AQUA + "- " + quest.id() + ChatColor.GRAY + " -> talk to " + ChatColor.YELLOW + quest.targetNpcId());
        }
    }

    private void handleStart(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /quest start <questId>");
            return;
        }

        QuestManager.StartResult result = questManager.startQuest(player, args[1], QuestTriggerSource.COMMAND, true);
        switch (result) {
            case QUEST_NOT_FOUND -> sender.sendMessage(ChatColor.RED + "Quest not found.");
            case QUEST_DISABLED -> sender.sendMessage(ChatColor.RED + "Quest is disabled.");
            case ALREADY_ACTIVE -> sender.sendMessage(ChatColor.YELLOW + "Quest already active.");
            case ALREADY_COMPLETED -> sender.sendMessage(ChatColor.YELLOW + "You already completed this quest.");
            case STARTED -> {
            }
        }
    }

    private void handleCancel(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /quest cancel <questId>");
            return;
        }

        QuestManager.CancelResult result = questManager.cancelQuest(player, args[1], true);
        switch (result) {
            case QUEST_NOT_FOUND -> sender.sendMessage(ChatColor.RED + "Quest not found.");
            case NOT_ACTIVE -> sender.sendMessage(ChatColor.YELLOW + "Quest is not active.");
            case CANCELED -> {
            }
        }
    }

    private void handleTalk(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /quest talk <znpcsNpcId>");
            return;
        }

        int changed = questManager.handleNpcConversation(player, args[1], QuestTriggerSource.COMMAND, true);
        if (changed == 0) {
            sender.sendMessage(ChatColor.GRAY + "No quest state changed for NPC id '" + args[1] + "'.");
        }
    }

    private void handleReload(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return;
        }
        questManager.reload();
        sender.sendMessage(ChatColor.GREEN + "Quest files reloaded.");
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /quest create <questId> [displayName]");
            return;
        }

        String id = ConversationQuest.normalizeId(args[1]);
        String displayName = args.length >= 3 ? join(args, 2) : id;
        if (!questManager.createQuest(id, displayName)) {
            sender.sendMessage(ChatColor.RED + "Quest already exists or id is invalid.");
            return;
        }
        sender.sendMessage(ChatColor.GREEN + "Created quest: " + id);
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /quest delete <questId>");
            return;
        }
        if (!questManager.deleteQuest(args[1])) {
            sender.sendMessage(ChatColor.RED + "Quest not found.");
            return;
        }
        sender.sendMessage(ChatColor.GREEN + "Deleted quest: " + ConversationQuest.normalizeId(args[1]));
    }

    private void handleSetName(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /quest setname <questId> <displayName>");
            return;
        }
        if (!questManager.setDisplayName(args[1], join(args, 2))) {
            sender.sendMessage(ChatColor.RED + "Quest not found.");
            return;
        }
        sender.sendMessage(ChatColor.GREEN + "Display name updated.");
    }

    private void handleSetDescription(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /quest setdescription <questId> <description>");
            return;
        }
        if (!questManager.setDescription(args[1], join(args, 2))) {
            sender.sendMessage(ChatColor.RED + "Quest not found.");
            return;
        }
        sender.sendMessage(ChatColor.GREEN + "Description updated.");
    }

    private void handleSetStarter(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /quest setstarter <questId> <znpcsNpcId|none>");
            return;
        }

        String starter = args[2].equalsIgnoreCase("none") ? "" : args[2];
        if (!questManager.setStarterNpc(args[1], starter)) {
            sender.sendMessage(ChatColor.RED + "Quest not found.");
            return;
        }
        sender.sendMessage(ChatColor.GREEN + "Starter NPC updated.");
    }

    private void handleSetTarget(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /quest settarget <questId> <znpcsNpcId>");
            return;
        }
        if (!questManager.setTargetNpc(args[1], args[2])) {
            sender.sendMessage(ChatColor.RED + "Quest not found.");
            return;
        }
        sender.sendMessage(ChatColor.GREEN + "Target NPC updated.");
    }

    private void handleSetRepeatable(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /quest setrepeatable <questId> <true|false>");
            return;
        }

        Boolean value = parseBoolean(args[2]);
        if (value == null) {
            sender.sendMessage(ChatColor.RED + "Value must be true or false.");
            return;
        }

        if (!questManager.setRepeatable(args[1], value)) {
            sender.sendMessage(ChatColor.RED + "Quest not found.");
            return;
        }
        sender.sendMessage(ChatColor.GREEN + "Repeatable set to " + value + ".");
    }

    private void handleSetEnabled(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /quest setenabled <questId> <true|false>");
            return;
        }

        Boolean value = parseBoolean(args[2]);
        if (value == null) {
            sender.sendMessage(ChatColor.RED + "Value must be true or false.");
            return;
        }

        if (!questManager.setEnabled(args[1], value)) {
            sender.sendMessage(ChatColor.RED + "Quest not found.");
            return;
        }
        sender.sendMessage(ChatColor.GREEN + "Enabled set to " + value + ".");
    }

    private void handleAddReward(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /quest addreward <questId> <command>");
            return;
        }

        if (!questManager.addRewardCommand(args[1], join(args, 2))) {
            sender.sendMessage(ChatColor.RED + "Quest not found or reward command invalid.");
            return;
        }
        sender.sendMessage(ChatColor.GREEN + "Reward command added.");
    }

    private void handleRemoveReward(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /quest removereward <questId> <index>");
            return;
        }

        int index;
        try {
            index = Integer.parseInt(args[2]);
        } catch (NumberFormatException exception) {
            sender.sendMessage(ChatColor.RED + "Index must be a number (1-based).");
            return;
        }

        if (!questManager.removeRewardCommand(args[1], index - 1)) {
            sender.sendMessage(ChatColor.RED + "Quest not found or index out of range.");
            return;
        }
        sender.sendMessage(ChatColor.GREEN + "Removed reward command #" + index + ".");
    }

    private void handleClearRewards(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /quest clearrewards <questId>");
            return;
        }

        if (!questManager.clearRewardCommands(args[1])) {
            sender.sendMessage(ChatColor.RED + "Quest not found.");
            return;
        }
        sender.sendMessage(ChatColor.GREEN + "Reward commands cleared.");
    }

    private void handleZnpcsHint(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /quest znpcshint <questId>");
            return;
        }

        ConversationQuest quest = questManager.quest(args[1]);
        if (quest == null) {
            sender.sendMessage(ChatColor.RED + "Quest not found.");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "ZNPCS Setup for " + quest.id());
        sender.sendMessage(ChatColor.GRAY + "Set an action command on NPC click:");
        sender.sendMessage(ChatColor.AQUA + "/quest talk " + quest.targetNpcId());
        if (quest.hasStarterNpc()) {
            sender.sendMessage(ChatColor.GRAY + "Starter NPC id: " + ChatColor.YELLOW + quest.starterNpcId());
        }
        sender.sendMessage(ChatColor.GRAY + "Target NPC id: " + ChatColor.YELLOW + quest.targetNpcId());
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
        return null;
    }

    private boolean requireAdmin(CommandSender sender) {
        if (sender.hasPermission("grivience.admin")) {
            return true;
        }
        sender.sendMessage(ChatColor.RED + "You lack permission.");
        return false;
    }

    private Boolean parseBoolean(String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        if (lower.equals("true") || lower.equals("yes") || lower.equals("on")) {
            return true;
        }
        if (lower.equals("false") || lower.equals("no") || lower.equals("off")) {
            return false;
        }
        return null;
    }

    private String join(String[] args, int start) {
        StringBuilder builder = new StringBuilder();
        for (int index = start; index < args.length; index++) {
            if (index > start) {
                builder.append(' ');
            }
            builder.append(args[index]);
        }
        return builder.toString();
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "=== Quest Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + ChatColor.GRAY + " - Open player quest board");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " menu" + ChatColor.GRAY + " - Open player quest board");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " list" + ChatColor.GRAY + " - List conversation quests");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " progress" + ChatColor.GRAY + " - Show active quests");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " start <questId>" + ChatColor.GRAY + " - Start a quest");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " cancel <questId>" + ChatColor.GRAY + " - Cancel an active quest");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " talk <znpcsNpcId>" + ChatColor.GRAY + " - Trigger NPC conversation quest check");

        if (!sender.hasPermission("grivience.admin")) {
            return;
        }

        sender.sendMessage(ChatColor.DARK_GRAY + "--- Admin ---");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " gui" + ChatColor.GRAY + " - Open quest editor GUI");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload" + ChatColor.GRAY + " - Reload quest files");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " create <id> [name]");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " delete <id>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " setname <id> <name...>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " setdescription <id> <desc...>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " setstarter <id> <npcId|none>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " settarget <id> <npcId>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " setrepeatable <id> <true|false>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " setenabled <id> <true|false>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " addreward <id> <command...>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " removereward <id> <index>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " clearrewards <id>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " znpcshint <id>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> root = new ArrayList<>(List.of("menu", "list", "progress", "start", "cancel", "talk", "help"));
            if (sender.hasPermission("grivience.admin")) {
                root.addAll(List.of("gui", "reload", "create", "delete", "setname", "setdescription", "setstarter", "settarget",
                        "setrepeatable", "setenabled", "addreward", "removereward", "clearrewards", "znpcshint"));
            }
            return filterPrefix(root, args[0]);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            switch (sub) {
                case "start", "cancel", "delete", "setname", "setdescription", "setdesc", "setstarter", "setstartnpc", "settarget", "settargetnpc",
                        "setrepeatable", "setenabled", "addreward", "removereward", "clearrewards", "znpcshint" -> {
                    return filterPrefix(questManager.questIds(), args[1]);
                }
                case "talk", "npc", "npctalk" -> {
                    return filterPrefix(questManager.starterNpcIds(), args[1]);
                }
                default -> {
                }
            }
        }

        if (args.length == 3) {
            switch (sub) {
                case "setrepeatable", "setenabled" -> {
                    return filterPrefix(List.of("true", "false"), args[2]);
                }
                case "setstarter" -> {
                    List<String> npcIds = new ArrayList<>(questManager.starterNpcIds());
                    npcIds.add("none");
                    return filterPrefix(npcIds, args[2]);
                }
                case "settarget" -> {
                    return filterPrefix(questManager.starterNpcIds(), args[2]);
                }
                case "removereward" -> {
                    ConversationQuest quest = questManager.quest(args[1]);
                    if (quest == null) {
                        return List.of();
                    }
                    List<String> indices = new ArrayList<>();
                    for (int index = 1; index <= quest.rewardCommands().size(); index++) {
                        indices.add(String.valueOf(index));
                    }
                    return filterPrefix(indices, args[2]);
                }
                default -> {
                }
            }
        }

        return List.of();
    }

    private List<String> filterPrefix(List<String> values, String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        List<String> output = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                output.add(value);
            }
        }
        return output;
    }
}
