package io.papermc.Grivience.item;

import io.papermc.Grivience.dungeon.YokaiType;
import io.papermc.Grivience.item.EnchantedFarmItemType;
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

public final class CustomItemService {
    private final JavaPlugin plugin;
    private final NamespacedKey itemIdKey;
    private final Map<String, Integer> modelDataOverrides = new HashMap<>();
    private final NamespacedKey reforgeIdKey;
    private final NamespacedKey reforgeBaseNameKey;
    private final NamespacedKey recipeFlyingRaijinKey;
    private final NamespacedKey recipeHayabusaKey;
    private final NamespacedKey recipeRaijinShortbowKey;
    private final NamespacedKey recipeGuardianHelmKey;
    private final NamespacedKey recipeGuardianChestKey;
    private final NamespacedKey recipeGuardianLegsKey;
    private final NamespacedKey recipeGuardianBootsKey;
    private final NamespacedKey farmRecipeKey;
    private final NamespacedKey recipeDrillUpgradeKey;
    private final NamespacedKey customMaterialKey;
    // Drill PDC keys
    private final NamespacedKey drillFuelKey;
    private final NamespacedKey drillFuelMaxKey;
    private final NamespacedKey drillEngineKey;
    private final NamespacedKey drillTankKey;
    private io.papermc.Grivience.item.GrapplingHookManager grapplingHookManager;

    private ItemStyle itemStyle = ItemStyle.SKYBLOCK;
    private double mobWeaponBaseChance;
    private final EnumMap<YokaiType, Double> yokaiWeaponChances = new EnumMap<>(YokaiType.class);
    private double mobArmorBaseChance;
    private final EnumMap<YokaiType, Double> yokaiArmorChances = new EnumMap<>(YokaiType.class);
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
        loadModelOverrides();
        this.reforgeIdKey = new NamespacedKey(plugin, "custom-reforge-id");
        this.reforgeBaseNameKey = new NamespacedKey(plugin, "custom-reforge-base-name");
        this.recipeFlyingRaijinKey = new NamespacedKey(plugin, "flying-raijin-recipe");
        this.recipeHayabusaKey = new NamespacedKey(plugin, "hayabusa_katana_recipe");
        this.recipeRaijinShortbowKey = new NamespacedKey(plugin, "raijin_shortbow_recipe");
        this.recipeGuardianHelmKey = new NamespacedKey(plugin, "guardian_helm_recipe");
        this.recipeGuardianChestKey = new NamespacedKey(plugin, "guardian_chest_recipe");
        this.recipeGuardianLegsKey = new NamespacedKey(plugin, "guardian_legs_recipe");
        this.recipeGuardianBootsKey = new NamespacedKey(plugin, "guardian_boots_recipe");
        this.farmRecipeKey = new NamespacedKey(plugin, "farm-compress");
        this.recipeDrillUpgradeKey = new NamespacedKey(plugin, "drill_upgrade");
        this.drillFuelKey = new NamespacedKey(plugin, "drill-fuel");
        this.drillFuelMaxKey = new NamespacedKey(plugin, "drill-fuel-max");
        this.drillEngineKey = new NamespacedKey(plugin, "drill-engine");
        this.drillTankKey = new NamespacedKey(plugin, "drill-tank");
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
                YokaiType type = YokaiType.parse(key);
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
                YokaiType type = YokaiType.parse(key);
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
        Bukkit.removeRecipe(recipeGuardianHelmKey);
        Bukkit.removeRecipe(recipeGuardianChestKey);
        Bukkit.removeRecipe(recipeGuardianLegsKey);
        Bukkit.removeRecipe(recipeGuardianBootsKey);
        Bukkit.removeRecipe(recipeDrillUpgradeKey);
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

        ItemStack fragment = createCraftingItem(RaijinCraftingItemType.GUARDIAN_FRAGMENT);
        fragment.setAmount(10);
        RecipeChoice.ExactChoice fragChoice = new RecipeChoice.ExactChoice(fragment);

        ShapedRecipe gHelm = new ShapedRecipe(recipeGuardianHelmKey, createArmor(CustomArmorType.GUARDIAN_HELM));
        gHelm.shape("FFF", "F F", "   ");
        gHelm.setIngredient('F', fragChoice);
        Bukkit.addRecipe(gHelm);

