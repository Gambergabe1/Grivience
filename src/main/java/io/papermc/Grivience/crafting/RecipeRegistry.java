package io.papermc.Grivience.crafting;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
                .result(createArmorPiece(Material.LEATHER_HELMET, ChatColor.GREEN + "Harvester's Visage"))
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
                .result(createArmorPiece(Material.LEATHER_CHESTPLATE, ChatColor.GREEN + "Harvester's Tunic"))
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
                .result(createArmorPiece(Material.LEATHER_LEGGINGS, ChatColor.GREEN + "Harvester's Breeches"))
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
                .result(createArmorPiece(Material.LEATHER_BOOTS, ChatColor.GREEN + "Harvester's Striders"))
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
        // Arcane Weaver (Mage) - Crafting Material: Mana Infused Fabric (Material.PURPUR_BLOCK)
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

        // Refined Titanium Block (Shaped 3x3)
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "enchanted_titanium_block"))
                .name("Refined Titanium Block")
                .result(new ItemStack(Material.DIAMOND_BLOCK, 1)) // Represents custom:enchanted_titanium_block
                .category(RecipeCategory.MINING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("RRR", "RRR", "RRR")
                .ingredient('R', new ItemStack(Material.PLAYER_HEAD)) // Refined Titanium
                .collectionId("TITANIUM")
                .collectionTierRequired(8)
                .requiredSkyblockLevel(0)
                .build());

        // --- Personal Compactor Recipes (Collection Discovery) ---

        // Personal Compactor 3000
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "personal_compactor_3000_recipe"))
                .name("Personal Compactor 3000")
                .result(new ItemStack(Material.DROPPER))
                .category(RecipeCategory.MINING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("CCC", "CRC", "CCC")
                .ingredient('C', new ItemStack(Material.COBBLESTONE)) // Enchanted Cobblestone
                .ingredient('R', new ItemStack(Material.REDSTONE)) // Enchanted Redstone
                .collectionId("cobblestone")
                .collectionTierRequired(4)
                .requiredSkyblockLevel(0)
                .build());

        // Personal Compactor 4000
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "personal_compactor_4000_recipe"))
                .name("Personal Compactor 4000")
                .result(new ItemStack(Material.DROPPER))
                .category(RecipeCategory.MINING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("RRR", "R R", "RRR")
                .ingredient('R', new ItemStack(Material.REDSTONE)) // Enchanted Redstone
                .collectionId("redstone")
                .collectionTierRequired(7)
                .requiredSkyblockLevel(0)
                .build());

        // Personal Compactor 5000
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "personal_compactor_5000_recipe"))
                .name("Personal Compactor 5000")
                .result(new ItemStack(Material.DROPPER))
                .category(RecipeCategory.MINING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("BBB", "BMB", "BBB")
                .ingredient('B', new ItemStack(Material.REDSTONE_BLOCK)) // Enchanted Redstone Block
                .ingredient('M', new ItemStack(Material.DROPPER)) // Personal Compactor 4000
                .collectionId("redstone")
                .collectionTierRequired(9)
                .requiredSkyblockLevel(0)
                .build());

        // Personal Compactor 6000
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "personal_compactor_6000_recipe"))
                .name("Personal Compactor 6000")
                .result(new ItemStack(Material.DROPPER))
                .category(RecipeCategory.MINING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("BBB", "BMB", "BBB")
                .ingredient('B', new ItemStack(Material.REDSTONE_BLOCK)) // Enchanted Redstone Block
                .ingredient('M', new ItemStack(Material.DROPPER)) // Personal Compactor 5000
                .collectionId("redstone")
                .collectionTierRequired(11)
                .requiredSkyblockLevel(0)
                .build());

        // Personal Compactor 7000
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "personal_compactor_7000_recipe"))
                .name("Personal Compactor 7000")
                .result(new ItemStack(Material.DROPPER))
                .category(RecipeCategory.MINING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("BBB", "BMB", "BBB")
                .ingredient('B', new ItemStack(Material.REDSTONE_BLOCK)) // Enchanted Redstone Block
                .ingredient('M', new ItemStack(Material.DROPPER)) // Personal Compactor 6000
                .collectionId("redstone")
                .collectionTierRequired(13)
                .requiredSkyblockLevel(0)
                .build());

        .collectionId("WHEAT")
        .collectionTierRequired(3)
        .requiredSkyblockLevel(0)
        .build());

        register(SkyblockRecipe.builder()
        .key(new NamespacedKey(plugin, "euclids_wheat_hoe"))
        .name("Euclid's Wheat Hoe")
        .result(((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.EUCLIDS_WHEAT_HOE, null))
        .category(RecipeCategory.FARMING)
        .shape(RecipeShape.SHAPED_3X3)
        .shapeLines("EE ", " B ", "   ")
        .ingredient('E', new ItemStack(Material.HAY_BLOCK)) // Enchanted Hay Bale
        .ingredient('B', ((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
        .collectionId("WHEAT")
        .collectionTierRequired(5)
        .requiredSkyblockLevel(0)
        .build());

        register(SkyblockRecipe.builder()
        .key(new NamespacedKey(plugin, "gauss_carrot_hoe"))
        .name("Gauss Carrot Hoe")
        .result(((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.GAUSS_CARROT_HOE, null))
        .category(RecipeCategory.FARMING)
        .shape(RecipeShape.SHAPED_3X3)
        .shapeLines("EE ", " B ", "   ")
        .ingredient('E', new ItemStack(Material.GOLDEN_CARROT)) // Enchanted Carrot
        .ingredient('B', ((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
        .collectionId("CARROT")
        .collectionTierRequired(5)
        .requiredSkyblockLevel(0)
        .build());

        register(SkyblockRecipe.builder()
        .key(new NamespacedKey(plugin, "pythagorean_potato_hoe"))
        .name("Pythagorean Potato Hoe")
        .result(((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.PYTHAGOREAN_POTATO_HOE, null))
        .category(RecipeCategory.FARMING)
        .shape(RecipeShape.SHAPED_3X3)
        .shapeLines("EE ", " B ", "   ")
        .ingredient('E', new ItemStack(Material.BAKED_POTATO)) // Enchanted Potato
        .ingredient('B', ((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
        .collectionId("POTATO")
        .collectionTierRequired(5)
        .requiredSkyblockLevel(0)
        .build());

        register(SkyblockRecipe.builder()
        .key(new NamespacedKey(plugin, "turing_sugar_cane_hoe"))
        .name("Turing Sugar Cane Hoe")
        .result(((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.TURING_SUGAR_CANE_HOE, null))
        .category(RecipeCategory.FARMING)
        .shape(RecipeShape.SHAPED_3X3)
        .shapeLines("EE ", " B ", "   ")
        .ingredient('E', new ItemStack(Material.PAPER)) // Enchanted Sugarcane
        .ingredient('B', ((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
        .collectionId("SUGAR_CANE")
        .collectionTierRequired(5)
        .requiredSkyblockLevel(0)
        .build());

        register(SkyblockRecipe.builder()
        .key(new NamespacedKey(plugin, "newton_nether_warts_hoe"))
        .name("Newton Nether Warts Hoe")
        .result(((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.NEWTON_NETHER_WARTS_HOE, null))
        .category(RecipeCategory.FARMING)
        .shape(RecipeShape.SHAPED_3X3)
        .shapeLines("EE ", " B ", "   ")
        .ingredient('E', new ItemStack(Material.NETHER_WART_BLOCK)) // Enchanted Nether Wart
        .ingredient('B', ((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
        .collectionId("NETHER_WART")
        .collectionTierRequired(5)
        .requiredSkyblockLevel(0)
        .build());

        register(SkyblockRecipe.builder()
        .key(new NamespacedKey(plugin, "melon_dicer"))
        .name("Melon Dicer")
        .result(((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MELON_DICER, null))
        .category(RecipeCategory.FARMING)
        .shape(RecipeShape.SHAPED_3X3)
        .shapeLines("EE ", " B ", "   ")
        .ingredient('E', new ItemStack(Material.MELON)) // Enchanted Melon Block
        .ingredient('B', ((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
        .collectionId("MELON")
        .collectionTierRequired(5)
        .requiredSkyblockLevel(0)
        .build());

        register(SkyblockRecipe.builder()
        .key(new NamespacedKey(plugin, "pumpkin_dicer"))
        .name("Pumpkin Dicer")
        .result(((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.PUMPKIN_DICER, null))
        .category(RecipeCategory.FARMING)
        .shape(RecipeShape.SHAPED_3X3)
        .shapeLines("EE ", " B ", "   ")
        .ingredient('E', new ItemStack(Material.JACK_O_LANTERN)) // Enchanted Pumpkin
        .ingredient('B', ((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
        .collectionId("PUMPKIN")
        .collectionTierRequired(5)
        .requiredSkyblockLevel(0)
        .build());

        // --- Enchantment Recipes ---
        register(SkyblockEnchantment.builder("smelting_touch", "Smelting Touch")
                .type(EnchantmentType.RARE)
                .rarity(EnchantmentRarity.RARE)
                .maxLevel(3)
                .baseXpCost(35)
                .conflictsWith("fortune", "silk_touch")
                .icon(Material.FURNACE)
                .build());

        // Unbreaking
        register(SkyblockEnchantment.builder("unbreaking", "Unbreaking")
                .type(EnchantmentType.UNIQUE)
                .rarity(EnchantmentRarity.UNCOMMON)
                .maxLevel(3)
                .baseXpCost(12)
                .icon(Material.DIAMOND_SWORD)
                .build());

        // First Strike
        register(SkyblockEnchantment.builder("first_strike", "First Strike")
                .type(EnchantmentType.RARE)
                .rarity(EnchantmentRarity.RARE)
                .maxLevel(5)
                .baseXpCost(15)
                .conflictsWith("sharpness", "smite", "bane_of_arthropods", "power")
                .icon(Material.IRON_SWORD)
                .build());
        
        // Critical
        register(SkyblockEnchantment.builder("critical", "Critical")
                .type(EnchantmentType.RARE)
                .rarity(EnchantmentRarity.RARE)
                .maxLevel(4)
                .baseXpCost(25)
                .conflictsWith("sharpness", "smite", "bane_of_arthropods")
                .icon(Material.DIAMOND_SWORD)
                .build());

        // Aqua Affinity
        register(SkyblockEnchantment.builder("aqua_affinity", "Aqua Affinity")
                .type(EnchantmentType.UNCOMMON)
                .rarity(EnchantmentRarity.UNCOMMON)
                .maxLevel(1)
                .baseXpCost(10)
                .icon(Material.CONDUIT)
                .build());

        // Fortune
        register(SkyblockEnchantment.builder("fortune", "Fortune")
                .type(EnchantmentType.DIGGING)
                .rarity(EnchantmentRarity.UNCOMMON)
                .maxLevel(4)
                .baseXpCost(15)
                .icon(Material.DIAMOND_PICKAXE)
                .build());

        // Silk Touch
        register(SkyblockEnchantment.builder("silk_touch", "Silk Touch")
                .type(EnchantmentType.DIGGING)
                .rarity(EnchantmentRarity.RARE)
                .maxLevel(1)
                .baseXpCost(30)
                .icon(Material.DIAMOND_PICKAXE)
                .build());

        // Protection
        register(SkyblockEnchantment.builder("protection", "Protection")
                .type(EnchantmentType.ARMOR)
                .rarity(EnchantmentRarity.UNCOMMON)
                .maxLevel(4)
                .baseXpCost(10)
                .conflictsWith("fire_protection", "blast_protection", "projectile_protection", "thorns")
                .icon(Material.NETHERITE_CHESTPLATE)
                .build());
    }
        .type(EnchantmentType.RARE)
        .rarity(EnchantmentRarity.RARE)
        .maxLevel(3)
        .baseXpCost(35)
        .conflictsWith("fortune", "silk_touch")
        .icon(Material.FURNACE)
        .build());

        // Unbreaking
        register(SkyblockEnchantment.builder("unbreaking", "Unbreaking")
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "mathematical_hoe_blueprint"))
                .name("Mathematical Hoe Blueprint")
                .result(((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("GGG", "G G", "GGG")
                .ingredient('G', new ItemStack(Material.GOLD_BLOCK))
                .collectionId("WHEAT")
                .collectionTierRequired(3)
                .requiredSkyblockLevel(0)
                .build());

        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "euclids_wheat_hoe"))
                .name("Euclid's Wheat Hoe")
                .result(((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.EUCLIDS_WHEAT_HOE, null))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("EE ", " B ", "   ")
                .ingredient('E', new ItemStack(Material.HAY_BLOCK)) // Enchanted Hay Bale
                .ingredient('B', ((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
                .collectionId("WHEAT")
                .collectionTierRequired(5)
                .requiredSkyblockLevel(0)
                .build());

        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "gauss_carrot_hoe"))
                .name("Gauss Carrot Hoe")
                .result(((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.GAUSS_CARROT_HOE, null))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("EE ", " B ", "   ")
                .ingredient('E', new ItemStack(Material.GOLDEN_CARROT)) // Enchanted Carrot
                .ingredient('B', ((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
                .collectionId("CARROT")
                .collectionTierRequired(5)
                .requiredSkyblockLevel(0)
                .build());

        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "pythagorean_potato_hoe"))
                .name("Pythagorean Potato Hoe")
                .result(((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.PYTHAGOREAN_POTATO_HOE, null))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("EE ", " B ", "   ")
                .ingredient('E', new ItemStack(Material.BAKED_POTATO)) // Enchanted Potato
                .ingredient('B', ((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
                .collectionId("POTATO")
                .collectionTierRequired(5)
                .requiredSkyblockLevel(0)
                .build());

        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "turing_sugar_cane_hoe"))
                .name("Turing Sugar Cane Hoe")
                .result(((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.TURING_SUGAR_CANE_HOE, null))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("EE ", " B ", "   ")
                .ingredient('E', new ItemStack(Material.PAPER)) // Enchanted Sugarcane
                .ingredient('B', ((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
                .collectionId("SUGAR_CANE")
                .collectionTierRequired(5)
                .requiredSkyblockLevel(0)
                .build());

        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "newton_nether_warts_hoe"))
                .name("Newton Nether Warts Hoe")
                .result(((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.NEWTON_NETHER_WARTS_HOE, null))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("EE ", " B ", "   ")
                .ingredient('E', new ItemStack(Material.NETHER_WART_BLOCK)) // Enchanted Nether Wart
                .ingredient('B', ((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
                .collectionId("NETHER_WART")
                .collectionTierRequired(5)
                .requiredSkyblockLevel(0)
                .build());

        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "melon_dicer"))
                .name("Melon Dicer")
                .result(((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MELON_DICER, null))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("EE ", " B ", "   ")
                .ingredient('E', new ItemStack(Material.MELON)) // Enchanted Melon Block
                .ingredient('B', ((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
                .collectionId("MELON")
                .collectionTierRequired(5)
                .requiredSkyblockLevel(0)
                .build());

        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "pumpkin_dicer"))
                .name("Pumpkin Dicer")
                .result(((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.PUMPKIN_DICER, null))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("EE ", " B ", "   ")
                .ingredient('E', new ItemStack(Material.JACK_O_LANTERN)) // Enchanted Pumpkin
                .ingredient('B', ((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
                .collectionId("PUMPKIN")
                .collectionId("IRON_INGOT")
                .collectionTierRequired(2)
                .requiredSkyblockLevel(0) // Set to 0 to remove Skyblock Level requirement
                .build());

        // --- Custom Hoe Recipes ---
        // Mathematical Hoe Blueprint
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "mathematical_hoe_blueprint"))
                .name("Mathematical Hoe Blueprint")
                .result(((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("GGG", "G G", "GGG")
                .ingredient('G', new ItemStack(Material.GOLD_BLOCK))
                .collectionId("WHEAT")
                .collectionTierRequired(3)
                .requiredSkyblockLevel(0)
                .build());

        // Euclid's Wheat Hoe
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "euclids_wheat_hoe"))
                .name("Euclid's Wheat Hoe")
                .result(((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.EUCLIDS_WHEAT_HOE, null))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("EE ", " B ", "   ")
                .ingredient('E', new ItemStack(Material.HAY_BLOCK)) // Enchanted Hay Bale
                .ingredient('B', ((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
                .collectionId("WHEAT")
                .collectionTierRequired(5)
                .requiredSkyblockLevel(0)
                .build());

        // Gauss Carrot Hoe
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "gauss_carrot_hoe"))
                .name("Gauss Carrot Hoe")
                .result(((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.GAUSS_CARROT_HOE, null))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("EE ", " B ", "   ")
                .ingredient('E', new ItemStack(Material.GOLDEN_CARROT)) // Enchanted Carrot
                .ingredient('B', ((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
                .collectionId("CARROT")
                .collectionTierRequired(5)
                .requiredSkyblockLevel(0)
                .build());

        // Pythagorean Potato Hoe
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "pythagorean_potato_hoe"))
                .name("Pythagorean Potato Hoe")
                .result(((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.PYTHAGOREAN_POTATO_HOE, null))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("EE ", " B ", "   ")
                .ingredient('E', new ItemStack(Material.BAKED_POTATO)) // Enchanted Potato
                .ingredient('B', ((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
                .collectionId("POTATO")
                .collectionTierRequired(5)
                .requiredSkyblockLevel(0)
                .build());

        // Turing Sugar Cane Hoe
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "turing_sugar_cane_hoe"))
                .name("Turing Sugar Cane Hoe")
                .result(((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.TURING_SUGAR_CANE_HOE, null))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("EE ", " B ", "   ")
                .ingredient('E', new ItemStack(Material.PAPER)) // Enchanted Sugarcane
                .ingredient('B', ((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
                .collectionId("SUGAR_CANE")
                .collectionTierRequired(5)
                .requiredSkyblockLevel(0)
                .build());

        // Newton Nether Warts Hoe
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "newton_nether_warts_hoe"))
                .name("Newton Nether Warts Hoe")
                .result(((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.NEWTON_NETHER_WARTS_HOE, null))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("EE ", " B ", "   ")
                .ingredient('E', new ItemStack(Material.NETHER_WART_BLOCK)) // Enchanted Nether Wart
                .ingredient('B', ((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
                .collectionId("NETHER_WART")
                .collectionTierRequired(5)
                .requiredSkyblockLevel(0)
                .build());

        // Melon Dicer
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "melon_dicer"))
                .name("Melon Dicer")
                .result(((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MELON_DICER, null))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("EE ", " B ", "   ")
                .ingredient('E', new ItemStack(Material.MELON)) // Enchanted Melon Block
                .ingredient('B', ((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
                .collectionId("MELON")
                .collectionTierRequired(5)
                .requiredSkyblockLevel(0)
                .build());

        // Pumpkin Dicer
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "pumpkin_dicer"))
                .name("Pumpkin Dicer")
                .result(((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.PUMPKIN_DICER, null))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("EE ", " B ", "   ")
                .ingredient('E', new ItemStack(Material.JACK_O_LANTERN)) // Enchanted Pumpkin
                .ingredient('B', ((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
                .collectionId("PUMPKIN")
                .collectionTierRequired(5)
                .requiredSkyblockLevel(0)
                .build());

        register(SkyblockEnchantment.builder("smelting_touch", "Smelting Touch")
                .type(EnchantmentType.RARE)
                .rarity(EnchantmentRarity.RARE)
                .maxLevel(3)
                .baseXpCost(35)
                .conflictsWith("fortune", "silk_touch")
                .icon(Material.FURNACE)
                .build());

        // Unbreaking
        register(SkyblockEnchantment.builder("unbreaking", "Unbreaking")
                .type(EnchantmentType.UNIQUE)
                .rarity(EnchantmentRarity.UNCOMMON)
                .maxLevel(3)
                .baseXpCost(12)
                .icon(Material.DIAMOND_SWORD)
                .build());

        // First Strike
        register(SkyblockEnchantment.builder("first_strike", "First Strike")
                .type(EnchantmentType.RARE)
                .rarity(EnchantmentRarity.RARE)
                .maxLevel(5)
                .baseXpCost(15)
                .conflictsWith("sharpness", "smite", "bane_of_arthropods", "power")
                .icon(Material.IRON_SWORD)
                .build());
        
        // Critical
        register(SkyblockEnchantment.builder("critical", "Critical")
                .type(EnchantmentType.RARE)
                .rarity(EnchantmentRarity.RARE)
                .maxLevel(4)
                .baseXpCost(25)
                .conflictsWith("sharpness", "smite", "bane_of_arthropods")
                .icon(Material.DIAMOND_SWORD)
                .build());

        // Aqua Affinity
        register(SkyblockEnchantment.builder("aqua_affinity", "Aqua Affinity")
                .type(EnchantmentType.UNCOMMON)
                .rarity(EnchantmentRarity.UNCOMMON)
                .maxLevel(1)
                .baseXpCost(10)
                .icon(Material.CONDUIT)
                .build());

        // Fortune
        register(SkyblockEnchantment.builder("fortune", "Fortune")
                .type(EnchantmentType.DIGGING)
                .rarity(EnchantmentRarity.UNCOMMON)
                .maxLevel(4)
                .baseXpCost(15)
                .icon(Material.DIAMOND_PICKAXE)
                .build());

        // Silk Touch
        register(SkyblockEnchantment.builder("silk_touch", "Silk Touch")
                .type(EnchantmentType.DIGGING)
                .rarity(EnchantmentRarity.RARE)
                .maxLevel(1)
                .baseXpCost(30)
                .icon(Material.DIAMOND_PICKAXE)
                .build());

        // Protection
        register(SkyblockEnchantment.builder("protection", "Protection")
                .type(EnchantmentType.ARMOR)
                .rarity(EnchantmentRarity.UNCOMMON)
                .maxLevel(4)
                .baseXpCost(10)
                .conflictsWith("fire_protection", "blast_protection", "projectile_protection", "thorns")
                .icon(Material.NETHERITE_CHESTPLATE)
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
            // Replace by key first so recipes remain craftable across reloads.
            Bukkit.removeRecipe(skyblockRecipe.getKey());
            boolean added = Bukkit.addRecipe(bukkitRecipe);
            if (added) {
                RECIPES.put(skyblockRecipe.getKey(), skyblockRecipe);
                plugin.getLogger().info("Registered recipe: " + skyblockRecipe.getName() + " (" + skyblockRecipe.getKey() + ")");
            } else {
                plugin.getLogger().warning("Bukkit rejected recipe registration: " + skyblockRecipe.getName() + " (" + skyblockRecipe.getKey() + ")");
            }
        } else {
            plugin.getLogger().warning("Failed to convert SkyblockRecipe to Bukkit Recipe for: " + skyblockRecipe.getName());
        }
    }

    /**
     * Registers a custom enchantment with its builder.
     *
     * @param id The enchantment ID (e.g., "smelting_touch").
     * @param name The enchantment name (e.g., "Smelting Touch").
     * @return A builder for configuring the enchantment.
     */
    public static Enchantment.Builder builder(String id, String name) {
        return SkyblockEnchantment.builder(id, name);
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

    private static ItemStack createArmorPiece(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }
}

        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "mathematical_hoe_blueprint"))
                .name("Mathematical Hoe Blueprint")
                .result(((io.papermc.Grivience.GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("GGG", "G G", "GGG")
                .ingredient('G', new ItemStack(Material.GOLD_BLOCK))
                .collectionId("WHEAT")
                .collectionTierRequired(3)
                .requiredSkyblockLevel(0)
                .build());

        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "euclids_wheat_hoe"))
                .name("Euclid's Wheat Hoe")
                .result(((io.papermc.Grivience.GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.EUCLIDS_WHEAT_HOE, null))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("EE ", " B ", "   ")
                .ingredient('E', new ItemStack(Material.HAY_BLOCK)) // Enchanted Hay Bale
                .ingredient('B', ((io.papermc.Grivience.GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
                .collectionId("WHEAT")
                .collectionTierRequired(5)
                .requiredSkyblockLevel(0)
                .build());

        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "gauss_carrot_hoe"))
                .name("Gauss Carrot Hoe")
                .result(((io.papermc.Grivience.GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.GAUSS_CARROT_HOE, null))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("EE ", " B ", "   ")
                .ingredient('E', new ItemStack(Material.GOLDEN_CARROT)) // Enchanted Carrot
                .ingredient('B', ((io.papermc.Grivience.GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
                .collectionId("CARROT")
                .collectionTierRequired(5)
                .requiredSkyblockLevel(0)
                .build());

        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "pythagorean_potato_hoe"))
                .name("Pythagorean Potato Hoe")
                .result(((io.papermc.Grivience.GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.PYTHAGOREAN_POTATO_HOE, null))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("EE ", " B ", "   ")
                .ingredient('E', new ItemStack(Material.BAKED_POTATO)) // Enchanted Potato
                .ingredient('B', ((io.papermc.Grivience.GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
                .collectionId("POTATO")
                .collectionTierRequired(5)
                .requiredSkyblockLevel(0)
                .build());

        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "turing_sugar_cane_hoe"))
                .name("Turing Sugar Cane Hoe")
                .result(((io.papermc.Grivience.GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.TURING_SUGAR_CANE_HOE, null))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("EE ", " B ", "   ")
                .ingredient('E', new ItemStack(Material.PAPER)) // Enchanted Sugar Cane
                .ingredient('B', ((io.papermc.Grivience.GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
                .collectionId("SUGAR_CANE")
                .collectionTierRequired(5)
                .requiredSkyblockLevel(0)
                .build());

        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "newton_nether_warts_hoe"))
                .name("Newton Nether Warts Hoe")
                .result(((io.papermc.Grivience.GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.NEWTON_NETHER_WARTS_HOE, null))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("EE ", " B ", "   ")
                .ingredient('E', new ItemStack(Material.NETHER_WART_BLOCK)) // Enchanted Nether Wart
                .ingredient('B', ((io.papermc.Grivience.GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
                .collectionId("NETHER_WART")
                .collectionTierRequired(5)
                .requiredSkyblockLevel(0)
                .build());

        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "melon_dicer"))
                .name("Melon Dicer")
                .result(((io.papermc.Grivience.GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MELON_DICER, null))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("EE ", " B ", "   ")
                .ingredient('E', new ItemStack(Material.MELON)) // Enchanted Melon Block
                .ingredient('B', ((io.papermc.Grivience.GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
                .collectionId("MELON")
                .collectionTierRequired(5)
                .requiredSkyblockLevel(0)
                .build());

        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "pumpkin_dicer"))
                .name("Pumpkin Dicer")
                .result(((io.papermc.Grivience.GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.PUMPKIN_DICER, null))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("EE ", " B ", "   ")
                .ingredient('E', new ItemStack(Material.JACK_O_LANTERN)) // Enchanted Pumpkin
                .ingredient('B', ((io.papermc.Grivience.GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
                .collectionId("PUMPKIN")
        .collectionId("IRON_INGOT")
                .collectionTierRequired(2)
                .requiredSkyblockLevel(0) // Set to 0 to remove Skyblock Level requirement
                .build());

        // --- Custom Hoe Recipes ---
        // Mathematical Hoe Blueprint
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "mathematical_hoe_blueprint"))
                .name("Mathematical Hoe Blueprint")
                .result(((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("GGG", "G G", "GGG")
                .ingredient('G', new ItemStack(Material.GOLD_BLOCK))
                .collectionId("WHEAT")
                .collectionTierRequired(3)
                .requiredSkyblockLevel(0)
                .build());

        // Euclid's Wheat Hoe
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "euclids_wheat_hoe"))
                .name("Euclid's Wheat Hoe")
                .result(((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.EUCLIDS_WHEAT_HOE, null))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("EE ", " B ", "   ")
                .ingredient('E', new ItemStack(Material.HAY_BLOCK)) // Enchanted Hay Bale
                .ingredient('B', ((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
                .collectionId("WHEAT")
                .collectionTierRequired(5)
                .requiredSkyblockLevel(0)
                .build());

        // Gauss Carrot Hoe
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "gauss_carrot_hoe"))
                .name("Gauss Carrot Hoe")
                .result(((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.GAUSS_CARROT_HOE, null))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("EE ", " B ", "   ")
                .ingredient('E', new ItemStack(Material.GOLDEN_CARROT)) // Enchanted Carrot
                .ingredient('B', ((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
                .collectionId("CARROT")
                .collectionTierRequired(5)
                .requiredSkyblockLevel(0)
                .build());

        // Pythagorean Potato Hoe
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "pythagorean_potato_hoe"))
                .name("Pythagorean Potato Hoe")
                .result(((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.PYTHAGOREAN_POTATO_HOE, null))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("EE ", " B ", "   ")
                .ingredient('E', new ItemStack(Material.BAKED_POTATO)) // Enchanted Potato
                .ingredient('B', ((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
                .collectionId("POTATO")
                .collectionTierRequired(5)
                .requiredSkyblockLevel(0)
                .build());

        // Turing Sugarcane Hoe
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "turing_sugar_cane_hoe"))
                .name("Turing Sugarcane Hoe")
                .result(((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.TURING_SUGAR_CANE_HOE, null))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("EE ", " B ", "   ")
                .ingredient('E', new ItemStack(Material.PAPER)) // Enchanted Sugarcane
                .ingredient('B', ((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
                .collectionId("SUGAR_CANE")
                .collectionTierRequired(5)
                .requiredSkyblockLevel(0)
                .build());

        // Newton Nether Warts Hoe
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "newton_nether_warts_hoe"))
                .name("Newton Nether Warts Hoe")
                .result(((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.NEWTON_NETHER_WARTS_HOE, null))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("EE ", " B ", "   ")
                .ingredient('E', new ItemStack(Material.NETHER_WART_BLOCK)) // Enchanted Nether Wart
                .ingredient('B', ((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
                .collectionId("NETHER_WART")
                .collectionTierRequired(5)
                .requiredSkyblockLevel(0)
                .build());

        // Melon Dicer
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "melon_dicer"))
                .name("Melon Dicer")
                .result(((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MELON_DICER, null))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("EE ", " B ", "   ")
                .ingredient('E', new ItemStack(Material.MELON)) // Enchanted Melon Block
                .ingredient('B', ((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
                .collectionId("MELON")
                .collectionTierRequired(5)
                .requiredSkyblockLevel(0)
                .build());

        // Pumpkin Dicer
        register(SkyblockRecipe.builder()
                .key(new NamespacedKey(plugin, "pumpkin_dicer"))
                .name("Pumpkin Dicer")
                .result(((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.PUMPKIN_DICER, null))
                .category(RecipeCategory.FARMING)
                .shape(RecipeShape.SHAPED_3X3)
                .shapeLines("EE ", " B ", "   ")
                .ingredient('E', new ItemStack(Material.JACK_O_LANTERN)) // Enchanted Pumpkin
                .ingredient('B', ((GriviencePlugin) plugin).getCustomItemService().createTool(io.papermc.Grivience.item.CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, null))
                .collectionId("PUMPKIN")
                .collectionTierRequired(5)
                .requiredSkyblockLevel(0)
                .build());

        // --- Enchantment Recipes ---
        register(SkyblockEnchantment.builder("smelting_touch", "Smelting Touch")
                .type(EnchantmentType.RARE)
                .rarity(EnchantmentRarity.RARE)
                .maxLevel(3)
                .baseXpCost(35)
                .conflictsWith("fortune", "silk_touch")
                .icon(Material.FURNACE)
                .build());

        // Unbreaking
        register(SkyblockEnchantment.builder("unbreaking", "Unbreaking")
