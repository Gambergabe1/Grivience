package io.papermc.Grivience.fasttravel;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.gui.SkyblockGui;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Fast Travel GUI using Player Heads for destinations.
 * Displays unlocked destinations and allows teleportation.
 */
public final class FastTravelGui implements Listener {
    private static final String TITLE = SkyblockGui.title("Fast Travel");
    private static final int[] DESTINATION_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    private static final int CLOSE_SLOT = 53, INFO_SLOT = 48, REFRESH_SLOT = 45, MAP_SLOT = 49;

    private final FastTravelManager manager;
    private final GriviencePlugin plugin;
    private final Map<UUID, Long> clickCooldowns = new HashMap<>();
    private final NamespacedKey actionKey;
    private final NamespacedKey destinationKey;

    public FastTravelGui(GriviencePlugin plugin, FastTravelManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.actionKey = new NamespacedKey(plugin, "fasttravel-action");
        this.destinationKey = new NamespacedKey(plugin, "fasttravel-destination");
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        FastTravelHolder holder = new FastTravelHolder(page);
        Inventory inv = Bukkit.createInventory(holder, 54, TITLE);
        holder.inventory = inv;
        createLayout(inv, player, page);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 0.5F, 1.2F);
    }

    private void createLayout(Inventory inv, Player player, int page) {
        // Background with purple/gray theme
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) {
                boolean border = i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8;
                inv.setItem(i, createGlass(border ? Material.GRAY_STAINED_GLASS_PANE : Material.BLACK_STAINED_GLASS_PANE));
            }
        }

        // Header - Title with compass
        inv.setItem(4, createTitle(player));
        inv.setItem(2, createStats(player));
        inv.setItem(6, createControls());

        // Destination slots (player heads)
        List<FastTravelManager.FastTravelPoint> points = new ArrayList<>(manager.getAllPoints());
        int start = page * DESTINATION_SLOTS.length;
        int end = Math.min(start + DESTINATION_SLOTS.length, points.size());

        for (int i = 0; i < DESTINATION_SLOTS.length && (start + i) < points.size(); i++) {
            FastTravelManager.FastTravelPoint point = points.get(start + i);
            boolean unlocked = manager.isUnlocked(player, point.key());
            inv.setItem(DESTINATION_SLOTS[i], createDestinationItem(point, unlocked, player));
        }

        // Navigation buttons
        inv.setItem(REFRESH_SLOT, createButton(Material.COMPASS, "&eRefresh", "&7Refresh destinations.", "refresh"));
        inv.setItem(46, createButton(Material.ARROW, page > 0 ? "&aPrevious Page" : "&7Previous Page", page > 0 ? "&7View previous destinations." : "&cYou are on the first page.", page > 0 ? "prev" : null));
        inv.setItem(INFO_SLOT, createButton(Material.BOOK, "&bInformation", "&7Click to view help.", "info"));
        
        int totalPages = (int) Math.ceil(points.size() / (double) DESTINATION_SLOTS.length);
        inv.setItem(MAP_SLOT, createButton(Material.FILLED_MAP, "&7Page " + (page + 1) + "/" + totalPages, "&7" + totalPages + " pages.", null));
        
        inv.setItem(50, createButton(Material.ARROW, page < totalPages - 1 ? "&aNext Page" : "&7Next Page", page < totalPages - 1 ? "&7View more destinations." : "&cYou are on the last page.", page < totalPages - 1 ? "next" : null));
        inv.setItem(CLOSE_SLOT, createButton(Material.BARRIER, "&cClose", "&7Close this menu.", "close"));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof FastTravelHolder holder)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player)) return;

        long now = System.currentTimeMillis();
        if (clickCooldowns.containsKey(player.getUniqueId()) && now - clickCooldowns.get(player.getUniqueId()) < 250) return;
        clickCooldowns.put(player.getUniqueId(), now);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        ItemMeta meta = clicked.getItemMeta();

        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (action != null) {
            handleAction(player, action, holder.page);
            return;
        }

        String destKey = meta.getPersistentDataContainer().get(destinationKey, PersistentDataType.STRING);
        if (destKey != null) {
            handleDestinationClick(player, destKey, e.getClick());
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof FastTravelHolder) {
            e.setCancelled(true);
        }
    }

    private void handleAction(Player player, String action, int page) {
        switch (action) {
            case "close" -> player.closeInventory();
            case "refresh" -> {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.5F);
                manager.load();
                open(player, page);
            }
            case "prev" -> open(player, page - 1);
            case "next" -> open(player, page + 1);
            case "info" -> showInfo(player);
        }
    }

    private void handleDestinationClick(Player player, String destKey, org.bukkit.event.inventory.ClickType click) {
        FastTravelManager.FastTravelPoint point = manager.getPointByName(destKey);
        if (point == null) return;

        if (!manager.isUnlocked(player, destKey)) {
            player.sendMessage(ChatColor.RED + "Locked! You haven't unlocked this destination yet.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.2F);
            return;
        }

        if (click.isRightClick()) {
            // Show destination info
            player.sendMessage("");
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + point.name());
            player.sendMessage(ChatColor.GRAY + point.description());
            player.sendMessage(ChatColor.GRAY + "World: " + ChatColor.AQUA + point.location().getWorld().getName());
            player.sendMessage(ChatColor.GRAY + "Coords: " + ChatColor.YELLOW + String.format("%.0f, %.0f, %.0f", point.location().getX(), point.location().getY(), point.location().getZ()));
            player.sendMessage("");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.5F);
        } else {
            // Teleport
            if (manager.teleport(player, destKey)) {
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8F, 1.0F);
                Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, 0), 20L);
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.2F);
            }
        }
    }

    private void showInfo(Player player) {
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Fast Travel Guide");
        player.sendMessage("");
        player.sendMessage(ChatColor.AQUA + "Left-Click: " + ChatColor.GRAY + "Teleport to destination");
        player.sendMessage(ChatColor.AQUA + "Right-Click: " + ChatColor.GRAY + "View destination info");
        player.sendMessage(ChatColor.AQUA + "Refresh: " + ChatColor.GRAY + "Update destination list");
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "Unlock destinations by visiting them for the first time!");
        player.sendMessage("");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.5F);
    }

    private ItemStack createTitle(Player player) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Fast Travel");
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Travel instantly to unlocked");
        lore.add(ChatColor.GRAY + "destinations across the world!");
        lore.add("");
        int unlocked = (int) manager.getUnlockedPoints(player).stream()
                .filter(key -> manager.getPointByName(key) != null)
                .count();
        int total = manager.getAllPoints().size();
        lore.add(ChatColor.GREEN + "Unlocked: " + ChatColor.AQUA + unlocked + "/" + total);
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click destinations to travel!");
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createStats(Player player) {
        ItemStack item = new ItemStack(Material.FILLED_MAP);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Stats");
        List<String> lore = new ArrayList<>();
        lore.add("");
        int unlocked = (int) manager.getUnlockedPoints(player).stream()
                .filter(key -> manager.getPointByName(key) != null)
                .count();
        int total = manager.getAllPoints().size();
        lore.add(ChatColor.GRAY + "Destinations Unlocked: " + unlocked + "/" + total);
        lore.add("");
        lore.add(ChatColor.YELLOW + "Keep exploring to");
        lore.add(ChatColor.YELLOW + "unlock more!");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createControls() {
        ItemStack item = new ItemStack(Material.BOOKSHELF);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Controls");
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Left-Click: Teleport");
        lore.add(ChatColor.GRAY + "Right-Click: View Info");
        lore.add(ChatColor.GRAY + "Refresh: Update List");
        lore.add("");
        lore.add(ChatColor.AQUA + "Unlock by visiting!");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createDestinationItem(FastTravelManager.FastTravelPoint point, boolean unlocked, Player player) {
        if (!unlocked) {
            // Locked destination - gray stained glass pane with lock
            ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.RED + point.name());
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + point.description());
            lore.add("");
            lore.add(ChatColor.RED + "Locked");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Visit this location to unlock!");
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(destinationKey, PersistentDataType.STRING, point.key());
            item.setItemMeta(meta);
            return item;
        }

        // Unlocked destination - player head
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
        
        // Set the player head owner
        String headOwner = point.headOwner();
        if (!headOwner.isEmpty()) {
            OfflinePlayer owner = Bukkit.getOfflinePlayer(headOwner);
            if (owner != null) {
                skullMeta.setOwningPlayer(owner);
            }
        }
        
        skullMeta.setDisplayName(ChatColor.GREEN + point.name());
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + point.description());
        lore.add("");
        lore.add(ChatColor.GRAY + "World: " + ChatColor.AQUA + point.location().getWorld().getName());
        lore.add(ChatColor.GRAY + "Coords: " + ChatColor.YELLOW + String.format("%.0f, %.0f, %.0f", 
                point.location().getX(), point.location().getY(), point.location().getZ()));
        lore.add("");
        
        if (point.requiredLevel() > 0) {
            lore.add(ChatColor.GOLD + "Required Level: " + ChatColor.YELLOW + point.requiredLevel());
            lore.add("");
        }
        
        lore.add(ChatColor.YELLOW + "Left-Click: Teleport");
        lore.add(ChatColor.AQUA + "Right-Click: Info");
        lore.add("");
        
        skullMeta.setLore(lore);
        skullMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        skullMeta.getPersistentDataContainer().set(destinationKey, PersistentDataType.STRING, point.key());
        item.setItemMeta(skullMeta);
        return item;
    }

    private ItemStack createButton(Material material, String displayName, String loreLine, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', loreLine));
        if (action != null && !action.isBlank()) {
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGlass(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    private static class FastTravelHolder implements InventoryHolder {
        private Inventory inventory;
        private final int page;

        FastTravelHolder(int page) {
            this.page = page;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
