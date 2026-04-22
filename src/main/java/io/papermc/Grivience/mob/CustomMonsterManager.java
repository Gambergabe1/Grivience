package io.papermc.Grivience.mob;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.item.CustomArmorManager;
import io.papermc.Grivience.item.CustomWeaponType;
import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.util.ArmorDurabilityUtil;
import io.papermc.Grivience.util.StackSizeSanitizer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import io.papermc.Grivience.util.MobHealthDisplay;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class CustomMonsterManager {
    public static final String VANILLA_MONSTER_PREFIX = "vanilla:";
    static final String SHADOW_STALKER_ID = "shadow_stalker";
    static final String SUMMONING_EYE_ITEM_KEY = "SUMMONING_EYE";
    static final double SHADOW_STALKER_SUMMONING_EYE_DROP_CHANCE = 0.05D;

    private final GriviencePlugin plugin;
    private final Map<String, CustomMonster> monsters = new HashMap<>();
    private final Map<UUID, SpawnPoint> spawnPoints = new ConcurrentHashMap<>();
    private final Map<String, List<SpawnPoint>> spawnPointsByWorld = new ConcurrentHashMap<>();
    private final Map<UUID, Long> nextSpawnAtMillis = new ConcurrentHashMap<>();
    private final NamespacedKey healthBaseNameKey;
    private final NamespacedKey spawnPointKey;
    private final Random random = new Random();
    private CustomItemService customItemService;
    private boolean spawningEnabled;
    private BukkitTask spawnerTask;

    public record SpawnBatchResult(int requested, int spawned) {
        public int blocked() {
            return Math.max(0, requested - spawned);
        }
    }

    public CustomMonsterManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.healthBaseNameKey = new NamespacedKey(plugin, "health_base_name");
        this.spawnPointKey = new NamespacedKey(plugin, "spawn_point");
        loadMonsters();
        loadSpawnPoints();
        restartSpawning();
    }

    public void setCustomItemService(CustomItemService customItemService) {
        this.customItemService = customItemService;
    }

    public void loadMonsters() {
        monsters.clear();
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection monstersSection = config.getConfigurationSection("custom-monsters.monsters");

        if (monstersSection == null) {
            return;
        }

        for (String monsterId : monstersSection.getKeys(false)) {
            CustomMonster monster = loadMonster(monsterId);
            if (monster != null) {
                monsters.put(monsterId, monster);
            }
        }
    }

    private CustomMonster loadMonster(String monsterId) {
        String path = "custom-monsters.monsters." + monsterId;
        if (!plugin.getConfig().contains(path)) {
            return null;
        }

        CustomMonster monster = new CustomMonster(monsterId);
        monster.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString(path + ".display-name", monsterId)));

        String typeName = plugin.getConfig().getString(path + ".entity-type", "ZOMBIE");
        monster.setEntityType(resolveConfiguredEntityType(monsterId, typeName));

        monster.setHealth(plugin.getConfig().getDouble(path + ".health", 20.0));
        double baseDamage = plugin.getConfig().getDouble(path + ".damage", 3.0);
        double damageMultiplier = Math.max(0.0D, plugin.getConfig().getDouble("custom-monsters.damage-multiplier", 1.0D));
        monster.setDamage(Math.max(0.0D, baseDamage * damageMultiplier));
        monster.setSpeed(plugin.getConfig().getDouble(path + ".speed", 0.23));
        monster.setLevel(plugin.getConfig().getInt(path + ".level", 1));
        monster.setExpReward(plugin.getConfig().getInt(path + ".exp-reward", 10));
        monster.setGlowing(plugin.getConfig().getBoolean(path + ".glowing", false));

        // Load drops
        monster.getDrops().clear();
        String dropsBase = path + ".drops";
        List<String> dropIds = plugin.getConfig().getStringList(dropsBase);
        if (dropIds == null || dropIds.isEmpty()) {
            ConfigurationSection dropSection = plugin.getConfig().getConfigurationSection(dropsBase);
            if (dropSection != null) {
                dropIds = new ArrayList<>(dropSection.getKeys(false));
            }
        }
        for (String dropId : dropIds) {
            String dropPath = dropsBase + "." + dropId;
            if (plugin.getConfig().contains(dropPath)) {
                MonsterDrop drop = new MonsterDrop();
                drop.setMaterial(plugin.getConfig().getString(dropPath + ".material", "ROTTEN_FLESH"));
                drop.setChance(plugin.getConfig().getDouble(dropPath + ".chance", 1.0));
                drop.setMinAmount(plugin.getConfig().getInt(dropPath + ".min-amount", 1));
                drop.setMaxAmount(plugin.getConfig().getInt(dropPath + ".max-amount", 1));
                if (plugin.getConfig().contains(dropPath + ".custom-item")) {
                    drop.setCustomItemId(plugin.getConfig().getString(dropPath + ".custom-item"));
                }
                monster.getDrops().add(drop);
            }
        }
        ensureDefaultDrops(monster);

        return monster;
    }

    static void ensureDefaultDrops(CustomMonster monster) {
        if (monster == null || !SHADOW_STALKER_ID.equalsIgnoreCase(monster.getId())) {
            return;
        }
        MonsterDrop summoningEyeDrop = monster.getDrops().stream()
                .filter(drop -> drop.isCustomItem() && SUMMONING_EYE_ITEM_KEY.equalsIgnoreCase(drop.getCustomItemId()))
                .findFirst()
                .orElse(null);
        if (summoningEyeDrop != null) {
            summoningEyeDrop.setChance(SHADOW_STALKER_SUMMONING_EYE_DROP_CHANCE);
            summoningEyeDrop.setMinAmount(1);
            summoningEyeDrop.setMaxAmount(1);
            return;
        }
        monster.getDrops().add(new MonsterDrop(true, SUMMONING_EYE_ITEM_KEY, SHADOW_STALKER_SUMMONING_EYE_DROP_CHANCE, 1, 1));
    }

    public void loadSpawnPoints() {
        spawnPoints.clear();
        spawnPointsByWorld.clear();
        nextSpawnAtMillis.clear();

        File spawnFile = new File(plugin.getDataFolder(), "mob-spawns.yml");
        if (!spawnFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(spawnFile);
        ConfigurationSection pointsSection = config.getConfigurationSection("spawn-points");

        if (pointsSection == null) {
            return;
        }

        for (String pointId : pointsSection.getKeys(false)) {
            ConfigurationSection pointSection = pointsSection.getConfigurationSection(pointId);
            if (pointSection != null) {
                try {
                    SpawnPoint point = new SpawnPoint(pointSection.getValues(false));
                    spawnPoints.put(point.getId(), point);
                    nextSpawnAtMillis.put(point.getId(), 0L);

                    String worldName = point.getWorldName();
                    if (worldName != null && !worldName.isBlank()) {
                        spawnPointsByWorld.computeIfAbsent(worldName, k -> new ArrayList<>()).add(point);
                    } else {
                        plugin.getLogger().warning("Skipping custom monster spawn point without a world: " + point.getId());
                    }
                } catch (Exception exception) {
                    plugin.getLogger().warning("Failed to load custom monster spawn point '" + pointId + "': " + exception.getMessage());
                }
            }
        }

        plugin.getLogger().info("Loaded " + spawnPoints.size() + " monster spawn points.");
    }

    public void saveSpawnPoints() {
        File spawnFile = new File(plugin.getDataFolder(), "mob-spawns.yml");
        YamlConfiguration config = new YamlConfiguration();

        for (SpawnPoint point : spawnPoints.values()) {
            String path = "spawn-points." + point.getId();
            config.set(path, point.serialize());
        }

        try {
            config.save(spawnFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save spawn points: " + e.getMessage());
        }
    }

    public SpawnPoint createSpawnPoint(Location location, String monsterId) {
        SpawnPoint point = new SpawnPoint(location, monsterId);
        spawnPoints.put(point.getId(), point);
        if (point.getWorldName() != null && !point.getWorldName().isBlank()) {
            spawnPointsByWorld.computeIfAbsent(point.getWorldName(), k -> new ArrayList<>()).add(point);
        }
        nextSpawnAtMillis.put(point.getId(), 0L);
        saveSpawnPoints();
        return point;
    }

    public boolean removeSpawnPoint(UUID pointId) {
        SpawnPoint point = spawnPoints.remove(pointId);
        nextSpawnAtMillis.remove(pointId);
        if (point != null) {
            String worldName = point.getWorldName();
            if (worldName != null) {
                List<SpawnPoint> worldPoints = spawnPointsByWorld.get(worldName);
                if (worldPoints != null) {
                    worldPoints.remove(point);
                }
            }
            saveSpawnPoints();
            return true;
        }
        return false;
    }

    public SpawnPoint getSpawnPoint(UUID pointId) {
        return spawnPoints.get(pointId);
    }

    public Collection<SpawnPoint> getSpawnPoints() {
        return spawnPoints.values();
    }

    public Collection<SpawnPoint> getSpawnPointsInWorld(String worldName) {
        return spawnPointsByWorld.getOrDefault(worldName, new ArrayList<>());
    }

    public CustomMonster getMonster(String id) {
        return monsters.get(id);
    }

    public Map<String, CustomMonster> getMonsters() {
        return new HashMap<>(monsters);
    }

    public String normalizeMonsterId(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return null;
        }

        CustomMonster custom = resolveCustomMonster(rawId);
        if (custom != null) {
            return custom.getId();
        }

        EntityType vanillaType = parseVanillaEntityType(rawId);
        if (vanillaType != null) {
            return VANILLA_MONSTER_PREFIX + vanillaType.name();
        }

        return null;
    }

    public String describeMonster(String monsterId) {
        CustomMonster custom = resolveCustomMonster(monsterId);
        if (custom != null) {
            return custom.getDisplayName();
        }

        EntityType vanillaType = parseVanillaEntityType(monsterId);
        if (vanillaType != null) {
            return "Vanilla " + humanizeEntityType(vanillaType);
        }

        return monsterId;
    }

    public List<String> vanillaMobSuggestions() {
        List<String> suggestions = new ArrayList<>();
        for (EntityType type : EntityType.values()) {
            if (!type.isAlive() || !type.isSpawnable()) {
                continue;
            }
            suggestions.add(type.name().toLowerCase(Locale.ROOT));
        }
        suggestions.sort(String::compareToIgnoreCase);
        return suggestions;
    }

    public EntityType parseVanillaEntityType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return null;
        }

        String normalized = rawType.trim();
        if (normalized.regionMatches(true, 0, VANILLA_MONSTER_PREFIX, 0, VANILLA_MONSTER_PREFIX.length())) {
            normalized = normalized.substring(VANILLA_MONSTER_PREFIX.length());
        }

        normalized = normalized
                .trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        EntityType type;
        try {
            type = EntityType.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }

        if (!type.isAlive() || !type.isSpawnable()) {
            return null;
        }
        return type;
    }

    public int getMaxManualSpawnAmount() {
        return Math.max(1, plugin.getConfig().getInt("custom-monsters.manual-spawn.max-amount", 25));
    }

    private void restartSpawning() {
        stopSpawning();
        spawningEnabled = plugin.getConfig().getBoolean("custom-monsters.enabled", true);

        if (!spawningEnabled) {
            return;
        }

        spawnerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            int maxSpawnsPerCycle = effectiveSpawnerBudget(
                    Math.max(1, plugin.getConfig().getInt("custom-monsters.spawner.max-spawns-per-cycle", 2))
            );
            if (plugin.getServerPerformanceMonitor() != null
                    && !plugin.getServerPerformanceMonitor().shouldProcess("custom-monsters-spawner-cycle", 1, 2)) {
                return;
            }
            for (Map.Entry<String, List<SpawnPoint>> worldEntry : spawnPointsByWorld.entrySet()) {
                World world = Bukkit.getWorld(worldEntry.getKey());
                if (world == null || !hasActivePlayers(world)) {
                    continue;
                }

                Map<String, Integer> activeCounts = null;
                for (SpawnPoint point : worldEntry.getValue()) {
                    if (!point.isActive() || !point.isLocationValid()) {
                        continue;
                    }

                    Location location = point.getLocation();
                    if (location == null || !world.equals(location.getWorld()) || !isChunkLoaded(location)) {
                        continue;
                    }

                    if (activeCounts == null) {
                        activeCounts = countActiveSpawnedEntitiesByPoint(world);
                    }

                    int activeCount = activeCounts.getOrDefault(point.getId().toString(), 0);
                    if (!point.shouldRefill(activeCount)) {
                        continue;
                    }

                    long nextSpawnAt = nextSpawnAtMillis.getOrDefault(point.getId(), 0L);
                    if (now < nextSpawnAt) {
                        continue;
                    }

                    int missing = Math.min(maxSpawnsPerCycle, point.getMaxNearbyEntities() - activeCount);
                    if (missing <= 0) {
                        continue;
                    }

                    nextSpawnAtMillis.put(point.getId(), now + Math.max(1L, point.getSpawnDelay()) * 50L);
                    int spawned = refillSpawnPoint(point, missing);
                    if (spawned > 0) {
                        activeCounts.put(point.getId().toString(), activeCount + spawned);
                    }
                }
            }
        }, 100L, 20L);
    }

    private void stopSpawning() {
        if (spawnerTask != null) {
            spawnerTask.cancel();
            spawnerTask = null;
        }
    }

    private int refillSpawnPoint(SpawnPoint point, int missing) {
        int spawned = 0;
        for (int i = 0; i < missing; i++) {
            if (spawnMonster(point)) {
                spawned++;
            }
        }
        return spawned;
    }

    private Map<String, Integer> countActiveSpawnedEntitiesByPoint(World world) {
        Map<String, Integer> counts = new HashMap<>();
        for (LivingEntity entity : world.getLivingEntities()) {
            if (entity == null || entity.isDead() || !entity.isValid()) {
                continue;
            }

            String spawnPointId = entity.getPersistentDataContainer().get(spawnPointKey, PersistentDataType.STRING);
            if (spawnPointId == null || spawnPointId.isBlank()) {
                continue;
            }

            counts.merge(spawnPointId, 1, Integer::sum);
        }
        return counts;
    }

    private boolean spawnMonster(SpawnPoint point) {
        Location spawnLoc = point.getLocation();
        if (spawnLoc == null || spawnLoc.getWorld() == null || !isChunkLoaded(spawnLoc)) {
            return false;
        }

        // Find a suitable spawn location within radius
        Location actualSpawn = findSpawnLocation(spawnLoc, point.getSpawnRadius(), resolveEntityType(point.getMonsterId()));
        if (actualSpawn == null) {
            return false;
        }

        LivingEntity entity = spawnMonster(point.getMonsterId(), actualSpawn);
        if (entity != null) {
            entity.getPersistentDataContainer().set(spawnPointKey, PersistentDataType.STRING, point.getId().toString());
            return true;
        }
        return false;
    }

    public LivingEntity spawnMonster(String monsterId, Location location) {
        CustomMonster monster = resolveCustomMonster(monsterId);
        if (monster != null) {
            Location safeLocation = normalizeSpawnLocation(location, monster.getEntityType());
            if (safeLocation == null) {
                return null;
            }
            return spawnCustomMonster(monster, safeLocation);
        }

        EntityType vanillaType = parseVanillaEntityType(monsterId);
        if (vanillaType != null) {
            Location safeLocation = normalizeSpawnLocation(location, vanillaType);
            if (safeLocation == null) {
                return null;
            }
            return spawnVanillaMonster(vanillaType, safeLocation);
        }
        return null;
    }

    public SpawnBatchResult spawnMonstersSafely(String monsterId, Location origin, int amount) {
        int requested = Math.max(0, amount);
        if (requested == 0 || origin == null || origin.getWorld() == null) {
            return new SpawnBatchResult(requested, 0);
        }

        int radius = Math.max(1, plugin.getConfig().getInt("custom-monsters.manual-spawn.radius", 6));
        int maxNearbyLiving = Math.max(1, plugin.getConfig().getInt("custom-monsters.manual-spawn.max-nearby-living", 24));
        int densityRadius = Math.max(radius, 8);
        int availableCapacity = Math.max(0, maxNearbyLiving - countNearbyNonPlayerLivingEntities(origin, densityRadius, 12));
        int target = Math.min(requested, availableCapacity);
        target = effectiveManualSpawnBudget(target);
        if (target <= 0) {
            return new SpawnBatchResult(requested, 0);
        }

        int spawned = 0;
        int attempts = Math.max(target * 6, 12);
        EntityType entityType = resolveEntityType(monsterId);
        for (int i = 0; i < attempts && spawned < target; i++) {
            Location candidate = findSpawnLocation(origin, radius, entityType);
            if (candidate == null) {
                continue;
            }
            if (countNearbyNonPlayerLivingEntities(candidate, 3, 4) >= 4) {
                continue;
            }
            LivingEntity entity = spawnMonster(monsterId, candidate);
            if (entity != null) {
                spawned++;
            }
        }
        return new SpawnBatchResult(requested, spawned);
    }

    private LivingEntity spawnCustomMonster(CustomMonster monster, Location location) {
        World world = location.getWorld();
        if (world == null) {
            return null;
        }

        Entity spawnedEntity;
        try {
            spawnedEntity = world.spawnEntity(location, monster.getEntityType());
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to spawn custom monster " + monster.getId() + ": " + exception.getMessage());
            return null;
        }
        if (!(spawnedEntity instanceof LivingEntity entity)) {
            spawnedEntity.remove();
            plugin.getLogger().warning("Configured custom monster " + monster.getId() + " uses non-living entity type " + monster.getEntityType().name() + ".");
            return null;
        }

        // Apply custom stats
        double scale = 5.0;
        if (plugin.getSkyblockCombatEngine() != null) {
            scale = plugin.getSkyblockCombatEngine().getHealthScale();
        }

        // --- NEW DYNAMIC SCALING ---
        int baseLevel = monster.getLevel();
        double baseHealth = monster.getHealth();
        int combatLevel = 0;
        
        Player nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (Player p : world.getPlayers()) {
            double d = p.getLocation().distanceSquared(location);
            if (d < nearestDist && d < 100 * 100) {
                nearest = p;
                nearestDist = d;
            }
        }
        
        if (nearest != null && plugin.getSkyblockSkillManager() != null) {
            combatLevel = plugin.getSkyblockSkillManager().getLevel(nearest, io.papermc.Grivience.skills.SkyblockSkill.COMBAT);
        }
        
        // Mobs gain 5% HP per Combat Level of nearby player
        double hpMultiplier = 1.0D + (combatLevel * 0.05D);
        double finalHealth = baseHealth * hpMultiplier;
        int finalLevel = baseLevel + combatLevel;
        // ---------------------------

        entity.setCustomNameVisible(true);
        io.papermc.Grivience.util.SkyblockDamageScaleUtil.setHealthSafely(entity, finalHealth / scale);

        var attackDamageAttr = entity.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE);        if (attackDamageAttr != null) {
            // Damage also scales slightly
            double damageMultiplier = 1.0D + (combatLevel * 0.02D);
            attackDamageAttr.setBaseValue((monster.getDamage() * damageMultiplier) / scale);
        }
        
        var movementSpeedAttr = entity.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED);
        if (movementSpeedAttr != null) {
            movementSpeedAttr.setBaseValue(monster.getSpeed());
        }
        
        MobHealthDisplay.setBaseName(entity, healthBaseNameKey, ChatColor.RED + monster.getDisplayName());

        if (monster.isGlowing()) {
            entity.setGlowing(true);
        }

        // Tag entity for custom drop handling
        entity.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "custom_monster"),
                org.bukkit.persistence.PersistentDataType.STRING,
                monster.getId()
        );

        entity.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "monster_level"),
                org.bukkit.persistence.PersistentDataType.INTEGER,
                finalLevel
        );

        applyMonsterLoadout(monster, entity);

        return entity;
    }

    private void applyMonsterLoadout(CustomMonster monster, LivingEntity entity) {
        if (monster == null || entity == null) {
            return;
        }
        if (!"crimson_warden".equalsIgnoreCase(monster.getId())) {
            return;
        }

        EntityEquipment equipment = entity.getEquipment();
        CustomArmorManager armorManager = plugin.getCustomArmorManager();
        if (equipment == null || armorManager == null) {
            return;
        }

        equipment.setHelmet(armorManager.createArmorPiece("crimson_warden", CustomArmorManager.ArmorPieceType.HELMET));
        equipment.setChestplate(armorManager.createArmorPiece("crimson_warden", CustomArmorManager.ArmorPieceType.CHESTPLATE));
        equipment.setLeggings(armorManager.createArmorPiece("crimson_warden", CustomArmorManager.ArmorPieceType.LEGGINGS));
        equipment.setBoots(armorManager.createArmorPiece("crimson_warden", CustomArmorManager.ArmorPieceType.BOOTS));
        if (customItemService != null) {
            equipment.setItemInMainHand(customItemService.createWeapon(CustomWeaponType.WARDENS_CLEAVER));
        }
        ArmorDurabilityUtil.ensureArmorUnbreakable(equipment);

        equipment.setHelmetDropChance(0.0F);
        equipment.setChestplateDropChance(0.0F);
        equipment.setLeggingsDropChance(0.0F);
        equipment.setBootsDropChance(0.0F);
        equipment.setItemInMainHandDropChance(0.0F);
        entity.setCanPickupItems(false);
        entity.setPersistent(true);
        entity.setRemoveWhenFarAway(false);
    }

    private LivingEntity spawnVanillaMonster(EntityType entityType, Location location) {
        World world = location.getWorld();
        if (world == null) {
            return null;
        }

        Entity entity;
        try {
            entity = world.spawnEntity(location, entityType);
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to spawn vanilla entity " + entityType.name() + ": " + exception.getMessage());
            return null;
        }
        if (entity instanceof LivingEntity livingEntity) {
            return livingEntity;
        }

        entity.remove();
        return null;
    }

    private CustomMonster resolveCustomMonster(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return null;
        }

        CustomMonster direct = monsters.get(rawId);
        if (direct != null) {
            return direct;
        }

        for (Map.Entry<String, CustomMonster> entry : monsters.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(rawId.trim())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String humanizeEntityType(EntityType entityType) {
        String[] parts = entityType.name().split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(part.charAt(0)).append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return builder.toString();
    }

    private Location findSpawnLocation(Location center, int radius, EntityType entityType) {
        if (center == null || center.getWorld() == null) {
            return null;
        }
        if (requiresWaterSpawn(entityType)) {
            return findAquaticSpawnLocation(center, radius);
        }
        return findGroundSpawnLocation(center, radius);
    }

    private Location findGroundSpawnLocation(Location center, int radius) {
        World world = center.getWorld();
        if (world == null) {
            return null;
        }
        int effectiveRadius = Math.max(0, radius);
        int centerY = center.getBlockY();
        int surfaceY = world.getHighestBlockYAt(center.getBlockX(), center.getBlockZ());
        boolean underground = centerY < surfaceY - 2;

        for (int attempt = 0; attempt < 10; attempt++) {
            int x = center.getBlockX() + random.nextInt(effectiveRadius * 2 + 1) - effectiveRadius;
            int z = center.getBlockZ() + random.nextInt(effectiveRadius * 2 + 1) - effectiveRadius;
            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }

            if (underground) {
                // Search near the spawn point's Y level instead of the world surface
                Location found = findUndergroundSpawnAt(world, x, z, centerY, effectiveRadius, center.getYaw(), center.getPitch());
                if (found != null) {
                    return found;
                }
            } else {
                int y = world.getHighestBlockYAt(x, z);
                Location loc = new Location(world, x + 0.5, y + 1, z + 0.5, center.getYaw(), center.getPitch());
                if (isSafeSpawnLocation(loc)) {
                    return loc;
                }
            }
        }

        return normalizeSpawnLocation(center, null);
    }

    private Location findUndergroundSpawnAt(World world, int x, int z, int centerY, int radius, float yaw, float pitch) {
        int verticalRange = Math.max(4, radius);
        int maxY = Math.min(world.getMaxHeight() - 2, centerY + verticalRange);
        int minY = Math.max(world.getMinHeight() + 1, centerY - verticalRange);
        for (int y = centerY; y >= minY; y--) {
            Location candidate = new Location(world, x + 0.5D, y, z + 0.5D, yaw, pitch);
            if (isSafeSpawnLocation(candidate)) {
                return candidate;
            }
        }
        for (int y = centerY + 1; y <= maxY; y++) {
            Location candidate = new Location(world, x + 0.5D, y, z + 0.5D, yaw, pitch);
            if (isSafeSpawnLocation(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private Location findAquaticSpawnLocation(Location center, int radius) {
        World world = center.getWorld();
        if (world == null) {
            return null;
        }
        int effectiveRadius = Math.max(0, radius);
        int verticalRange = Math.max(6, effectiveRadius);
        int minY = Math.max(world.getMinHeight() + 1, center.getBlockY() - verticalRange);
        int maxY = Math.min(world.getMaxHeight() - 2, center.getBlockY() + verticalRange);

        for (int attempt = 0; attempt < 16; attempt++) {
            int x = center.getBlockX() + random.nextInt(effectiveRadius * 2 + 1) - effectiveRadius;
            int z = center.getBlockZ() + random.nextInt(effectiveRadius * 2 + 1) - effectiveRadius;
            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }

            for (int y = maxY; y >= minY; y--) {
                Location candidate = new Location(world, x + 0.5D, y, z + 0.5D, center.getYaw(), center.getPitch());
                if (isSafeAquaticSpawnLocation(candidate)) {
                    return candidate;
                }
            }
        }

        return normalizeSpawnLocation(center, EntityType.GUARDIAN);
    }

    private boolean hasActivePlayers(World world) {
        if (world == null) {
            return false;
        }
        for (Player player : world.getPlayers()) {
            if (player == null || player.isDead()) {
                continue;
            }
            if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
                continue;
            }
            return true;
        }
        return false;
    }

    private int countNearbyNonPlayerLivingEntities(Location location, int horizontalRadius, int verticalRadius) {
        if (location == null || location.getWorld() == null) {
            return 0;
        }
        int count = 0;
        for (Entity nearby : location.getWorld().getNearbyEntities(location, horizontalRadius, verticalRadius, horizontalRadius)) {
            if (!(nearby instanceof LivingEntity living)) {
                continue;
            }
            if (living instanceof Player || living.isDead() || !living.isValid()) {
                continue;
            }
            count++;
        }
        return count;
    }

    private Location normalizeSpawnLocation(Location location, EntityType entityType) {
        if (location == null || location.getWorld() == null) {
            return null;
        }

        Location candidate = new Location(
                location.getWorld(),
                location.getBlockX() + 0.5D,
                location.getBlockY(),
                location.getBlockZ() + 0.5D,
                location.getYaw(),
                location.getPitch()
        );
        if (requiresWaterSpawn(entityType)) {
            return isSafeAquaticSpawnLocation(candidate) ? candidate : null;
        }
        return isSafeSpawnLocation(candidate) ? candidate : null;
    }

    private boolean isSafeSpawnLocation(Location location) {
        if (location == null || location.getWorld() == null || !isChunkLoaded(location)) {
            return false;
        }

        Block feet = location.getBlock();
        Block head = feet.getRelative(0, 1, 0);
        Block floor = feet.getRelative(0, -1, 0);
        if (!isPassableSpawnBlock(feet) || !isPassableSpawnBlock(head)) {
            return false;
        }
        if (!floor.getType().isSolid() || floor.isLiquid()) {
            return false;
        }

        Material floorType = floor.getType();
        return floorType != Material.MAGMA_BLOCK
                && floorType != Material.CAMPFIRE
                && floorType != Material.SOUL_CAMPFIRE;
    }

    private boolean isPassableSpawnBlock(Block block) {
        return block != null && !block.isLiquid() && (block.getType().isAir() || block.isPassable());
    }

    private boolean isSafeAquaticSpawnLocation(Location location) {
        if (location == null || location.getWorld() == null || !isChunkLoaded(location)) {
            return false;
        }

        Block feet = location.getBlock();
        Block head = feet.getRelative(0, 1, 0);
        return isWaterSpawnBlock(feet) && isWaterSpawnBlock(head);
    }

    private boolean isWaterSpawnBlock(Block block) {
        if (block == null) {
            return false;
        }

        Material type = block.getType();
        return type == Material.WATER
                || type == Material.BUBBLE_COLUMN
                || type == Material.SEAGRASS
                || type == Material.TALL_SEAGRASS
                || type == Material.KELP
                || type == Material.KELP_PLANT;
    }

    private boolean requiresWaterSpawn(EntityType entityType) {
        if (entityType == null) {
            return false;
        }
        return switch (entityType) {
            case AXOLOTL, COD, DOLPHIN, ELDER_GUARDIAN, GLOW_SQUID, GUARDIAN, PUFFERFISH,
                    SALMON, SQUID, TADPOLE, TROPICAL_FISH -> true;
            default -> false;
        };
    }

    private EntityType resolveEntityType(String monsterId) {
        CustomMonster customMonster = resolveCustomMonster(monsterId);
        if (customMonster != null) {
            return customMonster.getEntityType();
        }
        return parseVanillaEntityType(monsterId);
    }

    private EntityType resolveConfiguredEntityType(String monsterId, String rawType) {
        EntityType entityType = parseVanillaEntityType(rawType);
        if (entityType != null) {
            return entityType;
        }
        plugin.getLogger().warning("Custom monster " + monsterId + " has invalid entity type '" + rawType + "'. Falling back to ZOMBIE.");
        return EntityType.ZOMBIE;
    }

    private boolean isChunkLoaded(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        return location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    private int effectiveSpawnerBudget(int baseBudget) {
        if (plugin.getServerPerformanceMonitor() == null) {
            return baseBudget;
        }
        return plugin.getServerPerformanceMonitor().scaleBudget(baseBudget, 75, 50, 1);
    }

    private int effectiveManualSpawnBudget(int baseBudget) {
        if (baseBudget <= 0 || plugin.getServerPerformanceMonitor() == null) {
            return baseBudget;
        }
        return plugin.getServerPerformanceMonitor().scaleBudget(baseBudget, 60, 30, 1);
    }

    public void handleMonsterDeath(LivingEntity entity, Player killer) {
        String monsterId = entity.getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, "custom_monster"),
                org.bukkit.persistence.PersistentDataType.STRING
        );

        if (monsterId == null) {
            return;
        }

        CustomMonster monster = monsters.get(monsterId);
        if (monster == null) {
            return;
        }

        // Drop items
        for (MonsterDrop drop : monster.getDrops()) {
            if (Math.random() > drop.getChance()) {
                continue;
            }

            if (drop.isCustomItem() && customItemService != null) {
                ItemStack customItem = customItemService.createItemByKey(drop.getCustomItemId());
                if (customItem != null) {
                    int amount = drop.getMinAmount();
                    if (drop.getMaxAmount() > drop.getMinAmount()) {
                        amount = drop.getMinAmount() + new Random().nextInt(drop.getMaxAmount() - drop.getMinAmount() + 1);
                    }
                    customItem.setAmount(amount);
                    for (ItemStack dropStack : StackSizeSanitizer.splitToLegalStacks(customItem)) {
                        trackCollectionDrop(killer, dropStack);
                        entity.getWorld().dropItemNaturally(entity.getLocation(), dropStack);
                    }
                }
            } else {
                ItemStack dropItem = drop.toItemStack();
                if (dropItem != null) {
                    for (ItemStack dropStack : StackSizeSanitizer.splitToLegalStacks(dropItem)) {
                        trackCollectionDrop(killer, dropStack);
                        entity.getWorld().dropItemNaturally(entity.getLocation(), dropStack);
                    }
                }
            }
        }

        // Give XP
        if (killer != null) {
            // Award vanilla XP first (will be cleared by CustomMonsterDeathListener if not custom)
            // killer.giveExp(monster.getExpReward()); // Removed to avoid double XP if listener logic changes

            // Award Skyblock Combat XP
            if (monster != null && plugin.getSkyblockLevelManager() != null && plugin.getSkyblockLevelManager().getSkillManager() != null) {
                long combatSkillXpAward = plugin.getSkyblockLevelManager().resolveCombatSkillXp(entity);
                if (combatSkillXpAward > 0) {
                    plugin.getSkyblockLevelManager().getSkillManager().addXp(killer, io.papermc.Grivience.skills.SkyblockSkill.COMBAT, combatSkillXpAward);
                    plugin.getSkyblockLevelManager().getSkillManager().addXp(killer, io.papermc.Grivience.skills.SkyblockSkill.HUNTING, combatSkillXpAward); // Also award Hunting XP for mob kills
                }
            }
        }
    }

    private void trackCollectionDrop(Player killer, ItemStack item) {
        if (killer == null || item == null || item.getType().isAir()) {
            return;
        }
        var collectionsManager = plugin.getCollectionsManager();
        if (collectionsManager == null || !collectionsManager.isEnabled()) {
            return;
        }

        String itemId = null;
        if (customItemService != null) {
            itemId = customItemService.itemId(item);
        }
        if (itemId == null || itemId.isBlank()) {
            itemId = item.getType().name();
        }
        collectionsManager.addCollection(killer, itemId, item.getAmount());
    }

    public void reload() {
        loadMonsters();
        loadSpawnPoints();
        restartSpawning();
    }
}
