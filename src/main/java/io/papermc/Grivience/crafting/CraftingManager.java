package io.papermc.Grivience.crafting;

import io.papermc.Grivience.GriviencePlugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Main manager for the Skyblock Crafting System.
 * Handles initialization, registration, and coordination of all crafting components.
 */
public class CraftingManager implements Listener {
    private final GriviencePlugin plugin;
    private CraftingGuiManager guiManager;
    private boolean enabled = false;

    public CraftingManager(GriviencePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialize and enable the crafting system.
     */
    public void enable() {
        if (enabled) {
            return;
        }

        // Initialize recipe registry
        RecipeRegistry.init(plugin);
        
        // Initialize GUI manager
        this.guiManager = new CraftingGuiManager(plugin);
        Bukkit.getPluginManager().registerEvents(guiManager, plugin);
        
        // Register this manager as listener for crafting table override
        Bukkit.getPluginManager().registerEvents(this, plugin);

        plugin.getLogger().info("Loaded " + RecipeRegistry.count() + " crafting recipes");

        enabled = true;
        plugin.getLogger().info("Crafting System enabled (Skyblock accurate)");
    }

    /**
     * Disable the crafting system.
     */
    public void disable() {
        if (!enabled) {
            return;
        }

        enabled = false;
        plugin.getLogger().info("Crafting System disabled");
    }

    /**
     * Check if the crafting system is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Open crafting menu for a player.
     */
    public void openCraftingMenu(Player player) {
        if (!enabled) {
            player.sendMessage(ChatColor.RED + "Crafting system is not enabled.");
            return;
        }
        guiManager.openMain(player);
    }

    /**
     * Optional: intercept right-clicks on crafting tables to open the Skyblock crafting GUI.
     *
     * By default we do not override vanilla crafting tables, so players can use the standard crafting UI.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!enabled) return;

        // Default: allow vanilla crafting tables. Can be re-enabled via config.
        if (!plugin.getConfig().getBoolean("crafting.override-crafting-table", false)) {
            return;
        }
        
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.CRAFTING_TABLE) {
                event.setCancelled(true);
                openCraftingMenu(event.getPlayer());
            }
        }
    }

    /**
     * Reload the crafting system from configuration.
     */
    public void reload() {
        disable();
        enable();
    }

    public CraftingGuiManager getGuiManager() {
        return guiManager;
    }

    /**
     * Command handler for crafting commands.
     */
    public static class CraftingCommand implements CommandExecutor, TabCompleter {
        private final CraftingManager manager;

        public CraftingCommand(CraftingManager manager) {
            this.manager = manager;
        }

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                return true;
            }

            if (manager.isEnabled()) {
                if (args.length > 0) {
                    // Simple search implementation
                    String query = String.join(" ", args);
                    List<SkyblockRecipe> results = RecipeRegistry.search(query);
                    if (results.size() == 1) {
                        manager.getGuiManager().openRecipeDetail(player, results.get(0));
                    } else if (results.size() > 1) {
                        // For now just open main if multiple results
                        manager.openCraftingMenu(player);
                        player.sendMessage(ChatColor.YELLOW + "Found " + results.size() + " matches. Please be more specific.");
                    } else {
                        player.sendMessage(ChatColor.RED + "No recipes found matching '" + query + "'.");
                    }
                } else {
                    manager.openCraftingMenu(player);
                }
            } else {
                player.sendMessage(ChatColor.RED + "Crafting system is not enabled.");
            }

            return true;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            if (args.length == 1) {
                List<String> suggestions = new ArrayList<>();
                String query = args[0].toLowerCase();
                for (SkyblockRecipe recipe : RecipeRegistry.getAll()) {
                    if (recipe.getName().toLowerCase().startsWith(query)) {
                        suggestions.add(recipe.getName());
                    }
                }
                return suggestions;
            }
            return new ArrayList<>();
        }
    }
}
