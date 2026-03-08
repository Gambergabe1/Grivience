package io.papermc.Grivience.bazaar;

import net.wesjd.anvilgui.AnvilGUI;
import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.*;

/**
 * Skyblock 100% Accurate Bazaar GUI System.
 * Matches the exact layout, styling, colors, and behavior of Skyblock's bazaar.
 * 
 * Features:
 * - Exact slot positions matching Skyblock
 * - Correct color codes (§5 for bazaar theme, §6 for coins, §a/§c for buy/sell)
 * - Proper lore structure and formatting
 * - Accurate category icons and layout
 * - Correct navigation buttons and positions
 * - Skyblock-style instant buy/sell buttons
 * - Order placement with sign GUI
 */
public final class BazaarGuiManager implements Listener {
    // Skyblock-accurate inventory titles
    private static final String TITLE_MAIN = ChatColor.translateAlternateColorCodes('&', "&8Bazaar");
    private static final String TITLE_CATEGORY = ChatColor.translateAlternateColorCodes('&', "&8Bazaar - ");
    private static final String TITLE_PRODUCT = ChatColor.translateAlternateColorCodes('&', "&8Bazaar - ");
    private static final String TITLE_ORDERS = ChatColor.translateAlternateColorCodes('&', "&8Your Orders");
    private static final String TITLE_BAG = ChatColor.translateAlternateColorCodes('&', "&8Bazaar Stash");

