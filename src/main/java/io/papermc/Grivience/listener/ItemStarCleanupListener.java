package io.papermc.Grivience.listener;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.item.CustomItemService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Migration/cleanup listener: removes legacy Skyblock-style star prefixes (✪) from custom items.
 */
public final class ItemStarCleanupListener implements Listener {
    private final GriviencePlugin plugin;
    private final CustomItemService customItemService;

    public ItemStarCleanupListener(GriviencePlugin plugin, CustomItemService customItemService) {
        this.plugin = plugin;
        this.customItemService = customItemService;

        // One-time cleanup for plugin reloads where players may already be online.
        Bukkit.getScheduler().runTask(plugin, this::cleanupOnlinePlayers);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> cleanupPlayer(player));
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        Inventory inventory = event.getInventory();
        Bukkit.getScheduler().runTask(plugin, () -> cleanupInventory(inventory));
    }

    private void cleanupOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            cleanupPlayer(player);
        }
    }

    private void cleanupPlayer(Player player) {
        if (player == null) {
            return;
        }
        cleanupPlayerInventory(player.getInventory());
        cleanupInventory(player.getEnderChest());
    }

    private void cleanupPlayerInventory(PlayerInventory inventory) {
        if (inventory == null) {
            return;
        }

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            ItemStack cleaned = cleanupItem(item);
            if (cleaned != item) {
                inventory.setItem(slot, cleaned);
            }
        }

        ItemStack[] armor = inventory.getArmorContents();
        boolean changedArmor = false;
        for (int i = 0; i < armor.length; i++) {
            ItemStack original = armor[i];
            ItemStack cleaned = cleanupItem(original);
            if (cleaned != original) {
                armor[i] = cleaned;
                changedArmor = true;
            }
        }
        if (changedArmor) {
            inventory.setArmorContents(armor);
        }

        ItemStack offhand = inventory.getItemInOffHand();
        ItemStack cleanedOffhand = cleanupItem(offhand);
        if (cleanedOffhand != offhand) {
            inventory.setItemInOffHand(cleanedOffhand);
        }
    }

    private void cleanupInventory(Inventory inventory) {
        if (inventory == null) {
            return;
        }

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            ItemStack cleaned = cleanupItem(item);
            if (cleaned != item) {
                inventory.setItem(slot, cleaned);
            }
        }
    }

    private ItemStack cleanupItem(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return item;
        }

        // Only touch custom items produced by the plugin.
        if (customItemService == null || customItemService.itemId(item) == null) {
            return item;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return item;
        }

        String displayName = meta.getDisplayName();
        if (displayName == null || !containsStarSymbol(displayName)) {
            return item;
        }

        String cleaned = removeStarPrefix(displayName);
        if (cleaned.equals(displayName)) {
            return item;
        }

        meta.setDisplayName(cleaned);
        item.setItemMeta(meta);
        return item;
    }

    private String removeStarPrefix(String displayName) {
        if (displayName == null || displayName.isEmpty()) {
            return displayName;
        }

        int i = 0;
        int len = displayName.length();

        // Skip leading formatting (color codes + whitespace) before the visible first character.
        while (i < len) {
            char c = displayName.charAt(i);
            if (c == ChatColor.COLOR_CHAR && i + 1 < len) {
                i += 2;
                continue;
            }
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            break;
        }

        if (i >= len || !isStarSymbol(displayName.charAt(i))) {
            return displayName;
        }

        // Remove the star and any spacing after it. Keep the following color (rarity) code intact.
        i++;
        while (i < len && Character.isWhitespace(displayName.charAt(i))) {
            i++;
        }
        return displayName.substring(i);
    }

    private boolean containsStarSymbol(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (isStarSymbol(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean isStarSymbol(char c) {
        return c == '✪' || c == '★' || c == '☆';
    }
}
