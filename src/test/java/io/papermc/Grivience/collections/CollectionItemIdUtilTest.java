package io.papermc.Grivience.collections;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CollectionItemIdUtilTest {

    @Test
    void normalizeTrackedItemIdRepairsCompactorAliases() {
        assertEquals("carrot", CollectionItemIdUtil.normalizeTrackedItemId("carrot_item"));
        assertEquals("potato", CollectionItemIdUtil.normalizeTrackedItemId("potato_item"));
        assertEquals("melon", CollectionItemIdUtil.normalizeTrackedItemId("melon_slice"));
        assertEquals("nether_wart", CollectionItemIdUtil.normalizeTrackedItemId("nether_stalk"));
        assertEquals("enchanted_nether_wart", CollectionItemIdUtil.normalizeTrackedItemId("enchanted_nether_stalk"));
    }

    @Test
    void trackedItemIdForMaterialUsesCollectionIds() {
        assertEquals("melon", CollectionItemIdUtil.trackedItemIdForMaterial(Material.MELON_SLICE));
        assertEquals("iron_ingot", CollectionItemIdUtil.trackedItemIdForMaterial(Material.RAW_IRON));
        assertEquals("nether_wart", CollectionItemIdUtil.trackedItemIdForMaterial(Material.NETHER_WART));
        assertEquals("carrot", CollectionItemIdUtil.trackedItemIdForMaterial(Material.CARROT));
    }
}
