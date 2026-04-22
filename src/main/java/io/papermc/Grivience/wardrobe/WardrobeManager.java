package io.papermc.Grivience.wardrobe;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skyblock.profile.ProfileManager;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import io.papermc.Grivience.stats.SkyblockLevelManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hypixel-style wardrobe storage.
 *
 * Wardrobe data is scoped to the selected SkyBlock profile's canonical profile ID so coop
 * members share the same wardrobe and separate profiles do not bleed into one another.
 */
public final class WardrobeManager {
    public static final int UNLOCK_LEVEL = 5;
    public static final int MAX_SLOTS = 18;
    public static final int DEFAULT_SLOTS = 2;

    private static final int VIP_SLOTS = 5;
    private static final int VIP_PLUS_SLOTS = 7;
    private static final int VIP_PLUS_PLUS_SLOTS = 9;
    private static final int MVP_SLOTS = 11;
    private static final int MVP_PLUS_SLOTS = 14;
    private static final int MVP_PLUS_PLUS_SLOTS = MAX_SLOTS;

    /**
     * Represents a stored wardrobe setup.
     */
    public record SlotData(String name, ItemStack[] armor) {
    }

    private static final class WardrobeProfileData {
        private final List<SlotData> slots;
        private int equippedSlot;

        private WardrobeProfileData(List<SlotData> slots, int equippedSlot) {
            this.slots = slots;
            this.equippedSlot = equippedSlot;
        }
    }

    private final File folder;
    private final Map<UUID, WardrobeProfileData> cache = new ConcurrentHashMap<>();
    private final GriviencePlugin plugin;
    private final NamespacedKey customArmorSetKey;
    private final NamespacedKey customArmorPieceKey;

    public WardrobeManager(GriviencePlugin plugin, File dataFolder) {
        this.plugin = plugin;
        this.folder = new File(dataFolder, "wardrobe");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        this.customArmorSetKey = new NamespacedKey(plugin, "armor_set");
        this.customArmorPieceKey = new NamespacedKey(plugin, "armor_piece");
    }

    public List<SlotData> getSlots(Player player) {
        return state(player).slots;
    }

