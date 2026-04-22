package io.papermc.Grivience.enchantment;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.*;

/**
 * Skyblock 100% Accurate Enchantment Table GUI.
 * Completely redesigned to match Skyblock's exact layout and styling.
 */
public final class EnchantmentTableGui implements Listener {
    // Skyblock-accurate inventory title
    private static final String TITLE = ChatColor.translateAlternateColorCodes('&', "&8Enchant Item");

    // Skyblock-accurate slot positions
    private static final int ITEM_SLOT = 10;
    private static final int[] ENCHANT_SLOTS = {
        14, 15, 16, 17, 18, 19, 20,
        23, 24, 25, 26, 27, 28, 29,
        32, 33, 34, 35, 36, 37, 38,
        41, 42, 43, 44, 45, 46, 47
    };

    // Navigation slots
    private static final int PREV_PAGE_SLOT = 48;
    private static final int PAGE_INFO_SLOT = 49;
    private static final int NEXT_PAGE_SLOT = 50;
    private static final int CLOSE_SLOT = 52;

    private final JavaPlugin plugin;
    private final NamespacedKey enchantIdKey;
    private final NamespacedKey enchantLevelKey;
    private final NamespacedKey actionKey;
    private final SkyblockEnchantStorage enchantStorage;

    public EnchantmentTableGui(JavaPlugin plugin) {
        this.plugin = plugin;
        this.enchantIdKey = new NamespacedKey(plugin, "enchant-id");
        this.enchantLevelKey = new NamespacedKey(plugin, "enchant-level");
        this.actionKey = new NamespacedKey(plugin, "enchant-action");
        this.enchantStorage = new SkyblockEnchantStorage(plugin);
    }

    /**
     * Open the enchantment table GUI - 100% Skyblock accurate.
     */
    public void openEnchantTable(Player player) {
        openEnchantTable(player, 0);
    }

