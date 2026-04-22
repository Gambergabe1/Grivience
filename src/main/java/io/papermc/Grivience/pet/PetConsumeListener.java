package io.papermc.Grivience.pet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Minimal listener that lets players convert pet items into unlocked pets by right-clicking.
 */
public final class PetConsumeListener implements Listener {
    private final PetManager petManager;

    public PetConsumeListener(PetManager petManager) {
        this.petManager = petManager;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        // Avoid firing twice for off-hand.
        if (event.getHand() != EquipmentSlot.HAND) return;

        ItemStack item = event.getItem();
        if (item == null) return;

        String petId = petManager.petId(item);
        if (petId == null) return;

        Player player = event.getPlayer();
        if (petManager.unlockPet(player, petId)) {
            event.setCancelled(true);
            long xp = petManager.getStoredXp(item);
            if (xp > 0) {
                petManager.setPetXp(player, petId, xp);
            }
            item.setAmount(item.getAmount() - 1);
            player.sendMessage(ChatColor.GOLD + "Added " + ChatColor.AQUA + petId + ChatColor.GOLD + " to your pet collection.");
        }
    }
}
