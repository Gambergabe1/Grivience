package io.papermc.Grivience.skyblock.economy;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skyblock.profile.ProfileManager;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import org.bukkit.entity.Player;

/**
 * Profile-scoped bank operations (Skyblock-style).
 * <p>
 * Bank transactions move coins between the selected profile purse and bank and do not count as "earned/spent".
 */
public final class ProfileBankService {
    private final GriviencePlugin plugin;
    private final ProfileEconomyService profileEconomy;

    public ProfileBankService(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.profileEconomy = new ProfileEconomyService(plugin);
    }

    public SkyBlockProfile requireSelectedProfile(Player player) {
        return profileEconomy.requireSelectedProfile(player);
    }

    public long purseCoins(Player player) {
        double purse = profileEconomy.purse(player);
        if (!Double.isFinite(purse) || purse <= 0.0D) {
            return 0L;
        }
        return Math.max(0L, (long) Math.floor(purse + 1e-9D));
    }

    public long bankCoins(Player player) {
        SkyBlockProfile selectedProfile = profileEconomy.getSelectedProfile(player);
        if (selectedProfile == null) {
            return 0L;
        }
        SkyBlockProfile profile = resolveBankProfile(selectedProfile);

        double bank = profile.getBankBalance();
        if (!Double.isFinite(bank) || bank <= 0.0D) {
            return 0L;
        }
        return Math.max(0L, (long) Math.floor(bank + 1e-9D));
    }

    /**
     * Move coins from purse -> bank on the selected profile.
     */
    public boolean depositToBank(Player player, long coins) {
        if (player == null) {
            return false;
        }
        long safe = Math.max(0L, coins);
        if (safe == 0L) {
            return true;
        }

        ProfileManager manager = plugin.getProfileManager();
        if (manager == null) {
            return false;
        }

        SkyBlockProfile selectedProfile = requireSelectedProfile(player);
        if (selectedProfile == null) {
            return false;
        }
        SkyBlockProfile bankProfile = resolveBankProfile(selectedProfile);

        double purse = Math.max(0.0D, selectedProfile.getPurse());
        if (purse + 1e-9D < safe) {
            return false;
        }

        selectedProfile.setPurse(Math.max(0.0D, purse - safe));
        bankProfile.setBankBalance(Math.max(0.0D, bankProfile.getBankBalance() + safe));
        manager.saveProfile(selectedProfile);
        if (!selectedProfile.getProfileId().equals(bankProfile.getProfileId())) {
            manager.saveProfile(bankProfile);
        }
        return true;
    }

    /**
     * Move coins from bank -> purse on the selected profile.
     */
    public boolean withdrawFromBank(Player player, long coins) {
        if (player == null) {
            return false;
        }
        long safe = Math.max(0L, coins);
        if (safe == 0L) {
            return true;
        }

        ProfileManager manager = plugin.getProfileManager();
        if (manager == null) {
            return false;
        }

        SkyBlockProfile selectedProfile = requireSelectedProfile(player);
        if (selectedProfile == null) {
            return false;
        }
        SkyBlockProfile bankProfile = resolveBankProfile(selectedProfile);

        double bank = Math.max(0.0D, bankProfile.getBankBalance());
        if (bank + 1e-9D < safe) {
            return false;
        }

        bankProfile.setBankBalance(Math.max(0.0D, bank - safe));
        selectedProfile.setPurse(Math.max(0.0D, selectedProfile.getPurse() + safe));
        manager.saveProfile(selectedProfile);
        if (!selectedProfile.getProfileId().equals(bankProfile.getProfileId())) {
            manager.saveProfile(bankProfile);
        }
        return true;
    }

    private SkyBlockProfile resolveBankProfile(SkyBlockProfile selectedProfile) {
        ProfileManager manager = plugin.getProfileManager();
        if (manager == null) {
            return selectedProfile;
        }

        SkyBlockProfile resolved = manager.resolveSharedProfile(selectedProfile);
        return resolved != null ? resolved : selectedProfile;
    }
}
