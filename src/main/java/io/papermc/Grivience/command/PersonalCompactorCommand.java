package io.papermc.Grivience.command;

import io.papermc.Grivience.compactor.PersonalCompactorManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * /compactor command for Personal Compactor management.
 */
public final class PersonalCompactorCommand implements CommandExecutor, TabCompleter {
    private final PersonalCompactorManager manager;

    public PersonalCompactorCommand(PersonalCompactorManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        if (!player.hasPermission(PersonalCompactorManager.USE_PERMISSION)) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use Personal Compactor.");
            return true;
        }
        if (manager == null || !manager.isEnabled()) {
            player.sendMessage(ChatColor.RED + "Personal Compactor is currently disabled.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("open")) {
            manager.openMenu(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "stats", "status" -> showStats(player);
            case "compact", "force" -> {
                manager.compactNow(player);
                player.sendMessage(ChatColor.GREEN + "Personal Compactor pass executed.");
            }
            case "clear" -> clearSlot(player, args);
            case "help" -> showHelp(player, label);
            default -> showHelp(player, label);
        }
        return true;
    }

    private void clearSlot(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /compactor clear <slot>");
            return;
        }
        int slot;
        try {
            slot = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            player.sendMessage(ChatColor.RED + "Invalid slot number.");
            return;
        }
        if (slot < 1 || slot > PersonalCompactorManager.MAX_SLOTS) {
            player.sendMessage(ChatColor.RED + "Slot must be between 1 and " + PersonalCompactorManager.MAX_SLOTS + ".");
            return;
        }
        if (!manager.clearSlot(player, slot - 1)) {
            player.sendMessage(ChatColor.RED + "Failed to clear slot.");
            return;
        }
        player.sendMessage(ChatColor.YELLOW + "Cleared Personal Compactor slot " + slot + ".");
    }

    private void showStats(Player player) {
        int unlocked = manager.unlockedSlots(player);
        Map<Integer, String> configured = manager.configuredSlotsSnapshot(player);

        player.sendMessage("");
        player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Personal Compactor");
        player.sendMessage(ChatColor.GRAY + "Unlocked Slots: " + ChatColor.YELLOW + unlocked + ChatColor.GRAY + "/" + ChatColor.YELLOW + PersonalCompactorManager.MAX_SLOTS);
        player.sendMessage(ChatColor.GRAY + "Configured Slots: " + ChatColor.AQUA + configured.size());
        player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/compactor" + ChatColor.GRAY + " to configure.");
        player.sendMessage("");
    }

    private void showHelp(Player player, String label) {
        player.sendMessage(ChatColor.YELLOW + "/" + label + ChatColor.GRAY + " - Open Personal Compactor menu");
        player.sendMessage(ChatColor.GRAY + "Left-click a slot, then click a compacted item in your inventory.");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " stats" + ChatColor.GRAY + " - Show unlocked/configured slot info");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " clear <slot>" + ChatColor.GRAY + " - Clear a configured slot");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " compact" + ChatColor.GRAY + " - Force one compaction pass");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return List.of();
        }
        if (args.length == 1) {
            String partial = args[0].toLowerCase(Locale.ROOT);
            List<String> values = List.of("open", "stats", "clear", "compact", "help");
            List<String> out = new ArrayList<>();
            for (String value : values) {
                if (value.startsWith(partial)) {
                    out.add(value);
                }
            }
            return out;
        }
        if (args.length == 2 && "clear".equalsIgnoreCase(args[0])) {
            return List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12");
        }
        return List.of();
    }
}
