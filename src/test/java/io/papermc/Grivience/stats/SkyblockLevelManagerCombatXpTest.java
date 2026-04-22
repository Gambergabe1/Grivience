package io.papermc.Grivience.stats;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class SkyblockLevelManagerCombatXpTest {

    @Test
    void higherLevelMobsGrantMoreCombatXp() throws Exception {
        SkyblockLevelManager manager = configuredManager();

        LivingEntity levelSix = mockEntity(EntityType.ENDERMAN, 6, null, null, null);
        LivingEntity levelFifteen = mockEntity(EntityType.ENDERMAN, 15, null, null, null);

        // Test config: baseXp=1, levelsPerStep=3, xpPerStep=1
        // Formula: baseXp + (level / levelsPerStep) * xpPerStep
        // Level 6: 1 + (6/3)*1 = 3
        // Level 15: 1 + (15/3)*1 = 6
        assertEquals(3L, manager.resolveCombatSkillXp(levelSix));
        assertEquals(6L, manager.resolveCombatSkillXp(levelFifteen));
        assertTrue(manager.resolveCombatSkillXp(levelFifteen) > manager.resolveCombatSkillXp(levelSix));
    }

    @Test
    void empoweredMobsReceiveAnExtraCombatXpBonus() throws Exception {
        SkyblockLevelManager manager = configuredManager();

        LivingEntity empowered = mockEntity(EntityType.ENDERMAN, 15, null, null, (byte) 1);

        // Base XP for level 15: 1 + (15/3)*1 = 6
        // Empowered bonus: 2 XP
        // Total: 6 + 2 = 8
        assertEquals(8L, manager.resolveCombatSkillXp(empowered));
    }

    @Test
    void customBossesUseTheBossRewardFloor() throws Exception {
        SkyblockLevelManager manager = configuredManager();

        // Boss at level 5 (effective level is max(5, 15) = 15 due to boss minimum)
        // Boss formula: baseXp(24) + (15/2)*2 = 24 + 15 = 39
        // Note: actual result is 38 due to integer division behavior: (15/2) = 7 in Java
        // So: 24 + 7*2 = 24 + 14 = 38
        LivingEntity crimsonWarden = mockEntity(EntityType.ZOMBIE, 5, "crimson_warden", (byte) 1, null);
        assertEquals(38L, manager.resolveCombatSkillXp(crimsonWarden));
    }

    @Test
    void lowLevelPlayersReceiveBonusXpAgainstHighLevelMobs() throws Exception {
        SkyblockLevelManager manager = configuredManager();
        // Mock a player at level 5
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        // Set manager XP so player is level 5 (5 * 100 = 500 XP)
        manager.setXp(player.getUniqueId(), 500L);

        // Level 45 mob
        // Base XP: 1 + (45/3)*1 = 16
        // Player level 5 vs level 45: diff = 40, multiplier = 1.0 + min(9.0, 40*0.10) = 5.0
        // Total with player: 16 * 5.0 = 80
        LivingEntity highLevelMob = mockEntity(EntityType.ENDERMAN, 45, null, null, null);

        assertEquals(80L, manager.resolveCombatSkillXp(player, highLevelMob));
    }

    private static SkyblockLevelManager configuredManager() throws Exception {
        io.papermc.Grivience.GriviencePlugin plugin = mock(io.papermc.Grivience.GriviencePlugin.class);
        when(plugin.getName()).thenReturn("grivience");
        java.io.File tempDir = java.nio.file.Files.createTempDirectory("grivience-test").toFile();
        when(plugin.getDataFolder()).thenReturn(tempDir);
        when(plugin.getConfig()).thenReturn(new org.bukkit.configuration.file.YamlConfiguration());
        
        SkyblockLevelManager manager = new SkyblockLevelManager(plugin);
        setLongField(manager, "xpPerLevel", 100L);
        setLongField(manager, "combatSkillBaseXp", 1L);
        setIntField(manager, "combatSkillLevelsPerStep", 3);
        setLongField(manager, "combatSkillXpPerStep", 1L);
        setLongField(manager, "combatSkillEmpoweredBonusXp", 2L);
        setLongField(manager, "bossCombatSkillBaseXp", 24L);
        setIntField(manager, "bossCombatSkillLevelsPerStep", 2);
        setLongField(manager, "bossCombatSkillXpPerStep", 2L);
        setIntField(manager, "bossCombatSkillMinimumLevel", 15);
        return manager;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> customBossIds(SkyblockLevelManager manager) throws Exception {
        Field field = SkyblockLevelManager.class.getDeclaredField("customBossMonsterIds");
        field.setAccessible(true);
        return (Set<String>) field.get(manager);
    }

    private static LivingEntity mockEntity(
            EntityType type,
            Integer level,
            String customMonsterId,
            Byte bossMarker,
            Byte empoweredMarker
    ) {
        LivingEntity entity = mock(LivingEntity.class);
        PersistentDataContainer container = mock(PersistentDataContainer.class);
        Map<NamespacedKey, Integer> intValues = new HashMap<>();
        Map<NamespacedKey, String> stringValues = new HashMap<>();
        Map<NamespacedKey, Byte> byteValues = new HashMap<>();

        NamespacedKey monsterLevelKey = new NamespacedKey("grivience", "monster_level");
        NamespacedKey customMonsterKey = new NamespacedKey("grivience", "custom_monster");
        NamespacedKey bossMobKey = new NamespacedKey("grivience", "boss_mob");
        NamespacedKey empoweredKey = new NamespacedKey("grivience", "end_mines_empowered");

        if (level != null) {
            intValues.put(monsterLevelKey, level);
        }
        if (customMonsterId != null) {
            stringValues.put(customMonsterKey, customMonsterId);
        }
        if (bossMarker != null) {
            byteValues.put(bossMobKey, bossMarker);
        }
        if (empoweredMarker != null) {
            byteValues.put(empoweredKey, empoweredMarker);
        }

        when(entity.getType()).thenReturn(type);
        when(entity.getPersistentDataContainer()).thenReturn(container);

        // Mock has() method to check if key exists in the value maps
        when(container.has(any(NamespacedKey.class), eq(PersistentDataType.INTEGER)))
                .thenAnswer(invocation -> intValues.containsKey(invocation.getArgument(0)));
        when(container.has(any(NamespacedKey.class), eq(PersistentDataType.STRING)))
                .thenAnswer(invocation -> stringValues.containsKey(invocation.getArgument(0)));
        when(container.has(any(NamespacedKey.class), eq(PersistentDataType.BYTE)))
                .thenAnswer(invocation -> byteValues.containsKey(invocation.getArgument(0)));

        // Mock get() method
        when(container.get(any(NamespacedKey.class), eq(PersistentDataType.INTEGER)))
                .thenAnswer(invocation -> intValues.get(invocation.getArgument(0)));
        when(container.get(any(NamespacedKey.class), eq(PersistentDataType.STRING)))
                .thenAnswer(invocation -> stringValues.get(invocation.getArgument(0)));
        when(container.get(any(NamespacedKey.class), eq(PersistentDataType.BYTE)))
                .thenAnswer(invocation -> byteValues.get(invocation.getArgument(0)));
        return entity;
    }

    private static void setLongField(SkyblockLevelManager manager, String fieldName, long value) throws Exception {
        Field field = SkyblockLevelManager.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setLong(manager, value);
    }

    private static void setIntField(SkyblockLevelManager manager, String fieldName, int value) throws Exception {
        Field field = SkyblockLevelManager.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setInt(manager, value);
    }
}
