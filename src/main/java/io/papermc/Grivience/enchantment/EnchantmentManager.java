package io.papermc.Grivience.enchantment;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.command.PluginCommand;

/**
 * Main manager for the Skyblock Enchantment System.
 * Handles initialization, registration, and coordination of all enchantment components.
 */
public class EnchantmentManager implements Listener {
    private final GriviencePlugin plugin;
    private EnchantmentTableGui enchantTableGui;
    private SkyblockAnvilGui anvilGui;
    private EnchantmentCommand enchantCommand;
    private boolean enabled = false;

    public EnchantmentManager(GriviencePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialize and enable the enchantment system.
     */
    public void enable() {
        if (enabled) {
            return;
        }

        // Initialize enchantment registry
        EnchantmentRegistry.init();
        plugin.getLogger().info("Loaded " + EnchantmentRegistry.count() + " enchantments");

        // Initialize GUIs
        enchantTableGui = new EnchantmentTableGui(plugin);
        anvilGui = new SkyblockAnvilGui(plugin);

        // Initialize command
        enchantCommand = new EnchantmentCommand(plugin, enchantTableGui, anvilGui);

        // Register listeners
        Bukkit.getPluginManager().registerEvents(enchantTableGui, plugin);
        Bukkit.getPluginManager().registerEvents(anvilGui, plugin);
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Register commands
        registerCommands();

        enabled = true;
        plugin.getLogger().info("Enchantment System enabled (Skyblock style)");
    }

    /**
     * Disable the enchantment system.
     */
    public void disable() {
        if (!enabled) {
            return;
        }

        enabled = false;
        plugin.getLogger().info("Enchantment System disabled");
    }

    /**
     * Check if the enchantment system is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get the enchantment table GUI.
     */
    public EnchantmentTableGui getEnchantTableGui() {
        return enchantTableGui;
    }

    /**
     * Get the anvil GUI.
     */
    public SkyblockAnvilGui getAnvilGui() {
        return anvilGui;
    }

    /**
     * Open enchantment table for a player.
     */
    public void openEnchantTable(Player player) {
        if (!enabled) {
            player.sendMessage("Enchantment system is not enabled.");
            return;
        }
        enchantTableGui.openEnchantTable(player);
    }

    /**
     * Open anvil for a player.
     */
    public void openAnvil(Player player) {
        if (!enabled) {
            player.sendMessage("Enchantment system is not enabled.");
            return;
        }
        anvilGui.openAnvil(player);
    }

    /**
     * Reload the enchantment system from configuration.
     */
    public void reload() {
        disable();
        enable();
    }

    private void registerCommands() {
        // Only register primary command nodes; Bukkit handles aliases automatically.
        registerCommand("enchant");
        registerCommand("anvil");
        registerCommand("enchantinfo");
        registerCommand("enchantlist");
    }

    private void registerCommand(String name) {
        PluginCommand command = plugin.getCommand(name);
        if (command == null) {
            plugin.getLogger().warning("Missing command in plugin.yml: " + name);
            return;
        }
        command.setExecutor(enchantCommand);
        command.setTabCompleter(enchantCommand);
    }
}

