package io.papermc.Grivience.skyblock.listener;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.MoistureChangeEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Regenerates breakable crop blocks inside a configured Farm Hub region.
 */
public final class FarmHubCropRegenerationListener implements Listener {
    private static final Set<Material> REGENERATING_CROPS = EnumSet.of(
            Material.WHEAT,
            Material.CARROTS,
            Material.POTATOES,
            Material.BEETROOTS,
            Material.NETHER_WART,
            Material.COCOA,
            Material.SUGAR_CANE,
            Material.CACTUS,
            Material.BAMBOO,
            Material.KELP,
            Material.KELP_PLANT,
            Material.SWEET_BERRY_BUSH,
            Material.MELON,
            Material.PUMPKIN,
            Material.MELON_STEM,
            Material.ATTACHED_MELON_STEM,
            Material.PUMPKIN_STEM,
            Material.ATTACHED_PUMPKIN_STEM,
            Material.TORCHFLOWER_CROP,
            Material.PITCHER_CROP,
            Material.PITCHER_PLANT,
            Material.CHORUS_FLOWER,
            Material.CHORUS_PLANT
    );

    private final GriviencePlugin plugin;
    private final BukkitTask areaMaintenanceTask;
    private long hydrationTickBudget;
    private long forcedGrowthTickBudget;
    private long maintenanceScanIndex;

