package io.papermc.Grivience.mines;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.util.RegenPlaceholderUtil;
import io.papermc.Grivience.zone.Zone;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles the "Newbie Mines" standalone regeneration logic.
 * Independent of Minehub code but shares the same visual stages:
 * Ore -> Bedrock -> Cobblestone -> Iron/Coal/Gold.
 */
public final class NewbieMinesListener implements Listener {
    private static final String ZONE_ID = "newbie_mines";
    private static final Set<Material> BREAKABLE_ORES = EnumSet.of(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE
    );

    private final GriviencePlugin plugin;

    public NewbieMinesListener(GriviencePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onNewbieMineBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location loc = block.getLocation();
        
        // 1. Check if in Newbie Mines zone
        if (plugin.getZoneManager() == null) return;
        Zone zone = plugin.getZoneManager().getZone(ZONE_ID);
        if (zone == null || !zone.contains(loc)) return;

        // 2. Allow breaking even if the zone is protected by default
        Material type = block.getType();
        if (!BREAKABLE_ORES.contains(type)) {
            return;
        }

        // 3. Bypass any existing protection for these specific ores in this zone
        event.setCancelled(false);

        // 4. Handle Regeneration Cycle
        handleRegeneration(block, type);
    }

    private void handleRegeneration(Block block, Material originalType) {
        Location location = block.getLocation().clone();
        
        // Configuration values (standalone but using consistent defaults)
        long totalRegenTicks = 120L; // 6 seconds total
        long bedrockToCobbleDelay = totalRegenTicks / 2; // 3 seconds in bedrock
        long cobbleToOreDelay = totalRegenTicks; // another 3 seconds in cobble (total 6)

        // Step 1: Immediately turn to Bedrock (after current tick to avoid block-drop issues)
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Block b = location.getBlock();
            b.setType(Material.BEDROCK, false);
        });

        // Step 2: Bedrock -> Cobblestone
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Block b = location.getBlock();
            if (b.getType() == Material.BEDROCK) {
                b.setType(Material.COBBLESTONE, false);
            }
        }, bedrockToCobbleDelay);

        // Step 3: Cobblestone -> New Ore (Iron, Coal, or Gold)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Block b = location.getBlock();
            if (b.getType() == Material.COBBLESTONE) {
                Material nextOre = rollNewbieOre();
                // Match deepslate variant if original was deepslate
                if (originalType.name().startsWith("DEEPSLATE_") && !nextOre.name().startsWith("DEEPSLATE_")) {
                    nextOre = Material.valueOf("DEEPSLATE_" + nextOre.name());
                }
                b.setType(nextOre, false);
            }
        }, cobbleToOreDelay);
    }

    private Material rollNewbieOre() {
        int chance = ThreadLocalRandom.current().nextInt(100);
        if (chance < 60) return Material.COAL_ORE;    // 60% Coal
        if (chance < 90) return Material.IRON_ORE;    // 30% Iron
        return Material.GOLD_ORE;                     // 10% Gold
    }
}
