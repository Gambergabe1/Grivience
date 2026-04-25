package io.papermc.Grivience.farming;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.gui.SkyblockGui;
import io.papermc.Grivience.item.CustomToolType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AnitaShopGui implements Listener {
    private static final String TITLE = SkyblockGui.title("Anita's Shop");

    private final GriviencePlugin plugin;
    private final FarmingContestManager manager;

    private final org.bukkit.NamespacedKey actionKey;

    public AnitaShopGui(GriviencePlugin plugin, FarmingContestManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.actionKey = new org.bukkit.NamespacedKey(plugin, "anita_action");
    }

    private ItemStack createButton(Material material, String name, List<String> lore, String action) {
        ItemStack item = SkyblockGui.button(material, name, lore);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null && action != null && !action.isBlank()) {
            meta.getPersistentDataContainer().set(actionKey, org.bukkit.persistence.PersistentDataType.STRING, action);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String getAction(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(actionKey, org.bukkit.persistence.PersistentDataType.STRING);
    }

    public void openMenu(Player player) {
        if (player == null) return;
        Inventory inventory = Bukkit.createInventory(new AnitaHolder(), 54, TITLE);

        SkyblockGui.fillAll(inventory, SkyblockGui.filler(Material.GRAY_STAINED_GLASS_PANE));

        UUID profileId = manager.getResolveContestProfileId(player);
        long tickets = manager.getCurrency(profileId, FarmingContestManager.ContestCurrency.TICKETS);
        long bronze = manager.getCurrency(profileId, FarmingContestManager.ContestCurrency.BRONZE);
        long silver = manager.getCurrency(profileId, FarmingContestManager.ContestCurrency.SILVER);
        long gold = manager.getCurrency(profileId, FarmingContestManager.ContestCurrency.GOLD);

        // Wallet display
        inventory.setItem(4, SkyblockGui.button(Material.SUNFLOWER, ChatColor.GOLD + "Your Wallet", List.of(
                ChatColor.GRAY + "Cletus Tickets: " + ChatColor.AQUA + tickets,
                ChatColor.GRAY + "Bronze Medals: " + ChatColor.GOLD + bronze,
                ChatColor.GRAY + "Silver Medals: " + ChatColor.WHITE + silver,
                ChatColor.GRAY + "Gold Medals: " + ChatColor.YELLOW + gold
        )));

        // Mathematical Hoe Blueprint
        addShopItem(inventory, 20, CustomToolType.MATHEMATICAL_HOE_BLUEPRINT, "Mathematical Hoe Blueprint",
                32, 0, 0, 1, tickets, bronze, silver, gold);

        // Melon Dicer
        addShopItem(inventory, 22, CustomToolType.MELON_DICER, "Melon Dicer",
                32, 0, 0, 1, tickets, bronze, silver, gold);

        // Pumpkin Dicer
        addShopItem(inventory, 24, CustomToolType.PUMPKIN_DICER, "Pumpkin Dicer",
                32, 0, 0, 1, tickets, bronze, silver, gold);

        inventory.setItem(48, createButton(Material.ARROW, ChatColor.YELLOW + "Back", List.of(ChatColor.GRAY + "Return to Farming Contest menu."), "back"));
        inventory.setItem(49, SkyblockGui.closeButton());

        player.openInventory(inventory);
    }

    private void addShopItem(Inventory inv, int slot, CustomToolType type, String name, int reqTickets, int reqBronze, int reqSilver, int reqGold,
                             long tickets, long bronze, long silver, long gold) {
        
        ItemStack display = plugin.getCustomItemService().createTool(type, null);
        if (display == null) {
            inv.setItem(slot, SkyblockGui.button(Material.BARRIER, ChatColor.RED + "Error loading " + name, List.of()));
            return;
        }
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Item: " + ChatColor.AQUA + name);
        lore.add("");
        lore.add(ChatColor.GRAY + "Cost:");
        if (reqTickets > 0) lore.add(ChatColor.GRAY + "- " + ChatColor.AQUA + reqTickets + " Cletus Tickets");
        if (reqBronze > 0) lore.add(ChatColor.GRAY + "- " + ChatColor.GOLD + reqBronze + " Bronze Medals");
        if (reqSilver > 0) lore.add(ChatColor.GRAY + "- " + ChatColor.WHITE + reqSilver + " Silver Medals");
        if (reqGold > 0) lore.add(ChatColor.GRAY + "- " + ChatColor.YELLOW + reqGold + " Gold Medals");
        lore.add("");
        
        boolean canAfford = tickets >= reqTickets && bronze >= reqBronze && silver >= reqSilver && gold >= reqGold;
        
        if (canAfford) {
            lore.add(ChatColor.YELLOW + "Click to buy!");
        } else {
            lore.add(ChatColor.RED + "You cannot afford this!");
        }
        
        inv.setItem(slot, createButton(display.getType(), ChatColor.GREEN + "Buy " + name, lore, type.name()));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof AnitaHolder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack current = event.getCurrentItem();
        if (current == null) return;
        
        String action = getAction(current);
        if (action == null) {
            // Check if it's the standard close button
            if (current.getType() == Material.BARRIER && ChatColor.stripColor(current.getItemMeta().getDisplayName()).equals("Close")) {
                action = "close";
            } else {
                return;
            }
        }

        if (action.equals("close")) {
            player.closeInventory();
            return;
        } else if (action.equals("back")) {
            manager.openMenu(player);
            return;
        }

        CustomToolType type = CustomToolType.parse(action);
        if (type != null) {
            handlePurchase(player, type);
        }
    }

    private void handlePurchase(Player player, CustomToolType type) {
        UUID profileId = manager.getResolveContestProfileId(player);
        long tickets = manager.getCurrency(profileId, FarmingContestManager.ContestCurrency.TICKETS);
        long bronze = manager.getCurrency(profileId, FarmingContestManager.ContestCurrency.BRONZE);
        long silver = manager.getCurrency(profileId, FarmingContestManager.ContestCurrency.SILVER);
        long gold = manager.getCurrency(profileId, FarmingContestManager.ContestCurrency.GOLD);

        // All items cost 32 tickets and 1 gold medal for simplicity
        int reqTickets = 32;
        int reqBronze = 0;
        int reqSilver = 0;
        int reqGold = 1;

        if (tickets >= reqTickets && bronze >= reqBronze && silver >= reqSilver && gold >= reqGold) {
            // Deduct
            manager.addCurrency(profileId, FarmingContestManager.ContestCurrency.TICKETS, -reqTickets);
            manager.addCurrency(profileId, FarmingContestManager.ContestCurrency.GOLD, -reqGold);
            
            // Give item
            ItemStack item = plugin.getCustomItemService().createTool(type, null);
            if (item != null) {
                player.getInventory().addItem(item);
                player.sendMessage(ChatColor.GREEN + "You purchased a " + ChatColor.AQUA + type.name() + ChatColor.GREEN + "!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
            openMenu(player); // Refresh
        } else {
            player.sendMessage(ChatColor.RED + "You cannot afford this item!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    private static class AnitaHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}