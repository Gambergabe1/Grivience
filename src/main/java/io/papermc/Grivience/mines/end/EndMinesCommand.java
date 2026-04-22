package io.papermc.Grivience.mines.end;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.mines.end.mob.EndMinesMobManager;
import io.papermc.Grivience.mines.end.mob.EndMinesMobSpawnPoint;
import io.papermc.Grivience.mines.end.mob.EndMinesMobType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Player-facing entrypoint for the End Mines expansion.
 */
public final class EndMinesCommand implements CommandExecutor, TabCompleter {
    private static final List<String> ROOT_SUBCOMMANDS = List.of(
            "set",
            "generate",
            "status",
            "access",
            "heart",
            "convergence",
            "mobs",
            "mineables",
            "help"
    );

    private static final List<String> DEFAULT_MINEABLE_BLOCKS = List.of(
            "END_STONE",
            "END_STONE_BRICKS",
            "PURPUR_BLOCK",
            "PURPUR_PILLAR",
            "OBSIDIAN",
            "CRYING_OBSIDIAN",
            "AMETHYST_BLOCK",
            "BUDDING_AMETHYST",
            "CHORUS_PLANT",
            "CHORUS_FLOWER"
    );

    private final GriviencePlugin plugin;
    private final EndMinesManager endMinesManager;

