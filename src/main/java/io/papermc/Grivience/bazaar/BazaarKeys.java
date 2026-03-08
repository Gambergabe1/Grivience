package io.papermc.Grivience.bazaar;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/**
 * Namespaced keys for Bazaar persistent data.
 */
public final class BazaarKeys {
    public static final NamespacedKey ACTION_KEY;
    public static final NamespacedKey VALUE_KEY;

    private BazaarKeys() {}

    static {
        Plugin plugin = io.papermc.Grivience.GriviencePlugin.getPlugin(io.papermc.Grivience.GriviencePlugin.class);
        ACTION_KEY = plugin != null ? new NamespacedKey(plugin, "bazaar_action") : new NamespacedKey("grivience", "bazaar_action");
        VALUE_KEY = plugin != null ? new NamespacedKey(plugin, "bazaar_value") : new NamespacedKey("grivience", "bazaar_value");
    }
}
