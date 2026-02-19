package io.papermc.Grivience.dungeon;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.party.Party;
import io.papermc.Grivience.party.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class DungeonManager {
    private final GriviencePlugin plugin;
    private final PartyManager partyManager;
    private final CustomItemService customItemService;

    private final Map<UUID, DungeonSession> sessionsByParty = new HashMap<>();
    private final Map<UUID, DungeonSession> sessionsByPlayer = new HashMap<>();
    private final Map<UUID, DungeonSession> sessionsByMob = new HashMap<>();
    private final Map<String, FloorConfig> floors = new LinkedHashMap<>();

    private final ArrayDeque<Integer> freeArenaSlots = new ArrayDeque<>();
    private int nextArenaSlot;

    private String dungeonWorldName;
    private String exitWorldName;
    private Location origin;
    private int arenaSpacing;
    private int startCountdownSeconds;
    private String exitCommand;

    public DungeonManager(GriviencePlugin plugin, PartyManager partyManager, CustomItemService customItemService) {
        this.plugin = plugin;
        this.partyManager = partyManager;
        this.customItemService = customItemService;
        reloadFromConfig();
    }

    public void reloadFromConfig() {
        dungeonWorldName = plugin.getConfig().getString("dungeons.world", "world");
        exitWorldName = plugin.getConfig().getString("dungeons.exit-world", dungeonWorldName);

        double x = plugin.getConfig().getDouble("dungeons.origin.x", 0.0D);
        double y = plugin.getConfig().getDouble("dungeons.origin.y", 120.0D);
        double z = plugin.getConfig().getDouble("dungeons.origin.z", 0.0D);
        origin = new Location(null, x, y, z);

        arenaSpacing = Math.max(70, plugin.getConfig().getInt("dungeons.arena-spacing", 140));
        startCountdownSeconds = Math.max(1, plugin.getConfig().getInt("dungeons.countdown-seconds", 5));
        exitCommand = normalizeCommand(plugin.getConfig().getString("dungeons.exit-command", "spawn"));

        floors.clear();
        ConfigurationSection floorsSection = plugin.getConfig().getConfigurationSection("floors");
        if (floorsSection != null) {
            for (String floorId : floorsSection.getKeys(false)) {
                ConfigurationSection floorSection = floorsSection.getConfigurationSection(floorId);
                if (floorSection == null) {
                    continue;
                }
                FloorConfig config = FloorConfig.fromSection(floorId, floorSection);
                floors.put(config.id(), config);
            }
        }

        if (floors.isEmpty()) {
            plugin.getLogger().warning("No dungeon floors configured. Add floors in config.yml under 'floors'.");
        }
    }

    public String startDungeon(Player leader, String floorIdRaw) {
        Party party = partyManager.getParty(leader.getUniqueId());
        if (party == null) {
            return "You must be in a party to start a dungeon.";
        }
        if (!party.isLeader(leader.getUniqueId())) {
            return "Only the party leader can start a dungeon.";
        }
        if (sessionsByParty.containsKey(party.id())) {
            return "Your party already has an active dungeon run.";
        }

        String floorId = floorIdRaw.toUpperCase(Locale.ROOT);
        FloorConfig floor = floors.get(floorId);
        if (floor == null) {
            return "Unknown floor '" + floorIdRaw + "'. Use /dungeon floors.";
        }

        Set<UUID> onlineMembers = party.members().stream()
                .filter(this::isOnline)
                .collect(Collectors.toSet());

        if (onlineMembers.size() < floor.minPartySize()) {
            return "Need at least " + floor.minPartySize() + " online players for " + floor.id() + ".";
        }
        if (onlineMembers.size() > floor.maxPartySize()) {
            return "This floor supports up to " + floor.maxPartySize() + " players.";
        }

        World world = Bukkit.getWorld(dungeonWorldName);
        if (world == null) {
            return "Configured dungeon world '" + dungeonWorldName + "' does not exist.";
        }

        int slot = allocateArenaSlot();
        Location anchor = new Location(
                world,
                origin.getX() + (slot * arenaSpacing),
                origin.getY(),
                origin.getZ()
        );

        List<RoomType> encounterPlan = buildEncounterPlan(floor);
        ArenaLayout layout = DungeonBuilder.buildArena(world, anchor, floor, encounterPlan);
        DungeonSession session = new DungeonSession(
                plugin,
                this,
                customItemService,
                party.id(),
                onlineMembers,
                floor,
                slot,
                layout,
                encounterPlan
        );

        sessionsByParty.put(party.id(), session);
        for (UUID member : onlineMembers) {
            sessionsByPlayer.put(member, session);
        }
        session.start(startCountdownSeconds);
        return null;
    }

    public String abandonDungeon(UUID playerId, String actorName) {
        DungeonSession session = sessionsByPlayer.get(playerId);
        if (session == null) {
            return "You are not in an active dungeon run.";
        }
        session.abandon(actorName);
        return null;
    }

    public boolean isInDungeon(UUID playerId) {
        return sessionsByPlayer.containsKey(playerId);
    }

    public boolean areInSameSession(UUID first, UUID second) {
        if (first == null || second == null) {
            return false;
        }
        DungeonSession firstSession = sessionsByPlayer.get(first);
        if (firstSession == null) {
            return false;
        }
        return firstSession == sessionsByPlayer.get(second);
    }

    public DungeonSession getSession(UUID playerId) {
        return sessionsByPlayer.get(playerId);
    }

    public void trackMob(UUID mobId, DungeonSession session) {
        sessionsByMob.put(mobId, session);
    }

    public void untrackMob(UUID mobId) {
        sessionsByMob.remove(mobId);
    }

    public void handleMobDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        DungeonSession session = sessionsByMob.remove(entity.getUniqueId());
        if (session == null) {
            return;
        }
        session.handleTrackedMobDeath(entity, event);
    }

    public void handlePlayerDeath(UUID playerId) {
        DungeonSession session = sessionsByPlayer.get(playerId);
        if (session != null) {
            session.recordDeath(playerId);
        }
    }

    public Location getRespawnLocation(UUID playerId) {
        DungeonSession session = sessionsByPlayer.get(playerId);
        if (session == null) {
            return null;
        }
        return session.respawnLocationFor(playerId);
    }

    public void handlePlayerQuit(UUID playerId) {
        DungeonSession session = sessionsByPlayer.get(playerId);
        if (session != null) {
            session.handlePlayerQuit(playerId);
        }
    }

    public void handlePlayerJoin(Player player) {
        DungeonSession session = sessionsByPlayer.get(player.getUniqueId());
        if (session != null) {
            session.handlePlayerJoin(player);
        }
    }

    public void handlePlayerInteract(Player player, Action action, Block clickedBlock) {
        DungeonSession session = sessionsByPlayer.get(player.getUniqueId());
        if (session != null) {
            session.handlePlayerInteract(player, action, clickedBlock);
        }
    }

    public void handlePlayerMove(Player player, Location from, Location to) {
        DungeonSession session = sessionsByPlayer.get(player.getUniqueId());
        if (session != null) {
            session.handlePlayerMove(player, from, to);
        }
    }

    public void finalizeSession(
            DungeonSession session,
            boolean success,
            String grade,
            int score,
            long elapsedSeconds,
            String reason
    ) {
        sessionsByParty.remove(session.partyId());
        for (UUID playerId : session.members()) {
            sessionsByPlayer.remove(playerId);
        }
        sessionsByMob.entrySet().removeIf(entry -> entry.getValue() == session);
        freeArenaSlots.offer(session.arenaSlot());

        if (success) {
            executeRewards(session, grade, score);
        }

        for (UUID memberId : session.members()) {
            Player player = Bukkit.getPlayer(memberId);
            if (player == null) {
                continue;
            }

            player.sendMessage(ChatColor.GOLD + "[Dungeon] " + ChatColor.YELLOW + reason);
            if (success) {
                player.sendMessage(ChatColor.GOLD + "[Dungeon] " + ChatColor.GREEN + "Grade: " + grade + ChatColor.GRAY
                        + " | Score: " + score + ChatColor.GRAY + " | Time: " + formatTime(elapsedSeconds));
            } else {
                player.sendMessage(ChatColor.GOLD + "[Dungeon] " + ChatColor.RED + "Run failed.");
            }

            sendPlayerToExit(player);
        }

        cleanupArena(session);
    }

    public String nameOf(UUID playerId) {
        return partyManager.nameOf(playerId);
    }

    public Collection<FloorConfig> floors() {
        return List.copyOf(floors.values());
    }

    public List<String> floorIds() {
        return new ArrayList<>(floors.keySet());
    }

    public void shutdown() {
        List<DungeonSession> active = new ArrayList<>(sessionsByParty.values());
        for (DungeonSession session : active) {
            session.forceEnd("Server is shutting down.");
        }
        sessionsByParty.clear();
        sessionsByPlayer.clear();
        sessionsByMob.clear();
        freeArenaSlots.clear();
        nextArenaSlot = 0;
    }

    private void executeRewards(DungeonSession session, String grade, int score) {
        List<String> rewardCommands = session.floor().rewardsForGrade(grade);
        if (rewardCommands.isEmpty()) {
            return;
        }

        for (UUID memberId : session.members()) {
            Player player = Bukkit.getPlayer(memberId);
            if (player == null) {
                continue;
            }

            for (String command : rewardCommands) {
                String parsed = command
                        .replace("{player}", player.getName())
                        .replace("{floor}", session.floor().id())
                        .replace("{grade}", grade)
                        .replace("{score}", Integer.toString(score));
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
            }
        }
    }

    private int allocateArenaSlot() {
        Integer reused = freeArenaSlots.poll();
        if (reused != null) {
            return reused;
        }
        return nextArenaSlot++;
    }

    private List<RoomType> buildEncounterPlan(FloorConfig floor) {
        List<RoomType> plan = new ArrayList<>();
        for (int i = 0; i < floor.combatRooms(); i++) {
            plan.add(RoomType.COMBAT);
        }
        for (int i = 0; i < floor.puzzleRooms(); i++) {
            plan.add(randomPuzzleType(floor));
        }
        for (int i = 0; i < floor.treasureRooms(); i++) {
            plan.add(RoomType.TREASURE);
        }

        if (plan.isEmpty()) {
            plan.add(RoomType.COMBAT);
        }
        Collections.shuffle(plan);
        return plan;
    }

    private RoomType randomPuzzleType(FloorConfig floor) {
        List<RoomType> types = floor.puzzleTypes();
        if (types.isEmpty()) {
            RoomType[] fallback = {
                    RoomType.PUZZLE_SEQUENCE,
                    RoomType.PUZZLE_SYNC,
                    RoomType.PUZZLE_CHIME,
                    RoomType.PUZZLE_SEAL
            };
            return fallback[ThreadLocalRandom.current().nextInt(fallback.length)];
        }
        return types.get(ThreadLocalRandom.current().nextInt(types.size()));
    }

    private boolean isOnline(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        return player != null && player.isOnline();
    }

    private Location resolveExitSpawn(Location fallback) {
        World exitWorld = Bukkit.getWorld(exitWorldName);
        if (exitWorld != null) {
            return exitWorld.getSpawnLocation();
        }
        if (!Bukkit.getWorlds().isEmpty()) {
            return Bukkit.getWorlds().getFirst().getSpawnLocation();
        }
        return fallback;
    }

    private void sendPlayerToExit(Player player) {
        String command = exitCommand;
        if (command != null && !command.isBlank()) {
            boolean commandRan = player.performCommand(command);
            if (commandRan) {
                return;
            }
        }
        Location exitLocation = resolveExitSpawn(player.getWorld().getSpawnLocation());
        player.teleport(exitLocation);
    }

    private String normalizeCommand(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("/")) {
            return trimmed.substring(1);
        }
        return trimmed;
    }

    private String formatTime(long seconds) {
        long minutesPart = seconds / 60L;
        long secondsPart = seconds % 60L;
        return String.format("%02d:%02d", minutesPart, secondsPart);
    }

    private void cleanupArena(DungeonSession session) {
        World world = session.arenaWorld();
        if (world == null) {
            return;
        }
        List<ArenaLayout.Cuboid> volumes = session.cleanupVolumes();
        if (volumes.isEmpty()) {
            return;
        }

        for (ArenaLayout.Cuboid cuboid : volumes) {
            for (int x = cuboid.minX(); x <= cuboid.maxX(); x++) {
                for (int y = cuboid.minY(); y <= cuboid.maxY(); y++) {
                    for (int z = cuboid.minZ(); z <= cuboid.maxZ(); z++) {
                        Block block = world.getBlockAt(x, y, z);
                        if (block.getType() == Material.AIR) {
                            continue;
                        }
                        block.setType(Material.AIR, false);
                    }
                }
            }
        }
    }
}
