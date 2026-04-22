package io.papermc.Grivience.listener;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.item.RaijinCraftingItemType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class GodPotionListener implements Listener {
    private final GriviencePlugin plugin;
    private final CustomItemService itemService;

    public GodPotionListener(GriviencePlugin plugin, CustomItemService itemService) {
        this.plugin = plugin;
        this.itemService = itemService;
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (isGodPotion(item)) {
            applyGodPotion(event.getPlayer());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item != null && item.getType() == Material.DRAGON_BREATH && isGodPotion(item)) {
                applyGodPotion(event.getPlayer());
                item.setAmount(item.getAmount() - 1);
                event.setCancelled(true);
            }
        }
    }

    private boolean isGodPotion(ItemStack item) {
        String id = itemService.itemId(item);
        return "GOD_POTION".equals(id);
    }

    private void applyGodPotion(Player player) {
        // 24 hours in ticks = 24 * 60 * 60 * 20 = 1,728,000
        int duration = 1728000;

        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 6));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 4));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, duration, 3));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, 2));
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, duration, 3));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, 3));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, duration, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, duration, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, duration, 0));

        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "GOD POTION! " + ChatColor.YELLOW + "You feel the power of the gods coursing through your veins for 24 hours!");
        player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.2f);
    }
}
