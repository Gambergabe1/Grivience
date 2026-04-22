package io.papermc.Grivience.mob;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class MonsterSpawnAdminCommand implements CommandExecutor, TabCompleter {
    private final GriviencePlugin plugin;
    private final CustomMonsterManager monsterManager;
    private final MonsterGui monsterGui;

    public MonsterSpawnAdminCommand(GriviencePlugin plugin, CustomMonsterManager monsterManager) {
        this.plugin = plugin;
        this.monsterManager = monsterManager;
        this.monsterGui = new MonsterGui(monsterManager);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("grivience.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        switch (subCommand) {
            case "create" -> handleCreate(sender, args);
            case "set" -> handleSet(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "toggle" -> handleToggle(sender, args);
            case "spawn" -> handleSpawn(sender, args);
            case "gui", "view" -> handleGui(sender);
            case "monsters" -> handleMonsters(sender);
            case "help" -> sendHelp(sender);
            default -> {
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /mobspawn help");
            }
        }
        return true;
    }

    private void handleCreate(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /mobspawn create <monster_id|vanilla_entity_type>");
            return;
        }

        String monsterId = resolveMonsterIdOrSendError(sender, args[1]);
        if (monsterId == null) {
            return;
        }

        Location location = player.getLocation();
        SpawnPoint point = monsterManager.createSpawnPoint(location, monsterId);

        sender.sendMessage(ChatColor.GREEN + "Spawn point created!");
        sender.sendMessage(ChatColor.GRAY + "ID: " + ChatColor.AQUA + point.getId());
        sender.sendMessage(ChatColor.GRAY + "Monster: " + ChatColor.YELLOW + monsterManager.describeMonster(point.getMonsterId()));
        sender.sendMessage(ChatColor.GRAY + "Location: " + ChatColor.AQUA + 
                String.format("%.1f, %.1f, %.1f in %s", location.getX(), location.getY(), location.getZ(), location.getWorld().getName()));
        sender.sendMessage(ChatColor.YELLOW + "Monsters will now spawn here automatically!");
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /mobspawn set <spawn_point_id> <monster_id|vanilla_entity_type>");
            return;
        }

        UUID pointId;
        try {
            pointId = UUID.fromString(args[1]);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid spawn point ID format.");
            return;
        }

        SpawnPoint point = monsterManager.getSpawnPoint(pointId);
        if (point == null) {
            sender.sendMessage(ChatColor.RED + "Spawn point not found: " + args[1]);
            return;
        }

        String monsterId = resolveMonsterIdOrSendError(sender, args[2]);
        if (monsterId == null) {
            return;
        }

        point.setMonsterId(monsterId);
        monsterManager.saveSpawnPoints();
        sender.sendMessage(ChatColor.GREEN + "Spawn point monster updated to " + ChatColor.YELLOW + monsterManager.describeMonster(monsterId) + ChatColor.GREEN + ".");
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /mobspawn remove <spawn_point_id>");
            return;
        }

        try {
            UUID pointId = UUID.fromString(args[1]);
            if (monsterManager.removeSpawnPoint(pointId)) {
                sender.sendMessage(ChatColor.GREEN + "Spawn point removed successfully!");
            } else {
                sender.sendMessage(ChatColor.RED + "Spawn point not found: " + pointId);
            }
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid spawn point ID format.");
        }
    }

    private void handleList(CommandSender sender) {
        Collection<SpawnPoint> points = monsterManager.getSpawnPoints();
        if (points.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No spawn points configured.");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "=== Monster Spawn Points (" + points.size() + ") ===");
        for (SpawnPoint point : points) {
            String monsterName = monsterManager.describeMonster(point.getMonsterId());
            String status = point.isActive() ? ChatColor.GREEN + "Active" : ChatColor.RED + "Inactive";
            
            sender.sendMessage(ChatColor.AQUA + "ID: " + point.getId());
            sender.sendMessage(ChatColor.GRAY + "  Monster: " + ChatColor.YELLOW + monsterName);
            sender.sendMessage(ChatColor.GRAY + "  World: " + ChatColor.WHITE + 
                    (point.isLocationValid() ? point.getLocation().getWorld().getName() : "Unknown"));
            sender.sendMessage(ChatColor.GRAY + "  Status: " + status);
            sender.sendMessage("");
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /mobspawn info <spawn_point_id>");
            return;
        }

        try {
            UUID pointId = UUID.fromString(args[1]);
            SpawnPoint point = monsterManager.getSpawnPoint(pointId);
            if (point == null) {
                sender.sendMessage(ChatColor.RED + "Spawn point not found: " + pointId);
                return;
            }

            String monsterName = monsterManager.describeMonster(point.getMonsterId());

            sender.sendMessage(ChatColor.GOLD + "=== Spawn Point Info ===");
            sender.sendMessage(ChatColor.GRAY + "ID: " + ChatColor.AQUA + point.getId());
            sender.sendMessage(ChatColor.GRAY + "Monster: " + ChatColor.YELLOW + monsterName);
            sender.sendMessage(ChatColor.GRAY + "Spawn Radius: " + ChatColor.WHITE + point.getSpawnRadius());
            sender.sendMessage(ChatColor.GRAY + "Spawn Delay: " + ChatColor.WHITE + point.getSpawnDelay() + " ticks");
            sender.sendMessage(ChatColor.GRAY + "Max Nearby: " + ChatColor.WHITE + point.getMaxNearbyEntities()
                    + ChatColor.GRAY + " (refills when " + SpawnPoint.REFILL_TRIGGER_NEARBY_ENTITIES + " or fewer remain)");
            sender.sendMessage(ChatColor.GRAY + "Status: " + (point.isActive() ? ChatColor.GREEN + "Active" : ChatColor.RED + "Inactive"));
            if (point.isLocationValid()) {
                Location loc = point.getLocation();
                sender.sendMessage(ChatColor.GRAY + "Location: " + ChatColor.AQUA + 
                        String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ()));
                sender.sendMessage(ChatColor.GRAY + "World: " + ChatColor.WHITE + loc.getWorld().getName());
            }
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid spawn point ID format.");
        }
    }

    private void handleToggle(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /mobspawn toggle <spawn_point_id>");
            return;
        }

        try {
            UUID pointId = UUID.fromString(args[1]);
            SpawnPoint point = monsterManager.getSpawnPoint(pointId);
            if (point == null) {
                sender.sendMessage(ChatColor.RED + "Spawn point not found: " + pointId);
                return;
            }

            point.setActive(!point.isActive());
            monsterManager.saveSpawnPoints();

            String status = point.isActive() ? "activated" : "deactivated";
            sender.sendMessage(ChatColor.GREEN + "Spawn point " + status + "!");
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid spawn point ID format.");
        }
    }

    private void handleMonsters(CommandSender sender) {
        Map<String, CustomMonster> monsters = monsterManager.getMonsters();
        if (monsters.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No custom monsters configured.");
            sender.sendMessage(ChatColor.GRAY + "Vanilla entity types are still supported (example: zombie, skeleton, enderman).");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "=== Custom Monsters (" + monsters.size() + ") ===");
        for (CustomMonster monster : monsters.values()) {
            sender.sendMessage(ChatColor.AQUA + monster.getId() + ChatColor.GRAY + " - " + 
                    ChatColor.YELLOW + monster.getDisplayName() + 
                    ChatColor.GRAY + " (" + monster.getEntityType().name() + ")");
        }
        sender.sendMessage(ChatColor.GRAY + "Vanilla entity types are also supported in create/set/spawn.");
        sender.sendMessage(ChatColor.GRAY + "Use /mobspawn gui to see them in a menu.");
    }

    private void handleSpawn(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /mobspawn spawn <monster_id|vanilla_entity_type> [amount]");
            return;
        }

        String monsterId = resolveMonsterIdOrSendError(sender, args[1]);
        if (monsterId == null) {
            return;
        }

        int amount = 1;
        if (args.length > 2) {
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid amount.");
                return;
            }
        }

        if (amount < 1) {
            sender.sendMessage(ChatColor.RED + "Amount must be at least 1.");
            return;
        }
        int maxManualSpawnAmount = monsterManager.getMaxManualSpawnAmount();
        if (amount > maxManualSpawnAmount) {
            sender.sendMessage(ChatColor.RED + "Amount too high. Maximum is " + maxManualSpawnAmount + ".");
            return;
        }

        CustomMonsterManager.SpawnBatchResult result = monsterManager.spawnMonstersSafely(monsterId, player.getLocation(), amount);
        if (result.spawned() <= 0) {
            sender.sendMessage(ChatColor.RED + "No mobs were spawned. The area is unsafe or already too crowded.");
            return;
        }

        sender.sendMessage(ChatColor.GREEN + "Spawned " + ChatColor.YELLOW + result.spawned() + "x "
                + monsterManager.describeMonster(monsterId) + ChatColor.GREEN + "!");
        if (result.blocked() > 0) {
            sender.sendMessage(ChatColor.YELLOW + String.valueOf(result.blocked())
                    + ChatColor.GRAY + " requested spawns were skipped by safety or density limits.");
        }
    }

    private void handleGui(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) return;

        monsterGui.open(player);
    }

    public MonsterGui getMonsterGui() {
        return monsterGui;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Monster Spawn Admin Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/mobspawn create <monster_id|vanilla_entity_type>" + ChatColor.GRAY + " - Create spawn at your location");
        sender.sendMessage(ChatColor.YELLOW + "/mobspawn set <id> <monster_id|vanilla_entity_type>" + ChatColor.GRAY + " - Change a spawn point's mob");
        sender.sendMessage(ChatColor.YELLOW + "/mobspawn remove <id>" + ChatColor.GRAY + " - Remove a spawn point");
        sender.sendMessage(ChatColor.YELLOW + "/mobspawn list" + ChatColor.GRAY + " - List all spawn points");
        sender.sendMessage(ChatColor.YELLOW + "/mobspawn info <id>" + ChatColor.GRAY + " - View spawn point details");
        sender.sendMessage(ChatColor.YELLOW + "/mobspawn toggle <id>" + ChatColor.GRAY + " - Activate/deactivate spawn");
        sender.sendMessage(ChatColor.YELLOW + "/mobspawn monsters" + ChatColor.GRAY + " - List available monsters");
        sender.sendMessage(ChatColor.YELLOW + "/mobspawn spawn <monster_id|vanilla_entity_type> [amt]" + ChatColor.GRAY + " - Manual spawn");
        sender.sendMessage(ChatColor.YELLOW + "/mobspawn gui" + ChatColor.GRAY + " - View monsters in GUI");
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("grivience.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            List<String> commands = new ArrayList<>(List.of("create", "set", "remove", "list", "info", "toggle", "monsters", "spawn", "gui", "help"));
            return filterPrefix(commands, args[0]);
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("spawn"))) {
            return filterPrefix(allMonsterSuggestions(), args[1]);
        }

        if (args.length == 2 && List.of("set", "remove", "info", "toggle").contains(args[0].toLowerCase(Locale.ROOT))) {
            return filterPrefix(spawnPointIdSuggestions(), args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return filterPrefix(allMonsterSuggestions(), args[2]);
        }

        return List.of();
    }

    private List<String> allMonsterSuggestions() {
        Set<String> suggestions = new LinkedHashSet<>();
        suggestions.addAll(monsterManager.getMonsters().keySet());
        suggestions.addAll(monsterManager.vanillaMobSuggestions());
        return new ArrayList<>(suggestions);
    }

    private List<String> spawnPointIdSuggestions() {
        List<String> ids = new ArrayList<>();
        for (SpawnPoint point : monsterManager.getSpawnPoints()) {
            ids.add(point.getId().toString());
        }
        return ids;
    }

    private String resolveMonsterIdOrSendError(CommandSender sender, String rawId) {
        String normalizedId = monsterManager.normalizeMonsterId(rawId);
        if (normalizedId != null) {
            return normalizedId;
        }

        sender.sendMessage(ChatColor.RED + "Unknown mob type: " + rawId);
        sender.sendMessage(ChatColor.GRAY + "Use /mobspawn monsters for custom IDs, or use a vanilla entity type like zombie.");
        return null;
    }

    private List<String> filterPrefix(List<String> input, String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        List<String> filtered = new ArrayList<>();
        for (String candidate : input) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                filtered.add(candidate);
            }
        }
        return filtered;
    }
}
