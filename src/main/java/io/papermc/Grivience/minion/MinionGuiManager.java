package io.papermc.Grivience.minion;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.gui.SkyblockGui;
import io.papermc.Grivience.skyblock.island.Island;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class MinionGuiManager implements Listener {
    private static final String TITLE_OVERVIEW = SkyblockGui.title("Minion Management");
    private static final String TITLE_SINGLE_PREFIX = SkyblockGui.title("Minion ");

    private static final int SLOT_MINION_INFO = 13;
    private static final int SLOT_FUEL = 19;
    private static final int SLOT_SHIPPING = 28;
    private static final int SLOT_UPGRADE_ONE = 21;
    private static final int SLOT_UPGRADE_TWO = 23;
    private static final int SLOT_COLLECT = 31;
    private static final int SLOT_PICKUP = 48;
    private static final int SLOT_UPGRADE_TIER = 49;
    private static final int SLOT_BACK = 45;
    private static final int SLOT_CLOSE = 53;
    private static final int[] STORAGE_SLOTS = {6, 7, 8, 15, 16, 17, 24, 25, 26, 33, 34, 35, 42, 43, 44};

    private final GriviencePlugin plugin;
    private final MinionManager minionManager;
    private final NamespacedKey actionKey;
    private final NamespacedKey valueKey;

    public MinionGuiManager(GriviencePlugin plugin, MinionManager minionManager) {
        this.plugin = plugin;
        this.minionManager = minionManager;
        this.actionKey = new NamespacedKey(plugin, "minion-gui-action");
        this.valueKey = new NamespacedKey(plugin, "minion-gui-value");
    }

    public void openOverview(Player player) {
        if (player == null) {
            return;
        }
        if (minionManager == null || !minionManager.isEnabled()) {
            player.sendMessage(ChatColor.RED + "Minions are currently disabled.");
            return;
        }

        Island island = plugin.getIslandManager() != null ? plugin.getIslandManager().getIsland(player) : null;
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You don't have an island.");
            return;
        }
        if (!minionManager.canManageIsland(player, island)) {
            player.sendMessage(ChatColor.RED + "You cannot manage minions on this island.");
            return;
        }

        Inventory inventory = Bukkit.createInventory(
                new MinionMenuHolder(MinionMenuType.OVERVIEW, island.getId(), null),
                54,
                TITLE_OVERVIEW
        );
        fillBackground(inventory);

        List<MinionInstance> minions = minionManager.getMinionsForIsland(island);
        int limit = minionManager.getIslandMinionLimit(island);
        long totalStored = minionManager.getTotalStored(island);
        MinionManager.ConstellationInfo constellationInfo = minionManager.getConstellationInfo(island);

        inventory.setItem(4, createMenuItem(
                Material.PLAYER_HEAD,
                ChatColor.GREEN + "Island Minions",
                List.of(
                        ChatColor.GRAY + "Manage your placed minions.",
                        "",
                        ChatColor.GRAY + "Placed: " + ChatColor.AQUA + minions.size()
                                + ChatColor.DARK_GRAY + "/" + ChatColor.AQUA + (limit < 0 ? "Unlimited" : limit),
                        ChatColor.GRAY + "Stored Items: " + ChatColor.YELLOW + formatInt(totalStored),
                        ChatColor.GRAY + "Constellation: " + ChatColor.LIGHT_PURPLE + minionManager.constellationTierName(constellationInfo.tier())
                                + ChatColor.DARK_GRAY + " (T" + constellationInfo.tier() + ")",
                        ChatColor.GRAY + "Synergy Speed: " + ChatColor.GREEN + "+" + formatPercent(constellationInfo.speedMultiplier() - 1.0D),
                        ChatColor.GRAY + "Fragment Chance: " + ChatColor.LIGHT_PURPLE + formatPercent(constellationInfo.fragmentChance())
                ),
                "noop",
                ""
        ));

        inventory.setItem(12, createMenuItem(
                Material.CHEST,
                ChatColor.GOLD + "Collect All Resources",
                List.of(
                        ChatColor.GRAY + "Collects all stored resources",
                        ChatColor.GRAY + "from every placed minion.",
                        "",
                        ChatColor.YELLOW + "Click to collect"
                ),
                "collect_all",
                ""
        ));

        inventory.setItem(14, createMenuItem(
                Material.BARRIER,
                ChatColor.RED + "Pick Up All Minions",
                List.of(
                        ChatColor.GRAY + "Collects resources and picks up",
                        ChatColor.GRAY + "all minions on this island.",
                        "",
                        ChatColor.RED + "Returns minions as items."
                ),
                "pickup_all",
                ""
        ));

        int[] slots = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        if (minions.isEmpty()) {
            inventory.setItem(22, createMenuItem(
                    Material.GRAY_DYE,
                    ChatColor.GRAY + "No Minions Placed",
                    List.of(
                            ChatColor.GRAY + "Place a minion item on your island",
                            ChatColor.GRAY + "to start automatic production."
                    ),
                    "noop",
                    ""
            ));
        } else {
            int idx = 0;
            for (MinionInstance minion : minions) {
                if (idx >= slots.length) {
                    break;
                }
                int storageCap = minionManager.getStorageCap(minion.type(), minion.tier());
                MinionManager.FuelInfo fuel = minionManager.getFuelInfo(minion);
                int upgrades = countInstalledUpgrades(minion);

                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Stored: " + ChatColor.YELLOW + formatInt(minion.storedAmount())
                        + ChatColor.DARK_GRAY + "/" + ChatColor.YELLOW + formatInt(storageCap));
                lore.add(ChatColor.GRAY + "Speed: " + ChatColor.AQUA
                        + String.format(Locale.US, "%.1fs/action", minionManager.getSecondsPerAction(minion.type(), minion.tier())));
                lore.add(ChatColor.GRAY + "Fuel: " + (fuel == null ? ChatColor.RED + "None" : ChatColor.GREEN + fuel.displayName()));
                lore.add(ChatColor.GRAY + "Upgrades: " + ChatColor.GOLD + upgrades + ChatColor.DARK_GRAY + "/2");
                lore.add(ChatColor.GRAY + "Hopper Coins: " + ChatColor.GOLD + formatInt(Math.round(minion.hopperCoins())));
                lore.add("");
                lore.add(ChatColor.YELLOW + "Click to manage");

                inventory.setItem(slots[idx], createMenuItem(
                        minion.type().iconMaterial(),
                        ChatColor.GREEN + minion.type().displayName() + " Minion " + ChatColor.YELLOW + roman(minion.tier()),
                        lore,
                        "open_minion",
                        minion.id().toString()
                ));
                idx++;
            }
        }

        inventory.setItem(48, createMenuItem(
                Material.ARROW,
                ChatColor.YELLOW + "Back",
                List.of(ChatColor.GRAY + "Go back to Island Menu"),
                "back_island",
                ""
        ));
        inventory.setItem(49, createMenuItem(
                Material.BARRIER,
                ChatColor.RED + "Close",
                List.of(ChatColor.GRAY + "Close this menu"),
                "close",
                ""
        ));

        player.openInventory(inventory);
    }

    public void openMinion(Player player, UUID minionId) {
        if (player == null || minionId == null) {
            return;
        }
        MinionInstance minion = minionManager.getMinion(minionId);
        if (minion == null) {
            player.sendMessage(ChatColor.RED + "That minion no longer exists.");
            return;
        }
        if (!minionManager.canManageMinion(player, minion)) {
            player.sendMessage(ChatColor.RED + "You cannot manage that minion.");
            return;
        }

        Inventory inventory = Bukkit.createInventory(
                new MinionMenuHolder(MinionMenuType.SINGLE, minion.islandId(), minion.id()),
                54,
                TITLE_SINGLE_PREFIX + minion.type().displayName()
        );
        fillBackground(inventory);

        int storageCap = minionManager.getStorageCap(minion.type(), minion.tier());
        MinionManager.FuelInfo fuel = minionManager.getFuelInfo(minion);
        MinionManager.ConstellationInfo constellationInfo = minionManager.getConstellationInfo(minion.islandId());
        String upgOne = minion.upgradeSlotOneId();
        String upgTwo = minion.upgradeSlotTwoId();

        inventory.setItem(SLOT_MINION_INFO, createMenuItem(
                minion.type().iconMaterial(),
                ChatColor.GREEN + minion.type().displayName() + " Minion " + ChatColor.YELLOW + roman(minion.tier()),
                List.of(
                        ChatColor.GRAY + "Stored: " + ChatColor.YELLOW + formatInt(minion.storedAmount())
                                + ChatColor.DARK_GRAY + "/" + ChatColor.YELLOW + formatInt(storageCap),
                        ChatColor.GRAY + "Speed: " + ChatColor.AQUA
                                + String.format(Locale.US, "%.1fs/action", minionManager.getSecondsPerAction(minion.type(), minion.tier())),
                        ChatColor.GRAY + "Constellation: " + ChatColor.LIGHT_PURPLE + minionManager.constellationTierName(constellationInfo.tier())
                                + ChatColor.DARK_GRAY + " (+" + formatPercent(constellationInfo.speedMultiplier() - 1.0D) + " speed)",
                        ChatColor.GRAY + "Constellation Fragments: " + ChatColor.LIGHT_PURPLE + formatPercent(constellationInfo.fragmentChance()) + ChatColor.GRAY + " chance",
                        ChatColor.GRAY + "Fuel: " + (fuel == null ? ChatColor.RED + "None" : ChatColor.GREEN + fuel.displayName()),
                        ChatColor.GRAY + "Hopper Coins: " + ChatColor.GOLD + formatInt(Math.round(minion.hopperCoins())),
                        "",
                        ChatColor.DARK_GRAY + "Right-click this minion in-world to reopen this menu."
                ),
                "noop",
                ""
        ));

        inventory.setItem(SLOT_FUEL, fuelSlotItem(fuel));
        inventory.setItem(SLOT_UPGRADE_ONE, upgradeSlotItem(upgOne, 1));
        inventory.setItem(SLOT_UPGRADE_TWO, upgradeSlotItem(upgTwo, 2));
        inventory.setItem(SLOT_SHIPPING, shippingSlotItem(minion));

        Map<String, Integer> storedItems = minionManager.getStoredItems(minion);
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(storedItems.entrySet());
        entries.sort(Comparator.comparingInt((Map.Entry<String, Integer> e) -> Math.max(0, e.getValue() == null ? 0 : e.getValue())).reversed());

        for (int i = 0; i < STORAGE_SLOTS.length; i++) {
            int slot = STORAGE_SLOTS[i];
            if (i >= entries.size()) {
                inventory.setItem(slot, createMenuItem(
                        Material.GRAY_STAINED_GLASS_PANE,
                        ChatColor.DARK_GRAY + "Empty Storage",
                        List.of(ChatColor.GRAY + "Generated items appear here."),
                        "noop",
                        ""
                ));
                continue;
            }

            Map.Entry<String, Integer> entry = entries.get(i);
            String itemId = entry.getKey();
            int amount = Math.max(0, entry.getValue() == null ? 0 : entry.getValue());
            ItemStack display = minionManager.createStoredDisplayItem(itemId, amount);
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "noop");
                meta.getPersistentDataContainer().set(valueKey, PersistentDataType.STRING, "");
                display.setItemMeta(meta);
            }
            inventory.setItem(slot, display);
        }

        inventory.setItem(SLOT_COLLECT, createMenuItem(
                Material.CHEST,
                ChatColor.GOLD + "Collect Resources",
                List.of(
                        ChatColor.GRAY + "Collect all stored items",
                        ChatColor.GRAY + "from this minion.",
                        "",
                        ChatColor.YELLOW + "Click to collect"
                ),
                "collect_one",
                minion.id().toString()
        ));

        int upgradeCost = minionManager.getUpgradeCostForCurrentTier(minion.type(), minion.tier());
        String upgradeIngredientId = minionManager.getUpgradeIngredientForCurrentTier(minion.type(), minion.tier());
        boolean canUpgrade = upgradeCost > 0 && upgradeIngredientId != null;

        if (canUpgrade) {
            inventory.setItem(SLOT_UPGRADE_TIER, createMenuItem(
                    Material.ANVIL,
                    ChatColor.AQUA + "Upgrade to Tier " + ChatColor.YELLOW + roman(minion.tier() + 1),
                    List.of(
                            ChatColor.GRAY + "Cost: " + ChatColor.YELLOW + formatInt(upgradeCost) + " "
                                    + minionManager.getIngredientDisplayName(upgradeIngredientId),
                            "",
                            ChatColor.YELLOW + "Click to upgrade tier"
                    ),
                    "upgrade_tier",
                    minion.id().toString()
            ));
        } else {
            inventory.setItem(SLOT_UPGRADE_TIER, createMenuItem(
                    Material.GOLD_BLOCK,
                    ChatColor.GOLD + "Max Tier",
                    List.of(ChatColor.GRAY + "This minion is fully upgraded."),
                    "noop",
                    ""
            ));
        }

        inventory.setItem(SLOT_PICKUP, createMenuItem(
                Material.BARRIER,
                ChatColor.RED + "Pick Up Minion",
                List.of(
                        ChatColor.GRAY + "Collects resources and returns",
                        ChatColor.GRAY + "this minion to item form.",
                        "",
                        ChatColor.YELLOW + "Click to pick up"
                ),
                "pickup_one",
                minion.id().toString()
        ));

        inventory.setItem(SLOT_BACK, createMenuItem(
                Material.ARROW,
                ChatColor.YELLOW + "Back",
                List.of(ChatColor.GRAY + "Return to Minion Management"),
                "back_overview",
                ""
        ));
        inventory.setItem(SLOT_CLOSE, createMenuItem(
                Material.BARRIER,
                ChatColor.RED + "Close",
                List.of(ChatColor.GRAY + "Close this menu"),
                "close",
                ""
        ));

        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof MinionMenuHolder holder)) {
            return;
        }

        if (holder.menuType == MinionMenuType.OVERVIEW) {
            event.setCancelled(true);
            handleOverviewClick(event, player);
            return;
        }
        handleSingleClick(event, player, holder);
    }

    private void handleOverviewClick(InventoryClickEvent event, Player player) {
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getInventory())) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (isEmpty(clicked) || !clicked.hasItemMeta()) {
            return;
        }

        String action = clicked.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        String value = clicked.getItemMeta().getPersistentDataContainer().getOrDefault(valueKey, PersistentDataType.STRING, "");
        if (action == null || action.isBlank()) {
            return;
        }

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
        switch (action) {
            case "close" -> player.closeInventory();
            case "back_island" -> {
                if (plugin.getSkyblockMenuManager() != null) {
                    plugin.getSkyblockMenuManager().openIslandMenu(player);
                } else {
                    player.closeInventory();
                }
            }
            case "collect_all" -> {
                minionManager.collectAll(player);
                openOverview(player);
            }
            case "pickup_all" -> {
                minionManager.pickupAll(player);
                openOverview(player);
            }
            case "open_minion" -> {
                UUID id = parseUuid(value);
                if (id != null) {
                    openMinion(player, id);
                }
            }
            default -> {
            }
        }
    }

    private void handleSingleClick(InventoryClickEvent event, Player player, MinionMenuHolder holder) {
        UUID minionId = holder.minionId;
        MinionInstance minion = minionManager.getMinion(minionId);
        if (minion == null) {
            event.setCancelled(true);
            player.closeInventory();
            player.sendMessage(ChatColor.RED + "That minion no longer exists.");
            return;
        }
        if (!minionManager.canManageMinion(player, minion)) {
            event.setCancelled(true);
            player.closeInventory();
            player.sendMessage(ChatColor.RED + "You cannot manage that minion.");
            return;
        }

        if (event.getClickedInventory() == null) {
            return;
        }

        boolean topInventoryClick = event.getClickedInventory().equals(event.getView().getTopInventory());
        if (!topInventoryClick) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
                handleShiftInsert(player, minion, event.getCurrentItem());
                openMinion(player, minion.id());
                return;
            }
            // Allow normal interactions in the player's own inventory while this menu is open.
            event.setCancelled(false);
            return;
        }
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot == SLOT_FUEL) {
            handleFuelSlotClick(event, player, minion);
            openMinion(player, minion.id());
            return;
        }
        if (slot == SLOT_UPGRADE_ONE) {
            handleUpgradeSlotClick(event, player, minion, 1);
            openMinion(player, minion.id());
            return;
        }
        if (slot == SLOT_UPGRADE_TWO) {
            handleUpgradeSlotClick(event, player, minion, 2);
            openMinion(player, minion.id());
            return;
        }
        if (slot == SLOT_SHIPPING) {
            handleShippingSlotClick(event, player, minion);
            openMinion(player, minion.id());
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (isEmpty(clicked) || !clicked.hasItemMeta()) {
            return;
        }
        ItemMeta meta = clicked.getItemMeta();
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        String value = meta.getPersistentDataContainer().getOrDefault(valueKey, PersistentDataType.STRING, "");
        if (action == null || action.isBlank()) {
            return;
        }

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
        switch (action) {
            case "close" -> player.closeInventory();
            case "back_overview" -> openOverview(player);
            case "collect_one" -> {
                UUID id = parseUuid(value);
                if (id != null) {
                    minionManager.collectMinion(player, id);
                    openMinion(player, id);
                }
            }
            case "upgrade_tier" -> {
                UUID id = parseUuid(value);
                if (id != null) {
                    minionManager.upgradeMinion(player, id);
                    openMinion(player, id);
                }
            }
            case "pickup_one" -> {
                UUID id = parseUuid(value);
                if (id != null && minionManager.pickupMinion(player, id)) {
                    openOverview(player);
                }
            }
            default -> {
            }
        }
    }

    private void handleFuelSlotClick(InventoryClickEvent event, Player player, MinionInstance minion) {
        ItemStack cursor = event.getCursor();
        if (isEmpty(cursor)) {
            ItemStack removed = minionManager.removeFuelFromMinion(player, minion.id());
            if (removed != null) {
                event.setCursor(removed);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8F, 1.1F);
            }
            return;
        }

        if (!minionManager.isFuelItem(cursor)) {
            player.sendMessage(ChatColor.RED + "That item cannot be used as minion fuel.");
            return;
        }
        if (minionManager.addFuelToMinion(player, minion.id(), cursor)) {
            event.setCursor(cursor.getAmount() <= 0 ? null : cursor);
            player.playSound(player.getLocation(), Sound.BLOCK_LAVA_POP, 0.7F, 1.2F);
        }
    }

    private void handleUpgradeSlotClick(InventoryClickEvent event, Player player, MinionInstance minion, int slotIndex) {
        ItemStack cursor = event.getCursor();
        if (isEmpty(cursor)) {
            ItemStack removed = minionManager.removeUpgradeSlot(player, minion.id(), slotIndex);
            if (removed != null) {
                event.setCursor(removed);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8F, 1.1F);
            }
            return;
        }

        if (!minionManager.isUpgradeItem(cursor)) {
            player.sendMessage(ChatColor.RED + "That item is not a minion upgrade.");
            return;
        }
        if (minionManager.setUpgradeSlot(player, minion.id(), slotIndex, cursor)) {
            event.setCursor(cursor.getAmount() <= 0 ? null : cursor);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.6F, 1.3F);
        }
    }

    private void handleShippingSlotClick(InventoryClickEvent event, Player player, MinionInstance minion) {
        ItemStack cursor = event.getCursor();
        if (isEmpty(cursor)) {
            if (event.getClick().isRightClick()) {
                int shippingSlot = shippingSlot(minion);
                if (shippingSlot > 0) {
                    ItemStack removed = minionManager.removeUpgradeSlot(player, minion.id(), shippingSlot);
                    if (removed != null) {
                        event.setCursor(removed);
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8F, 1.1F);
                    }
                }
                return;
            }

            long coins = minionManager.collectHopperCoins(player, minion.id());
            if (coins > 0L) {
                player.sendMessage(ChatColor.GREEN + "Collected " + ChatColor.GOLD + formatInt(coins)
                        + ChatColor.GREEN + " coins from minion hopper sales.");
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.2F);
            }
            return;
        }

        if (!minionManager.isUpgradeItem(cursor)) {
            player.sendMessage(ChatColor.RED + "Place a hopper upgrade here.");
            return;
        }
        String upgradeId = minionManager.readUpgradeId(cursor);
        if (!minionManager.isShippingUpgrade(upgradeId)) {
            player.sendMessage(ChatColor.RED + "Only hopper upgrades can be inserted in this slot.");
            return;
        }

        int targetSlot = shippingSlot(minion);
        if (targetSlot <= 0) {
            if (minion.upgradeSlotOneId() == null) {
                targetSlot = 1;
            } else if (minion.upgradeSlotTwoId() == null) {
                targetSlot = 2;
            }
        }
        if (targetSlot <= 0) {
            player.sendMessage(ChatColor.RED + "Both upgrade slots are occupied.");
            return;
        }

        if (minionManager.setUpgradeSlot(player, minion.id(), targetSlot, cursor)) {
            event.setCursor(cursor.getAmount() <= 0 ? null : cursor);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.6F, 1.3F);
        }
    }

    private void handleShiftInsert(Player player, MinionInstance minion, ItemStack stack) {
        if (isEmpty(stack)) {
            return;
        }
        if (minionManager.isFuelItem(stack)) {
            if (minionManager.addFuelToMinion(player, minion.id(), stack)) {
                player.playSound(player.getLocation(), Sound.BLOCK_LAVA_POP, 0.7F, 1.2F);
            }
            return;
        }

        if (!minionManager.isUpgradeItem(stack)) {
            return;
        }
        String upgradeId = minionManager.readUpgradeId(stack);

        if (minionManager.isShippingUpgrade(upgradeId)) {
            int slot = shippingSlot(minion);
            if (slot <= 0) {
                if (minion.upgradeSlotOneId() == null) {
                    slot = 1;
                } else if (minion.upgradeSlotTwoId() == null) {
                    slot = 2;
                }
            }
            if (slot > 0 && minionManager.setUpgradeSlot(player, minion.id(), slot, stack)) {
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.6F, 1.3F);
            }
            return;
        }

        if (minion.upgradeSlotOneId() == null) {
            if (minionManager.setUpgradeSlot(player, minion.id(), 1, stack)) {
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.6F, 1.3F);
            }
            return;
        }
        if (minion.upgradeSlotTwoId() == null) {
            if (minionManager.setUpgradeSlot(player, minion.id(), 2, stack)) {
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.6F, 1.3F);
            }
        }
    }

    private ItemStack fuelSlotItem(MinionManager.FuelInfo fuel) {
        if (fuel == null) {
            return createMenuItem(
                    Material.BLAZE_POWDER,
                    ChatColor.AQUA + "Fuel",
                    List.of(
                            ChatColor.GRAY + "No fuel installed.",
                            "",
                            ChatColor.YELLOW + "Click with a fuel item to insert.",
                            ChatColor.DARK_GRAY + "Click empty to clear current fuel."
                    ),
                    "noop",
                    ""
            );
        }

        ItemStack stack = minionManager.createFuelItem(fuel.id(), 1);
        if (stack == null) {
            return createMenuItem(Material.BARRIER, ChatColor.RED + "Invalid Fuel", List.of(), "noop", "");
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "Status: " + (fuel.active() ? ChatColor.GREEN + "Active" : ChatColor.RED + "Expired"));
            if (!fuel.permanent()) {
                lore.add(ChatColor.GRAY + "Remaining: " + ChatColor.YELLOW + formatDuration(fuel.remainingMs()));
            }
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click with fuel to add/replace.");
            lore.add(ChatColor.YELLOW + "Click empty to remove.");
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "noop");
            meta.getPersistentDataContainer().set(valueKey, PersistentDataType.STRING, "");
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack upgradeSlotItem(String upgradeId, int slot) {
        if (upgradeId == null || upgradeId.isBlank()) {
            return createMenuItem(
                    Material.GRAY_DYE,
                    ChatColor.GRAY + "Upgrade Slot " + slot,
                    List.of(
                            ChatColor.GRAY + "No upgrade installed.",
                            "",
                            ChatColor.YELLOW + "Click with an upgrade item",
                            ChatColor.YELLOW + "to insert."
                    ),
                    "noop",
                    ""
            );
        }

        ItemStack stack = minionManager.createUpgradeItem(upgradeId, 1);
        if (stack == null) {
            return createMenuItem(Material.BARRIER, ChatColor.RED + "Invalid Upgrade", List.of(), "noop", "");
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click with upgrade to replace.");
            lore.add(ChatColor.YELLOW + "Click empty to remove.");
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "noop");
            meta.getPersistentDataContainer().set(valueKey, PersistentDataType.STRING, "");
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack shippingSlotItem(MinionInstance minion) {
        int shippingSlot = shippingSlot(minion);
        String installed = shippingSlot == 1 ? minion.upgradeSlotOneId() : shippingSlot == 2 ? minion.upgradeSlotTwoId() : null;

        if (installed == null) {
            return createMenuItem(
                    Material.HOPPER,
                    ChatColor.GOLD + "Automated Shipping",
                    List.of(
                            ChatColor.GRAY + "No hopper installed.",
                            "",
                            ChatColor.YELLOW + "Place a hopper upgrade here",
                            ChatColor.YELLOW + "to auto-sell overflow items."
                    ),
                    "noop",
                    ""
            );
        }

        ItemStack stack = minionManager.createUpgradeItem(installed, 1);
        if (stack == null) {
            return createMenuItem(Material.BARRIER, ChatColor.RED + "Invalid Hopper", List.of(), "noop", "");
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "Stored Coins: " + ChatColor.GOLD + formatInt(Math.round(minion.hopperCoins())));
            lore.add(ChatColor.YELLOW + "Left-click empty to collect coins.");
            lore.add(ChatColor.YELLOW + "Right-click empty to remove hopper.");
            lore.add(ChatColor.YELLOW + "Click with hopper to replace.");
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "noop");
            meta.getPersistentDataContainer().set(valueKey, PersistentDataType.STRING, "");
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private int shippingSlot(MinionInstance minion) {
        if (minion == null) {
            return -1;
        }
        if (minionManager.isShippingUpgrade(minion.upgradeSlotOneId())) {
            return 1;
        }
        if (minionManager.isShippingUpgrade(minion.upgradeSlotTwoId())) {
            return 2;
        }
        return -1;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof MinionMenuHolder holder)) {
            return;
        }
        if (holder.menuType == MinionMenuType.OVERVIEW) {
            event.setCancelled(true);
            return;
        }
        int topSize = event.getView().getTopInventory().getSize();
        for (Integer rawSlot : event.getRawSlots()) {
            if (rawSlot != null && rawSlot >= 0 && rawSlot < topSize) {
                // Keep GUI slots protected; allow dragging inside player's inventory.
                event.setCancelled(true);
                return;
            }
        }
    }

    private UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    private ItemStack createMenuItem(Material material, String name, List<String> lore, String action, String value) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action == null ? "" : action);
        meta.getPersistentDataContainer().set(valueKey, PersistentDataType.STRING, value == null ? "" : value);
        item.setItemMeta(meta);
        return item;
    }

    private void fillBackground(Inventory inventory) {
        ItemStack filler = createMenuItem(Material.BLACK_STAINED_GLASS_PANE, " ", List.of(), "noop", "");
        ItemStack border = createMenuItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of(), "noop", "");
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
        int[] borderSlots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53};
        for (int slot : borderSlots) {
            if (slot < inventory.getSize()) {
                inventory.setItem(slot, border);
            }
        }
    }

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir() || item.getAmount() <= 0;
    }

    private static int countInstalledUpgrades(MinionInstance minion) {
        int count = 0;
        if (minion != null) {
            if (minion.upgradeSlotOneId() != null && !minion.upgradeSlotOneId().isBlank()) {
                count++;
            }
            if (minion.upgradeSlotTwoId() != null && !minion.upgradeSlotTwoId().isBlank()) {
                count++;
            }
        }
        return count;
    }

    private static String roman(int value) {
        return switch (value) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            case 11 -> "XI";
            case 12 -> "XII";
            default -> String.valueOf(value);
        };
    }

    private static String formatInt(long value) {
        return String.format(Locale.US, "%,d", value);
    }

    private static String formatPercent(double ratio) {
        double percent = ratio * 100.0D;
        if (Math.abs(percent - Math.rint(percent)) < 0.0001D) {
            return String.format(Locale.US, "%.0f%%", percent);
        }
        return String.format(Locale.US, "%.1f%%", percent);
    }

    private static String formatDuration(long ms) {
        if (ms <= 0L) {
            return "Expired";
        }
        long seconds = Math.max(1L, ms / 1000L);
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        if (hours <= 0L) {
            return minutes + "m";
        }
        if (minutes <= 0L) {
            return hours + "h";
        }
        return hours + "h " + minutes + "m";
    }

    private enum MinionMenuType {
        OVERVIEW,
        SINGLE
    }

    private static final class MinionMenuHolder implements InventoryHolder {
        private final MinionMenuType menuType;
        private final UUID islandId;
        private final UUID minionId;

        private MinionMenuHolder(MinionMenuType menuType, UUID islandId, UUID minionId) {
            this.menuType = menuType;
            this.islandId = islandId;
            this.minionId = minionId;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
