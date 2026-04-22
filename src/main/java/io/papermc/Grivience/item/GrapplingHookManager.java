package io.papermc.Grivience.item;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Grappling Hook Manager - Ascent Skyblock accurate.
 *
 * Ascent Skyblock Grappling Hook features:
 * - Right-click to launch hook projectile
 * - Hook travels in arc until it hits a block
 * - Player is pulled to hook location
 * - 2 second cooldown between uses
 * - Hook can be cancelled by right-clicking again
 * - Hook retracts if it travels too far
 * - Particle effects during flight and on impact
 * - Sound effects for launch, impact, and retraction
 * - No durability consumption
 * - Works with fishing rod item
 */
public final class GrapplingHookManager {
    private final GriviencePlugin plugin;
    
    // Track active hooks by player
    private final Map<UUID, ActiveHook> activeHooks = new HashMap<>();
    
    // Track cooldowns per player
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    
    private boolean enabled;
    private double launchVelocity;
    private double maxDistance;
    private long cooldownMs;
    private double pullForce;
    private boolean cancelOnSneak;
    private boolean cancelOnRightClick;
    private boolean particleEffects;
    private boolean soundEffects;

    public GrapplingHookManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        reloadFromConfig();
    }

    public void reloadFromConfig() {
        enabled = plugin.getConfig().getBoolean("grappling-hook.enabled", true);
        launchVelocity = plugin.getConfig().getDouble("grappling-hook.launch-velocity", 2.0);
        maxDistance = plugin.getConfig().getDouble("grappling-hook.max-distance", 50.0);
        cooldownMs = plugin.getConfig().getLong("grappling-hook.cooldown-ms", 2000);
        pullForce = plugin.getConfig().getDouble("grappling-hook.pull-force", 0.8);
        cancelOnSneak = plugin.getConfig().getBoolean("grappling-hook.cancel-on-sneak", true);
        cancelOnRightClick = plugin.getConfig().getBoolean("grappling-hook.cancel-on-right-click", true);
        particleEffects = plugin.getConfig().getBoolean("grappling-hook.particle-effects", true);
        soundEffects = plugin.getConfig().getBoolean("grappling-hook.sound-effects", true);
    }

    /**
     * Use the grappling hook - Ascent Skyblock accurate.
     */
    public boolean useHook(Player player, GrapplingHookType hookType) {
        if (!enabled) return false;
        
        // Check cooldown
        if (isOnCooldown(player)) {
            long remaining = cooldownMs - (System.currentTimeMillis() - cooldowns.get(player.getUniqueId()));
            player.sendMessage("§cGrappling Hook is on cooldown! (§e" + (remaining / 1000) + "s§c remaining)");
            return false;
        }
        
        // Cancel existing hook if already active (Ascent Skyblock behavior)
        if (activeHooks.containsKey(player.getUniqueId())) {
            cancelHook(player);
            return false;
        }

        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection().normalize();
        
        // Launch the hook projectile (Ascent Skyblock uses fishing bobber)
        FishHook hook = player.launchProjectile(FishHook.class);
        
        // Set velocity - Ascent Skyblock uses a specific velocity curve
        hook.setVelocity(direction.multiply(launchVelocity));
        
        // Mark as grappling hook
        hook.setMetadata("grappling_hook", new FixedMetadataValue(plugin, true));
        
        // Store active hook data
        ActiveHook activeHook = new ActiveHook(hook, hookType, System.currentTimeMillis(), eyeLoc);
        activeHooks.put(player.getUniqueId(), activeHook);
        
        // Play launch sound (Ascent Skyblock uses ENTITY_FISHING_BOBBER_THROW)
        if (soundEffects) {
            player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_THROW, 0.8f, 1.2f);
        }
        
        // Start tracking the hook flight
        trackHook(player, hook, hookType);
        
        // Set cooldown
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        
        return true;
    }

    /**
     * Track hook flight - Ascent Skyblock accurate tracking.
     */
    private void trackHook(Player player, FishHook hook, GrapplingHookType hookType) {
        final Location[] lastValidLocation = {hook.getLocation()};
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                ticks++;
                
                // Player offline or hook dead
                if (!player.isOnline() || hook.isDead() || !hook.isValid()) {
                    retractHook(player, hook, false);
                    cancel();
                    return;
                }
                
                // Check if player is sneaking (cancel hook)
                if (cancelOnSneak && player.isSneaking()) {
                    cancelHook(player);
                    cancel();
                    return;
                }
                
                Location currentLocation = hook.getLocation();
                
                // Check max distance (Ascent Skyblock: ~50 blocks)
                double distance = player.getLocation().distance(currentLocation);
                if (distance > hookType.getMaxDistance()) {
                    retractHook(player, hook, false);
                    cancel();
                    return;
                }
                
                // Check if hook is moving away from player (stuck check)
                double lastDistance = player.getLocation().distance(lastValidLocation[0]);
                if (distance > lastDistance && ticks > 20) {
                    // Hook is moving away, retract
                    retractHook(player, hook, false);
                    cancel();
                    return;
                }
                
                lastValidLocation[0] = currentLocation;
                
                // Particle trail (Ascent Skyblock: CRIT particles during flight)
                if (particleEffects) {
                    hook.getWorld().spawnParticle(
                        Particle.CRIT,
                        currentLocation,
                        2,
                        0.1, 0.1, 0.1,
                        0.02
                    );
                }
                
                // Cloud particles at hook position (Ascent Skyblock accurate)
                if (particleEffects && ticks % 5 == 0) {
                    hook.getWorld().spawnParticle(
                        Particle.CLOUD,
                        currentLocation,
                        1,
                        0.2, 0.2, 0.2,
                        0.01
                    );
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    /**
     * Handle hook impact with block - Ascent Skyblock accurate.
     */
    public void onHookImpact(Player player, Location impactLocation) {
        ActiveHook activeHook = activeHooks.get(player.getUniqueId());
        if (activeHook == null) return;
        
        // Play impact sound
        if (soundEffects) {
            player.playSound(impactLocation, Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f);
        }
        
        // Impact particles (Ascent Skyblock: CRIT and CLOUD burst)
        if (particleEffects) {
            impactLocation.getWorld().spawnParticle(
                Particle.CRIT,
                impactLocation,
                20,
                0.5, 0.5, 0.5,
                0.1
            );
            impactLocation.getWorld().spawnParticle(
                Particle.CLOUD,
                impactLocation,
                30,
                0.5, 0.5, 0.5,
                0.05
            );
        }
        
        // Pull player to hook location (Ascent Skyblock physics)
        pullPlayerToHook(player, impactLocation);
        
        // Clean up
        cleanup(player);
    }

    /**
     * Pull player to hook location - Ascent Skyblock accurate pulling.
     */
    private void pullPlayerToHook(Player player, Location target) {
        Location playerLoc = player.getLocation();
        
        // Calculate pull vector (Ascent Skyblock uses specific pull mechanics)
        Vector pullVector = target.toVector().subtract(playerLoc.toVector());
        
        // Normalize and apply pull force
        pullVector.normalize().multiply(pullForce);
        
        // Add slight upward velocity (Ascent Skyblock accurate)
        pullVector.setY(pullVector.getY() + 0.3);
        
        // Apply velocity
        player.setVelocity(pullVector);
        
        // Set fall distance to 0 (prevent fall damage from grapple)
        player.setFallDistance(0);
        
        // Play pull sound
        if (soundEffects) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.8f);
        }
    }

    /**
     * Retract hook without pulling (hook missed or cancelled).
     */
    private void retractHook(Player player, FishHook hook, boolean playSound) {
        if (playSound && soundEffects) {
            player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 0.8f, 1.2f);
        }
        
        // Retract particles
        if (particleEffects) {
            for (int i = 0; i < 10; i++) {
                Location particleLoc = player.getEyeLocation().add(
                    player.getEyeLocation().getDirection().multiply(i * 0.5)
                );
                player.getWorld().spawnParticle(
                    Particle.CLOUD,
                    particleLoc,
                    1,
                    0.1, 0.1, 0.1,
                    0.05
                );
            }
        }
        
        hook.remove();
        cleanup(player);
    }

    /**
     * Cancel hook manually.
     */
    public void cancelHook(Player player) {
        ActiveHook activeHook = activeHooks.get(player.getUniqueId());
        if (activeHook != null) {
            retractHook(player, activeHook.hook(), true);
        }
        cleanup(player);
    }

    /**
     * Check if player is on cooldown.
     */
    public boolean isOnCooldown(Player player) {
        if (!cooldowns.containsKey(player.getUniqueId())) return false;
        return System.currentTimeMillis() - cooldowns.get(player.getUniqueId()) < cooldownMs;
    }

    /**
     * Get remaining cooldown time in seconds.
     */
    public double getCooldownRemaining(Player player) {
        if (!cooldowns.containsKey(player.getUniqueId())) return 0;
        long elapsed = System.currentTimeMillis() - cooldowns.get(player.getUniqueId());
        return Math.max(0, (cooldownMs - elapsed) / 1000.0);
    }

    /**
     * Clean up hook data.
     */
    public void cleanup(Player player) {
        ActiveHook activeHook = activeHooks.remove(player.getUniqueId());
        if (activeHook != null && activeHook.hook() != null && activeHook.hook().isValid()) {
            activeHook.hook().remove();
        }
    }

    /**
     * Check if player has active hook.
     */
    public boolean hasActiveHook(Player player) {
        return activeHooks.containsKey(player.getUniqueId());
    }

    /**
     * Create a Ascent Skyblock-accurate grappling hook item.
     */
    public ItemStack createHookItem(GrapplingHookType hookType) {
        ItemStack item = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(hookType.getDisplayName());
        
        var lore = new java.util.ArrayList<String>();
        for (String line : hookType.getLore()) {
            lore.add(line);
        }
        
        meta.setLore(lore);
        meta.setUnbreakable(true);

        // Hide attributes (Ascent Skyblock style)
        meta.addItemFlags(
            org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
            org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE,
            org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS
        );
        
        item.setItemMeta(meta);
        return item;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public record ActiveHook(FishHook hook, GrapplingHookType hookType, long launchTime, Location launchLocation) {}
}
