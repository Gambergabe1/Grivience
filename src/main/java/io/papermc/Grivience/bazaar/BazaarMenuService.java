package io.papermc.Grivience.bazaar;

import net.wesjd.anvilgui.AnvilGUI;
import io.papermc.Grivience.item.CustomItemService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Combined bazaar menu (custom + vanilla) using standard Bukkit GUI.
 */
public final class BazaarMenuService implements Listener {
    private static final int PAGE_SIZE = 45;
    private static final String TITLE = ChatColor.DARK_AQUA + "Bazaar";

    private final BazaarShopManager manager;
    private final CustomItemService customItemService;
    private final NamespacedKey entryKey;

    public BazaarMenuService(BazaarShopManager manager, CustomItemService customItemService) {
        this.manager = manager;
        this.customItemService = customItemService;
        this.entryKey = new NamespacedKey(manager.getPlugin(), "bazaar_entry");
    }

    public void openMain(Player player) {
        openMerged(player, 0);
    }

    private void openMerged(Player player, int page) {
        List<ItemEntry> entries = mergedEntries();
        int totalPages = Math.max(1, (int) Math.ceil(entries.size() / (double) PAGE_SIZE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));

        String title = TITLE + " " + (safePage + 1) + "/" + totalPages;
        BazaarHolder holder = new BazaarHolder(safePage);
        Inventory inventory = Bukkit.createInventory(holder, 54, title);
        holder.inventory = inventory;

        // Fill with items
        int start = safePage * PAGE_SIZE;
        int slot = 0;
        for (int i = start; i < Math.min(entries.size(), start + PAGE_SIZE); i++) {
            if (slot >= PAGE_SIZE) break;
            ItemEntry entry = entries.get(i);
            ItemStack displayItem = enrich(entry.display(), entry.pricing(), entry);
            inventory.setItem(slot, displayItem);
            slot++;
        }

        // Navigation row (bottom 9 slots)
        // Slot 45: Previous page
        inventory.setItem(45, createNavItem(
                safePage > 0 ? Material.ARROW : Material.BARRIER,
                safePage > 0 ? ChatColor.YELLOW + "Previous" : ChatColor.DARK_GRAY + "Previous",
                "Page " + Math.max(1, safePage),
                "prev_page"
        ));

        // Slot 46: Back to main
        inventory.setItem(46, createNavItem(Material.COMPASS, ChatColor.AQUA + "Back", "Bazaar Main", "back"));

        // Slot 47: Empty filler
        inventory.setItem(47, createFiller());

        // Slot 48: Trade info
        inventory.setItem(48, createNavItem(Material.EMERALD, ChatColor.GREEN + "Trade Info", "Click for help", "info"));

        // Slot 49: Close
        inventory.setItem(49, createNavItem(Material.BARRIER, ChatColor.RED + "Close", "Close menu", "close"));

        // Slot 50: Refresh
        inventory.setItem(50, createNavItem(Material.CLOCK, ChatColor.AQUA + "Refresh", "Reload items", "refresh"));

        // Slot 51: Empty filler
        inventory.setItem(51, createFiller());

        // Slot 52: Next page
        inventory.setItem(52, createNavItem(
                safePage + 1 < totalPages ? Material.ARROW : Material.BARRIER,
                safePage + 1 < totalPages ? ChatColor.YELLOW + "Next" : ChatColor.DARK_GRAY + "Next",
                "Page " + (safePage + 2),
                "next_page"
        ));

        // Slot 53: Empty filler
        inventory.setItem(53, createFiller());

        player.openInventory(inventory);
    }

