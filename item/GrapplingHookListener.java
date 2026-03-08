package io.papermc.Grivience.item;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Material;
import org.bukkit.entity.FishingHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataType;

/**
 * Listener for grappling hook events - 100% Skyblock accurate.
 */
public final class GrapplingHookListener implements Listener {
    private final GriviencePlugin plugin;
    private final GrapplingHookManager hookManager;

    public GrapplingHookListener(GriviencePlugin plugin, GrapplingHookManager hookManager) {
        this.plugin = plugin;
        this.hookManager = hookManager;
    }

    /**
     * Handle right-click to use grappling hook.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!hookManager.isEnabled()) return;
        
        Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.FISHING_ROD || !item.hasItemMeta()) return;
        
        // Detect hook via PDC
        String hookId = item.getItemMeta().getPersistentDataContainer().get(plugin.getGrapplingHookKey(), PersistentDataType.STRING);
        if (hookId == null) return;
        
        GrapplingHookType hookType = GrapplingHookType.parse(hookId);
        if (hookType == null) return;
        
        event.setCancelled(true);
        
        // Use the grappling hook
        hookManager.useHook(player, hookType);
    }

    /**
     * Handle projectile hit (hook landing).
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!hookManager.isEnabled()) return;
        
        if (!(event.getEntity() instanceof FishingHook hook)) return;
        
        // Check if this is a grappling hook
        if (!isGrapplingHook(hook)) return;
        
        // Find the shooter
        if (!(hook.getShooter() instanceof Player player)) return;
        
        // Impact location
        var hitBlock = event.getHitBlock();
        if (hitBlock != null) {
            // Offset to avoid getting stuck in the block
            hookManager.onHookImpact(player, hook.getLocation());
        }
    }

    /**
     * Cancel hook on sneak (Skyblock accurate).
     */
    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking() && hookManager.hasActiveHook(event.getPlayer())) {
            hookManager.cancelHook(event.getPlayer());
        }
    }

    /**
     * Prevent fishing with grappling hooks.
     */
    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (!hookManager.isEnabled()) return;
        
        Player player = event.getPlayer();
        if (hookManager.hasActiveHook(player)) {
            event.setCancelled(true);
        }
    }

    /**
     * Clean up when player disconnects.
     */
    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        hookManager.cleanup(event.getPlayer());
    }

    /**
     * Check if a projectile is a grappling hook.
     */
    private boolean isGrapplingHook(FishingHook hook) {
        for (MetadataValue meta : hook.getMetadata("grappling_hook")) {
            if (meta.asBoolean()) return true;
        }
        return false;
    }
}
