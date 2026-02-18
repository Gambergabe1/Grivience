package io.papermc.Grivience.listener;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.dungeon.DungeonManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class DungeonListener implements Listener {
    private static final String KEY_NAME_TOKEN = "Temple Key";

    private final DungeonManager dungeonManager;
    private final NamespacedKey runKeyTag;

    public DungeonListener(GriviencePlugin plugin, DungeonManager dungeonManager) {
        this.dungeonManager = dungeonManager;
        this.runKeyTag = new NamespacedKey(plugin, "run-key");
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        dungeonManager.handleMobDeath(event);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!dungeonManager.isInDungeon(player.getUniqueId())) {
            return;
        }

        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setKeepLevel(true);
        dungeonManager.handlePlayerDeath(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Location respawn = dungeonManager.getRespawnLocation(event.getPlayer().getUniqueId());
        if (respawn != null) {
            event.setRespawnLocation(respawn);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        dungeonManager.handlePlayerQuit(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        dungeonManager.handlePlayerJoin(player);
        if (!dungeonManager.isInDungeon(player.getUniqueId()) && removeTempleKeys(player)) {
            player.sendMessage(org.bukkit.ChatColor.RED + "Temple Keys were removed because you are not in an active dungeon.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }
        dungeonManager.handlePlayerInteract(event.getPlayer(), event.getAction(), event.getClickedBlock());
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        dungeonManager.handlePlayerMove(event.getPlayer(), from, to);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (dungeonManager.isInDungeon(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(org.bukkit.ChatColor.RED + "Blocks cannot be placed during a dungeon run.");
            return;
        }
        ItemStack item = event.getItemInHand();
        if (!isTempleKey(item)) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage(org.bukkit.ChatColor.RED + "Temple Keys cannot be placed.");
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!dungeonManager.isInDungeon(event.getPlayer().getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage(org.bukkit.ChatColor.RED + "Blocks cannot be broken during a dungeon run.");
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (!isTempleKey(item)) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage(org.bukkit.ChatColor.RED + "Temple Keys cannot be dropped.");
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        boolean keyCurrent = isTempleKey(event.getCurrentItem());
        boolean keyCursor = isTempleKey(event.getCursor());
        boolean keyHotbar = false;
        if (event.getClick() == ClickType.NUMBER_KEY && event.getWhoClicked() instanceof Player player) {
            int hotbar = event.getHotbarButton();
            if (hotbar >= 0) {
                keyHotbar = isTempleKey(player.getInventory().getItem(hotbar));
            }
        }
        if (!keyCurrent && !keyCursor && !keyHotbar) {
            return;
        }

        InventoryType topType = event.getView().getTopInventory().getType();
        if (topType == InventoryType.CRAFTING || topType == InventoryType.CREATIVE) {
            return;
        }

        event.setCancelled(true);
        if (event.getWhoClicked() instanceof Player player) {
            player.sendMessage(org.bukkit.ChatColor.RED + "Temple Keys cannot be moved into containers.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!isTempleKey(event.getOldCursor())) {
            return;
        }
        int topSize = event.getView().getTopInventory().getSize();
        boolean draggingIntoTop = event.getRawSlots().stream().anyMatch(slot -> slot < topSize);
        if (!draggingIntoTop) {
            return;
        }
        InventoryType topType = event.getView().getTopInventory().getType();
        if (topType == InventoryType.CRAFTING || topType == InventoryType.CREATIVE) {
            return;
        }
        event.setCancelled(true);
        if (event.getWhoClicked() instanceof Player player) {
            player.sendMessage(org.bukkit.ChatColor.RED + "Temple Keys cannot be moved into containers.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack item = event.getItem().getItemStack();
        if (!isTempleKey(item)) {
            return;
        }
        if (dungeonManager.isInDungeon(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        event.getItem().remove();
        player.sendMessage(org.bukkit.ChatColor.RED + "Temple Keys crumble outside the dungeon.");
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.getEntityType() == EntityType.CREEPER) {
            event.blockList().clear();
        }
    }

    private boolean isTempleKey(ItemStack item) {
        if (item == null || item.getType() != Material.TRIPWIRE_HOOK || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        String taggedSession = meta.getPersistentDataContainer().get(runKeyTag, PersistentDataType.STRING);
        if (taggedSession != null && !taggedSession.isBlank()) {
            return true;
        }
        String display = meta.hasDisplayName() ? org.bukkit.ChatColor.stripColor(meta.getDisplayName()) : "";
        return display != null && display.contains(KEY_NAME_TOKEN);
    }

    private boolean removeTempleKeys(Player player) {
        boolean removed = removeTempleKeysFromInventory(player.getInventory());
        removed |= removeTempleKeysFromInventory(player.getEnderChest());
        return removed;
    }

    private boolean removeTempleKeysFromInventory(Inventory inventory) {
        boolean removed = false;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (!isTempleKey(item)) {
                continue;
            }
            inventory.setItem(slot, null);
            removed = true;
        }
        return removed;
    }
}
