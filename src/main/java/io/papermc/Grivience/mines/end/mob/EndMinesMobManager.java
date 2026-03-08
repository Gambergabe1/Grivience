package io.papermc.Grivience.mines.end.mob;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.collections.CollectionsManager;
import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.item.EndMinesMaterialType;
import io.papermc.Grivience.mines.end.EndMinesManager;
import io.papermc.Grivience.util.MobHealthDisplay;
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
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/**
 * Spawns and controls End Mines-specific mobs with unique combat mechanics.
 */
public final class EndMinesMobManager implements Listener {
    private final GriviencePlugin plugin;
    private final EndMinesManager endMinesManager;
    private final CustomItemService customItemService;
    private final CollectionsManager collectionsManager;
    private final NamespacedKey mobTypeKey;
    private final NamespacedKey healthBaseNameKey;
    private final NamespacedKey projectileKey;
    private final Random random = new Random();
    private final Map<UUID, MobState> mobs = new HashMap<>();
    private final File spawnPointsFile;
    private final Map<UUID, EndMinesMobSpawnPoint> spawnPoints = new LinkedHashMap<>();
    private final Map<UUID, Integer> spawnPointCooldownTicks = new HashMap<>();

    private BukkitTask spawnTask;
    private BukkitTask abilityTask;

    private static final Set<Material> VALID_SPAWN_FLOORS = EnumSet.of(
            Material.END_STONE,
            Material.END_STONE_BRICKS,
            Material.PURPUR_BLOCK,
            Material.PURPUR_PILLAR,
            Material.OBSIDIAN,
            Material.CRYING_OBSIDIAN,
            Material.AMETHYST_BLOCK,
            Material.BUDDING_AMETHYST
    );

    private record DropSpec(EndMinesMaterialType type, double chance, int min, int max, boolean rare) {
    }

    private static final Map<EndMinesMobType, DropSpec[]> DROPS = new EnumMap<>(EndMinesMobType.class);

    static {
        DROPS.put(EndMinesMobType.RIFTWALKER, new DropSpec[]{
                new DropSpec(EndMinesMaterialType.ENDSTONE_SHARD, 0.75D, 1, 3, false),
                new DropSpec(EndMinesMaterialType.RIFT_ESSENCE, 0.18D, 1, 1, true),
                new DropSpec(EndMinesMaterialType.VOID_CRYSTAL, 0.02D, 1, 1, true)
        });
        DROPS.put(EndMinesMobType.CRYSTAL_SENTRY, new DropSpec[]{
                new DropSpec(EndMinesMaterialType.RIFT_ESSENCE, 0.30D, 1, 1, true),
                new DropSpec(EndMinesMaterialType.VOID_CRYSTAL, 0.12D, 1, 1, true),
                new DropSpec(EndMinesMaterialType.CHORUS_WEAVE, 0.20D, 1, 1, true)
        });
        DROPS.put(EndMinesMobType.OBSIDIAN_GOLEM, new DropSpec[]{
                new DropSpec(EndMinesMaterialType.OBSIDIAN_CORE, 0.65D, 1, 2, true),
                new DropSpec(EndMinesMaterialType.RIFT_ESSENCE, 0.22D, 1, 1, true),
                new DropSpec(EndMinesMaterialType.VOID_CRYSTAL, 0.04D, 1, 1, true)
        });
        
        // New Mobs
        DROPS.put(EndMinesMobType.ZOMBIE_MINER, new DropSpec[]{
                new DropSpec(EndMinesMaterialType.ORE_FRAGMENT, 0.40D, 1, 2, false)
        });
        DROPS.put(EndMinesMobType.SKELETON_WATCHER, new DropSpec[]{
                new DropSpec(EndMinesMaterialType.ORE_FRAGMENT, 0.40D, 1, 2, false)
        });
        DROPS.put(EndMinesMobType.CAVE_SPIDER_SWARM, new DropSpec[]{
                new DropSpec(EndMinesMaterialType.ORE_FRAGMENT, 0.20D, 1, 1, false)
        });
        DROPS.put(EndMinesMobType.ENDERMAN_EXCAVATOR, new DropSpec[]{
                new DropSpec(EndMinesMaterialType.ORE_FRAGMENT, 0.60D, 2, 4, false)
        });
        DROPS.put(EndMinesMobType.IRON_GOLEM_GUARDIAN, new DropSpec[]{
                new DropSpec(EndMinesMaterialType.ORE_FRAGMENT, 1.00D, 10, 20, true)
        });
    }

