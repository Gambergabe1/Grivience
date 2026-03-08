package io.papermc.Grivience.stats;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Forces combat stats refresh on equipment changes so held weapon stats apply immediately.
 *
 * SkyblockCombatEngine refreshes on a 1s ticker for performance. This listener queues a next-tick
 * refresh when the player changes hotbar slot or modifies inventory, then clamps mana to avoid
 * "over-max mana" exploits after swapping off Intelligence weapons.
 */
public final class SkyblockCombatRefreshListener implements Listener {
    private final JavaPlugin plugin;
    private final SkyblockCombatEngine combatEngine;
    private final SkyblockManaManager manaManager;

    private final Set<UUID> pending = ConcurrentHashMap.newKeySet();

    public SkyblockCombatRefreshListener(JavaPlugin plugin, SkyblockCombatEngine combatEngine, SkyblockManaManager manaManager) {
        this.plugin = plugin;
        this.combatEngine = combatEngine;
        this.manaManager = manaManager;
    }

    private void queueRefresh(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        if (!pending.add(playerId)) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            pending.remove(playerId);
            if (!player.isOnline()) {
                return;
            }

            if (combatEngine != null) {
                combatEngine.refreshNow(player);
            }
            if (manaManager != null) {
                manaManager.getMana(player);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeldItemChange(PlayerItemHeldEvent event) {
        queueRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        queueRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            queueRefresh(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            queueRefresh(player);
        }
    }
}

