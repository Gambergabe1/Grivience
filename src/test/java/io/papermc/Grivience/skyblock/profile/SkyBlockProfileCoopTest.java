package io.papermc.Grivience.skyblock.profile;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkyBlockProfileCoopTest {

    @Test
    void coopSharedProfileIdSurvivesSerialization() {
        UUID ownerId = UUID.randomUUID();
        UUID sharedProfileId = UUID.randomUUID();
        SkyBlockProfile profile = new SkyBlockProfile(ownerId, "Alpha");
        profile.setSharedProfileId(sharedProfileId);

        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection section = yaml.createSection("profile");
        profile.save(section);

        SkyBlockProfile restored = SkyBlockProfile.fromSection(section);
        assertNotNull(restored);
        assertTrue(restored.isCoopMemberProfile());
        assertEquals(sharedProfileId, restored.getSharedProfileId());
        assertEquals(sharedProfileId, restored.getCanonicalProfileId());
    }

    @Test
    void profileDoesNotTreatSelfAsSharedCanonicalId() {
        SkyBlockProfile profile = new SkyBlockProfile(UUID.randomUUID(), "Solo");

        profile.setSharedProfileId(profile.getProfileId());

        assertFalse(profile.isCoopMemberProfile());
        assertEquals(profile.getProfileId(), profile.getCanonicalProfileId());
    }
}
