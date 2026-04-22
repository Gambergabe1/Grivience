package io.papermc.Grivience.collections;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.crafting.RecipeRegistry;
import io.papermc.Grivience.crafting.SkyblockRecipe;
import io.papermc.Grivience.gui.SkyblockGui;
import io.papermc.Grivience.item.ArmorCraftingListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Skyblock 100% Accurate Collections GUI.
 * Matches the exact layout, styling, and behavior of Skyblock's collections menu.
 */
public class CollectionGUI implements Listener, InventoryHolder {
    private final GriviencePlugin plugin;
    private final CollectionsManager collectionsManager;
    private final Inventory holderInventory;

    // Skyblock-accurate inventory sizes and titles
    private static final String MAIN_TITLE = SkyblockGui.title("Collections");
    
    // Skyblock-accurate slot positions
    private static final int[] CATEGORY_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    private static final int[] COLLECTION_SLOTS = {9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44};
    private static final int[] TIER_SLOTS = {9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44};
    
    // GUI State per player
    private final Map<UUID, GuiState> playerStates;

    public CollectionGUI(GriviencePlugin plugin, CollectionsManager collectionsManager) {
        this.plugin = plugin;
        this.collectionsManager = collectionsManager;
        this.playerStates = new HashMap<>();
        this.holderInventory = Bukkit.createInventory(this, 54, MAIN_TITLE);
    }

    /**
     * Open the main collections menu - Skyblock accurate layout.
     */
    public void openMainGui(Player player) {
        openMainGui(player, player);
    }

    public void openMainGui(Player viewer, Player viewedPlayer) {
        ViewContext viewContext = createViewContext(viewer, viewedPlayer);
        openMainGui(viewer, viewContext);
    }

    private void openMainGui(Player viewer, ViewContext viewContext) {
        Inventory gui = Bukkit.createInventory(this, 54, MAIN_TITLE);

        fillSkyblockBackground(gui);

        // Header information (slot 4 - center top)
        gui.setItem(4, createMainHeader(viewContext));

        // Category buttons in Skyblock-accurate positions
        int slotIndex = 0;
        for (CollectionCategory category : CollectionCategory.values()) {
            if (slotIndex >= CATEGORY_SLOTS.length) break;
            
            int totalInCategory = collectionsManager.getCollectionsByCategory(category).size();
            if (totalInCategory == 0) continue; // Skip empty categories
            
            ItemStack icon = createCategoryIcon(category, totalInCategory, viewContext);
            gui.setItem(CATEGORY_SLOTS[slotIndex], icon);
            slotIndex++;
        }

        // Decorative items (Skyblock style)
        gui.setItem(48, createMenuItem(
            Material.BOOK,
            ChatColor.YELLOW + "" + ChatColor.BOLD + "Collection Guide",
            List.of(
                "",
                ChatColor.GRAY + "Collections track items you've",
                ChatColor.GRAY + "gathered throughout your gameplay.",
                "",
                ChatColor.GRAY + "Collect items to unlock:",
                ChatColor.GREEN + "  • Crafting Recipes",
                ChatColor.GREEN + "  • Skill XP",
                ChatColor.GREEN + "  • Stat Bonuses",
                ChatColor.GREEN + "  • Skyblock XP",
                "",
                ChatColor.YELLOW + "Click a category to browse!"
            )
        ));

        gui.setItem(50, createProfileStatsItem(viewContext));

        // Close button (bottom right)
        gui.setItem(53, createCloseButton());

        viewer.openInventory(gui);
        playOpenSound(viewer);

        playerStates.put(viewer.getUniqueId(), new GuiState(viewContext, GuiType.MAIN, null, null, null));
    }

    /**
     * Open category collections menu - Skyblock accurate.
     */
    public void openCategoryGui(Player player, CollectionCategory category) {
        openCategoryGui(player, createViewContext(player, player), category);
    }

    private void openCategoryGui(Player viewer, ViewContext viewContext, CollectionCategory category) {
        String title = SkyblockGui.title(ChatColor.stripColor(category.getDisplayName()));
        Inventory gui = Bukkit.createInventory(this, 54, title);

        fillSkyblockBackground(gui);

        // Category header
        gui.setItem(4, createCategoryHeader(category, viewContext));

        List<CollectionDefinition> collections = collectionsManager.getCollectionsByCategory(category);
        List<String> subcategories = collectionsManager.getSubcategories(category);

        if (!subcategories.isEmpty()) {
            int slotIndex = 0;
            for (String subcategory : subcategories) {
                if (slotIndex >= COLLECTION_SLOTS.length) break;
                ItemStack icon = createSubcategoryIcon(category, subcategory);
                gui.setItem(COLLECTION_SLOTS[slotIndex], icon);
                slotIndex++;
            }
        } else {
            int slotIndex = 0;
            for (CollectionDefinition collection : collections) {
                if (!collection.isEnabled()) continue;
                if (slotIndex >= COLLECTION_SLOTS.length) break;

                PlayerCollectionProgress progress = getProgress(viewContext, collection.getId());
                ItemStack icon = createCollectionIcon(collection, progress);
                gui.setItem(COLLECTION_SLOTS[slotIndex], icon);
                slotIndex++;
            }
        }

        // Back button
        gui.setItem(48, createBackButton("Collections"));

        // Info item
        gui.setItem(50, createMenuItem(
            Material.BOOK,
            ChatColor.YELLOW + "" + ChatColor.BOLD + "Category Info",
            List.of(
                "",
                ChatColor.GRAY + "Category: " + category.getDisplayName(),
                ChatColor.GRAY + "Collections: " + ChatColor.GREEN + collections.size(),
                subcategories.isEmpty() ? "" : (ChatColor.GRAY + "Subcategories: " + ChatColor.GREEN + subcategories.size()),
                "",
                subcategories.isEmpty() ? ChatColor.GRAY + "Click a collection to view" : ChatColor.GRAY + "Click a subcategory to view",
                subcategories.isEmpty() ? ChatColor.GRAY + "tiers and rewards." : ChatColor.GRAY + "its collections.",
                "",
                ChatColor.YELLOW + "Click to learn more!"
            )
        ));

        // Close button
        gui.setItem(53, createCloseButton());

        viewer.openInventory(gui);
        playOpenSound(viewer);

        playerStates.put(viewer.getUniqueId(), new GuiState(viewContext, GuiType.CATEGORY, category, null, null));
    }

