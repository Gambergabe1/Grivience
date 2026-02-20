package io.papermc.Grivience.listener;

import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.item.ItemRarity;
import io.papermc.Grivience.item.ReforgeStoneType;
import io.papermc.Grivience.item.ReforgeType;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
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
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public final class ReforgeAnvilGuiListener implements Listener {
    private static final String TITLE = ChatColor.DARK_AQUA + "Reforge Anvil";
    private static final int WEAPON_SLOT = 11;
    private static final int PREVIEW_SLOT = 13;
    private static final int STONE_SLOT = 15;
    private static final int CONFIRM_SLOT = 22;
    private static final int INFO_SLOT = 4;
    private static final int CLOSE_SLOT = 26;

    private final JavaPlugin plugin;
    private final CustomItemService customItemService;
    private final NamespacedKey actionKey;

    public ReforgeAnvilGuiListener(JavaPlugin plugin, CustomItemService customItemService) {
        this.plugin = plugin;
        this.customItemService = customItemService;
        this.actionKey = new NamespacedKey(plugin, "reforge-ui-action");
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

        Player player = event.getPlayer();
        Material type = event.getClickedBlock().getType();
        if (type != Material.ANVIL && type != Material.CHIPPED_ANVIL && type != Material.DAMAGED_ANVIL) {
            return;
        }
        if (!shouldOpenReforgeUi(player)) {
            return;
        }

        event.setCancelled(true);
        openReforgeUi(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof ReforgeHolder)) {
            return;
        }

        event.setCancelled(true);
        if (event.getClickedInventory() == null) {
            return;
        }

        if (Objects.equals(event.getClickedInventory(), top)) {
            handleTopClick(player, top, event.getRawSlot(), event.getCurrentItem());
            return;
        }

        handleBottomClick(player, top, event.getClickedInventory(), event.getSlot(), event.getCurrentItem());
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
        if (!(top.getHolder() instanceof ReforgeHolder)) {
            return;
        }

        returnInputToPlayer(player, top.getItem(WEAPON_SLOT), true);
        returnInputToPlayer(player, top.getItem(STONE_SLOT), false);
        top.setItem(WEAPON_SLOT, weaponPlaceholder());
        top.setItem(STONE_SLOT, stonePlaceholder());
    }

    private void handleTopClick(Player player, Inventory top, int rawSlot, ItemStack clicked) {
        if (rawSlot == WEAPON_SLOT) {
            returnInputToPlayer(player, top.getItem(WEAPON_SLOT), true);
            top.setItem(WEAPON_SLOT, weaponPlaceholder());
            refreshUi(player, top);
            return;
        }
        if (rawSlot == STONE_SLOT) {
            returnInputToPlayer(player, top.getItem(STONE_SLOT), false);
            top.setItem(STONE_SLOT, stonePlaceholder());
            refreshUi(player, top);
            return;
        }

        String action = actionOf(clicked);
        if ("confirm".equals(action)) {
            applyReforge(player, top);
            refreshUi(player, top);
            return;
        }
        if ("close".equals(action)) {
            player.closeInventory();
        }
    }

    private void handleBottomClick(
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
            refreshUi(player, top);
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
        refreshUi(player, top);
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

    private void applyReforge(Player player, Inventory top) {
        ItemStack weapon = top.getItem(WEAPON_SLOT);
        ItemStack stone = top.getItem(STONE_SLOT);
        if (!customItemService.isCustomDungeonWeapon(weapon)) {
            player.sendMessage(ChatColor.RED + "Insert a dungeon weapon first.");
            return;
        }

        ReforgeStoneType stoneType = ReforgeStoneType.parse(customItemService.itemId(stone));
        ReforgeCost cost = resolveCost(stoneType, weapon);
        if (!chargeCost(player, cost)) {
            return;
        }

        ReforgeType targetReforge = stoneType != null ? stoneType.reforgeType() : randomBasicReforge();
        ItemStack result = customItemService.applyReforge(weapon, targetReforge);
        result = customItemService.syncWeaponEnchantLore(result);
        giveToPlayer(player, result);

        top.setItem(WEAPON_SLOT, weaponPlaceholder());
        top.setItem(STONE_SLOT, stonePlaceholder());
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0F, 1.1F);

        if (stoneType == null) {
            player.sendMessage(ChatColor.GREEN + "Rolled random reforge: " + targetReforge.color() + targetReforge.displayName() + ChatColor.GREEN + ".");
        } else {
            player.sendMessage(ChatColor.GREEN + "Applied " + targetReforge.displayName() + " reforge.");
        }
    }

    public void openReforgeUi(Player player) {
        ReforgeHolder holder = new ReforgeHolder();
        Inventory inventory = Bukkit.createInventory(holder, 27, TITLE);
        holder.inventory = inventory;

        ItemStack filler = taggedItem(Material.BLACK_STAINED_GLASS_PANE, " ", List.of(), "filler");
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }

        inventory.setItem(INFO_SLOT, taggedItem(
                Material.ANVIL,
                ChatColor.AQUA + "Hypixel-Style Reforge",
                List.of(
                        ChatColor.GRAY + "Insert a custom dungeon weapon.",
                        ChatColor.GRAY + "No stone = Blacksmith pool roll.",
                        ChatColor.GRAY + "Stone = targeted reforge (incl. stone-only).",
                        ChatColor.GRAY + "Cost scales with weapon rarity."
                ),
                "info"
        ));
        inventory.setItem(WEAPON_SLOT, weaponPlaceholder());
        inventory.setItem(STONE_SLOT, stonePlaceholder());
        inventory.setItem(CLOSE_SLOT, taggedItem(
                Material.BARRIER,
                ChatColor.RED + "Close",
                List.of(ChatColor.GRAY + "Return inserted items."),
                "close"
        ));
        refreshUi(player, inventory);
        player.openInventory(inventory);
    }

    private void refreshUi(Player player, Inventory inventory) {
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

        ItemRarity rarity = customItemService.rarityOf(weapon);
        ReforgeCost cost = resolveCost(stoneType, weapon);
        boolean affordable = canAfford(player, cost);

        if (stoneType == null) {
            ItemStack preview = weapon.clone();
            ItemMeta previewMeta = preview.getItemMeta();
            List<String> previewLore = previewMeta.hasLore() && previewMeta.getLore() != null
                    ? new ArrayList<>(previewMeta.getLore())
                    : new ArrayList<>();
            previewLore.add("");
            previewLore.add(ChatColor.GRAY + "Mode: " + ChatColor.YELLOW + "Blacksmith Random Reforge");
            previewLore.add(ChatColor.GRAY + "Weapon Rarity: " + rarity.color() + rarity.displayName());
            previewLore.add(ChatColor.GRAY + "Pool: " + blacksmithPoolDisplay());
            previewLore.add(ChatColor.GRAY + "Cost: " + ChatColor.GREEN + formatCost(cost));
            previewMeta.setLore(previewLore);
            preview.setItemMeta(previewMeta);
            inventory.setItem(PREVIEW_SLOT, preview);

            List<String> confirmLore = new ArrayList<>();
            confirmLore.add(ChatColor.GRAY + "Mode: " + ChatColor.YELLOW + "Blacksmith Random Reforge");
            confirmLore.add(ChatColor.GRAY + "Weapon Rarity: " + rarity.color() + rarity.displayName());
            confirmLore.add(ChatColor.GRAY + "Cost: " + ChatColor.GREEN + formatCost(cost));
            confirmLore.add(balanceLine(player, cost));
            confirmLore.add("");
            confirmLore.add(affordable
                    ? ChatColor.GREEN + "Click to roll blacksmith reforge"
                    : ChatColor.RED + "You cannot afford this roll");
            inventory.setItem(CONFIRM_SLOT, taggedItem(
                    affordable ? Material.EMERALD : Material.REDSTONE,
                    affordable ? ChatColor.GREEN + "Roll Blacksmith Reforge" : ChatColor.RED + "Cannot Reforge",
                    confirmLore,
                    "confirm"
            ));
            return;
        }

        ItemStack preview = customItemService.applyReforge(weapon, stoneType.reforgeType());
        preview = customItemService.syncWeaponEnchantLore(preview);
        ItemMeta previewMeta = preview.getItemMeta();
        List<String> previewLore = previewMeta.hasLore() && previewMeta.getLore() != null
                ? new ArrayList<>(previewMeta.getLore())
                : new ArrayList<>();
        previewLore.add("");
        previewLore.add(ChatColor.GRAY + "Mode: " + ChatColor.YELLOW + "Stone Reforge");
        previewLore.add(ChatColor.GRAY + "Weapon Rarity: " + rarity.color() + rarity.displayName());
        previewLore.add(ChatColor.GRAY + "Cost: " + ChatColor.GREEN + formatCost(cost));
        previewLore.add(ChatColor.GRAY + customItemService.reforgeBonusLine(stoneType.reforgeType(), weapon));
        previewMeta.setLore(previewLore);
        preview.setItemMeta(previewMeta);
        inventory.setItem(PREVIEW_SLOT, preview);

        List<String> confirmLore = new ArrayList<>();
        confirmLore.add(ChatColor.GRAY + "Mode: " + ChatColor.YELLOW + "Stone Reforge");
        confirmLore.add(ChatColor.GRAY + "Weapon Rarity: " + rarity.color() + rarity.displayName());
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

    private boolean shouldOpenReforgeUi(Player player) {
        return isReforgeContextItem(player.getInventory().getItemInMainHand())
                || isReforgeContextItem(player.getInventory().getItemInOffHand());
    }

    private boolean isReforgeContextItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        if (customItemService.isCustomDungeonWeapon(item)) {
            return true;
        }
        String itemId = customItemService.itemId(item);
        if (itemId == null) {
            return false;
        }
        return ReforgeStoneType.parse(itemId) != null;
    }

    private ReforgeType randomBasicReforge() {
        List<ReforgeType> pool = ReforgeType.blacksmithPool();
        if (pool.isEmpty()) {
            return ReforgeType.JAGGED;
        }
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    private ReforgeCost resolveCost(ReforgeStoneType stoneType, ItemStack weapon) {
        double rarityMultiplier = rarityCostMultiplier(customItemService.rarityOf(weapon));
        if (useVaultEconomy()) {
            double coins = stoneType == null ? basicCoinCost() : stoneType.levelCost() * stoneCoinMultiplier();
            return ReforgeCost.coins(Math.max(1.0D, Math.round(coins * rarityMultiplier)));
        }
        int levels = stoneType == null ? basicLevelCost() : stoneType.levelCost();
        return ReforgeCost.levels(Math.max(1, (int) Math.round(levels * rarityMultiplier)));
    }

    private boolean canAfford(Player player, ReforgeCost cost) {
        if (cost.usesCoins()) {
            Economy economy = economy();
            return economy != null && economy.has(player, cost.coins());
        }
        return player.getLevel() >= cost.levels();
    }

    private boolean chargeCost(Player player, ReforgeCost cost) {
        if (cost.usesCoins()) {
            Economy economy = economy();
            if (economy == null) {
                player.sendMessage(ChatColor.RED + "Vault economy is unavailable.");
                return false;
            }
            if (!economy.has(player, cost.coins())) {
                player.sendMessage(ChatColor.RED + "You need " + formatCost(cost) + " to reforge.");
                return false;
            }
            EconomyResponse response = economy.withdrawPlayer(player, cost.coins());
            if (!response.transactionSuccess()) {
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
            Economy economy = economy();
            double balance = economy == null ? 0.0D : economy.getBalance(player);
            return ChatColor.GRAY + "Balance: " + ChatColor.YELLOW + "$" + String.format(Locale.ROOT, "%,.0f", balance);
        }
        return ChatColor.GRAY + "Your Levels: " + ChatColor.YELLOW + player.getLevel();
    }

    private int basicLevelCost() {
        return Math.max(1, plugin.getConfig().getInt("custom-items.reforge.basic-level-cost", 5));
    }

    private double basicCoinCost() {
        return Math.max(1.0D, plugin.getConfig().getDouble("custom-items.reforge.basic-cost-coins", 2500.0D));
    }

    private double stoneCoinMultiplier() {
        return Math.max(1.0D, plugin.getConfig().getDouble("custom-items.reforge.stone-cost-multiplier", 1200.0D));
    }

    private double rarityCostMultiplier(ItemRarity rarity) {
        ItemRarity effectiveRarity = rarity == null ? ItemRarity.RARE : rarity;
        String path = "custom-items.reforge.rarity-cost-multipliers." + effectiveRarity.name();
        return Math.max(0.10D, plugin.getConfig().getDouble(path, defaultRarityMultiplier(effectiveRarity)));
    }

    private double defaultRarityMultiplier(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> 0.75D;
            case UNCOMMON -> 0.90D;
            case RARE -> 1.00D;
            case EPIC -> 1.30D;
            case LEGENDARY -> 1.70D;
            case MYTHIC -> 2.20D;
        };
    }

    private boolean useVaultEconomy() {
        return plugin.getConfig().getBoolean("custom-items.reforge.use-vault-economy", true) && economy() != null;
    }

    private String blacksmithPoolDisplay() {
        List<ReforgeType> pool = ReforgeType.blacksmithPool();
        if (pool.isEmpty()) {
            return ChatColor.DARK_GRAY + "None";
        }
        return pool.stream()
                .map(type -> type.color() + type.displayName())
                .collect(Collectors.joining(ChatColor.GRAY + ", "));
    }

    private Economy economy() {
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            return null;
        }
        RegisteredServiceProvider<Economy> registration = Bukkit.getServer()
                .getServicesManager()
                .getRegistration(Economy.class);
        return registration == null ? null : registration.getProvider();
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
                List.of(
                        ChatColor.GRAY + "Insert a Reforge Stone for target reforge,",
                        ChatColor.GRAY + "or leave empty for random basic roll."
                ),
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

    private static final class ReforgeHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
