package io.papermc.Grivience.listener;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.stats.SkyblockCombatEngine;
import io.papermc.Grivience.util.DropDeliveryUtil;
import io.papermc.Grivience.util.FarmingSetBonusUtil;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Applies Skyblock-style Farming Fortune to crop drops.
 *
 * Hypixel-esque model:
 * - Extra crops = baseCropDrops * (FarmingFortune / 100)
 * - Fractional extra is rolled as chance.
 *
 * This listener only boosts the primary crop drop (e.g. Wheat from WHEAT, not seeds).
 */
public final class FarmingFortuneListener implements Listener {
    private final GriviencePlugin plugin;

    public FarmingFortuneListener(GriviencePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        Material blockType = event.getBlockState().getType();
        Material primaryDrop = primaryDropFor(blockType);
        if (primaryDrop == null) {
            return;
        }

        // Ageable crops only get boosted when fully grown.
        if (event.getBlockState().getBlockData() instanceof Ageable ageable && ageable.getAge() < ageable.getMaximumAge()) {
            return;
        }

        double farmingFortune = resolveFarmingFortune(player);
        if (!Double.isFinite(farmingFortune) || farmingFortune <= 0.0D) {
            return;
        }

        List<Item> items = event.getItems();
        if (items == null || items.isEmpty()) {
            return;
        }

        int baseDrops = countMaterial(items, primaryDrop);
        if (baseDrops <= 0) {
            return;
        }

        int extra = computeExtraDrops(baseDrops, farmingFortune);
        extra += computeHarvesterEmbraceExtraDrops(player, baseDrops);
        if (extra <= 0) {
            return;
        }

        addToDropEntities(event, primaryDrop, extra);
    }

    private double resolveFarmingFortune(Player player) {
        if (plugin == null) {
            return 0.0D;
        }
        SkyblockCombatEngine engine = plugin.getSkyblockCombatEngine();
        if (engine == null) {
            // Fallback: base fortune (level + skill) plus vanilla Fortune-enchant contribution.
            if (plugin.getSkyblockStatsManager() == null) {
                return regularFortuneFarmingBonus(player);
            }
            return Math.max(0.0D, plugin.getSkyblockStatsManager().getFarmingFortune(player))
                    + regularFortuneFarmingBonus(player);
        }
        return Math.max(0.0D, engine.stats(player).farmingFortune());
    }

    private double regularFortuneFarmingBonus(Player player) {
        if (player == null || plugin == null) {
            return 0.0D;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) {
            return 0.0D;
        }
        int fortuneLevel = held.getEnchantmentLevel(Enchantment.FORTUNE);
        if (fortuneLevel <= 0) {
            return 0.0D;
        }

        double perLevel = plugin.getConfig().getDouble("skyblock-combat.regular-fortune-farming-fortune-per-level", 15.0D);
        if (!Double.isFinite(perLevel) || perLevel <= 0.0D) {
            return 0.0D;
        }
        return fortuneLevel * perLevel;
    }

    private Material primaryDropFor(Material cropBlock) {
        if (cropBlock == null) {
            return null;
        }
        return switch (cropBlock) {
            case WHEAT -> Material.WHEAT;
            case CARROTS -> Material.CARROT;
            case POTATOES -> Material.POTATO;
            case BEETROOTS -> Material.BEETROOT;
            case NETHER_WART -> Material.NETHER_WART;
            case COCOA -> Material.COCOA_BEANS;
            case SUGAR_CANE -> Material.SUGAR_CANE;
            case CACTUS -> Material.CACTUS;
            case BAMBOO -> Material.BAMBOO;
            case SWEET_BERRY_BUSH -> Material.SWEET_BERRIES;
            case MELON -> Material.MELON_SLICE;
            case PUMPKIN -> Material.PUMPKIN;
            default -> null;
        };
    }

    private int countMaterial(List<Item> items, Material material) {
        int total = 0;
        for (Item item : items) {
            if (item == null) {
                continue;
            }
            ItemStack stack = item.getItemStack();
            if (stack == null || stack.getType() != material) {
                continue;
            }
            total += Math.max(0, stack.getAmount());
        }
        return total;
    }

    private int computeExtraDrops(int baseDrops, double farmingFortune) {
        // extra = base * fortune/100
        double expected = baseDrops * (farmingFortune / 100.0D);
        if (!Double.isFinite(expected) || expected <= 0.0D) {
            return 0;
        }
        int guaranteed = (int) Math.floor(expected);
        double fractional = expected - guaranteed;
        if (fractional > 0.0D && ThreadLocalRandom.current().nextDouble() < fractional) {
            guaranteed += 1;
        }
        return Math.max(0, guaranteed);
    }

    private int computeHarvesterEmbraceExtraDrops(Player player, int baseDrops) {
        if (plugin == null || plugin.getCustomArmorManager() == null) {
            return 0;
        }
        int equippedPieces = plugin.getCustomArmorManager().countEquippedPieces(player, FarmingSetBonusUtil.HARVESTER_EMBRACE_SET_ID);
        return FarmingSetBonusUtil.computeHarvesterEmbraceExtraDrops(equippedPieces, baseDrops);
    }

    private void addToDropEntities(BlockDropItemEvent event, Material material, int extra) {
        if (event == null || material == null || extra <= 0) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        if (plugin.getFarmingContestManager() != null) {
            plugin.getFarmingContestManager().recordHarvest(player, new ItemStack(material, extra));
        }
        DropDeliveryUtil.giveToInventoryOrDrop(
                player,
                new ItemStack(material, extra),
                event.getBlock().getLocation().add(0.5D, 0.5D, 0.5D),
                true
        );
    }
}
