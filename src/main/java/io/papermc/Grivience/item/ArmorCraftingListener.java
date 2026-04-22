package io.papermc.Grivience.item;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.collections.CollectionTier;
import io.papermc.Grivience.collections.CollectionsManager;
import io.papermc.Grivience.collections.PlayerCollectionProgress;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.RecipeChoice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ArmorCraftingListener implements Listener {
    private static final long WARN_THROTTLE_MS = 1500L;

    private final GriviencePlugin plugin;
    private final CustomArmorManager armorManager;
    private final CustomItemService customItemService;
    private final Map<NamespacedKey, String> recipeToSetMap = new HashMap<>();
    private final Set<NamespacedKey> armorRecipeKeys = new HashSet<>();
    private final Map<NamespacedKey, ArmorRecipeDefinition> armorRecipeDefinitions = new HashMap<>();
    private final Map<UUID, Long> lastWarnAtMs = new ConcurrentHashMap<>();

    public ArmorCraftingListener(GriviencePlugin plugin, CustomArmorManager armorManager, CustomItemService customItemService) {
        this.plugin = plugin;
        this.armorManager = armorManager;
        this.customItemService = customItemService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepare(PrepareItemCraftEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) {
            return;
        }

        ArmorRecipeDefinition recipeDefinition = recipeDefinition(event.getRecipe());
        if (recipeDefinition == null) {
            return;
        }

        if (!meetsCollectionRequirement(player, recipeDefinition)) {
            event.getInventory().setResult(null);
            warnLocked(player, recipeDefinition);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ArmorRecipeDefinition recipeDefinition = recipeDefinition(event.getRecipe());
        if (recipeDefinition != null && !meetsCollectionRequirement(player, recipeDefinition)) {
            event.setCancelled(true);
            warnLocked(player, recipeDefinition);
            return;
        }

        ItemStack result = event.getRecipe().getResult();
        if (result == null) {
            return;
        }

        String setId = armorManager.getArmorSetId(result);
        if (setId == null) {
            return;
        }

        player.sendMessage(ChatColor.GREEN + "Crafted custom armor piece!");
        
        // Award Carpentry XP
        plugin.getSkyblockLevelManager().recordCarpentryCraft(player, result);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (event.getPlayer() != null) {
            lastWarnAtMs.remove(event.getPlayer().getUniqueId());
        }
    }

    public void registerRecipe(String setId, CustomArmorManager.ArmorPieceType pieceType, ItemStack result, String[] shape,
                               Map<Character, ItemStack> ingredients, String recipeName,
                               String collectionId, int collectionTierRequired) {
        NamespacedKey key = new NamespacedKey(plugin, "armor_" + setId.toLowerCase() + "_" + pieceType.name().toLowerCase());
        // Replace existing armor recipe with the same key without wiping global recipes.
        Bukkit.removeRecipe(key);
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(shape);

        for (Map.Entry<Character, ItemStack> entry : ingredients.entrySet()) {
            ItemStack ingredient = entry.getValue();
            if (ingredient == null) {
                continue;
            }
            if (ingredient.hasItemMeta()) {
                recipe.setIngredient(entry.getKey(), new RecipeChoice.ExactChoice(ingredient));
            } else {
                recipe.setIngredient(entry.getKey(), ingredient.getType());
            }
        }

        plugin.getServer().addRecipe(recipe);
        recipeToSetMap.put(key, setId);
        armorRecipeKeys.add(key);
        armorRecipeDefinitions.put(key, new ArmorRecipeDefinition(
                key,
                recipeName == null || recipeName.isBlank() ? pieceType.name() : recipeName,
                collectionId,
                Math.max(0, collectionTierRequired)
        ));
    }

    public void registerRecipes() {
        // Remove previously registered armor recipes while preserving vanilla and other custom recipes.
        armorRecipeKeys.forEach(Bukkit::removeRecipe);
        armorRecipeKeys.clear();
        armorRecipeDefinitions.clear();

        // Register custom armor recipes from config
        for (Map.Entry<String, CustomArmorManager.CustomArmorSet> entry : armorManager.getArmorSets().entrySet()) {
            String setId = entry.getKey();
            CustomArmorManager.CustomArmorSet armorSet = entry.getValue();

            registerArmorSetRecipes(setId, armorSet);
        }
    }

    private void registerArmorSetRecipes(String setId, CustomArmorManager.CustomArmorSet armorSet) {
        String collectionId = plugin.getConfig().getString("custom-armor.sets." + setId + ".collection-id");
        int collectionTierRequired = Math.max(0, plugin.getConfig().getInt("custom-armor.sets." + setId + ".collection-tier-required", 0));

        for (Map.Entry<CustomArmorManager.ArmorPieceType, CustomArmorManager.ArmorPieceConfig> piece : armorSet.getPieces().entrySet()) {
            CustomArmorManager.ArmorPieceType pieceType = piece.getKey();
            CustomArmorManager.ArmorPieceConfig config = piece.getValue();

            ItemStack result = armorManager.createArmorPiece(setId, pieceType);
            if (result == null) {
                continue;
            }

            // Get crafting material from config
            String materialPath = "custom-armor.sets." + setId + ".crafting-material";
            String materialName = plugin.getConfig().getString(materialPath, "DIAMOND");
            ItemStack craftStack;
            if (materialName.toLowerCase().startsWith("custom:") && customItemService != null) {
                String key = materialName.substring("custom:".length());
                craftStack = customItemService.createItemByKey(key);
            } else {
                Material craftMaterial = Material.getMaterial(materialName);
                if (craftMaterial == null) {
                    craftMaterial = Material.DIAMOND;
                }
                craftStack = new ItemStack(craftMaterial);
            }
            if (craftStack == null) {
                craftStack = new ItemStack(Material.DIAMOND);
            }

            // Create recipe based on piece type
            String recipeName = displayName(result, config, pieceType);
            switch (pieceType) {
                case HELMET -> {
                    String[] shape = new String[]{"MMM", "M M", "   "};
                    Map<Character, ItemStack> ingredients = new HashMap<>();
                    ingredients.put('M', craftStack);
                    registerRecipe(setId, pieceType, result, shape, ingredients, recipeName, collectionId, collectionTierRequired);
                }
                case CHESTPLATE -> {
                    String[] shape = new String[]{"M M", "MMM", "MMM"};
                    Map<Character, ItemStack> ingredients = new HashMap<>();
                    ingredients.put('M', craftStack);
                    registerRecipe(setId, pieceType, result, shape, ingredients, recipeName, collectionId, collectionTierRequired);
                }
                case LEGGINGS -> {
                    String[] shape = new String[]{"MMM", "M M", "M M"};
                    Map<Character, ItemStack> ingredients = new HashMap<>();
                    ingredients.put('M', craftStack);
                    registerRecipe(setId, pieceType, result, shape, ingredients, recipeName, collectionId, collectionTierRequired);
                }
                case BOOTS -> {
                    String[] shape = new String[]{"M M", "M M", "   "};
                    Map<Character, ItemStack> ingredients = new HashMap<>();
                    ingredients.put('M', craftStack);
                    registerRecipe(setId, pieceType, result, shape, ingredients, recipeName, collectionId, collectionTierRequired);
                }
            }
        }
    }

    public int getRegisteredArmorRecipeCount() {
        return armorRecipeKeys.size();
    }

    public List<ArmorRecipeDefinition> getRegisteredRecipeDefinitions() {
        return List.copyOf(new ArrayList<>(armorRecipeDefinitions.values()));
    }

    private ArmorRecipeDefinition recipeDefinition(Recipe recipe) {
        if (!(recipe instanceof Keyed keyed)) {
            return null;
        }
        NamespacedKey key = keyed.getKey();
        if (key == null) {
            return null;
        }
        return armorRecipeDefinitions.get(key);
    }

    private boolean meetsCollectionRequirement(Player player, ArmorRecipeDefinition recipeDefinition) {
        if (player == null || recipeDefinition == null) {
            return true;
        }
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return true;
        }

        String collectionId = recipeDefinition.collectionId();
        int requiredTier = recipeDefinition.collectionTierRequired();
        if (collectionId == null || collectionId.isBlank() || requiredTier <= 0) {
            return true;
        }

        CollectionsManager collectionsManager = plugin.getCollectionsManager();
        if (collectionsManager == null || !collectionsManager.isEnabled()) {
            return true;
        }

        PlayerCollectionProgress progress = collectionsManager.getPlayerProgress(player, collectionId);
        return progress != null && progress.isTierUnlocked(requiredTier);
    }

    private void warnLocked(Player player, ArmorRecipeDefinition recipeDefinition) {
        if (player == null || recipeDefinition == null) {
            return;
        }

        String collectionId = recipeDefinition.collectionId();
        int requiredTier = recipeDefinition.collectionTierRequired();
        if (collectionId == null || collectionId.isBlank() || requiredTier <= 0) {
            return;
        }

        long now = System.currentTimeMillis();
        Long last = lastWarnAtMs.get(player.getUniqueId());
        if (last != null && (now - last) < WARN_THROTTLE_MS) {
            return;
        }
        lastWarnAtMs.put(player.getUniqueId(), now);

        player.sendMessage(ChatColor.RED + "Recipe locked. " + ChatColor.GRAY + "Requires " +
                ChatColor.YELLOW + collectionId + ChatColor.GRAY + " Collection Tier " +
                ChatColor.AQUA + CollectionTier.toRoman(requiredTier) + ChatColor.GRAY + ".");
    }

    private String displayName(ItemStack result, CustomArmorManager.ArmorPieceConfig config, CustomArmorManager.ArmorPieceType pieceType) {
        if (result != null && result.hasItemMeta() && result.getItemMeta().hasDisplayName()) {
            String stripped = ChatColor.stripColor(result.getItemMeta().getDisplayName());
            if (stripped != null && !stripped.isBlank()) {
                return stripped;
            }
        }
        if (config != null && config.getDisplayName() != null && !config.getDisplayName().isBlank()) {
            String stripped = ChatColor.stripColor(config.getDisplayName());
            if (stripped != null && !stripped.isBlank()) {
                return stripped;
            }
        }
        return pieceType == null ? "Armor Piece" : pieceType.name();
    }

    public record ArmorRecipeDefinition(
            NamespacedKey key,
            String name,
            String collectionId,
            int collectionTierRequired
    ) {
    }
}
