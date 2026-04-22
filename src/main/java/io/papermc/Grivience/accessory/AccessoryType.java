package io.papermc.Grivience.accessory;

import io.papermc.Grivience.item.ItemRarity;
import org.bukkit.Material;

import java.util.Locale;

/**
 * Registry of supported accessories and their Skyblock stat bonuses.
 */
public enum AccessoryType {
    CRIMSON_CHARM(
            "Crimson Charm",
            Material.RED_DYE,
            ItemRarity.RARE,
            "crimson",
            1,
            Category.COMBAT,
            0.0D, 0.0D, 5.0D, 2.0D, 0.0D, 0.0D, 0.0D,
            "A warm charm that sharpens your edge in close combat."
    ),
    CRIMSON_RING(
            "Crimson Ring",
            Material.REDSTONE,
            ItemRarity.EPIC,
            "crimson",
            2,
            Category.COMBAT,
            12.0D, 0.0D, 10.0D, 4.0D, 0.0D, 0.0D, 0.0D,
            "An empowered evolution of the Crimson Charm."
    ),
    CRIMSON_ARTIFACT(
            "Crimson Artifact",
            Material.NETHER_STAR,
            ItemRarity.LEGENDARY,
            "crimson",
            3,
            Category.COMBAT,
            24.0D, 0.0D, 18.0D, 6.0D, 0.0D, 0.0D, 0.0D,
            "A blazing relic sought by duelists."
    ),
    HARVESTER_CHARM(
            "Harvester Charm",
            Material.WHEAT,
            ItemRarity.RARE,
            "harvester",
            1,
            Category.HARVEST,
            8.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 20.0D,
            "A field token that improves crop quality."
    ),
    HARVESTER_RING(
            "Harvester Ring",
            Material.HAY_BLOCK,
            ItemRarity.EPIC,
            "harvester",
            2,
            Category.HARVEST,
            18.0D, 4.0D, 0.0D, 0.0D, 0.0D, 10.0D, 40.0D,
            "A refined upgrade favored by dedicated farmers."
    ),
    HARVESTER_ARTIFACT(
            "Harvester Artifact",
            Material.GOLDEN_HOE,
            ItemRarity.LEGENDARY,
            "harvester",
            3,
            Category.HARVEST,
            32.0D, 10.0D, 0.0D, 0.0D, 0.0D, 18.0D, 70.0D,
            "Ancient harvest magic bound to a perfect relic."
    ),
    QUARRY_TALISMAN(
            "Quarry Talisman",
            Material.COPPER_INGOT,
            ItemRarity.RARE,
            "quarry",
            1,
            Category.HARVEST,
            0.0D, 12.0D, 4.0D, 0.0D, 0.0D, 8.0D, 0.0D,
            "A miner's charm that steadies your stance underground."
    ),
    QUARRY_RING(
            "Quarry Ring",
            Material.IRON_INGOT,
            ItemRarity.EPIC,
            "quarry",
            2,
            Category.HARVEST,
            14.0D, 24.0D, 8.0D, 0.0D, 0.0D, 16.0D, 0.0D,
            "Runed with ore veins to boost mining focus."
    ),
    QUARRY_ARTIFACT(
            "Quarry Artifact",
            Material.DIAMOND_PICKAXE,
            ItemRarity.LEGENDARY,
            "quarry",
            3,
            Category.HARVEST,
            24.0D, 38.0D, 14.0D, 0.0D, 0.0D, 26.0D, 0.0D,
            "A deep-core relic prized by master excavators."
    ),
    ORCHARD_TALISMAN(
            "Orchard Talisman",
            Material.APPLE,
            ItemRarity.RARE,
            "orchard",
            1,
            Category.HARVEST,
            10.0D, 0.0D, 0.0D, 1.0D, 0.0D, 0.0D, 16.0D,
            "Keeps orchard trees in perfect seasonal rhythm."
    ),
    ORCHARD_RING(
            "Orchard Ring",
            Material.GOLDEN_APPLE,
            ItemRarity.EPIC,
            "orchard",
            2,
            Category.HARVEST,
            20.0D, 4.0D, 0.0D, 2.0D, 0.0D, 8.0D, 34.0D,
            "A polished band worn by elite orchard keepers."
    ),
    ORCHARD_ARTIFACT(
            "Orchard Artifact",
            Material.ENCHANTED_GOLDEN_APPLE,
            ItemRarity.LEGENDARY,
            "orchard",
            3,
            Category.HARVEST,
            36.0D, 9.0D, 0.0D, 3.0D, 0.0D, 16.0D, 58.0D,
            "A miracle relic that makes every harvest abundant."
    ),
    APIARY_CHARM(
            "Apiary Charm",
            Material.HONEYCOMB,
            ItemRarity.RARE,
            "apiary",
            1,
            Category.HARVEST,
            6.0D, 0.0D, 0.0D, 2.0D, 0.0D, 6.0D, 12.0D,
            "Buzzing with sweet energy from enchanted hives."
    ),
    APIARY_RING(
            "Apiary Ring",
            Material.HONEY_BOTTLE,
            ItemRarity.EPIC,
            "apiary",
            2,
            Category.HARVEST,
            14.0D, 5.0D, 0.0D, 3.0D, 0.0D, 12.0D, 28.0D,
            "A hive-forged ring that empowers patient growers."
    ),
    ROOTBOUND_IDOL(
            "Rootbound Idol",
            Material.MANGROVE_ROOTS,
            ItemRarity.LEGENDARY,
            "rootbound",
            1,
            Category.HARVEST,
            42.0D, 16.0D, 0.0D, 0.0D, 0.0D, 14.0D, 50.0D,
            "Anchors your harvest with the strength of old forests."
    ),
    GEO_CORE_CHARM(
            "Geo-Core Charm",
            Material.COAL,
            ItemRarity.RARE,
            "geocore",
            1,
            Category.HARVEST,
            0.0D, 10.0D, 4.0D, 0.0D, 0.0D, 10.0D, 0.0D,
            "Resonates with shallow ore seams."
    ),
    GEO_CORE_RING(
            "Geo-Core Ring",
            Material.REDSTONE_BLOCK,
            ItemRarity.EPIC,
            "geocore",
            2,
            Category.HARVEST,
            8.0D, 22.0D, 8.0D, 0.0D, 0.0D, 18.0D, 0.0D,
            "Channels the pulse of deeper veins."
    ),
    GEO_CORE_ARTIFACT(
            "Geo-Core Artifact",
            Material.OBSIDIAN,
            ItemRarity.LEGENDARY,
            "geocore",
            3,
            Category.HARVEST,
            18.0D, 36.0D, 14.0D, 0.0D, 0.0D, 28.0D, 0.0D,
            "A condensed heart of the mountain itself."
    ),
    MOLTEN_DRILL_CHARM(
            "Molten Drill Charm",
            Material.BLAZE_POWDER,
            ItemRarity.RARE,
            "molten",
            1,
            Category.HARVEST,
            0.0D, 14.0D, 6.0D, 0.0D, 0.0D, 10.0D, 0.0D,
            "Keeps your pick hot and your momentum steady."
    ),
    MOLTEN_DRILL_RING(
            "Molten Drill Ring",
            Material.MAGMA_CREAM,
            ItemRarity.EPIC,
            "molten",
            2,
            Category.HARVEST,
            12.0D, 26.0D, 10.0D, 0.0D, 0.0D, 16.0D, 0.0D,
            "Forged in magma to survive brutal excavation."
    ),
    BEDROCK_SIGIL(
            "Bedrock Sigil",
            Material.ANCIENT_DEBRIS,
            ItemRarity.LEGENDARY,
            "bedrock",
            1,
            Category.HARVEST,
            34.0D, 46.0D, 12.0D, 0.0D, 0.0D, 22.0D, 0.0D,
            "A sigil carried by miners who never retreat."
    ),
    TIDEBINDER_TALISMAN(
            "Tidebinder Talisman",
            Material.PRISMARINE_CRYSTALS,
            ItemRarity.RARE,
            "tidebinder",
            1,
            Category.WISDOM,
            0.0D, 8.0D, 0.0D, 0.0D, 6.0D, 40.0D, 0.0D,
            "Coaxes mana from nearby currents."
    ),
    ASTRAL_COMPASS(
            "Astral Compass",
            Material.COMPASS,
            ItemRarity.EPIC,
            "astral",
            1,
            Category.WISDOM,
            0.0D, 6.0D, 4.0D, 0.0D, 8.0D, 65.0D, 0.0D,
            "Its needle points toward the strongest ley lines."
    ),
    BASTION_RELIC(
            "Bastion Relic",
            Material.SHIELD,
            ItemRarity.EPIC,
            "bastion",
            1,
            Category.COMBAT,
            18.0D, 30.0D, 0.0D, 0.0D, 10.0D, 0.0D, 0.0D,
            "Forged to endure relentless dungeon pressure."
    ),
    VERDANT_IDOL(
            "Verdant Idol",
            Material.MOSS_BLOCK,
            ItemRarity.LEGENDARY,
            "verdant",
            1,
            Category.HARVEST,
            30.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 45.0D,
            "Blesses every harvest with richer yield."
    ),
    SHOCKWIRE_TRINKET(
            "Shockwire Trinket",
            Material.LIGHTNING_ROD,
            ItemRarity.RARE,
            "shockwire",
            1,
            Category.COMBAT,
            0.0D, 0.0D, 0.0D, 6.0D, 10.0D, 0.0D, 0.0D,
            "Stores static energy between attacks."
    ),
    ECHO_GEM(
            "Echo Gem",
            Material.AMETHYST_SHARD,
            ItemRarity.EPIC,
            "echo",
            1,
            Category.WISDOM,
            10.0D, 0.0D, 6.0D, 0.0D, 0.0D, 30.0D, 0.0D,
            "Reverberates the power of nearby accessories."
    ),
    VOIDWING_RELIC(
            "Voidwing Relic",
            Material.DRAGON_BREATH,
            ItemRarity.LEGENDARY,
            "voidwing",
            1,
            Category.COMBAT,
            28.0D, 24.0D, 18.0D, 4.0D, 14.0D, 40.0D, 0.0D,
            "A relic feathered with End dragon power and void energy."
    );

