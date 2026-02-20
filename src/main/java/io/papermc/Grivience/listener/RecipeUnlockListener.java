package io.papermc.Grivience.listener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Recipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.Keyed;

/**
 * Ensures players can see vanilla recipes after the plugin registers custom ones.
 * Unlocks all recipes in the minecraft namespace once per player to avoid the recipe book appearing empty.
 */
public final class RecipeUnlockListener implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey unlockedKey;

    public RecipeUnlockListener(JavaPlugin plugin) {
        this.plugin = plugin;
        this.unlockedKey = new NamespacedKey(plugin, "vanilla_recipes_unlocked");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        var data = player.getPersistentDataContainer();
        if (data.getOrDefault(unlockedKey, PersistentDataType.INTEGER, 0) == 1) {
            return; // already unlocked
        }

        int unlocked = 0;
        for (var it = Bukkit.recipeIterator(); it.hasNext(); ) {
            Recipe recipe = it.next();
            if (!(recipe instanceof Keyed keyed)) {
                continue;
            }
            NamespacedKey key = keyed.getKey();
            if (!key.getNamespace().equals(NamespacedKey.MINECRAFT)) {
                continue;
            }
            if (player.hasDiscoveredRecipe(key)) {
                continue;
            }
            player.discoverRecipe(key);
            unlocked++;
        }

        data.set(unlockedKey, PersistentDataType.INTEGER, 1);
        if (unlocked > 0) {
            player.sendMessage(ChatColor.GRAY + "Unlocked " + ChatColor.AQUA + unlocked + ChatColor.GRAY + " vanilla recipes.");
        }
    }
}
