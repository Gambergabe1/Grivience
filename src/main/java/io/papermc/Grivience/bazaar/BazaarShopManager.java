package io.papermc.Grivience.bazaar;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.item.CustomArmorManager;
import io.papermc.Grivience.item.CustomArmorManager.ArmorPieceType;
import io.papermc.Grivience.item.CustomItemService;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.UUID;
import java.io.File;

public final class BazaarShopManager implements Listener {
    private static final String TITLE_MAIN = ChatColor.DARK_AQUA + "Bazaar Exchange";
    private static final String TITLE_CUSTOM = ChatColor.DARK_AQUA + "Bazaar - Plugin Items";
    private static final String TITLE_VANILLA = ChatColor.DARK_AQUA + "Bazaar - Base Server";
    private static final int INVENTORY_SIZE = 54;
    private static final int PAGE_SIZE = 45;

    private final GriviencePlugin plugin;
    private final CustomItemService customItemService;
    private final CustomArmorManager customArmorManager;
    private final BazaarOrderStore orderStore;
    private final NamespacedKey actionKey;
    private final NamespacedKey valueKey;
    private final File bazaarConfigFile;

    private boolean enabled = true;
    private boolean useVaultEconomy = true;
    private boolean allowLevelFallback = true;
    private double defaultVanillaBuyPrice = 32.0D;
    private double defaultCustomBuyPrice = 5000.0D;
    private double defaultSellMultiplier = 0.60D;

    private final Map<String, Double> materialBuyOverrides = new HashMap<>();
    private final Map<String, Double> materialSellOverrides = new HashMap<>();
    private final Map<String, Double> customBuyOverrides = new HashMap<>();
    private final Map<String, Double> customSellOverrides = new HashMap<>();
    private final Map<String, DemandTracker> demand = new HashMap<>();

    private List<String> customKeys = List.of();
    private List<Material> vanillaMaterials = List.of();

    public BazaarShopManager(
            GriviencePlugin plugin,
            CustomItemService customItemService,
            CustomArmorManager customArmorManager
    ) {
        this.plugin = plugin;
        this.customItemService = customItemService;
        this.customArmorManager = customArmorManager;
        this.orderStore = new BazaarOrderStore(plugin);
        this.actionKey = new NamespacedKey(plugin, "bazaar-action");
        this.valueKey = new NamespacedKey(plugin, "bazaar-value");
        this.bazaarConfigFile = new File(plugin.getDataFolder(), "bazaar.yml");
        reloadFromConfig();
    }

    public void reloadFromConfig() {
        ConfigurationSection section = loadBazaarSection();
        enabled = section == null || section.getBoolean("enabled", true);
        useVaultEconomy = section == null || section.getBoolean("use-vault-economy", true);
        allowLevelFallback = section == null || section.getBoolean("allow-level-fallback", true);
        defaultVanillaBuyPrice = positive(section == null ? 32.0D : section.getDouble("default-prices.vanilla-buy", 32.0D), 32.0D);
        defaultCustomBuyPrice = positive(section == null ? 5000.0D : section.getDouble("default-prices.custom-buy", 5000.0D), 5000.0D);
        defaultSellMultiplier = clamp(section == null ? 0.60D : section.getDouble("default-sell-multiplier", 0.60D), 0.0D, 1.0D);

        loadMaterialPrices(section, "material-price-overrides", materialBuyOverrides);
        loadMaterialPrices(section, "material-sell-overrides", materialSellOverrides);
        loadCustomPrices(section, "custom-price-overrides", customBuyOverrides);
        loadCustomPrices(section, "custom-sell-overrides", customSellOverrides);

        rebuildCatalogs();
    }

    private ConfigurationSection loadBazaarSection() {
        if (bazaarConfigFile.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(bazaarConfigFile);
            return yaml.getConfigurationSection("bazaar");
        }
        return plugin.getConfig().getConfigurationSection("bazaar");
    }

    public void shutdown() {
        orderStore.save();
    }
    public boolean isEnabled() {
        return enabled;
    }

    public void openMainMenu(Player player) {
        if (!enabled) {
            player.sendMessage(ChatColor.RED + "Bazaar is disabled in config.");
            return;
        }

        MenuHolder holder = new MenuHolder(MenuType.MAIN, 0);
        Inventory inventory = Bukkit.createInventory(holder, 27, TITLE_MAIN);
        holder.inventory = inventory;

        fillInventory(inventory, decorativePane(Material.BLACK_STAINED_GLASS_PANE));
        for (int slot : List.of(1, 3, 5, 7, 19, 21, 23, 25)) {
            inventory.setItem(slot, decorativePane(Material.CYAN_STAINED_GLASS_PANE));
        }

        inventory.setItem(4, taggedItem(
                Material.BELL,
                ChatColor.GOLD + "Bazaar Directory",
                List.of(
                        ChatColor.GRAY + "Trade plugin + vanilla items.",
                        ChatColor.GRAY + "Economy: " + currencyDescription()
                ),
                "noop",
                "",
                true
        ));
        inventory.setItem(11, taggedItem(
                Material.NETHER_STAR,
                ChatColor.AQUA + "Plugin Items",
                List.of(
                        ChatColor.GRAY + "Custom dungeon gear, crafting materials,",
                        ChatColor.GRAY + "reforge stones, and custom armor sets.",
                        ChatColor.GRAY + "Entries: " + ChatColor.YELLOW + customKeys.size(),
                        "",
                        ChatColor.YELLOW + "Click to browse"
                ),
                "open_custom",
                "0",
                true
        ));
        inventory.setItem(15, taggedItem(
                Material.CHEST,
                ChatColor.GREEN + "Base Server Items",
                List.of(
                        ChatColor.GRAY + "All vanilla item materials from this server.",
                        ChatColor.GRAY + "Entries: " + ChatColor.YELLOW + vanillaMaterials.size(),
                        "",
                        ChatColor.YELLOW + "Click to browse"
                ),
                "open_vanilla",
                "0",
                true
        ));
        inventory.setItem(13, taggedItem(
                Material.EMERALD,
                ChatColor.GREEN + "Trade Controls",
                List.of(
                        ChatColor.GRAY + "Left click: " + ChatColor.YELLOW + "Buy 1",
                        ChatColor.GRAY + "Shift + Left: " + ChatColor.YELLOW + "Buy stack",
                        ChatColor.GRAY + "Right click: " + ChatColor.YELLOW + "Sell 1",
                        ChatColor.GRAY + "Shift + Right: " + ChatColor.YELLOW + "Sell stack"
                ),
                "noop",
                "",
                false
        ));
        inventory.setItem(22, taggedItem(
                Material.BARRIER,
                ChatColor.RED + "Close",
                List.of(ChatColor.GRAY + "Close this menu."),
                "close",
                ""
        ));
        player.openInventory(inventory);
    }