    private final String displayName;
    private final Material material;
    private final ItemRarity rarity;
    private final String family;
    private final int tier;
    private final Category category;
    private final double health;
    private final double defense;
    private final double strength;
    private final double critChance;
    private final double critDamage;
    private final double intelligence;
    private final double farmingFortune;
    private final String flavor;

    AccessoryType(
            String displayName,
            Material material,
            ItemRarity rarity,
            String family,
            int tier,
            Category category,
            double health,
            double defense,
            double strength,
            double critChance,
            double critDamage,
            double intelligence,
            double farmingFortune,
            String flavor
    ) {
        this.displayName = displayName;
        this.material = material;
        this.rarity = rarity;
        this.family = family;
        this.tier = tier;
        this.category = category;
        this.health = health;
        this.defense = defense;
        this.strength = strength;
        this.critChance = critChance;
        this.critDamage = critDamage;
        this.intelligence = intelligence;
        this.farmingFortune = farmingFortune;
        this.flavor = flavor;
    }

    public String displayName() {
        return displayName;
    }

    public Material material() {
        return material;
    }

    public ItemRarity rarity() {
        return rarity;
    }

    public String family() {
        return family;
    }

    public int tier() {
        return tier;
    }

    public Category category() {
        return category;
    }

    public double health() {
        return health;
    }

    public double defense() {
        return defense;
    }

    public double strength() {
        return strength;
    }

    public double critChance() {
        return critChance;
    }

    public double critDamage() {
        return critDamage;
    }

    public double intelligence() {
        return intelligence;
    }

    public double farmingFortune() {
        return farmingFortune;
    }

    public String flavor() {
        return flavor;
    }

    public String id() {
        return name();
    }

    public static AccessoryType parse(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String normalized = input.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        for (AccessoryType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return null;
    }

    public enum Category {
        COMBAT("Combat"),
        WISDOM("Wisdom"),
        HARVEST("Harvest");

        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }
}
