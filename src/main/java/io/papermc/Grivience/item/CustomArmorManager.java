package io.papermc.Grivience.item;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.crafting.RecipeCategory;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Manages custom armor sets and pieces with stats.
 */
public final class CustomArmorManager {
    private final GriviencePlugin plugin;
    private final Map<String, CustomArmorSet> armorSets = new HashMap<>();
    private final NamespacedKey armorSetKey;
    private final NamespacedKey armorPieceKey;

    public CustomArmorManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.armorSetKey = new NamespacedKey(plugin, "custom-armor-set");
        this.armorPieceKey = new NamespacedKey(plugin, "custom-armor-piece");
        reloadFromConfig();
    }

    public void reloadFromConfig() {
        armorSets.clear();
        ConfigurationSection sets = plugin.getConfig().getConfigurationSection("custom-armor.sets");
        if (sets != null) {
            for (String key : sets.getKeys(false)) {
                loadArmorSet(key, sets.getConfigurationSection(key));
            }
        }
    }

    private void loadArmorSet(String id, ConfigurationSection section) {
        if (section == null) return;

        String displayName = ChatColor.translateAlternateColorCodes('&', section.getString("display-name", id));
        String description = ChatColor.translateAlternateColorCodes('&', section.getString("description", ""));
        ItemRarity rarity = resolveRarity(section.getString("rarity"), displayName);
        int piecesRequired = section.getInt("pieces-required", 4);
        List<String> bonuses = section.getStringList("bonuses");
        
        // Recipe metadata
        RecipeCategory category = RecipeCategory.COMBAT;
        String categoryStr = section.getString("category");
        if (categoryStr != null) {
            try {
                category = RecipeCategory.valueOf(categoryStr.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {}
        }
        
        String collectionId = section.getString("collection-id");
        int collectionTierRequired = section.getInt("collection-tier-required", 0);

        Map<ArmorPieceType, ArmorPieceConfig> pieces = new EnumMap<>(ArmorPieceType.class);
        ConfigurationSection piecesSection = section.getConfigurationSection("pieces");
        if (piecesSection != null) {
            for (String pieceKey : piecesSection.getKeys(false)) {
                try {
                    ArmorPieceType type = ArmorPieceType.valueOf(pieceKey.toUpperCase(Locale.ROOT));
                    ConfigurationSection p = piecesSection.getConfigurationSection(pieceKey);
                    if (p == null) continue;

                    Material material = Material.valueOf(p.getString("material", "LEATHER_CHESTPLATE").toUpperCase(Locale.ROOT));
                    String pieceName = ChatColor.translateAlternateColorCodes('&', p.getString("display-name", displayName));
                    List<String> lore = p.getStringList("lore");
                    int armorValue = p.getInt("armor", 0);
                    double toughness = p.getDouble("toughness", 0.0D);
                    boolean glowing = p.getBoolean("glowing", false);
                    double manaBonus = p.getDouble("mana", 0.0D);
                    double healthBonus = p.getDouble("health", 0.0D);
                    double critChanceBonus = p.getDouble("crit-chance", 0.0D);
                    double critDamageBonus = p.getDouble("crit-damage", 0.0D);
                    double farmingFortuneBonus = p.getDouble("farming-fortune", 0.0D);
                    int breakingPowerBonus = p.getInt("breaking-power", 0);
                    int miningSpeedBonus = p.getInt("mining-speed", 0);
                    String color = p.getString("color");

                    pieces.put(type, new ArmorPieceConfig(
                        material, pieceName, lore, armorValue, toughness, glowing,
                        manaBonus, healthBonus, critChanceBonus, critDamageBonus,
                        farmingFortuneBonus, breakingPowerBonus, miningSpeedBonus, color
                    ));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        CustomArmorSet set = new CustomArmorSet(id, displayName, description, rarity, pieces, bonuses, piecesRequired, category, collectionId, collectionTierRequired);
        armorSets.put(id.toLowerCase(Locale.ROOT), set);
    }

    public ItemStack createArmorPiece(String setId, ArmorPieceType type) {
        CustomArmorSet set = armorSets.get(setId.toLowerCase(Locale.ROOT));
        if (set == null) return null;

        ArmorPieceConfig config = set.getPiece(type);
        if (config == null) return null;

        ItemStack item = new ItemStack(config.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        applyArmorPresentation(meta, setId, set, type, config);
        item.setItemMeta(meta);
        return item;
    }

    private void applyArmorPresentation(ItemMeta meta, String setId, CustomArmorSet set, ArmorPieceType type, ArmorPieceConfig config) {
        Map<Enchantment, Integer> displayEnchants = visibleEnchants(meta.getEnchants(), config.isGlowing());
        List<String> lore = styleDisplayLore(buildDisplayLore(set, type, config, enchantLore(displayEnchants)));

        meta.setDisplayName(styleDisplayName(config.getDisplayName(), set.getRarity()));
        meta.setLore(lore);
        meta.setUnbreakable(true);
        meta.addItemFlags(
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_DYE,
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_UNBREAKABLE,
                ItemFlag.HIDE_ARMOR_TRIM
        );

        if (config.isGlowing() && meta.getEnchants().isEmpty()) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        }

        if (meta instanceof LeatherArmorMeta leatherMeta && config.getColor() != null) {
            try {
                leatherMeta.setColor(Color.fromRGB(Integer.parseInt(config.getColor().replace("#", ""), 16)));
            } catch (Exception ignored) {}
        }

        meta.getPersistentDataContainer().set(armorSetKey, PersistentDataType.STRING, setId);
        meta.getPersistentDataContainer().set(armorPieceKey, PersistentDataType.STRING, type.name());
    }

    static ItemRarity resolveRarity(String configuredRarity, String displayName) {
        if (configuredRarity != null && !configuredRarity.isBlank()) {
            try {
                return ItemRarity.valueOf(configuredRarity.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {}
        }

        String colors = ChatColor.getLastColors(displayName == null ? "" : displayName);
        if (colors.contains(ChatColor.LIGHT_PURPLE.toString())) return ItemRarity.MYTHIC;
        if (colors.contains(ChatColor.GOLD.toString())) return ItemRarity.LEGENDARY;
        if (colors.contains(ChatColor.DARK_PURPLE.toString())) return ItemRarity.EPIC;
        if (colors.contains(ChatColor.BLUE.toString())) return ItemRarity.RARE;
        if (colors.contains(ChatColor.GREEN.toString())) return ItemRarity.UNCOMMON;
        return ItemRarity.COMMON;
    }

    static List<String> buildDisplayLore(CustomArmorSet set, ArmorPieceType type, ArmorPieceConfig config, List<String> enchantLore) {
        List<String> lore = new ArrayList<>();
        appendStatLine(lore, "Health", config.getHealthBonus(), ChatColor.RED, false);
        appendStatLine(lore, "Defense", config.getArmorValue(), ChatColor.GREEN, false);
        appendStatLine(lore, "Armor Toughness", config.getToughness(), ChatColor.DARK_GREEN, false);
        appendStatLine(lore, "Intelligence", config.getManaBonus(), ChatColor.AQUA, false);
        appendStatLine(lore, "Crit Chance", config.getCritChanceBonus(), ChatColor.BLUE, true);
        appendStatLine(lore, "Crit Damage", config.getCritDamageBonus(), ChatColor.BLUE, true);
        appendStatLine(lore, "Farming Fortune", config.getFarmingFortuneBonus(), ChatColor.GOLD, false);
        appendStatLine(lore, "Mining Speed", config.getMiningSpeedBonus(), ChatColor.GOLD, false);
        appendStatLine(lore, "Breaking Power", config.getBreakingPowerBonus(), ChatColor.DARK_GREEN, false);

        if (!enchantLore.isEmpty()) {
            appendSpacer(lore);
            lore.addAll(enchantLore);
        }

        List<String> bonusLines = bonusLines(set);
        if (!bonusLines.isEmpty()) {
            appendSpacer(lore);
            lore.add(setBonusHeader(set));
            lore.addAll(bonusLines);
        }

        List<String> flavorLines = flavorLines(set, config);
        if (!flavorLines.isEmpty()) {
            appendSpacer(lore);
            lore.addAll(flavorLines);
        }

        appendSpacer(lore);
        lore.add(set.getRarity().color() + "" + ChatColor.BOLD + set.getRarity().name() + " " + type.name());
        return lore;
    }

    private String styleDisplayName(String displayName, ItemRarity rarity) {
        CustomItemService itemService = plugin.getCustomItemService();
        if (itemService != null) {
            return itemService.styleItemName(displayName, rarity, false);
        }
        String plain = ChatColor.stripColor(displayName);
        return (rarity == null ? ChatColor.WHITE : rarity.color()) + (plain == null ? displayName : plain);
    }

    private List<String> styleDisplayLore(List<String> lore) {
        CustomItemService itemService = plugin.getCustomItemService();
        if (itemService != null) {
            return itemService.styleItemLore(lore, false);
        }
        return lore;
    }

    private List<String> enchantLore(Map<Enchantment, Integer> enchants) {
        if (enchants.isEmpty()) {
            return List.of();
        }
        CustomItemService itemService = plugin.getCustomItemService();
        if (itemService != null) {
            return itemService.enchantLore(enchants);
        }
        return List.of();
    }

    private static Map<Enchantment, Integer> visibleEnchants(Map<Enchantment, Integer> enchants, boolean glowing) {
        if (!glowing || enchants.isEmpty()) {
            return enchants;
        }
        if (enchants.size() == 1 && enchants.getOrDefault(Enchantment.UNBREAKING, 0) == 1) {
            return Map.of();
        }
        return enchants;
    }

    private static void appendStatLine(List<String> lore, String label, double value, ChatColor valueColor, boolean percent) {
        if (Math.abs(value) < 0.0001D) {
            return;
        }
        String suffix = percent ? "%" : "";
        String sign = value > 0 ? "+" : "";
        lore.add(ChatColor.GRAY + label + ": " + valueColor + sign + formatValue(value) + suffix);
    }

    private static String formatValue(double value) {
        if (Math.rint(value) == value) {
            return Integer.toString((int) value);
        }
        return String.format(Locale.US, "%.1f", value);
    }

    private static void appendSpacer(List<String> lore) {
        if (!lore.isEmpty() && !lore.get(lore.size() - 1).isEmpty()) {
            lore.add("");
        }
    }

    private static List<String> bonusLines(CustomArmorSet set) {
        List<String> lines = new ArrayList<>();
        for (String bonus : set.getBonuses()) {
            String translated = translateLine(bonus);
            if (translated == null || translated.isBlank()) {
                continue;
            }
            lines.add(translated);
        }
        return lines;
    }

    private static List<String> flavorLines(CustomArmorSet set, ArmorPieceConfig config) {
        List<String> lines = new ArrayList<>();
        for (String rawLine : config.getLore()) {
            String translated = translateLine(rawLine);
            if (translated == null || translated.isBlank() || isStructuredLoreLine(translated)) {
                continue;
            }
            lines.add(ChatColor.GRAY + stripColors(translated));
        }
        if (lines.isEmpty() && set.getDescription() != null && !set.getDescription().isBlank()) {
            lines.add(ChatColor.GRAY + stripColors(set.getDescription()));
        }
        return lines;
    }

    private static String setBonusHeader(CustomArmorSet set) {
        String header = isFullSetBonus(set) ? "Full Set Bonus: " : "Set Bonus: ";
        return ChatColor.GOLD + header + ChatColor.WHITE + stripColors(set.getDisplayName());
    }

    private static boolean isFullSetBonus(CustomArmorSet set) {
        if (set.getPiecesRequired() < 4) {
            return false;
        }
        boolean sawPieceRequirement = false;
        String requiredPrefix = set.getPiecesRequired() + " pieces:";
        for (String rawBonus : set.getBonuses()) {
            String plain = stripColors(translateLine(rawBonus)).toLowerCase(Locale.ROOT);
            if (plain.matches("\\d+\\s+pieces?:.*")) {
                sawPieceRequirement = true;
                if (!plain.startsWith(requiredPrefix)) {
                    return false;
                }
            }
        }
        return sawPieceRequirement || set.getPiecesRequired() >= 4;
    }

    private static boolean isStructuredLoreLine(String line) {
        String plain = stripColors(line).toLowerCase(Locale.ROOT);
        if (plain.isBlank()) {
            return false;
        }
        if (plain.startsWith("full set bonus:") || plain.startsWith("set bonus:") || plain.startsWith("ability:")) {
            return true;
        }
        if (plain.startsWith("wear ") || plain.matches("\\d+\\s+pieces?:.*")) {
            return true;
        }

        int colonIndex = plain.indexOf(':');
        if (colonIndex <= 0) {
            return false;
        }

        String label = plain.substring(0, colonIndex).trim();
        return Set.of(
                "health",
                "defense",
                "armor",
                "armor toughness",
                "intelligence",
                "mana",
                "crit chance",
                "crit damage",
                "farming fortune",
                "mining speed",
                "breaking power",
                "strength",
                "damage",
                "speed",
                "attack speed",
                "ferocity",
                "heal speed",
                "ability damage"
        ).contains(label);
    }

    private static String translateLine(String line) {
        if (line == null) {
            return null;
        }
        return ChatColor.translateAlternateColorCodes('&', line);
    }

    private static String stripColors(String line) {
        String stripped = ChatColor.stripColor(line);
        return stripped == null ? "" : stripped.trim();
    }

    public boolean isCustomArmor(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(armorSetKey, PersistentDataType.STRING);
    }

    public String getArmorSetId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(armorSetKey, PersistentDataType.STRING);
    }

    public ArmorPieceType getArmorPieceType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String typeName = item.getItemMeta().getPersistentDataContainer().get(armorPieceKey, PersistentDataType.STRING);
        if (typeName == null) return null;
        try {
            return ArmorPieceType.valueOf(typeName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public CustomArmorSet getArmorSet(String id) {
        if (id == null) return null;
        return armorSets.get(id.toLowerCase(Locale.ROOT));
    }

    public int countEquippedPieces(Player player, String setId) {
        if (player == null || setId == null) return 0;
        int count = 0;
        String target = setId.toLowerCase(Locale.ROOT);
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            if (target.equalsIgnoreCase(getArmorSetId(piece))) {
                count++;
            }
        }
        return count;
    }

    public boolean hasEquippedPieces(Player player, String setId, int required) {
        return countEquippedPieces(player, setId) >= required;
    }

    public int totalMiningSpeedBonus(Player player) {
        int total = 0;
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            String setId = getArmorSetId(piece);
            ArmorPieceType type = getArmorPieceType(piece);
            if (setId != null && type != null) {
                CustomArmorSet set = getArmorSet(setId);
                if (set != null) {
                    ArmorPieceConfig p = set.getPiece(type);
                    if (p != null) total += p.getMiningSpeedBonus();
                }
            }
        }
        return total;
    }

    public int totalBreakingPowerBonus(Player player) {
        int total = 0;
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            String setId = getArmorSetId(piece);
            ArmorPieceType type = getArmorPieceType(piece);
            if (setId != null && type != null) {
                CustomArmorSet set = getArmorSet(setId);
                if (set != null) {
                    ArmorPieceConfig p = set.getPiece(type);
                    if (p != null) total += p.getBreakingPowerBonus();
                }
            }
        }
        return total;
    }

    public double manaBonus(ItemStack item) {
        String setId = getArmorSetId(item);
        ArmorPieceType type = getArmorPieceType(item);
        if (setId != null && type != null) {
            CustomArmorSet set = getArmorSet(setId);
            if (set != null) {
                ArmorPieceConfig p = set.getPiece(type);
                if (p != null) return p.getManaBonus();
            }
        }
        return 0;
    }

    public ItemStack updateArmorLore(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return item;

        String setId = getArmorSetId(item);
        ArmorPieceType type = getArmorPieceType(item);
        if (setId == null || type == null) {
            return item;
        }

        CustomArmorSet set = getArmorSet(setId);
        if (set == null) {
            return item;
        }

        ArmorPieceConfig config = set.getPiece(type);
        if (config == null) {
            return item;
        }

        ItemStack updated = item.clone();
        ItemMeta meta = updated.getItemMeta();
        if (meta == null) {
            return updated;
        }

        applyArmorPresentation(meta, setId, set, type, config);
        updated.setItemMeta(meta);
        return updated;
    }

    public Map<String, CustomArmorSet> getArmorSets() {
        return Collections.unmodifiableMap(armorSets);
    }

    public NamespacedKey getArmorSetKey() {
        return armorSetKey;
    }

    public enum ArmorPieceType {
        HELMET,
        CHESTPLATE,
        LEGGINGS,
        BOOTS
    }

    public static class CustomArmorSet {
        private final String id;
        private final String displayName;
        private final String description;
        private final ItemRarity rarity;
        private final Map<ArmorPieceType, ArmorPieceConfig> pieces;
        private final List<String> bonuses;
        private final int piecesRequired;
        private final RecipeCategory category;
        private final String collectionId;
        private final int collectionTierRequired;

        public CustomArmorSet(String id, String displayName, String description,
                              ItemRarity rarity,
                              Map<ArmorPieceType, ArmorPieceConfig> pieces,
                              List<String> bonuses, int piecesRequired,
                              RecipeCategory category, String collectionId, int collectionTierRequired) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
            this.rarity = rarity;
            this.pieces = pieces;
            this.bonuses = bonuses;
            this.piecesRequired = piecesRequired;
            this.category = category;
            this.collectionId = collectionId;
            this.collectionTierRequired = collectionTierRequired;
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public ItemRarity getRarity() {
            return rarity;
        }

        public Map<ArmorPieceType, ArmorPieceConfig> getPieces() {
            return pieces;
        }

        public ArmorPieceConfig getPiece(ArmorPieceType type) {
            return pieces.get(type);
        }

        public List<String> getBonuses() {
            return bonuses;
        }

        public int getPiecesRequired() {
            return piecesRequired;
        }

        public RecipeCategory getCategory() {
            return category;
        }

        public String getCollectionId() {
            return collectionId;
        }

        public int getCollectionTierRequired() {
            return collectionTierRequired;
        }
    }

    public static class ArmorPieceConfig {
        private final Material material;
        private final String displayName;
        private final List<String> lore;
        private final int armorValue;
        private final double toughness;
        private final boolean glowing;
        private final double manaBonus;
        private final double healthBonus;
        private final double critChanceBonus;
        private final double critDamageBonus;
        private final double farmingFortuneBonus;
        private final int breakingPowerBonus;
        private final int miningSpeedBonus;
        private final String color;

        public ArmorPieceConfig(Material material, String displayName, List<String> lore,
                                int armorValue, double toughness, boolean glowing, double manaBonus,
                                double healthBonus, double critChanceBonus, double critDamageBonus,
                                double farmingFortuneBonus, int breakingPowerBonus, int miningSpeedBonus, String color) {
            this.material = material;
            this.displayName = displayName;
            this.lore = lore;
            this.armorValue = armorValue;
            this.toughness = toughness;
            this.glowing = glowing;
            this.manaBonus = manaBonus;
            this.healthBonus = healthBonus;
            this.critChanceBonus = critChanceBonus;
            this.critDamageBonus = critDamageBonus;
            this.farmingFortuneBonus = farmingFortuneBonus;
            this.breakingPowerBonus = breakingPowerBonus;
            this.miningSpeedBonus = miningSpeedBonus;
            this.color = color;
        }

        public Material getMaterial() {
            return material;
        }

        public String getDisplayName() {
            return displayName;
        }

        public List<String> getLore() {
            return lore;
        }

        public int getArmorValue() {
            return armorValue;
        }

        public double getToughness() {
            return toughness;
        }

        public boolean isGlowing() {
            return glowing;
        }

        public double getManaBonus() {
            return manaBonus;
        }

        public double getHealthBonus() {
            return healthBonus;
        }

        public double getCritChanceBonus() {
            return critChanceBonus;
        }

        public double getCritDamageBonus() {
            return critDamageBonus;
        }

        public double getFarmingFortuneBonus() {
            return farmingFortuneBonus;
        }

        public int getBreakingPowerBonus() {
            return breakingPowerBonus;
        }

        public int getMiningSpeedBonus() {
            return miningSpeedBonus;
        }

        public String getColor() {
            return color;
        }
    }
}
