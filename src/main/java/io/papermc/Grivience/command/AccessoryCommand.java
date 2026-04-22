package io.papermc.Grivience.command;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.accessory.AccessoryBonuses;
import io.papermc.Grivience.accessory.AccessoryManager;
import io.papermc.Grivience.storage.StorageType;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Opens the accessory bag and shows active accessory bonuses.
 */
public final class AccessoryCommand implements CommandExecutor, TabCompleter {
    private final GriviencePlugin plugin;
    private final AccessoryManager accessoryManager;

    public AccessoryCommand(GriviencePlugin plugin, AccessoryManager accessoryManager) {
        this.plugin = plugin;
        this.accessoryManager = accessoryManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("storage.accessory")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to access the Accessory Bag.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("open")) {
            openBag(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "upgrade", "upgrades" -> openUpgrade(player);
            case "stats", "bonus", "bonuses" -> showStats(player);
            case "help" -> showHelp(player, label);
            default -> showHelp(player, label);
        }
        return true;
    }

    private void openBag(Player player) {
        if (plugin.getStorageGui() != null) {
            plugin.getStorageGui().openStorage(player, StorageType.ACCESSORY_BAG);
            return;
        }
        player.sendMessage(ChatColor.RED + "Accessory Bag is unavailable right now.");
    }

    private void openUpgrade(Player player) {
        if (!player.hasPermission("storage.accessory.upgrade")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to upgrade the Accessory Bag.");
            return;
        }
        if (plugin.getStorageGui() != null) {
            plugin.getStorageGui().openUpgradeMenu(player, StorageType.ACCESSORY_BAG);
            return;
        }
        player.sendMessage(ChatColor.RED + "Accessory Bag upgrades are unavailable right now.");
    }

    private void showStats(Player player) {
        AccessoryBonuses bonuses = accessoryManager == null ? AccessoryBonuses.NONE : accessoryManager.bonuses(player);
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "━━━━━ Accessory Bag ━━━━━");
        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "Total Accessories: " + ChatColor.WHITE + bonuses.activeAccessories());
        player.sendMessage(ChatColor.GRAY + "Unique Accessories: " + ChatColor.AQUA + bonuses.uniqueAccessories());
        player.sendMessage(ChatColor.GRAY + "Magical Power: " + ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + bonuses.magicalPower() + " ✧");
        player.sendMessage(ChatColor.GRAY + "Echo Stacks: " + ChatColor.YELLOW + bonuses.echoStacks() + ChatColor.DARK_GRAY + "/" + ChatColor.YELLOW + "6");
        player.sendMessage(ChatColor.GRAY + "Resonance: " + (bonuses.resonanceActive() ? ChatColor.GREEN + "✔ ACTIVE" : ChatColor.DARK_GRAY + "✖ INACTIVE"));

        List<String> lines = new ArrayList<>();
        addLine(lines, "Health", bonuses.health(), ChatColor.RED, false);
        addLine(lines, "Defense", bonuses.defense(), ChatColor.GREEN, false);
        addLine(lines, "Strength", bonuses.strength(), ChatColor.RED, false);
        addLine(lines, "Crit Chance", bonuses.critChance(), ChatColor.BLUE, true);
        addLine(lines, "Crit Damage", bonuses.critDamage(), ChatColor.BLUE, true);
        addLine(lines, "Intelligence", bonuses.intelligence(), ChatColor.AQUA, false);
        addLine(lines, "Farming Fortune", bonuses.farmingFortune(), ChatColor.GOLD, false);

        if (lines.isEmpty()) {
            player.sendMessage("");
            player.sendMessage(ChatColor.DARK_GRAY + "No active accessory bonuses.");
        } else {
            player.sendMessage("");
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Stat Bonuses:");
            for (String line : lines) {
                player.sendMessage("  " + line);
            }
        }
        player.sendMessage("");
        player.sendMessage(ChatColor.DARK_GRAY + "Magical Power increases all stats by " + ChatColor.WHITE + String.format("%.1f", ((io.papermc.Grivience.accessory.AccessoryPower.statMultiplierFromPower(bonuses.magicalPower()) - 1.0) * 100)) + "%");
        player.sendMessage("");
    }

    private void addLine(List<String> lines, String label, double value, ChatColor color, boolean percent) {
        int rounded = (int) Math.round(value);
        if (rounded == 0) {
            return;
        }
        String suffix = percent ? "%" : "";
        lines.add(ChatColor.GRAY + label + ": " + color + "+" + rounded + suffix);
    }

    private void showHelp(Player player, String label) {
        player.sendMessage(ChatColor.YELLOW + "/" + label + ChatColor.GRAY + " - Open your Accessory Bag");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " upgrade" + ChatColor.GRAY + " - Upgrade Accessory Bag slots");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " stats" + ChatColor.GRAY + " - View active accessory bonuses");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String partial = args[0].toLowerCase(Locale.ROOT);
        List<String> suggestions = new ArrayList<>();
        for (String value : List.of("open", "upgrade", "stats", "help")) {
            if (value.startsWith(partial)) {
                suggestions.add(value);
            }
        }
        return suggestions;
    }
}
