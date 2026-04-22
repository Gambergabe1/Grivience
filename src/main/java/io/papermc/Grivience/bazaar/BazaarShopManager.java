package io.papermc.Grivience.bazaar;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.item.CustomArmorManager;
import io.papermc.Grivience.item.CustomArmorType;
import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.item.CustomToolType;
import io.papermc.Grivience.item.CustomWeaponType;
import io.papermc.Grivience.item.GrapplingHookType;
import io.papermc.Grivience.item.MiningItemType;
import io.papermc.Grivience.item.ReforgeStoneType;
import io.papermc.Grivience.skyblock.economy.ProfileEconomyService;
import io.papermc.Grivience.skyblock.island.Island;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Main Bazaar manager implementing Skyblock's Bazaar system with 100% accuracy.
 * Features:
 * - Instant Buy/Sell at market prices
 * - Buy/Sell Orders with price-time priority matching
 * - Dynamic pricing based on supply/demand
 * - Price history tracking
 * - Shopping bag claim system
 * - Category-based product organization
 */
public final class BazaarShopManager {
    private static final String TITLE_MAIN = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Bazaar";
    
    private final GriviencePlugin plugin;
    private final CustomItemService customItemService;
    private final CustomArmorManager customArmorManager;
    private final BazaarOrderBook orderBook;
    private final BazaarShoppingBag shoppingBag;
    private final BazaarPriceHistory priceHistory;
    private final BazaarProductCache productCache;
    private final BazaarAccountUpgrades accountUpgrades;
    private final ProfileEconomyService profileEconomy;
    private final File bazaarConfigFile;
    private final File productsConfigFile;
    private final NamespacedKey navigationItemKey;
    
    // Configuration
    private boolean enabled = true;
    // Hypixel baseline: 14 orders, 71,680 max items per transaction/order.
    private int maxOrdersPerPlayer = 14;
    private int maxOrderAmount = 71680;
    private double minOrderValue = 7.0;
    private double maxPriceSpreadPercent = 5.0;
    private double defaultSellMultiplier = 0.60;
    private double instantBuyPremium = 1.05;
    private double instantSellDiscount = 0.95;
    private double defaultVanillaBuyPrice = 32.0;
    private double defaultVanillaSellPrice = 19.2;
    private double defaultCustomBuyPrice = 5000.0;
    private double defaultCustomSellPrice = 3000.0;
    private boolean npcShopEnabled = true;
    private double npcShopSellMultiplier = 0.90;
    private boolean npcShopApplySellTax = true;
    private Map<String, Double> materialBuyOverrides = new HashMap<>();
    private Map<String, Double> materialSellOverrides = new HashMap<>();
    private Map<String, Double> customBuyOverrides = new HashMap<>();
    private Map<String, Double> customSellOverrides = new HashMap<>();
    private int cleanupIntervalMinutes = 10;
    private BukkitTask cleanupTask;
    
    // Product catalog
    private final Map<String, BazaarProduct> products = new HashMap<>();
    private final Map<BazaarProduct.BazaarCategory, List<BazaarProduct>> productsByCategory = new EnumMap<>(BazaarProduct.BazaarCategory.class);
    
    public BazaarShopManager(
            GriviencePlugin plugin,
            CustomItemService customItemService,
            CustomArmorManager customArmorManager
    ) {
        this.plugin = plugin;
        this.customItemService = customItemService;
        this.customArmorManager = customArmorManager;
        this.orderBook = new BazaarOrderBook(plugin);
        this.shoppingBag = new BazaarShoppingBag(plugin.getDataFolder());
        this.priceHistory = new BazaarPriceHistory(plugin.getDataFolder());
        this.productCache = new BazaarProductCache(this);
        this.accountUpgrades = new BazaarAccountUpgrades(plugin.getDataFolder());
        this.profileEconomy = new ProfileEconomyService(plugin);
        this.bazaarConfigFile = new File(plugin.getDataFolder(), "bazaar.yml");
        this.productsConfigFile = new File(plugin.getDataFolder(), "bazaar_products.yml");
        this.navigationItemKey = new NamespacedKey(plugin, "navigation_item");
        
        reloadFromConfig();
        initializeProducts();
    }
    
    public void reloadFromConfig() {
        ConfigurationSection section = loadBazaarSection();
        enabled = section == null || section.getBoolean("enabled", true);
        maxOrdersPerPlayer = section == null ? 14 : Math.max(1, section.getInt("max-orders-per-player", 14));
        maxOrderAmount = section == null ? 71680 : Math.max(1, section.getInt("max-order-amount", 71680));
        minOrderValue = section == null ? 7.0 : Math.max(0.0, section.getDouble("min-order-value", 7.0));
        maxPriceSpreadPercent = section == null ? 5.0 : Math.max(0.0, section.getDouble("max-price-spread-percent", 5.0));
        defaultSellMultiplier = section == null ? 0.60 : Math.max(0.0, section.getDouble("default-sell-multiplier", 0.60));
        instantBuyPremium = section == null ? 1.05 : section.getDouble("instant-buy-premium", section.getDouble("instant-buy-multiplier", 1.05));
        instantSellDiscount = section == null ? 0.95 : section.getDouble("instant-sell-discount", section.getDouble("instant-sell-multiplier", 0.95));

        ConfigurationSection defaults = section == null ? null : section.getConfigurationSection("default-prices");
        defaultVanillaBuyPrice = defaults == null ? 32.0 : defaults.getDouble("vanilla-buy", 32.0);
        defaultVanillaSellPrice = defaults == null ? 19.2 : defaults.getDouble("vanilla-sell", 19.2);
        defaultCustomBuyPrice = defaults == null ? 5000.0 : defaults.getDouble("custom-buy", 5000.0);
        defaultCustomSellPrice = defaults == null ? 3000.0 : defaults.getDouble("custom-sell", 3000.0);

        ConfigurationSection npcShopSection = section == null ? null : section.getConfigurationSection("npc-shop");
        npcShopEnabled = npcShopSection == null || npcShopSection.getBoolean("enabled", true);
        npcShopSellMultiplier = npcShopSection == null ? 0.90 : npcShopSection.getDouble("sell-multiplier", 0.90);
        npcShopApplySellTax = npcShopSection == null || npcShopSection.getBoolean("apply-bazaar-sell-tax", true);

        materialBuyOverrides = readDoubleMap(section, "material-price-overrides", key -> key.toUpperCase(Locale.ROOT));
        materialSellOverrides = readDoubleMap(section, "material-sell-overrides", key -> key.toUpperCase(Locale.ROOT));
        customBuyOverrides = readDoubleMap(section, "custom-price-overrides", key -> key.toLowerCase(Locale.ROOT));
        customSellOverrides = readDoubleMap(section, "custom-sell-overrides", key -> key.toLowerCase(Locale.ROOT));

        ConfigurationSection advanced = section == null ? null : section.getConfigurationSection("advanced");
        cleanupIntervalMinutes = advanced == null ? 10 : Math.max(1, advanced.getInt("cleanup-interval-minutes", 10));

        orderBook.reloadFromConfig(section);
        rescheduleCleanupTask();
        
        loadProducts();
    }
    
    private ConfigurationSection loadBazaarSection() {
        if (bazaarConfigFile.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(bazaarConfigFile);
            return yaml.getConfigurationSection("bazaar");
        }
        return plugin.getConfig().getConfigurationSection("bazaar");
    }

