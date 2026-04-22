package io.papermc.Grivience.dungeon;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.gui.SkyblockGui;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages player class selection for Dungeons.
 */
public final class DungeonClassManager implements Listener {
    private final GriviencePlugin plugin;
    private final Map<UUID, DungeonClass> selectedClasses = new HashMap<>();
    private final NamespacedKey classKey;

    public DungeonClassManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.classKey = new NamespacedKey(plugin, "dungeon_class");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public DungeonClass getSelectedClass(Player player) {
        return selectedClasses.getOrDefault(player.getUniqueId(), DungeonClass.BERSERK); // Berserk is default in HS for new players often
    }

    public void setSelectedClass(Player player, DungeonClass dungeonClass) {
        selectedClasses.put(player.getUniqueId(), dungeonClass);
        player.sendMessage(ChatColor.GREEN + "You are now a " + ChatColor.YELLOW + dungeonClass.getDisplayName() + ChatColor.GREEN + "!");
    }

    public void openClassSelection(Player player) {
        Inventory inv = Bukkit.createInventory(new ClassSelectionHolder(), 27, SkyblockGui.title("Dungeon Class Selection"));
        
        SkyblockGui.fillAll(inv, SkyblockGui.filler(Material.BLACK_STAINED_GLASS_PANE));

        int[] slots = {11, 12, 13, 14, 15};
        DungeonClass[] classes = DungeonClass.values();
        
        for (int i = 0; i < classes.length; i++) {
            DungeonClass cls = classes[i];
            ItemStack item = new ItemStack(cls.getIcon());
            ItemMeta meta = item.getItemMeta();
            
            boolean isSelected = getSelectedClass(player) == cls;
            meta.setDisplayName((isSelected ? ChatColor.GREEN : ChatColor.GOLD) + cls.getDisplayName());
            
            List<String> lore = new ArrayList<>();
            for (String line : cls.getDescription()) {
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            lore.add("");
            if (isSelected) {
                lore.add(ChatColor.GREEN + "SELECTED!");
                item.setType(Material.ENCHANTED_BOOK); // Visual indicator
            } else {
                lore.add(ChatColor.YELLOW + "Click to select!");
            }
            
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            meta.getPersistentDataContainer().set(classKey, PersistentDataType.STRING, cls.name());
            item.setItemMeta(meta);
            
            inv.setItem(slots[i], item);
        }

        inv.setItem(22, SkyblockGui.closeButton());
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ClassSelectionHolder)) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        Player player = (Player) event.getWhoClicked();
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        String className = meta.getPersistentDataContainer().get(classKey, PersistentDataType.STRING);
        if (className != null) {
            try {
                DungeonClass cls = DungeonClass.valueOf(className);
                setSelectedClass(player, cls);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                openClassSelection(player); // Refresh
            } catch (IllegalArgumentException ignored) {}
        } else if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
        }
    }

    private static final class ClassSelectionHolder implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }
}
