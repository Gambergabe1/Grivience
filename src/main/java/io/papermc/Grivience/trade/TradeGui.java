package io.papermc.Grivience.trade;

import io.papermc.Grivience.gui.SkyblockGui;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Hypixel-style trade GUI.
 *
 * Notes:
 * - Each participant gets their own inventory instance (both see their own offer on the left).
 * - Right side offer is mirrored from the other participant and is always locked.
 */
public final class TradeGui {
    public static final int SIZE = 54;

    public static final int COINS_SELF_SLOT = 45;
    public static final int COINS_OTHER_SLOT = 53;

    public static final int ACCEPT_SELF_SLOT = 47;
    public static final int CANCEL_SLOT = 49;
    public static final int ACCEPT_OTHER_SLOT = 51;

    public static final int[] LEFT_OFFER_SLOTS = new int[]{
            9, 10, 11, 12,
            18, 19, 20, 21,
            27, 28, 29, 30,
            36, 37, 38, 39
    };

    public static final int[] RIGHT_OFFER_SLOTS = new int[]{
            14, 15, 16, 17,
            23, 24, 25, 26,
            32, 33, 34, 35,
            41, 42, 43, 44
    };

    private static final boolean[] IS_LEFT_OFFER = new boolean[SIZE];
    private static final boolean[] IS_RIGHT_OFFER = new boolean[SIZE];

    static {
        for (int slot : LEFT_OFFER_SLOTS) {
            if (slot >= 0 && slot < SIZE) {
                IS_LEFT_OFFER[slot] = true;
            }
        }
        for (int slot : RIGHT_OFFER_SLOTS) {
            if (slot >= 0 && slot < SIZE) {
                IS_RIGHT_OFFER[slot] = true;
            }
        }
    }

    private TradeGui() {
    }

    public enum ViewType {
        TRADE,
        CONFIRM
    }

    public static boolean isLeftOfferSlot(int rawSlot) {
        return rawSlot >= 0 && rawSlot < SIZE && IS_LEFT_OFFER[rawSlot];
    }

    public static boolean isRightOfferSlot(int rawSlot) {
        return rawSlot >= 0 && rawSlot < SIZE && IS_RIGHT_OFFER[rawSlot];
    }

    public static boolean isTradeInventory(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof TradeHolder;
    }

    public static Inventory createTradeInventory(UUID sessionId, UUID viewerId, String viewerName, String otherName,
                                                 boolean viewerAccepted, boolean otherAccepted) {
        TradeHolder holder = new TradeHolder(sessionId, viewerId, ViewType.TRADE);
        String title = "Trade: " + (otherName == null ? "Player" : otherName);
        Inventory inv = Bukkit.createInventory(holder, SIZE, SkyblockGui.title(title));
        holder.setInventory(inv);

        buildBase(inv);
        updateTradeButtons(inv, viewerName, otherName, viewerAccepted, otherAccepted);
        return inv;
    }

    public static Inventory createConfirmInventory(UUID sessionId, UUID viewerId, String viewerName, String otherName,
                                                   boolean viewerConfirmed, boolean otherConfirmed,
                                                   ItemStack[] selfOffer, ItemStack[] otherOffer) {
        TradeHolder holder = new TradeHolder(sessionId, viewerId, ViewType.CONFIRM);
        String title = "Confirm Trade";
        Inventory inv = Bukkit.createInventory(holder, SIZE, SkyblockGui.title(title));
        holder.setInventory(inv);

        buildBase(inv);
        setSelfOffer(inv, selfOffer);
        setOtherOffer(inv, otherOffer);
        updateConfirmButtons(inv, viewerName, otherName, viewerConfirmed, otherConfirmed);
        return inv;
    }

    public static void buildBase(Inventory inv) {
        if (inv == null) {
            return;
        }

        ItemStack filler = SkyblockGui.filler(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler.clone());
        }

        // Clear offer slots so the UI looks like a trade window.
        for (int slot : LEFT_OFFER_SLOTS) {
            inv.setItem(slot, null);
        }
        for (int slot : RIGHT_OFFER_SLOTS) {
            inv.setItem(slot, null);
        }

