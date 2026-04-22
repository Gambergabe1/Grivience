package io.papermc.Grivience.skyblock.economy;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skyblock.profile.ProfileManager;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileBankServiceTest {

    @Test
    void depositUsesPersonalPurseButSharedCoopBank() {
        GriviencePlugin plugin = mock(GriviencePlugin.class);
        ProfileManager profileManager = mock(ProfileManager.class);
        Player player = mock(Player.class);

        UUID memberId = UUID.randomUUID();
        UUID sharedProfileId = UUID.randomUUID();
        SkyBlockProfile personalProfile = new SkyBlockProfile(memberId, "Member");
        personalProfile.setSharedProfileId(sharedProfileId);
        personalProfile.setPurse(500.0D);

        SkyBlockProfile sharedProfile = new SkyBlockProfile(UUID.randomUUID(), "Shared");
        sharedProfile.setBankBalance(1200.0D);

        when(plugin.getProfileManager()).thenReturn(profileManager);
        when(profileManager.getSelectedProfile(player)).thenReturn(personalProfile);
        when(profileManager.resolveSharedProfile(personalProfile)).thenReturn(sharedProfile);

        ProfileBankService service = new ProfileBankService(plugin);

        assertTrue(service.depositToBank(player, 200L));
        assertEquals(300.0D, personalProfile.getPurse());
        assertEquals(1400.0D, sharedProfile.getBankBalance());
        verify(profileManager).saveProfile(personalProfile);
        verify(profileManager).saveProfile(sharedProfile);
    }

    @Test
    void withdrawUsesSharedCoopBankButReturnsCoinsToPersonalPurse() {
        GriviencePlugin plugin = mock(GriviencePlugin.class);
        ProfileManager profileManager = mock(ProfileManager.class);
        Player player = mock(Player.class);

        UUID memberId = UUID.randomUUID();
        UUID sharedProfileId = UUID.randomUUID();
        SkyBlockProfile personalProfile = new SkyBlockProfile(memberId, "Member");
        personalProfile.setSharedProfileId(sharedProfileId);
        personalProfile.setPurse(75.0D);

        SkyBlockProfile sharedProfile = new SkyBlockProfile(UUID.randomUUID(), "Shared");
        sharedProfile.setBankBalance(600.0D);

        when(plugin.getProfileManager()).thenReturn(profileManager);
        when(profileManager.getSelectedProfile(player)).thenReturn(personalProfile);
        when(profileManager.resolveSharedProfile(personalProfile)).thenReturn(sharedProfile);

        ProfileBankService service = new ProfileBankService(plugin);

        assertTrue(service.withdrawFromBank(player, 125L));
        assertEquals(200.0D, personalProfile.getPurse());
        assertEquals(475.0D, sharedProfile.getBankBalance());
        verify(profileManager).saveProfile(personalProfile);
        verify(profileManager).saveProfile(sharedProfile);
    }

    @Test
    void bankBalanceLookupReadsSharedCoopBank() {
        GriviencePlugin plugin = mock(GriviencePlugin.class);
        ProfileManager profileManager = mock(ProfileManager.class);
        Player player = mock(Player.class);

        SkyBlockProfile personalProfile = new SkyBlockProfile(UUID.randomUUID(), "Member");
        personalProfile.setSharedProfileId(UUID.randomUUID());

        SkyBlockProfile sharedProfile = new SkyBlockProfile(UUID.randomUUID(), "Shared");
        sharedProfile.setBankBalance(999.0D);

        when(plugin.getProfileManager()).thenReturn(profileManager);
        when(profileManager.getSelectedProfile(player)).thenReturn(personalProfile);
        when(profileManager.resolveSharedProfile(personalProfile)).thenReturn(sharedProfile);

        ProfileBankService service = new ProfileBankService(plugin);

        assertEquals(999L, service.bankCoins(player));
        verify(profileManager, times(1)).resolveSharedProfile(personalProfile);
    }
}
