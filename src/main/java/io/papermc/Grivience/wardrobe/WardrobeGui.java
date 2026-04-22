package io.papermc.Grivience.wardrobe;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.gui.SkyblockGui;
import io.papermc.Grivience.util.DropDeliveryUtil;
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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Hypixel-style wardrobe GUI:
 * - 9 slots per page
 * - helmet/chestplate/leggings/boots preview rows
 * - status dye row for equip/save/unequip
 */
public final class WardrobeGui implements Listener {
    private static final int PAGE_SIZE = 9;
    private static final int PAGE_COUNT = 2;

    private static final int[] HELMET_ROW = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] CHESTPLATE_ROW = {9, 10, 11, 12, 13, 14, 15, 16, 17};
    private static final int[] LEGGINGS_ROW = {18, 19, 20, 21, 22, 23, 24, 25, 26};
    private static final int[] BOOTS_ROW = {27, 28, 29, 30, 31, 32, 33, 34, 35};
    private static final int[] STATUS_ROW = {36, 37, 38, 39, 40, 41, 42, 43, 44};

    private static final int BACK_SLOT = 45;
    private static final int TITLE_SLOT = 46;
    private static final int PREV_SLOT = 47;
    private static final int STATS_SLOT = 48;
    private static final int INFO_SLOT = 49;
    private static final int ACTIVE_SLOT = 50;
    private static final int NEXT_SLOT = 51;
    private static final int FILLER_SLOT = 52;
    private static final int CLOSE_SLOT = 53;

    private static final int ARMOR_INDEX_BOOTS = 0;
    private static final int ARMOR_INDEX_LEGGINGS = 1;
    private static final int ARMOR_INDEX_CHESTPLATE = 2;
    private static final int ARMOR_INDEX_HELMET = 3;

    private final WardrobeManager manager;
    private final GriviencePlugin plugin;
    private final Map<UUID, Long> clickCooldowns = new HashMap<>();
    private final NamespacedKey actionKey;
    private final NamespacedKey slotKey;

    public WardrobeGui(GriviencePlugin plugin, WardrobeManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.actionKey = new NamespacedKey(plugin, "wardrobe-action");
        this.slotKey = new NamespacedKey(plugin, "wardrobe-slot");
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int requestedPage) {
        int page = Math.max(0, Math.min(PAGE_COUNT - 1, requestedPage));
        WardrobeHolder holder = new WardrobeHolder(page);
        Inventory inventory = Bukkit.createInventory(holder, 54, title(page));
        holder.inventory = inventory;
        createLayout(inventory, player, page);
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.2F);
    }

    private void createLayout(Inventory inv, Player player, int page) {
        for (int slot = 0; slot < inv.getSize(); slot++) {
            inv.setItem(slot, filler(Material.BLACK_STAINED_GLASS_PANE));
        }

        int startSlot = page * PAGE_SIZE;
        for (int column = 0; column < PAGE_SIZE; column++) {
            int wardrobeSlot = startSlot + column;
            if (wardrobeSlot >= WardrobeManager.MAX_SLOTS) {
                continue;
            }

            WardrobeManager.SlotData data = manager.getSlot(player, wardrobeSlot);
            inv.setItem(HELMET_ROW[column], armorPieceItem(player, wardrobeSlot, data, ARMOR_INDEX_HELMET, "Helmet", Material.LEATHER_HELMET));
            inv.setItem(CHESTPLATE_ROW[column], armorPieceItem(player, wardrobeSlot, data, ARMOR_INDEX_CHESTPLATE, "Chestplate", Material.LEATHER_CHESTPLATE));
            inv.setItem(LEGGINGS_ROW[column], armorPieceItem(player, wardrobeSlot, data, ARMOR_INDEX_LEGGINGS, "Leggings", Material.LEATHER_LEGGINGS));
            inv.setItem(BOOTS_ROW[column], armorPieceItem(player, wardrobeSlot, data, ARMOR_INDEX_BOOTS, "Boots", Material.LEATHER_BOOTS));
            inv.setItem(STATUS_ROW[column], statusItem(player, wardrobeSlot, data));
        }

        inv.setItem(BACK_SLOT, actionButton(Material.ARROW, "&aGo Back", List.of("&7Return to the SkyBlock Menu."), "back"));
        inv.setItem(TITLE_SLOT, titleItem(player));
        inv.setItem(PREV_SLOT, actionButton(
                Material.ARROW,
                page > 0 ? "&aPrevious Page" : "&7Previous Page",
                List.of(page > 0 ? "&7View the previous wardrobe page." : "&cYou are already on the first page."),
                page > 0 ? "prev" : null
        ));
        inv.setItem(STATS_SLOT, statsItem(player));
        inv.setItem(INFO_SLOT, infoItem());
        inv.setItem(ACTIVE_SLOT, activeItem(player));
        inv.setItem(NEXT_SLOT, actionButton(
                Material.ARROW,
                page < PAGE_COUNT - 1 ? "&aNext Page" : "&7Next Page",
                List.of(page < PAGE_COUNT - 1 ? "&7View the next wardrobe page." : "&cYou are already on the last page."),
                page < PAGE_COUNT - 1 ? "next" : null
        ));
        inv.setItem(FILLER_SLOT, filler(Material.GRAY_STAINED_GLASS_PANE));
        inv.setItem(CLOSE_SLOT, actionButton(Material.BARRIER, "&cClose", List.of("&7Close this menu."), "close"));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof WardrobeHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        long now = System.currentTimeMillis();
        Long lastClick = clickCooldowns.get(player.getUniqueId());
        if (lastClick != null && now - lastClick < 150L) {
            return;
        }
        clickCooldowns.put(player.getUniqueId(), now);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (action != null) {
            handleAction(player, action, holder.page);
            return;
        }

        Integer slot = meta.getPersistentDataContainer().get(slotKey, PersistentDataType.INTEGER);
        if (slot != null) {
            handleWardrobeSlot(player, slot, event.getClick(), holder.page);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof WardrobeHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clickCooldowns.remove(event.getPlayer().getUniqueId());
    }

    private void handleAction(Player player, String action, int page) {
        switch (action) {
            case "back" -> {
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> player.performCommand("skyblock menu"));
            }
            case "close" -> player.closeInventory();
            case "prev" -> open(player, page - 1);
            case "next" -> open(player, page + 1);
            case "info" -> sendGuide(player);
            default -> {
            }
        }
    }

    private void handleWardrobeSlot(Player player, int slot, ClickType click, int page) {
        if (!manager.isUnlocked(player, slot)) {
            sendLockedMessage(player, slot);
            return;
        }

        if (click.isRightClick()) {
            saveSlot(player, slot, page);
            return;
        }

        if (!click.isLeftClick()) {
            return;
        }

        if (manager.isEquippedSlot(player, slot)) {
            unequipSlot(player, slot, page);
            return;
        }

        equipSlot(player, slot, page);
    }

    private void equipSlot(Player player, int slot, int page) {
        WardrobeManager.SlotData target = manager.getSlot(player, slot);
        if (target == null || manager.isEmptyArmor(target.armor())) {
            player.sendMessage(ChatColor.RED + "This wardrobe slot contains no armor.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            return;
        }

        ItemStack[] currentArmor = manager.cloneArmor(player.getInventory().getArmorContents());
        int equippedSlot = manager.getEquippedSlot(player);

        if (equippedSlot >= 0 && equippedSlot != slot) {
            WardrobeManager.SlotData currentWardrobeSlot = manager.getSlot(player, equippedSlot);
            if (currentWardrobeSlot != null && !manager.isEmptyArmor(currentArmor)) {
                manager.saveSlot(player, equippedSlot, currentArmor, currentWardrobeSlot.name());
            }
        } else if (equippedSlot < 0 && !manager.isEmptyArmor(currentArmor)) {
            moveArmorIntoInventory(player, currentArmor);
        }

        player.getInventory().setArmorContents(manager.cloneArmor(target.armor()));
        manager.setEquippedSlot(player, slot);
        player.sendMessage(ChatColor.GREEN + "Equipped Wardrobe Slot " + ChatColor.YELLOW + (slot + 1) + ChatColor.GREEN + ".");
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0F, 1.1F);
        Bukkit.getScheduler().runTask(plugin, () -> open(player, page));
    }

    private void unequipSlot(Player player, int slot, int page) {
        ItemStack[] currentArmor = manager.cloneArmor(player.getInventory().getArmorContents());
        WardrobeManager.SlotData active = manager.getSlot(player, slot);
        if (active != null && !manager.isEmptyArmor(currentArmor)) {
            manager.saveSlot(player, slot, currentArmor, active.name());
        }

        moveArmorIntoInventory(player, currentArmor);
        manager.clearEquippedSlot(player);
        player.sendMessage(ChatColor.YELLOW + "Unequipped your current wardrobe set.");
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0F, 0.9F);
        Bukkit.getScheduler().runTask(plugin, () -> open(player, page));
    }

    private void saveSlot(Player player, int slot, int page) {
        if (manager.isEquippedSlot(player, slot)) {
            player.sendMessage(ChatColor.RED + "You cannot modify your active wardrobe setup through the Wardrobe menu.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            return;
        }

        ItemStack[] armor = manager.cloneArmor(player.getInventory().getArmorContents());
        if (manager.isEmptyArmor(armor)) {
            player.sendMessage(ChatColor.RED + "Wear an armor set first.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            return;
        }

        WardrobeManager.SlotData existing = manager.getSlot(player, slot);
        String name = existing != null ? existing.name() : "Slot " + (slot + 1);
        manager.saveSlot(player, slot, armor, name);
        player.sendMessage(ChatColor.GREEN + "Stored your armor in Wardrobe Slot " + ChatColor.YELLOW + (slot + 1) + ChatColor.GREEN + ".");
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0F, 1.0F);
        Bukkit.getScheduler().runTask(plugin, () -> open(player, page));
    }

    private void moveArmorIntoInventory(Player player, ItemStack[] armor) {
        player.getInventory().setArmorContents(new ItemStack[4]);
        if (manager.isEmptyArmor(armor)) {
            return;
        }
        for (ItemStack piece : armor) {
            if (piece == null || piece.getType().isAir()) {
                continue;
            }
            DropDeliveryUtil.giveToInventoryOrDrop(player, piece, player.getLocation());
        }
    }

    private void sendGuide(Player player) {
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Wardrobe Guide");
        player.sendMessage(ChatColor.GRAY + "Left-Click a pink dye to equip that setup.");
        player.sendMessage(ChatColor.GRAY + "Left-Click a lime dye to unequip your active setup.");
        player.sendMessage(ChatColor.GRAY + "Right-Click a wardrobe slot to store your current armor.");
        player.sendMessage(ChatColor.GRAY + "The Wardrobe unlocks at " + ChatColor.YELLOW + "SkyBlock Level " + WardrobeManager.UNLOCK_LEVEL + ChatColor.GRAY + ".");
        player.sendMessage(ChatColor.GRAY + "Additional slots are granted by rank permissions.");
        player.sendMessage("");
    }

    private void sendLockedMessage(Player player, int slot) {
        String requirement = manager.getUnlockRequirement(player, slot);
        if (manager.getAllowedSlots(player) <= 0) {
            player.sendMessage(ChatColor.RED + "Wardrobe unlocks at " + ChatColor.YELLOW + requirement + ChatColor.RED + ".");
        } else {
            player.sendMessage(ChatColor.RED + "That wardrobe slot requires " + ChatColor.YELLOW + requirement + ChatColor.RED + ".");
        }
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
    }

    private ItemStack armorPieceItem(
            Player player,
            int wardrobeSlot,
            WardrobeManager.SlotData data,
            int armorIndex,
            String pieceName,
            Material placeholderType
    ) {
        boolean unlocked = manager.isUnlocked(player, wardrobeSlot);
        ItemStack storedPiece = storedArmorPiece(data, armorIndex);
        ItemStack item = storedPiece != null ? storedPiece.clone() : new ItemStack(unlocked ? placeholderType : Material.BLACK_STAINED_GLASS_PANE);

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        List<String> lore = meta.hasLore() && storedPiece != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add("");
        if (!unlocked) {
            meta.setDisplayName(ChatColor.DARK_GRAY + "Slot " + (wardrobeSlot + 1) + " " + pieceName);
            lore.add(ChatColor.RED + "Locked");
            lore.add(ChatColor.GRAY + "Requires " + manager.getUnlockRequirement(player, wardrobeSlot) + ".");
        } else if (storedPiece == null) {
            meta.setDisplayName(ChatColor.GRAY + "Slot " + (wardrobeSlot + 1) + " " + pieceName);
            lore.add(ChatColor.GRAY + "Place a " + pieceName.toLowerCase() + " here to add");
            lore.add(ChatColor.GRAY + "it to the armor set.");
        } else {
            lore.add(ChatColor.GOLD + "Wardrobe Slot " + (wardrobeSlot + 1));
            lore.add(ChatColor.GRAY + "Stored in this setup.");
            if (manager.isCustomArmor(storedPiece)) {
                String setName = manager.getCustomArmorSet(storedPiece);
                if (setName != null) {
                    lore.add(ChatColor.LIGHT_PURPLE + "Set: " + ChatColor.WHITE + formatSetName(setName));
                }
            }
        }

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack storedArmorPiece(WardrobeManager.SlotData data, int armorIndex) {
        if (data == null || data.armor() == null || armorIndex < 0 || armorIndex >= data.armor().length) {
            return null;
        }
        ItemStack piece = data.armor()[armorIndex];
        return piece == null || piece.getType().isAir() ? null : piece;
    }

    private ItemStack statusItem(Player player, int wardrobeSlot, WardrobeManager.SlotData data) {
        boolean unlocked = manager.isUnlocked(player, wardrobeSlot);
        boolean hasArmor = data != null && !manager.isEmptyArmor(data.armor());
        boolean active = unlocked && manager.isEquippedSlot(player, wardrobeSlot);

        Material material;
        String name;
        List<String> lore = new ArrayList<>();
        if (!unlocked) {
            material = Material.RED_DYE;
            name = ChatColor.RED + "Slot " + (wardrobeSlot + 1);
            lore.add(ChatColor.GRAY + "This wardrobe slot is locked.");
            lore.add(ChatColor.GRAY + "Requires " + manager.getUnlockRequirement(player, wardrobeSlot) + ".");
        } else if (active) {
            material = Material.LIME_DYE;
            name = ChatColor.GREEN + "Slot " + (wardrobeSlot + 1);
            lore.add(ChatColor.GRAY + "This wardrobe slot contains your");
            lore.add(ChatColor.GRAY + "current armor set.");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Left-Click to unequip.");
            lore.add(ChatColor.RED + "Right-Click disabled while active.");
        } else if (hasArmor) {
            material = Material.PINK_DYE;
            name = ChatColor.LIGHT_PURPLE + "Slot " + (wardrobeSlot + 1);
            lore.add(ChatColor.GRAY + "This wardrobe slot is ready to");
            lore.add(ChatColor.GRAY + "be equipped.");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Left-Click to equip.");
            lore.add(ChatColor.AQUA + "Right-Click to store your current set.");
        } else {
            material = Material.GRAY_DYE;
            name = ChatColor.GRAY + "Slot " + (wardrobeSlot + 1);
            lore.add(ChatColor.GRAY + "This wardrobe slot contains no");
            lore.add(ChatColor.GRAY + "armor.");
            lore.add("");
            lore.add(ChatColor.AQUA + "Right-Click to store your current set.");
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(slotKey, PersistentDataType.INTEGER, wardrobeSlot);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack titleItem(Player player) {
        ItemStack item = new ItemStack(Material.LEATHER_CHESTPLATE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(ChatColor.GREEN + "Wardrobe");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Quick swap between stored armor sets.");
        lore.add("");
        if (manager.getAllowedSlots(player) <= 0) {
            lore.add(ChatColor.RED + "Unlocked at SkyBlock Level " + WardrobeManager.UNLOCK_LEVEL + ".");
        } else {
            lore.add(ChatColor.GRAY + "Unlocked Slots: " + ChatColor.YELLOW + manager.getAllowedSlots(player));
            lore.add(ChatColor.GRAY + "Used Slots: " + ChatColor.YELLOW + manager.getUsedSlots(player));
        }
        meta.setLore(lore);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack statsItem(Player player) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(ChatColor.AQUA + "Wardrobe Stats");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "SkyBlock Level: " + ChatColor.YELLOW + (plugin.getSkyblockLevelManager() != null ? plugin.getSkyblockLevelManager().getLevel(player) : 0));
        lore.add(ChatColor.GRAY + "Unlocked Slots: " + ChatColor.YELLOW + manager.getAllowedSlots(player) + ChatColor.GRAY + "/" + WardrobeManager.MAX_SLOTS);
        lore.add(ChatColor.GRAY + "Stored Sets: " + ChatColor.YELLOW + manager.getUsedSlots(player));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack infoItem() {
        return actionButton(
                Material.WRITABLE_BOOK,
                "&bInformation",
                List.of(
                        "&7Wardrobe unlocks at SkyBlock Level 5.",
                        "&7Slots: None=2, VIP=5, VIP+=9, MVP=13, MVP+=18.",
                        "&7Pink dye equips, lime dye unequips, gray dye stores."
                ),
                "info"
        );
    }

    private ItemStack activeItem(Player player) {
        int activeSlot = manager.getEquippedSlot(player);
        ItemStack item = new ItemStack(activeSlot >= 0 ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        if (activeSlot >= 0) {
            meta.setDisplayName(ChatColor.GREEN + "Active Setup");
            meta.setLore(List.of(
                    ChatColor.GRAY + "Current Wardrobe Slot:",
                    ChatColor.YELLOW + "Slot " + (activeSlot + 1)
            ));
        } else {
            meta.setDisplayName(ChatColor.GRAY + "No Active Setup");
            meta.setLore(List.of(
                    ChatColor.GRAY + "Your current armor was not equipped",
                    ChatColor.GRAY + "from a wardrobe slot."
            ));
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack actionButton(Material material, String name, List<String> loreLines, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        List<String> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(lore);
        if (action != null) {
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack filler(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private String title(int page) {
        return SkyblockGui.title("Wardrobe (" + (page + 1) + "/" + PAGE_COUNT + ")");
    }

    private String formatSetName(String set) {
        return switch (set.toLowerCase()) {
            case "shogun" -> "Shogun Set";
            case "shinobi" -> "Shinobi Set";
            case "onmyoji" -> "Onmyoji Set";
            case "titan" -> "Titan Set";
            case "leviathan" -> "Leviathan Set";
            case "guardian" -> "Guardian Set";
            default -> set.substring(0, 1).toUpperCase() + set.substring(1).toLowerCase() + " Set";
        };
    }

    private static final class WardrobeHolder implements InventoryHolder {
        private final int page;
        private Inventory inventory;

        private WardrobeHolder(int page) {
            this.page = page;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
