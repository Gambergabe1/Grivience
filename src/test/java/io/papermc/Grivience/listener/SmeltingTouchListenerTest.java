package io.papermc.Grivience.listener;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.enchantment.EnchantmentRegistry;
import io.papermc.Grivience.enchantment.SkyblockEnchantment;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SmeltingTouchListenerTest {

    @Test
    void smeltingTouchSmeltsRawIronToIngot() {
        try (MockedStatic<EnchantmentRegistry> registryMock = mockStatic(EnchantmentRegistry.class)) {
            SkyblockEnchantment smeltingTouchEnchant = mock(SkyblockEnchantment.class);
            when(smeltingTouchEnchant.getId()).thenReturn("smelting_touch");
            registryMock.when(() -> EnchantmentRegistry.get("smelting_touch")).thenReturn(smeltingTouchEnchant);

            GriviencePlugin plugin = mock(GriviencePlugin.class);
            when(plugin.getName()).thenReturn("grivience");
            SmeltingTouchListener listener = new SmeltingTouchListener(plugin);

            Player player = mock(Player.class);
            PlayerInventory inventory = mock(PlayerInventory.class);
            ItemStack tool = mock(ItemStack.class);
            ItemMeta toolMeta = mock(ItemMeta.class);
            PersistentDataContainer pdc = mock(PersistentDataContainer.class);
            
            BlockDropItemEvent event = mock(BlockDropItemEvent.class);
            BlockState blockState = mock(BlockState.class);
            
            when(event.getPlayer()).thenReturn(player);
            when(event.getBlockState()).thenReturn(blockState);
            when(blockState.getType()).thenReturn(Material.IRON_ORE);
            
            when(player.getInventory()).thenReturn(inventory);
            when(inventory.getItemInMainHand()).thenReturn(tool);
            when(tool.getType()).thenReturn(Material.DIAMOND_PICKAXE);
            when(tool.hasItemMeta()).thenReturn(true);
            when(tool.getItemMeta()).thenReturn(toolMeta);
            when(toolMeta.getPersistentDataContainer()).thenReturn(pdc);
            
            when(pdc.get(any(NamespacedKey.class), eq(PersistentDataType.INTEGER))).thenReturn(1);

            List<Item> items = new ArrayList<>();
            Item itemEntity = mock(Item.class);
            ItemStack drops = mock(ItemStack.class);
            when(drops.getType()).thenReturn(Material.RAW_IRON);
            when(itemEntity.getItemStack()).thenReturn(drops);
            items.add(itemEntity);
            
            when(event.getItems()).thenReturn(items);

            listener.onBlockDrop(event);

            verify(drops).setType(Material.IRON_INGOT);
            verify(itemEntity).setItemStack(drops);
        }
    }

    @Test
    void netherGoldOreDropsIngotInsteadOfNuggets() {
        try (MockedStatic<EnchantmentRegistry> registryMock = mockStatic(EnchantmentRegistry.class)) {
            SkyblockEnchantment smeltingTouchEnchant = mock(SkyblockEnchantment.class);
            when(smeltingTouchEnchant.getId()).thenReturn("smelting_touch");
            registryMock.when(() -> EnchantmentRegistry.get("smelting_touch")).thenReturn(smeltingTouchEnchant);

            GriviencePlugin plugin = mock(GriviencePlugin.class);
            when(plugin.getName()).thenReturn("grivience");
            SmeltingTouchListener listener = new SmeltingTouchListener(plugin);

            Player player = mock(Player.class);
            PlayerInventory inventory = mock(PlayerInventory.class);
            ItemStack tool = mock(ItemStack.class);
            ItemMeta toolMeta = mock(ItemMeta.class);
            PersistentDataContainer pdc = mock(PersistentDataContainer.class);
            
            BlockDropItemEvent event = mock(BlockDropItemEvent.class);
            BlockState blockState = mock(BlockState.class);
            
            when(event.getPlayer()).thenReturn(player);
            when(event.getBlockState()).thenReturn(blockState);
            when(blockState.getType()).thenReturn(Material.NETHER_GOLD_ORE);
            
            when(player.getInventory()).thenReturn(inventory);
            when(inventory.getItemInMainHand()).thenReturn(tool);
            when(tool.getType()).thenReturn(Material.DIAMOND_PICKAXE);
            when(tool.hasItemMeta()).thenReturn(true);
            when(tool.getItemMeta()).thenReturn(toolMeta);
            when(toolMeta.getPersistentDataContainer()).thenReturn(pdc);
            
            when(pdc.get(any(NamespacedKey.class), eq(PersistentDataType.INTEGER))).thenReturn(1);

            List<Item> items = new ArrayList<>();
            Item item1 = mock(Item.class);
            Item item2 = mock(Item.class);
            ItemStack stack1 = mock(ItemStack.class);
            ItemStack stack2 = mock(ItemStack.class);
            
            when(stack1.getType()).thenReturn(Material.GOLD_NUGGET);
            when(stack2.getType()).thenReturn(Material.GOLD_NUGGET);
            when(item1.getItemStack()).thenReturn(stack1);
            when(item2.getItemStack()).thenReturn(stack2);
            items.add(item1);
            items.add(item2);
            
            when(event.getItems()).thenReturn(items);

            listener.onBlockDrop(event);

            // Verify first nugget stack became 1 ingot
            verify(stack1).setType(Material.GOLD_INGOT);
            verify(stack1).setAmount(1);
            verify(item1).setItemStack(stack1);
            
            // Verify second nugget stack was removed
            verify(item2).remove();
        }
    }
}
