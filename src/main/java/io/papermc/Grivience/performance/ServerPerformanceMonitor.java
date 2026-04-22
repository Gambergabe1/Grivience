package io.papermc.Grivience.performance;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class ServerPerformanceMonitor {
    public enum PerformanceTier {
        NORMAL,
        DEGRADED,
        CRITICAL
    }

    private final GriviencePlugin plugin;
    private final Map<String, Long> subsystemCounters = new HashMap<>();

    private BukkitTask sampleTask;
    private boolean enabled;
    private boolean adaptiveMitigationEnabled;
    private boolean logStateChanges;
    private double warningMspt;
    private double criticalMspt;
    private double recoveryMspt;
    private double smoothingFactor;
    private int degradedTransitionSamples;
    private int criticalTransitionSamples;
    private int recoveryTransitionSamples;
    private double estimatedMspt = 50.0D;
    private double estimatedTps = 20.0D;
    private long sampledTicks;
    private long lastSampleNanos = -1L;
    private PerformanceTier tier = PerformanceTier.NORMAL;
    private PerformanceTier pendingTier = PerformanceTier.NORMAL;
    private int pendingTierSamples;

    public ServerPerformanceMonitor(GriviencePlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        reload();
    }

    public void reload() {
        stopTask();
        loadConfig();
        resetState();
        if (!enabled) {
            return;
        }
        sampleTask = Bukkit.getScheduler().runTaskTimer(plugin, this::sampleTick, 1L, 1L);
    }

    public void shutdown() {
        stopTask();
        resetState();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAdaptiveMitigationEnabled() {
        return enabled && adaptiveMitigationEnabled;
    }

    public PerformanceTier getTier() {
        return tier;
    }

    public double getEstimatedMspt() {
        return estimatedMspt;
    }

    public double getEstimatedTps() {
        return estimatedTps;
    }

    public String getStatusSummary() {
        return String.format(
                Locale.ROOT,
                "%s (TPS %.2f, MSPT %.2f)",
                tier.name(),
                estimatedTps,
                estimatedMspt
        );
    }

    public long scalePeriod(long basePeriod, int degradedMultiplier, int criticalMultiplier) {
        if (basePeriod <= 0L || !isAdaptiveMitigationEnabled()) {
            return Math.max(1L, basePeriod);
        }
        int multiplier = switch (tier) {
            case DEGRADED -> Math.max(1, degradedMultiplier);
            case CRITICAL -> Math.max(Math.max(1, degradedMultiplier), criticalMultiplier);
            default -> 1;
        };
        return Math.max(1L, basePeriod * multiplier);
    }

    public int scaleBudget(int baseBudget, int degradedPercent, int criticalPercent, int minimumBudget) {
        if (baseBudget <= 0) {
            return Math.max(0, minimumBudget);
        }
        if (!isAdaptiveMitigationEnabled()) {
            return Math.max(minimumBudget, baseBudget);
        }

        int percent = switch (tier) {
            case DEGRADED -> clampPercent(degradedPercent);
            case CRITICAL -> clampPercent(criticalPercent);
            default -> 100;
        };
        int scaled = (int) Math.floor(baseBudget * (percent / 100.0D));
        return Math.max(minimumBudget, Math.min(baseBudget, scaled));
    }

    public boolean shouldProcess(String subsystemKey, int degradedModulo, int criticalModulo) {
        if (!isAdaptiveMitigationEnabled()) {
            return true;
        }

        int modulo = switch (tier) {
            case DEGRADED -> Math.max(1, degradedModulo);
            case CRITICAL -> Math.max(1, criticalModulo);
            default -> 1;
        };
        if (modulo <= 1) {
            return true;
        }

        long cycle = subsystemCounters.merge(subsystemKey, 1L, Long::sum) - 1L;
        return cycle % modulo == 0L;
    }

    private void loadConfig() {
        enabled = plugin.getConfig().getBoolean("performance-monitor.enabled", true);
        adaptiveMitigationEnabled = plugin.getConfig().getBoolean("performance-monitor.adaptive-mitigation.enabled", true);
        logStateChanges = plugin.getConfig().getBoolean("performance-monitor.logging.log-state-changes", true);
        warningMspt = Math.max(35.0D, plugin.getConfig().getDouble("performance-monitor.thresholds.warning-mspt", 47.5D));
        criticalMspt = Math.max(warningMspt + 1.0D, plugin.getConfig().getDouble("performance-monitor.thresholds.critical-mspt", 55.0D));
        recoveryMspt = Math.min(warningMspt - 1.0D, plugin.getConfig().getDouble("performance-monitor.thresholds.recovery-mspt", 45.0D));
        smoothingFactor = clamp(plugin.getConfig().getDouble("performance-monitor.smoothing-factor", 0.20D), 0.05D, 1.0D);
        degradedTransitionSamples = Math.max(1, plugin.getConfig().getInt("performance-monitor.transition-samples.degraded", 40));
        criticalTransitionSamples = Math.max(1, plugin.getConfig().getInt("performance-monitor.transition-samples.critical", 20));
        recoveryTransitionSamples = Math.max(1, plugin.getConfig().getInt("performance-monitor.transition-samples.recovery", 60));
    }

    private void resetState() {
        subsystemCounters.clear();
        estimatedMspt = 50.0D;
        estimatedTps = 20.0D;
        sampledTicks = 0L;
        lastSampleNanos = -1L;
        tier = PerformanceTier.NORMAL;
        pendingTier = PerformanceTier.NORMAL;
        pendingTierSamples = 0;
    }

    private void stopTask() {
        if (sampleTask != null) {
            sampleTask.cancel();
            sampleTask = null;
        }
    }

    private void sampleTick() {
        sampledTicks++;
        long now = System.nanoTime();
        if (lastSampleNanos < 0L) {
            lastSampleNanos = now;
            return;
        }

        double sampleMspt = (now - lastSampleNanos) / 1_000_000.0D;
        lastSampleNanos = now;
        if (!Double.isFinite(sampleMspt) || sampleMspt <= 0.0D) {
            return;
        }

        estimatedMspt += (sampleMspt - estimatedMspt) * smoothingFactor;
        estimatedTps = Math.min(20.0D, 1000.0D / Math.max(1.0D, estimatedMspt));
        updateTier();
    }

    private void updateTier() {
        PerformanceTier desiredTier = determineDesiredTier();
        if (desiredTier == tier) {
            pendingTier = tier;
            pendingTierSamples = 0;
            return;
        }

        if (pendingTier != desiredTier) {
            pendingTier = desiredTier;
            pendingTierSamples = 1;
        } else {
            pendingTierSamples++;
        }

        int requiredSamples = samplesRequiredFor(desiredTier);
        if (pendingTierSamples < requiredSamples) {
            return;
        }

        PerformanceTier previousTier = tier;
        tier = desiredTier;
        pendingTier = tier;
        pendingTierSamples = 0;

        if (logStateChanges) {
            plugin.getLogger().warning(buildStateChangeMessage(previousTier, tier));
        }
    }

    private PerformanceTier determineDesiredTier() {
        if (estimatedMspt >= criticalMspt) {
            return PerformanceTier.CRITICAL;
        }
        if (estimatedMspt >= warningMspt) {
            return PerformanceTier.DEGRADED;
        }
        if (estimatedMspt <= recoveryMspt) {
            return PerformanceTier.NORMAL;
        }
        return tier == PerformanceTier.CRITICAL ? PerformanceTier.DEGRADED : tier;
    }

    private int samplesRequiredFor(PerformanceTier desiredTier) {
        return switch (desiredTier) {
            case DEGRADED -> tier == PerformanceTier.CRITICAL ? recoveryTransitionSamples : degradedTransitionSamples;
            case CRITICAL -> criticalTransitionSamples;
            case NORMAL -> recoveryTransitionSamples;
        };
    }

    private String buildStateChangeMessage(PerformanceTier previousTier, PerformanceTier newTier) {
        return String.format(
                Locale.ROOT,
                "Server performance state changed from %s to %s (TPS %.2f, MSPT %.2f). Adaptive mitigation %s.",
                previousTier.name(),
                newTier.name(),
                estimatedTps,
                estimatedMspt,
                adaptiveMitigationEnabled ? "is active" : "is disabled"
        );
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clampPercent(int value) {
        return Math.max(1, Math.min(100, value));
    }
}
