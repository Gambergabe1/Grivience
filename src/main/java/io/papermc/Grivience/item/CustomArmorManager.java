package io.papermc.Grivience.item;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class CustomArmorManager {
    private final GriviencePlugin plugin;
    private final Map<String, CustomArmorSet> armorSets = new HashMap<>();
    private final NamespacedKey armorSetKey;
    private final NamespacedKey armorPieceKey;

    public CustomArmorManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.armorSetKey = new NamespacedKey(plugin, "armor_set");
        this.armorPieceKey = new NamespacedKey(plugin, "armor_piece");
    }

    public void registerArmorSet(CustomArmorSet armorSet) {
        armorSets.put(armorSet.getId(), armorSet);
    }

    public CustomArmorSet getArmorSet(String id) {
        return armorSets.get(id);
    }

    public Map<String, CustomArmorSet> getArmorSets() {
        return new HashMap<>(armorSets);
    }

    public ItemStack createArmorPiece(String setId, ArmorPieceType pieceType) {
        CustomArmorSet armorSet = armorSets.get(setId);
        if (armorSet == null) {
            return null;
        }

        ArmorPieceConfig pieceConfig = armorSet.getPiece(pieceType);
        if (pieceConfig == null) {
            return null;
        }

        ItemStack item = new ItemStack(pieceConfig.getMaterial());
        ItemMeta meta = item.getItemMeta();

        List<String> rawLore = pieceConfig.getLore() == null ? List.of() : pieceConfig.getLore();
        ItemRarity rarity = rarityFromLore(rawLore, ItemRarity.RARE);

        String baseName = ChatColor.stripColor(pieceConfig.getDisplayName());
        if (baseName == null || baseName.isBlank()) {
            baseName = pieceType.name();
        }
        meta.setDisplayName(rarity.color() + baseName);
        meta.setLore(normalizeLore(rawLore, rarity, pieceType));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
        meta.setUnbreakable(true);

        if (pieceConfig.isGlowing()) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        }

        if (meta instanceof Damageable damageable && damageable.getDamage() > 0) {
            damageable.setDamage(0);
        }

        if (meta instanceof LeatherArmorMeta leatherMeta) {
            Color leatherColor = parseLeatherColor(pieceConfig.getColor());
            if (leatherColor != null) {
                leatherMeta.setColor(leatherColor);
                meta = leatherMeta;
            }
        }

        meta.getPersistentDataContainer().set(armorSetKey, PersistentDataType.STRING, setId);
        meta.getPersistentDataContainer().set(armorPieceKey, PersistentDataType.STRING, pieceType.name());

        // Apply custom stats with correct equipment slot
        org.bukkit.inventory.EquipmentSlotGroup slotGroup = switch (pieceType) {
            case HELMET -> org.bukkit.inventory.EquipmentSlotGroup.HEAD;
            case CHESTPLATE -> org.bukkit.inventory.EquipmentSlotGroup.CHEST;
            case LEGGINGS -> org.bukkit.inventory.EquipmentSlotGroup.LEGS;
            case BOOTS -> org.bukkit.inventory.EquipmentSlotGroup.FEET;
        };

        // Apply custom stats
        if (pieceConfig.getArmorValue() > 0) {
            meta.addAttributeModifier(org.bukkit.attribute.Attribute.ARMOR,
                    new org.bukkit.attribute.AttributeModifier(
                            new NamespacedKey(plugin, "armor_" + pieceType.name().toLowerCase()),
                            pieceConfig.getArmorValue(),
                            org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER,
                            slotGroup));
        }

        if (pieceConfig.getToughness() > 0) {
            meta.addAttributeModifier(org.bukkit.attribute.Attribute.ARMOR_TOUGHNESS,
                    new org.bukkit.attribute.AttributeModifier(
                            new NamespacedKey(plugin, "armor_toughness_" + pieceType.name().toLowerCase()),
                            pieceConfig.getToughness(),
                            org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER,
                            slotGroup));
        }

        if (pieceConfig.getHealthBonus() > 0) {
            meta.addAttributeModifier(org.bukkit.attribute.Attribute.MAX_HEALTH,
                    new org.bukkit.attribute.AttributeModifier(
                            new NamespacedKey(plugin, "armor_health_" + pieceType.name().toLowerCase()),
                            pieceConfig.getHealthBonus(),
                            org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER,
                            slotGroup));
        }

        item.setItemMeta(meta);
        return item;
    }

    private Color parseLeatherColor(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String normalized = raw.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }

        if (normalized.matches("[0-9a-fA-F]{6}")) {
            try {
                int rgb = Integer.parseInt(normalized, 16);
                return Color.fromRGB(rgb);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }

        String[] parts = normalized.split(",");
        if (parts.length != 3) {
            return null;
        }
        try {
            int red = Integer.parseInt(parts[0].trim());
            int green = Integer.parseInt(parts[1].trim());
            int blue = Integer.parseInt(parts[2].trim());
            return Color.fromRGB(red, green, blue);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private List<String> normalizeLore(List<String> lore, ItemRarity rarity, ArmorPieceType pieceType) {
        List<String> normalized = new ArrayList<>();
        if (lore != null) {
            for (String line : lore) {
                if (line != null) {
                    normalized.add(line);
                }
            }
        }

        // Remove any existing rarity/type line and re-append it Hypixel-style.
        int rarityIndex = findRarityLineIndex(normalized, pieceType);
        if (rarityIndex != -1) {
            normalized.remove(rarityIndex);
        }

        // Trim trailing blanks.
        while (!normalized.isEmpty()) {
            String last = normalized.get(normalized.size() - 1);
            if (last == null || last.isBlank()) {
                normalized.remove(normalized.size() - 1);
                continue;
            }
            break;
        }

        if (!normalized.isEmpty()) {
            normalized.add("");
        }
        String rarityName = rarity == null ? "RARE" : rarity.name();
        String typeName = pieceType == null ? "HELMET" : pieceType.name();
        normalized.add((rarity == null ? ChatColor.BLUE : rarity.color()) + "" + ChatColor.BOLD + rarityName + " " + typeName);
        return normalized;
    }

    private int findRarityLineIndex(List<String> lore, ArmorPieceType pieceType) {
        if (lore == null || lore.isEmpty()) {
            return -1;
        }

        String requiredType = pieceType == null ? null : pieceType.name();
        for (int i = lore.size() - 1; i >= 0; i--) {
            String line = lore.get(i);
            if (line == null || line.isBlank()) {
                continue;
            }
            String plain = ChatColor.stripColor(line);
            if (plain == null) {
                continue;
            }
            String upper = plain.trim().toUpperCase(Locale.ROOT);
            if (upper.contains("MYTHIC") || upper.contains("LEGENDARY") || upper.contains("EPIC")
                    || upper.contains("RARE") || upper.contains("UNCOMMON") || upper.contains("COMMON")) {
                if (requiredType != null && !upper.contains(requiredType) && !upper.contains("ARMOR")) {
                    continue;
                }
                return i;
            }
        }
        return -1;
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
            String plain = ChatColor.stripColor(line);
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

    public String getArmorSetId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(armorSetKey, PersistentDataType.STRING);
    }

    public ArmorPieceType getArmorPieceType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        String pieceName = meta.getPersistentDataContainer().get(armorPieceKey, PersistentDataType.STRING);
        if (pieceName == null) {
            return null;
        }
        try {
            return ArmorPieceType.valueOf(pieceName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public double manaBonus(ItemStack item) {
        String setId = getArmorSetId(item);
        ArmorPieceType type = getArmorPieceType(item);
        if (setId == null || type == null) {
            return 0.0D;
        }
        CustomArmorSet set = armorSets.get(setId);
        if (set == null) {
            return 0.0D;
        }
        ArmorPieceConfig cfg = set.getPiece(type);
        return cfg == null ? 0.0D : cfg.getManaBonus();
    }

    public double critChanceBonus(ItemStack item) {
        String setId = getArmorSetId(item);
        ArmorPieceType type = getArmorPieceType(item);
        if (setId == null || type == null) {
            return 0.0D;
        }
        CustomArmorSet set = armorSets.get(setId);
        if (set == null) {
            return 0.0D;
        }
        ArmorPieceConfig cfg = set.getPiece(type);
        return cfg == null ? 0.0D : cfg.getCritChanceBonus();
    }

    public double critDamageBonus(ItemStack item) {
        String setId = getArmorSetId(item);
        ArmorPieceType type = getArmorPieceType(item);
        if (setId == null || type == null) {
            return 0.0D;
        }
        CustomArmorSet set = armorSets.get(setId);
        if (set == null) {
            return 0.0D;
        }
        ArmorPieceConfig cfg = set.getPiece(type);
        return cfg == null ? 0.0D : cfg.getCritDamageBonus();
    }

    public double farmingFortuneBonus(ItemStack item) {
        String setId = getArmorSetId(item);
        ArmorPieceType type = getArmorPieceType(item);
        if (setId == null || type == null) {
            return 0.0D;
        }
        CustomArmorSet set = armorSets.get(setId);
        if (set == null) {
            return 0.0D;
        }
        ArmorPieceConfig cfg = set.getPiece(type);
        return cfg == null ? 0.0D : cfg.getFarmingFortuneBonus();
    }

    public double totalCritChanceBonus(Player player) {
        double total = 0.0D;
        if (player == null) return total;
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            total += critChanceBonus(piece);
        }
        return total;
    }

    public double totalCritDamageBonus(Player player) {
        double total = 0.0D;
        if (player == null) return total;
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            total += critDamageBonus(piece);
        }
        return total;
    }

    public double totalFarmingFortuneBonus(Player player) {
        double total = 0.0D;
        if (player == null) return total;
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            total += farmingFortuneBonus(piece);
        }
        return total;
    }

    public int countEquippedPieces(Player player, String setId) {
        if (player == null || setId == null || setId.isBlank()) {
            return 0;
        }

        int pieces = 0;
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            if (piece == null) {
                continue;
            }
            String equippedSetId = getArmorSetId(piece);
            if (setId.equalsIgnoreCase(equippedSetId)) {
                pieces++;
            }
        }
        return pieces;
    }

    public boolean hasEquippedPieces(Player player, String setId, int requiredPieces) {
        if (requiredPieces <= 0) {
            return true;
        }
        return countEquippedPieces(player, setId) >= requiredPieces;
    }

    public boolean isCustomArmor(ItemStack item) {
        return getArmorSetId(item) != null;
    }

    public void reloadFromConfig() {
        armorSets.clear();

        Map<String, Object> setsConfig = plugin.getConfig().getConfigurationSection("custom-armor.sets").getValues(false);
        for (String setId : setsConfig.keySet()) {
            CustomArmorSet armorSet = loadArmorSet(setId);
            if (armorSet != null) {
                armorSets.put(setId, armorSet);
            }
        }
    }

    private CustomArmorSet loadArmorSet(String setId) {
        String path = "custom-armor.sets." + setId;
        if (!plugin.getConfig().contains(path)) {
            return null;
        }

        String displayName = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString(path + ".display-name", setId));
        String description = plugin.getConfig().getString(path + ".description", "");
        int piecesRequired = plugin.getConfig().getInt(path + ".pieces-required", 4);

        Map<ArmorPieceType, ArmorPieceConfig> pieces = new HashMap<>();
        for (ArmorPieceType pieceType : ArmorPieceType.values()) {
            String piecePath = path + ".pieces." + pieceType.name().toLowerCase();
            if (plugin.getConfig().contains(piecePath)) {
                Material material = Material.getMaterial(plugin.getConfig().getString(piecePath + ".material", "DIAMOND_CHESTPLATE"));
                String pieceName = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString(piecePath + ".display-name", ""));
                List<String> lore = new ArrayList<>();
                for (String loreLine : plugin.getConfig().getStringList(piecePath + ".lore")) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', loreLine));
                }
                int armorValue = plugin.getConfig().getInt(piecePath + ".armor", 0);
                double toughness = plugin.getConfig().getDouble(piecePath + ".toughness", 0);
                boolean glowing = plugin.getConfig().getBoolean(piecePath + ".glowing", false);
                double manaBonus = plugin.getConfig().getDouble(piecePath + ".mana", 0.0D);
                double healthBonus = plugin.getConfig().getDouble(piecePath + ".health", 0.0D);
                double critChance = plugin.getConfig().getDouble(piecePath + ".crit-chance", 0.0D);
                double critDamage = plugin.getConfig().getDouble(piecePath + ".crit-damage", 0.0D);
                double farmingFortune = plugin.getConfig().getDouble(piecePath + ".farming-fortune", 0.0D);
                String color = plugin.getConfig().getString(piecePath + ".color");

                pieces.put(pieceType, new ArmorPieceConfig(
                        material,
                        pieceName,
                        lore,
                        armorValue,
                        toughness,
                        glowing,
                        manaBonus,
                        healthBonus,
                        critChance,
                        critDamage,
                        farmingFortune,
                        color
                ));
            }
        }

        List<String> bonuses = plugin.getConfig().getStringList(path + ".bonuses");
        List<String> translatedBonuses = new ArrayList<>();
        for (String bonus : bonuses) {
            translatedBonuses.add(ChatColor.translateAlternateColorCodes('&', bonus));
        }

        return new CustomArmorSet(setId, displayName, description, pieces, translatedBonuses, piecesRequired);
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
        private final Map<ArmorPieceType, ArmorPieceConfig> pieces;
        private final List<String> bonuses;
        private final int piecesRequired;

        public CustomArmorSet(String id, String displayName, String description,
                              Map<ArmorPieceType, ArmorPieceConfig> pieces,
                              List<String> bonuses, int piecesRequired) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
            this.pieces = pieces;
            this.bonuses = bonuses;
            this.piecesRequired = piecesRequired;
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

        public int getTotalPieces() {
            return pieces.size();
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
        private final String color;

        public ArmorPieceConfig(Material material, String displayName, List<String> lore,
                                int armorValue, double toughness, boolean glowing, double manaBonus,
                                double healthBonus, double critChanceBonus, double critDamageBonus,
                                double farmingFortuneBonus, String color) {
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

        public String getColor() {
            return color;
        }
    }
}
