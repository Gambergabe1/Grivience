package io.papermc.Grivience.listener;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.stats.SkyblockCombatEngine;
import io.papermc.Grivience.util.DropDeliveryUtil;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
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
 * Applies Skyblock-style Foraging Fortune to log drops.
 */
public final class ForagingFortuneListener implements Listener {
    private final GriviencePlugin plugin;

    public ForagingFortuneListener(GriviencePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        Material blockType = event.getBlockState().getType();
        if (!isForagingBlock(blockType)) return;

        Material primaryDrop = primaryDropFor(blockType);
        if (primaryDrop == null) return;

        double foragingFortune = resolveForagingFortune(player);
        if (!Double.isFinite(foragingFortune) || foragingFortune <= 0.0D) return;

        List<Item> items = event.getItems();
        if (items == null || items.isEmpty()) return;

        int baseDrops = countMaterial(items, primaryDrop);
        if (baseDrops <= 0) return;

        int extra = computeExtraDrops(baseDrops, foragingFortune);
        if (extra <= 0) return;

        addToDropEntities(event, primaryDrop, extra);
    }

    private boolean isForagingBlock(Material type) {
        String name = type.name();
        return Tag.LOGS.isTagged(type) || name.endsWith("_STEM") || name.endsWith("_HYPHAE");
    }

    private double resolveForagingFortune(Player player) {
        if (plugin == null) return 0.0D;
        SkyblockCombatEngine engine = plugin.getSkyblockCombatEngine();
        if (engine == null) {
            if (plugin.getSkyblockStatsManager() == null) return 0.0D;
            return Math.max(0.0D, plugin.getSkyblockStatsManager().getForagingFortune(player));
        }
        return Math.max(0.0D, engine.stats(player).foragingFortune());
    }

    private Material primaryDropFor(Material logBlock) {
        if (logBlock == null) return null;
        // Most logs drop themselves
        if (Tag.LOGS.isTagged(logBlock) || logBlock.name().endsWith("_STEM") || logBlock.name().endsWith("_HYPHAE")) {
            return logBlock;
        }
        return null;
    }

    private int countMaterial(List<Item> items, Material material) {
        int total = 0;
        for (Item item : items) {
            if (item == null) continue;
            ItemStack stack = item.getItemStack();
            if (stack == null || stack.getType() != material) continue;
            total += Math.max(0, stack.getAmount());
        }
        return total;
    }

    private int computeExtraDrops(int baseDrops, double fortune) {
        double expected = baseDrops * (fortune / 100.0D);
        if (!Double.isFinite(expected) || expected <= 0.0D) return 0;
        int guaranteed = (int) Math.floor(expected);
        double fractional = expected - guaranteed;
        if (fractional > 0.0D && ThreadLocalRandom.current().nextDouble() < fractional) {
            guaranteed += 1;
        }
        return Math.max(0, guaranteed);
    }

    private void addToDropEntities(BlockDropItemEvent event, Material material, int extra) {
        if (event == null || material == null || extra <= 0) return;
        Player player = event.getPlayer();
        if (player == null) return;

        DropDeliveryUtil.giveToInventoryOrDrop(
                player,
                new ItemStack(material, extra),
                event.getBlock().getLocation().add(0.5D, 0.5D, 0.5D),
                true
        );
    }
}
