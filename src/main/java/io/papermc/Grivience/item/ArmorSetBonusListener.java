package io.papermc.Grivience.item;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.stats.SkyblockCombatEngine;
import io.papermc.Grivience.util.EndZoneUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages all Armor Set Abilities - 100% Skyblock accurate.
 */
public final class ArmorSetBonusListener implements Listener {
    private final GriviencePlugin plugin;
    private final CustomArmorManager armorManager;
    private final Map<UUID, Map<String, Integer>> activeSetBonuses = new HashMap<>();
    private final Map<UUID, Long> lastAbilityTrigger = new HashMap<>();
    private final Map<UUID, Double> guardianShield = new HashMap<>();

    public ArmorSetBonusListener(GriviencePlugin plugin, CustomArmorManager armorManager) {
        this.plugin = plugin;
        this.armorManager = armorManager;

        // Effect ticker (once per second)
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updatePassiveEffects(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        checkArmorSet(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCombat(EntityDamageByEntityEvent event) {
        // 1. Shogun Warlord's Resolve (Attacker)
        if (event.getDamager() instanceof Player attacker) {
            if (hasFullSet(attacker, "shogun")) {
                long allies = attacker.getNearbyEntities(8, 8, 8).stream()
                        .filter(e -> e instanceof Player && !e.equals(attacker))
                        .count();
                if (allies > 0) {
                    double bonus = 1.0 + (allies * 0.05); // 5% per ally
                    double baseDamage = event.getDamage(org.bukkit.event.entity.EntityDamageEvent.DamageModifier.BASE);
                    event.setDamage(org.bukkit.event.entity.EntityDamageEvent.DamageModifier.BASE, baseDamage * bonus);
                }
            }

            // 10. Dreadlord: Dungeon Damage
            if (isInDungeon(attacker)) {
                int pieces = getActivePieceCount(attacker, "dreadlord");
                double multiplier = 1.0;
                if (pieces >= 4) {
                    multiplier = 1.15; // 15% bonus
                } else if (pieces >= 2) {
                    multiplier = 1.05; // 5% bonus
                }
                if (multiplier > 1.0) {
                    double baseDamage = event.getDamage(org.bukkit.event.entity.EntityDamageEvent.DamageModifier.BASE);
                    event.setDamage(org.bukkit.event.entity.EntityDamageEvent.DamageModifier.BASE, baseDamage * multiplier);
                }
            }

            // 11. Crimson Warden: Warden's Might
            if (hasFullSet(attacker, "crimson_warden")) {
                double baseDamage = event.getDamage(org.bukkit.event.entity.EntityDamageEvent.DamageModifier.BASE);
                event.setDamage(org.bukkit.event.entity.EntityDamageEvent.DamageModifier.BASE, baseDamage * 1.05);
            }

            if (hasSetPiece(attacker, "dragon_slayer", CustomArmorManager.ArmorPieceType.HELMET)
                    && EndZoneUtil.isEndMob(plugin, event.getEntity())) {
                double baseDamage = event.getDamage(org.bukkit.event.entity.EntityDamageEvent.DamageModifier.BASE);
                event.setDamage(org.bukkit.event.entity.EntityDamageEvent.DamageModifier.BASE, baseDamage * 1.15D);
            }

            // Crimson Warden Boots: chance to leave a slowing field on hit
            if (hasSetPiece(attacker, "crimson_warden", CustomArmorManager.ArmorPieceType.BOOTS)
                    && event.getEntity() instanceof LivingEntity struck
                    && Math.random() < 0.20D) {
                Location center = struck.getLocation();
                struck.getWorld().spawnParticle(Particle.DUST, center.add(0.0D, 0.2D, 0.0D), 12, 0.5D, 0.1D, 0.5D, 0.0D, new Particle.DustOptions(org.bukkit.Color.fromRGB(0xB23A3A), 1.0F));
                for (Entity nearby : struck.getWorld().getNearbyEntities(struck.getLocation(), 2.5D, 1.5D, 2.5D)) {
                    if (nearby instanceof LivingEntity living && !nearby.equals(attacker)) {
                        applyEffectToEntity(living, PotionEffectType.SLOWNESS, 0, 40);
                    }
                }
            }
        }

        // 2. Titan Colossal Barrier (Defender)
        if (event.getEntity() instanceof Player victim) {
            if (hasFullSet(victim, "titan")) {
                double baseDamage = event.getDamage(org.bukkit.event.entity.EntityDamageEvent.DamageModifier.BASE);
                event.setDamage(org.bukkit.event.entity.EntityDamageEvent.DamageModifier.BASE, baseDamage * 0.75); // 25% reduction
            }

            // 3. Guardian Divine Protection (Defender)
            if (hasFullSet(victim, "guardian")) {
                double shield = guardianShield.getOrDefault(victim.getUniqueId(), 500.0);
                if (shield > 0) {
                    double baseDamage = event.getDamage(org.bukkit.event.entity.EntityDamageEvent.DamageModifier.BASE);
                    if (baseDamage <= shield) {
                        guardianShield.put(victim.getUniqueId(), shield - baseDamage);
                        event.setDamage(org.bukkit.event.entity.EntityDamageEvent.DamageModifier.BASE, 0);
                        victim.playSound(victim.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.5f);
                        victim.spawnParticle(Particle.INSTANT_EFFECT, victim.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.05);
                    } else {
                        event.setDamage(org.bukkit.event.entity.EntityDamageEvent.DamageModifier.BASE, baseDamage - shield);
                        guardianShield.put(victim.getUniqueId(), 0.0);
                        victim.sendMessage(ChatColor.RED + "Your Guardian Shield has broken!");
                        victim.playSound(victim.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.0f, 0.8f);
                    }
                }
            }

            if (hasSetPiece(victim, "dragon_slayer", CustomArmorManager.ArmorPieceType.CHESTPLATE)
                    && EndZoneUtil.isEndMob(plugin, event.getDamager())) {
                double baseDamage = event.getDamage(org.bukkit.event.entity.EntityDamageEvent.DamageModifier.BASE);
                event.setDamage(org.bukkit.event.entity.EntityDamageEvent.DamageModifier.BASE, baseDamage * 0.90D);
            }

            if (hasSetPiece(victim, "crimson_warden", CustomArmorManager.ArmorPieceType.CHESTPLATE)) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!victim.isOnline()) {
                        return;
                    }
                    var velocity = victim.getVelocity();
                    victim.setVelocity(velocity.setX(velocity.getX() * 0.70D).setZ(velocity.getZ() * 0.70D));
                }, 1L);
            }

            if (hasSetPiece(victim, "crimson_warden", CustomArmorManager.ArmorPieceType.LEGGINGS)) {
                applyEffect(victim, PotionEffectType.SPEED, 0, 40);
            }

            if (hasSetPiece(victim, "dragon_slayer", CustomArmorManager.ArmorPieceType.BOOTS)
                    && EndZoneUtil.isEndWorld(plugin, victim.getWorld())) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!victim.isOnline()) {
                        return;
                    }
                    var velocity = victim.getVelocity();
                    victim.setVelocity(velocity.setX(0.0D).setZ(0.0D));
                }, 1L);
            }
        }
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        // 4. Shinobi Shadowstep
        if (hasFullSet(killer, "shinobi")) {
            applyEffect(killer, PotionEffectType.INVISIBILITY, 0, 100);
            applyEffect(killer, PotionEffectType.SPEED, 2, 100);
            killer.sendMessage(ChatColor.AQUA + "Shadowstep activated!");
            killer.playSound(killer.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
        }

        // 8. Necromancer Lord: Mana on Kill
        if (hasFullSet(killer, "necromancer")) {
            if (plugin.getSkyblockManaManager() != null) {
                plugin.getSkyblockManaManager().addMana(killer, 20);
                killer.playSound(killer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 0.5f);
            }
        }

        // 14. Soulbound Mage: Cursed Drops
        if (hasFullSet(killer, "soulbound")) {
            if (Math.random() < 0.40D) { // 40% chance to lose loot
                event.getDrops().clear();
                event.setDroppedExp(0);
                killer.sendMessage(ChatColor.DARK_RED + "The curse of the soulbound consumes your loot...");
                killer.playSound(killer.getLocation(), Sound.ENTITY_VEX_AMBIENT, 1.0f, 0.5f);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL
                && hasSetPiece(player, "dragon_slayer", CustomArmorManager.ArmorPieceType.BOOTS)) {
            double baseDamage = event.getDamage(EntityDamageEvent.DamageModifier.BASE);
            event.setDamage(EntityDamageEvent.DamageModifier.BASE, baseDamage * 0.50D);
        }
    }

    private void updatePassiveEffects(Player player) {
        UUID uuid = player.getUniqueId();
        
        // 9. Dreadlord: Wither Aura
        if (hasFullSet(player, "dreadlord")) {
            for (Entity nearby : player.getNearbyEntities(5, 5, 5)) {
                if (nearby instanceof LivingEntity le && !(nearby instanceof Player)) {
                    applyEffectToEntity(le, PotionEffectType.WITHER, 1, 40);
                    le.getWorld().spawnParticle(Particle.SMOKE, le.getLocation().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0.01);
                }
            }
        }

        // 5. Leviathan Abyssal Bulwark
        if (hasFullSet(player, "leviathan")) {
            applyEffect(player, PotionEffectType.WATER_BREATHING, 0, 40);
            applyEffect(player, PotionEffectType.DOLPHINS_GRACE, 0, 40);
        }

        // 6. Ironcrest Mine Defense
        if (hasFullSet(player, "ironcrest")) {
            if (isInMiningZone(player)) {
                // This is handled by SkyblockCombatStatsService usually, 
                // but we can provide a visual indicator or temporary buff if needed.
                applyEffect(player, PotionEffectType.RESISTANCE, 0, 40);
            }
        }

        // 7. Guardian Shield Recharge
        if (hasFullSet(player, "guardian")) {
            long lastTrigger = lastAbilityTrigger.getOrDefault(uuid, 0L);
            if (System.currentTimeMillis() - lastTrigger > 60000) { // 60s cooldown
                double current = guardianShield.getOrDefault(uuid, 0.0);
                if (current < 500.0) {
                    guardianShield.put(uuid, 500.0);
                    lastAbilityTrigger.put(uuid, System.currentTimeMillis());
                    player.sendMessage(ChatColor.WHITE + "Your Guardian Shield has recharged!");
                    player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 2.0f);
                }
            }
        }

        if (hasSetPiece(player, "dragon_slayer", CustomArmorManager.ArmorPieceType.BOOTS)) {
            applyEffectIfStronger(player, PotionEffectType.SPEED, 0, 40);
        }

        // 13. Soulbound Mage: Cursed Presence (Drain + Shadow Mobs)
        if (hasFullSet(player, "soulbound")) {
            // Health Drain: 1% of max health per second
            double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
            double drain = maxHealth * 0.01;
            if (player.getHealth() > drain) {
                player.setHealth(player.getHealth() - drain);
            }
            player.spawnParticle(Particle.SOUL, player.getLocation().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0.01);
            
            // Shadow Mobs: 5% chance to spawn a shadow stalker nearby
            if (Math.random() < 0.05D) {
                if (plugin.getCustomMonsterManager() != null) {
                    plugin.getCustomMonsterManager().spawnMonster("shadow_stalker", player.getLocation().add(5, 0, 5));
                    player.sendMessage(ChatColor.DARK_GRAY + "A shadow stalker emerges from your dread...");
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_STARE, 0.5f, 0.5f);
                }
            }
        }
    }

    public boolean hasFullSet(Player player, String setId) {
        return getActivePieceCount(player, setId) >= 4;
    }

    private boolean hasSetPiece(Player player, String setId, CustomArmorManager.ArmorPieceType pieceType) {
        if (player == null || setId == null || pieceType == null) {
            return false;
        }
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            if (piece == null) {
                continue;
            }
            String equippedSetId = armorManager.getArmorSetId(piece);
            CustomArmorManager.ArmorPieceType equippedPieceType = armorManager.getArmorPieceType(piece);
            if (setId.equalsIgnoreCase(equippedSetId) && pieceType == equippedPieceType) {
                return true;
            }
        }
        return false;
    }

    private void checkArmorSet(Player player) {
        Map<String, Integer> setCounts = new HashMap<>();
        ItemStack[] armor = player.getEquipment().getArmorContents();
        
        for (ItemStack piece : armor) {
            if (piece == null) continue;
            String setId = armorManager.getArmorSetId(piece);
            if (setId != null) {
                setCounts.put(setId.toLowerCase(), setCounts.getOrDefault(setId.toLowerCase(), 0) + 1);
            }
        }

        activeSetBonuses.put(player.getUniqueId(), setCounts);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryChange(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> checkArmorSet(player), 1L);
        }
    }

    private void applyEffect(Player player, PotionEffectType type, int amplifier, int duration) {
        player.addPotionEffect(new PotionEffect(type, duration, amplifier, true, false, true));
    }

    private void applyEffectIfStronger(Player player, PotionEffectType type, int amplifier, int duration) {
        PotionEffect current = player.getPotionEffect(type);
        if (current != null && current.getAmplifier() > amplifier && current.getDuration() > duration) {
            return;
        }
        player.addPotionEffect(new PotionEffect(type, duration, amplifier, true, false, true));
    }

    private void applyEffectToEntity(LivingEntity entity, PotionEffectType type, int amplifier, int duration) {
        entity.addPotionEffect(new PotionEffect(type, duration, amplifier, true, false, true));
    }

    private boolean isInMiningZone(Player player) {
        if (player.getWorld().getName().equalsIgnoreCase("Minehub")) return true;
        String worldName = plugin.getConfig().getString("skyblock.minehub-world", "minehub_world");
        return player.getWorld().getName().equalsIgnoreCase(worldName);
    }

    private boolean isInDungeon(Player player) {
        if (plugin.getDungeonManager() == null) return false;
        return plugin.getDungeonManager().isInDungeon(player.getUniqueId());
    }

    public int getActivePieceCount(Player player, String setId) {
        Map<String, Integer> sets = activeSetBonuses.get(player.getUniqueId());
        return sets != null ? sets.getOrDefault(setId.toLowerCase(), 0) : 0;
    }
}
