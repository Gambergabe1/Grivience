package io.papermc.Grivience.util;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public final class SkyblockDamageScaleUtil {
    public static final double DEFAULT_HEALTH_SCALE = 5.0D;

    private SkyblockDamageScaleUtil() {
    }

    public static void setHealthSafely(LivingEntity entity, double health) {
        if (entity == null || !Double.isFinite(health) || health <= 0.0D) {
            return;
        }
        var maxAttr = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxAttr != null) {
            maxAttr.setBaseValue(health);
            // Re-read max health in case it was capped by the server-side attribute limit (e.g., spigot.yml)
            double effectiveMax = maxAttr.getValue();
            entity.setHealth(Math.max(1.0D, Math.min(health, effectiveMax)));
        } else {
            // Fallback for entities without health attribute if any
            entity.setHealth(Math.max(1.0D, Math.min(health, 20.0D)));
        }
    }

    public static double toMinecraftDamage(double skyblockDamage, double healthScale) {
        if (!Double.isFinite(skyblockDamage) || skyblockDamage <= 0.0D) {
            return 0.0D;
        }
        return skyblockDamage / normalizeHealthScale(healthScale);
    }

    public static double normalizeHealthScale(double healthScale) {
        if (!Double.isFinite(healthScale) || healthScale <= 0.0D) {
            return DEFAULT_HEALTH_SCALE;
        }
        return healthScale;
    }
}