    public void openSubcategoryGui(Player player, CollectionCategory category, String subcategory) {
        openSubcategoryGui(player, createViewContext(player, player), category, subcategory);
    }

    private void openSubcategoryGui(Player viewer, ViewContext viewContext, CollectionCategory category, String subcategory) {
        if (viewer == null || category == null || subcategory == null || subcategory.isBlank()) {
            return;
        }

        String title = SkyblockGui.title(ChatColor.stripColor(category.getDisplayName()));
        Inventory gui = Bukkit.createInventory(this, 54, title);

        fillSkyblockBackground(gui);

        List<String> headerLore = new ArrayList<>();
        headerLore.add("");
        headerLore.add(ChatColor.GRAY + "Subcategory: " + ChatColor.YELLOW + subcategory);
        headerLore.add(ChatColor.GRAY + "Category: " + category.getDisplayName());
        headerLore.add("");
        headerLore.add(ChatColor.GRAY + "Click a collection to view");
        headerLore.add(ChatColor.GRAY + "tiers and rewards.");
        gui.setItem(4, createMenuItem(category.getIcon(), ChatColor.GOLD + "" + ChatColor.BOLD + category.getDisplayName(), headerLore));

        List<CollectionDefinition> collections = collectionsManager.getCollectionsByCategoryAndSubcategory(category, subcategory);
        int slotIndex = 0;
        for (CollectionDefinition collection : collections) {
            if (!collection.isEnabled()) continue;
            if (slotIndex >= COLLECTION_SLOTS.length) break;

            PlayerCollectionProgress progress = getProgress(viewContext, collection.getId());
            ItemStack icon = createCollectionIcon(collection, progress);
            gui.setItem(COLLECTION_SLOTS[slotIndex], icon);
            slotIndex++;
        }

        gui.setItem(48, createBackButton(categoryName(category)));
        gui.setItem(53, createCloseButton());

        viewer.openInventory(gui);
        playOpenSound(viewer);

        playerStates.put(viewer.getUniqueId(), new GuiState(viewContext, GuiType.SUBCATEGORY, category, subcategory, null));
    }

    /**
     * Open collection details menu - Skyblock accurate tier display.
     */
    public void openCollectionDetailsGui(Player player, CollectionDefinition collection) {
        openCollectionDetailsGui(player, createViewContext(player, player), collection, null);
    }

    private void openCollectionDetailsGui(Player viewer, ViewContext viewContext, CollectionDefinition collection, String subcategory) {
        Inventory gui = Bukkit.createInventory(this, 54, SkyblockGui.title(ChatColor.stripColor(collection.getName())));

        fillSkyblockBackground(gui);

        PlayerCollectionProgress progress = getProgress(viewContext, collection.getId());

        // Collection info item (center - slot 13)
        gui.setItem(13, createCollectionDetailIcon(collection, progress));

        // Tier display (surrounding the center item)
        List<CollectionTier> tiers = collection.getTiers();
        int tierSlotIndex = 0;
        
        for (CollectionTier tier : tiers) {
            while (tierSlotIndex < TIER_SLOTS.length && TIER_SLOTS[tierSlotIndex] == 13) {
                tierSlotIndex++;
            }
            if (tierSlotIndex >= TIER_SLOTS.length) break;

            ItemStack tierIcon = createTierIcon(tier, progress, collection);
            gui.setItem(TIER_SLOTS[tierSlotIndex], tierIcon);
            tierSlotIndex++;
        }

        // Stats panel (bottom left)
        gui.setItem(45, createStatsPanel(viewer, viewContext, collection, progress));
        
        // Progress panel (bottom center-left)
        gui.setItem(46, createProgressPanel(collection, progress));
        
        // Rewards summary (bottom center-right)
        gui.setItem(48, createRewardsSummary(collection, progress));

        // Back button
        gui.setItem(50, createBackButton(categoryName(collection.getCategory())));

        // Close button
        gui.setItem(53, createCloseButton());

        viewer.openInventory(gui);
        playOpenSound(viewer);

        playerStates.put(viewer.getUniqueId(), new GuiState(viewContext, GuiType.COLLECTION_DETAILS, collection.getCategory(), subcategory, collection.getId()));
    }

