package io.papermc.Grivience.listener;

import io.papermc.Grivience.combat.CombatDamageModel;
import io.papermc.Grivience.dungeon.DungeonManager;
import io.papermc.Grivience.stats.SkyblockCombatEngine;
import io.papermc.Grivience.stats.SkyblockManaManager;
import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.item.CustomWeaponProfiles;
import io.papermc.Grivience.item.CustomWeaponType;
import io.papermc.Grivience.item.ReforgeType;
import io.papermc.Grivience.party.PartyManager;
import io.papermc.Grivience.dungeon.DungeonSession;
import io.papermc.Grivience.stats.SkyblockCombatStatsService;
import io.papermc.Grivience.stats.SkyblockPlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import io.papermc.Grivience.util.DamageIndicatorUtil;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import io.papermc.Grivience.enchantment.EnchantmentRegistry;
import io.papermc.Grivience.enchantment.SkyblockEnchantment;
import io.papermc.Grivience.enchantment.SkyblockEnchantStorage;
import io.papermc.Grivience.skills.SkyblockSkill;
import io.papermc.Grivience.skills.SkyblockSkillManager;

public final class CustomWeaponCombatListener implements Listener {
    private static final double WARDENS_CLEAVER_CLEAVE_RADIUS = 2.75D;
    private static final long WARDENS_CLEAVER_ARMOR_BREAK_TICKS = 100L;
    private static final long VOID_SHIFT_DURATION_MS = 3000L;
    private static final long FRACTURE_DASH_WINDOW_MS = 5000L;
    private static final long VOIDSHOT_DEBUFF_DURATION_MS = 8000L;
    private static final int MAX_VOIDSHOT_STACKS = 5;
    private static final int MAX_FRACTURE_DASH_STACKS = 5;
    private static final int MAX_RIFTSTORM_ABILITY_STACKS = 5;

    private final JavaPlugin plugin;
    private final CustomItemService customItemService;
    private final SkyblockCombatEngine combatEngine;
    private final SkyblockManaManager manaManager;
    private final DungeonManager dungeonManager;
    private final PartyManager partyManager;
    private final SkyblockEnchantStorage enchantStorage;
    private SkyblockSkillManager skillManager;
    private final Map<UUID, EnumMap<CustomWeaponType, Long>> abilityCooldowns = new HashMap<>();
    private final Map<UUID, ItemStack> trackedProjectileWeapons = new HashMap<>();
    private final Set<UUID> abilityDamageContext = new HashSet<>();
    private final Map<UUID, Long> voidShiftExpiries = new HashMap<>();
    private final Map<UUID, Integer> fractureDashStacks = new HashMap<>();
    private final Map<UUID, Long> fractureDashExpiries = new HashMap<>();
    private final Map<UUID, Integer> riftstormAbilityStacks = new HashMap<>();
    private final Map<UUID, Integer> voidshotDebuffStacks = new HashMap<>();
    private final Map<UUID, Long> voidshotDebuffExpiries = new HashMap<>();
    private final Map<UUID, java.util.ArrayDeque<MeleeIndicatorContext>> pendingMeleeIndicators = new HashMap<>();
    private final NamespacedKey wardensArmorBreakKey;

    private boolean combatEnabled;
    private boolean abilitiesEnabled;
    private boolean abilityRightClickBlocks;
    private boolean notifyCooldown;
    private boolean instantBowShotsEnabled;

    private double baseDamageScale;
    private double strengthScaling;

    private double abilityDamageScale;
    private double abilityManaScale;
    private double abilityCooldownScale;
    private double instantBowVelocity;
    private int instantBowCooldownTicks;

    public CustomWeaponCombatListener(
            JavaPlugin plugin,
            CustomItemService customItemService,
            SkyblockCombatEngine combatEngine,
            SkyblockManaManager manaManager,
            DungeonManager dungeonManager,
            PartyManager partyManager
    ) {
        this.plugin = plugin;
        this.customItemService = customItemService;
        this.combatEngine = combatEngine;
        this.manaManager = manaManager;
        this.dungeonManager = dungeonManager;
        this.partyManager = partyManager;
        this.enchantStorage = new SkyblockEnchantStorage(plugin);
        this.wardensArmorBreakKey = new NamespacedKey(plugin, "wardens_cleaver_armor_break");
    }

    public void setSkillManager(SkyblockSkillManager skillManager) {
        this.skillManager = skillManager;
    }

