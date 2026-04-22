package io.papermc.Grivience.enchantment;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.lang.reflect.Method;

/**
 * Stores and renders Skyblock-style enchantments on items.
 *
 * Vanilla enchants are applied as real Minecraft enchantments when a mapping exists.
 * Custom enchants are stored in PDC and rendered into lore.
 */
public final class SkyblockEnchantStorage {
    private static final String KEY_PREFIX = "sb_enchant_";

    private final Plugin plugin;

    public SkyblockEnchantStorage(Plugin plugin) {
        this.plugin = plugin;
    }

    public int getLevel(ItemStack item, SkyblockEnchantment enchantment) {
        if (item == null || item.getType().isAir() || enchantment == null) {
            return 0;
        }
        if (!item.hasItemMeta()) {
            return 0;
        }

        // Prefer real vanilla enchant levels if present.
        for (Enchantment vanilla : enchantment.getVanillaEnchantment()) {
            int level = item.getEnchantmentLevel(vanilla);
            if (level > 0) {
                return level;
            }
        }

        ItemMeta meta = item.getItemMeta();
        Integer stored = meta.getPersistentDataContainer().get(keyFor(enchantment.getId()), PersistentDataType.INTEGER);
        return stored != null ? Math.max(0, stored) : 0;
    }

    public boolean hasUltimateEnchant(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        for (SkyblockEnchantment enchantment : EnchantmentRegistry.getAll()) {
            if (!enchantment.isUltimate()) {
                continue;
            }
            if (getLevel(item, enchantment) > 0) {
                return true;
            }
        }
        return false;
    }

    public SkyblockEnchantment findFirstUltimateEnchant(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        for (SkyblockEnchantment enchantment : EnchantmentRegistry.getAll()) {
            if (!enchantment.isUltimate()) {
                continue;
            }
            if (getLevel(item, enchantment) > 0) {
                return enchantment;
            }
        }
        return null;
    }

    public ItemStack apply(ItemStack item, SkyblockEnchantment enchantment, int level) {
        if (item == null || item.getType().isAir() || enchantment == null) {
            return item;
        }
        int clamped = Math.max(1, Math.min(level, enchantment.getMaxLevel()));

        ItemStack updated = item.clone();
        ItemMeta meta = updated.getItemMeta();
        if (meta == null) {
            return item;
        }

        // Apply mapped vanilla enchants.
        for (Enchantment vanilla : enchantment.getVanillaEnchantment()) {
            meta.addEnchant(vanilla, clamped, true);
        }

        // Always store Skyblock enchant level so we can render consistently.
        meta.getPersistentDataContainer().set(keyFor(enchantment.getId()), PersistentDataType.INTEGER, clamped);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        // Persist enchant changes before rebuilding lore (so getLevel() sees the new state).
        updated.setItemMeta(meta);

        ItemMeta refreshedMeta = updated.getItemMeta();
        if (refreshedMeta == null) {
            return updated;
        }

        // Rebuild enchant lore (and strip old enchant lines) to avoid duplicates.
        List<String> lore = refreshedMeta.hasLore() && refreshedMeta.getLore() != null
                ? new ArrayList<>(refreshedMeta.getLore())
                : new ArrayList<>();
        lore.removeIf(this::isEnchantLoreLine);
        trimLeadingBlankLines(lore);

        List<String> enchantLines = buildEnchantLoreLines(updated);
        if (!enchantLines.isEmpty()) {
            List<String> combined = new ArrayList<>(enchantLines.size() + lore.size() + 1);
            combined.addAll(enchantLines);
            if (!lore.isEmpty()) {
                combined.add("");
            }
            combined.addAll(lore);
            lore = combined;
        }

        refreshedMeta.setLore(lore);
        refreshedMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        applyGlintOverride(refreshedMeta, !enchantLines.isEmpty());
        updated.setItemMeta(refreshedMeta);
        return updated;
    }

    public ItemStack createEnchantedBook(SkyblockEnchantment enchantment, int level) {
        ItemStack book = new ItemStack(org.bukkit.Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(enchantment.getType().getColor() + "Enchanted Book (" + enchantment.getName() + ")");
            book.setItemMeta(meta);
        }
        return apply(book, enchantment, level);
    }

