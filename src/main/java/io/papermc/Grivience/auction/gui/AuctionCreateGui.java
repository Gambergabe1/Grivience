package io.papermc.Grivience.auction.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class AuctionCreateGui implements InventoryHolder {

    private final Inventory inventory;
    private final Player player;
    private boolean isBin = true;
    private long price = 500;
    private long durationHours = 24;
    private ItemStack itemToAuction = null;

    public AuctionCreateGui(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, "Create Auction");
        update();
    }

    public void update() {
        inventory.clear();
        
        if (itemToAuction != null) {
            inventory.setItem(13, itemToAuction);
        }

        ItemStack priceItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta priceMeta = priceItem.getItemMeta();
        priceMeta.setDisplayName(ChatColor.GOLD + "Price / Starting Bid: " + price);
        List<String> priceLore = new ArrayList<>();
        priceLore.add(ChatColor.GRAY + "Left Click: +100");
        priceLore.add(ChatColor.GRAY + "Right Click: -100");
        priceMeta.setLore(priceLore);
        priceItem.setItemMeta(priceMeta);
        inventory.setItem(29, priceItem);

        ItemStack typeItem = new ItemStack(isBin ? Material.NAME_TAG : Material.PAPER);
        ItemMeta typeMeta = typeItem.getItemMeta();
        typeMeta.setDisplayName(ChatColor.YELLOW + "Auction Type: " + (isBin ? "BIN" : "Auction"));
        List<String> typeLore = new ArrayList<>();
        typeLore.add(ChatColor.GRAY + "Click to toggle");
        typeMeta.setLore(typeLore);
        typeItem.setItemMeta(typeMeta);
        inventory.setItem(31, typeItem);

        ItemStack durationItem = new ItemStack(Material.CLOCK);
        ItemMeta durationMeta = durationItem.getItemMeta();
        durationMeta.setDisplayName(ChatColor.AQUA + "Duration: " + durationHours + " hours");
        List<String> durationLore = new ArrayList<>();
        durationLore.add(ChatColor.GRAY + "Left Click: +1h");
        durationLore.add(ChatColor.GRAY + "Right Click: -1h");
        durationMeta.setLore(durationLore);
        durationItem.setItemMeta(durationMeta);
        inventory.setItem(33, durationItem);

        ItemStack createItem = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta createMeta = createItem.getItemMeta();
        createMeta.setDisplayName(ChatColor.GREEN + "Create Auction");
        createItem.setItemMeta(createMeta);
        inventory.setItem(49, createItem);
    }

    public void changePrice(long delta) {
        this.price = Math.max(1, this.price + delta);
        update();
    }

    public void changeDuration(long delta) {
        this.durationHours = Math.max(1, Math.min(336, this.durationHours + delta)); // Max 14 days
        update();
    }

    public void toggleType() {
        this.isBin = !this.isBin;
        update();
    }

    public void setItemToAuction(ItemStack item) {
        this.itemToAuction = item;
        update();
    }

    public ItemStack getItemToAuction() {
        return itemToAuction;
    }

    public boolean isBin() {
        return isBin;
    }

    public long getPrice() {
        return price;
    }

    public long getDurationHours() {
        return durationHours;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
