package io.papermc.Grivience.mines.end;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Prevents griefing of the End Mines layout while still allowing mining blocks.
 */
public final class EndMinesProtectionListener implements Listener {
    private final GriviencePlugin plugin;
    private final EndMinesManager endMinesManager;

    // Mineable blocks allowed by default. Can be extended via config later.
    private static final Set<Material> DEFAULT_MINEABLE = EnumSet.of(
            Material.END_STONE,
            Material.END_STONE_BRICKS,
            Material.PURPUR_BLOCK,
            Material.PURPUR_PILLAR,
            Material.OBSIDIAN,
            Material.CRYING_OBSIDIAN,
            Material.AMETHYST_BLOCK,
            Material.BUDDING_AMETHYST,
            Material.CHORUS_PLANT,
            Material.CHORUS_FLOWER
    );

    private volatile int cachedMineableHash = 0;
    private volatile Set<Material> cachedMineableBlocks = DEFAULT_MINEABLE;

    public EndMinesProtectionListener(GriviencePlugin plugin, EndMinesManager endMinesManager) {
        this.plugin = plugin;
        this.endMinesManager = endMinesManager;
    }

    private Set<Material> getMineableBlocks() {
        if (plugin == null) {
            return DEFAULT_MINEABLE;
        }

        List<String> configured = plugin.getConfig().getStringList("end-mines.protection.mineable-blocks");
        if (configured == null || configured.isEmpty()) {
            return DEFAULT_MINEABLE;
        }

        int hash = configured.hashCode();
        if (hash == cachedMineableHash && cachedMineableBlocks != null) {
            return cachedMineableBlocks;
        }

        EnumSet<Material> parsed = EnumSet.noneOf(Material.class);
        for (String value : configured) {
            if (value == null || value.isBlank()) {
                continue;
            }
            Material material = Material.matchMaterial(value.trim());
            if (material != null && material.isBlock()) {
                parsed.add(material);
            }
        }

        Set<Material> effective = parsed.isEmpty() ? DEFAULT_MINEABLE : parsed;
        cachedMineableHash = hash;
        cachedMineableBlocks = effective;
        return effective;
    }

    private boolean isEndMinesWorld(World world) {
        World end = endMinesManager == null ? null : endMinesManager.getWorld();
        return end != null && world != null && world.equals(end);
    }

    private boolean canBuild(Player player) {
        return player != null && (player.hasPermission("grivience.admin") || player.hasPermission("grivience.endmines.build"));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!endMinesManager.isEnabled()) {
            return;
        }
        if (!isEndMinesWorld(event.getBlock().getWorld())) {
            return;
        }
        if (canBuild(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage(org.bukkit.ChatColor.RED + "You cannot build in the End Mines.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!endMinesManager.isEnabled()) {
            return;
        }
        if (!isEndMinesWorld(event.getBlock().getWorld())) {
            return;
        }
        if (canBuild(event.getPlayer())) {
            return;
        }

        Material type = event.getBlock().getType();
        if (!getMineableBlocks().contains(type)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(org.bukkit.ChatColor.RED + "You cannot break that in the End Mines.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        if (!endMinesManager.isEnabled()) {
            return;
        }
        if (!isEndMinesWorld(event.getLocation().getWorld())) {
            return;
        }
        event.blockList().clear();
        event.setYield(0.0F);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPrime(ExplosionPrimeEvent event) {
        if (!endMinesManager.isEnabled()) {
            return;
        }
        if (!isEndMinesWorld(event.getEntity().getWorld())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEndermanGrief(EntityChangeBlockEvent event) {
        if (!endMinesManager.isEnabled()) {
            return;
        }
        if (!isEndMinesWorld(event.getBlock().getWorld())) {
            return;
        }
        // Enderman block pickup/placement grief prevention.
        event.setCancelled(true);
    }
}