    // Skyblock-accurate slot positions
    private static final int[] CATEGORY_SLOTS = {10, 19, 28, 37, 46};
    private static final int[] PRODUCT_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };

    // Navigation slots (bottom row - Skyblock accurate)
    private static final int BACK_SLOT = 48;
    private static final int PREV_SLOT = 47;
    private static final int INFO_SLOT = 45;
    private static final int MIDDLE_SLOT = 49;
    private static final int NEXT_SLOT = 50;
    // 49 is used for "Your Orders" in the product view; keep Close separate to avoid overwriting.
    private static final int CLOSE_SLOT = 53;

    private static final int[] SUBCATEGORY_SLOTS = {9, 18, 27, 36};

    // Product menu slots (Skyblock accurate)
    private static final int INSTANT_BUY_SLOT = 20;
    private static final int INSTANT_SELL_SLOT = 24;
    private static final int BUY_ORDER_SLOT = 29;
    private static final int SELL_ORDER_SLOT = 33;
    private static final int PRICE_HISTORY_SLOT = 38;
    private static final int ORDER_STATS_SLOT = 40;
    private static final int YOUR_ORDERS_SLOT = 49;

    // Skyblock-style amount tiers used across the bazaar.
    // Hypixel allows up to 71,680 items in a single bazaar transaction.
    private static final int MAX_BAZAAR_AMOUNT = 71680;
    private static final int[] AMOUNT_TIERS = {1, 64, 160, 256, 512, 1024, 71680};
    private static final int[] AMOUNT_TIER_SLOTS = {19, 20, 21, 22, 23, 24, 25};
    private static final int CUSTOM_AMOUNT_SLOT = 31;

    // Order creation menus.
    private static final int ORDER_PRICE_BEST_SLOT = 20;
    private static final int ORDER_PRICE_MARKET_SLOT = 22;
    private static final int ORDER_PRICE_CUSTOM_SLOT = 24;
    private static final int CONFIRM_SLOT = 15;
    private static final int CANCEL_SLOT = 11;
    private static final int CONFIRM_PRODUCT_SLOT = 13;

    private final GriviencePlugin plugin;
    private final BazaarShopManager shopManager;

    public BazaarGuiManager(GriviencePlugin plugin, BazaarShopManager shopManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
    }

    /**
     * Open the main Bazaar menu - 100% Skyblock-accurate.
     */
    public void openMain(Player player) {
        if (!shopManager.isEnabled()) {
            player.sendMessage(ChatColor.RED + "Bazaar is currently unavailable.");
            return;
        }
        if (!isBazaarUnlocked(player)) {
            return;
        }

        Inventory inventory = Bukkit.createInventory(new MainHolder(), 54, TITLE_MAIN);

        fillBazaarFrame(inventory);

        // Header/Title item (slot 4 - center top)
        setItem(inventory, 4, createMainHeader(player));

        // Categories in grid (Skyblock-accurate positions)
        BazaarProduct.BazaarCategory[] categories = BazaarProduct.BazaarCategory.values();
        for (int i = 0; i < Math.min(categories.length, CATEGORY_SLOTS.length); i++) {
            BazaarProduct.BazaarCategory category = categories[i];
            List<BazaarProduct> products = shopManager.getProductsByCategory(category);
            if (products.isEmpty()) continue;
            setItem(inventory, CATEGORY_SLOTS[i], createCategoryItem(category, products.size()));
        }

        // Bottom row navigation (Skyblock-accurate)
        setItem(inventory, BACK_SLOT, createButtonItem(
            Material.ARROW,
            "&e← Back",
            new String[]{"&7Return to previous menu."},
            "back"
        ));

        setItem(inventory, PREV_SLOT, createButtonItem(
            Material.GRAY_STAINED_GLASS_PANE,
            "&7Previous Page",
            new String[]{"&8No previous page."},
            "noop"
        ));

        setItem(inventory, 52, createStashNavButton());

        setItem(inventory, INFO_SLOT, createButtonItem(
            Material.ANVIL,
            "&aSearch",
            new String[]{"&7Search for an item to trade.", "&7Click to enter a query."},
            "search"
        ));

        setItem(inventory, MIDDLE_SLOT, createBazaarNavButton());

        setItem(inventory, 51, createOrdersNavButton());

        setItem(inventory, NEXT_SLOT, createButtonItem(
            Material.GRAY_STAINED_GLASS_PANE,
            "&7Next Page",
            new String[]{"&8No more pages."},
            "noop"
        ));

        setItem(inventory, CLOSE_SLOT, createButtonItem(
            Material.BARRIER,
            "&cClose",
            new String[]{"&7Close the bazaar."},
            "close"
        ));

        player.openInventory(inventory);
        playOpenSound(player);
    }

    private boolean isBazaarUnlocked(Player player) {
        if (player == null) {
            return false;
        }
        var levelManager = plugin.getSkyblockLevelManager();
        if (levelManager == null) {
            return true;
        }

        int level = levelManager.getLevel(player);
        if (level >= 7) {
            return true;
        }

        player.sendMessage(ChatColor.RED + "You must be SkyBlock Level 7 to access the Bazaar.");
        player.sendMessage(ChatColor.GRAY + "Current Level: " + ChatColor.YELLOW + level);
        return false;
    }

    /**
     * Open category menu showing products - Skyblock-accurate.
     */
    public void openCategory(Player player, BazaarProduct.BazaarCategory category, int page) {
        if (!isBazaarUnlocked(player)) {
            return;
        }
        openCategory(player, category, null, page);
    }

    private void openCategory(Player player, BazaarProduct.BazaarCategory category, BazaarProduct.BazaarSubcategory subcategory, int page) {
        List<BazaarProduct> products = shopManager.getProductsByCategory(category).stream()
                .filter(p -> !p.isVariant()) // Only show top-level products
                .toList();
        
        if (subcategory != null) {
            products = products.stream()
                    .filter(p -> p != null && p.getSubcategory() == subcategory)
                    .toList();
        }
        int pageSize = PRODUCT_SLOTS.length;
        int totalPages = Math.max(1, (products.size() + pageSize - 1) / pageSize);
        int currentPage = Math.max(0, Math.min(page, totalPages - 1));

        String title = TITLE_CATEGORY + category.getDisplayName() + " §8(" + (currentPage + 1) + "/" + totalPages + ")";
        Inventory inventory = Bukkit.createInventory(new CategoryHolder(category, subcategory, currentPage), 54, title);

        fillBazaarFrame(inventory);

        // Category header (slot 4)
        setItem(inventory, 4, createCategoryHeader(category, subcategory, products.size()));

        // Subcategory selector (left column)
        List<BazaarProduct.BazaarSubcategory> subcategories = getSubcategories(category);
        for (int i = 0; i < Math.min(subcategories.size(), SUBCATEGORY_SLOTS.length); i++) {
            BazaarProduct.BazaarSubcategory sub = subcategories.get(i);
            setItem(inventory, SUBCATEGORY_SLOTS[i], createSubcategoryItem(sub, sub == subcategory));
        }

        // Products in grid
        int start = currentPage * pageSize;
        int end = Math.min(start + pageSize, products.size());
        int slotIndex = 0;

        for (int i = start; i < end && slotIndex < PRODUCT_SLOTS.length; i++) {
            BazaarProduct product = products.get(i);
            product = shopManager.getProduct(product.getProductId()); // Refresh data
            setItem(inventory, PRODUCT_SLOTS[slotIndex], createProductItem(product));
            slotIndex++;
        }

        // Navigation (Skyblock-accurate bottom row)
        setItem(inventory, BACK_SLOT, createBackButton());
        setItem(inventory, PREV_SLOT, createPrevPageButton(currentPage > 0));
        setItem(inventory, 52, createStashNavButton());
        setItem(inventory, INFO_SLOT, createButtonItem(Material.ANVIL, "&aSearch", new String[]{"&7Search for an item to trade.", "&7Click to enter a query."}, "search"));
        setItem(inventory, MIDDLE_SLOT, createBazaarNavButton());
        setItem(inventory, NEXT_SLOT, createNextPageButton(currentPage < totalPages - 1));
        setItem(inventory, 51, createOrdersNavButton());
        setItem(inventory, CLOSE_SLOT, createCloseButton());

        player.openInventory(inventory);
        playOpenSound(player);
    }

    /**
     * Open menu showing variants of a product - Skyblock-accurate.
     */
    public void openVariantsMenu(Player player, BazaarProduct parent, BazaarProduct.BazaarCategory category, BazaarProduct.BazaarSubcategory subcategory, int page) {
        if (!isBazaarUnlocked(player) || parent == null) {
            return;
        }
         
        BazaarProduct.BazaarCategory finalCategory = category != null ? category : parent.getCategory();
        if (finalCategory == null) finalCategory = BazaarProduct.BazaarCategory.ODDITIES; // Absolute fallback

        String title = "§8Bazaar - " + parent.getProductName();
        Inventory inventory = Bukkit.createInventory(new VariantsHolder(parent, finalCategory, subcategory, page), 54, title);

        fillBazaarFrame(inventory);

        // Header
        List<BazaarProduct> variants = new ArrayList<>();
        variants.add(parent);
        variants.addAll(parent.getVariants());
        
        setItem(inventory, 4, createCategoryHeader(finalCategory, subcategory, variants.size()));

        // Centered slots based on count
        int[] variantSlots;
        if (variants.size() == 1) {
            variantSlots = new int[]{22};
        } else if (variants.size() == 2) {
            variantSlots = new int[]{21, 23};
        } else if (variants.size() == 3) {
            variantSlots = new int[]{20, 22, 24};
        } else if (variants.size() == 4) {
            variantSlots = new int[]{19, 21, 23, 25};
        } else {
            variantSlots = new int[]{19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        }

        for (int i = 0; i < Math.min(variants.size(), variantSlots.length); i++) {
            BazaarProduct v = variants.get(i);
            if (v == null) continue;
            // Ensure we have the freshest data from shop manager map
            BazaarProduct freshV = shopManager.getProduct(v.getProductId());
            setItem(inventory, variantSlots[i], createProductItem(freshV != null ? freshV : v));
        }

        // Navigation
        setItem(inventory, BACK_SLOT, createBackButton());
        setItem(inventory, 52, createStashNavButton());
        setItem(inventory, INFO_SLOT, createButtonItem(Material.ANVIL, "&aSearch", new String[]{"&7Search for an item to trade.", "&7Click to enter a query."}, "search"));
        setItem(inventory, MIDDLE_SLOT, createBazaarNavButton());
        setItem(inventory, 51, createOrdersNavButton());
        setItem(inventory, CLOSE_SLOT, createCloseButton());

        player.openInventory(inventory);
        playOpenSound(player);
    }

    /**
     * Open product detail menu for trading - Skyblock-accurate layout.
     */
    public void openProduct(Player player, BazaarProduct product) {
        if (!isBazaarUnlocked(player) || product == null) {
            return;
        }
        openProduct(player, product, product.getCategory(), null, 0, null, 0);
    }

    private void openProduct(Player player, BazaarProduct product, BazaarProduct.BazaarCategory category, BazaarProduct.BazaarSubcategory subcategory, int page) {
        openProduct(player, product, category, subcategory, page, null, 0);
    }

    private void openProduct(
            Player player,
            BazaarProduct product,
            BazaarProduct.BazaarCategory category,
            BazaarProduct.BazaarSubcategory subcategory,
            int page,
            String searchQuery,
            int searchPage
    ) {
        product = shopManager.getProduct(product.getProductId()); // Refresh

        String title = TITLE_PRODUCT + product.getProductName();
        Inventory inventory = Bukkit.createInventory(new ProductHolder(product, category, subcategory, page, searchQuery, searchPage), 54, title);

        fillBazaarFrame(inventory);

        // Product display (center top - slot 13)
        ItemStack productDisplay = createProductDisplayItem(product);
        setItem(inventory, 13, productDisplay);

        // Instant Buy button (slot 20 - left side)
        setItem(inventory, INSTANT_BUY_SLOT, createInstantBuyButton(product));

        // Instant Sell button (slot 24 - right side)
        setItem(inventory, INSTANT_SELL_SLOT, createInstantSellButton(product));

        // Buy Order button (slot 29 - left side)
        setItem(inventory, BUY_ORDER_SLOT, createBuyOrderButton(product));

        // Sell Order button (slot 33 - right side)
        setItem(inventory, SELL_ORDER_SLOT, createSellOrderButton(product));

        // Price history (slot 38)
        setItem(inventory, PRICE_HISTORY_SLOT, createPriceHistoryButton(product));

        // Order statistics (slot 40)
        setItem(inventory, ORDER_STATS_SLOT, createOrderStatsButton(product));

        // Back button (slot 45)
        setItem(inventory, BACK_SLOT, createBackToCategoryButton(category));

        // Bottom navigation (Skyblock-style)
        setItem(inventory, 52, createStashNavButton());
        setItem(inventory, INFO_SLOT, createButtonItem(Material.ANVIL, "&aSearch", new String[]{"&7Search for an item to trade.", "&7Click to enter a query."}, "search"));

        // Your orders for this product (slot 49)
        setItem(inventory, YOUR_ORDERS_SLOT, createYourOrdersButton(player, product));

        setItem(inventory, 51, createOrdersNavButton());

        // Close button (slot 52)
        setItem(inventory, CLOSE_SLOT, createCloseButton());

        player.openInventory(inventory);
        playOpenSound(player);
    }

    private void openInstantAmountMenu(
            Player player,
            BazaarProduct product,
            boolean isBuy,
            BazaarProduct.BazaarCategory category,
            BazaarProduct.BazaarSubcategory subcategory,
            int page,
            String searchQuery,
            int searchPage
    ) {
        if (player == null || product == null) {
            return;
        }

        product = shopManager.getProduct(product.getProductId());
        if (product == null) {
            return;
        }

        String title = ChatColor.translateAlternateColorCodes(
                '&',
                "&8Bazaar - " + (isBuy ? "Instant Buy" : "Instant Sell")
        );
        Inventory inventory = Bukkit.createInventory(
                new InstantAmountHolder(product.getProductId(), isBuy, category, subcategory, page, searchQuery, searchPage),
                54,
                title
        );

        fillBazaarFrame(inventory);

        setItem(inventory, 4, createInstantAmountHeader(player, product, isBuy));
        setItem(inventory, 13, createProductDisplayItem(product));

        for (int i = 0; i < Math.min(AMOUNT_TIERS.length, AMOUNT_TIER_SLOTS.length); i++) {
            setItem(inventory, AMOUNT_TIER_SLOTS[i], createInstantAmountTierButton(player, product, AMOUNT_TIERS[i], isBuy));
        }

        setItem(inventory, CUSTOM_AMOUNT_SLOT, createCustomAmountButton(isBuy, "instant_custom_amount"));

        setItem(inventory, BACK_SLOT, createBackButton());
        setItem(inventory, 52, createStashNavButton());
        setItem(inventory, INFO_SLOT, createButtonItem(Material.ANVIL, "&aSearch", new String[]{"&7Search for an item to trade.", "&7Click to enter a query."}, "search"));
        setItem(inventory, 51, createOrdersNavButton());
        setItem(inventory, CLOSE_SLOT, createCloseButton());

        player.openInventory(inventory);
        playOpenSound(player);
    }

    private void openOrderAmountMenu(
            Player player,
            BazaarProduct product,
            boolean isBuyOrder,
            BazaarProduct.BazaarCategory category,
            BazaarProduct.BazaarSubcategory subcategory,
            int page,
            String searchQuery,
            int searchPage
    ) {
        if (player == null || product == null) {
            return;
        }

        product = shopManager.getProduct(product.getProductId());
        if (product == null) {
            return;
        }

        String title = ChatColor.translateAlternateColorCodes(
                '&',
                "&8Bazaar - " + (isBuyOrder ? "Create Buy Order" : "Create Sell Offer")
        );
        Inventory inventory = Bukkit.createInventory(
                new OrderAmountHolder(product.getProductId(), isBuyOrder, category, subcategory, page, searchQuery, searchPage),
                54,
                title
        );

        fillBazaarFrame(inventory);

        setItem(inventory, 4, createOrderAmountHeader(product, isBuyOrder));
        setItem(inventory, 13, createProductDisplayItem(product));

        for (int i = 0; i < Math.min(AMOUNT_TIERS.length, AMOUNT_TIER_SLOTS.length); i++) {
            setItem(inventory, AMOUNT_TIER_SLOTS[i], createOrderAmountTierButton(product, AMOUNT_TIERS[i], isBuyOrder));
        }

        setItem(inventory, CUSTOM_AMOUNT_SLOT, createCustomAmountButton(isBuyOrder, "order_custom_amount"));

        setItem(inventory, BACK_SLOT, createBackButton());
        setItem(inventory, CLOSE_SLOT, createCloseButton());

        player.openInventory(inventory);
        playOpenSound(player);
    }

    private void openOrderPriceMenu(
            Player player,
            BazaarProduct product,
            int amount,
            boolean isBuyOrder,
            BazaarProduct.BazaarCategory category,
            BazaarProduct.BazaarSubcategory subcategory,
            int page,
            String searchQuery,
            int searchPage
    ) {
        if (player == null || product == null) {
            return;
        }

        product = shopManager.getProduct(product.getProductId());
        if (product == null) {
            return;
        }

        String title = ChatColor.translateAlternateColorCodes('&', "&8Bazaar - Set Price");
        Inventory inventory = Bukkit.createInventory(
                new OrderPriceHolder(product.getProductId(), amount, isBuyOrder, category, subcategory, page, searchQuery, searchPage),
                54,
                title
        );

        fillBazaarFrame(inventory);

        setItem(inventory, 4, createOrderPriceHeader(product, amount, isBuyOrder));
        setItem(inventory, 13, createProductDisplayItem(product));

        setItem(inventory, ORDER_PRICE_BEST_SLOT, createOrderBestPriceButton(product, amount, isBuyOrder));
        setItem(inventory, ORDER_PRICE_MARKET_SLOT, createOrderMarketPriceButton(product, amount, isBuyOrder));
        setItem(inventory, ORDER_PRICE_CUSTOM_SLOT, createCustomPriceButton(isBuyOrder));

        setItem(inventory, BACK_SLOT, createBackButton());
        setItem(inventory, CLOSE_SLOT, createCloseButton());

        player.openInventory(inventory);
        playOpenSound(player);
    }

    private void openOrderConfirmMenu(
            Player player,
            BazaarProduct product,
            int amount,
            double unitPrice,
            boolean isBuyOrder,
            BazaarProduct.BazaarCategory category,
            BazaarProduct.BazaarSubcategory subcategory,
            int page,
            String searchQuery,
            int searchPage
    ) {
        if (player == null || product == null) {
            return;
        }

        product = shopManager.getProduct(product.getProductId());
        if (product == null) {
            return;
        }

        String title = ChatColor.translateAlternateColorCodes('&', "&8Bazaar - Confirm Order");
        Inventory inventory = Bukkit.createInventory(
                new OrderConfirmHolder(product.getProductId(), amount, unitPrice, isBuyOrder, category, subcategory, page, searchQuery, searchPage),
                54,
                title
        );

        fillBazaarFrame(inventory);

        setItem(inventory, 4, createOrderConfirmHeader(player, product, amount, unitPrice, isBuyOrder));
        setItem(inventory, CONFIRM_PRODUCT_SLOT, createOrderConfirmProductItem(player, product, amount, unitPrice, isBuyOrder));
        setItem(inventory, CONFIRM_SLOT, createOrderConfirmButton(isBuyOrder));
        setItem(inventory, CANCEL_SLOT, createOrderCancelButton());

        setItem(inventory, BACK_SLOT, createBackButton());
        setItem(inventory, CLOSE_SLOT, createCloseButton());

        player.openInventory(inventory);
        playOpenSound(player);
    }

    private ItemStack createOrderCancelButton() {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Cancel");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Cancel order creation.");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to go back!");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        addPersistentData(meta, "action", "back");
        item.setItemMeta(meta);
        return item;
    }

    private void openPriceHistoryMenu(
            Player player,
            BazaarProduct product,
            BazaarProduct.BazaarCategory category,
            BazaarProduct.BazaarSubcategory subcategory,
            int page,
            String searchQuery,
            int searchPage
    ) {
        if (player == null || product == null) {
            return;
        }

        product = shopManager.getProduct(product.getProductId());
        if (product == null) {
            return;
        }

        String title = ChatColor.translateAlternateColorCodes('&', "&8Bazaar - Price History");
        Inventory inventory = Bukkit.createInventory(
                new PriceHistoryHolder(product.getProductId(), category, subcategory, page, searchQuery, searchPage),
                54,
                title
        );

        fillBazaarListFrame(inventory);

        List<BazaarPriceHistory.PriceEntry> entries = shopManager.getPriceHistory()
                .getHistory(product.getProductId())
                .getEntries();

        setItem(inventory, 4, createPriceHistoryHeader(product, entries.size()));

        if (entries.isEmpty()) {
            setItem(inventory, 22, createNoPriceHistoryItem());
        } else {
            int slot = 9;
            for (int i = entries.size() - 1; i >= 0 && slot < 45; i--) {
                setItem(inventory, slot++, createPriceHistoryEntryItem(entries.get(i)));
            }
        }

        setItem(inventory, BACK_SLOT, createBackButton());
        setItem(inventory, CLOSE_SLOT, createCloseButton());

        player.openInventory(inventory);
        playOpenSound(player);
    }

    /**
     * Open player's active orders menu - Skyblock-accurate.
     */
    public void openOrders(Player player) {
        if (!isBazaarUnlocked(player)) {
            return;
        }
        openOrders(player, new OrdersHolder());
    }

    private void openOrders(Player player, OrdersHolder holder) {
        UUID profileId = shopManager.requireProfileId(player);
        if (profileId == null) {
            return;
        }

        Inventory inventory = Bukkit.createInventory(holder, 54, TITLE_ORDERS);

        fillBazaarListFrame(inventory);

        // Header
        UUID ownerId = player.getUniqueId();
        List<BazaarOrder> allOrders = shopManager.getOrderBook().getProfileOrders(profileId, ownerId);
        List<BazaarOrder> orders = allOrders;
        if (holder.filterProductId != null && !holder.filterProductId.isBlank()) {
            orders = orders.stream()
                    .filter(o -> o != null && o.getProductId() != null && o.getProductId().equalsIgnoreCase(holder.filterProductId))
                    .toList();
        }

        setItem(inventory, 4, createOrdersHeader(player, orders.size()));

        if (orders.isEmpty()) {
            setItem(inventory, 22, createNoOrdersItem());
        } else {
            int slot = 9;
            for (BazaarOrder order : orders) {
                if (slot >= 45) break;
                BazaarProduct product = shopManager.getProduct(order.getProductId());
                Material icon = product != null ? product.getIcon() : Material.PAPER;
                ItemStack orderItem = order.createDisplayItem(product != null ? product.getProductName() : "Unknown", icon);
                ItemMeta meta = orderItem.getItemMeta();
                if (meta != null) {
                    addPersistentData(meta, "cancel_order", order.getOrderId());
                    orderItem.setItemMeta(meta);
                }
                setItem(inventory, slot, orderItem);
                slot++;
            }
        }

        // Navigation
        setItem(inventory, BACK_SLOT, createBackButton());
        setItem(inventory, 52, createStashNavButton());
        setItem(inventory, INFO_SLOT, createButtonItem(Material.ANVIL, "&aSearch", new String[]{"&7Search for an item to trade.", "&7Click to enter a query."}, "search"));
        setItem(inventory, MIDDLE_SLOT, createOrdersInfoItem(player, allOrders.size()));
        setItem(inventory, CLOSE_SLOT, createCloseButton());

        player.openInventory(inventory);
        playOpenSound(player);
    }

    /**
     * Open shopping bag menu - Skyblock-accurate.
     */
    public void openShoppingBag(Player player) {
        if (!isBazaarUnlocked(player)) {
            return;
        }
        UUID profileId = shopManager.requireProfileId(player);
        if (profileId == null) {
            return;
        }

        BazaarShoppingBag.PlayerShoppingBag bag = shopManager.getShoppingBag().getBag(profileId);
        double bagValue = calculateBagValue(profileId);

        String title = TITLE_BAG + " §8(§6" + shopManager.formatCoins(bagValue) + "§8)";
        Inventory inventory = Bukkit.createInventory(new ShoppingBagHolder(), 54, title);

        fillBazaarListFrame(inventory);

        // Header
        setItem(inventory, 4, createBagHeader(bagValue));

        // Items in bag
        if (bag.isEmpty() && bag.getCoins() <= 0) {
            setItem(inventory, 22, createEmptyBagItem());
        } else {
            int slot = 9;

            // Coins display
            if (bag.getCoins() > 0) {
                setItem(inventory, slot++, createCoinsItem(bag.getCoins()));
            }

            // Items
            for (Map.Entry<String, Integer> entry : bag.getItems().entrySet()) {
                if (slot >= 45) break;
                BazaarProduct product = shopManager.getProduct(entry.getKey());
                if (product == null) continue;
                setItem(inventory, slot++, createBagItem(product, entry.getValue()));
            }
        }

        // Navigation
        setItem(inventory, BACK_SLOT, createBackButton());
        setItem(inventory, INFO_SLOT, createButtonItem(Material.ANVIL, "&aSearch", new String[]{"&7Search for an item to trade.", "&7Click to enter a query."}, "search"));
        setItem(inventory, MIDDLE_SLOT, createClaimAllButton(player));
        setItem(inventory, 51, createOrdersNavButton());
        setItem(inventory, CLOSE_SLOT, createCloseButton());

        player.openInventory(inventory);
        playOpenSound(player);
    }

    // ==================== ITEM CREATORS ====================

    private ItemStack createMainHeader(Player player) {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GREEN + "Bazaar");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Trade items with other players");
        lore.add(ChatColor.GRAY + "instantly or place custom orders.");
        lore.add("");
        UUID profileId = shopManager.getSelectedProfileId(player);
        int orderCount = profileId == null ? 0 : shopManager.getOrderBook().getProfileOrders(profileId, player.getUniqueId()).size();
        int bagItems = profileId == null ? 0 : shopManager.getShoppingBag().getBag(profileId).getItems().size();
        lore.add(ChatColor.GRAY + "Active Orders: " + ChatColor.YELLOW + orderCount + "/" + shopManager.getMaxOrdersAllowed(player));
        lore.add(ChatColor.GRAY + "Items to Claim: " + ChatColor.YELLOW + bagItems);
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click a category to browse!");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSearchHeader(String query, int results) {
        ItemStack item = new ItemStack(Material.ANVIL);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Search Results");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Query: " + ChatColor.YELLOW + query);
        lore.add(ChatColor.GRAY + "Results: " + ChatColor.YELLOW + results);
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click an item to trade!");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNoSearchResultsItem(String query) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "No Results Found");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "No items match:");
        lore.add(ChatColor.YELLOW + query);
        lore.add("");
        lore.add(ChatColor.GRAY + "Try a different query.");
        lore.add(ChatColor.YELLOW + "Click Search to try again!");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCategoryHeader(BazaarProduct.BazaarCategory category, int productCount) {
        return createCategoryHeader(category, null, productCount);
    }

    private ItemStack createCategoryHeader(BazaarProduct.BazaarCategory category, BazaarProduct.BazaarSubcategory subcategory, int productCount) {
        ItemStack item = new ItemStack(category.getIcon());
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(category.getDisplayName());

        List<String> lore = new ArrayList<>();
        lore.add("");
        if (subcategory != null) {
            lore.add(ChatColor.GRAY + "Subcategory: " + ChatColor.YELLOW + subcategory.getDisplayName());
        }
        lore.add(ChatColor.GRAY + "Products: " + ChatColor.GREEN + productCount);
        lore.add("");
        lore.add(ChatColor.GRAY + "Browse items in this category");
        lore.add(ChatColor.GRAY + "and start trading.");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click a product to trade!");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        addPersistentData(meta, "clear_subcategory", category.name());
        item.setItemMeta(meta);
        return item;
    }

    private List<BazaarProduct.BazaarSubcategory> getSubcategories(BazaarProduct.BazaarCategory category) {
        if (category == null) {
            return List.of();
        }
        List<BazaarProduct.BazaarSubcategory> out = new ArrayList<>();
        for (BazaarProduct.BazaarSubcategory sub : BazaarProduct.BazaarSubcategory.values()) {
            if (sub != null && sub.getParent() == category) {
                out.add(sub);
            }
        }
        return out;
    }

    private ItemStack createSubcategoryItem(BazaarProduct.BazaarSubcategory subcategory, boolean selected) {
        Material icon = iconForSubcategory(subcategory);
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName((selected ? ChatColor.GREEN : ChatColor.YELLOW) + subcategory.getDisplayName());

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Filter the bazaar list");
        lore.add(ChatColor.GRAY + "to this subcategory.");
        lore.add("");
        if (selected) {
            lore.add(ChatColor.GREEN + "SELECTED");
        } else {
            lore.add(ChatColor.YELLOW + "Click to view!");
        }

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        if (selected) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        }
        addPersistentData(meta, "open_subcategory", subcategory.name());
        item.setItemMeta(meta);
        return item;
    }

    private Material iconForSubcategory(BazaarProduct.BazaarSubcategory subcategory) {
        if (subcategory == null) {
            return Material.PAPER;
        }
        return switch (subcategory) {
            case FARMING_ITEMS -> Material.HAY_BLOCK;
            case MUSHROOMS -> Material.RED_MUSHROOM;
            case ANIMAL -> Material.LEATHER;
            case ORE -> Material.IRON_ORE;
            case GEMS -> Material.EMERALD;
            case FUEL -> Material.COAL;
            case SPECIAL -> Material.AMETHYST_SHARD;
            case MOB_DROPS -> Material.ROTTEN_FLESH;
            case DUNGEON -> Material.SKELETON_SKULL;
            case SPECIAL_COMBAT -> Material.BLAZE_ROD;
            case WOOD -> Material.OAK_LOG;
            case SAPLING -> Material.OAK_SAPLING;
            case SPECIAL_FORAGING -> Material.STICK;
            case FISH -> Material.COD;
            case TREASURE -> Material.HEART_OF_THE_SEA;
            case SPECIAL_FISHING -> Material.PRISMARINE_CRYSTALS;
            case MAGIC -> Material.ENCHANTED_BOOK;
            case CONSUMABLES -> Material.GOLDEN_CARROT;
            case MISC -> Material.SLIME_BALL;
        };
    }

    private ItemStack createCategoryItem(BazaarProduct.BazaarCategory category, int productCount) {
        ItemStack item = new ItemStack(category.getIcon());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(category.getDisplayName());
        List<String> lore = new ArrayList<>();
        
        String cleanName = ChatColor.stripColor(category.getDisplayName()).toLowerCase();
        lore.add(ChatColor.GRAY + "Browse " + cleanName + " items.");
        
        List<BazaarProduct> categoryProducts = shopManager.getProductsByCategory(category);
        lore.add(ChatColor.GRAY + "Products: " + ChatColor.YELLOW + categoryProducts.size());

        int buyOrders = 0;
        int sellOrders = 0;
        double buyVolume = 0.0D;
        double sellVolume = 0.0D;
        for (BazaarProduct product : categoryProducts) {
            if (product == null) {
                continue;
            }
            buyOrders += product.getBuyOrderCount();
            sellOrders += product.getSellOrderCount();
            buyVolume += Math.max(0.0D, product.getBuyOrderVolume());
            sellVolume += Math.max(0.0D, product.getSellOrderVolume());
        }

        lore.add(ChatColor.GRAY + "Buy Orders: " + ChatColor.YELLOW + buyOrders);
        lore.add(ChatColor.GRAY + "Sell Offers: " + ChatColor.YELLOW + sellOrders);
        lore.add(ChatColor.GRAY + "Buy Volume: " + shopManager.formatCoins(buyVolume));
        lore.add(ChatColor.GRAY + "Sell Volume: " + shopManager.formatCoins(sellVolume));
        
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to browse!");
        
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        // Set action to open category
        meta.getPersistentDataContainer().set(BazaarKeys.ACTION_KEY, PersistentDataType.STRING, "open_category");
        // Set category name as value
        meta.getPersistentDataContainer().set(BazaarKeys.VALUE_KEY, PersistentDataType.STRING, category.name());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createProductItem(BazaarProduct product) {
        ItemStack item = product.createDisplayItem();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Re-enforce PDC keys for reliability
            meta.getPersistentDataContainer().set(BazaarKeys.ACTION_KEY, PersistentDataType.STRING, "open_product");
            meta.getPersistentDataContainer().set(BazaarKeys.VALUE_KEY, PersistentDataType.STRING, product.getProductId());
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createInstantAmountHeader(Player player, BazaarProduct product, boolean isBuy) {
        ItemStack item = new ItemStack(isBuy ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName((isBuy ? ChatColor.GREEN : ChatColor.RED) + "" + ChatColor.BOLD + (isBuy ? "Instant Buy" : "Instant Sell"));

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Item: " + ChatColor.YELLOW + product.getProductName());
        lore.add("");
        lore.add(ChatColor.GRAY + "Select an amount to " + (isBuy ? "buy" : "sell") + ".");
        lore.add(ChatColor.GRAY + "Items/coins will go to your");
        lore.add(ChatColor.GRAY + "Bazaar Stash.");
        if (!isBuy) {
            double taxRate = shopManager.getSellTaxRate(player);
            lore.add(ChatColor.GRAY + "Bazaar Tax: " + ChatColor.RED + String.format(Locale.ROOT, "%.2f%%", taxRate * 100.0D));
        }
        lore.add("");
        lore.add(ChatColor.YELLOW + "Choose an amount below!");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createInstantAmountTierButton(Player player, BazaarProduct product, int amount, boolean isBuy) {
        ItemStack item = new ItemStack(isBuy ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName((isBuy ? ChatColor.GREEN : ChatColor.RED) + (isBuy ? "Buy " : "Sell ") + ChatColor.YELLOW + amount + "x");

        List<String> lore = new ArrayList<>();
        lore.add("");
        double unit = isBuy ? product.getInstantBuyPrice() : product.getInstantSellPrice();
        if (Double.isFinite(unit) && unit > 0.0) {
            lore.add(ChatColor.GRAY + "Price per unit: " + shopManager.formatCoins(unit) + ChatColor.GRAY + " coins");
            double gross = unit * amount;
            double displayTotal = isBuy ? gross : shopManager.netAfterSellTax(player, gross);
            lore.add(ChatColor.GRAY + "Total: " + shopManager.formatCoins(displayTotal) + ChatColor.GRAY + " coins");
            if (!isBuy) {
                lore.add(ChatColor.DARK_GRAY + "(after tax)");
            }
        } else {
            lore.add(ChatColor.GRAY + "Price per unit: " + ChatColor.RED + "N/A");
        }
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to " + (isBuy ? "buy" : "sell") + "!");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        addPersistentData(meta, "instant_amount", String.valueOf(amount));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCustomAmountButton(boolean isBuy, String action) {
        ItemStack item = new ItemStack(Material.OAK_SIGN);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Custom Amount");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Enter a custom amount to");
        lore.add(ChatColor.GRAY + (isBuy ? "buy." : "sell."));
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to enter amount!");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        addPersistentData(meta, action, action);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createOrderAmountHeader(BazaarProduct product, boolean isBuyOrder) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName((isBuyOrder ? ChatColor.GOLD : ChatColor.GREEN) + "" + ChatColor.BOLD + (isBuyOrder ? "Create Buy Order" : "Create Sell Offer"));

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Item: " + ChatColor.YELLOW + product.getProductName());
        lore.add("");
        lore.add(ChatColor.GRAY + "Select how many items you");
        lore.add(ChatColor.GRAY + "want to " + (isBuyOrder ? "buy." : "sell."));
        lore.add("");
        lore.add(ChatColor.YELLOW + "Choose an amount below!");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createOrderAmountTierButton(BazaarProduct product, int amount, boolean isBuyOrder) {
        ItemStack item = new ItemStack(isBuyOrder ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName((isBuyOrder ? ChatColor.GREEN : ChatColor.RED) + "Set Amount " + ChatColor.YELLOW + amount + "x");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Select this amount for your");
        lore.add(ChatColor.GRAY + (isBuyOrder ? "buy order." : "sell offer."));
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to continue!");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        addPersistentData(meta, "order_amount", String.valueOf(amount));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createOrderPriceHeader(BazaarProduct product, int amount, boolean isBuyOrder) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName((isBuyOrder ? ChatColor.GOLD : ChatColor.GREEN) + "" + ChatColor.BOLD + (isBuyOrder ? "Buy Order Price" : "Sell Offer Price"));

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Item: " + ChatColor.YELLOW + product.getProductName());
        lore.add(ChatColor.GRAY + "Amount: " + ChatColor.YELLOW + amount + "x");
        lore.add("");
        lore.add(ChatColor.GRAY + "Select the price per unit");
        lore.add(ChatColor.GRAY + "for your order.");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Choose a price below!");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createOrderBestPriceButton(BazaarProduct product, int amount, boolean isBuyOrder) {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();

        double price = calculateRecommendedOrderPrice(product, isBuyOrder);
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + (isBuyOrder ? "Top Buy Order" : "Lowest Sell Offer"));

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Price per unit: " + shopManager.formatCoins(price) + ChatColor.GRAY + " coins");
        lore.add(ChatColor.GRAY + "Total: " + shopManager.formatCoins(price * amount) + ChatColor.GRAY + " coins");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to select!");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        addPersistentData(meta, "order_price", String.format(Locale.ROOT, "%.1f", price));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createOrderMarketPriceButton(BazaarProduct product, int amount, boolean isBuyOrder) {
        ItemStack item = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta meta = item.getItemMeta();

        double price = calculateMarketOrderPrice(product, isBuyOrder);
        meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Market Price");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Price per unit: " + shopManager.formatCoins(price) + ChatColor.GRAY + " coins");
        lore.add(ChatColor.GRAY + "Total: " + shopManager.formatCoins(price * amount) + ChatColor.GRAY + " coins");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to select!");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        addPersistentData(meta, "order_price", String.format(Locale.ROOT, "%.1f", price));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCustomPriceButton(boolean isBuyOrder) {
        ItemStack item = new ItemStack(Material.ANVIL);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Custom Price");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Enter a custom price per unit");
        lore.add(ChatColor.GRAY + "for your " + (isBuyOrder ? "buy order." : "sell offer."));
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to enter price!");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        addPersistentData(meta, "order_custom_price", "custom");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createOrderConfirmHeader(Player player, BazaarProduct product, int amount, double unitPrice, boolean isBuyOrder) {
        ItemStack item = new ItemStack(Material.BOOKSHELF);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Confirm Order");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Item: " + ChatColor.YELLOW + product.getProductName());
        lore.add(ChatColor.GRAY + "Amount: " + ChatColor.YELLOW + amount + "x");
        lore.add(ChatColor.GRAY + "Price per unit: " + shopManager.formatCoins(unitPrice));
        lore.add("");

        double total = unitPrice * amount;
        if (isBuyOrder) {
            lore.add(ChatColor.GRAY + "Total Cost: " + shopManager.formatCoins(total));
        } else {
            double taxRate = shopManager.getSellTaxRate(player);
            double net = shopManager.netAfterSellTax(player, total);
            lore.add(ChatColor.GRAY + "Bazaar Tax: " + ChatColor.RED + String.format(Locale.ROOT, "%.2f%%", taxRate * 100.0D));
            lore.add(ChatColor.GRAY + "Potential earnings: " + shopManager.formatCoins(net));
        }
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click the button below to confirm!");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createOrderConfirmProductItem(Player player, BazaarProduct product, int amount, double unitPrice, boolean isBuyOrder) {
        ItemStack item = new ItemStack(product.getIcon() != null ? product.getIcon() : Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.YELLOW + product.getProductName());

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + (isBuyOrder ? "Buy Order" : "Sell Offer"));
        lore.add("");
        lore.add(ChatColor.GRAY + "Amount: " + ChatColor.YELLOW + amount + "x");
        lore.add(ChatColor.GRAY + "Price per unit: " + shopManager.formatCoins(unitPrice));
        double gross = unitPrice * amount;
        double displayTotal = isBuyOrder ? gross : shopManager.netAfterSellTax(player, gross);
        lore.add(ChatColor.GRAY + "Total: " + shopManager.formatCoins(displayTotal));
        if (!isBuyOrder) {
            lore.add(ChatColor.DARK_GRAY + "(after tax)");
        }

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createOrderConfirmButton(boolean isBuyOrder) {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Confirm");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Place your " + (isBuyOrder ? "buy order" : "sell offer") + ".");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to confirm!");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        addPersistentData(meta, "order_confirm", "confirm");
        item.setItemMeta(meta);
        return item;
    }

    private double roundToTenth(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private double calculateRecommendedOrderPrice(BazaarProduct product, boolean isBuyOrder) {
        double highestBuy = product.getHighestBuyOrder();
        double lowestSell = product.getLowestSellOrder();

        if (isBuyOrder) {
            if (Double.isFinite(highestBuy) && highestBuy > 0.0) {
                return roundToTenth(highestBuy + 0.1);
            }
            if (Double.isFinite(lowestSell) && lowestSell > 0.0) {
                return roundToTenth(Math.max(0.1, lowestSell - 0.1));
            }
            double fallback = product.getInstantBuyPrice();
            return roundToTenth(Double.isFinite(fallback) && fallback > 0.0 ? fallback : 1.0);
        }

        if (Double.isFinite(lowestSell) && lowestSell > 0.0) {
            return roundToTenth(Math.max(0.1, lowestSell - 0.1));
        }
        if (Double.isFinite(highestBuy) && highestBuy > 0.0) {
            return roundToTenth(highestBuy + 0.1);
        }
        double fallback = product.getInstantSellPrice();
        return roundToTenth(Double.isFinite(fallback) && fallback > 0.0 ? fallback : 1.0);
    }

    private double calculateMarketOrderPrice(BazaarProduct product, boolean isBuyOrder) {
        double highestBuy = product.getHighestBuyOrder();
        double lowestSell = product.getLowestSellOrder();

        if (isBuyOrder) {
            if (Double.isFinite(highestBuy) && highestBuy > 0.0) {
                return roundToTenth(highestBuy);
            }
            double fallback = product.getInstantBuyPrice();
            return roundToTenth(Double.isFinite(fallback) && fallback > 0.0 ? fallback : 1.0);
        }

        if (Double.isFinite(lowestSell) && lowestSell > 0.0) {
            return roundToTenth(lowestSell);
        }
        double fallback = product.getInstantSellPrice();
        return roundToTenth(Double.isFinite(fallback) && fallback > 0.0 ? fallback : 1.0);
    }

    private ItemStack createProductDisplayItem(BazaarProduct product) {
        ItemStack item = new ItemStack(product.getIcon() != null ? product.getIcon() : Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        boolean enchanted = isEnchantedProduct(product);
        meta.setDisplayName((enchanted ? ChatColor.GREEN : ChatColor.WHITE) + product.getProductName());

        List<String> lore = new ArrayList<>();
        lore.add("");

        double lowestSell = product.getLowestSellOrder();
        double highestBuy = product.getHighestBuyOrder();
        
        lore.add("§e§lOrders:");
        if (!Double.isNaN(lowestSell) && lowestSell > 0) {
            lore.add("  §7Lowest Sell Offer: §a" + shopManager.formatCoins(lowestSell));
        } else {
            lore.add("  §7Lowest Sell Offer: §cN/A");
        }
        if (!Double.isNaN(highestBuy) && highestBuy > 0) {
            lore.add("  §7Highest Buy Order: §6" + shopManager.formatCoins(highestBuy));
        } else {
            lore.add("  §7Highest Buy Order: §cN/A");
        }
        lore.add("");

        double spread = calculateSpread(product);
        if (spread > 0.0D) {
            lore.add("§7Spread: §e" + String.format(Locale.ROOT, "%.1f", spread) + "%");
            lore.add("");
        }

        lore.add("§e§lMarket Depth:");
        lore.add("  §7Buy Orders: §6" + product.getBuyOrderCount() + " §8(" + product.formatNumber(product.getBuyOrderVolume()) + "§8)");
        lore.add("  §7Sell Offers: §a" + product.getSellOrderCount() + " §8(" + product.formatNumber(product.getSellOrderVolume()) + "§8)");
        lore.add("");

        lore.add("§e§l24h Statistics:");
        if (product.getSalesCount24h() > 0) {
            lore.add("  §7Sales: §6" + product.getSalesCount24h());
            lore.add("  §7Avg Price: §6" + shopManager.formatCoins(product.getAvgPrice24h()));
            
            String trendColor = product.getPriceChange24h() >= 0 ? "§a" : "§c";
            String trendArrow = product.getPriceChange24h() >= 0 ? "▲" : "▼";
            lore.add("  §7Trend: " + trendColor + trendArrow + " " + String.format(Locale.ROOT, "%.1f", Math.abs(product.getPriceChangePercent24h())) + "%");
        } else {
            lore.add("  §7No sales in 24h");
        }

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        if (enchanted) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createInstantBuyButton(BazaarProduct product) {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GREEN + "Instant Buy");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Buy instantly from sell offers.");
        lore.add("");
        if (!Double.isNaN(product.getInstantBuyPrice()) && product.getInstantBuyPrice() > 0) {
            lore.add(ChatColor.GRAY + "Price per unit: " + shopManager.formatCoins(product.getInstantBuyPrice()));
        } else {
            lore.add(ChatColor.GRAY + "Price per unit: " + ChatColor.RED + "N/A");
        }
        lore.add("");
        lore.add(ChatColor.GRAY + "Items will be delivered to");
        lore.add(ChatColor.GRAY + "your Bazaar Stash.");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to buy!");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        addPersistentData(meta, "instant_buy", product.getProductId());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createInstantSellButton(BazaarProduct product) {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.RED + "Instant Sell");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Sell instantly to buy orders.");
        lore.add("");
        if (!Double.isNaN(product.getInstantSellPrice()) && product.getInstantSellPrice() > 0) {
            lore.add(ChatColor.GRAY + "Price per unit: " + shopManager.formatCoins(product.getInstantSellPrice()));
        } else {
            lore.add(ChatColor.GRAY + "Price per unit: " + ChatColor.RED + "N/A");
        }
        lore.add("");
        lore.add(ChatColor.GRAY + "Coins will be delivered to");
        lore.add(ChatColor.GRAY + "your Bazaar Stash.");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to sell!");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        addPersistentData(meta, "instant_sell", product.getProductId());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBuyOrderButton(BazaarProduct product) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + "Create Buy Order");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Place an order to buy items");
        lore.add(ChatColor.GRAY + "at a price you choose.");
        lore.add("");
        if (!Double.isNaN(product.getHighestBuyOrder()) && product.getHighestBuyOrder() > 0) {
            lore.add(ChatColor.GRAY + "Highest Buy Order: " + shopManager.formatCoins(product.getHighestBuyOrder()));
        }
        if (!Double.isNaN(product.getLowestSellOrder()) && product.getLowestSellOrder() > 0) {
            lore.add(ChatColor.GRAY + "Lowest Sell Offer: " + shopManager.formatCoins(product.getLowestSellOrder()));
        }
        lore.add("");
        lore.add(ChatColor.GRAY + "Orders fill when sellers");
        lore.add(ChatColor.GRAY + "match your price.");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to create order!");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        addPersistentData(meta, "buy_order", product.getProductId());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSellOrderButton(BazaarProduct product) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GREEN + "Create Sell Offer");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Place an offer to sell items");
        lore.add(ChatColor.GRAY + "at a price you choose.");
        lore.add("");
        if (!Double.isNaN(product.getLowestSellOrder()) && product.getLowestSellOrder() > 0) {
            lore.add(ChatColor.GRAY + "Lowest Sell Offer: " + shopManager.formatCoins(product.getLowestSellOrder()));
        }
        if (!Double.isNaN(product.getHighestBuyOrder()) && product.getHighestBuyOrder() > 0) {
            lore.add(ChatColor.GRAY + "Highest Buy Order: " + shopManager.formatCoins(product.getHighestBuyOrder()));
        }
        lore.add("");
        lore.add(ChatColor.GRAY + "Offers fill when buyers");
        lore.add(ChatColor.GRAY + "match your price.");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to create offer!");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        addPersistentData(meta, "sell_order", product.getProductId());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPriceHistoryButton(BazaarProduct product) {
        ItemStack item = new ItemStack(Material.MAP);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Price History");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "View price trends over time.");
        lore.add("");

        if (product.getSalesCount24h() > 0) {
            lore.add(ChatColor.GRAY + "24h Average: " + ChatColor.YELLOW + shopManager.formatCoins(product.getAvgPrice24h()));
            lore.add(ChatColor.GRAY + "24h Low: " + ChatColor.BLUE + shopManager.formatCoins(product.getLowestPrice24h()));
            lore.add(ChatColor.GRAY + "24h High: " + ChatColor.RED + shopManager.formatCoins(product.getHighestPrice24h()));

            ChatColor trendColor = product.getPriceChange24h() >= 0 ? ChatColor.GREEN : ChatColor.RED;
            String trendArrow = product.getPriceChange24h() >= 0 ? "▲" : "▼";
            lore.add("");
            lore.add(ChatColor.GRAY + "Trend: " + trendColor + trendArrow + " " + String.format("%.2f", Math.abs(product.getPriceChangePercent24h())) + "%");
        } else {
            lore.add(ChatColor.GRAY + "No price history available.");
        }

        lore.add("");
        lore.add(ChatColor.AQUA + "Click to view graph!");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        addPersistentData(meta, "price_history", product.getProductId());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createOrderStatsButton(BazaarProduct product) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Market Stats");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Current market statistics.");
        lore.add("");
        lore.add(ChatColor.GRAY + "Buy Orders: " + ChatColor.YELLOW + product.getBuyOrderCount());
        lore.add(ChatColor.GRAY + "  Total: " + shopManager.formatCoins(product.getBuyOrderVolume()));
        lore.add("");
        lore.add(ChatColor.GRAY + "Sell Orders: " + ChatColor.YELLOW + product.getSellOrderCount());
        lore.add(ChatColor.GRAY + "  Total: " + shopManager.formatCoins(product.getSellOrderVolume()));
        lore.add("");
        lore.add(ChatColor.GRAY + "Spread: " + ChatColor.YELLOW + calculateSpread(product) + "%");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPriceHistoryHeader(BazaarProduct product, int entryCount) {
        ItemStack item = new ItemStack(Material.MAP);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Price History");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Item: " + ChatColor.YELLOW + product.getProductName());
        lore.add(ChatColor.GRAY + "Entries: " + ChatColor.YELLOW + entryCount);
        lore.add("");
        lore.add(ChatColor.GRAY + "Recent bazaar transactions.");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNoPriceHistoryItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "No Price History");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "No transactions have been");
        lore.add(ChatColor.GRAY + "recorded for this item yet.");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPriceHistoryEntryItem(BazaarPriceHistory.PriceEntry entry) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(entry.type().getDisplayName());

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Price: " + shopManager.formatCoins(entry.price()) + ChatColor.GRAY + " coins");
        lore.add(ChatColor.GRAY + "Amount: " + ChatColor.YELLOW + entry.amount());
        lore.add(ChatColor.GRAY + "Total: " + shopManager.formatCoins(entry.price() * entry.amount()) + ChatColor.GRAY + " coins");
        lore.add("");
        lore.add(ChatColor.GRAY + "When: " + ChatColor.YELLOW + formatAgo(entry.timestamp()));

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private String formatAgo(long timestamp) {
        long diffMs = Math.max(0L, System.currentTimeMillis() - timestamp);
        long seconds = diffMs / 1000;
        if (seconds < 60) {
            return seconds + "s ago";
        }

        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + "m ago";
        }

        long hours = minutes / 60;
        minutes %= 60;
        if (hours < 24) {
            return hours + "h " + minutes + "m ago";
        }

        long days = hours / 24;
        hours %= 24;
        return days + "d " + hours + "h ago";
    }

    private ItemStack createStashNavButton() {
        return createButtonItem(
                Material.CHEST,
                "&aBazaar Stash",
                new String[]{"&7Claim items and coins", "&7from bazaar transactions."},
                "bag"
        );
    }

    private ItemStack createOrdersNavButton() {
        return createButtonItem(
                Material.WRITABLE_BOOK,
                "&aManage Orders",
                new String[]{"&7View and manage your", "&7active buy and sell orders."},
                "orders"
        );
    }

    private ItemStack createBazaarNavButton() {
        return createButtonItem(
                Material.EXPERIENCE_BOTTLE,
                "&aBazaar",
                new String[]{"&7Trade with other players", "&7instantly or place orders."},
                "noop"
        );
    }

    private ItemStack createBackButton() {
        return createButtonItem(Material.ARROW, "&e← Back", new String[]{"&7Return to previous menu."}, "back");
    }

    private ItemStack createBackToCategoryButton(BazaarProduct.BazaarCategory category) {
        return createButtonItem(Material.ARROW, "&e← Back", new String[]{"&7Return to " + category.getDisplayName() + "."}, "back");
    }

    private ItemStack createPrevPageButton(boolean active) {
        if (active) {
            return createButtonItem(Material.ARROW, "&ePrevious Page", new String[]{"&7Go to previous page."}, "prev");
        }
        return createButtonItem(Material.GRAY_STAINED_GLASS_PANE, "&7Previous Page", new String[]{"&8No previous page."}, "noop");
    }

    private ItemStack createNextPageButton(boolean active) {
        if (active) {
            return createButtonItem(Material.ARROW, "&eNext Page", new String[]{"&7Go to next page."}, "next");
        }
        return createButtonItem(Material.GRAY_STAINED_GLASS_PANE, "&7Next Page", new String[]{"&8No more pages."}, "noop");
    }

    private ItemStack createCategoryInfoItem(BazaarProduct.BazaarCategory category, int productCount) {
        return createButtonItem(Material.BOOK, "&bCategory Info", new String[]{"&7Products: " + productCount, "&7Category: " + category.getDisplayName()}, "info");
    }

    private ItemStack createYourOrdersButton(Player player, BazaarProduct product) {
        UUID profileId = shopManager.getSelectedProfileId(player);
        List<BazaarOrder> orders = profileId == null ? List.of() : shopManager.getOrderBook().getProfileOrders(profileId, player.getUniqueId());
        long productOrders = orders.stream().filter(o -> o.getProductId().equals(product.getProductId())).count();

        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Your Orders");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "View your orders for this product.");
        lore.add("");
        lore.add(ChatColor.GRAY + "Active Orders: " + ChatColor.YELLOW + productOrders);
        lore.add(ChatColor.GRAY + "Total Orders: " + ChatColor.YELLOW + orders.size());
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to manage!");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        addPersistentData(meta, "your_orders", product.getProductId());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCloseButton() {
        return createButtonItem(Material.BARRIER, "&cClose", new String[]{}, "close");
    }

    private ItemStack createNoOrdersItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "No Active Orders");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "You have no active orders.");
        lore.add("");
        lore.add(ChatColor.GRAY + "Place buy or sell orders");
        lore.add(ChatColor.GRAY + "from product menus.");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Browse bazaar to start trading!");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createOrdersHeader(Player player, int orderCount) {
        ItemStack item = new ItemStack(Material.BOOKSHELF);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GREEN + "Bazaar Orders");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Manage your active bazaar orders.");
        lore.add("");
        lore.add(ChatColor.GRAY + "Active Orders: " + ChatColor.YELLOW + orderCount + "/" + shopManager.getMaxOrdersAllowed(player));
        lore.add("");
        lore.add(ChatColor.GRAY + "Orders expire after 7 days.");
        lore.add(ChatColor.GRAY + "Cancelled orders refund to your Bazaar Stash.");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createOrdersInfoItem(Player player, int orderCount) {
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Order Summary");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Active Orders: " + ChatColor.YELLOW + orderCount);
        int maxOrders = shopManager.getMaxOrdersAllowed(player);
        lore.add(ChatColor.GRAY + "Remaining: " + ChatColor.GREEN + Math.max(0, maxOrders - orderCount));
        lore.add("");
        lore.add(ChatColor.GRAY + "Manage orders from this menu.");
        lore.add(ChatColor.GRAY + "Click orders to cancel.");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBagHeader(double bagValue) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Bazaar Orders");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Collect your items and coins");
        lore.add(ChatColor.GRAY + "from filled orders and stash.");
        lore.add("");
        lore.add(ChatColor.GRAY + "Stash Value: " + ChatColor.GOLD + shopManager.formatCoins(bagValue));
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click items to collect!");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createEmptyBagItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.RED + "No items to claim!");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Your items and coins from");
        lore.add(ChatColor.GRAY + "Bazaar transactions will");
        lore.add(ChatColor.GRAY + "appear here.");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to go back!");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCoinsItem(double coins) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + "Coins to Claim");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Coins earned from selling");
        lore.add(ChatColor.GRAY + "items on the Bazaar.");
        lore.add("");
        lore.add(ChatColor.GRAY + "Amount: " + ChatColor.GOLD + shopManager.formatCoins(coins));
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to collect!");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        addPersistentData(meta, "claim_coins", String.valueOf(coins));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBagItem(BazaarProduct product, int amount) {
        ItemStack item = new ItemStack(product.getIcon() != null ? product.getIcon() : Material.PAPER);
        item.setAmount(Math.min(amount, 64));
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + product.getProductName());

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Amount: " + ChatColor.YELLOW + amount);
        lore.add("");
        lore.add(ChatColor.GRAY + "Items purchased or from");
        lore.add(ChatColor.GRAY + "cancelled orders.");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to collect!");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        addPersistentData(meta, "claim_item", product.getProductId() + ":" + amount);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createClaimAllButton(Player player) {
        ItemStack item = new ItemStack(Material.HOPPER);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Collect All");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Collect all items and coins");
        lore.add(ChatColor.GRAY + "from your Bazaar Stash.");
        lore.add("");
        lore.add(ChatColor.GREEN + "Click to collect!");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        addPersistentData(meta, "claim_all", "all");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createButtonItem(Material material, String displayName, String[] lore, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
        List<String> formattedLore = new ArrayList<>();
        for (String line : lore) {
            formattedLore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(formattedLore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        addPersistentData(meta, action, action);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createFillerPane(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void fillBazaarFrame(Inventory inventory) {
        if (inventory == null) {
            return;
        }
        ItemStack border = createFillerPane(Material.PURPLE_STAINED_GLASS_PANE);
        ItemStack inner = createFillerPane(Material.BLACK_STAINED_GLASS_PANE);

        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, border);
        }

        for (int slot : PRODUCT_SLOTS) {
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, inner);
            }
        }
    }

    private void fillBazaarListFrame(Inventory inventory) {
        if (inventory == null) {
            return;
        }
        ItemStack border = createFillerPane(Material.PURPLE_STAINED_GLASS_PANE);
        ItemStack inner = createFillerPane(Material.BLACK_STAINED_GLASS_PANE);

        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, border);
        }

        for (int slot = 9; slot < 45 && slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, inner);
        }
    }

    private void fillBackground(Inventory inventory, Material glassType) {
        ItemStack glass = new ItemStack(glassType);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            glass.setItemMeta(meta);
        }
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, glass);
            }
        }
    }

    private void setItem(Inventory inventory, int slot, ItemStack item) {
        if (slot >= 0 && slot < inventory.getSize()) {
            inventory.setItem(slot, item);
        }
    }

    private void addPersistentData(ItemMeta meta, String key, String value) {
        meta.getPersistentDataContainer().set(BazaarKeys.ACTION_KEY, PersistentDataType.STRING, key);
        meta.getPersistentDataContainer().set(BazaarKeys.VALUE_KEY, PersistentDataType.STRING, value);
    }

    private boolean isEnchantedProduct(BazaarProduct product) {
        if (product == null) {
            return false;
        }
        String id = product.getProductId();
        if (id != null && id.toUpperCase(Locale.ROOT).startsWith("ENCHANTED_")) {
            return true;
        }
        String name = product.getProductName();
        return name != null && name.toLowerCase(Locale.ROOT).startsWith("enchanted");
    }

    private double calculateSpread(BazaarProduct product) {
        if (Double.isNaN(product.getLowestSellOrder()) || Double.isNaN(product.getHighestBuyOrder())) return 0;
        if (product.getHighestBuyOrder() <= 0) return 0;
        return ((product.getLowestSellOrder() - product.getHighestBuyOrder()) / product.getHighestBuyOrder()) * 100;
    }

    private double calculateBagValue(UUID profileId) {
        BazaarShoppingBag.PlayerShoppingBag bag = shopManager.getShoppingBag().getBag(profileId);
        double total = bag.getCoins();
        for (Map.Entry<String, Integer> entry : bag.getItems().entrySet()) {
            BazaarProduct product = shopManager.getProduct(entry.getKey());
            if (product != null) {
                total += product.getInstantSellPrice() * entry.getValue();
            }
        }
        return total;
    }

    private void playOpenSound(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
    }

    private void playClickSound(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
    }

    // ==================== INVENTORY HANDLER ====================

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        // If the top inventory is a Bazaar menu, cancel ALL clicks in both top and bottom
        if (event.getInventory().getHolder() instanceof BazaarHolder holder) {
            event.setCancelled(true);
            
            // Only process actions for clicks in the top inventory
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getInventory())) {
                ItemStack clicked = event.getCurrentItem();
                if (clicked == null || clicked.getType().isAir() || !clicked.hasItemMeta()) return;

                ItemMeta meta = clicked.getItemMeta();
                String action = meta.getPersistentDataContainer().get(BazaarKeys.ACTION_KEY, PersistentDataType.STRING);
                if (action != null) {
                    handleAction(player, holder, clicked, action, event.getClick());
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof BazaarHolder) {
            event.setCancelled(true);
        }
    }

    private void handleAction(Player player, BazaarHolder holder, ItemStack clicked, String action, ClickType click) {
        playClickSound(player);

        switch (action) {
            case "back" -> {
                if (holder instanceof MainHolder) {
                    player.closeInventory();
                } else if (holder instanceof CategoryHolder) {
                    openMain(player);
                } else if (holder instanceof VariantsHolder vh) {
                    openCategory(player, vh.category, vh.subcategory, vh.page);
                } else if (holder instanceof ProductHolder ph) {
                    if (ph.product != null && ph.product.isVariant()) {
                        BazaarProduct parent = shopManager.getProduct(ph.product.getParentProductId());
                        if (parent != null) {
                            openVariantsMenu(player, parent, ph.category, ph.subcategory, ph.page);
                        } else {
                            openCategory(player, ph.category, ph.subcategory, ph.page);
                        }
                    } else if (ph.searchQuery != null && !ph.searchQuery.isBlank()) {
                        openSearchResults(player, ph.searchQuery, ph.searchPage);
                    } else {
                        openCategory(player, ph.category, ph.subcategory, ph.page);
                    }
                } else if (holder instanceof InstantAmountHolder iah) {
                    BazaarProduct product = shopManager.getProduct(iah.productId);
                    if (product != null) {
                        openProduct(player, product, iah.category, iah.subcategory, iah.page, iah.searchQuery, iah.searchPage);
                    } else {
                        openMain(player);
                    }
                } else if (holder instanceof OrderAmountHolder oah) {
                    BazaarProduct product = shopManager.getProduct(oah.productId);
                    if (product != null) {
                        openProduct(player, product, oah.category, oah.subcategory, oah.page, oah.searchQuery, oah.searchPage);
                    } else {
                        openMain(player);
                    }
                } else if (holder instanceof OrderPriceHolder oph) {
                    BazaarProduct product = shopManager.getProduct(oph.productId);
                    if (product != null) {
                        openOrderAmountMenu(player, product, oph.isBuyOrder, oph.category, oph.subcategory, oph.page, oph.searchQuery, oph.searchPage);
                    } else {
                        openMain(player);
                    }
                } else if (holder instanceof OrderConfirmHolder och) {
                    BazaarProduct product = shopManager.getProduct(och.productId);
                    if (product != null) {
                        openOrderPriceMenu(player, product, och.amount, och.isBuyOrder, och.category, och.subcategory, och.page, och.searchQuery, och.searchPage);
                    } else {
                        openMain(player);
                    }
                } else if (holder instanceof PriceHistoryHolder phh) {
                    BazaarProduct product = shopManager.getProduct(phh.productId);
                    if (product != null) {
                        openProduct(player, product, phh.category, phh.subcategory, phh.page, phh.searchQuery, phh.searchPage);
                    } else {
                        openMain(player);
                    }
                } else if (holder instanceof OrdersHolder oh && oh.returnProductId != null) {
                    BazaarProduct product = shopManager.getProduct(oh.returnProductId);
                    if (product != null) {
                        openProduct(player, product, oh.category, oh.subcategory, oh.page, oh.searchQuery, oh.searchPage);
                    } else {
                        openMain(player);
                    }
                } else if (holder instanceof OrdersHolder || holder instanceof ShoppingBagHolder || holder instanceof SearchHolder) {
                    openMain(player);
                }
            }
            case "close" -> player.closeInventory();
            case "noop" -> {}
            case "bag" -> openShoppingBag(player);
            case "orders" -> openOrders(player);
            case "your_orders" -> {
                String productId = clicked.getItemMeta().getPersistentDataContainer().get(BazaarKeys.VALUE_KEY, PersistentDataType.STRING);
                if (productId == null) {
                    return;
                }
                BazaarProduct product = shopManager.getProduct(productId);
                if (product == null) {
                    return;
                }
                if (holder instanceof ProductHolder ph) {
                    openOrders(player, new OrdersHolder(productId, productId, ph.category, ph.subcategory, ph.page, ph.searchQuery, ph.searchPage));
                } else {
                    openOrders(player, new OrdersHolder(productId, productId, product.getCategory(), null, 0, null, 0));
                }
            }
            case "info" -> player.sendMessage(ChatColor.YELLOW + "Bazaar Help: Use /bazaar help for commands.");
            case "search" -> openSearchSign(player);
            case "open_category" -> {
                String categoryName = clicked.getItemMeta().getPersistentDataContainer().get(BazaarKeys.VALUE_KEY, PersistentDataType.STRING);
                if (categoryName != null) {
                    BazaarProduct.BazaarCategory category = BazaarProduct.BazaarCategory.valueOf(categoryName);
                    if (holder instanceof VariantsHolder vh) {
                        openCategory(player, category, vh.page);
                    } else {
                        openCategory(player, category, 0);
                    }
                }
            }
            case "open_product" -> {
                if (clicked == null || !clicked.hasItemMeta()) return;
                String productId = clicked.getItemMeta().getPersistentDataContainer().get(BazaarKeys.VALUE_KEY, PersistentDataType.STRING);
                if (productId == null) return;
                
                BazaarProduct product = shopManager.getProduct(productId);
                if (product == null) return;

                // Handle product grouping
                if (product.hasVariants()) {
                    // This is a parent product (e.g. Wheat)
                    if (holder instanceof CategoryHolder ch) {
                        openVariantsMenu(player, product, ch.category, ch.subcategory, ch.page);
                    } else {
                        // Fallback context
                        openVariantsMenu(player, product, product.getCategory(), product.getSubcategory(), 0);
                    }
                } else {
                    // This is a single product or a variant (e.g. Enchanted Wheat)
                    if (holder instanceof CategoryHolder ch) {
                        openProduct(player, product, ch.category, ch.subcategory, ch.page);
                    } else if (holder instanceof VariantsHolder vh) {
                        openProduct(player, product, vh.category, vh.subcategory, vh.page);
                    } else if (holder instanceof SearchHolder sh) {
                        openProduct(player, product, product.getCategory(), product.getSubcategory(), sh.page, sh.query, sh.page);
                    } else {
                        openProduct(player, product);
                    }
                }
            }
            case "instant_buy" -> {
                String productId = clicked.getItemMeta().getPersistentDataContainer().get(BazaarKeys.VALUE_KEY, PersistentDataType.STRING);
                if (productId == null) {
                    return;
                }
                BazaarProduct product = shopManager.getProduct(productId);
                if (product == null) {
                    return;
                }
                if (holder instanceof ProductHolder ph) {
                    openInstantAmountMenu(player, product, true, ph.category, ph.subcategory, ph.page, ph.searchQuery, ph.searchPage);
                } else {
                    openInstantAmountMenu(player, product, true, product.getCategory(), null, 0, null, 0);
                }
            }
            case "instant_sell" -> {
                String productId = clicked.getItemMeta().getPersistentDataContainer().get(BazaarKeys.VALUE_KEY, PersistentDataType.STRING);
                if (productId == null) {
                    return;
                }
                BazaarProduct product = shopManager.getProduct(productId);
                if (product == null) {
                    return;
                }
                if (holder instanceof ProductHolder ph) {
                    openInstantAmountMenu(player, product, false, ph.category, ph.subcategory, ph.page, ph.searchQuery, ph.searchPage);
                } else {
                    openInstantAmountMenu(player, product, false, product.getCategory(), null, 0, null, 0);
                }
            }
            case "instant_amount" -> {
                if (!(holder instanceof InstantAmountHolder iah)) {
                    return;
                }
                String amountText = clicked.getItemMeta().getPersistentDataContainer().get(BazaarKeys.VALUE_KEY, PersistentDataType.STRING);
                if (amountText == null) {
                    return;
                }
                int amount;
                try {
                    amount = Integer.parseInt(amountText);
                } catch (NumberFormatException ignored) {
                    return;
                }
                if (amount <= 0) {
                    return;
                }

                BazaarProduct product = shopManager.getProduct(iah.productId);
                if (product == null) {
                    return;
                }

                if (iah.isBuy) {
                    shopManager.instantBuy(player, product.getProductId(), amount);
                } else {
                    shopManager.instantSell(player, product.getProductId(), amount);
                }

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    BazaarProduct refreshed = shopManager.getProduct(product.getProductId());
                    if (refreshed != null) {
                        openProduct(player, refreshed, iah.category, iah.subcategory, iah.page, iah.searchQuery, iah.searchPage);
                    }
                }, 2L);
            }
            case "instant_custom_amount" -> {
                if (!(holder instanceof InstantAmountHolder iah)) {
                    return;
                }
                BazaarProduct product = shopManager.getProduct(iah.productId);
                if (product == null) {
                    return;
                }
                openInstantCustomAmountSign(player, product, iah.isBuy, iah.category, iah.subcategory, iah.page, iah.searchQuery, iah.searchPage);
            }
            case "open_subcategory" -> {
                if (!(holder instanceof CategoryHolder ch)) {
                    return;
                }
                if (clicked != null && clicked.hasItemMeta()) {
                    String subName = clicked.getItemMeta().getPersistentDataContainer().get(BazaarKeys.VALUE_KEY, PersistentDataType.STRING);
                    if (subName != null) {
                        try {
                            BazaarProduct.BazaarSubcategory sub = BazaarProduct.BazaarSubcategory.valueOf(subName);
                            openCategory(player, ch.category, sub, 0);
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
            }
            case "clear_subcategory" -> {
                if (holder instanceof CategoryHolder ch) {
                    openCategory(player, ch.category, null, 0);
                }
            }
            case "buy_order" -> {
                String productId = clicked.getItemMeta().getPersistentDataContainer().get(BazaarKeys.VALUE_KEY, PersistentDataType.STRING);
                if (productId == null) {
                    return;
                }
                BazaarProduct product = shopManager.getProduct(productId);
                if (product == null) {
                    return;
                }
                if (holder instanceof ProductHolder ph) {
                    openOrderAmountMenu(player, product, true, ph.category, ph.subcategory, ph.page, ph.searchQuery, ph.searchPage);
                } else {
                    openOrderAmountMenu(player, product, true, product.getCategory(), null, 0, null, 0);
                }
            }
            case "sell_order" -> {
                String productId = clicked.getItemMeta().getPersistentDataContainer().get(BazaarKeys.VALUE_KEY, PersistentDataType.STRING);
                if (productId == null) {
                    return;
                }
                BazaarProduct product = shopManager.getProduct(productId);
                if (product == null) {
                    return;
                }
                if (holder instanceof ProductHolder ph) {
                    openOrderAmountMenu(player, product, false, ph.category, ph.subcategory, ph.page, ph.searchQuery, ph.searchPage);
                } else {
                    openOrderAmountMenu(player, product, false, product.getCategory(), null, 0, null, 0);
                }
            }
            case "order_amount" -> {
                if (!(holder instanceof OrderAmountHolder oah)) {
                    return;
                }
                String amountText = clicked.getItemMeta().getPersistentDataContainer().get(BazaarKeys.VALUE_KEY, PersistentDataType.STRING);
                if (amountText == null) {
                    return;
                }
                int amount;
                try {
                    amount = Integer.parseInt(amountText);
                } catch (NumberFormatException ignored) {
                    return;
                }
                BazaarProduct product = shopManager.getProduct(oah.productId);
                if (product == null) {
                    return;
                }
                openOrderPriceMenu(player, product, amount, oah.isBuyOrder, oah.category, oah.subcategory, oah.page, oah.searchQuery, oah.searchPage);
            }
            case "order_custom_amount" -> {
                if (!(holder instanceof OrderAmountHolder oah)) {
                    return;
                }
                BazaarProduct product = shopManager.getProduct(oah.productId);
                if (product == null) {
                    return;
                }
                openOrderCustomAmountSign(player, product, oah.isBuyOrder, oah.category, oah.subcategory, oah.page, oah.searchQuery, oah.searchPage);
            }
            case "order_price" -> {
                if (!(holder instanceof OrderPriceHolder oph)) {
                    return;
                }
                String priceText = clicked.getItemMeta().getPersistentDataContainer().get(BazaarKeys.VALUE_KEY, PersistentDataType.STRING);
                if (priceText == null) {
                    return;
                }
                double price;
                try {
                    price = Double.parseDouble(priceText);
                } catch (NumberFormatException ignored) {
                    return;
                }
                BazaarProduct product = shopManager.getProduct(oph.productId);
                if (product == null) {
                    return;
                }
                openOrderConfirmMenu(player, product, oph.amount, price, oph.isBuyOrder, oph.category, oph.subcategory, oph.page, oph.searchQuery, oph.searchPage);
            }
            case "order_custom_price" -> {
                if (!(holder instanceof OrderPriceHolder oph)) {
                    return;
                }
                BazaarProduct product = shopManager.getProduct(oph.productId);
                if (product == null) {
                    return;
                }
                openOrderCustomPriceSign(player, product, oph.amount, oph.isBuyOrder, oph.category, oph.subcategory, oph.page, oph.searchQuery, oph.searchPage);
            }
            case "order_confirm" -> {
                if (!(holder instanceof OrderConfirmHolder och)) {
                    return;
                }
                BazaarProduct product = shopManager.getProduct(och.productId);
                if (product == null) {
                    return;
                }
                if (och.isBuyOrder) {
                    shopManager.placeBuyOrder(player, product.getProductId(), och.amount, och.unitPrice);
                } else {
                    shopManager.placeSellOrder(player, product.getProductId(), och.amount, och.unitPrice);
                }
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    BazaarProduct refreshed = shopManager.getProduct(product.getProductId());
                    if (refreshed != null) {
                        openProduct(player, refreshed, och.category, och.subcategory, och.page, och.searchQuery, och.searchPage);
                    }
                }, 2L);
            }
            case "price_history" -> {
                String productId = clicked.getItemMeta().getPersistentDataContainer().get(BazaarKeys.VALUE_KEY, PersistentDataType.STRING);
                if (productId == null) {
                    return;
                }
                BazaarProduct product = shopManager.getProduct(productId);
                if (product == null) {
                    return;
                }
                if (holder instanceof ProductHolder ph) {
                    openPriceHistoryMenu(player, product, ph.category, ph.subcategory, ph.page, ph.searchQuery, ph.searchPage);
                } else {
                    openPriceHistoryMenu(player, product, product.getCategory(), null, 0, null, 0);
                }
            }
            case "cancel_order" -> {
                if (!(holder instanceof OrdersHolder oh)) {
                    return;
                }
                String orderId = clicked.getItemMeta().getPersistentDataContainer().get(BazaarKeys.VALUE_KEY, PersistentDataType.STRING);
                if (orderId == null) {
                    return;
                }
                shopManager.cancelOrder(player, orderId);
                openOrders(player, new OrdersHolder(oh.filterProductId, oh.returnProductId, oh.category, oh.subcategory, oh.page, oh.searchQuery, oh.searchPage));
            }
            case "claim_all" -> {
                shopManager.claimAll(player);
                openShoppingBag(player);
            }
            case "claim_coins" -> {
                shopManager.claimCoins(player);
                openShoppingBag(player);
            }
            case "claim_item" -> {
                if (clicked != null && clicked.hasItemMeta()) {
                    String value = clicked.getItemMeta().getPersistentDataContainer().get(BazaarKeys.VALUE_KEY, PersistentDataType.STRING);
                    if (value != null) {
                        String[] parts = value.split(":", 2);
                        if (parts.length == 2) {
                            String productId = parts[0];
                            try {
                                int amount = Integer.parseInt(parts[1]);
                                shopManager.claimItem(player, productId, amount);
                            } catch (NumberFormatException ignored) {
                                player.sendMessage(ChatColor.RED + "Invalid amount.");
                            }
                        }
                    }
                }
                openShoppingBag(player);
            }
            case "prev" -> {
                if (holder instanceof CategoryHolder ch) {
                    openCategory(player, ch.category, ch.subcategory, ch.page - 1);
                } else if (holder instanceof SearchHolder sh) {
                    openSearchResults(player, sh.query, sh.page - 1);
                }
            }
            case "next" -> {
                if (holder instanceof CategoryHolder ch) {
                    openCategory(player, ch.category, ch.subcategory, ch.page + 1);
                } else if (holder instanceof SearchHolder sh) {
                    openSearchResults(player, sh.query, sh.page + 1);
                }
            }
        }
    }

    private void openInstantCustomAmountSign(
            Player player,
            BazaarProduct product,
            boolean isBuy,
            BazaarProduct.BazaarCategory category,
            BazaarProduct.BazaarSubcategory subcategory,
            int page,
            String searchQuery,
            int searchPage
    ) {
        new AnvilGUI.Builder()
                .plugin(plugin)
                .title("Enter Amount")
                .itemLeft(new ItemStack(Material.NAME_TAG))
                .text("1")
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();
                    
                    String amountText = stateSnapshot.getText().trim();
                    int amount;
                    try {
                        amount = Integer.parseInt(amountText);
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Invalid amount!");
                        return List.of(AnvilGUI.ResponseAction.close());
                    }
                    if (amount <= 0) {
                        player.sendMessage(ChatColor.RED + "Invalid amount!");
                        return List.of(AnvilGUI.ResponseAction.close());
                    }
                    if (amount > MAX_BAZAAR_AMOUNT) {
                        player.sendMessage(ChatColor.RED + "Amount cannot exceed " + MAX_BAZAAR_AMOUNT + ".");
                        return List.of(AnvilGUI.ResponseAction.close());
                    }

                    if (isBuy) {
                        shopManager.instantBuy(player, product.getProductId(), amount);
                    } else {
                        shopManager.instantSell(player, product.getProductId(), amount);
                    }

                    return List.of(AnvilGUI.ResponseAction.run(() -> {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            BazaarProduct refreshed = shopManager.getProduct(product.getProductId());
                            if (refreshed != null) {
                                openProduct(player, refreshed, category, subcategory, page, searchQuery, searchPage);
                            }
                        }, 2L);
                    }));
                })
                .open(player);
    }

    private void openOrderCustomAmountSign(
            Player player,
            BazaarProduct product,
            boolean isBuyOrder,
            BazaarProduct.BazaarCategory category,
            BazaarProduct.BazaarSubcategory subcategory,
            int page,
            String searchQuery,
            int searchPage
    ) {
        new AnvilGUI.Builder()
                .plugin(plugin)
                .title("Enter Amount")
                .itemLeft(new ItemStack(Material.NAME_TAG))
                .text("1")
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();
                    
                    String amountText = stateSnapshot.getText().trim();
                    int amount;
                    try {
                        amount = Integer.parseInt(amountText);
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Invalid amount!");
                        return List.of(AnvilGUI.ResponseAction.close());
                    }
                    if (amount <= 0) {
                        player.sendMessage(ChatColor.RED + "Invalid amount!");
                        return List.of(AnvilGUI.ResponseAction.close());
                    }
                    if (amount > MAX_BAZAAR_AMOUNT) {
                        player.sendMessage(ChatColor.RED + "Amount cannot exceed " + MAX_BAZAAR_AMOUNT + ".");
                        return List.of(AnvilGUI.ResponseAction.close());
                    }

                    return List.of(AnvilGUI.ResponseAction.run(() -> openOrderPriceMenu(player, product, amount, isBuyOrder, category, subcategory, page, searchQuery, searchPage)));
                })
                .open(player);
    }

    private void openOrderCustomPriceSign(
            Player player,
            BazaarProduct product,
            int amount,
            boolean isBuyOrder,
            BazaarProduct.BazaarCategory category,
            BazaarProduct.BazaarSubcategory subcategory,
            int page,
            String searchQuery,
            int searchPage
    ) {
        double currentPrice = isBuyOrder ? product.getInstantBuyPrice() : product.getInstantSellPrice();
        String defaultPrice = String.format("%.1f", Double.isNaN(currentPrice) || currentPrice <= 0 ? 1.0 : currentPrice);

        new AnvilGUI.Builder()
                .plugin(plugin)
                .title("Enter Price")
                .itemLeft(new ItemStack(Material.GOLD_INGOT))
                .text(defaultPrice)
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();
                    
                    String priceText = stateSnapshot.getText().trim().replace(",", "");
                    double price;
                    try {
                        price = roundToTenth(Double.parseDouble(priceText));
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Invalid price!");
                        return List.of(AnvilGUI.ResponseAction.close());
                    }
                    if (!Double.isFinite(price) || price <= 0.0) {
                        player.sendMessage(ChatColor.RED + "Invalid price!");
                        return List.of(AnvilGUI.ResponseAction.close());
                    }
                    if (!isBuyOrder) {
                        // Hypixel restriction: custom sell offer price is limited to 50% above the lowest sell offer,
                        // unless there are no existing sell offers for the product.
                        double lowestSell = product.getLowestSellOrder();
                        if (Double.isFinite(lowestSell) && lowestSell > 0.0) {
                            double maxAllowed = lowestSell * 1.5;
                            if (price > maxAllowed + 1e-9D) {
                                player.sendMessage(ChatColor.RED + "Price cannot exceed " + shopManager.formatCoins(maxAllowed) + ChatColor.RED + " for this item.");
                                return List.of(AnvilGUI.ResponseAction.close());
                            }
                        }
                    }

                    double finalPrice = price;
                    return List.of(AnvilGUI.ResponseAction.run(() -> openOrderConfirmMenu(player, product, amount, finalPrice, isBuyOrder, category, subcategory, page, searchQuery, searchPage)));
                })
                .open(player);
    }

    private void openSearchSign(Player player) {
        new AnvilGUI.Builder()
                .plugin(plugin)
                .title("Bazaar Search")
                .itemLeft(new ItemStack(Material.OAK_SIGN))
                .text("Search...")
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();
                    
                    String query = stateSnapshot.getText().trim();
                    if (query.isEmpty() || query.equals("Search...")) {
                        player.sendMessage(ChatColor.RED + "Please enter a search query.");
                        return List.of(AnvilGUI.ResponseAction.close());
                    }
                    return List.of(AnvilGUI.ResponseAction.run(() -> openSearchResults(player, query, 0)));
                })
                .open(player);
    }

    private void openSearchResults(Player player, String query, int page) {
        if (player == null) {
            return;
        }
        if (query == null || query.isBlank()) {
            openMain(player);
            return;
        }

        String normalized = query.trim().toLowerCase(Locale.ROOT);
        List<BazaarProduct> matches = new ArrayList<>();
        for (BazaarProduct product : shopManager.getAllProducts()) {
            if (product == null) {
                continue;
            }
            String name = product.getProductName();
            String id = product.getProductId();
            if (name != null && name.toLowerCase(Locale.ROOT).contains(normalized)) {
                matches.add(product);
                continue;
            }
            if (id != null && id.toLowerCase(Locale.ROOT).contains(normalized)) {
                matches.add(product);
            }
        }
        matches.sort(Comparator.comparing(p -> {
            String name = p == null ? null : p.getProductName();
            return name == null ? "" : name;
        }, String.CASE_INSENSITIVE_ORDER));

        int pageSize = PRODUCT_SLOTS.length;
        int totalPages = Math.max(1, (matches.size() + pageSize - 1) / pageSize);
        int currentPage = Math.max(0, Math.min(page, totalPages - 1));

        String title = ChatColor.translateAlternateColorCodes('&', "&8Bazaar - Search") + " §8(" + (currentPage + 1) + "/" + totalPages + ")";
        Inventory inventory = Bukkit.createInventory(new SearchHolder(query.trim(), currentPage), 54, title);

        fillBazaarFrame(inventory);
        setItem(inventory, 4, createSearchHeader(query.trim(), matches.size()));

        if (matches.isEmpty()) {
            setItem(inventory, 22, createNoSearchResultsItem(query.trim()));
        } else {
            int start = currentPage * pageSize;
            int end = Math.min(start + pageSize, matches.size());
            int slotIndex = 0;
            for (int i = start; i < end && slotIndex < PRODUCT_SLOTS.length; i++) {
                BazaarProduct product = matches.get(i);
                if (product == null) {
                    continue;
                }
                BazaarProduct refreshed = shopManager.getProduct(product.getProductId());
                setItem(inventory, PRODUCT_SLOTS[slotIndex], createProductItem(refreshed != null ? refreshed : product));
                slotIndex++;
            }
        }

        setItem(inventory, BACK_SLOT, createBackButton());
        setItem(inventory, PREV_SLOT, createPrevPageButton(currentPage > 0));
        setItem(inventory, 52, createStashNavButton());
        setItem(inventory, INFO_SLOT, createButtonItem(Material.ANVIL, "&aSearch", new String[]{"&7Search again.", "&7Click to enter a query."}, "search"));
        setItem(inventory, MIDDLE_SLOT, createBazaarNavButton());
        setItem(inventory, NEXT_SLOT, createNextPageButton(currentPage < totalPages - 1));
        setItem(inventory, 51, createOrdersNavButton());
        setItem(inventory, CLOSE_SLOT, createCloseButton());

        player.openInventory(inventory);
        playOpenSound(player);
    }

    private String firstNonBlank(String[] lines) {
        if (lines == null) {
            return null;
        }
        for (String line : lines) {
            if (line != null && !line.isBlank()) {
                return line.trim();
            }
        }
        return null;
    }

    // ==================== HOLDER CLASSES ====================

    private interface BazaarHolder extends InventoryHolder {}

    private static class MainHolder implements BazaarHolder {
        @Override
        public Inventory getInventory() { return null; }
    }

    private static class CategoryHolder implements BazaarHolder {
        final BazaarProduct.BazaarCategory category;
        final BazaarProduct.BazaarSubcategory subcategory;
        final int page;

        CategoryHolder(BazaarProduct.BazaarCategory category, BazaarProduct.BazaarSubcategory subcategory, int page) {
            this.category = category;
            this.subcategory = subcategory;
            this.page = page;
        }

        @Override
        public Inventory getInventory() { return null; }
    }

    private static class VariantsHolder implements BazaarHolder {
        final BazaarProduct parent;
        final BazaarProduct.BazaarCategory category;
        final BazaarProduct.BazaarSubcategory subcategory;
        final int page;

        VariantsHolder(BazaarProduct parent, BazaarProduct.BazaarCategory category, BazaarProduct.BazaarSubcategory subcategory, int page) {
            this.parent = parent;
            this.category = category;
            this.subcategory = subcategory;
            this.page = page;
        }

        @Override
        public Inventory getInventory() { return null; }
    }

    private static class ProductHolder implements BazaarHolder {
        final BazaarProduct product;
        final BazaarProduct.BazaarCategory category;
        final BazaarProduct.BazaarSubcategory subcategory;
        final int page;
        final String searchQuery;
        final int searchPage;

        ProductHolder(BazaarProduct product, BazaarProduct.BazaarCategory category, BazaarProduct.BazaarSubcategory subcategory, int page) {
            this(product, category, subcategory, page, null, 0);
        }

        ProductHolder(
                BazaarProduct product,
                BazaarProduct.BazaarCategory category,
                BazaarProduct.BazaarSubcategory subcategory,
                int page,
                String searchQuery,
                int searchPage
        ) {
            this.product = product;
            this.category = category;
            this.subcategory = subcategory;
            this.page = page;
            this.searchQuery = searchQuery;
            this.searchPage = searchPage;
        }

        @Override
        public Inventory getInventory() { return null; }
    }

    private static class SearchHolder implements BazaarHolder {
        final String query;
        final int page;

        SearchHolder(String query, int page) {
            this.query = query;
            this.page = page;
        }

        @Override
        public Inventory getInventory() { return null; }
    }

    private static class OrdersHolder implements BazaarHolder {
        final String filterProductId;
        final String returnProductId;
        final BazaarProduct.BazaarCategory category;
        final BazaarProduct.BazaarSubcategory subcategory;
        final int page;
        final String searchQuery;
        final int searchPage;

        OrdersHolder() {
            this(null, null, null, null, 0, null, 0);
        }

        OrdersHolder(
                String filterProductId,
                String returnProductId,
                BazaarProduct.BazaarCategory category,
                BazaarProduct.BazaarSubcategory subcategory,
                int page,
                String searchQuery,
                int searchPage
        ) {
            this.filterProductId = filterProductId;
            this.returnProductId = returnProductId;
            this.category = category;
            this.subcategory = subcategory;
            this.page = page;
            this.searchQuery = searchQuery;
            this.searchPage = searchPage;
        }

        @Override
        public Inventory getInventory() { return null; }
    }

    private static class ShoppingBagHolder implements BazaarHolder {
        @Override
        public Inventory getInventory() { return null; }
    }

    private static class InstantAmountHolder implements BazaarHolder {
        final String productId;
        final boolean isBuy;
        final BazaarProduct.BazaarCategory category;
        final BazaarProduct.BazaarSubcategory subcategory;
        final int page;
        final String searchQuery;
        final int searchPage;

        InstantAmountHolder(
                String productId,
                boolean isBuy,
                BazaarProduct.BazaarCategory category,
                BazaarProduct.BazaarSubcategory subcategory,
                int page,
                String searchQuery,
                int searchPage
        ) {
            this.productId = productId;
            this.isBuy = isBuy;
            this.category = category;
            this.subcategory = subcategory;
            this.page = page;
            this.searchQuery = searchQuery;
            this.searchPage = searchPage;
        }

        @Override
        public Inventory getInventory() { return null; }
    }

    private static class OrderAmountHolder implements BazaarHolder {
        final String productId;
        final boolean isBuyOrder;
        final BazaarProduct.BazaarCategory category;
        final BazaarProduct.BazaarSubcategory subcategory;
        final int page;
        final String searchQuery;
        final int searchPage;

        OrderAmountHolder(
                String productId,
                boolean isBuyOrder,
                BazaarProduct.BazaarCategory category,
                BazaarProduct.BazaarSubcategory subcategory,
                int page,
                String searchQuery,
                int searchPage
        ) {
            this.productId = productId;
            this.isBuyOrder = isBuyOrder;
            this.category = category;
            this.subcategory = subcategory;
            this.page = page;
            this.searchQuery = searchQuery;
            this.searchPage = searchPage;
        }

        @Override
        public Inventory getInventory() { return null; }
    }

    private static class OrderPriceHolder implements BazaarHolder {
        final String productId;
        final int amount;
        final boolean isBuyOrder;
        final BazaarProduct.BazaarCategory category;
        final BazaarProduct.BazaarSubcategory subcategory;
        final int page;
        final String searchQuery;
        final int searchPage;

        OrderPriceHolder(
                String productId,
                int amount,
                boolean isBuyOrder,
                BazaarProduct.BazaarCategory category,
                BazaarProduct.BazaarSubcategory subcategory,
                int page,
                String searchQuery,
                int searchPage
        ) {
            this.productId = productId;
            this.amount = amount;
            this.isBuyOrder = isBuyOrder;
            this.category = category;
            this.subcategory = subcategory;
            this.page = page;
            this.searchQuery = searchQuery;
            this.searchPage = searchPage;
        }

        @Override
        public Inventory getInventory() { return null; }
    }

    private static class OrderConfirmHolder implements BazaarHolder {
        final String productId;
        final int amount;
        final double unitPrice;
        final boolean isBuyOrder;
        final BazaarProduct.BazaarCategory category;
        final BazaarProduct.BazaarSubcategory subcategory;
        final int page;
        final String searchQuery;
        final int searchPage;

        OrderConfirmHolder(
                String productId,
                int amount,
                double unitPrice,
                boolean isBuyOrder,
                BazaarProduct.BazaarCategory category,
                BazaarProduct.BazaarSubcategory subcategory,
                int page,
                String searchQuery,
                int searchPage
        ) {
            this.productId = productId;
            this.amount = amount;
            this.unitPrice = unitPrice;
            this.isBuyOrder = isBuyOrder;
            this.category = category;
            this.subcategory = subcategory;
            this.page = page;
            this.searchQuery = searchQuery;
            this.searchPage = searchPage;
        }

        @Override
        public Inventory getInventory() { return null; }
    }

    private static class PriceHistoryHolder implements BazaarHolder {
        final String productId;
        final BazaarProduct.BazaarCategory category;
        final BazaarProduct.BazaarSubcategory subcategory;
        final int page;
        final String searchQuery;
        final int searchPage;

        PriceHistoryHolder(
                String productId,
                BazaarProduct.BazaarCategory category,
                BazaarProduct.BazaarSubcategory subcategory,
                int page,
                String searchQuery,
                int searchPage
        ) {
            this.productId = productId;
            this.category = category;
            this.subcategory = subcategory;
            this.page = page;
            this.searchQuery = searchQuery;
            this.searchPage = searchPage;
        }

        @Override
        public Inventory getInventory() { return null; }
    }
}
