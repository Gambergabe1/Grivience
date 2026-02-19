package io.papermc.Grivience.listener;

import io.papermc.Grivience.item.CustomItemService;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

public final class CustomItemEnchantListener implements Listener {
    private final CustomItemService customItemService;

    public CustomItemEnchantListener(CustomItemService customItemService) {
        this.customItemService = customItemService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inventory = event.getInventory();
        ItemStack base = inventory.getItem(0);
        ItemStack addition = inventory.getItem(1);
        if (!isCustomDungeonWeapon(base) || addition == null) {
            return;
        }

        ItemStack result = base.clone();
        boolean changed = false;

        if (addition.hasItemMeta() && addition.getItemMeta() instanceof EnchantmentStorageMeta bookMeta) {
            for (Map.Entry<Enchantment, Integer> entry : bookMeta.getStoredEnchants().entrySet()) {
                int current = result.getEnchantmentLevel(entry.getKey());
                if (entry.getValue() > current) {
                    result.addUnsafeEnchantment(entry.getKey(), entry.getValue());
                    changed = true;
                }
            }
        } else if (addition.hasItemMeta()) {
            ItemMeta meta = addition.getItemMeta();
            for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                int current = result.getEnchantmentLevel(entry.getKey());
                if (entry.getValue() > current) {
                    result.addUnsafeEnchantment(entry.getKey(), entry.getValue());
                    changed = true;
                }
            }
        }

        if (!changed) {
            return;
        }

        result = customItemService.syncWeaponEnchantLore(result);
        event.setResult(result);
        event.getView().setRepairCost(1);
    }

    private boolean isCustomDungeonWeapon(ItemStack item) {
        return customItemService.isCustomDungeonWeapon(item);
    }
}
