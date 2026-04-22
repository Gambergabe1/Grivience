package io.papermc.Grivience.mob;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.item.CustomArmorManager;
import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.item.CustomWeaponType;
import io.papermc.Grivience.stats.SkyblockCombatStatsService;
import io.papermc.Grivience.util.ArmorDurabilityUtil;
import io.papermc.Grivience.util.DropDeliveryUtil;
import io.papermc.Grivience.util.SkyblockDamageScaleUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CrimsonWardenBossListener implements Listener {
    private static final String BOSS_ID = "crimson_warden";
    private static final byte MINION_MARKER = (byte) 1;

    private static final double BOSS_MELEE_DAMAGE_MIN = 15.0D;
    private static final double BOSS_MELEE_DAMAGE_MAX = 20.0D;
    private static final double MINION_MELEE_DAMAGE = 5.0D;
    private static final double GROUND_SLAM_DAMAGE_MIN = 8.0D;
    private static final double GROUND_SLAM_DAMAGE_MAX = 10.0D;
    private static final double GROUND_SLAM_RADIUS = 3.6D;
    private static final double PLAYER_DETECTION_RADIUS = 12.0D;
    private static final double HEALTH_AURA_REGEN = 2.0D;

    private static final long SUMMON_INTERVAL_MS = 15_000L;
    private static final long GROUND_SLAM_INTERVAL_MS = 10_000L;
    private static final long DEFENSE_SHRED_DURATION_TICKS = 100L;

    private final GriviencePlugin plugin;
    private final CustomItemService customItemService;
    private final CustomArmorManager armorManager;
    private final NamespacedKey customMonsterKey;
    private final NamespacedKey minionKey;
    private final Map<UUID, Long> nextSummonAtMs = new ConcurrentHashMap<>();
    private final Map<UUID, Long> nextSlamAtMs = new ConcurrentHashMap<>();
    private final Set<UUID> claimedFirstKillProfiles = ConcurrentHashMap.newKeySet();
    private final Set<UUID> bossAbilityDamageContext = ConcurrentHashMap.newKeySet();
    private final Random random = new Random();
    private final File claimsFile;
    private YamlConfiguration claimsConfig;

    public CrimsonWardenBossListener(
            GriviencePlugin plugin,
            CustomItemService customItemService,
            CustomArmorManager armorManager
    ) {
        this.plugin = plugin;
        this.customItemService = customItemService;
        this.armorManager = armorManager;
        this.customMonsterKey = new NamespacedKey(plugin, "custom_monster");
        this.minionKey = new NamespacedKey(plugin, "crimson_warden_minion");
        this.claimsFile = new File(plugin.getDataFolder(), "crimson-warden-rewards.yml");
        loadClaims();

        Bukkit.getScheduler().runTaskTimer(plugin, this::tickBosses, 20L, 20L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBossDamage(EntityDamageByEntityEvent event) {
        LivingEntity damager = event.getDamager() instanceof LivingEntity living ? living : null;
        if (damager == null) {
            return;
        }

        if (isCrimsonWarden(damager)) {
            if (bossAbilityDamageContext.contains(damager.getUniqueId())) {
                return;
            }
            event.setDamage(toMinecraftDamage(randomBetween(BOSS_MELEE_DAMAGE_MIN, BOSS_MELEE_DAMAGE_MAX)));
            if (event.getEntity() instanceof Player player && random.nextDouble() < 0.15D) {
                applyDefenseShred(player, 0.10D, DEFENSE_SHRED_DURATION_TICKS);
                player.sendMessage(ChatColor.DARK_RED + "Crimson Warden shredded your armor!");
                player.playSound(player.getLocation(), Sound.ITEM_AXE_STRIP, 0.8F, 0.75F);
            }
            return;
        }

        if (isCrimsonMinion(damager)) {
            event.setDamage(toMinecraftDamage(MINION_MELEE_DAMAGE));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBossDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (isCrimsonMinion(entity)) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            return;
        }
        if (!isCrimsonWarden(entity)) {
            return;
        }

        nextSummonAtMs.remove(entity.getUniqueId());
        nextSlamAtMs.remove(entity.getUniqueId());
        event.setDroppedExp(0);

        Player killer = entity.getKiller();
        if (killer == null) {
            return;
        }

        Location dropLocation = entity.getLocation().add(0.0D, 0.5D, 0.0D);
        ItemStack armorPiece = randomArmorPiece();
        if (armorPiece != null) {
            DropDeliveryUtil.giveToInventoryOrDrop(killer, armorPiece, dropLocation);
        }

        UUID rewardOwner = rewardOwnerId(killer);
        if (!claimedFirstKillProfiles.contains(rewardOwner) && customItemService != null) {
            ItemStack cleaver = customItemService.createWeapon(CustomWeaponType.WARDENS_CLEAVER);
            if (cleaver != null) {
                DropDeliveryUtil.giveToInventoryOrDrop(killer, cleaver, dropLocation);
                claimedFirstKillProfiles.add(rewardOwner);
                saveClaims();
                killer.sendMessage(ChatColor.GOLD + "First Crimson Warden kill: " + ChatColor.RED + "Warden's Cleaver");
                killer.playSound(killer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8F, 1.0F);
            }
        }

        spawnExperienceBurst(dropLocation, 100 + random.nextInt(51));

        if (random.nextDouble() < 0.05D) {
            ItemStack cosmetic = randomCosmeticDrop();
            if (cosmetic != null) {
                DropDeliveryUtil.giveToInventoryOrDrop(killer, cosmetic, dropLocation);
                killer.sendMessage(ChatColor.LIGHT_PURPLE + "Rare cosmetic drop: " + ChatColor.RED + cosmetic.getItemMeta().getDisplayName());
            }
        }
    }

    private void tickBosses() {
        long now = System.currentTimeMillis();
        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity living : world.getLivingEntities()) {
                if (!isCrimsonWarden(living)) {
                    continue;
                }
                tickBoss(living, now);
            }
        }
    }

    private void tickBoss(LivingEntity boss, long now) {
        if (!boss.isValid() || boss.isDead()) {
            nextSummonAtMs.remove(boss.getUniqueId());
            nextSlamAtMs.remove(boss.getUniqueId());
            return;
        }

        nextSummonAtMs.putIfAbsent(boss.getUniqueId(), now + SUMMON_INTERVAL_MS);
        nextSlamAtMs.putIfAbsent(boss.getUniqueId(), now + GROUND_SLAM_INTERVAL_MS);

        List<Player> nearbyPlayers = nearbyPlayers(boss.getLocation(), PLAYER_DETECTION_RADIUS);
        if (nearbyPlayers.isEmpty()) {
            double maxHealth = resolveMaxHealth(boss);
            if (boss.getHealth() < maxHealth) {
                boss.setHealth(Math.min(maxHealth, boss.getHealth() + HEALTH_AURA_REGEN));
                boss.getWorld().spawnParticle(Particle.HEART, boss.getLocation().add(0.0D, 1.4D, 0.0D), 2, 0.2D, 0.2D, 0.2D, 0.01D);
            }
            return;
        }

        if (now >= nextSummonAtMs.getOrDefault(boss.getUniqueId(), now + SUMMON_INTERVAL_MS)) {
            summonMinions(boss);
            nextSummonAtMs.put(boss.getUniqueId(), now + SUMMON_INTERVAL_MS);
        }

        List<Player> slamTargets = nearbyPlayers(boss.getLocation(), GROUND_SLAM_RADIUS);
        if (!slamTargets.isEmpty() && now >= nextSlamAtMs.getOrDefault(boss.getUniqueId(), now + GROUND_SLAM_INTERVAL_MS)) {
            doGroundSlam(boss, slamTargets);
            nextSlamAtMs.put(boss.getUniqueId(), now + GROUND_SLAM_INTERVAL_MS);
        }
    }

    private void summonMinions(LivingEntity boss) {
        int count = 2 + random.nextInt(2);
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 0.9F, 0.75F);
        boss.getWorld().spawnParticle(Particle.SMOKE, boss.getLocation().add(0.0D, 0.5D, 0.0D), 16, 0.6D, 0.3D, 0.6D, 0.02D);

        for (int i = 0; i < count; i++) {
            Location spawn = boss.getLocation().clone().add(
                    random.nextDouble(-2.5D, 2.5D),
                    0.0D,
                    random.nextDouble(-2.5D, 2.5D)
            );
            Zombie zombie = boss.getWorld().spawn(spawn, Zombie.class);
            zombie.setBaby(true);
            zombie.setCustomName(ChatColor.RED + "Crimson Warden Minion");
            zombie.setCustomNameVisible(true);
            zombie.getPersistentDataContainer().set(minionKey, PersistentDataType.BYTE, MINION_MARKER);
            zombie.setCanPickupItems(false);

            double scale = 5.0;
            if (plugin.getSkyblockCombatEngine() != null) {
                scale = plugin.getSkyblockCombatEngine().getHealthScale();
            }

            SkyblockDamageScaleUtil.setHealthSafely(zombie, 50.0D / scale);

            AttributeInstance attackDamage = zombie.getAttribute(Attribute.ATTACK_DAMAGE);
            if (attackDamage != null) {
                attackDamage.setBaseValue(MINION_MELEE_DAMAGE / scale);
            }
            AttributeInstance moveSpeed = zombie.getAttribute(Attribute.MOVEMENT_SPEED);
            if (moveSpeed != null) {
                moveSpeed.setBaseValue(0.28D);
            }

            equipMinion(zombie);
        }
    }

    private void equipMinion(Zombie zombie) {
        if (zombie == null) {
            return;
        }
        EntityEquipment equipment = zombie.getEquipment();
        if (equipment == null) {
            return;
        }

        ItemStack helmet = new ItemStack(org.bukkit.Material.LEATHER_HELMET);
        if (helmet.getItemMeta() instanceof LeatherArmorMeta leatherMeta) {
            leatherMeta.setColor(Color.fromRGB(0x9C1B1B));
            helmet.setItemMeta(leatherMeta);
        }

        equipment.setHelmet(ArmorDurabilityUtil.ensureArmorUnbreakable(helmet));
        equipment.setHelmetDropChance(0.0F);
    }

    private void doGroundSlam(LivingEntity boss, List<Player> targets) {
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.1F, 0.65F);
        boss.getWorld().spawnParticle(Particle.DUST, boss.getLocation().add(0.0D, 0.1D, 0.0D), 24, 0.8D, 0.1D, 0.8D, 0.0D, new Particle.DustOptions(Color.fromRGB(0xB62424), 1.4F));

        UUID bossId = boss.getUniqueId();
        bossAbilityDamageContext.add(bossId);
        try {
            for (Player player : targets) {
                if (player == null || !player.isOnline() || player.isDead()) {
                    continue;
                }
                player.damage(toMinecraftDamage(randomBetween(GROUND_SLAM_DAMAGE_MIN, GROUND_SLAM_DAMAGE_MAX)), boss);
                var push = player.getLocation().toVector().subtract(boss.getLocation().toVector());
                if (push.lengthSquared() > 1.0E-6D) {
                    push.normalize().multiply(1.05D).setY(0.34D);
                    player.setVelocity(player.getVelocity().add(push));
                }
            }
        } finally {
            bossAbilityDamageContext.remove(bossId);
        }
    }

    private List<Player> nearbyPlayers(Location center, double radius) {
        List<Player> players = new ArrayList<>();
        if (center == null || center.getWorld() == null) {
            return players;
        }
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof Player player
                    && player.isOnline()
                    && !player.isDead()
                    && player.getGameMode() != org.bukkit.GameMode.CREATIVE
                    && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                players.add(player);
            }
        }
        return players;
    }

    private boolean isCrimsonWarden(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        String monsterId = entity.getPersistentDataContainer().get(customMonsterKey, PersistentDataType.STRING);
        return BOSS_ID.equalsIgnoreCase(monsterId);
    }

    private boolean isCrimsonMinion(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        Byte marker = entity.getPersistentDataContainer().get(minionKey, PersistentDataType.BYTE);
        return marker != null && marker == MINION_MARKER;
    }

    private ItemStack randomArmorPiece() {
        if (armorManager == null) {
            return null;
        }
        List<CustomArmorManager.ArmorPieceType> pieces = List.of(
                CustomArmorManager.ArmorPieceType.HELMET,
                CustomArmorManager.ArmorPieceType.CHESTPLATE,
                CustomArmorManager.ArmorPieceType.LEGGINGS,
                CustomArmorManager.ArmorPieceType.BOOTS
        );
        return armorManager.createArmorPiece("crimson_warden", pieces.get(random.nextInt(pieces.size())));
    }

    private ItemStack randomCosmeticDrop() {
        return random.nextBoolean() ? createGlowingEyesCosmetic() : createDyedLeatherCosmetic();
    }

    private ItemStack createGlowingEyesCosmetic() {
        ItemStack item = new ItemStack(org.bukkit.Material.RED_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(ChatColor.RED + "Crimson Eyes Cosmetic");
        meta.setLore(List.of(
                ChatColor.GRAY + "A cosmetic keepsake from the",
                ChatColor.GRAY + "first defender of spawn.",
                "",
                ChatColor.BLUE + "" + ChatColor.BOLD + "RARE COSMETIC"
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createDyedLeatherCosmetic() {
        ItemStack item = new ItemStack(org.bukkit.Material.LEATHER_HELMET);
        if (item.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(Color.fromRGB(0xB62424));
            meta.setDisplayName(ChatColor.RED + "Warden's Visage");
            meta.setLore(List.of(
                    ChatColor.GRAY + "A dyeable vanity piece inspired",
                    ChatColor.GRAY + "by the Crimson Warden.",
                    "",
                    ChatColor.BLUE + "" + ChatColor.BOLD + "RARE COSMETIC"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void spawnExperienceBurst(Location location, int amount) {
        if (location == null || location.getWorld() == null || amount <= 0) {
            return;
        }
        ExperienceOrb orb = location.getWorld().spawn(location, ExperienceOrb.class);
        orb.setExperience(amount);
    }

    private UUID rewardOwnerId(Player player) {
        if (player == null) {
            return new UUID(0L, 0L);
        }
        if (plugin.getProfileManager() != null) {
            var profile = plugin.getProfileManager().getSelectedProfile(player);
            if (profile != null && profile.getProfileId() != null) {
                return profile.getProfileId();
            }
        }
        return player.getUniqueId();
    }

    private void applyDefenseShred(Player player, double percent, long durationTicks) {
        if (player == null || percent <= 0.0D || durationTicks <= 0L) {
            return;
        }
        player.getPersistentDataContainer().set(
                SkyblockCombatStatsService.TEMP_DEFENSE_SHRED_PERCENT_KEY,
                PersistentDataType.DOUBLE,
                Math.max(0.0D, Math.min(0.95D, percent))
        );
        player.getPersistentDataContainer().set(
                SkyblockCombatStatsService.TEMP_DEFENSE_SHRED_UNTIL_KEY,
                PersistentDataType.LONG,
                System.currentTimeMillis() + (durationTicks * 50L)
        );
        if (plugin.getSkyblockCombatEngine() != null) {
            plugin.getSkyblockCombatEngine().refreshNow(player);
        }
    }

    private double randomBetween(double min, double max) {
        if (max <= min) {
            return min;
        }
        return min + (random.nextDouble() * (max - min));
    }

    private double toMinecraftDamage(double skyblockDamage) {
        double scale = plugin.getSkyblockCombatEngine() == null
                ? SkyblockDamageScaleUtil.DEFAULT_HEALTH_SCALE
                : plugin.getSkyblockCombatEngine().getHealthScale();
        return SkyblockDamageScaleUtil.toMinecraftDamage(skyblockDamage, scale);
    }

    private double resolveMaxHealth(LivingEntity entity) {
        if (entity == null) {
            return 20.0D;
        }
        AttributeInstance maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
        return maxHealth == null ? 20.0D : Math.max(1.0D, maxHealth.getValue());
    }

    private void loadClaims() {
        claimsConfig = YamlConfiguration.loadConfiguration(claimsFile);
        List<String> rawClaims = claimsConfig.getStringList("claimed-profiles");
        for (String raw : rawClaims) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                claimedFirstKillProfiles.add(UUID.fromString(raw.trim()));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void saveClaims() {
        if (claimsConfig == null) {
            claimsConfig = new YamlConfiguration();
        }
        List<String> serialized = new ArrayList<>();
        for (UUID uuid : claimedFirstKillProfiles) {
            serialized.add(uuid.toString());
        }
        claimsConfig.set("claimed-profiles", serialized);
        try {
            claimsConfig.save(claimsFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save Crimson Warden rewards: " + exception.getMessage());
        }
    }
}
