package io.papermc.Grivience.wardrobe;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skyblock.profile.ProfileManager;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import io.papermc.Grivience.stats.SkyblockLevelManager;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WardrobeManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void selectedProfilesResolveToDifferentWardrobeKeys() throws Exception {
        ProfileManager profileManager = mock(ProfileManager.class);
        SkyblockLevelManager levelManager = mock(SkyblockLevelManager.class);
        GriviencePlugin plugin = mockPlugin(profileManager, levelManager);
        Player player = mock(Player.class);

        UUID ownerId = UUID.randomUUID();
        SkyBlockProfile alpha = new SkyBlockProfile(ownerId, "Alpha");
        SkyBlockProfile beta = new SkyBlockProfile(ownerId, "Beta");
        AtomicReference<SkyBlockProfile> selected = new AtomicReference<>(alpha);

        when(player.getUniqueId()).thenReturn(ownerId);
        when(profileManager.getSelectedProfile(player)).thenAnswer(invocation -> selected.get());
        when(profileManager.resolveSharedProfile(any(SkyBlockProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WardrobeManager manager = new WardrobeManager(plugin, tempDir.toFile());

        UUID alphaKey = resolveProfileId(manager, player);
        selected.set(beta);
        UUID betaKey = resolveProfileId(manager, player);

        assertEquals(alpha.getProfileId(), alphaKey);
        assertEquals(beta.getProfileId(), betaKey);
        assertNotEquals(alphaKey, betaKey);
    }

    @Test
    void coopMembersResolveToSharedCanonicalWardrobeKey() throws Exception {
        ProfileManager profileManager = mock(ProfileManager.class);
        SkyblockLevelManager levelManager = mock(SkyblockLevelManager.class);
        GriviencePlugin plugin = mockPlugin(profileManager, levelManager);

        Player firstPlayer = mock(Player.class);
        Player secondPlayer = mock(Player.class);
        UUID firstOwnerId = UUID.randomUUID();
        UUID secondOwnerId = UUID.randomUUID();
        UUID sharedProfileId = UUID.randomUUID();

        SkyBlockProfile firstMemberProfile = new SkyBlockProfile(firstOwnerId, "First");
        firstMemberProfile.setSharedProfileId(sharedProfileId);
        SkyBlockProfile secondMemberProfile = new SkyBlockProfile(secondOwnerId, "Second");
        secondMemberProfile.setSharedProfileId(sharedProfileId);

        when(firstPlayer.getUniqueId()).thenReturn(firstOwnerId);
        when(secondPlayer.getUniqueId()).thenReturn(secondOwnerId);
        when(profileManager.getSelectedProfile(firstPlayer)).thenReturn(firstMemberProfile);
        when(profileManager.getSelectedProfile(secondPlayer)).thenReturn(secondMemberProfile);
        when(profileManager.resolveSharedProfile(firstMemberProfile)).thenReturn(null);
        when(profileManager.resolveSharedProfile(secondMemberProfile)).thenReturn(null);

        WardrobeManager manager = new WardrobeManager(plugin, tempDir.toFile());

        assertEquals(sharedProfileId, resolveProfileId(manager, firstPlayer));
        assertEquals(sharedProfileId, resolveProfileId(manager, secondPlayer));
    }

    @Test
    void allowedSlotsMatchHypixelUnlockRules() {
        ProfileManager profileManager = mock(ProfileManager.class);
        SkyblockLevelManager levelManager = mock(SkyblockLevelManager.class);
        GriviencePlugin plugin = mockPlugin(profileManager, levelManager);
        Player player = mock(Player.class);

        UUID ownerId = UUID.randomUUID();
        SkyBlockProfile profile = new SkyBlockProfile(ownerId, "Solo");
        Set<String> permissions = new HashSet<>();

        when(player.getUniqueId()).thenReturn(ownerId);
        when(player.hasPermission(anyString())).thenAnswer(invocation -> permissions.contains(invocation.getArgument(0)));
        when(profileManager.getSelectedProfile(player)).thenReturn(profile);
        when(profileManager.resolveSharedProfile(profile)).thenReturn(profile);

        WardrobeManager manager = new WardrobeManager(plugin, tempDir.toFile());

        when(levelManager.getLevel(player)).thenReturn(4);
        assertEquals(0, manager.getAllowedSlots(player));

        when(levelManager.getLevel(player)).thenReturn(5);
        assertEquals(2, manager.getAllowedSlots(player));

        permissions.add("grivience.wardrobe.slots.vip");
        assertEquals(5, manager.getAllowedSlots(player));

        permissions.clear();
        permissions.add("grivience.wardrobe.slots.vipplus");
        assertEquals(7, manager.getAllowedSlots(player));

        permissions.clear();
        permissions.add("grivience.wardrobe.slots.vipplusplus");
        assertEquals(9, manager.getAllowedSlots(player));

        permissions.clear();
        permissions.add("grivience.wardrobe.slots.mvp");
        assertEquals(11, manager.getAllowedSlots(player));

        permissions.clear();
        permissions.add("grivience.wardrobe.slots.mvpplus");
        assertEquals(14, manager.getAllowedSlots(player));

        permissions.clear();
        permissions.add("grivience.wardrobe.slots.mvpplusplus");
        assertEquals(18, manager.getAllowedSlots(player));
    }

    @Test
    void lockedSlotsExplainWhetherLevelOrRankIsMissing() {
        ProfileManager profileManager = mock(ProfileManager.class);
        SkyblockLevelManager levelManager = mock(SkyblockLevelManager.class);
        GriviencePlugin plugin = mockPlugin(profileManager, levelManager);
        Player player = mock(Player.class);

        UUID ownerId = UUID.randomUUID();
        SkyBlockProfile profile = new SkyBlockProfile(ownerId, "Solo");

        when(player.getUniqueId()).thenReturn(ownerId);
        when(profileManager.getSelectedProfile(player)).thenReturn(profile);
        when(profileManager.resolveSharedProfile(profile)).thenReturn(profile);

        WardrobeManager manager = new WardrobeManager(plugin, tempDir.toFile());

        when(levelManager.getLevel(player)).thenReturn(3);
        assertEquals("SkyBlock Level 5", manager.getUnlockRequirement(player, 0));

        when(levelManager.getLevel(player)).thenReturn(5);
        assertEquals("VIP Rank", manager.getUnlockRequirement(player, 2));
        assertEquals("VIP+ Rank", manager.getUnlockRequirement(player, 5));
        assertEquals("VIP++ Rank", manager.getUnlockRequirement(player, 7));
        assertEquals("MVP Rank", manager.getUnlockRequirement(player, 9));
        assertEquals("MVP+ Rank", manager.getUnlockRequirement(player, 11));
        assertEquals("MVP++ Rank", manager.getUnlockRequirement(player, 14));
        assertTrue(manager.isUnlocked(player, 1));
    }

    private UUID resolveProfileId(WardrobeManager manager, Player player) throws Exception {
        Method method = WardrobeManager.class.getDeclaredMethod("resolveProfileId", Player.class);
        method.setAccessible(true);
        return (UUID) method.invoke(manager, player);
    }

    private GriviencePlugin mockPlugin(ProfileManager profileManager, SkyblockLevelManager levelManager) {
        GriviencePlugin plugin = mock(GriviencePlugin.class);
        when(plugin.getName()).thenReturn("Grivience");
        when(plugin.getLogger()).thenReturn(Logger.getLogger("WardrobeManagerTest"));
        when(plugin.getProfileManager()).thenReturn(profileManager);
        when(plugin.getSkyblockLevelManager()).thenReturn(levelManager);
        return plugin;
    }
}
