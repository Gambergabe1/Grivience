package io.papermc.Grivience.listener;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.item.ReforgeStoneType;
import io.papermc.Grivience.item.ReforgeType;
import io.papermc.Grivience.skyblock.economy.ProfileEconomyService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ReforgeAnvilGuiListener implements Listener {
    private static final String TITLE_MENU = ChatColor.DARK_AQUA + "Reforge Anvil";
    private static final String TITLE_ADVANCED = ChatColor.DARK_AQUA + "Apply Reforge Stone";

    private static final int MENU_INFO_SLOT = 4;
    private static final int MENU_BASIC_SLOT = 11;
    private static final int MENU_ADVANCED_SLOT = 15;
    private static final int MENU_CLOSE_SLOT = 26;

    private static final int WEAPON_SLOT = 11;
    private static final int PREVIEW_SLOT = 13;
    private static final int STONE_SLOT = 15;
    private static final int BACK_SLOT = 18;
    private static final int CONFIRM_SLOT = 22;
    private static final int INFO_SLOT = 4;
    private static final int CLOSE_SLOT = 26;

    private final GriviencePlugin plugin;
    private final CustomItemService customItemService;
    private final NamespacedKey actionKey;
    private final ProfileEconomyService profileEconomy;

    public ReforgeAnvilGuiListener(GriviencePlugin plugin, CustomItemService customItemService) {
        this.plugin = plugin;
        this.customItemService = customItemService;
        this.actionKey = new NamespacedKey(plugin, "reforge-ui-action");
        this.profileEconomy = new ProfileEconomyService(plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onAnvilInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }

        Material type = event.getClickedBlock().getType();
        if (type != Material.ANVIL && type != Material.CHIPPED_ANVIL && type != Material.DAMAGED_ANVIL) {
            return;
        }

        event.setCancelled(true);
        openReforgeUi(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof ReforgeHolder holder)) {
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

        if (holder.viewType == ReforgeView.MENU) {
            if (clickedInventory.equals(top)) {
                handleMenuTopClick(player, event.getCurrentItem());
            }
            return;
        }

        if (clickedInventory.equals(top)) {
            handleAdvancedTopClick(player, top, event.getRawSlot(), event.getCurrentItem());
        } else {
            handleAdvancedBottomClick(player, top, clickedInventory, event.getSlot(), event.getCurrentItem());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof ReforgeHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof ReforgeHolder holder)) {
            return;
        }
        if (holder.viewType != ReforgeView.ADVANCED) {
            return;
        }

        returnInputToPlayer(player, top.getItem(WEAPON_SLOT), true);
        returnInputToPlayer(player, top.getItem(STONE_SLOT), false);
        top.setItem(WEAPON_SLOT, weaponPlaceholder());
        top.setItem(STONE_SLOT, stonePlaceholder());
    }

    public void openReforgeUi(Player player) {
        openMainMenu(player);
    }

    public void openAdvancedUi(Player player) {
        ReforgeHolder holder = new ReforgeHolder(ReforgeView.ADVANCED);
        Inventory inventory = Bukkit.createInventory(holder, 27, TITLE_ADVANCED);
        holder.inventory = inventory;

        ItemStack filler = taggedItem(Material.BLACK_STAINED_GLASS_PANE, " ", List.of(), "filler");
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }

        inventory.setItem(INFO_SLOT, taggedItem(
                Material.ENCHANTED_BOOK,
                ChatColor.AQUA + "Advanced Reforging",
                List.of(
                        ChatColor.GRAY + "Insert a custom dungeon weapon",
                        ChatColor.GRAY + "and a matching Reforge Stone.",
                        ChatColor.GRAY + "Stone reforges apply directly."
                ),
                "info"
        ));
        inventory.setItem(BACK_SLOT, taggedItem(
                Material.ARROW,
                ChatColor.YELLOW + "Go Back",
                List.of(ChatColor.GRAY + "Return to Reforge Anvil."),
                "back"
        ));
        inventory.setItem(CLOSE_SLOT, taggedItem(
                Material.BARRIER,
                ChatColor.RED + "Close",
                List.of(ChatColor.GRAY + "Return inserted items."),
                "close"
        ));
        inventory.setItem(WEAPON_SLOT, weaponPlaceholder());
        inventory.setItem(STONE_SLOT, stonePlaceholder());
        refreshAdvancedUi(player, inventory);
        player.openInventory(inventory);
    }

    private void openMainMenu(Player player) {
        ReforgeHolder holder = new ReforgeHolder(ReforgeView.MENU);
        Inventory inventory = Bukkit.createInventory(holder, 27, TITLE_MENU);
        holder.inventory = inventory;

        ItemStack filler = taggedItem(Material.BLACK_STAINED_GLASS_PANE, " ", List.of(), "filler");
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }

        inventory.setItem(MENU_INFO_SLOT, taggedItem(
                Material.ANVIL,
                ChatColor.AQUA + "Reforging",
                List.of(
                        ChatColor.GRAY + "Basic: random reforge (Blacksmith).",
                        ChatColor.GRAY + "Advanced: apply a Reforge Stone."
                ),
                "info"
        ));
        inventory.setItem(MENU_BASIC_SLOT, taggedItem(
                Material.SMITHING_TABLE,
                ChatColor.YELLOW + "Basic Reforging",
                List.of(
                        ChatColor.GRAY + "Randomly apply a basic reforge.",
                        ChatColor.GRAY + "Uses Blacksmith mechanics."
                ),
                "open_basic"
        ));
        inventory.setItem(MENU_ADVANCED_SLOT, taggedItem(
                Material.AMETHYST_SHARD,
                ChatColor.LIGHT_PURPLE + "Advanced Reforging",
                List.of(
                        ChatColor.GRAY + "Apply a specific reforge",
                        ChatColor.GRAY + "using a Reforge Stone."
                ),
                "open_advanced"
        ));
        inventory.setItem(MENU_CLOSE_SLOT, taggedItem(
                Material.BARRIER,
                ChatColor.RED + "Close",
                List.of(ChatColor.GRAY + "Close the reforge menu."),
                "close"
        ));

        player.openInventory(inventory);
    }

    private void handleMenuTopClick(Player player, ItemStack clicked) {
        String action = actionOf(clicked);
        switch (action) {
            case "open_basic" -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (!player.performCommand("blacksmith")) {
                        player.sendMessage(ChatColor.RED + "Unable to open Blacksmith right now.");
                    }
                });
            }
            case "open_advanced" -> openAdvancedUi(player);
            case "close" -> player.closeInventory();
            default -> {
            }
        }
    }

    private void handleAdvancedTopClick(Player player, Inventory top, int rawSlot, ItemStack clicked) {
        if (rawSlot == WEAPON_SLOT) {
            returnInputToPlayer(player, top.getItem(WEAPON_SLOT), true);
            top.setItem(WEAPON_SLOT, weaponPlaceholder());
            refreshAdvancedUi(player, top);
            return;
        }
        if (rawSlot == STONE_SLOT) {
            returnInputToPlayer(player, top.getItem(STONE_SLOT), false);
            top.setItem(STONE_SLOT, stonePlaceholder());
            refreshAdvancedUi(player, top);
            return;
        }

        String action = actionOf(clicked);
        if ("confirm".equals(action)) {
            applyStoneReforge(player, top);
            refreshAdvancedUi(player, top);
            return;
        }
        if ("back".equals(action)) {
            openMainMenu(player);
            return;
        }
        if ("close".equals(action)) {
            player.closeInventory();
        }
    }

    private void handleAdvancedBottomClick(
            Player player,
            Inventory top,
            Inventory clickedInventory,
            int clickedSlot,
            ItemStack clicked
    ) {
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }

        if (customItemService.isCustomDungeonWeapon(clicked)) {
            if (customItemService.isCustomDungeonWeapon(top.getItem(WEAPON_SLOT))) {
                player.sendMessage(ChatColor.RED + "Weapon slot already has a dungeon weapon.");
                return;
            }
            moveSingleItem(clickedInventory, clickedSlot, clicked, top, WEAPON_SLOT);
            refreshAdvancedUi(player, top);
            return;
        }

        ReforgeStoneType stoneType = ReforgeStoneType.parse(customItemService.itemId(clicked));
        if (stoneType == null) {
            return;
        }
        if (ReforgeStoneType.parse(customItemService.itemId(top.getItem(STONE_SLOT))) != null) {
            player.sendMessage(ChatColor.RED + "Stone slot already has a reforge stone.");
            return;
        }
        moveSingleItem(clickedInventory, clickedSlot, clicked, top, STONE_SLOT);
        refreshAdvancedUi(player, top);
    }

    private void moveSingleItem(
            Inventory sourceInventory,
            int sourceSlot,
            ItemStack sourceItem,
            Inventory targetInventory,
            int targetSlot
    ) {
        ItemStack one = sourceItem.clone();
        one.setAmount(1);
        targetInventory.setItem(targetSlot, one);

        if (sourceItem.getAmount() <= 1) {
            sourceInventory.setItem(sourceSlot, null);
        } else {
            sourceItem.setAmount(sourceItem.getAmount() - 1);
            sourceInventory.setItem(sourceSlot, sourceItem);
        }
    }

    private void applyStoneReforge(Player player, Inventory top) {
        ItemStack weapon = top.getItem(WEAPON_SLOT);
        if (!customItemService.isCustomDungeonWeapon(weapon)) {
            player.sendMessage(ChatColor.RED + "Insert a dungeon weapon first.");
            return;
        }

        ItemStack stone = top.getItem(STONE_SLOT);
        ReforgeStoneType stoneType = ReforgeStoneType.parse(customItemService.itemId(stone));
        if (stoneType == null) {
            player.sendMessage(ChatColor.RED + "Insert a reforge stone first.");
            return;
        }

        ReforgeCost cost = resolveStoneCost(stoneType);
        if (!chargeCost(player, cost)) {
            return;
        }

        ReforgeType targetReforge = stoneType.reforgeType();
        ItemStack result = customItemService.applyReforge(weapon, targetReforge);
        result = customItemService.syncWeaponEnchantLore(result);
        giveToPlayer(player, result);

        top.setItem(WEAPON_SLOT, weaponPlaceholder());
        top.setItem(STONE_SLOT, stonePlaceholder());
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0F, 1.1F);
        player.sendMessage(ChatColor.GREEN + "Applied " + targetReforge.displayName() + " reforge.");
    }

    private void refreshAdvancedUi(Player player, Inventory inventory) {
        ItemStack weapon = inventory.getItem(WEAPON_SLOT);
        ItemStack stone = inventory.getItem(STONE_SLOT);
        boolean validWeapon = customItemService.isCustomDungeonWeapon(weapon);
        ReforgeStoneType stoneType = ReforgeStoneType.parse(customItemService.itemId(stone));

        if (!validWeapon) {
            inventory.setItem(PREVIEW_SLOT, taggedItem(
                    Material.BARRIER,
                    ChatColor.RED + "No Weapon",
                    List.of(ChatColor.GRAY + "Insert a custom dungeon weapon."),
                    "preview"
            ));
            inventory.setItem(CONFIRM_SLOT, taggedItem(
                    Material.RED_STAINED_GLASS_PANE,
                    ChatColor.RED + "Cannot Reforge",
                    List.of(ChatColor.GRAY + "Weapon slot is empty."),
                    "confirm"
            ));
            return;
        }

        if (stoneType == null) {
            inventory.setItem(PREVIEW_SLOT, taggedItem(
                    Material.REDSTONE,
                    ChatColor.RED + "No Reforge Stone",
                    List.of(ChatColor.GRAY + "Insert a reforge stone."),
                    "preview"
            ));
            inventory.setItem(CONFIRM_SLOT, taggedItem(
                    Material.RED_STAINED_GLASS_PANE,
                    ChatColor.RED + "Cannot Reforge",
                    List.of(ChatColor.GRAY + "Stone slot is empty."),
                    "confirm"
            ));
            return;
        }

        ReforgeCost cost = resolveStoneCost(stoneType);
        boolean affordable = canAfford(player, cost);

        ItemStack preview = customItemService.applyReforge(weapon, stoneType.reforgeType());
        preview = customItemService.syncWeaponEnchantLore(preview);
        ItemMeta previewMeta = preview.getItemMeta();
        List<String> previewLore = previewMeta.hasLore() && previewMeta.getLore() != null
                ? new ArrayList<>(previewMeta.getLore())
                : new ArrayList<>();
        previewLore.add("");
        previewLore.add(ChatColor.GRAY + "Mode: " + ChatColor.YELLOW + "Advanced Reforging");
        previewLore.add(ChatColor.GRAY + "Cost: " + ChatColor.GREEN + formatCost(cost));
        previewLore.add(ChatColor.GRAY + customItemService.reforgeBonusLine(stoneType.reforgeType(), weapon));
        previewMeta.setLore(previewLore);
        preview.setItemMeta(previewMeta);
        inventory.setItem(PREVIEW_SLOT, preview);

        List<String> confirmLore = new ArrayList<>();
        confirmLore.add(ChatColor.GRAY + "Mode: " + ChatColor.YELLOW + "Advanced Reforging");
        confirmLore.add(ChatColor.GRAY + "Cost: " + ChatColor.GREEN + formatCost(cost));
        confirmLore.add(balanceLine(player, cost));
        confirmLore.add(ChatColor.GRAY + customItemService.reforgeBonusLine(stoneType.reforgeType(), weapon));
        confirmLore.add("");
        confirmLore.add(affordable
                ? ChatColor.GREEN + "Click to apply " + stoneType.reforgeType().displayName()
                : ChatColor.RED + "You cannot afford this reforge");
        inventory.setItem(CONFIRM_SLOT, taggedItem(
                affordable ? Material.EMERALD : Material.REDSTONE,
                affordable ? ChatColor.GREEN + "Apply Reforge" : ChatColor.RED + "Cannot Reforge",
                confirmLore,
                "confirm"
        ));
    }

    private void returnInputToPlayer(Player player, ItemStack item, boolean weaponSlot) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        if (weaponSlot && !customItemService.isCustomDungeonWeapon(item)) {
            return;
        }
        if (!weaponSlot && ReforgeStoneType.parse(customItemService.itemId(item)) == null) {
            return;
        }
        giveToPlayer(player, item);
    }

    private void giveToPlayer(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    private ReforgeCost resolveStoneCost(ReforgeStoneType stoneType) {
        if (stoneType == null) {
            return ReforgeCost.coins(0.0D);
        }
        if (useVaultEconomy()) {
            return ReforgeCost.coins(stoneCoinCost(stoneType));
        }
        return ReforgeCost.levels(Math.max(1, stoneType.levelCost()));
    }

    private boolean canAfford(Player player, ReforgeCost cost) {
        if (cost.usesCoins()) {
            return profileEconomy.has(player, cost.coins());
        }
        return player.getLevel() >= cost.levels();
    }

    private boolean chargeCost(Player player, ReforgeCost cost) {
        if (cost.usesCoins()) {
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

        if (player.getLevel() < cost.levels()) {
            player.sendMessage(ChatColor.RED + "You need " + cost.levels() + " levels to reforge.");
            return false;
        }
        player.setLevel(Math.max(0, player.getLevel() - cost.levels()));
        return true;
    }

    private String formatCost(ReforgeCost cost) {
        if (cost.usesCoins()) {
            return "$" + String.format(Locale.ROOT, "%,.0f", cost.coins());
        }
        return cost.levels() + " levels";
    }

    private String balanceLine(Player player, ReforgeCost cost) {
        if (cost.usesCoins()) {
            double balance = profileEconomy.purse(player);
            return ChatColor.GRAY + "Purse: " + ChatColor.YELLOW + String.format(Locale.ROOT, "%,.0f", balance) + " coins";
        }
        return ChatColor.GRAY + "Your Levels: " + ChatColor.YELLOW + player.getLevel();
    }

    private double stoneCoinCost(ReforgeStoneType stoneType) {
        String path = "custom-items.reforge.stone-cost-coins." + stoneType.name();
        return Math.max(1.0D, plugin.getConfig().getDouble(path, defaultStoneCoinCost(stoneType)));
    }

    private double defaultStoneCoinCost(ReforgeStoneType stoneType) {
        return switch (stoneType) {
            case GENTLE_STONE -> 500.0D;
            case ODD_STONE, FAST_STONE, SHARP_STONE -> 1_000.0D;
            case FAIR_STONE, EPIC_STONE, HEROIC_STONE -> 2_500.0D;
            case SPICY_STONE, LEGENDARY_STONE -> 5_000.0D;
        };
    }

    private boolean useVaultEconomy() {
        return plugin.getConfig().getBoolean("custom-items.reforge.use-vault-economy", true);
    }

    private ItemStack weaponPlaceholder() {
        return taggedItem(
                Material.IRON_SWORD,
                ChatColor.YELLOW + "Weapon Slot",
                List.of(ChatColor.GRAY + "Click a custom dungeon weapon from your inventory."),
                "slot_weapon"
        );
    }

    private ItemStack stonePlaceholder() {
        return taggedItem(
                Material.AMETHYST_SHARD,
                ChatColor.YELLOW + "Stone Slot",
                List.of(ChatColor.GRAY + "Insert a Reforge Stone."),
                "slot_stone"
        );
    }

    private ItemStack taggedItem(Material material, String name, List<String> lore, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    private String actionOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return "";
        }
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().getOrDefault(actionKey, PersistentDataType.STRING, "");
    }

    private record ReforgeCost(boolean usesCoins, double coins, int levels) {
        private static ReforgeCost coins(double amount) {
            return new ReforgeCost(true, amount, 0);
        }

        private static ReforgeCost levels(int amount) {
            return new ReforgeCost(false, 0.0D, amount);
        }
    }

    private enum ReforgeView {
        MENU,
        ADVANCED
    }

    private static final class ReforgeHolder implements InventoryHolder {
        private Inventory inventory;
        private final ReforgeView viewType;

        private ReforgeHolder(ReforgeView viewType) {
            this.viewType = viewType;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