    // ==================== ITEM CREATORS ====================

    private ItemStack createMainHeader(ViewContext viewContext) {
        ItemStack item = new ItemStack(Material.BOOKSHELF);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Skyblock Collections");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Viewing: " + ChatColor.GOLD + viewContext.profileLabel());
        lore.add("");
        lore.add(ChatColor.GRAY + "Collections track items you've gathered");
        lore.add(ChatColor.GRAY + "throughout your gameplay on Skyblock.");
        lore.add("");
        lore.add(ChatColor.GRAY + "Total Collections: " + ChatColor.GREEN + collectionsManager.getEnabledCollections().size());
        lore.add(ChatColor.GRAY + "Maxed Collections: " + ChatColor.GOLD + collectionsManager.getMaxedCollectionsCount(viewContext.profileId));
        lore.add(ChatColor.GRAY + "Total Items Collected: " + ChatColor.YELLOW + formatNumber(collectionsManager.getTotalCollectedItems(viewContext.profileId)));
        lore.add("");
        lore.add(ChatColor.GRAY + "Collect items naturally by:");
        lore.add(ChatColor.GREEN + "  • Mining ores and stone");
        lore.add(ChatColor.GREEN + "  • Chopping wood");
        lore.add(ChatColor.GREEN + "  • Harvesting crops");
        lore.add(ChatColor.GREEN + "  • Killing mobs");
        lore.add(ChatColor.GREEN + "  • Fishing");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click a category to browse collections!");
        
        meta.setLore(sanitizeLore(lore));
        item.setItemMeta(meta);
        
