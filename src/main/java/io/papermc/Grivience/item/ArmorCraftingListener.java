package io.papermc.Grivience.item;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public final class ArmorCraftingListener implements Listener {
    private final GriviencePlugin plugin;
    private final CustomArmorManager armorManager;
    private final CustomItemService customItemService;
    private final Map<NamespacedKey, String> recipeToSetMap = new HashMap<>();
    private final Set<NamespacedKey> armorRecipeKeys = new HashSet<>();

    public ArmorCraftingListener(GriviencePlugin plugin, CustomArmorManager armorManager, CustomItemService customItemService) {
        this.plugin = plugin;
        this.armorManager = armorManager;
        this.customItemService = customItemService;
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
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

        // Verify the player has the required custom materials
        if (!hasRequiredMaterials(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You need special materials to craft this armor!");
            return;
        }

        player.sendMessage(ChatColor.GREEN + "Crafted custom armor piece!");
    }

    private boolean hasRequiredMaterials(Player player) {
        // Check if player has custom crafting materials in inventory
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) {
                continue;
            }
            if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer()
                    .has(new NamespacedKey(plugin, "custom_material"), PersistentDataType.STRING)) {
                return true;
            }
        }
        return false;
    }

    public void registerRecipe(String setId, CustomArmorManager.ArmorPieceType pieceType, ItemStack result, String[] shape,
                               Map<Character, ItemStack> ingredients) {
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
    }

    public void registerRecipes() {
        // Remove previously registered armor recipes while preserving vanilla and other custom recipes.
        armorRecipeKeys.forEach(Bukkit::removeRecipe);
        armorRecipeKeys.clear();

        // Register custom armor recipes from config
        for (Map.Entry<String, CustomArmorManager.CustomArmorSet> entry : armorManager.getArmorSets().entrySet()) {
            String setId = entry.getKey();
            CustomArmorManager.CustomArmorSet armorSet = entry.getValue();

            registerArmorSetRecipes(setId, armorSet);
        }
    }

    private void registerArmorSetRecipes(String setId, CustomArmorManager.CustomArmorSet armorSet) {
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
            switch (pieceType) {
                case HELMET -> {
                    String[] shape = new String[]{"MMM", "M M", "   "};
                    Map<Character, ItemStack> ingredients = new HashMap<>();
                    ingredients.put('M', craftStack);
                    registerRecipe(setId, pieceType, result, shape, ingredients);
                }
                case CHESTPLATE -> {
                    String[] shape = new String[]{"M M", "MMM", "MMM"};
                    Map<Character, ItemStack> ingredients = new HashMap<>();
                    ingredients.put('M', craftStack);
                    registerRecipe(setId, pieceType, result, shape, ingredients);
                }
                case LEGGINGS -> {
                    String[] shape = new String[]{"MMM", "M M", "M M"};
                    Map<Character, ItemStack> ingredients = new HashMap<>();
                    ingredients.put('M', craftStack);
                    registerRecipe(setId, pieceType, result, shape, ingredients);
                }
                case BOOTS -> {
                    String[] shape = new String[]{"M M", "M M", "   "};
                    Map<Character, ItemStack> ingredients = new HashMap<>();
                    ingredients.put('M', craftStack);
                    registerRecipe(setId, pieceType, result, shape, ingredients);
                }
            }
        }
    }

    public int getRegisteredArmorRecipeCount() {
        return armorRecipeKeys.size();
    }
}
