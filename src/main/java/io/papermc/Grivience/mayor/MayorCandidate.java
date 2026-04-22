package io.papermc.Grivience.mayor;

public enum MayorCandidate {
    AATROX("Aatrox", "Slayer buff: Gain more Slayer XP and reduce spawn costs.", "Aatrox"),
    MARINA("Marina", "Fishing buff: Gain more Fishing XP and rare sea creature chance.", "Marina"),
    PAUL("Paul", "Dungeon buff: Gain more Dungeon score and better reward chests.", "Paul");

    private final String displayName;
    private final String buffDescription;
    private final String skinName;

    MayorCandidate(String displayName, String buffDescription, String skinName) {
        this.displayName = displayName;
        this.buffDescription = buffDescription;
        this.skinName = skinName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBuffDescription() {
        return buffDescription;
    }

    public String getSkinName() {
        return skinName;
    }
}
