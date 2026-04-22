package io.papermc.Grivience.util;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;

public final class RegenPlaceholderUtil {
    private RegenPlaceholderUtil() {
    }

    public static Material resolvePlaceholder(FileConfiguration config, String path, String fallbackPath) {
        if (config == null) {
            return Material.BEDROCK;
        }
        String configured = null;
        if (path != null && !path.isBlank()) {
            configured = config.getString(path);
        }
        if ((configured == null || configured.isBlank()) && fallbackPath != null && !fallbackPath.isBlank()) {
            configured = config.getString(fallbackPath);
        }
        if (configured == null || configured.isBlank()) {
            return Material.BEDROCK;
        }

        Material material = Material.matchMaterial(configured.trim());
        if (material == null || !material.isBlock()) {
            return Material.BEDROCK;
        }
        return material;
    }

    public static boolean canRestore(Block block, Material placeholder) {
        if (block == null) {
            return false;
        }
        Material current = block.getType();
        if (current.isAir()) {
            return true;
        }
        return placeholder != null && !placeholder.isAir() && current == placeholder;
    }

    public static void placePlaceholder(Block block, Material placeholder) {
        if (block == null || placeholder == null || placeholder.isAir()) {
            return;
        }
        if (!block.getType().isAir()) {
            return;
        }
        block.setType(placeholder, false);
    }
}
