package io.papermc.Grivience.item;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

        meta.setDisplayName(pieceConfig.getDisplayName());
        meta.setLore(pieceConfig.getLore());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

        if (pieceConfig.isGlowing()) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
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
                    new org.bukkit.attribute.AttributeModifier(UUID.randomUUID(), "generic.armor",
                            pieceConfig.getArmorValue(), org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER,
                            slotGroup));
        }

        if (pieceConfig.getToughness() > 0) {
            meta.addAttributeModifier(org.bukkit.attribute.Attribute.ARMOR_TOUGHNESS,
                    new org.bukkit.attribute.AttributeModifier(UUID.randomUUID(), "generic.armor_toughness",
                            pieceConfig.getToughness(), org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER,
                            slotGroup));
        }

        item.setItemMeta(meta);
        return item;
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

                pieces.put(pieceType, new ArmorPieceConfig(material, pieceName, lore, armorValue, toughness, glowing));
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

        public ArmorPieceConfig(Material material, String displayName, List<String> lore,
                                int armorValue, double toughness, boolean glowing) {
            this.material = material;
            this.displayName = displayName;
            this.lore = lore;
            this.armorValue = armorValue;
            this.toughness = toughness;
            this.glowing = glowing;
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
    }
}
