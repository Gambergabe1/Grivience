package io.papermc.Grivience.item;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.FishingHook;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Manages grappling hook mechanics and physics - 100% Skyblock accurate.
 */
public final class GrapplingHookManager {
    private final GriviencePlugin plugin;
    
    // Track active hooks by player
    private final Map<UUID, ActiveHook> activeHooks = new HashMap<>();
    
    // Track cooldowns
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    
    private boolean enabled;
    private boolean particleEffects;
    private boolean soundEffects;

    public GrapplingHookManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        reloadFromConfig();
    }

    public void reloadFromConfig() {
        var config = plugin.getConfig();
        enabled = config.getBoolean("grappling-hook.enabled", true);
        particleEffects = config.getBoolean("grappling-hook.particle-effects", true);
        soundEffects = config.getBoolean("grappling-hook.sound-effects", true);
    }

    /**
     * Use a grappling hook.
     * @param player The player using the hook
     * @param hookType The type of hook being used
     * @return true if successful
     */
    public boolean useHook(Player player, GrapplingHookType hookType) {
        if (!enabled) return false;

        UUID uuid = player.getUniqueId();

        // Handle cancellation if already active
        if (activeHooks.containsKey(uuid)) {
            cancelHook(player);
            return true;
        }

        // Check cooldown
        if (isOnCooldown(player)) {
            player.sendMessage(ChatColor.RED + "This ability is on cooldown for " + getCooldownRemaining(player) + "s.");
            return false;
        }

        // Launch the hook
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection();
        
        // Create fishing hook projectile
        FishingHook hook = player.launchProjectile(FishingHook.class);
        hook.setVelocity(direction.multiply(hookType.getLaunchVelocity()));
        hook.setMetadata("grappling_hook", new FixedMetadataValue(plugin, true));
        
        // Store active hook data
        ActiveHook activeHook = new ActiveHook(hook, hookType, System.currentTimeMillis());
        activeHooks.put(uuid, activeHook);
        
        // Set cooldown
        cooldowns.put(uuid, System.currentTimeMillis() + hookType.getCooldownMs());
        
        // Play launch sound
        if (soundEffects) {
            player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_THROW, 1.2f, 0.8f);
        }
        
        // Start tracking
        trackHook(player, hook, hookType);
        
        return true;
    }

    /**
     * Track a hook's flight.
     */
    private void trackHook(Player player, FishingHook hook, GrapplingHookType hookType) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || hook.isDead() || !hook.isValid()) {
                    cleanup(player);
                    cancel();
                    return;
                }
                
                Location currentLocation = hook.getLocation();
                
                // Check distance
                if (player.getLocation().distance(currentLocation) > hookType.getMaxDistance()) {
                    cancelHook(player);
                    cancel();
                    return;
                }
                
                // Particle trail
                if (particleEffects) {
                    hook.getWorld().spawnParticle(Particle.CRIT, currentLocation, 2, 0.05, 0.05, 0.05, 0.01);
                    hook.getWorld().spawnParticle(Particle.CLOUD, currentLocation, 1, 0.02, 0.02, 0.02, 0.01);
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    /**
     * Handle hook impact with a block.
     */
    public void onHookImpact(Player player, Location impactLocation) {
        ActiveHook activeHook = activeHooks.get(player.getUniqueId());
        if (activeHook == null) return;
        
        // 1:1 Hypixel Skyblock Physics:
        // Instead of normalizing, we use a fraction of the total distance vector
        // This makes long-distance grapples faster/snappier.
        Vector playerVec = player.getLocation().toVector();
        Vector impactVec = impactLocation.toVector();
        
        // Calculate the difference vector
        Vector pullVec = impactVec.subtract(playerVec);
        
        // Apply multipliers: 0.3x for all axes + a 0.4 vertical 'bump'
        double multiplier = 0.3;
        double verticalBoost = 0.4;
        
        Vector velocity = pullVec.multiply(multiplier);
        velocity.setY(velocity.getY() + verticalBoost);
        
        // Safety cap to prevent extreme velocities (Hypixel also has limits)
        double maxVel = 2.5;
        if (velocity.length() > maxVel) {
            velocity = velocity.normalize().multiply(maxVel);
        }
        
        // Apply velocity
        player.setVelocity(velocity);
        
        // Negate fall damage
        player.setFallDistance(0);
        
        // Play pull and impact sounds
        if (soundEffects) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.8f, 0.8f);
            player.playSound(impactLocation, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.5f);
        }
        
        // Particle effects at impact
        if (particleEffects) {
            impactLocation.getWorld().spawnParticle(Particle.CRIT, impactLocation, 15, 0.3, 0.3, 0.3, 0.1);
            impactLocation.getWorld().spawnParticle(Particle.CLOUD, impactLocation, 10, 0.3, 0.3, 0.3, 0.05);
        }
        
        // Remove hook
        cleanup(player);
    }

    /**
     * Cancel an active hook.
     */
    public void cancelHook(Player player) {
        ActiveHook active = activeHooks.get(player.getUniqueId());
        if (active != null) {
            if (soundEffects) {
                player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0f, 1.0f);
            }
            if (particleEffects) {
                player.getWorld().spawnParticle(Particle.CLOUD, active.hook().getLocation(), 5, 0.1, 0.1, 0.1, 0.02);
            }
            active.hook().remove();
            cleanup(player);
        }
    }

    public boolean isOnCooldown(Player player) {
        Long time = cooldowns.get(player.getUniqueId());
        return time != null && time > System.currentTimeMillis();
    }

    public String getCooldownRemaining(Player player) {
        Long time = cooldowns.get(player.getUniqueId());
        if (time == null) return "0.0";
        double remaining = (time - System.currentTimeMillis()) / 1000.0;
        return String.format("%.1f", Math.max(0, remaining));
    }

    public void cleanup(Player player) {
        ActiveHook active = activeHooks.remove(player.getUniqueId());
        if (active != null && !active.hook().isDead()) {
            active.hook().remove();
        }
    }

    public boolean hasActiveHook(Player player) {
        return activeHooks.containsKey(player.getUniqueId());
    }

    public ItemStack createHookItem(GrapplingHookType hookType) {
        ItemStack item = new ItemStack(Material.FISHING_ROD);
        var meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(hookType.getDisplayName());
        
        var lore = new java.util.ArrayList<String>();
        for (String line : hookType.getLore()) {
            lore.add(line);
        }
        
        meta.setLore(lore);
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS);
        
        if (hookType.getCustomModelData() > 0) {
            meta.setCustomModelData(hookType.getCustomModelData());
        }

        // Store hook ID in PDC for reliable detection
        meta.getPersistentDataContainer().set(plugin.getGrapplingHookKey(), PersistentDataType.STRING, hookType.getId());
        
        item.setItemMeta(meta);
        return item;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public record ActiveHook(FishingHook hook, GrapplingHookType hookType, long launchTime) {}
}
