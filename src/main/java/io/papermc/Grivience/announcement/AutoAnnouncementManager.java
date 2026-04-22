package io.papermc.Grivience.announcement;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Random;

public final class AutoAnnouncementManager {
    private final GriviencePlugin plugin;
    private final Random random = new Random();
    private BukkitTask task;

    public AutoAnnouncementManager(GriviencePlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        if (!plugin.getConfig().getBoolean("auto-announcements.enabled", true)) {
            return;
        }

        long intervalTicks = plugin.getConfig().getLong("auto-announcements.interval-minutes", 180) * 60 * 20;
        if (intervalTicks <= 0) {
            return;
        }

        task = Bukkit.getScheduler().runTaskTimer(plugin, this::broadcastNext, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void broadcastNext() {
        List<String> messages = plugin.getConfig().getStringList("auto-announcements.messages");
        if (messages.isEmpty()) {
            return;
        }

        String message = messages.get(random.nextInt(messages.size()));
        String discordLink = plugin.getConfig().getString("social.discord-link", "https://discord.gg/yourlink");
        
        String formatted = ChatColor.translateAlternateColorCodes('&', message.replace("{discord_link}", discordLink));
        Bukkit.broadcastMessage(formatted);
    }
}