    private void rescheduleCleanupTask() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }

        if (!enabled) {
            return;
        }

        long periodTicks = Math.max(20L, cleanupIntervalMinutes * 60L * 20L);
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupExpiredOrders, periodTicks, periodTicks);
    }

    private void cleanupExpiredOrders() {
        List<BazaarOrder> expired = orderBook.removeExpiredActiveOrders();
        if (expired.isEmpty()) {
            return;
        }

        for (BazaarOrder order : expired) {
            if (order.getOrderType() == BazaarOrder.OrderType.BUY) {
                double refund = order.getRemainingValue();
                if (refund > 0.0) {
                    shoppingBag.addCoins(order.getProfileId(), refund);
                }
            } else {
                int remaining = order.getRemainingAmount();
                if (remaining > 0) {
                    shoppingBag.addItems(order.getProfileId(), order.getProductId(), remaining);
                }
            }
        }

        shoppingBag.save();
    }

    private Map<String, Double> readDoubleMap(
            ConfigurationSection section,
            String path,
            java.util.function.UnaryOperator<String> keyNormalizer
    ) {
        Map<String, Double> result = new HashMap<>();
        if (section == null) {
            return result;
        }

        Object raw = section.get(path);
        if (raw instanceof ConfigurationSection inner) {
            for (String key : inner.getKeys(false)) {
                result.put(keyNormalizer.apply(key), inner.getDouble(key));
            }
            return result;
        }

        if (raw instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String key)) {
                    continue;
                }
                Object value = entry.getValue();
                double number;
                if (value instanceof Number n) {
                    number = n.doubleValue();
                } else if (value instanceof String s) {
                    try {
                        number = Double.parseDouble(s);
                    } catch (NumberFormatException ignored) {
                        continue;
                    }
                } else {
                    continue;
                }
                result.put(keyNormalizer.apply(key), number);
            }
        }

        return result;
    }
    
    /**
     * Initialize the product catalog with all available items.
     */
    private void initializeProducts() {
        products.clear();
        productsByCategory.clear();
        
        // Initialize with Skyblock-style categories
        for (BazaarProduct.BazaarCategory category : BazaarProduct.BazaarCategory.values()) {
            productsByCategory.put(category, new ArrayList<>());
        }
        
        // Add vanilla materials to categories
        initializeVanillaProducts();
        
        // Add custom items
        initializeCustomProducts();
        
        // Update all product market data
        updateAllProductPrices();
    }
    
    private void initializeVanillaProducts() {
        // --- AGRICULTURE CATEGORY ---
        // Wheat Group
        BazaarProduct wheat = createProduct("WHEAT", "Wheat", Material.WHEAT, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.FARMING_ITEMS, 64);
        wheat.addVariant(createProduct("ENCHANTED_WHEAT", "Enchanted Wheat", Material.HAY_BLOCK, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.FARMING_ITEMS, 64));
        wheat.addVariant(createProduct("ENCHANTED_HAY_BLOCK", "Enchanted Hay Bale", Material.HAY_BLOCK, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.FARMING_ITEMS, 64));
        addProduct(wheat);
        
        addProduct(createProduct("WHEAT_SEEDS", "Seeds", Material.WHEAT_SEEDS, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.FARMING_ITEMS, 64));
        
        // Carrot Group
        BazaarProduct carrot = createProduct("CARROT", "Carrot", Material.CARROT, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.FARMING_ITEMS, 64);
        carrot.addVariant(createProduct("ENCHANTED_CARROT", "Enchanted Carrot", Material.GOLDEN_CARROT, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.FARMING_ITEMS, 64));
        carrot.addVariant(createProduct("ENCHANTED_GOLDEN_CARROT", "Enchanted Golden Carrot", Material.GOLDEN_CARROT, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.FARMING_ITEMS, 64));
        addProduct(carrot);
        
        // Potato Group
        BazaarProduct potato = createProduct("POTATO", "Potato", Material.POTATO, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.FARMING_ITEMS, 64);
        potato.addVariant(createProduct("ENCHANTED_POTATO", "Enchanted Potato", Material.BAKED_POTATO, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.FARMING_ITEMS, 64));
        potato.addVariant(createProduct("ENCHANTED_BAKED_POTATO", "Enchanted Baked Potato", Material.BAKED_POTATO, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.FARMING_ITEMS, 64));
        addProduct(potato);
        
        // Pumpkin Group
        BazaarProduct pumpkin = createProduct("PUMPKIN", "Pumpkin", Material.PUMPKIN, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.FARMING_ITEMS, 64);
        pumpkin.addVariant(createProduct("ENCHANTED_PUMPKIN", "Enchanted Pumpkin", Material.JACK_O_LANTERN, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.FARMING_ITEMS, 64));
        addProduct(pumpkin);
        
        // Melon Group
        BazaarProduct melon = createProduct("MELON", "Melon", Material.MELON_SLICE, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.FARMING_ITEMS, 64);
        melon.addVariant(createProduct("ENCHANTED_MELON", "Enchanted Melon", Material.MELON, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.FARMING_ITEMS, 64));
        melon.addVariant(createProduct("ENCHANTED_MELON_BLOCK", "Enchanted Melon Block", Material.MELON, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.FARMING_ITEMS, 64));
        addProduct(melon);
        
        // Sugar Cane Group
        BazaarProduct sugarCane = createProduct("SUGAR_CANE", "Sugar Cane", Material.SUGAR_CANE, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.FARMING_ITEMS, 64);
        sugarCane.addVariant(createProduct("ENCHANTED_SUGAR", "Enchanted Sugar", Material.SUGAR, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.FARMING_ITEMS, 64));
        sugarCane.addVariant(createProduct("ENCHANTED_SUGAR_CANE", "Enchanted Sugar Cane", Material.PAPER, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.FARMING_ITEMS, 64));
        addProduct(sugarCane);
        
        // Cactus Group
        BazaarProduct cactus = createProduct("CACTUS", "Cactus", Material.CACTUS, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.FARMING_ITEMS, 64);
        cactus.addVariant(createProduct("ENCHANTED_CACTUS_GREEN", "Enchanted Cactus Green", Material.GREEN_DYE, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.FARMING_ITEMS, 64));
        cactus.addVariant(createProduct("ENCHANTED_CACTUS", "Enchanted Cactus", Material.CACTUS, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.FARMING_ITEMS, 64));
        addProduct(cactus);
        
        // Cocoa Group
        BazaarProduct cocoa = createProduct("COCOA_BEANS", "Cocoa Beans", Material.COCOA_BEANS, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.FARMING_ITEMS, 64);
        cocoa.addVariant(createProduct("ENCHANTED_COCOA", "Enchanted Cocoa Beans", Material.COOKIE, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.FARMING_ITEMS, 64));
        addProduct(cocoa);
        
        // Nether Wart Group
        BazaarProduct netherWart = createProduct("NETHER_WART", "Nether Wart", Material.NETHER_WART, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.MUSHROOMS, 64);
        netherWart.addVariant(createProduct("ENCHANTED_NETHER_WART", "Enchanted Nether Wart", Material.NETHER_WART_BLOCK, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.MUSHROOMS, 64));
        addProduct(netherWart);
        
        // Mushroom Group
        BazaarProduct redMushroom = createProduct("RED_MUSHROOM", "Red Mushroom", Material.RED_MUSHROOM, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.MUSHROOMS, 64);
        redMushroom.addVariant(createProduct("ENCHANTED_RED_MUSHROOM", "Enchanted Red Mushroom", Material.RED_MUSHROOM_BLOCK, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.MUSHROOMS, 64));
        addProduct(redMushroom);
        
        BazaarProduct brownMushroom = createProduct("BROWN_MUSHROOM", "Brown Mushroom", Material.BROWN_MUSHROOM, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.MUSHROOMS, 64);
        brownMushroom.addVariant(createProduct("ENCHANTED_BROWN_MUSHROOM", "Enchanted Brown Mushroom", Material.BROWN_MUSHROOM_BLOCK, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.MUSHROOMS, 64));
        addProduct(brownMushroom);
        
        // Animal Groups
        BazaarProduct leather = createProduct("LEATHER", "Leather", Material.LEATHER, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.ANIMAL, 64);
        leather.addVariant(createProduct("ENCHANTED_LEATHER", "Enchanted Leather", Material.LEATHER, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.ANIMAL, 64));
        addProduct(leather);
        
        BazaarProduct beef = createProduct("RAW_BEEF", "Raw Beef", Material.BEEF, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.ANIMAL, 64);
        beef.addVariant(createProduct("ENCHANTED_RAW_BEEF", "Enchanted Raw Beef", Material.COOKED_BEEF, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.ANIMAL, 64));
        addProduct(beef);
        
        BazaarProduct pork = createProduct("PORKCHOP", "Raw Porkchop", Material.PORKCHOP, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.ANIMAL, 64);
        pork.addVariant(createProduct("ENCHANTED_PORK", "Enchanted Porkchop", Material.COOKED_PORKCHOP, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.ANIMAL, 64));
        pork.addVariant(createProduct("ENCHANTED_GRILLED_PORK", "Enchanted Grilled Pork", Material.COOKED_PORKCHOP, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.ANIMAL, 64));
        addProduct(pork);
        
        BazaarProduct chicken = createProduct("CHICKEN", "Raw Chicken", Material.CHICKEN, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.ANIMAL, 64);
        chicken.addVariant(createProduct("ENCHANTED_CHICKEN", "Enchanted Chicken", Material.COOKED_CHICKEN, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.ANIMAL, 64));
        addProduct(chicken);
        
        BazaarProduct mutton = createProduct("MUTTON", "Raw Mutton", Material.MUTTON, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.ANIMAL, 64);
        mutton.addVariant(createProduct("ENCHANTED_MUTTON", "Enchanted Mutton", Material.COOKED_MUTTON, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.ANIMAL, 64));
        mutton.addVariant(createProduct("ENCHANTED_COOKED_MUTTON", "Enchanted Cooked Mutton", Material.COOKED_MUTTON, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.ANIMAL, 64));
        addProduct(mutton);
        
        BazaarProduct rabbit = createProduct("RABBIT", "Raw Rabbit", Material.RABBIT, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.ANIMAL, 64);
        rabbit.addVariant(createProduct("ENCHANTED_RABBIT", "Enchanted Rabbit", Material.COOKED_RABBIT, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.ANIMAL, 64));
        addProduct(rabbit);
        
        BazaarProduct feather = createProduct("FEATHER", "Feather", Material.FEATHER, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.ANIMAL, 64);
        feather.addVariant(createProduct("ENCHANTED_FEATHER", "Enchanted Feather", Material.FEATHER, BazaarProduct.BazaarCategory.AGRICULTURE, BazaarProduct.BazaarSubcategory.ANIMAL, 64));
        addProduct(feather);

        // --- MINING CATEGORY ---
        BazaarProduct cobble = createProduct("COBBLESTONE", "Cobblestone", Material.COBBLESTONE, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.ORE, 64);
        cobble.addVariant(createProduct("ENCHANTED_COBBLESTONE", "Enchanted Cobblestone", Material.COBBLESTONE, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.ORE, 64));
        addProduct(cobble);
        
        BazaarProduct coal = createProduct("COAL", "Coal", Material.COAL, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.ORE, 64);
        coal.addVariant(createProduct("ENCHANTED_COAL", "Enchanted Coal", Material.COAL_BLOCK, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.ORE, 64));
        coal.addVariant(createProduct("ENCHANTED_COAL_BLOCK", "Enchanted Coal Block", Material.COAL_BLOCK, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.ORE, 64));
        addProduct(coal);
        
        BazaarProduct iron = createProduct("IRON_INGOT", "Iron Ingot", Material.IRON_INGOT, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.ORE, 64);
        iron.addVariant(createProduct("ENCHANTED_IRON", "Enchanted Iron", Material.IRON_BLOCK, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.ORE, 64));
        iron.addVariant(createProduct("ENCHANTED_IRON_BLOCK", "Enchanted Iron Block", Material.IRON_BLOCK, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.ORE, 64));
        addProduct(iron);
        
        BazaarProduct gold = createProduct("GOLD_INGOT", "Gold Ingot", Material.GOLD_INGOT, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.ORE, 64);
        gold.addVariant(createProduct("ENCHANTED_GOLD", "Enchanted Gold", Material.GOLD_BLOCK, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.ORE, 64));
        gold.addVariant(createProduct("ENCHANTED_GOLD_BLOCK", "Enchanted Gold Block", Material.GOLD_BLOCK, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.ORE, 64));
        addProduct(gold);
        
        BazaarProduct diamond = createProduct("DIAMOND", "Diamond", Material.DIAMOND, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.ORE, 64);
        diamond.addVariant(createProduct("ENCHANTED_DIAMOND", "Enchanted Diamond", Material.DIAMOND_BLOCK, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.ORE, 64));
        diamond.addVariant(createProduct("ENCHANTED_DIAMOND_BLOCK", "Enchanted Diamond Block", Material.DIAMOND_BLOCK, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.ORE, 64));
        addProduct(diamond);
        
        BazaarProduct emerald = createProduct("EMERALD", "Emerald", Material.EMERALD, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.ORE, 64);
        emerald.addVariant(createProduct("ENCHANTED_EMERALD", "Enchanted Emerald", Material.EMERALD_BLOCK, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.ORE, 64));
        emerald.addVariant(createProduct("ENCHANTED_EMERALD_BLOCK", "Enchanted Emerald Block", Material.EMERALD_BLOCK, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.ORE, 64));
        addProduct(emerald);
        
        BazaarProduct redstone = createProduct("REDSTONE", "Redstone", Material.REDSTONE, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.GEMS, 64);
        redstone.addVariant(createProduct("ENCHANTED_REDSTONE", "Enchanted Redstone", Material.REDSTONE_BLOCK, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.GEMS, 64));
        redstone.addVariant(createProduct("ENCHANTED_REDSTONE_BLOCK", "Enchanted Redstone Block", Material.REDSTONE_BLOCK, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.GEMS, 64));
        addProduct(redstone);
        
        BazaarProduct lapis = createProduct("LAPIS_LAZULI", "Lapis Lazuli", Material.LAPIS_LAZULI, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.GEMS, 64);
        lapis.addVariant(createProduct("ENCHANTED_LAPIS_LAZULI", "Enchanted Lapis Lazuli", Material.LAPIS_BLOCK, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.GEMS, 64));
        lapis.addVariant(createProduct("ENCHANTED_LAPIS_LAZULI_BLOCK", "Enchanted Lapis Block", Material.LAPIS_BLOCK, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.GEMS, 64));
        addProduct(lapis);
        
        BazaarProduct quartz = createProduct("QUARTZ", "Nether Quartz", Material.QUARTZ, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.ORE, 64);
        quartz.addVariant(createProduct("ENCHANTED_QUARTZ", "Enchanted Quartz", Material.QUARTZ_BLOCK, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.ORE, 64));
        quartz.addVariant(createProduct("ENCHANTED_QUARTZ_BLOCK", "Enchanted Quartz Block", Material.QUARTZ_BLOCK, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.ORE, 64));
        addProduct(quartz);
        
        BazaarProduct obsidian = createProduct("OBSIDIAN", "Obsidian", Material.OBSIDIAN, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.SPECIAL, 64);
        obsidian.addVariant(createProduct("ENCHANTED_OBSIDIAN", "Enchanted Obsidian", Material.OBSIDIAN, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.SPECIAL, 64));
        addProduct(obsidian);
        
        BazaarProduct glowstone = createProduct("GLOWSTONE_DUST", "Glowstone Dust", Material.GLOWSTONE_DUST, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.FUEL, 64);
        glowstone.addVariant(createProduct("ENCHANTED_GLOWSTONE_DUST", "Enchanted Glowstone Dust", Material.GLOWSTONE_DUST, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.FUEL, 64));
        glowstone.addVariant(createProduct("ENCHANTED_GLOWSTONE", "Enchanted Glowstone", Material.GLOWSTONE, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.FUEL, 64));
        addProduct(glowstone);
        
        BazaarProduct sand = createProduct("SAND", "Sand", Material.SAND, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.SPECIAL, 64);
        sand.addVariant(createProduct("ENCHANTED_SAND", "Enchanted Sand", Material.SAND, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.SPECIAL, 64));
        addProduct(sand);
        
        BazaarProduct flint = createProduct("FLINT", "Flint", Material.FLINT, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.SPECIAL, 64);
        flint.addVariant(createProduct("ENCHANTED_FLINT", "Enchanted Flint", Material.FLINT, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.SPECIAL, 64));
        addProduct(flint);
        
        BazaarProduct ice = createProduct("ICE", "Ice", Material.ICE, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.SPECIAL, 64);
        ice.addVariant(createProduct("ENCHANTED_ICE", "Enchanted Ice", Material.PACKED_ICE, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.SPECIAL, 64));
        addProduct(ice);
        
        BazaarProduct packedIce = createProduct("PACKED_ICE", "Packed Ice", Material.PACKED_ICE, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.SPECIAL, 64);
        packedIce.addVariant(createProduct("ENCHANTED_PACKED_ICE", "Enchanted Packed Ice", Material.PACKED_ICE, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.SPECIAL, 64));
        addProduct(packedIce);
        
        BazaarProduct endStone = createProduct("END_STONE", "End Stone", Material.END_STONE, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.SPECIAL, 64);
        endStone.addVariant(createProduct("ENCHANTED_END_STONE", "Enchanted End Stone", Material.END_STONE, BazaarProduct.BazaarCategory.MINING, BazaarProduct.BazaarSubcategory.SPECIAL, 64));
        addProduct(endStone);

        // --- COMBAT CATEGORY ---
        BazaarProduct flesh = createProduct("ROTTEN_FLESH", "Rotten Flesh", Material.ROTTEN_FLESH, BazaarProduct.BazaarCategory.COMBAT, BazaarProduct.BazaarSubcategory.MOB_DROPS, 64);
        flesh.addVariant(createProduct("ENCHANTED_ROTTEN_FLESH", "Enchanted Rotten Flesh", Material.ROTTEN_FLESH, BazaarProduct.BazaarCategory.COMBAT, BazaarProduct.BazaarSubcategory.MOB_DROPS, 64));
        addProduct(flesh);
        
        BazaarProduct bone = createProduct("BONE", "Bone", Material.BONE, BazaarProduct.BazaarCategory.COMBAT, BazaarProduct.BazaarSubcategory.MOB_DROPS, 64);
        bone.addVariant(createProduct("ENCHANTED_BONE", "Enchanted Bone", Material.BONE_BLOCK, BazaarProduct.BazaarCategory.COMBAT, BazaarProduct.BazaarSubcategory.MOB_DROPS, 64));
        addProduct(bone);
        
        BazaarProduct string = createProduct("STRING", "String", Material.STRING, BazaarProduct.BazaarCategory.COMBAT, BazaarProduct.BazaarSubcategory.MOB_DROPS, 64);
        string.addVariant(createProduct("ENCHANTED_STRING", "Enchanted String", Material.STRING, BazaarProduct.BazaarCategory.COMBAT, BazaarProduct.BazaarSubcategory.MOB_DROPS, 64));
        addProduct(string);
        
        BazaarProduct eye = createProduct("SPIDER_EYE", "Spider Eye", Material.SPIDER_EYE, BazaarProduct.BazaarCategory.COMBAT, BazaarProduct.BazaarSubcategory.MOB_DROPS, 64);
        eye.addVariant(createProduct("ENCHANTED_SPIDER_EYE", "Enchanted Spider Eye", Material.FERMENTED_SPIDER_EYE, BazaarProduct.BazaarCategory.COMBAT, BazaarProduct.BazaarSubcategory.MOB_DROPS, 64));
        addProduct(eye);
        
        BazaarProduct powder = createProduct("GUNPOWDER", "Gunpowder", Material.GUNPOWDER, BazaarProduct.BazaarCategory.COMBAT, BazaarProduct.BazaarSubcategory.MOB_DROPS, 64);
        powder.addVariant(createProduct("ENCHANTED_GUNPOWDER", "Enchanted Gunpowder", Material.GUNPOWDER, BazaarProduct.BazaarCategory.COMBAT, BazaarProduct.BazaarSubcategory.MOB_DROPS, 64));
        addProduct(powder);
        
        BazaarProduct slime = createProduct("SLIME_BALL", "Slimeball", Material.SLIME_BALL, BazaarProduct.BazaarCategory.COMBAT, BazaarProduct.BazaarSubcategory.MOB_DROPS, 64);
        slime.addVariant(createProduct("ENCHANTED_SLIME_BALL", "Enchanted Slimeball", Material.SLIME_BLOCK, BazaarProduct.BazaarCategory.COMBAT, BazaarProduct.BazaarSubcategory.MOB_DROPS, 64));
        slime.addVariant(createProduct("ENCHANTED_SLIME_BLOCK", "Enchanted Slime Block", Material.SLIME_BLOCK, BazaarProduct.BazaarCategory.COMBAT, BazaarProduct.BazaarSubcategory.MOB_DROPS, 64));
        addProduct(slime);
        
        BazaarProduct magma = createProduct("MAGMA_CREAM", "Magma Cream", Material.MAGMA_CREAM, BazaarProduct.BazaarCategory.COMBAT, BazaarProduct.BazaarSubcategory.MOB_DROPS, 64);
        magma.addVariant(createProduct("ENCHANTED_MAGMA_CREAM", "Enchanted Magma Cream", Material.MAGMA_BLOCK, BazaarProduct.BazaarCategory.COMBAT, BazaarProduct.BazaarSubcategory.MOB_DROPS, 64));
        addProduct(magma);
        
        BazaarProduct blaze = createProduct("BLAZE_ROD", "Blaze Rod", Material.BLAZE_ROD, BazaarProduct.BazaarCategory.COMBAT, BazaarProduct.BazaarSubcategory.MOB_DROPS, 64);
        blaze.addVariant(createProduct("ENCHANTED_BLAZE_POWDER", "Enchanted Blaze Powder", Material.BLAZE_POWDER, BazaarProduct.BazaarCategory.COMBAT, BazaarProduct.BazaarSubcategory.MOB_DROPS, 64));
        blaze.addVariant(createProduct("ENCHANTED_BLAZE_ROD", "Enchanted Blaze Rod", Material.BLAZE_ROD, BazaarProduct.BazaarCategory.COMBAT, BazaarProduct.BazaarSubcategory.MOB_DROPS, 64));
        addProduct(blaze);
        
        BazaarProduct ender = createProduct("ENDER_PEARL", "Ender Pearl", Material.ENDER_PEARL, BazaarProduct.BazaarCategory.COMBAT, BazaarProduct.BazaarSubcategory.MOB_DROPS, 16);
        ender.addVariant(createProduct("ENCHANTED_ENDER_PEARL", "Enchanted Ender Pearl", Material.ENDER_PEARL, BazaarProduct.BazaarCategory.COMBAT, BazaarProduct.BazaarSubcategory.MOB_DROPS, 16));
        ender.addVariant(createProduct("ENCHANTED_EYE_OF_ENDER", "Enchanted Eye of Ender", Material.ENDER_EYE, BazaarProduct.BazaarCategory.COMBAT, BazaarProduct.BazaarSubcategory.MOB_DROPS, 64));
        addProduct(ender);
        
        BazaarProduct ghast = createProduct("GHAST_TEAR", "Ghast Tear", Material.GHAST_TEAR, BazaarProduct.BazaarCategory.COMBAT, BazaarProduct.BazaarSubcategory.MOB_DROPS, 64);
        ghast.addVariant(createProduct("ENCHANTED_GHAST_TEAR", "Enchanted Ghast Tear", Material.GHAST_TEAR, BazaarProduct.BazaarCategory.COMBAT, BazaarProduct.BazaarSubcategory.MOB_DROPS, 64));
        addProduct(ghast);

        // --- WOODS & FISHES CATEGORY ---
        BazaarProduct oak = createProduct("OAK_LOG", "Oak Log", Material.OAK_LOG, BazaarProduct.BazaarCategory.WOODS_FISHES, BazaarProduct.BazaarSubcategory.WOOD, 64);
        oak.addVariant(createProduct("ENCHANTED_OAK_LOG", "Enchanted Oak Log", Material.OAK_WOOD, BazaarProduct.BazaarCategory.WOODS_FISHES, BazaarProduct.BazaarSubcategory.WOOD, 64));
        addProduct(oak);
        
        BazaarProduct spruce = createProduct("SPRUCE_LOG", "Spruce Log", Material.SPRUCE_LOG, BazaarProduct.BazaarCategory.WOODS_FISHES, BazaarProduct.BazaarSubcategory.WOOD, 64);
        spruce.addVariant(createProduct("ENCHANTED_SPRUCE_LOG", "Enchanted Spruce Log", Material.SPRUCE_WOOD, BazaarProduct.BazaarCategory.WOODS_FISHES, BazaarProduct.BazaarSubcategory.WOOD, 64));
        addProduct(spruce);
        
        BazaarProduct birch = createProduct("BIRCH_LOG", "Birch Log", Material.BIRCH_LOG, BazaarProduct.BazaarCategory.WOODS_FISHES, BazaarProduct.BazaarSubcategory.WOOD, 64);
        birch.addVariant(createProduct("ENCHANTED_BIRCH_LOG", "Enchanted Birch Log", Material.BIRCH_WOOD, BazaarProduct.BazaarCategory.WOODS_FISHES, BazaarProduct.BazaarSubcategory.WOOD, 64));
        addProduct(birch);
        
        BazaarProduct darkOak = createProduct("DARK_OAK_LOG", "Dark Oak Log", Material.DARK_OAK_LOG, BazaarProduct.BazaarCategory.WOODS_FISHES, BazaarProduct.BazaarSubcategory.WOOD, 64);
        darkOak.addVariant(createProduct("ENCHANTED_DARK_OAK_LOG", "Enchanted Dark Oak Log", Material.DARK_OAK_WOOD, BazaarProduct.BazaarCategory.WOODS_FISHES, BazaarProduct.BazaarSubcategory.WOOD, 64));
        addProduct(darkOak);
        
        BazaarProduct acacia = createProduct("ACACIA_LOG", "Acacia Log", Material.ACACIA_LOG, BazaarProduct.BazaarCategory.WOODS_FISHES, BazaarProduct.BazaarSubcategory.WOOD, 64);
        acacia.addVariant(createProduct("ENCHANTED_ACACIA_LOG", "Enchanted Acacia Log", Material.ACACIA_WOOD, BazaarProduct.BazaarCategory.WOODS_FISHES, BazaarProduct.BazaarSubcategory.WOOD, 64));
        addProduct(acacia);
        
        BazaarProduct jungle = createProduct("JUNGLE_LOG", "Jungle Log", Material.JUNGLE_LOG, BazaarProduct.BazaarCategory.WOODS_FISHES, BazaarProduct.BazaarSubcategory.WOOD, 64);
        jungle.addVariant(createProduct("ENCHANTED_JUNGLE_LOG", "Enchanted Jungle Log", Material.JUNGLE_WOOD, BazaarProduct.BazaarCategory.WOODS_FISHES, BazaarProduct.BazaarSubcategory.WOOD, 64));
        addProduct(jungle);
        
        BazaarProduct fish = createProduct("RAW_FISH", "Raw Fish", Material.COD, BazaarProduct.BazaarCategory.WOODS_FISHES, BazaarProduct.BazaarSubcategory.FISH, 64);
        fish.addVariant(createProduct("ENCHANTED_RAW_FISH", "Enchanted Raw Fish", Material.COOKED_COD, BazaarProduct.BazaarCategory.WOODS_FISHES, BazaarProduct.BazaarSubcategory.FISH, 64));
        addProduct(fish);
        
        BazaarProduct salmon = createProduct("RAW_SALMON", "Raw Salmon", Material.SALMON, BazaarProduct.BazaarCategory.WOODS_FISHES, BazaarProduct.BazaarSubcategory.FISH, 64);
        salmon.addVariant(createProduct("ENCHANTED_RAW_SALMON", "Enchanted Raw Salmon", Material.COOKED_SALMON, BazaarProduct.BazaarCategory.WOODS_FISHES, BazaarProduct.BazaarSubcategory.FISH, 64));
        addProduct(salmon);
        
        BazaarProduct puffer = createProduct("PUFFERFISH", "Pufferfish", Material.PUFFERFISH, BazaarProduct.BazaarCategory.WOODS_FISHES, BazaarProduct.BazaarSubcategory.FISH, 64);
        puffer.addVariant(createProduct("ENCHANTED_PUFFERFISH", "Enchanted Pufferfish", Material.PUFFERFISH, BazaarProduct.BazaarCategory.WOODS_FISHES, BazaarProduct.BazaarSubcategory.FISH, 64));
        addProduct(puffer);
        
        BazaarProduct prismarine = createProduct("PRISMARINE_SHARD", "Prismarine Shard", Material.PRISMARINE_SHARD, BazaarProduct.BazaarCategory.WOODS_FISHES, BazaarProduct.BazaarSubcategory.TREASURE, 64);
        prismarine.addVariant(createProduct("ENCHANTED_PRISMARINE_SHARD", "Enchanted Prismarine Shard", Material.PRISMARINE_SHARD, BazaarProduct.BazaarCategory.WOODS_FISHES, BazaarProduct.BazaarSubcategory.TREASURE, 64));
        addProduct(prismarine);
        
        BazaarProduct crystals = createProduct("PRISMARINE_CRYSTALS", "Prismarine Crystals", Material.PRISMARINE_CRYSTALS, BazaarProduct.BazaarCategory.WOODS_FISHES, BazaarProduct.BazaarSubcategory.TREASURE, 64);
        crystals.addVariant(createProduct("ENCHANTED_PRISMARINE_CRYSTALS", "Enchanted Prismarine Crystals", Material.PRISMARINE_CRYSTALS, BazaarProduct.BazaarCategory.WOODS_FISHES, BazaarProduct.BazaarSubcategory.TREASURE, 64));
        addProduct(crystals);
        
        BazaarProduct clay = createProduct("CLAY_BALL", "Clay", Material.CLAY_BALL, BazaarProduct.BazaarCategory.WOODS_FISHES, BazaarProduct.BazaarSubcategory.TREASURE, 64);
        clay.addVariant(createProduct("ENCHANTED_CLAY_BALL", "Enchanted Clay", Material.CLAY_BALL, BazaarProduct.BazaarCategory.WOODS_FISHES, BazaarProduct.BazaarSubcategory.TREASURE, 64));
        addProduct(clay);

        // --- ODDITIES CATEGORY ---
        addProduct(createProduct("EXPERIENCE_BOTTLE", "Bottle o' Enchanting", Material.EXPERIENCE_BOTTLE, BazaarProduct.BazaarCategory.ODDITIES, BazaarProduct.BazaarSubcategory.MAGIC, 64));
        addProduct(createProduct("ENCHANTED_BOOK", "Enchanted Book", Material.ENCHANTED_BOOK, BazaarProduct.BazaarCategory.ODDITIES, BazaarProduct.BazaarSubcategory.MAGIC, 1));
        addProduct(createProduct("NETHER_STAR", "Nether Star", Material.NETHER_STAR, BazaarProduct.BazaarCategory.ODDITIES, BazaarProduct.BazaarSubcategory.MISC, 64));
    }
    
    private void initializeCustomProducts() {
        if (customItemService == null) return;

        int included = 0;
        int excluded = 0;
        for (String key : customItemService.allItemKeys()) {
            ItemStack item = customItemService.createItemByKey(key);
            if (item == null) {
                continue;
            }
            if (!isBazaarEligibleCustomItem(key, item)) {
                excluded++;
                continue;
            }

            BazaarProduct.BazaarCategory category = BazaarProduct.BazaarCategory.ODDITIES;
            BazaarProduct.BazaarSubcategory subcategory = BazaarProduct.BazaarSubcategory.MISC;

            MiningItemType miningType = MiningItemType.parse(key);
            if (miningType != null) {
                category = BazaarProduct.BazaarCategory.MINING;
                subcategory = BazaarProduct.BazaarSubcategory.SPECIAL;
            }

            BazaarProduct product = new BazaarProduct(
                    "CUSTOM_" + key.toUpperCase(Locale.ROOT),
                    friendlyCustomProductName(key, item) + " " + ChatColor.DARK_GRAY + "(Custom)",
                    item.getType(),
                    category,
                    subcategory,
                    true,
                    key,
                    item.getMaxStackSize()
            );
            addProduct(product);
            included++;
        }

        plugin.getLogger().info("Bazaar custom catalog loaded: " + included
                + " commodities, " + excluded + " auction-house items excluded.");
    }

    static boolean isBazaarEligibleCustomItem(String key, ItemStack item) {
        if (item == null || isAirMaterial(item.getType())) {
            return false;
        }
        if (!isBazaarEligibleCustomKey(key, item.getType(), item.getMaxStackSize())) {
            return false;
        }
        return !looksAuctionHouseOnlyByItemMeta(item);
    }

    static boolean isBazaarEligibleCustomKey(String key, Material material, int maxStackSize) {
        if (key == null || key.isBlank() || material == null || isAirMaterial(material)) {
            return false;
        }
        
        MiningItemType miningType = MiningItemType.parse(key);
        if (miningType != null && miningType != MiningItemType.SAPPHIRE && miningType != MiningItemType.ENCHANTED_SAPPHIRE
                && miningType != MiningItemType.TITANIUM && miningType != MiningItemType.ENCHANTED_TITANIUM) {
            return false;
        }
        
        if (CustomWeaponType.parse(key) != null
                || CustomArmorType.parse(key) != null
                || CustomToolType.parse(key) != null
                || GrapplingHookType.parse(key) != null
                || ReforgeStoneType.parse(key) != null) {
            return false;
        }
        if (isEquipmentLikeMaterial(material)) {
            return false;
        }
        if (maxStackSize <= 1) {
            return false;
        }
        if (looksAuctionHouseOnlyByKey(key)) {
            return false;
        }
        return true;
    }

    private static boolean isEquipmentLikeMaterial(Material material) {
        if (material == null || isAirMaterial(material)) {
            return false;
        }
        return switch (material) {
            case BOW, CROSSBOW, TRIDENT, SHIELD, ELYTRA, FISHING_ROD, MACE -> true;
            default -> {
                String name = material.name();
                yield name.endsWith("_SWORD")
                        || name.endsWith("_AXE")
                        || name.endsWith("_PICKAXE")
                        || name.endsWith("_SHOVEL")
                        || name.endsWith("_HOE")
                        || name.endsWith("_HELMET")
                        || name.endsWith("_CHESTPLATE")
                        || name.endsWith("_LEGGINGS")
                        || name.endsWith("_BOOTS");
            }
        };
    }

    private static boolean looksAuctionHouseOnlyByKey(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        return lower.contains("sword")
                || lower.contains("bow")
                || lower.contains("shortbow")
                || lower.contains("staff")
                || lower.contains("wand")
                || lower.contains("scythe")
                || lower.contains("katana")
                || lower.contains("mace")
                || lower.contains("dagger")
                || lower.contains("gauntlet")
                || lower.contains("shield")
                || lower.contains("cloak")
                || lower.contains("pickaxe")
                || lower.contains("helmet")
                || lower.contains("chestplate")
                || lower.contains("leggings")
                || lower.contains("boots")
                || lower.contains("drill")
                || lower.contains("hook")
                || lower.contains("grappling")
                || lower.contains("talisman")
                || lower.contains("artifact")
                || lower.contains("relic");
    }

    private static boolean looksAuctionHouseOnlyByItemMeta(ItemStack item) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta() == null || !item.getItemMeta().hasLore()) {
            return false;
        }

        List<String> lore = item.getItemMeta().getLore();
        if (lore == null || lore.isEmpty()) {
            return false;
        }

        for (String line : lore) {
            if (line == null || line.isBlank()) {
                continue;
            }
            String plain = ChatColor.stripColor(line);
            if (plain == null || plain.isBlank()) {
                continue;
            }

            String upper = plain.toUpperCase(Locale.ROOT);
            if (upper.contains("WEAPON")
                    || upper.contains("ARMOR")
                    || upper.contains("DRILL")
                    || upper.contains("SHORTBOW")
                    || upper.contains("SWORD")
                    || upper.contains("BOW")
                    || upper.contains("HELMET")
                    || upper.contains("CHESTPLATE")
                    || upper.contains("LEGGINGS")
                    || upper.contains("BOOTS")
                    || upper.contains("TALISMAN")
                    || upper.contains("ARTIFACT")
                    || upper.contains("RELIC")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAirMaterial(Material material) {
        return material == Material.AIR
                || material == Material.CAVE_AIR
                || material == Material.VOID_AIR;
    }

    private String friendlyCustomProductName(String key, ItemStack item) {
        if (item != null && item.hasItemMeta() && item.getItemMeta() != null && item.getItemMeta().hasDisplayName()) {
            String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            if (displayName != null && !displayName.isBlank()) {
                return displayName.trim();
            }
        }

        String plain = key == null ? "" : key.replace("_", " ").toLowerCase(Locale.ROOT);
        return Arrays.stream(plain.split(" "))
                .filter(part -> !part.isBlank())
                .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1))
                .reduce("", (a, b) -> a.isEmpty() ? b : a + " " + b);
    }
    private BazaarProduct createProduct(
            String id,
            String name,
            Material icon,
            BazaarProduct.BazaarCategory category,
            BazaarProduct.BazaarSubcategory subcategory,
            int stackSize
    ) {
        return new BazaarProduct(id, name, icon, category, subcategory, false, null, stackSize);
    }

    private BazaarProduct createCustomProduct(
            String id,
            String name,
            String customKey,
            Material icon,
            BazaarProduct.BazaarCategory category,
            BazaarProduct.BazaarSubcategory subcategory,
            int stackSize
    ) {
        return new BazaarProduct(id, name, icon, category, subcategory, true, customKey, stackSize);
    }
    
    private void addProduct(BazaarProduct product) {
        if (product == null) return;
        products.put(product.getProductId().toLowerCase(Locale.ROOT), product);
        
        // Register variants in the map as well so they can be looked up by ID
        for (BazaarProduct variant : product.getVariants()) {
            products.put(variant.getProductId().toLowerCase(Locale.ROOT), variant);
        }
        
        // Only add top-level products to the category list to avoid duplicates in the main menu
        if (!product.isVariant()) {
            productsByCategory.computeIfAbsent(product.getCategory(), k -> new ArrayList<>()).add(product);
        }
    }
    
    private void loadProducts() {
        if (!productsConfigFile.exists()) {
            return;
        }
        
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(productsConfigFile);
        ConfigurationSection productsSection = yaml.getConfigurationSection("products");
        
        if (productsSection == null) return;
        
        for (String productId : productsSection.getKeys(false)) {
            ConfigurationSection productSection = productsSection.getConfigurationSection(productId);
            if (productSection == null) continue;
            
            // Load price history if exists
            // (Product objects are recreated on initializeProducts)
        }
    }
    
    /**
     * Update all product prices from current order book.
     */
    public void updateAllProductPrices() {
        for (BazaarProduct product : products.values()) {
            updateProductPrice(product);
        }
    }
    
    /**
     * Update a single product's price from order book.
     */
    private void updateProductPrice(BazaarProduct product) {
        List<BazaarOrder> buyOrders = orderBook.getBuyOrders(product.getProductId());
        List<BazaarOrder> sellOrders = orderBook.getSellOrders(product.getProductId());
        product.updateFromOrderBook(buyOrders, sellOrders);
    }

    private double defaultBuyPriceFor(BazaarProduct product) {
        if (product == null) {
            return 0.0;
        }

        if (product.isCustomItem()) {
            String key = product.getCustomItemKey();
            if (key != null) {
                Double override = customBuyOverrides.get(key.toLowerCase(Locale.ROOT));
                if (override != null) {
                    return override;
                }
            }
            return defaultCustomBuyPrice;
        }

        Material icon = product.getIcon();
        if (icon != null) {
            Double override = materialBuyOverrides.get(icon.name());
            if (override != null) {
                return override;
            }
        }
        return defaultVanillaBuyPrice;
    }

    private double defaultSellPriceFor(BazaarProduct product, double defaultBuy) {
        if (product == null) {
            return 0.0;
        }

        if (product.isCustomItem()) {
            String key = product.getCustomItemKey();
            if (key != null) {
                Double override = customSellOverrides.get(key.toLowerCase(Locale.ROOT));
                if (override != null) {
                    return override;
                }
            }
            if (defaultCustomSellPrice > 0.0) {
                return defaultCustomSellPrice;
            }
            return defaultBuy * defaultSellMultiplier;
        }

        Material icon = product.getIcon();
        if (icon != null) {
            Double override = materialSellOverrides.get(icon.name());
            if (override != null) {
                return override;
            }
        }
        if (defaultVanillaSellPrice > 0.0) {
            return defaultVanillaSellPrice;
        }
        return defaultBuy * defaultSellMultiplier;
    }

    private double marketPriceFor(String productId) {
        double highestBuy = orderBook.getHighestBuyPrice(productId);
        double lowestSell = orderBook.getLowestSellPrice(productId);

        boolean hasBuy = Double.isFinite(highestBuy) && highestBuy > 0.0;
        boolean hasSell = Double.isFinite(lowestSell) && lowestSell > 0.0;

        if (hasBuy && hasSell) {
            return (highestBuy + lowestSell) / 2.0;
        }
        if (hasSell) {
            return lowestSell;
        }
        if (hasBuy) {
            return highestBuy;
        }
        return Double.NaN;
    }

    private double sellTaxRateForOwner(UUID ownerId) {
        // Hypixel: selling is taxed (base 1.25%), reduced by the Bazaar Flipper account upgrade.
        double rate = accountUpgrades == null ? BazaarAccountUpgrades.sellTaxRateForTier(0) : accountUpgrades.sellTaxRate(ownerId);
        
        // Add Island Upgrade Bonus
        if (plugin.getIslandManager() != null) {
            Island island = plugin.getIslandManager().getIsland(ownerId);
            if (island != null) {
                int islandTier = island.getBazaarFlipperUpgrade();
                if (islandTier > 0) {
                    // Use the better rate between account and island
                    double islandRate = BazaarAccountUpgrades.sellTaxRateForTier(islandTier);
                    rate = Math.min(rate, islandRate);
                }
            }
        }
        
        return rate;
    }

    private double sellTaxRateForProfile(UUID profileId) {
        if (profileId == null) {
            return BazaarAccountUpgrades.sellTaxRateForTier(0);
        }
        var profileManager = plugin.getProfileManager();
        if (profileManager != null) {
            SkyBlockProfile profile = profileManager.getProfile(profileId);
            if (profile != null && profile.getOwnerId() != null) {
                return sellTaxRateForOwner(profile.getOwnerId());
            }
        }
        // Fallback: treat the profile id as an owner id for legacy data.
        return sellTaxRateForOwner(profileId);
    }

    private static double applySellTax(double gross, double taxRate) {
        if (!Double.isFinite(gross) || gross <= 0.0D) {
            return 0.0D;
        }
        if (!Double.isFinite(taxRate) || taxRate <= 0.0D) {
            return gross;
        }
        double clampedRate = Math.min(1.0D, Math.max(0.0D, taxRate));
        return Math.max(0.0D, gross * (1.0D - clampedRate));
    }
    
    // ==================== PUBLIC API ====================
    
    public boolean isEnabled() { return enabled; }
    public GriviencePlugin getPlugin() { return plugin; }
    public BazaarOrderBook getOrderBook() { return orderBook; }
    public BazaarShoppingBag getShoppingBag() { return shoppingBag; }
    public BazaarPriceHistory getPriceHistory() { return priceHistory; }
    public BazaarProductCache getProductCache() { return productCache; }
    public int getMaxOrdersPerPlayer() { return maxOrdersPerPlayer; }

    public int getMaxOrdersAllowed(Player player) {
        if (player == null) {
            return maxOrdersPerPlayer;
        }
        
        int base = accountUpgrades == null ? maxOrdersPerPlayer : accountUpgrades.maxOrders(player.getUniqueId());
        
        // Add Island Upgrade Bonus
        if (plugin.getIslandManager() != null) {
            Island island = plugin.getIslandManager().getIsland(player.getUniqueId());
            if (island != null) {
                int islandTier = island.getBazaarFlipperUpgrade();
                if (islandTier > 0) {
                    // Use the better order limit between account and island
                    int islandLimit = BazaarAccountUpgrades.maxOrdersForTier(islandTier);
                    base = Math.max(base, islandLimit);
                }
            }
        }
        
        int max = base;
        
        if (player.hasPermission("grivience.bazaar.limit.mvpplusplus")) {
            max = Math.max(max, base + 14);
        } else if (player.hasPermission("grivience.bazaar.limit.mvpplus")) {
            max = Math.max(max, base + 10);
        } else if (player.hasPermission("grivience.bazaar.limit.mvp")) {
            max = Math.max(max, base + 7);
        } else if (player.hasPermission("grivience.bazaar.limit.vipplusplus")) {
            max = Math.max(max, base + 4);
        } else if (player.hasPermission("grivience.bazaar.limit.vipplus")) {
            max = Math.max(max, base + 2);
        } else if (player.hasPermission("grivience.bazaar.limit.vip")) {
            max = Math.max(max, base + 1);
        }
        
        return max;
    }

    public double getSellTaxRate(Player player) {
        if (player == null) {
            return BazaarAccountUpgrades.sellTaxRateForTier(0);
        }
        return sellTaxRateForOwner(player.getUniqueId());
    }

    public double netAfterSellTax(Player player, double gross) {
        return applySellTax(gross, getSellTaxRate(player));
    }

    public record NpcSellQuote(
            BazaarProduct product,
            int amount,
            double bazaarCapUnitPrice,
            double npcUnitPrice,
            double grossCoins,
            double netCoins,
            String rejectionReason
    ) {
        public boolean sellable() {
            return rejectionReason == null;
        }
    }

    public record NpcBulkSellQuote(
            int sellableStacks,
            int sellableItems,
            int skippedStacks,
            double grossCoins,
            double netCoins
    ) {
        public boolean hasSellableItems() {
            return sellableItems > 0;
        }
    }

    public record NpcBulkSellResult(
            int soldProductTypes,
            int soldItems,
            int skippedStacks,
            double grossCoins,
            double netCoins
    ) {
        public boolean soldAnything() {
            return soldItems > 0;
        }
    }

    public boolean isNpcShopEnabled() {
        return npcShopEnabled;
    }

    public double getNpcShopSellMultiplier() {
        return npcShopSellMultiplier;
    }

    static double cappedNpcUnitPrice(double bazaarUnitPrice, double npcMultiplier) {
        if (!Double.isFinite(bazaarUnitPrice) || bazaarUnitPrice <= 0.0) {
            return 0.0;
        }
        double multiplier = Double.isFinite(npcMultiplier) ? Math.max(0.0, npcMultiplier) : 0.0;
        double candidate = bazaarUnitPrice * multiplier;
        if (!Double.isFinite(candidate) || candidate <= 0.0) {
            return 0.0;
        }
        return Math.min(candidate, bazaarUnitPrice);
    }

    public BazaarProduct findProductForItem(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return null;
        }
        if (isBlockedFromSelling(stack)) {
            return null;
        }

        if (customItemService != null) {
            String customId = customItemService.itemId(stack);
            if (customId != null && !customId.isBlank()) {
                // If this is a custom item, it must explicitly exist in the Bazaar catalog.
                return getProductByCustomKey(customId);
            }
        }

        return getProductByMaterial(stack.getType());
    }

    public NpcSellQuote quoteNpcSell(Player player, ItemStack stack) {
        int amount = stack == null ? 0 : stack.getAmount();
        return quoteNpcSell(player, stack, amount);
    }

    public NpcSellQuote quoteNpcSell(Player player, ItemStack stack, int amount) {
        if (!npcShopEnabled) {
            return new NpcSellQuote(null, 0, 0.0, 0.0, 0.0, 0.0, "NPC commodity shop is disabled.");
        }
        if (stack == null || stack.getType().isAir()) {
            return new NpcSellQuote(null, 0, 0.0, 0.0, 0.0, 0.0, "Hold an item in your main hand.");
        }
        if (isBlockedFromSelling(stack)) {
            if (plugin.getPersonalCompactorManager() != null && plugin.getPersonalCompactorManager().isCompactorItem(stack)) {
                return new NpcSellQuote(null, 0, 0.0, 0.0, 0.0, 0.0, "Personal Compacters cannot be sold to NPCs. Use the Auction House!");
            }
            return new NpcSellQuote(null, 0, 0.0, 0.0, 0.0, 0.0, "The Skyblock Menu cannot be sold.");
        }

        BazaarProduct product = findProductForItem(stack);
        if (product == null) {
            return new NpcSellQuote(null, 0, 0.0, 0.0, 0.0, 0.0,
                    "This item is not a Bazaar commodity. Use the Auction House plugin for gear.");
        }

        return quoteNpcSell(player, product, amount);
    }

    public NpcBulkSellQuote previewNpcInventorySell(Player player) {
        if (player == null) {
            return new NpcBulkSellQuote(0, 0, 0, 0.0, 0.0);
        }

        int sellableStacks = 0;
        int sellableItems = 0;
        int skippedStacks = 0;
        double gross = 0.0;
        double net = 0.0;

        for (ItemStack stack : combinedInventoryContents(player)) {
            if (stack == null || stack.getType().isAir() || stack.getAmount() <= 0) {
                continue;
            }

            NpcSellQuote quote = quoteNpcSell(player, stack, stack.getAmount());
            if (!quote.sellable()) {
                skippedStacks++;
                continue;
            }

            sellableStacks++;
            sellableItems += quote.amount();
            gross += quote.grossCoins();
            net += quote.netCoins();
        }

        return new NpcBulkSellQuote(sellableStacks, sellableItems, skippedStacks, gross, net);
    }

    public boolean sellMainHandToNpc(Player player) {
        if (player == null) {
            return false;
        }
        if (!npcShopEnabled) {
            player.sendMessage(ChatColor.RED + "NPC commodity shop is disabled.");
            return false;
        }

        UUID profileId = requireProfileId(player);
        if (profileId == null) {
            return false;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        NpcSellQuote quote = quoteNpcSell(player, hand, hand == null ? 0 : hand.getAmount());
        if (!quote.sellable()) {
            player.sendMessage(ChatColor.RED + quote.rejectionReason());
            return false;
        }

        int sellAmount = quote.amount();
        if (sellAmount <= 0 || hand == null || hand.getType().isAir() || hand.getAmount() < sellAmount) {
            player.sendMessage(ChatColor.RED + "You don't have enough items in your hand.");
            return false;
        }

        int remaining = hand.getAmount() - sellAmount;
        if (remaining <= 0) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            ItemStack updated = hand.clone();
            updated.setAmount(remaining);
            player.getInventory().setItemInMainHand(updated);
        }
        player.updateInventory();

        payoutNpcSell(player, profileId, quote.netCoins());
        priceHistory.recordTransaction(
                quote.product().getProductId(),
                quote.npcUnitPrice(),
                quote.amount(),
                BazaarPriceHistory.TransactionType.INSTANT_SELL
        );

        player.sendMessage(ChatColor.GREEN + "Sold " + ChatColor.YELLOW + quote.amount() + "x "
                + quote.product().getProductName() + ChatColor.GRAY + " to the NPC shop for "
                + ChatColor.GOLD + formatCoins(quote.netCoins()) + ChatColor.GRAY + ".");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.15f);
        return true;
    }

    public NpcBulkSellResult sellAllEligibleToNpc(Player player) {
        if (player == null) {
            return new NpcBulkSellResult(0, 0, 0, 0.0, 0.0);
        }
        if (!npcShopEnabled) {
            player.sendMessage(ChatColor.RED + "NPC commodity shop is disabled.");
            return new NpcBulkSellResult(0, 0, 0, 0.0, 0.0);
        }

        UUID profileId = requireProfileId(player);
        if (profileId == null) {
            return new NpcBulkSellResult(0, 0, 0, 0.0, 0.0);
        }

        Map<String, Integer> byProduct = new LinkedHashMap<>();
        int skippedStacks = 0;

        for (ItemStack stack : combinedInventoryContents(player)) {
            if (stack == null || stack.getType().isAir() || stack.getAmount() <= 0) {
                continue;
            }
            NpcSellQuote quote = quoteNpcSell(player, stack, stack.getAmount());
            if (!quote.sellable() || quote.product() == null) {
                skippedStacks++;
                continue;
            }
            byProduct.merge(quote.product().getProductId(), quote.amount(), Integer::sum);
        }

        if (byProduct.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No Bazaar-eligible commodities found in your inventory.");
            return new NpcBulkSellResult(0, 0, skippedStacks, 0.0, 0.0);
        }

        int soldItems = 0;
        int soldProductTypes = 0;
        double gross = 0.0;
        double net = 0.0;

        for (Map.Entry<String, Integer> entry : byProduct.entrySet()) {
            BazaarProduct product = getProduct(entry.getKey());
            if (product == null) {
                continue;
            }
            int amount = entry.getValue();
            if (amount <= 0) {
                continue;
            }

            NpcSellQuote quote = quoteNpcSell(player, product, amount);
            if (!quote.sellable()) {
                continue;
            }
            if (!removeItems(player, product, amount)) {
                continue;
            }

            soldProductTypes++;
            soldItems += amount;
            gross += quote.grossCoins();
            net += quote.netCoins();
            priceHistory.recordTransaction(
                    product.getProductId(),
                    quote.npcUnitPrice(),
                    amount,
                    BazaarPriceHistory.TransactionType.INSTANT_SELL
            );
        }

        if (soldItems <= 0) {
            player.sendMessage(ChatColor.RED + "No items could be sold to the NPC shop.");
            return new NpcBulkSellResult(0, 0, skippedStacks, 0.0, 0.0);
        }

        payoutNpcSell(player, profileId, net);
        player.sendMessage(ChatColor.GREEN + "Sold " + ChatColor.YELLOW + soldItems + " items"
                + ChatColor.GRAY + " across " + ChatColor.YELLOW + soldProductTypes + " commodities"
                + ChatColor.GRAY + " for " + ChatColor.GOLD + formatCoins(net) + ChatColor.GRAY + ".");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.55f, 1.1f);
        return new NpcBulkSellResult(soldProductTypes, soldItems, skippedStacks, gross, net);
    }

    private NpcSellQuote quoteNpcSell(Player player, BazaarProduct product, int amount) {
        if (product == null) {
            return new NpcSellQuote(null, 0, 0.0, 0.0, 0.0, 0.0, "This item is not a Bazaar commodity.");
        }
        if (amount <= 0) {
            return new NpcSellQuote(product, 0, 0.0, 0.0, 0.0, 0.0, "Invalid item amount.");
        }

        updateProductPrice(product);
        double bazaarCapUnitPrice = bazaarNpcSellCapPrice(product);
        double npcUnitPrice = cappedNpcUnitPrice(bazaarCapUnitPrice, npcShopSellMultiplier);
        if (!Double.isFinite(npcUnitPrice) || npcUnitPrice <= 0.0) {
            return new NpcSellQuote(product, amount, bazaarCapUnitPrice, 0.0, 0.0, 0.0,
                    "No valid NPC payout was found for this commodity.");
        }

        double gross = amount * npcUnitPrice;
        double net = npcShopApplySellTax ? netAfterSellTax(player, gross) : gross;
        return new NpcSellQuote(product, amount, bazaarCapUnitPrice, npcUnitPrice, gross, net, null);
    }

    private double bazaarNpcSellCapPrice(BazaarProduct product) {
        if (product == null) {
            return 0.0;
        }

        double instantSell = product.getInstantSellPrice();
        if (Double.isFinite(instantSell) && instantSell > 0.0) {
            return instantSell;
        }

        double defaultBuy = defaultBuyPriceFor(product);
        return defaultSellPriceFor(product, defaultBuy);
    }

    private List<ItemStack> combinedInventoryContents(Player player) {
        List<ItemStack> contents = new ArrayList<>();
        if (player == null) {
            return contents;
        }
        ItemStack[] storage = player.getInventory().getStorageContents();
        if (storage != null) {
            Collections.addAll(contents, storage);
        }
        ItemStack[] extra = player.getInventory().getExtraContents();
        if (extra != null) {
            Collections.addAll(contents, extra);
        }
        return contents;
    }

    private void payoutNpcSell(Player player, UUID profileId, double amount) {
        if (amount <= 0.0) {
            return;
        }
        if (!deposit(player, amount)) {
            shoppingBag.addCoins(profileId, amount);
            player.sendMessage(ChatColor.YELLOW + "Coins were moved to your Bazaar shopping bag because payout failed.");
        }
    }
    
    public Collection<BazaarProduct> getAllProducts() {
        return Collections.unmodifiableCollection(products.values());
    }
    
    public List<BazaarProduct> getProductsByCategory(BazaarProduct.BazaarCategory category) {
        return Collections.unmodifiableList(productsByCategory.getOrDefault(category, new ArrayList<>()));
    }
    
    public BazaarProduct getProduct(String productId) {
        return products.get(productId.toLowerCase(Locale.ROOT));
    }
    
    public BazaarProduct getProductByCustomKey(String customKey) {
        if (customKey == null || customKey.isBlank()) {
            return null;
        }
        for (BazaarProduct product : products.values()) {
            if (product.isCustomItem() && product.getCustomItemKey() != null
                    && product.getCustomItemKey().equalsIgnoreCase(customKey)) {
                return product;
            }
        }
        return null;
    }
    
    public BazaarProduct getProductByMaterial(Material material) {
        if (material == null) return null;
        for (BazaarProduct product : products.values()) {
            if (!product.isCustomItem() && product.getIcon() == material) {
                return product;
            }
        }
        return null;
    }
    
    /**
     * Place a buy order.
     */
    public BazaarOrder placeBuyOrder(Player player, String productId, int amount, double pricePerUnit) {
        if (!requireBazaarAccess(player)) {
            return null;
        }
        BazaarProduct product = getProduct(productId);
        if (product == null) {
            player.sendMessage(ChatColor.RED + "Product not found.");
            return null;
        }
        String canonicalProductId = product.getProductId();
        
        // Validate order
        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "Invalid amount.");
            return null;
        }
        if (amount > maxOrderAmount) {
            player.sendMessage(ChatColor.RED + "Order amount must be between 1 and " + maxOrderAmount + ".");
            return null;
        }
        
        if (!Double.isFinite(pricePerUnit) || pricePerUnit <= 0.0) {
            player.sendMessage(ChatColor.RED + "Invalid price.");
            return null;
        }
        
        UUID profileId = requireProfileId(player);
        if (profileId == null) {
            return null;
        }

        BazaarOrderBook.ValidationResult validation = orderBook.validateOrder(
                player.getUniqueId(),
                profileId,
                BazaarOrder.OrderType.BUY,
                pricePerUnit,
                amount,
                getMaxOrdersAllowed(player)
        );
        if (!validation.valid()) {
            player.sendMessage(validation.errorMessage());
            return null;
        }
        
        // Check funds
        double escrow = amount * pricePerUnit;
        if (!Double.isFinite(escrow) || escrow <= 0.0) {
            player.sendMessage(ChatColor.RED + "Invalid price.");
            return null;
        }
        if (!hasFunds(player, escrow)) {
            player.sendMessage(ChatColor.RED + "Insufficient funds. Need: " + formatCoins(escrow));
            return null;
        }
        
        // Withdraw escrow
        if (!withdraw(player, escrow)) {
            player.sendMessage(ChatColor.RED + "Failed to withdraw funds.");
            return null;
        }
        
        // Create order
        String orderId = orderBook.generateOrderId();
        long createdAt = System.currentTimeMillis();
        BazaarOrder order = orderBook.createOrder(
                orderId,
                player.getUniqueId(),
                profileId,
                player.getName(),
                canonicalProductId,
                BazaarOrder.OrderType.BUY,
                pricePerUnit,
                amount,
                createdAt
        );
        
        // Try to match immediately
        BazaarOrderBook.MatchResult match = orderBook.instantBuy(canonicalProductId, amount, pricePerUnit);
        
        if (match.filled() > 0) {
            for (BazaarOrderBook.MatchEntry entry : match.matches()) {
                order.fill(entry.amount(), entry.price());
            }

            // Buyer receives items.
            shoppingBag.addItems(profileId, canonicalProductId, match.filled());

            // Sellers receive coins at their ask price.
            Map<UUID, Double> coinsBySellerProfile = new HashMap<>();
            for (BazaarOrderBook.MatchEntry entry : match.matches()) {
                coinsBySellerProfile.merge(entry.counterpartyProfileId(), entry.amount() * entry.price(), Double::sum);
            }
            for (Map.Entry<UUID, Double> entry : coinsBySellerProfile.entrySet()) {
                UUID sellerProfileId = entry.getKey();
                double netCoins = applySellTax(entry.getValue(), sellTaxRateForProfile(sellerProfileId));
                if (netCoins > 0.0D) {
                    shoppingBag.addCoins(sellerProfileId, netCoins);
                }
            }

            // Refund "savings" (bid - ask) on the filled portion to the buyer's bag.
            double savings = 0.0;
            for (BazaarOrderBook.MatchEntry entry : match.matches()) {
                savings += Math.max(0.0, (pricePerUnit - entry.price()) * entry.amount());
            }
            if (savings > 0.0) {
                shoppingBag.addCoins(profileId, savings);
            }
            
            if (order.getRemainingAmount() > 0) {
                orderBook.placeOrder(order);
            }

            double avgPrice = match.totalCost() / match.filled();
            priceHistory.recordTransaction(canonicalProductId, avgPrice, match.filled(), BazaarPriceHistory.TransactionType.BUY_ORDER);
            updateProductPrice(product);

            player.sendMessage(ChatColor.GREEN + "Buy order filled " + ChatColor.YELLOW + match.filled() + "x " + product.getProductName()
                    + (order.getRemainingAmount() > 0 ? ChatColor.YELLOW + " (remaining: " + order.getRemainingAmount() + ")" : ""));

            return order.getRemainingAmount() > 0 ? order : null;
        }
        
        // No match - place full order
        orderBook.placeOrder(order);
        player.sendMessage(ChatColor.GREEN + "Buy order placed: " + ChatColor.YELLOW + amount + "x " + product.getProductName()
                + ChatColor.GRAY + " @ " + formatCoins(pricePerUnit) + ChatColor.GRAY + " each");
        
        return order;
    }
    
    /**
     * Place a sell order.
     */
    public BazaarOrder placeSellOrder(Player player, String productId, int amount, double pricePerUnit) {
        if (!requireBazaarAccess(player)) {
            return null;
        }
        BazaarProduct product = getProduct(productId);
        if (product == null) {
            player.sendMessage(ChatColor.RED + "Product not found.");
            return null;
        }
        String canonicalProductId = product.getProductId();
        
        // Validate
        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "Invalid amount.");
            return null;
        }
        if (amount > maxOrderAmount) {
            player.sendMessage(ChatColor.RED + "Order amount must be between 1 and " + maxOrderAmount + ".");
            return null;
        }
        
        if (!Double.isFinite(pricePerUnit) || pricePerUnit <= 0.0) {
            player.sendMessage(ChatColor.RED + "Invalid price.");
            return null;
        }

        UUID profileId = requireProfileId(player);
        if (profileId == null) {
            return null;
        }

        BazaarOrderBook.ValidationResult validation = orderBook.validateOrder(
                player.getUniqueId(),
                profileId,
                BazaarOrder.OrderType.SELL,
                pricePerUnit,
                amount,
                getMaxOrdersAllowed(player)
        );
        if (!validation.valid()) {
            player.sendMessage(validation.errorMessage());
            return null;
        }

        if (!hasItems(player, product, amount)) {
            player.sendMessage(ChatColor.RED + "You don't have enough items to sell.");
            return null;
        }

        if (!removeItems(player, product, amount)) {
            player.sendMessage(ChatColor.RED + "Failed to remove items from your inventory.");
            return null;
        }
        
        // Create order
        String orderId = orderBook.generateOrderId();
        long createdAt = System.currentTimeMillis();
        BazaarOrder order = orderBook.createOrder(
                orderId,
                player.getUniqueId(),
                profileId,
                player.getName(),
                canonicalProductId,
                BazaarOrder.OrderType.SELL,
                pricePerUnit,
                amount,
                createdAt
        );
        
        // Try to match immediately
        BazaarOrderBook.MatchResult match = orderBook.instantSell(canonicalProductId, amount, pricePerUnit);
        
        if (match.filled() > 0) {
            for (BazaarOrderBook.MatchEntry entry : match.matches()) {
                order.fill(entry.amount(), entry.price());
            }

            // Seller receives coins after Bazaar tax.
            double netEarned = applySellTax(match.totalCost(), sellTaxRateForOwner(player.getUniqueId()));
            shoppingBag.addCoins(profileId, netEarned);

            // Buyers receive items.
            Map<UUID, Integer> itemsByBuyerProfile = new HashMap<>();
            for (BazaarOrderBook.MatchEntry entry : match.matches()) {
                itemsByBuyerProfile.merge(entry.counterpartyProfileId(), entry.amount(), Integer::sum);
            }
            for (Map.Entry<UUID, Integer> entry : itemsByBuyerProfile.entrySet()) {
                shoppingBag.addItems(entry.getKey(), canonicalProductId, entry.getValue());
            }
            
            if (order.getRemainingAmount() > 0) {
                orderBook.placeOrder(order);
            }

            double avgPrice = match.totalCost() / match.filled();
            priceHistory.recordTransaction(canonicalProductId, avgPrice, match.filled(), BazaarPriceHistory.TransactionType.SELL_ORDER);
            updateProductPrice(product);

            player.sendMessage(ChatColor.GREEN + "Sell order filled " + ChatColor.YELLOW + match.filled() + "x " + product.getProductName()
                    + ChatColor.GRAY + " for " + formatCoins(netEarned)
                    + (order.getRemainingAmount() > 0 ? ChatColor.YELLOW + " (remaining: " + order.getRemainingAmount() + ")" : ""));

            return order.getRemainingAmount() > 0 ? order : null;
        }
        
        // No match - place full order
        orderBook.placeOrder(order);
        player.sendMessage(ChatColor.GREEN + "Sell order placed: " + ChatColor.YELLOW + amount + "x " + product.getProductName()
                + ChatColor.GRAY + " @ " + formatCoins(pricePerUnit) + ChatColor.GRAY + " each");
        
        return order;
    }
    
    /**
     * Execute instant buy.
     */
    public boolean instantBuy(Player player, String productId, int amount) {
        if (!requireBazaarAccess(player)) {
            return false;
        }
        BazaarProduct product = getProduct(productId);
        if (product == null) {
            player.sendMessage(ChatColor.RED + "Product not found.");
            return false;
        }
        String canonicalProductId = product.getProductId();

        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "Invalid amount.");
            return false;
        }
        if (amount > maxOrderAmount) {
            player.sendMessage(ChatColor.RED + "Amount must be between 1 and " + maxOrderAmount + ".");
            return false;
        }

        UUID profileId = requireProfileId(player);
        if (profileId == null) {
            return false;
        }

        double previewCost;
        BazaarOrderBook.MatchResult match;
        synchronized (orderBook) {
            // Preview cost so we don't mutate the order book unless the player can afford it.
            List<BazaarOrder> sellOrders = orderBook.getSellOrders(canonicalProductId);
            int previewRemaining = amount;
            int previewFilled = 0;
            previewCost = 0.0;
            for (BazaarOrder sell : sellOrders) {
                if (previewRemaining <= 0) break;
                if (!sell.isActive()) continue;
                int toFill = Math.min(previewRemaining, sell.getRemainingAmount());
                if (toFill <= 0) continue;
                previewFilled += toFill;
                previewRemaining -= toFill;
                previewCost += toFill * sell.getUnitPrice();
            }

            if (previewFilled == 0) {
                player.sendMessage(ChatColor.RED + "No sell orders available.");
                return false;
            }

            if (!hasFunds(player, previewCost)) {
                player.sendMessage(ChatColor.RED + "Insufficient funds. Need: " + formatCoins(previewCost));
                return false;
            }

            if (!withdraw(player, previewCost)) {
                player.sendMessage(ChatColor.RED + "Transaction failed.");
                return false;
            }

            // Execute against the preview fill amount we actually paid for.
            match = orderBook.instantBuy(canonicalProductId, previewFilled, Double.POSITIVE_INFINITY);
        }

        if (match.filled() == 0) {
            deposit(player, previewCost);
            player.sendMessage(ChatColor.RED + "No matching sell orders found.");
            return false;
        }

        // Refund any over-withdrawal if the book moved unexpectedly.
        double refund = previewCost - match.totalCost();
        if (refund > 0.0) {
            deposit(player, refund);
        }

        // Buyer receives items.
        shoppingBag.addItems(profileId, canonicalProductId, match.filled());

        // Sellers receive coins.
        Map<UUID, Double> coinsBySellerProfile = new HashMap<>();
        for (BazaarOrderBook.MatchEntry entry : match.matches()) {
            coinsBySellerProfile.merge(entry.counterpartyProfileId(), entry.amount() * entry.price(), Double::sum);
        }
        for (Map.Entry<UUID, Double> entry : coinsBySellerProfile.entrySet()) {
            UUID sellerProfileId = entry.getKey();
            double netCoins = applySellTax(entry.getValue(), sellTaxRateForProfile(sellerProfileId));
            if (netCoins > 0.0D) {
                shoppingBag.addCoins(sellerProfileId, netCoins);
            }
        }

        if (match.isPartiallyFilled()) {
            player.sendMessage(ChatColor.YELLOW + "Bought " + ChatColor.YELLOW + match.filled() + "x " + product.getProductName()
                    + ChatColor.GRAY + " (not enough sell offers)");
        } else {
            player.sendMessage(ChatColor.GREEN + "Bought " + ChatColor.YELLOW + match.filled() + "x " + product.getProductName()
                    + ChatColor.GRAY + " for " + formatCoins(match.totalCost()));
        }

        double avgPrice = match.totalCost() / match.filled();
        priceHistory.recordTransaction(canonicalProductId, avgPrice, match.filled(), BazaarPriceHistory.TransactionType.INSTANT_BUY);
        updateProductPrice(product);
        
        return true;
    }
    
    /**
     * Execute instant sell.
     */
    public boolean instantSell(Player player, String productId, int amount) {
        if (!requireBazaarAccess(player)) {
            return false;
        }
        BazaarProduct product = getProduct(productId);
        if (product == null) {
            player.sendMessage(ChatColor.RED + "Product not found.");
            return false;
        }
        String canonicalProductId = product.getProductId();

        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "Invalid amount.");
            return false;
        }
        if (amount > maxOrderAmount) {
            player.sendMessage(ChatColor.RED + "Amount must be between 1 and " + maxOrderAmount + ".");
            return false;
        }

        if (!hasItems(player, product, amount)) {
            player.sendMessage(ChatColor.RED + "You don't have enough items to sell.");
            return false;
        }

        UUID profileId = requireProfileId(player);
        if (profileId == null) {
            return false;
        }

        int previewFilled;
        BazaarOrderBook.MatchResult match;
        synchronized (orderBook) {
            // Preview how many will actually sell so we don't mutate the book until we have the items removed.
            List<BazaarOrder> buyOrders = orderBook.getBuyOrders(canonicalProductId);
            int previewRemaining = amount;
            previewFilled = 0;
            for (BazaarOrder buy : buyOrders) {
                if (previewRemaining <= 0) break;
                if (!buy.isActive()) continue;
                int toFill = Math.min(previewRemaining, buy.getRemainingAmount());
                if (toFill <= 0) continue;
                previewFilled += toFill;
                previewRemaining -= toFill;
            }

            if (previewFilled == 0) {
                player.sendMessage(ChatColor.RED + "No buy orders available.");
                return false;
            }

            if (!removeItems(player, product, previewFilled)) {
                player.sendMessage(ChatColor.RED + "Failed to remove items from your inventory.");
                return false;
            }

            match = orderBook.instantSell(canonicalProductId, previewFilled, 0.0);
        }

        if (match.filled() == 0) {
            // Extremely unlikely (requires the book to change unexpectedly), but don't lose the player's items.
            shoppingBag.addItems(profileId, canonicalProductId, previewFilled);
            player.sendMessage(ChatColor.RED + "No buy orders available. Items were returned to your shopping bag.");
            return false;
        }

        if (match.filled() < previewFilled) {
            int restore = previewFilled - match.filled();
            if (restore > 0) {
                shoppingBag.addItems(profileId, canonicalProductId, restore);
            }
        }
        
        // Seller receives coins after Bazaar tax.
        double netEarned = applySellTax(match.totalCost(), sellTaxRateForOwner(player.getUniqueId()));
        shoppingBag.addCoins(profileId, netEarned);

        // Buyers receive items.
        Map<UUID, Integer> itemsByBuyerProfile = new HashMap<>();
        for (BazaarOrderBook.MatchEntry entry : match.matches()) {
            itemsByBuyerProfile.merge(entry.counterpartyProfileId(), entry.amount(), Integer::sum);
        }
        for (Map.Entry<UUID, Integer> entry : itemsByBuyerProfile.entrySet()) {
            shoppingBag.addItems(entry.getKey(), canonicalProductId, entry.getValue());
        }
        
        if (match.isPartiallyFilled()) {
            player.sendMessage(ChatColor.YELLOW + "Sold " + ChatColor.YELLOW + match.filled() + "x " + product.getProductName()
                    + ChatColor.GRAY + " (not enough buy offers)");
        } else {
            player.sendMessage(ChatColor.GREEN + "Sold " + ChatColor.YELLOW + match.filled() + "x " + product.getProductName()
                    + ChatColor.GRAY + " for " + formatCoins(netEarned));
        }
        
        double avgPrice = match.totalCost() / match.filled();
        priceHistory.recordTransaction(canonicalProductId, avgPrice, match.filled(), BazaarPriceHistory.TransactionType.INSTANT_SELL);
        updateProductPrice(product);
        
        return true;
    }
    
    /**
     * Claim all items and coins from shopping bag.
     */
    public BazaarShoppingBag.BagContents claimAll(Player player) {
        UUID profileId = requireProfileId(player);
        if (profileId == null) {
            return new BazaarShoppingBag.BagContents(new HashMap<>(), 0.0);
        }

        BazaarShoppingBag.BagContents contents = shoppingBag.claimAll(profileId);
        
        if (contents.items().isEmpty() && contents.coins() <= 0) {
            player.sendMessage(ChatColor.RED + "Your shopping bag is empty.");
            return contents;
        }
        
        // Give items
        for (Map.Entry<String, Integer> entry : contents.items().entrySet()) {
            BazaarProduct product = getProduct(entry.getKey());
            if (product == null) {
                shoppingBag.addItems(profileId, entry.getKey(), entry.getValue());
                continue;
            }
            
            ItemStack item;
            if (product.isCustomItem()) {
                item = customItemService == null ? null : customItemService.createItemByKey(product.getCustomItemKey());
            } else {
                item = new ItemStack(product.getIcon());
            }
            if (item == null) {
                shoppingBag.addItems(profileId, product.getProductId(), entry.getValue());
                continue;
            }

            int remaining = entry.getValue();
            int maxStack = Math.max(1, item.getMaxStackSize());
            while (remaining > 0) {
                int stackAmount = Math.min(remaining, maxStack);
                ItemStack stack = item.clone();
                stack.setAmount(stackAmount);

                Map<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
                for (ItemStack leftover : leftovers.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }

                remaining -= stackAmount;
            }
        }
        
        // Give coins
        if (contents.coins() > 0) {
            if (!deposit(player, contents.coins())) {
                shoppingBag.addCoins(profileId, contents.coins());
                player.sendMessage(ChatColor.RED + "No Skyblock profile selected. Coins were kept in your shopping bag.");
            }
        }
        
        player.sendMessage(ChatColor.GREEN + "Claimed from shopping bag:");
        if (!contents.items().isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "  Items: " + contents.items().size() + " product types");
        }
        if (contents.coins() > 0) {
            player.sendMessage(ChatColor.GOLD + "  Coins: " + formatCoins(contents.coins()));
        }
        
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f);
        
        return contents;
    }

    public boolean claimCoins(Player player) {
        UUID profileId = requireProfileId(player);
        if (profileId == null) {
            return false;
        }

        double coins = shoppingBag.claimCoins(profileId);
        if (coins <= 0.0) {
            player.sendMessage(ChatColor.RED + "Your shopping bag has no coins.");
            return false;
        }

        if (!deposit(player, coins)) {
            shoppingBag.addCoins(profileId, coins);
            player.sendMessage(ChatColor.RED + "No Skyblock profile selected. Coins were kept in your shopping bag.");
            return false;
        }

        player.sendMessage(ChatColor.GREEN + "Claimed " + formatCoins(coins) + ChatColor.GREEN + " from your shopping bag.");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f);
        return true;
    }

    public boolean claimItem(Player player, String productId, int amount) {
        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "Invalid amount.");
            return false;
        }

        BazaarProduct product = getProduct(productId);
        if (product == null) {
            player.sendMessage(ChatColor.RED + "Product not found.");
            return false;
        }

        UUID profileId = requireProfileId(player);
        if (profileId == null) {
            return false;
        }

        Map<String, Integer> bagItems = shoppingBag.getBag(profileId).getItems();
        String bagKey = null;
        for (String key : bagItems.keySet()) {
            if (key.equalsIgnoreCase(product.getProductId()) || key.equalsIgnoreCase(productId)) {
                bagKey = key;
                break;
            }
        }

        if (bagKey == null) {
            player.sendMessage(ChatColor.RED + "That item isn't in your shopping bag.");
            return false;
        }

        int available = bagItems.getOrDefault(bagKey, 0);
        int toClaim = Math.min(amount, available);
        if (toClaim <= 0) {
            player.sendMessage(ChatColor.RED + "That item isn't in your shopping bag.");
            return false;
        }

        ItemStack item;
        if (product.isCustomItem()) {
            if (customItemService == null) {
                player.sendMessage(ChatColor.RED + "Custom items are unavailable right now.");
                return false;
            }
            item = customItemService.createItemByKey(product.getCustomItemKey());
        } else {
            item = new ItemStack(product.getIcon());
        }

        if (item == null) {
            player.sendMessage(ChatColor.RED + "Failed to create item.");
            return false;
        }

        int remaining = toClaim;
        int maxStack = Math.max(1, item.getMaxStackSize());
        while (remaining > 0) {
            int stackAmount = Math.min(remaining, maxStack);
            ItemStack stack = item.clone();
            stack.setAmount(stackAmount);

            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
            for (ItemStack leftover : leftovers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }

            remaining -= stackAmount;
        }

        shoppingBag.removeItems(profileId, bagKey, toClaim);

        player.sendMessage(ChatColor.GREEN + "Claimed " + ChatColor.YELLOW + toClaim + "x " + product.getProductName()
                + ChatColor.GREEN + " from your shopping bag.");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f);
        return true;
    }
    
    /**
     * Cancel an order and refund.
     */
    public boolean cancelOrder(Player player, String orderId) {
        if (!requireBazaarAccess(player)) {
            return false;
        }
        UUID profileId = requireProfileId(player);
        if (profileId == null) {
            return false;
        }

        BazaarOrder order = null;
        UUID ownerId = player.getUniqueId();
        for (BazaarOrder o : orderBook.getProfileOrders(profileId, ownerId)) {
            if (o.getOrderId().equals(orderId)) {
                order = o;
                break;
            }
        }
        
        if (order == null) {
            player.sendMessage(ChatColor.RED + "Order not found.");
            return false;
        }

        if (order.getOrderType() == BazaarOrder.OrderType.BUY) {
            double refund = order.getRemainingValue();
            if (refund > 0.0) {
                shoppingBag.addCoins(profileId, refund);
            }
            player.sendMessage(ChatColor.GREEN + "Cancelled buy order. Refunded " + formatCoins(refund)
                    + ChatColor.GRAY + " to your shopping bag.");
        } else {
            int remaining = order.getRemainingAmount();
            if (remaining > 0) {
                shoppingBag.addItems(profileId, order.getProductId(), remaining);
            }
            player.sendMessage(ChatColor.GREEN + "Cancelled sell order. Remaining items returned to your shopping bag.");
        }

        orderBook.removeOrder(orderId);
        orderBook.save();
        return true;
    }

    private boolean giveProductItems(Player player, BazaarProduct product, int amount) {
        if (amount <= 0) {
            return true;
        }
        if (player == null || product == null) {
            return false;
        }

        ItemStack item;
        if (product.isCustomItem()) {
            if (customItemService == null) {
                return false;
            }
            item = customItemService.createItemByKey(product.getCustomItemKey());
        } else {
            item = new ItemStack(product.getIcon());
        }

        if (item == null) {
            return false;
        }

        int remaining = amount;
        int maxStack = Math.max(1, item.getMaxStackSize());
        while (remaining > 0) {
            int stackAmount = Math.min(remaining, maxStack);
            ItemStack stack = item.clone();
            stack.setAmount(stackAmount);

            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
            for (ItemStack leftover : leftovers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }

            remaining -= stackAmount;
        }

        return true;
    }

    public UUID getSelectedProfileId(Player player) {
        SkyBlockProfile profile = profileEconomy.getSelectedProfile(player);
        return profile != null ? profile.getProfileId() : null;
    }

    private boolean requireBazaarAccess(Player player) {
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

    public UUID requireProfileId(Player player) {
        SkyBlockProfile profile = profileEconomy.requireSelectedProfile(player);
        if (profile == null) {
            return null;
        }

        UUID profileId = profile.getProfileId();
        orderBook.migrateLegacyOrders(player.getUniqueId(), profileId);
        shoppingBag.migrateLegacyBag(player.getUniqueId(), profileId);
        return profileId;
    }

    // ==================== INVENTORY HELPERS ====================

    private boolean hasItems(Player player, BazaarProduct product, int amount) {
        if (amount <= 0) {
            return true;
        }
        return countItems(player, product) >= amount;
    }

    private int countItems(Player player, BazaarProduct product) {
        if (player == null || product == null) {
            return 0;
        }

        int total = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (matchesProduct(stack, product)) {
                total += stack.getAmount();
            }
        }
        for (ItemStack stack : player.getInventory().getExtraContents()) {
            if (matchesProduct(stack, product)) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    private boolean removeItems(Player player, BazaarProduct product, int amount) {
        if (amount <= 0) {
            return true;
        }
        if (!hasItems(player, product, amount)) {
            return false;
        }

        int remaining = amount;
        var inventory = player.getInventory();

        ItemStack[] storage = inventory.getStorageContents();
        remaining = removeFromContents(storage, product, remaining);
        inventory.setStorageContents(storage);

        if (remaining > 0) {
            ItemStack[] extra = inventory.getExtraContents();
            remaining = removeFromContents(extra, product, remaining);
            inventory.setExtraContents(extra);
        }

        if (remaining > 0) {
            plugin.getLogger().warning("Failed to remove expected items from " + player.getName() + " for " + product.getProductId());
            return false;
        }

        player.updateInventory();
        return true;
    }

    private int removeFromContents(ItemStack[] contents, BazaarProduct product, int remaining) {
        if (contents == null || product == null) {
            return remaining;
        }

        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (!matchesProduct(stack, product)) {
                continue;
            }

            int take = Math.min(remaining, stack.getAmount());
            int newAmount = stack.getAmount() - take;
            remaining -= take;

            if (newAmount <= 0) {
                contents[i] = null;
            } else {
                stack.setAmount(newAmount);
                contents[i] = stack;
            }
        }

        return remaining;
    }

    private boolean matchesProduct(ItemStack stack, BazaarProduct product) {
        if (stack == null || stack.getType().isAir() || product == null) {
            return false;
        }
        if (isBlockedFromSelling(stack)) {
            return false;
        }

        if (product.isCustomItem()) {
            if (customItemService == null) {
                return false;
            }
            String itemId = customItemService.itemId(stack);
            return itemId != null && itemId.equalsIgnoreCase(product.getCustomItemKey());
        }

        return product.getIcon() != null && stack.getType() == product.getIcon();
    }

    private boolean isBlockedFromSelling(ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) {
            return false;
        }
        var meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        // Navigation item (Skyblock Menu)
        if (meta.getPersistentDataContainer().has(navigationItemKey, PersistentDataType.BYTE)) {
            return true;
        }

        // Personal Compacters are blocked from Bazaar/NPC shops (allowed on AH)
        if (plugin.getPersonalCompactorManager() != null && plugin.getPersonalCompactorManager().isCompactorItem(stack)) {
            return true;
        }

        return false;
    }
    
    // ==================== ECONOMY HELPERS ====================
    
    private boolean hasFunds(Player player, double amount) {
        if (amount <= 0.0) {
            return true;
        }
        if (profileEconomy.requireSelectedProfile(player) == null) {
            return false;
        }
        return profileEconomy.has(player, amount);
    }
    
    private boolean withdraw(Player player, double amount) {
        if (amount <= 0.0) {
            return true;
        }
        if (profileEconomy.requireSelectedProfile(player) == null) {
            return false;
        }
        return profileEconomy.withdraw(player, amount);
    }
    
    private boolean deposit(Player player, double amount) {
        if (amount <= 0.0) {
            return true;
        }
        if (profileEconomy.requireSelectedProfile(player) == null) {
            return false;
        }
        return profileEconomy.deposit(player, amount);
    }
    
    public String formatCoins(double amount) {
        if (!Double.isFinite(amount) || amount < 0.0D) {
            return ChatColor.RED + "N/A";
        }
        if (amount >= 1_000_000_000D) {
            return ChatColor.GOLD + String.format(Locale.US, "%.2f", amount / 1_000_000_000D) + ChatColor.GRAY + "B";
        }
        if (amount >= 1_000_000D) {
            return ChatColor.GOLD + String.format(Locale.US, "%.2f", amount / 1_000_000D) + ChatColor.GRAY + "M";
        }
        if (amount >= 1_000D) {
            return ChatColor.GOLD + String.format(Locale.US, "%.1f", amount / 1_000D) + ChatColor.GRAY + "K";
        }
        return ChatColor.YELLOW + String.format(Locale.US, "%.1f", amount);
    }

    public void shutdown() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        if (accountUpgrades != null) {
            accountUpgrades.save();
        }
        shoppingBag.save();
        priceHistory.save();
    }
    
    // ==================== HELPER METHODS FOR BAZAAR MENU SERVICE ====================
    
    /**
     * Price information for an item.
     */
    public record PriceInfo(double buyPrice, double sellPrice) {}
    
    /**
     * Get price info for a custom item.
     */
    public PriceInfo customPriceInfo(String customKey) {
        BazaarProduct product = getProductByCustomKey(customKey);
        if (product == null) {
            return new PriceInfo(0.0, 0.0);
        }
        return new PriceInfo(product.getInstantBuyPrice(), product.getInstantSellPrice());
    }
    
    /**
     * Get price info for a material.
     */
    public PriceInfo materialPriceInfo(Material material) {
        BazaarProduct product = getProductByMaterial(material);
        if (product == null) {
            return new PriceInfo(0.0, 0.0);
        }
        return new PriceInfo(product.getInstantBuyPrice(), product.getInstantSellPrice());
    }
    
    /**
     * List all custom item keys.
     */
    public List<String> listCustomKeys() {
        return products.values().stream()
                .filter(BazaarProduct::isCustomItem)
                .map(BazaarProduct::getCustomItemKey)
                .filter(Objects::nonNull)
                .map(key -> key.toLowerCase(Locale.ROOT))
                .distinct()
                .sorted()
                .toList();
    }
    
    /**
     * List all vanilla materials available in bazaar.
     */
    public List<Material> listVanillaMaterials() {
        return products.values().stream()
            .filter(p -> !p.isCustomItem())
            .map(BazaarProduct::getIcon)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }
    
    /**
     * Instant buy for custom item.
     */
    public void instantBuyCustom(Player player, String customKey, int amount) {
        BazaarProduct product = getProductByCustomKey(customKey);
        if (product == null) {
            player.sendMessage(ChatColor.RED + "Product not found.");
            return;
        }
        instantBuy(player, product.getProductId(), amount);
    }
    
    /**
     * Instant buy for material.
     */
    public void instantBuyMaterial(Player player, Material material, int amount) {
        BazaarProduct product = getProductByMaterial(material);
        if (product == null) {
            player.sendMessage(ChatColor.RED + "Product not found.");
            return;
        }
        instantBuy(player, product.getProductId(), amount);
    }
    
    /**
     * Instant sell for custom item.
     */
    public void instantSellCustom(Player player, String customKey, int amount) {
        BazaarProduct product = getProductByCustomKey(customKey);
        if (product == null) {
            player.sendMessage(ChatColor.RED + "Product not found.");
            return;
        }
        instantSell(player, product.getProductId(), amount);
    }
    
    /**
     * Instant sell for material.
     */
    public void instantSellMaterial(Player player, Material material, int amount) {
        BazaarProduct product = getProductByMaterial(material);
        if (product == null) {
            player.sendMessage(ChatColor.RED + "Product not found.");
            return;
        }
        instantSell(player, product.getProductId(), amount);
    }
}


