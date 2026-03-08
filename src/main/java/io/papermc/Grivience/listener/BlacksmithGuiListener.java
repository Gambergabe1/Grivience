package io.papermc.Grivience.listener;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.item.ReforgeType;
import io.papermc.Grivience.skyblock.economy.ProfileEconomyService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class BlacksmithGuiListener implements Listener {
    private static final String TITLE = ChatColor.DARK_GRAY + "Blacksmith";
    private static final int INPUT_SLOT = 11;
    private static final int RESULT_SLOT = 15;
    private static final int CONFIRM_SLOT = 22;
    private static final int CLOSE_SLOT = 26;

    private final GriviencePlugin plugin;
    private final CustomItemService customItemService;
    private final NamespacedKey actionKey;
    private final ProfileEconomyService profileEconomy;

    public BlacksmithGuiListener(GriviencePlugin plugin, CustomItemService customItemService) {
        this.plugin = plugin;
        this.customItemService = customItemService;
        this.actionKey = new NamespacedKey(plugin, "blacksmith-action");
        this.profileEconomy = new ProfileEconomyService(plugin);
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(new Holder(), 27, TITLE);
        fill(inv);
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        // If the top inventory is a Blacksmith menu, cancel ALL clicks
        if (event.getInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
            
            if (event.getClickedInventory() == null) return;

            if (event.getClickedInventory().equals(event.getInventory())) {
                handleTopClick(player, event.getInventory(), event.getSlot(), event.getCursor());
            } else {
                handleBottomClick(player, event.getInventory(), event.getSlot(), event.getCurrentItem());
            }
        }
    }

    private void handleTopClick(Player player, Inventory top, int slot, ItemStack cursor) {
        if (slot == CLOSE_SLOT) {
            player.closeInventory();
            return;
        }
        
        if (slot == INPUT_SLOT) {
            ItemStack existing = top.getItem(INPUT_SLOT);
            
            // Only allow placing items if cursor has a valid custom dungeon weapon
            if (cursor != null && !cursor.getType().isAir() && customItemService.isCustomDungeonWeapon(cursor)) {
                if (existing == null || existing.getType().isAir() || existing.getType() == Material.ANVIL) {
                    top.setItem(INPUT_SLOT, cursor.clone());
                    player.setItemOnCursor(null);
                    updatePreview(top);
                } else {
                    player.sendMessage(ChatColor.RED + "Remove the current item first.");
                }
            } 
            // Allow taking item back from input slot
            else if (cursor == null || cursor.getType().isAir()) {
                if (existing != null && !existing.getType().isAir() && existing.getType() != Material.ANVIL) {
                    player.setItemOnCursor(existing.clone());
                    top.setItem(INPUT_SLOT, taggedItem(new ItemStack(org.bukkit.Material.ANVIL), ChatColor.GREEN + "Place item to reforge", List.of(ChatColor.GRAY + "Only custom dungeon weapons")));
                    updatePreview(top);
                }
            }
            return;
        }
        
        if (slot == CONFIRM_SLOT) {
            apply(player, top);
            return;
        }
    }

    private void handleBottomClick(Player player, Inventory top, int slot, ItemStack clicked) {
        if (clicked == null || clicked.getType().isAir()) return;

        if (customItemService.isCustomDungeonWeapon(clicked)) {
            ItemStack existing = top.getItem(INPUT_SLOT);
            if (existing != null && existing.getType() != Material.ANVIL) {
                player.sendMessage(ChatColor.RED + "Remove the current item first!");
                return;
            }

            // Move item to reforge slot
            ItemStack toReforge = clicked.clone();
            toReforge.setAmount(1);
            top.setItem(INPUT_SLOT, toReforge);

            if (clicked.getAmount() > 1) {
                clicked.setAmount(clicked.getAmount() - 1);
            } else {
                player.getInventory().setItem(slot, null);
            }

            updatePreview(top);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof Holder)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        
        // Return input item to player
        ItemStack input = top.getItem(INPUT_SLOT);
        if (input != null && !input.getType().isAir()) {
            ItemStack dropped = player.getInventory().addItem(input).get(0);
            if (dropped != null) {
                player.getWorld().dropItemNaturally(player.getLocation(), dropped);
            }
        }
        
        // Clear the inventory to prevent any phantom items
        top.setItem(INPUT_SLOT, null);
        top.setItem(RESULT_SLOT, null);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof Holder)) {
            return;
        }
        event.setCancelled(true);
    }

    private void apply(Player player, Inventory inv) {
        ItemStack item = inv.getItem(INPUT_SLOT);
        if (item == null || item.getType().isAir() || !customItemService.isCustomDungeonWeapon(item)) {
            player.sendMessage(ChatColor.RED + "Place a reforgable weapon first.");
            return;
        }
        
        // Clear the result slot to prevent any potential duplication
        inv.setItem(RESULT_SLOT, null);
        
        double cost = basicCoinCost() * rarityMultiplier(item);
        if (profileEconomy.requireSelectedProfile(player) == null) {
            return;
        }
        if (!profileEconomy.has(player, cost)) {
            player.sendMessage(ChatColor.RED + "You need " + ChatColor.GOLD + String.format(Locale.ROOT, "%,.0f", cost) + " coins" + ChatColor.RED + " to reforge.");
            return;
        }
        if (!profileEconomy.withdraw(player, cost)) {
            player.sendMessage(ChatColor.RED + "Failed to charge reforge cost.");
            return;
        }
        
        ReforgeType rolled = randomBasicReforge();
        ItemStack result = customItemService.applyReforge(item, rolled);
        
        // Remove the input item BEFORE giving the result to prevent duplication
        inv.setItem(INPUT_SLOT, null);
        
        // Give the result to the player
        ItemStack dropped = player.getInventory().addItem(result).get(0);
        if (dropped != null) {
            // If inventory is full, drop on ground
            player.getWorld().dropItemNaturally(player.getLocation(), dropped);
            player.sendMessage(ChatColor.YELLOW + "Inventory full, some items dropped on ground.");
        }
        
        updatePreview(inv);
        player.sendMessage(ChatColor.GREEN + "Rolled " + rolled.color() + rolled.displayName() + ChatColor.GREEN + " for " + ChatColor.GOLD + String.format(Locale.ROOT, "%,.0f", cost) + " coins");
    }

    private void updatePreview(Inventory inv) {
        ItemStack input = inv.getItem(INPUT_SLOT);
        if (input == null || !customItemService.isCustomDungeonWeapon(input)) {
            inv.setItem(RESULT_SLOT, null);
            return;
        }
        ReforgeType rolled = randomBasicReforge();
        ItemStack preview = customItemService.applyReforge(input, rolled);
        ItemMeta meta = preview.getItemMeta();
        List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Cost: " + ChatColor.GOLD + String.format(Locale.ROOT, "%,.0f", basicCoinCost() * rarityMultiplier(input)) + " coins");
        meta.setLore(lore);
        preview.setItemMeta(meta);
        inv.setItem(RESULT_SLOT, preview);
    }

    private double basicCoinCost() {
        return Math.max(1.0D, plugin.getConfig().getDouble("custom-items.reforge.basic-cost-coins", 2500.0D));
    }

    private ReforgeType randomBasicReforge() {
        List<ReforgeType> pool = ReforgeType.blacksmithPool();
        if (pool.isEmpty()) {
            return ReforgeType.GENTLE;
        }
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    private void fill(Inventory inv) {
        ItemStack pane = taggedItem(new ItemStack(org.bukkit.Material.BLACK_STAINED_GLASS_PANE), " ", List.of());
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, pane);
        }
        inv.setItem(INPUT_SLOT, taggedItem(new ItemStack(org.bukkit.Material.ANVIL), ChatColor.GREEN + "Place item to reforge", List.of(ChatColor.GRAY + "Only custom dungeon weapons")));
        inv.setItem(RESULT_SLOT, taggedItem(new ItemStack(org.bukkit.Material.BOOK), ChatColor.YELLOW + "Random blacksmith reforge", List.of(ChatColor.GRAY + "Preview appears when item is inserted")));
        ItemStack confirm = taggedItem(new ItemStack(org.bukkit.Material.EMERALD), ChatColor.GOLD + "Reforge", List.of(ChatColor.GRAY + "Roll a random blacksmith reforge"));
        inv.setItem(CONFIRM_SLOT, confirm);
        
        // Close button
        ItemStack close = taggedItem(new ItemStack(org.bukkit.Material.BARRIER), ChatColor.RED + "Close", List.of(ChatColor.GRAY + "Close the blacksmith"));
        inv.setItem(CLOSE_SLOT, close);
    }

    private ItemStack taggedItem(ItemStack base, String name, List<String> lore) {
        base.editMeta(meta -> {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(actionKey, org.bukkit.persistence.PersistentDataType.STRING, "button");
        });
        return base;
    }

    private double rarityMultiplier(ItemStack item) {
        var rarity = customItemService.rarityOf(item);
        String path = "custom-items.reforge.rarity-cost-multipliers." + rarity.name();
        return Math.max(0.10D, plugin.getConfig().getDouble(path, defaultRarityMultiplier(rarity)));
    }

    private double defaultRarityMultiplier(io.papermc.Grivience.item.ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> 0.75D;
            case UNCOMMON -> 0.90D;
            case RARE -> 1.00D;
            case EPIC -> 1.30D;
            case LEGENDARY -> 1.70D;
            case MYTHIC -> 2.20D;
        };
    }

    private static final class Holder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
