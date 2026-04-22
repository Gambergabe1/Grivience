package io.papermc.Grivience.stats;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.dungeon.DungeonManager;
import io.papermc.Grivience.item.CustomArmorManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Skyblock-style mana pool: base mana + intelligence, passive regen, action bar display.
 */
public final class SkyblockManaManager implements Listener {
    private static final double EPSILON = 1e-6D;
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    private final GriviencePlugin plugin;
    private final SkyblockStatsManager statsManager;
    private final DungeonManager dungeonManager;
    private CustomArmorManager armorManager;
    private SkyblockCombatEngine combatEngine;

    private final Map<UUID, Double> manaByPlayer = new HashMap<>();

    private double baseMana;
    private double manaPerIntelligence;
    private double regenPercentPerSecond;
    private double regenFlatPerIntelligence;
    private long regenIntervalTicks;
    private long displayIntervalTicks;

    private BukkitTask tickTask;
    private int regenCursor;
    private long regenAccumulator;
    private int displayCursor;
    private long displayAccumulator;

    public SkyblockManaManager(GriviencePlugin plugin, SkyblockStatsManager statsManager, DungeonManager dungeonManager, CustomArmorManager armorManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
        this.dungeonManager = dungeonManager;
        this.armorManager = armorManager;
    }

    public void reload() {
        baseMana = plugin.getConfig().getDouble("mana.base", 100.0D);
        manaPerIntelligence = plugin.getConfig().getDouble("mana.mana-per-intelligence", 1.0D);
        regenPercentPerSecond = plugin.getConfig().getDouble("mana.regen.percent-per-second", 0.02D);
        regenFlatPerIntelligence = plugin.getConfig().getDouble("mana.regen.flat-per-intelligence", 0.1D);
        regenIntervalTicks = Math.max(1L, plugin.getConfig().getLong("mana.regen.interval-ticks", 20L));
        displayIntervalTicks = Math.max(1L, plugin.getConfig().getLong("mana.display.interval-ticks", 10L));
    }

    public void start() {
        reload();
        startTasks();
    }

    public void shutdown() {
        stopTasks();
        manaByPlayer.clear();
    }

    public double getMana(Player player) {
        if (player == null) {
            return Math.max(0.0D, baseMana);
        }
        return currentMana(player.getUniqueId(), maxMana(player));
    }

    public double maxMana(Player player) {
        return resolveManaStats(player).maxMana();
    }

    public boolean consume(Player player, double amount) {
        if (player == null || amount <= 0.0D) {
            return true;
        }

        // --- MANA REDUCTION BUFFS ---
        
        // Onmyoji Spirit Ward - Reduces mana cost by 15%
        if (hasOnmyojiSet(player)) {
            amount *= 0.85D;
        }
        
        // Pet Abilities (e.g., Sheep Pet)
        if (plugin.getPetManager() != null) {
            amount *= plugin.getPetManager().getManaCostMultiplier(player);
        }

        double current = getMana(player);
        if (current < amount - 0.0001D) {
            return false;
        }
        setMana(player, current - amount);
        return true;
    }

