package io.papermc.Grivience.enchantment;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Skyblock 100% Accurate Anvil GUI for combining enchantments.
 * Matches the exact layout, styling, and mechanics of Skyblock's anvil system.
 */
public final class SkyblockAnvilGui implements Listener {
    // Skyblock-accurate inventory title
    private static final String TITLE = ChatColor.translateAlternateColorCodes('&', "&8Anvil");

    // Skyblock-accurate slot positions
    private static final int INPUT_ITEM_SLOT = 10;      // Left - item to enchant
    private static final int INGREDIENT_SLOT = 14;      // Right - enchantment book/item
    private static final int OUTPUT_SLOT = 16;          // Result slot
    private static final int INFO_SLOT = 4;             // Center info
    private static final int COST_SLOT = 49;            // Cost display

    // Navigation slots
    private static final int TAKE_RESULT_SLOT = 52;     // Take result button
    private static final int CLOSE_SLOT = 50;           // Close button

    private final JavaPlugin plugin;
    private final NamespacedKey actionKey;
    private final NamespacedKey operationKey;
    private final SkyblockEnchantStorage enchantStorage;

    public SkyblockAnvilGui(JavaPlugin plugin) {
        this.plugin = plugin;
        this.actionKey = new NamespacedKey(plugin, "anvil-action");
        this.operationKey = new NamespacedKey(plugin, "anvil-operation");
        this.enchantStorage = new SkyblockEnchantStorage(plugin);
    }

    /**
     * Open the anvil GUI for a player.
     */
    public void openAnvil(Player player) {
        openAnvil(player, null, null);
    }

