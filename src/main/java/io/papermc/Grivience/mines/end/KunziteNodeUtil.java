package io.papermc.Grivience.mines.end;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class KunziteNodeUtil {
    private static final Set<Material> DEFAULT_KUNZITE_BLOCKS = EnumSet.of(
            Material.PINK_STAINED_GLASS,
            Material.PINK_STAINED_GLASS_PANE,
            Material.PINK_CONCRETE,
            Material.PINK_WOOL
    );

    private KunziteNodeUtil() {
    }

    public static boolean isConfiguredKunziteNode(FileConfiguration config, Block block) {
        if (!isEnabled(config) || block == null || block.getWorld() == null) {
            return false;
        }
        if (!block.getWorld().getName().equalsIgnoreCase(getWorldName(config))) {
            return false;
        }
        return getNodeMaterials(config).contains(block.getType());
    }

    public static boolean isEnabled(FileConfiguration config) {
        return config != null && config.getBoolean("end-hub.kunzite.enabled", true);
    }

    public static String getWorldName(FileConfiguration config) {
        if (config == null) {
            return "world_the_end";
        }
        String configured = config.getString("end-hub.kunzite.world-name");
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        return config.getString("skyblock.portal-routing.end.world-name", "world_the_end");
    }

    public static Set<Material> getNodeMaterials(FileConfiguration config) {
        if (config == null) {
            return DEFAULT_KUNZITE_BLOCKS;
        }

        List<String> configured = config.getStringList("end-hub.kunzite.blocks");
        if (configured == null || configured.isEmpty()) {
            return DEFAULT_KUNZITE_BLOCKS;
        }

        EnumSet<Material> parsed = EnumSet.noneOf(Material.class);
        for (String entry : configured) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            Material material = Material.matchMaterial(entry.trim().toUpperCase(Locale.ROOT));
            if (material != null) {
                parsed.add(material);
            }
        }
        return parsed.isEmpty() ? DEFAULT_KUNZITE_BLOCKS : parsed;
    }
}
