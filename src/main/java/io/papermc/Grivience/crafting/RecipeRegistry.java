package io.papermc.Grivience.crafting;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.Recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages the registration and lookup of custom Skyblock-style recipes.
 */
public class RecipeRegistry {

    private static final Map<NamespacedKey, SkyblockRecipe> RECIPES = new ConcurrentHashMap<>();
    private static JavaPlugin plugin; // Store plugin instance for NamespacedKey creation

    /**
     * Initializes the RecipeRegistry. Must be called once on plugin startup.
     *
     * @param plugin The main plugin instance.
     */
    public static void init(JavaPlugin plugin) {
        RecipeRegistry.plugin = plugin;
        // Clear existing recipes from Bukkit and our registry on re-initialization
        RECIPES.values().forEach(recipe -> Bukkit.removeRecipe(recipe.getKey()));
        RECIPES.clear();

        // --- Custom Material Recipes ---
        // Verdant Fiber (Shapeless)
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "verdant_fiber"))
                .name("Verdant Fiber")
                .result(new ItemStack(Material.STRING, 4)) // Represents custom:verdant_fiber
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPELESS)
                .ingredient('W', new ItemStack(Material.WHEAT))
                .ingredient('C', new ItemStack(Material.CARROT))
                .ingredient('P', new ItemStack(Material.POTATO))
                .collectionId("WHEAT")
                .collectionTierRequired(1)
                .requiredSkyblockLevel(0) // Set to 0 to remove Skyblock Level requirement
                .build());

        // Abyssal Scale (Shapeless)
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "abyssal_scale"))
                .name("Abyssal Scale")
                .result(new ItemStack(Material.PRISMARINE_SHARD, 1)) // Represents custom:abyssal_scale
                .category(RecipeCategory.FISHING)
                .shape(RecipeShape.SHAPELESS)
                .ingredient('D', new ItemStack(Material.COD))
                .ingredient('S', new ItemStack(Material.SALMON))
                .ingredient('T', new ItemStack(Material.TROPICAL_FISH))
                .ingredient('F', new ItemStack(Material.PUFFERFISH))
                .collectionId("COD")
                .collectionTierRequired(1)
                .requiredSkyblockLevel(0) // Set to 0 to remove Skyblock Level requirement
                .build());

        // Dungeon Relic Shard (Shaped 2x2)
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "dungeon_relic_shard"))
                .name("Dungeon Relic Shard")
                .result(new ItemStack(Material.NETHERITE_SCRAP, 1)) // Represents custom:dungeon_relic_shard
                .category(RecipeCategory.SLAYER)
                .shape(RecipeShape.SHAPED_2X2)
                .shapeLines("GI", "LR")
                .ingredient('G', new ItemStack(Material.GOLD_INGOT))
                .ingredient('I', new ItemStack(Material.IRON_INGOT))
                .ingredient('L', new ItemStack(Material.LAPIS_LAZULI))
                .ingredient('R', new ItemStack(Material.REDSTONE))
                .collectionId("IRON_INGOT")
                .collectionTierRequired(1)
                .requiredSkyblockLevel(0) // Set to 0 to remove Skyblock Level requirement
                .build());

        // Mana Infused Fabric (Shaped 3x3)
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "mana_infused_fabric"))
                .name("Mana Infused Fabric")
                .result(new ItemStack(Material.PURPUR_BLOCK, 1)) // Represents custom:mana_infused_fabric
                .category(RecipeCategory.ENCHANTING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("GSG", "SES", "GSG")
                .ingredient('G', new ItemStack(Material.GLOWSTONE_DUST))
                .ingredient('S', new ItemStack(Material.STRING))
                .ingredient('E', new ItemStack(Material.ENDER_PEARL))
                // Pattern: G S G, S E S, G S G
                .collectionId("ENDER_PEARL")
                .collectionTierRequired(1)
                .requiredSkyblockLevel(0) // Set to 0 to remove Skyblock Level requirement
                .build());


        // --- Armor Set Recipes ---
        // Harvester's Embrace (Farming) - Crafting Material: Verdant Fiber (Material.STRING)
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "harvester_helmet"))
                .name("Harvester's Visage")
                .result(new ItemStack(Material.LEATHER_HELMET))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("AAA", "A A", "   ")
                .ingredient('A', new ItemStack(Material.STRING)) // Verdant Fiber
                .collectionId("WHEAT")
                .collectionTierRequired(2)
                .requiredSkyblockLevel(0) // Set to 0 to remove Skyblock Level requirement
                .build());
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "harvester_chestplate"))
                .name("Harvester's Tunic")
                .result(new ItemStack(Material.LEATHER_CHESTPLATE))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("A A", "AAA", "AAA")
                .ingredient('A', new ItemStack(Material.STRING)) // Verdant Fiber
                .collectionId("WHEAT")
                .collectionTierRequired(2)
                .requiredSkyblockLevel(0) // Set to 0 to remove Skyblock Level requirement
                .build());
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "harvester_leggings"))
                .name("Harvester's Breeches")
                .result(new ItemStack(Material.LEATHER_LEGGINGS))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("AAA", "A A", "A A")
                .ingredient('A', new ItemStack(Material.STRING)) // Verdant Fiber
                .collectionId("WHEAT")
                .collectionTierRequired(2)
                .requiredSkyblockLevel(0) // Set to 0 to remove Skyblock Level requirement
                .build());
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "harvester_boots"))
                .name("Harvester's Striders")
                .result(new ItemStack(Material.LEATHER_BOOTS))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("   ", "A A", "A A")
                .ingredient('A', new ItemStack(Material.STRING)) // Verdant Fiber
                .collectionId("WHEAT")
                .collectionTierRequired(2)
                .requiredSkyblockLevel(0) // Set to 0 to remove Skyblock Level requirement
                .build());

        // Abyssal Diver (Fishing) - Crafting Material: Abyssal Scale (Material.PRISMARINE_SHARD)
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "abyssal_helmet"))
                .name("Abyssal Helmet")
                .result(new ItemStack(Material.IRON_HELMET))
                .category(RecipeCategory.FISHING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("AAA", "A A", "   ")
                .ingredient('A', new ItemStack(Material.PRISMARINE_SHARD)) // Abyssal Scale
                .collectionId("COD")
                .collectionTierRequired(2)
                .requiredSkyblockLevel(0) // Set to 0 to remove Skyblock Level requirement
                .build());
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "abyssal_chestplate"))
                .name("Abyssal Chestplate")
                .result(new ItemStack(Material.IRON_CHESTPLATE))
                .category(RecipeCategory.FISHING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("A A", "AAA", "AAA")
                .ingredient('A', new ItemStack(Material.PRISMARINE_SHARD)) // Abyssal Scale
                .collectionId("COD")
                .collectionTierRequired(2)
                .requiredSkyblockLevel(0) // Set to 0 to remove Skyblock Level requirement
                .build());
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "abyssal_leggings"))
                .name("Abyssal Leggings")
                .result(new ItemStack(Material.IRON_LEGGINGS))
                .category(RecipeCategory.FISHING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("AAA", "A A", "A A")
                .ingredient('A', new ItemStack(Material.PRISMARINE_SHARD)) // Abyssal Scale
                .collectionId("COD")
                .collectionTierRequired(2)
                .requiredSkyblockLevel(0) // Set to 0 to remove Skyblock Level requirement
                .build());
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "abyssal_boots"))
                .name("Abyssal Boots")
                .result(new ItemStack(Material.IRON_BOOTS))
                .category(RecipeCategory.FISHING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("   ", "A A", "A A")
                .ingredient('A', new ItemStack(Material.PRISMARINE_SHARD)) // Abyssal Scale
                .collectionId("COD")
                .collectionTierRequired(2)
                .requiredSkyblockLevel(0) // Set to 0 to remove Skyblock Level requirement
                .build());

        // Cryptic Conqueror (Dungeon) - Crafting Material: Dungeon Relic Shard (Material.NETHERITE_SCRAP)
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "cryptic_helmet"))
                .name("Cryptic Helmet")
                .result(new ItemStack(Material.GOLDEN_HELMET))
                .category(RecipeCategory.SLAYER)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("AAA", "A A", "   ")
                .ingredient('A', new ItemStack(Material.NETHERITE_SCRAP)) // Dungeon Relic Shard
                .collectionId("IRON_INGOT")
                .collectionTierRequired(2)
                .requiredSkyblockLevel(0) // Set to 0 to remove Skyblock Level requirement
                .build());
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "cryptic_chestplate"))
                .name("Cryptic Chestplate")
                .result(new ItemStack(Material.GOLDEN_CHESTPLATE))
                .category(RecipeCategory.SLAYER)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("A A", "AAA", "AAA")
                .ingredient('A', new ItemStack(Material.NETHERITE_SCRAP)) // Dungeon Relic Shard
                .collectionId("IRON_INGOT")
                .collectionTierRequired(2)
                .requiredSkyblockLevel(0) // Set to 0 to remove Skyblock Level requirement
                .build());
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "cryptic_leggings"))
                .name("Cryptic Leggings")
                .result(new ItemStack(Material.GOLDEN_LEGGINGS))
                .category(RecipeCategory.SLAYER)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("AAA", "A A", "A A")
                .ingredient('A', new ItemStack(Material.NETHERITE_SCRAP)) // Dungeon Relic Shard
                .collectionId("IRON_INGOT")
                .collectionTierRequired(2)
                .requiredSkyblockLevel(0) // Set to 0 to remove Skyblock Level requirement
                .build());
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "cryptic_boots"))
                .name("Cryptic Boots")
                .result(new ItemStack(Material.GOLDEN_BOOTS))
                .category(RecipeCategory.SLAYER)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("   ", "A A", "A A")
                .ingredient('A', new ItemStack(Material.NETHERITE_SCRAP)) // Dungeon Relic Shard
                .collectionId("IRON_INGOT")
                .collectionTierRequired(2)
                .requiredSkyblockLevel(0) // Set to 0 to remove Skyblock Level requirement
                .build());

        // Arcane Weaver (Mage) - Crafting Material: Mana Infused Fabric (Material.PURPUR_BLOCK)
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "arcane_hood"))
                .name("Arcane Hood")
                .result(new ItemStack(Material.LEATHER_HELMET))
                .category(RecipeCategory.ENCHANTING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("AAA", "A A", "   ")
                .ingredient('A', new ItemStack(Material.PURPUR_BLOCK)) // Mana Infused Fabric
                .collectionId("ENDER_PEARL")
                .collectionTierRequired(2)
                .requiredSkyblockLevel(0) // Set to 0 to remove Skyblock Level requirement
                .build());
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "arcane_robes"))
                .name("Arcane Robes")
                .result(new ItemStack(Material.LEATHER_CHESTPLATE))
                .category(RecipeCategory.ENCHANTING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("A A", "AAA", "AAA")
                .ingredient('A', new ItemStack(Material.PURPUR_BLOCK)) // Mana Infused Fabric
                .collectionId("ENDER_PEARL")
                .collectionTierRequired(2)
                .requiredSkyblockLevel(0) // Set to 0 to remove Skyblock Level requirement
                .build());
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "arcane_trousers"))
                .name("Arcane Trousers")
                .result(new ItemStack(Material.LEATHER_LEGGINGS))
                .category(RecipeCategory.ENCHANTING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("AAA", "A A", "A A")
                .ingredient('A', new ItemStack(Material.PURPUR_BLOCK)) // Mana Infused Fabric
                .collectionId("ENDER_PEARL")
                .collectionTierRequired(2)
                .requiredSkyblockLevel(0) // Set to 0 to remove Skyblock Level requirement
                .build());
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "arcane_boots"))
                .name("Arcane Boots")
                .result(new ItemStack(Material.LEATHER_BOOTS))
                .category(RecipeCategory.ENCHANTING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("   ", "A A", "A A")
                .ingredient('A', new ItemStack(Material.PURPUR_BLOCK)) // Mana Infused Fabric
                .collectionId("ENDER_PEARL")
                .collectionTierRequired(2)
                .requiredSkyblockLevel(0) // Set to 0 to remove Skyblock Level requirement
                .build());


        // --- Previous placeholder recipes with updated unlock conditions ---
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "enchanted_diamond_block"))
                .name("Enchanted Diamond Block")
                .result(new ItemStack(Material.DIAMOND_BLOCK))
                .category(RecipeCategory.MINING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("DDD", "DDD", "DDD")
                .ingredient('D', new ItemStack(Material.DIAMOND))
                .collectionId("DIAMOND")
                .collectionTierRequired(3) // Example
                .requiredSkyblockLevel(0) // Set to 0 to remove Skyblock Level requirement
                .build());
    }

    /**
     * Registers a new custom recipe with both our registry and Bukkit's recipe manager.
     *
     * @param skyblockRecipe The SkyblockRecipe to register.
     */
    public static void register(SkyblockRecipe skyblockRecipe) {
        if (RECIPES.containsKey(skyblockRecipe.getKey())) {
            plugin.getLogger().warning("Attempted to register duplicate recipe key: " + skyblockRecipe.getKey());
            return;
        }

        // Convert SkyblockRecipe to Bukkit Recipe and add to Bukkit
        Recipe bukkitRecipe = toBukkitRecipe(skyblockRecipe);
        if (bukkitRecipe != null) {
            Bukkit.addRecipe(bukkitRecipe);
            RECIPES.put(skyblockRecipe.getKey(), skyblockRecipe);
            plugin.getLogger().info("Registered recipe: " + skyblockRecipe.getName() + " (" + skyblockRecipe.getKey() + ")");
        } else {
            plugin.getLogger().warning("Failed to convert SkyblockRecipe to Bukkit Recipe for: " + skyblockRecipe.getName());
        }
    }

    /**
     * Converts a SkyblockRecipe into a Bukkit Recipe (ShapedRecipe or ShapelessRecipe).
     *
     * @param skyblockRecipe The custom SkyblockRecipe.
     * @return A Bukkit Recipe, or null if the shape is not supported.
     */
    private static Recipe toBukkitRecipe(SkyblockRecipe skyblockRecipe) {
        if (plugin == null) {
            // This should ideally not happen if init() is called correctly
            System.err.println("RecipeRegistry not initialized with plugin instance!");
            return null;
        }

        switch (skyblockRecipe.getShape()) {
            case SHAPED_3X3:
            case SHAPED_2X2:
                ShapedRecipe shapedRecipe = new ShapedRecipe(skyblockRecipe.getKey(), skyblockRecipe.getResult());

                String[] pattern = skyblockRecipe.getShapePattern();
                if (pattern != null && pattern.length > 0) {
                    shapedRecipe.shape(pattern);
                } else {
                    // Fallback to a default shape if none is provided
                    if (skyblockRecipe.getShape() == RecipeShape.SHAPED_2X2) {
                        shapedRecipe.shape("AB", "CD");
                    } else {
                        shapedRecipe.shape("ABC", "DEF", "GHI");
                    }
                }

                // Map ingredients based on their characters
                for (Map.Entry<Character, ItemStack> entry : skyblockRecipe.getIngredients().entrySet()) {
                    char ingredientChar = entry.getKey();
                    // Check if the symbol exists in the shape pattern
                    boolean found = false;
                    for (String row : shapedRecipe.getShape()) {
                        if (row.indexOf(ingredientChar) != -1) {
                            found = true;
                            break;
                        }
                    }

                    if (found) {
                        shapedRecipe.setIngredient(ingredientChar, entry.getValue().getType());
                    } else {
                        plugin.getLogger().warning("Symbol '" + ingredientChar + "' not found in shape for recipe: " + skyblockRecipe.getName());
                    }
                }
                return shapedRecipe;
            case SHAPELESS:
                ShapelessRecipe shapelessRecipe = new ShapelessRecipe(skyblockRecipe.getKey(), skyblockRecipe.getResult());
                for (Map.Entry<Character, ItemStack> entry : skyblockRecipe.getIngredients().entrySet()) {
                    // For shapeless, we just add the material type. Amount is handled by Bukkit.
                    shapelessRecipe.addIngredient(entry.getValue().getType());
                }
                return shapelessRecipe;
            case SPECIAL:
                // Special recipes are not directly handled by Bukkit's standard Recipe types.
                // These would require custom processing elsewhere, possibly through a custom crafting table
                // or specific event handlers.
                plugin.getLogger().warning("Special SkyblockRecipe cannot be converted to standard Bukkit Recipe: " + skyblockRecipe.getName());
                return null;
            default:
                return null;
        }
    }

    /**
     * Retrieves a custom recipe by its NamespacedKey.
     *
     * @param key The NamespacedKey of the recipe.
     * @return An Optional containing the SkyblockRecipe if found, otherwise empty.
     */
    public static Optional<SkyblockRecipe> getByKey(NamespacedKey key) {
        return Optional.ofNullable(RECIPES.get(key));
    }

    /**
     * Retrieves all registered custom recipes.
     *
     * @return An unmodifiable list of all SkyblockRecipes.
     */
    public static List<SkyblockRecipe> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(RECIPES.values()));
    }

    /**
     * Counts the number of registered recipes.
     *
     * @return The total number of registered recipes.
     */
    public static int count() {
        return RECIPES.size();
    }

    /**
     * Searches for recipes by name, case-insensitively.
     *
     * @param query The search query.
     * @return A list of matching recipes.
     */
    public static List<SkyblockRecipe> search(String query) {
        List<SkyblockRecipe> results = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase();
        for (SkyblockRecipe recipe : RECIPES.values()) {
            if (recipe.getName().toLowerCase().contains(lowerCaseQuery)) {
                results.add(recipe);
            }
        }
        return results;
    }

    /**
     * Retrieves recipes by category.
     *
     * @param category The category to filter by.
     * @return A list of recipes in the specified category.
     */
    public static List<SkyblockRecipe> getByCategory(RecipeCategory category) {
        return RECIPES.values().stream()
                .filter(recipe -> recipe.getCategory() == category)
                .collect(Collectors.toList());
    }
}
