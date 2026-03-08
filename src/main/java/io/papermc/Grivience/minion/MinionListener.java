package io.papermc.Grivience.minion;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class MinionListener implements Listener {
    private final MinionManager minionManager;
    private final MinionGuiManager minionGuiManager;

    public MinionListener(MinionManager minionManager, MinionGuiManager minionGuiManager) {
        this.minionManager = minionManager;
        this.minionGuiManager = minionGuiManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlaceMinion(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ItemStack inHand = event.getItem();
        if (!minionManager.isMinionItem(inHand)) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }
        event.setCancelled(true);
        minionManager.tryPlaceMinion(
                event.getPlayer(),
                event.getClickedBlock(),
                event.getBlockFace(),
                inHand
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractAtMinion(PlayerInteractAtEntityEvent event) {
        handleEntityInteract(event.getPlayer(), event.getRightClicked(), true);
        if (minionManager.isMinionEntity(event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractMinion(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof org.bukkit.entity.ArmorStand) {
            // Armor stands are handled by PlayerInteractAtEntityEvent.
            return;
        }
        handleEntityInteract(event.getPlayer(), event.getRightClicked(), false);
        if (minionManager.isMinionEntity(event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMinionDamageByEntity(EntityDamageByEntityEvent event) {
        MinionInstance minion = minionManager.getMinionByEntity(event.getEntity());
        if (minion == null) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (!minionManager.canManageMinion(player, minion)) {
            player.sendMessage(ChatColor.RED + "You cannot damage someone else's minion.");
            return;
        }
        player.sendMessage(ChatColor.YELLOW + "Use right-click to manage this minion.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMinionGeneralDamage(EntityDamageEvent event) {
        if (minionManager.isMinionEntity(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    private void handleEntityInteract(Player player, Entity entity, boolean preciseEvent) {
        MinionInstance minion = minionManager.getMinionByEntity(entity);
        if (minion == null) {
            return;
        }
        if (!minionManager.canManageMinion(player, minion)) {
            player.sendMessage(ChatColor.RED + "You cannot manage this minion.");
            return;
        }

        if (player.isSneaking() && !preciseEvent) {
            if (minionManager.pickupMinion(player, minion.id())) {
                minionGuiManager.openOverview(player);
            }
            return;
        }
        minionGuiManager.openMinion(player, minion.id());
    }
}
