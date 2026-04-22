package io.papermc.Grivience.crafting;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.item.CustomArmorManager;
import io.papermc.Grivience.item.CustomArmorManager.ArmorPieceType;
import io.papermc.Grivience.item.CustomArmorType;
import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.item.CustomWeaponType;
import io.papermc.Grivience.item.EndMinesMaterialType;
import io.papermc.Grivience.item.MiningItemType;
import io.papermc.Grivience.item.RaijinCraftingItemType;
import io.papermc.Grivience.minion.MinionManager;
import io.papermc.Grivience.minion.MinionType;
import io.papermc.Grivience.pet.PetDefinition;
import io.papermc.Grivience.pet.PetManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Material-to-recipe catalog used by the crafting guide GUI.
 */
public final class CraftingGuideCatalog {
    @FunctionalInterface
    public interface MaterialResolver {
        GuideMaterial resolve(ItemStack stack);
    }

    private static final Comparator<GuideMaterial> MATERIAL_SORT =
            Comparator.comparing(material -> stripColor(material.name()).toLowerCase(Locale.ROOT));
    private static final Comparator<GuideRecipe> RECIPE_SORT =
            Comparator.comparing(recipe -> stripColor(recipe.name()).toLowerCase(Locale.ROOT));

    private final List<GuideRecipe> recipes;
    private final List<GuideMaterial> materials;
    private final Map<String, GuideRecipe> recipesById;
    private final Map<String, GuideMaterial> materialsByKey;
    private final Map<String, List<GuideRecipe>> recipesByMaterialKey;
    private final MaterialResolver materialResolver;

    public CraftingGuideCatalog(List<GuideRecipe> recipes) {
        this(recipes, CraftingGuideCatalog::resolveVanillaMaterial);
    }

    public CraftingGuideCatalog(List<GuideRecipe> recipes, MaterialResolver materialResolver) {
        List<GuideRecipe> safeRecipes = recipes == null ? List.of() : recipes;
        MaterialResolver resolver = materialResolver == null ? CraftingGuideCatalog::resolveVanillaMaterial : materialResolver;

        Map<String, GuideRecipe> recipesById = new LinkedHashMap<>();
        for (GuideRecipe recipe : safeRecipes) {
            if (recipe == null || recipe.id() == null || recipe.id().isBlank()) {
                continue;
            }
            recipesById.putIfAbsent(recipe.id(), recipe);
        }

        Map<String, GuideMaterial> materialsByKey = new LinkedHashMap<>();
        Map<String, LinkedHashSet<String>> recipeIdsByMaterialKey = new LinkedHashMap<>();
        for (GuideRecipe recipe : recipesById.values()) {
            LinkedHashSet<String> materialKeysForRecipe = new LinkedHashSet<>();
            for (GuideIngredient ingredient : recipe.ingredients()) {
                if (ingredient == null) {
                    continue;
                }
                GuideMaterial material = resolver.resolve(ingredient.stack());
                if (material == null || material.key() == null || material.key().isBlank()) {
                    continue;
                }

                GuideMaterial existing = materialsByKey.get(material.key());
                if (existing == null) {
                    materialsByKey.put(material.key(), material);
                } else if (shouldReplaceMaterial(existing, material)) {
                    materialsByKey.put(material.key(), material);
                }

                if (materialKeysForRecipe.add(material.key())) {
                    recipeIdsByMaterialKey
                            .computeIfAbsent(material.key(), ignored -> new LinkedHashSet<>())
                            .add(recipe.id());
                }
            }
        }

        List<GuideMaterial> sortedMaterials = new ArrayList<>(materialsByKey.values());
        sortedMaterials.sort(MATERIAL_SORT);

        Map<String, List<GuideRecipe>> recipesByMaterialKey = new LinkedHashMap<>();
        for (Map.Entry<String, LinkedHashSet<String>> entry : recipeIdsByMaterialKey.entrySet()) {
            List<GuideRecipe> resolvedRecipes = new ArrayList<>();
            for (String recipeId : entry.getValue()) {
                GuideRecipe recipe = recipesById.get(recipeId);
                if (recipe != null) {
                    resolvedRecipes.add(recipe);
                }
            }
            resolvedRecipes.sort(RECIPE_SORT);
            recipesByMaterialKey.put(entry.getKey(), List.copyOf(resolvedRecipes));
        }

        List<GuideRecipe> sortedRecipes = new ArrayList<>(recipesById.values());
        sortedRecipes.sort(RECIPE_SORT);

        this.recipes = List.copyOf(sortedRecipes);
        this.materials = List.copyOf(sortedMaterials);
        this.recipesById = Map.copyOf(recipesById);
        this.materialsByKey = Map.copyOf(materialsByKey);
        this.recipesByMaterialKey = Map.copyOf(recipesByMaterialKey);
        this.materialResolver = resolver;
    }

