package io.papermc.Grivience.skyblock.listener;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skyblock.profile.ProfileManager;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.world.WorldLoadEvent;

import java.util.Locale;

/**
 * Enforces keep-inventory while applying a Skyblock-style coin loss on death.
 * <p>
 * Coins are removed from the selected profile purse only (bank is safe).
 */
public final class DeathPenaltyListener implements Listener {
    private final GriviencePlugin plugin;

    public DeathPenaltyListener(GriviencePlugin plugin) {
        this.plugin = plugin;
        enforceKeepInventoryForLoadedWorlds();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        if (event == null) {
            return;
        }
        enforceKeepInventory(event.getWorld());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        if (event == null) {
            return;
        }

        Player player = event.getEntity();
        if (player == null) {
            return;
        }

        // Always keep items, regardless of server gamerule.
        event.setKeepInventory(true);
        event.getDrops().clear();

        if (!plugin.getConfig().getBoolean("skyblock.death.purse-loss.enabled", true)) {
            return;
        }

        double percent = plugin.getConfig().getDouble("skyblock.death.purse-loss.percent", 0.5D);
        if (!Double.isFinite(percent) || percent <= 0.0D) {
            return;
        }
        percent = Math.max(0.0D, Math.min(1.0D, percent));

        ProfileManager profileManager = plugin.getProfileManager();
        if (profileManager == null) {
            return;
        }

        SkyBlockProfile profile = profileManager.getSelectedProfile(player);
        if (profile == null) {
            return;
        }

        long purseCoins = toCoins(profile.getPurse());
        if (purseCoins <= 0L) {
            return;
        }

        long lost = (long) Math.floor(purseCoins * percent + 1e-9D);
        if (lost <= 0L) {
            lost = 1L;
        }
        lost = Math.min(lost, purseCoins);

        profile.setPurse(Math.max(0.0D, profile.getPurse() - lost));
        profileManager.saveProfile(profile);

        player.sendMessage(ChatColor.RED + "You died and lost " + ChatColor.GOLD + formatCoins(lost) + ChatColor.RED + " coins!");
        player.sendMessage(ChatColor.GRAY + "Tip: " + ChatColor.YELLOW + "Store coins in the bank" + ChatColor.GRAY + " to keep them safe.");
    }

    private void enforceKeepInventoryForLoadedWorlds() {
        for (World world : plugin.getServer().getWorlds()) {
            enforceKeepInventory(world);
        }
    }

    private void enforceKeepInventory(World world) {
        if (world == null) {
            return;
        }
        // Keep inventory should always be enabled across the server.
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
    }

    private static long toCoins(double value) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            return 0L;
        }
        return Math.max(0L, (long) Math.floor(value + 1e-9D));
    }

    private static String formatCoins(long coins) {
        return String.format(Locale.US, "%,d", Math.max(0L, coins));
    }
}

