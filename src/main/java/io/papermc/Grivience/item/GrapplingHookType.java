package io.papermc.Grivience.item;

import org.bukkit.ChatColor;

/**
 * Grappling hook types - Ascent Skyblock accurate.
 * Ascent Skyblock has one main grappling hook obtainable from fishing.
 */
public enum GrapplingHookType {
    GRAPPLING_HOOK(
            "grappling_hook",
            "§fGrappling Hook",
            "§7Grants the ability to grapple to blocks.",
            "§7Right-click to launch the hook.",
            "",
            "§7Velocity: §b+20",
            "§7Cooldown: §b2s",
            "",
            "§9§lRARE"
    );

    private final String id;
    private final String displayName;
    private final String[] lore;
    private final double launchVelocity;
    private final double maxDistance;
    private final long cooldownMs;

    GrapplingHookType(String id, String displayName, String... lore) {
        this.id = id;
        this.displayName = displayName;
        this.lore = lore;
        this.launchVelocity = 2.0; // Ascent Skyblock uses velocity 2.0
        this.maxDistance = 50.0;   // Ascent Skyblock max range ~50 blocks
        this.cooldownMs = 2000;    // 2 second cooldown
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String[] getLore() {
        return lore;
    }

    public double getLaunchVelocity() {
        return launchVelocity;
    }

    public double getMaxDistance() {
        return maxDistance;
    }

    public long getCooldownMs() {
        return cooldownMs;
    }

    public static GrapplingHookType parse(String id) {
        if (id == null) return null;
        for (GrapplingHookType type : values()) {
            if (type.getId().equalsIgnoreCase(id) || type.name().equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }

    public static GrapplingHookType fromId(String id) {
        return parse(id);
    }
}