    /**
     * Open with specific page.
     */
    public void openEnchantTable(Player player, int page) {
        ItemStack heldItem = player.getInventory().getItemInMainHand();

        EnchantmentTableHolder holder = new EnchantmentTableHolder(heldItem);
        Inventory inventory = Bukkit.createInventory(holder, 54, TITLE);
        holder.inventory = inventory;

        List<SkyblockEnchantment> enchantments = EnchantmentRegistry.getNormalForItem(heldItem);
        int totalPages = Math.max(1, (int) Math.ceil(enchantments.size() / 28.0));
        int currentPage = Math.max(0, Math.min(page, totalPages - 1));
        holder.page = currentPage;
        holder.totalPages = totalPages;
        holder.totalEnchantments = enchantments.size();

        // Create Skyblock-accurate layout
        createSkyblockLayout(inventory, player, heldItem, enchantments, currentPage, totalPages);

        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0F, 1.2F);
    }

    /**
     * Create the complete Skyblock-accurate layout.
     */
    private void createSkyblockLayout(Inventory inventory, Player player, ItemStack heldItem, 
                                      List<SkyblockEnchantment> enchantments, int page, int totalPages) {
        // 1. Create decorative background
        createDecorativeBackground(inventory);

        // 2. Set item to enchant (left side)
        inventory.setItem(ITEM_SLOT, createItemDisplay(heldItem, player.getLevel()));

        // 3. Set enchantment options in grid
        int start = page * 28;
        int end = Math.min(start + 28, enchantments.size());
        int slotIndex = 0;

        for (int i = start; i < end && slotIndex < ENCHANT_SLOTS.length; i++) {
            SkyblockEnchantment enchantment = enchantments.get(i);
            int slot = ENCHANT_SLOTS[slotIndex++];
            inventory.setItem(slot, createEnchantmentOption(enchantment, heldItem, player.getLevel()));
        }

        // 4. Create bottom navigation row (Skyblock-accurate)
        createNavigationRow(inventory, player, page, totalPages, enchantments.size());

        // 5. Create decorative accent items
        createAccentItems(inventory, player);
    }

    /**
     * Create Skyblock-accurate decorative background.
     */
    private void createDecorativeBackground(Inventory inventory) {
        // Purple border (top and bottom rows)
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, createGlassPane(Material.PURPLE_STAINED_GLASS_PANE));
            inventory.setItem(45 + i, i == 4 ? createGlassPane(Material.PURPLE_STAINED_GLASS_PANE) : createGlassPane(Material.GRAY_STAINED_GLASS_PANE));
        }

        // Side borders (purple)
        for (int row = 1; row < 5; row++) {
            inventory.setItem(row * 9, createGlassPane(Material.PURPLE_STAINED_GLASS_PANE));
            inventory.setItem(row * 9 + 8, createGlassPane(Material.PURPLE_STAINED_GLASS_PANE));
        }

        // Inner background (gray)
        for (int row = 1; row < 5; row++) {
            for (int col = 1; col < 7; col++) {
                int slot = row * 9 + col;
                if (inventory.getItem(slot) == null) {
                    inventory.setItem(slot, createGlassPane(Material.GRAY_STAINED_GLASS_PANE));
                }
            }
        }
    }

    /**
     * Create the navigation row (bottom).
     */
    private void createNavigationRow(Inventory inventory, Player player, int page, int totalPages, int totalEnchants) {
        // Previous page (slot 48)
        inventory.setItem(PREV_PAGE_SLOT, createPrevPageButton(page > 0));

        // Enchant Guide (slot 46) - Between prev page and page info
        inventory.setItem(46, createEnchantGuideItem(player.getLevel()));

        // Next page (slot 50)
        inventory.setItem(NEXT_PAGE_SLOT, createNextPageButton(page < totalPages - 1));

        // Page info (slot 49) - CENTER
        inventory.setItem(PAGE_INFO_SLOT, createPageInfoItem(page, totalPages, totalEnchants, player.getLevel()));

        // Close button (slot 52)
        inventory.setItem(CLOSE_SLOT, createCloseButton());

        // Decorative fillers
        inventory.setItem(47, createGlassPane(Material.GRAY_STAINED_GLASS_PANE));
        inventory.setItem(51, createGlassPane(Material.GRAY_STAINED_GLASS_PANE));
        inventory.setItem(53, createGlassPane(Material.GRAY_STAINED_GLASS_PANE));
    }

    /**
     * Create decorative accent items.
     */
    private void createAccentItems(Inventory inventory, Player player) {
        // Center top - Enchant info (slot 4)
        inventory.setItem(4, createEnchantCenterInfo(player.getLevel()));

        // Center - Info book (slot 13)
        inventory.setItem(13, createInfoBook());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        InventoryHolder rawHolder = top.getHolder();

        if (rawHolder instanceof EnchantmentTableHolder holder) {
            event.setCancelled(true);
            if (event.getClick() == ClickType.DOUBLE_CLICK || event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
                return;
            }
            if (event.getClickedInventory() == null || event.getClickedInventory() != top) {
                return;
            }
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta()) return;

            ItemMeta meta = clicked.getItemMeta();
            String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);

            if (action != null) {
                handleAction(player, action, holder);
                return;
            }

            // Check for enchantment selection
            String enchantId = meta.getPersistentDataContainer().get(enchantIdKey, PersistentDataType.STRING);
            if (enchantId != null) {
                SkyblockEnchantment enchantment = EnchantmentRegistry.get(enchantId);
                if (enchantment != null) {
                    openLevelSelection(player, enchantment, holder.page);
                }
            }
            return;
        }

        if (rawHolder instanceof EnchantmentLevelHolder holder) {
            event.setCancelled(true);
            if (event.getClick() == ClickType.DOUBLE_CLICK || event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
                return;
            }
            if (event.getClickedInventory() == null || event.getClickedInventory() != top) {
                return;
            }
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta()) return;

            ItemMeta meta = clicked.getItemMeta();
            String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
            if (action != null) {
                switch (action) {
                    case "back" -> {
                        openEnchantTable(player, holder.returnPage);
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.0F);
                    }
                    case "close" -> {
                        player.closeInventory();
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 0.8F);
                    }
                    default -> {
                    }
                }
                return;
            }

            String enchantId = meta.getPersistentDataContainer().get(enchantIdKey, PersistentDataType.STRING);
            Integer level = meta.getPersistentDataContainer().get(enchantLevelKey, PersistentDataType.INTEGER);
            if (enchantId == null || level == null) {
                return;
            }

            SkyblockEnchantment enchantment = EnchantmentRegistry.get(enchantId);
            if (enchantment == null) {
                return;
            }

            boolean applied = applyEnchant(player, enchantment, level);
            if (applied) {
                openEnchantTable(player, holder.returnPage);
            } else {
                openLevelSelection(player, enchantment, holder.returnPage);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof EnchantmentTableHolder
                || event.getView().getTopInventory().getHolder() instanceof EnchantmentLevelHolder) {
            event.setCancelled(true);
        }
    }

    private void handleAction(Player player, String action, EnchantmentTableHolder holder) {
        switch (action) {
            case "prev_page" -> openEnchantTable(player, holder.page - 1);
            case "next_page" -> openEnchantTable(player, holder.page + 1);
            case "close" -> {
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 0.8F);
            }
        }
    }

    /**
     * Open level selection for an enchantment.
     */
    public void openLevelSelection(Player player, SkyblockEnchantment enchantment, int page) {
        ItemStack heldItem = player.getInventory().getItemInMainHand();

        EnchantmentLevelHolder holder = new EnchantmentLevelHolder(enchantment, heldItem, page);
        String title = ChatColor.translateAlternateColorCodes('&', "&5Enchant - " + enchantment.getName());
        Inventory inventory = Bukkit.createInventory(holder, 54, title);
        holder.inventory = inventory;

        // Background
        createDecorativeBackground(inventory);

        // Item display (slot 10)
        inventory.setItem(10, createItemDisplay(heldItem, player.getLevel()));

        // Enchantment info (slot 13)
        inventory.setItem(13, createEnchantmentDetail(enchantment));

        // Level options (slots 19-28)
        for (int level = 1; level <= Math.min(enchantment.getMaxLevel(), 10); level++) {
            int slot = 18 + level;
            inventory.setItem(slot, createLevelOption(enchantment, level, heldItem, player.getLevel()));
        }

        // Navigation
        inventory.setItem(45, createBackButton());
        inventory.setItem(49, createLevelInfoItem(enchantment, player.getLevel()));
        inventory.setItem(52, createCloseButton());

        // Fillers
        for (int i = 46; i <= 48; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, createGlassPane(Material.GRAY_STAINED_GLASS_PANE));
            }
        }
        for (int i = 50; i <= 51; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, createGlassPane(Material.GRAY_STAINED_GLASS_PANE));
            }
        }
        for (int i = 53; i < 54; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, createGlassPane(Material.GRAY_STAINED_GLASS_PANE));
            }
        }

        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.0F);
    }

    // ==================== ITEM CREATORS ====================

    private ItemStack createItemDisplay(ItemStack item, int playerLevel) {
        if (item == null || item.getType() == Material.AIR) {
            return createNamedItem(
                Material.BARRIER,
                ChatColor.RED + "" + ChatColor.BOLD + "No Item",
                List.of(
                    "",
                    ChatColor.GRAY + "Hold an item in your",
                    ChatColor.GRAY + "main hand to enchant.",
                    "",
                    ChatColor.RED + "No item selected!"
                )
            );
        }

        ItemStack display = item.clone();
        ItemMeta meta = display.getItemMeta();
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

        // Add enchantment info lore
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8&m---------------------"));
        lore.add("");
        lore.add(ChatColor.AQUA + "" + ChatColor.BOLD + "Enchantment Info:");
        lore.add(ChatColor.GRAY + "Available enchantments: " + ChatColor.GREEN + EnchantmentRegistry.getNormalForItem(item).size());
        lore.add(ChatColor.GRAY + "Your level: " + ChatColor.YELLOW + playerLevel);
        lore.add("");
        lore.add(ChatColor.YELLOW + "" + ChatColor.BOLD + "Click an enchantment →");
        lore.add(ChatColor.GRAY + "Select a level to apply!");
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8&m---------------------"));

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        display.setItemMeta(meta);
        return display;
    }

    private ItemStack createEnchantmentOption(SkyblockEnchantment enchantment, ItemStack item, int playerLevel) {
        int currentLevel = getCurrentEnchantLevel(item, enchantment);
        boolean canApply = currentLevel < enchantment.getMaxLevel();
        boolean canAfford = playerLevel >= enchantment.getXpCost(1);

        ItemStack stack = new ItemStack(enchantment.getIcon());
        ItemMeta meta = stack.getItemMeta();

        // Skyblock-accurate name formatting
        meta.setDisplayName(enchantment.getType().getColor() + "" + ChatColor.BOLD + enchantment.getName());

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Type: " + enchantment.getType().getDisplayName());
        lore.add(ChatColor.GRAY + "Category: " + formatCategory(enchantment.getCategory()));
        lore.add("");
        lore.add(ChatColor.GRAY + "Max Level: " + ChatColor.AQUA + enchantment.getMaxLevel());
        lore.add(ChatColor.GRAY + "Current Level: " + (currentLevel > 0 ? ChatColor.GREEN.toString() + currentLevel : ChatColor.RED + "None"));
        lore.add(ChatColor.GRAY + "XP Cost: " + ChatColor.AQUA + enchantment.getXpCost(1) + ChatColor.GRAY + " per level");
        lore.add("");

        // Description
        if (!enchantment.getDescription().isEmpty()) {
            lore.add(ChatColor.GRAY + "Description:");
            for (String line : enchantment.getDescription()) {
                lore.add(ChatColor.DARK_GRAY + "• " + ChatColor.GRAY + line);
            }
            lore.add("");
        }

        // Status
        if (!canApply) {
            lore.add(ChatColor.RED + "" + ChatColor.BOLD + "Already at max level!");
        } else if (!canAfford) {
            lore.add(ChatColor.RED + "" + ChatColor.BOLD + "Not enough levels!");
            lore.add(ChatColor.GRAY + "Need " + ChatColor.RED + (enchantment.getXpCost(1) - playerLevel) + ChatColor.GRAY + " more levels");
        } else {
            lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Click to view levels!");
        }

        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8&l➤ " + ChatColor.GRAY + "Right-click to select"));

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(enchantIdKey, PersistentDataType.STRING, enchantment.getId());
        stack.setItemMeta(meta);

        return stack;
    }

    private ItemStack createEnchantmentDetail(SkyblockEnchantment enchantment) {
        ItemStack stack = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = stack.getItemMeta();

        meta.setDisplayName(enchantment.getType().getColor() + "" + ChatColor.BOLD + enchantment.getName());

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Type: " + enchantment.getType().getDisplayName());
        lore.add(ChatColor.GRAY + "Category: " + formatCategory(enchantment.getCategory()));
        lore.add(ChatColor.GRAY + "Max Level: " + ChatColor.AQUA + enchantment.getMaxLevel());
        lore.add("");

        // Description
        lore.add(ChatColor.GOLD + "" + ChatColor.BOLD + "Description:");
        for (String line : enchantment.getDescription()) {
            lore.add(ChatColor.GRAY + line);
        }
        lore.add("");

        // Conflicts
        if (!enchantment.getConflictsWith().isEmpty()) {
            lore.add(ChatColor.RED + "" + ChatColor.BOLD + "Conflicts:");
            for (String conflict : enchantment.getConflictsWith()) {
                SkyblockEnchantment conflictEnchant = EnchantmentRegistry.get(conflict);
                if (conflictEnchant != null) {
                    lore.add(ChatColor.DARK_RED + "• " + conflictEnchant.getName());
                }
            }
            lore.add("");
        }

        // Ultimate/Dungeon tags
        if (enchantment.isUltimate()) {
            lore.add(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "ULTIMATE ENCHANTMENT");
            lore.add(ChatColor.GRAY + "Only one ultimate per item!");
            lore.add("");
        }

        if (enchantment.isDungeon()) {
            lore.add(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "🏰 DUNGEON ENCHANTMENT");
            lore.add(ChatColor.GRAY + "Only obtainable in dungeons!");
            lore.add("");
        }

        lore.add(ChatColor.YELLOW + "" + ChatColor.BOLD + "Select a level below →");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        stack.setItemMeta(meta);

        return stack;
    }

    private ItemStack createLevelOption(SkyblockEnchantment enchantment, int level, ItemStack item, int playerLevel) {
        int currentLevel = getCurrentEnchantLevel(item, enchantment);
        boolean alreadyHas = currentLevel >= level;
        int xpCost = enchantment.getXpCost(level);
        boolean canAfford = playerLevel >= xpCost;

        ChatColor color = alreadyHas ? ChatColor.GRAY : (canAfford ? ChatColor.GREEN : ChatColor.RED);
        String romanLevel = toRoman(level);

        ItemStack stack = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = stack.getItemMeta();

        meta.setDisplayName(color + "" + ChatColor.BOLD + "Level " + romanLevel);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Cost: " + ChatColor.AQUA + xpCost + " levels");
        lore.add(ChatColor.GRAY + "Your Level: " + (canAfford ? ChatColor.GREEN : ChatColor.RED) + playerLevel);
        lore.add("");

        if (alreadyHas) {
            lore.add(ChatColor.RED + "" + ChatColor.BOLD + "Already have this level!");
        } else if (!canAfford) {
            lore.add(ChatColor.RED + "" + ChatColor.BOLD + "Not enough levels!");
            lore.add(ChatColor.GRAY + "Need " + ChatColor.RED + (xpCost - playerLevel) + ChatColor.GRAY + " more levels");
        } else {
            lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Click to apply!");
        }

        lore.add("");
        lore.add(ChatColor.GRAY + "Enchantment: " + enchantment.getType().getColor() + enchantment.getName());
        lore.addAll(enchantment.getFormattedLore(level));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8&l➤ " + ChatColor.GRAY + "Click to enchant"));

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(enchantIdKey, PersistentDataType.STRING, enchantment.getId());
        meta.getPersistentDataContainer().set(enchantLevelKey, PersistentDataType.INTEGER, level);
        stack.setItemMeta(meta);

        return stack;
    }

    private ItemStack createPrevPageButton(boolean enabled) {
        return createButton(
            Material.ARROW,
            ChatColor.YELLOW + "" + ChatColor.BOLD + "◄ Previous",
            enabled ? 
                List.of("", ChatColor.GRAY + "Go to previous page.") :
                List.of("", ChatColor.RED + "Already on first page."),
            "prev_page",
            enabled
        );
    }

    private ItemStack createNextPageButton(boolean enabled) {
        return createButton(
            Material.ARROW,
            ChatColor.YELLOW + "" + ChatColor.BOLD + "Next ►",
            enabled ?
                List.of("", ChatColor.GRAY + "Go to next page.") :
                List.of("", ChatColor.RED + "Already on last page."),
            "next_page",
            enabled
        );
    }

    private ItemStack createPageInfoItem(int page, int totalPages, int totalEnchants, int playerLevel) {
        ItemStack stack = new ItemStack(Material.BOOK);
        ItemMeta meta = stack.getItemMeta();

        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Page " + (page + 1) + "/" + totalPages);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Total Enchantments: " + ChatColor.GREEN + totalEnchants);
        lore.add(ChatColor.GRAY + "Your Level: " + ChatColor.YELLOW + playerLevel);
        lore.add("");
        lore.add(ChatColor.GRAY + "Browse available enchantments");
        lore.add(ChatColor.GRAY + "for your currently held item.");
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8&l➤ " + ChatColor.GRAY + "Use arrows to navigate"));

        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createInfoBook() {
        ItemStack stack = new ItemStack(Material.BOOKSHELF);
        ItemMeta meta = stack.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Enchanting Guide");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Welcome to Enchanting!");
        lore.add("");
        lore.add(ChatColor.AQUA + "" + ChatColor.BOLD + "How to Use:");
        lore.add(ChatColor.DARK_GRAY + "• " + ChatColor.GRAY + "Hold an item in main hand");
        lore.add(ChatColor.DARK_GRAY + "• " + ChatColor.GRAY + "Select an enchantment");
        lore.add(ChatColor.DARK_GRAY + "• " + ChatColor.GRAY + "Choose desired level");
        lore.add(ChatColor.DARK_GRAY + "• " + ChatColor.GRAY + "Pay XP to apply");
        lore.add("");
        lore.add(ChatColor.GRAY + "Items can be enchanted");
        lore.add(ChatColor.GRAY + "even if already enchanted!");
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8&l➤ " + ChatColor.GRAY + "Compatible with all items"));

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createEnchantCenterInfo(int playerLevel) {
        ItemStack stack = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = stack.getItemMeta();

        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Player Info");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Your Enchanting Level: " + ChatColor.YELLOW + playerLevel);
        lore.add("");
        lore.add(ChatColor.GRAY + "Higher level = More enchant");
        lore.add(ChatColor.GRAY + "options and higher tiers.");
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8&l➤ " + ChatColor.GRAY + "Gain XP by mining, fighting,"));
        lore.add(ChatColor.GRAY + "fishing, and completing quests!");

        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createBackButton() {
        return createButton(
            Material.ARROW,
            ChatColor.YELLOW + "" + ChatColor.BOLD + "◄ Back",
            List.of("", ChatColor.GRAY + "Return to enchantment list."),
            "back",
            true
        );
    }

    private ItemStack createEnchantGuideItem(int playerLevel) {
        ItemStack stack = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = stack.getItemMeta();

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6&l✨ Enchantment Guide"));

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Learn about enchanting!"));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&e&lGuide:"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8  • &7Select an enchantment"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8  • &7Choose your level"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8  • &7Pay XP to apply"));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&a&lTips:"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8  • &7Higher levels cost more XP"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8  • &7Some enchantments conflict"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8  • &7Ultimate enchants are unique"));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Current Level: " + ChatColor.YELLOW + playerLevel));

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createCloseButton() {
        return createButton(
            Material.BARRIER,
            ChatColor.RED + "" + ChatColor.BOLD + "Close",
            List.of("", ChatColor.GRAY + "Close the enchantment table."),
            "close",
            true
        );
    }

    private ItemStack createLevelInfoItem(SkyblockEnchantment enchantment, int playerLevel) {
        ItemStack stack = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = stack.getItemMeta();

        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Level Selection");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Enchantment: " + enchantment.getType().getColor() + enchantment.getName());
        lore.add(ChatColor.GRAY + "Max Level: " + ChatColor.YELLOW + enchantment.getMaxLevel());
        lore.add(ChatColor.GRAY + "Your Level: " + ChatColor.YELLOW + playerLevel);
        lore.add("");
        lore.add(ChatColor.GRAY + "Select a level to apply");
        lore.add(ChatColor.GRAY + "the enchantment to your item.");
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8&l➤ " + ChatColor.GRAY + "Higher levels cost more XP"));

        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createButton(Material material, String name, List<String> lore, String action, boolean enabled) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();

        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        if (!enabled) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createGlassPane(Material material) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(" ");
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createNamedItem(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private String formatCategory(EnchantmentCategory category) {
        StringBuilder result = new StringBuilder();
        String name = category.name();
        String[] parts = name.split("_");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) result.append(" ");
            result.append(parts[i].charAt(0)).append(parts[i].substring(1).toLowerCase());
        }
        return result.toString();
    }

    private int getCurrentEnchantLevel(ItemStack item, SkyblockEnchantment enchantment) {
        return enchantStorage.getLevel(item, enchantment);
    }

    private String toRoman(int number) {
        if (number <= 0 || number > 10) {
            return String.valueOf(number);
        }
        String[] roman = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return roman[number];
    }

    private boolean applyEnchant(Player player, SkyblockEnchantment enchantment, int level) {
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || heldItem.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Hold an item in your main hand to enchant.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            return false;
        }

        if (heldItem.getAmount() != 1) {
            player.sendMessage(ChatColor.RED + "You can only enchant one item at a time.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            return false;
        }

        if (!enchantment.canEnchantItem(heldItem)) {
            player.sendMessage(ChatColor.RED + "That enchantment cannot be applied to this item.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            return false;
        }

        if (level < 1 || level > enchantment.getMaxLevel()) {
            player.sendMessage(ChatColor.RED + "Invalid level selected.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            return false;
        }

        int currentLevel = enchantStorage.getLevel(heldItem, enchantment);
        if (currentLevel >= level) {
            player.sendMessage(ChatColor.RED + "Your item already has that level or higher.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            return false;
        }

        SkyblockEnchantment conflicting = findConflictingEnchant(heldItem, enchantment);
        if (conflicting != null) {
            player.sendMessage(ChatColor.RED + "Conflicts with " + conflicting.getName() + "!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            return false;
        }

        if (enchantment.isUltimate()) {
            SkyblockEnchantment existingUltimate = enchantStorage.findFirstUltimateEnchant(heldItem);
            if (existingUltimate != null && !existingUltimate.getId().equals(enchantment.getId())) {
                player.sendMessage(ChatColor.LIGHT_PURPLE + "Only one ultimate enchant can be applied to an item!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                return false;
            }
        }

        int cost = enchantment.getXpCost(level);
        if (player.getLevel() < cost) {
            player.sendMessage(ChatColor.RED + "Not enough levels! Need " + cost + " levels.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            return false;
        }

        ItemStack updated = enchantStorage.apply(heldItem, enchantment, level);
        player.getInventory().setItemInMainHand(updated);
        player.updateInventory();
        player.setLevel(player.getLevel() - cost);
        
        ((GriviencePlugin) plugin).getSkyblockLevelManager().recordEnchanting(player, cost);

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.2F);
        player.sendMessage(ChatColor.GREEN + "Applied " + enchantment.getName() + " " + toRoman(level) + "!");
        return true;
    }

    private SkyblockEnchantment findConflictingEnchant(ItemStack item, SkyblockEnchantment enchantment) {
        for (SkyblockEnchantment existing : EnchantmentRegistry.getAll()) {
            if (existing.getId().equals(enchantment.getId())) {
                continue;
            }
            if (enchantStorage.getLevel(item, existing) <= 0) {
                continue;
            }
            if (enchantment.conflictsWith(existing)) {
                return existing;
            }
        }
        return null;
    }

    // ==================== HOLDERS ====================

    private static class EnchantmentTableHolder implements InventoryHolder {
        private Inventory inventory;
        private final ItemStack heldItem;
        private int page = 0;
        private int totalPages = 1;
        private int totalEnchantments = 0;

        public EnchantmentTableHolder(ItemStack heldItem) {
            this.heldItem = heldItem;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static class EnchantmentLevelHolder implements InventoryHolder {
        private Inventory inventory;
        private final SkyblockEnchantment enchantment;
        private final ItemStack heldItem;
        private final int returnPage;

        public EnchantmentLevelHolder(SkyblockEnchantment enchantment, ItemStack heldItem, int returnPage) {
            this.enchantment = enchantment;
            this.heldItem = heldItem;
            this.returnPage = returnPage;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
