package io.papermc.Grivience.mob;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CrimsonWardenAdminCommand implements CommandExecutor, TabCompleter {
    private final CrimsonWardenSpawnManager spawnManager;

    public CrimsonWardenAdminCommand(CrimsonWardenSpawnManager spawnManager) {
        this.spawnManager = spawnManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("grivience.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        switch (subCommand) {
            case "status" -> handleStatus(sender);
            case "setspawn" -> handleSetSpawn(sender);
            case "setradius" -> handleSetRadius(sender, label, args);
            case "spawn" -> handleSpawn(sender);
            case "respawn" -> handleRespawn(sender);
            case "despawn" -> handleDespawn(sender);
            case "enable" -> handleEnable(sender, true);
            case "disable" -> handleEnable(sender, false);
            case "reload" -> handleReload(sender);
            case "help" -> sendHelp(sender, label);
            default -> sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /" + label + " help.");
        }
        return true;
    }

    private void handleStatus(CommandSender sender) {
        Location spawnLocation = spawnManager.getSpawnLocation();
        LivingEntity activeBoss = spawnManager.getActiveBoss();

        sender.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "=== Crimson Warden Status ===");
        sender.sendMessage(ChatColor.GRAY + "Enabled: " + (spawnManager.isEnabled() ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));
        sender.sendMessage(ChatColor.GRAY + "Respawn Interval: " + ChatColor.GOLD + formatDuration(spawnManager.getRespawnIntervalMs()));
        sender.sendMessage(ChatColor.GRAY + "Leash Radius: " + ChatColor.AQUA + formatNumber(spawnManager.getLeashRadius()) + ChatColor.GRAY + " blocks");

        if (spawnLocation == null || spawnLocation.getWorld() == null) {
            sender.sendMessage(ChatColor.GRAY + "Spawn: " + ChatColor.RED + "Not configured");
        } else {
            sender.sendMessage(ChatColor.GRAY + "Spawn: " + ChatColor.AQUA + formatLocation(spawnLocation));
        }

        if (activeBoss != null) {
            sender.sendMessage(ChatColor.GRAY + "Boss State: " + ChatColor.GREEN + "Alive");
            sender.sendMessage(ChatColor.GRAY + "Boss World: " + ChatColor.YELLOW + activeBoss.getWorld().getName());
        } else if (!spawnManager.isSpawnConfigured()) {
            sender.sendMessage(ChatColor.GRAY + "Boss State: " + ChatColor.RED + "Waiting for spawn setup");
        } else if (!spawnManager.isEnabled()) {
            sender.sendMessage(ChatColor.GRAY + "Boss State: " + ChatColor.RED + "Auto-spawn disabled");
        } else {
            long remaining = spawnManager.getRemainingRespawnMs();
            if (remaining <= 0L) {
                sender.sendMessage(ChatColor.GRAY + "Boss State: " + ChatColor.YELLOW + "Ready to spawn");
            } else {
                sender.sendMessage(ChatColor.GRAY + "Boss State: " + ChatColor.GOLD + "Respawning in " + formatDuration(remaining));
            }
        }
    }

    private void handleSetSpawn(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }

        spawnManager.setSpawnLocation(player.getLocation());
        sender.sendMessage(ChatColor.GREEN + "Crimson Warden spawn set to " + ChatColor.AQUA + formatLocation(player.getLocation()) + ChatColor.GREEN + ".");
        sender.sendMessage(ChatColor.GRAY + "Auto-spawn is enabled. If no boss is active, the Warden will spawn on the next tick cycle.");
    }

    private void handleSetRadius(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " setradius <blocks>");
            return;
        }

        double radius;
        try {
            radius = Double.parseDouble(args[1]);
        } catch (NumberFormatException exception) {
            sender.sendMessage(ChatColor.RED + "Radius must be a number.");
            return;
        }

        spawnManager.setLeashRadius(radius);
        sender.sendMessage(ChatColor.GREEN + "Crimson Warden leash radius set to " + ChatColor.AQUA + formatNumber(spawnManager.getLeashRadius()) + ChatColor.GRAY + " blocks.");
    }

    private void handleSpawn(CommandSender sender) {
        if (!spawnManager.isSpawnConfigured()) {
            sender.sendMessage(ChatColor.RED + "Crimson Warden spawn is not configured. Use /crimsonwarden setspawn first.");
            return;
        }
        if (spawnManager.getActiveBoss() != null) {
            sender.sendMessage(ChatColor.RED + "A Crimson Warden is already alive. Use /crimsonwarden respawn to replace it.");
            return;
        }

        LivingEntity spawned = spawnManager.spawnNow(false);
        if (spawned == null) {
            sender.sendMessage(ChatColor.RED + "Failed to spawn the Crimson Warden.");
            return;
        }

        sender.sendMessage(ChatColor.GREEN + "Spawned the Crimson Warden at " + ChatColor.AQUA + formatLocation(spawned.getLocation()) + ChatColor.GREEN + ".");
    }

    private void handleRespawn(CommandSender sender) {
        if (!spawnManager.isSpawnConfigured()) {
            sender.sendMessage(ChatColor.RED + "Crimson Warden spawn is not configured. Use /crimsonwarden setspawn first.");
            return;
        }

        LivingEntity spawned = spawnManager.spawnNow(true);
        if (spawned == null) {
            sender.sendMessage(ChatColor.RED + "Failed to respawn the Crimson Warden.");
            return;
        }

        sender.sendMessage(ChatColor.GREEN + "Respawned the Crimson Warden at " + ChatColor.AQUA + formatLocation(spawned.getLocation()) + ChatColor.GREEN + ".");
    }

    private void handleDespawn(CommandSender sender) {
        if (!spawnManager.despawnActiveBoss(true)) {
            sender.sendMessage(ChatColor.RED + "There is no Crimson Warden alive.");
            return;
        }

        sender.sendMessage(ChatColor.GREEN + "Crimson Warden despawned.");
        sender.sendMessage(ChatColor.GRAY + "Next respawn in " + ChatColor.GOLD + formatDuration(spawnManager.getRespawnIntervalMs()) + ChatColor.GRAY + ".");
    }

    private void handleEnable(CommandSender sender, boolean enabled) {
        spawnManager.setEnabled(enabled);
        sender.sendMessage(ChatColor.GREEN + "Crimson Warden auto-spawn " + (enabled ? "enabled" : "disabled") + ".");
    }

    private void handleReload(CommandSender sender) {
        spawnManager.reload();
        sender.sendMessage(ChatColor.GREEN + "Crimson Warden spawn data reloaded.");
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "=== Crimson Warden Admin ===");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " status" + ChatColor.GRAY + " - Show boss spawn state and timer");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " setspawn" + ChatColor.GRAY + " - Set the boss spawn to your location");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " setradius <blocks>" + ChatColor.GRAY + " - Set how far the boss can roam from spawn");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " spawn" + ChatColor.GRAY + " - Spawn the boss immediately");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " respawn" + ChatColor.GRAY + " - Replace the current boss with a fresh spawn");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " despawn" + ChatColor.GRAY + " - Remove the boss and start the 90-minute cooldown");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " enable" + ChatColor.GRAY + " - Enable automatic respawns");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " disable" + ChatColor.GRAY + " - Disable automatic respawns");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload" + ChatColor.GRAY + " - Reload saved spawn data");
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
        return null;
    }

    private String formatLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return "unknown";
        }
        return String.format(
                "%.1f, %.1f, %.1f in %s",
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getWorld().getName()
        );
    }

    private String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;

        List<String> parts = new ArrayList<>();
        if (hours > 0L) {
            parts.add(hours + "h");
        }
        if (minutes > 0L) {
            parts.add(minutes + "m");
        }
        if (seconds > 0L || parts.isEmpty()) {
            parts.add(seconds + "s");
        }
        return String.join(" ", parts);
    }

    private String formatNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 1.0E-6D) {
            return Long.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("grivience.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            return filterPrefix(List.of("status", "setspawn", "setradius", "spawn", "respawn", "despawn", "enable", "disable", "reload", "help"), args[0]);
        }

        if (args.length == 2 && "setradius".equalsIgnoreCase(args[0])) {
            return filterPrefix(List.of("16", "24", "32", "48"), args[1]);
        }

        return List.of();
    }

    private List<String> filterPrefix(List<String> values, String prefix) {
        String lowered = prefix.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lowered)) {
                matches.add(value);
            }
        }
        return matches;
    }
}
