package io.papermc.Grivience.wardrobe;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.item.CustomArmorManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Skyblock 100% Accurate Wardrobe Manager.
 * Handles storage, retrieval, and management of wardrobe slots.
 * Fully compatible with custom armor and NBT data.
 */
public final class WardrobeManager {
    /**
     * Represents a wardrobe slot with name and armor.
     */
    public record SlotData(String name, ItemStack[] armor) {}

    private final File folder;
    private final Map<UUID, List<SlotData>> cache = new ConcurrentHashMap<>();
    private final GriviencePlugin plugin;

    // Configuration
    private final int maxSlots;
    private final int slotsPerLevel;
    private final int baseSlots;

    // NBT Keys for custom armor
    private final NamespacedKey customArmorSetKey;
    private final NamespacedKey customArmorPieceKey;

    public WardrobeManager(GriviencePlugin plugin, File dataFolder) {
        this.plugin = plugin;
        this.folder = new File(dataFolder, "wardrobe");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        // Skyblock-accurate configuration
        this.maxSlots = 18;        // Maximum wardrobe slots
        this.slotsPerLevel = 5;    // Unlock 1 slot every 5 levels
        this.baseSlots = 5;        // Start with 5 slots

        // Initialize NBT keys for custom armor detection
        this.customArmorSetKey = new NamespacedKey(plugin, "armor_set");
        this.customArmorPieceKey = new NamespacedKey(plugin, "armor_piece");
    }

