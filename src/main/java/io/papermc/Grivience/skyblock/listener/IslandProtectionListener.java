package io.papermc.Grivience.skyblock.listener;

import io.papermc.Grivience.skyblock.island.Island;
import io.papermc.Grivience.skyblock.island.IslandManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public final class IslandProtectionListener implements Listener {
    private final IslandManager islandManager;
    private final Set<Material> protectedInteract;

    public IslandProtectionListener(IslandManager islandManager) {
        this.islandManager = islandManager;
        this.protectedInteract = buildProtectedInteract();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Island island = islandAt(event.getBlock().getLocation());
        if (island == null) {
            return;
        }
        if (isAllowed(player, island)) {
            return;
        }
        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + "Visitors cannot break blocks on this island.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.PHYSICAL) {
            return;
        }
        Location loc = event.getClickedBlock() != null ? event.getClickedBlock().getLocation() : event.getPlayer().getLocation();
        Island island = islandAt(loc);
        if (island == null) {
            return;
        }
        if (isAllowed(event.getPlayer(), island)) {
            return;
        }
        Material type = event.getClickedBlock() != null ? event.getClickedBlock().getType() : Material.AIR;
        if (protectedInteract.contains(type)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Visitors cannot interact with blocks here.");
        }
    }

    private Island islandAt(Location loc) {
        return islandManager != null ? islandManager.getIslandAt(loc) : null;
    }

    private boolean isAllowed(Player player, Island island) {
        UUID uuid = player.getUniqueId();
        UUID owner = island.getOwner();
        return (owner != null && owner.equals(uuid)) || island.isMember(uuid);
    }

    private Set<Material> buildProtectedInteract() {
        Set<Material> set = EnumSet.of(
                Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL,
                Material.ENDER_CHEST, Material.SHULKER_BOX, Material.WHITE_SHULKER_BOX, Material.ORANGE_SHULKER_BOX,
                Material.MAGENTA_SHULKER_BOX, Material.LIGHT_BLUE_SHULKER_BOX, Material.YELLOW_SHULKER_BOX,
                Material.LIME_SHULKER_BOX, Material.PINK_SHULKER_BOX, Material.GRAY_SHULKER_BOX,
                Material.LIGHT_GRAY_SHULKER_BOX, Material.CYAN_SHULKER_BOX, Material.PURPLE_SHULKER_BOX,
                Material.BLUE_SHULKER_BOX, Material.BROWN_SHULKER_BOX, Material.GREEN_SHULKER_BOX,
                Material.RED_SHULKER_BOX, Material.BLACK_SHULKER_BOX,
                Material.LEVER,
                Material.STONE_BUTTON, Material.OAK_BUTTON, Material.SPRUCE_BUTTON, Material.BIRCH_BUTTON,
                Material.JUNGLE_BUTTON, Material.ACACIA_BUTTON, Material.DARK_OAK_BUTTON, Material.CRIMSON_BUTTON,
                Material.WARPED_BUTTON, Material.MANGROVE_BUTTON, Material.BAMBOO_BUTTON, Material.CHERRY_BUTTON,
                Material.POLISHED_BLACKSTONE_BUTTON,
                Material.STONE_PRESSURE_PLATE, Material.HEAVY_WEIGHTED_PRESSURE_PLATE, Material.LIGHT_WEIGHTED_PRESSURE_PLATE,
                Material.OAK_PRESSURE_PLATE, Material.SPRUCE_PRESSURE_PLATE, Material.BIRCH_PRESSURE_PLATE,
                Material.JUNGLE_PRESSURE_PLATE, Material.ACACIA_PRESSURE_PLATE, Material.DARK_OAK_PRESSURE_PLATE,
                Material.CRIMSON_PRESSURE_PLATE, Material.WARPED_PRESSURE_PLATE, Material.MANGROVE_PRESSURE_PLATE,
                Material.BAMBOO_PRESSURE_PLATE, Material.CHERRY_PRESSURE_PLATE, Material.POLISHED_BLACKSTONE_PRESSURE_PLATE,
                Material.REPEATER, Material.COMPARATOR, Material.REDSTONE_WIRE, Material.REDSTONE_TORCH,
                Material.REDSTONE_WALL_TORCH, Material.LEGACY_REDSTONE_TORCH_ON, Material.LEGACY_REDSTONE_TORCH_OFF,
                Material.OBSERVER, Material.DISPENSER, Material.DROPPER, Material.PISTON, Material.STICKY_PISTON
        );
        return set;
    }
}
