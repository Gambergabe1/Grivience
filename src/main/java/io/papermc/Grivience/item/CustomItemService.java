package io.papermc.Grivience.item;

import io.papermc.Grivience.dungeon.YokaiType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CustomItemService {
    private final JavaPlugin plugin;
    private final NamespacedKey itemIdKey;
    private final NamespacedKey reforgeIdKey;
    private final NamespacedKey recipeFlyingRaijinKey;

    private ItemStyle itemStyle = ItemStyle.JAPANESE;
    private double mobWeaponBaseChance;
    private final EnumMap<YokaiType, Double> yokaiWeaponChances = new EnumMap<>(YokaiType.class);
    private double mobArmorBaseChance;
    private final EnumMap<YokaiType, Double> yokaiArmorChances = new EnumMap<>(YokaiType.class);
    private double mobReforgeStoneChance;
    private double stormSigilChance;
    private double thunderEssenceChance;
    private double raijinCoreChance;
    private double flyingRaijinChance;
    private double bossArmorChance;
    private double bossReforgeStoneChance;

    public CustomItemService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.itemIdKey = new NamespacedKey(plugin, "custom-item-id");
        this.reforgeIdKey = new NamespacedKey(plugin, "custom-reforge-id");
        this.recipeFlyingRaijinKey = new NamespacedKey(plugin, "flying-raijin-recipe");
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
        flyingRaijinChance = clampChance(dropSection.getDouble("boss-materials.flying-raijin", flyingRaijinChance));
        bossArmorChance = clampChance(dropSection.getDouble("boss-materials.armor-piece", bossArmorChance));
        bossReforgeStoneChance = clampChance(dropSection.getDouble("boss-materials.reforge-stone", bossReforgeStoneChance));
    }

    public void registerRecipes() {
        Bukkit.removeRecipe(recipeFlyingRaijinKey);

        ShapedRecipe recipe = new ShapedRecipe(recipeFlyingRaijinKey, createWeapon(CustomWeaponType.FLYING_RAIJIN));
        recipe.shape("STS", " R ", " N ");
        recipe.setIngredient('S', new RecipeChoice.ExactChoice(createCraftingItem(RaijinCraftingItemType.STORM_SIGIL)));
        recipe.setIngredient('T', new RecipeChoice.ExactChoice(createCraftingItem(RaijinCraftingItemType.THUNDER_ESSENCE)));
        recipe.setIngredient('R', new RecipeChoice.ExactChoice(createCraftingItem(RaijinCraftingItemType.RAIJIN_CORE)));
        recipe.setIngredient('N', Material.NETHERITE_SWORD);
        Bukkit.addRecipe(recipe);
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

    public double flyingRaijinChance() {
        return flyingRaijinChance;
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
                    ChatColor.DARK_GRAY + "Damage: " + ChatColor.RED + "+125",
                    ChatColor.DARK_GRAY + "Strength: " + ChatColor.RED + "+45",
                    ChatColor.GRAY + "Heavy demon cleaver infused with rage.",
                    ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "EPIC DUNGEON WEAPON",
                    Map.of(Enchantment.SHARPNESS, 4, Enchantment.SWEEPING_EDGE, 2)
            );
            case TENGU_GALEBLADE -> weapon(
                    Material.DIAMOND_SWORD,
                    ChatColor.AQUA + "Tengu Galeblade",
                    "TENGU_GALEBLADE",
                    ChatColor.DARK_GRAY + "Damage: " + ChatColor.RED + "+110",
                    ChatColor.DARK_GRAY + "Crit Chance: " + ChatColor.BLUE + "+18%",
                    ChatColor.GRAY + "A blade that rides the mountain winds.",
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE DUNGEON WEAPON",
                    Map.of(Enchantment.SHARPNESS, 3, Enchantment.KNOCKBACK, 1)
            );
            case TENGU_STORMBOW -> weapon(
                    Material.BOW,
                    ChatColor.AQUA + "Tengu Stormbow",
                    "TENGU_STORMBOW",
                    ChatColor.DARK_GRAY + "Damage: " + ChatColor.RED + "+118",
                    ChatColor.DARK_GRAY + "Crit Chance: " + ChatColor.BLUE + "+22%",
                    ChatColor.GRAY + "A feathered bow that rides shrine tempests.",
                    ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "EPIC DUNGEON WEAPON",
                    Map.of(Enchantment.POWER, 6, Enchantment.PUNCH, 2, Enchantment.INFINITY, 1)
            );
            case KAPPA_TIDEBREAKER -> weapon(
                    Material.TRIDENT,
                    ChatColor.GREEN + "Kappa Tidebreaker",
                    "KAPPA_TIDEBREAKER",
                    ChatColor.DARK_GRAY + "Damage: " + ChatColor.RED + "+120",
                    ChatColor.DARK_GRAY + "Seaforce: " + ChatColor.BLUE + "+30",
                    ChatColor.GRAY + "Forged in shrine rivers and moonlit pools.",
                    ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "EPIC DUNGEON WEAPON",
                    Map.of(Enchantment.IMPALING, 5, Enchantment.LOYALTY, 1)
            );
            case ONRYO_SPIRITBLADE -> weapon(
                    Material.NETHERITE_SWORD,
                    ChatColor.GRAY + "Onryo Spiritblade",
                    "ONRYO_SPIRITBLADE",
                    ChatColor.DARK_GRAY + "Damage: " + ChatColor.RED + "+145",
                    ChatColor.DARK_GRAY + "Ferocity: " + ChatColor.BLUE + "+12",
                    ChatColor.GRAY + "Whispers of grudges cling to its edge.",
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY DUNGEON WEAPON",
                    Map.of(Enchantment.SHARPNESS, 5, Enchantment.FIRE_ASPECT, 2)
            );
            case JOROGUMO_STINGER -> weapon(
                    Material.STONE_SWORD,
                    ChatColor.DARK_GREEN + "Jorogumo Stinger",
                    "JOROGUMO_STINGER",
                    ChatColor.DARK_GRAY + "Damage: " + ChatColor.RED + "+95",
                    ChatColor.DARK_GRAY + "Attack Speed: " + ChatColor.BLUE + "+20%",
                    ChatColor.GRAY + "A fang blade coated in cursed silk venom.",
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE DUNGEON WEAPON",
                    Map.of(Enchantment.BANE_OF_ARTHROPODS, 5, Enchantment.LOOTING, 2)
            );
            case KITSUNE_FANG -> weapon(
                    Material.GOLDEN_SWORD,
                    ChatColor.GOLD + "Kitsune Fang",
                    "KITSUNE_FANG",
                    ChatColor.DARK_GRAY + "Damage: " + ChatColor.RED + "+108",
                    ChatColor.DARK_GRAY + "Intelligence: " + ChatColor.BLUE + "+60",
                    ChatColor.GRAY + "Its flame dances between illusion and ruin.",
                    ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "EPIC DUNGEON WEAPON",
                    Map.of(Enchantment.SHARPNESS, 4, Enchantment.FIRE_ASPECT, 2, Enchantment.LOOTING, 2)
            );
            case KITSUNE_DAWNBOW -> weapon(
                    Material.BOW,
                    ChatColor.GOLD + "Kitsune Dawnbow",
                    "KITSUNE_DAWNBOW",
                    ChatColor.DARK_GRAY + "Damage: " + ChatColor.RED + "+126",
                    ChatColor.DARK_GRAY + "Crit Damage: " + ChatColor.BLUE + "+40%",
                    ChatColor.GRAY + "Foxfire threads blaze along its drawn string.",
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY DUNGEON WEAPON",
                    Map.of(Enchantment.POWER, 7, Enchantment.FLAME, 1, Enchantment.INFINITY, 1, Enchantment.PUNCH, 1)
            );
            case GASHADOKURO_NODACHI -> weapon(
                    Material.NETHERITE_SWORD,
                    ChatColor.WHITE + "Gashadokuro Nodachi",
                    "GASHADOKURO_NODACHI",
                    ChatColor.DARK_GRAY + "Damage: " + ChatColor.RED + "+165",
                    ChatColor.DARK_GRAY + "Strength: " + ChatColor.RED + "+70",
                    ChatColor.GRAY + "Built from giant bones of forgotten wars.",
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY DUNGEON WEAPON",
                    Map.of(Enchantment.SHARPNESS, 6, Enchantment.LOOTING, 3)
            );
            case FLYING_RAIJIN -> weapon(
                    Material.NETHERITE_SWORD,
                    ChatColor.YELLOW + "" + ChatColor.BOLD + "Flying Raijin",
                    "FLYING_RAIJIN",
                    ChatColor.DARK_GRAY + "Damage: " + ChatColor.RED + "+210",
                    ChatColor.DARK_GRAY + "Strength: " + ChatColor.RED + "+100",
                    ChatColor.DARK_GRAY + "Ferocity: " + ChatColor.BLUE + "+25",
                    ChatColor.GRAY + "Ability: " + ChatColor.YELLOW + "Thunder Step",
                    ChatColor.DARK_GRAY + "Dash forward and strike with lightning force.",
                    ChatColor.GOLD + "" + ChatColor.BOLD + "MYTHIC DUNGEON WEAPON",
                    Map.of(Enchantment.SHARPNESS, 7, Enchantment.LOOTING, 4, Enchantment.UNBREAKING, 5, Enchantment.MENDING, 1)
            );
        };
    }

    public ItemStack createArmor(CustomArmorType type) {
        return switch (type) {
            case SHOGUN_KABUTO -> armor(
                    Material.NETHERITE_HELMET,
                    ChatColor.DARK_RED + "Shogun Kabuto",
                    "SHOGUN_KABUTO",
                    ChatColor.DARK_GRAY + "Defense: " + ChatColor.GREEN + "+22",
                    ChatColor.DARK_GRAY + "Health: " + ChatColor.RED + "+45",
                    ChatColor.DARK_GRAY + "Heal Speed: " + ChatColor.BLUE + "+4",
                    armorDescriptionLore(type),
                    fullSetBonusLore(type.setType()),
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY DUNGEON ARMOR",
                    Map.of(Enchantment.PROTECTION, 5, Enchantment.UNBREAKING, 4)
            );
            case SHOGUN_DO_MARU -> armor(
                    Material.NETHERITE_CHESTPLATE,
                    ChatColor.DARK_RED + "Shogun Do-Maru",
                    "SHOGUN_DO_MARU",
                    ChatColor.DARK_GRAY + "Defense: " + ChatColor.GREEN + "+34",
                    ChatColor.DARK_GRAY + "Health: " + ChatColor.RED + "+80",
                    ChatColor.DARK_GRAY + "Heal Speed: " + ChatColor.BLUE + "+6",
                    armorDescriptionLore(type),
                    fullSetBonusLore(type.setType()),
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY DUNGEON ARMOR",
                    Map.of(Enchantment.PROTECTION, 6, Enchantment.UNBREAKING, 4)
            );
            case SHOGUN_HAIDATE -> armor(
                    Material.NETHERITE_LEGGINGS,
                    ChatColor.DARK_RED + "Shogun Haidate",
                    "SHOGUN_HAIDATE",
                    ChatColor.DARK_GRAY + "Defense: " + ChatColor.GREEN + "+28",
                    ChatColor.DARK_GRAY + "Health: " + ChatColor.RED + "+62",
                    ChatColor.DARK_GRAY + "Heal Speed: " + ChatColor.BLUE + "+5",
                    armorDescriptionLore(type),
                    fullSetBonusLore(type.setType()),
                    ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "EPIC DUNGEON ARMOR",
                    Map.of(Enchantment.PROTECTION, 5, Enchantment.UNBREAKING, 4)
            );
            case SHOGUN_GETA -> armor(
                    Material.NETHERITE_BOOTS,
                    ChatColor.DARK_RED + "Shogun Geta",
                    "SHOGUN_GETA",
                    ChatColor.DARK_GRAY + "Defense: " + ChatColor.GREEN + "+18",
                    ChatColor.DARK_GRAY + "Health: " + ChatColor.RED + "+34",
                    ChatColor.DARK_GRAY + "Heal Speed: " + ChatColor.BLUE + "+3",
                    armorDescriptionLore(type),
                    fullSetBonusLore(type.setType()),
                    ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "EPIC DUNGEON ARMOR",
                    Map.of(Enchantment.PROTECTION, 5, Enchantment.FEATHER_FALLING, 4, Enchantment.UNBREAKING, 4)
            );
            case SHINOBI_MENPO -> armor(
                    Material.DIAMOND_HELMET,
                    ChatColor.DARK_AQUA + "Shinobi Menpo",
                    "SHINOBI_MENPO",
                    ChatColor.DARK_GRAY + "Defense: " + ChatColor.GREEN + "+16",
                    ChatColor.DARK_GRAY + "Health: " + ChatColor.RED + "+28",
                    ChatColor.DARK_GRAY + "Heal Speed: " + ChatColor.BLUE + "+8",
                    armorDescriptionLore(type),
                    fullSetBonusLore(type.setType()),
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE DUNGEON ARMOR",
                    Map.of(Enchantment.PROTECTION, 4, Enchantment.RESPIRATION, 3, Enchantment.UNBREAKING, 3)
            );
            case SHINOBI_JACKET -> armor(
                    Material.DIAMOND_CHESTPLATE,
                    ChatColor.DARK_AQUA + "Shinobi Jacket",
                    "SHINOBI_JACKET",
                    ChatColor.DARK_GRAY + "Defense: " + ChatColor.GREEN + "+24",
                    ChatColor.DARK_GRAY + "Health: " + ChatColor.RED + "+56",
                    ChatColor.DARK_GRAY + "Heal Speed: " + ChatColor.BLUE + "+9",
                    armorDescriptionLore(type),
                    fullSetBonusLore(type.setType()),
                    ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "EPIC DUNGEON ARMOR",
                    Map.of(Enchantment.PROTECTION, 5, Enchantment.UNBREAKING, 3)
            );
            case SHINOBI_LEGGINGS -> armor(
                    Material.DIAMOND_LEGGINGS,
                    ChatColor.DARK_AQUA + "Shinobi Leggings",
                    "SHINOBI_LEGGINGS",
                    ChatColor.DARK_GRAY + "Defense: " + ChatColor.GREEN + "+20",
                    ChatColor.DARK_GRAY + "Health: " + ChatColor.RED + "+44",
                    ChatColor.DARK_GRAY + "Heal Speed: " + ChatColor.BLUE + "+8",
                    armorDescriptionLore(type),
                    fullSetBonusLore(type.setType()),
                    ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "EPIC DUNGEON ARMOR",
                    Map.of(Enchantment.PROTECTION, 5, Enchantment.UNBREAKING, 3)
            );
            case SHINOBI_TABI -> armor(
                    Material.DIAMOND_BOOTS,
                    ChatColor.DARK_AQUA + "Shinobi Tabi",
                    "SHINOBI_TABI",
                    ChatColor.DARK_GRAY + "Defense: " + ChatColor.GREEN + "+14",
                    ChatColor.DARK_GRAY + "Health: " + ChatColor.RED + "+24",
                    ChatColor.DARK_GRAY + "Heal Speed: " + ChatColor.BLUE + "+10",
                    armorDescriptionLore(type),
                    fullSetBonusLore(type.setType()),
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE DUNGEON ARMOR",
                    Map.of(Enchantment.PROTECTION, 4, Enchantment.FEATHER_FALLING, 5, Enchantment.UNBREAKING, 3)
            );
            case ONMYOJI_EBOSHI -> armor(
                    Material.GOLDEN_HELMET,
                    ChatColor.LIGHT_PURPLE + "Onmyoji Eboshi",
                    "ONMYOJI_EBOSHI",
                    ChatColor.DARK_GRAY + "Defense: " + ChatColor.GREEN + "+18",
                    ChatColor.DARK_GRAY + "Health: " + ChatColor.RED + "+40",
                    ChatColor.DARK_GRAY + "Heal Speed: " + ChatColor.BLUE + "+6",
                    armorDescriptionLore(type),
                    fullSetBonusLore(type.setType()),
                    ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "EPIC DUNGEON ARMOR",
                    Map.of(Enchantment.PROTECTION, 5, Enchantment.UNBREAKING, 3)
            );
            case ONMYOJI_ROBE -> armor(
                    Material.GOLDEN_CHESTPLATE,
                    ChatColor.LIGHT_PURPLE + "Onmyoji Robe",
                    "ONMYOJI_ROBE",
                    ChatColor.DARK_GRAY + "Defense: " + ChatColor.GREEN + "+29",
                    ChatColor.DARK_GRAY + "Health: " + ChatColor.RED + "+74",
                    ChatColor.DARK_GRAY + "Heal Speed: " + ChatColor.BLUE + "+8",
                    armorDescriptionLore(type),
                    fullSetBonusLore(type.setType()),
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY DUNGEON ARMOR",
                    Map.of(Enchantment.PROTECTION, 6, Enchantment.UNBREAKING, 4)
            );
            case ONMYOJI_HAKAMA -> armor(
                    Material.GOLDEN_LEGGINGS,
                    ChatColor.LIGHT_PURPLE + "Onmyoji Hakama",
                    "ONMYOJI_HAKAMA",
                    ChatColor.DARK_GRAY + "Defense: " + ChatColor.GREEN + "+24",
                    ChatColor.DARK_GRAY + "Health: " + ChatColor.RED + "+56",
                    ChatColor.DARK_GRAY + "Heal Speed: " + ChatColor.BLUE + "+7",
                    armorDescriptionLore(type),
                    fullSetBonusLore(type.setType()),
                    ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "EPIC DUNGEON ARMOR",
                    Map.of(Enchantment.PROTECTION, 5, Enchantment.UNBREAKING, 4)
            );
            case ONMYOJI_SANDALS -> armor(
                    Material.GOLDEN_BOOTS,
                    ChatColor.LIGHT_PURPLE + "Onmyoji Sandals",
                    "ONMYOJI_SANDALS",
                    ChatColor.DARK_GRAY + "Defense: " + ChatColor.GREEN + "+16",
                    ChatColor.DARK_GRAY + "Health: " + ChatColor.RED + "+30",
                    ChatColor.DARK_GRAY + "Heal Speed: " + ChatColor.BLUE + "+6",
                    armorDescriptionLore(type),
                    fullSetBonusLore(type.setType()),
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE DUNGEON ARMOR",
                    Map.of(Enchantment.PROTECTION, 4, Enchantment.FEATHER_FALLING, 4, Enchantment.UNBREAKING, 3)
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
                    ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "EPIC CRAFTING MATERIAL"
            );
            case RAIJIN_CORE -> material(
                    Material.HEART_OF_THE_SEA,
                    ChatColor.GOLD + "Raijin Core",
                    "RAIJIN_CORE",
                    ChatColor.GRAY + "The divine core required for Flying Raijin.",
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY CRAFTING MATERIAL"
            );
        };
    }

    public ItemStack createReforgeStone(ReforgeStoneType type) {
        return switch (type) {
            case JAGGED_STONE -> material(
                    type.material(),
                    ChatColor.RED + "Jagged Stone",
                    "JAGGED_STONE",
                    ChatColor.GRAY + "Applies " + ReforgeType.JAGGED.color() + ReforgeType.JAGGED.displayName()
                            + ChatColor.GRAY + " to a dungeon weapon.",
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE REFORGE STONE"
            );
            case TITAN_STONE -> material(
                    type.material(),
                    ChatColor.GOLD + "Titan Stone",
                    "TITAN_STONE",
                    ChatColor.GRAY + "Applies " + ReforgeType.TITANIC.color() + ReforgeType.TITANIC.displayName()
                            + ChatColor.GRAY + " to a dungeon weapon.",
                    ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "EPIC REFORGE STONE"
            );
            case ARCANE_STONE -> material(
                    type.material(),
                    ChatColor.LIGHT_PURPLE + "Arcane Stone",
                    "ARCANE_STONE",
                    ChatColor.GRAY + "Applies " + ReforgeType.ARCANE.color() + ReforgeType.ARCANE.displayName()
                            + ChatColor.GRAY + " to a dungeon weapon.",
                    ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "EPIC REFORGE STONE"
            );
            case TEMPEST_STONE -> material(
                    type.material(),
                    ChatColor.AQUA + "Tempest Stone",
                    "TEMPEST_STONE",
                    ChatColor.GRAY + "Applies " + ReforgeType.TEMPEST.color() + ReforgeType.TEMPEST.displayName()
                            + ChatColor.GRAY + " to a dungeon weapon.",
                    ChatColor.GOLD + "" + ChatColor.BOLD + "LEGENDARY REFORGE STONE"
            );
        };
    }

    public ItemStack createReforgeAnvil() {
        return material(
                Material.ANVIL,
                ChatColor.DARK_AQUA + "Reforge Anvil",
                "REFORGE_ANVIL",
                ChatColor.GRAY + "Right-click to open the reforge menu",
                ChatColor.AQUA + "" + ChatColor.BOLD + "UTILITY ITEM"
        );
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

        List<String> lore = meta.hasLore() && meta.getLore() != null
                ? new ArrayList<>(meta.getLore())
                : new ArrayList<>();
        lore.removeIf(this::isReforgeLoreLine);
        lore.add(ChatColor.DARK_GRAY + "Reforge: " + reforgeType.color() + reforgeType.displayName());
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

    public String reforgeBonusLine(ReforgeType type) {
        if (type == null) {
            return ChatColor.GRAY + "No reforge";
        }
        List<String> parts = new ArrayList<>();
        if (type.damageBonus() > 0.0D) {
            parts.add(ChatColor.RED + "+" + (int) type.damageBonus() + " Damage");
        }
        if (type.strengthBonus() > 0.0D) {
            parts.add(ChatColor.RED + "+" + (int) type.strengthBonus() + " Strength");
        }
        if (type.critChanceBonus() > 0.0D) {
            parts.add(ChatColor.BLUE + "+" + (int) type.critChanceBonus() + "% Crit Chance");
        }
        if (type.critDamageBonus() > 0.0D) {
            parts.add(ChatColor.BLUE + "+" + (int) type.critDamageBonus() + "% Crit Damage");
        }
        return String.join(ChatColor.GRAY + ", ", parts);
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

        ReforgeStoneType reforgeStoneType = ReforgeStoneType.parse(key);
        if (reforgeStoneType != null) {
            return createReforgeStone(reforgeStoneType);
        }

        if ("REFORGE_ANVIL".equalsIgnoreCase(key)) {
            return createReforgeAnvil();
        }
        return null;
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
        for (ReforgeStoneType type : ReforgeStoneType.values()) {
            keys.add(type.name().toLowerCase());
        }
        keys.add("reforge_anvil");
        return keys;
    }

    private ItemStack weapon(
            Material material,
            String name,
            String id,
            String line1,
            String line2,
            String line3,
            String rarity,
            Map<Enchantment, Integer> enchants
    ) {
        List<String> lore = new ArrayList<>();
        lore.add(line1);
        lore.add(line2);
        lore.addAll(enchantLoreLines(enchants));
        lore.add("");
        lore.add(line3);
        lore.add("");
        lore.add(rarity);
        return build(material, name, id, lore, enchants, true);
    }

    private ItemStack weapon(
            Material material,
            String name,
            String id,
            String line1,
            String line2,
            String line3,
            String line4,
            String line5,
            String rarity,
            Map<Enchantment, Integer> enchants
    ) {
        List<String> lore = new ArrayList<>();
        lore.add(line1);
        lore.add(line2);
        lore.add(line3);
        lore.addAll(enchantLoreLines(enchants));
        lore.add("");
        lore.add(line4);
        lore.add(line5);
        lore.add("");
        lore.add(rarity);
        return build(material, name, id, lore, enchants, true);
    }

    private ItemStack material(Material material, String name, String id, String line1, String rarity) {
        List<String> lore = new ArrayList<>();
        lore.add(line1);
        lore.add("");
        lore.add(rarity);
        return build(material, name, id, lore, Map.of(), false);
    }

    private ItemStack armor(
            Material material,
            String name,
            String id,
            String line1,
            String line2,
            String line3,
            String line4,
            String line5,
            String rarity,
            Map<Enchantment, Integer> enchants
    ) {
        List<String> lore = new ArrayList<>();
        lore.add(line1);
        lore.add(line2);
        lore.add(line3);
        lore.add("");
        lore.add(line4);
        lore.add(line5);
        lore.add("");
        lore.add(rarity);
        return build(material, name, id, lore, enchants, true);
    }

    private ItemStack build(
            Material material,
            String name,
            String id,
            List<String> lore,
            Map<Enchantment, Integer> enchants,
            boolean hideEnchants
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(styledName(name));
        meta.setLore(styledLore(lore, hideEnchants));
        meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, id);
        if (hideEnchants) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);

        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            item.addUnsafeEnchantment(entry.getKey(), entry.getValue());
        }
        return item;
    }

    private String styledName(String original) {
        return switch (itemStyle) {
            case JAPANESE -> original;
            case SKYBLOCK -> ChatColor.GOLD + "✪ " + ChatColor.RESET + original;
            case MINIMAL -> ChatColor.WHITE + strip(original);
        };
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

        List<String> skyblock = new ArrayList<>(lore);
        skyblock.add("");
        if (weapon) {
            skyblock.add(ChatColor.GRAY + "This item can be reforged!");
        } else {
            skyblock.add(ChatColor.GRAY + "Used in advanced recipes.");
        }
        return skyblock;
    }

    private String fullSetBonusLore(ArmorSetType setType) {
        return switch (setType) {
            case SHOGUN -> ChatColor.DARK_GRAY + "Full Set Bonus: " + ChatColor.RED + "Warlord's Resolve";
            case SHINOBI -> ChatColor.DARK_GRAY + "Full Set Bonus: " + ChatColor.AQUA + "Shadowstep";
            case ONMYOJI -> ChatColor.DARK_GRAY + "Full Set Bonus: " + ChatColor.LIGHT_PURPLE + "Spirit Ward";
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
        return stripped != null && stripped.toLowerCase().startsWith("reforge:");
    }

    private boolean isEnchantLoreLine(String line) {
        if (line == null) {
            return false;
        }
        String stripped = strip(line);
        return stripped != null && stripped.toLowerCase(Locale.ROOT).startsWith("enchant:");
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

        List<String> lines = new ArrayList<>(entries.size());
        for (Map.Entry<Enchantment, Integer> entry : entries) {
            String name = enchantDisplayName(entry.getKey());
            int level = Math.max(1, entry.getValue());
            lines.add(ChatColor.BLUE + "Enchant: " + ChatColor.AQUA + name + " " + roman(level));
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
        flyingRaijinChance = 0.015D;
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
}
