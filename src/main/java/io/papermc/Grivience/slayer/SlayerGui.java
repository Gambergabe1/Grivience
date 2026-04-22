package io.papermc.Grivience.slayer;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class SlayerGui implements Listener {
    private final GriviencePlugin plugin;
    private final SlayerManager slayerManager;
    private static final String TITLE = ChatColor.DARK_RED + "Slayer Bosses";

    public SlayerGui(GriviencePlugin plugin, SlayerManager slayerManager) {
        this.plugin = plugin;
        this.slayerManager = slayerManager;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        inv.setItem(11, createGuiItem(Material.ROTTEN_FLESH, ChatColor.RED + "Revenant Horror", 
                "§7Target: §cZombies", "§7Kills Required: §c" + SlayerType.ZOMBIE.getRequiredXp(), "", "§eClick to start quest!"));
        inv.setItem(13, createGuiItem(Material.SPIDER_EYE, ChatColor.DARK_PURPLE + "Tarantula Broodfather", 
                "§7Target: §cSpiders", "§7Kills Required: §c" + SlayerType.SPIDER.getRequiredXp(), "", "§eClick to start quest!"));
        inv.setItem(15, createGuiItem(Material.BONE, ChatColor.WHITE + "Sven Packmaster", 
                "§7Target: §cWolves", "§7Kills Required: §c" + SlayerType.WOLF.getRequiredXp(), "", "§eClick to start quest!"));

        player.openInventory(inv);
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITLE)) return;
        event.setCancelled(true);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getInventory())) return;

        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        int slot = event.getRawSlot();
        if (slot == 11) {
            slayerManager.startQuest(player, SlayerType.ZOMBIE);
            player.closeInventory();
        } else if (slot == 13) {
            slayerManager.startQuest(player, SlayerType.SPIDER);
            player.closeInventory();
        } else if (slot == 15) {
            slayerManager.startQuest(player, SlayerType.WOLF);
            player.closeInventory();
        }
    }
}