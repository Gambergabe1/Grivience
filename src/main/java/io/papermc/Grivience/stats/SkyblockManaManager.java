package io.papermc.Grivience.stats;

import io.papermc.Grivience.dungeon.DungeonManager;
import io.papermc.Grivience.item.CustomArmorManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Skyblock-style mana pool: base mana + intelligence, passive regen, action bar display.
 */
public final class SkyblockManaManager implements Listener {
    private final JavaPlugin plugin;
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

    private BukkitTask regenTask;
    private BukkitTask displayTask;

    public SkyblockManaManager(JavaPlugin plugin, SkyblockStatsManager statsManager, DungeonManager dungeonManager, CustomArmorManager armorManager) {
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

        UUID playerId = player.getUniqueId();
        double max = maxMana(player);
        Double stored = manaByPlayer.get(playerId);
        double current = stored == null ? max : stored;

        if (!Double.isFinite(current)) {
            current = max;
        }

        double clamped = Math.max(0.0D, Math.min(max, current));
        if (stored == null || Math.abs(clamped - current) > 1e-6D) {
            manaByPlayer.put(playerId, clamped);
        }
        return clamped;
    }

    public double maxMana(Player player) {
        if (player == null) {
            return Math.max(0.0D, baseMana);
        }

        if (combatEngine != null) {
            double intelligence = combatEngine.stats(player).intelligence();
            return Math.max(0.0D, baseMana + (intelligence * manaPerIntelligence));
        }

        double intelligence = statsManager.getIntelligence(player);
        double armorBonus = 0.0D;
        if (armorManager != null) {
            for (ItemStack piece : player.getInventory().getArmorContents()) {
                armorBonus += armorManager.manaBonus(piece);
            }
        }
        return Math.max(0.0D, baseMana + (intelligence * manaPerIntelligence) + armorBonus);
    }

    public boolean consume(Player player, double amount) {
        if (player == null || amount <= 0.0D) {
            return true;
        }
        
        // Onmyoji Spirit Ward - Reduces mana cost by 15%
        if (hasOnmyojiSet(player)) {
            amount *= 0.85;
        }

        double current = getMana(player);
        if (current < amount - 0.0001D) {
            return false;
        }
        setMana(player, current - amount);
        return true;
    }

    private boolean hasOnmyojiSet(Player player) {
        if (armorManager == null) return false;
        int pieces = 0;
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            if (piece == null) continue;
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
        double clamped = Math.max(0.0D, Math.min(maxMana(player), value));
        manaByPlayer.put(player.getUniqueId(), clamped);
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
        regenTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickRegen, regenIntervalTicks, regenIntervalTicks);
        displayTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickDisplay, displayIntervalTicks, displayIntervalTicks);
    }

    private void stopTasks() {
        if (regenTask != null) {
            regenTask.cancel();
            regenTask = null;
        }
        if (displayTask != null) {
            displayTask.cancel();
            displayTask = null;
        }
    }

    private void tickRegen() {
        double seconds = regenIntervalTicks / 20.0D;
        for (Player player : Bukkit.getOnlinePlayers()) {
            double max = maxMana(player);
            double intelligence = combatEngine != null ? combatEngine.stats(player).intelligence() : statsManager.getIntelligence(player);
            double regen = (max * regenPercentPerSecond * seconds) + (intelligence * regenFlatPerIntelligence * seconds);
            setMana(player, getMana(player) + regen);
        }
    }

    private void tickDisplay() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Avoid clashing with dungeon action bar spam by letting dungeon tick override if present.
            if (dungeonManager != null && dungeonManager.getSession(player.getUniqueId()) != null) {
                continue;
            }
            sendHud(player);
        }
    }

    public void sendHud(Player player) {
        double mana = Math.round(getMana(player));
        double maxMana = Math.round(maxMana(player));
        double health = Math.round(player.getHealth());
        double maxHealth = Math.round(player.getMaxHealth());
        double defense = 0.0D;

        if (combatEngine != null) {
            SkyblockPlayerStats stats = combatEngine.stats(player);
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
        }
        String legacy = ChatColor.RED + "" + (int) health + "/" + (int) maxHealth + "❤ Health"
                + ChatColor.GREEN + " " + (int) defense + "❈ Defense"
                + ChatColor.AQUA + " " + (int) mana + "/" + (int) maxMana + "✎ Mana";
        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(legacy));
    }
}

