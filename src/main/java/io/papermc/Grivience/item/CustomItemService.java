package io.papermc.Grivience.item;

import io.papermc.Grivience.accessory.AccessoryType;
import io.papermc.Grivience.compactor.PersonalCompactorType;
import io.papermc.Grivience.dungeon.MonsterType;
import io.papermc.Grivience.item.EnchantedFarmItemType;
import io.papermc.Grivience.mines.DrillStatProfile;
import io.papermc.Grivience.util.ArmorDurabilityUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Color;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.lang.reflect.Method;

public final class CustomItemService {
    private static final String DRAGON_SLAYER_HELMET_TEXTURE_UUID = "c7ce3652-2bdb-6c36-56ce-be2319ee1681";
    private static final String DRAGON_SLAYER_HELMET_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzdjZTM2NTIyYmRiNmMzNjU2Y2ViZTIzMTllZTE2ODFlN2I1ZWQ3OWFlMmZjMjVlNTk5ZDUxZDg3YmQyODExNSJ9fX0=";
    private static final String SUMMONING_EYE_TEXTURE_UUID = "198a49ca-54c3-ea67-a86e-c8b9f16bdf46";
    private static final String SUMMONING_EYE_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTk4YTQ5Y2E1NGMzZWE2N2E4NmVjOGI5ZjE2YmRmNDZhYTVlZmM1YWVlZmI3YTE5Y2NjYzc5NjJlODIxYTU5OSJ9fX0=";

    private final JavaPlugin plugin;
    private final NamespacedKey itemIdKey;
    private final NamespacedKey reforgeIdKey;
    private final NamespacedKey reforgeBaseNameKey;
    private final NamespacedKey recipeFlyingRaijinKey;
    private final NamespacedKey recipeHayabusaKey;
    private final NamespacedKey recipeRaijinShortbowKey;
    private final NamespacedKey recipeSovereignAspectKey;
    private final NamespacedKey recipeGuardianHelmKey;
    private final NamespacedKey recipeGuardianChestKey;
    private final NamespacedKey recipeGuardianLegsKey;
    private final NamespacedKey recipeGuardianBootsKey;
    private final NamespacedKey farmRecipeKey;
    private final NamespacedKey recipeDrillUpgradeKey;
    private final NamespacedKey recipeVoltaKey;
    private final NamespacedKey recipeOilBarrelKey;
    private final NamespacedKey recipeMithrilEngineKey;
    private final NamespacedKey recipeTitaniumEngineKey;
    private final NamespacedKey recipeGemstoneEngineKey;
    private final NamespacedKey recipeDivanEngineKey;
    private final NamespacedKey recipeMediumFuelTankKey;
    private final NamespacedKey recipeLargeFuelTankKey;
    private final NamespacedKey recipeIroncrestDrillKey;
    private final NamespacedKey recipeTitaniumDrillKey;
    private final NamespacedKey recipeGemstoneDrillKey;
    private final NamespacedKey recipePersonalCompactor3000Key;
    private final NamespacedKey recipePersonalCompactor4000Key;
    private final NamespacedKey recipePersonalCompactor5000Key;
    private final NamespacedKey recipePersonalCompactor6000Key;
    private final NamespacedKey recipePersonalCompactor7000Key;
    private final NamespacedKey customMaterialKey;
    // Drill PDC keys
    private final NamespacedKey drillFuelKey;
    private final NamespacedKey drillFuelMaxKey;
    private final NamespacedKey drillEngineKey;
    private final NamespacedKey drillTankKey;
    private final NamespacedKey dungeonStarsKey;
    private final NamespacedKey dungeonizedKey;

    private io.papermc.Grivience.item.GrapplingHookManager grapplingHookManager;


    private ItemStyle itemStyle = ItemStyle.SKYBLOCK;
    private double mobWeaponBaseChance;
    private final EnumMap<MonsterType, Double> yokaiWeaponChances = new EnumMap<>(MonsterType.class);
    private double mobArmorBaseChance;
    private final EnumMap<MonsterType, Double> yokaiArmorChances = new EnumMap<>(MonsterType.class);
    private double mobReforgeStoneChance;
    private double stormSigilChance;
    private double thunderEssenceChance;
    private double raijinCoreChance;
    private double bossArmorChance;
    private double bossReforgeStoneChance;

    private static final String SB_DAMAGE = ChatColor.GRAY + "Damage: " + ChatColor.RED + "+";
    private static final String SB_STRENGTH = ChatColor.GRAY + "Strength: " + ChatColor.RED + "+";
    private static final String SB_CRIT_CHANCE = ChatColor.GRAY + "Crit Chance: " + ChatColor.BLUE + "+";
    private static final String SB_CRIT_DAMAGE = ChatColor.GRAY + "Crit Damage: " + ChatColor.BLUE + "+";
    private static final String SB_HEALTH = ChatColor.GRAY + "Health: " + ChatColor.RED + "+";
    private static final String SB_DEFENSE = ChatColor.GRAY + "Defense: " + ChatColor.GREEN + "+";
    private static final String SB_MANA = ChatColor.GRAY + "Intelligence: " + ChatColor.AQUA + "+";
    private static final String SB_HEAL_SPEED = ChatColor.GRAY + "Heal Speed: " + ChatColor.GREEN + "+";
    private static final String SB_SEA_FORCE = ChatColor.GRAY + "Seaforce: " + ChatColor.BLUE + "+";
    private static final String SB_FEROCITY = ChatColor.GRAY + "Ferocity: " + ChatColor.RED + "+";
    private static final String SB_ATTACK_SPEED = ChatColor.GRAY + "Attack Speed: " + ChatColor.GOLD + "+";
    private static final String SB_SPEED = ChatColor.GRAY + "Speed: " + ChatColor.WHITE + "+";

    private static final String ENCHANT_MARKER = ChatColor.BLACK.toString() + ChatColor.MAGIC.toString() + ChatColor.RESET.toString();

