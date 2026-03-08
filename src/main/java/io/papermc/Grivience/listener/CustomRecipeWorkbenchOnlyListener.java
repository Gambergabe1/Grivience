package io.papermc.Grivience.listener;

import org.bukkit.ChatColor;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ensures all custom (plugin-namespace) crafting recipes are crafted using the vanilla crafting table UI.
 *
 * This blocks crafting in the 2x2 player-inventory crafting grid for any recipe whose NamespacedKey
 * namespace matches the plugin name.
 */
public final class CustomRecipeWorkbenchOnlyListener implements Listener {
    private static final long WARN_THROTTLE_MS = 1500L;

    private final String pluginNamespace;
    private final Map<UUID, Long> lastWarnAtMs = new ConcurrentHashMap<>();

    public CustomRecipeWorkbenchOnlyListener(JavaPlugin plugin) {
        this.pluginNamespace = plugin == null
                ? ""
                : plugin.getName().toLowerCase(Locale.ROOT);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (event.getPlayer() != null) {
            lastWarnAtMs.remove(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepare(PrepareItemCraftEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) {
            return;
        }
        if (event.getInventory().getType() == InventoryType.WORKBENCH) {
            return;
        }

        if (!isCustomPluginRecipe(event.getRecipe())) {
            return;
        }

        event.getInventory().setResult(null);
        warn(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getInventory().getType() == InventoryType.WORKBENCH) {
            return;
        }

        if (!isCustomPluginRecipe(event.getRecipe())) {
            return;
        }

        event.setCancelled(true);
        warn(player);
    }

    private boolean isCustomPluginRecipe(Recipe recipe) {
        if (recipe == null) {
            return false;
        }
        if (pluginNamespace.isBlank()) {
            return false;
        }
        if (!(recipe instanceof Keyed keyed)) {
            return false;
        }
        NamespacedKey key = keyed.getKey();
        if (key == null) {
            return false;
        }
        return pluginNamespace.equalsIgnoreCase(key.getNamespace());
    }

    private void warn(Player player) {
        if (player == null || player.getUniqueId() == null) {
            return;
        }

        long now = System.currentTimeMillis();
        Long last = lastWarnAtMs.get(player.getUniqueId());
        if (last != null && (now - last) < WARN_THROTTLE_MS) {
            return;
        }
        lastWarnAtMs.put(player.getUniqueId(), now);

        player.sendMessage(ChatColor.RED + "This recipe must be crafted in a crafting table.");
    }
}

