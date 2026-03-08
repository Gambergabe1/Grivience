package io.papermc.Grivience.listener;

import io.papermc.Grivience.collections.CollectionTier;
import io.papermc.Grivience.collections.CollectionsManager;
import io.papermc.Grivience.collections.PlayerCollectionProgress;
import io.papermc.Grivience.crafting.RecipeRegistry;
import io.papermc.Grivience.crafting.SkyblockRecipe;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Recipe;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enforces collection-tier requirements for RecipeRegistry recipes when using the vanilla crafting UI.
 *
 * Relying purely on recipe discovery is not sufficient unless the server enables the {@code doLimitedCrafting}
 * gamerule, so we gate crafting via events.
 */
public final class RecipeCollectionGateListener implements Listener {
    private static final long WARN_THROTTLE_MS = 1500L;

    private final CollectionsManager collectionsManager;
    private final Map<UUID, Long> lastWarnAtMs = new ConcurrentHashMap<>();

    public RecipeCollectionGateListener(CollectionsManager collectionsManager) {
        this.collectionsManager = collectionsManager;
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

        SkyblockRecipe skyblockRecipe = lookupSkyblockRecipe(event.getRecipe());
        if (skyblockRecipe == null) {
            return;
        }

        if (!meetsCollectionRequirement(player, skyblockRecipe)) {
            // Hide result so the craft looks locked in the vanilla UI.
            event.getInventory().setResult(null);
            warnLocked(player, skyblockRecipe);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        SkyblockRecipe skyblockRecipe = lookupSkyblockRecipe(event.getRecipe());
        if (skyblockRecipe == null) {
            return;
        }

        if (!meetsCollectionRequirement(player, skyblockRecipe)) {
            event.setCancelled(true);
            warnLocked(player, skyblockRecipe);
        }
    }

    private SkyblockRecipe lookupSkyblockRecipe(Recipe recipe) {
        if (!(recipe instanceof Keyed keyed)) {
            return null;
        }

        NamespacedKey key = keyed.getKey();
        if (key == null) {
            return null;
        }

        return RecipeRegistry.getByKey(key).orElse(null);
    }

    private boolean meetsCollectionRequirement(Player player, SkyblockRecipe recipe) {
        if (player == null || recipe == null) {
            return true;
        }

        // Keep staff/admin/testing flows unblocked.
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return true;
        }

        if (collectionsManager == null || !collectionsManager.isEnabled()) {
            return true;
        }

        String collectionId = recipe.getCollectionId();
        int requiredTier = recipe.getCollectionTierRequired();
        if (collectionId == null || collectionId.isBlank() || requiredTier <= 0) {
            return true;
        }

        PlayerCollectionProgress progress = collectionsManager.getPlayerProgress(player, collectionId);
        return progress != null && progress.isTierUnlocked(requiredTier);
    }

    private void warnLocked(Player player, SkyblockRecipe recipe) {
        if (player == null || recipe == null) {
            return;
        }

        String collectionId = recipe.getCollectionId();
        int requiredTier = recipe.getCollectionTierRequired();
        if (collectionId == null || collectionId.isBlank() || requiredTier <= 0) {
            return;
        }

        long now = System.currentTimeMillis();
        Long last = lastWarnAtMs.get(player.getUniqueId());
        if (last != null && (now - last) < WARN_THROTTLE_MS) {
            return;
        }
        lastWarnAtMs.put(player.getUniqueId(), now);

        String tierLabel = CollectionTier.toRoman(requiredTier);
        player.sendMessage(ChatColor.RED + "Recipe locked. " + ChatColor.GRAY + "Requires " +
                ChatColor.YELLOW + collectionId + ChatColor.GRAY + " Collection Tier " + ChatColor.AQUA + tierLabel + ChatColor.GRAY + ".");
    }
}

