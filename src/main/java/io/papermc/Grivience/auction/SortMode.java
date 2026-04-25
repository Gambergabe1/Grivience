package io.papermc.Grivience.auction;

public enum SortMode {
    ENDING_SOON("Ending Soon"),
    MOST_RECENT("Most Recent"),
    LOWEST_PRICE("Lowest Price"),
    HIGHEST_BID("Highest Bid");

    private final String displayName;

    SortMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
