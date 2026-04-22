package io.papermc.Grivience.trade;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TradeListener implements Listener {
    private static final long COIN_INPUT_TIMEOUT_MS = 30_000L;

    private final GriviencePlugin plugin;
    private final TradeManager tradeManager;
    private final Map<UUID, CoinInput> coinInputByPlayer = new ConcurrentHashMap<>();

    public TradeListener(GriviencePlugin plugin, TradeManager tradeManager) {
        this.plugin = plugin;
        this.tradeManager = tradeManager;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSneakRightClickPlayer(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player target)) {
            return;
        }
        Player requester = event.getPlayer();
        if (!requester.isSneaking()) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return; // avoid double-fire (offhand)
        }

        // Hypixel-style: crouch + right-click a player to send a trade request.
        event.setCancelled(true);
        tradeManager.requestTrade(requester, target);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        InventoryHolder rawHolder = top != null ? top.getHolder() : null;
        if (!(rawHolder instanceof TradeGui.TradeHolder holder)) {
            return;
        }

        TradeSession session = tradeManager.getSessionById(holder.getSessionId());
        if (session == null || !session.isParticipant(player.getUniqueId())) {
            event.setCancelled(true);
            player.closeInventory();
            return;
        }

        // Never allow "collect to cursor" because it can pull from the mirrored offer slots.
        if (event.getClick() == ClickType.DOUBLE_CLICK || event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }

        int rawSlot = event.getRawSlot();

        if (holder.getViewType() == TradeGui.ViewType.CONFIRM) {
            event.setCancelled(true);
            if (rawSlot == TradeGui.CANCEL_SLOT) {
                tradeManager.cancelSession(session, ChatColor.RED + "Trade cancelled.");
            } else if (rawSlot == TradeGui.ACCEPT_SELF_SLOT) {
                tradeManager.toggleConfirm(session, player.getUniqueId());
            }
            return;
        }

        // TRADE view
        if (rawSlot == TradeGui.CANCEL_SLOT) {
            event.setCancelled(true);
            tradeManager.cancelSession(session, ChatColor.RED + "Trade cancelled.");
            return;
        }
        if (rawSlot == TradeGui.ACCEPT_SELF_SLOT) {
            event.setCancelled(true);
            tradeManager.toggleAccept(session, player.getUniqueId());
            return;
        }
        if (rawSlot == TradeGui.ACCEPT_OTHER_SLOT) {
            event.setCancelled(true);
            return;
        }
        if (rawSlot == TradeGui.COINS_SELF_SLOT) {
            event.setCancelled(true);
            beginCoinInput(player, session);
            return;
        }
        if (rawSlot == TradeGui.COINS_OTHER_SLOT) {
            event.setCancelled(true);
            return;
        }

        // Prevent dropping items out of the trade window.
        if (event.getClick() == ClickType.DROP || event.getClick() == ClickType.CONTROL_DROP) {
            if (rawSlot >= 0 && rawSlot < TradeGui.SIZE) {
                event.setCancelled(true);
            }
            return;
        }

        // Click in the top inventory
        if (rawSlot >= 0 && rawSlot < top.getSize()) {
            // Only allow interacting with the left offer slots (your offer). Everything else is locked.
            if (!TradeGui.isLeftOfferSlot(rawSlot)) {
                event.setCancelled(true);
                return;
            }

            // Allow normal behavior in your offer slots, but resync next tick.
            plugin.getServer().getScheduler().runTask(plugin, () -> tradeManager.markOfferChanged(session));
            return;
        }

        // Click in the bottom inventory
        if (shouldMirrorBottomShiftClick(event.isCancelled(), event.isShiftClick())) {
            // Shift-click would place into arbitrary top slots; force into the offer slots.
            event.setCancelled(true);
            moveToOfferSlots(player, top, event.getCurrentItem(), event.getClickedInventory(), event.getSlot());
            tradeManager.markOfferChanged(session);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        InventoryHolder rawHolder = top != null ? top.getHolder() : null;
        if (!(rawHolder instanceof TradeGui.TradeHolder holder)) {
            return;
        }

        TradeSession session = tradeManager.getSessionById(holder.getSessionId());
        if (session == null || !session.isParticipant(player.getUniqueId())) {
            event.setCancelled(true);
            player.closeInventory();
            return;
        }

        if (holder.getViewType() == TradeGui.ViewType.CONFIRM) {
            event.setCancelled(true);
            return;
        }

        // If the drag touches the top inventory, restrict it to left offer slots only.
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= 0 && rawSlot < top.getSize()) {
                if (!TradeGui.isLeftOfferSlot(rawSlot)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // Allow the drag, then mirror changes.
        plugin.getServer().getScheduler().runTask(plugin, () -> tradeManager.markOfferChanged(session));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        if (!(inv.getHolder() instanceof TradeGui.TradeHolder holder)) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        coinInputByPlayer.remove(player.getUniqueId());

        UUID playerId = player.getUniqueId();
        TradeSession session = tradeManager.getSessionByPlayer(playerId);
        if (session == null || !session.id.equals(holder.getSessionId())) {
            return;
        }

        // When switching from trade -> confirm we get a close event; only cancel if they're not in any trade view next tick.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            TradeSession current = tradeManager.getSessionByPlayer(playerId);
            if (current == null || !current.id.equals(holder.getSessionId())) {
                return;
            }
            Inventory currentTop = player.getOpenInventory().getTopInventory();
            if (TradeGui.isTradeInventory(currentTop)) {
                TradeGui.TradeHolder currentHolder = (TradeGui.TradeHolder) currentTop.getHolder();
                if (currentHolder != null && currentHolder.getSessionId().equals(holder.getSessionId())) {
                    return;
                }
            }
            tradeManager.cancelSession(current, ChatColor.RED + "Trade cancelled.");
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        coinInputByPlayer.remove(player.getUniqueId());
        TradeSession session = tradeManager.getSessionByPlayer(player.getUniqueId());
        if (session != null) {
            tradeManager.cancelSession(session, ChatColor.RED + "Trade cancelled (player left).");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (!coinInputByPlayer.containsKey(playerId)) {
            return;
        }

        String message = event.getMessage();
        event.setCancelled(true);

        // Handle on the main thread (trade state + inventory sync).
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            CoinInput input = coinInputByPlayer.get(playerId);
            if (input == null) {
                return;
            }

            long now = System.currentTimeMillis();
            if (now > input.expiresAtMs) {
                coinInputByPlayer.remove(playerId);
                player.sendMessage(ChatColor.RED + "Coin entry timed out.");
                return;
            }

            String msg = message == null ? "" : message.trim();
            if (msg.equalsIgnoreCase("cancel")) {
                coinInputByPlayer.remove(playerId);
                player.sendMessage(ChatColor.YELLOW + "Coin entry cancelled.");
                return;
            }

            String normalized = msg.replace(",", "").replace("_", "");
            long coins;
            try {
                coins = Long.parseLong(normalized);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid number. Type a whole number (e.g. 25000), or 'cancel'.");
                return;
            }

            TradeSession session = tradeManager.getSessionByPlayer(playerId);
            if (session == null || !session.id.equals(input.sessionId) || session.stage != TradeSession.Stage.TRADE) {
                coinInputByPlayer.remove(playerId);
                player.sendMessage(ChatColor.RED + "You are no longer in that trade.");
                return;
            }

            tradeManager.setCoinOffer(player, coins);
            coinInputByPlayer.remove(playerId);
        });
    }

    private void beginCoinInput(Player player, TradeSession session) {
        if (player == null || session == null) {
            return;
        }

        coinInputByPlayer.put(player.getUniqueId(), new CoinInput(session.id, System.currentTimeMillis() + COIN_INPUT_TIMEOUT_MS));
        player.sendMessage(ChatColor.GOLD + "Coin Offer");
        player.sendMessage(ChatColor.GRAY + "Type the number of coins to offer in chat (0 to clear).");
        player.sendMessage(ChatColor.DARK_GRAY + "Type 'cancel' to stop.");
    }

    private void moveToOfferSlots(Player player, Inventory tradeInventory, ItemStack stack, Inventory clickedInventory, int clickedSlot) {
        if (tradeInventory == null || stack == null || stack.getType().isAir() || clickedInventory == null) {
            return;
        }

        int dest = -1;
        for (int slot : TradeGui.LEFT_OFFER_SLOTS) {
            ItemStack existing = tradeInventory.getItem(slot);
            if (existing == null || existing.getType().isAir()) {
                dest = slot;
                break;
            }
        }

        if (dest == -1) {
            player.sendMessage(ChatColor.RED + "Your trade offer is full.");
            return;
        }

        tradeInventory.setItem(dest, stack.clone());
        clickedInventory.setItem(clickedSlot, null);
    }

    static boolean shouldMirrorBottomShiftClick(boolean cancelled, boolean shiftClick) {
        return shiftClick && !cancelled;
    }

    private record CoinInput(UUID sessionId, long expiresAtMs) {}
}
