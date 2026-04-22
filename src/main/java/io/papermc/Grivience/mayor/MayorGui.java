package io.papermc.Grivience.mayor;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MayorGui implements Listener {
    private final GriviencePlugin plugin;
    private final MayorManager mayorManager;
    private static final String TITLE = ChatColor.GOLD + "Mayor Election";

    public MayorGui(GriviencePlugin plugin, MayorManager mayorManager) {
        this.plugin = plugin;
        this.mayorManager = mayorManager;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        Map<String, Integer> votes = mayorManager.getVoteCounts();
        Map<String, Mayor> allMayors = mayorManager.getAllMayors();

        // Timer Info
        if (mayorManager.isElectionActive()) {
            long remaining = mayorManager.getElectionEndTime() - System.currentTimeMillis();
            inv.setItem(4, createGuiItem(Material.CLOCK, ChatColor.AQUA + "Election Ends In:", "§e" + formatTime(remaining)));
        } else {
            long remaining = mayorManager.getTermEndTime() - System.currentTimeMillis();
            inv.setItem(4, createGuiItem(Material.CLOCK, ChatColor.AQUA + "Term Ends In:", "§e" + formatTime(remaining)));
        }

        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        int index = 0;

        for (Mayor mayor : allMayors.values()) {
            if (index >= slots.length) break;
            
            Material mat = Material.PAPER;
            if (mayor.getName().equalsIgnoreCase("Aatrox")) mat = Material.GOLDEN_SWORD;
            else if (mayor.getName().equalsIgnoreCase("Marina")) mat = Material.FISHING_ROD;
            else if (mayor.getName().equalsIgnoreCase("Paul")) mat = Material.WITHER_SKELETON_SKULL;

            inv.setItem(slots[index++], createGuiItem(mat, ChatColor.YELLOW + mayor.getName(), 
                    "§7Buff: " + mayor.getBuffDescription(), 
                    "§7Current Votes: §e" + votes.getOrDefault(mayor.getName().toUpperCase(), 0), 
                    "", 
                    "§aClick to vote!"));
        }

        // Active Mayor Info
        Mayor active = mayorManager.getActiveMayor();
        if (active != null) {
            List<String> activeLore = new ArrayList<>();
            activeLore.add("§7Current Buffs: §b" + active.getBuffDescription());
            if (!active.getActions().isEmpty()) {
                activeLore.add("");
                activeLore.add("§eClick to interact with " + active.getName() + "!");
            }
            inv.setItem(49, createGuiItem(Material.TOTEM_OF_UNDYING, ChatColor.GREEN + "Current Mayor: " + active.getName(), activeLore.toArray(new String[0])));
        }

        player.openInventory(inv);
    }

    private String formatTime(long ms) {
        if (ms <= 0) return "Soon...";
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) return days + "d " + (hours % 24) + "h";
        if (hours > 0) return hours + "h " + (minutes % 60) + "m";
        if (minutes > 0) return minutes + "m " + (seconds % 60) + "s";
        return seconds + "s";
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
        
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        
        if (event.getRawSlot() == 49) {
            // Interact with active mayor
            mayorManager.performActions(player);
            player.closeInventory();
            return;
        }

        if (name.startsWith("Current Mayor: ")) return;

        // Strip "Current Mayor: " prefix if clicked the center one (handled above but safety)
        if (mayorManager.getAllMayors().containsKey(name.toUpperCase())) {
            mayorManager.castVote(player.getUniqueId(), name);
            player.sendMessage(ChatColor.GREEN + "You voted for " + name + "!");
            player.closeInventory();
        }
    }
}