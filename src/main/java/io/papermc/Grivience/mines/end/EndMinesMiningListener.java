package io.papermc.Grivience.mines.end;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.collections.CollectionsManager;
import io.papermc.Grivience.item.CustomArmorManager;
import io.papermc.Grivience.skyblock.island.Island;
import io.papermc.Grivience.item.CustomArmorManager.CustomArmorSet;
import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.mines.MiningEventManager;
import io.papermc.Grivience.mines.MiningSystemManager;
import io.papermc.Grivience.mines.DrillStatProfile;
import io.papermc.Grivience.item.EndMinesMaterialType;
import io.papermc.Grivience.util.DropDeliveryUtil;
import io.papermc.Grivience.util.RegenPlaceholderUtil;
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
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Adds End Mines-specific drops and block regeneration for a Skyblock-like mining loop.
 */
public final class EndMinesMiningListener implements Listener {
    private static final Set<Material> CRYSTAL_NODE_BLOCKS = Set.of(Material.AMETHYST_BLOCK, Material.BUDDING_AMETHYST);

    private final GriviencePlugin plugin;
    private final EndMinesManager endMinesManager;
    private final CustomItemService customItemService;
    private final CollectionsManager collectionsManager;
    private final CustomArmorManager armorManager;
    private final MiningSystemManager miningSystemManager;
    private final MiningEventManager miningEventManager;
    private final NamespacedKey customItemIdKey;
    private final Random random = new Random();
    private final Map<String, NodeStageState> crystalNodeStates = new HashMap<>();
    private final Map<java.util.UUID, Long> toolWarningCooldowns = new HashMap<>();
    private final Set<String> instaBreakCrystalNodes = new HashSet<>();

    private static final class NodeStageState {
        private int remainingHits;
        private long lastHitAt;

