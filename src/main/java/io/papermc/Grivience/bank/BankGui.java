package io.papermc.Grivience.bank;

import io.papermc.Grivience.gui.SkyblockGui;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Simple Hypixel-style bank GUI.
 * <p>
 * Coins are stored on the currently selected Skyblock profile.
 */
public final class BankGui {
    public static final int SIZE = 54;

    public static final int HEADER_SLOT = 4;
    public static final int DEPOSIT_MENU_SLOT = 29;
    public static final int WITHDRAW_MENU_SLOT = 33;

    public static final int BACK_SLOT = 45;
    public static final int CLOSE_SLOT = 49;

    // Amount buttons (deposit/withdraw views)
    public static final int AMOUNT_100_SLOT = 20;
    public static final int AMOUNT_1K_SLOT = 21;
    public static final int AMOUNT_10K_SLOT = 22;
    public static final int AMOUNT_100K_SLOT = 23;
    public static final int AMOUNT_1M_SLOT = 24;
    public static final int AMOUNT_ALL_SLOT = 29;
    public static final int AMOUNT_CUSTOM_SLOT = 31;

    private BankGui() {
    }

    public enum ViewType {
        MAIN,
        DEPOSIT,
        WITHDRAW
    }

    public static boolean isBankInventory(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof BankHolder;
    }

    public static ViewType viewType(Inventory inventory) {
        if (!isBankInventory(inventory)) {
            return null;
        }
        return ((BankHolder) inventory.getHolder()).getViewType();
    }

    public static Inventory create(Player viewer, ViewType viewType, long purseCoins, long bankCoins) {
        String title = switch (viewType == null ? ViewType.MAIN : viewType) {
            case MAIN -> "Bank";
            case DEPOSIT -> "Bank Deposit";
            case WITHDRAW -> "Bank Withdrawal";
        };

        BankHolder holder = new BankHolder(viewType);
        Inventory inv = Bukkit.createInventory(holder, SIZE, SkyblockGui.title(title));
        holder.setInventory(inv);

        buildBase(inv);
        populate(inv, viewType, viewer, purseCoins, bankCoins);
        return inv;
    }

    public static void populate(Inventory inv, ViewType viewType, Player viewer, long purseCoins, long bankCoins) {
        if (inv == null) {
            return;
        }
        ViewType type = viewType == null ? ViewType.MAIN : viewType;

        inv.setItem(HEADER_SLOT, summaryItem(viewer, purseCoins, bankCoins, type));

        if (type == ViewType.MAIN) {
            inv.setItem(DEPOSIT_MENU_SLOT, depositMenuButton());
            inv.setItem(WITHDRAW_MENU_SLOT, withdrawMenuButton());
            inv.setItem(BACK_SLOT, SkyblockGui.backButton("Skyblock Menu"));
            inv.setItem(CLOSE_SLOT, SkyblockGui.closeButton());
            return;
        }

        // Deposit / Withdraw views
        boolean deposit = type == ViewType.DEPOSIT;
        inv.setItem(AMOUNT_100_SLOT, amountButton(deposit, 100));
        inv.setItem(AMOUNT_1K_SLOT, amountButton(deposit, 1_000));
        inv.setItem(AMOUNT_10K_SLOT, amountButton(deposit, 10_000));
        inv.setItem(AMOUNT_100K_SLOT, amountButton(deposit, 100_000));
        inv.setItem(AMOUNT_1M_SLOT, amountButton(deposit, 1_000_000));
        inv.setItem(AMOUNT_ALL_SLOT, allButton(deposit));
        inv.setItem(AMOUNT_CUSTOM_SLOT, customButton(deposit));

        inv.setItem(BACK_SLOT, SkyblockGui.backButton("Bank"));
        inv.setItem(CLOSE_SLOT, SkyblockGui.closeButton());
    }

