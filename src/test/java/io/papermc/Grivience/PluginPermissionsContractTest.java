package io.papermc.Grivience;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginPermissionsContractTest {

    @Test
    void pluginYmlDeclaresExpectedPermissions() {
        YamlConfiguration pluginYml = loadPluginYml();
        List<String> requiredPermissions = List.of(
                "grivience.admin",
                "grivience.admin.globalevent",
                "grivience.admin.mineevent",
                "grivience.announce",
                "grivience.collections.admin",
                "grivience.endmines.build",
                "grivience.fasttravel.admin",
                "grivience.hub.build",
                "grivience.island.bypass",
                "grivience.island.bypass.admin",
                "grivience.minion.bypass",
                "grivience.storage.admin",
                "grivience.storage.admin.other",
                "grivience.visit.bypass",
                "grivience.visit.guestlimit.unlimited",
                "grivience.visit.guestlimit.vip",
                "grivience.visit.guestlimit.mvp",
                "grivience.visit.guestlimit.mvpplus",
                "grivience.visit.guestlimit.youtuber",
                "grivience.wardrobe.slots.vip",
                "grivience.wardrobe.slots.vipplus",
                "grivience.wardrobe.slots.mvp",
                "grivience.wardrobe.slots.mvpplus",
                "storage.personal",
                "storage.personal.upgrade",
                "storage.vault",
                "storage.vault.upgrade",
                "storage.ender",
                "storage.ender.upgrade",
                "storage.backpack",
                "storage.backpack.upgrade",
                "storage.warehouse",
                "storage.warehouse.upgrade",
                "storage.accessory",
                "storage.accessory.upgrade",
                "storage.potion",
                "storage.potion.upgrade",
                "grivience.personalcompactor"
        );

        for (String node : requiredPermissions) {
            assertTrue(pluginYml.contains("permissions." + node), "Missing permission node: " + node);
        }
    }

    @Test
    void eventCommandsUseScopedAdminPermissions() {
        YamlConfiguration pluginYml = loadPluginYml();
        assertEquals("grivience.admin.mineevent", pluginYml.getString("commands.mineevent.permission"));
        assertEquals("grivience.admin.globalevent", pluginYml.getString("commands.globalevent.permission"));
    }

    @Test
    void accessoryCommandIsDeclaredWithAccessoryPermission() {
        YamlConfiguration pluginYml = loadPluginYml();
        assertTrue(pluginYml.contains("commands.accessory"), "Missing /accessory command declaration");
        assertEquals("storage.accessory", pluginYml.getString("commands.accessory.permission"));
    }

    @Test
    void compactorCommandIsDeclaredWithCompactorPermission() {
        YamlConfiguration pluginYml = loadPluginYml();
        assertTrue(pluginYml.contains("commands.compactor"), "Missing /compactor command declaration");
        assertEquals("grivience.personalcompactor", pluginYml.getString("commands.compactor.permission"));
    }

    @Test
    void portalRouteCommandIsDeclaredWithAdminPermission() {
        YamlConfiguration pluginYml = loadPluginYml();
        assertTrue(pluginYml.contains("commands.portalroute"), "Missing /portalroute command declaration");
        assertEquals("grivience.admin", pluginYml.getString("commands.portalroute.permission"));
    }

    @Test
    void mountainCommandIsDeclaredWithAdminPermission() {
        YamlConfiguration pluginYml = loadPluginYml();
        assertTrue(pluginYml.contains("commands.mountain"), "Missing /mountain command declaration");
        assertEquals("grivience.admin", pluginYml.getString("commands.mountain.permission"));
    }

    @Test
    void dragonWatcherCommandUsageDocumentsForceSummon() {
        YamlConfiguration pluginYml = loadPluginYml();
        assertTrue(pluginYml.contains("commands.dragonwatcher"), "Missing /dragonwatcher command declaration");
        assertTrue(
                pluginYml.getString("commands.dragonwatcher.usage", "").contains("forcesummon <type> <tier> [mutation]"),
                "/dragonwatcher usage should document the force summon syntax"
        );
    }

    private YamlConfiguration loadPluginYml() {
        var stream = PluginPermissionsContractTest.class.getClassLoader().getResourceAsStream("plugin.yml");
        assertNotNull(stream, "plugin.yml should be available in test resources");
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