    public CustomItemService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.itemIdKey = new NamespacedKey(plugin, "custom-item-id");
        this.customMaterialKey = new NamespacedKey(plugin, "custom_material");
        this.reforgeIdKey = new NamespacedKey(plugin, "custom-reforge-id");
        this.reforgeBaseNameKey = new NamespacedKey(plugin, "custom-reforge-base-name");
        this.recipeFlyingRaijinKey = new NamespacedKey(plugin, "flying-raijin-recipe");
        this.recipeHayabusaKey = new NamespacedKey(plugin, "hayabusa_katana_recipe");
        this.recipeRaijinShortbowKey = new NamespacedKey(plugin, "raijin_shortbow_recipe");
        this.recipeSovereignAspectKey = new NamespacedKey(plugin, "sovereign_aspect_recipe");
        this.recipeGuardianHelmKey = new NamespacedKey(plugin, "guardian_helm_recipe");
        this.recipeGuardianChestKey = new NamespacedKey(plugin, "guardian_chest_recipe");
        this.recipeGuardianLegsKey = new NamespacedKey(plugin, "guardian_legs_recipe");
        this.recipeGuardianBootsKey = new NamespacedKey(plugin, "guardian_boots_recipe");
        this.farmRecipeKey = new NamespacedKey(plugin, "farm-compress");
        this.recipeDrillUpgradeKey = new NamespacedKey(plugin, "drill_upgrade");
        this.recipeVoltaKey = new NamespacedKey(plugin, "volta_recipe");
        this.recipeOilBarrelKey = new NamespacedKey(plugin, "oil_barrel_recipe");
        this.recipeMithrilEngineKey = new NamespacedKey(plugin, "mithril_engine_recipe");
        this.recipeTitaniumEngineKey = new NamespacedKey(plugin, "titanium_engine_recipe");
        this.recipeGemstoneEngineKey = new NamespacedKey(plugin, "gemstone_engine_recipe");
        this.recipeDivanEngineKey = new NamespacedKey(plugin, "divan_engine_recipe");
        this.recipeMediumFuelTankKey = new NamespacedKey(plugin, "medium_fuel_tank_recipe");
        this.recipeLargeFuelTankKey = new NamespacedKey(plugin, "large_fuel_tank_recipe");
        this.recipeIroncrestDrillKey = new NamespacedKey(plugin, "ironcrest_drill_recipe");
        this.recipeTitaniumDrillKey = new NamespacedKey(plugin, "titanium_drill_recipe");
        this.recipeGemstoneDrillKey = new NamespacedKey(plugin, "gemstone_drill_recipe");
        this.recipePersonalCompactor3000Key = new NamespacedKey(plugin, "personal_compactor_3000_recipe");
        this.recipePersonalCompactor4000Key = new NamespacedKey(plugin, "personal_compactor_4000_recipe");
        this.recipePersonalCompactor5000Key = new NamespacedKey(plugin, "personal_compactor_5000_recipe");
        this.recipePersonalCompactor6000Key = new NamespacedKey(plugin, "personal_compactor_6000_recipe");
        this.recipePersonalCompactor7000Key = new NamespacedKey(plugin, "personal_compactor_7000_recipe");
        this.drillFuelKey = new NamespacedKey(plugin, "drill-fuel");
        this.drillFuelMaxKey = new NamespacedKey(plugin, "drill-fuel-max");
        this.drillEngineKey = new NamespacedKey(plugin, "drill-engine");
        this.drillTankKey = new NamespacedKey(plugin, "drill-tank");
        this.dungeonStarsKey = new NamespacedKey(plugin, "dungeon-stars");
        this.dungeonizedKey = new NamespacedKey(plugin, "dungeonized");
        resetDropDefaults();
    }

    public void reloadFromConfig() {
        itemStyle = ItemStyle.parse(plugin.getConfig().getString("custom-items.style", "JAPANESE"));
        resetDropDefaults();

        ConfigurationSection dropSection = plugin.getConfig().getConfigurationSection("custom-items.drops");
        if (dropSection == null) {
            return;
        }

        mobWeaponBaseChance = clampChance(dropSection.getDouble("mob-weapon.base-chance", mobWeaponBaseChance));

        ConfigurationSection yokaiSection = dropSection.getConfigurationSection("mob-weapon.yokai");
        if (yokaiSection != null) {
            for (String key : yokaiSection.getKeys(false)) {
                MonsterType type = MonsterType.parse(key);
                if (type == null) {
                    continue;
                }
                yokaiWeaponChances.put(type, clampChance(yokaiSection.getDouble(key, yokaiWeaponChances.get(type))));
            }
        }

        mobArmorBaseChance = clampChance(dropSection.getDouble("mob-armor.base-chance", mobArmorBaseChance));
        ConfigurationSection armorYokaiSection = dropSection.getConfigurationSection("mob-armor.yokai");
        if (armorYokaiSection != null) {
            for (String key : armorYokaiSection.getKeys(false)) {
                MonsterType type = MonsterType.parse(key);
                if (type == null) {
                    continue;
                }
                yokaiArmorChances.put(type, clampChance(armorYokaiSection.getDouble(key, yokaiArmorChances.get(type))));
            }
        }

        mobReforgeStoneChance = clampChance(dropSection.getDouble("mob-reforge-stone.base-chance", mobReforgeStoneChance));

        stormSigilChance = clampChance(dropSection.getDouble("boss-materials.storm-sigil", stormSigilChance));
        thunderEssenceChance = clampChance(dropSection.getDouble("boss-materials.thunder-essence", thunderEssenceChance));
        raijinCoreChance = clampChance(dropSection.getDouble("boss-materials.raijin-core", raijinCoreChance));
        bossArmorChance = clampChance(dropSection.getDouble("boss-materials.armor-piece", bossArmorChance));
        bossReforgeStoneChance = clampChance(dropSection.getDouble("boss-materials.reforge-stone", bossReforgeStoneChance));
    }

    public void registerRecipes() {
        Bukkit.removeRecipe(recipeFlyingRaijinKey);
        Bukkit.removeRecipe(recipeHayabusaKey);
        Bukkit.removeRecipe(recipeRaijinShortbowKey);
        Bukkit.removeRecipe(recipeSovereignAspectKey);
        Bukkit.removeRecipe(recipeGuardianHelmKey);
        Bukkit.removeRecipe(recipeGuardianChestKey);
        Bukkit.removeRecipe(recipeGuardianLegsKey);
        Bukkit.removeRecipe(recipeGuardianBootsKey);
        Bukkit.removeRecipe(recipeDrillUpgradeKey);

        Bukkit.removeRecipe(recipeGuardianChestKey);
        Bukkit.removeRecipe(recipeGuardianLegsKey);
        Bukkit.removeRecipe(recipeGuardianBootsKey);
        Bukkit.removeRecipe(recipeDrillUpgradeKey);
        Bukkit.removeRecipe(recipeVoltaKey);
        Bukkit.removeRecipe(recipeOilBarrelKey);
        Bukkit.removeRecipe(recipeMithrilEngineKey);
        Bukkit.removeRecipe(recipeTitaniumEngineKey);
        Bukkit.removeRecipe(recipeGemstoneEngineKey);
        Bukkit.removeRecipe(recipeDivanEngineKey);
        Bukkit.removeRecipe(recipeMediumFuelTankKey);
        Bukkit.removeRecipe(recipeLargeFuelTankKey);
        Bukkit.removeRecipe(recipeIroncrestDrillKey);
        Bukkit.removeRecipe(recipeTitaniumDrillKey);
        Bukkit.removeRecipe(recipeGemstoneDrillKey);
        Bukkit.removeRecipe(recipePersonalCompactor4000Key);
        Bukkit.removeRecipe(recipePersonalCompactor5000Key);
        Bukkit.removeRecipe(recipePersonalCompactor6000Key);
        Bukkit.removeRecipe(recipePersonalCompactor7000Key);
        // farmRecipeKey acts as a placeholder key to allow discoverRecipe; logic handled by listener
        Bukkit.removeRecipe(farmRecipeKey);

        ShapedRecipe recipe = new ShapedRecipe(recipeFlyingRaijinKey, createWeapon(CustomWeaponType.FLYING_RAIJIN));
        recipe.shape("STS", " R ", " N ");
        recipe.setIngredient('S', new RecipeChoice.ExactChoice(createCraftingItem(RaijinCraftingItemType.STORM_SIGIL)));
        recipe.setIngredient('T', new RecipeChoice.ExactChoice(createCraftingItem(RaijinCraftingItemType.THUNDER_ESSENCE)));
        recipe.setIngredient('R', new RecipeChoice.ExactChoice(createCraftingItem(RaijinCraftingItemType.RAIJIN_CORE)));
        recipe.setIngredient('N', Material.NETHERITE_SWORD);
        Bukkit.addRecipe(recipe);

        ShapedRecipe katana = new ShapedRecipe(recipeHayabusaKey, createWeapon(CustomWeaponType.HAYABUSA_KATANA));
        katana.shape(" D ", " D ", " S ");
        katana.setIngredient('D', new RecipeChoice.ExactChoice(createCraftingItem(RaijinCraftingItemType.DRAGON_SCALE)));
        katana.setIngredient('S', Material.STICK);
        Bukkit.addRecipe(katana);

        ShapedRecipe stormbow = new ShapedRecipe(recipeRaijinShortbowKey, createWeapon(CustomWeaponType.RAIJIN_SHORTBOW));
        stormbow.shape("FB ", "F T", "FB ");
        stormbow.setIngredient('F', new RecipeChoice.ExactChoice(createCraftingItem(RaijinCraftingItemType.BLOSSOM_FIBER)));
        stormbow.setIngredient('T', new RecipeChoice.ExactChoice(createCraftingItem(RaijinCraftingItemType.THUNDER_ESSENCE)));
        stormbow.setIngredient('B', Material.BOW);
        Bukkit.addRecipe(stormbow);

        ShapedRecipe sovereignAspect = new ShapedRecipe(recipeSovereignAspectKey, createWeapon(CustomWeaponType.SOVEREIGN_ASPECT));
        sovereignAspect.shape("SKS", "SKS", "DRD");
        sovereignAspect.setIngredient('S', Material.STRING);
        sovereignAspect.setIngredient('K', new RecipeChoice.ExactChoice(createEndMinesMaterial(EndMinesMaterialType.KUNZITE)));
        sovereignAspect.setIngredient('D', new RecipeChoice.ExactChoice(createCraftingItem(RaijinCraftingItemType.DRAGONS_SPINE)));
        sovereignAspect.setIngredient('R', new RecipeChoice.ExactChoice(createWeapon(CustomWeaponType.RIFTBREAKER)));
        Bukkit.addRecipe(sovereignAspect);

        registerDrillCraftingRecipes();
        registerDrillForgeRecipes();
        registerPersonalCompactorRecipes();

        for (Player online : Bukkit.getOnlinePlayers()) {
            discoverRecipes(online);
        }
    }

    public void discoverRecipes(Player player) {
        if (player == null) {
            return;
        }
        player.discoverRecipe(recipeFlyingRaijinKey);
        player.discoverRecipe(recipeHayabusaKey);
        player.discoverRecipe(recipeRaijinShortbowKey);
        player.discoverRecipe(recipeSovereignAspectKey);
        player.discoverRecipe(recipeDrillUpgradeKey);
        player.discoverRecipe(recipeVoltaKey);
        player.discoverRecipe(recipeOilBarrelKey);
        player.discoverRecipe(recipeMithrilEngineKey);
        player.discoverRecipe(recipeTitaniumEngineKey);
        player.discoverRecipe(recipeGemstoneEngineKey);
        player.discoverRecipe(recipeDivanEngineKey);
        player.discoverRecipe(recipeMediumFuelTankKey);
        player.discoverRecipe(recipeLargeFuelTankKey);
        player.discoverRecipe(recipeIroncrestDrillKey);
        player.discoverRecipe(recipeTitaniumDrillKey);
        player.discoverRecipe(recipeGemstoneDrillKey);
        player.discoverRecipe(recipePersonalCompactor3000Key);
        player.discoverRecipe(recipePersonalCompactor4000Key);
        player.discoverRecipe(farmRecipeKey);
    }

    private void registerDrillCraftingRecipes() {
        ItemStack oreFragment = createEndMinesMaterial(EndMinesMaterialType.ORE_FRAGMENT);
        if (oreFragment == null) {
            return;
        }

        // Ironcrest Drill (Basic starter drill) - The ONLY one craftable in a table
        ShapedRecipe ironcrestDrill = new ShapedRecipe(recipeIroncrestDrillKey, createMiningItem(MiningItemType.IRONCREST_DRILL));
        ironcrestDrill.shape("III", "OPO", " S ");
        ironcrestDrill.setIngredient('I', Material.IRON_INGOT);
        ironcrestDrill.setIngredient('O', new RecipeChoice.ExactChoice(oreFragment));
        ironcrestDrill.setIngredient('P', Material.PISTON);
        ironcrestDrill.setIngredient('S', Material.STICK);
        Bukkit.addRecipe(ironcrestDrill);
    }

    private void registerDrillForgeRecipes() {
        // Handled by DrillForgeManager's internal logic.
        // We do NOT register Bukkit ShapedRecipes here to ensure they are ONLY craftable in the Forge.
    }

    private void registerPersonalCompactorRecipes() {
        ItemStack enchantedRedstone = createMiningItem(MiningItemType.ENCHANTED_REDSTONE);
        ItemStack enchantedRedstoneBlock = createMiningItem(MiningItemType.ENCHANTED_REDSTONE_BLOCK);
        ItemStack enchantedCobble = createMiningItem(MiningItemType.ENCHANTED_COBBLESTONE);
        
        if (enchantedRedstone == null || enchantedRedstoneBlock == null || enchantedCobble == null) {
            return;
        }

        // Tier 3000 - 60 Enchanted Cobblestone + 10 Enchanted Redstone
        ShapedRecipe pc3000 = new ShapedRecipe(recipePersonalCompactor3000Key, createPersonalCompactor(PersonalCompactorType.PERSONAL_COMPACTOR_3000));
        pc3000.shape("CCC", "CRC", "CCC");
        
        ItemStack cobble7 = enchantedCobble.clone(); cobble7.setAmount(7);
        ItemStack cobble8 = enchantedCobble.clone(); cobble8.setAmount(8);
        ItemStack redstone10 = enchantedRedstone.clone(); redstone10.setAmount(10);
        
        // Split 60 into 4x7 and 4x8
        pc3000.setIngredient('C', new RecipeChoice.ExactChoice(cobble7, cobble8));
        pc3000.setIngredient('R', new RecipeChoice.ExactChoice(redstone10));
        Bukkit.addRecipe(pc3000);

        // Tier 4000 - 7 stacks of Enchanted Redstone
        ShapedRecipe pc4000 = new ShapedRecipe(recipePersonalCompactor4000Key, createPersonalCompactor(PersonalCompactorType.PERSONAL_COMPACTOR_4000));
        pc4000.shape("RRR", "R R", "RRR");
        ItemStack redstoneStack = enchantedRedstone.clone();
        redstoneStack.setAmount(64);
        pc4000.setIngredient('R', new RecipeChoice.ExactChoice(redstoneStack));
        Bukkit.addRecipe(pc4000);

        // Tier 5000 - 7 stacks of Enchanted Redstone Block + Tier 4000
        ShapedRecipe pc5000 = new ShapedRecipe(recipePersonalCompactor5000Key, createPersonalCompactor(PersonalCompactorType.PERSONAL_COMPACTOR_5000));
        pc5000.shape("BBB", "BMB", "BBB");
        ItemStack blockStack = enchantedRedstoneBlock.clone();
        blockStack.setAmount(64);
        pc5000.setIngredient('B', new RecipeChoice.ExactChoice(blockStack));
        pc5000.setIngredient('M', new RecipeChoice.ExactChoice(createPersonalCompactor(PersonalCompactorType.PERSONAL_COMPACTOR_4000)));
        Bukkit.addRecipe(pc5000);

        // Tier 6000 - 8 stacks of Enchanted Redstone Block + Tier 5000
        ShapedRecipe pc6000 = new ShapedRecipe(recipePersonalCompactor6000Key, createPersonalCompactor(PersonalCompactorType.PERSONAL_COMPACTOR_6000));
        pc6000.shape("BBB", "BMB", "BBB"); // Note: Shape is same but ingredient count is higher via ExactChoice if we used that, but Minecraft shaped recipes don't support >1 per slot easily.
        // Actually, we can just use 8 slots of blocks.
        pc6000.shape("BBB", "BMB", "BBB");
        pc6000.setIngredient('B', new RecipeChoice.ExactChoice(blockStack));
        pc6000.setIngredient('M', new RecipeChoice.ExactChoice(createPersonalCompactor(PersonalCompactorType.PERSONAL_COMPACTOR_5000)));
        Bukkit.addRecipe(pc6000);

        // Tier 7000 - 8 stacks of Enchanted Redstone Block + Tier 6000
        ShapedRecipe pc7000 = new ShapedRecipe(recipePersonalCompactor7000Key, createPersonalCompactor(PersonalCompactorType.PERSONAL_COMPACTOR_7000));
        pc7000.shape("BBB", "BMB", "BBB");
        pc7000.setIngredient('B', new RecipeChoice.ExactChoice(blockStack));
        pc7000.setIngredient('M', new RecipeChoice.ExactChoice(createPersonalCompactor(PersonalCompactorType.PERSONAL_COMPACTOR_6000)));
        Bukkit.addRecipe(pc7000);
    }

    public double mobWeaponDropChance(MonsterType type) {
        if (type == null) {
            return mobWeaponBaseChance;
        }
        return yokaiWeaponChances.getOrDefault(type, mobWeaponBaseChance);
    }

    public double mobArmorDropChance(MonsterType type) {
        if (type == null) {
            return mobArmorBaseChance;
        }
        return yokaiArmorChances.getOrDefault(type, mobArmorBaseChance);
    }

    public double mobReforgeStoneChance() {
        return mobReforgeStoneChance;
    }

    public double stormSigilChance() {
        return stormSigilChance;
    }

    public double thunderEssenceChance() {
        return thunderEssenceChance;
    }

    public double raijinCoreChance() {
        return raijinCoreChance;
    }

    public double bossArmorChance() {
        return bossArmorChance;
    }

    public double bossReforgeStoneChance() {
        return bossReforgeStoneChance;
    }

    public ItemStyle itemStyle() {
        return itemStyle;
    }

    public ItemStack createWeapon(CustomWeaponType type) {
        return switch (type) {
            case WARDENS_CLEAVER -> createWardensCleaver();
            case NEWBIE_KATANA -> weapon(
                    Material.IRON_SWORD,
                    ChatColor.GREEN + "Newbie Katana",
                    "NEWBIE_KATANA",
                    List.of(SB_DAMAGE + "20", SB_STRENGTH + "10"),
                    "Novice's Grace",
                    "Heals the wielder for 1 HP on every hit.",
                    0, 0,
                    ChatColor.GREEN + "" + ChatColor.BOLD + "UNCOMMON",
                    "KATANA",
                    Map.of(Enchantment.SHARPNESS, 2)
            );
            case ONI_CLEAVER -> weapon(
                    Material.IRON_AXE,
                    ChatColor.RED + "Oni Cleaver",
                    "ONI_CLEAVER",
                    List.of(SB_DAMAGE + "125", SB_STRENGTH + "45"),
                    "Demonic Cleave",
                    "Cleaves nearby enemies for 25% of the primary target's damage.",
                    0, 0,
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC",
                    "SWORD",
                    Map.of(Enchantment.SHARPNESS, 4, Enchantment.SWEEPING_EDGE, 2)
            );
            case TENGU_GALEBLADE -> weapon(
                    Material.DIAMOND_SWORD,
                    ChatColor.AQUA + "Tengu Galeblade",
                    "TENGU_GALEBLADE",
                    List.of(SB_DAMAGE + "110", SB_CRIT_CHANCE + "18%"),
                    "Mountain Wind",
                    "Grants +50 Speed for 5 seconds.",
                    40, 10.0,
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE",
                    "SWORD",
                    Map.of(Enchantment.SHARPNESS, 3, Enchantment.KNOCKBACK, 1)
            );
            case TENGU_STORMBOW -> weapon(
                    Material.BOW,
                    ChatColor.AQUA + "Tengu Stormbow",
                    "TENGU_STORMBOW",
                    List.of(SB_DAMAGE + "118", SB_CRIT_CHANCE + "22%"),
                    "Shrine Tempest",
                    "Arrows explode on impact, dealing area damage.",
                    0, 0,
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC",
                    "BOW",
                    Map.of(Enchantment.POWER, 6, Enchantment.PUNCH, 2, Enchantment.INFINITY, 1)
            );
            case TENGU_SHORTBOW -> weapon(
                    Material.BOW,
                    ChatColor.AQUA + "Tengu Shortbow",
                    "TENGU_SHORTBOW",
                    List.of(SB_DAMAGE + "112", SB_CRIT_CHANCE + "26%"),
                    "Rapid Fire",
                    "Right-click to fire instantly.",
                    0, 0,
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC",
                    "SHORTBOW",
                    Map.of(Enchantment.POWER, 5, Enchantment.PUNCH, 1, Enchantment.INFINITY, 1)
            );
            case KAPPA_TIDEBREAKER -> weapon(
                    Material.TRIDENT,
                    ChatColor.GREEN + "Kappa Tidebreaker",
                    "KAPPA_TIDEBREAKER",
                    List.of(SB_DAMAGE + "120", SB_SEA_FORCE + "30"),
                    "Moonlit Pool",
                    "Strikes targets with a burst of water damage.",
                    35, 2.0,
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC",
                    "TRIDENT",
                    Map.of(Enchantment.IMPALING, 5, Enchantment.LOYALTY, 1)
            );
            case ONRYO_SPIRITBLADE -> weapon(
                    Material.NETHERITE_SWORD,
                    ChatColor.GRAY + "Onryo Spiritblade",
                    "ONRYO_SPIRITBLADE",
                    List.of(SB_DAMAGE + "145", SB_FEROCITY + "12"),
                    "Vengeful Spirit",
                    "Deals double damage to the enemy that last hit you.",
                    0, 0,
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY",
                    "SWORD",
                    Map.of(Enchantment.SHARPNESS, 5, Enchantment.FIRE_ASPECT, 2)
            );
            case ONRYO_SHORTBOW -> weapon(
                    Material.BOW,
                    ChatColor.GRAY + "Onryo Phantom Shortbow",
                    "ONRYO_SHORTBOW",
                    List.of(SB_DAMAGE + "138", SB_CRIT_DAMAGE + "30%"),
                    "Ghostly Shot",
                    "Fires an instant phantom arrow that pierces enemies.",
                    0, 0,
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY",
                    "SHORTBOW",
                    Map.of(Enchantment.POWER, 6, Enchantment.PUNCH, 1, Enchantment.INFINITY, 1)
            );
            case JOROGUMO_STINGER -> weapon(
                    Material.STONE_SWORD,
                    ChatColor.DARK_GREEN + "Jorogumo Stinger",
                    "JOROGUMO_STINGER",
                    List.of(SB_DAMAGE + "95", SB_ATTACK_SPEED + "20%"),
                    "Venomous Silk",
                    "Slows and poisons enemies for 3 seconds.",
                    0, 0,
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE",
                    "SWORD",
                    Map.of(Enchantment.BANE_OF_ARTHROPODS, 5, Enchantment.LOOTING, 2)
            );
            case JOROGUMO_SHORTBOW -> weapon(
                    Material.BOW,
                    ChatColor.DARK_GREEN + "Jorogumo Silk Shortbow",
                    "JOROGUMO_SHORTBOW",
                    List.of(SB_DAMAGE + "104", SB_CRIT_CHANCE + "24%"),
                    "Webbed Arrow",
                    "Right-click to fire instantly and slow targets.",
                    0, 0,
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE",
                    "SHORTBOW",
                    Map.of(Enchantment.POWER, 5, Enchantment.PUNCH, 1, Enchantment.INFINITY, 1)
            );
            case KITSUNE_FANG -> weapon(
                    Material.GOLDEN_SWORD,
                    ChatColor.GOLD + "Kitsune Fang",
                    "KITSUNE_FANG",
                    List.of(SB_DAMAGE + "108", SB_MANA + "60"),
                    "Foxfire Step",
                    "Teleport 6 blocks forward and leave a flame trail.",
                    50, 1.0,
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC",
                    "SWORD",
                    Map.of(Enchantment.SHARPNESS, 4, Enchantment.FIRE_ASPECT, 2, Enchantment.LOOTING, 2)
            );
            case KITSUNE_DAWNBOW -> weapon(
                    Material.BOW,
                    ChatColor.GOLD + "Kitsune Dawnbow",
                    "KITSUNE_DAWNBOW",
                    List.of(SB_DAMAGE + "126", SB_CRIT_DAMAGE + "40%"),
                    "Solar Flare",
                    "Ignites all enemies in a 5 block radius.",
                    60, 15.0,
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY",
                    "BOW",
                    Map.of(Enchantment.POWER, 7, Enchantment.FLAME, 1, Enchantment.INFINITY, 1, Enchantment.PUNCH, 1)
            );
            case KITSUNE_SHORTBOW -> weapon(
                    Material.BOW,
                    ChatColor.GOLD + "Kitsune Shortbow",
                    "KITSUNE_SHORTBOW",
                    List.of(SB_DAMAGE + "124", SB_CRIT_DAMAGE + "36%"),
                    "Foxfire Volley",
                    "Right-click to fire instantly.",
                    0, 0,
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY",
                    "SHORTBOW",
                    Map.of(Enchantment.POWER, 6, Enchantment.FLAME, 1, Enchantment.INFINITY, 1, Enchantment.PUNCH, 1)
            );
            case GASHADOKURO_NODACHI -> weapon(
                    Material.NETHERITE_SWORD,
                    ChatColor.WHITE + "Gashadokuro Nodachi",
                    "GASHADOKURO_NODACHI",
                    List.of(SB_DAMAGE + "165", SB_STRENGTH + "70"),
                    "Bone Crush",
                    "Deals massive damage but reduces your health by 5%.",
                    0, 2.0,
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY",
                    "SWORD",
                    Map.of(Enchantment.SHARPNESS, 6, Enchantment.LOOTING, 3)
            );
            case FLYING_RAIJIN -> weapon(
                    Material.NETHERITE_SWORD,
                    ChatColor.YELLOW + "" + ChatColor.BOLD + "Flying Raijin",
                    "FLYING_RAIJIN",
                    List.of(SB_DAMAGE + "210", SB_STRENGTH + "100", SB_FEROCITY + "25"),
                    "Thunder Step",
                    "Dash forward and strike with lightning force.",
                    100, 0.5,
                    ChatColor.GOLD + "" + ChatColor.BOLD + "MYTHIC",
                    "SWORD",
                    Map.of(Enchantment.SHARPNESS, 7, Enchantment.LOOTING, 4, Enchantment.UNBREAKING, 5, Enchantment.MENDING, 1)
            );
            case HAYABUSA_KATANA -> weapon(
                    Material.NETHERITE_SWORD,
                    ChatColor.AQUA + "Hayabusa Katana",
                    "HAYABUSA_KATANA",
                    List.of(SB_DAMAGE + "175", SB_CRIT_CHANCE + "32%"),
                    "Aerial Strike",
                    "Increases damage by 50% while in the air.",
                    0, 0,
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC",
                    "SWORD",
                    Map.of(Enchantment.SHARPNESS, 6, Enchantment.SWEEPING_EDGE, 3, Enchantment.UNBREAKING, 5)
            );
            case RAIJIN_SHORTBOW -> weapon(
                    Material.BOW,
                    ChatColor.YELLOW + "Raijin Shortbow",
                    "RAIJIN_SHORTBOW",
                    List.of(SB_DAMAGE + "140", SB_CRIT_CHANCE + "30%"),
                    "Thunder Bolt",
                    "Right-click to fire thunder arrows instantly.",
                    0, 0,
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY",
                    "SHORTBOW",
                    Map.of(Enchantment.POWER, 6, Enchantment.INFINITY, 1, Enchantment.UNBREAKING, 4)
            );
            case DRAGON_HUNTER_SHORTBOW -> weapon(
                    Material.BOW,
                    ChatColor.GOLD + "Dragon Hunter Shortbow",
                    "DRAGON_HUNTER_SHORTBOW",
                    List.of(SB_DAMAGE + "310", SB_CRIT_CHANCE + "60%"),
                    "Dragon Tracker",
                    "Instantly fires 3 arrows that track the Ender Dragon!",
                    0, 0,
                    ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "MYTHIC",
                    "SHORTBOW",
                    Map.of(Enchantment.POWER, 7, Enchantment.INFINITY, 1, Enchantment.UNBREAKING, 10)
            );
            case RIFTBLADE -> weaponWithFlavor(
                    Material.IRON_SWORD,
                    ChatColor.BLUE + "Riftblade",
                    "RIFTBLADE",
                    List.of(
                            ChatColor.GRAY + "Damage: " + ChatColor.RED + "+85",
                            ChatColor.GRAY + "Strength: " + ChatColor.RED + "+25"
                    ),
                    "A blade infused with unstable End energy.",
                    "Rift Step",
                    "Teleport " + ChatColor.GREEN + "8 blocks " + ChatColor.GRAY + "forward and deal\n"
                            + ChatColor.RED + "150 damage " + ChatColor.GRAY + "to nearby enemies.",
                    20,
                    2.0D,
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE",
                    "SWORD",
                    Map.of()
            );
            case VOID_ASPECT_BLADE -> weaponWithFlavor(
                    Material.DIAMOND_SWORD,
                    ChatColor.DARK_PURPLE + "Void Aspect Blade",
                    "VOID_ASPECT_BLADE",
                    List.of(
                            ChatColor.GRAY + "Damage: " + ChatColor.RED + "+130",
                            ChatColor.GRAY + "Strength: " + ChatColor.RED + "+45",
                            ChatColor.GRAY + "Crit Chance: " + ChatColor.RED + "+10%"
                    ),
                    "A refined weapon that bends space\nto its wielder's will.",
                    "Void Shift",
                    "Teleport " + ChatColor.GREEN + "10 blocks " + ChatColor.GRAY + "forward and gain\n"
                            + ChatColor.RED + "+40% damage " + ChatColor.GRAY + "for " + ChatColor.GREEN + "3 seconds.",
                    40,
                    2.0D,
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC",
                    "SWORD",
                    Map.of()
            );
            case RIFTBREAKER -> weaponWithFlavor(
                    Material.NETHERITE_SWORD,
                    ChatColor.GOLD + "Riftbreaker",
                    "RIFTBREAKER",
                    List.of(
                            ChatColor.GRAY + "Damage: " + ChatColor.RED + "+185",
                            ChatColor.GRAY + "Strength: " + ChatColor.RED + "+70",
                            ChatColor.GRAY + "Crit Damage: " + ChatColor.RED + "+25%"
                    ),
                    "The fabric of reality fractures\nwith every swing.",
                    "Fracture Dash",
                    "Dash through enemies up to " + ChatColor.GREEN + "12 blocks" + ChatColor.GRAY + ",\n"
                            + "dealing " + ChatColor.RED + "350 damage " + ChatColor.GRAY + "and leaving a\n"
                            + "rift trail that deals " + ChatColor.RED + "150 damage" + ChatColor.GRAY + ".\n\n"
                            + "Each consecutive dash within " + ChatColor.GREEN + "5s\n"
                            + ChatColor.GRAY + "grants " + ChatColor.RED + "+10% damage " + ChatColor.GRAY + "("
                            + ChatColor.GREEN + "Max 5 stacks" + ChatColor.GRAY + ").",
                    60,
                    1.0D,
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY",
                    "SWORD",
                    Map.of()
            );
            case SOVEREIGN_ASPECT -> weaponWithFlavor(
                    Material.NETHERITE_SWORD,
                    ChatColor.LIGHT_PURPLE + "Sovereign Aspect",
                    "SOVEREIGN_ASPECT",
                    List.of(
                            ChatColor.GRAY + "Damage: " + ChatColor.RED + "+260",
                            ChatColor.GRAY + "Strength: " + ChatColor.RED + "+110",
                            ChatColor.GRAY + "Crit Damage: " + ChatColor.RED + "+40%",
                            ChatColor.GRAY + "Intelligence: " + ChatColor.GREEN + "+150"
                    ),
                    "A weapon wielded by those who\ncommand the End itself.",
                    "Dimensional Collapse",
                    "Teleport up to " + ChatColor.GREEN + "14 blocks " + ChatColor.GRAY + "and pull all\n"
                            + "nearby enemies to your location,\n"
                            + "then deal " + ChatColor.RED + "800 damage" + ChatColor.GRAY + ".\n\n"
                            + "Damage increases by " + ChatColor.RED + "+5% " + ChatColor.GRAY + "per enemy hit.",
                    120,
                    3.0D,
                    ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "MYTHIC",
                    "SWORD",
                    Map.of()
            );
            case VOIDFANG_DAGGER -> weaponWithFlavor(
                    Material.GOLDEN_SWORD,
                    ChatColor.GOLD + "Voidfang Dagger",
                    "VOIDFANG_DAGGER",
                    List.of(
                            ChatColor.GRAY + "Damage: " + ChatColor.RED + "+140",
                            ChatColor.GRAY + "Strength: " + ChatColor.RED + "+35",
                            ChatColor.GRAY + "Crit Chance: " + ChatColor.RED + "+20%",
                            ChatColor.GRAY + "Attack Speed: " + ChatColor.GREEN + "+25%"
                    ),
                    "Strikes faster than the eye can follow.",
                    "Shadow Chain",
                    "Teleport between up to " + ChatColor.GREEN + "4 enemies" + ChatColor.GRAY + ",\n"
                            + "dealing " + ChatColor.RED + "220 damage " + ChatColor.GRAY + "per hit.\n\n"
                            + "Each hit reduces the next hit damage\n"
                            + ChatColor.GRAY + "by " + ChatColor.RED + "10%." ,
                    50,
                    3.0D,
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY",
                    "DAGGER",
                    Map.of()
            );
            case WARP_BOW -> weaponWithFlavor(
                    Material.BOW,
                    ChatColor.BLUE + "Warp Bow",
                    "WARP_BOW",
                    List.of(ChatColor.GRAY + "Damage: " + ChatColor.RED + "+120"),
                    "A bow infused with teleportation magic.",
                    "Warp Shot",
                    "Shoot an arrow that teleports you\nto its impact location.",
                    30,
                    2.0D,
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE",
                    "BOW",
                    Map.of()
            );
            case VOIDSHOT_BOW -> weaponWithFlavor(
                    Material.BOW,
                    ChatColor.DARK_PURPLE + "Voidshot Bow",
                    "VOIDSHOT_BOW",
                    List.of(
                            ChatColor.GRAY + "Damage: " + ChatColor.RED + "+165",
                            ChatColor.GRAY + "Crit Chance: " + ChatColor.RED + "+10%"
                    ),
                    "Arrows pierce through reality itself.",
                    "Piercing Rift Arrow",
                    "Arrows pierce enemies and apply\n"
                            + "a stacking debuff increasing damage\n"
                            + "taken by " + ChatColor.RED + "5% " + ChatColor.GRAY + "("
                            + ChatColor.GREEN + "Max 5 stacks" + ChatColor.GRAY + ").",
                    40,
                    1.0D,
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC",
                    "BOW",
                    Map.of()
            );
            case RIFTSTORM_BOW -> weaponWithFlavor(
                    Material.BOW,
                    ChatColor.GOLD + "Riftstorm Bow",
                    "RIFTSTORM_BOW",
                    List.of(
                            ChatColor.GRAY + "Damage: " + ChatColor.RED + "+210",
                            ChatColor.GRAY + "Crit Damage: " + ChatColor.RED + "+30%"
                    ),
                    "Unleashes chaos from the void.",
                    "Arrow Storm",
                    "Fire a spread of " + ChatColor.GREEN + "5 arrows " + ChatColor.GRAY + "that deal\n"
                            + ChatColor.RED + "120% damage" + ChatColor.GRAY + ".\n\n"
                            + "Each hit increases your next ability\n"
                            + "damage by " + ChatColor.RED + "+10% " + ChatColor.GRAY + "("
                            + ChatColor.GREEN + "Max 5 stacks" + ChatColor.GRAY + ").",
                    70,
                    2.0D,
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY",
                    "BOW",
                    Map.of()
            );
            case ORBITAL_LONGBOW -> weaponWithFlavor(
                    Material.BOW,
                    ChatColor.LIGHT_PURPLE + "Orbital Longbow",
                    "ORBITAL_LONGBOW",
                    List.of(
                            ChatColor.GRAY + "Damage: " + ChatColor.RED + "+270",
                            ChatColor.GRAY + "Crit Damage: " + ChatColor.RED + "+50%"
                    ),
                    "The void bends to your aim.",
                    "Gravity Collapse",
                    "Mark an area on impact, pulling enemies\n"
                            + "inward before exploding for\n"
                            + ChatColor.RED + "900 damage" + ChatColor.GRAY + ".",
                    120,
                    4.0D,
                    ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "MYTHIC",
                    "BOW",
                    Map.of()
            );

            // Mage Weapons - Staffs
            case ARCANE_STAFF -> weapon(
                    Material.BLAZE_ROD,
                    ChatColor.DARK_PURPLE + "Arcane Staff",
                    "ARCANE_STAFF",
                    List.of(SB_DAMAGE + "85", SB_MANA + "120"),
                    "Arcane Blast",
                    "Fire a concentrated blast of arcane energy.",
                    35, 0.0,
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC",
                    "MAGE WEAPON",
                    Map.of(Enchantment.SHARPNESS, 3, Enchantment.UNBREAKING, 3)
            );
            case FROSTBITE_STAFF -> weapon(
                    Material.BLAZE_ROD,
                    ChatColor.AQUA + "Frostbite Staff",
                    "FROSTBITE_STAFF",
                    List.of(SB_DAMAGE + "95", SB_MANA + "140"),
                    "Frost Nova",
                    "Blast nearby enemies with freezing ice.",
                    45, 0.0,
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC",
                    "MAGE WEAPON",
                    Map.of(Enchantment.SHARPNESS, 4, Enchantment.UNBREAKING, 3)
            );
            case INFERNO_STAFF -> weapon(
                    Material.BLAZE_ROD,
                    ChatColor.RED + "Inferno Staff",
                    "INFERNO_STAFF",
                    List.of(SB_DAMAGE + "110", SB_MANA + "130"),
                    "Inferno Burst",
                    "Unleash a torrent of flames.",
                    55, 0.0,
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY",
                    "MAGE WEAPON",
                    Map.of(Enchantment.SHARPNESS, 5, Enchantment.FIRE_ASPECT, 2, Enchantment.UNBREAKING, 4)
            );
            case STORMCALLER_STAFF -> weapon(
                    Material.BLAZE_ROD,
                    ChatColor.YELLOW + "Stormcaller Staff",
                    "STORMCALLER_STAFF",
                    List.of(SB_DAMAGE + "100", SB_MANA + "150"),
                    "Chain Lightning",
                    "Lightning arcs between multiple enemies.",
                    50, 0.0,
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY",
                    "MAGE WEAPON",
                    Map.of(Enchantment.SHARPNESS, 4, Enchantment.UNBREAKING, 4)
            );
            case VOIDWALKER_STAFF -> weapon(
                    Material.END_ROD,
                    ChatColor.DARK_PURPLE + "Voidwalker Staff",
                    "VOIDWALKER_STAFF",
                    List.of(SB_DAMAGE + "120", SB_MANA + "160"),
                    "Void Rift",
                    "Open a rift that damages and pulls enemies.",
                    65, 0.0,
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY",
                    "MAGE WEAPON",
                    Map.of(Enchantment.SHARPNESS, 5, Enchantment.UNBREAKING, 4)
            );
            case CELESTIAL_STAFF -> weapon(
                    Material.END_ROD,
                    ChatColor.WHITE + "Celestial Staff",
                    "CELESTIAL_STAFF",
                    List.of(SB_DAMAGE + "130", SB_MANA + "180"),
                    "Starfall",
                    "Call down meteors from the heavens.",
                    80, 0.0,
                    ChatColor.RED + "" + ChatColor.BOLD + "MYTHIC",
                    "MAGE WEAPON",
                    Map.of(Enchantment.SHARPNESS, 6, Enchantment.UNBREAKING, 5)
            );

            // Mage Weapons - Wands
            case FLAME_WAND -> weapon(
                    Material.STICK,
                    ChatColor.RED + "Flame Wand",
                    "FLAME_WAND",
                    List.of(SB_DAMAGE + "55", SB_MANA + "70"),
                    "Fireball",
                    "Launch an explosive fireball.",
                    25, 0.0,
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE",
                    "WAND",
                    Map.of(Enchantment.SHARPNESS, 2, Enchantment.FIRE_ASPECT, 1)
            );
            case ICE_WAND -> weapon(
                    Material.STICK,
                    ChatColor.AQUA + "Ice Wand",
                    "ICE_WAND",
                    List.of(SB_DAMAGE + "50", SB_MANA + "75"),
                    "Ice Spike",
                    "Impale enemies with ice.",
                    22, 0.0,
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE",
                    "WAND",
                    Map.of(Enchantment.SHARPNESS, 2)
            );
            case LIGHTNING_WAND -> weapon(
                    Material.STICK,
                    ChatColor.YELLOW + "Lightning Wand",
                    "LIGHTNING_WAND",
                    List.of(SB_DAMAGE + "60", SB_MANA + "80"),
                    "Thunder Strike",
                    "Strike a target with lightning.",
                    28, 0.0,
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE",
                    "WAND",
                    Map.of(Enchantment.SHARPNESS, 3)
            );
            case POISON_WAND -> weapon(
                    Material.STICK,
                    ChatColor.DARK_GREEN + "Poison Wand",
                    "POISON_WAND",
                    List.of(SB_DAMAGE + "45", SB_MANA + "65"),
                    "Toxic Cloud",
                    "Create a poisonous gas cloud.",
                    20, 0.0,
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE",
                    "WAND",
                    Map.of(Enchantment.SHARPNESS, 2)
            );
            case HEALING_WAND -> weapon(
                    Material.BLAZE_ROD,
                    ChatColor.GREEN + "Healing Wand",
                    "HEALING_WAND",
                    List.of(SB_DAMAGE + "40", SB_MANA + "90"),
                    "Heal",
                    "Restore health to yourself and allies.",
                    30, 0.0,
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE",
                    "WAND",
                    Map.of(Enchantment.SHARPNESS, 1)
            );

            // Mage Weapons - Scepters
            case SCEPTER_OF_HEALING -> weapon(
                    Material.GOLDEN_SHOVEL,
                    ChatColor.GREEN + "Scepter of Healing",
                    "SCEPTER_OF_HEALING",
                    List.of(SB_DAMAGE + "65", SB_MANA + "110"),
                    "Mass Heal",
                    "Heal all nearby allies.",
                    50, 0.0,
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC",
                    "SCEPTER",
                    Map.of(Enchantment.SHARPNESS, 3, Enchantment.UNBREAKING, 3)
            );
            case SCEPTER_OF_DECAY -> weapon(
                    Material.GOLDEN_SHOVEL,
                    ChatColor.DARK_GREEN + "Scepter of Decay",
                    "SCEPTER_OF_DECAY",
                    List.of(SB_DAMAGE + "80", SB_MANA + "100"),
                    "Wither Storm",
                    "Unleash a storm of decay.",
                    45, 0.0,
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC",
                    "SCEPTER",
                    Map.of(Enchantment.SHARPNESS, 4, Enchantment.UNBREAKING, 3)
            );
            case SCEPTER_OF_MENDING -> weapon(
                    Material.GOLDEN_SHOVEL,
                    ChatColor.WHITE + "Scepter of Mending",
                    "SCEPTER_OF_MENDING",
                    List.of(SB_DAMAGE + "70", SB_MANA + "125"),
                    "Regeneration Aura",
                    "Grant regeneration to nearby allies.",
                    40, 0.0,
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC",
                    "SCEPTER",
                    Map.of(Enchantment.SHARPNESS, 3, Enchantment.UNBREAKING, 4)
            );
        };
    }

    public ItemStack createArmor(CustomArmorType type) {
        return switch (type) {
            // --- YOKAI ARMOR SETS ---
            case SHOGUN_KABUTO -> armor(
                    Material.NETHERITE_HELMET,
                    ChatColor.DARK_RED + "Shogun Kabuto",
                    "SHOGUN_KABUTO",
                    List.of(SB_HEALTH + "45", SB_DEFENSE + "22", SB_HEAL_SPEED + "4"),
                    "Warlord's Resolve",
                    "Increases damage dealt by 5% for each nearby ally.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY",
                    "HELMET",
                    Map.of(Enchantment.PROTECTION, 5, Enchantment.UNBREAKING, 4)
            );
            case SHOGUN_DO_MARU -> armor(
                    Material.NETHERITE_CHESTPLATE,
                    ChatColor.DARK_RED + "Shogun Do-Maru",
                    "SHOGUN_DO_MARU",
                    List.of(SB_HEALTH + "80", SB_DEFENSE + "34", SB_HEAL_SPEED + "6"),
                    "Warlord's Resolve",
                    "Increases damage dealt by 5% for each nearby ally.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY",
                    "CHESTPLATE",
                    Map.of(Enchantment.PROTECTION, 6, Enchantment.UNBREAKING, 4)
            );
            case SHOGUN_HAIDATE -> armor(
                    Material.NETHERITE_LEGGINGS,
                    ChatColor.DARK_RED + "Shogun Haidate",
                    "SHOGUN_HAIDATE",
                    List.of(SB_HEALTH + "62", SB_DEFENSE + "28", SB_HEAL_SPEED + "5"),
                    "Warlord's Resolve",
                    "Increases damage dealt by 5% for each nearby ally.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY",
                    "LEGGINGS",
                    Map.of(Enchantment.PROTECTION, 5, Enchantment.UNBREAKING, 4)
            );
            case SHOGUN_GETA -> armor(
                    Material.NETHERITE_BOOTS,
                    ChatColor.DARK_RED + "Shogun Geta",
                    "SHOGUN_GETA",
                    List.of(SB_HEALTH + "34", SB_DEFENSE + "18", SB_HEAL_SPEED + "3"),
                    "Warlord's Resolve",
                    "Increases damage dealt by 5% for each nearby ally.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY",
                    "BOOTS",
                    Map.of(Enchantment.PROTECTION, 5, Enchantment.FEATHER_FALLING, 4, Enchantment.UNBREAKING, 4)
            );

            case SHINOBI_MENPO -> armor(
                    Material.DIAMOND_HELMET,
                    ChatColor.DARK_AQUA + "Shinobi Menpo",
                    "SHINOBI_MENPO",
                    List.of(SB_HEALTH + "28", SB_DEFENSE + "16", SB_HEAL_SPEED + "8"),
                    "Shadowstep",
                    "Gain temporary Invisibility and Speed III after killing a mob.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC",
                    "HELMET",
                    Map.of(Enchantment.PROTECTION, 4, Enchantment.RESPIRATION, 3, Enchantment.UNBREAKING, 3)
            );
            case SHINOBI_JACKET -> armor(
                    Material.DIAMOND_CHESTPLATE,
                    ChatColor.DARK_AQUA + "Shinobi Jacket",
                    "SHINOBI_JACKET",
                    List.of(SB_HEALTH + "56", SB_DEFENSE + "24", SB_HEAL_SPEED + "9"),
                    "Shadowstep",
                    "Gain temporary Invisibility and Speed III after killing a mob.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC",
                    "CHESTPLATE",
                    Map.of(Enchantment.PROTECTION, 5, Enchantment.UNBREAKING, 3)
            );
            case SHINOBI_LEGGINGS -> armor(
                    Material.DIAMOND_LEGGINGS,
                    ChatColor.DARK_AQUA + "Shinobi Leggings",
                    "SHINOBI_LEGGINGS",
                    List.of(SB_HEALTH + "44", SB_DEFENSE + "20", SB_HEAL_SPEED + "8"),
                    "Shadowstep",
                    "Gain temporary Invisibility and Speed III after killing a mob.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC",
                    "LEGGINGS",
                    Map.of(Enchantment.PROTECTION, 5, Enchantment.UNBREAKING, 3)
            );
            case SHINOBI_TABI -> armor(
                    Material.DIAMOND_BOOTS,
                    ChatColor.DARK_AQUA + "Shinobi Tabi",
                    "SHINOBI_TABI",
                    List.of(SB_HEALTH + "24", SB_DEFENSE + "14", SB_HEAL_SPEED + "10"),
                    "Shadowstep",
                    "Gain temporary Invisibility and Speed III after killing a mob.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC",
                    "BOOTS",
                    Map.of(Enchantment.PROTECTION, 4, Enchantment.FEATHER_FALLING, 5, Enchantment.UNBREAKING, 3)
            );

            case ONMYOJI_EBOSHI -> armor(
                    Material.GOLDEN_HELMET,
                    ChatColor.GOLD + "Onmyoji Eboshi",
                    "ONMYOJI_EBOSHI",
                    List.of(SB_HEALTH + "40", SB_DEFENSE + "18", SB_HEAL_SPEED + "6", SB_MANA + "20"),
                    "Spirit Ward",
                    "Reduces mana cost of all abilities by 15%.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY",
                    "HELMET",
                    Map.of(Enchantment.PROTECTION, 5, Enchantment.UNBREAKING, 3)
            );
            case ONMYOJI_ROBE -> armor(
                    Material.GOLDEN_CHESTPLATE,
                    ChatColor.GOLD + "Onmyoji Robe",
                    "ONMYOJI_ROBE",
                    List.of(SB_HEALTH + "74", SB_DEFENSE + "29", SB_HEAL_SPEED + "8", SB_MANA + "40"),
                    "Spirit Ward",
                    "Reduces mana cost of all abilities by 15%.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY",
                    "CHESTPLATE",
                    Map.of(Enchantment.PROTECTION, 6, Enchantment.UNBREAKING, 4)
            );
            case ONMYOJI_HAKAMA -> armor(
                    Material.GOLDEN_LEGGINGS,
                    ChatColor.GOLD + "Onmyoji Hakama",
                    "ONMYOJI_HAKAMA",
                    List.of(SB_HEALTH + "56", SB_DEFENSE + "24", SB_HEAL_SPEED + "7", SB_MANA + "30"),
                    "Spirit Ward",
                    "Reduces mana cost of all abilities by 15%.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY",
                    "LEGGINGS",
                    Map.of(Enchantment.PROTECTION, 5, Enchantment.UNBREAKING, 4)
            );
            case ONMYOJI_SANDALS -> armor(
                    Material.GOLDEN_BOOTS,
                    ChatColor.GOLD + "Onmyoji Sandals",
                    "ONMYOJI_SANDALS",
                    List.of(SB_HEALTH + "30", SB_DEFENSE + "16", SB_HEAL_SPEED + "6", SB_MANA + "15"),
                    "Spirit Ward",
                    "Reduces mana cost of all abilities by 15%.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY",
                    "BOOTS",
                    Map.of(Enchantment.PROTECTION, 4, Enchantment.FEATHER_FALLING, 4, Enchantment.UNBREAKING, 3)
            );

            // --- END-GAME TANK SETS ---
            case TITAN_HELM -> armor(
                    Material.NETHERITE_HELMET,
                    ChatColor.GOLD + "Titan Helm",
                    "TITAN_HELM",
                    List.of(SB_HEALTH + "150", SB_DEFENSE + "45", SB_HEAL_SPEED + "8", SB_MANA + "25"),
                    "Colossal Barrier",
                    "Take 25% reduced damage from all sources.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.RED + "" + ChatColor.BOLD + "MYTHIC",
                    "HELMET",
                    Map.of(Enchantment.PROTECTION, 6, Enchantment.UNBREAKING, 5, Enchantment.MENDING, 1)
            );
            case TITAN_CHESTPLATE -> armor(
                    Material.NETHERITE_CHESTPLATE,
                    ChatColor.GOLD + "Titan Chestplate",
                    "TITAN_CHESTPLATE",
                    List.of(SB_HEALTH + "220", SB_DEFENSE + "68", SB_HEAL_SPEED + "12", SB_MANA + "40"),
                    "Colossal Barrier",
                    "Take 25% reduced damage from all sources.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.RED + "" + ChatColor.BOLD + "MYTHIC",
                    "CHESTPLATE",
                    Map.of(Enchantment.PROTECTION, 7, Enchantment.UNBREAKING, 5, Enchantment.MENDING, 1)
            );
            case TITAN_LEGGINGS -> armor(
                    Material.NETHERITE_LEGGINGS,
                    ChatColor.GOLD + "Titan Leggings",
                    "TITAN_LEGGINGS",
                    List.of(SB_HEALTH + "180", SB_DEFENSE + "56", SB_HEAL_SPEED + "10", SB_MANA + "30"),
                    "Colossal Barrier",
                    "Take 25% reduced damage from all sources.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.RED + "" + ChatColor.BOLD + "MYTHIC",
                    "LEGGINGS",
                    Map.of(Enchantment.PROTECTION, 6, Enchantment.UNBREAKING, 5, Enchantment.MENDING, 1)
            );
            case TITAN_BOOTS -> armor(
                    Material.NETHERITE_BOOTS,
                    ChatColor.GOLD + "Titan Boots",
                    "TITAN_BOOTS",
                    List.of(SB_HEALTH + "120", SB_DEFENSE + "38", SB_HEAL_SPEED + "8", SB_MANA + "20"),
                    "Colossal Barrier",
                    "Take 25% reduced damage from all sources.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.RED + "" + ChatColor.BOLD + "MYTHIC",
                    "BOOTS",
                    Map.of(Enchantment.PROTECTION, 5, Enchantment.FEATHER_FALLING, 5, Enchantment.UNBREAKING, 5, Enchantment.MENDING, 1)
            );

            case LEVIATHAN_HELM -> armor(
                    Material.DIAMOND_HELMET,
                    ChatColor.DARK_AQUA + "Leviathan Helm",
                    "LEVIATHAN_HELM",
                    List.of(SB_HEALTH + "140", SB_DEFENSE + "42", SB_HEAL_SPEED + "15", SB_MANA + "35"),
                    "Abyssal Bulwark",
                    "Grants permanent Water Breathing and Dolphin's Grace.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY",
                    "HELMET",
                    Map.of(Enchantment.PROTECTION, 5, Enchantment.RESPIRATION, 3, Enchantment.UNBREAKING, 4)
            );
            case LEVIATHAN_CHESTPLATE -> armor(
                    Material.DIAMOND_CHESTPLATE,
                    ChatColor.DARK_AQUA + "Leviathan Chestplate",
                    "LEVIATHAN_CHESTPLATE",
                    List.of(SB_HEALTH + "200", SB_DEFENSE + "64", SB_HEAL_SPEED + "20", SB_MANA + "50"),
                    "Abyssal Bulwark",
                    "Grants permanent Water Breathing and Dolphin's Grace.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY",
                    "CHESTPLATE",
                    Map.of(Enchantment.PROTECTION, 6, Enchantment.UNBREAKING, 4)
            );
            case LEVIATHAN_LEGGINGS -> armor(
                    Material.DIAMOND_LEGGINGS,
                    ChatColor.DARK_AQUA + "Leviathan Leggings",
                    "LEVIATHAN_LEGGINGS",
                    List.of(SB_HEALTH + "165", SB_DEFENSE + "52", SB_HEAL_SPEED + "18", SB_MANA + "45"),
                    "Abyssal Bulwark",
                    "Grants permanent Water Breathing and Dolphin's Grace.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY",
                    "LEGGINGS",
                    Map.of(Enchantment.PROTECTION, 5, Enchantment.UNBREAKING, 4)
            );
            case LEVIATHAN_BOOTS -> armor(
                    Material.DIAMOND_BOOTS,
                    ChatColor.DARK_AQUA + "Leviathan Boots",
                    "LEVIATHAN_BOOTS",
                    List.of(SB_HEALTH + "110", SB_DEFENSE + "35", SB_HEAL_SPEED + "14", SB_MANA + "30"),
                    "Abyssal Bulwark",
                    "Grants permanent Water Breathing and Dolphin's Grace.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY",
                    "BOOTS",
                    Map.of(Enchantment.PROTECTION, 5, Enchantment.FEATHER_FALLING, 4, Enchantment.UNBREAKING, 4)
            );

            case GUARDIAN_HELM -> armor(
                    Material.PLAYER_HEAD,
                    ChatColor.WHITE + "Guardian Helm",
                    "GUARDIAN_HELM",
                    List.of(SB_HEALTH + "125", SB_DEFENSE + "38", SB_HEAL_SPEED + "20", SB_MANA + "40"),
                    "Divine Protection",
                    "Gain a protective shield that absorbs 500 damage every 60 seconds.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC",
                    "HELMET",
                    Map.of(Enchantment.PROTECTION, 5, Enchantment.UNBREAKING, 4)
            );
            case GUARDIAN_CHESTPLATE -> armor(
                    Material.LEATHER_CHESTPLATE,
                    ChatColor.WHITE + "Guardian Chestplate",
                    "GUARDIAN_CHESTPLATE",
                    List.of(SB_HEALTH + "185", SB_DEFENSE + "58", SB_HEAL_SPEED + "30", SB_MANA + "60"),
                    "Divine Protection",
                    "Gain a protective shield that absorbs 500 damage every 60 seconds.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC",
                    "CHESTPLATE",
                    Map.of(Enchantment.PROTECTION, 6, Enchantment.UNBREAKING, 4)
            );
            case GUARDIAN_LEGGINGS -> armor(
                    Material.LEATHER_LEGGINGS,
                    ChatColor.WHITE + "Guardian Leggings",
                    "GUARDIAN_LEGGINGS",
                    List.of(SB_HEALTH + "150", SB_DEFENSE + "48", SB_HEAL_SPEED + "25", SB_MANA + "50"),
                    "Divine Protection",
                    "Gain a protective shield that absorbs 500 damage every 60 seconds.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC",
                    "LEGGINGS",
                    Map.of(Enchantment.PROTECTION, 5, Enchantment.UNBREAKING, 4)
            );
            case GUARDIAN_BOOTS -> armor(
                    Material.LEATHER_BOOTS,
                    ChatColor.WHITE + "Guardian Boots",
                    "GUARDIAN_BOOTS",
                    List.of(SB_HEALTH + "100", SB_DEFENSE + "30", SB_HEAL_SPEED + "18", SB_MANA + "35"),
                    "Divine Protection",
                    "Gain a protective shield that absorbs 500 damage every 60 seconds.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC",
                    "BOOTS",
                    Map.of(Enchantment.PROTECTION, 4, Enchantment.FEATHER_FALLING, 4, Enchantment.UNBREAKING, 4)
            );

            // --- UTILITY / MINER SETS ---
            case MINER_HELM -> armor(
                    Material.IRON_HELMET,
                    ChatColor.YELLOW + "Miner's Helmet",
                    "MINER_HELM",
                    List.of(SB_DEFENSE + "5"),
                    "Double Drop Chance",
                    "Grants a 10% chance to mine double resources.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.GREEN + "" + ChatColor.BOLD + "UNCOMMON",
                    "HELMET",
                    Map.of()
            );
            case MINER_CHESTPLATE -> armor(
                    Material.IRON_CHESTPLATE,
                    ChatColor.YELLOW + "Miner's Chestplate",
                    "MINER_CHESTPLATE",
                    List.of(SB_DEFENSE + "10"),
                    "Double Drop Chance",
                    "Grants a 10% chance to mine double resources.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.GREEN + "" + ChatColor.BOLD + "UNCOMMON",
                    "CHESTPLATE",
                    Map.of()
            );
            case MINER_LEGGINGS -> armor(
                    Material.IRON_LEGGINGS,
                    ChatColor.YELLOW + "Miner's Leggings",
                    "MINER_LEGGINGS",
                    List.of(SB_DEFENSE + "8"),
                    "Double Drop Chance",
                    "Grants a 10% chance to mine double resources.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.GREEN + "" + ChatColor.BOLD + "UNCOMMON",
                    "LEGGINGS",
                    Map.of()
            );
            case MINER_BOOTS -> armor(
                    Material.IRON_BOOTS,
                    ChatColor.YELLOW + "Miner's Boots",
                    "MINER_BOOTS",
                    List.of(SB_DEFENSE + "4"),
                    "Double Drop Chance",
                    "Grants a 10% chance to mine double resources.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.GREEN + "" + ChatColor.BOLD + "UNCOMMON",
                    "BOOTS",
                    Map.of()
            );

            case IRONCREST_HELM -> armor(
                    Material.DIAMOND_HELMET,
                    ChatColor.GOLD + "Ironcrest Helmet",
                    "IRONCREST_HELM",
                    List.of(SB_HEALTH + "50", SB_DEFENSE + "20"),
                    "Mine Defense",
                    "Grants +50 Defense while in a Mining Zone.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE",
                    "HELMET",
                    Map.of()
            );
            case IRONCREST_CHESTPLATE -> armor(
                    Material.DIAMOND_CHESTPLATE,
                    ChatColor.GOLD + "Ironcrest Chestplate",
                    "IRONCREST_CHESTPLATE",
                    List.of(SB_HEALTH + "80", SB_DEFENSE + "35"),
                    "Mine Defense",
                    "Grants +50 Defense while in a Mining Zone.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE",
                    "CHESTPLATE",
                    Map.of()
            );
            case IRONCREST_LEGGINGS -> armor(
                    Material.DIAMOND_LEGGINGS,
                    ChatColor.GOLD + "Ironcrest Leggings",
                    "IRONCREST_LEGGINGS",
                    List.of(SB_HEALTH + "65", SB_DEFENSE + "28"),
                    "Mine Defense",
                    "Grants +50 Defense while in a Mining Zone.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE",
                    "LEGGINGS",
                    Map.of()
            );
            case IRONCREST_BOOTS -> armor(
                    Material.DIAMOND_BOOTS,
                    ChatColor.GOLD + "Ironcrest Boots",
                    "IRONCREST_BOOTS",
                    List.of(SB_HEALTH + "40", SB_DEFENSE + "15"),
                    "Mine Defense",
                    "Grants +50 Defense while in a Mining Zone.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE",
                    "BOOTS",
                    Map.of()
            );

            case DEEPCORE_HELM -> armor(
                    Material.NETHERITE_HELMET,
                    ChatColor.DARK_PURPLE + "Deepcore Helmet",
                    "DEEPCORE_HELM",
                    List.of(SB_HEALTH + "100", SB_DEFENSE + "40"),
                    "Instant Break",
                    "Instantly breaks any mining material with Breaking Power 5 or less.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC",
                    "HELMET",
                    Map.of()
            );
            case DEEPCORE_CHESTPLATE -> armor(
                    Material.NETHERITE_CHESTPLATE,
                    ChatColor.DARK_PURPLE + "Deepcore Chestplate",
                    "DEEPCORE_CHESTPLATE",
                    List.of(SB_HEALTH + "160", SB_DEFENSE + "60"),
                    "Instant Break",
                    "Instantly breaks any mining material with Breaking Power 5 or less.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC",
                    "CHESTPLATE",
                    Map.of()
            );
            case DEEPCORE_LEGGINGS -> armor(
                    Material.NETHERITE_LEGGINGS,
                    ChatColor.DARK_PURPLE + "Deepcore Leggings",
                    "DEEPCORE_LEGGINGS",
                    List.of(SB_HEALTH + "130", SB_DEFENSE + "50"),
                    "Instant Break",
                    "Instantly breaks any mining material with Breaking Power 5 or less.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC",
                    "LEGGINGS",
                    Map.of()
            );
            case DEEPCORE_BOOTS -> armor(
                    Material.NETHERITE_BOOTS,
                    ChatColor.DARK_PURPLE + "Deepcore Boots",
                    "DEEPCORE_BOOTS",
                    List.of(SB_HEALTH + "80", SB_DEFENSE + "30"),
                    "Instant Break",
                    "Instantly breaks any mining material with Breaking Power 5 or less.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC",
                    "BOOTS",
                    Map.of()
            );

            // --- NEW DUNGEON SETS ---
            case RONIN_HELM -> armor(
                    Material.IRON_HELMET,
                    ChatColor.BLUE + "Ronin Helmet",
                    "RONIN_HELM",
                    List.of(SB_HEALTH + "30", SB_DEFENSE + "15", SB_MANA + "10"),
                    "Way of the Blade",
                    "Increases sword damage by 10%.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE",
                    "HELMET",
                    Map.of(Enchantment.PROTECTION, 3)
            );
            case RONIN_CHESTPLATE -> armor(
                    Material.IRON_CHESTPLATE,
                    ChatColor.BLUE + "Ronin Chestplate",
                    "RONIN_CHESTPLATE",
                    List.of(SB_HEALTH + "55", SB_DEFENSE + "25", SB_MANA + "15"),
                    "Way of the Blade",
                    "Increases sword damage by 10%.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE",
                    "CHESTPLATE",
                    Map.of(Enchantment.PROTECTION, 4)
            );
            case RONIN_LEGGINGS -> armor(
                    Material.IRON_LEGGINGS,
                    ChatColor.BLUE + "Ronin Leggings",
                    "RONIN_LEGGINGS",
                    List.of(SB_HEALTH + "45", SB_DEFENSE + "20", SB_MANA + "12"),
                    "Way of the Blade",
                    "Increases sword damage by 10%.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE",
                    "LEGGINGS",
                    Map.of(Enchantment.PROTECTION, 4)
            );
            case RONIN_BOOTS -> armor(
                    Material.IRON_BOOTS,
                    ChatColor.BLUE + "Ronin Boots",
                    "RONIN_BOOTS",
                    List.of(SB_HEALTH + "25", SB_DEFENSE + "12", SB_MANA + "8"),
                    "Way of the Blade",
                    "Increases sword damage by 10%.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE",
                    "BOOTS",
                    Map.of(Enchantment.PROTECTION, 3, Enchantment.FEATHER_FALLING, 2)
            );

            case KAPPA_GUARDIAN_HELM -> armor(
                    Material.DIAMOND_HELMET,
                    ChatColor.BLUE + "Kappa Guardian Helmet",
                    "KAPPA_GUARDIAN_HELM",
                    List.of(SB_HEALTH + "60", SB_DEFENSE + "28", SB_MANA + "20"),
                    "Hydraulic Pressure",
                    "Gain +1 Defense for every 10 blocks of depth.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE",
                    "HELMET",
                    Map.of(Enchantment.PROTECTION, 4, Enchantment.RESPIRATION, 3)
            );
            case KAPPA_GUARDIAN_CHESTPLATE -> armor(
                    Material.DIAMOND_CHESTPLATE,
                    ChatColor.BLUE + "Kappa Guardian Chestplate",
                    "KAPPA_GUARDIAN_CHESTPLATE",
                    List.of(SB_HEALTH + "100", SB_DEFENSE + "42", SB_MANA + "30"),
                    "Hydraulic Pressure",
                    "Gain +1 Defense for every 10 blocks of depth.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE",
                    "CHESTPLATE",
                    Map.of(Enchantment.PROTECTION, 5)
            );
            case KAPPA_GUARDIAN_LEGGINGS -> armor(
                    Material.DIAMOND_LEGGINGS,
                    ChatColor.BLUE + "Kappa Guardian Leggings",
                    "KAPPA_GUARDIAN_LEGGINGS",
                    List.of(SB_HEALTH + "80", SB_DEFENSE + "34", SB_MANA + "25"),
                    "Hydraulic Pressure",
                    "Gain +1 Defense for every 10 blocks of depth.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE",
                    "LEGGINGS",
                    Map.of(Enchantment.PROTECTION, 5)
            );
            case KAPPA_GUARDIAN_BOOTS -> armor(
                    Material.DIAMOND_BOOTS,
                    ChatColor.BLUE + "Kappa Guardian Boots",
                    "KAPPA_GUARDIAN_BOOTS",
                    List.of(SB_HEALTH + "50", SB_DEFENSE + "22", SB_MANA + "15"),
                    "Hydraulic Pressure",
                    "Gain +1 Defense for every 10 blocks of depth.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE",
                    "BOOTS",
                    Map.of(Enchantment.PROTECTION, 4, Enchantment.FEATHER_FALLING, 4)
            );

            case TENGU_MASTER_HELM -> armor(
                    Material.NETHERITE_HELMET,
                    ChatColor.GOLD + "Tengu Master Helmet",
                    "TENGU_MASTER_HELM",
                    List.of(SB_HEALTH + "85", SB_DEFENSE + "35", SB_MANA + "50"),
                    "High Altitude",
                    "Deals +15% damage while above Y=100.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY",
                    "HELMET",
                    Map.of(Enchantment.PROTECTION, 5, Enchantment.UNBREAKING, 3)
            );
            case TENGU_MASTER_CHESTPLATE -> armor(
                    Material.NETHERITE_CHESTPLATE,
                    ChatColor.GOLD + "Tengu Master Chestplate",
                    "TENGU_MASTER_CHESTPLATE",
                    List.of(SB_HEALTH + "140", SB_DEFENSE + "55", SB_MANA + "80"),
                    "High Altitude",
                    "Deals +15% damage while above Y=100.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY",
                    "CHESTPLATE",
                    Map.of(Enchantment.PROTECTION, 6, Enchantment.UNBREAKING, 4)
            );
            case TENGU_MASTER_LEGGINGS -> armor(
                    Material.NETHERITE_LEGGINGS,
                    ChatColor.GOLD + "Tengu Master Leggings",
                    "TENGU_MASTER_LEGGINGS",
                    List.of(SB_HEALTH + "110", SB_DEFENSE + "45", SB_MANA + "60"),
                    "High Altitude",
                    "Deals +15% damage while above Y=100.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY",
                    "LEGGINGS",
                    Map.of(Enchantment.PROTECTION, 5, Enchantment.UNBREAKING, 4)
            );
            case TENGU_MASTER_BOOTS -> armor(
                    Material.NETHERITE_BOOTS,
                    ChatColor.GOLD + "Tengu Master Boots",
                    "TENGU_MASTER_BOOTS",
                    List.of(SB_HEALTH + "70", SB_DEFENSE + "28", SB_MANA + "40"),
                    "High Altitude",
                    "Deals +15% damage while above Y=100.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY",
                    "BOOTS",
                    Map.of(Enchantment.PROTECTION, 5, Enchantment.FEATHER_FALLING, 5, Enchantment.UNBREAKING, 3)
            );

            case SKELETON_SOLDIER_HELM -> armor(
                    Material.LEATHER_HELMET,
                    ChatColor.WHITE + "Skeleton Soldier Helmet",
                    "SKELETON_SOLDIER_HELM",
                    List.of(SB_HEALTH + "20", SB_DEFENSE + "10", SB_MANA + "5"),
                    "Undead Soul",
                    "Reduces damage taken from undead mobs by 10%.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.WHITE + "" + ChatColor.BOLD + "COMMON",
                    "HELMET",
                    Map.of(Enchantment.PROTECTION, 1)
            );
            case SKELETON_SOLDIER_CHESTPLATE -> armor(
                    Material.LEATHER_CHESTPLATE,
                    ChatColor.WHITE + "Skeleton Soldier Chestplate",
                    "SKELETON_SOLDIER_CHESTPLATE",
                    List.of(SB_HEALTH + "40", SB_DEFENSE + "18", SB_MANA + "10"),
                    "Undead Soul",
                    "Reduces damage taken from undead mobs by 10%.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.WHITE + "" + ChatColor.BOLD + "COMMON",
                    "CHESTPLATE",
                    Map.of(Enchantment.PROTECTION, 2)
            );
            case SKELETON_SOLDIER_LEGGINGS -> armor(
                    Material.LEATHER_LEGGINGS,
                    ChatColor.WHITE + "Skeleton Soldier Leggings",
                    "SKELETON_SOLDIER_LEGGINGS",
                    List.of(SB_HEALTH + "32", SB_DEFENSE + "14", SB_MANA + "8"),
                    "Undead Soul",
                    "Reduces damage taken from undead mobs by 10%.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.WHITE + "" + ChatColor.BOLD + "COMMON",
                    "LEGGINGS",
                    Map.of(Enchantment.PROTECTION, 2)
            );
            case SKELETON_SOLDIER_BOOTS -> armor(
                    Material.LEATHER_BOOTS,
                    ChatColor.WHITE + "Skeleton Soldier Boots",
                    "SKELETON_SOLDIER_BOOTS",
                    List.of(SB_HEALTH + "15", SB_DEFENSE + "8", SB_MANA + "5"),
                    "Undead Soul",
                    "Reduces damage taken from undead mobs by 10%.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.WHITE + "" + ChatColor.BOLD + "COMMON",
                    "BOOTS",
                    Map.of(Enchantment.PROTECTION, 1)
            );
            case DRAGON_SLAYER_HELMET -> dragonSlayerArmor(
                    Material.PLAYER_HEAD,
                    "Dragon Slayer Helmet",
                    "DRAGON_SLAYER_HELMET",
                    List.of(SB_DEFENSE + "120", SB_HEALTH + "90"),
                    "Dragon Hunter",
                    List.of(ChatColor.GRAY + "Deal " + ChatColor.RED + "+15%" + ChatColor.GRAY + " more damage to End mobs."),
                    List.of(ChatColor.DARK_GRAY + "Forged from hardened Endstone and",
                            ChatColor.DARK_GRAY + "infused with dragon essence."),
                    "HELMET"
            );
            case DRAGON_SLAYER_CHESTPLATE -> dragonSlayerArmor(
                    Material.LEATHER_CHESTPLATE,
                    "Dragon Slayer Chestplate",
                    "DRAGON_SLAYER_CHESTPLATE",
                    List.of(SB_DEFENSE + "220", SB_HEALTH + "160"),
                    "Scales of the End",
                    List.of(ChatColor.GRAY + "Reduces damage taken from End mobs",
                            ChatColor.GRAY + "by " + ChatColor.GREEN + "10%" + ChatColor.GRAY + "."),
                    List.of(ChatColor.DARK_GRAY + "Crafted in the depths of the End Mines,",
                            ChatColor.DARK_GRAY + "where dragons once ruled."),
                    "CHESTPLATE"
            );
            case DRAGON_SLAYER_LEGGINGS -> dragonSlayerArmor(
                    Material.LEATHER_LEGGINGS,
                    "Dragon Slayer Leggings",
                    "DRAGON_SLAYER_LEGGINGS",
                    List.of(SB_DEFENSE + "180", SB_HEALTH + "130"),
                    "End Resilience",
                    List.of(ChatColor.GRAY + "Gain " + ChatColor.RED + "+25 " + ChatColor.RED + "Strength"
                            + ChatColor.GRAY + " while in the End."),
                    List.of(ChatColor.DARK_GRAY + "Empowered by fragments of fallen dragons."),
                    "LEGGINGS"
            );
            case DRAGON_SLAYER_BOOTS -> dragonSlayerArmor(
                    Material.LEATHER_BOOTS,
                    "Dragon Slayer Boots",
                    "DRAGON_SLAYER_BOOTS",
                    List.of(SB_DEFENSE + "110", SB_HEALTH + "80", SB_SPEED + "10"),
                    "Void Step",
                    List.of(ChatColor.GRAY + "Reduces fall damage and grants",
                            ChatColor.AQUA + "immunity to void knockback."),
                    List.of(ChatColor.DARK_GRAY + "Lightweight boots designed for",
                            ChatColor.DARK_GRAY + "navigating unstable End terrain."),
                    "BOOTS"
            );

            // --- SOULBOUND MAGE SET ---
            case SOULBOUND_HELM -> armor(
                    Material.NETHERITE_HELMET,
                    ChatColor.DARK_PURPLE + "Soulbound Helm",
                    "SOULBOUND_HELM",
                    List.of(SB_MANA + "250", SB_HEALTH + "20", SB_DEFENSE + "15", SB_HEAL_SPEED + "-5"),
                    "Pact of the Forbidden",
                    "Grants massive Ability Damage but drains your soul.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "MYTHIC",
                    "HELMET",
                    Map.of(Enchantment.PROTECTION, 10, Enchantment.UNBREAKING, 5)
            );
            case SOULBOUND_CHESTPLATE -> armor(
                    Material.NETHERITE_CHESTPLATE,
                    ChatColor.DARK_PURPLE + "Soulbound Chestplate",
                    "SOULBOUND_CHESTPLATE",
                    List.of(SB_MANA + "450", SB_HEALTH + "40", SB_DEFENSE + "25", SB_HEAL_SPEED + "-8"),
                    "Pact of the Forbidden",
                    "Grants massive Ability Damage but drains your soul.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "MYTHIC",
                    "CHESTPLATE",
                    Map.of(Enchantment.PROTECTION, 10, Enchantment.UNBREAKING, 5)
            );
            case SOULBOUND_LEGGINGS -> armor(
                    Material.NETHERITE_LEGGINGS,
                    ChatColor.DARK_PURPLE + "Soulbound Leggings",
                    "SOULBOUND_LEGGINGS",
                    List.of(SB_MANA + "350", SB_HEALTH + "30", SB_DEFENSE + "20", SB_HEAL_SPEED + "-6"),
                    "Pact of the Forbidden",
                    "Grants massive Ability Damage but drains your soul.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "MYTHIC",
                    "LEGGINGS",
                    Map.of(Enchantment.PROTECTION, 10, Enchantment.UNBREAKING, 5)
            );
            case SOULBOUND_BOOTS -> armor(
                    Material.NETHERITE_BOOTS,
                    ChatColor.DARK_PURPLE + "Soulbound Boots",
                    "SOULBOUND_BOOTS",
                    List.of(SB_MANA + "200", SB_HEALTH + "15", SB_DEFENSE + "12", SB_HEAL_SPEED + "-4"),
                    "Pact of the Forbidden",
                    "Grants massive Ability Damage but drains your soul.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "MYTHIC",
                    "BOOTS",
                    Map.of(Enchantment.PROTECTION, 10, Enchantment.UNBREAKING, 5)
            );

            // --- ROOKIE SAMURAI SET ---
            case ROOKIE_SAMURAI_KABUTO -> armor(
                    Material.CHAINMAIL_HELMET,
                    ChatColor.GREEN + "Rookie Samurai Kabuto",
                    "ROOKIE_SAMURAI_KABUTO",
                    List.of(SB_DEFENSE + "10", SB_HEALTH + "15", SB_HEAL_SPEED + "1", SB_MANA + "10"),
                    "Bushido Spirit",
                    "Increases Newbie Katana damage and healing.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.GREEN + "" + ChatColor.BOLD + "UNCOMMON",
                    "HELMET",
                    Map.of(Enchantment.PROTECTION, 2)
            );
            case ROOKIE_SAMURAI_DO -> armor(
                    Material.CHAINMAIL_CHESTPLATE,
                    ChatColor.GREEN + "Rookie Samurai Do",
                    "ROOKIE_SAMURAI_DO",
                    List.of(SB_DEFENSE + "15", SB_HEALTH + "25", SB_HEAL_SPEED + "1", SB_MANA + "15"),
                    "Bushido Spirit",
                    "Increases Newbie Katana damage and healing.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.GREEN + "" + ChatColor.BOLD + "UNCOMMON",
                    "CHESTPLATE",
                    Map.of(Enchantment.PROTECTION, 2)
            );
            case ROOKIE_SAMURAI_KOTE -> armor(
                    Material.CHAINMAIL_LEGGINGS,
                    ChatColor.GREEN + "Rookie Samurai Kote",
                    "ROOKIE_SAMURAI_KOTE",
                    List.of(SB_DEFENSE + "12", SB_HEALTH + "20", SB_HEAL_SPEED + "1", SB_MANA + "10"),
                    "Bushido Spirit",
                    "Increases Newbie Katana damage and healing.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.GREEN + "" + ChatColor.BOLD + "UNCOMMON",
                    "LEGGINGS",
                    Map.of(Enchantment.PROTECTION, 2)
            );
            case ROOKIE_SAMURAI_SUNEATE -> armor(
                    Material.CHAINMAIL_BOOTS,
                    ChatColor.GREEN + "Rookie Samurai Suneate",
                    "ROOKIE_SAMURAI_SUNEATE",
                    List.of(SB_DEFENSE + "8", SB_HEALTH + "10", SB_HEAL_SPEED + "1", SB_MANA + "5"),
                    "Bushido Spirit",
                    "Increases Newbie Katana damage and healing.",
                    fullSetBonusLore(type.setType()),
                    ChatColor.GREEN + "" + ChatColor.BOLD + "UNCOMMON",
                    "BOOTS",
                    Map.of(Enchantment.PROTECTION, 2)
            );
        };
    }

    private ItemStack createWardensCleaver() {
        List<String> lore = new ArrayList<>();
        lore.add(SB_DAMAGE + "30");
        lore.add(SB_STRENGTH + "12");
        lore.add("");
        lore.add(ChatColor.GOLD + "Passive: Cleave");
        lore.add(ChatColor.GRAY + "Hits up to 2 nearby enemies for 50% damage.");
        lore.add("");
        lore.add(ChatColor.GOLD + "Passive: Armor Break");
        lore.add(ChatColor.GRAY + "15% chance to reduce enemy armor by 10% for 5s.");
        lore.add("");
        lore.add(ChatColor.GOLD + "Passive: Bloodthirst");
        lore.add(ChatColor.GRAY + "Restore " + ChatColor.GREEN + "2 Health" + ChatColor.GRAY + " per hit.");
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "\"A relic of the spawn's first protector.");
        lore.add(ChatColor.DARK_GRAY + "One swing and you feel its wrath.\"");
        lore.add("");
        lore.add(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC SWORD");

        ItemStack item = build(
                Material.IRON_SWORD,
                ChatColor.RED + "Warden's Cleaver",
                "WARDENS_CLEAVER",
                lore,
                Map.of(),
                true
        );
        return applyCosmeticGlint(item);
    }

    public ItemStack createCraftingItem(RaijinCraftingItemType type) {
        return switch (type) {
            case STORM_SIGIL -> material(
                    Material.AMETHYST_SHARD,
                    ChatColor.AQUA + "Storm Sigil",
                    "STORM_SIGIL",
                    ChatColor.GRAY + "A charged crest dropped by temple bosses.",
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE CRAFTING MATERIAL"
            );
            case THUNDER_ESSENCE -> material(
                    Material.ECHO_SHARD,
                    ChatColor.YELLOW + "Thunder Essence",
                    "THUNDER_ESSENCE",
                    ChatColor.GRAY + "Compressed thunder from a fallen shogun.",
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC CRAFTING MATERIAL"
            );
            case RAIJIN_CORE -> material(
                    Material.HEART_OF_THE_SEA,
                    ChatColor.GOLD + "Raijin Core",
                    "RAIJIN_CORE",
                    ChatColor.GRAY + "The divine core required for Flying Raijin.",
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY CRAFTING MATERIAL"
            );
            case SUMMONING_EYE -> material(
                    Material.PLAYER_HEAD,
                    ChatColor.LIGHT_PURPLE + "Summoning Eye",
                    "SUMMONING_EYE",
                    ChatColor.GRAY + "Used at a Dragon Ascension altar point.\n"
                            + ChatColor.GRAY + "Place one at each marked pedestal to begin the ritual.",
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC SUMMONING ITEM"
            );
            case ASCENSION_SHARD -> material(
                    Material.END_CRYSTAL,
                    ChatColor.LIGHT_PURPLE + "Ascension Shard",
                    "ASCENSION_SHARD",
                    ChatColor.GRAY + "A crystallized sliver shed during a dragon ascension.",
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC DRAGON MATERIAL"
            );
            case DRAGON_HEART -> material(
                    Material.NETHER_STAR,
                    ChatColor.RED + "Dragon Heart",
                    "DRAGON_HEART",
                    ChatColor.GRAY + "A pulsing core torn from the strongest dragons of the End.",
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY DRAGON MATERIAL"
            );
            case DRAGONS_SPINE -> material(
                    Material.BONE,
                    ChatColor.GOLD + "Dragon's Spine",
                    "DRAGONS_SPINE",
                    ChatColor.GRAY + "Still warm from the dragon's fall.",
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY CRAFTING MATERIAL"
            );
            case DRAGON_SCALE -> material(
                    Material.PRISMARINE_SHARD,
                    ChatColor.DARK_GREEN + "Dragon Scale",
                    "DRAGON_SCALE",
                    ChatColor.GRAY + "A hardened scale from storm dragons.",
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC CRAFTING MATERIAL"
            );
            case BLOSSOM_FIBER -> material(
                    Material.STRING,
                    ChatColor.BLUE + "Blossom Fiber",
                    "BLOSSOM_FIBER",
                    ChatColor.GRAY + "Silk woven from sacred cherry blossoms.",
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE CRAFTING MATERIAL"
            );
            case ARCANE_CLOTH -> material(
                    Material.PURPLE_WOOL,
                    ChatColor.DARK_PURPLE + "Arcane Cloth",
                    "ARCANE_CLOTH",
                    ChatColor.GRAY + "Fabric humming with latent mana.",
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC CRAFTING MATERIAL"
            );
            case SOULTHREAD -> material(
                    Material.STRING,
                    ChatColor.AQUA + "Soulthread",
                    "SOULTHREAD",
                    ChatColor.GRAY + "Thread spun from wraith silk and soul energy.",
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE CRAFTING MATERIAL"
            );
            case STAR_PEARL -> material(
                    Material.ENDER_PEARL,
                    ChatColor.GOLD + "Star Pearl",
                    "STAR_PEARL",
                    ChatColor.GRAY + "A crystallized mote of astral mana.",
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC CRAFTING MATERIAL"
            );
            case GUARDIAN_FRAGMENT -> material(
                    Material.PLAYER_HEAD,
                    ChatColor.DARK_PURPLE + "Guardian Fragment",
                    "GUARDIAN_FRAGMENT",
                    ChatColor.GRAY + "A broken shard of a guardian's soul.\n" +
                    ChatColor.GRAY + "Can be used to craft " + ChatColor.WHITE + "Guardian Armor" + ChatColor.GRAY + ".",
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC CRAFTING MATERIAL"
            );
            case GOD_POTION -> material(
                    Material.DRAGON_BREATH,
                    ChatColor.GOLD + "" + ChatColor.BOLD + "God Potion",
                    "GOD_POTION",
                    ChatColor.GRAY + "A legendary concoction that grants\n" +
                    ChatColor.GRAY + "immense power for " + ChatColor.YELLOW + "24 hours" + ChatColor.GRAY + ".\n\n" +
                    ChatColor.WHITE + "\u272a " + ChatColor.RED + "Strength VII\n" +
                    ChatColor.WHITE + "\u272a " + ChatColor.AQUA + "Speed V\n" +
                    ChatColor.WHITE + "\u272a " + ChatColor.GREEN + "Jump Boost IV\n" +
                    ChatColor.WHITE + "\u272a " + ChatColor.BLUE + "Resistance III\n" +
                    ChatColor.WHITE + "\u272a " + ChatColor.GOLD + "Haste IV\n" +
                    ChatColor.WHITE + "\u272a " + ChatColor.LIGHT_PURPLE + "Regeneration IV\n\n" +
                    ChatColor.GRAY + "Effects persist through death!",
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY CONSUMABLE"
            );
        };
    }

    public ItemStack createEndMinesMaterial(EndMinesMaterialType type) {
        return switch (type) {
            case KUNZITE -> material(
                    Material.PINK_DYE,
                    ChatColor.LIGHT_PURPLE + "Kunzite",
                    "KUNZITE",
                    ChatColor.GRAY + "A luminous pink crystal harvested from the End Hub.",
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC CRAFTING MATERIAL"
            );
            case ENDSTONE_SHARD -> material(
                    Material.END_STONE,
                    ChatColor.YELLOW + "Endstone Shard",
                    "ENDSTONE_SHARD",
                    ChatColor.GRAY + "A shard chipped from dense End stone.",
                    ChatColor.GREEN + "" + ChatColor.BOLD + "UNCOMMON CRAFTING MATERIAL"
            );
            case RIFT_ESSENCE -> material(
                    Material.ECHO_SHARD,
                    ChatColor.DARK_PURPLE + "Rift Essence",
                    "RIFT_ESSENCE",
                    ChatColor.GRAY + "Unstable energy leaking from a cracked rift.",
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE CRAFTING MATERIAL"
            );
            case VOID_CRYSTAL -> material(
                    Material.AMETHYST_SHARD,
                    ChatColor.DARK_PURPLE + "Void Crystal",
                    "VOID_CRYSTAL",
                    ChatColor.GRAY + "A crystal that hums with void resonance.",
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC CRAFTING MATERIAL"
            );
            case OBSIDIAN_CORE -> material(
                    Material.OBSIDIAN,
                    ChatColor.DARK_GRAY + "Obsidian Core",
                    "OBSIDIAN_CORE",
                    ChatColor.GRAY + "A hardened core pulled from molten obsidian.",
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC CRAFTING MATERIAL"
            );
            case CHORUS_WEAVE -> material(
                    Material.STRING,
                    ChatColor.AQUA + "Chorus Weave",
                    "CHORUS_WEAVE",
                    ChatColor.GRAY + "Woven fibers that shift between dimensions.",
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE CRAFTING MATERIAL"
            );
            case ORE_FRAGMENT -> material(
                    Material.PRISMARINE_CRYSTALS,
                    ChatColor.GRAY + "Ore Fragment",
                    "ORE_FRAGMENT",
                    ChatColor.GRAY + "Dropped by mobs. Used in crafting upgraded gear.",
                    ChatColor.WHITE + "" + ChatColor.BOLD + "COMMON MATERIAL"
            );
        };
    }

    public ItemStack createMiningItem(MiningItemType type) {
        return switch (type) {
            case IRONCREST_DRILL -> createDrill("IRONCREST_DRILL", "Ironcrest Drill");
            case TITANIUM_DRILL -> createDrill("TITANIUM_DRILL", "Titanium Drill");
            case GEMSTONE_DRILL -> createDrill("GEMSTONE_DRILL", "Gemstone Drill");
            case MITHRIL_ENGINE -> material(
                    Material.PISTON,
                    ChatColor.BLUE + "Mithril Engine",
                    "MITHRIL_ENGINE",
                    drillEngineDescription("MITHRIL_ENGINE"),
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE DRILL PART"
            );
            case TITANIUM_ENGINE -> material(
                    Material.STICKY_PISTON,
                    ChatColor.DARK_PURPLE + "Titanium Engine",
                    "TITANIUM_ENGINE",
                    drillEngineDescription("TITANIUM_ENGINE"),
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC DRILL PART"
            );
            case GEMSTONE_ENGINE -> material(
                    Material.OBSERVER,
                    ChatColor.GOLD + "Gemstone Engine",
                    "GEMSTONE_ENGINE",
                    drillEngineDescription("GEMSTONE_ENGINE"),
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY DRILL PART"
            );
            case DIVAN_ENGINE -> material(
                    Material.NETHER_STAR,
                    ChatColor.LIGHT_PURPLE + "Divan Engine",
                    "DIVAN_ENGINE",
                    drillEngineDescription("DIVAN_ENGINE"),
                    ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "MYTHIC DRILL PART"
            );
            case MEDIUM_FUEL_TANK -> material(
                    Material.BUCKET,
                    ChatColor.BLUE + "Medium Fuel Tank",
                    "MEDIUM_FUEL_TANK",
                    drillTankDescription("MEDIUM_FUEL_TANK"),
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE DRILL PART"
            );
            case LARGE_FUEL_TANK -> material(
                    Material.MINECART,
                    ChatColor.DARK_PURPLE + "Large Fuel Tank",
                    "LARGE_FUEL_TANK",
                    drillTankDescription("LARGE_FUEL_TANK"),
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC DRILL PART"
            );
            case PROSPECTOR_COMPASS -> material(
                    Material.COMPASS,
                    ChatColor.AQUA + "Prospector's Compass",
                    "PROSPECTOR_COMPASS",
                    ChatColor.GRAY + "Right-click to reveal current Rich Vein ore type.",
                    ChatColor.GREEN + "" + ChatColor.BOLD + "UNCOMMON MINING ITEM"
            );
            case STABILITY_ANCHOR -> material(
                    Material.HEAVY_CORE,
                    ChatColor.DARK_PURPLE + "Stability Anchor",
                    "STABILITY_ANCHOR",
                    ChatColor.GRAY + "Reduces Deep Pressure penalty for a limited time.",
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC MINING CONSUMABLE"
            );
            case MINING_XP_SCROLL -> material(
                    Material.PAPER,
                    ChatColor.YELLOW + "Mining XP Scroll",
                    "MINING_XP_SCROLL",
                    ChatColor.GRAY + "Consumable that grants bonus Mining XP.",
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE CONSUMABLE"
            );
            case ORE_FRAGMENT_BUNDLE -> material(
                    Material.CHEST,
                    ChatColor.GRAY + "Ore Fragment Bundle",
                    "ORE_FRAGMENT_BUNDLE",
                    ChatColor.GRAY + "Contains a variety of ore fragments.",
                    ChatColor.GREEN + "" + ChatColor.BOLD + "UNCOMMON CONSUMABLE"
            );
            case TEMP_MINING_SPEED_BOOST -> material(
                    Material.GOLDEN_PICKAXE,
                    ChatColor.YELLOW + "Temporary Mining Speed Boost",
                    "TEMP_MINING_SPEED_BOOST",
                    ChatColor.GRAY + "Grants +100 Mining Speed for 5 minutes.",
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE CONSUMABLE"
            );
            case VOLTA -> material(
                    Material.PRISMARINE_SHARD,
                    ChatColor.AQUA + "Volta",
                    "VOLTA",
                    ChatColor.GRAY + "A high-energy fuel source for drills. Grants " + ChatColor.YELLOW + "5,000" + ChatColor.GRAY + " fuel.",
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE DRILL FUEL"
            );
            case OIL_BARREL -> material(
                    Material.CAULDRON,
                    ChatColor.GRAY + "Oil Barrel",
                    "OIL_BARREL",
                    ChatColor.GRAY + "A bulk storage of crude oil. Grants " + ChatColor.YELLOW + "10,000" + ChatColor.GRAY + " fuel.",
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE DRILL FUEL"
            );
            case SAPPHIRE -> material(
                    Material.BLUE_DYE,
                    ChatColor.AQUA + "Sapphire",
                    "SAPPHIRE",
                    ChatColor.GRAY + "A beautiful blue gemstone found deep underground.",
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE GEMSTONE"
            );
            case ENCHANTED_SAPPHIRE -> material(
                    Material.BLUE_DYE,
                    ChatColor.DARK_PURPLE + "Enchanted Sapphire",
                    "ENCHANTED_SAPPHIRE",
                    ChatColor.GRAY + "A highly compressed, magical sapphire.",
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC GEMSTONE"
            );
            case TITANIUM -> material(
                    Material.PLAYER_HEAD,
                    ChatColor.WHITE + "Titanium",
                    "TITANIUM",
                    ChatColor.GRAY + "A lightweight yet durable metal found in the Minehub.",
                    ChatColor.GREEN + "" + ChatColor.BOLD + "UNCOMMON MATERIAL"
            );
            case ENCHANTED_TITANIUM -> material(
                    Material.PLAYER_HEAD,
                    ChatColor.AQUA + "Refined Titanium",
                    "ENCHANTED_TITANIUM",
                    ChatColor.GRAY + "A highly compressed, refined titanium ingot.",
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE MATERIAL"
            );
            case TITANIUM_BLOCK -> material(
                    Material.IRON_BLOCK,
                    ChatColor.WHITE + "Titanium Block",
                    "TITANIUM_BLOCK",
                    ChatColor.GRAY + "A solid block of forged titanium.",
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE MATERIAL"
            );
            case ENCHANTED_TITANIUM_BLOCK -> material(
                    Material.DIAMOND_BLOCK,
                    ChatColor.AQUA + "Refined Titanium Block",
                    "ENCHANTED_TITANIUM_BLOCK",
                    ChatColor.GRAY + "A solid block of refined titanium.",
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC MATERIAL"
            );
            case ENCHANTED_REDSTONE -> material(
                    Material.REDSTONE,
                    ChatColor.GREEN + "Enchanted Redstone",
                    "ENCHANTED_REDSTONE",
                    ChatColor.GRAY + "A highly compressed, magical redstone dust.",
                    ChatColor.GREEN + "" + ChatColor.BOLD + "UNCOMMON MATERIAL"
            );
            case ENCHANTED_REDSTONE_BLOCK -> material(
                    Material.REDSTONE_BLOCK,
                    ChatColor.BLUE + "Enchanted Redstone Block",
                    "ENCHANTED_REDSTONE_BLOCK",
                    ChatColor.GRAY + "A solid block of enchanted redstone.",
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE MATERIAL"
            );
            case ENCHANTED_COBBLESTONE -> material(
                    Material.COBBLESTONE,
                    ChatColor.GREEN + "Enchanted Cobblestone",
                    "ENCHANTED_COBBLESTONE",
                    ChatColor.GRAY + "A highly compressed, magical cobblestone.",
                    ChatColor.GREEN + "" + ChatColor.BOLD + "UNCOMMON MATERIAL"
            );
        };
    }

    private ItemStack stackAmount(ItemStack stack, int amount) {
        if (stack == null || stack.getType().isAir()) {
            return stack;
        }
        ItemStack copy = stack.clone();
        copy.setAmount(Math.max(1, amount));
        return copy;
    }

    private ItemStack createDrill(String id, String fallbackName) {
        // Build a SkyBlock-style Drill with fuel PDC and lore.
        Material material = switch (id) {
            case "TITANIUM_DRILL" -> Material.DIAMOND_PICKAXE;
            case "GEMSTONE_DRILL" -> Material.NETHERITE_PICKAXE;
            default -> Material.IRON_PICKAXE;
        };
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        var pdc = meta.getPersistentDataContainer();

        // Default parts
        pdc.set(drillEngineKey, PersistentDataType.STRING, "BASIC_ENGINE");
        pdc.set(drillTankKey, PersistentDataType.STRING, "SMALL_TANK");

        int maxFuel = 20000;
        pdc.set(itemIdKey, PersistentDataType.STRING, id);
        pdc.set(drillFuelMaxKey, PersistentDataType.INTEGER, maxFuel);
        pdc.set(drillFuelKey, PersistentDataType.INTEGER, maxFuel);

        updateDrillLore(meta);
        ItemRarity rarity = rarityFromLore(meta.getLore(), ItemRarity.RARE);
        meta.setDisplayName(styledName(resolveDisplayName(id, fallbackName), rarity, true));
        meta.setUnbreakable(true);

        // Hide attributes to keep lore clean
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createReforgeStone(ReforgeStoneType type) {
        return switch (type) {
            case GENTLE_STONE -> material(
                    type.material(),
                    ChatColor.GREEN + "Gentle Stone",
                    "GENTLE_STONE",
                    ChatColor.GRAY + "Applies " + ReforgeType.GENTLE.color() + ReforgeType.GENTLE.displayName()
                            + ChatColor.GRAY + " to a dungeon weapon.",
                    ChatColor.BLUE + "" + ChatColor.BOLD + "UNCOMMON REFORGE STONE"
            );
            case ODD_STONE -> material(
                    type.material(),
                    ChatColor.YELLOW + "Odd Stone",
                    "ODD_STONE",
                    ChatColor.GRAY + "Applies " + ReforgeType.ODD.color() + ReforgeType.ODD.displayName()
                            + ChatColor.GRAY + " to a dungeon weapon.",
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE REFORGE STONE"
            );
            case FAST_STONE -> material(
                    type.material(),
                    ChatColor.AQUA + "Fast Stone",
                    "FAST_STONE",
                    ChatColor.GRAY + "Applies " + ReforgeType.FAST.color() + ReforgeType.FAST.displayName()
                            + ChatColor.GRAY + " to a dungeon weapon.",
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE REFORGE STONE"
            );
            case FAIR_STONE -> material(
                    type.material(),
                    ChatColor.LIGHT_PURPLE + "Fair Stone",
                    "FAIR_STONE",
                    ChatColor.GRAY + "Applies " + ReforgeType.FAIR.color() + ReforgeType.FAIR.displayName()
                            + ChatColor.GRAY + " to a dungeon weapon.",
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC REFORGE STONE"
            );
            case EPIC_STONE -> material(
                    type.material(),
                    ChatColor.DARK_PURPLE + "Epic Stone",
                    "EPIC_STONE",
                    ChatColor.GRAY + "Applies " + ReforgeType.EPIC.color() + ReforgeType.EPIC.displayName()
                            + ChatColor.GRAY + " to a dungeon weapon.",
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC REFORGE STONE"
            );
            case SHARP_STONE -> material(
                    type.material(),
                    ChatColor.BLUE + "Sharp Stone",
                    "SHARP_STONE",
                    ChatColor.GRAY + "Applies " + ReforgeType.SHARP.color() + ReforgeType.SHARP.displayName()
                            + ChatColor.GRAY + " to a dungeon weapon.",
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE REFORGE STONE"
            );
            case HEROIC_STONE -> material(
                    type.material(),
                    ChatColor.DARK_AQUA + "Heroic Stone",
                    "HEROIC_STONE",
                    ChatColor.GRAY + "Applies " + ReforgeType.HEROIC.color() + ReforgeType.HEROIC.displayName()
                            + ChatColor.GRAY + " to a dungeon weapon.",
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC REFORGE STONE"
            );
            case SPICY_STONE -> material(
                    type.material(),
                    ChatColor.RED + "Spicy Stone",
                    "SPICY_STONE",
                    ChatColor.GRAY + "Applies " + ReforgeType.SPICY.color() + ReforgeType.SPICY.displayName()
                            + ChatColor.GRAY + " to a dungeon weapon.",
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY REFORGE STONE"
            );
            case LEGENDARY_STONE -> material(
                    type.material(),
                    ChatColor.GOLD + "Legendary Stone",
                    "LEGENDARY_STONE",
                    ChatColor.GRAY + "Applies " + ReforgeType.LEGENDARY.color() + ReforgeType.LEGENDARY.displayName()
                            + ChatColor.GRAY + " to a dungeon weapon.",
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY REFORGE STONE"
            );
        };
    }

    public static CustomWeaponType getCustomWeaponType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(org.bukkit.Bukkit.getPluginManager().getPlugin("Grivience"), "item_id");
        String id = meta.getPersistentDataContainer().get(key, org.bukkit.persistence.PersistentDataType.STRING);
        return CustomWeaponType.parse(id);
    }

    public String itemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
    }

    public ReforgeType reforgeOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        String reforgeId = meta.getPersistentDataContainer().get(reforgeIdKey, PersistentDataType.STRING);
        return ReforgeType.parse(reforgeId);
    }

    public boolean isReforgable(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        // If it has a custom weapon ID, it's reforgable.
        if (CustomWeaponType.parse(itemId(item)) != null) {
            return true;
        }
        // If it's a vanilla weapon, it's reforgable.
        return isVanillaWeapon(item);
    }

    private boolean isVanillaWeapon(ItemStack item) {
        Material type = item.getType();
        String name = type.name();
        return name.endsWith("_SWORD") || name.endsWith("_AXE") || name.endsWith("_BOW") 
                || name.equals("CROSSBOW") || name.equals("TRIDENT");
    }

    public boolean isCustomDungeonWeapon(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        return CustomWeaponType.parse(itemId(item)) != null;
    }

    public ItemStack applyReforge(ItemStack baseWeapon, ReforgeType reforgeType) {
        if (baseWeapon == null || reforgeType == null || !baseWeapon.hasItemMeta()) {
            return baseWeapon;
        }

        ItemStack result = baseWeapon.clone();
        ItemMeta meta = result.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(reforgeIdKey, PersistentDataType.STRING, reforgeType.name());

        ItemRarity rarity = rarityFromLore(meta.getLore(), ItemRarity.RARE);
        ChatColor rarityColor = rarity == null ? ChatColor.WHITE : rarity.color();

        String storedBaseName = meta.getPersistentDataContainer().get(reforgeBaseNameKey, PersistentDataType.STRING);
        String baseName = normalizeBaseName(storedBaseName);
        if (baseName == null || baseName.isBlank()) {
            baseName = normalizeBaseName(meta.hasDisplayName() ? meta.getDisplayName() : result.getType().name());
        }
        if (baseName == null || baseName.isBlank()) {
            baseName = result.getType().name();
        }
        meta.getPersistentDataContainer().set(reforgeBaseNameKey, PersistentDataType.STRING, baseName);
        meta.setDisplayName(rarityColor + reforgeType.displayName() + " " + baseName);

        List<String> lore = meta.hasLore() && meta.getLore() != null
                ? new ArrayList<>(meta.getLore())
                : new ArrayList<>();
        lore.removeIf(this::isReforgeLoreLine);
        lore.add(ChatColor.DARK_GRAY + "Reforge: " + reforgeType.color() + reforgeType.displayName());
        lore.add(ChatColor.DARK_GRAY + "Bonus: " + reforgeBonusLine(reforgeType, result));
        meta.setLore(lore);
        result.setItemMeta(meta);
        return syncWeaponEnchantLore(result);
    }

    public ItemStack syncWeaponEnchantLore(ItemStack weapon) {
        if (!isReforgable(weapon) || !weapon.hasItemMeta()) {
            return weapon;
        }
        ItemStack updated = weapon.clone();
        ItemMeta meta = updated.getItemMeta();
        List<String> lore = meta.hasLore() && meta.getLore() != null
                ? new ArrayList<>(meta.getLore())
                : new ArrayList<>();
        lore.removeIf(this::isEnchantLoreLine);
        lore.addAll(enchantInsertionIndex(lore), enchantLoreLines(updated.getEnchantments()));
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        updated.setItemMeta(meta);
        return updated;
    }

    public ReforgeStats reforgeStats(ReforgeType type, ItemStack weapon) {
        if (type == null) {
            return new ReforgeStats(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        ReforgeType.ReforgeStatProfile profile = type.statsFor(rarityOf(weapon));
        return new ReforgeStats(
                profile.damageBonus(),
                profile.strengthBonus(),
                profile.critChanceBonus(),
                profile.critDamageBonus(),
                profile.attackSpeedBonus(),
                profile.intelligenceBonus()
        );
    }

    public String reforgeBonusLine(ReforgeType type, ItemStack weapon) {
        if (type == null) {
            return ChatColor.GRAY + "No reforge";
        }
        ReforgeStats stats = reforgeStats(type, weapon);
        List<String> parts = new ArrayList<>();
        appendBonusPart(parts, stats.damageBonus(), " Damage", ChatColor.RED);
        appendBonusPart(parts, stats.strengthBonus(), " Strength", ChatColor.RED);
        appendBonusPart(parts, stats.critChanceBonus(), "% Crit Chance", ChatColor.BLUE);
        appendBonusPart(parts, stats.critDamageBonus(), "% Crit Damage", ChatColor.BLUE);
        appendBonusPart(parts, stats.attackSpeedBonus(), "% Attack Speed", ChatColor.GOLD);
        appendBonusPart(parts, stats.intelligenceBonus(), " Intelligence", ChatColor.AQUA);
        if (parts.isEmpty()) {
            return ChatColor.GRAY + "No bonus";
        }
        return String.join(ChatColor.GRAY + ", ", parts);
    }

    public String reforgeBonusLine(ReforgeType type) {
        return reforgeBonusLine(type, null);
    }

    public ItemStack createItemByKey(String key) {
        AccessoryType accessoryType = AccessoryType.parse(key);
        if (accessoryType != null) {
            return createAccessory(accessoryType);
        }

        CustomWeaponType weaponType = CustomWeaponType.parse(key);
        if (weaponType != null) {
            return createWeapon(weaponType);
        }

        CustomArmorType armorType = CustomArmorType.parse(key);
        if (armorType != null) {
            return createArmor(armorType);
        }

        RaijinCraftingItemType materialType = RaijinCraftingItemType.parse(key);
        if (materialType != null) {
            return createCraftingItem(materialType);
        }

        EndMinesMaterialType endMinesMaterialType = EndMinesMaterialType.parse(key);
        if (endMinesMaterialType != null) {
            return createEndMinesMaterial(endMinesMaterialType);
        }

        ReforgeStoneType reforgeStoneType = ReforgeStoneType.parse(key);
        if (reforgeStoneType != null) {
            return createReforgeStone(reforgeStoneType);
        }

        EnchantedFarmItemType farm = EnchantedFarmItemType.parse(key);
        if (farm != null) {
            return createEnchantedFarmItem(farm);
        }

        MiningItemType mining = MiningItemType.parse(key);
        if (mining != null) {
            return createMiningItem(mining);
        }

        PersonalCompactorType compactor = PersonalCompactorType.parse(key);
        if (compactor != null) {
            return createPersonalCompactor(compactor);
        }

        GrapplingHookType hook = GrapplingHookType.parse(key);
        if (hook != null) {
            return createGrapplingHook(hook);
        }

        CustomToolType tool = CustomToolType.parse(key);
        if (tool != null) {
            return createTool(tool);
        }
        return null;
    }

    public ItemStack createTool(CustomToolType type) {
        return switch (type) {
            // --- PICKAXES ---
            case IRONCREST_PICKAXE -> weapon(
                    Material.IRON_PICKAXE,
                    ChatColor.GOLD + "Ironcrest Pickaxe",
                    "IRONCREST_PICKAXE",
                    List.of(SB_ATTACK_SPEED + "10%", ChatColor.GRAY + "Mining Speed: " + ChatColor.GREEN + "+250"),
                    "Ore Seeker",
                    "Grants +20 Mining Fortune for 10s after breaking an ore.",
                    0, 30.0,
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE",
                    "PICKAXE",
                    Map.of(Enchantment.EFFICIENCY, 5, Enchantment.FORTUNE, 2)
            );
            case VOID_STEEL_PICKAXE -> weapon(
                    Material.NETHERITE_PICKAXE,
                    ChatColor.DARK_PURPLE + "Void-Steel Pickaxe",
                    "VOID_STEEL_PICKAXE",
                    List.of(SB_STRENGTH + "20", ChatColor.GRAY + "Mining Speed: " + ChatColor.GREEN + "+400"),
                    "Dimension Break",
                    "Deals double damage to End-based mining blocks.",
                    0, 0,
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC",
                    "PICKAXE",
                    Map.of(Enchantment.EFFICIENCY, 6, Enchantment.FORTUNE, 3)
            );
            case TITAN_BREAKER -> weapon(
                    Material.NETHERITE_PICKAXE,
                    ChatColor.RED + "Titan Breaker",
                    "TITAN_BREAKER",
                    List.of(SB_STRENGTH + "50", ChatColor.GRAY + "Mining Speed: " + ChatColor.GREEN + "+600"),
                    "Shatter",
                    "Instantly breaks any block with Breaking Power 7 or less.",
                    100, 10.0,
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY",
                    "PICKAXE",
                    Map.of(Enchantment.EFFICIENCY, 7, Enchantment.FORTUNE, 4)
            );

            // --- FARMING TOOLS ---
            case GILDED_HOE -> weapon(
                    Material.GOLDEN_HOE,
                    ChatColor.GOLD + "Gilded Hoe",
                    "GILDED_HOE",
                    List.of(ChatColor.GRAY + "Farming Fortune: " + ChatColor.GOLD + "+40"),
                    "Midas Touch",
                    "Small chance to find Gold Nuggets while harvesting crops.",
                    0, 0,
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE",
                    "HOE",
                    Map.of(Enchantment.UNBREAKING, 3)
            );
            case NEWTONIAN_HOE -> weapon(
                    Material.DIAMOND_HOE,
                    ChatColor.AQUA + "Newtonian Hoe",
                    "NEWTONIAN_HOE",
                    List.of(ChatColor.GRAY + "Farming Fortune: " + ChatColor.GOLD + "+80"),
                    "Gravity Harvest",
                    "Harvests crops in a 3x3 area around the broken block.",
                    20, 0,
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC",
                    "HOE",
                    Map.of(Enchantment.UNBREAKING, 5)
            );
            case GAIA_SCYTHE -> weapon(
                    Material.NETHERITE_HOE,
                    ChatColor.GREEN + "Gaia Scythe",
                    "GAIA_SCYTHE",
                    List.of(ChatColor.GRAY + "Farming Fortune: " + ChatColor.GOLD + "+150", SB_HEALTH + "100"),
                    "Earth's Blessing",
                    "Regenerates 1% of your max HP per crop harvested.",
                    0, 0,
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY",
                    "HOE",
                    Map.of(Enchantment.UNBREAKING, 10)
            );
        };
    }

    public ItemStack createGrapplingHook(GrapplingHookType type) {
        return weapon(
                Material.FISHING_ROD,
                type.getDisplayName(),
                type.getId(),
                List.of(), // No base stats for hook
                "Grapple",
                "Travel around in style.\nFire a hook and pull yourself\ntowards it!",
                0, 2.0,
                ChatColor.BLUE + "" + ChatColor.BOLD + "RARE",
                "ITEM",
                Map.of()
        );
    }

    public ItemStack createAccessory(AccessoryType type) {
        if (type == null) {
            return null;
        }

        List<String> lore = new ArrayList<>();
        appendAccessoryStat(lore, "Health", type.health(), ChatColor.RED, false);
        appendAccessoryStat(lore, "Defense", type.defense(), ChatColor.GREEN, false);
        appendAccessoryStat(lore, "Strength", type.strength(), ChatColor.RED, false);
        appendAccessoryStat(lore, "Crit Chance", type.critChance(), ChatColor.BLUE, true);
        appendAccessoryStat(lore, "Crit Damage", type.critDamage(), ChatColor.BLUE, true);
        appendAccessoryStat(lore, "Intelligence", type.intelligence(), ChatColor.AQUA, false);
        appendAccessoryStat(lore, "Farming Fortune", type.farmingFortune(), ChatColor.GOLD, false);

        lore.add("");
        int basePower = io.papermc.Grivience.accessory.AccessoryPower.calculatePower(type.rarity(), io.papermc.Grivience.accessory.AccessoryEnrichment.NONE);
        lore.add(ChatColor.GRAY + "Magical Power: " + ChatColor.AQUA + basePower);
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "Category: " + ChatColor.GRAY + type.category().displayName());
        lore.add(ChatColor.DARK_GRAY + "Family: " + ChatColor.GRAY + capitalize(type.family()));
        lore.add(ChatColor.GRAY + type.flavor());
        lore.add("");
        lore.add(ChatColor.YELLOW + "Unique Accessory: " + ChatColor.GRAY + "Only the highest tier");
        lore.add(ChatColor.GRAY + "of this family grants bonuses.");
        lore.add("");
        lore.add(type.rarity().color() + "" + ChatColor.BOLD + type.rarity().name() + " ACCESSORY");

        return build(
                type.material(),
                type.displayName(),
                type.id(),
                lore,
                Map.of(),
                false,
                false
        );
    }

    private void appendAccessoryStat(List<String> lore, String label, double value, ChatColor valueColor, boolean percent) {
        int rounded = (int) Math.round(value);
        if (rounded == 0) {
            return;
        }
        String suffix = percent ? "%" : "";
        lore.add(ChatColor.GRAY + label + ": " + valueColor + "+" + rounded + suffix);
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "Unknown";
        }
        String normalized = value.replace('_', ' ').replace('-', ' ').toLowerCase(Locale.ROOT);
        String[] words = normalized.split("\\s+");
        List<String> parts = new ArrayList<>(words.length);
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            parts.add(Character.toUpperCase(word.charAt(0)) + word.substring(1));
        }
        return String.join(" ", parts);
    }

    public List<String> allItemKeys() {
        List<String> keys = new ArrayList<>();
        for (AccessoryType type : AccessoryType.values()) {
            keys.add(type.id().toLowerCase(Locale.ROOT));
        }
        for (CustomWeaponType type : CustomWeaponType.values()) {
            keys.add(type.name().toLowerCase());
        }
        for (CustomArmorType type : CustomArmorType.values()) {
            keys.add(type.name().toLowerCase());
        }
        for (RaijinCraftingItemType type : RaijinCraftingItemType.values()) {
            keys.add(type.name().toLowerCase());
        }
        for (EndMinesMaterialType type : EndMinesMaterialType.values()) {
            keys.add(type.name().toLowerCase());
        }
        for (ReforgeStoneType type : ReforgeStoneType.values()) {
            keys.add(type.name().toLowerCase());
        }
        for (EnchantedFarmItemType type : EnchantedFarmItemType.values()) {
            keys.add(type.id().toLowerCase());
        }
        for (MiningItemType type : MiningItemType.values()) {
            keys.add(type.name().toLowerCase());
        }
        for (PersonalCompactorType type : PersonalCompactorType.values()) {
            keys.add(type.id().toLowerCase(Locale.ROOT));
        }
        for (GrapplingHookType type : GrapplingHookType.values()) {
            keys.add(type.getId().toLowerCase());
        }
        for (CustomToolType type : CustomToolType.values()) {
            keys.add(type.name().toLowerCase());
        }
        return keys;
    }

    public ItemStack createEnchantedFarmItem(EnchantedFarmItemType type) {
        String rarity = type.isTierTwo() ? ChatColor.BLUE + "" + ChatColor.BOLD + "RARE" : ChatColor.GREEN + "" + ChatColor.BOLD + "UNCOMMON";
        rarity += " CRAFTING MATERIAL";
        
        return material(
                guessMaterial(type),
                type.displayName(),
                type.id(),
                "Compressed farm goods.\nUse in recipes or bazaar.",
                rarity
        );
    }

    public ItemStack createPersonalCompactor(PersonalCompactorType type) {
        if (type == null) {
            return null;
        }
        List<String> lore = new ArrayList<>();
        String description = "Automatically compacts selected items in your inventory.\n"
                + "Right-click to configure compacting slots.\n"
                + "Slots: " + type.slots();
        for (String line : description.split("\n")) {
            lore.add(ChatColor.GRAY + line);
        }
        lore.add("");
        lore.add(type.rarity() + " ITEM");
        return build(
                type.material(),
                ChatColor.GOLD + type.displayName(),
                type.id(),
                lore,
                Map.of(Enchantment.UNBREAKING, 1),
                true,
                true
        );
    }

    private Material guessMaterial(EnchantedFarmItemType type) {
        if (type.baseMaterial() != null) {
            return type.baseMaterial();
        }
        // Tier 2 use a representative material
        return switch (type) {
            case ENCHANTED_HAY_BALE -> Material.HAY_BLOCK;
            case ENCHANTED_BAKED_POTATO -> Material.BAKED_POTATO;
            case ENCHANTED_SUGAR_CANE -> Material.SUGAR_CANE;
            case ENCHANTED_GLISTERING_MELON -> Material.GLISTERING_MELON_SLICE;
            case ENCHANTED_CACTUS_GREEN -> Material.GREEN_DYE;
            default -> Material.PAPER;
        };
    }

    private ItemStack weapon(
            Material material,
            String name,
            String id,
            List<String> stats,
            String abilityName,
            String abilityDesc,
            int manaCost,
            double cooldown,
            String rarity,
            String weaponType,
            Map<Enchantment, Integer> enchants
    ) {
        List<String> lore = new ArrayList<>();
        
        // 1. Stats
        for (String stat : stats) {
            if (stat != null && !stat.isEmpty()) {
                lore.add(stat);
            }
        }
        
        // 2. Enchants
        List<String> enchantLines = enchantLoreLines(enchants);
        if (!enchantLines.isEmpty()) {
            lore.add("");
            lore.addAll(enchantLines);
        }

        // 3. Ability
        if (abilityName != null && !abilityName.isEmpty()) {
            lore.add("");
            lore.add(ChatColor.GOLD + "Ability: " + abilityName + " " + ChatColor.YELLOW + ChatColor.BOLD + "RIGHT CLICK");
            if (abilityDesc != null && !abilityDesc.isEmpty()) {
                for (String line : abilityDesc.split("\n")) {
                    lore.add(ChatColor.GRAY + line);
                }
            }
            if (manaCost > 0) {
                lore.add(ChatColor.DARK_GRAY + "Mana Cost: " + ChatColor.AQUA + manaCost);
            }
            if (cooldown > 0) {
                lore.add(ChatColor.DARK_GRAY + "Cooldown: " + ChatColor.GREEN + cooldown + "s");
            }
        }

        // 4. Rarity
        lore.add("");
        lore.add(rarity + " " + weaponType);

        return build(material, name, id, lore, enchants, true);
    }

    private ItemStack weaponWithFlavor(
            Material material,
            String name,
            String id,
            List<String> stats,
            String flavor,
            String abilityName,
            String abilityDesc,
            int manaCost,
            double cooldown,
            String rarity,
            String weaponType,
            Map<Enchantment, Integer> enchants
    ) {
        List<String> lore = new ArrayList<>();

        for (String stat : stats) {
            if (stat != null && !stat.isEmpty()) {
                lore.add(stat);
            }
        }

        if (flavor != null && !flavor.isEmpty()) {
            lore.add("");
            for (String line : flavor.split("\n")) {
                lore.add(ChatColor.GRAY + line);
            }
        }

        List<String> enchantLines = enchantLoreLines(enchants);
        if (!enchantLines.isEmpty()) {
            lore.add("");
            lore.addAll(enchantLines);
        }

        if (abilityName != null && !abilityName.isEmpty()) {
            lore.add("");
            lore.add(ChatColor.GOLD + "Ability: " + abilityName + " " + ChatColor.YELLOW + ChatColor.BOLD + "RIGHT CLICK");
            if (abilityDesc != null && !abilityDesc.isEmpty()) {
                for (String line : abilityDesc.split("\n")) {
                    lore.add(ChatColor.GRAY + line);
                }
            }
            if (manaCost > 0) {
                lore.add(ChatColor.DARK_GRAY + "Mana Cost: " + ChatColor.AQUA + manaCost);
            }
            if (cooldown > 0) {
                lore.add(ChatColor.DARK_GRAY + "Cooldown: " + ChatColor.GREEN + cooldown + "s");
            }
        }

        lore.add("");
        lore.add(rarity + " " + weaponType);
        return build(material, name, id, lore, enchants, true);
    }

    private ItemStack material(Material material, String name, String id, String description, String rarity) {
        List<String> lore = new ArrayList<>();
        if (description != null && !description.isEmpty()) {
            for (String line : description.split("\n")) {
                lore.add(ChatColor.GRAY + line);
            }
            lore.add("");
        }
        lore.add(rarity);

        String normalizedId = id == null ? "" : id.toUpperCase(Locale.ROOT);
        boolean enchantedMaterial = normalizedId.startsWith("ENCHANTED_")
                || normalizedId.contains("_ESSENCE")
                || normalizedId.contains("_SIGIL")
                || normalizedId.contains("_CORE");

        ItemStack built = build(material, name, id, lore, Map.of(), false, true);
        if (enchantedMaterial) {
            built = applyCosmeticGlint(built);
        }
        return built;
    }

    private ItemStack armor(
            Material material,
            String name,
            String id,
            List<String> stats,
            String abilityName,
            String abilityDesc,
            String fullSetBonus,
            String rarity,
            String pieceType,
            Map<Enchantment, Integer> enchants
    ) {
        List<String> lore = new ArrayList<>();
        
        // 1. Stats
        for (String stat : stats) {
            if (stat != null && !stat.isEmpty()) {
                lore.add(stat);
            }
        }
        
        // 2. Ability
        if (abilityName != null && !abilityName.isEmpty()) {
            lore.add("");
            lore.add(ChatColor.GOLD + "Ability: " + abilityName + " " + ChatColor.YELLOW + ChatColor.BOLD + "FULL SET");
            if (abilityDesc != null && !abilityDesc.isEmpty()) {
                lore.add(ChatColor.GRAY + abilityDesc);
            }
        }
        
        // 3. Full Set Bonus (if different from abilityName)
        if (fullSetBonus != null && !fullSetBonus.isEmpty()) {
            if (abilityName == null || !fullSetBonus.contains(abilityName)) {
                lore.add("");
                lore.add(fullSetBonus);
            }
        }

        // 4. Enchants
        List<String> enchantLines = enchantLoreLines(enchants);
        if (!enchantLines.isEmpty()) {
            lore.add("");
            lore.addAll(enchantLines);
        }

        // 5. Rarity
        lore.add("");
        String rarityColor = rarity.substring(0, 2);
        lore.add(rarity + " " + pieceType);

        ItemStack item = build(material, name, id, lore, enchants, true);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Set armor set and piece metadata for set bonus detection
            String setId = id.split("_")[0].toLowerCase(Locale.ROOT);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "armor_set"), PersistentDataType.STRING, setId);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "armor_piece"), PersistentDataType.STRING, pieceType.toUpperCase(Locale.ROOT));
            item.setItemMeta(meta);
        }
        
        // Apply white color to leather armor
        if (item.getItemMeta() instanceof LeatherArmorMeta leatherMeta) {
            leatherMeta.setColor(Color.WHITE);
            item.setItemMeta(leatherMeta);
        }
        
        // Apply custom head texture for Guardian Helm
        if (id.equals("GUARDIAN_HELM") && item.getItemMeta() instanceof SkullMeta skullMeta) {
            try {
                PlayerProfile profile = Bukkit.createProfile(java.util.UUID.fromString("b27c74c3-fac5-43b5-85bc-2b153b901d66"), "skinb27c74c3");
                profile.setProperty(new ProfileProperty("textures", "ewogICJ0aW1lc3RhbXAiIDogMTc1MDY0NjgyNDIxMSwKICAicHJvZmlsZUlkIiA6ICI1ODc5MjNlNDkxMzM0ZDMzYWE4ZjQ3ZWJkZTljOTc3MiIsCiAgInByb2ZpbGVOYW1lIiA6ICJFbGV2ZW5mb3VyMTAiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjMyYmZiOGQ0ZjgxYWViNzFkMzQxMDJlM2JiY2JjZTU4MTM3YmYzNGU0NjczYWI1ZjJlZWI0OWE1MjhiZTBhMiIKICAgIH0KICB9Cn0=", "R0LRglh9wCuk+3rJFwx+QkwC6Nto3b3fyro3fJYl79V5FiLlEKdTIGD5c6RTMoCK1r6mxAanOPBGju0pCiP2GRXXAaWmVW0FuolT9LT1kx7VcUj6GrVV+PYj6i3zF3Pu7L/iO/D3oYYXr+f7ai62H47Ansl0EYSZNOC+Slu/3pc/2/tPwIAj534KwdWoRWa6KxMlyf8poFN6leOS7XAQXOIlc7GfMpg0UlABAxGKbRwdJqP9lCnv0a6CYu3G7YF3B733vx4FWvlmpTNzGmKcUYAsId1jGnzYflviCkPHrfICmWrutAK/5HrrM5bKMPVMY/diIcHnPbhPWnpdnECur9iWGFyM5sPPOALSno5TRF0BntrjVCcWsgEdJgYtZoqLsj64mJcTJn2S7mSlVFcGgeNTyqPiFvS6pahS+VXL4mNtJ/wGK4Vlms/0SJNcJmkHQv4HX1SFiHxbgq2xz73z+yCysiwHLS6KMRnfFuu/9J9wDxzmlncnJWP77VassjlA6+a2CxrWjUHoEjuhl+lbSpOmD1xN9Z3orYPWAoH5+F2Lj0+bRVNuf+CBjWjkjUOeQ/3aM0kW8f8pxU2+4PS36AGQcmpoFjtQaE3psRXPWC1zxfayrqlV7qpnmnGJbu+dmNolRHGnLHWvVqYr3oZj30PgTiyYizWuCz3tmZgH87A="));
                skullMeta.setPlayerProfile(profile);
                item.setItemMeta(skullMeta);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to apply Guardian Helm texture: " + e.getMessage());
            }
        }

        // Apply custom head texture for Guardian Fragment
        if (id.equals("GUARDIAN_FRAGMENT") && item.getItemMeta() instanceof SkullMeta skullMeta) {
            try {
                PlayerProfile profile = Bukkit.createProfile(java.util.UUID.fromString("58c6fb76-db3c-7718-5810-2e14e6dcc7f2"), "GuardianGoon");
                profile.setProperty(new ProfileProperty("textures", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNThjNmZiNzZkYjNjNzcxODU4MTAyZTE0ZTZkY2M3ZjI3OTFmNTdlYjkzYzBkNGNiZDljOGExZDlkY2JlMTAzMCJ9fX0="));
                skullMeta.setPlayerProfile(profile);
                item.setItemMeta(skullMeta);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to apply Guardian Fragment texture: " + e.getMessage());
            }
        }
        
        return item;
    }

    private ItemStack dragonSlayerArmor(
            Material material,
            String name,
            String id,
            List<String> stats,
            String abilityName,
            List<String> abilityDescription,
            List<String> flavorLines,
            String pieceType
    ) {
        List<String> lore = new ArrayList<>();
        lore.addAll(stats);
        lore.add("");
        lore.add(ChatColor.GOLD + "Ability: " + abilityName);
        lore.addAll(abilityDescription);
        lore.add("");
        lore.addAll(flavorLines);
        lore.add("");
        lore.add(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC " + pieceType);

        ItemStack item = build(material, name, id, lore, Map.of(), true);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "armor_set"), PersistentDataType.STRING, "dragon_slayer");
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "armor_piece"), PersistentDataType.STRING, pieceType);
        item.setItemMeta(meta);

        if (item.getItemMeta() instanceof LeatherArmorMeta leatherMeta) {
            leatherMeta.setColor(Color.fromRGB(0x2B163B));
            item.setItemMeta(leatherMeta);
        }

        return ArmorDurabilityUtil.ensureArmorUnbreakable(item);
    }

    private ItemStack build(
            Material material,
            String name,
            String id,
            List<String> lore,
            Map<Enchantment, Integer> enchants,
            boolean hideEnchants
    ) {
        return build(material, name, id, lore, enchants, hideEnchants, false);
    }

    private ItemStack build(
            Material material,
            String name,
            String id,
            List<String> lore,
            Map<Enchantment, Integer> enchants,
            boolean hideEnchants,
            boolean markAsMaterial
    ) {
        // Ensure ID consistency for stacking (PDC keys are case-sensitive)
        final String effectiveId = id.toUpperCase(Locale.ROOT);
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        ItemRarity rarity = rarityFromLore(lore, ItemRarity.RARE);
        boolean weapon = isWeaponId(effectiveId);
        boolean reforgeable = !markAsMaterial && (weapon || CustomArmorType.parse(effectiveId) != null);

        meta.setDisplayName(styledName(resolveDisplayName(effectiveId, name), rarity, weapon));
        meta.setLore(styledLore(lore, reforgeable));
        
        // --- NBT / PDC DATA ---
        meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, effectiveId);
        if (markAsMaterial) {
            meta.getPersistentDataContainer().set(customMaterialKey, PersistentDataType.STRING, effectiveId);
        }

        // Always make custom items unbreakable and hide the tag for consistency
        meta.setUnbreakable(true);

        // --- FLAGS (Standardized for stacking) ---
        meta.addItemFlags(
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_UNBREAKABLE,
                ItemFlag.HIDE_DESTROYS,
                ItemFlag.HIDE_PLACED_ON,
                ItemFlag.HIDE_DYE,
                ItemFlag.HIDE_ARMOR_TRIM
        );

        // --- TEXTURES (Heads) ---
        if (material == Material.PLAYER_HEAD && meta instanceof SkullMeta skullMeta) {
            if (effectiveId.equals("GUARDIAN_HELM")) {
                applyTexture(skullMeta, "b27c74c3-fac5-43b5-85bc-2b153b901d66", "ewogICJ0aW1lc3RhbXAiIDogMTc1MDY0NjgyNDIxMSwKICAicHJvZmlsZUlkIiA6ICI1ODc5MjNlNDkxMzM0ZDMzYWE4ZjQ3ZWJkZTljOTc3MiIsCiAgInByb2ZpbGVOYW1lIiA6ICJFbGV2ZW5mb3VyMTAiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjMyYmZiOGQ0ZjgxYWViNzFkMzQxMDJlM2JiY2JjZTU4MTM3YmYzNGU0NjczYWI1ZjJlZWI0OWE1MjhiZTBhMiIKICAgIH0KICB9Cn0=", "R0LRglh9wCuk+3rJFwx+QkwC6Nto3b3fyro3fJYl79V5FiLlEKdTIGD5c6RTMoCK1r6mxAanOPBGju0pCiP2GRXXAaWmVW0FuolT9LT1kx7VcUj6GrVV+PYj6i3zF3Pu7L/iO/D3oYYXr+f7ai62H47Ansl0EYSZNOC+Slu/3pc/2/tPwIAj534KwdWoRWa6KxMlyf8poFN6leOS7XAQXOIlc7GfMpg0UlABAxGKbRwdJqP9lCnv0a6CYu3G7YF3B733vx4FWvlmpTNzGmKcUYAsId1jGnzYflviCkPHrfICmWrutAK/5HrrM5bKMPVMY/diIcHnPbhPWnpdnECur9iWGFyM5sPPOALSno5TRF0BntrjVCcWsgEdJgYtZoqLsj64mJcTJn2S7mSlVFcGgeNTyqPiFvS6pahS+VXL4mNtJ/wGK4Vlms/0SJNcJmkHQv4HX1SFiHxbgq2xz73z+yCysiwHLS6KMRnfFuu/9J9wDxzmlncnJWP77VassjlA6+a2CxrWjUHoEjuhl+lbSpOmD1xN9Z3orYPWAoH5+F2Lj0+bRVNuf+CBjWjkjUOeQ/3aM0kW8f8pxU2+4PS36AGQcmpoFjtQaE3psRXPWC1zxfayrqlV7qpnmnGJbu+dmNolRHGnLHWvVqYr3oZj30PgTiyYizWuCz3tmZgH87A=");
            } else if (effectiveId.equals("GUARDIAN_FRAGMENT")) {
                applyTexture(skullMeta, "58c6fb76-db3c-7718-5810-2e14e6dcc7f2", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNThjNmZiNzZkYjNjNzcxODU4MTAyZTE0ZTZkY2M3ZjI3OTFmNTdlYjkzYzBkNGNiZDljOGExZDlkY2JlMTAzMCJ9fX0=", null);
            } else if (effectiveId.equals("SUMMONING_EYE")) {
                applyTexture(skullMeta, SUMMONING_EYE_TEXTURE_UUID, SUMMONING_EYE_TEXTURE, null);
            } else if (effectiveId.equals("DRAGON_SLAYER_HELMET")) {
                applyTexture(skullMeta, DRAGON_SLAYER_HELMET_TEXTURE_UUID, DRAGON_SLAYER_HELMET_TEXTURE, null);
            } else if (effectiveId.equals("TITANIUM")) {
                applyTexture(skullMeta, "324d23f0-bb3e-750a-e4fe-10a2a8490e8a", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzI0ZDIzZjBiYjNlNzUwYWU0ZmUxMGEyYTg0OTBlOGEwMjg4MDZiOGE5NTRjNDU3YmUzNzlkMDJiN2Q0NjUwMiJ9fX0=", null);
            } else if (effectiveId.equals("ENCHANTED_TITANIUM")) {
                applyTexture(skullMeta, "704baabf-7ef8-5482-5aae-1992e4a75ff7", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzA0YmFhYmY3ZWY4NTQ4MjVhYWUxOTkyZTRhNzVmZjcyODZlZDE2NTRkOGYxYTA4OTUyZTdiODY2OWNmNjkyZCJ9fX0=", null);
            }
        }

        item.setItemMeta(meta);

        // --- ENCHANTS ---
        if (enchants != null) {
            for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                item.addUnsafeEnchantment(entry.getKey(), entry.getValue());
            }
        }

        return item;
    }

    private void applyTexture(SkullMeta meta, String uuid, String texture, String signature) {
        try {
            PlayerProfile profile = Bukkit.createProfile(java.util.UUID.fromString(uuid), "CustomHead");
            profile.setProperty(new ProfileProperty("textures", texture, signature));
            meta.setPlayerProfile(profile);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to apply texture: " + e.getMessage());
        }
    }

    private String resolveDisplayName(String id, String fallback) {
        return fallback;
    }

    public int getDungeonStars(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(dungeonStarsKey, PersistentDataType.INTEGER, 0);
    }

    public ItemStack setDungeonStars(ItemStack item, int stars) {
        if (item == null || !item.hasItemMeta()) return item;
        ItemStack result = item.clone();
        ItemMeta meta = result.getItemMeta();
        meta.getPersistentDataContainer().set(dungeonStarsKey, PersistentDataType.INTEGER, Math.clamp(stars, 0, 5));
        result.setItemMeta(meta);
        return updateItemLore(result);
    }

    public ItemStack updateItemLore(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return item;
        
        // This is a generic lore updater that should be called when stars or other NBT changes
        // For now, let's just handle the name update with stars
        ItemMeta meta = item.getItemMeta();
        int stars = getDungeonStars(item);
        
        String baseName = meta.getPersistentDataContainer().get(reforgeBaseNameKey, PersistentDataType.STRING);
        if (baseName == null) baseName = strip(meta.getDisplayName());
        
        ItemRarity rarity = rarityOf(item);
        ReforgeType reforge = reforgeOf(item);
        boolean isDungeonized = isDungeonized(item);
        
        String displayName = (reforge != null ? reforge.displayName() + " " : "") + baseName;
        if (stars > 0) {
            displayName += " " + ChatColor.GOLD + "✪".repeat(stars);
        }
        
        meta.setDisplayName((rarity != null ? rarity.color() : ChatColor.WHITE) + displayName);
        
        // Update Lore - we need to find the rarity line and modify it
        List<String> lore = meta.getLore();
        if (lore != null && rarity != null) {
            for (int i = 0; i < lore.size(); i++) {
                String line = lore.get(i);
                if (line.contains(rarity.name()) && line.contains("ACCESSORY") == false) {
                    String prefix = isDungeonized ? "DUNGEON " : "";
                    lore.set(i, rarity.color() + "" + ChatColor.BOLD + prefix + rarity.name() + (line.contains("ITEM") ? " ITEM" : ""));
                }
            }
            meta.setLore(lore);
        }

        item.setItemMeta(meta);
        return item;
    }

    private String styledName(String original, ItemRarity rarity, boolean weapon) {
        String baseName = strip(original);
        ChatColor nameColor = rarity == null ? ChatColor.WHITE : rarity.color();
        return switch (itemStyle) {
            case JAPANESE -> nameColor + baseName;
            case SKYBLOCK -> nameColor + baseName;
            case MINIMAL -> nameColor + baseName;
        };
    }

    private boolean isWeaponId(String id) {
        return CustomWeaponType.parse(id) != null
                || CustomToolType.parse(id) != null
                || GrapplingHookType.parse(id) != null;
    }

    private ItemRarity rarityFromLore(List<String> lore, ItemRarity fallback) {
        if (lore == null) {
            return fallback;
        }

        for (int i = lore.size() - 1; i >= 0; i--) {
            String line = lore.get(i);
            if (line == null || line.isBlank()) {
                continue;
            }
            if (isReforgeLoreLine(line) || isEnchantLoreLine(line)) {
                continue;
            }
            String plain = strip(line);
            if (plain == null) {
                continue;
            }
            String upper = plain.toUpperCase(Locale.ROOT);
            if (upper.contains("MYTHIC")) {
                return ItemRarity.MYTHIC;
            }
            if (upper.contains("LEGENDARY")) {
                return ItemRarity.LEGENDARY;
            }
            if (upper.contains("EPIC")) {
                return ItemRarity.EPIC;
            }
            if (upper.contains("RARE")) {
                return ItemRarity.RARE;
            }
            if (upper.contains("UNCOMMON")) {
                return ItemRarity.UNCOMMON;
            }
            if (upper.contains("COMMON")) {
                return ItemRarity.COMMON;
            }
        }
        return fallback;
    }

    private String normalizeBaseName(String name) {
        if (name == null) {
            return null;
        }
        String stripped = strip(name);
        if (stripped.contains("_")) {
            // Likely a raw Material name, format it
            String[] parts = stripped.split("_");
            StringBuilder formatted = new StringBuilder();
            for (String part : parts) {
                if (part.isEmpty()) continue;
                formatted.append(Character.toUpperCase(part.charAt(0)))
                         .append(part.substring(1).toLowerCase(Locale.ROOT))
                         .append(" ");
            }
            return formatted.toString().trim();
        }

        String normalized = stripped.stripLeading();
        while (!normalized.isEmpty() && isStarSymbol(normalized.charAt(0))) {
            normalized = normalized.substring(1).stripLeading();
        }
        return normalized;
    }

    private boolean isStarSymbol(char c) {
        return c == '✪' || c == '★' || c == '☆';
    }

    private List<String> styledLore(List<String> lore, boolean weapon) {
        if (itemStyle == ItemStyle.JAPANESE) {
            return lore;
        }

        if (itemStyle == ItemStyle.MINIMAL) {
            List<String> compact = new ArrayList<>();
            for (String line : lore) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                String plain = strip(line);
                if (plain.contains(":") || plain.contains("WEAPON") || plain.contains("MATERIAL")) {
                    compact.add(ChatColor.GRAY + plain);
                }
            }
            if (compact.isEmpty()) {
                compact.add(ChatColor.GRAY + "Custom Item");
            }
            return compact;
        }

        // SKYBLOCK STYLE
        List<String> skyblock = new ArrayList<>();
        String rarityLine = null;
        for (String line : lore) {
            if (line == null) continue;
            String plain = strip(line);
            if (plain.isEmpty()) {
                if (!skyblock.isEmpty() && !skyblock.get(skyblock.size() - 1).isEmpty()) {
                    skyblock.add("");
                }
                continue;
            }
            // Check if this is the rarity line (usually all caps or containing rarity keywords)
            if (isRarityLine(line)) {
                rarityLine = normalizeRarityLine(line);
                continue;
            }
            skyblock.add(line);
        }

        // Ensure there's a space before the rarity line if we have content
        if (!skyblock.isEmpty() && !skyblock.get(skyblock.size() - 1).isEmpty()) {
            skyblock.add("");
        }

        if (rarityLine != null) {
            skyblock.add(rarityLine);
        }

        return skyblock;
    }

    private boolean isRarityLine(String line) {
        String plain = strip(line).toUpperCase(Locale.ROOT);
        for (ItemRarity rarity : ItemRarity.values()) {
            if (plain.contains(rarity.name())) {
                // Heuristic: rarity lines often also contain keywords like "WEAPON", "ARMOR", "MATERIAL", "ACCESSORY"
                if (plain.contains("WEAPON") || plain.contains("ARMOR") || plain.contains("MATERIAL") ||
                        plain.contains("ACCESSORY") || plain.contains("CHARM") || plain.contains("RING") ||
                        plain.contains("TALISMAN") || plain.contains("ARTIFACT") || plain.contains("TRINKET") ||
                        plain.contains("SWORD") || plain.contains("BOW") || plain.contains("AXE") ||
                        plain.contains("PICKAXE") || plain.contains("DRILL") || plain.contains("SHOVEL") ||
                        plain.contains("HOE") || plain.contains("SHEARS") || plain.contains("FISHING") ||
                        plain.contains("BOOTS") || plain.contains("LEGGINGS") || plain.contains("CHESTPLATE") ||
                        plain.contains("HELMET")) {
                    return true;
                }
                // If it's just the rarity name in bold, it's also likely the rarity line
                if (plain.trim().equals(rarity.name())) {
                    return true;
                }
            }
        }
        return false;
    }

    private String normalizeRarityLine(String line) {
        if (line == null) {
            return null;
        }
        String plain = strip(line);
        if (plain == null) {
            return line;
        }
        plain = plain.trim();
        if (plain.isEmpty()) {
            return line;
        }

        // Ensure the rarity line color matches the rarity keyword it contains.
        ItemRarity rarity = rarityFromLore(List.of(line), null);
        if (rarity == null) {
            return line;
        }

        return rarity.color() + "" + ChatColor.BOLD + plain.toUpperCase(Locale.ROOT);
    }

    private String fullSetBonusLore(ArmorSetType setType) {
        return switch (setType) {
            case SHOGUN -> ChatColor.RED + "Full Set Bonus: Warlord's Resolve";
            case SHINOBI -> ChatColor.AQUA + "Full Set Bonus: Shadowstep";
            case ONMYOJI -> ChatColor.GOLD + "Full Set Bonus: Spirit Ward";
            case TITAN -> ChatColor.GOLD + "Full Set Bonus: Colossal Barrier";
            case LEVIATHAN -> ChatColor.DARK_AQUA + "Full Set Bonus: Abyssal Bulwark";
            case GUARDIAN -> ChatColor.WHITE + "Full Set Bonus: Divine Protection";
            case MINER -> ChatColor.YELLOW + "Full Set Bonus: Double Drop Chance";
            case IRONCREST_GUARD -> ChatColor.BLUE + "Full Set Bonus: Mine Defense";
            case DEEPCORE -> ChatColor.DARK_PURPLE + "Full Set Bonus: Instant Break";
            case RONIN -> ChatColor.BLUE + "Full Set Bonus: Way of the Blade";
            case KAPPA_GUARDIAN -> ChatColor.BLUE + "Full Set Bonus: Hydraulic Pressure";
            case TENGU_MASTER -> ChatColor.GOLD + "Full Set Bonus: High Altitude";
            case SKELETON_SOLDIER -> ChatColor.WHITE + "Full Set Bonus: Undead Soul";
            case DRAGON_SLAYER -> ChatColor.DARK_PURPLE + "Full Set Bonus: Dragon's Dominion";
            case GHOUL_OVERSEER -> ChatColor.DARK_GREEN + "Full Set Bonus: Undead Command";
            case GILDED_HARVESTER -> ChatColor.GOLD + "Full Set Bonus: Bountiful Harvest";
            case DREADLORD -> ChatColor.DARK_GRAY + "Full Set Bonus: Dread Aura";
            case NECROMANCER -> ChatColor.DARK_GREEN + "Full Set Bonus: Undead Commander";
            case SOULBOUND_MAGE -> ChatColor.DARK_PURPLE + "Full Set Bonus: Pact of the Forbidden";
            case ROOKIE_SAMURAI -> ChatColor.GREEN + "Full Set Bonus: Bushido Spirit";
        };
    }

    private String armorDescriptionLore(CustomArmorType type) {
        String classColor = switch (type.weightClass()) {
            case HEAVY -> ChatColor.RED.toString();
            case BALANCED -> ChatColor.YELLOW.toString();
            case LIGHT -> ChatColor.AQUA.toString();
        };
        return ChatColor.DARK_GRAY + "Class: " + classColor + type.weightClass().displayName()
                + ChatColor.GRAY + " | " + type.gameplayDescription();
    }

    private boolean isReforgeLoreLine(String line) {
        if (line == null) {
            return false;
        }
        String stripped = strip(line);
        if (stripped == null) {
            return false;
        }
        String normalized = stripped.toLowerCase(Locale.ROOT);
        return normalized.startsWith("reforge:") || normalized.startsWith("bonus:");
    }

    private boolean isEnchantLoreLine(String line) {
        if (line == null) {
            return false;
        }
        return line.startsWith(ENCHANT_MARKER);
    }

    private int enchantInsertionIndex(List<String> lore) {
        for (int i = 0; i < lore.size(); i++) {
            String plain = strip(lore.get(i));
            if (plain == null || plain.isBlank()) {
                return i;
            }
        }
        return lore.size();
    }

    private List<String> enchantLoreLines(Map<Enchantment, Integer> enchants) {
        if (enchants.isEmpty()) {
            return List.of();
        }
        List<Map.Entry<Enchantment, Integer>> entries = new ArrayList<>(enchants.entrySet());
        entries.sort(Comparator.comparing(entry -> enchantSortKey(entry.getKey())));

        List<String> lines = new ArrayList<>();
        if (entries.size() <= 5) {
            // Vertical list for few enchants
            for (Map.Entry<Enchantment, Integer> entry : entries) {
                String name = enchantDisplayName(entry.getKey());
                int level = Math.max(1, entry.getValue());
                lines.add(ENCHANT_MARKER + ChatColor.BLUE + name + " " + roman(level));
            }
        } else {
            // Horizontal list (grouped) for many enchants
            StringBuilder currentLine = new StringBuilder(ENCHANT_MARKER + ChatColor.BLUE.toString());
            int count = 0;
            for (int i = 0; i < entries.size(); i++) {
                Map.Entry<Enchantment, Integer> entry = entries.get(i);
                String name = enchantDisplayName(entry.getKey());
                int level = Math.max(1, entry.getValue());
                
                currentLine.append(name).append(" ").append(roman(level));
                count++;

                if (i < entries.size() - 1) {
                    if (count >= 3) {
                        lines.add(currentLine.toString());
                        currentLine = new StringBuilder(ENCHANT_MARKER + ChatColor.BLUE.toString());
                        count = 0;
                    } else {
                        currentLine.append(ChatColor.GRAY).append(", ").append(ChatColor.BLUE);
                    }
                } else {
                    lines.add(currentLine.toString());
                }
            }
        }
        return lines;
    }

    private String enchantDisplayName(Enchantment enchantment) {
        String key = enchantSortKey(enchantment);
        String[] parts = key.split("_");
        List<String> words = new ArrayList<>(parts.length);
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            String lower = part.toLowerCase(Locale.ROOT);
            words.add(Character.toUpperCase(lower.charAt(0)) + lower.substring(1));
        }
        return String.join(" ", words);
    }

    private String enchantSortKey(Enchantment enchantment) {
        if (enchantment.getKey() != null) {
            return enchantment.getKey().getKey();
        }
        return "unknown_" + Integer.toHexString(System.identityHashCode(enchantment));
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
            default -> Integer.toString(value);
        };
    }

    private void appendBonusPart(List<String> parts, double value, String label, ChatColor color) {
        int rounded = (int) Math.round(value);
        if (rounded == 0) {
            return;
        }
        String sign = rounded > 0 ? "+" : "";
        parts.add(color + sign + rounded + label);
    }

    private ItemStack applyCosmeticGlint(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        applyGlintOverride(meta, true);
        item.setItemMeta(meta);
        return item;
    }

    private boolean applyGlintOverride(ItemMeta meta, boolean glint) {
        if (meta == null) {
            return false;
        }

        try {
            Method method = ItemMeta.class.getMethod("setEnchantmentGlintOverride", Boolean.class);
            method.invoke(meta, glint ? Boolean.TRUE : null);
            return true;
        } catch (NoSuchMethodException ignored) {
            // fall through
        } catch (Throwable ignored) {
            return false;
        }

        try {
            Method method = ItemMeta.class.getMethod("setEnchantmentGlintOverride", boolean.class);
            method.invoke(meta, glint);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private List<String> abilityLore(String name, String action, List<String> description, int mana, int cooldown) {
        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.GOLD + "Ability: " + name + " " + ChatColor.YELLOW + ChatColor.BOLD + action);
        for (String descLine : description) {
            lines.add(ChatColor.GRAY + descLine);
        }
        if (mana > 0) {
            lines.add(ChatColor.DARK_GRAY + "Mana Cost: " + ChatColor.AQUA + mana);
        }
        if (cooldown > 0) {
            lines.add(ChatColor.DARK_GRAY + "Cooldown: " + ChatColor.GREEN + cooldown + "s");
        }
        return lines;
    }

    public ItemRarity rarityOf(ItemStack weapon) {
        if (weapon == null || !weapon.hasItemMeta()) {
            return ItemRarity.RARE;
        }
        ItemMeta meta = weapon.getItemMeta();
        if (!meta.hasLore() || meta.getLore() == null) {
            return ItemRarity.RARE;
        }
        return rarityFromLore(meta.getLore(), ItemRarity.RARE);
    }

    private void resetDropDefaults() {
        mobWeaponBaseChance = 0.012D;
        yokaiWeaponChances.clear();
        yokaiWeaponChances.put(MonsterType.ONI_BRUTE, 0.020D);
        yokaiWeaponChances.put(MonsterType.TENGU_SKIRMISHER, 0.022D);
        yokaiWeaponChances.put(MonsterType.KAPPA_RAIDER, 0.018D);
        yokaiWeaponChances.put(MonsterType.ONRYO_WRAITH, 0.016D);
        yokaiWeaponChances.put(MonsterType.JOROGUMO_WEAVER, 0.016D);
        yokaiWeaponChances.put(MonsterType.KITSUNE_TRICKSTER, 0.018D);
        yokaiWeaponChances.put(MonsterType.GASHADOKURO_SENTINEL, 0.035D);

        mobArmorBaseChance = 0.010D;
        yokaiArmorChances.clear();
        yokaiArmorChances.put(MonsterType.ONI_BRUTE, 0.014D);
        yokaiArmorChances.put(MonsterType.TENGU_SKIRMISHER, 0.011D);
        yokaiArmorChances.put(MonsterType.KAPPA_RAIDER, 0.010D);
        yokaiArmorChances.put(MonsterType.ONRYO_WRAITH, 0.010D);
        yokaiArmorChances.put(MonsterType.JOROGUMO_WEAVER, 0.009D);
        yokaiArmorChances.put(MonsterType.KITSUNE_TRICKSTER, 0.011D);
        yokaiArmorChances.put(MonsterType.GASHADOKURO_SENTINEL, 0.022D);

        mobReforgeStoneChance = 0.018D;

        stormSigilChance = 0.38D;
        thunderEssenceChance = 0.22D;
        raijinCoreChance = 0.10D;
        bossArmorChance = 0.35D;
        bossReforgeStoneChance = 0.50D;
    }

    private double clampChance(double value) {
        if (value < 0.0D) {
            return 0.0D;
        }
        return Math.min(1.0D, value);
    }

    private String strip(String text) {
        String stripped = ChatColor.stripColor(text);
        return stripped == null ? text : stripped;
    }

    public record ReforgeStats(
            double damageBonus,
            double strengthBonus,
            double critChanceBonus,
            double critDamageBonus,
            double attackSpeedBonus,
            double intelligenceBonus
    ) {
    }

    public void updateDrillLore(ItemMeta meta) {
        if (meta == null) return;
        var pdc = meta.getPersistentDataContainer();

        String drillId = pdc.get(itemIdKey, PersistentDataType.STRING);
        if (drillId == null || !drillId.endsWith("_DRILL")) {
            return;
        }
        ItemRarity drillRarity = switch (drillId) {
            case "TITANIUM_DRILL" -> ItemRarity.LEGENDARY;
            case "GEMSTONE_DRILL" -> ItemRarity.MYTHIC;
            default -> ItemRarity.RARE;
        };
        String engine = pdc.getOrDefault(drillEngineKey, PersistentDataType.STRING, "BASIC_ENGINE");
        String tank = pdc.getOrDefault(drillTankKey, PersistentDataType.STRING, "SMALL_TANK");
        int fuel = pdc.getOrDefault(drillFuelKey, PersistentDataType.INTEGER, 0);

        DrillStatProfile.Profile profile = DrillStatProfile.resolve(drillId, engine, tank);
        int maxFuel = profile.maxFuel();

        // Ensure max fuel is updated in PDC if tank changed
        pdc.set(drillFuelMaxKey, PersistentDataType.INTEGER, maxFuel);
        if (fuel > maxFuel) {
            fuel = maxFuel;
            pdc.set(drillFuelKey, PersistentDataType.INTEGER, fuel);
        } else if (fuel < 0) {
            fuel = 0;
            pdc.set(drillFuelKey, PersistentDataType.INTEGER, fuel);
        }

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Mining Speed: " + ChatColor.GREEN + "+" + profile.miningSpeed());
        lore.add(ChatColor.GRAY + "Breaking Power: " + ChatColor.AQUA + profile.breakingPower());
        lore.add(ChatColor.GRAY + "Fuel Burn: " + ChatColor.YELLOW + profile.fuelCostPerBlock() + ChatColor.GRAY + "/block");
        lore.add(ChatColor.GRAY + "Drill Burst: " + ChatColor.AQUA + "Haste " + roman(profile.abilityAmplifier() + 1)
                + ChatColor.GRAY + " for " + ChatColor.AQUA + (profile.abilityDurationTicks() / 20) + "s");
        lore.add(ChatColor.GRAY + "Burst Cooldown: " + ChatColor.AQUA + (profile.abilityCooldownMillis() / 1000L) + "s");
        lore.add(ChatColor.GRAY + "Crystal Nodes: " + (profile.crystalNodeHitReduction() > 0
                ? ChatColor.GREEN + "-" + profile.crystalNodeHitReduction() + ChatColor.GRAY + " hit(s)"
                : ChatColor.GRAY + "Standard"));
        lore.add("");
        lore.add(ChatColor.GRAY + "Fuel: " + ChatColor.YELLOW + formatInt(fuel) + ChatColor.GRAY + "/" + ChatColor.YELLOW + formatInt(maxFuel));
        lore.add("");
        lore.add(ChatColor.GOLD + "" + ChatColor.BOLD + "DRILL PARTS:");
        lore.add(ChatColor.DARK_GRAY + "Engine: " + ChatColor.GRAY + formatPartName(engine));
        lore.add(ChatColor.DARK_GRAY + "Fuel Tank: " + ChatColor.GRAY + formatPartName(tank));
        lore.add("");
        lore.add(drillRarity.color() + "" + ChatColor.BOLD + drillRarity.name() + " DRILL");

        meta.setUnbreakable(true);
        if (meta instanceof org.bukkit.inventory.meta.Damageable damageable) {
            if (damageable.getDamage() > 0) {
                damageable.setDamage(0);
            }
        }
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        meta.setLore(styledLore(lore, false));
    }

    private String drillEngineDescription(String engineId) {
        DrillStatProfile.EngineTuning tuning = DrillStatProfile.engineTuning(engineId);
        List<String> lines = new ArrayList<>();
        lines.add("Mining Speed +" + tuning.miningSpeedBonus());
        lines.add("Fuel Burn -" + tuning.fuelReductionPerBlock() + " per block");
        lines.add("Drill Burst cooldown -" + (tuning.abilityCooldownReductionMillis() / 1000L) + "s");
        lines.add("Drill Burst duration +" + (tuning.abilityDurationBonusTicks() / 20) + "s");
        if (tuning.abilityAmplifierBonus() > 0) {
            lines.add("Drill Burst power +" + tuning.abilityAmplifierBonus() + " Haste tier");
        }
        return String.join("\n", lines);
    }

    private String drillTankDescription(String tankId) {
        DrillStatProfile.TankTuning tuning = DrillStatProfile.tankTuning(tankId);
        return "Fuel Capacity " + formatInt(tuning.maxFuel()) + "\nLonger runs before refueling.";
    }

    private String formatPartName(String id) {
        if (id == null) return "None";
        return switch (id) {
            case "BASIC_ENGINE" -> "Basic Engine";
            case "MITHRIL_ENGINE" -> "Mithril Engine";
            case "TITANIUM_ENGINE" -> "Titanium Engine";
            case "GEMSTONE_ENGINE" -> "Gemstone Engine";
            case "DIVAN_ENGINE" -> "Divan Engine";
            case "SMALL_TANK" -> "Small Tank";
            case "MEDIUM_FUEL_TANK" -> "Medium Fuel Tank";
            case "LARGE_FUEL_TANK" -> "Large Fuel Tank";
            default -> id.replace("_", " ");
        };
    }

    public NamespacedKey getDrillEngineKey() { return drillEngineKey; }
    public NamespacedKey getDrillTankKey() { return drillTankKey; }
    public NamespacedKey getRecipeDrillUpgradeKey() { return recipeDrillUpgradeKey; }

    public boolean isWeapon(ItemStack item) {
        if (item == null) return false;
        String id = itemId(item);
        return CustomWeaponType.parse(id) != null;
    }

    public boolean isArmor(ItemStack item) {
        if (item == null) return false;
        String id = itemId(item);
        return CustomArmorType.parse(id) != null;
    }

    public void setGrapplingHookManager(io.papermc.Grivience.item.GrapplingHookManager manager) {
        this.grapplingHookManager = manager;
    }

    public boolean isDungeonized(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        if (isNativeDungeonItem(item)) return true;
        return item.getItemMeta().getPersistentDataContainer().has(dungeonizedKey, PersistentDataType.BYTE);
    }

    public boolean isNativeDungeonItem(ItemStack item) {
        String id = itemId(item);
        if (id == null) return false;
        
        // List of items that are dungeon-native and don't need dungeonizing
        return id.startsWith("RONIN_") || 
               id.startsWith("KAPPA_GUARDIAN_") ||
               id.startsWith("TENGU_MASTER_") ||
               id.startsWith("SKELETON_SOLDIER_") ||
               id.startsWith("DREADLORD_") ||
               id.startsWith("NECROMANCER_") ||
               id.startsWith("SOULBOUND_") ||
               id.equals("WARDENS_CLEAVER");
    }

    public ItemStack dungeonize(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return item;
        ItemStack result = item.clone();
        ItemMeta meta = result.getItemMeta();
        meta.getPersistentDataContainer().set(dungeonizedKey, PersistentDataType.BYTE, (byte) 1);
        result.setItemMeta(meta);
        return updateItemLore(result);
    }

    private static String formatInt(int value) {
        return String.format(java.util.Locale.US, "%,d", value);
    }

}


