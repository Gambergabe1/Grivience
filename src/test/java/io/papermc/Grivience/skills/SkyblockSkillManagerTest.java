package io.papermc.Grivience.skills;

import io.papermc.Grivience.stats.SkyblockLevelManager;
import io.papermc.Grivience.skyblock.profile.ProfileManager;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public final class SkyblockSkillManagerTest {

    @Test
    void xpThresholds_useUnifiedTable() {
        io.papermc.Grivience.GriviencePlugin plugin = mock(io.papermc.Grivience.GriviencePlugin.class);
        when(plugin.getConfig()).thenReturn(new org.bukkit.configuration.file.YamlConfiguration());
        SkyblockSkillManager manager = new SkyblockSkillManager(plugin, null);

        assertEquals(60, manager.getMaxLevel());
        assertEquals(50, manager.getMaxLevel(SkyblockSkill.DUNGEONEERING));
        
        // Level 1 cumulative is 50
        assertEquals(50.0D, manager.getXpForLevel(SkyblockSkill.COMBAT, 1), 1e-9);
        // Level 3 cumulative is 50+125+200 = 375
        assertEquals(375.0D, manager.getXpForLevel(SkyblockSkill.COMBAT, 3), 1e-9);
    }

    @Test
    void summaryHelpers_handleNullPlayerSafely() {
        io.papermc.Grivience.GriviencePlugin plugin = mock(io.papermc.Grivience.GriviencePlugin.class);
        when(plugin.getConfig()).thenReturn(new org.bukkit.configuration.file.YamlConfiguration());
        SkyblockSkillManager manager = new SkyblockSkillManager(plugin, null);

        assertEquals(SkyblockSkill.values().length, manager.getTrackedSkillCount());
        assertEquals(0, manager.getTotalSkillLevels(null));
        assertEquals(0.0D, manager.getSkillAverage((Player) null), 1e-9);
        assertNull(manager.getHighestSkill(null));
    }

    @Test
    void addXpQueuesActiveProfileSkillSnapshotSave() {
        io.papermc.Grivience.GriviencePlugin plugin = mock(io.papermc.Grivience.GriviencePlugin.class);
        when(plugin.getConfig()).thenReturn(new org.bukkit.configuration.file.YamlConfiguration());
        SkyblockLevelManager levelManager = mock(SkyblockLevelManager.class);
        ProfileManager profileManager = mock(ProfileManager.class);
        SkyBlockProfile profile = mock(SkyBlockProfile.class);
        Player player = mock(Player.class);

        when(plugin.getProfileManager()).thenReturn(profileManager);
        when(profileManager.getSelectedProfile(player)).thenReturn(profile);
        when(profile.getSkillXp("FARMING")).thenReturn(0L);

        SkyblockSkillManager manager = new SkyblockSkillManager(plugin, levelManager);
        manager.addXp(player, SkyblockSkill.FARMING, 10L);

        verify(profileManager).syncSkillSnapshotsAndQueueSave(player);
        verify(profile).setSkillXp("FARMING", 10L);
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, Map<String, Long>> counters(SkyblockLevelManager levelManager) throws Exception {
        Field field = SkyblockLevelManager.class.getDeclaredField("countersByPlayer");
        field.setAccessible(true);
        return (Map<UUID, Map<String, Long>>) field.get(levelManager);
    }
}
