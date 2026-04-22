package io.papermc.Grivience.command;

import io.papermc.Grivience.item.CustomArmorManager;
import io.papermc.Grivience.item.CustomItemService;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminItemResolverTest {
    @Test
    void resolveSupportsArmorSetIdsWithUnderscores() {
        CustomItemService customItemService = mock(CustomItemService.class);
        CustomArmorManager customArmorManager = mock(CustomArmorManager.class);
        CustomArmorManager.CustomArmorSet armorSet = mock(CustomArmorManager.CustomArmorSet.class);
        ItemStack expected = mock(ItemStack.class);

        when(customItemService.createItemByKey(any())).thenReturn(null);
        when(customArmorManager.getArmorSet("harvesters_embrace")).thenReturn(armorSet);
        when(customArmorManager.createArmorPiece("harvesters_embrace", CustomArmorManager.ArmorPieceType.HELMET))
                .thenReturn(expected);

        AdminItemResolver resolver = new AdminItemResolver(customItemService, customArmorManager, null, null);

        assertSame(expected, resolver.resolve("harvesters_embrace_helmet", null));
    }

    @Test
    void allKeysIncludesSharedAdminGiveSources() {
        CustomItemService customItemService = mock(CustomItemService.class);
        CustomArmorManager customArmorManager = mock(CustomArmorManager.class);
        CustomArmorManager.CustomArmorSet armorSet = mock(CustomArmorManager.CustomArmorSet.class);

        when(customItemService.allItemKeys()).thenReturn(List.of("volta"));
        when(customArmorManager.getArmorSets()).thenReturn(Map.of("harvesters_embrace", armorSet));

        AdminItemResolver resolver = new AdminItemResolver(customItemService, customArmorManager, null, null);
        List<String> keys = resolver.allKeys();

        assertTrue(keys.contains("volta"));
        assertTrue(keys.contains("harvesters_embrace_helmet"));
    }
}
