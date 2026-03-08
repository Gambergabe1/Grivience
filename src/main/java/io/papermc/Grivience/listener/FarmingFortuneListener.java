package io.papermc.Grivience.listener;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.stats.SkyblockCombatEngine;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
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
        if (extra <= 0) {
            return;
        }

        addToDropEntities(event, items, primaryDrop, extra);
    }

    private double resolveFarmingFortune(Player player) {
        if (plugin == null) {
            return 0.0D;
        }
        SkyblockCombatEngine engine = plugin.getSkyblockCombatEngine();
        if (engine == null) {
            // Fallback: base fortune (level + skill). This does not include armor/tool bonuses.
            if (plugin.getSkyblockStatsManager() == null) {
                return 0.0D;
            }
            return Math.max(0.0D, plugin.getSkyblockStatsManager().getFarmingFortune(player));
        }
        return Math.max(0.0D, engine.stats(player).farmingFortune());
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

    private void addToDropEntities(BlockDropItemEvent event, List<Item> existing, Material material, int extra) {
        int remaining = extra;

        // Prefer stacking into existing item entities to avoid spawning a ton of new entities.
        for (Item item : existing) {
            if (remaining <= 0) {
                return;
            }
            if (item == null) {
                continue;
            }
            ItemStack stack = item.getItemStack();
            if (stack == null || stack.getType() != material) {
                continue;
            }

            int maxStack = stack.getMaxStackSize();
            if (maxStack <= 1) {
                continue;
            }
            int canAdd = Math.max(0, maxStack - stack.getAmount());
            if (canAdd <= 0) {
                continue;
            }

            int add = Math.min(canAdd, remaining);
            stack.setAmount(stack.getAmount() + add);
            item.setItemStack(stack);
            remaining -= add;
        }

        while (remaining > 0) {
            int add = Math.min(remaining, material.getMaxStackSize());
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(material, add));
            remaining -= add;
        }
    }
}

