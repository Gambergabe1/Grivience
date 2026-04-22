package io.papermc.Grivience.listener;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.item.ItemRarity;
import io.papermc.Grivience.item.ReforgeType;
import io.papermc.Grivience.skyblock.economy.ProfileEconomyService;
import io.papermc.Grivience.skyblock.profile.ProfileManager;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public final class BlacksmithGuiListener implements Listener {
    private static final String TITLE = ChatColor.DARK_GRAY + "Reforge Item";
    private static final String FIRST_REFORGE_FLAG = "__BLACKSMITH_FIRST_REFORGE_USED";

    private static final int INPUT_SLOT = 11;
    private static final int PREVIEW_SLOT = 15;
    private static final int BACK_SLOT = 18;
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
        Holder holder = new Holder();
        Inventory inv = Bukkit.createInventory(holder, 27, TITLE);
        holder.inventory = inv;
        fill(inv);
        updatePreview(player, inv);
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof Holder)) {
            return;
        }

        // Lock all movement while this GUI is open.
        event.setCancelled(true);

        // Extra hard-block for known extraction vectors.
        if (event.getClick() == ClickType.DOUBLE_CLICK || event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            return;
        }

        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) {
            return;
        }

        if (clickedInventory.equals(top)) {
            handleTopClick(player, top, event.getRawSlot(), event.getCursor());
        } else {
            handleBottomClick(player, top, clickedInventory, event.getSlot(), event.getCurrentItem());
        }
    }

    private void handleTopClick(Player player, Inventory top, int slot, ItemStack cursor) {
        if (slot == CLOSE_SLOT) {
            player.closeInventory();
            return;
        }
        if (slot == BACK_SLOT) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!player.performCommand("reforge")) {
                    player.sendMessage(ChatColor.RED + "Unable to open reforge menu.");
                }
            });
            return;
        }

        if (slot == INPUT_SLOT) {
            ItemStack existing = top.getItem(INPUT_SLOT);

            // Place weapon from cursor into input slot.
            if (cursor != null && !cursor.getType().isAir() && customItemService.isReforgable(cursor)) {
                if (existing == null || existing.getType().isAir() || existing.getType() == Material.ANVIL) {
                    top.setItem(INPUT_SLOT, cursor.clone());
                    player.setItemOnCursor(null);
                    updatePreview(player, top);
                } else {
                    player.sendMessage(ChatColor.RED + "Remove the current item first.");
                }
                return;
            }

            // Take weapon back into cursor.
            if (cursor == null || cursor.getType().isAir()) {
                if (existing != null && customItemService.isReforgable(existing)) {
                    player.setItemOnCursor(existing.clone());
                    top.setItem(INPUT_SLOT, inputPlaceholder());
                    updatePreview(player, top);
                }
            }
            return;
        }

        if (slot == CONFIRM_SLOT) {
            apply(player, top);
        }
    }

    private void handleBottomClick(Player player, Inventory top, Inventory clickedInventory, int clickedSlot, ItemStack clicked) {
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }
        if (!customItemService.isReforgable(clicked)) {
            return;
        }

        ItemStack existing = top.getItem(INPUT_SLOT);
        if (existing != null && existing.getType() != Material.ANVIL) {
            player.sendMessage(ChatColor.RED + "Remove the current item first.");
            return;
        }

        ItemStack toReforge = clicked.clone();
        toReforge.setAmount(1);
        top.setItem(INPUT_SLOT, toReforge);

        if (clicked.getAmount() > 1) {
            clicked.setAmount(clicked.getAmount() - 1);
        } else {
            clickedInventory.setItem(clickedSlot, null);
        }

        updatePreview(player, top);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof Holder)) {
            return;
        }

        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        ItemStack input = top.getItem(INPUT_SLOT);
        if (input != null && customItemService.isReforgable(input)) {
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(input);
            leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }

        top.setItem(INPUT_SLOT, null);
        top.setItem(PREVIEW_SLOT, null);
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
        if (item == null || item.getType().isAir() || !customItemService.isReforgable(item)) {
            player.sendMessage(ChatColor.RED + "Place a reforgable weapon first.");
            return;
        }

        ReforgeType current = customItemService.reforgeOf(item);
        ReforgeType rolled = randomBasicReforge(current);
        if (rolled == null) {
            player.sendMessage(ChatColor.RED + "No valid basic reforges are available.");
            return;
        }

        BasicReforgeCost cost = resolveCost(player, item);
        if (!chargeCost(player, cost)) {
            return;
        }

        ItemStack result = customItemService.applyReforge(item, rolled);

        // Remove input before giving output to avoid any dupe edge-cases.
        inv.setItem(INPUT_SLOT, inputPlaceholder());

        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(result);
        if (!leftovers.isEmpty()) {
            leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
            player.sendMessage(ChatColor.YELLOW + "Inventory full, some items dropped on ground.");
        }

        updatePreview(player, inv);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0F, 1.1F);
        player.sendMessage(ChatColor.GREEN + "Rolled " + rolled.color() + rolled.displayName()
                + ChatColor.GREEN + " for " + ChatColor.GOLD + formatCost(cost) + ChatColor.GREEN + ".");
    }

    private void updatePreview(Player player, Inventory inv) {
        ItemStack input = inv.getItem(INPUT_SLOT);
        if (input == null || !customItemService.isReforgable(input)) {
            inv.setItem(PREVIEW_SLOT, taggedItem(
                    new ItemStack(Material.BOOK),
                    ChatColor.YELLOW + "Basic Reforging",
                    List.of(ChatColor.GRAY + "Insert a reforgable weapon."),
                    "preview"
            ));
            inv.setItem(CONFIRM_SLOT, taggedItem(
                    new ItemStack(Material.RED_STAINED_GLASS_PANE),
                    ChatColor.RED + "Cannot Reforge",
                    List.of(ChatColor.GRAY + "No weapon inserted."),
                    "confirm"
            ));
            return;
        }

        BasicReforgeCost cost = resolveCost(player, input);
        ReforgeType current = customItemService.reforgeOf(input);
        List<ReforgeType> possible = possibleBasicReforges(current);
        boolean affordable = canAfford(player, cost);

        List<String> previewLore = new ArrayList<>();
        previewLore.add(ChatColor.GRAY + "Mode: " + ChatColor.YELLOW + "Basic Reforging");
        previewLore.add(ChatColor.GRAY + "Current: "
                + (current == null ? ChatColor.DARK_GRAY + "None" : current.color() + current.displayName()));
        previewLore.add(ChatColor.GRAY + "Possible:");
        previewLore.add(ChatColor.GRAY + poolDisplay(possible));
        previewLore.add("");
        previewLore.add(ChatColor.GRAY + "Cost: " + ChatColor.GOLD + formatCost(cost));
        previewLore.add(balanceLine(player, cost));
        inv.setItem(PREVIEW_SLOT, taggedItem(
                new ItemStack(Material.ENCHANTED_BOOK),
                ChatColor.YELLOW + "Random Basic Reforge",
                previewLore,
                "preview"
        ));

        List<String> confirmLore = new ArrayList<>();
        confirmLore.add(ChatColor.GRAY + "Click to roll a random");
        confirmLore.add(ChatColor.GRAY + "basic reforge.");
        confirmLore.add("");
        confirmLore.add(ChatColor.GRAY + "Cost: " + ChatColor.GOLD + formatCost(cost));
        confirmLore.add(balanceLine(player, cost));
        confirmLore.add("");
        confirmLore.add(affordable
                ? ChatColor.GREEN + "Click to reforge"
                : ChatColor.RED + "You cannot afford this");

        inv.setItem(CONFIRM_SLOT, taggedItem(
                new ItemStack(affordable ? Material.EMERALD : Material.REDSTONE),
                affordable ? ChatColor.GREEN + "Reforge Item" : ChatColor.RED + "Cannot Reforge",
                confirmLore,
                "confirm"
        ));
    }

    private BasicReforgeCost resolveCost(Player player, ItemStack item) {
        if (hasFirstReforgeDiscount(player)) {
            return BasicReforgeCost.coal(10);
        }
        ItemRarity rarity = customItemService.rarityOf(item);
        return BasicReforgeCost.coins(blacksmithCoinCost(rarity));
    }

    private boolean canAfford(Player player, BasicReforgeCost cost) {
        if (cost.usesCoal()) {
            return countPlainMaterial(player, Material.COAL) >= cost.coalAmount();
        }
        return profileEconomy.has(player, cost.coins());
    }

    private boolean chargeCost(Player player, BasicReforgeCost cost) {
        if (cost.usesCoal()) {
            int coal = countPlainMaterial(player, Material.COAL);
            if (coal < cost.coalAmount()) {
                player.sendMessage(ChatColor.RED + "You need " + cost.coalAmount() + " Coal to reforge.");
                return false;
            }
            if (!consumePlainMaterial(player, Material.COAL, cost.coalAmount())) {
                player.sendMessage(ChatColor.RED + "Failed to consume Coal cost.");
                return false;
            }
            markFirstReforgeUsed(player);
            return true;
        }

        if (profileEconomy.requireSelectedProfile(player) == null) {
            return false;
        }
        if (!profileEconomy.has(player, cost.coins())) {
            player.sendMessage(ChatColor.RED + "You need " + formatCost(cost) + " to reforge.");
            return false;
        }
        if (!profileEconomy.withdraw(player, cost.coins())) {
            player.sendMessage(ChatColor.RED + "Failed to charge reforge cost.");
            return false;
        }
        return true;
    }

    private boolean hasFirstReforgeDiscount(Player player) {
        SkyBlockProfile profile = profileEconomy.getSelectedProfile(player);
        return profile != null && !profile.getCompletedQuests().contains(FIRST_REFORGE_FLAG);
    }

    private void markFirstReforgeUsed(Player player) {
        SkyBlockProfile profile = profileEconomy.getSelectedProfile(player);
        if (profile == null || profile.getCompletedQuests().contains(FIRST_REFORGE_FLAG)) {
            return;
        }
        profile.completeQuest(FIRST_REFORGE_FLAG);
        ProfileManager manager = plugin.getProfileManager();
        if (manager != null) {
            manager.saveProfile(profile);
        }
    }

    private int countPlainMaterial(Player player, Material material) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null || stack.getType() != material) {
                continue;
            }
            if (customItemService.itemId(stack) != null) {
                continue;
            }
            total += stack.getAmount();
        }
        return total;
    }

    private boolean consumePlainMaterial(Player player, Material material, int amount) {
        if (amount <= 0) {
            return true;
        }
        int remaining = amount;
        Inventory inventory = player.getInventory();

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.getType() != material) {
                continue;
            }
            if (customItemService.itemId(stack) != null) {
                continue;
            }

            int remove = Math.min(remaining, stack.getAmount());
            int left = stack.getAmount() - remove;
            if (left <= 0) {
                inventory.setItem(slot, null);
            } else {
                stack.setAmount(left);
                inventory.setItem(slot, stack);
            }

            remaining -= remove;
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    private double blacksmithCoinCost(ItemRarity rarity) {
        ItemRarity effective = rarity == null ? ItemRarity.RARE : rarity;
        String path = "custom-items.reforge.blacksmith-coins." + effective.name();
        return Math.max(1.0D, plugin.getConfig().getDouble(path, defaultBlacksmithCoinCost(effective)));
    }

    private double defaultBlacksmithCoinCost(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> 250.0D;
            case UNCOMMON -> 500.0D;
            case RARE -> 1_000.0D;
            case EPIC -> 2_500.0D;
            case LEGENDARY -> 5_000.0D;
            case MYTHIC -> 10_000.0D;
        };
    }

    private ReforgeType randomBasicReforge(ReforgeType current) {
        List<ReforgeType> pool = possibleBasicReforges(current);
        if (pool.isEmpty()) {
            return null;
        }
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    private List<ReforgeType> possibleBasicReforges(ReforgeType current) {
        List<ReforgeType> pool = new ArrayList<>(ReforgeType.blacksmithPool());
        if (current != null && pool.size() > 1) {
            pool.remove(current);
        }
        return pool;
    }

    private String poolDisplay(List<ReforgeType> pool) {
        if (pool.isEmpty()) {
            return ChatColor.DARK_GRAY + "None";
        }
        return pool.stream()
                .map(type -> type.color() + type.displayName())
                .collect(Collectors.joining(ChatColor.GRAY + ", "));
    }

    private String formatCost(BasicReforgeCost cost) {
        if (cost.usesCoal()) {
            return cost.coalAmount() + " Coal";
        }
        return String.format(Locale.ROOT, "%,.0f", cost.coins()) + " coins";
    }

    private String balanceLine(Player player, BasicReforgeCost cost) {
        if (cost.usesCoal()) {
            int coal = countPlainMaterial(player, Material.COAL);
            return ChatColor.GRAY + "Your Coal: " + ChatColor.YELLOW + coal;
        }
        return ChatColor.GRAY + "Purse: " + ChatColor.YELLOW
                + String.format(Locale.ROOT, "%,.0f", profileEconomy.purse(player)) + " coins";
    }

    private void fill(Inventory inv) {
        ItemStack pane = taggedItem(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), " ", List.of(), "filler");
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, pane);
        }

        inv.setItem(INPUT_SLOT, inputPlaceholder());
        inv.setItem(BACK_SLOT, taggedItem(
                new ItemStack(Material.ARROW),
                ChatColor.YELLOW + "Go Back",
                List.of(ChatColor.GRAY + "Return to Reforge Anvil."),
                "back"
        ));
        inv.setItem(CLOSE_SLOT, taggedItem(
                new ItemStack(Material.BARRIER),
                ChatColor.RED + "Close",
                List.of(ChatColor.GRAY + "Close the blacksmith."),
                "close"
        ));
    }

    private ItemStack inputPlaceholder() {
        return taggedItem(
                new ItemStack(Material.ANVIL),
                ChatColor.GREEN + "Place item to reforge",
                List.of(ChatColor.GRAY + "Only reforgable weapons."),
                "input"
        );
    }

    private ItemStack taggedItem(ItemStack base, String name, List<String> lore, String action) {
        base.editMeta(meta -> {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        });
        return base;
    }

    private record BasicReforgeCost(boolean usesCoal, int coalAmount, double coins) {
        private static BasicReforgeCost coal(int amount) {
            return new BasicReforgeCost(true, Math.max(1, amount), 0.0D);
        }

        private static BasicReforgeCost coins(double amount) {
            return new BasicReforgeCost(false, 0, Math.max(1.0D, amount));
        }
    }

    private static final class Holder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