        return item;
    }

    private ItemStack createProfileStatsItem(ViewContext viewContext) {
        return createMenuItem(
                Material.PLAYER_HEAD,
                ChatColor.YELLOW + "" + ChatColor.BOLD + viewContext.statsTitle(),
                createStatsLore(viewContext)
        );
    }

    private ItemStack createCategoryHeader(CollectionCategory category, ViewContext viewContext) {
        ItemStack item = new ItemStack(category.getIcon());
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.BOLD + "" + category.getDisplayName());
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Viewing: " + ChatColor.YELLOW + viewContext.profileLabel());
        lore.add("");
        lore.add(ChatColor.GRAY + "Browse collections in this category.");
        lore.add("");
        
        int totalInCategory = collectionsManager.getCollectionsByCategory(category).size();
        int maxedInCategory = 0;
        
        for (CollectionDefinition collection : collectionsManager.getCollectionsByCategory(category)) {
            PlayerCollectionProgress progress = getProgress(viewContext, collection.getId());
            if (collection.isMaxed(progress.getCollectedAmount())) {
                maxedInCategory++;
            }
        }
        
        lore.add(ChatColor.GRAY + "Collections: " + ChatColor.GREEN + totalInCategory);
        lore.add(ChatColor.GRAY + "Maxed: " + ChatColor.GOLD + maxedInCategory + ChatColor.GRAY + "/" + ChatColor.GREEN + totalInCategory);
        lore.add("");
        lore.add(ChatColor.YELLOW + (collectionsManager.getSubcategories(category).isEmpty()
                ? "Click a collection to view tiers!"
                : "Click a subcategory to browse!"));
        
        meta.setLore(sanitizeLore(lore));
        item.setItemMeta(meta);
        
        return item;
    }

    private ItemStack createCategoryIcon(CollectionCategory category, int totalInCategory, ViewContext viewContext) {
        ItemStack item = new ItemStack(category.getIcon());
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.BOLD + "" + category.getDisplayName());
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Collections: " + ChatColor.GREEN + totalInCategory);
        lore.add(ChatColor.GRAY + "Completed: " + ChatColor.GOLD + countMaxedCollectionsInCategory(category, viewContext) + ChatColor.GRAY + "/" + ChatColor.GREEN + totalInCategory);
        lore.add("");
        lore.add(ChatColor.GRAY + category.getDescription());
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to browse!");
        
        meta.setLore(sanitizeLore(lore));
        item.setItemMeta(meta);
        
        return item;
    }

    private ItemStack createSubcategoryIcon(CollectionCategory category, String subcategory) {
        List<CollectionDefinition> collections = collectionsManager.getCollectionsByCategoryAndSubcategory(category, subcategory);

        Material icon = category.getIcon();
        if (!collections.isEmpty() && collections.get(0).getIcon() != null) {
            icon = collections.get(0).getIcon();
        }

        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + subcategory);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Collections: " + ChatColor.GREEN + collections.size());
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to browse!");

        meta.setLore(sanitizeLore(lore));
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createCollectionIcon(CollectionDefinition collection, PlayerCollectionProgress progress) {
        ItemStack item = new ItemStack(collection.getIcon() != null ? collection.getIcon() : Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        
        // Color based on completion
        boolean maxed = collection.isMaxed(progress.getCollectedAmount());
        ChatColor color = maxed ? ChatColor.GREEN : ChatColor.GOLD;
        
        meta.setDisplayName(color + collection.getName());
        
        List<String> lore = new ArrayList<>();
        int currentTierLevel = collection.getCurrentTierLevel(progress.getCollectedAmount());
        int maxTierLevel = collection.getTiers().isEmpty() ? 0 : collection.getTiers().get(collection.getTiers().size() - 1).getTierLevel();

        if (currentTierLevel > 0) {
            lore.add(ChatColor.YELLOW + "Tier " + CollectionTier.toRoman(currentTierLevel));
        }

        if (currentTierLevel < maxTierLevel) {
            CollectionTier nextTier = collection.getTiers().get(currentTierLevel);
            double percent = (double) progress.getCollectedAmount() / nextTier.getAmountRequired() * 100.0;
            if (percent > 100.0) percent = 100.0;

            lore.add(ChatColor.GRAY + "Progress to Tier " + CollectionTier.toRoman(nextTier.getTierLevel()) + ": " + ChatColor.YELLOW + String.format("%.1f%%", percent));
            lore.add(createProgressBar(percent, 20));
            lore.add(ChatColor.YELLOW + formatNumber(progress.getCollectedAmount()) + ChatColor.GRAY + "/" + ChatColor.YELLOW + formatNumber(nextTier.getAmountRequired()));
            lore.add("");
            lore.add(ChatColor.GREEN + "Tier " + CollectionTier.toRoman(nextTier.getTierLevel()) + " Rewards:");
            for (CollectionReward reward : nextTier.getRewards()) {
                lore.add("  " + reward.getFormattedLore());
            }
        } else {
            lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "MAXED OUT!");
            lore.add(ChatColor.YELLOW + formatNumber(progress.getCollectedAmount()) + " " + ChatColor.GRAY + "items collected");
        }
        
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to view details!");
        
        meta.setLore(sanitizeLore(lore));
        item.setItemMeta(meta);
        
        return item;
    }

    private ItemStack createCollectionDetailIcon(CollectionDefinition collection, PlayerCollectionProgress progress) {
        ItemStack item = new ItemStack(collection.getIcon() != null ? collection.getIcon() : Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.GOLD + collection.getName());
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + collection.getCategory().getDisplayName());
        if (collection.getDescription() != null && !collection.getDescription().isEmpty()) {
            lore.add(ChatColor.DARK_GRAY + collection.getDescription());
        }
        lore.add("");
        
        int currentTierLevel = collection.getCurrentTierLevel(progress.getCollectedAmount());
        int maxTierLevel = collection.getTiers().isEmpty() ? 0 : collection.getTiers().get(collection.getTiers().size() - 1).getTierLevel();
        
        if (currentTierLevel > 0) {
            lore.add(ChatColor.YELLOW + "Tier " + CollectionTier.toRoman(currentTierLevel));
        }

        if (currentTierLevel < maxTierLevel) {
            CollectionTier nextTier = collection.getTiers().get(currentTierLevel);
            double percent = (double) progress.getCollectedAmount() / nextTier.getAmountRequired() * 100.0;
            if (percent > 100.0) percent = 100.0;

            lore.add(ChatColor.GRAY + "Progress to Tier " + CollectionTier.toRoman(nextTier.getTierLevel()) + ": " + ChatColor.YELLOW + String.format("%.1f%%", percent));
            lore.add(createProgressBar(percent, 20));
            lore.add(ChatColor.YELLOW + formatNumber(progress.getCollectedAmount()) + ChatColor.GRAY + "/" + ChatColor.YELLOW + formatNumber(nextTier.getAmountRequired()));
            lore.add("");
            lore.add(ChatColor.GREEN + "Tier " + CollectionTier.toRoman(nextTier.getTierLevel()) + " Rewards:");
            for (CollectionReward reward : nextTier.getRewards()) {
                lore.add("  " + reward.getFormattedLore());
            }
        } else {
            lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "MAXED OUT!");
            lore.add(ChatColor.YELLOW + formatNumber(progress.getCollectedAmount()) + " " + ChatColor.GRAY + "items collected");
        }
        
        meta.setLore(sanitizeLore(lore));
        item.setItemMeta(meta);
        
        return item;
    }

    private ItemStack createTierIcon(CollectionTier tier, PlayerCollectionProgress progress, CollectionDefinition collection) {
        boolean unlocked = progress.isTierUnlocked(tier.getTierLevel());
        
        ItemStack item = new ItemStack(unlocked ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        
        ChatColor color = unlocked ? ChatColor.GREEN : ChatColor.GRAY;
        meta.setDisplayName(color + "" + ChatColor.BOLD + "Tier " + CollectionTier.toRoman(tier.getTierLevel()));
        
        List<String> lore = new ArrayList<>();
        
        if (unlocked) {
            lore.add("");
            lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "UNLOCKED!");
            lore.add("");
            lore.add(ChatColor.GRAY + "Required: " + ChatColor.GOLD + formatNumber(tier.getAmountRequired()));
            lore.add(ChatColor.GRAY + "Your Progress: " + ChatColor.GREEN + formatNumber(progress.getCollectedAmount()));
            lore.add("");
            lore.add(ChatColor.GREEN + "Rewards Unlocked:");
            for (CollectionReward reward : tier.getRewards()) {
                lore.add("  " + reward.getFormattedLore());
            }

            for (String recipeLine : recipeUnlockLore(collection, tier.getTierLevel())) {
                lore.add("  " + recipeLine);
            }
        } else {
            lore.add("");
            lore.add(ChatColor.RED + "" + ChatColor.BOLD + "LOCKED");
            lore.add("");
            lore.add(ChatColor.GRAY + "Required: " + ChatColor.GOLD + formatNumber(tier.getAmountRequired()));
            lore.add(ChatColor.GRAY + "Your Progress: " + ChatColor.YELLOW + formatNumber(progress.getCollectedAmount()));
            
            if (progress.getCollectedAmount() > 0) {
                double percent = Math.min(100, (progress.getCollectedAmount() * 100.0) / tier.getAmountRequired());
                lore.add(createProgressBar(percent, 15));
            }
            
            lore.add("");
            long needed = tier.getAmountRequired() - progress.getCollectedAmount();
            lore.add(ChatColor.RED + "" + formatNumber(needed) + " more needed!");
            
            if (!tier.getRewards().isEmpty()) {
                lore.add("");
                lore.add(ChatColor.GRAY + "Rewards:");
                for (CollectionReward reward : tier.getRewards()) {
                    lore.add("  " + ChatColor.GRAY + reward.getFormattedLore());
                }
            }

            List<String> recipeUnlocks = recipeUnlockLore(collection, tier.getTierLevel());
            if (!recipeUnlocks.isEmpty()) {
                lore.add("");
                lore.add(ChatColor.GRAY + "Recipes:");
                for (String recipeLine : recipeUnlocks) {
                    lore.add("  " + recipeLine);
                }
            }
        }
        
        meta.setLore(sanitizeLore(lore));
        item.setItemMeta(meta);
        
        return item;
    }

    private List<String> recipeUnlockLore(CollectionDefinition collection, int tierLevel) {
        if (collection == null || tierLevel <= 0) {
            return List.of();
        }
        String collectionId = collection.getId();
        if (collectionId == null || collectionId.isBlank()) {
            return List.of();
        }

        Set<String> names = new java.util.LinkedHashSet<>();
        for (SkyblockRecipe recipe : RecipeRegistry.getAll()) {
            if (recipe == null || recipe.getCollectionId() == null) {
                continue;
            }
            if (recipe.getCollectionTierRequired() != tierLevel) {
                continue;
            }
            if (!recipe.getCollectionId().equalsIgnoreCase(collectionId)) {
                continue;
            }
            names.add(recipe.getName());
        }

        if (plugin.getArmorCraftingListener() != null) {
            for (ArmorCraftingListener.ArmorRecipeDefinition recipe : plugin.getArmorCraftingListener().getRegisteredRecipeDefinitions()) {
                if (recipe == null || recipe.collectionId() == null) {
                    continue;
                }
                if (recipe.collectionTierRequired() != tierLevel) {
                    continue;
                }
                if (!recipe.collectionId().equalsIgnoreCase(collectionId)) {
                    continue;
                }
                names.add(recipe.name());
            }
        }

        List<String> lines = new ArrayList<>();
        for (String name : names) {
            lines.add(ChatColor.YELLOW + "Unlock Recipe: " + ChatColor.AQUA + name);
        }
        return lines;
    }

    private ItemStack createStatsPanel(Player viewer, ViewContext viewContext, CollectionDefinition collection, PlayerCollectionProgress progress) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Collection Stats");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Collection: " + ChatColor.GOLD + collection.getName());
        lore.add(ChatColor.GRAY + "Category: " + collection.getCategory().getDisplayName());
        lore.add("");

        boolean coopProfile = false;
        int coopMembers = 1;
        Set<UUID> coopMemberIds = Set.of();
        if (plugin.getIslandManager() != null && progress != null && progress.getProfileId() != null) {
            coopMemberIds = plugin.getIslandManager().getCoopMemberIdsForProfileId(progress.getProfileId());
            coopMembers = Math.max(1, coopMemberIds.size());
            coopProfile = coopMembers > 1;
        }

        lore.add(ChatColor.GRAY + (coopProfile ? "Co-op Progress:" : viewContext.progressLabel()));
        lore.add("  " + ChatColor.GOLD + "Collected: " + ChatColor.WHITE + formatNumber(progress.getCollectedAmount()));
        lore.add("  " + ChatColor.GOLD + "Total: " + ChatColor.WHITE + formatNumber(collection.getTotalAmountRequired()));
        long totalRequired = Math.max(1L, collection.getTotalAmountRequired());
        lore.add("  " + ChatColor.GOLD + "Percentage: " + ChatColor.WHITE + String.format("%.1f", (progress.getCollectedAmount() * 100.0) / totalRequired) + "%");
        lore.add("");
        lore.add(ChatColor.GRAY + "Current Tier: " + ChatColor.GOLD + CollectionTier.toRoman(collection.getCurrentTierLevel(progress.getCollectedAmount())));
        lore.add("");
        
        int maxedTiers = 0;
        for (CollectionTier tier : collection.getTiers()) {
            if (progress.isTierUnlocked(tier.getTierLevel())) {
                maxedTiers++;
            }
        }
        lore.add(ChatColor.GRAY + "Tiers Unlocked: " + ChatColor.GREEN + maxedTiers + ChatColor.GRAY + "/" + collection.getTiers().size());
        lore.add(ChatColor.GRAY + "Skyblock XP: " + ChatColor.AQUA + progress.getTotalSkyblockXpEarned());
        lore.add("");

        if (coopProfile && viewer != null && viewer.getUniqueId() != null) {
            Map<UUID, Long> contributions = progress.getContributionsByMember();
            UUID viewerId = viewer.getUniqueId();

            lore.add(ChatColor.GRAY + "Co-op Members: " + ChatColor.GREEN + coopMembers);
            lore.add("");
            lore.add(ChatColor.GRAY + "Co-op Contributions:");

            List<UUID> ordered = new ArrayList<>(coopMemberIds);
            ordered.sort((a, b) -> Long.compare(
                    contributions.getOrDefault(b, 0L),
                    contributions.getOrDefault(a, 0L)
            ));

            int shown = 0;
            for (UUID memberId : ordered) {
                if (memberId == null) {
                    continue;
                }
                if (shown >= 5) {
                    break;
                }

                long contributed = contributions.getOrDefault(memberId, 0L);
                boolean isViewer = memberId.equals(viewerId);
                String name;
                if (isViewer) {
                    name = "You";
                } else {
                    String offlineName = Bukkit.getOfflinePlayer(memberId).getName();
                    name = (offlineName == null || offlineName.isBlank()) ? memberId.toString().substring(0, 8) : offlineName;
                }

                ChatColor nameColor = isViewer ? ChatColor.GREEN : ChatColor.YELLOW;
                lore.add("  " + nameColor + name + ChatColor.GRAY + ": " + ChatColor.WHITE + formatNumber(contributed));
                shown++;
            }

            int remaining = ordered.size() - shown;
            if (remaining > 0) {
                lore.add("  " + ChatColor.DARK_GRAY + "(+" + remaining + " more)");
            }

            lore.add("");
        }
        
        int rank = collectionsManager.getPlayerRank(progress.getProfileId(), collection.getId());
        if (rank > 0) {
            lore.add(ChatColor.GRAY + (viewContext.selfView ? "Your Rank: " : "Profile Rank: ") + ChatColor.GOLD + "#" + rank);
        }
        
        meta.setLore(sanitizeLore(lore));
        item.setItemMeta(meta);
        
        return item;
    }

    private ItemStack createProgressPanel(CollectionDefinition collection, PlayerCollectionProgress progress) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Progress Tracker");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        
        CollectionTier nextTier = collection.getNextTier(progress.getCollectedAmount());
        if (nextTier != null && !collection.isMaxed(progress.getCollectedAmount())) {
            long needed = nextTier.getAmountRequired() - progress.getCollectedAmount();
            lore.add(ChatColor.GRAY + "Next Tier: " + ChatColor.AQUA + CollectionTier.toRoman(nextTier.getTierLevel()));
            lore.add(ChatColor.GRAY + "Required: " + ChatColor.GOLD + formatNumber(nextTier.getAmountRequired()));
            lore.add(ChatColor.GRAY + "Have: " + ChatColor.GREEN + formatNumber(progress.getCollectedAmount()));
            lore.add(ChatColor.GRAY + "Needed: " + ChatColor.RED + formatNumber(needed));
            lore.add("");
            lore.add(ChatColor.GRAY + "Keep collecting to unlock!");
        } else {
            lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "MAX COLLECTION!");
            lore.add("");
            lore.add(ChatColor.GRAY + "You've collected all");
            lore.add(ChatColor.GRAY + formatNumber(collection.getTotalAmountRequired()) + " items!");
        }
        
        meta.setLore(sanitizeLore(lore));
        item.setItemMeta(meta);
        
        return item;
    }

    private ItemStack createRewardsSummary(CollectionDefinition collection, PlayerCollectionProgress progress) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Rewards Summary");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        
        // Count total rewards
        int totalRewards = 0;
        int unlockedRewards = 0;
        int totalSkyblockXp = 0;
        int unlockedSkyblockXp = 0;
        
        for (CollectionTier tier : collection.getTiers()) {
            totalRewards += tier.getRewards().size();
            totalSkyblockXp += tier.getTotalSkyblockXp();
            
            if (progress.isTierUnlocked(tier.getTierLevel())) {
                unlockedRewards += tier.getRewards().size();
                unlockedSkyblockXp += tier.getTotalSkyblockXp();
            }
        }
        
        lore.add(ChatColor.GRAY + "Total Rewards: " + ChatColor.GOLD + totalRewards);
        lore.add(ChatColor.GRAY + "Unlocked: " + ChatColor.GREEN + unlockedRewards);
        lore.add(ChatColor.GRAY + "Locked: " + ChatColor.RED + (totalRewards - unlockedRewards));
        lore.add("");
        lore.add(ChatColor.GRAY + "Skyblock XP: " + ChatColor.AQUA + unlockedSkyblockXp + ChatColor.GRAY + "/" + totalSkyblockXp);
        lore.add("");
        
        if (progress.getUnlockedTiers().isEmpty()) {
            lore.add(ChatColor.GRAY + "Collect more to unlock");
            lore.add(ChatColor.GRAY + "your first rewards!");
        } else {
            lore.add(ChatColor.GREEN + "Keep collecting for");
            lore.add(ChatColor.GREEN + "more rewards!");
        }
        
        meta.setLore(sanitizeLore(lore));
        item.setItemMeta(meta);
        
        return item;
    }

    private ItemStack createMenuItem(Material material, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CollectionTextUtil.sanitizeDisplayText(displayName));
        meta.setLore(sanitizeLore(lore));
        item.setItemMeta(meta);
        return item;
    }

    private List<String> sanitizeLore(List<String> lore) {
        if (lore == null || lore.isEmpty()) {
            return lore;
        }

        List<String> sanitized = new ArrayList<>(lore.size());
        for (String line : lore) {
            sanitized.add(CollectionTextUtil.sanitizeDisplayText(line));
        }
        return sanitized;
    }

    private ItemStack createBackButton(String target) {
        return SkyblockGui.backButton(ChatColor.stripColor(target));
    }

    private ItemStack createCloseButton() {
        return SkyblockGui.closeButton();
    }

    private void fillSkyblockBackground(Inventory gui) {
        SkyblockGui.fillAll(gui, SkyblockGui.filler(Material.BLACK_STAINED_GLASS_PANE));

        ItemStack border = SkyblockGui.filler(Material.GRAY_STAINED_GLASS_PANE);
        int size = gui.getSize();
        for (int slot = 0; slot < size; slot++) {
            boolean top = slot < 9;
            boolean bottom = slot >= size - 9;
            boolean left = slot % 9 == 0;
            boolean right = slot % 9 == 8;
            if (top || bottom || left || right) {
                gui.setItem(slot, border.clone());
            }
        }
    }

    private String createProgressBar(double percent, int length) {
        if (length >= 0) {
            return CollectionTextUtil.createProgressBar(percent, length);
        }
        int filled = (int) (percent / 100.0 * length);
        int empty = length - filled;
        
        StringBuilder bar = new StringBuilder(ChatColor.GREEN.toString());
        for (int i = 0; i < filled; i++) {
            bar.append("█");
        }
        bar.append(ChatColor.GRAY);
        for (int i = 0; i < empty; i++) {
            bar.append("█");
        }
        
        return bar.toString();
    }

    private String formatNumber(long amount) {
        if (amount >= 1_000_000_000) {
            return String.format("%.1fB", amount / 1_000_000_000.0);
        } else if (amount >= 1_000_000) {
            return String.format("%.1fM", amount / 1_000_000.0);
        } else if (amount >= 1_000) {
            return String.format("%.1fK", amount / 1_000.0);
        } else {
            return String.valueOf(amount);
        }
    }

    private String categoryName(CollectionCategory category) {
        if (category == null) return "Collections";
        return category.getDisplayName();
    }

    private List<String> createStatsLore(ViewContext viewContext) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Viewing: " + ChatColor.GOLD + viewContext.profileLabel());
        lore.add("");
        lore.add(ChatColor.GRAY + "Total Collections: " + ChatColor.GREEN + collectionsManager.getEnabledCollections().size());
        lore.add(ChatColor.GRAY + "Maxed: " + ChatColor.GOLD + collectionsManager.getMaxedCollectionsCount(viewContext.profileId));
        lore.add(ChatColor.GRAY + "Total Items: " + ChatColor.YELLOW + formatNumber(collectionsManager.getTotalCollectedItems(viewContext.profileId)));
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to view details!");
        return lore;
    }

    private void playOpenSound(Player player) {
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
    }

    private ViewContext createViewContext(Player viewer, Player viewedPlayer) {
        Player target = viewedPlayer == null ? viewer : viewedPlayer;
        UUID profileId = target == null ? null : collectionsManager.getProfileId(target);
        if (profileId == null && target != null) {
            profileId = target.getUniqueId();
        }

        String displayName = target == null ? "Unknown" : target.getName();
        if (displayName == null || displayName.isBlank()) {
            displayName = profileId == null ? "Unknown" : profileId.toString().substring(0, 8);
        }

        boolean selfView = viewer != null
                && target != null
                && viewer.getUniqueId() != null
                && viewer.getUniqueId().equals(target.getUniqueId());
        return new ViewContext(profileId, displayName, selfView);
    }

    private PlayerCollectionProgress getProgress(ViewContext viewContext, String collectionId) {
        if (viewContext == null || viewContext.profileId == null) {
            return new PlayerCollectionProgress(null, collectionId);
        }
        return collectionsManager.getPlayerProgress(viewContext.profileId, collectionId);
    }

    private int countMaxedCollectionsInCategory(CollectionCategory category, ViewContext viewContext) {
        int maxed = 0;
        for (CollectionDefinition collection : collectionsManager.getCollectionsByCategory(category)) {
            PlayerCollectionProgress progress = getProgress(viewContext, collection.getId());
            if (collection.isMaxed(progress.getCollectedAmount())) {
                maxed++;
            }
        }
        return maxed;
    }

    // ==================== EVENT HANDLERS ====================

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        // If the top inventory is a Collections menu, cancel ALL clicks
        if (event.getInventory().getHolder() instanceof CollectionGUI) {
            event.setCancelled(true);
            
            // Only handle clicks in the top inventory
            if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getInventory())) {
                return;
            }

            if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) return;

            GuiState state = playerStates.get(player.getUniqueId());
            if (state == null) return;

            ItemStack clicked = event.getCurrentItem();
            if (!clicked.hasItemMeta()) return;

            String displayName = clicked.getItemMeta().getDisplayName();
            if (displayName == null) return;

            // Handle close button
            if (displayName.contains("Close")) {
                player.closeInventory();
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.5f);
                return;
            }

            // Handle back button
            if (displayName.contains("Back")) {
                handleBackButton(player, state);
                return;
            }

            // Handle category click (main menu)
            if (state.guiType == GuiType.MAIN) {
                for (CollectionCategory category : CollectionCategory.values()) {
                    if (displayName.contains(ChatColor.stripColor(category.getDisplayName()))) {
                        openCategoryGui(player, state.viewContext, category);
                        return;
                    }
                }
            }

            // Handle collection click (category menu)
            if (state.guiType == GuiType.CATEGORY && state.category != null) {
                List<String> subcategories = collectionsManager.getSubcategories(state.category);
                if (!subcategories.isEmpty()) {
                    for (String subcategory : subcategories) {
                        if (displayName.contains(ChatColor.stripColor(subcategory))) {
                            openSubcategoryGui(player, state.viewContext, state.category, subcategory);
                            return;
                        }
                    }
                    return;
                }

                List<CollectionDefinition> collections = collectionsManager.getCollectionsByCategory(state.category);
                for (CollectionDefinition collection : collections) {
                    if (displayName.contains(ChatColor.stripColor(collection.getName()))) {
                        openCollectionDetailsGui(player, state.viewContext, collection, null);
                        return;
                    }
                }
            }

            // Handle collection click (subcategory menu)
            if (state.guiType == GuiType.SUBCATEGORY && state.category != null && state.subcategory != null) {
                List<CollectionDefinition> collections = collectionsManager.getCollectionsByCategoryAndSubcategory(state.category, state.subcategory);
                for (CollectionDefinition collection : collections) {
                    if (displayName.contains(ChatColor.stripColor(collection.getName()))) {
                        openCollectionDetailsGui(player, state.viewContext, collection, state.subcategory);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof CollectionGUI)) return;
        playerStates.remove(event.getPlayer().getUniqueId());
    }

    private void handleBackButton(Player player, GuiState state) {
        switch (state.guiType) {
            case MAIN -> player.performCommand("skyblock menu");
            case CATEGORY -> openMainGui(player, state.viewContext);
            case SUBCATEGORY -> openCategoryGui(player, state.viewContext, state.category);
            case COLLECTION_DETAILS -> {
                if (state.category == null) {
                    openMainGui(player, state.viewContext);
                } else if (state.subcategory != null && !state.subcategory.isBlank()) {
                    openSubcategoryGui(player, state.viewContext, state.category, state.subcategory);
                } else {
                    openCategoryGui(player, state.viewContext, state.category);
                }
            }
            case LEADERBOARD -> openMainGui(player, state.viewContext);
            default -> player.closeInventory();
        }
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
    }

    // ==================== GUI STATE ====================

    private static class ViewContext {
        final UUID profileId;
        final String displayName;
        final boolean selfView;

        ViewContext(UUID profileId, String displayName, boolean selfView) {
            this.profileId = profileId;
            this.displayName = displayName == null || displayName.isBlank() ? "Unknown" : displayName;
            this.selfView = selfView;
        }

        String profileLabel() {
            return selfView ? "Your Profile" : displayName;
        }

        String statsTitle() {
            return selfView ? "Your Stats" : possessive(displayName) + " Stats";
        }

        String progressLabel() {
            return selfView ? "Your Progress:" : possessive(displayName) + " Progress:";
        }

        private String possessive(String raw) {
            if (raw == null || raw.isBlank()) {
                return "Profile";
            }
            return raw.endsWith("s") ? raw + "'" : raw + "'s";
        }
    }

    private static class GuiState {
        final ViewContext viewContext;
        final GuiType guiType;
        final CollectionCategory category;
        final String subcategory;
        final String collectionId;

        GuiState(ViewContext viewContext, GuiType guiType, CollectionCategory category, String subcategory, String collectionId) {
            this.viewContext = viewContext;
            this.guiType = guiType;
            this.category = category;
            this.subcategory = subcategory;
            this.collectionId = collectionId;
        }
    }

    // ==================== GUI TYPES ====================

    public enum GuiType {
        MAIN,
        CATEGORY,
        SUBCATEGORY,
        COLLECTION_DETAILS,
        LEADERBOARD
    }

    @Override
    public Inventory getInventory() {
        return holderInventory;
    }
}

