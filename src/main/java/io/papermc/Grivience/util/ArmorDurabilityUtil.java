package io.papermc.Grivience.util;

import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class ArmorDurabilityUtil {
    private static final NamespacedKey EXPLICIT_BREAK_KEY = new NamespacedKey("grivience", "allow_armor_break");
    private static final byte BREAK_ALLOWED = (byte) 1;

    private ArmorDurabilityUtil() {
    }

    public static ItemStack ensureArmorUnbreakable(ItemStack item) {
        if (!shouldPreventDurabilityLoss(item)) {
            return item;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        boolean changed = false;
        if (!meta.isUnbreakable()) {
            meta.setUnbreakable(true);
            changed = true;
        }
        if (meta instanceof Damageable damageable && damageable.getDamage() > 0) {
            damageable.setDamage(0);
            changed = true;
        }
        if (!meta.getItemFlags().contains(ItemFlag.HIDE_UNBREAKABLE)) {
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            changed = true;
        }

        if (changed) {
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void ensureArmorUnbreakable(EntityEquipment equipment) {
        if (equipment == null) {
            return;
        }
        equipment.setHelmet(ensureArmorUnbreakable(equipment.getHelmet()));
        equipment.setChestplate(ensureArmorUnbreakable(equipment.getChestplate()));
        equipment.setLeggings(ensureArmorUnbreakable(equipment.getLeggings()));
        equipment.setBoots(ensureArmorUnbreakable(equipment.getBoots()));
    }

    public static ItemStack markExplicitlyBreakable(ItemStack item) {
        if (item == null || isAir(item.getType())) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.getPersistentDataContainer().set(EXPLICIT_BREAK_KEY, PersistentDataType.BYTE, BREAK_ALLOWED);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean shouldPreventDurabilityLoss(ItemStack item) {
        if (item == null || isAir(item.getType())) {
            return false;
        }
        return (isArmor(item) || isWeapon(item) || isCustomItem(item)) && !isExplicitlyBreakable(item);
    }

    public static boolean isArmor(ItemStack item) {
        if (item == null || isAir(item.getType())) {
            return false;
        }
        String typeName = item.getType().name();
        return typeName.endsWith("_HELMET")
                || typeName.endsWith("_CHESTPLATE")
                || typeName.endsWith("_LEGGINGS")
                || typeName.endsWith("_BOOTS");
    }

    public static boolean isWeapon(ItemStack item) {
        if (item == null || isAir(item.getType())) {
            return false;
        }
        String typeName = item.getType().name();
        return typeName.endsWith("_SWORD")
                || typeName.endsWith("_AXE")
                || typeName.endsWith("_BOW")
                || typeName.equals("CROSSBOW")
                || typeName.equals("TRIDENT")
                || typeName.endsWith("_PICKAXE")
                || typeName.endsWith("_HOE")
                || typeName.endsWith("_SHOVEL");
    }

    public static boolean isCustomItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        // Check for any of our standard custom item PDC keys
        for (NamespacedKey key : meta.getPersistentDataContainer().getKeys()) {
            if (key.getNamespace().equals("grivience") && 
                (key.getKey().equals("custom-item-id") || key.getKey().equals("custom_material"))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isExplicitlyBreakable(ItemStack item) {
        if (item == null || isAir(item.getType()) || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        Byte marker = meta.getPersistentDataContainer().get(EXPLICIT_BREAK_KEY, PersistentDataType.BYTE);
        return marker != null && marker == BREAK_ALLOWED;
    }

    private static boolean isAir(Material type) {
        if (type == null) {
            return true;
        }
        String typeName = type.name();
        return "AIR".equals(typeName) || typeName.endsWith("_AIR");
    }
}
