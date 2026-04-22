package io.papermc.Grivience.listener;

import io.papermc.Grivience.collections.PlayerCollectionProgress;
import io.papermc.Grivience.crafting.RecipeRegistry;
import io.papermc.Grivience.crafting.SkyblockRecipe;
import io.papermc.Grivience.item.ArmorCraftingListener;
import io.papermc.Grivience.collections.CollectionsManager;
import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.stats.SkyblockLevelManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRecipeDiscoverEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.Keyed;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps the vanilla recipe book from filling with minecraft recipes while still
 * discovering the plugin's custom Skyblock recipes.
 */
public final class RecipeUnlockListener implements Listener {
    private final JavaPlugin plugin;
    private final CollectionsManager collectionsManager;
    private final SkyblockLevelManager skyblockLevelManager;


    public RecipeUnlockListener(JavaPlugin plugin, CollectionsManager collectionsManager, SkyblockLevelManager skyblockLevelManager) {
        this.plugin = plugin;
        this.collectionsManager = collectionsManager;
        this.skyblockLevelManager = skyblockLevelManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        syncRecipeBook(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onRecipeDiscover(PlayerRecipeDiscoverEvent event) {
        NamespacedKey key = event.getRecipe();
        if (isVanillaRecipeKey(key)) {
            event.setCancelled(true);
        }
    }

    public void syncRecipeBook(Player player) {
        if (player == null) {
            return;
        }

        clearVanillaRecipes(player);
        int customUnlockedCount = 0;

        for (SkyblockRecipe customRecipe : RecipeRegistry.getAll()) {
            if (player.hasDiscoveredRecipe(customRecipe.getKey())) {
                continue; // Already discovered
            }

            if (meetsCollectionRequirement(player, customRecipe.getCollectionId(), customRecipe.getCollectionTierRequired())) {
                player.discoverRecipe(customRecipe.getKey());
                customUnlockedCount++;
            }
        }

        if (plugin instanceof GriviencePlugin grivience && grivience.getArmorCraftingListener() != null) {
            for (ArmorCraftingListener.ArmorRecipeDefinition recipeDefinition : grivience.getArmorCraftingListener().getRegisteredRecipeDefinitions()) {
                if (recipeDefinition == null || recipeDefinition.key() == null || player.hasDiscoveredRecipe(recipeDefinition.key())) {
                    continue;
                }
                if (!meetsCollectionRequirement(player, recipeDefinition.collectionId(), recipeDefinition.collectionTierRequired())) {
                    continue;
                }
                player.discoverRecipe(recipeDefinition.key());
                customUnlockedCount++;
            }
        }

        if (customUnlockedCount > 0) {
            player.sendMessage(ChatColor.GOLD + "Unlocked " + ChatColor.YELLOW + customUnlockedCount + ChatColor.GOLD + " new custom recipes!");
        }
    }

    private void clearVanillaRecipes(Player player) {
        if (player == null) {
            return;
        }

        List<NamespacedKey> vanillaRecipes = new ArrayList<>();
        for (NamespacedKey key : player.getDiscoveredRecipes()) {
            if (isVanillaRecipeKey(key)) {
                vanillaRecipes.add(key);
            }
        }

        if (!vanillaRecipes.isEmpty()) {
            player.undiscoverRecipes(vanillaRecipes);
        }
    }

    private boolean isVanillaRecipeKey(NamespacedKey key) {
        return key != null && NamespacedKey.MINECRAFT.equalsIgnoreCase(key.getNamespace());
    }

    private boolean meetsCollectionRequirement(Player player, String collectionId, int requiredTier) {
        if (player == null || collectionId == null || requiredTier <= 0) {
            return true;
        }
        PlayerCollectionProgress playerCollectionProgress = collectionsManager.getPlayerProgress(player, collectionId);
        return playerCollectionProgress != null && playerCollectionProgress.isTierUnlocked(requiredTier);
    }
}
