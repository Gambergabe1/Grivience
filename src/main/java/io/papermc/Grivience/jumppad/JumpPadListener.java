package io.papermc.Grivience.jumppad;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies a calculated velocity boost when a player steps on a configured jump pad launch block.
 * Uses player-motion physics tuned for Hypixel-style jump arcs.
 */
public final class JumpPadListener implements Listener {
    private static final double PLAYER_GRAVITY = 0.08D;
    private static final double HORIZONTAL_DRAG = 0.91D;
    private static final double VERTICAL_DRAG = 0.98D;
    private static final int MIN_FLIGHT_TICKS = 8;
    private static final int MAX_FLIGHT_TICKS = 50;

    private final GriviencePlugin plugin;
    private final JumpPadManager manager;
    private final Map<UUID, Long> lastLaunchAtMs = new ConcurrentHashMap<>();

    public JumpPadListener(GriviencePlugin plugin, JumpPadManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        // Ignore micro-movements inside the same block to reduce event spam.
        if (from != null
                && from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        // Avoid relaunching while airborne from an existing jump-pad launch.
        if (!player.isOnGround()) {
            return;
        }

        // Check if player is within any jump pad area
        JumpPadManager.JumpPad pad = manager.getPadAtLocation(to);
        if (pad == null) {
            return;
        }

        Location targetPoint = pad.getTargetCenter();
        if (targetPoint == null) {
            return;
        }

        long now = System.currentTimeMillis();
        long cooldownMs = Math.max(0L, plugin.getConfig().getLong("jump-pads.cooldown-ms", 450L));
        Long last = lastLaunchAtMs.get(player.getUniqueId());
        if (last != null && (now - last) < cooldownMs) {
            return;
        }

        launchPlayer(player, targetPoint);
        lastLaunchAtMs.put(player.getUniqueId(), now);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        long fallWindowMs = Math.max(0L, plugin.getConfig().getLong("jump-pads.fall-cancel-window-ms", 3000L));
        if (fallWindowMs <= 0L) {
            return;
        }

        Long lastLaunch = lastLaunchAtMs.get(player.getUniqueId());
        if (lastLaunch == null) {
            return;
        }
        if ((System.currentTimeMillis() - lastLaunch) <= fallWindowMs) {
            event.setCancelled(true);
            player.setFallDistance(0F);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player != null) {
            lastLaunchAtMs.remove(player.getUniqueId());
        }
    }

    /**
     * Apply a physics-driven launch velocity from the player's current location toward target.
     */
    private void launchPlayer(Player player, Location target) {
        Location start = player.getLocation().clone();
        // Aim slightly above the target block so players do not clip into the floor on arrival.
        Location end = target.clone().add(0.0D, 0.8D, 0.0D);
        Vector velocity = calculateVelocity(start, end);

        player.setVelocity(velocity);
        player.setFallDistance(0);

        if (plugin.getConfig().getBoolean("jump-pads.hypixel-effects", true)) {
            player.getWorld().playSound(start, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.65F, 0.85F);
            player.getWorld().playSound(start, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.65F, 1.3F);
            player.getWorld().spawnParticle(Particle.CLOUD, start.clone().add(0.0D, 0.1D, 0.0D), 16, 0.25D, 0.08D, 0.25D, 0.01D);
            player.getWorld().spawnParticle(Particle.END_ROD, start.clone().add(0.0D, 0.2D, 0.0D), 8, 0.15D, 0.10D, 0.15D, 0.00D);
        }
    }

    /**
     * Compute initial velocity using Minecraft-like player drag/gravity and a target flight time.
     */
    private Vector calculateVelocity(Location start, Location end) {
        double baseHorizontal = sanitizePositive(
                plugin.getConfig().getDouble("jump-pads.horizontal-velocity", 2.20D),
                2.20D
        );
        double baseVertical = plugin.getConfig().getDouble("jump-pads.vertical-boost", 1.04D);

        int configuredTicks = plugin.getConfig().getInt("jump-pads.arrival-teleport-delay-ticks", 15);
        configuredTicks = (int) clamp(configuredTicks, MIN_FLIGHT_TICKS, MAX_FLIGHT_TICKS);

        Vector delta = end.toVector().subtract(start.toVector());
        double dx = delta.getX();
        double dy = delta.getY();
        double dz = delta.getZ();

        double horizontalDist = Math.hypot(dx, dz);
        double distanceTicks = horizontalDist / Math.max(0.25D, baseHorizontal);
        int flightTicks = (int) Math.round(clamp((configuredTicks + distanceTicks) * 0.5D, MIN_FLIGHT_TICKS, MAX_FLIGHT_TICKS));

        if (horizontalDist < 0.01D) {
            flightTicks = configuredTicks;
        }

        double horizontalSeries = geometricSeriesSum(HORIZONTAL_DRAG, flightTicks);
        double vx = horizontalSeries > 0.0D ? dx / horizontalSeries : 0.0D;
        double vz = horizontalSeries > 0.0D ? dz / horizontalSeries : 0.0D;

        double verticalSeries = geometricSeriesSum(VERTICAL_DRAG, flightTicks);
        double gravityAccumulation = (VERTICAL_DRAG * PLAYER_GRAVITY / (1.0D - VERTICAL_DRAG))
                * (flightTicks - verticalSeries);

        double vy = verticalSeries > 0.0D
                ? (dy + gravityAccumulation) / verticalSeries
                : baseVertical;

        // Keep the launch feeling "pad-like": always a clear pop upward.
        vy = Math.max(vy, baseVertical);
        double minVertical = sanitizePositive(plugin.getConfig().getDouble("jump-pads.min-vertical-speed", 0.58D), 0.58D);
        double maxVertical = sanitizePositive(plugin.getConfig().getDouble("jump-pads.max-vertical-speed", 1.42D), 1.42D);
        if (maxVertical < minVertical) {
            maxVertical = minVertical;
        }
        vy = clamp(vy, minVertical, maxVertical);

        double horizontalSpeed = Math.hypot(vx, vz);
        double minHorizontal = Math.max(0.15D, baseHorizontal * 0.45D);
        double maxHorizontal = sanitizePositive(
                plugin.getConfig().getDouble("jump-pads.max-horizontal-speed", Math.max(3.85D, baseHorizontal * 2.30D)),
                Math.max(3.85D, baseHorizontal * 2.30D)
        );

        if (horizontalSpeed > maxHorizontal) {
            double scale = maxHorizontal / horizontalSpeed;
            vx *= scale;
            vz *= scale;
        } else if (horizontalSpeed > 1.0E-6 && horizontalSpeed < minHorizontal && horizontalDist > 0.01D) {
            double scale = minHorizontal / horizontalSpeed;
            vx *= scale;
            vz *= scale;
        }

        return new Vector(vx, vy, vz);
    }

    private double geometricSeriesSum(double ratio, int terms) {
        if (terms <= 0) {
            return 0.0D;
        }
        if (Math.abs(1.0D - ratio) < 1.0E-9) {
            return terms;
        }
        return (1.0D - Math.pow(ratio, terms)) / (1.0D - ratio);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double sanitizePositive(double value, double fallback) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            return fallback;
        }
        return value;
    }
}
