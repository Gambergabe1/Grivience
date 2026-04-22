package io.papermc.Grivience.minion;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.event.GlobalEventManager;
import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.item.EnchantedFarmItemType;
import io.papermc.Grivience.util.EnchantedItemRecipeCatalog;
import io.papermc.Grivience.util.EnchantedItemRecipePattern;
import io.papermc.Grivience.skyblock.island.Island;
import io.papermc.Grivience.skyblock.island.IslandManager;
import io.papermc.Grivience.skyblock.profile.ProfileManager;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class MinionManager {
    private static final int ACTIONS_PER_ITEM = 2;
    private static final int[] OUTER_SLOTS = {0, 1, 2, 3, 5, 6, 7, 8};
    private static final String UTILITY_FUEL = "fuel";
    private static final String UTILITY_UPGRADE = "upgrade";
    private static final int[] CONSTELLATION_TYPE_REQUIREMENTS = {0, 3, 5, 8};
    private static final double[] CONSTELLATION_SPEED_MULTIPLIERS = {1.0D, 1.10D, 1.20D, 1.30D};
    private static final double[] CONSTELLATION_FRAGMENT_CHANCE = {0.0D, 0.0D, 0.05D, 0.10D};
    private static final double OVERCLOCK_CHIP_SPEED_MULTIPLIER = 1.15D;
    private static final double ASTRAL_RESONATOR_SPEED_MULTIPLIER = 1.08D;
    private static final double ASTRAL_RESONATOR_FRAGMENT_CHANCE_BONUS = 0.05D;
    private static final double DISPLAY_SEARCH_RADIUS_XZ = 1.5D;
    private static final double DISPLAY_SEARCH_RADIUS_Y = 3.0D;

    private static final Map<String, IngredientDefinition> INGREDIENTS = buildIngredientDefinitions();
    private static final List<IngredientRecipe> INGREDIENT_RECIPES = buildIngredientRecipes();
    private static final Map<String, FuelDefinition> FUELS = buildFuelDefinitions();
    private static final Map<String, UpgradeDefinition> UPGRADES = buildUpgradeDefinitions();
    private static final List<UtilityRecipe> UTILITY_RECIPES = buildUtilityRecipes();
    private static final Map<String, String> AUTO_SMELT_MAP = buildAutoSmeltMap();
    private static final Map<String, CompactRule> COMPACTOR_RULES = buildCompactorRules();
    private static final List<SuperCompactorRule> SUPER_COMPACTOR_RULES = buildSuperCompactorRules();
    private static final Map<String, Double> NPC_SELL_PRICES = buildNpcSellPrices();
    private static final Set<String> CUSTOM_INGREDIENT_IDS = INGREDIENTS.values().stream()
            .filter(IngredientDefinition::customOnly)
            .map(IngredientDefinition::id)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());

    static {
        validateEnchantedRecipeCoverage();
    }

    private final GriviencePlugin plugin;
    private final IslandManager islandManager;
    private final File dataFile;

    private final NamespacedKey minionEntityIdKey;
    private final NamespacedKey minionItemTypeKey;
    private final NamespacedKey minionItemTierKey;
    private final NamespacedKey minionIngredientIdKey;
    private final NamespacedKey minionUtilityTypeKey;
    private final NamespacedKey minionUtilityIdKey;

    private final Map<UUID, MinionInstance> minionsById = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> minionIdsByIsland = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> minionIdByDisplayEntity = new ConcurrentHashMap<>();
    private final Map<String, Double> cachedNpcSellPrices = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> constellationTierOverrideByIsland = new ConcurrentHashMap<>();

    private boolean enabled = true;
    private long tickIntervalTicks = 20L;
    private long autoSaveIntervalTicks = 20L * 60L;
    private int islandBaseLimit = 5;
    private int limitPerIslandLevel = 1;
    private double speedMultiplier = 1.0D;
    private double storageMultiplier = 1.0D;
    private int maxCatchupHours = 24;
    private int maxActionsPerTick = 3600;

    private int processingTaskId = -1;
    private int autoSaveTaskId = -1;
    private int displayAuditTaskId = -1;
    private boolean dirty = false;

    public MinionManager(GriviencePlugin plugin, IslandManager islandManager) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.dataFile = new File(plugin.getDataFolder(), "skyblock/minions.yml");
        this.minionEntityIdKey = new NamespacedKey(plugin, "minion-entity-id");
        this.minionItemTypeKey = new NamespacedKey(plugin, "minion-item-type");
        this.minionItemTierKey = new NamespacedKey(plugin, "minion-item-tier");
        this.minionIngredientIdKey = new NamespacedKey(plugin, "minion-ingredient-id");
        this.minionUtilityTypeKey = new NamespacedKey(plugin, "minion-utility-type");
        this.minionUtilityIdKey = new NamespacedKey(plugin, "minion-utility-id");

        reloadFromConfig();
        loadData();
        startTasks();
        Bukkit.getScheduler().runTask(plugin, this::spawnAllDisplays);
    }

    public GriviencePlugin getPlugin() {
        return plugin;
    }

    public void reloadFromConfig() {
        enabled = plugin.getConfig().getBoolean("minions.enabled", true);
        tickIntervalTicks = Math.max(10L, plugin.getConfig().getLong("minions.tick-interval-ticks", 20L));
        int autoSaveSeconds = Math.max(10, plugin.getConfig().getInt("minions.auto-save-interval-seconds", 60));
        autoSaveIntervalTicks = autoSaveSeconds * 20L;
        islandBaseLimit = plugin.getConfig().getInt("minions.base-island-limit", 5);
        limitPerIslandLevel = plugin.getConfig().getInt("minions.limit-per-island-level", 1);
        speedMultiplier = Math.max(0.1D, plugin.getConfig().getDouble("minions.speed-multiplier", 1.0D));
        storageMultiplier = Math.max(0.1D, plugin.getConfig().getDouble("minions.storage-multiplier", 1.0D));
        maxCatchupHours = Math.max(1, plugin.getConfig().getInt("minions.max-catchup-hours", 24));
        maxActionsPerTick = Math.max(60, plugin.getConfig().getInt("minions.max-actions-per-tick", 3600));

        restartTasks();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getTotalMinions() {
        return minionsById.size();
    }

    public ItemStack createMinionItem(MinionType type, int tier) {
        if (type == null) {
            return null;
        }
        int clampedTier = Math.max(1, Math.min(tier, type.maxTier()));
        ItemStack item = new ItemStack(type.iconMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(ChatColor.GREEN + type.displayName() + " Minion " + ChatColor.YELLOW + roman(clampedTier));
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Automates " + type.displayName().toLowerCase(Locale.ROOT) + " production.");
        lore.add("");
        lore.add(ChatColor.GRAY + "Tier: " + ChatColor.GOLD + roman(clampedTier));
        lore.add(ChatColor.GRAY + "Speed: " + ChatColor.AQUA + String.format(Locale.US, "%.1fs/action", getSecondsPerAction(type, clampedTier)));
        lore.add(ChatColor.GRAY + "Storage: " + ChatColor.YELLOW + formatInt(getStorageCap(type, clampedTier)));
        lore.add("");
        lore.add(ChatColor.YELLOW + "Place on your island to activate.");
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.getPersistentDataContainer().set(minionItemTypeKey, PersistentDataType.STRING, type.id());
        meta.getPersistentDataContainer().set(minionItemTierKey, PersistentDataType.INTEGER, clampedTier);
        item.setItemMeta(meta);
        return item;
    }

    public MinionItemData readMinionItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        String rawType = meta.getPersistentDataContainer().get(minionItemTypeKey, PersistentDataType.STRING);
        Integer tier = meta.getPersistentDataContainer().get(minionItemTierKey, PersistentDataType.INTEGER);
        if (rawType == null || tier == null) {
            return null;
        }
        MinionType type = MinionType.parse(rawType);
        if (type == null) {
            return null;
        }
        return new MinionItemData(type, Math.max(1, Math.min(tier, type.maxTier())));
    }

    public boolean isMinionItem(ItemStack item) {
        return readMinionItem(item) != null;
    }

    public ItemStack createIngredientItem(String ingredientId, int amount) {
        IngredientDefinition definition = ingredient(ingredientId);
        if (definition == null || amount <= 0) {
            return null;
        }

        CustomItemService itemService = plugin.getCustomItemService();
        if (itemService != null) {
            // Check for Farm outputs first (existing logic)
            EnchantedFarmItemType farmType = EnchantedFarmItemType.parse(definition.id());
            if (farmType != null) {
                ItemStack customFarmItem = itemService.createEnchantedFarmItem(farmType);
                if (customFarmItem != null && !customFarmItem.getType().isAir()) {
                    customFarmItem.setAmount(Math.min(amount, customFarmItem.getMaxStackSize()));
                    return customFarmItem;
                }
            }
            
            // Check for generalized custom items (Sapphire, etc.)
            ItemStack customItem = itemService.createItemByKey(definition.id());
            if (customItem != null && !customItem.getType().isAir()) {
                customItem.setAmount(Math.min(amount, customItem.getMaxStackSize()));
                return customItem;
            }
        }

        ItemStack stack = new ItemStack(definition.material(), Math.min(amount, definition.material().getMaxStackSize()));
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        meta.setDisplayName(ChatColor.GREEN + definition.displayName());
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Minion crafting ingredient.");
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "ID: " + ChatColor.GRAY + definition.id());
        lore.add("");
        lore.add(ChatColor.BLUE + "" + ChatColor.BOLD + "UNCOMMON " + ChatColor.BLUE + "CRAFTING MATERIAL");
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.getPersistentDataContainer().set(minionIngredientIdKey, PersistentDataType.STRING, definition.id());
        stack.setItemMeta(meta);
        return stack;
    }

    public ItemStack createFuelItem(String fuelId, int amount) {
        return createUtilityItem(UTILITY_FUEL, normalizeUtilityId(fuelId), amount);
    }

    public ItemStack createUpgradeItem(String upgradeId, int amount) {
        return createUtilityItem(UTILITY_UPGRADE, normalizeUtilityId(upgradeId), amount);
    }

    public String readFuelId(ItemStack stack) {
        String utilityType = readUtilityType(stack);
        String utilityId = readUtilityId(stack);
        if (UTILITY_FUEL.equals(utilityType) && utilityId != null) {
            return utilityId;
        }

        if (stack == null || stack.getType().isAir()) {
            return null;
        }
        // Vanilla fallback fuels.
        return switch (stack.getType()) {
            case COAL -> "coal";
            case CHARCOAL -> "charcoal";
            case COAL_BLOCK -> "block_of_coal";
            default -> null;
        };
    }

    public String readUpgradeId(ItemStack stack) {
        String utilityType = readUtilityType(stack);
        String utilityId = readUtilityId(stack);
        if (UTILITY_UPGRADE.equals(utilityType) && utilityId != null) {
            return utilityId;
        }
        return null;
    }

    public boolean isFuelItem(ItemStack stack) {
        String id = readFuelId(stack);
        return id != null && fuelDefinition(id) != null;
    }

    public boolean isUpgradeItem(ItemStack stack) {
        String id = readUpgradeId(stack);
        return id != null && upgradeDefinition(id) != null;
    }

    public List<String> fuelIds() {
        return new ArrayList<>(FUELS.keySet());
    }

    public List<String> upgradeIds() {
        return new ArrayList<>(UPGRADES.keySet());
    }

    public String fuelDisplayName(String fuelId) {
        FuelDefinition definition = fuelDefinition(fuelId);
        return definition == null ? prettyIngredientName(fuelId) : definition.displayName();
    }

    public String upgradeDisplayName(String upgradeId) {
        UpgradeDefinition definition = upgradeDefinition(upgradeId);
        return definition == null ? prettyIngredientName(upgradeId) : definition.displayName();
    }

    public String readIngredientId(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = stack.getItemMeta();
        String direct = meta.getPersistentDataContainer().get(minionIngredientIdKey, PersistentDataType.STRING);
        if (direct != null && !direct.isBlank()) {
            return normalizeIngredientId(direct);
        }

        if (plugin.getCustomItemService() != null) {
            String customId = plugin.getCustomItemService().itemId(stack);
            if (customId != null && !customId.isBlank()) {
                return normalizeIngredientId(customId);
            }
        }
        return null;
    }

    public String getIngredientDisplayName(String ingredientId) {
        IngredientDefinition definition = ingredient(ingredientId);
        return definition == null ? prettyIngredientName(ingredientId) : definition.displayName();
    }

    public boolean matchesIngredient(ItemStack stack, String expectedId, int requiredAmount) {
        if (stack == null || requiredAmount <= 0 || stack.getAmount() < requiredAmount) {
            return false;
        }
        String normalized = normalizeIngredientId(expectedId);
        IngredientDefinition definition = ingredient(normalized);
        if (definition == null) {
            return false;
        }

        String stackIngredientId = readIngredientId(stack);
        if (definition.customOnly()) {
            return normalized.equals(stackIngredientId);
        }

        if (stackIngredientId != null && CUSTOM_INGREDIENT_IDS.contains(stackIngredientId)) {
            return false;
        }
        return stack.getType() == definition.material();
    }

    /**
     * Exposes Super Compactor transforms in normalized ingredient-id form.
     * Personal Compactor uses this so both systems share the same compacting recipes.
     */
    public List<SuperCompactorRuleInfo> superCompactorRules() {
        List<SuperCompactorRuleInfo> out = new ArrayList<>(SUPER_COMPACTOR_RULES.size());
        for (SuperCompactorRule rule : SUPER_COMPACTOR_RULES) {
            if (rule == null || rule.inputAmount() <= 0 || rule.outputAmount() <= 0) {
                continue;
            }
            out.add(new SuperCompactorRuleInfo(rule.inputId(), rule.inputAmount(), rule.outputId(), rule.outputAmount()));
        }
        return out;
    }

    public boolean tryPlaceMinion(Player player, Block clickedBlock, BlockFace face, ItemStack inHand) {
        if (!enabled) {
            player.sendMessage(ChatColor.RED + "Minions are currently disabled.");
            return false;
        }
        if (player == null || clickedBlock == null || face == null || inHand == null) {
            return false;
        }

        MinionItemData itemData = readMinionItem(inHand);
        if (itemData == null) {
            return false;
        }

        Island island = islandManager.getIsland(player);
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You need an island before placing minions.");
            return false;
        }
        if (!canManageIsland(player, island)) {
            player.sendMessage(ChatColor.RED + "You cannot place minions on this island.");
            return false;
        }
        if (!island.isWithinIsland(clickedBlock.getLocation())) {
            player.sendMessage(ChatColor.RED + "You can only place minions inside your island boundaries.");
            return false;
        }

        int current = getMinionCountForIsland(island.getId());
        int limit = getIslandMinionLimit(island);
        if (limit >= 0 && current >= limit) {
            player.sendMessage(ChatColor.RED + "Minion limit reached (" + current + "/" + limit + ").");
            player.sendMessage(ChatColor.GRAY + "Upgrade your island to unlock more slots.");
            return false;
        }

        Block target = clickedBlock.getRelative(face);
        if (!target.getType().isAir()) {
            player.sendMessage(ChatColor.RED + "You need an empty block to place that minion.");
            return false;
        }
        if (!target.getRelative(BlockFace.UP).getType().isAir()) {
            player.sendMessage(ChatColor.RED + "Not enough vertical space for a minion.");
            return false;
        }
        if (!target.getRelative(BlockFace.DOWN).getType().isSolid()) {
            player.sendMessage(ChatColor.RED + "Minions require a solid block beneath them.");
            return false;
        }

        Location location = target.getLocation().add(0.5D, 0.05D, 0.5D);
        if (!island.isWithinIsland(location)) {
            player.sendMessage(ChatColor.RED + "You can only place minions inside your island boundaries.");
            return false;
        }

        UUID profileId = island.getProfileId();
        if (profileId == null) {
            ProfileManager profileManager = plugin.getProfileManager();
            if (profileManager != null) {
                SkyBlockProfile profile = profileManager.getSelectedProfile(player);
                if (profile != null) {
                    profileId = profile.getProfileId();
                }
            }
        }

        long now = System.currentTimeMillis();
        MinionInstance minion = new MinionInstance(
                UUID.randomUUID(),
                island.getId(),
                island.getOwner(),
                profileId,
                itemData.type(),
                itemData.tier(),
                0,
                0,
                now,
                now,
                location,
                null
        );
        addMinion(minion);
        spawnOrAttachDisplay(minion);

        int amount = inHand.getAmount();
        if (amount <= 1) {
            inHand.setAmount(0);
        } else {
            inHand.setAmount(amount - 1);
        }

        player.sendMessage(ChatColor.GREEN + "Placed " + ChatColor.AQUA + minion.type().displayName()
                + ChatColor.GREEN + " Minion " + ChatColor.YELLOW + roman(minion.tier()) + ChatColor.GREEN + ".");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7F, 1.2F);
        return true;
    }

    public List<MinionInstance> getMinionsForIsland(Island island) {
        if (island == null) {
            return List.of();
        }
        Set<UUID> ids = minionIdsByIsland.get(island.getId());
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<MinionInstance> list = new ArrayList<>(ids.size());
        for (UUID id : ids) {
            MinionInstance minion = minionsById.get(id);
            if (minion != null) {
                list.add(minion);
            }
        }
        list.sort(Comparator.comparing((MinionInstance m) -> m.type().displayName())
                .thenComparingInt(MinionInstance::tier));
        return list;
    }

    public MinionInstance getMinion(UUID minionId) {
        if (minionId == null) {
            return null;
        }
        return minionsById.get(minionId);
    }

    public MinionInstance getMinionByEntity(Entity entity) {
        if (entity == null) {
            return null;
        }
        String idRaw = entity.getPersistentDataContainer().get(minionEntityIdKey, PersistentDataType.STRING);
        if (idRaw != null && !idRaw.isBlank()) {
            try {
                UUID minionId = UUID.fromString(idRaw);
                MinionInstance minion = minionsById.get(minionId);
                if (minion != null) {
                    return minion;
                }
            } catch (Exception ignored) {
            }
        }

        UUID mapped = minionIdByDisplayEntity.get(entity.getUniqueId());
        if (mapped == null) {
            return null;
        }
        return minionsById.get(mapped);
    }

    public boolean isMinionEntity(Entity entity) {
        return getMinionByEntity(entity) != null;
    }

    public boolean canManageMinion(Player player, MinionInstance minion) {
        if (player == null || minion == null) {
            return false;
        }
        Island island = islandManager.getIslandById(minion.islandId());
        if (island != null) {
            return canManageIsland(player, island);
        }
        return hasBypass(player) || player.getUniqueId().equals(minion.ownerId());
    }

    public boolean canManageIsland(Player player, Island island) {
        if (player == null || island == null) {
            return false;
        }
        if (hasBypass(player)) {
            return true;
        }
        UUID playerId = player.getUniqueId();
        return (island.getOwner() != null && island.getOwner().equals(playerId)) || island.isMember(playerId);
    }

    public int collectMinion(Player player, UUID minionId) {
        MinionInstance minion = minionsById.get(minionId);
        if (minion == null) {
            if (player != null) {
                player.sendMessage(ChatColor.RED + "That minion no longer exists.");
            }
            return 0;
        }
        return collectMinion(player, minion, true);
    }

    public int collectAll(Player player) {
        if (player == null) {
            return 0;
        }
        Island island = islandManager.getIsland(player);
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You don't have an island.");
            return 0;
        }
        if (!canManageIsland(player, island)) {
            player.sendMessage(ChatColor.RED + "You cannot manage minions here.");
            return 0;
        }

        List<MinionInstance> minions = getMinionsForIsland(island);
        if (minions.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "You have no placed minions on this island.");
            return 0;
        }

        int total = 0;
        for (MinionInstance minion : minions) {
            total += collectMinion(player, minion, false);
        }

        if (total > 0) {
            player.sendMessage(ChatColor.GREEN + "Collected " + ChatColor.AQUA + formatInt(total)
                    + ChatColor.GREEN + " items from all minions.");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7F, 1.2F);
        } else {
            player.sendMessage(ChatColor.YELLOW + "No minion resources to collect.");
        }
        return total;
    }

    public boolean upgradeMinion(Player player, UUID minionId) {
        MinionInstance minion = minionsById.get(minionId);
        if (minion == null) {
            if (player != null) {
                player.sendMessage(ChatColor.RED + "That minion no longer exists.");
            }
            return false;
        }
        if (player == null || !canManageMinion(player, minion)) {
            if (player != null) {
                player.sendMessage(ChatColor.RED + "You cannot upgrade that minion.");
            }
            return false;
        }

        int maxTier = minion.type().maxCraftableTier();
        if (minion.tier() >= maxTier) {
            player.sendMessage(ChatColor.YELLOW + "That minion is already max tier.");
            return false;
        }

        MinionType.TierRecipe recipe = minion.type().recipeForTier(minion.tier() + 1);
        if (recipe == null) {
            player.sendMessage(ChatColor.RED + "No upgrade recipe found for this minion tier.");
            return false;
        }

        String ingredientId = recipe.ingredientId();
        int cost = recipe.amountPerOuterSlot() * OUTER_SLOTS.length;
        int has = countIngredient(player, ingredientId);
        if (has < cost) {
            player.sendMessage(ChatColor.RED + "You need " + ChatColor.YELLOW + formatInt(cost)
                    + ChatColor.RED + " " + getIngredientDisplayName(ingredientId).toLowerCase(Locale.ROOT)
                    + ChatColor.RED + " to upgrade this minion.");
            return false;
        }

        consumeIngredient(player, ingredientId, cost);
        minion.setTier(minion.tier() + 1);
        enforceStorageCap(minion);
        updateDisplay(minion);
        markDirty();

        player.sendMessage(ChatColor.GREEN + "Upgraded to " + ChatColor.AQUA + minion.type().displayName()
                + ChatColor.GREEN + " Minion " + ChatColor.YELLOW + roman(minion.tier()) + ChatColor.GREEN + ".");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8F, 1.2F);
        return true;
    }

    public boolean pickupMinion(Player player, UUID minionId) {
        MinionInstance minion = minionsById.get(minionId);
        if (minion == null) {
            if (player != null) {
                player.sendMessage(ChatColor.RED + "That minion no longer exists.");
            }
            return false;
        }
        return pickupMinion(player, minion, true);
    }

    public int pickupAll(Player player) {
        if (player == null) {
            return 0;
        }
        Island island = islandManager.getIsland(player);
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You don't have an island.");
            return 0;
        }
        if (!canManageIsland(player, island)) {
            player.sendMessage(ChatColor.RED + "You cannot manage minions here.");
            return 0;
        }

        List<MinionInstance> minions = new ArrayList<>(getMinionsForIsland(island));
        if (minions.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "You have no placed minions on this island.");
            return 0;
        }

        int removed = 0;
        for (MinionInstance minion : minions) {
            if (pickupMinion(player, minion, false)) {
                removed++;
            }
        }

        player.sendMessage(ChatColor.GREEN + "Picked up " + ChatColor.AQUA + removed
                + ChatColor.GREEN + " minion(s) from your island.");
        return removed;
    }

    public long getTotalStored(Island island) {
        if (island == null) {
            return 0L;
        }
        long total = 0L;
        for (MinionInstance minion : getMinionsForIsland(island)) {
            total += minion.storedAmount();
        }
        return total;
    }

    public ConstellationInfo getConstellationInfo(Island island) {
        return getConstellationInfo(island == null ? null : island.getId());
    }

    public ConstellationInfo getConstellationInfo(UUID islandId) {
        if (islandId == null) {
            return new ConstellationInfo(0, 0, null, 1.0D, 0.0D);
        }
        Integer override = constellationTierOverrideByIsland.get(islandId);
        int uniqueTypes = uniqueMinionTypeCount(islandId);
        int tier = override == null
                ? constellationTierForUniqueTypes(uniqueTypes)
                : clampConstellationTier(override);
        return new ConstellationInfo(
                tier,
                uniqueTypes,
                override == null ? null : clampConstellationTier(override),
                constellationSpeedMultiplierForTier(tier),
                constellationFragmentChanceForTier(tier)
        );
    }

    public boolean setConstellationTierOverride(UUID islandId, int tier) {
        if (islandId == null) {
            return false;
        }
        int clampedTier = clampConstellationTier(tier);
        Integer previous = constellationTierOverrideByIsland.put(islandId, clampedTier);
        if (!Objects.equals(previous, clampedTier)) {
            markDirty();
            return true;
        }
        return false;
    }

    public boolean clearConstellationTierOverride(UUID islandId) {
        if (islandId == null) {
            return false;
        }
        Integer removed = constellationTierOverrideByIsland.remove(islandId);
        if (removed != null) {
            markDirty();
            return true;
        }
        return false;
    }

    public String constellationTierName(int tier) {
        return constellationTierNameInternal(clampConstellationTier(tier));
    }

    public Map<String, Integer> getStoredItems(MinionInstance minion) {
        if (minion == null) {
            return Map.of();
        }
        return minion.storedItems();
    }

    public FuelInfo getFuelInfo(MinionInstance minion) {
        if (minion == null || minion.fuelItemId() == null) {
            return null;
        }
        FuelDefinition definition = fuelDefinition(minion.fuelItemId());
        if (definition == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        long expiresAt = minion.fuelExpiresAtMs();
        boolean active = definition.permanent() || expiresAt > now;
        long remainingMs = definition.permanent() ? Long.MAX_VALUE : Math.max(0L, expiresAt - now);
        return new FuelInfo(definition.id(), definition.displayName(), definition.speedMultiplier(), definition.dropMultiplier(), definition.permanent(), active, remainingMs);
    }

    public UpgradeInfo getUpgradeInfo(String upgradeId) {
        UpgradeDefinition definition = upgradeDefinition(upgradeId);
        if (definition == null) {
            return null;
        }
        return new UpgradeInfo(definition.id(), definition.displayName(), definition.description(), definition.kind().name());
    }

    public boolean isShippingUpgrade(String upgradeId) {
        UpgradeDefinition definition = upgradeDefinition(upgradeId);
        return definition != null && definition.kind() == UpgradeKind.SHIPPING;
    }

    public ItemStack createStoredDisplayItem(String itemId, int amount) {
        ItemStack base = stackFromStoredId(itemId);
        if (base == null || base.getType().isAir()) {
            base = new ItemStack(Material.BARRIER);
        }
        ItemMeta meta = base.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(Objects.requireNonNullElse(meta.getLore(), List.of())) : new ArrayList<>();
            if (!lore.isEmpty()) {
                lore.add("");
            }
            lore.add(ChatColor.GRAY + "Stored: " + ChatColor.YELLOW + formatInt(Math.max(0, amount)));
            lore.add(ChatColor.DARK_GRAY + "ID: " + ChatColor.GRAY + normalizeIngredientId(itemId));
            meta.setLore(lore);
            base.setItemMeta(meta);
        }
        base.setAmount(Math.max(1, Math.min(base.getMaxStackSize(), Math.max(1, amount))));
        return base;
    }

    public ItemStack createRecipeDisplayItem(String itemId, int amount) {
        ItemStack base = stackFromStoredId(itemId);
        if (base == null || base.getType().isAir()) {
            return null;
        }
        base = base.clone();
        base.setAmount(Math.max(1, amount));
        return base;
    }

    public ItemStack createRecipeRequirementDisplayItem(String requirement, int amount) {
        if (requirement == null || requirement.isBlank()) {
            return null;
        }

        String normalized = requirement.trim().toLowerCase(Locale.ROOT);
        String[] parts = normalized.split(":", 2);
        String kind = parts.length >= 2 ? parts[0] : "ing";
        String id = parts.length >= 2 ? parts[1] : normalized;
        if (id == null || id.isBlank()) {
            return null;
        }

        return switch (kind) {
            case "ing" -> createRecipeDisplayItem(id, amount);
            case "upg" -> createUpgradeItem(id, amount);
            case "fuel" -> createFuelItem(id, amount);
            case "mat" -> {
                Material material = Material.matchMaterial(id.toUpperCase(Locale.ROOT));
                yield material == null || material.isAir() ? null : new ItemStack(material, Math.max(1, amount));
            }
            default -> createRecipeDisplayItem(id, amount);
        };
    }

    public boolean addFuelToMinion(Player player, UUID minionId, ItemStack stack) {
        if (player == null || minionId == null || stack == null || stack.getType().isAir()) {
            return false;
        }
        MinionInstance minion = minionsById.get(minionId);
        if (minion == null || !canManageMinion(player, minion)) {
            return false;
        }

        String fuelId = readFuelId(stack);
        FuelDefinition newFuel = fuelDefinition(fuelId);
        if (newFuel == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        FuelDefinition current = fuelDefinition(minion.fuelItemId());
        if (current != null && current.permanent() && !newFuel.permanent()) {
            player.sendMessage(ChatColor.RED + "Remove permanent fuel first.");
            return false;
        }

        if (Objects.equals(minion.fuelItemId(), newFuel.id()) && !newFuel.permanent()) {
            long base = Math.max(now, minion.fuelExpiresAtMs());
            minion.setFuelExpiresAtMs(base + newFuel.durationMs());
        } else {
            minion.setFuelItemId(newFuel.id());
            minion.setFuelExpiresAtMs(newFuel.permanent() ? Long.MAX_VALUE : now + newFuel.durationMs());
        }
        decrementOne(stack);
        markDirty();
        updateDisplay(minion);
        return true;
    }

    public ItemStack removeFuelFromMinion(Player player, UUID minionId) {
        if (player == null || minionId == null) {
            return null;
        }
        MinionInstance minion = minionsById.get(minionId);
        if (minion == null || !canManageMinion(player, minion)) {
            return null;
        }
        FuelDefinition fuel = fuelDefinition(minion.fuelItemId());
        if (fuel == null) {
            return null;
        }
        if (!fuel.permanent()) {
            // Temporary fuels are consumed once inserted.
            minion.setFuelItemId(null);
            minion.setFuelExpiresAtMs(0L);
            markDirty();
            updateDisplay(minion);
            return null;
        }

        minion.setFuelItemId(null);
        minion.setFuelExpiresAtMs(0L);
        markDirty();
        updateDisplay(minion);
        return createFuelItem(fuel.id(), 1);
    }

    public boolean setUpgradeSlot(Player player, UUID minionId, int slot, ItemStack stack) {
        if (player == null || minionId == null || slot < 1 || slot > 2 || stack == null || stack.getType().isAir()) {
            return false;
        }
        MinionInstance minion = minionsById.get(minionId);
        if (minion == null || !canManageMinion(player, minion)) {
            return false;
        }

        String upgradeId = readUpgradeId(stack);
        UpgradeDefinition definition = upgradeDefinition(upgradeId);
        if (definition == null) {
            return false;
        }
        String otherId = slot == 1 ? minion.upgradeSlotTwoId() : minion.upgradeSlotOneId();
        if (otherId != null) {
            UpgradeDefinition other = upgradeDefinition(otherId);
            if (other != null && other.kind() == UpgradeKind.SHIPPING && definition.kind() == UpgradeKind.SHIPPING) {
                player.sendMessage(ChatColor.RED + "Only one hopper can be installed.");
                return false;
            }
        }

        if (slot == 1) {
            minion.setUpgradeSlotOneId(definition.id());
        } else {
            minion.setUpgradeSlotTwoId(definition.id());
        }
        decrementOne(stack);
        markDirty();
        updateDisplay(minion);
        return true;
    }

    public ItemStack removeUpgradeSlot(Player player, UUID minionId, int slot) {
        if (player == null || minionId == null || slot < 1 || slot > 2) {
            return null;
        }
        MinionInstance minion = minionsById.get(minionId);
        if (minion == null || !canManageMinion(player, minion)) {
            return null;
        }
        String upgradeId = slot == 1 ? minion.upgradeSlotOneId() : minion.upgradeSlotTwoId();
        if (upgradeId == null || upgradeId.isBlank()) {
            return null;
        }

        if (slot == 1) {
            minion.setUpgradeSlotOneId(null);
        } else {
            minion.setUpgradeSlotTwoId(null);
        }
        markDirty();
        updateDisplay(minion);
        return createUpgradeItem(upgradeId, 1);
    }

    public long collectHopperCoins(Player player, UUID minionId) {
        if (player == null || minionId == null) {
            return 0L;
        }
        MinionInstance minion = minionsById.get(minionId);
        if (minion == null || !canManageMinion(player, minion)) {
            return 0L;
        }
        long rounded = Math.max(0L, Math.round(minion.hopperCoins()));
        if (rounded <= 0L) {
            return 0L;
        }
        if (!depositToMinionProfile(minion, rounded)) {
            return 0L;
        }
        minion.setHopperCoins(Math.max(0.0D, minion.hopperCoins() - rounded));
        markDirty();
        return rounded;
    }

    public int getMinionCountForIsland(UUID islandId) {
        if (islandId == null) {
            return 0;
        }
        Set<UUID> ids = minionIdsByIsland.get(islandId);
        return ids == null ? 0 : ids.size();
    }

    public int getIslandMinionLimit(Island island) {
        if (island == null) {
            return islandBaseLimit;
        }
        if (islandBaseLimit < 0) {
            return -1;
        }
        int level = Math.max(1, island.getUpgradeLevel());
        int bonus = island.getMinionLimitUpgrade();
        return islandBaseLimit + Math.max(0, level - 1) * limitPerIslandLevel + bonus;
    }

    public int getStorageCap(MinionType type, int tier) {
        if (type == null) {
            return 64;
        }
        int raw = type.storageForTier(tier);
        return Math.max(1, (int) Math.round(raw * storageMultiplier));
    }

    public int getStorageCap(int tier) {
        // Legacy fallback path used by older callers.
        return getStorageCap(MinionType.COBBLESTONE, tier);
    }

    public int getUpgradeCostForCurrentTier(MinionType type, int currentTier) {
        if (type == null) {
            return 0;
        }
        MinionType.TierRecipe recipe = type.recipeForTier(currentTier + 1);
        if (recipe == null) {
            return 0;
        }
        return recipe.amountPerOuterSlot() * OUTER_SLOTS.length;
    }

    public String getUpgradeIngredientForCurrentTier(MinionType type, int currentTier) {
        if (type == null) {
            return null;
        }
        MinionType.TierRecipe recipe = type.recipeForTier(currentTier + 1);
        return recipe == null ? null : recipe.ingredientId();
    }

    public int getMaxTier(MinionType type) {
        if (type == null) {
            return 1;
        }
        return Math.max(1, type.maxTier());
    }

    public int getMaxCraftableTier(MinionType type) {
        if (type == null) {
            return 1;
        }
        return Math.max(1, type.maxCraftableTier());
    }

    public double getSecondsPerAction(MinionType type, int tier) {
        if (type == null) {
            return 1.0D;
        }
        return Math.max(0.1D, type.secondsPerAction(tier) * speedMultiplier);
    }

    public void shutdown() {
        if (processingTaskId != -1) {
            Bukkit.getScheduler().cancelTask(processingTaskId);
            processingTaskId = -1;
        }
        if (autoSaveTaskId != -1) {
            Bukkit.getScheduler().cancelTask(autoSaveTaskId);
            autoSaveTaskId = -1;
        }
        if (displayAuditTaskId != -1) {
            Bukkit.getScheduler().cancelTask(displayAuditTaskId);
            displayAuditTaskId = -1;
        }
        saveData();
    }

    private void startTasks() {
        if (processingTaskId != -1) {
            Bukkit.getScheduler().cancelTask(processingTaskId);
        }
        if (autoSaveTaskId != -1) {
            Bukkit.getScheduler().cancelTask(autoSaveTaskId);
        }
        if (displayAuditTaskId != -1) {
            Bukkit.getScheduler().cancelTask(displayAuditTaskId);
        }

        processingTaskId = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::processMinions,
                tickIntervalTicks,
                tickIntervalTicks
        ).getTaskId();

        autoSaveTaskId = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::saveIfDirty,
                autoSaveIntervalTicks,
                autoSaveIntervalTicks
        ).getTaskId();

        long displayAuditPeriod = Math.max(100L, tickIntervalTicks * 10L);
        displayAuditTaskId = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::auditDisplays,
                displayAuditPeriod,
                displayAuditPeriod
        ).getTaskId();
    }

    private void restartTasks() {
        if (processingTaskId == -1 && autoSaveTaskId == -1) {
            return;
        }
        startTasks();
    }

    private void processMinions() {
        if (!enabled || minionsById.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        List<MinionInstance> snapshot = new ArrayList<>(minionsById.values());
        for (MinionInstance minion : snapshot) {
            if (minion == null) {
                continue;
            }
            Island island = islandManager.getIslandById(minion.islandId());
            if (island == null) {
                removeMinion(minion, true);
                continue;
            }
            processSingleMinion(minion, island, now);
        }
    }

    private void auditDisplays() {
        if (!enabled || minionsById.isEmpty()) {
            return;
        }
        if (plugin.getServerPerformanceMonitor() != null
                && !plugin.getServerPerformanceMonitor().shouldProcess("minions-display-audit", 2, 4)) {
            return;
        }
        for (MinionInstance minion : minionsById.values()) {
            ensureDisplayAttached(minion);
        }
    }

    private void processSingleMinion(MinionInstance minion, Island island, long now) {
        long last = minion.lastActionAtMs();
        if (last <= 0L) {
            minion.setLastActionAtMs(now);
            markDirty();
            return;
        }

        cleanupExpiredFuel(minion, now);
        if (isHardStorageCapped(minion)) {
            minion.setActionProgress(0);
            minion.setLastActionAtMs(now);
            markDirty();
            return;
        }

        long elapsed = Math.max(0L, now - last);
        long catchupCapMs = maxCatchupHours * 60L * 60L * 1000L;
        long processUntil = elapsed > catchupCapMs ? last + catchupCapMs : now;
        if (processUntil <= last) {
            return;
        }

        int actionProgress = Math.max(0, minion.actionProgress());
        long cursor = last;
        long actionsProcessed = 0L;
        boolean storageBlocked = false;
        UUID islandId = island == null ? null : island.getId();
        double constellationSpeedMultiplier = constellationSpeedMultiplierForIsland(islandId);
        double constellationFragmentChance = constellationFragmentChanceForIsland(islandId);
        int effectiveMaxActionsPerTick = effectiveMaxActionsPerTick();

        while (cursor < processUntil && actionsProcessed < effectiveMaxActionsPerTick) {
            FuelDefinition fuel = activeFuelAt(minion, cursor);
            double speedMultiplier = fuel == null ? 1.0D : Math.max(0.05D, fuel.speedMultiplier());
            speedMultiplier *= upgradeSpeedMultiplier(minion);
            speedMultiplier *= constellationSpeedMultiplier;
            
            // Global Booster Integration
            if (plugin.getGlobalEventManager() != null) {
                speedMultiplier *= plugin.getGlobalEventManager().getMultiplier(GlobalEventManager.BoosterType.MINION_SPEED);
            }

            double secondsPerAction = getSecondsPerAction(minion.type(), minion.tier()) / speedMultiplier;
            long intervalMs = Math.max(100L, (long) Math.round(secondsPerAction * 1000L));
            long nextActionAt = cursor + intervalMs;
            if (nextActionAt > processUntil) {
                break;
            }

            cursor = nextActionAt;
            actionsProcessed++;
            actionProgress++;

            if (actionProgress >= ACTIONS_PER_ITEM) {
                actionProgress -= ACTIONS_PER_ITEM;
                int outputMultiplier = fuel == null ? 1 : Math.max(1, (int) Math.round(fuel.dropMultiplier()));
                if (!generateAndStore(minion, outputMultiplier, constellationFragmentChance)) {
                    storageBlocked = true;
                    break;
                }
            }
        }

        if (storageBlocked) {
            minion.setActionProgress(0);
            minion.setLastActionAtMs(now);
        } else {
            minion.setActionProgress(actionProgress);
            minion.setLastActionAtMs(cursor);
        }
        if (actionsProcessed > 0 || storageBlocked) {
            updateDisplay(minion);
            markDirty();
        }
    }

    private int effectiveMaxActionsPerTick() {
        if (plugin.getServerPerformanceMonitor() == null) {
            return maxActionsPerTick;
        }
        return plugin.getServerPerformanceMonitor().scaleBudget(maxActionsPerTick, 70, 45, 60);
    }

    private boolean generateAndStore(MinionInstance minion, int outputMultiplier, double constellationFragmentChance) {
        if (minion == null) {
            return false;
        }

        String baseId = baseOutputIngredientId(minion.type());
        if (baseId == null) {
            return false;
        }
        int baseAmount = Math.max(1, outputMultiplier);

        Map<String, Integer> drops = new LinkedHashMap<>();
        addDrop(drops, baseId, baseAmount);

        Set<String> upgrades = equippedUpgrades(minion);
        double totalConstellationFragmentChance = Math.max(0.0D, constellationFragmentChance);
        if (upgrades.contains("grivience_astral_resonator") && totalConstellationFragmentChance > 0.0D) {
            totalConstellationFragmentChance = Math.min(1.0D, totalConstellationFragmentChance + ASTRAL_RESONATOR_FRAGMENT_CHANCE_BONUS);
        }
        if (upgrades.contains("diamond_spreading")) {
            int extra = rollChance(baseAmount, 0.10D);
            if (extra > 0) {
                addDrop(drops, "diamond", extra);
            }
        }
        if (upgrades.contains("corrupt_soil")) {
            addDrop(drops, "sulphur", baseAmount);
            int extraFragments = rollChance(baseAmount, 0.20D);
            if (extraFragments > 0) {
                addDrop(drops, "corrupted_fragment", extraFragments);
            }
        }
        if (totalConstellationFragmentChance > 0.0D) {
            int extra = rollChance(baseAmount, totalConstellationFragmentChance);
            if (extra > 0) {
                addDrop(drops, "constellation_fragment", extra);
            }
        }

        if (upgrades.contains("auto_smelter")) {
            drops = applyAutoSmelter(drops);
        }

        if (!storeDrops(minion, drops)) {
            return false;
        }

        if (upgrades.contains("super_compactor_3000")) {
            compactStoredItems(minion, true);
        } else if (upgrades.contains("compactor")) {
            compactStoredItems(minion, false);
        }
        return true;
    }

    private int uniqueMinionTypeCount(UUID islandId) {
        if (islandId == null) {
            return 0;
        }
        Set<UUID> ids = minionIdsByIsland.get(islandId);
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        Set<MinionType> uniqueTypes = new HashSet<>();
        for (UUID minionId : ids) {
            MinionInstance minion = minionsById.get(minionId);
            if (minion != null) {
                uniqueTypes.add(minion.type());
            }
        }
        return uniqueTypes.size();
    }

    private int constellationTierForIsland(UUID islandId) {
        if (islandId == null) {
            return 0;
        }
        Integer override = constellationTierOverrideByIsland.get(islandId);
        if (override != null) {
            return clampConstellationTier(override);
        }
        return constellationTierForUniqueTypes(uniqueMinionTypeCount(islandId));
    }

    private double constellationSpeedMultiplierForIsland(UUID islandId) {
        return constellationSpeedMultiplierForTier(constellationTierForIsland(islandId));
    }

    private double constellationFragmentChanceForIsland(UUID islandId) {
        return constellationFragmentChanceForTier(constellationTierForIsland(islandId));
    }

    private static int clampConstellationTier(int tier) {
        return Math.max(0, Math.min(tier, CONSTELLATION_TYPE_REQUIREMENTS.length - 1));
    }

    private static int constellationTierForUniqueTypes(int uniqueTypes) {
        int safeUniqueTypes = Math.max(0, uniqueTypes);
        for (int tier = CONSTELLATION_TYPE_REQUIREMENTS.length - 1; tier >= 0; tier--) {
            if (safeUniqueTypes >= CONSTELLATION_TYPE_REQUIREMENTS[tier]) {
                return tier;
            }
        }
        return 0;
    }

    private static double constellationSpeedMultiplierForTier(int tier) {
        return CONSTELLATION_SPEED_MULTIPLIERS[clampConstellationTier(tier)];
    }

    private static double constellationFragmentChanceForTier(int tier) {
        return CONSTELLATION_FRAGMENT_CHANCE[clampConstellationTier(tier)];
    }

    private static String constellationTierNameInternal(int tier) {
        return switch (clampConstellationTier(tier)) {
            case 1 -> "Resonant";
            case 2 -> "Harmonic";
            case 3 -> "Astral";
            default -> "Dormant";
        };
    }

    private boolean storeDrops(MinionInstance minion, Map<String, Integer> drops) {
        if (minion == null || drops == null || drops.isEmpty()) {
            return true;
        }
        Map<String, Integer> stored = minion.storedItems();
        int capacity = getStorageCap(minion.type(), minion.tier());
        double soldCoins = 0.0D;
        UpgradeDefinition shipping = shippingUpgrade(minion);

        for (Map.Entry<String, Integer> entry : drops.entrySet()) {
            if (entry == null || entry.getKey() == null) {
                continue;
            }
            int amount = Math.max(0, entry.getValue() == null ? 0 : entry.getValue());
            if (amount <= 0) {
                continue;
            }

            int free = Math.max(0, capacity - totalStored(stored));
            int toStore = Math.min(amount, free);
            int overflow = amount - toStore;
            if (toStore > 0) {
                stored.merge(entry.getKey(), toStore, Integer::sum);
            }

            if (overflow <= 0) {
                continue;
            }

            if (shipping == null) {
                minion.setStoredItems(stored);
                return false;
            }
            double itemPrice = npcSellPrice(entry.getKey());
            soldCoins += overflow * itemPrice * shipping.sellMultiplier();
        }

        minion.setStoredItems(stored);
        if (soldCoins > 0.0D) {
            minion.addHopperCoins(soldCoins);
        }
        return true;
    }

    private void compactStoredItems(MinionInstance minion, boolean superCompactor) {
        if (minion == null) {
            return;
        }
        Map<String, Integer> stored = minion.storedItems();
        if (stored.isEmpty()) {
            return;
        }
        boolean changed;
        do {
            changed = applyCompactorPass(stored);
            if (superCompactor) {
                changed = applySuperCompactorPass(stored) || changed;
            }
        } while (changed);
        minion.setStoredItems(stored);
    }

    private boolean applyCompactorPass(Map<String, Integer> stored) {
        if (stored == null || stored.isEmpty()) {
            return false;
        }
        boolean changed = false;
        for (CompactRule rule : COMPACTOR_RULES.values()) {
            if (rule == null) {
                continue;
            }
            int amount = Math.max(0, stored.getOrDefault(rule.inputId(), 0));
            if (amount < rule.inputAmount()) {
                continue;
            }
            int crafts = amount / rule.inputAmount();
            if (crafts <= 0) {
                continue;
            }
            int consumed = crafts * rule.inputAmount();
            decrementStored(stored, rule.inputId(), consumed);
            stored.merge(rule.outputId(), crafts * rule.outputAmount(), Integer::sum);
            changed = true;
        }
        return changed;
    }

    private boolean applySuperCompactorPass(Map<String, Integer> stored) {
        if (stored == null || stored.isEmpty()) {
            return false;
        }
        boolean changed = false;
        for (SuperCompactorRule rule : SUPER_COMPACTOR_RULES) {
            if (rule == null) {
                continue;
            }
            int amount = Math.max(0, stored.getOrDefault(rule.inputId(), 0));
            if (amount < rule.inputAmount()) {
                continue;
            }
            int crafts = amount / rule.inputAmount();
            if (crafts <= 0) {
                continue;
            }
            int consumed = crafts * rule.inputAmount();
            decrementStored(stored, rule.inputId(), consumed);
            stored.merge(rule.outputId(), crafts * rule.outputAmount(), Integer::sum);
            changed = true;
        }
        return changed;
    }

    private int collectMinion(Player player, MinionInstance minion, boolean notify) {
        if (player == null || minion == null) {
            return 0;
        }
        if (!canManageMinion(player, minion)) {
            if (notify) {
                player.sendMessage(ChatColor.RED + "You cannot collect from that minion.");
            }
            return 0;
        }

        Map<String, Integer> stored = minion.storedItems();
        int total = stored.values().stream().mapToInt(value -> Math.max(0, value == null ? 0 : value)).sum();
        if (total <= 0) {
            if (notify) {
                player.sendMessage(ChatColor.YELLOW + "That minion has nothing to collect.");
            }
            return 0;
        }

        int inserted = 0;
        for (Map.Entry<String, Integer> entry : stored.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            int amount = Math.max(0, entry.getValue() == null ? 0 : entry.getValue());
            if (amount <= 0) {
                continue;
            }
            inserted += giveStoredItem(player, entry.getKey(), amount);
        }

        minion.clearStoredItems();
        updateDisplay(minion);
        markDirty();

        if (notify) {
            player.sendMessage(ChatColor.GREEN + "Collected " + ChatColor.AQUA + formatInt(inserted)
                    + ChatColor.GREEN + " from " + ChatColor.YELLOW + minion.type().displayName() + " Minion.");
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8F, 1.2F);
        }
        return inserted;
    }

    private boolean pickupMinion(Player player, MinionInstance minion, boolean notify) {
        if (player == null || minion == null) {
            return false;
        }
        if (!canManageMinion(player, minion)) {
            if (notify) {
                player.sendMessage(ChatColor.RED + "You cannot pick up that minion.");
            }
            return false;
        }

        collectMinion(player, minion, false);
        ItemStack item = createMinionItem(minion.type(), minion.tier());
        if (item != null) {
            var leftover = player.getInventory().addItem(item);
            if (!leftover.isEmpty()) {
                for (ItemStack stack : leftover.values()) {
                    if (stack == null) {
                        continue;
                    }
                    player.getWorld().dropItemNaturally(player.getLocation(), stack);
                }
            }
        }

        if (minion.fuelItemId() != null) {
            ItemStack fuel = createFuelItem(minion.fuelItemId(), 1);
            if (fuel != null) {
                var leftover = player.getInventory().addItem(fuel);
                if (!leftover.isEmpty()) {
                    for (ItemStack stack : leftover.values()) {
                        if (stack != null && !stack.getType().isAir()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), stack);
                        }
                    }
                }
            }
        }

        if (minion.upgradeSlotOneId() != null) {
            ItemStack upg = createUpgradeItem(minion.upgradeSlotOneId(), 1);
            if (upg != null) {
                var leftover = player.getInventory().addItem(upg);
                if (!leftover.isEmpty()) {
                    for (ItemStack stack : leftover.values()) {
                        if (stack != null && !stack.getType().isAir()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), stack);
                        }
                    }
                }
            }
        }
        if (minion.upgradeSlotTwoId() != null) {
            ItemStack upg = createUpgradeItem(minion.upgradeSlotTwoId(), 1);
            if (upg != null) {
                var leftover = player.getInventory().addItem(upg);
                if (!leftover.isEmpty()) {
                    for (ItemStack stack : leftover.values()) {
                        if (stack != null && !stack.getType().isAir()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), stack);
                        }
                    }
                }
            }
        }

        long hopperCoins = Math.max(0L, Math.round(minion.hopperCoins()));
        if (hopperCoins > 0L) {
            depositToMinionProfile(minion, hopperCoins);
        }

        removeMinion(minion, true);
        if (notify) {
            player.sendMessage(ChatColor.GREEN + "Picked up " + ChatColor.AQUA + minion.type().displayName()
                    + ChatColor.GREEN + " Minion " + ChatColor.YELLOW + roman(minion.tier()) + ChatColor.GREEN + ".");
            player.playSound(player.getLocation(), Sound.BLOCK_WOOD_BREAK, 0.8F, 1.0F);
        }
        return true;
    }

    private void addMinion(MinionInstance minion) {
        minionsById.put(minion.id(), minion);
        minionIdsByIsland.computeIfAbsent(minion.islandId(), ignored -> ConcurrentHashMap.newKeySet()).add(minion.id());
        if (minion.displayEntityId() != null) {
            minionIdByDisplayEntity.put(minion.displayEntityId(), minion.id());
        }
        markDirty();
    }

    private void removeMinion(MinionInstance minion, boolean removeDisplayEntity) {
        minionsById.remove(minion.id());
        Set<UUID> set = minionIdsByIsland.get(minion.islandId());
        if (set != null) {
            set.remove(minion.id());
            if (set.isEmpty()) {
                minionIdsByIsland.remove(minion.islandId());
            }
        }

        if (minion.displayEntityId() != null) {
            UUID displayId = minion.displayEntityId();
            minionIdByDisplayEntity.remove(displayId);
            if (removeDisplayEntity) {
                Entity entity = Bukkit.getEntity(displayId);
                if (entity != null && entity.isValid()) {
                    entity.remove();
                }
            }
            minion.setDisplayEntityId(null);
        }
        if (removeDisplayEntity) {
            removeNearbyDisplayEntities(minion, null);
        }
        markDirty();
    }

    private List<ArmorStand> findNearbyDisplayEntities(MinionInstance minion) {
        if (minion == null) {
            return List.of();
        }
        Location location = minion.location();
        if (location == null || location.getWorld() == null) {
            return List.of();
        }
        String minionIdRaw = minion.id().toString();
        List<ArmorStand> matches = new ArrayList<>();
        for (Entity nearby : location.getWorld().getNearbyEntities(
                location,
                DISPLAY_SEARCH_RADIUS_XZ,
                DISPLAY_SEARCH_RADIUS_Y,
                DISPLAY_SEARCH_RADIUS_XZ
        )) {
            if (!(nearby instanceof ArmorStand armorStand) || !nearby.isValid()) {
                continue;
            }
            String rawId = armorStand.getPersistentDataContainer().get(minionEntityIdKey, PersistentDataType.STRING);
            if (rawId == null || rawId.isBlank()) {
                continue;
            }
            if (rawId.equalsIgnoreCase(minionIdRaw)) {
                matches.add(armorStand);
            }
        }
        return matches;
    }

    private void removeNearbyDisplayEntities(MinionInstance minion, UUID keepEntityId) {
        for (ArmorStand stand : findNearbyDisplayEntities(minion)) {
            UUID standId = stand.getUniqueId();
            if (keepEntityId != null && keepEntityId.equals(standId)) {
                continue;
            }
            minionIdByDisplayEntity.remove(standId);
            if (stand.isValid()) {
                stand.remove();
            }
        }
    }

    private void spawnAllDisplays() {
        for (MinionInstance minion : minionsById.values()) {
            spawnOrAttachDisplay(minion);
        }
    }

    private void ensureDisplayAttached(MinionInstance minion) {
        DisplayBinding binding = bindDisplay(minion);
        if (binding == null || binding.stand() == null || !binding.changed()) {
            return;
        }
        applyDisplayAppearance(binding.stand(), minion);
        updateDisplayName(binding.stand(), minion);
    }

    private void spawnOrAttachDisplay(MinionInstance minion) {
        DisplayBinding binding = bindDisplay(minion);
        if (binding == null || binding.stand() == null) {
            return;
        }
        applyDisplayAppearance(binding.stand(), minion);
        updateDisplayName(binding.stand(), minion);
    }

    private DisplayBinding bindDisplay(MinionInstance minion) {
        if (minion == null || minion.location() == null || minion.location().getWorld() == null) {
            return null;
        }
        boolean updatedDisplayId = false;
        boolean createdOrAttached = false;
        ArmorStand stand = null;
        UUID displayId = minion.displayEntityId();
        if (displayId != null) {
            Entity existing = Bukkit.getEntity(displayId);
            if (existing instanceof ArmorStand armorStand && existing.isValid()) {
                stand = armorStand;
            } else {
                minionIdByDisplayEntity.remove(displayId);
                minion.setDisplayEntityId(null);
                updatedDisplayId = true;
            }
        }

        if (stand == null) {
            List<ArmorStand> nearbyDisplays = findNearbyDisplayEntities(minion);
            if (!nearbyDisplays.isEmpty()) {
                stand = nearbyDisplays.get(0);
                createdOrAttached = true;
            }
        }

        if (stand == null) {
            Location spawnLocation = minion.location().clone();
            stand = spawnLocation.getWorld().spawn(spawnLocation, ArmorStand.class, created -> {
                created.setGravity(false);
                created.setInvulnerable(true);
                created.setPersistent(true);
                created.setSilent(true);
                created.setVisible(true);
                created.setMarker(false);
                created.setSmall(true);
                created.setArms(true);
                created.setBasePlate(false);
                created.setCollidable(false);
                created.setCanPickupItems(false);
                created.setCustomNameVisible(true);
                created.getPersistentDataContainer().set(
                        minionEntityIdKey,
                        PersistentDataType.STRING,
                        minion.id().toString()
                );
            });
            createdOrAttached = true;
        } else {
            stand.getPersistentDataContainer().set(minionEntityIdKey, PersistentDataType.STRING, minion.id().toString());
        }

        UUID previousDisplayId = minion.displayEntityId();
        UUID standId = stand.getUniqueId();
        if (!standId.equals(previousDisplayId)) {
            if (previousDisplayId != null) {
                minionIdByDisplayEntity.remove(previousDisplayId);
            }
            minion.setDisplayEntityId(standId);
            updatedDisplayId = true;
        }

        if (createdOrAttached || updatedDisplayId) {
            removeNearbyDisplayEntities(minion, standId);
        }
        minionIdByDisplayEntity.put(standId, minion.id());
        if (updatedDisplayId) {
            markDirty();
        }
        return new DisplayBinding(stand, createdOrAttached || updatedDisplayId);
    }

    private void applyDisplayAppearance(ArmorStand stand, MinionInstance minion) {
        if (stand == null || minion == null) {
            return;
        }
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setPersistent(true);
        stand.setSilent(true);
        stand.setVisible(true);
        stand.setMarker(false);
        stand.setSmall(true);
        stand.setArms(true);
        stand.setBasePlate(false);
        stand.setCollidable(false);
        stand.setCanPickupItems(false);
        stand.setCustomNameVisible(true);
        EntityEquipment equipment = stand.getEquipment();
        if (equipment != null) {
            equipment.setHelmet(new ItemStack(minion.type().iconMaterial()));
            equipment.setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
            equipment.setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
            equipment.setBoots(new ItemStack(Material.CHAINMAIL_BOOTS));
            equipment.setItemInMainHand(new ItemStack(minion.type().baseCraftTool()));
            equipment.setItemInOffHand(null);
        }
    }

    private void updateDisplayName(ArmorStand stand, MinionInstance minion) {
        if (stand == null || minion == null) {
            return;
        }
        FuelDefinition fuel = activeFuelAt(minion, System.currentTimeMillis());
        boolean hasFuel = fuel != null;
        boolean hasHopper = shippingUpgrade(minion) != null;
        int constellationTier = constellationTierForIsland(minion.islandId());
        stand.setCustomName(
                ChatColor.GREEN + minion.type().displayName() + " Minion "
                        + ChatColor.GRAY + "[" + roman(minion.tier()) + "] "
                        + ChatColor.YELLOW + formatInt(minion.storedAmount())
                        + ChatColor.DARK_GRAY + "/"
                        + ChatColor.YELLOW + formatInt(getStorageCap(minion.type(), minion.tier()))
                        + ChatColor.DARK_GRAY + " ["
                        + (hasFuel ? ChatColor.AQUA + "F" : ChatColor.DARK_GRAY + "F")
                        + ChatColor.DARK_GRAY + "|"
                        + (hasHopper ? ChatColor.GOLD + "H" : ChatColor.DARK_GRAY + "H")
                        + ChatColor.DARK_GRAY + "|"
                        + (constellationTier > 0 ? ChatColor.LIGHT_PURPLE + "C" + constellationTier : ChatColor.DARK_GRAY + "C0")
                        + ChatColor.DARK_GRAY + "]"
        );
    }

    private void updateDisplay(MinionInstance minion) {
        if (minion == null || minion.displayEntityId() == null) {
            return;
        }
        Entity entity = Bukkit.getEntity(minion.displayEntityId());
        if (!(entity instanceof ArmorStand stand) || !entity.isValid()) {
            spawnOrAttachDisplay(minion);
            return;
        }
        updateDisplayName(stand, minion);
    }

    public int countIngredient(Player player, String ingredientId) {
        if (player == null || ingredientId == null || ingredientId.isBlank()) {
            return 0;
        }
        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (matchesIngredient(stack, ingredientId, 1)) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    public void consumeIngredient(Player player, String ingredientId, int amount) {
        if (player == null || ingredientId == null || ingredientId.isBlank() || amount <= 0) {
            return;
        }
        int remaining = amount;
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!matchesIngredient(stack, ingredientId, 1)) {
                continue;
            }
            int take = Math.min(remaining, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            if (stack.getAmount() <= 0) {
                player.getInventory().setItem(slot, null);
            } else {
                player.getInventory().setItem(slot, stack);
            }
            remaining -= take;
            if (remaining <= 0) {
                break;
            }
        }
    }

    private int giveItems(Player player, Material material, int amount) {
        if (player == null || material == null || material.isAir() || amount <= 0) {
            return 0;
        }
        ItemStack prototype = new ItemStack(material, 1);
        return giveItems(player, prototype, amount);
    }

    private int giveStoredItem(Player player, String itemId, int amount) {
        if (player == null || itemId == null || itemId.isBlank() || amount <= 0) {
            return 0;
        }
        ItemStack prototype = stackFromStoredId(itemId);
        if (prototype == null || prototype.getType().isAir()) {
            return 0;
        }
        return giveItems(player, prototype, amount);
    }

    private int giveItems(Player player, ItemStack prototype, int amount) {
        if (player == null || prototype == null || prototype.getType().isAir() || amount <= 0) {
            return 0;
        }
        int maxStack = Math.max(1, prototype.getMaxStackSize());
        int remaining = amount;
        int delivered = 0;
        while (remaining > 0) {
            int stackAmount = Math.min(remaining, maxStack);
            ItemStack stack = prototype.clone();
            stack.setAmount(stackAmount);

            var leftover = player.getInventory().addItem(stack);
            if (leftover.isEmpty()) {
                delivered += stackAmount;
                remaining -= stackAmount;
                continue;
            }

            int unplaced = 0;
            for (ItemStack itemStack : leftover.values()) {
                if (itemStack != null && !itemStack.getType().isAir()) {
                    unplaced += itemStack.getAmount();
                    player.getWorld().dropItemNaturally(player.getLocation(), itemStack);
                }
            }
            int placed = Math.max(0, stackAmount - unplaced);
            delivered += placed + unplaced;
            remaining -= stackAmount;
        }
        return delivered;
    }

    public CraftingMatch matchCraftingRecipe(ItemStack[] matrix) {
        if (matrix == null || matrix.length < 9) {
            return null;
        }

        CraftingMatch minionMatch = matchMinionRecipe(matrix);
        if (minionMatch != null) {
            return minionMatch;
        }
        CraftingMatch utilityMatch = matchUtilityRecipe(matrix);
        if (utilityMatch != null) {
            return utilityMatch;
        }
        return matchIngredientRecipe(matrix);
    }

    public int maxCrafts(ItemStack[] matrix, CraftingMatch match) {
        if (matrix == null || matrix.length < 9 || match == null) {
            return 0;
        }
        int max = Integer.MAX_VALUE;
        for (int slot = 0; slot < 9; slot++) {
            int required = match.slotCosts()[slot];
            if (required <= 0) {
                continue;
            }
            ItemStack stack = matrix[slot];
            if (stack == null || stack.getType().isAir()) {
                return 0;
            }
            // Protect against overstacked items (like 32000 items in a slot) 
            // by capping to the legal max stack size.
            int legalAmount = Math.min(stack.getAmount(), stack.getMaxStackSize());
            max = Math.min(max, legalAmount / required);
        }
        return max == Integer.MAX_VALUE ? 0 : Math.max(0, max);
    }

    public boolean consumeForCraft(ItemStack[] matrix, CraftingMatch match, int crafts) {
        if (matrix == null || matrix.length < 9 || match == null || crafts <= 0) {
            return false;
        }
        if (maxCrafts(matrix, match) < crafts) {
            return false;
        }

        for (int slot = 0; slot < 9; slot++) {
            int required = match.slotCosts()[slot];
            if (required <= 0) {
                continue;
            }
            ItemStack stack = matrix[slot];
            if (stack == null || stack.getType().isAir()) {
                return false;
            }
            int consume = required * crafts;
            int left = stack.getAmount() - consume;
            if (left <= 0) {
                matrix[slot] = null;
            } else {
                stack.setAmount(left);
                matrix[slot] = stack;
            }
        }
        return true;
    }

    private CraftingMatch matchMinionRecipe(ItemStack[] matrix) {
        for (MinionType type : MinionType.values()) {
            for (int tier = 1; tier <= type.maxCraftableTier(); tier++) {
                MinionType.TierRecipe recipe = type.recipeForTier(tier);
                if (recipe == null) {
                    continue;
                }

                if (!matchesMinionCenter(matrix[4], type, tier)) {
                    continue;
                }

                boolean matches = true;
                for (int slot : OUTER_SLOTS) {
                    if (!matchesIngredient(matrix[slot], recipe.ingredientId(), recipe.amountPerOuterSlot())) {
                        matches = false;
                        break;
                    }
                }
                if (!matches) {
                    continue;
                }

                ItemStack result = createMinionItem(type, tier);
                if (result == null) {
                    continue;
                }
                int[] costs = new int[9];
                costs[4] = 1;
                for (int slot : OUTER_SLOTS) {
                    costs[slot] = recipe.amountPerOuterSlot();
                }
                return new CraftingMatch(result, costs);
            }
        }
        return null;
    }

    private CraftingMatch matchIngredientRecipe(ItemStack[] matrix) {
        for (IngredientRecipe recipe : INGREDIENT_RECIPES) {
            boolean matches = true;
            for (int slot = 0; slot < 9; slot++) {
                int requiredAmount = recipe.slotCosts()[slot];
                String requiredId = recipe.slotIngredientIds()[slot];
                ItemStack stack = matrix[slot];
                if (requiredAmount <= 0) {
                    if (stack != null && !stack.getType().isAir()) {
                        matches = false;
                        break;
                    }
                    continue;
                }
                if (!matchesIngredient(stack, requiredId, requiredAmount)) {
                    matches = false;
                    break;
                }
            }

            if (!matches) {
                continue;
            }

            ItemStack result = createIngredientItem(recipe.outputIngredientId(), recipe.outputAmount());
            if (result == null) {
                continue;
            }
            return new CraftingMatch(result, recipe.slotCosts().clone());
        }
        return null;
    }

    private CraftingMatch matchUtilityRecipe(ItemStack[] matrix) {
        for (UtilityRecipe recipe : UTILITY_RECIPES) {
            boolean matches = true;
            for (int slot = 0; slot < 9; slot++) {
                int requiredAmount = recipe.slotCosts()[slot];
                String requirement = recipe.requirements()[slot];
                ItemStack stack = matrix[slot];
                if (requiredAmount <= 0) {
                    if (stack != null && !stack.getType().isAir()) {
                        matches = false;
                        break;
                    }
                    continue;
                }
                if (!matchesRequirement(stack, requirement, requiredAmount)) {
                    matches = false;
                    break;
                }
            }
            if (!matches) {
                continue;
            }

            ItemStack result = switch (recipe.outputType()) {
                case UTILITY_FUEL -> createFuelItem(recipe.outputId(), recipe.outputAmount());
                case UTILITY_UPGRADE -> createUpgradeItem(recipe.outputId(), recipe.outputAmount());
                default -> null;
            };
            if (result == null) {
                continue;
            }
            return new CraftingMatch(result, recipe.slotCosts().clone());
        }
        return null;
    }

    private boolean matchesMinionCenter(ItemStack center, MinionType type, int targetTier) {
        if (center == null || center.getType().isAir() || center.getAmount() <= 0 || type == null || targetTier < 1) {
            return false;
        }
        if (targetTier == 1) {
            return center.getType() == type.baseCraftTool();
        }

        MinionItemData centerData = readMinionItem(center);
        return centerData != null
                && centerData.type() == type
                && centerData.tier() == targetTier - 1;
    }

    private boolean hasBypass(Player player) {
        return player != null && (
                player.hasPermission("grivience.admin")
                        || player.hasPermission("grivience.island.bypass")
                        || player.hasPermission("grivience.minion.bypass")
        );
    }

    private static IngredientDefinition ingredient(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return INGREDIENTS.get(normalizeIngredientId(id));
    }

    private static String normalizeIngredientId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "enchanted_hay_bale" -> "enchanted_hay_block";
            case "enchanted_nether_wart" -> "enchanted_nether_stalk";
            default -> normalized;
        };
    }

    private static String prettyIngredientName(String id) {
        if (id == null || id.isBlank()) {
            return "Unknown";
        }
        String normalized = normalizeIngredientId(id);
        return switch (normalized) {
            case "carrot_item" -> "Carrot";
            case "potato_item" -> "Potato";
            case "nether_stalk" -> "Nether Wart";
            case "enchanted_hay_block" -> "Enchanted Hay Bale";
            case "enchanted_nether_stalk" -> "Enchanted Nether Wart";
            case "sulphur" -> "Sulphur";
            case "corrupted_fragment" -> "Corrupted Fragment";
            case "constellation_fragment" -> "Constellation Fragment";
            default -> {
                String[] parts = normalized.split("_");
                StringBuilder builder = new StringBuilder();
                for (String part : parts) {
                    if (part.isBlank()) {
                        continue;
                    }
                    if (!builder.isEmpty()) {
                        builder.append(' ');
                    }
                    builder.append(Character.toUpperCase(part.charAt(0)));
                    if (part.length() > 1) {
                        builder.append(part.substring(1));
                    }
                }
                yield builder.isEmpty() ? "Unknown" : builder.toString();
            }
        };
    }

    private static Map<String, IngredientDefinition> buildIngredientDefinitions() {
        Map<String, IngredientDefinition> map = new LinkedHashMap<>();

        addIngredient(map, "cobblestone", "Cobblestone", Material.COBBLESTONE, false);
        addIngredient(map, "stone", "Stone", Material.STONE, false);
        addIngredient(map, "coal", "Coal", Material.COAL, false);
        addIngredient(map, "iron_ingot", "Iron Ingot", Material.IRON_INGOT, false);
        addIngredient(map, "gold_ingot", "Gold Ingot", Material.GOLD_INGOT, false);
        addIngredient(map, "redstone", "Redstone Dust", Material.REDSTONE, false);
        addIngredient(map, "lapis_lazuli", "Lapis Lazuli", Material.LAPIS_LAZULI, false);
        addIngredient(map, "diamond", "Diamond", Material.DIAMOND, false);
        addIngredient(map, "emerald", "Emerald", Material.EMERALD, false);
        addIngredient(map, "wheat", "Wheat", Material.WHEAT, false);
        addIngredient(map, "hay_block", "Hay Bale", Material.HAY_BLOCK, false);
        addIngredient(map, "carrot_item", "Carrot", Material.CARROT, false);
        addIngredient(map, "potato_item", "Potato", Material.POTATO, false);
        addIngredient(map, "baked_potato", "Baked Potato", Material.BAKED_POTATO, false);
        addIngredient(map, "sugar_cane", "Sugar Cane", Material.SUGAR_CANE, false);
        addIngredient(map, "nether_stalk", "Nether Wart", Material.NETHER_WART, false);
        addIngredient(map, "golden_carrot", "Golden Carrot", Material.GOLDEN_CARROT, false);
        addIngredient(map, "mycelium", "Mycelium", Material.MYCELIUM, false);

        addIngredient(map, "enchanted_cobblestone", "Enchanted Cobblestone", Material.COBBLESTONE, true);
        addIngredient(map, "enchanted_coal", "Enchanted Coal", Material.COAL, true);
        addIngredient(map, "enchanted_coal_block", "Enchanted Coal Block", Material.COAL_BLOCK, true);
        addIngredient(map, "enchanted_iron", "Enchanted Iron", Material.IRON_INGOT, true);
        addIngredient(map, "enchanted_iron_block", "Enchanted Iron Block", Material.IRON_BLOCK, true);
        addIngredient(map, "enchanted_gold", "Enchanted Gold", Material.GOLD_INGOT, true);
        addIngredient(map, "enchanted_gold_block", "Enchanted Gold Block", Material.GOLD_BLOCK, true);
        addIngredient(map, "enchanted_redstone", "Enchanted Redstone", Material.REDSTONE, true);
        addIngredient(map, "enchanted_redstone_block", "Enchanted Redstone Block", Material.REDSTONE_BLOCK, true);
        addIngredient(map, "enchanted_lapis_lazuli", "Enchanted Lapis Lazuli", Material.LAPIS_LAZULI, true);
        addIngredient(map, "enchanted_lapis_lazuli_block", "Enchanted Lapis Block", Material.LAPIS_BLOCK, true);
        addIngredient(map, "enchanted_diamond", "Enchanted Diamond", Material.DIAMOND, true);
        addIngredient(map, "enchanted_diamond_block", "Enchanted Diamond Block", Material.DIAMOND_BLOCK, true);
        addIngredient(map, "enchanted_emerald", "Enchanted Emerald", Material.EMERALD, true);
        addIngredient(map, "enchanted_emerald_block", "Enchanted Emerald Block", Material.EMERALD_BLOCK, true);
        addIngredient(map, "enchanted_hay_block", "Enchanted Hay Bale", Material.HAY_BLOCK, true);
        addIngredient(map, "enchanted_carrot", "Enchanted Carrot", Material.CARROT, true);
        addIngredient(map, "enchanted_golden_carrot", "Enchanted Golden Carrot", Material.GOLDEN_CARROT, true);
        addIngredient(map, "enchanted_potato", "Enchanted Potato", Material.POTATO, true);
        addIngredient(map, "enchanted_baked_potato", "Enchanted Baked Potato", Material.BAKED_POTATO, true);
        addIngredient(map, "enchanted_sugar", "Enchanted Sugar", Material.SUGAR_CANE, true);
        addIngredient(map, "enchanted_sugar_cane", "Enchanted Sugar Cane", Material.SUGAR_CANE, true);
        addIngredient(map, "enchanted_nether_stalk", "Enchanted Nether Wart", Material.NETHER_WART, true);
        addIngredient(map, "enchanted_mycelium", "Enchanted Mycelium", Material.MYCELIUM, true);
        addIngredient(map, "sapphire", "Sapphire", Material.BLUE_DYE, true);
        addIngredient(map, "enchanted_sapphire", "Enchanted Sapphire", Material.BLUE_DYE, true);
        addIngredient(map, "sulphur", "Sulphur", Material.GUNPOWDER, true);
        addIngredient(map, "corrupted_fragment", "Corrupted Fragment", Material.PRISMARINE_SHARD, true);
        addIngredient(map, "constellation_fragment", "Constellation Fragment", Material.AMETHYST_SHARD, true);
        addMissingEnchantedFarmIngredients(map);

        return map;
    }

    private static void addIngredient(
            Map<String, IngredientDefinition> map,
            String id,
            String displayName,
            Material material,
            boolean customOnly
    ) {
        map.put(id, new IngredientDefinition(id, displayName, material, customOnly));
    }

    private static void addIngredientIfAbsent(
            Map<String, IngredientDefinition> map,
            String id,
            String displayName,
            Material material,
            boolean customOnly
    ) {
        if (id == null || id.isBlank() || material == null || material.isAir() || map.containsKey(id)) {
            return;
        }
        addIngredient(map, id, displayName, material, customOnly);
    }

    private static void addMissingEnchantedFarmIngredients(Map<String, IngredientDefinition> map) {
        for (EnchantedFarmItemType type : EnchantedFarmItemType.values()) {
            if (type == null) {
                continue;
            }
            if (type.baseMaterial() != null) {
                String rawId = ingredientIdFromFarmMaterial(type.baseMaterial());
                addIngredientIfAbsent(map, rawId, prettyIngredientName(rawId), type.baseMaterial(), false);
            }
            String outputId = normalizeIngredientId(type.id());
            addIngredientIfAbsent(
                    map,
                    outputId,
                    ChatColor.stripColor(type.displayName()),
                    materialForFarmIngredient(type),
                    true
            );
        }
    }

    private static List<IngredientRecipe> buildIngredientRecipes() {
        List<IngredientRecipe> recipes = new ArrayList<>();

        for (Map.Entry<String, String> entry : EnchantedItemRecipeCatalog.canonicalInputs().entrySet()) {
            recipes.add(canonicalEnchantedRecipe(entry.getKey(), entry.getValue()));
        }

        return List.copyOf(recipes);
    }

    private static void validateEnchantedRecipeCoverage() {
        Set<String> catalogOutputs = new LinkedHashSet<>(EnchantedItemRecipeCatalog.canonicalInputs().keySet());
        Set<String> ingredientOutputs = new LinkedHashSet<>();
        for (IngredientRecipe recipe : INGREDIENT_RECIPES) {
            if (recipe == null) {
                continue;
            }
            String outputId = normalizeIngredientId(recipe.outputIngredientId());
            if (outputId == null || !outputId.startsWith("enchanted_")) {
                continue;
            }
            if (!ingredientOutputs.add(outputId)) {
                throw new IllegalStateException("Duplicate enchanted ingredient recipe detected for " + outputId);
            }
            if (!catalogOutputs.contains(outputId)) {
                throw new IllegalStateException("Enchanted ingredient recipe is missing from the canonical catalog: " + outputId);
            }
        }

        if (!ingredientOutputs.equals(catalogOutputs)) {
            Set<String> missing = new LinkedHashSet<>(catalogOutputs);
            missing.removeAll(ingredientOutputs);
            Set<String> unexpected = new LinkedHashSet<>(ingredientOutputs);
            unexpected.removeAll(catalogOutputs);
            throw new IllegalStateException("Enchanted ingredient recipe coverage mismatch. Missing=" + missing + ", unexpected=" + unexpected);
        }

        Set<String> utilityOutputs = new LinkedHashSet<>();
        for (UtilityRecipe recipe : UTILITY_RECIPES) {
            if (recipe == null) {
                continue;
            }
            String outputId = normalizeIngredientId(recipe.outputId());
            if (outputId == null || !outputId.startsWith("enchanted_")) {
                continue;
            }
            if (!utilityOutputs.add(outputId)) {
                throw new IllegalStateException("Duplicate enchanted utility recipe detected for " + outputId);
            }
            if (ingredientOutputs.contains(outputId)) {
                throw new IllegalStateException("Enchanted output is defined by both ingredient and utility recipes: " + outputId);
            }
        }
    }

    private static IngredientRecipe canonicalEnchantedRecipe(String outputId, String inputId) {
        int[] costs = EnchantedItemRecipePattern.slotCosts();
        String[] ingredientIds = new String[9];
        for (int slot : EnchantedItemRecipePattern.REQUIRED_SLOTS) {
            ingredientIds[slot] = inputId;
        }
        return new IngredientRecipe(outputId, 1, costs, ingredientIds);
    }

    private static String ingredientIdFromFarmMaterial(Material material) {
        if (material == null) {
            return null;
        }
        return switch (material) {
            case CARROT -> "carrot_item";
            case POTATO -> "potato_item";
            case NETHER_WART -> "nether_stalk";
            default -> material.getKey().getKey();
        };
    }

    private static Material materialForFarmIngredient(EnchantedFarmItemType type) {
        if (type == null) {
            return Material.PAPER;
        }
        if (type.baseMaterial() != null) {
            return type.baseMaterial();
        }
        return switch (type) {
            case ENCHANTED_HAY_BALE -> Material.HAY_BLOCK;
            case ENCHANTED_BAKED_POTATO -> Material.BAKED_POTATO;
            case ENCHANTED_SUGAR_CANE -> Material.SUGAR_CANE;
            case ENCHANTED_GLISTERING_MELON -> Material.GLISTERING_MELON_SLICE;
            case ENCHANTED_CACTUS_GREEN -> Material.GREEN_DYE;
            default -> Material.PAPER;
        };
    }

    private ItemStack createUtilityItem(String utilityType, String utilityId, int amount) {
        String normalizedType = utilityType == null ? "" : utilityType.trim().toLowerCase(Locale.ROOT);
        String normalizedId = normalizeUtilityId(utilityId);
        if (normalizedId == null || amount <= 0) {
            return null;
        }

        if (UTILITY_FUEL.equals(normalizedType)) {
            FuelDefinition definition = fuelDefinition(normalizedId);
            if (definition == null) {
                return null;
            }
            ItemStack stack = new ItemStack(definition.material(), Math.min(amount, definition.material().getMaxStackSize()));
            ItemMeta meta = stack.getItemMeta();
            if (meta == null) {
                return stack;
            }
            meta.setDisplayName(ChatColor.AQUA + definition.displayName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Minion Fuel");
            lore.add("");
            if (Math.abs(definition.speedMultiplier() - 1.0D) > 0.001D) {
                lore.add(ChatColor.GRAY + "Speed Bonus: " + ChatColor.GREEN + "+" + formatPercent(definition.speedMultiplier() - 1.0D));
            }
            if (definition.dropMultiplier() > 1.0D) {
                lore.add(ChatColor.GRAY + "Output Multiplier: " + ChatColor.GOLD + "x" + formatDecimal(definition.dropMultiplier()));
            }
            lore.add(ChatColor.GRAY + "Duration: " + ChatColor.YELLOW + (definition.permanent() ? "Infinite" : formatDuration(definition.durationMs())));
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "ID: " + ChatColor.GRAY + definition.id());
            lore.add("");
            lore.add(ChatColor.BLUE + "" + ChatColor.BOLD + "UNCOMMON " + ChatColor.BLUE + "MINION FUEL");
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.getPersistentDataContainer().set(minionUtilityTypeKey, PersistentDataType.STRING, UTILITY_FUEL);
            meta.getPersistentDataContainer().set(minionUtilityIdKey, PersistentDataType.STRING, definition.id());
            stack.setItemMeta(meta);
            return stack;
        }

        if (UTILITY_UPGRADE.equals(normalizedType)) {
            UpgradeDefinition definition = upgradeDefinition(normalizedId);
            if (definition == null) {
                return null;
            }
            ItemStack stack = new ItemStack(definition.material(), Math.min(amount, definition.material().getMaxStackSize()));
            ItemMeta meta = stack.getItemMeta();
            if (meta == null) {
                return stack;
            }
            meta.setDisplayName(ChatColor.GOLD + definition.displayName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Minion Upgrade");
            lore.add("");
            lore.add(ChatColor.GRAY + definition.description());
            if (definition.kind() == UpgradeKind.SHIPPING) {
                lore.add(ChatColor.GRAY + "Sells overflow at " + ChatColor.GREEN + formatPercent(definition.sellMultiplier()) + ChatColor.GRAY + " NPC value.");
            }
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "ID: " + ChatColor.GRAY + definition.id());
            lore.add("");
            lore.add(ChatColor.BLUE + "" + ChatColor.BOLD + "UNCOMMON " + ChatColor.BLUE + "MINION UPGRADE");
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.getPersistentDataContainer().set(minionUtilityTypeKey, PersistentDataType.STRING, UTILITY_UPGRADE);
            meta.getPersistentDataContainer().set(minionUtilityIdKey, PersistentDataType.STRING, definition.id());
            stack.setItemMeta(meta);
            return stack;
        }
        return null;
    }

    private String readUtilityType(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        String type = meta.getPersistentDataContainer().get(minionUtilityTypeKey, PersistentDataType.STRING);
        if (type == null || type.isBlank()) {
            return null;
        }
        return type.trim().toLowerCase(Locale.ROOT);
    }

    private String readUtilityId(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        String id = meta.getPersistentDataContainer().get(minionUtilityIdKey, PersistentDataType.STRING);
        return normalizeUtilityId(id);
    }

    private static String normalizeUtilityId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private static FuelDefinition fuelDefinition(String fuelId) {
        if (fuelId == null || fuelId.isBlank()) {
            return null;
        }
        return FUELS.get(normalizeUtilityId(fuelId));
    }

    private static UpgradeDefinition upgradeDefinition(String upgradeId) {
        if (upgradeId == null || upgradeId.isBlank()) {
            return null;
        }
        return UPGRADES.get(normalizeUtilityId(upgradeId));
    }

    private FuelDefinition activeFuelAt(MinionInstance minion, long timestamp) {
        if (minion == null || minion.fuelItemId() == null) {
            return null;
        }
        FuelDefinition definition = fuelDefinition(minion.fuelItemId());
        if (definition == null) {
            return null;
        }
        if (definition.permanent()) {
            return definition;
        }
        return minion.fuelExpiresAtMs() > timestamp ? definition : null;
    }

    private void cleanupExpiredFuel(MinionInstance minion, long now) {
        if (minion == null || minion.fuelItemId() == null) {
            return;
        }
        FuelDefinition definition = fuelDefinition(minion.fuelItemId());
        if (definition == null) {
            minion.setFuelItemId(null);
            minion.setFuelExpiresAtMs(0L);
            markDirty();
            return;
        }
        if (!definition.permanent() && minion.fuelExpiresAtMs() <= now) {
            minion.setFuelItemId(null);
            minion.setFuelExpiresAtMs(0L);
            markDirty();
        }
    }

    private Set<String> equippedUpgrades(MinionInstance minion) {
        Set<String> upgrades = new HashSet<>();
        if (minion == null) {
            return upgrades;
        }
        String first = normalizeUtilityId(minion.upgradeSlotOneId());
        String second = normalizeUtilityId(minion.upgradeSlotTwoId());
        if (first != null && upgradeDefinition(first) != null) {
            upgrades.add(first);
        }
        if (second != null && upgradeDefinition(second) != null) {
            upgrades.add(second);
        }
        return upgrades;
    }

    private double upgradeSpeedMultiplier(MinionInstance minion) {
        Set<String> upgrades = equippedUpgrades(minion);
        double speed = 1.0D;
        if (upgrades.contains("grivience_overclock_chip")) {
            speed *= OVERCLOCK_CHIP_SPEED_MULTIPLIER;
        }
        if (upgrades.contains("grivience_astral_resonator")) {
            speed *= ASTRAL_RESONATOR_SPEED_MULTIPLIER;
        }
        return speed;
    }

    private UpgradeDefinition shippingUpgrade(MinionInstance minion) {
        UpgradeDefinition first = upgradeDefinition(minion == null ? null : minion.upgradeSlotOneId());
        UpgradeDefinition second = upgradeDefinition(minion == null ? null : minion.upgradeSlotTwoId());
        UpgradeDefinition chosen = null;
        if (first != null && first.kind() == UpgradeKind.SHIPPING) {
            chosen = first;
        }
        if (second != null && second.kind() == UpgradeKind.SHIPPING) {
            if (chosen == null || second.sellMultiplier() > chosen.sellMultiplier()) {
                chosen = second;
            }
        }
        return chosen;
    }

    private boolean isHardStorageCapped(MinionInstance minion) {
        if (minion == null) {
            return false;
        }
        if (shippingUpgrade(minion) != null) {
            return false;
        }
        return minion.storedAmount() >= getStorageCap(minion.type(), minion.tier());
    }

    private static String baseOutputIngredientId(MinionType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case COBBLESTONE -> "cobblestone";
            case COAL -> "coal";
            case IRON -> "iron_ingot";
            case GOLD -> "gold_ingot";
            case REDSTONE -> "redstone";
            case LAPIS -> "lapis_lazuli";
            case DIAMOND -> "diamond";
            case EMERALD -> "emerald";
            case WHEAT -> "wheat";
            case CARROT -> "carrot_item";
            case POTATO -> "potato_item";
            case SUGAR_CANE -> "sugar_cane";
            case NETHER_WART -> "nether_stalk";
        };
    }

    private static int rollChance(int attempts, double chancePerAttempt) {
        int safeAttempts = Math.max(0, attempts);
        double safeChance = Math.max(0.0D, Math.min(1.0D, chancePerAttempt));
        int hits = 0;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < safeAttempts; i++) {
            if (random.nextDouble() <= safeChance) {
                hits++;
            }
        }
        return hits;
    }

    private static void addDrop(Map<String, Integer> drops, String itemId, int amount) {
        if (drops == null || itemId == null || itemId.isBlank() || amount <= 0) {
            return;
        }
        drops.merge(itemId, amount, Integer::sum);
    }

    private static Map<String, Integer> applyAutoSmelter(Map<String, Integer> drops) {
        if (drops == null || drops.isEmpty()) {
            return Map.of();
        }
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : drops.entrySet()) {
            if (entry == null || entry.getKey() == null) {
                continue;
            }
            int amount = Math.max(0, entry.getValue() == null ? 0 : entry.getValue());
            if (amount <= 0) {
                continue;
            }
            String converted = AUTO_SMELT_MAP.getOrDefault(entry.getKey(), entry.getKey());
            out.merge(converted, amount, Integer::sum);
        }
        return out;
    }

    private static int totalStored(Map<String, Integer> stored) {
        if (stored == null || stored.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (Integer value : stored.values()) {
            total += Math.max(0, value == null ? 0 : value);
        }
        return total;
    }

    private static void decrementStored(Map<String, Integer> stored, String itemId, int amount) {
        if (stored == null || itemId == null || itemId.isBlank() || amount <= 0) {
            return;
        }
        int current = Math.max(0, stored.getOrDefault(itemId, 0));
        int next = current - amount;
        if (next <= 0) {
            stored.remove(itemId);
        } else {
            stored.put(itemId, next);
        }
    }

    private void enforceStorageCap(MinionInstance minion) {
        if (minion == null) {
            return;
        }
        int cap = getStorageCap(minion.type(), minion.tier());
        Map<String, Integer> stored = minion.storedItems();
        int total = totalStored(stored);
        if (total <= cap) {
            return;
        }

        int overflow = total - cap;
        for (String key : new ArrayList<>(stored.keySet())) {
            if (overflow <= 0) {
                break;
            }
            int value = Math.max(0, stored.getOrDefault(key, 0));
            if (value <= 0) {
                stored.remove(key);
                continue;
            }
            int take = Math.min(value, overflow);
            decrementStored(stored, key, take);
            overflow -= take;
        }
        minion.setStoredItems(stored);
    }

    private ItemStack stackFromStoredId(String itemId) {
        String normalized = normalizeIngredientId(itemId);
        IngredientDefinition definition = ingredient(normalized);
        if (definition != null) {
            if (definition.customOnly()) {
                return createIngredientItem(definition.id(), 1);
            }
            return new ItemStack(definition.material(), 1);
        }

        if (UPGRADES.containsKey(normalized)) {
            return createUpgradeItem(normalized, 1);
        }
        if (FUELS.containsKey(normalized)) {
            return createFuelItem(normalized, 1);
        }

        Material direct = Material.matchMaterial(itemId);
        if (direct == null) {
            direct = Material.matchMaterial(itemId.toUpperCase(Locale.ROOT));
        }
        if (direct == null || direct.isAir()) {
            return null;
        }
        return new ItemStack(direct, 1);
    }

    private double npcSellPrice(String itemId) {
        String normalized = normalizeIngredientId(itemId);
        if (normalized == null) {
            return 0.0D;
        }
        Double cached = cachedNpcSellPrices.get(normalized);
        if (cached != null) {
            return cached;
        }
        double computed = npcSellPriceInternal(normalized, new HashSet<>());
        if (!Double.isFinite(computed) || computed <= 0.0D) {
            computed = 1.0D;
        }
        cachedNpcSellPrices.put(normalized, computed);
        return computed;
    }

    private double npcSellPriceInternal(String itemId, Set<String> seen) {
        if (itemId == null || itemId.isBlank()) {
            return 0.0D;
        }
        Double direct = NPC_SELL_PRICES.get(itemId);
        if (direct != null && direct > 0.0D) {
            return direct;
        }
        if (!seen.add(itemId)) {
            return 0.0D;
        }

        for (IngredientRecipe recipe : INGREDIENT_RECIPES) {
            if (!itemId.equals(recipe.outputIngredientId())) {
                continue;
            }
            double total = 0.0D;
            for (int slot = 0; slot < 9; slot++) {
                int amount = recipe.slotCosts()[slot];
                if (amount <= 0) {
                    continue;
                }
                String ingredientId = recipe.slotIngredientIds()[slot];
                total += amount * npcSellPriceInternal(normalizeIngredientId(ingredientId), seen);
            }
            if (total > 0.0D && recipe.outputAmount() > 0) {
                return total / recipe.outputAmount();
            }
        }
        return 1.0D;
    }

    private boolean depositToMinionProfile(MinionInstance minion, long coins) {
        if (minion == null || coins <= 0L) {
            return false;
        }
        ProfileManager profileManager = plugin.getProfileManager();
        if (profileManager == null) {
            return false;
        }

        SkyBlockProfile profile = null;
        if (minion.profileId() != null) {
            profile = profileManager.getProfile(minion.profileId());
        }
        if (profile == null && minion.ownerId() != null) {
            profile = profileManager.getSelectedProfile(minion.ownerId());
        }
        if (profile == null) {
            return false;
        }

        profile.setPurse(Math.max(0.0D, profile.getPurse() + coins));
        profile.addCoinsEarned((int) Math.min(Integer.MAX_VALUE, coins));
        profileManager.saveProfile(profile);
        return true;
    }

    private static void decrementOne(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return;
        }
        if (stack.getAmount() <= 1) {
            stack.setAmount(0);
            return;
        }
        stack.setAmount(stack.getAmount() - 1);
    }

    private boolean matchesRequirement(ItemStack stack, String requirement, int requiredAmount) {
        if (requiredAmount <= 0) {
            return stack == null || stack.getType().isAir();
        }
        if (stack == null || stack.getType().isAir() || stack.getAmount() < requiredAmount) {
            return false;
        }
        if (requirement == null || requirement.isBlank()) {
            return false;
        }

        String token = requirement.trim().toLowerCase(Locale.ROOT);
        if (token.startsWith("ing:")) {
            return matchesIngredient(stack, token.substring(4), requiredAmount);
        }
        if (token.startsWith("mat:")) {
            Material expected = Material.matchMaterial(token.substring(4).toUpperCase(Locale.ROOT));
            return expected != null && stack.getType() == expected;
        }
        if (token.startsWith("fuel:")) {
            String expected = normalizeUtilityId(token.substring(5));
            String actual = readFuelId(stack);
            return expected != null && expected.equals(actual);
        }
        if (token.startsWith("upg:")) {
            String expected = normalizeUtilityId(token.substring(4));
            String actual = readUpgradeId(stack);
            return expected != null && expected.equals(actual);
        }
        return matchesIngredient(stack, token, requiredAmount);
    }

    private static String formatPercent(double ratio) {
        double percent = ratio * 100.0D;
        if (Math.abs(percent - Math.rint(percent)) < 0.0001D) {
            return String.format(Locale.US, "%.0f%%", percent);
        }
        return String.format(Locale.US, "%.1f%%", percent);
    }

    private static String formatDecimal(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001D) {
            return String.format(Locale.US, "%.0f", value);
        }
        return String.format(Locale.US, "%.2f", value);
    }

    private static String formatDuration(long ms) {
        if (ms <= 0L) {
            return "Expired";
        }
        long seconds = Math.max(1L, ms / 1000L);
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        if (hours <= 0L) {
            return minutes + "m";
        }
        if (minutes <= 0L) {
            return hours + "h";
        }
        return hours + "h " + minutes + "m";
    }

    private static Map<String, FuelDefinition> buildFuelDefinitions() {
        Map<String, FuelDefinition> map = new LinkedHashMap<>();

        addFuel(map, "coal", "Coal", Material.COAL, 1.05D, 1.0D, 30L * 60L * 1000L, false);
        addFuel(map, "charcoal", "Charcoal", Material.CHARCOAL, 1.05D, 1.0D, 30L * 60L * 1000L, false);
        addFuel(map, "block_of_coal", "Block of Coal", Material.COAL_BLOCK, 1.05D, 1.0D, 5L * 60L * 60L * 1000L, false);
        addFuel(map, "enchanted_coal", "Enchanted Coal", Material.COAL, 1.10D, 1.0D, 24L * 60L * 60L * 1000L, false);
        addFuel(map, "enchanted_charcoal", "Enchanted Charcoal", Material.CHARCOAL, 1.20D, 1.0D, 36L * 60L * 60L * 1000L, false);
        addFuel(map, "hamster_wheel", "Hamster Wheel", Material.RABBIT_FOOT, 1.50D, 1.0D, 24L * 60L * 60L * 1000L, false);
        addFuel(map, "foul_flesh", "Foul Flesh", Material.ROTTEN_FLESH, 1.90D, 1.0D, 5L * 60L * 60L * 1000L, false);
        addFuel(map, "enchanted_bread", "Enchanted Bread", Material.BREAD, 1.05D, 1.0D, 12L * 60L * 60L * 1000L, false);
        addFuel(map, "catalyst", "Catalyst", Material.BLAZE_POWDER, 1.00D, 3.0D, 3L * 60L * 60L * 1000L, false);
        addFuel(map, "hyper_catalyst", "Hyper Catalyst", Material.BLAZE_ROD, 1.00D, 4.0D, 6L * 60L * 60L * 1000L, false);
        addFuel(map, "tasty_cheese", "Tasty Cheese", Material.HONEYCOMB, 1.00D, 2.0D, 1L * 60L * 60L * 1000L, false);
        addFuel(map, "solar_panel", "Solar Panel", Material.DAYLIGHT_DETECTOR, 1.25D, 1.0D, Long.MAX_VALUE, true);
        addFuel(map, "enchanted_lava_bucket", "Enchanted Lava Bucket", Material.LAVA_BUCKET, 1.25D, 1.0D, Long.MAX_VALUE, true);
        addFuel(map, "magma_bucket", "Magma Bucket", Material.MAGMA_CREAM, 1.30D, 1.0D, Long.MAX_VALUE, true);
        addFuel(map, "plasma_bucket", "Plasma Bucket", Material.BUCKET, 1.35D, 1.0D, Long.MAX_VALUE, true);
        addFuel(map, "everburning_flame", "Everburning Flame", Material.FIRE_CHARGE, 1.35D, 1.0D, Long.MAX_VALUE, true);

        // Custom extension
        addFuel(map, "grivience_starfuel", "Starfuel Cell", Material.NETHER_STAR, 1.60D, 1.0D, 24L * 60L * 60L * 1000L, false);

        return Map.copyOf(map);
    }

    private static void addFuel(
            Map<String, FuelDefinition> map,
            String id,
            String displayName,
            Material material,
            double speedMultiplier,
            double dropMultiplier,
            long durationMs,
            boolean permanent
    ) {
        map.put(id, new FuelDefinition(id, displayName, material, speedMultiplier, dropMultiplier, durationMs, permanent));
    }

    private static Map<String, UpgradeDefinition> buildUpgradeDefinitions() {
        Map<String, UpgradeDefinition> map = new LinkedHashMap<>();

        addUpgrade(map, "auto_smelter", "Auto Smelter", Material.FURNACE, UpgradeKind.PROCESSOR, "Automatically smelts generated items.", 0.0D);
        addUpgrade(map, "compactor", "Compactor", Material.DROPPER, UpgradeKind.PROCESSOR, "Compacts generated resources into block forms.", 0.0D);
        addUpgrade(map, "super_compactor_3000", "Super Compactor 3000", Material.OBSERVER, UpgradeKind.PROCESSOR, "Compacts resources into enchanted forms.", 0.0D);
        addUpgrade(map, "diamond_spreading", "Diamond Spreading", Material.DIAMOND, UpgradeKind.SPECIAL, "10% chance to generate diamonds per action.", 0.0D);
        addUpgrade(map, "corrupt_soil", "Corrupt Soil", Material.SOUL_SOIL, UpgradeKind.SPECIAL, "Generates Sulphur and Corrupted Fragments.", 0.0D);
        addUpgrade(map, "budget_hopper", "Budget Hopper", Material.HOPPER, UpgradeKind.SHIPPING, "Sells overflow resources automatically.", 0.50D);
        addUpgrade(map, "enchanted_hopper", "Enchanted Hopper", Material.HOPPER, UpgradeKind.SHIPPING, "Sells overflow resources automatically.", 0.70D);

        // Custom extensions
        addUpgrade(map, "grivience_overclock_chip", "Overclock Chip", Material.REDSTONE_BLOCK, UpgradeKind.SPECIAL, "Custom: +15% speed while installed.", 0.0D);
        addUpgrade(map, "grivience_astral_resonator", "Astral Resonator", Material.AMETHYST_CLUSTER, UpgradeKind.SPECIAL, "Custom: +8% speed and +5% Constellation Fragment chance while constellation is active.", 0.0D);
        addUpgrade(map, "grivience_quantum_hopper", "Quantum Hopper", Material.ENDER_CHEST, UpgradeKind.SHIPPING, "Custom: sells overflow at premium rate.", 0.85D);

        return Map.copyOf(map);
    }

    private static void addUpgrade(
            Map<String, UpgradeDefinition> map,
            String id,
            String displayName,
            Material material,
            UpgradeKind kind,
            String description,
            double sellMultiplier
    ) {
        map.put(id, new UpgradeDefinition(id, displayName, material, kind, description, sellMultiplier));
    }

    private static List<UtilityRecipe> buildUtilityRecipes() {
        List<UtilityRecipe> recipes = new ArrayList<>();

        recipes.add(utilityRecipe(
                UTILITY_UPGRADE,
                "auto_smelter",
                1,
                new int[]{8, 8, 8, 8, 1, 8, 8, 8, 8},
                new String[]{"ing:cobblestone", "ing:cobblestone", "ing:cobblestone", "ing:cobblestone", "ing:coal", "ing:cobblestone", "ing:cobblestone", "ing:cobblestone", "ing:cobblestone"}
        ));

        recipes.add(utilityRecipe(
                UTILITY_UPGRADE,
                "compactor",
                1,
                new int[]{1, 1, 1, 1, 1, 0, 1, 1, 1},
                new String[]{"ing:enchanted_cobblestone", "ing:enchanted_cobblestone", "ing:enchanted_cobblestone", "ing:enchanted_cobblestone", "ing:enchanted_cobblestone", "", "ing:enchanted_cobblestone", "ing:enchanted_redstone", "ing:enchanted_cobblestone"}
        ));

        recipes.add(utilityRecipe(
                UTILITY_UPGRADE,
                "super_compactor_3000",
                1,
                new int[]{32, 32, 32, 32, 1, 32, 32, 32, 32},
                new String[]{"ing:enchanted_cobblestone", "ing:enchanted_cobblestone", "ing:enchanted_cobblestone", "ing:enchanted_cobblestone", "upg:compactor", "ing:enchanted_cobblestone", "ing:enchanted_cobblestone", "ing:enchanted_redstone", "ing:enchanted_cobblestone"}
        ));

        recipes.add(utilityRecipe(
                UTILITY_UPGRADE,
                "diamond_spreading",
                1,
                new int[]{1, 1, 1, 1, 1, 1, 1, 1, 1},
                new String[]{"mat:VINE", "mat:VINE", "mat:VINE", "mat:VINE", "ing:enchanted_diamond", "mat:VINE", "mat:VINE", "mat:VINE", "mat:VINE"}
        ));

        recipes.add(utilityRecipe(
                UTILITY_UPGRADE,
                "budget_hopper",
                1,
                new int[]{1, 1, 0, 1, 1, 1, 0, 1, 0},
                new String[]{"ing:enchanted_iron", "ing:enchanted_iron", "", "ing:enchanted_iron", "mat:CHEST", "ing:enchanted_iron", "", "ing:enchanted_iron", ""}
        ));

        recipes.add(utilityRecipe(
                UTILITY_UPGRADE,
                "enchanted_hopper",
                1,
                new int[]{1, 1, 0, 1, 1, 1, 0, 1, 0},
                new String[]{"ing:enchanted_iron_block", "ing:enchanted_iron_block", "", "ing:enchanted_iron_block", "upg:budget_hopper", "ing:enchanted_iron_block", "", "ing:enchanted_iron_block", ""}
        ));

        recipes.add(utilityRecipe(
                UTILITY_UPGRADE,
                "corrupt_soil",
                1,
                new int[]{20, 20, 20, 20, 20, 20, 20, 20, 20},
                new String[]{"ing:enchanted_mycelium", "ing:enchanted_mycelium", "ing:enchanted_mycelium", "ing:enchanted_mycelium", "ing:corrupted_fragment", "ing:enchanted_mycelium", "ing:enchanted_mycelium", "ing:enchanted_mycelium", "ing:enchanted_mycelium"}
        ));

        recipes.add(utilityRecipe(
                UTILITY_UPGRADE,
                "grivience_overclock_chip",
                1,
                new int[]{1, 1, 1, 1, 1, 1, 1, 1, 1},
                new String[]{"ing:enchanted_redstone_block", "ing:enchanted_redstone_block", "ing:enchanted_redstone_block", "ing:enchanted_redstone_block", "upg:compactor", "ing:enchanted_redstone_block", "ing:enchanted_redstone_block", "ing:enchanted_redstone_block", "ing:enchanted_redstone_block"}
        ));

        recipes.add(utilityRecipe(
                UTILITY_UPGRADE,
                "grivience_astral_resonator",
                1,
                new int[]{8, 4, 8, 4, 1, 4, 8, 4, 8},
                new String[]{"ing:constellation_fragment", "ing:enchanted_redstone_block", "ing:constellation_fragment", "ing:enchanted_redstone_block", "upg:grivience_overclock_chip", "ing:enchanted_redstone_block", "ing:constellation_fragment", "ing:enchanted_redstone_block", "ing:constellation_fragment"}
        ));

        recipes.add(utilityRecipe(
                UTILITY_UPGRADE,
                "grivience_quantum_hopper",
                1,
                new int[]{1, 1, 1, 1, 1, 1, 1, 1, 1},
                new String[]{"ing:enchanted_diamond_block", "ing:enchanted_diamond_block", "ing:enchanted_diamond_block", "ing:enchanted_diamond_block", "upg:enchanted_hopper", "ing:enchanted_diamond_block", "ing:enchanted_diamond_block", "ing:enchanted_diamond_block", "ing:enchanted_diamond_block"}
        ));

        recipes.add(utilityRecipe(
                UTILITY_FUEL,
                "grivience_starfuel",
                1,
                new int[]{1, 1, 1, 1, 1, 1, 1, 1, 1},
                new String[]{"ing:enchanted_coal_block", "ing:enchanted_coal_block", "ing:enchanted_coal_block", "ing:enchanted_coal_block", "mat:NETHER_STAR", "ing:enchanted_coal_block", "ing:enchanted_coal_block", "ing:enchanted_coal_block", "ing:enchanted_coal_block"}
        ));

        return List.copyOf(recipes);
    }

    private static UtilityRecipe utilityRecipe(String outputType, String outputId, int outputAmount, int[] costs, String[] requirements) {
        return new UtilityRecipe(outputType, outputId, outputAmount, costs, requirements);
    }

    private static Map<String, String> buildAutoSmeltMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("cobblestone", "stone");
        map.put("potato_item", "baked_potato");
        return Map.copyOf(map);
    }

    private static Map<String, CompactRule> buildCompactorRules() {
        Map<String, CompactRule> map = new LinkedHashMap<>();
        map.put("coal", new CompactRule("coal", 9, "coal_block", 1));
        map.put("iron_ingot", new CompactRule("iron_ingot", 9, "iron_block", 1));
        map.put("gold_ingot", new CompactRule("gold_ingot", 9, "gold_block", 1));
        map.put("redstone", new CompactRule("redstone", 9, "redstone_block", 1));
        map.put("lapis_lazuli", new CompactRule("lapis_lazuli", 9, "lapis_block", 1));
        map.put("diamond", new CompactRule("diamond", 9, "diamond_block", 1));
        map.put("emerald", new CompactRule("emerald", 9, "emerald_block", 1));
        map.put("wheat", new CompactRule("wheat", 9, "hay_block", 1));
        return Map.copyOf(map);
    }

    private static List<SuperCompactorRule> buildSuperCompactorRules() {
        List<SuperCompactorRule> rules = new ArrayList<>();
        for (IngredientRecipe recipe : INGREDIENT_RECIPES) {
            if (recipe == null) {
                continue;
            }
            String inputId = null;
            int totalInput = 0;
            boolean singleInput = true;
            for (int slot = 0; slot < 9; slot++) {
                int cost = recipe.slotCosts()[slot];
                if (cost <= 0) {
                    continue;
                }
                String ingredientId = normalizeIngredientId(recipe.slotIngredientIds()[slot]);
                if (ingredientId == null) {
                    singleInput = false;
                    break;
                }
                if (inputId == null) {
                    inputId = ingredientId;
                } else if (!inputId.equals(ingredientId)) {
                    singleInput = false;
                    break;
                }
                totalInput += cost;
            }
            if (!singleInput || inputId == null || totalInput <= 0 || recipe.outputAmount() <= 0) {
                continue;
            }
            rules.add(new SuperCompactorRule(inputId, totalInput, recipe.outputIngredientId(), recipe.outputAmount()));
        }
        return List.copyOf(rules);
    }

    private static Map<String, Double> buildNpcSellPrices() {
        Map<String, Double> map = new LinkedHashMap<>();
        map.put("cobblestone", 1.0D);
        map.put("stone", 1.0D);
        map.put("coal", 2.0D);
        map.put("iron_ingot", 3.0D);
        map.put("gold_ingot", 4.0D);
        map.put("redstone", 1.0D);
        map.put("lapis_lazuli", 1.0D);
        map.put("diamond", 8.0D);
        map.put("emerald", 6.0D);
        map.put("wheat", 6.0D);
        map.put("hay_block", 54.0D);
        map.put("carrot_item", 3.0D);
        map.put("potato_item", 3.0D);
        map.put("baked_potato", 3.0D);
        map.put("sugar_cane", 4.0D);
        map.put("nether_stalk", 4.0D);
        map.put("golden_carrot", 3.0D);
        map.put("sulphur", 10.0D);
        map.put("corrupted_fragment", 20.0D);
        map.put("constellation_fragment", 35.0D);

        map.put("coal_block", 18.0D);
        map.put("iron_block", 27.0D);
        map.put("gold_block", 36.0D);
        map.put("redstone_block", 9.0D);
        map.put("lapis_block", 9.0D);
        map.put("diamond_block", 72.0D);
        map.put("emerald_block", 54.0D);

        return Map.copyOf(map);
    }

    private synchronized void loadData() {
        minionsById.clear();
        minionIdsByIsland.clear();
        minionIdByDisplayEntity.clear();
        constellationTierOverrideByIsland.clear();
        dirty = false;

        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection constellationSection = config.getConfigurationSection("constellation-overrides");
        if (constellationSection != null) {
            for (String key : constellationSection.getKeys(false)) {
                if (key == null || key.isBlank()) {
                    continue;
                }
                try {
                    UUID islandId = UUID.fromString(key);
                    int tier = clampConstellationTier(constellationSection.getInt(key, 0));
                    constellationTierOverrideByIsland.put(islandId, tier);
                } catch (Exception ignored) {
                }
            }
        }

        ConfigurationSection root = config.getConfigurationSection("minions");
        if (root == null) {
            return;
        }

        int loaded = 0;
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            MinionInstance minion = MinionInstance.fromSection(section);
            if (minion == null) {
                continue;
            }
            minionsById.put(minion.id(), minion);
            minionIdsByIsland.computeIfAbsent(minion.islandId(), ignored -> ConcurrentHashMap.newKeySet()).add(minion.id());
            loaded++;
        }

        if (loaded > 0) {
            plugin.getLogger().info("Loaded " + loaded + " minions.");
        }
    }

    private synchronized void saveData() {
        if (!dirty) {
            return;
        }
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection root = config.createSection("minions");
        for (MinionInstance minion : minionsById.values()) {
            ConfigurationSection section = root.createSection(minion.id().toString());
            minion.save(section);
        }
        if (!constellationTierOverrideByIsland.isEmpty()) {
            ConfigurationSection constellationSection = config.createSection("constellation-overrides");
            for (Map.Entry<UUID, Integer> entry : constellationTierOverrideByIsland.entrySet()) {
                if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                constellationSection.set(entry.getKey().toString(), clampConstellationTier(entry.getValue()));
            }
        }

        File parent = dataFile.getParentFile();
        if (parent != null && !parent.exists()) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }

        try {
            config.save(dataFile);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save minions: " + e.getMessage());
        }
    }

    private void saveIfDirty() {
        if (dirty) {
            saveData();
        }
    }

    private void markDirty() {
        dirty = true;
    }

    private String roman(int value) {
        return switch (value) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            case 11 -> "XI";
            case 12 -> "XII";
            default -> String.valueOf(value);
        };
    }

    private static String formatInt(long value) {
        return String.format(Locale.US, "%,d", value);
    }

    public record CraftingMatch(ItemStack result, int[] slotCosts) {
    }

    public record IngredientDefinition(String id, String displayName, Material material, boolean customOnly) {
    }

    public record IngredientRecipe(String outputIngredientId, int outputAmount, int[] slotCosts, String[] slotIngredientIds) {
    }

    public static Map<String, IngredientDefinition> getIngredients() {
        return Collections.unmodifiableMap(INGREDIENTS);
    }

    public static List<IngredientRecipe> getIngredientRecipes() {
        return INGREDIENT_RECIPES;
    }

    public static List<UtilityRecipeInfo> getUtilityRecipes() {
        List<UtilityRecipeInfo> info = new ArrayList<>(UTILITY_RECIPES.size());
        for (UtilityRecipe recipe : UTILITY_RECIPES) {
            if (recipe == null) {
                continue;
            }
            info.add(new UtilityRecipeInfo(
                    recipe.outputType(),
                    recipe.outputId(),
                    recipe.outputAmount(),
                    recipe.slotCosts().clone(),
                    recipe.requirements().clone()
            ));
        }
        return List.copyOf(info);
    }

    private record FuelDefinition(
            String id,
            String displayName,
            Material material,
            double speedMultiplier,
            double dropMultiplier,
            long durationMs,
            boolean permanent
    ) {
    }

    private enum UpgradeKind {
        PROCESSOR,
        SHIPPING,
        SPECIAL
    }

    private record UpgradeDefinition(
            String id,
            String displayName,
            Material material,
            UpgradeKind kind,
            String description,
            double sellMultiplier
    ) {
    }

    private record UtilityRecipe(String outputType, String outputId, int outputAmount, int[] slotCosts, String[] requirements) {
    }

    public record UtilityRecipeInfo(String outputType, String outputId, int outputAmount, int[] slotCosts, String[] requirements) {
        public UtilityRecipeInfo {
            slotCosts = slotCosts == null ? new int[0] : slotCosts.clone();
            requirements = requirements == null ? new String[0] : requirements.clone();
        }

        @Override
        public int[] slotCosts() {
            return slotCosts.clone();
        }

        @Override
        public String[] requirements() {
            return requirements.clone();
        }
    }

    public record SuperCompactorRuleInfo(String inputId, int inputAmount, String outputId, int outputAmount) {
    }

    private record CompactRule(String inputId, int inputAmount, String outputId, int outputAmount) {
    }

    private record SuperCompactorRule(String inputId, int inputAmount, String outputId, int outputAmount) {
    }

    public record FuelInfo(
            String id,
            String displayName,
            double speedMultiplier,
            double dropMultiplier,
            boolean permanent,
            boolean active,
            long remainingMs
    ) {
    }

    public record UpgradeInfo(
            String id,
            String displayName,
            String description,
            String kind
    ) {
    }

    public record ConstellationInfo(
            int tier,
            int uniqueMinionTypes,
            Integer overrideTier,
            double speedMultiplier,
            double fragmentChance
    ) {
    }

    public record MinionItemData(MinionType type, int tier) {
    }

    private record DisplayBinding(ArmorStand stand, boolean changed) {
    }
}
