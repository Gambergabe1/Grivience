package io.papermc.Grivience.trade;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

final class TradeSession {
    enum Stage {
        TRADE,
        CONFIRM
    }

    static final class Participant {
        final UUID id;
        final String name;

        Inventory tradeInventory;
        Inventory confirmInventory;

        boolean accepted;
        boolean confirmed;

        long coinOffer;

        Participant(UUID id, String name) {
            this.id = id;
            this.name = name == null ? "Player" : name;
        }
    }

    final UUID id;
    final Participant a;
    final Participant b;

    Stage stage = Stage.TRADE;

    // Captured offers when transitioning to confirm.
    ItemStack[] offerA;
    ItemStack[] offerB;
    long coinsOfferA;
    long coinsOfferB;

    TradeSession(UUID id, UUID aId, String aName, UUID bId, String bName) {
        this.id = id;
        this.a = new Participant(aId, aName);
        this.b = new Participant(bId, bName);
    }

    boolean isParticipant(UUID playerId) {
        return playerId != null && (a.id.equals(playerId) || b.id.equals(playerId));
    }

    Participant participant(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        if (a.id.equals(playerId)) {
            return a;
        }
        if (b.id.equals(playerId)) {
            return b;
        }
        return null;
    }

    Participant other(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        if (a.id.equals(playerId)) {
            return b;
        }
        if (b.id.equals(playerId)) {
            return a;
        }
        return null;
    }

    boolean bothAccepted() {
        return a.accepted && b.accepted;
    }

    boolean bothConfirmed() {
        return a.confirmed && b.confirmed;
    }

    static int countStacks(ItemStack[] offer) {
        if (offer == null) {
            return 0;
        }
        int count = 0;
        for (ItemStack item : offer) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            count++;
        }
        return count;
    }
}
