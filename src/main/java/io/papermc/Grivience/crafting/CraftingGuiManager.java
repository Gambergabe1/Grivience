package io.papermc.Grivience.crafting;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.collections.CollectionsManager;
import io.papermc.Grivience.collections.PlayerCollectionProgress;
import io.papermc.Grivience.gui.SkyblockGui;
import io.papermc.Grivience.util.DropDeliveryUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Locale;

/**
 * Manages the Skyblock-accurate Crafting GUI.
 */
public final class CraftingGuiManager implements Listener {
    private static final int[] CONTENT_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    private static final int[] GRID_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};

    private final GriviencePlugin plugin;
    private final NamespacedKey typeKey;
    private final NamespacedKey valueKey;

    public CraftingGuiManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.typeKey = new NamespacedKey(plugin, "crafting_gui_type");
        this.valueKey = new NamespacedKey(plugin, "crafting_gui_value");
    }

    public enum GuiType {
        MAIN, CATEGORY, RECIPE_DETAIL, MATERIAL_INDEX, MATERIAL_DETAIL, GUIDE_RECIPE_DETAIL, SEARCH_RESULTS
    }

    private static class CraftingHolder implements InventoryHolder {
        private final GuiType type;
        private final Object data;
        private Inventory inventory;

        public CraftingHolder(GuiType type, Object data) {
            this.type = type;
            this.data = data;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private record MaterialIndexContext(int page) {
    }

    private record MaterialDetailContext(String materialKey, int page, int indexPage) {
    }

    private record GuideRecipeContext(CraftingGuideCatalog.GuideRecipe recipe, String materialKey, int page, int indexPage) {
    }

    public CraftingGuideCatalog buildGuideCatalog() {
        return CraftingGuideCatalog.build(plugin);
    }

    public List<CraftingGuideCatalog.GuideMaterial> findMaterials(String query) {
        return buildGuideCatalog().searchMaterials(query);
    }

    public List<String> materialSearchTerms() {
        List<String> terms = new ArrayList<>();
        for (CraftingGuideCatalog.GuideMaterial material : buildGuideCatalog().materials()) {
            terms.add(stripColor(material.name()));
        }
        return terms;
    }

    public void openMaterialIndex(Player player) {
        openMaterialIndex(player, 0);
    }

    public void openMaterialGuide(Player player, String materialKey) {
        openMaterialDetail(player, materialKey, 0, 0);
    }

    public void openMain(Player player) {
        CraftingHolder holder = new CraftingHolder(GuiType.MAIN, null);
        Inventory inv = Bukkit.createInventory(holder, 54, SkyblockGui.title("Crafting Table"));
        holder.inventory = inv;

        SkyblockGui.fillAll(inv, SkyblockGui.filler(Material.PURPLE_STAINED_GLASS_PANE));

        // Header
        inv.setItem(4, SkyblockGui.button(Material.CRAFTING_TABLE, ChatColor.GREEN + "Crafting Table", List.of(ChatColor.GRAY + "Select a category to browse recipes.")));

        // Categories
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        RecipeCategory[] categories = RecipeCategory.values();
        for (int i = 0; i < categories.length && i < slots.length; i++) {
            RecipeCategory cat = categories[i];
            inv.setItem(slots[i], createCategoryItem(cat));
        }

        // Bottom Row
        inv.setItem(45, SkyblockGui.backButton("Skyblock Menu"));
        inv.setItem(47, SkyblockGui.button(Material.COMPASS, ChatColor.GREEN + "Search Recipes", List.of(ChatColor.GRAY + "Find a recipe or crafting material.")));
        inv.setItem(48, SkyblockGui.button(Material.BOOKSHELF, ChatColor.GREEN + "Crafting Materials", List.of(ChatColor.GRAY + "Browse what each material can craft into.")));
        inv.setItem(49, SkyblockGui.button(Material.EXPERIENCE_BOTTLE, ChatColor.GOLD + "Carpentry Level", List.of(ChatColor.GRAY + "View your progress in Carpentry.")));
        inv.setItem(52, SkyblockGui.closeButton());

        player.openInventory(inv);
    }

    private ItemStack createCategoryItem(RecipeCategory category) {
        Material material = switch (category) {
            case FARMING -> Material.WHEAT;
            case MINING -> Material.DIAMOND_PICKAXE;
            case COMBAT -> Material.DIAMOND_SWORD;
            case FISHING -> Material.FISHING_ROD;
            case FORAGING -> Material.OAK_LOG;
            case ENCHANTING -> Material.ENCHANTING_TABLE;
            case ALCHEMY -> Material.BREWING_STAND;
            case CARPENTRY -> Material.CRAFTING_TABLE;
            case SLAYER -> Material.NETHER_STAR;
            case SPECIAL -> Material.DRAGON_EGG;
        };

        ItemStack item = SkyblockGui.button(material, ChatColor.GREEN + category.getDisplayName(), 
                List.of(ChatColor.GRAY + "Click to view " + category.getDisplayName() + " recipes."));
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(valueKey, PersistentDataType.STRING, category.name());
        item.setItemMeta(meta);
        return item;
    }

    public void openCategory(Player player, RecipeCategory category) {
        CraftingHolder holder = new CraftingHolder(GuiType.CATEGORY, category);
        Inventory inv = Bukkit.createInventory(holder, 54, SkyblockGui.title(category.getDisplayName() + " Recipes"));
        holder.inventory = inv;

        SkyblockGui.fillAll(inv, SkyblockGui.filler(Material.GRAY_STAINED_GLASS_PANE));

        inv.setItem(4, createCategoryItem(category));

        // Get both Registry and Catalog recipes
        List<SkyblockRecipe> registryRecipes = RecipeRegistry.getByCategory(category);
        CraftingGuideCatalog catalog = buildGuideCatalog();
        List<CraftingGuideCatalog.GuideRecipe> catalogRecipes = catalog.recipes().stream()
                .filter(r -> r.category() == category && !r.id().startsWith("registry:"))
                .toList();

        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        int slotIndex = 0;
        
        // Fill registry recipes
        for (int i = 0; i < registryRecipes.size() && slotIndex < slots.length; i++) {
            inv.setItem(slots[slotIndex++], createRecipeIcon(registryRecipes.get(i), player));
        }
        
        // Fill catalog recipes (e.g. custom armors)
        for (int i = 0; i < catalogRecipes.size() && slotIndex < slots.length; i++) {
            inv.setItem(slots[slotIndex++], createGuideRecipeIconForCategory(catalogRecipes.get(i), player));
        }

        inv.setItem(45, SkyblockGui.backButton("Main Menu"));
        inv.setItem(52, SkyblockGui.closeButton());

        player.openInventory(inv);
    }

    private ItemStack createGuideRecipeIconForCategory(CraftingGuideCatalog.GuideRecipe recipe, Player player) {
        boolean unlocked = true;
        String collectionId = recipe.collectionId();
        int requiredTier = recipe.collectionTierRequired();

        if (collectionId != null && !collectionId.isBlank() && requiredTier > 0) {
            CollectionsManager collectionsManager = plugin.getCollectionsManager();
            if (collectionsManager != null) {
                PlayerCollectionProgress progress = collectionsManager.getPlayerProgress(player, collectionId);
                unlocked = progress != null && progress.isTierUnlocked(requiredTier);
            }
        }

        ItemStack item = unlocked ? recipe.result().clone() : new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Set the custom name for both locked and unlocked states
        meta.setDisplayName((unlocked ? ChatColor.GREEN : ChatColor.RED) + recipe.name());

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Category: " + ChatColor.GREEN + (recipe.category() != null ? recipe.category().getDisplayName() : "Unknown"));
        lore.add("");

        if (collectionId != null && !collectionId.isBlank()) {
            ChatColor color = unlocked ? ChatColor.GREEN : ChatColor.RED;
            lore.add(color + "Requires " + collectionId + " Collection Tier " + requiredTier);
            lore.add("");
        }

        if (unlocked) {
            lore.add(ChatColor.YELLOW + "Click to view recipe!");
        } else {
            lore.add(ChatColor.RED + "This recipe is currently locked!");
        }

        meta.setLore(lore);
        meta.getPersistentDataContainer().set(valueKey, PersistentDataType.STRING, recipe.id());
        item.setItemMeta(meta);
        return item;
    }

    private void openMaterialIndex(Player player, int page) {
        CraftingGuideCatalog catalog = buildGuideCatalog();
        List<CraftingGuideCatalog.GuideMaterial> materials = catalog.materials();
        int safePage = Math.max(0, page);
        int totalPages = Math.max(1, (int) Math.ceil((double) materials.size() / (double) CONTENT_SLOTS.length));
        if (safePage >= totalPages) {
            safePage = totalPages - 1;
        }

        CraftingHolder holder = new CraftingHolder(GuiType.MATERIAL_INDEX, new MaterialIndexContext(safePage));
        Inventory inv = Bukkit.createInventory(holder, 54, SkyblockGui.title("Crafting Materials"));
        holder.inventory = inv;

        SkyblockGui.fillAll(inv, SkyblockGui.filler(Material.BROWN_STAINED_GLASS_PANE));
        inv.setItem(4, SkyblockGui.button(
                Material.BOOKSHELF,
                ChatColor.GREEN + "Crafting Materials",
                List.of(ChatColor.GRAY + "Every crafting material has a recipe menu.", ChatColor.GRAY + "Click an item to view what it crafts.")
        ));

        int start = safePage * CONTENT_SLOTS.length;
        for (int i = 0; i < CONTENT_SLOTS.length && start + i < materials.size(); i++) {
            CraftingGuideCatalog.GuideMaterial material = materials.get(start + i);
            inv.setItem(CONTENT_SLOTS[i], createMaterialIndexItem(material, catalog.recipesForMaterial(material.key()).size()));
        }

        inv.setItem(45, SkyblockGui.backButton("Crafting Table"));
        inv.setItem(49, SkyblockGui.closeButton());
        if (safePage > 0) {
            inv.setItem(48, SkyblockGui.button(Material.ARROW, ChatColor.GREEN + "Previous Page", List.of(ChatColor.GRAY + "Go to page " + safePage + ".")));
        }
        inv.setItem(50, SkyblockGui.button(Material.PAPER, ChatColor.YELLOW + "Page " + (safePage + 1) + "/" + totalPages, List.of(ChatColor.GRAY.toString() + materials.size() + " materials")));
        if (safePage + 1 < totalPages) {
            inv.setItem(52, SkyblockGui.button(Material.ARROW, ChatColor.GREEN + "Next Page", List.of(ChatColor.GRAY + "Go to page " + (safePage + 2) + ".")));
        }

        player.openInventory(inv);
    }

    private void openMaterialDetail(Player player, String materialKey, int page, int indexPage) {
        CraftingGuideCatalog catalog = buildGuideCatalog();
        Optional<CraftingGuideCatalog.GuideMaterial> materialOpt = catalog.material(materialKey);
        if (materialOpt.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Unknown crafting material.");
            return;
        }

        CraftingGuideCatalog.GuideMaterial material = materialOpt.get();
        List<CraftingGuideCatalog.GuideRecipe> recipes = catalog.recipesForMaterial(materialKey);
        int safePage = Math.max(0, page);
        int totalPages = Math.max(1, (int) Math.ceil((double) recipes.size() / (double) CONTENT_SLOTS.length));
        if (safePage >= totalPages) {
            safePage = totalPages - 1;
        }

        CraftingHolder holder = new CraftingHolder(GuiType.MATERIAL_DETAIL, new MaterialDetailContext(materialKey, safePage, Math.max(0, indexPage)));
        Inventory inv = Bukkit.createInventory(holder, 54, SkyblockGui.title(trimTitle(material.name() + " Uses")));
        holder.inventory = inv;

        SkyblockGui.fillAll(inv, SkyblockGui.filler(Material.GRAY_STAINED_GLASS_PANE));
        inv.setItem(4, decorateMenuItem(
                material.icon(),
                List.of(
                        ChatColor.GRAY + "Recipes: " + ChatColor.GREEN + recipes.size(),
                        "",
                        ChatColor.YELLOW + "This material is used in these crafts."
                ),
                false
        ));

        int start = safePage * CONTENT_SLOTS.length;
        for (int i = 0; i < CONTENT_SLOTS.length && start + i < recipes.size(); i++) {
            CraftingGuideCatalog.GuideRecipe recipe = recipes.get(start + i);
            inv.setItem(CONTENT_SLOTS[i], createGuideRecipeIcon(recipe));
        }

        inv.setItem(45, SkyblockGui.backButton("Crafting Materials"));
        inv.setItem(49, SkyblockGui.closeButton());
        if (safePage > 0) {
            inv.setItem(48, SkyblockGui.button(Material.ARROW, ChatColor.GREEN + "Previous Page", List.of(ChatColor.GRAY + "Go to page " + safePage + ".")));
        }
        inv.setItem(50, SkyblockGui.button(Material.PAPER, ChatColor.YELLOW + "Page " + (safePage + 1) + "/" + totalPages, List.of(ChatColor.GRAY.toString() + recipes.size() + " recipes")));
        if (safePage + 1 < totalPages) {
            inv.setItem(52, SkyblockGui.button(Material.ARROW, ChatColor.GREEN + "Next Page", List.of(ChatColor.GRAY + "Go to page " + (safePage + 2) + ".")));
        }

        player.openInventory(inv);
    }

    private ItemStack createRecipeIcon(SkyblockRecipe recipe, Player player) {
        boolean unlocked = true;
        String collectionId = recipe.getCollectionId();
        int requiredTier = recipe.getCollectionTierRequired();

        if (collectionId != null && !collectionId.isBlank() && requiredTier > 0) {
            CollectionsManager collectionsManager = plugin.getCollectionsManager();
            if (collectionsManager != null) {
                PlayerCollectionProgress progress = collectionsManager.getPlayerProgress(player, collectionId);
                unlocked = progress != null && progress.isTierUnlocked(requiredTier);
            }
        }

        ItemStack item = unlocked ? recipe.getResult().clone() : new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Set the custom name for both locked and unlocked states
        meta.setDisplayName((unlocked ? ChatColor.GREEN : ChatColor.RED) + recipe.getName());

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Category: " + ChatColor.GREEN + recipe.getCategory().getDisplayName());
        lore.add("");

        if (collectionId != null && !collectionId.isBlank()) {
            ChatColor color = unlocked ? ChatColor.GREEN : ChatColor.RED;
            lore.add(color + "Requires " + collectionId + " Collection Tier " + requiredTier);
            lore.add("");
        }

        if (unlocked) {
            lore.add(ChatColor.YELLOW + "Click to view recipe!");
        } else {
            lore.add(ChatColor.RED + "This recipe is currently locked!");
        }

        meta.setLore(lore);
        meta.getPersistentDataContainer().set(valueKey, PersistentDataType.STRING, recipe.getKey().toString());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createMaterialIndexItem(CraftingGuideCatalog.GuideMaterial material, int recipeCount) {
        ItemStack item = decorateMenuItem(
                material.icon(),
                List.of(
                        ChatColor.GRAY + "Recipes: " + ChatColor.GREEN + recipeCount,
                        "",
                        ChatColor.YELLOW + "Click to view craft uses."
                ),
                false
        );
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(valueKey, PersistentDataType.STRING, material.key());
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createGuideRecipeIcon(CraftingGuideCatalog.GuideRecipe recipe) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Source: " + ChatColor.GREEN + recipe.source());
        if (recipe.category() != null) {
            lore.add(ChatColor.GRAY + "Category: " + ChatColor.GREEN + recipe.category().getDisplayName());
        }
        if (recipe.collectionId() != null && !recipe.collectionId().isBlank()) {
            lore.add(ChatColor.RED + "Requires " + recipe.collectionId() + " Collection Tier " + recipe.collectionTierRequired());
        }
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to view recipe!");
        ItemStack item = decorateMenuItem(recipe.result(), lore, true);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(valueKey, PersistentDataType.STRING, recipe.id());
            item.setItemMeta(meta);
        }
        return item;
    }

    private void openGuideRecipeDetail(Player player, CraftingGuideCatalog.GuideRecipe recipe, String materialKey, int materialPage, int indexPage) {
        if (recipe == null) {
            player.sendMessage(ChatColor.RED + "Recipe data is unavailable.");
            return;
        }

        CraftingHolder holder = new CraftingHolder(GuiType.GUIDE_RECIPE_DETAIL, new GuideRecipeContext(recipe, materialKey, materialPage, indexPage));
        Inventory inv = Bukkit.createInventory(holder, 54, SkyblockGui.title(trimTitle(recipe.name())));
        holder.inventory = inv;

        SkyblockGui.fillAll(inv, SkyblockGui.filler(Material.GRAY_STAINED_GLASS_PANE));
        inv.setItem(13, recipe.result());

        for (CraftingGuideCatalog.GuideIngredient ingredient : recipe.ingredients()) {
            if (ingredient == null || ingredient.stack() == null || ingredient.stack().getType().isAir()) {
                continue;
            }
            inv.setItem(GRID_SLOTS[ingredient.slot()], ingredient.stack());
        }

        List<String> infoLore = new ArrayList<>();
        infoLore.add(ChatColor.GRAY + "Source: " + ChatColor.GREEN + recipe.source());
        if (recipe.category() != null) {
            infoLore.add(ChatColor.GRAY + "Category: " + ChatColor.GREEN + recipe.category().getDisplayName());
        }
        if (recipe.collectionId() != null && !recipe.collectionId().isBlank()) {
            infoLore.add(ChatColor.RED + "Requires " + recipe.collectionId() + " Collection Tier " + recipe.collectionTierRequired());
        }
        infoLore.add("");
        infoLore.add(ChatColor.YELLOW + "Click the result or any ingredient");
        infoLore.add(ChatColor.YELLOW + "to browse that material's recipes.");
        inv.setItem(16, SkyblockGui.button(Material.BOOK, ChatColor.GREEN + "Recipe Info", infoLore));

        inv.setItem(45, SkyblockGui.backButton("Material Uses"));
        inv.setItem(46, SkyblockGui.button(
                Material.CRAFTING_TABLE,
                ChatColor.GREEN + "Open Crafting Table",
                List.of(ChatColor.GRAY + "Crafting is done in the default crafting table UI.")
        ));
        inv.setItem(48, SkyblockGui.button(Material.PAPER, ChatColor.GREEN + "Ingredients Needed", getGuideIngredientLore(recipe, player)));
        inv.setItem(50, SkyblockGui.button(
                Material.BARRIER,
                ChatColor.RED + "Quick Craft Disabled",
                List.of(ChatColor.GRAY + "Use a crafting table to craft this recipe.")
        ));
        inv.setItem(52, SkyblockGui.closeButton());

        player.openInventory(inv);
    }

    public void openRecipeDetail(Player player, SkyblockRecipe recipe) {
        CraftingHolder holder = new CraftingHolder(GuiType.RECIPE_DETAIL, recipe);
        Inventory inv = Bukkit.createInventory(holder, 54, SkyblockGui.title(recipe.getName()));
        holder.inventory = inv;

        SkyblockGui.fillAll(inv, SkyblockGui.filler(Material.GRAY_STAINED_GLASS_PANE));

        // Result slot
        inv.setItem(13, recipe.getResult().clone());

        // Crafting Grid slots: 10,11,12, 19,20,21, 28,29,30
        int[] gridSlots = {10, 11, 12, 19, 20, 21, 28, 29, 30};
        
        if (recipe.getShape() == RecipeShape.SHAPELESS) {
            int slotIndex = 0;
            for (ItemStack ingredient : recipe.getIngredients().values()) {
                if (slotIndex < gridSlots.length) {
                    inv.setItem(gridSlots[slotIndex++], ingredient.clone());
                }
            }
        } else {
            String[] pattern = recipe.getShapePattern();
            if (pattern == null) {
                // Fallback for default patterns
                pattern = recipe.getShape() == RecipeShape.SHAPED_2X2 ? new String[]{"AB", "CD"} : new String[]{"ABC", "DEF", "GHI"};
            }
            
            Map<Character, ItemStack> ingredients = recipe.getIngredients();
            for (int r = 0; r < pattern.length; r++) {
                for (int c = 0; c < pattern[r].length(); c++) {
                    char symbol = pattern[r].charAt(c);
                    if (symbol != ' ' && ingredients.containsKey(symbol)) {
                        int slot = gridSlots[r * 3 + c];
                        inv.setItem(slot, ingredients.get(symbol).clone());
                    }
                }
            }
        }

        // Info book
        inv.setItem(16, SkyblockGui.button(
                Material.BOOK,
                ChatColor.GREEN + "Recipe Info",
                List.of(
                        ChatColor.GRAY + "This item can be crafted in a workbench.",
                        ChatColor.GRAY + "Use the material guide to browse",
                        ChatColor.GRAY + "what ingredients can craft into."
                )
        ));

        // Buttons
        inv.setItem(45, SkyblockGui.backButton(recipe.getCategory().getDisplayName()));
        inv.setItem(46, SkyblockGui.button(
                Material.CRAFTING_TABLE,
                ChatColor.GREEN + "Open Crafting Table",
                List.of(ChatColor.GRAY + "Crafting is done in the default crafting table UI.")
        ));
        inv.setItem(48, SkyblockGui.button(Material.PAPER, ChatColor.GREEN + "Ingredients Needed", getIngredientLore(recipe, player)));
        inv.setItem(50, SkyblockGui.button(
                Material.BARRIER,
                ChatColor.RED + "Quick Craft Disabled",
                List.of(ChatColor.GRAY + "Use a crafting table to craft this recipe.")
        ));
        inv.setItem(52, SkyblockGui.closeButton());

        player.openInventory(inv);
    }

    private List<String> getIngredientLore(SkyblockRecipe recipe, Player player) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Required Materials:");
        
        for (Map.Entry<Character, ItemStack> entry : recipe.getIngredients().entrySet()) {
            ItemStack item = entry.getValue();
            int count = item.getAmount();
            boolean has = player.getInventory().containsAtLeast(item, count);
            ChatColor color = has ? ChatColor.GREEN : ChatColor.RED;
            lore.add(color + "- " + ingredientName(item) + " x" + count);
        }

        lore.add("");
        lore.add(ChatColor.YELLOW + "Browse /craft materials");
        return lore;
    }

    private List<String> getGuideIngredientLore(CraftingGuideCatalog.GuideRecipe recipe, Player player) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Required Materials:");
        for (CraftingGuideCatalog.GuideIngredient ingredient : recipe.ingredients()) {
            if (ingredient == null || ingredient.stack() == null || ingredient.stack().getType().isAir()) {
                continue;
            }
            ItemStack item = ingredient.stack();
            int count = Math.max(1, item.getAmount());
            boolean has = player.getInventory().containsAtLeast(item, count);
            ChatColor color = has ? ChatColor.GREEN : ChatColor.RED;
            lore.add(color + "- " + ingredientName(item) + " x" + count);
        }
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click the result or ingredients");
        lore.add(ChatColor.YELLOW + "to browse their material menu.");
        return lore;
    }

    private ItemStack decorateMenuItem(ItemStack source, List<String> extraLore, boolean preserveLore) {
        if (source == null || source.getType().isAir()) {
            return new ItemStack(Material.BARRIER);
        }

        ItemStack clone = source.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta != null) {
            List<String> lore = preserveLore && meta.hasLore() && meta.getLore() != null
                    ? new ArrayList<>(meta.getLore())
                    : new ArrayList<>();
            if (!lore.isEmpty() && extraLore != null && !extraLore.isEmpty()) {
                lore.add("");
            }
            if (extraLore != null && !extraLore.isEmpty()) {
                lore.addAll(extraLore);
            }
            meta.setLore(lore);
            clone.setItemMeta(meta);
        }
        return clone;
    }

    private String ingredientName(ItemStack item) {
        if (item != null && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName() && meta.getDisplayName() != null && !meta.getDisplayName().isBlank()) {
                return stripColor(meta.getDisplayName());
            }
        }
        return stripColor(item == null ? "Unknown" : item.getType().name().toLowerCase(Locale.ROOT).replace('_', ' '));
    }

    private String trimTitle(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Crafting";
        }
        String stripped = stripColor(raw);
        return stripped.length() <= 30 ? stripped : stripped.substring(0, 30);
    }

    private String stripColor(String raw) {
        String stripped = ChatColor.stripColor(raw);
        return stripped == null ? "" : stripped;
    }

    private Optional<CraftingGuideCatalog.GuideMaterial> resolveGuideMaterial(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return Optional.empty();
        }
        return buildGuideCatalog().materialForItem(item);
    }

    private boolean isGridSlot(int slot) {
        for (int gridSlot : GRID_SLOTS) {
            if (gridSlot == slot) {
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof CraftingHolder holder)) return;

        event.setCancelled(true);

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getInventory())) {
            return;
        }

        int slot = event.getRawSlot();
        
        switch (holder.type) {
            case MAIN -> handleMainClick(player, slot, event.getCurrentItem());
            case CATEGORY -> handleCategoryClick(player, slot, (RecipeCategory) holder.data, event.getCurrentItem());
            case RECIPE_DETAIL -> handleRecipeDetailClick(player, slot, (SkyblockRecipe) holder.data);
            case MATERIAL_INDEX -> handleMaterialIndexClick(player, slot, (MaterialIndexContext) holder.data, event.getCurrentItem());
            case MATERIAL_DETAIL -> handleMaterialDetailClick(player, slot, (MaterialDetailContext) holder.data, event.getCurrentItem());
            case GUIDE_RECIPE_DETAIL -> handleGuideRecipeDetailClick(player, slot, (GuideRecipeContext) holder.data, event.getCurrentItem());
        }
    }

    private void handleMainClick(Player player, int slot, ItemStack item) {
        if (slot == 52) {
            player.closeInventory();
            return;
        }
        if (slot == 45) {
            plugin.getSkyblockMenuManager().openMainMenu(player);
            return;
        }
        if (slot == 47) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "Use /craft <recipe or material> to search.");
            return;
        }
        if (slot == 48) {
            openMaterialIndex(player);
            return;
        }
        if (slot == 49) {
            if (plugin.getSkillsGui() != null) {
                plugin.getSkillsGui().openSkillDetails(player, io.papermc.Grivience.skills.SkyblockSkill.CARPENTRY);
            } else {
                player.sendMessage(ChatColor.YELLOW + "Carpentry system is coming soon!");
            }
            return;
        }

        if (item != null && item.hasItemMeta()) {
            String catName = item.getItemMeta().getPersistentDataContainer().get(valueKey, PersistentDataType.STRING);
            if (catName != null) {
                try {
                    RecipeCategory cat = RecipeCategory.valueOf(catName);
                    openCategory(player, cat);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    private void handleCategoryClick(Player player, int slot, RecipeCategory category, ItemStack item) {
        if (slot == 45) {
            openMain(player);
            return;
        }
        if (slot == 52) {
            player.closeInventory();
            return;
        }

        if (item != null && item.hasItemMeta()) {
            String val = item.getItemMeta().getPersistentDataContainer().get(valueKey, PersistentDataType.STRING);
            if (val != null) {
                // Try Registry first
                NamespacedKey key = NamespacedKey.fromString(val);
                if (key != null && RecipeRegistry.getByKey(key).isPresent()) {
                    RecipeRegistry.getByKey(key).ifPresent(recipe -> openRecipeDetail(player, recipe));
                } else {
                    // Try Catalog
                    buildGuideCatalog().recipe(val).ifPresent(recipe -> openGuideRecipeDetail(player, recipe, "", 0, 0));
                }
            }
        }
    }

    private void handleRecipeDetailClick(Player player, int slot, SkyblockRecipe recipe) {
        if (slot == 45) {
            openCategory(player, recipe.getCategory());
            return;
        }
        if (slot == 52) {
            player.closeInventory();
            return;
        }

        if (slot == 46) {
            // Use the vanilla crafting UI for all custom recipes.
            player.closeInventory();
            player.openWorkbench(null, true);
            player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_TRAPDOOR_OPEN, 1f, 1.2f);
            player.sendMessage(ChatColor.GRAY + "Craft this recipe using a crafting table.");
        } else if (slot == 50) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            player.sendMessage(ChatColor.YELLOW + "Quick crafting is disabled. Use a crafting table.");
        }
    }

    private void handleMaterialIndexClick(Player player, int slot, MaterialIndexContext context, ItemStack item) {
        if (slot == 45) {
            openMain(player);
            return;
        }
        if (slot == 49) {
            player.closeInventory();
            return;
        }
        if (slot == 48 && context.page() > 0) {
            openMaterialIndex(player, context.page() - 1);
            return;
        }
        if (slot == 52) {
            CraftingGuideCatalog catalog = buildGuideCatalog();
            int totalPages = Math.max(1, (int) Math.ceil((double) catalog.materials().size() / (double) CONTENT_SLOTS.length));
            if (context.page() + 1 < totalPages) {
                openMaterialIndex(player, context.page() + 1);
            }
            return;
        }

        if (item != null && item.hasItemMeta()) {
            String materialKey = item.getItemMeta().getPersistentDataContainer().get(valueKey, PersistentDataType.STRING);
            if (materialKey != null && !materialKey.isBlank()) {
                openMaterialDetail(player, materialKey, 0, context.page());
            }
        }
    }

    private void handleMaterialDetailClick(Player player, int slot, MaterialDetailContext context, ItemStack item) {
        if (slot == 45) {
            openMaterialIndex(player, context.indexPage());
            return;
        }
        if (slot == 49) {
            player.closeInventory();
            return;
        }
        if (slot == 48 && context.page() > 0) {
            openMaterialDetail(player, context.materialKey(), context.page() - 1, context.indexPage());
            return;
        }
        if (slot == 52) {
            int totalPages = Math.max(1, (int) Math.ceil((double) buildGuideCatalog().recipesForMaterial(context.materialKey()).size() / (double) CONTENT_SLOTS.length));
            if (context.page() + 1 < totalPages) {
                openMaterialDetail(player, context.materialKey(), context.page() + 1, context.indexPage());
            }
            return;
        }

        if (item != null && item.hasItemMeta()) {
            String recipeId = item.getItemMeta().getPersistentDataContainer().get(valueKey, PersistentDataType.STRING);
            if (recipeId != null && !recipeId.isBlank()) {
                buildGuideCatalog().recipe(recipeId).ifPresent(recipe -> openGuideRecipeDetail(player, recipe, context.materialKey(), context.page(), context.indexPage()));
            }
        }
    }

    private void handleGuideRecipeDetailClick(Player player, int slot, GuideRecipeContext context, ItemStack item) {
        if (slot == 45) {
            openMaterialDetail(player, context.materialKey(), context.page(), context.indexPage());
            return;
        }
        if (slot == 52) {
            player.closeInventory();
            return;
        }
        if (slot == 46) {
            player.closeInventory();
            player.openWorkbench(null, true);
            player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_TRAPDOOR_OPEN, 1f, 1.2f);
            player.sendMessage(ChatColor.GRAY + "Craft this recipe using a crafting table.");
            return;
        }
        if (slot == 50) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            player.sendMessage(ChatColor.YELLOW + "Quick crafting is disabled. Use a crafting table.");
            return;
        }

        if ((slot == 13 || isGridSlot(slot)) && item != null && !item.getType().isAir()) {
            Optional<CraftingGuideCatalog.GuideMaterial> material = resolveGuideMaterial(item);
            if (material.isPresent() && !buildGuideCatalog().recipesForMaterial(material.get().key()).isEmpty()) {
                openMaterialDetail(player, material.get().key(), 0, context.indexPage());
            }
        }
    }

    private boolean tryCraft(Player player, SkyblockRecipe recipe, int multiplier) {
        // Staff Protection
        io.papermc.Grivience.item.StaffManager staffManager = plugin.getStaffManager();
        if (staffManager != null && staffManager.isEnabled()) {
            for (ItemStack invItem : player.getInventory().getContents()) {
                if (invItem != null && staffManager.isStaff(invItem)) {
                    // Check if this staff item would be consumed by the recipe
                    // To be safe, we just prevent crafting if any staff is in inventory
                    // but we should only prevent it if it's one of the materials used.
                    for (ItemStack ingredient : recipe.getIngredients().values()) {
                        if (invItem.isSimilar(ingredient)) {
                            player.sendMessage(ChatColor.RED + "You cannot use a Staff as a crafting ingredient!");
                            return false;
                        }
                    }
                }
            }
        }

        // Check ingredients
        for (ItemStack ingredient : recipe.getIngredients().values()) {
            ItemStack check = ingredient.clone();
            check.setAmount(ingredient.getAmount() * multiplier);
            if (!player.getInventory().containsAtLeast(check, check.getAmount())) {
                return false;
            }
        }

        // Remove ingredients
        for (ItemStack ingredient : recipe.getIngredients().values()) {
            ItemStack remove = ingredient.clone();
            remove.setAmount(ingredient.getAmount() * multiplier);
            player.getInventory().removeItem(remove);
        }

        // Add result
        ItemStack result = recipe.getResult().clone();
        result.setAmount(result.getAmount() * multiplier);
        DropDeliveryUtil.giveToInventoryOrDrop(player, result, player.getLocation());

        return true;
    }

    private int calculateMaxCraft(Player player, SkyblockRecipe recipe) {
        int max = 64; // Limit to one stack for now
        for (ItemStack ingredient : recipe.getIngredients().values()) {
            int has = 0;
            for (ItemStack invItem : player.getInventory().getContents()) {
                if (invItem != null && invItem.isSimilar(ingredient)) {
                    has += invItem.getAmount();
                }
            }
            int canMake = has / ingredient.getAmount();
            max = Math.min(max, canMake);
        }
        return max;
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof CraftingHolder) {
            event.setCancelled(true);
        }
    }
}
