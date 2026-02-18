package io.papermc.Grivience.item;

public enum ArmorWeightClass {
    HEAVY("Heavy"),
    BALANCED("Balanced"),
    LIGHT("Light");

    private final String displayName;

    ArmorWeightClass(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
