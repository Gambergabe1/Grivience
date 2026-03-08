package io.papermc.Grivience.listener;

import io.papermc.Grivience.collections.PlayerCollectionProgress;
import io.papermc.Grivience.crafting.RecipeRegistry;
import io.papermc.Grivience.crafting.SkyblockRecipe;
import io.papermc.Grivience.collections.CollectionsManager;
import io.papermc.Grivience.stats.SkyblockLevelManager;

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
    private final CollectionsManager collectionsManager;
    private final SkyblockLevelManager skyblockLevelManager;


    public RecipeUnlockListener(JavaPlugin plugin, CollectionsManager collectionsManager, SkyblockLevelManager skyblockLevelManager) {
        this.plugin = plugin;
        this.unlockedKey = new NamespacedKey(plugin, "vanilla_recipes_unlocked");
        this.collectionsManager = collectionsManager;
        this.skyblockLevelManager = skyblockLevelManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        var data = player.getPersistentDataContainer();
        boolean fullVanillaUnlockDone = data.getOrDefault(unlockedKey, PersistentDataType.INTEGER, 0) == 1;

        int vanillaUnlockedCount = 0;
        for (var it = Bukkit.recipeIterator(); it.hasNext(); ) {
            Recipe recipe = it.next();
            if (!(recipe instanceof Keyed keyed)) {
                continue;
            }
            NamespacedKey key = keyed.getKey();
            if (!key.getNamespace().equals(NamespacedKey.MINECRAFT)) {
                continue;
            }
            
            // If we already did a full unlock, we only need to check recipes the player doesn't have yet
            // (e.g. if new recipes were added to Minecraft or the plugin)
            if (!player.hasDiscoveredRecipe(key)) {
                player.discoverRecipe(key);
                vanillaUnlockedCount++;
            }
        }

        if (!fullVanillaUnlockDone) {
            data.set(unlockedKey, PersistentDataType.INTEGER, 1);
        }
        
        if (vanillaUnlockedCount > 0 && !fullVanillaUnlockDone) {
            player.sendMessage(ChatColor.GRAY + "Unlocked " + ChatColor.AQUA + vanillaUnlockedCount + ChatColor.GRAY + " vanilla recipes.");
        }

        // --- Custom Recipe Unlocking ---
        int customUnlockedCount = 0;

        for (SkyblockRecipe customRecipe : RecipeRegistry.getAll()) {
            if (player.hasDiscoveredRecipe(customRecipe.getKey())) {
                continue; // Already discovered
            }

            boolean meetsCollectionRequirement = true;
            if (customRecipe.getCollectionId() != null && customRecipe.getCollectionTierRequired() > 0) {
                // Get player's progress for this specific collection
                PlayerCollectionProgress playerCollectionProgress = collectionsManager.getPlayerProgress(player, customRecipe.getCollectionId());

                if (playerCollectionProgress == null || !playerCollectionProgress.isTierUnlocked(customRecipe.getCollectionTierRequired())) {
                    meetsCollectionRequirement = false;
                }
            }

            if (meetsCollectionRequirement) {
                player.discoverRecipe(customRecipe.getKey());
                customUnlockedCount++;
            }
        }

        if (customUnlockedCount > 0) {
            player.sendMessage(ChatColor.GOLD + "Unlocked " + ChatColor.YELLOW + customUnlockedCount + ChatColor.GOLD + " new custom recipes!");
        }
    }
}
