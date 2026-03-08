package io.papermc.Grivience.util;

import io.papermc.Grivience.GriviencePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.text.DecimalFormat;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility to spawn Hypixel-style damage indicators.
 */
public final class DamageIndicatorUtil {
    private static final DecimalFormat DAMAGE_FORMAT = new DecimalFormat("#,###.#");

    private DamageIndicatorUtil() {
    }

    /**
     * Spawns a damage indicator hologram above the entity.
     *
     * @param plugin      Plugin instance
     * @param entity      Target entity
     * @param damage      Damage amount
     * @param isCritical  Whether the hit was a critical hit
     */
    public static void spawn(GriviencePlugin plugin, Entity entity, double damage, boolean isCritical) {
        if (entity == null || !entity.isValid() || damage <= 0.0D) {
            return;
        }

        Location loc = entity.getLocation().add(
                ThreadLocalRandom.current().nextDouble(-1.0, 1.0),
                entity.getHeight() * 0.75 + ThreadLocalRandom.current().nextDouble(0.1, 0.5),
                ThreadLocalRandom.current().nextDouble(-1.0, 1.0)
        );

        String text = formatDamage(damage, isCritical);
        Component component = LegacyComponentSerializer.legacySection().deserialize(text);

        entity.getWorld().spawn(loc, ArmorStand.class, armorStand -> {
            armorStand.setVisible(false);
            armorStand.setMarker(true);
            armorStand.setGravity(false);
            armorStand.setSmall(true);
            armorStand.setCustomNameVisible(true);
            armorStand.customName(component);
            armorStand.setInvulnerable(true);
            armorStand.setPersistent(false);

            // Remove after 1 second
            Bukkit.getScheduler().runTaskLater(plugin, armorStand::remove, 20L);
        });
    }

    private static String formatDamage(double damage, boolean isCritical) {
        String base = DAMAGE_FORMAT.format(damage);
        if (!isCritical) {
            return ChatColor.GRAY + base;
        }

        // Hypixel critical hit colors: §f✧§e4§67§c4§f✧
        StringBuilder sb = new StringBuilder("§f✧");
        char[] chars = base.toCharArray();
        ChatColor[] colors = {ChatColor.WHITE, ChatColor.YELLOW, ChatColor.GOLD, ChatColor.RED, ChatColor.WHITE};
        
        for (int i = 0; i < chars.length; i++) {
            sb.append(colors[i % colors.length].toString()).append(chars[i]);
        }
        sb.append("§f✧");
        return sb.toString();
    }
}
