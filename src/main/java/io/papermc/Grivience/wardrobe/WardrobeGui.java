package io.papermc.Grivience.wardrobe;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.gui.SkyblockGui;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Skyblock 100% Accurate Wardrobe GUI System.
 */
public final class WardrobeGui implements Listener {
    private static final String TITLE = SkyblockGui.title("Wardrobe");
    private static final int[] WARDROBE_SLOTS = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34};
    private static final int BACK_SLOT = 45, INFO_SLOT = 48, MIDDLE_SLOT = 49, CLOSE_SLOT = 52;

    private final WardrobeManager manager;
    private final GriviencePlugin plugin;
    private final Map<UUID, Integer> renameQueue = new HashMap<>();
    private final Map<UUID, Long> clickCooldowns = new HashMap<>();
    private final NamespacedKey actionKey;
    private final NamespacedKey slotKey;

    public WardrobeGui(GriviencePlugin plugin, WardrobeManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.actionKey = new NamespacedKey(plugin, "wardrobe-action");
        this.slotKey = new NamespacedKey(plugin, "wardrobe-slot");
    }

    public void open(Player player) { open(player, 0); }

    public void open(Player player, int page) {
        WardrobeHolder holder = new WardrobeHolder(page);
        Inventory inv = Bukkit.createInventory(holder, 54, TITLE);
        holder.inventory = inv;
        createLayout(inv, player, page);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.2F);
    }

    private void createLayout(Inventory inv, Player player, int page) {
        // Background
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) {
                boolean border = i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8;
                inv.setItem(i, createGlass(border ? Material.GRAY_STAINED_GLASS_PANE : Material.BLACK_STAINED_GLASS_PANE));
            }
        }

        // Header
        inv.setItem(4, createTitle(player));
        inv.setItem(2, createStats(manager.getUsedSlots(player), manager.getAllowedSlots(player)));
        inv.setItem(6, createControls());

        // Wardrobe slots
        List<WardrobeManager.SlotData> slots = manager.getSlots(player);
        int allowed = manager.getAllowedSlots(player);
        int start = page * 21;
        for (int i = 0; i < 21 && (start + i) < allowed; i++) {
            int slotIdx = start + i;
            WardrobeManager.SlotData data = slotIdx < slots.size() ? slots.get(slotIdx) : null;
            inv.setItem(WARDROBE_SLOTS[i], createSlotItem(data, slotIdx, slotIdx < allowed));
        }

        // Navigation
        inv.setItem(BACK_SLOT, createButton(Material.ARROW, "&aGo Back", "&7To Skyblock Menu.", "back"));
        inv.setItem(46, createButton(Material.ARROW, page > 0 ? "&aPrevious Page" : "&7Previous Page", page > 0 ? "&7View previous wardrobe slots." : "&cYou are on the first page.", page > 0 ? "prev" : null));
        inv.setItem(INFO_SLOT, createButton(Material.BOOK, "&bInformation", "&7Click to view wardrobe help.", "info"));
        int totalPages = (int) Math.ceil(allowed / 21.0);
        inv.setItem(MIDDLE_SLOT, createButton(Material.BOOK, "&7Page " + (page + 1) + "/" + totalPages, "&7" + totalPages + " pages.", null));
        inv.setItem(50, createButton(Material.ARROW, page < totalPages - 1 ? "&aNext Page" : "&7Next Page", page < totalPages - 1 ? "&7View more wardrobe slots." : "&cYou are on the last page.", page < totalPages - 1 ? "next" : null));
        inv.setItem(CLOSE_SLOT, createButton(Material.BARRIER, "&cClose", "&7Close this menu.", "close"));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof WardrobeHolder h)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;

        long now = System.currentTimeMillis();
        if (clickCooldowns.containsKey(p.getUniqueId()) && now - clickCooldowns.get(p.getUniqueId()) < 200) return;
        clickCooldowns.put(p.getUniqueId(), now);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        ItemMeta meta = clicked.getItemMeta();
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (action != null) {
            handleAction(p, action, h.page);
            return;
        }

        Integer slot = meta.getPersistentDataContainer().get(slotKey, PersistentDataType.INTEGER);
        if (slot != null) {
            handleSlot(p, slot, e.getClick(), h.page);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) { if (e.getInventory().getHolder() instanceof WardrobeHolder) e.setCancelled(true); }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        if (renameQueue.containsKey(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
            int slot = renameQueue.remove(e.getPlayer().getUniqueId());
            String name = ChatColor.translateAlternateColorCodes('&', e.getMessage());
            if (name.length() > 30) { e.getPlayer().sendMessage(ChatColor.RED + "Name too long!"); return; }
            manager.rename(e.getPlayer(), slot, name);
            e.getPlayer().sendMessage(ChatColor.GREEN + "Renamed to: " + ChatColor.AQUA + name);
            e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.0F);
            Bukkit.getScheduler().runTask(plugin, () -> open(e.getPlayer()));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) { renameQueue.remove(e.getPlayer().getUniqueId()); clickCooldowns.remove(e.getPlayer().getUniqueId()); }

    private void handleAction(Player p, String action, int page) {
        switch (action) {
            case "back" -> { p.closeInventory(); Bukkit.getScheduler().runTask(plugin, () -> p.performCommand("skyblock menu")); }
            case "close" -> p.closeInventory();
            case "prev" -> open(p, page - 1);
            case "next" -> open(p, page + 1);
            case "info" -> {
                p.sendMessage(""); p.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Wardrobe Guide"); p.sendMessage("");
                p.sendMessage(ChatColor.AQUA + "Left-Click: " + ChatColor.GRAY + "Equip armor");
                p.sendMessage(ChatColor.AQUA + "Shift+Left: " + ChatColor.GRAY + "Quick equip");
                p.sendMessage(ChatColor.AQUA + "Right-Click: " + ChatColor.GRAY + "Save armor");
                p.sendMessage(ChatColor.AQUA + "Shift+Right: " + ChatColor.GRAY + "Rename slot"); p.sendMessage("");
            }
        }
    }

    private void handleSlot(Player p, int slot, org.bukkit.event.inventory.ClickType click, int page) {
        if (slot >= manager.getAllowedSlots(p)) {
            p.sendMessage(ChatColor.RED + "Locked! Unlock at level " + manager.getRequiredLevel(slot));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            return;
        }
        if (click.isLeftClick()) {
            if (click.isShiftClick()) quickEquip(p, slot);
            else equipSlot(p, slot);
        } else if (click.isRightClick()) {
            if (click.isShiftClick()) { renameQueue.put(p.getUniqueId(), slot); p.closeInventory(); p.sendMessage(ChatColor.YELLOW + "Type new name:"); }
            else saveSlot(p, slot);
        }
    }

    private void equipSlot(Player p, int slot) {
        WardrobeManager.SlotData data = manager.getSlot(p, slot);
        if (data == null || manager.isEmptyArmor(data.armor())) { p.sendMessage(ChatColor.RED + "Slot is empty!"); return; }
        
        // Check if player is already wearing this exact armor set (prevent duplication)
        ItemStack[] currentArmor = p.getInventory().getArmorContents();
        if (isSameArmorSet(currentArmor, data.armor())) {
            p.sendMessage(ChatColor.YELLOW + "You are already wearing this armor set!");
            return;
        }
        
        // Equip the stored armor directly without saving current armor
        p.getInventory().setArmorContents(manager.cloneArmor(data.armor()));
        p.sendMessage(ChatColor.GREEN + "Equipped: " + ChatColor.AQUA + data.name());
        p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0F, 1.0F);
        Bukkit.getScheduler().runTask(plugin, () -> open(p));
    }

    /**
     * Check if two armor arrays represent the same set.
     */
    private boolean isSameArmorSet(ItemStack[] armor1, ItemStack[] armor2) {
        if (armor1 == null || armor2 == null) return false;
        if (armor1.length != armor2.length) return false;
        for (int i = 0; i < armor1.length; i++) {
            ItemStack item1 = armor1[i];
            ItemStack item2 = armor2[i];
            if (item1 == null && item2 == null) continue;
            if (item1 == null || item2 == null) return false;
            if (!item1.isSimilar(item2)) return false;
        }
        return true;
    }

    private void quickEquip(Player p, int slot) {
        WardrobeManager.SlotData data = manager.getSlot(p, slot);
        if (data == null || manager.isEmptyArmor(data.armor())) { p.sendMessage(ChatColor.RED + "Slot is empty!"); return; }
        p.getInventory().setArmorContents(manager.cloneArmor(data.armor()));
        p.sendMessage(ChatColor.GREEN + "Equipped: " + ChatColor.AQUA + data.name());
        p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0F, 1.0F);
    }

    private void saveSlot(Player p, int slot) {
        ItemStack[] armor = p.getInventory().getArmorContents();
        if (manager.isEmptyArmor(armor)) { p.sendMessage(ChatColor.RED + "Wear armor first!"); return; }
        WardrobeManager.SlotData existing = manager.getSlot(p, slot);
        String name = existing != null ? existing.name() : "Slot " + (slot + 1);
        manager.saveSlot(p, slot, armor, name);
        p.sendMessage(ChatColor.GREEN + "Saved to: " + ChatColor.AQUA + name);
        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0F, 1.0F);
        Bukkit.getScheduler().runTask(plugin, () -> open(p));
    }

    private ItemStack createTitle(Player p) {
        ItemStack i = new ItemStack(Material.CHEST); ItemMeta m = i.getItemMeta();
        m.setDisplayName(ChatColor.GREEN + "Wardrobe");
        List<String> l = new ArrayList<>(); l.add(""); l.add(ChatColor.GRAY + "Store and manage armor sets."); l.add("");
        l.add(ChatColor.GREEN + "Slots: " + manager.getUsedSlots(p) + "/" + manager.getAllowedSlots(p)); l.add("");
        l.add(ChatColor.YELLOW + "Click slots to manage!"); m.setLore(l); m.addEnchant(Enchantment.UNBREAKING, 1, true); m.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES); i.setItemMeta(m); return i;
    }

    private ItemStack createStats(int used, int total) {
        ItemStack i = new ItemStack(Material.BOOK); ItemMeta m = i.getItemMeta();
        m.setDisplayName(ChatColor.AQUA + "Stats");
        List<String> l = new ArrayList<>(); l.add(""); l.add(ChatColor.GRAY + "Used: " + used + "/" + total); l.add("");
        l.add(ChatColor.YELLOW.toString() + (total - used) + " available"); m.setLore(l); i.setItemMeta(m); return i;
    }

    private ItemStack createControls() {
        ItemStack i = new ItemStack(Material.LEATHER_CHESTPLATE); ItemMeta m = i.getItemMeta();
        m.setDisplayName(ChatColor.YELLOW + "Controls");
        List<String> l = new ArrayList<>(); l.add(""); l.add(ChatColor.GRAY + "Left: Equip"); l.add(ChatColor.GRAY + "Right: Save");
        l.add(ChatColor.GRAY + "Shift+Right: Rename"); m.setLore(l); i.setItemMeta(m); return i;
    }

    private ItemStack createSlotItem(WardrobeManager.SlotData data, int slot, boolean unlocked) {
        if (!unlocked) {
            ItemStack i = new ItemStack(Material.RED_STAINED_GLASS_PANE); ItemMeta m = i.getItemMeta();
            m.setDisplayName(ChatColor.RED + "Locked");
            List<String> l = new ArrayList<>(); l.add(""); l.add(ChatColor.GRAY + "Unlock at level: " + manager.getRequiredLevel(slot)); m.setLore(l); i.setItemMeta(m); return i;
        }
        if (data == null || manager.isEmptyArmor(data.armor())) {
            ItemStack i = new ItemStack(Material.GRAY_STAINED_GLASS_PANE); ItemMeta m = i.getItemMeta();
            m.setDisplayName(ChatColor.GRAY + "Slot " + (slot + 1));
            List<String> l = new ArrayList<>(); l.add(""); l.add(ChatColor.GRAY + "Empty slot"); l.add("");
            l.add(ChatColor.YELLOW + "Right-Click to save your current armor."); l.add("");
            m.setLore(l);
            m.getPersistentDataContainer().set(slotKey, PersistentDataType.INTEGER, slot);
            i.setItemMeta(m);
            return i;
        }
        ItemStack display = manager.getFirstArmorPiece(data.armor());
        ItemMeta m = display.getItemMeta();
        List<String> l = m.hasLore() ? new ArrayList<>(m.getLore()) : new ArrayList<>();
        l.add(""); l.add(ChatColor.translateAlternateColorCodes('&', "&8&m━━━━━━━━━━━━━━━━━━━━━"));
        l.add(ChatColor.translateAlternateColorCodes('&', "&6&lWardrobe Slot " + (slot + 1)));
        l.add(ChatColor.translateAlternateColorCodes('&', "&7Name: &e" + data.name())); l.add("");
        
        // Show custom armor set info if applicable
        if (manager.isCustomArmor(display)) {
            String set = manager.getCustomArmorSet(display);
            String piece = manager.getCustomArmorPiece(display);
            if (set != null) {
                l.add(ChatColor.LIGHT_PURPLE + "Custom Armor Set:");
                l.add(ChatColor.translateAlternateColorCodes('&', "&8  • &d" + formatSetName(set)));
                if (piece != null) l.add(ChatColor.translateAlternateColorCodes('&', "&8  • &7" + formatPieceName(piece)));
                l.add("");
            }
        }
        
        l.add(ChatColor.YELLOW + "Left-Click: Equip");
        l.add(ChatColor.AQUA + "Right-Click: Save");
        l.add(ChatColor.GOLD + "Shift+Right: Rename");
        l.add("");
        m.setLore(l);
        m.getPersistentDataContainer().set(slotKey, PersistentDataType.INTEGER, slot);
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        display.setItemMeta(m); return display;
    }

    private ItemStack createButton(Material mat, String name, String lore1, String action) {
        ItemStack i = new ItemStack(mat); ItemMeta m = i.getItemMeta();
        m.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        List<String> l = new ArrayList<>(); l.add(""); l.add(ChatColor.translateAlternateColorCodes('&', lore1));
        m.setLore(l);
        if (action != null) {
            m.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        }
        i.setItemMeta(m);
        return i;
    }

    private ItemStack createGlass(Material mat) { ItemStack i = new ItemStack(mat); ItemMeta m = i.getItemMeta(); m.setDisplayName(" "); i.setItemMeta(m); return i; }

    private String formatSetName(String set) {
        // Format custom armor set name nicely
        return switch (set.toLowerCase()) {
            case "shogun" -> "Shogun Set";
            case "shinobi" -> "Shinobi Set";
            case "onmyoji" -> "Onmyoji Set";
            case "titan" -> "Titan Set";
            case "leviathan" -> "Leviathan Set";
            case "guardian" -> "Guardian Set";
            default -> set.toUpperCase().charAt(0) + set.substring(1).toLowerCase() + " Set";
        };
    }

    private String formatPieceName(String piece) {
        // Format armor piece name nicely
        return switch (piece.toLowerCase()) {
            case "helmet" -> "Helmet";
            case "chestplate" -> "Chestplate";
            case "leggings" -> "Leggings";
            case "boots" -> "Boots";
            default -> piece.toUpperCase().charAt(0) + piece.substring(1).toLowerCase();
        };
    }

    private static class WardrobeHolder implements InventoryHolder {
        private Inventory inventory; private final int page;
        WardrobeHolder(int page) { this.page = page; }
        @Override public Inventory getInventory() { return inventory; }
    }
}

