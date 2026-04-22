package io.papermc.Grivience.enchantment;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Registry containing all Skyblock enchantments.
 * Contains 100+ enchantments matching Skyblock's system.
 */
public class EnchantmentRegistry {
    private static final Map<String, SkyblockEnchantment> ENCHANTMENTS = new LinkedHashMap<>();

    /**
     * Initialize and register all enchantments.
     */
    public static void init() {
        if (!ENCHANTMENTS.isEmpty()) {
            return; // Already initialized
        }

        // ==================== COMBAT ENCHANTS (SWORD/AXE) ====================

        // Sharpness
        register(SkyblockEnchantment.builder("sharpness", "Sharpness")
            .type(EnchantmentType.COMMON)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(7)
            .vanillaEnchantment(Enchantment.SHARPNESS)
            .description(
                "Increases melee damage by {level} damage per hit.",
                "Each level adds +1 damage."
            )
            .baseXpCost(10)
            .conflictsWith("smite", "bane_of_arthropods", "cleave")
            .icon(Material.IRON_SWORD)
            .build());

        // Smite
        register(SkyblockEnchantment.builder("smite", "Smite")
            .type(EnchantmentType.UNCOMMON)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(7)
            .vanillaEnchantment(Enchantment.SMITE)
            .description(
                "Deal +{level}x damage to zombie-type enemies.",
                "Effective against zombies, zombie pigmen, and wither skeletons."
            )
            .baseXpCost(8)
            .conflictsWith("sharpness", "bane_of_arthropods", "cleave")
            .icon(Material.WITHER_SKELETON_SKULL)
            .build());

        // Bane of Arthropods
        register(SkyblockEnchantment.builder("bane_of_arthropods", "Bane of Arthropods")
            .type(EnchantmentType.UNCOMMON)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(7)
            .vanillaEnchantment(Enchantment.BANE_OF_ARTHROPODS)
            .description(
                "Deal +{level}x damage to arthropod enemies.",
                "Effective against spiders, cave spiders, silverfish, and endermites."
            )
            .baseXpCost(8)
            .conflictsWith("sharpness", "smite", "cleave")
            .icon(Material.SPIDER_EYE)
            .build());

        // Cleave
        register(SkyblockEnchantment.builder("cleave", "Cleave")
            .type(EnchantmentType.RARE)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(6)
            .description(
                "Hit up to {level} additional enemies near your target.",
                "Each cleave deals 50% of original damage."
            )
            .baseXpCost(15)
            .conflictsWith("sharpness", "smite", "bane_of_arthropods")
            .icon(Material.DIAMOND_AXE)
            .build());

        // Critical
        register(SkyblockEnchantment.builder("critical", "Critical")
            .type(EnchantmentType.COMMON)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(7)
            .description(
                "Increases critical hit chance by {level}%.",
                "Critical hits deal 50% more damage."
            )
            .baseXpCost(12)
            .icon(Material.DIAMOND_SWORD)
            .build());

        // First Strike
        register(SkyblockEnchantment.builder("first_strike", "First Strike")
            .type(EnchantmentType.RARE)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(5)
            .description(
                "Deal +{level}x damage on your first hit.",
                "Resets after 5 seconds of not attacking."
            )
            .baseXpCost(20)
            .conflictsWith("triple_strike")
            .icon(Material.IRON_SWORD)
            .build());

        // Triple Strike
        register(SkyblockEnchantment.builder("triple_strike", "Triple Strike")
            .type(EnchantmentType.EPIC)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(5)
            .description(
                "Hit {level} additional times with 30% damage each.",
                "Triggers on every attack."
            )
            .baseXpCost(25)
            .conflictsWith("first_strike")
            .icon(Material.DIAMOND_SWORD)
            .build());

        // Execute
        register(SkyblockEnchantment.builder("execute", "Execute")
            .type(EnchantmentType.RARE)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(6)
            .description(
                "Deal up to +{level}x damage based on enemy's missing HP.",
                "More effective against low health targets."
            )
            .baseXpCost(18)
            .conflictsWith("prosecute")
            .icon(Material.GOLDEN_AXE)
            .build());

        // Prosecute
        register(SkyblockEnchantment.builder("prosecute", "Prosecute")
            .type(EnchantmentType.EPIC)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(6)
            .description(
                "Deal up to +{level}x damage based on enemy's current HP.",
                "More effective against high health targets."
            )
            .baseXpCost(20)
            .conflictsWith("execute")
            .icon(Material.DIAMOND_AXE)
            .build());

        // Giant Killer
        register(SkyblockEnchantment.builder("giant_killer", "Giant Killer")
            .type(EnchantmentType.RARE)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(7)
            .description(
                "Deal +{level}% more damage to enemies with more HP than you.",
                "Scales with the HP difference."
            )
            .baseXpCost(15)
            .conflictsWith("titan_killer")
            .icon(Material.IRON_SWORD)
            .build());

        // Titan Killer
        register(SkyblockEnchantment.builder("titan_killer", "Titan Killer")
            .type(EnchantmentType.EPIC)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(7)
            .description(
                "Deal +{level}% more damage based on your defense.",
                "Higher defense = more damage."
            )
            .baseXpCost(20)
            .conflictsWith("giant_killer")
            .icon(Material.DIAMOND_CHESTPLATE)
            .build());

        // Lifesteal
        register(SkyblockEnchantment.builder("lifesteal", "Lifesteal")
            .type(EnchantmentType.RARE)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(5)
            .description(
                "Heal {level} HP per hit.",
                "Does not work on bosses."
            )
            .baseXpCost(25)
            .conflictsWith("drain", "mana_steal")
            .icon(Material.GOLDEN_APPLE)
            .build());

        // Drain
        register(SkyblockEnchantment.builder("drain", "Drain")
            .type(EnchantmentType.EPIC)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(5)
            .description(
                "Heal {level}% of damage dealt as HP.",
                "More effective than Lifesteal but harder to obtain."
            )
            .baseXpCost(30)
            .conflictsWith("lifesteal", "mana_steal")
            .icon(Material.ENCHANTED_GOLDEN_APPLE)
            .build());

        // Mana Steal
        register(SkyblockEnchantment.builder("mana_steal", "Mana Steal")
            .type(EnchantmentType.RARE)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(4)
            .description(
                "Restore {level} mana per hit.",
                "Does not work on bosses."
            )
            .baseXpCost(20)
            .conflictsWith("lifesteal", "drain")
            .icon(Material.BLAZE_POWDER)
            .build());

        // Thunderbolt
        register(SkyblockEnchantment.builder("thunderbolt", "Thunderbolt")
            .type(EnchantmentType.RARE)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(6)
            .description(
                "Strike lightning every {level} hits.",
                "Deals {level}x your strength as damage."
            )
            .baseXpCost(22)
            .conflictsWith("thunderlord")
            .icon(Material.BLAZE_ROD)
            .build());

        // Thunderlord
        register(SkyblockEnchantment.builder("thunderlord", "Thunderlord")
            .type(EnchantmentType.EPIC)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(6)
            .description(
                "Strike lightning every {level} hits.",
                "Deals more damage than Thunderbolt."
            )
            .baseXpCost(28)
            .conflictsWith("thunderbolt")
            .icon(Material.LIGHTNING_ROD)
            .build());

        // Venomous
        register(SkyblockEnchantment.builder("venomous", "Venomous")
            .type(EnchantmentType.RARE)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(6)
            .description(
                "Poison enemies for {level} seconds.",
                "Deals damage over time."
            )
            .baseXpCost(18)
            .icon(Material.SPIDER_EYE)
            .build());

        // Fire Aspect
        register(SkyblockEnchantment.builder("fire_aspect", "Fire Aspect")
            .type(EnchantmentType.COMMON)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(3)
            .vanillaEnchantment(Enchantment.FIRE_ASPECT)
            .description(
                "Set enemies on fire for {level} seconds.",
                "Deals 4 damage per second."
            )
            .baseXpCost(10)
            .icon(Material.BLAZE_POWDER)
            .build());

        // Looting
        register(SkyblockEnchantment.builder("looting", "Looting")
            .type(EnchantmentType.UNCOMMON)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(5)
            .vanillaEnchantment(Enchantment.LOOTING)
            .description(
                "Increases mob drops by {level}%.",
                "Does not work on bosses."
            )
            .baseXpCost(15)
            .icon(Material.EMERALD)
            .build());

        // Scavenger
        register(SkyblockEnchantment.builder("scavenger", "Scavenger")
            .type(EnchantmentType.RARE)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(5)
            .description(
                "Gain {level} coins per kill.",
                "Killing mobs rewards extra coins."
            )
            .baseXpCost(20)
            .icon(Material.GOLD_INGOT)
            .build());

        // Vampirism
        register(SkyblockEnchantment.builder("vampirism", "Vampirism")
            .type(EnchantmentType.LEGENDARY)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(6)
            .description(
                "Heal {level}% of damage dealt as HP.",
                "Obtained from Spooky Festival."
            )
            .baseXpCost(35)
            .conflictsWith("lifesteal", "drain")
            .icon(Material.ENCHANTED_GOLDEN_APPLE)
            .build());

        // ==================== BOW ENCHANTS ====================

        // Power
        register(SkyblockEnchantment.builder("power", "Power")
            .type(EnchantmentType.COMMON)
            .category(EnchantmentCategory.BOW)
            .maxLevel(7)
            .vanillaEnchantment(Enchantment.POWER)
            .description(
                "Increases arrow damage by {level}.",
                "Each level adds +25% damage."
            )
            .baseXpCost(10)
            .conflictsWith("snipe")
            .icon(Material.BOW)
            .build());

        // Flame
        register(SkyblockEnchantment.builder("flame", "Flame")
            .type(EnchantmentType.UNCOMMON)
            .category(EnchantmentCategory.BOW)
            .maxLevel(2)
            .vanillaEnchantment(Enchantment.FLAME)
            .description(
                "Arrows set enemies on fire for {level} seconds.",
                "Deals 4 damage per second."
            )
            .baseXpCost(12)
            .icon(Material.BLAZE_POWDER)
            .build());

        // Punch
        register(SkyblockEnchantment.builder("punch", "Punch")
            .type(EnchantmentType.COMMON)
            .category(EnchantmentCategory.BOW)
            .maxLevel(3)
            .vanillaEnchantment(Enchantment.PUNCH)
            .description(
                "Knocks back enemies {level} blocks.",
                "Useful for keeping enemies at distance."
            )
            .baseXpCost(8)
            .icon(Material.ARROW)
            .build());

        // Infinity
        register(SkyblockEnchantment.builder("infinity", "Infinity")
            .type(EnchantmentType.RARE)
            .category(EnchantmentCategory.BOW)
            .maxLevel(2)
            .vanillaEnchantment(Enchantment.INFINITY)
            .description(
                "Arrows have {level}% chance to not be consumed.",
                "At level 2, arrows are never consumed."
            )
            .baseXpCost(20)
            .icon(Material.ARROW)
            .build());

        // Snipe
        register(SkyblockEnchantment.builder("snipe", "Snipe")
            .type(EnchantmentType.RARE)
            .category(EnchantmentCategory.BOW)
            .maxLevel(4)
            .description(
                "Deal +{level}x damage when shooting from 30+ blocks away.",
                "Damage scales with distance."
            )
            .baseXpCost(22)
            .conflictsWith("power")
            .icon(Material.CROSSBOW)
            .build());

        // Infinite Quiver
        register(SkyblockEnchantment.builder("infinite_quiver", "Infinite Quiver")
            .type(EnchantmentType.MYTHIC)
            .category(EnchantmentCategory.BOW)
            .maxLevel(10)
            .description(
                "Arrows have {level}% chance to not be consumed.",
                "Dungeon-only enchantment."
            )
            .baseXpCost(40)
            .isDungeon(true)
            .icon(Material.TIPPED_ARROW)
            .build());

        // ==================== ARMOR ENCHANTS ====================

        // Protection
        register(SkyblockEnchantment.builder("protection", "Protection")
            .type(EnchantmentType.COMMON)
            .category(EnchantmentCategory.ARMOR)
            .maxLevel(7)
            .vanillaEnchantment(Enchantment.PROTECTION)
            .description(
                "Reduces all damage by {level}%.",
                "Does not stack with other protection types."
            )
            .baseXpCost(10)
            .conflictsWith("blast_protection", "fire_protection", "projectile_protection")
            .icon(Material.DIAMOND_CHESTPLATE)
            .build());

        // Blast Protection
        register(SkyblockEnchantment.builder("blast_protection", "Blast Protection")
            .type(EnchantmentType.UNCOMMON)
            .category(EnchantmentCategory.ARMOR)
            .maxLevel(5)
            .vanillaEnchantment(Enchantment.BLAST_PROTECTION)
            .description(
                "Reduces explosion damage by {level}%.",
                "Also reduces knockback from explosions."
            )
            .baseXpCost(8)
            .conflictsWith("protection", "fire_protection", "projectile_protection")
            .icon(Material.TNT)
            .build());

        // Fire Protection
        register(SkyblockEnchantment.builder("fire_protection", "Fire Protection")
            .type(EnchantmentType.UNCOMMON)
            .category(EnchantmentCategory.ARMOR)
            .maxLevel(5)
            .vanillaEnchantment(Enchantment.FIRE_PROTECTION)
            .description(
                "Reduces fire damage by {level}%.",
                "Also reduces burn time."
            )
            .baseXpCost(8)
            .conflictsWith("protection", "blast_protection", "projectile_protection")
            .icon(Material.BLAZE_ROD)
            .build());

        // Projectile Protection
        register(SkyblockEnchantment.builder("projectile_protection", "Projectile Protection")
            .type(EnchantmentType.UNCOMMON)
            .category(EnchantmentCategory.ARMOR)
            .maxLevel(5)
            .vanillaEnchantment(Enchantment.PROJECTILE_PROTECTION)
            .description(
                "Reduces projectile damage by {level}%.",
                "Effective against arrows, snowballs, and eggs."
            )
            .baseXpCost(8)
            .conflictsWith("protection", "blast_protection", "fire_protection")
            .icon(Material.ARROW)
            .build());

        // Growth
        register(SkyblockEnchantment.builder("growth", "Growth")
            .type(EnchantmentType.COMMON)
            .category(EnchantmentCategory.ARMOR)
            .maxLevel(7)
            .description(
                "Increases max HP by {level} per piece.",
                "Stacks across all armor pieces."
            )
            .baseXpCost(12)
            .icon(Material.GOLDEN_APPLE)
            .build());

        // Thorns
        register(SkyblockEnchantment.builder("thorns", "Thorns")
            .type(EnchantmentType.RARE)
            .category(EnchantmentCategory.CHESTPLATE)
            .maxLevel(5)
            .vanillaEnchantment(Enchantment.THORNS)
            .description(
                "Reflect {level}% of melee damage back to attacker.",
                "Costs durability when triggered."
            )
            .baseXpCost(15)
            .conflictsWith("reflection")
            .icon(Material.CACTUS)
            .build());

        // Reflection
        register(SkyblockEnchantment.builder("reflection", "Reflection")
            .type(EnchantmentType.EPIC)
            .category(EnchantmentCategory.CHESTPLATE)
            .maxLevel(5)
            .description(
                "Reflect {level}% of melee damage back to attacker.",
                "Better than Thorns but rarer."
            )
            .baseXpCost(25)
            .conflictsWith("thorns")
            .icon(Material.SHIELD)
            .build());

        // Rejuvenate
        register(SkyblockEnchantment.builder("rejuvenate", "Rejuvenate")
            .type(EnchantmentType.RARE)
            .category(EnchantmentCategory.ARMOR)
            .maxLevel(5)
            .description(
                "Regenerate {level} HP per second.",
                "Dungeon-only enchantment."
            )
            .baseXpCost(20)
            .isDungeon(true)
            .conflictsWith("respite")
            .icon(Material.GOLDEN_CARROT)
            .build());

        // Respite
        register(SkyblockEnchantment.builder("respite", "Respite")
            .type(EnchantmentType.UNCOMMON)
            .category(EnchantmentCategory.ARMOR)
            .maxLevel(5)
            .description(
                "Heal {level} HP when not in combat for 5 seconds.",
                "Obtained from experiments."
            )
            .baseXpCost(15)
            .conflictsWith("rejuvenate")
            .icon(Material.GOLDEN_APPLE)
            .build());

        // Feather Falling
        register(SkyblockEnchantment.builder("feather_falling", "Feather Falling")
            .type(EnchantmentType.COMMON)
            .category(EnchantmentCategory.BOOTS)
            .maxLevel(10)
            .vanillaEnchantment(Enchantment.FEATHER_FALLING)
            .description(
                "Reduces fall damage by {level}%.",
                "Dungeon version goes up to level 10."
            )
            .baseXpCost(8)
            .icon(Material.FEATHER)
            .build());

        // Depth Strider
        register(SkyblockEnchantment.builder("depth_strider", "Depth Strider")
            .type(EnchantmentType.UNCOMMON)
            .category(EnchantmentCategory.BOOTS)
            .maxLevel(4)
            .vanillaEnchantment(Enchantment.DEPTH_STRIDER)
            .description(
                "Increases underwater movement speed by {level}%.",
                "Each level reduces water slowdown."
            )
            .baseXpCost(12)
            .icon(Material.WATER_BUCKET)
            .build());

        // Counter Strike
        register(SkyblockEnchantment.builder("counter_strike", "Counter Strike")
            .type(EnchantmentType.RARE)
            .category(EnchantmentCategory.ARMOR)
            .maxLevel(5)
            .description(
                "Reflect {level} damage when hit.",
                "Obtained from Dark Auction."
            )
            .baseXpCost(20)
            .icon(Material.SHIELD)
            .build());

        // Big Brain
        register(SkyblockEnchantment.builder("big_brain", "Big Brain")
            .type(EnchantmentType.EPIC)
            .category(EnchantmentCategory.HELMET)
            .maxLevel(5)
            .description(
                "Increases max mana by {level} per level.",
                "Obtained from Dark Auction."
            )
            .baseXpCost(25)
            .conflictsWith("small_brain")
            .icon(Material.BOOK)
            .build());

        // Small Brain
        register(SkyblockEnchantment.builder("small_brain", "Small Brain")
            .type(EnchantmentType.UNCOMMON)
            .category(EnchantmentCategory.HELMET)
            .maxLevel(3)
            .description(
                "Increases max mana by {level} per level.",
                "Weaker version of Big Brain."
            )
            .baseXpCost(10)
            .conflictsWith("big_brain")
            .icon(Material.BOOK)
            .build());

        // ==================== TOOL ENCHANTS ====================

        // Efficiency
        register(SkyblockEnchantment.builder("efficiency", "Efficiency")
            .type(EnchantmentType.COMMON)
            .category(EnchantmentCategory.TOOL)
            .maxLevel(7)
            .vanillaEnchantment(Enchantment.EFFICIENCY)
            .description(
                "Increases mining speed by {level}%.",
                "Higher levels mine faster."
            )
            .baseXpCost(10)
            .icon(Material.DIAMOND_PICKAXE)
            .build());

        // Fortune
        register(SkyblockEnchantment.builder("fortune", "Fortune")
            .type(EnchantmentType.RARE)
            .category(EnchantmentCategory.TOOL)
            .maxLevel(5)
            .vanillaEnchantment(Enchantment.FORTUNE)
            .description(
                "Increases block drops by up to {level}x.",
                "Does not stack with Silk Touch."
            )
            .baseXpCost(20)
            .conflictsWith("silk_touch", "smelting_touch")
            .icon(Material.EMERALD)
            .build());

        // Silk Touch
        register(SkyblockEnchantment.builder("silk_touch", "Silk Touch")
            .type(EnchantmentType.RARE)
            .category(EnchantmentCategory.TOOL)
            .maxLevel(1)
            .vanillaEnchantment(Enchantment.SILK_TOUCH)
            .description(
                "Mine blocks as themselves.",
                "Cannot be combined with Fortune."
            )
            .baseXpCost(25)
            .conflictsWith("fortune", "smelting_touch")
            .icon(Material.GRASS_BLOCK)
            .build());

        // Smelting Touch
        register(SkyblockEnchantment.builder("smelting_touch", "Smelting Touch")
            .type(EnchantmentType.EPIC)
            .category(EnchantmentCategory.TOOL)
            .maxLevel(1)
            .description(
                "Automatically smelts mined blocks.",
                "Ores drop as ingots."
            )
            .baseXpCost(35)
            .conflictsWith("fortune", "silk_touch")
            .icon(Material.FURNACE)
            .build());

        // Unbreaking
        register(SkyblockEnchantment.builder("unbreaking", "Unbreaking")
            .type(EnchantmentType.COMMON)
            .category(EnchantmentCategory.UNIVERSAL)
            .maxLevel(5)
            .vanillaEnchantment(Enchantment.UNBREAKING)
            .description(
                "Item has {level}% chance to not lose durability.",
                "Extends item lifespan."
            )
            .baseXpCost(10)
            .icon(Material.ANVIL)
            .build());

        // Mending
        register(SkyblockEnchantment.builder("mending", "Mending")
            .type(EnchantmentType.LEGENDARY)
            .category(EnchantmentCategory.UNIVERSAL)
            .maxLevel(2)
            .vanillaEnchantment(Enchantment.MENDING)
            .description(
                "Repairs item using XP orbs.",
                "1 XP = 2 durability."
            )
            .baseXpCost(50)
            .icon(Material.EXPERIENCE_BOTTLE)
            .build());

        // Luck of the Sea
        register(SkyblockEnchantment.builder("luck_of_the_sea", "Luck of the Sea")
            .type(EnchantmentType.UNCOMMON)
            .category(EnchantmentCategory.FISHING_ROD)
            .maxLevel(5)
            .vanillaEnchantment(Enchantment.LUCK_OF_THE_SEA)
            .description(
                "Increases chance of treasure by {level}%.",
                "Reduces junk catches."
            )
            .baseXpCost(12)
            .icon(Material.FISHING_ROD)
            .build());

        // Lure
        register(SkyblockEnchantment.builder("lure", "Lure")
            .type(EnchantmentType.COMMON)
            .category(EnchantmentCategory.FISHING_ROD)
            .maxLevel(4)
            .vanillaEnchantment(Enchantment.LURE)
            .description(
                "Reduces wait time by {level} seconds.",
                "Fish bite faster."
            )
            .baseXpCost(10)
            .icon(Material.TROPICAL_FISH)
            .build());

        // Magnet
        register(SkyblockEnchantment.builder("magnet", "Magnet")
            .type(EnchantmentType.RARE)
            .category(EnchantmentCategory.FISHING_ROD)
            .maxLevel(5)
            .description(
                "Attracts items within {level} blocks.",
                "Picks up nearby drops automatically."
            )
            .baseXpCost(18)
            .icon(Material.COMPASS)
            .build());

        // ==================== FARMING ENCHANTS ====================

        // Cultivating
        register(SkyblockEnchantment.builder("cultivating", "Cultivating")
            .type(EnchantmentType.COMMON)
            .category(EnchantmentCategory.HOE)
            .maxLevel(10)
            .description(
                "Levels up by breaking crops.",
                "Grants farming fortune as it levels."
            )
            .baseXpCost(8)
            .icon(Material.WHEAT)
            .build());

        // Replenish
        register(SkyblockEnchantment.builder("replenish", "Replenish")
            .type(EnchantmentType.RARE)
            .category(EnchantmentCategory.HOE)
            .maxLevel(1)
            .description(
                "Automatically replants crops.",
                "Uses seeds from inventory."
            )
            .baseXpCost(30)
            .icon(Material.WHEAT_SEEDS)
            .build());

        // Turbo Crop
        register(SkyblockEnchantment.builder("turbo_crop", "Turbo-Crop")
            .type(EnchantmentType.UNCOMMON)
            .category(EnchantmentCategory.HOE)
            .maxLevel(5)
            .description(
                "Increases farming speed by {level}%.",
                "Obtained from Cletus's contests."
            )
            .baseXpCost(15)
            .icon(Material.GOLDEN_HOE)
            .build());

        // Compact
        register(SkyblockEnchantment.builder("compact", "Compact")
            .type(EnchantmentType.COMMON)
            .category(EnchantmentCategory.PICKAXE)
            .maxLevel(10)
            .description(
                "Automatically compresses mined blocks.",
                "Levels up by mining."
            )
            .baseXpCost(10)
            .icon(Material.COBBLESTONE)
            .build());

        // ==================== ULTIMATE ENCHANTS ====================

        // Ultimate Wise
        register(SkyblockEnchantment.builder("ultimate_wise", "Ultimate Wise")
            .type(EnchantmentType.LEGENDARY)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(5)
            .description(
                "Reduces mana cost of abilities by {level}%.",
                "Ultimate enchantment."
            )
            .baseXpCost(50)
            .isUltimate(true)
            .icon(Material.BOOK)
            .build());

        // Soul Eater
        register(SkyblockEnchantment.builder("soul_eater", "Soul Eater")
            .type(EnchantmentType.LEGENDARY)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(5)
            .description(
                "Deal +{level}% damage per nearby mob.",
                "Ultimate enchantment."
            )
            .baseXpCost(55)
            .isUltimate(true)
            .icon(Material.SOUL_LANTERN)
            .build());

        // One For All
        register(SkyblockEnchantment.builder("one_for_all", "One For All")
            .type(EnchantmentType.LEGENDARY)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(1)
            .description(
                "Deal +500% damage, but only hit one enemy.",
                "Ultimate enchantment."
            )
            .baseXpCost(100)
            .isUltimate(true)
            .conflictsWith("cleave", "soul_eater")
            .icon(Material.DIAMOND_SWORD)
            .build());

        // Fatal Tempo
        register(SkyblockEnchantment.builder("fatal_tempo", "Fatal Tempo")
            .type(EnchantmentType.LEGENDARY)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(5)
            .description(
                "Gain {level}% attack speed per hit (stacks up to 10 times).",
                "Ultimate enchantment."
            )
            .baseXpCost(60)
            .isUltimate(true)
            .icon(Material.CLOCK)
            .build());

        // Inferno
        register(SkyblockEnchantment.builder("inferno", "Inferno")
            .type(EnchantmentType.LEGENDARY)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(5)
            .description(
                "Ignite all nearby enemies every {level} seconds.",
                "Ultimate enchantment."
            )
            .baseXpCost(55)
            .isUltimate(true)
            .icon(Material.BLAZE_POWDER)
            .build());

        // Rend
        register(SkyblockEnchantment.builder("rend", "Rend")
            .type(EnchantmentType.LEGENDARY)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(5)
            .description(
                "Deal +{level}% damage to enemies above 50% HP.",
                "Ultimate enchantment."
            )
            .baseXpCost(50)
            .isUltimate(true)
            .icon(Material.DIAMOND_AXE)
            .build());

        // Swarm
        register(SkyblockEnchantment.builder("swarm", "Swarm")
            .type(EnchantmentType.LEGENDARY)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(5)
            .description(
                "Spawn {level} silverfish per hit.",
                "Ultimate enchantment."
            )
            .baseXpCost(45)
            .isUltimate(true)
            .icon(Material.SPAWNER)
            .build());

        // Duplex
        register(SkyblockEnchantment.builder("duplex", "Duplex")
            .type(EnchantmentType.LEGENDARY)
            .category(EnchantmentCategory.BOW)
            .maxLevel(5)
            .description(
                "Fire {level} additional arrows per shot.",
                "Ultimate enchantment."
            )
            .baseXpCost(55)
            .isUltimate(true)
            .icon(Material.CROSSBOW)
            .build());

        // Combo
        register(SkyblockEnchantment.builder("combo", "Combo")
            .type(EnchantmentType.LEGENDARY)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(5)
            .description(
                "Gain +{level}% damage per consecutive hit (resets after 3s).",
                "Ultimate enchantment."
            )
            .baseXpCost(50)
            .isUltimate(true)
            .icon(Material.IRON_SWORD)
            .build());

        // Chimera
        register(SkyblockEnchantment.builder("chimera", "Chimera")
            .type(EnchantmentType.LEGENDARY)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(5)
            .description(
                "Combine effects of {level} other ultimate enchantments.",
                "Ultimate enchantment."
            )
            .baseXpCost(75)
            .isUltimate(true)
            .icon(Material.DRAGON_HEAD)
            .build());

        // Last Stand
        register(SkyblockEnchantment.builder("last_stand", "Last Stand")
            .type(EnchantmentType.LEGENDARY)
            .category(EnchantmentCategory.ARMOR)
            .maxLevel(5)
            .description(
                "Prevent death once every {level} minutes.",
                "Ultimate enchantment."
            )
            .baseXpCost(80)
            .isUltimate(true)
            .icon(Material.TOTEM_OF_UNDYING)
            .build());

        // Legion
        register(SkyblockEnchantment.builder("legion", "Legion")
            .type(EnchantmentType.LEGENDARY)
            .category(EnchantmentCategory.ARMOR)
            .maxLevel(5)
            .description(
                "Gain +{level}% defense per nearby enemy.",
                "Ultimate enchantment."
            )
            .baseXpCost(45)
            .isUltimate(true)
            .icon(Material.SHIELD)
            .build());

        // Refrigerate
        register(SkyblockEnchantment.builder("refrigerate", "Refrigerate")
            .type(EnchantmentType.LEGENDARY)
            .category(EnchantmentCategory.ARMOR)
            .maxLevel(5)
            .description(
                "Convert {level}% of mana used into defense for 10s.",
                "Ultimate enchantment."
            )
            .baseXpCost(50)
            .isUltimate(true)
            .icon(Material.PACKED_ICE)
            .build());

        // Mana Vampire
        register(SkyblockEnchantment.builder("mana_vampire", "Mana Vampire")
            .type(EnchantmentType.LEGENDARY)
            .category(EnchantmentCategory.ARMOR)
            .maxLevel(10)
            .description(
                "Restore {level} mana per second.",
                "Ultimate enchantment."
            )
            .baseXpCost(40)
            .isUltimate(true)
            .conflictsWith("hardened_mana", "strong_mana", "ferocious_mana")
            .icon(Material.BLAZE_POWDER)
            .build());

        // Hardened Mana
        register(SkyblockEnchantment.builder("hardened_mana", "Hardened Mana")
            .type(EnchantmentType.EPIC)
            .category(EnchantmentCategory.ARMOR)
            .maxLevel(10)
            .description(
                "Increases max mana by {level} and defense by {level}.",
                "Ultimate enchantment."
            )
            .baseXpCost(35)
            .isUltimate(true)
            .conflictsWith("mana_vampire", "strong_mana", "ferocious_mana")
            .icon(Material.DIAMOND)
            .build());

        // Strong Mana
        register(SkyblockEnchantment.builder("strong_mana", "Strong Mana")
            .type(EnchantmentType.EPIC)
            .category(EnchantmentCategory.ARMOR)
            .maxLevel(10)
            .description(
                "Increases max mana by {level}.",
                "Ultimate enchantment."
            )
            .baseXpCost(30)
            .isUltimate(true)
            .conflictsWith("mana_vampire", "hardened_mana", "ferocious_mana")
            .icon(Material.BOOK)
            .build());

        // Ferocious Mana
        register(SkyblockEnchantment.builder("ferocious_mana", "Ferocious Mana")
            .type(EnchantmentType.EPIC)
            .category(EnchantmentCategory.ARMOR)
            .maxLevel(10)
            .description(
                "Increases max mana by {level} and strength by {level}.",
                "Ultimate enchantment."
            )
            .baseXpCost(40)
            .isUltimate(true)
            .conflictsWith("mana_vampire", "hardened_mana", "strong_mana")
            .icon(Material.BLAZE_ROD)
            .build());

        // ==================== DUNGEON ENCHANTS ====================

        // Lethality
        register(SkyblockEnchantment.builder("lethality", "Lethality")
            .type(EnchantmentType.LEGENDARY)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(6)
            .description(
                "Deal +{level}% damage to dungeon bosses.",
                "Dungeon-only enchantment."
            )
            .baseXpCost(40)
            .isDungeon(true)
            .icon(Material.NETHER_STAR)
            .build());

        // Overload
        register(SkyblockEnchantment.builder("overload", "Overload")
            .type(EnchantmentType.EPIC)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(5)
            .description(
                "Deal +{level}% damage but take {level}% more damage.",
                "Dungeon-only enchantment."
            )
            .baseXpCost(35)
            .isDungeon(true)
            .icon(Material.REDSTONE)
            .build());

        // Hecatomb
        register(SkyblockEnchantment.builder("hecatomb", "Hecatomb")
            .type(EnchantmentType.LEGENDARY)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(10)
            .description(
                "Deal +{level}% damage per 100 mobs killed (permanent).",
                "Upgrades by doing S runs in dungeons."
            )
            .baseXpCost(50)
            .isDungeon(true)
            .icon(Material.WITHER_SKELETON_SKULL)
            .build());

        // ==================== SPECIAL ENCHANTS ====================

        // Luck
        register(SkyblockEnchantment.builder("luck", "Luck")
            .type(EnchantmentType.RARE)
            .category(EnchantmentCategory.UNIVERSAL)
            .maxLevel(3)
            .description(
                "Increases lucky drops by {level}%.",
                "Rare enchantment."
            )
            .baseXpCost(30)
            .icon(Material.RABBIT_FOOT)
            .build());

        // Vicious
        register(SkyblockEnchantment.builder("vicious", "Vicious")
            .type(EnchantmentType.EPIC)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(3)
            .description(
                "Increases critical damage by {level}%.",
                "Obtained from Dark Auction."
            )
            .baseXpCost(35)
            .icon(Material.DIAMOND_SWORD)
            .build());

        // Tabasco
        register(SkyblockEnchantment.builder("tabasco", "Tabasco")
            .type(EnchantmentType.RARE)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(2)
            .description(
                "Increases fire aspect duration by {level} seconds.",
                "Synergizes with Fire Aspect."
            )
            .baseXpCost(20)
            .icon(Material.BLAZE_POWDER)
            .build());

        // Cubism
        register(SkyblockEnchantment.builder("cubism", "Cubism")
            .type(EnchantmentType.UNCOMMON)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(3)
            .description(
                "Deal +{level}x damage to slimes and magma cubes.",
                "Specialized anti-slime enchant."
            )
            .baseXpCost(15)
            .icon(Material.SLIME_BALL)
            .build());

        // Delicate
        register(SkyblockEnchantment.builder("delicate", "Delicate")
            .type(EnchantmentType.RARE)
            .category(EnchantmentCategory.PICKAXE)
            .maxLevel(1)
            .description(
                "Breaks ores without destroying them.",
                "Used for mining special ores."
            )
            .baseXpCost(25)
            .icon(Material.DIAMOND_PICKAXE)
            .build());

        // Aqua Affinity
        register(SkyblockEnchantment.builder("aqua_affinity", "Aqua Affinity")
            .type(EnchantmentType.UNCOMMON)
            .category(EnchantmentCategory.HELMET)
            .maxLevel(1)
            .vanillaEnchantment(Enchantment.AQUA_AFFINITY)
            .description(
                "Mine underwater at normal speed.",
                "Removes mining penalty in water."
            )
            .baseXpCost(10)
            .icon(Material.WATER_BUCKET)
            .build());

        // Respiration
        register(SkyblockEnchantment.builder("respiration", "Respiration")
            .type(EnchantmentType.UNCOMMON)
            .category(EnchantmentCategory.HELMET)
            .maxLevel(4)
            .vanillaEnchantment(Enchantment.RESPIRATION)
            .description(
                "Extends underwater breathing by {level} seconds.",
                "Reduces air consumption."
            )
            .baseXpCost(10)
            .icon(Material.COD)
            .build());

        // Curse of Vanishing
        register(SkyblockEnchantment.builder("vanishing", "Curse of Vanishing")
            .type(EnchantmentType.MYTHIC)
            .category(EnchantmentCategory.UNIVERSAL)
            .maxLevel(1)
            .vanillaEnchantment(Enchantment.VANISHING_CURSE)
            .description(
                "Item disappears on death.",
                "Curse enchantment."
            )
            .baseXpCost(5)
            .icon(Material.BARRIER)
            .build());

        // Knockback
        register(SkyblockEnchantment.builder("knockback", "Knockback")
            .type(EnchantmentType.COMMON)
            .category(EnchantmentCategory.WEAPON)
            .maxLevel(3)
            .vanillaEnchantment(Enchantment.KNOCKBACK)
            .description(
                "Knocks back enemies {level} blocks.",
                "Melee version of Punch."
            )
            .baseXpCost(8)
            .icon(Material.STICK)
            .build());

        // Dragon Tracker
        register(SkyblockEnchantment.builder("dragon_tracker", "Dragon Tracker")
            .type(EnchantmentType.DRAGON_TRACKER)
            .category(EnchantmentCategory.BOW)
            .maxLevel(1)
            .description(
                "Arrows track the Ender Dragon.",
                "Increases accuracy against dragons."
            )
            .baseXpCost(100)
            .icon(Material.COMPASS)
            .build());
    }

