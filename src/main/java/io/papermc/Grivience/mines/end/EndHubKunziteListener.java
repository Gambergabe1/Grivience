package io.papermc.Grivience.mines.end;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.collections.CollectionsManager;
import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.item.EndMinesMaterialType;
import io.papermc.Grivience.util.DropDeliveryUtil;
import io.papermc.Grivience.util.RegenPlaceholderUtil;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Treats configured pink blocks in the End Hub as Kunzite nodes with timed respawn.
 */
public final class EndHubKunziteListener implements Listener {
    private final GriviencePlugin plugin;
    private final CustomItemService customItemService;
    private final CollectionsManager collectionsManager;

    public EndHubKunziteListener(GriviencePlugin plugin, CustomItemService customItemService, CollectionsManager collectionsManager) {
        this.plugin = plugin;
        this.customItemService = customItemService;
        this.collectionsManager = collectionsManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!isEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (canBypassAsBuilder(player)) {
            return;
        }

        Block block = event.getBlock();
        if (!KunziteNodeUtil.isConfiguredKunziteNode(plugin.getConfig(), block)) {
            return;
        }

        Material originalType = block.getType();
        BlockData originalData = block.getBlockData();
        event.setCancelled(true);
        event.setDropItems(false);
        event.setExpToDrop(0);
        Material placeholder = regenPlaceholder();
        block.setType(placeholder.isAir() ? Material.AIR : placeholder, false);

        HeartOfTheEndMinesManager.MiningBonus miningBonus = HeartOfTheEndMinesManager.MiningBonus.none();
        HeartOfTheEndMinesManager heartManager = plugin.getHeartOfTheEndMinesManager();
        if (heartManager != null) {
            miningBonus = heartManager.recordMining(player, HeartOfTheEndMinesManager.MiningSource.KUNZITE);
        }

        if (plugin.getSkyblockLevelManager() != null) {
            plugin.getSkyblockLevelManager().addMiningActions(player, 1L);
            if (plugin.getSkyblockLevelManager().getSkillManager() != null) {
                // Kunzite is a rare end mineral, matching high-tier XP
                plugin.getSkyblockLevelManager().getSkillManager().addXp(player, io.papermc.Grivience.skills.SkyblockSkill.MINING, 15L);
            }
        }

        int amount = rollDropAmount() + miningBonus.bonusDrops();
        ItemStack kunzite = customItemService == null ? null : customItemService.createEndMinesMaterial(EndMinesMaterialType.KUNZITE);
        if (kunzite != null) {
            kunzite.setAmount(Math.max(1, amount));
            Location dropLoc = block.getLocation().add(0.5D, 0.5D, 0.5D);
            DropDeliveryUtil.giveToInventoryOrDrop(player, kunzite, dropLoc);
            if (collectionsManager != null) {
                String itemId = customItemService.itemId(kunzite);
                if (itemId != null && !itemId.isBlank()) {
                    collectionsManager.addCollection(player, itemId, kunzite.getAmount());
                }
            }
        }

        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.7F, 1.5F);
        scheduleRespawn(block, originalType, originalData, placeholder);
    }

    private boolean canBypassAsBuilder(Player player) {
        if (player == null) {
            return false;
        }
        if (!player.isSneaking()) {
            return false;
        }
        return player.hasPermission("grivience.admin") || player.hasPermission("grivience.hub.build");
    }

    private boolean isEnabled() {
        return KunziteNodeUtil.isEnabled(plugin.getConfig());
    }

    private int rollDropAmount() {
        int min = Math.max(1, plugin.getConfig().getInt("end-hub.kunzite.drop-min", 1));
        int max = Math.max(min, plugin.getConfig().getInt("end-hub.kunzite.drop-max", 2));
        if (max <= min) {
            return min;
        }
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private Material regenPlaceholder() {
        return RegenPlaceholderUtil.resolvePlaceholder(plugin.getConfig(), "end-hub.kunzite.regen.placeholder-block", null);
    }

    private void scheduleRespawn(Block block, Material originalType, BlockData originalData, Material placeholder) {
        if (!plugin.getConfig().getBoolean("end-hub.kunzite.regen.enabled", true)) {
            return;
        }

        int delay = Math.max(20, plugin.getConfig().getInt("end-hub.kunzite.respawn-delay-ticks", 60));
        Location location = block.getLocation();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Block current = location.getBlock();
            if (!RegenPlaceholderUtil.canRestore(current, placeholder)) {
                return;
            }
            current.setType(originalType, false);
            if (originalData != null) {
                try {
                    current.setBlockData(originalData, false);
                } catch (IllegalArgumentException ignored) {
                    // Keep the restored material even if the stored data is no longer valid.
                }
            }
        }, delay);
    }
}
