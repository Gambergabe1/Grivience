package io.papermc.Grivience.dungeon;

public enum RoomType {
    COMBAT("Dojo Clash"),
    PUZZLE_SEQUENCE("Ofuda Sequence"),
    PUZZLE_SYNC("Shrine Bell Sync"),
    TREASURE("Kura Vault");

    private final String displayName;

    RoomType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isPuzzle() {
        return this == PUZZLE_SEQUENCE || this == PUZZLE_SYNC;
    }
}
