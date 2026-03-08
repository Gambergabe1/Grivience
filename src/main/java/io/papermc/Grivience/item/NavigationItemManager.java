package io.papermc.Grivience.item;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.gui.SkyblockMenuManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public final class NavigationItemManager implements Listener {
    private static final int NAVIGATION_SLOT = 8; // Slot 9 (0-indexed)
    private static final String NAVIGATION_ITEM_NAME = ChatColor.GREEN + "Skyblock Menu " + ChatColor.YELLOW + "(Click)";
    private static final List<String> NAVIGATION_ITEM_LORE = List.of(
            ChatColor.GRAY + "View all of your",
            ChatColor.GRAY + "Skyblock menus",
            "",
            ChatColor.YELLOW + "Right-Click to Open"
    );

    private final GriviencePlugin plugin;
    private final SkyblockMenuManager skyblockMenuManager;
    private final NamespacedKey navigationKey;

    public NavigationItemManager(GriviencePlugin plugin, SkyblockMenuManager skyblockMenuManager) {
        this.plugin = plugin;
        this.skyblockMenuManager = skyblockMenuManager;
        this.navigationKey = new NamespacedKey(plugin, "navigation_item");
        
        startEnforcementTask();
    }

    private void startEnforcementTask() {
        // Periodically ensure the item is in the correct slot for all players
        // This handles cases like /clear or weird inventory desyncs
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.isValid()) {
                        ensureNavigationPresent(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Check every second
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        giveNavigationItem(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(org.bukkit.event.player.PlayerRespawnEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> giveNavigationItem(event.getPlayer()), 1L);
    }

    public void giveNavigationItem(Player player) {
        if (!plugin.getConfig().getBoolean("navigation-item.enabled", true)) {
            return;
        }

        ItemStack navigationItem = createNavigationItem();
        player.getInventory().setItem(NAVIGATION_SLOT, navigationItem);
    }

    public ItemStack createNavigationItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(NAVIGATION_ITEM_NAME);
        meta.setLore(NAVIGATION_ITEM_LORE);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(navigationKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!plugin.getConfig().getBoolean("navigation-item.enabled", true)) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta.getPersistentDataContainer().has(navigationKey, PersistentDataType.BYTE)) {
            event.setCancelled(true);
            openNavigationMenu(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (isNavigationItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot drop your Skyblock menu.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        
        // Handle number key swapping
        boolean hotbarSwap = false;
        if (event.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY) {
            int hotbarSlot = event.getHotbarButton();
            if (hotbarSlot != -1) {
                ItemStack hotbarItem = player.getInventory().getItem(hotbarSlot);
                if (isNavigationItem(hotbarItem)) {
                    hotbarSwap = true;
                }
            }
        }

        boolean movingNav = isNavigationItem(current) || isNavigationItem(cursor) || hotbarSwap;
        if (!movingNav) {
            return;
        }

        // Prevent moving the item into any container; keep it only in the hotbar slot.
        InventoryType topType = event.getView().getTopInventory().getType();
        if (topType != InventoryType.CRAFTING && topType != InventoryType.CREATIVE) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "The Skyblock menu cannot be moved into containers.");
            return;
        }

        // If moving within player inventory, enforce the designated slot.
        // We only enforce if the navigation item is NOT in the target slot
        int rawSlot = event.getRawSlot();
        if (rawSlot != NAVIGATION_SLOT && movingNav) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Keep the Skyblock menu in slot " + (NAVIGATION_SLOT + 1) + ".");
            // Re-sync to be safe
            plugin.getServer().getScheduler().runTask(plugin, () -> ensureNavigationPresent(player));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (isNavigationItem(event.getOldCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommandPreprocess(org.bukkit.event.player.PlayerCommandPreprocessEvent event) {
        String message = event.getMessage().toLowerCase();
        if (message.startsWith("/clear") || message.startsWith("/minecraft:clear")) {
            Player player = event.getPlayer();
            ItemStack navItem = player.getInventory().getItem(NAVIGATION_SLOT);
            
            if (isNavigationItem(navItem)) {
                // Clone the item to preserve it
                ItemStack preserved = navItem.clone();
                
                // Re-give the item after the command executes
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        player.getInventory().setItem(NAVIGATION_SLOT, preserved);
                    }
                });
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreativeClick(org.bukkit.event.inventory.InventoryCreativeEvent event) {
        if (isNavigationItem(event.getCurrentItem()) || isNavigationItem(event.getCursor())) {
            event.setCancelled(true);
        }
    }

    private void ensureNavigationPresent(Player player) {
        if (!plugin.getConfig().getBoolean("navigation-item.enabled", true)) {
            return;
        }
        
        // Check slot 9
        ItemStack current = player.getInventory().getItem(NAVIGATION_SLOT);
        if (!isNavigationItem(current)) {
            // Remove duplicates first
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                if (i == NAVIGATION_SLOT) continue;
                ItemStack item = player.getInventory().getItem(i);
                if (isNavigationItem(item)) {
                    player.getInventory().setItem(i, null);
                }
            }
            // Give it back in the right slot
            giveNavigationItem(player);
        }
    }

    private void openNavigationMenu(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
        skyblockMenuManager.openMainMenu(player);
    }

    public boolean isNavigationItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(navigationKey, PersistentDataType.BYTE);
    }
}