        private NodeStageState(int remainingHits, long lastHitAt) {
            this.remainingHits = remainingHits;
            this.lastHitAt = lastHitAt;
        }
    }

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
                new DropSpec(EndMinesMaterialType.VOID_CRYSTAL, 0.18D, 1, 2, true),
                new DropSpec(EndMinesMaterialType.RIFT_ESSENCE, 0.10D, 1, 1, true)
        });
        dropsByBlock.put(Material.BUDDING_AMETHYST, dropsByBlock.get(Material.AMETHYST_BLOCK));

        // Chorus grove
        dropsByBlock.put(Material.CHORUS_PLANT, new DropSpec[]{
                new DropSpec(EndMinesMaterialType.CHORUS_WEAVE, 0.06D, 1, 1, true)
        });
        dropsByBlock.put(Material.CHORUS_FLOWER, dropsByBlock.get(Material.CHORUS_PLANT));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(BlockDamageEvent event) {
        if (endMinesManager == null || !endMinesManager.isEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        if (player == null || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (player.hasPermission("grivience.admin") || player.hasPermission("grivience.endmines.build")) {
            return;
        }

        Block block = event.getBlock();
        if (block == null || block.getWorld() == null || endMinesManager.getWorld() == null || !block.getWorld().equals(endMinesManager.getWorld())) {
            return;
        }

        Material type = block.getType();
        if (!dropsByBlock.containsKey(type)) {
            return;
        }

        if (!canMineBlock(player, type)) {
            event.setCancelled(true);
            return;
        }

        if (!isCrystalNode(type)) {
            return;
        }

        int hitsRequired = requiredCrystalHits(player, type, player.getInventory().getItemInMainHand());
        if (hitsRequired <= 1) {
            return;
        }

        String key = nodeKey(block);
        long now = System.currentTimeMillis();
        long resetWindowMs = Math.max(500L, plugin.getConfig().getLong("end-mines.mining.crystal-nodes.hit-reset-ms", 4000L));
        NodeStageState state = crystalNodeStates.get(key);
        if (state == null || now - state.lastHitAt > resetWindowMs) {
            state = new NodeStageState(hitsRequired, now);
            crystalNodeStates.put(key, state);
        }

        state.lastHitAt = now;
        state.remainingHits = Math.max(0, state.remainingHits - 1);
        if (state.remainingHits > 0) {
            event.setCancelled(true);
            block.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, block.getLocation().add(0.5D, 0.7D, 0.5D), 6, 0.18D, 0.18D, 0.18D, 0.01D);
            block.getWorld().playSound(block.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_HIT, 0.6F, 1.2F);
            player.sendActionBar(ChatColor.LIGHT_PURPLE + "Crystal Stability: " + ChatColor.WHITE + state.remainingHits + ChatColor.GRAY + " hit(s) left");
            return;
        }

        crystalNodeStates.remove(key);
        instaBreakCrystalNodes.add(key);
        event.setInstaBreak(true);
        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.9F, 1.0F);
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
        if (!canMineBlock(player, type)) {
            event.setCancelled(true);
            return;
        }

        String nodeKey = nodeKey(block);
        if (isCrystalNode(type) && requiredCrystalHits(player, type, player.getInventory().getItemInMainHand()) > 1 && !instaBreakCrystalNodes.remove(nodeKey)) {
            event.setCancelled(true);
            return;
        }
        crystalNodeStates.remove(nodeKey);

        DropSpec[] specs = dropsByBlock.get(type);
        HeartOfTheEndMinesManager heartManager = plugin.getHeartOfTheEndMinesManager();
        HeartOfTheEndMinesManager.MiningBonus heartBonus = heartManager == null
                ? HeartOfTheEndMinesManager.MiningBonus.none()
                : heartManager.recordMining(player, HeartOfTheEndMinesManager.MiningSource.END_MINE);
        if (specs == null || specs.length == 0 || customItemService == null) {
            maybeRegen(block, type, block.getBlockData());
            return;
        }

        double bonus = rareDropBonusMultiplier(player) * heartBonus.rareDropMultiplier();
        
        // Rich Vein Bonus
        Material richVein = miningSystemManager.getCurrentRichVein();
        int dropMultiplier = (richVein != null && richVein == type) ? 2 : 1;
        if (isCrystalNode(type)) {
            dropMultiplier *= Math.max(1, plugin.getConfig().getInt("end-mines.mining.crystal-nodes.reward-multiplier", 2));
        }
        
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
            if (!spec.rare()) {
                amount += heartBonus.bonusDrops();
            }
            
            // Miner Set Bonus (chance to double drops)
            if (hasMinerFullSet(player) && random.nextDouble() < 0.10) {
                amount *= 2;
                player.sendMessage(ChatColor.YELLOW + "Miner's Set Bonus: Double Drops!");
            }
            
            item.setAmount(amount);
            DropDeliveryUtil.giveToInventoryOrDrop(player, item, dropLoc);
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
        int delay = regenDelayFor(originalType);
        Material placeholder = regenPlaceholderFor(originalType);

        Location loc = block.getLocation();
        plugin.getServer().getScheduler().runTask(plugin, () -> RegenPlaceholderUtil.placePlaceholder(loc.getBlock(), placeholder));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Block current = loc.getBlock();
            // Only regen if the spot still has the placeholder or remains empty.
            if (!RegenPlaceholderUtil.canRestore(current, placeholder)) {
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

    private boolean canMineBlock(Player player, Material type) {
        if (player == null || type == null) {
            return false;
        }
        if (!plugin.getConfig().getBoolean("end-mines.mining.tool-requirements.enabled", true)) {
            return true;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        int breakingPower = breakingPower(tool);
        boolean validMiningTool = isValidMiningTool(tool);
        int requiredPower = requiredBreakingPower(type);

        if (requiredPower <= 0) {
            return true;
        }
        if (!validMiningTool || breakingPower < requiredPower) {
            warnToolRequirement(player, requiredPower, type);
            return false;
        }
        return true;
    }

    private boolean isValidMiningTool(ItemStack tool) {
        if (tool == null || tool.getType().isAir()) {
            return false;
        }
        String customId = customItemService == null ? null : customItemService.itemId(tool);
        if (customId != null && customId.endsWith("_DRILL")) {
            return true;
        }
        return tool.getType().name().endsWith("_PICKAXE");
    }

    private int breakingPower(ItemStack tool) {
        if (tool == null || tool.getType().isAir()) {
            return 0;
        }
        String customId = customItemService == null ? null : customItemService.itemId(tool);
        if (customId != null) {
            DrillStatProfile.Profile profile = drillProfile(tool, customId);
            if (profile != null) {
                return profile.breakingPower();
            }
            return 0;
        }
        return switch (tool.getType()) {
            case NETHERITE_PICKAXE -> 7;
            case DIAMOND_PICKAXE -> 6;
            case IRON_PICKAXE -> 4;
            case STONE_PICKAXE -> 3;
            case WOODEN_PICKAXE, GOLDEN_PICKAXE -> 2;
            default -> 0;
        };
    }

    private int requiredBreakingPower(Material type) {
        if (type == null) {
            return 0;
        }
        if (isCrystalNode(type)) {
            return Math.max(1, plugin.getConfig().getInt("end-mines.mining.tool-requirements.crystal-breaking-power", 7));
        }
        if (type == Material.OBSIDIAN || type == Material.CRYING_OBSIDIAN) {
            return Math.max(1, plugin.getConfig().getInt("end-mines.mining.tool-requirements.obsidian-breaking-power", 6));
        }
        if (type == Material.END_STONE || type == Material.END_STONE_BRICKS || type == Material.PURPUR_BLOCK || type == Material.PURPUR_PILLAR) {
            return Math.max(1, plugin.getConfig().getInt("end-mines.mining.tool-requirements.end-stone-breaking-power", 3));
        }
        return 0;
    }

    private void warnToolRequirement(Player player, int requiredPower, Material type) {
        long now = System.currentTimeMillis();
        long last = toolWarningCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 1200L) {
            return;
        }
        toolWarningCooldowns.put(player.getUniqueId(), now);
        player.sendActionBar(ChatColor.RED + "Requires Breaking Power " + requiredPower + ChatColor.GRAY + " for " + friendlyName(type));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5F, 0.6F);
    }

    private boolean isCrystalNode(Material type) {
        return type != null && CRYSTAL_NODE_BLOCKS.contains(type);
    }

    private int requiredCrystalHits(Player player, Material type, ItemStack tool) {
        if (!isCrystalNode(type)) {
            return 1;
        }
        int baseHits = type == Material.BUDDING_AMETHYST
                ? Math.max(1, plugin.getConfig().getInt("end-mines.mining.crystal-nodes.budding-hits", 4))
                : Math.max(1, plugin.getConfig().getInt("end-mines.mining.crystal-nodes.amethyst-hits", 3));
        
        // --- PET BONUSES ---
        if (player != null && plugin.getPetManager() != null) {
            double miningSpeed = plugin.getPetManager().getMiningSpeed(player);
            if (miningSpeed > 0) {
                // Reduces hits by 1 every 50 mining speed from pets
                int reduction = (int) (miningSpeed / 50.0);
                baseHits = Math.max(1, baseHits - reduction);
            }
        }
                
        DrillStatProfile.Profile profile = drillProfile(tool, customItemService == null ? null : customItemService.itemId(tool));
        if (profile == null) {
            return baseHits;
        }
        return Math.max(1, baseHits - profile.crystalNodeHitReduction());
    }

    private DrillStatProfile.Profile drillProfile(ItemStack tool, String customId) {
        if (tool == null || tool.getType().isAir() || customItemService == null || !DrillStatProfile.isDrillId(customId) || !tool.hasItemMeta()) {
            return null;
        }
        ItemStack liveTool = tool;
        String engineId = liveTool.getItemMeta().getPersistentDataContainer().get(customItemService.getDrillEngineKey(), PersistentDataType.STRING);
        String tankId = liveTool.getItemMeta().getPersistentDataContainer().get(customItemService.getDrillTankKey(), PersistentDataType.STRING);
        return DrillStatProfile.resolve(customId, engineId, tankId);
    }

    private int regenDelayFor(Material type) {
        if (isCrystalNode(type)) {
            return Math.max(20, plugin.getConfig().getInt("end-mines.mining.crystal-nodes.regen-delay-ticks", 180));
        }
        return Math.max(20, plugin.getConfig().getInt("end-mines.mining.regen.delay-ticks", 60));
    }

    private Material regenPlaceholderFor(Material type) {
        if (isCrystalNode(type)) {
            return RegenPlaceholderUtil.resolvePlaceholder(
                    plugin.getConfig(),
                    "end-mines.mining.crystal-nodes.placeholder-block",
                    "end-mines.mining.regen.placeholder-block"
            );
        }
        return RegenPlaceholderUtil.resolvePlaceholder(plugin.getConfig(), "end-mines.mining.regen.placeholder-block", null);
    }

    private String nodeKey(Block block) {
        Location location = block.getLocation();
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    private String friendlyName(Material type) {
        String raw = type == null ? "block" : type.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
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

        // Island Upgrades
        if (plugin.getIslandManager() != null) {
            Island island = plugin.getIslandManager().getIsland(player.getUniqueId());
            if (island != null) {
                int luckLevel = island.getEndMinesLuckUpgrade();
                if (luckLevel > 0) {
                    multiplier *= (1.0 + (luckLevel * 0.05D)); // +5% per level
                }
            }
        }

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
