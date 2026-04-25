package io.papermc.Grivience.stats;

import io.papermc.Grivience.mines.MiningEventManager;
import io.papermc.Grivience.item.CustomArmorManager;
import io.papermc.Grivience.util.FarmingSetBonusUtil;
import io.papermc.Grivience.util.DropDeliveryUtil;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerFishEvent;

import java.util.Collection;
import java.util.List;

/**
 * Awards Skyblock XP and progression counters for Skyblock-style level tracking.
 */
public final class SkyblockLevelListener implements Listener {
    private static final float XP_DING_VOLUME = 0.35F;
    private static final float XP_DING_PITCH = 1.45F;

    private final SkyblockLevelManager levelManager;
    private final MiningEventManager miningEventManager;

    public SkyblockLevelListener(SkyblockLevelManager levelManager, MiningEventManager miningEventManager) {
        this.levelManager = levelManager;
        this.miningEventManager = miningEventManager;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onMobKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null || isBypassedGameMode(killer) || event.getEntity() instanceof Player) {
            return;
        }
        
        // Sea Creature detection
        boolean isSeaCreature = event.getEntity().getPersistentDataContainer().has(
            new org.bukkit.NamespacedKey(levelManager.getPlugin(), "sea_creature"), org.bukkit.persistence.PersistentDataType.BYTE);
        
        if (isSeaCreature) {
            levelManager.recordFishingCatch(killer); // Killing sea creatures gives Fishing XP
        } else {
            levelManager.recordCombatKill(killer, event.getEntity());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || isBypassedGameMode(player)) {
            return;
        }
        
        ItemStack result = event.getRecipe().getResult();
        if (result != null) {
            levelManager.recordCarpentryCraft(player, result);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        // Alchemy XP when taking potions from brewing stand
        if (event.getInventory().getType() == InventoryType.BREWING && event.getSlot() < 3) {
            if (!(event.getWhoClicked() instanceof Player player) || isBypassedGameMode(player)) return;
            
            ItemStack item = event.getCurrentItem();
            if (item != null && (item.getType() == Material.POTION || item.getType() == Material.SPLASH_POTION || item.getType() == Material.LINGERING_POTION)) {
                levelManager.recordAlchemy(player, item.getAmount());
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (isBypassedGameMode(player)) {
            return;
        }

        Material type = event.getBlock().getType();

        if (levelManager.isFarmingMaterial(type)) {
            // Ageable crops only count when mature.
            if (event.getBlock().getBlockData() instanceof Ageable ageable && ageable.getAge() < ageable.getMaximumAge()) {
                return;
            }
            levelManager.recordFarmingHarvest(player, type);
            playXpDing(player);

            // Gilded Harvester Ability: 15% chance for double drops
            if (hasFullSet(player, FarmingSetBonusUtil.GILDED_HARVESTER_SET_ID) && Math.random() < 0.15) {
                int bonusItems = deliverBonusDrops(player, event.getBlock());
                if (bonusItems > 0) {
                    player.sendMessage(ChatColor.GOLD + "\u2618 Bountiful Harvest activated!");
                }
            }
            return;
        }

        if (levelManager.isMiningMaterial(type)) {
            levelManager.recordMiningOre(player, type);
            playXpDing(player);
            
            // Miner Set Ability: 10% chance for double drops
            if (levelManager.hasMinerFullSet(player) && Math.random() < 0.10) {
                int bonusItems = deliverBonusDrops(player, event.getBlock());
                if (bonusItems > 0) {
                    player.sendMessage(ChatColor.GOLD + "\u2726 Double Drop activated!");
                }
            }

            // Integrate with Mining Events (e.g. King's Inspection) if mined in the Mine Hub.
            if (miningEventManager != null) {
                String worldName = event.getBlock().getWorld().getName();
                String mineHubWorld = miningEventManager.getPlugin().getConfig().getString("skyblock.minehub-world", "minehub_world");
                // Allow both the configured world and the explicitly requested 'Minehub' world name.
                if (worldName.equalsIgnoreCase(mineHubWorld) || worldName.equalsIgnoreCase("Minehub")) {
                    miningEventManager.registerMine(player);
                }
            }
            return;
        }

        if (levelManager.isForagingMaterial(type)) {
            levelManager.recordForagingLog(player, type);
        }
    }

    private void playXpDing(Player player) {
        if (player == null) {
            return;
        }
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, XP_DING_VOLUME, XP_DING_PITCH);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null || isBypassedGameMode(player)) {
            return;
        }
        levelManager.recordFishingCatch(player);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeepcoreInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.LEFT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        
        Player player = event.getPlayer();
        if (isBypassedGameMode(player)) return;
        
        if (levelManager.hasDeepcoreFullSet(player)) {
            Material type = event.getClickedBlock().getType();
            if (levelManager.isMiningMaterial(type)) {
                // Instantly break the block
                player.breakBlock(event.getClickedBlock());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFarmingInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        if (isBypassedGameMode(player)) return;

        Material type = event.getClickedBlock().getType();
        if (type == Material.SWEET_BERRY_BUSH || type == Material.COCOA) {
            if (event.getClickedBlock().getBlockData() instanceof Ageable ageable && ageable.getAge() >= ageable.getMaximumAge()) {
                levelManager.recordFarmingHarvest(player, type);
            }
        }
    }

    private int deliverBonusDrops(Player player, Block block) {
        if (player == null || block == null) {
            return 0;
        }

        Collection<ItemStack> drops = resolveBreakDrops(player, block);
        if (drops == null || drops.isEmpty()) {
            return 0;
        }

        int delivered = 0;
        for (ItemStack drop : drops) {
            if (drop == null || drop.getType().isAir() || drop.getAmount() <= 0) {
                continue;
            }
            delivered += drop.getAmount();
            if (levelManager.getPlugin() instanceof io.papermc.Grivience.GriviencePlugin grivience
                    && grivience.getFarmingContestManager() != null) {
                grivience.getFarmingContestManager().recordHarvest(player, drop.clone());
            }
            DropDeliveryUtil.giveToInventoryOrDrop(player, drop.clone(), block.getLocation().add(0.5D, 0.5D, 0.5D), true);
        }
        return delivered;
    }

    private Collection<ItemStack> resolveBreakDrops(Player player, Block block) {
        if (block == null) {
            return List.of();
        }
        ItemStack tool = player == null ? null : player.getInventory().getItemInMainHand();
        if (tool == null || tool.getType().isAir()) {
            return block.getDrops();
        }
        Collection<ItemStack> drops = block.getDrops(tool, player);
        if (drops == null || drops.isEmpty()) {
            return block.getDrops();
        }
        return drops;
    }

    private boolean hasFullSet(Player player, String setId) {
        if (player == null || setId == null) return false;
        CustomArmorManager armorManager = levelManager.getArmorManager();
        if (armorManager == null) return false;
        return armorManager.hasEquippedPieces(player, setId, 4);
    }

    private boolean isBypassedGameMode(Player player) {
        if (player == null) {
            return true;
        }
        return player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR;
    }
}