    /**
     * Open the anvil GUI with pre-selected items.
     */
    public void openAnvil(Player player, ItemStack targetItem, ItemStack ingredient) {
        AnvilHolder holder = new AnvilHolder(targetItem, ingredient);
        Inventory inventory = Bukkit.createInventory(holder, 54, TITLE);
        holder.inventory = inventory;

        // Fill background with purple stained glass (Skyblock theme)
        fillBackground(inventory);

        // Set input slots
        inventory.setItem(INPUT_ITEM_SLOT, createItemSlot(targetItem, "target"));
        inventory.setItem(INGREDIENT_SLOT, createItemSlot(ingredient, "ingredient"));

        // Calculate and set output
        updateOutput(player, holder, inventory);

        // Info item
        inventory.setItem(INFO_SLOT, createAnvilInfo());

        // Navigation buttons
        inventory.setItem(CLOSE_SLOT, createCloseButton());
        inventory.setItem(TAKE_RESULT_SLOT, createTakeResultButton());

        // Cost display
        inventory.setItem(COST_SLOT, createCostDisplay(0, player.getLevel()));

        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0F, 1.0F);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof AnvilHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null || event.getClickedInventory() != top) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta()) {
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);

        if (action != null) {
            handleAction(player, action, holder);
            return;
        }

        // Handle item slot clicks
        if (event.getSlot() == INPUT_ITEM_SLOT) {
            // Take target item
            if (holder.targetItem != null) {
                giveItem(player, holder.targetItem);
                holder.targetItem = null;
                updateDisplay(player, holder);
            }
        } else if (event.getSlot() == INGREDIENT_SLOT) {
            // Take ingredient
            if (holder.ingredient != null) {
                giveItem(player, holder.ingredient);
                holder.ingredient = null;
                updateDisplay(player, holder);
            }
        } else if (event.getSlot() == OUTPUT_SLOT) {
            // Take result
            takeResult(player, holder);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof AnvilHolder) {
            event.setCancelled(true);
        }
    }

    private void handleAction(Player player, String action, AnvilHolder holder) {
        switch (action) {
            case "close" -> {
                // Return items before closing
                if (holder.targetItem != null) {
                    giveItem(player, holder.targetItem);
                }
                if (holder.ingredient != null) {
                    giveItem(player, holder.ingredient);
                }
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 0.8F);
            }
            case "take_result" -> takeResult(player, holder);
        }
    }

    private void takeResult(Player player, AnvilHolder holder) {
        if (holder.result == null) {
            player.sendMessage(ChatColor.RED + "No result to take!");
            return;
        }

        if (player.getLevel() < holder.cost) {
            player.sendMessage(ChatColor.RED + "Not enough levels! Need " + holder.cost + " levels.");
            return;
        }

        // Deduct cost
        player.setLevel(player.getLevel() - holder.cost);

        // Give result
        giveItem(player, holder.result);

        // Clear slots
        holder.targetItem = null;
        holder.ingredient = null;
        holder.result = null;
        holder.cost = 0;

        updateDisplay(player, holder);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
        player.sendMessage(ChatColor.GREEN + "Enchantment applied successfully!");
    }

    private void updateDisplay(Player player, AnvilHolder holder) {
        Inventory inventory = holder.inventory;
        if (inventory == null) return;

        inventory.setItem(INPUT_ITEM_SLOT, createItemSlot(holder.targetItem, "target"));
        inventory.setItem(INGREDIENT_SLOT, createItemSlot(holder.ingredient, "ingredient"));
        updateOutput(player, holder, inventory);
        inventory.setItem(COST_SLOT, createCostDisplay(holder.cost, player.getLevel()));
    }

    private void updateOutput(Player player, AnvilHolder holder, Inventory inventory) {
        if (holder.targetItem == null || holder.ingredient == null) {
            inventory.setItem(OUTPUT_SLOT, createOutputSlot(null, "Place items"));
            inventory.setItem(COST_SLOT, createCostDisplay(0, player.getLevel()));
            holder.result = null;
            holder.cost = 0;
            return;
        }

        AnvilResult result = calculateResult(holder.targetItem, holder.ingredient);
        holder.result = result.result();
        holder.cost = result.cost();

        if (holder.result != null) {
            inventory.setItem(OUTPUT_SLOT, createOutputSlot(holder.result, "Click to take"));
        } else {
            inventory.setItem(OUTPUT_SLOT, createOutputSlot(null, result.error()));
        }

        inventory.setItem(COST_SLOT, createCostDisplay(holder.cost, player.getLevel()));
    }

    private AnvilResult calculateResult(ItemStack target, ItemStack ingredient) {
        if (target == null || ingredient == null) {
            return new AnvilResult(null, 0, "Place both items");
        }

        // Check if ingredient is an enchanted book
        if (ingredient.getType() == Material.ENCHANTED_BOOK) {
            return combineWithBook(target, ingredient);
        }

        // Check if combining two items
        if (target.getType() == ingredient.getType()) {
            return combineTwoItems(target, ingredient);
        }

        return new AnvilResult(null, 0, "Cannot combine these items");
    }

    private AnvilResult combineWithBook(ItemStack target, ItemStack book) {
        if (!book.hasItemMeta() || !book.getItemMeta().hasEnchants()) {
            return new AnvilResult(null, 0, "Book has no enchantments");
        }

        ItemStack result = target.clone();
        int totalCost = 0;
        boolean appliedAny = false;

        // Get all enchantments from the book
        Map<org.bukkit.enchantments.Enchantment, Integer> bookEnchants = book.getEnchantments();

        for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : bookEnchants.entrySet()) {
            org.bukkit.enchantments.Enchantment enchant = entry.getKey();
            int bookLevel = entry.getValue();

            // Find matching Skyblock enchantment
            SkyblockEnchantment sbEnchant = findSkyblockEnchantment(enchant);
            if (sbEnchant == null) continue;

            // Check conflicts
            if (hasConflict(result, sbEnchant)) {
                continue; // Skip conflicting enchantments
            }

            int currentLevel = result.getEnchantmentLevel(enchant);
            int newLevel = calculateNewLevel(currentLevel, bookLevel, enchant.getMaxLevel());

            if (newLevel > currentLevel) {
                result.addUnsafeEnchantment(enchant, newLevel);
                totalCost += calculateEnchantCost(sbEnchant, newLevel);
                appliedAny = true;
            }
        }

        if (!appliedAny) {
            return new AnvilResult(null, 0, "No enchantments can be applied");
        }

        result = enchantStorage.refreshLore(result);

        // Apply 25% discount for books
        totalCost = (int) (totalCost * 0.75);
        totalCost = Math.max(1, totalCost);

        return new AnvilResult(result, totalCost, null);
    }

    private AnvilResult combineTwoItems(ItemStack item1, ItemStack item2) {
        ItemStack result = item1.clone();
        int totalCost = 0;
        boolean appliedAny = false;

        // Combine enchantments from item2 to item1
        Map<org.bukkit.enchantments.Enchantment, Integer> item2Enchants = item2.getEnchantments();

        for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : item2Enchants.entrySet()) {
            org.bukkit.enchantments.Enchantment enchant = entry.getKey();
            int item2Level = entry.getValue();

            SkyblockEnchantment sbEnchant = findSkyblockEnchantment(enchant);
            if (sbEnchant == null) continue;

            if (hasConflict(result, sbEnchant)) {
                continue;
            }

            int currentLevel = result.getEnchantmentLevel(enchant);
            int newLevel = calculateNewLevel(currentLevel, item2Level, enchant.getMaxLevel());

            if (newLevel > currentLevel) {
                result.addUnsafeEnchantment(enchant, newLevel);
                totalCost += calculateEnchantCost(sbEnchant, newLevel);
                appliedAny = true;
            }
        }

        if (!appliedAny) {
            return new AnvilResult(null, 0, "No enchantments to combine");
        }

        result = enchantStorage.refreshLore(result);

        totalCost = Math.max(1, totalCost);

        return new AnvilResult(result, totalCost, null);
    }

    private SkyblockEnchantment findSkyblockEnchantment(org.bukkit.enchantments.Enchantment enchant) {
        for (SkyblockEnchantment sbEnchant : EnchantmentRegistry.getAll()) {
            if (sbEnchant.getVanillaEnchantment().contains(enchant)) {
                return sbEnchant;
            }
        }
        return null;
    }

    private boolean hasConflict(ItemStack item, SkyblockEnchantment newEnchant) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasEnchants()) {
            return false;
        }

        for (org.bukkit.enchantments.Enchantment existing : item.getEnchantments().keySet()) {
            SkyblockEnchantment existingSb = findSkyblockEnchantment(existing);
            if (existingSb != null && newEnchant.conflictsWith(existingSb)) {
                return true;
            }
        }

        return false;
    }

    private int calculateNewLevel(int current, int added, int maxLevel) {
        if (current == added) {
            return Math.min(current + 1, maxLevel);
        }
        return Math.max(current, added);
    }

    private int calculateEnchantCost(SkyblockEnchantment enchant, int level) {
        return enchant.getXpCost(level);
    }

    private ItemStack createItemSlot(ItemStack item, String type) {
        if (item == null) {
            return createNamedItem(
                Material.GRAY_STAINED_GLASS_PANE,
                ChatColor.GRAY + "Place " + ChatColor.BOLD + type.substring(0, 1).toUpperCase() + type.substring(1)
            );
        }

        ItemStack display = item.clone();
        ItemMeta meta = display.getItemMeta();
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

        lore.add("");
        lore.add(ChatColor.GRAY + "Click to remove");

        meta.setLore(lore);
        meta.getPersistentDataContainer().set(operationKey, PersistentDataType.STRING, "remove_" + type);
        display.setItemMeta(meta);

        return display;
    }

    private ItemStack createOutputSlot(ItemStack item, String message) {
        if (item == null) {
            return createNamedItem(
                Material.BARRIER,
                ChatColor.RED + "No Result",
                List.of(ChatColor.GRAY + message)
            );
        }

        ItemStack display = item.clone();
        ItemMeta meta = display.getItemMeta();
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

        lore.add("");
        lore.add(ChatColor.GREEN + "Click to take result");

        meta.setLore(lore);
        display.setItemMeta(meta);

        return display;
    }

    private ItemStack createCostDisplay(int cost, int playerLevel) {
        boolean canAfford = playerLevel >= cost;

        ItemStack stack = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = stack.getItemMeta();

        meta.setDisplayName((canAfford ? ChatColor.GREEN : ChatColor.RED) + "Cost: " + ChatColor.AQUA + cost + " levels");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Your Level: " + (canAfford ? ChatColor.GREEN : ChatColor.RED) + playerLevel);
        lore.add("");

        if (cost > 0) {
            lore.add(ChatColor.GRAY + "Enchantments are " + ChatColor.GREEN + "25% cheaper" + ChatColor.GRAY + "when using books!");
        } else {
            lore.add(ChatColor.GRAY + "Place items to see cost");
        }

        meta.setLore(lore);
        stack.setItemMeta(meta);

        return stack;
    }

    private ItemStack createAnvilInfo() {
        ItemStack stack = new ItemStack(Material.ANVIL);
        ItemMeta meta = stack.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + "Anvil Guide");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Combine enchantments using:");
        lore.add(ChatColor.DARK_GRAY + "• " + ChatColor.GRAY + "Enchanted Books (25% discount)");
        lore.add(ChatColor.DARK_GRAY + "• " + ChatColor.GRAY + "Similar items");
        lore.add("");
        lore.add(ChatColor.GRAY + "Rules:");
        lore.add(ChatColor.DARK_GRAY + "• " + ChatColor.GRAY + "Conflicting enchants won't combine");
        lore.add(ChatColor.DARK_GRAY + "• " + ChatColor.GRAY + "Higher level replaces lower");
        lore.add(ChatColor.DARK_GRAY + "• " + ChatColor.GRAY + "Same level = +1 level");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Tip: Use books for cheaper combining!");

        meta.setLore(lore);
        stack.setItemMeta(meta);

        return stack;
    }

    private ItemStack createCloseButton() {
        ItemStack stack = new ItemStack(Material.BARRIER);
        ItemMeta meta = stack.getItemMeta();

        meta.setDisplayName(ChatColor.RED + "Close");
        meta.setLore(List.of(ChatColor.GRAY + "Close the anvil and return items."));
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "close");
        stack.setItemMeta(meta);

        return stack;
    }

    private ItemStack createTakeResultButton() {
        ItemStack stack = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = stack.getItemMeta();

        meta.setDisplayName(ChatColor.GREEN + "Take Result");
        meta.setLore(List.of(ChatColor.GRAY + "Click to apply enchantments."));
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "take_result");
        stack.setItemMeta(meta);

        return stack;
    }

    private void fillBackground(Inventory inventory) {
        ItemStack filler = createNamedItem(Material.PURPLE_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    private ItemStack createNamedItem(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null) {
            meta.setLore(lore);
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createNamedItem(Material material, String name) {
        return createNamedItem(material, name, null);
    }

    private void giveItem(Player player, ItemStack item) {
        if (item == null) return;

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItem(player.getLocation(), drop);
        }
    }

    private record AnvilResult(ItemStack result, int cost, String error) {}

    private static class AnvilHolder implements InventoryHolder {
        private Inventory inventory;
        private ItemStack targetItem;
        private ItemStack ingredient;
        private ItemStack result;
        private int cost = 0;

        public AnvilHolder(ItemStack targetItem, ItemStack ingredient) {
            this.targetItem = targetItem;
            this.ingredient = ingredient;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}