    private static void register(SkyblockEnchantment enchantment) {
        ENCHANTMENTS.put(enchantment.getId(), enchantment);
    }

    /**
     * Get an enchantment by its ID.
     */
    public static SkyblockEnchantment get(String id) {
        return ENCHANTMENTS.get(id);
    }

    /**
     * Get all registered enchantments.
     */
    public static Collection<SkyblockEnchantment> getAll() {
        return Collections.unmodifiableCollection(ENCHANTMENTS.values());
    }

    /**
     * Get all enchantments that can be applied to an item.
     */
    public static List<SkyblockEnchantment> getForItem(ItemStack item) {
        List<SkyblockEnchantment> result = new ArrayList<>();
        for (SkyblockEnchantment enchantment : ENCHANTMENTS.values()) {
            if (enchantment.canEnchantItem(item)) {
                result.add(enchantment);
            }
        }
        return result;
    }

    /**
     * Get all non-ultimate enchantments for an item.
     */
    public static List<SkyblockEnchantment> getNormalForItem(ItemStack item) {
        List<SkyblockEnchantment> result = new ArrayList<>();
        for (SkyblockEnchantment enchantment : getForItem(item)) {
            if (!enchantment.isUltimate()) {
                result.add(enchantment);
            }
        }
        return result;
    }

    /**
     * Get all ultimate enchantments.
     */
    public static List<SkyblockEnchantment> getUltimateEnchantments() {
        List<SkyblockEnchantment> result = new ArrayList<>();
        for (SkyblockEnchantment enchantment : ENCHANTMENTS.values()) {
            if (enchantment.isUltimate()) {
                result.add(enchantment);
            }
        }
        return result;
    }

    /**
     * Get all dungeon enchantments.
     */
    public static List<SkyblockEnchantment> getDungeonEnchantments() {
        List<SkyblockEnchantment> result = new ArrayList<>();
        for (SkyblockEnchantment enchantment : ENCHANTMENTS.values()) {
            if (enchantment.isDungeon()) {
                result.add(enchantment);
            }
        }
        return result;
    }

    /**
     * Check if an enchantment exists.
     */
    public static boolean hasEnchantment(String id) {
        return ENCHANTMENTS.containsKey(id);
    }

    /**
     * Get the total count of registered enchantments.
     */
    public static int count() {
        return ENCHANTMENTS.size();
    }
}

