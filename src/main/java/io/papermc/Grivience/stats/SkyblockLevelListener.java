package io.papermc.Grivience.stats;

import io.papermc.Grivience.mines.MiningEventManager;
import io.papermc.Grivience.item.CustomArmorManager;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
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

/**
 * Awards Skyblock XP and progression counters for Skyblock-style level tracking.
 */
public final class SkyblockLevelListener implements Listener {
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
            levelManager.recordCombatKill(killer, event.getEntityType());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || isBypassedGameMode(player)) {
            return;
        }
        
        ItemStack result = event.getRecipe().getResult();
        if (result != null) {
            levelManager.recordCarpentry(player, result.getAmount());
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

            // Gilded Harvester Ability: 15% chance for double drops
            if (hasFullSet(player, "gilded_harvester") && Math.random() < 0.15) {
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(type, 1));
                player.sendMessage(ChatColor.GOLD + "\u2618 Bountiful Harvest activated!");
            }
            return;
        }

        if (levelManager.isMiningMaterial(type)) {
            levelManager.recordMiningOre(player, type);
            
            // Miner Set Ability: 10% chance for double drops
            if (levelManager.hasMinerFullSet(player) && Math.random() < 0.10) {
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(type));
                player.sendMessage(ChatColor.GOLD + "❂ Double Drop activated!");
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

    private boolean hasFullSet(Player player, String setId) {
        if (player == null || setId == null) return false;
        int pieces = 0;
        CustomArmorManager armorManager = levelManager.getArmorManager();
        if (armorManager == null) return false;
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            if (piece == null) continue;
            String itemSetId = armorManager.getArmorSetId(piece);
            if (setId.equalsIgnoreCase(itemSetId)) pieces++;
        }
        return pieces >= 4;
    }

    private boolean isBypassedGameMode(Player player) {
        if (player == null) {
            return true;
        }
        return player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR;
    }
}
