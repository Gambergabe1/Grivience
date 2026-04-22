package io.papermc.Grivience.listener;

import io.papermc.Grivience.util.DropDeliveryUtil;
import io.papermc.Grivience.util.StackSizeSanitizer;
import io.papermc.Grivience.util.StackSizeSanitizer.SanitizedStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class InventoryStackSafetyListener implements Listener {
    private final JavaPlugin plugin;
    private static final ThreadLocal<Boolean> SANITIZING = ThreadLocal.withInitial(() -> false);

    public InventoryStackSafetyListener(JavaPlugin plugin) {
        this.plugin = plugin;
        this.plugin.getServer().getScheduler().runTaskTimer(
                this.plugin,
                () -> {
                    for (Player player : this.plugin.getServer().getOnlinePlayers()) {
                        sanitizePlayerState(player);
                    }
                },
                40L,
                40L
        );
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        scheduleSanitize(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            scheduleSanitize(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (SANITIZING.get()) {
            return;
        }
        if (!(event.getView().getPlayer() instanceof Player player)) {
            return;
        }
        // Just sanitize, don't call updateInventory here as it's redundant and causes loops.
        sanitizeCraftingInventory(event.getInventory(), player);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (SANITIZING.get()) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        boolean changed = sanitizeCraftingInventory(event.getInventory(), player);
        changed |= sanitizeCursor(player);
        if (changed) {
            player.updateInventory();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            scheduleSanitize(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            scheduleSanitize(player);
        }
    }

    private void scheduleSanitize(Player player) {
        if (player == null) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> sanitizePlayerState(player));
    }

    private void sanitizePlayerState(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (SANITIZING.get()) {
            return;
        }

        try {
            SANITIZING.set(true);
            boolean changed = sanitizePlayerInventory(player);
            if (player.getOpenInventory() != null && player.getOpenInventory().getTopInventory() instanceof CraftingInventory craftingInventory) {
                changed |= sanitizeCraftingInventory(craftingInventory, player);
            }
            changed |= sanitizeCursor(player);

            if (changed) {
                player.updateInventory();
            }
        } finally {
            SANITIZING.set(false);
        }
    }

    private boolean sanitizePlayerInventory(Player player) {
        PlayerInventory inventory = player.getInventory();
        boolean changed = false;
        List<ItemStack> overflow = new ArrayList<>();

        ItemStack[] storage = inventory.getStorageContents();
        for (int i = 0; i < storage.length; i++) {
            SanitizedStack sanitized = StackSizeSanitizer.sanitize(storage[i]);
            if (!isChanged(storage[i], sanitized.primary())) {
                continue;
            }
            storage[i] = sanitized.primary();
            overflow.addAll(sanitized.overflow());
            changed = true;
        }
        if (changed) {
            inventory.setStorageContents(storage);
        }

        ItemStack[] armor = inventory.getArmorContents();
        boolean armorChanged = false;
        for (int i = 0; i < armor.length; i++) {
            SanitizedStack sanitized = StackSizeSanitizer.sanitize(armor[i]);
            if (!isChanged(armor[i], sanitized.primary())) {
                continue;
            }
            armor[i] = sanitized.primary();
            overflow.addAll(sanitized.overflow());
            armorChanged = true;
        }
        if (armorChanged) {
            inventory.setArmorContents(armor);
            changed = true;
        }

        SanitizedStack offHand = StackSizeSanitizer.sanitize(inventory.getItemInOffHand());
        if (isChanged(inventory.getItemInOffHand(), offHand.primary())) {
            inventory.setItemInOffHand(offHand.primary());
            overflow.addAll(offHand.overflow());
            changed = true;
        }

        if (!overflow.isEmpty()) {
            spillToPlayer(player, overflow);
            changed = true;
        }

        return changed;
    }

    private boolean sanitizeCraftingInventory(CraftingInventory inventory, Player player) {
        if (inventory == null || player == null) {
            return false;
        }
        if (SANITIZING.get()) {
            return false;
        }

        boolean changed = false;
        ItemStack[] matrix = inventory.getMatrix();
        List<ItemStack> overflow = new ArrayList<>();
        ItemStack result = inventory.getResult();

        try {
            SANITIZING.set(true);
            for (int i = 0; i < matrix.length; i++) {
                SanitizedStack sanitized = StackSizeSanitizer.sanitize(matrix[i]);
                if (!isChanged(matrix[i], sanitized.primary())) {
                    continue;
                }
                matrix[i] = sanitized.primary();
                overflow.addAll(sanitized.overflow());
                changed = true;
            }

            SanitizedStack sanitizedResult = StackSizeSanitizer.sanitize(result);
            if (isChanged(result, sanitizedResult.primary())) {
                result = sanitizedResult.primary();
                changed = true;
            }
        } finally {
            SANITIZING.set(false);
        }

        if (changed) {
            inventory.setMatrix(matrix);
            inventory.setResult(result);
        }

        if (!overflow.isEmpty()) {
            spillToPlayer(player, overflow);
        }

        return changed;
    }

    private boolean sanitizeCursor(Player player) {
        if (player == null) {
            return false;
        }

        SanitizedStack sanitized = StackSizeSanitizer.sanitize(player.getItemOnCursor());
        if (!isChanged(player.getItemOnCursor(), sanitized.primary())) {
            return false;
        }

        player.setItemOnCursor(sanitized.primary());
        spillToPlayer(player, sanitized.overflow());
        return true;
    }

    private void spillToPlayer(Player player, List<ItemStack> overflow) {
        if (player == null || overflow == null || overflow.isEmpty()) {
            return;
        }
        DropDeliveryUtil.giveToInventoryOrDrop(player, overflow, null);
    }

    private boolean isChanged(ItemStack original, ItemStack sanitized) {
        if (original == null || original.getType().isAir()) {
            return false;
        }
        if (sanitized == null) {
            return true;
        }
        return original.getAmount() != sanitized.getAmount();
    }
}
