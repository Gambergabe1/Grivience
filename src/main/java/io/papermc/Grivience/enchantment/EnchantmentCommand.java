package io.papermc.Grivience.enchantment;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Command handler for enchantment-related commands.
 * /enchant, /et, /anvil, /av
 */
public class EnchantmentCommand implements CommandExecutor, TabCompleter {
    private final GriviencePlugin plugin;
    private final EnchantmentTableGui enchantTableGui;
    private final SkyblockAnvilGui anvilGui;

    public EnchantmentCommand(GriviencePlugin plugin, EnchantmentTableGui enchantTableGui, SkyblockAnvilGui anvilGui) {
        this.plugin = plugin;
        this.enchantTableGui = enchantTableGui;
        this.anvilGui = anvilGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        String cmd = command.getName().toLowerCase();

        if (cmd.equals("enchant") || cmd.equals("et") || cmd.equals("enchantmenttable")) {
            handleEnchantTable(player, args);
        } else if (cmd.equals("anvil") || cmd.equals("av")) {
            handleAnvil(player, args);
        } else if (cmd.equals("enchantinfo") || cmd.equals("ei")) {
            handleEnchantInfo(player, args);
        } else if (cmd.equals("enchantlist") || cmd.equals("el")) {
            handleEnchantList(player, args);
        }

        return true;
    }

    private void handleEnchantTable(Player player, String[] args) {
        enchantTableGui.openEnchantTable(player);
        player.sendMessage(ChatColor.GREEN + "Opening Enchantment Table...");
    }

    private void handleAnvil(Player player, String[] args) {
        anvilGui.openAnvil(player);
        player.sendMessage(ChatColor.GREEN + "Opening Anvil...");
    }

    private void handleEnchantInfo(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /enchantinfo <enchantment_id>");
            return;
        }

        String enchantId = args[0].toLowerCase();
        SkyblockEnchantment enchantment = EnchantmentRegistry.get(enchantId);

        if (enchantment == null) {
            player.sendMessage(ChatColor.RED + "Enchantment not found: " + enchantId);
            player.sendMessage(ChatColor.GRAY + "Use /enchantlist to see all enchantments.");
            return;
        }

        sendEnchantInfo(player, enchantment);
    }

    private void handleEnchantList(Player player, String[] args) {
        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }

        sendEnchantList(player, page);
    }

    private void sendEnchantInfo(Player player, SkyblockEnchantment enchantment) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m--------------------"));
        player.sendMessage(enchantment.getType().getColor().toString() + ChatColor.BOLD + enchantment.getName());
        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "ID: " + ChatColor.YELLOW + enchantment.getId());
        player.sendMessage(ChatColor.GRAY + "Type: " + enchantment.getType().getDisplayName());
        player.sendMessage(ChatColor.GRAY + "Category: " + formatCategory(enchantment.getCategory()));
        player.sendMessage(ChatColor.GRAY + "Max Level: " + ChatColor.AQUA + enchantment.getMaxLevel());
        player.sendMessage(ChatColor.GRAY + "Base XP Cost: " + ChatColor.YELLOW + enchantment.getBaseXpCost());
        player.sendMessage("");

        if (!enchantment.getDescription().isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "Description:");
            for (String line : enchantment.getDescription()) {
                player.sendMessage(ChatColor.DARK_GRAY + "• " + ChatColor.GRAY + line);
            }
            player.sendMessage("");
        }

        if (enchantment.isUltimate()) {
            player.sendMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "ULTIMATE ENCHANTMENT");
            player.sendMessage(ChatColor.GRAY + "Only one ultimate can be applied to an item.");
            player.sendMessage("");
        }

        if (enchantment.isDungeon()) {
            player.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "DUNGEON ENCHANTMENT");
            player.sendMessage(ChatColor.GRAY + "Only obtainable from dungeon loot.");
            player.sendMessage("");
        }

        if (!enchantment.getConflictsWith().isEmpty()) {
            player.sendMessage(ChatColor.RED + "Conflicts with:");
            for (String conflict : enchantment.getConflictsWith()) {
                SkyblockEnchantment conflictEnchant = EnchantmentRegistry.get(conflict);
                if (conflictEnchant != null) {
                    player.sendMessage(ChatColor.DARK_RED + "• " + conflictEnchant.getName());
                }
            }
            player.sendMessage("");
        }

        player.sendMessage(ChatColor.GRAY + "XP Cost per level: " + ChatColor.AQUA + enchantment.getXpCost(1));
        player.sendMessage(ChatColor.GRAY + "Max XP Cost: " + ChatColor.AQUA + enchantment.getXpCost(enchantment.getMaxLevel()));

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m--------------------"));
    }

    private void sendEnchantList(Player player, int page) {
        List<SkyblockEnchantment> allEnchants = new ArrayList<>(EnchantmentRegistry.getAll());
        int perPage = 10;
        int totalPages = Math.max(1, (int) Math.ceil(allEnchants.size() / (double) perPage));

        if (page < 1 || page > totalPages) {
            player.sendMessage(ChatColor.RED + "Invalid page. Use 1-" + totalPages);
            return;
        }

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m--------------------"));
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Enchantment List " + ChatColor.YELLOW + "Page " + page + "/" + totalPages);
        player.sendMessage("");

        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, allEnchants.size());

        for (int i = start; i < end; i++) {
            SkyblockEnchantment enchant = allEnchants.get(i);
            ChatColor color = enchant.getType().getColor();
            String ultimateTag = enchant.isUltimate() ? ChatColor.LIGHT_PURPLE + " [ULT]" : "";
            String dungeonTag = enchant.isDungeon() ? ChatColor.DARK_PURPLE + " [DUN]" : "";
            player.sendMessage(color + "• " + enchant.getName() + ChatColor.GRAY + " (Max: " + enchant.getMaxLevel() + ")" + ultimateTag + dungeonTag);
        }

        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "Use /enchantlist <page> to navigate.");
        player.sendMessage(ChatColor.GRAY + "Use /enchantinfo <id> for details.");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m--------------------"));
    }

    private String formatCategory(EnchantmentCategory category) {
        StringBuilder result = new StringBuilder();
        String name = category.name();
        String[] parts = name.split("_");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) result.append(" ");
            result.append(parts[i].charAt(0)).append(parts[i].substring(1).toLowerCase());
        }
        return result.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String input = args[0].toLowerCase();

            if (command.getName().equalsIgnoreCase("enchantinfo") || command.getName().equalsIgnoreCase("ei")) {
                for (SkyblockEnchantment enchant : EnchantmentRegistry.getAll()) {
                    if (enchant.getId().toLowerCase().startsWith(input)) {
                        completions.add(enchant.getId());
                    }
                }
            }

            return completions;
        }

        return new ArrayList<>();
    }
}