    private ItemStack createNavItem(Material material, String name, String lore, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of(ChatColor.GRAY + lore));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        if (action != null) {
            meta.getPersistentDataContainer().set(entryKey, PersistentDataType.STRING, "action:" + action);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack enrich(ItemStack base, BazaarShopManager.PriceInfo priceInfo, ItemEntry entry) {
        if (base == null) {
            return createErrorItem();
        }
        ItemStack clone = base.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta == null) {
            return clone;
        }

        String originalName = meta.hasDisplayName() ? meta.getDisplayName() : ChatColor.WHITE + formatMaterialName(clone.getType());
        List<String> existingLore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

        // Add separator if lore exists
        if (!existingLore.isEmpty()) {
            existingLore.add("");
        }

        // Add bazaar pricing info
        existingLore.add(ChatColor.GOLD + "" + ChatColor.BOLD + "Bazaar Info");
        existingLore.add(ChatColor.GRAY + "Buy: " + ChatColor.GOLD + manager.formatCoins(priceInfo.buyPrice()));
        existingLore.add(ChatColor.GRAY + "Sell: " + ChatColor.GREEN + manager.formatCoins(priceInfo.sellPrice()));
        existingLore.add("");
        existingLore.add(ChatColor.DARK_GRAY + "Left=Buy | Right=Sell | Shift=Stack");
        existingLore.add(ChatColor.DARK_GRAY + "Tap=Custom Amount");

        meta.setDisplayName(originalName + ChatColor.RESET + " " + ChatColor.GOLD + "[Bazaar]");
        meta.setLore(existingLore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

        // Store entry data for click handling
        String entryData = entry.custom() ? "custom:" + entry.key() : "material:" + entry.material().name();
        meta.getPersistentDataContainer().set(entryKey, PersistentDataType.STRING, "entry:" + entryData);

        clone.setItemMeta(meta);
        return clone;
    }

    private ItemStack createErrorItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Item Unavailable");
        meta.setLore(List.of(ChatColor.GRAY + "This item cannot be displayed."));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof BazaarHolder)) return;

        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getClickedInventory().getType() != org.bukkit.event.inventory.InventoryType.CHEST) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir() || !clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();
        String data = meta.getPersistentDataContainer().get(entryKey, PersistentDataType.STRING);
        if (data == null) return;

        ClickType click = event.getClick();

        // Handle navigation buttons
        if (data.startsWith("action:")) {
            String action = data.substring("action:".length());
            handleNavigation(player, action);
            return;
        }

        // Handle item trades
        if (data.startsWith("entry:")) {
            String entryData = data.substring("entry:".length());
            handleTradeClick(player, entryData, click);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof BazaarHolder) {
            event.setCancelled(true);
        }
    }

    private void handleNavigation(Player player, String action) {
        Inventory inv = player.getOpenInventory().getTopInventory();
        if (!(inv.getHolder() instanceof BazaarHolder holder)) {
            return;
        }
        int currentPage = holder.getPage();

        switch (action) {
            case "prev_page" -> openMerged(player, Math.max(0, currentPage - 1));
            case "next_page" -> {
                List<ItemEntry> entries = mergedEntries();
                int totalPages = Math.max(1, (int) Math.ceil(entries.size() / (double) PAGE_SIZE));
                openMerged(player, Math.min(totalPages - 1, currentPage + 1));
            }
            case "back" -> openMerged(player, 0);
            case "refresh" -> openMerged(player, currentPage);
            case "close" -> player.closeInventory();
            case "info" -> showTradeHelp(player);
        }
    }       

    private void handleTradeClick(Player player, String entryData, ClickType click) {
        boolean isCustom = entryData.startsWith("custom:");
        String key = entryData.substring(isCustom ? "custom:".length() : "material:".length());

        if (click == null || click == ClickType.UNKNOWN) {
            // Treat as custom amount
            promptAmount(player, amount -> {
                if (isCustom) manager.instantBuyCustom(player, key, amount);
                else manager.instantBuyMaterial(player, Material.getMaterial(key), amount);
            });
            return;
        }

        boolean right = click.isRightClick();
        boolean shift = click.isShiftClick();

        if (shift) {
            // Shift-click: trade full stack
            int stackSize = isCustom ? 64 : Material.getMaterial(key).getMaxStackSize();
            if (right) {
                if (isCustom) manager.instantSellCustom(player, key, stackSize);
                else manager.instantSellMaterial(player, Material.getMaterial(key), stackSize);
            } else {
                if (isCustom) manager.instantBuyCustom(player, key, stackSize);
                else manager.instantBuyMaterial(player, Material.getMaterial(key), stackSize);
            }
            return;
        }

        if (isCustom) {
            if (right) {
                promptAmount(player, amount -> manager.instantSellCustom(player, key, amount));
            } else {
                promptAmount(player, amount -> manager.instantBuyCustom(player, key, amount));
            }
        } else {
            Material mat = Material.getMaterial(key);
            if (right) {
                promptAmount(player, amount -> manager.instantSellMaterial(player, mat, amount));
            } else {
                promptAmount(player, amount -> manager.instantBuyMaterial(player, mat, amount));
            }
        }
    }

    private void promptAmount(Player player, IntConsumer onAmount) {
        new AnvilGUI.Builder()
                .plugin(manager.getPlugin())
                .title("Enter Amount")
                .itemLeft(new ItemStack(Material.NAME_TAG))
                .text("1")
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return java.util.Collections.emptyList();
                    
                    try {
                        String input = stateSnapshot.getText().trim();
                        if (input.isEmpty()) {
                            player.sendMessage(ChatColor.RED + "Please enter a valid number.");
                            return List.of(AnvilGUI.ResponseAction.close());
                        }
                        int amount = Integer.parseInt(input);
                        amount = Math.max(1, Math.min(640, amount));
                        int finalAmount = amount;
                        return List.of(AnvilGUI.ResponseAction.run(() -> onAmount.accept(finalAmount)));
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Invalid number. Please enter 1-640.");
                        return List.of(AnvilGUI.ResponseAction.close());
                    }
                })
                .open(player);
    }

    private void showTradeHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Bazaar Trading Help ===");
        player.sendMessage(ChatColor.YELLOW + "Left Click: " + ChatColor.GRAY + "Buy items");
        player.sendMessage(ChatColor.YELLOW + "Right Click: " + ChatColor.GRAY + "Sell items");
        player.sendMessage(ChatColor.YELLOW + "Shift + Left: " + ChatColor.GRAY + "Buy full stack");
        player.sendMessage(ChatColor.YELLOW + "Shift + Right: " + ChatColor.GRAY + "Sell full stack");
        player.sendMessage(ChatColor.YELLOW + "Tap (no click): " + ChatColor.GRAY + "Enter custom amount");
        player.sendMessage(ChatColor.GOLD + "Prices are dynamic based on player orders!");
    }

    private String formatMaterialName(Material material) {
        if (material == null) return "Unknown";
        String name = material.name();
        String[] parts = name.split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                result.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    result.append(part.substring(1).toLowerCase());
                }
                result.append(" ");
            }
        }
        return result.toString().trim();
    }

    private List<ItemEntry> mergedEntries() {
        List<ItemEntry> entries = new ArrayList<>();

        // Add custom items
        for (String key : manager.listCustomKeys()) {
            ItemStack display = customItemService.createItemByKey(key);
            if (display == null) continue;
            entries.add(new ItemEntry(true, key, null, display, manager.customPriceInfo(key)));
        }

        // Add vanilla materials
        for (Material mat : manager.listVanillaMaterials()) {
            entries.add(new ItemEntry(false, null, mat, new ItemStack(mat), manager.materialPriceInfo(mat)));
        }

        // Sort alphabetically by display name
        entries.sort(Comparator.comparing(entry -> displayName(entry.display()), String.CASE_INSENSITIVE_ORDER));
        return entries;
    }

    private String displayName(ItemStack item) {
        if (item == null) return "Unknown";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String name = item.getItemMeta().getDisplayName();
            String stripped = ChatColor.stripColor(name);
            return stripped != null ? stripped : item.getType().name();
        }
        return item.getType().name();
    }

    private static final class BazaarHolder implements InventoryHolder {
        private final int page;
        private Inventory inventory;

        private BazaarHolder(int page) {
            this.page = page;
        }

        public int getPage() {
            return page;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private record ItemEntry(boolean custom, String key, Material material, ItemStack display, BazaarShopManager.PriceInfo pricing) {
    }
}
