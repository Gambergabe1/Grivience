package io.papermc.Grivience.enchantment;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Represents a Skyblock-style enchantment with all its properties.
 */
public class SkyblockEnchantment {
    private final String id;
    private final String name;
    private final EnchantmentType type;
    private final EnchantmentCategory category;
    private final int maxLevel;
    private final Set<Enchantment> vanillaEnchantment;
    private final List<String> description;
    private final int baseXpCost;
    private final Set<String> conflictsWith;
    private final int requiredEnchantingLevel;
    private final boolean isUltimate;
    private final boolean isDungeon;
    private final Material icon;

    public SkyblockEnchantment(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.type = builder.type;
        this.category = builder.category;
        this.maxLevel = builder.maxLevel;
        this.vanillaEnchantment = builder.vanillaEnchantment;
        this.description = builder.description;
        this.baseXpCost = builder.baseXpCost;
        this.conflictsWith = builder.conflictsWith;
        this.requiredEnchantingLevel = builder.requiredEnchantingLevel;
        this.isUltimate = builder.isUltimate;
        this.isDungeon = builder.isDungeon;
        this.icon = builder.icon;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public EnchantmentType getType() {
        return type;
    }

    public EnchantmentCategory getCategory() {
        return category;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public Set<Enchantment> getVanillaEnchantment() {
        return vanillaEnchantment;
    }

    public List<String> getDescription() {
        return description;
    }

    public int getBaseXpCost() {
        return baseXpCost;
    }

    public Set<String> getConflictsWith() {
        return conflictsWith;
    }

    public int getRequiredEnchantingLevel() {
        return requiredEnchantingLevel;
    }

    public boolean isUltimate() {
        return isUltimate;
    }

    public boolean isDungeon() {
        return isDungeon;
    }

    public Material getIcon() {
        return icon;
    }

    /**
     * Get the formatted display name for a specific level.
     */
    public String getDisplayName(int level) {
        String romanLevel = toRoman(level);
        return type.getColor() + name + " " + romanLevel;
    }

    /**
     * Get the formatted lore description.
     */
    public List<String> getFormattedLore(int level) {
        List<String> formatted = new ArrayList<>();
        for (String line : description) {
            formatted.add(ChatColor.GRAY + line.replace("{level}", String.valueOf(level)));
        }
        return formatted;
    }

    /**
     * Calculate XP cost for this enchantment at given level.
     */
    public int getXpCost(int level) {
        return baseXpCost * level;
    }

    /**
     * Check if this enchantment conflicts with another.
     */
    public boolean conflictsWith(SkyblockEnchantment other) {
        return conflictsWith.contains(other.getId()) || other.getConflictsWith().contains(getId());
    }

    /**
     * Check if this enchantment can be applied to an item.
     */
    public boolean canEnchantItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        Material type = item.getType();

        // Check category
        switch (category) {
            case SWORD:
                return type == Material.WOODEN_SWORD || type == Material.STONE_SWORD ||
                       type == Material.IRON_SWORD || type == Material.GOLDEN_SWORD ||
                       type == Material.DIAMOND_SWORD || type == Material.NETHERITE_SWORD;
            case BOW:
                return type == Material.BOW;
            case AXE:
                return type == Material.WOODEN_AXE || type == Material.STONE_AXE ||
                       type == Material.IRON_AXE || type == Material.GOLDEN_AXE ||
                       type == Material.DIAMOND_AXE || type == Material.NETHERITE_AXE;
            case PICKAXE:
                return type == Material.WOODEN_PICKAXE || type == Material.STONE_PICKAXE ||
                       type == Material.IRON_PICKAXE || type == Material.GOLDEN_PICKAXE ||
                       type == Material.DIAMOND_PICKAXE || type == Material.NETHERITE_PICKAXE;
            case DRILL:
                // Custom drill handling would go here
                return false;
            case HOE:
                return type == Material.WOODEN_HOE || type == Material.STONE_HOE ||
                       type == Material.IRON_HOE || type == Material.GOLDEN_HOE ||
                       type == Material.DIAMOND_HOE || type == Material.NETHERITE_HOE;
            case SHEARS:
                return type == Material.SHEARS;
            case SHOVEL:
                return type == Material.WOODEN_SHOVEL || type == Material.STONE_SHOVEL ||
                       type == Material.IRON_SHOVEL || type == Material.GOLDEN_SHOVEL ||
                       type == Material.DIAMOND_SHOVEL || type == Material.NETHERITE_SHOVEL;
            case FISHING_ROD:
                return type == Material.FISHING_ROD;
            case HELMET:
                return type == Material.LEATHER_HELMET || type == Material.CHAINMAIL_HELMET ||
                       type == Material.IRON_HELMET || type == Material.GOLDEN_HELMET ||
                       type == Material.DIAMOND_HELMET || type == Material.NETHERITE_HELMET ||
                       type == Material.TURTLE_HELMET;
            case CHESTPLATE:
                return type == Material.LEATHER_CHESTPLATE || type == Material.CHAINMAIL_CHESTPLATE ||
                       type == Material.IRON_CHESTPLATE || type == Material.GOLDEN_CHESTPLATE ||
                       type == Material.DIAMOND_CHESTPLATE || type == Material.NETHERITE_CHESTPLATE;
            case LEGGINGS:
                return type == Material.LEATHER_LEGGINGS || type == Material.CHAINMAIL_LEGGINGS ||
                       type == Material.IRON_LEGGINGS || type == Material.GOLDEN_LEGGINGS ||
                       type == Material.DIAMOND_LEGGINGS || type == Material.NETHERITE_LEGGINGS;
            case BOOTS:
                return type == Material.LEATHER_BOOTS || type == Material.CHAINMAIL_BOOTS ||
                       type == Material.IRON_BOOTS || type == Material.GOLDEN_BOOTS ||
                       type == Material.DIAMOND_BOOTS || type == Material.NETHERITE_BOOTS;
            case ARMOR:
                return isArmor(type);
            case TOOL:
                return isTool(type);
            case WEAPON:
                return isWeapon(type);
            case EQUIPMENT:
                return isEquipment(type);
            case UNIVERSAL:
            default:
                return true;
        }
    }

    private boolean isArmor(Material type) {
        return type.name().contains("HELMET") || type.name().contains("CHESTPLATE") ||
               type.name().contains("LEGGINGS") || type.name().contains("BOOTS");
    }

    private boolean isTool(Material type) {
        return type.name().contains("PICKAXE") || type.name().contains("AXE") ||
               type.name().contains("SHOVEL") || type.name().contains("HOE");
    }

    private boolean isWeapon(Material type) {
        // Hypixel SkyBlock weapons are melee (sword/axe). Bow enchants use the BOW category.
        return type.name().contains("SWORD") || type.name().contains("AXE");
    }

    private boolean isEquipment(Material type) {
        return type.name().contains("HELMET") || type.name().contains("CHESTPLATE") ||
               type.name().contains("LEGGINGS") || type.name().contains("BOOTS") ||
               type == Material.ELYTRA;
    }

    private String toRoman(int number) {
        if (number <= 0 || number > 10) {
            return String.valueOf(number);
        }
        String[] roman = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return roman[number];
    }

    public static Builder builder(String id, String name) {
        return new Builder(id, name);
    }

    public static class Builder {
        private final String id;
        private final String name;
        private EnchantmentType type = EnchantmentType.COMMON;
        private EnchantmentCategory category = EnchantmentCategory.UNIVERSAL;
        private int maxLevel = 5;
        private Set<Enchantment> vanillaEnchantment = new HashSet<>();
        private List<String> description = new ArrayList<>();
        private int baseXpCost = 10;
        private Set<String> conflictsWith = new HashSet<>();
        private int requiredEnchantingLevel = 0;
        private boolean isUltimate = false;
        private boolean isDungeon = false;
        private Material icon = Material.ENCHANTED_BOOK;

        public Builder(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public Builder type(EnchantmentType type) {
            this.type = type;
            return this;
        }

        public Builder category(EnchantmentCategory category) {
            this.category = category;
            return this;
        }

        public Builder maxLevel(int maxLevel) {
            this.maxLevel = maxLevel;
            return this;
        }

        public Builder vanillaEnchantment(Enchantment enchantment) {
            this.vanillaEnchantment.add(enchantment);
            return this;
        }

        public Builder description(String... description) {
            this.description = Arrays.asList(description);
            return this;
        }

        public Builder baseXpCost(int baseXpCost) {
            this.baseXpCost = baseXpCost;
            return this;
        }

        public Builder conflictsWith(String... conflicts) {
            this.conflictsWith.addAll(Arrays.asList(conflicts));
            return this;
        }

        public Builder requiredEnchantingLevel(int level) {
            this.requiredEnchantingLevel = level;
            return this;
        }

        public Builder isUltimate(boolean ultimate) {
            isUltimate = ultimate;
            return this;
        }

        public Builder isDungeon(boolean dungeon) {
            isDungeon = dungeon;
            return this;
        }

        public Builder icon(Material icon) {
            this.icon = icon;
            return this;
        }

        public SkyblockEnchantment build() {
            return new SkyblockEnchantment(this);
        }
    }
}

