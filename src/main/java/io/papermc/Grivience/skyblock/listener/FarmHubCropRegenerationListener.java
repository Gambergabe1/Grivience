/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.GameMode
 *  org.bukkit.Keyed
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.Particle
 *  org.bukkit.Tag
 *  org.bukkit.World
 *  org.bukkit.block.Block
 *  org.bukkit.block.data.Ageable
 *  org.bukkit.block.data.Bisected
 *  org.bukkit.block.data.Bisected$Half
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.block.data.type.Farmland
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.block.BlockBreakEvent
 *  org.bukkit.event.block.BlockFadeEvent
 *  org.bukkit.event.block.BlockGrowEvent
 *  org.bukkit.event.block.BlockPlaceEvent
 *  org.bukkit.event.block.MoistureChangeEvent
 *  org.bukkit.event.entity.EntityChangeBlockEvent
 *  org.bukkit.event.player.PlayerBucketEmptyEvent
 *  org.bukkit.event.player.PlayerBucketFillEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package io.papermc.Grivience.skyblock.listener;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.enchantment.EnchantmentRegistry;
import io.papermc.Grivience.enchantment.SkyblockEnchantStorage;
import io.papermc.Grivience.enchantment.SkyblockEnchantment;
import io.papermc.Grivience.item.CustomArmorManager;
import io.papermc.Grivience.stats.SkyblockCombatEngine;
import io.papermc.Grivience.util.CropReplantUtil;
import io.papermc.Grivience.util.DropDeliveryUtil;
import io.papermc.Grivience.util.FarmingSetBonusUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.MoistureChangeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class FarmHubCropRegenerationListener
implements Listener {
    private static final long PROTECTION_MESSAGE_THROTTLE_MS = 1200L;
    private static final List<ManagedAreaProfile> MANAGED_AREAS = List.of(new ManagedAreaProfile("farmhub", "Farm Hub", "skyblock.farmhub-crop-area.", "skyblock.farmhub-world", "skyblock.farmhub-crop-regen.", "skyblock.farmhub-crop-growth.", "skyblock.farmhub-farmland.", "skyblock.farmhub-maintenance.", "skyblock.farmhub-tree-regen.", "grivience.farmhub.build"), new ManagedAreaProfile("hub", "Hub", "skyblock.hub-crop-area.", "skyblock.hub-world", "skyblock.hub-crop-regen.", "skyblock.hub-crop-growth.", "skyblock.hub-farmland.", "skyblock.hub-maintenance.", "skyblock.hub-tree-regen.", "grivience.hub.build"), new ManagedAreaProfile("oaklys", "Oaklys Wood Depo", "skyblock.oaklys-wood-depo.", "skyblock.hub-world", "skyblock.oaklys-wood-regen.", "skyblock.oaklys-wood-growth.", "skyblock.oaklys-wood-farmland.", "skyblock.oaklys-wood-maintenance.", "skyblock.oaklys-wood-regen.", "grivience.oaklys.build"));
    private static final Set<Material> REGENERATING_CROPS = EnumSet.of(Material.WHEAT, new Material[]{Material.CARROTS, Material.POTATOES, Material.BEETROOTS, Material.NETHER_WART, Material.COCOA, Material.SUGAR_CANE, Material.CACTUS, Material.BAMBOO, Material.KELP, Material.KELP_PLANT, Material.SWEET_BERRY_BUSH, Material.MELON, Material.PUMPKIN, Material.MELON_STEM, Material.ATTACHED_MELON_STEM, Material.PUMPKIN_STEM, Material.ATTACHED_PUMPKIN_STEM, Material.TORCHFLOWER_CROP, Material.PITCHER_CROP, Material.PITCHER_PLANT, Material.CHORUS_FLOWER, Material.CHORUS_PLANT});
    private static final Set<Material> EXTRA_FARMABLE_BREAK = EnumSet.of(Material.RED_MUSHROOM, new Material[]{Material.BROWN_MUSHROOM, Material.RED_MUSHROOM_BLOCK, Material.BROWN_MUSHROOM_BLOCK, Material.MUSHROOM_STEM, Material.MANGROVE_PROPAGULE, Material.MANGROVE_ROOTS, Material.MUDDY_MANGROVE_ROOTS, Material.CRIMSON_FUNGUS, Material.WARPED_FUNGUS, Material.NETHER_WART_BLOCK, Material.WARPED_WART_BLOCK});
    private static final Set<Material> EXTRA_FARMABLE_PLACE = EnumSet.of(Material.RED_MUSHROOM, Material.BROWN_MUSHROOM, Material.MANGROVE_PROPAGULE, Material.CRIMSON_FUNGUS, Material.WARPED_FUNGUS);
    private final GriviencePlugin plugin;
    private final BukkitTask areaMaintenanceTask;
    private final Map<UUID, Long> lastProtectionWarnAtMs = new ConcurrentHashMap<UUID, Long>();
    private final Map<String, Long> hydrationTickBudgetByArea = new ConcurrentHashMap<String, Long>();
    private final Map<String, Long> forcedGrowthTickBudgetByArea = new ConcurrentHashMap<String, Long>();
    private final Map<String, Long> maintenanceScanIndexByArea = new ConcurrentHashMap<String, Long>();
    private final Set<String> pendingTreeBlocks = ConcurrentHashMap.newKeySet();
    private final Set<String> instantReplantBreaks = ConcurrentHashMap.newKeySet();
    private final SkyblockEnchantStorage enchantStorage;

    public FarmHubCropRegenerationListener(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.enchantStorage = new SkyblockEnchantStorage((Plugin)plugin);
        this.areaMaintenanceTask = Bukkit.getScheduler().runTaskTimer((Plugin)plugin, () -> this.tickAreaMaintenance(), 40L, 20L);
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onCropGrow(BlockGrowEvent event) {
        if (event == null || event.getBlock() == null || event.getNewState() == null) {
            return;
        }
        Block block = event.getBlock();
        BlockData newData = event.getNewState().getBlockData();
        Material cropType = event.getNewState().getType();
        if (!REGENERATING_CROPS.contains(cropType) && !REGENERATING_CROPS.contains(block.getType())) {
            return;
        }
        ManagedAreaProfile area = this.resolveManagedArea(event.getNewState().getLocation());
        if (area == null || !this.isGrowthBoostEnabled(area)) {
            return;
        }
        int bonusSteps = this.bonusGrowthSteps(area);
        if (bonusSteps <= 0) {
            return;
        }
        if (newData instanceof Ageable) {
            Ageable ageable = (Ageable)newData;
            int currentAge = ageable.getAge();
            int maxAge = ageable.getMaximumAge();
            int boostedAge = Math.min(maxAge, currentAge + bonusSteps);
            if (boostedAge > currentAge) {
                ageable.setAge(boostedAge);
                event.getNewState().setBlockData((BlockData)ageable);
            }
            return;
        }
        if (this.isVerticalPlant(cropType)) {
            Location start = event.getNewState().getLocation().clone();
            BlockData clonedData = newData.clone();
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.applyVerticalGrowthBoost(start, cropType, clonedData, bonusSteps, area));
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onCropBreak(BlockBreakEvent event) {
        if (event.getPlayer() == null || event.getBlock() == null) {
            return;
        }
        Block broken = event.getBlock();
        if (!REGENERATING_CROPS.contains(broken.getType())) {
            return;
        }
        ManagedAreaProfile area = this.resolveManagedArea(broken.getLocation());
        if (area == null || !this.isRegenEnabled(area)) {
            return;
        }
        List<CropSnapshot> snapshots = this.snapshotsFor(broken, area);
        if (snapshots.isEmpty()) {
            return;
        }
        if (this.instantReplantBreaks.remove(this.blockKey(broken.getLocation()))) {
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.regenerateSnapshots(snapshots));
            return;
        }
        long delayTicks = Math.max(1L, this.plugin.getConfig().getLong(area.regenBase() + "delay-ticks", 60L));
        BukkitTask particleTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, () -> {
            for (CropSnapshot snapshot : snapshots) {
                snapshot.location().getWorld().spawnParticle(Particle.HAPPY_VILLAGER, snapshot.location().clone().add(0.5, 0.2, 0.5), 1, 0.2, 0.1, 0.2, 0.02);
            }
        }, 0L, 10L);
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            particleTask.cancel();
            this.regenerateSnapshots(snapshots);
        }, delayTicks);
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onCropBreakDrops(BlockBreakEvent event) {
        Material replantCost;
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (player == null || block == null) {
            return;
        }
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        Material cropType = block.getType();
        if (!REGENERATING_CROPS.contains(cropType)) {
            return;
        }
        ManagedAreaProfile area = this.resolveManagedArea(block.getLocation());
        if (area == null) {
            return;
        }
        Collection<ItemStack> vanillaDrops = this.resolveBreakDrops(block, player);
        if (vanillaDrops == null || vanillaDrops.isEmpty()) {
            return;
        }
        Collection<ItemStack> fortuneReferenceDrops = this.cloneDrops(vanillaDrops);
        if (this.shouldInstantReplant(player, block) && (replantCost = CropReplantUtil.replantCost(cropType)) != null) {
            boolean paid;
            boolean bl = paid = CropReplantUtil.removeOneFromStacks(vanillaDrops, replantCost) || CropReplantUtil.removeOneFromInventory(player, replantCost);
            if (paid) {
                this.instantReplantBreaks.add(this.blockKey(block.getLocation()));
            }
        }
        event.setDropItems(false);
        Location dropLocation = block.getLocation().add(0.5, 0.5, 0.5);
        if (this.plugin.getFarmingContestManager() != null) {
            this.plugin.getFarmingContestManager().recordHarvest(player, vanillaDrops);
        }
        DropDeliveryUtil.giveToInventoryOrDrop(player, vanillaDrops, dropLocation);
        this.applyFarmingFortuneBonus(player, cropType, block.getBlockData(), fortuneReferenceDrops, dropLocation);
    }

    private Collection<ItemStack> resolveBreakDrops(Block block, Player player) {
        ItemStack held;
        if (block == null) {
            return List.of();
        }
        ItemStack itemStack = held = player == null ? null : player.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) {
            return new ArrayList<ItemStack>(block.getDrops());
        }
        Collection drops = block.getDrops(held, (Entity)player);
        if (drops == null || drops.isEmpty()) {
            return new ArrayList<ItemStack>(block.getDrops());
        }
        return new ArrayList<ItemStack>(drops);
    }

    private Collection<ItemStack> cloneDrops(Collection<ItemStack> drops) {
        ArrayList<ItemStack> clones = new ArrayList<ItemStack>();
        if (drops == null) {
            return clones;
        }
        for (ItemStack stack : drops) {
            if (stack == null) continue;
            clones.add(stack.clone());
        }
        return clones;
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onTreeBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (player == null || block == null) {
            return;
        }
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (!this.isTreeBlock(block.getType())) {
            return;
        }
        ManagedAreaProfile area = this.resolveManagedArea(block.getLocation());
        if (area == null || !this.isTreeRegenEnabled(area) || this.isPendingTreeBlock(block.getLocation())) {
            return;
        }
        List<BlockSnapshot> snapshots = this.treeSnapshotsFor(block, area);
        if (snapshots.isEmpty()) {
            return;
        }
        long delayTicks = Math.max(1L, this.plugin.getConfig().getLong(area.treeRegenBase() + "delay-ticks", 120L));
        this.markPendingTree(snapshots, true);
        BukkitTask particleTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, () -> {
            for (BlockSnapshot snapshot : snapshots) {
                if (!Tag.LOGS.isTagged(snapshot.data().getMaterial())) continue;
                snapshot.location().getWorld().spawnParticle(Particle.HAPPY_VILLAGER, snapshot.location().clone().add(0.5, 0.5, 0.5), 1, 0.3, 0.3, 0.3, 0.05);
            }
        }, 0L, 10L);
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            try {
                particleTask.cancel();
                this.regenerateTreeSnapshots(snapshots);
            }
            finally {
                this.markPendingTree(snapshots, false);
            }
        }, delayTicks);
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onMoistureChange(MoistureChangeEvent event) {
        if (event == null || event.getBlock() == null) {
            return;
        }
        Block block = event.getBlock();
        if (block.getType() != Material.FARMLAND) {
            return;
        }
        ManagedAreaProfile area = this.resolveManagedArea(block.getLocation());
        if (area == null || !this.isFarmlandForcedHydrationEnabled(area)) {
            return;
        }
        this.forceHydrated(block);
        event.setCancelled(true);
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onFarmlandFade(BlockFadeEvent event) {
        if (event == null || event.getBlock() == null || event.getNewState() == null) {
            return;
        }
        Block block = event.getBlock();
        if (block.getType() != Material.FARMLAND) {
            return;
        }
        if (event.getNewState().getType() != Material.DIRT) {
            return;
        }
        ManagedAreaProfile area = this.resolveManagedArea(block.getLocation());
        if (area == null || !this.isFarmlandForcedHydrationEnabled(area)) {
            return;
        }
        event.setCancelled(true);
        this.forceHydrated(block);
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onFarmlandTrample(PlayerInteractEvent event) {
        if (event == null || event.getAction() != Action.PHYSICAL) {
            return;
        }
        Block clicked = event.getClickedBlock();
        if (clicked == null || clicked.getType() != Material.FARMLAND) {
            return;
        }
        ManagedAreaProfile area = this.resolveManagedArea(clicked.getLocation());
        if (area == null || !this.isFarmlandForcedHydrationEnabled(area)) {
            return;
        }
        event.setCancelled(true);
        this.forceHydrated(clicked);
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onFarmlandTrampleByEntity(EntityChangeBlockEvent event) {
        if (event == null || event.getBlock() == null) {
            return;
        }
        Block block = event.getBlock();
        if (block.getType() != Material.FARMLAND) {
            return;
        }
        if (event.getTo() != Material.DIRT) {
            return;
        }
        ManagedAreaProfile area = this.resolveManagedArea(block.getLocation());
        if (area == null || !this.isFarmlandForcedHydrationEnabled(area)) {
            return;
        }
        event.setCancelled(true);
        this.forceHydrated(block);
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onFarmHubProtectedBreak(BlockBreakEvent event) {
        if (event == null || event.getBlock() == null) {
            return;
        }
        ManagedAreaProfile area = this.resolveManagedArea(event.getBlock().getLocation());
        if (area == null) {
            if (this.isHubOrFarmHubWorld(event.getBlock().getWorld().getName()) && !this.canBypassFarmHubProtection(event.getPlayer(), null)) {
                event.setCancelled(true);
                this.warnFarmHubProtection(event.getPlayer(), "The Hub is protected. You can only break blocks in designated resource areas.");
            }
            return;
        }
        if (this.canBypassFarmHubProtection(event.getPlayer(), area)) {
            return;
        }
        if (this.isFarmableBreakType(event.getBlock().getType(), area)) {
            return;
        }
        event.setCancelled(true);
        this.warnFarmHubProtection(event.getPlayer(), this.areaDisplayName(area) + " is protected. Only configured resources can be broken.");
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onFarmHubProtectedPlace(BlockPlaceEvent event) {
        if (event == null || event.getBlockPlaced() == null) {
            return;
        }
        ManagedAreaProfile area = this.resolveManagedArea(event.getBlockPlaced().getLocation());
        if (area == null) {
            return;
        }
        if (this.canBypassFarmHubProtection(event.getPlayer(), area)) {
            return;
        }
        if (this.isFarmablePlaceType(event.getBlockPlaced().getType())) {
            return;
        }
        event.setCancelled(true);
        this.warnFarmHubProtection(event.getPlayer(), this.areaDisplayName(area) + " is protected. Only crops and farmable blocks can be placed.");
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onFarmHubBucketEmpty(PlayerBucketEmptyEvent event) {
        ManagedAreaProfile area;
        if (event == null) {
            return;
        }
        Block target = event.getBlock();
        ManagedAreaProfile managedAreaProfile = area = target == null ? null : this.resolveManagedArea(target.getLocation());
        if (area == null) {
            return;
        }
        if (this.canBypassFarmHubProtection(event.getPlayer(), area)) {
            return;
        }
        event.setCancelled(true);
        this.warnFarmHubProtection(event.getPlayer(), "Buckets are disabled in this " + this.areaDisplayName(area) + " area.");
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onFarmHubBucketFill(PlayerBucketFillEvent event) {
        if (event == null || event.getBlockClicked() == null) {
            return;
        }
        ManagedAreaProfile area = this.resolveManagedArea(event.getBlockClicked().getLocation());
        if (area == null) {
            if (this.isHubOrFarmHubWorld(event.getBlockClicked().getWorld().getName()) && !this.canBypassFarmHubProtection(event.getPlayer(), null)) {
                event.setCancelled(true);
                this.warnFarmHubProtection(event.getPlayer(), "Buckets are disabled in the Hub.");
            }
            return;
        }
        if (this.canBypassFarmHubProtection(event.getPlayer(), area)) {
            return;
        }
        event.setCancelled(true);
        this.warnFarmHubProtection(event.getPlayer(), "Buckets are disabled in this " + this.areaDisplayName(area) + " area.");
    }

    private boolean isRegenEnabled(ManagedAreaProfile area) {
        return area != null && this.plugin.getConfig().getBoolean(area.regenBase() + "enabled", true);
    }

    private boolean isGrowthBoostEnabled(ManagedAreaProfile area) {
        return area != null && this.plugin.getConfig().getBoolean(area.growthBase() + "enabled", true);
    }

    private boolean isForcedGrowthEnabled(ManagedAreaProfile area) {
        return area != null && this.plugin.getConfig().getBoolean(area.growthBase() + "force-growth", true);
    }

    private boolean isFarmlandForcedHydrationEnabled(ManagedAreaProfile area) {
        return area != null && this.plugin.getConfig().getBoolean(area.farmlandBase() + "force-hydrated", true);
    }

    private boolean isTreeRegenEnabled(ManagedAreaProfile area) {
        return area != null && this.plugin.getConfig().getBoolean(area.treeRegenBase() + "enabled", true);
    }

    private boolean canBypassFarmHubProtection(Player player, ManagedAreaProfile area) {
        return player != null && (player.hasPermission("grivience.admin") || player.hasPermission(area.bypassPermission()));
    }

    private boolean shouldInstantReplant(Player player, Block block) {
        if (player == null || block == null) {
            return false;
        }
        Material type = block.getType();
        if (!CropReplantUtil.isReplantable(type) || !CropReplantUtil.isMature(type, block.getBlockData())) {
            return false;
        }
        return this.hasReplenishEnchant(player.getInventory().getItemInMainHand()) || this.hasHarvesterInstantReplant(player);
    }

    private boolean hasReplenishEnchant(ItemStack item) {
        SkyblockEnchantment replenish = EnchantmentRegistry.get("replenish");
        return replenish != null && this.enchantStorage.getLevel(item, replenish) > 0;
    }

    private boolean hasHarvesterInstantReplant(Player player) {
        CustomArmorManager armorManager = this.plugin.getCustomArmorManager();
        if (armorManager == null || player == null) {
            return false;
        }
        int pieces = 0;
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            if (piece == null || !"harvester_embrace".equalsIgnoreCase(armorManager.getArmorSetId(piece))) continue;
            ++pieces;
        }
        return pieces >= 4;
    }

    private String areaDisplayName(ManagedAreaProfile area) {
        if (area == null) {
            return "Hub";
        }
        if (!"farmhub".equalsIgnoreCase(area.id()) || !this.usesSharedFarmHubWorld()) {
            return area.displayName();
        }
        return "Hub";
    }

    private boolean isHubOrFarmHubWorld(String worldName) {
        String hubWorld = this.plugin.getConfig().getString("skyblock.hub-world", "world");
        String farmhubWorld = this.plugin.getConfig().getString("skyblock.farmhub-world", "world");
        if (worldName == null) return false;
        return worldName.equalsIgnoreCase(hubWorld) || worldName.equalsIgnoreCase(farmhubWorld);
    }

    private boolean usesSharedFarmHubWorld() {
        String hubWorld = this.plugin.getConfig().getString("skyblock.hub-world", "world");
        String farmhubWorld = this.plugin.getConfig().getString("skyblock.farmhub-world", "world");
        if (hubWorld == null || farmhubWorld == null) {
            return false;
        }
        return hubWorld.equalsIgnoreCase(farmhubWorld);
    }

    private boolean isFarmableBreakType(Material type, ManagedAreaProfile area) {
        if (type == null || type.isAir()) {
            return false;
        }
        boolean isCrop = REGENERATING_CROPS.contains(type) || EXTRA_FARMABLE_BREAK.contains(type);
        boolean isTree = Tag.LOGS.isTagged(type) || Tag.LEAVES.isTagged(type) || Tag.SAPLINGS.isTagged(type);
        
        if (area != null && "oaklys".equalsIgnoreCase(area.id())) {
            return isTree;
        }
        if (area != null && ("farmhub".equalsIgnoreCase(area.id()) || "hub".equalsIgnoreCase(area.id()))) {
            return isCrop;
        }
        return isCrop || isTree;
    }

    private boolean isFarmablePlaceType(Material type) {
        if (type == null || type.isAir()) {
            return false;
        }
        if (REGENERATING_CROPS.contains(type) || EXTRA_FARMABLE_PLACE.contains(type)) {
            return true;
        }
        return Tag.SAPLINGS.isTagged(type);
    }

    private void warnFarmHubProtection(Player player, String message) {
        if (player == null || message == null || message.isBlank()) {
            return;
        }
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastWarn = this.lastProtectionWarnAtMs.get(playerId);
        if (lastWarn != null && now - lastWarn < 1200L) {
            return;
        }
        this.lastProtectionWarnAtMs.put(playerId, now);
        player.sendMessage(String.valueOf(ChatColor.RED) + message);
    }

    private int bonusGrowthSteps(ManagedAreaProfile area) {
        double multiplier = this.plugin.getConfig().getDouble(area.growthBase() + "multiplier", 4.0);
        if (!Double.isFinite(multiplier)) {
            multiplier = 4.0;
        }
        multiplier = Math.max(1.0, Math.min(8.0, multiplier));
        int extra = (int)Math.floor(multiplier) - 1;
        double fractional = multiplier - Math.floor(multiplier);
        if (fractional > 0.0 && ThreadLocalRandom.current().nextDouble() < fractional) {
            ++extra;
        }
        return Math.max(0, Math.min(extra, 6));
    }

    private ManagedAreaProfile resolveManagedArea(Location location) {
        for (ManagedAreaProfile area : MANAGED_AREAS) {
            if (!this.isInManagedArea(location, area)) continue;
            return area;
        }
        return null;
    }

    private boolean isInManagedArea(Location location, ManagedAreaProfile area) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        if (area == null) {
            return false;
        }
        String areaBase = area.areaBase();
        if (!this.plugin.getConfig().getBoolean(areaBase + "enabled", false)) {
            return false;
        }
        String worldName = this.plugin.getConfig().getString(areaBase + "world");
        if (worldName == null || worldName.isBlank()) {
            worldName = this.plugin.getConfig().getString(area.worldKey(), "world");
        }
        if (!location.getWorld().getName().equalsIgnoreCase(worldName)) {
            return false;
        }
        int x1 = this.plugin.getConfig().getInt(areaBase + "pos1.x");
        int y1 = this.plugin.getConfig().getInt(areaBase + "pos1.y");
        int z1 = this.plugin.getConfig().getInt(areaBase + "pos1.z");
        int x2 = this.plugin.getConfig().getInt(areaBase + "pos2.x");
        int y2 = this.plugin.getConfig().getInt(areaBase + "pos2.y");
        int z2 = this.plugin.getConfig().getInt(areaBase + "pos2.z");
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    private void tickAreaMaintenance() {
        if (this.areaMaintenanceTask == null || this.areaMaintenanceTask.isCancelled()) {
            return;
        }
        for (ManagedAreaProfile area : MANAGED_AREAS) {
            this.tickAreaMaintenance(area);
        }
    }

    private void tickAreaMaintenance(ManagedAreaProfile area) {
        int forcedSteps;
        long interval;
        AreaBounds bounds = this.getAreaBounds(area);
        if (bounds == null) {
            return;
        }
        long hydrationBudget = this.hydrationTickBudgetByArea.getOrDefault(area.id(), 0L) + 20L;
        long forcedGrowthBudget = this.forcedGrowthTickBudgetByArea.getOrDefault(area.id(), 0L) + 20L;
        boolean runHydration = false;
        boolean runForcedGrowth = false;
        if (this.isFarmlandForcedHydrationEnabled(area)) {
            interval = Math.max(20L, this.plugin.getConfig().getLong(area.farmlandBase() + "hydrate-interval-ticks", 40L));
            if (hydrationBudget >= interval) {
                runHydration = true;
                hydrationBudget = 0L;
            }
        } else {
            hydrationBudget = 0L;
        }
        if (this.isForcedGrowthEnabled(area)) {
            interval = Math.max(20L, this.plugin.getConfig().getLong(area.growthBase() + "force-interval-ticks", 20L));
            if (forcedGrowthBudget >= interval) {
                runForcedGrowth = true;
                forcedGrowthBudget = 0L;
            }
        } else {
            forcedGrowthBudget = 0L;
        }
        this.hydrationTickBudgetByArea.put(area.id(), hydrationBudget);
        this.forcedGrowthTickBudgetByArea.put(area.id(), forcedGrowthBudget);
        if (!runHydration && !runForcedGrowth) {
            return;
        }
        long maxHydrationScan = Math.max(1L, this.plugin.getConfig().getLong(area.farmlandBase() + "max-scan-blocks", 350000L));
        long maxGrowthScan = Math.max(1L, this.plugin.getConfig().getLong(area.growthBase() + "max-scan-blocks", 300000L));
        long maxAllowedScan = Math.max(maxHydrationScan, maxGrowthScan);
        if (bounds.volume() > maxAllowedScan) {
            // empty if block
        }
        int n = forcedSteps = runForcedGrowth ? Math.max(1, this.bonusGrowthSteps(area) + 1) : 0;
        if (runForcedGrowth && forcedSteps <= 0) {
            forcedSteps = 1;
        }
        this.scanAreaSlice(bounds, area, runHydration, runForcedGrowth, forcedSteps);
    }

    private void scanAreaSlice(AreaBounds bounds, ManagedAreaProfile area, boolean runHydration, boolean runForcedGrowth, int forcedSteps) {
        if (bounds == null || bounds.world() == null || bounds.volume() <= 0L) {
            return;
        }
        long scanBudget = Math.max(500L, this.plugin.getConfig().getLong(area.maintenanceBase() + "scan-blocks-per-run", 50000L));
        long toProcess = Math.min(bounds.volume(), scanBudget);
        long maintenanceScanIndex = this.maintenanceScanIndexByArea.getOrDefault(area.id(), 0L);
        if (maintenanceScanIndex >= bounds.volume()) {
            maintenanceScanIndex = 0L;
        }
        for (long i = 0L; i < toProcess; ++i) {
            long index = (maintenanceScanIndex + i) % bounds.volume();
            Block block = this.blockAtIndex(bounds, index);
            if (block == null) continue;
            if (runHydration && block.getType() == Material.FARMLAND) {
                this.forceHydrated(block);
            }
            if (!runForcedGrowth) continue;
            this.forceGrowth(block, forcedSteps, area);
        }
        this.maintenanceScanIndexByArea.put(area.id(), (maintenanceScanIndex + toProcess) % bounds.volume());
    }

    private Block blockAtIndex(AreaBounds bounds, long index) {
        long depth;
        long width = bounds.width();
        long layerSize = width * (depth = bounds.depth());
        if (layerSize <= 0L) {
            return null;
        }
        long yOffset = index / layerSize;
        long rem = index % layerSize;
        long zOffset = rem / width;
        long xOffset = rem % width;
        int x = bounds.minX() + (int)xOffset;
        int y = bounds.minY() + (int)yOffset;
        int z = bounds.minZ() + (int)zOffset;
        return bounds.world().getBlockAt(x, y, z);
    }

    private void forceGrowth(Block block, int steps, ManagedAreaProfile area) {
        if (block == null || steps <= 0) {
            return;
        }
        Material type = block.getType();
        if (!REGENERATING_CROPS.contains(type)) {
            return;
        }
        BlockData data = block.getBlockData();
        if (data instanceof Ageable) {
            Ageable ageable = (Ageable)data;
            int currentAge = ageable.getAge();
            int maxAge = ageable.getMaximumAge();
            int boostedAge = Math.min(maxAge, currentAge + steps);
            if (boostedAge > currentAge) {
                ageable.setAge(boostedAge);
                block.setBlockData((BlockData)ageable, false);
            }
            return;
        }
        if (this.isVerticalPlant(type)) {
            this.applyVerticalGrowthBoost(block.getLocation(), type, data, steps, area);
        }
    }

    private AreaBounds getAreaBounds(ManagedAreaProfile area) {
        World world;
        if (area == null) {
            return null;
        }
        String areaBase = area.areaBase();
        if (!this.plugin.getConfig().getBoolean(areaBase + "enabled", false)) {
            return null;
        }
        String worldName = this.plugin.getConfig().getString(areaBase + "world");
        if (worldName == null || worldName.isBlank()) {
            worldName = this.plugin.getConfig().getString(area.worldKey(), "world");
        }
        if ((world = Bukkit.getWorld((String)worldName)) == null) {
            return null;
        }
        int x1 = this.plugin.getConfig().getInt(areaBase + "pos1.x");
        int y1 = this.plugin.getConfig().getInt(areaBase + "pos1.y");
        int z1 = this.plugin.getConfig().getInt(areaBase + "pos1.z");
        int x2 = this.plugin.getConfig().getInt(areaBase + "pos2.x");
        int y2 = this.plugin.getConfig().getInt(areaBase + "pos2.y");
        int z2 = this.plugin.getConfig().getInt(areaBase + "pos2.z");
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);
        long width = maxX - minX + 1;
        long height = maxY - minY + 1;
        long depth = maxZ - minZ + 1;
        long volume = Math.max(0L, width * height * depth);
        return new AreaBounds(world, minX, maxX, minY, maxY, minZ, maxZ, width, depth, volume);
    }

    private void applyFarmingFortuneBonus(Player player, Material cropType, BlockData cropData, Collection<ItemStack> baseDrops, Location dropLocation) {
        Ageable ageable;
        Material primaryDrop = this.primaryFarmingDrop(cropType);
        if (primaryDrop == null || baseDrops == null || baseDrops.isEmpty()) {
            return;
        }
        if (cropData instanceof Ageable && (ageable = (Ageable)cropData).getAge() < ageable.getMaximumAge()) {
            return;
        }
        double farmingFortune = this.resolveFarmingFortune(player);
        if (!Double.isFinite(farmingFortune) || farmingFortune <= 0.0) {
            return;
        }
        int basePrimaryDrops = 0;
        for (ItemStack stack : baseDrops) {
            if (stack == null || stack.getType() != primaryDrop) continue;
            basePrimaryDrops += Math.max(0, stack.getAmount());
        }
        if (basePrimaryDrops <= 0) {
            return;
        }
        int extra = this.computeFortuneExtra(basePrimaryDrops, farmingFortune);
        extra += this.computeHarvesterEmbraceExtra(player, basePrimaryDrops);
        while (extra > 0) {
            int add = Math.min(extra, primaryDrop.getMaxStackSize());
            if (this.plugin.getFarmingContestManager() != null) {
                this.plugin.getFarmingContestManager().recordHarvest(player, new ItemStack(primaryDrop, add));
            }
            DropDeliveryUtil.giveToInventoryOrDrop(player, new ItemStack(primaryDrop, add), dropLocation);
            extra -= add;
        }
    }

    private int computeHarvesterEmbraceExtra(Player player, int basePrimaryDrops) {
        if (player == null || this.plugin == null || this.plugin.getCustomArmorManager() == null) {
            return 0;
        }
        int equippedPieces = this.plugin.getCustomArmorManager().countEquippedPieces(player, "harvester_embrace");
        return FarmingSetBonusUtil.computeHarvesterEmbraceExtraDrops(equippedPieces, basePrimaryDrops);
    }

    private double resolveFarmingFortune(Player player) {
        if (player == null || this.plugin == null) {
            return 0.0;
        }
        SkyblockCombatEngine combatEngine = this.plugin.getSkyblockCombatEngine();
        if (combatEngine != null) {
            return Math.max(0.0, combatEngine.stats(player).farmingFortune());
        }
        if (this.plugin.getSkyblockStatsManager() == null) {
            return this.regularFortuneFarmingBonus(player);
        }
        return Math.max(0.0, this.plugin.getSkyblockStatsManager().getFarmingFortune(player)) + this.regularFortuneFarmingBonus(player);
    }

    private double regularFortuneFarmingBonus(Player player) {
        if (player == null || this.plugin == null) {
            return 0.0;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) {
            return 0.0;
        }
        int fortuneLevel = held.getEnchantmentLevel(Enchantment.FORTUNE);
        if (fortuneLevel <= 0) {
            return 0.0;
        }
        double perLevel = this.plugin.getConfig().getDouble("skyblock-combat.regular-fortune-farming-fortune-per-level", 15.0);
        if (!Double.isFinite(perLevel) || perLevel <= 0.0) {
            return 0.0;
        }
        return (double)fortuneLevel * perLevel;
    }

    private int computeFortuneExtra(int baseDrops, double farmingFortune) {
        double expected = (double)baseDrops * (farmingFortune / 100.0);
        if (!Double.isFinite(expected) || expected <= 0.0) {
            return 0;
        }
        int guaranteed = (int)Math.floor(expected);
        double fractional = expected - (double)guaranteed;
        if (fractional > 0.0 && ThreadLocalRandom.current().nextDouble() < fractional) {
            ++guaranteed;
        }
        return Math.max(0, guaranteed);
    }

    private Material primaryFarmingDrop(Material cropType) {
        if (cropType == null) {
            return null;
        }
        return switch (cropType) {
            case Material.WHEAT -> Material.WHEAT;
            case Material.CARROTS -> Material.CARROT;
            case Material.POTATOES -> Material.POTATO;
            case Material.BEETROOTS -> Material.BEETROOT;
            case Material.NETHER_WART -> Material.NETHER_WART;
            case Material.COCOA -> Material.COCOA_BEANS;
            case Material.SUGAR_CANE -> Material.SUGAR_CANE;
            case Material.CACTUS -> Material.CACTUS;
            case Material.BAMBOO -> Material.BAMBOO;
            case Material.KELP, Material.KELP_PLANT -> Material.KELP;
            case Material.SWEET_BERRY_BUSH -> Material.SWEET_BERRIES;
            case Material.MELON -> Material.MELON_SLICE;
            case Material.PUMPKIN -> Material.PUMPKIN;
            case Material.CHORUS_PLANT, Material.CHORUS_FLOWER -> Material.CHORUS_FRUIT;
            default -> null;
        };
    }

    private void forceHydrated(Block block) {
        if (block == null || block.getType() != Material.FARMLAND) {
            return;
        }
        BlockData blockData = block.getBlockData();
        if (!(blockData instanceof Farmland)) {
            return;
        }
        Farmland farmland = (Farmland)blockData;
        int maxMoisture = farmland.getMaximumMoisture();
        if (farmland.getMoisture() >= maxMoisture) {
            return;
        }
        farmland.setMoisture(maxMoisture);
        block.setBlockData((BlockData)farmland, false);
    }

    private boolean isTreeBlock(Material type) {
        if (type == null || type.isAir()) {
            return false;
        }
        return Tag.LOGS.isTagged(type) || Tag.LEAVES.isTagged(type) || Tag.SAPLINGS.isTagged(type);
    }

    private boolean isPendingTreeBlock(Location location) {
        return location != null && this.pendingTreeBlocks.contains(this.blockKey(location));
    }

    private void markPendingTree(List<BlockSnapshot> snapshots, boolean pending) {
        if (snapshots == null || snapshots.isEmpty()) {
            return;
        }
        for (BlockSnapshot snapshot : snapshots) {
            if (snapshot == null || snapshot.location() == null) continue;
            String key = this.blockKey(snapshot.location());
            if (pending) {
                this.pendingTreeBlocks.add(key);
                continue;
            }
            this.pendingTreeBlocks.remove(key);
        }
    }

    private String blockKey(Location location) {
        if (location == null || location.getWorld() == null) {
            return "unknown";
        }
        return String.valueOf(location.getWorld().getUID()) + ":" + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private List<BlockSnapshot> treeSnapshotsFor(Block origin, ManagedAreaProfile area) {
        ArrayList<BlockSnapshot> snapshots = new ArrayList<BlockSnapshot>();
        if (origin == null || area == null) {
            return snapshots;
        }
        int maxTreeBlocks = Math.max(16, this.plugin.getConfig().getInt(area.treeRegenBase() + "max-tree-blocks", 256));
        ConcurrentHashMap.KeySetView visited = ConcurrentHashMap.newKeySet();
        ArrayList<Block> queue = new ArrayList<Block>();
        queue.add(origin);
        boolean foundLog = false;
        boolean foundLeafOrSapling = false;
        for (int index = 0; index < queue.size() && snapshots.size() < maxTreeBlocks; ++index) {
            String key;
            Block current = (Block)queue.get(index);
            if (current == null || !this.isInManagedArea(current.getLocation(), area) || !this.isTreeBlock(current.getType()) || !visited.add(key = this.blockKey(current.getLocation()))) continue;
            snapshots.add(new BlockSnapshot(current.getLocation().clone(), current.getBlockData().clone()));
            if (Tag.LOGS.isTagged(current.getType())) {
                foundLog = true;
            }
            if (Tag.LEAVES.isTagged(current.getType()) || Tag.SAPLINGS.isTagged(current.getType())) {
                foundLeafOrSapling = true;
            }
            for (int dx = -1; dx <= 1; ++dx) {
                for (int dy = -1; dy <= 1; ++dy) {
                    for (int dz = -1; dz <= 1; ++dz) {
                        Block next;
                        if (dx == 0 && dy == 0 && dz == 0 || (next = current.getRelative(dx, dy, dz)) == null || !this.isTreeBlock(next.getType()) || !this.isInManagedArea(next.getLocation(), area)) continue;
                        queue.add(next);
                    }
                }
            }
        }
        if (!foundLog || !foundLeafOrSapling) {
            return List.of();
        }
        snapshots.sort(Comparator.comparingInt(s -> s.location().getBlockY()));
        return snapshots;
    }

    private void regenerateTreeSnapshots(List<BlockSnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return;
        }
        for (BlockSnapshot snapshot : snapshots) {
            if (snapshot == null || snapshot.location() == null || snapshot.location().getWorld() == null || !snapshot.location().isWorldLoaded()) continue;
            snapshot.location().getBlock().setBlockData(snapshot.data(), false);
        }
    }

    private List<CropSnapshot> snapshotsFor(Block broken, ManagedAreaProfile area) {
        ArrayList<CropSnapshot> snapshots = new ArrayList<CropSnapshot>();
        if (broken == null) {
            return snapshots;
        }
        Material brokenType = broken.getType();
        BlockData brokenData = broken.getBlockData();
        if (this.isVerticalPlant(brokenType)) {
            Material normalized = this.normalizeVerticalCrop(brokenType);
            Block base = this.findBottomOfColumn(broken, normalized, area);
            snapshots.add(this.seedlingSnapshotOf(base, normalized));
            Block scan = base.getRelative(0, 1, 0);
            for (int i = 0; i < 16 && this.isInVerticalFamily(scan.getType(), normalized); ++i) {
                snapshots.add(this.clearSnapshotOf(scan));
                scan = scan.getRelative(0, 1, 0);
            }
            snapshots.sort(Comparator.comparingInt(s -> s.location().getBlockY()));
            return snapshots;
        }
        if (brokenData instanceof Bisected) {
            Bisected bisected = (Bisected)brokenData;
            Block base = bisected.getHalf() == Bisected.Half.TOP ? broken.getRelative(0, -1, 0) : broken;
            snapshots.add(this.seedlingSnapshotOf(base, base.getType()));
            Block top = base.getRelative(0, 1, 0);
            if (top.getType() == base.getType()) {
                snapshots.add(this.clearSnapshotOf(top));
            }
            snapshots.sort(Comparator.comparingInt(s -> s.location().getBlockY()));
            return snapshots;
        }
        snapshots.add(this.seedlingSnapshotOf(broken, brokenType));
        snapshots.sort(Comparator.comparingInt(s -> s.location().getBlockY()));
        return snapshots;
    }

    private boolean isVerticalPlant(Material material) {
        return material == Material.SUGAR_CANE || material == Material.CACTUS || material == Material.BAMBOO || material == Material.KELP || material == Material.KELP_PLANT || material == Material.CHORUS_PLANT || material == Material.CHORUS_FLOWER;
    }

    private void applyVerticalGrowthBoost(Location startLocation, Material cropType, BlockData data, int bonusSteps, ManagedAreaProfile area) {
        Block above;
        if (startLocation == null || startLocation.getWorld() == null || bonusSteps <= 0) {
            return;
        }
        Block start = startLocation.getBlock();
        if (!this.isInManagedArea(startLocation, area)) {
            return;
        }
        Material normalized = this.normalizeVerticalCrop(cropType);
        Block top = this.findTopOfColumn(start, normalized, area);
        for (int i = 0; i < bonusSteps && this.isInManagedArea((above = top.getRelative(0, 1, 0)).getLocation(), area) && this.canGrowInto(normalized, above.getType()); ++i) {
            if (this.isKelp(normalized)) {
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

    private Block findBottomOfColumn(Block start, Material cropType, ManagedAreaProfile area) {
        Block current = start;
        Block down;
        while (this.isInManagedArea((down = current.getRelative(0, -1, 0)).getLocation(), area) && this.isInVerticalFamily(down.getType(), cropType)) {
            current = down;
        }
        return current;
    }

    private Block findTopOfColumn(Block start, Material cropType, ManagedAreaProfile area) {
        Block current = start;
        Block up;
        while (this.isInManagedArea((up = current.getRelative(0, 1, 0)).getLocation(), area) && this.isInVerticalFamily(up.getType(), cropType)) {
            current = up;
        }
        return current;
    }

    private boolean canGrowInto(Material cropType, Material destination) {
        if (destination.isAir()) {
            return true;
        }
        return this.isKelp(cropType) && destination == Material.WATER;
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
        if (this.isKelp(cropType)) {
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
        return new CropSnapshot(block.getLocation().clone(), this.seedlingDataFor(block, originalType));
    }

    private CropSnapshot clearSnapshotOf(Block block) {
        return new CropSnapshot(block.getLocation().clone(), Material.AIR.createBlockData());
    }

    private BlockData seedlingDataFor(Block block, Material originalType) {
        BlockData data;
        Material seedlingType = this.resolveSeedlingType(block, originalType);
        BlockData blockData = data = seedlingType == originalType ? block.getBlockData().clone() : seedlingType.createBlockData();
        if (data instanceof Bisected) {
            Bisected bisected = (Bisected)data;
            bisected.setHalf(Bisected.Half.BOTTOM);
        }
        if (data instanceof Ageable) {
            Ageable ageable = (Ageable)data;
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
            if (snapshot == null || snapshot.location() == null || snapshot.location().getWorld() == null || !snapshot.location().isWorldLoaded()) continue;
            Block block = snapshot.location().getBlock();
            block.setBlockData(snapshot.data(), false);
        }
    }

    private record ManagedAreaProfile(String id, String displayName, String areaBase, String worldKey, String regenBase, String growthBase, String farmlandBase, String maintenanceBase, String treeRegenBase, String bypassPermission) {
    }

    private record AreaBounds(World world, int minX, int maxX, int minY, int maxY, int minZ, int maxZ, long width, long depth, long volume) {
    }

    private record BlockSnapshot(Location location, BlockData data) {
    }

    private record CropSnapshot(Location location, BlockData data) {
    }
}
