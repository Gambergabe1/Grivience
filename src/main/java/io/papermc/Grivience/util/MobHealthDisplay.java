package io.papermc.Grivience.util;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.stats.SkyblockCombatEngine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;

import java.text.DecimalFormat;

/**
 * Utility to show live health values above mobs.
 */
public final class MobHealthDisplay {
    private static final DecimalFormat HEALTH_FORMAT = new DecimalFormat("#,##0");

    private MobHealthDisplay() {
    }

    public static void setBaseName(LivingEntity living, NamespacedKey baseKey, String baseName) {
        if (living == null || baseKey == null) return;
        living.getPersistentDataContainer().set(baseKey, PersistentDataType.STRING, baseName);
        update(living, baseKey);
    }

    public static void update(LivingEntity living, NamespacedKey baseKey) {
        if (living == null || baseKey == null) return;

        String baseName = living.getPersistentDataContainer().get(baseKey, PersistentDataType.STRING);
        if (baseName == null || baseName.isBlank()) {
            Component custom = living.customName();
            baseName = custom != null
                    ? LegacyComponentSerializer.legacySection().serialize(custom)
                    : living.getName();
        }

        AttributeInstance maxHealthAttr = living.getAttribute(Attribute.MAX_HEALTH);
        double mcMax = maxHealthAttr != null ? Math.max(1.0D, maxHealthAttr.getValue()) : 20.0D;
        double mcCurrent = Math.max(0.0D, Math.min(living.getHealth(), mcMax));

        double scale = 5.0; // Default fallback
        try {
            GriviencePlugin plugin = GriviencePlugin.getPlugin(GriviencePlugin.class);
            SkyblockCombatEngine engine = plugin.getSkyblockCombatEngine();
            if (engine != null) {
                scale = engine.getHealthScale();
            }
        } catch (Exception ignored) {}

        double maxHealth = mcMax * scale;
        double currentHealth = mcCurrent * scale;

        // Attempt to find level from metadata or default to 1
        int level = 1;
        NamespacedKey monsterLevelKey = new NamespacedKey("grivience", "monster_level");
        if (living.getPersistentDataContainer().has(monsterLevelKey, PersistentDataType.INTEGER)) {
            Integer lvl = living.getPersistentDataContainer().get(monsterLevelKey, PersistentDataType.INTEGER);
            if (lvl != null) level = lvl;
        }

        String healthColor = ChatColor.RED.toString();
        String healthText = ChatColor.GRAY + "[" + ChatColor.DARK_GRAY + "Lv" + level + ChatColor.GRAY + "] " + baseName + " " + healthColor + HEALTH_FORMAT.format(currentHealth) + ChatColor.GRAY + "/" + healthColor + HEALTH_FORMAT.format(maxHealth) + ChatColor.RED + "❤";
        Component healthComponent = LegacyComponentSerializer.legacySection().deserialize(healthText);
        living.customName(healthComponent);
        living.setCustomNameVisible(true);
    }
}
