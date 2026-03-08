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
                "storage.potion.upgrade"
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

    private YamlConfiguration loadPluginYml() {
        var stream = PluginPermissionsContractTest.class.getClassLoader().getResourceAsStream("plugin.yml");
        assertNotNull(stream, "plugin.yml should be available in test resources");
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
