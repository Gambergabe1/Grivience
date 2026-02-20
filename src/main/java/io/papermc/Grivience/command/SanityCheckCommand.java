package io.papermc.Grivience.command;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.item.ArmorCraftingListener;
import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.skyblock.island.IslandManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.NamespacedKey;
import org.bukkit.Keyed;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Recipe;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Lightweight health check command to verify critical systems like recipes and mob spawning.
 */
public final class SanityCheckCommand implements CommandExecutor, TabCompleter {
    private final GriviencePlugin plugin;
    private final ArmorCraftingListener armorCraftingListener;
    private final IslandManager islandManager;
    private final CustomItemService customItemService;

    public SanityCheckCommand(
            GriviencePlugin plugin,
            ArmorCraftingListener armorCraftingListener,
            IslandManager islandManager,
            CustomItemService customItemService
    ) {
        this.plugin = plugin;
        this.armorCraftingListener = armorCraftingListener;
        this.islandManager = islandManager;
        this.customItemService = customItemService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String mode = args.length > 0 ? args[0].toLowerCase() : "all";

        switch (mode) {
            case "recipes" -> reportRecipes(sender);
            case "mobs" -> reportMobs(sender);
            default -> {
                reportRecipes(sender);
                reportMobs(sender);
            }
        }
        return true;
    }

    private void reportRecipes(CommandSender sender) {
        int totalRecipes = 0;
        int armorRecipes = 0;
        NamespacedKey flyingRaijinKey = new NamespacedKey(plugin, "flying-raijin-recipe");
        boolean hasFlyingRaijin = false;
        int vanillaRecipes = 0;
        int vanillaDiscovered = 0;
        Player playerSender = sender instanceof Player p ? p : null;

        for (Iterator<Recipe> it = Bukkit.recipeIterator(); it.hasNext(); ) {
            Recipe recipe = it.next();
            NamespacedKey key = (recipe instanceof Keyed keyed) ? keyed.getKey() : null;
            if (recipe == null || key == null) {
                continue;
            }
            totalRecipes++;
            if (NamespacedKey.MINECRAFT.equals(key.getNamespace())) {
                vanillaRecipes++;
                if (playerSender != null && playerSender.hasDiscoveredRecipe(key)) {
                    vanillaDiscovered++;
                }
            }
            if (key.getKey().startsWith("armor_") && key.getNamespace().equals(plugin.getName().toLowerCase())) {
                armorRecipes++;
            }
            if (key.equals(flyingRaijinKey)) {
                hasFlyingRaijin = true;
            }
        }

        int registeredArmor = armorCraftingListener != null ? armorCraftingListener.getRegisteredArmorRecipeCount() : -1;

        sender.sendMessage(ChatColor.YELLOW + "=== Recipe Sanity ===");
        sender.sendMessage(statLine("Total recipes", totalRecipes));
        sender.sendMessage(statLine("Vanilla recipes", vanillaRecipes));
        if (playerSender != null) {
            sender.sendMessage(statLine("Vanilla discovered (you)", vanillaDiscovered));
        }
        sender.sendMessage(statLine("Armor recipes (iterator)", armorRecipes));
        if (registeredArmor >= 0) {
            sender.sendMessage(statLine("Armor recipes (listener)", registeredArmor));
        }
        sender.sendMessage(statLine("Flying Raijin recipe", hasFlyingRaijin ? "present" : "MISSING"));
        sender.sendMessage(ChatColor.GRAY + "Custom item keys tracked: " + customItemService.allItemKeys().size());
    }

    private void reportMobs(CommandSender sender) {
        World world = islandManager.getIslandWorld();
        sender.sendMessage(ChatColor.YELLOW + "=== Mob Spawn Sanity ===");
        if (world == null) {
            sender.sendMessage(ChatColor.RED + "Skyblock world not initialized.");
            return;
        }

        Boolean gamerule = world.getGameRuleValue(GameRule.DO_MOB_SPAWNING);
        boolean allowMonsters = world.getAllowMonsters();
        boolean generatorMobs = world.getGenerator() != null && world.getGenerator().shouldGenerateMobs();

        sender.sendMessage(statLine("World", world.getName()));
        sender.sendMessage(statLine("GameRule DO_MOB_SPAWNING", gamerule == null ? "null" : gamerule.toString()));
        sender.sendMessage(statLine("World allowMonsters", allowMonsters));
        sender.sendMessage(statLine("Generator shouldGenerateMobs", generatorMobs));
    }

    private String statLine(String label, Object value) {
        return ChatColor.GRAY + label + ChatColor.WHITE + ": " + ChatColor.AQUA + value;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = List.of("all", "recipes", "mobs");
            String prefix = args[0].toLowerCase();
            List<String> matches = new ArrayList<>();
            for (String option : options) {
                if (option.startsWith(prefix)) {
                    matches.add(option);
                }
            }
            return matches;
        }
        return List.of();
    }
}
