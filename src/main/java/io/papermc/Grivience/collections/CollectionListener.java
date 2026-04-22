package io.papermc.Grivience.collections;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Listener for collection tracking.
 * Tracks natural item collections (not player-placed blocks).
 * 
 * Skyblock accurate:
 * - Only natural blocks count (player-placed blocks don't count)
 * - Mob drops count toward Combat collections
 * - Fishing catches count toward Fishing collections
 * - Crop harvesting counts toward Farming collections
 * - Wood chopping counts toward Foraging collections
 * - Mining counts toward Mining collections
 */
public class CollectionListener implements Listener {
    private final GriviencePlugin plugin;
    private final CollectionsManager collectionsManager;

    private static final long PLACED_BLOCKS_SAVE_DELAY_TICKS = 200L;

    private final File placedBlocksFile;
    private final Set<String> placedBlocks = new HashSet<>();
    private int placedBlocksSaveTaskId = -1;

    // Crops that count as farming
    private static final Set<Material> FARMING_CROPS = EnumSet.of(
        Material.WHEAT, Material.CARROTS, Material.POTATOES,
        Material.BEETROOTS, Material.NETHER_WART,
        Material.COCOA, Material.SWEET_BERRY_BUSH,
        Material.PITCHER_CROP, Material.TORCHFLOWER_CROP
    );

    // Natural ores (not player-placed)
    private static final Set<Material> MINING_ORES = EnumSet.of(
        Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
        Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
        Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
        Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
        Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
        Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
        Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
        Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
        Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE,
        Material.ANCIENT_DEBRIS,
        Material.GILDED_BLACKSTONE,
        Material.RAW_IRON_BLOCK, Material.RAW_GOLD_BLOCK, Material.RAW_COPPER_BLOCK
    );

    // Natural stone types
    private static final Set<Material> MINING_STONE = EnumSet.of(
        Material.STONE, Material.COBBLESTONE, Material.GRAVEL,
        Material.SAND, Material.RED_SAND, Material.SANDSTONE,
        Material.END_STONE, Material.NETHERRACK,
        Material.BASALT, Material.BLACKSTONE,
        Material.DEEPSLATE, Material.TUFF,
        Material.CALCITE, Material.SMOOTH_BASALT,
        Material.DRIPSTONE_BLOCK, Material.POINTED_DRIPSTONE,
        Material.AMETHYST_BLOCK, Material.BUDDING_AMETHYST
    );

    // Logs that count as foraging
    private static final Set<Material> FORAGING_LOGS = EnumSet.of(
        Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG,
        Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG,
        Material.MANGROVE_LOG, Material.CHERRY_LOG,
        Material.CRIMSON_STEM, Material.WARPED_HYPHAE,
        Material.OAK_WOOD, Material.SPRUCE_WOOD, Material.BIRCH_WOOD,
        Material.JUNGLE_WOOD, Material.ACACIA_WOOD, Material.DARK_OAK_WOOD,
        Material.MANGROVE_WOOD, Material.CHERRY_WOOD,
        Material.CRIMSON_HYPHAE, Material.WARPED_HYPHAE,
        Material.STRIPPED_OAK_LOG, Material.STRIPPED_SPRUCE_LOG,
        Material.STRIPPED_BIRCH_LOG, Material.STRIPPED_JUNGLE_LOG,
        Material.STRIPPED_ACACIA_LOG, Material.STRIPPED_DARK_OAK_LOG,
        Material.STRIPPED_MANGROVE_LOG, Material.STRIPPED_CHERRY_LOG,
        Material.STRIPPED_CRIMSON_STEM, Material.STRIPPED_WARPED_HYPHAE,
        Material.STRIPPED_OAK_WOOD, Material.STRIPPED_SPRUCE_WOOD,
        Material.STRIPPED_BIRCH_WOOD, Material.STRIPPED_JUNGLE_WOOD,
        Material.STRIPPED_ACACIA_WOOD, Material.STRIPPED_DARK_OAK_WOOD,
        Material.STRIPPED_MANGROVE_WOOD, Material.STRIPPED_CHERRY_WOOD,
        Material.STRIPPED_CRIMSON_HYPHAE, Material.STRIPPED_WARPED_HYPHAE
    );

    public CollectionListener(GriviencePlugin plugin, CollectionsManager collectionsManager) {
        this.plugin = plugin;
        this.collectionsManager = collectionsManager;
        this.placedBlocksFile = new File(plugin.getDataFolder(), "collections" + File.separator + "placed_blocks.yml");
        loadPlacedBlocks();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (block == null) {
            return;
        }

        if (shouldTrackPlaced(block.getType())) {
            trackPlaced(block);
        }
    }

    /**
     * Handle block breaking for Mining, Foraging, and Farming collections.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material type = block.getType();

        // Crops only count when mature; crops still count even if player-planted.
        if (FARMING_CROPS.contains(type)) {
            if (block.getBlockData() instanceof Ageable ageable && ageable.getAge() < ageable.getMaximumAge()) {
                return;
            }
            collectBlockDrops(player, block);
            return;
        }

        boolean enforceNoPlaced = shouldTrackPlaced(type);

        // Prevent place/break looping for mining/foraging-style collections.
        boolean wasPlaced = untrackIfPlaced(block);
        if (wasPlaced && enforceNoPlaced) {
            return;
        }

        // Track common Skyblock collection sources.
        if (MINING_ORES.contains(type)
                || MINING_STONE.contains(type)
                || FORAGING_LOGS.contains(type)
                || type == Material.SUGAR_CANE
                || type == Material.CACTUS
                || type == Material.BAMBOO
                || type == Material.MELON
                || type == Material.PUMPKIN
                || type == Material.BROWN_MUSHROOM
                || type == Material.RED_MUSHROOM
                || type == Material.VINE
                || type == Material.TWISTING_VINES
                || type == Material.WEEPING_VINES
                || type == Material.LILY_PAD
                || type == Material.CHORUS_PLANT
                || type == Material.CHORUS_FLOWER) {
            collectBlockDrops(player, block);
        }
    }

    /**
     * Handle mob deaths for Combat collections.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player player = entity == null ? null : entity.getKiller();
        if (player == null) {
            return;
        }
        
        for (ItemStack drop : event.getDrops()) {
            if (drop == null || drop.getType() == Material.AIR) continue;

            collectionsManager.addCollectionFromDrop(player, drop);
        }
    }

    /**
     * Handle fishing for Fishing collections.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        
        if (event.getCaught() instanceof Item caughtItem) {
            ItemStack stack = caughtItem.getItemStack();
            if (stack == null || stack.getType() == Material.AIR) return;

            collectionsManager.addCollectionFromDrop(player, stack);
        }
    }

    /**
     * Handle item consumption (for certain collections).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null || item.getType() == Material.AIR) return;
        
        // Some collections track consumption
        String itemId = item.getType().name().toLowerCase();
        // Note: We don't add to collection here as consumption isn't "collection"
    }

    public void shutdown() {
        savePlacedBlocks();
    }

    private void collectBlockDrops(Player player, Block block) {
        if (player == null || block == null) {
            return;
        }

        Collection<ItemStack> drops = resolveBreakDrops(block, player);
        for (ItemStack drop : drops) {
            if (drop == null || drop.getType() == Material.AIR) {
                continue;
            }
            collectionsManager.addCollectionFromDrop(player, drop);
        }
    }

    private Collection<ItemStack> resolveBreakDrops(Block block, Player player) {
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

    private boolean shouldTrackPlaced(Material type) {
        if (type == null) {
            return false;
        }

        // Skyblock behavior: prevent place/break collection farming for Mining/Foraging blocks,
        // but allow player-planted farming sources to count.
        return MINING_ORES.contains(type)
                || MINING_STONE.contains(type)
                || FORAGING_LOGS.contains(type);
    }

    private String placedKey(Block block) {
        return block.getWorld().getUID() + ":" + block.getX() + "," + block.getY() + "," + block.getZ();
    }

    private void trackPlaced(Block block) {
        if (block == null) {
            return;
        }
        if (placedBlocks.add(placedKey(block))) {
            queuePlacedBlocksSave();
        }
    }

    /**
     * @return true if this block was player-placed and has been untracked.
     */
    private boolean untrackIfPlaced(Block block) {
        if (block == null) {
            return false;
        }
        boolean removed = placedBlocks.remove(placedKey(block));
        if (removed) {
            queuePlacedBlocksSave();
        }
        return removed;
    }

    private void loadPlacedBlocks() {
        placedBlocks.clear();
        if (placedBlocksFile == null || !placedBlocksFile.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(placedBlocksFile);
        List<String> entries = yaml.getStringList("placed-blocks");
        if (entries == null || entries.isEmpty()) {
            return;
        }

        for (String entry : entries) {
            if (entry != null && !entry.isBlank()) {
                placedBlocks.add(entry.trim());
            }
        }
    }

    private void savePlacedBlocks() {
        if (placedBlocksFile == null) {
            return;
        }

        File parent = placedBlocksFile.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }

        YamlConfiguration yaml = new YamlConfiguration();
        List<String> entries = new ArrayList<>(placedBlocks);
        entries.sort(String::compareTo);
        yaml.set("placed-blocks", entries);

        try {
            yaml.save(placedBlocksFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save placed blocks: " + e.getMessage());
        }
    }

    private void queuePlacedBlocksSave() {
        if (placedBlocksSaveTaskId != -1) {
            return;
        }

        placedBlocksSaveTaskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            placedBlocksSaveTaskId = -1;
            savePlacedBlocks();
        }, PLACED_BLOCKS_SAVE_DELAY_TICKS).getTaskId();
    }
}

