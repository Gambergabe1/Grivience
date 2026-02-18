package io.papermc.Grivience.dungeon;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.item.ArmorSetType;
import io.papermc.Grivience.item.CustomArmorType;
import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.item.CustomWeaponType;
import io.papermc.Grivience.item.RaijinCraftingItemType;
import io.papermc.Grivience.item.ReforgeStoneType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public final class DungeonSession {
    private static final String RUN_KEY_NAME = ChatColor.GOLD + "Temple Key";
    private static final String PUZZLE_GUIDE_NAME = ChatColor.AQUA + "Temple Puzzle Guide";

    private final GriviencePlugin plugin;
    private final DungeonManager manager;
    private final CustomItemService customItemService;
    private final UUID sessionId = UUID.randomUUID();
    private final NamespacedKey runKeyTag;
    private final NamespacedKey puzzleGuideTag;
    private final UUID partyId;
    private final Set<UUID> members;
    private final FloorConfig floor;
    private final int arenaSlot;
    private final List<Location> rooms;
    private final List<RoomType> encounterPlan;
    private final Map<UUID, TrackedMobInfo> trackedMobs = new HashMap<>();
    private final Map<Integer, ArenaLayout.Gate> gatesByIndex = new HashMap<>();
    private final Map<String, Integer> gateIndexByBlock = new HashMap<>();
    private final Set<Integer> openedGates = new HashSet<>();

    private BukkitTask countdownTask;
    private BukkitTask actionBarTask;
    private BukkitTask puzzleTask;

    private Location currentRoomCenter;
    private long startedAtMillis;
    private int deaths;
    private int clearedCombatRooms;
    private int clearedPuzzleRooms;
    private int clearedTreasureRooms;
    private int currentEncounterIndex = -1;
    private RoomType currentRoomType;
    private Integer pendingKeyGateIndex;
    private boolean bossActive;
    private boolean ended;
    private boolean transitioning;

    private final Map<String, Integer> sequencePadIndexByKey = new HashMap<>();
    private final List<Integer> sequenceOrder = new ArrayList<>();
    private int sequenceProgress;

    private final List<String> syncPlateKeys = new ArrayList<>();
    private int syncStableTicks;
    private int syncRequiredPlates;
    private int syncPoweredPlates;

    private String relicChestKey;
    private final double floorHealthScale;

    public DungeonSession(
            GriviencePlugin plugin,
            DungeonManager manager,
            CustomItemService customItemService,
            UUID partyId,
            Set<UUID> members,
            FloorConfig floor,
            int arenaSlot,
            ArenaLayout layout,
            List<RoomType> encounterPlan
    ) {
        this.plugin = plugin;
        this.manager = manager;
        this.customItemService = customItemService;
        this.partyId = partyId;
        this.members = new HashSet<>(members);
        this.floor = floor;
        this.arenaSlot = arenaSlot;
        this.runKeyTag = new NamespacedKey(plugin, "run-key");
        this.puzzleGuideTag = new NamespacedKey(plugin, "puzzle-guide");
        this.rooms = new ArrayList<>(layout.roomCenters());
        this.encounterPlan = List.copyOf(encounterPlan);
        this.currentRoomCenter = rooms.getFirst();
        this.floorHealthScale = 1.0D + (Math.max(1, floorNumber(floor.id())) - 1) * 0.45D;

        for (ArenaLayout.Gate gate : layout.gates()) {
            gatesByIndex.put(gate.index(), gate);
            for (Location location : gate.barrierBlocks()) {
                gateIndexByBlock.put(blockKey(location), gate.index());
            }
        }
    }

    public UUID sessionId() {
        return sessionId;
    }

    public UUID partyId() {
        return partyId;
    }

    public Set<UUID> members() {
        return Set.copyOf(members);
    }

    public FloorConfig floor() {
        return floor;
    }

    public int arenaSlot() {
        return arenaSlot;
    }

    public void start(int countdownSeconds) {
        teleportMembers(currentRoomCenter);
        broadcast("Entering " + floor.id() + " - " + floor.displayName() + ".");

        countdownTask = new BukkitRunnable() {
            private int remaining = countdownSeconds;

            @Override
            public void run() {
                if (ended) {
                    cancel();
                    return;
                }
                if (remaining <= 0) {
                    cancel();
                    countdownTask = null;
                    startedAtMillis = System.currentTimeMillis();
                    startActionBarTicker();
                    openGate(0);
                    broadcast("First gate opened. Advance into room 1.");
                    beginEncounterRoom(0);
                    return;
                }

                broadcast("Torii gate opens in " + remaining + "...");
                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void abandon(String actorName) {
        finish(false, actorName + " abandoned the dungeon.");
    }

    public void forceEnd(String reason) {
        finish(false, reason);
    }

    public void recordDeath(UUID playerId) {
        if (ended || !members.contains(playerId)) {
            return;
        }
        deaths++;
        broadcast(manager.nameOf(playerId) + " died. Total deaths: " + deaths);
    }

    public Location respawnLocationFor(UUID playerId) {
        if (ended || !members.contains(playerId)) {
            return null;
        }
        return currentRoomCenter.clone();
    }

    public void handlePlayerQuit(UUID playerId) {
        if (ended || !members.contains(playerId)) {
            return;
        }
        Player quitting = Bukkit.getPlayer(playerId);
        boolean hadKey = quitting != null && removeSessionRunKeysFromPlayer(quitting);
        if (hadKey && pendingKeyGateIndex != null) {
            Player reassigned = grantRunKeyToSingleCarrier(playerId);
            if (reassigned != null) {
                broadcast("Temple Key reassigned to " + reassigned.getName() + ".");
            }
        }
        if (onlineMemberCount() == 0) {
            finish(false, "All party members left the dungeon.");
        }
    }

    public void handlePlayerJoin(Player player) {
        if (ended || !members.contains(player.getUniqueId())) {
            return;
        }
        if (currentRoomCenter == null) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && !ended) {
                player.teleport(currentRoomCenter);
                player.sendMessage(ChatColor.GOLD + "[Dungeon] " + ChatColor.YELLOW + "You rejoined your active run.");
                if (currentRoomType != null && currentRoomType.isPuzzle()) {
                    givePuzzleGuideToPlayer(player, currentRoomType);
                }
            }
        }, 20L);
    }

    public void handleTrackedMobDeath(LivingEntity entity, EntityDeathEvent deathEvent) {
        if (ended) {
            return;
        }
        TrackedMobInfo mobInfo = trackedMobs.remove(entity.getUniqueId());
        if (mobInfo == null) {
            return;
        }
        if (deathEvent != null) {
            deathEvent.getDrops().clear();
            deathEvent.setDroppedExp(experienceForTrackedMob(mobInfo));
        }
        rollCustomDrops(entity, mobInfo);
        if (!trackedMobs.isEmpty()) {
            return;
        }

        if (bossActive) {
            finish(true, "Shogun defeated.");
            return;
        }

        if (currentRoomType == RoomType.COMBAT && !transitioning) {
            clearCurrentRoom("Dojo clash cleared.");
        }
    }

    public void handlePlayerMove(Player player, Location from, Location to) {
        if (ended || transitioning || bossActive || currentRoomType != RoomType.PUZZLE_SEQUENCE) {
            return;
        }
        if (from == null || to == null || from.getWorld() == null || to.getWorld() == null) {
            return;
        }
        if (!Objects.equals(from.getWorld().getUID(), to.getWorld().getUID())) {
            return;
        }

        String previousKey = blockKey(from.getWorld(), from.getBlockX(), from.getBlockY() - 1, from.getBlockZ());
        String currentKey = blockKey(to.getWorld(), to.getBlockX(), to.getBlockY() - 1, to.getBlockZ());
        if (Objects.equals(previousKey, currentKey)) {
            return;
        }

        Integer steppedPadIndex = sequencePadIndexByKey.get(currentKey);
        if (steppedPadIndex == null || sequenceOrder.isEmpty()) {
            return;
        }

        int expected = sequenceOrder.get(sequenceProgress);
        if (steppedPadIndex == expected) {
            sequenceProgress++;
            if (sequenceProgress >= sequenceOrder.size()) {
                clearCurrentRoom("Ofuda sequence solved.");
                return;
            }
            broadcast(player.getName() + " hit the correct pad (" + sequenceProgress + "/" + sequenceOrder.size() + ").");
            return;
        }

        sequenceProgress = 0;
        broadcast(player.getName() + " stepped on the wrong pad. Sequence reset.");
    }

    public void handlePlayerInteract(Player player, Action action, Block clickedBlock) {
        if (ended || clickedBlock == null) {
            return;
        }
        if (action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Integer gateIndex = gateIndexByBlock.get(blockKey(clickedBlock.getLocation()));
        if (gateIndex != null && !openedGates.contains(gateIndex)) {
            handleGateUnlockAttempt(player, gateIndex);
            return;
        }

        if (bossActive || transitioning || currentRoomType != RoomType.TREASURE) {
            return;
        }

        Material type = clickedBlock.getType();
        if (type != Material.CHEST && type != Material.TRAPPED_CHEST) {
            return;
        }

        String key = blockKey(clickedBlock.getLocation());
        if (relicChestKey != null && relicChestKey.equals(key)) {
            player.getInventory().addItem(new ItemStack(Material.EMERALD, 8));
            player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 3));
            clearCurrentRoom("Sacred relic chest found.");
            return;
        }

        broadcast(player.getName() + " opened a decoy chest. Yokai ambush!");
        spawnDungeonMob(floor.mobHealthMultiplier() * floorHealthScale * 1.2D, floor.mobDamageTier() + 1);
    }

    private void handleGateUnlockAttempt(Player player, int gateIndex) {
        if (gateIndex == 0) {
            openGate(0);
            return;
        }
        if (pendingKeyGateIndex == null || pendingKeyGateIndex != gateIndex) {
            player.sendMessage(ChatColor.RED + "This gate is sealed. Clear the current room first.");
            return;
        }
        if (!consumeRunKey(player)) {
            player.sendMessage(ChatColor.RED + "You need a Temple Key to open this gate.");
            return;
        }

        openGate(gateIndex);
        pendingKeyGateIndex = null;

        if (gateIndex < encounterPlan.size()) {
            beginEncounterRoom(gateIndex);
        } else {
            beginBossRoom();
        }
        broadcast(player.getName() + " unlocked the gate with a Temple Key.");
    }
    private void beginEncounterRoom(int index) {
        if (ended) {
            return;
        }
        if (index < 0 || index >= encounterPlan.size()) {
            return;
        }

        cancelPuzzleTask();
        resetObjectiveState();

        bossActive = false;
        transitioning = false;
        currentEncounterIndex = index;
        currentRoomType = encounterPlan.get(index);
        currentRoomCenter = rooms.get(index + 1);

        switch (currentRoomType) {
            case COMBAT -> {
                int roomNumber = index + 1;
                broadcast("Dojo clash room " + roomNumber + "/" + encounterPlan.size() + " started.");
                spawnCombatWave(roomNumber);
            }
            case PUZZLE_SEQUENCE -> {
                broadcast("Ofuda sequence trial. Step on pads in the shown order.");
                givePuzzleGuide(RoomType.PUZZLE_SEQUENCE);
                setupSequencePuzzle();
            }
            case PUZZLE_SYNC -> {
                givePuzzleGuide(RoomType.PUZZLE_SYNC);
                setupSyncPuzzle();
            }
            case TREASURE -> {
                broadcast("Kura vault room. Find and open the relic chest.");
                setupTreasureRoom();
            }
        }
    }

    private void beginBossRoom() {
        if (ended) {
            return;
        }

        cancelPuzzleTask();
        resetObjectiveState();

        transitioning = false;
        bossActive = true;
        currentRoomType = null;
        currentRoomCenter = rooms.getLast();
        broadcast("Shogun encounter started: " + floor.bossName());

        spawnMob(floor.bossType(), floor.bossHealthMultiplier() * floorHealthScale, floor.mobDamageTier() + 4, true);
        int minions = Math.max(2, onlineMemberCount() + (encounterPlan.size() / 3));
        for (int i = 0; i < minions; i++) {
            spawnDungeonMob(
                    floor.mobHealthMultiplier() * floorHealthScale * 1.8D,
                    floor.mobDamageTier() + 2,
                    distributedSpawnInRoom(currentRoomCenter, i, minions)
            );
        }

        if (trackedMobs.isEmpty()) {
            finish(true, "Shogun defeated.");
        }
    }

    private void spawnCombatWave(int roomNumber) {
        int online = onlineMemberCount();
        int mobCount = floor.baseMobCount() + (roomNumber - 1) * 2 + Math.max(0, online - 1);
        double roomHealthScale = floor.mobHealthMultiplier() * floorHealthScale * (1.0D + ((roomNumber - 1) * 0.16D));
        int roomDamageTier = floor.mobDamageTier() + (roomNumber / 2);

        for (int i = 0; i < mobCount; i++) {
            spawnDungeonMob(roomHealthScale, roomDamageTier, distributedSpawnInRoom(currentRoomCenter, i, mobCount));
        }
        if (trackedMobs.isEmpty()) {
            clearCurrentRoom("Dojo clash cleared.");
        }
    }

    private void spawnDungeonMob(double healthScale, int damageTier) {
        spawnDungeonMob(healthScale, damageTier, null);
    }

    private void spawnDungeonMob(double healthScale, int damageTier, Location preferredSpawn) {
        if (floor.folkloreMobs()) {
            spawnFolkloreMob(randomYokaiType(damageTier), healthScale, damageTier, preferredSpawn);
            return;
        }
        spawnMob(randomMobType(), healthScale, damageTier, false, preferredSpawn);
    }

    private void setupSequencePuzzle() {
        sequencePadIndexByKey.clear();
        sequenceOrder.clear();
        sequenceProgress = 0;

        if (currentRoomCenter == null || currentRoomCenter.getWorld() == null) {
            clearCurrentRoom("Puzzle skipped (room unavailable).");
            return;
        }

        World world = currentRoomCenter.getWorld();
        int centerX = currentRoomCenter.getBlockX();
        int floorY = currentRoomCenter.getBlockY() - 1;
        int centerZ = currentRoomCenter.getBlockZ();

        int[][] offsets = {
                {-3, -2},
                {-1, -2},
                {1, -2},
                {3, -2}
        };
        Material[] colors = {
                Material.RED_CONCRETE,
                Material.LIME_CONCRETE,
                Material.BLUE_CONCRETE,
                Material.YELLOW_CONCRETE
        };

        for (int i = 0; i < offsets.length; i++) {
            int x = centerX + offsets[i][0];
            int z = centerZ + offsets[i][1];
            world.getBlockAt(x, floorY, z).setType(colors[i], false);
            sequencePadIndexByKey.put(blockKey(world, x, floorY, z), i);
        }

        List<Integer> base = new ArrayList<>(List.of(0, 1, 2, 3));
        Collections.shuffle(base);
        sequenceOrder.addAll(base);

        broadcast("Ofuda order: " + formatSequenceOrder(sequenceOrder));
    }

    private void setupSyncPuzzle() {
        syncPlateKeys.clear();
        syncStableTicks = 0;
        syncRequiredPlates = 0;
        syncPoweredPlates = 0;

        if (currentRoomCenter == null || currentRoomCenter.getWorld() == null) {
            clearCurrentRoom("Puzzle skipped (room unavailable).");
            return;
        }

        World world = currentRoomCenter.getWorld();
        int centerX = currentRoomCenter.getBlockX();
        int floorY = currentRoomCenter.getBlockY() - 1;
        int centerZ = currentRoomCenter.getBlockZ();

        int[][] offsets = {
                {-4, -4},
                {-4, 4},
                {4, -4},
                {4, 4}
        };
        for (int[] offset : offsets) {
            int x = centerX + offset[0];
            int z = centerZ + offset[1];
            // Keep solid support at floor height and place plate on top so players cannot fall through.
            world.getBlockAt(x, floorY, z).setType(Material.BAMBOO_PLANKS, false);
            world.getBlockAt(x, floorY + 1, z).setType(Material.STONE_PRESSURE_PLATE, false);
            syncPlateKeys.add(blockKey(world, x, floorY + 1, z));
        }
        syncRequiredPlates = computeSyncRequiredPlates();
        broadcast("Shrine bell sync trial. Hold " + syncRequiredPlates + " bell plate(s) active for 2 seconds.");

        puzzleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (ended || transitioning || bossActive || currentRoomType != RoomType.PUZZLE_SYNC) {
                    cancel();
                    return;
                }
                syncRequiredPlates = computeSyncRequiredPlates();
                syncPoweredPlates = countPoweredSyncPlates();
                if (syncPoweredPlates >= syncRequiredPlates) {
                    syncStableTicks++;
                    if (syncStableTicks >= 2) {
                        clearCurrentRoom("Shrine bell sync solved.");
                    }
                } else {
                    syncStableTicks = 0;
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void setupTreasureRoom() {
        relicChestKey = null;
        if (currentRoomCenter == null || currentRoomCenter.getWorld() == null) {
            clearCurrentRoom("Treasure skipped (room unavailable).");
            return;
        }

        World world = currentRoomCenter.getWorld();
        int centerX = currentRoomCenter.getBlockX();
        int floorY = currentRoomCenter.getBlockY();
        int centerZ = currentRoomCenter.getBlockZ();

        List<Location> chestLocations = List.of(
                new Location(world, centerX - 3, floorY, centerZ + 2),
                new Location(world, centerX, floorY, centerZ + 4),
                new Location(world, centerX + 3, floorY, centerZ + 2)
        );

        int relicIndex = ThreadLocalRandom.current().nextInt(chestLocations.size());
        for (int i = 0; i < chestLocations.size(); i++) {
            Location chestLocation = chestLocations.get(i);
            Material chestType = i == relicIndex ? Material.TRAPPED_CHEST : Material.CHEST;
            world.getBlockAt(chestLocation).setType(chestType, false);
            if (i == relicIndex) {
                relicChestKey = blockKey(chestLocation);
                world.getBlockAt(chestLocation.clone().subtract(0, 1, 0)).setType(Material.GOLD_BLOCK, false);
            } else {
                world.getBlockAt(chestLocation.clone().subtract(0, 1, 0)).setType(Material.STONE_BRICKS, false);
            }
        }
    }

    private int countPoweredSyncPlates() {
        if (currentRoomCenter == null || currentRoomCenter.getWorld() == null || syncPlateKeys.isEmpty()) {
            return 0;
        }
        World world = currentRoomCenter.getWorld();
        int powered = 0;
        for (String key : syncPlateKeys) {
            Block block = resolveBlockFromKey(world, key);
            if (block == null) {
                continue;
            }
            if (block.getBlockData() instanceof Powerable powerable && powerable.isPowered()) {
                powered++;
            }
        }
        return powered;
    }

    private int computeSyncRequiredPlates() {
        if (syncPlateKeys.isEmpty()) {
            return 1;
        }
        int online = Math.max(1, onlineMemberCount());
        return Math.max(1, Math.min(syncPlateKeys.size(), online));
    }

    private void clearCurrentRoom(String reason) {
        if (ended || transitioning || bossActive) {
            return;
        }
        transitioning = true;
        cancelPuzzleTask();

        if (currentRoomType == RoomType.COMBAT) {
            clearedCombatRooms++;
        } else if (currentRoomType != null && currentRoomType.isPuzzle()) {
            clearedPuzzleRooms++;
        } else if (currentRoomType == RoomType.TREASURE) {
            clearedTreasureRooms++;
        }

        int gateToUnlock = currentEncounterIndex + 1;
        pendingKeyGateIndex = gateToUnlock;
        Player keyCarrier = grantRunKeyToSingleCarrier(null);

        if (gateToUnlock < encounterPlan.size()) {
            int nextRoom = gateToUnlock + 1;
            if (keyCarrier != null) {
                broadcast(reason + " Temple Key granted to " + keyCarrier.getName() + ". Unlock the gate to room " + nextRoom + ".");
            } else {
                broadcast(reason + " Temple Key could not be assigned. Unlock the gate to room " + nextRoom + " once someone has inventory space.");
            }
        } else {
            if (keyCarrier != null) {
                broadcast(reason + " Temple Key granted to " + keyCarrier.getName() + ". Unlock the sanctum gate.");
            } else {
                broadcast(reason + " Temple Key could not be assigned. Unlock the sanctum gate once someone has inventory space.");
            }
        }
    }
    private void spawnMob(EntityType type, double healthScale, int damageTier, boolean boss) {
        spawnMob(type, healthScale, damageTier, boss, null);
    }

    private void spawnMob(EntityType type, double healthScale, int damageTier, boolean boss, Location preferredSpawn) {
        if (currentRoomCenter == null || currentRoomCenter.getWorld() == null) {
            return;
        }
        Location spawnLocation = preferredSpawn != null
                ? preferredSpawn
                : randomSpawnInRoom(currentRoomCenter, Math.max(3, (floor.roomSize() / 2) - 3));
        if (spawnLocation == null) {
            spawnLocation = fallbackSpawnInRoom(currentRoomCenter);
        }
        if (spawnLocation == null) {
            return;
        }
        Entity entity = currentRoomCenter.getWorld().spawnEntity(spawnLocation, type);
        if (!(entity instanceof LivingEntity living)) {
            entity.remove();
            return;
        }

        applyStats(living, healthScale, damageTier, boss);
        if (boss) {
            living.customName(Component.text(floor.bossName(), NamedTextColor.DARK_RED));
            living.setCustomNameVisible(true);
        } else {
            living.customName(Component.text("Temple Yokai", NamedTextColor.GRAY));
            living.setCustomNameVisible(true);
        }

        if (living instanceof Mob mob) {
            Player randomTarget = randomOnlineMember();
            if (randomTarget != null) {
                mob.setTarget(randomTarget);
            }
        }

        UUID mobId = living.getUniqueId();
        trackedMobs.put(mobId, new TrackedMobInfo(boss, null));
        manager.trackMob(mobId, this);
    }

    private void spawnFolkloreMob(YokaiType yokai, double healthScale, int damageTier, Location preferredSpawn) {
        if (currentRoomCenter == null || currentRoomCenter.getWorld() == null) {
            return;
        }
        Location spawnLocation = preferredSpawn != null
                ? preferredSpawn
                : randomSpawnInRoom(currentRoomCenter, Math.max(3, (floor.roomSize() / 2) - 3));
        if (spawnLocation == null) {
            spawnLocation = fallbackSpawnInRoom(currentRoomCenter);
        }
        if (spawnLocation == null) {
            return;
        }
        Entity entity = currentRoomCenter.getWorld().spawnEntity(spawnLocation, yokai.entityType());
        if (!(entity instanceof LivingEntity living)) {
            entity.remove();
            return;
        }

        int scaledTier = Math.max(0, damageTier);
        applyStats(living, healthScale * yokai.healthScale(), Math.max(0, (int) Math.round(scaledTier * yokai.damageScale())), false);
        living.customName(Component.text(yokai.displayName(), yokai.nameColor()));
        living.setCustomNameVisible(true);
        yokai.applyLoadout(living, scaledTier);
        yokai.applySpecialEffects(living, scaledTier);

        if (living instanceof Mob mob) {
            Player randomTarget = randomOnlineMember();
            if (randomTarget != null) {
                mob.setTarget(randomTarget);
            }
        }

        UUID mobId = living.getUniqueId();
        trackedMobs.put(mobId, new TrackedMobInfo(false, yokai));
        manager.trackMob(mobId, this);
    }

    private void rollCustomDrops(LivingEntity entity, TrackedMobInfo mobInfo) {
        if (mobInfo.boss()) {
            rollBossMaterialDrops(entity);
            rollBossArmorDrop(entity);
            rollBossReforgeStoneDrop(entity);
            return;
        }
        rollMobWeaponDrop(entity, mobInfo.yokaiType());
        rollMobArmorDrop(entity, mobInfo.yokaiType());
        rollMobReforgeStoneDrop(entity, mobInfo.yokaiType());
    }

    private void rollMobWeaponDrop(LivingEntity entity, YokaiType yokaiType) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        CustomWeaponType weapon;
        double chance = customItemService.mobWeaponDropChance(yokaiType);

        if (yokaiType == null) {
            weapon = randomDungeonWeapon();
        } else {
            weapon = switch (yokaiType) {
                case ONI_BRUTE -> CustomWeaponType.ONI_CLEAVER;
                case TENGU_SKIRMISHER -> random.nextBoolean()
                        ? CustomWeaponType.TENGU_GALEBLADE
                        : CustomWeaponType.TENGU_STORMBOW;
                case KAPPA_RAIDER -> CustomWeaponType.KAPPA_TIDEBREAKER;
                case ONRYO_WRAITH -> CustomWeaponType.ONRYO_SPIRITBLADE;
                case JOROGUMO_WEAVER -> CustomWeaponType.JOROGUMO_STINGER;
                case KITSUNE_TRICKSTER -> random.nextBoolean()
                        ? CustomWeaponType.KITSUNE_FANG
                        : CustomWeaponType.KITSUNE_DAWNBOW;
                case GASHADOKURO_SENTINEL -> CustomWeaponType.GASHADOKURO_NODACHI;
            };
        }

        if (random.nextDouble() <= chance) {
            ItemStack weaponDrop = customItemService.createWeapon(weapon);
            awardLoot(entity, weaponDrop);
        }
    }

    private void rollMobArmorDrop(LivingEntity entity, YokaiType yokaiType) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextDouble() > customItemService.mobArmorDropChance(yokaiType)) {
            return;
        }
        ArmorSetType setType = armorSetForYokai(yokaiType);
        CustomArmorType piece = randomArmorPiece(setType);
        if (piece == null) {
            return;
        }
        ItemStack armorDrop = customItemService.createArmor(piece);
        awardLoot(entity, armorDrop);
    }

    private void rollMobReforgeStoneDrop(LivingEntity entity, YokaiType yokaiType) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextDouble() > customItemService.mobReforgeStoneChance()) {
            return;
        }
        ReforgeStoneType stoneType = randomReforgeStone(yokaiType, false);
        ItemStack stone = customItemService.createReforgeStone(stoneType);
        awardLoot(entity, stone);
    }

    private void rollBossMaterialDrops(LivingEntity entity) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        if (random.nextDouble() <= customItemService.stormSigilChance()) {
            ItemStack sigil = customItemService.createCraftingItem(RaijinCraftingItemType.STORM_SIGIL);
            sigil.setAmount(1 + random.nextInt(2));
            awardLoot(entity, sigil);
        }
        if (random.nextDouble() <= customItemService.thunderEssenceChance()) {
            ItemStack essence = customItemService.createCraftingItem(RaijinCraftingItemType.THUNDER_ESSENCE);
            awardLoot(entity, essence);
        }
        if (random.nextDouble() <= customItemService.raijinCoreChance()) {
            ItemStack core = customItemService.createCraftingItem(RaijinCraftingItemType.RAIJIN_CORE);
            awardLoot(entity, core);
            broadcast(ChatColor.LIGHT_PURPLE + "A Raijin Core has dropped from the Shogun!");
        }
        if (random.nextDouble() <= customItemService.flyingRaijinChance()) {
            ItemStack flyingRaijin = customItemService.createWeapon(CustomWeaponType.FLYING_RAIJIN);
            awardLoot(entity, flyingRaijin);
            broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "Legendary Drop! Flying Raijin has manifested!");
        }
    }

    private void rollBossArmorDrop(LivingEntity entity) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextDouble() > customItemService.bossArmorChance()) {
            return;
        }

        ArmorSetType setType = randomArmorSet();
        CustomArmorType piece = randomArmorPiece(setType);
        if (piece == null) {
            return;
        }
        awardLoot(entity, customItemService.createArmor(piece));
        broadcast(ChatColor.AQUA + "A " + setType.displayName() + " armor piece dropped from the Shogun!");
    }

    private void rollBossReforgeStoneDrop(LivingEntity entity) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextDouble() > customItemService.bossReforgeStoneChance()) {
            return;
        }

        ReforgeStoneType stoneType = randomReforgeStone(null, true);
        awardLoot(entity, customItemService.createReforgeStone(stoneType));
        broadcast(ChatColor.AQUA + "A " + stoneType.reforgeType().displayName() + " Reforge Stone dropped!");
    }

    private void awardLoot(LivingEntity source, ItemStack item) {
        Player killer = source.getKiller();
        String display = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                ? item.getItemMeta().getDisplayName()
                : item.getType().name();

        if (killer != null && members.contains(killer.getUniqueId())) {
            Map<Integer, ItemStack> leftovers = killer.getInventory().addItem(item);
            leftovers.values().forEach(leftover -> killer.getWorld().dropItemNaturally(source.getLocation(), leftover));
            killer.sendMessage(ChatColor.GOLD + "[Loot] " + ChatColor.YELLOW + "You found " + display + ChatColor.YELLOW + ".");
            return;
        }
        if (source.getWorld() != null) {
            source.getWorld().dropItemNaturally(source.getLocation(), item);
        }
    }

    private void applyStats(LivingEntity living, double healthScale, int damageTier, boolean boss) {
        AttributeInstance maxHealth = living.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            double scaledHealth = Math.max(
                    8.0D,
                    maxHealth.getBaseValue() * healthScale * (1.0D + ((members.size() - 1) * 0.12D))
            );
            maxHealth.setBaseValue(scaledHealth);
            living.setHealth(scaledHealth);
        }

        AttributeInstance attack = living.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attack != null) {
            double scaledDamage = Math.max(1.0D, attack.getBaseValue() * (1.0D + (damageTier * 0.25D)));
            attack.setBaseValue(scaledDamage);
        }

        int speedAmplifier = Math.min(2, damageTier / 3);
        if (speedAmplifier > 0) {
            living.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, speedAmplifier, false, false));
        }
        if (boss) {
            living.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1, false, false));
            living.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1, false, false));
        }
        living.setRemoveWhenFarAway(false);
    }

    private void openGate(int gateIndex) {
        ArenaLayout.Gate gate = gatesByIndex.get(gateIndex);
        if (gate == null) {
            return;
        }
        openedGates.add(gateIndex);
        for (Location location : gate.barrierBlocks()) {
            if (location.getWorld() == null) {
                continue;
            }
            location.getWorld().getBlockAt(location).setType(Material.AIR, false);
        }
    }

    private Player grantRunKeyToSingleCarrier(UUID excludedPlayerId) {
        removeSessionRunKeysFromOnlineMembers();
        List<Player> online = onlineMembers(excludedPlayerId);
        if (online.isEmpty()) {
            return null;
        }
        Collections.shuffle(online);
        for (Player player : online) {
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(createRunKey());
            if (leftovers.isEmpty()) {
                player.sendMessage(ChatColor.GOLD + "[Dungeon] " + ChatColor.YELLOW + "You carry the Temple Key.");
                return player;
            }
        }

        Player fallback = online.getFirst();
        fallback.getWorld().dropItemNaturally(fallback.getLocation(), createRunKey());
        fallback.sendMessage(ChatColor.RED + "Your inventory is full. Temple Key dropped at your feet.");
        return fallback;
    }

    private ItemStack createRunKey() {
        ItemStack key = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta meta = key.getItemMeta();
        meta.setDisplayName(RUN_KEY_NAME);
        meta.setLore(List.of(
                ChatColor.GRAY + "Opens a sealed temple gate",
                ChatColor.DARK_GRAY + sessionToken()
        ));
        meta.getPersistentDataContainer().set(runKeyTag, PersistentDataType.STRING, sessionId.toString());
        key.setItemMeta(meta);
        return key;
    }

    private boolean consumeRunKey(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (!isRunKey(item)) {
                continue;
            }
            int amount = item.getAmount();
            if (amount <= 1) {
                inventory.setItem(slot, null);
            } else {
                item.setAmount(amount - 1);
                inventory.setItem(slot, item);
            }
            return true;
        }
        return false;
    }

    private boolean isRunKey(ItemStack item) {
        if (item == null || item.getType() != Material.TRIPWIRE_HOOK || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        String taggedSession = meta.getPersistentDataContainer().get(runKeyTag, PersistentDataType.STRING);
        return Objects.equals(taggedSession, sessionId.toString());
    }

    private void startActionBarTicker() {
        actionBarTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (ended) {
                    cancel();
                    return;
                }
                long elapsedSeconds = startedAtMillis == 0L ? 0L : (System.currentTimeMillis() - startedAtMillis) / 1000L;

                String stage;
                if (bossActive) {
                    stage = "Shogun";
                } else if (currentEncounterIndex >= 0 && currentEncounterIndex < encounterPlan.size()) {
                    RoomType type = encounterPlan.get(currentEncounterIndex);
                    stage = "Room " + (currentEncounterIndex + 1) + "/" + encounterPlan.size() + " " + type.displayName();
                } else {
                    stage = "Preparing";
                }

                String puzzleProgress = "";
                if (!bossActive && currentRoomType == RoomType.PUZZLE_SEQUENCE && !sequenceOrder.isEmpty()) {
                    puzzleProgress = " | Seq " + sequenceProgress + "/" + sequenceOrder.size();
                } else if (!bossActive && currentRoomType == RoomType.PUZZLE_SYNC) {
                    int required = Math.max(1, syncRequiredPlates);
                    puzzleProgress = " | Sync " + syncPoweredPlates + "/" + required + " | Hold " + syncStableTicks + "/2";
                }

                String gateState = pendingKeyGateIndex == null ? "" : " | Key Needed";
                Component actionBar = Component.text(
                        floor.id() + " | " + stage + puzzleProgress + gateState + " | Deaths " + deaths + " | " + formatTime(elapsedSeconds),
                        NamedTextColor.GOLD
                );
                forEachOnlineMember(player -> player.sendActionBar(actionBar));
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void finish(boolean success, String reason) {
        if (ended) {
            return;
        }
        ended = true;
        cancelTasks();
        despawnTrackedMobs();
        removeSessionRunKeysFromMembers();

        long elapsedSeconds = startedAtMillis == 0L ? 0L : (System.currentTimeMillis() - startedAtMillis) / 1000L;
        int score = success ? calculateScore(elapsedSeconds) : 0;
        String grade = success ? gradeForScore(score) : "F";

        manager.finalizeSession(this, success, grade, score, elapsedSeconds, reason);
    }

    private void cancelTasks() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        if (actionBarTask != null) {
            actionBarTask.cancel();
            actionBarTask = null;
        }
        cancelPuzzleTask();
    }

    private void cancelPuzzleTask() {
        if (puzzleTask != null) {
            puzzleTask.cancel();
            puzzleTask = null;
        }
    }

    private void despawnTrackedMobs() {
        Set<UUID> copy = Set.copyOf(trackedMobs.keySet());
        trackedMobs.clear();
        for (UUID mobId : copy) {
            manager.untrackMob(mobId);
            Entity entity = Bukkit.getEntity(mobId);
            if (entity != null && !entity.isDead()) {
                entity.remove();
            }
        }
    }
    private void resetObjectiveState() {
        sequencePadIndexByKey.clear();
        sequenceOrder.clear();
        sequenceProgress = 0;
        syncPlateKeys.clear();
        syncStableTicks = 0;
        syncRequiredPlates = 0;
        syncPoweredPlates = 0;
        relicChestKey = null;
    }

    private void givePuzzleGuide(RoomType roomType) {
        forEachOnlineMember(player -> givePuzzleGuideToPlayer(player, roomType));
    }

    private void givePuzzleGuideToPlayer(Player player, RoomType roomType) {
        if (hasPuzzleGuide(player)) {
            return;
        }
        ItemStack guide = createPuzzleGuideBook(roomType);
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(guide);
        leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        player.sendMessage(ChatColor.AQUA + "You received a Temple Puzzle Guide.");
    }

    private boolean hasPuzzleGuide(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() != Material.WRITTEN_BOOK || !item.hasItemMeta()) {
                continue;
            }
            ItemMeta meta = item.getItemMeta();
            Byte marker = meta.getPersistentDataContainer().get(puzzleGuideTag, PersistentDataType.BYTE);
            if (marker != null && marker == (byte) 1) {
                return true;
            }
        }
        return false;
    }

    private ItemStack createPuzzleGuideBook(RoomType focusRoom) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        ItemMeta baseMeta = book.getItemMeta();
        if (!(baseMeta instanceof BookMeta meta)) {
            return book;
        }

        meta.setTitle(ChatColor.stripColor(PUZZLE_GUIDE_NAME));
        meta.setAuthor("Shrine Scribe");
        meta.addPage(
                ChatColor.GOLD + "Temple Puzzles\n\n"
                        + ChatColor.BLACK + "This guide explains puzzle rooms.\n\n"
                        + "Focus Room:\n"
                        + focusLabel(focusRoom)
        );
        meta.addPage(
                ChatColor.BLUE + "Shrine Bell Sync\n\n"
                        + ChatColor.BLACK + "Stand on bell plates together.\n"
                        + "Hold the required count for 2 seconds.\n\n"
                        + "Required plates = online party members (max 4)."
        );
        meta.addPage(
                ChatColor.DARK_RED + "Ofuda Sequence\n\n"
                        + ChatColor.BLACK + "Step on the colored pads in the order shown in chat.\n"
                        + "Wrong step resets progress."
        );
        meta.addPage(
                ChatColor.DARK_GREEN + "Tips\n\n"
                        + ChatColor.BLACK + "Call out colors in voice/chat.\n"
                        + "For Bell Sync, have each player claim one plate.\n"
                        + "Do not jump early before 2 seconds complete."
        );
        meta.getPersistentDataContainer().set(puzzleGuideTag, PersistentDataType.BYTE, (byte) 1);
        book.setItemMeta(meta);
        return book;
    }

    private String focusLabel(RoomType focusRoom) {
        if (focusRoom == RoomType.PUZZLE_SYNC) {
            return ChatColor.BLUE + "Shrine Bell Sync";
        }
        if (focusRoom == RoomType.PUZZLE_SEQUENCE) {
            return ChatColor.DARK_RED + "Ofuda Sequence";
        }
        return ChatColor.GRAY + "General";
    }

    private int calculateScore(long elapsedSeconds) {
        int score = 300;
        score -= (int) (elapsedSeconds / 6L);
        score -= deaths * 20;
        score += clearedCombatRooms * 12;
        score += clearedPuzzleRooms * 15;
        score += clearedTreasureRooms * 8;
        if (elapsedSeconds <= 900L) {
            score += 20;
        }
        score += 20;
        return Math.max(0, Math.min(300, score));
    }

    private String gradeForScore(int score) {
        if (score >= 270) {
            return "S";
        }
        if (score >= 220) {
            return "A";
        }
        if (score >= 170) {
            return "B";
        }
        if (score >= 120) {
            return "C";
        }
        return "D";
    }

    private void broadcast(String message) {
        forEachOnlineMember(player -> player.sendMessage(ChatColor.GOLD + "[Dungeon] " + ChatColor.YELLOW + message));
    }

    private void teleportMembers(Location location) {
        forEachOnlineMember(player -> {
            player.teleport(location);
            player.setFallDistance(0.0F);
            double maxHealth = player.getAttribute(Attribute.MAX_HEALTH) != null
                    ? player.getAttribute(Attribute.MAX_HEALTH).getValue()
                    : 20.0D;
            player.setHealth(Math.min(maxHealth, player.getHealth() + 6.0D));
            player.setFoodLevel(20);
        });
    }

    private void forEachOnlineMember(Consumer<Player> consumer) {
        for (UUID memberId : members) {
            Player player = Bukkit.getPlayer(memberId);
            if (player == null || !player.isOnline()) {
                continue;
            }
            consumer.accept(player);
        }
    }

    private List<Player> onlineMembers(UUID excludedPlayerId) {
        List<Player> online = new ArrayList<>();
        for (UUID memberId : members) {
            if (excludedPlayerId != null && excludedPlayerId.equals(memberId)) {
                continue;
            }
            Player player = Bukkit.getPlayer(memberId);
            if (player == null || !player.isOnline()) {
                continue;
            }
            online.add(player);
        }
        return online;
    }

    private void removeSessionRunKeysFromMembers() {
        for (UUID memberId : members) {
            Player player = Bukkit.getPlayer(memberId);
            if (player == null) {
                continue;
            }
            removeSessionRunKeysFromPlayer(player);
        }
    }

    private void removeSessionRunKeysFromOnlineMembers() {
        for (Player player : onlineMembers(null)) {
            removeSessionRunKeysFromPlayer(player);
        }
    }

    private boolean removeSessionRunKeysFromPlayer(Player player) {
        boolean removedAny = false;
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (!isRunKey(item)) {
                continue;
            }
            inventory.setItem(slot, null);
            removedAny = true;
        }
        return removedAny;
    }

    private int onlineMemberCount() {
        int online = 0;
        for (UUID memberId : members) {
            Player player = Bukkit.getPlayer(memberId);
            if (player != null && player.isOnline()) {
                online++;
            }
        }
        return online;
    }

    private Player randomOnlineMember() {
        List<Player> online = new ArrayList<>();
        for (UUID memberId : members) {
            Player player = Bukkit.getPlayer(memberId);
            if (player != null && player.isOnline()) {
                online.add(player);
            }
        }
        if (online.isEmpty()) {
            return null;
        }
        return online.get(ThreadLocalRandom.current().nextInt(online.size()));
    }

    private EntityType randomMobType() {
        List<EntityType> pool = floor.mobPool();
        if (pool.isEmpty()) {
            return EntityType.ZOMBIE;
        }
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    private ArmorSetType armorSetForYokai(YokaiType yokaiType) {
        if (yokaiType == null) {
            return randomArmorSet();
        }
        return switch (yokaiType) {
            case ONI_BRUTE -> ArmorSetType.SHOGUN;
            case TENGU_SKIRMISHER, JOROGUMO_WEAVER, KITSUNE_TRICKSTER -> ArmorSetType.SHINOBI;
            case KAPPA_RAIDER, ONRYO_WRAITH -> ArmorSetType.ONMYOJI;
            case GASHADOKURO_SENTINEL -> randomArmorSet();
        };
    }

    private ArmorSetType randomArmorSet() {
        ArmorSetType[] values = ArmorSetType.values();
        return values[ThreadLocalRandom.current().nextInt(values.length)];
    }

    private CustomArmorType randomArmorPiece(ArmorSetType setType) {
        List<CustomArmorType> pieces = CustomArmorType.piecesForSet(setType);
        if (pieces.isEmpty()) {
            return null;
        }
        return pieces.get(ThreadLocalRandom.current().nextInt(pieces.size()));
    }

    private ReforgeStoneType randomReforgeStone(YokaiType yokaiType, boolean boss) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (boss) {
            if (random.nextDouble() < 0.25D) {
                return ReforgeStoneType.TEMPEST_STONE;
            }
            return switch (random.nextInt(3)) {
                case 0 -> ReforgeStoneType.JAGGED_STONE;
                case 1 -> ReforgeStoneType.TITAN_STONE;
                default -> ReforgeStoneType.ARCANE_STONE;
            };
        }
        if (yokaiType == YokaiType.GASHADOKURO_SENTINEL && random.nextDouble() < 0.10D) {
            return ReforgeStoneType.TEMPEST_STONE;
        }
        return switch (random.nextInt(3)) {
            case 0 -> ReforgeStoneType.JAGGED_STONE;
            case 1 -> ReforgeStoneType.TITAN_STONE;
            default -> ReforgeStoneType.ARCANE_STONE;
        };
    }

    private CustomWeaponType randomDungeonWeapon() {
        List<CustomWeaponType> pool = List.of(
                CustomWeaponType.ONI_CLEAVER,
                CustomWeaponType.TENGU_GALEBLADE,
                CustomWeaponType.TENGU_STORMBOW,
                CustomWeaponType.KAPPA_TIDEBREAKER,
                CustomWeaponType.ONRYO_SPIRITBLADE,
                CustomWeaponType.JOROGUMO_STINGER,
                CustomWeaponType.KITSUNE_FANG,
                CustomWeaponType.KITSUNE_DAWNBOW
        );
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    private int floorNumber(String floorId) {
        if (floorId == null || floorId.isBlank()) {
            return 1;
        }
        StringBuilder digits = new StringBuilder();
        for (char c : floorId.toCharArray()) {
            if (Character.isDigit(c)) {
                digits.append(c);
            }
        }
        if (digits.isEmpty()) {
            return 1;
        }
        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private YokaiType randomYokaiType(int damageTier) {
        List<YokaiType> pool = floor.yokaiPool();
        if (pool.isEmpty()) {
            return YokaiType.ONI_BRUTE;
        }
        if (damageTier >= 3 && pool.contains(YokaiType.GASHADOKURO_SENTINEL) && ThreadLocalRandom.current().nextDouble() < 0.35D) {
            return YokaiType.GASHADOKURO_SENTINEL;
        }
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    private Location randomSpawnInRoom(Location center, int radius) {
        World world = center.getWorld();
        if (world == null) {
            return null;
        }

        int centerX = center.getBlockX();
        int centerZ = center.getBlockZ();
        int floorY = center.getBlockY() - 1;
        int maxRoomRadius = Math.max(2, (floor.roomSize() / 2) - 3);
        int boundedRadius = Math.max(2, Math.min(radius, maxRoomRadius));

        for (int attempt = 0; attempt < 40; attempt++) {
            int x = centerX + ThreadLocalRandom.current().nextInt(-boundedRadius, boundedRadius + 1);
            int z = centerZ + ThreadLocalRandom.current().nextInt(-boundedRadius, boundedRadius + 1);
            if (isSafeSpawnColumn(world, x, floorY, z)) {
                return new Location(world, x + 0.5D, floorY + 1.0D, z + 0.5D);
            }
        }

        if (isSafeSpawnColumn(world, centerX, floorY, centerZ)) {
            return new Location(world, centerX + 0.5D, floorY + 1.0D, centerZ + 0.5D);
        }
        return null;
    }

    private Location distributedSpawnInRoom(Location center, int index, int total) {
        if (center == null || center.getWorld() == null) {
            return null;
        }
        World world = center.getWorld();
        int centerX = center.getBlockX();
        int centerZ = center.getBlockZ();
        int floorY = center.getBlockY() - 1;
        int maxRoomRadius = Math.max(3, (floor.roomSize() / 2) - 3);
        int ringRadius = Math.max(3, maxRoomRadius - 1);
        int count = Math.max(1, total);
        double angle = (Math.PI * 2.0D * index) / count;

        for (int ring = 0; ring < 3; ring++) {
            int radius = Math.max(3, ringRadius - (ring * 2));
            int x = centerX + (int) Math.round(Math.cos(angle) * radius);
            int z = centerZ + (int) Math.round(Math.sin(angle) * radius);
            if (isSafeSpawnColumn(world, x, floorY, z)) {
                return new Location(world, x + 0.5D, floorY + 1.0D, z + 0.5D);
            }
        }
        return randomSpawnInRoom(center, ringRadius);
    }

    private boolean isSafeSpawnColumn(World world, int x, int floorY, int z) {
        Block floorBlock = world.getBlockAt(x, floorY, z);
        Block feetBlock = world.getBlockAt(x, floorY + 1, z);
        Block headBlock = world.getBlockAt(x, floorY + 2, z);
        Block upperHeadBlock = world.getBlockAt(x, floorY + 3, z);
        if (!floorBlock.getType().isSolid() || !feetBlock.isPassable() || !headBlock.isPassable() || !upperHeadBlock.isPassable()) {
            return false;
        }

        int solidCardinalFloor = 0;
        if (world.getBlockAt(x - 1, floorY, z).getType().isSolid()) {
            solidCardinalFloor++;
        }
        if (world.getBlockAt(x + 1, floorY, z).getType().isSolid()) {
            solidCardinalFloor++;
        }
        if (world.getBlockAt(x, floorY, z - 1).getType().isSolid()) {
            solidCardinalFloor++;
        }
        if (world.getBlockAt(x, floorY, z + 1).getType().isSolid()) {
            solidCardinalFloor++;
        }
        return solidCardinalFloor >= 2;
    }

    private Location fallbackSpawnInRoom(Location center) {
        World world = center.getWorld();
        if (world == null) {
            return null;
        }
        int centerX = center.getBlockX();
        int centerZ = center.getBlockZ();
        int floorY = center.getBlockY() - 1;

        int[][] offsets = {
                {0, 0},
                {3, 0},
                {-3, 0},
                {0, 3},
                {0, -3},
                {2, 2},
                {-2, 2},
                {2, -2},
                {-2, -2}
        };
        for (int[] offset : offsets) {
            int x = centerX + offset[0];
            int z = centerZ + offset[1];
            if (!prepareFallbackSpawnColumn(world, x, floorY, z)) {
                continue;
            }
            return new Location(world, x + 0.5D, floorY + 1.0D, z + 0.5D);
        }
        return null;
    }

    private boolean prepareFallbackSpawnColumn(World world, int x, int floorY, int z) {
        Block floorBlock = world.getBlockAt(x, floorY, z);
        if (!floorBlock.getType().isSolid()) {
            floorBlock.setType(floor.floorMaterial(), false);
        }

        world.getBlockAt(x, floorY + 1, z).setType(Material.AIR, false);
        world.getBlockAt(x, floorY + 2, z).setType(Material.AIR, false);
        world.getBlockAt(x, floorY + 3, z).setType(Material.AIR, false);
        return world.getBlockAt(x, floorY, z).getType().isSolid();
    }

    private int experienceForTrackedMob(TrackedMobInfo mobInfo) {
        int base = mobInfo.boss() ? 120 : 14;
        if (!mobInfo.boss() && mobInfo.yokaiType() == YokaiType.GASHADOKURO_SENTINEL) {
            base += 18;
        }
        return base + Math.max(0, floor.mobDamageTier() * 3);
    }

    private String formatSequenceOrder(List<Integer> order) {
        List<String> names = new ArrayList<>();
        for (Integer index : order) {
            names.add(colorName(index));
        }
        return String.join(" -> ", names);
    }

    private String colorName(int index) {
        return switch (index) {
            case 0 -> "Vermilion";
            case 1 -> "Jade";
            case 2 -> "Indigo";
            case 3 -> "Gold";
            default -> "Unknown";
        };
    }

    private String formatTime(long seconds) {
        long minutesPart = seconds / 60L;
        long secondsPart = seconds % 60L;
        return String.format("%02d:%02d", minutesPart, secondsPart);
    }

    private String sessionToken() {
        return sessionId.toString().substring(0, 8);
    }

    private String blockKey(Location location) {
        if (location.getWorld() == null) {
            return "null:0:0:0";
        }
        return blockKey(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    private String blockKey(World world, int x, int y, int z) {
        return world.getUID() + ":" + x + ":" + y + ":" + z;
    }

    private Block resolveBlockFromKey(World world, String key) {
        String[] parts = key.split(":");
        if (parts.length < 4) {
            return null;
        }
        int length = parts.length;
        try {
            int x = Integer.parseInt(parts[length - 3]);
            int y = Integer.parseInt(parts[length - 2]);
            int z = Integer.parseInt(parts[length - 1]);
            return world.getBlockAt(x, y, z);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private record TrackedMobInfo(boolean boss, YokaiType yokaiType) {
    }
}
