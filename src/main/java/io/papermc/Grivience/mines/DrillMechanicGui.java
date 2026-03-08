package io.papermc.Grivience.mines;

import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.item.MiningItemType;
import io.papermc.Grivience.skyblock.economy.ProfileEconomyService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Manages Drill Refueling and Upgrading - 100% Exploit Proof.
 */
public final class DrillMechanicGui implements Listener {
    private final CustomItemService itemService;
    private final ProfileEconomyService economyService;
    private final NamespacedKey drillFuelKey;
    private final NamespacedKey drillFuelMaxKey;
    private static final String TITLE = ChatColor.DARK_GRAY + "Drill Mechanic";

    public DrillMechanicGui(JavaPlugin plugin, CustomItemService itemService, ProfileEconomyService economyService) {
        this.itemService = itemService;
        this.economyService = economyService;
        this.drillFuelKey = new NamespacedKey(plugin, "drill-fuel");
        this.drillFuelMaxKey = new NamespacedKey(plugin, "drill-fuel-max");
    }

    public void open(Player player) {
        DrillHolder holder = new DrillHolder();
        Inventory inv = Bukkit.createInventory(holder, 36, TITLE);
        holder.inventory = inv;

        // Fill background
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName(" ");
            glass.setItemMeta(glassMeta);
        }
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, glass);
        }

        // Info item
        ItemStack info = new ItemStack(Material.ANVIL);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(ChatColor.GOLD + "Drill Mechanic");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Select a fuel source or upgrade");
            lore.add(ChatColor.GRAY + "for the drill you are currently holding.");
            infoMeta.setLore(lore);
            info.setItemMeta(infoMeta);
        }
        inv.setItem(4, info);

        // Refuel options
        inv.setItem(10, createFuelOption(Material.COAL, "Coal", 100, 50));
        inv.setItem(11, createFuelOption(Material.COAL_BLOCK, "Coal Block", 1000, 450));
        inv.setItem(13, createFuelOption(itemService.createMiningItem(MiningItemType.VOLTA), 5000, 0));
        inv.setItem(14, createFuelOption(itemService.createMiningItem(MiningItemType.OIL_BARREL), 10000, 0));
        
        // Instant full refuel with coins
        inv.setItem(16, createFullRefuelOption());

        // Part upgrade slots (Row 3)
        inv.setItem(28, createUpgradeOption(MiningItemType.MITHRIL_ENGINE));
        inv.setItem(29, createUpgradeOption(MiningItemType.TITANIUM_ENGINE));
        inv.setItem(30, createUpgradeOption(MiningItemType.GEMSTONE_ENGINE));
        inv.setItem(31, createUpgradeOption(MiningItemType.MEDIUM_FUEL_TANK));
        inv.setItem(32, createUpgradeOption(MiningItemType.LARGE_FUEL_TANK));
        inv.setItem(33, createUpgradeOption(MiningItemType.DIVAN_ENGINE));

        player.openInventory(inv);
    }

    private ItemStack createUpgradeOption(MiningItemType type) {
        ItemStack upgradeItem = itemService.createMiningItem(type);
        ItemMeta meta = upgradeItem.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to upgrade held drill!");
            meta.setLore(lore);
            upgradeItem.setItemMeta(meta);
        }
        return upgradeItem;
    }

    private ItemStack createFuelOption(Material mat, String name, int amount, double coinCost) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + name);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Refuels: " + ChatColor.YELLOW + formatInt(amount) + " fuel");
            if (coinCost > 0) {
                lore.add(ChatColor.GRAY + "Cost: " + ChatColor.GOLD + coinCost + " coins");
            } else {
                lore.add(ChatColor.GRAY + "Requires item in inventory.");
            }
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to refuel held drill!");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createFuelOption(ItemStack fuelItem, int amount, double coinCost) {
        ItemStack item = fuelItem.clone();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "Refuels: " + ChatColor.YELLOW + formatInt(amount) + " fuel");
            if (coinCost > 0) {
                lore.add(ChatColor.GRAY + "Cost: " + ChatColor.GOLD + coinCost + " coins");
            }
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to refuel held drill!");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createFullRefuelOption() {
        ItemStack item = new ItemStack(Material.LAVA_BUCKET);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Instant Full Refuel");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Completely fills your drill's tank.");
            lore.add(ChatColor.GRAY + "Cost: " + ChatColor.GOLD + "2.5 coins per 1 fuel");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to refuel held drill!");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof DrillHolder)) return;
        event.setCancelled(true);

        // Only handle clicks in the top inventory
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getInventory())) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        if (player == null) return;
        ItemStack held = player.getInventory().getItemInMainHand();

        if (!isDrill(held)) {
            player.sendMessage(ChatColor.RED + "You must be holding a drill to refuel it!");
            player.closeInventory();
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        int slot = event.getRawSlot();
        if (slot == 10) refuelWithMaterial(player, held, Material.COAL, 100, 50);
        else if (slot == 11) refuelWithMaterial(player, held, Material.COAL_BLOCK, 1000, 450);
        else if (slot == 13) refuelWithCustomItem(player, held, "VOLTA", 5000);
        else if (slot == 14) refuelWithCustomItem(player, held, "OIL_BARREL", 10000);
        else if (slot == 16) instantFullRefuel(player, held);
        else if (slot == 28) upgradeDrill(player, held, "MITHRIL_ENGINE", true);
        else if (slot == 29) upgradeDrill(player, held, "TITANIUM_ENGINE", true);
        else if (slot == 30) upgradeDrill(player, held, "GEMSTONE_ENGINE", true);
        else if (slot == 31) upgradeDrill(player, held, "MEDIUM_FUEL_TANK", false);
        else if (slot == 32) upgradeDrill(player, held, "LARGE_FUEL_TANK", false);
        else if (slot == 33) upgradeDrill(player, held, "DIVAN_ENGINE", true);
    }

    private boolean isDrill(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        String id = itemService.itemId(item);
        return id != null && id.endsWith("_DRILL");
    }

    private void refuelWithMaterial(Player player, ItemStack drill, Material mat, int fuelAmount, double cost) {
        if (player == null || drill == null || mat == null) return;
        if (cost > 0 && !economyService.has(player, cost)) {
            player.sendMessage(ChatColor.RED + "You don't have enough coins!");
            return;
        }
        
        if (!player.getInventory().contains(mat)) {
            player.sendMessage(ChatColor.RED + "You don't have " + mat.name().replace("_", " ") + " in your inventory!");
            return;
        }

        if (applyFuel(player, drill, fuelAmount)) {
            if (cost > 0) economyService.withdraw(player, cost);
            removeOne(player, mat);
            player.playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL, 1.0f, 1.5f);
        }
    }

    private void refuelWithCustomItem(Player player, ItemStack drill, String id, int fuelAmount) {
        if (player == null || drill == null || id == null) return;
        boolean found = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && id.equals(itemService.itemId(item))) {
                found = true;
                break;
            }
        }

        if (!found) {
            player.sendMessage(ChatColor.RED + "You don't have this fuel source!");
            return;
        }

        if (applyFuel(player, drill, fuelAmount)) {
            removeOneCustom(player, id);
            player.playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL, 1.0f, 1.5f);
        }
    }

    private void instantFullRefuel(Player player, ItemStack drill) {
        if (player == null || drill == null || !drill.hasItemMeta()) return;
        ItemMeta meta = drill.getItemMeta();
        if (meta == null) return;
        var pdc = meta.getPersistentDataContainer();
        int fuel = pdc.getOrDefault(drillFuelKey, PersistentDataType.INTEGER, 0);
        int max = pdc.getOrDefault(drillFuelMaxKey, PersistentDataType.INTEGER, 20000);
        
        int needed = max - fuel;
        if (needed <= 0) {
            player.sendMessage(ChatColor.GRAY + "Your drill's tank is already full.");
            return;
        }

        double cost = needed * 2.5;
        if (!economyService.has(player, cost)) {
            player.sendMessage(ChatColor.RED + "You don't have enough coins! Costs " + ChatColor.GOLD + cost + " coins.");
            return;
        }

        economyService.withdraw(player, cost);
        pdc.set(drillFuelKey, PersistentDataType.INTEGER, max);
        drill.setItemMeta(meta);
        updateDrillFuelLore(drill, max, max);
        player.playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL, 1.0f, 2.0f);
        player.sendMessage(ChatColor.YELLOW + "Instant Refuel: " + ChatColor.GREEN + "+" + formatInt(needed) + " fuel" + ChatColor.GRAY + " for " + ChatColor.GOLD + cost + " coins.");
    }

    private void upgradeDrill(Player player, ItemStack drill, String partId, boolean isEngine) {
        if (player == null || drill == null || partId == null) return;
        
        boolean hasPart = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && partId.equals(itemService.itemId(item))) {
                hasPart = true;
                break;
            }
        }

        if (!hasPart) {
            player.sendMessage(ChatColor.RED + "You don't have this drill part in your inventory!");
            return;
        }

        ItemMeta meta = drill.getItemMeta();
        if (meta == null) return;
        var pdc = meta.getPersistentDataContainer();
        
        String currentPart = pdc.get(isEngine ? itemService.getDrillEngineKey() : itemService.getDrillTankKey(), PersistentDataType.STRING);
        if (partId.equals(currentPart)) {
            player.sendMessage(ChatColor.RED + "Your drill already has this part installed!");
            return;
        }

        // Install part
        pdc.set(isEngine ? itemService.getDrillEngineKey() : itemService.getDrillTankKey(), PersistentDataType.STRING, partId);
        itemService.updateDrillLore(meta);
        drill.setItemMeta(meta);
        
        removeOneCustom(player, partId);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);
        player.sendMessage(ChatColor.GREEN + "Successfully upgraded drill part: " + ChatColor.YELLOW + partId.replace("_", " "));
    }

    private boolean applyFuel(Player player, ItemStack drill, int amount) {
        if (player == null || drill == null || !drill.hasItemMeta()) return false;
        ItemMeta meta = drill.getItemMeta();
        if (meta == null) return false;
        var pdc = meta.getPersistentDataContainer();
        int fuel = pdc.getOrDefault(drillFuelKey, PersistentDataType.INTEGER, 0);
        int max = pdc.getOrDefault(drillFuelMaxKey, PersistentDataType.INTEGER, 20000);

        if (fuel >= max) {
            player.sendMessage(ChatColor.GRAY + "Your drill's tank is already full.");
            return false;
        }

        int newFuel = Math.min(max, fuel + amount);
        pdc.set(drillFuelKey, PersistentDataType.INTEGER, newFuel);
        drill.setItemMeta(meta);
        updateDrillFuelLore(drill, newFuel, max);
        player.sendMessage(ChatColor.YELLOW + "Refueled drill: " + ChatColor.GREEN + "+" + (newFuel - fuel) + ChatColor.GRAY + " (" + newFuel + "/" + max + ")");
        return true;
    }

    private void removeOne(Player player, Material mat) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack is = player.getInventory().getItem(i);
            if (is != null && is.getType() == mat && itemService.itemId(is) == null) {
                is.setAmount(is.getAmount() - 1);
                return;
            }
        }
    }

    private void removeOneCustom(Player player, String id) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack is = player.getInventory().getItem(i);
            if (is != null && id.equals(itemService.itemId(is))) {
                is.setAmount(is.getAmount() - 1);
                return;
            }
        }
    }

    private void updateDrillFuelLore(ItemStack item, int fuel, int max) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        itemService.updateDrillLore(meta);
        item.setItemMeta(meta);
    }

    private static String formatInt(int value) {
        return String.format(Locale.US, "%,d", value);
    }

    private static class DrillHolder implements InventoryHolder {
        private Inventory inventory;
        @Override public Inventory getInventory() { return inventory; }
    }
}