    private boolean hasOnmyojiSet(Player player) {
        if (armorManager == null) {
            return false;
        }
        int pieces = 0;
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            if (piece == null) {
                continue;
            }
            String setId = armorManager.getArmorSetId(piece);
            if ("onmyoji".equalsIgnoreCase(setId)) {
                pieces++;
            }
        }
        return pieces >= 4;
    }

    public void addMana(Player player, double amount) {
        if (player == null || amount <= 0.0D) {
            return;
        }
        setMana(player, getMana(player) + amount);
    }

    public void setMana(Player player, double value) {
        if (player == null) {
            return;
        }
        setMana(player.getUniqueId(), resolveManaStats(player).maxMana(), value);
    }

    public void setArmorManager(CustomArmorManager armorManager) {
        this.armorManager = armorManager;
    }

    public void setCombatEngine(SkyblockCombatEngine combatEngine) {
        this.combatEngine = combatEngine;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        setMana(event.getPlayer(), maxMana(event.getPlayer()));
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> setMana(event.getPlayer(), maxMana(event.getPlayer())));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manaByPlayer.remove(event.getPlayer().getUniqueId());
    }

    private void startTasks() {
        stopTasks();
        resetTaskState();
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    private void stopTasks() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        resetTaskState();
    }

    private void resetTaskState() {
        regenCursor = 0;
        regenAccumulator = 0L;
        displayCursor = 0;
        displayAccumulator = 0L;
    }

    private void tick() {
        Player[] online = Bukkit.getOnlinePlayers().toArray(Player[]::new);
        if (online.length == 0) {
            resetTaskState();
            return;
        }

        tickRegen(online);
        tickDisplay(online);
    }

    private void tickRegen(Player[] online) {
        long effectiveIntervalTicks = effectiveRegenIntervalTicks();
        regenAccumulator += online.length;
        int playersThisTick = (int) Math.min(online.length, regenAccumulator / effectiveIntervalTicks);
        if (playersThisTick <= 0) {
            return;
        }
        regenAccumulator %= effectiveIntervalTicks;

        double seconds = effectiveIntervalTicks / 20.0D;
        for (int i = 0; i < playersThisTick; i++) {
            Player player = online[(regenCursor + i) % online.length];
            ManaStats manaStats = resolveManaStats(player);
            double regen = (manaStats.maxMana() * regenPercentPerSecond * seconds)
                    + (manaStats.intelligence() * regenFlatPerIntelligence * seconds);
            setMana(player.getUniqueId(), manaStats.maxMana(), currentMana(player.getUniqueId(), manaStats.maxMana()) + regen);
        }
        regenCursor = (regenCursor + playersThisTick) % online.length;
    }

    private void tickDisplay(Player[] online) {
        long effectiveIntervalTicks = effectiveDisplayIntervalTicks();
        displayAccumulator += online.length;
        int playersThisTick = (int) Math.min(online.length, displayAccumulator / effectiveIntervalTicks);
        if (playersThisTick <= 0) {
            return;
        }
        displayAccumulator %= effectiveIntervalTicks;

        for (int i = 0; i < playersThisTick; i++) {
            Player player = online[(displayCursor + i) % online.length];
            if (dungeonManager != null && dungeonManager.getSession(player.getUniqueId()) != null) {
                continue;
            }
            sendHud(player);
        }
        displayCursor = (displayCursor + playersThisTick) % online.length;
    }

    public void sendHud(Player player) {
        ManaStats manaStats = resolveManaStats(player);
        double mana = Math.round(currentMana(player.getUniqueId(), manaStats.maxMana()));
        double maxMana = Math.round(manaStats.maxMana());
        double health = Math.round(player.getHealth());
        double maxHealth = Math.round(player.getMaxHealth());
        double defense = 0.0D;

        SkyblockPlayerStats stats = manaStats.stats();
        if (stats != null) {
            double mcMax = Math.max(1.0D, player.getMaxHealth());
            double ratio = player.getHealth() / mcMax;
            if (!Double.isFinite(ratio)) {
                ratio = 1.0D;
            }
            ratio = Math.max(0.0D, Math.min(1.0D, ratio));

            maxHealth = Math.round(stats.health());
            health = Math.round(stats.health() * ratio);
            defense = Math.round(stats.defense());
        }
        String legacy = ChatColor.RED + "" + (int) health + "/" + (int) maxHealth + "\u2764 Health"
                + ChatColor.GREEN + " " + (int) defense + "\u2748 Defense"
                + ChatColor.AQUA + " " + (int) mana + "/" + (int) maxMana + "\u270e Mana";
        player.sendActionBar(LEGACY_SERIALIZER.deserialize(legacy));
    }

    private long effectiveRegenIntervalTicks() {
        if (plugin.getServerPerformanceMonitor() == null) {
            return regenIntervalTicks;
        }
        return plugin.getServerPerformanceMonitor().scalePeriod(regenIntervalTicks, 2, 4);
    }

    private long effectiveDisplayIntervalTicks() {
        if (plugin.getServerPerformanceMonitor() == null) {
            return displayIntervalTicks;
        }
        return plugin.getServerPerformanceMonitor().scalePeriod(displayIntervalTicks, 2, 4);
    }

    private double currentMana(UUID playerId, double maxMana) {
        Double stored = manaByPlayer.get(playerId);
        double current = stored == null ? maxMana : stored;
        if (!Double.isFinite(current)) {
            current = maxMana;
        }

        double clamped = Math.max(0.0D, Math.min(maxMana, current));
        if (stored == null || Math.abs(clamped - current) > EPSILON) {
            manaByPlayer.put(playerId, clamped);
        }
        return clamped;
    }

    private void setMana(UUID playerId, double maxMana, double value) {
        double clamped = Math.max(0.0D, Math.min(maxMana, value));
        manaByPlayer.put(playerId, clamped);
    }

    private ManaStats resolveManaStats(Player player) {
        if (player == null) {
            return new ManaStats(Math.max(0.0D, baseMana), 0.0D, null);
        }

        if (combatEngine != null) {
            SkyblockPlayerStats stats = combatEngine.stats(player);
            double intelligence = stats == null ? 0.0D : stats.intelligence();
            double maxMana = Math.max(0.0D, baseMana + (intelligence * manaPerIntelligence));
            return new ManaStats(maxMana, intelligence, stats);
        }

        double intelligence = statsManager.getIntelligence(player);
        double armorBonus = 0.0D;
        if (armorManager != null) {
            for (ItemStack piece : player.getInventory().getArmorContents()) {
                if (piece != null) {
                    armorBonus += armorManager.manaBonus(piece);
                }
            }
        }
        double maxMana = Math.max(0.0D, baseMana + (intelligence * manaPerIntelligence) + armorBonus);
        return new ManaStats(maxMana, intelligence, null);
    }

    private record ManaStats(double maxMana, double intelligence, SkyblockPlayerStats stats) {
    }
}
