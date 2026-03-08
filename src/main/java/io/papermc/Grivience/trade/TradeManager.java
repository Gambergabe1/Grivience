package io.papermc.Grivience.trade;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skyblock.economy.ProfileEconomyService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TradeManager {
    private static final long REQUEST_TIMEOUT_MS = 60_000L;
    private static final double REQUEST_DISTANCE_SQ = 9.0 * 9.0;

    private final GriviencePlugin plugin;
    private final ProfileEconomyService profileEconomy;
    private final Map<UUID, TradeRequest> incomingByTarget = new HashMap<>();
    private final Map<UUID, TradeRequest> outgoingByRequester = new HashMap<>();
    private final Map<UUID, TradeSession> sessionsByPlayer = new HashMap<>();
    private final Map<UUID, TradeSession> sessionsById = new HashMap<>();

    public TradeManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.profileEconomy = new ProfileEconomyService(plugin);
    }

    public TradeSession getSessionByPlayer(UUID playerId) {
        return playerId != null ? sessionsByPlayer.get(playerId) : null;
    }

    public TradeSession getSessionById(UUID sessionId) {
        return sessionId != null ? sessionsById.get(sessionId) : null;
    }

    public void requestTrade(Player requester, Player target) {
        if (requester == null || target == null) {
            return;
        }
        if (requester.getUniqueId().equals(target.getUniqueId())) {
            requester.sendMessage(ChatColor.RED + "You cannot trade with yourself.");
            return;
        }
        if (getSessionByPlayer(requester.getUniqueId()) != null) {
            requester.sendMessage(ChatColor.RED + "You are already in a trade.");
            return;
        }
        if (getSessionByPlayer(target.getUniqueId()) != null) {
            requester.sendMessage(ChatColor.RED + "That player is already in a trade.");
            return;
        }

        if (outgoingByRequester.containsKey(requester.getUniqueId())) {
            requester.sendMessage(ChatColor.RED + "You already have a pending trade request. Use /trade cancel.");
            return;
        }
        if (incomingByTarget.containsKey(target.getUniqueId())) {
            requester.sendMessage(ChatColor.RED + "That player already has a pending trade request.");
            return;
        }
        if (!withinDistance(requester, target)) {
            requester.sendMessage(ChatColor.RED + "You must be closer to trade (within 9 blocks).");
            return;
        }

        long expiresAt = System.currentTimeMillis() + REQUEST_TIMEOUT_MS;
        TradeRequest request = new TradeRequest(requester.getUniqueId(), requester.getName(), target.getUniqueId(), expiresAt);

        outgoingByRequester.put(requester.getUniqueId(), request);
        incomingByTarget.put(target.getUniqueId(), request);

        requester.sendMessage(ChatColor.GREEN + "Trade request sent to " + ChatColor.YELLOW + target.getName() + ChatColor.GREEN + ".");
        requester.sendMessage(ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/trade cancel" + ChatColor.GRAY + " to cancel.");

        target.sendMessage(ChatColor.YELLOW + requester.getName() + ChatColor.GREEN + " has sent you a trade request.");
        target.sendMessage(ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/trade accept " + requester.getName() + ChatColor.GRAY + " to trade.");
    }

    public void cancelOutgoing(Player requester) {
        if (requester == null) {
            return;
        }
        TradeRequest request = outgoingByRequester.remove(requester.getUniqueId());
        if (request == null) {
            requester.sendMessage(ChatColor.RED + "You have no pending trade request.");
            return;
        }
        incomingByTarget.remove(request.targetId);

        requester.sendMessage(ChatColor.YELLOW + "Trade request cancelled.");
        Player target = Bukkit.getPlayer(request.targetId);
        if (target != null) {
            target.sendMessage(ChatColor.RED + requester.getName() + " cancelled the trade request.");
        }
    }

    public void declineIncoming(Player target, Player requester) {
        if (target == null) {
            return;
        }
        TradeRequest request = incomingByTarget.get(target.getUniqueId());
        if (request == null) {
            target.sendMessage(ChatColor.RED + "You have no pending trade request.");
            return;
        }
        if (requester != null && !request.requesterId.equals(requester.getUniqueId())) {
            target.sendMessage(ChatColor.RED + "That player did not request a trade with you.");
            return;
        }

        incomingByTarget.remove(target.getUniqueId());
        outgoingByRequester.remove(request.requesterId);

        target.sendMessage(ChatColor.YELLOW + "Trade request declined.");
        Player req = Bukkit.getPlayer(request.requesterId);
        if (req != null) {
            req.sendMessage(ChatColor.RED + target.getName() + " declined your trade request.");
        }
    }

    public void acceptIncoming(Player target, Player requester) {
        if (target == null) {
            return;
        }
        TradeRequest request = incomingByTarget.get(target.getUniqueId());
        if (request == null) {
            target.sendMessage(ChatColor.RED + "You have no pending trade request.");
            return;
        }
        if (requester != null && !request.requesterId.equals(requester.getUniqueId())) {
            target.sendMessage(ChatColor.RED + "That player did not request a trade with you.");
            return;
        }

        long now = System.currentTimeMillis();
        if (now > request.expiresAtMs) {
            incomingByTarget.remove(target.getUniqueId());
            outgoingByRequester.remove(request.requesterId);
            target.sendMessage(ChatColor.RED + "That trade request has expired.");
            return;
        }

        Player req = Bukkit.getPlayer(request.requesterId);
        if (req == null) {
            incomingByTarget.remove(target.getUniqueId());
            outgoingByRequester.remove(request.requesterId);
            target.sendMessage(ChatColor.RED + "That player is no longer online.");
            return;
        }
        if (getSessionByPlayer(target.getUniqueId()) != null || getSessionByPlayer(req.getUniqueId()) != null) {
            incomingByTarget.remove(target.getUniqueId());
            outgoingByRequester.remove(request.requesterId);
            target.sendMessage(ChatColor.RED + "Trade could not be started (someone is already trading).");
            return;
        }
        if (!withinDistance(target, req)) {
            target.sendMessage(ChatColor.RED + "You must be closer to trade (within 9 blocks).");
            return;
        }

        incomingByTarget.remove(target.getUniqueId());
        outgoingByRequester.remove(request.requesterId);

        startSession(req, target);
    }

    public void cancelActiveTrade(Player player) {
        if (player == null) {
            return;
        }
        TradeSession session = getSessionByPlayer(player.getUniqueId());
        if (session == null) {
            player.sendMessage(ChatColor.RED + "You are not in an active trade.");
            return;
        }
        cancelSession(session, ChatColor.RED + "Trade cancelled.");
    }

    public void markOfferChanged(TradeSession session) {
        if (session == null || session.stage != TradeSession.Stage.TRADE) {
            return;
        }
        session.a.accepted = false;
        session.b.accepted = false;
        syncTradeViews(session);
    }

    public void setCoinOffer(Player player, long coins) {
        if (player == null) {
            return;
        }
        TradeSession session = getSessionByPlayer(player.getUniqueId());
        if (session == null || session.stage != TradeSession.Stage.TRADE) {
            player.sendMessage(ChatColor.RED + "You are not in an active trade.");
            return;
        }

        long safe = Math.max(0L, coins);
        if (safe > 0L && profileEconomy.requireSelectedProfile(player) == null) {
            return;
        }
        long purse = purseCoins(player);
        if (safe > purse) {
            player.sendMessage(ChatColor.RED + "You do not have that many coins in your purse.");
            player.sendMessage(ChatColor.GRAY + "Purse: " + ChatColor.YELLOW + String.format(java.util.Locale.US, "%,d", purse) + " coins");
            return;
        }

        TradeSession.Participant p = session.participant(player.getUniqueId());
        if (p == null) {
            return;
        }
        p.coinOffer = safe;
        markOfferChanged(session);
    }

    public void syncTradeViews(TradeSession session) {
        if (session == null) {
            return;
        }

        if (session.stage == TradeSession.Stage.TRADE) {
            ItemStack[] offerA = TradeGui.readSelfOffer(session.a.tradeInventory);
            ItemStack[] offerB = TradeGui.readSelfOffer(session.b.tradeInventory);

            TradeGui.setOtherOffer(session.a.tradeInventory, offerB);
            TradeGui.setOtherOffer(session.b.tradeInventory, offerA);

            TradeGui.updateTradeButtons(session.a.tradeInventory, session.a.name, session.b.name, session.a.accepted, session.b.accepted);
            TradeGui.updateTradeButtons(session.b.tradeInventory, session.b.name, session.a.name, session.b.accepted, session.a.accepted);

            Player aPlayer = Bukkit.getPlayer(session.a.id);
            Player bPlayer = Bukkit.getPlayer(session.b.id);
            long purseA = purseCoins(aPlayer);
            long purseB = purseCoins(bPlayer);

            TradeGui.updateCoinSlots(session.a.tradeInventory, session.b.name, session.a.coinOffer, session.b.coinOffer, purseA, true);
            TradeGui.updateCoinSlots(session.b.tradeInventory, session.a.name, session.b.coinOffer, session.a.coinOffer, purseB, true);
            return;
        }

        if (session.stage == TradeSession.Stage.CONFIRM) {
            TradeGui.updateConfirmButtons(session.a.confirmInventory, session.a.name, session.b.name, session.a.confirmed, session.b.confirmed);
            TradeGui.updateConfirmButtons(session.b.confirmInventory, session.b.name, session.a.name, session.b.confirmed, session.a.confirmed);

            Player aPlayer = Bukkit.getPlayer(session.a.id);
            Player bPlayer = Bukkit.getPlayer(session.b.id);
            long purseA = purseCoins(aPlayer);
            long purseB = purseCoins(bPlayer);

            TradeGui.updateCoinSlots(session.a.confirmInventory, session.b.name, session.coinsOfferA, session.coinsOfferB, purseA, false);
            TradeGui.updateCoinSlots(session.b.confirmInventory, session.a.name, session.coinsOfferB, session.coinsOfferA, purseB, false);
        }
    }

    public void toggleAccept(TradeSession session, UUID playerId) {
        if (session == null || session.stage != TradeSession.Stage.TRADE) {
            return;
        }
        TradeSession.Participant p = session.participant(playerId);
        if (p == null) {
            return;
        }

        Player player = Bukkit.getPlayer(playerId);
        if (player != null && p.coinOffer > 0L) {
            if (profileEconomy.requireSelectedProfile(player) == null) {
                return;
            }
            if (!profileEconomy.has(player, p.coinOffer)) {
                player.sendMessage(ChatColor.RED + "You don't have enough coins in your purse for that offer.");
                player.sendMessage(ChatColor.GRAY + "Reduce your coin offer before accepting.");
                return;
            }
        }

        p.accepted = !p.accepted;
        syncTradeViews(session);

        if (session.bothAccepted()) {
            openConfirm(session);
        }
    }

    public void toggleConfirm(TradeSession session, UUID playerId) {
        if (session == null || session.stage != TradeSession.Stage.CONFIRM) {
            return;
        }
        TradeSession.Participant p = session.participant(playerId);
        if (p == null) {
            return;
        }
        p.confirmed = !p.confirmed;
        syncTradeViews(session);

        if (session.bothConfirmed()) {
            finishTrade(session);
        }
    }

    public void cancelSession(TradeSession session, String reason) {
        if (session == null) {
            return;
        }
        if (sessionsById.remove(session.id) == null) {
            return; // already cancelled
        }
        sessionsByPlayer.remove(session.a.id);
        sessionsByPlayer.remove(session.b.id);

        Player aPlayer = Bukkit.getPlayer(session.a.id);
        Player bPlayer = Bukkit.getPlayer(session.b.id);

        if (aPlayer != null) {
            returnOfferItems(aPlayer, session.a.tradeInventory);
            if (reason != null) {
                aPlayer.sendMessage(reason);
            }
            closeIfViewingTrade(aPlayer, session.id);
        }

        if (bPlayer != null) {
            returnOfferItems(bPlayer, session.b.tradeInventory);
            if (reason != null) {
                bPlayer.sendMessage(reason);
            }
            closeIfViewingTrade(bPlayer, session.id);
        }
    }

    private void startSession(Player requester, Player target) {
        UUID sessionId = UUID.randomUUID();
        TradeSession session = new TradeSession(sessionId, requester.getUniqueId(), requester.getName(), target.getUniqueId(), target.getName());

        session.a.tradeInventory = TradeGui.createTradeInventory(sessionId, session.a.id, session.a.name, session.b.name, false, false);
        session.b.tradeInventory = TradeGui.createTradeInventory(sessionId, session.b.id, session.b.name, session.a.name, false, false);

        sessionsById.put(sessionId, session);
        sessionsByPlayer.put(session.a.id, session);
        sessionsByPlayer.put(session.b.id, session);

        // Populate mirrored offers + coin offer slots before opening the GUI.
        syncTradeViews(session);

        requester.openInventory(session.a.tradeInventory);
        target.openInventory(session.b.tradeInventory);

        requester.sendMessage(ChatColor.GREEN + "Trading with " + ChatColor.YELLOW + target.getName() + ChatColor.GREEN + ".");
        target.sendMessage(ChatColor.GREEN + "Trading with " + ChatColor.YELLOW + requester.getName() + ChatColor.GREEN + ".");
    }

    private void openConfirm(TradeSession session) {
        if (session == null || session.stage != TradeSession.Stage.TRADE) {
            return;
        }

        Player aPlayer = Bukkit.getPlayer(session.a.id);
        Player bPlayer = Bukkit.getPlayer(session.b.id);
        if (aPlayer == null || bPlayer == null) {
            cancelSession(session, ChatColor.RED + "Trade cancelled (player disconnected).");
            return;
        }

        // Validate coin offers before locking in the confirm step.
        if (!ensureCoinsAvailable(aPlayer, session.a.coinOffer) || !ensureCoinsAvailable(bPlayer, session.b.coinOffer)) {
            cancelSession(session, ChatColor.RED + "Trade cancelled (not enough coins).");
            return;
        }

        session.offerA = TradeGui.readSelfOffer(session.a.tradeInventory);
        session.offerB = TradeGui.readSelfOffer(session.b.tradeInventory);
        session.coinsOfferA = Math.max(0L, session.a.coinOffer);
        session.coinsOfferB = Math.max(0L, session.b.coinOffer);

        session.stage = TradeSession.Stage.CONFIRM;
        session.a.confirmed = false;
        session.b.confirmed = false;

        session.a.confirmInventory = TradeGui.createConfirmInventory(session.id, session.a.id, session.a.name, session.b.name, false, false, session.offerA, session.offerB);
        session.b.confirmInventory = TradeGui.createConfirmInventory(session.id, session.b.id, session.b.name, session.a.name, false, false, session.offerB, session.offerA);

        if (aPlayer != null) {
            aPlayer.openInventory(session.a.confirmInventory);
            aPlayer.sendMessage(ChatColor.YELLOW + "Confirm the trade to complete it.");
        }
        if (bPlayer != null) {
            bPlayer.openInventory(session.b.confirmInventory);
            bPlayer.sendMessage(ChatColor.YELLOW + "Confirm the trade to complete it.");
        }

        // Ensure coin offer items are present in the confirm view.
        syncTradeViews(session);
    }

    private void finishTrade(TradeSession session) {
        if (session == null || session.stage != TradeSession.Stage.CONFIRM) {
            return;
        }

        Player aPlayer = Bukkit.getPlayer(session.a.id);
        Player bPlayer = Bukkit.getPlayer(session.b.id);
        if (aPlayer == null || bPlayer == null) {
            cancelSession(session, ChatColor.RED + "Trade cancelled (player disconnected).");
            return;
        }

        int aNeeds = TradeSession.countStacks(session.offerB);
        int bNeeds = TradeSession.countStacks(session.offerA);
        if (countEmptySlots(aPlayer) < aNeeds) {
            cancelSession(session, ChatColor.RED + aPlayer.getName() + " does not have enough inventory space.");
            return;
        }
        if (countEmptySlots(bPlayer) < bNeeds) {
            cancelSession(session, ChatColor.RED + bPlayer.getName() + " does not have enough inventory space.");
            return;
        }

        // Trade coins (purse) if either side offered any.
        long coinsFromA = Math.max(0L, session.coinsOfferA);
        long coinsFromB = Math.max(0L, session.coinsOfferB);
        if (coinsFromA > 0L || coinsFromB > 0L) {
            if (!ensureCoinsAvailable(aPlayer, coinsFromA) || !ensureCoinsAvailable(bPlayer, coinsFromB)) {
                cancelSession(session, ChatColor.RED + "Trade cancelled (not enough coins).");
                return;
            }

            if (!transferCoins(aPlayer, bPlayer, coinsFromA, coinsFromB)) {
                cancelSession(session, ChatColor.RED + "Trade cancelled (coin transfer failed).");
                return;
            }
        }

        // Remove items from the offer slots (they remain in the trade inventory until this point).
        clearOfferSlots(session.a.tradeInventory);
        clearOfferSlots(session.b.tradeInventory);

        giveItems(aPlayer, session.offerB);
        giveItems(bPlayer, session.offerA);

        aPlayer.closeInventory();
        bPlayer.closeInventory();

        aPlayer.sendMessage(ChatColor.GREEN + "Trade completed.");
        bPlayer.sendMessage(ChatColor.GREEN + "Trade completed.");

        sessionsById.remove(session.id);
        sessionsByPlayer.remove(session.a.id);
        sessionsByPlayer.remove(session.b.id);
    }

    private void clearOfferSlots(Inventory tradeInventory) {
        if (tradeInventory == null) {
            return;
        }
        for (int slot : TradeGui.LEFT_OFFER_SLOTS) {
            tradeInventory.setItem(slot, null);
        }
    }

    private void returnOfferItems(Player owner, Inventory tradeInventory) {
        if (owner == null || tradeInventory == null) {
            return;
        }
        for (int slot : TradeGui.LEFT_OFFER_SLOTS) {
            ItemStack item = tradeInventory.getItem(slot);
            if (item == null || item.getType().isAir()) {
                continue;
            }
            tradeInventory.setItem(slot, null);
            Map<Integer, ItemStack> leftover = owner.getInventory().addItem(item);
            if (!leftover.isEmpty()) {
                for (ItemStack drop : leftover.values()) {
                    if (drop != null && !drop.getType().isAir()) {
                        owner.getWorld().dropItemNaturally(owner.getLocation(), drop);
                    }
                }
            }
        }
    }

    private void giveItems(Player receiver, ItemStack[] items) {
        if (receiver == null || items == null) {
            return;
        }
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            Map<Integer, ItemStack> leftover = receiver.getInventory().addItem(item.clone());
            if (!leftover.isEmpty()) {
                // Should not happen due to space checks, but don't delete items.
                for (ItemStack drop : leftover.values()) {
                    if (drop != null && !drop.getType().isAir()) {
                        receiver.getWorld().dropItemNaturally(receiver.getLocation(), drop);
                    }
                }
            }
        }
    }

    private int countEmptySlots(Player player) {
        if (player == null) {
            return 0;
        }
        int empty = 0;
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (ItemStack item : contents) {
            if (item == null || item.getType().isAir()) {
                empty++;
            }
        }
        return empty;
    }

    private void closeIfViewingTrade(Player player, UUID sessionId) {
        if (player == null) {
            return;
        }
        Inventory top = player.getOpenInventory().getTopInventory();
        if (!TradeGui.isTradeInventory(top)) {
            return;
        }
        InventoryHolder holder = top.getHolder();
        if (holder instanceof TradeGui.TradeHolder tradeHolder && sessionId.equals(tradeHolder.getSessionId())) {
            player.closeInventory();
        }
    }

    private boolean withinDistance(Player a, Player b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.getWorld() == null || b.getWorld() == null || !a.getWorld().equals(b.getWorld())) {
            return false;
        }
        Location la = a.getLocation();
        Location lb = b.getLocation();
        return la.distanceSquared(lb) <= REQUEST_DISTANCE_SQ;
    }

    private long purseCoins(Player player) {
        if (player == null) {
            return 0L;
        }
        double purse = profileEconomy.purse(player);
        if (!Double.isFinite(purse) || purse <= 0.0D) {
            return 0L;
        }
        return Math.max(0L, (long) Math.floor(purse + 1e-9D));
    }

    private boolean ensureCoinsAvailable(Player player, long coins) {
        if (player == null) {
            return false;
        }
        if (coins <= 0L) {
            return true;
        }
        // Ensure the player has a selected profile (creates a default profile if needed).
        if (profileEconomy.requireSelectedProfile(player) == null) {
            return false;
        }
        return profileEconomy.has(player, coins);
    }

    private boolean transferCoins(Player aPlayer, Player bPlayer, long coinsFromA, long coinsFromB) {
        if (coinsFromA > 0L) {
            if (!profileEconomy.withdraw(aPlayer, coinsFromA)) {
                return false;
            }
        }
        if (coinsFromB > 0L) {
            if (!profileEconomy.withdraw(bPlayer, coinsFromB)) {
                if (coinsFromA > 0L) {
                    profileEconomy.deposit(aPlayer, coinsFromA);
                }
                return false;
            }
        }

        // Deposit to opposite players.
        if (coinsFromB > 0L && !profileEconomy.deposit(aPlayer, coinsFromB)) {
            // Roll back
            profileEconomy.deposit(bPlayer, coinsFromB);
            if (coinsFromA > 0L) {
                profileEconomy.deposit(aPlayer, coinsFromA);
            }
            return false;
        }
        if (coinsFromA > 0L && !profileEconomy.deposit(bPlayer, coinsFromA)) {
            // Best-effort roll back; try to undo the deposit we just made to A as well.
            if (coinsFromB > 0L) {
                profileEconomy.withdraw(aPlayer, coinsFromB);
                profileEconomy.deposit(bPlayer, coinsFromB);
            }
            profileEconomy.deposit(aPlayer, coinsFromA);
            return false;
        }

        return true;
    }

    private static final class TradeRequest {
        final UUID requesterId;
        final String requesterName;
        final UUID targetId;
        final long expiresAtMs;

        TradeRequest(UUID requesterId, String requesterName, UUID targetId, long expiresAtMs) {
            this.requesterId = requesterId;
            this.requesterName = requesterName == null ? "Player" : requesterName;
            this.targetId = targetId;
            this.expiresAtMs = expiresAtMs;
        }
    }
}