    /**
     * Get all wardrobe slots for a player.
     */
    public List<SlotData> getSlots(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), this::load);
    }

    /**
     * Get a specific slot for a player.
     */
    public SlotData getSlot(Player player, int slotIndex) {
        List<SlotData> slots = getSlots(player);
        if (slotIndex < 0 || slotIndex >= slots.size()) {
            return null;
        }
        return slots.get(slotIndex);
    }

    /**
     * Save armor to a wardrobe slot.
     */
    public void saveSlot(Player player, int slot, ItemStack[] armor) {
        saveSlot(player, slot, armor, defaultName(slot));
    }

    /**
     * Save armor to a wardrobe slot with custom name.
     */
    public void saveSlot(Player player, int slot, ItemStack[] armor, String name) {
        if (armor == null || isEmptyArmor(armor)) {
            return;
        }

        List<SlotData> slots = getSlots(player);
        ensureSize(slots, slot + 1);
        slots.set(slot, new SlotData(name == null || name.isBlank() ? defaultName(slot) : name, cloneArmor(armor)));
        save(player.getUniqueId(), slots);
    }

    /**
     * Rename a wardrobe slot.
     */
    public void rename(Player player, int slot, String name) {
        List<SlotData> slots = getSlots(player);
        ensureSize(slots, slot + 1);
        SlotData existing = slots.get(slot);
        ItemStack[] armor = existing == null ? null : existing.armor();
        slots.set(slot, new SlotData(name == null || name.isBlank() ? defaultName(slot) : name, armor));
        save(player.getUniqueId(), slots);
    }

    /**
     * Check if a player has armor in a slot.
     */
    public boolean hasArmor(Player player, int slot) {
        SlotData data = getSlot(player, slot);
        return data != null && data.armor() != null && !isEmptyArmor(data.armor());
    }

    /**
     * Get the number of used slots.
     */
    public int getUsedSlots(Player player) {
        List<SlotData> slots = getSlots(player);
        int count = 0;
        for (SlotData data : slots) {
            if (data != null && data.armor() != null && !isEmptyArmor(data.armor())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Get the number of allowed slots based on player level.
     * Skyblock-accurate: Unlock slots by leveling up.
     */
    public int getAllowedSlots(Player player) {
        // Get player's Skyblock level
        int level = 1;
        if (Bukkit.getServer().getPluginManager().isPluginEnabled("Grivience")) {
            // Try to get level from stats system
            try {
                org.bukkit.scoreboard.Scoreboard scoreboard = player.getScoreboard();
                if (scoreboard != null) {
                    org.bukkit.scoreboard.Objective levelObj = scoreboard.getObjective("sb_level");
                    if (levelObj != null) {
                        org.bukkit.scoreboard.Score score = levelObj.getScore(player);
                        if (score.isScoreSet()) {
                            level = score.getScore();
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // Calculate allowed slots: base + (level / slotsPerLevel)
        int allowed = baseSlots + (level / slotsPerLevel);
        return Math.min(allowed, maxSlots);
    }

    /**
     * Get the required level to unlock a slot.
     */
    public int getRequiredLevel(int slotIndex) {
        if (slotIndex < baseSlots) {
            return 1; // Base slots available from start
        }
        return (slotIndex - baseSlots + 1) * slotsPerLevel;
    }

    /**
     * Check if armor array is empty.
     */
    public boolean isEmptyArmor(ItemStack[] armor) {
        if (armor == null) return true;
        for (ItemStack piece : armor) {
            if (piece != null && piece.getType() != org.bukkit.Material.AIR) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if an item is custom armor.
     */
    public boolean isCustomArmor(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(customArmorSetKey, PersistentDataType.STRING) ||
               meta.getPersistentDataContainer().has(customArmorPieceKey, PersistentDataType.STRING);
    }

    /**
     * Get custom armor set name from item.
     */
    public String getCustomArmorSet(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(customArmorSetKey, PersistentDataType.STRING);
    }

    /**
     * Get custom armor piece type from item.
     */
    public String getCustomArmorPiece(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(customArmorPieceKey, PersistentDataType.STRING);
    }

    /**
     * Clone an armor array (preserves NBT and custom armor data).
     */
    public ItemStack[] cloneArmor(ItemStack[] src) {
        if (src == null) return null;
        ItemStack[] clone = new ItemStack[src.length];
        for (int i = 0; i < src.length; i++) {
            ItemStack item = src[i];
            clone[i] = item == null ? null : item.clone(); // clone() preserves NBT
        }
        return clone;
    }

    /**
     * Load wardrobe data from file.
     */
    private List<SlotData> load(UUID id) {
        File file = new File(folder, id.toString() + ".yml");
        List<SlotData> slots = new ArrayList<>();

        if (!file.exists()) {
            return slots;
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        int count = cfg.getInt("slots", 0);

        for (int i = 0; i < count; i++) {
            List<?> list = cfg.getList("slot." + i + ".armor", new ArrayList<>());
            ItemStack[] armor = new ItemStack[list.size()];
            for (int j = 0; j < list.size(); j++) {
                Object obj = list.get(j);
                if (obj instanceof ItemStack) {
                    armor[j] = (ItemStack) obj;
                }
            }
            String name = cfg.getString("slot." + i + ".name", defaultName(i));
            slots.add(new SlotData(name, armor));
        }

        return slots;
    }

    /**
     * Save wardrobe data to file.
     */
    private void save(UUID id, List<SlotData> slots) {
        File file = new File(folder, id.toString() + ".yml");
        FileConfiguration cfg = new YamlConfiguration();

        // Count non-empty slots
        int nonEmptyCount = 0;
        for (SlotData data : slots) {
            if (data != null && data.armor() != null && !isEmptyArmor(data.armor())) {
                nonEmptyCount++;
            }
        }

        cfg.set("slots", nonEmptyCount);

        for (int i = 0; i < slots.size(); i++) {
            SlotData data = slots.get(i);
            if (data != null && data.armor() != null && !isEmptyArmor(data.armor())) {
                cfg.set("slot." + i + ".name", data.name());
                cfg.set("slot." + i + ".armor", data.armor());
            }
        }

        try {
            cfg.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().severe("[Grivience] Failed to save wardrobe for " + id + ": " + e.getMessage());
        }
    }

    /**
     * Ensure list has enough capacity.
     */
    private void ensureSize(List<SlotData> list, int size) {
        while (list.size() < size) {
            list.add(null);
        }
    }

    /**
     * Get default slot name.
     */
    private String defaultName(int slot) {
        return "Slot " + (slot + 1);
    }

    /**
     * Get first non-empty armor piece from array.
     */
    public ItemStack getFirstArmorPiece(ItemStack[] armor) {
        if (armor == null) return new ItemStack(Material.ARMOR_STAND);
        for (ItemStack piece : armor) {
            if (piece != null && piece.getType() != Material.AIR) {
                return piece.clone();
            }
        }
        return new ItemStack(Material.ARMOR_STAND);
    }

    /**
     * Clear cache for a player (on logout).
     */
    public void clearCache(UUID playerId) {
        cache.remove(playerId);
    }

    /**
     * Save all cached wardrobe data to disk.
     */
    public void saveAll() {
        if (cache.isEmpty()) return;
        Bukkit.getLogger().info("[Grivience] Saving " + cache.size() + " cached wardrobes...");
        for (Map.Entry<UUID, List<SlotData>> entry : cache.entrySet()) {
            save(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Reload all wardrobe data.
     */
    public void reload() {
        saveAll();
        cache.clear();
    }
}

