package io.papermc.Grivience.auction;

import java.util.UUID;

public record AuctionBid(UUID bidder, String bidderName, long amount, long timestamp) {
}