    public SlotData getSlot(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= MAX_SLOTS) {
            return null;
        }
        List<SlotData> slots = getSlots(player);
        if (slotIndex >= slots.size()) {
            return null;
        }
        return slots.get(slotIndex);
    }

    public void saveSlot(Player player, int slot, ItemStack[] armor) {
        saveSlot(player, slot, armor, defaultName(slot));
    }

    public void saveSlot(Player player, int slot, ItemStack[] armor, String name) {
        if (slot < 0 || slot >= MAX_SLOTS || armor == null || isEmptyArmor(armor)) {
            return;
        }

        WardrobeProfileData state = state(player);
        ensureSize(state.slots, slot + 1);
        state.slots.set(slot, new SlotData(resolveSlotName(slot, name), cloneArmor(armor)));
        save(player, state);
    }

    public void rename(Player player, int slot, String name) {
        if (slot < 0 || slot >= MAX_SLOTS) {
            return;
        }

        WardrobeProfileData state = state(player);
        ensureSize(state.slots, slot + 1);
        SlotData existing = state.slots.get(slot);
        ItemStack[] armor = existing == null ? null : existing.armor();
        state.slots.set(slot, new SlotData(resolveSlotName(slot, name), armor));
        save(player, state);
    }

    public boolean hasArmor(Player player, int slot) {
        SlotData data = getSlot(player, slot);
        return data != null && data.armor() != null && !isEmptyArmor(data.armor());
    }

    public int getUsedSlots(Player player) {
        int count = 0;
        for (SlotData data : getSlots(player)) {
            if (data != null && data.armor() != null && !isEmptyArmor(data.armor())) {
                count++;
            }
        }
        return count;
    }

    public int getAllowedSlots(Player player) {
        if (getSkyBlockLevel(player) < UNLOCK_LEVEL) {
            return 0;
        }
        return slotCapForPermissions(player);
    }

    public int getRequiredLevel(int slotIndex) {
        return UNLOCK_LEVEL;
    }

    public boolean isUnlocked(Player player, int slotIndex) {
        return slotIndex >= 0 && slotIndex < getAllowedSlots(player);
    }

    public String getUnlockRequirement(Player player, int slotIndex) {
        if (getSkyBlockLevel(player) < UNLOCK_LEVEL) {
            return "SkyBlock Level " + UNLOCK_LEVEL;
        }
        String rankRequirement = rankRequirement(slotIndex);
        return rankRequirement == null ? "Unlocked" : rankRequirement;
    }

    public int getEquippedSlot(Player player) {
        return state(player).equippedSlot;
    }

    public boolean isEquippedSlot(Player player, int slot) {
        return getEquippedSlot(player) == slot;
    }

    public void setEquippedSlot(Player player, int slot) {
        WardrobeProfileData state = state(player);
        state.equippedSlot = slot >= 0 && slot < MAX_SLOTS ? slot : -1;
        save(player, state);
    }

    public void clearEquippedSlot(Player player) {
        WardrobeProfileData state = state(player);
        if (state.equippedSlot == -1) {
            return;
        }
        state.equippedSlot = -1;
        save(player, state);
    }

    public boolean isEmptyArmor(ItemStack[] armor) {
        if (armor == null) {
            return true;
        }
        for (ItemStack piece : armor) {
            if (piece != null && !piece.getType().isAir()) {
                return false;
            }
        }
        return true;
    }

    public boolean isCustomArmor(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        var meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(customArmorSetKey, PersistentDataType.STRING)
                || meta.getPersistentDataContainer().has(customArmorPieceKey, PersistentDataType.STRING);
    }

    public String getCustomArmorSet(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(customArmorSetKey, PersistentDataType.STRING);
    }

    public String getCustomArmorPiece(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(customArmorPieceKey, PersistentDataType.STRING);
    }

    public ItemStack[] cloneArmor(ItemStack[] src) {
        if (src == null) {
            return null;
        }
        ItemStack[] clone = new ItemStack[src.length];
        for (int i = 0; i < src.length; i++) {
            clone[i] = src[i] == null ? null : src[i].clone();
        }
        return clone;
    }

    public ItemStack getFirstArmorPiece(ItemStack[] armor) {
        if (armor == null) {
            return new ItemStack(Material.ARMOR_STAND);
        }
        for (int i = armor.length - 1; i >= 0; i--) {
            ItemStack piece = armor[i];
            if (piece != null && !piece.getType().isAir()) {
                return piece.clone();
            }
        }
        return new ItemStack(Material.ARMOR_STAND);
    }

    public void clearCache(UUID profileId) {
        if (profileId != null) {
            cache.remove(profileId);
        }
    }

    public void saveAll() {
        if (cache.isEmpty()) {
            return;
        }
        Bukkit.getLogger().info("[Grivience] Saving " + cache.size() + " cached wardrobes...");
        for (Map.Entry<UUID, WardrobeProfileData> entry : cache.entrySet()) {
            save(entry.getKey(), entry.getValue());
        }
    }

    public void reload() {
        saveAll();
        cache.clear();
    }

    private WardrobeProfileData state(Player player) {
        UUID profileId = resolveProfileId(player);
        return cache.computeIfAbsent(profileId, this::load);
    }

    private UUID resolveProfileId(Player player) {
        UUID fallback = player == null ? null : player.getUniqueId();
        if (fallback == null) {
            return new UUID(0L, 0L);
        }

        ProfileManager profileManager = plugin.getProfileManager();
        if (profileManager != null && player != null) {
            SkyBlockProfile selected = profileManager.getSelectedProfile(player);
            if (selected != null) {
                SkyBlockProfile shared = profileManager.resolveSharedProfile(selected);
                UUID profileId = shared != null ? shared.getProfileId() : selected.getCanonicalProfileId();
                if (profileId != null) {
                    migrateLegacyWardrobe(fallback, profileId);
                    return profileId;
                }
            }
        }

        return fallback;
    }

    private synchronized void migrateLegacyWardrobe(UUID ownerId, UUID profileId) {
        if (ownerId == null || profileId == null || ownerId.equals(profileId)) {
            return;
        }

        File legacyFile = fileFor(ownerId);
        if (!legacyFile.exists()) {
            return;
        }

        File profileFile = fileFor(profileId);
        if (profileFile.exists()) {
            return;
        }

        WardrobeProfileData legacyData = cache.remove(ownerId);
        if (legacyData == null) {
            legacyData = load(ownerId);
        }

        save(profileId, legacyData);
        if (!legacyFile.delete()) {
            plugin.getLogger().warning("Failed to delete legacy wardrobe file " + legacyFile.getName() + " after profile migration.");
        }
    }

    private WardrobeProfileData load(UUID profileId) {
        File file = fileFor(profileId);
        List<SlotData> slots = new ArrayList<>();
        int equippedSlot = -1;

        if (!file.exists()) {
            return new WardrobeProfileData(slots, -1);
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection slotSection = cfg.getConfigurationSection("slot");
        if (slotSection != null) {
            for (String key : slotSection.getKeys(false)) {
                int slotIndex;
                try {
                    slotIndex = Integer.parseInt(key);
                } catch (NumberFormatException ignored) {
                    continue;
                }
                if (slotIndex < 0 || slotIndex >= MAX_SLOTS) {
                    continue;
                }

                ConfigurationSection dataSection = slotSection.getConfigurationSection(key);
                if (dataSection == null) {
                    continue;
                }

                List<?> rawArmor = dataSection.getList("armor", List.of());
                ItemStack[] armor = readArmor(rawArmor);
                if (isEmptyArmor(armor)) {
                    continue;
                }

                ensureSize(slots, slotIndex + 1);
                slots.set(slotIndex, new SlotData(
                        resolveSlotName(slotIndex, dataSection.getString("name")),
                        armor
                ));
            }
        }

        int configuredEquippedSlot = cfg.getInt("active-slot", -1);
        if (configuredEquippedSlot >= 0 && configuredEquippedSlot < slots.size()) {
            SlotData active = slots.get(configuredEquippedSlot);
            if (active != null && !isEmptyArmor(active.armor())) {
                equippedSlot = configuredEquippedSlot;
            }
        }

        return new WardrobeProfileData(slots, equippedSlot);
    }

    private ItemStack[] readArmor(List<?> rawArmor) {
        ItemStack[] armor = new ItemStack[Math.max(4, rawArmor.size())];
        for (int i = 0; i < rawArmor.size(); i++) {
            Object raw = rawArmor.get(i);
            if (raw instanceof ItemStack item) {
                armor[i] = item;
            }
        }
        return armor;
    }

    private void save(Player player, WardrobeProfileData state) {
        save(resolveProfileId(player), state);
    }

    private void save(UUID profileId, WardrobeProfileData state) {
        File file = fileFor(profileId);
        FileConfiguration cfg = new YamlConfiguration();

        int equippedSlot = state.equippedSlot;
        if (equippedSlot < 0 || equippedSlot >= state.slots.size()) {
            equippedSlot = -1;
        } else {
            SlotData active = state.slots.get(equippedSlot);
            if (active == null || isEmptyArmor(active.armor())) {
                equippedSlot = -1;
            }
        }

        cfg.set("active-slot", equippedSlot);
        cfg.set("slot-count", state.slots.size());
        for (int i = 0; i < state.slots.size(); i++) {
            SlotData data = state.slots.get(i);
            if (data == null || data.armor() == null || isEmptyArmor(data.armor())) {
                continue;
            }
            cfg.set("slot." + i + ".name", resolveSlotName(i, data.name()));
            cfg.set("slot." + i + ".armor", cloneArmor(data.armor()));
        }

        try {
            cfg.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().severe("[Grivience] Failed to save wardrobe for " + profileId + ": " + e.getMessage());
        }
    }

    private File fileFor(UUID profileId) {
        return new File(folder, profileId.toString() + ".yml");
    }

    private int getSkyBlockLevel(Player player) {
        SkyblockLevelManager levelManager = plugin.getSkyblockLevelManager();
        if (levelManager == null || player == null) {
            return 0;
        }
        return Math.max(0, levelManager.getLevel(player));
    }

    private int slotCapForPermissions(Player player) {
        if (player == null) {
            return 0;
        }
        if (player.hasPermission("grivience.wardrobe.slots.mvpplusplus")) {
            return MVP_PLUS_PLUS_SLOTS;
        }
        if (player.hasPermission("grivience.wardrobe.slots.mvpplus")) {
            return MVP_PLUS_SLOTS;
        }
        if (player.hasPermission("grivience.wardrobe.slots.mvp")) {
            return MVP_SLOTS;
        }
        if (player.hasPermission("grivience.wardrobe.slots.vipplusplus")) {
            return VIP_PLUS_PLUS_SLOTS;
        }
        if (player.hasPermission("grivience.wardrobe.slots.vipplus")) {
            return VIP_PLUS_SLOTS;
        }
        if (player.hasPermission("grivience.wardrobe.slots.vip")) {
            return VIP_SLOTS;
        }
        return DEFAULT_SLOTS;
    }

    private String rankRequirement(int slotIndex) {
        if (slotIndex < DEFAULT_SLOTS) {
            return null;
        }
        if (slotIndex < VIP_SLOTS) {
            return "VIP Rank";
        }
        if (slotIndex < VIP_PLUS_SLOTS) {
            return "VIP+ Rank";
        }
        if (slotIndex < VIP_PLUS_PLUS_SLOTS) {
            return "VIP++ Rank";
        }
        if (slotIndex < MVP_SLOTS) {
            return "MVP Rank";
        }
        if (slotIndex < MVP_PLUS_SLOTS) {
            return "MVP+ Rank";
        }
        if (slotIndex < MVP_PLUS_PLUS_SLOTS) {
            return "MVP++ Rank";
        }
        return "Unavailable";
    }

    private void ensureSize(List<SlotData> list, int size) {
        while (list.size() < size) {
            list.add(null);
        }
    }

    private String resolveSlotName(int slot, String name) {
        if (name == null || name.isBlank()) {
            return defaultName(slot);
        }
        return name;
    }

    private String defaultName(int slot) {
        return "Slot " + (slot + 1);
    }
}