    public void openCustomMenu(Player player, int page) {
        openCategoryMenu(player, MenuType.CUSTOM, page);
    }

    public void openVanillaMenu(Player player, int page) {
        openCategoryMenu(player, MenuType.VANILLA, page);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof MenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getClickedInventory().getType() == InventoryType.PLAYER) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir() || !clicked.hasItemMeta()) {
            return;
        }
        ItemMeta meta = clicked.getItemMeta();
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (action == null) {
            return;
        }
        String value = meta.getPersistentDataContainer().getOrDefault(valueKey, PersistentDataType.STRING, "");

        switch (action) {
            case "open_main" -> {
                playUiClick(player);
                openMainMenu(player);
            }
            case "open_custom" -> {
                playUiClick(player);
                openCustomMenu(player, parsePage(value));
            }
            case "open_vanilla" -> {
                playUiClick(player);
                openVanillaMenu(player, parsePage(value));
            }
            case "prev_page" -> {
                playUiClick(player);
                openCategoryMenu(player, holder.type, holder.page - 1);
            }
            case "next_page" -> {
                playUiClick(player);
                openCategoryMenu(player, holder.type, holder.page + 1);
            }
            case "refresh_page" -> {
                playUiClick(player);
                openCategoryMenu(player, holder.type, holder.page);
            }
            case "trade_entry" -> {
                if (holder.type == MenuType.CUSTOM) {
                    handleCustomTrade(player, value, event.getClick());
                } else if (holder.type == MenuType.VANILLA) {
                    handleVanillaTrade(player, value, event.getClick());
                }
            }
            case "close" -> {
                playUiClick(player);
                player.closeInventory();
            }
            case "noop" -> player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5F, 0.8F);
            default -> {
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }

    private void openCategoryMenu(Player player, MenuType type, int requestedPage) {
        if (!enabled) {
            player.sendMessage(ChatColor.RED + "Bazaar is disabled in config.");
            return;
        }

        int totalEntries = type == MenuType.CUSTOM ? customKeys.size() : vanillaMaterials.size();
        int pageCount = pageCount(totalEntries);
        int page = Math.max(0, Math.min(requestedPage, pageCount - 1));

        String titleBase = type == MenuType.CUSTOM ? TITLE_CUSTOM : TITLE_VANILLA;
        String title = titleBase + ChatColor.GRAY + " [" + (page + 1) + "/" + pageCount + "]";
        MenuHolder holder = new MenuHolder(type, page);
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, title);
        holder.inventory = inventory;

        fillInventory(inventory, decorativePane(Material.BLACK_STAINED_GLASS_PANE));
        fillRange(inventory, 45, 53, decorativePane(Material.GRAY_STAINED_GLASS_PANE));

        int startIndex = page * PAGE_SIZE;
        int endIndex = Math.min(totalEntries, startIndex + PAGE_SIZE);
        int slot = 0;
        for (int index = startIndex; index < endIndex; index++) {
            ItemStack entry = type == MenuType.CUSTOM
                    ? customEntryDisplay(customKeys.get(index))
                    : vanillaEntryDisplay(vanillaMaterials.get(index));
            if (entry != null) {
                inventory.setItem(slot, entry);
                slot++;
            }
        }

        boolean hasPrev = page > 0;
        boolean hasNext = page < pageCount - 1;
        inventory.setItem(45, taggedItem(
                hasPrev ? Material.ARROW : Material.BARRIER,
                hasPrev ? ChatColor.YELLOW + "Previous Page" : ChatColor.DARK_GRAY + "Previous Page",
                List.of(ChatColor.GRAY + "Go to page " + Math.max(1, page)),
                hasPrev ? "prev_page" : "noop",
                ""
        ));
        inventory.setItem(46, taggedItem(
                Material.COMPASS,
                ChatColor.AQUA + "Back To Bazaar",
                List.of(ChatColor.GRAY + "Return to category selector."),
                "open_main",
                ""
        ));
        inventory.setItem(49, taggedItem(
                Material.EMERALD,
                ChatColor.GREEN + "Trade Info",
                List.of(
                        ChatColor.GRAY + "Economy: " + currencyDescription(),
                        ChatColor.GRAY + "Left/Right click to buy/sell.",
                        ChatColor.GRAY + "Shift-click for stack trades."
                ),
                "noop",
                "",
                true
        ));
        inventory.setItem(51, taggedItem(
                Material.CLOCK,
                ChatColor.AQUA + "Refresh",
                List.of(ChatColor.GRAY + "Reload this page."),
                "refresh_page",
                ""
        ));
        inventory.setItem(52, taggedItem(
                Material.BARRIER,
                ChatColor.RED + "Close",
                List.of(ChatColor.GRAY + "Close this menu."),
                "close",
                ""
        ));
        inventory.setItem(53, taggedItem(
                hasNext ? Material.ARROW : Material.BARRIER,
                hasNext ? ChatColor.YELLOW + "Next Page" : ChatColor.DARK_GRAY + "Next Page",
                List.of(ChatColor.GRAY + "Go to page " + (page + 2)),
                hasNext ? "next_page" : "noop",
                ""
        ));

        player.openInventory(inventory);
    }

    private ItemStack customEntryDisplay(String key) {
        ItemStack template = customItemTemplate(key);
        if (template == null) {
            return null;
        }
        ItemStack display = template.clone();
        if (display.getAmount() <= 0) {
            display.setAmount(1);
        }
        ItemMeta meta = display.getItemMeta();
        List<String> lore = meta.hasLore() && meta.getLore() != null
                ? new ArrayList<>(meta.getLore())
                : new ArrayList<>();
        if (!lore.isEmpty()) {
            lore.add("");
        }
        double buyPrice = customBuyPrice(key);
        double sellPrice = customSellPrice(key, buyPrice);
        double topBuy = bestBuyPrice(customOrderKey(key));
        double topSell = bestSellPrice(customOrderKey(key));
        lore.add(ChatColor.GRAY + "Instant Buy: " + ChatColor.GOLD + formatAmount(buyPrice)
                + (Double.isNaN(topSell) ? "" : ChatColor.DARK_GRAY + " (Top Sell " + formatAmount(topSell) + ")"));
        lore.add(ChatColor.GRAY + "Instant Sell: " + ChatColor.GOLD + formatAmount(sellPrice)
                + (Double.isNaN(topBuy) ? "" : ChatColor.DARK_GRAY + " (Top Buy " + formatAmount(topBuy) + ")"));
        lore.add(ChatColor.DARK_GRAY + "Left=Buy  Right=Sell  Shift=Stack");
        lore.add(ChatColor.DARK_GRAY + "Middle=Buy Order  Drop=Sell Order");
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "trade_entry");
        meta.getPersistentDataContainer().set(valueKey, PersistentDataType.STRING, key);
        display.setItemMeta(meta);
        return display;
    }

    private ItemStack vanillaEntryDisplay(Material material) {
        ItemStack display = new ItemStack(material);
        ItemMeta meta = display.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + readableMaterialName(material));
        double buyPrice = materialBuyPrice(material);
        double sellPrice = materialSellPrice(material, buyPrice);
        double topBuy = bestBuyPrice(materialOrderKey(material));
        double topSell = bestSellPrice(materialOrderKey(material));
        meta.setLore(List.of(
                ChatColor.GRAY + "Instant Buy: " + ChatColor.GOLD + formatAmount(buyPrice)
                        + (Double.isNaN(topSell) ? "" : ChatColor.DARK_GRAY + " (Top Sell " + formatAmount(topSell) + ")"),
                ChatColor.GRAY + "Instant Sell: " + ChatColor.GOLD + formatAmount(sellPrice)
                        + (Double.isNaN(topBuy) ? "" : ChatColor.DARK_GRAY + " (Top Buy " + formatAmount(topBuy) + ")"),
                ChatColor.DARK_GRAY + "Left=Buy  Right=Sell  Shift=Stack",
                ChatColor.DARK_GRAY + "Middle=Buy Order  Drop=Sell Order"
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "trade_entry");
        meta.getPersistentDataContainer().set(valueKey, PersistentDataType.STRING, "vanilla:" + material.name());
        display.setItemMeta(meta);
        return display;
    }

    private void handleCustomTrade(Player player, String key, ClickType click) {
        if (!click.isLeftClick() && !click.isRightClick()) {
            return;
        }
        ItemStack template = customItemTemplate(key);
        if (template == null) {
            playUiError(player);
            player.sendMessage(ChatColor.RED + "Unknown plugin item entry.");
            return;
        }

        int amount = click.isShiftClick() ? Math.max(1, template.getMaxStackSize()) : 1;
        String orderKey = customOrderKey(key);
        if (click.isLeftClick()) {
            buyItem(player, orderKey, template, amount, customBuyPrice(key));
        } else {
            sellCustomItem(player, orderKey, key, amount, customSellPrice(key, customBuyPrice(key)));
        }
    }

    private void handleVanillaTrade(Player player, String materialKey, ClickType click) {
        if (!click.isLeftClick() && !click.isRightClick()) {
            return;
        }
        Material material = Material.getMaterial(materialKey);
        if (material == null || material.isAir() || !material.isItem()) {
            playUiError(player);
            player.sendMessage(ChatColor.RED + "Invalid material entry.");
            return;
        }

        int amount = click.isShiftClick() ? Math.max(1, material.getMaxStackSize()) : 1;
        String orderKey = materialOrderKey(material);
        if (click.isLeftClick()) {
            buyItem(player, orderKey, new ItemStack(material), amount, materialBuyPrice(material));
        } else {
            sellVanillaItem(player, orderKey, material, amount, materialSellPrice(material, materialBuyPrice(material)));
        }
    }

    private void buyItem(Player player, String orderKey, ItemStack template, int amount, double unitPrice) {
        int normalizedAmount = Math.max(1, amount);
        FillResult market = buyFromSellOrders(player, orderKey, template, normalizedAmount);
        int filled = market.filled();
        int remaining = normalizedAmount - filled;
        if (remaining <= 0) {
            return;
        }
        double totalPrice = unitPrice * remaining;
        CurrencyMode currencyMode = currencyMode();
        if (currencyMode == CurrencyMode.NONE) {
            playUiError(player);
            player.sendMessage(ChatColor.RED + "No Bazaar currency provider is available.");
            return;
        }
        if (!hasFunds(player, totalPrice, currencyMode)) {
            playUiError(player);
            player.sendMessage(ChatColor.RED + "You do not have enough " + currencyWord(currencyMode) + ".");
            player.sendMessage(ChatColor.GRAY + "Cost: " + ChatColor.GOLD + formatAmount(totalPrice));
            return;
        }
        if (!withdraw(player, totalPrice, currencyMode)) {
            playUiError(player);
            player.sendMessage(ChatColor.RED + "Transaction failed while charging your " + currencyWord(currencyMode) + ".");
            return;
        }

        ItemStack toGive = template.clone();
        toGive.setAmount(remaining);
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(toGive);
        leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));

        playUiSuccess(player);
        player.sendMessage(ChatColor.GREEN + "Bought "
                + ChatColor.YELLOW + normalizedAmount + "x "
                + itemDisplayName(template)
                + ChatColor.GREEN + " for "
                + ChatColor.GOLD + formatAmount(totalPrice + market.totalCoins()) + ChatColor.GREEN + ".");
        recordDemand(orderKey, remaining); // buying from NPC increases demand
    }

    private void sellCustomItem(Player player, String orderKey, String key, int amount, double unitPrice) {
        CurrencyMode currencyMode = currencyMode();
        if (currencyMode == CurrencyMode.NONE) {
            playUiError(player);
            player.sendMessage(ChatColor.RED + "No Bazaar currency provider is available.");
            return;
        }

        int requested = Math.max(1, amount);
        ItemStack template = customItemTemplate(key);
        if (template == null) {
            playUiError(player);
            player.sendMessage(ChatColor.RED + "Unknown plugin item entry.");
            return;
        }
        int available = countMatchingInInventory(player.getInventory(), requested, item -> matchesCustomEntry(key, item));
        if (available <= 0) {
            playUiError(player);
            player.sendMessage(ChatColor.RED + "You do not have that item in your inventory.");
            return;
        }

        int sold = removeMatchingFromInventory(player.getInventory(), available, item -> matchesCustomEntry(key, item));
        if (sold <= 0) {
            playUiError(player);
            player.sendMessage(ChatColor.RED + "Unable to remove the item from inventory.");
            return;
        }

        FillResult res = sellIntoBuyOrders(player, orderKey, template, sold);
        int remaining = sold - res.filled();
        double payout = unitPrice * Math.max(0, remaining);
        if (payout > 0 && !deposit(player, payout, currencyMode)) {
            returnItems(player, template, sold);
            playUiError(player);
            player.sendMessage(ChatColor.RED + "Transaction failed while paying your " + currencyWord(currencyMode) + ".");
            return;
        }
        recordDemand(orderKey, -remaining); // selling into NPC lowers demand

        playUiSuccess(player);
        player.sendMessage(ChatColor.GREEN + "Sold "
                + ChatColor.YELLOW + sold + "x "
                + itemDisplayName(template)
                + ChatColor.GREEN + " for "
                + ChatColor.GOLD + formatAmount(payout + res.totalCoins()) + ChatColor.GREEN + ".");
    }

    private void sellVanillaItem(Player player, String orderKey, Material material, int amount, double unitPrice) {
        CurrencyMode currencyMode = currencyMode();
        if (currencyMode == CurrencyMode.NONE) {
            playUiError(player);
            player.sendMessage(ChatColor.RED + "No Bazaar currency provider is available.");
            return;
        }

        int requested = Math.max(1, amount);
        int available = countMatchingInInventory(player.getInventory(), requested, item -> isVanillaMatch(item, material));
        if (available <= 0) {
            playUiError(player);
            player.sendMessage(ChatColor.RED + "You do not have that item in your inventory.");
            return;
        }

        int sold = removeMatchingFromInventory(player.getInventory(), available, item -> isVanillaMatch(item, material));
        if (sold <= 0) {
            playUiError(player);
            player.sendMessage(ChatColor.RED + "Unable to remove the item from inventory.");
            return;
        }

        FillResult res = sellIntoBuyOrders(player, orderKey, new ItemStack(material), sold);
        int remaining = sold - res.filled();
        double payout = unitPrice * Math.max(0, remaining);
        if (payout > 0 && !deposit(player, payout, currencyMode)) {
            returnItems(player, new ItemStack(material), sold);
            playUiError(player);
            player.sendMessage(ChatColor.RED + "Transaction failed while paying your " + currencyWord(currencyMode) + ".");
            return;
        }
        recordDemand(orderKey, -remaining); // selling into NPC lowers demand

        playUiSuccess(player);
        player.sendMessage(ChatColor.GREEN + "Sold "
                + ChatColor.YELLOW + sold + "x "
                + ChatColor.GREEN + readableMaterialName(material)
                + ChatColor.GREEN + " for "
                + ChatColor.GOLD + formatAmount(payout + res.totalCoins()) + ChatColor.GREEN + ".");
    }

    private boolean ordersEnabled() {
        return currencyMode() == CurrencyMode.VAULT;
    }

    private String customOrderKey(String key) {
        return key.toLowerCase(Locale.ROOT);
    }

    private String materialOrderKey(Material material) {
        return "vanilla:" + material.name().toLowerCase(Locale.ROOT);
    }

    private void createBuyOrderForEntry(Player player, String orderKey, ItemStack template, double unitPrice) {
        if (!ordersEnabled()) {
            playUiError(player);
            player.sendMessage(ChatColor.RED + "Buy/Sell orders require Vault economy enabled.");
            return;
        }
        int amount = Math.max(1, template.getMaxStackSize());
        double total = unitPrice * amount;
        if (!hasFunds(player, total, CurrencyMode.VAULT)) {
            playUiError(player);
            player.sendMessage(ChatColor.RED + "Not enough coins to create a buy order.");
            player.sendMessage(ChatColor.GRAY + "Cost: " + ChatColor.GOLD + formatAmount(total));
            return;
        }
        if (!withdraw(player, total, CurrencyMode.VAULT)) {
            playUiError(player);
            player.sendMessage(ChatColor.RED + "Failed to withdraw coins for the buy order.");
            return;
        }
        BazaarOrder order = new BazaarOrder(
                orderStore.nextId(),
                player.getUniqueId(),
                orderKey,
                unitPrice,
                amount,
                BazaarOrder.Type.BUY,
                System.currentTimeMillis(),
                null
        );
        orderStore.addOrder(order);
        playUiSuccess(player);
        player.sendMessage(ChatColor.GREEN + "Created buy order #" + order.getId()
                + ChatColor.GREEN + " for " + ChatColor.YELLOW + amount + "x " + itemDisplayName(template)
                + ChatColor.GREEN + " at " + ChatColor.GOLD + formatAmount(unitPrice) + ChatColor.GREEN + " each.");
    }

    private void createSellOrderForEntry(
            Player player,
            String orderKey,
            ItemStack template,
            double unitPrice,
            Predicate<ItemStack> predicate
    ) {
        if (!ordersEnabled()) {
            playUiError(player);
            player.sendMessage(ChatColor.RED + "Buy/Sell orders require Vault economy enabled.");
            return;
        }
        int maxStack = Math.max(1, template.getMaxStackSize());
        int available = countMatchingInInventory(player.getInventory(), maxStack, predicate);
        if (available <= 0) {
            playUiError(player);
            player.sendMessage(ChatColor.RED + "You do not have that item in your inventory.");
            return;
        }
        int removed = removeMatchingFromInventory(player.getInventory(), Math.min(maxStack, available), predicate);
        if (removed <= 0) {
            playUiError(player);
            player.sendMessage(ChatColor.RED + "Unable to remove the item from inventory.");
            return;
        }
        BazaarOrder order = new BazaarOrder(
                orderStore.nextId(),
                player.getUniqueId(),
                orderKey,
                unitPrice,
                removed,
                BazaarOrder.Type.SELL,
                System.currentTimeMillis(),
                template.clone()
        );
        orderStore.addOrder(order);
        playUiSuccess(player);
        player.sendMessage(ChatColor.GREEN + "Created sell order #" + order.getId()
                + ChatColor.GREEN + " for " + ChatColor.YELLOW + removed + "x " + itemDisplayName(template)
                + ChatColor.GREEN + " at " + ChatColor.GOLD + formatAmount(unitPrice) + ChatColor.GREEN + " each.");
    }

    private FillResult buyFromSellOrders(Player buyer, String orderKey, ItemStack template, int amount) {
        if (!ordersEnabled() || amount <= 0) {
            return new FillResult(0, 0.0D);
        }
        List<BazaarOrder> orders = orderStore.ordersFor(orderKey, BazaarOrder.Type.SELL);
        int acquired = 0;
        double spent = 0.0D;
        for (BazaarOrder order : orders) {
            if (acquired >= amount) {
                break;
            }
            int take = Math.min(order.getRemainingAmount(), amount - acquired);
            double cost = order.getUnitPrice() * take;
            if (!hasFunds(buyer, cost, CurrencyMode.VAULT)) {
                continue;
            }
            if (!withdraw(buyer, cost, CurrencyMode.VAULT)) {
                continue;
            }
            if (!depositToUuid(order.getOwner(), cost)) {
                deposit(buyer, cost, CurrencyMode.VAULT);
                continue;
            }
            ItemStack base = order.getStoredItem() != null ? order.getStoredItem() : template;
            returnItems(buyer, base, take);
            order.decrement(take);
            acquired += take;
            spent += cost;
            if (order.getRemainingAmount() <= 0) {
                orderStore.removeOrder(order.getId());
            } else {
                orderStore.updateOrder(order);
            }
            notifyOrderOwner(order.getOwner(), ChatColor.YELLOW + "Your sell order #" + order.getId()
                    + " sold " + take + "x " + itemDisplayName(base) + ".");
        }
        return new FillResult(acquired, spent);
    }

    private FillResult sellIntoBuyOrders(Player seller, String orderKey, ItemStack template, int amount) {
        if (!ordersEnabled() || amount <= 0) {
            return new FillResult(0, 0.0D);
        }
        List<BazaarOrder> orders = orderStore.ordersFor(orderKey, BazaarOrder.Type.BUY);
        int remaining = amount;
        int sold = 0;
        double revenue = 0.0D;
        for (BazaarOrder order : orders) {
            if (remaining <= 0) {
                break;
            }
            int toSell = Math.min(order.getRemainingAmount(), remaining);
            if (toSell <= 0) {
                continue;
            }
            double payout = order.getUnitPrice() * toSell;
            if (!deposit(seller, payout, CurrencyMode.VAULT)) {
                break;
            }
            deliverToOrderOwner(order, template, toSell);
            order.decrement(toSell);
            sold += toSell;
            revenue += payout;
            remaining -= toSell;
            if (order.getRemainingAmount() <= 0) {
                orderStore.removeOrder(order.getId());
            } else {
                orderStore.updateOrder(order);
            }
            notifyOrderOwner(order.getOwner(), ChatColor.YELLOW + "Your buy order #" + order.getId()
                    + " filled " + toSell + "x " + itemDisplayName(template) + ".");
        }
        return new FillResult(sold, revenue);
    }

    private void deliverToOrderOwner(BazaarOrder order, ItemStack template, int amount) {
        Player ownerPlayer = Bukkit.getPlayer(order.getOwner());
        ItemStack base = order.getStoredItem() != null ? order.getStoredItem() : template;
        if (ownerPlayer != null && ownerPlayer.isOnline()) {
            returnItems(ownerPlayer, base, amount);
            ownerPlayer.sendMessage(ChatColor.GREEN + "Your buy order #" + order.getId() + " received "
                    + ChatColor.YELLOW + amount + "x " + itemDisplayName(base) + ChatColor.GREEN + ".");
            return;
        }
        orderStore.recordDelivery(order.getOwner(), order.getItemKey(), amount);
    }

    private void deliverPending(Player player) {
        Map<String, Integer> pending = orderStore.consumeDeliveries(player.getUniqueId());
        if (pending.isEmpty()) {
            return;
        }
        int delivered = 0;
        for (Map.Entry<String, Integer> entry : pending.entrySet()) {
            ItemStack template = templateFromOrderKey(entry.getKey());
            if (template == null) {
                continue;
            }
            returnItems(player, template, entry.getValue());
            delivered += entry.getValue();
        }
        if (delivered > 0) {
            player.sendMessage(ChatColor.GREEN + "Delivered " + ChatColor.YELLOW + delivered + ChatColor.GREEN + " items from your buy orders.");
        }
    }

    private ItemStack templateFromOrderKey(String key) {
        if (key.startsWith("vanilla:")) {
            String raw = key.substring("vanilla:".length());
            Material material = Material.getMaterial(raw.toUpperCase(Locale.ROOT));
            if (material == null || material.isAir() || !material.isItem()) {
                return null;
            }
            return new ItemStack(material);
        }
        return customItemTemplate(key);
    }

    private boolean depositToUuid(UUID uuid, double amount) {
        Economy economy = economy();
        if (economy == null) {
            return false;
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        EconomyResponse response = economy.depositPlayer(offlinePlayer, amount);
        return response.transactionSuccess();
    }

    private void notifyOrderOwner(UUID owner, String message) {
        Player player = Bukkit.getPlayer(owner);
        if (player != null && player.isOnline()) {
            player.sendMessage(message);
        }
    }
    private int removeMatchingFromInventory(PlayerInventory inventory, int limit, Predicate<ItemStack> predicate) {
        int remaining = Math.max(0, limit);
        int removed = 0;
        for (int slot = 0; slot < 36 && remaining > 0; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType().isAir()) {
                continue;
            }
            if (!predicate.test(item)) {
                continue;
            }
            int take = Math.min(remaining, item.getAmount());
            removed += take;
            remaining -= take;
            int left = item.getAmount() - take;
            if (left <= 0) {
                inventory.setItem(slot, null);
            } else {
                item.setAmount(left);
            }
        }
        return removed;
    }

    private int countMatchingInInventory(PlayerInventory inventory, int limit, Predicate<ItemStack> predicate) {
        int remaining = Math.max(0, limit);
        int counted = 0;
        for (int slot = 0; slot < 36 && remaining > 0; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType().isAir()) {
                continue;
            }
            if (!predicate.test(item)) {
                continue;
            }
            int taken = Math.min(remaining, item.getAmount());
            counted += taken;
            remaining -= taken;
        }
        return counted;
    }

    private void returnItems(Player player, ItemStack template, int amount) {
        if (template == null || amount <= 0) {
            return;
        }
        int remaining = amount;
        int maxStack = Math.max(1, template.getMaxStackSize());
        while (remaining > 0) {
            int stackAmount = Math.min(remaining, maxStack);
            ItemStack stack = template.clone();
            stack.setAmount(stackAmount);
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
            leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            remaining -= stackAmount;
        }
    }

    private boolean matchesCustomEntry(String key, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        if (key.startsWith("item:")) {
            String expected = key.substring("item:".length()).toUpperCase(Locale.ROOT);
            String customId = customItemService.itemId(item);
            if (customId == null) {
                return false;
            }
            return customId.equalsIgnoreCase(expected);
        }

        if (!key.startsWith("armor:") || customArmorManager == null) {
            return false;
        }
        String[] split = key.split(":", 3);
        if (split.length < 3) {
            return false;
        }
        String armorSetId = customArmorManager.getArmorSetId(item);
        ArmorPieceType pieceType = customArmorManager.getArmorPieceType(item);
        if (armorSetId == null || pieceType == null) {
            return false;
        }
        return armorSetId.equalsIgnoreCase(split[1]) && pieceType.name().equalsIgnoreCase(split[2]);
    }

    private boolean isVanillaMatch(ItemStack item, Material material) {
        if (item == null || item.getType() != material) {
            return false;
        }
        return customItemService.itemId(item) == null
                && (customArmorManager == null || !customArmorManager.isCustomArmor(item));
    }

    private ItemStack customItemTemplate(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        if (key.startsWith("item:")) {
            String raw = key.substring("item:".length());
            return customItemService.createItemByKey(raw);
        }
        if (key.startsWith("armor:") && customArmorManager != null) {
            String[] split = key.split(":", 3);
            if (split.length < 3) {
                return null;
            }
            ArmorPieceType pieceType;
            try {
                pieceType = ArmorPieceType.valueOf(split[2].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
            return customArmorManager.createArmorPiece(split[1], pieceType);
        }
        return null;
    }

    private double bestSellPrice(String orderKey) {
        if (!ordersEnabled()) return Double.NaN;
        return orderStore.bestPrice(orderKey, BazaarOrder.Type.SELL);
    }

    private double bestBuyPrice(String orderKey) {
        if (!ordersEnabled()) return Double.NaN;
        return orderStore.bestPrice(orderKey, BazaarOrder.Type.BUY);
    }

    private double materialBuyPrice(Material material) {
        double best = bestSellPrice(materialOrderKey(material));
        if (!Double.isNaN(best)) return best;
        double base = materialBuyOverrides.getOrDefault(material.name(), defaultVanillaBuyPrice);
        return applyDemand(materialOrderKey(material), base);
    }

    private double materialSellPrice(Material material, double buyPrice) {
        double best = bestBuyPrice(materialOrderKey(material));
        if (!Double.isNaN(best)) return best;
        double base = materialSellOverrides.getOrDefault(material.name(), Math.max(0.0D, buyPrice * defaultSellMultiplier));
        return applyDemand(materialOrderKey(material), base);
    }

    private double customBuyPrice(String key) {
        String overrideKey = normalizeCustomOverrideKey(key);
        double best = bestSellPrice(customOrderKey(overrideKey));
        if (!Double.isNaN(best)) return best;
        double base = customBuyOverrides.getOrDefault(overrideKey, defaultCustomBuyPrice);
        return applyDemand(customOrderKey(overrideKey), base);
    }

    private double customSellPrice(String key, double buyPrice) {
        String overrideKey = normalizeCustomOverrideKey(key);
        double best = bestBuyPrice(customOrderKey(overrideKey));
        if (!Double.isNaN(best)) return best;
        double base = customSellOverrides.getOrDefault(overrideKey, Math.max(0.0D, buyPrice * defaultSellMultiplier));
        return applyDemand(customOrderKey(overrideKey), base);
    }

    private String normalizeCustomOverrideKey(String key) {
        if (key.startsWith("item:")) {
            return normalizeKey(key.substring("item:".length()));
        }
        if (key.startsWith("armor:")) {
            String[] split = key.split(":", 3);
            if (split.length >= 3) {
                return "armor_" + normalizeKey(split[1]) + "_" + normalizeKey(split[2]);
            }
        }
        return normalizeKey(key);
    }

    private CurrencyMode currencyMode() {
        if (useVaultEconomy && economy() != null) {
            return CurrencyMode.VAULT;
        }
        if (allowLevelFallback) {
            return CurrencyMode.LEVELS;
        }
        return CurrencyMode.NONE;
    }

    private boolean hasFunds(Player player, double amount, CurrencyMode mode) {
        return switch (mode) {
            case VAULT -> {
                Economy economy = economy();
                yield economy != null && economy.has(player, amount);
            }
            case LEVELS -> player.getLevel() >= levelCost(amount);
            case NONE -> false;
        };
    }

    private boolean withdraw(Player player, double amount, CurrencyMode mode) {
        return switch (mode) {
            case VAULT -> {
                Economy economy = economy();
                if (economy == null) {
                    yield false;
                }
                EconomyResponse response = economy.withdrawPlayer(player, amount);
                yield response.transactionSuccess();
            }
            case LEVELS -> {
                int cost = levelCost(amount);
                if (player.getLevel() < cost) {
                    yield false;
                }
                player.setLevel(Math.max(0, player.getLevel() - cost));
                yield true;
            }
            case NONE -> false;
        };
    }

    private boolean deposit(Player player, double amount, CurrencyMode mode) {
        return switch (mode) {
            case VAULT -> {
                Economy economy = economy();
                if (economy == null) {
                    yield false;
                }
                EconomyResponse response = economy.depositPlayer(player, amount);
                yield response.transactionSuccess();
            }
            case LEVELS -> {
                int payout = levelPayout(amount);
                player.setLevel(player.getLevel() + payout);
                yield true;
            }
            case NONE -> false;
        };
    }

    private Economy economy() {
        RegisteredServiceProvider<Economy> registration = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (registration == null) {
            return null;
        }
        return registration.getProvider();
    }

    private String currencyDescription() {
        return switch (currencyMode()) {
            case VAULT -> ChatColor.GREEN + "Vault Coins";
            case LEVELS -> ChatColor.YELLOW + "XP Levels";
            case NONE -> ChatColor.RED + "Unavailable";
        };
    }

    private String currencyWord(CurrencyMode mode) {
        return switch (mode) {
            case VAULT -> "coins";
            case LEVELS -> "levels";
            case NONE -> "currency";
        };
    }

    private int parsePage(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private int pageCount(int size) {
        return Math.max(1, (size + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private String itemDisplayName(ItemStack item) {
        if (item == null) {
            return ChatColor.WHITE + "Unknown Item";
        }
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return ChatColor.WHITE + readableMaterialName(item.getType());
    }

    private String readableMaterialName(Material material) {
        String lower = material.name().toLowerCase(Locale.ROOT);
        String[] words = lower.split("_");
        List<String> parts = new ArrayList<>(words.length);
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            parts.add(Character.toUpperCase(word.charAt(0)) + word.substring(1));
        }
        return String.join(" ", parts);
    }

    private String formatAmount(double value) {
        CurrencyMode mode = currencyMode();
        if (mode == CurrencyMode.VAULT) {
            Economy economy = economy();
            if (economy != null) {
                return economy.format(value);
            }
        }
        if (mode == CurrencyMode.LEVELS) {
            return levelCost(value) + " levels";
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private int levelCost(double value) {
        return Math.max(1, (int) Math.ceil(Math.max(0.0D, value)));
    }

    private int levelPayout(double value) {
        return Math.max(1, (int) Math.ceil(Math.max(0.0D, value)));
    }

    private void rebuildCatalogs() {
        List<String> rebuiltCustom = new ArrayList<>();
        for (String key : customItemService.allItemKeys()) {
            rebuiltCustom.add("item:" + normalizeKey(key));
        }
        if (customArmorManager != null) {
            List<String> setIds = new ArrayList<>(customArmorManager.getArmorSets().keySet());
            setIds.sort(String.CASE_INSENSITIVE_ORDER);
            for (String setId : setIds) {
                for (ArmorPieceType pieceType : ArmorPieceType.values()) {
                    ItemStack piece = customArmorManager.createArmorPiece(setId, pieceType);
                    if (piece != null) {
                        rebuiltCustom.add("armor:" + setId + ":" + pieceType.name().toLowerCase(Locale.ROOT));
                    }
                }
            }
        }
        rebuiltCustom.sort(Comparator.comparing(this::customSortLabel, String.CASE_INSENSITIVE_ORDER));
        customKeys = List.copyOf(rebuiltCustom);

        List<Material> rebuiltMaterials = Arrays.stream(Material.values())
                .filter(Material::isItem)
                .filter(material -> !material.isAir())
                .filter(material -> !material.isLegacy())
                .sorted(Comparator.comparing(this::readableMaterialName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        vanillaMaterials = rebuiltMaterials;
    }

    private String customSortLabel(String key) {
        ItemStack template = customItemTemplate(key);
        String label = itemDisplayName(template);
        String plain = ChatColor.stripColor(label);
        if (plain == null || plain.isBlank()) {
            return key;
        }
        return plain;
    }

    private void loadMaterialPrices(ConfigurationSection root, String path, Map<String, Double> output) {
        output.clear();
        if (root == null) {
            return;
        }
        ConfigurationSection section = root.getConfigurationSection(path);
        if (section == null) {
            return;
        }
        for (String rawKey : section.getKeys(false)) {
            String normalized = rawKey.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
            Material material = Material.getMaterial(normalized);
            if (material == null) {
                continue;
            }
            double value = section.getDouble(rawKey, -1.0D);
            if (value > 0.0D) {
                output.put(material.name(), value);
            }
        }
    }

    private void loadCustomPrices(ConfigurationSection root, String path, Map<String, Double> output) {
        output.clear();
        if (root == null) {
            return;
        }
        ConfigurationSection section = root.getConfigurationSection(path);
        if (section == null) {
            return;
        }
        for (String rawKey : section.getKeys(false)) {
            double value = section.getDouble(rawKey, -1.0D);
            if (value <= 0.0D) {
                continue;
            }
            output.put(normalizeKey(rawKey), value);
        }
    }

    private String normalizeKey(String key) {
        return key.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private ItemStack decorativePane(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.BLACK + " ");
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    private void fillInventory(Inventory inventory, ItemStack filler) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler.clone());
        }
    }

    private void fillRange(Inventory inventory, int start, int end, ItemStack filler) {
        for (int slot = start; slot <= end && slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler.clone());
        }
    }

    private ItemStack taggedItem(Material material, String name, List<String> lore, String action, String value) {
        return taggedItem(material, name, lore, action, value, false);
    }

    private ItemStack taggedItem(
            Material material,
            String name,
            List<String> lore,
            String action,
            String value,
            boolean glow
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        if (glow) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
        }
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        meta.getPersistentDataContainer().set(valueKey, PersistentDataType.STRING, value);
        item.setItemMeta(meta);
        return item;
    }

    private void playUiClick(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7F, 1.1F);
    }

    private void playUiSuccess(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.1F);
    }

    private void playUiError(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7F, 0.8F);
    }

    private double positive(double value, double fallback) {
        return value > 0.0D ? value : fallback;
    }

    private double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        return Math.min(max, value);
    }

    private void recordDemand(String orderKey, int delta) {
        if (orderKey == null) return;
        DemandTracker tracker = demand.computeIfAbsent(orderKey, k -> new DemandTracker());
        tracker.add(delta);
    }

    private double applyDemand(String orderKey, double basePrice) {
        DemandTracker tracker = demand.get(orderKey);
        if (tracker == null) {
            return basePrice;
        }
        double factor = tracker.multiplier();
        return Math.max(0.0D, basePrice * factor);
    }

    private static final class DemandTracker {
        private int net;
        private long last;

        void add(int delta) {
            long now = System.currentTimeMillis();
            decay(now);
            net += delta;
            last = now;
        }

        double multiplier() {
            long now = System.currentTimeMillis();
            decay(now);
            double capped = Math.max(-5.0D, Math.min(5.0D, net / 50.0D)); // +/-5 ticks
            double result = 1.0D + capped * 0.05D;
            return Math.max(0.75D, Math.min(1.25D, result));
        }

        private void decay(long now) {
            if (last == 0) {
                last = now;
                return;
            }
            long elapsed = now - last;
            if (elapsed > 300_000L) { // 5 minutes
                net = 0;
                last = now;
            }
        }
    }

    private static final class FillResult {
        private final int amount;
        private final double revenue;

        private FillResult(int amount, double revenue) {
            this.amount = amount;
            this.revenue = revenue;
        }

        public int filled() {
            return amount;
        }

        public double totalCoins() {
            return revenue;
        }
    }
    private enum CurrencyMode {
        VAULT,
        LEVELS,
        NONE
    }

    private enum MenuType {
        MAIN,
        CUSTOM,
        VANILLA
    }

    private static final class MenuHolder implements InventoryHolder {
        private final MenuType type;
        private final int page;
        private Inventory inventory;

        private MenuHolder(MenuType type, int page) {
            this.type = type;
            this.page = page;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

}





















