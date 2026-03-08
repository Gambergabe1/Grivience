package io.papermc.Grivience.listener;

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
import io.papermc.Grivience.stats.SkyblockPlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import io.papermc.Grivience.util.DamageIndicatorUtil;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
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
import io.papermc.Grivience.enchantment.SkyblockEnchantStorage;
import io.papermc.Grivience.skills.SkyblockSkill;
import io.papermc.Grivience.skills.SkyblockSkillManager;
import org.bukkit.Bukkit;

public final class CustomWeaponCombatListener implements Listener {
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

        baseDamageScale = sanitizePositive(plugin.getConfig().getDouble("custom-items.combat.damage.base-scale", 0.04D), 0.04D);
        // Skyblock melee scaling: 1% damage per Strength.
        strengthScaling = sanitizePositive(plugin.getConfig().getDouble("custom-items.combat.damage.strength-scaling", 0.01D), 0.01D);

        abilityDamageScale = sanitizePositive(plugin.getConfig().getDouble("custom-items.combat.mana-abilities.damage-scale", 0.06D), 0.06D);
        abilityManaScale = sanitizePositive(plugin.getConfig().getDouble("custom-items.combat.mana-abilities.mana-scale", 1.0D), 1.0D);
        abilityCooldownScale = sanitizePositive(plugin.getConfig().getDouble("custom-items.combat.mana-abilities.cooldown-scale", 1.0D), 1.0D);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!combatEnabled) {
            return;
        }

        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null || abilityDamageContext.contains(attacker.getUniqueId())) {
            return;
        }
        if (event.getEntity() instanceof Player victim && isFriendly(attacker, victim)) {
            event.setCancelled(true);
            return;
        }

        LivingEntity victim = event.getEntity() instanceof LivingEntity le ? le : null;
        if (victim == null) return;

        ItemStack weaponItem = resolveWeaponItem(attacker, event.getDamager());
        CustomWeaponType weaponType = resolveWeaponType(weaponItem);
        if (weaponType == null) {
            return;
        }

        WeaponProfile profile = applyReforge(profile(weaponType), customItemService.reforgeOf(weaponItem), weaponItem);
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

        // --- SKILL PERKS ---
        double perkMultiplier = 1.0;
        if (skillManager != null) {
            double warriorPerk = skillManager.getPerkValue(attacker, SkyblockSkill.COMBAT);
            if (warriorPerk > 0) {
                perkMultiplier += (warriorPerk / 100.0);
            }
        }

        // Skyblock-style melee formula (scaled to Minecraft using baseDamageScale).
        double skyblockDamage = (weaponDamage + 5.0D) * (1.0D + (totalStrength * strengthScaling)) * enchantMultiplier * perkMultiplier;
        double finalDamage = Math.max(0.1D, skyblockDamage * baseDamageScale * dungeonDamageScale(attacker));
        boolean isCritical = false;

        double critChancePercent = stats == null ? profile.critChancePercent() : stats.critChancePercent();
        critChancePercent = Math.max(0.0D, critChancePercent);
        critChancePercent = clamp(critChancePercent, 0.0D, 100.0D);
        if (ThreadLocalRandom.current().nextDouble() < (critChancePercent / 100.0D)) {
            isCritical = true;
            double critDamagePercent = stats == null ? profile.critDamagePercent() : stats.critDamagePercent();
            critDamagePercent = Math.max(0.0D, critDamagePercent);
            double critMultiplier = 1.0D + (critDamagePercent / 100.0D);
            finalDamage *= Math.max(1.0D, critMultiplier);
            attacker.getWorld().spawnParticle(Particle.CRIT, event.getEntity().getLocation().add(0.0D, 1.0D, 0.0D), 8, 0.35D, 0.25D, 0.35D, 0.02D);
        }

        // Apply damage indicator
        double indicatorDamage = finalDamage / baseDamageScale; // Show original Skyblock numbers
        DamageIndicatorUtil.spawn((GriviencePlugin) plugin, event.getEntity(), indicatorDamage, isCritical);

        event.setDamage(Math.max(0.1D, finalDamage));
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
        if (!customItemService.isCustomDungeonWeapon(bow)) {
            return;
        }
        trackedProjectileWeapons.put(event.getProjectile().getUniqueId(), bow.clone());
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        trackedProjectileWeapons.remove(event.getEntity().getUniqueId());
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
        boolean rightClickAir = action == Action.RIGHT_CLICK_AIR;
        boolean rightClickBlock = action == Action.RIGHT_CLICK_BLOCK;
        boolean leftClickAir = action == Action.LEFT_CLICK_AIR;
        boolean leftClickBlock = action == Action.LEFT_CLICK_BLOCK;
        if (!rightClickAir && !rightClickBlock && !leftClickAir && !leftClickBlock) {
            return;
        }
        if ((rightClickBlock || leftClickBlock) && event.getClickedBlock() != null && event.getClickedBlock().getType().isInteractable()) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack bow = player.getInventory().getItemInMainHand();
        if (bow.getType() != Material.BOW) {
            return;
        }

        CustomWeaponType weaponType = resolveWeaponType(bow);
        if (!isInstantBowWeapon(weaponType)) {
            return;
        }
        if (player.hasCooldown(Material.BOW)) {
            event.setCancelled(true);
            return;
        }

        AmmoSource ammo = findAmmo(player);
        if (ammo == null && player.getGameMode() != GameMode.CREATIVE) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7F, 0.7F);
            event.setCancelled(true);
            return;
        }

        ItemStack ammoItem = ammo == null ? null : ammo.stack();
        AbstractArrow projectile = launchInstantArrow(player, bow, ammoItem);
        trackedProjectileWeapons.put(projectile.getUniqueId(), bow.clone());

        if (shouldConsumeAmmo(player, bow, ammoItem)) {
            consumeAmmo(player, ammo);
        }
        if (instantBowCooldownTicks > 0) {
            player.setCooldown(Material.BOW, instantBowCooldownTicks);
        }

        event.setCancelled(true);
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
                || weaponType == CustomWeaponType.RAIJIN_SHORTBOW;
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
        AbstractArrow projectile;
        if (ammoItem != null && ammoItem.getType() == Material.SPECTRAL_ARROW) {
            projectile = player.launchProjectile(SpectralArrow.class);
        } else {
            Arrow arrow = player.launchProjectile(Arrow.class);
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

        Vector direction = player.getLocation().getDirection();
        if (direction.lengthSquared() < 1.0E-6D) {
            direction = new Vector(0.0D, 0.0D, 1.0D);
        }
        projectile.setVelocity(direction.normalize().multiply(instantBowVelocity));
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
        double totalStrength = combatEngine == null ? profile.strength() : combatEngine.stats(player).strength();
        double strengthMultiplier = Math.max(0.2D, 1.0D + (totalStrength * strengthScaling));
        double intelligenceMultiplier = Math.max(0.25D, 1.0D + (totalIntelligence / 100.0D));
        double baseAbilityDamage = Math.max(
                2.0D,
                profile.flatDamage() * abilityDamageScale * ability.damageMultiplier() * strengthMultiplier * intelligenceMultiplier
        );
        player.sendMessage(ChatColor.AQUA + "Used " + ability.name() + ChatColor.GRAY + " (" + Math.round(manaCostUsed) + " mana)");

        switch (ability.effect()) {
            case DEMON_CRUSH -> {
                playAbilityCast(player, Sound.ENTITY_RAVAGER_ROAR, Particle.SWEEP_ATTACK);
                hitNearby(player, player.getLocation(), ability.radius(), baseAbilityDamage, ability.knockback(), target -> {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0, false, true, true));
                });
            }
            case WIND_SLASH -> {
                playAbilityCast(player, Sound.ENTITY_PHANTOM_FLAP, Particle.CLOUD);
                hitNearby(player, player.getLocation(), ability.radius(), baseAbilityDamage, ability.knockback() + 0.15D, null);
            }
            case TIDE_SURGE -> {
                playAbilityCast(player, Sound.ENTITY_DROWNED_SHOOT, Particle.SPLASH);
                hitNearby(player, player.getLocation(), ability.radius(), baseAbilityDamage, ability.knockback(), target -> {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 0, false, true, true));
                });
            }
            case SPIRIT_CUT -> {
                playAbilityCast(player, Sound.ENTITY_ALLAY_HURT, Particle.SOUL);
                hitNearby(player, player.getLocation(), ability.radius(), baseAbilityDamage, ability.knockback(), target -> {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0, false, true, true));
                });
            }
            case WEB_SNARE -> {
                playAbilityCast(player, Sound.ENTITY_SPIDER_AMBIENT, Particle.CLOUD);
                hitNearby(player, player.getLocation(), ability.radius(), baseAbilityDamage, ability.knockback(), target -> {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 1, false, true, true));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0, false, true, true));
                });
            }
            case FOXFIRE_BURST -> {
                playAbilityCast(player, Sound.ITEM_FIRECHARGE_USE, Particle.FLAME);
                hitNearby(player, player.getLocation(), ability.radius(), baseAbilityDamage, ability.knockback(), target -> {
                    target.setFireTicks(Math.max(target.getFireTicks(), 60));
                });
            }
            case BONE_CLEAVE -> {
                playAbilityCast(player, Sound.ENTITY_WITHER_SKELETON_AMBIENT, Particle.CRIT);
                hitNearby(player, player.getLocation(), ability.radius() + 0.5D, baseAbilityDamage * 1.15D, ability.knockback(), null);
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
                hitNearby(player, strike, ability.radius(), baseAbilityDamage * 1.25D, ability.knockback() + 0.15D, null);
                player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0F, 1.1F);
            }

            // Mage Weapon Abilities
            case ARCANE_BLAST -> {
                playAbilityCast(player, Sound.ENTITY_ENDER_DRAGON_GROWL, Particle.PORTAL);
                shootProjectile(player, baseAbilityDamage, ability.knockback(), Particle.PORTAL, Color.fromRGB(0xD400FF));
            }
            case FROST_NOVA -> {
                playAbilityCast(player, Sound.BLOCK_POWDER_SNOW_BREAK, Particle.SNOWFLAKE);
                hitNearby(player, player.getLocation(), ability.radius(), baseAbilityDamage, ability.knockback(), target -> {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1, false, true, true));
                });
                player.getWorld().spawnParticle(Particle.SNOWFLAKE, player.getLocation().add(0.0D, 0.5D, 0.0D), 50, 0.5D, 0.3D, 0.5D, 0.01D);
            }
            case INFERNO_BURST -> {
                playAbilityCast(player, Sound.ENTITY_BLAZE_SHOOT, Particle.FLAME);
                shootProjectile(player, baseAbilityDamage, ability.knockback(), Particle.FLAME, Color.fromRGB(0xFF4500));
                hitNearby(player, player.getLocation(), ability.radius() * 0.6D, baseAbilityDamage * 0.5D, 0.0D, target -> {
                    target.setFireTicks(Math.max(target.getFireTicks(), 100));
                });
            }
            case CHAIN_LIGHTNING -> {
                playAbilityCast(player, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, Particle.ELECTRIC_SPARK);
                List<LivingEntity> targets = getNearbyEnemies(player.getLocation(), ability.radius());
                int maxChains = Math.min(targets.size(), 5);
                for (int i = 0; i < maxChains; i++) {
                    LivingEntity target = targets.get(i);
                    target.damage(baseAbilityDamage * (1.0D - i * 0.15D), player);
                    target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation(), 10, 0.3D, 0.3D, 0.3D, 0.01D);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5F, 1.2F);
                }
            }
            case VOID_RIFT -> {
                playAbilityCast(player, Sound.ENTITY_ENDERMAN_TELEPORT, Particle.DRAGON_BREATH);
                Location center = player.getLocation().add(player.getLocation().getDirection().multiply(3.0D));
                hitNearby(player, center, ability.radius(), baseAbilityDamage, 0.6D, target -> {
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
                            hitNearby(player, strikeLoc, 2.5D, baseAbilityDamage * 0.6D, 0.3D, null);
                        }
                    }, i * 10L);
                }
            }
            case FIREBALL -> {
                playAbilityCast(player, Sound.ENTITY_BLAZE_SHOOT, Particle.FLAME);
                shootProjectile(player, baseAbilityDamage, ability.knockback(), Particle.FLAME, Color.fromRGB(0xFF6600));
            }
            case ICE_SPIKE -> {
                playAbilityCast(player, Sound.BLOCK_POWDER_SNOW_BREAK, Particle.SNOWFLAKE);
                shootProjectile(player, baseAbilityDamage, ability.knockback(), Particle.SNOWFLAKE, Color.fromRGB(0x00FFFF));
            }
            case THUNDER_STRIKE -> {
                playAbilityCast(player, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, Particle.ELECTRIC_SPARK);
                Location targetLoc = player.getLocation().add(player.getLocation().getDirection().multiply(5.0D));
                if (targetLoc.getWorld() != null) {
                    targetLoc.getWorld().strikeLightningEffect(targetLoc);
                    targetLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, targetLoc, 25, 0.4D, 0.3D, 0.4D, 0.01D);
                    hitNearby(player, targetLoc, 3.0D, baseAbilityDamage, 0.4D, null);
                }
            }
            case TOXIC_CLOUD -> {
                playAbilityCast(player, Sound.ENTITY_WITCH_AMBIENT, Particle.CAMPFIRE_COSY_SMOKE);
                Location center = player.getLocation().add(player.getLocation().getDirection().multiply(2.0D));
                hitNearby(player, center, ability.radius(), baseAbilityDamage, 0.2D, target -> {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 0, false, true, true));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 80, 0, false, true, true));
                });
                player.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, center, 40, 0.4D, 0.3D, 0.4D, 0.01D);
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, center, 30, 0.4D, 0.3D, 0.4D, 0.01D);
            }
            case HEAL -> {
                playAbilityCast(player, Sound.ENTITY_PLAYER_LEVELUP, Particle.HEART);
                double healAmount = baseAbilityDamage * 0.8D + (ability.manaCost() * 0.5D);
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
                hitNearby(player, player.getLocation(), ability.radius(), baseAbilityDamage, ability.knockback(), target -> {
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

    private void playAbilityCast(Player player, Sound sound, Particle particle) {
        player.playSound(player.getLocation(), sound, 0.95F, 1.0F);
        player.getWorld().spawnParticle(particle, player.getLocation().add(0.0D, 1.1D, 0.0D), 20, 0.45D, 0.25D, 0.45D, 0.02D);
    }

    private void hitNearby(
            Player caster,
            Location center,
            double radius,
            double damage,
            double knockback,
            TargetEffect effect
    ) {
        if (center.getWorld() == null) {
            return;
        }
        UUID casterId = caster.getUniqueId();
        abilityDamageContext.add(casterId);
        try {
            for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                if (!(entity instanceof LivingEntity target)) {
                    continue;
                }
                if (target.equals(caster) || target.isDead() || target.isInvulnerable()) {
                    continue;
                }
                if (target instanceof Player otherPlayer && otherPlayer.getGameMode().isInvulnerable()) {
                    continue;
                }
                if (target instanceof Player otherPlayer && isFriendly(caster, otherPlayer)) {
                    continue;
                }

                target.damage(damage, caster);
                if (effect != null) {
                    effect.apply(target);
                }

                if (knockback > 0.0D) {
                    Vector push = target.getLocation().toVector().subtract(center.toVector());
                    if (push.lengthSquared() > 1.0E-6D) {
                        push.normalize().multiply(knockback).setY(0.18D);
                        target.setVelocity(target.getVelocity().add(push));
                    }
                }
            }
        } finally {
            abilityDamageContext.remove(casterId);
        }
    }

    private void shootProjectile(Player player, double damage, double knockback, Particle particle, Color color) {
        Location loc = player.getLocation().add(0.0D, 1.5D, 0.0D);
        Vector direction = loc.getDirection();
        
        shootProjectileTask(player, loc.clone(), direction.clone(), damage, knockback, particle, 0);
    }
    
    private void shootProjectileTask(Player player, Location current, Vector vel, double damage, double knockback, Particle particle, int ticks) {
        if (ticks >= 100) {
            return;
        }
        
        current.add(vel);
        player.getWorld().spawnParticle(particle, current, 5, 0.1D, 0.1D, 0.1D, 0.01D);
        
        // Check for hits
        for (Entity entity : player.getWorld().getNearbyEntities(current, 0.8D, 0.8D, 0.8D)) {
            if (entity instanceof LivingEntity target && !entity.equals(player)) {
                if (target.isInvulnerable() || target.isDead()) {
                    continue;
                }
                if (target instanceof Player otherPlayer && isFriendly(player, otherPlayer)) {
                    continue;
                }
                
                target.damage(damage, player);
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
            shootProjectileTask(player, current, vel, damage, knockback, particle, ticks + 1), 1L);
    }

    private List<LivingEntity> getNearbyEnemies(Location center, double radius) {
        List<LivingEntity> enemies = new ArrayList<>();
        if (center.getWorld() == null) {
            return enemies;
        }
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof LivingEntity living && !entity.isDead() && !entity.isInvulnerable()) {
                enemies.add(living);
            }
        }
        return enemies;
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

    private boolean isFriendly(Player first, Player second) {
        return dungeonManager.areInSameSession(first.getUniqueId(), second.getUniqueId())
                || partyManager.areInSameParty(first.getUniqueId(), second.getUniqueId());
    }

    private interface TargetEffect {
        void apply(LivingEntity target);
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
}

