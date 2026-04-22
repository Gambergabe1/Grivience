package io.papermc.Grivience.skyblock.portal;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.util.CommandDispatchUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class PortalRoutingManager {
    private static final String BASE_PATH = "skyblock.portal-routing.";
    private static final Map<String, String> BUILTIN_TARGET_COMMANDS = Map.of(
            "hub", "hub",
            "minehub", "minehub",
            "farmhub", "farmhub",
            "dungeonhub", "dungeonhub",
            "island", "island go",
            "skyblock", "island go"
    );
    private static final Set<String> PRIMARY_BUILTIN_TARGETS = Set.of("hub", "minehub", "farmhub", "dungeonhub", "island");

    private final GriviencePlugin plugin;

    public PortalRoutingManager(GriviencePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled(PortalKind portalKind) {
        return plugin.getConfig().getBoolean(path(portalKind) + ".enabled", defaultEnabled(portalKind));
    }

    public void setEnabled(PortalKind portalKind, boolean enabled) {
        plugin.getConfig().set(path(portalKind) + ".enabled", enabled);
        plugin.saveConfig();
    }

    public String getTarget(PortalKind portalKind) {
        return plugin.getConfig().getString(path(portalKind) + ".target", "hub").toLowerCase(Locale.ROOT);
    }

    public String getWorldName(PortalKind portalKind) {
        return plugin.getConfig().getString(path(portalKind) + ".world-name", defaultWorldName(portalKind));
    }

    public void saveBuiltinTarget(PortalKind portalKind, String target) {
        plugin.getConfig().set(path(portalKind) + ".enabled", true);
        plugin.getConfig().set(path(portalKind) + ".target", target.toLowerCase(Locale.ROOT));
        plugin.saveConfig();
    }

    public void saveWorldTarget(PortalKind portalKind, String worldName) {
        plugin.getConfig().set(path(portalKind) + ".enabled", true);
        plugin.getConfig().set(path(portalKind) + ".target", "world");
        plugin.getConfig().set(path(portalKind) + ".world-name", worldName);
        plugin.saveConfig();
    }

    public boolean teleport(Player player, PortalKind portalKind) {
        if (player == null || !isEnabled(portalKind)) {
            return false;
        }

        String target = getTarget(portalKind);
        String command = BUILTIN_TARGET_COMMANDS.get(target);
        if (command != null) {
            return CommandDispatchUtil.dispatchPlayer(plugin, player, command);
        }
        if ("world".equals(target)) {
            return teleportToWorld(player, portalKind);
        }

        player.sendMessage(ChatColor.RED + "Portal route target '" + target + "' is not valid. Ask an administrator to update it.");
        plugin.getLogger().warning("Invalid " + portalKind.configKey() + " portal target configured: " + target);
        return false;
    }

    public String describeRoute(PortalKind portalKind) {
        if (!isEnabled(portalKind)) {
            return ChatColor.RED + "disabled";
        }

        String target = getTarget(portalKind);
        if ("world".equals(target)) {
            return ChatColor.GREEN + "world" + ChatColor.GRAY + " -> " + ChatColor.YELLOW + getWorldName(portalKind);
        }
        return ChatColor.GREEN + target;
    }

    public List<String> builtinTargets() {
        return PRIMARY_BUILTIN_TARGETS.stream().sorted().toList();
    }

    public List<String> availableWorlds() {
        return Bukkit.getWorlds().stream()
                .map(World::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public PortalKind detectLookedAtPortal(Player player) {
        if (player == null) {
            return null;
        }

        Block targetBlock = player.getTargetBlockExact(12);
        if (targetBlock == null) {
            return null;
        }

        return detectPortalKind(targetBlock);
    }

    private boolean teleportToWorld(Player player, PortalKind portalKind) {
        String worldName = getWorldName(portalKind);
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage(ChatColor.RED + "Configured world '" + worldName + "' is not loaded.");
            return false;
        }

        Location destination = world.getSpawnLocation().clone().add(0.5D, 0.0D, 0.5D);
        player.teleport(destination);
        player.playSound(destination, Sound.ENTITY_PLAYER_TELEPORT, 1.0F, 1.0F);
        player.sendMessage(ChatColor.GREEN + "Teleported to world '" + worldName + "' via " + portalKind.displayName() + " Portal.");
        return true;
    }

    private PortalKind detectPortalKind(Block block) {
        if (block == null) {
            return null;
        }
        if (isPortalBlockNearby(block, Material.NETHER_PORTAL)) {
            return PortalKind.NETHER;
        }
        if (isPortalBlockNearby(block, Material.END_PORTAL)) {
            return PortalKind.END;
        }
        return null;
    }

    private boolean isPortalBlockNearby(Block origin, Material portalMaterial) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (origin.getRelative(x, y, z).getType() == portalMaterial) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean defaultEnabled(PortalKind portalKind) {
        if (portalKind == PortalKind.NETHER) {
            return plugin.getConfig().getBoolean("skyblock.nether-portal-to-hub.enabled", true);
        }
        return false;
    }

    private String defaultWorldName(PortalKind portalKind) {
        if (portalKind == PortalKind.NETHER) {
            return plugin.getConfig().getString("skyblock.hub-world", "world");
        }
        return "world_the_end";
    }

    private String path(PortalKind portalKind) {
        return BASE_PATH + portalKind.configKey();
    }

    public enum PortalKind {
        NETHER("nether", "Nether"),
        END("end", "End");

        private final String configKey;
        private final String displayName;

        PortalKind(String configKey, String displayName) {
            this.configKey = configKey;
            this.displayName = displayName;
        }

        public String configKey() {
            return configKey;
        }

        public String displayName() {
            return displayName;
        }

        public static PortalKind fromArgument(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            String normalized = value.toLowerCase(Locale.ROOT);
            for (PortalKind portalKind : values()) {
                if (portalKind.configKey.equals(normalized)) {
                    return portalKind;
                }
            }
            return null;
        }
    }
}
