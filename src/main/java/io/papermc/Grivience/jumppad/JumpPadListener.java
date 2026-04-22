package io.papermc.Grivience.jumppad;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skills.SkyblockSkill;
import org.bukkit.ChatColor;
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
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Set;
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
    private final Map<UUID, ActiveLaunch> activeLaunches = new ConcurrentHashMap<>();
    private final Set<UUID> internalTeleports = ConcurrentHashMap.newKeySet();
    private long launchSequence = 0L;

    public JumpPadListener(GriviencePlugin plugin, JumpPadManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    private boolean checkRequirements(Player player, JumpPadManager.JumpPad pad) {
        if (player.hasPermission("grivience.admin.jumppad.bypass")) {
            return true;
        }

        // Skyblock Level
        if (pad.getMinSkyblockLevel() > 0) {
            int current = plugin.getSkyblockLevelManager() != null ? plugin.getSkyblockLevelManager().getLevel(player) : 0;
            if (current < pad.getMinSkyblockLevel()) {
                warnRequirement(player, "Skyblock Level", pad.getMinSkyblockLevel(), current);
                return false;
            }
        }

        // Skill Levels
        if (plugin.getSkyblockSkillManager() != null) {
            // Combat
            if (pad.getMinCombatLevel() > 0) {
                int current = plugin.getSkyblockSkillManager().getLevel(player, SkyblockSkill.COMBAT);
                if (current < pad.getMinCombatLevel()) {
                    warnRequirement(player, "Combat Level", pad.getMinCombatLevel(), current);
                    return false;
                }
            }

            // Mining
            if (pad.getMinMiningLevel() > 0) {
                int current = plugin.getSkyblockSkillManager().getLevel(player, SkyblockSkill.MINING);
                if (current < pad.getMinMiningLevel()) {
                    warnRequirement(player, "Mining Level", pad.getMinMiningLevel(), current);
                    return false;
                }
            }

            // Farming
            if (pad.getMinFarmingLevel() > 0) {
                int current = plugin.getSkyblockSkillManager().getLevel(player, SkyblockSkill.FARMING);
                if (current < pad.getMinFarmingLevel()) {
                    warnRequirement(player, "Farming Level", pad.getMinFarmingLevel(), current);
                    return false;
                }
            }
        }

        return true;
    }

    private void warnRequirement(Player player, String type, int required, int current) {
        player.sendMessage(ChatColor.RED + "You do not meet the requirements to use this Jump Pad!");
        player.sendMessage(ChatColor.GRAY + "Requires " + ChatColor.YELLOW + type + " " + required + ChatColor.GRAY + " (You have " + ChatColor.RED + current + ChatColor.GRAY + ")");
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5F, 1.0F);
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

        if (activeLaunches.containsKey(player.getUniqueId())) {
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

        // Check requirements
        if (!checkRequirements(player, pad)) {
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

        LaunchPlan plan = createLaunchPlan(player, pad, targetPoint);
        if (plan == null) {
            return;
        }

        launchPlayer(player, pad, plan);
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
            clearActiveLaunch(player.getUniqueId());
            lastLaunchAtMs.remove(player.getUniqueId());
        }
    }

    /**
     * Cancel active jump-pad travel if the player is teleported by some other system.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        if (internalTeleports.remove(playerId)) {
            return;
        }
        clearActiveLaunch(playerId);
    }

    /**
     * Apply a physics-driven launch velocity, then route the player to the configured exit halfway through the arc.
     */
    private void launchPlayer(Player player, JumpPadManager.JumpPad pad, LaunchPlan plan) {
        UUID playerId = player.getUniqueId();
        clearActiveLaunch(playerId);

        Location start = player.getLocation().clone();
        // Use the pad area's Y-level as launch baseline so arcs stay consistent across ramps/stairs.
        if (pad != null) {
            Location launchCenter = pad.getLaunchCenter();
            if (launchCenter != null
                    && launchCenter.getWorld() != null
                    && launchCenter.getWorld().equals(start.getWorld())) {
                start.setY(launchCenter.getY());
            }
        }
        player.setVelocity(plan.launchVelocity());
        player.setFallDistance(0);

        if (plugin.getConfig().getBoolean("jump-pads.hypixel-effects", true)) {
            player.getWorld().playSound(start, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.65F, 0.85F);
            player.getWorld().playSound(start, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.65F, 1.3F);
            player.getWorld().spawnParticle(Particle.CLOUD, start.clone().add(0.0D, 0.1D, 0.0D), 16, 0.25D, 0.08D, 0.25D, 0.01D);
            player.getWorld().spawnParticle(Particle.END_ROD, start.clone().add(0.0D, 0.2D, 0.0D), 8, 0.15D, 0.10D, 0.15D, 0.00D);
        }

        long token = ++launchSequence;
        BukkitTask midpointTeleportTask = plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> completeLaunch(playerId, token),
                plan.midpointDelayTicks()
        );
        BukkitTask releaseTask = plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> releaseLaunch(playerId, token),
                plan.midpointDelayTicks() + plan.releaseDelayTicks()
        );
        activeLaunches.put(playerId, new ActiveLaunch(token, plan, midpointTeleportTask, releaseTask));
    }

    private LaunchPlan createLaunchPlan(Player player, JumpPadManager.JumpPad pad, Location target) {
        if (player == null || pad == null || target == null || target.getWorld() == null) {
            return null;
        }

        Location start = player.getLocation().clone();
        Location launchCenter = pad.getLaunchCenter();
        if (launchCenter != null
                && launchCenter.getWorld() != null
                && launchCenter.getWorld().equals(start.getWorld())) {
            start.setY(launchCenter.getY());
        }

        Location arrival = target.clone().add(0.0D, plugin.getConfig().getDouble("jump-pads.arrival-y-offset", 0.15D), 0.0D);
        Location visualEnd = resolveVisualEnd(start, arrival);
        LaunchArc arc = calculateLaunchArc(start, visualEnd);

        double midpointRatio = clamp(
                plugin.getConfig().getDouble("jump-pads.midpoint-teleport-ratio", 0.5D),
                0.20D,
                1.0D
        );
        int midpointDelayTicks = Math.max(1, (int) Math.round(arc.flightTicks() * midpointRatio));
        int releaseDelayTicks = Math.max(2, plugin.getConfig().getInt("jump-pads.post-teleport-lock-ticks", 6));
        Vector exitVelocity = createExitVelocity(arrival);
        return new LaunchPlan(arrival, arc.velocity(), exitVelocity, midpointDelayTicks, releaseDelayTicks);
    }

    private Location resolveVisualEnd(Location start, Location arrival) {
        if (start == null || arrival == null) {
            return arrival;
        }
        if (start.getWorld() != null && start.getWorld().equals(arrival.getWorld())) {
            return arrival.clone().add(0.0D, 0.8D, 0.0D);
        }

        Vector forward = start.getDirection().clone();
        forward.setY(0.0D);
        if (forward.lengthSquared() < 1.0E-6D) {
            forward = new Vector(0.0D, 0.0D, 1.0D);
        } else {
            forward.normalize();
        }

        double previewDistance = sanitizePositive(
                plugin.getConfig().getDouble("jump-pads.cross-world-preview-distance", 8.0D),
                8.0D
        );
        double previewRise = sanitizePositive(
                plugin.getConfig().getDouble("jump-pads.cross-world-preview-rise", 3.0D),
                3.0D
        );
        return start.clone().add(forward.multiply(previewDistance)).add(0.0D, previewRise, 0.0D);
    }

    private Vector createExitVelocity(Location arrival) {
        if (arrival == null) {
            return new Vector(0.0D, 0.0D, 0.0D);
        }

        Vector direction = arrival.getDirection().clone();
        direction.setY(0.0D);
        if (direction.lengthSquared() < 1.0E-6D) {
            direction = new Vector(0.0D, 0.0D, 0.0D);
        } else {
            double horizontalSpeed = Math.max(0.0D, plugin.getConfig().getDouble("jump-pads.post-teleport-horizontal-speed", 0.0D));
            direction.normalize().multiply(horizontalSpeed);
        }
        direction.setY(Math.max(0.0D, plugin.getConfig().getDouble("jump-pads.post-teleport-vertical-speed", 0.10D)));
        return direction;
    }

    private void completeLaunch(UUID playerId, long token) {
        ActiveLaunch active = activeLaunches.get(playerId);
        if (active == null || active.token() != token) {
            return;
        }

        Player player = plugin.getServer().getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            clearActiveLaunch(playerId);
            return;
        }

        Location arrival = active.plan().arrivalLocation().clone();
        if (arrival.getWorld() == null) {
            clearActiveLaunch(playerId);
            return;
        }

        internalTeleports.add(playerId);
        boolean teleported = player.teleport(arrival);
        if (!teleported) {
            internalTeleports.remove(playerId);
            clearActiveLaunch(playerId);
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> internalTeleports.remove(playerId));

        player.setVelocity(active.plan().exitVelocity().clone());
        player.setFallDistance(0.0F);
        lastLaunchAtMs.put(playerId, System.currentTimeMillis());

        if (plugin.getConfig().getBoolean("jump-pads.hypixel-effects", true)) {
            arrival.getWorld().playSound(arrival, Sound.ENTITY_ENDERMAN_TELEPORT, 0.45F, 1.35F);
            arrival.getWorld().spawnParticle(Particle.PORTAL, arrival.clone().add(0.0D, 0.4D, 0.0D), 18, 0.25D, 0.35D, 0.25D, 0.05D);
            arrival.getWorld().spawnParticle(Particle.CLOUD, arrival.clone().add(0.0D, 0.2D, 0.0D), 12, 0.18D, 0.08D, 0.18D, 0.01D);
        }
    }

    private void releaseLaunch(UUID playerId, long token) {
        ActiveLaunch active = activeLaunches.get(playerId);
        if (active == null || active.token() != token) {
            return;
        }
        activeLaunches.remove(playerId, active);
    }

    private void clearActiveLaunch(UUID playerId) {
        ActiveLaunch removed = activeLaunches.remove(playerId);
        if (removed == null) {
            return;
        }
        if (removed.midpointTeleportTask() != null) {
            removed.midpointTeleportTask().cancel();
        }
        if (removed.releaseTask() != null) {
            removed.releaseTask().cancel();
        }
    }

    /**
     * Compute initial velocity using Minecraft-like player drag/gravity and a target flight time.
     */
    private LaunchArc calculateLaunchArc(Location start, Location end) {
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
        double horizontalTicks = horizontalDist / Math.max(0.25D, baseHorizontal);
        // Account for Y-delta explicitly so high/low targets get an appropriate time-of-flight.
        double verticalTicks = dy > 0.0D
                ? dy / Math.max(0.20D, baseVertical * 0.80D)
                : Math.abs(dy) / Math.max(1.20D, baseVertical * 2.00D);
        double estimatedTicks = Math.max(horizontalTicks, horizontalTicks + (verticalTicks * 0.55D));
        int flightTicks = (int) Math.round(clamp((configuredTicks + estimatedTicks) * 0.5D, MIN_FLIGHT_TICKS, MAX_FLIGHT_TICKS));

        if (horizontalDist < 0.01D) {
            double pureVerticalTicks = dy > 0.0D ? verticalTicks : configuredTicks;
            flightTicks = (int) Math.round(clamp(Math.max(configuredTicks, pureVerticalTicks), MIN_FLIGHT_TICKS, MAX_FLIGHT_TICKS));
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

        double minVertical = sanitizePositive(plugin.getConfig().getDouble("jump-pads.min-vertical-speed", 0.58D), 0.58D);
        double maxVertical = sanitizePositive(plugin.getConfig().getDouble("jump-pads.max-vertical-speed", 1.42D), 1.42D);
        if (maxVertical < minVertical) {
            maxVertical = minVertical;
        }
        if (dy > 0.0D) {
            // Higher targets need more headroom than flat/downward routes.
            maxVertical += Math.min(1.20D, dy * 0.03D);
        }
        if (dy < 0.0D) {
            // Lower targets should not be forced into an excessive upward pop.
            minVertical = Math.max(0.20D, minVertical * 0.55D);
        } else {
            minVertical = Math.max(minVertical, baseVertical);
        }

        vy = Math.max(vy, minVertical);
        vy = clamp(vy, minVertical, maxVertical);

        double horizontalSpeed = Math.hypot(vx, vz);
        double minHorizontal = horizontalDist < 1.50D ? 0.0D : Math.max(0.15D, baseHorizontal * 0.45D);
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

        return new LaunchArc(new Vector(vx, vy, vz), flightTicks);
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

    private record LaunchArc(Vector velocity, int flightTicks) {
    }

    private record LaunchPlan(
            Location arrivalLocation,
            Vector launchVelocity,
            Vector exitVelocity,
            int midpointDelayTicks,
            int releaseDelayTicks
    ) {
    }

    private record ActiveLaunch(
            long token,
            LaunchPlan plan,
            BukkitTask midpointTeleportTask,
            BukkitTask releaseTask
    ) {
    }
}