    public static CraftingGuideCatalog build(GriviencePlugin plugin) {
        if (plugin == null) {
            return new CraftingGuideCatalog(List.of());
        }

        List<GuideRecipe> recipes = new ArrayList<>();
        addSkyblockRecipes(recipes);
        addCustomItemRecipes(recipes, plugin.getCustomItemService());
        addConfiguredArmorRecipes(recipes, plugin);
        addPetRecipes(recipes, plugin.getPetManager());
        addMinionRecipes(recipes, plugin.getMinionManager());

        CustomItemService itemService = plugin.getCustomItemService();
        MinionManager minionManager = plugin.getMinionManager();
        return new CraftingGuideCatalog(recipes, stack -> resolveMaterial(itemService, minionManager, stack));
    }

    public List<GuideRecipe> recipes() {
        return recipes;
    }

    public List<GuideMaterial> materials() {
        return materials;
    }

    public Optional<GuideRecipe> recipe(String recipeId) {
        if (recipeId == null || recipeId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(recipesById.get(recipeId));
    }

    public Optional<GuideMaterial> material(String materialKey) {
        if (materialKey == null || materialKey.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(materialsByKey.get(materialKey));
    }

    public List<GuideRecipe> recipesForMaterial(String materialKey) {
        if (materialKey == null || materialKey.isBlank()) {
            return List.of();
        }
        return recipesByMaterialKey.getOrDefault(materialKey, List.of());
    }

    public List<GuideMaterial> searchMaterials(String query) {
        if (query == null || query.isBlank()) {
            return materials;
        }

        String normalized = query.trim().toLowerCase(Locale.ROOT);
        List<GuideMaterial> matches = new ArrayList<>();
        for (GuideMaterial material : materials) {
            String name = stripColor(material.name()).toLowerCase(Locale.ROOT);
            if (name.contains(normalized) || material.key().toLowerCase(Locale.ROOT).contains(normalized)) {
                matches.add(material);
            }
        }
        matches.sort(MATERIAL_SORT);
        return matches;
    }

    public Optional<GuideMaterial> materialForItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return Optional.empty();
        }
        GuideMaterial resolved = materialResolver.resolve(item);
        if (resolved == null || resolved.key() == null || resolved.key().isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(materialsByKey.get(resolved.key()));
    }

    private static void addSkyblockRecipes(List<GuideRecipe> recipes) {
        for (SkyblockRecipe recipe : RecipeRegistry.getAll()) {
            if (recipe == null) {
                continue;
            }

            List<GuideIngredient> ingredients = new ArrayList<>();
            if (recipe.getShape() == RecipeShape.SHAPELESS) {
                int slot = 0;
                for (ItemStack ingredient : recipe.getIngredients().values()) {
                    if (ingredient == null || ingredient.getType().isAir()) {
                        continue;
                    }
                    if (slot >= 9) {
                        break;
                    }
                    ingredients.add(new GuideIngredient(slot++, ingredient));
                }
            } else {
                String[] pattern = recipe.getShapePattern();
                if (pattern == null || pattern.length == 0) {
                    pattern = recipe.getShape() == RecipeShape.SHAPED_2X2
                            ? new String[]{"AB", "CD"}
                            : new String[]{"ABC", "DEF", "GHI"};
                }
                for (int row = 0; row < pattern.length; row++) {
                    for (int column = 0; column < pattern[row].length(); column++) {
                        char symbol = pattern[row].charAt(column);
                        if (symbol == ' ') {
                            continue;
                        }
                        ItemStack ingredient = recipe.getIngredients().get(symbol);
                        if (ingredient == null || ingredient.getType().isAir()) {
                            continue;
                        }
                        ingredients.add(new GuideIngredient((row * 3) + column, ingredient));
                    }
                }
            }

            recipes.add(new GuideRecipe(
                    "registry:" + recipe.getKey(),
                    recipe.getName(),
                    displayClone(recipe.getResult(), recipe.getName()),
                    ingredients,
                    "Skyblock Recipes",
                    recipe.getCategory(),
                    recipe.getCollectionId(),
                    recipe.getCollectionTierRequired()
            ));
        }
    }

    private static void addCustomItemRecipes(List<GuideRecipe> recipes, CustomItemService itemService) {
        if (itemService == null) {
            return;
        }

        recipes.add(recipe(
                "custom-item:flying-raijin",
                "Flying Raijin",
                itemService.createWeapon(CustomWeaponType.FLYING_RAIJIN),
                "Custom Weapons",
                RecipeCategory.COMBAT,
                null,
                0,
                slot(0, itemService.createCraftingItem(RaijinCraftingItemType.STORM_SIGIL)),
                slot(1, itemService.createCraftingItem(RaijinCraftingItemType.THUNDER_ESSENCE)),
                slot(2, itemService.createCraftingItem(RaijinCraftingItemType.STORM_SIGIL)),
                slot(4, new ItemStack(Material.NETHERITE_SWORD)),
                slot(7, itemService.createCraftingItem(RaijinCraftingItemType.RAIJIN_CORE))
        ));

        recipes.add(recipe(
                "custom-item:hayabusa-katana",
                "Hayabusa Katana",
                itemService.createWeapon(CustomWeaponType.HAYABUSA_KATANA),
                "Custom Weapons",
                RecipeCategory.COMBAT,
                null,
                0,
                slot(1, itemService.createCraftingItem(RaijinCraftingItemType.DRAGON_SCALE)),
                slot(4, itemService.createCraftingItem(RaijinCraftingItemType.DRAGON_SCALE)),
                slot(7, new ItemStack(Material.STICK))
        ));

        recipes.add(recipe(
                "custom-item:raijin-shortbow",
                "Raijin Shortbow",
                itemService.createWeapon(CustomWeaponType.RAIJIN_SHORTBOW),
                "Custom Weapons",
                RecipeCategory.COMBAT,
                null,
                0,
                slot(0, itemService.createCraftingItem(RaijinCraftingItemType.BLOSSOM_FIBER)),
                slot(1, new ItemStack(Material.BOW)),
                slot(3, itemService.createCraftingItem(RaijinCraftingItemType.BLOSSOM_FIBER)),
                slot(5, itemService.createCraftingItem(RaijinCraftingItemType.THUNDER_ESSENCE)),
                slot(6, itemService.createCraftingItem(RaijinCraftingItemType.BLOSSOM_FIBER)),
                slot(7, new ItemStack(Material.BOW))
        ));

        recipes.add(recipe(
                "custom-item:sovereign-aspect",
                "Sovereign Aspect",
                itemService.createWeapon(CustomWeaponType.SOVEREIGN_ASPECT),
                "Custom Weapons",
                RecipeCategory.COMBAT,
                null,
                0,
                slot(0, new ItemStack(Material.STRING)),
                slot(1, itemService.createEndMinesMaterial(EndMinesMaterialType.KUNZITE)),
                slot(2, new ItemStack(Material.STRING)),
                slot(3, new ItemStack(Material.STRING)),
                slot(4, itemService.createEndMinesMaterial(EndMinesMaterialType.KUNZITE)),
                slot(5, new ItemStack(Material.STRING)),
                slot(6, itemService.createCraftingItem(RaijinCraftingItemType.DRAGONS_SPINE)),
                slot(7, itemService.createWeapon(CustomWeaponType.RIFTBREAKER)),
                slot(8, itemService.createCraftingItem(RaijinCraftingItemType.DRAGONS_SPINE))
        ));

        ItemStack fragment10 = stackAmount(itemService.createCraftingItem(RaijinCraftingItemType.GUARDIAN_FRAGMENT), 10);
        recipes.add(recipe(
                "guardian:helm",
                "Guardian Helm",
                itemService.createArmor(CustomArmorType.GUARDIAN_HELM),
                "Guardian Armor",
                RecipeCategory.COMBAT,
                null,
                0,
                slot(0, fragment10),
                slot(1, fragment10),
                slot(2, fragment10),
                slot(3, fragment10),
                slot(5, fragment10)
        ));
        recipes.add(recipe(
                "guardian:chestplate",
                "Guardian Chestplate",
                itemService.createArmor(CustomArmorType.GUARDIAN_CHESTPLATE),
                "Guardian Armor",
                RecipeCategory.COMBAT,
                null,
                0,
                slot(0, fragment10),
                slot(2, fragment10),
                slot(3, fragment10),
                slot(4, fragment10),
                slot(5, fragment10),
                slot(6, fragment10),
                slot(7, fragment10),
                slot(8, fragment10)
        ));
        recipes.add(recipe(
                "guardian:leggings",
                "Guardian Leggings",
                itemService.createArmor(CustomArmorType.GUARDIAN_LEGGINGS),
                "Guardian Armor",
                RecipeCategory.COMBAT,
                null,
                0,
                slot(0, fragment10),
                slot(1, fragment10),
                slot(2, fragment10),
                slot(3, fragment10),
                slot(5, fragment10),
                slot(6, fragment10),
                slot(8, fragment10)
        ));
        recipes.add(recipe(
                "guardian:boots",
                "Guardian Boots",
                itemService.createArmor(CustomArmorType.GUARDIAN_BOOTS),
                "Guardian Armor",
                RecipeCategory.COMBAT,
                null,
                0,
                slot(3, fragment10),
                slot(5, fragment10),
                slot(6, fragment10),
                slot(8, fragment10)
        ));

        ItemStack oreFragment = itemService.createEndMinesMaterial(EndMinesMaterialType.ORE_FRAGMENT);
        if (oreFragment != null) {
            recipes.add(recipe(
                    "drill-upgrade:ironcrest-to-titanium",
                    "Titanium Drill Upgrade",
                    itemService.createMiningItem(MiningItemType.TITANIUM_DRILL),
                    "Drill Upgrades",
                    RecipeCategory.MINING,
                    null,
                    0,
                    slot(0, oreFragment),
                    slot(1, oreFragment),
                    slot(2, oreFragment),
                    slot(3, oreFragment),
                    slot(4, itemService.createMiningItem(MiningItemType.IRONCREST_DRILL)),
                    slot(5, oreFragment),
                    slot(6, oreFragment),
                    slot(7, oreFragment),
                    slot(8, oreFragment)
            ));
            recipes.add(recipe(
                    "drill-upgrade:titanium-to-gemstone",
                    "Gemstone Drill Upgrade",
                    itemService.createMiningItem(MiningItemType.GEMSTONE_DRILL),
                    "Drill Upgrades",
                    RecipeCategory.MINING,
                    null,
                    0,
                    slot(0, oreFragment),
                    slot(1, oreFragment),
                    slot(2, oreFragment),
                    slot(3, oreFragment),
                    slot(4, itemService.createMiningItem(MiningItemType.TITANIUM_DRILL)),
                    slot(5, oreFragment),
                    slot(6, oreFragment),
                    slot(7, oreFragment),
                    slot(8, oreFragment)
            ));
        }

        ItemStack endstoneShard = itemService.createEndMinesMaterial(EndMinesMaterialType.ENDSTONE_SHARD);
        ItemStack riftEssence = itemService.createEndMinesMaterial(EndMinesMaterialType.RIFT_ESSENCE);
        ItemStack voidCrystal = itemService.createEndMinesMaterial(EndMinesMaterialType.VOID_CRYSTAL);
        ItemStack obsidianCore = itemService.createEndMinesMaterial(EndMinesMaterialType.OBSIDIAN_CORE);
        ItemStack chorusWeave = itemService.createEndMinesMaterial(EndMinesMaterialType.CHORUS_WEAVE);
        ItemStack kunzite = itemService.createEndMinesMaterial(EndMinesMaterialType.KUNZITE);
        if (endstoneShard != null && oreFragment != null && riftEssence != null && voidCrystal != null
                && obsidianCore != null && chorusWeave != null && kunzite != null) {
            recipes.add(recipe(
                    "drill-forge:volta",
                    "Volta",
                    stackAmount(itemService.createMiningItem(MiningItemType.VOLTA), 4),
                    "Drill Forge",
                    RecipeCategory.MINING,
                    null,
                    0,
                    slot(0, endstoneShard),
                    slot(1, new ItemStack(Material.REDSTONE)),
                    slot(2, endstoneShard),
                    slot(3, new ItemStack(Material.REDSTONE)),
                    slot(4, new ItemStack(Material.REDSTONE_BLOCK)),
                    slot(5, new ItemStack(Material.REDSTONE)),
                    slot(6, endstoneShard),
                    slot(7, new ItemStack(Material.REDSTONE)),
                    slot(8, endstoneShard)
            ));
            recipes.add(recipe(
                    "drill-forge:oil-barrel",
                    "Oil Barrel",
                    itemService.createMiningItem(MiningItemType.OIL_BARREL),
                    "Drill Forge",
                    RecipeCategory.MINING,
                    null,
                    0,
                    slot(0, obsidianCore),
                    slot(1, itemService.createMiningItem(MiningItemType.VOLTA)),
                    slot(2, obsidianCore),
                    slot(3, chorusWeave),
                    slot(4, new ItemStack(Material.CAULDRON)),
                    slot(5, chorusWeave),
                    slot(6, obsidianCore),
                    slot(7, itemService.createMiningItem(MiningItemType.VOLTA)),
                    slot(8, obsidianCore)
            ));
            recipes.add(recipe(
                    "drill-forge:mithril-engine",
                    "Mithril Engine",
                    itemService.createMiningItem(MiningItemType.MITHRIL_ENGINE),
                    "Drill Forge",
                    RecipeCategory.MINING,
                    null,
                    0,
                    slot(0, endstoneShard),
                    slot(1, oreFragment),
                    slot(2, endstoneShard),
                    slot(3, new ItemStack(Material.REDSTONE_BLOCK)),
                    slot(4, new ItemStack(Material.PISTON)),
                    slot(5, new ItemStack(Material.REDSTONE_BLOCK)),
                    slot(6, endstoneShard),
                    slot(7, oreFragment),
                    slot(8, endstoneShard)
            ));
            recipes.add(recipe(
                    "drill-forge:titanium-engine",
                    "Titanium Engine",
                    itemService.createMiningItem(MiningItemType.TITANIUM_ENGINE),
                    "Drill Forge",
                    RecipeCategory.MINING,
                    null,
                    0,
                    slot(0, voidCrystal),
                    slot(1, riftEssence),
                    slot(2, voidCrystal),
                    slot(3, new ItemStack(Material.STICKY_PISTON)),
                    slot(4, itemService.createMiningItem(MiningItemType.MITHRIL_ENGINE)),
                    slot(5, new ItemStack(Material.STICKY_PISTON)),
                    slot(6, voidCrystal),
                    slot(7, obsidianCore),
                    slot(8, voidCrystal)
            ));
            recipes.add(recipe(
                    "drill-forge:gemstone-engine",
                    "Gemstone Engine",
                    itemService.createMiningItem(MiningItemType.GEMSTONE_ENGINE),
                    "Drill Forge",
                    RecipeCategory.MINING,
                    null,
                    0,
                    slot(0, kunzite),
                    slot(1, voidCrystal),
                    slot(2, kunzite),
                    slot(3, new ItemStack(Material.OBSERVER)),
                    slot(4, itemService.createMiningItem(MiningItemType.TITANIUM_ENGINE)),
                    slot(5, new ItemStack(Material.OBSERVER)),
                    slot(6, kunzite),
                    slot(7, riftEssence),
                    slot(8, kunzite)
            ));
            recipes.add(recipe(
                    "drill-forge:divan-engine",
                    "Divan Engine",
                    itemService.createMiningItem(MiningItemType.DIVAN_ENGINE),
                    "Drill Forge",
                    RecipeCategory.MINING,
                    null,
                    0,
                    slot(0, kunzite),
                    slot(1, voidCrystal),
                    slot(2, kunzite),
                    slot(3, new ItemStack(Material.NETHER_STAR)),
                    slot(4, itemService.createMiningItem(MiningItemType.GEMSTONE_ENGINE)),
                    slot(5, new ItemStack(Material.NETHER_STAR)),
                    slot(6, kunzite),
                    slot(7, chorusWeave),
                    slot(8, kunzite)
            ));
            recipes.add(recipe(
                    "drill-forge:medium-fuel-tank",
                    "Medium Fuel Tank",
                    itemService.createMiningItem(MiningItemType.MEDIUM_FUEL_TANK),
                    "Drill Forge",
                    RecipeCategory.MINING,
                    null,
                    0,
                    slot(0, oreFragment),
                    slot(1, new ItemStack(Material.BUCKET)),
                    slot(2, oreFragment),
                    slot(3, new ItemStack(Material.BUCKET)),
                    slot(4, obsidianCore),
                    slot(5, new ItemStack(Material.BUCKET)),
                    slot(6, oreFragment),
                    slot(7, new ItemStack(Material.BUCKET)),
                    slot(8, oreFragment)
            ));
            recipes.add(recipe(
                    "drill-forge:large-fuel-tank",
                    "Large Fuel Tank",
                    itemService.createMiningItem(MiningItemType.LARGE_FUEL_TANK),
                    "Drill Forge",
                    RecipeCategory.MINING,
                    null,
                    0,
                    slot(0, voidCrystal),
                    slot(1, obsidianCore),
                    slot(2, voidCrystal),
                    slot(3, itemService.createMiningItem(MiningItemType.MEDIUM_FUEL_TANK)),
                    slot(4, new ItemStack(Material.MINECART)),
                    slot(5, itemService.createMiningItem(MiningItemType.MEDIUM_FUEL_TANK)),
                    slot(6, voidCrystal),
                    slot(7, obsidianCore),
                    slot(8, voidCrystal)
            ));
        }
    }

    private static void addConfiguredArmorRecipes(List<GuideRecipe> recipes, GriviencePlugin plugin) {
        if (plugin == null || plugin.getCustomArmorManager() == null) {
            return;
        }

        CustomArmorManager armorManager = plugin.getCustomArmorManager();
        CustomItemService itemService = plugin.getCustomItemService();
        FileConfiguration config = plugin.getConfig();

        for (Map.Entry<String, CustomArmorManager.CustomArmorSet> entry : armorManager.getArmorSets().entrySet()) {
            String setId = entry.getKey();
            if (setId == null || setId.isBlank()) {
                continue;
            }

            // Only show recipes if a crafting material is explicitly configured
            String configMaterial = config.getString("custom-armor.sets." + setId + ".crafting-material");
            if (configMaterial == null || configMaterial.isBlank()) {
                continue;
            }

            ItemStack craftMaterial = resolveConfiguredCraftMaterial(itemService, configMaterial);
            if (craftMaterial == null) {
                // If it was configured but failed to resolve (e.g. invalid material name), also skip
                continue;
            }

            for (ArmorPieceType pieceType : ArmorPieceType.values()) {
                ItemStack result = armorManager.createArmorPiece(setId, pieceType);
                if (result == null) {
                    continue;
                }

                GuideIngredient[] ingredients = switch (pieceType) {
                    case HELMET -> new GuideIngredient[]{
                            slot(0, craftMaterial), slot(1, craftMaterial), slot(2, craftMaterial), slot(3, craftMaterial), slot(5, craftMaterial)
                    };
                    case CHESTPLATE -> new GuideIngredient[]{
                            slot(0, craftMaterial), slot(2, craftMaterial), slot(3, craftMaterial), slot(4, craftMaterial),
                            slot(5, craftMaterial), slot(6, craftMaterial), slot(7, craftMaterial), slot(8, craftMaterial)
                    };
                    case LEGGINGS -> new GuideIngredient[]{
                            slot(0, craftMaterial), slot(1, craftMaterial), slot(2, craftMaterial), slot(3, craftMaterial),
                            slot(5, craftMaterial), slot(6, craftMaterial), slot(8, craftMaterial)
                    };
                    case BOOTS -> new GuideIngredient[]{
                            slot(0, craftMaterial), slot(2, craftMaterial), slot(3, craftMaterial), slot(5, craftMaterial)
                    };
                };

                recipes.add(recipe(
                        "armor:" + setId.toLowerCase(Locale.ROOT) + ":" + pieceType.name().toLowerCase(Locale.ROOT),
                        displayName(result, pieceType.name()),
                        result,
                        "Custom Armor",
                        null,
                        config.getString("custom-armor.sets." + setId + ".collection-id"),
                        Math.max(0, config.getInt("custom-armor.sets." + setId + ".collection-tier-required", 0)),
                        ingredients
                ));
            }
        }
    }

    private static void addPetRecipes(List<GuideRecipe> recipes, PetManager petManager) {
        if (petManager == null) {
            return;
        }

        Collection<PetDefinition> definitions = petManager.allPets();
        for (PetDefinition definition : definitions) {
            if (definition == null) {
                continue;
            }

            ItemStack result = petManager.createPetItem(definition.id(), null, true);
            Material icon = definition.icon() == null ? Material.LEAD : definition.icon();
            recipes.add(recipe(
                    "pet:" + definition.id(),
                    definition.displayName(),
                    result,
                    "Pets",
                    RecipeCategory.SPECIAL,
                    null,
                    0,
                    slot(0, new ItemStack(Material.EGG)),
                    slot(1, new ItemStack(icon))
            ));
        }
    }

    private static void addMinionRecipes(List<GuideRecipe> recipes, MinionManager minionManager) {
        if (minionManager == null) {
            return;
        }

        for (MinionManager.IngredientRecipe recipe : MinionManager.getIngredientRecipes()) {
            if (recipe == null) {
                continue;
            }

            ItemStack result = minionManager.createIngredientItem(recipe.outputIngredientId(), recipe.outputAmount());
            if (result == null) {
                continue;
            }

            List<GuideIngredient> ingredients = new ArrayList<>();
            for (int slot = 0; slot < 9; slot++) {
                int amount = recipe.slotCosts()[slot];
                String inputId = recipe.slotIngredientIds()[slot];
                if (amount <= 0 || inputId == null || inputId.isBlank()) {
                    continue;
                }
                ItemStack display = minionManager.createRecipeDisplayItem(inputId, amount);
                if (display != null) {
                    ingredients.add(new GuideIngredient(slot, display));
                }
            }

            recipes.add(new GuideRecipe(
                    "minion-ingredient:" + recipe.outputIngredientId(),
                    stripColor(displayName(result, recipe.outputIngredientId())),
                    result,
                    ingredients,
                    "Minion Ingredients",
                    RecipeCategory.SPECIAL,
                    null,
                    0
            ));
        }

        for (MinionManager.UtilityRecipeInfo recipe : MinionManager.getUtilityRecipes()) {
            if (recipe == null) {
                continue;
            }

            ItemStack result = switch (recipe.outputType()) {
                case "fuel" -> minionManager.createFuelItem(recipe.outputId(), recipe.outputAmount());
                case "upgrade" -> minionManager.createUpgradeItem(recipe.outputId(), recipe.outputAmount());
                default -> null;
            };
            if (result == null) {
                continue;
            }

            List<GuideIngredient> ingredients = new ArrayList<>();
            for (int slot = 0; slot < 9; slot++) {
                int amount = recipe.slotCosts()[slot];
                String requirement = recipe.requirements()[slot];
                if (amount <= 0 || requirement == null || requirement.isBlank()) {
                    continue;
                }
                ItemStack display = minionManager.createRecipeRequirementDisplayItem(requirement, amount);
                if (display != null) {
                    ingredients.add(new GuideIngredient(slot, display));
                }
            }

            recipes.add(new GuideRecipe(
                    "minion-utility:" + recipe.outputType() + ":" + recipe.outputId(),
                    stripColor(displayName(result, recipe.outputId())),
                    result,
                    ingredients,
                    "Minion Utilities",
                    RecipeCategory.SPECIAL,
                    null,
                    0
            ));
        }

        for (MinionType type : MinionType.values()) {
            for (int tier = 1; tier <= type.maxCraftableTier(); tier++) {
                MinionType.TierRecipe recipe = type.recipeForTier(tier);
                if (recipe == null) {
                    continue;
                }

                ItemStack result = minionManager.createMinionItem(type, tier);
                if (result == null) {
                    continue;
                }

                List<GuideIngredient> ingredients = new ArrayList<>();
                ItemStack center = tier == 1
                        ? new ItemStack(type.baseCraftTool())
                        : minionManager.createMinionItem(type, tier - 1);
                if (center != null) {
                    ingredients.add(new GuideIngredient(4, center));
                }

                ItemStack outer = minionManager.createRecipeDisplayItem(recipe.ingredientId(), recipe.amountPerOuterSlot());
                if (outer != null) {
                    for (int slot : List.of(0, 1, 2, 3, 5, 6, 7, 8)) {
                        ingredients.add(new GuideIngredient(slot, outer));
                    }
                }

                recipes.add(new GuideRecipe(
                        "minion-tier:" + type.id() + ":" + tier,
                        stripColor(displayName(result, type.displayName() + " Minion")),
                        result,
                        ingredients,
                        "Minion Crafting",
                        RecipeCategory.SPECIAL,
                        null,
                        0
                ));
            }
        }
    }

    private static GuideRecipe recipe(
            String id,
            String fallbackName,
            ItemStack result,
            String source,
            RecipeCategory category,
            String collectionId,
            int collectionTierRequired,
            GuideIngredient... ingredients
    ) {
        List<GuideIngredient> list = new ArrayList<>();
        if (ingredients != null) {
            Collections.addAll(list, ingredients);
        }
        String name = displayName(result, fallbackName);
        return new GuideRecipe(
                id,
                stripColor(name),
                displayClone(result, fallbackName),
                list,
                source,
                category,
                collectionId,
                collectionTierRequired
        );
    }

    private static GuideIngredient slot(int slot, ItemStack stack) {
        return new GuideIngredient(slot, stack);
    }

    private static ItemStack stackAmount(ItemStack stack, int amount) {
        if (stack == null || stack.getType().isAir()) {
            return null;
        }
        ItemStack copy = stack.clone();
        copy.setAmount(Math.max(1, amount));
        return copy;
    }

    private static ItemStack resolveConfiguredCraftMaterial(CustomItemService itemService, String configured) {
        if (configured == null || configured.isBlank()) {
            return null;
        }

        if (configured.regionMatches(true, 0, "custom:", 0, "custom:".length())) {
            String key = configured.substring("custom:".length());
            return itemService == null ? null : itemService.createItemByKey(key);
        }

        Material material = Material.matchMaterial(configured.trim().toUpperCase(Locale.ROOT));
        return material == null ? null : new ItemStack(material);
    }

    private static GuideMaterial resolveMaterial(CustomItemService itemService, MinionManager minionManager, ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return null;
        }

        if (itemService != null) {
            String itemId = itemService.itemId(stack);
            if (itemId != null && !itemId.isBlank()) {
                String name = displayName(stack, itemId);
                return new GuideMaterial("custom:" + normalizeId(itemId), stripColor(name), minimalIcon(stack, name));
            }
        }

        if (minionManager != null) {
            String ingredientId = minionManager.readIngredientId(stack);
            if (ingredientId != null && !ingredientId.isBlank()) {
                MinionManager.IngredientDefinition definition = MinionManager.getIngredients().get(normalizeId(ingredientId));
                String name = definition == null ? displayName(stack, ingredientId) : definition.displayName();
                return new GuideMaterial("minion-ingredient:" + normalizeId(ingredientId), stripColor(name), minimalIcon(stack, name));
            }

            String fuelId = minionManager.readFuelId(stack);
            if (fuelId != null && !fuelId.isBlank()) {
                String name = minionManager.fuelDisplayName(fuelId);
                return new GuideMaterial("minion-fuel:" + normalizeId(fuelId), stripColor(name), minimalIcon(stack, name));
            }

            String upgradeId = minionManager.readUpgradeId(stack);
            if (upgradeId != null && !upgradeId.isBlank()) {
                String name = minionManager.upgradeDisplayName(upgradeId);
                return new GuideMaterial("minion-upgrade:" + normalizeId(upgradeId), stripColor(name), minimalIcon(stack, name));
            }
        }

        return resolveVanillaMaterial(stack);
    }

