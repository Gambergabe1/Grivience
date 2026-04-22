package io.papermc.Grivience.pet;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.stats.SkyblockLevelManager;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;

/**
 * Awards pet EXP from Skyblock-like skill actions, matching Skyblock's "pet EXP from Skill XP" behavior.
 *
 * This is intentionally separate from vanilla XP gain.
 */
public final class PetSkillXpListener implements Listener {
    private final GriviencePlugin plugin;
    private final PetManager petManager;
    private final SkyblockLevelManager levelManager;

    public PetSkillXpListener(GriviencePlugin plugin, PetManager petManager, SkyblockLevelManager levelManager) {
        this.plugin = plugin;
        this.petManager = petManager;
        this.levelManager = levelManager;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onMobKill(EntityDeathEvent event) {
        if (!petManager.isExpEnabled()) {
            return;
        }
        Player killer = event.getEntity().getKiller();
        if (killer == null || isBypassedGameMode(killer) || event.getEntity() instanceof Player) {
            return;
        }

        long skillXp = levelManager != null
                ? levelManager.resolveCombatSkillXp(event.getEntity())
                : skillXp("combat-kill");
        if (skillXp > 0L) {
            petManager.addSkillXp(killer, PetSkillType.COMBAT, skillXp);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!petManager.isExpEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null || isBypassedGameMode(player)) {
            return;
        }

        Material type = event.getBlock().getType();
        if (levelManager != null) {
            if (levelManager.isFarmingMaterial(type)) {
                if (event.getBlock().getBlockData() instanceof Ageable ageable && ageable.getAge() < ageable.getMaximumAge()) {
                    return;
                }
                long xp = skillXp("farming-harvest");
                if (xp > 0L) {
                    petManager.addSkillXp(player, PetSkillType.FARMING, xp);
                }
                return;
            }

            if (levelManager.isMiningMaterial(type)) {
                long xp = skillXp("mining-ore");
                if (xp > 0L) {
                    petManager.addSkillXp(player, PetSkillType.MINING, xp);
                }
                return;
            }

            if (levelManager.isForagingMaterial(type)) {
                long xp = skillXp("foraging-log");
                if (xp > 0L) {
                    petManager.addSkillXp(player, PetSkillType.FORAGING, xp);
                }
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onFish(PlayerFishEvent event) {
        if (!petManager.isExpEnabled()) {
            return;
        }
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null || isBypassedGameMode(player)) {
            return;
        }
        long xp = skillXp("fishing-catch");
        if (xp > 0L) {
            petManager.addSkillXp(player, PetSkillType.FISHING, xp);
        }
    }

    private long skillXp(String actionKey) {
        if (actionKey == null || actionKey.isBlank()) {
            return 0L;
        }
        String petKey = "skyblock-pets.skill-xp." + actionKey;
        long configured = plugin.getConfig().getLong(petKey, Long.MIN_VALUE);
        if (configured != Long.MIN_VALUE) {
            return Math.max(0L, configured);
        }

        // Backward-compatible fallback: use the "skyblock-leveling.action-xp" values if pet XP isn't configured.
        String fallbackKey = "skyblock-leveling.action-xp." + actionKey;
        return Math.max(0L, plugin.getConfig().getLong(fallbackKey, 0L));
    }

    private boolean isBypassedGameMode(Player player) {
        if (player == null) {
            return true;
        }
        GameMode mode = player.getGameMode();
        return mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR;
    }
}