    public FarmHubCropRegenerationListener(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.areaMaintenanceTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickAreaMaintenance, 40L, 20L);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCropGrow(BlockGrowEvent event) {
        if (event == null || event.getBlock() == null || event.getNewState() == null) {
            return;
        }
        if (!isGrowthBoostEnabled()) {
            return;
        }

        Block block = event.getBlock();
        BlockData newData = event.getNewState().getBlockData();
        Material cropType = event.getNewState().getType();
        if (!REGENERATING_CROPS.contains(cropType) && !REGENERATING_CROPS.contains(block.getType())) {
            return;
        }
        if (!isInFarmHubArea(event.getNewState().getLocation())) {
            return;
        }

        int bonusSteps = bonusGrowthSteps();
        if (bonusSteps <= 0) {
            return;
        }

        if (newData instanceof Ageable ageable) {
            int currentAge = ageable.getAge();
            int maxAge = ageable.getMaximumAge();
            int boostedAge = Math.min(maxAge, currentAge + bonusSteps);
            if (boostedAge > currentAge) {
                ageable.setAge(boostedAge);
                event.getNewState().setBlockData(ageable);
            }
            return;
        }

        if (isVerticalPlant(cropType)) {
            Location start = event.getNewState().getLocation().clone();
            BlockData clonedData = newData.clone();
            Bukkit.getScheduler().runTask(plugin, () ->
                    applyVerticalGrowthBoost(start, cropType, clonedData, bonusSteps));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCropBreak(BlockBreakEvent event) {
        if (event.getPlayer() == null || event.getBlock() == null) {
            return;
        }
        if (!isRegenEnabled()) {
            return;
        }

        Block broken = event.getBlock();
        if (!REGENERATING_CROPS.contains(broken.getType())) {
            return;
        }
        if (!isInFarmHubArea(broken.getLocation())) {
            return;
        }

        List<CropSnapshot> snapshots = snapshotsFor(broken);
        if (snapshots.isEmpty()) {
            return;
        }

        long delayTicks = Math.max(1L, plugin.getConfig().getLong("skyblock.farmhub-crop-regen.delay-ticks", 60L));
        Bukkit.getScheduler().runTaskLater(plugin, () -> regenerateSnapshots(snapshots), delayTicks);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMoistureChange(MoistureChangeEvent event) {
        if (event == null || event.getBlock() == null) {
            return;
        }
        if (!isFarmlandForcedHydrationEnabled()) {
            return;
        }
        Block block = event.getBlock();
        if (block.getType() != Material.FARMLAND) {
            return;
        }
        if (!isInFarmHubArea(block.getLocation())) {
            return;
        }

        forceHydrated(block);
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFarmlandFade(BlockFadeEvent event) {
        if (event == null || event.getBlock() == null || event.getNewState() == null) {
            return;
        }
        if (!isFarmlandForcedHydrationEnabled()) {
            return;
        }
        Block block = event.getBlock();
        if (block.getType() != Material.FARMLAND) {
            return;
        }
        if (event.getNewState().getType() != Material.DIRT) {
            return;
        }
        if (!isInFarmHubArea(block.getLocation())) {
            return;
        }

        event.setCancelled(true);
        forceHydrated(block);
    }

    private boolean isRegenEnabled() {
        return plugin.getConfig().getBoolean("skyblock.farmhub-crop-regen.enabled", true);
    }

    private boolean isGrowthBoostEnabled() {
        return plugin.getConfig().getBoolean("skyblock.farmhub-crop-growth.enabled", true);
    }

    private boolean isForcedGrowthEnabled() {
        return plugin.getConfig().getBoolean("skyblock.farmhub-crop-growth.force-growth", true);
    }

    private boolean isFarmlandForcedHydrationEnabled() {
        return plugin.getConfig().getBoolean("skyblock.farmhub-farmland.force-hydrated", true);
    }

    private int bonusGrowthSteps() {
        double multiplier = plugin.getConfig().getDouble("skyblock.farmhub-crop-growth.multiplier", 4.0D);
        if (!Double.isFinite(multiplier)) {
            multiplier = 4.0D;
        }
        multiplier = Math.max(1.0D, Math.min(8.0D, multiplier));

        int extra = (int) Math.floor(multiplier) - 1;
        double fractional = multiplier - Math.floor(multiplier);
        if (fractional > 0.0D && ThreadLocalRandom.current().nextDouble() < fractional) {
            extra++;
        }
        return Math.max(0, Math.min(extra, 6));
    }

    private boolean isInFarmHubArea(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }

        String areaBase = "skyblock.farmhub-crop-area.";
        if (!plugin.getConfig().getBoolean(areaBase + "enabled", false)) {
            return false;
        }

        String worldName = plugin.getConfig().getString(areaBase + "world");
        if (worldName == null || worldName.isBlank()) {
            worldName = plugin.getConfig().getString("skyblock.farmhub-world", "world");
        }
        if (!location.getWorld().getName().equalsIgnoreCase(worldName)) {
            return false;
        }

        int x1 = plugin.getConfig().getInt(areaBase + "pos1.x");
        int y1 = plugin.getConfig().getInt(areaBase + "pos1.y");
        int z1 = plugin.getConfig().getInt(areaBase + "pos1.z");
        int x2 = plugin.getConfig().getInt(areaBase + "pos2.x");
        int y2 = plugin.getConfig().getInt(areaBase + "pos2.y");
        int z2 = plugin.getConfig().getInt(areaBase + "pos2.z");

        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    private void tickAreaMaintenance() {
        if (areaMaintenanceTask == null || areaMaintenanceTask.isCancelled()) {
            return;
        }

        AreaBounds bounds = getFarmHubAreaBounds();
        if (bounds == null) {
            return;
        }

        boolean runHydration = false;
        boolean runForcedGrowth = false;

        hydrationTickBudget += 20L;
        forcedGrowthTickBudget += 20L;

        if (isFarmlandForcedHydrationEnabled()) {
            long interval = Math.max(20L, plugin.getConfig().getLong("skyblock.farmhub-farmland.hydrate-interval-ticks", 40L));
            if (hydrationTickBudget >= interval) {
                runHydration = true;
                hydrationTickBudget = 0L;
            }
        } else {
            hydrationTickBudget = 0L;
        }

        if (isForcedGrowthEnabled()) {
            long interval = Math.max(20L, plugin.getConfig().getLong("skyblock.farmhub-crop-growth.force-interval-ticks", 20L));
            if (forcedGrowthTickBudget >= interval) {
                runForcedGrowth = true;
                forcedGrowthTickBudget = 0L;
            }
        } else {
            forcedGrowthTickBudget = 0L;
        }

        if (!runHydration && !runForcedGrowth) {
            return;
        }

        long maxHydrationScan = Math.max(1L, plugin.getConfig().getLong("skyblock.farmhub-farmland.max-scan-blocks", 350_000L));
        long maxGrowthScan = Math.max(1L, plugin.getConfig().getLong("skyblock.farmhub-crop-growth.max-scan-blocks", 300_000L));
        long maxAllowedScan = Math.max(maxHydrationScan, maxGrowthScan);
        if (bounds.volume() > maxAllowedScan) {
            // Area is larger than legacy full-scan cap: switch to slice scanning so growth is still enforced.
        }

        int forcedSteps = runForcedGrowth ? Math.max(1, bonusGrowthSteps() + 1) : 0;
        if (runForcedGrowth && forcedSteps <= 0) {
            forcedSteps = 1;
        }
        scanAreaSlice(bounds, runHydration, runForcedGrowth, forcedSteps);
    }

    private void scanAreaSlice(AreaBounds bounds, boolean runHydration, boolean runForcedGrowth, int forcedSteps) {
        if (bounds == null || bounds.world() == null || bounds.volume() <= 0L) {
            return;
        }

        long scanBudget = Math.max(500L, plugin.getConfig().getLong("skyblock.farmhub-maintenance.scan-blocks-per-run", 50000L));
        long toProcess = Math.min(bounds.volume(), scanBudget);

        if (maintenanceScanIndex >= bounds.volume()) {
            maintenanceScanIndex = 0L;
        }

        for (long i = 0L; i < toProcess; i++) {
            long index = (maintenanceScanIndex + i) % bounds.volume();
            Block block = blockAtIndex(bounds, index);
            if (block == null) {
                continue;
            }

            if (runHydration && block.getType() == Material.FARMLAND) {
                forceHydrated(block);
            }
            if (runForcedGrowth) {
                forceGrowth(block, forcedSteps);
            }
        }

        maintenanceScanIndex = (maintenanceScanIndex + toProcess) % bounds.volume();
    }

    private Block blockAtIndex(AreaBounds bounds, long index) {
        long width = bounds.width();
        long depth = bounds.depth();
        long layerSize = width * depth;
        if (layerSize <= 0L) {
            return null;
        }

        long yOffset = index / layerSize;
        long rem = index % layerSize;
        long zOffset = rem / width;
        long xOffset = rem % width;

        int x = bounds.minX() + (int) xOffset;
        int y = bounds.minY() + (int) yOffset;
        int z = bounds.minZ() + (int) zOffset;
        return bounds.world().getBlockAt(x, y, z);
    }

    private void forceGrowth(Block block, int steps) {
        if (block == null || steps <= 0) {
            return;
        }

        Material type = block.getType();
        if (!REGENERATING_CROPS.contains(type)) {
            return;
        }

        BlockData data = block.getBlockData();
        if (data instanceof Ageable ageable) {
            int currentAge = ageable.getAge();
            int maxAge = ageable.getMaximumAge();
            int boostedAge = Math.min(maxAge, currentAge + steps);
            if (boostedAge > currentAge) {
                ageable.setAge(boostedAge);
                block.setBlockData(ageable, false);
            }
            return;
        }

        if (isVerticalPlant(type)) {
            applyVerticalGrowthBoost(block.getLocation(), type, data, steps);
        }
    }

    private AreaBounds getFarmHubAreaBounds() {
        String areaBase = "skyblock.farmhub-crop-area.";
        if (!plugin.getConfig().getBoolean(areaBase + "enabled", false)) {
            return null;
        }

        String worldName = plugin.getConfig().getString(areaBase + "world");
        if (worldName == null || worldName.isBlank()) {
            worldName = plugin.getConfig().getString("skyblock.farmhub-world", "world");
        }

        var world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }

        int x1 = plugin.getConfig().getInt(areaBase + "pos1.x");
        int y1 = plugin.getConfig().getInt(areaBase + "pos1.y");
        int z1 = plugin.getConfig().getInt(areaBase + "pos1.z");
        int x2 = plugin.getConfig().getInt(areaBase + "pos2.x");
        int y2 = plugin.getConfig().getInt(areaBase + "pos2.y");
        int z2 = plugin.getConfig().getInt(areaBase + "pos2.z");

        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);

        long width = (long) (maxX - minX + 1);
        long height = (long) (maxY - minY + 1);
        long depth = (long) (maxZ - minZ + 1);
        long volume = Math.max(0L, width * height * depth);

        return new AreaBounds(world, minX, maxX, minY, maxY, minZ, maxZ, width, depth, volume);
    }

    private void forceHydrated(Block block) {
        if (block == null || block.getType() != Material.FARMLAND) {
            return;
        }
        if (!(block.getBlockData() instanceof Farmland farmland)) {
            return;
        }
        int maxMoisture = farmland.getMaximumMoisture();
        if (farmland.getMoisture() >= maxMoisture) {
            return;
        }
        farmland.setMoisture(maxMoisture);
        block.setBlockData(farmland, false);
    }

    private List<CropSnapshot> snapshotsFor(Block broken) {
        List<CropSnapshot> snapshots = new ArrayList<>();
        if (broken == null) {
            return snapshots;
        }

        Material brokenType = broken.getType();
        BlockData brokenData = broken.getBlockData();

        if (isVerticalPlant(brokenType)) {
            Material normalized = normalizeVerticalCrop(brokenType);
            Block base = findBottomOfColumn(broken, normalized);
            snapshots.add(seedlingSnapshotOf(base, normalized));

            Block scan = base.getRelative(0, 1, 0);
            for (int i = 0; i < 16; i++) {
                if (!isInVerticalFamily(scan.getType(), normalized)) {
                    break;
                }
                snapshots.add(clearSnapshotOf(scan));
                scan = scan.getRelative(0, 1, 0);
            }
            snapshots.sort(Comparator.comparingInt(s -> s.location().getBlockY()));
            return snapshots;
        }

        if (brokenData instanceof Bisected bisected) {
            Block base = bisected.getHalf() == Bisected.Half.TOP
                    ? broken.getRelative(0, -1, 0)
                    : broken;
            snapshots.add(seedlingSnapshotOf(base, base.getType()));

            Block top = base.getRelative(0, 1, 0);
            if (top.getType() == base.getType()) {
                snapshots.add(clearSnapshotOf(top));
            }
            snapshots.sort(Comparator.comparingInt(s -> s.location().getBlockY()));
            return snapshots;
        }

        snapshots.add(seedlingSnapshotOf(broken, brokenType));

        snapshots.sort(Comparator.comparingInt(s -> s.location().getBlockY()));
        return snapshots;
    }

    private boolean isVerticalPlant(Material material) {
        return material == Material.SUGAR_CANE
                || material == Material.CACTUS
                || material == Material.BAMBOO
                || material == Material.KELP
                || material == Material.KELP_PLANT
                || material == Material.CHORUS_PLANT
                || material == Material.CHORUS_FLOWER;
    }

    private void applyVerticalGrowthBoost(Location startLocation, Material cropType, BlockData data, int bonusSteps) {
        if (startLocation == null || startLocation.getWorld() == null || bonusSteps <= 0) {
            return;
        }
        Block start = startLocation.getBlock();
        if (!isInFarmHubArea(startLocation)) {
            return;
        }

        Material normalized = normalizeVerticalCrop(cropType);
        Block top = findTopOfColumn(start, normalized);

        for (int i = 0; i < bonusSteps; i++) {
            Block above = top.getRelative(0, 1, 0);
            if (!isInFarmHubArea(above.getLocation())) {
                break;
            }
            if (!canGrowInto(normalized, above.getType())) {
                break;
            }

            if (isKelp(normalized)) {
                if (top.getType() == Material.KELP) {
                    top.setType(Material.KELP_PLANT, false);
                }
                above.setType(Material.KELP, false);
            } else {
                above.setType(normalized, false);
                BlockData clone = data.clone();
                if (clone.getMaterial() == normalized) {
                    above.setBlockData(clone, false);
                }
            }
            top = above;
        }
    }

    private Block findBottomOfColumn(Block start, Material cropType) {
        Block current = start;
        while (true) {
            Block down = current.getRelative(0, -1, 0);
            if (!isInVerticalFamily(down.getType(), cropType)) {
                return current;
            }
            current = down;
        }
    }

    private Block findTopOfColumn(Block start, Material cropType) {
        Block current = start;
        while (true) {
            Block up = current.getRelative(0, 1, 0);
            if (!isInVerticalFamily(up.getType(), cropType)) {
                return current;
            }
            current = up;
        }
    }

    private boolean canGrowInto(Material cropType, Material destination) {
        if (destination.isAir()) {
            return true;
        }
        return isKelp(cropType) && destination == Material.WATER;
    }

    private Material normalizeVerticalCrop(Material cropType) {
        if (cropType == Material.KELP_PLANT) {
            return Material.KELP;
        }
        if (cropType == Material.CHORUS_FLOWER) {
            return Material.CHORUS_PLANT;
        }
        return cropType;
    }

    private boolean isInVerticalFamily(Material material, Material cropType) {
        if (material == cropType) {
            return true;
        }
        if (isKelp(cropType)) {
            return material == Material.KELP || material == Material.KELP_PLANT;
        }
        if (cropType == Material.CHORUS_PLANT) {
            return material == Material.CHORUS_PLANT || material == Material.CHORUS_FLOWER;
        }
        return false;
    }

    private boolean isKelp(Material material) {
        return material == Material.KELP || material == Material.KELP_PLANT;
    }

    private CropSnapshot seedlingSnapshotOf(Block block, Material originalType) {
        return new CropSnapshot(block.getLocation().clone(), seedlingDataFor(block, originalType));
    }

    private CropSnapshot clearSnapshotOf(Block block) {
        return new CropSnapshot(block.getLocation().clone(), Material.AIR.createBlockData());
    }

    private BlockData seedlingDataFor(Block block, Material originalType) {
        Material seedlingType = resolveSeedlingType(block, originalType);
        BlockData data = (seedlingType == originalType)
                ? block.getBlockData().clone()
                : seedlingType.createBlockData();

        if (data instanceof Bisected bisected) {
            bisected.setHalf(Bisected.Half.BOTTOM);
        }
        if (data instanceof Ageable ageable) {
            ageable.setAge(0);
        }
        return data;
    }

    private Material resolveSeedlingType(Block block, Material originalType) {
        if (originalType == Material.ATTACHED_MELON_STEM) {
            return Material.MELON_STEM;
        }
        if (originalType == Material.ATTACHED_PUMPKIN_STEM) {
            return Material.PUMPKIN_STEM;
        }
        if (originalType == Material.PITCHER_PLANT) {
            return Material.PITCHER_CROP;
        }
        if (originalType == Material.KELP_PLANT) {
            return Material.KELP;
        }
        if (originalType == Material.CHORUS_PLANT) {
            return Material.CHORUS_FLOWER;
        }
        return originalType;
    }

    private void regenerateSnapshots(List<CropSnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return;
        }

        for (CropSnapshot snapshot : snapshots) {
            if (snapshot == null || snapshot.location() == null || snapshot.location().getWorld() == null) {
                continue;
            }
            if (!snapshot.location().isWorldLoaded()) {
                continue;
            }
            Block block = snapshot.location().getBlock();
            block.setBlockData(snapshot.data(), false);
        }
    }

    private record CropSnapshot(Location location, BlockData data) {}

    private record AreaBounds(
            org.bukkit.World world,
            int minX,
            int maxX,
            int minY,
            int maxY,
            int minZ,
            int maxZ,
            long width,
            long depth,
            long volume
    ) {}
}
