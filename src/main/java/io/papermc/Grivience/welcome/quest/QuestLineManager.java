package io.papermc.Grivience.welcome.quest;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;

/**
 * Main manager for the Welcome Quest Line system.
 */
public class QuestLineManager {
    private final GriviencePlugin plugin;
    private QuestProgressManager progressManager;
    private QuestGuiManager guiManager;
    private QuestProgressListener progressListener;
    private boolean enabled = false;

    public QuestLineManager(GriviencePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialize and enable the quest line system.
     */
    public void enable() {
        if (enabled) return;

        // Initialize quest registry
        WelcomeQuestRegistry.init();
        plugin.getLogger().info("Loaded " + WelcomeQuestRegistry.count() + " welcome quests");

        // Initialize progress manager
        progressManager = new QuestProgressManager(plugin);
        plugin.getLogger().info("Quest progress manager enabled");

        // Initialize GUI manager
        guiManager = new QuestGuiManager(plugin, progressManager);
        Bukkit.getPluginManager().registerEvents(guiManager, plugin);
        plugin.getLogger().info("Quest GUI enabled");

        // Initialize progress listener
        progressListener = new QuestProgressListener(plugin, progressManager);
        Bukkit.getPluginManager().registerEvents(progressListener, plugin);
        plugin.getLogger().info("Quest progress listener enabled");

        // Register command
        QuestLineCommand command = new QuestLineCommand(this);
        registerCommand("questline", command);
        registerCommand("ql", command);

        enabled = true;
        plugin.getLogger().info("Welcome Quest Line enabled!");
    }

    /**
     * Disable the quest line system.
     */
    public void disable() {
        if (!enabled) return;

        // Save progress
        if (progressManager != null) {
            progressManager.saveProgress();
        }

        enabled = false;
        plugin.getLogger().info("Welcome Quest Line disabled");
    }

    /**
     * Reload the quest line system.
     */
    public void reload() {
        disable();
        enable();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public QuestProgressManager getProgressManager() {
        return progressManager;
    }

    public QuestGuiManager getGuiManager() {
        return guiManager;
    }

    private void registerCommand(String name, CommandExecutor executor) {
        PluginCommand command = plugin.getCommand(name);
        if (command == null) {
            plugin.getLogger().warning("Missing command in plugin.yml: " + name);
            return;
        }
        command.setExecutor(executor);
        if (executor instanceof TabCompleter tabCompleter) {
            command.setTabCompleter(tabCompleter);
        }
    }

    /**
     * Command handler for quest line commands.
     */
    private static class QuestLineCommand implements CommandExecutor, TabCompleter {
        private final QuestLineManager manager;

        public QuestLineCommand(QuestLineManager manager) {
            this.manager = manager;
        }

        @Override
        public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
            if (!(sender instanceof org.bukkit.entity.Player player)) {
                sender.sendMessage(org.bukkit.ChatColor.RED + "This command can only be used by players.");
                return true;
            }

            if (args.length == 0) {
                // Open quest book
                manager.getGuiManager().openQuestBook(player);
                return true;
            }

            String subCommand = args[0].toLowerCase();

            if (subCommand.equals("progress")) {
                // Show progress
                int completed = manager.getProgressManager().getTotalCompleted(player);
                int total = WelcomeQuestRegistry.count();
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Quest Progress: " + completed + "/" + total);
                return true;
            }

            if (subCommand.equals("reset") && player.hasPermission("grivience.admin")) {
                // Reset progress (admin only)
                manager.getProgressManager().resetProgress(player);
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Quest progress reset!");
                return true;
            }

            if (subCommand.equals("help")) {
                sendHelp(player);
                return true;
            }

            player.sendMessage(org.bukkit.ChatColor.RED + "Unknown subcommand. Use /questline help for help.");
            return true;
        }

        private void sendHelp(org.bukkit.entity.Player player) {
            player.sendMessage("");
            player.sendMessage(org.bukkit.ChatColor.GOLD + "" + org.bukkit.ChatColor.BOLD + "Quest Line Commands");
            player.sendMessage("");
            player.sendMessage(org.bukkit.ChatColor.YELLOW + "/questline" + org.bukkit.ChatColor.GRAY + " - Open quest book");
            player.sendMessage(org.bukkit.ChatColor.YELLOW + "/questline progress" + org.bukkit.ChatColor.GRAY + " - View progress");
            player.sendMessage(org.bukkit.ChatColor.YELLOW + "/questline reset" + org.bukkit.ChatColor.GRAY + " - Reset progress (admin)");
            player.sendMessage(org.bukkit.ChatColor.YELLOW + "/questline help" + org.bukkit.ChatColor.GRAY + " - Show this help");
            player.sendMessage("");
        }

        @Override
        public java.util.List<String> onTabComplete(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
            if (args.length == 1) {
                java.util.List<String> completions = new java.util.ArrayList<>();
                String input = args[0].toLowerCase();

                if ("progress".startsWith(input)) completions.add("progress");
                if ("reset".startsWith(input) && sender.hasPermission("grivience.admin")) completions.add("reset");
                if ("help".startsWith(input)) completions.add("help");

                return completions;
            }

            return new java.util.ArrayList<>();
        }
    }
}
