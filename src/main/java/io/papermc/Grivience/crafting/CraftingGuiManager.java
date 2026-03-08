package io.papermc.Grivience.crafting;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.gui.SkyblockGui;
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

/**
 * Manages the Skyblock-accurate Crafting GUI.
 */
public final class CraftingGuiManager implements Listener {
    private final GriviencePlugin plugin;
    private final NamespacedKey typeKey;
    private final NamespacedKey valueKey;

    public CraftingGuiManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.typeKey = new NamespacedKey(plugin, "crafting_gui_type");
        this.valueKey = new NamespacedKey(plugin, "crafting_gui_value");
    }

    public enum GuiType {
        MAIN, CATEGORY, RECIPE_DETAIL, SEARCH_RESULTS
    }

    private static class CraftingHolder implements InventoryHolder {
        private final GuiType type;
        private final Object data; // e.g., RecipeCategory or SkyblockRecipe or String (query)
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
        inv.setItem(47, SkyblockGui.button(Material.COMPASS, ChatColor.GREEN + "Search Recipes", List.of(ChatColor.GRAY + "Find a specific recipe by name.")));
        inv.setItem(48, SkyblockGui.button(Material.BOOKSHELF, ChatColor.GREEN + "Recipe Guide", List.of(ChatColor.GRAY + "View all unlocked recipes.")));
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

        List<SkyblockRecipe> recipes = RecipeRegistry.getByCategory(category);
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        
        for (int i = 0; i < recipes.size() && i < slots.length; i++) {
            SkyblockRecipe recipe = recipes.get(i);
            inv.setItem(slots[i], createRecipeIcon(recipe, player));
        }

        inv.setItem(45, SkyblockGui.backButton("Main Menu"));
        inv.setItem(52, SkyblockGui.closeButton());

        player.openInventory(inv);
    }

    private ItemStack createRecipeIcon(SkyblockRecipe recipe, Player player) {
        ItemStack item = recipe.getResult().clone();
        ItemMeta meta = item.getItemMeta();
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Category: " + ChatColor.GREEN + recipe.getCategory().getDisplayName());
        lore.add("");
        
        // Requirements (simplified for now)
        if (recipe.getCollectionId() != null) {
            lore.add(ChatColor.RED + "Requires " + recipe.getCollectionId() + " Collection Tier " + recipe.getCollectionTierRequired());
            lore.add("");
        }
        
        lore.add(ChatColor.YELLOW + "Click to view recipe!");
        
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(valueKey, PersistentDataType.STRING, recipe.getKey().toString());
        item.setItemMeta(meta);
        return item;
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
        inv.setItem(16, SkyblockGui.button(Material.BOOK, ChatColor.GREEN + "Recipe Info", List.of(ChatColor.GRAY + "This item can be crafted in a workbench.")));

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
            lore.add(color + "- " + item.getType().name() + " x" + count);
        }
        
        return lore;
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
            player.sendMessage(ChatColor.YELLOW + "Use /craft <name> to search for a recipe!");
            return;
        }
        if (slot == 48) {
            player.sendMessage(ChatColor.YELLOW + "Recipe Guide is under construction!");
            return;
        }
        if (slot == 49) {
            player.sendMessage(ChatColor.YELLOW + "Carpentry system is coming soon!");
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
            String keyStr = item.getItemMeta().getPersistentDataContainer().get(valueKey, PersistentDataType.STRING);
            if (keyStr != null) {
                NamespacedKey key = NamespacedKey.fromString(keyStr);
                if (key != null) {
                    RecipeRegistry.getByKey(key).ifPresent(recipe -> openRecipeDetail(player, recipe));
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
        Map<Integer, ItemStack> leftOver = player.getInventory().addItem(result);
        for (ItemStack item : leftOver.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }

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
