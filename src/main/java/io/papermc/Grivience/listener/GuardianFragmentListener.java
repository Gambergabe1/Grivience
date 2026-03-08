package io.papermc.Grivience.listener;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.gui.GuardianRecipeGui;
import io.papermc.Grivience.item.CustomItemService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.Set;

public final class GuardianFragmentListener implements Listener {
    private static final Set<String> GUARDIAN_ARMOR_IDS = Set.of(
            "GUARDIAN_HELM",
            "GUARDIAN_CHESTPLATE",
            "GUARDIAN_LEGGINGS",
            "GUARDIAN_BOOTS"
    );

    private final GriviencePlugin plugin;
    private final CustomItemService itemService;
    private final GuardianRecipeGui recipeGui;

    public GuardianFragmentListener(GriviencePlugin plugin, CustomItemService itemService, GuardianRecipeGui recipeGui) {
        this.plugin = plugin;
        this.itemService = itemService;
        this.recipeGui = recipeGui;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.PLAYER_HEAD) {
            return;
        }

        String id = itemService.itemId(item);
        if ("GUARDIAN_FRAGMENT".equalsIgnoreCase(id)) {
            event.setCancelled(true);
            recipeGui.open(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (isHelmetSlotClick(event)) {
            ItemStack cursor = event.getCursor();
            if (isGuardianFragment(cursor)) {
                event.setCancelled(true);
                player.sendMessage("§cGuardian Fragments cannot be equipped in the helmet slot.");
                return;
            }

            if (event.getClick().isKeyboardClick() && event.getHotbarButton() >= 0) {
                ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
                if (isGuardianFragment(hotbarItem)) {
                    event.setCancelled(true);
                    player.sendMessage("§cGuardian Fragments cannot be equipped in the helmet slot.");
                    return;
                }
            }
        }

        if (event.isShiftClick() && isGuardianFragment(event.getCurrentItem()) && player.getInventory().getHelmet() == null) {
            event.setCancelled(true);
            player.sendMessage("§cGuardian Fragments cannot be equipped in the helmet slot.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (!containsGuardianFragment(event.getNewItems())) {
            return;
        }

        // Player inventory view helmet raw slot.
        if (event.getRawSlots().contains(5) || event.getInventorySlots().contains(39)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareGuardianArmorCraft(PrepareItemCraftEvent event) {
        if (!(event.getInventory() instanceof CraftingInventory inventory)) {
            return;
        }
        ItemStack result = event.getRecipe() == null ? null : event.getRecipe().getResult();
        String armorId = result == null ? null : itemService.itemId(result);
        int[] requiredSlots = guardianArmorSlots(armorId);
        if (requiredSlots == null) {
            return;
        }

        if (!hasValidGuardianArmorMatrix(inventory.getMatrix(), requiredSlots)) {
            inventory.setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraftGuardianArmor(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory() instanceof CraftingInventory inventory)) {
            return;
        }

        ItemStack result = event.getRecipe() == null ? null : event.getRecipe().getResult();
        String armorId = result == null ? null : itemService.itemId(result);
        int[] requiredSlots = guardianArmorSlots(armorId);
        if (requiredSlots == null) {
            return;
        }

        ItemStack[] matrix = inventory.getMatrix();
        if (!hasValidGuardianArmorMatrix(matrix, requiredSlots)) {
            event.setCancelled(true);
            player.sendMessage("§cYou need 10 Guardian Fragments in each slot of the correct armor shape.");
            return;
        }

        if (event.isShiftClick()) {
            event.setCancelled(true);
            player.sendMessage("§cShift-crafting Guardian Armor is disabled. Craft with normal click.");
            return;
        }

        // Vanilla consumes one item per occupied slot; subtract 9 now so total consumed is 10 per slot.
        for (int slot : requiredSlots) {
            ItemStack stack = matrix[slot];
            if (stack == null || stack.getType().isAir()) {
                event.setCancelled(true);
                player.sendMessage("§cYou need 10 Guardian Fragments in each slot of the correct armor shape.");
                return;
            }
            if (stack.getAmount() < 10) {
                event.setCancelled(true);
                player.sendMessage("§cYou need 10 Guardian Fragments in each slot of the correct armor shape.");
                return;
            }
            stack.setAmount(stack.getAmount() - 9);
            matrix[slot] = stack.getAmount() > 0 ? stack : null;
        }

        inventory.setMatrix(matrix);
    }

    private boolean isHelmetSlotClick(InventoryClickEvent event) {
        if (!(event.getClickedInventory() instanceof PlayerInventory)) {
            return false;
        }
        // PlayerInventory helmet slot index.
        return event.getSlot() == 39;
    }

    private boolean containsGuardianFragment(Map<Integer, ItemStack> newItems) {
        if (newItems == null || newItems.isEmpty()) {
            return false;
        }
        for (ItemStack stack : newItems.values()) {
            if (isGuardianFragment(stack)) {
                return true;
            }
        }
        return false;
    }

    private boolean isGuardianFragment(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        String id = itemService.itemId(stack);
        return "GUARDIAN_FRAGMENT".equalsIgnoreCase(id);
    }

    private int[] guardianArmorSlots(String armorId) {
        if (armorId == null || !GUARDIAN_ARMOR_IDS.contains(armorId)) {
            return null;
        }
        return switch (armorId) {
            case "GUARDIAN_HELM" -> new int[]{0, 1, 2, 3, 5};
            case "GUARDIAN_CHESTPLATE" -> new int[]{0, 2, 3, 4, 5, 6, 7, 8};
            case "GUARDIAN_LEGGINGS" -> new int[]{0, 1, 2, 3, 5, 6, 8};
            case "GUARDIAN_BOOTS" -> new int[]{0, 2, 3, 5};
            default -> null;
        };
    }

    private boolean hasValidGuardianArmorMatrix(ItemStack[] matrix, int[] requiredSlots) {
        if (matrix == null || matrix.length < 9 || requiredSlots == null) {
            return false;
        }

        boolean[] required = new boolean[9];
        for (int slot : requiredSlots) {
            if (slot < 0 || slot >= 9) {
                return false;
            }
            required[slot] = true;
        }

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = matrix[slot];
            if (required[slot]) {
                if (!isGuardianFragment(stack)) {
                    return false;
                }
                if (stack.getAmount() < 10) {
                    return false;
                }
                continue;
            }
            if (stack != null && !stack.getType().isAir()) {
                return false;
            }
        }

        return true;
    }
}