    private static final class MobState {
        private final EndMinesMobType type;
        private int abilityCooldownTicks;

        private MobState(EndMinesMobType type, int abilityCooldownTicks) {
            this.type = type;
            this.abilityCooldownTicks = abilityCooldownTicks;
        }
    }

    public EndMinesMobManager(
            GriviencePlugin plugin,
            EndMinesManager endMinesManager,
            CustomItemService customItemService,
            CollectionsManager collectionsManager
    ) {
        this.plugin = plugin;
        this.endMinesManager = endMinesManager;
        this.customItemService = customItemService;
        this.collectionsManager = collectionsManager;
        this.mobTypeKey = new NamespacedKey(plugin, "end_mines_mob_type");
        this.healthBaseNameKey = new NamespacedKey(plugin, "health_base_name");
        this.projectileKey = new NamespacedKey(plugin, "end_mines_projectile");
        plugin.getDataFolder().mkdirs();
        this.spawnPointsFile = new File(plugin.getDataFolder(), "end-mines-mob-spawns.yml");
        loadSpawnPoints();
    }

    public void enable() {
        if (spawnTask != null || abilityTask != null) {
            return;
        }

        // Refresh configured spawn points on (re)enable so edits via commands apply cleanly.
        loadSpawnPoints();

        long spawnInterval = Math.max(10L, plugin.getConfig().getLong("end-mines.mobs.spawn-interval-ticks", 40L));
        spawnTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::spawnTick, spawnInterval, spawnInterval);

        abilityTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::abilityTick, 10L, 10L);
    }

    public void shutdown() {
        if (spawnTask != null) {
            spawnTask.cancel();
            spawnTask = null;
        }
        if (abilityTask != null) {
            abilityTask.cancel();
            abilityTask = null;
        }
        mobs.clear();
    }

    public Collection<EndMinesMobSpawnPoint> getSpawnPoints() {
        return Collections.unmodifiableCollection(spawnPoints.values());
    }

    public EndMinesMobSpawnPoint getSpawnPoint(UUID id) {
        if (id == null) {
            return null;
        }
        return spawnPoints.get(id);
    }

    public EndMinesMobSpawnPoint createSpawnPoint(Location location, EndMinesMobType type, int spawnRadius, int spawnDelayTicks, int maxNearbyEntities) {
        if (location == null || location.getWorld() == null || type == null) {
            return null;
        }
        UUID id = UUID.randomUUID();
        EndMinesMobSpawnPoint point = new EndMinesMobSpawnPoint(id);
        point.setMobTypeId(type.id());
        point.setLocation(location);
        point.setSpawnRadius(spawnRadius);
        point.setSpawnDelayTicks(spawnDelayTicks);
        point.setMaxNearbyEntities(maxNearbyEntities);
        spawnPoints.put(id, point);
        spawnPointCooldownTicks.put(id, 0);
        saveSpawnPoints();
        return point;
    }

    public boolean removeSpawnPoint(UUID id) {
        if (id == null) {
            return false;
        }
        EndMinesMobSpawnPoint removed = spawnPoints.remove(id);
        spawnPointCooldownTicks.remove(id);
        if (removed != null) {
            saveSpawnPoints();
            return true;
        }
        return false;
    }

    public boolean toggleSpawnPoint(UUID id) {
        EndMinesMobSpawnPoint point = getSpawnPoint(id);
        if (point == null) {
            return false;
        }
        point.setActive(!point.active());
        saveSpawnPoints();
        return true;
    }

    public boolean updateSpawnPoint(UUID id, Consumer<EndMinesMobSpawnPoint> editor) {
        if (id == null || editor == null) {
            return false;
        }
        EndMinesMobSpawnPoint point = spawnPoints.get(id);
        if (point == null) {
            return false;
        }
        editor.accept(point);
        spawnPointCooldownTicks.put(id, Math.max(0, point.spawnDelayTicks()));
        saveSpawnPoints();
        return true;
    }

    private void loadSpawnPoints() {
        spawnPoints.clear();
        spawnPointCooldownTicks.clear();

        if (!spawnPointsFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(spawnPointsFile);
        ConfigurationSection section = config.getConfigurationSection("spawn-points");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            if (key == null || key.isBlank()) {
                continue;
            }
            UUID id;
            try {
                id = UUID.fromString(key);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            ConfigurationSection pointSection = section.getConfigurationSection(key);
            if (pointSection == null) {
                continue;
            }

            EndMinesMobSpawnPoint point = EndMinesMobSpawnPoint.load(id, pointSection);
            if (point == null) {
                continue;
            }
            spawnPoints.put(id, point);
            spawnPointCooldownTicks.put(id, Math.max(0, point.spawnDelayTicks()));
        }
    }

    private void saveSpawnPoints() {
        YamlConfiguration config = new YamlConfiguration();
        for (EndMinesMobSpawnPoint point : spawnPoints.values()) {
            if (point == null) {
                continue;
            }
            ConfigurationSection section = config.createSection("spawn-points." + point.id());
            point.save(section);
        }
        try {
            config.save(spawnPointsFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save End Mines mob spawn points: " + e.getMessage());
        }
    }

    private boolean isEnabled() {
        return endMinesManager != null
                && endMinesManager.isEnabled()
                && plugin.getConfig().getBoolean("end-mines.mobs.enabled", true);
    }

    private boolean isInEndMines(World world) {
        World end = endMinesManager == null ? null : endMinesManager.getWorld();
        return end != null && world != null && world.equals(end);
    }

    private void spawnTick() {
        if (!isEnabled()) {
            return;
        }
        if (!endMinesManager.isGenerated()) {
            return;
        }

        World world = endMinesManager.getWorld();
        if (world == null) {
            return;
        }
        List<Player> players = world.getPlayers();
        if (players.isEmpty()) {
            return;
        }

        long intervalLong = Math.max(10L, plugin.getConfig().getLong("end-mines.mobs.spawn-interval-ticks", 40L));
        int spawnInterval = (int) Math.max(1L, Math.min(Integer.MAX_VALUE, intervalLong));
        String mode = plugin.getConfig().getString("end-mines.mobs.spawn-mode", "around_players");
        String normalized = mode == null ? "around_players" : mode.trim().toLowerCase(Locale.ROOT);

        switch (normalized) {
            case "spawn_points" -> spawnFromSpawnPoints(world, players, spawnInterval);
            case "mixed" -> {
                spawnFromSpawnPoints(world, players, spawnInterval);
                spawnAroundPlayers(world, players);
            }
            default -> spawnAroundPlayers(world, players);
        }
    }

    private void spawnFromSpawnPoints(World world, List<Player> players, int spawnInterval) {
        if (world == null || players == null || players.isEmpty()) {
            return;
        }
        if (spawnPoints.isEmpty()) {
            return;
        }

        int despawnDistance = Math.max(24, plugin.getConfig().getInt("end-mines.mobs.despawn-distance", 72));
        double activationSq = (double) despawnDistance * despawnDistance;

        int minY = plugin.getConfig().getInt("end-mines.generation.min-y", 60);
        int maxY = plugin.getConfig().getInt("end-mines.generation.max-y", 78);

        for (EndMinesMobSpawnPoint point : spawnPoints.values()) {
            if (point == null || !point.active()) {
                continue;
            }
            Location base = point.toLocation();
            if (base == null || base.getWorld() == null) {
                continue;
            }
            if (!isInEndMines(base.getWorld())) {
                continue;
            }
            if (!hasNearbyPlayer(base, players, activationSq)) {
                continue;
            }

            UUID id = point.id();
            int remaining = spawnPointCooldownTicks.getOrDefault(id, 0) - spawnInterval;
            if (remaining > 0) {
                spawnPointCooldownTicks.put(id, remaining);
                continue;
            }

            int nearby = countNearbyEndMinesMobsAt(base, Math.max(6, point.spawnRadius()));
            if (nearby >= point.maxNearbyEntities()) {
                spawnPointCooldownTicks.put(id, Math.max(spawnInterval, 1));
                continue;
            }

            EndMinesMobType type = EndMinesMobType.parse(point.mobTypeId());
            if (type == null) {
                type = EndMinesMobType.RIFTWALKER;
            }

            Location spawn = findSpawnNearPoint(base, point.spawnRadius(), minY, maxY);
            if (spawn != null) {
                spawnMob(world, spawn, type);
            }

            spawnPointCooldownTicks.put(id, point.spawnDelayTicks());
        }
    }

    private Location findSpawnNearPoint(Location base, int radius, int minY, int maxY) {
        if (base == null || base.getWorld() == null) {
            return null;
        }
        int attempts = Math.max(3, Math.min(20, plugin.getConfig().getInt("end-mines.mobs.spawn-attempts", 10)));
        World world = base.getWorld();
        int bx = base.getBlockX();
        int by = base.getBlockY();
        int bz = base.getBlockZ();

        for (int i = 0; i < attempts; i++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            Location resolved = resolveFloor(world, bx + dx, by, bz + dz, minY, maxY);
            if (resolved != null) {
                return resolved;
            }
        }
        return resolveFloor(world, bx, by, bz, minY, maxY);
    }

    private int countNearbyEndMinesMobsAt(Location location, int radius) {
        if (location == null || location.getWorld() == null) {
            return 0;
        }
        int count = 0;
        for (Entity nearby : location.getWorld().getNearbyEntities(location, radius, 12, radius)) {
            if (!(nearby instanceof LivingEntity living)) {
                continue;
            }
            if (living.getPersistentDataContainer().has(mobTypeKey, PersistentDataType.STRING)) {
                count++;
            }
        }
        return count;
    }

    private void spawnAroundPlayers(World world, List<Player> players) {
        int spawnRadius = Math.max(8, plugin.getConfig().getInt("end-mines.mobs.spawn-radius", 28));
        int maxNearPlayer = Math.max(0, plugin.getConfig().getInt("end-mines.mobs.max-near-player", 12));
        int safeRadiusFromSpawn = Math.max(0, plugin.getConfig().getInt("end-mines.mobs.safe-radius-from-spawn", 12));
        int spawnAttempts = Math.max(1, plugin.getConfig().getInt("end-mines.mobs.spawn-attempts", 10));

        Location mineSpawn = endMinesManager.getSpawnLocation(world);
        double safeRadiusSq = safeRadiusFromSpawn <= 0 ? 0.0D : safeRadiusFromSpawn * safeRadiusFromSpawn;

        int minY = plugin.getConfig().getInt("end-mines.generation.min-y", 60);
        int maxY = plugin.getConfig().getInt("end-mines.generation.max-y", 78);

        for (Player player : players) {
            if (player == null || player.isDead()) {
                continue;
            }
            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            if (player.hasPermission("grivience.admin") || player.hasPermission("grivience.endmines.build")) {
                continue;
            }

            int nearby = countNearbyEndMinesMobs(player, spawnRadius);
            if (nearby >= maxNearPlayer) {
                continue;
            }

            int toSpawn = Math.min(2, Math.max(0, maxNearPlayer - nearby));
            for (int i = 0; i < toSpawn; i++) {
                EndMinesMobType type = rollType();
                Location spawn = findSpawnLocation(world, player.getLocation(), spawnRadius, minY, maxY, mineSpawn, safeRadiusSq, spawnAttempts);
                if (spawn == null) {
                    break;
                }
                spawnMob(world, spawn, type);
            }
        }
    }

    private int countNearbyEndMinesMobs(Player player, int radius) {
        int count = 0;
        for (Entity nearby : player.getNearbyEntities(radius, 12, radius)) {
            if (!(nearby instanceof LivingEntity living)) {
                continue;
            }
            if (living.getPersistentDataContainer().has(mobTypeKey, PersistentDataType.STRING)) {
                count++;
            }
        }
        return count;
    }

    private EndMinesMobType rollType() {
        int total = 0;
        for (EndMinesMobType type : EndMinesMobType.values()) {
            total += Math.max(0, type.weight());
        }
        int roll = random.nextInt(Math.max(1, total));
        int cursor = 0;
        for (EndMinesMobType type : EndMinesMobType.values()) {
            cursor += Math.max(0, type.weight());
            if (roll < cursor) {
                return type;
            }
        }
        return EndMinesMobType.RIFTWALKER;
    }

    private Location findSpawnLocation(
            World world,
            Location around,
            int radius,
            int minY,
            int maxY,
            Location mineSpawn,
            double safeRadiusSq,
            int attempts
    ) {
        if (world == null || around == null) {
            return null;
        }

        int baseX = around.getBlockX();
        int baseY = around.getBlockY();
        int baseZ = around.getBlockZ();

        for (int i = 0; i < attempts; i++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            int dy = random.nextInt(13) - 6;

            int x = baseX + dx;
            int y = Math.max(minY + 2, Math.min(maxY - 2, baseY + dy));
            int z = baseZ + dz;

            Location candidate = new Location(world, x + 0.5D, y, z + 0.5D);
            if (safeRadiusSq > 0.0D && mineSpawn != null && mineSpawn.getWorld() != null && mineSpawn.getWorld().equals(world)) {
                if (mineSpawn.distanceSquared(candidate) <= safeRadiusSq) {
                    continue;
                }
            }

            Location resolved = resolveFloor(world, x, y, z, minY, maxY);
            if (resolved == null) {
                continue;
            }
            if (safeRadiusSq > 0.0D && mineSpawn != null && mineSpawn.getWorld() != null && mineSpawn.getWorld().equals(world)) {
                if (mineSpawn.distanceSquared(resolved) <= safeRadiusSq) {
                    continue;
                }
            }
            return resolved;
        }

        return null;
    }

    private Location resolveFloor(World world, int x, int startY, int z, int minY, int maxY) {
        int y = Math.max(minY + 2, Math.min(maxY - 2, startY));
        for (int scan = 0; scan < 12; scan++) {
            int yy = y - scan;
            if (yy <= minY + 1) {
                break;
            }
            Block floor = world.getBlockAt(x, yy - 1, z);
            Block feet = world.getBlockAt(x, yy, z);
            Block head = world.getBlockAt(x, yy + 1, z);

            if (!feet.getType().isAir() || !head.getType().isAir()) {
                continue;
            }
            if (!floor.getType().isSolid()) {
                continue;
            }
            if (floor.getType() == Material.BEDROCK) {
                continue;
            }
            if (!VALID_SPAWN_FLOORS.contains(floor.getType())) {
                continue;
            }
            return new Location(world, x + 0.5D, yy, z + 0.5D);
        }
        return null;
    }

    private void spawnMob(World world, Location location, EndMinesMobType type) {
        if (world == null || location == null || type == null) {
            return;
        }
        Entity raw = world.spawnEntity(location, type.entityType());
        if (!(raw instanceof LivingEntity living)) {
            raw.remove();
            return;
        }

        living.setRemoveWhenFarAway(true);
        living.setCanPickupItems(false);

        PersistentDataContainer pdc = living.getPersistentDataContainer();
        pdc.set(mobTypeKey, PersistentDataType.STRING, type.id());

        applyAttributes(living, type);
        equipMob(living, type);

        // Keep health nameplates consistent with the rest of the plugin.
        MobHealthDisplay.setBaseName(living, healthBaseNameKey, ChatColor.RED + type.displayName());

        mobs.put(living.getUniqueId(), new MobState(type, initialCooldown(type)));
    }

    private void equipMob(LivingEntity living, EndMinesMobType type) {
        if (living.getEquipment() == null) return;
        
        switch (type) {
            case ZOMBIE_MINER:
                living.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
                living.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_PICKAXE));
                break;
            case SKELETON_WATCHER:
                living.getEquipment().setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
                living.getEquipment().setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
                living.getEquipment().setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
                living.getEquipment().setBoots(new ItemStack(Material.CHAINMAIL_BOOTS));
                break;
        }
    }

    private void applyAttributes(LivingEntity living, EndMinesMobType type) {
        int y = living.getLocation().getBlockY();
        double scaleFactor = 1.0 + Math.max(0, (64 - y) / 64.0); // Deeper = stronger

        AttributeInstance maxHealth = living.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(Math.max(1.0D, type.maxHealth() * scaleFactor));
            try {
                living.setHealth(maxHealth.getValue());
            } catch (IllegalArgumentException ignored) {
                living.setHealth(Math.max(1.0D, Math.min(living.getHealth(), maxHealth.getValue())));
            }
        }

        AttributeInstance attack = living.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attack != null) {
            double damageMultiplier = Math.max(0.0D, plugin.getConfig().getDouble("end-mines.mobs.damage-multiplier", 1.0D));
            attack.setBaseValue(Math.max(0.0D, type.attackDamage() * scaleFactor * damageMultiplier));
        }

        AttributeInstance speed = living.getAttribute(Attribute.MOVEMENT_SPEED);
        double speedMod = 1.0;
        if (type == EndMinesMobType.CAVE_SPIDER_SWARM) speedMod = 1.5;
        if (speed != null && type.moveSpeed() > 0.0D) {
            speed.setBaseValue(type.moveSpeed() * speedMod);
        }
    }

    private int initialCooldown(EndMinesMobType type) {
        return switch (type) {
            case RIFTWALKER -> 40 + random.nextInt(60);
            case CRYSTAL_SENTRY -> 30 + random.nextInt(50);
            case OBSIDIAN_GOLEM -> 50 + random.nextInt(70);
            default -> 100 + random.nextInt(100);
        };
    }

    private void abilityTick() {
        if (!isEnabled()) {
            return;
        }

        World world = endMinesManager.getWorld();
        if (world == null) {
            return;
        }
        if (world.getPlayers().isEmpty()) {
            // Keep the world clean when no one is around.
            for (UUID uuid : new ArrayList<>(mobs.keySet())) {
                Entity raw = Bukkit.getEntity(uuid);
                if (raw != null && raw.getWorld() != null && isInEndMines(raw.getWorld())) {
                    raw.remove();
                }
            }
            mobs.clear();
            return;
        }

        int despawnDistance = Math.max(24, plugin.getConfig().getInt("end-mines.mobs.despawn-distance", 72));
        double despawnSq = despawnDistance * despawnDistance;

        Iterator<Map.Entry<UUID, MobState>> it = mobs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, MobState> entry = it.next();
            UUID uuid = entry.getKey();
            MobState state = entry.getValue();
            Entity raw = Bukkit.getEntity(uuid);

            if (!(raw instanceof LivingEntity living) || living.isDead()) {
                it.remove();
                continue;
            }
            if (!isInEndMines(living.getWorld())) {
                it.remove();
                continue;
            }

            if (!hasNearbyPlayer(living.getLocation(), world.getPlayers(), despawnSq)) {
                living.remove();
                it.remove();
                continue;
            }

            state.abilityCooldownTicks -= 10;
            if (state.abilityCooldownTicks > 0) {
                continue;
            }

            triggerAbility(living, state.type);
            state.abilityCooldownTicks = initialCooldown(state.type);
        }
    }

    private boolean hasNearbyPlayer(Location loc, List<Player> players, double maxDistSq) {
        if (loc == null || players == null || players.isEmpty()) {
            return false;
        }
        for (Player player : players) {
            if (player == null || player.isDead()) {
                continue;
            }
            if (player.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            if (player.getLocation().distanceSquared(loc) <= maxDistSq) {
                return true;
            }
        }
        return false;
    }

    private void triggerAbility(LivingEntity living, EndMinesMobType type) {
        switch (type) {
            case RIFTWALKER -> riftstep(living);
            case CRYSTAL_SENTRY -> crystalVolley(living);
            case OBSIDIAN_GOLEM -> obsidianSlam(living);
        }
    }

    private void riftstep(LivingEntity living) {
        if (!(living instanceof Mob mob)) {
            return;
        }
        LivingEntity target = mob.getTarget();
        Player playerTarget = target instanceof Player p ? p : nearestPlayer(living.getLocation(), 22.0D);
        if (playerTarget == null) {
            return;
        }

        Location dest = behindPlayer(playerTarget.getLocation(), 2.3D);
        Location safe = findSafeTeleport(living.getWorld(), dest);
        if (safe == null) {
            return;
        }

        living.getWorld().spawnParticle(Particle.REVERSE_PORTAL, living.getLocation().add(0, 1.0D, 0), 25, 0.4D, 0.6D, 0.4D, 0.02D);
        living.teleport(safe);
        living.getWorld().spawnParticle(Particle.REVERSE_PORTAL, safe.clone().add(0, 1.0D, 0), 25, 0.4D, 0.6D, 0.4D, 0.02D);

        playerTarget.playSound(playerTarget.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.9F, 0.7F);
        playerTarget.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.DARKNESS, 50, 0, false, true, true));
        playerTarget.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 60, 0, false, true, true));
    }

    private void crystalVolley(LivingEntity living) {
        Player target = nearestPlayer(living.getLocation(), 18.0D);
        if (target == null) {
            return;
        }

        World world = living.getWorld();
        Location origin = living.getLocation().add(0, 0.9D, 0);
        Entity spawned = world.spawnEntity(origin, org.bukkit.entity.EntityType.SHULKER_BULLET);
        if (!(spawned instanceof ShulkerBullet bullet)) {
            spawned.remove();
            return;
        }

        bullet.setTarget(target);
        if (bullet instanceof Projectile projectile) {
            projectile.setShooter(living);
        }
        bullet.getPersistentDataContainer().set(projectileKey, PersistentDataType.STRING, "crystal_sentry");

        world.playSound(origin, Sound.ENTITY_SHULKER_SHOOT, 0.8F, 1.2F);
    }

    private void obsidianSlam(LivingEntity living) {
        Location center = living.getLocation();
        List<Player> targets = nearbyPlayers(center, 5.0D);
        if (targets.isEmpty()) {
            return;
        }

        World world = living.getWorld();
        BlockData obsidian = Material.OBSIDIAN.createBlockData();
        world.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 0.8F, 0.9F);
        world.spawnParticle(Particle.BLOCK_CRUMBLE, center.clone().add(0, 0.2D, 0), 60, 1.2D, 0.3D, 1.2D, obsidian);

        for (Player player : targets) {
            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            Vector knock = player.getLocation().toVector().subtract(center.toVector());
            if (knock.lengthSquared() < 1.0E-6D) {
                knock = new Vector(1.0D, 0.0D, 0.0D);
            }
            knock.normalize().multiply(0.85D).setY(0.35D);
            player.setVelocity(knock);
            player.damage(6.0D, living);
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 50, 0, false, true, true));
        }
    }

    private Player nearestPlayer(Location from, double range) {
        if (from == null || from.getWorld() == null) {
            return null;
        }
        double bestSq = range * range;
        Player best = null;
        for (Player player : from.getWorld().getPlayers()) {
            if (player == null || player.isDead()) {
                continue;
            }
            if (player.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            double d = player.getLocation().distanceSquared(from);
            if (d <= bestSq) {
                bestSq = d;
                best = player;
            }
        }
        return best;
    }

    private List<Player> nearbyPlayers(Location from, double range) {
        if (from == null || from.getWorld() == null) {
            return List.of();
        }
        double rangeSq = range * range;
        List<Player> out = new ArrayList<>();
        for (Player player : from.getWorld().getPlayers()) {
            if (player == null || player.isDead()) {
                continue;
            }
            if (player.getLocation().distanceSquared(from) <= rangeSq) {
                out.add(player);
            }
        }
        return out;
    }

    private Location behindPlayer(Location location, double distance) {
        Location base = location.clone();
        Vector dir = base.getDirection().setY(0.0D);
        if (dir.lengthSquared() < 1.0E-6D) {
            dir = new Vector(1.0D, 0.0D, 0.0D);
        }
        dir.normalize().multiply(-distance);
        return base.add(dir);
    }

    private Location findSafeTeleport(World world, Location desired) {
        if (world == null || desired == null) {
            return null;
        }

        int x = desired.getBlockX();
        int z = desired.getBlockZ();
        int y = desired.getBlockY();

        int[][] offsets = new int[][]{
                {0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}
        };

        for (int[] off : offsets) {
            int xx = x + off[0];
            int zz = z + off[1];
            for (int dy = -1; dy <= 2; dy++) {
                int yy = y + dy;
                Block below = world.getBlockAt(xx, yy - 1, zz);
                Block feet = world.getBlockAt(xx, yy, zz);
                Block head = world.getBlockAt(xx, yy + 1, zz);
                if (!feet.getType().isAir() || !head.getType().isAir()) {
                    continue;
                }
                if (!below.getType().isSolid() || below.getType() == Material.BEDROCK) {
                    continue;
                }
                return new Location(world, xx + 0.5D, yy, zz + 0.5D, desired.getYaw(), desired.getPitch());
            }
        }

        return null;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamageByMob(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        
        Entity rawDamager = event.getDamager();
        if (rawDamager instanceof Projectile proj && proj.getShooter() instanceof LivingEntity shooter) {
            rawDamager = shooter;
        }

        if (!(rawDamager instanceof LivingEntity damager)) {
            return;
        }

        if (!isInEndMines(damager.getWorld())) {
            return;
        }

        EndMinesMobType type = getMobType(damager);
        if (type == null) {
            return;
        }

        // Ironcrest Guard Bonus: Reduced damage from mine mobs
        if (hasIroncrestGuardFullSet(player)) {
            event.setDamage(event.getDamage() * 0.70); // 30% reduction
        }

        if (type == EndMinesMobType.RIFTWALKER) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.WEAKNESS, 60, 0, false, true, true));
        }
        if (type == EndMinesMobType.OBSIDIAN_GOLEM) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 40, 0, false, true, true));
        }
    }

    private boolean hasIroncrestGuardFullSet(Player player) {
        if (customItemService == null || player == null) return false;
        int pieces = 0;
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item == null) continue;
            String id = customItemService.itemId(item);
            if (id != null && id.startsWith("IRONCREST_")) pieces++;
        }
        return pieces >= 4;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMobDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null || !isInEndMines(entity.getWorld())) {
            return;
        }

        EndMinesMobType type = getMobType(entity);
        if (type == null) {
            return;
        }

        mobs.remove(entity.getUniqueId());
        event.getDrops().clear();
        event.setDroppedExp(0);

        Player killer = entity.getKiller();
        if (killer == null || customItemService == null) {
            return;
        }

        // Enderman Excavator bonus XP
        if (type == EndMinesMobType.ENDERMAN_EXCAVATOR) {
            killer.sendMessage(ChatColor.YELLOW + "+Bonus Mining XP from Enderman Excavator!");
        }

        DropSpec[] specs = DROPS.getOrDefault(type, new DropSpec[0]);
        Location dropLoc = entity.getLocation().add(0, 0.5D, 0);
        for (DropSpec spec : specs) {
            if (random.nextDouble() > spec.chance()) {
                continue;
            }
            ItemStack item = customItemService.createEndMinesMaterial(spec.type());
            if (item == null) {
                continue;
            }
            int amount = roll(spec.min(), spec.max());
            item.setAmount(amount);
            entity.getWorld().dropItemNaturally(dropLoc, item);
            trackCollection(killer, item);

            if (spec.rare()) {
                killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7F, 1.5F);
                killer.sendMessage(ChatColor.LIGHT_PURPLE + "RARE DROP! " + ChatColor.AQUA + itemName(item));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Projectile projectile)) {
            return;
        }
        if (!(projectile instanceof ShulkerBullet)) {
            return;
        }
        if (!(event.getHitEntity() instanceof Player player)) {
            return;
        }
        if (!projectile.getPersistentDataContainer().has(projectileKey, PersistentDataType.STRING)) {
            return;
        }
        if (!isInEndMines(player.getWorld())) {
            return;
        }

        // Extra "crystal" sting on hit: keep it short to avoid being oppressive.
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 30, 0, false, true, true));
    }

    private EndMinesMobType getMobType(LivingEntity entity) {
        if (entity == null) {
            return null;
        }
        String id = entity.getPersistentDataContainer().get(mobTypeKey, PersistentDataType.STRING);
        return EndMinesMobType.parse(id);
    }

    private int roll(int min, int max) {
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
            id = item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "custom-item-id"), PersistentDataType.STRING);
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
}
