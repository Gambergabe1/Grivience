package io.papermc.Grivience.stats;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Booster Cookie and God Potion durations and effects.
 */
public final class BoosterCookieManager implements Listener {
    private final GriviencePlugin plugin;
    private final File file;
    private final Map<UUID, Long> cookieUntil = new ConcurrentHashMap<>();
    private final Map<UUID, Long> godPotionUntil = new ConcurrentHashMap<>();

    public BoosterCookieManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "cookie_data.yml");
        load();
        
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 100L, 100L); // Every 5 seconds
    }

    public void load() {
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        ConfigurationSection cookies = config.getConfigurationSection("cookies");
        if (cookies != null) {
            for (String key : cookies.getKeys(false)) {
                cookieUntil.put(UUID.fromString(key), cookies.getLong(key));
            }
        }
        
        ConfigurationSection potions = config.getConfigurationSection("god_potions");
        if (potions != null) {
            for (String key : potions.getKeys(false)) {
                godPotionUntil.put(UUID.fromString(key), potions.getLong(key));
            }
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Long> entry : cookieUntil.entrySet()) {
            config.set("cookies." + entry.getKey().toString(), entry.getValue());
        }
        for (Map.Entry<UUID, Long> entry : godPotionUntil.entrySet()) {
            config.set("god_potions." + entry.getKey().toString(), entry.getValue());
        }
        try {
            config.save(file);
        } catch (IOException ignored) {}
    }

    public boolean hasCookie(Player player) {
        return cookieUntil.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis();
    }

    public boolean hasGodPotion(Player player) {
        return godPotionUntil.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis();
    }

    public void consumeCookie(Player player, long durationMs) {
        long current = cookieUntil.getOrDefault(player.getUniqueId(), System.currentTimeMillis());
        if (current < System.currentTimeMillis()) current = System.currentTimeMillis();
        cookieUntil.put(player.getUniqueId(), current + durationMs);
        
        player.sendMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "BOOSTER COOKIE! " + ChatColor.GREEN + "You consumed a Booster Cookie! Flight and bonuses active.");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1f, 1f);
        player.setAllowFlight(true);
        save();
    }

    public void consumeGodPotion(Player player, long durationMs) {
        long current = godPotionUntil.getOrDefault(player.getUniqueId(), System.currentTimeMillis());
        if (current < System.currentTimeMillis()) current = System.currentTimeMillis();
        godPotionUntil.put(player.getUniqueId(), current + durationMs);
        
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "GOD POTION! " + ChatColor.GREEN + "You consumed a God Potion! Massive stat boosts active.");
        player.playSound(player.getLocation(), Sound.ITEM_BOTTLE_FILL_DRAGONBREATH, 1f, 1.5f);
        save();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (hasCookie(event.getPlayer())) {
            event.getPlayer().setAllowFlight(true);
        }
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Long until = cookieUntil.get(player.getUniqueId());
            if (until != null && until < now) {
                cookieUntil.remove(player.getUniqueId());
                player.setAllowFlight(player.getGameMode() == org.bukkit.GameMode.CREATIVE || player.getGameMode() == org.bukkit.GameMode.SPECTATOR);
                player.sendMessage(ChatColor.RED + "Your Booster Cookie has expired!");
                save();
            }
            
            Long gUntil = godPotionUntil.get(player.getUniqueId());
            if (gUntil != null && gUntil < now) {
                godPotionUntil.remove(player.getUniqueId());
                player.sendMessage(ChatColor.RED + "Your God Potion has expired!");
                save();
            }
        }
    }
}
