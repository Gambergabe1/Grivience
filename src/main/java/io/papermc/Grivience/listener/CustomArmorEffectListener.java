package io.papermc.Grivience.listener;

import io.papermc.Grivience.item.ArmorSetType;
import io.papermc.Grivience.item.CustomArmorType;
import io.papermc.Grivience.item.CustomItemService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;

public final class CustomArmorEffectListener {
    private static final int EFFECT_TICKS = 60;

    private final JavaPlugin plugin;
    private final CustomItemService customItemService;
    private BukkitTask tickTask;

    public CustomArmorEffectListener(JavaPlugin plugin, CustomItemService customItemService) {
        this.plugin = plugin;
        this.customItemService = customItemService;
        startTicker();
    }

    private void startTicker() {
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                applyArmorBuffs(player);
            }
        }, 20L, 20L);
    }

    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
    }

    private void applyArmorBuffs(Player player) {
        PlayerInventory inventory = player.getInventory();
        int defense = 0;
        int health = 0;
        int healSpeed = 0;
        EnumMap<ArmorSetType, Integer> pieceCounts = new EnumMap<>(ArmorSetType.class);

        for (ItemStack piece : inventory.getArmorContents()) {
            CustomArmorType armorType = armorTypeOf(piece);
            if (armorType == null) {
                continue;
            }
            defense += armorType.defense();
            health += armorType.health();
            healSpeed += armorType.healSpeed();
            pieceCounts.merge(armorType.setType(), 1, Integer::sum);
        }

        if (defense > 0) {
            int amp = Math.min(2, Math.max(0, defense / 55));
            applyIfStronger(player, PotionEffectType.RESISTANCE, amp);
        }
        if (health > 0) {
            int amp = Math.min(4, Math.max(0, health / 50));
            applyIfStronger(player, PotionEffectType.HEALTH_BOOST, amp);
        }
        if (healSpeed > 0) {
            int amp = Math.min(2, Math.max(0, healSpeed / 8));
            applyIfStronger(player, PotionEffectType.REGENERATION, amp);
        }

        if (pieceCounts.getOrDefault(ArmorSetType.SHOGUN, 0) >= 4) {
            applyIfStronger(player, PotionEffectType.RESISTANCE, 2);
            applyIfStronger(player, PotionEffectType.STRENGTH, 0);
        }
        if (pieceCounts.getOrDefault(ArmorSetType.SHINOBI, 0) >= 4) {
            applyIfStronger(player, PotionEffectType.SPEED, 2);
            applyIfStronger(player, PotionEffectType.JUMP_BOOST, 1);
        }
        if (pieceCounts.getOrDefault(ArmorSetType.ONMYOJI, 0) >= 4) {
            applyIfStronger(player, PotionEffectType.REGENERATION, 2);
            applyIfStronger(player, PotionEffectType.ABSORPTION, 1);
        }
    }

    private void applyIfStronger(Player player, PotionEffectType type, int amplifier) {
        PotionEffect current = player.getPotionEffect(type);
        if (current != null && current.getAmplifier() > amplifier && current.getDuration() > EFFECT_TICKS) {
            return;
        }
        player.addPotionEffect(new PotionEffect(type, EFFECT_TICKS, amplifier, true, false, true));
    }

    private CustomArmorType armorTypeOf(ItemStack item) {
        String itemId = customItemService.itemId(item);
        return CustomArmorType.parse(itemId);
    }
}
