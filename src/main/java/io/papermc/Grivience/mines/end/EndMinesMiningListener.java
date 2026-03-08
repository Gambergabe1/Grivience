package io.papermc.Grivience.mines.end;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.collections.CollectionsManager;
import io.papermc.Grivience.item.CustomArmorManager;
import io.papermc.Grivience.item.CustomArmorManager.CustomArmorSet;
import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.mines.MiningEventManager;
import io.papermc.Grivience.mines.MiningSystemManager;
import io.papermc.Grivience.item.EndMinesMaterialType;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * Adds End Mines-specific drops and block regeneration for a Skyblock-like mining loop.
 */
public final class EndMinesMiningListener implements Listener {
    private final GriviencePlugin plugin;
    private final EndMinesManager endMinesManager;
    private final CustomItemService customItemService;
    private final CollectionsManager collectionsManager;
    private final CustomArmorManager armorManager;
    private final MiningSystemManager miningSystemManager;
    private final MiningEventManager miningEventManager;
    private final NamespacedKey customItemIdKey;
    private final Random random = new Random();

    private record DropSpec(EndMinesMaterialType material, double chance, int min, int max, boolean rare) {
    }

    private final Map<Material, DropSpec[]> dropsByBlock = new EnumMap<>(Material.class);

    public EndMinesMiningListener(
            GriviencePlugin plugin,
            EndMinesManager endMinesManager,
            CustomItemService customItemService,
            CollectionsManager collectionsManager,
            CustomArmorManager armorManager,
            MiningSystemManager miningSystemManager,
            MiningEventManager miningEventManager
    ) {
        this.plugin = plugin;
        this.endMinesManager = endMinesManager;
        this.customItemService = customItemService;
        this.collectionsManager = collectionsManager;
        this.armorManager = armorManager;
        this.miningSystemManager = miningSystemManager;
        this.miningEventManager = miningEventManager;
        this.customItemIdKey = new NamespacedKey(plugin, "custom-item-id");
        seedDefaultDrops();
    }

    private void seedDefaultDrops() {
        // Common End stone
        dropsByBlock.put(Material.END_STONE, new DropSpec[]{
                new DropSpec(EndMinesMaterialType.ENDSTONE_SHARD, 0.30D, 1, 2, false),
                new DropSpec(EndMinesMaterialType.RIFT_ESSENCE, 0.04D, 1, 1, true)
        });
        dropsByBlock.put(Material.END_STONE_BRICKS, dropsByBlock.get(Material.END_STONE));
        dropsByBlock.put(Material.PURPUR_BLOCK, new DropSpec[]{
                new DropSpec(EndMinesMaterialType.ENDSTONE_SHARD, 0.18D, 1, 2, false),
                new DropSpec(EndMinesMaterialType.RIFT_ESSENCE, 0.06D, 1, 1, true)
        });
        dropsByBlock.put(Material.PURPUR_PILLAR, dropsByBlock.get(Material.PURPUR_BLOCK));

        // Obsidian vein
        dropsByBlock.put(Material.OBSIDIAN, new DropSpec[]{
                new DropSpec(EndMinesMaterialType.OBSIDIAN_CORE, 0.06D, 1, 1, true),
                new DropSpec(EndMinesMaterialType.VOID_CRYSTAL, 0.008D, 1, 1, true)
        });
        dropsByBlock.put(Material.CRYING_OBSIDIAN, new DropSpec[]{
                new DropSpec(EndMinesMaterialType.OBSIDIAN_CORE, 0.09D, 1, 1, true),
                new DropSpec(EndMinesMaterialType.VOID_CRYSTAL, 0.012D, 1, 1, true)
        });

        // Crystal vein
        dropsByBlock.put(Material.AMETHYST_BLOCK, new DropSpec[]{
                new DropSpec(EndMinesMaterialType.VOID_CRYSTAL, 0.05D, 1, 1, true),
                new DropSpec(EndMinesMaterialType.RIFT_ESSENCE, 0.04D, 1, 1, true)
        });
        dropsByBlock.put(Material.BUDDING_AMETHYST, dropsByBlock.get(Material.AMETHYST_BLOCK));

        // Chorus grove
        dropsByBlock.put(Material.CHORUS_PLANT, new DropSpec[]{
                new DropSpec(EndMinesMaterialType.CHORUS_WEAVE, 0.06D, 1, 1, true)
        });
        dropsByBlock.put(Material.CHORUS_FLOWER, dropsByBlock.get(Material.CHORUS_PLANT));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (endMinesManager == null || !endMinesManager.isEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        
        // Deep Pressure Penalty
        double penalty = miningSystemManager.getDeepPressurePenalty(player);
        if (penalty > 0 && !hasDeepcoreFullSet(player)) {
            // Apply Slowness to simulate mining speed reduction (Mining Fatigue is better but more intrusive)
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 40, (int)(penalty * 5)));
        }

        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (player.hasPermission("grivience.admin") || player.hasPermission("grivience.endmines.build")) {
            return; // admins can build without regen/drop noise
        }

        Block block = event.getBlock();
        World world = block.getWorld();
        if (world == null || endMinesManager.getWorld() == null || !world.equals(endMinesManager.getWorld())) {
            return;
        }

        Material type = block.getType();
        DropSpec[] specs = dropsByBlock.get(type);
        if (specs == null || specs.length == 0 || customItemService == null) {
            maybeRegen(block, type, block.getBlockData());
            return;
        }

        double bonus = rareDropBonusMultiplier(player);
        
        // Rich Vein Bonus
        Material richVein = miningSystemManager.getCurrentRichVein();
        int dropMultiplier = (richVein != null && richVein == type) ? 2 : 1;
        
