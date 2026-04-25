package io.papermc.Grivience.listener;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.enchantment.EnchantmentRegistry;
import io.papermc.Grivience.enchantment.SkyblockEnchantStorage;
import io.papermc.Grivience.enchantment.SkyblockEnchantment;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;

/**
 * Implements the Smelting Touch enchantment.
 * Automatically smelts block drops if the tool has the enchantment.
 */
public final class SmeltingTouchListener implements Listener {
    private final GriviencePlugin plugin;
    private final SkyblockEnchantStorage enchantStorage;
    private static final Map<Material, Material> SMELT_MAP = new EnumMap<>(Material.class);

    static {
        // Ores
        SMELT_MAP.put(Material.RAW_IRON, Material.IRON_INGOT);
        SMELT_MAP.put(Material.IRON_ORE, Material.IRON_INGOT);
        SMELT_MAP.put(Material.DEEPSLATE_IRON_ORE, Material.IRON_INGOT);
        
        SMELT_MAP.put(Material.RAW_GOLD, Material.GOLD_INGOT);
        SMELT_MAP.put(Material.GOLD_ORE, Material.GOLD_INGOT);
        SMELT_MAP.put(Material.DEEPSLATE_GOLD_ORE, Material.GOLD_INGOT);
        SMELT_MAP.put(Material.NETHER_GOLD_ORE, Material.GOLD_INGOT);
        
        SMELT_MAP.put(Material.RAW_COPPER, Material.COPPER_INGOT);
        SMELT_MAP.put(Material.COPPER_ORE, Material.COPPER_INGOT);
        SMELT_MAP.put(Material.DEEPSLATE_COPPER_ORE, Material.COPPER_INGOT);
        
        SMELT_MAP.put(Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP);
        
        // Building Blocks
        SMELT_MAP.put(Material.COBBLESTONE, Material.STONE);
        SMELT_MAP.put(Material.COBBLED_DEEPSLATE, Material.DEEPSLATE);
        SMELT_MAP.put(Material.SAND, Material.GLASS);
        SMELT_MAP.put(Material.RED_SAND, Material.GLASS);
        SMELT_MAP.put(Material.CLAY_BALL, Material.BRICK);
        SMELT_MAP.put(Material.CLAY, Material.TERRACOTTA);
        SMELT_MAP.put(Material.NETHERRACK, Material.NETHER_BRICK);
        SMELT_MAP.put(Material.STONE, Material.SMOOTH_STONE);
        
        // Logs -> Charcoal
        SMELT_MAP.put(Material.OAK_LOG, Material.CHARCOAL);
        SMELT_MAP.put(Material.SPRUCE_LOG, Material.CHARCOAL);
        SMELT_MAP.put(Material.BIRCH_LOG, Material.CHARCOAL);
        SMELT_MAP.put(Material.JUNGLE_LOG, Material.CHARCOAL);
        SMELT_MAP.put(Material.ACACIA_LOG, Material.CHARCOAL);
        SMELT_MAP.put(Material.DARK_OAK_LOG, Material.CHARCOAL);
        SMELT_MAP.put(Material.MANGROVE_LOG, Material.CHARCOAL);
        SMELT_MAP.put(Material.CHERRY_LOG, Material.CHARCOAL);
        SMELT_MAP.put(Material.OAK_WOOD, Material.CHARCOAL);
        SMELT_MAP.put(Material.SPRUCE_WOOD, Material.CHARCOAL);
        SMELT_MAP.put(Material.BIRCH_WOOD, Material.CHARCOAL);
        SMELT_MAP.put(Material.JUNGLE_WOOD, Material.CHARCOAL);
        SMELT_MAP.put(Material.ACACIA_WOOD, Material.CHARCOAL);
        SMELT_MAP.put(Material.DARK_OAK_WOOD, Material.CHARCOAL);
        SMELT_MAP.put(Material.MANGROVE_WOOD, Material.CHARCOAL);
        SMELT_MAP.put(Material.CHERRY_WOOD, Material.CHARCOAL);
        
        // Other
        SMELT_MAP.put(Material.CACTUS, Material.GREEN_DYE);
        SMELT_MAP.put(Material.KELP, Material.DRIED_KELP);
        SMELT_MAP.put(Material.POTATO, Material.BAKED_POTATO);
        SMELT_MAP.put(Material.MUTTON, Material.COOKED_MUTTON);
        SMELT_MAP.put(Material.BEEF, Material.COOKED_BEEF);
        SMELT_MAP.put(Material.CHICKEN, Material.COOKED_CHICKEN);
        SMELT_MAP.put(Material.PORKCHOP, Material.COOKED_PORKCHOP);
        SMELT_MAP.put(Material.COD, Material.COOKED_COD);
        SMELT_MAP.put(Material.SALMON, Material.COOKED_SALMON);
        SMELT_MAP.put(Material.RABBIT, Material.COOKED_RABBIT);
    }

    public SmeltingTouchListener(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.enchantStorage = new SkyblockEnchantStorage(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool == null || tool.getType() == Material.AIR) return;

        SkyblockEnchantment smeltingTouch = EnchantmentRegistry.get("smelting_touch");
        if (smeltingTouch == null) return;

        if (enchantStorage.getLevel(tool, smeltingTouch) <= 0) {
            return;
        }

        Material blockType = event.getBlockState().getType();
        boolean isNetherGold = blockType == Material.NETHER_GOLD_ORE;
        boolean nuggetSmelted = false;

        for (Item itemEntity : event.getItems()) {
            ItemStack stack = itemEntity.getItemStack();
            Material type = stack.getType();

            if (isNetherGold && type == Material.GOLD_NUGGET) {
                if (!nuggetSmelted) {
                    stack.setType(Material.GOLD_INGOT);
                    stack.setAmount(1);
                    itemEntity.setItemStack(stack);
                    nuggetSmelted = true;
                } else {
                    itemEntity.remove();
                }
                continue;
            }

            Material smelted = SMELT_MAP.get(type);
            if (smelted != null) {
                stack.setType(smelted);
                itemEntity.setItemStack(stack);
            }
        }
    }
}
