package io.papermc.Grivience.compactor;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Personal Compactor interaction and compact-on-pickup hooks.
 */
public final class PersonalCompactorListener implements Listener {
    private final PersonalCompactorManager manager;

    public PersonalCompactorListener(GriviencePlugin plugin, PersonalCompactorManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == null || !event.getAction().isRightClick()) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        if (player == null || manager == null || !manager.isEnabled()) {
            return;
        }
        if (!player.hasPermission(PersonalCompactorManager.USE_PERMISSION)) {
            return;
        }

        ItemStack hand = event.getItem();
        if (!manager.isCompactorItem(hand)) {
            return;
        }

        event.setCancelled(true);
        manager.openMenu(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (manager == null || !manager.isEnabled()) {
            return;
        }
        if (!player.hasPermission(PersonalCompactorManager.USE_PERMISSION)) {
            return;
        }
        manager.queueCompaction(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (manager == null) {
            return;
        }
        InventoryView view = event.getView();
        if (!manager.isMenu(view.getTopInventory())) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!manager.isMenuViewer(view.getTopInventory(), player)) {
            player.closeInventory();
            return;
        }
        if (!player.hasPermission(PersonalCompactorManager.USE_PERMISSION)) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use Personal Compactor.");
            player.closeInventory();
            return;
        }

        int rawSlot = event.getRawSlot();
        int topSize = view.getTopInventory().getSize();
        if (rawSlot < 0) {
            return;
        }

        Integer selectedSlot = manager.selectedSlot(view.getTopInventory());

        // Top inventory interactions.
        if (rawSlot < topSize) {
            if (rawSlot == 31) {
                player.closeInventory();
                return;
            }

            int compactorSlot = manager.compactorSlotFromGuiSlot(rawSlot);
            if (compactorSlot < 0) {
                return;
            }

            if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) {
                if (manager.clearSlot(player, compactorSlot)) {
                    player.sendMessage(ChatColor.YELLOW + "Cleared Personal Compactor slot " + (compactorSlot + 1) + ".");
                    manager.openMenu(player, compactorSlot);
                }
                return;
            }

            ItemStack cursor = event.getCursor();
            if (cursor == null || cursor.getType().isAir()) {
                if (!Objects.equals(selectedSlot, compactorSlot)) {
                    player.sendMessage(ChatColor.GREEN + "Selected Personal Compactor slot " + ChatColor.YELLOW + (compactorSlot + 1) + ChatColor.GREEN + ".");
                }
                manager.openMenu(player, compactorSlot);
                return;
            }

            applySlotFromItem(player, compactorSlot, cursor);
            return;
        }

        // Bottom inventory: click an item to auto-fill first available slot.
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }

        int target = selectedSlot != null ? selectedSlot : manager.firstAvailableConfiguredSlot(player);
        if (target < 0) {
            player.sendMessage(ChatColor.RED + "Select a Personal Compactor slot first, or clear one that is already configured.");
            return;
        }
        applySlotFromItem(player, target, clicked);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (manager == null) {
            return;
        }
        if (!manager.isMenu(event.getView().getTopInventory())) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            event.setCancelled(true);
            return;
        }
        if (!manager.isMenuViewer(event.getView().getTopInventory(), player)) {
            event.setCancelled(true);
            player.closeInventory();
            return;
        }
        event.setCancelled(true);
    }

    private void applySlotFromItem(Player player, int slot, ItemStack item) {
        PersonalCompactorManager.SetSlotResult result = manager.setSlotFromItem(player, slot, item);
        switch (result) {
            case SUCCESS -> {
                String name = displayName(item);
                player.sendMessage(ChatColor.GREEN + "Personal Compactor slot " + (slot + 1) + " target set to " + ChatColor.YELLOW + name + ChatColor.GREEN + ".");
                manager.compactNow(player);
                manager.openMenu(player, slot);
            }
            case LOCKED_SLOT -> player.sendMessage(ChatColor.RED + "That slot is locked by your current Personal Compactor tier.");
            case UNSUPPORTED_ITEM -> player.sendMessage(ChatColor.RED + "That item cannot be used as a compactor target.");
            case NO_PROFILE -> player.sendMessage(ChatColor.RED + "No Skyblock profile selected.");
            case INVALID_ITEM, INVALID_SLOT -> player.sendMessage(ChatColor.RED + "Invalid compactor slot or item.");
        }
    }

    private String displayName(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return "Unknown";
        }
        if (stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) {
            String stripped = ChatColor.stripColor(stack.getItemMeta().getDisplayName());
            if (stripped != null && !stripped.isBlank()) {
                return stripped;
            }
        }

        String raw = stack.getType().name().toLowerCase(Locale.ROOT);
        String[] words = raw.split("_");
        List<String> parts = new ArrayList<>(words.length);
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            parts.add(Character.toUpperCase(word.charAt(0)) + word.substring(1));
        }
        return String.join(" ", parts);
    }
}
