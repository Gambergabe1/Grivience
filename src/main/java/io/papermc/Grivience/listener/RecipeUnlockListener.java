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
import java.util.Iterator;
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
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        // The recipe book can still be initializing during PlayerJoinEvent. Defer sync by one tick so
        // essential vanilla recipes reliably stick for fresh joins when limited crafting is enabled.
        Bukkit.getScheduler().runTask(plugin, () -> syncRecipeBook(player));
    }

    @EventHandler(ignoreCancelled = true)
    public void onRecipeDiscover(PlayerRecipeDiscoverEvent event) {
        NamespacedKey key = event.getRecipe();
        if (isVanillaRecipeKey(key) && !isEssentialVanillaRecipe(key)) {
            event.setCancelled(true);
        }
    }

    public void syncRecipeBook(Player player) {
        if (player == null) {
            return;
        }

        clearVanillaRecipes(player);
        discoverEssentialVanillaRecipes(player);
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
            if (isVanillaRecipeKey(key) && !isEssentialVanillaRecipe(key)) {
                vanillaRecipes.add(key);
            }
        }

        if (!vanillaRecipes.isEmpty()) {
            player.undiscoverRecipes(vanillaRecipes);
        }
    }

    private void discoverEssentialVanillaRecipes(Player player) {
        List<NamespacedKey> keys = new ArrayList<>();
        for (Iterator<Recipe> it = Bukkit.recipeIterator(); it.hasNext(); ) {
            NamespacedKey key = recipeKey(it.next());
            if (isEssentialVanillaRecipe(key)) {
                keys.add(key);
            }
        }
        if (!keys.isEmpty()) {
            player.discoverRecipes(keys);
        }
    }

    static boolean isEssentialVanillaRecipe(NamespacedKey key) {
        if (key == null || !NamespacedKey.MINECRAFT.equalsIgnoreCase(key.getNamespace())) {
            return false;
        }
        
        String path = key.getKey().toLowerCase();
        
        // Essential Building & Utility
        if (path.contains("planks") || path.contains("slab") || path.contains("stairs") || 
            path.contains("fence") || path.contains("door") || path.contains("trapdoor") ||
            path.contains("button") || path.contains("pressure_plate") || path.contains("gate") ||
            path.contains("sign") || path.contains("chest") || path.contains("stick") ||
            path.contains("crafting_table") || path.contains("furnace") || path.contains("torch") ||
            path.contains("ladder") || path.contains("glass_pane") || path.contains("bowl") ||
            path.contains("boat") || path.equals("paper") || path.equals("book") || 
            path.equals("bucket") || path.equals("bread") || path.contains("glass") ||
            path.contains("wool") || path.contains("carpet") || path.contains("terracotta") ||
            path.contains("concrete") || path.contains("stone_brick") || path.contains("polished")) {
            return true;
        }
        
        // Basic Tools (Wood/Stone)
        if (path.startsWith("wooden_") || path.startsWith("stone_")) {
            return true;
        }
        
        return false;
    }

    private boolean isVanillaRecipeKey(NamespacedKey key) {
        return key != null && NamespacedKey.MINECRAFT.equalsIgnoreCase(key.getNamespace());
    }

    static NamespacedKey recipeKey(Recipe recipe) {
        if (!(recipe instanceof Keyed keyed)) {
            return null;
        }
        return keyed.getKey();
    }

    private boolean meetsCollectionRequirement(Player player, String collectionId, int requiredTier) {
        if (player == null || collectionId == null || requiredTier <= 0) {
            return true;
        }
        PlayerCollectionProgress playerCollectionProgress = collectionsManager.getPlayerProgress(player, collectionId);
        return playerCollectionProgress != null && playerCollectionProgress.isTierUnlocked(requiredTier);
    }
}
