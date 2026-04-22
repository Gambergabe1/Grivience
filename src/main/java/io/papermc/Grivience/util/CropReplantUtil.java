package io.papermc.Grivience.util;

import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class CropReplantUtil {
    private static final Set<Material> REPLANTABLE_CROPS = EnumSet.of(
            Material.WHEAT,
            Material.CARROTS,
            Material.POTATOES,
            Material.BEETROOTS,
            Material.NETHER_WART,
            Material.COCOA,
            Material.SWEET_BERRY_BUSH,
            Material.TORCHFLOWER_CROP,
            Material.PITCHER_CROP,
            Material.PITCHER_PLANT
    );

    private CropReplantUtil() {
    }

    public static boolean isReplantable(Material material) {
        return material != null && REPLANTABLE_CROPS.contains(material);
    }

    public static boolean isMature(Material material, BlockData data) {
        if (!isReplantable(material) || data == null) {
            return false;
        }
        if (data instanceof Ageable ageable) {
            return ageable.getAge() >= ageable.getMaximumAge();
        }
        return false;
    }

    public static Material replantCost(Material material) {
        if (material == null) {
            return null;
        }
        return switch (material) {
            case WHEAT -> Material.WHEAT_SEEDS;
            case CARROTS -> Material.CARROT;
            case POTATOES -> Material.POTATO;
            case BEETROOTS -> Material.BEETROOT_SEEDS;
            case NETHER_WART -> Material.NETHER_WART;
            case COCOA -> Material.COCOA_BEANS;
            case SWEET_BERRY_BUSH -> Material.SWEET_BERRIES;
            case TORCHFLOWER_CROP -> Material.TORCHFLOWER_SEEDS;
            case PITCHER_CROP, PITCHER_PLANT -> Material.PITCHER_POD;
            default -> null;
        };
    }

    public static BlockData seedlingData(Material originalType, BlockData originalData) {
        if (originalType == null || originalData == null) {
            return Material.AIR.createBlockData();
        }

        Material seedlingType = switch (originalType) {
            case PITCHER_PLANT -> Material.PITCHER_CROP;
            default -> originalType;
        };

        BlockData seedling = seedlingType == originalType
                ? originalData.clone()
                : seedlingType.createBlockData();

        if (seedling instanceof Bisected bisected) {
            bisected.setHalf(Bisected.Half.BOTTOM);
        }
        if (seedling instanceof Ageable ageable) {
            ageable.setAge(0);
        }
        return seedling;
    }

    public static boolean removeOneFromStacks(Collection<ItemStack> stacks, Material material) {
        if (stacks == null || stacks.isEmpty() || material == null) {
            return false;
        }
        Iterator<ItemStack> iterator = stacks.iterator();
        while (iterator.hasNext()) {
            ItemStack stack = iterator.next();
            if (stack == null || stack.getType() != material || stack.getAmount() <= 0) {
                continue;
            }
            if (stack.getAmount() == 1) {
                iterator.remove();
                return true;
            }
            stack.setAmount(stack.getAmount() - 1);
            return true;
        }
        return false;
    }

    public static boolean removeOneFromDropEntities(List<Item> items, Material material) {
        if (items == null || items.isEmpty() || material == null) {
            return false;
        }
        Iterator<Item> iterator = items.iterator();
        while (iterator.hasNext()) {
            Item entity = iterator.next();
            if (entity == null) {
                continue;
            }
            ItemStack stack = entity.getItemStack();
            if (stack == null || stack.getType() != material || stack.getAmount() <= 0) {
                continue;
            }
            if (stack.getAmount() == 1) {
                iterator.remove();
                entity.remove();
                return true;
            }
            stack.setAmount(stack.getAmount() - 1);
            entity.setItemStack(stack);
            return true;
        }
        return false;
    }

    public static boolean removeOneFromInventory(Player player, Material material) {
        if (player == null || material == null) {
            return false;
        }
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack stack = contents[slot];
            if (stack == null || stack.getType() != material || stack.getAmount() <= 0) {
                continue;
            }
            if (stack.getAmount() == 1) {
                player.getInventory().setItem(slot, null);
            } else {
                stack.setAmount(stack.getAmount() - 1);
                player.getInventory().setItem(slot, stack);
            }
            return true;
        }
        return false;
    }
}
