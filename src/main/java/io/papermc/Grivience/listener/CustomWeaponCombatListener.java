package io.papermc.Grivience.listener;

import io.papermc.Grivience.dungeon.DungeonManager;
import io.papermc.Grivience.hook.AuraSkillsHook;
import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.item.CustomWeaponType;
import io.papermc.Grivience.item.ReforgeType;
import io.papermc.Grivience.party.PartyManager;
import org.bukkit.ChatColor;
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

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class CustomWeaponCombatListener implements Listener {
    private final JavaPlugin plugin;
    private final CustomItemService customItemService;
    private final AuraSkillsHook auraSkillsHook;
    private final DungeonManager dungeonManager;
    private final PartyManager partyManager;
    private final Map<UUID, EnumMap<CustomWeaponType, Long>> abilityCooldowns = new HashMap<>();
    private final Map<UUID, ItemStack> trackedProjectileWeapons = new HashMap<>();
    private final Set<UUID> abilityDamageContext = new HashSet<>();

    private boolean combatEnabled;
    private boolean abilitiesEnabled;
    private boolean abilityRightClickBlocks;
    private boolean notifyCooldown;
    private boolean requireAuraForAbilities;
    private boolean instantBowShotsEnabled;

    private double baseDamageScale;
    private double strengthScaling;
    private double baseCritChance;
    private double baseCritDamagePercent;

    private double abilityDamageScale;
    private double abilityManaScale;
    private double abilityCooldownScale;
    private double instantBowVelocity;
    private int instantBowCooldownTicks;

    public CustomWeaponCombatListener(
            JavaPlugin plugin,
            CustomItemService customItemService,
            AuraSkillsHook auraSkillsHook,
            DungeonManager dungeonManager,
            PartyManager partyManager
    ) {
        this.plugin = plugin;
        this.customItemService = customItemService;
        this.auraSkillsHook = auraSkillsHook;
        this.dungeonManager = dungeonManager;
        this.partyManager = partyManager;
    }

    public void reloadFromConfig() {
        combatEnabled = plugin.getConfig().getBoolean("custom-items.combat.enabled", true);
        abilitiesEnabled = plugin.getConfig().getBoolean("custom-items.combat.mana-abilities.enabled", true);
        abilityRightClickBlocks = plugin.getConfig().getBoolean("custom-items.combat.mana-abilities.allow-right-click-block", false);
        notifyCooldown = plugin.getConfig().getBoolean("custom-items.combat.mana-abilities.notify-cooldown", true);
        requireAuraForAbilities = plugin.getConfig().getBoolean("custom-items.combat.mana-abilities.require-auraskills", true);
        instantBowShotsEnabled = plugin.getConfig().getBoolean("custom-items.combat.instant-bow-shots.enabled", true);
        instantBowVelocity = sanitizePositive(plugin.getConfig().getDouble("custom-items.combat.instant-bow-shots.arrow-velocity", 3.0D), 3.0D);
        instantBowCooldownTicks = Math.max(0, plugin.getConfig().getInt("custom-items.combat.instant-bow-shots.cooldown-ticks", 4));

        baseDamageScale = sanitizePositive(plugin.getConfig().getDouble("custom-items.combat.damage.base-scale", 0.04D), 0.04D);
        strengthScaling = sanitizePositive(plugin.getConfig().getDouble("custom-items.combat.damage.strength-scaling", 0.0045D), 0.0045D);
        baseCritChance = clamp(plugin.getConfig().getDouble("custom-items.combat.damage.base-crit-chance", 0.05D), 0.0D, 0.95D);
        baseCritDamagePercent = sanitizePositive(plugin.getConfig().getDouble("custom-items.combat.damage.base-crit-damage-percent", 50.0D), 50.0D);

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

        ItemStack weaponItem = resolveWeaponItem(attacker, event.getDamager());
        CustomWeaponType weaponType = resolveWeaponType(weaponItem);
        if (weaponType == null) {
            return;
        }

        WeaponProfile profile = applyReforge(profile(weaponType), customItemService.reforgeOf(weaponItem));
        double auraStrength = auraSkillsHook.getStat(attacker, AuraSkillsHook.StatKey.STRENGTH);
        double auraCritChance = auraSkillsHook.getStat(attacker, AuraSkillsHook.StatKey.CRIT_CHANCE);
        double auraCritDamage = auraSkillsHook.getStat(attacker, AuraSkillsHook.StatKey.CRIT_DAMAGE);

        double vanillaDamage = Math.max(0.1D, event.getDamage());
        double scaledBase = vanillaDamage + (profile.flatDamage() * baseDamageScale);
        double strengthMultiplier = Math.max(0.15D, 1.0D + ((profile.strength() + auraStrength) * strengthScaling));
        double finalDamage = scaledBase * strengthMultiplier;

        double critChance = clamp(baseCritChance + ((profile.critChancePercent() + auraCritChance) / 100.0D), 0.0D, 0.95D);
        if (ThreadLocalRandom.current().nextDouble() < critChance) {
            double critMultiplier = Math.max(
                    1.0D,
                    1.0D + ((baseCritDamagePercent + profile.critDamagePercent() + auraCritDamage) / 100.0D)
            );
            finalDamage *= critMultiplier;
            attacker.getWorld().spawnParticle(Particle.CRIT, event.getEntity().getLocation().add(0.0D, 1.0D, 0.0D), 8, 0.35D, 0.25D, 0.35D, 0.02D);
        }

        event.setDamage(Math.max(0.1D, finalDamage));
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
        if (!rightClickAir && !rightClickBlock) {
            return;
        }
        if (rightClickBlock && event.getClickedBlock() != null && event.getClickedBlock().getType().isInteractable()) {
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

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
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
        if (rightClickBlock && !abilityRightClickBlocks) {
            return;
        }
        if (rightClickBlock && event.getClickedBlock() != null && event.getClickedBlock().getType().isInteractable()) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        CustomWeaponType weaponType = resolveWeaponType(item);
        if (weaponType == null) {
            return;
        }

        WeaponProfile profile = applyReforge(profile(weaponType), customItemService.reforgeOf(item));
        WeaponAbility ability = resolveAbility(weaponType, profile.defaultAbility());
        if (ability == null) {
            return;
        }

        long now = System.currentTimeMillis();
        long cooldownRemaining = cooldownRemainingMillis(player.getUniqueId(), weaponType, now);
        if (cooldownRemaining > 0L) {
            if (notifyCooldown) {
                player.sendMessage(ChatColor.RED + ability.name() + " is on cooldown for " + formatSeconds(cooldownRemaining) + "s.");
            }
            return;
        }

        double manaCost = ability.manaCost() * abilityManaScale;
        if (manaCost > 0.0D) {
            if (!auraSkillsHook.isAvailable()) {
                if (requireAuraForAbilities) {
                    player.sendMessage(ChatColor.RED + "AuraSkills is required to use mana abilities.");
                    return;
                }
            } else if (!auraSkillsHook.consumeMana(player, manaCost)) {
                player.sendMessage(ChatColor.RED + "Not enough mana.");
                return;
            }
        }

        long cooldownMillis = Math.max(500L, Math.round(ability.cooldownSeconds() * abilityCooldownScale * 1000.0D));
        setCooldown(player.getUniqueId(), weaponType, now + cooldownMillis);
        castAbility(player, profile, ability);
    }

    private boolean isInstantBowWeapon(CustomWeaponType weaponType) {
        return weaponType == CustomWeaponType.TENGU_STORMBOW
                || weaponType == CustomWeaponType.KITSUNE_DAWNBOW;
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
        projectile.setKnockbackStrength(Math.max(0, bow.getEnchantmentLevel(Enchantment.PUNCH)));
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
            return trident.getItem();
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

    private void castAbility(Player player, WeaponProfile profile, WeaponAbility ability) {
        double auraStrength = auraSkillsHook.getStat(player, AuraSkillsHook.StatKey.STRENGTH);
        double strengthMultiplier = Math.max(0.2D, 1.0D + ((profile.strength() + auraStrength) * strengthScaling));
        double baseAbilityDamage = Math.max(2.0D, profile.flatDamage() * abilityDamageScale * ability.damageMultiplier() * strengthMultiplier);
        player.sendMessage(ChatColor.AQUA + "Used " + ability.name() + ChatColor.GRAY + " (" + Math.round(ability.manaCost() * abilityManaScale) + " mana)");

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
        return switch (type) {
            case ONI_CLEAVER -> new WeaponProfile(
                    125.0D, 45.0D, 0.0D, 0.0D,
                    new WeaponAbility("Demon Crush", AbilityEffect.DEMON_CRUSH, 40.0D, 8.0D, 1.00D, 3.6D, 0.45D)
            );
            case TENGU_GALEBLADE -> new WeaponProfile(
                    110.0D, 20.0D, 18.0D, 20.0D,
                    new WeaponAbility("Wind Slash", AbilityEffect.WIND_SLASH, 32.0D, 6.0D, 0.95D, 4.0D, 0.52D)
            );
            case TENGU_STORMBOW -> new WeaponProfile(
                    118.0D, 16.0D, 22.0D, 24.0D,
                    null
            );
            case KAPPA_TIDEBREAKER -> new WeaponProfile(
                    120.0D, 30.0D, 0.0D, 10.0D,
                    new WeaponAbility("Tide Surge", AbilityEffect.TIDE_SURGE, 36.0D, 7.0D, 1.05D, 3.8D, 0.40D)
            );
            case ONRYO_SPIRITBLADE -> new WeaponProfile(
                    145.0D, 45.0D, 5.0D, 20.0D,
                    new WeaponAbility("Wraith Cut", AbilityEffect.SPIRIT_CUT, 46.0D, 9.0D, 1.10D, 3.7D, 0.42D)
            );
            case JOROGUMO_STINGER -> new WeaponProfile(
                    95.0D, 18.0D, 8.0D, 10.0D,
                    new WeaponAbility("Silk Snare", AbilityEffect.WEB_SNARE, 28.0D, 6.0D, 0.85D, 3.5D, 0.30D)
            );
            case KITSUNE_FANG -> new WeaponProfile(
                    108.0D, 25.0D, 12.0D, 25.0D,
                    new WeaponAbility("Foxfire Burst", AbilityEffect.FOXFIRE_BURST, 40.0D, 8.0D, 1.00D, 4.2D, 0.36D)
            );
            case KITSUNE_DAWNBOW -> new WeaponProfile(
                    126.0D, 22.0D, 14.0D, 40.0D,
                    null
            );
            case GASHADOKURO_NODACHI -> new WeaponProfile(
                    165.0D, 70.0D, 4.0D, 30.0D,
                    new WeaponAbility("Bone Cleave", AbilityEffect.BONE_CLEAVE, 58.0D, 11.0D, 1.20D, 4.1D, 0.52D)
            );
            case FLYING_RAIJIN -> new WeaponProfile(
                    210.0D, 100.0D, 10.0D, 35.0D,
                    new WeaponAbility("Thunder Step", AbilityEffect.THUNDER_STEP, 75.0D, 12.0D, 1.25D, 4.2D, 0.58D)
            );
        };
    }

    private WeaponProfile applyReforge(WeaponProfile profile, ReforgeType reforgeType) {
        if (profile == null || reforgeType == null) {
            return profile;
        }
        return new WeaponProfile(
                profile.flatDamage() + reforgeType.damageBonus(),
                profile.strength() + reforgeType.strengthBonus(),
                profile.critChancePercent() + reforgeType.critChanceBonus(),
                profile.critDamagePercent() + reforgeType.critDamageBonus(),
                profile.defaultAbility()
        );
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
        THUNDER_STEP
    }

    private record AmmoSource(boolean offHand, int slot, ItemStack stack) {
    }
}