    public ItemStack refreshLore(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        ItemStack updated = item.clone();
        ItemMeta updatedMeta = updated.getItemMeta();
        if (updatedMeta == null) {
            return item;
        }

        List<String> lore = updatedMeta.hasLore() && updatedMeta.getLore() != null ? new ArrayList<>(updatedMeta.getLore()) : new ArrayList<>();
        lore.removeIf(this::isEnchantLoreLine);
        trimLeadingBlankLines(lore);

        List<String> enchantLines = buildEnchantLoreLines(updated);
        if (!enchantLines.isEmpty()) {
            List<String> combined = new ArrayList<>(enchantLines.size() + lore.size() + 1);
            combined.addAll(enchantLines);
            if (!lore.isEmpty()) {
                combined.add("");
            }
            combined.addAll(lore);
            lore = combined;
        }

        updatedMeta.setLore(lore);
        updatedMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        applyGlintOverride(updatedMeta, !enchantLines.isEmpty());
        updated.setItemMeta(updatedMeta);
        return updated;
    }

    private List<String> buildEnchantLoreLines(ItemStack item) {
        List<EnchantLine> lines = new ArrayList<>();
        for (SkyblockEnchantment enchantment : EnchantmentRegistry.getAll()) {
            int level = getLevel(item, enchantment);
            if (level <= 0) {
                continue;
            }
            lines.add(new EnchantLine(enchantment, level));
        }

        lines.sort(Comparator
                .comparingInt((EnchantLine line) -> line.enchantment().getType().getTier()).reversed()
                .thenComparing(line -> line.enchantment().getName(), String.CASE_INSENSITIVE_ORDER));

        List<String> lore = new ArrayList<>(lines.size());
        for (EnchantLine line : lines) {
            lore.add(line.enchantment().getDisplayName(line.level()));
        }
        return lore;
    }

    private boolean isEnchantLoreLine(String line) {
        if (line == null) {
            return false;
        }
        String stripped = ChatColor.stripColor(line);
        if (stripped == null) {
            return false;
        }
        String normalized = stripped.trim();
        if (normalized.isEmpty()) {
            return false;
        }

        // Legacy format used by some custom items.
        if (normalized.toLowerCase(Locale.ROOT).startsWith("enchant:")) {
            return true;
        }

        // Skyblock-style "Name IV" lines.
        for (SkyblockEnchantment enchantment : EnchantmentRegistry.getAll()) {
            String name = enchantment.getName();
            if (!normalized.startsWith(name)) {
                continue;
            }
            String remainder = normalized.substring(name.length()).trim();
            if (remainder.isEmpty()) {
                continue;
            }
            if (isRomanNumeral(remainder) || isInteger(remainder)) {
                return true;
            }
        }
        return false;
    }

    private void trimLeadingBlankLines(List<String> lore) {
        while (!lore.isEmpty()) {
            String stripped = ChatColor.stripColor(lore.get(0));
            if (stripped != null && stripped.trim().isEmpty()) {
                lore.remove(0);
                continue;
            }
            break;
        }
    }

    private boolean isRomanNumeral(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (char c : value.toCharArray()) {
            if ("IVXLCDM".indexOf(c) == -1) {
                return false;
            }
        }
        return true;
    }

    private boolean isInteger(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    private NamespacedKey keyFor(String enchantId) {
        String safeId = enchantId == null ? "unknown" : enchantId.toLowerCase(Locale.ROOT);
        return new NamespacedKey(plugin, KEY_PREFIX + safeId);
    }

    private void applyGlintOverride(ItemMeta meta, boolean glint) {
        if (meta == null) {
            return;
        }

        // Paper/Bukkit (1.20+) supports glint override without adding real enchantments.
        // Use reflection so the plugin still compiles/runs if the API changes.
        try {
            Method method = ItemMeta.class.getMethod("setEnchantmentGlintOverride", Boolean.class);
            method.invoke(meta, glint ? Boolean.TRUE : null);
            return;
        } catch (NoSuchMethodException ignored) {
            // fall through
        } catch (Throwable ignored) {
            return;
        }

        try {
            Method method = ItemMeta.class.getMethod("setEnchantmentGlintOverride", boolean.class);
            method.invoke(meta, glint);
        } catch (Throwable ignored) {
        }
    }

    private record EnchantLine(SkyblockEnchantment enchantment, int level) {}
}
