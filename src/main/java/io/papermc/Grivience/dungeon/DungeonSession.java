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
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
import org.bukkit.util.Vector;

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
    private final List<ArenaLayout.Cuboid> cleanupVolumes;
    private final List<RoomType> encounterPlan;
    private final Map<UUID, TrackedMobInfo> trackedMobs = new HashMap<>();
    private final Map<Integer, ArenaLayout.Gate> gatesByIndex = new HashMap<>();
    private final Map<String, Integer> gateIndexByBlock = new HashMap<>();
    private final Set<Integer> openedGates = new HashSet<>();

    private BukkitTask countdownTask;
    private BukkitTask actionBarTask;
    private BukkitTask puzzleTask;
    private BukkitTask bossAbilityTask;

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
    private UUID activeBossId;
    private boolean bossEnraged;
    private int bossAbilityCooldownSeconds;
    private int bossReinforcementCooldownSeconds;
    private int bossAbilityCycle;

    private final Map<String, Integer> sequencePadIndexByKey = new HashMap<>();
    private final Map<Integer, Location> sequencePadLocationByIndex = new HashMap<>();
    private final List<Integer> sequenceOrder = new ArrayList<>();
    private int sequenceProgress;

    private final List<String> syncPlateKeys = new ArrayList<>();
    private final List<Location> syncPlateLocations = new ArrayList<>();
    private int syncStableTicks;
    private int syncRequiredPlates;
    private int syncPoweredPlates;
    private int syncLastPoweredPlates;

    private final Map<String, Integer> chimeBellIndexByKey = new HashMap<>();
    private final Map<Integer, Location> chimeBellLocationByIndex = new HashMap<>();
    private final List<Integer> chimeOrder = new ArrayList<>();
    private int chimeProgress;

    private final List<String> sealLeverKeys = new ArrayList<>();
    private final List<Location> sealLeverLocations = new ArrayList<>();
    private final List<Boolean> sealTargetStates = new ArrayList<>();
    private int sealMatchedLevers;
    private int sealStableTicks;
    private int sealLastMatchedLevers;

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
        this.cleanupVolumes = List.copyOf(layout.cleanupVolumes());
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

    public World arenaWorld() {
        if (rooms.isEmpty()) {
            return null;
        }
        return rooms.getFirst().getWorld();
    }

    public List<ArenaLayout.Cuboid> cleanupVolumes() {
        return cleanupVolumes;
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

        if (bossActive && mobInfo.boss()) {
            stopBossController();
            finish(true, "Shogun defeated.");
            return;
        }

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
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.85F, 1.25F);
            if (sequenceProgress >= sequenceOrder.size()) {
                clearCurrentRoom("Ofuda sequence solved.");
                return;
            }
            int nextPad = sequenceOrder.get(sequenceProgress);
            broadcast(player.getName() + " hit the correct pad (" + sequenceProgress + "/" + sequenceOrder.size() + "). Next: " + colorName(nextPad) + ChatColor.YELLOW + ".");
            highlightSequencePad(nextPad, Particle.END_ROD);
            return;
        }

        int firstPad = sequenceOrder.getFirst();
        sequenceProgress = 0;
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.95F, 0.9F);
        broadcast(player.getName() + " stepped on the wrong pad. Sequence reset. Restart at " + colorName(firstPad) + ChatColor.YELLOW + ".");
        highlightSequencePad(firstPad, Particle.CRIT);
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

        Material type = clickedBlock.getType();
        if (!bossActive && !transitioning && currentRoomType == RoomType.PUZZLE_CHIME && type == Material.BELL) {
            handleChimeInteract(player, clickedBlock);
            return;
        }

        if (!bossActive && !transitioning && currentRoomType == RoomType.PUZZLE_SEAL && type == Material.LEVER) {
            if (!sealLeverKeys.contains(blockKey(clickedBlock.getLocation()))) {
                return;
            }
            int matched = countMatchedSealLevers();
            int total = Math.max(1, sealLeverKeys.size());
            player.sendMessage(ChatColor.GOLD + "[Dungeon] " + ChatColor.YELLOW + "Seal alignment: " + matched + "/" + total + ".");
            return;
        }

        if (bossActive || transitioning || currentRoomType != RoomType.TREASURE) {
            return;
        }

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
        stopBossController();

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
                broadcast("Ofuda sequence trial started.");
                broadcast("Step on pads in the shown order. Wrong pad resets progress.");
                broadcast("Pad row (left to right): "
                        + colorName(0) + ChatColor.YELLOW + ", "
                        + colorName(1) + ChatColor.YELLOW + ", "
                        + colorName(2) + ChatColor.YELLOW + ", "
                        + colorName(3) + ChatColor.YELLOW + ".");
                givePuzzleGuide(RoomType.PUZZLE_SEQUENCE);
                setupSequencePuzzle();
            }
            case PUZZLE_SYNC -> {
                broadcast("Shrine bell sync trial started.");
                broadcast("Hold the required plates together for 2 seconds.");
                givePuzzleGuide(RoomType.PUZZLE_SYNC);
                setupSyncPuzzle();
            }
            case PUZZLE_CHIME -> {
                broadcast("Storm chime memory trial started.");
                broadcast("Ring bells in the shown order. Wrong bell resets progress.");
                givePuzzleGuide(RoomType.PUZZLE_CHIME);
                setupChimePuzzle();
            }
            case PUZZLE_SEAL -> {
                broadcast("Thunder seal alignment trial started.");
                broadcast("Match lever states to the shown seal pattern and hold it for 2 seconds.");
                givePuzzleGuide(RoomType.PUZZLE_SEAL);
                setupSealPuzzle();
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
        stopBossController();

        transitioning = false;
        bossActive = true;
        bossEnraged = false;
        currentRoomType = null;
        currentRoomCenter = rooms.getLast();
        broadcast("Shogun encounter started: " + floor.bossName());

        LivingEntity bossEntity = spawnMob(
                floor.bossType(),
                floor.bossHealthMultiplier() * floorHealthScale,
                floor.mobDamageTier() + 4,
                true
        );
        if (bossEntity != null) {
            activeBossId = bossEntity.getUniqueId();
            startBossController();
        }

        int minions = Math.max(3, onlineMemberCount() + (encounterPlan.size() / 2));
        for (int i = 0; i < minions; i++) {
            spawnDungeonMob(
                    floor.mobHealthMultiplier() * floorHealthScale * 2.0D,
                    floor.mobDamageTier() + 3,
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
        sequencePadLocationByIndex.clear();
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
            sequencePadLocationByIndex.put(i, new Location(world, x + 0.5D, floorY + 1.0D, z + 0.5D));
        }

        int sequenceLength = sequenceLengthForFloor();
        int previous = -1;
        for (int i = 0; i < sequenceLength; i++) {
            int next = ThreadLocalRandom.current().nextInt(offsets.length);
            if (next == previous) {
                next = (next + 1 + ThreadLocalRandom.current().nextInt(offsets.length - 1)) % offsets.length;
            }
            sequenceOrder.add(next);
            previous = next;
        }

        broadcast("Ofuda order (" + sequenceOrder.size() + "): " + formatSequenceOrder(sequenceOrder));
        highlightSequencePad(sequenceOrder.getFirst(), Particle.END_ROD);

        puzzleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (ended || transitioning || bossActive || currentRoomType != RoomType.PUZZLE_SEQUENCE) {
                    cancel();
                    return;
                }
                if (sequenceOrder.isEmpty()) {
                    return;
                }
                int expectedIndex = Math.max(0, Math.min(sequenceProgress, sequenceOrder.size() - 1));
                int nextPad = sequenceOrder.get(expectedIndex);
                highlightSequencePad(nextPad, Particle.END_ROD);
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void setupSyncPuzzle() {
        syncPlateKeys.clear();
        syncPlateLocations.clear();
        syncStableTicks = 0;
        syncRequiredPlates = 0;
        syncPoweredPlates = 0;
        syncLastPoweredPlates = -1;

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
            syncPlateLocations.add(new Location(world, x + 0.5D, floorY + 1.05D, z + 0.5D));
        }
        syncRequiredPlates = computeSyncRequiredPlates();
        syncLastPoweredPlates = 0;
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
                showSyncPlateHints();

                if (syncPoweredPlates != syncLastPoweredPlates) {
                    int missing = Math.max(0, syncRequiredPlates - syncPoweredPlates);
                    if (missing > 0) {
                        broadcast("Bell plates active: " + syncPoweredPlates + "/" + syncRequiredPlates + ". Need " + missing + " more.");
                    } else {
                        broadcast("Bell plates aligned: " + syncPoweredPlates + "/" + syncRequiredPlates + ". Hold steady!");
                    }
                    syncLastPoweredPlates = syncPoweredPlates;
                }

                if (syncPoweredPlates >= syncRequiredPlates) {
                    syncStableTicks++;
                    if (syncStableTicks == 1) {
                        broadcast("Bell sync almost complete. Hold for 1 more second.");
                    }
                    if (syncStableTicks >= 2) {
                        clearCurrentRoom("Shrine bell sync solved.");
                    }
                } else {
                    if (syncStableTicks > 0) {
                        broadcast("Bell sync interrupted. Re-align all required plates.");
                    }
                    syncStableTicks = 0;
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void setupChimePuzzle() {
        chimeBellIndexByKey.clear();
        chimeBellLocationByIndex.clear();
        chimeOrder.clear();
        chimeProgress = 0;

        if (currentRoomCenter == null || currentRoomCenter.getWorld() == null) {
            clearCurrentRoom("Puzzle skipped (room unavailable).");
            return;
        }

        World world = currentRoomCenter.getWorld();
        int centerX = currentRoomCenter.getBlockX();
        int floorY = currentRoomCenter.getBlockY() - 1;
        int centerZ = currentRoomCenter.getBlockZ();

        int bellOffset = Math.max(3, (floor.roomSize() / 2) - 4);
        int[][] offsets = {
                {0, -bellOffset},
                {-bellOffset, 0},
                {bellOffset, 0},
                {0, bellOffset}
        };

        for (int i = 0; i < offsets.length; i++) {
            int x = centerX + offsets[i][0];
            int z = centerZ + offsets[i][1];
            world.getBlockAt(x, floorY, z).setType(Material.POLISHED_ANDESITE, false);
            world.getBlockAt(x, floorY + 1, z).setType(Material.BELL, false);
            chimeBellIndexByKey.put(blockKey(world, x, floorY + 1, z), i);
            chimeBellLocationByIndex.put(i, new Location(world, x + 0.5D, floorY + 1.1D, z + 0.5D));
        }

        int orderLength = chimeLengthForFloor();
        int previous = -1;
        for (int i = 0; i < orderLength; i++) {
            int next = ThreadLocalRandom.current().nextInt(offsets.length);
            if (next == previous) {
                next = (next + 1 + ThreadLocalRandom.current().nextInt(offsets.length - 1)) % offsets.length;
            }
            chimeOrder.add(next);
            previous = next;
        }

        broadcast("Storm chime order (" + chimeOrder.size() + "): " + formatChimeOrder(chimeOrder));
        highlightChimeBell(chimeOrder.getFirst(), Particle.NOTE);

        puzzleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (ended || transitioning || bossActive || currentRoomType != RoomType.PUZZLE_CHIME) {
                    cancel();
                    return;
                }
                if (chimeOrder.isEmpty()) {
                    return;
                }
                int expectedIndex = Math.max(0, Math.min(chimeProgress, chimeOrder.size() - 1));
                int nextBell = chimeOrder.get(expectedIndex);
                highlightChimeBell(nextBell, Particle.NOTE);
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void handleChimeInteract(Player player, Block clickedBlock) {
        if (chimeOrder.isEmpty()) {
            return;
        }
        Integer playedBell = chimeBellIndexByKey.get(blockKey(clickedBlock.getLocation()));
        if (playedBell == null) {
            return;
        }

        int expected = chimeOrder.get(chimeProgress);
        if (playedBell == expected) {
            chimeProgress++;
            player.playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 0.85F, 1.2F);
            if (chimeProgress >= chimeOrder.size()) {
                clearCurrentRoom("Storm chime memory solved.");
                return;
            }
            int nextBell = chimeOrder.get(chimeProgress);
            broadcast(player.getName() + " rang the correct bell (" + chimeProgress + "/" + chimeOrder.size() + "). Next: "
                    + chimeName(nextBell) + ChatColor.YELLOW + ".");
            highlightChimeBell(nextBell, Particle.NOTE);
            return;
        }

        chimeProgress = 0;
        int firstBell = chimeOrder.getFirst();
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.95F, 0.9F);
        broadcast(player.getName() + " rang the wrong bell. Sequence reset. Restart at " + chimeName(firstBell) + ChatColor.YELLOW + ".");
        highlightChimeBell(firstBell, Particle.CRIT);
    }

    private void setupSealPuzzle() {
        sealLeverKeys.clear();
        sealLeverLocations.clear();
        sealTargetStates.clear();
        sealMatchedLevers = 0;
        sealStableTicks = 0;
        sealLastMatchedLevers = -1;

        if (currentRoomCenter == null || currentRoomCenter.getWorld() == null) {
            clearCurrentRoom("Puzzle skipped (room unavailable).");
            return;
        }

        World world = currentRoomCenter.getWorld();
        int centerX = currentRoomCenter.getBlockX();
        int floorY = currentRoomCenter.getBlockY() - 1;
        int centerZ = currentRoomCenter.getBlockZ();

        int leverOffset = Math.max(3, (floor.roomSize() / 2) - 4);
        int[][] offsets = {
                {-leverOffset, -leverOffset},
                {leverOffset, -leverOffset},
                {-leverOffset, leverOffset},
                {leverOffset, leverOffset}
        };
        for (int i = 0; i < offsets.length; i++) {
            int x = centerX + offsets[i][0];
            int z = centerZ + offsets[i][1];
            world.getBlockAt(x, floorY, z).setType(Material.POLISHED_BLACKSTONE, false);
            world.getBlockAt(x, floorY + 1, z).setType(Material.LEVER, false);
            sealLeverKeys.add(blockKey(world, x, floorY + 1, z));
            sealLeverLocations.add(new Location(world, x + 0.5D, floorY + 1.1D, z + 0.5D));
            sealTargetStates.add(ThreadLocalRandom.current().nextBoolean());
        }

        boolean hasOn = sealTargetStates.contains(Boolean.TRUE);
        boolean hasOff = sealTargetStates.contains(Boolean.FALSE);
        if (!hasOn || !hasOff) {
            int flipIndex = ThreadLocalRandom.current().nextInt(sealTargetStates.size());
            sealTargetStates.set(flipIndex, !sealTargetStates.get(flipIndex));
        }

        for (int i = 0; i < sealLeverKeys.size(); i++) {
            Block lever = resolveBlockFromKey(world, sealLeverKeys.get(i));
            if (lever == null) {
                continue;
            }
            if (lever.getBlockData() instanceof Powerable powerable) {
                powerable.setPowered(false);
                lever.setBlockData(powerable, false);
            }
            Material marker = sealTargetStates.get(i) ? Material.LIME_STAINED_GLASS : Material.GRAY_STAINED_GLASS;
            world.getBlockAt(lever.getX(), lever.getY() + 2, lever.getZ()).setType(marker, false);
        }

        sealMatchedLevers = countMatchedSealLevers();
        sealLastMatchedLevers = sealMatchedLevers;
        broadcast("Thunder seal pattern: " + formatSealTargetPattern() + ChatColor.YELLOW + ".");
        broadcast("Set each lever to match its target (lime = ON, gray = OFF).");

        puzzleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (ended || transitioning || bossActive || currentRoomType != RoomType.PUZZLE_SEAL) {
                    cancel();
                    return;
                }
                sealMatchedLevers = countMatchedSealLevers();
                showSealLeverHints();

                if (sealMatchedLevers != sealLastMatchedLevers) {
                    broadcast("Seal alignment: " + sealMatchedLevers + "/" + sealLeverKeys.size() + ".");
                    sealLastMatchedLevers = sealMatchedLevers;
                }

                if (sealMatchedLevers >= sealLeverKeys.size()) {
                    sealStableTicks++;
                    if (sealStableTicks == 1) {
                        broadcast("Thunder seals aligned. Hold for 1 more second.");
                    }
                    if (sealStableTicks >= 2) {
                        clearCurrentRoom("Thunder seal alignment solved.");
                    }
                } else {
                    if (sealStableTicks > 0) {
                        broadcast("Seal alignment broke. Re-align all levers.");
                    }
                    sealStableTicks = 0;
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

    private int countMatchedSealLevers() {
        if (currentRoomCenter == null || currentRoomCenter.getWorld() == null || sealLeverKeys.isEmpty()) {
            return 0;
        }
        World world = currentRoomCenter.getWorld();
        int matched = 0;
        for (int i = 0; i < sealLeverKeys.size(); i++) {
            Block lever = resolveBlockFromKey(world, sealLeverKeys.get(i));
            if (lever == null) {
                continue;
            }
            boolean powered = lever.getBlockData() instanceof Powerable powerable && powerable.isPowered();
            boolean target = i < sealTargetStates.size() && sealTargetStates.get(i);
            if (powered == target) {
                matched++;
            }
        }
        return matched;
    }

    private void highlightSequencePad(int padIndex, Particle particle) {
        Location marker = sequencePadLocationByIndex.get(padIndex);
        if (marker == null || marker.getWorld() == null) {
            return;
        }
        marker.getWorld().spawnParticle(particle, marker, 16, 0.28D, 0.15D, 0.28D, 0.01D);
    }

    private void highlightChimeBell(int bellIndex, Particle particle) {
        Location marker = chimeBellLocationByIndex.get(bellIndex);
        if (marker == null || marker.getWorld() == null) {
            return;
        }
        marker.getWorld().spawnParticle(particle, marker, 14, 0.24D, 0.14D, 0.24D, 0.01D);
    }

    private void showSyncPlateHints() {
        if (syncPlateLocations.isEmpty()) {
            return;
        }
        for (Location marker : syncPlateLocations) {
            if (marker.getWorld() == null) {
                continue;
            }
            Block block = marker.getBlock();
            boolean powered = block.getBlockData() instanceof Powerable powerable && powerable.isPowered();
            if (powered) {
                marker.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, marker, 6, 0.18D, 0.12D, 0.18D, 0.01D);
            } else {
                marker.getWorld().spawnParticle(Particle.CRIT, marker, 8, 0.2D, 0.12D, 0.2D, 0.01D);
            }
        }
    }

    private void showSealLeverHints() {
        if (sealLeverLocations.isEmpty() || currentRoomCenter == null || currentRoomCenter.getWorld() == null) {
            return;
        }
        World world = currentRoomCenter.getWorld();
        for (int i = 0; i < sealLeverLocations.size(); i++) {
            Location marker = sealLeverLocations.get(i);
            if (marker.getWorld() == null) {
                continue;
            }
            Block lever = i < sealLeverKeys.size() ? resolveBlockFromKey(world, sealLeverKeys.get(i)) : null;
            if (lever == null) {
                continue;
            }
            boolean powered = lever.getBlockData() instanceof Powerable powerable && powerable.isPowered();
            boolean target = i < sealTargetStates.size() && sealTargetStates.get(i);
            if (powered == target) {
                marker.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, marker, 6, 0.18D, 0.12D, 0.18D, 0.01D);
            } else {
                marker.getWorld().spawnParticle(Particle.CRIT, marker, 8, 0.2D, 0.12D, 0.2D, 0.01D);
            }
        }
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
    private LivingEntity spawnMob(EntityType type, double healthScale, int damageTier, boolean boss) {
        return spawnMob(type, healthScale, damageTier, boss, null);
    }

    private LivingEntity spawnMob(EntityType type, double healthScale, int damageTier, boolean boss, Location preferredSpawn) {
        if (currentRoomCenter == null || currentRoomCenter.getWorld() == null) {
            return null;
        }
        Location spawnLocation = preferredSpawn != null
                ? preferredSpawn
                : randomSpawnInRoom(currentRoomCenter, Math.max(3, (floor.roomSize() / 2) - 3));
        if (spawnLocation == null) {
            spawnLocation = fallbackSpawnInRoom(currentRoomCenter);
        }
        if (spawnLocation == null) {
            return null;
        }
        Entity entity = currentRoomCenter.getWorld().spawnEntity(spawnLocation, type);
        if (!(entity instanceof LivingEntity living)) {
            entity.remove();
            return null;
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
        return living;
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

    private void startBossController() {
        if (activeBossId == null) {
            return;
        }
        stopBossControllerTask();
        bossAbilityCycle = 0;
        bossAbilityCooldownSeconds = randomAbilityCooldownSeconds();
        bossReinforcementCooldownSeconds = randomReinforcementCooldownSeconds();

        bossAbilityTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (ended || !bossActive) {
                    stopBossController();
                    return;
                }

                LivingEntity boss = resolveActiveBoss();
                if (boss == null) {
                    stopBossController();
                    return;
                }

                maintainBossAggression(boss);
                updateBossEnrageState(boss);

                bossAbilityCooldownSeconds = Math.max(0, bossAbilityCooldownSeconds - 1);
                if (bossAbilityCooldownSeconds <= 0) {
                    triggerBossAbility(boss);
                    bossAbilityCooldownSeconds = randomAbilityCooldownSeconds();
                }

                bossReinforcementCooldownSeconds = Math.max(0, bossReinforcementCooldownSeconds - 1);
                if (bossReinforcementCooldownSeconds <= 0) {
                    summonBossReinforcements(boss);
                    bossReinforcementCooldownSeconds = randomReinforcementCooldownSeconds();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void stopBossController() {
        stopBossControllerTask();
        activeBossId = null;
        bossEnraged = false;
        bossAbilityCycle = 0;
        bossAbilityCooldownSeconds = 0;
        bossReinforcementCooldownSeconds = 0;
    }

    private void stopBossControllerTask() {
        if (bossAbilityTask != null) {
            bossAbilityTask.cancel();
            bossAbilityTask = null;
        }
    }

    private LivingEntity resolveActiveBoss() {
        if (activeBossId == null) {
            return null;
        }
        Entity entity = Bukkit.getEntity(activeBossId);
        if (!(entity instanceof LivingEntity living) || living.isDead()) {
            return null;
        }
        return living;
    }

    private void maintainBossAggression(LivingEntity boss) {
        if (!(boss instanceof Mob mob)) {
            return;
        }
        if (mob.getTarget() instanceof Player target
                && target.isOnline()
                && !target.isDead()
                && members.contains(target.getUniqueId())) {
            return;
        }
        Player retarget = randomBossTarget();
        if (retarget != null) {
            mob.setTarget(retarget);
        }
    }

    private void updateBossEnrageState(LivingEntity boss) {
        if (bossEnraged) {
            return;
        }
        AttributeInstance maxHealth = boss.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null || maxHealth.getValue() <= 0.0D) {
            return;
        }
        if (boss.getHealth() > maxHealth.getValue() * 0.50D) {
            return;
        }

        bossEnraged = true;
        boss.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2, false, false));
        boss.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 2, false, false));
        boss.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 2, false, false));
        if (boss.getWorld() != null) {
            boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 1.7F, 0.65F);
            boss.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, boss.getLocation().add(0.0D, 1.0D, 0.0D), 80, 0.85D, 0.5D, 0.85D, 0.02D);
        }
        broadcast(ChatColor.DARK_RED + floor.bossName() + " is ENRAGED!");
        bossAbilityCooldownSeconds = Math.min(bossAbilityCooldownSeconds, 3);
        bossReinforcementCooldownSeconds = Math.min(bossReinforcementCooldownSeconds, 5);
    }

    private void triggerBossAbility(LivingEntity boss) {
        bossAbilityCycle++;
        BossArchetype archetype = BossArchetype.from(floor.bossType());
        boolean alternate = bossAbilityCycle % 2 == 0;

        switch (archetype) {
            case RONIN_GATEKEEPER -> {
                if (alternate) {
                    castRoninWhirlwind(boss);
                } else {
                    castRoninIaidoDash(boss);
                }
            }
            case ONMYOJI_SHOGUN -> {
                if (alternate) {
                    castOnmyojiHexVolley(boss);
                } else {
                    castOnmyojiFangSurge(boss);
                }
            }
            case STORM_DAIMYO -> {
                if (alternate) {
                    castStormStampedeCharge(boss);
                } else {
                    castStormThunderSlam(boss);
                }
            }
            case RAIJIN_AVATAR -> {
                if (alternate) {
                    castRaijinGravityWell(boss);
                } else {
                    castRaijinCataclysm(boss);
                }
            }
            case TEMPLE_WARLORD -> castFallbackShadowNova(boss);
        }
    }

    private void castRoninIaidoDash(LivingEntity boss) {
        Player target = randomBossTarget();
        if (target == null || target.getWorld() == null) {
            return;
        }

        Location bossLoc = boss.getLocation();
        World world = bossLoc.getWorld();
        if (world == null) {
            return;
        }

        broadcast(ChatColor.RED + floor.bossName() + " uses Iaido Dash!");
        world.playSound(bossLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.6F, 0.65F);
        world.spawnParticle(Particle.SWEEP_ATTACK, bossLoc.add(0.0D, 0.8D, 0.0D), 16, 0.55D, 0.15D, 0.55D, 0.01D);

        Vector dash = target.getLocation().toVector().subtract(bossLoc.toVector());
        if (dash.lengthSquared() > 0.2D) {
            dash = dash.normalize().multiply(bossEnraged ? 1.45D : 1.2D);
            dash.setY(0.25D);
            boss.setVelocity(dash);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (ended || !bossActive || boss.isDead()) {
                return;
            }
            Location strike = target.isOnline() ? target.getLocation().clone() : boss.getLocation().clone();
            World strikeWorld = strike.getWorld();
            if (strikeWorld == null) {
                return;
            }
            strikeWorld.spawnParticle(Particle.CRIT, strike.add(0.0D, 1.0D, 0.0D), 45, 1.1D, 0.25D, 1.1D, 0.08D);
            strikeWorld.spawnParticle(Particle.SWEEP_ATTACK, strike, 22, 1.0D, 0.15D, 1.0D, 0.01D);
            strikeWorld.playSound(strike, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.75F, 0.55F);
            damagePlayersInRadius(
                    boss,
                    strike,
                    3.4D,
                    bossEnraged ? 12.0D : 8.5D,
                    0.45D,
                    PotionEffectType.SLOWNESS,
                    bossEnraged ? 80 : 50,
                    1
            );
        }, 8L);
    }

    private void castRoninWhirlwind(LivingEntity boss) {
        broadcast(ChatColor.RED + floor.bossName() + " begins Whirlwind Cleave!");
        final int pulses = bossEnraged ? 4 : 3;

        new BukkitRunnable() {
            private int hits;

            @Override
            public void run() {
                if (ended || !bossActive || boss.isDead()) {
                    cancel();
                    return;
                }

                Location center = boss.getLocation().clone().add(0.0D, 0.8D, 0.0D);
                World world = center.getWorld();
                if (world == null) {
                    cancel();
                    return;
                }

                world.spawnParticle(Particle.SWEEP_ATTACK, center, 20, 1.2D, 0.3D, 1.2D, 0.01D);
                world.spawnParticle(Particle.CLOUD, center, 26, 1.0D, 0.15D, 1.0D, 0.02D);
                world.playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.4F, 0.6F);
                damagePlayersInRadius(
                        boss,
                        center,
                        4.1D,
                        bossEnraged ? 7.5D : 5.2D,
                        0.35D,
                        null,
                        0,
                        0
                );

                hits++;
                if (hits >= pulses) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private void castOnmyojiFangSurge(LivingEntity boss) {
        List<Player> targets = bossTargets();
        if (targets.isEmpty()) {
            return;
        }
        Collections.shuffle(targets);

        Location origin = boss.getLocation().clone();
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        broadcast(ChatColor.DARK_PURPLE + floor.bossName() + " casts Spirit Fang Surge!");
        world.playSound(origin, Sound.ENTITY_EVOKER_CAST_SPELL, 1.6F, 0.8F);

        int lanes = Math.min(targets.size(), bossEnraged ? 3 : 2);
        int fangsPerLane = bossEnraged ? 8 : 6;
        for (int i = 0; i < lanes; i++) {
            Player target = targets.get(i);
            Vector direction = target.getLocation().toVector().subtract(origin.toVector());
            if (direction.lengthSquared() < 0.1D) {
                continue;
            }
            direction = direction.normalize();

            for (int step = 1; step <= fangsPerLane; step++) {
                Location fangLoc = origin.clone().add(direction.clone().multiply(step * 1.4D));
                fangLoc.setY(target.getLocation().getY());
                world.spawnEntity(fangLoc, EntityType.EVOKER_FANGS);
                if (step % 2 == 0) {
                    world.spawnParticle(Particle.WITCH, fangLoc.clone().add(0.0D, 0.2D, 0.0D), 6, 0.2D, 0.05D, 0.2D, 0.02D);
                }
            }
        }
    }

    private void castOnmyojiHexVolley(LivingEntity boss) {
        Location center = boss.getLocation().clone();
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        broadcast(ChatColor.DARK_PURPLE + floor.bossName() + " unleashes Hex Volley!");
        world.playSound(center, Sound.ENTITY_EVOKER_CAST_SPELL, 1.3F, 1.1F);
        world.spawnParticle(Particle.WITCH, center.add(0.0D, 1.1D, 0.0D), 40, 1.1D, 0.3D, 1.1D, 0.03D);

        for (Player player : bossTargets()) {
            if (!Objects.equals(player.getWorld().getUID(), world.getUID())) {
                continue;
            }
            if (player.getLocation().distanceSquared(center) > 10.0D * 10.0D) {
                continue;
            }
            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR || player.isDead()) {
                continue;
            }
            player.damage(bossEnraged ? 7.8D : 5.5D, boss);
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, bossEnraged ? 120 : 80, 0, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, bossEnraged ? 80 : 60, 0, false, true));
            world.spawnParticle(Particle.WITCH, player.getLocation().add(0.0D, 1.0D, 0.0D), 14, 0.3D, 0.3D, 0.3D, 0.02D);
        }

        int summons = bossEnraged ? 2 : 1;
        for (int i = 0; i < summons; i++) {
            spawnDungeonMob(
                    floor.mobHealthMultiplier() * floorHealthScale * (bossEnraged ? 2.2D : 1.8D),
                    floor.mobDamageTier() + (bossEnraged ? 4 : 3),
                    distributedSpawnInRoom(currentRoomCenter, i, summons)
            );
        }
    }

    private void castStormThunderSlam(LivingEntity boss) {
        Player target = randomBossTarget();
        if (target == null) {
            return;
        }
        Location strike = target.getLocation().clone();
        World world = strike.getWorld();
        if (world == null) {
            return;
        }

        broadcast(ChatColor.BLUE + floor.bossName() + " calls down Thunder Slam!");
        world.strikeLightningEffect(strike);
        world.playSound(strike, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.8F, 0.95F);
        world.spawnParticle(Particle.ELECTRIC_SPARK, strike.add(0.0D, 1.0D, 0.0D), 45, 1.1D, 0.6D, 1.1D, 0.03D);
        damagePlayersInRadius(
                boss,
                strike,
                4.6D,
                bossEnraged ? 11.0D : 7.8D,
                0.52D,
                PotionEffectType.SLOWNESS,
                bossEnraged ? 90 : 60,
                0
        );
    }

    private void castStormStampedeCharge(LivingEntity boss) {
        Player target = randomBossTarget();
        if (target == null) {
            return;
        }
        Location bossLoc = boss.getLocation().clone();
        World world = bossLoc.getWorld();
        if (world == null) {
            return;
        }

        broadcast(ChatColor.BLUE + floor.bossName() + " launches Stampede Charge!");
        world.playSound(bossLoc, Sound.ENTITY_RAVAGER_ROAR, 1.4F, 0.8F);
        boss.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 80, bossEnraged ? 3 : 2, false, false));
        Vector charge = target.getLocation().toVector().subtract(bossLoc.toVector());
        if (charge.lengthSquared() > 0.2D) {
            charge = charge.normalize().multiply(bossEnraged ? 1.55D : 1.3D);
            charge.setY(0.25D);
            boss.setVelocity(charge);
        }

        new BukkitRunnable() {
            private int pulses;

            @Override
            public void run() {
                if (ended || !bossActive || boss.isDead()) {
                    cancel();
                    return;
                }
                Location center = boss.getLocation().clone().add(0.0D, 0.6D, 0.0D);
                World pulseWorld = center.getWorld();
                if (pulseWorld == null) {
                    cancel();
                    return;
                }
                pulseWorld.spawnParticle(Particle.CLOUD, center, 34, 1.2D, 0.2D, 1.2D, 0.06D);
                pulseWorld.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.0F, 1.1F);
                damagePlayersInRadius(
                        boss,
                        center,
                        4.0D,
                        bossEnraged ? 8.6D : 6.2D,
                        0.40D,
                        null,
                        0,
                        0
                );
                pulses++;
                if (pulses >= (bossEnraged ? 3 : 2)) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 8L, 12L);
    }

    private void castRaijinCataclysm(LivingEntity boss) {
        List<Player> targets = bossTargets();
        if (targets.isEmpty()) {
            return;
        }
        Location center = boss.getLocation().clone();
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        broadcast(ChatColor.LIGHT_PURPLE + floor.bossName() + " unleashes Cataclysmic Storm!");
        world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0F, 0.75F);
        world.spawnParticle(Particle.ELECTRIC_SPARK, center.add(0.0D, 1.2D, 0.0D), 70, 1.4D, 0.8D, 1.4D, 0.04D);

        for (Player player : targets) {
            if (!Objects.equals(player.getWorld().getUID(), world.getUID())) {
                continue;
            }
            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR || player.isDead()) {
                continue;
            }
            world.strikeLightningEffect(player.getLocation());
            player.damage(bossEnraged ? 10.8D : 7.6D, boss);
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, bossEnraged ? 140 : 90, 0, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, bossEnraged ? 100 : 60, 0, false, true));
        }
    }

    private void castRaijinGravityWell(LivingEntity boss) {
        Location center = boss.getLocation().clone();
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        broadcast(ChatColor.LIGHT_PURPLE + floor.bossName() + " creates a Gravity Well!");
        world.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.3F, 0.95F);
        world.spawnParticle(Particle.PORTAL, center.add(0.0D, 0.8D, 0.0D), 65, 1.3D, 0.5D, 1.3D, 0.05D);
        pullPlayersToward(center, 12.0D, bossEnraged ? 1.2D : 0.9D);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (ended || !bossActive || boss.isDead()) {
                return;
            }
            Location detonation = boss.getLocation().clone().add(0.0D, 0.8D, 0.0D);
            World detonationWorld = detonation.getWorld();
            if (detonationWorld == null) {
                return;
            }
            detonationWorld.spawnParticle(Particle.EXPLOSION, detonation, 3, 0.2D, 0.2D, 0.2D, 0.02D);
            detonationWorld.spawnParticle(Particle.ELECTRIC_SPARK, detonation, 70, 1.2D, 0.6D, 1.2D, 0.05D);
            detonationWorld.playSound(detonation, Sound.ENTITY_GENERIC_EXPLODE, 1.5F, 0.9F);
            damagePlayersInRadius(
                    boss,
                    detonation,
                    5.4D,
                    bossEnraged ? 12.5D : 9.0D,
                    0.58D,
                    PotionEffectType.SLOWNESS,
                    bossEnraged ? 80 : 50,
                    1
            );
        }, 20L);
    }

    private void castFallbackShadowNova(LivingEntity boss) {
        Location center = boss.getLocation().clone().add(0.0D, 0.8D, 0.0D);
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        broadcast(ChatColor.DARK_RED + floor.bossName() + " casts Shadow Nova!");
        world.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.4F, 0.8F);
        world.spawnParticle(Particle.SMOKE, center, 55, 1.3D, 0.5D, 1.3D, 0.06D);
        damagePlayersInRadius(
                boss,
                center,
                4.8D,
                bossEnraged ? 9.4D : 6.7D,
                0.40D,
                PotionEffectType.WEAKNESS,
                bossEnraged ? 100 : 60,
                0
        );
    }

    private void summonBossReinforcements(LivingEntity boss) {
        int aliveMinions = aliveTrackedMinionCount();
        int cap = bossEnraged ? 8 : 6;
        if (aliveMinions >= cap) {
            return;
        }
        int spawnCount = Math.min(cap - aliveMinions, bossEnraged ? 3 : 2);
        if (spawnCount <= 0) {
            return;
        }

        Location center = boss.getLocation();
        World world = center.getWorld();
        if (world != null) {
            world.playSound(center, Sound.ITEM_TOTEM_USE, 0.9F, 0.85F);
            world.spawnParticle(Particle.SOUL, center.add(0.0D, 1.0D, 0.0D), 24, 0.7D, 0.35D, 0.7D, 0.02D);
        }
        broadcast(ChatColor.DARK_RED + floor.bossName() + " summons reinforcements!");

        for (int i = 0; i < spawnCount; i++) {
            spawnDungeonMob(
                    floor.mobHealthMultiplier() * floorHealthScale * (bossEnraged ? 2.35D : 2.0D),
                    floor.mobDamageTier() + (bossEnraged ? 5 : 4),
                    distributedSpawnInRoom(currentRoomCenter, i, spawnCount)
            );
        }
    }

    private List<Player> bossTargets() {
        List<Player> online = new ArrayList<>();
        for (Player player : onlineMembers(null)) {
            if (player.getGameMode() == GameMode.SPECTATOR || player.isDead()) {
                continue;
            }
            online.add(player);
        }
        if (online.isEmpty() || currentRoomCenter == null || currentRoomCenter.getWorld() == null) {
            return online;
        }

        List<Player> inRoom = new ArrayList<>();
        World world = currentRoomCenter.getWorld();
        double range = Math.max(14.0D, (floor.roomSize() / 2.0D) + 8.0D);
        double rangeSquared = range * range;
        for (Player player : online) {
            if (!Objects.equals(player.getWorld().getUID(), world.getUID())) {
                continue;
            }
            if (player.getGameMode() == GameMode.SPECTATOR || player.isDead()) {
                continue;
            }
            if (player.getLocation().distanceSquared(currentRoomCenter) <= rangeSquared) {
                inRoom.add(player);
            }
        }

        return inRoom;
    }

    private Player randomBossTarget() {
        List<Player> targets = bossTargets();
        if (targets.isEmpty()) {
            return null;
        }
        return targets.get(ThreadLocalRandom.current().nextInt(targets.size()));
    }

    private void damagePlayersInRadius(
            LivingEntity source,
            Location center,
            double radius,
            double damage,
            double verticalKnockback,
            PotionEffectType debuffType,
            int debuffDurationTicks,
            int debuffAmplifier
    ) {
        if (center == null || center.getWorld() == null) {
            return;
        }
        double radiusSquared = radius * radius;
        for (Player player : bossTargets()) {
            if (!Objects.equals(player.getWorld().getUID(), center.getWorld().getUID())) {
                continue;
            }
            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR || player.isDead()) {
                continue;
            }
            if (player.getLocation().distanceSquared(center) > radiusSquared) {
                continue;
            }

            player.damage(damage, source);

            Vector push = player.getLocation().toVector().subtract(center.toVector());
            if (push.lengthSquared() < 1.0E-4D) {
                push = new Vector(
                        ThreadLocalRandom.current().nextDouble(-0.25D, 0.25D),
                        0.0D,
                        ThreadLocalRandom.current().nextDouble(-0.25D, 0.25D)
                );
            }
            if (push.lengthSquared() >= 1.0E-4D) {
                push = push.normalize().multiply(0.58D);
                push.setY(verticalKnockback);
                player.setVelocity(player.getVelocity().add(push));
            }

            if (debuffType != null && debuffDurationTicks > 0) {
                player.addPotionEffect(new PotionEffect(
                        debuffType,
                        debuffDurationTicks,
                        Math.max(0, debuffAmplifier),
                        false,
                        true
                ));
            }
        }
    }

    private void pullPlayersToward(Location center, double radius, double pullStrength) {
        if (center == null || center.getWorld() == null) {
            return;
        }
        double radiusSquared = radius * radius;
        for (Player player : bossTargets()) {
            if (!Objects.equals(player.getWorld().getUID(), center.getWorld().getUID())) {
                continue;
            }
            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR || player.isDead()) {
                continue;
            }
            if (player.getLocation().distanceSquared(center) > radiusSquared) {
                continue;
            }

            Vector pull = center.toVector().subtract(player.getLocation().toVector());
            if (pull.lengthSquared() < 1.0E-4D) {
                continue;
            }
            pull = pull.normalize().multiply(pullStrength);
            pull.setY(0.30D);
            player.setVelocity(player.getVelocity().add(pull));
        }
    }

    private int aliveTrackedMinionCount() {
        int alive = 0;
        for (TrackedMobInfo info : trackedMobs.values()) {
            if (!info.boss()) {
                alive++;
            }
        }
        return alive;
    }

    private int randomAbilityCooldownSeconds() {
        int floorLevel = Math.max(1, floorNumber(floor.id()));
        int fastFloorBonus = Math.min(2, floorLevel / 2);
        if (bossEnraged) {
            int min = Math.max(3, 5 - fastFloorBonus);
            int max = Math.max(min, 7 - fastFloorBonus);
            return ThreadLocalRandom.current().nextInt(min, max + 1);
        }
        int min = Math.max(5, 7 - fastFloorBonus);
        int max = Math.max(min, 10 - fastFloorBonus);
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private int randomReinforcementCooldownSeconds() {
        if (bossEnraged) {
            return ThreadLocalRandom.current().nextInt(7, 11);
        }
        return ThreadLocalRandom.current().nextInt(11, 16);
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
                case TENGU_SKIRMISHER -> switch (random.nextInt(3)) {
                    case 0 -> CustomWeaponType.TENGU_GALEBLADE;
                    case 1 -> CustomWeaponType.TENGU_STORMBOW;
                    default -> CustomWeaponType.TENGU_SHORTBOW;
                };
                case KAPPA_RAIDER -> CustomWeaponType.KAPPA_TIDEBREAKER;
                case ONRYO_WRAITH -> random.nextBoolean()
                        ? CustomWeaponType.ONRYO_SPIRITBLADE
                        : CustomWeaponType.ONRYO_SHORTBOW;
                case JOROGUMO_WEAVER -> random.nextBoolean()
                        ? CustomWeaponType.JOROGUMO_STINGER
                        : CustomWeaponType.JOROGUMO_SHORTBOW;
                case KITSUNE_TRICKSTER -> switch (random.nextInt(3)) {
                    case 0 -> CustomWeaponType.KITSUNE_FANG;
                    case 1 -> CustomWeaponType.KITSUNE_DAWNBOW;
                    default -> CustomWeaponType.KITSUNE_SHORTBOW;
                };
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
            int floorLevel = Math.max(1, floorNumber(floor.id()));
            double partyScale = 1.0D + (Math.max(0, members.size() - 1) * 0.05D);
            double tierScale = 1.0D + (Math.max(0, damageTier) * 0.28D);
            double floorScale = 1.0D + ((floorLevel - 1) * 0.10D);
            double bossScale = boss ? 1.55D : 1.0D;
            double scaledDamage = Math.max(1.0D, attack.getBaseValue() * tierScale * floorScale * partyScale * bossScale);
            attack.setBaseValue(scaledDamage);
        }

        int speedAmplifier = Math.min(3, (boss ? 1 : 0) + (damageTier / 3));
        if (speedAmplifier > 0) {
            living.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, speedAmplifier, false, false));
        }
        if (boss) {
            living.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 2, false, false));
            living.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 2, false, false));
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
                    LivingEntity boss = resolveActiveBoss();
                    if (boss != null) {
                        String bossState = bossHealthStatus(boss);
                        stage = bossState.isBlank() ? "Shogun" : "Shogun " + bossState;
                    } else {
                        stage = "Shogun";
                    }
                } else if (currentEncounterIndex >= 0 && currentEncounterIndex < encounterPlan.size()) {
                    RoomType type = encounterPlan.get(currentEncounterIndex);
                    stage = "Room " + (currentEncounterIndex + 1) + "/" + encounterPlan.size() + " " + type.displayName();
                } else {
                    stage = "Preparing";
                }

                String puzzleProgress = "";
                if (!bossActive && currentRoomType == RoomType.PUZZLE_SEQUENCE && !sequenceOrder.isEmpty()) {
                    int expectedIndex = Math.max(0, Math.min(sequenceProgress, sequenceOrder.size() - 1));
                    int nextPad = sequenceOrder.get(expectedIndex);
                    puzzleProgress = " | Seq " + sequenceProgress + "/" + sequenceOrder.size() + " | Next " + colorNamePlain(nextPad);
                } else if (!bossActive && currentRoomType == RoomType.PUZZLE_SYNC) {
                    int required = Math.max(1, syncRequiredPlates);
                    puzzleProgress = " | Sync " + syncPoweredPlates + "/" + required + " | Hold " + syncStableTicks + "/2";
                } else if (!bossActive && currentRoomType == RoomType.PUZZLE_CHIME && !chimeOrder.isEmpty()) {
                    int expectedIndex = Math.max(0, Math.min(chimeProgress, chimeOrder.size() - 1));
                    int nextBell = chimeOrder.get(expectedIndex);
                    puzzleProgress = " | Chime " + chimeProgress + "/" + chimeOrder.size() + " | Next " + chimeNamePlain(nextBell);
                } else if (!bossActive && currentRoomType == RoomType.PUZZLE_SEAL) {
                    int total = Math.max(1, sealLeverKeys.size());
                    puzzleProgress = " | Seal " + sealMatchedLevers + "/" + total + " | Hold " + sealStableTicks + "/2";
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
        stopBossControllerTask();
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
        sequencePadLocationByIndex.clear();
        sequenceOrder.clear();
        sequenceProgress = 0;
        syncPlateKeys.clear();
        syncPlateLocations.clear();
        syncStableTicks = 0;
        syncRequiredPlates = 0;
        syncPoweredPlates = 0;
        syncLastPoweredPlates = -1;
        chimeBellIndexByKey.clear();
        chimeBellLocationByIndex.clear();
        chimeOrder.clear();
        chimeProgress = 0;
        sealLeverKeys.clear();
        sealLeverLocations.clear();
        sealTargetStates.clear();
        sealMatchedLevers = 0;
        sealStableTicks = 0;
        sealLastMatchedLevers = -1;
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
                        + ChatColor.BLACK + "Plates sit in each corner.\n"
                        + "Action bar shows plates needed.\n"
                        + "Stand on plates together and\n"
                        + "hold for 2 seconds to clear.\n\n"
                        + "Sparkles mark empty plates."
        );
        meta.addPage(
                ChatColor.DARK_RED + "Ofuda Sequence\n\n"
                        + ChatColor.BLACK + "Pad row (L -> R):\n"
                        + "Vermilion, Jade, Indigo, Gold.\n"
                        + "Chat shows the order to press.\n"
                        + "Follow exactly; next pad glows.\n"
                        + "Higher floors = longer combos.\n"
                        + "Wrong pad resets the chain."
        );
        meta.addPage(
                ChatColor.AQUA + "Storm Chime Memory\n\n"
                        + ChatColor.BLACK + "Four bells chime in order:\n"
                        + "North, West, East, South.\n"
                        + "Memorize the pattern shown,\n"
                        + "then ring bells in that order.\n"
                        + "Notes point to the next bell.\n"
                        + "Mistakes reset the sequence."
        );
        meta.addPage(
                ChatColor.DARK_PURPLE + "Thunder Seal Align\n\n"
                        + ChatColor.BLACK + "Corner levers have targets:\n"
                        + "Lime glass = Lever ON (up)\n"
                        + "Gray glass = Lever OFF (down)\n"
                        + "Match all levers, then hold\n"
                        + "positions for 2 seconds."
        );
        meta.addPage(
                ChatColor.DARK_GREEN + "Tips\n\n"
                        + ChatColor.BLACK + "Assign callers for orders.\n"
                        + "Count down before group steps.\n"
                        + "Use action bar for timers/count.\n"
                        + "Particles show next target or\n"
                        + "missing plates.\n"
                        + "If reset, regroup and retry fast."
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
        if (focusRoom == RoomType.PUZZLE_CHIME) {
            return ChatColor.AQUA + "Storm Chime Memory";
        }
        if (focusRoom == RoomType.PUZZLE_SEAL) {
            return ChatColor.DARK_PURPLE + "Thunder Seal Alignment";
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
                CustomWeaponType.TENGU_SHORTBOW,
                CustomWeaponType.KAPPA_TIDEBREAKER,
                CustomWeaponType.ONRYO_SPIRITBLADE,
                CustomWeaponType.ONRYO_SHORTBOW,
                CustomWeaponType.JOROGUMO_STINGER,
                CustomWeaponType.JOROGUMO_SHORTBOW,
                CustomWeaponType.KITSUNE_FANG,
                CustomWeaponType.KITSUNE_DAWNBOW,
                CustomWeaponType.KITSUNE_SHORTBOW
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

    private int sequenceLengthForFloor() {
        int floorLevel = Math.max(1, floorNumber(floor.id()));
        return Math.max(4, Math.min(12, 4 + (floorLevel - 1)));
    }

    private int chimeLengthForFloor() {
        int floorLevel = Math.max(1, floorNumber(floor.id()));
        return Math.max(3, Math.min(9, 3 + floorLevel));
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

    private String formatChimeOrder(List<Integer> order) {
        List<String> names = new ArrayList<>();
        for (Integer index : order) {
            names.add(chimeName(index));
        }
        return String.join(" -> ", names);
    }

    private String formatSealTargetPattern() {
        List<String> pattern = new ArrayList<>();
        for (int i = 0; i < sealTargetStates.size(); i++) {
            pattern.add(sealLeverLabel(i) + " " + (sealTargetStates.get(i) ? "ON" : "OFF"));
        }
        return String.join(", ", pattern);
    }

    private String chimeName(int index) {
        return switch (index) {
            case 0 -> ChatColor.AQUA + "North";
            case 1 -> ChatColor.GREEN + "West";
            case 2 -> ChatColor.GOLD + "East";
            case 3 -> ChatColor.LIGHT_PURPLE + "South";
            default -> ChatColor.GRAY + "Unknown";
        };
    }

    private String chimeNamePlain(int index) {
        return switch (index) {
            case 0 -> "North";
            case 1 -> "West";
            case 2 -> "East";
            case 3 -> "South";
            default -> "Unknown";
        };
    }

    private String sealLeverLabel(int index) {
        return switch (index) {
            case 0 -> "NW";
            case 1 -> "NE";
            case 2 -> "SW";
            case 3 -> "SE";
            default -> "#" + (index + 1);
        };
    }

    private String colorName(int index) {
        return switch (index) {
            case 0 -> ChatColor.RED + "Vermilion";
            case 1 -> ChatColor.GREEN + "Jade";
            case 2 -> ChatColor.BLUE + "Indigo";
            case 3 -> ChatColor.GOLD + "Gold";
            default -> ChatColor.GRAY + "Unknown";
        };
    }

    private String colorNamePlain(int index) {
        return switch (index) {
            case 0 -> "Vermilion";
            case 1 -> "Jade";
            case 2 -> "Indigo";
            case 3 -> "Gold";
            default -> "Unknown";
        };
    }

    private String bossHealthStatus(LivingEntity boss) {
        AttributeInstance maxHealth = boss.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null || maxHealth.getValue() <= 0.0D) {
            return bossEnraged ? "ENRAGED" : "";
        }
        int healthPercent = (int) Math.round((boss.getHealth() / maxHealth.getValue()) * 100.0D);
        healthPercent = Math.max(0, Math.min(100, healthPercent));
        return "HP " + healthPercent + "%" + (bossEnraged ? " ENRAGED" : "");
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

    private enum BossArchetype {
        RONIN_GATEKEEPER,
        ONMYOJI_SHOGUN,
        STORM_DAIMYO,
        RAIJIN_AVATAR,
        TEMPLE_WARLORD;

        private static BossArchetype from(EntityType entityType) {
            return switch (entityType) {
                case VINDICATOR -> RONIN_GATEKEEPER;
                case EVOKER -> ONMYOJI_SHOGUN;
                case RAVAGER -> STORM_DAIMYO;
                case WARDEN -> RAIJIN_AVATAR;
                default -> TEMPLE_WARLORD;
            };
        }
    }

    private record TrackedMobInfo(boolean boss, YokaiType yokaiType) {
    }
}
