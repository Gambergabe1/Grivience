package io.papermc.Grivience.stats;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies Skyblock-style health/defense mechanics to players.
 *
 * - Max health is mapped from Skyblock HP -> Minecraft health points.
 * - Vanilla armor/toughness can be neutralized so Skyblock Defense is the source of mitigation.
 * - Incoming damage is reduced using Skyblock's defense formula: damage * 100/(100+defense).
 */
public final class SkyblockCombatEngine implements Listener {
    private static final double EPSILON = 1e-6D;

    private static final NamespacedKey KEY_MAX_HEALTH = new NamespacedKey("grivience", "sb_max_health");
    private static final NamespacedKey KEY_ARMOR_ZERO = new NamespacedKey("grivience", "sb_armor_zero");
    private static final NamespacedKey KEY_TOUGHNESS_ZERO = new NamespacedKey("grivience", "sb_toughness_zero");

    private final io.papermc.Grivience.GriviencePlugin plugin;
    private final SkyblockCombatStatsService statsService;
    private final Map<UUID, SkyblockPlayerStats> cached = new ConcurrentHashMap<>();

    private BukkitTask tickTask;

    private boolean enabled;
    private boolean neutralizeVanillaArmor;
    private double sbToMcHealthScale;

    public SkyblockCombatEngine(
            io.papermc.Grivience.GriviencePlugin plugin,
            SkyblockCombatStatsService statsService
    ) {
        this.plugin = plugin;
        this.statsService = statsService;
        reload();
        start();
    }

    public double getHealthScale() {
        return sbToMcHealthScale;
    }

    public void reload() {
        boolean wasEnabled = enabled;
        enabled = plugin.getConfig().getBoolean("skyblock-combat.enabled", true);
        neutralizeVanillaArmor = plugin.getConfig().getBoolean("skyblock-combat.neutralize-vanilla-armor", true);
        sbToMcHealthScale = clampFinite(plugin.getConfig().getDouble("skyblock-combat.sb-health-scale", 5.0D), 0.1D, 100.0D);
        statsService.reload();

        if (wasEnabled && !enabled) {
            stop();
            for (Player player : Bukkit.getOnlinePlayers()) {
                removeModifier(player, Attribute.MAX_HEALTH, KEY_MAX_HEALTH);
                removeModifier(player, Attribute.ARMOR, KEY_ARMOR_ZERO);
                removeModifier(player, Attribute.ARMOR_TOUGHNESS, KEY_TOUGHNESS_ZERO);
            }
            cached.clear();
        } else if (!wasEnabled && enabled) {
            start();
        }
    }

    private void start() {
        stop();
        if (!enabled) {
            return;
        }
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 20L);
    }

    private void stop() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
    }

    public void shutdown() {
        stop();
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeModifier(player, Attribute.MAX_HEALTH, KEY_MAX_HEALTH);
            removeModifier(player, Attribute.ARMOR, KEY_ARMOR_ZERO);
            removeModifier(player, Attribute.ARMOR_TOUGHNESS, KEY_TOUGHNESS_ZERO);
        }
        cached.clear();
    }

    public SkyblockPlayerStats stats(Player player) {
        if (player == null) {
            return statsService.compute(null);
        }
        SkyblockPlayerStats stats = cached.get(player.getUniqueId());
        if (stats != null) {
            return stats;
        }
        SkyblockPlayerStats computed = statsService.compute(player);
        cached.put(player.getUniqueId(), computed);
        return computed;
    }

    public void refreshNow(Player player) {
        refresh(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled) {
            return;
        }
        // Delay one tick so inventory/attributes are ready.
        Bukkit.getScheduler().runTaskLater(plugin, () -> refresh(event.getPlayer()), 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cached.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!enabled) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (isBypassed(player)) {
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID
                || event.getCause() == EntityDamageEvent.DamageCause.SUICIDE) {
            return;
        }

        double defense = Math.max(0.0D, stats(player).defense());
        if (defense <= 0.0D) {
            return;
        }

        double multiplier = 100.0D / (100.0D + defense);
        double damage = event.getDamage();
        if (!Double.isFinite(damage) || damage <= 0.0D) {
            return;
        }
        event.setDamage(Math.max(0.0D, damage * multiplier));
    }

    private void tick() {
        if (!enabled) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            refresh(player);
        }
    }

    private void refresh(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        SkyblockPlayerStats stats = statsService.compute(player);
        cached.put(player.getUniqueId(), stats);
        applyAttributes(player, stats);
    }

    private void applyAttributes(Player player, SkyblockPlayerStats stats) {
        if (!enabled) {
            return;
        }
        if (isBypassed(player)) {
            return;
        }

        double desiredMcMaxHealth = clampFinite(stats.health() / sbToMcHealthScale, 1.0D, 2048.0D);
        setAttributeTo(player, Attribute.MAX_HEALTH, KEY_MAX_HEALTH, desiredMcMaxHealth);

        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null && !player.isDead()) {
            double max = Math.max(1.0D, maxHealth.getValue());
            if (player.getHealth() > max + EPSILON) {
                player.setHealth(max);
            }
        }

        if (neutralizeVanillaArmor) {
            setAttributeTo(player, Attribute.ARMOR, KEY_ARMOR_ZERO, 0.0D);
            setAttributeTo(player, Attribute.ARMOR_TOUGHNESS, KEY_TOUGHNESS_ZERO, 0.0D);
        }
    }

    private void setAttributeTo(Player player, Attribute attribute, NamespacedKey key, double desiredValue) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst == null) {
            return;
        }
        removeModifier(inst, key);

        double current = inst.getValue();
        if (!Double.isFinite(current)) {
            return;
        }
        double delta = desiredValue - current;
        if (!Double.isFinite(delta) || Math.abs(delta) < EPSILON) {
            return;
        }

        inst.addModifier(new AttributeModifier(key, delta, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
    }

    private void removeModifier(Player player, Attribute attribute, NamespacedKey key) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst == null) {
            return;
        }
        removeModifier(inst, key);
    }

    private void removeModifier(AttributeInstance inst, NamespacedKey key) {
        AttributeModifier toRemove = null;
        for (AttributeModifier modifier : inst.getModifiers()) {
            if (modifier != null && key.equals(modifier.getKey())) {
                toRemove = modifier;
                break;
            }
        }
        if (toRemove != null) {
            inst.removeModifier(toRemove);
        }
    }

    private boolean isBypassed(Player player) {
        if (player == null) {
            return true;
        }
        GameMode mode = player.getGameMode();
        return mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR;
    }

    private double clampFinite(double value, double min, double max) {
        if (!Double.isFinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}

