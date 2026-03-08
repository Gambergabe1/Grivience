package io.papermc.Grivience.gui;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.item.CustomArmorType;
import io.papermc.Grivience.item.CustomItemService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI to display Guardian Armor recipes - 100% Skyblock accurate.
 */
public final class GuardianRecipeGui implements InventoryHolder, Listener {
    private final GriviencePlugin plugin;
    private final CustomItemService itemService;
    private final Inventory inventory;

    public GuardianRecipeGui(GriviencePlugin plugin, CustomItemService itemService) {
        this.plugin = plugin;
        this.itemService = itemService;
        this.inventory = Bukkit.createInventory(this, 54, "Guardian Armor Recipes");
        
        initialize();
    }

    private void initialize() {
        SkyblockGui.fillAll(inventory, SkyblockGui.filler(Material.BLACK_STAINED_GLASS_PANE));

        // Display Armor Pieces with Recipe Info
        inventory.setItem(13, createRecipeInfo(CustomArmorType.GUARDIAN_HELM, 10));
        inventory.setItem(22, createRecipeInfo(CustomArmorType.GUARDIAN_CHESTPLATE, 10));
        inventory.setItem(31, createRecipeInfo(CustomArmorType.GUARDIAN_LEGGINGS, 10));
        inventory.setItem(40, createRecipeInfo(CustomArmorType.GUARDIAN_BOOTS, 10));

        inventory.setItem(49, SkyblockGui.closeButton());
    }

    private ItemStack createRecipeInfo(CustomArmorType type, int fragmentCount) {
        ItemStack item = itemService.createArmor(type);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        List<String> lore = meta.getLore();
        if (lore == null) lore = new ArrayList<>();
        else lore = new ArrayList<>(lore);

        lore.add("");
        lore.add(ChatColor.GOLD + "Recipe:");
        lore.add(ChatColor.GRAY + "Requires " + ChatColor.BLUE + "10x Guardian Fragments per slot");
        lore.add(ChatColor.GRAY + "(Standard armor shape in crafting table)");
        lore.add(ChatColor.DARK_GRAY + "Shift-click crafting is disabled for this recipe.");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        if (event.getInventory().getHolder() instanceof GuardianRecipeGui) {
            event.setCancelled(true);
            
            // Only process actions for clicks in the top inventory
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getInventory())) {
                if (event.getRawSlot() == 49) {
                    player.closeInventory();
                }
            }
        }
    }
}