        // Event XP/Drop Multipliers (placeholder for XP boost)
        double xpMultiplier = miningEventManager.getXpMultiplier();
        if (plugin.getSkyblockLevelManager() != null) {
            long baseActions = 1;
            long bonusActions = 0;
            if (xpMultiplier > 1.0) {
                // If xpMultiplier is 1.25, it means +25%. 
                // In Skyblock terms, this usually applies to the skill actions or total XP.
                // Here we apply it as a chance for a bonus action.
                double bonusChance = xpMultiplier - 1.0;
                if (random.nextDouble() < bonusChance) {
                    bonusActions = 1;
                }
            }
            plugin.getSkyblockLevelManager().addMiningActions(player, baseActions + bonusActions);
        }
        
        // Ore Streak
        miningSystemManager.incrementStreak(player);
        miningEventManager.registerMine(player);
        
        // Reinforced Ores (10% chance base, 3x more in breach)
        double reinforcedChance = miningEventManager.isBreachActive(null) ? 0.30 : 0.10;
        boolean reinforced = random.nextDouble() < reinforcedChance;
        if (reinforced) {
            miningEventManager.addStability(1);
            if (hasDeepcoreFullSet(player) && random.nextDouble() < 0.20) {
                player.sendMessage(ChatColor.DARK_PURPLE + "Deepcore Set: Instant break on Reinforced Ore!");
                dropMultiplier *= 2;
            } else {
                player.sendMessage(ChatColor.GOLD + "You struck a Reinforced Ore! Bonus drops granted.");
                dropMultiplier *= 2;
            }
        }

        Location dropLoc = block.getLocation().add(0.5, 0.5, 0.5);
        for (DropSpec spec : specs) {
            double chance = spec.chance();
            if (spec.rare()) {
                chance *= bonus;
            }
            if (random.nextDouble() > chance) {
                continue;
            }

            ItemStack item = customItemService.createEndMinesMaterial(spec.material());
            if (item == null) {
                continue;
            }
            int amount = rollAmount(spec.min(), spec.max()) * dropMultiplier;
            
            // Miner Set Bonus (chance to double drops)
            if (hasMinerFullSet(player) && random.nextDouble() < 0.10) {
                amount *= 2;
                player.sendMessage(ChatColor.YELLOW + "Miner's Set Bonus: Double Drops!");
            }
            
            item.setAmount(amount);
            world.dropItemNaturally(dropLoc, item);
            trackCollection(player, item);

            if (spec.rare()) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6F, 1.6F);
                player.sendMessage(ChatColor.LIGHT_PURPLE + "RARE DROP! " + ChatColor.AQUA + itemName(item));
            }
        }

        maybeRegen(block, type, block.getBlockData());
    }

    private void maybeRegen(Block block, Material originalType, BlockData originalData) {
        if (!plugin.getConfig().getBoolean("end-mines.mining.regen.enabled", true)) {
            return;
        }
        int delay = Math.max(20, plugin.getConfig().getInt("end-mines.mining.regen.delay-ticks", 60));

        Location loc = block.getLocation();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Block current = loc.getBlock();
            // Only regen if the spot is still empty (avoid overwriting admin edits).
            if (current.getType() != Material.AIR) {
                return;
            }
            current.setType(originalType, false);
            if (originalData != null) {
                try {
                    current.setBlockData(originalData, false);
                } catch (IllegalArgumentException ignored) {
                    // Some blocks may fail to re-apply data across versions.
                }
            }
        }, delay);
    }

    private int rollAmount(int min, int max) {
        if (max <= min) {
            return Math.max(1, min);
        }
        return min + random.nextInt(max - min + 1);
    }

    private void trackCollection(Player player, ItemStack item) {
        if (collectionsManager == null || player == null || item == null) {
            return;
        }
        String id = null;
        if (item.hasItemMeta()) {
            id = item.getItemMeta().getPersistentDataContainer().get(customItemIdKey, PersistentDataType.STRING);
        }
        if (id == null || id.isBlank()) {
            id = item.getType().name();
        }
        collectionsManager.addCollection(player, id, item.getAmount());
    }

    private String itemName(ItemStack item) {
        if (item == null) {
            return "Item";
        }
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().name();
    }

    private double rareDropBonusMultiplier(Player player) {
        double multiplier = 1.0D;
        multiplier *= miningEventManager.getRareDropMultiplier();
        // Future-proof hook: a dedicated End miner set can boost rare drops.
        if (armorManager == null || player == null) {
            return multiplier;
        }
        CustomArmorSet set = armorManager.getArmorSet("void_miner");
        if (set == null) {
            return 1.0D;
        }
        int pieces = 0;
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            if (piece == null) continue;
            String setId = armorManager.getArmorSetId(piece);
            if (setId != null && setId.equalsIgnoreCase("void_miner")) {
                pieces++;
            }
        }
        if (pieces >= 4) {
            return 1.35D;
        }
        if (pieces >= 2) {
            return 1.15D;
        }
        return 1.0D;
    }

    private boolean hasMinerFullSet(Player player) {
        if (armorManager == null || player == null) return false;
        int pieces = 0;
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            if (piece == null) continue;
            String setId = armorManager.getArmorSetId(piece);
            if ("miner".equalsIgnoreCase(setId)) pieces++;
        }
        return pieces >= 4;
    }

    private boolean hasDeepcoreFullSet(Player player) {
        if (armorManager == null || player == null) return false;
        int pieces = 0;
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            if (piece == null) continue;
            String setId = armorManager.getArmorSetId(piece);
            if ("deepcore".equalsIgnoreCase(setId)) pieces++;
        }
        return pieces >= 4;
    }
}