    public void reloadFromConfig() {
        combatEnabled = plugin.getConfig().getBoolean("custom-items.combat.enabled", true);
        abilitiesEnabled = plugin.getConfig().getBoolean("custom-items.combat.mana-abilities.enabled", true);
        abilityRightClickBlocks = plugin.getConfig().getBoolean("custom-items.combat.mana-abilities.allow-right-click-block", true);
        notifyCooldown = plugin.getConfig().getBoolean("custom-items.combat.mana-abilities.notify-cooldown", true);
        instantBowShotsEnabled = plugin.getConfig().getBoolean("custom-items.combat.instant-bow-shots.enabled", true);
        instantBowVelocity = sanitizePositive(plugin.getConfig().getDouble("custom-items.combat.instant-bow-shots.arrow-velocity", 3.0D), 3.0D);
        instantBowCooldownTicks = Math.max(0, plugin.getConfig().getInt("custom-items.combat.instant-bow-shots.cooldown-ticks", 4));

        double scale = 5.0;
        if (combatEngine != null) {
            scale = combatEngine.getHealthScale();
        }
        double defaultBaseScale = 1.0D / scale;

        baseDamageScale = sanitizePositive(plugin.getConfig().getDouble("custom-items.combat.damage.base-scale", defaultBaseScale), defaultBaseScale);
        // Skyblock melee scaling: 1% damage per Strength.
        strengthScaling = sanitizePositive(plugin.getConfig().getDouble("custom-items.combat.damage.strength-scaling", 0.01D), 0.01D);

        abilityDamageScale = sanitizePositive(plugin.getConfig().getDouble("custom-items.combat.mana-abilities.damage-scale", defaultBaseScale), defaultBaseScale);
        abilityManaScale = sanitizePositive(plugin.getConfig().getDouble("custom-items.combat.mana-abilities.mana-scale", 1.0D), 1.0D);
        abilityCooldownScale = sanitizePositive(plugin.getConfig().getDouble("custom-items.combat.mana-abilities.cooldown-scale", 1.0D), 1.0D);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!combatEnabled) {
            return;
        }

        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null || abilityDamageContext.contains(attacker.getUniqueId())) {
            return;
        }
        if (event.getEntity() instanceof Player victim) {
            if (isEndMinesPvpBlocked(attacker, victim) || isFriendly(attacker, victim)) {
                event.setCancelled(true);
                return;
            }
        }

        LivingEntity victim = event.getEntity() instanceof LivingEntity le ? le : null;
        if (victim == null) return;

        ItemStack weaponItem = resolveWeaponItem(attacker, event.getDamager());
        CustomWeaponType weaponType = resolveWeaponType(weaponItem);
        
        WeaponProfile baseProfile;
        if (weaponType != null) {
            baseProfile = profile(weaponType);
        } else {
            baseProfile = vanillaProfile(weaponItem);
        }

        WeaponProfile profile = applyReforge(baseProfile, customItemService.reforgeOf(weaponItem), weaponItem);
        SkyblockPlayerStats stats = combatEngine == null ? null : combatEngine.stats(attacker);

        double weaponDamage = Math.max(0.0D, profile.flatDamage());
        double totalStrength = stats == null ? profile.strength() : stats.strength();
        totalStrength = Math.max(0.0D, totalStrength);

        // --- OFFENSIVE ENCHANTS (ATTACKER) ---
        double enchantMultiplier = 1.0;
        
        // Sharpness: +5% damage per level
        int sharpness = enchantStorage.getLevel(weaponItem, EnchantmentRegistry.get("sharpness"));
        if (sharpness > 0) enchantMultiplier += (sharpness * 0.05);

        // Smite: +8% damage per level to undead
        int smite = enchantStorage.getLevel(weaponItem, EnchantmentRegistry.get("smite"));
        if (smite > 0 && isUndead(victim)) enchantMultiplier += (smite * 0.08);

        // Bane of Arthropods: +8% damage per level to spiders/bugs
        int bane = enchantStorage.getLevel(weaponItem, EnchantmentRegistry.get("bane_of_arthropods"));
        if (bane > 0 && isArthropod(victim)) enchantMultiplier += (bane * 0.08);

        // --- SYNERGY: Newbie Katana + Rookie Samurai ---
        if (weaponType == CustomWeaponType.NEWBIE_KATANA) {
            if (plugin instanceof io.papermc.Grivience.GriviencePlugin gPlugin && gPlugin.getArmorSetBonusListener() != null) {
                if (gPlugin.getArmorSetBonusListener().hasFullSet(attacker, "rookie")) {
                    enchantMultiplier += 0.20; // +20% damage bonus
                }
            }
        }

        // --- SKILL PERKS ---
        double perkMultiplier = 1.0;
        if (skillManager != null) {
            double warriorPerk = skillManager.getPerkValue(attacker, SkyblockSkill.COMBAT);
            if (warriorPerk > 0) {
                perkMultiplier += (warriorPerk / 100.0);
            }
        }

        // --- PET ABILITIES (ATTACKER) ---
        double petPerkMultiplier = 1.0;
        if (plugin instanceof io.papermc.Grivience.GriviencePlugin gPlugin && gPlugin.getPetManager() != null) {
            petPerkMultiplier = gPlugin.getPetManager().getAbilityMultiplier(attacker, victim);
        }

        // Skyblock-style melee formula (scaled to Minecraft using baseDamageScale).
        double critChancePercent = stats == null ? profile.critChancePercent() : stats.critChancePercent();
        boolean isCritical = CombatDamageModel.isCriticalHit(critChancePercent, ThreadLocalRandom.current().nextDouble());
        double critDamagePercent = stats == null ? profile.critDamagePercent() : stats.critDamagePercent();
        CombatDamageModel.AttackResult attack = CombatDamageModel.computeWeaponAttack(
                weaponDamage,
                totalStrength,
                critDamagePercent,
                isCritical,
                strengthScaling,
                enchantMultiplier,
                perkMultiplier * petPerkMultiplier,
                baseDamageScale,
                dungeonDamageScale(attacker),
                persistentDamageBuffMultiplier(attacker),
                targetIncomingDamageMultiplier(victim)
        );

        // Global Booster Integration
        if (plugin instanceof io.papermc.Grivience.GriviencePlugin griv && griv.getGlobalEventManager() != null) {
            attack = attack.scale(griv.getGlobalEventManager().getMultiplier(io.papermc.Grivience.event.GlobalEventManager.BoosterType.DAMAGE));
        }

        double finalDamage = Math.max(0.1D, attack.minecraftDamage());
        if (attack.critical()) {
            attacker.getWorld().spawnParticle(Particle.CRIT, event.getEntity().getLocation().add(0.0D, 1.0D, 0.0D), 8, 0.35D, 0.25D, 0.35D, 0.02D);
        }

        // --- ENHANCED DAMAGE FEEL: BYPASS VANILLA MODIFIERS ---
        // We set the damage to the BASE modifier and zero out everything else 
        // to ensure Skyblock damage is "true" relative to the health scale.
        event.setDamage(org.bukkit.event.entity.EntityDamageEvent.DamageModifier.BASE, Math.max(0.1D, finalDamage));
        for (org.bukkit.event.entity.EntityDamageEvent.DamageModifier modifier : org.bukkit.event.entity.EntityDamageEvent.DamageModifier.values()) {
            if (modifier != org.bukkit.event.entity.EntityDamageEvent.DamageModifier.BASE && event.isApplicable(modifier)) {
                event.setDamage(modifier, 0.0D);
            }
        }
        
        // --- BYPASS HURT RESISTANCE (I-FRAMES) ---
        // Skyblock combat requires consistent hits. Vanilla 10-tick invulnerability
        // makes many hits do 0 damage despite showing particles/holograms.
        if (victim instanceof LivingEntity living) {
            // Set to 0 so the next hit can land immediately. 
            // Combined with Attack Speed attributes, this makes combat feel responsive.
            living.setNoDamageTicks(0);
        }
        
        queueMeleeIndicator(event.getEntity(), attack.critical(), attack.displayDamage());
        applyWardensCleaverPassives(attacker, victim, weaponType, finalDamage);
        applyNewbieKatanaPassives(attacker, weaponType);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onDamageMonitor(EntityDamageByEntityEvent event) {
        if (!combatEnabled) return;

        MeleeIndicatorContext context = pollMeleeIndicator(event.getEntity());
        if (context == null || event.isCancelled()) {
            return;
        }

        double indicatorDamage = context.displayDamage();
        if (indicatorDamage <= 0.0D) {
            return;
        }

        DamageIndicatorUtil.spawn((GriviencePlugin) plugin, event.getEntity(), indicatorDamage, context.critical());
    }

    private void queueMeleeIndicator(Entity entity, boolean critical, double displayDamage) {
        if (entity == null) {
            return;
        }
        pendingMeleeIndicators.computeIfAbsent(entity.getUniqueId(), ignored -> new java.util.ArrayDeque<>())
                .addLast(new MeleeIndicatorContext(critical, displayDamage));
    }

    private MeleeIndicatorContext pollMeleeIndicator(Entity entity) {
        if (entity == null) {
            return null;
        }
        java.util.ArrayDeque<MeleeIndicatorContext> queued = pendingMeleeIndicators.get(entity.getUniqueId());
        if (queued == null) {
            return null;
        }
        MeleeIndicatorContext context = queued.pollFirst();
        if (queued.isEmpty()) {
            pendingMeleeIndicators.remove(entity.getUniqueId());
        }
        return context;
    }

    private void applyNewbieKatanaPassives(Player attacker, CustomWeaponType weaponType) {
        if (weaponType != CustomWeaponType.NEWBIE_KATANA || attacker == null) {
            return;
        }

        double healAmount = 1.0D;
        if (plugin instanceof io.papermc.Grivience.GriviencePlugin gPlugin && gPlugin.getArmorSetBonusListener() != null) {
            if (gPlugin.getArmorSetBonusListener().hasFullSet(attacker, "rookie")) {
                healAmount = 2.0D;
            }
        }

        double maxHealth = attacker.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        attacker.setHealth(Math.min(maxHealth, attacker.getHealth() + healAmount));
    }

    private void applyWardensCleaverPassives(Player attacker, LivingEntity victim, CustomWeaponType weaponType, double finalDamage) {

        if (weaponType != CustomWeaponType.WARDENS_CLEAVER || attacker == null || victim == null || finalDamage <= 0.0D) {
            return;
        }

        // Bloodthirst
        double maxHealth = Math.max(1.0D, attacker.getMaxHealth());
        attacker.setHealth(Math.min(maxHealth, attacker.getHealth() + 2.0D));

        // Armor Break
        if (ThreadLocalRandom.current().nextDouble() < 0.15D) {
            applyArmorBreak(victim, 0.10D, WARDENS_CLEAVER_ARMOR_BREAK_TICKS);
            attacker.playSound(attacker.getLocation(), Sound.ITEM_AXE_STRIP, 0.55F, 1.25F);
        }

        // Cleave
        List<LivingEntity> secondaryTargets = new ArrayList<>();
        for (Entity nearby : victim.getWorld().getNearbyEntities(victim.getLocation(), WARDENS_CLEAVER_CLEAVE_RADIUS, 1.75D, WARDENS_CLEAVER_CLEAVE_RADIUS)) {
            if (!(nearby instanceof LivingEntity living)) {
                continue;
            }
            if (living.equals(victim) || living.equals(attacker) || living.isDead() || living.isInvulnerable()) {
                continue;
            }
            if (living instanceof Player otherPlayer) {
                if (isEndMinesPvpBlocked(attacker, otherPlayer) || isFriendly(attacker, otherPlayer)) {
                    continue;
                }
            }
            secondaryTargets.add(living);
        }
        secondaryTargets.sort((left, right) -> Double.compare(
                left.getLocation().distanceSquared(victim.getLocation()),
                right.getLocation().distanceSquared(victim.getLocation())
        ));

        if (secondaryTargets.isEmpty()) {
            return;
        }

        UUID attackerId = attacker.getUniqueId();
        abilityDamageContext.add(attackerId);
        try {
            int hitCount = 0;
            double cleaveDamage = Math.max(0.1D, finalDamage * 0.5D);
            for (LivingEntity target : secondaryTargets) {
                target.damage(cleaveDamage, attacker);
                hitCount++;
                if (hitCount >= 2) {
                    break;
                }
            }
        } finally {
            abilityDamageContext.remove(attackerId);
        }

        victim.getWorld().spawnParticle(Particle.SWEEP_ATTACK, victim.getLocation().add(0.0D, 1.0D, 0.0D), 1, 0.4D, 0.0D, 0.4D, 0.0D);
        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.7F, 0.9F);
    }

    private void applyArmorBreak(LivingEntity target, double percent, long durationTicks) {
        if (target == null || percent <= 0.0D || durationTicks <= 0L) {
            return;
        }

        if (target instanceof Player player) {
            player.getPersistentDataContainer().set(
                    SkyblockCombatStatsService.TEMP_DEFENSE_SHRED_PERCENT_KEY,
                    org.bukkit.persistence.PersistentDataType.DOUBLE,
                    clamp(percent, 0.0D, 0.95D)
            );
            player.getPersistentDataContainer().set(
                    SkyblockCombatStatsService.TEMP_DEFENSE_SHRED_UNTIL_KEY,
                    org.bukkit.persistence.PersistentDataType.LONG,
                    System.currentTimeMillis() + (durationTicks * 50L)
            );
            if (plugin instanceof GriviencePlugin grivience && grivience.getSkyblockCombatEngine() != null) {
                grivience.getSkyblockCombatEngine().refreshNow(player);
            }
            return;
        }

        AttributeInstance armor = target.getAttribute(Attribute.ARMOR);
        if (armor == null) {
            return;
        }

        removeModifier(armor, wardensArmorBreakKey);
        double currentArmor = armor.getValue();
        if (!Double.isFinite(currentArmor) || currentArmor <= 0.0D) {
            return;
        }

        double reduction = currentArmor * clamp(percent, 0.0D, 0.95D);
        if (!Double.isFinite(reduction) || reduction <= 0.0D) {
            return;
        }

        armor.addModifier(new AttributeModifier(
                wardensArmorBreakKey,
                -reduction,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.ANY
        ));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!target.isValid()) {
                return;
            }
            AttributeInstance current = target.getAttribute(Attribute.ARMOR);
            if (current != null) {
                removeModifier(current, wardensArmorBreakKey);
            }
        }, durationTicks);
    }

    private void removeModifier(AttributeInstance instance, NamespacedKey key) {
        if (instance == null || key == null) {
            return;
        }
        AttributeModifier toRemove = null;
        for (AttributeModifier modifier : instance.getModifiers()) {
            if (modifier != null && key.equals(modifier.getKey())) {
                toRemove = modifier;
                break;
            }
        }
        if (toRemove != null) {
            instance.removeModifier(toRemove);
        }
    }

    private boolean isUndead(Entity entity) {
        return switch (entity.getType()) {
            case ZOMBIE, ZOMBIE_VILLAGER, HUSK, DROWNED, ZOMBIE_HORSE, SKELETON, STRAY, WITHER_SKELETON, SKELETON_HORSE, PHANTOM, ZOMBIFIED_PIGLIN -> true;
            default -> false;
        };
    }

    private boolean isArthropod(Entity entity) {
        return switch (entity.getType()) {
            case SPIDER, CAVE_SPIDER, BEE, SILVERFISH, ENDERMITE -> true;
            default -> false;
        };
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack bow = event.getBow();
        if (bow == null) {
            return;
        }

        if (hasDragonTracker(bow) && event.getProjectile() instanceof AbstractArrow arrow) {
            applyDragonTracking(arrow);
        }

        if (!customItemService.isCustomDungeonWeapon(bow)) {
            return;
        }
        trackedProjectileWeapons.put(event.getProjectile().getUniqueId(), bow.clone());
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        UUID projectileId = event.getEntity().getUniqueId();
        if (event.getHitEntity() == null) {
            trackedProjectileWeapons.remove(projectileId);
            return;
        }
        // Delay cleanup by one tick so the matching damage event can still resolve the bow snapshot.
        Bukkit.getScheduler().runTask(plugin, () -> trackedProjectileWeapons.remove(projectileId));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInstantBowInteract(PlayerInteractEvent event) {
        if (!combatEnabled || !instantBowShotsEnabled) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action = event.getAction();
        boolean isRightClick = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
        boolean isLeftClick = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;

        if (!isRightClick && !isLeftClick) {
            return;
        }

        if ((action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_BLOCK) && 
                event.getClickedBlock() != null && event.getClickedBlock().getType().isInteractable()) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack bow = player.getInventory().getItemInMainHand();
        if (bow == null || bow.getType() != Material.BOW) {
            return;
        }

        CustomWeaponType weaponType = resolveWeaponType(bow);
        if (!isInstantBowWeapon(weaponType)) {
            return;
        }

        if (!supportsInstantBowAction(weaponType, action)) {
            return;
        }

        if (handleInstantBowShot(player, bow)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerAnimation(PlayerAnimationEvent event) {
        if (!combatEnabled || !instantBowShotsEnabled) {
            return;
        }
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack bow = player.getInventory().getItemInMainHand();
        if (bow == null || bow.getType() != Material.BOW) {
            return;
        }

        CustomWeaponType weaponType = resolveWeaponType(bow);
        if (!isInstantBowWeapon(weaponType)) {
            return;
        }

        // Only handle LEFT_CLICK_AIR behavior via animation, as LEFT_CLICK_BLOCK is handled by PlayerInteractEvent.
        // If it's a block click, PlayerInteractEvent will fire first and set the Material.BOW cooldown.
        if (!supportsInstantBowAction(weaponType, Action.LEFT_CLICK_AIR)) {
            return;
        }

        handleInstantBowShot(player, bow);
    }

    private boolean handleInstantBowShot(Player player, ItemStack bow) {
        if (player.hasCooldown(Material.BOW)) {
            return true;
        }

        AmmoSource ammo = findAmmo(player);
        if (ammo == null && player.getGameMode() != GameMode.CREATIVE) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7F, 0.7F);
            return true;
        }

        ItemStack ammoItem = ammo == null ? null : ammo.stack();
        
        CustomWeaponType weaponType = CustomItemService.getCustomWeaponType(bow);
        int shots = isInstantBowWeapon(weaponType) ? 3 : 1;
        boolean tracking = (weaponType == CustomWeaponType.DRAGON_HUNTER_SHORTBOW) || hasDragonTracker(bow);
        
        for (int i = 0; i < shots; i++) {
            AbstractArrow projectile = launchInstantArrow(player, bow, ammoItem);
            
            if (shots > 1) {
                Vector dir = projectile.getVelocity();
                if (i == 1) rotateVector(dir, 3.5);
                if (i == 2) rotateVector(dir, -3.5);
                projectile.setVelocity(dir);
            }

            if (tracking) {
                applyDragonTracking(projectile);
            }
            
            trackedProjectileWeapons.put(projectile.getUniqueId(), bow.clone());
        }

        if (shouldConsumeAmmo(player, bow, ammoItem)) {
            consumeAmmo(player, ammo);
        }
        if (instantBowCooldownTicks > 0) {
            player.setCooldown(Material.BOW, instantBowCooldownTicks);
        }
        return true;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (!abilitiesEnabled) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action = event.getAction();
        boolean rightClickAir = action == Action.RIGHT_CLICK_AIR;
        boolean rightClickBlock = action == Action.RIGHT_CLICK_BLOCK;
        if (!rightClickAir && !rightClickBlock) {
            return;
        }

        // Only block if it's a right-click on an actual interactable block like a chest, furnace, etc.
        // We allow abilities on things like Grass, Stone, etc. even if they are technically "interactable" in some contexts.
        if (rightClickBlock && event.getClickedBlock() != null) {
            Material type = event.getClickedBlock().getType();
            if (type.isInteractable() && !type.name().contains("STAIRS") && !type.name().contains("FENCE") && !type.name().contains("WALL")) {
                // If it's a real interactable (Chest, Door, etc.), don't trigger ability.
                return;
            }
        }

        if (rightClickBlock && !abilityRightClickBlocks) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            return;
        }

        CustomWeaponType weaponType = resolveWeaponType(item);
        if (weaponType == null) {
            return;
        }

        WeaponProfile profile = applyReforge(profile(weaponType), customItemService.reforgeOf(item), item);
        WeaponAbility ability = resolveAbility(weaponType, profile.defaultAbility());
        if (ability == null) {
            return;
        }

        event.setCancelled(true); // Cancel to prevent vanilla behavior (like blocking)

        long now = System.currentTimeMillis();
        long cooldownRemaining = cooldownRemainingMillis(player.getUniqueId(), weaponType, now);
        if (cooldownRemaining > 0L) {
            if (notifyCooldown) {
                player.sendMessage(ChatColor.RED + ability.name() + " is on cooldown for " + formatSeconds(cooldownRemaining) + "s.");
            }
            return;
        }

        SkyblockPlayerStats stats = combatEngine == null ? null : combatEngine.stats(player);
        double totalIntelligence = stats == null ? profile.intelligence() : stats.intelligence();
        double manaCost = ability.manaCost() * abilityManaScale;
        if (totalIntelligence != 0.0D) {
            double reductionFactor = 1.0D - (totalIntelligence / 400.0D);
            reductionFactor = Math.min(1.5D, Math.max(0.25D, reductionFactor));
            manaCost *= reductionFactor;
        }
        if (manaCost > 0.0D && !manaManager.consume(player, manaCost)) {
            player.sendMessage(ChatColor.RED + "Not enough mana! " + ChatColor.AQUA + "You need " + (int)Math.round(manaCost) + " Mana.");
            return;
        }

        long cooldownMillis = Math.max(500L, Math.round(ability.cooldownSeconds() * abilityCooldownScale * 1000.0D));
        setCooldown(player.getUniqueId(), weaponType, now + cooldownMillis);
        castAbility(player, profile, ability, manaCost, totalIntelligence);
    }

    private boolean isInstantBowWeapon(CustomWeaponType weaponType) {
        return weaponType == CustomWeaponType.TENGU_STORMBOW
                || weaponType == CustomWeaponType.KITSUNE_DAWNBOW
                || weaponType == CustomWeaponType.TENGU_SHORTBOW
                || weaponType == CustomWeaponType.KITSUNE_SHORTBOW
                || weaponType == CustomWeaponType.ONRYO_SHORTBOW
                || weaponType == CustomWeaponType.JOROGUMO_SHORTBOW
                || weaponType == CustomWeaponType.RAIJIN_SHORTBOW
                || weaponType == CustomWeaponType.DRAGON_HUNTER_SHORTBOW
                || weaponType == CustomWeaponType.VOIDSHOT_BOW;
    }

    private boolean supportsInstantBowAction(CustomWeaponType weaponType, Action action) {
        if (weaponType == CustomWeaponType.VOIDSHOT_BOW) {
            return action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
        }
        return true;
    }

    private AmmoSource findAmmo(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack offHand = inventory.getItemInOffHand();
        if (isArrowAmmo(offHand)) {
            return new AmmoSource(true, -1, offHand.clone());
        }

        ItemStack[] storage = inventory.getStorageContents();
        for (int slot = 0; slot < storage.length; slot++) {
            ItemStack stack = storage[slot];
            if (isArrowAmmo(stack)) {
                return new AmmoSource(false, slot, stack.clone());
            }
        }
        return null;
    }

    private boolean isArrowAmmo(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return false;
        }
        return stack.getType() == Material.ARROW
                || stack.getType() == Material.SPECTRAL_ARROW
                || stack.getType() == Material.TIPPED_ARROW;
    }

    private AbstractArrow launchInstantArrow(Player player, ItemStack bow, ItemStack ammoItem) {
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();
        if (direction.lengthSquared() < 1.0E-6D) {
            direction = new Vector(0.0D, 0.0D, 1.0D);
        }
        direction = direction.normalize();
        Location spawnLocation = eyeLocation.clone().add(direction.clone().multiply(0.35D));
        if (spawnLocation.getWorld() == null) {
            spawnLocation = player.getLocation().clone().add(0.0D, 1.5D, 0.0D);
        }

        AbstractArrow projectile;
        if (ammoItem != null && ammoItem.getType() == Material.SPECTRAL_ARROW) {
            projectile = spawnLocation.getWorld().spawn(spawnLocation, SpectralArrow.class, arrow -> {
                arrow.setShooter(player);
            });
        } else {
            Arrow arrow = spawnLocation.getWorld().spawn(spawnLocation, Arrow.class, spawned -> {
                spawned.setShooter(player);
            });
            if (ammoItem != null
                    && ammoItem.getType() == Material.TIPPED_ARROW
                    && ammoItem.hasItemMeta()
                    && ammoItem.getItemMeta() instanceof PotionMeta potionMeta) {
                if (potionMeta.getBasePotionType() != null) {
                    arrow.setBasePotionType(potionMeta.getBasePotionType());
                }
                for (PotionEffect effect : potionMeta.getCustomEffects()) {
                    arrow.addCustomEffect(effect, true);
                }
                if (potionMeta.hasColor()) {
                    arrow.setColor(potionMeta.getColor());
                }
            }
            projectile = arrow;
        }

        projectile.setVelocity(direction.multiply(instantBowVelocity));
        projectile.setCritical(true);

        int powerLevel = Math.max(0, bow.getEnchantmentLevel(Enchantment.POWER));
        double powerBonus = powerLevel > 0 ? (powerLevel * 0.5D) + 0.5D : 0.0D;
        projectile.setDamage(2.0D + powerBonus);
        projectile.setWeapon(bow.clone());
        if (bow.getEnchantmentLevel(Enchantment.FLAME) > 0) {
            projectile.setFireTicks(100);
        }

        if (isInfiniteShot(player, bow, ammoItem)) {
            projectile.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
        }
        player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.0F);
        return projectile;
    }

    private boolean shouldConsumeAmmo(Player player, ItemStack bow, ItemStack ammoItem) {
        return ammoItem != null && !isInfiniteShot(player, bow, ammoItem);
    }

    private boolean isInfiniteShot(Player player, ItemStack bow, ItemStack ammoItem) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }
        return ammoItem != null
                && ammoItem.getType() == Material.ARROW
                && bow.getEnchantmentLevel(Enchantment.INFINITY) > 0;
    }

    private void consumeAmmo(Player player, AmmoSource ammo) {
        if (ammo == null) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        if (ammo.offHand()) {
            inventory.setItemInOffHand(decrementStack(inventory.getItemInOffHand()));
            return;
        }
        inventory.setItem(ammo.slot(), decrementStack(inventory.getItem(ammo.slot())));
    }

    private ItemStack decrementStack(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return new ItemStack(Material.AIR);
        }
        if (stack.getAmount() <= 1) {
            return new ItemStack(Material.AIR);
        }
        ItemStack updated = stack.clone();
        updated.setAmount(stack.getAmount() - 1);
        return updated;
    }

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    private ItemStack resolveWeaponItem(Player player, Entity damager) {
        if (damager instanceof Trident trident) {
            return trident.getItemStack();
        }
        if (damager instanceof Projectile projectile) {
            ItemStack tracked = trackedProjectileWeapons.remove(projectile.getUniqueId());
            if (tracked != null) {
                return tracked;
            }
            if (projectile instanceof AbstractArrow arrow) {
                ItemStack weapon = arrow.getWeapon();
                if (weapon != null && !weapon.getType().isAir()) {
                    return weapon.clone();
                }
            }
        }
        return player.getInventory().getItemInMainHand();
    }

    private CustomWeaponType resolveWeaponType(ItemStack item) {
        String itemId = customItemService.itemId(item);
        if (itemId == null) {
            return null;
        }
        return CustomWeaponType.parse(itemId);
    }

    private long cooldownRemainingMillis(UUID playerId, CustomWeaponType type, long now) {
        EnumMap<CustomWeaponType, Long> cooldownByWeapon = abilityCooldowns.get(playerId);
        if (cooldownByWeapon == null) {
            return 0L;
        }
        long expiresAt = cooldownByWeapon.getOrDefault(type, 0L);
        return Math.max(0L, expiresAt - now);
    }

    private void setCooldown(UUID playerId, CustomWeaponType type, long expiresAt) {
        EnumMap<CustomWeaponType, Long> cooldownByWeapon = abilityCooldowns.computeIfAbsent(playerId, ignored -> new EnumMap<>(CustomWeaponType.class));
        cooldownByWeapon.put(type, expiresAt);
    }

    private void castAbility(Player player, WeaponProfile profile, WeaponAbility ability, double manaCostUsed, double totalIntelligence) {
        SkyblockPlayerStats stats = combatEngine == null ? null : combatEngine.stats(player);
        double totalStrength = stats == null ? profile.strength() : stats.strength();
        double supportAbilityValue = Math.max(
                2.0D,
                profile.flatDamage()
                        * abilityDamageScale
                        * ability.damageMultiplier()
                        * Math.max(0.2D, 1.0D + (Math.max(0.0D, totalStrength) * strengthScaling))
                        * Math.max(0.25D, 1.0D + (Math.max(0.0D, totalIntelligence) / 100.0D))
        );
        AbilityCastContext abilityContext = buildAbilityCastContext(player, profile, ability, totalIntelligence);
        player.sendMessage(ChatColor.AQUA + "Used " + ability.name() + ChatColor.GRAY + " (" + Math.round(manaCostUsed) + " mana)");

        switch (ability.effect()) {
            case DEMON_CRUSH -> {
                playAbilityCast(player, Sound.ENTITY_RAVAGER_ROAR, Particle.SWEEP_ATTACK);
                hitNearby(player, player.getLocation(), ability.radius(), abilityContext, ability.knockback(), target -> {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0, false, true, true));
                });
            }
            case WIND_SLASH -> {
                playAbilityCast(player, Sound.ENTITY_PHANTOM_FLAP, Particle.CLOUD);
                hitNearby(player, player.getLocation(), ability.radius(), abilityContext, ability.knockback() + 0.15D, null);
            }
            case TIDE_SURGE -> {
                playAbilityCast(player, Sound.ENTITY_DROWNED_SHOOT, Particle.SPLASH);
                hitNearby(player, player.getLocation(), ability.radius(), abilityContext, ability.knockback(), target -> {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 0, false, true, true));
                });
            }
            case SPIRIT_CUT -> {
                playAbilityCast(player, Sound.ENTITY_ALLAY_HURT, Particle.SOUL);
                hitNearby(player, player.getLocation(), ability.radius(), abilityContext, ability.knockback(), target -> {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0, false, true, true));
                });
            }
            case WEB_SNARE -> {
                playAbilityCast(player, Sound.ENTITY_SPIDER_AMBIENT, Particle.CLOUD);
                hitNearby(player, player.getLocation(), ability.radius(), abilityContext, ability.knockback(), target -> {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 1, false, true, true));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0, false, true, true));
                });
            }
            case FOXFIRE_BURST -> {
                playAbilityCast(player, Sound.ITEM_FIRECHARGE_USE, Particle.FLAME);
                hitNearby(player, player.getLocation(), ability.radius(), abilityContext, ability.knockback(), target -> {
                    target.setFireTicks(Math.max(target.getFireTicks(), 60));
                });
            }
            case BONE_CLEAVE -> {
                playAbilityCast(player, Sound.ENTITY_WITHER_SKELETON_AMBIENT, Particle.CRIT);
                hitNearby(player, player.getLocation(), ability.radius() + 0.5D, abilityContext.scale(1.15D), ability.knockback(), null);
            }
            case THUNDER_STEP -> {
                Vector horizontal = player.getLocation().getDirection().setY(0.0D);
                if (horizontal.lengthSquared() < 1.0E-6D) {
                    horizontal = new Vector(1.0D, 0.0D, 0.0D);
                }
                horizontal.normalize();
                player.setVelocity(horizontal.clone().multiply(1.35D).setY(0.35D));

                Location strike = player.getLocation().clone().add(horizontal.multiply(3.0D));
                if (strike.getWorld() != null) {
                    strike.getWorld().strikeLightningEffect(strike);
                    strike.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, strike, 30, 0.8D, 0.3D, 0.8D, 0.02D);
                }
                hitNearby(player, strike, ability.radius(), abilityContext.scale(1.25D), ability.knockback() + 0.15D, null);
                player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0F, 1.1F);
            }
            case RIFT_STEP -> {
                Location origin = player.getLocation().clone();
                Location destination = forwardTeleportLocation(player, 8.0D);
                teleportWithRiftEffects(player, origin, destination, Particle.PORTAL, Sound.ENTITY_ENDERMAN_TELEPORT);
                damageNearby(player, destination, Math.max(2.8D, ability.radius()), abilityContext, 0.28D, null);
            }
            case VOID_SHIFT -> {
                Location origin = player.getLocation().clone();
                Location destination = forwardTeleportLocation(player, 10.0D);
                teleportWithRiftEffects(player, origin, destination, Particle.REVERSE_PORTAL, Sound.ENTITY_ENDERMAN_TELEPORT);
                applyVoidShift(player);
                player.getWorld().spawnParticle(Particle.END_ROD, destination.clone().add(0.0D, 1.0D, 0.0D), 18, 0.35D, 0.45D, 0.35D, 0.02D);
            }
            case FRACTURE_DASH -> {
                int stacks = advanceFractureDashStacks(player);
                AbilityCastContext trailContext = buildAbilityCastContext(player, profile, totalIntelligence, 150.0D, false);
                Location origin = player.getLocation().clone();
                Location destination = forwardTeleportLocation(player, 12.0D);
                teleportWithRiftEffects(player, origin, destination, Particle.DRAGON_BREATH, Sound.ENTITY_ENDERMAN_TELEPORT);
                damageAlongPath(player, origin, destination, abilityContext, 1.3D, 0.30D);
                spawnRiftTrail(player, origin, destination, trailContext);
                player.sendActionBar(ChatColor.RED + "Fracture stacks: " + ChatColor.WHITE + stacks + ChatColor.DARK_GRAY + "/" + ChatColor.WHITE + MAX_FRACTURE_DASH_STACKS);
            }
            case DIMENSIONAL_COLLAPSE -> {
                Location origin = player.getLocation().clone();
                Location destination = forwardTeleportLocation(player, 14.0D);
                teleportWithRiftEffects(player, origin, destination, Particle.PORTAL, Sound.ENTITY_ENDERMAN_TELEPORT);
                List<LivingEntity> targets = getHostileTargets(player, destination, Math.max(4.5D, ability.radius()));
                AbilityCastContext empowered = abilityContext.scale(1.0D + (targets.size() * 0.05D));
                for (LivingEntity target : targets) {
                    pullTarget(target, destination, 0.90D);
                    damageTarget(player, target, empowered);
                }
                destination.getWorld().spawnParticle(Particle.DRAGON_BREATH, destination, 70, 1.6D, 0.7D, 1.6D, 0.03D);
                destination.getWorld().playSound(destination, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.9F, 1.3F);
            }
            case SHADOW_CHAIN -> {
                Location chainOrigin = player.getLocation().clone();
                List<LivingEntity> targets = findShadowChainTargets(player, chainOrigin, Math.max(10.0D, ability.radius()));
                double damageScale = 1.0D;
                for (LivingEntity target : targets) {
                    Location from = player.getLocation().clone();
                    Location to = safeLocationNear(target.getLocation(), player, 2.5D);
                    if (to != null) {
                        teleportWithRiftEffects(player, from, to, Particle.SMOKE, Sound.ENTITY_ENDERMAN_TELEPORT);
                    }
                    if (damageTarget(player, target, abilityContext.scale(damageScale))) {
                        target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0.0D, 1.0D, 0.0D), 1, 0.2D, 0.0D, 0.2D, 0.0D);
                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8F, 1.35F);
                    }
                    damageScale = Math.max(0.70D, damageScale - 0.10D);
                }
            }
            case WARP_SHOT -> launchWarpShot(player);
            case PIERCING_RIFT_ARROW -> {
                launchPiercingRiftArrow(player, abilityContext);
            }
            case ARROW_STORM -> {
                launchArrowStorm(player, abilityContext);
            }
            case GRAVITY_COLLAPSE -> {
                launchGravityCollapse(player, abilityContext, Math.max(4.0D, ability.radius()));
            }

            // Mage Weapon Abilities
            case ARCANE_BLAST -> {
                playAbilityCast(player, Sound.ENTITY_ENDER_DRAGON_GROWL, Particle.PORTAL);
                shootProjectile(player, abilityContext, ability.knockback(), Particle.PORTAL);
            }
            case FROST_NOVA -> {
                playAbilityCast(player, Sound.BLOCK_POWDER_SNOW_BREAK, Particle.SNOWFLAKE);
                hitNearby(player, player.getLocation(), ability.radius(), abilityContext, ability.knockback(), target -> {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1, false, true, true));
                });
                player.getWorld().spawnParticle(Particle.SNOWFLAKE, player.getLocation().add(0.0D, 0.5D, 0.0D), 50, 0.5D, 0.3D, 0.5D, 0.01D);
            }
            case INFERNO_BURST -> {
                playAbilityCast(player, Sound.ENTITY_BLAZE_SHOOT, Particle.FLAME);
                shootProjectile(player, abilityContext, ability.knockback(), Particle.FLAME);
                hitNearby(player, player.getLocation(), ability.radius() * 0.6D, abilityContext.scale(0.5D), 0.0D, target -> {
                    target.setFireTicks(Math.max(target.getFireTicks(), 100));
                });
            }
            case CHAIN_LIGHTNING -> {
                playAbilityCast(player, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, Particle.ELECTRIC_SPARK);
                List<LivingEntity> targets = getHostileTargets(player, player.getLocation(), ability.radius());
                int maxChains = Math.min(targets.size(), 5);
                for (int i = 0; i < maxChains; i++) {
                    LivingEntity target = targets.get(i);
                    if (damageTarget(player, target, abilityContext.scale(1.0D - (i * 0.15D)))) {
                        target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation(), 10, 0.3D, 0.3D, 0.3D, 0.01D);
                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5F, 1.2F);
                    }
                }
            }
            case VOID_RIFT -> {
                playAbilityCast(player, Sound.ENTITY_ENDERMAN_TELEPORT, Particle.DRAGON_BREATH);
                Location center = player.getLocation().add(player.getLocation().getDirection().multiply(3.0D));
                hitNearby(player, center, ability.radius(), abilityContext, 0.6D, target -> {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, false, true, true));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0, false, true, true));
                });
                player.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 40, 0.5D, 0.3D, 0.5D, 0.02D);
                player.getWorld().spawnParticle(Particle.PORTAL, center, 30, 0.6D, 0.4D, 0.6D, 0.03D);
            }
            case STARFALL -> {
                playAbilityCast(player, Sound.ENTITY_ENDER_DRAGON_GROWL, Particle.END_ROD);
                Location center = player.getLocation().add(player.getLocation().getDirection().multiply(4.0D));
                for (int i = 0; i < 5; i++) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        Location strikeLoc = center.clone().add(
                                ThreadLocalRandom.current().nextDouble(-3.0D, 3.0D),
                                0.0D,
                                ThreadLocalRandom.current().nextDouble(-3.0D, 3.0D)
                        );
                        if (strikeLoc.getWorld() != null) {
                            strikeLoc.getWorld().strikeLightningEffect(strikeLoc);
                            strikeLoc.getWorld().spawnParticle(Particle.END_ROD, strikeLoc, 20, 0.5D, 0.3D, 0.5D, 0.01D);
                            hitNearby(player, strikeLoc, 2.5D, abilityContext.scale(0.6D), 0.3D, null);
                        }
                    }, i * 10L);
                }
            }
            case FIREBALL -> {
                playAbilityCast(player, Sound.ENTITY_BLAZE_SHOOT, Particle.FLAME);
                shootProjectile(player, abilityContext, ability.knockback(), Particle.FLAME);
            }
            case ICE_SPIKE -> {
                playAbilityCast(player, Sound.BLOCK_POWDER_SNOW_BREAK, Particle.SNOWFLAKE);
                shootProjectile(player, abilityContext, ability.knockback(), Particle.SNOWFLAKE);
            }
            case THUNDER_STRIKE -> {
                playAbilityCast(player, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, Particle.ELECTRIC_SPARK);
                Location targetLoc = player.getLocation().add(player.getLocation().getDirection().multiply(5.0D));
                if (targetLoc.getWorld() != null) {
                    targetLoc.getWorld().strikeLightningEffect(targetLoc);
                    targetLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, targetLoc, 25, 0.4D, 0.3D, 0.4D, 0.01D);
                    hitNearby(player, targetLoc, 3.0D, abilityContext, 0.4D, null);
                }
            }
            case TOXIC_CLOUD -> {
                playAbilityCast(player, Sound.ENTITY_WITCH_AMBIENT, Particle.CAMPFIRE_COSY_SMOKE);
                Location center = player.getLocation().add(player.getLocation().getDirection().multiply(2.0D));
                hitNearby(player, center, ability.radius(), abilityContext, 0.2D, target -> {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 0, false, true, true));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 80, 0, false, true, true));
                });
                player.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, center, 40, 0.4D, 0.3D, 0.4D, 0.01D);
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, center, 30, 0.4D, 0.3D, 0.4D, 0.01D);
            }
            case HEAL -> {
                playAbilityCast(player, Sound.ENTITY_PLAYER_LEVELUP, Particle.HEART);
                double healAmount = supportAbilityValue * 0.8D + (ability.manaCost() * 0.5D);
                double newHealth = Math.min(player.getMaxHealth(), player.getHealth() + healAmount);
                player.setHealth(newHealth);
                player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0.0D, 1.0D, 0.0D), 20, 0.5D, 0.5D, 0.5D, 0.01D);
                player.sendMessage(ChatColor.GREEN + "Healed for " + Math.round(healAmount) + " health.");
            }
            case MASS_HEAL -> {
                playAbilityCast(player, Sound.BLOCK_BEACON_ACTIVATE, Particle.HEART);
                double intelligence = combatEngine == null ? 0.0D : combatEngine.stats(player).intelligence();
                double healAmount = 50.0D + (intelligence * 0.3D);
                for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), 8.0D, 8.0D, 8.0D)) {
                    if (entity instanceof Player ally && isFriendly(player, ally)) {
                        double newHealth = Math.min(ally.getMaxHealth(), ally.getHealth() + healAmount);
                        ally.setHealth(newHealth);
                        ally.getWorld().spawnParticle(Particle.HEART, ally.getLocation().add(0.0D, 1.0D, 0.0D), 15, 0.4D, 0.4D, 0.4D, 0.01D);
                        ally.sendMessage(ChatColor.GREEN + "Healed for " + Math.round(healAmount) + " health by " + player.getName() + ".");
                    }
                }
                double newHealth = Math.min(player.getMaxHealth(), player.getHealth() + healAmount);
                player.setHealth(newHealth);
                player.sendMessage(ChatColor.GREEN + "Mass heal restored " + Math.round(healAmount) + " health to nearby allies.");
            }
            case WITHER_STORM -> {
                playAbilityCast(player, Sound.ENTITY_WITHER_AMBIENT, Particle.SOUL);
                hitNearby(player, player.getLocation(), ability.radius(), abilityContext, ability.knockback(), target -> {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 120, 1, false, true, true));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 0, false, true, true));
                });
                player.getWorld().spawnParticle(Particle.SOUL, player.getLocation().add(0.0D, 0.5D, 0.0D), 50, 0.5D, 0.3D, 0.5D, 0.01D);
            }
            case REGENERATION_AURA -> {
                playAbilityCast(player, Sound.BLOCK_BEACON_ACTIVATE, Particle.HAPPY_VILLAGER);
                for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), 8.0D, 8.0D, 8.0D)) {
                    if (entity instanceof Player ally && isFriendly(player, ally)) {
                        ally.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1, false, true, true));
                        ally.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, ally.getLocation().add(0.0D, 1.0D, 0.0D), 15, 0.4D, 0.4D, 0.4D, 0.01D);
                        ally.sendMessage(ChatColor.AQUA + "Granted Regeneration by " + player.getName() + ".");
                    }
                }
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1, false, true, true));
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0.0D, 1.0D, 0.0D), 20, 0.4D, 0.4D, 0.4D, 0.01D);
                player.sendMessage(ChatColor.AQUA + "Regeneration Aura activated for 10 seconds.");
            }
        }
    }

    private AbilityCastContext buildAbilityCastContext(Player player, WeaponProfile profile, WeaponAbility ability, double totalIntelligence) {
        double baseDisplayDamage = abilityBaseDisplayDamage(profile, ability);
        return buildAbilityCastContext(player, profile, totalIntelligence, baseDisplayDamage, abilityConsumesStoredBonus(ability.effect()));
    }

    private AbilityCastContext buildAbilityCastContext(
            Player player,
            WeaponProfile profile,
            double totalIntelligence,
            double baseDisplayDamage,
            boolean consumeStoredBonus
    ) {
        if (baseDisplayDamage <= 0.0D) {
            return new AbilityCastContext(0.0D, false);
        }

        SkyblockPlayerStats stats = combatEngine == null ? null : combatEngine.stats(player);
        double totalStrength = stats == null ? profile.strength() : stats.strength();
        double critChancePercent = stats == null ? profile.critChancePercent() : stats.critChancePercent();
        double critDamagePercent = Math.max(0.0D, stats == null ? profile.critDamagePercent() : stats.critDamagePercent());
        double storedAbilityMultiplier = consumeStoredBonus ? consumeRiftstormAbilityMultiplier(player) : 1.0D;
        boolean critical = CombatDamageModel.isCriticalHit(critChancePercent, ThreadLocalRandom.current().nextDouble());
        CombatDamageModel.AttackResult attack = CombatDamageModel.computeAbilityAttack(
                baseDisplayDamage,
                totalStrength,
                totalIntelligence,
                critDamagePercent,
                critical,
                strengthScaling,
                abilityDamageScale,
                persistentDamageBuffMultiplier(player),
                storedAbilityMultiplier,
                dungeonDamageScale(player),
                1.0D
        );

        // Global Booster Integration
        if (plugin instanceof io.papermc.Grivience.GriviencePlugin griv && griv.getGlobalEventManager() != null) {
            attack = attack.scale(griv.getGlobalEventManager().getMultiplier(io.papermc.Grivience.event.GlobalEventManager.BoosterType.DAMAGE));
        }

        return new AbilityCastContext(
                Math.max(0.1D, attack.minecraftDamage()),
                attack.critical()
        );
    }

    private double abilityBaseDisplayDamage(WeaponProfile profile, WeaponAbility ability) {
        return switch (ability.effect()) {
            case VOID_SHIFT, WARP_SHOT, HEAL, MASS_HEAL, REGENERATION_AURA -> 0.0D;
            case RIFT_STEP -> 150.0D;
            case FRACTURE_DASH -> 350.0D;
            case DIMENSIONAL_COLLAPSE -> 800.0D;
            case SHADOW_CHAIN -> 220.0D;
            case GRAVITY_COLLAPSE -> 900.0D;
            default -> Math.max(0.0D, profile.flatDamage() * ability.damageMultiplier());
        };
    }

    private boolean abilityConsumesStoredBonus(AbilityEffect effect) {
        return switch (effect) {
            case VOID_SHIFT, WARP_SHOT, HEAL, MASS_HEAL, REGENERATION_AURA -> false;
            default -> true;
        };
    }

    private double persistentDamageBuffMultiplier(Player player) {
        if (player == null) {
            return 1.0D;
        }
        double multiplier = 1.0D;
        if (hasActiveVoidShift(player)) {
            multiplier += 0.40D;
        }
        multiplier += activeFractureDashStacks(player) * 0.10D;
        return multiplier;
    }

    private double targetIncomingDamageMultiplier(LivingEntity target) {
        if (target == null) {
            return 1.0D;
        }
        double multiplier = 1.0D + (activeVoidshotStacks(target) * 0.05D);
        
        if (target instanceof Player player && plugin instanceof GriviencePlugin gPlugin && gPlugin.getPetManager() != null) {
            multiplier *= gPlugin.getPetManager().getIncomingDamageMultiplier(player);
        }
        
        return multiplier;
    }

    private boolean hasActiveVoidShift(Player player) {
        Long expiresAt = voidShiftExpiries.get(player.getUniqueId());
        if (expiresAt == null || expiresAt < System.currentTimeMillis()) {
            voidShiftExpiries.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    private void applyVoidShift(Player player) {
        voidShiftExpiries.put(player.getUniqueId(), System.currentTimeMillis() + VOID_SHIFT_DURATION_MS);
        player.sendActionBar(ChatColor.LIGHT_PURPLE + "Void Shift active: " + ChatColor.RED + "+40% damage");
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.9F, 1.5F);
    }

    private int advanceFractureDashStacks(Player player) {
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        long expiresAt = fractureDashExpiries.getOrDefault(playerId, 0L);
        int nextStacks = expiresAt >= now ? fractureDashStacks.getOrDefault(playerId, 0) + 1 : 1;
        nextStacks = Math.min(MAX_FRACTURE_DASH_STACKS, Math.max(1, nextStacks));
        fractureDashStacks.put(playerId, nextStacks);
        fractureDashExpiries.put(playerId, now + FRACTURE_DASH_WINDOW_MS);
        return nextStacks;
    }

    private int activeFractureDashStacks(Player player) {
        UUID playerId = player.getUniqueId();
        long expiresAt = fractureDashExpiries.getOrDefault(playerId, 0L);
        if (expiresAt < System.currentTimeMillis()) {
            fractureDashStacks.remove(playerId);
            fractureDashExpiries.remove(playerId);
            return 0;
        }
        return fractureDashStacks.getOrDefault(playerId, 0);
    }

    private double consumeRiftstormAbilityMultiplier(Player player) {
        int stacks = riftstormAbilityStacks.remove(player.getUniqueId());
        return 1.0D + (Math.min(MAX_RIFTSTORM_ABILITY_STACKS, Math.max(0, stacks)) * 0.10D);
    }

    private void incrementRiftstormAbilityStacks(Player player) {
        UUID playerId = player.getUniqueId();
        int next = Math.min(MAX_RIFTSTORM_ABILITY_STACKS, riftstormAbilityStacks.getOrDefault(playerId, 0) + 1);
        riftstormAbilityStacks.put(playerId, next);
        player.sendActionBar(ChatColor.GOLD + "Arrow Storm charge: " + ChatColor.WHITE + next + ChatColor.DARK_GRAY + "/" + ChatColor.WHITE + MAX_RIFTSTORM_ABILITY_STACKS);
    }

    private int activeVoidshotStacks(LivingEntity target) {
        UUID targetId = target.getUniqueId();
        long expiresAt = voidshotDebuffExpiries.getOrDefault(targetId, 0L);
        if (expiresAt < System.currentTimeMillis()) {
            voidshotDebuffStacks.remove(targetId);
            voidshotDebuffExpiries.remove(targetId);
            return 0;
        }
        return voidshotDebuffStacks.getOrDefault(targetId, 0);
    }

    private void applyVoidshotDebuff(LivingEntity target) {
        UUID targetId = target.getUniqueId();
        int nextStacks = Math.min(MAX_VOIDSHOT_STACKS, activeVoidshotStacks(target) + 1);
        voidshotDebuffStacks.put(targetId, nextStacks);
        voidshotDebuffExpiries.put(targetId, System.currentTimeMillis() + VOIDSHOT_DEBUFF_DURATION_MS);
        target.getWorld().spawnParticle(Particle.REVERSE_PORTAL, target.getLocation().add(0.0D, 1.0D, 0.0D), 14, 0.25D, 0.35D, 0.25D, 0.02D);
    }

    private int damageNearby(
            Player caster,
            Location center,
            double radius,
            AbilityCastContext context,
            double knockback,
            TargetEffect effect
    ) {
        int hits = 0;
        for (LivingEntity target : getHostileTargets(caster, center, radius)) {
            if (damageTarget(caster, target, context)) {
                hits++;
                if (effect != null) {
                    effect.apply(target);
                }
                if (knockback > 0.0D) {
                    Vector push = target.getLocation().toVector().subtract(center.toVector());
                    if (push.lengthSquared() > 1.0E-6D) {
                        target.setVelocity(target.getVelocity().add(push.normalize().multiply(knockback).setY(0.15D)));
                    }
                }
            }
        }
        return hits;
    }

    private boolean damageTarget(Player caster, LivingEntity target, AbilityCastContext context) {
        if (caster == null || target == null || context == null || context.damage() <= 0.0D) {
            return false;
        }
        if (!isHostileTarget(caster, target)) {
            return false;
        }

        double vulnerabilityMultiplier = targetIncomingDamageMultiplier(target);
        AbilityCastContext appliedContext = context.scale(vulnerabilityMultiplier);
        double actualDamage = Math.max(0.1D, appliedContext.damage());
        double healthBefore = Math.max(0.0D, target.getHealth());
        UUID casterId = caster.getUniqueId();
        abilityDamageContext.add(casterId);
        try {
            target.damage(actualDamage, caster);
        } finally {
            abilityDamageContext.remove(casterId);
        }
        double healthAfter = target.isDead() ? 0.0D : Math.max(0.0D, target.getHealth());
        double appliedMinecraftDamage = Math.max(0.0D, healthBefore - healthAfter);
        if (appliedMinecraftDamage <= 0.0D) {
            return false;
        }

        double indicatorDamage = CombatDamageModel.displayDamageFromAppliedMcDamage(appliedMinecraftDamage, abilityDamageScale);
        if (indicatorDamage > 0.0D) {
            DamageIndicatorUtil.spawn((GriviencePlugin) plugin, target, indicatorDamage, appliedContext.critical());
        }
        return true;
    }

    private int damageAlongPath(Player caster, Location start, Location end, AbilityCastContext context, double radius, double knockback) {
        if (start.getWorld() == null || end.getWorld() == null || !start.getWorld().equals(end.getWorld())) {
            return 0;
        }
        Set<UUID> hitTargets = new HashSet<>();
        Vector path = end.toVector().subtract(start.toVector());
        int segments = Math.max(1, (int) Math.ceil(Math.max(1.0D, start.distance(end)) * 2.0D));
        int hits = 0;
        for (int index = 0; index <= segments; index++) {
            Location point = start.clone().add(path.clone().multiply(index / (double) segments));
            point.getWorld().spawnParticle(Particle.DRAGON_BREATH, point.clone().add(0.0D, 0.35D, 0.0D), 4, 0.14D, 0.18D, 0.14D, 0.01D);
            for (Entity entity : point.getWorld().getNearbyEntities(point, radius, radius, radius)) {
                if (!(entity instanceof LivingEntity target) || !hitTargets.add(target.getUniqueId())) {
                    continue;
                }
                if (!damageTarget(caster, target, context)) {
                    continue;
                }
                hits++;
                if (knockback > 0.0D && path.lengthSquared() > 1.0E-6D) {
                    target.setVelocity(target.getVelocity().add(path.clone().normalize().multiply(knockback).setY(0.12D)));
                }
            }
        }
        return hits;
    }

    private void spawnRiftTrail(Player caster, Location start, Location end, AbilityCastContext context) {
        damageAlongPath(caster, start, end, context, 1.0D, 0.0D);
        if (start.getWorld() == null || end.getWorld() == null || !start.getWorld().equals(end.getWorld())) {
            return;
        }
        Vector path = end.toVector().subtract(start.toVector());
        int segments = Math.max(1, (int) Math.ceil(Math.max(1.0D, start.distance(end)) * 2.0D));
        for (int index = 0; index <= segments; index++) {
            Location point = start.clone().add(path.clone().multiply(index / (double) segments));
            point.getWorld().spawnParticle(Particle.REVERSE_PORTAL, point.clone().add(0.0D, 0.2D, 0.0D), 6, 0.18D, 0.12D, 0.18D, 0.01D);
            point.getWorld().spawnParticle(Particle.END_ROD, point.clone().add(0.0D, 0.3D, 0.0D), 2, 0.08D, 0.08D, 0.08D, 0.01D);
        }
    }

    private List<LivingEntity> findShadowChainTargets(Player caster, Location center, double radius) {
        List<LivingEntity> remaining = getHostileTargets(caster, center, radius);
        List<LivingEntity> chain = new ArrayList<>();
        Location pivot = center.clone();
        while (!remaining.isEmpty() && chain.size() < 4) {
            Location currentPivot = pivot;
            remaining.sort((left, right) -> Double.compare(
                    safeDistanceSquared(left.getLocation(), currentPivot),
                    safeDistanceSquared(right.getLocation(), currentPivot)
            ));
            LivingEntity next = remaining.remove(0);
            chain.add(next);
            pivot = next.getLocation();
        }
        return chain;
    }

    private List<LivingEntity> getHostileTargets(Player caster, Location center, double radius) {
        List<LivingEntity> targets = new ArrayList<>();
        if (center == null || center.getWorld() == null) {
            return targets;
        }
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof LivingEntity target && isHostileTarget(caster, target)) {
                targets.add(target);
            }
        }
        return targets;
    }

    private boolean isHostileTarget(Player caster, LivingEntity target) {
        if (target == null || caster == null || target.equals(caster) || target.isDead() || target.isInvulnerable()) {
            return false;
        }
        if (target instanceof Player otherPlayer) {
            if (otherPlayer.getGameMode().isInvulnerable()) {
                return false;
            }
            if (isEndMinesPvpBlocked(caster, otherPlayer) || isFriendly(caster, otherPlayer)) {
                return false;
            }
        }
        return true;
    }

    private void teleportWithRiftEffects(Player player, Location origin, Location destination, Particle particle, Sound sound) {
        if (origin.getWorld() != null) {
            origin.getWorld().spawnParticle(particle, origin.clone().add(0.0D, 1.0D, 0.0D), 20, 0.35D, 0.45D, 0.35D, 0.02D);
            origin.getWorld().playSound(origin, sound, 0.8F, 1.2F);
        }
        if (destination == null) {
            return;
        }
        player.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
        player.teleport(destination, PlayerTeleportEvent.TeleportCause.PLUGIN);
        player.setFallDistance(0.0F);
        if (destination.getWorld() != null) {
            destination.getWorld().spawnParticle(particle, destination.clone().add(0.0D, 1.0D, 0.0D), 24, 0.35D, 0.45D, 0.35D, 0.02D);
            destination.getWorld().playSound(destination, sound, 0.8F, 1.35F);
        }
    }

    private Location forwardTeleportLocation(Player player, double maxDistance) {
        Location origin = player.getLocation().clone();
        Vector direction = origin.getDirection();
        if (direction.lengthSquared() < 1.0E-6D) {
            return origin;
        }
        direction.normalize();
        Location best = origin;
        for (double distance = 0.5D; distance <= maxDistance; distance += 0.5D) {
            Location sample = origin.clone().add(direction.clone().multiply(distance));
            Location safe = safeLocationNear(sample, player, 1.5D);
            if (safe == null) {
                break;
            }
            best = safe;
        }
        return best;
    }

    private Location safeLocationNear(Location center, Player player, double radius) {
        if (center == null || center.getWorld() == null) {
            return null;
        }
        int[] yOffsets = {0, 1, -1, 2, -2};
        int searchRadius = Math.max(0, (int) Math.ceil(radius));
        for (int dy : yOffsets) {
            for (int dx = -searchRadius; dx <= searchRadius; dx++) {
                for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                    Location candidate = new Location(
                            center.getWorld(),
                            center.getBlockX() + dx + 0.5D,
                            center.getBlockY() + dy,
                            center.getBlockZ() + dz + 0.5D,
                            player.getLocation().getYaw(),
                            player.getLocation().getPitch()
                    );
                    if (isSafeStandingLocation(candidate)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private boolean isSafeStandingLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        Material feet = location.getBlock().getType();
        Material head = location.clone().add(0.0D, 1.0D, 0.0D).getBlock().getType();
        Material ground = location.clone().add(0.0D, -1.0D, 0.0D).getBlock().getType();
        return location.getBlock().isPassable()
                && location.clone().add(0.0D, 1.0D, 0.0D).getBlock().isPassable()
                && ground.isSolid()
                && ground != Material.LAVA
                && ground != Material.MAGMA_BLOCK
                && ground != Material.CAMPFIRE
                && feet != Material.LAVA
                && head != Material.LAVA;
    }

    private void pullTarget(LivingEntity target, Location center, double strength) {
        Vector pull = center.toVector().subtract(target.getLocation().toVector());
        if (pull.lengthSquared() < 1.0E-6D) {
            return;
        }
        target.setVelocity(target.getVelocity().add(pull.normalize().multiply(strength).setY(0.12D)));
    }

    private void launchWarpShot(Player player) {
        playAbilityCast(player, Sound.ITEM_CHORUS_FRUIT_TELEPORT, Particle.PORTAL);
        Location start = player.getEyeLocation().clone();
        Vector velocity = start.getDirection().normalize().multiply(0.95D);
        travelAbilityProjectile(player, start, velocity, 22, Particle.END_ROD, 0.55D, new HashSet<>(), new AbilityProjectileHandler() {
            @Override
            public boolean onEntityImpact(LivingEntity target, Location impact) {
                Location safe = safeLocationNear(target.getLocation(), player, 2.5D);
                if (safe == null) {
                    safe = player.getLocation().clone();
                }
                teleportWithRiftEffects(player, player.getLocation().clone(), safe, Particle.PORTAL, Sound.ENTITY_ENDERMAN_TELEPORT);
                return true;
            }

            @Override
            public boolean onBlockImpact(Location impact) {
                Location safe = safeLocationNear(impact, player, 2.5D);
                if (safe == null) {
                    safe = player.getLocation().clone();
                }
                teleportWithRiftEffects(player, player.getLocation().clone(), safe, Particle.PORTAL, Sound.ENTITY_ENDERMAN_TELEPORT);
                return true;
            }
        });
    }

    private void launchPiercingRiftArrow(Player player, AbilityCastContext context) {
        playAbilityCast(player, Sound.ENTITY_ARROW_SHOOT, Particle.REVERSE_PORTAL);
        Location start = player.getEyeLocation().clone();
        Vector velocity = start.getDirection().normalize().multiply(1.05D);
        travelAbilityProjectile(player, start, velocity, 26, Particle.REVERSE_PORTAL, 0.60D, new HashSet<>(), new AbilityProjectileHandler() {
            @Override
            public boolean onEntityImpact(LivingEntity target, Location impact) {
                if (damageTarget(player, target, context)) {
                    applyVoidshotDebuff(target);
                    target.getWorld().spawnParticle(Particle.PORTAL, impact, 18, 0.18D, 0.18D, 0.18D, 0.02D);
                }
                return false;
            }

            @Override
            public boolean onBlockImpact(Location impact) {
                impact.getWorld().spawnParticle(Particle.PORTAL, impact, 18, 0.25D, 0.25D, 0.25D, 0.02D);
                impact.getWorld().playSound(impact, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.7F, 1.4F);
                return true;
            }
        });
    }

    private void launchArrowStorm(Player player, AbilityCastContext context) {
        playAbilityCast(player, Sound.ENTITY_ARROW_SHOOT, Particle.CRIT);
        Location start = player.getEyeLocation().clone();
        Vector baseDirection = start.getDirection().normalize();
        double[] offsets = {-12.0D, -6.0D, 0.0D, 6.0D, 12.0D};
        for (double offset : offsets) {
            Vector velocity = rotateAroundY(baseDirection.clone(), Math.toRadians(offset)).multiply(1.0D);
            travelAbilityProjectile(player, start.clone(), velocity, 20, Particle.CRIT, 0.55D, new HashSet<>(), new AbilityProjectileHandler() {
                @Override
                public boolean onEntityImpact(LivingEntity target, Location impact) {
                    if (damageTarget(player, target, context)) {
                        incrementRiftstormAbilityStacks(player);
                        target.getWorld().spawnParticle(Particle.CRIT, impact, 12, 0.15D, 0.15D, 0.15D, 0.01D);
                    }
                    return true;
                }

                @Override
                public boolean onBlockImpact(Location impact) {
                    impact.getWorld().spawnParticle(Particle.CRIT, impact, 8, 0.12D, 0.12D, 0.12D, 0.01D);
                    return true;
                }
            });
        }
    }

    private void launchGravityCollapse(Player player, AbilityCastContext context, double radius) {
        playAbilityCast(player, Sound.ENTITY_ARROW_SHOOT, Particle.DRAGON_BREATH);
        Location start = player.getEyeLocation().clone();
        Vector velocity = start.getDirection().normalize().multiply(0.95D);
        travelAbilityProjectile(player, start, velocity, 24, Particle.DRAGON_BREATH, 0.60D, new HashSet<>(), new AbilityProjectileHandler() {
            @Override
            public boolean onEntityImpact(LivingEntity target, Location impact) {
                triggerGravityCollapse(player, impact, context, radius);
                return true;
            }

            @Override
            public boolean onBlockImpact(Location impact) {
                triggerGravityCollapse(player, impact, context, radius);
                return true;
            }
        });
    }

    private void triggerGravityCollapse(Player player, Location impact, AbilityCastContext context, double radius) {
        if (impact.getWorld() == null) {
            return;
        }
        List<LivingEntity> targets = getHostileTargets(player, impact, radius);
        for (LivingEntity target : targets) {
            pullTarget(target, impact, 1.10D);
        }
        impact.getWorld().spawnParticle(Particle.DRAGON_BREATH, impact, 80, 1.3D, 0.6D, 1.3D, 0.03D);
        impact.getWorld().spawnParticle(Particle.REVERSE_PORTAL, impact, 50, 1.0D, 0.4D, 1.0D, 0.02D);
        impact.getWorld().playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 0.75F);
        for (LivingEntity target : targets) {
            damageTarget(player, target, context);
        }
    }

    private void travelAbilityProjectile(
            Player caster,
            Location current,
            Vector velocity,
            int ticksRemaining,
            Particle particle,
            double hitRadius,
            Set<UUID> hitTargets,
            AbilityProjectileHandler handler
    ) {
        if (ticksRemaining <= 0 || caster == null || !caster.isOnline() || current.getWorld() == null) {
            return;
        }

        for (int step = 0; step < 2; step++) {
            current.add(velocity);
            if (current.getWorld() == null) {
                return;
            }
            current.getWorld().spawnParticle(particle, current, 6, 0.10D, 0.10D, 0.10D, 0.01D);
            if (!current.getBlock().isPassable()) {
                if (handler.onBlockImpact(current.clone())) {
                    return;
                }
            }
            for (Entity entity : current.getWorld().getNearbyEntities(current, hitRadius, hitRadius, hitRadius)) {
                if (!(entity instanceof LivingEntity target) || target.equals(caster) || !hitTargets.add(target.getUniqueId())) {
                    continue;
                }
                if (!isHostileTarget(caster, target)) {
                    continue;
                }
                if (handler.onEntityImpact(target, current.clone())) {
                    return;
                }
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () ->
                travelAbilityProjectile(caster, current, velocity, ticksRemaining - 1, particle, hitRadius, hitTargets, handler), 1L);
    }

    private Vector rotateAroundY(Vector vector, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double x = vector.getX() * cos - vector.getZ() * sin;
        double z = vector.getX() * sin + vector.getZ() * cos;
        return new Vector(x, vector.getY(), z);
    }

    private double safeDistanceSquared(Location first, Location second) {
        if (first == null || second == null || first.getWorld() == null || second.getWorld() == null || !first.getWorld().equals(second.getWorld())) {
            return Double.MAX_VALUE;
        }
        return first.distanceSquared(second);
    }

    private void playAbilityCast(Player player, Sound sound, Particle particle) {
        player.playSound(player.getLocation(), sound, 0.95F, 1.0F);
        player.getWorld().spawnParticle(particle, player.getLocation().add(0.0D, 1.1D, 0.0D), 20, 0.45D, 0.25D, 0.45D, 0.02D);
    }

    private void hitNearby(
            Player caster,
            Location center,
            double radius,
            AbilityCastContext context,
            double knockback,
            TargetEffect effect
    ) {
        if (center == null || center.getWorld() == null) {
            return;
        }
        damageNearby(caster, center, radius, context, knockback, effect);
    }

    private void shootProjectile(Player player, AbilityCastContext context, double knockback, Particle particle) {
        Location loc = player.getLocation().add(0.0D, 1.5D, 0.0D);
        Vector direction = loc.getDirection();

        shootProjectileTask(player, loc.clone(), direction.clone(), context, knockback, particle, 0);
    }

    private void shootProjectileTask(Player player, Location current, Vector vel, AbilityCastContext context, double knockback, Particle particle, int ticks) {
        if (ticks >= 100) {
            return;
        }

        current.add(vel);
        player.getWorld().spawnParticle(particle, current, 5, 0.1D, 0.1D, 0.1D, 0.01D);

        // Check for hits
        for (Entity entity : player.getWorld().getNearbyEntities(current, 0.8D, 0.8D, 0.8D)) {
            if (entity instanceof LivingEntity target && damageTarget(player, target, context)) {
                Vector push = target.getLocation().toVector().subtract(current.toVector());
                if (push.lengthSquared() > 1.0E-6D) {
                    push.normalize().multiply(knockback).setY(0.15D);
                    target.setVelocity(target.getVelocity().add(push));
                }

                player.getWorld().spawnParticle(Particle.CRIT, current, 20, 0.3D, 0.3D, 0.3D, 0.02D);
                player.getWorld().playSound(current, Sound.ENTITY_GENERIC_EXPLODE, 0.8F, 1.0F);
                return;
            }
        }

        // Check for block collision
        if (!current.getBlock().getType().isAir()) {
            player.getWorld().spawnParticle(Particle.SMOKE, current, 15, 0.2D, 0.2D, 0.2D, 0.01D);
            player.getWorld().playSound(current, Sound.BLOCK_FIRE_EXTINGUISH, 0.7F, 1.0F);
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () ->
                shootProjectileTask(player, current, vel, context, knockback, particle, ticks + 1), 1L);
    }

    private WeaponAbility resolveAbility(CustomWeaponType type, WeaponAbility defaults) {
        if (defaults == null) {
            return null;
        }
        String path = "custom-items.combat.mana-abilities.weapons." + type.name().toLowerCase(Locale.ROOT);
        double manaCost = sanitizePositive(plugin.getConfig().getDouble(path + ".mana-cost", defaults.manaCost()), defaults.manaCost());
        double cooldownSeconds = sanitizePositive(plugin.getConfig().getDouble(path + ".cooldown-seconds", defaults.cooldownSeconds()), defaults.cooldownSeconds());
        double damageMultiplier = sanitizePositive(plugin.getConfig().getDouble(path + ".damage-multiplier", defaults.damageMultiplier()), defaults.damageMultiplier());
        double radius = sanitizePositive(plugin.getConfig().getDouble(path + ".radius", defaults.radius()), defaults.radius());
        double knockback = sanitizePositive(plugin.getConfig().getDouble(path + ".knockback", defaults.knockback()), defaults.knockback());
        return new WeaponAbility(defaults.name(), defaults.effect(), manaCost, cooldownSeconds, damageMultiplier, radius, knockback);
    }

    private WeaponProfile profile(CustomWeaponType type) {
        CustomWeaponProfiles.StatProfile base = CustomWeaponProfiles.stats(type);
        WeaponAbility ability = defaultAbility(type);
        return new WeaponProfile(
                base.flatDamage(),
                base.strength(),
                base.critChancePercent(),
                base.critDamagePercent(),
                base.attackSpeedPercent(),
                base.intelligence(),
                ability
        );
    }

    private WeaponAbility defaultAbility(CustomWeaponType type) {
        if (type == null) {
            return null;
        }

        return switch (type) {
            case ONI_CLEAVER -> new WeaponAbility("Demon Crush", AbilityEffect.DEMON_CRUSH, 40.0D, 8.0D, 1.00D, 3.6D, 0.45D);
            case TENGU_GALEBLADE -> new WeaponAbility("Wind Slash", AbilityEffect.WIND_SLASH, 32.0D, 6.0D, 0.95D, 4.0D, 0.52D);
            case KAPPA_TIDEBREAKER -> new WeaponAbility("Tide Surge", AbilityEffect.TIDE_SURGE, 36.0D, 7.0D, 1.05D, 3.8D, 0.40D);
            case ONRYO_SPIRITBLADE -> new WeaponAbility("Wraith Cut", AbilityEffect.SPIRIT_CUT, 46.0D, 9.0D, 1.10D, 3.7D, 0.42D);
            case JOROGUMO_STINGER -> new WeaponAbility("Silk Snare", AbilityEffect.WEB_SNARE, 28.0D, 6.0D, 0.85D, 3.5D, 0.30D);
            case KITSUNE_FANG -> new WeaponAbility("Foxfire Burst", AbilityEffect.FOXFIRE_BURST, 40.0D, 8.0D, 1.00D, 4.2D, 0.36D);
            case GASHADOKURO_NODACHI -> new WeaponAbility("Bone Cleave", AbilityEffect.BONE_CLEAVE, 58.0D, 11.0D, 1.20D, 4.1D, 0.52D);
            case FLYING_RAIJIN -> new WeaponAbility("Thunder Step", AbilityEffect.THUNDER_STEP, 75.0D, 12.0D, 1.25D, 4.2D, 0.58D);
            case HAYABUSA_KATANA -> new WeaponAbility("Aerial Dash", AbilityEffect.WIND_SLASH, 42.0D, 6.5D, 1.05D, 3.5D, 0.35D);
            case RIFTBLADE -> new WeaponAbility("Rift Step", AbilityEffect.RIFT_STEP, 20.0D, 2.0D, 1.00D, 3.0D, 0.30D);
            case VOID_ASPECT_BLADE -> new WeaponAbility("Void Shift", AbilityEffect.VOID_SHIFT, 40.0D, 2.0D, 1.00D, 0.0D, 0.0D);
            case RIFTBREAKER -> new WeaponAbility("Fracture Dash", AbilityEffect.FRACTURE_DASH, 60.0D, 1.0D, 1.00D, 12.0D, 0.30D);
            case SOVEREIGN_ASPECT -> new WeaponAbility("Dimensional Collapse", AbilityEffect.DIMENSIONAL_COLLAPSE, 120.0D, 3.0D, 1.00D, 4.5D, 0.0D);
            case VOIDFANG_DAGGER -> new WeaponAbility("Shadow Chain", AbilityEffect.SHADOW_CHAIN, 50.0D, 3.0D, 1.00D, 12.0D, 0.0D);
            case WARP_BOW -> new WeaponAbility("Warp Shot", AbilityEffect.WARP_SHOT, 30.0D, 2.0D, 1.00D, 0.0D, 0.0D);
            case VOIDSHOT_BOW -> new WeaponAbility("Piercing Rift Arrow", AbilityEffect.PIERCING_RIFT_ARROW, 40.0D, 1.0D, 1.00D, 0.0D, 0.0D);
            case RIFTSTORM_BOW -> new WeaponAbility("Arrow Storm", AbilityEffect.ARROW_STORM, 70.0D, 2.0D, 1.20D, 0.0D, 0.0D);
            case ORBITAL_LONGBOW -> new WeaponAbility("Gravity Collapse", AbilityEffect.GRAVITY_COLLAPSE, 120.0D, 4.0D, 1.00D, 4.5D, 0.0D);

            // Mage Weapons - Staffs
            case ARCANE_STAFF -> new WeaponAbility("Arcane Blast", AbilityEffect.ARCANE_BLAST, 35.0D, 5.0D, 1.10D, 4.5D, 0.40D);
            case FROSTBITE_STAFF -> new WeaponAbility("Frost Nova", AbilityEffect.FROST_NOVA, 45.0D, 7.0D, 1.15D, 5.0D, 0.35D);
            case INFERNO_STAFF -> new WeaponAbility("Inferno Burst", AbilityEffect.INFERNO_BURST, 55.0D, 8.0D, 1.25D, 4.8D, 0.38D);
            case STORMCALLER_STAFF -> new WeaponAbility("Chain Lightning", AbilityEffect.CHAIN_LIGHTNING, 50.0D, 6.0D, 1.20D, 5.5D, 0.30D);
            case VOIDWALKER_STAFF -> new WeaponAbility("Void Rift", AbilityEffect.VOID_RIFT, 65.0D, 10.0D, 1.35D, 4.5D, 0.50D);
            case CELESTIAL_STAFF -> new WeaponAbility("Starfall", AbilityEffect.STARFALL, 80.0D, 14.0D, 1.50D, 6.0D, 0.40D);

            // Mage Weapons - Wands
            case FLAME_WAND -> new WeaponAbility("Fireball", AbilityEffect.FIREBALL, 25.0D, 4.0D, 0.90D, 3.5D, 0.35D);
            case ICE_WAND -> new WeaponAbility("Ice Spike", AbilityEffect.ICE_SPIKE, 22.0D, 3.5D, 0.85D, 4.0D, 0.30D);
            case LIGHTNING_WAND -> new WeaponAbility("Thunder Strike", AbilityEffect.THUNDER_STRIKE, 28.0D, 4.5D, 0.95D, 3.8D, 0.32D);
            case POISON_WAND -> new WeaponAbility("Toxic Cloud", AbilityEffect.TOXIC_CLOUD, 20.0D, 5.0D, 0.80D, 4.2D, 0.25D);
            case HEALING_WAND -> new WeaponAbility("Heal", AbilityEffect.HEAL, 30.0D, 6.0D, 0.0D, 0.0D, 0.0D);

            // Mage Weapons - Scepters
            case SCEPTER_OF_HEALING -> new WeaponAbility("Mass Heal", AbilityEffect.MASS_HEAL, 50.0D, 12.0D, 0.0D, 0.0D, 0.0D);
            case SCEPTER_OF_DECAY -> new WeaponAbility("Wither Storm", AbilityEffect.WITHER_STORM, 45.0D, 8.0D, 1.10D, 5.5D, 0.30D);
            case SCEPTER_OF_MENDING -> new WeaponAbility("Regeneration Aura", AbilityEffect.REGENERATION_AURA, 40.0D, 10.0D, 0.0D, 0.0D, 0.0D);
            default -> null;
        };
    }

    private WeaponProfile vanillaProfile(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return new WeaponProfile(1.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, null);
        }

        double damage = switch (item.getType()) {
            case WOODEN_SWORD, GOLDEN_SWORD -> 4.0D;
            case STONE_SWORD -> 5.0D;
            case IRON_SWORD -> 6.0D;
            case DIAMOND_SWORD -> 7.0D;
            case NETHERITE_SWORD -> 8.0D;
            
            case WOODEN_AXE, GOLDEN_AXE -> 7.0D;
            case STONE_AXE, IRON_AXE, DIAMOND_AXE -> 9.0D;
            case NETHERITE_AXE -> 10.0D;
            
            case TRIDENT -> 9.0D;
            
            default -> 1.0D;
        };

        return new WeaponProfile(damage, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, null);
    }

    private WeaponProfile applyReforge(WeaponProfile profile, ReforgeType reforgeType, ItemStack weapon) {
        if (profile == null || reforgeType == null) {
            return profile;
        }
        CustomItemService.ReforgeStats stats = customItemService.reforgeStats(reforgeType, weapon);
        return new WeaponProfile(
                profile.flatDamage() + stats.damageBonus(),
                profile.strength() + stats.strengthBonus(),
                profile.critChancePercent() + stats.critChanceBonus(),
                profile.critDamagePercent() + stats.critDamageBonus(),
                profile.attackSpeedPercent() + stats.attackSpeedBonus(),
                profile.intelligence() + stats.intelligenceBonus(),
                profile.defaultAbility()
        );
    }

    private double dungeonDamageScale(Player attacker) {
        if (attacker == null) {
            return 1.0D;
        }
        DungeonSession session = dungeonManager.getSession(attacker.getUniqueId());
        if (session == null || session.floor() == null) {
            return 1.0D;
        }
        int floorNum = parseFloorNumber(session.floor().id());
        return 1.0D + Math.max(0, floorNum - 1) * 0.08D;
    }

    private int parseFloorNumber(String floorId) {
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

    private double sanitizePositive(double value, double fallback) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0.0D) {
            return fallback;
        }
        return value;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private String formatSeconds(long milliseconds) {
        return String.format(Locale.ROOT, "%.1f", milliseconds / 1000.0D);
    }

    private boolean isEndMinesPvpBlocked(Player attacker, Player victim) {
        if (attacker == null || victim == null) {
            return false;
        }
        String endMinesWorld = plugin.getConfig().getString("end-mines.world-name", "skyblock_end_mines");
        return endMinesWorld != null
                && victim.getWorld() != null
                && endMinesWorld.equalsIgnoreCase(victim.getWorld().getName());
    }

    private boolean isFriendly(Player first, Player second) {
        return dungeonManager.areInSameSession(first.getUniqueId(), second.getUniqueId())
                || partyManager.areInSameParty(first.getUniqueId(), second.getUniqueId());
    }

    private interface AbilityProjectileHandler {
        boolean onEntityImpact(LivingEntity target, Location impact);

        boolean onBlockImpact(Location impact);
    }

    private interface TargetEffect {
        void apply(LivingEntity target);
    }

    private record MeleeIndicatorContext(boolean critical, double displayDamage) {
    }

    private record AbilityCastContext(
            double damage,
            boolean critical
    ) {
        private AbilityCastContext scale(double multiplier) {
            return new AbilityCastContext(damage * multiplier, critical);
        }
    }

    private record WeaponProfile(
            double flatDamage,
            double strength,
            double critChancePercent,
            double critDamagePercent,
            double attackSpeedPercent,
            double intelligence,
            WeaponAbility defaultAbility
    ) {
    }

    private record WeaponAbility(
            String name,
            AbilityEffect effect,
            double manaCost,
            double cooldownSeconds,
            double damageMultiplier,
            double radius,
            double knockback
    ) {
    }

    private enum AbilityEffect {
        DEMON_CRUSH,
        WIND_SLASH,
        TIDE_SURGE,
        SPIRIT_CUT,
        WEB_SNARE,
        FOXFIRE_BURST,
        BONE_CLEAVE,
        THUNDER_STEP,
        RIFT_STEP,
        VOID_SHIFT,
        FRACTURE_DASH,
        DIMENSIONAL_COLLAPSE,
        SHADOW_CHAIN,
        WARP_SHOT,
        PIERCING_RIFT_ARROW,
        ARROW_STORM,
        GRAVITY_COLLAPSE,

        // Mage Weapon Abilities
        ARCANE_BLAST,
        FROST_NOVA,
        INFERNO_BURST,
        CHAIN_LIGHTNING,
        VOID_RIFT,
        STARFALL,
        FIREBALL,
        ICE_SPIKE,
        THUNDER_STRIKE,
        TOXIC_CLOUD,
        HEAL,
        MASS_HEAL,
        WITHER_STORM,
        REGENERATION_AURA
    }

    private record AmmoSource(boolean offHand, int slot, ItemStack stack) {
    }

    private boolean hasDragonTracker(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        SkyblockEnchantment tracker = EnchantmentRegistry.get("dragon_tracker");
        return tracker != null && enchantStorage.getLevel(item, tracker) > 0;
    }

    private void applyDragonTracking(AbstractArrow arrow) {
        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (arrow.isDead() || arrow.isOnGround() || ticks > 100) {
                    this.cancel();
                    return;
                }
                org.bukkit.entity.EnderDragon dragon = null;
                for (org.bukkit.entity.Entity entity : arrow.getWorld().getEntities()) {
                    if (entity instanceof org.bukkit.entity.EnderDragon d && !d.isDead()) {
                        dragon = d;
                        break;
                    }
                }
                if (dragon != null) {
                    Vector toDragon = dragon.getLocation().add(0, 1.5, 0).toVector().subtract(arrow.getLocation().toVector()).normalize();
                    Vector current = arrow.getVelocity();
                    double speed = current.length();
                    arrow.setVelocity(current.add(toDragon.multiply(0.2)).normalize().multiply(speed));
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void rotateVector(Vector vector, double degrees) {
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double x = vector.getX() * cos - vector.getZ() * sin;
        double z = vector.getX() * sin + vector.getZ() * cos;
        vector.setX(x);
        vector.setZ(z);
    }
}

