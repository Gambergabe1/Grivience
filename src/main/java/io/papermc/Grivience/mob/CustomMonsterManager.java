package io.papermc.Grivience.mob;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.item.CustomItemService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class CustomMonsterManager {
    private final GriviencePlugin plugin;
    private final Map<String, CustomMonster> monsters = new HashMap<>();
    private final Map<UUID, SpawnPoint> spawnPoints = new ConcurrentHashMap<>();
    private final Map<String, List<SpawnPoint>> spawnPointsByWorld = new ConcurrentHashMap<>();
    private CustomItemService customItemService;
    private boolean spawningEnabled;

    public CustomMonsterManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        loadMonsters();
        loadSpawnPoints();
        startSpawning();
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
        try {
            monster.setEntityType(EntityType.valueOf(typeName.toUpperCase()));
        } catch (IllegalArgumentException e) {
            monster.setEntityType(EntityType.ZOMBIE);
        }

        monster.setHealth(plugin.getConfig().getDouble(path + ".health", 20.0));
        monster.setDamage(plugin.getConfig().getDouble(path + ".damage", 3.0));
        monster.setSpeed(plugin.getConfig().getDouble(path + ".speed", 0.23));
        monster.setExpReward(plugin.getConfig().getInt(path + ".exp-reward", 10));
        monster.setGlowing(plugin.getConfig().getBoolean(path + ".glowing", false));

        // Load drops
        monster.getDrops().clear();
        List<String> dropIds = plugin.getConfig().getStringList(path + ".drops");
        for (String dropId : dropIds) {
            String dropPath = path + ".drops." + dropId;
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

        return monster;
    }

    public void loadSpawnPoints() {
        spawnPoints.clear();
        spawnPointsByWorld.clear();

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
                SpawnPoint point = new SpawnPoint(pointSection.getValues(false));
                spawnPoints.put(point.getId(), point);

                String worldName = pointSection.getString("world");
                spawnPointsByWorld.computeIfAbsent(worldName, k -> new ArrayList<>()).add(point);
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
        spawnPointsByWorld.computeIfAbsent(location.getWorld().getName(), k -> new ArrayList<>()).add(point);
        saveSpawnPoints();
        return point;
    }

    public boolean removeSpawnPoint(UUID pointId) {
        SpawnPoint point = spawnPoints.remove(pointId);
        if (point != null) {
            List<SpawnPoint> worldPoints = spawnPointsByWorld.get(point.getLocation().getWorld().getName());
            if (worldPoints != null) {
                worldPoints.remove(point);
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

    private void startSpawning() {
        spawningEnabled = plugin.getConfig().getBoolean("custom-monsters.enabled", true);

        if (!spawningEnabled) {
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                for (SpawnPoint point : spawnPoints.values()) {
                    if (!point.isActive() || !point.isLocationValid()) {
                        continue;
                    }

                    if (point.countNearbyEntities() >= point.getMaxNearbyEntities()) {
                        continue;
                    }

                    spawnMonster(point);
                }
            }
        }.runTaskTimer(plugin, 100L, 20L);
    }

    private void spawnMonster(SpawnPoint point) {
        CustomMonster monster = monsters.get(point.getMonsterId());
        if (monster == null) {
            return;
        }

        Location spawnLoc = point.getLocation();
        World world = spawnLoc.getWorld();

        // Find a suitable spawn location within radius
        Location actualSpawn = findSpawnLocation(spawnLoc, point.getSpawnRadius());
        if (actualSpawn == null) {
            return;
        }

        LivingEntity entity = (LivingEntity) world.spawnEntity(actualSpawn, monster.getEntityType());

        // Apply custom stats
        entity.setCustomNameVisible(true);
        entity.setCustomName(ChatColor.RED + monster.getDisplayName());
        entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(monster.getHealth());
        entity.setHealth(monster.getHealth());
        entity.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).setBaseValue(monster.getDamage());
        entity.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED).setBaseValue(monster.getSpeed());

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
                new org.bukkit.NamespacedKey(plugin, "spawn_point"),
                org.bukkit.persistence.PersistentDataType.STRING,
                point.getId().toString()
        );
    }

    private Location findSpawnLocation(Location center, int radius) {
        World world = center.getWorld();
        Random random = new Random();

        for (int attempt = 0; attempt < 10; attempt++) {
            int x = center.getBlockX() + random.nextInt(radius * 2 + 1) - radius;
            int z = center.getBlockZ() + random.nextInt(radius * 2 + 1) - radius;
            int y = world.getHighestBlockYAt(x, z);

            Location loc = new Location(world, x + 0.5, y + 1, z + 0.5);
            if (loc.getBlock().getType().isAir() && loc.getBlock().getRelative(0, -1, 0).getType().isSolid()) {
                return loc;
            }
        }

        return null;
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
                    entity.getWorld().dropItemNaturally(entity.getLocation(), customItem);
                }
            } else {
                ItemStack dropItem = drop.toItemStack();
                if (dropItem != null) {
                    entity.getWorld().dropItemNaturally(entity.getLocation(), dropItem);
                }
            }
        }

        // Give XP
        if (killer != null) {
            killer.giveExp(monster.getExpReward());
        }
    }

    public void reload() {
        loadMonsters();
        loadSpawnPoints();
    }
}
