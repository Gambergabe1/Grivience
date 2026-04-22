package io.papermc.Grivience.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArmorDurabilityUtilTest {

    @Test
    void ensureArmorUnbreakableMarksArmorAndClearsDamage() {
        ItemStack helmet = mockItem(Material.IRON_HELMET, 12);

        ArmorDurabilityUtil.ensureArmorUnbreakable(helmet);

        ItemMeta updated = helmet.getItemMeta();
        assertTrue(updated.isUnbreakable());
        assertTrue(updated.getItemFlags().contains(ItemFlag.HIDE_UNBREAKABLE));
        assertTrue(((Damageable) updated).getDamage() == 0);
    }

    @Test
    void explicitBreakMarkerSkipsProtection() {
        ItemStack boots = mockItem(Material.DIAMOND_BOOTS, 0);

        ArmorDurabilityUtil.markExplicitlyBreakable(boots);
        ArmorDurabilityUtil.ensureArmorUnbreakable(boots);

        assertTrue(ArmorDurabilityUtil.isExplicitlyBreakable(boots));
        assertFalse(ArmorDurabilityUtil.shouldPreventDurabilityLoss(boots));
        assertFalse(boots.getItemMeta().isUnbreakable());
    }

    @Test
    void nonArmorItemsAreIgnored() {
        ItemStack stick = mockItem(Material.STICK, 0);

        ArmorDurabilityUtil.ensureArmorUnbreakable(stick);

        assertFalse(ArmorDurabilityUtil.shouldPreventDurabilityLoss(stick));
        assertFalse(stick.getItemMeta().isUnbreakable());
        verify(stick, never()).setItemMeta(any(ItemMeta.class));
    }

    private static ItemStack mockItem(Material material, int initialDamage) {
        ItemStack item = mock(ItemStack.class);
        Damageable meta = mockDamageableMeta(initialDamage);
        AtomicReference<ItemMeta> metaRef = new AtomicReference<>(meta);

        when(item.getType()).thenReturn(material);
        when(item.hasItemMeta()).thenReturn(true);
        when(item.getItemMeta()).thenAnswer(invocation -> metaRef.get());
        when(item.setItemMeta(any(ItemMeta.class))).thenAnswer(invocation -> {
            metaRef.set(invocation.getArgument(0));
            return true;
        });
        return item;
    }

    private static Damageable mockDamageableMeta(int initialDamage) {
        Damageable meta = mock(Damageable.class);
        PersistentDataContainer container = mockPersistentDataContainer();
        AtomicBoolean unbreakable = new AtomicBoolean(false);
        AtomicInteger damage = new AtomicInteger(initialDamage);
        Set<ItemFlag> flags = EnumSet.noneOf(ItemFlag.class);

        when(meta.isUnbreakable()).thenAnswer(invocation -> unbreakable.get());
        when(meta.getDamage()).thenAnswer(invocation -> damage.get());
        when(meta.getItemFlags()).thenAnswer(invocation -> Set.copyOf(flags));
        when(meta.getPersistentDataContainer()).thenReturn(container);

        org.mockito.Mockito.doAnswer(invocation -> {
            unbreakable.set(invocation.getArgument(0));
            return null;
        }).when(meta).setUnbreakable(anyBoolean());
        org.mockito.Mockito.doAnswer(invocation -> {
            damage.set(invocation.getArgument(0));
            return null;
        }).when(meta).setDamage(anyInt());
        org.mockito.Mockito.doAnswer(invocation -> {
            for (Object argument : invocation.getArguments()) {
                if (argument instanceof ItemFlag flag) {
                    flags.add(flag);
                    continue;
                }
                if (argument instanceof ItemFlag[] addedFlags) {
                    for (ItemFlag flag : addedFlags) {
                        if (flag != null) {
                            flags.add(flag);
                        }
                    }
                }
            }
            return null;
        }).when(meta).addItemFlags(any(ItemFlag[].class));

        return meta;
    }

    private static PersistentDataContainer mockPersistentDataContainer() {
        PersistentDataContainer container = mock(PersistentDataContainer.class);
        Map<NamespacedKey, Byte> values = new HashMap<>();

        when(container.get(any(NamespacedKey.class), eq(PersistentDataType.BYTE)))
                .thenAnswer(invocation -> values.get(invocation.getArgument(0)));
        org.mockito.Mockito.doAnswer(invocation -> {
            NamespacedKey key = invocation.getArgument(0);
            Byte value = invocation.getArgument(2);
            values.put(key, value);
            return null;
        }).when(container).set(any(NamespacedKey.class), eq(PersistentDataType.BYTE), any(Byte.class));

        return container;
    }
}
