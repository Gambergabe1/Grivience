package io.papermc.Grivience.item;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;

/**
 * Grappling Hook Listener - 99% Ascent Skyblock accurate.
 * 
 * Ascent Skyblock behaviors implemented:
 * - Right-click to launch
 * - Right-click again to cancel
 * - Sneak to cancel
 * - Cannot fish while hook active
 * - Hook retracts on disconnect
 * - Cooldown enforcement
 */
public final class GrapplingHookListener implements Listener {
    private final GriviencePlugin plugin;
    private final GrapplingHookManager hookManager;

    public GrapplingHookListener(GriviencePlugin plugin, GrapplingHookManager hookManager) {
        this.plugin = plugin;
        this.hookManager = hookManager;
    }

    /**
     * Handle right-click to use grappling hook - Ascent Skyblock accurate.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!hookManager.isEnabled()) return;
        
        Player player = event.getPlayer();
        Action action = event.getAction();
        
        // Only handle right-click actions
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;
        
        // Check if item is a grappling hook
        GrapplingHookType hookType = getHookType(item);
        if (hookType == null) return;

        // Don't break normal interactions (chests, doors, etc.) just because the player is holding the hook.
        // Players can still use the hook by clicking air or a non-interactable block.
        if (action == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Material clicked = event.getClickedBlock().getType();
            if (clicked.isInteractable()) {
                // If the hook is currently active, allow using a chest to also "cancel" it without blocking the click.
                if (hookManager.hasActiveHook(player)) {
                    hookManager.cancelHook(player);
                    player.sendMessage(ChatColor.GRAY + "Grappling Hook cancelled.");
                }
                return;
            }
        }
        
        event.setCancelled(true);
        
        // If player already has active hook, cancel it (Ascent Skyblock behavior)
        if (hookManager.hasActiveHook(player)) {
            hookManager.cancelHook(player);
            player.sendMessage(ChatColor.GRAY + "Grappling Hook cancelled.");
            return;
        }
        
        // Use the grappling hook
        if (hookManager.useHook(player, hookType)) {
            // No durability consumption (Ascent Skyblock accurate)
            // Grappling hooks in Ascent Skyblock don't consume durability
        }
    }

    /**
     * Handle projectile hit (hook landing on block) - Ascent Skyblock accurate.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!hookManager.isEnabled()) return;
        
        if (!(event.getEntity() instanceof FishHook hook)) return;
        if (!isGrapplingHook(hook)) return;
        
        // Get the player who threw it
        Player player = hook.getShooter() instanceof Player p ? p : null;
        if (player == null) return;
        
        // Handle block hit
        var hitBlock = event.getHitBlock();
        var hitFace = event.getHitBlockFace();
        
        if (hitBlock != null && hitFace != null) {
            // Calculate impact location on block face (Ascent Skyblock accurate)
            var impactLocation = hitBlock.getLocation().clone().add(
                hitFace.getDirection().multiply(0.5)
            );
            
            // Offset to player height for better landing
            impactLocation.setY(impactLocation.getY() + 1.0);
            
            hookManager.onHookImpact(player, impactLocation);
        }
    }

    /**
     * Handle hook hitting entity - Ascent Skyblock accurate (no effect on entities).
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectileHitEntity(ProjectileHitEvent event) {
        if (!hookManager.isEnabled()) return;
        if (!(event.getEntity() instanceof FishHook hook)) return;
        if (!isGrapplingHook(hook)) return;
        
        // Ascent Skyblock grappling hooks don't work on entities - retract
        var hitEntity = event.getHitEntity();
        if (hitEntity != null) {
            Player player = hook.getShooter() instanceof Player p ? p : null;
            if (player != null) {
                hookManager.cancelHook(player);
            }
        }
    }

    /**
     * Prevent fishing while grappling hook is active - Ascent Skyblock accurate.
     */
    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (!hookManager.isEnabled()) return;
        
        Player player = event.getPlayer();
        
        // Cancel fishing if grappling hook is active
        if (hookManager.hasActiveHook(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Cannot fish while grappling hook is active!");
            return;
        }
        
        // Check if using grappling hook item
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (getHookType(item) != null) {
            event.setCancelled(true);
        }
    }

    /**
     * Handle sneak to cancel hook - Ascent Skyblock accurate.
     */
    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        if (!hookManager.isEnabled()) return;
        if (!event.isSneaking()) return;
        
        Player player = event.getPlayer();
        
        // Cancel hook if player sneaks (optional Ascent Skyblock behavior)
        if (hookManager.hasActiveHook(player)) {
            hookManager.cancelHook(player);
            player.sendMessage(ChatColor.GRAY + "Grappling Hook cancelled.");
        }
    }

    /**
     * Clean up when player disconnects - Ascent Skyblock accurate.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        hookManager.cleanup(event.getPlayer());
    }

    /**
     * Check if an item is a grappling hook - Ascent Skyblock accurate detection.
     */
    private GrapplingHookType getHookType(ItemStack item) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        
        var lore = item.getItemMeta().getLore();
        if (lore == null) return null;
        
        // Check for grappling hook identifier in lore
        for (String line : lore) {
            String lowerLine = line.toLowerCase();
            if (lowerLine.contains("grapple") || lowerLine.contains("grappling hook")) {
                return GrapplingHookType.GRAPPLING_HOOK;
            }
            // Check for Ascent Skyblock-style ability text
            if (lowerLine.contains("right-click to launch")) {
                return GrapplingHookType.GRAPPLING_HOOK;
            }
        }
        
        // Check custom model data
        if (item.getItemMeta().hasCustomModelData()) {
            int modelData = item.getItemMeta().getCustomModelData();
            if (modelData >= 1001 && modelData <= 1010) {
                return GrapplingHookType.GRAPPLING_HOOK;
            }
        }
        
        return null;
    }

    /**
     * Check if a projectile is a grappling hook.
     */
    private boolean isGrapplingHook(FishHook hook) {
        for (MetadataValue meta : hook.getMetadata("grappling_hook")) {
            if (meta.asBoolean()) return true;
        }
        return false;
    }
}
