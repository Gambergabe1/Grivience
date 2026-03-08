package io.papermc.Grivience.welcome;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;

/**
 * Main manager for the Welcome Event system.
 * Handles initialization, registration, and coordination of all welcome components.
 */
public class WelcomeManager {
    private final GriviencePlugin plugin;
    private XPBoostManager xpBoostManager;
    private WelcomeEventManager welcomeEventManager;
    private WelcomeListener welcomeListener;
    private WelcomeCommand welcomeCommand;
    private boolean enabled = false;

    public WelcomeManager(GriviencePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialize and enable the welcome event system.
     */
    public void enable() {
        if (enabled) {
            return;
        }

        // Check if enabled in config
        enabled = plugin.getConfig().getBoolean("welcome-event.enabled", true);
        if (!enabled) {
            plugin.getLogger().info("Welcome Event system disabled in config.");
            return;
        }

        // Initialize XP Boost Manager
        xpBoostManager = new XPBoostManager(plugin);
        plugin.getLogger().info("XP Boost Manager enabled");

        // Initialize Welcome Event Manager
        welcomeEventManager = new WelcomeEventManager(plugin, xpBoostManager);
        plugin.getLogger().info("Welcome Event Manager enabled");

        // Initialize Listener
        welcomeListener = new WelcomeListener(plugin, welcomeEventManager);
        Bukkit.getPluginManager().registerEvents(welcomeListener, plugin);
        plugin.getLogger().info("Welcome Listener registered");

        // Initialize Command
        welcomeCommand = new WelcomeCommand(plugin, welcomeEventManager);
        registerCommand("welcome", welcomeCommand);
        plugin.getLogger().info("Welcome Command registered");

        plugin.getLogger().info("Welcome Event System enabled (Hard Hat Harry)");
    }

    /**
     * Disable the welcome event system.
     */
    public void disable() {
        if (!enabled) {
            return;
        }

        // Save data
        if (welcomeEventManager != null) {
            welcomeEventManager.saveClaimedRewards();
        }
        if (xpBoostManager != null) {
            xpBoostManager.saveBoosts();
        }

        enabled = false;
        plugin.getLogger().info("Welcome Event System disabled");
    }

    /**
     * Check if the welcome event system is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get the XP Boost Manager.
     */
    public XPBoostManager getXPBoostManager() {
        return xpBoostManager;
    }

    /**
     * Get the Welcome Event Manager.
     */
    public WelcomeEventManager getWelcomeEventManager() {
        return welcomeEventManager;
    }

    /**
     * Reload the welcome event system from configuration.
     */
    public void reload() {
        disable();
        enable();
    }

    private void registerCommand(String name, CommandExecutor executor) {
        PluginCommand command = plugin.getCommand(name);
        if (command == null) {
            plugin.getLogger().warning("Missing command in plugin.yml: " + name);
            return;
        }
        if (executor instanceof TabCompleter tabCompleter) {
            command.setExecutor(executor);
            command.setTabCompleter(tabCompleter);
        } else {
            command.setExecutor(executor);
        }
    }
}