        ShapedRecipe gChest = new ShapedRecipe(recipeGuardianChestKey, createArmor(CustomArmorType.GUARDIAN_CHESTPLATE));
        gChest.shape("F F", "FFF", "FFF");
        gChest.setIngredient('F', fragChoice);
        Bukkit.addRecipe(gChest);

        ShapedRecipe gLegs = new ShapedRecipe(recipeGuardianLegsKey, createArmor(CustomArmorType.GUARDIAN_LEGGINGS));
        gLegs.shape("FFF", "F F", "F F");
        gLegs.setIngredient('F', fragChoice);
        Bukkit.addRecipe(gLegs);

        ShapedRecipe gBoots = new ShapedRecipe(recipeGuardianBootsKey, createArmor(CustomArmorType.GUARDIAN_BOOTS));
        gBoots.shape("F F", "F F", "   ");
        gBoots.setIngredient('F', fragChoice);
        Bukkit.addRecipe(gBoots);

        // Drill tier upgrades are a single recipe; output is determined dynamically in DrillUpgradeCraftListener.
        ShapedRecipe drillUpgrade = new ShapedRecipe(recipeDrillUpgradeKey, createMiningItem(MiningItemType.MITHRIL_DRILL));
        drillUpgrade.shape("FFF", "FDF", "FFF");
        drillUpgrade.setIngredient('F', new RecipeChoice.ExactChoice(createEndMinesMaterial(EndMinesMaterialType.ORE_FRAGMENT)));
        // Any Iron Pickaxe can match; DrillUpgradeCraftListener validates the input is a drill variant.
        drillUpgrade.setIngredient('D', Material.IRON_PICKAXE);
        Bukkit.addRecipe(drillUpgrade);

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
        player.discoverRecipe(recipeGuardianHelmKey);
        player.discoverRecipe(recipeGuardianChestKey);
        player.discoverRecipe(recipeGuardianLegsKey);
        player.discoverRecipe(recipeGuardianBootsKey);
        player.discoverRecipe(recipeDrillUpgradeKey);
        player.discoverRecipe(farmRecipeKey);
    }

    public double mobWeaponDropChance(YokaiType type) {
        if (type == null) {
            return mobWeaponBaseChance;
        }
        return yokaiWeaponChances.getOrDefault(type, mobWeaponBaseChance);
    }

    public double mobArmorDropChance(YokaiType type) {
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
        };
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
        };
    }

    public ItemStack createEndMinesMaterial(EndMinesMaterialType type) {
        return switch (type) {
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
            case MITHRIL_DRILL -> createDrill("MITHRIL_DRILL", "Mithril Drill");
            case TITANIUM_DRILL -> createDrill("TITANIUM_DRILL", "Titanium Drill");
            case GEMSTONE_DRILL -> createDrill("GEMSTONE_DRILL", "Gemstone Drill");
            case MITHRIL_ENGINE -> material(
                    Material.PISTON,
                    ChatColor.BLUE + "Mithril Engine",
                    "MITHRIL_ENGINE",
                    ChatColor.GRAY + "Increases drill mining speed by " + ChatColor.GREEN + "+50" + ChatColor.GRAY + ".",
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE DRILL PART"
            );
            case TITANIUM_ENGINE -> material(
                    Material.STICKY_PISTON,
                    ChatColor.DARK_PURPLE + "Titanium Engine",
                    "TITANIUM_ENGINE",
                    ChatColor.GRAY + "Increases drill mining speed by " + ChatColor.GREEN + "+100" + ChatColor.GRAY + ".",
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "EPIC DRILL PART"
            );
            case GEMSTONE_ENGINE -> material(
                    Material.OBSERVER,
                    ChatColor.GOLD + "Gemstone Engine",
                    "GEMSTONE_ENGINE",
                    ChatColor.GRAY + "Increases drill mining speed by " + ChatColor.GREEN + "+150" + ChatColor.GRAY + ".",
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY DRILL PART"
            );
            case DIVAN_ENGINE -> material(
                    Material.NETHER_STAR,
                    ChatColor.LIGHT_PURPLE + "Divan Engine",
                    "DIVAN_ENGINE",
                    ChatColor.GRAY + "Increases drill mining speed by " + ChatColor.GREEN + "+200" + ChatColor.GRAY + ".",
                    ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "MYTHIC DRILL PART"
            );
            case MEDIUM_FUEL_TANK -> material(
                    Material.BUCKET,
                    ChatColor.BLUE + "Medium Fuel Tank",
                    "MEDIUM_FUEL_TANK",
                    ChatColor.GRAY + "Increases drill fuel capacity to " + ChatColor.YELLOW + "50,000" + ChatColor.GRAY + ".",
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE DRILL PART"
            );
            case LARGE_FUEL_TANK -> material(
                    Material.MINECART,
                    ChatColor.DARK_PURPLE + "Large Fuel Tank",
                    "LARGE_FUEL_TANK",
                    ChatColor.GRAY + "Increases drill fuel capacity to " + ChatColor.YELLOW + "100,000" + ChatColor.GRAY + ".",
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
        };
    }

    private ItemStack createDrill(String id, String fallbackName) {
        // Build a SkyBlock-style Drill with fuel PDC and lore.
        ItemStack item = new ItemStack(Material.IRON_PICKAXE);
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

        // Set custom model data if defined
        Integer modelData = modelDataOverrides.get(id.toLowerCase(Locale.ROOT));
        if (modelData != null) {
            meta.setCustomModelData(modelData);
        }

        updateDrillLore(meta);
        ItemRarity rarity = rarityFromLore(meta.getLore(), ItemRarity.RARE);
        meta.setDisplayName(styledName(resolveDisplayName(id, fallbackName), rarity, true));

        // Hide attributes to keep lore clean
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
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
        if (!isCustomDungeonWeapon(weapon) || !weapon.hasItemMeta()) {
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

    public List<String> allItemKeys() {
        List<String> keys = new ArrayList<>();
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
        
        // 2. Ability
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

        // 3. Enchants
        List<String> enchantLines = enchantLoreLines(enchants);
        if (!enchantLines.isEmpty()) {
            lore.add("");
            lore.addAll(enchantLines);
        }

        // 4. Rarity
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
        
        // Add enchantment glint for all enchanted items
        Map<Enchantment, Integer> enchants = Map.of();
        boolean hideEnchants = false;
        
        if (id.startsWith("ENCHANTED_") || id.contains("_ESSENCE") || id.contains("_SIGIL") || id.contains("_CORE")) {
            enchants = Map.of(Enchantment.UNBREAKING, 1);
            hideEnchants = true;
        }
        
        return build(material, name, id, lore, enchants, hideEnchants, true);
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
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        ItemRarity rarity = rarityFromLore(lore, ItemRarity.RARE);
        boolean weapon = isWeaponId(id);
        boolean reforgeable = !markAsMaterial && (weapon || CustomArmorType.parse(id) != null);

        meta.setDisplayName(styledName(resolveDisplayName(id, name), rarity, weapon));
        meta.setLore(styledLore(lore, reforgeable));
        meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, id);
        if (markAsMaterial) {
            meta.getPersistentDataContainer().set(customMaterialKey, PersistentDataType.STRING, id);
        }
        if (hideEnchants) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);

        Integer model = modelDataOverrides.get(id.toLowerCase(Locale.ROOT));
        if (model != null) {
            meta.setCustomModelData(model);
            item.setItemMeta(meta);
        }

        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            item.addUnsafeEnchantment(entry.getKey(), entry.getValue());
        }

        // Apply custom head textures globally
        if (material == Material.PLAYER_HEAD && item.getItemMeta() instanceof SkullMeta skullMeta) {
            if (id.equals("GUARDIAN_HELM")) {
                applyTexture(skullMeta, "b27c74c3-fac5-43b5-85bc-2b153b901d66", "ewogICJ0aW1lc3RhbXAiIDogMTc1MDY0NjgyNDIxMSwKICAicHJvZmlsZUlkIiA6ICI1ODc5MjNlNDkxMzM0ZDMzYWE4ZjQ3ZWJkZTljOTc3MiIsCiAgInByb2ZpbGVOYW1lIiA6ICJFbGV2ZW5mb3VyMTAiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjMyYmZiOGQ0ZjgxYWViNzFkMzQxMDJlM2JiY2JjZTU4MTM3YmYzNGU0NjczYWI1ZjJlZWI0OWE1MjhiZTBhMiIKICAgIH0KICB9Cn0=", "R0LRglh9wCuk+3rJFwx+QkwC6Nto3b3fyro3fJYl79V5FiLlEKdTIGD5c6RTMoCK1r6mxAanOPBGju0pCiP2GRXXAaWmVW0FuolT9LT1kx7VcUj6GrVV+PYj6i3zF3Pu7L/iO/D3oYYXr+f7ai62H47Ansl0EYSZNOC+Slu/3pc/2/tPwIAj534KwdWoRWa6KxMlyf8poFN6leOS7XAQXOIlc7GfMpg0UlABAxGKbRwdJqP9lCnv0a6CYu3G7YF3B733vx4FWvlmpTNzGmKcUYAsId1jGnzYflviCkPHrfICmWrutAK/5HrrM5bKMPVMY/diIcHnPbhPWnpdnECur9iWGFyM5sPPOALSno5TRF0BntrjVCcWsgEdJgYtZoqLsj64mJcTJn2S7mSlVFcGgeNTyqPiFvS6pahS+VXL4mNtJ/wGK4Vlms/0SJNcJmkHQv4HX1SFiHxbgq2xz73z+yCysiwHLS6KMRnfFuu/9J9wDxzmlncnJWP77VassjlA6+a2CxrWjUHoEjuhl+lbSpOmD1xN9Z3orYPWAoH5+F2Lj0+bRVNuf+CBjWjkjUOeQ/3aM0kW8f8pxU2+4PS36AGQcmpoFjtQaE3psRXPWC1zxfayrqlV7qpnmnGJbu+dmNolRHGnLHWvVqYr3oZj30PgTiyYizWuCz3tmZgH87A=");
            } else if (id.equals("GUARDIAN_FRAGMENT")) {
                applyTexture(skullMeta, "58c6fb76-db3c-7718-5810-2e14e6dcc7f2", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNThjNmZiNzZkYjNjNzcxODU4MTAyZTE0ZTZkY2M3ZjI3OTFmNTdlYjkzYzBkNGNiZDljOGExZDlkY2JlMTAzMCJ9fX0=", null);
            }
            item.setItemMeta(skullMeta);
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
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("resource-pack.name-map");
        if (section == null) {
            return fallback;
        }
        String mapped = section.getString(id.toLowerCase(Locale.ROOT));
        if (mapped == null || mapped.isBlank()) {
            return fallback;
        }
        return ChatColor.translateAlternateColorCodes('&', mapped);
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
        String normalized = strip(name);
        normalized = normalized.stripLeading();
        while (!normalized.isEmpty() && isStarSymbol(normalized.charAt(0))) {
            normalized = normalized.substring(1).stripLeading();
        }
        return normalized;
    }

    private boolean isStarSymbol(char c) {
        return c == '✪' || c == '★' || c == '☆';
    }

    private void loadModelOverrides() {
        modelDataOverrides.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("resource-pack.models");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            int value = section.getInt(key, -1);
            if (value > 0) {
                modelDataOverrides.put(key.toLowerCase(Locale.ROOT), value);
            }
        }
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
            case GILDED_HARVESTER -> ChatColor.GOLD + "Full Set Bonus: Bountiful Harvest";
            case DREADLORD -> ChatColor.DARK_GRAY + "Full Set Bonus: Dread Aura";
            case NECROMANCER -> ChatColor.DARK_GREEN + "Full Set Bonus: Undead Commander";
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
        yokaiWeaponChances.put(YokaiType.ONI_BRUTE, 0.020D);
        yokaiWeaponChances.put(YokaiType.TENGU_SKIRMISHER, 0.022D);
        yokaiWeaponChances.put(YokaiType.KAPPA_RAIDER, 0.018D);
        yokaiWeaponChances.put(YokaiType.ONRYO_WRAITH, 0.016D);
        yokaiWeaponChances.put(YokaiType.JOROGUMO_WEAVER, 0.016D);
        yokaiWeaponChances.put(YokaiType.KITSUNE_TRICKSTER, 0.018D);
        yokaiWeaponChances.put(YokaiType.GASHADOKURO_SENTINEL, 0.035D);

        mobArmorBaseChance = 0.010D;
        yokaiArmorChances.clear();
        yokaiArmorChances.put(YokaiType.ONI_BRUTE, 0.014D);
        yokaiArmorChances.put(YokaiType.TENGU_SKIRMISHER, 0.011D);
        yokaiArmorChances.put(YokaiType.KAPPA_RAIDER, 0.010D);
        yokaiArmorChances.put(YokaiType.ONRYO_WRAITH, 0.010D);
        yokaiArmorChances.put(YokaiType.JOROGUMO_WEAVER, 0.009D);
        yokaiArmorChances.put(YokaiType.KITSUNE_TRICKSTER, 0.011D);
        yokaiArmorChances.put(YokaiType.GASHADOKURO_SENTINEL, 0.022D);

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
        ItemRarity drillRarity = switch (drillId) {
            case "MITHRIL_DRILL" -> ItemRarity.EPIC;
            case "TITANIUM_DRILL" -> ItemRarity.LEGENDARY;
            case "GEMSTONE_DRILL" -> ItemRarity.MYTHIC;
            default -> ItemRarity.RARE;
        };
        int baseSpeed = switch (drillId) {
            case "MITHRIL_DRILL" -> 600;
            case "TITANIUM_DRILL" -> 800;
            case "GEMSTONE_DRILL" -> 1000;
            default -> 400;
        };
        int breakingPower = switch (drillId) {
            case "MITHRIL_DRILL" -> 6;
            case "TITANIUM_DRILL" -> 7;
            case "GEMSTONE_DRILL" -> 8;
            default -> 5;
        };

        String engine = pdc.getOrDefault(drillEngineKey, PersistentDataType.STRING, "BASIC_ENGINE");
        String tank = pdc.getOrDefault(drillTankKey, PersistentDataType.STRING, "SMALL_TANK");
        int fuel = pdc.getOrDefault(drillFuelKey, PersistentDataType.INTEGER, 0);

        int bonusSpeed = switch (engine) {
            case "MITHRIL_ENGINE" -> 50;
            case "TITANIUM_ENGINE" -> 100;
            case "GEMSTONE_ENGINE" -> 150;
            case "DIVAN_ENGINE" -> 200;
            default -> 0;
        };
        
        int maxFuel = switch (tank) {
            case "MEDIUM_FUEL_TANK" -> 50000;
            case "LARGE_FUEL_TANK" -> 100000;
            default -> 20000;
        };
        
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
        lore.add(ChatColor.GRAY + "Mining Speed: " + ChatColor.GREEN + "+" + (baseSpeed + bonusSpeed));
        lore.add(ChatColor.GRAY + "Breaking Power: " + ChatColor.AQUA + breakingPower);
        lore.add("");
        lore.add(ChatColor.GRAY + "Fuel: " + ChatColor.YELLOW + formatInt(fuel) + ChatColor.GRAY + "/" + ChatColor.YELLOW + formatInt(maxFuel));
        lore.add("");
        lore.add(ChatColor.GOLD + "" + ChatColor.BOLD + "DRILL PARTS:");
        lore.add(ChatColor.DARK_GRAY + "Engine: " + ChatColor.GRAY + formatPartName(engine));
        lore.add(ChatColor.DARK_GRAY + "Fuel Tank: " + ChatColor.GRAY + formatPartName(tank));
        lore.add("");
        lore.add(drillRarity.color() + "" + ChatColor.BOLD + drillRarity.name() + " DRILL");

        meta.setLore(styledLore(lore, false));
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

    public void setGrapplingHookManager(io.papermc.Grivience.item.GrapplingHookManager manager) {
        this.grapplingHookManager = manager;
    }

    private static String formatInt(int value) {
        return String.format(java.util.Locale.US, "%,d", value);
    }

}


