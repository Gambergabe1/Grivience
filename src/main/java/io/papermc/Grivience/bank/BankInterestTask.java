package io.papermc.Grivience.bank;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skyblock.island.Island;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Periodically awards bank interest to SkyBlock profiles.
 * Interest is paid out every 31 hours (Hypixel style).
 */
public final class BankInterestTask implements Runnable {
    private static final long INTEREST_COOLDOWN_MS = TimeUnit.HOURS.toMillis(31);
    
    private final GriviencePlugin plugin;
    private final BankManager bankManager;

    public BankInterestTask(GriviencePlugin plugin, BankManager bankManager) {
        this.plugin = plugin;
        this.bankManager = bankManager;
    }

    @Override
    public void run() {
        if (plugin.getProfileManager() == null) return;

        long now = System.currentTimeMillis();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            SkyBlockProfile profile = plugin.getProfileManager().getSelectedProfile(player);
            if (profile == null) continue;

            if (now - profile.getLastSaveTime() < 0) continue; // safety

            // Check if it's time for interest
            long timeSinceLast = now - profile.getLastInterestTime();
            if (timeSinceLast >= INTEREST_COOLDOWN_MS) {
                payoutInterest(player, profile);
            }
        }
    }

    private void payoutInterest(Player player, SkyBlockProfile profile) {
        double balance = profile.getBankBalance();
        if (balance <= 0) {
            profile.setLastInterestTime(System.currentTimeMillis());
            return;
        }

        // Base rate: 2%
        double rate = 0.02;
        
        // Island Upgrade Bonus: +10% interest rate per level (additive to base rate? 
        // No, Hypixel usually does it as a percentage of the balance up to a cap.
        // We'll do: Base 2%, +0.5% per upgrade level.
        if (plugin.getIslandManager() != null) {
            Island island = plugin.getIslandManager().getIsland(player.getUniqueId());
            if (island != null) {
                rate += (island.getBankInterestUpgrade() * 0.005);
            }
        }

        double interest = balance * rate;
        
        // Cap interest at 250,000 coins (default)
        interest = Math.min(interest, 250000.0);
        
        if (interest > 0) {
            profile.setBankBalance(balance + interest);
            profile.setLastInterestTime(System.currentTimeMillis());
            
            player.sendMessage("");
            player.sendMessage(ChatColor.GOLD + "BANK INTEREST!");
            player.sendMessage(ChatColor.GRAY + "You earned " + ChatColor.GOLD + String.format("%,.1f", interest) + ChatColor.GRAY + " coins from interest.");
            player.sendMessage(ChatColor.YELLOW + "New Balance: " + ChatColor.GOLD + String.format("%,.1f", profile.getBankBalance()));
            player.sendMessage("");
        } else {
            profile.setLastInterestTime(System.currentTimeMillis());
        }
        
        plugin.getProfileManager().saveProfile(profile);
    }
}
