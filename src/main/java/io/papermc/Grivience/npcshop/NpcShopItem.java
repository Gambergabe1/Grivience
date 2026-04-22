package io.papermc.Grivience.npcshop;

import org.bukkit.inventory.ItemStack;

public record NpcShopItem(ItemStack item, double price, String targetShopId) {
    public NpcShopItem(ItemStack item, double price) {
        this(item, price, null);
    }

    public boolean isCategory() {
        return targetShopId != null;
    }
}