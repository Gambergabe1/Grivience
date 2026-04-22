package io.papermc.Grivience.soul;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class SoulListener implements Listener {
    private final GriviencePlugin plugin;
    private final SoulManager soulManager;

    public SoulListener(GriviencePlugin plugin, SoulManager soulManager) {
        this.plugin = plugin;
        this.soulManager = soulManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        Location loc = event.getClickedBlock().getLocation();
        String soulId = soulManager.getSoulAt(loc);
        
        if (soulId != null) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            SkyBlockProfile profile = plugin.getProfileManager().getSelectedProfile(player);
            
            if (profile == null) {
                player.sendMessage(ChatColor.RED + "You must have a Skyblock profile selected to collect souls.");
                return;
            }
            
            if (profile.hasDiscoveredSoul(soulId)) {
                player.sendMessage(ChatColor.YELLOW + "You have already found this soul!");
                return;
            }
            
            profile.addDiscoveredSoul(soulId);
            int count = profile.getDiscoveredSouls().size();
            int total = soulManager.getTotalSouls();
            
            player.sendMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "SOUL COLLECTED!");
            player.sendMessage(ChatColor.GRAY + "You found a hidden soul!");
            player.sendMessage(ChatColor.GRAY + "Progress: " + ChatColor.LIGHT_PURPLE + count + ChatColor.GRAY + " / " + ChatColor.LIGHT_PURPLE + total);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        }
    }
}
