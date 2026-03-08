package io.papermc.Grivience.welcome.quest;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.Material;
import org.bukkit.ChatColor;

import java.util.HashSet;
import java.util.Set;

/**
 * Listener that tracks player actions for quest progress.
 */
public class QuestProgressListener implements Listener {
    private final GriviencePlugin plugin;
    private final QuestProgressManager progressManager;
    private final Set<Material> woodTypes = new HashSet<>();
    private final Set<Material> oreTypes = new HashSet<>();
    private final Set<Material> cropTypes = new HashSet<>();

    public QuestProgressListener(GriviencePlugin plugin, QuestProgressManager progressManager) {
        this.plugin = plugin;
        this.progressManager = progressManager;
        initializeMaterialSets();
    }

    private void initializeMaterialSets() {
        // Wood types
        woodTypes.add(Material.OAK_LOG);
        woodTypes.add(Material.SPRUCE_LOG);
        woodTypes.add(Material.BIRCH_LOG);
        woodTypes.add(Material.JUNGLE_LOG);
        woodTypes.add(Material.ACACIA_LOG);
        woodTypes.add(Material.DARK_OAK_LOG);

        // Ore types
        oreTypes.add(Material.COAL_ORE);
        oreTypes.add(Material.IRON_ORE);
        oreTypes.add(Material.GOLD_ORE);
        oreTypes.add(Material.DIAMOND_ORE);
        oreTypes.add(Material.EMERALD_ORE);
        oreTypes.add(Material.LAPIS_ORE);
        oreTypes.add(Material.REDSTONE_ORE);
        oreTypes.add(Material.COPPER_ORE);

        // Crop types
        cropTypes.add(Material.WHEAT);
        cropTypes.add(Material.CARROTS);
        cropTypes.add(Material.POTATOES);
        cropTypes.add(Material.BEETROOTS);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();

        // Check for mining quests
        for (WelcomeQuest quest : WelcomeQuestRegistry.getAll()) {
            if (progressManager.isQuestCompleted(player, quest.getId())) continue;

            switch (quest.getType()) {
                case MINE_BLOCKS -> {
                    if (isMiningBlock(blockType)) {
                        progressManager.incrementQuestProgress(player, quest.getId(), 1);
                        checkQuestCompletion(player, quest);
                    }
                }
                case MINE_SPECIFIC_BLOCK -> {
                    if (blockType == Material.COBBLESTONE || blockType == Material.STONE) {
                        progressManager.incrementQuestProgress(player, quest.getId(), 1);
                        checkQuestCompletion(player, quest);
                    }
                }
                case MINE_ORE -> {
                    if (oreTypes.contains(blockType)) {
                        progressManager.incrementQuestProgress(player, quest.getId(), 1);
                        checkQuestCompletion(player, quest);
                    }
                }
                case CHOP_WOOD, CHOP_SPECIFIC_WOOD -> {
                    if (woodTypes.contains(blockType)) {
                        progressManager.incrementQuestProgress(player, quest.getId(), 1);
                        checkQuestCompletion(player, quest);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) return;
        Player player = event.getEntity().getKiller();

        for (WelcomeQuest quest : WelcomeQuestRegistry.getAll()) {
            if (progressManager.isQuestCompleted(player, quest.getId())) continue;

            if (quest.getType() == QuestType.KILL_MOBS_COUNT || quest.getType() == QuestType.KILL_MOB || quest.getType() == QuestType.KILL_SPECIFIC_MOB) {
                progressManager.incrementQuestProgress(player, quest.getId(), 1);
                checkQuestCompletion(player, quest);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_ENTITY) return;
        Player player = event.getPlayer();

        for (WelcomeQuest quest : WelcomeQuestRegistry.getAll()) {
            if (progressManager.isQuestCompleted(player, quest.getId())) continue;

            if (quest.getType() == QuestType.CATCH_FISH || quest.getType() == QuestType.CATCH_SPECIFIC_FISH) {
                progressManager.incrementQuestProgress(player, quest.getId(), 1);
                checkQuestCompletion(player, quest);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        for (WelcomeQuest quest : WelcomeQuestRegistry.getAll()) {
            if (progressManager.isQuestCompleted(player, quest.getId())) continue;

            if (quest.getType() == QuestType.CRAFT_ITEM) {
                progressManager.incrementQuestProgress(player, quest.getId(), 1);
                checkQuestCompletion(player, quest);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Check for bazaar interaction
        if (event.getAction().toString().contains("RIGHT")) {
            for (WelcomeQuest quest : WelcomeQuestRegistry.getAll()) {
                if (progressManager.isQuestCompleted(player, quest.getId())) continue;

                if (quest.getType() == QuestType.OPEN_BAZAAR) {
                    // Simplified - would need actual bazaar interaction detection
                }
            }
        }
    }

    private boolean isMiningBlock(Material material) {
        return material == Material.STONE || material == Material.COBBLESTONE ||
               material == Material.DEEPSLATE || material == Material.NETHERRACK ||
               oreTypes.contains(material);
    }

    private void checkQuestCompletion(Player player, WelcomeQuest quest) {
        int progress = progressManager.getQuestProgress(player, quest.getId());
        if (quest.isComplete(progress)) {
            progressManager.completeQuest(player, quest.getId());
            giveRewards(player, quest);
        }
    }

    private void giveRewards(Player player, WelcomeQuest quest) {
        // Give money reward
        if (quest.getMoneyReward() > 0) {
            // Would integrate with economy system
            player.sendMessage(ChatColor.GOLD + "Reward: $" + quest.getMoneyReward());
        }

        // Give XP reward
        if (quest.getXpReward() > 0) {
            player.giveExp(quest.getXpReward());
            player.sendMessage(ChatColor.AQUA + "Reward: " + quest.getXpReward() + " XP");
        }

        // Play success sound
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
    }
}
