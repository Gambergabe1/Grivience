package io.papermc.Grivience.accessory;

import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessoryTypeTest {

    @Test
    void parseAcceptsCommonFormats() {
        assertEquals(AccessoryType.CRIMSON_CHARM, AccessoryType.parse("crimson_charm"));
        assertEquals(AccessoryType.CRIMSON_CHARM, AccessoryType.parse("crimson charm"));
        assertEquals(AccessoryType.CRIMSON_CHARM, AccessoryType.parse("crimson-charm"));
        assertNull(AccessoryType.parse("not_a_real_accessory"));
        assertNull(AccessoryType.parse(null));
    }

    @Test
    void familiesRetainHighestTierVariant() {
        Map<String, AccessoryType> byFamilyMaxTier = java.util.Arrays.stream(AccessoryType.values())
                .collect(Collectors.groupingBy(
                        AccessoryType::family,
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparingInt(AccessoryType::tier)),
                                optional -> optional.orElse(null)
                        )
                ));

        AccessoryType crimson = byFamilyMaxTier.get("crimson");
        assertNotNull(crimson);
        assertEquals(AccessoryType.CRIMSON_ARTIFACT, crimson);

        for (AccessoryType type : AccessoryType.values()) {
            assertTrue(type.tier() >= 1, "Accessory tier should start at 1: " + type.name());
            assertNotNull(type.family(), "Accessory family should not be null: " + type.name());
            assertNotNull(type.category(), "Accessory category should not be null: " + type.name());
        }
    }
}