    private static GuideMaterial resolveVanillaMaterial(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return null;
        }
        String name = displayName(stack, prettyMaterialName(stack.getType()));
        return new GuideMaterial(
                "material:" + stack.getType().name().toLowerCase(Locale.ROOT),
                stripColor(name),
                minimalIcon(stack, name)
        );
    }

    private static boolean shouldReplaceMaterial(GuideMaterial existing, GuideMaterial replacement) {
        if (existing == null) {
            return true;
        }
        if (replacement == null) {
            return false;
        }
        ItemStack existingIcon = existing.icon();
        ItemStack replacementIcon = replacement.icon();
        boolean existingCustom = existingIcon != null && existingIcon.hasItemMeta() && existingIcon.getItemMeta().hasDisplayName();
        boolean replacementCustom = replacementIcon != null && replacementIcon.hasItemMeta() && replacementIcon.getItemMeta().hasDisplayName();
        return !existingCustom && replacementCustom;
    }

    private static ItemStack displayClone(ItemStack source, String fallbackName) {
        if (source == null || source.getType().isAir()) {
            return null;
        }

        ItemStack clone = source.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta != null && (!meta.hasDisplayName() || meta.getDisplayName() == null || meta.getDisplayName().isBlank())) {
            meta.setDisplayName(ChatColor.GREEN + fallbackName);
            clone.setItemMeta(meta);
        }
        return clone;
    }

    private static ItemStack minimalIcon(ItemStack source, String name) {
        if (source == null || source.getType().isAir()) {
            return null;
        }

        ItemStack clone = source.clone();
        clone.setAmount(1);
        ItemMeta meta = clone.getItemMeta();
        if (meta != null) {
            meta.setLore(List.of());
            if (!meta.hasDisplayName() || meta.getDisplayName() == null || meta.getDisplayName().isBlank()) {
                meta.setDisplayName(ChatColor.GREEN + name);
            }
            clone.setItemMeta(meta);
        }
        return clone;
    }

    private static String displayName(ItemStack stack, String fallback) {
        if (stack != null && stack.hasItemMeta()) {
            ItemMeta meta = stack.getItemMeta();
            if (meta != null && meta.hasDisplayName() && meta.getDisplayName() != null && !meta.getDisplayName().isBlank()) {
                return meta.getDisplayName();
            }
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return stack == null ? "Unknown" : prettyMaterialName(stack.getType());
    }

    private static String prettyMaterialName(Material material) {
        if (material == null) {
            return "Unknown";
        }
        String raw = material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        String[] words = raw.split("\\s+");
        List<String> parts = new ArrayList<>(words.length);
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            parts.add(Character.toUpperCase(word.charAt(0)) + word.substring(1));
        }
        return String.join(" ", parts);
    }

    private static String normalizeId(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    private static String stripColor(String input) {
        String stripped = ChatColor.stripColor(input);
        return stripped == null ? "" : stripped;
    }

    public record GuideMaterial(String key, String name, ItemStack icon) {
        public GuideMaterial {
            key = key == null ? "" : key;
            name = name == null ? "" : name;
            icon = icon == null ? null : icon.clone();
        }

        @Override
        public ItemStack icon() {
            return icon == null ? null : icon.clone();
        }
    }

    public record GuideIngredient(int slot, ItemStack stack) {
        public GuideIngredient {
            if (slot < 0 || slot > 8) {
                throw new IllegalArgumentException("Guide ingredient slot must be between 0 and 8");
            }
            stack = stack == null ? null : stack.clone();
        }

        @Override
        public ItemStack stack() {
            return stack == null ? null : stack.clone();
        }
    }

    public static final class GuideRecipe {
        private final String id;
        private final String name;
        private final ItemStack result;
        private final List<GuideIngredient> ingredients;
        private final String source;
        private final RecipeCategory category;
        private final String collectionId;
        private final int collectionTierRequired;

        public GuideRecipe(
                String id,
                String name,
                ItemStack result,
                List<GuideIngredient> ingredients,
                String source,
                RecipeCategory category,
                String collectionId,
                int collectionTierRequired
        ) {
            this.id = Objects.requireNonNullElse(id, "");
            this.name = Objects.requireNonNullElse(name, "");
            this.result = result == null ? null : result.clone();
            this.ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
            this.source = Objects.requireNonNullElse(source, "Crafting");
            this.category = category;
            this.collectionId = collectionId;
            this.collectionTierRequired = Math.max(0, collectionTierRequired);
        }

        public String id() {
            return id;
        }

        public String name() {
            return name;
        }

        public ItemStack result() {
            return result == null ? null : result.clone();
        }

        public List<GuideIngredient> ingredients() {
            return ingredients;
        }

        public String source() {
            return source;
        }

        public RecipeCategory category() {
            return category;
        }

        public String collectionId() {
            return collectionId;
        }

        public int collectionTierRequired() {
            return collectionTierRequired;
        }
    }
}