    public static void playOpenSound(Player player) {
        if (player == null) {
            return;
        }
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.2F);
    }

    private static void buildBase(Inventory inv) {
        ItemStack bg = SkyblockGui.filler(Material.BLACK_STAINED_GLASS_PANE);
        ItemStack border = SkyblockGui.filler(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < inv.getSize(); slot++) {
            boolean isBorder = slot < 9 || slot >= inv.getSize() - 9 || slot % 9 == 0 || slot % 9 == 8;
            inv.setItem(slot, (isBorder ? border : bg).clone());
        }
    }

    private static ItemStack summaryItem(Player viewer, long purseCoins, long bankCoins, ViewType viewType) {
        String name = ChatColor.GOLD + "" + ChatColor.BOLD + "Bank Account";
        Material icon = Material.PAPER;

        List<String> lore = new ArrayList<>();
        if (viewType == ViewType.MAIN) {
            lore.add(ChatColor.GRAY + "Store coins safely in your bank.");
        } else if (viewType == ViewType.DEPOSIT) {
            lore.add(ChatColor.GRAY + "Move coins from your purse into the bank.");
        } else {
            lore.add(ChatColor.GRAY + "Move coins from the bank into your purse.");
        }
        lore.add("");
        lore.add(ChatColor.GRAY + "Purse: " + ChatColor.GOLD + formatCoins(purseCoins) + " coins");
        lore.add(ChatColor.GRAY + "Bank: " + ChatColor.GOLD + formatCoins(bankCoins) + " coins");
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "Profile-scoped (instanced balance).");

        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack depositMenuButton() {
        return SkyblockGui.button(
                Material.CHEST,
                ChatColor.GREEN + "Deposit Coins",
                List.of(
                        ChatColor.GRAY + "Move coins from your purse",
                        ChatColor.GRAY + "into your bank.",
                        "",
                        ChatColor.YELLOW + "Click to deposit!"
                )
        );
    }

    private static ItemStack withdrawMenuButton() {
        return SkyblockGui.button(
                Material.ENDER_CHEST,
                ChatColor.AQUA + "Withdraw Coins",
                List.of(
                        ChatColor.GRAY + "Move coins from your bank",
                        ChatColor.GRAY + "into your purse.",
                        "",
                        ChatColor.YELLOW + "Click to withdraw!"
                )
        );
    }

    private static ItemStack amountButton(boolean deposit, long amount) {
        long safe = Math.max(0L, amount);
        Material mat = deposit ? Material.EMERALD : Material.REDSTONE;
        String verb = deposit ? "Deposit" : "Withdraw";
        ChatColor color = deposit ? ChatColor.GREEN : ChatColor.RED;

        return SkyblockGui.button(
                mat,
                color + verb + " " + formatCoins(safe),
                List.of(
                        ChatColor.GRAY + "Amount: " + ChatColor.GOLD + formatCoins(safe) + " coins",
                        "",
                        ChatColor.YELLOW + "Click to " + verb.toLowerCase(Locale.ROOT) + "!"
                )
        );
    }

    private static ItemStack allButton(boolean deposit) {
        String verb = deposit ? "Deposit" : "Withdraw";
        ChatColor color = deposit ? ChatColor.GREEN : ChatColor.RED;
        Material mat = deposit ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK;

        return SkyblockGui.button(
                mat,
                color + verb + " All",
                List.of(
                        ChatColor.GRAY + "Move all available coins.",
                        "",
                        ChatColor.YELLOW + "Click to " + verb.toLowerCase(Locale.ROOT) + " all!"
                )
        );
    }

    private static ItemStack customButton(boolean deposit) {
        String verb = deposit ? "Deposit" : "Withdraw";
        ChatColor color = deposit ? ChatColor.GREEN : ChatColor.RED;

        return SkyblockGui.button(
                Material.OAK_SIGN,
                color + "Custom " + verb,
                List.of(
                        ChatColor.GRAY + "Enter an amount in chat.",
                        "",
                        ChatColor.YELLOW + "Click to set an amount!",
                        ChatColor.DARK_GRAY + "Type 'cancel' to stop."
                )
        );
    }

    private static String formatCoins(long coins) {
        return String.format(Locale.US, "%,d", Math.max(0L, coins));
    }

    public static final class BankHolder implements InventoryHolder {
        private final ViewType viewType;
        private Inventory inventory;

        public BankHolder(ViewType viewType) {
            this.viewType = viewType == null ? ViewType.MAIN : viewType;
        }

        public ViewType getViewType() {
            return viewType;
        }

        private void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}

