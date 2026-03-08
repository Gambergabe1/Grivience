package io.papermc.Grivience.quest;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

public final class QuestListener implements Listener {
    private final GriviencePlugin plugin;
    private final QuestManager questManager;

    public QuestListener(GriviencePlugin plugin, QuestManager questManager) {
        this.plugin = plugin;
        this.questManager = questManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            List<ConversationQuest> active = questManager.activeQuests(player.getUniqueId());
            if (active.isEmpty()) {
                return;
            }
            player.sendMessage(ChatColor.GOLD + "[Quest] " + ChatColor.YELLOW + "You have " + active.size() + " active quest(s). Use /quest progress");
        }, 40L);
    }
}
