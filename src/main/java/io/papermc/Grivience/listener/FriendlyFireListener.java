package io.papermc.Grivience.listener;

import io.papermc.Grivience.dungeon.DungeonManager;
import io.papermc.Grivience.party.PartyManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class FriendlyFireListener implements Listener {
    private final DungeonManager dungeonManager;
    private final PartyManager partyManager;

    public FriendlyFireListener(DungeonManager dungeonManager, PartyManager partyManager) {
        this.dungeonManager = dungeonManager;
        this.partyManager = partyManager;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null) {
            return;
        }
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }
        if (isFriendly(attacker, victim)) {
            event.setCancelled(true);
        }
    }

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    private boolean isFriendly(Player first, Player second) {
        return dungeonManager.areInSameSession(first.getUniqueId(), second.getUniqueId())
                || partyManager.areInSameParty(first.getUniqueId(), second.getUniqueId());
    }
}
