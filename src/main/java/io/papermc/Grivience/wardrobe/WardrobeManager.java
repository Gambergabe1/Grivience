package io.papermc.Grivience.wardrobe;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public final class WardrobeManager {
    private final File folder;
    private final Map<UUID, List<ItemStack[]>> cache = new ConcurrentHashMap<>();

    public WardrobeManager(File dataFolder) {
        this.folder = new File(dataFolder, "wardrobe");
        if (!folder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            folder.mkdirs();
        }
    }

    public List<ItemStack[]> getSlots(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), id -> load(id));
    }

    public void saveSlot(Player player, int slot, ItemStack[] armor) {
        List<ItemStack[]> slots = getSlots(player);
        ensureSize(slots, slot + 1);
        slots.set(slot, armor);
        save(player.getUniqueId(), slots);
    }

    public ItemStack[] slot(Player player, int slot) {
        List<ItemStack[]> slots = getSlots(player);
        if (slot < 0 || slot >= slots.size()) {
            return null;
        }
        return slots.get(slot);
    }

    private List<ItemStack[]> load(UUID id) {
        File file = new File(folder, id.toString() + ".yml");
        List<ItemStack[]> slots = new ArrayList<>();
        if (!file.exists()) {
            return slots;
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        int count = cfg.getInt("slots", 0);
        for (int i = 0; i < count; i++) {
            ItemStack[] armor = ((List<ItemStack>) cfg.getList("slot." + i, new ArrayList<ItemStack>())).toArray(new ItemStack[0]);
            slots.add(armor);
        }
        return slots;
    }

    private void save(UUID id, List<ItemStack[]> slots) {
        File file = new File(folder, id.toString() + ".yml");
        FileConfiguration cfg = new YamlConfiguration();
        cfg.set("slots", slots.size());
        for (int i = 0; i < slots.size(); i++) {
            ItemStack[] armor = slots.get(i);
            if (armor != null) {
                cfg.set("slot." + i, armor);
            }
        }
        try {
            cfg.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().severe("[Grivience] Failed to save wardrobe for " + id + ": " + e.getMessage());
        }
    }

    private void ensureSize(List<ItemStack[]> list, int size) {
        while (list.size() < size) {
            list.add(null);
        }
    }
}
