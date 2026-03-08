package io.papermc.Grivience.listener;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Simple listener that normalizes player attack speed to feel snappier on join.
 */
public final class AttackSpeedListener implements Listener {
    private final GriviencePlugin plugin;
    private final double attackSpeed;

    public AttackSpeedListener(GriviencePlugin plugin) {
        this(plugin, 32.0D); // higher default for faster combat pacing
    }

    public AttackSpeedListener(GriviencePlugin plugin, double attackSpeed) {
        this.plugin = plugin;
        this.attackSpeed = attackSpeed;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Attribute attribute = resolveAttackSpeedAttribute();
        if (attribute == null) return;

        AttributeInstance attr = player.getAttribute(attribute);
        if (attr != null && attr.getBaseValue() < attackSpeed) {
            attr.setBaseValue(attackSpeed);
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
