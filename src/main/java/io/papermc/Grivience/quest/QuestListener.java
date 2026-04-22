package io.papermc.Grivience.quest;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skills.SkyblockSkill;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import org.bukkit.event.player.PlayerChangedWorldEvent;

import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class QuestListener implements Listener {
    private final GriviencePlugin plugin;
    private final QuestManager questManager;

    public QuestListener(GriviencePlugin plugin, QuestManager questManager) {
        this.plugin = plugin;
        this.questManager = questManager;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        
        String target = event.getEntityType().name();
        
        // Check for custom monster tag
        org.bukkit.persistence.PersistentDataContainer pdc = event.getEntity().getPersistentDataContainer();
        org.bukkit.NamespacedKey customMobKey = new org.bukkit.NamespacedKey(plugin, "custom_monster");
        if (pdc.has(customMobKey, org.bukkit.persistence.PersistentDataType.STRING)) {
            String customId = pdc.get(customMobKey, org.bukkit.persistence.PersistentDataType.STRING);
            if (customId != null) {
                target = customId;
            }
        }
        
        questManager.advanceObjective(killer, QuestObjective.ObjectiveType.KILL_MOBS, target, 1);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        
        ItemStack item = event.getItem().getItemStack();
        String target = item.getType().name();
        
        // Check for custom item ID
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
            org.bukkit.NamespacedKey customItemKey = new org.bukkit.NamespacedKey(plugin, "custom-item-id");
            if (pdc.has(customItemKey, org.bukkit.persistence.PersistentDataType.STRING)) {
                String customId = pdc.get(customItemKey, org.bukkit.persistence.PersistentDataType.STRING);
                if (customId != null) {
                    target = customId;
                }
            }
        }
        
        questManager.advanceObjective(player, QuestObjective.ObjectiveType.COLLECT_ITEMS, target, item.getAmount());
    }

    public void onLevelUp(Player player, SkyblockSkill skill, int newLevel) {
        questManager.advanceObjective(player, QuestObjective.ObjectiveType.REACH_LEVEL, 
                skill.name(), newLevel);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();
        String minehubWorld = plugin.getConfig().getString("skyblock.minehub-world", "Minehub");
        
        if (worldName.equalsIgnoreCase(minehubWorld) || worldName.equalsIgnoreCase("Minehub")) {
            questManager.startQuest(player, "ironcrest_part1_arrival", QuestTriggerSource.SYSTEM, true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            List<ConversationQuest> active = questManager.activeQuests(player);
            if (!active.isEmpty()) {
                player.sendMessage(ChatColor.GOLD + "[Quest] " + ChatColor.YELLOW + "You have " + active.size() + " active quest(s). Use /quest progress");
            }

            // Auto-start Mining Level 20 quest if eligible
            checkAndStartMiningQuest(player);
        }, 40L);
    }

    private void checkAndStartMiningQuest(Player player) {
        if (plugin.getSkyblockLevelManager() == null || plugin.getSkyblockLevelManager().getSkillManager() == null) {
            return;
        }

        int miningLevel = plugin.getSkyblockLevelManager().getSkillManager().getLevel(player, SkyblockSkill.MINING);
        if (miningLevel < 20) {
            return;
        }

        // Check if quest is already completed or active
        List<ConversationQuest> activeQuests = questManager.activeQuests(player);
        for (ConversationQuest quest : activeQuests) {
            if ("mining_apprentice".equals(quest.id())) {
                return; // Already active
            }
        }

        // Check if already completed by trying to start it
        QuestManager.StartResult startResult = questManager.startQuest(player, "mining_apprentice", QuestTriggerSource.SYSTEM, false);
        if (startResult == QuestManager.StartResult.ALREADY_COMPLETED) {
            return;
        }

        // Auto-complete the quest if it was just started
        if (startResult == QuestManager.StartResult.STARTED) {
            QuestManager.CompleteResult completeResult = questManager.completeQuest(player, "mining_apprentice", QuestTriggerSource.SYSTEM, true);
            if (completeResult == QuestManager.CompleteResult.COMPLETED) {
                player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "[New Quest!] " + ChatColor.YELLOW + "Mining Apprentice - Auto-completed!");
            }
        }
    }
}
