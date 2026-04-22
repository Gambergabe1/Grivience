package io.papermc.Grivience.gui;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.skyblock.economy.ProfileEconomyService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI for adding Dungeon Stars to items using coins.
 */
public final class DungeonStarGui implements Listener {
    private static final String TITLE = SkyblockGui.title("Dungeon Star Upgrade");
    private static final int ITEM_SLOT = 13;
    private static final int UPGRADE_SLOT = 31;
    private static final int CLOSE_SLOT = 49;

    private final GriviencePlugin plugin;
    private final CustomItemService itemService;
    private final ProfileEconomyService economyService;

    public DungeonStarGui(GriviencePlugin plugin, CustomItemService itemService, ProfileEconomyService economyService) {
        this.plugin = plugin;
        this.itemService = itemService;
        this.economyService = economyService;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(new StarHolder(), 54, TITLE);
        
        // Fill background
        ItemStack filler = SkyblockGui.filler(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

        inv.setItem(ITEM_SLOT, null); // Empty slot for item
        updateUpgradeButton(inv, null);
        inv.setItem(CLOSE_SLOT, SkyblockGui.closeButton());

        player.openInventory(inv);
    }

    private void updateUpgradeButton(Inventory inv, ItemStack target) {
        if (target == null || target.getType() == Material.AIR) {
            inv.setItem(UPGRADE_SLOT, SkyblockGui.taggedItem(
                    Material.BARRIER,
                    ChatColor.RED + "No Item!",
                    List.of(ChatColor.GRAY + "Place an item above to upgrade it."),
                    "noop", ""
            ));
            return;
        }

        boolean isDungeonized = itemService.isDungeonized(target);
        if (!isDungeonized) {
            double dungeonizeCost = 1_000_000.0;
            ItemStack dungeonizeItem = new ItemStack(Material.NETHER_STAR);
            ItemMeta meta = dungeonizeItem.getItemMeta();
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Dungeonize Item");
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "This item is not dungeon-ready.");
            lore.add(ChatColor.GRAY + "Dungeonizing allows an item to");
            lore.add(ChatColor.GRAY + "receive stars and stat boosts");
            lore.add(ChatColor.GRAY + "while inside Dungeons.");
            lore.add("");
            lore.add(ChatColor.GRAY + "Cost: " + ChatColor.GOLD + String.format("%,.0f", dungeonizeCost) + " coins");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to Dungeonize!");
            
            meta.setLore(lore);
            dungeonizeItem.setItemMeta(meta);
            inv.setItem(UPGRADE_SLOT, dungeonizeItem);
            return;
        }

        int currentStars = itemService.getDungeonStars(target);
        if (currentStars >= 5) {
            inv.setItem(UPGRADE_SLOT, SkyblockGui.taggedItem(
                    Material.BARRIER,
                    ChatColor.RED + "Max Stars!",
                    List.of(ChatColor.GRAY + "This item is already at 5 stars."),
                    "noop", ""
            ));
            return;
        }

        double cost = getStarCost(currentStars);
        int nextStars = currentStars + 1;

        ItemStack upgradeItem = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta meta = upgradeItem.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Upgrade to " + nextStars + " Star" + (nextStars > 1 ? "s" : ""));
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Current Stars: " + ChatColor.GOLD + "✪".repeat(currentStars));
        lore.add(ChatColor.GRAY + "Next Stars: " + ChatColor.GOLD + "✪".repeat(nextStars));
        lore.add("");
        lore.add(ChatColor.GRAY + "Cost: " + ChatColor.GOLD + String.format("%,.0f", cost) + " coins");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to upgrade!");
        
        meta.setLore(lore);
        upgradeItem.setItemMeta(meta);
        inv.setItem(UPGRADE_SLOT, upgradeItem);
    }

    private double getStarCost(int currentStars) {
        return switch (currentStars) {
            case 0 -> 100_000.0;
            case 1 -> 250_000.0;
            case 2 -> 500_000.0;
            case 3 -> 1_000_000.0;
            case 4 -> 2_500_000.0;
            default -> 0.0;
        };
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof StarHolder)) return;

        Inventory inv = event.getInventory();
        int slot = event.getRawSlot();
        Player player = (Player) event.getWhoClicked();

        if (slot < 54) {
            // Clicked in GUI
            if (slot == UPGRADE_SLOT) {
                event.setCancelled(true);
                handleAction(player, inv);
            } else if (slot == CLOSE_SLOT) {
                event.setCancelled(true);
                player.closeInventory();
            } else if (slot != ITEM_SLOT) {
                event.setCancelled(true);
            } else {
                // Clicking the item slot
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    updateUpgradeButton(inv, inv.getItem(ITEM_SLOT));
                }, 1L);
            }
        } else {
            // Clicked in player inventory
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                updateUpgradeButton(inv, inv.getItem(ITEM_SLOT));
            }, 1L);
        }
    }

    private void handleAction(Player player, Inventory inv) {
        ItemStack target = inv.getItem(ITEM_SLOT);
        if (target == null || target.getType() == Material.AIR) return;

        if (!itemService.isDungeonized(target)) {
            handleDungeonize(player, inv, target);
        } else {
            handleUpgrade(player, inv, target);
        }
    }

    private void handleDungeonize(Player player, Inventory inv, ItemStack target) {
        double cost = 1_000_000.0;
        if (!economyService.has(player, cost)) {
            player.sendMessage(ChatColor.RED + "You don't have enough coins to dungeonize this!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        economyService.withdraw(player, cost);
        ItemStack dungeonized = itemService.dungeonize(target);
        inv.setItem(ITEM_SLOT, dungeonized);
        
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Item has been successfully Dungeonized!");
        
        updateUpgradeButton(inv, dungeonized);
    }

    private void handleUpgrade(Player player, Inventory inv, ItemStack target) {
        int currentStars = itemService.getDungeonStars(target);
        if (currentStars >= 5) return;

        double cost = getStarCost(currentStars);
        if (!economyService.has(player, cost)) {
            player.sendMessage(ChatColor.RED + "You don't have enough coins!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        economyService.withdraw(player, cost);
        ItemStack upgraded = itemService.setDungeonStars(target, currentStars + 1);
        inv.setItem(ITEM_SLOT, upgraded);
        
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);
        player.sendMessage(ChatColor.GREEN + "Successfully upgraded item to " + (currentStars + 1) + " stars!");
        
        updateUpgradeButton(inv, upgraded);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof StarHolder)) return;
        if (event.getRawSlots().contains(ITEM_SLOT)) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                updateUpgradeButton(event.getInventory(), event.getInventory().getItem(ITEM_SLOT));
            }, 1L);
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof StarHolder)) return;
        
        Inventory inv = event.getInventory();
        ItemStack item = inv.getItem(ITEM_SLOT);
        if (item != null && item.getType() != Material.AIR) {
            Player player = (Player) event.getPlayer();
            // Return item to player or drop it
            var leftovers = player.getInventory().addItem(item);
            leftovers.values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
        }
    }

    private static final class StarHolder implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }
}
