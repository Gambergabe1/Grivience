package io.papermc.Grivience.listener;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;

/**
 * Reverts combat mechanics to 1.8-style spammable system.
 * Handles attack speed, sweep attacks, and shield behavior.
 */
public final class AttackSpeedListener implements Listener {
    private static final double DEFAULT_LEGACY_SPEED = 1024.0D;

    private final GriviencePlugin plugin;
    private final Attribute attackSpeedAttribute;
    
    private boolean enabled;
    private boolean disableSweep;
    private boolean disableShields;
    private double targetSpeed;

    public AttackSpeedListener(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.attackSpeedAttribute = resolveAttackSpeedAttribute();
        reloadFromConfig();
        Bukkit.getScheduler().runTask(plugin, this::applyToOnlinePlayers);
    }

    public void reloadFromConfig() {
        this.enabled = plugin.getConfig().getBoolean("legacy-combat.enabled", true);
        this.disableSweep = plugin.getConfig().getBoolean("legacy-combat.disable-sweep-attacks", true);
        this.disableShields = plugin.getConfig().getBoolean("legacy-combat.disable-shields", false);
        this.targetSpeed = plugin.getConfig().getDouble("legacy-combat.legacy-attack-speed", DEFAULT_LEGACY_SPEED);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled) return;
        applyLegacyAttackSpeed(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (!enabled) return;
        Bukkit.getScheduler().runTask(plugin, () -> applyLegacyAttackSpeed(event.getPlayer()));
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (!enabled) return;
        applyLegacyAttackSpeed(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onSweep(EntityDamageByEntityEvent event) {
        if (!enabled || !disableSweep) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onShield(PlayerInteractEvent event) {
        if (!enabled || !disableShields) return;
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.getItem() != null && event.getItem().getType() == org.bukkit.Material.SHIELD) {
                event.setCancelled(true);
            }
        }
    }

    private void applyToOnlinePlayers() {
        if (!enabled) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyLegacyAttackSpeed(player);
        }
    }

    private void applyLegacyAttackSpeed(Player player) {
        if (attackSpeedAttribute == null) return;

        AttributeInstance attribute = player.getAttribute(attackSpeedAttribute);
        if (attribute != null) {
            attribute.setBaseValue(targetSpeed);
        }
    }

    private Attribute resolveAttackSpeedAttribute() {
        for (Attribute attribute : Registry.ATTRIBUTE) {
            String name = Registry.ATTRIBUTE.getKey(attribute).getKey();
            if ("generic_attack_speed".equalsIgnoreCase(name) || "player_attack_speed".equalsIgnoreCase(name)) {
                return attribute;
            }
        }
        plugin.getLogger().warning("Attack speed attribute not found; skipping attack speed adjustment.");
        return null;
    }
}