        // Separator column.
        ItemStack sep = SkyblockGui.filler(Material.GRAY_STAINED_GLASS_PANE);
        int[] sepSlots = new int[]{4, 13, 22, 31, 40};
        for (int slot : sepSlots) {
            if (slot >= 0 && slot < inv.getSize()) {
                inv.setItem(slot, sep.clone());
            }
        }
    }

    public static void updateTradeButtons(Inventory inv, String viewerName, String otherName,
                                          boolean viewerAccepted, boolean otherAccepted) {
        if (inv == null) {
            return;
        }

        inv.setItem(ACCEPT_SELF_SLOT, acceptButton(viewerAccepted, false));
        inv.setItem(ACCEPT_OTHER_SLOT, otherStatus(otherAccepted, false, otherName));
        inv.setItem(CANCEL_SLOT, cancelButton());
    }

    public static void updateConfirmButtons(Inventory inv, String viewerName, String otherName,
                                            boolean viewerConfirmed, boolean otherConfirmed) {
        if (inv == null) {
            return;
        }

        inv.setItem(ACCEPT_SELF_SLOT, acceptButton(viewerConfirmed, true));
        inv.setItem(ACCEPT_OTHER_SLOT, otherStatus(otherConfirmed, true, otherName));
        inv.setItem(CANCEL_SLOT, cancelButton());
    }

    public static void updateCoinSlots(Inventory inv, String otherName, long selfCoins, long otherCoins, long selfPurse, boolean editable) {
        if (inv == null) {
            return;
        }
        inv.setItem(COINS_SELF_SLOT, coinOfferSelf(selfCoins, selfPurse, editable));
        inv.setItem(COINS_OTHER_SLOT, coinOfferOther(otherCoins, otherName));
    }

    public static ItemStack[] readSelfOffer(Inventory inv) {
        ItemStack[] out = new ItemStack[LEFT_OFFER_SLOTS.length];
        if (inv == null) {
            return out;
        }
        for (int i = 0; i < LEFT_OFFER_SLOTS.length; i++) {
            ItemStack item = inv.getItem(LEFT_OFFER_SLOTS[i]);
            out[i] = (item == null || item.getType().isAir()) ? null : item.clone();
        }
        return out;
    }

    public static void setSelfOffer(Inventory inv, ItemStack[] offer) {
        if (inv == null) {
            return;
        }
        for (int i = 0; i < LEFT_OFFER_SLOTS.length; i++) {
            ItemStack item = offer != null && i < offer.length ? offer[i] : null;
            inv.setItem(LEFT_OFFER_SLOTS[i], (item == null || item.getType().isAir()) ? null : item.clone());
        }
    }

    public static void setOtherOffer(Inventory inv, ItemStack[] offer) {
        if (inv == null) {
            return;
        }
        for (int i = 0; i < RIGHT_OFFER_SLOTS.length; i++) {
            ItemStack item = offer != null && i < offer.length ? offer[i] : null;
            inv.setItem(RIGHT_OFFER_SLOTS[i], (item == null || item.getType().isAir()) ? null : item.clone());
        }
    }

    private static ItemStack acceptButton(boolean accepted, boolean confirm) {
        Material mat;
        String name;
        List<String> lore = new ArrayList<>();

        if (confirm) {
            mat = accepted ? Material.GREEN_TERRACOTTA : Material.LIME_TERRACOTTA;
            name = accepted ? ChatColor.GREEN + "Confirmed" : ChatColor.YELLOW + "Confirm";
            lore.add(ChatColor.GRAY + "Confirm this trade.");
            if (!accepted) {
                lore.add("");
                lore.add(ChatColor.YELLOW + "Click to confirm!");
            }
        } else {
            mat = accepted ? Material.GREEN_WOOL : Material.LIME_WOOL;
            name = accepted ? ChatColor.GREEN + "Accepted" : ChatColor.YELLOW + "Accept";
            lore.add(ChatColor.GRAY + "Accept this trade.");
            if (!accepted) {
                lore.add("");
                lore.add(ChatColor.YELLOW + "Click to accept!");
            }
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack otherStatus(boolean accepted, boolean confirm, String otherName) {
        Material mat = accepted ? Material.GREEN_DYE : Material.RED_DYE;
        String label = otherName == null ? "Other player" : otherName;
        String name = (accepted ? ChatColor.GREEN : ChatColor.RED) + label;

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + (confirm ? "Confirm status" : "Accept status") + ": " + (accepted ? ChatColor.GREEN + "Ready" : ChatColor.RED + "Not ready"));

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack cancelButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Cancel");
            meta.setLore(List.of(ChatColor.GRAY + "Cancel this trade."));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack coinOfferSelf(long coins, long purse, boolean editable) {
        long safeCoins = Math.max(0L, coins);
        long safePurse = Math.max(0L, purse);

        ItemStack item = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Your Coin Offer");

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Offer coins from your purse.");
            lore.add("");
            lore.add(ChatColor.GRAY + "Offering: " + ChatColor.GOLD + formatCoins(safeCoins) + " coins");
            lore.add(ChatColor.GRAY + "Purse: " + ChatColor.YELLOW + formatCoins(safePurse) + " coins");

            if (editable) {
                lore.add("");
                lore.add(ChatColor.YELLOW + "Click to set an amount!");
                lore.add(ChatColor.GRAY + "Type a number in chat (0 to clear).");
                lore.add(ChatColor.DARK_GRAY + "Type 'cancel' to stop.");
            }

            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack coinOfferOther(long coins, String otherName) {
        long safeCoins = Math.max(0L, coins);
        String label = otherName == null ? "Other player" : otherName;

        ItemStack item = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + label + "'s Coin Offer");

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "They are offering:");
            lore.add("");
            lore.add(ChatColor.GOLD + formatCoins(safeCoins) + " coins");
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String formatCoins(long coins) {
        return String.format(Locale.US, "%,d", Math.max(0L, coins));
    }

    public static final class TradeHolder implements InventoryHolder {
        private final UUID sessionId;
        private final UUID viewerId;
        private final ViewType viewType;
        private Inventory inventory;

        public TradeHolder(UUID sessionId, UUID viewerId, ViewType viewType) {
            this.sessionId = sessionId;
            this.viewerId = viewerId;
            this.viewType = viewType == null ? ViewType.TRADE : viewType;
        }

        public UUID getSessionId() {
            return sessionId;
        }

        public UUID getViewerId() {
            return viewerId;
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
