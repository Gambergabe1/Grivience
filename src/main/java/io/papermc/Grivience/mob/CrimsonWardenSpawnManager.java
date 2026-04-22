package io.papermc.Grivience.mob;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class CrimsonWardenSpawnManager implements Listener {
    private static final String BOSS_ID = "crimson_warden";
    private static final byte MANAGED_MARKER = (byte) 1;
    private static final long DEFAULT_RESPAWN_INTERVAL_MS = 90L * 60L * 1000L;
    private static final long RECOVERY_GRACE_MS = 15_000L;
    private static final long RECOVERY_SCAN_INTERVAL_MS = 5_000L;
    private static final double DEFAULT_LEASH_RADIUS = 32.0D;
    private static final double MIN_LEASH_RADIUS = 4.0D;

    private final GriviencePlugin plugin;
    private final CustomMonsterManager customMonsterManager;
    private final NamespacedKey customMonsterKey;
    private final NamespacedKey managedBossKey;
    private final File dataFile;

    private YamlConfiguration dataConfig;
    private Location spawnLocation;
    private boolean enabled;
    private double leashRadius;
    private long respawnIntervalMs;
    private long nextRespawnAtMs;
    private UUID activeBossId;
    private long recoveryUntilMs;
    private long lastRecoveryScanAtMs;
    private BukkitTask tickTask;

    public CrimsonWardenSpawnManager(GriviencePlugin plugin, CustomMonsterManager customMonsterManager) {
        this.plugin = plugin;
        this.customMonsterManager = customMonsterManager;
        this.customMonsterKey = new NamespacedKey(plugin, "custom_monster");
        this.managedBossKey = new NamespacedKey(plugin, "crimson_warden_managed");
        this.dataFile = new File(plugin.getDataFolder(), "crimson-warden-spawn.yml");
        load();
        startTicker();
    }

    public synchronized void reload() {
        load();
    }

    public synchronized void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        save();
    }

    public synchronized boolean isEnabled() {
        return enabled;
    }

    public synchronized void setEnabled(boolean enabled) {
        this.enabled = enabled;
        save();
    }

    public synchronized boolean isSpawnConfigured() {
        return spawnLocation != null && spawnLocation.getWorld() != null;
    }

    public synchronized Location getSpawnLocation() {
        return spawnLocation == null ? null : spawnLocation.clone();
    }

    public synchronized void setSpawnLocation(Location location) {
        this.spawnLocation = sanitizeLocation(location);
        this.enabled = true;
        this.recoveryUntilMs = 0L;
        if (nextRespawnAtMs < 0L) {
            nextRespawnAtMs = 0L;
        }
        save();
    }

    public synchronized long getRespawnIntervalMs() {
        return respawnIntervalMs;
    }

    public synchronized long getNextRespawnAtMs() {
        return nextRespawnAtMs;
    }

    public synchronized long getRemainingRespawnMs() {
        if (resolveCurrentBossNow() != null) {
            return 0L;
        }
        return Math.max(0L, nextRespawnAtMs - System.currentTimeMillis());
    }

    public synchronized LivingEntity getActiveBoss() {
        return resolveCurrentBossNow();
    }

    public synchronized double getLeashRadius() {
        return leashRadius;
    }

    public synchronized void setLeashRadius(double leashRadius) {
        this.leashRadius = sanitizeLeashRadius(leashRadius);
        save();
    }

    public synchronized LivingEntity spawnNow(boolean replaceExisting) {
        if (!isSpawnConfigured()) {
            return null;
        }

        LivingEntity existing = resolveCurrentBossNow();
        if (existing != null) {
            if (!replaceExisting) {
                return null;
            }
            activeBossId = null;
            existing.remove();
        }

        nextRespawnAtMs = 0L;
        LivingEntity spawned = spawnManagedBoss();
        save();
        return spawned;
    }

    public synchronized boolean despawnActiveBoss(boolean startRespawnCooldown) {
        LivingEntity existing = resolveCurrentBossNow();
        if (existing == null) {
            return false;
        }

        activeBossId = null;
        if (startRespawnCooldown) {
            nextRespawnAtMs = System.currentTimeMillis() + respawnIntervalMs;
        } else {
            nextRespawnAtMs = 0L;
        }
        save();
        existing.remove();
        return true;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBossDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!isManagedBoss(entity)) {
            return;
        }

        activeBossId = null;
        nextRespawnAtMs = System.currentTimeMillis() + respawnIntervalMs;
        save();
    }

    private synchronized void startTicker() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private synchronized void tick() {
        LivingEntity activeBoss = resolveActiveBoss(true);
        if (activeBoss != null) {
            enforceLeash(activeBoss);
            return;
        }
        if (findAnyBoss() != null) {
            return;
        }
        if (!enabled || !isSpawnConfigured()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (activeBossId != null && now < recoveryUntilMs) {
            return;
        }
        if (nextRespawnAtMs > now) {
            return;
        }

        spawnManagedBoss();
    }

    private LivingEntity spawnManagedBoss() {
        if (!isSpawnConfigured()) {
            return null;
        }
        if (customMonsterManager == null) {
            return null;
        }

        LivingEntity existingBoss = findAnyBoss();
        if (existingBoss != null) {
            if (isManagedBoss(existingBoss)) {
                activeBossId = existingBoss.getUniqueId();
                recoveryUntilMs = 0L;
            }
            return null;
        }

        Location location = spawnLocation.clone();
        World world = location.getWorld();
        if (world == null) {
            return null;
        }

        Chunk chunk = location.getChunk();
        if (!chunk.isLoaded()) {
            chunk.load();
        }

        LivingEntity boss = customMonsterManager.spawnMonster(BOSS_ID, location);
        if (boss == null) {
            return null;
        }

        boss.getPersistentDataContainer().set(managedBossKey, PersistentDataType.BYTE, MANAGED_MARKER);
        boss.setPersistent(true);
        boss.setRemoveWhenFarAway(false);
        boss.setCanPickupItems(false);

        activeBossId = boss.getUniqueId();
        recoveryUntilMs = 0L;
        lastRecoveryScanAtMs = 0L;
        nextRespawnAtMs = 0L;
        save();

        world.spawnParticle(Particle.DUST, boss.getLocation().add(0.0D, 0.2D, 0.0D), 28, 0.9D, 0.3D, 0.9D, 0.0D,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(0xB62424), 1.5F));
        world.playSound(boss.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.55F, 1.45F);
        return boss;
    }

    private void enforceLeash(LivingEntity boss) {
        if (boss == null || spawnLocation == null || spawnLocation.getWorld() == null) {
            return;
        }

        Location anchor = spawnLocation.clone();
        World anchorWorld = anchor.getWorld();
        Location current = boss.getLocation();
        World currentWorld = current.getWorld();
        boolean wrongWorld = currentWorld == null || !anchorWorld.getUID().equals(currentWorld.getUID());
        boolean outsideRadius = !wrongWorld && current.distanceSquared(anchor) > (leashRadius * leashRadius);
        if (!wrongWorld && !outsideRadius) {
            return;
        }

        Chunk chunk = anchor.getChunk();
        if (!chunk.isLoaded()) {
            chunk.load();
        }

        if (currentWorld != null) {
            currentWorld.spawnParticle(Particle.SMOKE, current.clone().add(0.0D, 1.0D, 0.0D), 16, 0.35D, 0.45D, 0.35D, 0.01D);
        }

        boss.teleport(anchor);
        boss.setVelocity(new org.bukkit.util.Vector(0.0D, 0.0D, 0.0D));
        if (boss instanceof org.bukkit.entity.Mob mob) {
            mob.setTarget(null);
        }

        anchorWorld.spawnParticle(
                Particle.DUST,
                anchor.clone().add(0.0D, 0.2D, 0.0D),
                18,
                0.45D,
                0.25D,
                0.45D,
                0.0D,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(0xB62424), 1.2F)
        );
        anchorWorld.playSound(anchor, Sound.ENTITY_ENDERMAN_TELEPORT, 0.55F, 0.75F);
    }

    private LivingEntity resolveActiveBoss(boolean allowRecoveryScan) {
        if (activeBossId != null) {
            Entity entity = Bukkit.getEntity(activeBossId);
            if (entity instanceof LivingEntity living && isManagedBoss(living) && living.isValid() && !living.isDead()) {
                return living;
            }
        }

        if (!allowRecoveryScan) {
            return null;
        }

        long now = System.currentTimeMillis();
        if (activeBossId != null && now < recoveryUntilMs) {
            return null;
        }
        if ((now - lastRecoveryScanAtMs) < RECOVERY_SCAN_INTERVAL_MS) {
            return null;
        }

        lastRecoveryScanAtMs = now;
        LivingEntity recovered = findManagedBoss();
        if (recovered != null) {
            activeBossId = recovered.getUniqueId();
            recoveryUntilMs = 0L;
            return recovered;
        }

        return null;
    }

    private LivingEntity resolveActiveBossNow() {
        if (activeBossId != null) {
            Entity entity = Bukkit.getEntity(activeBossId);
            if (entity instanceof LivingEntity living && isManagedBoss(living) && living.isValid() && !living.isDead()) {
                return living;
            }
        }

        LivingEntity recovered = findManagedBoss();
        if (recovered != null) {
            activeBossId = recovered.getUniqueId();
            recoveryUntilMs = 0L;
        }
        return recovered;
    }

    private LivingEntity resolveCurrentBossNow() {
        LivingEntity managed = resolveActiveBossNow();
        if (managed != null) {
            return managed;
        }
        return findAnyBoss();
    }

    private LivingEntity findManagedBoss() {
        return findBoss(true);
    }

    private LivingEntity findAnyBoss() {
        return findBoss(false);
    }

    private LivingEntity findBoss(boolean managedOnly) {
        if (spawnLocation != null && spawnLocation.getWorld() != null) {
            Chunk chunk = spawnLocation.getChunk();
            if (!chunk.isLoaded()) {
                chunk.load();
            }
        }

        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity living : world.getLivingEntities()) {
                if (!living.isValid() || living.isDead()) {
                    continue;
                }
                if (!isCrimsonWarden(living)) {
                    continue;
                }
                if (managedOnly && !isManagedBoss(living)) {
                    continue;
                }
                if (isManagedBoss(living)) {
                    activeBossId = living.getUniqueId();
                    recoveryUntilMs = 0L;
                }
                if (!managedOnly || isManagedBoss(living)) {
                    return living;
                }
            }
        }
        return null;
    }

    private boolean isCrimsonWarden(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        String monsterId = entity.getPersistentDataContainer().get(customMonsterKey, PersistentDataType.STRING);
        return BOSS_ID.equalsIgnoreCase(monsterId);
    }

    private boolean isManagedBoss(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        Byte managedMarker = entity.getPersistentDataContainer().get(managedBossKey, PersistentDataType.BYTE);
        return isCrimsonWarden(entity) && managedMarker != null && managedMarker == MANAGED_MARKER;
    }

    private void load() {
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        spawnLocation = readLocation(dataConfig, "spawn");
        enabled = dataConfig.getBoolean("enabled", true);
        leashRadius = sanitizeLeashRadius(dataConfig.getDouble("leash-radius", DEFAULT_LEASH_RADIUS));
        respawnIntervalMs = Math.max(60_000L, dataConfig.getLong("respawn-interval-ms", DEFAULT_RESPAWN_INTERVAL_MS));
        nextRespawnAtMs = Math.max(0L, dataConfig.getLong("next-respawn-at-ms", 0L));
        activeBossId = parseUuid(dataConfig.getString("active-boss-uuid"));
        recoveryUntilMs = activeBossId == null ? 0L : System.currentTimeMillis() + RECOVERY_GRACE_MS;
        lastRecoveryScanAtMs = 0L;
    }

    private void save() {
        if (dataConfig == null) {
            dataConfig = new YamlConfiguration();
        }

        dataConfig.set("enabled", enabled);
        dataConfig.set("leash-radius", leashRadius);
        dataConfig.set("respawn-interval-ms", respawnIntervalMs);
        dataConfig.set("next-respawn-at-ms", nextRespawnAtMs);
        dataConfig.set("active-boss-uuid", activeBossId == null ? null : activeBossId.toString());
        writeLocation(dataConfig, "spawn", spawnLocation);

        try {
            dataConfig.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save Crimson Warden spawn data: " + exception.getMessage());
        }
    }

    private Location readLocation(YamlConfiguration config, String path) {
        if (config == null || !config.isConfigurationSection(path)) {
            return null;
        }

        String worldName = config.getString(path + ".world");
        World world = worldName == null ? null : Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }

        return new Location(
                world,
                config.getDouble(path + ".x"),
                config.getDouble(path + ".y"),
                config.getDouble(path + ".z"),
                (float) config.getDouble(path + ".yaw", 0.0D),
                (float) config.getDouble(path + ".pitch", 0.0D)
        );
    }

    private void writeLocation(YamlConfiguration config, String path, Location location) {
        if (config == null) {
            return;
        }

        config.set(path, null);
        if (location == null || location.getWorld() == null) {
            return;
        }

        config.set(path + ".world", location.getWorld().getName());
        config.set(path + ".x", location.getX());
        config.set(path + ".y", location.getY());
        config.set(path + ".z", location.getZ());
        config.set(path + ".yaw", location.getYaw());
        config.set(path + ".pitch", location.getPitch());
    }

    private Location sanitizeLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        return new Location(
                location.getWorld(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
    }

    private double sanitizeLeashRadius(double radius) {
        if (!Double.isFinite(radius)) {
            return DEFAULT_LEASH_RADIUS;
        }
        return Math.max(MIN_LEASH_RADIUS, radius);
    }

    private UUID parseUuid(String rawUuid) {
        if (rawUuid == null || rawUuid.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(rawUuid.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
