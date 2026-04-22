package io.papermc.Grivience.dungeon;

public enum RoomType {
    COMBAT("Crypt Clash"),
    PUZZLE_SEQUENCE("Rune Sequence"),
    PUZZLE_SYNC("Bell Plate Sync"),
    PUZZLE_CHIME("Storm Chime Memory"),
    PUZZLE_SEAL("Lever Seal Alignment"),
    TREASURE("Treasure Vault");

    private final String displayName;

    RoomType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isPuzzle() {
        return this == PUZZLE_SEQUENCE
                || this == PUZZLE_SYNC
                || this == PUZZLE_CHIME
                || this == PUZZLE_SEAL;
    }
}