    public EndMinesCommand(GriviencePlugin plugin, EndMinesManager endMinesManager) {
        this.plugin = plugin;
        this.endMinesManager = endMinesManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            String sub = args[0] == null ? "" : args[0].trim().toLowerCase(Locale.ROOT);
            return switch (sub) {
                case "set" -> handleSet(sender);
                case "generate" -> handleGenerate(sender);
                case "status" -> handleStatus(sender);
                case "access" -> handleAccess(sender, label, slice(args, 1));
                case "heart" -> handleHeart(sender, label, slice(args, 1));
                case "convergence" -> handleConvergence(sender, label, slice(args, 1));
                case "mobs" -> handleMobs(sender, label, slice(args, 1));
                case "mineables" -> handleMineables(sender, label, slice(args, 1));
                case "help" -> {
                    sendHelp(sender, label);
                    yield true;
                }
                default -> {
                    sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /" + label + " help.");
                    yield true;
                }
            };
        }

        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }

        if (plugin.getConfig().getBoolean("end-mines.access.operator-only", false) && !player.isOp()) {
            player.sendMessage(ChatColor.RED + "End Mines access is currently operator-only.");
            return true;
        }

        int required = plugin.getConfig().getInt("end-mines.required-level", 25);
        if (required > 0 && !player.hasPermission("grivience.admin")) {
            int level = plugin.getSkyblockStatsManager() == null ? 0 : plugin.getSkyblockStatsManager().getLevel(player);
            if (level < required) {
                player.sendMessage(ChatColor.RED + "You need Skyblock Level " + required + " to access the End Mines.");
                return true;
            }
        }

        if (endMinesManager.teleportToEndMines(player)) {
            player.sendMessage(ChatColor.GREEN + "Teleported to the End Mines!");
        }
        return true;
    }

    private boolean handleGenerate(CommandSender sender) {
        if (!requireOperator(sender)) {
            return true;
        }

        if (!endMinesManager.isEnabled()) {
            sender.sendMessage(ChatColor.RED + "End Mines is disabled in config.");
            return true;
        }

        if (endMinesManager.isGenerating()) {
            sender.sendMessage(ChatColor.YELLOW + "End Mines generation is already in progress.");
            return true;
        }

        endMinesManager.generateLayout(true);
        sender.sendMessage(ChatColor.GREEN + "Generating End Mines layout...");
        return true;
    }

    private boolean handleStatus(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== End Mines Status ===");
        sender.sendMessage(ChatColor.GRAY + "Enabled: " + ChatColor.AQUA + endMinesManager.isEnabled());
        sender.sendMessage(ChatColor.GRAY + "World: " + ChatColor.AQUA + endMinesManager.getWorldName());
        sender.sendMessage(ChatColor.GRAY + "Loaded: " + ChatColor.AQUA + (endMinesManager.getWorld() != null));
        sender.sendMessage(ChatColor.GRAY + "Generated: " + ChatColor.AQUA + endMinesManager.isGenerated());
        sender.sendMessage(ChatColor.GRAY + "Generating: " + ChatColor.AQUA + endMinesManager.isGenerating());
        sender.sendMessage(ChatColor.GRAY + "Access op-only: " + ChatColor.AQUA + plugin.getConfig().getBoolean("end-mines.access.operator-only", false));
        sender.sendMessage(ChatColor.GRAY + "Mob spawn mode: " + ChatColor.AQUA + plugin.getConfig().getString("end-mines.mobs.spawn-mode", "around_players"));
        sender.sendMessage(ChatColor.GRAY + "Heart system: " + ChatColor.AQUA + plugin.getConfig().getBoolean("end-mines.heart.enabled", true));
        sender.sendMessage(ChatColor.GRAY + "Convergence: " + ChatColor.AQUA + plugin.getConfig().getBoolean("end-mines.convergence.enabled", true));
        sender.sendMessage(ChatColor.GRAY + "Kunzite world: " + ChatColor.AQUA + plugin.getConfig().getString(
                "end-hub.kunzite.world-name",
                plugin.getConfig().getString("skyblock.portal-routing.end.world-name", "world_the_end")
        ));
        EndMinesConvergenceManager convergenceManager = plugin.getEndMinesConvergenceManager();
        if (convergenceManager != null) {
            Location altar = convergenceManager.getAltarLocation();
            if (altar != null && altar.getWorld() != null) {
                sender.sendMessage(ChatColor.GRAY + "Convergence altar: " + ChatColor.AQUA + altar.getWorld().getName()
                        + ChatColor.GRAY + " (" + fmt(altar.getX()) + ", " + fmt(altar.getY()) + ", " + fmt(altar.getZ()) + ")");
            } else {
                sender.sendMessage(ChatColor.GRAY + "Convergence altar: " + ChatColor.RED + "Not configured");
            }
        }
        EndMinesMobManager mobManager = plugin.getEndMinesMobManager();
        if (mobManager != null) {
            sender.sendMessage(ChatColor.GRAY + "Mob spawn points: " + ChatColor.AQUA + mobManager.getSpawnPoints().size());
        } else {
            sender.sendMessage(ChatColor.GRAY + "Mob system: " + ChatColor.RED + "Not loaded");
        }
        sender.sendMessage(ChatColor.GRAY + "Mineable blocks: " + ChatColor.AQUA + getMineableBlockNames().size());
        return true;
    }

    private boolean handleSet(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
            return true;
        }
        if (!requireOperator(sender)) {
            return true;
        }

        Location loc = player.getLocation();
        plugin.getConfig().set("end-mines.world-name", loc.getWorld().getName());
        plugin.getConfig().set("end-mines.spawn.x", loc.getX());
        plugin.getConfig().set("end-mines.spawn.y", loc.getY());
        plugin.getConfig().set("end-mines.spawn.z", loc.getZ());
        plugin.getConfig().set("end-mines.spawn.yaw", loc.getYaw());
        plugin.getConfig().set("end-mines.spawn.pitch", loc.getPitch());
        plugin.saveConfig();
        if (plugin.getFastTravelManager() != null) {
            plugin.getFastTravelManager().syncHubWarpsFromConfig();
        }

        sender.sendMessage(ChatColor.GREEN + "End Mines spawn set to " + loc.getWorld().getName() + " (" +
                String.format("%.1f", loc.getX()) + ", " + String.format("%.1f", loc.getY()) + ", " +
                String.format("%.1f", loc.getZ()) + ").");
        return true;
    }

    private boolean requireOperator(CommandSender sender) {
        if (sender == null) {
            return false;
        }
        if (sender instanceof ConsoleCommandSender) {
            return true;
        }
        if (sender.isOp()) {
            return true;
        }
        sender.sendMessage(ChatColor.RED + "This command is operator-only.");
        return false;
    }

    private String[] slice(String[] input, int fromIndex) {
        if (input == null || fromIndex >= input.length) {
            return new String[0];
        }
        int size = input.length - fromIndex;
        String[] out = new String[size];
        System.arraycopy(input, fromIndex, out, 0, size);
        return out;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "=== End Mines Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + ChatColor.GRAY + " - Teleport to the End Mines");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " status" + ChatColor.GRAY + " - View End Mines status");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " set" + ChatColor.DARK_GRAY + " (OP)" + ChatColor.GRAY + " - Set End Mines spawn");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " generate" + ChatColor.DARK_GRAY + " (OP)" + ChatColor.GRAY + " - (Re)generate the layout");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " access <on|off|status>" + ChatColor.DARK_GRAY + " (OP)" + ChatColor.GRAY + " - Toggle op-only access");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " heart [open|stats]" + ChatColor.GRAY + " - Open or inspect Heart of the End Mines");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " heart reset <player|profileId>" + ChatColor.DARK_GRAY + " (OP)" + ChatColor.GRAY + " - Reset Heart progress");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " convergence [status|claim]" + ChatColor.GRAY + " - View or complete End Convergence");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " convergence setaltar" + ChatColor.DARK_GRAY + " (OP)" + ChatColor.GRAY + " - Set the altar to the block you are looking at");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " convergence resetcooldown <player|profileId>" + ChatColor.DARK_GRAY + " (OP)" + ChatColor.GRAY + " - Clear a Convergence cooldown");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " mobs ..." + ChatColor.DARK_GRAY + " (OP)" + ChatColor.GRAY + " - Configure mob spawn points");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " mineables ..." + ChatColor.DARK_GRAY + " (OP)" + ChatColor.GRAY + " - Configure mineable blocks");
    }

    private void sendMobsHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "=== End Mines Mob Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " mobs types" + ChatColor.GRAY + " - List mob types");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " mobs mode <around_players|spawn_points|mixed|status>" + ChatColor.GRAY + " - Set spawn mode");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " mobs list" + ChatColor.GRAY + " - List spawn points");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " mobs add <type> [radius] [delayTicks] [maxNearby]" + ChatColor.GRAY + " - Add at your location");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " mobs move <uuid>" + ChatColor.GRAY + " - Move spawn point to your location");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " mobs set <uuid> <type> [radius] [delayTicks] [maxNearby]" + ChatColor.GRAY + " - Update settings");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " mobs toggle <uuid>" + ChatColor.GRAY + " - Enable/disable a spawn point");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " mobs remove <uuid>" + ChatColor.GRAY + " - Remove a spawn point");
    }

    private void sendMineablesHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "=== End Mines Mineables Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " mineables list" + ChatColor.GRAY + " - List mineable blocks");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " mineables add <material>" + ChatColor.GRAY + " - Add a mineable block");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " mineables remove <material>" + ChatColor.GRAY + " - Remove a mineable block");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " mineables reset" + ChatColor.GRAY + " - Reset to defaults");
    }

    private boolean handleAccess(CommandSender sender, String label, String[] args) {
        if (!requireOperator(sender)) {
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            sender.sendMessage(ChatColor.GRAY + "End Mines access op-only: " + ChatColor.AQUA
                    + plugin.getConfig().getBoolean("end-mines.access.operator-only", false));
            return true;
        }

        boolean enabled;
        switch (args[0].trim().toLowerCase(Locale.ROOT)) {
            case "on", "true", "enable", "enabled" -> enabled = true;
            case "off", "false", "disable", "disabled" -> enabled = false;
            default -> {
                sender.sendMessage(ChatColor.RED + "Usage: /" + label + " access <on|off|status>");
                return true;
            }
        }

        plugin.getConfig().set("end-mines.access.operator-only", enabled);
        plugin.saveConfig();
        sender.sendMessage(ChatColor.GREEN + "End Mines access op-only set to " + ChatColor.AQUA + enabled + ChatColor.GREEN + ".");
        return true;
    }

    private boolean handleHeart(CommandSender sender, String label, String[] args) {
        HeartOfTheEndMinesManager heartManager = plugin.getHeartOfTheEndMinesManager();
        if (heartManager == null || !heartManager.isEnabled()) {
            sender.sendMessage(ChatColor.RED + "Heart of the End Mines is disabled.");
            return true;
        }

        if (args.length > 0) {
            String sub = args[0].trim().toLowerCase(Locale.ROOT);
            if (sub.equals("reset")) {
                if (!requireOperator(sender)) {
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " heart reset <player|profileId>");
                    return true;
                }
                HeartOfTheEndMinesManager.HeartProfileRef target = heartManager.resolveProfileTarget(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Heart profile target not found.");
                    return true;
                }
                boolean reset = heartManager.resetProfileProgress(target.profileId());
                if (!reset) {
                    sender.sendMessage(ChatColor.RED + "No Heart of the End Mines progress was found for " + target.displayName() + ChatColor.RED + ".");
                    return true;
                }
                sender.sendMessage(ChatColor.GREEN + "Reset Heart of the End Mines progress for " + ChatColor.AQUA
                        + target.displayName() + ChatColor.GREEN + ".");
                return true;
            }

            Player player = requirePlayer(sender);
            if (player == null) {
                return true;
            }
            if (sub.equals("stats") || sub.equals("status")) {
                heartManager.sendStats(player);
                return true;
            }
            if (!sub.equals("open")) {
                sender.sendMessage(ChatColor.RED + "Usage: /" + label + " heart [open|stats|reset <player|profileId>]");
                return true;
            }
        }

        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        heartManager.openGui(player);
        return true;
    }

    private boolean handleConvergence(CommandSender sender, String label, String[] args) {
        EndMinesConvergenceManager convergenceManager = plugin.getEndMinesConvergenceManager();
        if (convergenceManager == null || !convergenceManager.isEnabled()) {
            sender.sendMessage(ChatColor.RED + "End Convergence is disabled.");
            return true;
        }

        if (args.length == 0 || args[0].trim().equalsIgnoreCase("status")) {
            Player player = requirePlayer(sender);
            if (player == null) {
                return true;
            }
            convergenceManager.sendStatus(player);
            return true;
        }

        String sub = args[0].trim().toLowerCase(Locale.ROOT);
        switch (sub) {
            case "claim", "complete" -> {
                Player player = requirePlayer(sender);
                if (player == null) {
                    return true;
                }
                convergenceManager.attempt(player);
                return true;
            }
            case "setaltar" -> {
                if (!requireOperator(sender)) {
                    return true;
                }
                Player player = requirePlayerInEndMines(sender);
                if (player == null) {
                    return true;
                }
                Block target = player.getTargetBlockExact(8);
                if (target == null || target.getType().isAir()) {
                    sender.sendMessage(ChatColor.RED + "Look directly at the altar block you want to use and try again.");
                    return true;
                }
                Material altarMaterial = convergenceManager.styleAltarBlock(target);
                Location altar = target.getLocation().add(0.5D, 0.0D, 0.5D);
                altar.setYaw(player.getLocation().getYaw());
                altar.setPitch(player.getLocation().getPitch());
                convergenceManager.setAltar(altar);
                sender.sendMessage(ChatColor.GREEN + "End Convergence altar set to " + ChatColor.AQUA + altar.getWorld().getName()
                        + ChatColor.GREEN + " (" + ChatColor.AQUA + fmt(altar.getX()) + ChatColor.GREEN + ", "
                        + ChatColor.AQUA + fmt(altar.getY()) + ChatColor.GREEN + ", "
                        + ChatColor.AQUA + fmt(altar.getZ()) + ChatColor.GREEN + ")"
                        + ChatColor.GRAY + " using " + ChatColor.LIGHT_PURPLE + altarMaterial.name() + ChatColor.GRAY + ".");
                return true;
            }
            case "resetcooldown" -> {
                if (!requireOperator(sender)) {
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " convergence resetcooldown <player|profileId>");
                    return true;
                }
                EndMinesConvergenceManager.ConvergenceProfileRef target = convergenceManager.resolveProfileTarget(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Convergence target not found.");
                    return true;
                }
                boolean reset = convergenceManager.resetCooldown(target.profileId());
                if (!reset) {
                    sender.sendMessage(ChatColor.RED + "No End Convergence cooldown was found for " + target.displayName() + ChatColor.RED + ".");
                    return true;
                }
                sender.sendMessage(ChatColor.GREEN + "Cleared End Convergence cooldown for " + ChatColor.AQUA
                        + target.displayName() + ChatColor.GREEN + ".");
                return true;
            }
            default -> {
                sender.sendMessage(ChatColor.RED + "Usage: /" + label + " convergence [status|claim|setaltar|resetcooldown <player|profileId>]");
                return true;
            }
        }
    }

    private boolean handleMineables(CommandSender sender, String label, String[] args) {
        if (!requireOperator(sender)) {
            return true;
        }

        if (args.length == 0) {
            sendMineablesHelp(sender, label);
            return true;
        }

        String sub = args[0].trim().toLowerCase(Locale.ROOT);
        switch (sub) {
            case "list" -> {
                List<String> blocks = getMineableBlockNames();
                sender.sendMessage(ChatColor.GOLD + "=== End Mines Mineable Blocks (" + blocks.size() + ") ===");
                for (String name : blocks) {
                    sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.AQUA + name);
                }
                return true;
            }
            case "add" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " mineables add <material>");
                    return true;
                }
                Material material = Material.matchMaterial(args[1]);
                if (material == null) {
                    sender.sendMessage(ChatColor.RED + "Unknown material: " + args[1]);
                    return true;
                }
                if (!material.isBlock()) {
                    sender.sendMessage(ChatColor.RED + "That material is not a block: " + material.name());
                    return true;
                }
                List<String> current = new ArrayList<>(getMineableBlockNames());
                String name = material.name();
                if (containsIgnoreCase(current, name)) {
                    sender.sendMessage(ChatColor.YELLOW + "Already mineable: " + ChatColor.AQUA + name);
                    return true;
                }
                current.add(name);
                plugin.getConfig().set("end-mines.protection.mineable-blocks", current);
                plugin.saveConfig();
                sender.sendMessage(ChatColor.GREEN + "Added mineable block: " + ChatColor.AQUA + name);
                return true;
            }
            case "remove" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " mineables remove <material>");
                    return true;
                }
                String target = args[1].trim().toUpperCase(Locale.ROOT);
                List<String> current = new ArrayList<>(getMineableBlockNames());
                boolean removed = current.removeIf(value -> value != null && value.trim().equalsIgnoreCase(target));
                if (!removed) {
                    sender.sendMessage(ChatColor.YELLOW + "Not in mineables list: " + ChatColor.AQUA + target);
                    return true;
                }
                plugin.getConfig().set("end-mines.protection.mineable-blocks", current);
                plugin.saveConfig();
                sender.sendMessage(ChatColor.GREEN + "Removed mineable block: " + ChatColor.AQUA + target);
                return true;
            }
            case "reset" -> {
                plugin.getConfig().set("end-mines.protection.mineable-blocks", new ArrayList<>(DEFAULT_MINEABLE_BLOCKS));
                plugin.saveConfig();
                sender.sendMessage(ChatColor.GREEN + "Reset End Mines mineable blocks to defaults.");
                return true;
            }
            default -> {
                sender.sendMessage(ChatColor.RED + "Usage: /" + label + " mineables <list|add|remove|reset>");
                return true;
            }
        }
    }

    private List<String> getMineableBlockNames() {
        List<String> configured = plugin.getConfig().getStringList("end-mines.protection.mineable-blocks");
        if (configured == null || configured.isEmpty()) {
            return DEFAULT_MINEABLE_BLOCKS;
        }

        List<String> normalized = new ArrayList<>();
        for (String value : configured) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            normalized.add(trimmed.toUpperCase(Locale.ROOT));
        }
        return normalized.isEmpty() ? DEFAULT_MINEABLE_BLOCKS : normalized;
    }

    private boolean containsIgnoreCase(List<String> values, String needle) {
        if (values == null || needle == null) {
            return false;
        }
        for (String value : values) {
            if (value != null && value.equalsIgnoreCase(needle)) {
                return true;
            }
        }
        return false;
    }

    private boolean handleMobs(CommandSender sender, String label, String[] args) {
        if (!requireOperator(sender)) {
            return true;
        }

        EndMinesMobManager mobManager = plugin.getEndMinesMobManager();
        if (mobManager == null) {
            sender.sendMessage(ChatColor.RED + "End Mines mob system is not loaded.");
            return true;
        }

        if (args.length == 0) {
            sendMobsHelp(sender, label);
            return true;
        }

        String sub = args[0].trim().toLowerCase(Locale.ROOT);
        switch (sub) {
            case "types" -> {
                sender.sendMessage(ChatColor.GOLD + "=== End Mines Mob Types ===");
                for (EndMinesMobType type : EndMinesMobType.values()) {
                    sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.AQUA + type.id()
                            + ChatColor.GRAY + " (" + type.displayName() + ChatColor.GRAY + ")");
                }
                return true;
            }
            case "mode" -> {
                return handleMobMode(sender, label, slice(args, 1));
            }
            case "list" -> {
                return handleMobList(sender, label, mobManager);
            }
            case "add" -> {
                return handleMobAdd(sender, label, mobManager, slice(args, 1));
            }
            case "remove" -> {
                return handleMobRemove(sender, label, mobManager, slice(args, 1));
            }
            case "toggle" -> {
                return handleMobToggle(sender, label, mobManager, slice(args, 1));
            }
            case "move" -> {
                return handleMobMove(sender, label, mobManager, slice(args, 1));
            }
            case "set" -> {
                return handleMobSet(sender, label, mobManager, slice(args, 1));
            }
            default -> {
                sender.sendMessage(ChatColor.RED + "Usage: /" + label + " mobs <types|mode|list|add|remove|toggle|move|set>");
                return true;
            }
        }
    }

    private boolean handleMobMode(CommandSender sender, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            sender.sendMessage(ChatColor.GRAY + "End Mines mob spawn mode: " + ChatColor.AQUA
                    + plugin.getConfig().getString("end-mines.mobs.spawn-mode", "around_players"));
            return true;
        }

        String mode = args[0].trim().toLowerCase(Locale.ROOT);
        if (!mode.equals("around_players") && !mode.equals("spawn_points") && !mode.equals("mixed")) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " mobs mode <around_players|spawn_points|mixed|status>");
            return true;
        }

        plugin.getConfig().set("end-mines.mobs.spawn-mode", mode);
        plugin.saveConfig();
        sender.sendMessage(ChatColor.GREEN + "End Mines mob spawn mode set to " + ChatColor.AQUA + mode + ChatColor.GREEN + ".");
        return true;
    }

    private boolean handleMobList(CommandSender sender, String label, EndMinesMobManager mobManager) {
        Collection<EndMinesMobSpawnPoint> points = mobManager.getSpawnPoints();
        if (points.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No End Mines mob spawn points configured.");
            sender.sendMessage(ChatColor.GRAY + "Use /" + label + " mobs add <type> to create one.");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "=== End Mines Mob Spawn Points (" + points.size() + ") ===");
        for (EndMinesMobSpawnPoint point : points) {
            String idShort = point.id().toString().substring(0, 8);
            String status = point.active() ? (ChatColor.GREEN + "ACTIVE") : (ChatColor.RED + "INACTIVE");
            sender.sendMessage(ChatColor.GRAY + idShort + " " + status
                    + ChatColor.GRAY + " type=" + ChatColor.AQUA + point.mobTypeId()
                    + ChatColor.GRAY + " @ " + ChatColor.AQUA + point.worldName()
                    + ChatColor.GRAY + " (" + fmt(point.x()) + ", " + fmt(point.y()) + ", " + fmt(point.z()) + ")"
                    + ChatColor.DARK_GRAY + " r=" + point.spawnRadius()
                    + " delay=" + point.spawnDelayTicks()
                    + " max=" + point.maxNearbyEntities());
        }
        return true;
    }

    private boolean handleMobAdd(CommandSender sender, String label, EndMinesMobManager mobManager, String[] args) {
        Player player = requirePlayerInEndMines(sender);
        if (player == null) {
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " mobs add <type> [spawnRadius] [delayTicks] [maxNearby]");
            return true;
        }

        EndMinesMobType type = EndMinesMobType.parse(args[0]);
        if (type == null) {
            sender.sendMessage(ChatColor.RED + "Unknown mob type: " + args[0]);
            sender.sendMessage(ChatColor.GRAY + "Use /" + label + " mobs types to list valid types.");
            return true;
        }

        int radius = args.length >= 2 ? parseInt(args[1], 6, 1, 64) : 6;
        int delay = args.length >= 3 ? parseInt(args[2], 100, 1, 20_000) : 100;
        int maxNearby = args.length >= 4
                ? parseInt(args[3], 6, 0, EndMinesMobSpawnPoint.MAX_NEARBY_ENTITIES_LIMIT)
                : 6;

        EndMinesMobSpawnPoint point = mobManager.createSpawnPoint(player.getLocation(), type, radius, delay, maxNearby);
        if (point == null) {
            sender.sendMessage(ChatColor.RED + "Failed to create spawn point.");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "Created End Mines mob spawn point " + ChatColor.AQUA
                + point.id().toString().substring(0, 8) + ChatColor.GREEN + " (" + ChatColor.AQUA + point.id()
                + ChatColor.GREEN + ").");
        return true;
    }

    private boolean handleMobRemove(CommandSender sender, String label, EndMinesMobManager mobManager, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " mobs remove <uuid>");
            return true;
        }

        UUID id = resolveSpawnPointId(sender, mobManager, args[0]);
        if (id == null) {
            return true;
        }

        boolean removed = mobManager.removeSpawnPoint(id);
        if (removed) {
            sender.sendMessage(ChatColor.GREEN + "Removed spawn point " + ChatColor.AQUA + id.toString().substring(0, 8) + ChatColor.GREEN + ".");
        } else {
            sender.sendMessage(ChatColor.RED + "Spawn point not found.");
        }
        return true;
    }

    private boolean handleMobToggle(CommandSender sender, String label, EndMinesMobManager mobManager, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " mobs toggle <uuid>");
            return true;
        }

        UUID id = resolveSpawnPointId(sender, mobManager, args[0]);
        if (id == null) {
            return true;
        }

        boolean toggled = mobManager.toggleSpawnPoint(id);
        if (!toggled) {
            sender.sendMessage(ChatColor.RED + "Spawn point not found.");
            return true;
        }

        EndMinesMobSpawnPoint point = mobManager.getSpawnPoint(id);
        sender.sendMessage(ChatColor.GREEN + "Spawn point " + ChatColor.AQUA + id.toString().substring(0, 8)
                + ChatColor.GREEN + " active=" + ChatColor.AQUA + (point != null && point.active()) + ChatColor.GREEN + ".");
        return true;
    }

    private boolean handleMobMove(CommandSender sender, String label, EndMinesMobManager mobManager, String[] args) {
        Player player = requirePlayerInEndMines(sender);
        if (player == null) {
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " mobs move <uuid>");
            return true;
        }

        UUID id = resolveSpawnPointId(sender, mobManager, args[0]);
        if (id == null) {
            return true;
        }

        boolean updated = mobManager.updateSpawnPoint(id, point -> point.setLocation(player.getLocation()));
        if (!updated) {
            sender.sendMessage(ChatColor.RED + "Spawn point not found.");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "Moved spawn point " + ChatColor.AQUA + id.toString().substring(0, 8)
                + ChatColor.GREEN + " to your location.");
        return true;
    }

    private boolean handleMobSet(CommandSender sender, String label, EndMinesMobManager mobManager, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " mobs set <uuid> <type> [spawnRadius] [delayTicks] [maxNearby]");
            return true;
        }

        UUID id = resolveSpawnPointId(sender, mobManager, args[0]);
        if (id == null) {
            return true;
        }

        EndMinesMobSpawnPoint existing = mobManager.getSpawnPoint(id);
        if (existing == null) {
            sender.sendMessage(ChatColor.RED + "Spawn point not found.");
            return true;
        }

        EndMinesMobType type = EndMinesMobType.parse(args[1]);
        if (type == null) {
            sender.sendMessage(ChatColor.RED + "Unknown mob type: " + args[1]);
            sender.sendMessage(ChatColor.GRAY + "Use /" + label + " mobs types to list valid types.");
            return true;
        }

        int radius = args.length >= 3 ? parseInt(args[2], existing.spawnRadius(), 1, 64) : existing.spawnRadius();
        int delay = args.length >= 4 ? parseInt(args[3], existing.spawnDelayTicks(), 1, 20_000) : existing.spawnDelayTicks();
        int maxNearby = args.length >= 5
                ? parseInt(args[4], existing.maxNearbyEntities(), 0, EndMinesMobSpawnPoint.MAX_NEARBY_ENTITIES_LIMIT)
                : existing.maxNearbyEntities();

        mobManager.updateSpawnPoint(id, point -> {
            point.setMobTypeId(type.id());
            point.setSpawnRadius(radius);
            point.setSpawnDelayTicks(delay);
            point.setMaxNearbyEntities(maxNearby);
        });

        sender.sendMessage(ChatColor.GREEN + "Updated spawn point " + ChatColor.AQUA + id.toString().substring(0, 8)
                + ChatColor.GREEN + " type=" + ChatColor.AQUA + type.id()
                + ChatColor.GREEN + " r=" + ChatColor.AQUA + radius
                + ChatColor.GREEN + " delay=" + ChatColor.AQUA + delay
                + ChatColor.GREEN + " max=" + ChatColor.AQUA + maxNearby + ChatColor.GREEN + ".");
        return true;
    }

    private Player requirePlayerInEndMines(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return null;
        }
        if (endMinesManager.getWorld() == null) {
            sender.sendMessage(ChatColor.RED + "End Mines world is not loaded.");
            return null;
        }
        if (!player.getWorld().equals(endMinesManager.getWorld())) {
            sender.sendMessage(ChatColor.RED + "You must be in the End Mines world to run this command.");
            return null;
        }
        return player;
    }

    private UUID resolveSpawnPointId(CommandSender sender, EndMinesMobManager mobManager, String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        String raw = token.trim();
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
        }

        String prefix = raw.toLowerCase(Locale.ROOT);
        List<UUID> matches = new ArrayList<>();
        for (EndMinesMobSpawnPoint point : mobManager.getSpawnPoints()) {
            String id = point.id().toString().toLowerCase(Locale.ROOT);
            if (id.startsWith(prefix)) {
                matches.add(point.id());
            }
        }

        if (matches.size() == 1) {
            return matches.get(0);
        }

        if (matches.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Unknown spawn point: " + raw);
            return null;
        }

        sender.sendMessage(ChatColor.RED + "Ambiguous spawn point id. Matches:");
        for (UUID id : matches) {
            sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.AQUA + id.toString().substring(0, 8) + ChatColor.GRAY + " (" + id + ")");
        }
        return null;
    }

    private int parseInt(String input, int fallback, int min, int max) {
        if (input == null || input.isBlank()) {
            return fallback;
        }
        try {
            int value = Integer.parseInt(input.trim());
            return Math.max(min, Math.min(max, value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private String fmt(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
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
        if (args.length == 1) {
            return filter(ROOT_SUBCOMMANDS, args[0]);
        }

        String root = args[0] == null ? "" : args[0].trim().toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            return switch (root) {
                case "heart" -> filter(sender != null && requireTabOperator(sender)
                        ? List.of("open", "stats", "reset")
                        : List.of("open", "stats"), args[1]);
                case "convergence" -> filter(sender != null && requireTabOperator(sender)
                        ? List.of("status", "claim", "setaltar", "resetcooldown")
                        : List.of("status", "claim"), args[1]);
                case "mobs" -> filter(List.of("types", "mode", "list", "add", "move", "set", "toggle", "remove"), args[1]);
                case "mineables" -> filter(List.of("list", "add", "remove", "reset"), args[1]);
                case "access" -> filter(List.of("on", "off", "status"), args[1]);
                default -> List.of();
            };
        }

        if (root.equals("heart") && args.length == 3 && args[1] != null && args[1].trim().equalsIgnoreCase("reset")) {
            List<String> playerNames = new ArrayList<>();
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                playerNames.add(player.getName());
            }
            return filter(playerNames, args[2]);
        }

        if (root.equals("convergence") && args.length == 3 && args[1] != null && args[1].trim().equalsIgnoreCase("resetcooldown")) {
            List<String> playerNames = new ArrayList<>();
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                playerNames.add(player.getName());
            }
            return filter(playerNames, args[2]);
        }

        if (root.equals("mobs")) {
            if (args.length == 3) {
                String sub = args[1] == null ? "" : args[1].trim().toLowerCase(Locale.ROOT);
                if (sub.equals("add")) {
                    return filter(mobTypeIds(), args[2]);
                }
                if (sub.equals("remove") || sub.equals("toggle") || sub.equals("move") || sub.equals("set")) {
                    return filter(spawnPointIdPrefixes(), args[2]);
                }
                if (sub.equals("mode")) {
                    return filter(List.of("around_players", "spawn_points", "mixed", "status"), args[2]);
                }
            }
            if (args.length == 4 && args[1] != null && args[1].trim().equalsIgnoreCase("set")) {
                return filter(mobTypeIds(), args[3]);
            }
        }

        if (root.equals("mineables") && args.length == 3 && args[1] != null && args[1].trim().equalsIgnoreCase("remove")) {
            return filter(getMineableBlockNames(), args[2]);
        }
        return List.of();
    }

    private boolean requireTabOperator(CommandSender sender) {
        return sender instanceof ConsoleCommandSender || sender.isOp();
    }

    private List<String> spawnPointIdPrefixes() {
        EndMinesMobManager mobManager = plugin.getEndMinesMobManager();
        if (mobManager == null) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (EndMinesMobSpawnPoint point : mobManager.getSpawnPoints()) {
            out.add(point.id().toString().substring(0, 8));
        }
        return out;
    }

    private List<String> mobTypeIds() {
        List<String> out = new ArrayList<>();
        for (EndMinesMobType type : EndMinesMobType.values()) {
            out.add(type.id());
        }
        return out;
    }

    private List<String> filter(List<String> input, String prefix) {
        String p = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String candidate : input) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(p)) {
                out.add(candidate);
            }
        }
        return out;
    }
}

